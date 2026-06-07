package com.knoxhack.echoagriculturereclamation;

import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import com.knoxhack.echoagriculturereclamation.content.ReclamationMachineRules;
import com.knoxhack.echoagriculturereclamation.content.ReclamationProcessDefinition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoAgricultureReclamationNativeModule implements EchoNativeSurfaceModuleEntrypoint {
   public static final String MODULE_ID = "echoagriculturereclamation";
   public static final String GREENHOUSE_BLOCK_CONTRACT_ID = "echoagriculturereclamation:block/greenhouse_machine_rules";
   public static final String SEED_ITEM_CONTRACT_ID = "echoagriculturereclamation:item/seed_supply_process";
   public static final String DASHBOARD_UI_CONTRACT_ID = "echoagriculturereclamation:ui/reclamation_process_cards";
   public static final String RESTORATION_WORLDGEN_CONTRACT_ID = "echoagriculturereclamation:worldgen/restoration_envelope";
   public static final List<String> CONTRACT_IDS = List.of(
      GREENHOUSE_BLOCK_CONTRACT_ID,
      SEED_ITEM_CONTRACT_ID,
      DASHBOARD_UI_CONTRACT_ID,
      RESTORATION_WORLDGEN_CONTRACT_ID
   );

   public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
      Map<String, Object> referenceProbe = exerciseReferenceBehavior();
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("activated", true);
      result.put("activationStage", "agriculture_reclamation_native_contract_active");
      result.put("adapterCoreUsed", true);
      result.put("nativeAdapterCodeExecuted", true);
      result.put("serviceCodeExecuted", true);
      result.put("moduleId", MODULE_ID);
      result.put("packId", context.getOrDefault("packId", "unknown"));
      result.put("registeredFeatureContracts", CONTRACT_IDS);
      result.put("logicalRegistrationCount", CONTRACT_IDS.size());
      result.put("adapterDomains", List.of("blocks", "items", "ui_screens", "worldgen"));
      result.put("runtimeTargets", List.of("echo_native", "echo_runtime_standalone"));
      result.put("greenhouseMachineRulesRoundTrip", referenceProbe.get("greenhouseMachineRulesRoundTrip"));
      result.put("seedSupplyProcessRoundTrip", referenceProbe.get("seedSupplyProcessRoundTrip"));
      result.put("processCardRoundTrip", referenceProbe.get("processCardRoundTrip"));
      result.put("restorationEnvelopeRoundTrip", referenceProbe.get("restorationEnvelopeRoundTrip"));
      result.put("referenceProbe", referenceProbe);
      result.put("registryInjected", false);
      result.put("registryMutated", false);
      result.put("transformsPerformed", false);
      result.put("summary", "Agriculture Reclamation native contract exercised greenhouse machine tuning, seed/process outputs, process-card normalization, and restoration envelope behavior.");
      return Map.copyOf(result);
   }

   public static void main(String[] args) {
      Map<String, Object> activation = new EchoAgricultureReclamationNativeModule()
         .describeNativeSurfaces(Map.of("packId", "agriculture-reclamation-smoke"));
      require(Boolean.TRUE.equals(activation.get("activated")),
         "Agriculture Reclamation native adapter should activate");
      require(Boolean.TRUE.equals(activation.get("greenhouseMachineRulesRoundTrip")),
         "Agriculture Reclamation native adapter should exercise greenhouse machine rules");
      require(Boolean.TRUE.equals(activation.get("seedSupplyProcessRoundTrip")),
         "Agriculture Reclamation native adapter should exercise seed supply process behavior");
      require(Boolean.TRUE.equals(activation.get("processCardRoundTrip")),
         "Agriculture Reclamation native adapter should exercise UI process card behavior");
      require(Boolean.TRUE.equals(activation.get("restorationEnvelopeRoundTrip")),
         "Agriculture Reclamation native adapter should exercise restoration envelope behavior");
      System.out.println("agriculture reclamation native adapter smoke PASS contracts=" + CONTRACT_IDS.size());
   }

   private Map<String, Object> exerciseReferenceBehavior() {
      ReclamationMachineRules defaults = ReclamationMachineRules.defaults();
      ReclamationMachineRules normalized = new ReclamationMachineRules(
         -1,
         -4,
         -5,
         0,
         0,
         0,
         0,
         0,
         0,
         6,
         4,
         6,
         2,
         18,
         14,
         10,
         4,
         0,
         0,
         1,
         -2
      ).normalized();
      ReclamationProcessDefinition seedProcess = new ReclamationProcessDefinition(
         "Seed Vault Analysis",
         "Seed Vault Terminal",
         "",
         List.of("echoagriculturereclamation:recovered_seed_capsule", " "),
         List.of(),
         List.of("echoagriculturereclamation:contaminated_seed"),
         80,
         -10,
         List.of("Creates one profiled contaminated seed.")
      );
      ReclamationProcessDefinition uiProcess = new ReclamationProcessDefinition(
         "Bio Reactor Biomass",
         "Bio Reactor",
         "Biomass Bio-Reaction",
         List.of("Agriculture crop matter"),
         List.of(),
         List.of("echoagriculturereclamation:bio_gel"),
         120,
         0,
         List.of("Special crops can add secondary outputs.")
      ).normalized();

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("greenhouseMachineRulesRoundTrip", defaults.greenhouseHorizontalRange() == 6
         && defaults.greenhouseFilterWeight() == 18
         && normalized.hydroponicGrowthTicks() == 1
         && normalized.pollinatorDroneServiceTicks() == 20
         && normalized.pollinatorDroneServiceRadius() == 1);
      result.put("seedSupplyProcessRoundTrip", seedProcess.id().equals("seed_vault_analysis")
         && seedProcess.machine().equals("seed_vault_terminal")
         && seedProcess.title().equals("seed_vault_analysis")
         && seedProcess.outputs().contains("echoagriculturereclamation:contaminated_seed")
         && seedProcess.powerCost() == 0);
      result.put("processCardRoundTrip", uiProcess.id().equals("bio_reactor_biomass")
         && uiProcess.machine().equals("bio_reactor")
         && uiProcess.title().equals("Biomass Bio-Reaction")
         && uiProcess.ticks() == 120
         && !uiProcess.notes().isEmpty());
      result.put("restorationEnvelopeRoundTrip", defaults.greenhouseHorizontalRange() == defaults.greenhouseUpRange()
         && defaults.greenhouseDownRange() == 4
         && defaults.pollinatorDroneHomeRadius() >= defaults.pollinatorDroneServiceRadius()
         && defaults.greenhouseGlassWeight() + defaults.greenhouseFilterWeight() + defaults.greenhouseControllerWeight() > 25);
      result.put("seedProcessId", seedProcess.id());
      result.put("uiProcessId", uiProcess.id());
      result.put("greenhouseRange", defaults.greenhouseHorizontalRange());
      return Map.copyOf(result);
   }

   private static void require(boolean condition, String message) {
      if (!condition) {
         throw new AssertionError(message);
      }
   }
}
