package com.knoxhack.echonexusprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echonexusprotocol.EchoNexusProtocol;
import com.knoxhack.echonexusprotocol.block.entity.NexusMachineBlockEntity;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
   private static final Object BLOCK_ENTITIES = EchoBackendRegistryBridge.create(Registries.BLOCK_ENTITY_TYPE, EchoNexusProtocol.MODID);
   public static final EchoBackendRegistryEntry<BlockEntityType<NexusMachineBlockEntity>> NEXUS_MACHINE = EchoBackendRegistryBridge.register(
      BLOCK_ENTITIES,
      "nexus_machine",
      () -> new BlockEntityType(NexusMachineBlockEntity::new, machineBlocks())
   );

   private ModBlockEntities() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(BLOCK_ENTITIES, eventBus);
   }

   private static Set<Block> machineBlocks() {
      return Set.copyOf(ModBlocks.MACHINE_BLOCKS.stream().map(EchoBackendRegistryEntry::get).toList());
   }
}
