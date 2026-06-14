package com.knoxhack.echoblockworks.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoblockworks.EchoBlockworks;
import com.knoxhack.echoblockworks.block.entity.BlockworksTableBlockEntity;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
   private static final Object BLOCK_ENTITIES = EchoBackendRegistryBridge.create(Registries.BLOCK_ENTITY_TYPE, EchoBlockworks.MODID);

   public static final NativeRegistryHolder<BlockEntityType<BlockworksTableBlockEntity>> BLOCKWORKS_TABLE =
      register("blockworks_table", () -> new BlockEntityType<>(BlockworksTableBlockEntity::new, Set.of(ModBlocks.BLOCKWORKS_TABLE.get())));

   private ModBlockEntities() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(BLOCK_ENTITIES, eventBus);
   }

   private static <T extends BlockEntityType<?>> NativeRegistryHolder<T> register(String id, java.util.function.Supplier<T> factory) {
      EchoBackendRegistryEntry<T> entry = EchoBackendRegistryBridge.register(BLOCK_ENTITIES, id, factory);
      return NativeRegistryHolder.deferred(id, entry);
   }
}
