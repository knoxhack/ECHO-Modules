package com.knoxhack.echorelictech.item;

import com.knoxhack.echorelictech.api.RelicTechApi;
import com.knoxhack.echorelictech.api.event.RelicTechEvents;
import com.knoxhack.echorelictech.api.relic.RelicCondition;
import com.knoxhack.echorelictech.api.relic.RelicInstanceData;
import com.knoxhack.echorelictech.config.RelicTechConfig;
import com.knoxhack.echorelictech.registry.ModDataComponents;
import com.knoxhack.echorelictech.server.RelicInstabilityManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class GuardianLensItem extends Item {
    public GuardianLensItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;

        var data = stack.get(ModDataComponents.RELIC_DATA.get());
        if (data == null) {
            stack.set(ModDataComponents.RELIC_DATA.get(), new RelicInstanceData(
                Identifier.fromNamespaceAndPath("echorelictech", "guardian_lens"),
                RelicCondition.DAMAGED, 0, BlockPos.ZERO, "", 0, false, false, false, false, 0));
            return InteractionResult.SUCCESS;
        }

        if (!data.identified()) {
            serverPlayer.sendSystemMessage(Component.translatable("item.echorelictech.relic.unidentified"));
            return InteractionResult.FAIL;
        }

        var activation = RelicTechApi.activation(stack);
        int radius = activation.radius() > 0 ? activation.radius() : RelicTechConfig.GUARDIAN_LENS_SCAN_RADIUS.get();
        BlockPos center = player.blockPosition();
        java.util.List<String> signals = new java.util.ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            var blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof com.knoxhack.echorelictech.block.entity.RelicAnalyzerBlockEntity ||
                blockEntity instanceof com.knoxhack.echorelictech.block.entity.ContainmentLockerBlockEntity ||
                blockEntity instanceof com.knoxhack.echorelictech.block.entity.NullBatteryDockBlockEntity ||
                blockEntity instanceof com.knoxhack.echorelictech.block.entity.PrototypeWorkbenchBlockEntity) {
                signals.add(pos.toShortString());
                if (signals.size() >= 4) {
                    break;
                }
            }
        }
        for (BlockPos marker : RelicTechApi.getDiscoveredVaultMarkers(serverPlayer)) {
            if (signals.size() >= 4) {
                break;
            }
            if (marker.distSqr(center) <= (double) radius * radius) {
                signals.add(marker.toShortString());
            }
        }
        if (!signals.isEmpty()) {
            serverPlayer.sendSystemMessage(Component.translatable("item.echorelictech.guardian_lens.detected", radius));
            for (String signal : signals) {
                serverPlayer.sendSystemMessage(Component.translatable("item.echorelictech.guardian_lens.signal", signal));
            }
        } else {
            serverPlayer.sendSystemMessage(Component.translatable("item.echorelictech.guardian_lens.none", radius));
        }
        RelicInstabilityManager.addInstability(serverPlayer, RelicTechApi.instabilityCost(stack, 5));
        RelicTechEvents.fireUse(serverPlayer, Identifier.fromNamespaceAndPath("echorelictech", "guardian_lens"), stack);
        RelicTechApi.tryTriggerFailure(serverPlayer, stack, new com.knoxhack.echorelictech.api.relic.RelicUseContext(level, player, stack, player.blockPosition(), false));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.echorelictech.guardian_lens.scan_radius", RelicTechConfig.GUARDIAN_LENS_SCAN_RADIUS.get()));
        tooltip.accept(Component.translatable("item.echorelictech.guardian_lens.risk"));
    }
}
