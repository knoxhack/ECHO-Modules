package com.knoxhack.echoashfallprotocol.endgame;

import com.knoxhack.echoashfallprotocol.block.NexusCoreBlock;
import com.knoxhack.echoashfallprotocol.block.entity.NexusCoreBlockEntity;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.guardian.BiomeGuardianProfile;
import com.knoxhack.echoashfallprotocol.guardian.BiomeGuardianProfiles;
import com.knoxhack.echoashfallprotocol.world.NexusCampaignData;
import com.knoxhack.echoashfallprotocol.world.NexusWorldData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class NexusAccessRules {

    private NexusAccessRules() {
    }

    public static Status evaluate(ServerPlayer player, ServerLevel level, NexusCoreBlockEntity core) {
        return evaluate((Player) player, level, core);
    }

    public static Status evaluate(Player player, ServerLevel level, NexusCoreBlockEntity core) {
        return evaluate(QuestData.get(player), level, core);
    }

    public static Status evaluate(QuestData questData, ServerLevel level, NexusCoreBlockEntity core) {
        NexusWorldData worldData = NexusWorldData.get(level.getServer().overworld());
        if (worldData.hasChoiceBeenMade()) {
            String path = stateLabel(worldData.getState());
            String chooser = worldData.getPlayerName();
            String suffix = chooser == null || chooser.isBlank() ? "." : " by " + chooser + ".";
            return Status.denied(true, worldData.getState(), chooser, 0, 0, "",
                    Component.literal("[NEXUS] The Core has already been resolved: " + path + suffix),
                    "SEALED: " + path + suffix);
        }

        if (core == null) {
            return Status.denied(false, NexusWorldData.WorldState.NORMAL, "", 0, 0, "",
                    Component.literal("[ECHO-7] No unresolved Nexus Core interface found within range."),
                    "NO CORE: Stand near an unresolved Nexus Core.");
        }

        int activatedNodes = core.getActivatedNodeCount(level, core.getBlockPos());
        if (core.hasChoiceBeenMade()) {
            return Status.denied(false, NexusWorldData.WorldState.NORMAL, "", activatedNodes, 0, "",
                    Component.literal("[NEXUS] The Core has already been resolved. Its fate is sealed."),
                    "SEALED: Local Core interface is already resolved.");
        }

        GuardianReadiness guardians = guardianReadiness(questData);
        if (guardians.missingCount() > 0) {
            String suffix = guardians.missingCount() == 1 ? "" : "s";
            String statusText = "LOCKED: " + guardians.missingCount() + " guardian signal" + suffix
                    + " unresolved. Next: " + guardians.firstMissingTitle() + ".";
            return Status.denied(false, NexusWorldData.WorldState.NORMAL, "", activatedNodes,
                    guardians.missingCount(), guardians.firstMissingTitle(),
                    Component.literal("[ECHO-7] Nexus Core locked. "
                            + guardians.missingCount()
                            + " guardian signal"
                            + suffix
                            + " unresolved. Next: "
                            + guardians.firstMissingTitle()
                            + "."),
                    statusText);
        }

        if (activatedNodes < NexusCoreBlock.REQUIRED_NODES) {
            String statusText = "LOCKED: " + activatedNodes + "/" + NexusCoreBlock.REQUIRED_NODES
                    + " Power Nodes active near the Core.";
            return Status.denied(false, NexusWorldData.WorldState.NORMAL, "", activatedNodes, 0, "",
                    Component.literal("[ECHO-7] Nexus Core locked. Restore "
                            + (NexusCoreBlock.REQUIRED_NODES - activatedNodes)
                            + " more Power Nodes near the Core."),
                    statusText);
        }

        NexusCampaignData campaign = NexusCampaignData.get(level.getServer().overworld());
        if (!campaign.isWarfrontComplete()
                && questData.isMissionCompleted("find_nexus_core")
                && questData.isMissionCompleted("stabilize_nexus_grid")) {
            campaign.bootstrapWarfrontComplete(core.getBlockPos());
        }
        if (!campaign.isWarfrontComplete()) {
            String statusText = "LOCKED: Warfront "
                    + campaign.getResolvedRelayCount() + "/" + NexusCampaignData.REQUIRED_RELAY_RESOLUTION_COUNT
                    + " relays, siege " + (campaign.isSiegeComplete() ? "complete" : "pending") + ".";
            return Status.denied(false, NexusWorldData.WorldState.NORMAL, "", activatedNodes, 0, "",
                    Component.literal("[ECHO-7] Nexus Warfront unresolved. Resolve three Prime Relays and survive the Core Countermeasure Siege."),
                    statusText);
        }

        return new Status(true, false, NexusWorldData.WorldState.NORMAL, "", activatedNodes,
                NexusCoreBlock.REQUIRED_NODES, 0, "", Component.empty(),
                "READY: " + activatedNodes + "/" + NexusCoreBlock.REQUIRED_NODES
                        + " Power Nodes active. All guardian signals and Warfront checks resolved.");
    }

    public static boolean hasDefeatedAllGuardians(ServerPlayer player) {
        return guardianReadiness(QuestData.get(player)).missingCount() == 0;
    }

    public static String stateLabel(NexusWorldData.WorldState state) {
        return switch (state) {
            case RESTORED -> "RESTORE";
            case DESTROYED -> "DESTROY";
            case CONTROLLED -> "CONTROL";
            case NORMAL -> "NONE";
        };
    }

    private static GuardianReadiness guardianReadiness(QuestData quest) {
        int missingCount = 0;
        String firstMissingTitle = "";

        for (BiomeGuardianProfile profile : BiomeGuardianProfiles.all()) {
            if (isGuardianDefeated(quest, profile)) {
                continue;
            }
            missingCount++;
            if (firstMissingTitle.isEmpty()) {
                firstMissingTitle = profile.title();
            }
        }

        return new GuardianReadiness(missingCount, firstMissingTitle);
    }

    private static boolean isGuardianDefeated(QuestData quest, BiomeGuardianProfile profile) {
        return quest.isMissionCompleted(profile.missionId()) || quest.getEntityKills(profile.entityId()) >= 1;
    }

    private record GuardianReadiness(int missingCount, String firstMissingTitle) {
    }

    public record Status(
            boolean allowed,
            boolean worldResolved,
            NexusWorldData.WorldState worldState,
            String chooserName,
            int activatedNodes,
            int requiredNodes,
            int missingGuardianCount,
            String firstMissingGuardianTitle,
            Component denialMessage,
            String statusText
    ) {
        private static Status denied(boolean worldResolved, NexusWorldData.WorldState worldState, String chooserName,
                                     int activatedNodes, int missingGuardianCount, String firstMissingGuardianTitle,
                                     Component denialMessage, String statusText) {
            return new Status(false, worldResolved, worldState, chooserName, activatedNodes,
                    NexusCoreBlock.REQUIRED_NODES, missingGuardianCount, firstMissingGuardianTitle,
                    denialMessage, statusText);
        }
    }
}
