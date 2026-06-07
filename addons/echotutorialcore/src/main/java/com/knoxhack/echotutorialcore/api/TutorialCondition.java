package com.knoxhack.echotutorialcore.api;

import java.util.Map;
import net.minecraft.resources.Identifier;

public record TutorialCondition(
        TutorialConditionType type,
        Identifier target,
        int count,
        boolean invert,
        String addon,
        Map<String, String> context) {
    public TutorialCondition {
        type = type == null ? TutorialConditionType.PROGRESS : type;
        count = Math.max(0, count);
        addon = addon == null ? "" : addon.strip();
        context = context == null ? Map.of() : Map.copyOf(context);
    }

    public String asLegacyString() {
        String value = target == null ? addon : target.toString();
        if ((value == null || value.isBlank()) && context.containsKey("value")) {
            value = context.get("value");
        }
        value = value == null ? "" : value;
        String encoded = switch (type) {
            case PROGRESS -> "progress:" + value;
            case MISSING_PROGRESS -> "missing_progress:" + value;
            case MOD_LOADED -> "mod_loaded:" + firstNonBlank(addon, value);
            case MOD_MISSING -> "mod_missing:" + firstNonBlank(addon, value);
            case GUIDE_MODE -> "guide_mode:" + value;
            case HAS_ITEM -> "has_item:" + value;
            case MISSING_ITEM -> "missing_item:" + value;
            case HAS_TAG -> "has_tag:" + value;
            case MISSING_TAG -> "missing_tag:" + value;
            case ACTIVE_HAZARD -> "active_hazard:" + value;
            case ACTIVE_REGION -> "active_region:" + value;
            case MISSION_STATE -> "mission_state:" + value;
            case POWER_ALERT -> "power_alert:" + value;
            case MISTAKE -> "mistake:" + value;
            case MISTAKE_COUNT -> "mistake_count:" + value + ">=" + Math.max(1, count);
            case REPEATED_DEATH_COUNT -> "repeated_death_count:" + Math.max(1, count);
            case TIME_SINCE_PROGRESS_MINUTES -> "time_since_progress_minutes:" + Math.max(1, count);
            case TERMINAL_UNUSED_MINUTES -> "terminal_unused_minutes:" + Math.max(1, count);
            case SCANNER_UNUSED_MINUTES -> "scanner_unused_minutes:" + Math.max(1, count);
            case HOLOMAP_UNUSED_MINUTES -> "holomap_unused_minutes:" + Math.max(1, count);
            case LENS_UNUSED_MINUTES -> "lens_unused_minutes:" + Math.max(1, count);
        };
        return invert ? "!" + encoded : encoded;
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second == null ? "" : second;
    }
}
