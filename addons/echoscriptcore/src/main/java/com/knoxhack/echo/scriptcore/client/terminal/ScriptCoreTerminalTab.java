package com.knoxhack.echo.scriptcore.client.terminal;

import com.knoxhack.echo.scriptcore.EchoScriptCore;
import com.knoxhack.echo.scriptcore.adapter.EchoScriptAdapterRegistry;
import com.knoxhack.echo.scriptcore.api.EchoScriptAdapter;
import com.knoxhack.echo.scriptcore.api.EchoScriptCoreApi;
import com.knoxhack.echo.scriptcore.api.EchoScriptDefinitionView;
import com.knoxhack.echo.scriptcore.api.EchoScriptDiagnostic;
import com.knoxhack.echo.scriptcore.api.EchoScriptDiagnosticsSummary;
import com.knoxhack.echo.scriptcore.api.EchoScriptLoadResult;
import com.knoxhack.echo.scriptcore.registry.EchoScriptRegistry;
import com.knoxhack.echoterminal.api.ClientTerminalTab;
import com.knoxhack.echoterminal.api.TerminalRenderContext;
import com.knoxhack.echoterminal.api.TerminalScreenCorePageMetadata;
import com.knoxhack.echoterminal.api.TerminalTabChrome;
import com.knoxhack.echoterminal.api.TerminalTabDescriptor;
import com.knoxhack.echoterminal.api.TerminalUi;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class ScriptCoreTerminalTab implements ClientTerminalTab, TerminalScreenCorePageMetadata {
    public static final Identifier TAB_ID = EchoScriptCore.id("terminal_browser");
    private static final Identifier SCREENCORE_PAGE_ID =
            Identifier.fromNamespaceAndPath("echoterminal", "terminal_scriptcore_browser");
    private static final int ROW_H = 34;
    private static final int FILTER_H = 20;
    private static final int MAX_CHIPS = 12;

    private final TerminalTabDescriptor descriptor =
            new TerminalTabDescriptor(TAB_ID, "SCRIPTCORE", 215, 0xFF66D9EF);
    private final TerminalTabChrome chrome =
            TerminalTabChrome.of("ScriptCore", TerminalTabChrome.GROUP_SYSTEMS, "SC",
                    "Scripted pack browser", 215);
    private final List<Hitbox> hitboxes = new ArrayList<>();

    private View view = View.OVERVIEW;
    private int selectedIndex;
    private String searchText = "";
    private String packFilter = "";
    private String typeFilter = "";
    private EchoScriptDiagnostic.Severity severityFilter;
    private AdapterFilter adapterFilter = AdapterFilter.ALL;
    private int lastListY;

    @Override
    public TerminalTabDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public TerminalTabChrome chrome() {
        return chrome;
    }

    @Override
    public Identifier screenCorePageId() {
        return SCREENCORE_PAGE_ID;
    }

    @Override
    public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int mouseX, int mouseY, float partialTick) {
        hitboxes.clear();
        clampSelection();
        int x = context.contentX();
        int y = context.contentY();
        int w = context.contentWidth();
        EchoScriptDiagnosticsSummary summary = EchoScriptCoreApi.get().diagnosticsSummary();
        EchoScriptLoadResult last = EchoScriptCoreApi.get().lastResult();
        int cy = TerminalUi.flatDataPanel(context, graphics, x, y, w, 86,
                "SCRIPTCORE", summary.definitionCount() + " definition(s) / "
                        + summary.errors() + " error(s) / " + summary.warnings() + " warning(s)",
                descriptor.accentColor()) + 8;
        cy = renderTabs(context, graphics, x + 10, cy, w - 20, mouseX, mouseY) + 10;
        cy = renderFilters(context, graphics, x + 10, cy, w - 20, mouseX, mouseY) + 8;
        switch (view) {
            case OVERVIEW -> renderOverview(context, graphics, x + 10, cy, w - 20, summary, last);
            case DEFINITIONS -> renderDefinitions(context, graphics, x + 10, cy, w - 20, mouseX, mouseY);
            case DIAGNOSTICS -> renderDiagnostics(context, graphics, x + 10, cy, w - 20, mouseX, mouseY, last);
            case ADAPTERS -> renderAdapters(context, graphics, x + 10, cy, w - 20, mouseX, mouseY);
        }
    }

    @Override
    public int contentHeight(TerminalRenderContext context) {
        int rows = switch (view) {
            case OVERVIEW -> 11;
            case DEFINITIONS -> Math.max(8, filteredDefinitions().size());
            case DIAGNOSTICS -> Math.max(8, filteredDiagnostics(EchoScriptCoreApi.get().lastResult().diagnostics()).size());
            case ADAPTERS -> Math.max(8, filteredAdapters().size());
        };
        return Math.max(context.contentHeight(), 178 + rows * ROW_H);
    }

    @Override
    public boolean mouseClicked(TerminalRenderContext context, double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        int x = context.contentX() + 10;
        int y = context.contentY() + 94;
        int w = context.contentWidth() - 20;
        int tabW = Math.max(72, Math.min(120, (w - 18) / View.values().length));
        for (int i = 0; i < View.values().length; i++) {
            int tx = x + i * (tabW + 6);
            if (TerminalUi.inside(mouseX, mouseY, tx, y, tabW, 18)) {
                view = View.values()[i];
                selectedIndex = 0;
                context.playCommandSound();
                return true;
            }
        }
        for (Hitbox hitbox : List.copyOf(hitboxes)) {
            if (TerminalUi.inside(mouseX, mouseY, hitbox.x(), hitbox.y(), hitbox.w(), hitbox.h())) {
                hitbox.action().run();
                selectedIndex = 0;
                context.playCommandSound();
                return true;
            }
        }
        int index = ((int) mouseY - lastListY) / ROW_H;
        int size = switch (view) {
            case DEFINITIONS -> filteredDefinitions().size();
            case DIAGNOSTICS -> filteredDiagnostics(EchoScriptCoreApi.get().lastResult().diagnostics()).size();
            case ADAPTERS -> filteredAdapters().size();
            case OVERVIEW -> 0;
        };
        if (index >= 0 && index < size) {
            selectedIndex = index;
            context.playCommandSound();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(TerminalRenderContext context, KeyEvent event) {
        if (event == null || view == View.OVERVIEW) {
            return false;
        }
        int key = event.key();
        if (key == GLFW.GLFW_KEY_BACKSPACE && !searchText.isEmpty()) {
            searchText = searchText.substring(0, searchText.offsetByCodePoints(searchText.length(), -1));
            selectedIndex = 0;
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (!searchText.isEmpty()) {
                searchText = "";
                selectedIndex = 0;
                return true;
            }
            if (clearFilters()) {
                selectedIndex = 0;
                return true;
            }
        }
        if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN) {
            int delta = key == GLFW.GLFW_KEY_UP ? -1 : 1;
            int size = switch (view) {
                case DEFINITIONS -> filteredDefinitions().size();
                case DIAGNOSTICS -> filteredDiagnostics(EchoScriptCoreApi.get().lastResult().diagnostics()).size();
                case ADAPTERS -> filteredAdapters().size();
                case OVERVIEW -> 0;
            };
            selectedIndex = Math.max(0, Math.min(Math.max(0, size - 1), selectedIndex + delta));
            return size > 0;
        }
        return false;
    }

    @Override
    public boolean charTyped(TerminalRenderContext context, CharacterEvent event) {
        if (event == null || view == View.OVERVIEW || !event.isAllowedChatCharacter() || searchText.length() >= 48) {
            return false;
        }
        String typed = event.codepointAsString();
        if (typed == null || typed.isBlank()) {
            return false;
        }
        searchText += typed.toLowerCase(Locale.ROOT);
        selectedIndex = 0;
        return true;
    }

    private int renderTabs(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, int mouseX, int mouseY) {
        int tabW = Math.max(72, Math.min(120, (w - 18) / View.values().length));
        int cx = x;
        for (View candidate : View.values()) {
            boolean selected = candidate == view;
            TerminalUi.compactButton(context, graphics, cx, y, tabW, 18, candidate.label,
                    descriptor.accentColor(), true, TerminalUi.inside(mouseX, mouseY, cx, y, tabW, 18));
            if (selected) {
                graphics.outline(cx, y, tabW, 18, descriptor.accentColor());
            }
            cx += tabW + 6;
        }
        return y + 18;
    }

    private int renderFilters(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, int mouseX, int mouseY) {
        if (view == View.OVERVIEW) {
            return y;
        }
        int cy = y;
        String search = searchText.isBlank() ? "type to search, esc clears" : searchText;
        cy = line(context, graphics, x, cy, w, "Search", search, searchText.isBlank()
                ? TerminalUi.muted(context) : TerminalUi.success(context));
        if (view == View.DEFINITIONS) {
            cy = renderChipRow(context, graphics, x, cy, w, "Type", typeFilter, typeOptions(),
                    value -> typeFilter = value, mouseX, mouseY);
            cy = renderChipRow(context, graphics, x, cy, w, "Pack", packFilter, packOptions(),
                    value -> packFilter = value, mouseX, mouseY);
        } else if (view == View.DIAGNOSTICS) {
            List<String> severities = List.of("ERROR", "WARNING", "INFO");
            cy = renderChipRow(context, graphics, x, cy, w, "Severity",
                    severityFilter == null ? "" : severityFilter.name(), severities, value ->
                            severityFilter = value.isBlank() ? null : EchoScriptDiagnostic.Severity.valueOf(value),
                    mouseX, mouseY);
        } else if (view == View.ADAPTERS) {
            cy = renderChipRow(context, graphics, x, cy, w, "Adapter", adapterFilter == AdapterFilter.ALL ? "" : adapterFilter.name(),
                    List.of(AdapterFilter.LIVE.name(), AdapterFilter.STUB.name()), value ->
                            adapterFilter = value.isBlank() ? AdapterFilter.ALL : AdapterFilter.valueOf(value),
                    mouseX, mouseY);
        }
        return cy;
    }

    private int renderChipRow(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, String label, String active, List<String> values, java.util.function.Consumer<String> setter,
            int mouseX, int mouseY) {
        int labelW = Math.max(56, Math.min(92, w / 5));
        TerminalUi.line(context, graphics, label.toUpperCase(Locale.ROOT), x, y + 5, labelW, TerminalUi.muted(context));
        int cx = x + labelW + 6;
        cx = chip(context, graphics, cx, y, 42, "all", active == null || active.isBlank(),
                () -> setter.accept(""), mouseX, mouseY);
        int count = 0;
        for (String value : values) {
            if (value == null || value.isBlank() || count++ >= MAX_CHIPS) {
                continue;
            }
            String labelText = value.length() > 18 ? value.substring(0, 18) : value;
            int chipW = Math.max(54, Math.min(128, 18 + labelText.length() * 6));
            if (cx + chipW > x + w) {
                y += FILTER_H;
                cx = x + labelW + 6;
            }
            cx = chip(context, graphics, cx, y, chipW, labelText, value.equals(active),
                    () -> setter.accept(value), mouseX, mouseY);
        }
        return y + FILTER_H;
    }

    private int chip(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, String label, boolean active, Runnable action, int mouseX, int mouseY) {
        boolean hovered = TerminalUi.inside(mouseX, mouseY, x, y, w, 16);
        TerminalUi.compactButton(context, graphics, x, y, w, 16, label, active ? TerminalUi.success(context) : descriptor.accentColor(),
                true, hovered);
        if (active) {
            graphics.outline(x, y, w, 16, TerminalUi.success(context));
        }
        hitboxes.add(new Hitbox(x, y, w, 16, action));
        return x + w + 4;
    }

    private void renderOverview(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, EchoScriptDiagnosticsSummary summary, EchoScriptLoadResult last) {
        int cy = y;
        cy = line(context, graphics, x, cy, w, "Loaded packs", summary.loadedPacks().toString(), TerminalUi.text(context));
        cy = line(context, graphics, x, cy, w, "Counts by type", EchoScriptRegistry.INSTANCE.countByType().toString(), TerminalUi.text(context));
        cy = line(context, graphics, x, cy, w, "Last reload", last.durationMs() + "ms, files="
                + last.loadedFiles().size() + ", failed=" + last.failedCount(), TerminalUi.text(context));
        cy = line(context, graphics, x, cy, w, "Runtime storage",
                (summary.runtimeStorageAvailable() ? "available" : "unavailable") + " / " + summary.runtimeStorageBackend(),
                summary.runtimeStorageAvailable() ? TerminalUi.success(context) : TerminalUi.warning(context));
        cy = line(context, graphics, x, cy, w, "Missing adapters", summary.missingAdapters().toString(), TerminalUi.warning(context));
        cy = line(context, graphics, x, cy, w, "Broken refs", Long.toString(summary.brokenReferences()), TerminalUi.text(context));
        line(context, graphics, x, cy, w, "Branching", "world states, faction reputation, metrics, dialogue choices", TerminalUi.text(context));
    }

    private void renderDefinitions(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, int mouseX, int mouseY) {
        List<EchoScriptDefinitionView> definitions = filteredDefinitions();
        lastListY = y;
        int cy = y;
        for (int i = 0; i < definitions.size(); i++) {
            EchoScriptDefinitionView definition = definitions.get(i);
            boolean selected = i == selectedIndex;
            String source = definition.source().orElseGet(() -> definition.sourceFile().map(Path::toString).orElse("runtime"));
            TerminalUi.dataListRow(context, graphics, x, cy, w, ROW_H - 4,
                    definition.id().toString(),
                    definition.type() + " / " + definition.pack() + " / " + definition.title().orElse("(untitled)"),
                    TerminalUi.trim(context, source, 80),
                    selected,
                    TerminalUi.inside(mouseX, mouseY, x, cy, w, ROW_H - 4),
                    descriptor.accentColor(),
                    selected ? TerminalUi.success(context) : TerminalUi.accent(context));
            cy += ROW_H;
        }
        if (definitions.isEmpty()) {
            TerminalUi.emptyState(context, graphics, x, y, w,
                    "NO MATCHING DEFINITIONS", "Adjust filters or reload JSON files under config/echo/scripts.", descriptor.accentColor());
        }
    }

    private void renderDiagnostics(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, int mouseX, int mouseY, EchoScriptLoadResult last) {
        List<EchoScriptDiagnostic> diagnostics = filteredDiagnostics(last.diagnostics());
        lastListY = y;
        int cy = y;
        for (int i = 0; i < diagnostics.size(); i++) {
            EchoScriptDiagnostic diagnostic = diagnostics.get(i);
            boolean selected = i == selectedIndex;
            int color = switch (diagnostic.severity()) {
                case ERROR -> TerminalUi.danger(context);
                case WARNING -> TerminalUi.warning(context);
                case INFO -> TerminalUi.muted(context);
            };
            String detail = diagnostic.definitionId().map(id -> id + " / ").orElse("")
                    + diagnostic.jsonPath().orElse("$") + " / "
                    + diagnostic.suggestion().orElse(diagnostic.message());
            TerminalUi.dataListRow(context, graphics, x, cy, w, ROW_H - 4,
                    diagnostic.code(), detail, diagnostic.severity().name(), selected,
                    TerminalUi.inside(mouseX, mouseY, x, cy, w, ROW_H - 4),
                    descriptor.accentColor(), color);
            cy += ROW_H;
        }
        if (diagnostics.isEmpty()) {
            TerminalUi.emptyState(context, graphics, x, y, w,
                    "NO MATCHING DIAGNOSTICS", "ScriptCore has no diagnostics for the current filters.", descriptor.accentColor());
        }
    }

    private void renderAdapters(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, int mouseX, int mouseY) {
        List<EchoScriptAdapter> adapters = filteredAdapters();
        lastListY = y;
        int cy = y;
        for (int i = 0; i < adapters.size(); i++) {
            EchoScriptAdapter adapter = adapters.get(i);
            boolean selected = i == selectedIndex;
            boolean available = adapter.isAvailable();
            TerminalUi.dataListRow(context, graphics, x, cy, w, ROW_H - 4,
                    adapter.id().getPath(),
                    "defs=" + adapter.supportedDefinitionTypes().size()
                            + ", actions=" + adapter.supportedActions().size()
                            + ", conditions=" + adapter.supportedConditions().size(),
                    available ? "LIVE" : "STUB",
                    selected,
                    TerminalUi.inside(mouseX, mouseY, x, cy, w, ROW_H - 4),
                    descriptor.accentColor(),
                    available ? TerminalUi.success(context) : TerminalUi.warning(context));
            cy += ROW_H;
        }
    }

    private static int line(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, String label, String value, int valueColor) {
        TerminalUi.line(context, graphics, label.toUpperCase(Locale.ROOT), x, y, Math.max(70, w / 3), TerminalUi.muted(context));
        TerminalUi.line(context, graphics, value, x + Math.max(90, w / 4), y, Math.max(80, w - Math.max(98, w / 4)), valueColor);
        return y + 18;
    }

    private List<EchoScriptDefinitionView> filteredDefinitions() {
        return filterDefinitionsForTests(definitions(), searchText, typeFilter, packFilter);
    }

    private List<EchoScriptDiagnostic> filteredDiagnostics(List<EchoScriptDiagnostic> diagnostics) {
        return filterDiagnosticsForTests(diagnostics, searchText, severityFilter);
    }

    private List<EchoScriptAdapter> filteredAdapters() {
        String normalized = normalize(searchText);
        return EchoScriptAdapterRegistry.INSTANCE.adapters().stream()
                .filter(adapter -> adapterFilter == AdapterFilter.ALL
                        || (adapterFilter == AdapterFilter.LIVE && adapter.isAvailable())
                        || (adapterFilter == AdapterFilter.STUB && !adapter.isAvailable()))
                .filter(adapter -> normalized.isBlank()
                        || adapter.id().toString().toLowerCase(Locale.ROOT).contains(normalized)
                        || String.join(" ", adapter.supportedDefinitionTypes()).toLowerCase(Locale.ROOT).contains(normalized)
                        || String.join(" ", adapter.supportedActions()).toLowerCase(Locale.ROOT).contains(normalized)
                        || String.join(" ", adapter.supportedConditions()).toLowerCase(Locale.ROOT).contains(normalized))
                .sorted(Comparator.comparing(adapter -> adapter.id().toString()))
                .toList();
    }

    public static List<EchoScriptDefinitionView> filterDefinitionsForTests(
            List<EchoScriptDefinitionView> definitions, String search, String type, String pack) {
        String normalizedSearch = normalize(search);
        String normalizedType = normalize(type);
        String normalizedPack = normalize(pack);
        return (definitions == null ? List.<EchoScriptDefinitionView>of() : definitions).stream()
                .filter(definition -> normalizedType.isBlank() || normalize(definition.type()).equals(normalizedType))
                .filter(definition -> normalizedPack.isBlank() || normalize(definition.pack()).equals(normalizedPack))
                .filter(definition -> normalizedSearch.isBlank() || definitionHaystack(definition).contains(normalizedSearch))
                .sorted(Comparator.comparing(EchoScriptDefinitionView::type)
                        .thenComparing(definition -> definition.id().toString()))
                .toList();
    }

    public static List<EchoScriptDiagnostic> filterDiagnosticsForTests(
            List<EchoScriptDiagnostic> diagnostics, String search, EchoScriptDiagnostic.Severity severity) {
        String normalizedSearch = normalize(search);
        return (diagnostics == null ? List.<EchoScriptDiagnostic>of() : diagnostics).stream()
                .filter(diagnostic -> severity == null || diagnostic.severity() == severity)
                .filter(diagnostic -> normalizedSearch.isBlank() || diagnosticHaystack(diagnostic).contains(normalizedSearch))
                .sorted(Comparator.comparing(EchoScriptDiagnostic::severity).thenComparing(EchoScriptDiagnostic::code))
                .limit(100)
                .toList();
    }

    private List<String> typeOptions() {
        return EchoScriptRegistry.INSTANCE.countByType().keySet().stream()
                .sorted()
                .toList();
    }

    private List<String> packOptions() {
        return EchoScriptRegistry.INSTANCE.all().stream()
                .map(EchoScriptDefinitionView::pack)
                .filter(pack -> pack != null && !pack.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .sorted()
                .toList();
    }

    private boolean clearFilters() {
        boolean hadFilters = !packFilter.isBlank() || !typeFilter.isBlank()
                || severityFilter != null || adapterFilter != AdapterFilter.ALL;
        packFilter = "";
        typeFilter = "";
        severityFilter = null;
        adapterFilter = AdapterFilter.ALL;
        return hadFilters;
    }

    private void clampSelection() {
        int size = switch (view) {
            case OVERVIEW -> 1;
            case DEFINITIONS -> filteredDefinitions().size();
            case DIAGNOSTICS -> filteredDiagnostics(EchoScriptCoreApi.get().lastResult().diagnostics()).size();
            case ADAPTERS -> filteredAdapters().size();
        };
        selectedIndex = Math.max(0, Math.min(Math.max(0, size - 1), selectedIndex));
    }

    private static List<EchoScriptDefinitionView> definitions() {
        return EchoScriptRegistry.INSTANCE.all().stream()
                .sorted(Comparator.comparing(EchoScriptDefinitionView::type)
                        .thenComparing(definition -> definition.id().toString()))
                .toList();
    }

    private static String definitionHaystack(EchoScriptDefinitionView definition) {
        if (definition == null) {
            return "";
        }
        return normalize(String.join(" ",
                definition.id().toString(),
                definition.type(),
                definition.pack(),
                definition.title().orElse(""),
                definition.description().orElse(""),
                definition.source().orElse(""),
                definition.sourceFile().map(Path::toString).orElse(""),
                String.join(" ", definition.tags())));
    }

    private static String diagnosticHaystack(EchoScriptDiagnostic diagnostic) {
        if (diagnostic == null) {
            return "";
        }
        return normalize(String.join(" ",
                diagnostic.severity().name(),
                diagnostic.code(),
                diagnostic.message(),
                diagnostic.definitionId().map(Identifier::toString).orElse(""),
                diagnostic.file().map(Path::toString).orElse(""),
                diagnostic.jsonPath().orElse(""),
                diagnostic.suggestion().orElse("")));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }

    private enum View {
        OVERVIEW("Overview"),
        DEFINITIONS("Definitions"),
        DIAGNOSTICS("Diagnostics"),
        ADAPTERS("Adapters");

        private final String label;

        View(String label) {
            this.label = label;
        }
    }

    private enum AdapterFilter {
        ALL,
        LIVE,
        STUB
    }

    private record Hitbox(int x, int y, int w, int h, Runnable action) {
    }
}
