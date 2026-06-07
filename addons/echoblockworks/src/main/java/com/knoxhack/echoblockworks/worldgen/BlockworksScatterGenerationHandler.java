package com.knoxhack.echoblockworks.worldgen;

import com.knoxhack.echoblockworks.Config;
import com.knoxhack.echoblockworks.registry.ModBlocks;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

public final class BlockworksScatterGenerationHandler {
   private static final long SALT = 0xEC0B10C0FFEE2L;
   private static final List<String> SCATTER_BLOCKS = List.of(
      "ashstone_debris",
      "ashstone_cracked_brick",
      "charred_concrete_broken",
      "charred_concrete_rebar",
      "rusted_metal_cracked",
      "rusted_metal_dark_plate",
      "rubble_pile",
      "scattered_debris",
      "hanging_wire",
      "flickering_warning_light"
   );

   private BlockworksScatterGenerationHandler() {
   }

   public static void onNewChunkLoad(ServerLevel level, ChunkAccess chunk) {
      if (!Config.PROCEDURAL_SCATTER_ENABLED.get()) {
         return;
      }
      int chunkX = chunk.getPos().getWorldPosition().getX() >> 4;
      int chunkZ = chunk.getPos().getWorldPosition().getZ() >> 4;
      if (!selected(level, chunkX, chunkZ)) {
         return;
      }
      BlockPos center = chunk.getPos().getWorldPosition().offset(8, 0, 8);
      generate(level, center, new Random(level.getSeed() ^ center.asLong() ^ SALT));
   }

   public static boolean selected(ServerLevel level, int chunkX, int chunkZ) {
      int spacing = Math.max(12, Config.SCATTER_SPACING_CHUNKS.get());
      int regionX = Math.floorDiv(chunkX, spacing);
      int regionZ = Math.floorDiv(chunkZ, spacing);
      Random random = new Random(level.getSeed() + (long)regionX * 341873128712L + (long)regionZ * 132897987541L + SALT);
      int selectedX = regionX * spacing + random.nextInt(spacing);
      int selectedZ = regionZ * spacing + random.nextInt(spacing);
      return chunkX == selectedX && chunkZ == selectedZ;
   }

   public static int generate(ServerLevel level, BlockPos origin, Random random) {
      BlockPos base = surface(level, origin);
      if (!canGenerate(level, base)) {
         return 0;
      }
      int radius = Math.max(2, Config.SCATTER_SEARCH_RADIUS.get());
      int targetPieces = Math.max(1, Config.SCATTER_MAX_PIECES.get());
      int placed = 0;
      for (int attempt = 0; attempt < targetPieces * 4 && placed < targetPieces; attempt++) {
         int x = random.nextInt(radius * 2 + 1) - radius;
         int z = random.nextInt(radius * 2 + 1) - radius;
         BlockPos pos = surface(level, base.offset(x, 0, z));
         if (!canPlacePiece(level, pos, base.getY())) {
            continue;
         }
         String blockId = SCATTER_BLOCKS.get(random.nextInt(SCATTER_BLOCKS.size()));
         placePiece(level, pos, blockId, random);
         placed++;
      }
      return placed;
   }

   private static boolean canGenerate(ServerLevel level, BlockPos base) {
      if (!footprintChunksLoaded(level, base, Math.max(2, Config.SCATTER_SEARCH_RADIUS.get()))) {
         return false;
      }
      String biome = level.getBiome(base).unwrapKey().map(key -> key.identifier().getPath()).orElse("");
      if (biome.contains("ocean") || biome.contains("river") || biome.contains("beach")) {
         return false;
      }
      int minY = Integer.MAX_VALUE;
      int maxY = Integer.MIN_VALUE;
      for (int x = -6; x <= 6; x += 3) {
         for (int z = -6; z <= 6; z += 3) {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, base.getX() + x, base.getZ() + z);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
            BlockPos sample = new BlockPos(base.getX() + x, y, base.getZ() + z);
            if (!level.getFluidState(sample).isEmpty() || !level.getFluidState(sample.below()).isEmpty()) {
               return false;
            }
         }
      }
      return maxY - minY <= 5;
   }

   private static boolean footprintChunksLoaded(ServerLevel level, BlockPos base, int radius) {
      int minChunkX = Math.floorDiv(base.getX() - radius, 16);
      int maxChunkX = Math.floorDiv(base.getX() + radius, 16);
      int minChunkZ = Math.floorDiv(base.getZ() - radius, 16);
      int maxChunkZ = Math.floorDiv(base.getZ() + radius, 16);
      for (int cx = minChunkX; cx <= maxChunkX; cx++) {
         for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
            if (!level.hasChunk(cx, cz)) {
               return false;
            }
         }
      }
      return true;
   }

   private static boolean canPlacePiece(ServerLevel level, BlockPos pos, int baseY) {
      return Math.abs(pos.getY() - baseY) <= 5
         && level.getBlockState(pos).isAir()
         && !level.getBlockState(pos.below()).isAir()
         && level.getFluidState(pos).isEmpty()
         && level.getFluidState(pos.below()).isEmpty();
   }

   private static void placePiece(ServerLevel level, BlockPos pos, String blockId, Random random) {
      BlockState state = state(blockId);
      level.setBlock(pos, state, 3);
      if (random.nextInt(5) == 0 && state.getBlock() != block("rubble_pile") && state.getBlock() != block("scattered_debris")) {
         BlockPos cap = pos.above();
         if (level.getBlockState(cap).isAir()) {
            level.setBlock(cap, state(random.nextBoolean() ? "rubble_pile" : "scattered_debris"), 3);
         }
      }
   }

   private static BlockPos surface(ServerLevel level, BlockPos origin) {
      int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin.getX(), origin.getZ());
      return new BlockPos(origin.getX(), y, origin.getZ());
   }

   private static BlockState state(String id) {
      return block(id).defaultBlockState();
   }

   private static Block block(String id) {
      return ModBlocks.blockForId(id).orElseThrow(() -> new IllegalStateException("Unknown Blockworks scatter block " + id)).get();
   }
}
