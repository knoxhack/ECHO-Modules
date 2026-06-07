package com.knoxhack.echoashfallprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.block.entity.*;
import com.knoxhack.echoashfallprotocol.block.entity.DeepCoreMinerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

public class ModBlockEntities {
    public static final Object BLOCK_ENTITIES =
            EchoBackendRegistryBridge.create(Registries.BLOCK_ENTITY_TYPE, EchoAshfallProtocol.MODID);

    public static final EchoBackendRegistryEntry<BlockEntityType<HandRecyclerBlockEntity>> HAND_RECYCLER =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "hand_recycler",
                    () -> new BlockEntityType<>(HandRecyclerBlockEntity::new, Set.of(ModBlocks.HAND_RECYCLER.get())));

    public static final EchoBackendRegistryEntry<BlockEntityType<ThermalBurnerBlockEntity>> THERMAL_BURNER =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "thermal_burner",
                    () -> new BlockEntityType<>(ThermalBurnerBlockEntity::new, Set.of(ModBlocks.THERMAL_BURNER.get())));

    public static final EchoBackendRegistryEntry<BlockEntityType<WaterPurifierBlockEntity>> WATER_PURIFIER =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "water_purifier",
                    () -> new BlockEntityType<>(WaterPurifierBlockEntity::new, Set.of(ModBlocks.WATER_PURIFIER.get())));

    public static final EchoBackendRegistryEntry<BlockEntityType<RainCollectorBlockEntity>> RAIN_COLLECTOR =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "rain_collector",
                    () -> new BlockEntityType<>(RainCollectorBlockEntity::new, Set.of(ModBlocks.RAIN_COLLECTOR.get())));

    public static final EchoBackendRegistryEntry<BlockEntityType<MicroGeneratorBlockEntity>> MICRO_GENERATOR =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "micro_generator",
                    () -> new BlockEntityType<>(MicroGeneratorBlockEntity::new, Set.of(ModBlocks.MICRO_GENERATOR.get())));

    // === TIER 2.5 POWER GENERATION (Machinery Expansion) ===
    public static final EchoBackendRegistryEntry<BlockEntityType<com.knoxhack.echoashfallprotocol.block.entity.ThermalArrayBlockEntity>> THERMAL_ARRAY =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "thermal_array",
                    () -> new BlockEntityType<>(com.knoxhack.echoashfallprotocol.block.entity.ThermalArrayBlockEntity::new, Set.of(ModBlocks.THERMAL_ARRAY.get())));

    // Orphan Machines - Phase 2 Implementation
    public static final EchoBackendRegistryEntry<BlockEntityType<BatteryBankBlockEntity>> BATTERY_BANK =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "battery_bank",
                    () -> new BlockEntityType<>(BatteryBankBlockEntity::new, Set.of(ModBlocks.BATTERY_BANK.get())));

    public static final EchoBackendRegistryEntry<BlockEntityType<ScrapDynamoBlockEntity>> SCRAP_DYNAMO =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "scrap_dynamo",
                    () -> new BlockEntityType<>(ScrapDynamoBlockEntity::new, Set.of(ModBlocks.SCRAP_DYNAMO.get())));

    public static final EchoBackendRegistryEntry<BlockEntityType<NexusCapacitorBlockEntity>> NEXUS_CAPACITOR =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "nexus_capacitor",
                    () -> new BlockEntityType<>(NexusCapacitorBlockEntity::new, Set.of(ModBlocks.NEXUS_CAPACITOR.get())));

    public static final EchoBackendRegistryEntry<BlockEntityType<LoadDistributorBlockEntity>> LOAD_DISTRIBUTOR =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "load_distributor",
                    () -> new BlockEntityType<>(LoadDistributorBlockEntity::new, Set.of(ModBlocks.LOAD_DISTRIBUTOR.get())));

    public static final EchoBackendRegistryEntry<BlockEntityType<ScrapPressBlockEntity>> SCRAP_PRESS =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "scrap_press",
                    () -> new BlockEntityType<>(ScrapPressBlockEntity::new, Set.of(ModBlocks.SCRAP_PRESS.get())));

    public static final EchoBackendRegistryEntry<BlockEntityType<SignalScannerBlockEntity>> SIGNAL_SCANNER =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "signal_scanner",
                    () -> new BlockEntityType<>(SignalScannerBlockEntity::new, Set.of(ModBlocks.SIGNAL_SCANNER.get())));

    public static final EchoBackendRegistryEntry<BlockEntityType<FieldMedBayBlockEntity>> FIELD_MED_BAY =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "field_med_bay",
                    () -> new BlockEntityType<>(FieldMedBayBlockEntity::new, Set.of(ModBlocks.FIELD_MED_BAY.get())));

    public static final EchoBackendRegistryEntry<BlockEntityType<AtmosphericScrubberBlockEntity>> ATMOSPHERIC_SCRUBBER =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "atmospheric_scrubber",
                    () -> new BlockEntityType<>(AtmosphericScrubberBlockEntity::new, Set.of(ModBlocks.ATMOSPHERIC_SCRUBBER.get())));

    public static final EchoBackendRegistryEntry<BlockEntityType<AutofeedHopperBlockEntity>> AUTOFEED_HOPPER =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "autofeed_hopper",
                    () -> new BlockEntityType<>(AutofeedHopperBlockEntity::new, Set.of(ModBlocks.AUTOFEED_HOPPER.get())));

    public static final EchoBackendRegistryEntry<BlockEntityType<ContaminantCondenserBlockEntity>> CONTAMINANT_CONDENSER =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "contaminant_condenser",
                    () -> new BlockEntityType<>(ContaminantCondenserBlockEntity::new, Set.of(ModBlocks.CONTAMINANT_CONDENSER.get())));

    public static final EchoBackendRegistryEntry<BlockEntityType<FilterWorkbenchBlockEntity>> FILTER_WORKBENCH =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "filter_workbench",
                    () -> new BlockEntityType<>(FilterWorkbenchBlockEntity::new, Set.of(ModBlocks.FILTER_WORKBENCH.get())));

    public static final EchoBackendRegistryEntry<BlockEntityType<PowerNodeBlockEntity>> POWER_NODE =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "power_node",
                    () -> new BlockEntityType<>(PowerNodeBlockEntity::new, Set.of(ModBlocks.POWER_NODE.get())));

    public static final EchoBackendRegistryEntry<BlockEntityType<NexusCoreBlockEntity>> NEXUS_CORE =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "nexus_core",
                    () -> new BlockEntityType<>(NexusCoreBlockEntity::new, Set.of(ModBlocks.NEXUS_CORE.get())));

    // === GEO-EXTRACTOR MACHINES ===
    public static final EchoBackendRegistryEntry<BlockEntityType<com.knoxhack.echoashfallprotocol.block.entity.OreGrinderBlockEntity>> ORE_GRINDER =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "ore_grinder",
                    () -> new BlockEntityType<>(com.knoxhack.echoashfallprotocol.block.entity.OreGrinderBlockEntity::new, Set.of(ModBlocks.ORE_GRINDER.get())));

    public static final EchoBackendRegistryEntry<BlockEntityType<com.knoxhack.echoashfallprotocol.block.entity.IsotopeRefinerBlockEntity>> ISOTOPE_REFINER =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "isotope_refiner",
                    () -> new BlockEntityType<>(com.knoxhack.echoashfallprotocol.block.entity.IsotopeRefinerBlockEntity::new, Set.of(ModBlocks.ISOTOPE_REFINER.get())));

    public static final EchoBackendRegistryEntry<BlockEntityType<com.knoxhack.echoashfallprotocol.block.entity.CrystallineSynthesizerBlockEntity>> CRYSTALLINE_SYNTHESIZER =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "crystalline_synthesizer",
                    () -> new BlockEntityType<>(com.knoxhack.echoashfallprotocol.block.entity.CrystallineSynthesizerBlockEntity::new, Set.of(ModBlocks.CRYSTALLINE_SYNTHESIZER.get())));

    // === ENDGAME MACHINES ===
    public static final EchoBackendRegistryEntry<BlockEntityType<DeepCoreMinerBlockEntity>> DEEP_CORE_MINER =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "deep_core_miner",
                    () -> new BlockEntityType<>(DeepCoreMinerBlockEntity::new, Set.of(ModBlocks.DEEP_CORE_MINER.get())));

    public static final EchoBackendRegistryEntry<BlockEntityType<com.knoxhack.echoashfallprotocol.block.entity.RadiationCleanserBlockEntity>> RADIATION_CLEANSER =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "radiation_cleanser",
                    () -> new BlockEntityType<>(com.knoxhack.echoashfallprotocol.block.entity.RadiationCleanserBlockEntity::new, Set.of(ModBlocks.RADIATION_CLEANSER.get())));

    // === MACHINE INTEGRATION ===
    public static final EchoBackendRegistryEntry<BlockEntityType<com.knoxhack.echoashfallprotocol.block.entity.ItemPipeBlockEntity>> ITEM_PIPE =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "item_pipe",
                    () -> new BlockEntityType<>(com.knoxhack.echoashfallprotocol.block.entity.ItemPipeBlockEntity::new, Set.of(ModBlocks.ITEM_PIPE.get())));

    public static final EchoBackendRegistryEntry<BlockEntityType<com.knoxhack.echoashfallprotocol.block.entity.PowerCableBlockEntity>> POWER_CABLE =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "power_cable",
                    () -> new BlockEntityType<>(com.knoxhack.echoashfallprotocol.block.entity.PowerCableBlockEntity::new, Set.of(
                            ModBlocks.POWER_CABLE.get(),
                            ModBlocks.REINFORCED_POWER_CABLE.get(),
                            ModBlocks.HIGH_VOLTAGE_POWER_CABLE.get())));

    public static final EchoBackendRegistryEntry<BlockEntityType<com.knoxhack.echoashfallprotocol.block.entity.FactoryControllerBlockEntity>> FACTORY_CONTROLLER =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "factory_controller",
                    () -> new BlockEntityType<>(com.knoxhack.echoashfallprotocol.block.entity.FactoryControllerBlockEntity::new, Set.of(ModBlocks.FACTORY_CONTROLLER.get())));

    public static final EchoBackendRegistryEntry<BlockEntityType<StructureCacheBlockEntity>> STRUCTURE_CACHE =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "structure_cache",
                    () -> new BlockEntityType<>(StructureCacheBlockEntity::new, Set.of(ModBlocks.STRUCTURE_CACHE.get())));

    public static final EchoBackendRegistryEntry<BlockEntityType<EchoContainerBlockEntity>> ECHO_CONTAINER =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "echo_container",
                    () -> new BlockEntityType<>(EchoContainerBlockEntity::new, Set.of(
                            ModBlocks.ECHO_CACHE.get(),
                            ModBlocks.ECHO_CRATE.get(),
                            ModBlocks.SUPPLY_CRATE.get())));
}
