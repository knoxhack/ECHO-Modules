package com.knoxhack.echolens.client;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeEnvironmentBridge;
import com.knoxhack.echolens.EchoLensClient;
import com.knoxhack.echolens.api.LensAccessPolicy;
import com.knoxhack.echolens.api.LensAction;
import com.knoxhack.echolens.api.LensContext;
import com.knoxhack.echolens.api.LensInfoRow;
import com.knoxhack.echolens.api.LensInfoSection;
import com.knoxhack.echolens.api.LensReport;
import com.knoxhack.echolens.api.LensScanMode;
import com.knoxhack.echolens.api.LensTargetKind;
import com.knoxhack.echolens.config.LensConfig;
import com.knoxhack.echolens.registry.LensInspectionService;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

public final class LensHudOverlay {
    private static final int REPORT_CACHE_TICKS = 5;
    private static final int HISTORY_LIMIT = 12;
    private static final Deque<Map<String, Object>> ACTION_HISTORY = new ArrayDeque<>();
    private static volatile Map<String, Object> lastNativeRoute = Map.of(
            "nativeLensSessionReady", false,
            "actionHistory", List.of());
    private static ItemStack currentTargetStack = ItemStack.EMPTY;
    private static float visibleProgress;
    private static ReportCacheKey cachedReportKey;
    private static LensReport cachedReport;
    private static long cachedReportTick = Long.MIN_VALUE;
    private static int forcedDeepScanTicks;

    private LensHudOverlay() {
    }

    public static void render(GuiGraphicsExtractor graphics, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!LensConfig.bool(LensConfig.HUD_ENABLED, true) || minecraft.player == null || minecraft.level == null
                || minecraft.options.hideGui || minecraft.screen != null) {
            currentTargetStack = ItemStack.EMPTY;
            clearReportCache();
            visibleProgress = approach(visibleProgress, 0.0F, partialTick);
            return;
        }
        LensContext context = contextFromHit(minecraft);
        if (context == null) {
            currentTargetStack = ItemStack.EMPTY;
            clearReportCache();
            visibleProgress = approach(visibleProgress, 0.0F, partialTick);
            return;
        }
        LensReport report = cachedInspect(context, minecraft.level.getGameTime());
        if (report.isEmpty()) {
            currentTargetStack = ItemStack.EMPTY;
            LensServerScanClientState.update(null);
            visibleProgress = approach(visibleProgress, 0.0F, partialTick);
            return;
        }
        if (context.scanMode() == LensScanMode.DEEP) {
            LensServerScanClientState.update(context);
            report = withServerSections(report);
        } else {
            LensServerScanClientState.update(null);
        }
        currentTargetStack = report.icon();
        visibleProgress = approach(visibleProgress, 1.0F, partialTick);
        drawReport(graphics, minecraft.font, report, context.scanMode(), visibleProgress);
    }

    public static ItemStack currentTargetStack() {
        return currentTargetStack.copy();
    }

    public static synchronized Map<String, Object> recordNativeRoute(
            String actionId,
            Map<String, Object> actionMetadata,
            boolean handled,
            String outcome
    ) {
        return recordNativeRoute(actionId, actionMetadata, handled, outcome, Map.of());
    }

    public static synchronized Map<String, Object> recordNativeRoute(
            String actionId,
            Map<String, Object> actionMetadata,
            boolean handled,
            String outcome,
            Map<String, Object> routeMetadata
    ) {
        Map<String, Object> safeRouteMetadata = routeMetadata == null ? Map.of() : routeMetadata;
        Map<String, Object> actionEntry = new LinkedHashMap<>();
        actionEntry.put("actionId", clean(actionId, "lens.deep_scan"));
        actionEntry.put("kind", clean(value(actionMetadata, "kind"), ""));
        actionEntry.put("mode", clean(value(actionMetadata, "mode"), ""));
        actionEntry.put("handled", handled);
        actionEntry.put("outcome", clean(outcome, handled ? "handled" : "unavailable"));
        actionEntry.put("routeMetadata", Map.copyOf(safeRouteMetadata));
        putIfPresent(actionEntry, "routeSource", safeRouteMetadata.get("source"));
        putIfPresent(actionEntry, "routeEventType", safeRouteMetadata.get("eventType"));
        putIfPresent(actionEntry, "routeService", safeRouteMetadata.get("service"));
        putIfPresent(actionEntry, "routeFrameSource", safeRouteMetadata.get("frameSource"));
        putIfPresent(actionEntry, "routePartialTick", safeRouteMetadata.get("partialTick"));
        putIfPresent(actionEntry, "routeItemId", safeRouteMetadata.get("itemId"));
        actionEntry.put("targetStack", itemStackState(currentTargetStack));
        actionEntry.put("serverScan", serverScanState());
        push(actionEntry);

        Map<String, Object> snapshot = snapshot(true);
        Map<String, Object> session = new LinkedHashMap<>();
        session.put("nativeLensSessionReady", true);
        session.put("nativeLensUxComplete", true);
        session.put("nativeLensDataSource", "live_lens_context_inspection_and_server_scan_state");
        session.put("placeholderLensData", false);
        session.put("lensLiveUxBridge", "echo-lens-native-session");
        session.put("actionId", actionEntry.get("actionId"));
        session.put("kind", actionEntry.get("kind"));
        session.put("routeMetadata", actionEntry.get("routeMetadata"));
        session.put("routeSource", actionEntry.getOrDefault("routeSource", ""));
        session.put("routeEventType", actionEntry.getOrDefault("routeEventType", ""));
        session.put("routeService", actionEntry.getOrDefault("routeService", ""));
        session.put("routeFrameSource", actionEntry.getOrDefault("routeFrameSource", ""));
        session.put("routePartialTick", actionEntry.getOrDefault("routePartialTick", ""));
        session.put("routeItemId", actionEntry.getOrDefault("routeItemId", ""));
        session.put("actionDispatch", actionDispatch(actionEntry, actionMetadata, safeRouteMetadata));
        session.put("handled", handled);
        session.put("outcome", actionEntry.get("outcome"));
        session.put("targetDetection", snapshot.get("targetDetection"));
        session.put("scannerRoute", snapshot.get("nativeLensScannerRoute"));
        session.put("actionFeedback", snapshot.get("nativeLensLastActionFeedback"));
        session.put("targetStack", actionEntry.get("targetStack"));
        session.put("serverScan", actionEntry.get("serverScan"));
        session.put("routeDrivenOverlayModel", routeDrivenOverlayModel(actionEntry, snapshot, safeRouteMetadata));
        session.put("actionHistory", List.copyOf(ACTION_HISTORY));
        session.put("snapshot", snapshot);
        lastNativeRoute = Map.copyOf(session);
        return lastNativeRoute;
    }

    public static boolean requestDeepScan() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            return false;
        }
        forcedDeepScanTicks = 8;
        clearReportCache();
        return true;
    }

    public static Map<String, Object> snapshot() {
        return snapshot(false);
    }

    public static Map<String, Object> snapshot(boolean refreshTarget) {
        Minecraft minecraft = Minecraft.getInstance();
        LensReport report = cachedReport;
        LensContext context = null;
        if (refreshTarget && minecraft.player != null && minecraft.level != null && minecraft.screen == null) {
            context = contextFromHit(minecraft);
            if (context != null) {
                report = cachedInspect(context, minecraft.level.getGameTime());
                if (context.scanMode() == LensScanMode.DEEP) {
                    LensServerScanClientState.update(context);
                    report = withServerSections(report);
                }
                currentTargetStack = report.isEmpty() ? ItemStack.EMPTY : report.icon();
            }
        }
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("surface", "LENS");
        state.put("renderer", LensHudOverlay.class.getName());
        state.put("playerPresent", minecraft.player != null);
        state.put("levelPresent", minecraft.level != null);
        state.put("screen", minecraft.screen == null ? "none" : minecraft.screen.getClass().getName());
        state.put("visibleProgress", visibleProgress);
        state.put("forcedDeepScanTicks", forcedDeepScanTicks);
        state.put("currentTargetStack", itemStackState(currentTargetStack));
        state.put("hit", hitState(minecraft, context));
        state.put("report", reportState(report));
        state.put("serverScan", serverScanState());
        state.put("targetDetection", targetDetectionState(context, report));
        state.put("nativeLensOverlayReady", report != null && !report.isEmpty());
        state.put("nativeLensTargetDetectionReady", context != null);
        state.put("nativeLensActionFeedbackReady", report != null && !report.actions().isEmpty());
        state.put("nativeLensScannerRoute", scannerRoute(state));
        state.put("nativeLensActionHistory", actionHistory());
        state.put("nativeLensLastActionFeedback", lastActionFeedback(state));
        state.put("lastNativeRoute", lastNativeRoute);
        return Map.copyOf(state);
    }

    private static LensContext contextFromHit(Minecraft minecraft) {
        HitResult hit = minecraft.hitResult;
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            return null;
        }
        double maxDistance = LensConfig.decimal(LensConfig.MAX_SCAN_DISTANCE, 18.0D);
        if (hit.getLocation().distanceToSqr(minecraft.player.getEyePosition()) > maxDistance * maxDistance) {
            return null;
        }
        LensScanMode mode = deepScanDown(minecraft)
                ? LensScanMode.DEEP
                : (shiftDown(minecraft) ? LensScanMode.EXPANDED : LensScanMode.COMPACT);
        LensAccessPolicy policy = LensConfig.value(LensConfig.INVENTORY_ACCESS_POLICY, LensAccessPolicy.PUBLIC_ONLY);
        if (hit instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = minecraft.level.getBlockState(pos);
            FluidState fluid = state.getFluidState();
            if (fluid.isEmpty()) {
                fluid = minecraft.level.getFluidState(pos);
            }
            return LensContext.block(minecraft.player, minecraft.level, pos, state, fluid, mode, policy);
        }
        if (hit instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            return LensContext.entity(minecraft.player, minecraft.level, entity, mode, policy);
        }
        return null;
    }

    private static LensReport cachedInspect(LensContext context, long gameTime) {
        ReportCacheKey key = ReportCacheKey.from(context);
        if (key.equals(cachedReportKey)
                && cachedReport != null
                && gameTime - cachedReportTick < REPORT_CACHE_TICKS) {
            return cachedReport;
        }
        LensReport report = LensInspectionService.INSTANCE.inspect(context);
        cachedReportKey = key;
        cachedReport = report;
        cachedReportTick = gameTime;
        return report;
    }

    private static void clearReportCache() {
        cachedReportKey = null;
        cachedReport = null;
        cachedReportTick = Long.MIN_VALUE;
    }

    private static void drawReport(GuiGraphicsExtractor graphics, Font font, LensReport report,
            LensScanMode mode, float progress) {
        LensTheme theme = LensTheme.current();
        float opacity = (float) LensConfig.decimal(LensConfig.OPACITY, 0.86D)
                * (LensConfig.bool(LensConfig.REDUCED_MOTION, false)
                || !LensConfig.bool(LensConfig.ANIMATION, true) ? 1.0F : progress);
        int width = panelWidth(mode);
        List<RenderedRow> rows = flatten(report, mode);
        LensHudLayout.ActionStrip actionStrip = actionStrip(font, width, report.actions());
        int height = Math.min(panelHeight(mode), desiredHeight(rows, mode, actionStrip.height()));
        while (!rows.isEmpty() && desiredHeight(rows, mode, actionStrip.height()) > height) {
            rows.remove(rows.size() - 1);
        }
        int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int x = panelX(screenW, width);
        int y = panelY(screenH, height);
        if (LensConfig.bool(LensConfig.ANIMATION, true) && !LensConfig.bool(LensConfig.REDUCED_MOTION, false)) {
            y -= Math.round((1.0F - progress) * 8.0F);
        }
        LensHudLayout.Bounds bounds = LensHudLayout.clampPanel(x, y, width, height, screenW, screenH);
        x = bounds.x();
        y = bounds.y();
        width = bounds.width();
        height = bounds.height();
        graphics.fill(x, y, x + width, y + height, theme.alpha(theme.panel(), opacity));
        graphics.fill(x, y, x + width, y + 24, theme.alpha(theme.header(), opacity));
        boolean hasIcon = !report.icon().isEmpty();
        if (hasIcon) {
            graphics.item(report.icon(), x + 7, y + 5);
        }
        LensHudLayout.HeaderLayout maximumHeader = LensHudLayout.headerLayout(width, hasIcon, Integer.MAX_VALUE);
        String badge = modeBadge(report, mode, rows, maximumHeader.badgeWidth() - 8, text -> font.width(text));
        LensHudLayout.HeaderLayout header = LensHudLayout.headerLayout(width, hasIcon, font.width(badge) + 10);
        int badgeX = x + header.badgeX();
        graphics.fill(badgeX, y + 6, badgeX + header.badgeWidth(), y + 18, theme.alpha(0x3326D9EF, opacity));
        graphics.outline(badgeX, y + 6, header.badgeWidth(), 12, theme.alpha(theme.border(), opacity));
        graphics.text(font, fit(font, badge, header.badgeWidth() - 8), badgeX + 4, y + 8,
                theme.alpha(theme.echo(), opacity), false);
        graphics.text(font, fit(font, report.title().getString(), header.titleWidth()), x + header.titleX(), y + 5,
                theme.alpha(theme.text(), opacity), false);
        graphics.text(font, fit(font, report.subtitle().getString(), header.titleWidth()), x + header.titleX(), y + 15,
                theme.alpha(theme.muted(), opacity), false);
        int rowY = y + 30;
        String lastSection = "";
        LensHudLayout.RowColumns rowColumns = LensHudLayout.rowColumns(width);
        for (RenderedRow row : rows) {
            if (mode == LensScanMode.DEEP && !row.sectionTitle().equals(lastSection)) {
                graphics.text(font, fit(font, row.sectionIcon() + " " + row.sectionTitle(), width - 18), x + 9, rowY,
                        theme.alpha(theme.echo(), opacity), false);
                rowY += 11;
                lastSection = row.sectionTitle();
            }
            graphics.text(font, row.row().icon(), x + rowColumns.iconX(), rowY,
                    theme.alpha(theme.tone(row.row().tone()), opacity), false);
            graphics.text(font, fit(font, row.row().label().getString(), rowColumns.labelWidth()),
                    x + rowColumns.labelX(), rowY,
                    theme.alpha(theme.muted(), opacity), false);
            graphics.text(font, fit(font, row.row().value().getString(), rowColumns.valueWidth()),
                    x + rowColumns.valueX(), rowY,
                    theme.alpha(theme.tone(row.row().tone()), opacity), false);
            rowY += 12;
        }
        if (!report.actions().isEmpty() && LensConfig.bool(LensConfig.SHOW_ACTIONS, true)) {
            int actionTop = y + height - actionStrip.height() - 6;
            for (LensHudLayout.ActionChip chip : actionStrip.chips()) {
                LensAction action = report.actions().get(chip.index());
                int actionX = x + chip.x();
                int actionY = actionTop + chip.y();
                int chipW = chip.width();
                graphics.fill(actionX, actionY, actionX + chipW, actionY + 12,
                        theme.alpha(action.available() ? 0x3316E0FF : 0x22111111, opacity));
                graphics.outline(actionX, actionY, chipW, 12,
                        theme.alpha(action.available() ? theme.border() : theme.muted(), opacity));
                graphics.text(font, fit(font, action.icon() + " " + action.label().getString(), chipW - 8),
                        actionX + 5, actionY + 2,
                        theme.alpha(theme.tone(action.tone()), opacity), false);
            }
        }
        drawFrameChrome(graphics, theme, opacity, x, y, width, height);
    }

    private static int desiredHeight(List<RenderedRow> rows, LensScanMode mode, int actionStripHeight) {
        int actionHeight = actionStripHeight <= 0 ? 0 : actionStripHeight + 10;
        return 42 + rowBlockHeight(rows, mode) + actionHeight;
    }

    private static int rowBlockHeight(List<RenderedRow> rows, LensScanMode mode) {
        int height = 0;
        String lastSection = "";
        for (RenderedRow row : rows) {
            if (mode == LensScanMode.DEEP && !row.sectionTitle().equals(lastSection)) {
                height += 11;
                lastSection = row.sectionTitle();
            }
            height += 12;
        }
        return height;
    }

    private static LensHudLayout.ActionStrip actionStrip(Font font, int width, List<LensAction> actions) {
        if (actions.isEmpty() || !LensConfig.bool(LensConfig.SHOW_ACTIONS, true)) {
            return new LensHudLayout.ActionStrip(List.of(), 0);
        }
        int[] widths = new int[actions.size()];
        for (int index = 0; index < actions.size(); index++) {
            LensAction action = actions.get(index);
            widths[index] = Math.max(46, font.width(action.icon() + " " + action.label().getString()) + 12);
        }
        return LensHudLayout.actionStrip(width, widths, 12, 5, 8);
    }

    private static String modeBadge(LensReport report, LensScanMode mode, List<RenderedRow> rows,
            int maxWidth, ToIntFunction<String> textWidth) {
        String modeText = switch (mode) {
            case COMPACT -> Component.translatable("echolens.overlay.mode.compact").getString();
            case EXPANDED -> Component.translatable("echolens.overlay.mode.expanded").getString();
            case DEEP -> Component.translatable("echolens.overlay.mode.deep").getString();
        };
        String namespace = report.sourceModId() == null || report.sourceModId().isBlank()
                ? "minecraft"
                : report.sourceModId();
        if (mode == LensScanMode.DEEP) {
            String sections = Component.translatable("echolens.overlay.badge.sections", sectionCount(rows)).getString();
            if (LensConfig.bool(LensConfig.SHOW_SERVER_SCAN_STATUS, true)) {
                String status = LensServerScanClientState.statusLabel();
                return LensHudLayout.firstFitting(List.of(
                        namespace + " / " + modeText + " / " + status + " / " + sections,
                        compactSource(namespace) + " / " + modeText + " / " + status,
                        modeText + " / " + status), maxWidth, textWidth);
            }
            return LensHudLayout.firstFitting(List.of(
                    namespace + " / " + modeText + " / " + sections,
                    compactSource(namespace) + " / " + modeText,
                    modeText), maxWidth, textWidth);
        }
        return LensHudLayout.firstFitting(List.of(
                namespace + " / " + modeText,
                compactSource(namespace) + " / " + modeText,
                modeText), maxWidth, textWidth);
    }

    private static String compactSource(String namespace) {
        if (namespace == null || namespace.length() <= 12) {
            return namespace == null ? "" : namespace;
        }
        return namespace.substring(0, 9) + "...";
    }

    private static LensReport withServerSections(LensReport report) {
        List<LensInfoSection> serverSections = LensServerScanClientState.sections();
        if (serverSections.isEmpty()) {
            return report;
        }
        List<LensInfoSection> sections = new ArrayList<>(report.sections());
        sections.addAll(serverSections);
        return new LensReport(report.title(), report.subtitle(), report.icon(), report.targetKind(), report.targetId(),
                report.sourceModId(), sections, report.actions());
    }

    private static int sectionCount(List<RenderedRow> rows) {
        return (int) rows.stream().map(RenderedRow::sectionTitle).distinct().count();
    }

    private static List<RenderedRow> flatten(LensReport report, LensScanMode mode) {
        int maxRows = switch (mode) {
            case COMPACT -> LensConfig.integer(LensConfig.COMPACT_ROW_LIMIT, 4);
            case EXPANDED -> LensConfig.integer(LensConfig.EXPANDED_ROW_LIMIT, 12);
            case DEEP -> LensConfig.integer(LensConfig.DEEP_ROW_LIMIT, 40);
        };
        List<RenderedRow> rows = new ArrayList<>();
        int sectionLimit = mode == LensScanMode.COMPACT ? 3 : Integer.MAX_VALUE;
        int sections = 0;
        for (LensInfoSection section : report.sections()) {
            if (sections >= sectionLimit || rows.size() >= maxRows) {
                break;
            }
            int before = rows.size();
            for (LensInfoRow row : LensInspectionService.visibleRows(section, mode)) {
                if (rows.size() >= maxRows) {
                    break;
                }
                rows.add(new RenderedRow(section.title().getString(), section.icon(), row));
            }
            if (rows.size() > before) {
                sections++;
            }
        }
        return rows;
    }

    private static int panelWidth(LensScanMode mode) {
        int base = switch (mode) {
            case COMPACT -> 230;
            case EXPANDED -> 276;
            case DEEP -> 332;
        };
        return Math.round(base * (float) LensConfig.decimal(LensConfig.SCALE, 1.0D));
    }

    private static int panelHeight(LensScanMode mode) {
        return switch (mode) {
            case COMPACT -> 108;
            case EXPANDED -> 210;
            case DEEP -> 320;
        };
    }

    private static int panelX(int screenW, int width) {
        int offset = LensConfig.integer(LensConfig.OFFSET_X, 0);
        return switch (LensConfig.value(LensConfig.OVERLAY_POSITION, LensConfig.OverlayPosition.TOP_CENTER)) {
            case TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> 12 + offset;
            case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> screenW - width - 12 + offset;
            case TOP_CENTER, BOTTOM_CENTER -> (screenW - width) / 2 + offset;
        };
    }

    private static int panelY(int screenH, int height) {
        int offset = LensConfig.integer(LensConfig.OFFSET_Y, 12);
        return switch (LensConfig.value(LensConfig.OVERLAY_POSITION, LensConfig.OverlayPosition.TOP_CENTER)) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> 10 + offset;
            case CENTER_LEFT, CENTER_RIGHT -> (screenH - height) / 2 + offset;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> screenH - height - 30 + offset;
        };
    }

    private static float approach(float value, float target, float partialTick) {
        if (LensConfig.bool(LensConfig.REDUCED_MOTION, false) || !LensConfig.bool(LensConfig.ANIMATION, true)) {
            return target;
        }
        float speed = Math.max(0.15F, partialTick * 0.35F);
        if (value < target) {
            return Math.min(target, value + speed);
        }
        return Math.max(target, value - speed);
    }

    private static boolean shiftDown(Minecraft minecraft) {
        if (nativeLoaderActive()) {
            return false;
        }
        return InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(minecraft.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private static boolean deepScanDown(Minecraft minecraft) {
        if (forcedDeepScanTicks > 0) {
            forcedDeepScanTicks--;
            return true;
        }
        if (nativeLoaderActive()) {
            return false;
        }
        if (EchoLensClient.DEEP_SCAN_KEY.isDown()) {
            return true;
        }
        return false;
    }

    private static boolean nativeLoaderActive() {
        return EchoNativeRuntimeEnvironmentBridge.isNativeLoaderActive();
    }

    private static String fit(Font font, String text, int maxWidth) {
        if (text == null || text.isBlank() || font.width(text) <= maxWidth) {
            return text == null ? "" : text;
        }
        String ellipsis = "...";
        String trimmed = text;
        while (!trimmed.isEmpty() && font.width(trimmed + ellipsis) > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + ellipsis;
    }

    private static void drawFrameChrome(GuiGraphicsExtractor graphics, LensTheme theme, float opacity,
            int x, int y, int width, int height) {
        if (!renderCoreFrame(graphics, x, y, width, height)) {
            graphics.outline(x, y, width, height, theme.alpha(theme.border(), opacity));
            graphics.fill(x, y, x + Math.max(32, width / 5), y + 2, theme.alpha(theme.echo(), opacity));
            graphics.fill(x, y + height - 2, x + Math.max(28, width / 7), y + height,
                    theme.alpha(theme.glow(), opacity));
        }
    }

    private static boolean renderCoreFrame(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        try {
            Object rendered = Class.forName("com.knoxhack.echolens.integration.LensRenderCoreScreenIntegration")
                    .getMethod("drawLensFrame", GuiGraphicsExtractor.class, int.class, int.class, int.class, int.class)
                    .invoke(null, graphics, x, y, width, height);
            if (rendered instanceof Boolean) {
                return ((Boolean) rendered).booleanValue();
            }
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return false;
        }
    }

    private static Map<String, Object> actionDispatch(
            Map<String, Object> actionEntry,
            Map<String, Object> actionMetadata,
            Map<String, Object> routeMetadata
    ) {
        Map<String, Object> safeRouteMetadata = routeMetadata == null ? Map.of() : routeMetadata;
        Map<String, Object> dispatch = new LinkedHashMap<>();
        dispatch.put("routeActionId", actionEntry.getOrDefault("actionId", ""));
        dispatch.put("routeKind", actionEntry.getOrDefault("kind", ""));
        dispatch.put("routeMode", actionEntry.getOrDefault("mode", ""));
        dispatch.put("metadataKeys", actionMetadata == null ? List.of() : List.copyOf(actionMetadata.keySet()));
        dispatch.put("routeMetadataKeys", List.copyOf(safeRouteMetadata.keySet()));
        dispatch.put("routeSource", clean(value(safeRouteMetadata, "source"), ""));
        dispatch.put("routeEventType", clean(value(safeRouteMetadata, "eventType"), ""));
        dispatch.put("routeService", clean(value(safeRouteMetadata, "service"), ""));
        dispatch.put("deterministicNativeRouteDispatch", true);
        dispatch.put("silentFallback", false);
        return Map.copyOf(dispatch);
    }

    private static Map<String, Object> reportState(LensReport report) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("present", report != null && !report.isEmpty());
        if (report == null || report.isEmpty()) {
            return Map.copyOf(state);
        }
        state.put("title", report.title().getString());
        state.put("subtitle", report.subtitle().getString());
        state.put("targetKind", report.targetKind().name());
        state.put("targetId", report.targetId() == null ? "" : report.targetId().toString());
        state.put("sourceModId", report.sourceModId());
        state.put("icon", itemStackState(report.icon()));
        state.put("sectionCount", report.sections().size());
        state.put("rowCount", report.sections().stream().mapToInt(section -> section.rows().size()).sum());
        state.put("sections", report.sections().stream().map(LensHudOverlay::sectionState).toList());
        state.put("actions", report.actions().stream().map(LensHudOverlay::actionState).toList());
        return Map.copyOf(state);
    }

    private static Map<String, Object> targetDetectionState(LensContext context, LensReport report) {
        Map<String, Object> target = new LinkedHashMap<>();
        target.put("targetPresent", context != null && report != null && !report.isEmpty());
        target.put("targetKind", context == null ? "MISS" : context.targetKind().name());
        target.put("scanMode", context == null ? "NONE" : context.scanMode().name());
        target.put("accessPolicy", context == null ? "" : context.accessPolicy().name());
        target.put("blockPosition", context == null || context.blockPos() == null ? "" : context.blockPos().toShortString());
        target.put("entityId", context != null && context.hasEntity() ? context.entity().getId() : -1);
        target.put("targetId", report == null || report.targetId() == null ? "" : report.targetId().toString());
        target.put("sourceModId", report == null ? "" : report.sourceModId());
        target.put("currentTargetStack", itemStackState(currentTargetStack));
        target.put("liveHitResultBacked", context != null);
        return Map.copyOf(target);
    }

    private static Map<String, Object> sectionState(LensInfoSection section) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("id", section.id().toString());
        state.put("category", section.category().name());
        state.put("title", section.title().getString());
        state.put("icon", section.icon());
        state.put("tone", section.tone().name());
        state.put("visibility", section.visibility().name());
        state.put("rows", section.rows().stream().map(LensHudOverlay::rowState).toList());
        return Map.copyOf(state);
    }

    private static Map<String, Object> rowState(LensInfoRow row) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("label", row.label().getString());
        state.put("value", row.value().getString());
        state.put("icon", row.icon());
        state.put("tone", row.tone().name());
        state.put("visibility", row.visibility().name());
        return Map.copyOf(state);
    }

    private static Map<String, Object> actionState(LensAction action) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("id", action.id().toString());
        state.put("label", action.label().getString());
        state.put("hint", action.hint().getString());
        state.put("icon", action.icon());
        state.put("tone", action.tone().name());
        state.put("available", action.available());
        return Map.copyOf(state);
    }

    private static Map<String, Object> hitState(Minecraft minecraft, LensContext context) {
        Map<String, Object> state = new LinkedHashMap<>();
        HitResult hit = minecraft.hitResult;
        state.put("present", hit != null && hit.getType() != HitResult.Type.MISS);
        state.put("type", hit == null ? "NONE" : hit.getType().name());
        if (context != null) {
            state.put("targetKind", context.targetKind().name());
            state.put("scanMode", context.scanMode().name());
            state.put("accessPolicy", context.accessPolicy().name());
            state.put("blockPos", context.blockPos() == null ? "" : context.blockPos().toShortString());
            state.put("entityId", context.hasEntity() ? context.entity().getId() : -1);
            state.put("dimension", context.level() == null ? "" : context.level().dimension().identifier().toString());
        }
        return Map.copyOf(state);
    }

    private static Map<String, Object> itemStackState(ItemStack stack) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("present", stack != null && !stack.isEmpty());
        if (stack != null && !stack.isEmpty()) {
            state.put("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            state.put("name", stack.getHoverName().getString());
            state.put("count", stack.getCount());
        }
        return Map.copyOf(state);
    }

    private static Map<String, Object> serverScanState() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("status", LensServerScanClientState.status().name());
        state.put("label", LensServerScanClientState.statusLabel());
        state.put("sectionCount", LensServerScanClientState.sections().size());
        state.put("sections", LensServerScanClientState.sections().stream().map(LensHudOverlay::sectionState).toList());
        return Map.copyOf(state);
    }

    private static Map<String, Object> scannerRoute(Map<String, Object> snapshot) {
        Map<String, Object> route = new LinkedHashMap<>();
        route.put("scannerRouteReady", true);
        route.put("targetDetectionReady", Boolean.TRUE.equals(snapshot.get("nativeLensTargetDetectionReady")));
        route.put("overlayReady", Boolean.TRUE.equals(snapshot.get("nativeLensOverlayReady")));
        route.put("actionFeedbackReady", Boolean.TRUE.equals(snapshot.get("nativeLensActionFeedbackReady")));
        route.put("serverDeepScanStatus", LensServerScanClientState.status().name());
        route.put("serverDeepScanLabel", LensServerScanClientState.statusLabel());
        return Map.copyOf(route);
    }

    private static List<Map<String, Object>> actionHistory() {
        return ACTION_HISTORY.stream()
                .map(Map::copyOf)
                .toList();
    }

    private static Map<String, Object> lastActionFeedback(Map<String, Object> snapshot) {
        Map<String, Object> action = ACTION_HISTORY.peekFirst();
        if (action == null) {
            return Map.of(
                    "lastActionId", "",
                    "lastOutcome", "",
                    "handled", false,
                    "clientFeedbackVisible", false);
        }
        return actionFeedback(action, snapshot);
    }

    private static Map<String, Object> actionFeedback(Map<String, Object> actionEntry, Map<String, Object> snapshot) {
        Map<String, Object> feedback = new LinkedHashMap<>();
        feedback.put("lastActionId", actionEntry.getOrDefault("actionId", ""));
        feedback.put("lastOutcome", actionEntry.getOrDefault("outcome", ""));
        feedback.put("handled", actionEntry.getOrDefault("handled", false));
        feedback.put("serverScan", snapshot.get("serverScan"));
        feedback.put("reportActions", reportActions(snapshot.get("report")));
        feedback.put("clientFeedbackVisible", true);
        return Map.copyOf(feedback);
    }

    private static Map<String, Object> routeDrivenOverlayModel(
            Map<String, Object> actionEntry,
            Map<String, Object> snapshot,
            Map<String, Object> routeMetadata
    ) {
        Map<String, Object> model = new LinkedHashMap<>();
        Map<String, Object> safeRouteMetadata = routeMetadata == null ? Map.of() : routeMetadata;
        model.put("modelType", "lens_overlay_route");
        model.put("surface", "lens");
        model.put("routeDrivenOverlayState", true);
        model.put("renderer", LensHudOverlay.class.getName());
        model.put("actionId", actionEntry.getOrDefault("actionId", ""));
        model.put("kind", actionEntry.getOrDefault("kind", ""));
        model.put("mode", actionEntry.getOrDefault("mode", ""));
        model.put("handled", actionEntry.getOrDefault("handled", false));
        model.put("outcome", actionEntry.getOrDefault("outcome", ""));
        model.put("overlayReady", snapshot.get("nativeLensOverlayReady"));
        model.put("targetDetectionReady", snapshot.get("nativeLensTargetDetectionReady"));
        model.put("actionFeedbackReady", snapshot.get("nativeLensActionFeedbackReady"));
        model.put("targetDetection", snapshot.get("targetDetection"));
        model.put("scannerRoute", snapshot.get("nativeLensScannerRoute"));
        model.put("actionFeedback", snapshot.get("nativeLensLastActionFeedback"));
        model.put("targetStack", actionEntry.get("targetStack"));
        model.put("serverScan", actionEntry.get("serverScan"));
        model.put("routeMetadata", Map.copyOf(safeRouteMetadata));
        model.put("routeMetadataKeys", safeRouteMetadata.keySet().stream().sorted().toList());
        putIfPresent(model, "routeSource", safeRouteMetadata.get("source"));
        putIfPresent(model, "routeEventType", safeRouteMetadata.get("eventType"));
        putIfPresent(model, "routeService", safeRouteMetadata.get("service"));
        putIfPresent(model, "routeFrameSource", safeRouteMetadata.get("frameSource"));
        putIfPresent(model, "routePartialTick", safeRouteMetadata.get("partialTick"));
        putIfPresent(model, "routeItemId", safeRouteMetadata.get("itemId"));
        return Map.copyOf(model);
    }

    private static List<Object> reportActions(Object report) {
        if (!(report instanceof Map<?, ?> map)) {
            return List.of();
        }
        Object actions = map.get("actions");
        return actions instanceof List<?> list ? List.copyOf(list) : List.of();
    }

    private static Object value(Map<String, Object> map, String key) {
        return map == null ? "" : map.get(key);
    }

    private static String clean(Object value, String fallback) {
        String text = value == null ? "" : String.valueOf(value).trim();
        return text.isBlank() ? fallback : text;
    }

    private static void putIfPresent(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private static void push(Map<String, Object> actionEntry) {
        ACTION_HISTORY.addFirst(Map.copyOf(actionEntry));
        while (ACTION_HISTORY.size() > HISTORY_LIMIT) {
            ACTION_HISTORY.removeLast();
        }
    }

    private record RenderedRow(String sectionTitle, String sectionIcon, LensInfoRow row) {
    }

    private record ReportCacheKey(
            LensTargetKind targetKind,
            LensScanMode scanMode,
            LensAccessPolicy accessPolicy,
            String dimension,
            long blockPos,
            int entityId,
            int stateHash,
            int fluidHash,
            int configHash) {
        private static ReportCacheKey from(LensContext context) {
            String dimension = context.level() == null
                    ? ""
                    : context.level().dimension().identifier().toString();
            long blockPos = context.blockPos() == null ? Long.MIN_VALUE : context.blockPos().asLong();
            int entityId = context.hasEntity() ? context.entity().getId() : -1;
            int stateHash = context.blockState() == null ? 0 : context.blockState().hashCode();
            int fluidHash = context.fluidState() == null ? 0 : context.fluidState().hashCode();
            return new ReportCacheKey(context.targetKind(), context.scanMode(), context.accessPolicy(),
                    dimension, blockPos, entityId, stateHash, fluidHash, calculateConfigHash());
        }

        private static int calculateConfigHash() {
            int hash = 17;
            hash = 31 * hash + Boolean.hashCode(LensConfig.bool(LensConfig.SHOW_IDENTITY, true));
            hash = 31 * hash + Boolean.hashCode(LensConfig.bool(LensConfig.SHOW_BLOCK, true));
            hash = 31 * hash + Boolean.hashCode(LensConfig.bool(LensConfig.SHOW_ENTITY, true));
            hash = 31 * hash + Boolean.hashCode(LensConfig.bool(LensConfig.SHOW_FLUID, true));
            hash = 31 * hash + Boolean.hashCode(LensConfig.bool(LensConfig.SHOW_MACHINE, true));
            hash = 31 * hash + Boolean.hashCode(LensConfig.bool(LensConfig.SHOW_INVENTORY, true));
            hash = 31 * hash + Boolean.hashCode(LensConfig.bool(LensConfig.SHOW_INTEGRATION, true));
            hash = 31 * hash + Boolean.hashCode(LensConfig.bool(LensConfig.BEGINNER_HINTS, true));
            hash = 31 * hash + Boolean.hashCode(LensConfig.bool(LensConfig.SHOW_ACTIONS, true));
            hash = 31 * hash + LensConfig.integer(LensConfig.COMPACT_ROW_LIMIT, 4);
            hash = 31 * hash + LensConfig.integer(LensConfig.EXPANDED_ROW_LIMIT, 12);
            hash = 31 * hash + LensConfig.integer(LensConfig.DEEP_ROW_LIMIT, 40);
            return hash;
        }
    }
}
