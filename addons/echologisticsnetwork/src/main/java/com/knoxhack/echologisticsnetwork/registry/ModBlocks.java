package com.knoxhack.echologisticsnetwork.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echologisticsnetwork.EchoLogisticsNetwork;
import com.knoxhack.echologisticsnetwork.block.LogisticsBlock;
import com.knoxhack.echologisticsnetwork.block.LogisticsBlock.LogisticsKind;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
   public static final Object BLOCKS = EchoBackendRegistryBridge.create(BuiltInRegistries.BLOCK, EchoLogisticsNetwork.MODID);

   public static final EchoBackendRegistryEntry<Block> LOGISTICS_TERMINAL = logistics(LogisticsKind.LOGISTICS_TERMINAL, MapColor.COLOR_CYAN, 4.0F);
   public static final EchoBackendRegistryEntry<Block> SUPPLY_CRATE = logistics(LogisticsKind.SUPPLY_CRATE, MapColor.WOOD, 2.5F);
   public static final EchoBackendRegistryEntry<Block> SMART_STORAGE_LABEL = logistics(LogisticsKind.SMART_STORAGE_LABEL, MapColor.COLOR_LIGHT_BLUE, 1.0F);
   public static final EchoBackendRegistryEntry<Block> DRONE_DELIVERY_DOCK = logistics(LogisticsKind.DRONE_DELIVERY_DOCK, MapColor.METAL, 4.0F);
   public static final EchoBackendRegistryEntry<Block> ROUTE_REQUESTER = logistics(LogisticsKind.ROUTE_REQUESTER, MapColor.COLOR_GRAY, 3.0F);
   public static final EchoBackendRegistryEntry<Block> LOADOUT_LOCKER = logistics(LogisticsKind.LOADOUT_LOCKER, MapColor.COLOR_GREEN, 3.0F);
   public static final EchoBackendRegistryEntry<Block> FACTION_TRADE_DEPOT = logistics(LogisticsKind.FACTION_TRADE_DEPOT, MapColor.GOLD, 3.5F);
   public static final EchoBackendRegistryEntry<Block> REMOTE_REWARD_RELAY = logistics(LogisticsKind.REMOTE_REWARD_RELAY, MapColor.COLOR_PURPLE, 3.0F);
   public static final EchoBackendRegistryEntry<Block> AUTO_RESTOCK_STATION = logistics(LogisticsKind.AUTO_RESTOCK_STATION, MapColor.COLOR_ORANGE, 3.0F);

   public static final List<EchoBackendRegistryEntry<Block>> ALL_BLOCKS = List.of(
      LOGISTICS_TERMINAL,
      SUPPLY_CRATE,
      SMART_STORAGE_LABEL,
      DRONE_DELIVERY_DOCK,
      ROUTE_REQUESTER,
      LOADOUT_LOCKER,
      FACTION_TRADE_DEPOT,
      REMOTE_REWARD_RELAY,
      AUTO_RESTOCK_STATION
   );

   private ModBlocks() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(BLOCKS, eventBus);
   }

   private static EchoBackendRegistryEntry<Block> logistics(LogisticsKind kind, MapColor color, float strength) {
      return EchoBackendRegistryBridge.registerWithId(BLOCKS, kind.getSerializedName(), id -> {
         BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
            .setId(ResourceKey.create(Registries.BLOCK, id))
            .mapColor(color)
            .strength(strength, strength * 2.0F)
            .sound(SoundType.METAL)
            .noOcclusion();
         return new LogisticsBlock(kind, properties);
      });
   }
}
