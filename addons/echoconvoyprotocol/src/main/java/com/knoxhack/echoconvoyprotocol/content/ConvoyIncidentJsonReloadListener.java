package com.knoxhack.echoconvoyprotocol.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.knoxhack.echoconvoyprotocol.EchoConvoyProtocol;
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

public final class ConvoyIncidentJsonReloadListener extends SimplePreparableReloadListener<Map<Identifier, ConvoyIncidentProfile>> {
   private static final String DIRECTORY = "convoy_incidents";

   @Override
   protected Map<Identifier, ConvoyIncidentProfile> prepare(ResourceManager manager, ProfilerFiller profiler) {
      Map<Identifier, ConvoyIncidentProfile> profiles = new LinkedHashMap<>();
      for (Map.Entry<Identifier, Resource> entry : manager.listResources(DIRECTORY, id -> id.getPath().endsWith(".json")).entrySet()) {
         Identifier resourceId = entry.getKey();
         Identifier profileId = contentId(resourceId);
         try (Reader reader = entry.getValue().openAsReader()) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
               throw new JsonParseException("Root must be a JSON object.");
            }
            profiles.put(profileId, parseProfile(profileId, root.getAsJsonObject()));
         } catch (IOException | RuntimeException exception) {
            EchoConvoyProtocol.LOGGER.warn("Could not parse Convoy incident profile {}.", resourceId, exception);
         }
      }
      return profiles;
   }

   @Override
   protected void apply(Map<Identifier, ConvoyIncidentProfile> profiles, ResourceManager manager, ProfilerFiller profiler) {
      ConvoyContent.replaceJsonIncidents(profiles);
   }

   public static ConvoyIncidentProfile parseProfileForTests(Identifier id, JsonObject json) {
      return parseProfile(id, json);
   }

   private static ConvoyIncidentProfile parseProfile(Identifier profileId, JsonObject json) {
      JsonArray array = array(json, "incidents");
      if (array == null) {
         throw new JsonParseException("Convoy incident profile " + profileId + " is missing incidents array.");
      }
      List<ConvoyIncidentDefinition> incidents = new ArrayList<>();
      int index = 0;
      for (JsonElement element : array) {
         if (!element.isJsonObject()) {
            throw new JsonParseException("Convoy incident profile " + profileId + " incident " + index + " must be an object.");
         }
         JsonObject object = element.getAsJsonObject();
         incidents.add(new ConvoyIncidentDefinition(
            profileId,
            identifier(object, "id", Identifier.fromNamespaceAndPath(profileId.getNamespace(), profileId.getPath() + "_" + index)),
            stageIndex(object),
            string(object, "displayText", string(object, "text", "Convoy field incident detected.")),
            boundedInteger(profileId, object, "readinessThreshold", 60, 0, 100),
            integer(object, "fuelEffect", 0),
            integer(object, "integrityEffect", 0),
            integer(object, "cargoEffect", 0),
            nonNegativeInteger(profileId, object, "delayTicks", 0),
            identifier(object, "requiredResponseTask", Identifier.fromNamespaceAndPath(EchoConvoyProtocol.MODID, "resolve_field_incident")),
            string(object, "holomapMarkerHint", "field_incident")
         ));
         index++;
      }
      return new ConvoyIncidentProfile(profileId, incidents);
   }

   private static int stageIndex(JsonObject json) {
      if (json.has("stage")) {
         return Math.max(0, json.get("stage").getAsInt() - 1);
      }
      String stageId = string(json, "stageId", "");
      if (stageId.startsWith("stage_")) {
         try {
            return Math.max(0, Integer.parseInt(stageId.substring("stage_".length())) - 1);
         } catch (NumberFormatException ignored) {
            return 0;
         }
      }
      return 0;
   }

   private static Identifier contentId(Identifier resourceId) {
      String path = resourceId.getPath();
      String prefix = DIRECTORY + "/";
      if (path.startsWith(prefix)) {
         path = path.substring(prefix.length());
      }
      if (path.endsWith(".json")) {
         path = path.substring(0, path.length() - ".json".length());
      }
      return Identifier.fromNamespaceAndPath(resourceId.getNamespace(), path);
   }

   private static JsonArray array(JsonObject json, String key) {
      JsonElement element = json.get(key);
      return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
   }

   private static Identifier identifier(JsonObject json, String key, Identifier fallback) {
      String value = string(json, key, "");
      return value.isBlank() ? fallback : Identifier.parse(value);
   }

   private static String string(JsonObject json, String key, String fallback) {
      if (json == null) {
         return fallback;
      }
      JsonElement element = json.get(key);
      return element == null || element.isJsonNull() ? fallback : element.getAsString();
   }

   private static int integer(JsonObject json, String key, int fallback) {
      if (json == null) {
         return fallback;
      }
      JsonElement element = json.get(key);
      return element == null || element.isJsonNull() ? fallback : element.getAsInt();
   }

   private static int nonNegativeInteger(Identifier profileId, JsonObject json, String key, int fallback) {
      int value = integer(json, key, fallback);
      if (value < 0) {
         throw new JsonParseException("Convoy incident profile " + profileId + " field '" + key + "' must be non-negative.");
      }
      return value;
   }

   private static int boundedInteger(Identifier profileId, JsonObject json, String key, int fallback, int min, int max) {
      int value = integer(json, key, fallback);
      if (value < min || value > max) {
         throw new JsonParseException("Convoy incident profile " + profileId + " field '" + key + "' must be between " + min + " and " + max + ".");
      }
      return value;
   }
}
