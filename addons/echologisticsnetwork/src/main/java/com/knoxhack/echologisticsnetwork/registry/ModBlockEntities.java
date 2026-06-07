package com.knoxhack.echologisticsnetwork.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echologisticsnetwork.EchoLogisticsNetwork;
import com.knoxhack.echologisticsnetwork.block.entity.LogisticsBlockEntity;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
   private static final Object BLOCK_ENTITIES =
      EchoBackendRegistryBridge.create(Registries.BLOCK_ENTITY_TYPE, EchoLogisticsNetwork.MODID);

   public static final EchoBackendRegistryEntry<BlockEntityType<LogisticsBlockEntity>> LOGISTICS =
      EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "logistics",
         () -> new BlockEntityType(LogisticsBlockEntity::new, logisticsBlocks()));

   private ModBlockEntities() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(BLOCK_ENTITIES, eventBus);
   }

   private static Set<Block> logisticsBlocks() {
      return Set.of(
         ModBlocks.LOGISTICS_TERMINAL.get(),
         ModBlocks.SUPPLY_CRATE.get(),
         ModBlocks.SMART_STORAGE_LABEL.get(),
         ModBlocks.DRONE_DELIVERY_DOCK.get(),
         ModBlocks.ROUTE_REQUESTER.get(),
         ModBlocks.LOADOUT_LOCKER.get(),
         ModBlocks.FACTION_TRADE_DEPOT.get(),
         ModBlocks.REMOTE_REWARD_RELAY.get(),
         ModBlocks.AUTO_RESTOCK_STATION.get()
      );
   }
}
