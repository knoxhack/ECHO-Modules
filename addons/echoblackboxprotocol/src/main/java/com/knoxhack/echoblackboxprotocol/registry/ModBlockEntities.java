package com.knoxhack.echoblackboxprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoblackboxprotocol.EchoBlackboxProtocol;
import com.knoxhack.echoblackboxprotocol.block.entity.BlackboxMachineBlockEntity;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
   private static final Object BLOCK_ENTITIES = EchoBackendRegistryBridge.create(Registries.BLOCK_ENTITY_TYPE, EchoBlackboxProtocol.MODID);

   public static final EchoBackendRegistryEntry<BlockEntityType<BlackboxMachineBlockEntity>> BLACKBOX_MACHINE = EchoBackendRegistryBridge.register(BLOCK_ENTITIES, 
      "blackbox_machine", () -> new BlockEntityType<>(BlackboxMachineBlockEntity::new, machineBlocks())
   );

   private ModBlockEntities() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(BLOCK_ENTITIES, eventBus);
   }

   private static Set<Block> machineBlocks() {
      return Set.of(
         ModBlocks.BLACKBOX_DECODER.get(),
         ModBlocks.MEMORY_PROJECTOR.get(),
         ModBlocks.ARCHIVE_TERMINAL.get(),
         ModBlocks.CORE_KEY_ASSEMBLER.get(),
         ModBlocks.TRUTH_ENGINE.get(),
         ModBlocks.MEMORY_STABILIZER.get(),
         ModBlocks.PROTOCOL_EXTRACTOR.get()
      );
   }
}
