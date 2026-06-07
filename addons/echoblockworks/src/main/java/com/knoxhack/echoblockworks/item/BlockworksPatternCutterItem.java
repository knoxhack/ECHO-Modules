package com.knoxhack.echoblockworks.item;

import com.knoxhack.echoblockworks.block.BlockworksStateUtil;
import com.knoxhack.echoblockworks.content.BlockworksBlockInfo;
import com.knoxhack.echoblockworks.content.BlockworksCatalog;
import com.knoxhack.echoblockworks.integration.BlockworksMissionHooks;
import com.knoxhack.echoblockworks.registry.ModBlocks;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BlockworksPatternCutterItem extends Item {
   public BlockworksPatternCutterItem(Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResult useOn(UseOnContext context) {
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      BlockState state = level.getBlockState(pos);
      Optional<BlockworksBlockInfo> source = BlockworksCatalog.blockInfo(ModBlocks.idOf(state.getBlock()));
      if (source.isEmpty()) {
         return InteractionResult.PASS;
      }
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      }

      boolean backwards = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
      Optional<BlockworksBlockInfo> target = BlockworksCatalog.cycle(source.get(), backwards);
      if (target.isEmpty() || target.get() == source.get()) {
         return InteractionResult.PASS;
      }

      Block targetBlock = ModBlocks.blockFor(target.get()).get();
      BlockState replacement = BlockworksStateUtil.copySharedProperties(state, targetBlock.defaultBlockState());
      level.setBlock(pos, replacement, 3);
      level.playSound(null, pos, SoundEvents.COPPER_BULB_TURN_ON, SoundSource.BLOCKS, 0.75F, backwards ? 0.8F : 1.05F);
      if (level instanceof ServerLevel serverLevel) {
         serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
            pos.getX() + 0.5D, pos.getY() + 0.75D, pos.getZ() + 0.5D,
            6, 0.25D, 0.25D, 0.25D, 0.01D);
      }
      if (context.getPlayer() != null && !context.getPlayer().hasInfiniteMaterials()) {
         context.getItemInHand().hurtAndBreak(1, context.getPlayer(), context.getHand());
      }
      BlockworksMissionHooks.recordPatternCutter(context.getPlayer(), target.get().blockId());
      BlockworksMissionHooks.recordShowcaseSite(context.getPlayer(), "pattern_cutter");
      return InteractionResult.SUCCESS_SERVER;
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
      tooltip.accept(Component.translatable("tooltip.echoblockworks.echo_pattern_cutter.forward").withStyle(ChatFormatting.GRAY));
      tooltip.accept(Component.translatable("tooltip.echoblockworks.echo_pattern_cutter.backward").withStyle(ChatFormatting.DARK_GRAY));
      tooltip.accept(Component.translatable("tooltip.echoblockworks.echo_pattern_cutter.durability").withStyle(ChatFormatting.DARK_AQUA));
   }
}
