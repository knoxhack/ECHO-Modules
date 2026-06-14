package com.knoxhack.signalos;

import com.knoxhack.echo.adaptercore.EchoNativeEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeModuleAdapter;
import com.knoxhack.echo.adaptercore.EchoNativeRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoNativeStoryRuntimeBridge;
import dev.echo.nativeplatform.contracts.EchoNativeActivationSurfaceRegistrar;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoSignalOsNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    @Override
    public void registerServices(EchoNativeModuleLoadContext context) {
        Map<String, Object> activation = describeNativeSurfaces(
                EchoNativeActivationSurfaceRegistrar.bridgeContext(context)
        );
        context.registerService(
                "adaptercore.signalos.contract",
                activation,
                "adaptercore",
                "terminal",
                "archive",
                "data_drive",
                "mission",
                "chapter"
        );
    }

    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover SignalOS archive, drive, message, story flag, and chapter contracts.")
                .phase("register_story_contracts", "Record terminal, archive, data-drive, mission, and lore update contracts.")
                .phase("attach_story_events", "Record terminal open, data drive read, mission start, and save/load hooks.")
                .phase("ready", "Expose SignalOS as the native story runtime provider.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("ui_surface", "signalos:ui/terminal", "SignalOS terminal surface contract.")
                .register("archive_entry", "signalos:archive/field_cache", "SignalOS archive entry contract.")
                .register("data_drive", "signalos:data_drive/handoff_drive", "SignalOS data drive contract.")
                .register("signal_message", "signalos:signal/secure_cache", "SignalOS signal message contract.")
                .register("story_flag", "signalos:story_flag/cache_secured", "SignalOS story flag contract.")
                .register("mission", "signalos:mission/secure_cache", "SignalOS story mission contract.")
                .register("chapter", "signalos:chapter/cache_handoff", "SignalOS chapter unlock contract.")
                .register("lore_surface", "signalos:lore/index_wiki", "Index/Wiki/Lore update contract.")
                .register("save_record", "signalos:save/story_state", "SignalOS story state persistence contract.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("terminal.open", "SignalOsTerminalServices.open", "Open SignalOS in the terminal host.")
                .hook("data_drive.read", "SignalOsDataDriveItem.read", "Read active data drive and unlock archives.")
                .hook("story.mission.start", "SignalOsMissionHooks.start", "Start SignalOS story mission from a signal message.")
                .hook("story.flag.save", "SignalOsTerminalServices.saveFlags", "Persist story flags and archive unlocks.")
                .hook("lore.index.update", "SignalOsContentRegistry.publishLore", "Publish SignalOS records to Index, Wiki, and Lore surfaces.");
        EchoNativeStoryRuntimeBridge storyRuntime = new EchoNativeStoryRuntimeBridge(MODULE_ID)
                .openTerminal("signalos:ui/terminal", "signalos:signal/secure_cache")
                .readDataDrive(
                        "signalos:data_drive/handoff_drive",
                        "signalos:archive/field_cache",
                        "signalos:story_flag/cache_secured"
                )
                .startMission("signalos:signal/secure_cache", "signalos:mission/secure_cache")
                .unlockChapter("signalos:chapter/cache_handoff", "signalos:story_flag/cache_secured");
        Map<String, Object> storyRuntimeReport = storyRuntime.report();
        SignalOsTerminalSessionContract terminalSessionContract = new SignalOsTerminalSessionContract();
        Map<String, Object> terminalSession = terminalSessionContract.execute(
                context.getOrDefault("operatorId", "operator-ashfall-01"),
                "echo_native");
        boolean terminalSessionPassed = terminalSessionContract.referenceSessionPassed(terminalSession);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "signalos_native_terminal_session_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("lifecycleBridge", lifecycle.describe());
        result.put("registryBridge", registry.describe());
        result.put("eventBridge", events.describe());
        result.put("storyRuntimeBridge", storyRuntimeReport);
        result.put("storyRuntimeServiceCodeExecuted", Boolean.TRUE.equals(storyRuntimeReport.get("serviceCodeExecuted")));
        result.put("storyRuntimeHandlerExecutionCount", storyRuntimeReport.get("handlerExecutionCount"));
        result.put("logicalRegistrationCount", 9);
        result.put("eventHookCount", 5);
        result.put("registeredFeatureContracts", List.of(
                "signalos.archives",
                "signalos.chapters",
                "signalos.data_drives",
                "signalos.missions",
                "signalos.terminal",
                "signalos.story_state",
                "signalos.lore_updates",
                SignalOsTerminalSessionContract.ADAPTERCORE_CONTRACT_ID
        ));
        result.put("terminalSession", terminalSession);
        result.put("terminalSessionExecuted", terminalSessionPassed);
        result.put("terminalSessionContract", SignalOsTerminalSessionContract.ADAPTERCORE_CONTRACT_ID);
        result.put("terminalSessionDriveFileCount", ((List<?>) ((Map<?, ?>) terminalSession.get("mountedDrive")).get("files")).size());
        result.put("terminalSessionDiagnosticsCount", ((List<?>) terminalSession.get("diagnostics")).size());
        result.put("requiresStoryBridge", true);
        result.put("requiresTerminalBridge", true);
        result.put("requiresRegistryBridge", true);
        result.put("requiresLifecycleBridge", true);
        result.put("requiresEventBridge", true);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("serviceCodeExecuted", true);
        result.put("transformsPerformed", false);
        result.put("summary", "SignalOS native contract booted the terminal shell, mounted the operator handoff drive, unlocked Field Interface archive state, and prepared story save persistence through AdapterCore.");
        return result;
    }

    private static final String MODULE_ID = "signalos";
}
