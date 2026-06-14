package com.knoxhack.echoashfallprotocol.client.screen;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntSupplier;

import dev.echo.nativeplatform.loader.NativeLoaderClasspathSupport;
import dev.echo.nativeplatform.loader.NativeLoaderAshfallWorldStartupService;
import dev.echo.nativeplatform.loader.NativeLoaderAshfallWorldStartupService.StartupPlan;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Native Loader safe Ashfall title screen. It intentionally avoids legacy runtime-only
 * client APIs so the vanilla Native Loader handoff can instantiate it directly.
 */
public final class EchoNativeMainMenuScreen extends Screen {
    private static final int BG = 0xFF050910;
    private static final int PANEL = 0xD00B1824;
    private static final int PANEL_SOFT = 0xB0102430;
    private static final int LINE = 0xFF38DFF4;
    private static final int LINE_DIM = 0x8838DFF4;
    private static final int CYAN = 0xFF66E8FF;
    private static final int CYAN_DIM = 0xFF7AAFC0;
    private static final int GREEN = 0xFF7CFFB2;
    private static final int AMBER = 0xFFFFC857;
    private static final int TEXT = 0xFFE8F8FF;
    private static final int MUTED = 0xFF8CA2AE;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_COUNT = 5;
    private static final AtomicBoolean PRODUCT_WORLD_AUTO_OPEN_ATTEMPTED = new AtomicBoolean(false);
    private int ticks;
    private boolean productWorldAutoOpenTriggered;

    public EchoNativeMainMenuScreen() {
        super(Component.literal("ECHO Native Ashfall"));
    }

    @Override
    protected void init() {
        int menuWidth = commandWidth();
        int menuX = commandX(menuWidth);
        int menuY = commandY();
        int gap = commandButtonGap();
        int buttonY = menuY + 48;
        this.addRenderableWidget(nativeButton("[ ASHFALL WORLD ]",
                button -> openOrCreateProductWorld(),
                menuX + 18, buttonY, menuWidth - 36));
        this.addRenderableWidget(nativeButton("[ MULTIPLAYER ]",
                button -> this.minecraft.setScreen(NativeRouteScreen.multiplayer(this)),
                menuX + 18, buttonY + gap, menuWidth - 36));
        this.addRenderableWidget(nativeButton("[ MODULE INDEX ]",
                button -> this.minecraft.setScreen(new ModuleIndexScreen(this)),
                menuX + 18, buttonY + gap * 2, menuWidth - 36));
        this.addRenderableWidget(nativeButton("[ OPTIONS ]",
                button -> this.minecraft.setScreen(NativeRouteScreen.options(this)),
                menuX + 18, buttonY + gap * 3, menuWidth - 36));
        this.addRenderableWidget(nativeButton("[ QUIT ]",
                button -> this.minecraft.stop(),
                menuX + 18, buttonY + gap * 4, menuWidth - 36));
    }

    @Override
    public void tick() {
        super.tick();
        this.ticks++;
        if (!this.productWorldAutoOpenTriggered
                && productWorldAutoOpen()
                && this.ticks > 8
                && PRODUCT_WORLD_AUTO_OPEN_ATTEMPTED.compareAndSet(false, true)) {
            this.productWorldAutoOpenTriggered = true;
            openOrCreateProductWorld();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        renderBackgroundPlate(graphics);
        renderArchivePanel(graphics);
        renderCommandPanel(graphics);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        renderFooter(graphics);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void renderBackgroundPlate(GuiGraphicsExtractor graphics) {
        graphics.fill(0, 0, this.width, this.height, BG);
        graphics.fill(0, 0, this.width, Math.max(1, this.height / 5), 0x660B1530);
        graphics.fill(0, Math.max(0, this.height - this.height / 3), this.width, this.height, 0x80000008);
        int sweep = this.width <= 0 ? 0 : (this.ticks * 3) % Math.max(1, this.width);
        graphics.fill(Math.max(0, sweep - 80), 0, Math.min(this.width, sweep), this.height, 0x101AB8D0);
        for (int y = 12 + (this.ticks % 18); y < this.height; y += 18) {
            graphics.fill(0, y, this.width, y + 1, 0x1828D7F4);
        }
    }

    private void renderArchivePanel(GuiGraphicsExtractor graphics) {
        boolean compact = compactLayout();
        int margin = margin();
        int commandWidth = commandWidth();
        int commandX = commandX(commandWidth);
        int left = margin;
        int top = compact ? 24 : 34;
        int right = compact ? this.width - margin : commandX - margin;
        int bottom = compact ? Math.min(this.height - margin, commandY() - 10) : this.height - 42;
        if (bottom - top < 88) {
            if (compact) {
                return;
            }
            bottom = Math.min(this.height - margin, top + 88);
        }
        graphics.fill(left, top, right, bottom, 0xBA071019);
        graphics.outline(left, top, right - left, bottom - top, LINE);
        graphics.fill(left + 1, top + 1, right - 1, top + 21, 0x8C19013A);
        graphics.fill(left + 12, top + 17, left + Math.min(180, right - left - 12), top + 18,
                pulseColor(0x5538DFF4, 0xBB66E8FF, 42));

        int contentWidth = Math.max(120, right - left - 28);
        boolean cursor = ((this.ticks / 18) % 2) == 0;
        pixelText(graphics, "ECHO", left + 14, top + 28, CYAN, compact ? 2 : 3);
        text(graphics, "NATIVE LOADER // ASHFALL" + (cursor ? "_" : ""), left + 14, top + 7, CYAN);
        text(graphics, "MODULE PROFILE: echoashfallprotocol", left + 14, top + (compact ? 51 : 61), TEXT);
        text(graphics, "VANILLA HANDOFF: ACTIVE", left + 14, top + (compact ? 65 : 75), GREEN);

        int y = top + (compact ? 88 : 106);
        drawSection(graphics, left + 14, y, "RUNTIME STATUS", contentWidth);
        y += 18;
        drawStatus(graphics, left + 18, y, "Client", "ECHO Native Loader Client", GREEN, contentWidth);
        y += 15;
        drawStatus(graphics, left + 18, y, "Profile", "Ashfall Protocol native product profile", CYAN, contentWidth);
        y += 15;
        drawStatus(graphics, left + 18, y, "World", NativeLoaderAshfallWorldStartupService.configuredProductWorldFolder(), AMBER,
                contentWidth);
        y += 15;
        drawStatus(graphics, left + 18, y, "Modules", moduleSummary(), AMBER, contentWidth);

        if (!compact && y + 64 < bottom) {
            y += 30;
            drawSection(graphics, left + 14, y, "ASHFALL SIGNAL", contentWidth);
            y += 18;
            drawWrapped(graphics,
                    "The native client is now projecting an Ashfall-owned screen from the addon jar, not a generated bootstrap dashboard.",
                    left + 18, y, contentWidth - 8, 4, TEXT);
        }
    }

    private void renderCommandPanel(GuiGraphicsExtractor graphics) {
        int width = commandWidth();
        int left = commandX(width);
        int top = commandY();
        int height = commandHeight();
        int bottom = Math.min(this.height - margin(), top + height);
        graphics.fill(left, top, left + width, bottom, PANEL);
        graphics.outline(left, top, width, bottom - top, LINE);
        graphics.fill(left + 1, top + 1, left + width - 1, top + 29, 0x8620024A);
        graphics.fill(left + 12, top + 30, left + width - 12, top + 31, pulseColor(0x5038DFF4, 0xB466E8FF, 48));
        text(graphics, "ECHO BUS", left + 18, top + 10, CYAN);
        text(graphics, "SELECT BOOT SIGNAL", left + 18, top + 24, MUTED);
        int meterTop = bottom - 32;
        if (meterTop > top + 160) {
            text(graphics, "NATIVE", left + 18, meterTop, CYAN_DIM);
            int meterLeft = left + 74;
            int meterRight = left + width - 18;
            graphics.outline(meterLeft, meterTop - 2, meterRight - meterLeft, 8, LINE_DIM);
            int pulseWidth = 18 + (this.ticks % Math.max(22, meterRight - meterLeft - 18));
            graphics.fill(meterLeft + 2, meterTop, Math.min(meterRight - 2, meterLeft + pulseWidth), meterTop + 4, 0xB766E8FF);
        }
    }

    private void renderFooter(GuiGraphicsExtractor graphics) {
        int y = this.height - 26;
        if (y < 8) {
            return;
        }
        text(graphics, "Runtime " + SharedConstants.getCurrentVersion().name(), 22, y, MUTED);
        String right = "Native profile screen: Ashfall";
        int rightWidth = this.font.width(right);
        text(graphics, right, Math.max(22, this.width - rightWidth - 22), y, CYAN_DIM);
    }

    private NativeButton nativeButton(String label, Button.OnPress action, int x, int y, int width) {
        return new NativeButton(x, y, width, BUTTON_HEIGHT, label, action, () -> this.ticks);
    }

    private void openOrCreateProductWorld() {
        Minecraft client = this.minecraft == null ? Minecraft.getInstance() : this.minecraft;
        EchoNativeAshfallWorldOpenDispatcher.openOrCreateProductWorldFromNativeLoader(client, this);
    }

    public static Screen productStartupFailureScreen(Screen parent, StartupPlan plan) {
        return new ProductWorldStartupFailureScreen(
                parent,
                productStartupFailureTitle(plan),
                plan.failureLines());
    }

    public static Screen productStartupFailureScreen(Screen parent, String title, List<String> lines) {
        return new ProductWorldStartupFailureScreen(parent, title, lines);
    }

    private static String productStartupFailureTitle(StartupPlan plan) {
        return switch (plan.failureKind()) {
            case "old_vanilla_save_guard" -> "WORLD FOLDER IS NOT ASHFALL";
            case "missing_product_datapack" -> "ASHFALL DATAPACK OFFLINE";
            default -> "ASHFALL STARTUP BLOCKED";
        };
    }

    private static boolean productWorldAutoOpen() {
        return NativeLoaderAshfallWorldStartupService.productWorldAutoOpen();
    }

    private void drawSection(GuiGraphicsExtractor graphics, int x, int y, String label, int width) {
        text(graphics, ":: " + label, x, y, CYAN);
        graphics.fill(x, y + 11, x + Math.max(40, Math.min(width, 230)), y + 12, LINE_DIM);
    }

    private void drawStatus(GuiGraphicsExtractor graphics, int x, int y, String label, String value, int color, int width) {
        int labelWidth = compactLayout() ? 76 : 88;
        text(graphics, label.toUpperCase() + ":", x, y, CYAN_DIM);
        text(graphics, clipToWidth(value, Math.max(40, width - labelWidth - 6)), x + labelWidth, y, color);
    }

    private int drawWrapped(GuiGraphicsExtractor graphics, String value, int x, int y, int width, int maxLines, int color) {
        String remaining = value.trim();
        int line = 0;
        while (!remaining.isEmpty() && line < maxLines) {
            String next = takeLine(remaining, width);
            text(graphics, next, x, y + line * 11, color);
            remaining = remaining.substring(next.length()).trim();
            line++;
        }
        return y + line * 11;
    }

    private String takeLine(String value, int width) {
        if (this.font.width(value) <= width) {
            return value;
        }
        String[] words = value.split(" ");
        String line = "";
        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (this.font.width(candidate) > width) {
                return line.isEmpty() ? clipToWidth(word, width) : line;
            }
            line = candidate;
        }
        return line;
    }

    private String clipToWidth(String value, int width) {
        if (this.font.width(value) <= width) {
            return value;
        }
        String suffix = "...";
        int limit = Math.max(1, value.length() - 1);
        while (limit > 1 && this.font.width(value.substring(0, limit) + suffix) > width) {
            limit--;
        }
        return value.substring(0, limit) + suffix;
    }

    private String moduleSummary() {
        int count = nativeModuleNames().size();
        return count <= 0 ? "module classpath pending" : count + " staged native module jars";
    }

    private static List<String> nativeModuleNames() {
        return NativeLoaderClasspathSupport.nativeModuleClasspathEntries("echo.native.moduleClasspath").stream()
                .map(entry -> {
                    String normalized = entry.replace('\\', '/');
                    int slash = normalized.lastIndexOf('/');
                    return slash >= 0 ? normalized.substring(slash + 1) : normalized;
                })
                .filter(name -> !name.isBlank())
                .toList();
    }

    private void text(GuiGraphicsExtractor graphics, String value, int x, int y, int color) {
        graphics.text(this.font, value, x, y, color, false);
    }

    private void pixelText(GuiGraphicsExtractor graphics, String value, int x, int y, int color, int scale) {
        for (int index = 0; index < value.length(); index++) {
            text(graphics, String.valueOf(value.charAt(index)), x + index * 7 * scale, y, color);
        }
    }

    private boolean compactLayout() {
        return this.width < 640 || this.height < 330;
    }

    private int margin() {
        return clamp(this.width / 32, 12, 34);
    }

    private int commandWidth() {
        return clamp(this.width / 3, 196, 244);
    }

    private int commandHeight() {
        int desired = compactLayout() ? 190 : 220;
        int minimum = 48 + commandButtonGap() * (BUTTON_COUNT - 1) + BUTTON_HEIGHT + 12;
        int available = Math.max(minimum, this.height - margin() * 2);
        return clamp(desired, minimum, available);
    }

    private int commandX(int commandWidth) {
        if (compactLayout()) {
            return Math.max(margin(), (this.width - commandWidth) / 2);
        }
        return Math.max(margin(), this.width - commandWidth - margin());
    }

    private int commandY() {
        int desired = compactLayout() ? this.height / 2 - 38 : this.height / 2 - 108;
        int minTop = margin();
        int maxTop = Math.max(minTop, this.height - commandHeight() - margin());
        return clamp(desired, minTop, maxTop);
    }

    private int commandButtonGap() {
        int available = Math.max(0, this.height - margin() * 2 - 48 - BUTTON_HEIGHT - 12);
        return clamp(available / Math.max(1, BUTTON_COUNT - 1), 18, compactLayout() ? 23 : 25);
    }

    private static int pulseColor(int low, int high, int period) {
        int phase = (int) (System.currentTimeMillis() / 50L % Math.max(1, period));
        float t = phase / (float) Math.max(1, period - 1);
        int alpha = (int) (((low >>> 24) & 0xFF) * (1.0F - t) + ((high >>> 24) & 0xFF) * t);
        int red = (int) (((low >>> 16) & 0xFF) * (1.0F - t) + ((high >>> 16) & 0xFF) * t);
        int green = (int) (((low >>> 8) & 0xFF) * (1.0F - t) + ((high >>> 8) & 0xFF) * t);
        int blue = (int) ((low & 0xFF) * (1.0F - t) + (high & 0xFF) * t);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class NativeButton extends Button {
        private final String label;
        private final IntSupplier ticks;

        private NativeButton(int x, int y, int width, int height, String label, OnPress onPress, IntSupplier ticks) {
            super(x, y, width, height, Component.literal(label), onPress, DEFAULT_NARRATION);
            this.label = label;
            this.ticks = ticks;
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            boolean selected = this.isHoveredOrFocused();
            int x = this.getX();
            int y = this.getY();
            int width = this.getWidth();
            int height = this.getHeight();
            int tick = this.ticks.getAsInt();
            graphics.fill(x, y, x + width, y + height, selected ? 0xC31A0A3B : 0xA507151D);
            graphics.outline(x, y, width, height, selected ? CYAN : LINE);
            graphics.fill(x + 2, y + 2, x + 5, y + height - 2, selected ? 0xFF66E8FF : 0x7738DFF4);
            if (selected) {
                int sweep = 10 + (tick % Math.max(12, width - 24));
                graphics.fill(x + 8, y + height - 3, Math.min(x + width - 8, x + sweep), y + height - 2, 0xCC66E8FF);
            }
            Minecraft minecraft = Minecraft.getInstance();
            int labelWidth = minecraft.font.width(this.label);
            graphics.text(minecraft.font, this.label, x + Math.max(10, (width - labelWidth) / 2), y + 6,
                    selected ? TEXT : CYAN, false);
        }
    }

    private static final class ModuleIndexScreen extends Screen {
        private final Screen parent;
        private int ticks;

        private ModuleIndexScreen(Screen parent) {
            super(Component.literal("ECHO Native Module Index"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            int buttonWidth = Math.min(180, Math.max(120, this.width / 4));
            this.addRenderableWidget(Button.builder(Component.literal("[ BACK ]"),
                            button -> this.minecraft.setScreen(this.parent))
                    .bounds(this.width - buttonWidth - 22, this.height - 34, buttonWidth, 20)
                    .build());
        }

        @Override
        public void tick() {
            super.tick();
            this.ticks++;
        }

        @Override
        public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, this.width, this.height, BG);
            graphics.fill(20, 20, this.width - 20, this.height - 46, PANEL_SOFT);
            graphics.outline(20, 20, this.width - 40, this.height - 66, LINE);
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
            graphics.text(this.font, "ECHO NATIVE MODULE INDEX", 34, 34, CYAN, false);
            graphics.text(this.font, "Loaded from native module classpath", 34, 50, MUTED, false);
            List<String> modules = nativeModuleNames();
            int y = 76;
            int maxRows = Math.max(1, (this.height - 126) / 12);
            for (int index = 0; index < Math.min(maxRows, modules.size()); index++) {
                String line = modules.get(index);
                graphics.text(this.font, line, 42, y + index * 12, index % 2 == 0 ? TEXT : CYAN_DIM, false);
            }
            if (modules.size() > maxRows) {
                graphics.text(this.font, "... " + (modules.size() - maxRows) + " more modules", 42, y + maxRows * 12 + 4,
                        AMBER, false);
            }
            graphics.text(this.font, "tick " + this.ticks, 34, this.height - 31, LINE_DIM, false);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    private static final class ProductWorldStartupFailureScreen extends Screen {
        private final Screen parent;
        private final String titleText;
        private final List<String> lines;
        private int ticks;

        private ProductWorldStartupFailureScreen(Screen parent, String titleText, List<String> lines) {
            super(Component.literal("ECHO Native Ashfall Startup"));
            this.parent = parent;
            this.titleText = titleText == null || titleText.isBlank() ? "ASHFALL STARTUP BLOCKED" : titleText;
            this.lines = lines == null ? List.of() : List.copyOf(lines);
        }

        @Override
        protected void init() {
            int buttonWidth = Math.min(180, Math.max(120, this.width / 4));
            this.addRenderableWidget(Button.builder(Component.literal("[ BACK ]"),
                            button -> this.minecraft.setScreen(this.parent))
                    .bounds(this.width - buttonWidth - 22, this.height - 34, buttonWidth, 20)
                    .build());
        }

        @Override
        public void tick() {
            super.tick();
            this.ticks++;
        }

        @Override
        public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, this.width, this.height, BG);
            graphics.fill(24, 24, this.width - 24, this.height - 48, 0xD0180712);
            graphics.outline(24, 24, this.width - 48, this.height - 72, AMBER);
            graphics.fill(25, 25, this.width - 25, 52, 0x86200212);
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
            graphics.text(this.font, "ECHO NATIVE ASHFALL", 40, 36, CYAN, false);
            graphics.text(this.font, this.titleText + (((this.ticks / 18) % 2) == 0 ? "_" : ""), 40, 68, AMBER, false);
            int y = 94;
            int maxWidth = Math.max(80, this.width - 92);
            for (String line : this.lines) {
                String remaining = line == null ? "" : line.trim();
                if (remaining.isBlank()) {
                    y += 12;
                    continue;
                }
                while (!remaining.isBlank() && y < this.height - 58) {
                    String next = takeLine(this.font, remaining, maxWidth);
                    graphics.text(this.font, next, 46, y, TEXT, false);
                    remaining = remaining.substring(Math.min(next.length(), remaining.length())).trim();
                    y += 12;
                }
            }
            graphics.text(this.font, "Native product world creation is fail-closed.", 40, this.height - 56, MUTED, false);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        private static String takeLine(net.minecraft.client.gui.Font font, String value, int width) {
            if (font.width(value) <= width) {
                return value;
            }
            String[] words = value.split(" ");
            String line = "";
            for (String word : words) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (font.width(candidate) > width) {
                    return line.isEmpty() ? clipToWidth(font, word, width) : line;
                }
                line = candidate;
            }
            return line;
        }

        private static String clipToWidth(net.minecraft.client.gui.Font font, String value, int width) {
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
    }

    private static final class NativeRouteScreen extends Screen {
        private final Screen parent;
        private final String heading;
        private final String route;
        private final List<String> lines;
        private int ticks;

        private NativeRouteScreen(Screen parent, String heading, String route, List<String> lines) {
            super(Component.literal(heading));
            this.parent = parent;
            this.heading = heading;
            this.route = route;
            this.lines = List.copyOf(lines);
        }

        static NativeRouteScreen multiplayer(Screen parent) {
            return new NativeRouteScreen(
                    parent,
                    "ECHO Native Multiplayer",
                    "native_ui:multiplayer",
                    List.of(
                            "profile: Ashfall",
                            "route: native multiplayer uplink",
                            "status: local product session shell active",
                            "vanilla menu handoff: blocked"));
        }

        static NativeRouteScreen options(Screen parent) {
            return new NativeRouteScreen(
                    parent,
                    "ECHO Native Settings",
                    "native_ui:settings",
                    List.of(
                            "profile: Ashfall",
                            "route: native product settings",
                            "resource stack: product-owned",
                            "vanilla menu handoff: blocked"));
        }

        @Override
        protected void init() {
            int buttonWidth = Math.min(180, Math.max(120, this.width / 4));
            this.addRenderableWidget(Button.builder(Component.literal("[ BACK ]"),
                            button -> this.minecraft.setScreen(this.parent))
                    .bounds(this.width - buttonWidth - 22, this.height - 34, buttonWidth, 20)
                    .build());
        }

        @Override
        public void tick() {
            super.tick();
            this.ticks++;
        }

        @Override
        public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, this.width, this.height, BG);
            int sweep = this.width <= 0 ? 0 : (this.ticks * 3) % Math.max(1, this.width);
            graphics.fill(Math.max(0, sweep - 84), 0, Math.min(this.width, sweep), this.height, 0x1119B7D4);
            graphics.fill(24, 24, this.width - 24, this.height - 48, PANEL_SOFT);
            graphics.outline(24, 24, this.width - 48, this.height - 72, LINE);
            graphics.fill(25, 25, this.width - 25, 54, 0x8620024A);
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
            graphics.text(this.font, "ECHO NATIVE", 40, 38, CYAN, false);
            graphics.text(this.font, this.heading.toUpperCase(java.util.Locale.ROOT)
                    + (((this.ticks / 18) % 2) == 0 ? "_" : ""), 40, 70, TEXT, false);
            graphics.text(this.font, "surface-id: " + this.route, 40, 88, CYAN_DIM, false);
            int y = 116;
            for (String line : this.lines) {
                graphics.text(this.font, line, 48, y, line.contains("blocked") ? GREEN : MUTED, false);
                y += 14;
            }
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

}
