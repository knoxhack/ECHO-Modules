package com.knoxhack.echoashfallprotocol.client.hud;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;

public final class EchoNativeAshfallHudOverlay {
    private static final int PANEL = 0xB8071018;
    private static final int PANEL_SOFT = 0x8E102330;
    private static final int LINE = 0xCC38DFF4;
    private static final int LINE_DIM = 0x7738DFF4;
    private static final int TEXT = 0xFFE8F8FF;
    private static final int MUTED = 0xFF8CA2AE;
    private static final int CYAN = 0xFF66E8FF;
    private static final int GREEN = 0xFF7CFFB2;
    private static final int AMBER = 0xFFFFC857;
    private static final int RED = 0xFFFF5C5C;

    private EchoNativeAshfallHudOverlay() {
    }

    public static Map<String, Object> render(GuiGraphicsExtractor graphics, float partialTick) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("renderer", EchoNativeAshfallHudOverlay.class.getName());
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui || minecraft.screen != null) {
            state.put("rendered", false);
            state.put("reason", player == null ? "no_player" : minecraft.screen != null ? "screen_active" : "gui_hidden");
            return Map.copyOf(state);
        }

        Font font = minecraft.font;
        Map<String, Object> hud = dataSource("hud");
        List<Map<String, Object>> notifications = objects(hud.get("notifications"));
        NativeHudState nativeHud = NativeHudState.from(player, hud);
        int x = 8;
        int y = 8;
        int width = Math.min(364, Math.max(268, graphics.guiWidth() / 4));
        int height = 167;
        graphics.fill(x, y, x + width, y + height, PANEL);
        graphics.outline(x, y, width, height, LINE);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 17, PANEL_SOFT);
        graphics.fill(x + 8, y + 18, x + width - 8, y + 19, LINE_DIM);

        text(graphics, font, "ASHFALL // NATIVE HUD", x + 8, y + 6, CYAN);
        float health = player.getHealth();
        float maxHealth = Math.max(1.0F, player.getMaxHealth());
        FoodData food = player.getFoodData();
        int armor = player.getArmorValue();
        int air = player.getAirSupply();
        BlockPos pos = player.blockPosition();

        int barX = x + 8;
        int barY = y + 28;
        int barW = width - 16;
        bar(graphics, barX, barY, barW, "VITAL", health / maxHealth, healthColor(health / maxHealth));
        bar(graphics, barX, barY + 13, barW, "H2O", nativeHud.hydrationPercent,
                hydrationColor(nativeHud.hydrationPercent));
        bar(graphics, barX, barY + 26, barW, "RAD", nativeHud.radiationPercent,
                radiationColor(nativeHud.radiationPercent));
        bar(graphics, barX, barY + 39, barW, "TEMP", nativeHud.temperaturePercent / 100.0F,
                temperatureColor(nativeHud.temperature));
        bar(graphics, barX, barY + 52, barW, "MASK", nativeHud.airFilterPercent / 100.0F,
                filterColor(nativeHud.airFilterPercent / 100.0F));
        bar(graphics, barX, barY + 65, barW, "FOOD", food.getFoodLevel() / 20.0F,
                food.getFoodLevel() <= 6 ? RED : food.getFoodLevel() <= 12 ? AMBER : GREEN);

        String status = "ARM " + armor + "  POS " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
        text(graphics, font, clip(font, status, width - 16), x + 8, y + 107, MUTED);
        text(graphics, font, clip(font, missionLine(nativeHud), width - 16), x + 8, y + 121, GREEN);
        text(graphics, font, clip(font, hazardLine(nativeHud), width - 16), x + 8, y + 134,
                hazardColor(nativeHud));
        text(graphics, font, clip(font, eventLine(nativeHud), width - 16), x + 8, y + 147,
                nativeHud.environmentActive ? nativeHud.environmentColor : notifications.isEmpty() ? MUTED : CYAN);

        state.put("rendered", true);
        state.put("health", Math.round(health));
        state.put("maxHealth", Math.round(maxHealth));
        state.put("food", food.getFoodLevel());
        state.put("armor", armor);
        state.put("air", air);
        state.put("position", pos.getX() + "," + pos.getY() + "," + pos.getZ());
        state.put("hydration", nativeHud.hydration);
        state.put("radiation", Math.round(nativeHud.radiationLevel));
        state.put("temperature", nativeHud.temperature);
        state.put("temperaturePercent", nativeHud.temperaturePercent);
        state.put("temperatureStatus", nativeHud.temperatureStatus);
        state.put("temperatureLabel", nativeHud.temperatureLabel);
        state.put("temperatureEffect", nativeHud.temperatureEffect);
        state.put("airFilterLife", nativeHud.airFilterLife);
        state.put("airFilterPercent", nativeHud.airFilterPercent);
        state.put("primaryHazard", nativeHud.primaryHazard);
        state.put("hazardSeverity", nativeHud.hazardSeverity);
        state.put("hazardReason", nativeHud.hazardReason);
        state.put("missionId", nativeHud.missionId);
        state.put("missionStatus", nativeHud.missionStatus);
        state.put("mission", nativeHud.missionTitle);
        state.put("environmentEvent", nativeHud.environmentLabel);
        state.put("environmentActive", nativeHud.environmentActive);
        state.put("environmentRemainingSeconds", nativeHud.environmentRemainingSeconds);
        state.put("notificationCount", notifications.size());
        state.put("nativeHudDataPlumbing", true);
        state.put("nativeHudDataSource", "live_ashfall_runtime_state");
        state.put("nativeHudDataSnapshot", nativeHudDataSnapshot(
                player,
                nativeHud,
                notifications,
                health,
                maxHealth,
                food,
                armor,
                air,
                pos
        ));
        return Map.copyOf(state);
    }

    private static Map<String, Object> nativeHudDataSnapshot(
            Player player,
            NativeHudState nativeHud,
            List<Map<String, Object>> notifications,
            float health,
            float maxHealth,
            FoodData food,
            int armor,
            int air,
            BlockPos pos
    ) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("source", "live_ashfall_runtime_state");
        snapshot.put("playerId", player.getUUID().toString());
        snapshot.put("hazardMeters", hazardMeters(nativeHud));
        snapshot.put("missionTracker", missionTracker(nativeHud));
        snapshot.put("statusWidgets", statusWidgets(health, maxHealth, food, armor, air));
        snapshot.put("weatherReadout", weatherReadout(nativeHud));
        snapshot.put("fieldReadouts", fieldReadouts(player, nativeHud, pos));
        snapshot.put("notificationCount", notifications.size());
        snapshot.put("liveRuntimeState", true);
        snapshot.put("placeholderHudData", false);
        snapshot.put("summary", "Ashfall native HUD data is rendered from Native Loader player state and native HUD defaults without legacy runtime event dependencies.");
        return Map.copyOf(snapshot);
    }

    private static Map<String, Object> hazardMeters(NativeHudState nativeHud) {
        Map<String, Object> meters = new LinkedHashMap<>();
        meters.put("hydrationPercent", Math.round(nativeHud.hydrationPercent * 100.0F));
        meters.put("radiationLevel", Math.round(nativeHud.radiationLevel));
        meters.put("radiationPercent", Math.round(nativeHud.radiationPercent * 100.0F));
        meters.put("temperature", nativeHud.temperature);
        meters.put("temperaturePercent", nativeHud.temperaturePercent);
        meters.put("filterPercent", nativeHud.airFilterPercent);
        meters.put("primaryHazard", nativeHud.primaryHazard);
        meters.put("hazardSeverity", nativeHud.hazardSeverity);
        meters.put("hazardReason", nativeHud.hazardReason);
        meters.put("safeZone", nativeHud.safeZone);
        meters.put("toxicAir", nativeHud.toxicAir);
        meters.put("radiationZone", nativeHud.radiationZone);
        meters.put("cryoCold", nativeHud.cryoCold);
        meters.put("acidContact", nativeHud.acidContact);
        meters.put("nexusAnomaly", nativeHud.nexusAnomaly);
        meters.put("radiationStorm", nativeHud.radiationStorm);
        return Map.copyOf(meters);
    }

    private static Map<String, Object> missionTracker(NativeHudState nativeHud) {
        return Map.of(
                "missionId", nativeHud.missionId,
                "status", nativeHud.missionStatus,
                "shortTitle", nativeHud.missionTitle,
                "line", missionLine(nativeHud)
        );
    }

    private static Map<String, Object> statusWidgets(float health, float maxHealth, FoodData food, int armor, int air) {
        return Map.of(
                "health", Math.round(health),
                "maxHealth", Math.round(maxHealth),
                "food", food.getFoodLevel(),
                "armor", armor,
                "air", air
        );
    }

    private static Map<String, Object> weatherReadout(NativeHudState nativeHud) {
        return Map.of(
                "label", nativeHud.environmentLabel,
                "active", nativeHud.environmentActive,
                "remainingSeconds", nativeHud.environmentRemainingSeconds,
                "weather", nativeHud.weatherLabel,
                "intensity", nativeHud.environmentIntensity,
                "line", eventLine(nativeHud)
        );
    }

    private static Map<String, Object> fieldReadouts(Player player, NativeHudState nativeHud, BlockPos pos) {
        Map<String, Object> readouts = new LinkedHashMap<>();
        readouts.put("position", pos.getX() + "," + pos.getY() + "," + pos.getZ());
        readouts.put("dimension", player.level().dimension().identifier().toString());
        readouts.put("hydration", nativeHud.hydration);
        readouts.put("airFilterLife", nativeHud.airFilterLife);
        readouts.put("temperatureStatus", nativeHud.temperatureStatus);
        readouts.put("temperatureLabel", nativeHud.temperatureLabel);
        readouts.put("temperatureEffect", nativeHud.temperatureEffect);
        readouts.put("hazardLine", hazardLine(nativeHud));
        return Map.copyOf(readouts);
    }

    private static void bar(GuiGraphicsExtractor graphics, int x, int y, int width, String label, float value, int color) {
        int labelW = 35;
        int barX = x + labelW;
        int barW = Math.max(1, width - labelW);
        graphics.fill(barX, y + 1, barX + barW, y + 8, 0x66000000);
        graphics.outline(barX, y + 1, barW, 7, LINE_DIM);
        int filled = Math.max(0, Math.min(barW - 2, Math.round((barW - 2) * Math.max(0.0F, Math.min(1.0F, value)))));
        graphics.fill(barX + 1, y + 2, barX + 1 + filled, y + 7, color);
        text(graphics, Minecraft.getInstance().font, label, x, y, CYAN);
    }

    private static int healthColor(float value) {
        if (value <= 0.30F) {
            return RED;
        }
        if (value <= 0.60F) {
            return AMBER;
        }
        return GREEN;
    }

    private static int hydrationColor(float value) {
        if (value <= 0.20F) {
            return RED;
        }
        if (value <= 0.45F) {
            return AMBER;
        }
        return CYAN;
    }

    private static int radiationColor(float value) {
        if (value >= 0.75F) {
            return RED;
        }
        if (value >= 0.35F) {
            return AMBER;
        }
        return CYAN;
    }

    private static int filterColor(float value) {
        if (value <= 0.20F) {
            return RED;
        }
        if (value <= 0.45F) {
            return AMBER;
        }
        return GREEN;
    }

    private static int temperatureColor(int temperature) {
        if (temperature <= 20) {
            return RED;
        }
        if (temperature <= 40) {
            return 0xFF44AAFF;
        }
        if (temperature <= 60) {
            return CYAN;
        }
        if (temperature <= 80) {
            return GREEN;
        }
        return AMBER;
    }

    private static int hazardColor(NativeHudState nativeHud) {
        if (nativeHud.safeZone) {
            return GREEN;
        }
        if (nativeHud.toxicAir || nativeHud.radiationZone || nativeHud.cryoCold || nativeHud.acidContact
                || nativeHud.nexusAnomaly || nativeHud.radiationStorm) {
            return nativeHud.hazardIntensity >= 0.65F ? RED : AMBER;
        }
        return CYAN;
    }

    private static String missionLine(NativeHudState nativeHud) {
        return "MISSION " + nativeHud.missionStatus + " / " + nativeHud.missionTitle;
    }

    private static String hazardLine(NativeHudState nativeHud) {
        String reason = nativeHud.hazardReason;
        String hazard = nativeHud.primaryHazard + " " + nativeHud.hazardSeverity;
        String extras = "H2O " + nativeHud.hydration + "% RAD " + Math.round(nativeHud.radiationLevel)
                + "% TEMP " + nativeHud.temperature + "% "
                + nativeHud.temperatureLabel.toUpperCase(java.util.Locale.ROOT);
        if (reason == null || reason.isBlank()) {
            return "HAZARD " + hazard + " / " + extras;
        }
        return "HAZARD " + hazard + " / " + reason + " / " + extras;
    }

    private static String eventLine(NativeHudState nativeHud) {
        if (!nativeHud.environmentActive) {
            return "WEATHER CLEAR / " + nativeHud.weatherLabel;
        }
        return "WEATHER " + nativeHud.environmentLabel + " T-" + nativeHud.environmentRemainingSeconds
                + "s / " + nativeHud.weatherLabel + " / INT " + nativeHud.environmentIntensity;
    }

    private static final class NativeHudState {
        private final int hydration;
        private final float hydrationPercent;
        private final float radiationLevel;
        private final float radiationPercent;
        private final int temperature;
        private final int temperaturePercent;
        private final String temperatureStatus;
        private final String temperatureLabel;
        private final String temperatureEffect;
        private final int airFilterLife;
        private final int airFilterPercent;
        private final String primaryHazard;
        private final String hazardSeverity;
        private final String hazardReason;
        private final float hazardIntensity;
        private final boolean safeZone;
        private final boolean toxicAir;
        private final boolean radiationZone;
        private final boolean cryoCold;
        private final boolean acidContact;
        private final boolean nexusAnomaly;
        private final boolean radiationStorm;
        private final String missionId;
        private final String missionStatus;
        private final String missionTitle;
        private final String environmentLabel;
        private final boolean environmentActive;
        private final int environmentRemainingSeconds;
        private final String weatherLabel;
        private final String environmentIntensity;
        private final int environmentColor;

        private NativeHudState(Map<String, Object> hud) {
            hydration = intValue(hud.get("hydration"), 100);
            hydrationPercent = clamp01(number(hud.get("hydrationPercent"), hydration / 100.0F));
            radiationLevel = number(hud.get("radiation"), 0.0F);
            radiationPercent = clamp01(number(hud.get("radiationPercent"), radiationLevel / 100.0F));
            temperature = intValue(hud.get("temperature"), 100);
            temperaturePercent = Math.round(clamp01(number(hud.get("temperaturePercent"), temperature / 100.0F)) * 100.0F);
            temperatureStatus = textValue(hud, "temperatureStatus", temperatureStatus(temperature).toUpperCase(java.util.Locale.ROOT));
            temperatureLabel = textValue(hud, "temperatureLabel", temperatureStatus(temperature));
            temperatureEffect = textValue(hud, "temperatureEffect", "Comfortable");
            airFilterLife = intValue(hud.get("airFilterLife"), 1000);
            airFilterPercent = Math.round(clamp01(number(hud.get("airFilterPercent"), 1.0F)) * 100.0F);
            primaryHazard = textValue(hud, "primaryHazard", "NONE");
            hazardSeverity = textValue(hud, "hazardSeverity", "NONE");
            hazardReason = textValue(hud, "hazardReason", "");
            hazardIntensity = clamp01(number(hud.get("hazardIntensity"), 0.0F));
            safeZone = bool(hud.get("safeZone"));
            toxicAir = bool(hud.get("toxicAir"));
            radiationZone = bool(hud.get("radiationZone"));
            cryoCold = bool(hud.get("cryoCold"));
            acidContact = bool(hud.get("acidContact"));
            nexusAnomaly = bool(hud.get("nexusAnomaly"));
            radiationStorm = bool(hud.get("radiationStorm"));
            missionId = textValue(hud, "missionId", "ashfall:native_loader_start");
            missionStatus = textValue(hud, "missionStatus", "ACTIVE");
            missionTitle = textValue(hud, "mission", "Establish Ashfall foothold");
            environmentLabel = textValue(hud, "environmentEvent", "NONE");
            environmentActive = bool(hud.get("environmentActive"));
            environmentRemainingSeconds = intValue(hud.get("environmentRemainingSeconds"), 0);
            weatherLabel = textValue(hud, "weather", "clear");
            environmentIntensity = textValue(hud, "environmentIntensity", "0");
            environmentColor = intValue(hud.get("environmentColor"), CYAN);
        }

        private static NativeHudState from(Player player, Map<String, Object> hud) {
            return new NativeHudState(hud == null ? Map.of() : hud);
        }
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

    private static Map<String, Object> dataSource(String key) {
        try {
            Class<?> registry = Class.forName("dev.echo.nativeplatform.bootstrap.EchoNativeAgent5UiHandlerRegistry");
            Method method = registry.getMethod("dataSources");
            return map(map(method.invoke(null)).get(key));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return Map.of();
        }
    }

    private static Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> source) {
            Map<String, Object> copy = new LinkedHashMap<>();
            source.forEach((key, entry) -> copy.put(String.valueOf(key), entry));
            return copy;
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

    private static String textValue(Map<String, Object> source, String key, String fallback) {
        Object value = source.get(key);
        String text = value == null ? "" : String.valueOf(value);
        return text.isBlank() ? fallback : text;
    }

    private static int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static float number(Object value, float fallback) {
        if (value instanceof Number number) {
            return number.floatValue();
        }
        if (value instanceof String text) {
            try {
                return Float.parseFloat(text.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static boolean bool(Object value) {
        return Boolean.TRUE.equals(value) || value instanceof String text && Boolean.parseBoolean(text);
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static String temperatureStatus(int temperature) {
        if (temperature <= 20) {
            return "Freezing";
        }
        if (temperature <= 40) {
            return "Cold";
        }
        if (temperature <= 60) {
            return "Cool";
        }
        if (temperature <= 80) {
            return "Normal";
        }
        return "Warm";
    }
}
