package com.knoxhack.echoagriculturereclamation.content;

import com.knoxhack.echoagriculturereclamation.EchoAgricultureReclamation;
import com.knoxhack.echoagriculturereclamation.config.ReclamationConfig;
import com.knoxhack.echocore.api.EchoCoreServices;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ReclamationContent {
   private static final Map<String, ReclamationCropRule> DEFAULT_CROPS = defaultCrops();
   private static final EnumMap<SoilState, ReclamationSoilRule> DEFAULT_SOILS = defaultSoils();
   private static volatile Map<String, ReclamationCropRule> cropRules = DEFAULT_CROPS;
   private static volatile EnumMap<SoilState, ReclamationSoilRule> soilRules = DEFAULT_SOILS;
   private static volatile ReclamationMachineRules machineRules = ReclamationMachineRules.defaults();
   private static volatile ReclamationProgressionRules progressionRules = ReclamationProgressionRules.defaults();
   private static volatile Map<String, ReclamationProcessDefinition> processDefinitions = defaultProcesses();

   private ReclamationContent() {
   }

   public static ReclamationCropRule crop(CropSpec spec) {
      return cropRules.getOrDefault(spec.path(), defaultCropRule(spec));
   }

   public static ReclamationCropRule defaultCropRule(CropSpec spec) {
      return DEFAULT_CROPS.getOrDefault(spec.path(), ReclamationCropRule.defaultFor(spec));
   }

   public static ReclamationSoilRule soil(SoilState state) {
      return soilRules.getOrDefault(state, defaultSoilRule(state));
   }

   public static ReclamationSoilRule defaultSoilRule(SoilState state) {
      return DEFAULT_SOILS.getOrDefault(state, ReclamationSoilRule.defaultFor(state));
   }

   public static ReclamationMachineRules machines() {
      return ReclamationConfig.apply(machineRules);
   }

   public static ReclamationMachineRules defaultMachineRules() {
      return ReclamationMachineRules.defaults();
   }

   public static ReclamationProgressionRules progression() {
      return ReclamationConfig.apply(progressionRules);
   }

   public static ReclamationProgressionRules defaultProgressionRules() {
      return ReclamationProgressionRules.defaults();
   }

   public static Map<String, ReclamationProcessDefinition> processes() {
      return processDefinitions;
   }

   public static Map<String, ReclamationProcessDefinition> defaultProcesses() {
      Map<String, ReclamationProcessDefinition> defaults = new LinkedHashMap<>();
      putProcess(defaults, "seed_vault_analysis", "seed_vault_terminal", "Seed Vault Analysis",
         java.util.List.of("echoagriculturereclamation:recovered_seed_capsule"), java.util.List.of(),
         java.util.List.of("echoagriculturereclamation:contaminated_seed"), 80, 0,
         java.util.List.of("Creates one profiled contaminated seed and records the crop in FIELD > Reclamation."));
      putProcess(defaults, "soil_purifier_enzyme", "soil_purifier", "Enzyme Soil Purification",
         java.util.List.of("reclamation soil"), java.util.List.of("echoagriculturereclamation:purification_enzyme"),
         java.util.List.of("next safer soil state"), 100, 0,
         java.util.List.of("Converts dead, contaminated, irradiated, or toxic local soils without changing biome ids."));
      putProcess(defaults, "gene_stabilization", "gene_stabilizer", "Gene Stabilization",
         java.util.List.of("echoagriculturereclamation:contaminated_seed"), java.util.List.of("echoagriculturereclamation:bio_gel", "echoagriculturereclamation:gene_sample"),
         java.util.List.of("echoagriculturereclamation:stabilized_seed"), 120, 0,
         java.util.List.of("Consumes one catalyst and writes a stable seed profile."));
      putProcess(defaults, "bio_reactor_biomass", "bio_reactor", "Biomass Bio-Reaction",
         java.util.List.of("Agriculture crop matter or seed biomass"), java.util.List.of(),
         java.util.List.of("echoagriculturereclamation:bio_gel"), 120, 0,
         java.util.List.of("Special crops can add enzymes, gene samples, or optional sibling outputs."));
      putProcess(defaults, "compost_recycler_biomass", "compost_recycler", "Biomass Composting",
         java.util.List.of("Agriculture crop matter or seed biomass"), java.util.List.of(),
         java.util.List.of("echoagriculturereclamation:soil_nutrient_mix"), 100, 0,
         java.util.List.of("Returns nutrient mix for hydroponics and purifier passes."));
      return Map.copyOf(defaults);
   }

   public static void replaceJsonContent(LoadedContent loaded) {
      Map<String, ReclamationCropRule> nextCrops = new LinkedHashMap<>(DEFAULT_CROPS);
      loaded.cropRules().forEach((id, rule) -> {
         CropSpec spec = CropSpec.byPath(id);
         if (!spec.path().equals(id)) {
            EchoAgricultureReclamation.LOGGER.warn("Ignoring Agriculture crop rule for unknown crop id '{}'.", id);
         } else {
            nextCrops.put(id, rule.normalized(spec));
         }
      });

      EnumMap<SoilState, ReclamationSoilRule> nextSoils = new EnumMap<>(DEFAULT_SOILS);
      loaded.soilRules().forEach((state, rule) -> nextSoils.put(state, rule.normalized(state)));

      cropRules = Map.copyOf(nextCrops);
      soilRules = nextSoils;
      machineRules = loaded.machineRules().normalized();
      progressionRules = loaded.progressionRules().normalized();
      Map<String, ReclamationProcessDefinition> nextProcesses = new LinkedHashMap<>(defaultProcesses());
      loaded.processDefinitions().forEach((id, process) -> nextProcesses.put(id, process.normalized()));
      processDefinitions = Map.copyOf(nextProcesses);
      EchoAgricultureReclamation.LOGGER.info("Loaded Agriculture Reclamation data rules: {} crops, {} soils, {} processes.",
         cropRules.size(), soilRules.size(), processDefinitions.size());
      EchoCoreServices.invalidateIndexRecipes("agriculture reclamation content changed");
   }

   private static void putProcess(Map<String, ReclamationProcessDefinition> target, String id, String machine, String title,
         java.util.List<String> inputs, java.util.List<String> catalysts, java.util.List<String> outputs, int ticks, int powerCost,
         java.util.List<String> notes) {
      ReclamationProcessDefinition definition = new ReclamationProcessDefinition(id, machine, title, inputs, catalysts, outputs, ticks, powerCost, notes);
      target.put(definition.id(), definition);
   }

   private static Map<String, ReclamationCropRule> defaultCrops() {
      Map<String, ReclamationCropRule> defaults = new LinkedHashMap<>();
      CropSpec.ALL.forEach(spec -> defaults.put(spec.path(), ReclamationCropRule.defaultFor(spec)));
      return Map.copyOf(defaults);
   }

   private static EnumMap<SoilState, ReclamationSoilRule> defaultSoils() {
      EnumMap<SoilState, ReclamationSoilRule> defaults = new EnumMap<>(SoilState.class);
      for (SoilState state : SoilState.values()) {
         defaults.put(state, ReclamationSoilRule.defaultFor(state));
      }
      return defaults;
   }

   public record LoadedContent(
      Map<String, ReclamationCropRule> cropRules,
      EnumMap<SoilState, ReclamationSoilRule> soilRules,
      ReclamationMachineRules machineRules,
      ReclamationProgressionRules progressionRules,
      Map<String, ReclamationProcessDefinition> processDefinitions
   ) {
   }
}
