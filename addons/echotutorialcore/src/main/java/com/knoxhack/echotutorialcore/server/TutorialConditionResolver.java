package com.knoxhack.echotutorialcore.server;

import com.knoxhack.echocore.api.EchoRuntimeModules;
import com.knoxhack.echotutorialcore.data.TutorialPlayerData;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class TutorialConditionResolver {
    private TutorialConditionResolver() {}

    public static boolean allMatch(Player player, Iterable<String> conditions) {
        if (player == null || conditions == null) {
            return false;
        }
        boolean sawCondition = false;
        for (String condition : conditions) {
            if (condition == null || condition.isBlank()) {
                continue;
            }
            sawCondition = true;
            if (!matches(player, condition)) {
                return false;
            }
        }
        return sawCondition;
    }

    public static boolean matches(Player player, String condition) {
        if (player == null || condition == null || condition.isBlank()) {
            return false;
        }
        String clean = condition.trim();
        if (clean.startsWith("!") && clean.length() > 1) {
            return !matches(player, clean.substring(1));
        }
        String normalized = clean.toLowerCase(Locale.ROOT);
        TutorialPlayerData data = TutorialPlayerData.get(player);

        if (normalized.startsWith("progress:") || normalized.startsWith("has_progress:")) {
            return data.hasProgress(idTail(clean));
        }
        if (normalized.startsWith("missing_progress:")
                || normalized.startsWith("not_progress:")
                || normalized.startsWith("no_progress:")
                || normalized.startsWith("!progress:")) {
            return !data.hasProgress(idTail(clean));
        }
        if (normalized.startsWith("mod_loaded:") || normalized.startsWith("addon_loaded:")) {
            String modId = simpleTail(clean);
            return !modId.isBlank() && EchoRuntimeModules.isLoaded(modId);
        }
        if (normalized.startsWith("mod_missing:") || normalized.startsWith("addon_missing:")) {
            String modId = simpleTail(clean);
            return !modId.isBlank() && !EchoRuntimeModules.isLoaded(modId);
        }
        if (normalized.startsWith("guide_mode:")) {
            return TutorialProgressManager.getGuideMode(player).name().equalsIgnoreCase(simpleTail(clean));
        }
        if (normalized.startsWith("has_item:") || normalized.startsWith("inventory_item:")) {
            Identifier id = Identifier.tryParse(idTail(clean));
            return id != null && hasItem(player, id);
        }
        if (normalized.startsWith("missing_item:")) {
            Identifier id = Identifier.tryParse(idTail(clean));
            return id != null && !hasItem(player, id);
        }
        if (normalized.startsWith("has_tag:") || normalized.startsWith("inventory_tag:")) {
            Identifier id = Identifier.tryParse(idTail(clean));
            return id != null && hasItemTag(player, id);
        }
        if (normalized.startsWith("missing_tag:")) {
            Identifier id = Identifier.tryParse(idTail(clean));
            return id != null && !hasItemTag(player, id);
        }
        if (normalized.startsWith("hazard:") || normalized.startsWith("active_hazard:")) {
            return data.lastHazardIds().contains(idTail(clean));
        }
        if (normalized.startsWith("region:") || normalized.startsWith("active_region:")) {
            return data.lastRegionId().equalsIgnoreCase(idTail(clean));
        }
        if (normalized.startsWith("mission_state:") || normalized.startsWith("mission:")) {
            return data.lastMissionState().equalsIgnoreCase(simpleTail(clean));
        }
        if (normalized.startsWith("power_alert:")) {
            return data.lastPowerAlert().equalsIgnoreCase(simpleTail(clean));
        }
        if (normalized.startsWith("mistake:")) {
            return data.getMistakeCount(idTail(clean)) > 0;
        }
        if (normalized.startsWith("mistake_count:")) {
            return mistakeCountMatches(data, clean);
        }
        if (normalized.startsWith("repeated_death_count:")) {
            return data.repeatedDeathCount() >= intTail(clean, 1);
        }
        if (normalized.startsWith("time_since_progress_minutes:")) {
            long minutes = intTail(clean, 1);
            long last = data.lastProgressGameTime();
            return player.level().getGameTime() - last >= minutes * 1200L;
        }
        if (normalized.startsWith("terminal_unused_minutes:")) {
            return olderThan(player, data.lastTerminalOpenTime(), intTail(clean, 1));
        }
        if (normalized.startsWith("scanner_unused_minutes:")) {
            return olderThan(player, data.lastScannerUseTime(), intTail(clean, 1));
        }
        if (normalized.startsWith("holomap_unused_minutes:")) {
            return olderThan(player, data.lastHoloMapOpenTime(), intTail(clean, 1));
        }
        if (normalized.startsWith("lens_unused_minutes:")) {
            return olderThan(player, data.lastLensScanTime(), intTail(clean, 1));
        }
        return false;
    }

    public static boolean isKnownConditionKey(String condition) {
        if (condition == null || condition.isBlank()) {
            return false;
        }
        String normalized = condition.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("progress:")
                || normalized.startsWith("has_progress:")
                || normalized.startsWith("missing_progress:")
                || normalized.startsWith("not_progress:")
                || normalized.startsWith("no_progress:")
                || normalized.startsWith("!progress:")
                || (normalized.startsWith("!") && isKnownConditionKey(normalized.substring(1)))
                || normalized.startsWith("mod_loaded:")
                || normalized.startsWith("addon_loaded:")
                || normalized.startsWith("mod_missing:")
                || normalized.startsWith("addon_missing:")
                || normalized.startsWith("guide_mode:")
                || normalized.startsWith("has_item:")
                || normalized.startsWith("inventory_item:")
                || normalized.startsWith("missing_item:")
                || normalized.startsWith("has_tag:")
                || normalized.startsWith("inventory_tag:")
                || normalized.startsWith("missing_tag:")
                || normalized.startsWith("hazard:")
                || normalized.startsWith("active_hazard:")
                || normalized.startsWith("region:")
                || normalized.startsWith("active_region:")
                || normalized.startsWith("mission_state:")
                || normalized.startsWith("mission:")
                || normalized.startsWith("power_alert:")
                || normalized.startsWith("mistake:")
                || normalized.startsWith("mistake_count:")
                || normalized.startsWith("repeated_death_count:")
                || normalized.startsWith("time_since_progress_minutes:")
                || normalized.startsWith("terminal_unused_minutes:")
                || normalized.startsWith("scanner_unused_minutes:")
                || normalized.startsWith("holomap_unused_minutes:")
                || normalized.startsWith("lens_unused_minutes:");
    }

    private static boolean hasItem(Player player, Identifier id) {
        Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null) {
            return false;
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasItemTag(Player player, Identifier tagId) {
        TagKey<Item> tag = TagKey.create(Registries.ITEM, tagId);
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.is(tag)) {
                return true;
            }
        }
        return false;
    }

    private static boolean mistakeCountMatches(TutorialPlayerData data, String condition) {
        String body = condition.substring(condition.indexOf(':') + 1);
        int gte = body.lastIndexOf(">=");
        if (gte >= 0) {
            String key = body.substring(0, gte);
            int threshold = parseInt(body.substring(gte + 2), 1);
            return data.getMistakeCount(key) >= threshold;
        }
        return data.getMistakeCount(body) > 0;
    }

    private static boolean olderThan(Player player, long timestamp, int minutes) {
        return timestamp <= 0 || player.level().getGameTime() - timestamp >= Math.max(1, minutes) * 1200L;
    }

    private static String idTail(String condition) {
        int idx = condition.indexOf(':');
        return idx < 0 ? "" : condition.substring(idx + 1).trim();
    }

    private static String simpleTail(String condition) {
        String tail = idTail(condition);
        int namespaceSeparator = tail.indexOf(':');
        return namespaceSeparator >= 0 ? tail.substring(namespaceSeparator + 1).trim() : tail;
    }

    private static int intTail(String condition, int fallback) {
        return parseInt(simpleTail(condition), fallback);
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (RuntimeException e) {
            return fallback;
        }
    }
}
