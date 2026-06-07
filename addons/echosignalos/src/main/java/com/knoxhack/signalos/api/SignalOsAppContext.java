package com.knoxhack.signalos.api;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side action context for apps that need to interpret payloads with
 * operator and network metadata.
 */
public record SignalOsAppContext(
        ServerPlayer player,
        Identifier appId,
        String networkId,
        int accessTier,
        boolean activeDrivePresent,
        String activeDriveLabel,
        int activeDriveVersion,
        boolean activeDriveWritable,
        String activeDriveStatus) {
    public SignalOsAppContext {
        networkId = networkId == null || networkId.isBlank() ? "offline" : networkId.strip();
        accessTier = Math.max(0, accessTier);
        activeDriveLabel = activeDriveLabel == null || activeDriveLabel.isBlank() ? "No Drive" : activeDriveLabel.strip();
        activeDriveVersion = Math.max(0, activeDriveVersion);
        activeDriveStatus = activeDriveStatus == null || activeDriveStatus.isBlank()
                ? activeDrivePresent ? "READY" : "NO DRIVE"
                : activeDriveStatus.strip();
    }

    public SignalOsAppContext(ServerPlayer player, Identifier appId, String networkId, int accessTier,
            boolean activeDrivePresent, String activeDriveLabel) {
        this(player, appId, networkId, accessTier, activeDrivePresent, activeDriveLabel, 0, false,
                activeDrivePresent ? "READY" : "NO DRIVE");
    }

    public SignalOsAppContext(ServerPlayer player, Identifier appId, String networkId, int accessTier) {
        this(player, appId, networkId, accessTier, false, "No Drive", 0, false, "NO DRIVE");
    }

    public SignalOsActionResult requireWritableDrive() {
        if (!activeDrivePresent) {
            return SignalOsActionResult.failure(SignalOsDriveResultCode.NO_ACTIVE_DRIVE,
                    "[SignalOS] Insert a V2 SignalOS Data Drive first.");
        }
        if (!activeDriveWritable) {
            return SignalOsActionResult.failure(SignalOsDriveResultCode.UNSUPPORTED_DRIVE,
                    "[SignalOS] Active drive is not writable: " + activeDriveStatus + ".");
        }
        return SignalOsActionResult.success("");
    }
}
