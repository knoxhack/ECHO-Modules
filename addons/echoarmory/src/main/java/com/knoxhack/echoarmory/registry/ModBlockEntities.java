package com.knoxhack.echoarmory.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoarmory.EchoArmory;
import com.knoxhack.echoarmory.block.entity.ArmoryStationBlockEntity;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
   private static final Object BLOCK_ENTITIES =
      EchoBackendRegistryBridge.create(Registries.BLOCK_ENTITY_TYPE, EchoArmory.MODID);

   public static final EchoBackendRegistryEntry<BlockEntityType<ArmoryStationBlockEntity>> ARMORY_STATION =
      EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "armory_station",
         () -> new BlockEntityType(ArmoryStationBlockEntity::new, armoryBlocks()));

   private ModBlockEntities() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(BLOCK_ENTITIES, eventBus);
   }

   private static Set<Block> armoryBlocks() {
      return Set.of(
         ModBlocks.ARMORY_BENCH.get(),
         ModBlocks.WEAPON_FORGE.get(),
         ModBlocks.ARMOR_FORGE.get(),
         ModBlocks.ENERGY_CORE_CHARGING_STATION.get(),
         ModBlocks.MODULE_UPGRADE_TABLE.get(),
         ModBlocks.SIGIL_ENGRAVER.get(),
         ModBlocks.LOADOUT_TERMINAL.get(),
         ModBlocks.WEAPON_RACK.get(),
         ModBlocks.ARMOR_STAND.get(),
         ModBlocks.VEIL_INFUSER.get(),
         ModBlocks.CONSTRUCT_DOCK.get()
      );
   }
}
