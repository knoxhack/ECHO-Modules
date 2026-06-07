package com.knoxhack.echoterminal.mission;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRole;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

final class VanillaJourneyDefinitions {
    private static final String MISSIONS_PATH = "data/echoterminal/vanilla_journey/missions.json";
    private static final String REWARD_TIERS_PATH = "data/echoterminal/vanilla_journey/reward_tiers.json";
    private static final Map<String, RewardTierDefinition> REWARD_TIERS = loadRewardTiers();
    private static final List<VanillaMission> MISSIONS = loadMissions(REWARD_TIERS);
    private static final Map<Identifier, VanillaMission> MISSIONS_BY_ID = indexMissions(MISSIONS);

    private VanillaJourneyDefinitions() {
    }

    static List<VanillaMission> missions() {
        return MISSIONS;
    }

    static VanillaMission mission(Identifier id) {
        return MISSIONS_BY_ID.get(id);
    }

    private static Map<String, RewardTierDefinition> loadRewardTiers() {
        JsonObject root = readObject(REWARD_TIERS_PATH);
        JsonObject tiersObject = requiredObject(root, "tiers", REWARD_TIERS_PATH);
        Map<String, RewardTierDefinition> tiers = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : tiersObject.entrySet()) {
            String tierId = entry.getKey();
            JsonObject tierObject = asObject(entry.getValue(), REWARD_TIERS_PATH + "#" + tierId);
            RewardTierDefinition tier = new RewardTierDefinition(
                    tierId,
                    requiredString(tierObject, "label", REWARD_TIERS_PATH + "#" + tierId),
                    booleanOrDefault(tierObject, "claimable", true),
                    rewardStacks(requiredArray(tierObject, "rewards", REWARD_TIERS_PATH + "#" + tierId),
                            REWARD_TIERS_PATH + "#" + tierId + ".rewards"));
            tiers.put(tierId, tier);
        }
        if (tiers.isEmpty()) {
            throw new IllegalStateException("No Baseline reward tiers defined in " + REWARD_TIERS_PATH);
        }
        return Map.copyOf(tiers);
    }

    private static List<VanillaMission> loadMissions(Map<String, RewardTierDefinition> tiers) {
        JsonObject root = readObject(MISSIONS_PATH);
        JsonArray missionsArray = requiredArray(root, "missions", MISSIONS_PATH);
        List<VanillaMission> missions = new ArrayList<>();
        for (int i = 0; i < missionsArray.size(); i++) {
            String context = MISSIONS_PATH + ".missions[" + i + "]";
            JsonObject missionObject = asObject(missionsArray.get(i), context);
            String tierId = requiredString(missionObject, "tier", context);
            RewardTierDefinition tier = tiers.get(tierId);
            if (tier == null) {
                throw new IllegalStateException("Unknown Baseline reward tier '" + tierId + "' in " + context);
            }
            missions.add(new VanillaMission(
                    identifier(requiredString(missionObject, "advancement", context), context + ".advancement"),
                    requiredString(missionObject, "title", context),
                    requiredString(missionObject, "briefing", context),
                    requiredString(missionObject, "guide", context),
                    requiredString(missionObject, "phase_id", context),
                    requiredString(missionObject, "phase", context),
                    requiredInt(missionObject, "phase_order", context),
                    requiredInt(missionObject, "order", context),
                    item(requiredString(missionObject, "icon", context), context + ".icon"),
                    tier,
                    role(requiredString(missionObject, "role", context), context + ".role")));
        }
        if (missions.isEmpty()) {
            throw new IllegalStateException("No Baseline missions defined in " + MISSIONS_PATH);
        }
        return List.copyOf(missions);
    }

    private static Map<Identifier, VanillaMission> indexMissions(List<VanillaMission> missions) {
        Map<Identifier, VanillaMission> indexed = new LinkedHashMap<>();
        for (VanillaMission mission : missions) {
            VanillaMission duplicate = indexed.put(mission.id(), mission);
            if (duplicate != null) {
                throw new IllegalStateException("Duplicate Baseline mission id '" + mission.id() + "'");
            }
        }
        return Map.copyOf(indexed);
    }

    private static List<RewardStackDefinition> rewardStacks(JsonArray rewards, String context) {
        List<RewardStackDefinition> stacks = new ArrayList<>();
        for (int i = 0; i < rewards.size(); i++) {
            String rewardContext = context + "[" + i + "]";
            JsonObject reward = asObject(rewards.get(i), rewardContext);
            int count = requiredInt(reward, "count", rewardContext);
            if (count <= 0) {
                throw new IllegalStateException("Reward count must be positive in " + rewardContext);
            }
            stacks.add(new RewardStackDefinition(identifier(requiredString(reward, "item", rewardContext), rewardContext + ".item"), count));
        }
        return List.copyOf(stacks);
    }

    private static JsonObject readObject(String path) {
        InputStream input = openResource(path);
        if (input == null) {
            throw new IllegalStateException("Missing Baseline data resource: " + path);
        }
        try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return asObject(JsonParser.parseReader(reader), path);
        } catch (IOException | JsonParseException exception) {
            throw new IllegalStateException("Invalid Baseline data resource: " + path, exception);
        }
    }

    private static InputStream openResource(String path) {
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        if (contextLoader != null) {
            InputStream input = contextLoader.getResourceAsStream(path);
            if (input != null) {
                return input;
            }
        }
        ClassLoader classLoader = VanillaJourneyDefinitions.class.getClassLoader();
        if (classLoader != null && classLoader != contextLoader) {
            InputStream input = classLoader.getResourceAsStream(path);
            if (input != null) {
                return input;
            }
        }
        InputStream input = VanillaJourneyDefinitions.class.getResourceAsStream("/" + path);
        if (input != null) {
            return input;
        }
        return openDevResource(path);
    }

    private static InputStream openDevResource(String path) {
        Path cursor = Path.of("").toAbsolutePath();
        for (int i = 0; i < 6 && cursor != null; i++) {
            Path local = cursor.resolve("src/main/resources").resolve(path);
            InputStream localInput = openIfExists(local);
            if (localInput != null) {
                return localInput;
            }
            Path workspace = cursor.resolve("addons/echoterminal/src/main/resources").resolve(path);
            InputStream workspaceInput = openIfExists(workspace);
            if (workspaceInput != null) {
                return workspaceInput;
            }
            Path sibling = cursor.resolve("../echoterminal/src/main/resources").normalize().resolve(path);
            InputStream siblingInput = openIfExists(sibling);
            if (siblingInput != null) {
                return siblingInput;
            }
            cursor = cursor.getParent();
        }
        return null;
    }

    private static InputStream openIfExists(Path path) {
        try {
            return Files.isRegularFile(path) ? Files.newInputStream(path) : null;
        } catch (IOException ignored) {
            return null;
        }
    }

    private static JsonObject requiredObject(JsonObject object, String field, String context) {
        if (!object.has(field)) {
            throw new IllegalStateException("Missing required field '" + field + "' in " + context);
        }
        return asObject(object.get(field), context + "." + field);
    }

    private static JsonArray requiredArray(JsonObject object, String field, String context) {
        if (!object.has(field) || !object.get(field).isJsonArray()) {
            throw new IllegalStateException("Missing required array field '" + field + "' in " + context);
        }
        return object.getAsJsonArray(field);
    }

    private static JsonObject asObject(JsonElement element, String context) {
        if (element == null || !element.isJsonObject()) {
            throw new IllegalStateException("Expected JSON object in " + context);
        }
        return element.getAsJsonObject();
    }

    private static String requiredString(JsonObject object, String field, String context) {
        if (!object.has(field) || !object.get(field).isJsonPrimitive()) {
            throw new IllegalStateException("Missing required string field '" + field + "' in " + context);
        }
        String value = object.get(field).getAsString();
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Blank required string field '" + field + "' in " + context);
        }
        return value;
    }

    private static int requiredInt(JsonObject object, String field, String context) {
        if (!object.has(field) || !object.get(field).isJsonPrimitive()) {
            throw new IllegalStateException("Missing required integer field '" + field + "' in " + context);
        }
        return object.get(field).getAsInt();
    }

    private static boolean booleanOrDefault(JsonObject object, String field, boolean fallback) {
        return object.has(field) ? object.get(field).getAsBoolean() : fallback;
    }

    private static Identifier identifier(String value, String context) {
        Identifier id = Identifier.tryParse(value);
        if (id == null) {
            throw new IllegalStateException("Invalid identifier '" + value + "' in " + context);
        }
        return id;
    }

    private static Item item(String value, String context) {
        Identifier id = identifier(value, context);
        return BuiltInRegistries.ITEM.getOptional(id)
                .orElseThrow(() -> new IllegalStateException("Unknown item '" + value + "' in " + context));
    }

    private static TerminalMissionRole role(String value, String context) {
        try {
            return TerminalMissionRole.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Unknown Baseline mission role '" + value + "' in " + context, exception);
        }
    }

    record VanillaMission(
            Identifier id,
            String title,
            String briefing,
            String guide,
            String phaseId,
            String phaseTitle,
            int phaseOrder,
            int missionOrder,
            Item icon,
            RewardTierDefinition tier,
            TerminalMissionRole role) {
        boolean claimable() {
            return tier.claimable();
        }

        List<ItemStack> rewardStacks() {
            return tier.rewardStacks();
        }
    }

    record RewardTierDefinition(String id, String label, boolean claimable, List<RewardStackDefinition> rewards) {
        RewardTierDefinition {
            rewards = List.copyOf(rewards);
        }

        List<ItemStack> rewardStacks() {
            return rewards.stream()
                    .map(reward -> new ItemStack(item(reward.itemId().toString(), "Baseline reward tier '" + id + "'"), reward.count()))
                    .toList();
        }
    }

    record RewardStackDefinition(Identifier itemId, int count) {
    }
}
