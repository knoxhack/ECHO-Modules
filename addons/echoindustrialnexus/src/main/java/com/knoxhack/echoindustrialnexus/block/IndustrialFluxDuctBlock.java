package com.knoxhack.echoindustrialnexus.block;

import com.knoxhack.echoindustrialnexus.block.entity.IndustrialFluxDuctBlockEntity;
import com.knoxhack.echoindustrialnexus.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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

public class IndustrialFluxDuctBlock extends Block implements EntityBlock {
   private final String displayName;
   private final int transferLimit;

   public IndustrialFluxDuctBlock(Properties properties) {
      this("Copper Flux Duct", 256, properties);
   }

   public IndustrialFluxDuctBlock(String displayName, int transferLimit, Properties properties) {
      super(properties);
      this.displayName = displayName;
      this.transferLimit = transferLimit;
   }

   public int transferLimit() {
      return this.transferLimit;
   }

   public @Nullable BlockEntity newBlockEntity(BlockPos worldPosition, BlockState blockState) {
      return new IndustrialFluxDuctBlockEntity(worldPosition, blockState);
   }

   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> type) {
      return type == ModBlockEntities.FLUX_DUCT.get()
         ? (tickLevel, pos, state, blockEntity) -> IndustrialFluxDuctBlockEntity.tick(tickLevel, pos, state, (IndustrialFluxDuctBlockEntity)blockEntity)
         : null;
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (!level.isClientSide()) {
         player.sendSystemMessage(Component.literal("ECHO INDUSTRIAL // " + this.displayName + " ready. Transfer limit: " + this.transferLimit + " TF/pull."));
      }

      return InteractionResult.SUCCESS;
   }
}
