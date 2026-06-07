package com.knoxhack.echoindustrialnexus.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoindustrialnexus.EchoIndustrialNexus;
import com.knoxhack.echoindustrialnexus.block.entity.IndustrialFluxDuctBlockEntity;
import com.knoxhack.echoindustrialnexus.block.entity.IndustrialFluidPipeBlockEntity;
import com.knoxhack.echoindustrialnexus.block.entity.IndustrialItemDuctBlockEntity;
import com.knoxhack.echoindustrialnexus.block.entity.IndustrialMachineBlockEntity;
import com.knoxhack.echoindustrialnexus.block.entity.IndustrialMultiblockControllerBlockEntity;
import com.knoxhack.echoindustrialnexus.block.entity.IndustrialMultiblockCrateBlockEntity;
import com.knoxhack.echoindustrialnexus.block.entity.IndustrialRoboticArmMountBlockEntity;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
   private static final Object BLOCK_ENTITIES = EchoBackendRegistryBridge.create(Registries.BLOCK_ENTITY_TYPE, EchoIndustrialNexus.MODID);
   public static final EchoBackendRegistryEntry<BlockEntityType<IndustrialMachineBlockEntity>> INDUSTRIAL_MACHINE = EchoBackendRegistryBridge.register(BLOCK_ENTITIES, 
      "industrial_machine", () -> new BlockEntityType(IndustrialMachineBlockEntity::new, machineBlocks())
   );
   public static final EchoBackendRegistryEntry<BlockEntityType<IndustrialFluxDuctBlockEntity>> FLUX_DUCT = EchoBackendRegistryBridge.register(BLOCK_ENTITIES, 
      "flux_duct", () -> new BlockEntityType(IndustrialFluxDuctBlockEntity::new, fluxDuctBlocks())
   );
   public static final EchoBackendRegistryEntry<BlockEntityType<IndustrialItemDuctBlockEntity>> ITEM_DUCT = EchoBackendRegistryBridge.register(BLOCK_ENTITIES, 
      "item_duct", () -> new BlockEntityType(IndustrialItemDuctBlockEntity::new, itemDuctBlocks())
   );
   public static final EchoBackendRegistryEntry<BlockEntityType<IndustrialFluidPipeBlockEntity>> FLUID_PIPE = EchoBackendRegistryBridge.register(BLOCK_ENTITIES, 
      "fluid_pipe", () -> new BlockEntityType(IndustrialFluidPipeBlockEntity::new, fluidPipeBlocks())
   );
   public static final EchoBackendRegistryEntry<BlockEntityType<IndustrialMultiblockControllerBlockEntity>> INDUSTRIAL_MULTIBLOCK_CONTROLLER =
      EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "industrial_multiblock_controller", () -> new BlockEntityType<>(
         IndustrialMultiblockControllerBlockEntity::new, industrialMultiblockControllerBlocks()));
   public static final EchoBackendRegistryEntry<BlockEntityType<IndustrialMultiblockCrateBlockEntity>> INDUSTRIAL_MULTIBLOCK_CRATE =
      EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "industrial_multiblock_crate", () -> new BlockEntityType<>(
         IndustrialMultiblockCrateBlockEntity::new, industrialMultiblockCrateBlocks()));
   public static final EchoBackendRegistryEntry<BlockEntityType<IndustrialRoboticArmMountBlockEntity>> INDUSTRIAL_ROBOTIC_ARM =
      EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "industrial_robotic_arm", () -> new BlockEntityType<>(
         IndustrialRoboticArmMountBlockEntity::new, Set.of((Block)ModBlocks.ROBOTIC_ARM_MOUNT.get())));

   private ModBlockEntities() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(BLOCK_ENTITIES, eventBus);
   }

   private static Set<Block> machineBlocks() {
      return Set.of(
         (Block)ModBlocks.SCRAP_DYNAMO.get(),
         (Block)ModBlocks.THERMAL_ARRAY.get(),
         (Block)ModBlocks.GEOTHERMAL_PUMP.get(),
         (Block)ModBlocks.REACTOR_HEAT_EXCHANGER.get(),
         (Block)ModBlocks.SOLAR_CONCENTRATOR.get(),
         (Block)ModBlocks.STATIC_HEAT_EXCHANGER.get(),
         (Block)ModBlocks.FURNACE_WARDEN_CORE.get(),
         (Block)ModBlocks.ORE_GRINDER.get(),
         (Block)ModBlocks.SALVAGE_SHREDDER.get(),
         (Block)ModBlocks.ALLOY_KILN.get(),
         (Block)ModBlocks.SUBSTRATE_GRINDER.get(),
         (Block)ModBlocks.FLUID_REFINER.get(),
         (Block)ModBlocks.WATER_PURIFIER.get(),
         (Block)ModBlocks.FILTER_PRESS.get(),
         (Block)ModBlocks.COMPONENT_ASSEMBLER.get(),
         (Block)ModBlocks.INDUSTRIAL_RECYCLER.get(),
         (Block)ModBlocks.CORRUPTION_SAFE_RECYCLER.get(),
         (Block)ModBlocks.NEXUS_THERMAL_INJECTOR.get(),
         (Block)ModBlocks.REALITY_FURNACE.get(),
         (Block)ModBlocks.FACTORY_CONTROLLER.get(),
         (Block)ModBlocks.FLUX_CAPACITOR_BANK.get(),
         (Block)ModBlocks.REINFORCED_CAPACITOR.get(),
         (Block)ModBlocks.STABILIZED_FLUX_BANK.get(),
         (Block)ModBlocks.HYBRID_FLUX_BANK.get(),
         (Block)ModBlocks.CORE_FLUX_BANK.get(),
         (Block)ModBlocks.INDUSTRIAL_SCRUBBER.get()
      );
   }

   private static Set<Block> itemDuctBlocks() {
      return Set.of(
         (Block)ModBlocks.SCRAP_DUCT.get(),
         (Block)ModBlocks.REINFORCED_DUCT.get(),
         (Block)ModBlocks.SMART_DUCT.get(),
         (Block)ModBlocks.VACUUM_DUCT.get(),
         (Block)ModBlocks.NEXUS_SAFE_DUCT.get()
      );
   }

   private static Set<Block> fluxDuctBlocks() {
      return Set.of(
         (Block)ModBlocks.COPPER_FLUX_DUCT.get(),
         (Block)ModBlocks.REINFORCED_FLUX_DUCT.get(),
         (Block)ModBlocks.STABILIZED_FLUX_DUCT.get(),
         (Block)ModBlocks.HYBRID_FLUX_DUCT.get(),
         (Block)ModBlocks.CORE_FLUX_DUCT.get()
      );
   }

   private static Set<Block> fluidPipeBlocks() {
      return Set.of(
         (Block)ModBlocks.RUSTED_PIPE.get(),
         (Block)ModBlocks.REINFORCED_PIPE.get(),
         (Block)ModBlocks.PRESSURIZED_PIPE.get(),
         (Block)ModBlocks.SHIELDED_PIPE.get(),
         (Block)ModBlocks.STATIC_PIPE.get()
      );
   }

   private static Set<Block> industrialMultiblockControllerBlocks() {
      return Set.of(
         (Block)ModBlocks.INDUSTRIAL_CONTROLLER.get(),
         (Block)ModBlocks.INDUSTRIAL_ASSEMBLY_LINE_CONTROLLER.get(),
         (Block)ModBlocks.NEXUS_FURNACE_ARRAY_CONTROLLER.get(),
         (Block)ModBlocks.RECIPE_MATRIX_CORE.get(),
         (Block)ModBlocks.SCRAP_PROCESSOR_CONTROLLER.get(),
         (Block)ModBlocks.PLATE_PRESS_CONTROLLER.get(),
         (Block)ModBlocks.CIRCUIT_FABRICATOR_CONTROLLER.get(),
         (Block)ModBlocks.MACHINE_FRAME_ASSEMBLER_CONTROLLER.get(),
         (Block)ModBlocks.COOLING_STATION_CONTROLLER.get(),
         (Block)ModBlocks.INSPECTION_SCANNER_CONTROLLER.get()
      );
   }

   private static Set<Block> industrialMultiblockCrateBlocks() {
      return Set.of(
         (Block)ModBlocks.INPUT_DEPOT_CRATE.get(),
         (Block)ModBlocks.OUTPUT_DEPOT_CRATE.get()
      );
   }
}
