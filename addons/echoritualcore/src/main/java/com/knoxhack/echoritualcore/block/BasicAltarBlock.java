package com.knoxhack.echoritualcore.block;

import com.knoxhack.echoritualcore.api.RitualCoreApi;
import com.knoxhack.echoritualcore.block.entity.BasicAltarBlockEntity;
import com.knoxhack.echoritualcore.menu.RitualAltarMenu;
import com.knoxhack.echoritualcore.ritual.RitualExecutionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class BasicAltarBlock extends Block implements EntityBlock {
    public BasicAltarBlock(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BasicAltarBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            RitualExecutionContext context = RitualExecutionContext.create(serverPlayer, pos);
            if (player.isShiftKeyDown()) {
                context.sendStructureDiagnostic();
                context.updateAltar(null, null, BasicAltarBlockEntity.RESULT_READY,
                        "Diagnostic scan: " + context.structure().summary());
            } else {
                serverPlayer.openMenu(new SimpleMenuProvider(
                        (containerId, inventory, p) -> new RitualAltarMenu(containerId, inventory, level, pos),
                        Component.translatable("block.echoritualcore.basic_altar")), pos);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            boolean performed = RitualCoreApi.tryAltarRitual(serverPlayer, stack, player.isShiftKeyDown(), pos);
            return performed ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }
}
