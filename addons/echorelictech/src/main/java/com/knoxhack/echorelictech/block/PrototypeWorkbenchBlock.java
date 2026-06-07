package com.knoxhack.echorelictech.block;

import com.knoxhack.echorelictech.api.RelicTechApi;
import com.knoxhack.echorelictech.block.entity.PrototypeWorkbenchBlockEntity;
import com.knoxhack.echorelictech.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class PrototypeWorkbenchBlock extends Block implements EntityBlock {
    public PrototypeWorkbenchBlock(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
        var data = stack.get(ModDataComponents.RELIC_DATA.get());
        if (data == null) {
            player.sendSystemMessage(Component.translatable("block.echorelictech.prototype_workbench.insert_relic"));
            return InteractionResult.SUCCESS;
        }
        String actionKey = RelicTechApi.defaultWorkbenchAction(stack, player.isShiftKeyDown());
        if (RelicTechApi.applyWorkbenchAction(serverPlayer, stack, actionKey)
                && level.getBlockEntity(pos) instanceof PrototypeWorkbenchBlockEntity be) {
            be.setLastAction(actionKey);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PrototypeWorkbenchBlockEntity(pos, state);
    }
}
