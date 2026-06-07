package com.knoxhack.echotutorialcore.server;

import com.knoxhack.echotutorialcore.EchoTutorialCore;
import com.knoxhack.echotutorialcore.api.TutorialConditionType;
import com.knoxhack.echotutorialcore.api.TutorialGuideMode;
import com.knoxhack.echotutorialcore.api.TutorialRequirement;
import com.knoxhack.echotutorialcore.config.TutorialConfig;
import com.knoxhack.echotutorialcore.data.TutorialPlayerData;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class TutorialRequirementResolver {
    private TutorialRequirementResolver() {}

    public static void showRequirementHint(Player player, Identifier requirementId) {
        if (player == null || requirementId == null) return;
        TutorialGuideMode mode = TutorialProgressManager.getGuideMode(player);
        if (mode == TutorialGuideMode.OFF) return;
        if (!TutorialConfig.ENABLE_RECIPE_LOCK_EXPLANATIONS.get()) return;

        TutorialRequirement requirement = resolveRequirement(requirementId);
        boolean missing = isMissing(player, requirement);
        String status = missing ? "Missing " : "Requirement met: ";
        String guide = requirement.helpCardId() == null ? "" : " Guide: " + requirement.helpCardId();
        player.sendSystemMessage(Component.literal("[ECHO-7] " + status + explain(requirement) + "." + guide));
    }

    public static List<String> resolveMissingItems(Player player, List<Identifier> items) {
        List<String> missing = new ArrayList<>();
        if (items == null) return missing;
        for (Identifier id : items) {
            if (!hasItem(player, id)) {
                missing.add(id.toString());
            }
        }
        return missing;
    }

    public static List<TutorialRequirement> resolveMissingRequirements(Player player, List<TutorialRequirement> requirements) {
        List<TutorialRequirement> missing = new ArrayList<>();
        if (player == null || requirements == null) return missing;
        for (TutorialRequirement requirement : requirements) {
            if (requirement != null && isMissing(player, requirement)) {
                missing.add(requirement);
            }
        }
        return missing;
    }

    public static TutorialRequirement resolveRequirement(Identifier requirementId) {
        if (requirementId == null) {
            return new TutorialRequirement(id("unknown_requirement"), TutorialConditionType.PROGRESS, id("unknown_requirement"), 1,
                    "unknown requirement", id("why_recipe_locked"));
        }
        String path = requirementId.getPath();
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.startsWith("item/") || lower.startsWith("item_")) {
            return requirement(requirementId, TutorialConditionType.HAS_ITEM, target(requirementId, lower.startsWith("item/") ? 5 : 5),
                    id("why_recipe_locked"));
        }
        if (lower.startsWith("tag/") || lower.startsWith("tag_")) {
            return requirement(requirementId, TutorialConditionType.HAS_TAG, target(requirementId, lower.startsWith("tag/") ? 4 : 4),
                    id("why_recipe_locked"));
        }
        if (lower.startsWith("research/") || lower.startsWith("research_")) {
            return requirement(requirementId, TutorialConditionType.PROGRESS,
                    id("research_" + stripPrefix(path, lower.startsWith("research/") ? "research/" : "research_")), id("research_basics"));
        }
        if (lower.startsWith("mission/") || lower.startsWith("mission_")) {
            return requirement(requirementId, TutorialConditionType.PROGRESS,
                    id("mission_" + stripPrefix(path, lower.startsWith("mission/") ? "mission/" : "mission_")), id("mission_browser"));
        }
        if (lower.startsWith("faction/") || lower.startsWith("faction_")) {
            return requirement(requirementId, TutorialConditionType.PROGRESS,
                    id("faction_" + stripPrefix(path, lower.startsWith("faction/") ? "faction/" : "faction_")), id("faction_basics"));
        }
        return requirement(requirementId, TutorialConditionType.PROGRESS, requirementId, id("why_recipe_locked"));
    }

    private static TutorialRequirement requirement(
            Identifier id,
            TutorialConditionType type,
            Identifier target,
            Identifier helpCardId) {
        return new TutorialRequirement(id, type, target, 1, readable(type, target), helpCardId);
    }

    private static boolean isMissing(Player player, TutorialRequirement requirement) {
        if (player == null || requirement == null || requirement.target() == null) {
            return true;
        }
        TutorialPlayerData data = TutorialPlayerData.get(player);
        return switch (requirement.type()) {
            case HAS_ITEM -> !hasItem(player, requirement.target(), requirement.count());
            case MISSING_ITEM -> hasItem(player, requirement.target(), requirement.count());
            case HAS_TAG -> !hasItemTag(player, requirement.target(), requirement.count());
            case MISSING_TAG -> hasItemTag(player, requirement.target(), requirement.count());
            case PROGRESS -> !data.hasProgress(requirement.target());
            case MISSING_PROGRESS -> data.hasProgress(requirement.target());
            case MISSION_STATE -> !data.lastMissionState().equalsIgnoreCase(requirement.target().getPath());
            case ACTIVE_REGION -> !data.lastRegionId().equalsIgnoreCase(requirement.target().toString());
            case ACTIVE_HAZARD -> !data.lastHazardIds().contains(requirement.target().toString());
            default -> !TutorialConditionResolver.matches(player, condition(requirement));
        };
    }

    private static boolean hasItem(Player player, Identifier id) {
        return hasItem(player, id, 1);
    }

    private static boolean hasItem(Player player, Identifier id, int count) {
        if (player == null || id == null) return false;
        Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null) return false;
        int found = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.is(item)) {
                found += stack.getCount();
                if (found >= Math.max(1, count)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasItemTag(Player player, Identifier tagId, int count) {
        TagKey<Item> tag = TagKey.create(Registries.ITEM, tagId);
        int found = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.is(tag)) {
                found += stack.getCount();
                if (found >= Math.max(1, count)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String explain(TutorialRequirement requirement) {
        if (requirement.label() != null && !requirement.label().isBlank()) {
            return requirement.label();
        }
        return readable(requirement.type(), requirement.target());
    }

    private static String readable(TutorialConditionType type, Identifier target) {
        String label = target == null ? "unknown" : target.toString();
        return switch (type) {
            case HAS_ITEM -> "item " + label;
            case HAS_TAG -> "item tag #" + label;
            case PROGRESS -> "progress " + label;
            case MISSION_STATE -> "mission state " + label;
            case ACTIVE_REGION -> "region " + label;
            case ACTIVE_HAZARD -> "hazard context " + label;
            default -> type.name().toLowerCase(Locale.ROOT) + " " + label;
        };
    }

    private static String condition(TutorialRequirement requirement) {
        return switch (requirement.type()) {
            case HAS_ITEM -> "has_item:" + requirement.target();
            case MISSING_ITEM -> "missing_item:" + requirement.target();
            case HAS_TAG -> "has_tag:" + requirement.target();
            case MISSING_TAG -> "missing_tag:" + requirement.target();
            case MISSING_PROGRESS -> "missing_progress:" + requirement.target();
            case ACTIVE_REGION -> "active_region:" + requirement.target();
            case ACTIVE_HAZARD -> "active_hazard:" + requirement.target();
            case MISSION_STATE -> "mission_state:" + requirement.target();
            default -> "progress:" + requirement.target();
        };
    }

    private static Identifier target(Identifier requirementId, int prefixLength) {
        String path = requirementId.getPath();
        String stripped = path.length() > prefixLength ? path.substring(prefixLength) : path;
        return stripped.contains(":") && Identifier.tryParse(stripped) != null
                ? Identifier.parse(stripped)
                : Identifier.fromNamespaceAndPath(requirementId.getNamespace(), stripped);
    }

    private static String stripPrefix(String path, String prefix) {
        return path.startsWith(prefix) ? path.substring(prefix.length()) : path;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, path);
    }
}
