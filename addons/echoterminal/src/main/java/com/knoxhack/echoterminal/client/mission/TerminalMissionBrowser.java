package com.knoxhack.echoterminal.client.mission;

import com.knoxhack.echoterminal.EchoTerminal;
import com.knoxhack.echoterminal.api.TerminalIcon;
import com.knoxhack.echoterminal.api.TerminalRenderCache;
import com.knoxhack.echoterminal.api.TerminalRenderContext;
import com.knoxhack.echoterminal.api.TerminalUi;
import com.knoxhack.echoterminal.api.TerminalVisualAssets;
import com.knoxhack.echoterminal.api.mission.TerminalMissionAction;
import com.knoxhack.echoterminal.api.mission.TerminalMissionActions;
import com.knoxhack.echoterminal.api.mission.TerminalMissionChapter;
import com.knoxhack.echoterminal.api.mission.TerminalMissionDefinition;
import com.knoxhack.echoterminal.api.mission.TerminalMissionIntelKind;
import com.knoxhack.echoterminal.api.mission.TerminalMissionIntelUnlock;
import com.knoxhack.echoterminal.api.mission.TerminalMissionPresentation;
import com.knoxhack.echoterminal.api.mission.TerminalMissionProvider;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRequirement;
import com.knoxhack.echoterminal.api.mission.TerminalMissionReward;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRole;
import com.knoxhack.echoterminal.api.mission.TerminalMissionSnapshot;
import com.knoxhack.echoterminal.api.mission.TerminalMissionStatus;
import com.knoxhack.echoterminal.api.mission.TerminalMissionVisuals;
import com.knoxhack.echoterminal.client.screen.TerminalClientOptions;
import com.knoxhack.echoterminal.client.screen.TerminalScrollbar;
import com.knoxhack.echoterminal.mission.MainSurvivalQuestProvider;
import com.knoxhack.echoterminal.player.TerminalPlayerData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntSupplier;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public final class TerminalMissionBrowser {
    private static final int PHASE_ROW_HEIGHT = 22;
    private static final int MISSION_ROW_HEIGHT = 30;
    private static final int ACTION_BAR_HEIGHT = 92;
    private static final int SIDE_CARD_HEIGHT = 66;
    private static final int SIDE_CARD_SECTION_HEADER_HEIGHT = 42;
    private static final int INTEL_ROW_HEIGHT = 27;
    private static final int TREE_FOCUS_EXTRA = 8;
    private static final int STATE_REFRESH_TICKS = 10;
    private static final int WIDTH_BUCKET_SIZE = 80;
    private static final int SPLIT_LAYOUT_MIN_WIDTH = 820;
    private static final int INTEL_LAYOUT_MIN_WIDTH = 1180;
    private static final String ACTIONS_SECTION_TITLE = "ACTIONS";
    private static final String EMPTY_REQUIREMENTS_COPY =
            "No checklist is needed for this record. Follow the next step above.";
    private static final String MET_REQUIREMENTS_COPY =
            "Requirements are clear. Use the available actions below.";

    public enum ActionMode {
        FULL_ACTIONS,
        TRACKING_ONLY
    }

    public enum InitialTreeFocus {
        ALIGN_SELECTED_TOP,
        TOP
    }

    public enum InitialSelection {
        FOCUS_RECORD,
        FIRST_RECORD
    }

    public enum DefaultPhaseExpansion {
        SMART,
        FIRST_ONLY
    }

    private enum SideCardState {
        LOCKED,
        ACTIVE,
        READY,
        ARCHIVED
    }

    private enum MissionFilter {
        ACTIVE("ACTIVE"),
        READY("READY"),
        LOCKED("LOCKED"),
        DONE("DONE"),
        ALL("ALL");

        private final String label;

        MissionFilter(String label) {
            this.label = label;
        }

        boolean includes(MissionRecord record) {
            if (record == null) {
                return false;
            }
            return switch (this) {
                case ACTIVE -> !record.phaseLocked()
                        && (record.snapshot().status() == TerminalMissionStatus.UNLOCKED
                                || record.snapshot().status() == TerminalMissionStatus.CLAIMABLE);
                case READY -> !record.phaseLocked()
                        && record.snapshot().status() == TerminalMissionStatus.CLAIMABLE;
                case LOCKED -> record.phaseLocked()
                        || record.snapshot().status() == TerminalMissionStatus.LOCKED
                        || record.snapshot().status() == TerminalMissionStatus.VIEW_ONLY;
                case DONE -> isDone(record.snapshot().status());
                case ALL -> true;
            };
        }
    }

    private enum MissionDisplayMode {
        VISUAL_RPG("VISUAL"),
        MINIMAL_FUTURE("MINIMAL");

        private final String label;

        MissionDisplayMode(String label) {
            this.label = label;
        }
    }

    private enum ScrollPane {
        NONE,
        TREE,
        DETAIL,
        INTEL
    }

    private final TerminalMissionProvider provider;
    private final Identifier tabId;
    private final int stateRefreshTicks;
    private final ActionMode actionMode;
    private final InitialTreeFocus initialTreeFocus;
    private final InitialSelection initialSelection;
    private final DefaultPhaseExpansion defaultPhaseExpansion;
    private final List<Hitbox> hitboxes = new ArrayList<>();
    private final Set<String> expandedPhases = new LinkedHashSet<>();
    private final Set<String> collapsedPhases = new LinkedHashSet<>();
    private final Set<String> warnedProviderSurfaces = new LinkedHashSet<>();

    private Identifier selectedMissionId;
    private Identifier lastDetailMissionId;
    private TreeFocusMode pendingTreeFocus = TreeFocusMode.NONE;
    private int treeScroll;
    private int detailScroll;
    private int lastTreeX;
    private int lastTreeY;
    private int lastTreeW;
    private int lastTreeH;
    private int lastTreeContentH;
    private int lastTreeScrollbarX;
    private int lastTreeScrollbarY;
    private int lastTreeScrollbarW;
    private int lastTreeScrollbarH;
    private int lastDetailX;
    private int lastDetailY;
    private int lastDetailW;
    private int lastDetailH;
    private int lastDetailContentH;
    private int lastDetailScrollbarX;
    private int lastDetailScrollbarY;
    private int lastDetailScrollbarW;
    private int lastDetailScrollbarH;
    private boolean lastDetailScrollable;
    private int intelScroll;
    private int lastIntelX;
    private int lastIntelY;
    private int lastIntelW;
    private int lastIntelH;
    private int lastIntelContentH;
    private int lastIntelScrollbarX;
    private int lastIntelScrollbarY;
    private int lastIntelScrollbarW;
    private int lastIntelScrollbarH;
    private boolean lastIntelScrollable;
    private ScrollPane scrollbarDragPane = ScrollPane.NONE;
    private int scrollbarDragOffset;
    private boolean hitboxClipActive;
    private int hitboxClipX;
    private int hitboxClipY;
    private int hitboxClipW;
    private int hitboxClipH;
    private MissionFilter missionFilter = MissionFilter.ALL;
    private CacheKey cachedStateKey;
    private CacheKey staleServedKey;
    private MissionRenderState cachedState;
    private long cachedStateFrame = -1L;

    public TerminalMissionBrowser(TerminalMissionProvider provider, Identifier tabId, boolean showExpandControls) {
        this(provider, tabId, showExpandControls, STATE_REFRESH_TICKS);
    }

    public TerminalMissionBrowser(
            TerminalMissionProvider provider, Identifier tabId, boolean showExpandControls, int stateRefreshTicks) {
        this(provider, tabId, showExpandControls, stateRefreshTicks, ActionMode.FULL_ACTIONS,
                InitialTreeFocus.ALIGN_SELECTED_TOP);
    }

    public TerminalMissionBrowser(TerminalMissionProvider provider, Identifier tabId, boolean showExpandControls,
            ActionMode actionMode, InitialTreeFocus initialTreeFocus) {
        this(provider, tabId, showExpandControls, STATE_REFRESH_TICKS, actionMode, initialTreeFocus);
    }

    public TerminalMissionBrowser(TerminalMissionProvider provider, Identifier tabId, boolean showExpandControls,
            ActionMode actionMode, InitialTreeFocus initialTreeFocus, InitialSelection initialSelection,
            DefaultPhaseExpansion defaultPhaseExpansion) {
        this(provider, tabId, showExpandControls, STATE_REFRESH_TICKS, actionMode, initialTreeFocus,
                initialSelection, defaultPhaseExpansion);
    }

    public TerminalMissionBrowser(
            TerminalMissionProvider provider,
            Identifier tabId,
            boolean showExpandControls,
            int stateRefreshTicks,
            ActionMode actionMode,
            InitialTreeFocus initialTreeFocus) {
        this(provider, tabId, showExpandControls, stateRefreshTicks, actionMode, initialTreeFocus,
                InitialSelection.FOCUS_RECORD, DefaultPhaseExpansion.SMART);
    }

    public TerminalMissionBrowser(
            TerminalMissionProvider provider,
            Identifier tabId,
            boolean showExpandControls,
            int stateRefreshTicks,
            ActionMode actionMode,
            InitialTreeFocus initialTreeFocus,
            InitialSelection initialSelection,
            DefaultPhaseExpansion defaultPhaseExpansion) {
        this.provider = provider;
        this.tabId = tabId;
        this.stateRefreshTicks = Math.max(1, stateRefreshTicks);
        this.actionMode = actionMode == null ? ActionMode.FULL_ACTIONS : actionMode;
        this.initialTreeFocus = initialTreeFocus == null ? InitialTreeFocus.ALIGN_SELECTED_TOP : initialTreeFocus;
        this.initialSelection = initialSelection == null ? InitialSelection.FOCUS_RECORD : initialSelection;
        this.defaultPhaseExpansion = defaultPhaseExpansion == null
                ? DefaultPhaseExpansion.SMART
                : defaultPhaseExpansion;
    }

    public void onSelected(TerminalRenderContext context) {
        expandedPhases.clear();
        collapsedPhases.clear();
        treeScroll = 0;
        detailScroll = 0;
        intelScroll = 0;
        scrollbarDragPane = ScrollPane.NONE;
        selectedMissionId = null;
        lastDetailMissionId = null;
        invalidateStateCache();
        pendingTreeFocus = initialTreeFocus == InitialTreeFocus.ALIGN_SELECTED_TOP
                ? TreeFocusMode.ALIGN_SELECTED_TOP
                : TreeFocusMode.NONE;
    }

    public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        hitboxes.clear();
        MissionRenderState state = buildState(context);
        normalizeSelection(state);
        syncDetailScrollWithSelection();
        TerminalMissionChapter chapter = chapter();
        int x = context.contentX();
        int y = context.contentY();
        int w = context.contentWidth();
        int h = context.contentHeight();
        if (state.allRecords().isEmpty()) {
            Identifier hero = context.theme().tokens().assets().loading() == null
                    ? TerminalVisualAssets.MISSIONS_VISUAL_HERO
                    : context.theme().tokens().assets().loading();
            y = TerminalUi.imageHero(context, graphics, hero,
                    x, y, w, Math.min(60, Math.max(44, h / 5)), chapter.accentColor());
            TerminalUi.emptyState(context, graphics, x, y, w,
                    chapter.title(), "No mission records are available from this chapter yet.", chapter.accentColor());
            return;
        }

        MissionRecord selected = selectedRecord(state);
        boolean splitLayout = w >= 820;
        boolean intelLayout = splitLayout && supportsRouteIntelLayout() && w >= INTEL_LAYOUT_MIN_WIDTH;
        if (intelLayout) {
            int gap = TerminalClientOptions.cyberglassActive() ? 14 : 10;
            int leftW = Math.max(320, Math.min(TerminalClientOptions.cyberglassActive() ? 460 : 420, w * 34 / 100));
            int middleW = Math.max(320, Math.min(TerminalClientOptions.cyberglassActive() ? 440 : 410, w * 31 / 100));
            int detailX = x + leftW + gap;
            int intelX = detailX + middleW + gap;
            int intelW = Math.max(280, w - leftW - middleW - gap * 2);
            drawRoadmapPane(context, graphics, state, x, y, leftW, h, mouseX, mouseY);
            drawDetailPane(context, graphics, selected, detailX, y, middleW, h, mouseX, mouseY, true);
            drawIntelPane(context, graphics, state, selected, intelX, y, intelW, h, mouseX, mouseY);
        } else if (splitLayout) {
            int gap = TerminalClientOptions.cyberglassActive() ? 16 : 12;
            int leftW = Math.max(340, Math.min(TerminalClientOptions.cyberglassActive() ? 520 : 460, w * 40 / 100));
            int detailX = x + leftW + gap;
            int detailW = Math.max(420, w - leftW - gap);
            drawRoadmapPane(context, graphics, state, x, y, leftW, h, mouseX, mouseY);
            drawDetailPane(context, graphics, selected, detailX, y, detailW, h, mouseX, mouseY, true);
        } else {
            int treeH = stackedTreeHeight(context, state, w, h);
            drawRoadmapPane(context, graphics, state, x, y, w, treeH, mouseX, mouseY);
            drawDetailPane(context, graphics, selected, x, y + treeH + 10, w,
                    Math.max(180, detailBodyHeight(context, selected, w, false) + actionBarHeight() + 18),
                    mouseX, mouseY, false);
        }
    }

    public boolean mouseClicked(TerminalRenderContext context, double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        if (beginScrollbarDrag(mouseX, mouseY)) {
            return true;
        }
        for (Hitbox hitbox : List.copyOf(hitboxes)) {
            if (TerminalUi.inside(mouseX, mouseY, hitbox.x(), hitbox.y(), hitbox.w(), hitbox.h())) {
                if (hitbox.enabled()) {
                    hitbox.action().run();
                } else {
                    context.playRejectedSound();
                }
                return true;
            }
        }
        return false;
    }

    public boolean keyPressed(TerminalRenderContext context, KeyEvent event) {
        if (event == null) {
            return false;
        }
        return handleKey(context, event.key());
    }

    private boolean handleKey(TerminalRenderContext context, int key) {
        MissionRenderState state = buildState(context);
        normalizeSelection(state);
        if (key == GLFW.GLFW_KEY_UP) {
            return selectRelative(state, -1);
        }
        if (key == GLFW.GLFW_KEY_DOWN) {
            return selectRelative(state, 1);
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_SPACE) {
            return activateSelectedAction(context, state);
        }
        return false;
    }

    public boolean charTyped(TerminalRenderContext context, CharacterEvent event) {
        return false;
    }

    public boolean mouseScrolled(TerminalRenderContext context, double mouseX, double mouseY, double delta) {
        int amount = (int) Math.round(delta * 18.0D);
        if (TerminalUi.inside(mouseX, mouseY, lastTreeX, lastTreeY, lastTreeW, lastTreeH)
                && maxTreeScroll() > 0) {
            treeScroll = TerminalUi.clampScroll(treeScroll - amount, lastTreeContentH, lastTreeH);
            return true;
        }
        if (lastDetailScrollable && TerminalUi.inside(mouseX, mouseY, lastDetailX, lastDetailY, lastDetailW, lastDetailH)
                && maxDetailScroll() > 0) {
            detailScroll = TerminalUi.clampScroll(detailScroll - amount, lastDetailContentH, lastDetailH);
            return true;
        }
        if (lastIntelScrollable && TerminalUi.inside(mouseX, mouseY, lastIntelX, lastIntelY, lastIntelW, lastIntelH)
                && maxIntelScroll() > 0) {
            intelScroll = TerminalUi.clampScroll(intelScroll - amount, lastIntelContentH, lastIntelH);
            return true;
        }
        return false;
    }

    public boolean mouseDragged(TerminalRenderContext context, double mouseX, double mouseY, int button,
            double dragX, double dragY) {
        if (button != 0 || scrollbarDragPane == ScrollPane.NONE) {
            return false;
        }
        updateScrollbarDrag(mouseY);
        return true;
    }

    public boolean mouseReleased(TerminalRenderContext context, double mouseX, double mouseY, int button) {
        if (button != 0 || scrollbarDragPane == ScrollPane.NONE) {
            return false;
        }
        scrollbarDragPane = ScrollPane.NONE;
        return true;
    }

    private boolean beginScrollbarDrag(double mouseX, double mouseY) {
        TerminalScrollbar.Metrics tree = treeScrollbar();
        if (tree.insideTrack(mouseX, mouseY)) {
            scrollbarDragPane = ScrollPane.TREE;
            scrollbarDragOffset = tree.dragOffset(mouseY);
            treeScroll = TerminalUi.clampScroll(tree.scrollForMouse(mouseY, scrollbarDragOffset),
                    lastTreeContentH, lastTreeH);
            return true;
        }
        TerminalScrollbar.Metrics detail = detailScrollbar();
        if (detail.insideTrack(mouseX, mouseY)) {
            scrollbarDragPane = ScrollPane.DETAIL;
            scrollbarDragOffset = detail.dragOffset(mouseY);
            detailScroll = TerminalUi.clampScroll(detail.scrollForMouse(mouseY, scrollbarDragOffset),
                    lastDetailContentH, lastDetailH);
            return true;
        }
        TerminalScrollbar.Metrics intel = intelScrollbar();
        if (intel.insideTrack(mouseX, mouseY)) {
            scrollbarDragPane = ScrollPane.INTEL;
            scrollbarDragOffset = intel.dragOffset(mouseY);
            intelScroll = TerminalUi.clampScroll(intel.scrollForMouse(mouseY, scrollbarDragOffset),
                    lastIntelContentH, lastIntelH);
            return true;
        }
        return false;
    }

    private void updateScrollbarDrag(double mouseY) {
        if (scrollbarDragPane == ScrollPane.TREE) {
            TerminalScrollbar.Metrics tree = treeScrollbar();
            treeScroll = TerminalUi.clampScroll(tree.scrollForMouse(mouseY, scrollbarDragOffset),
                    lastTreeContentH, lastTreeH);
        } else if (scrollbarDragPane == ScrollPane.DETAIL) {
            TerminalScrollbar.Metrics detail = detailScrollbar();
            detailScroll = TerminalUi.clampScroll(detail.scrollForMouse(mouseY, scrollbarDragOffset),
                    lastDetailContentH, lastDetailH);
        } else if (scrollbarDragPane == ScrollPane.INTEL) {
            TerminalScrollbar.Metrics intel = intelScrollbar();
            intelScroll = TerminalUi.clampScroll(intel.scrollForMouse(mouseY, scrollbarDragOffset),
                    lastIntelContentH, lastIntelH);
        }
    }

    private TerminalScrollbar.Metrics treeScrollbar() {
        return TerminalScrollbar.vertical(lastTreeScrollbarX, lastTreeScrollbarY, lastTreeScrollbarW,
                lastTreeScrollbarH, treeScroll, maxTreeScroll());
    }

    private TerminalScrollbar.Metrics detailScrollbar() {
        return TerminalScrollbar.vertical(lastDetailScrollbarX, lastDetailScrollbarY, lastDetailScrollbarW,
                lastDetailScrollbarH, detailScroll, maxDetailScroll());
    }

    private TerminalScrollbar.Metrics intelScrollbar() {
        return TerminalScrollbar.vertical(lastIntelScrollbarX, lastIntelScrollbarY, lastIntelScrollbarW,
                lastIntelScrollbarH, intelScroll, maxIntelScroll());
    }

    private int maxTreeScroll() {
        return Math.max(0, lastTreeContentH - lastTreeH);
    }

    private int maxDetailScroll() {
        return Math.max(0, lastDetailContentH - lastDetailH);
    }

    private int maxIntelScroll() {
        return Math.max(0, lastIntelContentH - lastIntelH);
    }

    public int contentHeight(TerminalRenderContext context) {
        int w = context.contentWidth();
        MissionRenderState state = buildState(context);
        normalizeSelection(state);
        if (w >= SPLIT_LAYOUT_MIN_WIDTH) {
            return context.contentHeight();
        }
        MissionRecord selected = selectedRecord(state);
        return Math.max(context.contentHeight(), stackedTreeHeight(context, state, w, context.contentHeight())
                + detailBodyHeight(context, selected, w, false) + actionBarHeight() + 38);
    }

    private int densityStep() {
        return TerminalClientOptions.interfaceDensity().compactness();
    }

    private int phaseRowHeight() {
        if (TerminalClientOptions.cyberglassActive()) {
            int base = TerminalClientOptions.cyberglassCinematic() ? 42 : TerminalClientOptions.cyberglassCompact() ? 28 : 34;
            return TerminalClientOptions.largeTextMode() ? base + 4 : base;
        }
        int base = TerminalClientOptions.largeTextMode() ? PHASE_ROW_HEIGHT + 4 : PHASE_ROW_HEIGHT;
        return Math.max(TerminalClientOptions.largeTextMode() ? 28 : 20, base - densityStep());
    }

    private int missionRowHeight() {
        if (TerminalClientOptions.cyberglassActive()) {
            int base = TerminalClientOptions.cyberglassCinematic() ? 60 : TerminalClientOptions.cyberglassCompact() ? 40 : 50;
            return TerminalClientOptions.largeTextMode() ? base + 6 : base;
        }
        int base = TerminalClientOptions.largeTextMode() ? MISSION_ROW_HEIGHT + 8 : MISSION_ROW_HEIGHT;
        return Math.max(TerminalClientOptions.largeTextMode() ? 42 : 30, base - densityStep() * 2);
    }

    private int actionBarHeight() {
        if (TerminalClientOptions.cyberglassActive()) {
            int base = TerminalClientOptions.cyberglassCinematic() ? 116 : TerminalClientOptions.cyberglassCompact() ? 84 : 100;
            return TerminalClientOptions.largeTextMode() ? base + 10 : base;
        }
        int base = TerminalClientOptions.largeTextMode() ? ACTION_BAR_HEIGHT + 18 : ACTION_BAR_HEIGHT;
        return Math.max(TerminalClientOptions.largeTextMode() ? 104 : 78, base - densityStep() * 6);
    }

    public boolean hasCachedStateForTests() {
        return cachedState != null;
    }

    public int visibleMissionCountForTests(TerminalRenderContext context) {
        return buildState(context, false).visibleRecords().size();
    }

    public int allMissionCountForTests(TerminalRenderContext context) {
        return buildState(context, false).allRecords().size();
    }

    public boolean keyCodeForTests(TerminalRenderContext context, int key) {
        return handleKey(context, key);
    }

    public int treePaneHeightForTests(TerminalRenderContext context, int width) {
        return treePaneHeight(context, buildState(context, false), width);
    }

    public List<String> phaseDebugRowsForTests(TerminalRenderContext context) {
        MissionRenderState state = buildState(context, false);
        return state.allPhases().stream()
                .map(phase -> phase.label() + "|" + phase.stateLabel() + "|" + phase.contextTitle())
                .toList();
    }

    public boolean phaseExpandedForTests(TerminalRenderContext context, String label) {
        MissionRenderState state = buildState(context, false);
        return state.allPhases().stream()
                .filter(phase -> phase.label().equals(label))
                .findFirst()
                .map(this::isPhaseExpanded)
                .orElse(false);
    }

    public Identifier focusMissionIdForTests(TerminalRenderContext context) {
        MissionRenderState state = buildState(context, false);
        MissionRecord focus = state.focusRecord();
        return focus == null ? null : focus.id();
    }

    public Identifier selectedMissionIdForTests(TerminalRenderContext context) {
        MissionRenderState state = buildState(context, false);
        normalizeSelection(state);
        return selectedMissionId;
    }

    public int detailHeaderHeightForTests(TerminalRenderContext context, Identifier missionId) {
        MissionRenderState state = buildState(context, false);
        MissionRecord record = state.allRecords().stream()
                .filter(candidate -> candidate.id().equals(missionId))
                .findFirst()
                .orElse(null);
        return record == null ? 0 : briefingHeaderHeight(record);
    }

    public boolean selectMissionForTests(TerminalRenderContext context, Identifier missionId) {
        MissionRenderState state = buildState(context, false);
        if (state.allRecords().stream().noneMatch(record -> record.id().equals(missionId))) {
            return false;
        }
        selectMission(missionId, false);
        normalizeSelection(state);
        MissionRecord selected = selectedRecord(state);
        return selected != null && selected.id().equals(missionId);
    }

    public List<Identifier> sideCardMissionIdsForTests(TerminalRenderContext context, Identifier anchorId) {
        MissionRenderState state = buildState(context, false);
        MissionRecord anchor = state.allRecords().stream()
                .filter(record -> record.id().equals(anchorId))
                .findFirst()
                .orElse(null);
        return sideRecordsFor(state, anchor).stream()
                .map(MissionRecord::id)
                .toList();
    }

    public boolean sideCardBodySelectableForTests(
            TerminalRenderContext context, Identifier anchorId, Identifier sideCardId) {
        MissionRenderState state = buildState(context, false);
        MissionRecord anchor = state.allRecords().stream()
                .filter(record -> record.id().equals(anchorId))
                .findFirst()
                .orElse(null);
        return sideCardViewsFor(state, anchor).stream()
                .anyMatch(view -> view.record().id().equals(sideCardId));
    }

    public int intelUnlockCountForTests(TerminalRenderContext context, Identifier missionId) {
        MissionRenderState state = buildState(context, false);
        return state.allRecords().stream()
                .filter(record -> record.id().equals(missionId))
                .findFirst()
                .map(record -> record.intelUnlocks().size())
                .orElse(0);
    }

    public String sideCardStatusForTests(TerminalRenderContext context, Identifier missionId) {
        MissionRenderState state = buildState(context, false);
        return state.allRecords().stream()
                .filter(record -> record.id().equals(missionId))
                .findFirst()
                .map(record -> sideCardStatusLabel(sideCardState(record)))
                .orElse("");
    }

    public String sideCardProgressForTests(TerminalRenderContext context, Identifier missionId) {
        MissionRenderState state = buildState(context, false);
        return state.allRecords().stream()
                .filter(record -> record.id().equals(missionId))
                .findFirst()
                .map(TerminalMissionBrowser::sideCardProgress)
                .orElse("");
    }

    public int sideCardsHeightForTests(TerminalRenderContext context, Identifier anchorId, int width) {
        MissionRenderState state = buildState(context, false);
        MissionRecord anchor = state.allRecords().stream()
                .filter(record -> record.id().equals(anchorId))
                .findFirst()
                .orElse(null);
        return sideCardsHeight(context, state, anchor, width);
    }

    public int largeDetailMaxScrollForTests(TerminalRenderContext context, Identifier missionId, int width, int height) {
        MissionRenderState state = buildState(context, false);
        MissionRecord record = state.allRecords().stream()
                .filter(candidate -> candidate.id().equals(missionId))
                .findFirst()
                .orElse(null);
        if (record == null) {
            return 0;
        }
        setupLargeDetailScrollForTests(context, record, width, height);
        return maxDetailScroll();
    }

    public int dragLargeDetailScrollbarToBottomForTests(
            TerminalRenderContext context, Identifier missionId, int width, int height) {
        MissionRenderState state = buildState(context, false);
        MissionRecord record = state.allRecords().stream()
                .filter(candidate -> candidate.id().equals(missionId))
                .findFirst()
                .orElse(null);
        if (record == null) {
            return 0;
        }
        setupLargeDetailScrollForTests(context, record, width, height);
        detailScroll = 0;
        scrollbarDragPane = ScrollPane.DETAIL;
        scrollbarDragOffset = 2;
        updateScrollbarDrag(lastDetailScrollbarY + lastDetailScrollbarH + 40);
        scrollbarDragPane = ScrollPane.NONE;
        return detailScroll;
    }

    public int detailScrollForTests() {
        return detailScroll;
    }

    public int intelGroupCountForTests(
            TerminalRenderContext context, Identifier anchorId, TerminalMissionIntelKind kind) {
        MissionRenderState state = buildState(context, false);
        MissionRecord anchor = state.allRecords().stream()
                .filter(record -> record.id().equals(anchorId))
                .findFirst()
                .orElse(null);
        List<SideCardView> sideCards = sideCardViewsFor(state, anchor);
        return (int) intelRowsFor(anchor, sideCards).stream()
                .filter(row -> row.unlock().kind() == kind)
                .count();
    }

    public String intelOverflowSummaryForTests(
            TerminalRenderContext context, Identifier anchorId, TerminalMissionIntelKind kind, int visibleRows) {
        MissionRenderState state = buildState(context, false);
        MissionRecord anchor = state.allRecords().stream()
                .filter(record -> record.id().equals(anchorId))
                .findFirst()
                .orElse(null);
        List<SideCardView> sideCards = sideCardViewsFor(state, anchor);
        int count = (int) intelRowsFor(anchor, sideCards).stream()
                .filter(row -> row.unlock().kind() == kind)
                .count();
        int hidden = Math.max(0, count - Math.max(0, visibleRows));
        return hidden == 0 ? "" : intelOverflowLabel(hidden);
    }

    public boolean missionReadOnlyForTests(TerminalRenderContext context, Identifier missionId) {
        MissionRenderState state = buildState(context, false);
        return state.allRecords().stream()
                .filter(record -> record.id().equals(missionId))
                .findFirst()
                .map(record -> readOnly(record))
                .orElse(false);
    }

    public int enabledActionCountForTests(TerminalRenderContext context, Identifier missionId) {
        MissionRenderState state = buildState(context, false);
        return state.allRecords().stream()
                .filter(record -> record.id().equals(missionId))
                .findFirst()
                .map(record -> readOnly(record)
                        ? 0
                        : (int) record.snapshot().actions().stream().filter(TerminalMissionAction::enabled).count())
                .orElse(0);
    }

    public boolean activateMissionActionForTests(TerminalRenderContext context, Identifier missionId) {
        MissionRenderState state = buildState(context, false);
        selectMission(missionId, false);
        return activateSelectedAction(context, state);
    }

    public boolean trackingOnlyForTests() {
        return actionMode == ActionMode.TRACKING_ONLY;
    }

    public String rowStatusLabelForTests(TerminalMissionStatus status, boolean locked) {
        return roadmapStatusLabel(status, locked);
    }

    public String missionRowTitleForTests(TerminalRenderContext context, Identifier missionId) {
        MissionRenderState state = buildState(context, false);
        return state.allRecords().stream()
                .filter(record -> record.id().equals(missionId))
                .findFirst()
                .map(TerminalMissionBrowser::missionRowTitle)
                .orElse("");
    }

    public String detailPhaseChipForTests(TerminalRenderContext context, Identifier missionId) {
        MissionRenderState state = buildState(context, false);
        return state.allRecords().stream()
                .filter(record -> record.id().equals(missionId))
                .findFirst()
                .map(TerminalMissionBrowser::detailPhaseChip)
                .orElse("");
    }

    public List<Identifier> roadmapMissionIdsForTests(TerminalRenderContext context) {
        MissionRenderState state = buildState(context, false);
        return state.visiblePhases().stream()
                .flatMap(phase -> phase.records().stream())
                .map(MissionRecord::id)
                .toList();
    }

    public int applyTreeFocusForTests(TerminalRenderContext context, int viewportHeight) {
        MissionRenderState state = buildState(context, false);
        normalizeSelection(state);
        lastTreeH = Math.max(1, viewportHeight);
        lastTreeContentH = treeRowsHeight(context, state);
        focusTreeOnSelection(state);
        treeScroll = TerminalUi.clampScroll(treeScroll, lastTreeContentH, lastTreeH);
        return treeScroll;
    }

    public int treeScrollForTests() {
        return treeScroll;
    }

    public int treeMaxScrollForTests(TerminalRenderContext context, int viewportHeight) {
        MissionRenderState state = buildState(context, false);
        return Math.max(0, treeRowsHeight(context, state) - Math.max(1, viewportHeight));
    }

    public int selectedRowOffsetForTests(TerminalRenderContext context) {
        MissionRenderState state = buildState(context, false);
        normalizeSelection(state);
        return selectedRowOffset(state);
    }

    public String stickyActionsTitleForTests() {
        return ACTIONS_SECTION_TITLE;
    }

    public String emptyRequirementsCopyForTests() {
        return EMPTY_REQUIREMENTS_COPY;
    }

    public String metRequirementsCopyForTests() {
        return MET_REQUIREMENTS_COPY;
    }

    private void setupLargeDetailScrollForTests(
            TerminalRenderContext context, MissionRecord record, int width, int height) {
        int actionH = Math.min(actionBarHeight(), Math.max(78, height / 5));
        int bodyH = Math.max(64, height - actionH - 26);
        lastDetailX = 8;
        lastDetailY = 8;
        lastDetailW = Math.max(80, width - 18);
        lastDetailH = bodyH;
        lastDetailContentH = detailBodyHeight(context, record, width, true);
        lastDetailScrollbarX = Math.max(0, width - 9);
        lastDetailScrollbarY = 8;
        lastDetailScrollbarW = 7;
        lastDetailScrollbarH = bodyH;
        lastDetailScrollable = true;
        detailScroll = TerminalUi.clampScroll(detailScroll, lastDetailContentH, lastDetailH);
    }

    private MissionRenderState buildState(TerminalRenderContext context) {
        return buildState(context, true);
    }

    private MissionRenderState buildState(TerminalRenderContext context, boolean allowStale) {
        long frameId = TerminalRenderCache.current().frameId();
        CacheKey key = cacheKey(context);
        if (cachedState != null && key.equals(cachedStateKey)) {
            return cachedState;
        }
        if (cachedState != null && allowStale && !key.equals(staleServedKey)) {
            staleServedKey = key;
            return cachedState;
        }
        MissionRenderState state = buildFreshState(context);
        cachedState = state;
        cachedStateFrame = frameId;
        cachedStateKey = key;
        staleServedKey = null;
        return state;
    }

    private MissionRenderState buildFreshState(TerminalRenderContext context) {
        List<TerminalMissionDefinition> definitions = safeMissions(context).stream()
                .filter(definition -> definition != null)
                .sorted(Comparator
                        .comparingInt(TerminalMissionDefinition::phaseOrder)
                        .thenComparingInt(TerminalMissionDefinition::missionOrder)
                        .thenComparing(mission -> mission.id().toString()))
                .toList();
        List<MissionRecord> rawRecords = new ArrayList<>();
        for (TerminalMissionDefinition definition : definitions) {
            TerminalMissionSnapshot snapshot = safeSnapshot(context, definition);
            TerminalMissionPresentation presentation = safePresentation(context, definition, snapshot);
            TerminalMissionVisuals visuals = safeVisuals(context, definition, snapshot);
            TerminalMissionRole role = safeRole(context, definition, snapshot);
            List<Identifier> routePrerequisites = safeRoutePrerequisites(context, definition, snapshot, role);
            Optional<Identifier> routeAnchor = safeRouteAnchor(context, definition, snapshot, role);
            List<TerminalMissionIntelUnlock> intelUnlocks = safeIntelUnlocks(context, definition, snapshot, role);
            rawRecords.add(new MissionRecord(definition, snapshot, presentation, visuals, role,
                    routePrerequisites, routeAnchor, intelUnlocks));
        }
        boolean routeIntelLayout = supportsRouteIntelLayout();
        PhaseModel rawPhaseModel = buildPhaseModel(rawRecords, routeIntelLayout);
        List<MissionRecord> records = rawRecords.stream()
                .map(record -> record.withPhase(rawPhaseModel.phase(record.phaseKey())))
                .toList();
        PhaseModel phaseModel = buildPhaseModel(records, routeIntelLayout);
        List<MissionRecord> routeVisible = routeIntelLayout
                ? records.stream().filter(TerminalMissionBrowser::visibleRouteRecord).toList()
                : records;
        List<MissionRecord> visible = routeVisible.stream()
                .filter(missionFilter::includes)
                .toList();
        List<PhaseGroup> visiblePhases = visiblePhases(phaseModel, visible);
        MissionRecord focus = focusRecord(visible);
        int completed = 0;
        for (MissionRecord record : visible) {
            if (isDone(record.snapshot().status())) {
                completed++;
            }
        }
        return new MissionRenderState(records, visible, phaseModel.phases(), visiblePhases, focus, completed);
    }

    private static boolean visibleRouteRecord(MissionRecord record) {
        return record.routeAnchor().isEmpty() && record.role() != TerminalMissionRole.OPTIONAL;
    }

    private CacheKey cacheKey(TerminalRenderContext context) {
        net.minecraft.world.entity.player.Player player = context == null ? null : context.player();
        UUID playerId = player == null ? new UUID(0L, 0L) : player.getUUID();
        long gameTime = player == null || player.level() == null ? 0L : player.level().getGameTime();
        int refreshTick = (int) Math.max(0L, gameTime / stateRefreshTicks);
        int widthBucket = context == null ? 0 : Math.max(0, context.contentWidth() / WIDTH_BUCKET_SIZE);
        return new CacheKey(providerName(), tabId, playerId, widthBucket, refreshTick);
    }

    private TerminalMissionChapter chapter() {
        try {
            TerminalMissionChapter chapter = provider == null ? null : provider.chapter();
            return chapter == null ? fallbackChapter() : chapter;
        } catch (RuntimeException exception) {
            warnProviderFailure("chapter", exception);
            return fallbackChapter();
        }
    }

    private TerminalMissionChapter fallbackChapter() {
        Identifier id = tabId == null ? Identifier.fromNamespaceAndPath("echoterminal", "unknown_missions") : tabId;
        return new TerminalMissionChapter(id, "Mission Records", "Mission provider unavailable.", Integer.MAX_VALUE,
                0xFF66D9FF, true);
    }

    private List<TerminalMissionDefinition> safeMissions(TerminalRenderContext context) {
        try {
            List<TerminalMissionDefinition> missions = provider == null
                    ? List.of()
                    : provider.missions(context == null ? null : context.player());
            return missions == null ? List.of() : missions;
        } catch (RuntimeException exception) {
            warnProviderFailure("mission list", exception);
            return List.of();
        }
    }

    private TerminalMissionSnapshot safeSnapshot(TerminalRenderContext context, TerminalMissionDefinition definition) {
        try {
            TerminalMissionSnapshot snapshot = provider.snapshot(context == null ? null : context.player(), definition.id());
            return snapshot == null ? fallbackSnapshot(definition) : snapshot;
        } catch (RuntimeException exception) {
            warnProviderFailure("mission snapshot", exception);
            return fallbackSnapshot(definition);
        }
    }

    private TerminalMissionSnapshot fallbackSnapshot(TerminalMissionDefinition definition) {
        return new TerminalMissionSnapshot(
                definition.id(),
                TerminalMissionStatus.LOCKED,
                0.0F,
                "LOCKED",
                "Mission provider unavailable.",
                "Check Command Deck blocker status or reload this chapter later.",
                List.of());
    }

    private TerminalMissionPresentation safePresentation(TerminalRenderContext context,
            TerminalMissionDefinition definition, TerminalMissionSnapshot snapshot) {
        try {
            TerminalMissionPresentation presentation = provider.presentation(
                    context == null ? null : context.player(), definition, snapshot);
            return presentation == null
                    ? TerminalMissionPresentation.fallback(definition, snapshot)
                    : presentation;
        } catch (RuntimeException exception) {
            warnProviderFailure("mission presentation", exception);
            return TerminalMissionPresentation.fallback(definition, snapshot);
        }
    }

    private TerminalMissionVisuals safeVisuals(TerminalRenderContext context,
            TerminalMissionDefinition definition, TerminalMissionSnapshot snapshot) {
        try {
            TerminalMissionVisuals visuals = provider.visuals(context == null ? null : context.player(), definition, snapshot);
            return visuals == null ? TerminalMissionVisuals.fallback(definition, snapshot) : visuals;
        } catch (RuntimeException exception) {
            warnProviderFailure("mission visuals", exception);
            return TerminalMissionVisuals.fallback(definition, snapshot);
        }
    }

    private TerminalMissionRole safeRole(TerminalRenderContext context,
            TerminalMissionDefinition definition, TerminalMissionSnapshot snapshot) {
        try {
            TerminalMissionRole role = provider.role(context == null ? null : context.player(), definition, snapshot);
            return role == null ? TerminalMissionRole.fallback(definition, snapshot) : role;
        } catch (RuntimeException exception) {
            warnProviderFailure("mission role", exception);
            return TerminalMissionRole.fallback(definition, snapshot);
        }
    }

    private Optional<Identifier> safeRouteAnchor(TerminalRenderContext context,
            TerminalMissionDefinition definition, TerminalMissionSnapshot snapshot, TerminalMissionRole role) {
        try {
            Optional<Identifier> anchor = provider.routeAnchor(
                    context == null ? null : context.player(), definition, snapshot, role);
            return anchor == null ? Optional.empty() : anchor.filter(java.util.Objects::nonNull);
        } catch (RuntimeException exception) {
            warnProviderFailure("mission route anchor", exception);
            return Optional.empty();
        }
    }

    private List<Identifier> safeRoutePrerequisites(TerminalRenderContext context,
            TerminalMissionDefinition definition, TerminalMissionSnapshot snapshot, TerminalMissionRole role) {
        try {
            List<Identifier> prerequisites = provider.routePrerequisites(
                    context == null ? null : context.player(), definition, snapshot, role);
            return prerequisites == null ? List.of() : prerequisites.stream()
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .toList();
        } catch (RuntimeException exception) {
            warnProviderFailure("mission route prerequisites", exception);
            return List.of();
        }
    }

    private List<TerminalMissionIntelUnlock> safeIntelUnlocks(TerminalRenderContext context,
            TerminalMissionDefinition definition, TerminalMissionSnapshot snapshot, TerminalMissionRole role) {
        try {
            List<TerminalMissionIntelUnlock> unlocks = provider.intelUnlocks(
                    context == null ? null : context.player(), definition, snapshot, role);
            return unlocks == null ? List.of() : unlocks.stream()
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .toList();
        } catch (RuntimeException exception) {
            warnProviderFailure("mission intel unlocks", exception);
            return List.of();
        }
    }

    private void warnProviderFailure(String surface, RuntimeException exception) {
        if (warnedProviderSurfaces.add(surface)) {
            EchoTerminal.LOGGER.warn("Terminal mission provider {} failed while building {}; rendering fallback.",
                    providerName(), surface, exception);
        }
    }

    private String providerName() {
        return provider == null ? "<null>" : provider.getClass().getName();
    }

    private boolean supportsRouteIntelLayout() {
        return MainSurvivalQuestProvider.TAB_ID.equals(tabId);
    }

    private void drawRoadmapPane(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            MissionRenderState state, int x, int y, int w, int h, int mouseX, int mouseY) {
        TerminalMissionChapter chapter = chapter();
        TerminalUi.cinematicPanel(context, graphics, x, y, w, h,
                TerminalUi.chapterAccent(context, chapter.accentColor()));
        int total = state.allRecords().size();
        int completed = completedCount(state.allRecords());
        int pct = total > 0 ? Math.round(completed * 100.0F / total) : 0;
        int innerX = x + (TerminalClientOptions.cyberglassActive() ? 14 : 12);
        int innerW = w - (TerminalClientOptions.cyberglassActive() ? 28 : 24);
        int titleY = y + (TerminalClientOptions.cyberglassActive() ? 14 : 10);
        String title = TerminalClientOptions.cyberglassActive()
                ? chapter.title().toUpperCase(Locale.ROOT)
                : "MISSIONS";
        TerminalUi.line(context, graphics, title, innerX + (TerminalClientOptions.cyberglassActive() ? 28 : 0), titleY,
                Math.max(48, innerW - 180), TerminalClientOptions.cyberglassActive() ? TerminalUi.TEXT : chapter.accentColor());
        if (TerminalClientOptions.cyberglassActive()) {
            TerminalUi.iconBadge(context, graphics, TerminalIcon.MISSIONS, innerX, titleY - 3, 22,
                    TerminalUi.chapterAccent(context, chapter.accentColor()), true);
            int sx = innerX + 8;
            int sy = titleY + 28;
            sx = drawRoadmapStatChip(context, graphics, sx, sy, "ACTIVE", activeCount(state.allRecords()), TerminalUi.CYAN) + 5;
            sx = drawRoadmapStatChip(context, graphics, sx, sy, "READY", readyCount(state.allRecords()), TerminalUi.GREEN) + 5;
            sx = drawRoadmapStatChip(context, graphics, sx, sy, "LOCKED", lockedCount(state.allRecords()), TerminalUi.AMBER) + 5;
            drawRoadmapStatChip(context, graphics, sx, sy, "DONE", completed, TerminalUi.GREEN);
        } else {
            String countLine = "ECHO-7 PROTOCOL // " + pct + "% COMPLETE";
            int countW = Math.min(180, Math.max(120, w / 3));
            TerminalUi.line(context, graphics, countLine, x + w - countW - 12, titleY, countW, TerminalUi.MUTED);
            TerminalUi.divider(graphics, x + 12, y + 27, w - 24, chapter.accentColor());
        }
        int summaryY = y + (TerminalClientOptions.cyberglassActive() ? 60 : 36);
        int summaryH = drawRouteSummary(context, graphics, state, innerX, summaryY, innerW - 4);
        int filterY = summaryY + summaryH + 5;
        int filterH = drawFilterChips(context, graphics, state, innerX, filterY, innerW - 4, mouseX, mouseY);
        int listY = filterY + filterH + 6;

        lastTreeX = innerX;
        lastTreeY = listY;
        lastTreeW = innerW - 4;
        lastTreeH = Math.max(68, h - (listY - y) - 12);
        lastTreeContentH = treeRowsHeight(context, state);
        lastTreeScrollbarX = innerX + innerW - 5;
        lastTreeScrollbarY = listY;
        lastTreeScrollbarW = 6;
        lastTreeScrollbarH = lastTreeH;
        focusTreeOnSelection(state);
        treeScroll = TerminalUi.clampScroll(treeScroll, lastTreeContentH, lastTreeH);
        boolean scissor = lastTreeContentH > lastTreeH;
        if (scissor) {
            graphics.enableScissor(innerX, listY, innerX + innerW - 4, listY + lastTreeH);
        }
        drawTreeRows(context, graphics, state, innerX, listY - (scissor ? treeScroll : 0),
                innerW, listY, lastTreeH, mouseX, mouseY);
        if (scissor) {
            graphics.disableScissor();
            TerminalUi.scrollbar(context, graphics, lastTreeScrollbarX, lastTreeScrollbarY, lastTreeScrollbarH,
                    treeScroll, Math.max(0, lastTreeContentH - lastTreeH), chapter.accentColor(),
                    TerminalUi.inside(mouseX, mouseY, innerX, listY, innerW, lastTreeH));
        }
    }

    private int drawRouteSummary(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            MissionRenderState state, int x, int y, int w) {
        int h = routeSummaryHeight();
        MissionRecord focus = state.focusRecord();
        int color = focus == null || focus.phaseLocked()
                ? chapter().accentColor()
                : statusColor(focus.snapshot().status());
        TerminalUi.flatHudPanel(context, graphics, x, y, w, h, TerminalUi.chapterAccent(context, color));
        String next = focus == null ? "Route signal pending" : missionRowTitle(focus);
        int ready = readyRewardCount(state);
        int chipW = ready > 0 ? Math.max(62, Math.min(96, TerminalUi.statusBadgeWidth(context, "READY " + ready))) : 0;
        int chipX = ready > 0 ? x + w - chipW - 8 : x + w;
        int titleW = Math.max(40, chipX - (x + 54) - 8);
        TerminalUi.line(context, graphics, "NEXT", x + 8, y + 6, 46, chapter().accentColor());
        TerminalUi.line(context, graphics, next, x + 54, y + 6, titleW, TerminalUi.TEXT);
        if (ready > 0) {
            TerminalUi.miniStatusPill(context, graphics, "READY " + ready,
                    chipX, y + 5, chipW, TerminalUi.GREEN, false);
        }
        String stage = focus == null ? chapter().summary() : detailPhaseChip(focus);
        TerminalUi.line(context, graphics, stage, x + 8, y + 20, w - 16, TerminalUi.MUTED);
        return h;
    }

    private int drawRoadmapStatChip(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, String label, int count, int color) {
        String text = String.format(Locale.ROOT, "%02d %s", Math.max(0, count), label);
        int width = Math.max(72, Math.min(118, TerminalUi.statusBadgeWidth(context, text) + 16));
        TerminalUi.miniStatusPill(context, graphics, text, x, y, width, color, false);
        return x + width;
    }

    private int drawFilterChips(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            MissionRenderState state, int x, int y, int w, int mouseX, int mouseY) {
        int h = TerminalClientOptions.cyberglassActive() ? 26 : 20;
        if (!TerminalClientOptions.cyberglassActive()) {
            TerminalUi.densePanel(context, graphics, x, y, w, h, chapter().accentColor());
        }
        int cx = x + 6;
        for (MissionFilter filter : MissionFilter.values()) {
            int chipW = Math.max(54, Math.min(110, TerminalUi.statusBadgeWidth(context, filter.label) + 16));
            if (cx + chipW > x + w - (TerminalClientOptions.cyberglassActive() ? 116 : 4)) {
                break;
            }
            boolean selected = missionFilter == filter;
            int chipY = y + (TerminalClientOptions.cyberglassActive() ? 5 : 3);
            boolean hovered = TerminalUi.inside(mouseX, mouseY, cx, chipY, chipW, 14);
            int color = selected ? chapter().accentColor() : hovered ? TerminalUi.TEXT : TerminalUi.MUTED;
            TerminalUi.miniStatusPill(context, graphics, filter.label, cx, chipY, chipW, color, selected);
            addHitbox(cx, chipY, chipW, 14, true, () -> selectMissionFilter(filter));
            cx += chipW + 6;
        }
        if (TerminalClientOptions.cyberglassActive() && w >= 280) {
            MissionRecord active = firstActiveRecord(state);
            int buttonW = Math.min(118, Math.max(96, w / 4));
            int bx = x + w - buttonW - 8;
            boolean enabled = active != null;
            boolean hovered = enabled && TerminalUi.inside(mouseX, mouseY, bx, y + 4, buttonW, 17);
            TerminalUi.compactButton(context, graphics, bx, y + 4, buttonW, 17,
                    "JUMP TO ACTIVE", chapter().accentColor(), enabled, hovered);
            if (enabled) {
                addHitbox(bx, y + 4, buttonW, 17, true, () -> selectMission(active.id(), true));
            }
        }
        return h;
    }

    private void drawTreeRows(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            MissionRenderState state, int x, int y, int w, int viewportY, int viewportH, int mouseX, int mouseY) {
        if (state.visibleRecords().isEmpty()) {
            TerminalUi.emptyState(context, graphics, x + 2, y + 4, Math.max(80, w - 12),
                    "No Mission Records", "No mission records are available from this chapter yet.", TerminalUi.MUTED);
            return;
        }
        int cy = y;
        int phaseH = phaseRowHeight();
        int missionH = missionRowHeight();
        for (PhaseGroup phase : state.visiblePhases()) {
            List<MissionRecord> phaseAll = recordsForPhase(state.allRecords(), phase.id());
            int complete = completedCount(phaseAll);
            int total = phaseAll.size();
            boolean expanded = isPhaseExpanded(phase);
            int phaseColor = phase.locked() ? TerminalUi.MUTED : phase.complete() ? TerminalUi.GREEN : chapter().accentColor();
            boolean hover = TerminalUi.inside(mouseX, mouseY, x, cy, w - 8, phaseH);
            String progressCount = complete + "/" + total;
            String phaseLabel = phase.label().isBlank() ? phase.contextTitle() : phase.label();
            if (visible(cy, phaseH, viewportY, viewportH)) {
                TerminalUi.phaseAccordionRow(context, graphics, x, cy, w - 8, phaseH,
                        String.format(Locale.ROOT, "%02d", phase.displayIndex() + 1),
                        phaseLabel, progressCount, expanded, hover, phaseColor);
                String toggleLabel = expanded ? "COMPACT" : "EXPAND";
                int toggleW = Math.min(56, Math.max(42, w / 5));
                if (w >= 220) {
                    TerminalUi.line(context, graphics, toggleLabel, x + w - toggleW - 40,
                            cy + Math.max(5, (phaseH - 8) / 2), toggleW, TerminalUi.MUTED);
                }
                PhaseGroup hitPhase = phase;
                addHitbox(x, cy, w - 8, phaseH, true, () -> togglePhase(hitPhase));
            }
            cy += phaseH + 2;
            if (!expanded) {
                continue;
            }
            for (MissionRecord record : phase.records()) {
                boolean selected = record.id().equals(selectedMissionId);
                if (selected) {
                    drawMissionRow(context, graphics, record, x + 8, cy, w - 18, viewportY, viewportH, mouseX, mouseY);
                } else {
                    drawSubduedMissionRow(context, graphics, record, x + 8, cy, w - 18, viewportY, viewportH, mouseX, mouseY);
                }
                cy += missionH;
            }
            cy += 2;
        }
    }

    private void drawMissionRow(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            MissionRecord record, int rowX, int y, int rowW, int viewportY, int viewportH, int mouseX, int mouseY) {
        int missionH = missionRowHeight();
        if (!visible(y, missionH, viewportY, viewportH)) {
            return;
        }
        TerminalMissionSnapshot snapshot = record.snapshot();
        boolean selected = record.id().equals(selectedMissionId);
        boolean locked = record.phaseLocked();
        int color = locked ? TerminalUi.MUTED : statusColor(snapshot.status());
        String statusLabel = roadmapStatusLabel(snapshot.status(), locked);
        int chipW = Math.max(50, Math.min(Math.max(58, rowW / 4),
                Math.min(98, TerminalUi.statusBadgeWidth(context, statusLabel))));
        int chipX = rowX + rowW - chipW - 6;
        int textX = rowX + 30;
        int titleW = Math.max(34, chipX - textX - 8);
        int progressW = Math.max(36, chipX - textX);
        int rowH = missionH - 4;
        boolean hovered = TerminalUi.inside(mouseX, mouseY, rowX, y, rowW, rowH);
        TerminalRenderContext recordContext = context.withChapterTheme(record.definition().id().getNamespace(),
                chapter().title(), record.definition().id().getNamespace());
        boolean emphasized = !locked
                && (snapshot.status() == TerminalMissionStatus.UNLOCKED
                        || snapshot.status() == TerminalMissionStatus.CLAIMABLE);
        TerminalUi.roadmapMissionRow(recordContext, graphics, rowX, y, rowW, rowH,
                selected, hovered, emphasized, color);
        if (selected) {
            graphics.outline(rowX, y, rowW, rowH, color);
        }
        int iconSize = Math.min(20, Math.max(16, missionH - 10));
        int iconY = y + Math.max(3, (rowH - iconSize) / 2);
        TerminalIcon semanticIcon = semanticIconForStatus(snapshot.status(), locked);
        TerminalUi.iconBadge(recordContext, graphics, semanticIcon, rowX + 5, iconY, iconSize, color,
                selected || hovered);
        TerminalUi.line(context, graphics, missionRowTitle(record),
                textX, y + (TerminalClientOptions.cyberglassActive() ? 7 : 3), titleW, locked ? selected ? TerminalUi.TEXT : TerminalUi.MUTED
                        : missionTitleColor(snapshot.status(), selected, color));
        if (TerminalClientOptions.cyberglassActive() && rowH >= 44) {
            TerminalUi.line(context, graphics, missionRowSubtitle(record),
                    textX, y + 21, titleW, TerminalUi.MUTED);
        }
        TerminalUi.missionStatusPill(context, graphics, statusLabel, chipX,
                y + (TerminalClientOptions.cyberglassActive() ? 9 : 3), chipW);
        int progressY = TerminalClientOptions.cyberglassActive()
                ? y + Math.max(22, rowH - 8)
                : y + Math.max(17, missionH - 10);
        TerminalUi.progress(recordContext, graphics, textX, progressY, progressW, 4,
                snapshot.progress(), color);
        addHitbox(rowX, y, rowW, rowH, true, () -> selectMission(record.id(), false));
    }

    private void drawSubduedMissionRow(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            MissionRecord record, int rowX, int y, int rowW, int viewportY, int viewportH, int mouseX, int mouseY) {
        int missionH = missionRowHeight();
        if (!visible(y, missionH, viewportY, viewportH)) {
            return;
        }
        TerminalMissionSnapshot snapshot = record.snapshot();
        boolean selected = record.id().equals(selectedMissionId);
        boolean locked = record.phaseLocked();
        int color = locked ? TerminalUi.MUTED : statusColor(snapshot.status());
        String statusLabel = roadmapStatusLabel(snapshot.status(), locked);
        int rowH = missionH - 4;
        boolean hovered = TerminalUi.inside(mouseX, mouseY, rowX, y, rowW, rowH);
        TerminalIcon semanticIcon = semanticIconForStatus(snapshot.status(), locked);
        TerminalUi.subduedMissionRow(context, graphics, rowX, y, rowW, rowH,
                semanticIcon, missionRowTitle(record), statusLabel,
                snapshot.progress(), color, selected, hovered);
        addHitbox(rowX, y, rowW, rowH, true, () -> selectMission(record.id(), false));
    }

    private static TerminalIcon semanticIconForStatus(TerminalMissionStatus status, boolean locked) {
        if (locked) {
            return TerminalIcon.LOCK;
        }
        return switch (status) {
            case COMPLETED, CLAIMED -> TerminalIcon.CHECK;
            case CLAIMABLE -> TerminalIcon.CHECK;
            case UNLOCKED -> TerminalIcon.TARGET;
            case VIEW_ONLY -> TerminalIcon.CLOCK;
            case LOCKED -> TerminalIcon.LOCK;
        };
    }

    private void drawDetailPane(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            MissionRecord record, int x, int y, int w, int h, int mouseX, int mouseY, boolean scrollable) {
        boolean largeCard = scrollable;
        TerminalUi.cinematicPanel(context, graphics, x, y, w - 4, h,
                TerminalUi.chapterAccent(context, chapter().accentColor()));
        if (record == null) {
            TerminalUi.emptyState(context, graphics, x + 10, y + 12, w - 24,
                    "Select Mission", "Choose a mission record from the command queue.", TerminalUi.MUTED);
            return;
        }
        if (largeCard) {
            int actionH = Math.min(actionBarHeight(), Math.max(78, h / 5));
            int actionY = y + h - actionH - 10;
            int bodyX = x + 8;
            int bodyY = y + 8;
            int bodyW = Math.max(80, w - 18);
            int bodyH = Math.max(64, actionY - bodyY - 8);
            lastDetailX = bodyX;
            lastDetailY = bodyY;
            lastDetailW = bodyW;
            lastDetailH = bodyH;
            lastDetailScrollable = true;
            lastDetailContentH = detailBodyHeight(context, record, w, true);
            lastDetailScrollbarX = x + w - 9;
            lastDetailScrollbarY = bodyY;
            lastDetailScrollbarW = 7;
            lastDetailScrollbarH = bodyH;
            detailScroll = TerminalUi.clampScroll(detailScroll, lastDetailContentH, lastDetailH);
            boolean scissor = lastDetailContentH > lastDetailH;
            graphics.enableScissor(bodyX, bodyY, bodyX + bodyW, bodyY + bodyH);
            int drawY = y - (scissor ? detailScroll : 0);
            int drawnContentH = withHitboxClip(bodyX, bodyY, bodyW, bodyH,
                    () -> drawLargeMissionCard(context, graphics, record, x, drawY, w, h, mouseX, mouseY));
            lastDetailContentH = Math.max(lastDetailContentH, drawnContentH);
            graphics.disableScissor();
            if (maxDetailScroll() > 0) {
                TerminalUi.scrollbar(context, graphics, lastDetailScrollbarX, lastDetailScrollbarY,
                        lastDetailScrollbarH, detailScroll, maxDetailScroll(), chapter().accentColor(),
                        TerminalUi.inside(mouseX, mouseY, bodyX, bodyY, bodyW, bodyH));
            }
            drawStickyActions(context, graphics, record, x + 14, actionY, w - 32, actionH, mouseX, mouseY);
            return;
        }
        int bodyX = x + 12;
        int bodyW = w - 30;
        int bodyY = y + 12;
        int actionH = Math.min(actionBarHeight(), Math.max(78, h / 5));
        int actionY = y + h - actionH - 10;
        int bodyH = Math.max(64, actionY - bodyY - 8);
        lastDetailX = bodyX;
        lastDetailY = bodyY;
        lastDetailW = bodyW;
        lastDetailH = bodyH;
        lastDetailScrollable = scrollable;
        lastDetailContentH = detailBodyHeight(context, record, bodyW, false);
        lastDetailScrollbarX = x + w - 9;
        lastDetailScrollbarY = bodyY;
        lastDetailScrollbarW = 7;
        lastDetailScrollbarH = bodyH;
        detailScroll = TerminalUi.clampScroll(detailScroll, lastDetailContentH, lastDetailH);
        boolean scissor = scrollable && lastDetailContentH > lastDetailH;
        if (scissor) {
            graphics.enableScissor(bodyX, bodyY, bodyX + bodyW - 4, bodyY + bodyH);
        }
        int cy = scissor
                ? withHitboxClip(bodyX, bodyY, bodyW, bodyH,
                        () -> drawDetailBody(context, graphics, record, bodyX, bodyY - detailScroll, bodyW, mouseX, mouseY, false))
                : drawDetailBody(context, graphics, record, bodyX, bodyY, bodyW, mouseX, mouseY, false);
        lastDetailContentH = Math.max(lastDetailContentH, cy - (bodyY - (scissor ? detailScroll : 0)));
        if (scissor) {
            graphics.disableScissor();
            TerminalUi.scrollbar(context, graphics, lastDetailScrollbarX, lastDetailScrollbarY, lastDetailScrollbarH,
                    detailScroll, Math.max(0, lastDetailContentH - bodyH), chapter().accentColor(),
                    TerminalUi.inside(mouseX, mouseY, bodyX, bodyY, bodyW, bodyH));
        }
        drawStickyActions(context, graphics, record, bodyX, actionY, bodyW, actionH, mouseX, mouseY);
    }

    private int drawDetailBody(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            MissionRecord record, int x, int y, int w, int mouseX, int mouseY, boolean largeCard) {
        int cy = drawBriefingHeader(context, graphics, record, x, y, w, mouseX, mouseY) + 8;
        cy = drawMissionModeChips(context, graphics, record, x, cy, w) + 4;
        cy = drawNextStepCallout(context, graphics, record, x, cy, w) + 2;
        cy = drawSideCardsSection(context, graphics, buildState(context), record, x, cy, w, mouseX, mouseY) + 4;
        TerminalUi.sectionHeader(context, graphics, "REQUIREMENTS", "", x, cy, w - 4, chapter().accentColor());
        cy += 20;
        List<TerminalMissionRequirement> unmet = unmetRequirements(record);
        if (record.definition().requirements().isEmpty()) {
            TerminalUi.line(context, graphics, EMPTY_REQUIREMENTS_COPY,
                    x + 4, cy, w - 12, TerminalUi.MUTED);
            cy += 14;
        } else if (unmet.isEmpty()) {
            TerminalUi.line(context, graphics, MET_REQUIREMENTS_COPY,
                    x + 4, cy, w - 12, TerminalUi.GREEN);
            cy += 14;
        } else {
            for (TerminalMissionRequirement requirement : unmet) {
                cy = drawRequirementRow(context, graphics, requirement, x + 2, cy, w - 10, mouseX, mouseY);
            }
            int hidden = record.definition().requirements().size() - unmet.size();
            if (hidden > 0) {
                TerminalUi.line(context, graphics, hidden + " completed requirement(s) hidden.",
                        x + 4, cy, w - 12, TerminalUi.MUTED);
                cy += 14;
            }
        }
        cy += 4;
        cy = drawRewards(context, graphics, record, x, cy, w, mouseX, mouseY);
        if (!largeCard) {
            TerminalUi.sectionHeader(context, graphics, "FIELD GUIDE", "", x, cy, w - 4, chapter().accentColor());
            cy += 20;
            boolean lockedForCommands = phaseLockedForCommands(record);
            String guide = record.definition().fieldGuide().isBlank()
                    ? record.definition().briefing()
                    : record.definition().fieldGuide();
            cy = TerminalUi.wrap(context, graphics, previewText(guide, "Field guide signal unavailable.", lockedForCommands),
                    x + 2, cy, w - 12, lockedForCommands ? TerminalUi.MUTED : TerminalUi.TEXT) + 9;
            if (!record.presentation().relatedIntelKey().isBlank()) {
                TerminalUi.sectionHeader(context, graphics, "RELATED INTEL", "", x, cy, w - 4, chapter().accentColor());
                cy += 20;
                cy = TerminalUi.wrap(context, graphics, intelLabel(record.presentation().relatedIntelKey()),
                        x + 2, cy, w - 12, TerminalUi.MUTED) + 8;
            }
            if (!record.definition().prerequisites().isEmpty()) {
                TerminalUi.sectionHeader(context, graphics, "PREREQUISITES", "", x, cy, w - 4, chapter().accentColor());
                cy += 20;
                for (String prerequisite : record.definition().prerequisites()) {
                    cy = TerminalUi.wrap(context, graphics, "- " + prerequisite, x + 2, cy, w - 12, TerminalUi.MUTED) + 2;
                }
            }
        }
        return cy + 8;
    }

    private int drawLargeMissionCard(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            MissionRecord record, int x, int y, int w, int h, int mouseX, int mouseY) {
        boolean locked = phaseLockedForCommands(record);
        int color = locked ? TerminalUi.MUTED : statusColor(record.snapshot().status());
        int padX = x + 14;
        int padW = w - 32;
        int cy = y + 14;
        int imageH = TerminalClientOptions.cyberglassActive()
                ? Math.min(TerminalClientOptions.cyberglassCinematic() ? 172 : 150, Math.max(116, h / 4))
                : Math.min(128, Math.max(96, h / 5));
        TerminalRenderContext recordContext = context.withChapterTheme(record.definition().id().getNamespace(),
                chapter().title(), record.definition().id().getNamespace());
        Identifier art = record.visuals().categoryArt();
        if (art != null) {
            TerminalUi.questArtCard(recordContext, graphics, art, padX, cy, padW, imageH, color, false, false);
        } else {
            TerminalUi.flatHudPanel(recordContext, graphics, padX, cy, padW, imageH, color);
        }
        int overlayY = cy + imageH - (TerminalClientOptions.cyberglassActive() ? 72 : 38);
        graphics.fill(padX, overlayY, padX + padW, cy + imageH, 0x8802070C);
        String primaryStatus = locked ? "LOCKED" : compactStatusLabel(record.snapshot());
        String secondaryStatus = roleChipLabel(record.role());
        int pillW = Math.max(82, Math.min(120, padW / 4));
        String phaseChip = detailPhaseChip(record);
        int phaseChipW = Math.max(74, Math.min(160, TerminalUi.statusBadgeWidth(context, phaseChip)));
        phaseChipW = Math.min(phaseChipW, Math.max(74, padW - pillW - 36));
        int chipY = TerminalClientOptions.cyberglassActive() ? overlayY + 10 : overlayY + 6;
        TerminalUi.miniStatusPill(context, graphics, phaseChip, padX + 10, chipY, phaseChipW, color, false);
        TerminalUi.missionStatusPill(context, graphics, primaryStatus, padX + padW - pillW - 10, chipY, pillW);
        if (!secondaryStatus.isBlank()) {
            TerminalUi.miniStatusPill(context, graphics, secondaryStatus,
                    padX + padW - pillW - 10, chipY + 18, pillW, color, false);
        }
        String title = record.presentation().shortTitle().toUpperCase(Locale.ROOT);
        if (TerminalClientOptions.cyberglassActive()) {
            TerminalUi.line(context, graphics, title, padX + 12, overlayY + 36,
                    Math.max(80, padW - 24), TerminalUi.TEXT);
            String source = missionRowSubtitle(record);
            if (!source.isBlank()) {
                TerminalUi.line(context, graphics, source, padX + 12, overlayY + 52,
                        Math.max(80, padW - 24), TerminalUi.MUTED);
            }
        }
        cy += imageH + 10;
        if (!TerminalClientOptions.cyberglassActive()) {
            TerminalUi.line(context, graphics, title, padX, cy, padW, TerminalUi.TEXT);
            cy += 16;
        }
        String objective = previewText(record.presentation().objectiveSummary(), record.definition().briefing(), locked);
        cy = TerminalUi.wrap(context, graphics, objective, padX, cy, padW,
                locked ? TerminalUi.MUTED : TerminalUi.TEXT) + 10;
        cy = drawNextStepCallout(context, graphics, record, padX, cy, padW) + 10;
        cy = drawSideCardsSection(context, graphics, buildState(context), record, padX, cy, padW, mouseX, mouseY) + 8;
        TerminalUi.sectionHeader(context, graphics, "REQUIREMENTS", "", padX, cy, padW, chapter().accentColor());
        cy += 22;
        List<TerminalMissionRequirement> unmet = unmetRequirements(record);
        if (record.definition().requirements().isEmpty()) {
            TerminalUi.line(context, graphics, EMPTY_REQUIREMENTS_COPY, padX, cy, padW, TerminalUi.MUTED);
            cy += 14;
        } else if (unmet.isEmpty()) {
            TerminalUi.line(context, graphics, MET_REQUIREMENTS_COPY, padX, cy, padW, TerminalUi.GREEN);
            cy += 14;
        } else {
            for (TerminalMissionRequirement requirement : unmet) {
                int rowH = requirementHeight(context, requirement, padW);
                int requirementColor = requirement.satisfied() ? TerminalUi.GREEN : TerminalUi.RED;
                TerminalUi.flatHudPanel(context, graphics, padX, cy, padW, rowH - 4, requirementColor);
                TerminalUi.itemSlot(context, graphics, requirement.icon(), padX + 6, cy + 6, requirementColor,
                        TerminalUi.inside(mouseX, mouseY, padX + 6, cy + 6, 20, 20));
                int chipW = Math.max(74, Math.min(92, padW / 4));
                int chipX = padX + padW - chipW - 10;
                int textW = Math.max(38, chipX - (padX + 32) - 8);
                TerminalUi.line(context, graphics, requirement.label(), padX + 32, cy + 6, textW, requirementColor);
                String progress = requirement.need() > 0 ? requirement.have() + "/" + requirement.need() : "";
                TerminalUi.missionStatusPill(context, graphics, requirement.satisfied() ? "DONE" : "MISSING",
                        chipX, cy + 6, chipW);
                TerminalUi.wrap(context, graphics, requirement.detail().isBlank() ? progress : requirement.detail(),
                        padX + 32, cy + 20, Math.max(40, textW), TerminalUi.MUTED);
                cy += rowH;
            }
            int hidden = record.definition().requirements().size() - unmet.size();
            if (hidden > 0) {
                TerminalUi.line(context, graphics, hidden + " completed requirement(s) hidden.",
                        padX, cy, padW, TerminalUi.MUTED);
                cy += 14;
            }
        }
        cy += 8;
        cy = drawRewards(context, graphics, record, padX, cy, padW, mouseX, mouseY);
        return cy - y + 8;
    }

    private int drawBriefingHeader(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            MissionRecord record, int x, int y, int w, int mouseX, int mouseY) {
        boolean locked = phaseLockedForCommands(record);
        int color = locked ? TerminalUi.MUTED : statusColor(record.snapshot().status());
        int height = briefingHeaderHeight(record);
        boolean visualHeader = TerminalClientOptions.useVisualAssets();
        String detail = locked
                ? record.phaseLabel() + " / " + emptyFallback(record.presentation().routeHint(), record.definition().category())
                : tagLine(record.definition(), record.presentation(), record.role());
        TerminalRenderContext recordContext = context.withChapterTheme(record.definition().id().getNamespace(),
                chapter().title(), record.definition().id().getNamespace());
        Identifier banner = TerminalUi.chapterBanner(recordContext);
        return TerminalUi.v2HeroHeader(recordContext, graphics,
                banner == null ? record.visuals().categoryArt() : banner,
                TerminalUi.themedMissionIcon(recordContext, record.definition().id(), record.definition().category()),
                TerminalIcon.DEFAULT,
                x, y, w - 4, height,
                record.presentation().shortTitle().toUpperCase(Locale.ROOT),
                detail,
                previewText(record.presentation().objectiveSummary(), record.definition().briefing(), locked),
                locked ? "PREVIEW" : compactStatusLabel(record.snapshot()),
                locked ? "LOCKED" : roleChipLabel(record.role()),
                record.snapshot().progress(), color, locked ? TerminalUi.MUTED : color, visualHeader);
    }

    private int drawNextStepCallout(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            MissionRecord record, int x, int y, int w) {
        String hint = record.presentation().nextStep().isBlank()
                ? "This record is visible for planning."
                : record.presentation().nextStep();
        boolean lockedForCommands = phaseLockedForCommands(record);
        if (lockedForCommands) {
            hint = record.phaseUnlockHint();
        }
        return TerminalUi.callout(context, graphics, x, y, w - 4,
                lockedForCommands ? "LOCKED PHASE" : "NEXT STEP", hint,
                lockedForCommands ? TerminalUi.MUTED : actionHintColor(record.snapshot()));
    }

    private int drawMissionModeChips(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            MissionRecord record, int x, int y, int w) {
        MissionDisplayMode activeMode = TerminalClientOptions.useVisualAssets()
                ? MissionDisplayMode.VISUAL_RPG
                : MissionDisplayMode.MINIMAL_FUTURE;
        int cx = x + 2;
        for (MissionDisplayMode mode : MissionDisplayMode.values()) {
            int chipW = Math.max(62, Math.min(96, TerminalUi.statusBadgeWidth(context, mode.label) + 16));
            if (cx + chipW > x + w - 6) {
                break;
            }
            boolean selected = mode == activeMode;
            int color = selected ? statusColor(record.snapshot().status()) : TerminalUi.MUTED;
            TerminalUi.miniStatusPill(context, graphics, mode.label, cx, y, chipW, color, selected);
            cx += chipW + 6;
        }
        return y + missionModeChipsHeight();
    }

    private int drawSideCardsSection(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            MissionRenderState state, MissionRecord anchor, int x, int y, int w, int mouseX, int mouseY) {
        List<SideCardView> sideCards = sideCardViewsFor(state, anchor);
        if (sideCards.isEmpty()) {
            return y;
        }
        TerminalUi.sectionHeader(context, graphics, "SIDE OPS (OPTIONAL INTEL)",
                sideProgress(sideCards), x, y, w - 4, TerminalUi.AMBER);
        int cy = y + 20;
        TerminalUi.line(context, graphics, "Optional intel. Nonblocking route context.",
                x + 2, cy, w - 12, TerminalUi.MUTED);
        cy += 16;
        int railX = x + 10;
        int cardX = x + 20;
        int cardW = Math.max(120, w - 28);
        int sideCardH = TerminalClientOptions.cyberglassActive() ? SIDE_CARD_HEIGHT + 8 : SIDE_CARD_HEIGHT;
        int firstNodeY = cy + sideCardH / 2;
        int lastNodeY = firstNodeY + (sideCards.size() - 1) * (sideCardH + 6);
        graphics.fill(railX, firstNodeY, railX + 2, lastNodeY + 1, 0x5538DFF4);
        for (SideCardView side : sideCards) {
            int nodeY = cy + sideCardH / 2;
            graphics.fill(railX - 3, nodeY - 3, railX + 5, nodeY + 5, 0xEE071017);
            graphics.outline(railX - 3, nodeY - 3, 8, 8, TerminalUi.opaque(side.color()));
            graphics.fill(railX - 1, nodeY - 1, railX + 3, nodeY + 3, TerminalUi.opaque(side.color()));
            cy = drawSideCard(context, graphics, side, cardX, cy, cardW, mouseX, mouseY) + 6;
        }
        return cy;
    }

    private int drawSideCard(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            SideCardView view, int x, int y, int w, int mouseX, int mouseY) {
        MissionRecord record = view.record();
        int color = view.color();
        int h = TerminalClientOptions.cyberglassActive() ? SIDE_CARD_HEIGHT + 8 : SIDE_CARD_HEIGHT;
        boolean hovered = TerminalUi.inside(mouseX, mouseY, x, y, w, h);
        TerminalUi.flatHudPanel(context, graphics, x, y, w, h, color);
        if (hovered) {
            graphics.outline(x, y, w, h, TerminalUi.opaque(color));
        }
        boolean archived = view.state() == SideCardState.ARCHIVED;
        TerminalUi.iconBadge(context, graphics, view.icon(), x + 8, y + 9, 28, color, hovered || archived);
        int rightW = Math.max(78, Math.min(104, w / 4));
        int rightX = x + w - rightW - 10;
        int textX = x + 46;
        int textW = Math.max(48, rightX - textX - 10);
        int titleColor = view.state() == SideCardState.LOCKED ? TerminalUi.MUTED : TerminalUi.TEXT;
        TerminalUi.line(context, graphics, view.title(), textX, y + 7, textW, titleColor);
        TerminalUi.line(context, graphics, compactText(view.summary(), 92), textX, y + 21, textW,
                view.state() == SideCardState.LOCKED ? TerminalUi.MUTED : TerminalUi.text(context));
        TerminalUi.line(context, graphics, view.progressLabel(), textX, y + 35, textW, TerminalUi.MUTED);
        int countW = Math.max(52, Math.min(84, TerminalUi.statusBadgeWidth(context, view.intelCountLabel())));
        TerminalUi.miniStatusPill(context, graphics, view.intelCountLabel(), textX, y + 47,
                Math.min(countW, Math.max(52, textW)), color, false);
        TerminalUi.miniStatusPill(context, graphics, sideCardStatusLabel(view.state()), rightX, y + 7,
                rightW, color, archived);
        boolean buttonHover = TerminalUi.inside(mouseX, mouseY, rightX, y + h - 24, rightW, 17);
        TerminalUi.compactButton(context, graphics, rightX, y + h - 24, rightW,
                view.actionLabel(), color, view.actionEnabled(), buttonHover);
        if (view.actionEnabled()) {
            TerminalMissionAction hitAction = view.primaryAction();
            addHitbox(rightX, y + h - 24, rightW, 17, true,
                    () -> sendMissionAction(context, record.definition().id(), hitAction.id()));
        }
        addHitbox(x, y, w, h, true, () -> selectMission(record.definition().id(), false));
        return y + h;
    }

    private void drawIntelPane(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            MissionRenderState state, MissionRecord selected, int x, int y, int w, int h, int mouseX, int mouseY) {
        TerminalUi.cinematicPanel(context, graphics, x, y, w - 4, h,
                TerminalUi.chapterAccent(context, TerminalUi.CYAN));
        int padX = x + 12;
        int padW = w - 28;
        int cy = y + 12;
        String title = selected == null ? "INTEL & DISCOVERIES" : selected.presentation().shortTitle();
        TerminalUi.line(context, graphics, "INTEL & DISCOVERIES", padX, cy, Math.max(80, padW - 86), TerminalUi.TEXT);
        TerminalUi.miniStatusPill(context, graphics, "INTEL", x + w - 82, cy - 1, 62, TerminalUi.CYAN, false);
        cy += 20;
        int heroH = Math.max(52, Math.min(82, h / 6));
        int bodyY = cy;
        int bodyH = Math.max(54, y + h - 12 - bodyY);
        if (selected != null) {
            cy = bodyY - intelScroll;
            TerminalRenderContext selectedContext = context.withChapterTheme(selected.definition().id().getNamespace(),
                    chapter().title(), selected.definition().id().getNamespace());
            List<SideCardView> sideCards = sideCardViewsFor(state, selected);
            List<IntelUnlockRow> unlocks = intelRowsFor(selected, sideCards);
            int archived = (int) sideCards.stream().filter(view -> view.state() == SideCardState.ARCHIVED).count();
            String summary = sideCards.isEmpty()
                    ? "No optional intel has attached to this route record yet."
                    : archived + "/" + sideCards.size() + " optional intel records archived.";
            lastIntelX = padX;
            lastIntelY = bodyY;
            lastIntelW = padW;
            lastIntelH = bodyH;
            lastIntelContentH = intelPaneContentHeight(context, selected, sideCards, unlocks, summary, padW, heroH);
            lastIntelScrollable = true;
            lastIntelScrollbarX = x + w - 9;
            lastIntelScrollbarY = bodyY;
            lastIntelScrollbarW = 7;
            lastIntelScrollbarH = bodyH;
            intelScroll = TerminalUi.clampScroll(intelScroll, lastIntelContentH, lastIntelH);
            boolean scissor = lastIntelContentH > bodyH;
            cy = bodyY - (scissor ? intelScroll : 0);
            if (scissor) {
                graphics.enableScissor(padX, bodyY, padX + padW, bodyY + bodyH);
            }
            TerminalUi.imageHero(selectedContext, graphics, selected.visuals().categoryArt(),
                    padX, cy, padW, heroH, selected.phaseLocked() ? TerminalUi.MUTED : statusColor(selected.snapshot().status()));
            int overlayY = cy + Math.max(8, heroH - 24);
            graphics.fill(padX, overlayY, padX + padW, cy + heroH, 0x9A02070C);
            TerminalUi.line(context, graphics, title, padX + 8, overlayY + 7,
                    Math.max(54, padW - 88), TerminalUi.TEXT);
            TerminalUi.miniStatusPill(context, graphics, detailPhaseChip(selected),
                    padX + padW - 82, overlayY + 5, 74,
                    selected.phaseLocked() ? TerminalUi.MUTED : statusColor(selected.snapshot().status()), false);
            cy += heroH + 10;
            cy = TerminalUi.wrap(context, graphics, summary, padX, cy, padW, TerminalUi.MUTED) + 10;
            if (unlocks.isEmpty()) {
                TerminalUi.emptyState(context, graphics, padX, cy, padW,
                        "NO INTEL TARGETS", "Select a route record with optional intel side cards.", TerminalUi.MUTED);
            } else {
                for (TerminalMissionIntelKind kind : TerminalMissionIntelKind.values()) {
                    List<IntelUnlockRow> group = unlocks.stream()
                            .filter(row -> row.unlock().kind() == kind)
                            .toList();
                    if (group.isEmpty()) {
                        continue;
                    }
                    int color = intelKindColor(kind);
                    TerminalUi.sectionHeader(context, graphics, kind.displayName().toUpperCase(Locale.ROOT),
                            group.size() + " signal(s)", padX, cy, padW, color);
                    cy += 20;
                    for (IntelUnlockRow row : group) {
                        cy = drawIntelUnlockRow(context, graphics, row, padX + 2, cy, padW - 4);
                    }
                    cy += 4;
                }
            }
            lastIntelContentH = Math.max(lastIntelContentH, cy - (bodyY - (scissor ? intelScroll : 0)));
            if (scissor) {
                graphics.disableScissor();
                TerminalUi.scrollbar(context, graphics, lastIntelScrollbarX, lastIntelScrollbarY,
                        lastIntelScrollbarH, intelScroll, maxIntelScroll(), TerminalUi.CYAN,
                        TerminalUi.inside(mouseX, mouseY, lastIntelX, lastIntelY, lastIntelW, lastIntelH));
            }
            return;
        }
        lastIntelX = padX;
        lastIntelY = bodyY;
        lastIntelW = padW;
        lastIntelH = bodyH;
        lastIntelContentH = bodyH;
        lastIntelScrollable = false;
        intelScroll = 0;
        TerminalUi.emptyState(context, graphics, padX, bodyY, padW,
                "NO INTEL TARGETS", "Select a route record with optional intel side cards.", TerminalUi.MUTED);
    }

    private int intelPaneContentHeight(TerminalRenderContext context, MissionRecord selected,
            List<SideCardView> sideCards, List<IntelUnlockRow> unlocks, String summary, int width, int heroH) {
        int height = selected == null ? 0 : heroH + 10;
        height += TerminalUi.wrappedHeight(context, summary, width) + 10;
        if (unlocks.isEmpty()) {
            return height + 48;
        }
        for (TerminalMissionIntelKind kind : TerminalMissionIntelKind.values()) {
            long rows = unlocks.stream()
                    .filter(row -> row.unlock().kind() == kind)
                    .count();
            if (rows > 0) {
                height += 20 + (int) rows * INTEL_ROW_HEIGHT + 4;
            }
        }
        return height + 8;
    }

    private int drawIntelUnlockRow(
            TerminalRenderContext context,
            GuiGraphicsExtractor graphics,
            IntelUnlockRow row,
            int x,
            int y,
            int w) {
        int color = row.color();
        TerminalUi.flatHudPanel(context, graphics, x, y, w, INTEL_ROW_HEIGHT - 3, color);
        TerminalUi.iconBadge(context, graphics, intelKindIcon(row.unlock().kind()), x + 7, y + 5, 15, color,
                row.state() == SideCardState.READY || row.state() == SideCardState.ARCHIVED);
        int chipW = Math.max(50, Math.min(74, TerminalUi.statusBadgeWidth(context, row.stateLabel())));
        int chipX = x + w - chipW - 8;
        int titleW = Math.max(40, chipX - (x + 28) - 8);
        TerminalUi.line(context, graphics, row.unlock().title(), x + 28, y + 5, titleW,
                row.state() == SideCardState.LOCKED ? TerminalUi.MUTED : TerminalUi.TEXT);
        TerminalUi.miniStatusPill(context, graphics, row.stateLabel(), chipX, y + 5, chipW, color,
                row.state() == SideCardState.ARCHIVED);
        return y + INTEL_ROW_HEIGHT;
    }

    private int drawRequirementRow(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            TerminalMissionRequirement requirement, int x, int y, int w, int mouseX, int mouseY) {
        int color = requirement.satisfied() ? TerminalUi.GREEN : TerminalUi.RED;
        int rowH = requirementHeight(context, requirement, w);
        TerminalUi.flatHudPanel(context, graphics, x, y, w, rowH - 4, color);
        TerminalUi.itemSlot(context, graphics, requirement.icon(), x + 6, y + 6, color,
                TerminalUi.inside(mouseX, mouseY, x + 6, y + 6, 20, 20));
        int chipW = Math.max(74, Math.min(92, w / 4));
        int chipX = x + w - chipW - 10;
        int textW = Math.max(38, chipX - (x + 32) - 8);
        TerminalUi.line(context, graphics, requirement.label(), x + 32, y + 6, textW, color);
        String progress = requirement.need() > 0 ? requirement.have() + "/" + requirement.need() : "";
        TerminalUi.missionStatusPill(context, graphics, requirement.satisfied() ? "DONE" : "MISSING",
                chipX, y + 6, chipW);
        TerminalUi.wrap(context, graphics, requirement.detail().isBlank() ? progress : requirement.detail(),
                x + 32, y + 20, Math.max(40, textW), TerminalUi.MUTED);
        return y + rowH;
    }

    private int drawRewards(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            MissionRecord record, int x, int y, int w, int mouseX, int mouseY) {
        List<ItemStack> stacks = record.definition().rewards().stream()
                .map(TerminalMissionReward::stack)
                .filter(stack -> !stack.isEmpty())
                .toList();
        boolean hasTextReward = record.definition().rewards().stream()
                .anyMatch(reward -> reward.stack().isEmpty() && !reward.label().isBlank());
        if (stacks.isEmpty() && !hasTextReward) {
            return y;
        }
        TerminalUi.sectionHeader(context, graphics, "REWARDS", "", x, y, w - 4, chapter().accentColor());
        int cy = y + 20;
        cy = TerminalUi.itemGrid(context, graphics, stacks, x + 2, cy, w - 12,
                chapter().accentColor(), mouseX, mouseY) + 3;
        for (TerminalMissionReward reward : record.definition().rewards()) {
            if (!reward.stack().isEmpty()) {
                continue;
            }
            cy = TerminalUi.wrap(context, graphics, reward.label() + ": " + reward.detail(),
                    x + 2, cy, w - 12, TerminalUi.MUTED) + 2;
        }
        return cy + 8;
    }

    private void drawStickyActions(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            MissionRecord record, int x, int y, int w, int h, int mouseX, int mouseY) {
        String summary = actionSummary(record);
        TerminalUi.actionBarPanel(context, graphics, x, y, w - 4, h,
                TerminalUi.chapterAccent(context, chapter().accentColor()));
        TerminalUi.line(context, graphics, ACTIONS_SECTION_TITLE, x + 8, y + 8, w - 20, chapter().accentColor());
        int summaryBottom = TerminalUi.wrap(context, graphics, summary, x + 8, y + 21, w - 20, TerminalUi.TEXT);
        int buttonY = Math.min(y + h - 28, Math.max(y + 42, summaryBottom + 6));
        if (phaseLockedForCommands(record)) {
            int buttonW = Math.min(220, Math.max(118, w / 3));
            int buttonX = x + w - buttonW - 12;
            TerminalUi.disabledCommandButton(context, graphics, buttonX, buttonY, buttonW, 22,
                    "TRACK MISSION", TerminalUi.themedActionIcon(context, "track", TerminalVisualAssets.ICON_ACTION_VIEW));
            TerminalUi.line(context, graphics, record.phaseUnlockHint(), x + 8, buttonY + 4,
                    Math.max(40, buttonX - x - 18), TerminalUi.MUTED);
            return;
        }
        List<TerminalMissionAction> actions = record.snapshot().actions();
        boolean tracking = TerminalPlayerData.get(context.player()).isTracking(tabId, record.definition().id());
        List<CommandButton> buttons = new ArrayList<>();
        if (actionMode == ActionMode.FULL_ACTIONS) {
            for (TerminalMissionAction action : actions) {
                TerminalMissionAction hitAction = action;
                buttons.add(new CommandButton(
                        action.label(),
                        action.enabled(),
                        action.enabled() ? "" : action.disabledReason(),
                        actionIcon(context, action),
                        true,
                        () -> sendMissionAction(context, record.definition().id(), hitAction.id())));
            }
        }
        buttons.add(new CommandButton(
                tracking ? "UNTRACK" : "TRACK",
                true,
                "",
                TerminalUi.themedActionIcon(context, "track", TerminalVisualAssets.ICON_ACTION_VIEW),
                false,
                () -> sendTrackingAction(context, record.definition().id(), tracking)));
        int buttonH = 22;
        int gap = buttons.size() > 3 ? 6 : 8;
        int gaps = Math.max(0, buttons.size() - 1) * gap;
        int buttonW = buttons.size() == 1
                ? Math.min(220, w - 20)
                : Math.min(150, Math.max(72, (w - 16 - gaps) / buttons.size()));
        int bx = x + 8;
        for (CommandButton button : buttons) {
            boolean hover = button.enabled() && TerminalUi.inside(mouseX, mouseY, bx, buttonY, buttonW, buttonH);
            if (button.enabled() && button.primary()) {
                TerminalUi.primaryCommandButton(context, graphics, bx, buttonY, buttonW, buttonH, button.label(),
                        button.icon(), chapter().accentColor(), hover);
            } else if (button.enabled()) {
                TerminalUi.secondaryCommandButton(context, graphics, bx, buttonY, buttonW, buttonH, button.label(),
                        button.icon(), chapter().accentColor(), hover);
            } else {
                TerminalUi.disabledCommandButton(context, graphics, bx, buttonY, buttonW, buttonH,
                        button.label(), button.icon());
            }
            addHitbox(bx, buttonY, buttonW, buttonH, button.enabled(), button.action());
            bx += buttonW + gap;
        }
        String reason = actionMode == ActionMode.TRACKING_ONLY
                ? "Mission commands live in Survival Route; tracking still pins this record to the Command Deck."
                : firstDisabledReason(actions);
        if (reason.isBlank() && actions.isEmpty()) {
            reason = "No mission command is available; tracking still pins this record to the Command Deck.";
        }
        if (!reason.isBlank() && buttonY + 39 < y + h) {
            TerminalUi.line(context, graphics, reason, x + 8, buttonY + 29, w - 20, 0xFFC2D4DC);
        }
    }

    private int treePaneHeight(TerminalRenderContext context, MissionRenderState state, int width) {
        if (TerminalClientOptions.cyberglassActive()) {
            return 60 + 26 + routeSummaryHeight() + 8 + treeRowsHeight(context, state);
        }
        return 20 + 22 + routeSummaryHeight() + 6 + treeRowsHeight(context, state);
    }

    private int stackedTreeHeight(TerminalRenderContext context, MissionRenderState state, int width, int viewportHeight) {
        int preferred = Math.max(190, viewportHeight * (width >= 620 ? 52 : 46) / 100);
        int maximum = Math.max(180, viewportHeight - 128);
        return Math.min(treePaneHeight(context, state, width), Math.min(preferred, maximum));
    }

    private int treeRowsHeight(TerminalRenderContext context, MissionRenderState state) {
        int height = 0;
        int phaseH = phaseRowHeight();
        int missionH = missionRowHeight();
        for (PhaseGroup phase : state.visiblePhases()) {
            height += phaseH + 2;
            if (isPhaseExpanded(phase)) {
                height += phase.records().size() * missionH + 2;
            }
        }
        return height + 6;
    }

    private int routeSummaryHeight() {
        if (TerminalClientOptions.cyberglassActive()) {
            return TerminalClientOptions.cyberglassCinematic() ? 58 : TerminalClientOptions.cyberglassCompact() ? 40 : 50;
        }
        return TerminalClientOptions.largeTextMode() ? 42 : 34;
    }

    private int detailBodyHeight(TerminalRenderContext context, MissionRecord record, int width, boolean largeCard) {
        if (record == null) {
            return 60;
        }
        if (largeCard) {
            int imageH = TerminalClientOptions.cyberglassActive()
                    ? (TerminalClientOptions.cyberglassCinematic() ? 186 : 164)
                    : 160;
            int height = imageH + 14 + 16 + 14 + 40 + 22;
            height += sideCardsHeight(context, record, width);
            if (record.definition().requirements().isEmpty()) {
                height += 14;
            } else {
                List<TerminalMissionRequirement> unmet = unmetRequirements(record);
                if (unmet.isEmpty()) {
                    height += 14;
                }
                for (TerminalMissionRequirement requirement : unmet) {
                    height += requirementHeight(context, requirement, width);
                }
                if (!unmet.isEmpty() && unmet.size() < record.definition().requirements().size()) {
                    height += 14;
                }
            }
            int stackRewards = (int) record.definition().rewards().stream()
                    .filter(reward -> !reward.stack().isEmpty())
                    .count();
            int textRewards = (int) record.definition().rewards().stream()
                    .filter(reward -> reward.stack().isEmpty() && !reward.label().isBlank())
                    .count();
            if (stackRewards > 0 || textRewards > 0) {
                height += 20 + TerminalUi.itemGridHeight(stackRewards, width - 12) + 12;
                height += textRewards * 14;
            }
            height += 22 + 14 + 10;
            return height;
        }
        int height = briefingHeaderHeight(record) + 8;
        height += missionModeChipsHeight() + 4;
        height += nextStepCalloutHeight(context, record, width);
        height += sideCardsHeight(context, record, width);
        height += 20;
        if (record.definition().requirements().isEmpty()) {
            height += 14;
        } else {
            List<TerminalMissionRequirement> unmet = unmetRequirements(record);
            if (unmet.isEmpty()) {
                height += 14;
            }
            for (TerminalMissionRequirement requirement : unmet) {
                height += requirementHeight(context, requirement, width);
            }
            if (!unmet.isEmpty() && unmet.size() < record.definition().requirements().size()) {
                height += 14;
            }
        }
        String guide = record.definition().fieldGuide().isBlank()
                ? record.definition().briefing()
                : record.definition().fieldGuide();
        height += 20 + TerminalUi.wrappedHeight(context, guide, width - 12) + 18;
        int stackRewards = (int) record.definition().rewards().stream()
                .filter(reward -> !reward.stack().isEmpty())
                .count();
        int textRewards = (int) record.definition().rewards().stream()
                .filter(reward -> reward.stack().isEmpty() && !reward.label().isBlank())
                .count();
        if (stackRewards > 0 || textRewards > 0) {
            height += 20 + TerminalUi.itemGridHeight(stackRewards, width - 12) + 12;
            height += textRewards * 14;
        }
        if (!record.presentation().relatedIntelKey().isBlank()) {
            height += 20 + TerminalUi.wrappedHeight(context, record.presentation().relatedIntelKey(), width - 12) + 8;
        }
        if (!record.definition().prerequisites().isEmpty()) {
            height += 20;
            for (String prerequisite : record.definition().prerequisites()) {
                height += TerminalUi.wrappedHeight(context, "- " + prerequisite, width - 12) + 2;
            }
        }
        return height + 8;
    }

    private int missionModeChipsHeight() {
        if (TerminalClientOptions.cyberglassActive()) {
            return TerminalClientOptions.cyberglassCompact() ? 18 : 22;
        }
        return TerminalClientOptions.largeTextMode() ? 22 : 18;
    }

    private int briefingHeaderHeight(MissionRecord record) {
        if (TerminalClientOptions.cyberglassActive()) {
            return TerminalClientOptions.cyberglassCinematic() ? 164 : TerminalClientOptions.cyberglassCompact() ? 112 : 136;
        }
        boolean visualHeader = TerminalClientOptions.useVisualAssets();
        return Math.max(92, (visualHeader ? 104 : 98) - densityStep() * 5);
    }

    private int nextStepCalloutHeight(TerminalRenderContext context, MissionRecord record, int width) {
        String hint = record.presentation().nextStep().isBlank()
                ? "This record is visible for planning."
                : record.presentation().nextStep();
        if (phaseLockedForCommands(record)) {
            hint = record.phaseUnlockHint();
        }
        return Math.max(40, 27 + TerminalUi.wrappedHeight(context, hint, Math.max(40, width - 24))) + 5;
    }

    private int sideCardsHeight(TerminalRenderContext context, MissionRecord record, int width) {
        return sideCardsHeight(context, buildState(context), record, width);
    }

    private int sideCardsHeight(
            TerminalRenderContext context, MissionRenderState state, MissionRecord record, int width) {
        if (!supportsRouteIntelLayout()) {
            return 0;
        }
        List<SideCardView> sideCards = sideCardViewsFor(state, record);
        if (sideCards.isEmpty()) {
            return 0;
        }
        int cardH = TerminalClientOptions.cyberglassActive() ? SIDE_CARD_HEIGHT + 8 : SIDE_CARD_HEIGHT;
        return SIDE_CARD_SECTION_HEADER_HEIGHT + sideCards.size() * (cardH + 6) + 8;
    }

    private int requirementHeight(TerminalRenderContext context, TerminalMissionRequirement requirement, int width) {
        String detail = requirement.detail();
        if (requirement.need() > 0 && detail.isBlank()) {
            detail = requirement.have() + "/" + requirement.need();
        }
        int chipW = Math.max(74, Math.min(92, width / 4));
        int detailH = TerminalUi.wrappedHeight(context, detail, Math.max(40, width - chipW - 54));
        int min = TerminalClientOptions.cyberglassActive() ? 48 : 38;
        return Math.max(min, 24 + detailH + (TerminalClientOptions.cyberglassActive() ? 8 : 0)) + 5;
    }

    private static List<TerminalMissionRequirement> unmetRequirements(MissionRecord record) {
        if (record == null) {
            return List.of();
        }
        return record.definition().requirements().stream()
                .filter(requirement -> !requirement.satisfied())
                .toList();
    }

    private void normalizeSelection(MissionRenderState state) {
        if (state.visibleRecords().isEmpty()) {
            selectedMissionId = null;
            return;
        }
        if (selectedMissionId != null && state.allRecords().stream().anyMatch(record -> record.id().equals(selectedMissionId))) {
            return;
        }
        MissionRecord focus;
        if (initialSelection == InitialSelection.FIRST_RECORD) {
            focus = state.visibleRecords().get(0);
        } else {
            focus = state.focusRecord() == null
                    || state.visibleRecords().stream().noneMatch(record -> record.id().equals(state.focusRecord().id()))
                            ? state.visibleRecords().get(0)
                            : state.focusRecord();
        }
        selectedMissionId = focus.id();
    }

    private boolean selectRelative(MissionRenderState state, int offset) {
        List<MissionRecord> rows = navigationRecords(state);
        if (rows.isEmpty()) {
            return false;
        }
        int index = 0;
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).id().equals(selectedMissionId)) {
                index = i;
                break;
            }
        }
        selectMission(rows.get(Math.floorMod(index + offset, rows.size())).id(), true);
        return true;
    }

    private boolean activateSelectedAction(TerminalRenderContext context, MissionRenderState state) {
        if (actionMode == ActionMode.TRACKING_ONLY) {
            return false;
        }
        MissionRecord selected = selectedRecord(state);
        if (selected == null || readOnly(selected)) {
            return false;
        }
        for (TerminalMissionAction action : selected.snapshot().actions()) {
            if (action.enabled()) {
                sendMissionAction(context, selected.definition().id(), action.id());
                return true;
            }
        }
        return false;
    }

    private boolean readOnly(MissionRecord record) {
        return record == null || phaseLockedForCommands(record) || actionMode == ActionMode.TRACKING_ONLY;
    }

    private String actionSummary(MissionRecord record) {
        if (phaseLockedForCommands(record)) {
            return "Preview only. Finish the blocker above to unlock actions.";
        }
        if (actionMode == ActionMode.TRACKING_ONLY) {
            return "Reference view only. Use Survival Route for mission commands, or track this record for the Command Deck.";
        }
        return commandSummary(record.snapshot(), record.presentation());
    }

    private void sendMissionAction(TerminalRenderContext context, Identifier missionId, String actionId) {
        context.sendAction(tabId, TerminalMissionActions.MISSION_ACTION,
                TerminalMissionActions.payload(chapter().id(), missionId, actionId));
        invalidateStateCache();
    }

    private void sendTrackingAction(TerminalRenderContext context, Identifier missionId, boolean clear) {
        context.sendAction(tabId, TerminalMissionActions.TRACK_MISSION,
                TerminalMissionActions.trackingPayload(tabId, chapter().id(), missionId, clear));
        invalidateStateCache();
    }

    private List<MissionRecord> navigationRecords(MissionRenderState state) {
        List<MissionRecord> rows = new ArrayList<>();
        for (PhaseGroup phase : state.visiblePhases()) {
            if (isPhaseExpanded(phase)) {
                rows.addAll(phase.records());
            }
        }
        return rows.isEmpty() ? state.visibleRecords() : rows;
    }

    private MissionRecord selectedRecord(MissionRenderState state) {
        if (selectedMissionId == null) {
            return null;
        }
        return state.allRecords().stream()
                .filter(record -> record.id().equals(selectedMissionId))
                .findFirst()
                .orElse(null);
    }

    private MissionRecord focusRecord(List<MissionRecord> records) {
        List<MissionRecord> unlocked = records.stream()
                .filter(record -> !record.phaseLocked())
                .toList();
        return unlocked.stream()
                .filter(record -> record.role() == TerminalMissionRole.MAIN)
                .filter(record -> record.snapshot().status() == TerminalMissionStatus.CLAIMABLE)
                .findFirst()
                .or(() -> unlocked.stream()
                        .filter(record -> record.role() == TerminalMissionRole.MAIN)
                        .filter(record -> record.snapshot().status() == TerminalMissionStatus.UNLOCKED)
                        .findFirst())
                .or(() -> unlocked.stream()
                        .filter(record -> record.role() == TerminalMissionRole.MAIN)
                        .filter(record -> !isDone(record.snapshot().status()))
                        .findFirst())
                .or(() -> unlocked.stream()
                        .filter(record -> record.snapshot().status() == TerminalMissionStatus.CLAIMABLE)
                        .findFirst())
                .or(() -> unlocked.stream()
                        .filter(record -> !isDone(record.snapshot().status()))
                        .findFirst())
                .orElse(records.isEmpty() ? null : records.get(0));
    }

    private void selectMission(Identifier missionId, boolean focusTree) {
        if (missionId == null) {
            return;
        }
        if (!missionId.equals(selectedMissionId)) {
            selectedMissionId = missionId;
            detailScroll = 0;
            intelScroll = 0;
            lastDetailMissionId = missionId;
            invalidateStateCache();
        }
        if (focusTree) {
            pendingTreeFocus = TreeFocusMode.ENSURE_SELECTED_VISIBLE;
        }
    }

    private void selectMissionFilter(MissionFilter filter) {
        if (filter == null || missionFilter == filter) {
            return;
        }
        missionFilter = filter;
        treeScroll = 0;
        detailScroll = 0;
        intelScroll = 0;
        selectedMissionId = null;
        pendingTreeFocus = TreeFocusMode.ALIGN_SELECTED_TOP;
        invalidateStateCache();
    }

    private void syncDetailScrollWithSelection() {
        if (selectedMissionId == null) {
            lastDetailMissionId = null;
            detailScroll = 0;
            intelScroll = 0;
            return;
        }
        if (!selectedMissionId.equals(lastDetailMissionId)) {
            detailScroll = 0;
            intelScroll = 0;
            lastDetailMissionId = selectedMissionId;
        }
    }

    private void focusTreeOnSelection(MissionRenderState state) {
        if (pendingTreeFocus == TreeFocusMode.NONE || selectedMissionId == null || state.visibleRecords().isEmpty()) {
            return;
        }
        int rowY = selectedRowOffset(state);
        if (rowY < 0) {
            pendingTreeFocus = TreeFocusMode.NONE;
            return;
        }
        if (pendingTreeFocus == TreeFocusMode.ALIGN_SELECTED_TOP) {
            treeScroll = TerminalUi.clampScroll(rowY, lastTreeContentH, lastTreeH);
        } else if (rowY < treeScroll) {
            treeScroll = Math.max(0, rowY - TREE_FOCUS_EXTRA);
        } else if (rowY + missionRowHeight() > treeScroll + lastTreeH) {
            treeScroll = Math.max(0, rowY + missionRowHeight() - lastTreeH + TREE_FOCUS_EXTRA);
        }
        pendingTreeFocus = TreeFocusMode.NONE;
    }

    private int selectedRowOffset(MissionRenderState state) {
        int cy = 0;
        int phaseH = phaseRowHeight();
        int missionH = missionRowHeight();
        for (PhaseGroup phase : state.visiblePhases()) {
            cy += phaseH + 2;
            if (!isPhaseExpanded(phase)) {
                continue;
            }
            for (MissionRecord record : phase.records()) {
                if (record.id().equals(selectedMissionId)) {
                    return cy;
                }
                cy += missionH;
            }
            cy += 2;
        }
        return -1;
    }

    private boolean isPhaseExpanded(PhaseGroup phase) {
        if (collapsedPhases.contains(phase.id())) {
            return false;
        }
        if (expandedPhases.contains(phase.id())) {
            return true;
        }
        if (phase.records().stream().anyMatch(record -> record.id().equals(selectedMissionId))) {
            return true;
        }
        if (defaultPhaseExpansion == DefaultPhaseExpansion.FIRST_ONLY) {
            return phase.displayIndex() == 0;
        }
        if (phase.locked()) {
            return false;
        }
        if (phase.records().stream().anyMatch(record -> record.snapshot().status() == TerminalMissionStatus.CLAIMABLE)) {
            return true;
        }
        return !phase.complete();
    }

    private void togglePhase(PhaseGroup phase) {
        if (isPhaseExpanded(phase)) {
            expandedPhases.remove(phase.id());
            collapsedPhases.add(phase.id());
        } else {
            collapsedPhases.remove(phase.id());
            expandedPhases.add(phase.id());
        }
        invalidateStateCache();
    }

    private PhaseModel buildPhaseModel(List<MissionRecord> records) {
        return buildPhaseModel(records, false);
    }

    private PhaseModel buildPhaseModel(List<MissionRecord> records, boolean ignoreAnchoredCompletion) {
        Map<String, List<MissionRecord>> grouped = new LinkedHashMap<>();
        for (MissionRecord record : records) {
            grouped.computeIfAbsent(record.phaseKey(), ignored -> new ArrayList<>()).add(record);
        }
        List<PhaseGroup> sorted = new ArrayList<>();
        for (List<MissionRecord> group : grouped.values()) {
            MissionRecord first = group.get(0);
            sorted.add(new PhaseGroup(
                    first.phaseKey(),
                    "",
                    first.definition().phaseTitle(),
                    first.definition().phaseOrder(),
                    -1,
                    false,
                    phaseComplete(ignoreAnchoredCompletion
                            ? group.stream().filter(record -> record.routeAnchor().isEmpty()).toList()
                            : group),
                    "",
                    group));
        }
        sorted.sort(Comparator.comparingInt(PhaseGroup::order).thenComparing(PhaseGroup::id));
        List<PhaseGroup> phases = new ArrayList<>();
        Map<String, PhaseGroup> byId = new LinkedHashMap<>();
        boolean unlocked = true;
        PhaseGroup blocking = null;
        for (int i = 0; i < sorted.size(); i++) {
            PhaseGroup seed = sorted.get(i);
            boolean locked = !unlocked;
            String label = phaseDisplayLabel(seed.contextTitle(), i);
            String hint = locked && blocking != null
                    ? "Complete " + blocking.label() + " main objectives to unlock"
                    : "";
            PhaseGroup phase = new PhaseGroup(seed.id(), label, seed.contextTitle(), seed.order(), i,
                    locked, seed.complete(), hint, seed.records());
            phases.add(phase);
            byId.put(phase.id(), phase);
            if (!locked && !phase.complete()) {
                unlocked = false;
                blocking = phase;
            }
        }
        return new PhaseModel(List.copyOf(phases), Map.copyOf(byId));
    }

    private List<PhaseGroup> visiblePhases(PhaseModel phaseModel, List<MissionRecord> visibleRecords) {
        Map<String, List<MissionRecord>> grouped = new LinkedHashMap<>();
        for (MissionRecord record : visibleRecords) {
            grouped.computeIfAbsent(record.phaseKey(), ignored -> new ArrayList<>()).add(record);
        }
        List<PhaseGroup> visible = new ArrayList<>();
        for (PhaseGroup phase : phaseModel.phases()) {
            List<MissionRecord> records = grouped.get(phase.id());
            if (records != null && !records.isEmpty()) {
                visible.add(phase.withRecords(records));
            }
        }
        return List.copyOf(visible);
    }

    private List<MissionRecord> recordsForPhase(List<MissionRecord> records, String phaseId) {
        return records.stream()
                .filter(record -> record.phaseKey().equals(phaseId))
                .toList();
    }

    private int completedCount(List<MissionRecord> records) {
        int count = 0;
        for (MissionRecord record : records) {
            if (isDone(record.snapshot().status())) {
                count++;
            }
        }
        return count;
    }

    private static boolean phaseComplete(List<MissionRecord> records) {
        for (MissionRecord record : records) {
            if (record.role() == TerminalMissionRole.MAIN && !isGateComplete(record.snapshot().status())) {
                return false;
            }
        }
        return true;
    }

    private static boolean isGateComplete(TerminalMissionStatus status) {
        return status == TerminalMissionStatus.COMPLETED
                || status == TerminalMissionStatus.CLAIMED
                || status == TerminalMissionStatus.CLAIMABLE;
    }

    private static String phaseKey(TerminalMissionDefinition definition) {
        return definition.phaseOrder() + "::" + definition.phaseId();
    }

    private static int statusColor(TerminalMissionStatus status) {
        return switch (status) {
            case COMPLETED, CLAIMED, CLAIMABLE -> TerminalUi.GREEN;
            case UNLOCKED -> TerminalUi.AMBER;
            case LOCKED, VIEW_ONLY -> TerminalUi.MUTED;
        };
    }

    private static int missionTitleColor(TerminalMissionStatus status, boolean selected, int statusColor) {
        if (selected) {
            return TerminalUi.TEXT;
        }
        return switch (status) {
            case CLAIMABLE, UNLOCKED -> statusColor;
            case COMPLETED, CLAIMED -> TerminalUi.GREEN;
            case LOCKED, VIEW_ONLY -> TerminalUi.MUTED;
        };
    }

    private static String compactStatusLabel(TerminalMissionSnapshot snapshot) {
        return switch (snapshot.status()) {
            case UNLOCKED -> "ACTIVE";
            case CLAIMABLE -> "READY TO CLAIM";
            case COMPLETED, CLAIMED -> "DONE";
            case VIEW_ONLY -> "REFERENCE";
            case LOCKED -> "LOCKED";
        };
    }

    private static String roadmapStatusLabel(TerminalMissionStatus status, boolean locked) {
        if (locked) {
            return "LOCKED";
        }
        return switch (status == null ? TerminalMissionStatus.LOCKED : status) {
            case CLAIMABLE -> "READY";
            case UNLOCKED -> "ACTIVE";
            case COMPLETED, CLAIMED -> "DONE";
            case VIEW_ONLY -> "INFO";
            case LOCKED -> "LOCKED";
        };
    }

    private static int actionHintColor(TerminalMissionSnapshot snapshot) {
        return switch (snapshot.status()) {
            case COMPLETED, CLAIMABLE, CLAIMED -> TerminalUi.GREEN;
            case UNLOCKED -> TerminalUi.AMBER;
            case LOCKED, VIEW_ONLY -> TerminalUi.MUTED;
        };
    }

    private static String commandSummary(TerminalMissionSnapshot snapshot, TerminalMissionPresentation presentation) {
        return switch (snapshot.status()) {
            case CLAIMABLE -> "Reward cache is ready. Claim it here before moving on.";
            case COMPLETED, CLAIMED -> "Protocol complete. Any pending cache remains available here.";
            case UNLOCKED -> presentation.nextStep();
            case VIEW_ONLY -> "Reference record. Track it if you want it pinned to the Command Deck.";
            case LOCKED -> snapshot.unlockReason().isBlank() ? presentation.nextStep() : snapshot.unlockReason();
        };
    }

    private static String previewText(String value, String fallback, boolean locked) {
        String text = emptyFallback(value, fallback);
        return locked ? mysticCipher(text) : text;
    }

    private static String mysticCipher(String text) {
        String alphabet = "AZURETHOMNIVKSLY";
        StringBuilder cipher = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                cipher.append(alphabet.charAt((c - 'A' + i) % alphabet.length()));
            } else if (c >= 'a' && c <= 'z') {
                cipher.append(Character.toLowerCase(alphabet.charAt((c - 'a' + i) % alphabet.length())));
            } else if (c >= '0' && c <= '9') {
                cipher.append((char) ('0' + (c - '0' + i) % 10));
            } else {
                cipher.append(c);
            }
        }
        return cipher.toString();
    }

    private static String tagLine(TerminalMissionDefinition mission,
            TerminalMissionPresentation presentation, TerminalMissionRole role) {
        List<String> tags = new ArrayList<>();
        if (role == TerminalMissionRole.OPTIONAL) {
            tags.add("Optional");
        } else if (role == TerminalMissionRole.REFERENCE) {
            tags.add("Reference");
        }
        if (!presentation.routeHint().isBlank()) {
            tags.add(presentation.routeHint());
        }
        tags.addAll(presentation.tags());
        if (tags.isEmpty()) {
            tags.add(emptyFallback(mission.category(), "Mission"));
            tags.add(emptyFallback(mission.difficulty(), "Standard"));
        }
        return String.join(" / ", tags);
    }

    private static String roleLabel(TerminalMissionRole role) {
        return switch (role) {
            case MAIN -> "Main progression";
            case OPTIONAL -> "Optional / nonblocking";
            case REFERENCE -> "Reference";
        };
    }

    private static String roleChipLabel(TerminalMissionRole role) {
        return switch (role) {
            case MAIN -> "MAIN";
            case OPTIONAL -> "OPTIONAL";
            case REFERENCE -> "REFERENCE";
        };
    }

    private List<MissionRecord> sideRecordsFor(MissionRenderState state, MissionRecord anchor) {
        if (!supportsRouteIntelLayout() || state == null || anchor == null) {
            return List.of();
        }
        return state.allRecords().stream()
                .filter(record -> record.routeAnchor().isPresent())
                .filter(record -> record.routeAnchor().filter(anchor.id()::equals).isPresent())
                .filter(record -> routePrerequisitesSatisfied(state, record))
                .sorted(Comparator
                        .comparingInt((MissionRecord record) -> record.definition().phaseOrder())
                        .thenComparingInt(record -> record.definition().missionOrder())
                        .thenComparing(record -> record.id().toString()))
                .toList();
    }

    private List<SideCardView> sideCardViewsFor(MissionRenderState state, MissionRecord anchor) {
        if (!supportsRouteIntelLayout() || state == null || anchor == null) {
            return List.of();
        }
        return sideRecordsFor(state, anchor).stream()
                .map(record -> sideCardView(state, record))
                .toList();
    }

    private SideCardView sideCardView(MissionRenderState state, MissionRecord record) {
        Identifier rootAnchor = routeRootAnchor(record, state).orElse(record.routeAnchor().orElse(record.id()));
        SideCardState stateLabel = sideCardState(record);
        TerminalMissionAction action = firstEnabledAction(record.snapshot().actions());
        boolean actionEnabled = action != null && !readOnly(record);
        String actionLabel = action == null ? "VIEW" : action.label();
        String intelCount = record.intelUnlocks().isEmpty()
                ? "NO INTEL"
                : record.intelUnlocks().size() + " INTEL";
        return new SideCardView(
                record,
                rootAnchor,
                record.routeAnchor(),
                stateLabel,
                action,
                actionEnabled,
                actionLabel,
                emptyFallback(record.presentation().shortTitle(), record.definition().title()),
                sideCardSummary(record),
                sideCardProgress(record),
                intelCount,
                sideCardColor(stateLabel, record),
                intelIcon(record));
    }

    private Optional<Identifier> routeRootAnchor(MissionRecord record, MissionRenderState state) {
        if (record == null || record.routeAnchor().isEmpty()) {
            return Optional.empty();
        }
        Map<Identifier, MissionRecord> byId = new LinkedHashMap<>();
        for (MissionRecord candidate : state.allRecords()) {
            byId.put(candidate.id(), candidate);
        }
        Identifier current = record.routeAnchor().get();
        Set<Identifier> seen = new LinkedHashSet<>();
        while (seen.add(current)) {
            MissionRecord parent = byId.get(current);
            if (parent == null || parent.routeAnchor().isEmpty()) {
                return Optional.of(current);
            }
            current = parent.routeAnchor().get();
        }
        return Optional.of(record.routeAnchor().get());
    }

    private static boolean routePrerequisitesSatisfied(MissionRenderState state, MissionRecord record) {
        if (record == null || record.routePrerequisites().isEmpty()) {
            return true;
        }
        Map<Identifier, MissionRecord> byId = new LinkedHashMap<>();
        for (MissionRecord candidate : state == null ? List.<MissionRecord>of() : state.allRecords()) {
            byId.put(candidate.id(), candidate);
        }
        for (Identifier prerequisite : record.routePrerequisites()) {
            MissionRecord gate = byId.get(prerequisite);
            if (gate != null && !routeGateComplete(gate.snapshot().status())) {
                return false;
            }
        }
        return true;
    }

    private static boolean routeGateComplete(TerminalMissionStatus status) {
        return status == TerminalMissionStatus.COMPLETED
                || status == TerminalMissionStatus.CLAIMED
                || status == TerminalMissionStatus.CLAIMABLE;
    }

    private static List<IntelUnlockRow> intelRowsFor(MissionRecord selected, List<SideCardView> sideCards) {
        Map<String, IntelUnlockRow> rows = new LinkedHashMap<>();
        if (selected != null) {
            SideCardState selectedState = sideCardState(selected);
            for (TerminalMissionIntelUnlock unlock : selected.intelUnlocks()) {
                addIntelRow(rows, unlock, selected, selectedState);
            }
        }
        for (SideCardView side : sideCards == null ? List.<SideCardView>of() : sideCards) {
            for (TerminalMissionIntelUnlock unlock : side.record().intelUnlocks()) {
                addIntelRow(rows, unlock, side.record(), side.state());
            }
        }
        return List.copyOf(rows.values());
    }

    private static void addIntelRow(
            Map<String, IntelUnlockRow> rows,
            TerminalMissionIntelUnlock unlock,
            MissionRecord owner,
            SideCardState state) {
        if (unlock == null) {
            return;
        }
        String key = unlock.kind().name() + "|" + unlock.id();
        rows.putIfAbsent(key, new IntelUnlockRow(
                unlock,
                owner,
                state,
                sideCardColor(state, owner),
                intelRowStateLabel(state)));
    }

    private static TerminalMissionAction firstEnabledAction(List<TerminalMissionAction> actions) {
        for (TerminalMissionAction action : actions == null ? List.<TerminalMissionAction>of() : actions) {
            if (action.enabled()) {
                return action;
            }
        }
        return null;
    }

    private static String sideProgress(List<SideCardView> sideCards) {
        int archived = (int) sideCards.stream()
                .filter(view -> view.state() == SideCardState.ARCHIVED)
                .count();
        return archived + "/" + sideCards.size() + " archived";
    }

    private static SideCardState sideCardState(MissionRecord record) {
        if (record == null || phaseLockedForCommands(record)) {
            return SideCardState.LOCKED;
        }
        return switch (record.snapshot().status()) {
            case CLAIMABLE -> SideCardState.READY;
            case COMPLETED, CLAIMED -> SideCardState.ARCHIVED;
            case UNLOCKED -> SideCardState.ACTIVE;
            case VIEW_ONLY, LOCKED -> SideCardState.LOCKED;
        };
    }

    private static String sideCardStatusLabel(SideCardState state) {
        return switch (state == null ? SideCardState.LOCKED : state) {
            case LOCKED -> "LOCKED";
            case ACTIVE -> "ACTIVE";
            case READY -> "READY";
            case ARCHIVED -> "ARCHIVED";
        };
    }

    private static String intelRowStateLabel(SideCardState state) {
        return switch (state == null ? SideCardState.LOCKED : state) {
            case LOCKED -> "LOCKED";
            case ACTIVE -> "UNLOCKED";
            case READY -> "READY";
            case ARCHIVED -> "ARCHIVED";
        };
    }

    private static String sideCardSummary(MissionRecord record) {
        if (record == null) {
            return "";
        }
        String summary = record.presentation().objectiveSummary();
        if (summary.isBlank()) {
            summary = record.definition().briefing();
        }
        return summary;
    }

    private static String sideCardProgress(MissionRecord record) {
        if (record == null) {
            return "";
        }
        SideCardState state = sideCardState(record);
        if (state == SideCardState.LOCKED) {
            return "Locked";
        }
        if (state == SideCardState.READY) {
            return "Ready to archive";
        }
        if (state == SideCardState.ARCHIVED) {
            return "Archived";
        }
        int have = 0;
        int need = 0;
        for (TerminalMissionRequirement requirement : record.definition().requirements()) {
            if (requirement.need() > 0) {
                have += Math.min(requirement.have(), requirement.need());
                need += requirement.need();
            }
        }
        if (need > 0) {
            return have + "/" + need + " complete";
        }
        return Math.round(record.snapshot().progress() * 100.0F) + "% complete";
    }

    private static int sideCardColor(SideCardState state, MissionRecord record) {
        if (state == SideCardState.LOCKED) {
            return TerminalUi.MUTED;
        }
        if (state == SideCardState.ARCHIVED) {
            return TerminalUi.GREEN;
        }
        if (state == SideCardState.READY) {
            return TerminalUi.AMBER;
        }
        return record == null ? TerminalUi.CYAN : record.intelUnlocks().stream()
                .findFirst()
                .map(unlock -> intelKindColor(unlock.kind()))
                .orElse(TerminalUi.CYAN);
    }

    private static TerminalIcon intelIcon(MissionRecord record) {
        TerminalMissionIntelKind kind = record == null || record.intelUnlocks().isEmpty()
                ? TerminalMissionIntelKind.DISCOVERY
                : record.intelUnlocks().get(0).kind();
        return switch (kind) {
            case ARCHIVE -> TerminalIcon.ARCHIVES;
            case ROUTE, POI -> TerminalIcon.WORLD;
            case FACTION -> TerminalIcon.FIELD;
            case DISCOVERY -> TerminalIcon.SEARCH;
        };
    }

    private static TerminalIcon intelKindIcon(TerminalMissionIntelKind kind) {
        return switch (kind == null ? TerminalMissionIntelKind.DISCOVERY : kind) {
            case ARCHIVE -> TerminalIcon.ARCHIVES;
            case ROUTE, POI -> TerminalIcon.WORLD;
            case FACTION -> TerminalIcon.FIELD;
            case DISCOVERY -> TerminalIcon.SEARCH;
        };
    }

    private static int intelKindColor(TerminalMissionIntelKind kind) {
        return switch (kind == null ? TerminalMissionIntelKind.DISCOVERY : kind) {
            case ARCHIVE -> 0xFFB889F5;
            case ROUTE -> 0xFF3EDFA3;
            case DISCOVERY -> TerminalUi.CYAN;
            case FACTION -> 0xFFFF5B8B;
            case POI -> 0xFF5DA8FF;
        };
    }

    private static String intelOverflowLabel(int hidden) {
        return "+" + Math.max(0, hidden) + " more signals";
    }

    private static int readyRewardCount(MissionRenderState state) {
        if (state == null) {
            return 0;
        }
        int count = 0;
        for (MissionRecord record : state.visibleRecords()) {
            if (!record.phaseLocked() && record.snapshot().status() == TerminalMissionStatus.CLAIMABLE) {
                count++;
            }
        }
        return count;
    }

    private static int activeCount(List<MissionRecord> records) {
        int count = 0;
        for (MissionRecord record : records) {
            if (!record.phaseLocked()
                    && (record.snapshot().status() == TerminalMissionStatus.UNLOCKED
                            || record.snapshot().status() == TerminalMissionStatus.CLAIMABLE)) {
                count++;
            }
        }
        return count;
    }

    private static int readyCount(List<MissionRecord> records) {
        int count = 0;
        for (MissionRecord record : records) {
            if (!record.phaseLocked() && record.snapshot().status() == TerminalMissionStatus.CLAIMABLE) {
                count++;
            }
        }
        return count;
    }

    private static int lockedCount(List<MissionRecord> records) {
        int count = 0;
        for (MissionRecord record : records) {
            if (record.phaseLocked()
                    || record.snapshot().status() == TerminalMissionStatus.LOCKED
                    || record.snapshot().status() == TerminalMissionStatus.VIEW_ONLY) {
                count++;
            }
        }
        return count;
    }

    private static MissionRecord firstActiveRecord(MissionRenderState state) {
        if (state == null) {
            return null;
        }
        for (MissionRecord record : state.allRecords()) {
            if (!record.phaseLocked()
                    && (record.snapshot().status() == TerminalMissionStatus.UNLOCKED
                            || record.snapshot().status() == TerminalMissionStatus.CLAIMABLE)) {
                return record;
            }
        }
        return null;
    }

    private static String missionRowTitle(MissionRecord record) {
        if (record == null) {
            return "";
        }
        String title = emptyFallback(record.presentation().shortTitle(), record.definition().title());
        if (MainSurvivalQuestProvider.CHAPTER_ID.equals(record.definition().chapterId())) {
            String source = emptyFallback(record.definition().category(), "");
            if (!source.isBlank() && !source.equalsIgnoreCase(title)) {
                return source + " / " + title;
            }
        }
        return title;
    }

    private static String missionRowSubtitle(MissionRecord record) {
        if (record == null) {
            return "";
        }
        String title = missionRowTitle(record);
        String routeHint = emptyFallback(record.presentation().routeHint(), "");
        boolean sourceHint = !routeHint.isBlank()
                && !routeHint.equalsIgnoreCase(record.phaseLabel())
                && !routeHint.equalsIgnoreCase(record.phaseContext())
                && !routeHint.equalsIgnoreCase(title);
        if (sourceHint) {
            return routeHint;
        }
        return emptyFallback(record.definition().category(), record.phaseContext());
    }

    private static String detailPhaseChip(MissionRecord record) {
        if (record == null) {
            return "STAGE 1";
        }
        String label = emptyFallback(record.phaseLabel(), "");
        if (label.isBlank() || isNumericPhaseLabel(label)) {
            return "STAGE " + Math.max(1, record.definition().phaseOrder() + 1);
        }
        if (label.toLowerCase(Locale.ROOT).startsWith("stage ")) {
            return label.toUpperCase(Locale.ROOT);
        }
        return label;
    }

    private static String phaseDisplayLabel(String contextTitle, int displayIndex) {
        String title = emptyFallback(contextTitle, "").trim();
        if (title.isBlank() || isNumericPhaseLabel(title)) {
            return "Stage " + (displayIndex + 1);
        }
        return title;
    }

    private static boolean isNumericPhaseLabel(String value) {
        if (value == null) {
            return false;
        }
        String lower = value.trim().toLowerCase(Locale.ROOT);
        if (!lower.startsWith("phase")) {
            return false;
        }
        String rest = lower.substring("phase".length())
                .replace("_", "")
                .replace("-", "")
                .trim();
        if (rest.isBlank()) {
            return false;
        }
        for (int i = 0; i < rest.length(); i++) {
            if (!Character.isDigit(rest.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static String firstDisabledReason(List<TerminalMissionAction> actions) {
        for (TerminalMissionAction action : actions) {
            if (!action.enabled() && !action.disabledReason().isBlank()) {
                return action.label() + ": " + action.disabledReason();
            }
        }
        return "";
    }

    private static Identifier actionIcon(TerminalRenderContext context, TerminalMissionAction action) {
        String value = ((action == null ? "" : action.id()) + " " + (action == null ? "" : action.label()))
                .toLowerCase(Locale.ROOT);
        if (value.contains("claim") || value.contains("reward")) {
            return TerminalUi.themedActionIcon(context, "claim", TerminalVisualAssets.ICON_ACTION_CLAIM);
        }
        if (value.contains("turn") || value.contains("submit") || value.contains("finish")) {
            return TerminalUi.themedActionIcon(context, "turn_in", TerminalVisualAssets.ICON_ACTION_TURN_IN);
        }
        if (value.contains("scan")) {
            return TerminalUi.themedActionIcon(context, "scan", TerminalVisualAssets.ICON_ACTION_SCAN);
        }
        if (value.contains("open")) {
            return TerminalUi.themedActionIcon(context, "open", TerminalVisualAssets.ICON_ACTION_OPEN_ROADMAP);
        }
        return TerminalUi.themedActionIcon(context, "view", TerminalVisualAssets.ICON_ACTION_VIEW);
    }

    private static String intelLabel(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        String cleaned = key;
        int colon = cleaned.indexOf(':');
        if (colon >= 0 && colon + 1 < cleaned.length()) {
            cleaned = cleaned.substring(colon + 1);
        }
        if (cleaned.startsWith("ashfall_")) {
            cleaned = cleaned.substring("ashfall_".length());
        }
        String[] words = cleaned.replace('_', ' ').split(" ");
        StringBuilder label = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (label.length() > 0) {
                label.append(' ');
            }
            label.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                label.append(word.substring(1));
            }
        }
        return label.length() == 0 ? key : label.toString();
    }

    private static boolean isDone(TerminalMissionStatus status) {
        return status == TerminalMissionStatus.COMPLETED || status == TerminalMissionStatus.CLAIMED;
    }

    private static boolean phaseLockedForCommands(MissionRecord record) {
        return record != null && record.phaseLocked() && record.routeAnchor().isEmpty();
    }

    private static boolean visible(int y, int h, int viewportY, int viewportH) {
        return y + h >= viewportY && y <= viewportY + viewportH;
    }

    private static String emptyFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String compactText(String value, int maxChars) {
        String clean = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        int limit = Math.max(12, maxChars);
        return clean.length() <= limit ? clean : clean.substring(0, Math.max(0, limit - 3)) + "...";
    }

    private int withHitboxClip(int x, int y, int w, int h, IntSupplier action) {
        boolean previousActive = hitboxClipActive;
        int previousX = hitboxClipX;
        int previousY = hitboxClipY;
        int previousW = hitboxClipW;
        int previousH = hitboxClipH;
        hitboxClipActive = true;
        hitboxClipX = x;
        hitboxClipY = y;
        hitboxClipW = w;
        hitboxClipH = h;
        try {
            return action.getAsInt();
        } finally {
            hitboxClipActive = previousActive;
            hitboxClipX = previousX;
            hitboxClipY = previousY;
            hitboxClipW = previousW;
            hitboxClipH = previousH;
        }
    }

    private void addHitbox(int x, int y, int w, int h, boolean enabled, Runnable action) {
        if (!hitboxClipActive) {
            hitboxes.add(new Hitbox(x, y, w, h, enabled, action));
            return;
        }
        int clippedX = Math.max(x, hitboxClipX);
        int clippedY = Math.max(y, hitboxClipY);
        int clippedRight = Math.min(x + w, hitboxClipX + hitboxClipW);
        int clippedBottom = Math.min(y + h, hitboxClipY + hitboxClipH);
        if (clippedRight <= clippedX || clippedBottom <= clippedY) {
            return;
        }
        hitboxes.add(new Hitbox(clippedX, clippedY, clippedRight - clippedX, clippedBottom - clippedY,
                enabled, action));
    }

    private void invalidateStateCache() {
        cachedState = null;
        cachedStateKey = null;
        staleServedKey = null;
        cachedStateFrame = -1L;
    }

    private record CacheKey(
            String providerName,
            Identifier tabId,
            UUID playerId,
            int widthBucket,
            int refreshTick) {
    }

    private record MissionRenderState(
            List<MissionRecord> allRecords,
            List<MissionRecord> visibleRecords,
            List<PhaseGroup> allPhases,
            List<PhaseGroup> visiblePhases,
            MissionRecord focusRecord,
            int completedCount) {
    }

    private record SideCardView(
            MissionRecord record,
            Identifier rootAnchor,
            Optional<Identifier> directAnchor,
            SideCardState state,
            TerminalMissionAction primaryAction,
            boolean actionEnabled,
            String actionLabel,
            String title,
            String summary,
            String progressLabel,
            String intelCountLabel,
            int color,
            TerminalIcon icon) {
        SideCardView {
            directAnchor = directAnchor == null ? Optional.empty() : directAnchor;
            actionLabel = emptyFallback(actionLabel, "VIEW");
            title = emptyFallback(title, "");
            summary = emptyFallback(summary, "");
            progressLabel = emptyFallback(progressLabel, "");
            intelCountLabel = emptyFallback(intelCountLabel, "NO INTEL");
        }
    }

    private record IntelUnlockRow(
            TerminalMissionIntelUnlock unlock,
            MissionRecord owner,
            SideCardState state,
            int color,
            String stateLabel) {
        IntelUnlockRow {
            state = state == null ? SideCardState.LOCKED : state;
            stateLabel = emptyFallback(stateLabel, sideCardStatusLabel(state));
        }
    }

    private record MissionRecord(
            TerminalMissionDefinition definition,
            TerminalMissionSnapshot snapshot,
            TerminalMissionPresentation presentation,
            TerminalMissionVisuals visuals,
            TerminalMissionRole role,
            List<Identifier> routePrerequisites,
            Optional<Identifier> routeAnchor,
            List<TerminalMissionIntelUnlock> intelUnlocks,
            String phaseKey,
            String phaseLabel,
            String phaseContext,
            boolean phaseLocked,
            String phaseUnlockHint) {
        MissionRecord {
            routePrerequisites = List.copyOf(routePrerequisites == null ? List.of() : routePrerequisites);
            routeAnchor = routeAnchor == null ? Optional.empty() : routeAnchor;
            intelUnlocks = List.copyOf(intelUnlocks == null ? List.of() : intelUnlocks);
        }

        MissionRecord(
                TerminalMissionDefinition definition,
                TerminalMissionSnapshot snapshot,
                TerminalMissionPresentation presentation,
            TerminalMissionVisuals visuals,
            TerminalMissionRole role) {
            this(definition, snapshot, presentation, visuals, role, List.of(), Optional.empty(), List.of(),
                    TerminalMissionBrowser.phaseKey(definition), "", definition.phaseTitle(), false, "");
        }

        MissionRecord(
                TerminalMissionDefinition definition,
                TerminalMissionSnapshot snapshot,
                TerminalMissionPresentation presentation,
                TerminalMissionVisuals visuals,
                TerminalMissionRole role,
                List<Identifier> routePrerequisites,
                Optional<Identifier> routeAnchor,
                List<TerminalMissionIntelUnlock> intelUnlocks) {
            this(definition, snapshot, presentation, visuals, role, routePrerequisites, routeAnchor, intelUnlocks,
                    TerminalMissionBrowser.phaseKey(definition), "", definition.phaseTitle(), false, "");
        }

        Identifier id() {
            return definition.id();
        }

        MissionRecord withPhase(PhaseGroup phase) {
            if (phase == null) {
                return this;
            }
            return new MissionRecord(definition, snapshot, presentation, visuals, role, routePrerequisites,
                    routeAnchor, intelUnlocks,
                    phase.id(), phase.label(), phase.contextTitle(), phase.locked(), phase.unlockHint());
        }
    }

    private record PhaseGroup(
            String id,
            String label,
            String contextTitle,
            int order,
            int displayIndex,
            boolean locked,
            boolean complete,
            String unlockHint,
            List<MissionRecord> records) {
        PhaseGroup withRecords(List<MissionRecord> records) {
            return new PhaseGroup(id, label, contextTitle, order, displayIndex, locked, complete, unlockHint, records);
        }

        String stateLabel() {
            if (locked) {
                return "LOCKED";
            }
            return complete ? "COMPLETE" : "ACTIVE";
        }
    }

    private record PhaseModel(List<PhaseGroup> phases, Map<String, PhaseGroup> byId) {
        PhaseGroup phase(String id) {
            return byId.get(id);
        }
    }

    private record Hitbox(int x, int y, int w, int h, boolean enabled, Runnable action) {
    }

    private record CommandButton(
            String label,
            boolean enabled,
            String disabledReason,
            Identifier icon,
            boolean primary,
            Runnable action) {
    }

    private enum TreeFocusMode {
        NONE,
        ALIGN_SELECTED_TOP,
        ENSURE_SELECTED_VISIBLE
    }

}
