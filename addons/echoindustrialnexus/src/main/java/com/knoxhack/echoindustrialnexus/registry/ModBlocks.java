package com.knoxhack.echoindustrialnexus.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echomultiblockcore.block.MultiblockCrateBlock;
import com.knoxhack.echoindustrialnexus.EchoIndustrialNexus;
import com.knoxhack.echoindustrialnexus.block.IndustrialFluxDuctBlock;
import com.knoxhack.echoindustrialnexus.block.IndustrialFluidPipeBlock;
import com.knoxhack.echoindustrialnexus.block.IndustrialItemDuctBlock;
import com.knoxhack.echoindustrialnexus.block.IndustrialMachineBlock;
import com.knoxhack.echoindustrialnexus.block.IndustrialMultiblockControllerBlock;
import com.knoxhack.echoindustrialnexus.block.IndustrialMultiblockCrateBlock;
import com.knoxhack.echoindustrialnexus.block.IndustrialRoboticArmMountBlock;
import com.knoxhack.echoindustrialnexus.multiblock.IndustrialMultiblockTasks;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
   public static final Object BLOCKS = EchoBackendRegistryBridge.create(BuiltInRegistries.BLOCK, EchoIndustrialNexus.MODID);
   public static final EchoBackendRegistryEntry<Block> SCRAP_DYNAMO = machine("scrap_dynamo", IndustrialMachineBlock.MachineKind.SCRAP_DYNAMO, MapColor.TERRACOTTA_ORANGE);
   public static final EchoBackendRegistryEntry<Block> THERMAL_ARRAY = machine("thermal_array", IndustrialMachineBlock.MachineKind.THERMAL_ARRAY, MapColor.COLOR_ORANGE);
   public static final EchoBackendRegistryEntry<Block> GEOTHERMAL_PUMP = machine(
      "geothermal_pump", IndustrialMachineBlock.MachineKind.GEOTHERMAL_PUMP, MapColor.TERRACOTTA_ORANGE
   );
   public static final EchoBackendRegistryEntry<Block> REACTOR_HEAT_EXCHANGER = machine(
      "reactor_heat_exchanger", IndustrialMachineBlock.MachineKind.REACTOR_HEAT_EXCHANGER, MapColor.COLOR_GREEN
   );
   public static final EchoBackendRegistryEntry<Block> SOLAR_CONCENTRATOR = machine(
      "solar_concentrator", IndustrialMachineBlock.MachineKind.SOLAR_CONCENTRATOR, MapColor.GOLD
   );
   public static final EchoBackendRegistryEntry<Block> STATIC_HEAT_EXCHANGER = machine(
      "static_heat_exchanger", IndustrialMachineBlock.MachineKind.STATIC_HEAT_EXCHANGER, MapColor.COLOR_PURPLE
   );
   public static final EchoBackendRegistryEntry<Block> FURNACE_WARDEN_CORE = machine(
      "furnace_warden_core", IndustrialMachineBlock.MachineKind.FURNACE_WARDEN_CORE, MapColor.TERRACOTTA_RED
   );
   public static final EchoBackendRegistryEntry<Block> ORE_GRINDER = machine("ore_grinder", IndustrialMachineBlock.MachineKind.ORE_GRINDER, MapColor.COLOR_GRAY);
   public static final EchoBackendRegistryEntry<Block> SALVAGE_SHREDDER = machine(
      "salvage_shredder", IndustrialMachineBlock.MachineKind.SALVAGE_SHREDDER, MapColor.COLOR_BLACK
   );
   public static final EchoBackendRegistryEntry<Block> ALLOY_KILN = machine("alloy_kiln", IndustrialMachineBlock.MachineKind.ALLOY_KILN, MapColor.TERRACOTTA_RED);
   public static final EchoBackendRegistryEntry<Block> SUBSTRATE_GRINDER = machine(
      "substrate_grinder", IndustrialMachineBlock.MachineKind.SUBSTRATE_GRINDER, MapColor.DIRT
   );
   public static final EchoBackendRegistryEntry<Block> FLUID_REFINER = machine("fluid_refiner", IndustrialMachineBlock.MachineKind.FLUID_REFINER, MapColor.COLOR_CYAN);
   public static final EchoBackendRegistryEntry<Block> WATER_PURIFIER = machine(
      "water_purifier", IndustrialMachineBlock.MachineKind.WATER_PURIFIER, MapColor.COLOR_LIGHT_BLUE
   );
   public static final EchoBackendRegistryEntry<Block> FILTER_PRESS = machine("filter_press", IndustrialMachineBlock.MachineKind.FILTER_PRESS, MapColor.COLOR_CYAN);
   public static final EchoBackendRegistryEntry<Block> COMPONENT_ASSEMBLER = machine(
      "component_assembler", IndustrialMachineBlock.MachineKind.COMPONENT_ASSEMBLER, MapColor.COLOR_LIGHT_BLUE
   );
   public static final EchoBackendRegistryEntry<Block> INDUSTRIAL_RECYCLER = machine(
      "industrial_recycler", IndustrialMachineBlock.MachineKind.INDUSTRIAL_RECYCLER, MapColor.COLOR_GREEN
   );
   public static final EchoBackendRegistryEntry<Block> CORRUPTION_SAFE_RECYCLER = machine(
      "corruption_safe_recycler", IndustrialMachineBlock.MachineKind.CORRUPTION_SAFE_RECYCLER, MapColor.COLOR_PURPLE
   );
   public static final EchoBackendRegistryEntry<Block> NEXUS_THERMAL_INJECTOR = machine(
      "nexus_thermal_injector", IndustrialMachineBlock.MachineKind.NEXUS_THERMAL_INJECTOR, MapColor.COLOR_PURPLE
   );
   public static final EchoBackendRegistryEntry<Block> REALITY_FURNACE = machine(
      "reality_furnace", IndustrialMachineBlock.MachineKind.REALITY_FURNACE, MapColor.COLOR_PURPLE
   );
   public static final EchoBackendRegistryEntry<Block> FACTORY_CONTROLLER = machine(
      "factory_controller", IndustrialMachineBlock.MachineKind.FACTORY_CONTROLLER, MapColor.COLOR_CYAN
   );
   public static final EchoBackendRegistryEntry<Block> FLUX_CAPACITOR_BANK = machine(
      "flux_capacitor_bank", IndustrialMachineBlock.MachineKind.FLUX_CAPACITOR_BANK, MapColor.COLOR_PURPLE
   );
   public static final EchoBackendRegistryEntry<Block> REINFORCED_CAPACITOR = machine(
      "reinforced_capacitor", IndustrialMachineBlock.MachineKind.REINFORCED_CAPACITOR, MapColor.COLOR_ORANGE
   );
   public static final EchoBackendRegistryEntry<Block> STABILIZED_FLUX_BANK = machine(
      "stabilized_flux_bank", IndustrialMachineBlock.MachineKind.STABILIZED_FLUX_BANK, MapColor.COLOR_CYAN
   );
   public static final EchoBackendRegistryEntry<Block> HYBRID_FLUX_BANK = machine(
      "hybrid_flux_bank", IndustrialMachineBlock.MachineKind.HYBRID_FLUX_BANK, MapColor.COLOR_PURPLE
   );
   public static final EchoBackendRegistryEntry<Block> CORE_FLUX_BANK = machine("core_flux_bank", IndustrialMachineBlock.MachineKind.CORE_FLUX_BANK, MapColor.COLOR_BLACK);
   public static final EchoBackendRegistryEntry<Block> INDUSTRIAL_SCRUBBER = machine(
      "industrial_scrubber", IndustrialMachineBlock.MachineKind.INDUSTRIAL_SCRUBBER, MapColor.PLANT
   );
   public static final EchoBackendRegistryEntry<Block> COPPER_FLUX_DUCT = fluxDuct("copper_flux_duct", "Copper Flux Duct", 256, MapColor.COLOR_ORANGE);
   public static final EchoBackendRegistryEntry<Block> REINFORCED_FLUX_DUCT = fluxDuct("reinforced_flux_duct", "Reinforced Flux Duct", 768, MapColor.COLOR_GRAY);
   public static final EchoBackendRegistryEntry<Block> STABILIZED_FLUX_DUCT = fluxDuct("stabilized_flux_duct", "Stabilized Flux Duct", 1536, MapColor.COLOR_CYAN);
   public static final EchoBackendRegistryEntry<Block> HYBRID_FLUX_DUCT = fluxDuct("hybrid_flux_duct", "Hybrid Flux Duct", 3072, MapColor.COLOR_PURPLE);
   public static final EchoBackendRegistryEntry<Block> CORE_FLUX_DUCT = fluxDuct("core_flux_duct", "Core Flux Duct", 8192, MapColor.COLOR_BLACK);
   public static final EchoBackendRegistryEntry<Block> SCRAP_DUCT = itemDuct("scrap_duct", "Scrap Duct", 24, false, false, MapColor.COLOR_GRAY);
   public static final EchoBackendRegistryEntry<Block> REINFORCED_DUCT = itemDuct("reinforced_duct", "Reinforced Duct", 14, false, false, MapColor.COLOR_GRAY);
   public static final EchoBackendRegistryEntry<Block> SMART_DUCT = itemDuct("smart_duct", "Smart Duct", 10, false, false, MapColor.COLOR_CYAN);
   public static final EchoBackendRegistryEntry<Block> VACUUM_DUCT = itemDuct("vacuum_duct", "Vacuum Duct", 12, true, false, MapColor.COLOR_BLACK);
   public static final EchoBackendRegistryEntry<Block> NEXUS_SAFE_DUCT = itemDuct("nexus_safe_duct", "Nexus-Safe Duct", 10, false, true, MapColor.COLOR_PURPLE);
   public static final EchoBackendRegistryEntry<Block> RUSTED_PIPE = fluidPipe("rusted_pipe", IndustrialFluidPipeBlock.PipeTier.RUSTED, MapColor.TERRACOTTA_ORANGE);
   public static final EchoBackendRegistryEntry<Block> REINFORCED_PIPE = fluidPipe("reinforced_pipe", IndustrialFluidPipeBlock.PipeTier.REINFORCED, MapColor.COLOR_GRAY);
   public static final EchoBackendRegistryEntry<Block> PRESSURIZED_PIPE = fluidPipe("pressurized_pipe", IndustrialFluidPipeBlock.PipeTier.PRESSURIZED, MapColor.COLOR_LIGHT_BLUE);
   public static final EchoBackendRegistryEntry<Block> SHIELDED_PIPE = fluidPipe("shielded_pipe", IndustrialFluidPipeBlock.PipeTier.SHIELDED, MapColor.COLOR_GREEN);
   public static final EchoBackendRegistryEntry<Block> STATIC_PIPE = fluidPipe("static_pipe", IndustrialFluidPipeBlock.PipeTier.STATIC, MapColor.COLOR_PURPLE);
   public static final EchoBackendRegistryEntry<Block> SIGNAL_DUCT = metal("signal_duct", MapColor.COLOR_CYAN);
   public static final EchoBackendRegistryEntry<Block> REINFORCED_FACTORY_WALL = metal("reinforced_factory_wall", MapColor.COLOR_GRAY);
   public static final EchoBackendRegistryEntry<Block> RUSTED_CATWALK = metal("rusted_catwalk", MapColor.TERRACOTTA_ORANGE);
   public static final EchoBackendRegistryEntry<Block> INDUSTRIAL_GRATE = metal("industrial_grate", MapColor.COLOR_GRAY);
   public static final EchoBackendRegistryEntry<Block> WARNING_STRIPE_BLOCK = metal("warning_stripe_block", MapColor.GOLD);
   public static final EchoBackendRegistryEntry<Block> ASHPROOF_GLASS = glass("ashproof_glass", MapColor.COLOR_GRAY);
   public static final EchoBackendRegistryEntry<Block> REINFORCED_GLASS = glass("reinforced_glass", MapColor.COLOR_LIGHT_BLUE);
   public static final EchoBackendRegistryEntry<Block> MACHINE_CASING = metal("machine_casing", MapColor.METAL);
   public static final EchoBackendRegistryEntry<Block> THERMAL_COIL_BLOCK = metal("thermal_coil_block", MapColor.COLOR_ORANGE);
   public static final EchoBackendRegistryEntry<Block> VENT_BLOCK = metal("vent_block", MapColor.COLOR_GRAY);
   public static final EchoBackendRegistryEntry<Block> PIPE_WALL = metal("pipe_wall", MapColor.COLOR_GRAY);
   public static final EchoBackendRegistryEntry<Block> CONTROL_PANEL = metal("control_panel", MapColor.COLOR_CYAN);
   public static final EchoBackendRegistryEntry<Block> ECHO_MONITOR = glass("echo_monitor", MapColor.COLOR_CYAN);
   public static final EchoBackendRegistryEntry<Block> BROKEN_MONITOR = glass("broken_monitor", MapColor.COLOR_BLACK);
   public static final EchoBackendRegistryEntry<Block> FACTORY_LIGHT = glass("factory_light", MapColor.GOLD);
   public static final EchoBackendRegistryEntry<Block> EMERGENCY_LIGHT = glass("emergency_light", MapColor.TERRACOTTA_RED);
   public static final EchoBackendRegistryEntry<Block> HAZARD_DOOR = metal("hazard_door", MapColor.GOLD);
   public static final EchoBackendRegistryEntry<Block> MAINTENANCE_HATCH = metal("maintenance_hatch", MapColor.COLOR_GRAY);
   public static final EchoBackendRegistryEntry<Block> CONVEYOR_FLOOR = metal("conveyor_floor", MapColor.COLOR_BLACK);
   public static final EchoBackendRegistryEntry<Block> SMOKE_VENT = metal("smoke_vent", MapColor.COLOR_GRAY);
   public static final EchoBackendRegistryEntry<Block> PRESSURE_GAUGE_BLOCK = metal("pressure_gauge_block", MapColor.COLOR_CYAN);
   public static final EchoBackendRegistryEntry<Block> COOLING_FAN_BLOCK = metal("cooling_fan_block", MapColor.COLOR_GRAY);
   public static final EchoBackendRegistryEntry<Block> INDUSTRIAL_CONTROLLER = industrialController(
      "industrial_controller", "industrial_assembly_line", IndustrialMultiblockTasks.WELD_REINFORCED_MACHINE_FRAME
   );
   public static final EchoBackendRegistryEntry<Block> INDUSTRIAL_ASSEMBLY_LINE_CONTROLLER = industrialController(
      "industrial_assembly_line_controller", "industrial_assembly_line", IndustrialMultiblockTasks.WELD_REINFORCED_MACHINE_FRAME
   );
   public static final EchoBackendRegistryEntry<Block> RECIPE_MATRIX_CORE = industrialController(
      "recipe_matrix_core", "recipe_matrix_core", IndustrialMultiblockTasks.ENCODE_RECIPE_MATRIX_SHARD
   );
   public static final EchoBackendRegistryEntry<Block> NEXUS_FURNACE_ARRAY_CONTROLLER = industrialController(
      "nexus_furnace_array_controller", "nexus_furnace_array", IndustrialMultiblockTasks.STABILIZE_HYBRID_THERMAL_CORE
   );
   public static final EchoBackendRegistryEntry<Block> SCRAP_PROCESSOR_CONTROLLER = industrialController(
      "scrap_processor_controller", "scrap_processor", IndustrialMultiblockTasks.PROCESS_SCRAP_INTO_SCRAP_PLATE
   );
   public static final EchoBackendRegistryEntry<Block> PLATE_PRESS_CONTROLLER = industrialController(
      "plate_press_controller", "plate_press", IndustrialMultiblockTasks.PRESS_SCRAP_PLATE_INTO_REFINED_PLATE
   );
   public static final EchoBackendRegistryEntry<Block> CIRCUIT_FABRICATOR_CONTROLLER = industrialController(
      "circuit_fabricator_controller", "circuit_fabricator", IndustrialMultiblockTasks.ASSEMBLE_PRECISION_CIRCUIT
   );
   public static final EchoBackendRegistryEntry<Block> MACHINE_FRAME_ASSEMBLER_CONTROLLER = industrialController(
      "machine_frame_assembler_controller", "industrial_assembly_line", IndustrialMultiblockTasks.WELD_REINFORCED_MACHINE_FRAME
   );
   public static final EchoBackendRegistryEntry<Block> COOLING_STATION_CONTROLLER = industrialController(
      "cooling_station_controller", "recipe_matrix_core", IndustrialMultiblockTasks.ENCODE_RECIPE_MATRIX_SHARD
   );
   public static final EchoBackendRegistryEntry<Block> INSPECTION_SCANNER_CONTROLLER = industrialController(
      "inspection_scanner_controller", "circuit_fabricator", IndustrialMultiblockTasks.ASSEMBLE_PRECISION_CIRCUIT
   );
   public static final EchoBackendRegistryEntry<Block> INDUSTRIAL_POWER_BUS = metal("industrial_power_bus", MapColor.COLOR_ORANGE);
   public static final EchoBackendRegistryEntry<Block> INDUSTRIAL_ITEM_BUS = metal("industrial_item_bus", MapColor.COLOR_LIGHT_BLUE);
   public static final EchoBackendRegistryEntry<Block> INDUSTRIAL_FLUID_BUS = metal("industrial_fluid_bus", MapColor.COLOR_CYAN);
   public static final EchoBackendRegistryEntry<Block> INDUSTRIAL_DATA_BUS = metal("industrial_data_bus", MapColor.COLOR_CYAN);
   public static final EchoBackendRegistryEntry<Block> REINFORCED_MACHINE_CASING = metal("reinforced_machine_casing", MapColor.METAL);
   public static final EchoBackendRegistryEntry<Block> FACTORY_FLOOR_PANEL = metal("factory_floor_panel", MapColor.COLOR_BLACK);
   public static final EchoBackendRegistryEntry<Block> INDUSTRIAL_WORKCELL_FRAME = metal("industrial_workcell_frame", MapColor.COLOR_GRAY);
   public static final EchoBackendRegistryEntry<Block> ROBOTIC_ARM_MOUNT = custom(
      "robotic_arm_mount",
      IndustrialRoboticArmMountBlock::new,
      p -> p.mapColor(MapColor.COLOR_GRAY).strength(3.5F, 8.0F).sound(SoundType.METAL).noOcclusion()
   );
   public static final EchoBackendRegistryEntry<Block> INPUT_DEPOT_CRATE = custom(
      "input_depot_crate",
      properties -> new IndustrialMultiblockCrateBlock(MultiblockCrateBlock.CrateKind.INPUT, properties),
      p -> p.mapColor(MapColor.WOOD).strength(2.5F, 4.0F).sound(SoundType.WOOD)
   );
   public static final EchoBackendRegistryEntry<Block> OUTPUT_DEPOT_CRATE = custom(
      "output_depot_crate",
      properties -> new IndustrialMultiblockCrateBlock(MultiblockCrateBlock.CrateKind.OUTPUT, properties),
      p -> p.mapColor(MapColor.WOOD).strength(2.5F, 4.0F).sound(SoundType.WOOD)
   );
   public static final EchoBackendRegistryEntry<Block> INDUSTRIAL_STORAGE_CRATE = metal("industrial_storage_crate", MapColor.WOOD);
   public static final EchoBackendRegistryEntry<Block> COOLANT_TANK = glass("coolant_tank", MapColor.COLOR_CYAN);
   public static final EchoBackendRegistryEntry<Block> MATERIAL_HOPPER = metal("material_hopper", MapColor.COLOR_GRAY);
   public static final EchoBackendRegistryEntry<Block> ASSEMBLY_GANTRY_RAIL = metal("assembly_gantry_rail", MapColor.COLOR_GRAY);
   public static final EchoBackendRegistryEntry<Block> WARNING_LIGHT = glass("warning_light", MapColor.TERRACOTTA_RED);
   public static final EchoBackendRegistryEntry<Block> MACHINE_STATUS_PANEL = glass("machine_status_panel", MapColor.COLOR_CYAN);
   public static final List<EchoBackendRegistryEntry<Block>> ALL_BLOCKS = List.of(
      SCRAP_DYNAMO,
      THERMAL_ARRAY,
      GEOTHERMAL_PUMP,
      REACTOR_HEAT_EXCHANGER,
      SOLAR_CONCENTRATOR,
      STATIC_HEAT_EXCHANGER,
      FURNACE_WARDEN_CORE,
      ORE_GRINDER,
      SALVAGE_SHREDDER,
      ALLOY_KILN,
      SUBSTRATE_GRINDER,
      FLUID_REFINER,
      WATER_PURIFIER,
      FILTER_PRESS,
      COMPONENT_ASSEMBLER,
      INDUSTRIAL_RECYCLER,
      CORRUPTION_SAFE_RECYCLER,
      NEXUS_THERMAL_INJECTOR,
      REALITY_FURNACE,
      FACTORY_CONTROLLER,
      FLUX_CAPACITOR_BANK,
      REINFORCED_CAPACITOR,
      STABILIZED_FLUX_BANK,
      HYBRID_FLUX_BANK,
      CORE_FLUX_BANK,
      INDUSTRIAL_SCRUBBER,
      COPPER_FLUX_DUCT,
      REINFORCED_FLUX_DUCT,
      STABILIZED_FLUX_DUCT,
      HYBRID_FLUX_DUCT,
      CORE_FLUX_DUCT,
      SCRAP_DUCT,
      REINFORCED_DUCT,
      SMART_DUCT,
      VACUUM_DUCT,
      NEXUS_SAFE_DUCT,
      RUSTED_PIPE,
      REINFORCED_PIPE,
      PRESSURIZED_PIPE,
      SHIELDED_PIPE,
      STATIC_PIPE,
      SIGNAL_DUCT,
      REINFORCED_FACTORY_WALL,
      RUSTED_CATWALK,
      INDUSTRIAL_GRATE,
      WARNING_STRIPE_BLOCK,
      ASHPROOF_GLASS,
      REINFORCED_GLASS,
      MACHINE_CASING,
      THERMAL_COIL_BLOCK,
      VENT_BLOCK,
      PIPE_WALL,
      CONTROL_PANEL,
      ECHO_MONITOR,
      BROKEN_MONITOR,
      FACTORY_LIGHT,
      EMERGENCY_LIGHT,
      HAZARD_DOOR,
      MAINTENANCE_HATCH,
      CONVEYOR_FLOOR,
      SMOKE_VENT,
      PRESSURE_GAUGE_BLOCK,
      COOLING_FAN_BLOCK,
      INDUSTRIAL_CONTROLLER,
      INDUSTRIAL_ASSEMBLY_LINE_CONTROLLER,
      RECIPE_MATRIX_CORE,
      NEXUS_FURNACE_ARRAY_CONTROLLER,
      SCRAP_PROCESSOR_CONTROLLER,
      PLATE_PRESS_CONTROLLER,
      CIRCUIT_FABRICATOR_CONTROLLER,
      MACHINE_FRAME_ASSEMBLER_CONTROLLER,
      COOLING_STATION_CONTROLLER,
      INSPECTION_SCANNER_CONTROLLER,
      INDUSTRIAL_POWER_BUS,
      INDUSTRIAL_ITEM_BUS,
      INDUSTRIAL_FLUID_BUS,
      INDUSTRIAL_DATA_BUS,
      REINFORCED_MACHINE_CASING,
      FACTORY_FLOOR_PANEL,
      INDUSTRIAL_WORKCELL_FRAME,
      ROBOTIC_ARM_MOUNT,
      INPUT_DEPOT_CRATE,
      OUTPUT_DEPOT_CRATE,
      INDUSTRIAL_STORAGE_CRATE,
      COOLANT_TANK,
      MATERIAL_HOPPER,
      ASSEMBLY_GANTRY_RAIL,
      WARNING_LIGHT,
      MACHINE_STATUS_PANEL
   );

   private ModBlocks() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(BLOCKS, eventBus);
   }

   private static EchoBackendRegistryEntry<Block> machine(String name, IndustrialMachineBlock.MachineKind kind, MapColor color) {
      return custom(
         name, properties -> new IndustrialMachineBlock(kind, properties), p -> p.mapColor(color).strength(4.0F, 8.0F).sound(SoundType.METAL)
      );
   }

   private static EchoBackendRegistryEntry<Block> fluxDuct(String name, String displayName, int transferLimit, MapColor color) {
      return custom(
         name,
         properties -> new IndustrialFluxDuctBlock(displayName, transferLimit, properties),
         p -> p.mapColor(color).strength(2.0F, 5.0F).sound(SoundType.COPPER).noOcclusion()
      );
   }


   private static EchoBackendRegistryEntry<Block> itemDuct(String name, String displayName, int transferInterval, boolean vacuum, boolean nexusSafe, MapColor color) {
      return custom(
         name,
         properties -> new IndustrialItemDuctBlock(displayName, transferInterval, vacuum, nexusSafe, properties),
         p -> p.mapColor(color).strength(2.0F, 5.0F).sound(SoundType.COPPER).noOcclusion()
      );
   }

   private static EchoBackendRegistryEntry<Block> fluidPipe(String name, IndustrialFluidPipeBlock.PipeTier tier, MapColor color) {
      return custom(
         name,
         properties -> new IndustrialFluidPipeBlock(tier, properties),
         p -> p.mapColor(color).strength(2.0F, 5.0F).sound(SoundType.COPPER).noOcclusion()
      );
   }

   private static EchoBackendRegistryEntry<Block> industrialController(String name, String definitionPath, Identifier defaultTask) {
      return custom(
         name,
         properties -> new IndustrialMultiblockControllerBlock(EchoIndustrialNexus.id(definitionPath), defaultTask, properties),
         p -> p.mapColor(MapColor.COLOR_CYAN).strength(4.0F, 8.0F).sound(SoundType.METAL).noOcclusion()
      );
   }

   private static EchoBackendRegistryEntry<Block> metal(String name, MapColor color) {
      return custom(name, Block::new, p -> p.mapColor(color).strength(4.0F, 8.0F).sound(SoundType.METAL));
   }

   private static EchoBackendRegistryEntry<Block> glass(String name, MapColor color) {
      return custom(
         name,
         Block::new,
         p -> p.mapColor(color).strength(0.8F, 1.5F).sound(SoundType.GLASS).noOcclusion().isValidSpawn((state, level, pos, entityType) -> false)
      );
   }

   private static EchoBackendRegistryEntry<Block> custom(
      String name,
      Function<BlockBehaviour.Properties, ? extends Block> factory,
      Function<BlockBehaviour.Properties, BlockBehaviour.Properties> propertiesFactory
   ) {
      return EchoBackendRegistryBridge.registerWithId(
         BLOCKS,
         name,
         id -> factory.apply(withId(propertiesFactory.apply(BlockBehaviour.Properties.of()), id))
      );
   }

   private static BlockBehaviour.Properties withId(BlockBehaviour.Properties properties, Identifier id) {
      return properties.setId(ResourceKey.create(Registries.BLOCK, id));
   }
}
