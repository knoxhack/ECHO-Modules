package com.knoxhack.echoashfallprotocol.client.hud;

import com.knoxhack.echoashfallprotocol.Config;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.echo.MissionRegistry;
import com.knoxhack.echoashfallprotocol.echo.Mission;
import com.knoxhack.echoashfallprotocol.echo.MissionUxSummary;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.entity.ScoutDrone;
import com.knoxhack.echoashfallprotocol.event.EnvironmentalEventStatus;
import com.knoxhack.echoashfallprotocol.event.EnvironmentalEventType;
import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import com.knoxhack.echoashfallprotocol.survival.ColdData;
import com.knoxhack.echoashfallprotocol.survival.MutationData;
import com.knoxhack.echoashfallprotocol.survival.MutationManager;
import com.knoxhack.echoashfallprotocol.survival.SurvivalData;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echoterminal.client.hud.TerminalHudNotice;
import com.knoxhack.echoterminal.client.hud.TerminalHudNoticeSurface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Survival HUD overlay — supports COMPACT and NORMAL display modes.
 * Press [V] to cycle between modes. Press [N] to reopen the field briefing.
 */
public class SurvivalHudOverlay {

    // Layout constants for NORMAL mode
    private static final int PX   = 6;
    private static final int PY   = 6;
    private static final int PW   = 214;
    private static final int BW   = 94;   // bar width
    private static final int BH   = 8;    // bar height

    // Layout constants for COMPACT mode
    private static final int CPW  = 100;
    private static final int CBH  = 5;
    private static final int NOTICE_SHELF_GAP = 6;
    private static final int NOTICE_ROW_HEIGHT = 36;
    private static final int NOTICE_ROW_GAP = 4;
    private static final int NOTICE_MAX_ROWS = 2;
    private static final List<String> NORMAL_STATUS_ROW_LABELS = List.of(
            "VITAL", "FOOD", "H2O", "AIR/MASK", "RAD", "TEMP");
    private static final int NORMAL_STATUS_BLOCK_HEIGHT = 94;

    // ECHO tactical vitals palette
    private static final int VITAL_AIR_SAFE = 0xFF46D6A8;
    private static final int VITAL_H2O_SAFE = 0xFF61C7FF;
    private static final int VITAL_RAD_LOW = 0xFF6DA8FF;
    private static final int VITAL_WARNING = 0xFFFFC95C;
    private static final int VITAL_DANGER = 0xFFFF5C5C;
    private static final int VITAL_TEXT = 0xFFE4F2FF;
    private static final int VITAL_TEXT_DIM = 0xFF98AFC4;
    private static final float FILTER_WARNING_PCT = 0.20f;
    private static final float RADIATION_WARNING_LEVEL = 35.0f;
    private static final Identifier HAZARD_ICON_ATLAS = Identifier.fromNamespaceAndPath(
            EchoAshfallProtocol.MODID, "textures/gui/hud/ashfall_event_icons.png");
    private static final int HAZARD_ICON_ATLAS_W = 512;
    private static final int HAZARD_ICON_ATLAS_H = 256;
    private static final int HAZARD_ICON_CELL = 128;
    private static final int ICON_RADIATION = 0;
    private static final int ICON_TOXIC = 1;
    private static final int ICON_BLACKOUT = 2;
    private static final int ICON_ASH = 3;
    private static final int ICON_CRYO = 4;
    private static final int ICON_NEXUS = 5;
    private static final int ICON_WATER = 6;
    private static final int ICON_SAFE = 7;
    private static final int PRIORITY_ACID_CONTACT = 100;
    private static final int PRIORITY_NEXUS_ANOMALY = 95;
    private static final int PRIORITY_TOXIC_NO_MASK = 92;
    private static final int PRIORITY_TOXIC_FILTER_EMPTY = 90;
    private static final int PRIORITY_NEXUS_EVENT = 84;
    private static final int PRIORITY_EXPOSED_STORM = 82;
    private static final int PRIORITY_MAJOR_EVENT = 80;
    private static final int PRIORITY_HIGH_CRYO = 76;
    private static final int PRIORITY_CRYO_EVENT = 74;
    private static final int PRIORITY_CRITICAL_RADIATION = 72;
    private static final int PRIORITY_CRITICAL_WATER = 66;
    private static final int PRIORITY_ASH_EVENT = 58;
    private static final int PRIORITY_BLACKOUT_EVENT = 54;
    private static final int PRIORITY_FILTER_ACTIVE = 42;
    private static final int PRIORITY_RADIATION_WARNING = 40;
    private static final int PRIORITY_CRYO_WARNING = 38;
    private static final int PRIORITY_WATER_WARNING = 34;
    private static final int PRIORITY_SHELTERED_STORM = 32;
    private static final int PRIORITY_LOW_FILTER = 28;
    private static final int PRIORITY_MUTATION = 22;
    private static final int PRIORITY_SAFE_RECOVERY = 8;
    private static final Comparator<HazardAlert> HAZARD_ALERT_ORDER =
            Comparator.comparingInt(HazardAlert::priority).reversed()
                    .thenComparing(HazardAlert::label)
                    .thenComparing(HazardAlert::value);

    public static void render(GuiGraphicsExtractor graphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || mc.screen != null) return;

        SurvivalData survival = player.getData(ModAttachments.SURVIVAL_DATA.get());
        MutationData mutations = player.getData(ModAttachments.MUTATION_DATA.get());
        QuestData quest        = player.getData(ModAttachments.QUEST_DATA.get());

        switch (HudState.getMode()) {
            case COMPACT  -> renderCompact(graphics, player, survival, mutations, quest);
            case NORMAL   -> renderNormal(graphics, player, survival, mutations, quest);
            case EXTENDED -> renderExtended(graphics, player, survival, mutations, quest);
        }
        BossHudOverlay.renderCompass(graphics, partialTick);
    }

    public record NoticeShelfLayout(int x, int y, int width, int rowHeight, int rowGap, int maxRows) {
    }

    public static NoticeShelfLayout noticeShelfLayoutForTests(int statusPanelBottom) {
        return noticeShelfLayout(statusPanelBottom);
    }

    public static List<String> normalStatusRowLabelsForTests() {
        return NORMAL_STATUS_ROW_LABELS;
    }

    public static int normalStatusPanelBottomForTests(int alertCount, boolean showMutations, boolean graceActive) {
        return PY + normalPanelHeight(alertCount, showMutations, graceActive);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COMPACT MODE — three slim bars + active mission title, all in ~68px tall
    // ─────────────────────────────────────────────────────────────────────────
    private static void renderCompact(GuiGraphicsExtractor graphics, Player player,
                                      SurvivalData survival, MutationData mutations, QuestData quest) {
        Minecraft mc = Minecraft.getInstance();
        int x = PX, y = PY;
        List<HazardAlert> alerts = buildHazardAlerts(survival, mutations);
        int visibleAlerts = Math.min(alerts.size(), 3);
        int chipBlockH = visibleAlerts > 0 ? visibleAlerts * 12 + 2 : 12;
        int panelH = 42 + chipBlockH + (HudState.isGraceActive() ? 26 : 0);

        // Shadow
        graphics.fill(x + 1, y + 1, x + CPW + 3, y + panelH + 1, 0x40000000);
        // Panel bg - slightly taller for 4 bars
        graphics.fill(x, y, x + CPW + 2, y + panelH, 0x88111826);
        // Top accent line
        graphics.fill(x, y, x + CPW + 2, y + 1, 0xAA4DBAF4);

        // Header
        graphics.text(mc.font, "A//S", x + 3, y + 3, 0xFF6ED4FF, false);
        // Mode hint
        graphics.text(mc.font, "[V]", x + CPW - 14, y + 3, 0x88AACCEE, false);
        y += 14;

        y += renderGraceCountdown(graphics, x + 3, y, CPW - 4);

        if (visibleAlerts == 0) {
            graphics.text(mc.font, "STATUS CLEAR", x + 3, y, VITAL_TEXT_DIM, false);
            y += 12;
        } else {
            for (int i = 0; i < visibleAlerts; i++) {
                renderCompactChip(graphics, x + 3, y, alerts.get(i));
                y += 12;
            }
        }

        // Active mission (one line, trimmed)
        MissionUxSummary compactMission = MissionUxSummary.current(player, quest);
        if (!compactMission.missionId().isBlank()) {
            String txt = mc.font.plainSubstrByWidth(compactMission.shortTitle(), CPW - 4);
            graphics.text(mc.font, txt, x + 3, y, 0xFFCCDDEE, false);
        }
        y += 10;

        // Mutation dot indicator
        if (shouldShowMutationPanel(mutations)) {
            String dot = "* ".repeat(Math.min(mutations.getMutationCount(), 5)).trim();
            graphics.text(mc.font, dot, x + 3, y, 0xFFC8A4FF, false);
        }

        // Open guidance hint (blinks)
        if ((System.currentTimeMillis() / 800L) % 2 == 0) {
            graphics.text(mc.font, terminalInstalled() ? "[M] TERMINAL" : "[N] GUIDE", x + 3, y, 0x7757A8FF, false);
        }
        renderTerminalNoticeShelf(graphics, PY + panelH);
    }

    private static void renderCompactBar(GuiGraphicsExtractor g, int x, int y, float pct, int color, String label) {
        Minecraft mc = Minecraft.getInstance();
        graphics_text(g, mc, label, x, y + 1, color);
        int bx = x + 10;
        g.fill(bx, y,       bx + CPW - 11, y + CBH + 2, 0x88151E29);
        g.fill(bx + 1, y + 1, bx + CPW - 12, y + CBH + 1, 0x44374658);
        int barW = CPW - 13;
        int fw = (int) (barW * Math.max(0f, Math.min(1f, pct)));
        if (fw > 0) g.fill(bx + 1, y + 1, bx + 1 + fw, y + CBH + 1, color);
        for (int i = 1; i <= 3; i++) {
            int tx = bx + 1 + (barW * i / 4);
            g.fill(tx, y + 1, tx + 1, y + CBH + 1, 0x557E9AB4);
        }

        // Critical flash warnings
        long now = System.currentTimeMillis();
        boolean flash = (now / 400L) % 2 == 0;

        // Missing mask, low air, high radiation, or low hydration - flash red/amber.
        if ((label.equals("M") && pct <= 0.0f) || (label.equals("A") && pct < 0.20f)) {
            if (flash) g.fill(bx + 1, y + 1, bx + CPW - 12, y + CBH + 1, warningOverlay(0x55, 0xFF0000));
        } else if (label.equals("R")) {
            if (pct > 0.75f && flash) {
                g.fill(bx + 1, y + 1, bx + CPW - 12, y + CBH + 1, warningOverlay(0x55, 0xFF0000));
            } else if (pct > 0.50f && flash) {
                g.fill(bx + 1, y + 1, bx + CPW - 12, y + CBH + 1, warningOverlay(0x55, 0xFFAA00));
            }
        } else if (label.equals("H") && pct < 0.20f && flash) {
            g.fill(bx + 1, y + 1, bx + CPW - 12, y + CBH + 1, warningOverlay(0x55, 0xFF0000));
        }
    }

    private static void renderCompactChip(GuiGraphicsExtractor g, int x, int y, HazardAlert alert) {
        Minecraft mc = Minecraft.getInstance();
        g.fill(x, y - 1, x + CPW - 4, y + 10, alert.backgroundColor());
        g.fill(x, y - 1, x + 1, y + 10, alert.color());
        renderHazardIcon(g, alert, x + 3, y, 10, 0xE8);
        String label = fitText(mc, alert.label(), 34);
        g.text(mc.font, label, x + 16, y + 1, alert.color(), false);
        String value = fitText(mc, alert.value(), 34);
        g.text(mc.font, value, x + CPW - 7 - mc.font.width(value), y + 1, VITAL_TEXT, false);
    }

    // Helper — avoids repeated inline casts
    private static void graphics_text(GuiGraphicsExtractor g, Minecraft mc, String text, int x, int y, int color) {
        g.text(mc.font, text, x, y, color, false);
    }

    private static void drawRightHeader(GuiGraphicsExtractor g, Minecraft mc, String status, String hint,
                                        int rightX, int y, int statusColor, int hintColor, int minStatusX) {
        int hintWidth = mc.font.width(hint);
        int hintX = rightX - hintWidth;
        g.text(mc.font, hint, hintX, y, hintColor, false);

        int statusWidth = mc.font.width(status);
        int statusX = hintX - 5 - statusWidth;
        if (statusX >= minStatusX) {
            g.text(mc.font, status, statusX, y, statusColor, false);
        }
    }

    private static int warningOverlay(int alpha, int rgb) {
        int scaledAlpha = Math.max(0, Math.min(255, (int)(alpha * Config.HUD_WARNING_INTENSITY.get())));
        return (scaledAlpha << 24) | (rgb & 0x00FFFFFF);
    }

    private static int filterColor(float pct) {
        if (pct <= 0.20f) return VITAL_DANGER;
        if (pct <= 0.45f) return VITAL_WARNING;
        return VITAL_AIR_SAFE;
    }

    private static int hydrationColor(float pct) {
        if (pct <= 0.20f) return VITAL_DANGER;
        if (pct <= 0.45f) return VITAL_WARNING;
        return VITAL_H2O_SAFE;
    }

    private static int radiationColor(float pct) {
        if (pct >= 0.75f) return VITAL_DANGER;
        if (pct >= 0.35f) return VITAL_WARNING;
        return VITAL_RAD_LOW;
    }

    private static int renderGraceCountdown(GuiGraphicsExtractor g, int x, int y, int w) {
        if (!HudState.isGraceActive()) {
            return 0;
        }

        Minecraft mc = Minecraft.getInstance();
        boolean warning = HudState.isGraceEndingSoon();
        long now = System.currentTimeMillis();
        boolean pulse = warning && (now / 420L) % 2L == 0L;
        int accent = warning ? (pulse ? 0xFFFFD54F : 0xFFE6A743) : 0xFF6ED4FF;
        int subColor = warning ? 0xFFFFD54F : 0xFF8FE6D1;
        int bg = warning ? 0x663A2A12 : 0x55203535;
        int boxW = Math.max(96, w);

        g.fill(x, y, x + boxW, y + 22, bg);
        g.fill(x, y, x + boxW, y + 1, accent);
        g.text(mc.font, "Emergency Buffer: " + HudState.getGraceCountdownText(), x + 4, y + 3, accent, false);
        g.text(mc.font, warning ? "Prep window ending" : "Equip, drink, shelter, craft", x + 4, y + 13, subColor, false);
        return 26;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NORMAL MODE — full panel with vitals, mutations, mission tracker
    // ─────────────────────────────────────────────────────────────────────────
    private static void renderNormal(GuiGraphicsExtractor graphics, Player player,
                                     SurvivalData survival, MutationData mutations, QuestData quest) {
        Minecraft mc  = Minecraft.getInstance();
        List<HazardAlert> alerts = buildHazardAlerts(survival, mutations);
        List<HazardAlert> secondaryAlerts = secondaryAlerts(alerts);
        boolean showMutations = shouldShowMutationPanel(mutations);
        int chipBlockH = normalAlertBlockHeight(alerts.size());
        int panelH = normalPanelHeight(alerts.size(), showMutations, HudState.isGraceActive());
        int y = PY;

        // Drop shadow
        graphics.fill(PX + 2, PY + 2, PX + PW + 2, PY + panelH + 2, 0x40000000);
        // Panel body
        graphics.fill(PX, PY, PX + PW, PY + panelH, 0x82111826);
        // Header bg
        graphics.fill(PX, PY, PX + PW, PY + 15, 0xBB223952);
        // Header bottom border
        graphics.fill(PX, PY + 15, PX + PW, PY + 16, 0xCC4DBAF4);
        // Top accent
        graphics.fill(PX, PY, PX + PW, PY + 1, 0xCC6ED4FF);
        // Right accent
        graphics.fill(PX + PW - 1, PY + 1, PX + PW, PY + panelH, 0x664DBAF4);

        // Header text
        graphics.text(mc.font, "ECHO // STATUS", PX + 6, y + 3, 0xFFE8F6FF, false);

        // ECHO status with pulse
        long t = System.currentTimeMillis();
        int pulseAlpha = 180 + (int)((Math.sin(t / 600.0) + 1.0) * 37);
        int echoColor = (pulseAlpha << 24) | 0x008EDCFF;
        drawRightHeader(graphics, mc, "ECHO-7", "[V]", PX + PW - 6, y + 3,
                echoColor, 0x66AACCEE, PX + 6 + mc.font.width("ECHO // STATUS") + 8);
        y += 20;

        y += renderGraceCountdown(graphics, PX + 6, y - 2, PW - 12);

        y = renderNormalStatusStack(graphics, player, survival, ColdData.get(player), PX + 4, y);

        graphics.fill(PX + 4, y - 4, PX + PW - 4, y + chipBlockH, 0x4A121C29);
        graphics.fill(PX + 4, y - 4, PX + PW - 4, y - 3, 0x884DBAF4);
        if (alerts.isEmpty()) {
            graphics.text(mc.font, "Expedition status stable", PX + 8, y + 2, VITAL_TEXT_DIM, false);
            y += chipBlockH;
        } else {
            HazardAlert primary = alerts.get(0);
            renderPriorityAlert(graphics, PX + 8, y, primary);
            y += 20;
            for (HazardAlert alert : secondaryAlerts) {
                renderStatusChip(graphics, PX + 8, y, alert);
                y += 14;
            }
            y += 4;
        }

        if (showMutations) {
            y = renderMutationSection(graphics, PX + 4, y, mutations);
        }
        renderMissionSection(graphics, PX + 4, y + 2, quest);

        // Guidance open hint (blinks slowly)
        if ((t / 1000L) % 6 < 3) {
            graphics.text(mc.font, terminalInstalled() ? "[M] Open Terminal" : "[N] Open Guide",
                    PX + 6, PY + panelH - 10, 0x6657A8FF, false);
        }
        renderTerminalNoticeShelf(graphics, PY + panelH);
    }

    private static int normalPanelHeight(int alertCount, boolean showMutations, boolean graceActive) {
        return 88 + NORMAL_STATUS_BLOCK_HEIGHT + normalAlertBlockHeight(alertCount)
                + (showMutations ? 40 : 0)
                + (graceActive ? 26 : 0);
    }

    private static int normalAlertBlockHeight(int alertCount) {
        if (alertCount <= 0) {
            return 18;
        }
        int secondaryCount = Math.min(Math.max(0, alertCount - 1), 4);
        return 24 + secondaryCount * 14 + 4;
    }

    private static int renderNormalStatusStack(GuiGraphicsExtractor graphics, Player player,
                                               SurvivalData survival, ColdData coldData,
                                               int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        FoodData food = player.getFoodData();
        float healthPct = player.getHealth() / Math.max(1.0f, player.getMaxHealth());
        float foodPct = food.getFoodLevel() / 20.0f;
        float hydrationPct = survival.getHydrationPercent();
        boolean toxicAir = survival.isToxicAirActive();
        boolean hasMask = survival.hasMask();
        float filterPct = survival.getFilterPercent();
        float airPct = hasMask ? filterPct : (toxicAir ? 0.0f : 1.0f);
        float radPct = survival.getRadiationLevel() / SurvivalData.MAX_RADIATION;
        float tempPct = coldData.getTemperature() / 100.0f;

        graphics.fill(x, y - 4, x + PW - 8, y + NORMAL_STATUS_BLOCK_HEIGHT - 2, 0x4F101B29);
        graphics.fill(x, y - 4, x + PW - 8, y - 3, 0xAA61C7FF);

        int rowY = y;
        renderTacticalVitalRow(graphics, x + 4, rowY, "VITAL", healthPct,
                Math.round(player.getHealth()) + "/" + Math.round(player.getMaxHealth()),
                healthColor(healthPct), healthPct <= 0.30f, healthPct <= 0.60f);
        rowY += 13;
        renderTacticalVitalRow(graphics, x + 4, rowY, "FOOD", foodPct,
                food.getFoodLevel() + "/20", foodColor(foodPct), foodPct <= 0.30f, foodPct <= 0.60f);
        rowY += 13;
        renderTacticalVitalRow(graphics, x + 4, rowY, "H2O", hydrationPct,
                survival.getHydration() + "%", hydrationColor(hydrationPct),
                hydrationPct <= 0.20f, hydrationPct <= 0.45f);
        rowY += 13;
        renderTacticalVitalRow(graphics, x + 4, rowY, "AIR/MASK", airPct,
                airStatusText(survival, toxicAir, hasMask, filterPct), filterColor(airPct),
                toxicAir && (!hasMask || survival.isFilterDepleted()), toxicAir && airPct <= 0.45f);
        rowY += 13;
        renderTacticalVitalRow(graphics, x + 4, rowY, "RAD", radPct,
                (int) survival.getRadiationLevel() + "%", radiationColor(radPct),
                radPct >= 0.75f, radPct >= 0.35f);
        rowY += 13;
        renderTacticalVitalRow(graphics, x + 4, rowY, "TEMP", tempPct,
                coldData.getStatus().getDisplayName(), temperatureColor(coldData),
                temperatureCritical(coldData), temperatureWarning(coldData));

        BlockPos pos = player.blockPosition();
        String fieldLine = "ARM " + player.getArmorValue()
                + "  POS " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
        graphics.text(mc.font, fitText(mc, fieldLine, PW - 18), x + 8, y + 80, VITAL_TEXT_DIM, false);
        return y + NORMAL_STATUS_BLOCK_HEIGHT;
    }

    private static String airStatusText(SurvivalData survival, boolean toxicAir, boolean hasMask, float filterPct) {
        if (toxicAir) {
            return hasMask ? "MASK " + (int) (filterPct * 100.0f) + "%" : "NO MASK";
        }
        if (hasMask && filterPct < 1.0f) {
            return "MASK " + (int) (filterPct * 100.0f) + "%";
        }
        return survival.hasMask() ? "MASK READY" : "CLEAR";
    }

    private static int healthColor(float pct) {
        if (pct <= 0.30f) return VITAL_DANGER;
        if (pct <= 0.60f) return VITAL_WARNING;
        return VITAL_AIR_SAFE;
    }

    private static int foodColor(float pct) {
        if (pct <= 0.30f) return VITAL_DANGER;
        if (pct <= 0.60f) return VITAL_WARNING;
        return VITAL_AIR_SAFE;
    }

    private static int temperatureColor(ColdData coldData) {
        return switch (coldData.getStatus()) {
            case FREEZING -> 0xFF2D7DFF;
            case COLD -> 0xFF44AAFF;
            case COOL -> 0xFF88CCFF;
            case NORMAL -> VITAL_AIR_SAFE;
            case WARM -> VITAL_WARNING;
        };
    }

    private static boolean temperatureCritical(ColdData coldData) {
        return switch (coldData.getStatus()) {
            case FREEZING -> true;
            default -> false;
        };
    }

    private static boolean temperatureWarning(ColdData coldData) {
        return switch (coldData.getStatus()) {
            case FREEZING, COLD, WARM -> true;
            default -> false;
        };
    }

    private static void renderTacticalVitalRow(GuiGraphicsExtractor graphics, int x, int y,
                                               String label, float rawPct, String rightText,
                                               int color, boolean critical, boolean warningPulse) {
        Minecraft mc = Minecraft.getInstance();
        float pct = Math.max(0f, Math.min(1f, rawPct));
        long now = System.currentTimeMillis();
        boolean flash = (now / 420L) % 2L == 0L;

        int rowW = PW - 16;
        int rowH = 12;
        int barX = x + 52;
        int barW = BW - 12;
        int fillW = (int)((barW - 2) * pct);
        int rowBg = critical && flash ? 0x552A1114 : 0x33151E29;
        String fittedRightText = fitText(mc, rightText, rowW - (barX - x) - barW - 8);

        graphics.fill(x, y - 1, x + rowW, y + rowH, rowBg);
        graphics.fill(x, y - 1, x + 1, y + rowH, color);
        graphics.text(mc.font, label, x + 4, y + 2, critical && flash ? VITAL_DANGER : VITAL_TEXT, false);

        graphics.fill(barX, y + 2, barX + barW, y + BH + 2, 0xAA101722);
        graphics.fill(barX + 1, y + 3, barX + barW - 1, y + BH + 1, 0x66374658);
        if (fillW > 0) {
            graphics.fill(barX + 1, y + 3, barX + 1 + fillW, y + BH + 1, color);
        }
        for (int i = 1; i <= 3; i++) {
            int tx = barX + (barW * i / 4);
            graphics.fill(tx, y + 3, tx + 1, y + BH + 1, 0x557E9AB4);
        }

        if (critical && flash) {
            graphics.fill(barX + 1, y + 3, barX + barW - 1, y + BH + 1, 0x33FF3333);
        } else if (warningPulse) {
            int alpha = 20 + (int)((Math.sin(now / 240.0) + 1.0) * 18);
            graphics.fill(barX + 1, y + 3, barX + Math.max(2, fillW), y + BH + 1, (alpha << 24) | 0x00FFC95C);
        }

        graphics.text(mc.font, fittedRightText, x + rowW - mc.font.width(fittedRightText) - 4, y + 2,
                critical && flash ? VITAL_DANGER : VITAL_TEXT_DIM, false);
    }

    private static void renderPriorityAlert(GuiGraphicsExtractor graphics, int x, int y, HazardAlert alert) {
        Minecraft mc = Minecraft.getInstance();
        int rowW = PW - 16;
        graphics.fill(x, y - 2, x + rowW, y + 18, alert.backgroundColor());
        graphics.fill(x, y - 2, x + rowW, y - 1, alert.color());
        graphics.fill(x, y - 2, x + 2, y + 18, alert.color());
        renderHazardIcon(graphics, alert, x + 5, y + 1, 14, 0xF0);
        String label = fitText(mc, alert.label(), 42);
        String action = fitText(mc, alert.actionText(), 72);
        String value = fitText(mc, alert.value(), 48);
        graphics.text(mc.font, label, x + 24, y + 1, alert.color(), false);
        graphics.text(mc.font, action, x + 70, y + 1, VITAL_TEXT, false);
        graphics.text(mc.font, value, x + rowW - mc.font.width(value) - 5, y + 1, VITAL_TEXT, false);
        String detail = fitText(mc, alert.title(), rowW - 34);
        graphics.text(mc.font, detail, x + 24, y + 10, VITAL_TEXT_DIM, false);
    }

    private static void renderStatusChip(GuiGraphicsExtractor graphics, int x, int y, HazardAlert alert) {
        Minecraft mc = Minecraft.getInstance();
        int rowW = PW - 16;
        graphics.fill(x, y - 1, x + rowW, y + 12, alert.backgroundColor());
        graphics.fill(x, y - 1, x + 1, y + 12, alert.color());
        renderHazardIcon(graphics, alert, x + 4, y + 1, 9, 0xCC);
        String label = fitText(mc, alert.label(), 38);
        graphics.text(mc.font, label, x + 17, y + 2, alert.color(), false);
        String value = fitText(mc, alert.value(), 88);
        graphics.text(mc.font, value, x + rowW - mc.font.width(value) - 5, y + 2, VITAL_TEXT, false);
    }

    private static List<HazardAlert> buildHazardAlerts(SurvivalData survival, MutationData mutations) {
        List<HazardAlert> alerts = new ArrayList<>();
        EnvironmentalEventStatus eventStatus = currentEventStatus();
        if (eventStatus.active()) {
            boolean shelteredRadiation = eventStatus.type() == EnvironmentalEventType.RADIATION_STORM
                    && survival.isStormSheltered();
            alerts.add(new HazardAlert(
                    eventPriority(eventStatus.type()),
                    eventChipLabel(eventStatus.type()),
                    shelteredRadiation ? "COVER" : "T-" + eventStatus.remainingSeconds() + "s",
                    eventStatus.centerWarningTitle(),
                    eventStatus.centerWarningSubtitle(),
                    shelteredRadiation ? "COVER HOLDING" : eventActionText(eventStatus.type()),
                    eventStatus.hudColor(),
                    0x4420142A,
                    eventIcon(eventStatus.type()),
                    !shelteredRadiation));
        }
        if (survival.isNexusAnomaly()) {
            alerts.add(HazardAlert.danger(PRIORITY_NEXUS_ANOMALY, "NEXUS", survival.getHazardSeverity(),
                    "NEXUS ANOMALY", "FIELD INSTABILITY CRITICAL\nRETREAT OR DEPLOY SCRUBBER",
                    "RETREAT", ICON_NEXUS, true));
        }
        if (survival.isAcidContact()) {
            alerts.add(HazardAlert.danger(PRIORITY_ACID_CONTACT, "ACID", "CONTACT",
                    "ACID CONTACT", "LEAVE SLUDGE IMMEDIATELY\nFULL HAZMAT ADVISED",
                    "MOVE NOW", ICON_TOXIC, true));
        }
        if (!eventStatus.active() && survival.isRadiationStorm()) {
            alerts.add(survival.isStormSheltered()
                    ? HazardAlert.warn(PRIORITY_SHELTERED_STORM, "STORM", "COVER",
                            "RADIATION STORM", "SHELTER HOLDING\nWAIT FOR CLEAR SKIES",
                            "COVER HOLDING", ICON_RADIATION, false)
                    : HazardAlert.danger(PRIORITY_EXPOSED_STORM, "STORM", "EXPOSED",
                            "RADIATION STORM", "EXPOSED TO GRIDFALL RAIN\nGET UNDER COVER",
                            "GET COVER", ICON_RADIATION, true));
        }
        if (survival.isCryoZone()) {
            boolean highCryo = "HIGH".equals(survival.getHazardSeverity());
            alerts.add(new HazardAlert(highCryo ? PRIORITY_HIGH_CRYO : PRIORITY_CRYO_WARNING,
                    "CRYO",
                    survival.getHazardSeverity(),
                    "CRYO COLD",
                    "BODY HEAT FALLING\nFIRE OR THERMAL LINER REQUIRED",
                    highCryo ? "FIND HEAT" : "KEEP WARM",
                    highCryo ? VITAL_DANGER : VITAL_WARNING,
                    highCryo ? 0x552A1114 : 0x553A2A12,
                    ICON_CRYO,
                    highCryo));
        }
        if (survival.isSafeZone()) {
            alerts.add(HazardAlert.safe(PRIORITY_SAFE_RECOVERY, "SAFE", survival.getRadiationLevel() > 5.0f ? "SCRUBBING" : "SCRUBBED",
                    "SAFE POCKET", "ATMOSPHERIC SCRUBBER FIELD ACTIVE",
                    "HOLD POSITION", ICON_SAFE, false));
        }

        float filterPct = survival.getFilterPercent();
        if (survival.isToxicAirActive()) {
            if (!survival.hasMask()) {
                alerts.add(HazardAlert.danger(PRIORITY_TOXIC_NO_MASK, "TOXIC", "NO MASK",
                        "TOXIC AIR", "MASK REQUIRED\nLEAVE EXPOSURE OR FIND SHELTER",
                        "MASK REQUIRED", ICON_TOXIC, true));
            } else if (survival.isFilterDepleted()) {
                alerts.add(HazardAlert.danger(PRIORITY_TOXIC_FILTER_EMPTY, "FILTER", "EMPTY",
                        "FILTER EMPTY", "AIR SEAL COMPROMISED\nREPLACE CARTRIDGE OR RETREAT",
                        "FILTER EMPTY", ICON_TOXIC, true));
            } else {
                alerts.add(HazardAlert.warn(PRIORITY_FILTER_ACTIVE, "FILTER", (int)(filterPct * 100.0f) + "%",
                        "FILTER ACTIVE", "TOXIC AIR FILTERING\nWATCH CARTRIDGE LIFE",
                        "FILTERING", ICON_TOXIC, false));
            }
        } else if (survival.hasMask() && filterPct <= FILTER_WARNING_PCT) {
            alerts.add(HazardAlert.warn(PRIORITY_LOW_FILTER, "FILTER", (int)(filterPct * 100.0f) + "%",
                    "LOW FILTER", "CARTRIDGE NEAR EMPTY\nREPLACE BEFORE EXPOSURE",
                    "LOW FILTER", ICON_TOXIC, false));
        }

        float radiation = survival.getRadiationLevel();
        if (radiation >= RADIATION_WARNING_LEVEL || survival.isRadiationZone()) {
            boolean critical = radiation >= 75.0f;
            String value = survival.isSafeZone() ? "SCRUBBING" : (int) radiation + "%";
            alerts.add(new HazardAlert(critical ? PRIORITY_CRITICAL_RADIATION : PRIORITY_RADIATION_WARNING,
                    "RAD",
                    value,
                    "CRITICAL RADIATION",
                    "RADIATION LEVELS DANGEROUS\nSEEK SHELTER IMMEDIATELY",
                    critical ? "SEEK SHELTER" : "LIMIT EXPOSURE",
                    critical ? VITAL_DANGER : VITAL_WARNING,
                    0x44261518,
                    ICON_RADIATION,
                    critical));
        }

        int hydrationWarning = Math.max(0, Math.min(100, Config.HYDRATION_WARNING_LEVEL.get()));
        int hydrationPenalty = Math.max(0, Math.min(100, Config.HYDRATION_PENALTY_LEVEL.get()));
        if (survival.getHydration() <= hydrationWarning) {
            boolean critical = survival.getHydration() <= hydrationPenalty;
            alerts.add(new HazardAlert(critical ? PRIORITY_CRITICAL_WATER : PRIORITY_WATER_WARNING,
                    "WATER",
                    survival.getHydration() + "%",
                    "DEHYDRATION",
                    "HYDRATION CRITICALLY LOW\nDRINK WATER NOW",
                    critical ? "DRINK WATER" : "WATER SOON",
                    critical ? VITAL_DANGER : VITAL_WARNING,
                    0x4412232A,
                    ICON_WATER,
                    critical));
        }

        if (MutationManager.shouldShowInstabilityWarning(survival, mutations)) {
            alerts.add(new HazardAlert(PRIORITY_MUTATION,
                    "MUT",
                    mutations.getMutationCount() + " UNSTABLE",
                    "MUTATION INSTABILITY",
                    "FIELD MED BAY ADVISED",
                    "MONITORING",
                    0xFFC8A4FF,
                    0x4420152D,
                    ICON_NEXUS,
                    false));
        }
        alerts.sort(HAZARD_ALERT_ORDER);
        return alerts;
    }

    private static List<HazardAlert> secondaryAlerts(List<HazardAlert> alerts) {
        if (alerts.size() <= 1) {
            return List.of();
        }
        return alerts.subList(1, Math.min(alerts.size(), 5));
    }

    private static boolean shouldShowMutationPanel(MutationData mutations) {
        return mutations.getMutationCount() >= 3 || !mutations.getActiveSideEffects().isEmpty();
    }

    private static HazardAlert primaryCenterAlert(List<HazardAlert> alerts) {
        for (HazardAlert alert : alerts) {
            if (alert.centerEligible()) {
                return alert;
            }
        }
        return null;
    }

    private static int eventPriority(EnvironmentalEventType type) {
        return switch (type) {
            case NEXUS_SURGE -> PRIORITY_NEXUS_EVENT;
            case RADIATION_STORM, TOXIC_STORM -> PRIORITY_MAJOR_EVENT;
            case CRYO_FRONT -> PRIORITY_CRYO_EVENT;
            case ASH_STORM -> PRIORITY_ASH_EVENT;
            case BLACKOUT -> PRIORITY_BLACKOUT_EVENT;
            default -> 0;
        };
    }

    private static String eventActionText(EnvironmentalEventType type) {
        return switch (type) {
            case RADIATION_STORM -> "GET COVER";
            case TOXIC_STORM -> "FILTERS UP";
            case BLACKOUT -> "RESERVE POWER";
            case ASH_STORM -> "SHELTER";
            case CRYO_FRONT -> "FIND HEAT";
            case NEXUS_SURGE -> "CLEAR SOURCE";
            default -> "MONITOR";
        };
    }

    private static int eventIcon(EnvironmentalEventType type) {
        return switch (type) {
            case RADIATION_STORM -> ICON_RADIATION;
            case TOXIC_STORM -> ICON_TOXIC;
            case BLACKOUT -> ICON_BLACKOUT;
            case ASH_STORM -> ICON_ASH;
            case CRYO_FRONT -> ICON_CRYO;
            case NEXUS_SURGE -> ICON_NEXUS;
            default -> ICON_SAFE;
        };
    }

    private static void renderHazardIcon(GuiGraphicsExtractor g, HazardAlert alert, int x, int y, int size, int alpha) {
        if (alert.iconIndex() < 0 || size <= 0) {
            return;
        }
        int icon = Math.max(0, Math.min(7, alert.iconIndex()));
        int u = (icon % 4) * HAZARD_ICON_CELL;
        int v = (icon / 4) * HAZARD_ICON_CELL;
        int color = (Math.max(0, Math.min(255, alpha)) << 24) | 0x00FFFFFF;
        g.blit(RenderPipelines.GUI_TEXTURED, HAZARD_ICON_ATLAS, x, y, u, v, size, size,
                HAZARD_ICON_CELL, HAZARD_ICON_CELL, HAZARD_ICON_ATLAS_W, HAZARD_ICON_ATLAS_H, color);
    }

    private static String fitText(Minecraft mc, String text, int maxWidth) {
        if (text == null || text.isBlank()) {
            return "";
        }
        if (mc.font.width(text) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        int suffixWidth = mc.font.width(suffix);
        if (maxWidth <= suffixWidth) {
            return mc.font.plainSubstrByWidth(text, maxWidth);
        }
        return mc.font.plainSubstrByWidth(text, maxWidth - suffixWidth) + suffix;
    }

    private static void renderTerminalNoticeShelf(GuiGraphicsExtractor graphics, int statusPanelBottom) {
        List<TerminalHudNotice> notices = terminalHudNotices();
        if (notices.isEmpty()) {
            return;
        }
        NoticeShelfLayout layout = noticeShelfLayout(statusPanelBottom);
        int rows = Math.min(layout.maxRows(), notices.size());
        for (int i = 0; i < rows; i++) {
            int rowY = layout.y() + i * (layout.rowHeight() + layout.rowGap());
            renderTerminalNoticeRow(graphics, layout.x(), rowY, layout.width(), layout.rowHeight(), notices.get(i));
        }
    }

    private static NoticeShelfLayout noticeShelfLayout(int statusPanelBottom) {
        return new NoticeShelfLayout(PX, statusPanelBottom + NOTICE_SHELF_GAP,
                PW, NOTICE_ROW_HEIGHT, NOTICE_ROW_GAP, NOTICE_MAX_ROWS);
    }

    private static List<TerminalHudNotice> terminalHudNotices() {
        if (!terminalInstalled()) {
            return List.of();
        }
        try {
            if (!TerminalHudNoticeSurface.externalSurfaceClaimed()) {
                return List.of();
            }
            return TerminalHudNoticeSurface.activeNotices();
        } catch (LinkageError | RuntimeException exception) {
            return List.of();
        }
    }

    private static void renderTerminalNoticeRow(GuiGraphicsExtractor graphics, int x, int y,
                                                int width, int height, TerminalHudNotice notice) {
        Minecraft mc = Minecraft.getInstance();
        int accent = notice.accentColor();
        graphics.fill(x + 2, y + 2, x + width + 2, y + height + 2, 0x40000000);
        graphics.fill(x, y, x + width, y + height, 0xA6111826);
        graphics.fill(x, y, x + width, y + 1, withAlpha(accent, 0xCC));
        graphics.fill(x, y, x + 2, y + height, withAlpha(accent, 0xDD));
        graphics.fill(x + 2, y + height - 1, x + width, y + height, withAlpha(accent, 0x55));

        String source = fitText(mc, notice.sourceLabel(), 88);
        String status = notice.hasCountBadge()
                ? notice.statusLabel() + " x" + notice.count()
                : notice.statusLabel();
        status = fitText(mc, status, 78);
        graphics.text(mc.font, source, x + 7, y + 4, withAlpha(accent, 0xFF), false);
        if (!status.isBlank()) {
            graphics.text(mc.font, status, x + width - 7 - mc.font.width(status), y + 4, 0xFF8CA7B5, false);
        }

        String title = fitText(mc, notice.title(), width - 16);
        graphics.text(mc.font, title, x + 7, y + 15, VITAL_TEXT, false);
        String detail = notice.detail().isBlank() ? notice.footer() : notice.detail();
        detail = fitText(mc, detail, width - 16);
        graphics.text(mc.font, detail, x + 7, y + 25, VITAL_TEXT_DIM, false);

        if (notice.hasProgress()) {
            int barX = x + 7;
            int barY = y + height - 4;
            int barW = width - 14;
            graphics.fill(barX, barY, barX + barW, barY + 2, 0x66364552);
            graphics.fill(barX, barY, barX + Math.max(2, Math.round(barW * notice.progress())), barY + 2, accent);
        }
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    private record HazardAlert(
            int priority,
            String label,
            String value,
            String title,
            String subtitle,
            String actionText,
            int color,
            int backgroundColor,
            int iconIndex,
            boolean centerEligible
    ) {
        static HazardAlert danger(int priority, String label, String value, String title, String subtitle,
                                  String actionText, int iconIndex, boolean centerEligible) {
            return new HazardAlert(priority, label, value, title, subtitle, actionText,
                    VITAL_DANGER, 0x552A1114, iconIndex, centerEligible);
        }

        static HazardAlert warn(int priority, String label, String value, String title, String subtitle,
                                String actionText, int iconIndex, boolean centerEligible) {
            return new HazardAlert(priority, label, value, title, subtitle, actionText,
                    VITAL_WARNING, 0x553A2A12, iconIndex, centerEligible);
        }

        static HazardAlert safe(int priority, String label, String value, String title, String subtitle,
                                String actionText, int iconIndex, boolean centerEligible) {
            return new HazardAlert(priority, label, value, title, subtitle, actionText,
                    VITAL_AIR_SAFE, 0x4420342D, iconIndex, centerEligible);
        }

        boolean pulseCenterBorder() {
            return centerEligible && priority >= PRIORITY_CRITICAL_WATER;
        }
    }

    private static int renderMutationSection(GuiGraphicsExtractor graphics, int x, int y, MutationData data) {
        int w = PW - 8;
        graphics.fill(x, y, x + w, y + 36, 0x4A111B28);
        graphics.fill(x, y, x + w, y + 1,  0x885C4A87);

        Minecraft mc = Minecraft.getInstance();
        String mutLabel = data.getMutationCount() == 0 ? "MUTATIONS: NONE"
                        : "MUTATIONS: " + data.getMutationCount() + "  ▲";
        graphics.text(mc.font, mutLabel, x + 4, y + 3, 0xFFC8A4FF, false);

        int listY = y + 14;
        int shown = 0;
        for (String mutId : data.getActiveMutations()) {
            MutationData.MutationType type = mutationFromId(mutId);
            String text = type != null ? ("+ " + type.getDisplayName()) : ("+ " + mutId);
            graphics.text(mc.font, text, x + 4, listY, 0xFFD4C9EA, false);
            listY += 9;
            shown++;
            if (shown >= 2) break;
        }
        if (shown == 0) {
            graphics.text(mc.font, "  Stable genome", x + 4, listY, 0xFFA3B2C4, false);
        }

        if (!data.getActiveSideEffects().isEmpty()) {
            String fx = data.getActiveSideEffects().get(0).toLowerCase(Locale.ROOT).replace('_', ' ');
            graphics.text(mc.font, "! " + fx, x + 108, y + 16, 0xFFE69797, false);
        }

        return y + 40;
    }

    private static void renderMissionSection(GuiGraphicsExtractor graphics, int x, int y, QuestData data) {
        Mission current = MissionRegistry.getMission(data.getCurrentPhase(), data.getCurrentMissionIndex());
        if (current == null) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        MissionUxSummary summary = player == null
                ? null
                : MissionUxSummary.forHud(player, data, current);
        int pw = PW - 8;
        int ph = 52;
        graphics.fill(x, y, x + pw, y + ph, 0x4A111B28);
        graphics.fill(x, y, x + pw, y + 1,  0x9961B7EA);
        // Thin bottom border
        graphics.fill(x, y + ph - 1, x + pw, y + ph, 0x6661B7EA);

        int totalPhases = Math.max(1, MissionRegistry.getPhaseCount());
        String phaseLabel = "PHASE " + (data.getCurrentPhase() + 1) + "/" + totalPhases;
        graphics.text(mc.font, phaseLabel, x + 4, y + 3, 0xFF9FC9E7, false);

        int cur   = data.getCurrentMissionIndex() + 1;
        int total = Math.max(1, MissionRegistry.getMissionCount(data.getCurrentPhase()));
        graphics.text(mc.font, cur + "/" + total, x + pw - 22, y + 3, 0xFF6EC6FF, false);

        // Objective text — wrap to 2 lines
        String full = summary == null ? current.objectiveText() : summary.shortTitle();
        String line1 = mc.font.plainSubstrByWidth(full, pw - 10);
        graphics.text(mc.font, line1, x + 4, y + 14, 0xFFF2F7FF, false);
        String step = summary == null ? current.echoMessage() : summary.nextStep();
        if (!step.isBlank()) {
            String line2 = mc.font.plainSubstrByWidth(step, pw - 10);
            graphics.text(mc.font, line2, x + 4, y + 24, 0xFFCCDDEE, false);
        }

        // Progress bar
        int pby = y + 42;
        int pbw = pw - 8;
        graphics.fill(x + 4, pby, x + 4 + pbw, pby + 5, 0x66364552);
        int fw = (int)(pbw * (cur / (float) total));
        graphics.fill(x + 4, pby, x + 4 + Math.max(2, fw), pby + 5, 0xFF57A8FF);

        // [M] hint blinks
        if ((System.currentTimeMillis() / 900L) % 2 == 0) {
            graphics.text(mc.font, "[M] full details", x + pw - 72, y + 3, 0x5557A8FF, false);
        }
    }

    private static MutationData.MutationType mutationFromId(String id) {
        for (MutationData.MutationType t : MutationData.MutationType.values()) {
            if (t.getId().equals(id)) return t;
        }
        return null;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // EXTENDED MODE — Full immersive HUD with all panels (Mockup Design)
    // ═════════════════════════════════════════════════════════════════════════

    // Color constants matching the mockup design
    private static final int COL_BG_DARK   = 0xFF080C14;
    private static final int COL_BG_PANEL  = 0xFF121820;
    private static final int COL_PRIMARY   = 0xFF2980FF;
    private static final int COL_SUCCESS   = 0xFF00E676;
    private static final int COL_WARNING   = 0xFFFFD54F;
    private static final int COL_DANGER    = 0xFFFF5252;
    private static final int COL_INFO      = 0xFF00B0FF;
    private static final int COL_TEXT      = 0xFFE0E8F0;
    private static final int COL_TEXT_DIM  = 0xFF8B8EC5;
    private static final int COL_PURPLE    = 0xFFC8A4FF;

    private static void renderExtended(GuiGraphicsExtractor g, Player player,
                                       SurvivalData survival, MutationData mutations, QuestData quest) {
        Minecraft mc = Minecraft.getInstance();
        ColdData coldData = ColdData.get(player);
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        // ── TOP LEFT: ECHO-7 Terminal Panel ──
        renderEchoTerminalPanel(g, 6, 6, survival, mutations, coldData);
        renderTerminalNoticeShelf(g, 6 + extendedEchoPanelHeight());

        // ── TOP RIGHT: Situation Report ──
        renderSituationReport(g, screenW - 156, 6, player, survival, coldData);

        // ── LEFT CENTER: Active Effects ──
        renderActiveEffects(g, 6, 145, player);

        // ── RIGHT CENTER: Mini Radar ──
        renderMiniRadar(g, screenW - 86, 145, player);

        // ── LEFT BOTTOM: Mission Tracker ──
        renderExtendedMissionTracker(g, 6, screenH - 95, quest);

        // ── RIGHT BOTTOM: Time & Day ──
        renderTimeAndDay(g, screenW - 86, screenH - 55, player);

        // ── CENTER: Hazard Warnings ──
        renderCenterWarnings(g, screenW / 2, screenH / 2 - 30, survival, mutations);
    }

    private static void renderEchoTerminalPanel(GuiGraphicsExtractor g, int x, int y,
                                                 SurvivalData survival, MutationData mutations, ColdData coldData) {
        Minecraft mc = Minecraft.getInstance();
        int w = 140, h = extendedEchoPanelHeight();

        // Panel background with improved styling
        g.fill(x, y, x + w, y + h, COL_BG_PANEL);
        // Inner shadow
        g.fill(x, y, x + w, y + 1, 0xFF0A1018);
        g.fill(x, y, x + 1, y + h, 0xFF0A1018);
        // Outer border
        g.fill(x, y, x + w, y + 1, COL_PRIMARY);
        g.fill(x, y + h - 1, x + w, y + h, COL_PRIMARY);
        g.fill(x, y, x + 1, y + h, COL_PRIMARY);
        g.fill(x + w - 1, y, x + w, y + h, COL_PRIMARY);
        // Corner accents
        g.fill(x, y, x + 8, y + 2, COL_PRIMARY);
        g.fill(x, y, x + 2, y + 8, COL_PRIMARY);

        // Header with pulsing ONLINE indicator
        long t = System.currentTimeMillis();
        int pulseAlpha = 180 + (int)((Math.sin(t / 600.0) + 1.0) * 37);
        int echoColor = (pulseAlpha << 24) | 0x008EDCFF;
        String header = terminalInstalled() ? "ECHO-7 TERMINAL" : "ECHO-7 HUD";
        g.text(mc.font, header, x + 4, y + 3, COL_PRIMARY, false);
        drawRightHeader(g, mc, "ONLINE", "[V]", x + w - 4, y + 3,
                echoColor, 0x66AACCEE, x + 4 + mc.font.width(header) + 8);
        y += 14;

        int barW = w - 12;
        int barH = 8;
        y += renderGraceCountdown(g, x + 4, y, barW);

        // MASK / AIR / RAD / H2O vitals
        boolean hasMask = survival.hasMask();
        boolean toxicAir = survival.isToxicAirActive();
        renderExtendedBar(g, x + 4, y, barW, barH, hasMask ? 1.0f : 0.0f,
                "MASK", hasMask ? "READY" : (toxicAir ? "NO MASK" : "STOWED"), hasMask || !toxicAir ? VITAL_AIR_SAFE : VITAL_DANGER);
        y += 14;

        float filterPct = survival.getFilterPercent();
        float airPct = hasMask ? filterPct : (toxicAir ? 0.0f : 1.0f);
        String filterStatus = toxicAir ? (hasMask ? (int)(filterPct * 100) + "%" : "UNSEALED") : "CLEAR";
        int airBarColor = toxicAir && (!hasMask || survival.isFilterDepleted()) ? VITAL_DANGER : filterColor(airPct);
        renderExtendedBar(g, x + 4, y, barW, barH, airPct, "AIR", filterStatus, airBarColor);
        y += 14;

        float radPct = survival.getRadiationLevel() / SurvivalData.MAX_RADIATION;
        String radStatus = (int)survival.getRadiationLevel() + "%";
        int radColor = radiationColor(radPct);
        renderExtendedBar(g, x + 4, y, barW, barH, radPct, "RAD", radStatus, radColor);
        y += 14;

        float h2oPct = survival.getHydrationPercent();
        String h2oStatus = survival.getHydration() + "%";
        int h2oColor = hydrationColor(h2oPct);
        renderExtendedBar(g, x + 4, y, barW, barH, h2oPct, "H2O", h2oStatus, h2oColor);
        y += 14;

        // TEMP bar
        float tempPct = coldData.getTemperature() / 100.0f;
        String tempStatus = coldData.getStatus().getDisplayName();
        int tempColor = switch (coldData.getStatus()) {
            case FREEZING -> 0xFF0044AA;
            case COLD -> 0xFF44AAFF;
            case COOL -> 0xFF88CCFF;
            case NORMAL -> COL_SUCCESS;
            case WARM -> COL_WARNING;
        };
        renderExtendedBar(g, x + 4, y, barW, barH, tempPct, "TEMP", tempStatus, tempColor);
        y += 16;

        // MASK STATUS section
        g.text(mc.font, "MASK STATUS", x + 4, y, COL_TEXT_DIM, false);
        y += 10;
        String maskLine = survival.hasMask() ? "Durability: " + (int)(filterPct * 100) + "%" : "NO MASK EQUIPPED";
        g.text(mc.font, maskLine, x + 8, y, survival.hasMask() ? COL_TEXT : COL_DANGER, false);
        y += 10;
        boolean filtering = survival.isToxicAirActive() && survival.hasMask() && !survival.isFilterDepleted();
        String airQuality = survival.isToxicAirActive()
                ? (filtering ? "AIR QUALITY: FILTERING" : "AIR QUALITY: TOXIC")
                : "AIR QUALITY: CLEAR";
        int airColor = survival.isToxicAirActive() && !filtering ? COL_DANGER : COL_SUCCESS;
        g.text(mc.font, airQuality, x + 8, y, airColor, false);
        y += 14;

        // MUTATIONS section
        String mutLabel = "MUTATIONS: " + mutations.getMutationCount();
        g.text(mc.font, mutLabel, x + 4, y, COL_PURPLE, false);
        if (!mutations.getActiveMutations().isEmpty()) {
            y += 10;
            int shown = 0;
            for (String mutId : mutations.getActiveMutations()) {
                if (shown >= 2) break;
                MutationData.MutationType type = mutationFromId(mutId);
                String name = type != null ? type.getDisplayName() : mutId;
                g.text(mc.font, "+ " + name, x + 8, y, COL_PURPLE, false);
                y += 9;
                shown++;
            }
        }
    }

    private static int extendedEchoPanelHeight() {
        return HudState.isGraceActive() ? 184 : 158;
    }

    private static void renderExtendedBar(GuiGraphicsExtractor g, int x, int y, int w, int h,
                                          float pct, String label, String value, int color) {
        Minecraft mc = Minecraft.getInstance();
        pct = Math.max(0f, Math.min(1f, pct));

        // Label
        g.text(mc.font, label, x, y, COL_TEXT_DIM, false);

        // Bar track
        int barX = x + 38;
        int barW = w - 50;
        g.fill(barX, y + 1, barX + barW, y + h + 1, 0xFF1A2430);
        g.fill(barX + 1, y + 2, barX + barW - 1, y + h, 0xFF374658);

        // Bar fill
        int fw = (int)((barW - 2) * pct);
        if (fw > 0) g.fill(barX + 1, y + 2, barX + 1 + fw, y + h, color);
        for (int i = 1; i <= 3; i++) {
            int tx = barX + (barW * i / 4);
            g.fill(tx, y + 2, tx + 1, y + h, 0x557E9AB4);
        }

        // Value text
        g.text(mc.font, value, x + w - mc.font.width(value) - 2, y, COL_TEXT, false);
    }

    private static void renderSituationReport(GuiGraphicsExtractor g, int x, int y,
                                               Player player, SurvivalData survival, ColdData coldData) {
        Minecraft mc = Minecraft.getInstance();
        int w = 150, h = 130;

        // Panel background with gradient effect
        g.fill(x, y, x + w, y + h, COL_BG_PANEL);
        // Inner shadow (top/left darker)
        g.fill(x, y, x + w, y + 1, 0xFF0A1018);
        g.fill(x, y, x + 1, y + h, 0xFF0A1018);
        // Outer border with primary color
        g.fill(x, y, x + w, y + 1, COL_PRIMARY);
        g.fill(x, y + h - 1, x + w, y + h, COL_PRIMARY);
        g.fill(x, y, x + 1, y + h, COL_PRIMARY);
        g.fill(x + w - 1, y, x + w, y + h, COL_PRIMARY);
        // Corner accents
        g.fill(x, y, x + 8, y + 2, COL_PRIMARY);
        g.fill(x, y, x + 2, y + 8, COL_PRIMARY);

        // Header
        g.text(mc.font, "SITUATION REPORT", x + 4, y + 3, COL_PRIMARY, false);
        y += 16;

        // Environment/Biome - extract name from registry key
        BlockPos pos = player.blockPosition();
        var biomeHolder = player.level().getBiome(pos);
        String biomeName = biomeHolder.unwrapKey()
                .map(Object::toString)
                .map(str -> {
                    // Extract path from "ResourceKey[minecraft:worldgen/biome / minecraft:plains]"
                    int slash = str.lastIndexOf('/');
                    int bracket = str.lastIndexOf(']');
                    if (slash > 0 && bracket > slash) {
                        return str.substring(slash + 1, bracket);
                    }
                    return "unknown";
                })
                .map(path -> path.replace('_', ' ').toUpperCase())
                .map(name -> {
                    // Clean up common biome names for display
                    if (name.contains("PLAINS")) return "PLAINS";
                    if (name.contains("FOREST")) return "FOREST";
                    if (name.contains("DESERT")) return "DESERT";
                    if (name.contains("MOUNTAIN")) return "MOUNTAINS";
                    if (name.contains("OCEAN")) return "OCEAN";
                    if (name.contains("RIVER")) return "RIVER";
                    if (name.contains("WASTELAND")) return "WASTELAND";
                    if (name.contains("CRYOGENIC")) return "CRYO RUINS";
                    return name.length() > 12 ? name.substring(0, 12) : name;
                })
                .orElse("UNKNOWN");
        g.text(mc.font, "ENV:", x + 4, y, COL_TEXT_DIM, false);
        g.text(mc.font, biomeName, x + w - mc.font.width(biomeName) - 4, y, COL_TEXT, false);
        y += 12;

        // Radiation level
        float rad = survival.getRadiationLevel();
        String radLevel = rad < 25 ? "LOW" : rad < 50 ? "MODERATE" : rad < 75 ? "HIGH" : "CRITICAL";
        int radColor = rad < 25 ? COL_SUCCESS : rad < 50 ? COL_WARNING : COL_DANGER;
        g.text(mc.font, "RADIATION:", x + 4, y, COL_TEXT_DIM, false);
        g.text(mc.font, radLevel, x + w - mc.font.width(radLevel) - 4, y, radColor, false);
        y += 12;

        // Toxic Air status
        boolean toxicAir = survival.isToxicAirActive();
        String toxicStatus = survival.isSafeZone() ? "SCRUBBED" : toxicAir ? "ACTIVE" : "CLEAR";
        int toxicColor = toxicAir ? COL_DANGER : COL_SUCCESS;
        g.text(mc.font, "TOXIC AIR:", x + 4, y, COL_TEXT_DIM, false);
        g.text(mc.font, toxicStatus, x + w - mc.font.width(toxicStatus) - 4, y, toxicColor, false);
        y += 12;

        // Weather
        String weather = "CLEAR";
        int weatherColor = COL_SUCCESS;
        EnvironmentalEventStatus eventStatus = currentEventStatus();
        if (eventStatus.active()) {
            weather = eventStatus.label();
            weatherColor = eventStatus.hudColor();
        } else if (player.level().isThundering()) {
            weather = "STORM";
            weatherColor = COL_DANGER;
        } else if (player.level().isRaining()) {
            weather = "RAIN";
            weatherColor = COL_WARNING;
        }
        g.text(mc.font, "WEATHER:", x + 4, y, COL_TEXT_DIM, false);
        g.text(mc.font, weather, x + w - mc.font.width(weather) - 4, y, weatherColor, false);
        y += 12;
        if (eventStatus.active()) {
            String eventMeta = "T-" + eventStatus.remainingSeconds() + "s I" + eventStatus.intensityText();
            g.text(mc.font, "EVENT:", x + 4, y, COL_TEXT_DIM, false);
            g.text(mc.font, eventMeta, x + w - mc.font.width(eventMeta) - 4, y, weatherColor, false);
            y += 12;
        }

        // Temperature
        String tempStr = coldData.getTemperature() + "°C";
        g.text(mc.font, "TEMP:", x + 4, y, COL_TEXT_DIM, false);
        g.text(mc.font, tempStr, x + w - mc.font.width(tempStr) - 4, y, COL_TEXT, false);
        y += 12;

        // Drone status
        boolean hasDrone = hasActiveDrone(player);
        String droneStatus = hasDrone ? "ONLINE" : "OFFLINE";
        int droneColor = hasDrone ? COL_SUCCESS : COL_TEXT_DIM;
        g.text(mc.font, "DRONE:", x + 4, y, COL_TEXT_DIM, false);
        g.text(mc.font, droneStatus, x + w - mc.font.width(droneStatus) - 4, y, droneColor, false);
    }

    // Cache for drone status to avoid checking every frame
    private static final long DRONE_STATUS_CACHE_TICKS = 100L;
    private static UUID cachedDronePlayer = null;
    private static String cachedDroneDimension = "";
    private static long lastDroneCheckTick = Long.MIN_VALUE;
    private static boolean cachedDroneStatus = false;
    private static final long RADAR_THREAT_CACHE_TICKS = 20L;
    private static UUID cachedThreatPlayer = null;
    private static String cachedThreatDimension = "";
    private static BlockPos cachedThreatBlock = BlockPos.ZERO;
    private static long lastThreatScanTick = Long.MIN_VALUE;
    private static List<Vec3> cachedThreatPositions = List.of();

    private static boolean hasActiveDrone(Player player) {
        long now = player.level().getGameTime();
        UUID playerId = player.getUUID();
        String dimension = player.level().dimension().toString();
        if (playerId.equals(cachedDronePlayer)
                && dimension.equals(cachedDroneDimension)
                && now >= lastDroneCheckTick
                && now - lastDroneCheckTick < DRONE_STATUS_CACHE_TICKS) {
            return cachedDroneStatus;
        }
        cachedDronePlayer = playerId;
        cachedDroneDimension = dimension;
        lastDroneCheckTick = now;

        // Check for ScoutDrone in vicinity - this works on both sides
        var level = player.level();
        var aabb = player.getBoundingBox().inflate(64.0);
        var drones = level.getEntitiesOfClass(ScoutDrone.class, aabb, drone ->
            drone.getOwnerUUID() != null && drone.getOwnerUUID().equals(player.getUUID())
        );
        cachedDroneStatus = !drones.isEmpty();
        return cachedDroneStatus;
    }

    private static void renderActiveEffects(GuiGraphicsExtractor g, int x, int y, Player player) {
        Minecraft mc = Minecraft.getInstance();
        int w = 140;

        // Panel background with improved styling
        int h = 70;
        g.fill(x, y, x + w, y + h, COL_BG_PANEL);
        // Inner shadow
        g.fill(x, y, x + w, y + 1, 0xFF0A1018);
        g.fill(x, y, x + 1, y + h, 0xFF0A1018);
        // Outer border
        g.fill(x, y, x + w, y + 1, COL_PRIMARY);
        g.fill(x, y + h - 1, x + w, y + h, COL_PRIMARY);
        g.fill(x, y, x + 1, y + h, COL_PRIMARY);
        g.fill(x + w - 1, y, x + w, y + h, COL_PRIMARY);
        // Corner accent
        g.fill(x, y, x + 8, y + 2, COL_PRIMARY);

        // Header
        g.text(mc.font, "ACTIVE EFFECTS", x + 4, y + 3, COL_PRIMARY, false);
        y += 14;

        // Get active effects
        var effects = new ArrayList<MobEffectInstance>(player.getActiveEffects());
        if (effects.isEmpty()) {
            g.text(mc.font, "No active effects", x + 4, y, COL_TEXT_DIM, false);
            return;
        }

        // Show up to 3 effects
        int shown = 0;
        for (MobEffectInstance effect : effects) {
            if (shown >= 3) break;
            String name = effect.getEffect().getRegisteredName();
            name = name.substring(name.lastIndexOf(':') + 1).toUpperCase().replace('_', ' ');
            if (name.length() > 12) name = name.substring(0, 12);

            int duration = effect.getDuration() / 20; // Convert ticks to seconds
            String time = String.format("%d:%02d", duration / 60, duration % 60);

            g.text(mc.font, name, x + 4, y, COL_TEXT, false);
            g.text(mc.font, time, x + w - mc.font.width(time) - 4, y, COL_TEXT_DIM, false);
            y += 11;
            shown++;
        }
    }

    private static void renderMiniRadar(GuiGraphicsExtractor g, int x, int y, Player player) {
        Minecraft mc = Minecraft.getInstance();
        int size = 74;

        // Radar background with improved styling
        g.fill(x, y, x + size, y + size, COL_BG_PANEL);
        // Inner shadow for depth
        g.fill(x, y, x + size, y + 1, 0xFF0A1018);
        g.fill(x, y, x + 1, y + size, 0xFF0A1018);
        // Outer border
        g.fill(x, y, x + size, y + 1, COL_PRIMARY);
        g.fill(x, y + size - 1, x + size, y + size, COL_PRIMARY);
        g.fill(x, y, x + 1, y + size, COL_PRIMARY);
        g.fill(x + size - 1, y, x + size, y + size, COL_PRIMARY);
        // Corner accents
        g.fill(x, y, x + 6, y + 2, COL_PRIMARY);
        g.fill(x, y, x + 2, y + 6, COL_PRIMARY);

        int cx = x + size / 2;
        int cy = y + size / 2;

        // Cardinal directions
        g.text(mc.font, "N", cx - 3, y + 2, COL_TEXT_DIM, false);
        g.text(mc.font, "S", cx - 3, y + size - 10, COL_TEXT_DIM, false);
        g.text(mc.font, "W", x + 2, cy - 4, COL_TEXT_DIM, false);
        g.text(mc.font, "E", x + size - 8, cy - 4, COL_TEXT_DIM, false);

        // Crosshair lines
        g.fill(cx, y + 12, cx + 1, y + size - 12, 0x332980FF);
        g.fill(x + 12, cy, x + size - 12, cy + 1, 0x332980FF);

        // Player indicator (triangle)
        g.fill(cx, cy - 3, cx + 1, cy + 3, COL_SUCCESS);
        g.fill(cx - 1, cy - 2, cx + 2, cy, COL_SUCCESS);
        g.fill(cx - 2, cy - 1, cx + 3, cy - 1, COL_SUCCESS);

        Vec3 playerPos = player.position();
        List<Vec3> threats = radarThreatPositions(player);

        for (Vec3 threatPos : threats) {
            double dx = threatPos.x - playerPos.x;
            double dz = threatPos.z - playerPos.z;

            // Scale and clamp to radar
            int dotX = cx + (int)(dx * 0.8);
            int dotY = cy + (int)(dz * 0.8);
            dotX = Math.max(x + 8, Math.min(x + size - 8, dotX));
            dotY = Math.max(y + 12, Math.min(y + size - 12, dotY));

            g.fill(dotX - 1, dotY - 1, dotX + 2, dotY + 2, COL_DANGER);
        }
    }

    private static List<Vec3> radarThreatPositions(Player player) {
        long now = player.level().getGameTime();
        UUID playerId = player.getUUID();
        String dimension = player.level().dimension().toString();
        BlockPos block = player.blockPosition();
        if (playerId.equals(cachedThreatPlayer)
                && dimension.equals(cachedThreatDimension)
                && block.equals(cachedThreatBlock)
                && now >= lastThreatScanTick
                && now - lastThreatScanTick < RADAR_THREAT_CACHE_TICKS) {
            return cachedThreatPositions;
        }

        List<Monster> threats = player.level().getEntitiesOfClass(Monster.class,
                player.getBoundingBox().inflate(32.0));
        List<Vec3> positions = new ArrayList<>();
        for (Monster threat : threats) {
            if (positions.size() >= 5) break;
            positions.add(threat.position());
        }
        cachedThreatPlayer = playerId;
        cachedThreatDimension = dimension;
        cachedThreatBlock = block;
        lastThreatScanTick = now;
        cachedThreatPositions = List.copyOf(positions);
        return cachedThreatPositions;
    }

    private static void renderExtendedMissionTracker(GuiGraphicsExtractor g, int x, int y, QuestData quest) {
        Minecraft mc = Minecraft.getInstance();
        int w = 214, h = 85;

        // Panel background with improved styling
        g.fill(x, y, x + w, y + h, COL_BG_PANEL);
        // Inner shadow
        g.fill(x, y, x + w, y + 1, 0xFF0A1018);
        g.fill(x, y, x + 1, y + h, 0xFF0A1018);
        // Outer border
        g.fill(x, y, x + w, y + 1, COL_PRIMARY);
        g.fill(x, y + h - 1, x + w, y + h, COL_PRIMARY);
        g.fill(x, y, x + 1, y + h, COL_PRIMARY);
        g.fill(x + w - 1, y, x + w, y + h, COL_PRIMARY);
        // Corner accent
        g.fill(x, y, x + 8, y + 2, COL_PRIMARY);

        // Header
        g.text(mc.font, "MISSION TRACKER", x + 4, y + 3, COL_PRIMARY, false);
        y += 14;

        Mission current = MissionRegistry.getMission(quest.getCurrentPhase(), quest.getCurrentMissionIndex());
        if (current == null) {
            g.text(mc.font, "All missions complete", x + 4, y, COL_SUCCESS, false);
            return;
        }
        Player player = mc.player;
        MissionUxSummary summary = player == null
                ? null
                : MissionUxSummary.forHud(player, quest, current);

        // Phase label
        int totalPhases = Math.max(1, MissionRegistry.getPhaseCount());
        String phaseLabel = "PHASE " + (quest.getCurrentPhase() + 1) + "/" + totalPhases;
        g.text(mc.font, phaseLabel, x + 4, y, COL_TEXT_DIM, false);
        y += 12;

        // Mission objective with checkboxes style
        String full = summary == null ? current.objectiveText() : summary.shortTitle();
        String line1 = mc.font.plainSubstrByWidth(full, w - 12);
        g.text(mc.font, "[ ] " + line1, x + 4, y, COL_TEXT, false);
        y += 11;

        String step = summary == null ? current.echoMessage() : summary.nextStep();
        if (!step.isBlank()) {
            String line2 = mc.font.plainSubstrByWidth(step, w - 12);
            g.text(mc.font, line2, x + 4, y, COL_TEXT_DIM, false);
        }
    }

    private static void renderTimeAndDay(GuiGraphicsExtractor g, int x, int y, Player player) {
        Minecraft mc = Minecraft.getInstance();
        int w = 80, h = 50;

        // Panel background with improved styling
        g.fill(x, y, x + w, y + h, COL_BG_PANEL);
        // Inner shadow
        g.fill(x, y, x + w, y + 1, 0xFF0A1018);
        g.fill(x, y, x + 1, y + h, 0xFF0A1018);
        // Outer border
        g.fill(x, y, x + w, y + 1, COL_PRIMARY);
        g.fill(x, y + h - 1, x + w, y + h, COL_PRIMARY);
        g.fill(x, y, x + 1, y + h, COL_PRIMARY);
        g.fill(x + w - 1, y, x + w, y + h, COL_PRIMARY);
        // Corner accent
        g.fill(x, y, x + 6, y + 2, COL_PRIMARY);

        // Calculate day and time from client level
        var clientLevel = Minecraft.getInstance().level;
        long dayTime = clientLevel != null ? clientLevel.getGameTime() : 0;
        int day = (int)(dayTime / 24000L) + 1;
        int timeOfDay = (int)(dayTime % 24000L);

        // Convert to hours:minutes (6am = 0, Minecraft time offset)
        int hours = ((timeOfDay / 1000) + 6) % 24;
        int minutes = (int)((timeOfDay % 1000) * 60 / 1000);

        // Day label
        g.text(mc.font, "DAY " + day, x + 4, y + 4, COL_TEXT, false);

        // Time display
        String timeStr = String.format("%02d:%02d %s", hours % 12 == 0 ? 12 : hours % 12, minutes, hours < 12 ? "AM" : "PM");
        g.text(mc.font, timeStr, x + 4, y + 18, COL_PRIMARY, false);

        // Sun/Moon indicator
        boolean isDay = timeOfDay < 13000;
        String indicator = isDay ? "DAY" : "NIGHT";
        int indicatorColor = isDay ? COL_WARNING : COL_TEXT_DIM;
        g.text(mc.font, indicator, x + w - 16, y + 18, indicatorColor, false);
    }

    private static void renderCenterWarnings(GuiGraphicsExtractor g, int cx, int cy,
                                             SurvivalData survival, MutationData mutations) {
        Minecraft mc = Minecraft.getInstance();

        HazardAlert alert = primaryCenterAlert(buildHazardAlerts(survival, mutations));
        if (alert == null) {
            return;
        }

        // Warning box
        int w = centerWarningWidth(mc, alert);
        String[] subtitleLines = alert.subtitle().split("\n", 3);
        int h = Math.max(50, 30 + subtitleLines.length * 10);
        int x = cx - w / 2;
        int y = cy - h / 2;

        if (alert.pulseCenterBorder()) {
            int alpha = 18 + (int)((Math.sin(System.currentTimeMillis() / 180.0) + 1.0) * 16);
            int pulse = warningOverlay(alpha, alert.color());
            g.fill(x - 2, y - 2, x + w + 2, y - 1, pulse);
            g.fill(x - 2, y + h + 1, x + w + 2, y + h + 2, pulse);
            g.fill(x - 2, y - 1, x - 1, y + h + 1, pulse);
            g.fill(x + w + 1, y - 1, x + w + 2, y + h + 1, pulse);
        }

        // Warning background
        g.fill(x, y, x + w, y + h, COL_BG_DARK | 0xAA000000);
        g.fill(x, y, x + w, y + 1, alert.color());
        g.fill(x, y + h - 1, x + w, y + h, alert.color());
        g.fill(x, y, x + 1, y + h, alert.color());
        g.fill(x + w - 1, y, x + w, y + h, alert.color());

        renderHazardIcon(g, alert, x + 7, y + 13, 24, 0xF0);

        // Title
        g.text(mc.font, fitText(mc, alert.title(), w - 42), x + 36, y + 8, alert.color(), false);

        // Subtitle lines
        int ly = y + 22;
        for (String line : subtitleLines) {
            g.text(mc.font, fitText(mc, line, w - 42), x + 36, ly, COL_TEXT, false);
            ly += 10;
        }
    }

    private static int centerWarningWidth(Minecraft mc, HazardAlert alert) {
        int textWidth = mc.font.width(alert.title());
        for (String line : alert.subtitle().split("\n", 3)) {
            textWidth = Math.max(textWidth, mc.font.width(line));
        }
        return Math.max(160, Math.min(220, textWidth + 50));
    }

    private static EnvironmentalEventStatus currentEventStatus() {
        Minecraft mc = Minecraft.getInstance();
        long gameTime = mc.level == null ? 0L : mc.level.getGameTime();
        return HudState.getEnvironmentalEventStatus(gameTime);
    }

    private static String eventChipLabel(EnvironmentalEventType type) {
        return switch (type) {
            case RADIATION_STORM -> "RAD";
            case TOXIC_STORM -> "ACID";
            case BLACKOUT -> "GRID";
            case ASH_STORM -> "ASH";
            case CRYO_FRONT -> "CRYO";
            case NEXUS_SURGE -> "SURGE";
            default -> "EVENT";
        };
    }

    private static boolean terminalInstalled() {
        return EchoRuntimeModules.isLoaded("echoterminal");
    }
}
