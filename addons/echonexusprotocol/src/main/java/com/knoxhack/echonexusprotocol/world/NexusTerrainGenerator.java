package com.knoxhack.echonexusprotocol.world;

import com.knoxhack.echonexusprotocol.registry.ModBlocks;
import com.knoxhack.echonexusprotocol.registry.ModWorldgen;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.blending.Blender;

public class NexusTerrainGenerator extends ChunkGenerator {
   public static final MapCodec<NexusTerrainGenerator> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.biomeSource))
         .apply(instance, NexusTerrainGenerator::new)
   );
   private static final int BASE_Y = 50;

   public NexusTerrainGenerator(BiomeSource biomeSource) {
      super(biomeSource);
   }

   protected MapCodec<? extends ChunkGenerator> codec() {
      return (MapCodec<? extends ChunkGenerator>)ModWorldgen.NEXUS_TERRAIN.get();
   }

   public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState random, StructureManager structureManager, ChunkAccess chunk) {
      ChunkPos chunkPos = chunk.getPos();
      Heightmap ocean = chunk.getOrCreateHeightmapUnprimed(Types.OCEAN_FLOOR_WG);
      Heightmap surface = chunk.getOrCreateHeightmapUnprimed(Types.WORLD_SURFACE_WG);
      MutableBlockPos pos = new MutableBlockPos();

      for (int localX = 0; localX < 16; localX++) {
         for (int localZ = 0; localZ < 16; localZ++) {
            int worldX = chunkPos.getBlockX(localX);
            int worldZ = chunkPos.getBlockZ(localZ);
            int top = topHeight(worldX, worldZ);
            if (top > 50) {
               for (int y = 50; y <= top; y++) {
                  BlockState state = stateFor(worldX, y, worldZ, top);
                  pos.set(localX, y, localZ);
                  chunk.setBlockState(pos, state);
                  ocean.update(localX, y, localZ, state);
                  surface.update(localX, y, localZ, state);
               }
            }
         }
      }

      return CompletableFuture.completedFuture(chunk);
   }

   public void buildSurface(WorldGenRegion region, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
   }

   public void applyCarvers(
      WorldGenRegion region, long seed, RandomState random, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk
   ) {
   }

   public void spawnOriginalMobs(WorldGenRegion region) {
   }

   public int getGenDepth() {
      return 384;
   }

   public int getMinY() {
      return 0;
   }

   public int getSeaLevel() {
      return -63;
   }

   public int getSpawnHeight(LevelHeightAccessor level) {
      return 76;
   }

   public int getBaseHeight(int x, int z, Types type, LevelHeightAccessor level, RandomState random) {
      return Math.max(level.getMinY(), topHeight(x, z) + 1);
   }

   public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
      BlockState[] states = new BlockState[level.getHeight()];
      int top = topHeight(x, z);

      for (int y = 50; y <= top && y - level.getMinY() < states.length; y++) {
         int index = y - level.getMinY();
         if (index >= 0) {
            states[index] = stateFor(x, y, z, top);
         }
      }

      for (int i = 0; i < states.length; i++) {
         if (states[i] == null) {
            states[i] = Blocks.AIR.defaultBlockState();
         }
      }

      return new NoiseColumn(level.getMinY(), states);
   }

   public void addDebugScreenInfo(List<String> lines, RandomState random, BlockPos pos) {
      lines.add("ECHO-7 Nexus Protocol terrain");
   }

   public static int topHeight(int x, int z) {
      return !island(x, z) && !bridge(x, z) ? 49 : 72 + wave(x, z) + (bridge(x, z) ? -3 : 0);
   }

   private static BlockState stateFor(int x, int y, int z, int top) {
      if (y == top) {
         return switch (Math.floorMod(x + z, 5)) {
            case 0 -> ((Block)ModBlocks.FRAGMENTED_SOIL.get()).defaultBlockState();
            case 1 -> ((Block)ModBlocks.DATA_CRACKED_STONE.get()).defaultBlockState();
            case 2 -> ((Block)ModBlocks.GLASSED_DUST.get()).defaultBlockState();
            default -> ((Block)ModBlocks.SIGNAL_BURNED_GRASS.get()).defaultBlockState();
         };
      } else {
         return Math.floorMod(x * 3 + y + z * 5, 17) == 0
            ? ((Block)ModBlocks.NEXUS_CRYSTAL_CLUSTER.get()).defaultBlockState()
            : ((Block)ModBlocks.BLACKBOX_DEEPSLATE.get()).defaultBlockState();
      }
   }

   public static String landmarkType(ChunkPos chunkPos) {
      return "";
   }

   private static boolean island(int x, int z) {
      int cx = Math.floorMod(x, 59) - 29;
      int cz = Math.floorMod(z, 59) - 29;
      return cx * cx + cz * cz < 460;
   }

   private static boolean bridge(int x, int z) {
      return Math.abs(Math.floorMod(x + z, 59) - 29) < 2 || Math.abs(Math.floorMod(x - z, 59) - 29) < 2;
   }

   private static int wave(int x, int z) {
      return Math.floorMod(x * 734287 + z * 912271 + x * z * 13, 11) - 5;
   }
}
