package com.knoxhack.echoashfallprotocol.block;

import com.knoxhack.echoashfallprotocol.power.PowerNetwork;
import com.knoxhack.echoashfallprotocol.power.PowerDiagnostic;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class EnergyMeterBlock extends Block {
    public static final MapCodec<EnergyMeterBlock> CODEC = simpleCodec(EnergyMeterBlock::new);

    public EnergyMeterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            PowerNetwork.NetworkReport report = PowerNetwork.scan(level, pos);
            PowerDiagnostic diagnostic = PowerNetwork.diagnose(level, pos, Math.max(1, report.estimatedDemand()));
            serverPlayer.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.energy_meter.header"));
            serverPlayer.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.energy_meter.energy",
                    report.storedEnergy(), report.capacity()));
            serverPlayer.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.energy_meter.transfer",
                    report.bottleneckLabel(), report.relayCount(), report.sourceCount()));
            serverPlayer.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.energy_meter.demand",
                    report.estimatedDemand(), report.priorityMode().getDisplayName()));
            serverPlayer.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.energy_meter.issue",
                    Component.translatable(diagnostic.issue().translationKey()),
                    Component.translatable(diagnostic.hintKey())));
        }
        return InteractionResult.SUCCESS;
    }
}
