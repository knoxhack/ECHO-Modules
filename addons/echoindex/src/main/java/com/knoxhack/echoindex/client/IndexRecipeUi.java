package com.knoxhack.echoindex.client;

import com.knoxhack.echocore.api.index.IndexRecipeSlot;
import com.knoxhack.echocore.api.index.IndexRecipeView;
import com.knoxhack.echocore.api.index.IndexMachineLayout;
import com.knoxhack.echocore.api.index.IndexMachineLayoutGauge;
import com.knoxhack.echocore.api.index.IndexMachineLayoutSlot;
import com.knoxhack.echocore.api.index.IndexMachineLayoutTemplates;
import com.knoxhack.echocore.api.index.IndexSlotRole;
import com.knoxhack.echoindex.Config;
import com.knoxhack.echoindex.network.IndexRecipeQueryPacket;
import com.knoxhack.echoindex.service.IndexIngredientNeed;
import com.knoxhack.echoindex.service.IndexRecipeActionState;
import com.knoxhack.echoindex.service.IndexRecipeDisplayMetadata;
import com.knoxhack.echoindex.service.IndexRecipeLayoutType;
import com.knoxhack.echoindex.service.IndexRecipePlan;
import com.knoxhack.echoindex.service.IndexRecipePlanner;
import com.knoxhack.echoindex.service.IndexRecipeQueryClientState;
import com.knoxhack.echoindex.service.IndexRecipeSnapshot;
import com.knoxhack.echoindex.service.IndexRecipeSourceKind;
import com.knoxhack.echoindex.service.IndexService;
import com.knoxhack.echonetcore.client.EchoNetClientActions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class IndexRecipeUi {
    public static int BG = IndexThemeStyle.FALLBACK_BG;
    public static int PANEL = IndexThemeStyle.FALLBACK_PANEL;
    public static int ROW = IndexThemeStyle.FALLBACK_ROW;
    public static int SLOT_BG = IndexThemeStyle.FALLBACK_SLOT_BG;
    public static int SLOT_BG_HOVER = IndexThemeStyle.FALLBACK_SLOT_BG_HOVER;
    public static int SLOT_OUTLINE = IndexThemeStyle.FALLBACK_SLOT_OUTLINE;
    public static int SLOT_OUTLINE_HOVER = IndexThemeStyle.FALLBACK_SLOT_OUTLINE_HOVER;
    public static int CYAN = IndexThemeStyle.FALLBACK_ACCENT;
    public static int TEXT = IndexThemeStyle.FALLBACK_TEXT;
    public static int MUTED = IndexThemeStyle.FALLBACK_MUTED;
    public static int WARN = IndexThemeStyle.FALLBACK_WARNING;
    public static int RED = IndexThemeStyle.FALLBACK_ERROR;
    public static int GREEN = IndexThemeStyle.FALLBACK_SUCCESS;
    private static int SECTION_BG = IndexThemeStyle.FALLBACK_SECTION_BG;
    private static int SECTION_BORDER = IndexThemeStyle.FALLBACK_SECTION_BORDER;
    private static final int DIAGRAM_SLOT = 28;
    private static final int DIAGRAM_OUTPUT_SLOT = 32;
    private static final int STANDARD_SLOT = 20;

    private static final Map<String, Integer> CHOICE_OFFSETS = new HashMap<>();
    private static Identifier lastHoveredRecipeId;
    private static Identifier lastHoveredItemId;
    private static Identifier lastQueriedItemId;
    private static ViewMode lastViewMode = ViewMode.RECIPES;
    private static int lastSelectedCardIndex;
    private static int lastSelectedCardCount;
    private static String lastQueryCacheState = "none";
    private static Identifier currentRecipeId;
    private static int currentChoiceCell;

    private IndexRecipeUi() {
    }

    public static void refreshTheme() {
        IndexThemeStyle.Palette palette = IndexThemeStyle.palette();
        BG = palette.background();
        PANEL = palette.panel();
        ROW = palette.row();
        SLOT_BG = palette.slotBg();
        SLOT_BG_HOVER = palette.slotBgHover();
        SLOT_OUTLINE = palette.slotOutline();
        SLOT_OUTLINE_HOVER = palette.slotOutlineHover();
        CYAN = palette.accent();
        TEXT = palette.text();
        MUTED = palette.muted();
        WARN = palette.warning();
        RED = palette.error();
        GREEN = palette.success();
        SECTION_BG = palette.sectionBg();
        SECTION_BORDER = palette.sectionBorder();
    }

    public static List<IndexRecipeView> viewsFor(Player player, Item item, ViewMode mode) {
        if (item == null) {
            return List.of();
        }
        recordQueryState(player, item, mode);
        if (clientContext(player)) {
            requestServerViews(item);
            return switch (mode) {
                case USES -> IndexRecipeQueryClientState.usesFor(item);
                case SOURCES -> IndexRecipeQueryClientState.sourcesFor(item);
                case RECIPES -> IndexRecipeQueryClientState.recipesFor(item);
            };
        }
        return switch (mode) {
            case USES -> IndexService.INSTANCE.usesFor(player, item);
            case SOURCES -> IndexService.INSTANCE.recipesFor(player, item).stream()
                    .filter(IndexRecipeUi::sourceCard)
                    .toList();
            case RECIPES -> IndexService.INSTANCE.recipesFor(player, item).stream()
                    .filter(recipe -> !sourceCard(recipe))
                    .toList();
        };
    }

    public static boolean sourceCard(IndexRecipeView recipe) {
        return IndexRecipeSourceKind.isSourceCard(recipe);
    }

    public static ItemStack recipeIcon(IndexRecipeView recipe, ItemStack fallback) {
        if (recipe == null) {
            return fallback == null ? ItemStack.EMPTY : fallback;
        }
        for (IndexRecipeSlot slot : recipe.slots()) {
            if (slot.role() == IndexSlotRole.OUTPUT && !slot.stacks().isEmpty() && !slot.stacks().getFirst().isEmpty()) {
                return slot.stacks().getFirst();
            }
        }
        return recipe.machine().isEmpty() ? fallback : recipe.machine();
    }

    public static List<ItemStack> linkedStacks(IndexRecipeView recipe) {
        List<ItemStack> stacks = new ArrayList<>();
        if (recipe == null) {
            return stacks;
        }
        for (IndexRecipeSlot slot : recipe.slots()) {
            for (ItemStack stack : slot.stacks()) {
                if (!stack.isEmpty()) {
                    stacks.add(stack);
                }
            }
        }
        if (!recipe.machine().isEmpty()) {
            stacks.add(recipe.machine());
        }
        return stacks;
    }

    public static String sourceKindLabel(IndexRecipeView recipe) {
        if (!sourceCard(recipe)) {
            return recipe == null ? "" : IndexAddonPresentation.displayName(recipe.sourceModId());
        }
        return IndexRecipeSourceKind.of(recipe).label();
    }

    private static String text(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    public static void drawRecipeCard(GuiGraphicsExtractor graphics, Font font, IndexRecipeView recipe,
            int x, int y, int w, int h, ItemStack fallback, int mouseX, int mouseY, List<SlotHit> slotHits) {
        refreshTheme();
        drawRecipeCardBackgroundNoRefresh(graphics, x, y, w, h, false);
        drawRecipeCardContentsNoRefresh(graphics, font, recipe, x, y, w, h, fallback, mouseX, mouseY, slotHits);
    }

    static void drawRecipeCardBackground(GuiGraphicsExtractor graphics, int x, int y, int w, int h,
            boolean clearBackdrop) {
        refreshTheme();
        drawRecipeCardBackgroundNoRefresh(graphics, x, y, w, h, clearBackdrop);
    }

    static void drawRecipeCardContents(GuiGraphicsExtractor graphics, Font font, IndexRecipeView recipe,
            int x, int y, int w, int h, ItemStack fallback, int mouseX, int mouseY, List<SlotHit> slotHits) {
        refreshTheme();
        drawRecipeCardContentsNoRefresh(graphics, font, recipe, x, y, w, h, fallback, mouseX, mouseY, slotHits);
    }

    private static void drawRecipeCardBackgroundNoRefresh(GuiGraphicsExtractor graphics, int x, int y, int w, int h,
            boolean clearBackdrop) {
        if (clearBackdrop) {
            graphics.fill(x, y, x + w, y + h, IndexThemeStyle.alpha(PANEL, 246));
        }
        IndexThemeStyle.card(graphics, x, y, w, h, false);
    }

    private static void drawRecipeCardContentsNoRefresh(GuiGraphicsExtractor graphics, Font font, IndexRecipeView recipe,
            int x, int y, int w, int h, ItemStack fallback, int mouseX, int mouseY, List<SlotHit> slotHits) {
        if (recipe == null) {
            graphics.text(font, text("screen.echoindex.recipe.none_selected"), x + 10, y + 12, MUTED, false);
            return;
        }
        beginRecipeChoiceScope(recipe);
        try {
            if (inside(mouseX, mouseY, x, y, w, h)) {
                lastHoveredRecipeId = recipe.id();
            }
            IndexRecipePlan plan = IndexRecipePlanner.plan(Minecraft.getInstance().player, recipe);
            IndexRecipeDisplayMetadata metadata = IndexRecipeQueryClientState.metadata(recipe.id()).orElse(null);
            CardMode mode = cardMode(w, h);
            if (metadata != null && metadata.vanillaLayout()) {
                drawVanillaRecipeCardContents(graphics, font, recipe, metadata, plan, x, y, w, h,
                        fallback, mouseX, mouseY, slotHits);
                return;
            }
            IndexMachineLayout machineLayout = metadata == null ? null : metadata.machineLayout();
            if ((machineLayout == null || machineLayout.empty()) && shouldUseGeneratedMachineLayout(recipe, mode, w, h)) {
                machineLayout = sourceCard(recipe)
                        ? IndexMachineLayoutTemplates.sourceStation(recipe, "generated_source_station")
                        : IndexMachineLayoutTemplates.process(recipe, "generated_process", false);
            }
            if (machineLayout != null && !machineLayout.empty() && mode != CardMode.COMPACT) {
                drawMachineLayoutRecipeCardContents(graphics, font, recipe, machineLayout, plan, x, y, w, h,
                        fallback, mouseX, mouseY, slotHits, mode);
                return;
            }
            IndexAddonPresentation.Style source = IndexAddonPresentation.style(recipe.sourceModId());
            int contentY = drawUnifiedHeader(graphics, font, recipeIcon(recipe, fallback), recipe.title(),
                    source, IndexAddonPresentation.categoryLabel(recipe.categoryId()),
                    sourceCard(recipe) ? sourceKindLabel(recipe) : text("screen.echoindex.recipe.kind.process"),
                    sourceCard(recipe) ? WARN : source.accent(),
                    plan, true, recipe.processTicks(), x, y, w, mode);
            contentY = drawStatPills(graphics, font, recipe, plan, x + 10, contentY + 2, w - 20,
                    maxStatPills(mode));

            int contentBottom = y + h - (Config.DEBUG_SHOW_RECIPE_IDS.get() ? 18 : 7);
            List<SlotLane> lanes = slotLanes(recipe);
            int totalSlots = recipe.slots().size();
            RenderedLane rendered = drawSlotLanes(graphics, font, lanes, x + 8,
                    contentY + (mode == CardMode.DIAGRAM ? 8 : 3), w - 16, contentBottom,
                    mouseX, mouseY, slotHits, plan, mode);
            int laneY = rendered.nextY();
            int renderedSlots = rendered.slots();
            if (renderedSlots < totalSlots && laneY + 10 <= contentBottom) {
                graphics.text(font, text("screen.echoindex.recipe.more_process_rows", totalSlots - renderedSlots),
                        x + 10, laneY, MUTED, false);
                laneY += 12;
            }
            laneY = drawNotes(graphics, font, recipe.notes(), x + 10, laneY + 2, w - 20,
                    contentBottom, maxNoteRows(mode));
            if (Config.DEBUG_SHOW_RECIPE_IDS.get()) {
                graphics.text(font, trim(font, recipe.id().toString(), w - 20), x + 10, y + h - 14, 0xFF6C7E84, false);
            }
        } finally {
            endRecipeChoiceScope();
        }
    }

    private static void drawVanillaRecipeCardContents(GuiGraphicsExtractor graphics, Font font, IndexRecipeView recipe,
            IndexRecipeDisplayMetadata metadata, IndexRecipePlan plan, int x, int y, int w, int h,
            ItemStack fallback, int mouseX, int mouseY, List<SlotHit> slotHits) {
        IndexRecipeDisplayMetadata effectiveMetadata = metadata.hasRenderableInputCells()
                ? metadata
                : metadata.withFallbackInputCellsFromSlots(recipe.slots());
        CardMode mode = cardMode(w, h);
        ItemStack icon = recipeIcon(recipe, fallback);
        IndexAddonPresentation.Style source = IndexAddonPresentation.style(recipe.sourceModId());
        int visualY = drawUnifiedHeader(graphics, font, icon, recipe.title(), source,
                layoutLabel(effectiveMetadata.type()), layoutBadge(effectiveMetadata.type()),
                badgeColor(effectiveMetadata.type()), plan, true, recipe.processTicks(), x, y, w, mode);
        int visualX = x + Math.max(10, (w - layoutVisualWidth(effectiveMetadata, mode)) / 2);
        int summaryY = switch (effectiveMetadata.type()) {
            case CRAFTING_SHAPED, CRAFTING_SHAPELESS -> drawCraftingLayout(graphics, font, effectiveMetadata,
                    visualX, visualY, mouseX, mouseY, slotHits, plan, mode);
            case COOKING -> drawCookingLayout(graphics, font, recipe, effectiveMetadata, visualX, visualY, mouseX, mouseY, slotHits, plan, mode);
            case STONECUTTING -> drawStonecuttingLayout(graphics, font, effectiveMetadata, visualX, visualY, mouseX, mouseY, slotHits, plan, mode);
            case SMITHING -> drawSmithingLayout(graphics, font, effectiveMetadata, visualX, visualY, mouseX, mouseY, slotHits, plan, mode);
            case GENERIC -> visualY;
        };

        int noteY = drawNeedSummary(graphics, font, plan, x + 10, summaryY + 6, w - 20, y + h - 30,
                maxNeedRows(mode));
        int maxNotesVanilla = maxNoteRows(mode);
        if (mode != CardMode.COMPACT) {
            for (String note : recipe.notes().stream().limit(maxNotesVanilla).toList()) {
                if (noteY > y + h - 22) {
                    break;
                }
                graphics.textWithWordWrap(font, Component.literal(note), x + 10, noteY, w - 20, MUTED);
                noteY += 16;
            }
        }
        if (Config.DEBUG_SHOW_RECIPE_IDS.get()) {
            graphics.text(font, trim(font, recipe.id().toString(), w - 20), x + 10, y + h - 14, 0xFF6C7E84, false);
        }
    }

    private static void drawMachineLayoutRecipeCardContents(GuiGraphicsExtractor graphics, Font font, IndexRecipeView recipe,
            IndexMachineLayout layout, IndexRecipePlan plan, int x, int y, int w, int h,
            ItemStack fallback, int mouseX, int mouseY, List<SlotHit> slotHits, CardMode mode) {
        IndexAddonPresentation.Style source = IndexAddonPresentation.style(recipe.sourceModId());
        String layoutLabel = layout.exact()
                ? text("screen.echoindex.recipe.layout.machine_exact")
                : text("screen.echoindex.recipe.layout.machine_representative");
        int contentY = drawUnifiedHeader(graphics, font, recipeIcon(recipe, fallback), recipe.title(),
                source, IndexAddonPresentation.categoryLabel(recipe.categoryId()), layoutLabel,
                layout.exact() ? CYAN : WARN, plan, true, recipe.processTicks(), x, y, w, mode);
        contentY = drawStatPills(graphics, font, recipe, plan, x + 10, contentY + 2, w - 20,
                maxStatPills(mode));
        int contentBottom = y + h - (Config.DEBUG_SHOW_RECIPE_IDS.get() ? 18 : 7);
        int reserved = mode == CardMode.DIAGRAM ? 88 : 58;
        int panelH = Math.max(72, Math.min(mode == CardMode.DIAGRAM ? 178 : 126,
                contentBottom - contentY - reserved));
        int nextY = drawMachineLayoutPanel(graphics, font, recipe, layout, x + 10, contentY + 4,
                w - 20, panelH, mouseX, mouseY, slotHits, plan);
        int noteY = drawNeedSummary(graphics, font, plan, x + 10, nextY + 6, w - 20,
                contentBottom, maxNeedRows(mode));
        noteY = drawNotes(graphics, font, recipe.notes(), x + 10, noteY + 2, w - 20,
                contentBottom, maxNoteRows(mode));
        if (Config.DEBUG_SHOW_RECIPE_IDS.get()) {
            graphics.text(font, trim(font, recipe.id().toString(), w - 20), x + 10, y + h - 14, 0xFF6C7E84, false);
        }
    }

    private static int drawMachineLayoutPanel(GuiGraphicsExtractor graphics, Font font, IndexRecipeView recipe,
            IndexMachineLayout layout, int x, int y, int width, int height, int mouseX, int mouseY,
            List<SlotHit> slotHits, IndexRecipePlan plan) {
        graphics.fill(x, y, x + width, y + height, 0x48102630);
        graphics.outline(x, y, width, height, layout.exact() ? 0x7766E8FF : 0x88FFD166);
        String title = layout.title().isBlank() ? layout.templateId() : layout.title();
        String badge = layout.exact()
                ? text("screen.echoindex.recipe.layout.machine_exact")
                : text("screen.echoindex.recipe.layout.machine_representative");
        int badgeW = width >= 220 ? Math.min(96, Math.max(52, font.width(badge) + 12)) : 0;
        int titleMax = badgeW > 0 ? width - badgeW - 24 : width - 16;
        graphics.text(font, trim(font, title, Math.max(32, titleMax)), x + 8, y + 6, CYAN, false);
        if (badgeW > 0) {
            drawTinyPill(graphics, font, x + width - badgeW - 8, y + 4, badge, layout.exact() ? CYAN : WARN, badgeW);
        }

        int viewX = x + 8;
        int viewY = y + 21;
        int viewW = width - 16;
        int viewH = height - 29;
        double scale = Math.min(viewW / (double) Math.max(1, layout.width()),
                viewH / (double) Math.max(1, layout.height()));
        int scaledW = Math.max(1, (int) Math.round(layout.width() * scale));
        int scaledH = Math.max(1, (int) Math.round(layout.height() * scale));
        int originX = viewX + Math.max(0, (viewW - scaledW) / 2);
        int originY = viewY + Math.max(0, (viewH - scaledH) / 2);
        graphics.fill(originX, originY, originX + scaledW, originY + scaledH, 0x4A101820);
        graphics.outline(originX, originY, scaledW, scaledH, 0x4438DFF4);

        for (IndexMachineLayoutGauge gauge : layout.gauges()) {
            drawMachineGauge(graphics, font, gauge, originX, originY, scale);
        }
        for (IndexMachineLayoutSlot slot : layout.slots()) {
            drawMachineLayoutSlot(graphics, font, recipe, slot, originX, originY, scale,
                    mouseX, mouseY, slotHits, plan);
        }
        return y + height + 2;
    }

    private static void drawMachineGauge(GuiGraphicsExtractor graphics, Font font, IndexMachineLayoutGauge gauge,
            int originX, int originY, double scale) {
        int x = originX + scaled(gauge.x(), scale);
        int y = originY + scaled(gauge.y(), scale);
        int w = Math.max(2, scaled(gauge.width(), scale));
        int h = Math.max(2, scaled(gauge.height(), scale));
        IndexThemeStyle.progressBar(graphics, x, y, w, h, Math.max(1, (int) (w * 0.65D)), gauge.color());
        String label = gaugeLabel(gauge);
        if (!label.isBlank() && h >= 5) {
            graphics.text(font, trim(font, label, Math.max(24, w + 20)), x, Math.max(0, y - 10), MUTED, false);
        }
    }

    private static void drawMachineLayoutSlot(GuiGraphicsExtractor graphics, Font font, IndexRecipeView recipe,
            IndexMachineLayoutSlot layoutSlot, int originX, int originY, double scale, int mouseX, int mouseY,
            List<SlotHit> slotHits, IndexRecipePlan plan) {
        int x = originX + scaled(layoutSlot.x(), scale);
        int y = originY + scaled(layoutSlot.y(), scale);
        int size = Math.max(14, scaled(layoutSlot.size(), scale));
        IndexRecipeSlot recipeSlot = recipeSlot(recipe, layoutSlot.recipeSlotIndex());
        IndexSlotRole role = recipeSlot == null ? layoutSlot.role() : recipeSlot.role();
        String label = !layoutSlot.label().isBlank()
                ? layoutSlot.label()
                : recipeSlot == null ? roleLabel(role) : slotLabel(recipeSlot);
        List<ItemStack> choices = machineLayoutChoices(recipe, recipeSlot, role);
        if (size >= 20 && scale >= 0.72D && y - 10 >= originY) {
            graphics.text(font, trim(font, label, Math.max(54, size + 42)), x, y - 10, roleColor(role), false);
        }
        drawRecipeCell(graphics, font, choices, role, plan, x, y, size, mouseX, mouseY, slotHits);
        if (choices.isEmpty() && !label.isBlank()) {
            boolean hover = inside(mouseX, mouseY, x, y, size, size);
            graphics.fill(x, y, x + size, y + size, hover ? SLOT_BG_HOVER : SLOT_BG);
            graphics.outline(x, y, size, size, hover ? SLOT_OUTLINE_HOVER : roleColor(role));
            graphics.text(font, trim(font, compactSlotText(label), size - 4), x + 2, y + Math.max(3, size / 2 - 4),
                    roleColor(role), false);
        }
    }

    private static IndexRecipeSlot recipeSlot(IndexRecipeView recipe, int slotIndex) {
        if (recipe == null || slotIndex < 0 || slotIndex >= recipe.slots().size()) {
            return null;
        }
        return recipe.slots().get(slotIndex);
    }

    private static List<ItemStack> machineLayoutChoices(IndexRecipeView recipe, IndexRecipeSlot slot,
            IndexSlotRole role) {
        if (slot != null && !slot.stacks().isEmpty()) {
            return slot.stacks();
        }
        if (role == IndexSlotRole.MACHINE && recipe != null && !recipe.machine().isEmpty()) {
            return List.of(recipe.machine());
        }
        return List.of();
    }

    private static String gaugeLabel(IndexMachineLayoutGauge gauge) {
        String kind = gauge.kind().toLowerCase(Locale.ROOT);
        return switch (kind) {
            case "energy", "power", "charge" -> text("screen.echoindex.recipe.label.energy");
            case "fluid", "tank", "water" -> text("screen.echoindex.recipe.label.fluid_short");
            case "heat" -> text("screen.echoindex.recipe.stat.heat");
            case "source" -> text("screen.echoindex.recipe.stat.source");
            default -> gauge.label().isBlank() ? text("screen.echoindex.recipe.label.progress") : gauge.label();
        };
    }

    private static int scaled(int value, double scale) {
        return (int) Math.round(value * scale);
    }

    private static boolean shouldUseGeneratedMachineLayout(IndexRecipeView recipe, CardMode mode, int width, int height) {
        return recipe != null
                && mode != CardMode.COMPACT
                && width >= 290
                && height >= 220
                && (IndexRecipeSnapshot.hasRole(recipe, IndexSlotRole.MACHINE)
                || IndexRecipeSnapshot.hasRole(recipe, IndexSlotRole.CATALYST)
                || recipe.processTicks() > 0
                || sourceCard(recipe));
    }

    private static int drawUnifiedHeader(GuiGraphicsExtractor graphics, Font font, ItemStack icon, String title,
            IndexAddonPresentation.Style source, String categoryLabel, String kindLabel, int kindColor,
            IndexRecipePlan plan, boolean allowTransfer, int processTicks, int x, int y, int w, CardMode mode) {
        int headerH = mode == CardMode.COMPACT ? 42 : mode == CardMode.DIAGRAM ? 58 : 52;
        int accent = source.accent();
        graphics.fill(x, y, x + w, y + 2, IndexThemeStyle.alpha(accent, 180));
        graphics.fill(x + 1, y + 2, x + w - 1, y + headerH, IndexThemeStyle.alpha(SECTION_BG, 184));
        graphics.outline(x, y, w, headerH, IndexThemeStyle.alpha(SECTION_BORDER, 112));
        graphics.item(icon, x + 10, y + 9);

        int statusW = Math.min(statusLaneWidth(plan, mode), Math.max(62, w / 3));
        int titleW = Math.max(42, w - statusW - 52);
        graphics.text(font, trim(font, title, titleW), x + 34, y + 8, CYAN, true);

        int pillX = x + 34;
        pillX += drawTinyPill(graphics, font, pillX, y + 22, source.shortLabel(), accent, 50) + 4;
        pillX += drawTinyPill(graphics, font, pillX, y + 22, categoryLabel, 0x8838DFF4,
                Math.max(54, titleW - 42)) + 4;
        if (mode != CardMode.COMPACT && headerH >= 52 && pillX < x + w - statusW - 30) {
            drawTinyPill(graphics, font, pillX, y + 35, kindLabel, kindColor, Math.max(44, titleW));
        }

        int statusX = x + Math.max(44, w - statusW - 6);
        drawStatusPill(graphics, font, statusX, y + 8, statusW, statusLabel(plan, allowTransfer),
                statusColor(plan, allowTransfer));
        String planDetail = statusDetail(plan, allowTransfer);
        String detail = machineRequired(plan) && !planDetail.isBlank()
                ? planDetail
                : processTicks > 0 ? text("screen.echoindex.recipe.ticks", processTicks) : planDetail;
        if (!detail.isBlank() && mode != CardMode.COMPACT) {
            graphics.text(font, trim(font, detail, statusW), statusX, y + 28, MUTED, false);
        }
        return y + headerH + 3;
    }

    private static int drawStatPills(GuiGraphicsExtractor graphics, Font font, IndexRecipeView recipe,
            IndexRecipePlan plan, int x, int y, int width, int maxPills) {
        List<String> stats = statPills(recipe, plan, maxPills);
        if (stats.isEmpty()) {
            return y;
        }
        int cx = x;
        int cy = y;
        for (String stat : stats) {
            int pillW = Math.min(108, Math.max(32, font.width(stat) + 10));
            if (cx + pillW > x + width) {
                cy += 15;
                cx = x;
            }
            if (cy > y + 16) {
                break;
            }
            drawTinyPill(graphics, font, cx, cy, stat, 0x8845CFEA, pillW);
            cx += pillW + 4;
        }
        return cy + 16;
    }

    private static List<String> statPills(IndexRecipeView recipe, IndexRecipePlan plan, int maxPills) {
        List<String> stats = new ArrayList<>();
        if (recipe.processTicks() > 0) {
            stats.add(text("screen.echoindex.recipe.ticks", recipe.processTicks()));
        }
        if (plan != null && plan.missingCount() > 0) {
            stats.add(text("screen.echoindex.recipe.status.missing", plan.missingCount()));
        }
        for (String note : recipe.notes()) {
            String stat = compactStat(note);
            if (!stat.isBlank() && !stats.contains(stat)) {
                stats.add(stat);
            }
            if (stats.size() >= maxPills) {
                break;
            }
        }
        return stats.stream().limit(maxPills).toList();
    }

    private static String compactStat(String note) {
        if (note == null || note.isBlank()) {
            return "";
        }
        String lower = note.toLowerCase(Locale.ROOT);
        String number = firstNumber(note);
        if (lower.contains("thermal flux") || lower.contains(" flux")) {
            return number.isBlank() ? text("screen.echoindex.recipe.stat.flux")
                    : text("screen.echoindex.recipe.stat.flux_amount", number);
        }
        if (lower.contains("power") || lower.contains("energy")) {
            return number.isBlank() ? text("screen.echoindex.recipe.stat.power")
                    : text("screen.echoindex.recipe.stat.power_amount", number);
        }
        if (lower.contains("chance") || lower.contains("byproduct")) {
            return number.isBlank() ? text("screen.echoindex.recipe.stat.byproduct")
                    : text("screen.echoindex.recipe.stat.byproduct_amount", number);
        }
        if (lower.contains("heat")) {
            return number.isBlank() ? text("screen.echoindex.recipe.stat.heat")
                    : text("screen.echoindex.recipe.stat.heat_amount", number);
        }
        if (lower.contains("route")) {
            return text("screen.echoindex.recipe.stat.route");
        }
        if (lower.contains("mission")) {
            return text("screen.echoindex.recipe.stat.mission");
        }
        if (lower.contains("source")) {
            return text("screen.echoindex.recipe.stat.source");
        }
        return "";
    }

    private static String firstNumber(String value) {
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isDigit(c) || (c == '.' && !digits.isEmpty())) {
                digits.append(c);
            } else if (!digits.isEmpty()) {
                break;
            }
        }
        return digits.toString();
    }

    private static List<SlotLane> slotLanes(IndexRecipeView recipe) {
        List<IndexRecipeSlot> inputs = new ArrayList<>();
        List<IndexRecipeSlot> catalysts = new ArrayList<>();
        List<IndexRecipeSlot> machines = new ArrayList<>();
        List<IndexRecipeSlot> outputs = new ArrayList<>();
        List<IndexRecipeSlot> byproducts = new ArrayList<>();
        List<IndexRecipeSlot> notes = new ArrayList<>();
        for (IndexRecipeSlot slot : recipe.slots()) {
            switch (slot.role()) {
                case INPUT -> inputs.add(slot);
                case CATALYST -> catalysts.add(slot);
                case MACHINE -> machines.add(slot);
                case OUTPUT -> {
                    if (slotLabel(slot).toLowerCase(Locale.ROOT).contains("byproduct")) {
                        byproducts.add(slot);
                    } else {
                        outputs.add(slot);
                    }
                }
                default -> notes.add(slot);
            }
        }
        List<SlotLane> lanes = new ArrayList<>();
        addLane(lanes, text("screen.echoindex.recipe.section.inputs"), CYAN, inputs);
        addLane(lanes, text("screen.echoindex.recipe.section.catalysts"), 0xFFE09CFF, catalysts);
        addLane(lanes, text("screen.echoindex.recipe.section.machine"), WARN, machines);
        addLane(lanes, text("screen.echoindex.recipe.section.outputs"), GREEN, outputs);
        addLane(lanes, text("screen.echoindex.recipe.section.byproducts"), 0xFFFFB86B, byproducts);
        addLane(lanes, text("screen.echoindex.recipe.section.notes"), MUTED, notes);
        return lanes;
    }

    private static void addLane(List<SlotLane> lanes, String title, int color, List<IndexRecipeSlot> slots) {
        if (!slots.isEmpty()) {
            lanes.add(new SlotLane(title, color, slots));
        }
    }

    private static RenderedLane drawSlotLanes(GuiGraphicsExtractor graphics, Font font, List<SlotLane> lanes,
            int x, int y, int width, int bottom, int mouseX, int mouseY, List<SlotHit> slotHits,
            IndexRecipePlan plan, CardMode mode) {
        if (lanes.isEmpty()) {
            return new RenderedLane(y, 0);
        }
        if (mode != CardMode.DIAGRAM || width < 360) {
            int laneY = y;
            int renderedSlots = 0;
            for (SlotLane lane : lanes) {
                if (laneY + 24 > bottom) {
                    break;
                }
                RenderedLane rendered = drawSlotLane(graphics, font, lane, x, laneY, width,
                        bottom, mouseX, mouseY, slotHits, plan);
                if (rendered.slots() == 0) {
                    break;
                }
                renderedSlots += rendered.slots();
                laneY = rendered.nextY();
            }
            return new RenderedLane(laneY, renderedSlots);
        }
        int gap = 8;
        int columnW = (width - gap) / 2;
        int laneY = y;
        int renderedSlots = 0;
        for (int i = 0; i < lanes.size(); i += 2) {
            if (laneY + 38 > bottom) {
                break;
            }
            SlotLane left = lanes.get(i);
            SlotLane right = i + 1 < lanes.size() ? lanes.get(i + 1) : null;
            int maxRows = Math.max(1, Math.min(5, (bottom - laneY - 22) / 26));
            RenderedLane leftRendered = drawSlotSection(graphics, font, left, x, laneY, columnW,
                    maxRows, mouseX, mouseY, slotHits, plan);
            RenderedLane rightRendered = right == null
                    ? new RenderedLane(laneY, 0)
                    : drawSlotSection(graphics, font, right, x + columnW + gap, laneY, width - columnW - gap,
                            maxRows, mouseX, mouseY, slotHits, plan);
            if (leftRendered.slots() == 0 && rightRendered.slots() == 0) {
                break;
            }
            renderedSlots += leftRendered.slots() + rightRendered.slots();
            laneY = Math.max(leftRendered.nextY(), rightRendered.nextY()) + 6;
        }
        return new RenderedLane(laneY, renderedSlots);
    }

    private static RenderedLane drawSlotLane(GuiGraphicsExtractor graphics, Font font, SlotLane lane,
            int x, int y, int width, int bottom, int mouseX, int mouseY, List<SlotHit> slotHits,
            IndexRecipePlan plan) {
        int available = bottom - y;
        if (available < 38) {
            return new RenderedLane(y, 0);
        }
        int maxSlots = Math.max(1, (available - 17) / 24);
        int visibleSlots = Math.min(lane.slots().size(), maxSlots);
        int laneH = 17 + visibleSlots * 24 + 3;
        graphics.fill(x, y, x + width, y + laneH, SECTION_BG);
        graphics.outline(x, y, width, laneH, lane.color() & 0xAAFFFFFF);
        graphics.text(font, lane.title(), x + 6, y + 5, lane.color(), false);
        int rowY = y + 16;
        for (IndexRecipeSlot slot : lane.slots().stream().limit(visibleSlots).toList()) {
            drawSlotGroup(graphics, font, slot, x + 6, rowY, width - 12, mouseX, mouseY, slotHits, plan);
            rowY += 24;
        }
        return new RenderedLane(y + laneH + 4, visibleSlots);
    }

    private static RenderedLane drawSlotSection(GuiGraphicsExtractor graphics, Font font, SlotLane lane,
            int x, int y, int width, int maxRows, int mouseX, int mouseY, List<SlotHit> slotHits,
            IndexRecipePlan plan) {
        int visibleSlots = Math.min(lane.slots().size(), Math.max(1, maxRows));
        int more = Math.max(0, lane.slots().size() - visibleSlots);
        int laneH = 22 + visibleSlots * 26 + (more > 0 ? 12 : 4);
        graphics.fill(x, y, x + width, y + laneH, SECTION_BG);
        graphics.outline(x, y, width, laneH, lane.color() & 0xAAFFFFFF);
        graphics.text(font, lane.title(), x + 7, y + 6, lane.color(), false);
        int rowY = y + 21;
        for (IndexRecipeSlot slot : lane.slots().stream().limit(visibleSlots).toList()) {
            drawSlotGroup(graphics, font, slot, x + 7, rowY, width - 14, mouseX, mouseY, slotHits, plan);
            rowY += 26;
        }
        if (more > 0) {
            graphics.text(font, text("screen.echoindex.recipe.more", more), x + 7, rowY - 1, MUTED, false);
        }
        return new RenderedLane(y + laneH, visibleSlots);
    }

    private static int drawNotes(GuiGraphicsExtractor graphics, Font font, List<String> notes,
            int x, int y, int width, int bottom, int maxRows) {
        if (notes.isEmpty() || y + 22 > bottom || maxRows <= 0) {
            return y;
        }
        graphics.text(font, text("screen.echoindex.recipe.section.notes"), x, y + 4, MUTED, false);
        y += 14;
        int rendered = 0;
        for (String note : notes) {
            if (y + 18 > bottom || rendered >= maxRows) {
                break;
            }
            graphics.fill(x, y, x + width, y + 16, 0x33102630);
            graphics.outline(x, y, width, 16, 0x3345CFEA);
            graphics.text(font, trim(font, note, width - 8), x + 4, y + 5, MUTED, false);
            y += 18;
            rendered++;
        }
        if (notes.size() > rendered && y + 10 <= bottom) {
            graphics.text(font, text("screen.echoindex.recipe.more_notes", notes.size() - rendered), x + 2, y, MUTED, false);
            y += 12;
        }
        return y;
    }

    private static void drawStatusPill(GuiGraphicsExtractor graphics, Font font, int x, int y, int width,
            String label, int color) {
        graphics.fill(x, y, x + width, y + 14, 0x42102630);
        graphics.outline(x, y, width, 14, IndexThemeStyle.alpha(color, 185));
        graphics.centeredText(font, trim(font, label, width - 6), x + width / 2, y + 4, color);
    }

    private static int drawTinyPill(GuiGraphicsExtractor graphics, Font font, int x, int y, String label,
            int color, int maxWidth) {
        int width = Math.min(Math.max(26, maxWidth), Math.max(26, font.width(label) + 10));
        graphics.fill(x, y, x + width, y + 13, 0x32102630);
        graphics.outline(x, y, width, 13, IndexThemeStyle.alpha(color, 150));
        graphics.centeredText(font, trim(font, label, width - 6), x + width / 2, y + 4, color);
        return width;
    }

    private static int drawCraftingLayout(GuiGraphicsExtractor graphics, Font font, IndexRecipeDisplayMetadata metadata,
            int x, int y, int mouseX, int mouseY, List<SlotHit> slotHits, IndexRecipePlan plan, CardMode mode) {
        int cell = mode == CardMode.DIAGRAM ? DIAGRAM_SLOT : STANDARD_SLOT;
        int gap = mode == CardMode.DIAGRAM ? 3 : 0;
        int grid = 3;
        int gridSize = grid * cell + (grid - 1) * gap;
        graphics.fill(x - 5, y - 5, x + gridSize + 5, y + gridSize + 5, 0x33102630);
        graphics.outline(x - 5, y - 5, gridSize + 10, gridSize + 10, 0x7738DFF4);
        for (int row = 0; row < grid; row++) {
            for (int col = 0; col < grid; col++) {
                List<ItemStack> choices = recipeCell(metadata, col, row);
                drawRecipeCell(graphics, font, choices, IndexSlotRole.INPUT, plan,
                        x + col * (cell + gap), y + row * (cell + gap), cell, mouseX, mouseY, slotHits);
            }
        }
        int arrowX = x + gridSize + (mode == CardMode.DIAGRAM ? 22 : 12);
        int arrowY = y + gridSize / 2 - 3;
        graphics.text(font, ">", arrowX, arrowY, CYAN, false);
        int outputSize = mode == CardMode.DIAGRAM ? DIAGRAM_OUTPUT_SLOT : STANDARD_SLOT;
        int outputX = arrowX + (mode == CardMode.DIAGRAM ? 30 : 34);
        int outputY = y + gridSize / 2 - outputSize / 2;
        drawRecipeCell(graphics, font, List.of(output(metadata)), IndexSlotRole.OUTPUT, plan,
                outputX, outputY, outputSize, mouseX, mouseY, slotHits);
        if (metadata.type() == IndexRecipeLayoutType.CRAFTING_SHAPELESS) {
            drawTinyPill(graphics, font, x, y + gridSize + 9,
                    text("screen.echoindex.recipe.layout.shapeless_badge"), CYAN, 86);
        }
        if (!metadata.machine().isEmpty()) {
            int machineY = y + gridSize + (mode == CardMode.DIAGRAM ? 12 : 8);
            drawRecipeCell(graphics, font, List.of(metadata.machine()), IndexSlotRole.MACHINE, plan,
                    outputX, machineY, STANDARD_SLOT, mouseX, mouseY, slotHits);
            graphics.text(font, trim(font, metadata.machine().getHoverName().getString(), 110),
                    outputX + 24, machineY + 6, MUTED, false);
        }
        return y + gridSize + (metadata.machine().isEmpty() ? 18 : 40);
    }

    private static int drawCookingLayout(GuiGraphicsExtractor graphics, Font font, IndexRecipeView recipe,
            IndexRecipeDisplayMetadata metadata, int x, int y, int mouseX, int mouseY, List<SlotHit> slotHits,
            IndexRecipePlan plan, CardMode mode) {
        List<ItemStack> input = metadata.cells().isEmpty() ? List.of() : metadata.cells().getFirst();
        List<ItemStack> fuel = firstSlotChoices(recipe, IndexSlotRole.CATALYST);
        int cell = mode == CardMode.DIAGRAM ? DIAGRAM_SLOT : STANDARD_SLOT;
        drawDiagramCell(graphics, font, text("screen.echoindex.recipe.label.input"), input, IndexSlotRole.INPUT,
                plan, x + 4, y + 6, cell, mouseX, mouseY, slotHits);
        drawDiagramCell(graphics, font, text("screen.echoindex.recipe.label.fuel"), fuel, IndexSlotRole.CATALYST,
                plan, x + 4, y + 40, cell, mouseX, mouseY, slotHits);
        if (!metadata.machine().isEmpty()) {
            drawDiagramCell(graphics, font, text("screen.echoindex.recipe.label.machine"), List.of(metadata.machine()),
                    IndexSlotRole.MACHINE, plan, x + (mode == CardMode.DIAGRAM ? 78 : 58), y + 25,
                    cell, mouseX, mouseY, slotHits);
        }
        int arrowX = x + (mode == CardMode.DIAGRAM ? 130 : 94);
        graphics.text(font, ">", arrowX, y + 35, CYAN, false);
        drawDiagramCell(graphics, font, text("screen.echoindex.recipe.label.output"), List.of(output(metadata)),
                IndexSlotRole.OUTPUT, plan, arrowX + 32, y + 25, cell, mouseX, mouseY, slotHits);
        return y + (mode == CardMode.DIAGRAM ? 78 : 62);
    }

    private static int drawStonecuttingLayout(GuiGraphicsExtractor graphics, Font font, IndexRecipeDisplayMetadata metadata,
            int x, int y, int mouseX, int mouseY, List<SlotHit> slotHits, IndexRecipePlan plan, CardMode mode) {
        List<ItemStack> input = metadata.cells().isEmpty() ? List.of() : metadata.cells().getFirst();
        int cell = mode == CardMode.DIAGRAM ? DIAGRAM_SLOT : STANDARD_SLOT;
        drawDiagramCell(graphics, font, text("screen.echoindex.recipe.label.input"), input, IndexSlotRole.INPUT,
                plan, x + 4, y + 20, cell, mouseX, mouseY, slotHits);
        if (!metadata.machine().isEmpty()) {
            drawDiagramCell(graphics, font, text("screen.echoindex.recipe.label.machine"), List.of(metadata.machine()),
                    IndexSlotRole.MACHINE, plan, x + (mode == CardMode.DIAGRAM ? 74 : 58), y + 20,
                    cell, mouseX, mouseY, slotHits);
        }
        int arrowX = x + (mode == CardMode.DIAGRAM ? 130 : 94);
        graphics.text(font, ">", arrowX, y + 29, CYAN, false);
        drawDiagramCell(graphics, font, text("screen.echoindex.recipe.label.output"), List.of(output(metadata)),
                IndexSlotRole.OUTPUT, plan, arrowX + 32, y + 20, cell, mouseX, mouseY, slotHits);
        return y + (mode == CardMode.DIAGRAM ? 70 : 56);
    }

    private static int drawSmithingLayout(GuiGraphicsExtractor graphics, Font font, IndexRecipeDisplayMetadata metadata,
            int x, int y, int mouseX, int mouseY, List<SlotHit> slotHits, IndexRecipePlan plan, CardMode mode) {
        int cell = mode == CardMode.DIAGRAM ? DIAGRAM_SLOT : STANDARD_SLOT;
        int step = mode == CardMode.DIAGRAM ? 34 : 24;
        for (int i = 0; i < 3; i++) {
            List<ItemStack> choices = i < metadata.cells().size() ? metadata.cells().get(i) : List.of();
            drawDiagramCell(graphics, font, text("screen.echoindex.recipe.label.input"), choices, IndexSlotRole.INPUT, plan,
                    x + i * step, y + 20, cell, mouseX, mouseY, slotHits);
        }
        if (!metadata.machine().isEmpty()) {
            drawDiagramCell(graphics, font, text("screen.echoindex.recipe.label.machine"), List.of(metadata.machine()),
                    IndexSlotRole.MACHINE, plan, x + 3 * step + 12, y + 20, cell, mouseX, mouseY, slotHits);
        }
        int arrowX = x + (mode == CardMode.DIAGRAM ? 150 : 118);
        graphics.text(font, ">", arrowX, y + 29, CYAN, false);
        drawDiagramCell(graphics, font, text("screen.echoindex.recipe.label.output"), List.of(output(metadata)),
                IndexSlotRole.OUTPUT, plan, arrowX + 32, y + 20, cell, mouseX, mouseY, slotHits);
        return y + (mode == CardMode.DIAGRAM ? 72 : 56);
    }

    private static void drawDiagramCell(GuiGraphicsExtractor graphics, Font font, String label, List<ItemStack> choices,
            IndexSlotRole role, IndexRecipePlan plan, int x, int y, int size, int mouseX, int mouseY,
            List<SlotHit> slotHits) {
        graphics.text(font, trim(font, label, Math.max(size + 34, 72)), x, y - 11, roleColor(role), false);
        drawRecipeCell(graphics, font, choices, role, plan, x, y, size, mouseX, mouseY, slotHits);
    }

    private static int roleColor(IndexSlotRole role) {
        return switch (role) {
            case OUTPUT -> GREEN;
            case MACHINE -> WARN;
            case CATALYST -> 0xFFE09CFF;
            case INPUT -> CYAN;
            default -> MUTED;
        };
    }

    private static void drawRecipeCell(GuiGraphicsExtractor graphics, Font font, List<ItemStack> choices,
            IndexSlotRole role, IndexRecipePlan plan, int x, int y, int size, int mouseX, int mouseY,
            List<SlotHit> slotHits) {
        boolean hover = inside(mouseX, mouseY, x, y, size, size);
        IndexIngredientNeed need = needForChoices(plan, role, choices);
        int outline = cellOutline(role, need, choices, hover);
        graphics.fill(x, y, x + size, y + size, hover ? SLOT_BG_HOVER : SLOT_BG);
        graphics.outline(x, y, size, size, outline);
        String choiceKey = choiceKey(role, x, y, choices);
        int choiceCount = visibleChoiceCount(choices);
        ItemStack stack = visibleChoice(choices, choiceKey);
        if (!stack.isEmpty()) {
            int inset = Math.max(2, (size - 16) / 2);
            if (hover) {
                lastHoveredItemId = IndexService.itemId(stack.getItem());
            }
            graphics.item(stack, x + inset, y + inset);
            int required = need == null ? Math.max(1, stack.getCount()) : need.required();
            graphics.itemDecorations(font, stack, x + inset, y + inset);
            if (required > 1) {
                String badge = Integer.toString(required);
                graphics.text(font, badge, x + size - font.width(badge) - 1, y + size - 8, TEXT, true);
            }
            if (choices != null && choices.size() > 1) {
                String badge = "+" + (choices.size() - 1);
                graphics.text(font, badge, x + size - font.width(badge) - 1, y + 1, CYAN, true);
            }
            if (hover) {
                IndexTooltipUtil.showItemTooltip(graphics, font, stack, cellTooltip(stack, choices, role, need),
                        x + size / 2, y + size / 2);
            }
            if (slotHits != null) {
                slotHits.add(new SlotHit(x, y, size, size, stack.copy(), role, choiceKey, choiceCount));
            }
        }
    }

    private static int drawNeedSummary(GuiGraphicsExtractor graphics, Font font, IndexRecipePlan plan,
            int x, int y, int width, int bottom, int maxRows) {
        List<NeedRow> rows = aggregateNeeds(plan);
        if (rows.isEmpty() || y > bottom) {
            return y;
        }
        graphics.text(font, text("screen.echoindex.recipe.section.ingredients"), x, y, MUTED, false);
        y += 13;
        int rendered = 0;
        for (NeedRow row : rows) {
            if (y + 18 > bottom || rendered >= maxRows) {
                break;
            }
            int color = row.missing() == 0 ? GREEN : WARN;
            graphics.item(row.stack(), x, y);
            String line = row.stack().getHoverName().getString() + " "
                    + Math.max(0, row.required() - row.missing()) + "/" + row.required();
            graphics.text(font, trim(font, line, width - 24), x + 22, y + 5, color, false);
            y += 19;
            rendered++;
        }
        if (rows.size() > rendered && y + 10 <= bottom) {
            graphics.text(font, text("screen.echoindex.recipe.more", rows.size() - rendered), x + 2, y, MUTED, false);
            y += 12;
        }
        return y + 2;
    }

    private static List<NeedRow> aggregateNeeds(IndexRecipePlan plan) {
        if (plan == null || plan.needs().isEmpty()) {
            return List.of();
        }
        Map<String, NeedAccumulator> grouped = new LinkedHashMap<>();
        for (IndexIngredientNeed need : plan.needs()) {
            if (need.selected().isEmpty() || need.role() == IndexSlotRole.OUTPUT || need.role() == IndexSlotRole.MACHINE) {
                continue;
            }
            String key = need.role().name() + ":" + IndexService.itemId(need.selected().getItem());
            grouped.computeIfAbsent(key, ignored -> new NeedAccumulator(need.selected()))
                    .add(need.required(), need.missing());
        }
        return grouped.values().stream()
                .map(NeedAccumulator::row)
                .toList();
    }

    private static List<ItemStack> recipeCell(IndexRecipeDisplayMetadata metadata, int col, int row) {
        int width = Math.max(1, metadata.width());
        int height = Math.max(1, metadata.height());
        if (col >= width || row >= height) {
            return List.of();
        }
        int index = row * width + col;
        return index >= 0 && index < metadata.cells().size() ? metadata.cells().get(index) : List.of();
    }

    private static List<ItemStack> firstSlotChoices(IndexRecipeView recipe, IndexSlotRole role) {
        for (IndexRecipeSlot slot : recipe.slots()) {
            if (slot.role() == role) {
                return slot.stacks();
            }
        }
        return List.of();
    }

    private static ItemStack output(IndexRecipeDisplayMetadata metadata) {
        return metadata.output().isEmpty() ? ItemStack.EMPTY : metadata.output();
    }

    private static ItemStack firstStack(List<ItemStack> stacks) {
        if (stacks == null) {
            return ItemStack.EMPTY;
        }
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack visibleChoice(List<ItemStack> choices, String choiceKey) {
        if (choices == null || choices.isEmpty()) {
            return ItemStack.EMPTY;
        }
        List<ItemStack> visible = choices.stream()
                .filter(stack -> stack != null && !stack.isEmpty())
                .toList();
        if (visible.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (visible.size() == 1) {
            return visible.getFirst();
        }
        Integer manualOffset = CHOICE_OFFSETS.get(choiceKey);
        if (manualOffset != null) {
            return visible.get(Math.floorMod(manualOffset, visible.size()));
        }
        long frame = System.currentTimeMillis() / 1200L;
        return visible.get((int) Math.floorMod(frame, visible.size()));
    }

    private static int visibleChoiceCount(List<ItemStack> choices) {
        if (choices == null || choices.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (ItemStack stack : choices) {
            if (stack != null && !stack.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static String choiceKey(IndexSlotRole role, int x, int y, List<ItemStack> choices) {
        StringBuilder builder = new StringBuilder(role.name());
        if (currentRecipeId != null) {
            builder.append(':').append(currentRecipeId).append(':').append(currentChoiceCell++);
        } else {
            builder.append(':').append(x).append(':').append(y);
        }
        if (choices != null) {
            int added = 0;
            for (ItemStack stack : choices) {
                if (stack != null && !stack.isEmpty()) {
                    builder.append(':').append(IndexService.itemId(stack.getItem()));
                    added++;
                    if (added >= 8) {
                        break;
                    }
                }
            }
        }
        return builder.toString();
    }

    private static void beginRecipeChoiceScope(IndexRecipeView recipe) {
        currentRecipeId = recipe == null ? null : recipe.id();
        currentChoiceCell = 0;
    }

    private static void endRecipeChoiceScope() {
        currentRecipeId = null;
        currentChoiceCell = 0;
    }

    private static IndexIngredientNeed needForChoices(IndexRecipePlan plan, IndexSlotRole role, List<ItemStack> choices) {
        if (plan == null || role == null || choices == null || choices.isEmpty()) {
            return null;
        }
        for (IndexIngredientNeed need : plan.needs()) {
            if (need.role() != role || need.selected().isEmpty()) {
                continue;
            }
            for (ItemStack choice : choices) {
                if (choice != null && !choice.isEmpty() && choice.is(need.selected().getItem())) {
                    return need;
                }
            }
        }
        return null;
    }

    private static int cellOutline(IndexSlotRole role, IndexIngredientNeed need, List<ItemStack> choices, boolean hover) {
        if (hover) {
            return CYAN;
        }
        if (need != null) {
            return need.satisfied() ? GREEN : RED;
        }
        if (choices == null || choices.isEmpty() || firstStack(choices).isEmpty()) {
            return 0x33445A63;
        }
        return switch (role) {
            case OUTPUT -> CYAN;
            case MACHINE -> WARN;
            case CATALYST -> 0xFFE09CFF;
            case INPUT -> CYAN;
            default -> MUTED;
        };
    }

    private static List<Component> cellTooltip(ItemStack stack, List<ItemStack> choices,
            IndexSlotRole role, IndexIngredientNeed need) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(stack.getHoverName());
        tooltip.add(Component.literal(roleLabel(role)));
        if (Config.DEBUG_SHOW_RECIPE_IDS.get()) {
            tooltip.add(Component.literal(IndexService.itemId(stack.getItem()).toString()));
        }
        if (need != null) {
            int available = availableCount(stack.getItem());
            tooltip.add(Component.literal(text("screen.echoindex.recipe.tooltip.available", available)));
            tooltip.add(Component.literal(text("screen.echoindex.recipe.tooltip.required", need.required())));
            tooltip.add(Component.literal(text("screen.echoindex.recipe.tooltip.missing",
                    Math.max(0, need.required() - available))));
        }
        if (choices != null && choices.size() > 1) {
            tooltip.add(Component.literal(text("screen.echoindex.recipe.tooltip.choices")));
            int shown = 0;
            for (ItemStack choice : choices) {
                if (choice == null || choice.isEmpty()) {
                    continue;
                }
                tooltip.add(Component.literal(text("screen.echoindex.recipe.tooltip.choice",
                        choice.getHoverName().getString())));
                shown++;
                if (shown >= 8) {
                    break;
                }
            }
            int remaining = choices.size() - shown;
            if (remaining > 0) {
                tooltip.add(Component.literal(text("screen.echoindex.recipe.more", remaining)));
            }
        }
        IndexTooltipUtil.appendModName(tooltip, stack);
        return tooltip;
    }

    public static void cycleChoice(SlotHit hit, int direction) {
        if (hit == null || !hit.choiceCyclable()) {
            return;
        }
        int current = CHOICE_OFFSETS.getOrDefault(hit.choiceKey(), 0);
        CHOICE_OFFSETS.put(hit.choiceKey(), current + (direction == 0 ? 1 : direction));
    }

    public static void recordCardSelection(ViewMode mode, int selected, int count) {
        lastViewMode = mode == null ? ViewMode.RECIPES : mode;
        lastSelectedCardIndex = Math.max(0, selected);
        lastSelectedCardCount = Math.max(0, count);
    }

    public static Identifier lastHoveredRecipeId() {
        return lastHoveredRecipeId;
    }

    public static Identifier lastHoveredItemId() {
        return lastHoveredItemId;
    }

    public static Identifier lastQueriedItemId() {
        return lastQueriedItemId;
    }

    public static ViewMode lastViewMode() {
        return lastViewMode;
    }

    public static String selectedCardLabel() {
        if (lastSelectedCardCount <= 0) {
            return "0 / 0";
        }
        return (Math.min(lastSelectedCardIndex, lastSelectedCardCount - 1) + 1) + " / " + lastSelectedCardCount;
    }

    public static String lastQueryCacheState() {
        return lastQueryCacheState;
    }

    private static CardMode cardMode(int width, int height) {
        if (height < 128 || width < 210) {
            return CardMode.COMPACT;
        }
        if (width >= 390 && height >= 300) {
            return CardMode.DIAGRAM;
        }
        return height < 204 || width < 270 ? CardMode.STANDARD : CardMode.TALL;
    }

    private static int maxStatPills(CardMode mode) {
        return switch (mode) {
            case COMPACT -> 2;
            case STANDARD -> 4;
            case TALL -> 6;
            case DIAGRAM -> 7;
        };
    }

    private static int maxNeedRows(CardMode mode) {
        return switch (mode) {
            case COMPACT -> 2;
            case STANDARD -> 4;
            case TALL -> 6;
            case DIAGRAM -> 7;
        };
    }

    private static int maxNoteRows(CardMode mode) {
        return switch (mode) {
            case COMPACT -> 1;
            case STANDARD -> 2;
            case TALL, DIAGRAM -> 3;
        };
    }

    private static int statusLaneWidth(IndexRecipePlan plan, CardMode mode) {
        int base = mode == CardMode.COMPACT ? 64 : 82;
        if (plan != null && plan.missingCount() >= 10) {
            base += 10;
        }
        return Math.min(mode == CardMode.COMPACT ? 88 : 104, base);
    }

    private static int layoutVisualWidth(IndexRecipeDisplayMetadata metadata, CardMode mode) {
        if (mode == CardMode.DIAGRAM) {
            return switch (metadata.type()) {
                case CRAFTING_SHAPED, CRAFTING_SHAPELESS -> metadata.machine().isEmpty() ? 188 : 242;
                case COOKING, STONECUTTING -> 218;
                case SMITHING -> 238;
                case GENERIC -> 160;
            };
        }
        return switch (metadata.type()) {
            case CRAFTING_SHAPED, CRAFTING_SHAPELESS -> metadata.machine().isEmpty() ? 130 : 186;
            case COOKING, STONECUTTING -> 166;
            case SMITHING -> 176;
            case GENERIC -> 120;
        };
    }

    public static String statusLabel(IndexRecipePlan plan, boolean allowTransfer) {
        if (plan == null) {
            return text("screen.echoindex.recipe.status.plan_only");
        }
        if (plan.sourceCard()) {
            return text("screen.echoindex.recipe.status.plan_only");
        }
        if (allowTransfer && plan.canTransfer()) {
            return text("screen.echoindex.recipe.status.ready");
        }
        if (plan.missingCount() > 0) {
            return text("screen.echoindex.recipe.status.missing", plan.missingCount());
        }
        if (!plan.transferBlocker().isBlank() && plan.craftingRecipe()) {
            return text("screen.echoindex.recipe.status.blocked");
        }
        if (machineRequired(plan)) {
            return text("screen.echoindex.recipe.status.machine_required");
        }
        return plan.state().label();
    }

    public static int statusColor(IndexRecipePlan plan, boolean allowTransfer) {
        if (plan == null) {
            return MUTED;
        }
        if (allowTransfer && plan.canTransfer()) {
            return GREEN;
        }
        if (plan.missingCount() > 0) {
            return WARN;
        }
        if (machineRequired(plan)) {
            return WARN;
        }
        return switch (plan.state()) {
            case READY -> GREEN;
            case MISSING -> WARN;
            case PLAN_ONLY -> MUTED;
        };
    }

    public static String statusDetail(IndexRecipePlan plan, boolean allowTransfer) {
        if (plan == null) {
            return "";
        }
        if (allowTransfer && plan.canTransfer()) {
            return text("screen.echoindex.recipe.status.ready_transfer");
        }
        if (plan.missingCount() > 0) {
            return text("screen.echoindex.recipe.status.missing", plan.missingCount());
        }
        if (!plan.transferBlocker().isBlank()) {
            return plan.transferBlocker();
        }
        if (plan.sourceCard()) {
            return text("screen.echoindex.recipe.status.plan_only");
        }
        return plan.state() == IndexRecipeActionState.PLAN_ONLY ? text("screen.echoindex.recipe.status.plan_only") : "";
    }

    private static boolean machineRequired(IndexRecipePlan plan) {
        return plan != null
                && !plan.sourceCard()
                && !plan.craftingRecipe()
                && plan.state() == IndexRecipeActionState.PLAN_ONLY;
    }

    private static int availableCount(Item item) {
        Player player = Minecraft.getInstance().player;
        if (item == null || player == null) {
            return 0;
        }
        int total = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (!stack.isEmpty() && stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static String layoutLabel(IndexRecipeLayoutType type) {
        return switch (type) {
            case CRAFTING_SHAPED -> text("screen.echoindex.recipe.layout.shaped_crafting");
            case CRAFTING_SHAPELESS -> text("screen.echoindex.recipe.layout.shapeless_crafting");
            case COOKING -> text("screen.echoindex.recipe.layout.cooking");
            case STONECUTTING -> text("screen.echoindex.recipe.layout.stonecutting");
            case SMITHING -> text("screen.echoindex.recipe.layout.smithing");
            case GENERIC -> text("screen.echoindex.recipe.layout.recipe");
        };
    }

    private static String layoutBadge(IndexRecipeLayoutType type) {
        return switch (type) {
            case CRAFTING_SHAPED -> text("screen.echoindex.recipe.layout.shaped_badge");
            case CRAFTING_SHAPELESS -> text("screen.echoindex.recipe.layout.shapeless_badge");
            case COOKING -> text("screen.echoindex.recipe.layout.cooking");
            case STONECUTTING -> text("screen.echoindex.recipe.layout.stonecutting");
            case SMITHING -> text("screen.echoindex.recipe.layout.smithing");
            case GENERIC -> text("screen.echoindex.recipe.label.machine");
        };
    }

    private static int badgeColor(IndexRecipeLayoutType type) {
        return switch (type) {
            case CRAFTING_SHAPED, CRAFTING_SHAPELESS -> CYAN;
            case COOKING -> 0xFFFFB86B;
            case STONECUTTING -> 0xFFD6E6EE;
            case SMITHING -> 0xFFE09CFF;
            case GENERIC -> WARN;
        };
    }

    private static void drawBadge(GuiGraphicsExtractor graphics, Font font, int x, int y, String label, int color) {
        int w = Math.max(32, font.width(label) + 10);
        graphics.fill(x, y, x + w, y + 13, 0x66102630);
        graphics.outline(x, y, w, 13, color);
        graphics.centeredText(font, label, x + w / 2, y + 4, color);
    }

    private static String roleLabel(IndexSlotRole role) {
        return switch (role) {
            case OUTPUT -> text("screen.echoindex.recipe.label.output");
            case MACHINE -> text("screen.echoindex.recipe.label.machine");
            case CATALYST -> text("screen.echoindex.recipe.label.catalyst");
            case INPUT -> text("screen.echoindex.recipe.label.input");
            default -> text("screen.echoindex.recipe.label.info");
        };
    }

    private enum CardMode {
        COMPACT,
        STANDARD,
        TALL,
        DIAGRAM
    }

    private static final class NeedAccumulator {
        private final ItemStack stack;
        private int required;
        private int missing;

        private NeedAccumulator(ItemStack stack) {
            this.stack = stack.copy();
        }

        private void add(int required, int missing) {
            this.required += Math.max(0, required);
            this.missing += Math.max(0, missing);
        }

        private NeedRow row() {
            return new NeedRow(stack, required, Math.min(required, missing));
        }
    }

    private record NeedRow(ItemStack stack, int required, int missing) {
    }

    private record SlotLane(String title, int color, List<IndexRecipeSlot> slots) {
    }

    private record RenderedLane(int nextY, int slots) {
    }

    public static void drawSlotGroup(GuiGraphicsExtractor graphics, Font font, IndexRecipeSlot slot, int x, int y,
            int width, int mouseX, int mouseY, List<SlotHit> slotHits) {
        drawSlotGroup(graphics, font, slot, x, y, width, mouseX, mouseY, slotHits, null);
    }

    public static void drawSlotGroup(GuiGraphicsExtractor graphics, Font font, IndexRecipeSlot slot, int x, int y,
            int width, int mouseX, int mouseY, List<SlotHit> slotHits, IndexRecipePlan plan) {
        int labelColor = switch (slot.role()) {
            case OUTPUT -> GREEN;
            case MACHINE -> WARN;
            case CATALYST -> 0xFFE09CFF;
            case INPUT -> CYAN;
            default -> MUTED;
        };
        IndexIngredientNeed need = needFor(plan, slot);
        if (need != null) {
            labelColor = need.satisfied() ? GREEN : WARN;
        }
        String label = need == null ? slotLabel(slot)
                : slotLabel(slot) + " " + Math.min(need.available(), need.required()) + "/" + need.required();
        int labelW = Math.min(86, Math.max(56, width / 3));
        graphics.text(font, trim(font, label, labelW), x, y + 6, labelColor, false);
        int itemX = x + labelW + 4;
        int max = Math.max(1, (width - labelW - 8) / 20);
        List<ItemStack> visibleStacks = slot.stacks().stream()
                .filter(stack -> stack != null && !stack.isEmpty())
                .limit(max)
                .toList();
        if (visibleStacks.isEmpty()) {
            int textW = Math.max(28, width - labelW - 6);
            graphics.fill(itemX, y + 1, itemX + textW, y + 19, 0x66102630);
            graphics.outline(itemX, y + 1, textW, 18, labelColor);
            graphics.text(font, trim(font, textSlotLabel(slot), textW - 8), itemX + 4, y + 6, labelColor, false);
            return;
        }
        for (ItemStack stack : visibleStacks) {
            boolean hover = inside(mouseX, mouseY, itemX, y, 20, 20);
            graphics.fill(itemX, y, itemX + 20, y + 20, hover ? SLOT_BG_HOVER : SLOT_BG);
            graphics.outline(itemX, y, 20, 20, hover ? SLOT_OUTLINE_HOVER : SLOT_OUTLINE);
            graphics.item(stack, itemX + 2, y + 2);
            graphics.itemDecorations(font, stack, itemX + 2, y + 2);
            if (hover) {
                IndexTooltipUtil.showItemTooltip(graphics, font, stack, itemX + 10, y + 10);
            }
            if (slotHits != null && !stack.isEmpty()) {
                slotHits.add(new SlotHit(itemX, y, 20, 20, stack.copy(), slot.role(),
                        choiceKey(slot.role(), itemX, y, slot.stacks()), visibleChoiceCount(slot.stacks())));
            }
            itemX += 22;
        }
    }

    private static IndexIngredientNeed needFor(IndexRecipePlan plan, IndexRecipeSlot slot) {
        if (plan == null || slot == null || slot.stacks().isEmpty()) {
            return null;
        }
        for (IndexIngredientNeed need : plan.needs()) {
            if (need.role() != slot.role() || need.selected().isEmpty()) {
                continue;
            }
            for (ItemStack stack : slot.stacks()) {
                if (!stack.isEmpty() && stack.is(need.selected().getItem())) {
                    return need;
                }
            }
        }
        return null;
    }

    public static String emptyMessage(Player player, Item item, ViewMode mode) {
        if (clientContext(player)) {
            requestServerViews(item);
            var result = IndexRecipeQueryClientState.result(item);
            if (result.isEmpty()) {
                return text("screen.echoindex.recipe.empty.loading");
            }
            String warning = result.get().warning();
            if (!warning.isBlank()) {
                return warning;
            }
            return switch (mode) {
                case USES -> text("screen.echoindex.recipe.empty.no_uses");
                case SOURCES -> IndexRecipeQueryClientState.health().sourceFactCount() > 0
                        ? text("screen.echoindex.recipe.empty.no_sources")
                        : text("screen.echoindex.recipe.empty.no_sources_loading");
                case RECIPES -> text("screen.echoindex.recipe.empty.no_recipes");
            };
        }
        var snapshot = IndexService.INSTANCE.recipeSnapshot(player);
        boolean noProviderRecipes = snapshot.recipes().isEmpty();
        boolean sourcesLoaded = snapshot.sourceCardsLoaded();
        return switch (mode) {
            case USES -> text("screen.echoindex.recipe.empty.no_uses");
            case SOURCES -> sourcesLoaded
                    ? text("screen.echoindex.recipe.empty.no_sources")
                    : text("screen.echoindex.recipe.empty.no_sources_loading");
            case RECIPES -> {
                if (snapshot.recipesStillLoading()) {
                    yield text("screen.echoindex.recipe.empty.loading");
                }
                if (noProviderRecipes) {
                    yield text("screen.echoindex.recipe.empty.provider_warning");
                }
                yield text("screen.echoindex.recipe.empty.no_recipes");
            }
        };
    }

    private static String textSlotLabel(IndexRecipeSlot slot) {
        String label = slotLabel(slot).strip();
        if (label.isBlank()) {
            return roleLabel(slot.role());
        }
        String lower = label.toLowerCase(Locale.ROOT);
        if (lower.startsWith("input fluid:")) {
            return text("screen.echoindex.recipe.label.fluid_in", label.substring("input fluid:".length()).strip());
        }
        if (lower.startsWith("output fluid:")) {
            return text("screen.echoindex.recipe.label.fluid_out", label.substring("output fluid:".length()).strip());
        }
        if (lower.startsWith("fluid:")) {
            return text("screen.echoindex.recipe.label.fluid", label.substring("fluid:".length()).strip());
        }
        return label;
    }

    private static String compactSlotText(String label) {
        String safe = label == null ? "" : label.strip();
        if (safe.isBlank()) {
            return "";
        }
        String[] words = safe.split("\\s+");
        if (words.length == 1) {
            return words[0].length() <= 4 ? words[0] : words[0].substring(0, 4);
        }
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (!word.isBlank()) {
                builder.append(Character.toUpperCase(word.charAt(0)));
            }
            if (builder.length() >= 3) {
                break;
            }
        }
        return builder.toString();
    }

    public static String slotLabel(IndexRecipeSlot slot) {
        if (!slot.label().isBlank()) {
            return slot.label();
        }
        return switch (slot.role()) {
            case OUTPUT -> text("screen.echoindex.recipe.label.output");
            case MACHINE -> text("screen.echoindex.recipe.label.machine");
            case CATALYST -> text("screen.echoindex.recipe.label.catalyst");
            case INPUT -> text("screen.echoindex.recipe.label.input");
            default -> text("screen.echoindex.recipe.label.info");
        };
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static String trim(Font font, String text, int width) {
        String safe = text == null ? "" : text;
        if (font.width(safe) <= width) {
            return safe;
        }
        String ellipsis = "...";
        while (!safe.isEmpty() && font.width(safe + ellipsis) > width) {
            safe = safe.substring(0, safe.length() - 1);
        }
        return safe + ellipsis;
    }

    public enum ViewMode {
        RECIPES("Recipes"),
        USES("Uses"),
        SOURCES("Sources");

        private final String label;

        ViewMode(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private static void requestServerViews(Item item) {
        if (item == null) {
            return;
        }
        Identifier itemId = IndexService.itemId(item);
        if (IndexRecipeQueryClientState.shouldRequest(itemId)) {
            EchoNetClientActions.trySendServerboundAction(new IndexRecipeQueryPacket(itemId, true, true, true));
        }
    }

    private static boolean clientContext(Player player) {
        return player != null && player.level() != null && player.level().getServer() == null;
    }

    private static void recordQueryState(Player player, Item item, ViewMode mode) {
        lastViewMode = mode == null ? ViewMode.RECIPES : mode;
        if (item == null) {
            lastQueryCacheState = "none";
            return;
        }
        Identifier itemId = IndexService.itemId(item);
        lastQueriedItemId = itemId;
        if (clientContext(player)) {
            lastQueryCacheState = IndexRecipeQueryClientState.result(item).isPresent() ? "hit" : "miss";
        } else {
            lastQueryCacheState = "server";
        }
    }

    public record SlotHit(int x, int y, int w, int h, ItemStack stack, IndexSlotRole role,
            String choiceKey, int choiceCount) {
        public SlotHit(int x, int y, int w, int h, ItemStack stack) {
            this(x, y, w, h, stack, IndexSlotRole.INFO, "", 0);
        }

        public boolean choiceCyclable() {
            return choiceCount > 1 && choiceKey != null && !choiceKey.isBlank();
        }
    }
}
