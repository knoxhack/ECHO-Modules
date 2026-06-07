package com.knoxhack.echoashfallprotocol.block;

import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreHazardRuntime;
import com.knoxhack.echoashfallprotocol.block.menu.ResearchLabMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Research Lab Block - core block for the midgame research system.
 * Players interact with this block to open the Research Lab perk tree GUI.
 */
public class ResearchLabBlock extends Block {

    private static final Component CONTAINER_TITLE = Component.translatable("container.EchoAshfallProtocol.research_lab");

    public ResearchLabBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return useResearchLab(level, pos, player);
    }

    public static InteractionResult useResearchLab(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        MenuProvider menuProvider = new SimpleMenuProvider(
            (containerId, playerInventory, p) -> new ResearchLabMenu(containerId, playerInventory),
            CONTAINER_TITLE
        );
        player.openMenu(menuProvider);
        if (player instanceof ServerPlayer serverPlayer) {
            AshfallAdapterCoreHazardRuntime.labObjective(
                    serverPlayer,
                    "research_lab_opened",
                    pos,
                    "research_lab_block_use");
        }
        return InteractionResult.SUCCESS;
    }
}
