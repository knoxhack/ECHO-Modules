package com.knoxhack.echonexusprotocol.block;

import com.knoxhack.echonexusprotocol.entity.NexusMobEntity;
import com.knoxhack.echonexusprotocol.registry.ModEntities;
import com.knoxhack.echonexusprotocol.registry.ModSounds;
import com.knoxhack.echonexusprotocol.world.NexusWorldData;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RealityTearBlock extends Block {
   public static final MapCodec<RealityTearBlock> CODEC = simpleCodec(RealityTearBlock::new);

   public RealityTearBlock(Properties properties) {
      super(properties);
   }

   protected MapCodec<? extends Block> codec() {
      return CODEC;
   }

   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return Shapes.empty();
   }

   protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
      if (!level.isClientSide()) {
         level.scheduleTick(pos, this, 60);
      }
   }

   protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      NexusWorldData data = NexusWorldData.get(level);
      ChunkPos chunk = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
      data.markRealityTearActive(chunk);
      boolean merged = "merge".equals(data.endingState());
      data.addCorruptionPressure(chunk, merged ? 1 : 3);
      data.addFieldValue(chunk, merged ? -1 : -2);
      level.playSound(null, pos, ModSounds.REALITY_TEAR_PULSE.get(), SoundSource.BLOCKS, 0.45F, 0.7F);
      if (!merged && random.nextInt(4) == 0) {
         NexusMobEntity mob = (NexusMobEntity)((EntityType)ModEntities.DATA_WRAITH.get()).create(level, EntitySpawnReason.EVENT);
         if (mob != null) {
            mob.setPos(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
            level.addFreshEntity(mob);
         }
      }

      level.scheduleTick(pos, this, 100);
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      }
      ServerLevel serverLevel = (ServerLevel)level;
      NexusWorldData data = NexusWorldData.get(serverLevel);
      if (!"merge".equals(data.endingState()) || !(player instanceof ServerPlayer serverPlayer)) {
         player.sendSystemMessage(net.minecraft.network.chat.Component.literal("ECHO-7 // Reality Tear unstable. Merge path required for safe traversal."));
         return InteractionResult.CONSUME;
      }
      int dx = level.getRandom().nextInt(97) - 48;
      int dz = level.getRandom().nextInt(97) - 48;
      BlockPos target = serverLevel.getHeightmapPos(Types.MOTION_BLOCKING_NO_LEAVES, pos.offset(dx, 0, dz)).above();
      serverPlayer.teleportTo(serverLevel, target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, java.util.Set.of(), player.getYRot(), player.getXRot(), false);
      data.addCorruptionPressure(new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4), -4);
      serverLevel.playSound(null, target, ModSounds.REALITY_TEAR_PULSE.get(), SoundSource.PLAYERS, 0.7F, 1.4F);
      return InteractionResult.SUCCESS_SERVER;
   }
}
