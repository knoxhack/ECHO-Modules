package com.knoxhack.echoindustrialnexus.block;

import com.knoxhack.echoindustrialnexus.block.entity.IndustrialItemDuctBlockEntity;
import com.knoxhack.echoindustrialnexus.registry.ModBlockEntities;
import com.knoxhack.echoindustrialnexus.registry.ModItems;
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
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class IndustrialItemDuctBlock extends Block implements EntityBlock {
   private final String displayName;
   private final int transferInterval;
   private final boolean vacuum;
   private final boolean nexusSafe;

   public IndustrialItemDuctBlock(String displayName, int transferInterval, boolean vacuum, boolean nexusSafe, Properties properties) {
      super(properties);
      this.displayName = displayName;
      this.transferInterval = Math.max(2, transferInterval);
      this.vacuum = vacuum;
      this.nexusSafe = nexusSafe;
   }

   public int transferInterval() {
      return this.transferInterval;
   }

   public boolean vacuum() {
      return this.vacuum;
   }

   public boolean nexusSafe() {
      return this.nexusSafe;
   }

   public boolean smart() {
      return this.displayName.toLowerCase(java.util.Locale.ROOT).contains("smart");
   }

   public String displayName() {
      return this.displayName;
   }

   public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new IndustrialItemDuctBlockEntity(pos, state);
   }

   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return type == ModBlockEntities.ITEM_DUCT.get()
         ? (tickLevel, pos, blockState, blockEntity) -> IndustrialItemDuctBlockEntity.tick(tickLevel, pos, blockState, (IndustrialItemDuctBlockEntity)blockEntity)
         : null;
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (!level.isClientSide()) {
         if (level.getBlockEntity(pos) instanceof IndustrialItemDuctBlockEntity duct) {
            player.sendSystemMessage(duct.filterStatus(this));
         } else {
            String safety = this.nexusSafe ? " Nexus-safe handling enabled." : " Nexus materials blocked.";
            player.sendSystemMessage(Component.literal("ECHO INDUSTRIAL // " + this.displayName + " routing every " + this.transferInterval + " ticks." + safety));
         }
      }
      return InteractionResult.SUCCESS;
   }

   @Override
   protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
      if (!this.smart()) {
         return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
      }
      if (!(level.getBlockEntity(pos) instanceof IndustrialItemDuctBlockEntity duct)) {
         return InteractionResult.PASS;
      }
      if (!level.isClientSide()) {
         if (stack.is(ModItems.THERMAL_WRENCH.get())) {
            duct.toggleFilterMode();
            player.sendSystemMessage(duct.filterStatus(this));
         } else if (duct.installFilter(player, stack)) {
            player.sendSystemMessage(duct.filterStatus(this));
         } else {
            player.sendSystemMessage(duct.filterStatus(this));
         }
      }
      return InteractionResult.SUCCESS;
   }

   @Override
   public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
      if (!level.isClientSide() && blockEntity instanceof IndustrialItemDuctBlockEntity duct) {
         duct.dropFilterContents(level, pos);
      }

      super.playerDestroy(level, player, pos, state, blockEntity, tool);
   }
}
