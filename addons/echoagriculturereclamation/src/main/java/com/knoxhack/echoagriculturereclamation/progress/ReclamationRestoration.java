package com.knoxhack.echoagriculturereclamation.progress;

import com.knoxhack.echoagriculturereclamation.content.CropSpec;
import com.knoxhack.echoagriculturereclamation.content.ReclamationContent;
import com.knoxhack.echoagriculturereclamation.content.ReclamationCropLogic;
import com.knoxhack.echoagriculturereclamation.content.SeedProfile;
import com.knoxhack.echoagriculturereclamation.content.SoilState;
import com.knoxhack.echoagriculturereclamation.integration.ReclamationCrossAddonIntegration;
import com.knoxhack.echoagriculturereclamation.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class ReclamationRestoration {
   private ReclamationRestoration() {
   }

   public static int purifyArea(Level level, BlockPos center, int radius, int maxBlocks) {
      int changed = 0;
      for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -1, -radius), center.offset(radius, 1, radius))) {
         if (changed >= maxBlocks) {
            break;
         }
         SoilState current = SoilState.fromBlock(level.getBlockState(pos));
         SoilState next = current.purifiedStep();
         if (next != current && setSoil(level, pos, next)) {
            changed++;
         }
      }
      if (changed > 0 && level instanceof ServerLevel serverLevel) {
         ReclamationWorldData.get(serverLevel).addStat("soil_purified", changed);
      }
      return changed;
   }

   public static void cropMatured(ServerLevel level, BlockPos cropPos, Player player, CropSpec spec, SeedProfile profile) {
      ReclamationProgress.GreenhouseContext greenhouse = ReclamationProgress.greenhouseContext(level, cropPos);
      int gain = ReclamationCrossAddonIntegration.restorationGain(
         player,
         ReclamationCropLogic.restorationGain(spec, profile, greenhouse)
      );
      ReclamationWorldData world = ReclamationWorldData.get(level);
      ChunkPos chunk = new ChunkPos(cropPos.getX() >> 4, cropPos.getZ() >> 4);
      int score = world.addRestoration(chunk, gain);
      world.recordFieldHistory(chunk, level.getGameTime(), greenhouse.qualityLabel(), threshold(score), greenhouse.nextAction());
      if (player != null) {
         ReclamationProgress.max(player, "restoration_score", score);
         if (score >= ReclamationContent.progression().restoreThreshold()) {
            ReclamationProgress.mark(player, "restore_chunk");
         }
      }
      var rules = ReclamationContent.progression();
      if (score >= rules.purifyThreshold() && level.getRandom().nextInt(3) == 0) {
         purifyArea(level, cropPos, 2, score >= rules.restoreThreshold() * 3 / 4 ? rules.cropPurifyMaxHigh() : rules.cropPurifyMaxLow());
      }
      if (score >= rules.stabilizeThreshold() && ReclamationContent.crop(spec).restorationWeight() >= rules.restorationCropWeightForStabilization()) {
         stabilizeArea(level, cropPos, 2, rules.cropStabilizeMax());
      }
      if (score >= rules.restoreThreshold()) {
         stabilizeArea(level, cropPos, 3, rules.cropRestoreMax());
      }
   }

   public static int scanPulse(ServerLevel level, BlockPos center, Player player, ReclamationProgress.GreenhouseContext greenhouse) {
      ReclamationWorldData world = ReclamationWorldData.get(level);
      ChunkPos chunk = new ChunkPos(center.getX() >> 4, center.getZ() >> 4);
      var rules = ReclamationContent.progression();
      int baseGain = greenhouse.score() >= rules.greenhouseSafeThreshold()
         ? rules.scannerSafeRestorationGain()
         : rules.scannerUnsafeRestorationGain();
      int score = world.addRestoration(chunk, ReclamationCrossAddonIntegration.restorationGain(player, greenhouse.restorationGain(baseGain)));
      world.setGreenhouseSafety(chunk, greenhouse.score());
      world.recordFieldHistory(chunk, level.getGameTime(), greenhouse.qualityLabel(), threshold(score), greenhouse.nextAction());
      int changed = 0;
      if (score >= rules.purifyThreshold()) {
         changed += purifyArea(level, center, 2, score >= rules.restoreThreshold() * 3 / 4 ? rules.scannerPurifyMaxHigh() : rules.scannerPurifyMaxLow());
      }
      if (score >= rules.stabilizeThreshold()) {
         changed += stabilizeArea(level, center, 2, rules.scannerStabilizeMax());
      }
      if (score >= rules.restoreThreshold()) {
         changed += stabilizeArea(level, center, 3, rules.scannerRestoreMax());
      }
      if (player != null) {
         ReclamationProgress.max(player, "restoration_score", score);
         if (score >= rules.restoreThreshold()) {
            ReclamationProgress.mark(player, "restore_chunk");
         }
         if (changed > 0) {
            ReclamationProgress.add(player, "soil_purified", changed);
         }
      }
      return score;
   }

   public static int stabilizeArea(Level level, BlockPos center, int radius, int maxBlocks) {
      int changed = 0;
      SoilState target = radius >= 3 ? SoilState.RESTORED : SoilState.STABILIZED;
      for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -1, -radius), center.offset(radius, 1, radius))) {
         if (changed >= maxBlocks) {
            break;
         }
         SoilState current = SoilState.fromBlock(level.getBlockState(pos));
         if (current.ordinal() < target.ordinal() && setSoil(level, pos, target)) {
            changed++;
         }
      }
      return changed;
   }

   public static boolean setSoil(Level level, BlockPos pos, SoilState state) {
      BlockState replacement = ModBlocks.blockFor(state).defaultBlockState();
      if (level.getBlockState(pos).is(replacement.getBlock())) {
         return false;
      }
      SoilState current = SoilState.fromBlock(level.getBlockState(pos));
      if (current == SoilState.DEAD && !isReclamationSoil(level.getBlockState(pos))
         && !ReclamationCrossAddonIntegration.isExternalRestorableSoil(level.getBlockState(pos))) {
         return false;
      }
      level.setBlock(pos, replacement, 3);
      return true;
   }

   private static boolean isReclamationSoil(BlockState state) {
      return state.is(ModBlocks.DEAD_SOIL.get())
         || state.is(ModBlocks.CONTAMINATED_SOIL.get())
         || state.is(ModBlocks.IRRADIATED_SOIL.get())
         || state.is(ModBlocks.TOXIC_MUD.get())
         || state.is(ModBlocks.PURIFIED_SOIL.get())
         || state.is(ModBlocks.STABILIZED_SOIL.get())
         || state.is(ModBlocks.RESTORED_SOIL.get());
   }

   private static int threshold(int score) {
      var rules = ReclamationContent.progression();
      if (score >= rules.restoreThreshold()) {
         return rules.restoreThreshold();
      }
      if (score >= rules.stabilizeThreshold()) {
         return rules.stabilizeThreshold();
      }
      if (score >= rules.purifyThreshold()) {
         return rules.purifyThreshold();
      }
      return 0;
   }
}
