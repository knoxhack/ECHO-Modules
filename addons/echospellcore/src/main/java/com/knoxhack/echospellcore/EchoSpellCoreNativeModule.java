package com.knoxhack.echospellcore;

import com.knoxhack.echo.adaptercore.EchoNativeEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeModuleAdapter;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import com.knoxhack.echo.adaptercore.EchoNativeRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoNativeServiceBridge;
import com.knoxhack.echo.adaptercore.EchoNativeStoryRuntimeBridge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoSpellCoreNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> spellCastResolution = EchoSpellCoreCastResolutionContract.executeReferenceCast(
                context.getOrDefault("packId", "unknown")
        );
        boolean spellCastResolutionPassed = EchoSpellCoreCastResolutionContract.referenceCastPassed(
                spellCastResolution
        );
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover spell definition, focus, deck, projectile, and cooldown contracts.")
                .phase("register_spell_contracts", "Record Signal Pulse and spell runtime state contracts.")
                .phase("attach_spell_events", "Record cast, cooldown, projectile, mission, and lore update hooks.")
                .phase("execute_spell_cast_resolution", "Execute Signal Focus spell cost, cooldown, projectile, and effect behavior.")
                .phase("ready", "Expose SpellCore as the native story spell provider.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("spell", "echospellcore:spell/signal_pulse", "Signal Pulse spell contract.")
                .register("item", "echospellcore:signal_focus", "Signal focus casting item contract.")
                .register("item", "echospellcore:spell_deck", "Spell deck loadout contract.")
                .register("projectile", "echospellcore:projectile/aether_bolt", "Portable spell projectile contract.")
                .register("save_record", "echospellcore:save/spell_state", "Spell cooldown and clarity state contract.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("spell.cast", "SpellCoreApi.cast", "Apply spell usage and gameplay state mutation.")
                .hook("spell.cooldown", "SpellCoreApi.cooldown", "Persist spell cooldown state.")
                .hook("projectile.spawn", "SpellProjectileEntity.spawn", "Bridge spell projectile intent.")
                .hook("mission.hook", "SpellCoreMissionCoreIntegration", "Publish spell mission hook coverage.")
                .hook("lore.index.update", "SpellCoreTerminalIntegration", "Publish spell records to Terminal, Index, and Lore surfaces.");
        EchoNativeStoryRuntimeBridge storyRuntime = new EchoNativeStoryRuntimeBridge(MODULE_ID)
                .castSpell("echospellcore:spell/signal_pulse", "signalClarity", 1);
        EchoNativeServiceBridge services = new EchoNativeServiceBridge(MODULE_ID)
                .surfaceService("player", "echospellcore:signal_focus_cast_service", "signal_focus_cast_resolution",
                        "Executes deterministic Signal Focus spell cost, cooldown, projectile, and hit-effect behavior.",
                        "spell.aether_costs", "spell.cooldowns", "spell.projectiles", "spell.runtime_hooks");
        Map<String, Object> storyRuntimeReport = storyRuntime.report();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "spellcore_native_cast_resolution_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("lifecycleBridge", lifecycle.describe());
        result.put("registryBridge", registry.describe());
        result.put("eventBridge", events.describe());
        result.put("storyRuntimeBridge", storyRuntimeReport);
        result.put("serviceBridge", services.describe());
        result.put("spellCastResolution", spellCastResolution);
        result.put("spellCastResolved", spellCastResolutionPassed);
        result.put("storyRuntimeServiceCodeExecuted", Boolean.TRUE.equals(storyRuntimeReport.get("serviceCodeExecuted")));
        result.put("storyRuntimeHandlerExecutionCount", storyRuntimeReport.get("handlerExecutionCount"));
        result.put("logicalRegistrationCount", 5);
        result.put("eventHookCount", 5);
        result.put("approvedNativeServiceCount", 1);
        result.put("registeredFeatureContracts", List.of(
                "spell.definitions",
                "spell.focus",
                "spell.deck",
                "spell.projectiles",
                "spell.runtime_hooks",
                EchoSpellCoreCastResolutionContract.ADAPTERCORE_CONTRACT_ID
        ));
        result.put("requiresSpellBridge", true);
        result.put("registryMutated", false);
        result.put("serviceBridgeStarted", true);
        result.put("serviceCodeExecuted", true);
        result.put("transformsPerformed", false);
        result.put("summary", "SpellCore native contract executed Signal Focus spell cost, cooldown, projectile, and hit-effect behavior through AdapterCore.");
        return result;
    }

    private static final String MODULE_ID = "echospellcore";
}
