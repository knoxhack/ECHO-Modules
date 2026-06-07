package com.knoxhack.echoashfallprotocol.event;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Environmental event types for the world.
 */
public enum EnvironmentalEventType {
    NONE("Clear", ChatFormatting.GRAY, "Conditions normal"),
    RADIATION_STORM("RADIATION STORM", ChatFormatting.GREEN, "Severe rad-spike in progress - seek shelter!"),
    TOXIC_STORM("ACID RAIN", ChatFormatting.DARK_PURPLE, "Corrosive rainfall active - seek cover or keep the expedition short."),
    BLACKOUT("BLACKOUT", ChatFormatting.DARK_GRAY, "Power grid failure - generators offline! Battery banks can keep emergency reserve alive."),
    ASH_STORM("ASH STORM", ChatFormatting.GRAY, "Fine ash reducing visibility - cover filters and ration water."),
    CRYO_FRONT("CRYO FRONT", ChatFormatting.AQUA, "Cryogenic front moving through - find shelter or thermal protection."),
    NEXUS_SURGE("NEXUS SURGE", ChatFormatting.LIGHT_PURPLE, "Anomaly pulse rising - avoid scars and unstable machinery.");

    private final String displayName;
    private final ChatFormatting color;
    private final String warning;

    EnvironmentalEventType(String displayName, ChatFormatting color, String warning) {
        this.displayName = displayName;
        this.color = color;
        this.warning = warning;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public String getWarning() {
        return warning;
    }

    public Component getAlertMessage() {
        return Component.literal("[ECHO-7] ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal("[ " + displayName + " ]").withStyle(color, ChatFormatting.BOLD))
                .append(Component.literal(" - " + warning).withStyle(ChatFormatting.WHITE));
    }
}
