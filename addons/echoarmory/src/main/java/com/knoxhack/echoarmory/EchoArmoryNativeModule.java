package com.knoxhack.echoarmory;

import com.knoxhack.echo.adaptercore.EchoAdapterRuntime;
import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import com.knoxhack.echoarmory.api.ArmoryAdapterCoreReference;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoArmoryNativeModule implements EchoNativeSurfaceModuleEntrypoint {
   public static final String MODULE_ID = "echoarmory";
   public static final String GEAR_STATE_CONTRACT_ID = "echoarmory:item/gear_state_normalization";
   public static final String STATION_PREVIEW_CONTRACT_ID = "echoarmory:recipe/station_operation_preview";
   public static final String ROUTE_READINESS_CONTRACT_ID = "echoarmory:player/route_readiness_score";
   public static final List<String> CONTRACT_IDS = List.of(
      GEAR_STATE_CONTRACT_ID,
      STATION_PREVIEW_CONTRACT_ID,
      ROUTE_READINESS_CONTRACT_ID
   );

   public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
      ArmoryAdapterCoreReference.ItemStateProbe itemProbe = ArmoryAdapterCoreReference.itemStateProbe();
      ArmoryAdapterCoreReference.RecipePreviewProbe recipeProbe = ArmoryAdapterCoreReference.recipePreviewProbe();
      ArmoryAdapterCoreReference.ReadinessProbe readinessProbe = ArmoryAdapterCoreReference.readinessProbe();

      Map<String, Object> referenceProbe = new LinkedHashMap<>();
      referenceProbe.put("gearStateRoundTrip", itemProbe.passed());
      referenceProbe.put("stationPreviewRoundTrip", recipeProbe.passed());
      referenceProbe.put("routeReadinessRoundTrip", readinessProbe.passed());
      referenceProbe.put("normalizedModules", itemProbe.modules());
      referenceProbe.put("energyStored", itemProbe.energyStored());
      referenceProbe.put("energyCapacity", itemProbe.energyCapacity());
      referenceProbe.put("tier", itemProbe.tier());
      referenceProbe.put("blockedOperation", recipeProbe.blockedOperation());
      referenceProbe.put("readyScore", readinessProbe.readyScore());
      referenceProbe.put("stagedScore", readinessProbe.stagedScore());
      referenceProbe.put("lockedScore", readinessProbe.lockedScore());
      referenceProbe.put("stagedAction", readinessProbe.stagedAction());

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("activated", true);
      result.put("activationStage", "armory_native_contract_active");
      result.put("adapterCoreUsed", true);
      result.put("nativeAdapterCodeExecuted", true);
      result.put("serviceCodeExecuted", true);
      result.put("moduleId", MODULE_ID);
      result.put("packId", context.getOrDefault("packId", "unknown"));
      result.put("registeredFeatureContracts", CONTRACT_IDS);
      result.put("logicalRegistrationCount", CONTRACT_IDS.size());
      result.put("adapterDomains", List.of("items", "recipes", "player"));
      result.put("runtimeTargets", List.of(
         EchoAdapterRuntime.NEOFORGE.serializedName(),
         EchoAdapterRuntime.ECHO_NATIVE.serializedName(),
         EchoAdapterRuntime.ECHO_RUNTIME_STANDALONE.serializedName()));
      result.put("gearStateRoundTrip", itemProbe.passed());
      result.put("stationPreviewRoundTrip", recipeProbe.passed());
      result.put("routeReadinessRoundTrip", readinessProbe.passed());
      result.put("referenceProbe", Map.copyOf(referenceProbe));
      result.put("registryInjected", false);
      result.put("registryMutated", false);
      result.put("transformsPerformed", false);
      result.put("summary", "Armory native contract exercised gear/module state normalization, station operation previews, and route-readiness scoring.");
      return Map.copyOf(result);
   }

   public static void main(String[] args) {
      Map<String, Object> activation = new EchoArmoryNativeModule()
         .describeNativeSurfaces(Map.of("packId", "armory-smoke"));
      require(Boolean.TRUE.equals(activation.get("activated")),
         "Armory native adapter should activate");
      require(Boolean.TRUE.equals(activation.get("gearStateRoundTrip")),
         "Armory native adapter should exercise item gear/module state behavior");
      require(Boolean.TRUE.equals(activation.get("stationPreviewRoundTrip")),
         "Armory native adapter should exercise station recipe preview behavior");
      require(Boolean.TRUE.equals(activation.get("routeReadinessRoundTrip")),
         "Armory native adapter should exercise player route-readiness scoring behavior");
      System.out.println("armory native adapter smoke PASS contracts=" + CONTRACT_IDS.size());
   }

   private static void require(boolean condition, String message) {
      if (!condition) {
         throw new AssertionError(message);
      }
   }
}
