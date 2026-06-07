package com.knoxhack.echoindustrialnexus.block.entity;

import com.knoxhack.echoindustrialnexus.block.IndustrialFluxDuctBlock;
import com.knoxhack.echoindustrialnexus.flux.ThermalFluxNetwork;
import com.knoxhack.echoindustrialnexus.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class IndustrialFluxDuctBlockEntity extends BlockEntity {
   private int transferCooldown;

   public IndustrialFluxDuctBlockEntity(BlockPos pos, BlockState blockState) {
      super((BlockEntityType)ModBlockEntities.FLUX_DUCT.get(), pos, blockState);
   }

   public static void tick(Level level, BlockPos pos, BlockState state, IndustrialFluxDuctBlockEntity duct) {
      if (!level.isClientSide() && state.getBlock() instanceof IndustrialFluxDuctBlock fluxDuct) {
         if (duct.transferCooldown > 0) {
            duct.transferCooldown--;
            return;
         }
         ThermalFluxNetwork.balanceFlux(level, pos, fluxDuct.transferLimit());
         duct.transferCooldown = 9;
      }
   }

   public int transferCooldown() {
      return this.transferCooldown;
   }
}
