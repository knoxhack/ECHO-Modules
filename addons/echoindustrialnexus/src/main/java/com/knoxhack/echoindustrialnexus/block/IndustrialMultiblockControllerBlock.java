package com.knoxhack.echoindustrialnexus.block;

import com.knoxhack.echomultiblockcore.block.MultiblockControllerBlock;
import com.knoxhack.echoindustrialnexus.block.entity.IndustrialMultiblockControllerBlockEntity;
import com.knoxhack.echoindustrialnexus.registry.ModBlockEntities;
import com.knoxhack.echoindustrialnexus.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class IndustrialMultiblockControllerBlock extends MultiblockControllerBlock {
   private final Identifier defaultTaskId;

   public IndustrialMultiblockControllerBlock(Identifier defaultDefinitionId, Identifier defaultTaskId, Properties properties) {
      super(defaultDefinitionId, properties);
      this.defaultTaskId = defaultTaskId;
   }

   public Identifier defaultTaskId() {
      return defaultTaskId;
   }

   @Override
   public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new IndustrialMultiblockControllerBlockEntity(pos, state);
   }

   @Override
   protected InteractionResult useItemOn(
      ItemStack stack,
      BlockState state,
      Level level,
      BlockPos pos,
      Player player,
      InteractionHand hand,
      BlockHitResult hitResult
   ) {
      Identifier upgradeId = upgradeId(stack);
      if (upgradeId == null) {
         return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
      }
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      }
      if (level.getBlockEntity(pos) instanceof IndustrialMultiblockControllerBlockEntity controller
         && controller.installUpgrade(upgradeId, player)) {
         if (!player.getAbilities().instabuild) {
            stack.shrink(1);
            player.setItemInHand(hand, stack);
         }
         return InteractionResult.SUCCESS;
      }
      return InteractionResult.CONSUME;
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return type == ModBlockEntities.INDUSTRIAL_MULTIBLOCK_CONTROLLER.get()
         ? (tickLevel, pos, blockState, blockEntity) -> IndustrialMultiblockControllerBlockEntity.tick(
            tickLevel, pos, blockState, (IndustrialMultiblockControllerBlockEntity)blockEntity)
         : null;
   }

   private static @Nullable Identifier upgradeId(ItemStack stack) {
      if (stack.is(ModItems.SPEED_SERVO.get()) || stack.is(ModItems.SPEED_UPGRADE_CHIP.get())) {
         return Identifier.fromNamespaceAndPath("echoindustrialnexus", "speed_servo");
      }
      if (stack.is(ModItems.EFFICIENCY_COIL.get())) {
         return Identifier.fromNamespaceAndPath("echoindustrialnexus", "efficiency_coil");
      }
      if (stack.is(ModItems.HEAT_SINK_UPGRADE.get()) || stack.is(ModItems.COOLING_UPGRADE_CHIP.get())) {
         return Identifier.fromNamespaceAndPath("echoindustrialnexus", "heat_sink_upgrade");
      }
      if (stack.is(ModItems.NEXUS_STABILIZER_UPGRADE.get())) {
         return Identifier.fromNamespaceAndPath("echoindustrialnexus", "nexus_stabilizer_upgrade");
      }
      if (stack.is(ModItems.FACTORY_LINK_CHIP.get())) {
         return Identifier.fromNamespaceAndPath("echoindustrialnexus", "factory_link_chip");
      }
      if (stack.is(ModItems.OVERCLOCK_CORE.get())) {
         return Identifier.fromNamespaceAndPath("echoindustrialnexus", "overclock_core");
      }
      if (stack.is(ModItems.EMERGENCY_SHUTDOWN_MODULE.get())) {
         return Identifier.fromNamespaceAndPath("echoindustrialnexus", "emergency_shutdown_module");
      }
      return null;
   }
}
