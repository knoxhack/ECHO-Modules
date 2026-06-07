package com.knoxhack.echoarmory.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.knoxhack.echoarmory.EchoArmory;
import com.knoxhack.echoarmory.block.ArmoryStationBlock.StationKind;
import com.knoxhack.echoarmory.item.ArmoryData;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class ArmoryJsonReloadListener extends SimplePreparableReloadListener<ArmoryContent.LoadedContent> {
   private static final String GEAR_DIR = "echoarmory/gear";
   private static final String MODULE_DIR = "echoarmory/modules";
   private static final String SYNERGY_DIR = "echoarmory/synergies";
   private static final String LOADOUT_DIR = "echoarmory/loadouts";
   private static final String FACTION_UNLOCK_DIR = "echoarmory/faction_unlocks";
   private static final String BOSS_RECOMMENDATION_DIR = "echoarmory/boss_recommendations";
   private static final String STATION_RECIPE_DIR = "echoarmory/station_recipes";
   private static final String FIRING_MODE_DIR = "echoarmory/firing_modes";
   private static final String ROUTE_PROFILE_DIR = "echoarmory/route_profiles";

   @Override
   protected ArmoryContent.LoadedContent prepare(ResourceManager manager, ProfilerFiller profiler) {
      Map<Identifier, GearDefinition> gear = new LinkedHashMap<>();
      Map<Identifier, ModuleDefinition> modules = new LinkedHashMap<>();
      Map<Identifier, SynergyDefinition> synergies = new LinkedHashMap<>();
      Map<Identifier, ArmoryLoadoutDefinition> loadouts = new LinkedHashMap<>();
      Map<Identifier, FactionUnlockDefinition> factionUnlocks = new LinkedHashMap<>();
      Map<Identifier, BossRecommendationDefinition> bossRecommendations = new LinkedHashMap<>();
      Map<Identifier, StationRecipeDefinition> stationRecipes = new LinkedHashMap<>();
      Map<Identifier, FiringModeDefinition> firingModes = new LinkedHashMap<>();
      Map<Identifier, RouteProfileDefinition> routeProfiles = new LinkedHashMap<>();
      load(manager, GEAR_DIR, gear, ArmoryJsonReloadListener::parseGear);
      load(manager, MODULE_DIR, modules, ArmoryJsonReloadListener::parseModule);
      load(manager, SYNERGY_DIR, synergies, ArmoryJsonReloadListener::parseSynergy);
      load(manager, LOADOUT_DIR, loadouts, ArmoryJsonReloadListener::parseLoadout);
      load(manager, FACTION_UNLOCK_DIR, factionUnlocks, ArmoryJsonReloadListener::parseFactionUnlock);
      load(manager, BOSS_RECOMMENDATION_DIR, bossRecommendations, ArmoryJsonReloadListener::parseBossRecommendation);
      load(manager, STATION_RECIPE_DIR, stationRecipes, ArmoryJsonReloadListener::parseStationRecipe);
      load(manager, FIRING_MODE_DIR, firingModes, ArmoryJsonReloadListener::parseFiringMode);
      load(manager, ROUTE_PROFILE_DIR, routeProfiles, ArmoryJsonReloadListener::parseRouteProfile);
      return new ArmoryContent.LoadedContent(gear, modules, synergies, loadouts, factionUnlocks, bossRecommendations,
         stationRecipes, firingModes, routeProfiles);
   }

   @Override
   protected void apply(ArmoryContent.LoadedContent loaded, ResourceManager manager, ProfilerFiller profiler) {
      ArmoryContent.replaceJsonContent(loaded);
   }

   public static GearDefinition parseGearForTests(Identifier id, JsonObject json) {
      return parseGear(id, json);
   }

   public static ModuleDefinition parseModuleForTests(Identifier id, JsonObject json) {
      return parseModule(id, json);
   }

   public static ArmoryLoadoutDefinition parseLoadoutForTests(Identifier id, JsonObject json) {
      return parseLoadout(id, json);
   }

   public static StationRecipeDefinition parseStationRecipeForTests(Identifier id, JsonObject json) {
      return parseStationRecipe(id, json);
   }

   public static FiringModeDefinition parseFiringModeForTests(Identifier id, JsonObject json) {
      return parseFiringMode(id, json);
   }

   public static RouteProfileDefinition parseRouteProfileForTests(Identifier id, JsonObject json) {
      return parseRouteProfile(id, json);
   }

   private static <T> void load(ResourceManager manager, String directory, Map<Identifier, T> target, Parser<T> parser) {
      for (Map.Entry<Identifier, Resource> entry : manager.listResources(directory, id -> id.getPath().endsWith(".json")).entrySet()) {
         Identifier resourceId = entry.getKey();
         Identifier id = contentId(resourceId, directory);
         try (Reader reader = entry.getValue().openAsReader()) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
               throw new JsonParseException("Root must be a JSON object.");
            }
            T parsed = parser.parse(id, root.getAsJsonObject());
            if (target.putIfAbsent(id, parsed) != null) {
               EchoArmory.LOGGER.warn("Duplicate Armory data id {} from {} ignored.", id, resourceId);
            }
         } catch (IOException | RuntimeException exception) {
            EchoArmory.LOGGER.warn("Could not parse Armory data file {}.", resourceId, exception);
         }
      }
   }

   private static Identifier contentId(Identifier resourceId, String directory) {
      String path = resourceId.getPath();
      String prefix = directory + "/";
      if (path.startsWith(prefix)) {
         path = path.substring(prefix.length());
      }
      if (path.endsWith(".json")) {
         path = path.substring(0, path.length() - ".json".length());
      }
      return Identifier.fromNamespaceAndPath(resourceId.getNamespace(), path);
   }

   private static GearDefinition parseGear(Identifier id, JsonObject json) {
      return new GearDefinition(
         id,
         string(json, "title", id.getPath()),
         string(json, "baseType", "utility"),
         integer(json, "tier", 1),
         integer(json, "moduleSlots", 0),
         number(json, "baseDamage", 0.0F),
         integer(json, "baseDefense", 0),
         integer(json, "energyCapacity", 0),
         string(json, "craftingStage", "Tier " + integer(json, "tier", 1)),
         string(json, "factionGate", ""),
         stringList(json, "allowedSlots"),
         stringList(json, "tags")
      );
   }

   private static ModuleDefinition parseModule(Identifier id, JsonObject json) {
      return new ModuleDefinition(
         id,
         string(json, "title", id.getPath()),
         string(json, "slotType", "utility"),
         string(json, "effectType", "utility"),
         number(json, "damageBonus", 0.0F),
         integer(json, "defenseBonus", 0),
         integer(json, "energyCost", 0),
         integer(json, "instability", 0),
         integer(json, "toxicProtection", 0),
         integer(json, "radiationProtection", 0),
         integer(json, "coldProtection", 0),
         integer(json, "heatProtection", 0),
         integer(json, "fractureProtection", 0),
         stringList(json, "compatibleTypes"),
         stringList(json, "synergyTags")
      );
   }

   private static SynergyDefinition parseSynergy(Identifier id, JsonObject json) {
      List<String> requiredTags = stringList(json, "requiredTags");
      if (requiredTags.isEmpty()) {
         throw new JsonParseException("Synergy '" + id + "' must declare requiredTags.");
      }
      return new SynergyDefinition(
         id,
         string(json, "title", id.getPath()),
         requiredTags,
         string(json, "effect", "status"),
         integer(json, "potency", 1),
         string(json, "terminalHint", "")
      );
   }

   private static ArmoryLoadoutDefinition parseLoadout(Identifier id, JsonObject json) {
      if (integer(json, "schemaVersion", 1) >= 2 && json.has("minProtection")) {
         throw new JsonParseException("Loadout '" + id + "' uses removed beta field minProtection; use requiredProtections.");
      }
      return new ArmoryLoadoutDefinition(
         id,
         string(json, "title", id.getPath()),
         integer(json, "order", 0),
         identifier(json, "icon", Identifier.withDefaultNamespace("chest")),
         string(json, "weapon", ""),
         stringList(json, "armor"),
         stringList(json, "modules"),
         integer(json, "minTier", 1),
         integer(json, "minProtection", 0),
         protectionRequirements(json, "requiredProtections", integer(json, "minProtection", 0)),
         string(json, "logisticsPreset", "")
      );
   }

   private static FactionUnlockDefinition parseFactionUnlock(Identifier id, JsonObject json) {
      return new FactionUnlockDefinition(
         id,
         requiredIdentifier(json, "factionId"),
         integer(json, "minReputation", 0),
         string(json, "unlockId", ""),
         string(json, "title", id.getPath())
      );
   }

   private static BossRecommendationDefinition parseBossRecommendation(Identifier id, JsonObject json) {
      return new BossRecommendationDefinition(
         id,
         string(json, "bossName", id.getPath()),
         integer(json, "minTier", 1),
         integer(json, "fractureProtection", 0),
         stringList(json, "recommendedTags"),
         string(json, "hint", "")
      );
   }

   private static StationRecipeDefinition parseStationRecipe(Identifier id, JsonObject json) {
      requireSchemaVersion2(id, json);
      return new StationRecipeDefinition(
         id,
         string(json, "title", id.getPath()),
         StationKind.byName(string(json, "stationKind", "armory_bench")),
         string(json, "operation", ""),
         stringList(json, "gearTags"),
         stringList(json, "moduleTags"),
         stringList(json, "auxItems"),
         string(json, "result", ""),
         integer(json, "energyCost", 0),
         integer(json, "order", 0)
      );
   }

   private static FiringModeDefinition parseFiringMode(Identifier id, JsonObject json) {
      requireSchemaVersion2(id, json);
      return new FiringModeDefinition(
         id,
         string(json, "title", id.getPath()),
         string(json, "family", ""),
         projectileKind(string(json, "projectileKind", "energy_bolt")),
         integer(json, "ammoCost", 1),
         integer(json, "energyCost", 12),
         integer(json, "cooldownTicks", 24),
         number(json, "range", 12.0F),
         number(json, "velocity", 0.8F),
         number(json, "damageScale", 1.0F),
         integer(json, "instability", 0),
         stringList(json, "gearTags")
      );
   }

   private static RouteProfileDefinition parseRouteProfile(Identifier id, JsonObject json) {
      requireSchemaVersion2(id, json);
      return new RouteProfileDefinition(
         id,
         string(json, "title", id.getPath()),
         string(json, "routeFamily", id.getPath()),
         string(json, "hazardType", ""),
         string(json, "bossId", ""),
         string(json, "loadoutId", ""),
         integer(json, "minTier", 1),
         protectionRequirements(json, "requiredProtections", 0),
         stringList(json, "recommendedTags"),
         integer(json, "order", 0)
      );
   }

   private static List<String> stringList(JsonObject json, String key) {
      JsonElement element = json.get(key);
      if (element == null || element.isJsonNull()) {
         return List.of();
      }
      if (!element.isJsonArray()) {
         throw new JsonParseException("Field '" + key + "' must be an array.");
      }
      ArrayList<String> values = new ArrayList<>();
      JsonArray array = element.getAsJsonArray();
      for (JsonElement value : array) {
         if (!value.isJsonPrimitive()) {
            throw new JsonParseException("Field '" + key + "' must contain strings.");
         }
         String text = value.getAsString();
         if (!text.isBlank()) {
            values.add(text.strip());
         }
      }
      return List.copyOf(values);
   }

   private static Map<ArmoryData.ProtectionType, Integer> protectionRequirements(JsonObject json, String key, int fallbackFracture) {
      JsonElement element = json.get(key);
      if (element == null || element.isJsonNull()) {
         return fallbackFracture <= 0
            ? Map.of()
            : Map.of(ArmoryData.ProtectionType.FRACTURE, Math.max(0, fallbackFracture));
      }
      if (!element.isJsonObject()) {
         throw new JsonParseException("Field '" + key + "' must be an object.");
      }
      EnumMap<ArmoryData.ProtectionType, Integer> values = new EnumMap<>(ArmoryData.ProtectionType.class);
      JsonObject object = element.getAsJsonObject();
      for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
         ArmoryData.ProtectionType type = protectionType(entry.getKey());
         if (entry.getValue() == null || entry.getValue().isJsonNull()) {
            continue;
         }
         int required = Math.max(0, entry.getValue().getAsInt());
         if (required > 0) {
            values.put(type, required);
         }
      }
      return Map.copyOf(values);
   }

   private static ArmoryData.ProtectionType protectionType(String key) {
      String normalized = key == null ? "" : key.trim().toUpperCase(Locale.ROOT);
      try {
         return ArmoryData.ProtectionType.valueOf(normalized);
      } catch (IllegalArgumentException exception) {
         throw new JsonParseException("Unknown Armory protection key '" + key + "'.", exception);
      }
   }

   private static FiringModeDefinition.ProjectileKind projectileKind(String key) {
      String normalized = key == null ? "" : key.trim().toUpperCase(Locale.ROOT);
      if ("ENERGY".equals(normalized) || "BOLT".equals(normalized)) {
         normalized = "ENERGY_BOLT";
      } else if ("ARROW".equals(normalized) || "VEIL".equals(normalized)) {
         normalized = "VEIL_ARROW";
      } else if ("CHAKRAM".equals(normalized) || "SIGIL".equals(normalized)) {
         normalized = "SIGIL_CHAKRAM";
      }
      try {
         return FiringModeDefinition.ProjectileKind.valueOf(normalized);
      } catch (IllegalArgumentException exception) {
         throw new JsonParseException("Unknown Armory projectile kind '" + key + "'.", exception);
      }
   }

   private static void requireSchemaVersion2(Identifier id, JsonObject json) {
      int version = integer(json, "schemaVersion", 0);
      if (version != 2) {
         throw new JsonParseException("Armory content '" + id + "' must declare schemaVersion 2.");
      }
   }

   private static Identifier identifier(JsonObject json, String key, Identifier fallback) {
      String value = string(json, key, "");
      return value.isBlank() ? fallback : Identifier.parse(value);
   }

   private static Identifier requiredIdentifier(JsonObject json, String key) {
      String value = string(json, key, "");
      if (value.isBlank()) {
         throw new JsonParseException("Missing required identifier field '" + key + "'.");
      }
      return Identifier.parse(value);
   }

   private static String string(JsonObject json, String key, String fallback) {
      JsonElement element = json.get(key);
      return element == null || element.isJsonNull() ? fallback : element.getAsString();
   }

   private static int integer(JsonObject json, String key, int fallback) {
      JsonElement element = json.get(key);
      return element == null || element.isJsonNull() ? fallback : element.getAsInt();
   }

   private static float number(JsonObject json, String key, float fallback) {
      JsonElement element = json.get(key);
      return element == null || element.isJsonNull() ? fallback : element.getAsFloat();
   }

   @FunctionalInterface
   private interface Parser<T> {
      T parse(Identifier id, JsonObject json);
   }
}
