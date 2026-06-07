package com.knoxhack.echoashfallprotocol.client;

import com.knoxhack.echoashfallprotocol.block.WorkshopBlock;
import com.knoxhack.echoashfallprotocol.machine.MachineState;
import com.knoxhack.echoashfallprotocol.machine.MachineStateProvider;
import com.knoxhack.echoashfallprotocol.research.PerkEffectHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;
import java.util.Locale;

/**
 * Client-side machine tooltip feedback for workshop and operator bonuses.
 */
public class MachineStateTooltipHandler {

    public static void onItemTooltip(Object event) {
        ClientTooltipEventView view = ClientTooltipEventView.from(event);
        if (view == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        HitResult hitResult = mc.hitResult;
        if (!(hitResult instanceof BlockHitResult blockHit)) {
            return;
        }

        BlockPos pos = blockHit.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        if (!(state.getBlock() instanceof MachineStateProvider provider) || !provider.showStateInTooltip()) {
            return;
        }

        MachineState machineState = provider.getMachineState(mc.level, pos, state);
        List<Component> tooltip = view.tooltip();
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("\u00A78[Field Machine]"));
        tooltip.add(machineState.getTooltip());
        tooltip.add(Component.literal("\u00A77Route fix: \u00A7f" + getRecoveryHint(machineState)));

        if (WorkshopBlock.isInWorkshop(mc.level, pos)) {
            tooltip.add(Component.literal("\u00A76Workshop field\u00A7r +20% speed, -10% power"));
        }

        float perkSpeed = PerkEffectHandler.getMachineSpeedMultiplier(mc.player);
        if (perkSpeed > 1.0F) {
            tooltip.add(Component.literal(
                "\u00A7bOperator training\u00A7r x" + String.format(Locale.ROOT, "%.2f", perkSpeed)
            ));
        }

        if (machineState == MachineState.PROCESSING) {
            tooltip.add(Component.literal("\u00A77Open to inspect progress").withStyle(net.minecraft.ChatFormatting.ITALIC));
        }
    }

    private static String getRecoveryHint(MachineState state) {
        return switch (state) {
            case IDLE -> "add valid input or confirm the recipe";
            case PROCESSING -> "hold route, or add speed/power support";
            case UNPOWERED -> "connect generator, battery, or cable nearby";
            case JAMMED -> "repair wear and clear blocked inventories";
            case OFFLINE -> "confirm placement, activation, and access";
            case BLOCKED -> "clear output or connect item routing";
            case BROWNOUT -> "add fuel, charge storage, or reduce demand";
            case BOTTLENECK -> "upgrade cable path or reduce FE/t demand";
            case PRIORITY_PAUSED -> "change Load Distributor mode or restore charge";
            case CONTROLLER_DISABLED -> "enable the Factory Controller or remove pause rules";
            case UNSTABLE -> "repair before running rare materials";
            case GENERATING -> "connect storage or nearby machines";
        };
    }
}
