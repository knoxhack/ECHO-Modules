package com.knoxhack.echoterminal.client.mission;

import com.knoxhack.echoterminal.EchoTerminal;
import com.knoxhack.echoterminal.api.TerminalUi;
import com.knoxhack.echoterminal.api.mission.TerminalMissionChapter;
import com.knoxhack.echoterminal.api.mission.TerminalMissionDefinition;
import com.knoxhack.echoterminal.api.mission.TerminalMissionPresentation;
import com.knoxhack.echoterminal.api.mission.TerminalMissionProvider;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRegistry;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRole;
import com.knoxhack.echoterminal.api.mission.TerminalMissionSnapshot;
import com.knoxhack.echoterminal.api.mission.TerminalMissionStatus;
import com.knoxhack.echoterminal.api.mission.TerminalMissionVisuals;
import com.knoxhack.echoterminal.client.hud.TerminalHudNotice;
import com.knoxhack.echoterminal.client.hud.TerminalHudNoticeSurface;
import com.knoxhack.echoterminal.client.screen.EchoTerminalScreens;
import com.knoxhack.echoterminal.client.screen.TerminalClientOptions;
import com.knoxhack.echoterminal.mission.MainSurvivalQuestProvider;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class TerminalMissionHudController {
    private static final TerminalMissionHudController INSTANCE = new TerminalMissionHudController();
    private static final int PASSIVE_POLL_INTERVAL_TICKS = 100;
    private static final int NOTICE_DURATION_FRAMES = 220;
    private static final int NOTICE_COOLDOWN_TICKS = 80;
    private static final int MAX_PENDING_NOTICES = 3;
    private static final long SLOW_SCAN_WARN_NANOS = 12_000_000L;
    private static final long SLOW_SCAN_LOG_COOLDOWN_TICKS = 200L;
    private static final Identifier SUMMARY_ID =
            Identifier.fromNamespaceAndPath(EchoTerminal.MODID, "mission_notice_summary");

    private final Map<MissionKey, MissionState> missionStates = new HashMap<>();
    private final Map<PhaseKey, PhaseState> phaseStates = new HashMap<>();
    private final Map<NoticeKey, Long> cooldowns = new HashMap<>();
    private final ArrayDeque<TerminalMissionNotice> pendingNotices = new ArrayDeque<>();
    private TerminalMissionNotice activeNotice;
    private int activeFrames;
    private UUID playerId;
    private String worldKey = "";
    private long lastPollTick = Long.MIN_VALUE;
    private long lastSlowScanLogTick = Long.MIN_VALUE;
    private int nextProviderIndex;
    private int baselineProviderScans;
    private boolean baselineReady;

    public static void tick() {
        INSTANCE.tickInternal(Minecraft.getInstance());
    }

    public static void render(GuiGraphicsExtractor graphics, float partialTick) {
        INSTANCE.renderInternal(graphics, partialTick);
    }

    public static java.util.Optional<TerminalHudNotice> activeNoticeForHud() {
        TerminalMissionNotice notice = INSTANCE.activeNotice;
        return notice == null ? java.util.Optional.empty() : java.util.Optional.of(toHudNotice(notice));
    }

    public TerminalMissionHudController() {
    }

    public void scanForTests(Player player, long gameTime) {
        scanAll(player, TerminalMissionRegistry.providers(), gameTime);
    }

    public List<TerminalMissionNotice> drainQueuedNoticesForTests() {
        List<TerminalMissionNotice> notices = new ArrayList<>(pendingNotices);
        pendingNotices.clear();
        return notices;
    }

    public void resetForTests() {
        resetAll();
    }

    public static TerminalHudNotice noticeForHudForTests(TerminalMissionNotice notice) {
        return toHudNotice(notice);
    }

    private void tickInternal(Minecraft minecraft) {
        if (!TerminalClientOptions.missionHudNotifications) {
            resetAll();
            return;
        }
        Player player = minecraft.player;
        if (player == null || minecraft.level == null) {
            resetAll();
            return;
        }

        ensureScope(player);
        if (EchoTerminalScreens.isManagedTerminalScreen(minecraft.screen)) {
            return;
        }
        long gameTime = player.level() == null ? 0L : player.level().getGameTime();
        int pollInterval = PASSIVE_POLL_INTERVAL_TICKS;
        if (lastPollTick == Long.MIN_VALUE || gameTime - lastPollTick >= pollInterval) {
            lastPollTick = gameTime;
            long start = System.nanoTime();
            scanNextProvider(player, TerminalMissionRegistry.providers(), gameTime);
            logSlowScan(gameTime, start);
        }

        if (minecraft.screen == null && !minecraft.options.hideGui) {
            advanceVisibleNotice();
        }
    }

    private void renderInternal(GuiGraphicsExtractor graphics, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!TerminalClientOptions.missionHudNotifications || minecraft.player == null
                || minecraft.options.hideGui || minecraft.screen != null || activeNotice == null) {
            return;
        }
        if (!TerminalHudNoticeSurface.shouldRenderInternalCards()) {
            return;
        }
        renderNotice(graphics, activeNotice, activeFrames + partialTick, TerminalClientOptions.reduceMotion(),
                !TerminalClientOptions.useVisualAssets());
    }

    private void scanAll(Player player, List<TerminalMissionProvider> providers, long gameTime) {
        cleanupCooldowns(gameTime);
        boolean initializing = !baselineReady;
        List<TerminalMissionNotice> notices = new ArrayList<>();
        for (TerminalMissionProvider provider : activeProviders(providers)) {
            notices.addAll(scanProvider(player, provider, initializing));
        }
        baselineReady = true;
        baselineProviderScans = 0;
        nextProviderIndex = 0;
        enqueueNotices(notices, gameTime);
    }

    private void scanNextProvider(Player player, List<TerminalMissionProvider> providers, long gameTime) {
        cleanupCooldowns(gameTime);
        List<TerminalMissionProvider> activeProviders = activeProviders(providers);
        if (activeProviders.isEmpty()) {
            baselineReady = true;
            baselineProviderScans = 0;
            nextProviderIndex = 0;
            return;
        }
        if (nextProviderIndex >= activeProviders.size()) {
            nextProviderIndex = 0;
        }
        boolean initializing = !baselineReady;
        TerminalMissionProvider provider = activeProviders.get(nextProviderIndex);
        nextProviderIndex = (nextProviderIndex + 1) % activeProviders.size();
        List<TerminalMissionNotice> notices = scanProvider(player, provider, initializing);
        if (initializing && ++baselineProviderScans >= activeProviders.size()) {
            baselineReady = true;
            baselineProviderScans = 0;
        }
        enqueueNotices(notices, gameTime);
    }

    private List<TerminalMissionNotice> scanProvider(
            Player player,
            TerminalMissionProvider provider,
            boolean initializing) {
        TerminalMissionChapter chapter = safeChapter(provider);
        if (chapter == null || MainSurvivalQuestProvider.CHAPTER_ID.equals(chapter.id())) {
            return List.of();
        }

        Map<MissionKey, MissionState> nextMissions = new HashMap<>();
        Map<PhaseKey, PhaseState> nextPhases = new HashMap<>();
        List<TerminalMissionNotice> notices = new ArrayList<>();
        for (TerminalMissionDefinition definition : safeMissions(provider, player)) {
            if (definition == null) {
                continue;
            }
            TerminalMissionSnapshot snapshot = safeSnapshot(provider, player, definition);
            TerminalMissionPresentation presentation = safePresentation(provider, player, definition, snapshot);
            TerminalMissionVisuals visuals = safeVisuals(provider, player, definition, snapshot);
            TerminalMissionRole role = safeRole(provider, player, definition, snapshot);
            MissionState state = MissionState.of(chapter, definition, snapshot, presentation, visuals, role);
            MissionKey key = new MissionKey(chapter.id(), definition.id());
            nextMissions.put(key, state);

            if (state.phaseActive()) {
                nextPhases.putIfAbsent(state.phaseKey(), state.phaseState());
            }

            if (!initializing) {
                MissionState previous = missionStates.get(key);
                if (previous == null) {
                    noticeForNewMission(state).ifPresent(notices::add);
                } else {
                    notices.addAll(noticesForTransition(previous, state));
                }
            }
        }

        if (!initializing) {
            for (Map.Entry<PhaseKey, PhaseState> entry : nextPhases.entrySet()) {
                if (!phaseStates.containsKey(entry.getKey())) {
                    notices.add(entry.getValue().notice());
                }
            }
        }

        Identifier chapterId = chapter.id();
        missionStates.keySet().removeIf(key -> chapterId.equals(key.chapterId()));
        missionStates.putAll(nextMissions);
        phaseStates.keySet().removeIf(key -> chapterId.equals(key.chapterId()));
        phaseStates.putAll(nextPhases);
        return notices;
    }

    private static List<TerminalMissionProvider> activeProviders(List<TerminalMissionProvider> providers) {
        if (providers == null || providers.isEmpty()) {
            return List.of();
        }
        List<TerminalMissionProvider> active = new ArrayList<>();
        for (TerminalMissionProvider provider : providers) {
            if (!skipProvider(provider)) {
                active.add(provider);
            }
        }
        return active;
    }

    private static boolean skipProvider(TerminalMissionProvider provider) {
        return provider == null || provider == MainSurvivalQuestProvider.INSTANCE;
    }

    private static java.util.Optional<TerminalMissionNotice> noticeForNewMission(MissionState state) {
        if (state.status() == TerminalMissionStatus.CLAIMABLE) {
            return java.util.Optional.of(state.notice(TerminalMissionNoticeType.CACHE_READY));
        }
        if (state.status() == TerminalMissionStatus.COMPLETED) {
            return java.util.Optional.of(state.notice(TerminalMissionNoticeType.OBJECTIVE_READY));
        }
        if (state.status() == TerminalMissionStatus.UNLOCKED) {
            return java.util.Optional.of(state.notice(TerminalMissionNoticeType.MISSION_AVAILABLE));
        }
        return java.util.Optional.empty();
    }

    private static List<TerminalMissionNotice> noticesForTransition(MissionState previous, MissionState current) {
        List<TerminalMissionNotice> notices = new ArrayList<>();
        if (current.status() == TerminalMissionStatus.CLAIMED
                && previous.status() != TerminalMissionStatus.CLAIMED) {
            notices.add(current.notice(TerminalMissionNoticeType.CACHE_CLAIMED));
        } else if (current.status() == TerminalMissionStatus.CLAIMABLE
                && previous.status() != TerminalMissionStatus.CLAIMABLE) {
            notices.add(current.notice(TerminalMissionNoticeType.CACHE_READY));
        } else if (current.status() == TerminalMissionStatus.COMPLETED
                && previous.status() != TerminalMissionStatus.COMPLETED) {
            notices.add(current.notice(TerminalMissionNoticeType.OBJECTIVE_READY));
        } else if (current.status() == TerminalMissionStatus.UNLOCKED
                && previous.status() != TerminalMissionStatus.UNLOCKED
                && !available(previous.status())) {
            notices.add(current.notice(TerminalMissionNoticeType.MISSION_AVAILABLE));
        } else if (previous.progress() < 1.0F && current.progress() >= 1.0F
                && current.status() == TerminalMissionStatus.UNLOCKED) {
            notices.add(current.notice(TerminalMissionNoticeType.OBJECTIVE_READY));
        }
        return notices;
    }

    private void enqueueNotices(List<TerminalMissionNotice> notices, long gameTime) {
        if (notices == null || notices.isEmpty()) {
            return;
        }
        List<TerminalMissionNotice> filtered = new ArrayList<>();
        for (TerminalMissionNotice notice : notices) {
            NoticeKey key = NoticeKey.of(notice);
            Long cooldownUntil = cooldowns.get(key);
            if (cooldownUntil == null || gameTime >= cooldownUntil) {
                filtered.add(notice);
                cooldowns.put(key, gameTime + NOTICE_COOLDOWN_TICKS);
            }
        }
        if (filtered.isEmpty()) {
            return;
        }
        if (filtered.size() > MAX_PENDING_NOTICES) {
            enqueue(summaryNotice(filtered));
            return;
        }
        for (TerminalMissionNotice notice : filtered) {
            enqueue(notice);
        }
    }

    private void enqueue(TerminalMissionNotice notice) {
        while (pendingNotices.size() >= MAX_PENDING_NOTICES) {
            pendingNotices.removeFirst();
        }
        pendingNotices.addLast(notice);
    }

    private static TerminalMissionNotice summaryNotice(List<TerminalMissionNotice> notices) {
        TerminalMissionNotice first = notices.get(0);
        return new TerminalMissionNotice(
                TerminalMissionNoticeType.SUMMARY,
                first.chapterId(),
                SUMMARY_ID,
                "ECHO Network",
                notices.size() + " mission signals updated",
                "Open the ECHO Terminal to review the refreshed route state.",
                "Provider sync",
                "SYNC",
                ItemStack.EMPTY,
                first.accentColor(),
                0.0F,
                notices.size());
    }

    private void advanceVisibleNotice() {
        if (activeNotice == null) {
            activeNotice = pendingNotices.pollFirst();
            activeFrames = 0;
            return;
        }
        activeFrames++;
        if (activeFrames >= NOTICE_DURATION_FRAMES) {
            activeNotice = pendingNotices.pollFirst();
            activeFrames = 0;
        }
    }

    private void ensureScope(Player player) {
        UUID nextPlayer = player.getUUID();
        String nextWorld = player.level() == null ? "" : player.level().dimension().identifier().toString();
        if (!Objects.equals(playerId, nextPlayer) || !Objects.equals(worldKey, nextWorld)) {
            resetAll();
            playerId = nextPlayer;
            worldKey = nextWorld;
        }
    }

    private void resetAll() {
        missionStates.clear();
        phaseStates.clear();
        cooldowns.clear();
        pendingNotices.clear();
        activeNotice = null;
        activeFrames = 0;
        lastPollTick = Long.MIN_VALUE;
        lastSlowScanLogTick = Long.MIN_VALUE;
        nextProviderIndex = 0;
        baselineProviderScans = 0;
        baselineReady = false;
    }

    private void cleanupCooldowns(long gameTime) {
        cooldowns.entrySet().removeIf(entry -> gameTime >= entry.getValue());
    }

    private static boolean available(TerminalMissionStatus status) {
        return status == TerminalMissionStatus.UNLOCKED
                || status == TerminalMissionStatus.CLAIMABLE
                || status == TerminalMissionStatus.COMPLETED
                || status == TerminalMissionStatus.CLAIMED;
    }

    private void logSlowScan(long gameTime, long startNanos) {
        long elapsed = System.nanoTime() - startNanos;
        if (elapsed < SLOW_SCAN_WARN_NANOS) {
            return;
        }
        if (lastSlowScanLogTick != Long.MIN_VALUE
                && gameTime - lastSlowScanLogTick < SLOW_SCAN_LOG_COOLDOWN_TICKS) {
            return;
        }
        lastSlowScanLogTick = gameTime;
        EchoTerminal.LOGGER.warn("Mission HUD provider poll took {} ms.",
                String.format(java.util.Locale.ROOT, "%.2f", elapsed / 1_000_000.0D));
    }

    private static TerminalMissionChapter safeChapter(TerminalMissionProvider provider) {
        try {
            return provider.chapter();
        } catch (RuntimeException exception) {
            EchoTerminal.LOGGER.debug("Mission HUD skipped provider with failing chapter metadata.", exception);
            return null;
        }
    }

    private static List<TerminalMissionDefinition> safeMissions(TerminalMissionProvider provider, Player player) {
        try {
            List<TerminalMissionDefinition> missions = provider.missions(player);
            return missions == null ? List.of() : missions;
        } catch (RuntimeException exception) {
            EchoTerminal.LOGGER.debug("Mission HUD skipped provider with failing mission list.", exception);
            return List.of();
        }
    }

    private static TerminalMissionSnapshot safeSnapshot(
            TerminalMissionProvider provider, Player player, TerminalMissionDefinition definition) {
        try {
            TerminalMissionSnapshot snapshot = provider.snapshot(player, definition.id());
            return snapshot == null
                    ? new TerminalMissionSnapshot(definition.id(), TerminalMissionStatus.LOCKED,
                            0.0F, "LOCKED", "Mission provider returned no snapshot.", "", List.of())
                    : snapshot;
        } catch (RuntimeException exception) {
            EchoTerminal.LOGGER.debug("Mission HUD used a fallback snapshot after provider failure.", exception);
            return new TerminalMissionSnapshot(definition.id(), TerminalMissionStatus.LOCKED,
                    0.0F, "LOCKED", "Mission provider snapshot failed.", "", List.of());
        }
    }

    private static TerminalMissionPresentation safePresentation(
            TerminalMissionProvider provider,
            Player player,
            TerminalMissionDefinition definition,
            TerminalMissionSnapshot snapshot) {
        try {
            TerminalMissionPresentation presentation = provider.presentation(player, definition, snapshot);
            return presentation == null ? TerminalMissionPresentation.fallback(definition, snapshot) : presentation;
        } catch (RuntimeException exception) {
            EchoTerminal.LOGGER.debug("Mission HUD used fallback presentation after provider failure.", exception);
            return TerminalMissionPresentation.fallback(definition, snapshot);
        }
    }

    private static TerminalMissionVisuals safeVisuals(
            TerminalMissionProvider provider,
            Player player,
            TerminalMissionDefinition definition,
            TerminalMissionSnapshot snapshot) {
        try {
            TerminalMissionVisuals visuals = provider.visuals(player, definition, snapshot);
            return visuals == null ? TerminalMissionVisuals.fallback(definition, snapshot) : visuals;
        } catch (RuntimeException exception) {
            EchoTerminal.LOGGER.debug("Mission HUD used fallback visuals after provider failure.", exception);
            return TerminalMissionVisuals.fallback(definition, snapshot);
        }
    }

    private static TerminalMissionRole safeRole(
            TerminalMissionProvider provider,
            Player player,
            TerminalMissionDefinition definition,
            TerminalMissionSnapshot snapshot) {
        try {
            TerminalMissionRole role = provider.role(player, definition, snapshot);
            return role == null ? TerminalMissionRole.fallback(definition, snapshot) : role;
        } catch (RuntimeException exception) {
            EchoTerminal.LOGGER.debug("Mission HUD used fallback role after provider failure.", exception);
            return TerminalMissionRole.fallback(definition, snapshot);
        }
    }

    private static void renderNotice(
            GuiGraphicsExtractor graphics, TerminalMissionNotice notice, float frameAge,
            boolean reducedMotion, boolean minimalVisuals) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        int screenW = minecraft.getWindow().getGuiScaledWidth();
        int w = Math.max(206, Math.min(304, screenW - 16));
        int h = 72;
        float enter = Math.min(1.0F, frameAge / 12.0F);
        float exit = frameAge > NOTICE_DURATION_FRAMES - 18
                ? Math.min(1.0F, (frameAge - (NOTICE_DURATION_FRAMES - 18)) / 18.0F)
                : 0.0F;
        boolean animated = !reducedMotion && !minimalVisuals;
        int slide = animated ? Math.round((1.0F - enter + exit) * 32.0F) : 0;
        int x = screenW - w - 8 + slide;
        int y = 10;
        int accent = notice.accentColor();
        TerminalUi.flatHudPanel(graphics, x, y, w, h, accent);
        graphics.fill(x + 6, y + 22, x + w - 6, y + h - 11, 0x8802070C);
        if (animated && frameAge % 24 < 12) {
            graphics.fill(x + 2, y + 2, x + Math.max(28, Math.min(w - 2, w / 4)), y + 4, accent);
        }

        String status = font.plainSubstrByWidth(notice.statusLabel(), 72);
        int statusW = font.width(status);
        String eyebrow = notice.type().label() + " // " + notice.chapterTitle();
        graphics.text(font, font.plainSubstrByWidth(eyebrow, Math.max(40, w - statusW - 28)),
                x + 10, y + 7, accent, false);
        graphics.text(font, status, x + w - 10 - statusW, y + 7, statusColor(notice.type()), false);

        int textX = x + 36;
        ItemStack icon = notice.icon();
        graphics.fill(x + 9, y + 26, x + 31, y + 48, 0xFF0D171F);
        graphics.outline(x + 9, y + 26, 22, 22, 0x552E8E9D);
        if (!icon.isEmpty()) {
            graphics.item(icon, x + 12, y + 29);
            graphics.itemDecorations(font, icon, x + 12, y + 29);
        } else {
            graphics.fill(x + 15, y + 36, x + 25, y + 38, TerminalUi.CYAN_DIM);
        }

        graphics.text(font, font.plainSubstrByWidth(notice.title(), Math.max(40, w - 48)),
                textX, y + 25, 0xFFE9FBFF, false);
        graphics.text(font, font.plainSubstrByWidth(notice.detail(), Math.max(40, w - 48)),
                textX, y + 38, 0xFFB9CAD8, false);
        String route = notice.routeHint().isBlank() ? notice.type().label() : notice.routeHint();
        graphics.text(font, font.plainSubstrByWidth(route, Math.max(40, w - 20)),
                x + 10, y + 54, 0xFF8CA7B5, false);

        if (notice.progress() > 0.0F) {
            int barW = w - 20;
            int fill = Math.max(3, Math.round(barW * notice.progress()));
            graphics.fill(x + 10, y + h - 6, x + 10 + barW, y + h - 3, 0xFF1D2A35);
            graphics.fill(x + 10, y + h - 6, x + 10 + fill, y + h - 3, accent);
        }
    }

    private static int statusColor(TerminalMissionNoticeType type) {
        return switch (type) {
            case CACHE_READY, CACHE_CLAIMED, OBJECTIVE_READY -> TerminalUi.GREEN;
            case PHASE_ONLINE, MISSION_AVAILABLE, SUMMARY -> TerminalUi.AMBER;
        };
    }

    private static TerminalHudNotice toHudNotice(TerminalMissionNotice notice) {
        return new TerminalHudNotice(
                notice.type().label(),
                notice.statusLabel(),
                notice.title(),
                notice.detail(),
                notice.routeHint().isBlank() ? notice.chapterTitle() : notice.routeHint(),
                notice.accentColor(),
                notice.progress(),
                notice.count());
    }

    private record MissionKey(Identifier chapterId, Identifier missionId) {
    }

    private record PhaseKey(Identifier chapterId, String phaseId) {
    }

    private record NoticeKey(TerminalMissionNoticeType type, Identifier chapterId, Identifier missionId) {
        static NoticeKey of(TerminalMissionNotice notice) {
            return new NoticeKey(notice.type(), notice.chapterId(), notice.missionId());
        }
    }

    private record MissionState(
            Identifier chapterId,
            Identifier missionId,
            String chapterTitle,
            String phaseId,
            String phaseTitle,
            String title,
            String detail,
            String routeHint,
            String statusLabel,
            ItemStack icon,
            int accent,
            float progress,
            TerminalMissionStatus status,
            TerminalMissionRole role,
            String visualTone) {
        static MissionState of(
                TerminalMissionChapter chapter,
                TerminalMissionDefinition definition,
                TerminalMissionSnapshot snapshot,
                TerminalMissionPresentation presentation,
                TerminalMissionVisuals visuals,
                TerminalMissionRole role) {
            String phaseId = definition.phaseOrder() + "::" + definition.phaseId();
            return new MissionState(
                    chapter.id(),
                    definition.id(),
                    chapter.title(),
                    phaseId,
                    definition.phaseTitle(),
                    presentation.shortTitle(),
                    presentation.nextStep(),
                    presentation.routeHint().isBlank() ? visuals.trackType() : presentation.routeHint(),
                    snapshot.statusLabel(),
                    definition.icon(),
                    chapter.accentColor(),
                    snapshot.progress(),
                    snapshot.status(),
                    role,
                    visuals.visualTone());
        }

        PhaseKey phaseKey() {
            return new PhaseKey(chapterId, phaseId);
        }

        boolean phaseActive() {
            return role != TerminalMissionRole.REFERENCE && available(status);
        }

        PhaseState phaseState() {
            return new PhaseState(chapterId, phaseId, chapterTitle, phaseTitle, icon, accent, visualTone);
        }

        TerminalMissionNotice notice(TerminalMissionNoticeType type) {
            return new TerminalMissionNotice(type, chapterId, missionId, chapterTitle, title,
                    detail, routeHint, statusLabel, icon, accent, progress, 1);
        }
    }

    private record PhaseState(
            Identifier chapterId,
            String phaseId,
            String chapterTitle,
            String phaseTitle,
            ItemStack icon,
            int accent,
            String visualTone) {
        TerminalMissionNotice notice() {
            Identifier missionId = Identifier.fromNamespaceAndPath(
                    chapterId.getNamespace(),
                    "phase/" + phaseId.toLowerCase(java.util.Locale.ROOT)
                            .replaceAll("[^a-z0-9_./-]+", "_"));
            return new TerminalMissionNotice(
                    TerminalMissionNoticeType.PHASE_ONLINE,
                    chapterId,
                    missionId,
                    chapterTitle,
                    phaseTitle,
                    "New mission phase available through the ECHO Terminal.",
                    visualTone,
                    "ONLINE",
                    icon,
                    accent,
                    0.0F,
                    1);
        }
    }
}
