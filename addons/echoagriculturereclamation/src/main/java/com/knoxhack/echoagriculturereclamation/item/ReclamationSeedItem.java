package com.knoxhack.echoagriculturereclamation.item;

import com.knoxhack.echoagriculturereclamation.block.entity.ReclamationCropBlockEntity;
import com.knoxhack.echoagriculturereclamation.content.CropSpec;
import com.knoxhack.echoagriculturereclamation.content.ReclamationCropLogic;
import com.knoxhack.echoagriculturereclamation.content.SeedProfile;
import com.knoxhack.echoagriculturereclamation.content.SoilState;
import com.knoxhack.echoagriculturereclamation.integration.ReclamationCrossAddonIntegration;
import com.knoxhack.echoagriculturereclamation.progress.ReclamationProgress;
import com.knoxhack.echoagriculturereclamation.registry.ModBlocks;
import com.knoxhack.echoagriculturereclamation.registry.ModItems;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class ReclamationSeedItem extends Item {
   private final Mode mode;

   public ReclamationSeedItem(Mode mode, Properties properties) {
      super(properties);
      this.mode = mode;
   }

   @Override
   public InteractionResult use(Level level, Player player, InteractionHand hand) {
      if (mode != Mode.CAPSULE) {
         return InteractionResult.PASS;
      }
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      }
      CropSpec spec = ReclamationCrossAddonIntegration.recoveredCrop(player, level.getRandom());
      SeedProfile profile = ReclamationCrossAddonIntegration.recoveredProfile(player, spec, level.getRandom());
      ItemStack seed = new ItemStack(ModItems.CONTAMINATED_SEED.get());
      seed.set(ModItems.seedProfileComponent(), profile);
      if (!player.getInventory().add(seed)) {
         player.drop(seed, false);
      }
      if (!player.getAbilities().instabuild) {
         player.getItemInHand(hand).shrink(1);
      }
      ReclamationProgress.discoverSeed(player, spec);
      player.sendSystemMessage(Component.literal("ECHO FIELD // Recovered seed capsule opened: " + spec.displayName()
         + ", contamination " + profile.contaminationTier() + ", stability " + profile.stability() + "%. Plant on compatible soil, tray-grow, or stabilize with Gene Sample/Bio-Gel."));
      return InteractionResult.SUCCESS_SERVER;
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
      SeedProfile profile = stack.get(ModItems.seedProfileComponent());
      if (profile == null) {
         tooltip.accept(Component.translatable(mode == Mode.CAPSULE
            ? "tooltip.echoagriculturereclamation.seed_capsule"
            : "tooltip.echoagriculturereclamation.unprofiled_seed").withStyle(ChatFormatting.GRAY));
         return;
      }
      tooltip.accept(Component.translatable(
         "tooltip.echoagriculturereclamation.seed_profile",
         profile.spec().displayName(),
         profile.stability(),
         profile.contaminationTier()
      ).withStyle(ReclamationCropLogic.stable(profile) ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
      tooltip.accept(Component.translatable(ReclamationCropLogic.stable(profile)
         ? "tooltip.echoagriculturereclamation.seed_profile.stable"
         : "tooltip.echoagriculturereclamation.seed_profile.unstable").withStyle(ChatFormatting.GRAY));
      tooltip.accept(Component.translatable(
         "tooltip.echoagriculturereclamation.seed_profile.category",
         profile.spec().category().displayName()
      ).withStyle(ChatFormatting.DARK_AQUA));
      tooltip.accept(Component.translatable(
         "tooltip.echoagriculturereclamation.seed_profile.route_hint"
      ).withStyle(ChatFormatting.DARK_GRAY));
   }

   @Override
   public InteractionResult useOn(UseOnContext context) {
      if (mode == Mode.CAPSULE) {
         return InteractionResult.PASS;
      }
      Level level = context.getLevel();
      Player player = context.getPlayer();
      if (player == null) {
         return InteractionResult.PASS;
      }
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      }
      ItemStack stack = context.getItemInHand();
      SeedProfile profile = stack.get(ModItems.seedProfileComponent());
      if (profile == null) {
         player.sendSystemMessage(Component.literal("ECHO FIELD // Seed lacks recovered profile data. Analyze a seed capsule first."));
         return InteractionResult.CONSUME;
      }
      BlockPos soilPos = context.getClickedPos();
      BlockPos cropPos = soilPos.above();
      if (!level.isEmptyBlock(cropPos)) {
         player.sendSystemMessage(Component.literal("ECHO FIELD // Planting space obstructed."));
         return InteractionResult.CONSUME;
      }
      SoilState soil = SoilState.fromBlock(level.getBlockState(soilPos));
      var greenhouse = ReclamationProgress.greenhouseContext(level, soilPos);
      if (!ReclamationCropLogic.canGrow(profile.spec(), soil, profile, greenhouse)) {
         player.sendSystemMessage(Component.literal("ECHO FIELD // " + profile.spec().displayName() + " rejected " + soil.displayName()
            + " at greenhouse safety " + greenhouse.score() + "/100. Purify soil, raise greenhouse safety, or use a Hydroponic Tray."));
         return InteractionResult.CONSUME;
      }
      level.setBlock(cropPos, ModBlocks.cropBlock(profile.spec()).plantedState(profile), 3);
      if (level.getBlockEntity(cropPos) instanceof ReclamationCropBlockEntity crop) {
         crop.setProfile(profile);
      }
      if (!player.getAbilities().instabuild) {
         stack.shrink(1);
      }
      ReclamationProgress.discoverSeed(player, profile.spec());
      ReclamationProgress.mark(player, "first_growth_started");
      if (level instanceof ServerLevel) {
         ReclamationProgress.metrics(player);
      }
      player.sendSystemMessage(Component.literal("ECHO FIELD // Planted " + profile.spec().displayName() + " in " + soil.displayName()
         + " (stability " + profile.stability() + "%, contamination " + profile.contaminationTier() + ")."));
      return InteractionResult.SUCCESS_SERVER;
   }

   public enum Mode {
      CAPSULE,
      CONTAMINATED,
      STABILIZED
   }
}
