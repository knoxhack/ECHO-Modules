package com.knoxhack.echoashfallprotocol.client.hud;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
    private static final String NOTIFICATION_ANCHOR = "below_ashfall_status_panel";
    private static final int NOTICE_GAP = 6;
    private static final int NOTICE_ROW_HEIGHT = 34;
    private static final int NOTICE_ROW_GAP = 4;
    private static final int NOTICE_MAX_ROWS = 2;
    private static final String STARTER_MISSION_RESOURCE =
            "data/echoashfallprotocol/missioncore/missions/secure_crash_outpost.json";
    private static final List<String> STATUS_METER_LABELS = List.of(
            "VITAL", "FOOD", "H2O", "AIR/MASK", "RAD", "TEMP");

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
        Map<String, Object> hud = nativeHudPayload(dataSource("hud"), dataSource("missionLog"));
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
        List<Map<String, Object>> statusMeters = statusMeters(nativeHud, health, maxHealth, food, air);
        List<Map<String, Object>> notificationRows = notificationRows(notifications);
        String missionLine = missionLine(nativeHud);
        String hazardLine = hazardLine(nativeHud);
        String weatherLine = eventLine(nativeHud);

        int barX = x + 8;
        int barY = y + 28;
        int barW = width - 16;
        bar(graphics, barX, barY, barW, "VITAL", health / maxHealth, healthColor(health / maxHealth));
        bar(graphics, barX, barY + 13, barW, "FOOD", food.getFoodLevel() / 20.0F,
                food.getFoodLevel() <= 6 ? RED : food.getFoodLevel() <= 12 ? AMBER : GREEN);
        bar(graphics, barX, barY + 26, barW, "H2O", nativeHud.hydrationPercent,
                hydrationColor(nativeHud.hydrationPercent));
        bar(graphics, barX, barY + 39, barW, "AIR/MASK", nativeHud.airFilterPercent / 100.0F,
                filterColor(nativeHud.airFilterPercent / 100.0F));
        bar(graphics, barX, barY + 52, barW, "RAD", nativeHud.radiationPercent,
                radiationColor(nativeHud.radiationPercent));
        bar(graphics, barX, barY + 65, barW, "TEMP", nativeHud.temperaturePercent / 100.0F,
                temperatureColor(nativeHud.temperature));

        String status = "ARM " + armor + "  POS " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
        text(graphics, font, clip(font, status, width - 16), x + 8, y + 107, MUTED);
        text(graphics, font, clip(font, missionLine, width - 16), x + 8, y + 121, GREEN);
        text(graphics, font, clip(font, hazardLine, width - 16), x + 8, y + 134,
                hazardColor(nativeHud));
        text(graphics, font, clip(font, weatherLine, width - 16), x + 8, y + 147,
                nativeHud.environmentActive ? nativeHud.environmentColor : notifications.isEmpty() ? MUTED : CYAN);
        renderNotificationRows(graphics, font, x, y + height + NOTICE_GAP, width, notificationRows);

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
        state.put("statusMeters", statusMeters);
        state.put("missionLine", missionLine);
        state.put("hazardLine", hazardLine);
        state.put("weatherLine", weatherLine);
        state.put("notificationRows", notificationRows);
        state.put("notificationAnchor", NOTIFICATION_ANCHOR);
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
                pos,
                statusMeters,
                notificationRows,
                missionLine,
                hazardLine,
                weatherLine
        ));
        return Map.copyOf(state);
    }

    public static List<String> statusMeterLabelsForTests() {
        return STATUS_METER_LABELS;
    }

    public static String notificationAnchorForTests() {
        return NOTIFICATION_ANCHOR;
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
            BlockPos pos,
            List<Map<String, Object>> statusMeters,
            List<Map<String, Object>> notificationRows,
            String missionLine,
            String hazardLine,
            String weatherLine
    ) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("source", "live_ashfall_runtime_state");
        snapshot.put("playerId", player.getUUID().toString());
        snapshot.put("hazardMeters", hazardMeters(nativeHud));
        snapshot.put("missionTracker", missionTracker(nativeHud));
        snapshot.put("statusWidgets", statusWidgets(health, maxHealth, food, armor, air));
        snapshot.put("statusMeters", statusMeters);
        snapshot.put("missionLine", missionLine);
        snapshot.put("hazardLine", hazardLine);
        snapshot.put("weatherLine", weatherLine);
        snapshot.put("weatherReadout", weatherReadout(nativeHud));
        snapshot.put("fieldReadouts", fieldReadouts(player, nativeHud, pos));
        snapshot.put("notificationCount", notifications.size());
        snapshot.put("notificationRows", notificationRows);
        snapshot.put("notificationAnchor", NOTIFICATION_ANCHOR);
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

    private static List<Map<String, Object>> statusMeters(
            NativeHudState nativeHud,
            float health,
            float maxHealth,
            FoodData food,
            int air
    ) {
        return List.of(
                statusMeter("VITAL", Math.round(health) + "/" + Math.round(maxHealth), health / Math.max(1.0F, maxHealth)),
                statusMeter("FOOD", food.getFoodLevel() + "/20", food.getFoodLevel() / 20.0F),
                statusMeter("H2O", nativeHud.hydration + "%", nativeHud.hydrationPercent),
                statusMeter("AIR/MASK", "FILTER " + nativeHud.airFilterPercent + "% AIR " + air,
                        nativeHud.airFilterPercent / 100.0F),
                statusMeter("RAD", Math.round(nativeHud.radiationLevel) + "%", nativeHud.radiationPercent),
                statusMeter("TEMP", nativeHud.temperature + "% " + nativeHud.temperatureLabel,
                        nativeHud.temperaturePercent / 100.0F)
        );
    }

    private static Map<String, Object> statusMeter(String label, String value, float percent) {
        Map<String, Object> meter = new LinkedHashMap<>();
        meter.put("label", label);
        meter.put("value", value);
        meter.put("percent", Math.round(clamp01(percent) * 100.0F));
        return Map.copyOf(meter);
    }

    private static List<Map<String, Object>> notificationRows(List<Map<String, Object>> notifications) {
        if (notifications.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> notification : notifications) {
            if (rows.size() >= NOTICE_MAX_ROWS) {
                break;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            String message = textValue(notification, "message", "");
            String title = textValue(notification, "title", message);
            String detail = textValue(notification, "detail", textValue(notification, "footer", ""));
            row.put("source", textValue(notification, "sourceLabel",
                    textValue(notification, "source", textValue(notification, "id", "ASHFALL"))));
            row.put("status", textValue(notification, "statusLabel",
                    textValue(notification, "severity", "INFO")));
            row.put("title", title.isBlank() ? textValue(notification, "id", "Ashfall notice") : title);
            row.put("detail", detail);
            row.put("accentColor", intValue(notification.get("accentColor"), CYAN));
            row.put("anchor", NOTIFICATION_ANCHOR);
            rows.add(Map.copyOf(row));
        }
        return List.copyOf(rows);
    }

    private static void renderNotificationRows(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            List<Map<String, Object>> rows
    ) {
        for (int i = 0; i < rows.size(); i++) {
            int rowY = y + i * (NOTICE_ROW_HEIGHT + NOTICE_ROW_GAP);
            renderNotificationRow(graphics, font, x, rowY, width, NOTICE_ROW_HEIGHT, rows.get(i));
        }
    }

    private static void renderNotificationRow(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            int height,
            Map<String, Object> row
    ) {
        int accent = intValue(row.get("accentColor"), CYAN);
        graphics.fill(x + 2, y + 2, x + width + 2, y + height + 2, 0x40000000);
        graphics.fill(x, y, x + width, y + height, PANEL);
        graphics.fill(x, y, x + width, y + 1, withAlpha(accent, 0xCC));
        graphics.fill(x, y, x + 2, y + height, withAlpha(accent, 0xDD));

        String source = clip(font, String.valueOf(row.getOrDefault("source", "ASHFALL")), 118);
        String status = clip(font, String.valueOf(row.getOrDefault("status", "INFO")), 82);
        text(graphics, font, source, x + 7, y + 4, withAlpha(accent, 0xFF));
        text(graphics, font, status, x + width - 7 - font.width(status), y + 4, MUTED);

        String title = clip(font, String.valueOf(row.getOrDefault("title", "")), width - 16);
        text(graphics, font, title, x + 7, y + 15, TEXT);
        String detail = clip(font, String.valueOf(row.getOrDefault("detail", "")), width - 16);
        if (!detail.isBlank()) {
            text(graphics, font, detail, x + 7, y + 24, MUTED);
        }
    }

    private static void bar(GuiGraphicsExtractor graphics, int x, int y, int width, String label, float value, int color) {
        int labelW = 54;
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

    private static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
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
        for (ClassLoader loader : registryClassLoaders()) {
            try {
                Class<?> registry = Class.forName(
                        "dev.echo.nativeplatform.bootstrap.EchoNativeAgent5UiHandlerRegistry",
                        true,
                        loader
                );
                Method method = registry.getMethod("dataSources");
                return map(map(method.invoke(null)).get(key));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                continue;
            }
        }
        return Map.of();
    }

    private static List<ClassLoader> registryClassLoaders() {
        List<ClassLoader> loaders = new ArrayList<>();
        addClassLoader(loaders, Thread.currentThread().getContextClassLoader());
        addClassLoader(loaders, ClassLoader.getSystemClassLoader());
        ClassLoader moduleLoader = EchoNativeAshfallHudOverlay.class.getClassLoader();
        while (moduleLoader != null) {
            addClassLoader(loaders, moduleLoader);
            moduleLoader = moduleLoader.getParent();
        }
        return List.copyOf(loaders);
    }

    private static void addClassLoader(List<ClassLoader> loaders, ClassLoader loader) {
        if (loader != null && !loaders.contains(loader)) {
            loaders.add(loader);
        }
    }

    private static Map<String, Object> nativeHudPayload(Map<String, Object> hud, Map<String, Object> missionLog) {
        Map<String, Object> merged = new LinkedHashMap<>(hud == null ? Map.of() : hud);
        Map<String, Object> mission = missionLog == null || missionLog.isEmpty()
                ? starterMissionFallback()
                : missionLog;
        putIfBlank(merged, "missionId", firstText(
                textValue(mission, "missionId", ""),
                textValue(mission, "id", "")
        ));
        putIfBlank(merged, "missionStatus", textValue(mission, "status", ""));
        putIfBlank(merged, "mission", firstText(
                textValue(mission, "objective", ""),
                textValue(mission, "title", "")
        ));
        putIfBlank(merged, "missionTitle", textValue(mission, "title", ""));
        putIfBlank(merged, "missionObjective", textValue(mission, "objective", ""));
        return Map.copyOf(merged);
    }

    private static Map<String, Object> starterMissionFallback() {
        ClassLoader loader = EchoNativeAshfallHudOverlay.class.getClassLoader();
        try (InputStream stream = loader == null
                ? ClassLoader.getSystemResourceAsStream(STARTER_MISSION_RESOURCE)
                : loader.getResourceAsStream(STARTER_MISSION_RESOURCE)) {
            if (stream == null) {
                return Map.of();
            }
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            JsonObject mission = JsonParser.parseString(json).getAsJsonObject();
            String objective = firstObjectiveLabel(mission);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("missionId", jsonString(mission, "id", "echoashfallprotocol:secure_crash_outpost"));
            payload.put("status", "TRACKED");
            payload.put("title", jsonString(mission, "title", "Anchor Pod Outpost"));
            payload.put("objective", objective.isBlank() ? jsonString(mission, "title", "Anchor Pod Outpost") : objective);
            payload.put("source", STARTER_MISSION_RESOURCE);
            return Map.copyOf(payload);
        } catch (IOException | RuntimeException exception) {
            return Map.of();
        }
    }

    private static String firstObjectiveLabel(JsonObject mission) {
        JsonElement objectivesElement = mission.get("objectives");
        if (!(objectivesElement instanceof JsonArray objectives) || objectives.isEmpty()) {
            return "";
        }
        JsonElement first = objectives.get(0);
        if (!first.isJsonObject()) {
            return "";
        }
        return jsonString(first.getAsJsonObject(), "label", "");
    }

    private static String jsonString(JsonObject object, String key, String fallback) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        String value = element.getAsString();
        return value == null || value.isBlank() ? fallback : value;
    }

    private static void putIfBlank(Map<String, Object> target, String key, String value) {
        if (!hasText(target.get(key)) && value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }

    private static boolean hasText(Object value) {
        return value != null && !String.valueOf(value).isBlank();
    }

    private static String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
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
