package com.knoxhack.echoblockworks.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoblockworks.EchoBlockworks;
import com.knoxhack.echoblockworks.block.BlockworksDecorativeBlock;
import com.knoxhack.echoblockworks.block.BlockworksSlabBlock;
import com.knoxhack.echoblockworks.block.BlockworksStairBlock;
import com.knoxhack.echoblockworks.block.BlockworksTableBlock;
import com.knoxhack.echoblockworks.block.BlockworksWallBlock;
import com.knoxhack.echoblockworks.block.CeilingStripBlock;
import com.knoxhack.echoblockworks.block.DirectionalDetailBlock;
import com.knoxhack.echoblockworks.block.LowDetailBlock;
import com.knoxhack.echoblockworks.block.SparkingDetailBlock;
import com.knoxhack.echoblockworks.content.BlockworksBlockInfo;
import com.knoxhack.echoblockworks.content.BlockworksCatalog;
import com.knoxhack.echoblockworks.content.BlockworksDetailKind;
import com.knoxhack.echoblockworks.content.BlockworksDetailSpec;
import com.knoxhack.echoblockworks.content.BlockworksShapeKind;
import com.knoxhack.echoblockworks.content.BlockworksTheme;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
   public static final Object BLOCKS = EchoBackendRegistryBridge.create(BuiltInRegistries.BLOCK, EchoBlockworks.MODID);
   private static final Map<String, NativeRegistryHolder<Block>> BY_ID = new LinkedHashMap<>();
   private static final List<NativeRegistryHolder<Block>> BLOCKWORKS_BLOCKS = new ArrayList<>();
   private static final List<NativeRegistryHolder<Block>> CREATIVE_BLOCKS = new ArrayList<>();
   private static final Map<String, NativeRegistryHolder<Block>> DETAIL_BLOCKS = new LinkedHashMap<>();

   public static final NativeRegistryHolder<Block> BLOCKWORKS_TABLE;

   static {
      for (BlockworksBlockInfo info : BlockworksCatalog.blockInfos()) {
         NativeRegistryHolder<Block> block = register(info.blockId(), id -> createBlock(info, propertiesFor(info, id)));
         BY_ID.put(info.blockId(), block);
         BLOCKWORKS_BLOCKS.add(block);
         CREATIVE_BLOCKS.add(block);
      }
      for (BlockworksDetailSpec detail : BlockworksCatalog.details()) {
         NativeRegistryHolder<Block> block = register(detail.id(), id -> createDetailBlock(detail, propertiesFor(detail, id)));
         BY_ID.put(detail.id(), block);
         DETAIL_BLOCKS.put(detail.id(), block);
         CREATIVE_BLOCKS.add(block);
      }
      BLOCKWORKS_TABLE = register("echo_blockworks_table", id -> new BlockworksTableBlock(baseProperties(id)
         .mapColor(MapColor.METAL)
         .strength(3.5F, 8.0F)
         .sound(SoundType.METAL)));
      BY_ID.put("echo_blockworks_table", BLOCKWORKS_TABLE);
      CREATIVE_BLOCKS.add(BLOCKWORKS_TABLE);
   }

   private ModBlocks() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(BLOCKS, eventBus);
   }

   public static NativeRegistryHolder<Block> blockFor(BlockworksBlockInfo info) {
      NativeRegistryHolder<Block> block = BY_ID.get(info.blockId());
      if (block == null) {
         throw new IllegalArgumentException("Unknown Blockworks block " + info.blockId());
      }
      return block;
   }

   public static java.util.Optional<NativeRegistryHolder<Block>> blockForId(String id) {
      return java.util.Optional.ofNullable(BY_ID.get(id));
   }

   public static List<NativeRegistryHolder<Block>> blockworksBlocks() {
      return List.copyOf(BLOCKWORKS_BLOCKS);
   }

   public static List<NativeRegistryHolder<Block>> creativeBlocks() {
      return List.copyOf(CREATIVE_BLOCKS);
   }

   public static List<NativeRegistryHolder<Block>> detailBlocks() {
      return List.copyOf(DETAIL_BLOCKS.values());
   }

   public static String idOf(Block block) {
      return BuiltInRegistries.BLOCK.getKey(block).getPath();
   }

   private static Block createBlock(BlockworksBlockInfo info, BlockBehaviour.Properties properties) {
      return switch (info.shape()) {
         case FULL -> new BlockworksDecorativeBlock(info, properties);
         case SLAB -> new BlockworksSlabBlock(info, properties);
         case STAIRS -> new BlockworksStairBlock(info, properties);
         case WALL -> new BlockworksWallBlock(info, properties);
      };
   }

   private static Block createDetailBlock(BlockworksDetailSpec detail, BlockBehaviour.Properties properties) {
      if ("sparking_cable_panel".equals(detail.id())) {
         return new SparkingDetailBlock(properties);
      }
      return switch (detail.kind()) {
         case FULL -> new Block(properties);
         case DIRECTIONAL_FULL -> new DirectionalDetailBlock(false, properties);
         case WALL_MOUNTED -> new DirectionalDetailBlock(true, properties);
         case CEILING_STRIP -> new CeilingStripBlock(properties);
         case FLOOR_LOW -> new LowDetailBlock(properties);
      };
   }

   private static NativeRegistryHolder<Block> register(String id, java.util.function.Function<Identifier, Block> factory) {
      EchoBackendRegistryEntry<Block> entry = EchoBackendRegistryBridge.registerWithId(BLOCKS, id, factory);
      return NativeRegistryHolder.deferred(id, entry);
   }

   private static BlockBehaviour.Properties baseProperties(Identifier id) {
      return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id));
   }

   private static BlockBehaviour.Properties propertiesFor(BlockworksBlockInfo info, Identifier id) {
      BlockBehaviour.Properties properties = baseProperties(id)
         .mapColor(colorFor(info.family().theme()))
         .requiresCorrectToolForDrops();
      if (info.variant().glassLike() || info.family().theme() == BlockworksTheme.RECLAMATION) {
         properties.strength(0.8F, 1.5F).sound(SoundType.GLASS).noOcclusion()
            .isValidSpawn((state, level, pos, entityType) -> false);
      } else if (info.family().theme() == BlockworksTheme.RUINED) {
         properties.strength(2.5F, 6.0F).sound(SoundType.STONE);
      } else if (info.family().theme() == BlockworksTheme.NEXUS) {
         properties.strength(2.0F, 6.0F).sound(SoundType.AMETHYST);
      } else {
         properties.strength(4.0F, 8.0F).sound(SoundType.METAL);
      }
      if (info.variant().light() > 0) {
         properties.lightLevel(state -> info.variant().light());
      }
      return properties;
   }

   private static BlockBehaviour.Properties propertiesFor(BlockworksDetailSpec detail, Identifier id) {
      BlockBehaviour.Properties properties = baseProperties(id)
         .mapColor(colorFor(detail.theme()))
         .strength(detail.kind() == BlockworksDetailKind.FLOOR_LOW ? 0.6F : 2.0F, 3.0F)
         .sound(detail.theme() == BlockworksTheme.RUINED ? SoundType.STONE : SoundType.METAL)
         .noOcclusion();
      if (detail.light() > 0) {
         properties.lightLevel(state -> detail.light());
      }
      return properties;
   }

   private static MapColor colorFor(BlockworksTheme theme) {
      return switch (theme) {
         case INDUSTRIAL, ECHO_TECH, TERMINAL, CYBERGLASS, ORBITAL, BLACKBOX, ARCHIVE, HAZARD -> MapColor.METAL;
         case RUINED -> MapColor.COLOR_GRAY;
         case NEXUS -> MapColor.COLOR_PURPLE;
         case RECLAMATION, LAB -> MapColor.PLANT;
         case CONVOY -> MapColor.TERRACOTTA_YELLOW;
      };
   }
}
