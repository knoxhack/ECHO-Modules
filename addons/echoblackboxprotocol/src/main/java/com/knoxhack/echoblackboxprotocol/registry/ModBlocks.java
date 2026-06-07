package com.knoxhack.echoblackboxprotocol.registry;

import net.minecraft.core.registries.Registries;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoblackboxprotocol.block.BlackboxMachineBlock;
import com.knoxhack.echoblackboxprotocol.block.BlackboxMonolithBlock;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxDungeon;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxMachineKind;
import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
   public static final Object BLOCKS = EchoBackendRegistryBridge.create(Registries.BLOCK, "echoblackboxprotocol");
   public static final EchoBackendRegistryEntry<Block> CORE_BRICK = stone("core_brick", MapColor.COLOR_BLACK);
   public static final EchoBackendRegistryEntry<Block> SIGNAL_GLASS = glass("signal_glass", MapColor.COLOR_LIGHT_BLUE);
   public static final EchoBackendRegistryEntry<Block> BLACK_METAL_BLOCK = metal("black_metal_block", MapColor.COLOR_BLACK);
   public static final EchoBackendRegistryEntry<Block> CORRUPTED_FERRITE_BLOCK = metal("corrupted_ferrite_block", MapColor.COLOR_PURPLE);
   public static final EchoBackendRegistryEntry<Block> BLACKBOX_DECODER = machine("blackbox_decoder", BlackboxMachineKind.BLACKBOX_DECODER, MapColor.COLOR_BLUE);
   public static final EchoBackendRegistryEntry<Block> MEMORY_PROJECTOR = machine("memory_projector", BlackboxMachineKind.MEMORY_PROJECTOR, MapColor.COLOR_LIGHT_BLUE);
   public static final EchoBackendRegistryEntry<Block> ARCHIVE_TERMINAL = machine("archive_terminal", BlackboxMachineKind.ARCHIVE_TERMINAL, MapColor.COLOR_GRAY);
   public static final EchoBackendRegistryEntry<Block> CORE_KEY_ASSEMBLER = machine("core_key_assembler", BlackboxMachineKind.CORE_KEY_ASSEMBLER, MapColor.COLOR_CYAN);
   public static final EchoBackendRegistryEntry<Block> TRUTH_ENGINE = machine("truth_engine", BlackboxMachineKind.TRUTH_ENGINE, MapColor.COLOR_PURPLE);
   public static final EchoBackendRegistryEntry<Block> MEMORY_STABILIZER = machine("memory_stabilizer", BlackboxMachineKind.MEMORY_STABILIZER, MapColor.COLOR_LIGHT_BLUE);
   public static final EchoBackendRegistryEntry<Block> PROTOCOL_EXTRACTOR = machine("protocol_extractor", BlackboxMachineKind.PROTOCOL_EXTRACTOR, MapColor.COLOR_RED);
   public static final EchoBackendRegistryEntry<Block> VAULT_MONOLITH = monolith("vault_monolith", BlackboxDungeon.VAULT);
   public static final EchoBackendRegistryEntry<Block> BUNKER_MONOLITH = monolith("bunker_monolith", BlackboxDungeon.BUNKER);
   public static final EchoBackendRegistryEntry<Block> LABYRINTH_MONOLITH = monolith("labyrinth_monolith", BlackboxDungeon.LABYRINTH);
   public static final EchoBackendRegistryEntry<Block> TEMPLE_MONOLITH = monolith("temple_monolith", BlackboxDungeon.TEMPLE);
   public static final EchoBackendRegistryEntry<Block> CORE_CHAMBER_MONOLITH = monolith("core_chamber_monolith", BlackboxDungeon.CORE_CHAMBER);
   public static final List<EchoBackendRegistryEntry<Block>> ALL_BLOCKS = List.of(
      CORE_BRICK,
      SIGNAL_GLASS,
      BLACK_METAL_BLOCK,
      CORRUPTED_FERRITE_BLOCK,
      BLACKBOX_DECODER,
      MEMORY_PROJECTOR,
      ARCHIVE_TERMINAL,
      CORE_KEY_ASSEMBLER,
      TRUTH_ENGINE,
      MEMORY_STABILIZER,
      PROTOCOL_EXTRACTOR,
      VAULT_MONOLITH,
      BUNKER_MONOLITH,
      LABYRINTH_MONOLITH,
      TEMPLE_MONOLITH,
      CORE_CHAMBER_MONOLITH
   );

   private ModBlocks() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(BLOCKS, eventBus);
   }

   private static EchoBackendRegistryEntry<Block> machine(String name, BlackboxMachineKind kind, MapColor color) {
      return EchoBackendRegistryBridge.registerBlock(BLOCKS, 
         name,
         properties -> new BlackboxMachineBlock(kind, properties),
         p -> p.mapColor(color).strength(5.0F, 10.0F).sound(SoundType.METAL).lightLevel(state -> 3)
      );
   }

   private static EchoBackendRegistryEntry<Block> monolith(String name, BlackboxDungeon dungeon) {
      return EchoBackendRegistryBridge.registerBlock(BLOCKS, 
         name,
         properties -> new BlackboxMonolithBlock(dungeon, properties),
         p -> p.mapColor(MapColor.COLOR_BLACK).strength(8.0F, 20.0F).sound(SoundType.ANCIENT_DEBRIS).lightLevel(state -> 5)
      );
   }

   private static EchoBackendRegistryEntry<Block> metal(String name, MapColor color) {
      return EchoBackendRegistryBridge.registerSimpleBlock(BLOCKS, name, p -> p.mapColor(color).strength(4.5F, 9.0F).sound(SoundType.METAL));
   }

   private static EchoBackendRegistryEntry<Block> stone(String name, MapColor color) {
      return EchoBackendRegistryBridge.registerSimpleBlock(BLOCKS, name, p -> p.mapColor(color).strength(3.0F, 8.0F).sound(SoundType.DEEPSLATE));
   }

   private static EchoBackendRegistryEntry<Block> glass(String name, MapColor color) {
      return EchoBackendRegistryBridge.registerSimpleBlock(BLOCKS, 
         name, p -> p.mapColor(color).strength(0.8F, 1.5F).sound(SoundType.GLASS).noOcclusion().isValidSpawn((state, level, pos, entityType) -> false)
      );
   }
}
