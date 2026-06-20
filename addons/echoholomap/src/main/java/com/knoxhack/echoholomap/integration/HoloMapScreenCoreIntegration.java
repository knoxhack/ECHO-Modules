package com.knoxhack.echoholomap.integration;

import com.knoxhack.echoholomap.EchoHoloMap;
import com.knoxhack.echoholomap.EchoHoloMapClient;
import com.knoxhack.echoholomap.client.HoloMapUiController;
import com.knoxhack.echoholomap.client.HoloMapVisualStyle;
import com.knoxhack.echoscreencore.api.EchoDataContext;
import com.knoxhack.echoscreencore.api.EchoScreenRegistry;
import com.knoxhack.echoscreencore.api.EchoScreens;
import com.knoxhack.echoscreencore.api.component.EchoComponentFactory;
import com.knoxhack.echoscreencore.api.layout.EchoMeasureResult;
import com.knoxhack.echoscreencore.client.input.EchoInputRouter;
import com.knoxhack.echoscreencore.client.component.AbstractEchoComponent;
import com.knoxhack.echoscreencore.client.component.EchoComponentSurfaces;
import com.knoxhack.echoscreencore.client.component.EchoComponentSupport;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;
import com.knoxhack.echoscreencore.client.style.EchoStyleValues;
import com.knoxhack.echoscreencore.api.style.EchoStyle;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class HoloMapScreenCoreIntegration {
    private static final Identifier FULLSCREEN_PAGE =
            Identifier.fromNamespaceAndPath(EchoHoloMap.MODID, "fullscreen_holomap");
    private static final int CANVAS_MIN_HEIGHT = 240;
    private static boolean registered;
    private static boolean invalidCanvasBoundsLogged;
    private static long canvasRenderCount;
    private static int lastCanvasX;
    private static int lastCanvasY;
    private static int lastCanvasWidth;
    private static int lastCanvasHeight;

    private HoloMapScreenCoreIntegration() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        EchoScreenRegistry.registerComponent("holomap-canvas", CanvasComponent::new);
        EchoScreenRegistry.registerComponent("holomap-mode-button", ModeButtonComponent::new);
        EchoScreenRegistry.registerComponent("holomap-virtual-list", VirtualListComponent::new);
        EchoScreenRegistry.registerAction("holomap.sync", context -> {
            if (EchoHoloMapClient.nativeLoaderClientActiveForScreens()) {
                return EchoHoloMapClient.dispatchNativeScreenCoreFullscreenCommand(
                        "holomap.sync", "sync", (EchoHoloMapClient.NativeScreenCoreActionRunner) null);
            }
            HoloMapUiController.fullscreen().requestSync(true);
            EchoScreens.invalidateData();
            return true;
        });
        EchoScreenRegistry.registerAction("holomap.center", context -> {
            if (EchoHoloMapClient.nativeLoaderClientActiveForScreens()) {
                return EchoHoloMapClient.dispatchNativeScreenCoreFullscreenCommand(
                        "holomap.center", "center", (EchoHoloMapClient.NativeScreenCoreActionRunner) null);
            }
            HoloMapUiController.fullscreen().centerOnPlayer();
            EchoScreens.invalidateData();
            return true;
        });
        EchoScreenRegistry.registerAction("holomap.toggle_markers", context -> {
            if (EchoHoloMapClient.nativeLoaderClientActiveForScreens()) {
                return EchoHoloMapClient.dispatchNativeScreenCoreFullscreenCommand(
                        "holomap.toggle_markers", "toggleMarkers",
                        (EchoHoloMapClient.NativeScreenCoreActionRunner) null);
            }
            HoloMapUiController.fullscreen().toggleMarkers();
            EchoScreens.invalidateData();
            return true;
        });
        EchoScreenRegistry.registerAction("holomap.cycle_fields", context -> {
            if (EchoHoloMapClient.nativeLoaderClientActiveForScreens()) {
                return EchoHoloMapClient.dispatchNativeScreenCoreFullscreenCommand(
                        "holomap.cycle_fields", "cycleFields",
                        (EchoHoloMapClient.NativeScreenCoreActionRunner) null);
            }
            HoloMapUiController.fullscreen().cycleFieldMode();
            EchoScreens.invalidateData();
            return true;
        });
        EchoScreenRegistry.registerAction("holomap.toggle_waypoints", context -> {
            if (EchoHoloMapClient.nativeLoaderClientActiveForScreens()) {
                return EchoHoloMapClient.dispatchNativeScreenCoreFullscreenCommand(
                        "holomap.toggle_waypoints", "toggleWaypoints",
                        (EchoHoloMapClient.NativeScreenCoreActionRunner) null);
            }
            HoloMapUiController.fullscreen().toggleWaypoints();
            EchoScreens.invalidateData();
            return true;
        });
        EchoScreenRegistry.registerAction("holomap.close", context -> {
            if (EchoHoloMapClient.nativeLoaderClientActiveForScreens()) {
                return EchoHoloMapClient.dispatchNativeScreenCoreFullscreenCommand(
                        "holomap.close", "close", (actionId, reason) -> context.close());
            }
            return context.close();
        });
    }

    public static boolean openFullscreen() {
        register();
        if (EchoHoloMapClient.nativeLoaderClientActiveForScreens()) {
            EchoNativeLoadStatus lifecycleStatus = EchoHoloMapClient.publishNativeScreenLifecycle(
                        "open",
                        "holomap.screencore.open_fullscreen",
                        HoloMapScreenCoreIntegration.class.getName(),
                        Map.of(
                                "targetScreenClass", HoloMapScreenCoreIntegration.class.getName(),
                                "transitionSource", "holomap_screencore_open_fullscreen",
                                "screenBridge", "echoscreencore"
                        ));
            if (lifecycleStatus != EchoNativeLoadStatus.MUTATED) {
                return false;
            }
        }
        HoloMapUiController.fullscreen().open();
        return EchoScreens.open(FULLSCREEN_PAGE, EchoDataContext.empty()
                .put("screen.title", "ECHO HoloMap"));
    }

    public static void resetCanvasDiagnosticsForTests() {
        invalidCanvasBoundsLogged = false;
        canvasRenderCount = 0L;
        lastCanvasX = 0;
        lastCanvasY = 0;
        lastCanvasWidth = 0;
        lastCanvasHeight = 0;
    }

    public static long canvasRenderCountForTests() {
        return canvasRenderCount;
    }

    public static int lastCanvasWidthForTests() {
        return lastCanvasWidth;
    }

    public static int lastCanvasHeightForTests() {
        return lastCanvasHeight;
    }

    public static int canvasMeasuredHeightForTests(int availableHeight) {
        return canvasPreferredHeight(availableHeight, CANVAS_MIN_HEIGHT);
    }

    private static final class ModeButtonComponent extends AbstractEchoComponent {
        private ModeButtonComponent(EchoComponentFactory.Context context) {
            super(EchoComponentSupport.node(context), EchoComponentSupport.children(context));
        }

        @Override
        public boolean focusable() {
            return true;
        }

        @Override
        public EchoMeasureResult measure(EchoRenderContext context, int availableWidth, int availableHeight) {
            return new EchoMeasureResult(Math.max(72, availableWidth), Math.max(24, availableHeight));
        }

        @Override
        protected void renderSelf(EchoRenderContext context) {
            HoloMapUiController controller = HoloMapUiController.fullscreen();
            String mode = node().attribute("mode", "");
            String label = controller.controlLabel(mode);
            boolean enabled = controller.controlActive(mode);
            boolean active = hovered() || focused();
            int accent = enabled ? HoloMapVisualStyle.ACCENT : HoloMapVisualStyle.MUTED;
            EchoStyle current = effectiveStyle(context);
            int background = EchoStyleValues.color(current, "background", context.theme(),
                    enabled ? 0x6610242B : 0x440B141A, context.diagnostics());
            int border = active
                    ? context.theme().color("accent", HoloMapVisualStyle.ACCENT)
                    : EchoStyleValues.color(current, "border-color", context.theme(),
                            HoloMapVisualStyle.withAlpha(accent, enabled ? 0xB0 : 0x66), context.diagnostics());
            if (!EchoComponentSurfaces.renderGlass(context, current, bounds(), background, border,
                    context.theme().color("accent", HoloMapVisualStyle.ACCENT), active || enabled)) {
                context.graphics().fill(bounds().x(), bounds().y(), bounds().right(), bounds().bottom(), background);
                context.render().outline(context.graphics(), bounds().x(), bounds().y(), bounds().width(), bounds().height(),
                        border);
            }
            if (enabled) {
                context.graphics().fill(bounds().x() + 4, bounds().bottom() - 2,
                        bounds().right() - 4, bounds().bottom() - 1,
                        HoloMapVisualStyle.withAlpha(HoloMapVisualStyle.ACCENT, active ? 0xCC : 0x88));
            }
            String trimmed = VirtualListComponent.trim(context.font(), label, Math.max(8, bounds().width() - 10));
            int labelX = bounds().x() + Math.max(4, (bounds().width() - context.font().width(trimmed)) / 2);
            int labelY = bounds().y() + Math.max(4, (bounds().height() - 8) / 2);
            context.graphics().text(context.font(), trimmed, labelX, labelY,
                    enabled ? HoloMapVisualStyle.TEXT : HoloMapVisualStyle.MUTED, false);
        }
    }

    private static final class CanvasComponent extends AbstractEchoComponent
            implements EchoHoloMapClient.NativeScreenCoreFullscreenTarget {
        private CanvasComponent(EchoComponentFactory.Context context) {
            super(EchoComponentSupport.node(context), EchoComponentSupport.children(context));
        }

        @Override
        public boolean focusable() {
            return true;
        }

        @Override
        public EchoMeasureResult measure(EchoRenderContext context, int availableWidth, int availableHeight) {
            int minHeight = EchoStyleValues.length(style(), "min-height", availableHeight,
                    CANVAS_MIN_HEIGHT, context.theme(), context.diagnostics());
            int targetHeight = EchoStyleValues.length(style(), "height", availableHeight,
                    availableHeight, context.theme(), context.diagnostics());
            return new EchoMeasureResult(Math.max(1, availableWidth),
                    canvasPreferredHeight(targetHeight, minHeight));
        }

        @Override
        public void render(EchoRenderContext context) {
            recordCanvasBounds(bounds().x(), bounds().y(), bounds().width(), bounds().height());
            if (bounds().width() <= 0 || bounds().height() <= 0) {
                logInvalidCanvasBounds();
                return;
            }
            super.render(context);
        }

        @Override
        protected void renderSelf(EchoRenderContext context) {
            canvasRenderCount++;
            HoloMapUiController.fullscreen().render(context.graphics(), context.font(),
                    bounds().x(), bounds().y(), bounds().width(), bounds().height(),
                    context.mouseX(), context.mouseY());
            context.render().outline(context.graphics(), bounds().x(), bounds().y(), bounds().width(), bounds().height(),
                    HoloMapVisualStyle.withAlpha(HoloMapVisualStyle.ACCENT, hovered() || focused() ? 0xCC : 0x88));
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button,
                EchoInputRouter.ActionRunner actions) {
            if (EchoHoloMapClient.nativeLoaderClientActiveForScreens()) {
                return EchoHoloMapClient.dispatchNativeScreenCoreFullscreenMouse(
                        this,
                        actionRunner(actions),
                        "click",
                        mouseX,
                        mouseY,
                        button,
                        0,
                        false,
                        0.0D,
                        0.0D,
                        bounds().x(),
                        bounds().y(),
                        bounds().width(),
                        bounds().height());
            }
            return handleNativeRouteMouse("click", mouseX, mouseY, button, 0, false, 0.0D, 0.0D,
                    actionRunner(actions));
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY,
                EchoInputRouter.ActionRunner actions) {
            if (EchoHoloMapClient.nativeLoaderClientActiveForScreens()) {
                return EchoHoloMapClient.dispatchNativeScreenCoreFullscreenMouse(
                        this,
                        actionRunner(actions),
                        "drag",
                        mouseX,
                        mouseY,
                        button,
                        0,
                        false,
                        dragX,
                        dragY,
                        bounds().x(),
                        bounds().y(),
                        bounds().width(),
                        bounds().height());
            }
            return handleNativeRouteMouse("drag", mouseX, mouseY, button, 0, false, dragX, dragY,
                    actionRunner(actions));
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button,
                EchoInputRouter.ActionRunner actions) {
            if (EchoHoloMapClient.nativeLoaderClientActiveForScreens()) {
                return EchoHoloMapClient.dispatchNativeScreenCoreFullscreenMouse(
                        this,
                        actionRunner(actions),
                        "release",
                        mouseX,
                        mouseY,
                        button,
                        0,
                        false,
                        0.0D,
                        0.0D,
                        bounds().x(),
                        bounds().y(),
                        bounds().width(),
                        bounds().height());
            }
            return handleNativeRouteMouse("release", mouseX, mouseY, button, 0, false, 0.0D, 0.0D,
                    actionRunner(actions));
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double deltaY) {
            if (EchoHoloMapClient.nativeLoaderClientActiveForScreens()) {
                return EchoHoloMapClient.dispatchNativeScreenCoreFullscreenScroll(
                        this,
                        mouseX,
                        mouseY,
                        deltaY,
                        bounds().x(),
                        bounds().y(),
                        bounds().width(),
                        bounds().height());
            }
            return handleNativeRouteScroll(mouseX, mouseY, 0.0D, deltaY);
        }

        @Override
        public boolean keyPressed(int key, EchoInputRouter.ActionRunner actions) {
            if (EchoHoloMapClient.nativeLoaderClientActiveForScreens()) {
                return EchoHoloMapClient.dispatchNativeScreenCoreFullscreenKey(
                        this,
                        actionRunner(actions),
                        key,
                        bounds().width(),
                        bounds().height());
            }
            return handleNativeRouteKey(key, actionRunner(actions));
        }

        @Override
        public String nativeRouteScreenClass() {
            return HoloMapScreenCoreIntegration.class.getName();
        }

        @Override
        public boolean handleNativeRouteMouse(
                String phase,
                double mouseX,
                double mouseY,
                int button,
                int modifiers,
                boolean doubleClick,
                double dragX,
                double dragY,
                EchoHoloMapClient.NativeScreenCoreActionRunner actions
        ) {
            if ("drag".equals(phase)) {
                return button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                        && HoloMapUiController.fullscreen().mouseDragged(dragX, dragY,
                                bounds().width(), bounds().height());
            }
            if ("release".equals(phase)) {
                return button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                        && HoloMapUiController.fullscreen().mouseReleased(bounds().width(), bounds().height());
            }
            if (!"click".equals(phase)) {
                return false;
            }
            HoloMapUiController.ClickResult result =
                    HoloMapUiController.fullscreen().mouseClicked(mouseX, mouseY, button);
            if (result == HoloMapUiController.ClickResult.CLOSE) {
                EchoScreens.invalidateData();
                return actions != null && actions.run("holomap.close", "mouse");
            }
            if (result.handled()) {
                EchoScreens.invalidateData();
                return true;
            }
            return false;
        }

        @Override
        public boolean handleNativeRouteScroll(double mouseX, double mouseY, double scrollX, double scrollY) {
            return HoloMapUiController.fullscreen().mouseScrolled(scrollY, mouseX, mouseY,
                    bounds().x(), bounds().y(), bounds().width(), bounds().height());
        }

        @Override
        public boolean handleNativeRouteKey(int key, EchoHoloMapClient.NativeScreenCoreActionRunner actions) {
            boolean handled = HoloMapUiController.fullscreen().keyPressed(key, bounds().width(), bounds().height());
            if (handled) {
                EchoScreens.invalidateData();
            }
            return handled;
        }

        private EchoHoloMapClient.NativeScreenCoreActionRunner actionRunner(EchoInputRouter.ActionRunner actions) {
            return (actionId, reason) -> actions != null && actions.run(actionId, this, reason);
        }
    }

    private static int canvasPreferredHeight(int targetHeight, int minHeight) {
        return Math.max(Math.max(1, minHeight), targetHeight);
    }

    private static void recordCanvasBounds(int x, int y, int width, int height) {
        lastCanvasX = x;
        lastCanvasY = y;
        lastCanvasWidth = width;
        lastCanvasHeight = height;
    }

    private static void logInvalidCanvasBounds() {
        if (invalidCanvasBoundsLogged) {
            return;
        }
        invalidCanvasBoundsLogged = true;
        EchoHoloMap.LOGGER.warn("ECHO HoloMap ScreenCore canvas has invalid bounds: x={}, y={}, width={}, height={}.",
                lastCanvasX, lastCanvasY, lastCanvasWidth, lastCanvasHeight);
    }

    private static final class VirtualListComponent extends AbstractEchoComponent {
        private static final int DETAIL_HEIGHT = 42;
        private static final int HEADER_HEIGHT = 16;
        private static final int ROW_HEIGHT = 24;
        private static final int VERTICAL_PADDING = 6;

        private VirtualListComponent(EchoComponentFactory.Context context) {
            super(EchoComponentSupport.node(context), EchoComponentSupport.children(context));
        }

        @Override
        public boolean focusable() {
            return true;
        }

        @Override
        public EchoMeasureResult measure(EchoRenderContext context, int availableWidth, int availableHeight) {
            int reservedHeight = intAttribute("reserved-controls-height", 0);
            return new EchoMeasureResult(Math.max(1, availableWidth),
                    Math.max(80, availableHeight - Math.max(0, reservedHeight)));
        }

        @Override
        protected void renderSelf(EchoRenderContext context) {
            int x = bounds().x();
            int y = bounds().y();
            int width = bounds().width();
            int height = bounds().height();
            List<HoloMapUiController.Entry> entries = HoloMapUiController.fullscreen().entries();
            HoloMapUiController.Entry selected = selectedEntry(entries);
            int detailHeight = selected == null ? 0 : DETAIL_HEIGHT;
            int listX = x + 4;
            int listY = y + VERTICAL_PADDING + detailHeight;
            int listW = Math.max(1, width - 8);
            int listH = Math.max(1, height - VERTICAL_PADDING * 2 - detailHeight);
            setMaxScroll(Math.max(0, contentHeight(entries) - listH));

            context.graphics().fill(x, y, x + width, y + height, 0x4C061014);
            if (selected != null) {
                drawSelectedDetail(context, selected, x + 5, y + 5, width - 10, DETAIL_HEIGHT - 5);
            }
            if (entries.isEmpty()) {
                context.graphics().text(context.font(), "No visible signals.", x + 8, listY + 4,
                        HoloMapVisualStyle.MUTED, false);
            } else {
                context.render().enableScissor(context.graphics(), listX, listY, listW, listH);
                try {
                    int rowY = listY - scrollOffset();
                    for (HoloMapUiController.Entry entry : entries) {
                        int rowHeight = rowHeight(entry);
                        if (rowY + rowHeight >= listY && rowY <= listY + listH) {
                            drawEntry(context, entry, listX, rowY, listW, rowHeight);
                        }
                        rowY += rowHeight;
                    }
                } finally {
                    context.render().disableScissor(context.graphics());
                }
                drawScrollbar(context, listX, listY, listW, listH);
            }
            if (focused()) {
                context.render().outline(context.graphics(), x, y, width, height,
                        HoloMapVisualStyle.withAlpha(HoloMapVisualStyle.ACCENT, 0xCC));
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button,
                com.knoxhack.echoscreencore.client.input.EchoInputRouter.ActionRunner actions) {
            if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                return false;
            }
            HoloMapUiController.Entry entry = entryAt(mouseY);
            if (entry != null && !entry.header() && entry.id() != null) {
                if (EchoHoloMapClient.nativeLoaderClientActiveForScreens()) {
                    return EchoHoloMapClient.dispatchNativeScreenCoreFullscreenCommand(
                            "holomap.select_entry",
                            "selectEntry",
                            null,
                            Map.of("entryId", entry.id()));
                }
                boolean selected = HoloMapUiController.fullscreen().selectEntry(entry.id());
                if (selected) {
                    EchoScreens.invalidateData();
                }
                return selected;
            }
            return true;
        }

        private HoloMapUiController.Entry entryAt(double mouseY) {
            List<HoloMapUiController.Entry> entries = HoloMapUiController.fullscreen().entries();
            HoloMapUiController.Entry selected = selectedEntry(entries);
            int listY = bounds().y() + VERTICAL_PADDING + (selected == null ? 0 : DETAIL_HEIGHT);
            int localY = (int) Math.floor(mouseY - listY + scrollOffset());
            if (localY < 0) {
                return null;
            }
            int cursor = 0;
            for (HoloMapUiController.Entry entry : entries) {
                int rowHeight = rowHeight(entry);
                if (localY >= cursor && localY < cursor + rowHeight) {
                    return entry.header() ? null : entry;
                }
                cursor += rowHeight;
            }
            return null;
        }

        private static void drawSelectedDetail(EchoRenderContext context, HoloMapUiController.Entry entry,
                int x, int y, int width, int height) {
            context.graphics().fill(x, y, x + width, y + height, 0x5526DFF4);
            context.graphics().fill(x, y, x + 2, y + height, HoloMapVisualStyle.ACCENT);
            context.render().outline(context.graphics(), x, y, width, height,
                    HoloMapVisualStyle.withAlpha(HoloMapVisualStyle.ACCENT, 0xAA));
            context.graphics().fill(x + 7, y + 7, x + 11, y + height - 7, entry.color());
            String title = trim(context.font(), entry.title(), Math.max(8, width - 22));
            context.graphics().text(context.font(), title, x + 15, y + 6, HoloMapVisualStyle.TEXT, false);
            String meta = trim(context.font(), entry.kindLabel() + entry.countLabel()
                    + " | " + entry.distanceLabel() + " | " + entry.coordinateLabel(), Math.max(8, width - 22));
            context.graphics().text(context.font(), meta, x + 15, y + 18, HoloMapVisualStyle.MUTED, false);
        }

        private static void drawEntry(EchoRenderContext context, HoloMapUiController.Entry entry,
                int x, int y, int width, int height) {
            if (entry.header()) {
                context.graphics().fill(x + 2, y + 8, x + width - 2, y + 9,
                        HoloMapVisualStyle.withAlpha(HoloMapVisualStyle.ACCENT, 0x3F));
                context.graphics().text(context.font(), trim(context.font(),
                        entry.title().toUpperCase(java.util.Locale.ROOT), Math.max(24, width - 10)),
                        x + 4, y + 2, HoloMapVisualStyle.MUTED, false);
                return;
            }
            if (entry.selected()) {
                context.graphics().fill(x + 1, y + 1, x + width - 2, y + height - 1, 0x3F26DFF4);
                context.graphics().fill(x + 3, y + 3, x + 5, y + height - 3, HoloMapVisualStyle.ACCENT);
            } else {
                context.graphics().fill(x + 6, y + height - 1, x + width - 6, y + height,
                        HoloMapVisualStyle.withAlpha(HoloMapVisualStyle.ACCENT, 0x20));
            }
            context.graphics().fill(x + 9, y + 6, x + 13, y + height - 6, entry.color());
            int textX = x + 18;
            String title = trim(context.font(), entry.prefix() + " " + entry.title() + entry.countLabel(),
                    Math.max(8, width - 46));
            context.graphics().text(context.font(), title, textX, y + 4,
                    entry.selected() ? HoloMapVisualStyle.TEXT : entry.color(), false);
            String meta = trim(context.font(), entry.kindLabel() + " | " + entry.distanceLabel()
                    + " | " + entry.coordinateLabel(), Math.max(8, width - 46));
            context.graphics().text(context.font(), meta, textX, y + 14, HoloMapVisualStyle.MUTED, false);
        }

        private void drawScrollbar(EchoRenderContext context, int x, int y, int width, int height) {
            if (maxScroll() <= 0 || height <= 8) {
                return;
            }
            int railX = x + width - 3;
            int railH = Math.max(12, height - 8);
            int thumbH = Math.max(12, railH * height / Math.max(height + maxScroll(), 1));
            int thumbY = y + 4 + (railH - thumbH) * scrollOffset() / Math.max(1, maxScroll());
            context.graphics().fill(railX, y + 4, railX + 1, y + 4 + railH,
                    HoloMapVisualStyle.withAlpha(HoloMapVisualStyle.ACCENT, hovered() || focused() ? 0x66 : 0x3A));
            context.graphics().fill(railX - 1, thumbY, railX + 2, thumbY + thumbH,
                    HoloMapVisualStyle.withAlpha(HoloMapVisualStyle.ACCENT, focused() ? 0xDD : 0xAA));
        }

        private static HoloMapUiController.Entry selectedEntry(List<HoloMapUiController.Entry> entries) {
            for (HoloMapUiController.Entry entry : entries) {
                if (!entry.header() && entry.selected()) {
                    return entry;
                }
            }
            return null;
        }

        private static int contentHeight(List<HoloMapUiController.Entry> entries) {
            int height = 0;
            for (HoloMapUiController.Entry entry : entries) {
                height += rowHeight(entry);
            }
            return height;
        }

        private static int rowHeight(HoloMapUiController.Entry entry) {
            return entry.header() ? HEADER_HEIGHT : ROW_HEIGHT;
        }

        private static String trim(net.minecraft.client.gui.Font font, String text, int maxWidth) {
            if (font == null || text == null || maxWidth <= 0 || font.width(text) <= maxWidth) {
                return text == null ? "" : text;
            }
            if (maxWidth <= font.width("...")) {
                return font.plainSubstrByWidth(text, maxWidth);
            }
            return font.plainSubstrByWidth(text, Math.max(1, maxWidth - font.width("..."))).stripTrailing() + "...";
        }

        private int intAttribute(String name, int fallback) {
            String value = node().attribute(name, Integer.toString(fallback)).strip().replace("px", "");
            if (value.isBlank()) {
                return fallback;
            }
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
    }
}
