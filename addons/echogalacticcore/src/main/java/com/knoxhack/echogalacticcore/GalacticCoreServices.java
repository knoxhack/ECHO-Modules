package com.knoxhack.echogalacticcore;

import com.knoxhack.echogalacticcore.asdk.GalacticCoreNativeMutations;
import com.knoxhack.echogalacticcore.content.GalacticCoreAttachments;
import com.knoxhack.echogalacticcore.content.GalacticCoreBlockEntities;
import com.knoxhack.echogalacticcore.content.GalacticCoreBlocks;
import com.knoxhack.echogalacticcore.content.GalacticCoreCapabilities;
import com.knoxhack.echogalacticcore.content.GalacticCoreDimensions;
import com.knoxhack.echogalacticcore.content.GalacticCoreDungeons;
import com.knoxhack.echogalacticcore.content.GalacticCoreEntities;
import com.knoxhack.echogalacticcore.content.GalacticCoreFluids;
import com.knoxhack.echogalacticcore.content.GalacticCoreGameplayManifests;
import com.knoxhack.echogalacticcore.content.GalacticCoreItems;
import com.knoxhack.echogalacticcore.content.GalacticCoreMachines;
import com.knoxhack.echogalacticcore.content.GalacticCorePackets;
import com.knoxhack.echogalacticcore.content.GalacticCoreRecipes;
import com.knoxhack.echogalacticcore.content.GalacticCoreScreens;
import com.knoxhack.echogalacticcore.integration.GalacticCoreEchoIntegrations;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreHostCallbacks;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreHostBindingContracts;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreHostExecutionBridge;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreLiveHostAdapters;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreLiveHostEntrypoints;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreLiveSessionMutations;
import com.knoxhack.echogalacticcore.runtime.GalacticCorePlatformExecutors;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeAdapters;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeGateway;
import com.knoxhack.echogalacticcore.runtime.GalacticCoreRuntimeService;
import dev.echo.nativeplatform.contracts.EchoNativeAttachmentService;
import dev.echo.nativeplatform.contracts.EchoNativeCapabilityService;
import dev.echo.nativeplatform.contracts.EchoNativeCommandService;
import dev.echo.nativeplatform.contracts.EchoNativeConfigService;
import dev.echo.nativeplatform.contracts.EchoNativeEventService;
import dev.echo.nativeplatform.contracts.EchoNativeLifecycleService;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadContext;
import dev.echo.nativeplatform.contracts.EchoNativeNetworkService;
import dev.echo.nativeplatform.contracts.EchoNativeRegistryService;
import dev.echo.nativeplatform.contracts.EchoNativeRenderService;
import dev.echo.nativeplatform.contracts.EchoNativeResourceService;
import dev.echo.nativeplatform.contracts.EchoNativeSaveDataService;
import dev.echo.nativeplatform.contracts.EchoNativeScreenService;
import dev.echo.nativeplatform.contracts.EchoNativeWorldgenService;

import java.util.List;
import java.util.Map;

public final class GalacticCoreServices {
    private GalacticCoreServices() {
    }

    public static void registerModuleServices(EchoNativeModuleLoadContext context) {
        context.registerService(
                GalacticCoreIds.id("port_plan"),
                new GalacticCorePortPlanService(),
                "identity",
                "parity",
                "release_gate"
        );
        GalacticCoreRuntimeService runtime = new GalacticCoreRuntimeService();
        context.registerService(
                GalacticCoreIds.id("runtime"),
                runtime,
                "gameplay",
                "machines",
                "oxygen",
                "energy",
                "player_gear",
                "rockets"
        );
        context.registerService(
                GalacticCoreIds.id("runtime_gateway"),
                new GalacticCoreRuntimeGateway(runtime),
                "events",
                "network",
                "screens",
                "gameplay"
        );
        GalacticCoreRuntimeGateway gateway = context.serviceRegistry()
                .service(GalacticCoreIds.MOD_ID, GalacticCoreIds.id("runtime_gateway"), GalacticCoreRuntimeGateway.class)
                .orElseThrow();
        context.registerService(
                GalacticCoreIds.id("runtime_adapters"),
                new GalacticCoreRuntimeAdapters(runtime, gateway),
                "block_entities",
                "dimension_transfer",
                "dungeons",
                "treasure",
                "gameplay"
        );
        GalacticCoreRuntimeAdapters adapters = context.serviceRegistry()
                .service(GalacticCoreIds.MOD_ID, GalacticCoreIds.id("runtime_adapters"), GalacticCoreRuntimeAdapters.class)
                .orElseThrow();
        context.registerService(
                GalacticCoreIds.id("host_callbacks"),
                new GalacticCoreHostCallbacks(runtime, gateway, adapters),
                "host_callbacks",
                "block_entities",
                "dimension_transfer",
                "dungeons",
                "screens",
                "network",
                "events"
        );
        GalacticCoreHostCallbacks hostCallbacks = context.serviceRegistry()
                .service(GalacticCoreIds.MOD_ID, GalacticCoreIds.id("host_callbacks"), GalacticCoreHostCallbacks.class)
                .orElseThrow();
        context.registerService(
                GalacticCoreIds.id("host_execution_bridge"),
                new GalacticCoreHostExecutionBridge(hostCallbacks),
                "host_execution",
                "dimension_transfer",
                "dungeons",
                "screens",
                "events"
        );
        GalacticCoreHostExecutionBridge hostExecutionBridge = context.serviceRegistry()
                .service(GalacticCoreIds.MOD_ID, GalacticCoreIds.id("host_execution_bridge"), GalacticCoreHostExecutionBridge.class)
                .orElseThrow();
        context.registerService(
                GalacticCoreIds.id("host_binding_contracts"),
                new GalacticCoreHostBindingContracts(hostExecutionBridge),
                "host_binding",
                "world",
                "entities",
                "screens",
                "typed_receipts"
        );
        GalacticCoreHostBindingContracts hostBindingContracts = context.serviceRegistry()
                .service(GalacticCoreIds.MOD_ID, GalacticCoreIds.id("host_binding_contracts"), GalacticCoreHostBindingContracts.class)
                .orElseThrow();
        context.registerService(
                GalacticCoreIds.id("live_host_adapters"),
                new GalacticCoreLiveHostAdapters(hostBindingContracts),
                "live_host",
                "world",
                "entities",
                "screens",
                "typed_receipts"
        );
        GalacticCoreLiveHostAdapters liveHostAdapters = context.serviceRegistry()
                .service(GalacticCoreIds.MOD_ID, GalacticCoreIds.id("live_host_adapters"), GalacticCoreLiveHostAdapters.class)
                .orElseThrow();
        context.registerService(
                GalacticCoreIds.id("live_host_entrypoints"),
                new GalacticCoreLiveHostEntrypoints(liveHostAdapters),
                "live_host",
                "entrypoints",
                "world",
                "entities",
                "screens"
        );
        GalacticCoreLiveHostEntrypoints liveHostEntrypoints = context.serviceRegistry()
                .service(GalacticCoreIds.MOD_ID, GalacticCoreIds.id("live_host_entrypoints"), GalacticCoreLiveHostEntrypoints.class)
                .orElseThrow();
        context.registerService(
                GalacticCoreIds.id("platform_executors"),
                new GalacticCorePlatformExecutors(liveHostEntrypoints),
                "platform_executor",
                "world",
                "entities",
                "screens",
                "typed_receipts"
        );
        GalacticCorePlatformExecutors platformExecutors = context.serviceRegistry()
                .service(GalacticCoreIds.MOD_ID, GalacticCoreIds.id("platform_executors"), GalacticCorePlatformExecutors.class)
                .orElseThrow();
        context.registerService(
                GalacticCoreIds.id("live_session_mutations"),
                new GalacticCoreLiveSessionMutations(
                        platformExecutors,
                        GalacticCoreLiveSessionMutations.contractOnlyHostSink()
                ),
                "live_session",
                "host_mutation",
                "world",
                "entities",
                "screens"
        );
    }

    public static void phase(EchoNativeModuleLoadContext context, String phase) {
        context.recordMutation(
                "lifecycle",
                "native_lifecycle_callback_executed",
                phase,
                EchoNativeLoadStatus.MUTATED
        );
        GalacticCoreNativeMutations.service(context, "echo.native.lifecycle", EchoNativeLifecycleService.class)
                .ifPresent(lifecycle -> GalacticCoreNativeMutations.record(
                        context,
                        lifecycle.phase(GalacticCoreNativeMutations.common(
                                "lifecycle",
                                "phase",
                                GalacticCoreIds.id("phase/" + phase),
                                Map.of("phase", phase, "nativeEntrypoint", GalacticCoreIds.NATIVE_ENTRYPOINT)
                        ))
                ));
    }

    public static void resolveDependencies(EchoNativeModuleLoadContext context) {
        context.resolveDependency("echoadaptercore");
        GalacticCoreIds.OPTIONAL_INTEGRATIONS.forEach(context::resolveDependency);
    }

    public static void registerCommonRuntime(EchoNativeModuleLoadContext context) {
        GalacticCoreNativeMutations.service(context, "echo.native.config", EchoNativeConfigService.class)
                .ifPresent(config -> GalacticCoreNativeMutations.record(
                        context,
                        config.register(GalacticCoreNativeMutations.common(
                                "config",
                                "register",
                                GalacticCoreIds.id("config/common"),
                                Map.of(
                                        "oxygenDifficulty", "classic",
                                        "rocketProgression", "tiered",
                                        "enableLegacyParityWarnings", true
                                )
                        ))
                ));
        GalacticCoreNativeMutations.service(context, "echo.native.events", EchoNativeEventService.class)
                .ifPresent(events -> List.of(
                                "player_tick_life_support",
                                "rocket_launch_prepare",
                                "celestial_route_unlock",
                                "resource_reload_parity"
                        ).forEach(event -> GalacticCoreNativeMutations.record(
                                context,
                                events.subscribe(GalacticCoreNativeMutations.common(
                                        "events",
                                        "subscribe",
                                        GalacticCoreIds.id("event/" + event),
                                        Map.of("legacyReplacement", "Forge event bus listener")
                                ))
                        )));
        GalacticCoreNativeMutations.service(context, "echo.native.commands", EchoNativeCommandService.class)
                .ifPresent(commands -> GalacticCoreNativeMutations.record(
                        context,
                        commands.register(GalacticCoreNativeMutations.common(
                                "commands",
                                "register",
                                GalacticCoreIds.id("command/galacticcore"),
                                Map.of("purpose", "diagnostics, parity, route inspection")
                        ))
                ));
        GalacticCoreNativeMutations.service(context, "echo.native.resources", EchoNativeResourceService.class)
                .ifPresent(resources -> {
                    GalacticCoreNativeMutations.record(
                            context,
                            resources.registerReloadListener(GalacticCoreNativeMutations.common(
                                    "resources",
                                    "registerReloadListener",
                                    GalacticCoreIds.id("resources/legacy_asset_migration"),
                                    Map.of("from", "assets/galacticraftcore + assets/galacticraftplanets", "to", "assets/echogalacticcore")
                            ))
                    );
                    GalacticCoreNativeMutations.record(
                            context,
                            resources.runDatagen(GalacticCoreNativeMutations.common(
                                    "resources",
                                    "runDatagen",
                                    GalacticCoreIds.id("datagen/native_descriptors"),
                                    Map.of("outputs", "lang, recipes, tags, parity manifests")
                            ))
                    );
                });
        registerExecutableRuntimeContracts(context);
        registerRuntimeGatewayContracts(context);
        registerRuntimeAdapterContracts(context);
        registerHostCallbackContracts(context);
        registerHostExecutionContracts(context);
        registerConcreteHostBindingContracts(context);
        registerLiveHostAdapterContracts(context);
        registerLiveHostEntrypointContracts(context);
        registerPlatformExecutorContracts(context);
        registerLiveSessionMutationContracts(context);
    }

    public static void registerContent(EchoNativeModuleLoadContext context) {
        EchoNativeRegistryService registry = GalacticCoreNativeMutations
                .service(context, "echo.native.registry", EchoNativeRegistryService.class)
                .orElse(null);
        EchoNativeWorldgenService worldgen = GalacticCoreNativeMutations
                .service(context, "echo.native.worldgen", EchoNativeWorldgenService.class)
                .orElse(null);
        EchoNativeNetworkService network = GalacticCoreNativeMutations
                .service(context, "echo.native.network", EchoNativeNetworkService.class)
                .orElse(null);
        EchoNativeCapabilityService capabilities = GalacticCoreNativeMutations
                .service(context, "echo.native.capabilities", EchoNativeCapabilityService.class)
                .orElse(null);
        EchoNativeAttachmentService attachments = GalacticCoreNativeMutations
                .service(context, "echo.native.attachments", EchoNativeAttachmentService.class)
                .orElse(null);
        EchoNativeScreenService screens = GalacticCoreNativeMutations
                .service(context, "echo.native.screens", EchoNativeScreenService.class)
                .orElse(null);
        EchoNativeResourceService resources = GalacticCoreNativeMutations
                .service(context, "echo.native.resources", EchoNativeResourceService.class)
                .orElse(null);
        EchoNativeSaveDataService saveData = GalacticCoreNativeMutations
                .service(context, "echo.native.save_data", EchoNativeSaveDataService.class)
                .orElse(null);

        if (registry != null) {
            GalacticCoreBlocks.register(context, registry);
            GalacticCoreItems.register(context, registry);
            GalacticCoreFluids.register(context, registry);
            GalacticCoreBlockEntities.register(context, registry);
            GalacticCoreRecipes.register(context, registry);
            GalacticCoreEntities.register(context, registry);
        }
        if (registry != null && worldgen != null) {
            GalacticCoreDimensions.register(context, registry, worldgen);
        }
        if (registry != null && worldgen != null && resources != null) {
            GalacticCoreDungeons.register(context, registry, worldgen, resources);
        }
        if (resources != null) {
            GalacticCoreGameplayManifests.register(context, resources);
        }
        if (network != null) {
            GalacticCorePackets.register(context, network);
        }
        if (capabilities != null) {
            GalacticCoreCapabilities.register(context, capabilities);
        }
        if (attachments != null) {
            GalacticCoreAttachments.register(context, attachments);
        }
        if (screens != null) {
            GalacticCoreScreens.register(context, screens);
        }
        if (capabilities != null && saveData != null) {
            GalacticCoreMachines.register(context, capabilities, saveData);
        }
        if (capabilities != null && resources != null && screens != null) {
            GalacticCoreEchoIntegrations.register(context, capabilities, resources, screens);
        }
    }

    public static void registerClientRuntime(EchoNativeModuleLoadContext context) {
        GalacticCoreNativeMutations.service(context, "echo.native.render", EchoNativeRenderService.class)
                .ifPresent(render -> {
                    GalacticCoreNativeMutations.record(
                            context,
                            render.registerHudOverlay(GalacticCoreNativeMutations.client(
                                    "render",
                                    "registerHudOverlay",
                                    GalacticCoreIds.id("hud/oxygen_tanks"),
                                    Map.of("legacySource", "OverlayOxygenTanks", "screenCoreReady", true)
                            ))
                    );
                    GalacticCoreNativeMutations.record(
                            context,
                            render.registerRenderHook(GalacticCoreNativeMutations.client(
                                    "render",
                                    "registerRenderHook",
                                    GalacticCoreIds.id("render/rocket_and_lander"),
                                    Map.of("legacySource", "RenderTier1Rocket + RenderLander", "renderCoreReady", true)
                            ))
                    );
                });
        GalacticCoreNativeMutations.service(context, "echo.native.screens", EchoNativeScreenService.class)
                .ifPresent(screens -> GalacticCoreNativeMutations.record(
                        context,
                        screens.registerKeybind(GalacticCoreNativeMutations.client(
                                "screens",
                                "registerKeybind",
                                GalacticCoreIds.id("keybind/open_celestial_selection"),
                                Map.of("legacySource", "GuiCelestialSelection", "replacement", "HoloMap route surface")
                        ))
                ));
    }

    public static void registerServerRuntime(EchoNativeModuleLoadContext context) {
        GalacticCoreNativeMutations.service(context, "echo.native.lifecycle", EchoNativeLifecycleService.class)
                .ifPresent(lifecycle -> GalacticCoreNativeMutations.record(
                        context,
                        lifecycle.registerGameTest(GalacticCoreNativeMutations.server(
                                "lifecycle",
                                "registerGameTest",
                                GalacticCoreIds.id("gametest/native_parity_smoke"),
                                Map.of("requiredSurfaces", List.of("registry", "network", "capabilities", "attachments", "worldgen"))
                        ))
                ));
    }

    public static void shutdown(EchoNativeModuleLoadContext context) {
        GalacticCoreNativeMutations.service(context, "echo.native.lifecycle", EchoNativeLifecycleService.class)
                .ifPresent(lifecycle -> GalacticCoreNativeMutations.record(
                        context,
                        lifecycle.shutdown(GalacticCoreNativeMutations.common(
                                "lifecycle",
                                "shutdown",
                                GalacticCoreIds.id("shutdown/stable_unload"),
                                Map.of("stableUnload", true)
                        ))
                ));
    }

    private static void registerExecutableRuntimeContracts(EchoNativeModuleLoadContext context) {
        GalacticCoreRuntimeService runtime = context.serviceRegistry()
                .service(GalacticCoreIds.MOD_ID, GalacticCoreIds.id("runtime"), GalacticCoreRuntimeService.class)
                .orElse(null);
        if (runtime == null) {
            return;
        }
        GalacticCoreNativeMutations.service(context, "echo.native.capabilities", EchoNativeCapabilityService.class)
                .ifPresent(capabilities -> GalacticCoreNativeMutations.record(
                        context,
                        capabilities.mutate(GalacticCoreNativeMutations.common(
                                "capabilities",
                                "installRuntimeModel",
                                GalacticCoreIds.id("runtime/gameplay_models"),
                                runtime.evidence()
                        ))
                ));
        GalacticCoreNativeMutations.service(context, "echo.native.attachments", EchoNativeAttachmentService.class)
                .ifPresent(attachments -> GalacticCoreNativeMutations.record(
                        context,
                        attachments.attach(GalacticCoreNativeMutations.common(
                                "attachments",
                                "attach",
                                GalacticCoreIds.id("runtime/player_progression_state"),
                                Map.of(
                                        "source", "galacticraft_legacy_runtime_parity",
                                        "typedReceiptsOnly", true,
                                        "startingProgression", GalacticCoreRuntimeService.PlayerProgression.starting().toString(),
                                        "replaces", "GCPlayerStats schematic and route mutation"
                                )
                        ))
                ));
        GalacticCoreNativeMutations.service(context, "echo.native.save_data", EchoNativeSaveDataService.class)
                .ifPresent(saveData -> GalacticCoreNativeMutations.record(
                        context,
                        saveData.write(GalacticCoreNativeMutations.common(
                                "save_data",
                                "write",
                                GalacticCoreIds.id("runtime/default_machine_states"),
                                Map.of(
                                        "source", "galacticraft_legacy_runtime_parity",
                                        "typedReceiptsOnly", true,
                                        "oxygenCollector", runtime.defaultMachine(GalacticCoreRuntimeService.MachineType.OXYGEN_COLLECTOR).toString(),
                                        "oxygenSealer", runtime.defaultMachine(GalacticCoreRuntimeService.MachineType.OXYGEN_SEALER).toString(),
                                        "fuelLoader", runtime.defaultMachine(GalacticCoreRuntimeService.MachineType.FUEL_LOADER).toString(),
                                        "rocketWorkbench", runtime.defaultMachine(GalacticCoreRuntimeService.MachineType.ROCKET_WORKBENCH).toString()
                                )
                        ))
                ));
        GalacticCoreNativeMutations.service(context, "echo.native.save_data", EchoNativeSaveDataService.class)
                .ifPresent(saveData -> GalacticCoreNativeMutations.record(
                        context,
                        saveData.write(GalacticCoreNativeMutations.common(
                                "save_data",
                                "write",
                                GalacticCoreIds.id("runtime/route_and_dungeon_progression"),
                                Map.of(
                                        "source", "galacticraft_legacy_runtime_parity",
                                        "typedReceiptsOnly", true,
                                        "moonRewardUnlocks", "route/mars, schematic/tier_2_rocket",
                                        "marsRewardUnlocks", "route/asteroids, route/venus, schematic/tier_3_rocket",
                                        "venusRewardUnlocks", "schematic/astro_miner"
                                )
                        ))
                ));
        GalacticCoreNativeMutations.service(context, "echo.native.worldgen", EchoNativeWorldgenService.class)
                .ifPresent(worldgen -> GalacticCoreNativeMutations.record(
                        context,
                        worldgen.registerFeature(GalacticCoreNativeMutations.common(
                                "worldgen",
                                "registerFeature",
                                GalacticCoreIds.id("runtime/environment_scan_models"),
                                Map.of(
                                        "source", "galacticraft_legacy_runtime_parity",
                                        "typedReceiptsOnly", true,
                                        "moon", runtime.scanEnvironment(GalacticCoreIds.id("moon")).toString(),
                                        "mars", runtime.scanEnvironment(GalacticCoreIds.id("mars")).toString(),
                                        "venus", runtime.scanEnvironment(GalacticCoreIds.id("venus")).toString()
                                )
                        ))
                ));
    }

    private static void registerRuntimeGatewayContracts(EchoNativeModuleLoadContext context) {
        GalacticCoreRuntimeGateway gateway = context.serviceRegistry()
                .service(GalacticCoreIds.MOD_ID, GalacticCoreIds.id("runtime_gateway"), GalacticCoreRuntimeGateway.class)
                .orElse(null);
        if (gateway == null) {
            return;
        }
        GalacticCoreNativeMutations.service(context, "echo.native.capabilities", EchoNativeCapabilityService.class)
                .ifPresent(capabilities -> GalacticCoreNativeMutations.record(
                        context,
                        capabilities.mutate(GalacticCoreNativeMutations.common(
                                "capabilities",
                                "installRuntimeGateway",
                                GalacticCoreIds.id("runtime/gateway"),
                                gateway.evidence()
                        ))
                ));
        gateway.releaseSmokeActions().forEach(action -> {
            if ("events".equals(action.surface())) {
                GalacticCoreNativeMutations.service(context, "echo.native.events", EchoNativeEventService.class)
                        .ifPresent(events -> GalacticCoreNativeMutations.record(
                                context,
                                events.publish(GalacticCoreNativeMutations.common(
                                        action.surface(),
                                        action.action(),
                                        action.target(),
                                        action.evidence()
                                ))
                        ));
            } else if ("network".equals(action.surface())) {
                GalacticCoreNativeMutations.service(context, "echo.native.network", EchoNativeNetworkService.class)
                        .ifPresent(network -> {
                            if ("broadcast".equals(action.action())) {
                                GalacticCoreNativeMutations.record(
                                        context,
                                        network.broadcast(GalacticCoreNativeMutations.common(
                                                action.surface(),
                                                action.action(),
                                                action.target(),
                                                action.evidence()
                                        ))
                                );
                            } else {
                                GalacticCoreNativeMutations.record(
                                        context,
                                        network.sendToPlayer(GalacticCoreNativeMutations.common(
                                                action.surface(),
                                                action.action(),
                                                action.target(),
                                                action.evidence()
                                        ))
                                );
                            }
                        });
            } else if ("screens".equals(action.surface())) {
                GalacticCoreNativeMutations.service(context, "echo.native.screens", EchoNativeScreenService.class)
                        .ifPresent(screens -> GalacticCoreNativeMutations.record(
                                context,
                                screens.open(GalacticCoreNativeMutations.client(
                                        action.surface(),
                                        action.action(),
                                        action.target(),
                                        action.evidence()
                                ))
                        ));
            } else if ("worldgen".equals(action.surface())) {
                recordWorldgenAction(context, action);
            }
        });
    }

    private static void registerRuntimeAdapterContracts(EchoNativeModuleLoadContext context) {
        GalacticCoreRuntimeAdapters adapters = context.serviceRegistry()
                .service(GalacticCoreIds.MOD_ID, GalacticCoreIds.id("runtime_adapters"), GalacticCoreRuntimeAdapters.class)
                .orElse(null);
        if (adapters == null) {
            return;
        }
        GalacticCoreNativeMutations.service(context, "echo.native.capabilities", EchoNativeCapabilityService.class)
                .ifPresent(capabilities -> GalacticCoreNativeMutations.record(
                        context,
                        capabilities.mutate(GalacticCoreNativeMutations.common(
                                "capabilities",
                                "installRuntimeAdapters",
                                GalacticCoreIds.id("runtime/adapters"),
                                adapters.evidence()
                        ))
                ));
        GalacticCoreNativeMutations.service(context, "echo.native.save_data", EchoNativeSaveDataService.class)
                .ifPresent(saveData -> GalacticCoreNativeMutations.record(
                        context,
                        saveData.write(GalacticCoreNativeMutations.common(
                                "save_data",
                                "write",
                                GalacticCoreIds.id("runtime/adapter_state_contracts"),
                                Map.ofEntries(
                                        Map.entry("source", "galacticraft_legacy_runtime_adapters"),
                                        Map.entry("typedReceiptsOnly", true),
                                        Map.entry("machineBlockEntities", "oxygen_collector, oxygen_sealer, fuel_loader, rocket_workbench"),
                                        Map.entry("dimensionTransfer", "route requirement plus environment scan"),
                                        Map.entry("transferPlacement", "worldgen placement target, landing coordinates, entry mode, host actions"),
                                        Map.entry("transferExecution", "host execution target, chunk ticket, teleport placement, and progression sync actions"),
                                        Map.entry("screenSurfaces", "HoloMap route surface, rendered route menu, route interactions, ScreenCore launch checklist, rendered checklist menu, and checklist controls"),
                                        Map.entry("treasureChestScreen", "ScreenCore treasure reward surface, loot preview, schematic preview, claim actions"),
                                        Map.entry("renderedMenus", "HoloMap route, ScreenCore checklist, and treasure chest layout/widget contracts"),
                                        Map.entry("dungeonStructures", "room plan, boss room, locked treasure room, worldgen target"),
                                        Map.entry("bossEntitySpawns", "legacy-derived boss entity source, spawn room, attributes, and host spawn actions"),
                                        Map.entry("bossEncounters", "health phase, key drop, save-data target"),
                                        Map.entry("bossAiSteps", "movement intent, attack intent, room lock state, host entity actions"),
                                        Map.entry("dungeonRewards", "treasure interaction and boss/key claim with attachment-backed progression")
                                )
                        ))
                ));
        GalacticCoreNativeMutations.service(context, "echo.native.worldgen", EchoNativeWorldgenService.class)
                .ifPresent(worldgen -> GalacticCoreNativeMutations.record(
                        context,
                        worldgen.registerFeature(GalacticCoreNativeMutations.common(
                                "worldgen",
                                "registerFeature",
                                GalacticCoreIds.id("runtime/dungeon_encounter_adapters"),
                                Map.of(
                                        "source", "galacticraft_legacy_runtime_adapters",
                                        "typedReceiptsOnly", true,
                                        "moon", "moon_dungeon_tier_1 -> evolved_skeleton_boss",
                                        "mars", "mars_dungeon_tier_2 -> evolved_creeper_boss",
                                        "venus", "venus_dungeon_tier_3 -> spider_queen"
                                )
                        ))
                ));
        adapters.releaseAdapterSmokeActions().forEach(action -> {
            if ("events".equals(action.surface())) {
                GalacticCoreNativeMutations.service(context, "echo.native.events", EchoNativeEventService.class)
                        .ifPresent(events -> GalacticCoreNativeMutations.record(
                                context,
                                events.publish(GalacticCoreNativeMutations.common(
                                        action.surface(),
                                        action.action(),
                                        action.target(),
                                        action.evidence()
                                ))
                        ));
            } else if ("network".equals(action.surface())) {
                GalacticCoreNativeMutations.service(context, "echo.native.network", EchoNativeNetworkService.class)
                        .ifPresent(network -> {
                            if ("broadcast".equals(action.action())) {
                                GalacticCoreNativeMutations.record(
                                        context,
                                        network.broadcast(GalacticCoreNativeMutations.common(
                                                action.surface(),
                                                action.action(),
                                                action.target(),
                                                action.evidence()
                                        ))
                                );
                            } else {
                                GalacticCoreNativeMutations.record(
                                        context,
                                        network.sendToPlayer(GalacticCoreNativeMutations.common(
                                                action.surface(),
                                                action.action(),
                                                action.target(),
                                                action.evidence()
                                        ))
                                );
                            }
                        });
            } else if ("screens".equals(action.surface())) {
                GalacticCoreNativeMutations.service(context, "echo.native.screens", EchoNativeScreenService.class)
                        .ifPresent(screens -> GalacticCoreNativeMutations.record(
                                context,
                                screens.open(GalacticCoreNativeMutations.client(
                                        action.surface(),
                                        action.action(),
                                        action.target(),
                                        action.evidence()
                                ))
                        ));
            } else if ("worldgen".equals(action.surface())) {
                recordWorldgenAction(context, action);
            }
        });
    }

    private static void registerHostCallbackContracts(EchoNativeModuleLoadContext context) {
        GalacticCoreHostCallbacks hostCallbacks = context.serviceRegistry()
                .service(GalacticCoreIds.MOD_ID, GalacticCoreIds.id("host_callbacks"), GalacticCoreHostCallbacks.class)
                .orElse(null);
        if (hostCallbacks == null) {
            return;
        }
        GalacticCoreNativeMutations.service(context, "echo.native.capabilities", EchoNativeCapabilityService.class)
                .ifPresent(capabilities -> GalacticCoreNativeMutations.record(
                        context,
                        capabilities.mutate(GalacticCoreNativeMutations.common(
                                "capabilities",
                                "installHostCallbacks",
                                GalacticCoreIds.id("runtime/host_callbacks"),
                                hostCallbacks.evidence()
                        ))
                ));
        GalacticCoreNativeMutations.service(context, "echo.native.lifecycle", EchoNativeLifecycleService.class)
                .ifPresent(lifecycle -> GalacticCoreNativeMutations.record(
                        context,
                        lifecycle.registerGameTest(GalacticCoreNativeMutations.server(
                                "lifecycle",
                                "registerGameTest",
                                GalacticCoreIds.id("gametest/host_callback_smoke"),
                                Map.of(
                                        "source", "galacticraft_legacy_host_callbacks",
                                        "typedReceiptsOnly", true,
                                        "callbacks", "machine, life_support, route, holomap, holomap_menu, holomap_interaction, screencore, screencore_menu, screencore_interaction, transfer, transfer_placement, transfer_execution, environment, dungeon_structure, boss_spawn, boss, boss_ai, treasure, treasure_screen, treasure_menu"
                                )
                        ))
                ));
        GalacticCoreNativeMutations.service(context, "echo.native.events", EchoNativeEventService.class)
                .ifPresent(events -> List.of(
                                "host_machine_block_entity_tick",
                                "host_player_life_support_tick",
                                "host_dimension_transfer_request",
                                "host_transfer_placement_prepare",
                                "host_dimension_transfer_execute",
                                "host_holomap_route_surface_open",
                                "host_holomap_route_menu_render",
                                "host_holomap_route_interaction",
                                "host_screencore_launch_checklist_open",
                                "host_screencore_launch_checklist_menu_render",
                                "host_screencore_launch_checklist_interaction",
                                "host_dungeon_structure_prepare",
                                "host_boss_entity_spawn",
                                "host_boss_encounter_tick",
                                "host_boss_ai_step",
                                "host_dungeon_treasure_interaction",
                                "host_treasure_chest_screen_open",
                                "host_treasure_chest_menu_render",
                                "host_dungeon_treasure_claim"
                        ).forEach(event -> GalacticCoreNativeMutations.record(
                                context,
                                events.subscribe(GalacticCoreNativeMutations.common(
                                        "events",
                                        "subscribe",
                                        GalacticCoreIds.id("event/" + event),
                                        Map.of(
                                                "source", "galacticraft_legacy_host_callbacks",
                                                "typedReceiptsOnly", true,
                                                "replacement", "ASDK host callback facade"
                                        )
                                ))
                        )));
        hostCallbacks.releaseHostCallbackSmokeActions().forEach(action -> {
            if ("events".equals(action.surface())) {
                GalacticCoreNativeMutations.service(context, "echo.native.events", EchoNativeEventService.class)
                        .ifPresent(events -> GalacticCoreNativeMutations.record(
                                context,
                                events.publish(GalacticCoreNativeMutations.common(
                                        action.surface(),
                                        action.action(),
                                        action.target(),
                                        action.evidence()
                                ))
                        ));
            } else if ("network".equals(action.surface())) {
                GalacticCoreNativeMutations.service(context, "echo.native.network", EchoNativeNetworkService.class)
                        .ifPresent(network -> {
                            if ("broadcast".equals(action.action())) {
                                GalacticCoreNativeMutations.record(
                                        context,
                                        network.broadcast(GalacticCoreNativeMutations.common(
                                                action.surface(),
                                                action.action(),
                                                action.target(),
                                                action.evidence()
                                        ))
                                );
                            } else {
                                GalacticCoreNativeMutations.record(
                                        context,
                                        network.sendToPlayer(GalacticCoreNativeMutations.common(
                                                action.surface(),
                                                action.action(),
                                                action.target(),
                                                action.evidence()
                                        ))
                                );
                            }
                        });
            } else if ("screens".equals(action.surface())) {
                GalacticCoreNativeMutations.service(context, "echo.native.screens", EchoNativeScreenService.class)
                        .ifPresent(screens -> GalacticCoreNativeMutations.record(
                                context,
                                screens.open(GalacticCoreNativeMutations.client(
                                        action.surface(),
                                        action.action(),
                                        action.target(),
                                        action.evidence()
                                ))
                        ));
            } else if ("worldgen".equals(action.surface())) {
                recordWorldgenAction(context, action);
            }
        });
    }

    private static void registerHostExecutionContracts(EchoNativeModuleLoadContext context) {
        GalacticCoreHostExecutionBridge bridge = context.serviceRegistry()
                .service(GalacticCoreIds.MOD_ID, GalacticCoreIds.id("host_execution_bridge"), GalacticCoreHostExecutionBridge.class)
                .orElse(null);
        if (bridge == null) {
            return;
        }
        GalacticCoreNativeMutations.service(context, "echo.native.capabilities", EchoNativeCapabilityService.class)
                .ifPresent(capabilities -> GalacticCoreNativeMutations.record(
                        context,
                        capabilities.mutate(GalacticCoreNativeMutations.common(
                                "capabilities",
                                "installHostExecutionBridge",
                                GalacticCoreIds.id("runtime/host_execution_bridge"),
                                bridge.evidence()
                        ))
                ));
        GalacticCoreNativeMutations.service(context, "echo.native.lifecycle", EchoNativeLifecycleService.class)
                .ifPresent(lifecycle -> GalacticCoreNativeMutations.record(
                        context,
                        lifecycle.registerGameTest(GalacticCoreNativeMutations.server(
                                "lifecycle",
                                "registerGameTest",
                                GalacticCoreIds.id("gametest/host_execution_bridge_smoke"),
                                Map.of(
                                        "source", "galacticraft_legacy_host_execution_bridge",
                                        "typedReceiptsOnly", true,
                                        "bindings", "transfer_execution, boss_spawn, rendered_menus"
                                )
                        ))
                ));
        GalacticCoreNativeMutations.service(context, "echo.native.save_data", EchoNativeSaveDataService.class)
                .ifPresent(saveData -> GalacticCoreNativeMutations.record(
                        context,
                        saveData.write(GalacticCoreNativeMutations.common(
                                "save_data",
                                "write",
                                GalacticCoreIds.id("runtime/host_execution_state_contracts"),
                                Map.of(
                                        "source", "galacticraft_legacy_host_execution_bridge",
                                        "typedReceiptsOnly", true,
                                        "transferExecution", "dimension load, chunk ticket, player placement, progression sync",
                                        "bossSpawn", "boss room load, entity spawn, encounter state attach, room lock",
                                        "renderedMenus", "renderer bind, widget mount, action wiring, screen state sync"
                                )
                        ))
                ));
        bridge.releaseHostExecutionSmokeActions().forEach(action -> {
            if ("events".equals(action.surface())) {
                GalacticCoreNativeMutations.service(context, "echo.native.events", EchoNativeEventService.class)
                        .ifPresent(events -> GalacticCoreNativeMutations.record(
                                context,
                                events.publish(GalacticCoreNativeMutations.common(
                                        action.surface(),
                                        action.action(),
                                        action.target(),
                                        action.evidence()
                                ))
                        ));
            } else if ("screens".equals(action.surface())) {
                GalacticCoreNativeMutations.service(context, "echo.native.screens", EchoNativeScreenService.class)
                        .ifPresent(screens -> GalacticCoreNativeMutations.record(
                                context,
                                screens.open(GalacticCoreNativeMutations.client(
                                        action.surface(),
                                        action.action(),
                                        action.target(),
                                        action.evidence()
                                ))
                        ));
            }
        });
    }

    private static void registerConcreteHostBindingContracts(EchoNativeModuleLoadContext context) {
        GalacticCoreHostBindingContracts bindings = context.serviceRegistry()
                .service(GalacticCoreIds.MOD_ID, GalacticCoreIds.id("host_binding_contracts"), GalacticCoreHostBindingContracts.class)
                .orElse(null);
        if (bindings == null) {
            return;
        }
        GalacticCoreNativeMutations.service(context, "echo.native.capabilities", EchoNativeCapabilityService.class)
                .ifPresent(capabilities -> {
                    GalacticCoreNativeMutations.record(
                            context,
                            capabilities.mutate(GalacticCoreNativeMutations.common(
                                    "capabilities",
                                    "installConcreteHostBindings",
                                    GalacticCoreIds.id("runtime/concrete_host_bindings"),
                                    bindings.evidence()
                            ))
                    );
                    bindings.releaseHostBindingSmokeContracts().stream()
                            .filter(contract -> "echo.native.capabilities".equals(contract.serviceId()))
                            .forEach(contract -> GalacticCoreNativeMutations.record(
                                    context,
                                    capabilities.registerIntegration(GalacticCoreNativeMutations.server(
                                            contract.surface(),
                                            contract.action(),
                                            contract.target(),
                                            contract.evidence()
                                    ))
                            ));
                });
        GalacticCoreNativeMutations.service(context, "echo.native.lifecycle", EchoNativeLifecycleService.class)
                .ifPresent(lifecycle -> GalacticCoreNativeMutations.record(
                        context,
                        lifecycle.registerGameTest(GalacticCoreNativeMutations.server(
                                "lifecycle",
                                "registerGameTest",
                                GalacticCoreIds.id("gametest/concrete_host_binding_smoke"),
                                Map.of(
                                        "source", "galacticraft_legacy_concrete_host_bindings",
                                        "typedReceiptsOnly", true,
                                        "bindings", "world_dimension_transfer, entity_boss_spawn, screen_menus"
                                )
                        ))
                ));
        GalacticCoreNativeMutations.service(context, "echo.native.save_data", EchoNativeSaveDataService.class)
                .ifPresent(saveData -> GalacticCoreNativeMutations.record(
                        context,
                        saveData.write(GalacticCoreNativeMutations.common(
                                "save_data",
                                "write",
                                GalacticCoreIds.id("runtime/concrete_host_binding_state_contracts"),
                                Map.of(
                                        "source", "galacticraft_legacy_concrete_host_bindings",
                                        "typedReceiptsOnly", true,
                                        "worldBinding", "dimension transfer placement owned by echo.native.worldgen",
                                        "entityBinding", "boss entity spawn owned by echo.native.capabilities",
                                        "screenBinding", "rendered menus owned by echo.native.screens"
                                )
                        ))
                ));
        bindings.releaseHostBindingSmokeContracts().forEach(contract -> {
            if ("echo.native.worldgen".equals(contract.serviceId())) {
                GalacticCoreNativeMutations.service(context, "echo.native.worldgen", EchoNativeWorldgenService.class)
                        .ifPresent(worldgen -> GalacticCoreNativeMutations.record(
                                context,
                                worldgen.placeStructure(GalacticCoreNativeMutations.server(
                                        contract.surface(),
                                        contract.action(),
                                        contract.target(),
                                        contract.evidence()
                                ))
                        ));
            } else if ("echo.native.screens".equals(contract.serviceId())) {
                GalacticCoreNativeMutations.service(context, "echo.native.screens", EchoNativeScreenService.class)
                        .ifPresent(screens -> GalacticCoreNativeMutations.record(
                                context,
                                screens.registerMenu(GalacticCoreNativeMutations.client(
                                        contract.surface(),
                                        contract.action(),
                                        contract.target(),
                                        contract.evidence()
                                ))
                        ));
            }
        });
    }

    private static void registerLiveHostAdapterContracts(EchoNativeModuleLoadContext context) {
        GalacticCoreLiveHostAdapters adapters = context.serviceRegistry()
                .service(GalacticCoreIds.MOD_ID, GalacticCoreIds.id("live_host_adapters"), GalacticCoreLiveHostAdapters.class)
                .orElse(null);
        if (adapters == null) {
            return;
        }
        GalacticCoreNativeMutations.service(context, "echo.native.capabilities", EchoNativeCapabilityService.class)
                .ifPresent(capabilities -> {
                    GalacticCoreNativeMutations.record(
                            context,
                            capabilities.mutate(GalacticCoreNativeMutations.common(
                                    "capabilities",
                                    "installLiveHostAdapters",
                                    GalacticCoreIds.id("runtime/live_host_adapters"),
                                    adapters.evidence()
                            ))
                    );
                    adapters.releaseLiveHostAdapterSmokePlans().stream()
                            .filter(plan -> "echo.native.capabilities".equals(plan.serviceId()))
                            .forEach(plan -> GalacticCoreNativeMutations.record(
                                    context,
                                    capabilities.registerIntegration(GalacticCoreNativeMutations.server(
                                            plan.surface(),
                                            plan.action(),
                                            plan.target(),
                                            plan.evidence()
                                    ))
                            ));
                });
        GalacticCoreNativeMutations.service(context, "echo.native.lifecycle", EchoNativeLifecycleService.class)
                .ifPresent(lifecycle -> GalacticCoreNativeMutations.record(
                        context,
                        lifecycle.registerGameTest(GalacticCoreNativeMutations.server(
                                "lifecycle",
                                "registerGameTest",
                                GalacticCoreIds.id("gametest/live_host_adapter_smoke"),
                                Map.of(
                                        "source", "galacticraft_legacy_live_host_adapters",
                                        "typedReceiptsOnly", true,
                                        "adapters", "world_dimension_transfer, entity_boss_spawn, screen_menus"
                                )
                        ))
                ));
        GalacticCoreNativeMutations.service(context, "echo.native.save_data", EchoNativeSaveDataService.class)
                .ifPresent(saveData -> GalacticCoreNativeMutations.record(
                        context,
                        saveData.write(GalacticCoreNativeMutations.common(
                                "save_data",
                                "write",
                                GalacticCoreIds.id("runtime/live_host_adapter_state_contracts"),
                                Map.of(
                                        "source", "galacticraft_legacy_live_host_adapters",
                                        "typedReceiptsOnly", true,
                                        "worldAdapter", "destination level resolve, chunk ticket, player placement, progression attachment sync",
                                        "entityAdapter", "boss room resolve, entity spawn, state attach, treasure room lock",
                                        "screenAdapter", "screen factory resolve, renderer mount, widget mount, action wire, state sync"
                                )
                        ))
                ));
        adapters.releaseLiveHostAdapterSmokePlans().forEach(plan -> {
            if ("echo.native.worldgen".equals(plan.serviceId())) {
                GalacticCoreNativeMutations.service(context, "echo.native.worldgen", EchoNativeWorldgenService.class)
                        .ifPresent(worldgen -> GalacticCoreNativeMutations.record(
                                context,
                                worldgen.placeStructure(GalacticCoreNativeMutations.server(
                                        plan.surface(),
                                        plan.action(),
                                        plan.target(),
                                        plan.evidence()
                                ))
                        ));
            } else if ("echo.native.screens".equals(plan.serviceId())) {
                GalacticCoreNativeMutations.service(context, "echo.native.screens", EchoNativeScreenService.class)
                        .ifPresent(screens -> GalacticCoreNativeMutations.record(
                                context,
                                screens.open(GalacticCoreNativeMutations.client(
                                        plan.surface(),
                                        plan.action(),
                                        plan.target(),
                                        plan.evidence()
                                ))
                        ));
            }
        });
    }

    private static void registerLiveHostEntrypointContracts(EchoNativeModuleLoadContext context) {
        GalacticCoreLiveHostEntrypoints entrypoints = context.serviceRegistry()
                .service(GalacticCoreIds.MOD_ID, GalacticCoreIds.id("live_host_entrypoints"), GalacticCoreLiveHostEntrypoints.class)
                .orElse(null);
        if (entrypoints == null) {
            return;
        }
        GalacticCoreNativeMutations.service(context, "echo.native.capabilities", EchoNativeCapabilityService.class)
                .ifPresent(capabilities -> {
                    GalacticCoreNativeMutations.record(
                            context,
                            capabilities.mutate(GalacticCoreNativeMutations.common(
                                    "capabilities",
                                    "installLiveHostEntrypoints",
                                    GalacticCoreIds.id("runtime/live_host_entrypoints"),
                                    entrypoints.evidence()
                            ))
                    );
                    entrypoints.releaseLiveHostEntrypointSmokeResults().stream()
                            .map(GalacticCoreLiveHostEntrypoints.LiveHostInvocationResult::action)
                            .filter(action -> "capabilities".equals(action.surface()))
                            .forEach(action -> GalacticCoreNativeMutations.record(
                                    context,
                                    capabilities.registerIntegration(GalacticCoreNativeMutations.server(
                                            action.surface(),
                                            action.action(),
                                            action.target(),
                                            action.evidence()
                                    ))
                            ));
                });
        GalacticCoreNativeMutations.service(context, "echo.native.lifecycle", EchoNativeLifecycleService.class)
                .ifPresent(lifecycle -> GalacticCoreNativeMutations.record(
                        context,
                        lifecycle.registerGameTest(GalacticCoreNativeMutations.server(
                                "lifecycle",
                                "registerGameTest",
                                GalacticCoreIds.id("gametest/live_host_entrypoint_smoke"),
                                Map.of(
                                        "source", "galacticraft_legacy_live_host_entrypoints",
                                        "typedReceiptsOnly", true,
                                        "entrypoints", "world_transfer, boss_spawn, screen_menus"
                                )
                        ))
                ));
        GalacticCoreNativeMutations.service(context, "echo.native.save_data", EchoNativeSaveDataService.class)
                .ifPresent(saveData -> GalacticCoreNativeMutations.record(
                        context,
                        saveData.write(GalacticCoreNativeMutations.common(
                                "save_data",
                                "write",
                                GalacticCoreIds.id("runtime/live_host_entrypoint_state_contracts"),
                                Map.of(
                                        "source", "galacticraft_legacy_live_host_entrypoints",
                                        "typedReceiptsOnly", true,
                                        "worldEntrypoint", "host callable dimension transfer entrypoint",
                                        "entityEntrypoint", "host callable boss spawn entrypoint",
                                        "screenEntrypoint", "host callable menu open entrypoints"
                                )
                        ))
                ));
        entrypoints.releaseLiveHostEntrypointSmokeResults().stream()
                .map(GalacticCoreLiveHostEntrypoints.LiveHostInvocationResult::action)
                .forEach(action -> {
                    if ("worldgen".equals(action.surface())) {
                        GalacticCoreNativeMutations.service(context, "echo.native.worldgen", EchoNativeWorldgenService.class)
                                .ifPresent(worldgen -> GalacticCoreNativeMutations.record(
                                        context,
                                        worldgen.placeStructure(GalacticCoreNativeMutations.server(
                                                action.surface(),
                                                action.action(),
                                                action.target(),
                                                action.evidence()
                                        ))
                                ));
                    } else if ("screens".equals(action.surface())) {
                        GalacticCoreNativeMutations.service(context, "echo.native.screens", EchoNativeScreenService.class)
                                .ifPresent(screens -> GalacticCoreNativeMutations.record(
                                        context,
                                        screens.open(GalacticCoreNativeMutations.client(
                                                action.surface(),
                                                action.action(),
                                                action.target(),
                                                action.evidence()
                                        ))
                                ));
                    }
                });
    }

    private static void registerPlatformExecutorContracts(EchoNativeModuleLoadContext context) {
        GalacticCorePlatformExecutors executors = context.serviceRegistry()
                .service(GalacticCoreIds.MOD_ID, GalacticCoreIds.id("platform_executors"), GalacticCorePlatformExecutors.class)
                .orElse(null);
        if (executors == null) {
            return;
        }
        GalacticCoreNativeMutations.service(context, "echo.native.capabilities", EchoNativeCapabilityService.class)
                .ifPresent(capabilities -> {
                    GalacticCoreNativeMutations.record(
                            context,
                            capabilities.mutate(GalacticCoreNativeMutations.common(
                                    "capabilities",
                                    "installPlatformExecutors",
                                    GalacticCoreIds.id("runtime/platform_executors"),
                                    executors.evidence()
                            ))
                    );
                    executors.releasePlatformExecutorSmokeResults().stream()
                            .map(GalacticCorePlatformExecutors.PlatformExecutionResult::action)
                            .filter(action -> "capabilities".equals(action.surface()))
                            .forEach(action -> GalacticCoreNativeMutations.record(
                                    context,
                                    capabilities.registerIntegration(GalacticCoreNativeMutations.server(
                                            action.surface(),
                                            action.action(),
                                            action.target(),
                                            action.evidence()
                                    ))
                            ));
                });
        GalacticCoreNativeMutations.service(context, "echo.native.lifecycle", EchoNativeLifecycleService.class)
                .ifPresent(lifecycle -> GalacticCoreNativeMutations.record(
                        context,
                        lifecycle.registerGameTest(GalacticCoreNativeMutations.server(
                                "lifecycle",
                                "registerGameTest",
                                GalacticCoreIds.id("gametest/platform_executor_smoke"),
                                Map.of(
                                        "source", "galacticraft_legacy_platform_executor_facade",
                                        "typedReceiptsOnly", true,
                                        "platformMutationDeferred", true,
                                        "executors", "world_transfer, boss_spawn, screen_menus"
                                )
                        ))
                ));
        GalacticCoreNativeMutations.service(context, "echo.native.save_data", EchoNativeSaveDataService.class)
                .ifPresent(saveData -> GalacticCoreNativeMutations.record(
                        context,
                        saveData.write(GalacticCoreNativeMutations.common(
                                "save_data",
                                "write",
                                GalacticCoreIds.id("runtime/platform_executor_state_contracts"),
                                Map.of(
                                        "source", "galacticraft_legacy_platform_executor_facade",
                                        "typedReceiptsOnly", true,
                                        "platformMutationDeferred", true,
                                        "worldExecutor", "dimension transfer executor facade state",
                                        "entityExecutor", "boss spawn executor facade state",
                                        "screenExecutor", "screen open executor facade state"
                                )
                        ))
                ));
        executors.releasePlatformExecutorSmokeResults().stream()
                .map(GalacticCorePlatformExecutors.PlatformExecutionResult::action)
                .forEach(action -> {
                    if ("worldgen".equals(action.surface())) {
                        GalacticCoreNativeMutations.service(context, "echo.native.worldgen", EchoNativeWorldgenService.class)
                                .ifPresent(worldgen -> GalacticCoreNativeMutations.record(
                                        context,
                                        worldgen.placeStructure(GalacticCoreNativeMutations.server(
                                                action.surface(),
                                                action.action(),
                                                action.target(),
                                                action.evidence()
                                        ))
                                ));
                    } else if ("screens".equals(action.surface())) {
                        GalacticCoreNativeMutations.service(context, "echo.native.screens", EchoNativeScreenService.class)
                                .ifPresent(screens -> GalacticCoreNativeMutations.record(
                                        context,
                                        screens.open(GalacticCoreNativeMutations.client(
                                                action.surface(),
                                                action.action(),
                                                action.target(),
                                                action.evidence()
                                        ))
                                ));
                    }
                });
    }

    private static void registerLiveSessionMutationContracts(EchoNativeModuleLoadContext context) {
        GalacticCoreLiveSessionMutations liveSessionMutations = context.serviceRegistry()
                .service(GalacticCoreIds.MOD_ID, GalacticCoreIds.id("live_session_mutations"), GalacticCoreLiveSessionMutations.class)
                .orElse(null);
        if (liveSessionMutations == null) {
            return;
        }
        GalacticCoreNativeMutations.service(context, "echo.native.capabilities", EchoNativeCapabilityService.class)
                .ifPresent(capabilities -> {
                    GalacticCoreNativeMutations.record(
                            context,
                            capabilities.mutate(GalacticCoreNativeMutations.common(
                                    "capabilities",
                                    "installLiveSessionMutations",
                                    GalacticCoreIds.id("runtime/live_session_mutations"),
                                    liveSessionMutations.evidence()
                            ))
                    );
                    liveSessionMutations.releaseLiveSessionMutationSmokeResults().stream()
                            .map(GalacticCoreLiveSessionMutations.LiveSessionMutationResult::action)
                            .filter(action -> "capabilities".equals(action.surface()))
                            .forEach(action -> GalacticCoreNativeMutations.record(
                                    context,
                                    capabilities.registerIntegration(GalacticCoreNativeMutations.server(
                                            action.surface(),
                                            action.action(),
                                            action.target(),
                                            action.evidence()
                                    ))
                            ));
                });
        GalacticCoreNativeMutations.service(context, "echo.native.lifecycle", EchoNativeLifecycleService.class)
                .ifPresent(lifecycle -> GalacticCoreNativeMutations.record(
                        context,
                        lifecycle.registerGameTest(GalacticCoreNativeMutations.server(
                                "lifecycle",
                                "registerGameTest",
                                GalacticCoreIds.id("gametest/live_session_mutation_smoke"),
                                Map.of(
                                        "source", "galacticraft_legacy_live_session_mutation_bridge",
                                        "typedReceiptsOnly", true,
                                        "hostOwnedMutationBoundary", true,
                                        "mutations", "world_transfer, boss_spawn, screen_menus"
                                )
                        ))
                ));
        GalacticCoreNativeMutations.service(context, "echo.native.save_data", EchoNativeSaveDataService.class)
                .ifPresent(saveData -> GalacticCoreNativeMutations.record(
                        context,
                        saveData.write(GalacticCoreNativeMutations.common(
                                "save_data",
                                "write",
                                GalacticCoreIds.id("runtime/live_session_mutation_state_contracts"),
                                Map.of(
                                        "source", "galacticraft_legacy_live_session_mutation_bridge",
                                        "typedReceiptsOnly", true,
                                        "hostOwnedMutationBoundary", true,
                                        "worldMutation", "host-owned teleport and progression sync boundary",
                                        "entityMutation", "host-owned boss construction and encounter attachment boundary",
                                        "screenMutation", "host-owned menu open and state sync boundary"
                                )
                        ))
                ));
        liveSessionMutations.releaseLiveSessionMutationSmokeResults().stream()
                .map(GalacticCoreLiveSessionMutations.LiveSessionMutationResult::action)
                .forEach(action -> {
                    if ("worldgen".equals(action.surface())) {
                        GalacticCoreNativeMutations.service(context, "echo.native.worldgen", EchoNativeWorldgenService.class)
                                .ifPresent(worldgen -> GalacticCoreNativeMutations.record(
                                        context,
                                        worldgen.placeStructure(GalacticCoreNativeMutations.server(
                                                action.surface(),
                                                action.action(),
                                                action.target(),
                                                action.evidence()
                                        ))
                                ));
                    } else if ("screens".equals(action.surface())) {
                        GalacticCoreNativeMutations.service(context, "echo.native.screens", EchoNativeScreenService.class)
                                .ifPresent(screens -> GalacticCoreNativeMutations.record(
                                        context,
                                        screens.open(GalacticCoreNativeMutations.client(
                                                action.surface(),
                                                action.action(),
                                                action.target(),
                                                action.evidence()
                                        ))
                                ));
                    }
                });
    }

    private static void recordWorldgenAction(
            EchoNativeModuleLoadContext context,
            GalacticCoreRuntimeGateway.RuntimeAction action
    ) {
        GalacticCoreNativeMutations.service(context, "echo.native.worldgen", EchoNativeWorldgenService.class)
                .ifPresent(worldgen -> GalacticCoreNativeMutations.record(
                        context,
                        "placeStructure".equals(action.action())
                                ? worldgen.placeStructure(GalacticCoreNativeMutations.common(
                                        action.surface(),
                                        action.action(),
                                        action.target(),
                                        action.evidence()
                                ))
                                : worldgen.registerFeature(GalacticCoreNativeMutations.common(
                                        action.surface(),
                                        action.action(),
                                        action.target(),
                                        action.evidence()
                                ))
                ));
    }
}
