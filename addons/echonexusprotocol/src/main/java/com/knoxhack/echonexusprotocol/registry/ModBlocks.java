package com.knoxhack.echonexusprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echonexusprotocol.EchoNexusProtocol;
import com.knoxhack.echonexusprotocol.block.BlackboxMonolithCoreBlock;
import com.knoxhack.echonexusprotocol.block.NexusMachineBlock;
import com.knoxhack.echonexusprotocol.block.ProtocolSealBlock;
import com.knoxhack.echonexusprotocol.block.RealityTearBlock;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
   public static final Object BLOCKS = EchoBackendRegistryBridge.create(BuiltInRegistries.BLOCK, EchoNexusProtocol.MODID);
   public static final EchoBackendRegistryEntry<Block> FRAGMENTED_SOIL = dust("fragmented_soil", MapColor.COLOR_PURPLE);
   public static final EchoBackendRegistryEntry<Block> DATA_CRACKED_STONE = stone("data_cracked_stone", MapColor.COLOR_BLUE);
   public static final EchoBackendRegistryEntry<Block> SIGNAL_BURNED_GRASS = dust("signal_burned_grass", MapColor.COLOR_LIGHT_BLUE);
   public static final EchoBackendRegistryEntry<Block> GLASSED_DUST = glass("glassed_dust", MapColor.TERRACOTTA_LIGHT_GRAY);
   public static final EchoBackendRegistryEntry<Block> HOLLOW_SIGNAL_WOOD = wood("hollow_signal_wood", MapColor.TERRACOTTA_WHITE);
   public static final EchoBackendRegistryEntry<Block> DEAD_SIGNAL_LEAVES = leaves("dead_signal_leaves", MapColor.COLOR_LIGHT_BLUE);
   public static final EchoBackendRegistryEntry<Block> CORRUPTED_FERRITE_ORE = stone("corrupted_ferrite_ore", MapColor.COLOR_PURPLE);
   public static final EchoBackendRegistryEntry<Block> BLACKBOX_DEEPSLATE = stone("blackbox_deepslate", MapColor.COLOR_BLACK);
   public static final EchoBackendRegistryEntry<Block> STATIC_FLUID_BLOCK = simpleBlock(
      "static_fluid_block", p -> p.mapColor(MapColor.COLOR_PURPLE).strength(100.0F).sound(SoundType.SLIME_BLOCK).noOcclusion()
   );
   public static final EchoBackendRegistryEntry<Block> NEXUS_CRYSTAL_CLUSTER = simpleBlock(
      "nexus_crystal_cluster",
      p -> p.mapColor(MapColor.COLOR_CYAN)
         .strength(0.8F, 1.5F)
         .sound(SoundType.GLASS)
         .noOcclusion()
         .isValidSpawn((state, level, pos, entityType) -> false)
         .lightLevel(state -> 12)
   );
   public static final EchoBackendRegistryEntry<Block> BLACKBOX_PLATE = metal("blackbox_plate", MapColor.COLOR_BLACK);
   public static final EchoBackendRegistryEntry<Block> SIGNAL_GLASS = glass("signal_glass", MapColor.COLOR_LIGHT_BLUE);
   public static final EchoBackendRegistryEntry<Block> CORE_BRICK = metal("core_brick", MapColor.COLOR_BLACK);
   public static final EchoBackendRegistryEntry<Block> CORE_GLASS_BLOCK = glass("core_glass_block", MapColor.COLOR_CYAN);
   public static final EchoBackendRegistryEntry<Block> WHITE_SIGNAL_LOG = wood("white_signal_log", MapColor.TERRACOTTA_WHITE);
   public static final EchoBackendRegistryEntry<Block> WHITE_SIGNAL_LEAVES = leaves("white_signal_leaves", MapColor.SNOW);
   public static final EchoBackendRegistryEntry<Block> BLACKBOX_MONOLITH_CORE = customBlock("blackbox_monolith_core", BlackboxMonolithCoreBlock::new, p -> p.mapColor(MapColor.COLOR_BLACK).strength(8.0F, 16.0F).sound(SoundType.METAL).lightLevel(state -> 10));
   public static final EchoBackendRegistryEntry<Block> REALITY_TEAR = customBlock(
      "reality_tear",
      RealityTearBlock::new,
      p -> p.mapColor(MapColor.COLOR_PURPLE).strength(-1.0F, 3600000.0F).sound(SoundType.AMETHYST).noOcclusion().lightLevel(state -> 12)
   );
   public static final EchoBackendRegistryEntry<Block> NEXUS_RECYCLER = machine("nexus_recycler", NexusMachineBlock.MachineKind.NEXUS_RECYCLER, MapColor.COLOR_BLUE);
   public static final EchoBackendRegistryEntry<Block> NEXUS_CHARGE_TANK = machine(
      "nexus_charge_tank", NexusMachineBlock.MachineKind.NEXUS_CHARGE_TANK, MapColor.COLOR_CYAN
   );
   public static final EchoBackendRegistryEntry<Block> CORRUPTION_FILTER = machine(
      "corruption_filter", NexusMachineBlock.MachineKind.CORRUPTION_FILTER, MapColor.COLOR_LIGHT_BLUE
   );
   public static final EchoBackendRegistryEntry<Block> NEXUS_FIELD_STABILIZER = machine(
      "nexus_field_stabilizer", NexusMachineBlock.MachineKind.NEXUS_FIELD_STABILIZER, MapColor.COLOR_BLUE
   );
   public static final EchoBackendRegistryEntry<Block> NEXUS_INFUSER = machine("nexus_infuser", NexusMachineBlock.MachineKind.NEXUS_INFUSER, MapColor.COLOR_PURPLE);
   public static final EchoBackendRegistryEntry<Block> MEMORY_DECODER = machine("memory_decoder", NexusMachineBlock.MachineKind.MEMORY_DECODER, MapColor.COLOR_LIGHT_BLUE);
   public static final EchoBackendRegistryEntry<Block> REALITY_FORGE = machine("reality_forge", NexusMachineBlock.MachineKind.REALITY_FORGE, MapColor.COLOR_BLACK);
   public static final EchoBackendRegistryEntry<Block> CORRUPTION_REACTOR = machine(
      "corruption_reactor", NexusMachineBlock.MachineKind.CORRUPTION_REACTOR, MapColor.COLOR_PURPLE
   );
   public static final EchoBackendRegistryEntry<Block> PROTOCOL_SEAL = customBlock(
      "protocol_seal", ProtocolSealBlock::new, p -> p.mapColor(MapColor.COLOR_CYAN).strength(0.2F).sound(SoundType.GLASS).noOcclusion().lightLevel(state -> 8)
   );
   public static final List<EchoBackendRegistryEntry<Block>> MACHINE_BLOCKS = List.of(
      NEXUS_RECYCLER, NEXUS_CHARGE_TANK, CORRUPTION_FILTER, NEXUS_FIELD_STABILIZER, NEXUS_INFUSER, MEMORY_DECODER, REALITY_FORGE, CORRUPTION_REACTOR
   );
   public static final List<EchoBackendRegistryEntry<Block>> ALL_BLOCKS = List.of(
      FRAGMENTED_SOIL,
      DATA_CRACKED_STONE,
      SIGNAL_BURNED_GRASS,
      GLASSED_DUST,
      HOLLOW_SIGNAL_WOOD,
      DEAD_SIGNAL_LEAVES,
      CORRUPTED_FERRITE_ORE,
      BLACKBOX_DEEPSLATE,
      STATIC_FLUID_BLOCK,
      NEXUS_CRYSTAL_CLUSTER,
      BLACKBOX_PLATE,
      SIGNAL_GLASS,
      CORE_BRICK,
      CORE_GLASS_BLOCK,
      WHITE_SIGNAL_LOG,
      WHITE_SIGNAL_LEAVES,
      BLACKBOX_MONOLITH_CORE,
      REALITY_TEAR,
      NEXUS_RECYCLER,
      NEXUS_CHARGE_TANK,
      CORRUPTION_FILTER,
      NEXUS_FIELD_STABILIZER,
      NEXUS_INFUSER,
      MEMORY_DECODER,
      REALITY_FORGE,
      CORRUPTION_REACTOR,
      PROTOCOL_SEAL
   );

   private ModBlocks() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(BLOCKS, eventBus);
   }

   public static BlockState corruptedVariant(BlockState state) {
      if (state.is(net.minecraft.world.level.block.Blocks.DIRT) || state.is(net.minecraft.world.level.block.Blocks.COARSE_DIRT)) {
         return ((Block)FRAGMENTED_SOIL.get()).defaultBlockState();
      } else if (state.is(net.minecraft.world.level.block.Blocks.STONE)) {
         return ((Block)DATA_CRACKED_STONE.get()).defaultBlockState();
      } else if (state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
         return ((Block)SIGNAL_BURNED_GRASS.get()).defaultBlockState();
      } else if (state.is(net.minecraft.world.level.block.Blocks.SAND) || state.is(net.minecraft.world.level.block.Blocks.RED_SAND)) {
         return ((Block)GLASSED_DUST.get()).defaultBlockState();
      } else if (state.is(net.minecraft.world.level.block.Blocks.WATER)) {
         return ((Block)STATIC_FLUID_BLOCK.get()).defaultBlockState();
      } else if (state.is(net.minecraft.world.level.block.Blocks.OAK_LOG)
         || state.is(net.minecraft.world.level.block.Blocks.BIRCH_LOG)
         || state.is(net.minecraft.world.level.block.Blocks.SPRUCE_LOG)) {
         return ((Block)HOLLOW_SIGNAL_WOOD.get()).defaultBlockState();
      } else if (state.is(net.minecraft.world.level.block.Blocks.OAK_LEAVES)
         || state.is(net.minecraft.world.level.block.Blocks.BIRCH_LEAVES)
         || state.is(net.minecraft.world.level.block.Blocks.SPRUCE_LEAVES)) {
         return ((Block)DEAD_SIGNAL_LEAVES.get()).defaultBlockState();
      } else if (state.is(net.minecraft.world.level.block.Blocks.IRON_ORE)) {
         return ((Block)CORRUPTED_FERRITE_ORE.get()).defaultBlockState();
      } else {
         return state.is(net.minecraft.world.level.block.Blocks.DEEPSLATE) ? ((Block)BLACKBOX_DEEPSLATE.get()).defaultBlockState() : null;
      }
   }

   public static BlockState cleanVariant(BlockState state) {
      if (state.is((Block)FRAGMENTED_SOIL.get())) {
         return net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState();
      } else if (state.is((Block)DATA_CRACKED_STONE.get())) {
         return net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
      } else if (state.is((Block)SIGNAL_BURNED_GRASS.get())) {
         return net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState();
      } else if (state.is((Block)GLASSED_DUST.get())) {
         return net.minecraft.world.level.block.Blocks.SAND.defaultBlockState();
      } else if (state.is((Block)STATIC_FLUID_BLOCK.get())) {
         return net.minecraft.world.level.block.Blocks.WATER.defaultBlockState();
      } else if (state.is((Block)HOLLOW_SIGNAL_WOOD.get())) {
         return net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState();
      } else if (state.is((Block)DEAD_SIGNAL_LEAVES.get())) {
         return net.minecraft.world.level.block.Blocks.OAK_LEAVES.defaultBlockState();
      } else if (state.is((Block)CORRUPTED_FERRITE_ORE.get())) {
         return net.minecraft.world.level.block.Blocks.IRON_ORE.defaultBlockState();
      } else {
         return state.is((Block)BLACKBOX_DEEPSLATE.get()) ? net.minecraft.world.level.block.Blocks.DEEPSLATE.defaultBlockState() : null;
      }
   }

   private static EchoBackendRegistryEntry<Block> machine(String name, NexusMachineBlock.MachineKind kind, MapColor color) {
      return customBlock(
         name, properties -> new NexusMachineBlock(kind, properties), p -> p.mapColor(color).strength(4.0F, 8.0F).sound(SoundType.METAL).lightLevel(state -> 4)
      );
   }

   private static EchoBackendRegistryEntry<Block> metal(String name, MapColor color) {
      return simpleBlock(name, p -> p.mapColor(color).strength(4.0F, 8.0F).sound(SoundType.METAL));
   }

   private static EchoBackendRegistryEntry<Block> stone(String name, MapColor color) {
      return simpleBlock(name, p -> p.mapColor(color).strength(2.8F, 7.0F).sound(SoundType.STONE));
   }

   private static EchoBackendRegistryEntry<Block> dust(String name, MapColor color) {
      return simpleBlock(name, p -> p.mapColor(color).strength(0.7F).sound(SoundType.SAND));
   }

   private static EchoBackendRegistryEntry<Block> wood(String name, MapColor color) {
      return simpleBlock(name, p -> p.mapColor(color).strength(2.0F).sound(SoundType.WOOD));
   }

   private static EchoBackendRegistryEntry<Block> leaves(String name, MapColor color) {
      return simpleBlock(name, p -> p.mapColor(color).strength(0.2F).sound(SoundType.GRASS).noOcclusion());
   }

   private static EchoBackendRegistryEntry<Block> glass(String name, MapColor color) {
      return simpleBlock(
         name, p -> p.mapColor(color).strength(0.8F, 1.5F).sound(SoundType.GLASS).noOcclusion().isValidSpawn((state, level, pos, entityType) -> false)
      );
   }

   private static EchoBackendRegistryEntry<Block> simpleBlock(String name, UnaryOperator<BlockBehaviour.Properties> properties) {
      return customBlock(name, Block::new, properties);
   }

   private static <T extends Block> EchoBackendRegistryEntry<T> customBlock(String name,
         Function<BlockBehaviour.Properties, T> factory, UnaryOperator<BlockBehaviour.Properties> propertyMutator) {
      return EchoBackendRegistryBridge.registerWithId(BLOCKS, name, id -> factory.apply(propertyMutator.apply(properties(id))));
   }

   private static BlockBehaviour.Properties properties(Identifier id) {
      return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id));
   }
}
