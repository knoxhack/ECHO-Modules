package com.knoxhack.echoterminal.api.mission;

import java.util.List;
import net.minecraft.resources.Identifier;

public record TerminalMissionSnapshot(
        Identifier missionId,
        TerminalMissionStatus status,
        float progress,
        String statusLabel,
        String unlockReason,
        String actionHint,
        List<TerminalMissionAction> actions) {
    public TerminalMissionSnapshot {
        status = status == null ? TerminalMissionStatus.LOCKED : status;
        progress = Math.max(0.0F, Math.min(1.0F, progress));
        statusLabel = statusLabel == null || statusLabel.isBlank() ? status.name() : statusLabel;
        unlockReason = unlockReason == null ? "" : unlockReason;
        actionHint = actionHint == null ? "" : actionHint;
        actions = List.copyOf(actions == null ? List.of() : actions);
    }
}
