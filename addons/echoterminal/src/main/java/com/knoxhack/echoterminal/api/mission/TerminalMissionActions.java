package com.knoxhack.echoterminal.api.mission;

import com.knoxhack.echoterminal.EchoTerminal;
import com.knoxhack.echoterminal.api.TerminalActionContext;
import com.knoxhack.echoterminal.api.TerminalActionRegistry;
import com.knoxhack.echoterminal.integration.TerminalRuntimeSpineBridge;
import com.knoxhack.echoterminal.player.TerminalPlayerData;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class TerminalMissionActions {
    public static final Identifier MISSION_ACTION =
            Identifier.fromNamespaceAndPath(EchoTerminal.MODID, "mission_action");
    public static final Identifier TRACK_MISSION =
            Identifier.fromNamespaceAndPath(EchoTerminal.MODID, "track_mission");

    private TerminalMissionActions() {
    }

    public static void registerForTab(Identifier tabId) {
        TerminalActionRegistry.register(tabId, MISSION_ACTION,
                TerminalMissionActions::handleMissionAction,
                TerminalMissionActions::validateMissionAction);
        TerminalActionRegistry.register(tabId, TRACK_MISSION,
                TerminalMissionActions::handleTrackMission,
                TerminalMissionActions::validateTrackMission);
    }

    public static String payload(Identifier chapterId, Identifier missionId, String actionId) {
        return chapterId + "|" + missionId + "|" + (actionId == null ? "" : actionId);
    }

    public static String trackingPayload(Identifier tabId, Identifier chapterId, Identifier missionId, boolean clear) {
        return chapterId + "|" + missionId + "|" + (clear ? "clear" : "track") + ";" + tabId;
    }

    private static boolean validateMissionAction(TerminalActionContext context) {
        if (context.player() == null) {
            return true;
        }
        ParsedMissionAction parsed = parse(context.payload());
        if (parsed == null) {
            context.player().sendSystemMessage(Component.literal("[ECHO-7] Invalid mission action payload."), true);
            return false;
        }
        return TerminalMissionRegistry.provider(parsed.chapterId())
                .map(provider -> actionAvailable(context.player(), provider, parsed.missionId(), parsed.actionId()))
                .orElseGet(() -> {
                    context.player().sendSystemMessage(Component.literal("[ECHO-7] Mission provider offline."), true);
                    return false;
                });
    }

    private static boolean validateTrackMission(TerminalActionContext context) {
        if (context.player() == null) {
            return true;
        }
        ParsedMissionAction parsed = parse(context.payload());
        if (parsed == null) {
            context.player().sendSystemMessage(Component.literal("[ECHO-7] Invalid tracking payload."), true);
            return false;
        }
        if ("clear".equals(parsed.actionId())) {
            return true;
        }
        return TerminalMissionRegistry.provider(parsed.chapterId())
                .map(provider -> missionExists(context.player(), provider, parsed.missionId()))
                .orElse(false);
    }

    private static void handleMissionAction(ServerPlayer player, String payload) {
        ParsedMissionAction parsed = parse(payload);
        if (parsed == null) {
            if (player != null) {
                player.sendSystemMessage(Component.literal("[ECHO-7] Invalid mission action payload."), true);
            }
            return;
        }
        if (player == null) {
            TerminalMissionRegistry.provider(parsed.chapterId())
                    .ifPresent(provider -> provider.handleAction(null, parsed.missionId(), parsed.actionId()));
            return;
        }
        boolean handled = TerminalMissionRegistry.provider(parsed.chapterId())
                .map(provider -> provider.handleAction(player, parsed.missionId(), parsed.actionId()))
                .orElse(false);
        if (handled) {
            TerminalRuntimeSpineBridge.publishAction(
                    player,
                    TerminalRuntimeSpineBridge.TERMINAL_MISSION_ACTION,
                    parsed.missionId(),
                    parsed.tabIdFallback(),
                    MISSION_ACTION,
                    Map.of(
                            "chapter_id", parsed.chapterId().toString(),
                            "mission_id", parsed.missionId().toString(),
                            "mission_action", parsed.actionId()));
        } else {
            player.sendSystemMessage(Component.literal("[ECHO-7] Mission action rejected."), true);
        }
    }

    private static void handleTrackMission(ServerPlayer player, String payload) {
        if (player == null) {
            return;
        }
        ParsedMissionAction parsed = parse(payload);
        if (parsed == null) {
            player.sendSystemMessage(Component.literal("[ECHO-7] Invalid tracking payload."), true);
            return;
        }
        TerminalPlayerData data = TerminalPlayerData.get(player);
        if ("clear".equals(parsed.actionId())) {
            data.clearTrackedMission();
            TerminalPlayerData.saveAndSync(player, data);
            TerminalRuntimeSpineBridge.publishAction(
                    player,
                    TerminalRuntimeSpineBridge.TERMINAL_MISSION_UNTRACKED,
                    parsed.missionId(),
                    parsed.tabIdFallback(),
                    TRACK_MISSION,
                    Map.of(
                            "chapter_id", parsed.chapterId().toString(),
                            "mission_id", parsed.missionId().toString(),
                            "tracking_state", "clear"));
            player.sendSystemMessage(Component.literal("[ECHO-7] Mission tracking cleared."), true);
            return;
        }
        TerminalMissionRegistry.provider(parsed.chapterId()).ifPresentOrElse(provider -> {
            List<TerminalMissionDefinition> missions = safeMissions(player, provider);
            TerminalMissionDefinition definition = missions.stream()
                    .filter(candidate -> candidate.id().equals(parsed.missionId()))
                    .findFirst()
                    .orElse(null);
            if (definition == null) {
                player.sendSystemMessage(Component.literal("[ECHO-7] Mission tracking target offline."), true);
                return;
            }
            TerminalMissionSnapshot snapshot = provider.snapshot(player, definition.id());
            TerminalMissionPresentation presentation = provider.presentation(player, definition, snapshot);
            String title = presentation == null || presentation.shortTitle().isBlank()
                    ? definition.title()
                    : presentation.shortTitle();
            String nextStep = presentation == null ? "" : presentation.nextStep();
            String actionHint = snapshot == null ? "" : snapshot.actionHint();
            data.trackMission(
                    parsed.tabIdFallback(),
                    parsed.chapterId(),
                    definition.id(),
                    title,
                    nextStep.isBlank() ? actionHint : nextStep,
                    provider.chapter().accentColor(),
                    player.tickCount);
            TerminalPlayerData.saveAndSync(player, data);
            TerminalRuntimeSpineBridge.publishAction(
                    player,
                    TerminalRuntimeSpineBridge.TERMINAL_MISSION_TRACKED,
                    definition.id(),
                    parsed.tabIdFallback(),
                    TRACK_MISSION,
                    Map.of(
                            "chapter_id", parsed.chapterId().toString(),
                            "mission_id", definition.id().toString(),
                            "tracking_state", "track"));
            player.sendSystemMessage(Component.literal("[ECHO-7] Tracking " + title + "."), true);
        }, () -> player.sendSystemMessage(Component.literal("[ECHO-7] Mission provider offline."), true));
    }

    private static ParsedMissionAction parse(String payload) {
        String[] parts = payload == null ? new String[0] : payload.split("\\|", 3);
        if (parts.length != 3) {
            return null;
        }
        Identifier chapterId = Identifier.tryParse(parts[0]);
        Identifier missionId = Identifier.tryParse(parts[1]);
        String actionId = parts[2];
        Identifier tabId = null;
        int tabSeparator = actionId.indexOf(';');
        if (tabSeparator >= 0) {
            tabId = Identifier.tryParse(actionId.substring(tabSeparator + 1));
            actionId = actionId.substring(0, tabSeparator);
        }
        if (chapterId == null || missionId == null || actionId == null || actionId.isBlank()) {
            return null;
        }
        return new ParsedMissionAction(tabId, chapterId, missionId, actionId);
    }

    private static boolean missionExists(
            ServerPlayer player,
            TerminalMissionProvider provider,
            Identifier missionId) {
        try {
            return safeMissions(player, provider).stream().anyMatch(mission -> mission.id().equals(missionId));
        } catch (RuntimeException exception) {
            EchoTerminal.LOGGER.warn("Terminal mission provider failed while validating tracking target.", exception);
            return false;
        }
    }

    private static boolean actionAvailable(
            ServerPlayer player,
            TerminalMissionProvider provider,
            Identifier missionId,
            String actionId) {
        try {
            List<TerminalMissionDefinition> missions = safeMissions(player, provider);
            boolean missionExists = missions.stream().anyMatch(mission -> mission.id().equals(missionId));
            if (!missionExists) {
                player.sendSystemMessage(Component.literal("[ECHO-7] Mission record no longer exists."), true);
                return false;
            }
            TerminalMissionSnapshot snapshot = provider.snapshot(player, missionId);
            boolean available = snapshot.actions().stream()
                    .anyMatch(action -> action.enabled() && action.id().equals(actionId));
            if (!available) {
                player.sendSystemMessage(Component.literal("[ECHO-7] Mission command is not currently available."), true);
            }
            return available;
        } catch (RuntimeException exception) {
            EchoTerminal.LOGGER.warn("Terminal mission provider failed while validating action.", exception);
            player.sendSystemMessage(Component.literal("[ECHO-7] Mission command rejected by provider."), true);
            return false;
        }
    }

    private static List<TerminalMissionDefinition> safeMissions(
            ServerPlayer player,
            TerminalMissionProvider provider) {
        List<TerminalMissionDefinition> missions = provider.missions(player);
        return missions == null ? List.of() : missions;
    }

    private record ParsedMissionAction(Identifier tabId, Identifier chapterId, Identifier missionId, String actionId) {
        Identifier tabIdFallback() {
            return tabId == null ? chapterId : tabId;
        }
    }
}
