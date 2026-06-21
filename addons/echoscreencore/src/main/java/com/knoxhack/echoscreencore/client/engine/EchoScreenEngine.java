package com.knoxhack.echoscreencore.client.engine;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knoxhack.echoscreencore.EchoScreenCoreMod;
import com.knoxhack.echoscreencore.api.EchoDataContext;
import com.knoxhack.echoscreencore.api.EchoScreenRegistry;
import com.knoxhack.echoscreencore.api.action.EchoAction;
import com.knoxhack.echoscreencore.api.action.EchoActionContext;
import com.knoxhack.echoscreencore.api.action.EchoActionRegistry;
import com.knoxhack.echoscreencore.api.layout.EchoRect;
import com.knoxhack.echoscreencore.api.style.EchoStyle;
import com.knoxhack.echoscreencore.api.theme.EchoAccessibilitySettings;
import com.knoxhack.echoscreencore.client.component.AbstractEchoComponent;
import com.knoxhack.echoscreencore.client.component.EchoComponent;
import com.knoxhack.echoscreencore.client.component.EchoComponentRegistry;
import com.knoxhack.echoscreencore.client.api.EchoFitScreenSurface;
import com.knoxhack.echoscreencore.client.component.basic.TextComponent;
import com.knoxhack.echoscreencore.client.debug.EchoDebugOverlay;
import com.knoxhack.echoscreencore.client.debug.EchoDiagnosticCatalog;
import com.knoxhack.echoscreencore.client.debug.EchoScreenDiagnostics;
import com.knoxhack.echoscreencore.client.input.EchoFocusManager;
import com.knoxhack.echoscreencore.client.input.EchoInputRouter;
import com.knoxhack.echoscreencore.client.layout.EchoLayoutEngine;
import com.knoxhack.echoscreencore.client.layout.EchoResponsiveContext;
import com.knoxhack.echoscreencore.client.overlay.EchoOverlayManager;
import com.knoxhack.echoscreencore.client.parser.EchoMarkupParser;
import com.knoxhack.echoscreencore.client.parser.EchoNode;
import com.knoxhack.echoscreencore.client.parser.EchoPageDefinition;
import com.knoxhack.echoscreencore.client.render.EchoRenderBridge;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;
import com.knoxhack.echoscreencore.client.render.EchoTextLayer;
import com.knoxhack.echoscreencore.client.render.EchoThemeBridge;
import com.knoxhack.echoscreencore.client.style.EchoStyleParser;
import com.knoxhack.echoscreencore.client.style.EchoStyleResolver;
import com.knoxhack.echoscreencore.client.style.EchoStyleSheet;
import com.knoxhack.echoscreencore.client.style.EchoStyleValues;
import com.knoxhack.echoscreencore.client.state.EchoPageStateStore;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public final class EchoScreenEngine {
    private static final Map<Identifier, EchoPageDefinition> PAGE_CACHE = new ConcurrentHashMap<>();
    private static final Map<Identifier, EchoStyleSheet> STYLE_CACHE = new ConcurrentHashMap<>();
    private static final Map<Identifier, EchoPageDefinition> COMPONENT_CACHE = new ConcurrentHashMap<>();
    private static final Pattern TEMPLATE_PARAM = Pattern.compile("\\{\\s*param\\.([^{}|]+)(?:\\|([^{}]*))?}");
    private static final List<Identifier> DEFAULT_STYLES = List.of(
        EchoScreenCoreMod.id("base"),
        EchoScreenCoreMod.id("components")
    );

    public record FitPolicy(String mode, int designWidth, int designHeight, boolean canvas) {
        public static FitPolicy responsive() {
            return new FitPolicy("responsive", 0, 0, false);
        }

        public static FitPolicy canvas(int designWidth, int designHeight) {
            return new FitPolicy("canvas", Math.max(1, designWidth), Math.max(1, designHeight), true);
        }

        public String designLabel() {
            return canvas ? designWidth + "x" + designHeight : "none";
        }
    }

    public record ClickActionProbeResult(
            boolean found,
            boolean handled,
            String action,
            String componentId,
            String actionValue,
            int x,
            int y,
            int width,
            int height,
            List<String> diagnostics) {
    }

    private final Identifier pageId;
    private final EchoDataContext baseDataContext;
    private EchoDataContext dataContext;
    private final EchoAccessibilitySettings accessibility;
    private final EchoActionContext.ScreenControls controls;
    private final EchoScreenDiagnostics diagnostics = new EchoScreenDiagnostics();
    private final EchoMarkupParser markupParser = new EchoMarkupParser();
    private final EchoStyleParser styleParser = new EchoStyleParser();
    private final EchoStyleResolver styleResolver = new EchoStyleResolver();
    private final EchoLayoutEngine layoutEngine = new EchoLayoutEngine();
    private final EchoThemeBridge themeBridge = new EchoThemeBridge();
    private final EchoRenderBridge renderBridge = new EchoRenderBridge();
    private final EchoTextLayer textLayer = new EchoTextLayer();
    private final EchoBindingResolver bindingResolver = new EchoBindingResolver();
    private final EchoFocusManager focusManager = new EchoFocusManager();
    private final EchoInputRouter inputRouter = new EchoInputRouter(focusManager);
    private final EchoDebugOverlay debugOverlay = new EchoDebugOverlay();
    private final EchoOverlayManager overlayManager = new EchoOverlayManager();
    private EchoComponent root;
    private int lastWidth = -1;
    private int lastHeight = -1;
    private int lastMouseX = Integer.MIN_VALUE;
    private int lastMouseY = Integer.MIN_VALUE;
    private boolean treeDirty = true;
    private boolean dataDirty = true;
    private boolean layoutDirty = true;
    private boolean focusDirty = true;
    private boolean debug;
    private boolean validationDirty = true;
    private boolean hoverDirty = true;
    private int validationPasses;
    private String lastOverlayStack = "";

    public EchoScreenEngine(Identifier pageId, EchoDataContext dataContext, EchoAccessibilitySettings accessibility,
            EchoActionContext.ScreenControls controls) {
        this.pageId = pageId;
        this.baseDataContext = dataContext == null ? EchoDataContext.empty() : dataContext;
        this.dataContext = EchoPageStateStore.attach(this.baseDataContext, pageId);
        this.accessibility = accessibility == null ? EchoAccessibilitySettings.DEFAULT : accessibility;
        this.controls = controls;
    }

    public static void clearCaches() {
        PAGE_CACHE.clear();
        STYLE_CACHE.clear();
        COMPONENT_CACHE.clear();
    }

    public static void clearStyleCaches() {
        STYLE_CACHE.clear();
    }

    public static void clearComponentCaches() {
        COMPONENT_CACHE.clear();
    }

    public static void clearPageCache(Identifier pageId) {
        if (pageId != null) {
            PAGE_CACHE.remove(pageId);
        }
    }

    public static List<Identifier> availablePages() {
        LinkedHashSet<Identifier> pages = new LinkedHashSet<>(availableResources("eui/pages", ".eui.xml"));
        pages.addAll(manifestPages());
        ArrayList<Identifier> sorted = new ArrayList<>(pages);
        sorted.sort(java.util.Comparator.comparing(Identifier::toString));
        return List.copyOf(sorted);
    }

    public static List<Identifier> availableStyles() {
        return availableResources("eui/styles", ".eui.css");
    }

    public static List<Identifier> availableComponents() {
        return availableResources("eui/components", ".eui.xml");
    }

    public static List<String> inspectPage(Identifier pageId) {
        return inspectPage(pageId, 854, 480);
    }

    public static List<String> inspectPage(Identifier pageId, int width, int height) {
        if (pageId == null) {
            return List.of("Invalid page id.");
        }
        EchoScreenEngine engine = new EchoScreenEngine(pageId, EchoDataContext.empty(), EchoAccessibilitySettings.DEFAULT, null);
        EchoPageDefinition page = engine.loadPage(pageId);
        FitPolicy policy = fitPolicy(page.root());
        EchoFitScreenSurface.Fit fit = EchoFitScreenSurface.fit(Math.max(1, width), Math.max(1, height),
                policy.designWidth(), policy.designHeight(), policy.canvas());
        int layoutWidth = fit.layoutWidth();
        int layoutHeight = fit.layoutHeight();
        engine.ensureTree();
        EchoRenderContext context = engine.context(null, null, layoutWidth, layoutHeight, 0, 0, 0.0F);
        if (engine.root != null) {
            engine.layoutEngine.layout(engine.root, context, layoutWidth, layoutHeight);
            engine.focusManager.rebuild(engine.root);
            engine.lastWidth = layoutWidth;
            engine.lastHeight = layoutHeight;
            engine.validateLayout(engine.root, 0);
        }
        ArrayList<String> lines = new ArrayList<>();
        lines.add("page=" + pageId);
        lines.add("resource=" + page.resourceId());
        lines.add("viewport=" + Math.max(1, width) + "x" + Math.max(1, height)
                + " breakpoint=" + context.responsive().activeBreakpoint().name().toLowerCase(Locale.ROOT));
        lines.add(String.format(Locale.ROOT, "fit=%s design=%s layout=%dx%d scaled=%dx%d offset=%d,%d scale=%.3f",
                policy.mode(), policy.designLabel(), layoutWidth, layoutHeight,
                fit.scaledWidth(), fit.scaledHeight(), fit.offsetX(), fit.offsetY(), fit.scale()));
        if (engine.root != null) {
            lines.add("root=" + engine.root.bounds().width() + "x" + engine.root.bounds().height()
                    + " scrollOwners=" + countTag(engine.root, "scroll")
                    + " focusable=" + countFocusable(engine.root));
        }
        lines.add("diagnostics=" + engine.diagnostics().issues().size());
        engine.diagnostics().issues().stream()
            .limit(12)
            .map(issue -> issue.code() + ": " + issue.message()
                    + " Fix: " + EchoDiagnosticCatalog.fixHint(issue.code()))
            .forEach(lines::add);
        return List.copyOf(lines);
    }

    public FitPolicy fitPolicy() {
        return fitPolicy(loadPage(pageId).root());
    }

    public static FitPolicy fitPolicy(EchoNode root) {
        if (root == null) {
            return FitPolicy.responsive();
        }
        String mode = root.attribute("fit-mode", "responsive").trim().toLowerCase(Locale.ROOT);
        boolean hasDesignCanvas = root.hasAttribute("design-width") || root.hasAttribute("design-height");
        if ("canvas".equals(mode) || hasDesignCanvas) {
            int designWidth = positiveInt(root.attribute("design-width",
                    Integer.toString(EchoFitScreenSurface.DEFAULT_DESIGN_WIDTH)),
                    EchoFitScreenSurface.DEFAULT_DESIGN_WIDTH);
            int designHeight = positiveInt(root.attribute("design-height",
                    Integer.toString(EchoFitScreenSurface.DEFAULT_DESIGN_HEIGHT)),
                    EchoFitScreenSurface.DEFAULT_DESIGN_HEIGHT);
            return FitPolicy.canvas(designWidth, designHeight);
        }
        return FitPolicy.responsive();
    }

    private static int positiveInt(String raw, int fallback) {
        int value = EchoStyleValues.intValue(raw, fallback);
        return value > 0 ? value : fallback;
    }

    public static List<String> inspectTextNodesForTests(Identifier pageId, EchoDataContext dataContext, int width, int height) {
        if (pageId == null) {
            return List.of("Invalid page id.");
        }
        EchoScreenEngine engine = new EchoScreenEngine(pageId,
                dataContext == null ? EchoDataContext.empty() : dataContext,
                EchoAccessibilitySettings.DEFAULT, null);
        ArrayList<TextComponent.TextDrawRecord> records = new ArrayList<>();
        TextComponent.setDrawProbeForTests(records::add);
        try {
            engine.render(ProbeGuiGraphics.create(), new ProbeFont(),
                    Math.max(1, width), Math.max(1, height), 0, 0, 0.0F);
        } catch (LinkageError | RuntimeException exception) {
            engine.ensureTree();
            EchoRenderContext context = engine.context(null, null, Math.max(1, width), Math.max(1, height), 0, 0, 0.0F);
            if (engine.root != null) {
                engine.layoutEngine.layout(engine.root, context, Math.max(1, width), Math.max(1, height));
            }
        } finally {
            TextComponent.setDrawProbeForTests(null);
        }
        ArrayList<String> lines = new ArrayList<>();
        if (!records.isEmpty()) {
            records.stream()
                    .filter(record -> !record.value().isBlank())
                    .map(EchoScreenEngine::textRecordLine)
                    .forEach(lines::add);
        } else if (engine.root != null) {
            EchoRenderContext context = engine.context(null, null, Math.max(1, width), Math.max(1, height), 0, 0, 0.0F);
            engine.collectTextNodeLines(engine.root, context, lines);
        }
        return List.copyOf(lines);
    }

    public static ClickActionProbeResult clickActionForTests(
            Identifier pageId,
            EchoDataContext dataContext,
            String actionId,
            int width,
            int height) {
        if (pageId == null || actionId == null || actionId.isBlank()) {
            return new ClickActionProbeResult(false, false, actionId, "", "", 0, 0, 0, 0, List.of("Missing page or action id."));
        }
        EchoScreenEngine engine = new EchoScreenEngine(pageId,
                dataContext == null ? EchoDataContext.empty() : dataContext,
                EchoAccessibilitySettings.DEFAULT,
                null);
        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, height);
        engine.ensureTree();
        if (engine.root == null) {
            return new ClickActionProbeResult(false, false, actionId, "", "", 0, 0, 0, 0, engine.diagnosticLines(actionId));
        }
        EchoRenderContext context = engine.context(null, new ProbeFont(), safeWidth, safeHeight, 0, 0, 0.0F);
        engine.layoutEngine.layout(engine.root, context, safeWidth, safeHeight);
        engine.focusManager.rebuild(engine.root);
        engine.lastWidth = safeWidth;
        engine.lastHeight = safeHeight;
        engine.layoutDirty = false;
        engine.focusDirty = false;
        EchoComponent component = engine.firstClickableAction(engine.root, actionId);
        if (component == null) {
            return new ClickActionProbeResult(false, false, actionId, "", "", 0, 0, 0, 0, engine.diagnosticLines(actionId));
        }
        EchoRect bounds = component.bounds() == null ? EchoRect.ZERO : component.bounds();
        int clickX = bounds.x() + Math.max(1, bounds.width()) / 2;
        int clickY = bounds.y() + Math.max(1, bounds.height()) / 2;
        EchoDataContext actionContext = component.dataContext() == null ? engine.dataContext : component.dataContext();
        String action = engine.bindingResolver.resolve(component.action(), actionContext, engine.diagnostics);
        String actionValue = engine.bindingResolver.resolve(component.actionValue(), actionContext, engine.diagnostics);
        boolean handled = engine.mouseClicked(clickX, clickY, GLFW.GLFW_MOUSE_BUTTON_LEFT);
        return new ClickActionProbeResult(
                true,
                handled,
                action,
                component.node().id(),
                actionValue,
                clickX,
                clickY,
                bounds.width(),
                bounds.height(),
                engine.diagnosticLines(actionId));
    }

    private EchoComponent firstClickableAction(EchoComponent component, String actionId) {
        if (component == null) {
            return null;
        }
        EchoDataContext localContext = component.dataContext() == null ? dataContext : component.dataContext();
        String action = bindingResolver.resolve(component.action(), localContext, diagnostics);
        if (actionId.equals(action)
                && component.bounds().width() > 0
                && component.bounds().height() > 0
                && component.focusable()
                && !component.disabled()) {
            return component;
        }
        for (EchoComponent child : component.children()) {
            EchoComponent found = firstClickableAction(child, actionId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private List<String> diagnosticLines() {
        return diagnosticLines("");
    }

    private List<String> diagnosticLines(String actionId) {
        ArrayList<String> lines = new ArrayList<>();
        collectActionDiagnostics(root, actionId == null ? "" : actionId, lines, 0);
        diagnostics.issues().stream()
                .map(issue -> issue.code() + ": " + issue.message())
                .limit(Math.max(0, 64 - lines.size()))
                .forEach(lines::add);
        return List.copyOf(lines);
    }

    private void collectActionDiagnostics(EchoComponent component, String actionId, List<String> lines, int depth) {
        if (component == null || lines.size() >= 64) {
            return;
        }
        EchoDataContext localContext = component.dataContext() == null ? dataContext : component.dataContext();
        String action = bindingResolver.resolve(component.action(), localContext, diagnostics);
        if (!action.isBlank() && (actionId.isBlank() || actionId.equals(action) || lines.size() < 24)) {
            EchoRect bounds = component.bounds() == null ? EchoRect.ZERO : component.bounds();
            String componentId = component.node().id().isBlank() ? "<no-id>" : component.node().id();
            lines.add("action_candidate: action=" + action
                    + " id=" + componentId
                    + " tag=" + component.node().tagName()
                    + " class=" + component.node().classes()
                    + " focusable=" + component.focusable()
                    + " disabled=" + component.disabled()
                    + " bounds=" + bounds.x() + "," + bounds.y() + "," + bounds.width() + "x" + bounds.height());
        }
        if (depth >= 64) {
            return;
        }
        for (EchoComponent child : component.children()) {
            collectActionDiagnostics(child, actionId, lines, depth + 1);
        }
    }

    private static String textRecordLine(TextComponent.TextDrawRecord record) {
        EchoRect bounds = record.bounds() == null ? EchoRect.ZERO : record.bounds();
        int alpha = (record.color() >>> 24) & 255;
        return record.tag() + "|class=" + record.classes()
                + "|value=" + record.value()
                + "|bounds=" + bounds.x() + "," + bounds.y() + "," + bounds.width() + "x" + bounds.height()
                + "|color=#" + String.format(Locale.ROOT, "%08x", record.color())
                + "|alpha=" + alpha
                + "|clipped=" + record.clipped()
                + "|queued=" + record.queued()
                + "|drawCalled=" + record.drawCalled()
                + "|status=" + record.status();
    }

    public static List<String> validateReferencePages(List<String> pageIds) {
        int[][] viewports = {{360, 240}, {854, 480}, {1280, 720}};
        ArrayList<String> issues = new ArrayList<>();
        for (String raw : pageIds == null ? List.<String>of() : pageIds) {
            Identifier pageId;
            try {
                pageId = Identifier.parse(raw);
            } catch (RuntimeException exception) {
                issues.add("unknown_reference_page: " + raw + " Fix: " + EchoDiagnosticCatalog.fixHint("unknown_reference_page"));
                continue;
            }
            for (int[] viewport : viewports) {
                List<String> lines = inspectPage(pageId, viewport[0], viewport[1]);
                boolean badPage = pageId.getPath().equals("reference_bad_layouts");
                boolean hasDiagnostics = lines.stream().anyMatch(line -> line.startsWith("diagnostics=") && !line.equals("diagnostics=0"));
                if (!badPage && hasDiagnostics) {
                    issues.add("reference_page_failed_contract: " + pageId + " @ " + viewport[0] + "x" + viewport[1]
                            + " Fix: " + EchoDiagnosticCatalog.fixHint("reference_page_failed_contract"));
                }
            }
        }
        return issues.isEmpty() ? List.of("ScreenCore reference pages valid.") : List.copyOf(issues);
    }

    private static int countTag(EchoComponent component, String tag) {
        if (component == null) {
            return 0;
        }
        int count = tag.equals(component.node().tagName()) ? 1 : 0;
        for (EchoComponent child : component.children()) {
            count += countTag(child, tag);
        }
        return count;
    }

    private static int countFocusable(EchoComponent component) {
        if (component == null) {
            return 0;
        }
        int count = component.focusable() ? 1 : 0;
        for (EchoComponent child : component.children()) {
            count += countFocusable(child);
        }
        return count;
    }

    private void collectTextNodeLines(EchoComponent component, EchoRenderContext context, List<String> lines) {
        if (component == null) {
            return;
        }
        String tag = component.node().tagName();
        if ("title".equals(tag) || "text".equals(tag)) {
            String raw = component.node().attribute("value", "");
            if (raw.isBlank()) {
                raw = component.node().text();
            }
            EchoDataContext local = component.dataContext() == null ? dataContext : component.dataContext();
            String value = bindingResolver.resolve(raw, local, diagnostics);
            String classes = String.join(" ", component.node().classes());
            boolean queueEligible = !value.isBlank() && component.bounds().width() > 0 && component.bounds().height() > 0;
            lines.add(tag + "|class=" + classes
                    + "|value=" + value
                    + "|bounds=" + component.bounds().x() + "," + component.bounds().y()
                    + "," + component.bounds().width() + "x" + component.bounds().height()
                    + "|queueEligible=" + queueEligible
                    + "|visibility=" + component.style().value("visibility", "visible")
                    + "|layout=" + component.style().value("layout", ""));
        }
        for (EchoComponent child : component.children()) {
            collectTextNodeLines(child, context, lines);
        }
    }

    public static List<String> validateManifests() {
        ArrayList<String> issues = new ArrayList<>();
        LinkedHashSet<Identifier> seen = new LinkedHashSet<>();
        try {
            for (Map.Entry<Identifier, Resource> entry : Minecraft.getInstance().getResourceManager()
                    .listResources("eui", id -> id.getPath().endsWith("eui_manifest.json")).entrySet()) {
                try (Reader reader = entry.getValue().openAsReader()) {
                    JsonElement element = JsonParser.parseReader(reader);
                    if (!element.isJsonObject()) {
                        issues.add(entry.getKey() + ": root must be an object");
                        continue;
                    }
                    JsonObject root = element.getAsJsonObject();
                    if (!root.has("pages") || !root.get("pages").isJsonArray()) {
                        issues.add(entry.getKey() + ": missing pages array");
                        continue;
                    }
                    for (JsonElement pageElement : root.getAsJsonArray("pages")) {
                        if (!pageElement.isJsonObject() || !pageElement.getAsJsonObject().has("id")) {
                            issues.add(entry.getKey() + ": page entry missing id");
                            continue;
                        }
                        String raw = pageElement.getAsJsonObject().get("id").getAsString();
                        try {
                            Identifier id = Identifier.parse(raw);
                            if (!seen.add(id)) {
                                issues.add(entry.getKey() + ": duplicate page id " + id);
                            }
                        } catch (RuntimeException exception) {
                            issues.add(entry.getKey() + ": invalid page id " + raw);
                        }
                    }
                }
            }
        } catch (RuntimeException | IOException exception) {
            issues.add("manifest read failed: " + exception.getMessage());
        }
        return issues.isEmpty() ? List.of("ScreenCore manifests valid.") : List.copyOf(issues);
    }

    private static List<Identifier> availableResources(String folder, String suffix) {
        ArrayList<Identifier> pages = new ArrayList<>();
        try {
            for (Identifier resource : Minecraft.getInstance().getResourceManager()
                    .listResources(folder, id -> id.getPath().endsWith(suffix)).keySet()) {
                String path = resource.getPath();
                path = path.substring((folder + "/").length(), path.length() - suffix.length());
                pages.add(Identifier.fromNamespaceAndPath(resource.getNamespace(), path));
            }
        } catch (RuntimeException exception) {
            EchoScreenCoreMod.LOGGER.debug("Unable to list ScreenCore resources in {}.", folder, exception);
        }
        pages.sort(java.util.Comparator.comparing(Identifier::toString));
        return List.copyOf(pages);
    }

    private static List<Identifier> manifestPages() {
        ArrayList<Identifier> pages = new ArrayList<>();
        try {
            for (Resource resource : Minecraft.getInstance().getResourceManager()
                    .listResources("eui", id -> id.getPath().endsWith("eui_manifest.json")).values()) {
                try (Reader reader = resource.openAsReader()) {
                    JsonElement element = JsonParser.parseReader(reader);
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    JsonObject root = element.getAsJsonObject();
                    if (!root.has("pages") || !root.get("pages").isJsonArray()) {
                        continue;
                    }
                    for (JsonElement pageElement : root.getAsJsonArray("pages")) {
                        if (pageElement.isJsonObject() && pageElement.getAsJsonObject().has("id")) {
                            try {
                                pages.add(Identifier.parse(pageElement.getAsJsonObject().get("id").getAsString()));
                            } catch (RuntimeException ignored) {
                            }
                        }
                    }
                }
            }
        } catch (RuntimeException | IOException exception) {
            EchoScreenCoreMod.LOGGER.debug("Unable to read ScreenCore page manifests.", exception);
        }
        return List.copyOf(pages);
    }

    public EchoScreenDiagnostics diagnostics() {
        return diagnostics;
    }

    public boolean debug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public int validationPassesForTests() {
        return validationPasses;
    }

    public int hoverHitTestsForTests() {
        return inputRouter.hoverHitTestsForTests();
    }

    public void markDataDirty() {
        dataContext = EchoPageStateStore.attach(baseDataContext, pageId);
        dataDirty = true;
        treeDirty = true;
        layoutDirty = true;
        focusDirty = true;
        validationDirty = true;
        hoverDirty = true;
        inputRouter.invalidateHover();
    }

    public void reloadPage() {
        PAGE_CACHE.remove(pageId);
        markDataDirty();
    }

    public void render(GuiGraphicsExtractor graphics, Font font, int width, int height, int mouseX, int mouseY, float partialTick) {
        EchoRenderContext context = context(graphics, font, width, height, mouseX, mouseY, partialTick);
        renderBridge.beginFrame();
        textLayer.beginFrame();
        try {
            ensureTree();
            if (root == null) {
                return;
            }
            overlayManager.beginFrame(root);
            boolean viewportChanged = width != lastWidth || height != lastHeight;
            boolean mouseMoved = mouseX != lastMouseX || mouseY != lastMouseY;
            String overlayStack = overlayManager.describeStack();
            boolean overlayStackChanged = !overlayStack.equals(lastOverlayStack);
            if (overlayStackChanged) {
                lastOverlayStack = overlayStack;
                inputRouter.invalidateHover();
            }
            if (layoutDirty || viewportChanged) {
                layoutEngine.layout(root, context, width, height);
                lastWidth = width;
                lastHeight = height;
                layoutDirty = false;
                inputRouter.invalidateHover();
            }
            if (focusDirty || viewportChanged) {
                focusManager.rebuild(root);
                focusDirty = false;
                inputRouter.invalidateHover();
            }
            if (mouseMoved) {
                lastMouseX = mouseX;
                lastMouseY = mouseY;
            }
            if (hoverDirty) {
                inputRouter.invalidateHover();
                hoverDirty = false;
            }
            inputRouter.updateHover(root, mouseX, mouseY);
            if (debug || validationDirty) {
                validateLayout(root, 0);
                validationDirty = false;
                validationPasses++;
            }
            root.render(context);
            nextRenderStratum(graphics);
            overlayManager.render(context);
            nextRenderStratum(graphics);
            textLayer.flush(context);
            if (debug) {
                nextRenderStratum(graphics);
                debugOverlay.render(context, root, inputRouter.hoverTarget(), focusManager);
            }
            if (dataDirty) {
                dataDirty = false;
            }
        } finally {
            renderBridge.endFrame(graphics);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ensureLayoutForInput(mouseX, mouseY);
        if (root == null) {
            return false;
        }
        if (overlayManager.mouseClicked(mouseX, mouseY, button, inputRouter, this::runAction)) {
            layoutDirty = true;
            validationDirty = true;
            inputRouter.invalidateHover();
            return true;
        }
        return inputRouter.mouseClicked(root, mouseX, mouseY, button, this::runAction);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        ensureLayoutForInput(mouseX, mouseY);
        return root != null && inputRouter.mouseReleased(mouseX, mouseY, button, this::runAction)
                || root != null && root.bounds().contains(mouseX, mouseY);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        ensureLayoutForInput(mouseX, mouseY);
        return root != null && inputRouter.mouseDragged(mouseX, mouseY, button, dragX, dragY, this::runAction);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double deltaY) {
        ensureLayoutForInput(mouseX, mouseY);
        boolean handled = root != null && inputRouter.mouseScrolled(root, mouseX, mouseY, deltaY);
        if (handled) {
            layoutDirty = true;
            inputRouter.invalidateHover();
        }
        return handled;
    }

    public boolean keyPressed(int key) {
        if (overlayManager.keyPressed(key, inputRouter, this::runAction)) {
            layoutDirty = true;
            validationDirty = true;
            inputRouter.invalidateHover();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            return runAction("back");
        }
        boolean handled = inputRouter.keyPressed(key, this::runAction);
        if (handled) {
            layoutDirty = true;
            inputRouter.invalidateHover();
        }
        return handled;
    }

    public boolean charTyped(String typed) {
        boolean handled = inputRouter.charTyped(typed, this::runAction);
        if (handled) {
            markDataDirty();
        }
        return handled;
    }

    public boolean runAction(String actionSpec) {
        return runAction(actionSpec, null, "");
    }

    public boolean runAction(String actionSpec, EchoComponent component, String inputEvent) {
        if (actionSpec == null || actionSpec.isBlank()) {
            return false;
        }
        EchoDataContext actionContext = component != null && component.dataContext() != null ? component.dataContext() : dataContext;
        String action = bindingResolver.resolve(actionSpec.strip(), actionContext, diagnostics);
        String argument = component == null ? "" : bindingResolver.resolve(component.actionValue(), actionContext, diagnostics);
        if (argument.isBlank() && component != null && !component.currentValue().isBlank()) {
            argument = component.currentValue();
        }
        int colon = action.indexOf(':');
        if (argument.isBlank() && colon > 0) {
            argument = action.substring(colon + 1);
            action = action.substring(0, colon);
        }
        String actionPage = component == null ? "" : bindingResolver.resolve(component.node().attribute("action-page", ""), actionContext, diagnostics);
        return switch (action) {
            case "noop" -> true;
            case "close", "screencore.close" -> controls != null && controls.close();
            case "back", "screencore.back" -> {
                String pageBackAction = pageBackAction();
                yield pageBackAction.isBlank() ? controls != null && controls.back() : runAction(pageBackAction, null, inputEvent);
            }
            case "reload_page", "screencore.reload_page" -> {
                reloadPage();
                yield true;
            }
            case "open_modal", "screencore.open_modal" -> {
                String target = component == null ? "" : bindingResolver.resolve(component.node().attribute("action-target", ""), actionContext, diagnostics);
                if (target.isBlank()) {
                    target = argument;
                }
                boolean opened = overlayManager.openModal(target, focusManager, component);
                if (!opened) {
                    diagnostics.warnOnce("modal_missing", target);
                }
                yield opened;
            }
            case "close_modal", "screencore.close_modal", "confirm", "screencore.confirm", "cancel", "screencore.cancel" -> {
                overlayManager.closeModal(focusManager);
                yield true;
            }
            case "debug_toggle", "screencore.debug_toggle" -> {
                debug = !debug;
                yield true;
            }
            case "open_page", "screencore.open_page" -> controls != null && controls.open(parsePage(actionPage.isBlank() ? argument : actionPage), dataContext);
            default -> runRegisteredAction(actionSpec, action, argument, component, actionContext, inputEvent);
        };
    }

    private boolean runRegisteredAction(String actionSpec, String action, String argument, EchoComponent component,
            EchoDataContext actionDataContext, String inputEvent) {
        Optional<EchoAction> registered = EchoActionRegistry.action(actionSpec).or(() -> EchoActionRegistry.action(action));
        if (registered.isEmpty()) {
            diagnostics.warnOnce("action_not_registered", actionSpec);
            return false;
        }
        return registered.get().run(new EchoActionContext(
            pageId,
            component == null ? "" : component.node().id(),
            dataContext,
            component == null ? null : component.dataContext(),
            action,
            argument,
            argument,
            actionParams(component, actionDataContext),
            inputEvent == null ? "" : inputEvent,
            controls
        ));
    }

    private Map<String, String> actionParams(EchoComponent component, EchoDataContext actionDataContext) {
        if (component == null) {
            return Map.of();
        }
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : component.node().attributes().entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("action-param-")) {
                params.put(key.substring("action-param-".length()),
                    bindingResolver.resolve(entry.getValue(), actionDataContext, diagnostics));
            }
        }
        String target = bindingResolver.resolve(component.node().attribute("action-target", ""), actionDataContext, diagnostics);
        if (!target.isBlank()) {
            params.putIfAbsent("target", target);
        }
        String page = bindingResolver.resolve(component.node().attribute("action-page", ""), actionDataContext, diagnostics);
        if (!page.isBlank()) {
            params.putIfAbsent("page", page);
        }
        return Map.copyOf(params);
    }

    private String pageBackAction() {
        ensureTree();
        if (root == null || root.node() == null) {
            return "";
        }
        String action = root.node().attribute("escape-action", "");
        if (action.isBlank()) {
            action = root.node().attribute("back-action", "");
        }
        action = action == null ? "" : action.strip();
        return Set.of("", "back", "screencore.back").contains(action) ? "" : action;
    }

    private Identifier parsePage(String raw) {
        String value = raw == null || raw.isBlank() ? pageId.toString() : raw.strip();
        try {
            return value.contains(":")
                ? Identifier.parse(value)
                : Identifier.fromNamespaceAndPath(pageId.getNamespace(), value);
        } catch (RuntimeException exception) {
            diagnostics.warnOnce("bad_resource_path", value);
            return pageId;
        }
    }

    private EchoRenderContext context(GuiGraphicsExtractor graphics, Font font, int width, int height, int mouseX, int mouseY, float partialTick) {
        return new EchoRenderContext(
            graphics,
            font,
            width,
            height,
            mouseX,
            mouseY,
            partialTick,
            themeBridge.tokens(accessibility),
            themeBridge,
            renderBridge,
            accessibility,
            dataContext,
            bindingResolver,
            focusManager,
            EchoResponsiveContext.of(width, height, guiScale()),
            overlayManager,
            diagnostics,
            textLayer,
            debug
        );
    }

    private static void nextRenderStratum(GuiGraphicsExtractor graphics) {
        if (graphics != null) {
            graphics.nextStratum();
        }
    }

    private void ensureTree() {
        if (!treeDirty) {
            return;
        }
        diagnostics.clear();
        EchoPageDefinition page = loadPage(pageId);
        List<EchoStyleSheet> styles = loadStyles(page);
        Set<String> ids = new HashSet<>();
        dataContext = EchoPageStateStore.attach(baseDataContext, pageId);
        EchoNode expandedRoot = expandTemplates(page.root(), page.pageId().getNamespace(), new ArrayDeque<>(), Map.of(), Map.of());
        root = build(expandedRoot, styles, ids, dataContext, List.of());
        treeDirty = false;
        layoutDirty = true;
        focusDirty = true;
        validationDirty = true;
        inputRouter.invalidateHover();
    }

    private void ensureLayoutForInput(double mouseX, double mouseY) {
        ensureTree();
        if (root == null || (!layoutDirty && !focusDirty) || lastWidth <= 0 || lastHeight <= 0) {
            return;
        }
        Font font;
        try {
            font = Minecraft.getInstance().font;
        } catch (RuntimeException exception) {
            return;
        }
        EchoRenderContext context = context(null, font, lastWidth, lastHeight,
                (int) Math.round(mouseX), (int) Math.round(mouseY), 0.0F);
        layoutEngine.layout(root, context, lastWidth, lastHeight);
        if (focusDirty) {
            focusManager.rebuild(root);
            focusDirty = false;
        }
        layoutDirty = false;
        inputRouter.invalidateHover();
    }

    private double guiScale() {
        try {
            return Minecraft.getInstance().getWindow().getGuiScale();
        } catch (RuntimeException exception) {
            return 1.0D;
        }
    }

    private EchoNode expandTemplates(EchoNode node, String namespace, Deque<Identifier> stack,
            Map<String, String> params, Map<String, List<EchoNode>> slots) {
        List<EchoNode> expanded = expandNode(node, namespace, stack, params, slots);
        if (expanded.isEmpty()) {
            return EchoNode.builder("empty-state")
                .attribute("title", "Empty ScreenCore template")
                .attribute("body", "Template expansion produced no root node.")
                .build();
        }
        if (expanded.size() == 1) {
            return expanded.get(0);
        }
        EchoNode.Builder wrapper = EchoNode.builder("column").attribute("gap", "space(sm)");
        expanded.forEach(wrapper::child);
        return wrapper.build();
    }

    private List<EchoNode> expandNode(EchoNode node, String namespace, Deque<Identifier> stack,
            Map<String, String> params, Map<String, List<EchoNode>> slots) {
        if (node == null) {
            return List.of();
        }
        if ("include".equals(node.tagName()) || "component".equals(node.tagName())) {
            return expandTemplateInvocation(node, namespace, stack, params);
        }
        if ("slot-outlet".equals(node.tagName())) {
            String name = node.attribute("name", "default");
            List<EchoNode> projected = slots.get(name);
            if (projected == null || projected.isEmpty()) {
                projected = node.children();
            }
            ArrayList<EchoNode> out = new ArrayList<>();
            for (EchoNode child : projected) {
                out.addAll(expandNode(child, namespace, stack, params, slots));
            }
            return List.copyOf(out);
        }
        if ("param".equals(node.tagName()) || "slot".equals(node.tagName())) {
            return List.of();
        }
        EchoNode.Builder builder = EchoNode.builder(node.tagName())
            .source(node.source())
            .text(applyParams(node.text(), params, node));
        for (Map.Entry<String, String> attribute : node.attributes().entrySet()) {
            builder.attribute(attribute.getKey(), applyParams(attribute.getValue(), params, node));
        }
        for (EchoNode child : node.children()) {
            for (EchoNode expandedChild : expandNode(child, namespace, stack, params, slots)) {
                builder.child(expandedChild);
            }
        }
        return List.of(builder.build());
    }

    private List<EchoNode> expandTemplateInvocation(EchoNode node, String namespace, Deque<Identifier> stack,
            Map<String, String> parentParams) {
        String raw = applyParams(node.attribute("src", ""), parentParams, node);
        Identifier templateId = parseTemplateId(raw, namespace);
        if (templateId == null) {
            diagnostics.warnOnce("template_missing", node.attribute("src", ""));
            return List.of();
        }
        if (stack.contains(templateId)) {
            diagnostics.warnOnce("circular_include", templateId.toString());
            return List.of();
        }
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        for (Map.Entry<String, String> attribute : node.attributes().entrySet()) {
            if (!Set.of("src", "id", "class").contains(attribute.getKey())) {
                params.put(attribute.getKey(), applyParams(attribute.getValue(), parentParams, node));
            }
        }
        LinkedHashMap<String, List<EchoNode>> slots = new LinkedHashMap<>();
        ArrayList<EchoNode> defaultSlot = new ArrayList<>();
        for (EchoNode child : node.children()) {
            if ("param".equals(child.tagName())) {
                String name = child.attribute("name", "");
                if (!name.isBlank()) {
                    params.put(name, applyParams(child.attribute("value", child.text()), parentParams, child));
                }
            } else if ("slot".equals(child.tagName())) {
                slots.put(child.attribute("name", "default"), child.children());
            } else {
                defaultSlot.add(child);
            }
        }
        if (!defaultSlot.isEmpty()) {
            slots.putIfAbsent("default", List.copyOf(defaultSlot));
            slots.putIfAbsent("body", List.copyOf(defaultSlot));
        }
        EchoPageDefinition template = loadComponentTemplate(templateId);
        EchoNode rootNode = template.root();
        List<EchoNode> templateRoots = "template".equals(rootNode.tagName()) ? rootNode.children() : List.of(rootNode);
        stack.push(templateId);
        ArrayList<EchoNode> out = new ArrayList<>();
        for (EchoNode root : templateRoots) {
            out.addAll(expandNode(root, templateId.getNamespace(), stack, params, slots));
        }
        stack.pop();
        return List.copyOf(out);
    }

    private String applyParams(String raw, Map<String, String> params, EchoNode sourceNode) {
        if (raw == null || raw.isEmpty() || params.isEmpty() && !raw.contains("{param.")) {
            return raw == null ? "" : raw;
        }
        Matcher matcher = TEMPLATE_PARAM.matcher(raw);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1).strip();
            String fallback = matcher.group(2);
            String replacement = params.get(key);
            if (replacement == null) {
                replacement = fallback == null ? "" : fallback;
                if (fallback == null) {
                    diagnostics.warnOnce("template_param_missing", key + " in " + sourceNode.tagName());
                }
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private Identifier parseTemplateId(String raw, String namespace) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return raw.contains(":")
                ? Identifier.parse(raw.strip())
                : Identifier.fromNamespaceAndPath(namespace, raw.strip());
        } catch (RuntimeException exception) {
            diagnostics.warnOnce("bad_resource_path", raw);
            return null;
        }
    }

    private EchoComponent build(EchoNode node, List<EchoStyleSheet> styles, Set<String> ids,
            EchoDataContext localContext, List<EchoNode> ancestors) {
        EchoNode runtimeNode = runtimeId(node, ids);
        ArrayList<EchoComponent> children = new ArrayList<>();
        if (isBoundList(runtimeNode) || "repeat".equals(runtimeNode.tagName())) {
            children.addAll(buildRepeatedChildren(runtimeNode, styles, ids, localContext, ancestors));
        } else {
            List<EchoNode> childAncestors = appendAncestor(ancestors, runtimeNode);
            for (EchoNode child : runtimeNode.children()) {
                children.add(build(child, styles, ids, localContext, childAncestors));
            }
        }
        EchoStyle style = styleResolver.resolve(runtimeNode, ancestors, styles, accessibility, diagnostics,
                com.knoxhack.echoscreencore.client.style.EchoStyleState.NONE);
        if (runtimeNode.hasAttribute("action")) {
            validateAction(runtimeNode.attribute("action", ""));
        }
        EchoComponent component = EchoComponentRegistry.create(runtimeNode, children, style, diagnostics);
        if (component instanceof AbstractEchoComponent abstractComponent) {
            abstractComponent.setStyleContext(styles, ancestors);
        }
        component.setDataContext(localContext);
        return component;
    }

    private List<EchoComponent> buildRepeatedChildren(EchoNode node, List<EchoStyleSheet> styles, Set<String> ids,
            EchoDataContext parentContext, List<EchoNode> ancestors) {
        String source = node.attribute("bind", node.attribute("source", ""));
        String itemName = node.attribute("item", "item").strip();
        if (itemName.isBlank()) {
            diagnostics.warnOnce("repeater_item_name_missing", node.tagName() + "#" + node.id());
            itemName = "item";
        }
        Optional<Object> resolved = resolveSource(source, parentContext);
        if (resolved.isEmpty()) {
            diagnostics.warnOnce("bound_list_source_missing", source);
            return buildEmptyState(node, styles, ids, parentContext, ancestors);
        }
        List<?> items = collection(resolved.get());
        if (items == null) {
            diagnostics.warnOnce("bound_list_source_not_collection", source);
            return buildEmptyState(node, styles, ids, parentContext, ancestors);
        }
        if (items.isEmpty()) {
            return buildEmptyState(node, styles, ids, parentContext, ancestors);
        }
        if (items.size() > 100 && !"scroll".equals(node.tagName())) {
            diagnostics.warnOnce("too_many_repeated_rows_without_scroll", node.tagName() + "#" + node.id() + " -> " + items.size());
        }
        ArrayList<EchoComponent> repeated = new ArrayList<>();
        String selected = bindingResolver.resolve(node.attribute("selected", ""), parentContext, diagnostics);
        String keyExpression = node.attribute("key", "");
        List<EchoNode> templates = node.children().stream()
            .filter(child -> !"empty-state".equals(child.tagName()))
            .toList();
        if (templates.isEmpty()) {
            diagnostics.warnOnce("repeater_template_missing", node.tagName() + "#" + node.id());
            return List.of();
        }
        List<EchoNode> childAncestors = appendAncestor(ancestors, node);
        for (int index = 0; index < items.size(); index++) {
            Object item = items.get(index);
            EchoDataContext itemContext = parentContext.child(itemName, item)
                .put(itemName + ".index", index)
                .put("repeat.index", index)
                .put("repeat.value", item);
            for (EchoNode template : templates) {
                EchoNode expanded = repeatNode(template, itemContext, itemName, index, selected, keyExpression);
                repeated.add(build(expanded, styles, ids, itemContext, childAncestors));
            }
        }
        return List.copyOf(repeated);
    }

    private Optional<Object> resolveSource(String source, EchoDataContext context) {
        if (source == null || source.isBlank()) {
            return Optional.empty();
        }
        if (bindingResolver.containsBinding(source)) {
            return bindingResolver.resolveObject(source, context, diagnostics);
        }
        return context.resolve(source.strip());
    }

    private List<EchoComponent> buildEmptyState(EchoNode node, List<EchoStyleSheet> styles, Set<String> ids,
            EchoDataContext parentContext, List<EchoNode> ancestors) {
        List<EchoNode> childAncestors = appendAncestor(ancestors, node);
        return node.children().stream()
            .filter(child -> "empty-state".equals(child.tagName()))
            .map(child -> build(child, styles, ids, parentContext, childAncestors))
            .toList();
    }

    private static List<EchoNode> appendAncestor(List<EchoNode> ancestors, EchoNode node) {
        ArrayList<EchoNode> next = new ArrayList<>(ancestors == null ? List.of() : ancestors);
        if (node != null) {
            next.add(node);
        }
        return List.copyOf(next);
    }

    private EchoNode repeatNode(EchoNode node, EchoDataContext itemContext, String itemName, int index, String selected, String keyExpression) {
        EchoNode.Builder builder = EchoNode.builder(node.tagName()).source(node.source()).text(node.text());
        String stableKey = bindingResolver.resolve(keyExpression, itemContext, diagnostics);
        for (Map.Entry<String, String> attribute : node.attributes().entrySet()) {
            String key = attribute.getKey();
            String value = attribute.getValue();
            if (bindingResolver.containsBinding(value)) {
                value = bindingResolver.resolve(value, itemContext, diagnostics);
            }
            if ("id".equals(key)) {
                if (value.isBlank()) {
                    value = stableKey.isBlank() ? itemName + "-" + index : stableKey;
                }
            }
            builder.attribute(key, value);
        }
        if (!stableKey.isBlank() && node.id().isBlank()) {
            builder.attribute("id", stableKey);
            builder.attribute("key", stableKey);
        }
        String candidate = bindingResolver.resolve(node.attribute("value",
            node.attribute("id", node.attribute("action-value", ""))), itemContext, diagnostics);
        if (!selected.isBlank() && !candidate.isBlank() && selected.equals(candidate)) {
            builder.attribute("selected", "true");
            builder.attribute("active", "true");
        }
        for (EchoNode child : node.children()) {
            builder.child(repeatNode(child, itemContext, itemName, index, selected, ""));
        }
        return builder.build();
    }

    private EchoNode runtimeId(EchoNode node, Set<String> ids) {
        if (node.id().isBlank()) {
            return node;
        }
        if (ids.add(node.id())) {
            return node;
        }
        String next = node.id() + "--" + ids.size();
        while (!ids.add(next)) {
            next = node.id() + "--" + ids.size();
        }
        diagnostics.warnOnce("repeated_template_duplicate_id", node.id());
        EchoNode.Builder builder = EchoNode.builder(node.tagName()).source(node.source()).text(node.text());
        for (Map.Entry<String, String> attribute : node.attributes().entrySet()) {
            builder.attribute(attribute.getKey(), "id".equals(attribute.getKey()) ? next : attribute.getValue());
        }
        for (EchoNode child : node.children()) {
            builder.child(child);
        }
        return builder.build();
    }

    private static boolean isBoundList(EchoNode node) {
        return Set.of("list", "select", "dropdown").contains(node.tagName()) && node.hasAttribute("bind");
    }

    private static List<?> collection(Object value) {
        if (value instanceof List<?> list) {
            return list;
        }
        if (value instanceof Iterable<?> iterable) {
            ArrayList<Object> out = new ArrayList<>();
            iterable.forEach(out::add);
            return out;
        }
        if (value != null && value.getClass().isArray()) {
            ArrayList<Object> out = new ArrayList<>();
            int length = java.lang.reflect.Array.getLength(value);
            for (int i = 0; i < length; i++) {
                out.add(java.lang.reflect.Array.get(value, i));
            }
            return out;
        }
        return null;
    }

    private void validateAction(String action) {
        if (action == null || action.isBlank()) {
            return;
        }
        String base = action.contains(":") ? action.substring(0, action.indexOf(':')) : action;
        if (Set.of("noop", "close", "back", "open_page", "debug_toggle", "reload_page",
            "open_modal", "close_modal", "confirm", "cancel",
            "screencore.open_page", "screencore.back", "screencore.close", "screencore.reload_page",
            "screencore.debug_toggle", "screencore.open_modal", "screencore.close_modal",
            "screencore.confirm", "screencore.cancel").contains(base)) {
            return;
        }
        if (EchoActionRegistry.action(action).isEmpty() && EchoActionRegistry.action(base).isEmpty()) {
            diagnostics.warnOnce("action_not_registered", action);
        }
    }

    private EchoPageDefinition loadPage(Identifier id) {
        return PAGE_CACHE.computeIfAbsent(id, page -> {
            Identifier resourceId = pageResource(page);
            String xml = readResource(resourceId).orElse("<page><empty-state title=\"Missing page\" body=\"" + resourceId + "\"/></page>");
            return markupParser.parse(page, resourceId, xml, diagnostics);
        });
    }

    private EchoPageDefinition loadComponentTemplate(Identifier id) {
        return COMPONENT_CACHE.computeIfAbsent(id, component -> {
            Identifier resourceId = componentResource(component);
            String xml = readResource(resourceId).orElse("<empty-state title=\"Missing component template\" body=\"" + resourceId + "\"/>");
            return markupParser.parse(component, resourceId, xml, diagnostics);
        });
    }

    private List<EchoStyleSheet> loadStyles(EchoPageDefinition page) {
        ArrayList<Identifier> ids = new ArrayList<>(DEFAULT_STYLES);
        ids.addAll(EchoScreenRegistry.styleSheets());
        String pageStyles = page.root().attribute("styles", "");
        if (!pageStyles.isBlank()) {
            for (String style : pageStyles.split(",")) {
                if (!style.isBlank()) {
                    try {
                        ids.add(style.contains(":") ? Identifier.parse(style.strip()) : Identifier.fromNamespaceAndPath(page.pageId().getNamespace(), style.strip()));
                    } catch (RuntimeException exception) {
                        diagnostics.warnOnce("bad_resource_path", style.strip());
                    }
                }
            }
        }
        ArrayList<EchoStyleSheet> sheets = new ArrayList<>();
        for (Identifier id : ids) {
            sheets.add(STYLE_CACHE.computeIfAbsent(id, styleId -> {
                Identifier resourceId = styleResource(styleId);
                String css = readResource(resourceId).orElse("");
                return styleParser.parse(resourceId, css, diagnostics);
            }));
        }
        return List.copyOf(sheets);
    }

    private Optional<String> readResource(Identifier resourceId) {
        Optional<String> managedResource = readManagedResource(resourceId);
        if (managedResource.isPresent()) {
            return managedResource;
        }
        Optional<String> classpathResource = readClasspathResource(resourceId);
        if (classpathResource.isPresent()) {
            return classpathResource;
        }
        diagnostics.warnOnce("resource_missing", resourceId.toString());
        return Optional.empty();
    }

    private Optional<String> readManagedResource(Identifier resourceId) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null || minecraft.getResourceManager() == null) {
                return Optional.empty();
            }
            Optional<Resource> resource = minecraft.getResourceManager().getResource(resourceId);
            if (resource.isEmpty()) {
                return Optional.empty();
            }
            try (Reader reader = resource.get().openAsReader()) {
                StringBuilder out = new StringBuilder();
                char[] buffer = new char[2048];
                int read;
                while ((read = reader.read(buffer)) >= 0) {
                    out.append(buffer, 0, read);
                }
                return Optional.of(out.toString());
            }
        } catch (IOException | RuntimeException exception) {
            diagnostics.warnOnce("resource_read_failed", resourceId + ": " + exception.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> readClasspathResource(Identifier resourceId) {
        String path = "assets/" + resourceId.getNamespace() + "/" + resourceId.getPath();
        try (InputStream stream = EchoScreenEngine.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return Optional.empty();
            }
            return Optional.of(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException exception) {
            diagnostics.warnOnce("resource_read_failed", resourceId + ": " + exception.getMessage());
            return Optional.empty();
        }
    }

    private static Identifier pageResource(Identifier page) {
        String path = page.getPath();
        if (!path.startsWith("eui/pages/")) {
            path = "eui/pages/" + path;
        }
        if (!path.endsWith(".eui.xml")) {
            path += ".eui.xml";
        }
        return Identifier.fromNamespaceAndPath(page.getNamespace(), path);
    }

    private static Identifier styleResource(Identifier style) {
        String path = style.getPath();
        if (!path.startsWith("eui/styles/")) {
            path = "eui/styles/" + path;
        }
        if (!path.endsWith(".eui.css")) {
            path += ".eui.css";
        }
        return Identifier.fromNamespaceAndPath(style.getNamespace(), path);
    }

    private static Identifier componentResource(Identifier component) {
        String path = component.getPath();
        if (!path.startsWith("eui/components/")) {
            path = "eui/components/" + path;
        }
        if (!path.endsWith(".eui.xml")) {
            path += ".eui.xml";
        }
        return Identifier.fromNamespaceAndPath(component.getNamespace(), path);
    }

    private void validateLayout(EchoComponent component, int borderedDepth) {
        validateLayout(component, borderedDepth, false, false);
    }

    private void validateLayout(EchoComponent component, int borderedDepth, boolean insideScroll, boolean insideRow) {
        if (component == null) {
            return;
        }
        boolean currentScroll = "scroll".equals(component.node().tagName());
        boolean currentRow = rowLike(component);
        if (currentScroll && insideScroll) {
            diagnostics.warnOnce("nested_scroll_region", label(component)
                    + " is inside another scroll region. Fix: keep only one scroll owner for this content path.");
        }
        if (component == root && (component.bounds().x() < 0 || component.bounds().y() < 0
            || component.bounds().right() > Math.max(1, lastWidth)
            || component.bounds().bottom() > Math.max(1, lastHeight) + Math.max(1, component.maxScroll()))) {
            diagnostics.warnOnce("root_overflows_viewport", label(component)
                    + " exceeds the viewport. Fix: use sc_app_shell, stack-below, or a scroll owner.");
        }
        validateAuthoringRules(component, insideRow);
        int nextBorderedDepth = borderedDepth + (hasBorder(component) ? 1 : 0);
        if (nextBorderedDepth > 4) {
            diagnostics.warnOnce("too_many_nested_bordered_containers", component.node().tagName() + "#" + component.node().id());
        }
        if (component.children().isEmpty() && Set.of("panel", "card", "section", "hero-card").contains(component.node().tagName())
            && component.node().text().isBlank() && !component.node().hasAttribute("title")) {
            diagnostics.warnOnce("empty_panel_without_empty_state", component.node().tagName() + "#" + component.node().id());
        }
        for (EchoComponent child : component.children()) {
            if (child.bounds().width() <= 0 || child.bounds().height() <= 0) {
                validateLayout(child, nextBorderedDepth, insideScroll || currentScroll, insideRow || currentRow);
                continue;
            }
            if (!component.bounds().contains(child.bounds().x(), child.bounds().y())
                || child.bounds().right() > component.bounds().right() + 1
                || child.bounds().bottom() > component.bounds().bottom() + Math.max(1, child.maxScroll())) {
                diagnostics.warnOnce("component_outside_parent_bounds", child.node().tagName() + " in " + component.node().tagName());
            }
            if (currentRow && child.bounds().right() > component.bounds().right() + 1) {
                diagnostics.warnOnce("row_overflow", label(component)
                        + " children exceed row bounds. Fix: use fixed badges/icons and one flexible sc-row-copy column.");
            }
            if (component.maxScroll() <= 0 && child.bounds().bottom() > component.bounds().bottom() + 1
                && !"scroll".equals(component.node().tagName())) {
                diagnostics.warnOnce("overflow_without_scroll", child.node().tagName() + " in " + component.node().tagName());
            }
            validateLayout(child, nextBorderedDepth, insideScroll || currentScroll, insideRow || currentRow);
        }
    }

    private void validateAuthoringRules(EchoComponent component, boolean insideRow) {
        String tag = component.node().tagName();
        String height = component.style().value("height", "");
        int fixedHeight = fixedPixelValue(height);
        if (fixedHeight > 260 && !"scroll".equals(tag)) {
            diagnostics.warnOnce("large_fixed_height", label(component) + " uses height=" + height
                    + ". Fix: move long content into a scroll region and cap panels near 260px.");
        }
        if ((tag.equals("grid") || tag.equals("split-view")) && columnCount(component) > 1
                && component.style().value("stack-below").isEmpty()
                && component.node().attribute("stack-below", "").isBlank()) {
            diagnostics.warnOnce("grid_missing_stack_below", label(component)
                    + " has multiple columns and no stack-below. Fix: add stack-below=\"900\" or use sc-list-detail/sc-three-column.");
        }
        if (tag.equals("list") && component.node().children().stream().noneMatch(child -> "empty-state".equals(child.tagName()))) {
            diagnostics.warnOnce("missing_list_empty_state", label(component)
                    + " has no empty-state. Fix: add an empty-state child.");
        }
        if (insideRow && Set.of("title", "text", "label").contains(tag)
                && component.style().value("max-lines").isEmpty()
                && !component.style().bool("wrap", false)
                && !"hidden".equalsIgnoreCase(component.style().value("overflow", ""))) {
            diagnostics.warnOnce("unbounded_row_text", label(component)
                    + " in a row has no max-lines, wrap, or overflow guard. Fix: use sc-row-copy or set max-lines and overflow.");
        }
    }

    private static int fixedPixelValue(String raw) {
        if (raw == null || raw.isBlank() || raw.contains("%") || raw.contains("fr")
                || raw.startsWith("space(") || raw.startsWith("font(") || raw.startsWith("scale(")) {
            return -1;
        }
        return EchoStyleValues.intValue(raw, -1);
    }

    private static int columnCount(EchoComponent component) {
        String columns = component.style().value("columns", component.node().attribute("columns", ""));
        if (columns == null || columns.isBlank()) {
            return 1;
        }
        int count = 0;
        for (String part : columns.split("\\s+")) {
            if (!part.isBlank()) {
                count++;
            }
        }
        return Math.max(1, count);
    }

    private static boolean rowLike(EchoComponent component) {
        String tag = component.node().tagName();
        String layout = component.style().value("layout", tag);
        return "row".equals(layout) || tag.equals("row") || tag.equals("list-row")
                || tag.equals("nav-item") || tag.equals("dropdown-item") || tag.equals("option")
                || tag.equals("dialog-actions") || tag.equals("status-chip-row");
    }

    private static String label(EchoComponent component) {
        String id = component.node().id().isBlank() ? "" : "#" + component.node().id();
        String classes = component.node().classes().isEmpty() ? "" : "." + String.join(".", component.node().classes());
        return component.node().tagName() + id + classes;
    }

    private static boolean hasBorder(EchoComponent component) {
        try {
            return Float.parseFloat(component.style().value("border-width", "0").replace("px", "").strip()) > 0.0F;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static final class ProbeFont extends Font {
        private ProbeFont() {
            super(new Provider() {
                @Override
                public net.minecraft.client.gui.GlyphSource glyphs(FontDescription description) {
                    return null;
                }

                @Override
                public net.minecraft.client.gui.font.glyphs.EffectGlyph effect() {
                    return null;
                }
            });
        }

        @Override
        public int width(String value) {
            return value == null ? 0 : value.length() * 6;
        }

        @Override
        public String plainSubstrByWidth(String value, int width) {
            return plainSubstrByWidth(value, width, false);
        }

        @Override
        public String plainSubstrByWidth(String value, int width, boolean reverse) {
            if (value == null || width <= 0) {
                return "";
            }
            int maxChars = Math.max(0, width / 6);
            if (value.length() <= maxChars) {
                return value;
            }
            return reverse ? value.substring(Math.max(0, value.length() - maxChars)) : value.substring(0, maxChars);
        }
    }

    private static final class ProbeGuiGraphics extends GuiGraphicsExtractor {
        private ProbeGuiGraphics(int width, int height) {
            super(null, null, width, height);
        }

        private static ProbeGuiGraphics create() {
            try {
                java.lang.reflect.Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                sun.misc.Unsafe unsafe = (sun.misc.Unsafe) field.get(null);
                return (ProbeGuiGraphics) unsafe.allocateInstance(ProbeGuiGraphics.class);
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                return null;
            }
        }

        @Override
        public void nextStratum() {
        }

        @Override
        public void enableScissor(int x0, int y0, int x1, int y1) {
        }

        @Override
        public void disableScissor() {
        }

        @Override
        public void fill(int x0, int y0, int x1, int y1, int color) {
        }

        @Override
        public void outline(int x, int y, int width, int height, int color) {
        }

        @Override
        public void text(Font font, String text, int x, int y, int color) {
        }

        @Override
        public void text(Font font, String text, int x, int y, int color, boolean shadow) {
        }

        @Override
        public void centeredText(Font font, String text, int x, int y, int color) {
        }

        @Override
        public void blit(Identifier texture, int x0, int y0, int x1, int y1,
                float u0, float u1, float v0, float v1) {
        }

        @Override
        public void item(ItemStack stack, int x, int y) {
        }

        @Override
        public void itemDecorations(Font font, ItemStack stack, int x, int y) {
        }

        @Override
        public void setTooltipForNextFrame(Font font, ItemStack stack, int x, int y) {
        }
    }
}
