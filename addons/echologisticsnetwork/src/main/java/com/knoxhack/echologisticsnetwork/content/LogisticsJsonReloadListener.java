package com.knoxhack.echologisticsnetwork.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.knoxhack.echologisticsnetwork.EchoLogisticsNetwork;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class LogisticsJsonReloadListener extends SimplePreparableReloadListener<LogisticsContent.LoadedContent> {
   private static final String CATEGORY_DIR = "echologisticsnetwork/categories";
   private static final String LOADOUT_DIR = "echologisticsnetwork/loadouts";
   private static final String OFFER_DIR = "echologisticsnetwork/faction_offers";

   @Override
   protected LogisticsContent.LoadedContent prepare(ResourceManager manager, ProfilerFiller profiler) {
      Map<Identifier, SupplyCategory> categories = new LinkedHashMap<>();
      Map<Identifier, LoadoutPreset> loadouts = new LinkedHashMap<>();
      Map<Identifier, FactionDepotOffer> offers = new LinkedHashMap<>();
      load(manager, CATEGORY_DIR, categories, LogisticsJsonReloadListener::parseCategory);
      load(manager, LOADOUT_DIR, loadouts, LogisticsJsonReloadListener::parseLoadout);
      load(manager, OFFER_DIR, offers, LogisticsJsonReloadListener::parseOffer);
      return new LogisticsContent.LoadedContent(categories, loadouts, offers);
   }

   @Override
   protected void apply(LogisticsContent.LoadedContent loaded, ResourceManager manager, ProfilerFiller profiler) {
      LogisticsContent.replaceJsonContent(loaded);
   }

   public static SupplyCategory parseCategoryForTests(Identifier id, JsonObject json) {
      return parseCategory(id, json);
   }

   public static LoadoutPreset parseLoadoutForTests(Identifier id, JsonObject json) {
      return parseLoadout(id, json);
   }

   public static FactionDepotOffer parseOfferForTests(Identifier id, JsonObject json) {
      return parseOffer(id, json);
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
               EchoLogisticsNetwork.LOGGER.warn("Duplicate Logistics data id {} from {} ignored.", id, resourceId);
            }
         } catch (IOException | RuntimeException exception) {
            EchoLogisticsNetwork.LOGGER.warn("Could not parse Logistics data file {}.", resourceId, exception);
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

   private static SupplyCategory parseCategory(Identifier id, JsonObject json) {
      return new SupplyCategory(
         id,
         string(json, "title", id.getPath()),
         integer(json, "order", 0),
         color(json, "accentColor", 0xFF66E8FF),
         integer(json, "lowStockTarget", 0),
         identifier(json, "tagId", Identifier.fromNamespaceAndPath(id.getNamespace(), "echo_logistics/" + id.getPath()))
      );
   }

   private static LoadoutPreset parseLoadout(Identifier id, JsonObject json) {
      List<LoadoutRequirement> requirements = new ArrayList<>();
      JsonArray array = requiredArray(json, "requirements");
      if (array.isEmpty()) {
         throw new JsonParseException("Loadout '" + id + "' must declare at least one requirement.");
      }
      for (JsonElement element : array) {
         if (!element.isJsonObject()) {
            throw new JsonParseException("Loadout '" + id + "' requirements must be objects.");
         }
         requirements.add(parseRequirement(element.getAsJsonObject()));
      }
      List<Identifier> targetBlocks = new ArrayList<>();
      JsonArray blocks = array(json, "targetBlockTypes");
      if (blocks != null) {
         for (JsonElement block : blocks) {
            if (!block.isJsonPrimitive()) {
               throw new JsonParseException("Loadout '" + id + "' targetBlockTypes must be strings.");
            }
            targetBlocks.add(Identifier.parse(block.getAsString()));
         }
      }
      return new LoadoutPreset(
         id,
         string(json, "title", id.getPath()),
         integer(json, "order", 0),
         identifier(json, "icon", Identifier.withDefaultNamespace("chest")),
         requirements,
         targetBlocks,
         integer(json, "deliveryTicks", 160),
         parseRestockPolicy(json)
      );
   }

   private static FactoryRestockPolicy parseRestockPolicy(JsonObject json) {
      Identifier taskId = json.has("factoryTaskId") && !json.get("factoryTaskId").getAsString().isBlank()
         ? Identifier.parse(json.get("factoryTaskId").getAsString())
         : null;
      int targetRuns = integer(json, "restockTargetRuns", taskId == null ? 0 : 3);
      int minRuns = integer(json, "restockMinRuns", targetRuns > 0 ? 1 : 0);
      int maxInFlight = integer(json, "restockMaxInFlight", 1);
      int cooldownTicks = integer(json, "restockCooldownTicks", 200);
      return new FactoryRestockPolicy(taskId, targetRuns, minRuns, maxInFlight, cooldownTicks);
   }

   private static LoadoutRequirement parseRequirement(JsonObject json) {
      int count = positiveInteger(json, "count", 1);
      boolean optional = bool(json, "optional", false);
      int selectors = (json.has("item") ? 1 : 0) + (json.has("tag") ? 1 : 0) + (json.has("category") ? 1 : 0);
      if (selectors != 1) {
         throw new JsonParseException("Loadout requirement must declare exactly one of item, tag, or category.");
      }
      if (json.has("item")) {
         return new LoadoutRequirement(LoadoutRequirement.Kind.ITEM, requiredIdentifier(json, "item"), count, optional);
      }
      if (json.has("tag")) {
         return new LoadoutRequirement(LoadoutRequirement.Kind.TAG, requiredIdentifier(json, "tag"), count, optional);
      }
      return new LoadoutRequirement(LoadoutRequirement.Kind.CATEGORY, requiredIdentifier(json, "category"), count, optional);
   }

   private static FactionDepotOffer parseOffer(Identifier id, JsonObject json) {
      return new FactionDepotOffer(
         id,
         identifier(json, "factionId", Identifier.fromNamespaceAndPath(id.getNamespace(), "logistics_exchange")),
         requiredStackSpec(json, "input"),
         requiredStackSpec(json, "output"),
         integer(json, "reputationDelta", 0),
         integer(json, "minReputation", 0),
         integer(json, "cooldownTicks", 1200)
      );
   }

   private static FactionDepotOffer.StackSpec requiredStackSpec(JsonObject json, String key) {
      JsonObject stack = requiredObject(json, key);
      Identifier item = requiredIdentifier(stack, "item");
      int count = positiveInteger(stack, "count", 1);
      return new FactionDepotOffer.StackSpec(item, count);
   }

   private static JsonObject requiredObject(JsonObject json, String key) {
      JsonElement element = json.get(key);
      if (element == null || element.isJsonNull()) {
         throw new JsonParseException("Missing required object field '" + key + "'.");
      }
      if (!element.isJsonObject()) {
         throw new JsonParseException("Field '" + key + "' must be an object.");
      }
      return element.getAsJsonObject();
   }

   private static JsonObject object(JsonObject json, String key) {
      JsonElement element = json.get(key);
      if (element == null || element.isJsonNull()) {
         return null;
      }
      if (!element.isJsonObject()) {
         throw new JsonParseException("Field '" + key + "' must be an object.");
      }
      return element.getAsJsonObject();
   }

   private static JsonArray requiredArray(JsonObject json, String key) {
      JsonElement element = json.get(key);
      if (element == null || element.isJsonNull()) {
         throw new JsonParseException("Missing required array field '" + key + "'.");
      }
      if (!element.isJsonArray()) {
         throw new JsonParseException("Field '" + key + "' must be an array.");
      }
      return element.getAsJsonArray();
   }

   private static JsonArray array(JsonObject json, String key) {
      JsonElement element = json.get(key);
      if (element == null || element.isJsonNull()) {
         return null;
      }
      if (!element.isJsonArray()) {
         throw new JsonParseException("Field '" + key + "' must be an array.");
      }
      return element.getAsJsonArray();
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
      if (element == null || element.isJsonNull()) {
         return fallback;
      }
      if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
         String value = element.getAsString().strip();
         if (value.isBlank()) {
            return fallback;
         }
         try {
            return Integer.decode(value);
         } catch (NumberFormatException exception) {
            throw new JsonParseException("Field '" + key + "' must be an integer.", exception);
         }
      }
      return element.getAsInt();
   }

   private static int color(JsonObject json, String key, int fallback) {
      JsonElement element = json.get(key);
      if (element == null || element.isJsonNull()) {
         return fallback;
      }
      if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
         String value = element.getAsString().strip();
         if (value.isBlank()) {
            return fallback;
         }
         String normalized = value.startsWith("#") ? "0x" + value.substring(1) : value;
         try {
            long parsed = Long.decode(normalized);
            return (int)parsed;
         } catch (NumberFormatException exception) {
            throw new JsonParseException("Field '" + key + "' must be an integer, 0xAARRGGBB, or #AARRGGBB color.", exception);
         }
      }
      return element.getAsInt();
   }

   private static boolean bool(JsonObject json, String key, boolean fallback) {
      JsonElement element = json.get(key);
      return element == null || element.isJsonNull() ? fallback : element.getAsBoolean();
   }

   private static int positiveInteger(JsonObject json, String key, int fallback) {
      int value = integer(json, key, fallback);
      if (value <= 0) {
         throw new JsonParseException("Field '" + key + "' must be greater than zero.");
      }
      return value;
   }

   @FunctionalInterface
   private interface Parser<T> {
      T parse(Identifier id, JsonObject json);
   }
}
