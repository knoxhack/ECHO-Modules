package com.knoxhack.echoashfallprotocol.machine;

import com.knoxhack.echoashfallprotocol.power.PowerIssue;
import com.knoxhack.echoashfallprotocol.power.PowerNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Machine operational states for UX feedback.
 */
public enum MachineState {
    IDLE("Idle", ChatFormatting.GRAY, "Ready - add input, fuel, or a valid recipe"),
    PROCESSING("Processing", ChatFormatting.GREEN, "Working - output will appear when progress completes"),
    UNPOWERED("No Power", ChatFormatting.RED, "Needs generator, battery, power cable, or charged network"),
    JAMMED("Jammed", ChatFormatting.YELLOW, "Maintenance required - repair wear or clear the machine"),
    OFFLINE("Offline", ChatFormatting.DARK_GRAY, "Inactive - check block state, ownership, or required setup"),
    BLOCKED("Output Full", ChatFormatting.GOLD, "Cannot process - clear output slot, pipe, or adjacent inventory"),
    BROWNOUT("Brownout", ChatFormatting.RED, "Connected power exists, but the network is empty"),
    BOTTLENECK("Cable Bottleneck", ChatFormatting.GOLD, "Cable transfer limit is below this machine's FE demand"),
    PRIORITY_PAUSED("Priority Paused", ChatFormatting.YELLOW, "Load Distributor is reserving power for survival systems"),
    CONTROLLER_DISABLED("Controller Disabled", ChatFormatting.YELLOW, "Factory Controller has paused this connected machine"),
    UNSTABLE("Unstable", ChatFormatting.DARK_RED, "Failure risk - repair before running expensive inputs"),
    GENERATING("Generating", ChatFormatting.BLUE, "Producing power - connect machines or charge storage");

    private final String displayName;
    private final ChatFormatting color;
    private final String description;

    MachineState(String displayName, ChatFormatting color, String description) {
        this.displayName = displayName;
        this.color = color;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public String getDescription() {
        return description;
    }

    public Component getTooltip() {
        return Component.literal(displayName).withStyle(color)
                .append(Component.literal(" - " + description).withStyle(ChatFormatting.WHITE));
    }

    public static MachineState blockingPowerState(Level level, BlockPos pos) {
        return fromPowerIssue(PowerNetwork.diagnose(level, pos).issue());
    }

    public static MachineState fromPowerIssue(PowerIssue issue) {
        return switch (issue) {
            case NO_LINK, LOCAL_BUFFER_EMPTY -> UNPOWERED;
            case NETWORK_EMPTY, BLACKOUT_STORAGE_ONLY -> BROWNOUT;
            case CABLE_BOTTLENECK -> BOTTLENECK;
            case PRIORITY_PAUSED -> PRIORITY_PAUSED;
            case CONTROLLER_DISABLED -> CONTROLLER_DISABLED;
            case OK -> null;
        };
    }
}
