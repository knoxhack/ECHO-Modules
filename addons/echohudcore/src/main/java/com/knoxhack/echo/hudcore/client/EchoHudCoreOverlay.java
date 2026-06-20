package com.knoxhack.echo.hudcore.client;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeEnvironmentBridge;
import com.knoxhack.echo.hudcore.EchoHudSnapshotContract;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.Level;

public final class EchoHudCoreOverlay {
    private static final int PANEL = 0xB5091218;
    private static final int PANEL_SOFT = 0x7A102833;
    private static final int ACCENT = 0xDD55E4F5;
    private static final int ACCENT_DIM = 0x7755E4F5;
    private static final int TEXT = 0xFFEAF8FF;
    private static final int MUTED = 0xFF8EA8B5;
    private static final int GREEN = 0xFF7CFFB2;
    private static final int AMBER = 0xFFFFCB66;
    private static final int RED = 0xFFFF6868;
    private static final int BLUE = 0xFF77B8FF;
    private static boolean routeEnabled;
    private static Map<String, Object> nativeRouteState = Map.of(
            "routeEnabled", false,
            "mutationCount", 0);

    private EchoHudCoreOverlay() {
    }

    public static void render(GuiGraphicsExtractor graphics, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (!shouldRender(minecraft, player)) {
            return;
        }
        Font font = minecraft.font;
        int screenW = graphics.guiWidth();
        int leftW = Math.min(286, Math.max(214, screenW / 5));
        int x = 8;
        int y = 8;
        int leftH = 102;
        drawPanel(graphics, x, y, leftW, leftH, "ECHO HUDCORE");

        FoodData food = player.getFoodData();
        float health = player.getHealth();
        float maxHealth = Math.max(1.0F, player.getMaxHealth());
        int rowY = y + 24;
        bar(graphics, font, x + 10, rowY, leftW - 20, "VITAL", health / maxHealth, healthColor(health / maxHealth),
                Math.round(health) + "/" + Math.round(maxHealth));
        bar(graphics, font, x + 10, rowY + 14, leftW - 20, "FOOD", food.getFoodLevel() / 20.0F,
                foodColor(food.getFoodLevel()), Integer.toString(food.getFoodLevel()));
        bar(graphics, font, x + 10, rowY + 28, leftW - 20, "AIR", Math.min(1.0F, player.getAirSupply() / 300.0F),
                airColor(player.getAirSupply()), Integer.toString(player.getAirSupply()));

        BlockPos pos = player.blockPosition();
        line(graphics, font, x + 10, y + 70, "ARM " + player.getArmorValue() + "  POS "
                + pos.getX() + " " + pos.getY() + " " + pos.getZ(), MUTED, leftW - 20);
        line(graphics, font, x + 10, y + 84, "DIM " + dimension(player.level()), BLUE, leftW - 20);

        int rightW = Math.min(260, Math.max(194, screenW / 6));
        int rightX = screenW - rightW - 8;
        int rightY = 8;
        drawPanel(graphics, rightX, rightY, rightW, 78, "FIELD STATE");
        line(graphics, font, rightX + 10, rightY + 24, "BIO " + biome(player), GREEN, rightW - 20);
        line(graphics, font, rightX + 10, rightY + 38, "HEAD " + heading(player), ACCENT, rightW - 20);
        line(graphics, font, rightX + 10, rightY + 52, effectSummary(player.getActiveEffects()), effectColor(player), rightW - 20);

        int compassW = 172;
        int compassX = (screenW - compassW) / 2;
        int compassY = graphics.guiHeight() - 34;
        drawCompass(graphics, font, compassX, compassY, compassW, heading(player));
    }

    public static boolean handleNativeHudAction(String actionId, Map<String, Object> action) {
        return handleNativeHudAction(actionId, action, Map.of());
    }

    public static boolean handleNativeHudAction(
            String actionId,
            Map<String, Object> action,
            Map<String, Object> metadata
    ) {
        if (!nativeLoaderActive()) {
            return false;
        }
        routeEnabled = true;
        Map<String, Object> safeAction = action == null ? Map.of() : action;
        Map<String, Object> safeMetadata = metadata == null ? Map.of() : metadata;
        String kind = text(safeAction.get("kind"));
        boolean handled = switch (actionId) {
            case "hud.render", "hud.update_snapshot", "hud.mission_tracker.render",
                    "hud.hazard_readout.render", "hud.compass_indicator.render",
                    "hud.screen_safe_area.resolve", "native_loader.overlay_focus" -> true;
            default -> "hud_render".equals(kind)
                    || "hud_state_update".equals(kind)
                    || "hud_overlay_focus".equals(kind)
                    || "hud_widget_render".equals(kind)
                    || "hud_layout_resolve".equals(kind);
        };
        if (handled) {
            recordNativeRouteState(actionId, safeAction, kind, safeMetadata);
        }
        return handled;
    }

    public static void enableNativeRoute() {
        if (nativeLoaderActive()) {
            routeEnabled = true;
            recordNativeRouteState("hud.native_route.enable", Map.of("kind", "hud_route_enable"),
                    "hud_route_enable", Map.of("source", "echohudcore_client_route_registry"));
        }
    }

    public static Map<String, Object> nativeRouteState() {
        return Map.copyOf(nativeRouteState);
    }

    private static boolean shouldRender(Minecraft minecraft, Player player) {
        boolean nativeRuntime = nativeLoaderActive();
        return (!nativeRuntime || routeEnabled || Boolean.getBoolean("echo.native.hudcore.force"))
                && player != null
                && !minecraft.options.hideGui
                && minecraft.screen == null;
    }

    private static void drawPanel(GuiGraphicsExtractor graphics, int x, int y, int w, int h, String title) {
        graphics.fill(x, y, x + w, y + h, PANEL);
        graphics.outline(x, y, w, h, ACCENT_DIM);
        graphics.fill(x + 1, y + 1, x + w - 1, y + 17, PANEL_SOFT);
        graphics.fill(x + 8, y + 18, x + w - 8, y + 19, ACCENT_DIM);
        graphics.text(Minecraft.getInstance().font, title, x + 8, y + 6, ACCENT, false);
    }

    private static void bar(GuiGraphicsExtractor graphics, Font font, int x, int y, int w, String label, float value,
            int color, String readout) {
        int labelW = 38;
        int readoutW = Math.max(22, font.width(readout) + 4);
        int barX = x + labelW;
        int barW = Math.max(12, w - labelW - readoutW);
        graphics.text(font, label, x, y, ACCENT, false);
        graphics.fill(barX, y + 2, barX + barW, y + 9, 0x66000000);
        graphics.outline(barX, y + 2, barW, 7, ACCENT_DIM);
        int filled = Math.max(0, Math.min(barW - 2, Math.round((barW - 2) * clamp01(value))));
        graphics.fill(barX + 1, y + 3, barX + 1 + filled, y + 8, color);
        graphics.text(font, readout, x + w - readoutW + 4, y, color, false);
    }

    private static void drawCompass(GuiGraphicsExtractor graphics, Font font, int x, int y, int w, String heading) {
        graphics.fill(x, y, x + w, y + 20, 0x8C071018);
        graphics.outline(x, y, w, 20, ACCENT_DIM);
        graphics.fill(x + w / 2 - 1, y + 2, x + w / 2 + 1, y + 18, ACCENT);
        String left = "W";
        String right = "E";
        graphics.text(font, left, x + 10, y + 7, MUTED, false);
        graphics.text(font, "N", x + w / 2 - font.width("N") / 2, y + 2, TEXT, false);
        graphics.text(font, "S", x + w / 2 - font.width("S") / 2, y + 12, MUTED, false);
        graphics.text(font, right, x + w - 10 - font.width(right), y + 7, MUTED, false);
        graphics.text(font, heading, x + w / 2 + 8, y + 7, ACCENT, false);
    }

    private static void line(GuiGraphicsExtractor graphics, Font font, int x, int y, String text, int color, int width) {
        graphics.text(font, fit(font, text, width), x, y, color, false);
    }

    private static String heading(Player player) {
        float yaw = player.getYRot() % 360.0F;
        if (yaw < 0.0F) {
            yaw += 360.0F;
        }
        String cardinal = switch (Math.round(yaw / 45.0F) & 7) {
            case 0 -> "S";
            case 1 -> "SW";
            case 2 -> "W";
            case 3 -> "NW";
            case 4 -> "N";
            case 5 -> "NE";
            case 6 -> "E";
            default -> "SE";
        };
        return cardinal + " " + Math.round(yaw);
    }

    private static String dimension(Level level) {
        ResourceKey<Level> key = level.dimension();
        return key.identifier().toString();
    }

    private static String biome(Player player) {
        return player.level().getBiome(player.blockPosition()).unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse("unregistered");
    }

    private static String effectSummary(Collection<MobEffectInstance> effects) {
        if (effects == null || effects.isEmpty()) {
            return "EFFECT clear";
        }
        List<String> names = new ArrayList<>();
        for (MobEffectInstance effect : effects) {
            names.add(effect.getDescriptionId().replace("effect.", ""));
            if (names.size() >= 2) {
                break;
            }
        }
        return "EFFECT " + String.join(", ", names);
    }

    private static int effectColor(Player player) {
        return player.getActiveEffects().isEmpty() ? GREEN : AMBER;
    }

    private static int healthColor(float value) {
        if (value <= 0.3F) {
            return RED;
        }
        if (value <= 0.6F) {
            return AMBER;
        }
        return GREEN;
    }

    private static int foodColor(int food) {
        if (food <= 6) {
            return RED;
        }
        if (food <= 12) {
            return AMBER;
        }
        return GREEN;
    }

    private static int airColor(int air) {
        if (air <= 80) {
            return RED;
        }
        if (air <= 180) {
            return AMBER;
        }
        return BLUE;
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static String fit(Font font, String text, int width) {
        if (text == null || text.isBlank()) {
            return "";
        }
        if (font.width(text) <= width) {
            return text;
        }
        return font.plainSubstrByWidth(text, Math.max(4, width - font.width("..."))) + "...";
    }

    private static boolean nativeLoaderActive() {
        return EchoNativeRuntimeEnvironmentBridge.isNativeLoaderActive();
    }

    private static void recordNativeRouteState(
            String actionId,
            Map<String, Object> action,
            String kind,
            Map<String, Object> metadata
    ) {
        Map<String, Object> previous = nativeRouteState;
        Map<String, Object> next = new LinkedHashMap<>(previous);
        Map<String, Object> safeMetadata = metadata == null ? Map.of() : metadata;
        next.put("routeEnabled", routeEnabled);
        next.put("lastActionId", actionId == null ? "" : actionId);
        next.put("lastKind", kind);
        next.put("lastWidget", text(action.get("widget")));
        next.put("lastMetadata", Map.copyOf(safeMetadata));
        putIfPresent(next, "lastSource", safeMetadata.get("source"));
        putIfPresent(next, "lastService", safeMetadata.get("service"));
        putIfPresent(next, "lastFrameSource", safeMetadata.get("frameSource"));
        putIfPresent(next, "lastScreenWidth", safeMetadata.get("screenWidth"));
        putIfPresent(next, "lastScreenHeight", safeMetadata.get("screenHeight"));
        putIfPresent(next, "lastPartialTick", safeMetadata.get("partialTick"));
        next.put("mutationCount", intValue(previous.get("mutationCount")) + 1);
        switch (kind) {
            case "hud_render" -> {
                next.put("renderCount", intValue(previous.get("renderCount")) + 1);
                next.put("lastRenderModel", hudRenderModel(safeMetadata));
            }
            case "hud_state_update" -> {
                next.put("snapshotUpdateCount", intValue(previous.get("snapshotUpdateCount")) + 1);
                next.put("lastSnapshotModel", hudSnapshotModel());
            }
            case "hud_overlay_focus" -> next.put("lastOverlayFocusModel", overlayFocusModel(safeMetadata));
            case "hud_widget_render" -> {
                next.put("widgetRenderCount", intValue(previous.get("widgetRenderCount")) + 1);
                Map<String, Object> widgetModel = hudWidgetModel(action, safeMetadata);
                next.put("lastWidgetModel", widgetModel);
                next.put("widgetModels", updatedWidgetModels(previous.get("widgetModels"), widgetModel));
            }
            case "hud_layout_resolve" -> {
                next.put("layoutResolveCount", intValue(previous.get("layoutResolveCount")) + 1);
                next.put("lastLayoutModel", hudLayoutModel(safeMetadata));
            }
            default -> {
                if ("hud.render".equals(actionId)) {
                    next.put("renderCount", intValue(previous.get("renderCount")) + 1);
                    next.put("lastRenderModel", hudRenderModel(safeMetadata));
                }
            }
        }
        nativeRouteState = Map.copyOf(next);
    }

    private static Map<String, Object> hudSnapshotModel() {
        return EchoHudSnapshotContract.executeReferenceSnapshot(
                EchoHudSnapshotContract.REFERENCE_MISSION_ID,
                EchoHudSnapshotContract.REFERENCE_HAZARD_ID);
    }

    private static Map<String, Object> hudRenderModel(Map<String, Object> metadata) {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("modelType", "hud_render");
        model.put("surface", "hud");
        model.put("routeDrivenRendererState", true);
        model.put("snapshot", hudSnapshotModel());
        putIfPresent(model, "screenWidth", metadata.get("screenWidth"));
        putIfPresent(model, "screenHeight", metadata.get("screenHeight"));
        putIfPresent(model, "partialTick", metadata.get("partialTick"));
        return Map.copyOf(model);
    }

    private static Map<String, Object> hudWidgetModel(Map<String, Object> action, Map<String, Object> metadata) {
        String widget = text(action.get("widget"));
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("modelType", "hud_widget_render");
        model.put("widget", widget);
        model.put("widgetId", widget.isBlank() ? "echohudcore:unknown_widget" : "echohudcore:" + widget);
        model.put("routeDrivenRendererState", true);
        model.put("anchor", switch (widget) {
            case "mission_tracker" -> "top_left";
            case "hazard_readout" -> "top_right";
            case "compass_indicator" -> "bottom_center";
            default -> "floating";
        });
        model.put("rows", switch (widget) {
            case "mission_tracker" -> List.of(
                    Map.of("key", "mission", "value", EchoHudSnapshotContract.REFERENCE_MISSION_ID),
                    Map.of("key", "state", "value", "active"));
            case "hazard_readout" -> List.of(
                    Map.of("key", "hazard", "value", EchoHudSnapshotContract.REFERENCE_HAZARD_ID),
                    Map.of("key", "severity", "value", "warning"));
            case "compass_indicator" -> List.of(
                    Map.of("key", "target", "value", "echoterminal:field_ops/first_ten_minutes"),
                    Map.of("key", "bearing", "value", "NE"));
            default -> List.of(Map.of("key", "status", "value", "unknown"));
        });
        putIfPresent(model, "screenWidth", metadata.get("screenWidth"));
        putIfPresent(model, "screenHeight", metadata.get("screenHeight"));
        putIfPresent(model, "partialTick", metadata.get("partialTick"));
        return Map.copyOf(model);
    }

    private static Map<String, Object> hudLayoutModel(Map<String, Object> metadata) {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("modelType", "hud_layout_resolve");
        model.put("safeAreaId", "echohudcore:screen_safe_area");
        model.put("routeDrivenLayoutState", true);
        model.put("left", 12);
        model.put("top", 10);
        model.put("right", 12);
        model.put("bottom", 18);
        model.put("respectsChat", true);
        model.put("respectsBossBars", true);
        model.put("respectsSubtitles", true);
        putIfPresent(model, "screenWidth", metadata.get("screenWidth"));
        putIfPresent(model, "screenHeight", metadata.get("screenHeight"));
        return Map.copyOf(model);
    }

    private static Map<String, Object> overlayFocusModel(Map<String, Object> metadata) {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("modelType", "hud_overlay_focus");
        model.put("routeDrivenFocusState", true);
        putIfPresent(model, "focusedSurface", metadata.get("focusedSurface"));
        putIfPresent(model, "previousSurface", metadata.get("previousSurface"));
        putIfPresent(model, "focusSource", metadata.get("focusSource"));
        return Map.copyOf(model);
    }

    private static Map<String, Object> updatedWidgetModels(Object previous, Map<String, Object> widgetModel) {
        Map<String, Object> widgets = new LinkedHashMap<>();
        if (previous instanceof Map<?, ?> previousMap) {
            for (Map.Entry<?, ?> entry : previousMap.entrySet()) {
                widgets.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        widgets.put(String.valueOf(widgetModel.get("widgetId")), widgetModel);
        return Map.copyOf(widgets);
    }

    private static void putIfPresent(Map<String, Object> state, String key, Object value) {
        if (value != null && !String.valueOf(value).isBlank()) {
            state.put(key, value);
        }
    }

    private static int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? 0 : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim().toLowerCase(Locale.ROOT);
    }
}
