package com.knoxhack.echoagriculturereclamation.block;

import com.knoxhack.echoagriculturereclamation.block.entity.ReclamationCropBlockEntity;
import com.knoxhack.echoagriculturereclamation.content.CropSpec;
import com.knoxhack.echoagriculturereclamation.content.ReclamationContent;
import com.knoxhack.echoagriculturereclamation.content.ReclamationCropLogic;
import com.knoxhack.echoagriculturereclamation.content.SeedProfile;
import com.knoxhack.echoagriculturereclamation.content.SoilState;
import com.knoxhack.echoagriculturereclamation.integration.ReclamationCrossAddonIntegration;
import com.knoxhack.echoagriculturereclamation.progress.ReclamationProgress;
import com.knoxhack.echoagriculturereclamation.progress.ReclamationRestoration;
import com.knoxhack.echoagriculturereclamation.registry.ModBlocks;
import com.knoxhack.echoagriculturereclamation.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class ReclamationCropBlock extends Block implements EntityBlock {
   public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
   public static final BooleanProperty STABILIZED = BooleanProperty.create("stabilized");
   private static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D);
   private final CropSpec spec;

   public ReclamationCropBlock(CropSpec spec, Properties properties) {
      super(properties);
      this.spec = spec;
      registerDefaultState(stateDefinition.any().setValue(AGE, 0).setValue(STABILIZED, false));
   }

   public CropSpec spec() {
      return spec;
   }

   public BlockState plantedState(SeedProfile profile) {
      return defaultBlockState().setValue(AGE, 0).setValue(STABILIZED, ReclamationCropLogic.stable(profile));
   }

   @Override
   public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new ReclamationCropBlockEntity(pos, state);
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(AGE, STABILIZED);
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   private boolean canPlantOn(BlockState state, BlockGetter level, BlockPos pos) {
      return state.getBlock() == ModBlocks.HYDROPONIC_TRAY.get() || SoilState.fromBlock(state).canSupport(spec, 50);
   }

   @Override
   protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      return canPlantOn(level.getBlockState(pos.below()), level, pos.below());
   }

   @Override
   protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      if (state.getValue(AGE) >= 7) {
         return;
      }
      SoilState soil = level.getBlockState(pos.below()).getBlock() == ModBlocks.HYDROPONIC_TRAY.get()
         ? SoilState.STABILIZED
         : SoilState.fromBlock(level.getBlockState(pos.below()));
      var greenhouse = ReclamationProgress.growthGreenhouseContext(level, pos);
      SeedProfile profile = profileAt(level, pos, state);
      if (!ReclamationCropLogic.canGrow(spec, soil, profile, greenhouse)) {
         if (random.nextInt(100) < ReclamationContent.crop(spec).failedGrowthDeathChance()) {
            level.destroyBlock(pos, false);
         }
         return;
      }
      int chance = Math.max(1, ReclamationCropLogic.growthChance(spec, soil, profile, greenhouse, 0)
         - ReclamationCrossAddonIntegration.weatherGrowthPenalty(level, pos, greenhouse.quality() == ReclamationProgress.GreenhouseZoneQuality.SAFE));
      if (random.nextInt(100) < chance) {
         level.setBlock(pos, state.setValue(AGE, state.getValue(AGE) + 1), 3);
      }
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      }
      if (state.getValue(AGE) < 7) {
         player.sendSystemMessage(Component.literal("ECHO FIELD // " + spec.displayName() + " stability " + (state.getValue(STABILIZED) ? "stable" : "volatile") + ". Growth incomplete."));
         return InteractionResult.CONSUME;
      }
      SeedProfile profile = profileAt(level, pos, state);
      boolean stable = ReclamationCropLogic.stable(profile);
      var greenhouse = ReclamationProgress.greenhouseContext(level, pos);
      int count = ReclamationCropLogic.yield(spec, profile, greenhouse, false);
      give(player, new ItemStack(ModItems.produceFor(spec).get(), Math.max(1, count)));
      boolean returnSeed = !stable && ReclamationProgress.needsStabilizationSeed(player)
         || ReclamationCropLogic.shouldReturnContaminatedSeed(level.getRandom(), profile, greenhouse);
      if (returnSeed) {
         ItemStack contaminated = new ItemStack(ModItems.CONTAMINATED_SEED.get());
         contaminated.set(ModItems.seedProfileComponent(), ReclamationCropLogic.degradedSeed(profile));
         give(player, contaminated);
         ReclamationProgress.recordStabilizationSeed(player);
      }
      ReclamationProgress.recordGrowth(player, spec, stable);
      ReclamationCrossAddonIntegration.playFieldCue(player, "complete");
      if (level instanceof ServerLevel serverLevel) {
         ReclamationRestoration.cropMatured(serverLevel, pos, player, spec, profile);
      }
      level.setBlock(pos, state.setValue(AGE, 0), 3);
      return InteractionResult.SUCCESS_SERVER;
   }

   public boolean serviceFromPollinator(ServerLevel level, BlockPos pos, int baseGrowthBonus) {
      BlockState state = level.getBlockState(pos);
      if (!state.is(this) || state.getValue(AGE) >= 7) {
         return false;
      }
      var greenhouse = ReclamationProgress.growthGreenhouseContext(level, pos);
      int bonus = greenhouse.pollinationBonus(baseGrowthBonus);
      if (bonus <= 0) {
         return false;
      }
      SoilState soil = level.getBlockState(pos.below()).getBlock() == ModBlocks.HYDROPONIC_TRAY.get()
         ? SoilState.STABILIZED
         : SoilState.fromBlock(level.getBlockState(pos.below()));
      SeedProfile profile = profileAt(level, pos, state);
      if (!ReclamationCropLogic.canGrow(spec, soil, profile, greenhouse)) {
         return false;
      }
      int chance = Math.min(95, Math.max(1, ReclamationCropLogic.growthChance(spec, soil, profile, greenhouse, 0)
         - ReclamationCrossAddonIntegration.weatherGrowthPenalty(level, pos, greenhouse.quality() == ReclamationProgress.GreenhouseZoneQuality.SAFE)) + bonus);
      if (level.getRandom().nextInt(100) < chance) {
         level.setBlock(pos, state.setValue(AGE, state.getValue(AGE) + 1), 3);
      }
      return true;
   }

   private SeedProfile profileAt(Level level, BlockPos pos, BlockState state) {
      if (level.getBlockEntity(pos) instanceof ReclamationCropBlockEntity crop) {
         return crop.profileOrFallback(spec, state.getValue(STABILIZED));
      }
      return ReclamationCropLogic.fallbackProfile(spec, state.getValue(STABILIZED));
   }

   private static void give(Player player, ItemStack stack) {
      if (!player.getInventory().add(stack)) {
         player.drop(stack, false);
      }
   }
}
