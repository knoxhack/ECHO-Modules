package com.knoxhack.echoindustrialnexus.block;

import com.knoxhack.echoindustrialnexus.block.entity.IndustrialFluidPipeBlockEntity;
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
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class IndustrialFluidPipeBlock extends Block implements EntityBlock {
   private final PipeTier tier;

   public IndustrialFluidPipeBlock(PipeTier tier, Properties properties) {
      super(properties);
      this.tier = tier;
   }

   public PipeTier tier() {
      return this.tier;
   }

   public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new IndustrialFluidPipeBlockEntity(pos, state);
   }

   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return type == ModBlockEntities.FLUID_PIPE.get()
         ? (tickLevel, pos, blockState, blockEntity) -> IndustrialFluidPipeBlockEntity.tick(tickLevel, pos, blockState, (IndustrialFluidPipeBlockEntity)blockEntity)
         : null;
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (!level.isClientSide()) {
         player.sendSystemMessage(Component.literal("ECHO INDUSTRIAL // " + this.tier.displayName() + " "
            + this.tier.transferRate() + " mB/t, capacity " + this.tier.capacity() + " mB."));
      }
      return InteractionResult.SUCCESS;
   }

   public enum PipeTier {
      RUSTED("Rusted Pipe", 1000, 80, true, false, false, false, 2),
      REINFORCED("Reinforced Pipe", 4000, 160, false, true, true, false, 0),
      PRESSURIZED("Pressurized Pipe", 6000, 320, false, true, true, false, 0),
      SHIELDED("Shielded Pipe", 4000, 240, false, true, true, false, 0),
      STATIC("Static Pipe", 4000, 240, false, false, false, true, 1);

      private final String displayName;
      private final int capacity;
      private final int transferRate;
      private final boolean dirtyOnly;
      private final boolean acceptsHazard;
      private final boolean acceptsPressurized;
      private final boolean nexusOnly;
      private final int lossPerTransfer;

      PipeTier(String displayName, int capacity, int transferRate, boolean dirtyOnly, boolean acceptsHazard, boolean acceptsPressurized, boolean nexusOnly, int lossPerTransfer) {
         this.displayName = displayName;
         this.capacity = capacity;
         this.transferRate = transferRate;
         this.dirtyOnly = dirtyOnly;
         this.acceptsHazard = acceptsHazard;
         this.acceptsPressurized = acceptsPressurized;
         this.nexusOnly = nexusOnly;
         this.lossPerTransfer = lossPerTransfer;
      }

      public String displayName() {
         return this.displayName;
      }

      public int capacity() {
         return this.capacity;
      }

      public int transferRate() {
         return this.transferRate;
      }

      public boolean dirtyOnly() {
         return this.dirtyOnly;
      }

      public boolean acceptsHazard() {
         return this.acceptsHazard;
      }

      public boolean acceptsPressurized() {
         return this.acceptsPressurized;
      }

      public boolean nexusOnly() {
         return this.nexusOnly;
      }

      public int lossPerTransfer() {
         return this.lossPerTransfer;
      }
   }
}
