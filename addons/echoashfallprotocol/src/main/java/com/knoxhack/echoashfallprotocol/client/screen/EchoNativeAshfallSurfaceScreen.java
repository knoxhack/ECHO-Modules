package com.knoxhack.echoashfallprotocol.client.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class EchoNativeAshfallSurfaceScreen extends Screen {
    private static final int BG = 0xFF04080D;
    private static final int PANEL = 0xD0091420;
    private static final int PANEL_SOFT = 0xA8122532;
    private static final int LINE = 0xCC38DFF4;
    private static final int LINE_DIM = 0x6638DFF4;
    private static final int TEXT = 0xFFE8F8FF;
    private static final int MUTED = 0xFF8CA2AE;
    private static final int CYAN = 0xFF66E8FF;
    private static final int GREEN = 0xFF7CFFB2;
    private static final int AMBER = 0xFFFFC857;
    private static final int RED = 0xFFFF5C5C;
    private static final List<String> SURFACES = List.of("TERMINAL", "INDEX", "LENS", "HOLOMAP", "WIKI");

    private final String mode;
    private int ticks;
    private boolean renderCallbackExecuted;
    private int renderCallbackCount;
    private String renderCallbackMode = "";
    private int renderCallbackLineCount;
    private int renderCallbackWidth;
    private int renderCallbackHeight;

    public EchoNativeAshfallSurfaceScreen(String surface) {
        super(Component.literal("ECHO Native " + normalize(surface)));
        this.mode = normalize(surface);
    }

    @Override
    protected void init() {
        int top = 18;
        int left = margin();
        int gap = 4;
        int buttonWidth = Math.max(62, Math.min(92, (this.width - margin() * 2 - gap * SURFACES.size()) / 6));
        int x = left;
        for (String surface : SURFACES) {
            this.addRenderableWidget(Button.builder(Component.literal(label(surface)),
                            button -> this.minecraft.setScreen(new EchoNativeAshfallSurfaceScreen(surface)))
                    .bounds(x, top, buttonWidth, 18)
                    .build());
            x += buttonWidth + gap;
        }
        int backWidth = Math.max(58, Math.min(82, this.width - x - margin()));
        if (backWidth >= 48) {
            this.addRenderableWidget(Button.builder(Component.literal("BACK"),
                            button -> this.minecraft.setScreen(null))
                    .bounds(this.width - margin() - backWidth, top, backWidth, 18)
                    .build());
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.ticks++;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, BG);
        int sweep = this.width <= 0 ? 0 : (this.ticks * 4) % Math.max(1, this.width);
        graphics.fill(Math.max(0, sweep - 96), 0, Math.min(this.width, sweep), this.height, 0x1119B7D4);
        for (int y = 46 + (this.ticks % 16); y < this.height; y += 16) {
            graphics.fill(0, y, this.width, y + 1, 0x1428D7F4);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        List<String> body = bodyLines();
        this.renderCallbackExecuted = true;
        this.renderCallbackCount++;
        this.renderCallbackMode = this.mode;
        this.renderCallbackLineCount = body.size();
        this.renderCallbackWidth = this.width;
        this.renderCallbackHeight = this.height;

        int margin = margin();
        int top = 46;
        int bottom = Math.max(top + 80, this.height - margin());
        int left = margin;
        int right = Math.max(left + 120, this.width - margin);
        graphics.fill(left, top, right, bottom, PANEL);
        graphics.outline(left, top, right - left, bottom - top, LINE);
        graphics.fill(left + 1, top + 1, right - 1, top + 26, PANEL_SOFT);
        graphics.fill(left + 12, top + 28, right - 12, top + 29, pulseColor());

        Font font = this.font;
        String title = "ASHFALL " + label(this.mode) + " // NATIVE MODULE SURFACE";
        text(graphics, font, clip(font, title, right - left - 28), left + 14, top + 9, CYAN);
        text(graphics, font, platformSurfaceLine(), left + 14, top + 36, MUTED);
        text(graphics, font, playerLine(), left + 14, top + 50, GREEN);

        int y = top + 76;
        int maxRows = Math.max(1, (bottom - y - 18) / 12);
        for (int index = 0; index < Math.min(maxRows, body.size()); index++) {
            int color = colorForLine(body.get(index), index);
            text(graphics, font, clip(font, body.get(index), right - left - 28), left + 14, y + index * 12, color);
        }
        if (body.size() > maxRows) {
            text(graphics, font, "... " + (body.size() - maxRows) + " more rows", left + 14,
                    y + maxRows * 12 + 2, AMBER);
        }
        String footer = "M Terminal  G Index  Hold Left Alt Lens  J HoloMap  K Minimap  Esc Close";
        text(graphics, font, clip(font, footer, right - left - 28), left + 14, bottom - 16, LINE);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private Map<String, Object> surfaceState() {
        Map<String, Object> state = new LinkedHashMap<>();
        Map<String, Object> dataSource = dataSource(this.mode);
        state.put("mode", this.mode);
        state.put("surface", this.mode);
        state.put("renderCallbackExecuted", this.renderCallbackExecuted);
        state.put("renderCallbackCount", this.renderCallbackCount);
        state.put("renderCallbackMode", this.renderCallbackMode);
        state.put("renderCallbackLineCount", this.renderCallbackLineCount);
        state.put("renderCallbackWidth", this.renderCallbackWidth);
        state.put("renderCallbackHeight", this.renderCallbackHeight);
        state.put("moduleCount", modules().size());
        state.put("nativeDataRecordCount", number(dataSource.get("recordCount")));
        state.put("nativeDataSourcePath", textValue(dataSource, "sourcePath", ""));
        state.put("moduleClasspath", System.getProperty("echo.native.moduleClasspath", ""));
        return Map.copyOf(state);
    }

    private List<String> bodyLines() {
        List<ModuleInfo> modules = modules();
        Map<String, Object> dataSources = dataSources();
        List<String> lines = new ArrayList<>();
        switch (this.mode) {
            case "TERMINAL" -> terminalLines(lines, modules, map(dataSources.get("terminal")));
            case "INDEX" -> indexLines(lines, modules, map(dataSources.get("index")));
            case "LENS" -> lensLines(lines, modules, map(dataSources.get("lens")));
            case "HOLOMAP" -> holomapLines(lines, modules, map(dataSources.get("holomap")));
            case "WIKI" -> wikiLines(lines, modules, map(dataSources.get("wiki")));
            case "LOADING" -> loadingLines(lines, modules);
            default -> terminalLines(lines, modules, map(dataSources.get("terminal")));
        }
        return List.copyOf(lines);
    }

    private void terminalLines(List<String> lines, List<ModuleInfo> modules, Map<String, Object> terminal) {
        lines.add(textValue(terminal, "prompt", "ASHFALL>") + " status");
        lines.add(textValue(terminal, "title", "Native Terminal"));
        lines.add(textValue(terminal, "summary", "Native route page loaded"));
        lines.add("source: " + textValue(terminal, "sourcePath", "native source pending"));
        lines.add("pages: " + objects(terminal.get("pages")).size() + "  staged-modules: " + modules.size());
        lines.add(textValue(terminal, "readyLine", "Native terminal ready"));
        lines.add("");
        for (Map<String, Object> panel : objects(terminal.get("panels")).stream().limit(6).toList()) {
            lines.add("[" + textValue(panel, "state", "panel") + "] "
                    + textValue(panel, "title", textValue(panel, "id", "panel"))
                    + " :: " + textValue(panel, "body", ""));
        }
        if (!strings(terminal.get("nextSteps")).isEmpty()) {
            lines.add("");
            for (String step : strings(terminal.get("nextSteps")).stream().limit(5).toList()) {
                lines.add("> " + step);
            }
        }
        if (objects(terminal.get("pages")).isEmpty()) {
            productDataUnavailable(lines, "Terminal pages", "terminal.defaultRoute",
                    "data/echoashfallprotocol/echoterminal/pages");
        }
    }

    private void indexLines(List<String> lines, List<ModuleInfo> modules, Map<String, Object> index) {
        lines.add(textValue(index, "title", "Native Field Index"));
        lines.add(textValue(index, "summary", "Index entry loaded"));
        lines.add("query: " + textValue(index, "query", "ashfall") + "  entries: " + objects(index.get("entries")).size());
        lines.add("source: " + textValue(index, "sourcePath", "native source pending"));
        lines.add("");
        for (Map<String, Object> entry : objects(index.get("entries")).stream().limit(14).toList()) {
            String title = textValue(entry, "title", textValue(entry, "id", "index entry"));
            String subtitle = textValue(entry, "subtitle", textValue(entry, "category", ""));
            String summary = textValue(entry, "summary", "");
            lines.add(pad(title, 28) + pad(subtitle, 24) + summary);
        }
        if (objects(index.get("entries")).isEmpty()) {
            productDataUnavailable(lines, "Index entries", "indexEntry",
                    "data/echoashfallprotocol/echoindex/entries");
        }
    }

    private void lensLines(List<String> lines, List<ModuleInfo> modules, Map<String, Object> lens) {
        lines.add(textValue(lens, "title", "Portable Signal Scanner"));
        lines.add(textValue(lens, "result", textValue(lens, "summary", "Native lens profile loaded")));
        lines.add("source: " + textValue(lens, "sourcePath", "native source pending"));
        lines.add("target: " + targetLine());
        lines.add("scan-context: " + playerLine());
        lines.add("profiles: " + objects(lens.get("profiles")).size() + "  rows: " + objects(lens.get("rows")).size());
        lines.add("");
        for (Map<String, Object> row : objects(lens.get("rows")).stream().limit(12).toList()) {
            lines.add(textValue(row, "riskLabel", "scan") + " / "
                    + textValue(row, "target", "target")
                    + " -> " + textValue(row, "text", textValue(row, "indexEntry", "")));
        }
        if (objects(lens.get("rows")).isEmpty()) {
            productDataUnavailable(lines, "Lens scan rows", "lensProfile",
                    "data/echoashfallprotocol/echolens/scan_profiles");
        }
    }

    private void holomapLines(List<String> lines, List<ModuleInfo> modules, Map<String, Object> holomap) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        lines.add(textValue(holomap, "layerName", "HoloMap projection"));
        lines.add(textValue(holomap, "focus", textValue(holomap, "description", "Native client route surface")));
        lines.add("source: " + textValue(holomap, "sourcePath", "native source pending"));
        if (player != null && minecraft.level != null) {
            BlockPos pos = player.blockPosition();
            lines.add("anchor: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
            lines.add("dimension: " + minecraft.level.dimension().identifier());
            lines.add("biome: " + biomeLine(minecraft, pos));
        } else {
            lines.add("anchor: no player in active world");
        }
        lines.add("");
        lines.add("markers: " + objects(holomap.get("markers")).size()
                + "  layers: " + objects(holomap.get("layers")).size());
        for (Map<String, Object> marker : objects(holomap.get("markers")).stream().limit(12).toList()) {
            lines.add(" - " + textValue(marker, "label", textValue(marker, "id", "marker"))
                    + " [" + textValue(marker, "riskLabel", textValue(marker, "kind", "route")) + "] "
                    + textValue(marker, "description", ""));
        }
        if (objects(holomap.get("markers")).isEmpty()) {
            productDataUnavailable(lines, "HoloMap markers", "holomapLayer",
                    "data/echoashfallprotocol/echoholomap/layers");
        }
    }

    private void wikiLines(List<String> lines, List<ModuleInfo> modules, Map<String, Object> wiki) {
        lines.add(textValue(wiki, "page", "Ashfall Native Loader module wiki"));
        lines.add(textValue(wiki, "summary", "Wiki article loaded"));
        lines.add("source: " + textValue(wiki, "sourcePath", "native source pending"));
        lines.add("articles: " + objects(wiki.get("articles")).size()
                + "  blocks: " + objects(wiki.get("blocks")).size());
        lines.add("");
        for (Map<String, Object> article : objects(wiki.get("articles")).stream().limit(12).toList()) {
            lines.add(textValue(article, "title", textValue(article, "id", "article"))
                    + " :: " + textValue(article, "summary", ""));
        }
        if (objects(wiki.get("articles")).isEmpty()) {
            productDataUnavailable(lines, "Wiki articles", "wiki.defaultPage",
                    "data/echoashfallprotocol/echowiki/articles");
        }
    }

    private void productDataUnavailable(List<String> lines, String contentName, String profileKey, String expectedPath) {
        lines.add("product-data unavailable: " + contentName);
        lines.add("expected native profile key: " + profileKey);
        lines.add("expected product data path: " + expectedPath);
        lines.add("Native Loader did not mount live product content for this surface.");
        lines.add("This screen is showing a product data fault, not descriptor fallback output.");
    }

    private void loadingLines(List<String> lines, List<ModuleInfo> modules) {
        lines.add("ECHO Native Loader resource handoff");
        lines.add("profile: Ashfall Protocol");
        lines.add("module jars mounted: " + modules.size());
        lines.add("screen projection: product-owned");
        lines.add("hud projection: profile renderer");
        lines.add("next: Terminal / Lens / HoloMap native routes");
    }

    private String platformSurfaceLine() {
        String screenId = platformValue("nativeUiScreenIdForSurface", this.mode, "native_ui:" + this.mode.toLowerCase(Locale.ROOT));
        String target = platformValue("nativeUiTargetForSurface", this.mode, screenId);
        return "surface-id: " + screenId + "  target: " + target;
    }

    private static String platformValue(String methodName, String surface, String fallback) {
        try {
            Class<?> type = Class.forName("dev.echo.nativeplatform.bootstrap.EchoNativeBootstrapMain");
            Method method = type.getDeclaredMethod(methodName, String.class);
            method.setAccessible(true);
            Object value = method.invoke(null, surface);
            String text = value == null ? "" : String.valueOf(value);
            return text.isBlank() ? fallback : text;
        } catch (ReflectiveOperationException exception) {
            return fallback;
        }
    }

    private static Map<String, Object> dataSource(String surface) {
        String key = switch (normalize(surface)) {
            case "INDEX" -> "index";
            case "LENS" -> "lens";
            case "HOLOMAP" -> "holomap";
            case "WIKI" -> "wiki";
            default -> "terminal";
        };
        return map(dataSources().get(key));
    }

    private static Map<String, Object> dataSources() {
        try {
            Class<?> registry = Class.forName("dev.echo.nativeplatform.bootstrap.EchoNativeAgent5UiHandlerRegistry");
            Method method = registry.getMethod("dataSources");
            return map(method.invoke(null));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> source) {
            Map<String, Object> copy = new LinkedHashMap<>();
            source.forEach((key, entry) -> copy.put(String.valueOf(key), entry));
            return Map.copyOf(copy);
        }
        return Map.of();
    }

    private static List<Map<String, Object>> objects(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object row : iterable) {
            Map<String, Object> mapped = map(row);
            if (!mapped.isEmpty()) {
                rows.add(mapped);
            }
        }
        return List.copyOf(rows);
    }

    private static List<String> strings(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<String> rows = new ArrayList<>();
        for (Object row : iterable) {
            String text = row == null ? "" : String.valueOf(row);
            if (!text.isBlank()) {
                rows.add(text);
            }
        }
        return List.copyOf(rows);
    }

    private static String textValue(Map<String, Object> source, String key, String fallback) {
        Object value = source.get(key);
        String text = value == null ? "" : String.valueOf(value);
        return text.isBlank() ? fallback : text;
    }

    private static int number(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? 0 : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static String playerLine() {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            return "player: no active client player";
        }
        BlockPos pos = player.blockPosition();
        return "player: " + player.getName().getString() + " @ " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    private static String targetLine() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.hitResult instanceof BlockHitResult blockHit
                && blockHit.getType() == HitResult.Type.BLOCK
                && minecraft.level != null) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = minecraft.level.getBlockState(pos);
            return state.getBlock() + " @ " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
        }
        return "no block target";
    }

    private static String biomeLine(Minecraft minecraft, BlockPos pos) {
        try {
            return minecraft.level.getBiome(pos).unwrapKey()
                    .map(key -> key.identifier().toString())
                    .orElse("unknown");
        } catch (RuntimeException exception) {
            return "unknown";
        }
    }

    private static List<ModuleInfo> modules() {
        String classpath = System.getProperty("echo.native.moduleClasspath", "");
        if (classpath.isBlank()) {
            return List.of();
        }
        List<ModuleInfo> modules = new ArrayList<>();
        for (String entry : classpath.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            Path path = Path.of(entry);
            if (Files.isRegularFile(path)) {
                modules.add(readModule(path));
            }
        }
        modules.sort(Comparator.comparing(module -> module.id));
        return List.copyOf(modules);
    }

    private static ModuleInfo readModule(Path path) {
        String fileName = path.getFileName() == null ? path.toString() : path.getFileName().toString();
        try (ZipFile zip = new ZipFile(path.toFile())) {
            ZipEntry descriptor = zip.getEntry("META-INF/echo.mod.json");
            if (descriptor == null) {
                return new ModuleInfo(stripJar(fileName), stripJar(fileName), "", false, fileName);
            }
            try (InputStreamReader reader = new InputStreamReader(zip.getInputStream(descriptor), StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                JsonObject access = object(json.get("access"));
                String id = string(json, "id", stripJar(fileName));
                String name = string(json, "name", id);
                String entrypoint = string(access, "nativeEntrypoint", "");
                boolean classpath = hasClasspath(access.get("nativeClasspath"));
                return new ModuleInfo(id, name, entrypoint, classpath, fileName);
            }
        } catch (RuntimeException | java.io.IOException exception) {
            return new ModuleInfo(stripJar(fileName), stripJar(fileName), "", false, fileName);
        }
    }

    private static JsonObject object(JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
    }

    private static String string(JsonObject object, String key, String fallback) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        try {
            String value = element.getAsString();
            return value == null || value.isBlank() ? fallback : value;
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static boolean hasClasspath(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return false;
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            return !array.isEmpty();
        }
        try {
            return !element.getAsString().isBlank();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static String stripJar(String fileName) {
        return fileName.endsWith(".jar") ? fileName.substring(0, fileName.length() - 4) : fileName;
    }

    private static String normalize(String surface) {
        String value = surface == null ? "" : surface.trim().toUpperCase(Locale.ROOT);
        return value.isBlank() ? "TERMINAL" : value;
    }

    private static String label(String surface) {
        return switch (normalize(surface)) {
            case "HOLOMAP" -> "HoloMap";
            case "MAIN_MENU" -> "Main Menu";
            default -> normalize(surface).charAt(0) + normalize(surface).substring(1).toLowerCase(Locale.ROOT);
        };
    }

    private int margin() {
        return Math.max(12, Math.min(30, this.width / 32));
    }

    private int colorForLine(String line, int index) {
        if (line.startsWith(">") || line.startsWith("$")) {
            return GREEN;
        }
        if (line.contains("missing") || line.contains("pending") || line.contains("no ")) {
            return AMBER;
        }
        if (line.contains("failed")) {
            return RED;
        }
        return index % 3 == 0 ? TEXT : index % 3 == 1 ? CYAN : MUTED;
    }

    private static void text(GuiGraphicsExtractor graphics, Font font, String value, int x, int y, int color) {
        graphics.text(font, value, x, y, color, false);
    }

    private static String clip(Font font, String value, int width) {
        if (font.width(value) <= width) {
            return value;
        }
        String suffix = "...";
        int limit = Math.max(1, value.length() - 1);
        while (limit > 1 && font.width(value.substring(0, limit) + suffix) > width) {
            limit--;
        }
        return value.substring(0, limit) + suffix;
    }

    private static String pad(String value, int width) {
        String clipped = value.length() > width - 1 ? value.substring(0, Math.max(1, width - 2)) + "." : value;
        return String.format(Locale.ROOT, "%-" + width + "s", clipped);
    }

    private static int pulseColor() {
        int phase = (int) (System.currentTimeMillis() / 50L % 48L);
        int alpha = 80 + Math.round(80.0F * (phase / 47.0F));
        return (alpha << 24) | 0x38DFF4;
    }

    private record ModuleInfo(
            String id,
            String name,
            String nativeEntrypoint,
            boolean nativeClasspath,
            String fileName
    ) {
        boolean hasNativeEntrypoint() {
            return nativeEntrypoint != null && !nativeEntrypoint.isBlank();
        }

        String entrypointStatus() {
            return hasNativeEntrypoint() ? shortEntrypoint() : "missing nativeEntrypoint";
        }

        String classpathStatus() {
            return nativeClasspath ? "nativeClasspath" : "classpath pending";
        }

        String shortEntrypoint() {
            if (!hasNativeEntrypoint()) {
                return "none";
            }
            int dot = nativeEntrypoint.lastIndexOf('.');
            return dot < 0 ? nativeEntrypoint : nativeEntrypoint.substring(dot + 1);
        }
    }
}
