package com.knoxhack.echonexusprotocol.block;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echonexusprotocol.data.NexusPlayerData;
import com.knoxhack.echonexusprotocol.integration.NexusProgression;
import com.knoxhack.echonexusprotocol.registry.ModSounds;
import com.knoxhack.echonexusprotocol.world.NexusWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class BlackboxMonolithCoreBlock extends Block {
   public BlackboxMonolithCoreBlock(Properties properties) { super(properties); }
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) { if (level.isClientSide()) return InteractionResult.SUCCESS; if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) activate(serverLevel, pos, serverPlayer); return InteractionResult.SUCCESS_SERVER; }
   public static void activate(ServerLevel level, BlockPos pos, ServerPlayer player) { NexusPlayerData data = NexusPlayerData.get(player); boolean first = data.activateBlackboxMonolith(); data.unlockResearch(NexusPlayerData.RESEARCH_FORBIDDEN_CORE_ACCESS); NexusWorldData worldData = NexusWorldData.get(level); worldData.activateBlackboxMonolith(); worldData.startAnomalyStorm(new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4), level.getGameTime()); data.refreshFieldTelemetry(player); NexusPlayerData.saveAndSync(player, data); EchoCoreServices.recordMilestone(player, NexusProgression.BLACKBOX_MONOLITH); level.playSound(null, pos, ModSounds.MONOLITH_ACTIVATE.get(), SoundSource.BLOCKS, 1.0F, first ? 0.8F : 1.1F); player.sendSystemMessage(Component.literal(first ? "ECHO-7 // Blackbox Monolith active. I have seen this structure before. Correction: I was created here." : "ECHO-7 // Blackbox route already awake. The Monolith is still listening.")); }
}
