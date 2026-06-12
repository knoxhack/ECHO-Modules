package com.knoxhack.echoashfallprotocol.block;

import com.knoxhack.echoashfallprotocol.block.entity.NexusCoreBlockEntity;
import com.knoxhack.echoashfallprotocol.echo.EchoMessages;
import com.knoxhack.echoashfallprotocol.endgame.NexusAccessRules;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The Nexus Core - the endgame block that caused the Gridfall.
 */
public class NexusCoreBlock extends BaseEntityBlock {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final MapCodec<NexusCoreBlock> CODEC = simpleCodec(NexusCoreBlock::new);
    public static final int REQUIRED_NODES = 5;
    public NexusCoreBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, true));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NexusCoreBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            return useNexusCore(level, pos, serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }

    public static InteractionResult useNexusCore(Level level, BlockPos pos, ServerPlayer serverPlayer) {
        if (!(level instanceof ServerLevel serverLevel)
                || !(level.getBlockEntity(pos) instanceof NexusCoreBlockEntity be)) {
            return InteractionResult.CONSUME;
        }
        if (!be.isDiscovered()) {
            be.setDiscovered();
            serverPlayer.sendSystemMessage(Component.literal(
                    EchoMessages.getMessage(EchoMessages.Context.NEXUS_CORE_FOUND)));
        }

        NexusAccessRules.Status access = NexusAccessRules.evaluate(serverPlayer, serverLevel, be);
        if (!access.allowed()) {
            serverPlayer.sendSystemMessage(access.denialMessage());
            return InteractionResult.CONSUME;
        }

        serverPlayer.sendSystemMessage(Component.literal("[NEXUS CORE] " + access.statusText()));
        if (EchoRuntimeModules.isLoaded("echoterminal")) {
            serverPlayer.sendSystemMessage(Component.literal(
                    "[ECHO-7] Nexus interface armed in the ECHO Terminal NEXUS tab. Fallback commands remain: /nexus restore|destroy|control."));
        } else {
            serverPlayer.sendSystemMessage(Component.literal(
                    "[ECHO-7] Nexus interface armed. Use /nexus restore, /nexus destroy, or /nexus control when you are ready to make history permanent."));
        }
        return InteractionResult.SUCCESS;
    }
}
