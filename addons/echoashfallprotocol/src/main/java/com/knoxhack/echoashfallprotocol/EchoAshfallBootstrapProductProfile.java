package com.knoxhack.echoashfallprotocol;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeBlockActionRule;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeBlockConstructorBinding;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeBlockFallbackRule;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeBridgePolicy;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeActionKeyPathHints;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeEntityDefinition;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeFieldActionRoute;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeInfoModuleStaticFieldArgumentInvocation;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeInfoModuleStaticInvocation;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeInfoModuleStaticFieldValue;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeInfoModuleStaticValueInvocation;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeIntegrationHook;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeItemActionRule;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeItemConstructorBinding;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeMachineOperationRules;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeMachineScenarioRule;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeModuleActionRoute;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeModulePathActionRoute;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeOutputRule;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeOutputRules;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativePathValueRule;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativePathValueRules;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativePhysicalActionRoute;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeRewardGrant;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeRewardRule;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeSourceBackedContentMapping;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeSourceContractFile;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeUiActionRoute;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeUiSurfaceRoute;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeWorldPaintPlacement;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeWorldPaintRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class EchoAshfallBootstrapProductProfile implements EchoNativeBootstrapProductProfile {
    private static final String NAMESPACE = "echoashfallprotocol";

    @Override
    public String namespace() {
        return NAMESPACE;
    }

    @Override
    public String nativeLoaderMainLabel() {
        return "ECHO Native Loader";
    }

    @Override
    public String nativeLoaderClientLabel() {
        return "ECHO Native Loader - Ashfall";
    }

    @Override
    public String nativeLoaderSessionMessage() {
        return "[ECHO Native Loader] Ashfall beta session active. This is not legacy runtime.";
    }

    @Override
    public String nativeLoaderWindowTitle() {
        return "ECHO Native Loader - Ashfall Beta";
    }

    @Override
    public String nativeLoaderAdapterCoreServiceId() {
        return "adaptercore.native_loader.backend";
    }

    @Override
    public String nativeLoaderRuntimeHostClass() {
        return "com.knoxhack.echoashfallprotocol.event.NativeLoaderEchoRuntimeHost";
    }

    @Override
    public String nativeMinecraftRuntimeHostClass() {
        return "com.knoxhack.echoashfallprotocol.event.NativeMinecraftEchoRuntimeHost";
    }

    @Override
    public String nativeMinecraftRuntimeHostId() {
        return NAMESPACE + ":native_minecraft_runtime_host";
    }

    @Override
    public String nativeLoaderBackendClass() {
        return "dev.echo.nativeplatform.loader.NativeLoaderAdapterCoreBackend";
    }

    @Override
    public String nativeLoaderRuntimeLane() {
        return "Native Loader";
    }

    @Override
    public String nativeUiActionCommand() {
        return "native.ui.ashfall_drone_command";
    }

    @Override
    public String nativeGameplayBridgeKey() {
        return "ashfallGameplayBridge";
    }

    @Override
    public List<String> nativeLegacyGameplayBridgeKeys() {
        return List.of("nativeProductGameplayBridge");
    }

    @Override
    public String nativeGameplayBridgeId() {
        return "adaptercore.native_ashfall_gameplay_content";
    }

    @Override
    public String nativeGameplayPackId() {
        return "ashfall";
    }

    @Override
    public String nativeGameplayDisplayName() {
        return "Ashfall";
    }

    @Override
    public String nativeGameplayHandlerClassName() {
        return "AshfallAdapterCoreGameplayHandlers";
    }

    @Override
    public String nativePlayableRuntimeKey() {
        return "ashfallPlayableBetaRuntime";
    }

    @Override
    public String nativePlayableHudLedgerTarget() {
        return "ashfall.playable_beta.hud";
    }

    @Override
    public String nativeIndexSearchQuery() {
        return "ashfall";
    }

    @Override
    public String nativeLensFallbackTarget() {
        return id("portable_signal_scanner");
    }

    @Override
    public String nativeCompatibilityDelegateClass() {
        return "com.knoxhack.echoashfallprotocol.event.legacy runtimeEchoRuntimeHost";
    }

    @Override
    public String nativeCompatibilityDelegateId() {
        return id("legacy_runtime_runtime_host");
    }

    @Override
    public NativeBridgePolicy nativeInitialEventBridgePolicy() {
        return new NativeBridgePolicy(
                true,
                false,
                "descriptor_event_services",
                "Ashfall native event bridge waits for registered event services before executing gameplay handlers.",
                Map.of(
                        "handlerSubscribed", false,
                        "runtimeEventPublished", false
                )
        );
    }

    @Override
    public NativeBridgePolicy nativeInitialServiceBridgePolicy() {
        return new NativeBridgePolicy(
                true,
                false,
                "ashfall_native_service_registry",
                "Ashfall native service bridge waits for runtime registry and event bridge attachment before executing service code.",
                Map.of("serviceRegistryInitialized", false)
        );
    }

    @Override
    public NativeBridgePolicy nativeClientUiBridgePolicy() {
        return new NativeBridgePolicy(
                true,
                false,
                "real_echo_module_hotkey_bridge",
                "AdapterCore will install a native client UI host and route Terminal, Index, Lens, HoloMap, and Wiki through their module bridges when available.",
                Map.of(
                        "clientUiHostAttached", false,
                        "terminalFallbackReady", false,
                        "indexFallbackReady", false,
                        "lensFallbackReady", false,
                        "hudFallbackReady", false,
                        "customMainMenuReady", false
                )
        );
    }

    @Override
    public List<String> requiredGameplayHandlerEvents() {
        return List.of(
                "player_join",
                "client_tick",
                "world_tick",
                "item_use",
                "block_place",
                "block_break",
                "entity_interact",
                "screen_open",
                "command_execution",
                "save_load",
                "resource_reload"
        );
    }

    @Override
    public List<String> requiredAgent7WorldLiveHooks() {
        return List.of(
                "echoworldcore:player_tick.post",
                "echoweathercore:level_tick.post",
                "echoatmospherecore:level_tick.post",
                "echobiomecore:level_tick.post",
                "echostructurecore:level_tick.post",
                "echospawncore:finalize_spawn",
                "echodifficultycore:server_starting",
                "echostatuscore:server_starting"
        );
    }

    @Override
    public List<String> requiredLiveMutationSurfaces() {
        return List.of(
                "inventory",
                "world_blocks",
                "save_data",
                "hud"
        );
    }

    @Override
    public List<NativeEntityDefinition> nativeEntities() {
        return List.of(
                entity("rad_zombie", "com.knoxhack.echoashfallprotocol.entity.RadZombie", "net.minecraft.world.entity.monster.zombie.Zombie", "MONSTER", 0.6F, 1.95F, 64, false),
                entity("scavenger_bandit", "com.knoxhack.echoashfallprotocol.entity.ScavengerBandit", "net.minecraft.world.entity.monster.zombie.Zombie", "MONSTER", 0.6F, 1.95F, 64, false),
                entity("irradiated_wolf", "com.knoxhack.echoashfallprotocol.entity.IrradiatedWolf", "net.minecraft.world.entity.animal.wolf.Wolf", "MONSTER", 0.6F, 0.85F, 64, false),
                entity("echo_drone", "com.knoxhack.echoashfallprotocol.entity.EchoDrone", "net.minecraft.world.entity.monster.Vex", "MONSTER", 0.5F, 0.5F, 64, false),
                entity("scout_drone", "com.knoxhack.echoashfallprotocol.entity.ScoutDrone", "net.minecraft.world.entity.monster.Vex", "MISC", 0.5F, 0.5F, 64, true),
                entity("echo_companion_drone", "com.knoxhack.echoashfallprotocol.entity.EchoCompanionDrone", "net.minecraft.world.entity.monster.Vex", "MISC", 0.5F, 0.5F, 64, true),
                entity("glowing_ghoul", "com.knoxhack.echoashfallprotocol.entity.GlowingGhoul", "net.minecraft.world.entity.monster.zombie.Zombie", "MONSTER", 0.6F, 1.95F, 64, true),
                entity("ash_wraith", "com.knoxhack.echoashfallprotocol.entity.AshWraith", "net.minecraft.world.entity.monster.Vex", "MONSTER", 0.6F, 1.9F, 64, false),
                entity("toxic_slime", "com.knoxhack.echoashfallprotocol.entity.ToxicSlime", "net.minecraft.world.entity.monster.Slime", "MONSTER", 0.8F, 0.8F, 64, false),
                entity("city_stalker", "com.knoxhack.echoashfallprotocol.entity.CityStalker", "net.minecraft.world.entity.monster.zombie.Zombie", "MONSTER", 0.6F, 1.8F, 64, false),
                entity("rust_walker", "com.knoxhack.echoashfallprotocol.entity.RustWalker", "net.minecraft.world.entity.monster.zombie.Zombie", "MONSTER", 1.0F, 2.2F, 64, false),
                entity("steam_wraith", "com.knoxhack.echoashfallprotocol.entity.SteamWraith", "net.minecraft.world.entity.monster.Vex", "MONSTER", 0.6F, 1.9F, 64, false),
                entity("mutated_crawler", "com.knoxhack.echoashfallprotocol.entity.MutatedCrawler", "net.minecraft.world.entity.monster.zombie.Zombie", "MONSTER", 0.5F, 0.7F, 64, false),
                entity("wild_dog", "com.knoxhack.echoashfallprotocol.entity.WildDog", "net.minecraft.world.entity.animal.wolf.Wolf", "MONSTER", 0.65F, 0.9F, 64, false),
                entity("feral_human", "com.knoxhack.echoashfallprotocol.entity.FeralHuman", "net.minecraft.world.entity.monster.zombie.Zombie", "MONSTER", 0.6F, 1.95F, 64, false),
                entity("crash_survivor", "com.knoxhack.echoashfallprotocol.entity.CrashSurvivor", "net.minecraft.world.entity.npc.villager.Villager", "CREATURE", 0.6F, 1.95F, 64, false),
                entity("faction_npc", "com.knoxhack.echoashfallprotocol.entity.faction.FactionNpcEntity", "net.minecraft.world.entity.npc.villager.Villager", "CREATURE", 0.6F, 1.95F, 64, false),
                entity("gridbound_husk", "com.knoxhack.echoashfallprotocol.entity.NexusPressureMobEntity", "net.minecraft.world.entity.monster.zombie.Zombie", "MONSTER", 0.6F, 1.95F, 80, false),
                entity("relay_warden", "com.knoxhack.echoashfallprotocol.entity.NexusPressureMobEntity", "net.minecraft.world.entity.monster.zombie.Zombie", "MONSTER", 0.9F, 2.4F, 88, true),
                entity("signal_leech", "com.knoxhack.echoashfallprotocol.entity.NexusPressureMobEntity", "net.minecraft.world.entity.monster.Vex", "MONSTER", 0.55F, 1.45F, 80, false),
                entity("nexus_nullifier", "com.knoxhack.echoashfallprotocol.entity.NexusPressureMobEntity", "net.minecraft.world.entity.monster.zombie.Zombie", "MONSTER", 0.8F, 2.3F, 96, true),
                entity("warden_boss", "com.knoxhack.echoashfallprotocol.entity.boss.WardenBossEntity", "net.minecraft.world.entity.monster.zombie.Zombie", "MONSTER", 1.5F, 3.0F, 128, true),
                entity("wasteland_sentinel", "com.knoxhack.echoashfallprotocol.entity.boss.BiomeBossEntity", "net.minecraft.world.entity.monster.zombie.Zombie", "MONSTER", 1.0F, 2.5F, 96, false),
                entity("crash_zone_colossus", "com.knoxhack.echoashfallprotocol.entity.boss.BiomeBossEntity", "net.minecraft.world.entity.monster.zombie.Zombie", "MONSTER", 1.2F, 2.8F, 96, true),
                entity("cryogenic_overseer", "com.knoxhack.echoashfallprotocol.entity.boss.BiomeBossEntity", "net.minecraft.world.entity.monster.zombie.Zombie", "MONSTER", 1.0F, 2.6F, 96, true),
                entity("industrial_juggernaut", "com.knoxhack.echoashfallprotocol.entity.boss.BiomeBossEntity", "net.minecraft.world.entity.monster.zombie.Zombie", "MONSTER", 1.3F, 2.9F, 96, true),
                entity("nexus_scar_avatar", "com.knoxhack.echoashfallprotocol.entity.boss.BiomeBossEntity", "net.minecraft.world.entity.monster.zombie.Zombie", "MONSTER", 1.1F, 2.8F, 112, true),
                entity("radiation_behemoth", "com.knoxhack.echoashfallprotocol.entity.boss.BiomeBossEntity", "net.minecraft.world.entity.monster.zombie.Zombie", "MONSTER", 1.3F, 3.0F, 96, true),
                entity("city_ruin_stalker", "com.knoxhack.echoashfallprotocol.entity.boss.BiomeBossEntity", "net.minecraft.world.entity.monster.zombie.Zombie", "MONSTER", 0.9F, 2.4F, 96, false),
                entity("plains_warlord", "com.knoxhack.echoashfallprotocol.entity.boss.BiomeBossEntity", "net.minecraft.world.entity.monster.zombie.Zombie", "MONSTER", 1.0F, 2.5F, 96, false),
                entity("toxic_hive_matriarch", "com.knoxhack.echoashfallprotocol.entity.boss.BiomeBossEntity", "net.minecraft.world.entity.monster.Slime", "MONSTER", 1.2F, 2.5F, 96, false),
                entity("corruption_bloom", "com.knoxhack.echoashfallprotocol.entity.boss.NexusFinalBossEntity", "net.minecraft.world.entity.monster.zombie.Zombie", "MONSTER", 1.35F, 2.9F, 128, true),
                entity("severance_engine", "com.knoxhack.echoashfallprotocol.entity.boss.NexusFinalBossEntity", "net.minecraft.world.entity.monster.zombie.Zombie", "MONSTER", 1.45F, 3.0F, 128, true),
                entity("mirror_command", "com.knoxhack.echoashfallprotocol.entity.boss.NexusFinalBossEntity", "net.minecraft.world.entity.monster.zombie.Zombie", "MONSTER", 1.25F, 2.85F, 128, true)
        );
    }

    @Override
    public List<NativeSourceBackedContentMapping> nativeSourceBackedItemMappings() {
        return List.of(
                item("basic_battery", "battery", "BatteryItem"),
                item("advanced_battery", "battery", "BatteryItem"),
                item("elite_battery", "battery", "BatteryItem"),
                item("filter_cartridge_basic", "filter", "FilterCartridgeItem"),
                item("filter_cartridge_advanced", "filter", "FilterCartridgeItem"),
                item("filter_cartridge_elite", "filter", "FilterCartridgeItem"),
                item("dirty_water_bottle", "water", "DirtyWaterItem"),
                item("filtered_water_bottle", "water", "FilteredWaterItem"),
                item("boiled_water_bottle", "water", "BoiledWaterItem"),
                item("clean_water_bottle", "water", "CleanWaterItem"),
                item("mutagen_vial", "medical_hazard", "MutagenItem"),
                item("rad_away", "medical_hazard", "RadAwayItem"),
                item("crude_filter", "water", "CrudeFilterItem"),
                item("portable_signal_scanner", "scanner_lens", "SignalScannerItem"),
                item("scrap_knife", "weapon", "ScrapKnifeItem"),
                item("gas_mask", "hazard_gear", "GasMaskItem"),
                item("schematic_fragment_weapons", "research_progression", "SchematicFragmentItem"),
                item("schematic_fragment_armor", "research_progression", "SchematicFragmentItem"),
                item("schematic_fragment_machines", "research_progression", "SchematicFragmentItem"),
                item("schematic_fragment_medical", "research_progression", "SchematicFragmentItem"),
                item("schematic_fragment_energy", "research_progression", "SchematicFragmentItem"),
                item("hazmat_helmet", "hazard_gear", "HazmatArmorItem"),
                item("hazmat_chestplate", "hazard_gear", "HazmatArmorItem"),
                item("hazmat_leggings", "hazard_gear", "HazmatArmorItem"),
                item("hazmat_boots", "hazard_gear", "HazmatArmorItem"),
                item("contaminated_iron", "hazard_resource", "ContaminatedItem"),
                item("contaminated_gold", "hazard_resource", "ContaminatedItem"),
                item("contaminated_redstone", "hazard_resource", "ContaminatedItem"),
                item("contaminated_lapis", "hazard_resource", "ContaminatedItem"),
                item("alloy_blade", "weapon", "AlloyBladeItem"),
                item("alloy_hammer", "weapon", "AlloyHammerItem"),
                item("nexus_blade", "weapon", "NexusBladeItem"),
                item("nexus_annihilator", "weapon", "NexusAnnihilatorItem"),
                item("nexus_crystal", "endgame_progression", "NexusCrystalItem"),
                item("prefall_archives_key", "endgame_progression", "PrefallArchivesKeyItem"),
                item("return_keystone", "recovery_fast_travel", "ReturnKeystoneItem"),
                item("instability_dampener", "hazard_gear", "InstabilityDampenerItem"),
                item("relay_scanner_lens", "scanner_lens", "RelayScannerLensItem"),
                item("return_beacon", "recovery_fast_travel", "ReturnBeaconItem"),
                item("scout_drone_item", "drone", "ScoutDroneItem"),
                item("rare_tech_schematic", "research_progression", "RareTechSchematicItem"),
                item("bone_knife", "weapon", "BoneKnifeItem"),
                item("crude_spear", "weapon", "CrudeSpearItem"),
                item("hide_wrap", "survival_gear", "HideWrapItem"),
                item("bandage", "medical_hazard", "BandageItem"),
                item("stim_pack", "medical_hazard", "StimPackItem"),
                item("hand_warmer", "survival_gear", "HandWarmerItem")
        );
    }

    @Override
    public List<NativeSourceBackedContentMapping> nativeSourceBackedBlockMappings() {
        return List.of(
                block("emergency_bunk", "survival_starting_pod", "EmergencyBunkBlock"),
                block("weapon_rack", "npc_profession_station", "ProfessionBlock"),
                block("trade_counter", "npc_profession_station", "ProfessionBlock"),
                block("map_table", "holomap_navigation", "ProfessionBlock"),
                block("bio_processing_station", "machine_station", "ProfessionBlock"),
                block("spore_garden", "machine_station", "ProfessionBlock"),
                block("acidic_sludge", "hazard_terrain", "AcidicSludgeBlock"),
                block("acid_mud", "hazard_terrain", "AcidicSludgeBlock"),
                block("dry_grass", "hazard_vegetation", "HazardousBushBlock"),
                block("mutated_bush", "hazard_vegetation", "HazardousBushBlock"),
                block("thorn_scrub", "hazard_vegetation", "HazardousBushBlock"),
                block("dry_tall_grass", "hazard_vegetation", "HazardousDoublePlantBlock"),
                block("wasteland_tall_grass", "hazard_vegetation", "HazardousDoublePlantBlock"),
                block("toxic_tall_grass", "hazard_vegetation", "HazardousDoublePlantBlock"),
                block("nuclear_tall_grass", "hazard_vegetation", "HazardousDoublePlantBlock"),
                block("burnt_tall_grass", "hazard_vegetation", "HazardousDoublePlantBlock"),
                block("wasteland_grass", "biome_tinted_vegetation", "BiomeTintedGrassBlock"),
                block("toxic_grass", "biome_tinted_vegetation", "BiomeTintedGrassBlock"),
                block("nuclear_grass", "biome_tinted_vegetation", "BiomeTintedGrassBlock"),
                block("burnt_grass", "biome_tinted_vegetation", "BiomeTintedGrassBlock"),
                block("mutated_leaves_purple", "mutated_foliage", "MutatedLeavesBlock"),
                block("mutated_leaves_gray", "mutated_foliage", "MutatedLeavesBlock"),
                block("dead_wood_log", "terrain_log", "net.minecraft.world.level.block.RotatedPillarBlock"),
                block("charred_wood_log", "terrain_log", "net.minecraft.world.level.block.RotatedPillarBlock"),
                block("ash_layer", "terrain_layer", "net.minecraft.world.level.block.SnowLayerBlock")
        );
    }

    @Override
    public List<NativeSourceContractFile> nativeRegistrySourceContractFiles() {
        return List.of(
                sourceFile("items", "addons/echoashfallprotocol/src/main/java/com/knoxhack/echoashfallprotocol/registry/ModItems.java"),
                sourceFile("blocks", "addons/echoashfallprotocol/src/main/java/com/knoxhack/echoashfallprotocol/registry/ModBlocks.java"),
                sourceFile("item_classes", "addons/echoashfallprotocol/src/main/java/com/knoxhack/echoashfallprotocol/item"),
                sourceFile("block_classes", "addons/echoashfallprotocol/src/main/java/com/knoxhack/echoashfallprotocol/block")
        );
    }

    @Override
    public List<NativeItemConstructorBinding> nativeItemConstructorBindings() {
        return List.of(
                itemConstructor("basic_battery", "BatteryItem", "com.knoxhack.echoashfallprotocol.item.BatteryItem$Tier", "BASIC"),
                itemConstructor("advanced_battery", "BatteryItem", "com.knoxhack.echoashfallprotocol.item.BatteryItem$Tier", "ADVANCED"),
                itemConstructor("elite_battery", "BatteryItem", "com.knoxhack.echoashfallprotocol.item.BatteryItem$Tier", "ELITE"),
                itemConstructor("filter_cartridge_basic", "FilterCartridgeItem", "com.knoxhack.echoashfallprotocol.item.FilterCartridgeItem$Tier", "BASIC"),
                itemConstructor("filter_cartridge_advanced", "FilterCartridgeItem", "com.knoxhack.echoashfallprotocol.item.FilterCartridgeItem$Tier", "ADVANCED"),
                itemConstructor("filter_cartridge_elite", "FilterCartridgeItem", "com.knoxhack.echoashfallprotocol.item.FilterCartridgeItem$Tier", "ELITE"),
                itemConstructor("schematic_fragment_weapons", "SchematicFragmentItem", "com.knoxhack.echoashfallprotocol.item.SchematicFragmentItem$SchematicType", "WEAPONS"),
                itemConstructor("schematic_fragment_armor", "SchematicFragmentItem", "com.knoxhack.echoashfallprotocol.item.SchematicFragmentItem$SchematicType", "ARMOR"),
                itemConstructor("schematic_fragment_machines", "SchematicFragmentItem", "com.knoxhack.echoashfallprotocol.item.SchematicFragmentItem$SchematicType", "MACHINES"),
                itemConstructor("schematic_fragment_medical", "SchematicFragmentItem", "com.knoxhack.echoashfallprotocol.item.SchematicFragmentItem$SchematicType", "MEDICAL"),
                itemConstructor("schematic_fragment_energy", "SchematicFragmentItem", "com.knoxhack.echoashfallprotocol.item.SchematicFragmentItem$SchematicType", "ENERGY"),
                itemConstructor("hazmat_helmet", "HazmatArmorItem", "runtime:world.entity.EquipmentSlot", "HEAD"),
                itemConstructor("hazmat_chestplate", "HazmatArmorItem", "runtime:world.entity.EquipmentSlot", "CHEST"),
                itemConstructor("hazmat_leggings", "HazmatArmorItem", "runtime:world.entity.EquipmentSlot", "LEGS"),
                itemConstructor("hazmat_boots", "HazmatArmorItem", "runtime:world.entity.EquipmentSlot", "FEET"),
                itemConstructor("dirty_water_bottle", "DirtyWaterItem"),
                itemConstructor("filtered_water_bottle", "FilteredWaterItem"),
                itemConstructor("boiled_water_bottle", "BoiledWaterItem"),
                itemConstructor("clean_water_bottle", "CleanWaterItem"),
                itemConstructor("mutagen_vial", "MutagenItem"),
                itemConstructor("rad_away", "RadAwayItem"),
                itemConstructor("crude_filter", "CrudeFilterItem"),
                itemConstructor("portable_signal_scanner", "SignalScannerItem"),
                itemConstructor("scrap_knife", "ScrapKnifeItem"),
                itemConstructor("gas_mask", "GasMaskItem"),
                itemConstructor("contaminated_iron", "ContaminatedItem"),
                itemConstructor("contaminated_gold", "ContaminatedItem"),
                itemConstructor("contaminated_redstone", "ContaminatedItem"),
                itemConstructor("contaminated_lapis", "ContaminatedItem"),
                itemConstructor("alloy_blade", "AlloyBladeItem"),
                itemConstructor("alloy_hammer", "AlloyHammerItem"),
                itemConstructor("nexus_blade", "NexusBladeItem"),
                itemConstructor("nexus_annihilator", "NexusAnnihilatorItem"),
                itemConstructor("nexus_crystal", "NexusCrystalItem"),
                itemConstructor("prefall_archives_key", "PrefallArchivesKeyItem"),
                itemConstructor("return_keystone", "ReturnKeystoneItem"),
                itemConstructor("instability_dampener", "InstabilityDampenerItem"),
                itemConstructor("relay_scanner_lens", "RelayScannerLensItem"),
                itemConstructor("return_beacon", "ReturnBeaconItem"),
                itemConstructor("scout_drone_item", "ScoutDroneItem"),
                itemConstructor("rare_tech_schematic", "RareTechSchematicItem"),
                itemConstructor("bone_knife", "BoneKnifeItem"),
                itemConstructor("crude_spear", "CrudeSpearItem"),
                itemConstructor("hide_wrap", "HideWrapItem"),
                itemConstructor("bandage", "BandageItem"),
                itemConstructor("stim_pack", "StimPackItem"),
                itemConstructor("hand_warmer", "HandWarmerItem")
        );
    }

    @Override
    public List<NativeBlockConstructorBinding> nativeBlockConstructorBindings() {
        return List.of(
                blockConstructor("acidic_sludge", "AcidicSludgeBlock"),
                blockConstructor("acid_mud", "AcidicSludgeBlock"),
                blockConstructor("dry_grass", "HazardousBushBlock"),
                blockConstructor("mutated_bush", "HazardousBushBlock"),
                blockConstructor("thorn_scrub", "HazardousBushBlock"),
                blockConstructor("dry_tall_grass", "HazardousDoublePlantBlock"),
                blockConstructor("wasteland_tall_grass", "HazardousDoublePlantBlock"),
                blockConstructor("toxic_tall_grass", "HazardousDoublePlantBlock"),
                blockConstructor("nuclear_tall_grass", "HazardousDoublePlantBlock"),
                blockConstructor("burnt_tall_grass", "HazardousDoublePlantBlock"),
                blockConstructor("wasteland_grass", "BiomeTintedGrassBlock"),
                blockConstructor("toxic_grass", "BiomeTintedGrassBlock"),
                blockConstructor("nuclear_grass", "BiomeTintedGrassBlock"),
                blockConstructor("burnt_grass", "BiomeTintedGrassBlock"),
                blockConstructor("mutated_leaves_purple", "MutatedLeavesBlock"),
                blockConstructor("mutated_leaves_gray", "MutatedLeavesBlock"),
                blockConstructor("dead_wood_log", "runtime:world.level.block.RotatedPillarBlock"),
                blockConstructor("charred_wood_log", "runtime:world.level.block.RotatedPillarBlock"),
                blockConstructor("ash_layer", "runtime:world.level.block.SnowLayerBlock"),
                blockConstructor("weapon_rack", "ProfessionBlock"),
                blockConstructor("trade_counter", "ProfessionBlock"),
                blockConstructor("map_table", "ProfessionBlock"),
                blockConstructor("bio_processing_station", "ProfessionBlock"),
                blockConstructor("spore_garden", "ProfessionBlock"),
                blockConstructor("echo_cache", "EchoContainerBlock"),
                blockConstructor("echo_crate", "EchoContainerBlock"),
                blockConstructor("supply_crate", "EchoContainerBlock"),
                blockConstructor("emergency_bunk", "EmergencyBunkBlock"),
                powerCable("reinforced_power_cable", 2000, 256),
                powerCable("high_voltage_power_cable", 4000, 1024)
        );
    }

    @Override
    public Map<String, String> nativeModuleClassOverrides() {
        return Map.ofEntries(
                Map.entry("echoagriculturereclamation", "com.knoxhack.echoagriculturereclamation.EchoAgricultureReclamationNativeModule"),
                Map.entry("echoaetherworks", "com.knoxhack.echoaetherworks.EchoAetherWorksNativeModule"),
                Map.entry("echoarcanacore", "com.knoxhack.echoarcanacore.EchoArcanaCoreNativeModule"),
                Map.entry("echoarcaneindex", "com.knoxhack.echoarcaneindex.EchoArcaneIndexNativeModule"),
                Map.entry("echoarmory", "com.knoxhack.echoarmory.EchoArmoryNativeModule"),
                Map.entry("echoblackboxprotocol", "com.knoxhack.echoblackboxprotocol.EchoBlackboxProtocolNativeModule"),
                Map.entry("echoblockworks", "com.knoxhack.echoblockworks.EchoBlockworksNativeModule"),
                Map.entry("echoconvoyprotocol", "com.knoxhack.echoconvoyprotocol.EchoConvoyProtocolNativeModule"),
                Map.entry("echocursecore", "com.knoxhack.echocursecore.EchoCurseCoreNativeModule"),
                Map.entry("echodatacore", "com.knoxhack.echodatacore.EchoDataCoreNativeModule"),
                Map.entry("echofamiliarcore", "com.knoxhack.echofamiliarcore.EchoFamiliarCoreNativeModule"),
                Map.entry("echogrimoire", "com.knoxhack.echogrimoire.EchoGrimoireNativeModule"),
                Map.entry("echoholomap", "com.knoxhack.echoholomap.EchoHoloMapNativeModule"),
                Map.entry("echoindex", "com.knoxhack.echoindex.EchoIndexNativeModule"),
                Map.entry("echoindustrialnexus", "com.knoxhack.echoindustrialnexus.EchoIndustrialNexusNativeModule"),
                Map.entry("echolens", "com.knoxhack.echolens.EchoLensNativeModule"),
                Map.entry("echologisticsnetwork", "com.knoxhack.echologisticsnetwork.EchoLogisticsNetworkNativeModule"),
                Map.entry("echomissioncore", "com.knoxhack.echomissioncore.EchoMissionCoreNativeModule"),
                Map.entry("echomultiblockcore", "com.knoxhack.echomultiblockcore.EchoMultiblockCoreNativeModule"),
                Map.entry("echonexusprotocol", "com.knoxhack.echonexusprotocol.EchoNexusProtocolNativeModule"),
                Map.entry("echoorbitalremnants", "com.knoxhack.echoorbitalremnants.EchoOrbitalRemnantsNativeModule"),
                Map.entry("echopowergrid", "com.knoxhack.echopowergrid.EchoPowerGridNativeModule"),
                Map.entry("echorecovery", "com.knoxhack.echorecovery.EchoRecoveryNativeModule"),
                Map.entry("echorelictech", "com.knoxhack.echorelictech.EchoRelicTechNativeModule"),
                Map.entry("echoriftworlds", "com.knoxhack.echoriftworlds.EchoRiftWorldsNativeModule"),
                Map.entry("echoritualcore", "com.knoxhack.echoritualcore.EchoRitualCoreNativeModule"),
                Map.entry("echoscreencore", "com.knoxhack.echoscreencore.EchoScreenCoreNativeModule"),
                Map.entry("echospellcore", "com.knoxhack.echospellcore.EchoSpellCoreNativeModule"),
                Map.entry("echostationfall", "com.knoxhack.echostationfall.EchoStationfallNativeModule"),
                Map.entry("echoterminal", "com.knoxhack.echoterminal.EchoTerminalNativeModule"),
                Map.entry("echoweathercore", "com.knoxhack.echoweathercore.EchoWeatherCoreNativeModule"),
                Map.entry("echowiki", "com.knoxhack.echowiki.EchoWikiNativeModule"),
                Map.entry("signalos", "com.knoxhack.signalos.EchoSignalOsNativeModule"),
                Map.entry(NAMESPACE, "com.knoxhack.echoashfallprotocol.EchoAshfallNativeModule")
        );
    }

    @Override
    public List<String> nativeModuleNamespacePrefixes() {
        return List.of("echo", "signalos");
    }

    @Override
    public List<NativeModuleActionRoute> nativeModuleActionRoutes() {
        return List.of(
                new NativeModuleActionRoute(
                        "echoindustrialnexus",
                        "machine_rig",
                        List.of(
                                "echoindustrialnexus:component_assembler", "minecraft:copper_ingot",
                                "echoindustrialnexus:machine_casing", "minecraft:iron_ingot"
                        ),
                        List.of(
                                "echoindustrialnexus:component_assembler", "minecraft:copper_ingot",
                                "echoindustrialnexus:industrial_power_bus", "minecraft:redstone"
                        ),
                        "Industrial Nexus assembled a live machine rig and produced parts.",
                        "Industrial Nexus machine cycled scrap into components.",
                        "runDefaultScenario",
                        "",
                        true,
                        List.of(),
                        List.of(),
                        List.of()
                ),
                new NativeModuleActionRoute(
                        "echoblockworks",
                        "outpost_pad",
                        List.of(
                                "echoblockworks:ashstone_brick", "minecraft:stone_bricks",
                                "echoblockworks:charred_concrete_cracked", "minecraft:polished_deepslate"
                        ),
                        List.of(),
                        "Blockworks deployed a build pad instead of acting like a generic block.",
                        "Blockworks block expanded into a buildable field platform.",
                        "",
                        "",
                        false,
                        List.of(),
                        List.of(),
                        List.of(new NativeModulePathActionRoute(
                                List.of("terminal", "panel", "server", "monitor"),
                                "",
                                List.of("echoblockworks:broken_monitor", "minecraft:redstone"),
                                List.of("echoblockworks:broken_monitor", "minecraft:redstone_lamp"),
                                List.of(),
                                "Blockworks data panel powered its local signal display."
                        ))
                ),
                new NativeModuleActionRoute(
                        "echoweathercore",
                        "shelter",
                        List.of("echoweathercore:storm_scanner", "minecraft:compass"),
                        List.of(),
                        "WeatherCore built a small shelter node and warning kit.",
                        "WeatherCore block fired a storm warning and shelter countermeasure.",
                        "",
                        "",
                        false,
                        List.of(),
                        List.of("weather clear"),
                        List.of()
                ),
                new NativeModuleActionRoute(
                        "echoconvoyprotocol",
                        "convoy_pad",
                        List.of(
                                "echoconvoyprotocol:vehicle_service_pad", "minecraft:minecart",
                                "echoconvoyprotocol:convoy_route_chip", "minecraft:rail"
                        ),
                        List.of(
                                "echoconvoyprotocol:convoy_repair_kit", "minecraft:minecart",
                                "echoconvoyprotocol:vehicle_service_pad", "minecraft:rail"
                        ),
                        "Convoy Protocol staged a vehicle pad, rails, and route supplies.",
                        "Convoy block staged vehicle service, rails, and route logistics."
                ),
                new NativeModuleActionRoute(
                        "echoorbitalremnants",
                        "orbital_pad",
                        List.of(
                                "echoorbitalremnants:emergency_rocket", "minecraft:firework_rocket",
                                "echoorbitalremnants:emergency_oxygen_cell", "minecraft:glass_bottle"
                        ),
                        List.of(
                                "echoorbitalremnants:emergency_rocket", "minecraft:firework_rocket",
                                "echoorbitalremnants:emergency_oxygen_cell", "minecraft:glass_bottle"
                        ),
                        "Orbital Remnants calibrated a launch beacon and recovery payload.",
                        "Orbital block locked a launch beacon and signal relay."
                ),
                new NativeModuleActionRoute(
                        "echonexusprotocol",
                        "containment",
                        List.of(
                                "echonexusprotocol:memory_shard", "minecraft:echo_shard",
                                "echonexusprotocol:nexus_field_stabilizer", "minecraft:amethyst_shard"
                        ),
                        List.of(
                                "echonexusprotocol:memory_shard", "minecraft:echo_shard",
                                "echonexusprotocol:field_anchor", "minecraft:amethyst_shard"
                        ),
                        "Nexus Protocol stabilized a containment cell and memory shard.",
                        "Nexus block stabilized corruption containment."
                ),
                new NativeModuleActionRoute(
                        "echomultiblockcore",
                        "multiblock_frame",
                        List.of("echomultiblockcore:reinforced_machine_frame", "minecraft:iron_block"),
                        List.of("echomultiblockcore:data_bus", "minecraft:redstone"),
                        "Multiblock Core projected a controller frame with data and power buses.",
                        "Multiblock controller assembled a live frame footprint."
                ),
                new NativeModuleActionRoute(
                        "echoagriculturereclamation",
                        "greenhouse",
                        List.of(
                                "echoagriculturereclamation:recovered_seed_capsule", "minecraft:wheat_seeds",
                                "echoagriculturereclamation:soil_nutrient_mix", "minecraft:bone_meal"
                        ),
                        List.of(
                                "echoagriculturereclamation:bio_gel", "minecraft:wheat",
                                "echoagriculturereclamation:soil_nutrient_mix", "minecraft:bone_meal"
                        ),
                        "Agriculture Reclamation seeded a hydroponic patch and food loop.",
                        "Agriculture block reclaimed soil and produced biomass."
                ),
                new NativeModuleActionRoute(
                        "echoarmory",
                        "armory_station",
                        List.of(
                                "echoarmory:arcane_shield", "minecraft:shield",
                                "echoarmory:energy_rifle", "minecraft:iron_sword"
                        ),
                        List.of(
                                "echoarmory:arcane_shield", "minecraft:shield",
                                "echoarmory:armory_alloy_plate", "minecraft:iron_ingot"
                        ),
                        "Armory issued a field loadout and configured the weapon station.",
                        "Armory block configured weapon service and loadout routing."
                ),
                new NativeModuleActionRoute(
                        "echoblackboxprotocol",
                        "relic_seal",
                        List.of("echoblackboxprotocol:echo_blackbox_fragment", "minecraft:book"),
                        List.of("echoblackboxprotocol:echo_blackbox_fragment", "minecraft:book"),
                        "Blackbox Protocol extracted archive evidence from the vault route.",
                        "Blackbox block recovered archive data from the sealed vault."
                ),
                new NativeModuleActionRoute(
                        "echorelictech",
                        "relic_seal",
                        List.of("echorelictech:relic_shard", "minecraft:amethyst_shard"),
                        List.of("echorelictech:relic_shard", "minecraft:amethyst_shard"),
                        "RelicTech opened a containment seal and charged a relic component.",
                        "RelicTech block charged a containment artifact."
                ),
                new NativeModuleActionRoute(
                        "echopowergrid",
                        "power_circuit",
                        List.of(
                                "echopowergrid:power_cell", "minecraft:redstone",
                                "echopowergrid:low_voltage_cable", "minecraft:redstone"
                        ),
                        List.of(
                                "echopowergrid:power_cell", "minecraft:redstone",
                                "echopowergrid:low_voltage_cable", "minecraft:redstone"
                        ),
                        "PowerGrid energized a local circuit and supplied redstone.",
                        "PowerGrid block energized a visible local circuit."
                ),
                new NativeModuleActionRoute(
                        "echostationfall",
                        "station_node",
                        List.of(
                                "echostationfall:station_battery", "minecraft:clock",
                                "echostationfall:pressure_seal_kit", "minecraft:iron_ingot"
                        ),
                        List.of(
                                "echostationfall:station_battery", "minecraft:clock",
                                "echostationfall:station_access_card", "minecraft:iron_ingot"
                        ),
                        "Stationfall restored a station node and pressure-door route.",
                        "Stationfall block restored station power and control routing."
                ),
                new NativeModuleActionRoute(
                        "echospellcore",
                        "arcana_node",
                        List.of(
                                "echospellcore:signal_focus", "minecraft:amethyst_shard",
                                "echospellcore:aether_catalyst", "minecraft:ender_pearl"
                        ),
                        List.of(
                                "echospellcore:signal_focus", "minecraft:amethyst_shard",
                                "echospellcore:aether_catalyst", "minecraft:ender_pearl"
                        ),
                        "Spell Core resolved a field cast and placed a focus node.",
                        "Spell Core block resolved a field ritual effect."
                ),
                new NativeModuleActionRoute(
                        "echoritualcore",
                        "arcana_node",
                        List.of(
                                "echoritualcore:ritual_focus", "minecraft:amethyst_shard",
                                "echoritualcore:stability_seal", "minecraft:ender_pearl"
                        ),
                        List.of(
                                "echoritualcore:ritual_focus", "minecraft:amethyst_shard",
                                "echoritualcore:stability_seal", "minecraft:ender_pearl"
                        ),
                        "Ritual Core resolved a field ritual and placed a focus node.",
                        "Ritual Core block resolved a field ritual effect."
                ),
                new NativeModuleActionRoute(
                        "echologisticsnetwork",
                        "logistics_depot",
                        List.of(
                                "echologisticsnetwork:faction_trade_depot", "minecraft:chest",
                                "echologisticsnetwork:route_manifest", "minecraft:emerald"
                        ),
                        List.of(
                                "echologisticsnetwork:faction_trade_depot", "minecraft:chest",
                                "echologisticsnetwork:route_manifest", "minecraft:emerald"
                        ),
                        "Logistics Network built a depot and routed request supplies.",
                        "Logistics block routed depot storage and trade supplies."
                )
        );
    }

    @Override
    public List<NativeIntegrationHook> nativeIntegrationHooks() {
        return List.of(
                hook("core_services", "ashfallCoreServicesRegistered",
                        "com.knoxhack.echoashfallprotocol.integration.AshfallCoreServices", "register"),
                hook("companion_drone_data_key", "ashfallCompanionDroneDataKeyRegistered",
                        "com.knoxhack.echoashfallprotocol.entity.drone.CompanionDroneStateStore", "registerDataKey"),
                hook("drone_map_provider", "ashfallDroneMapProviderRegistered",
                        "com.knoxhack.echoashfallprotocol.entity.drone.DroneScanService", "registerMapProvider"),
                hook("world_core_builtins", "ashfallWorldCoreRegionsRegistered",
                        "com.knoxhack.echoashfallprotocol.integration.AshfallWorldCoreBuiltins", "register"),
                hook("mission_core_integration", "ashfallMissionCoreRegistered",
                        "com.knoxhack.echoashfallprotocol.integration.AshfallMissionCoreIntegration", "registerWhenReady"),
                hook("index_provider", "ashfallIndexProviderRegistered",
                        "com.knoxhack.echoashfallprotocol.integration.AshfallIndexProvider", "register"),
                hook("lens_integration", "ashfallDroneLensIntegrationRegistered",
                        "com.knoxhack.echoashfallprotocol.integration.AshfallDroneLensIntegration", "register"),
                hook("render_static_surfaces", "ashfallRenderCoreStaticSurfacesRegistered",
                        "com.knoxhack.echoashfallprotocol.integration.AshfallRenderCoreClientIntegration", "registerStaticSurfaces")
        );
    }

    @Override
    public Map<String, List<String>> nativeModuleServiceClasses() {
        return Map.ofEntries(
                Map.entry("echoagriculturereclamation", List.of(
                        "com.knoxhack.echoagriculturereclamation.integration.ReclamationTerminalCommonIntegration",
                        "com.knoxhack.echoagriculturereclamation.integration.ReclamationLensIntegration",
                        "com.knoxhack.echoagriculturereclamation.integration.ReclamationLogisticsIntegration"
                )),
                Map.entry("echoarcanacore", List.of(
                        "com.knoxhack.echoarcanacore.integration.ArcanaCoreMissionIntegration",
                        "com.knoxhack.echoarcanacore.integration.veilbound.ArcanaVeilboundLensIntegration"
                )),
                Map.entry("echospellcore", List.of(
                        "com.knoxhack.echospellcore.integration.terminal.SpellCoreTerminalIntegration",
                        "com.knoxhack.echospellcore.integration.lens.SpellCoreLensIntegration",
                        "com.knoxhack.echospellcore.integration.missioncore.SpellCoreMissionCoreIntegration"
                )),
                Map.entry("echorelictech", List.of(
                        "com.knoxhack.echorelictech.integration.terminal.RelicTechTerminalCommonIntegration",
                        "com.knoxhack.echorelictech.integration.lens.RelicTechLensIntegration",
                        "com.knoxhack.echorelictech.integration.holomap.RelicTechHoloMapIntegration",
                        "com.knoxhack.echorelictech.integration.missioncore.RelicTechMissionCoreIntegration",
                        "com.knoxhack.echorelictech.integration.worldcore.RelicTechWorldCoreIntegration"
                )),
                Map.entry("echosoundcore", List.of(
                        "com.knoxhack.echosoundcore.integration.terminal.SoundCoreTerminalIntegration",
                        "com.knoxhack.echosoundcore.integration.lens.SoundCoreLensIntegration",
                        "com.knoxhack.echosoundcore.integration.holomap.SoundCoreHoloMapIntegration",
                        "com.knoxhack.echosoundcore.integration.worldcore.SoundCoreWorldCoreIntegration"
                )),
                Map.entry("echonpcore", List.of(
                        "com.knoxhack.echo.npcore.integration.terminal.NpcTerminalIntegration"
                )),
                Map.entry("echoterminal", List.of(
                        "com.knoxhack.echoterminal.BuiltinTerminalCommonIntegration",
                        "com.knoxhack.echoterminal.client.BuiltinTerminalTabs"
                )),
                Map.entry("echoindex", List.of(
                        "com.knoxhack.echoindex.integration.IndexTerminalCommonIntegration",
                        "com.knoxhack.echoindex.integration.IndexMissionCoreIntegration"
                )),
                Map.entry("echolens", List.of(
                        "com.knoxhack.echolens.provider.LensBuiltins",
                        "com.knoxhack.echolens.integration.LensCoreIntegration"
                )),
                Map.entry("echoholomap", List.of(
                        "com.knoxhack.echoholomap.integration.HoloMapTerminalCommonIntegration",
                        "com.knoxhack.echoholomap.integration.HoloMapMissionCoreIntegration",
                        "com.knoxhack.echoholomap.integration.HoloMapIndexIntegration"
                )),
                Map.entry("echowiki", List.of(
                        "com.knoxhack.echowiki.content.WikiContentRegistry",
                        "com.knoxhack.echowiki.integration.WikiTerminalClientIntegration"
                )),
                Map.entry("signalos", List.of(
                        "com.knoxhack.signalos.content.SignalOsBuiltinContent",
                        "com.knoxhack.signalos.service.SignalOsBuiltinActions"
                ))
        );
    }

    @Override
    public String nativeAdapterCoreMachineRuntimeClass() {
        return "com.knoxhack.echoashfallprotocol.AshfallAdapterCoreMachinePowerRuntime";
    }

    @Override
    public List<String> nativeRuntimeHostFactoryClasses() {
        return List.of(
                "com.knoxhack.echoashfallprotocol.event.NativeLoaderRuntimeHostFactory",
                "com.knoxhack.echoashfallprotocol.event.legacy runtimeRuntimeHostFactory"
        );
    }

    @Override
    public Map<String, String> nativeAdapterCoreGameplayClasses() {
        return Map.ofEntries(
                Map.entry("poi_scanner_service", "com.knoxhack.echoashfallprotocol.world.POIScannerService"),
                Map.entry("poi_scan_hit", "com.knoxhack.echoashfallprotocol.world.POIScannerService$ScanHit"),
                Map.entry("field_ops_contract_handler", "com.knoxhack.echoashfallprotocol.event.FieldOpsContractHandler"),
                Map.entry("faction_events", "com.knoxhack.echoashfallprotocol.faction.FactionEvents"),
                Map.entry("early_event_runtime", "com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreEarlyEventRuntime"),
                Map.entry("hazard_runtime", "com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreHazardRuntime"),
                Map.entry("exploration_runtime", "com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreExplorationRuntime"),
                Map.entry("late_runtime", "com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreLateRuntime"),
                Map.entry("machine_runtime_host", "com.knoxhack.echoashfallprotocol.nativebridge.AshfallAdapterCoreMachineRuntimeHost"),
                Map.entry("mission_trigger_runtime", "com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreMissionTriggerRuntime"),
                Map.entry("campaign_data", "com.knoxhack.echoashfallprotocol.world.NexusCampaignData"),
                Map.entry("world_state_data", "com.knoxhack.echoashfallprotocol.world.NexusWorldData")
        );
    }

    @Override
    public List<String> requiredNativeServiceSurfaces() {
        return List.of(
                "player_recovery",
                "holomap_lens_codex_wiki",
                "weather_sound_atmosphere",
                "screen_safe_ui"
        );
    }

    @Override
    public Map<String, List<String>> nativeServiceSurfaceModules() {
        return Map.ofEntries(
                Map.entry("echorecovery", List.of("player_recovery")),
                Map.entry("echoplayercore", List.of("player_recovery")),
                Map.entry("echoholomap", List.of("holomap_lens_codex_wiki")),
                Map.entry("echolens", List.of("holomap_lens_codex_wiki")),
                Map.entry("echowiki", List.of("holomap_lens_codex_wiki")),
                Map.entry("echocodexcore", List.of("holomap_lens_codex_wiki")),
                Map.entry("echoguidecore", List.of("holomap_lens_codex_wiki")),
                Map.entry("echoweathercore", List.of("weather_sound_atmosphere")),
                Map.entry("echosoundcore", List.of("weather_sound_atmosphere")),
                Map.entry("echoatmospherecore", List.of("weather_sound_atmosphere")),
                Map.entry("echoscreencore", List.of("screen_safe_ui")),
                Map.entry("echohudcore", List.of("screen_safe_ui"))
        );
    }

    @Override
    public Map<String, String> nativeGameplayContentDataPrefixes() {
        return Map.of(
                "missions", "data/echoashfallprotocol/missioncore/missions/",
                "world_regions", "data/echoashfallprotocol/echoworldcore/world_regions/",
                "progression_advancements", "data/echoashfallprotocol/advancement/",
                "hazard_biome_tags", "data/echoashfallprotocol/tags/worldgen/biome/"
        );
    }

    @Override
    public String nativeResourcePackDescription() {
        return "ECHO Native Ashfall runtime resources";
    }

    @Override
    public String nativeSaveDatapackDescription() {
        return "ECHO Native Ashfall worldgen";
    }

    @Override
    public String nativeSaveDatapackFileName() {
        return "echo-native-ashfall-datapack.zip";
    }

    @Override
    public String nativeSourceResourceRootMarker() {
        return "data/echoashfallprotocol/dimension/wasteland_overworld.json";
    }

    @Override
    public List<String> nativeRequiredResourceEntries() {
        return List.of(
                "data/echoashfallprotocol/dimension/wasteland_overworld.json",
                "data/echoashfallprotocol/dimension/prefall_archives.json",
                "data/echoashfallprotocol/dimension_type/prefall_archives.json",
                "data/echoashfallprotocol/worldgen/noise_settings/wasteland_overworld.json",
                "data/echoashfallprotocol/worldgen/world_preset/ashfall_wasteland.json",
                nativeMachineRecipeCatalogPath(),
                "data/minecraft/worldgen/world_preset/normal.json"
        );
    }

    @Override
    public List<String> nativeSaveDatapackEntryPrefixes() {
        return List.of(
                "data/echoashfallprotocol/worldgen/",
                "data/echoashfallprotocol/tags/worldgen/",
                "data/echoashfallprotocol/structure/",
                "data/echoashfallprotocol/structures/",
                "data/echoashfallprotocol/dimension/",
                "data/echoashfallprotocol/dimension_type/",
                "data/echoashfallprotocol/tags/block/",
                "data/minecraft/worldgen/",
                "data/minecraft/tags/worldgen/",
                "data/minecraft/tags/block/"
        );
    }

    @Override
    public Map<String, String> nativeSaveDatapackRequiredEntriesByValidationKey() {
        return Map.of(
                "prefallDimensionTypePresent",
                "data/echoashfallprotocol/dimension_type/prefall_archives.json",
                "overworldNoiseSettingsPresent",
                "data/echoashfallprotocol/worldgen/noise_settings/wasteland_overworld.json"
        );
    }

    @Override
    public List<String> nativeModuleResourceSourcePathMarkers() {
        return List.of(
                "/addons/echoashfallprotocol/",
                "/build/tmp/echo-native-m17-mods/"
        );
    }

    @Override
    public String nativeStructureTemplateSourcePrefix() {
        return "data/echoashfallprotocol/structure/";
    }

    @Override
    public String nativeStructureTemplateTargetPrefix() {
        return "data/echoashfallprotocol/structures/";
    }

    @Override
    public String nativeWorldgenStructurePrefix() {
        return "data/echoashfallprotocol/worldgen/structure/";
    }

    @Override
    public String nativeWorldgenBiomePrefix() {
        return "data/echoashfallprotocol/worldgen/biome/";
    }

    @Override
    public String nativeWorldPresetMirrorSource() {
        return "data/minecraft/worldgen/world_preset/normal.json";
    }

    @Override
    public String nativeWorldPresetMirrorTarget() {
        return "data/echoashfallprotocol/worldgen/world_preset/ashfall_wasteland.json";
    }

    @Override
    public String nativeMachineRecipeCatalogPath() {
        return "data/echoashfallprotocol/adaptercore/native_machine_recipes.properties";
    }

    @Override
    public String nativeMachineRecipeCatalogSourcePath() {
        return "addons/echoashfallprotocol/src/main/java/com/knoxhack/echoashfallprotocol/AshfallNativeMachineRecipeCatalog.java";
    }

    @Override
    public String nativeRecipeBehaviorContractSourcePath() {
        return "addons/echoashfallprotocol/src/main/java/com/knoxhack/echoashfallprotocol/compat/jei/EchoJeiRecipeCatalog.java";
    }

    @Override
    public List<String> nativeMachineRecipeCatalogTypes() {
        return List.of(
                "echoashfallprotocol:hand_recycler",
                "echoashfallprotocol:water_purifier",
                "echoashfallprotocol:water_collection",
                "echoashfallprotocol:thermal_burner",
                "echoashfallprotocol:micro_generator",
                "echoashfallprotocol:filter_workbench",
                "echoashfallprotocol:scrap_press",
                "echoashfallprotocol:ore_grinder",
                "echoashfallprotocol:isotope_refiner",
                "echoashfallprotocol:radiation_cleanser",
                "echoashfallprotocol:crystalline_synthesizer",
                "echoashfallprotocol:deep_core_miner"
        );
    }

    @Override
    public List<NativeMachineScenarioRule> nativeMachineScenarioRules() {
        return List.of(
                new NativeMachineScenarioRule(List.of("water_purifier", "purifier", "filter", "water"), "runWaterPurifierScenario"),
                new NativeMachineScenarioRule(List.of("scrap_dynamo", "dynamo"), "runScrapDynamoScenario"),
                new NativeMachineScenarioRule(List.of("battery", "capacitor", "power_cell"), "runBatteryBankBalancingScenario"),
                new NativeMachineScenarioRule(List.of("cable", "power_node"), "runCableTierScenario"),
                new NativeMachineScenarioRule(List.of("thermal", "burner", "generator", "reactor"), "runThermalBurnerScenario"),
                new NativeMachineScenarioRule(List.of("autofeed"), "runAutofeedHopperScenario"),
                new NativeMachineScenarioRule(List.of("contaminant"), "runContaminantCondenserScenario"),
                new NativeMachineScenarioRule(List.of("isotope"), "runIsotopeRefinerScenario"),
                new NativeMachineScenarioRule(List.of("radiation_cleanser", "cleanser"), "runRadiationCleanserScenario"),
                new NativeMachineScenarioRule(List.of("crystalline", "synthesizer"), "runCrystallineSynthesizerScenario"),
                new NativeMachineScenarioRule(List.of("deep_core", "miner", "drill"), "runDeepCoreMinerScenario"),
                new NativeMachineScenarioRule(List.of("load_distributor", "router"), "runPriorityRoutingScenario"),
                new NativeMachineScenarioRule(List.of("factory", "controller"), "runFactoryControllerToggleScenario"),
                new NativeMachineScenarioRule(List.of("item_pipe", "pipe", "hopper", "logistics"), "runLogisticsRoutingScenario"),
                new NativeMachineScenarioRule(List.of("jam", "repair"), "runJamRepairScenario"),
                new NativeMachineScenarioRule(List.of("wear", "degrade"), "runWearThresholdScenario"),
                new NativeMachineScenarioRule(List.of("backpressure", "output"), "runOutputBackpressureScenario"),
                new NativeMachineScenarioRule(List.of("recipe", "refiner", "recycler", "grinder", "press", "lab", "workshop"), "runRecipeCatalogScenario")
        );
    }

    @Override
    public List<NativeWorldPaintRecipe> nativeWorldPaintRecipes() {
        return List.of(
                recipe("signal_trace", signalTracePaint()),
                recipe("outpost_pad", outpostPadPaint()),
                recipe("wasteland_patch", wastelandPatchPaint()),
                recipe("machine_rig", machineRigPaint()),
                recipe("power_circuit", powerCircuitPaint()),
                recipe("convoy_pad", convoyPadPaint()),
                recipe("orbital_pad", orbitalPadPaint()),
                recipe("greenhouse", greenhousePaint()),
                recipe("multiblock_frame", multiblockFramePaint()),
                recipe("containment", containmentPaint()),
                recipe("shelter", shelterPaint()),
                recipe("armory_station", armoryStationPaint()),
                recipe("relic_seal", relicSealPaint()),
                recipe("station_node", stationNodePaint()),
                recipe("arcana_node", arcanaNodePaint()),
                recipe("logistics_depot", logisticsDepotPaint())
        );
    }

    @Override
    public List<NativeFieldActionRoute> nativeItemFieldActionRoutes() {
        return List.of(
                field("signal", "signal_trace", "", false,
                        List.of(reward(id("relay_scanner_lens"), 1, id("ash"), 1)),
                        List.of(), "", false, List.of(), List.of(), "", "",
                        "{product} scanner marked cache, relay, hazard, and route trace in-world."),
                field("route", "", "", false,
                        List.of(reward(id("field_manual"), 1, "echoterminal:echo_terminal_remote", 1)),
                        List.of(), "", false,
                        List.of(
                                product(0, 0, 0, "map_table", id("relay_station")),
                                product(1, 0, 0, "relay_station", id("power_node"))
                        ),
                        List.of(), "", "",
                        "{product} map item placed a map table and relay waypoint."),
                field("survival", "", "runWaterPurifierScenario", false,
                        List.of(
                                reward(id("clean_water_bottle"), 1, id("field_manual"), 1),
                                reward("minecraft:bread", 1)
                        ),
                        List.of(), "", false,
                        List.of(product(0, 0, 0, "water_purifier", id("relay_station"))),
                        List.of(), "", "",
                        "{product} survival item produced clean-water and medical recovery supplies."),
                field("machine", "machine_rig", "", true,
                        List.of(
                                reward(id("scrap_metal"), 1, "minecraft:iron_ingot", 1),
                                reward("minecraft:redstone", 4)
                        ),
                        List.of(), "", false, List.of(), List.of(), "", "",
                        "{product} machine item converted salvage into usable parts."),
                field("power", "power_circuit", "", true,
                        List.of(
                                reward(id("power_cell"), 1, "minecraft:redstone", 1),
                                reward("minecraft:redstone", 6)
                        ),
                        List.of(), "", false, List.of(), List.of(), "", "",
                        "{product} power item energized a visible field circuit."),
                field("cache", "", "", false,
                        List.of(),
                        List.of(
                                reward(id("scrap_metal"), 1, "minecraft:iron_ingot", 1),
                                reward(id("ash"), 1, id("scrap_metal"), 1),
                                reward("minecraft:bread", 2)
                        ),
                        "loot", false, List.of(), List.of(), "", "",
                        "{product} cache item recovered salvage and route supplies."),
                field("entity", "", "", false,
                        List.of(reward("minecraft:bone", 1, "minecraft:redstone", 1)),
                        List.of(), "", false, List.of(), List.of(), "", "",
                        "{product} entity item armed a field companion route."),
                field("wasteland", "wasteland_patch", "", false,
                        List.of(reward(id("ash"), 1, id("scrap_metal"), 1)),
                        List.of(), "", false, List.of(), List.of(), "", "",
                        "{product} terrain item spread wasteland biome material around the sample point."),
                field("shelter", "shelter", "", false,
                        List.of(reward("minecraft:campfire", 1, "minecraft:torch", 1)),
                        List.of(), "", false, List.of(), List.of(), "", "",
                        "{product} survival item deployed shelter and heat support."),
                field("default", "outpost_pad", "", false,
                        List.of(reward(id("scrap_metal"), 1, id("ash"), 1)),
                        List.of(), "", false, List.of(), List.of(), "", "",
                        "{product} item executed its native field loop with distinct world support.")
        );
    }

    @Override
    public List<NativeFieldActionRoute> nativeBlockFieldActionRoutes() {
        return List.of(
                field("cache", "", "", false,
                        List.of(),
                        List.of(
                                reward(id("scrap_metal"), 1, "minecraft:iron_ingot", 1),
                                reward(id("ash"), 1, id("scrap_metal"), 1),
                                reward("minecraft:bread", 2)
                        ),
                        "loot", true,
                        List.of(product(0, 1, 0, "echo_cache", id("relay_station"))),
                        List.of(), "", "",
                        "{product} cache opened salvage, food, and route data."),
                field("signal", "", "", false,
                        List.of(reward(id("relay_scanner_lens"), 1, "minecraft:compass", 1)),
                        List.of(), "", false,
                        List.of(
                                product(0, 1, 0, "relay_station", id("power_node")),
                                product(1, 0, 0, "power_node", "minecraft:redstone_lamp")
                        ),
                        List.of(), "Relay route synced", "aqua",
                        "{product} relay repaired, powered, and synced the route network."),
                field("route", "", "", false,
                        List.of(reward(id("field_manual"), 1, "echoterminal:echo_terminal_remote", 1)),
                        List.of(), "", false,
                        List.of(product(0, 1, 0, "map_table", id("relay_station"))),
                        List.of(), "", "",
                        "{product} map table refreshed waypoint and recovery-route data."),
                field("survival", "", "runWaterPurifierScenario", false,
                        List.of(reward(id("clean_water_bottle"), 1, id("field_manual"), 1)),
                        List.of(), "", false,
                        List.of(product(0, 1, 0, "water_purifier", id("relay_station"))),
                        List.of("effect clear @s minecraft:poison"), "", "",
                        "{product} purifier cycled clean water and scrubbed poison."),
                field("machine", "machine_rig", "", true,
                        List.of(
                                reward("minecraft:iron_ingot", 1, "minecraft:copper_ingot", 1),
                                reward("minecraft:redstone", 3)
                        ),
                        List.of(), "", false, List.of(), List.of(), "", "",
                        "{product} machine processed scrap, research, and parts."),
                field("power", "power_circuit", "", true,
                        List.of(), List.of(), "", false, List.of(),
                        List.of("effect give @s minecraft:haste 60 0 true"), "", "",
                        "{product} power block energized the local grid and work speed."),
                field("agriculture", "greenhouse", "", false,
                        List.of(reward("minecraft:bone_meal", 1, "minecraft:wheat", 1)),
                        List.of(), "", false, List.of(), List.of(), "", "",
                        "{product} plant block reclaimed soil and generated biomass."),
                field("armory", "armory_station", "", false,
                        List.of(reward("minecraft:shield", 1, "minecraft:iron_sword", 1)),
                        List.of(), "", false, List.of(),
                        List.of("effect give @s minecraft:strength 45 0 true"), "", "",
                        "{product} armory block configured weapons and combat boost."),
                field("structure", "outpost_pad", "", false,
                        List.of(), List.of(), "", false, List.of(), List.of(), "", "",
                        "{product} structure block built a field outpost pad."),
                field("wasteland", "wasteland_patch", "", false,
                        List.of(reward(id("ash"), 1, id("scrap_metal"), 1)),
                        List.of(), "", false, List.of(), List.of(), "", "",
                        "{product} terrain block spread wasteland biome material."),
                field("shelter", "shelter", "", false,
                        List.of(), List.of(), "", false, List.of(),
                        List.of("weather clear"), "", "",
                        "{product} atmosphere block stabilized weather and survival telemetry."),
                fieldWithBlockReward("default", "", "", false,
                        List.of(), List.of(), "", false,
                        List.of(product(0, 1, 0, "relay_station", id("echo_cache"))),
                        List.of(), "", "",
                        "{product} block executed its native gameplay role.")
        );
    }

    private static NativeWorldPaintRecipe recipe(String style, List<NativeWorldPaintPlacement> placements) {
        return new NativeWorldPaintRecipe(style, placements);
    }

    private static NativeFieldActionRoute field(
            String actionKey,
            String paintStyle,
            String scenarioMethod,
            boolean scenarioFromPath,
            List<NativeRewardGrant> grants,
            List<NativeRewardGrant> oneShotGrants,
            String oneShotSuffix,
            boolean oneShotUsesBlockPosition,
            List<NativeWorldPaintPlacement> blockPlacements,
            List<String> commands,
            String actionBarText,
            String actionBarColor,
            String summary
    ) {
        return new NativeFieldActionRoute(
                actionKey,
                paintStyle,
                scenarioMethod,
                scenarioFromPath,
                grants,
                oneShotGrants,
                oneShotSuffix,
                oneShotUsesBlockPosition,
                blockPlacements,
                commands,
                actionBarText,
                actionBarColor,
                false,
                summary
        );
    }

    private static NativeFieldActionRoute fieldWithBlockReward(
            String actionKey,
            String paintStyle,
            String scenarioMethod,
            boolean scenarioFromPath,
            List<NativeRewardGrant> grants,
            List<NativeRewardGrant> oneShotGrants,
            String oneShotSuffix,
            boolean oneShotUsesBlockPosition,
            List<NativeWorldPaintPlacement> blockPlacements,
            List<String> commands,
            String actionBarText,
            String actionBarColor,
            String summary
    ) {
        return new NativeFieldActionRoute(
                actionKey,
                paintStyle,
                scenarioMethod,
                scenarioFromPath,
                grants,
                oneShotGrants,
                oneShotSuffix,
                oneShotUsesBlockPosition,
                blockPlacements,
                commands,
                actionBarText,
                actionBarColor,
                true,
                summary
        );
    }

    private static NativeRewardGrant reward(String itemId, int count) {
        return new NativeRewardGrant(itemId, count);
    }

    private static NativeRewardGrant reward(String itemId, int count, String fallbackItemId, int fallbackCount) {
        return new NativeRewardGrant(itemId, count, fallbackItemId, fallbackCount);
    }

    private static NativeWorldPaintPlacement product(int dx, int dy, int dz, String path, String fallbackBlockId) {
        return new NativeWorldPaintPlacement(dx, dy, dz, path, fallbackBlockId, true);
    }

    private static NativeWorldPaintPlacement block(int dx, int dy, int dz, String blockId) {
        return new NativeWorldPaintPlacement(dx, dy, dz, blockId, "", false);
    }

    private static NativeWorldPaintPlacement block(int dx, int dy, int dz, String blockId, String fallbackBlockId) {
        return new NativeWorldPaintPlacement(dx, dy, dz, blockId, fallbackBlockId, false);
    }

    private static List<NativeWorldPaintPlacement> signalTracePaint() {
        List<NativeWorldPaintPlacement> placements = new ArrayList<>();
        for (int index = 0; index < 6; index++) {
            placements.add(product(index, 0, index % 2 == 0 ? 1 : -1, "ash_layer", "minecraft:torch"));
        }
        placements.add(product(6, 0, 0, "relay_station", "minecraft:lodestone"));
        return List.copyOf(placements);
    }

    private static List<NativeWorldPaintPlacement> outpostPadPaint() {
        List<NativeWorldPaintPlacement> placements = new ArrayList<>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean border = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                placements.add(block(dx, -1, dz,
                        border ? "echoblockworks:reinforced_metal_panel" : "echoblockworks:charred_concrete_cracked",
                        border ? "minecraft:iron_block" : "minecraft:polished_deepslate"));
            }
        }
        placements.add(product(0, 0, 0, "echo_cache", "minecraft:barrel"));
        placements.add(product(2, 0, 0, "relay_station", "minecraft:lodestone"));
        placements.add(product(-2, 0, 0, "power_node", "minecraft:redstone_lamp"));
        return List.copyOf(placements);
    }

    private static List<NativeWorldPaintPlacement> wastelandPatchPaint() {
        List<NativeWorldPaintPlacement> placements = new ArrayList<>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) + Math.abs(dz) <= 3) {
                    placements.add(product(dx, -1, dz, "wasteland_dirt", "minecraft:coarse_dirt"));
                    if ((dx + dz) % 2 == 0) {
                        placements.add(product(dx, 0, dz, "ash_layer", "minecraft:dead_bush"));
                    }
                }
            }
        }
        return List.copyOf(placements);
    }

    private static List<NativeWorldPaintPlacement> machineRigPaint() {
        List<NativeWorldPaintPlacement> placements = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                placements.add(block(dx, -1, dz,
                        "echoblockworks:charred_concrete_cracked", "minecraft:polished_deepslate"));
            }
        }
        placements.add(block(0, 0, 0, "echoindustrialnexus:machine_status_panel", "minecraft:blast_furnace"));
        placements.add(block(1, 0, 0, "echoindustrialnexus:robotic_arm_mount", "minecraft:smithing_table"));
        placements.add(product(-1, 0, 0, "scrap_press", "minecraft:stonecutter"));
        placements.add(product(0, 0, 1, "ore_grinder", "minecraft:furnace"));
        return List.copyOf(placements);
    }

    private static List<NativeWorldPaintPlacement> powerCircuitPaint() {
        List<NativeWorldPaintPlacement> placements = new ArrayList<>();
        placements.add(block(0, 0, 0, "echopowergrid:field_battery_bank", "minecraft:redstone_block"));
        for (int index = 1; index <= 5; index++) {
            placements.add(product(index, 0, 0,
                    index % 2 == 0 ? "power_cable" : "high_voltage_power_cable",
                    "minecraft:redstone_wire"));
            placements.add(block(index, 1, 0,
                    index % 2 == 0 ? "minecraft:redstone_lamp" : "minecraft:lightning_rod",
                    "minecraft:torch"));
        }
        placements.add(product(6, 0, 0, "power_node", "minecraft:lodestone"));
        return List.copyOf(placements);
    }

    private static List<NativeWorldPaintPlacement> convoyPadPaint() {
        List<NativeWorldPaintPlacement> placements = new ArrayList<>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                placements.add(block(dx, -1, dz, "echoconvoyprotocol:vehicle_service_pad", "minecraft:smooth_stone"));
            }
            placements.add(block(dx, 0, 0, "echoconvoyprotocol:reinforced_ramp_block", "minecraft:rail"));
        }
        placements.add(block(-2, 0, 1, "echoconvoyprotocol:route_marker_panel", "minecraft:lodestone"));
        placements.add(block(2, 0, 1, "echoconvoyprotocol:vehicle_dock", "minecraft:anvil"));
        return List.copyOf(placements);
    }

    private static List<NativeWorldPaintPlacement> orbitalPadPaint() {
        List<NativeWorldPaintPlacement> placements = new ArrayList<>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 || Math.abs(dz) == 2 || Math.abs(dx) + Math.abs(dz) <= 2) {
                    placements.add(block(dx, -1, dz, "echoorbitalremnants:launch_platform", "minecraft:iron_block"));
                }
            }
        }
        placements.add(block(0, 0, 0, "echoorbitalremnants:docking_beacon", "minecraft:beacon"));
        placements.add(block(0, 1, 0, "minecraft:lightning_rod", "minecraft:torch"));
        placements.add(block(2, 0, 0, "echoorbitalremnants:oxygen_compressor", "minecraft:blast_furnace"));
        placements.add(block(-2, 0, 0, "echoorbitalremnants:fuel_refinery", "minecraft:smoker"));
        return List.copyOf(placements);
    }

    private static List<NativeWorldPaintPlacement> greenhousePaint() {
        List<NativeWorldPaintPlacement> placements = new ArrayList<>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                placements.add(block(dx, -1, dz, "minecraft:farmland"));
                if ((Math.abs(dx) + Math.abs(dz)) % 2 == 0) {
                    placements.add(block(dx, 0, dz, "echoagriculturereclamation:hydroponic_tray", "minecraft:wheat"));
                }
            }
        }
        placements.add(block(0, 0, 0, "echoagriculturereclamation:seed_vault_terminal", "minecraft:composter"));
        placements.add(block(2, 0, 2, "minecraft:water_cauldron", "minecraft:cauldron"));
        return List.copyOf(placements);
    }

    private static List<NativeWorldPaintPlacement> multiblockFramePaint() {
        List<NativeWorldPaintPlacement> placements = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                placements.add(block(dx, -1, dz, "echomultiblockcore:reinforced_machine_frame", "minecraft:iron_block"));
            }
        }
        placements.add(block(0, 0, 0, "echomultiblockcore:multiblock_controller", "minecraft:lodestone"));
        placements.add(block(1, 0, 0, "echomultiblockcore:data_bus", "minecraft:observer"));
        placements.add(block(-1, 0, 0, "echomultiblockcore:power_bus", "minecraft:redstone_block"));
        placements.add(block(0, 0, 1, "echomultiblockcore:input_crate", "minecraft:barrel"));
        placements.add(block(0, 0, -1, "echomultiblockcore:output_crate", "minecraft:chest"));
        return List.copyOf(placements);
    }

    private static List<NativeWorldPaintPlacement> containmentPaint() {
        List<NativeWorldPaintPlacement> placements = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                placements.add(block(dx, -1, dz, "minecraft:obsidian"));
            }
        }
        placements.add(block(0, 0, 0, "echonexusprotocol:nexus_recycler", "minecraft:lodestone"));
        placements.add(block(1, 0, 0, "minecraft:amethyst_block", "minecraft:lapis_block"));
        placements.add(block(-1, 0, 0, "minecraft:crying_obsidian", "minecraft:obsidian"));
        placements.add(block(0, 1, 0, "minecraft:end_rod", "minecraft:torch"));
        return List.copyOf(placements);
    }

    private static List<NativeWorldPaintPlacement> shelterPaint() {
        List<NativeWorldPaintPlacement> placements = new ArrayList<>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                    placements.add(block(dx, 0, dz,
                            "echoblockworks:reinforced_metal_panel", "minecraft:polished_deepslate"));
                    placements.add(block(dx, 2, dz, "minecraft:smooth_stone_slab"));
                }
            }
        }
        placements.add(block(0, 0, 0, "echoweathercore:weather_station", "minecraft:campfire"));
        placements.add(block(1, 0, 0, "echoweathercore:route_warning_post", "minecraft:bell"));
        placements.add(block(-1, 0, 0, "echoweathercore:emergency_siren", "minecraft:note_block"));
        return List.copyOf(placements);
    }

    private static List<NativeWorldPaintPlacement> armoryStationPaint() {
        return List.of(
                block(0, -1, 0, "echoblockworks:charred_concrete_cracked", "minecraft:polished_deepslate"),
                block(0, 0, 0, "echoarmory:weapon_rack", "minecraft:smithing_table"),
                block(1, 0, 0, "echoarmory:loadout_terminal", "minecraft:anvil"),
                block(-1, 0, 0, "minecraft:target", "minecraft:hay_block"),
                block(0, 0, 1, "minecraft:barrel", "minecraft:chest")
        );
    }

    private static List<NativeWorldPaintPlacement> relicSealPaint() {
        return List.of(
                block(0, -1, 0, "minecraft:obsidian", "minecraft:deepslate"),
                block(0, 0, 0, "echorelictech:relic_containment_case", "minecraft:amethyst_block"),
                block(1, 0, 0, "echoblackboxprotocol:vault_monolith", "minecraft:lodestone"),
                block(-1, 0, 0, "echorelictech:containment_locker", "minecraft:barrel"),
                block(0, 1, 0, "echorelictech:relic_containment_case", "echoblackboxprotocol:vault_monolith")
        );
    }

    private static List<NativeWorldPaintPlacement> stationNodePaint() {
        List<NativeWorldPaintPlacement> placements = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                placements.add(block(dx, -1, dz, "echostationfall:stationfall_plating", "minecraft:smooth_stone"));
            }
        }
        placements.add(block(0, 0, 0, "echostationfall:station_power_node", "minecraft:redstone_lamp"));
        placements.add(block(1, 0, 0, "echostationfall:command_console", "minecraft:lectern"));
        placements.add(block(-1, 0, 0, "echostationfall:pressure_door", "minecraft:iron_door"));
        return List.copyOf(placements);
    }

    private static List<NativeWorldPaintPlacement> arcanaNodePaint() {
        return List.of(
                block(0, -1, 0, "minecraft:amethyst_block"),
                block(0, 0, 0, "echoaetherworks:aether_cell", "minecraft:enchanting_table"),
                block(1, 0, 0, "echoritualcore:offering_pedestal", "minecraft:lectern"),
                block(-1, 0, 0, "echospellcore:signal_focus", "minecraft:lapis_block"),
                block(0, 1, 0, "minecraft:end_rod", "minecraft:torch")
        );
    }

    private static List<NativeWorldPaintPlacement> logisticsDepotPaint() {
        return List.of(
                block(0, -1, 0, "echoblockworks:charred_concrete_cracked", "minecraft:polished_deepslate"),
                block(0, 0, 0, "echologisticsnetwork:logistics_terminal", "minecraft:barrel"),
                block(1, 0, 0, "echologisticsnetwork:route_requester", "minecraft:hopper"),
                block(-1, 0, 0, "echologisticsnetwork:faction_trade_depot", "minecraft:chest"),
                block(0, 0, 1, "signalos:network_relay", "minecraft:lodestone")
        );
    }

    @Override
    public String nativeItemGroupTranslationKey() {
        return "itemGroup.echo_native.ashfall";
    }

    @Override
    public String nativeItemGroupTranslationName() {
        return "Ashfall";
    }

    @Override
    public Map<String, List<String>> nativeCreativeTabPreferredIcons() {
        return Map.of(
                NAMESPACE,
                List.of(
                        id("portable_signal_scanner"),
                        id("scrap_knife"),
                        id("power_cell")
                ),
                "echoindustrialnexus",
                List.of(
                        "echoindustrialnexus:machine_casing",
                        "echoindustrialnexus:ai_control_chip"
                ),
                "echopowergrid",
                List.of(
                        "echopowergrid:power_node",
                        "echopowergrid:battery_core"
                ),
                "echoterminal",
                List.of("echoterminal:echo_terminal_remote"),
                "echowiki",
                List.of("echowiki:guide_book"),
                "signalos",
                List.of("signalos:data_drive")
        );
    }

    @Override
    public List<String> nativeInfoModuleNamespaces() {
        return List.of(
                "echoterminal",
                "echowiki",
                "echoindex",
                "echolens",
                "echoholomap",
                "signalos"
        );
    }

    @Override
    public Map<String, String> nativeInfoModuleRuntimeRoutes() {
        return Map.of(
                "echoterminal", "terminal",
                "echoindex", "index",
                "echolens", "lens",
                "echoholomap", "holomap",
                "echowiki", "wiki",
                "signalos", "signalos"
        );
    }

    @Override
    public Map<String, String> nativeModuleDisplayNames() {
        return Map.of(
                "echoterminal", "Terminal",
                "echoindex", "Index",
                "echolens", "Lens",
                "echoholomap", "HoloMap",
                "echowiki", "Wiki",
                "signalos", "SignalOS"
        );
    }

    @Override
    public Map<String, List<NativeInfoModuleStaticInvocation>> nativeInfoModuleStaticInvocations() {
        return Map.of(
                "echoterminal", List.of(
                        staticInvocation("terminalCoreServicesRegistered",
                                "com.knoxhack.echoterminal.service.EchoTerminalCoreServices", "register"),
                        staticInvocation("terminalCommonIntegrationRegistered",
                                "com.knoxhack.echoterminal.BuiltinTerminalCommonIntegration", "register"),
                        staticInvocation("terminalBuiltinTabsRegistered",
                                "com.knoxhack.echoterminal.client.BuiltinTerminalTabs", "register"),
                        staticInvocation("terminalTabsSorted",
                                "com.knoxhack.echoterminal.api.TerminalTabRegistry", "ensureSorted")
                ),
                "echoindex", List.of(
                        staticInvocation("indexTerminalCommonIntegrationRegistered",
                                "com.knoxhack.echoindex.integration.IndexTerminalCommonIntegration", "register"),
                        staticInvocation("indexMissionCoreIntegrationRegistered",
                                "com.knoxhack.echoindex.integration.IndexMissionCoreIntegration", "register")
                ),
                "echolens", List.of(
                        staticInvocation("lensBuiltinsRegistered",
                                "com.knoxhack.echolens.provider.LensBuiltins", "register"),
                        staticInvocation("lensCoreIntegrationRegistered",
                                "com.knoxhack.echolens.integration.LensCoreIntegration", "register"),
                        staticInvocation("lensMissionCoreIntegrationRegistered",
                                "com.knoxhack.echolens.integration.LensMissionCoreIntegration", "register")
                ),
                "echoholomap", List.of(
                        staticInvocation("holoMapTerminalCommonIntegrationRegistered",
                                "com.knoxhack.echoholomap.integration.HoloMapTerminalCommonIntegration", "register"),
                        staticInvocation("holoMapTerminalClientIntegrationRegistered",
                                "com.knoxhack.echoholomap.integration.HoloMapTerminalClientIntegration", "register"),
                        staticInvocation("holoMapMissionCoreIntegrationRegistered",
                                "com.knoxhack.echoholomap.integration.HoloMapMissionCoreIntegration", "register"),
                        staticInvocation("holoMapIndexIntegrationRegistered",
                                "com.knoxhack.echoholomap.integration.HoloMapIndexIntegration", "register")
                ),
                "echowiki", List.of(
                        staticInvocation("wikiDefaultsEnsured",
                                "com.knoxhack.echowiki.content.WikiContentRegistry", "ensureDefaults"),
                        staticInvocation("wikiTerminalClientIntegrationRegistered",
                                "com.knoxhack.echowiki.integration.WikiTerminalClientIntegration", "register")
                ),
                "signalos", List.of(
                        staticInvocation("signalOsBuiltinContentRegistered",
                                "com.knoxhack.signalos.content.SignalOsBuiltinContent", "register"),
                        staticInvocation("signalOsBuiltinActionsRegistered",
                                "com.knoxhack.signalos.service.SignalOsBuiltinActions", "register")
                )
        );
    }

    @Override
    public Map<String, List<NativeInfoModuleStaticValueInvocation>> nativeInfoModuleStaticValueInvocations() {
        return Map.of(
                "echoterminal", List.of(
                        staticValueInvocation("terminalTabs",
                                "com.knoxhack.echoterminal.api.TerminalTabRegistry", "tabs")
                ),
                "echolens", List.of(
                        staticValueInvocation("lensProviderCount",
                                "com.knoxhack.echolens.registry.LensProviderRegistry", "count"),
                        staticValueInvocation("lensServerProviders",
                                "com.knoxhack.echolens.registry.LensProviderRegistry", "serverProviders"),
                        staticValueInvocation("lensDiagnostics",
                                "com.knoxhack.echolens.registry.LensProviderRegistry", "diagnostics")
                ),
                "signalos", List.of(
                        staticValueInvocation("signalOsChapters",
                                "com.knoxhack.signalos.content.SignalOsContentRegistry", "chapters"),
                        staticValueInvocation("signalOsMissions",
                                "com.knoxhack.signalos.content.SignalOsContentRegistry", "missions"),
                        staticValueInvocation("signalOsArchives",
                                "com.knoxhack.signalos.content.SignalOsContentRegistry", "archives"),
                        staticValueInvocation("signalOsApps",
                                "com.knoxhack.signalos.content.SignalOsContentRegistry", "apps")
                )
        );
    }

    @Override
    public Map<String, List<NativeInfoModuleStaticFieldValue>> nativeInfoModuleStaticFieldValues() {
        return Map.of(
                "echoterminal", List.of(
                        staticField("terminalMainSurvivalProviderInstance",
                                "com.knoxhack.echoterminal.mission.MainSurvivalQuestProvider", "INSTANCE"),
                        staticField("terminalVanillaJourneyProviderInstance",
                                "com.knoxhack.echoterminal.mission.VanillaJourneyProvider", "INSTANCE"),
                        staticField("terminalMainSurvivalTabId",
                                "com.knoxhack.echoterminal.mission.MainSurvivalQuestProvider", "TAB_ID"),
                        staticField("terminalVanillaJourneyTabId",
                                "com.knoxhack.echoterminal.mission.VanillaJourneyProvider", "TAB_ID")
                ),
                "echoindex", List.of(
                        staticField("indexServiceInstance",
                                "com.knoxhack.echoindex.service.IndexService", "INSTANCE")
                ),
                "echoholomap", List.of(
                        staticField("holoMapServiceInstance",
                                "com.knoxhack.echoholomap.map.HoloMapService", "INSTANCE")
                )
        );
    }

    @Override
    public Map<String, List<NativeInfoModuleStaticFieldArgumentInvocation>> nativeInfoModuleStaticFieldArgumentInvocations() {
        return Map.of(
                "echoterminal", List.of(
                        staticFieldArgumentInvocation("terminalMainSurvivalProviderRegistered",
                                "com.knoxhack.echoterminal.api.mission.TerminalMissionRegistry", "register",
                                "terminalMainSurvivalProviderInstance"),
                        staticFieldArgumentInvocation("terminalVanillaJourneyProviderRegistered",
                                "com.knoxhack.echoterminal.api.mission.TerminalMissionRegistry", "register",
                                "terminalVanillaJourneyProviderInstance"),
                        staticFieldArgumentInvocation("terminalMainSurvivalActionsRegistered",
                                "com.knoxhack.echoterminal.api.mission.TerminalMissionActions", "registerForTab",
                                "terminalMainSurvivalTabId"),
                        staticFieldArgumentInvocation("terminalVanillaJourneyActionsRegistered",
                                "com.knoxhack.echoterminal.api.mission.TerminalMissionActions", "registerForTab",
                                "terminalVanillaJourneyTabId")
                )
        );
    }

    @Override
    public List<String> nativeInfoModulePlacementHints() {
        return List.of("terminal", "network", "relay", "workstation");
    }

    @Override
    public List<String> nativeInfoModuleRewardItemHints() {
        return List.of("remote", "terminal", "guide", "data_drive");
    }

    @Override
    public List<String> nativeInfoModuleFallbackBlockIds() {
        return List.of("signalos:network_relay", id("relay_station"));
    }

    @Override
    public List<String> nativeInfoModuleFallbackItemIds() {
        return List.of("echoterminal:echo_terminal_remote", id("field_manual"));
    }

    @Override
    public List<String> nativeRecoveryModuleNamespaces() {
        return List.of("echorecovery");
    }

    @Override
    public List<String> nativeRecoveryPlacementHints() {
        return List.of("recovery_cache", "grave", "death_cache");
    }

    @Override
    public List<String> nativeRecoveryBlockPlacementHints() {
        return List.of("recovery_cache", "grave", "death_cache", "soul_urn");
    }

    @Override
    public List<String> nativeRecoveryRewardItemHints() {
        return List.of("recovery_compass", "recovery_token", "grave_key");
    }

    @Override
    public List<String> nativeArcanaModuleNamespaces() {
        return List.of(
                "echoaetherworks",
                "echocursecore",
                "echofamiliarcore",
                "echoriftworlds"
        );
    }

    @Override
    public List<String> nativeArcanaPlacementHints() {
        return List.of("aether", "curse", "spirit", "rift", "cell", "pocket");
    }

    @Override
    public List<String> nativeArcanaRewardItemHints() {
        return List.of("aether", "curse", "spirit", "rift", "charm", "core", "sample");
    }

    @Override
    public List<String> nativeArcanaFallbackItemIds() {
        return List.of("minecraft:amethyst_shard", "minecraft:ender_pearl");
    }

    @Override
    public List<String> nativeItemShimPathHints() {
        return List.of(
                "battery",
                "water_bottle",
                "filter",
                "rad_away",
                "mutagen",
                "scanner",
                "drone",
                "keystone",
                "beacon",
                "dampener",
                "archives_key",
                "schematic",
                "bandage",
                "stim_pack",
                "hand_warmer"
        );
    }

    @Override
    public List<String> nativeBlockShimPathHints() {
        return List.of(
                "terminal",
                "relay",
                "scanner",
                "cache",
                "crate",
                "map_table",
                "purifier",
                "cleanser",
                "dynamo",
                "generator",
                "power_node",
                "power_cable",
                "reactor",
                "capacitor",
                "battery",
                "press",
                "grinder",
                "burner",
                "array",
                "workshop",
                "research_lab",
                "med_bay",
                "station",
                "machine",
                "controller",
                "structure_cache",
                "nexus_core"
        );
    }

    @Override
    public List<NativeUiSurfaceRoute> nativeUiSurfaceRoutes() {
        return List.of(
                surface("MAIN_MENU", "echoashfallprotocol:main_menu", id("echo_native_main_menu"), id("echo_native_main_menu")),
                surface("LOADING", "echoashfallprotocol:loading", id("echo_native_loading"), id("echo_native_loading")),
                surface("TERMINAL", "echoterminal:terminal", "echoterminal:terminal", "echoterminal:terminal"),
                surface("INDEX", "echoindex:index", "echoindex:index", "echoindex:index"),
                surface("LENS", "echolens:lens", id("portable_signal_scanner"), id("portable_signal_scanner")),
                surface("HOLOMAP", "echoholomap:map", "echoholomap:ashfall_map", "echoholomap:ashfall_map"),
                surface("WIKI", "echowiki:wiki", "echowiki:ashfall", "echowiki:ashfall"),
                surface("SIGNALOS", "signalos:terminal", "signalos:terminal", "signalos:terminal"),
                surface("HUD", "echohudcore:hud", id("runtime_hud_notification"), id("runtime_hud_notification")),
                surface("MISSION_LOG", "echoscreencore:mission_log", id("secure_crash_outpost"), id("secure_crash_outpost")),
                surface("MACHINE", id("machine"), id("machine"), id("machine")),
                surface("ASHFALL_DRONE", id("drone"), id("companion_drone"), id("companion_drone"))
        );
    }

    @Override
    public Map<String, String> nativeClientScreenClasses() {
        return Map.of(
                "MAIN_MENU", "com.knoxhack.echoashfallprotocol.client.screen.EchoNativeMainMenuScreen",
                "LOADING", "com.knoxhack.echoashfallprotocol.client.screen.EchoNativeAshfallSurfaceScreen",
                "TERMINAL", "com.knoxhack.echoashfallprotocol.client.screen.EchoNativeTerminalLaunchScreen",
                "INDEX", "com.knoxhack.echoindex.client.IndexCatalogScreen",
                "LENS", "com.knoxhack.echoashfallprotocol.client.screen.EchoNativeAshfallSurfaceScreen",
                "HOLOMAP", "com.knoxhack.echoholomap.client.HoloMapFullScreenMapScreen",
                "WIKI", "com.knoxhack.echoashfallprotocol.client.screen.EchoNativeAshfallSurfaceScreen"
        );
    }

    @Override
    public Map<String, String> nativeClientHudRendererClasses() {
        return Map.ofEntries(
                Map.entry("HUD", "com.knoxhack.echoashfallprotocol.client.hud.EchoNativeAshfallHudOverlay"),
                Map.entry("INDEX", "com.knoxhack.echoindex.client.IndexOverlay"),
                Map.entry("LENS", "com.knoxhack.echolens.client.LensHudOverlay"),
                Map.entry("HOLOMAP", "com.knoxhack.echoholomap.client.HoloMapMiniMapOverlay")
        );
    }

    @Override
    public Map<String, String> nativeClientLoadingRendererClasses() {
        return Map.of(
                "LOADING", "com.knoxhack.echoashfallprotocol.client.screen.EchoNativeAshfallLoadingOverlay"
        );
    }

    @Override
    public List<String> nativeUiHotkeys() {
        return List.of(
                "M:Terminal",
                "G:Index Catalog",
                "R:Index Recipes",
                "U:Index Uses",
                "B:Index Bookmarks",
                "Left Alt:Lens Deep Scan",
                "Right Alt:Lens Deep Scan",
                "J:HoloMap",
                "K:HoloMap Minimap",
                "]:HoloMap Zoom In",
                "[:HoloMap Zoom Out",
                "\\:HoloMap Corner",
                "N:SignalOS Terminal",
                "X:Ashfall Drone Recall",
                "C:Ashfall Drone Scan",
                "Y:Ashfall Drone Scout",
                "Z:Ashfall Drone Status",
                "H:Ashfall Drone Assist"
        );
    }

    @Override
    public Map<String, String> nativeUiHotkeyConflicts() {
        return Map.of();
    }

    @Override
    public String nativeRecoveryItemId() {
        return id("portable_signal_scanner");
    }

    @Override
    public String nativePlayableProofMarkerBlockId() {
        return id("native_loader_proof_marker");
    }

    @Override
    public List<String> nativePlayableStarterToolItemIds() {
        return List.of(
                "echoterminal:echo_terminal_remote",
                id("portable_signal_scanner"),
                id("relay_scanner_lens"),
                "echoweathercore:storm_scanner",
                id("power_cell")
        );
    }

    @Override
    public String nativePlayableStarterRegionCoreBlockId() {
        return "echoblockworks:charred_concrete_cracked";
    }

    @Override
    public String nativePlayableStarterRegionTerrainBlockId() {
        return id("wasteland_dirt");
    }

    @Override
    public String nativePlayableStarterRegionSurfaceBlockId() {
        return id("ash_layer");
    }

    @Override
    public List<String> nativePlayableStarterRegionFeatureBlockIds() {
        return List.of(
                id("echo_cache"),
                id("relay_station"),
                id("map_table"),
                id("scrap_dynamo"),
                id("power_node")
        );
    }

    @Override
    public String nativeInteractionProbeItemId() {
        return id("portable_signal_scanner");
    }

    @Override
    public String nativeInteractionProbePlacementBlockId() {
        return id("echo_cache");
    }

    @Override
    public String nativeInteractionProbeBlockUseId() {
        return id("relay_station");
    }

    @Override
    public String nativeInteractionProbeEntityItemId() {
        return id("scout_drone_item");
    }

    @Override
    public List<NativeActionKeyPathHints> nativeActionKeyPathHints() {
        return List.of(
                new NativeActionKeyPathHints("cache", List.of("cache", "crate", "vault", "loot", "chest", "supply", "salvage")),
                new NativeActionKeyPathHints("signal", List.of("relay", "antenna", "radio", "signal", "terminal", "scanner", "lens", "radar", "satellite", "visor")),
                new NativeActionKeyPathHints("route", List.of("map", "marker", "holomap", "waypoint", "route")),
                new NativeActionKeyPathHints("survival", List.of("purifier", "filter", "water", "cleanser", "scrubber", "med", "med_bay", "stim", "radaway")),
                new NativeActionKeyPathHints("machine", List.of("machine", "factory", "recycler", "grinder", "refiner", "synthesizer", "press", "lab", "workshop")),
                new NativeActionKeyPathHints("power", List.of("power", "dynamo", "battery", "cable", "reactor", "thermal", "generator", "capacitor", "nexus_core")),
                new NativeActionKeyPathHints("agriculture", List.of("crop", "plant", "fungus", "grass", "sapling", "berries", "seed", "soil")),
                new NativeActionKeyPathHints("armory", List.of("weapon", "rack", "armory", "upgrade", "dock", "armor", "ammo", "loadout")),
                new NativeActionKeyPathHints("structure", List.of("structure", "builder", "base", "grid", "outpost", "camp")),
                new NativeActionKeyPathHints("entity", List.of("drone", "spawn_egg", "sentinel", "wraith", "zombie", "wolf", "slime", "creature")),
                new NativeActionKeyPathHints("shelter", List.of("campfire", "bunk", "shelter", "warmer", "cold", "rain", "atmosphere", "weather", "sensor")),
                new NativeActionKeyPathHints("vehicle", List.of("vehicle", "convoy", "rover", "garage", "repair", "gantry", "dock")),
                new NativeActionKeyPathHints("orbital", List.of("launch", "orbital", "oxygen", "fuel", "satellite", "docking")),
                new NativeActionKeyPathHints("arcana", List.of("spell", "ritual", "relic", "aether", "curse", "rift", "familiar")),
                new NativeActionKeyPathHints("wasteland", List.of("ash", "wasteland", "scorched", "toxic", "radiation", "nuclear", "burnt"))
        );
    }

    @Override
    public List<NativeRewardRule> nativeStarterRewardRules() {
        return List.of(
                new NativeRewardRule(
                        List.of("scanner"),
                        List.of(new NativeRewardGrant(id("ash"), 1))
                ),
                new NativeRewardRule(
                        List.of("terminal"),
                        List.of(new NativeRewardGrant(nativeRecoveryItemId(), 1))
                )
        );
    }

    @Override
    public List<NativeRewardRule> nativeBlockRewardRules() {
        return List.of(
                new NativeRewardRule(
                        List.of("cache", "crate"),
                        List.of(new NativeRewardGrant(id("scrap_metal"), 2, id("ash"), 1))
                ),
                new NativeRewardRule(
                        List.of("relay", "map_table"),
                        List.of(new NativeRewardGrant(id("relay_scanner_lens"), 1))
                )
        );
    }

    @Override
    public List<NativeItemActionRule> nativeItemActionRules() {
        return List.of(
                new NativeItemActionRule("water_bottle", List.of("water_bottle")),
                new NativeItemActionRule("radiation_medicine", List.of("rad_away")),
                new NativeItemActionRule("mutagen_delegate", List.of("mutagen")),
                new NativeItemActionRule("filter_cartridge", List.of("filter_cartridge")),
                new NativeItemActionRule("crude_filter", List.of("crude_filter")),
                new NativeItemActionRule("bandage", List.of("bandage")),
                new NativeItemActionRule("stim_pack", List.of("stim_pack")),
                new NativeItemActionRule("hand_warmer", List.of("hand_warmer")),
                new NativeItemActionRule("scanner", List.of("scanner", "lens")),
                new NativeItemActionRule("deploy_entity", List.of("drone")),
                new NativeItemActionRule("beacon_keystone", List.of("beacon", "keystone")),
                new NativeItemActionRule("weather_dampener", List.of("dampener")),
                new NativeItemActionRule("archive_item", List.of("schematic", "data_log"))
        );
    }

    @Override
    public List<NativeBlockActionRule> nativeBlockActionRules() {
        return List.of(
                new NativeBlockActionRule("cache", List.of("cache", "crate", "structure_cache")),
                new NativeBlockActionRule("relay", List.of("relay_station")),
                new NativeBlockActionRule("power_node", List.of("power_node")),
                new NativeBlockActionRule("emergency_bunk", List.of("emergency_bunk")),
                new NativeBlockActionRule("water_machine", List.of("water_purifier", "rain_collector")),
                new NativeBlockActionRule("hazard_machine", List.of("radiation_cleanser", "atmospheric_scrubber", "field_med_bay")),
                new NativeBlockActionRule("research_lab", List.of("research_lab")),
                new NativeBlockActionRule("generator", List.of("scrap_dynamo", "micro_generator", "thermal_burner", "thermal_array")),
                new NativeBlockActionRule("power_grid", List.of(
                        "battery_bank",
                        "power_cable",
                        "reinforced_power_cable",
                        "high_voltage_power_cable",
                        "energy_meter",
                        "load_distributor",
                        "factory_controller",
                        "nexus_capacitor"
                )),
                new NativeBlockActionRule("processor", List.of(
                        "scrap_press",
                        "ore_grinder",
                        "isotope_refiner",
                        "crystalline_synthesizer",
                        "deep_core_miner",
                        "hand_recycler",
                        "filter_workbench",
                        "workshop",
                        "item_pipe",
                        "autofeed_hopper",
                        "contaminant_condenser"
                )),
                new NativeBlockActionRule("stationary_scanner", List.of("signal_scanner", "stationary_scanner")),
                new NativeBlockActionRule("map_table", List.of("map_table")),
                new NativeBlockActionRule("campaign_core", List.of("nexus_core"))
        );
    }

    @Override
    public Map<String, String> nativeBlockActionMachineIds() {
        return Map.ofEntries(
                Map.entry("cache", "recovery_cache"),
                Map.entry("relay", "relay_station"),
                Map.entry("power_node", "power_node"),
                Map.entry("emergency_bunk", "emergency_bunk"),
                Map.entry("research_lab", "research_lab"),
                Map.entry("campaign_core", "nexus_core")
        );
    }

    @Override
    public List<NativeBlockFallbackRule> nativeBlockFallbackRules() {
        return List.of(
                new NativeBlockFallbackRule(List.of("layer", "grass", "bush"), "minecraft:dead_bush"),
                new NativeBlockFallbackRule(List.of("cache", "crate"), "minecraft:barrel"),
                new NativeBlockFallbackRule(List.of("relay", "map", "power_node"), "minecraft:lodestone"),
                new NativeBlockFallbackRule(List.of("dynamo", "generator"), "minecraft:redstone_block"),
                new NativeBlockFallbackRule(List.of("concrete", "asphalt"), "minecraft:polished_deepslate")
        );
    }

    @Override
    public NativeMachineOperationRules nativeMachineOperationRules() {
        return new NativeMachineOperationRules(
                List.of("energy_cell", "power_cell", "basic_battery"),
                List.of("energy_cell", "power_cell", "basic_battery", "advanced_battery", "elite_battery"),
                List.of("filter_cartridge_basic", "filter_cartridge_advanced", "filter_cartridge_elite"),
                "dirty_water_bottle",
                "clean_water_bottle",
                80,
                List.of("filter_cartridge_advanced"),
                List.of("contaminated_iron", "contaminated_gold", "contaminated_redstone", "contaminated_lapis"),
                4_000,
                400,
                2_000,
                40,
                2_000,
                20,
                List.of(
                        "rare_tech_schematic",
                        "schematic_fragment_machines",
                        "schematic_fragment_energy",
                        "schematic_fragment_medical",
                        "schematic_fragment_weapons",
                        "schematic_fragment_armor"
                ),
                List.of(
                        "minecraft:coal",
                        "minecraft:charcoal",
                        "minecraft:oak_planks",
                        "minecraft:spruce_planks",
                        "minecraft:birch_planks",
                        "minecraft:dark_oak_planks",
                        "minecraft:stick",
                        "scrap_metal",
                        "scrap_plastic",
                        "scrap_circuit",
                        "ash"
                ),
                List.of("energy_cell", "power_cell", "basic_battery", "advanced_battery", "elite_battery"),
                1_000,
                "energy_cell",
                List.of("rain_collector"),
                List.of("radiation_cleanser"),
                List.of("med_bay")
        );
    }

    @Override
    public NativePathValueRules nativeBatteryCapacityRules() {
        return new NativePathValueRules(0, List.of(
                new NativePathValueRule(List.of("basic_battery"), 2_000),
                new NativePathValueRule(List.of("advanced_battery"), 10_000),
                new NativePathValueRule(List.of("elite_battery"), 50_000)
        ));
    }

    @Override
    public NativePathValueRules nativeMachineCapacityRules() {
        return new NativePathValueRules(2_000, List.of(
                new NativePathValueRule(List.of("elite_battery", "factory_controller", "nexus_capacitor", "high_voltage"), 50_000),
                new NativePathValueRule(List.of("advanced_battery", "battery_bank", "load_distributor", "reinforced"), 10_000),
                new NativePathValueRule(List.of("water_purifier", "radiation_cleanser", "ore_grinder", "isotope_refiner"), 4_000)
        ));
    }

    @Override
    public NativePathValueRules nativeFuelEnergyRules() {
        return new NativePathValueRules(100, List.of(
                new NativePathValueRule(List.of("elite_battery"), 50_000),
                new NativePathValueRule(List.of("advanced_battery"), 10_000),
                new NativePathValueRule(List.of("basic_battery"), 2_000),
                new NativePathValueRule(List.of("energy_cell"), 1_000),
                new NativePathValueRule(List.of("power_cell"), 750),
                new NativePathValueRule(List.of("coal", "charcoal"), 400),
                new NativePathValueRule(List.of("scrap_metal"), 120),
                new NativePathValueRule(List.of("ash"), 40)
        ));
    }

    @Override
    public NativePathValueRules nativeEnergyItemChargeRules() {
        return new NativePathValueRules(1_000, List.of(
                new NativePathValueRule(List.of("elite_battery"), 8_000),
                new NativePathValueRule(List.of("advanced_battery"), 4_000),
                new NativePathValueRule(List.of("basic_battery"), 2_000),
                new NativePathValueRule(List.of("power_cell"), 1_500),
                new NativePathValueRule(List.of("energy_cell"), 1_000)
        ));
    }

    @Override
    public NativeOutputRules nativeContaminatedOutputRules() {
        return new NativeOutputRules("minecraft:iron_ingot", List.of(
                new NativeOutputRule(List.of("gold"), "minecraft:gold_ingot"),
                new NativeOutputRule(List.of("redstone"), "minecraft:redstone"),
                new NativeOutputRule(List.of("lapis"), "minecraft:lapis_lazuli")
        ));
    }

    @Override
    public List<String> nativeMachinePaths() {
        return List.of(
                "thermal_burner",
                "thermal_array",
                "micro_generator",
                "scrap_dynamo",
                "battery_bank",
                "energy_meter",
                "load_distributor",
                "factory_controller",
                "nexus_capacitor",
                "water_purifier",
                "rain_collector",
                "radiation_cleanser",
                "atmospheric_scrubber",
                "field_med_bay",
                "scrap_press",
                "ore_grinder",
                "isotope_refiner",
                "crystalline_synthesizer",
                "deep_core_miner",
                "hand_recycler",
                "filter_workbench",
                "workshop",
                "autofeed_hopper",
                "contaminant_condenser",
                "research_lab",
                "signal_scanner",
                "stationary_scanner",
                "relay_station",
                "power_node"
        );
    }

    @Override
    public String nativeMachineScreenId() {
        return id("machine");
    }

    @Override
    public String nativeMachineEffectPrefix() {
        return id("native_machine_screen.open");
    }

    @Override
    public List<NativeUiActionRoute> nativeUiActionRoutes() {
        return List.of(new NativeUiActionRoute(
                "ASHFALL_DRONE",
                nativeUiActionCommand(),
                "echoashfallprotocol:drone",
                "echoashfallprotocol:companion_drone",
                "echoashfallprotocol:companion_drone",
                "native_ui_ashfall_drone",
                "com.knoxhack.echoashfallprotocol.EchoAshfallProtocolClient",
                "echoashfallprotocol:drone_command",
                "com.knoxhack.echoashfallprotocol.network.DroneCommandPacket",
                "H",
                "index.bookmark",
                "ashfall.drone_assist",
                Map.of(
                        "ashfall.drone_recall", "recall",
                        "ashfall.drone_scan", "scan_area",
                        "ashfall.drone_scout", "scout_ahead",
                        "ashfall.drone_status", "status",
                        "ashfall.drone_assist", "toggle_assist"
                )
        ));
    }

    @Override
    public Map<String, List<String>> nativeUiDataSourceRoots() {
        return Map.ofEntries(
                Map.entry("signalos", List.of(
                        "data/echoashfallprotocol/signalos/chapters",
                        "data/signalos/signalos/chapters",
                        "data/echoashfallprotocol/signalos/missions",
                        "data/signalos/signalos/missions",
                        "data/echoashfallprotocol/signalos/archives",
                        "data/signalos/signalos/archives",
                        "data/echoashfallprotocol/signalos/apps",
                        "data/signalos/signalos/apps",
                        "data/echoashfallprotocol/signalos/data_records",
                        "data/signalos/signalos/data_records",
                        "data/echoashfallprotocol/signalos/drive_templates",
                        "data/signalos/signalos/drive_templates",
                        "data/echoashfallprotocol/signalos/net_sites",
                        "data/signalos/signalos/net_sites"
                )),
                Map.entry("productActionMissions", List.of("data/echoashfallprotocol/missioncore/missions")),
                Map.entry("machineRecipes", List.of(
                        "data/echoashfallprotocol/recipe",
                        "data/echoashfallprotocol/recipes",
                        "data/echoashfallprotocol/station_recipes"
                )),
                Map.entry("terminalPages", List.of(
                        "data/echoashfallprotocol/echoterminal/pages",
                        "data/echoterminal/echoterminal/pages"
                )),
                Map.entry("indexEntries", List.of(
                        "data/echoashfallprotocol/echoindex/entries",
                        "data/echoashfallprotocol/echo_index/entries",
                        "data/echoindex/echo_index/entries"
                )),
                Map.entry("lensProfiles", List.of(
                        "data/echoashfallprotocol/echolens/scan_profiles",
                        "data/echoashfallprotocol/lens/scan_profiles",
                        "data/echolens/echolens/scan_profiles"
                )),
                Map.entry("missionLog", List.of("data/echoashfallprotocol/missioncore/missions")),
                Map.entry("recoveryGraveTypes", List.of(
                        "data/echorecovery/echorecovery/recovery_grave_type",
                        "data/echoashfallprotocol/echorecovery/recovery_grave_type"
                )),
                Map.entry("recoveryPresets", List.of(
                        "data/echorecovery/echorecovery/recovery_preset",
                        "data/echoashfallprotocol/echorecovery/recovery_preset"
                )),
                Map.entry("recoveryRules", List.of(
                        "data/echorecovery/echorecovery/recovery_rule",
                        "data/echoashfallprotocol/echorecovery/recovery_rule"
                )),
                Map.entry("holomapLayers", List.of(
                        "data/echoashfallprotocol/echoholomap/layers",
                        "data/echoashfallprotocol/holomap/layers",
                        "data/echoholomap/echoholomap/layers"
                )),
                Map.entry("wikiContent", List.of(
                        "data/echoashfallprotocol/echowiki/articles",
                        "data/echowiki/echowiki/articles",
                        "data/echoashfallprotocol/echowiki/collections",
                        "data/echowiki/echowiki/collections",
                        "data/echoashfallprotocol/echowiki/guide_books",
                        "data/echowiki/echowiki/guide_books"
                ))
        );
    }

    @Override
    public Map<String, String> nativeUiDefaultContentIds() {
        return Map.ofEntries(
                Map.entry("scanner.scanTarget", id("scan_first_poi")),
                Map.entry("terminal.defaultRoute", "echoterminal:" + nativeGameplayPackId() + "_first_steps"),
                Map.entry("missionLog.itemId", id("field_manual")),
                Map.entry("signalosRecord", "chapters"),
                Map.entry("productActionRepairMission", "repair_echo_drone"),
                Map.entry("productActionIntelMission", "recover_drone_intel"),
                Map.entry("terminalPage", "ashfall_first_month_routes"),
                Map.entry("indexEntry", "faction_field_contacts"),
                Map.entry("lensProfile", "ashfall_showcase_scans"),
                Map.entry("missionLog", "secure_crash_outpost"),
                Map.entry("recoveryGraveType", "ashfall_field_recovery_cache"),
                Map.entry("recoveryPreset", "forgiving"),
                Map.entry("recoveryRule", "recovery_defaults"),
                Map.entry("holomapLayer", "first_month_field_intel"),
                Map.entry("wikiArticle", "guides/ashfall")
        );
    }

    @Override
    public List<String> nativeMainMenuOptions() {
        return List.of("Ashfall World", "Guarded Worlds", "Module Index", "Settings", "Quit");
    }

    @Override
    public List<NativePhysicalActionRoute> nativePhysicalActionRoutes() {
        return List.of(
                physicalAction("X", "ASHFALL_DRONE", "ashfall.drone_recall", false),
                physicalAction("C", "ASHFALL_DRONE", "ashfall.drone_scan", false),
                physicalAction("Y", "ASHFALL_DRONE", "ashfall.drone_scout", false),
                physicalAction("Z", "ASHFALL_DRONE", "ashfall.drone_status", false),
                physicalAction("H", "ASHFALL_DRONE", "ashfall.drone_assist", true)
        );
    }

    private static NativeEntityDefinition entity(
            String path,
            String className,
            String fallbackClassName,
            String category,
            float width,
            float height,
            int trackingRange,
            boolean fireImmune
    ) {
        return new NativeEntityDefinition(path, className, fallbackClassName, category, width, height, trackingRange, fireImmune);
    }

    private static NativeSourceBackedContentMapping item(String path, String family, String className) {
        return new NativeSourceBackedContentMapping(
                NAMESPACE + ":" + path,
                family,
                "addons/echoashfallprotocol/src/main/java/com/knoxhack/echoashfallprotocol/registry/ModItems.java",
                "com.knoxhack.echoashfallprotocol.item." + className,
                "newRealEchoItem"
        );
    }

    private static NativeSourceBackedContentMapping block(String path, String family, String className) {
        return new NativeSourceBackedContentMapping(
                NAMESPACE + ":" + path,
                family,
                "addons/echoashfallprotocol/src/main/java/com/knoxhack/echoashfallprotocol/registry/ModBlocks.java",
                className.startsWith("net.minecraft.")
                        ? className
                        : "com.knoxhack.echoashfallprotocol.block." + className,
                "newRealEchoBlock"
        );
    }

    private static NativeSourceContractFile sourceFile(String kind, String path) {
        return new NativeSourceContractFile(kind, path);
    }

    private static NativeItemConstructorBinding itemConstructor(String path, String className) {
        return itemConstructor(path, className, "", "");
    }

    private static NativeItemConstructorBinding itemConstructor(
            String path,
            String className,
            String enumClassName,
            String enumName
    ) {
        return new NativeItemConstructorBinding(
                NAMESPACE + ":" + path,
                "com.knoxhack.echoashfallprotocol.item." + className,
                enumClassName,
                enumName
        );
    }

    private static NativeBlockConstructorBinding blockConstructor(String path, String className) {
        String resolvedClassName = className.contains(".") || className.startsWith("runtime:")
                ? className
                : "com.knoxhack.echoashfallprotocol.block." + className;
        return new NativeBlockConstructorBinding(
                NAMESPACE + ":" + path,
                resolvedClassName,
                "properties",
                0,
                0
        );
    }

    private static NativeBlockConstructorBinding powerCable(String path, int capacity, int transferRate) {
        return new NativeBlockConstructorBinding(
                NAMESPACE + ":" + path,
                "com.knoxhack.echoashfallprotocol.block.PowerCableBlock",
                "properties_int_int",
                capacity,
                transferRate
        );
    }

    private static NativeIntegrationHook hook(String role, String reportKey, String className, String methodName) {
        return new NativeIntegrationHook(role, reportKey, className, methodName);
    }

    private static NativeInfoModuleStaticInvocation staticInvocation(
            String reportKey,
            String className,
            String methodName
    ) {
        return new NativeInfoModuleStaticInvocation(reportKey, className, methodName);
    }

    private static NativeInfoModuleStaticValueInvocation staticValueInvocation(
            String reportKey,
            String className,
            String methodName
    ) {
        return new NativeInfoModuleStaticValueInvocation(reportKey, className, methodName);
    }

    private static NativeInfoModuleStaticFieldValue staticField(
            String reportKey,
            String className,
            String fieldName
    ) {
        return new NativeInfoModuleStaticFieldValue(reportKey, className, fieldName);
    }

    private static NativeInfoModuleStaticFieldArgumentInvocation staticFieldArgumentInvocation(
            String reportKey,
            String className,
            String methodName,
            String fieldValueKey
    ) {
        return new NativeInfoModuleStaticFieldArgumentInvocation(reportKey, className, methodName, fieldValueKey);
    }

    private static NativeUiSurfaceRoute surface(
            String surface,
            String screenId,
            String canonicalId,
            String target
    ) {
        return new NativeUiSurfaceRoute(surface, screenId, canonicalId, target);
    }

    private static NativePhysicalActionRoute physicalAction(
            String key,
            String surface,
            String action,
            boolean contextual
    ) {
        return new NativePhysicalActionRoute(key, surface, action, contextual);
    }
}
