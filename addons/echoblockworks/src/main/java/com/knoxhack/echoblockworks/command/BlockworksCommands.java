package com.knoxhack.echoblockworks.command;

import com.knoxhack.echoblockworks.EchoBlockworks;
import com.knoxhack.echoblockworks.block.BlockworksStateUtil;
import com.knoxhack.echoblockworks.content.BlockworksBlockInfo;
import com.knoxhack.echoblockworks.content.BlockworksCatalog;
import com.knoxhack.echoblockworks.content.BlockworksFamily;
import com.knoxhack.echoblockworks.content.BlockworksShapeKind;
import com.knoxhack.echoblockworks.registry.ModBlocks;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class BlockworksCommands {
   private BlockworksCommands() {
   }

   public static void registerCommands(
      com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher,
      CommandBuildContext buildContext,
      Commands.CommandSelection selection) {
      dispatcher.register(Commands.literal("echoblockworks")
         .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
         .then(Commands.literal("families")
            .executes(context -> families(context.getSource().getPlayerOrException())))
         .then(Commands.literal("variants")
            .then(Commands.argument("family", StringArgumentType.word())
               .executes(context -> variants(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "family")))))
         .then(Commands.literal("givefamily")
            .then(Commands.argument("family", StringArgumentType.word())
               .executes(context -> giveFamily(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "family")))))
         .then(Commands.literal("convert")
            .then(Commands.argument("variant", StringArgumentType.word())
               .executes(context -> convertLookedAt(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "variant"))))));
   }

   private static int families(ServerPlayer player) {
      tell(player, "Families: " + BlockworksCatalog.families().stream().map(BlockworksFamily::id).toList(), ChatFormatting.AQUA);
      return Command.SINGLE_SUCCESS;
   }

   private static int variants(ServerPlayer player, String familyId) {
      return BlockworksCatalog.family(familyId)
         .map(family -> {
            tell(player, family.displayName() + ": " + family.variants().stream().map(variant -> variant.id()).toList(), ChatFormatting.GRAY);
            return Command.SINGLE_SUCCESS;
         })
         .orElseGet(() -> {
            tell(player, "Unknown family: " + familyId, ChatFormatting.RED);
            return 0;
         });
   }

   private static int giveFamily(ServerPlayer player, String familyId) {
      BlockworksFamily family = BlockworksCatalog.family(familyId).orElse(null);
      if (family == null) {
         tell(player, "Unknown family: " + familyId, ChatFormatting.RED);
         return 0;
      }
      int count = 0;
      for (BlockworksBlockInfo info : BlockworksCatalog.blockInfos()) {
         if (!info.family().id().equals(family.id()) || info.shape() != BlockworksShapeKind.FULL) {
            continue;
         }
         ItemStack stack = new ItemStack(ModBlocks.blockFor(info).get());
         if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
         }
         count++;
      }
      tell(player, "Gave " + count + " " + family.displayName() + " variant blocks.", ChatFormatting.GREEN);
      return count;
   }

   private static int convertLookedAt(ServerPlayer player, String variantId) {
      HitResult hit = player.pick(5.0D, 0.0F, false);
      if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
         tell(player, "Look at a Blockworks block to convert it.", ChatFormatting.RED);
         return 0;
      }
      BlockState state = player.level().getBlockState(blockHit.getBlockPos());
      BlockworksBlockInfo source = BlockworksCatalog.blockInfo(ModBlocks.idOf(state.getBlock())).orElse(null);
      if (source == null) {
         tell(player, "Target block is not a Blockworks family block.", ChatFormatting.RED);
         return 0;
      }
      String normalizedVariant = variantId.toLowerCase(Locale.ROOT);
      BlockworksBlockInfo target = BlockworksCatalog.target(source.family().id(), normalizedVariant, source.shape()).orElse(null);
      if (target == null) {
         tell(player, "Variant '" + variantId + "' is unavailable for " + source.family().displayName() + " " + source.shape().displayName() + ".", ChatFormatting.RED);
         return 0;
      }
      Block targetBlock = ModBlocks.blockFor(target).get();
      BlockState replacement = BlockworksStateUtil.copySharedProperties(state, targetBlock.defaultBlockState());
      player.level().setBlock(blockHit.getBlockPos(), replacement, 3);
      tell(player, "Converted target to " + target.displayName() + ".", ChatFormatting.GREEN);
      return Command.SINGLE_SUCCESS;
   }

   private static void tell(ServerPlayer player, String message, ChatFormatting color) {
      player.sendSystemMessage(Component.literal("[ECHO BLOCKWORKS] " + message).withStyle(color));
   }
}
