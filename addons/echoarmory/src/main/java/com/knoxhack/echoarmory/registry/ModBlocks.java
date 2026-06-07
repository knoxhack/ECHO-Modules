package com.knoxhack.echoarmory.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoarmory.EchoArmory;
import com.knoxhack.echoarmory.block.ArmoryStationBlock;
import com.knoxhack.echoarmory.block.ArmoryStationBlock.StationKind;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
   public static final Object BLOCKS = EchoBackendRegistryBridge.create(BuiltInRegistries.BLOCK, EchoArmory.MODID);

   public static final EchoBackendRegistryEntry<Block> ARMORY_BENCH = station(StationKind.ARMORY_BENCH, MapColor.COLOR_GRAY);
   public static final EchoBackendRegistryEntry<Block> WEAPON_FORGE = station(StationKind.WEAPON_FORGE, MapColor.TERRACOTTA_RED);
   public static final EchoBackendRegistryEntry<Block> ARMOR_FORGE = station(StationKind.ARMOR_FORGE, MapColor.COLOR_LIGHT_BLUE);
   public static final EchoBackendRegistryEntry<Block> ENERGY_CORE_CHARGING_STATION = station(StationKind.ENERGY_CORE_CHARGING_STATION, MapColor.COLOR_PURPLE);
   public static final EchoBackendRegistryEntry<Block> MODULE_UPGRADE_TABLE = station(StationKind.MODULE_UPGRADE_TABLE, MapColor.COLOR_CYAN);
   public static final EchoBackendRegistryEntry<Block> SIGIL_ENGRAVER = station(StationKind.SIGIL_ENGRAVER, MapColor.GOLD);
   public static final EchoBackendRegistryEntry<Block> LOADOUT_TERMINAL = station(StationKind.LOADOUT_TERMINAL, MapColor.COLOR_BLACK);
   public static final EchoBackendRegistryEntry<Block> WEAPON_RACK = station(StationKind.WEAPON_RACK, MapColor.WOOD);
   public static final EchoBackendRegistryEntry<Block> ARMOR_STAND = station(StationKind.ARMOR_STAND, MapColor.METAL);
   public static final EchoBackendRegistryEntry<Block> VEIL_INFUSER = station(StationKind.VEIL_INFUSER, MapColor.COLOR_PURPLE);
   public static final EchoBackendRegistryEntry<Block> CONSTRUCT_DOCK = station(StationKind.CONSTRUCT_DOCK, MapColor.COLOR_GREEN);

   public static final List<EchoBackendRegistryEntry<Block>> ALL_BLOCKS = List.of(
      ARMORY_BENCH,
      WEAPON_FORGE,
      ARMOR_FORGE,
      ENERGY_CORE_CHARGING_STATION,
      MODULE_UPGRADE_TABLE,
      SIGIL_ENGRAVER,
      LOADOUT_TERMINAL,
      WEAPON_RACK,
      ARMOR_STAND,
      VEIL_INFUSER,
      CONSTRUCT_DOCK
   );

   private ModBlocks() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(BLOCKS, eventBus);
   }

   private static EchoBackendRegistryEntry<Block> station(StationKind kind, MapColor color) {
      return EchoBackendRegistryBridge.registerWithId(BLOCKS, kind.getSerializedName(), id -> new ArmoryStationBlock(
         kind,
         BlockBehaviour.Properties.of()
            .setId(ResourceKey.create(Registries.BLOCK, id))
            .mapColor(color)
            .strength(3.5F, 7.0F)
            .sound(SoundType.METAL)
            .noOcclusion()));
   }
}
