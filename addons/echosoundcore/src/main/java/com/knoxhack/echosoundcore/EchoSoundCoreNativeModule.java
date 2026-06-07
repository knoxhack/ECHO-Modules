package com.knoxhack.echosoundcore;

import com.knoxhack.echo.adaptercore.EchoNativeEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeModuleAdapter;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import com.knoxhack.echo.adaptercore.EchoNativeRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimePacketConsumerBridge;
import com.knoxhack.echo.adaptercore.EchoNativeServiceBridge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoSoundCoreNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> audioDispatch = EchoSoundCoreAudioDispatchContract.executeReferenceDispatch(
                context.getOrDefault("packId", "unknown")
        );
        boolean audioDispatchPassed = EchoSoundCoreAudioDispatchContract.referenceDispatchPassed(audioDispatch);
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover SoundCore adaptive audio and profile contracts.")
                .phase("register_audio_contracts", "Record audio events, network actions, profiles, and service contracts.")
                .phase("attach_audio_events", "Record command, reload, and optional integration hooks.")
                .phase("execute_audio_dispatch", "Execute audio profile dispatch, UI cue, stinger, ambience, and network audio action behavior.")
                .phase("ready", "Expose SoundCore as the native audio provider for Ashfall.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("sound", "echosoundcore:ui_confirm", "Shared UI confirmation cue.")
                .register("sound", "echosoundcore:ui_warning", "Shared UI warning cue.")
                .register("sound", "echosoundcore:mission_stinger", "Mission completion stinger.")
                .register("sound", "echosoundcore:ambient_loop", "Adaptive ambience loop contract.")
                .register("network_payload", "echosoundcore:play_audio_action", "Server-to-client audio action payload.")
                .register("resource_profile", "echosoundcore:audio_profiles", "Audio profile reload contract.")
                .register("service", "echosoundcore:sound_service", "ECHO sound service provider.")
                .register("integration", "echosoundcore:mission", "Mission audio integration.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("common.setup", "EchoSoundCore.commonSetup", "Attach sound service and optional integrations.")
                .hook("network.payload_register", "SoundCoreNetwork.registerPayloads", "Attach SoundCore payload contracts.")
                .hook("commands.register", "SoundCoreCommands.register", "Expose SoundCore command surface when native command bridge exists.")
                .hook("data.reload", "SoundCoreDataReloadListener", "Attach audio profile reloaders.")
                .hook("optional.integrations", "EchoSoundCore.registerOptionalIntegrations", "Prepare audio integration fanout.");
        EchoNativeServiceBridge services = new EchoNativeServiceBridge(MODULE_ID)
                .surfaceService("weather_sound_atmosphere", "echosoundcore:sound_service", "audio_dispatch",
                        "Keeps UI cue, stinger, ambience, and adaptive music profile state ready for native audio actions.",
                        "sound.service", "sound.ui_cues", "sound.stingers", "sound.ambience", "sound.adaptive_music")
                .surfaceService("weather_sound_atmosphere", "echosoundcore:audio_profile_service", "audio_profiles",
                        "Keeps data-driven audio profile contracts ready for reload and weather integrations.",
                        "sound.audio_profiles", "weather.events");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "soundcore_native_audio_dispatch_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("lifecycleBridge", lifecycle.describe());
        result.put("registryBridge", registry.describe());
        result.put("eventBridge", events.describe());
        result.put("serviceBridge", services.describe());
        result.put("audioDispatch", audioDispatch);
        result.put("audioDispatchExecuted", audioDispatchPassed);
        result.put("logicalRegistrationCount", 8);
        result.put("eventHookCount", 5);
        result.put("approvedNativeServiceCount", 2);
        result.put("registeredFeatureContracts", List.of(
                "sound.adaptive_music",
                "sound.ambience",
                "sound.audio_profiles",
                "sound.diagnostics",
                "sound.network_actions",
                "sound.service",
                "sound.stingers",
                "sound.ui_cues",
                EchoSoundCoreAudioDispatchContract.ADAPTERCORE_CONTRACT_ID
        ));
        result.put("requiresSoundBridge", true);
        result.put("requiresNetworkBridge", true);
        result.put("requiresRegistryBridge", true);
        result.put("requiresLifecycleBridge", true);
        result.put("requiresEventBridge", true);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("serviceBridgeStarted", true);
        result.put("serviceCodeExecuted", audioDispatchPassed);
        result.put("transformsPerformed", false);
        result.put("summary", "SoundCore native contract registered audio hooks and executed the AdapterCore audio dispatch service.");
        return result;
    }

    public Map<String, Object> consumeAshfallRuntimePackets(Map<String, Object> runtimePacketBindings) {
        return new EchoNativeRuntimePacketConsumerBridge(MODULE_ID).consume(
                "echosoundcore:ashfall_runtime_packet_consumers",
                runtimePacketBindings,
                List.of("echosoundcore:ambience"));
    }

    private static final String MODULE_ID = "echosoundcore";
}
