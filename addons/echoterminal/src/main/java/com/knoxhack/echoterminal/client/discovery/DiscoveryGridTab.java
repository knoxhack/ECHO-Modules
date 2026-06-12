package com.knoxhack.echoterminal.client.discovery;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoDiscoveryCategory;
import com.echoplatform.echocore.api.EchoDiscoveryEntry;
import com.echoplatform.echocore.api.EchoDiscoveryState;
import com.echoplatform.echocore.api.EchoResolvedDiscoveryEntry;
import com.knoxhack.echoterminal.EchoTerminal;
import com.knoxhack.echoterminal.api.TerminalRenderContext;
import com.knoxhack.echoterminal.api.ClientTerminalTab;
import com.knoxhack.echoterminal.api.TerminalTab;
import com.knoxhack.echoterminal.api.TerminalTabChrome;
import com.knoxhack.echoterminal.api.TerminalTabDescriptor;
import com.knoxhack.echoterminal.api.TerminalUi;
import com.knoxhack.echoterminal.api.TerminalVisualAssets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class DiscoveryGridTab implements ClientTerminalTab {
    public static final Identifier TAB_ID = Identifier.fromNamespaceAndPath(EchoTerminal.MODID, "discovery_grid");
    private static final int GAP = 8;
    private static final int CARD_H = 116;

    private final TerminalTabDescriptor descriptor =
            new TerminalTabDescriptor(TAB_ID, "DISCOVERY GRID", 126, 0xFF66E8FF);
    private final TerminalTabChrome chrome =
            TerminalTabChrome.of("Discovery Grid", TerminalTabChrome.GROUP_FIELD, "DG",
                    "Spoiler-safe discovered features", 126);
    private EchoDiscoveryCategory categoryFilter;
    private StateFilter stateFilter = StateFilter.ALL;
    private List<ChipRect> categoryRects = List.of();
    private List<ChipRect> stateRects = List.of();
    private Player cachedPlayer;
    private long cachedGameTime = Long.MIN_VALUE;
    private EchoDiscoveryCategory cachedCategoryFilter;
    private StateFilter cachedStateFilter;
    private DiscoverySnapshot cachedSnapshot = DiscoverySnapshot.empty();

    @Override
    public TerminalTabDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public TerminalTabChrome chrome() {
        return chrome;
    }

    @Override
    public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int mouseX, int mouseY, float partialTick) {
        int x = context.contentX();
        int y = context.contentY();
        int w = context.contentWidth();
        DiscoverySnapshot snapshot = discoverySnapshot(context);
        List<EchoResolvedDiscoveryEntry> visible = snapshot.visible();
        Counts counts = snapshot.counts();

        int cy = TerminalUi.flatDataPanel(context, graphics, x, y, w, 92,
                "DISCOVERY GRID", summary(counts), descriptor.accentColor()) + 6;
        int filterH = categoryFilterHeight(w - 20) + stateFilterHeight(w - 20) + 22;
        TerminalUi.filterToolbarPanel(context, graphics, x, cy - 4, w, filterH, descriptor.accentColor());
        cy = renderCategoryFilters(context, graphics, x + 10, cy + 4, w - 20, mouseX, mouseY) + 6;
        cy = renderStateFilters(context, graphics, x + 10, cy, w - 20, mouseX, mouseY) + 14;

        if (visible.isEmpty()) {
            TerminalUi.emptyState(context, graphics, x + 10, cy, w - 20,
                    "NO SIGNALS MATCH", "Change the category or status filter to inspect the grid.", descriptor.accentColor());
            return;
        }

        int columns = columns(w);
        int cardW = (w - 20 - (columns - 1) * GAP) / columns;
        int startX = x + 10;
        int viewportTop = context.contentY() + context.scrollY() - GAP;
        int viewportBottom = viewportTop + context.contentHeight() + GAP * 2;
        for (int i = 0; i < visible.size(); i++) {
            int col = i % columns;
            int row = i / columns;
            int cardX = startX + col * (cardW + GAP);
            int cardY = cy + row * (CARD_H + GAP);
            if (cardY + CARD_H < viewportTop || cardY > viewportBottom) {
                continue;
            }
            renderCard(context, graphics, visible.get(i), cardX, cardY, cardW, CARD_H,
                    TerminalUi.inside(mouseX, mouseY, cardX, cardY, cardW, CARD_H));
        }
    }

    @Override
    public int contentHeight(TerminalRenderContext context) {
        int w = context.contentWidth();
        int columns = columns(w);
        int rows = (int) Math.ceil(discoverySnapshot(context).visible().size() / (double) columns);
        int controls = 142 + categoryFilterHeight(w - 20) + stateFilterHeight(w - 20) - 30;
        return Math.max(context.contentHeight(), controls + Math.max(1, rows) * (CARD_H + GAP));
    }

    @Override
    public boolean mouseClicked(TerminalRenderContext context, double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        for (ChipRect rect : categoryRects) {
            if (TerminalUi.inside(mouseX, mouseY, rect.x(), rect.y(), rect.w(), rect.h())) {
                categoryFilter = rect.category();
                context.playCommandSound();
                return true;
            }
        }
        for (ChipRect rect : stateRects) {
            if (TerminalUi.inside(mouseX, mouseY, rect.x(), rect.y(), rect.w(), rect.h())) {
                stateFilter = rect.stateFilter();
                context.playCommandSound();
                return true;
            }
        }
        return false;
    }

    public static List<EchoDiscoveryEntry> visibleEntriesForTests(
            Player player, EchoDiscoveryCategory category, EchoDiscoveryState state) {
        StateFilter filter = state == null ? StateFilter.ALL : StateFilter.valueOf(state.name());
        return filter(resolvedEntries(player), category, filter).stream()
                .map(EchoResolvedDiscoveryEntry::entry)
                .toList();
    }

    private void renderCard(TerminalRenderContext context, GuiGraphicsExtractor graphics, EchoResolvedDiscoveryEntry resolved,
            int x, int y, int w, int h, boolean hovered) {
        EchoDiscoveryEntry entry = resolved.entry();
        EchoDiscoveryState state = resolved.state();
        boolean revealed = state != EchoDiscoveryState.LOCKED;
        int accent = state == EchoDiscoveryState.CHECKED ? TerminalUi.GREEN
                : state == EchoDiscoveryState.DISCOVERED ? entry.accentColor()
                : TerminalUi.CYAN_DIM;
        Identifier hero = revealed ? entry.heroArt() : TerminalVisualAssets.CARD_PANEL_ROUTE_MAP;
        TerminalUi.imagePanel(context, graphics, hero, x, y, w, h, accent, revealed ? 0.76F : 0.90F, true);
        if (hovered) {
            graphics.outline(x, y, w, h, TerminalUi.opaque(accent));
        }
        graphics.fill(x, y, x + 4, y + h, TerminalUi.opaque(accent));

        Identifier icon = revealed ? entry.iconArt() : TerminalVisualAssets.ICON_STATE_LOCKED;
        TerminalUi.iconTextureBadge(context, graphics, icon, x + 9, y + 10, 28, accent, revealed);
        TerminalUi.miniStatusPill(context, graphics, stateLabel(state), x + w - 76, y + 12, 64, accent, state == EchoDiscoveryState.CHECKED);
        TerminalUi.line(context, graphics, entry.chapterId().getPath().replace('_', ' ').toUpperCase(Locale.ROOT),
                x + 44, y + 9, Math.max(40, w - 126), TerminalUi.MUTED);
        TerminalUi.line(context, graphics, revealed ? entry.revealedTitle() : entry.lockedHintTitle(),
                x + 44, y + 22, Math.max(40, w - 126), revealed ? TerminalUi.text(context) : TerminalUi.MUTED);
        TerminalUi.line(context, graphics, entry.category().displayName(),
                x + 10, y + 45, Math.max(40, w - 20), accent);
        int bodyEnd = TerminalUi.wrap(context, graphics, revealed ? entry.revealedSummary() : entry.hintText(),
                x + 10, y + 58, Math.max(40, w - 20), revealed ? TerminalUi.text(context) : TerminalUi.MUTED);
        String progress = switch (state) {
            case CHECKED -> "Checked by live progression";
            case DISCOVERED -> "Discovered and added to this terminal";
            case LOCKED -> "Locked: hints only";
        };
        TerminalUi.line(context, graphics, progress, x + 10, Math.min(y + h - 24, bodyEnd + 3),
                Math.max(40, w - 20), accent);
        if (entry.relatedMissionId() != null && revealed) {
            TerminalUi.line(context, graphics, "Mission link: " + entry.relatedMissionId().getPath(),
                    x + 10, y + h - 12, Math.max(40, w - 20), TerminalUi.MUTED);
        }
    }

    private int renderCategoryFilters(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, int mouseX, int mouseY) {
        List<ChipRect> rects = new ArrayList<>();
        int columns = w < 360 ? 3 : 6;
        int chipW = Math.max(44, (w - (columns - 1) * GAP) / columns);
        int cx = x;
        int cy = y;
        int index = 0;
        renderChip(context, graphics, cx, cy, chipW, "All", categoryFilter == null,
                TerminalUi.inside(mouseX, mouseY, cx, cy, chipW, 15));
        rects.add(new ChipRect(cx, cy, chipW, 15, null, null));
        index++;
        cx += chipW + GAP;
        for (EchoDiscoveryCategory category : EchoDiscoveryCategory.values()) {
            if (index % columns == 0) {
                cx = x;
                cy += 18;
            }
            renderChip(context, graphics, cx, cy, chipW, chipLabel(category.displayName()), categoryFilter == category,
                    TerminalUi.inside(mouseX, mouseY, cx, cy, chipW, 15));
            rects.add(new ChipRect(cx, cy, chipW, 15, category, null));
            index++;
            cx += chipW + GAP;
        }
        categoryRects = rects;
        return cy + 15;
    }

    private int renderStateFilters(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, int mouseX, int mouseY) {
        List<ChipRect> rects = new ArrayList<>();
        int columns = w < 300 ? 2 : 4;
        int chipW = Math.max(58, (w - (columns - 1) * GAP) / columns);
        int cx = x;
        int cy = y;
        int index = 0;
        for (StateFilter filter : StateFilter.values()) {
            if (index > 0 && index % columns == 0) {
                cx = x;
                cy += 18;
            }
            renderChip(context, graphics, cx, cy, chipW, filter.label(), stateFilter == filter,
                    TerminalUi.inside(mouseX, mouseY, cx, cy, chipW, 15));
            rects.add(new ChipRect(cx, cy, chipW, 15, null, filter));
            index++;
            cx += chipW + GAP;
        }
        stateRects = rects;
        return cy + 15;
    }

    private void renderChip(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, String label, boolean selected, boolean hovered) {
        TerminalUi.filterChip(context, graphics, x, y, w, label, selected, true,
                descriptor.accentColor(), hovered);
    }

    private DiscoverySnapshot discoverySnapshot(TerminalRenderContext context) {
        Player player = context.player();
        long gameTime = player == null || player.level() == null ? 0L : player.level().getGameTime();
        if (cachedSnapshot != null
                && cachedPlayer == player
                && cachedGameTime == gameTime
                && cachedCategoryFilter == categoryFilter
                && cachedStateFilter == stateFilter) {
            return cachedSnapshot;
        }
        List<EchoResolvedDiscoveryEntry> all = resolvedEntries(player);
        List<EchoResolvedDiscoveryEntry> visible = filter(all, categoryFilter, stateFilter);
        cachedPlayer = player;
        cachedGameTime = gameTime;
        cachedCategoryFilter = categoryFilter;
        cachedStateFilter = stateFilter;
        cachedSnapshot = new DiscoverySnapshot(all, visible, Counts.of(all));
        return cachedSnapshot;
    }

    private static List<EchoResolvedDiscoveryEntry> resolvedEntries(Player player) {
        return EchoCoreServices.resolvedDiscoveryEntries(player);
    }

    private static List<EchoResolvedDiscoveryEntry> filter(
            List<EchoResolvedDiscoveryEntry> entries, EchoDiscoveryCategory category, StateFilter state) {
        return entries.stream()
                .filter(entry -> category == null || entry.entry().category() == category)
                .filter(entry -> state == null || state == StateFilter.ALL || entry.state() == state.state())
                .toList();
    }

    private static int columns(int width) {
        if (width >= 620) {
            return 3;
        }
        if (width >= 390) {
            return 2;
        }
        return 1;
    }

    private static int categoryFilterHeight(int width) {
        return width < 360 ? 33 : 15;
    }

    private static int stateFilterHeight(int width) {
        return width < 300 ? 33 : 15;
    }

    private static String summary(Counts counts) {
        return counts.checked() + " checked / " + counts.discovered() + " discovered / " + counts.locked() + " locked";
    }

    private static String chipLabel(String label) {
        return label == null || label.length() <= 10 ? label : label.substring(0, 9);
    }

    private static String stateLabel(EchoDiscoveryState state) {
        return switch (state) {
            case CHECKED -> "CHECKED";
            case DISCOVERED -> "DISC";
            case LOCKED -> "LOCKED";
        };
    }

    private enum StateFilter {
        ALL("All", null),
        DISCOVERED("Discovered", EchoDiscoveryState.DISCOVERED),
        CHECKED("Checked", EchoDiscoveryState.CHECKED),
        LOCKED("Locked", EchoDiscoveryState.LOCKED);

        private final String label;
        private final EchoDiscoveryState state;

        StateFilter(String label, EchoDiscoveryState state) {
            this.label = label;
            this.state = state;
        }

        String label() {
            return label;
        }

        EchoDiscoveryState state() {
            return state;
        }
    }

    private record DiscoverySnapshot(
            List<EchoResolvedDiscoveryEntry> all,
            List<EchoResolvedDiscoveryEntry> visible,
            Counts counts) {
        private static DiscoverySnapshot empty() {
            return new DiscoverySnapshot(List.of(), List.of(), Counts.of(List.of()));
        }
    }

    private record ChipRect(int x, int y, int w, int h, EchoDiscoveryCategory category, StateFilter stateFilter) {
    }

    private record Counts(int checked, int discovered, int locked) {
        static Counts of(List<EchoResolvedDiscoveryEntry> entries) {
            EnumMap<EchoDiscoveryState, Integer> counts = new EnumMap<>(EchoDiscoveryState.class);
            for (EchoResolvedDiscoveryEntry entry : entries) {
                counts.merge(entry.state(), 1, Integer::sum);
            }
            return new Counts(
                    counts.getOrDefault(EchoDiscoveryState.CHECKED, 0),
                    counts.getOrDefault(EchoDiscoveryState.DISCOVERED, 0),
                    counts.getOrDefault(EchoDiscoveryState.LOCKED, 0));
        }
    }
}
