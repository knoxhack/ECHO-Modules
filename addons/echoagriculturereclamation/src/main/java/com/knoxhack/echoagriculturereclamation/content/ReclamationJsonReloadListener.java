package com.knoxhack.echoagriculturereclamation.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.knoxhack.echoagriculturereclamation.EchoAgricultureReclamation;
import java.io.IOException;
import java.io.Reader;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class ReclamationJsonReloadListener extends SimplePreparableReloadListener<ReclamationContent.LoadedContent> {
   private static final String CROP_DIR = "echoagriculturereclamation/crops";
   private static final String SOIL_DIR = "echoagriculturereclamation/soil";
   private static final String MACHINE_DIR = "echoagriculturereclamation/machines";
   private static final String PROGRESSION_DIR = "echoagriculturereclamation/progression";
   private static final String PROCESS_DIR = "echoagriculturereclamation/processes";

   @Override
   protected ReclamationContent.LoadedContent prepare(ResourceManager manager, ProfilerFiller profiler) {
      Map<String, ReclamationCropRule> crops = new LinkedHashMap<>();
      EnumMap<SoilState, ReclamationSoilRule> soils = new EnumMap<>(SoilState.class);
      ReclamationMachineRules machines = ReclamationContent.defaultMachineRules();
      ReclamationProgressionRules progression = ReclamationContent.defaultProgressionRules();
      Map<String, ReclamationProcessDefinition> processes = new LinkedHashMap<>();

      loadCrops(manager, crops);
      loadSoils(manager, soils);
      for (JsonObject json : jsonObjects(manager, MACHINE_DIR).values()) {
         machines = parseMachines(json, machines);
      }
      for (JsonObject json : jsonObjects(manager, PROGRESSION_DIR).values()) {
         progression = parseProgression(json, progression);
      }
      loadProcesses(manager, processes);
      return new ReclamationContent.LoadedContent(crops, soils, machines, progression, processes);
   }

   @Override
   protected void apply(ReclamationContent.LoadedContent loaded, ResourceManager manager, ProfilerFiller profiler) {
      ReclamationContent.replaceJsonContent(loaded);
   }

   private static void loadCrops(ResourceManager manager, Map<String, ReclamationCropRule> target) {
      for (Map.Entry<Identifier, JsonObject> entry : jsonObjects(manager, CROP_DIR).entrySet()) {
         Identifier id = entry.getKey();
         JsonObject json = entry.getValue();
         JsonObject aggregate = object(json, "crops");
         if (aggregate != null) {
            for (Map.Entry<String, JsonElement> cropEntry : aggregate.entrySet()) {
               if (cropEntry.getValue().isJsonObject()) {
                  putCrop(target, cropEntry.getKey(), cropEntry.getValue().getAsJsonObject());
               }
            }
         } else {
            putCrop(target, string(json, "crop", id.getPath()), json);
         }
      }
   }

   private static void putCrop(Map<String, ReclamationCropRule> target, String cropId, JsonObject json) {
      CropSpec spec = CropSpec.byPath(cropId);
      ReclamationCropRule fallback = ReclamationContent.defaultCropRule(spec);
      target.put(cropId, new ReclamationCropRule(
         cropId,
         integer(json, "baseGrowthChance", fallback.baseGrowthChance()),
         integer(json, "baseYield", fallback.baseYield()),
         integer(json, "restorationWeight", fallback.restorationWeight()),
         integer(json, "greenhouseBypassThreshold", fallback.greenhouseBypassThreshold()),
         integer(json, "soilGrowthDivisor", fallback.soilGrowthDivisor()),
         integer(json, "greenhouseGrowthDivisor", fallback.greenhouseGrowthDivisor()),
         integer(json, "stabilityGrowthDivisor", fallback.stabilityGrowthDivisor()),
         integer(json, "nutrientGrowthBonus", fallback.nutrientGrowthBonus()),
         integer(json, "stableGrowthBonus", fallback.stableGrowthBonus()),
         integer(json, "hydroponicYieldBonus", fallback.hydroponicYieldBonus()),
         integer(json, "stableYieldBonus", fallback.stableYieldBonus()),
         integer(json, "safeGreenhouseYieldBonus", fallback.safeGreenhouseYieldBonus()),
         integer(json, "contaminatedSeedReturnCeiling", fallback.contaminatedSeedReturnCeiling()),
         integer(json, "seedStabilityLoss", fallback.seedStabilityLoss()),
         integer(json, "seedContaminationIncrease", fallback.seedContaminationIncrease()),
         integer(json, "failedGrowthDeathChance", fallback.failedGrowthDeathChance())
      ));
   }

   private static void loadSoils(ResourceManager manager, EnumMap<SoilState, ReclamationSoilRule> target) {
      for (Map.Entry<Identifier, JsonObject> entry : jsonObjects(manager, SOIL_DIR).entrySet()) {
         Identifier id = entry.getKey();
         JsonObject json = entry.getValue();
         JsonObject aggregate = object(json, "soil");
         if (aggregate != null) {
            for (Map.Entry<String, JsonElement> soilEntry : aggregate.entrySet()) {
               if (soilEntry.getValue().isJsonObject()) {
                  putSoil(target, soilEntry.getKey(), soilEntry.getValue().getAsJsonObject());
               }
            }
         } else {
            putSoil(target, string(json, "state", id.getPath()), json);
         }
      }
   }

   private static void putSoil(EnumMap<SoilState, ReclamationSoilRule> target, String stateName, JsonObject json) {
      SoilState state = soilState(stateName);
      if (state == null) {
         EchoAgricultureReclamation.LOGGER.warn("Ignoring Agriculture soil rule for unknown soil state '{}'.", stateName);
         return;
      }
      ReclamationSoilRule fallback = ReclamationContent.defaultSoilRule(state);
      target.put(state, new ReclamationSoilRule(
         state,
         integer(json, "growthChance", fallback.growthChance()),
         integer(json, "restorationGain", fallback.restorationGain()),
         bool(json, "safe", fallback.safe()),
         categories(json, "supportedCategories", fallback.supportedCategories()),
         categories(json, "stabilizedSupportedCategories", fallback.stabilizedSupportedCategories()),
         integer(json, "stabilizedSupportMinStability", fallback.stabilizedSupportMinStability())
      ));
   }

   private static ReclamationMachineRules parseMachines(JsonObject json, ReclamationMachineRules fallback) {
      return new ReclamationMachineRules(
         integer(json, "soilPurifierRadius", fallback.soilPurifierRadius()),
         integer(json, "soilPurifierEnzymeBlocks", fallback.soilPurifierEnzymeBlocks()),
         integer(json, "soilPurifierNutrientBlocks", fallback.soilPurifierNutrientBlocks()),
         integer(json, "hydroponicGrowthTicks", fallback.hydroponicGrowthTicks()),
         integer(json, "hydroponicNutrientCap", fallback.hydroponicNutrientCap()),
         integer(json, "hydroponicNutrientPerMix", fallback.hydroponicNutrientPerMix()),
         integer(json, "bioReactorOrganicOutput", fallback.bioReactorOrganicOutput()),
         integer(json, "bioReactorGeneSampleOutput", fallback.bioReactorGeneSampleOutput()),
         integer(json, "compostRecyclerOutput", fallback.compostRecyclerOutput()),
         integer(json, "greenhouseHorizontalRange", fallback.greenhouseHorizontalRange()),
         integer(json, "greenhouseDownRange", fallback.greenhouseDownRange()),
         integer(json, "greenhouseUpRange", fallback.greenhouseUpRange()),
         integer(json, "greenhouseGlassWeight", fallback.greenhouseGlassWeight()),
         integer(json, "greenhouseFilterWeight", fallback.greenhouseFilterWeight()),
         integer(json, "greenhouseDockWeight", fallback.greenhouseDockWeight()),
         integer(json, "greenhouseControllerWeight", fallback.greenhouseControllerWeight()),
         integer(json, "greenhouseTrayWeight", fallback.greenhouseTrayWeight()),
         integer(json, "pollinatorDroneServiceRadius", fallback.pollinatorDroneServiceRadius()),
         integer(json, "pollinatorDroneHomeRadius", fallback.pollinatorDroneHomeRadius()),
         integer(json, "pollinatorDroneServiceTicks", fallback.pollinatorDroneServiceTicks()),
         integer(json, "pollinatorDroneGrowthBonus", fallback.pollinatorDroneGrowthBonus())
      );
   }

   private static ReclamationProgressionRules parseProgression(JsonObject json, ReclamationProgressionRules fallback) {
      return new ReclamationProgressionRules(
         integer(json, "greenhouseSafeThreshold", fallback.greenhouseSafeThreshold()),
         integer(json, "foodKnownSeedBonus", fallback.foodKnownSeedBonus()),
         integer(json, "foodItemValue", fallback.foodItemValue()),
         integer(json, "recoveredSeedMinStability", fallback.recoveredSeedMinStability()),
         integer(json, "recoveredSeedStabilityRange", fallback.recoveredSeedStabilityRange()),
         integer(json, "recoveredSeedMinContamination", fallback.recoveredSeedMinContamination()),
         integer(json, "recoveredSeedContaminationRange", fallback.recoveredSeedContaminationRange()),
         integer(json, "scannerUnsafeRestorationGain", fallback.scannerUnsafeRestorationGain()),
         integer(json, "scannerSafeRestorationGain", fallback.scannerSafeRestorationGain()),
         integer(json, "purifyThreshold", fallback.purifyThreshold()),
         integer(json, "stabilizeThreshold", fallback.stabilizeThreshold()),
         integer(json, "restoreThreshold", fallback.restoreThreshold()),
         integer(json, "cropPurifyMaxLow", fallback.cropPurifyMaxLow()),
         integer(json, "cropPurifyMaxHigh", fallback.cropPurifyMaxHigh()),
         integer(json, "cropStabilizeMax", fallback.cropStabilizeMax()),
         integer(json, "cropRestoreMax", fallback.cropRestoreMax()),
         integer(json, "scannerPurifyMaxLow", fallback.scannerPurifyMaxLow()),
         integer(json, "scannerPurifyMaxHigh", fallback.scannerPurifyMaxHigh()),
         integer(json, "scannerStabilizeMax", fallback.scannerStabilizeMax()),
         integer(json, "scannerRestoreMax", fallback.scannerRestoreMax()),
         integer(json, "restorationCropWeightForStabilization", fallback.restorationCropWeightForStabilization())
      );
   }

   private static void loadProcesses(ResourceManager manager, Map<String, ReclamationProcessDefinition> target) {
      for (Map.Entry<Identifier, JsonObject> entry : jsonObjects(manager, PROCESS_DIR).entrySet()) {
         Identifier id = entry.getKey();
         JsonObject json = entry.getValue();
         JsonObject aggregate = object(json, "processes");
         if (aggregate != null) {
            for (Map.Entry<String, JsonElement> processEntry : aggregate.entrySet()) {
               if (processEntry.getValue().isJsonObject()) {
                  putProcess(target, processEntry.getKey(), processEntry.getValue().getAsJsonObject());
               }
            }
         } else {
            putProcess(target, string(json, "id", id.getPath()), json);
         }
      }
   }

   private static void putProcess(Map<String, ReclamationProcessDefinition> target, String processId, JsonObject json) {
      ReclamationProcessDefinition definition = new ReclamationProcessDefinition(
         processId,
         string(json, "machine", "generic"),
         string(json, "title", processId),
         strings(json, "inputs"),
         strings(json, "catalysts"),
         strings(json, "outputs"),
         integer(json, "ticks", 0),
         integer(json, "powerCost", 0),
         strings(json, "notes")
      );
      target.put(definition.id(), definition);
   }

   private static Map<Identifier, JsonObject> jsonObjects(ResourceManager manager, String directory) {
      Map<Identifier, JsonObject> objects = new LinkedHashMap<>();
      for (Map.Entry<Identifier, Resource> entry : manager.listResources(directory, id -> id.getPath().endsWith(".json")).entrySet()) {
         Identifier resourceId = entry.getKey();
         Identifier id = contentId(resourceId, directory);
         try (Reader reader = entry.getValue().openAsReader()) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
               throw new JsonParseException("Root must be a JSON object.");
            }
            objects.put(id, root.getAsJsonObject());
         } catch (IOException | RuntimeException exception) {
            EchoAgricultureReclamation.LOGGER.warn("Could not parse Agriculture data file {}.", resourceId, exception);
         }
      }
      return objects;
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

   private static JsonObject object(JsonObject json, String key) {
      JsonElement element = json.get(key);
      return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
   }

   private static JsonArray array(JsonObject json, String key) {
      JsonElement element = json.get(key);
      return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
   }

   private static String string(JsonObject json, String key, String fallback) {
      JsonElement element = json.get(key);
      return element == null || element.isJsonNull() ? fallback : element.getAsString();
   }

   private static int integer(JsonObject json, String key, int fallback) {
      JsonElement element = json.get(key);
      return element == null || element.isJsonNull() ? fallback : element.getAsInt();
   }

   private static boolean bool(JsonObject json, String key, boolean fallback) {
      JsonElement element = json.get(key);
      return element == null || element.isJsonNull() ? fallback : element.getAsBoolean();
   }

   private static Set<CropCategory> categories(JsonObject json, String key, Set<CropCategory> fallback) {
      JsonArray values = array(json, key);
      if (values == null) {
         return fallback;
      }
      EnumSet<CropCategory> categories = EnumSet.noneOf(CropCategory.class);
      for (JsonElement value : values) {
         String name = normalize(value.getAsString());
         if ("ALL".equals(name)) {
            categories.addAll(EnumSet.allOf(CropCategory.class));
         } else if ("NON_NEXUS".equals(name)) {
            categories.addAll(ReclamationSoilRule.nonNexus());
         } else {
            categories.add(CropCategory.valueOf(name));
         }
      }
      return categories.isEmpty() ? Set.of() : Set.copyOf(categories);
   }

   private static java.util.List<String> strings(JsonObject json, String key) {
      JsonArray values = array(json, key);
      if (values == null) {
         return java.util.List.of();
      }
      java.util.List<String> result = new java.util.ArrayList<>();
      for (JsonElement value : values) {
         if (value != null && !value.isJsonNull()) {
            result.add(value.getAsString());
         }
      }
      return java.util.List.copyOf(result);
   }

   private static SoilState soilState(String value) {
      try {
         return SoilState.valueOf(normalize(value));
      } catch (IllegalArgumentException exception) {
         return null;
      }
   }

   private static String normalize(String value) {
      return value.trim().toUpperCase(java.util.Locale.ROOT).replace('-', '_').replace(' ', '_');
   }
}
