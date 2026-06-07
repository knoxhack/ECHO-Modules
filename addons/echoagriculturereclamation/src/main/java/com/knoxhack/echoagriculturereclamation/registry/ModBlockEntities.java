package com.knoxhack.echoagriculturereclamation.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoagriculturereclamation.EchoAgricultureReclamation;
import com.knoxhack.echoagriculturereclamation.block.entity.HydroponicTrayBlockEntity;
import com.knoxhack.echoagriculturereclamation.block.entity.ReclamationCropBlockEntity;
import com.knoxhack.echoagriculturereclamation.block.entity.ReclamationMachineBlockEntity;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
   private static final Object BLOCK_ENTITIES =
      EchoBackendRegistryBridge.create(Registries.BLOCK_ENTITY_TYPE, EchoAgricultureReclamation.MODID);

   public static final EchoBackendRegistryEntry<BlockEntityType<HydroponicTrayBlockEntity>> HYDROPONIC_TRAY =
      EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "hydroponic_tray", () -> new BlockEntityType<>(HydroponicTrayBlockEntity::new, Set.of((Block)ModBlocks.HYDROPONIC_TRAY.get())));

   public static final EchoBackendRegistryEntry<BlockEntityType<ReclamationCropBlockEntity>> CROP =
      EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "crop", () -> new BlockEntityType<>(ReclamationCropBlockEntity::new, cropBlocks()));

   public static final EchoBackendRegistryEntry<BlockEntityType<ReclamationMachineBlockEntity>> MACHINE =
      EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "machine", () -> new BlockEntityType<>(ReclamationMachineBlockEntity::new, machineBlocks()));

   private ModBlockEntities() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(BLOCK_ENTITIES, eventBus);
   }

   private static Set<Block> cropBlocks() {
      return ModBlocks.cropBlocks().stream().map(block -> (Block)block.get()).collect(Collectors.toUnmodifiableSet());
   }

   private static Set<Block> machineBlocks() {
      return Set.of(
         (Block)ModBlocks.SEED_VAULT_TERMINAL.get(),
         (Block)ModBlocks.SOIL_PURIFIER.get(),
         (Block)ModBlocks.GENE_STABILIZER.get(),
         (Block)ModBlocks.BIO_REACTOR.get(),
         (Block)ModBlocks.GREENHOUSE_CONTROLLER.get(),
         (Block)ModBlocks.POLLINATOR_DRONE_DOCK.get(),
         (Block)ModBlocks.SPORE_FILTER.get(),
         (Block)ModBlocks.COMPOST_RECYCLER.get(),
         (Block)ModBlocks.ECOLOGY_SCANNER.get()
      );
   }
}
