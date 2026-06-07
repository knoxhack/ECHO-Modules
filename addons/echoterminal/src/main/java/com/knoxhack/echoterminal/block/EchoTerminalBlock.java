package com.knoxhack.echoterminal.block;

import com.knoxhack.echoterminal.block.entity.EchoTerminalBlockEntity;
import com.knoxhack.echoterminal.menu.EchoTerminalMenu;
import com.knoxhack.echoterminal.service.EchoTerminalCoreServices;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class EchoTerminalBlock extends Block implements EntityBlock {
    private static final Component CONTAINER_TITLE = Component.translatable("container.echoterminal.echo_terminal");

    public EchoTerminalBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EchoTerminalBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof EchoTerminalBlockEntity terminal && placer instanceof Player player) {
            terminal.setOwnerIfMissing(player);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        EchoTerminalCoreServices.rememberTerminal(player, pos);
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof EchoTerminalBlockEntity terminal) {
                terminal.setOwnerIfMissing(player);
                terminal.recordActivity();
            }
            MenuProvider menuProvider = new SimpleMenuProvider(
                    (containerId, playerInventory, p) -> new EchoTerminalMenu(
                            containerId,
                            playerInventory,
                            ContainerLevelAccess.create(level, pos)),
                    CONTAINER_TITLE);
            player.openMenu(menuProvider);
        }
        return InteractionResult.SUCCESS;
    }
}
