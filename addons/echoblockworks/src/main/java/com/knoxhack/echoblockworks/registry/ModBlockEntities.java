package com.knoxhack.echoblockworks.registry;

import com.knoxhack.echoblockworks.EchoBlockworks;
import com.knoxhack.echoblockworks.block.entity.BlockworksTableBlockEntity;
import java.util.Set;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
   public static final NativeRegistryHolder<BlockEntityType<BlockworksTableBlockEntity>> BLOCKWORKS_TABLE =
      NativeRegistryHolder.of("blockworks_table", new BlockEntityType<>(BlockworksTableBlockEntity::new, Set.of(ModBlocks.BLOCKWORKS_TABLE.get())));

   private ModBlockEntities() {
   }

   public static void register() {
   }
}
