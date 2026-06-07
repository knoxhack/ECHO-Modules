package com.knoxhack.echoashfallprotocol.client.hud;

import com.knoxhack.echoashfallprotocol.boss.BossHudProfile;
import com.knoxhack.echoashfallprotocol.boss.BossHudProfiles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

public final class BossHudOverlay {
    private static final int MAX_PANEL_W = 300;
    private static final int MIN_PANEL_W = 190;
    private static final int PANEL_H = 46;
    private static final int COMPACT_PANEL_H = 40;
    private static final int HEALTH_MAX_W = 176;
    private static final int HEALTH_H = 7;
    private static final int COMPASS_W = 240;
    private static final int COMPASS_H = 34;
    private static final Map<String, Float> DISPLAY_HEALTH = new HashMap<>();

    private BossHudOverlay() {
    }

    public static void onBossEvent(Object event) {
        BossEventView view = BossEventView.from(event);
        if (view == null) {
            return;
        }
        String title = view.bossEvent().getName().getString();
        Optional<BossHudProfile> maybeProfile = BossHudProfiles.byTitle(title);
        if (maybeProfile.isEmpty()) {
            return;
        }

        BossHudProfile profile = maybeProfile.get();
        view.setCanceled(true);
        int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int panelW = Math.min(MAX_PANEL_W, Math.max(120, screenW - 12));
        boolean compact = panelW < 236;
        int panelH = compact ? COMPACT_PANEL_H : PANEL_H;
        view.setIncrement(panelH + 8);
        int centerX = view.x() + 91;
        int panelX = Mth.clamp(centerX - panelW / 2, 6, Math.max(6, screenW - panelW - 6));
        renderBossPanel(view.graphics(), view.bossEvent(), profile, panelX, view.y() - 7,
                panelW, panelH, compact);
    }

    private record BossEventView(
            Object event,
            GuiGraphicsExtractor graphics,
            LerpingBossEvent bossEvent,
            int x,
            int y
    ) {
        private static BossEventView from(Object event) {
            if (event == null) {
                return null;
            }
            try {
                Object graphics = event.getClass().getMethod("getGuiGraphics").invoke(event);
                Object bossEvent = event.getClass().getMethod("getBossEvent").invoke(event);
                Object x = event.getClass().getMethod("getX").invoke(event);
                Object y = event.getClass().getMethod("getY").invoke(event);
                if (graphics instanceof GuiGraphicsExtractor extractor
                        && bossEvent instanceof LerpingBossEvent lerpingBossEvent
                        && x instanceof Number xNumber
                        && y instanceof Number yNumber) {
                    return new BossEventView(event, extractor, lerpingBossEvent,
                            xNumber.intValue(), yNumber.intValue());
                }
            } catch (ReflectiveOperationException | RuntimeException exception) {
                return null;
            }
            return null;
        }

        private void setCanceled(boolean canceled) {
            invoke("setCanceled", boolean.class, canceled);
        }

        private void setIncrement(int increment) {
            invoke("setIncrement", int.class, increment);
        }

        private void invoke(String methodName, Class<?> parameterType, Object value) {
            try {
                event.getClass().getMethod(methodName, parameterType).invoke(event, value);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
    }

    public static void renderCompass(GuiGraphicsExtractor graphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        HudState.BossTarget target = HudState.getBossTarget();
        if (player == null || mc.level == null || target == null || !target.active()) {
            return;
        }
        if (!mc.level.dimension().identifier().toString().equals(target.dimension())) {
            return;
        }
        if (target.isLiveBoss()) {
            return;
        }

        int x = mc.getWindow().getGuiScaledWidth() / 2 - COMPASS_W / 2;
        int y = 34;
        renderCompassPanel(graphics, player, target, x, y, COMPASS_W, COMPASS_H);
    }

    private static void renderBossPanel(GuiGraphicsExtractor g, LerpingBossEvent event, BossHudProfile profile,
                                        int x, int y, int panelW, int panelH, boolean compact) {
        Minecraft mc = Minecraft.getInstance();
        float pct = smoothHealth(profile.bossId(), Mth.clamp(event.getProgress(), 0.0F, 1.0F));
        int accent = profile.accentColor();
        Optional<HudState.BossTarget> target = matchingLiveTarget(profile);
        int phase = target.map(HudState.BossTarget::phase)
                .filter(value -> value > 0)
                .orElseGet(() -> profile.phaseForHealth(pct));
        int healthW = Math.max(78, Math.min(HEALTH_MAX_W, panelW - 106));
        int fillW = Math.max(0, Math.round((healthW - 4) * pct));
        boolean finalPhase = phase >= 3;
        long tick = mc.level == null ? 0L : mc.level.getGameTime();
        long pulse = tick / 3L;
        int warning = finalPhase && pulse % 2L == 0L ? 0xFFFF405C : accent;

        g.fill(x + 2, y + 2, x + panelW + 2, y + panelH + 2, 0x77000000);
        g.fill(x, y, x + panelW, y + panelH, 0xDD061018);
        g.fill(x, y, x + panelW, y + 2, warning);
        g.fill(x, y + panelH - 2, x + panelW, y + panelH, withAlpha(accent, 0xAA));

        int sweep = (int) ((tick * 3L) % (panelW + 64)) - 64;
        int sweepStart = Math.max(0, sweep);
        int sweepEnd = Math.min(panelW, sweep + 42);
        if (sweepEnd > sweepStart) {
            g.fill(x + sweepStart, y + 2, x + sweepEnd, y + panelH - 2,
                    withAlpha(accent, finalPhase ? 0x30 : 0x20));
        }

        String eyebrow = "ECHO THREAT // " + profile.category().name();
        g.text(mc.font, mc.font.plainSubstrByWidth(eyebrow, compact ? 92 : 126), x + 8, y + 5, accent, false);
        String phaseLabel = finalPhase ? profile.phaseWarningLabel() : "PHASE " + Math.max(1, phase);
        String phaseText = mc.font.plainSubstrByWidth(phaseLabel, compact ? 76 : 118);
        g.text(mc.font, phaseText,
                x + panelW - 8 - mc.font.width(phaseText), y + 5,
                finalPhase ? 0xFFFF8FA3 : 0xFFB9CAD8, false);

        String title = mc.font.plainSubstrByWidth(profile.title(), panelW - 84);
        g.text(mc.font, title, x + 8, y + 16, 0xFFEAF7FF, false);
        String health = Math.round(pct * 100.0F) + "%";
        g.text(mc.font, health, x + panelW - 8 - mc.font.width(health), y + 16, 0xFFEAF7FF, false);

        int barX = x + 8;
        int barY = y + 28;
        g.fill(barX, barY, barX + healthW, barY + HEALTH_H, 0xFF101824);
        g.fill(barX + 2, barY + 2, barX + healthW - 2, barY + HEALTH_H - 1, 0xFF263442);
        if (fillW > 0) {
            g.fill(barX + 2, barY + 2, barX + 2 + fillW, barY + HEALTH_H - 1, finalPhase ? warning : accent);
        }

        drawPhasePips(g, x + panelW - 50, y + 31, phase, finalPhase ? warning : accent);
        String mechanic = mc.font.plainSubstrByWidth(profile.subtitle(), compact ? panelW - 16 : 150);
        g.text(mc.font, mechanic, x + 8, y + 37, finalPhase ? 0xFFFFC2CA : 0xFF98AFC4, false);

        String bearing = target.map(value -> distanceLine(mc.player, value)).orElse(profile.counterplayLabel());
        if (!compact) {
            String trimmedBearing = mc.font.plainSubstrByWidth(bearing, 108);
            g.text(mc.font, trimmedBearing, x + panelW - 8 - mc.font.width(trimmedBearing), y + 37,
                    target.isPresent() ? accent : 0xFF8CA7B5, false);
        }
    }

    private static float smoothHealth(String bossId, float target) {
        Float current = DISPLAY_HEALTH.get(bossId);
        if (current == null || Math.abs(current - target) > 0.35F) {
            DISPLAY_HEALTH.put(bossId, target);
            return target;
        }
        float next = Mth.lerp(0.18F, current, target);
        DISPLAY_HEALTH.put(bossId, next);
        return next;
    }

    private static void drawPhasePips(GuiGraphicsExtractor g, int x, int y, int phase, int accent) {
        for (int i = 1; i <= 3; i++) {
            int color = i <= phase ? accent : 0xFF334352;
            g.fill(x + (i - 1) * 14, y, x + (i - 1) * 14 + 10, y + 3, color);
        }
    }

    private static Optional<HudState.BossTarget> matchingLiveTarget(BossHudProfile profile) {
        Minecraft mc = Minecraft.getInstance();
        HudState.BossTarget target = HudState.getBossTarget();
        if (mc.level == null || target == null || !target.active() || !target.isLiveBoss()) {
            return Optional.empty();
        }
        if (!target.bossId().equals(profile.bossId())) {
            return Optional.empty();
        }
        if (!mc.level.dimension().identifier().toString().equals(target.dimension())) {
            return Optional.empty();
        }
        return Optional.of(target);
    }

    private static void renderCompassPanel(GuiGraphicsExtractor g, Player player, HudState.BossTarget target,
                                           int x, int y, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        int accent = 0xFF000000 | (target.accentColor() & 0x00FFFFFF);
        g.fill(x + 2, y + 2, x + w + 2, y + h + 2, 0x77000000);
        g.fill(x, y, x + w, y + h, 0xCC071018);
        g.fill(x, y, x + w, y + 2, accent);

        int railY = y + 18;
        int railX = x + 12;
        int railW = w - 24;
        g.fill(railX, railY, railX + railW, railY + 2, 0xFF263442);
        g.fill(railX + railW / 2 - 1, railY - 4, railX + railW / 2 + 1, railY + 6, 0xFF90A6B8);

        float relative = relativeAngle(player, target);
        int needleX = railX + railW / 2 + Math.round(Mth.clamp(relative / 90.0F, -1.0F, 1.0F) * (railW / 2 - 5));
        g.fill(needleX - 2, railY - 6, needleX + 3, railY + 8, accent);
        g.fill(needleX - 4, railY - 2, needleX + 5, railY + 3, 0xAAFFFFFF);

        String type = "ENTRANCE".equals(target.targetKind()) ? "ENTRANCE" : target.targetKind();
        String label = mc.font.plainSubstrByWidth("ECHO COMPASS // " + type, w - 10);
        g.text(mc.font, label, x + 6, y + 5, accent, false);
        String title = mc.font.plainSubstrByWidth(target.compassLabel() + " / " + target.title(), w - 86);
        g.text(mc.font, title, x + 6, y + 24, 0xFFEAF7FF, false);
        String distance = distanceLine(player, target);
        g.text(mc.font, distance, x + w - 6 - mc.font.width(distance), y + 24, 0xFFB9CAD8, false);
    }

    private static float relativeAngle(Player player, HudState.BossTarget target) {
        double dx = (target.x() + 0.5D) - player.getX();
        double dz = (target.z() + 0.5D) - player.getZ();
        float targetYaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        return Mth.wrapDegrees(targetYaw - player.getYRot());
    }

    private static String distanceLine(Player player, HudState.BossTarget target) {
        if (player == null) {
            return "";
        }
        double dx = (target.x() + 0.5D) - player.getX();
        double dy = target.y() - player.getY();
        double dz = (target.z() + 0.5D) - player.getZ();
        int meters = (int) Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));
        String yDelta = verticalDelta(dy);
        return bearingFromRelative(relativeAngle(player, target)) + " " + meters + "m " + yDelta;
    }

    private static String bearingFromRelative(float relative) {
        float abs = Math.abs(relative);
        if (abs <= 10.0F) {
            return "AHEAD";
        }
        if (abs >= 158.0F) {
            return "BEHIND";
        }
        return (relative < 0.0F ? "LEFT " : "RIGHT ") + Math.round(abs);
    }

    private static String verticalDelta(double dy) {
        long rounded = Math.round(dy);
        if (rounded > 0) {
            return "UP +" + rounded;
        }
        if (rounded < 0) {
            return "DOWN " + Math.abs(rounded);
        }
        return "LEVEL";
    }

    private static int withAlpha(int color, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }
}
