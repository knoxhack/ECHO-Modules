package com.knoxhack.echorelictech;

import com.knoxhack.echo.adaptercore.EchoNativeEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeModuleAdapter;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import com.knoxhack.echo.adaptercore.EchoNativeRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoNativeStoryRuntimeBridge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoRelicTechNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> containmentPlan = EchoRelicTechContainmentContract.executeReferencePlan(
                context.getOrDefault("packId", "unknown")
        );
        boolean containmentPlanPassed = EchoRelicTechContainmentContract.referencePlanPassed(containmentPlan);
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover RelicTech relic, vault, containment, and instability contracts.")
                .phase("register_relic_content", "Record relic blocks, items, components, and data loaders.")
                .phase("attach_relic_events", "Record command, reload, tick, and internal relic event hooks.")
                .phase("execute_containment_plan", "Execute relic scan, workbench, instability, vault, and containment plan behavior.")
                .phase("ready", "Expose RelicTech as the native relic gameplay provider for Ashfall.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("block", "echorelictech:relic_vault", "Relic vault block contract.")
                .register("block", "echorelictech:null_battery_dock", "Null battery dock block contract.")
                .register("item", "echorelictech:unidentified_relic", "Unidentified relic item contract.")
                .register("item", "echorelictech:echo_mirror", "Echo Mirror relic item contract.")
                .register("relic_effect", "echorelictech:relic_effect/echo_mirror", "Echo Mirror story relic effect contract.")
                .register("relic_effect", "echorelictech:relic_effect/phase_anchor", "Phase Anchor story relic effect contract.")
                .register("data_component", "echorelictech:relic_data", "Relic instance data component.")
                .register("save_record", "echorelictech:save/relic_story_state", "RelicTech story state persistence contract.")
                .register("resource_profile", "echorelictech:relic_definitions", "Relic definition reload contract.")
                .register("resource_profile", "echorelictech:relic_failures", "Relic failure table reload contract.")
                .register("resource_profile", "echorelictech:relic_vaults", "Relic vault reload contract.")
                .register("service", "echorelictech:instability", "Relic instability service contract.")
                .register("integration", "echorelictech:nexus", "Nexus relic research integration.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("common.setup", "EchoRelicTech.commonSetup", "Attach RelicTech optional integrations.")
                .hook("data.reload", "RelicDefinitionLoader", "Attach relic definition reloaders.")
                .hook("data.reload", "RelicFailureLoader", "Attach relic failure reloaders.")
                .hook("data.reload", "RelicVaultLoader", "Attach relic vault reloaders.")
                .hook("server.tick.post", "RelicInstabilityManager.tickDecay", "Prepare instability decay tick.")
                .hook("server.tick.post", "EchoMirrorDecoyTracker.tick", "Prepare mirror decoy tick.")
                .hook("relic.effect.apply", "RelicTechArcanaIntegration.applyPhaseAnchor", "Apply Phase Anchor story relic effect.")
                .hook("commands.register", "RelicTechCommands.register", "Expose relic commands when native command bridge exists.")
                .hook("relic.event", "RelicTechEvents", "Prepare relic analyze/use/vault event fanout.");
        EchoNativeStoryRuntimeBridge storyRuntime = new EchoNativeStoryRuntimeBridge(MODULE_ID)
                .applyRelicEffect(
                        "echorelictech:relic_effect/echo_mirror",
                        "signalClarity",
                        2,
                        "signalos:archive/field_cache"
                )
                .applyRelicEffect(
                        "echorelictech:relic_effect/phase_anchor",
                        "aetherCharge",
                        2,
                        "echogrimoire:archive/arcane_codex"
                );
        Map<String, Object> storyRuntimeReport = storyRuntime.report();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "relictech_native_contract_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("lifecycleBridge", lifecycle.describe());
        result.put("registryBridge", registry.describe());
        result.put("eventBridge", events.describe());
        result.put("storyRuntimeBridge", storyRuntimeReport);
        result.put("containmentPlan", containmentPlan);
        result.put("containmentPlanExecuted", containmentPlanPassed);
        result.put("storyRuntimeServiceCodeExecuted", Boolean.TRUE.equals(storyRuntimeReport.get("serviceCodeExecuted")));
        result.put("storyRuntimeHandlerExecutionCount", storyRuntimeReport.get("handlerExecutionCount"));
        result.put("logicalRegistrationCount", 13);
        result.put("eventHookCount", 9);
        result.put("registeredFeatureContracts", List.of(
                "relictech.analysis",
                "relictech.containment",
                "relictech.instability",
                "relictech.relics",
                "relictech.research",
                "relictech.vaults",
                EchoRelicTechContainmentContract.ADAPTERCORE_CONTRACT_ID
        ));
        result.put("requiresRelicBridge", true);
        result.put("requiresRegistryBridge", true);
        result.put("requiresLifecycleBridge", true);
        result.put("requiresEventBridge", true);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("serviceCodeExecuted", true);
        result.put("transformsPerformed", false);
        result.put("summary", "RelicTech native contract registered relic, vault, instability, and reload hooks and executed the AdapterCore containment plan service.");
        return result;
    }

    private static final String MODULE_ID = "echorelictech";
}
