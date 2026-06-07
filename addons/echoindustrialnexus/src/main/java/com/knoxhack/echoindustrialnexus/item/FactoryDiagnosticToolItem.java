package com.knoxhack.echoindustrialnexus.item;

import com.knoxhack.echomultiblockcore.block.entity.MultiblockControllerBlockEntity;
import com.knoxhack.echomultiblockcore.block.entity.RoboticArmBlockEntity;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class FactoryDiagnosticToolItem extends Item {
   public FactoryDiagnosticToolItem(Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResult useOn(UseOnContext context) {
      Level level = context.getLevel();
      Player player = context.getPlayer();
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      }
      if (player == null) {
         return InteractionResult.CONSUME;
      }
      BlockEntity blockEntity = level.getBlockEntity(context.getClickedPos());
      if (blockEntity instanceof MultiblockControllerBlockEntity controller) {
         controller.diagnosticLines().forEach(line -> player.sendSystemMessage(Component.literal(line)));
         return InteractionResult.SUCCESS_SERVER;
      }
      if (blockEntity instanceof RoboticArmBlockEntity arm) {
         player.sendSystemMessage(arm.statusComponent());
         return InteractionResult.SUCCESS_SERVER;
      }
      player.sendSystemMessage(Component.translatable("message.echoindustrialnexus.diagnostic.no_target"));
      return InteractionResult.SUCCESS_SERVER;
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
      Consumer<Component> tooltip, TooltipFlag flag) {
      tooltip.accept(Component.translatable("tooltip.echoindustrialnexus.factory_diagnostic_tool").withStyle(ChatFormatting.AQUA));
   }
}
