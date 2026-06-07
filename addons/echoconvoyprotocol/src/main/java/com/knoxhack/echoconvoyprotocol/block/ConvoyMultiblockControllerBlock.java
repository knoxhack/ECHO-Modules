package com.knoxhack.echoconvoyprotocol.block;

import com.knoxhack.echoconvoyprotocol.block.entity.ConvoyMultiblockControllerBlockEntity;
import com.knoxhack.echoconvoyprotocol.registry.ModBlockEntities;
import com.knoxhack.echomultiblockcore.block.MultiblockControllerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class ConvoyMultiblockControllerBlock extends MultiblockControllerBlock {
   public ConvoyMultiblockControllerBlock(Identifier defaultDefinitionId, Properties properties) {
      super(defaultDefinitionId, properties);
   }

   @Override
   public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new ConvoyMultiblockControllerBlockEntity(pos, state);
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return type == ModBlockEntities.CONVOY_MULTIBLOCK_CONTROLLER.get()
         ? (tickLevel, pos, blockState, blockEntity) -> ConvoyMultiblockControllerBlockEntity.tick(
            tickLevel,
            pos,
            blockState,
            (ConvoyMultiblockControllerBlockEntity)blockEntity
         )
         : null;
   }
}
