package com.knoxhack.echoashfallprotocol;

import com.knoxhack.echo.adaptercore.EchoNativeEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeGameplayLifecycleHostPlan;
import com.knoxhack.echo.adaptercore.EchoNativeGameplayLifecycleHostRuntime;
import com.knoxhack.echo.adaptercore.EchoNativeLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeMinecraftRuntimeAdapterReadinessReport;
import com.knoxhack.echo.adaptercore.EchoNativeRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeApiFreeze;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeGapAudit;
import com.knoxhack.echo.adaptercore.EchoNativeServiceBridge;
import com.knoxhack.echoashfallprotocol.echo.AshfallBetaRouteContract;
import com.knoxhack.echoashfallprotocol.nativebridge.AshfallNativeEarlyEventRuntimeBinding;
import com.knoxhack.echoashfallprotocol.nativebridge.AshfallNativeExistingPlayerRepairRuntime;
import com.knoxhack.echoashfallprotocol.nativebridge.AshfallNativeExplorationRuntimeBinding;
import com.knoxhack.echoashfallprotocol.nativebridge.AshfallNativeFirstJoinProfile;
import com.knoxhack.echoashfallprotocol.nativebridge.AshfallNativeFirstJoinParityVerifier;
import com.knoxhack.echoashfallprotocol.nativebridge.AshfallNativeFirstSpawnEquivalenceHarness;
import com.knoxhack.echoashfallprotocol.nativebridge.AshfallNativeFirstSpawnRuntimeBinding;
import com.knoxhack.echoashfallprotocol.nativebridge.AshfallNativeHazardRuntimeBinding;
import com.knoxhack.echoashfallprotocol.nativebridge.AshfallNativeLateGameRouteBootstrap;
import com.knoxhack.echoashfallprotocol.nativebridge.AshfallNativeLateRuntimeBinding;
import com.knoxhack.echoashfallprotocol.nativebridge.AshfallNativeMajorRouteBootstrap;
import com.knoxhack.echoashfallprotocol.nativebridge.AshfallNativeMidgameRouteBootstrap;
import com.knoxhack.echoashfallprotocol.nativebridge.AshfallNativeMachineRuntimeBinding;
import com.knoxhack.echoashfallprotocol.nativebridge.AshfallNativeRecoveryHandoff;
import com.knoxhack.echoashfallprotocol.nativebridge.AshfallNativeRouteHazardHandoff;
import com.knoxhack.echoashfallprotocol.nativebridge.AshfallNativeRuntimePacketBindings;
import com.knoxhack.echoashfallprotocol.nativebridge.AshfallNativeRuntimeHardening;
import com.knoxhack.echoashfallprotocol.nativebridge.AshfallNativeRuntimeMutationEvidence;
import com.knoxhack.echoashfallprotocol.nativebridge.AshfallNativeSurfaceConsumers;
import com.knoxhack.echoashfallprotocol.nativebridge.AshfallNativeUiHudHandoff;
import com.knoxhack.echoashfallprotocol.registry.ModCreativeTabs;
import dev.echo.nativeplatform.contracts.EchoNativeActivationSurfaceRegistrar;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoAshfallNativeModule implements EchoNativeModuleEntrypoint {
    @Override
    public void discover(EchoNativeModuleLoadContext context) {
        context.attribute("nativeEntrypointBridge", "direct_native_module_entrypoint");
        context.attribute("nativeEntrypointClass", getClass().getName());
        context.attribute("nativeModuleEntrypoint", true);
        context.attribute("nativeActivationEntrypoint", false);
    }

    @Override
    public void registerServices(EchoNativeModuleLoadContext context) {
        EchoNativeActivationSurfaceRegistrar.registerServices(
                context,
                this,
                activation(context),
                "native_module_entrypoint",
                "direct_native_module_entrypoint"
        );
    }

    @Override
    public void registerContent(EchoNativeModuleLoadContext context) {
        EchoNativeActivationSurfaceRegistrar.registerContent(context, activation(context));
    }

    @Override
    public void ready(EchoNativeModuleLoadContext context) {
        context.attribute("ashfallNativeModuleReady", true);
        EchoNativeActivationSurfaceRegistrar.ready(context);
    }

    private Map<String, Object> activation(EchoNativeModuleLoadContext context) {
        return EchoNativeActivationSurfaceRegistrar.activation(
                context,
                () -> describeNativeSurfaces(EchoNativeActivationSurfaceRegistrar.bridgeContext(context))
        );
    }

    private Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, String> safeContext = context == null ? Map.of() : context;
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover Ashfall descriptor and feature contracts.")
                .phase("register_logical_content", "Record Ashfall logical blocks, items, effects, routes, and gameplay contracts before Minecraft registry mutation exists.")
                .phase("attach_events", "Record Ashfall gameplay event hooks for the future native event bridge.")
                .phase("world_ready", "Prepare Ashfall world bootstrap hooks after native registry and lifecycle bridges are implemented.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("block", "echoashfallprotocol:echo_crate", "Ashfall field loot crate.")
                .register("block", "echoashfallprotocol:echo_cache", "Ashfall sealed recovery cache.")
                .register("block", "echoashfallprotocol:micro_generator", "Ashfall early power generator with fuel, wear, local buffer, and adjacent output.")
                .register("block", "echoashfallprotocol:battery_bank", "Ashfall power storage buffer with adjacent distribution and battery balancing.")
                .register("block", "echoashfallprotocol:scrap_dynamo", "Ashfall steadier scrap-fueled power generator contract.")
                .register("block", "echoashfallprotocol:scrap_press", "Ashfall powered machine crafting bridge with HopperHandler automation.")
                .register("block", "echoashfallprotocol:thermal_burner", "Ashfall heat machine for the first outpost processing line.")
                .register("block", "echoashfallprotocol:filter_workbench", "Ashfall hazard-supply workbench for gas mask filter cartridges.")
                .register("block", "echoashfallprotocol:research_lab", "Ashfall research station for schematic fragments and survival doctrine.")
                .register("block", "echoashfallprotocol:ore_grinder", "Ashfall substrate processing machine with power, wear, byproducts, and HopperHandler automation.")
                .register("block", "echoashfallprotocol:isotope_refiner", "Ashfall powered catalyst refiner that converts ingots through crystal dust into clean or contaminated advanced outputs.")
                .register("block", "echoashfallprotocol:radiation_cleanser", "Ashfall powered decontamination machine that cleans contaminated salvage through advanced filters.")
                .register("block", "echoashfallprotocol:crystalline_synthesizer", "Ashfall phased endgame synthesizer with escalating power, wear, and power-failure output fallback.")
                .register("block", "echoashfallprotocol:deep_core_miner", "Ashfall depth-gated endgame resource generator with high power draw, wear, and output chaining.")
                .register("block", "echoashfallprotocol:autofeed_hopper", "Ashfall powered player-support machine that feeds hungry nearby players.")
                .register("block", "echoashfallprotocol:contaminant_condenser", "Ashfall powered world processor that converts nearby toxic puddles into sand.")
                .register("block", "echoashfallprotocol:item_pipe", "Ashfall item logistics pipe routing machine outputs into valid inputs.")
                .register("block", "echoashfallprotocol:power_cable", "Ashfall basic bidirectional power cable tier.")
                .register("block", "echoashfallprotocol:reinforced_power_cable", "Ashfall reinforced bidirectional power cable tier.")
                .register("block", "echoashfallprotocol:high_voltage_power_cable", "Ashfall high-voltage bidirectional power cable tier.")
                .register("block", "echoashfallprotocol:energy_meter", "Ashfall power diagnostic block showing grid storage, demand, bottleneck, and brownout state.")
                .register("block", "echoashfallprotocol:load_distributor", "Ashfall buffered power router with priority modes.")
                .register("block", "echoashfallprotocol:factory_controller", "Ashfall factory monitor scanning machines through pipes and cables.")
                .register("block", "echoashfallprotocol:acidic_sludge", "Ashfall toxic fluid terrain block used by wasteland hazard features.")
                .register("block", "echoashfallprotocol:fallout_dust", "Ashfall radioactive dust layer used by wasteland surface features.")
                .register("block", "echoashfallprotocol:contaminated_soil", "Ashfall contaminated terrain substrate used by toxic and radiation biomes.")
                .register("block", "echoashfallprotocol:wasteland_dirt", "Ashfall base wasteland terrain substrate.")
                .register("block", "echoashfallprotocol:wasteland_grass_block", "Ashfall wasteland grass terrain substrate.")
                .register("block", "echoashfallprotocol:ashen_wasteland_dirt", "Ashfall ashen wasteland dirt substrate.")
                .register("block", "echoashfallprotocol:burnt_wasteland_soil", "Ashfall burnt wasteland soil substrate.")
                .register("block", "echoashfallprotocol:toxic_wasteland_grass_block", "Ashfall toxic biome grass substrate.")
                .register("block", "echoashfallprotocol:mutated_wasteland_grass_block", "Ashfall mutated biome grass substrate.")
                .register("block", "echoashfallprotocol:irradiated_crust", "Ashfall irradiated biome crust substrate.")
                .register("block", "echoashfallprotocol:nexus_cracked_soil", "Ashfall nexus-scar cracked soil substrate.")
                .register("block", "echoashfallprotocol:oil_stained_concrete", "Ashfall industrial ruin concrete substrate.")
                .register("block", "echoashfallprotocol:cracked_asphalt", "Ashfall ruined city asphalt substrate.")
                .register("block", "echoashfallprotocol:concrete_rubble", "Ashfall concrete rubble worldgen substrate.")
                .register("block", "echoashfallprotocol:rusted_metal_sheet", "Ashfall rusted metal sheet ruin substrate.")
                .register("block", "echoashfallprotocol:toxic_waste_barrel", "Ashfall toxic waste barrel hazard feature.")
                .register("block", "echoashfallprotocol:mutated_bush", "Ashfall mutated bush hazard vegetation.")
                .register("block", "echoashfallprotocol:wasteland_stone", "Ashfall wasteland stone noise-settings default block.")
                .register("block", "echoashfallprotocol:wasteland_trace_rubble", "Ashfall trace-rubble biome substrate.")
                .register("block", "echoashfallprotocol:industrial_aggregate", "Ashfall industrial aggregate biome substrate.")
                .register("block", "echoashfallprotocol:toxic_slagstone", "Ashfall toxic slagstone biome substrate.")
                .register("block", "echoashfallprotocol:irradiated_shale", "Ashfall irradiated shale biome substrate.")
                .register("block", "echoashfallprotocol:cryogenic_fractured_stone", "Ashfall cryogenic fractured stone biome substrate.")
                .register("block", "echoashfallprotocol:crash_slag", "Ashfall crash-zone slag biome substrate.")
                .register("block", "echoashfallprotocol:nexus_scar_stone", "Ashfall nexus-scar nexus_scar_stone feature block.")
                .register("block", "echoashfallprotocol:echo_crystal", "Ashfall nexus crystal feature block.")
                .register("block", "echoashfallprotocol:energized_fissure", "Ashfall energized fissure feature block.")
                .register("block", "echoashfallprotocol:scorched_ash", "Ashfall crash-zone scorched ash surface block.")
                .register("block", "echoashfallprotocol:twisted_metal", "Ashfall crash-zone twisted metal feature block.")
                .register("block", "echoashfallprotocol:cable_bundle", "Ashfall ruined city cable bundle feature block.")
                .register("block", "echoashfallprotocol:cracked_earth", "Ashfall ruined plains cracked earth feature block.")
                .register("block", "echoashfallprotocol:ash_stone", "Ashfall ash stone feature block.")
                .register("block", "echoashfallprotocol:scrap_ore", "Ashfall salvage ore feature block.")
                .register("block", "echoashfallprotocol:thorn_scrub", "Ashfall thorn scrub hazard vegetation.")
                .register("block", "echoashfallprotocol:acid_mud", "Ashfall acid mud biome substrate.")
                .register("block", "echoashfallprotocol:ooze_crystal", "Ashfall toxic swamp ooze crystal feature block.")
                .register("block", "echoashfallprotocol:corroded_pipe", "Ashfall toxic/industrial corroded pipe feature block.")
                .register("block", "echoashfallprotocol:rebar_block", "Ashfall ruined structure rebar feature block.")
                .register("block", "echoashfallprotocol:shattered_glass", "Ashfall ruined city shattered glass feature block.")
                .register("block", "echoashfallprotocol:uranium_crystal", "Ashfall radiation zone uranium crystal feature block.")
                .register("block", "echoashfallprotocol:radioactive_sludge", "Ashfall radiation zone radioactive sludge feature block.")
                .register("block", "echoashfallprotocol:permafrost", "Ashfall cryogenic biome permafrost substrate.")
                .register("block", "echoashfallprotocol:blue_ice_crystal", "Ashfall cryogenic blue ice crystal feature block.")
                .register("block", "echoashfallprotocol:frozen_conduit", "Ashfall cryogenic frozen conduit feature block.")
                .register("item", "echoashfallprotocol:scrap_metal", "Core salvage material.")
                .register("item", "echoashfallprotocol:scrap_wire", "Salvaged wire used by Ashfall machine and scanner crafting.")
                .register("item", "echoashfallprotocol:circuit_board", "Salvage electronics progression item.")
                .register("item", "echoashfallprotocol:scrap_circuit", "Salvaged circuit bundle used by Ashfall research and faction bridge rewards.")
                .register("item", "echoashfallprotocol:machine_casing", "Ashfall machine frame used by mid-route factory and processing blocks.")
                .register("item", "echoashfallprotocol:energy_cell", "Ashfall portable energy component used by factory-controller and POI scanner progression.")
                .register("item", "echoashfallprotocol:portable_signal_scanner", "Durable Ashfall scanner that starts the scanner-led POI route loop.")
                .register("item", "echoashfallprotocol:basic_battery", "Portable FE storage item used by the Ashfall power route.")
                .register("item", "echoashfallprotocol:machine_upgrade_overclock", "Machine upgrade module that increases processing speed with higher power demand.")
                .register("item", "echoashfallprotocol:gas_mask", "Ashfall head-slot equipment required before marked toxic hazard pockets.")
                .register("item", "echoashfallprotocol:filter_cartridge_basic", "Ashfall basic gas mask filter reserve for toxic-zone expedition prep.")
                .register("item", "echoashfallprotocol:filter_cartridge_advanced", "Ashfall advanced gas mask filter reserve for longer toxic-zone expedition prep.")
                .register("item", "echoashfallprotocol:field_manual", "Ashfall onboarding manual.")
                .register("entity", "echoashfallprotocol:rad_zombie", "Ashfall wasteland hostile entity with native spawn/attribute parity contract.",
                        registryProperties("com.knoxhack.echoashfallprotocol.entity.ModEntities", "RAD_ZOMBIE"))
                .register("entity", "echoashfallprotocol:scout_drone", "Ashfall deployable scout drone entity with native route/action parity contract.",
                        registryProperties("com.knoxhack.echoashfallprotocol.entity.ModEntities", "SCOUT_DRONE"))
                .register("entity", "echoashfallprotocol:echo_companion_drone", "Ashfall companion drone entity with native player-state and command parity contract.",
                        registryProperties("com.knoxhack.echoashfallprotocol.entity.ModEntities", "ECHO_COMPANION_DRONE"))
                .register("entity", "echoashfallprotocol:mirror_command", "Ashfall Nexus finale boss entity with native route and progression parity contract.",
                        registryProperties("com.knoxhack.echoashfallprotocol.entity.ModEntities", "MIRROR_COMMAND"))
                .register("menu", "echoashfallprotocol:hand_recycler", "Ashfall Hand Recycler native menu mutation declaration.",
                        registryProperties("com.knoxhack.echoashfallprotocol.registry.ModMenuTypes", "HAND_RECYCLER"))
                .register("menu", "echoashfallprotocol:water_purifier", "Ashfall Water Purifier native menu mutation declaration.",
                        registryProperties("com.knoxhack.echoashfallprotocol.registry.ModMenuTypes", "WATER_PURIFIER"))
                .register("menu", "echoashfallprotocol:micro_generator", "Ashfall Micro Generator native menu mutation declaration.",
                        registryProperties("com.knoxhack.echoashfallprotocol.registry.ModMenuTypes", "MICRO_GENERATOR"))
                .register("menu", "echoashfallprotocol:scrap_press", "Ashfall Scrap Press native menu mutation declaration.",
                        registryProperties("com.knoxhack.echoashfallprotocol.registry.ModMenuTypes", "SCRAP_PRESS"))
                .register("sound", "echoashfallprotocol:ui.echo_message", "Ashfall ECHO UI message native sound declaration.",
                        registryProperties("com.knoxhack.echoashfallprotocol.registry.ModSounds", "ECHO_MESSAGE"))
                .register("sound", "echoashfallprotocol:ui.echo_complete", "Ashfall ECHO completion native sound declaration.",
                        registryProperties("com.knoxhack.echoashfallprotocol.registry.ModSounds", "ECHO_COMPLETE"))
                .register("sound", "echoashfallprotocol:event.radiation_storm", "Ashfall radiation storm native ambience declaration.",
                        registryProperties("com.knoxhack.echoashfallprotocol.registry.ModSounds", "RADIATION_STORM"))
                .register("sound", "echoashfallprotocol:event.blackout", "Ashfall blackout native ambience declaration.",
                        registryProperties("com.knoxhack.echoashfallprotocol.registry.ModSounds", "BLACKOUT"))
                .register("particle", "echoashfallprotocol:radiation_dust", "Ashfall native radiation dust particle profile for HUD/weather feedback.",
                        registryProperties("com.knoxhack.echoashfallprotocol.weather.AshfallWeatherRuntime", "radiation_dust"))
                .register("particle", "echoashfallprotocol:ash_snow", "Ashfall native ash-front particle profile for HUD/weather feedback.",
                        registryProperties("com.knoxhack.echoashfallprotocol.weather.AshfallWeatherRuntime", "ash_snow"))
                .register("effect", "echoashfallprotocol:alliance", "Ashfall Nexus alliance native mob-effect declaration.",
                        registryProperties("com.knoxhack.echoashfallprotocol.registry.ModEffects", "ALLIANCE"))
                .register("command", "echoashfallprotocol:drone", "Ashfall companion drone native command tree declaration.",
                        registryProperties("com.knoxhack.echoashfallprotocol.command.CompanionDroneCommands", "command"))
                .register("command", "echoashfallprotocol:ashfall", "Ashfall mission terminal native command tree declaration.",
                        registryProperties("com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreMissionTriggerRuntime", "ashfall"))
                .register("command", "echoashfallprotocol:echoevent", "Ashfall environmental event native command tree declaration.",
                        registryProperties("com.knoxhack.echoashfallprotocol.event.EnvironmentalEventCommandHandler", "echoevent"))
                .register("data_component", "echoashfallprotocol:stored_energy", "Ashfall stored-energy native data-component declaration.",
                        registryProperties("com.knoxhack.echoashfallprotocol.registry.ModDataComponents", "STORED_ENERGY"))
                .register("data_component", "echoashfallprotocol:ashfall_tooltip", "Ashfall tooltip native data-component declaration.",
                        registryProperties("com.knoxhack.echoashfallprotocol.registry.ModDataComponents", "ASHFALL_TOOLTIP"))
                .register(
                        "creative_tab",
                        "echoashfallprotocol:ashes_tab",
                        "Ashfall Protocol native module creative tab contract.",
                        creativeTabProperties(
                                "itemGroup.EchoAshfallProtocol",
                                "echoashfallprotocol:scrap_knife",
                                ashfallCreativeTabItems()
                        ))
                .register(
                        "creative_tab",
                        "echoashfallprotocol:native_modules_tab",
                        "Native Loader product creative tab that surfaces Ashfall essentials plus registered ECHO module items and blocks.",
                        nativeModulesCreativeTabProperties(
                                "itemGroup.EchoAshfallNativeModules",
                                "echoashfallprotocol:portable_signal_scanner",
                                nativeModulesCreativeTabItems()
                        ))
                .register("main_menu", "echoashfallprotocol:echo_native_main_menu", "Ashfall product main-menu surface projection.")
                .register("loading_screen", "echoashfallprotocol:echo_native_loading", "Ashfall product loading-screen surface projection.")
                .register("hud", "echoashfallprotocol:ashfall_survival_hud", "Ashfall survival HUD overlay surface projection.")
                .register("client_overlay", "echoashfallprotocol:ashfall_status_overlay", "Ashfall status, hazard, and mission overlay projection.")
                .register("terminal", "echoashfallprotocol:terminal_eui_handoff", "Ashfall terminal launch screen handoff projection.")
                .register("index", "echoashfallprotocol:index_handoff", "Ashfall Index catalog route handoff projection.")
                .register("lens", "echoashfallprotocol:lens_handoff", "Ashfall Lens inspection route handoff projection.")
                .register("holomap", "echoashfallprotocol:holomap_minimap_handoff", "Ashfall HoloMap minimap handoff projection.")
                .register("holomap", "echoashfallprotocol:holomap_fullscreen_handoff", "Ashfall HoloMap fullscreen navigation handoff projection.")
                .register("block_entity", "echoashfallprotocol:hand_recycler", "Native parity contract for HandRecyclerBlockEntity.")
                .register("block_entity", "echoashfallprotocol:water_purifier", "Native parity contract for WaterPurifierBlockEntity.")
                .register("block_entity", "echoashfallprotocol:rain_collector", "Native parity contract for RainCollectorBlockEntity.")
                .register("block_entity", "echoashfallprotocol:micro_generator", "Native parity contract for MicroGeneratorBlockEntity.")
                .register("block_entity", "echoashfallprotocol:thermal_array", "Native parity contract for ThermalArrayBlockEntity.")
                .register("block_entity", "echoashfallprotocol:battery_bank", "Native parity contract for BatteryBankBlockEntity.")
                .register("block_entity", "echoashfallprotocol:scrap_dynamo", "Native parity contract for ScrapDynamoBlockEntity.")
                .register("block_entity", "echoashfallprotocol:nexus_capacitor", "Native parity contract for NexusCapacitorBlockEntity.")
                .register("block_entity", "echoashfallprotocol:load_distributor", "Native parity contract for LoadDistributorBlockEntity.")
                .register("block_entity", "echoashfallprotocol:scrap_press", "Native parity contract for ScrapPressBlockEntity.")
                .register("block_entity", "echoashfallprotocol:signal_scanner", "Native parity contract for SignalScannerBlockEntity.")
                .register("block_entity", "echoashfallprotocol:field_med_bay", "Native parity contract for FieldMedBayBlockEntity.")
                .register("block_entity", "echoashfallprotocol:atmospheric_scrubber", "Native parity contract for AtmosphericScrubberBlockEntity.")
                .register("block_entity", "echoashfallprotocol:thermal_burner", "Native parity contract for ThermalBurnerBlockEntity.")
                .register("block_entity", "echoashfallprotocol:filter_workbench", "Native parity contract for FilterWorkbenchBlockEntity.")
                .register("block_entity", "echoashfallprotocol:power_node", "Native parity contract for PowerNodeBlockEntity.")
                .register("block_entity", "echoashfallprotocol:nexus_core", "Native parity contract for NexusCoreBlockEntity.")
                .register("block_entity", "echoashfallprotocol:ore_grinder", "Native parity contract for OreGrinderBlockEntity.")
                .register("block_entity", "echoashfallprotocol:isotope_refiner", "Native parity contract for IsotopeRefinerBlockEntity.")
                .register("block_entity", "echoashfallprotocol:radiation_cleanser", "Native parity contract for RadiationCleanserBlockEntity.")
                .register("block_entity", "echoashfallprotocol:crystalline_synthesizer", "Native parity contract for CrystallineSynthesizerBlockEntity.")
                .register("block_entity", "echoashfallprotocol:deep_core_miner", "Native parity contract for DeepCoreMinerBlockEntity.")
                .register("block_entity", "echoashfallprotocol:autofeed_hopper", "Native parity contract for AutofeedHopperBlockEntity.")
                .register("block_entity", "echoashfallprotocol:contaminant_condenser", "Native parity contract for ContaminantCondenserBlockEntity.")
                .register("block_entity", "echoashfallprotocol:item_pipe", "Native parity contract for ItemPipeBlockEntity.")
                .register("block_entity", "echoashfallprotocol:power_cable", "Native parity contract for PowerCableBlockEntity.")
                .register("block_entity", "echoashfallprotocol:factory_controller", "Native parity contract for FactoryControllerBlockEntity.")
                .register("block_entity", "echoashfallprotocol:structure_cache", "Native parity contract for StructureCacheBlockEntity.")
                .register("block_entity", "echoashfallprotocol:echo_container", "Native parity contract for EchoContainerBlockEntity.")
                .register("recipe", "echoashfallprotocol:scrap_press", "Scrap press recipe family.")
                .register("recipe", "echoashfallprotocol:ore_grinder", "Substrate grinder recipe table.")
                .register("worldgen", "echoashfallprotocol:wasteland_route", "Ashfall route and POI world profile.")
                .register("data_pack", "echoashfallprotocol:ashfall_worldgen_datapack", "Mount Ashfall product datapack resources for native world startup.")
                .register("resource_pack", "echoashfallprotocol:ashfall_client_resources", "Mount Ashfall product client resources for native menus, HUD, items, blocks, and surfaces.")
                .register("world_preset", "minecraft:normal", "Override the native product normal world preset with Ashfall wasteland biome selection.")
                .register("world_preset", "echoashfallprotocol:ashfall_wasteland", "Expose the Ashfall wasteland world preset as a native product startup option.")
                .register("worldgen", "echoashfallprotocol:wasteland_overworld_noise_settings", "Mount Ashfall wasteland overworld noise settings for native world creation.")
                .register("worldgen", "echoashfallprotocol:wasteland_biomes", "Mount Ashfall biome definitions for wasteland, ruined plains, crash zone, industrial ruins, toxic swamp, ruined cityscape, radiation zone, cryogenic ruins, and nexus scar.")
                .register("worldgen", "echoashfallprotocol:wasteland_structures", "Mount Ashfall POI, vault, ruin, bunker, crash, and route structure definitions.")
                .register("tag", "echoashfallprotocol:wasteland_worldgen_tags", "Mount Ashfall biome and structure tags used by native wasteland world startup.")
                .register("sound", "echoashfallprotocol:event.ash_storm", "Ashfall ash-storm weather feedback sound contract.");
        registerSourceBackedAshfallBlocks(registry);
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("server.started", "AshfallMissionCoreIntegration.registerWhenReady", "Attach Ashfall mission content after server bootstrap.")
                .hook("server.tick.post", "EchoAshfallProtocol.tryRegisterMissionCoreIntegration", "Retry optional mission integration until sibling modules are ready.")
                .hook("player.respawn_position", "EmergencyBunkRespawnEvents.onPlayerRespawnPosition", "Route emergency bunk respawn logic.")
                .hook("player.first_join", "PlayerStartingKitHandler.onPlayerLoggedIn", "Spawn or repair the personal drop pod, set respawn, grant the first objective, and give the starter briefing.")
                .hook("player.first_join", "AshfallAdapterCoreMissionTriggerRuntime.playerSpawned", "Start the Agent 6 beta mission trigger contract on player spawn.")
                .hook("world.tick", "AshfallAdapterCoreMissionTriggerRuntime.worldTick", "Update Agent 6 beta mission region objectives during live world ticks.")
                .hook("terminal.command", "AshfallAdapterCoreMissionTriggerRuntime.terminalMissionCommand", "Advance Agent 6 beta mission terminal objectives from the mission command.")
                .hook("hud.objective", "AshfallAdapterCoreMissionTriggerRuntime.publishHudObjective", "Display Agent 6 beta mission objective state through the native HUD-facing line.")
                .hook("save.player_data", "AshfallAdapterCoreMissionTriggerRuntime.writeMissionSnapshot", "Persist Agent 6 beta mission trigger state in player save data.")
                .hook("player.first_join", "AshfallNativeFirstJoinProfile.describe", "Expose the first-join crash recovery flow as native-safe gameplay data.")
                .hook("player.block_placed", "MissionCoreService.recordObjective", "Progress native block-placement objectives when Ash Campfire, Mutated Sapling, Rain Collector, Emergency Bunk, Hand Recycler, Micro Generator, Water Purifier, Battery Bank, Scrap Dynamo, Power Cable, Reinforced Power Cable, Energy Meter, Load Distributor, Scrap Press, Item Pipe, Thermal Burner, Filter Workbench, Research Lab, or Factory Controller anchors are placed.")
                .hook("player.use_block", "MissionCoreService.recordObjective", "Progress native use-block objectives such as Load Distributor Survival First priority setup.")
                .hook("player.inventory_changed", "MissionCoreService.recordObjective", "Progress native item and inventory-predicate objectives for tools, water, food buffers, ration stockpiles, field-kit assembly, schematic authentication, machine casing production, clean-water reserves, charged batteries, machine upgrades, base-stability reserves, filter cartridges, and Portable Signal Scanner pickup.")
                .hook("player.equipment_changed", "MissionCoreService.recordObjective", "Progress native equipment objectives such as equipping the Gas Mask in the head slot.")
                .hook("player.consume_item", "MissionCoreService.recordObjective", "Progress the native Clean Water mission when hydration is proven by consumption.")
                .hook("player.sleep", "MissionCoreService.recordObjective", "Progress native shelter objectives when bed or Emergency Bunk sleep is confirmed.")
                .hook("ashfall.special_marker", "MissionCoreService.recordObjective", "Progress native special-marker objectives such as dirty-water collection, emergency filtration, and Load Distributor power:priority_set confirmation.")
                .hook("player.craft_item", "MissionCoreService.recordObjective", "Progress native craft-backed objectives such as Emergency Water Loop, Wasteland Field Kit assembly, and Portable Signal Scanner crafting.")
                .hook("ashfall.inventory_predicate", "MissionCoreService.recordObjective", "Progress native inventory-predicate objectives such as food buffers, ration stockpiles, field-kit tool checks, schematic-fragment checks, machine-casing checks, clean-water reserve checks, Overclock Module checks, Base Stability reserves, filter checks, and Portable Signal Scanner possession.")
                .hook("ashfall.block_requirement", "MissionCoreService.recordObjective", "Progress native block-requirement objectives such as Hand Recycler, Micro Generator, Water Purifier, Battery Bank, Scrap Dynamo, Power Cable, Reinforced Power Cable, Energy Meter, Load Distributor, Scrap Press, Item Pipe, Thermal Burner, Filter Workbench, Research Lab, Factory Controller construction, and Base Stability outpost checks once AdapterCore can inspect placed blocks.")
                .hook("ashfall.research_updated", "MissionCoreService.recordObjective", "Progress native research predicates such as the first schematic unlock once AdapterCore can inspect ResearchData.")
                .hook("ashfall.schematic_unlocked", "MissionCoreService.recordObjective", "Progress native schematic objectives when a Research Lab unlocks the first schematic category.")
                .hook("ashfall.mission_completed", "MissionCoreService.recordObjective", "Progress native alternate-completion objectives such as the Base Stability shortcut when First Faction Contact is already complete.")
                .hook("ashfall.equipment_predicate", "MissionCoreService.recordObjective", "Progress native equipment-predicate objectives such as Gas Mask head-slot verification.")
                .hook("ashfall.energy_predicate", "MissionCoreService.recordObjective", "Progress native charged-item objectives such as Basic Battery >= 1000 FE once AdapterCore can inspect item energy.")
                .hook("ashfall.terminal_page", "AshfallAdapterCoreMajorRouteRuntime.terminalPageOpened", "Progress native major-route objectives when Terminal route pages are opened.")
                .hook("ashfall.hazard_check", "AshfallAdapterCoreMajorRouteRuntime.relayWeatherWindowChecked", "Progress native major-route weather and hazard-check objectives.")
                .hook("ashfall.radiation_changed", "AshfallAdapterCoreHazardRuntime.radiationChanged", "Progress native radiation exposure and decay objectives from SurvivalData mutation.")
                .hook("ashfall.mutation_gained", "AshfallAdapterCoreHazardRuntime.mutationGained", "Progress native mutation objectives when MutationData gains a live mutation.")
                .hook("ashfall.treatment_applied", "AshfallAdapterCoreHazardRuntime.treatmentApplied", "Progress native treatment objectives when RadAway or medical support mutates player state.")
                .hook("ashfall.hazard_route_check", "AshfallAdapterCoreHazardRuntime.hazardRouteCheck", "Progress native hazard route checks from live survival hazard snapshots.")
                .hook("ashfall.location_visited", "AshfallAdapterCoreHazardRuntime.markLocation", "Progress native hazard location objectives such as radiation-zone scouting, lab, and vault route markers.")
                .hook("ashfall.lab_objective", "AshfallAdapterCoreHazardRuntime.labObjective", "Progress native Research Lab objectives from live lab interactions and schematic decoding.")
                .hook("ashfall.vault_objective", "AshfallAdapterCoreHazardRuntime.hazardRouteObjective", "Progress native Bio Lab, Reactor Ruin, and Military Vault objectives from live scanner route discoveries.")
                .hook("ashfall.boss_defeated", "AshfallAdapterCoreLateRuntime.bossDefeated", "Progress native Warden, Prime Relay commander, and path-finale boss defeat objectives from live death credit.")
                .hook("ashfall.relay_activated", "AshfallAdapterCoreLateRuntime.relayActivated", "Progress native radio relay and Prime Relay activation objectives from live block and campaign state.")
                .hook("player.machine_powered", "AshfallAdapterCoreLateRuntime.powerNodeState", "Progress native power-node state objectives from live NexusWorldData and PowerNodeBlock activation.")
                .hook("ashfall.scout_drone_route", "AshfallAdapterCoreLateRuntime.scoutDroneRoute", "Progress native scout-drone route objectives from deployed and scavenge-mode drone route intel.")
                .hook("ashfall.nexus_state", "AshfallAdapterCoreLateRuntime.nexusState", "Progress native Nexus campaign and shared world-state objectives from live NexusCampaignData/NexusWorldData snapshots.")
                .hook("ashfall.prime_relay_resolved", "AshfallAdapterCoreLateRuntime.primeRelayResolved", "Progress native Prime Relay resolution objectives when stabilize, sever, or override mutates the campaign.")
                .hook("ashfall.ending_choice", "AshfallAdapterCoreLateRuntime.endingChoice", "Progress native ending-choice objectives when restore, destroy, or control is committed.")
                .hook("ashfall.post_nexus_persisted", "AshfallAdapterCoreLateRuntime.postNexusPersisted", "Progress native post-Nexus persistence objectives when PostNexusData is saved and synced.")
                .hook("holomap.marker_selected", "AshfallAdapterCoreMajorRouteRuntime.holomapMarkerSelected", "Progress native major-route objectives when the First Relay Station marker is tracked.")
                .hook("player.scanner_used", "AshfallAdapterCoreMajorRouteRuntime.relayConsoleScanned", "Progress native major-route scan objectives such as the relay console.")
                .hook("powergrid.repair", "AshfallAdapterCoreMajorRouteRuntime.relayPowerCouplerRepaired", "Progress native major-route repair objectives such as the relay power coupler.")
                .hook("player.terminal_opened", "AshfallAdapterCoreMajorRouteRuntime.relayCacheClaimed", "Progress native schematic-fragment recovery and major-route cache objectives when loot containers are claimed.")
                .hook("terminal.route_record", "AshfallAdapterCoreMajorRouteRuntime.terminalRouteRecordUpdated", "Progress native major-route return objectives when Terminal route records are updated.")
                .hook("data.reload", "Ashfall JSON content reload", "Reload Ashfall route, POI, and survival definitions.");
        Map<String, Object> gameplayBootstrap = AshfallNativeGameplayBootstrap.initialize(safeContext);
        Map<String, Object> majorRouteBootstrap = AshfallNativeMajorRouteBootstrap.initialize(safeContext);
        Map<String, Object> midgameRouteBootstrap = AshfallNativeMidgameRouteBootstrap.initialize(safeContext);
        Map<String, Object> lateGameRouteBootstrap = AshfallNativeLateGameRouteBootstrap.initialize(safeContext);
        Map<String, Object> machinePowerRuntimeTarget = AshfallNativeMachinePowerRuntimeTarget.initialize(safeContext);
        Map<String, Object> machinePowerResourceAudit = AshfallNativeMachinePowerResourceAudit.run(safeContext);
        Map<String, Object> machineRuntimeBinding = AshfallNativeMachineRuntimeBinding.describe(
                machinePowerRuntimeTarget,
                machinePowerResourceAudit);
        Map<String, Object> agent9NativeTechRuntime = AshfallNativeAgent9TechRuntime.run(safeContext);
        Map<String, Object> agent9TechModuleEntrypoints = AshfallNativeAgent9TechModuleEntrypoints.fromRuntime(
                agent9NativeTechRuntime);
        Map<String, Object> firstJoinProfile = AshfallNativeFirstJoinProfile.describe(safeContext);
        @SuppressWarnings("unchecked")
        Map<String, Object> firstJoinExecution = (Map<String, Object>) firstJoinProfile.get("adapterCoreExecution");
        Map<String, Object> minecraftRuntimeHostCallQueue = childMap(firstJoinExecution, "minecraftRuntimeHostCallQueue");
        Map<String, Object> minecraftRuntimeAdapterReadiness = new EchoNativeMinecraftRuntimeAdapterReadinessReport(MODULE_ID).audit(
                "echoashfallprotocol:first_join_minecraft_runtime_adapter_readiness",
                childMap(firstJoinExecution, "minecraftRuntimeAdapterContract"));
        Map<String, Object> recoveryHandoff = AshfallNativeRecoveryHandoff.describe(firstJoinProfile, firstJoinExecution);
        Map<String, Object> existingPlayerRepairRuntime = AshfallNativeExistingPlayerRepairRuntime.execute(
                firstJoinProfile,
                childMap(recoveryHandoff, "recoveryNavigationHostInvocationContract"));
        Map<String, Object> uiHudConsumers = AshfallNativeSurfaceConsumers.uiHudScreenSafe(firstJoinProfile);
        Map<String, Object> uiHudHandoff = AshfallNativeUiHudHandoff.describe(firstJoinProfile, uiHudConsumers);
        Map<String, Object> weatherSoundAtmosphereConsumers = AshfallNativeSurfaceConsumers.weatherSoundAtmosphere(firstJoinProfile);
        Map<String, Object> routeHazardHandoff = AshfallNativeRouteHazardHandoff.describe(firstJoinProfile, weatherSoundAtmosphereConsumers);
        Map<String, Object> runtimePacketBindings = AshfallNativeRuntimePacketBindings.describe(
                firstJoinExecution,
                recoveryHandoff,
                uiHudHandoff,
                routeHazardHandoff);
        Map<String, Object> gameplayLifecycleHostPlan = new EchoNativeGameplayLifecycleHostPlan(MODULE_ID).plan(
                "echoashfallprotocol:agent3_gameplay_lifecycle_host_plan",
                childMap(firstJoinExecution, "minecraftRuntimeAdapterContract"),
                childMap(recoveryHandoff, "recoveryNavigationHostInvocationContract"),
                childMap(uiHudHandoff, "uiHudHostInvocationContract"),
                childMap(routeHazardHandoff, "atmosphereHostInvocationContract"));
        Map<String, Object> gameplayLifecycleHostRuntime = new EchoNativeGameplayLifecycleHostRuntime(MODULE_ID).execute(
                "echoashfallprotocol:agent3_gameplay_lifecycle_host_runtime",
                gameplayLifecycleHostPlan,
                childMap(firstJoinExecution, "minecraftRuntimeAdapterContract"),
                childMap(recoveryHandoff, "recoveryNavigationHostInvocationContract"),
                childMap(uiHudHandoff, "uiHudHostInvocationContract"),
                childMap(routeHazardHandoff, "atmosphereHostInvocationContract"));
        Map<String, Object> firstJoinParityVerifier = AshfallNativeFirstJoinParityVerifier.verify(
                firstJoinProfile,
                firstJoinExecution,
                gameplayLifecycleHostPlan,
                gameplayLifecycleHostRuntime);
        Map<String, Object> firstSpawnEquivalenceHarness = AshfallNativeFirstSpawnEquivalenceHarness.evaluate(
                firstJoinProfile,
                minecraftRuntimeAdapterReadiness,
                existingPlayerRepairRuntime,
                gameplayLifecycleHostRuntime,
                firstJoinParityVerifier);
        Map<String, Object> firstSpawnRuntimeBinding = AshfallNativeFirstSpawnRuntimeBinding.describe(
                firstJoinExecution,
                firstSpawnEquivalenceHarness);
        Map<String, Object> eventBridge = events.describe();
        Map<String, Object> earlyEventRuntimeBinding = AshfallNativeEarlyEventRuntimeBinding.describe(
                eventBridge,
                gameplayBootstrap);
        Map<String, Object> explorationRuntimeBinding = AshfallNativeExplorationRuntimeBinding.describe(
                eventBridge,
                gameplayBootstrap);
        Map<String, Object> hazardRuntimeBinding = AshfallNativeHazardRuntimeBinding.describe(
                eventBridge,
                midgameRouteBootstrap,
                machineRuntimeBinding);
        Map<String, Object> lateRuntimeBinding = AshfallNativeLateRuntimeBinding.describe(
                eventBridge,
                hazardRuntimeBinding,
                lateGameRouteBootstrap);
        Map<String, Object> runtimeHardening = AshfallNativeRuntimeHardening.describe(
                eventBridge,
                firstSpawnRuntimeBinding,
                earlyEventRuntimeBinding,
                machineRuntimeBinding,
                explorationRuntimeBinding,
                hazardRuntimeBinding,
                lateRuntimeBinding);
        Map<String, Object> nativeRuntimeApiFreeze = EchoNativeRuntimeApiFreeze.describe(MODULE_ID);
        Map<String, Object> liveRuntimeMutationEvidence = AshfallNativeRuntimeMutationEvidence.snapshot();
        boolean liveHostMutationRan = Boolean.TRUE.equals(liveRuntimeMutationEvidence.get("realNativeStateMutated"))
                && Boolean.TRUE.equals(liveRuntimeMutationEvidence.get("minecraftRuntimeAccessed"));
        Map<String, Object> nativeRuntimeGapAudit = new EchoNativeRuntimeGapAudit(MODULE_ID).audit(
                "echoashfallprotocol:adaptercore_native_runtime_gap_audit",
                List.of(
                        liveRuntimeMutationEvidence,
                        majorRouteBootstrap,
                        midgameRouteBootstrap,
                        lateGameRouteBootstrap,
                        firstJoinExecution,
                        minecraftRuntimeHostCallQueue,
                        recoveryHandoff,
                        uiHudHandoff,
                        routeHazardHandoff,
                        firstSpawnRuntimeBinding,
                        earlyEventRuntimeBinding,
                        machineRuntimeBinding,
                        explorationRuntimeBinding,
                        hazardRuntimeBinding,
                        lateRuntimeBinding,
                        runtimeHardening,
                        runtimePacketBindings,
                        gameplayLifecycleHostRuntime,
                        nativeRuntimeApiFreeze));
        Map<String, Object> agent6BetaRouteContract = Map.of(
                "id", AshfallBetaRouteContract.CONTRACT_ID,
                "moduleId", MODULE_ID,
                "missionId", AshfallBetaRouteContract.FIRST_MISSION_ID,
                "nextMissionId", AshfallBetaRouteContract.NEXT_MISSION_ID,
                "route", AshfallBetaRouteContract.betaRoute(),
                "objectiveCount", AshfallBetaRouteContract.betaObjectives().size(),
                "triggerIds", AshfallBetaRouteContract.betaObjectives().stream()
                        .map(objective -> objective.trigger().id())
                        .distinct()
                        .toList(),
                "nativeRuntimeClass", "com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreMissionTriggerRuntime",
                "standaloneRuntimeClass", "dev.echo.standalone.runtime.gameplay.EchoAshfallStandaloneMissionRuntime",
                "status", "PASS");
        EchoNativeServiceBridge services = new EchoNativeServiceBridge(MODULE_ID)
                .preparedSurfaceService("missions", "echoashfallprotocol:first_route_bootstrap", "gameplay_content",
                        "Validate the first Ashfall MissionCore/WorldCore gameplay data path for native loader activation.",
                        gameplayBootstrap,
                        "ashfall.missions", "ashfall.world", "ashfall.survival")
                .preparedSurfaceService("missions", "echoashfallprotocol:ashfall_first_playable_loop", "agent6_beta_route_contract",
                        "Binds the Agent 6 first playable loop to AdapterCore mission triggers shared by Native and Standalone runtimes.",
                        agent6BetaRouteContract,
                        "ashfall.missions", "adaptercore.events", "standalone.gameplay", "parity.agent6")
                .preparedSurfaceService("major_routes", "echoashfallprotocol:first_major_route_bootstrap", "major_route_gameplay_content",
                        "Validate the First Relay Station MissionCore/HoloMap/Lens/PowerGrid/Terminal route as AdapterCore native command evidence.",
                        majorRouteBootstrap,
                        "ashfall.major_routes", "ashfall.missions", "holomap.layers", "lens.scans", "powergrid.repair", "terminal.route_records")
                .preparedSurfaceService("midgame_routes", "echoashfallprotocol:midgame_route_bootstrap", "midgame_route_gameplay_content",
                        "Validate Ashfall biohazard, medical, radiation, dense-alloy, and thermal/geology routes as AdapterCore native route-state replay evidence.",
                        midgameRouteBootstrap,
                        "ashfall.midgame_routes", "ashfall.missions", "ashfall.radiation", "ashfall.medical", "ashfall.machine_power")
                .preparedSurfaceService("late_game_routes", "echoashfallprotocol:late_game_route_bootstrap", "late_game_route_gameplay_content",
                        "Validate Ashfall power nodes, relay stations, bosses, cryogenic ruins, Nexus core, Prime relays, final decision, and RESTORE/DESTROY/CONTROL endings as AdapterCore native route-state replay evidence.",
                        lateGameRouteBootstrap,
                        "ashfall.late_game_routes", "ashfall.nexus", "ashfall.endings", "ashfall.bosses", "ashfall.missions")
                .preparedSurfaceService("machine_power_logistics", "echoashfallprotocol:machine_power_runtime_target", "adaptercore_runtime_target",
                        "Bind Ashfall machine, power, logistics, event, and capability behavior to AdapterCore runtime surfaces with JDK-only rehearsal evidence.",
                        machinePowerRuntimeTarget,
                        "ashfall.machine_power", "ashfall.logistics", "adaptercore.runtime_targets")
                .preparedSurfaceService("machine_power_logistics", "echoashfallprotocol:machine_native_runtime_binding", "machine_native_runtime_binding",
                        "Validates Ashfall machine ticking, energy capability, item capability, persistence, and diagnostics source bindings; live mutation is claimed only from post-mutation evidence.",
                        machineRuntimeBinding,
                        "adaptercore.block_entities", "adaptercore.capabilities", "ashfall.machine_power", "ashfall.logistics")
                .preparedSurfaceService("agent9_tech_modules", "echoashfallprotocol:agent9_tech_module_entrypoints", "agent9_module_entrypoints",
                        "Executes Agent 9 machine, power, logistics, multiblock, vehicle, economy, loot, recipe, and base-grid module entrypoints against the native tech runtime packet.",
                        agent9TechModuleEntrypoints,
                        "adaptercore.agent9", "ashfall.machine_power", "ashfall.logistics", "ashfall.economy", "ashfall.vehicles")
                .preparedSurfaceService("machine_power_resources", "echoashfallprotocol:machine_power_resource_fidelity", "resource_fidelity_report",
                        "Verify machine, power, and logistics placeable blocks have blockstates, block models, item models, item definitions, block textures, loot tables, and native report visibility.",
                        machinePowerResourceAudit,
                        "ashfall.machine_power", "ashfall.logistics", "resources.placeable_blocks")
                .preparedSurfaceService("player_recovery", "echoashfallprotocol:first_join_crash_recovery", "first_join_spawn_and_respawn",
                        "Prepares the Ashfall personal drop pod, respawn repair, starter briefing, and first objective profile as AdapterCore native commands; live execution is claimed only from mutation evidence.",
                        firstJoinExecution,
                        "ashfall.first_join", "ashfall.drop_pod", "ashfall.respawn", "recovery.field_caches")
                .preparedSurfaceService("player_recovery", "echoashfallprotocol:first_join_minecraft_runtime_adapter_readiness", "minecraft_runtime_adapter_readiness",
                        "Audits first-join Minecraft-backed host adapter invocations for payload, source operation, idempotency, and host API readiness before native host implementation.",
                        minecraftRuntimeAdapterReadiness,
                        "adaptercore.host_adapters", "ashfall.first_join", "ashfall.drop_pod", "ashfall.respawn")
                .preparedSurfaceService("player_recovery", "echoashfallprotocol:first_join_minecraft_runtime_host_call_queue", "minecraft_runtime_host_call_queue",
                        "Prepares the first-join Minecraft-backed inventory, structure, respawn, advancement, screen, and HUD host calls; the queue is unconsumed until a live host mutation records evidence.",
                        minecraftRuntimeHostCallQueue,
                        "adaptercore.host_calls", "adaptercore.host_adapters", "ashfall.first_join", "ashfall.drop_pod", "ashfall.respawn")
                .preparedSurfaceService("holomap_lens_codex_wiki", "echoashfallprotocol:first_join_recovery_handoff", "recovery_navigation_handoff",
                        "Publishes first-join death/cache/grave handoff, recovery compass visibility, field-cache map layers, and existing-player repair evidence for Recovery and HoloMap consumers.",
                        recoveryHandoff,
                        "recovery.field_caches", "recovery.graves", "recovery.compass", "holomap.layers")
                .preparedSurfaceService("holomap_lens_codex_wiki", "echoashfallprotocol:first_join_recovery_navigation_host_call_queue", "recovery_navigation_host_call_queue",
                        "Prepares Recovery field-cache/grave, HoloMap visibility, and existing-player repair host calls; the surface queue remains unconsumed until a live host mutation records evidence.",
                        childMap(recoveryHandoff, "recoveryNavigationHostCallQueue"),
                        "adaptercore.surface_host_calls", "recovery.field_caches", "recovery.graves", "holomap.layers")
                .preparedSurfaceService("player_recovery", "echoashfallprotocol:existing_player_repair_runtime", "existing_player_repair_runtime",
                        "Executes the returning-player repair branch for underground pods, missing respawn bindings, and Terminal remote reissue through AdapterCore native repair scenarios.",
                        existingPlayerRepairRuntime,
                        "ashfall.first_join", "ashfall.respawn", "ashfall.drop_pod", "recovery.repair")
                .preparedSurfaceService("ui_hud_screen_safe", "echoashfallprotocol:first_ten_minutes_briefing", "screen_safe_onboarding",
                        "Publishes the first-ten-minutes checklist, welcome screen, HUD mission line, Terminal handoff, Wiki guide, Lens, HoloMap, and Codex links as AdapterCore commands.",
                        uiHudHandoff,
                        "hud.mission_tracker", "hud.hazard_readout", "screen.welcome", "wiki.guide", "terminal.card")
                .preparedSurfaceService("ui_hud_screen_safe", "echoashfallprotocol:first_join_ui_hud_host_call_queue", "ui_hud_host_call_queue",
                        "Prepares HUD mission tracker, hazard readout, welcome screen, Terminal, Wiki, Lens, HoloMap, and Codex host calls; the surface queue remains unconsumed until live host evidence arrives.",
                        childMap(uiHudHandoff, "uiHudHostCallQueue"),
                        "adaptercore.surface_host_calls", "hud.mission_tracker", "hud.hazard_readout", "screen.welcome", "terminal.card", "wiki.guide")
                .preparedSurfaceService("weather_sound_atmosphere", "echoashfallprotocol:route_hazard_context", "opening_route_atmosphere",
                        "Publishes opening route weather risk, ash-storm feedback, ambience, atmosphere visibility, particles, sky-fog, and HUD hazard readout as AdapterCore commands.",
                        routeHazardHandoff,
                        "weather.route_hazards", "sound.weather_cues", "sound.ambience", "atmosphere.visibility")
                .preparedSurfaceService("weather_sound_atmosphere", "echoashfallprotocol:route_hazard_atmosphere_host_call_queue", "weather_sound_atmosphere_host_call_queue",
                        "Prepares WeatherCore route hazard, SoundCore ambience, AtmosphereCore visibility, and HUD hazard weather host calls; the surface queue remains unconsumed until live host evidence arrives.",
                        childMap(routeHazardHandoff, "atmosphereHostCallQueue"),
                        "adaptercore.surface_host_calls", "weather.route_hazards", "sound.weather_cues", "atmosphere.visibility", "hud.hazard_readout")
                .preparedSurfaceService("adaptercore_runtime_packets", "echoashfallprotocol:agent3_runtime_packet_bindings", "runtime_packet_bindings",
                        "Binds Agent 3 AdapterCore runtime packets to player/recovery, Recovery, HoloMap, UI/HUD, Lens, Wiki, Codex, WeatherCore, SoundCore, and AtmosphereCore consumers.",
                        runtimePacketBindings,
                        "adaptercore.runtime_packets", "ashfall.first_join", "ashfall.recovery", "ashfall.ui_hud", "ashfall.weather_audio")
                .preparedSurfaceService("adaptercore_gameplay_lifecycle", "echoashfallprotocol:agent3_gameplay_lifecycle_host_plan", "gameplay_lifecycle_host_plan",
                        "Orders Agent 3 first-join, Recovery/HoloMap, UI/HUD, and weather/sound/atmosphere host invocations for native-loader lifecycle execution.",
                        gameplayLifecycleHostPlan,
                        "adaptercore.lifecycle", "ashfall.first_join", "ashfall.recovery", "ashfall.ui_hud", "ashfall.weather_audio")
                .preparedSurfaceService("adaptercore_gameplay_lifecycle", "echoashfallprotocol:agent3_gameplay_lifecycle_host_runtime", "gameplay_lifecycle_host_runtime",
                        "Produces an Agent 3 no-launch AdapterCore host-state snapshot for first-join gameplay parity without claiming live host mutation.",
                        gameplayLifecycleHostRuntime,
                        "adaptercore.lifecycle", "adaptercore.host_runtime", "ashfall.first_join", "ashfall.recovery", "ashfall.ui_hud", "ashfall.weather_audio")
                .preparedSurfaceService("adaptercore_gameplay_lifecycle", "echoashfallprotocol:agent3_first_join_parity_verifier", "first_join_parity_verifier",
                        "Verifies the no-launch AdapterCore host-state snapshot against Ashfall first-join legacy runtime gameplay parity requirements without counting it as live mutation.",
                        firstJoinParityVerifier,
                        "adaptercore.lifecycle", "adaptercore.host_runtime", "ashfall.first_join", "ashfall.recovery", "ashfall.ui_hud", "ashfall.weather_audio")
                .preparedSurfaceService("adaptercore_gameplay_lifecycle", "echoashfallprotocol:first_spawn_equivalence_harness", "first_spawn_equivalence_harness",
                        "Publishes no-launch new-player and returning-player first-spawn equivalence test cases for future live native host integration tests.",
                        firstSpawnEquivalenceHarness,
                        "adaptercore.lifecycle", "adaptercore.host_runtime", "ashfall.first_join", "ashfall.recovery")
                .preparedSurfaceService("adaptercore_gameplay_lifecycle", "echoashfallprotocol:first_spawn_native_runtime_binding", "first_spawn_native_runtime_binding",
                        "Validates first-spawn AdapterCore host-call source bindings for inventory, drop-pod structure, respawn, advancement, packet, HUD, and player state mutation; live mutation is claimed only from post-mutation evidence.",
                        firstSpawnRuntimeBinding,
                        "adaptercore.lifecycle", "adaptercore.host_runtime", "ashfall.first_join", "ashfall.drop_pod", "ashfall.respawn")
                .preparedSurfaceService("adaptercore_events", "echoashfallprotocol:early_event_native_runtime_binding", "early_event_native_runtime_binding",
                        "Validates early gameplay AdapterCore event source bindings for item obtain, consume, block placement, crafting, water markers, shelter sleep, and special-marker mutation.",
                        earlyEventRuntimeBinding,
                        "adaptercore.events", "ashfall.survival", "ashfall.missions", "ashfall.special_markers")
                .preparedSurfaceService("adaptercore_exploration", "echoashfallprotocol:exploration_native_runtime_binding", "exploration_native_runtime_binding",
                        "Validates exploration AdapterCore event source bindings for scanner use, POI discovery, cache opening, data-log recovery, faction actions, reputation updates, drone state, and perk unlocks.",
                        explorationRuntimeBinding,
                        "adaptercore.events", "ashfall.exploration", "ashfall.factions", "ashfall.drones", "ashfall.research")
                .preparedSurfaceService("adaptercore_hazards", "echoashfallprotocol:hazard_native_runtime_binding", "hazard_native_runtime_binding",
                        "Validates hazard AdapterCore event source bindings for radiation, mutation, treatment, med bay, cleanser, scrubber, lab, vault, and hazard route-check mutation.",
                        hazardRuntimeBinding,
                        "adaptercore.events", "ashfall.hazards", "ashfall.medical", "ashfall.midgame_routes", "ashfall.research")
                .preparedSurfaceService("adaptercore_late_runtime", "echoashfallprotocol:late_native_runtime_binding", "late_native_runtime_binding",
                        "Validates late-game AdapterCore event source bindings for boss defeat, relay activation, power node state, scout drone routes, Nexus state, Prime Relay resolution, ending choice, and post-Nexus persistence.",
                        lateRuntimeBinding,
                        "adaptercore.events", "ashfall.nexus", "ashfall.prime_relays", "ashfall.endgame", "ashfall.post_nexus")
                .preparedSurfaceService("adaptercore_hardening", "echoashfallprotocol:adaptercore_runtime_hardening", "adaptercore_runtime_hardening",
                        "Hardens AdapterCore runtime mutation against missing addons, unloaded chunks, invalid players, duplicate events, logical side drift, reloads, and save migration gaps.",
                        runtimeHardening,
                        "adaptercore.hardening", "adaptercore.events", "ashfall.native_runtime", "ashfall.migration")
                .preparedSurfaceService("adaptercore_api", "echoashfallprotocol:adaptercore_native_runtime_api_freeze", "native_runtime_api_freeze",
                        "Freezes the AdapterCore native runtime interfaces for inventory, world blocks/state, structures, block entities, capabilities, events, packets, HUD, player state, and save data.",
                        nativeRuntimeApiFreeze,
                        "adaptercore.api", "adaptercore.native_runtime", "adaptercore.release_candidate")
                .preparedSurfaceService("adaptercore_runtime_gap_audit", "echoashfallprotocol:adaptercore_native_runtime_gap_audit", "native_runtime_gap_audit",
                        "Audits temporary AdapterCore evidence queues and descriptor-only runtime bindings so unresolved native-state mutation gaps remain visible.",
                        nativeRuntimeGapAudit,
                        "adaptercore.audit", "adaptercore.host_calls", "ashfall.native_runtime");
        Map<String, Object> registryBridge = registry.describe();
        Map<String, Object> machinePowerBridge = AshfallNativeMachinePowerContracts.describe(safeContext);
        Map<String, Object> serviceBridge = services.describe();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "ashfall_native_contract_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", safeContext.getOrDefault("packId", "unknown"));
        result.put("ashfallProtocolNativeVisible", true);
        result.put("lifecycleBridge", lifecycle.describe());
        result.put("registryBridge", registryBridge);
        result.put("eventBridge", eventBridge);
        result.put("serviceBridge", serviceBridge);
        result.put("nativeGameplayBootstrap", gameplayBootstrap);
        result.put("firstMajorRouteBootstrap", majorRouteBootstrap);
        result.put("midgameRouteBootstrap", midgameRouteBootstrap);
        result.put("lateGameRouteBootstrap", lateGameRouteBootstrap);
        result.put("machinePowerBridge", machinePowerBridge);
        result.put("machinePowerRuntimeTarget", machinePowerRuntimeTarget);
        result.put("machinePowerResourceAudit", machinePowerResourceAudit);
        result.put("machineRuntimeBinding", machineRuntimeBinding);
        result.put("agent9NativeTechRuntime", agent9NativeTechRuntime);
        result.put("agent9TechModuleEntrypoints", agent9TechModuleEntrypoints);
        result.put("uiHudScreenSafeConsumers", uiHudConsumers);
        result.put("uiHudScreenSafeHandoff", uiHudHandoff);
        result.put("weatherSoundAtmosphereConsumers", weatherSoundAtmosphereConsumers);
        result.put("routeHazardAtmosphereHandoff", routeHazardHandoff);
        result.put("runtimePacketBindings", runtimePacketBindings);
        result.put("gameplayLifecycleHostPlan", gameplayLifecycleHostPlan);
        result.put("gameplayLifecycleHostRuntime", gameplayLifecycleHostRuntime);
        result.put("firstJoinParityVerifier", firstJoinParityVerifier);
        result.put("firstSpawnEquivalenceHarness", firstSpawnEquivalenceHarness);
        result.put("firstSpawnRuntimeBinding", firstSpawnRuntimeBinding);
        result.put("earlyEventRuntimeBinding", earlyEventRuntimeBinding);
        result.put("explorationRuntimeBinding", explorationRuntimeBinding);
        result.put("hazardRuntimeBinding", hazardRuntimeBinding);
        result.put("lateRuntimeBinding", lateRuntimeBinding);
        result.put("runtimeHardening", runtimeHardening);
        result.put("nativeRuntimeApiFreeze", nativeRuntimeApiFreeze);
        result.put("nativeRuntimeGapAudit", nativeRuntimeGapAudit);
        result.put("liveRuntimeMutationEvidence", liveRuntimeMutationEvidence);
        result.put("nativeGameplayProfiles", List.of(firstJoinProfile));
        result.put("firstJoinCrashRecoveryProfile", firstJoinProfile);
        result.put("firstJoinCrashRecoveryExecution", firstJoinExecution);
        result.put("minecraftRuntimeHostCallQueue", minecraftRuntimeHostCallQueue);
        result.put("minecraftRuntimeAdapterReadiness", minecraftRuntimeAdapterReadiness);
        result.put("firstJoinRecoveryHandoff", recoveryHandoff);
        result.put("existingPlayerRepairRuntime", existingPlayerRepairRuntime);
        result.put("logicalRegistrationCount", registryBridge.get("registrationCount"));
        result.put("eventHookCount", eventBridge.get("hookCount"));
        result.put("approvedNativeServiceCount", serviceBridge.get("approvedServiceCount"));
        result.put("preparedNativeServiceCount", serviceBridge.get("preparedServiceCount"));
        result.put("executedNativeServiceCount", serviceBridge.get("executedServiceCount"));
        result.put("gameplayHookEvidence", true);
        result.put("gameplayHookVerifiedCount", gameplayBootstrap.get("verifiedHookCount"));
        result.put("majorRouteHookVerifiedCount", majorRouteBootstrap.get("verifiedHookCount"));
        result.put("majorRouteAdapterCoreCommandStatus", majorRouteBootstrap.get("status"));
        result.put("midgameRouteAdapterCoreReplayStatus", midgameRouteBootstrap.get("status"));
        result.put("midgameRouteVerifiedMissionCount", midgameRouteBootstrap.get("verifiedMissionCount"));
        result.put("midgameRouteVerifiedObjectiveCount", midgameRouteBootstrap.get("verifiedObjectiveCount"));
        result.put("midgameRouteGrantedRewardCount", midgameRouteBootstrap.get("grantedRewardCount"));
        result.put("lateGameRouteAdapterCoreReplayStatus", lateGameRouteBootstrap.get("status"));
        result.put("lateGameRouteVerifiedMissionCount", lateGameRouteBootstrap.get("verifiedMissionCount"));
        result.put("lateGameRouteVerifiedObjectiveCount", lateGameRouteBootstrap.get("verifiedObjectiveCount"));
        result.put("lateGameRouteVerifiedEventCount", lateGameRouteBootstrap.get("verifiedEventCount"));
        result.put("lateGameRouteGrantedRewardCount", lateGameRouteBootstrap.get("grantedRewardCount"));
        result.put("nativeGameplayParityContractsPrepared", true);
        result.put("nativeGameplayParityContracts", false);
        result.put("adapterCoreRuntimeTargetPrepared", "PASS".equals(machinePowerRuntimeTarget.get("status")));
        result.put("adapterCoreRuntimeTargetExecuted", false);
        result.put("adapterCoreRuntimeTargetStatus", machinePowerRuntimeTarget.get("status"));
        result.put("machinePowerResourceAuditStatus", machinePowerResourceAudit.get("status"));
        result.put("machinePowerResourceAuditBlockCount", machinePowerResourceAudit.get("auditedBlockCount"));
        result.put("machineRuntimeBindingStatus", machineRuntimeBinding.get("status"));
        result.put("machineRuntimeImplementedOperationCount", machineRuntimeBinding.get("implementedOperationCount"));
        result.put("machineRuntimeDeclaredOperationCount", machineRuntimeBinding.get("declaredOperationCount"));
        result.put("agent9NativeTechRuntimeStatus", agent9NativeTechRuntime.get("status"));
        result.put("agent9TechModuleEntrypointsStatus", agent9TechModuleEntrypoints.get("status"));
        result.put("agent9TechModuleEntrypointCount", agent9TechModuleEntrypoints.get("moduleEntrypointCount"));
        result.put("machineContractCount", machinePowerBridge.get("machineContractCount"));
        result.put("powerContractCount", machinePowerBridge.get("powerContractCount"));
        result.put("logisticsContractCount", machinePowerBridge.get("logisticsContractCount"));
        result.put("registeredFeatureContracts", List.of(
                "ashfall.content",
                "ashfall.adaptercore_hardening",
                "ashfall.drones",
                "ashfall.exploration",
                "ashfall.factions",
                "ashfall.first_join_recovery",
                "ashfall.hazards",
                "ashfall.hud",
                "ashfall.index",
                "ashfall.late_game_routes",
                "ashfall.loading_screen",
                "ashfall.logistics",
                "ashfall.machine_power",
                "ashfall.main_menu",
                "ashfall.medical",
                "ashfall.midgame_routes",
                "ashfall.missions",
                "ashfall.major_routes",
                "ashfall.module_ui_surfaces",
                "ashfall.native_client_overlay",
                "ashfall.nexus",
                "ashfall.post_nexus",
                "ashfall.prime_relays",
                "ashfall.protocol",
                "ashfall.research",
                "ashfall.survival",
                "ashfall.weather_audio",
                "ashfall.world"
        ));
        result.put("requiresRegistryBridge", true);
        result.put("requiresLifecycleBridge", true);
        result.put("requiresEventBridge", true);
        result.put("requiresLivePlayerStateBridge", true);
        result.put("registryInjected", false);
        result.put("registryMutated", false);
        result.put("serviceBridgeStarted", serviceBridge.get("applied"));
        result.put("serviceCodeExecuted", serviceBridge.get("serviceCodeExecuted"));
        result.put("runtimePacketBindingStatus", runtimePacketBindings.get("status"));
        result.put("runtimePacketBindingCount", runtimePacketBindings.get("packetCount"));
        result.put("gameplayLifecycleHostPlanStatus", gameplayLifecycleHostPlan.get("status"));
        result.put("gameplayLifecycleHostPlanInvocationCount", gameplayLifecycleHostPlan.get("readyInvocationCount"));
        result.put("gameplayLifecycleHostRuntimeStatus", gameplayLifecycleHostRuntime.get("status"));
        result.put("gameplayLifecycleHostRuntimeAppliedInvocationCount", gameplayLifecycleHostRuntime.get("appliedInvocationCount"));
        result.put("firstJoinParityVerifierStatus", firstJoinParityVerifier.get("status"));
        result.put("firstJoinParityRequirementCount", firstJoinParityVerifier.get("validatedRequirementCount"));
        result.put("firstSpawnEquivalenceStatus", firstSpawnEquivalenceHarness.get("status"));
        result.put("firstSpawnEquivalenceTestCaseCount", firstSpawnEquivalenceHarness.get("testCaseCount"));
        result.put("firstSpawnRuntimeBindingStatus", firstSpawnRuntimeBinding.get("status"));
        result.put("firstSpawnRuntimeImplementedOperationCount", firstSpawnRuntimeBinding.get("implementedOperationCount"));
        result.put("firstSpawnRuntimeDeclaredOperationCount", firstSpawnRuntimeBinding.get("declaredOperationCount"));
        result.put("earlyEventRuntimeBindingStatus", earlyEventRuntimeBinding.get("status"));
        result.put("earlyEventRuntimeImplementedEventCount", earlyEventRuntimeBinding.get("implementedEventCount"));
        result.put("earlyEventRuntimeDeclaredEventCount", earlyEventRuntimeBinding.get("declaredEventCount"));
        result.put("explorationRuntimeBindingStatus", explorationRuntimeBinding.get("status"));
        result.put("explorationRuntimeImplementedOperationCount", explorationRuntimeBinding.get("implementedOperationCount"));
        result.put("explorationRuntimeDeclaredOperationCount", explorationRuntimeBinding.get("declaredOperationCount"));
        result.put("hazardRuntimeBindingStatus", hazardRuntimeBinding.get("status"));
        result.put("hazardRuntimeImplementedOperationCount", hazardRuntimeBinding.get("implementedOperationCount"));
        result.put("hazardRuntimeDeclaredOperationCount", hazardRuntimeBinding.get("declaredOperationCount"));
        result.put("lateRuntimeBindingStatus", lateRuntimeBinding.get("status"));
        result.put("lateRuntimeImplementedOperationCount", lateRuntimeBinding.get("implementedOperationCount"));
        result.put("lateRuntimeDeclaredOperationCount", lateRuntimeBinding.get("declaredOperationCount"));
        result.put("runtimeHardeningStatus", runtimeHardening.get("status"));
        result.put("runtimeHardeningHandledFailureModeCount", runtimeHardening.get("handledFailureModeCount"));
        result.put("minecraftRuntimeAdapterReadinessStatus", minecraftRuntimeAdapterReadiness.get("status"));
        result.put("minecraftRuntimeAdapterReadinessCount", minecraftRuntimeAdapterReadiness.get("readyAdapterCount"));
        result.put("minecraftRuntimeHostCallQueuePreparedStatus", minecraftRuntimeHostCallQueue.get("status"));
        result.put("minecraftRuntimeHostCallQueueStatus", liveHostMutationRan
                ? "CONSUMED_BY_LIVE_HOST_MUTATION"
                : "NOT_CONSUMED_NO_HOST_MUTATION");
        result.put("minecraftRuntimeHostCallQueueConsumed", liveHostMutationRan);
        result.put("minecraftRuntimeHostCallQueueCount", minecraftRuntimeHostCallQueue.get("queuedHostCallCount"));
        result.put("recoveryNavigationHostInvocationStatus",
                childMap(recoveryHandoff, "recoveryNavigationHostInvocationContract").get("status"));
        result.put("recoveryNavigationHostInvocationCount",
                childMap(recoveryHandoff, "recoveryNavigationHostInvocationContract").get("readyInvocationCount"));
        result.put("recoveryNavigationHostCallQueueStatus", childMap(recoveryHandoff, "recoveryNavigationHostCallQueue").get("status"));
        result.put("recoveryNavigationHostCallQueueCount", childMap(recoveryHandoff, "recoveryNavigationHostCallQueue").get("queuedHostCallCount"));
        result.put("existingPlayerRepairRuntimeStatus", existingPlayerRepairRuntime.get("status"));
        result.put("existingPlayerRepairScenarioCount", existingPlayerRepairRuntime.get("scenarioCount"));
        result.put("uiHudHostInvocationStatus", childMap(uiHudHandoff, "uiHudHostInvocationContract").get("status"));
        result.put("uiHudHostInvocationCount", childMap(uiHudHandoff, "uiHudHostInvocationContract").get("readyInvocationCount"));
        result.put("uiHudHostCallQueueStatus", childMap(uiHudHandoff, "uiHudHostCallQueue").get("status"));
        result.put("uiHudHostCallQueueCount", childMap(uiHudHandoff, "uiHudHostCallQueue").get("queuedHostCallCount"));
        result.put("atmosphereHostInvocationStatus", childMap(routeHazardHandoff, "atmosphereHostInvocationContract").get("status"));
        result.put("atmosphereHostInvocationCount", childMap(routeHazardHandoff, "atmosphereHostInvocationContract").get("readyInvocationCount"));
        result.put("atmosphereHostCallQueueStatus", childMap(routeHazardHandoff, "atmosphereHostCallQueue").get("status"));
        result.put("atmosphereHostCallQueueCount", childMap(routeHazardHandoff, "atmosphereHostCallQueue").get("queuedHostCallCount"));
        result.put("firstJoinAdapterCoreCommandQueuePrepared", "PASS".equals(firstJoinExecution.get("status")));
        result.put("firstJoinAdapterCoreCommandsExecuted", liveHostMutationRan);
        result.put("firstJoinAdapterCorePreparedCommandCount", firstJoinExecution.get("preparedCommandCount"));
        result.put("firstJoinAdapterCoreExecutedCommandCount", firstJoinExecution.get("executedCommandCount"));
        result.put("firstJoinAdapterCoreCommandCount", firstJoinExecution.get("preparedCommandCount"));
        result.put("liveRuntimeMutationEvidenceStatus", liveRuntimeMutationEvidence.get("status"));
        result.put("liveRuntimeMutationCount", liveRuntimeMutationEvidence.get("mutationCount"));
        result.put("liveRuntimeDispatchedActionCount", liveRuntimeMutationEvidence.get("dispatchedActionCount"));
        result.put("liveRuntimeSaveUpdated", liveRuntimeMutationEvidence.get("saveUpdated"));
        result.put("liveRuntimeVisibleFeedbackEmitted", liveRuntimeMutationEvidence.get("visibleFeedbackEmitted"));
        result.put("nativeRuntimeApiFreezeStatus", nativeRuntimeApiFreeze.get("status"));
        result.put("nativeRuntimeGapAuditStatus", nativeRuntimeGapAudit.get("runtimeGapStatus"));
        result.put("nativeRuntimeQueuedGapCount", nativeRuntimeGapAudit.get("queuedGapCount"));
        result.put("nativeRuntimeP0GapCount", nativeRuntimeGapAudit.get("p0GapCount"));
        result.put("minecraftRuntimeAccessed", liveHostMutationRan);
        result.put("minecraftRuntimeMutated", liveHostMutationRan);
        result.put("minecraftWiringClaimed", liveHostMutationRan);
        result.put("unsafeRuntimeWorkStarted", false);
        result.put("transformsPerformed", false);
        result.put("summary", liveHostMutationRan
                ? "Ashfall Protocol native activation reports post-mutation evidence from a live AdapterCore-dispatched legacy runtime host call: runtime host resolved, NativeResult returned, save/HUD facts recorded, and the mutation ledger captured the committed state change."
                : "Ashfall Protocol native activation registered logical descriptors and prepared AdapterCore call surfaces, but it does not claim Minecraft wiring because no live runtime host mutation has been recorded during activation.");
        return result;
    }

    private static void registerSourceBackedAshfallBlocks(EchoNativeRegistryBridge registry) {
        for (String blockId : List.of(
                "ash_bush",
                "ash_campfire",
                "ash_layer",
                "atmospheric_scrubber",
                "burnt_fern",
                "burnt_grass",
                "burnt_tall_grass",
                "charred_wood_log",
                "concrete_chunk",
                "dead_wood_log",
                "debris_block",
                "deep_ash",
                "drop_pod_glass",
                "drop_pod_hull",
                "dry_grass",
                "dry_tall_grass",
                "emergency_bunk",
                "field_med_bay",
                "hand_recycler",
                "irradiated_cactus",
                "mutated_leaves_gray",
                "mutated_leaves_purple",
                "mutated_sapling",
                "nexus_capacitor",
                "nexus_core",
                "nuclear_fungus",
                "nuclear_grass",
                "nuclear_tall_grass",
                "power_node",
                "radiation_block",
                "rain_collector",
                "relay_station",
                "rubble",
                "rusted_metal_debris",
                "rusty_wheat",
                "scattered_bones",
                "signal_scanner",
                "structure_cache",
                "supply_crate",
                "thermal_array",
                "toxic_grass",
                "toxic_moss",
                "toxic_puddle",
                "toxic_tall_grass",
                "wasteland_grass",
                "wasteland_reed",
                "wasteland_tall_grass",
                "water_purifier",
                "bio_processing_station",
                "survey_table",
                "spore_garden",
                "trade_counter",
                "weapon_rack",
                "wild_berry_bush",
                "workshop_block"
        )) {
            registry.register(
                    "block",
                    MODULE_ID + ":" + blockId,
                    "Ashfall source-backed block native declaration mirrored from ModBlocks.",
                    sourceBackedAshfallBlockProperties(blockId)
            );
        }
    }

    private static Map<String, Object> sourceBackedAshfallBlockProperties(String blockId) {
        return registryProperties(
                "com.knoxhack.echoashfallprotocol.registry.ModBlocks",
                blockId.toUpperCase(java.util.Locale.ROOT)
        );
    }

    private static Map<String, Object> creativeTabProperties(
            String titleKey,
            String iconItem,
            List<String> itemIds
    ) {
        boolean registryBacked = itemIds != null
                && !itemIds.isEmpty()
                && itemIds.equals(registeredNativeModuleCreativeTabItems());
        return Map.of(
                "titleKey", titleKey,
                "iconItem", iconItem,
                "orderAnchor", "minecraft:building_blocks",
                "orderStrategy", "with_tabs_before_anchor",
                "searchVisibility", "parent_and_search_tabs",
                "searchVisible", true,
                "itemIds", itemIds == null ? List.of() : List.copyOf(itemIds),
                "nativeCreativeTabItemsDeclared", itemIds != null && !itemIds.isEmpty(),
                "nativeCreativeTabRegistryBacked", registryBacked,
                "nativeCreativeTabPopulationMode", registryBacked
                        ? "registered_native_module_item_ids"
                        : "declared_native_registry_host_item_ids"
        );
    }

    private static Map<String, Object> registryProperties(String ownerClass, String ownerMember) {
        return Map.of(
                "sourceBacked", true,
                "sourceOwnerClass", ownerClass,
                "sourceOwnerMember", ownerMember,
                "nativeRegistryMutationFirstClass", true
        );
    }

    private static Map<String, Object> nativeModulesCreativeTabProperties(
            String titleKey,
            String iconItem,
            List<String> itemIds
    ) {
        List<String> registryBackedItemIds = registeredNativeModuleCreativeTabRegistryBackedItems();
        List<String> featuredItemIds = registeredNativeModuleCreativeTabFeaturedItems();
        List<String> sourceNamespaces = registeredNativeModuleCreativeTabNamespaces();
        boolean sourceResolved = itemIds != null
                && !itemIds.isEmpty()
                && !registryBackedItemIds.isEmpty()
                && !featuredItemIds.isEmpty();
        Map<String, Object> properties = new LinkedHashMap<>(creativeTabProperties(titleKey, iconItem, itemIds));
        properties.put("surfaceIds", nativeModulesCreativeTabSurfaceIds());
        properties.put("nativeCreativeTabSurfaceIdsDeclared", true);
        properties.put("registryBackedItemIds", registryBackedItemIds);
        properties.put("featuredItemIds", featuredItemIds);
        properties.put("sourceNamespaces", sourceNamespaces);
        properties.put("nativeCreativeTabSourceBacked", sourceResolved);
        properties.put("nativeCreativeTabSourceResolvedFromRuntime", sourceResolved);
        properties.put("nativeCreativeTabFallbackPopulationUsed", !sourceResolved);
        properties.put("nativeCreativeTabFallbackOnlyEvidence", !sourceResolved);
        properties.put("releaseCreativeTabSourceTrusted", sourceResolved);
        properties.put("nativeCreativeTabPopulationMode", sourceResolved
                ? "registered_native_module_item_ids"
                : "fallback_native_module_item_ids_pre_minecraft");
        properties.put("nativeCreativeTabPopulationOwnerClass",
                "com.knoxhack.echoashfallprotocol.registry.ModCreativeTabs");
        properties.put("nativeCreativeTabPopulationOwnerMember", "nativeLoaderRegistryBackedCreativeItemIds");
        properties.put("nativeCreativeTabFullPopulationOwnerMember", "nativeModuleCreativeItemIds");
        properties.put("nativeCreativeTabFeaturedOwnerMember", "nativeModuleCreativeFeaturedItemIds");
        properties.put("nativeCreativeTabNamespaceOwnerMember", "nativeModuleCreativeNamespaces");
        return Map.copyOf(properties);
    }

    private static List<String> ashfallCreativeTabItems() {
        return List.of(
                "echoashfallprotocol:scrap_metal",
                "echoashfallprotocol:scrap_wire",
                "echoashfallprotocol:scrap_circuit",
                "echoashfallprotocol:scrap_plastic",
                "echoashfallprotocol:ash",
                "echoashfallprotocol:circuit_board",
                "echoashfallprotocol:energy_cell",
                "echoashfallprotocol:basic_battery",
                "echoashfallprotocol:portable_signal_scanner",
                "echoashfallprotocol:field_manual",
                "echoashfallprotocol:gas_mask",
                "echoashfallprotocol:filter_cartridge_basic",
                "echoashfallprotocol:hand_recycler",
                "echoashfallprotocol:water_purifier",
                "echoashfallprotocol:micro_generator",
                "echoashfallprotocol:scrap_press",
                "echoashfallprotocol:factory_controller"
        );
    }

    private static List<String> nativeModulesCreativeTabItems() {
        List<String> registeredItems = registeredNativeModuleCreativeTabItems();
        if (!registeredItems.isEmpty()) {
            return registeredItems;
        }
        return fallbackNativeModulesCreativeTabItems();
    }

    private static List<String> registeredNativeModuleCreativeTabItems() {
        return ModCreativeTabs.nativeModuleCreativeItemIds();
    }

    private static List<String> registeredNativeModuleCreativeTabRegistryBackedItems() {
        return ModCreativeTabs.nativeLoaderRegistryBackedCreativeItemIds();
    }

    private static List<String> registeredNativeModuleCreativeTabFeaturedItems() {
        return ModCreativeTabs.nativeModuleCreativeFeaturedItemIds();
    }

    private static List<String> registeredNativeModuleCreativeTabNamespaces() {
        return ModCreativeTabs.nativeModuleCreativeNamespaces();
    }

    private static List<String> fallbackNativeModulesCreativeTabItems() {
        return List.of(
                "echoashfallprotocol:field_manual",
                "echoashfallprotocol:portable_signal_scanner",
                "echoashfallprotocol:gas_mask",
                "echoashfallprotocol:filter_cartridge_basic",
                "echoashfallprotocol:basic_battery",
                "echoashfallprotocol:energy_cell",
                "echoashfallprotocol:hand_recycler",
                "echoashfallprotocol:water_purifier",
                "echoashfallprotocol:micro_generator",
                "echoashfallprotocol:signal_scanner",
                "echoashfallprotocol:scrap_press",
                "echoashfallprotocol:factory_controller",
                "echoashfallprotocol:relay_scanner_lens",
                "echoashfallprotocol:survey_table",
                "echoashfallprotocol:nexus_crystal",
                "echoterminal:echo_terminal",
                "echoterminal:echo_terminal_remote"
        );
    }

    private static List<String> fallbackNativeModulesCreativeFeaturedItems() {
        return List.of(
                "echoashfallprotocol:field_manual",
                "echoashfallprotocol:portable_signal_scanner",
                "echoashfallprotocol:gas_mask",
                "echoashfallprotocol:filter_cartridge_basic",
                "echoashfallprotocol:basic_battery",
                "echoashfallprotocol:energy_cell",
                "echoashfallprotocol:hand_recycler",
                "echoashfallprotocol:water_purifier",
                "echoashfallprotocol:micro_generator",
                "echoashfallprotocol:signal_scanner",
                "echoashfallprotocol:scrap_press",
                "echoashfallprotocol:factory_controller",
                "echoashfallprotocol:relay_scanner_lens",
                "echoashfallprotocol:survey_table",
                "echoashfallprotocol:nexus_crystal"
        );
    }

    private static List<String> nativeModulesCreativeTabSurfaceIds() {
        return List.of(
                "echoashfallprotocol:terminal_eui_handoff",
                "echoashfallprotocol:index_handoff",
                "echoashfallprotocol:lens_handoff",
                "echoashfallprotocol:holomap_minimap_handoff",
                "echoashfallprotocol:holomap_fullscreen_handoff",
                "echoashfallprotocol:echo_native_main_menu",
                "echoashfallprotocol:echo_native_loading",
                "echoashfallprotocol:ashfall_survival_hud",
                "echoashfallprotocol:ashfall_status_overlay"
        );
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> childMap(Map<String, Object> parent, String key) {
        if (parent != null && parent.get(key) instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private static final String MODULE_ID = "echoashfallprotocol";
}
