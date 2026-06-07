package com.knoxhack.echoagriculturereclamation.block;

import com.knoxhack.echoagriculturereclamation.block.entity.HydroponicTrayBlockEntity;
import com.knoxhack.echoagriculturereclamation.content.SeedProfile;
import com.knoxhack.echoagriculturereclamation.registry.ModBlockEntities;
import com.knoxhack.echoagriculturereclamation.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class HydroponicTrayBlock extends Block implements EntityBlock {
   public HydroponicTrayBlock(Properties properties) {
      super(properties);
   }

   @Override
   public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new HydroponicTrayBlockEntity(pos, state);
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return !level.isClientSide() && type == ModBlockEntities.HYDROPONIC_TRAY.get()
         ? (tickLevel, pos, blockState, blockEntity) -> HydroponicTrayBlockEntity.tick(tickLevel, pos, blockState, (HydroponicTrayBlockEntity)blockEntity)
         : null;
   }

   @Override
   protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      }
      if (!(level.getBlockEntity(pos) instanceof HydroponicTrayBlockEntity tray)) {
         return InteractionResult.CONSUME;
      }
      if (stack.is(ModItems.SOIL_NUTRIENT_MIX.get())) {
         return tray.addNutrient(player, stack) ? InteractionResult.SUCCESS_SERVER : InteractionResult.CONSUME;
      }
      SeedProfile profile = stack.get(ModItems.seedProfileComponent());
      if (profile == null) {
         player.sendSystemMessage(Component.literal("ECHO FIELD // Hydroponic tray expects a recovered seed profile."));
         return InteractionResult.CONSUME;
      }
      return tray.insertSeed(player, stack) ? InteractionResult.SUCCESS_SERVER : InteractionResult.CONSUME;
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      }
      if (level.getBlockEntity(pos) instanceof HydroponicTrayBlockEntity tray) {
         if (player.isShiftKeyDown()) {
            tray.extractSeed(player);
         } else if (tray.readyToHarvest()) {
            tray.harvest(player);
         } else {
            player.openMenu(tray);
         }
      }
      return InteractionResult.SUCCESS_SERVER;
   }
}
