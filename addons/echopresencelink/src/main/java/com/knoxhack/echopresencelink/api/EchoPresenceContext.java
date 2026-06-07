package com.knoxhack.echopresencelink.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

public record EchoPresenceContext(
        Player player,
        String screenClassName,
        String screenTitle,
        String worldName,
        String serverName,
        BlockPos coordinates,
        boolean privacyMode,
        boolean includeWorldName,
        boolean includeServerName,
        boolean includeCoordinates,
        boolean showButtons,
        long nowEpochSeconds,
        long sessionStartEpochSeconds) {
    public EchoPresenceContext {
        screenClassName = PresenceSanitizer.text(screenClassName, 96, "");
        screenTitle = PresenceSanitizer.text(screenTitle, 96, "");
        worldName = PresenceSanitizer.text(worldName, 96, "");
        serverName = PresenceSanitizer.text(serverName, 96, "");
        coordinates = coordinates == null ? BlockPos.ZERO : coordinates.immutable();
        nowEpochSeconds = Math.max(0L, nowEpochSeconds);
        sessionStartEpochSeconds = Math.max(0L, sessionStartEpochSeconds);
    }

    public String safeWorldName() {
        return privacyMode && !includeWorldName ? "" : worldName;
    }

    public String safeServerName() {
        return privacyMode && !includeServerName ? "" : serverName;
    }

    public String safeCoordinateLine() {
        if (privacyMode && !includeCoordinates) {
            return "";
        }
        return "X " + coordinates.getX() + " Y " + coordinates.getY() + " Z " + coordinates.getZ();
    }
}
