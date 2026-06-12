package com.knoxhack.echoashfallprotocol.endgame;

import com.knoxhack.echoashfallprotocol.block.NexusCoreBlock;
import com.knoxhack.echoashfallprotocol.block.entity.NexusCoreBlockEntity;
import com.knoxhack.echoashfallprotocol.echo.Mission;
import com.knoxhack.echoashfallprotocol.echo.MissionRegistry;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreLateRuntime;
import com.knoxhack.echoashfallprotocol.network.NexusStatePacket;
import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import com.knoxhack.echoashfallprotocol.world.NexusCampaignData;
import com.knoxhack.echoashfallprotocol.world.NexusWorldData;
import com.echoplatform.echocore.api.network.EchoPacketKind;
import com.knoxhack.echonetcore.api.EchoNetSend;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Authoritative server actions for the Nexus Warfront and path finale layer.
 */
public final class NexusCampaignActions {
    private static final int CORE_SEARCH_HORIZONTAL = 24;
    private static final int CORE_SEARCH_VERTICAL = 10;

    private NexusCampaignActions() {
    }

    public static boolean handleTerminalAction(ServerPlayer player, String payload) {
        String command = normalize(payload);
        if (command.isBlank() || "status".equals(command)) {
            return sendStatus(player);
        }
        if ("awaken".equals(command)) {
            return awakenCore(player);
        }
        if ("scan".equals(command) || "scan_relays".equals(command)) {
            return scanPrimeRelays(player);
        }
        if ("siege".equals(command) || "survive_siege".equals(command)) {
            return surviveCoreCountermeasure(player);
        }
        if ("operation".equals(command) || "path_operation".equals(command)) {
            return completePathOperation(player);
        }
        if ("finale".equals(command) || "final_boss".equals(command)) {
            return completeFinale(player);
        }
        if ("encounter".equals(command)) {
            return NexusRelaySiteService.startNextEncounter(player);
        }
        if (command.startsWith("encounter:")) {
            String[] parts = command.split(":");
            if (parts.length >= 2) {
                return NexusRelaySiteService.startOrRecoverEncounter(player, NexusRelayType.byName(parts[1]));
            }
        }
        if (command.startsWith("relay:")) {
            String[] parts = command.split(":");
            if (parts.length >= 3) {
                return resolveRelay(player, parts[1], parts[2]);
            }
            if (parts.length == 2) {
                return resolveNextRelay(player, parts[1]);
            }
        }
        player.sendSystemMessage(Component.literal(
                "[ECHO-7] Unknown Nexus Warfront command. Valid field actions: status, awaken, scan, relay, siege, operation, finale."));
        return false;
    }

    public static boolean awakenCore(ServerPlayer player) {
        ServerLevel level = overworld(player);
        if (level == null) {
            return false;
        }
        NexusCoreBlockEntity core = findUnresolvedCore((ServerLevel) player.level(), player.blockPosition());
        if (core == null) {
            player.sendSystemMessage(Component.literal(
                    "[ECHO-7] Stand near an unresolved Nexus Core. ECHO cannot wake a signal it cannot anchor."));
            return false;
        }

        NexusCampaignData campaign = NexusCampaignData.get(level);
        campaign.awaken(core.getBlockPos());
        AshfallAdapterCoreLateRuntime.nexusState(
                player,
                campaign,
                NexusWorldData.get(level),
                "awakened",
                "nexus_awaken");
        QuestData quest = QuestData.get(player);
        quest.visitLocation("special", "nexus:awakened");
        QuestData.saveAndSync(player, quest);
        completeReadyMission(player, "awaken_nexus_core");
        syncCampaignState(level);
        player.sendSystemMessage(Component.literal(
                "[NEXUS] Core awake. Instability " + campaign.getInstability() + "%. Prime Relay scan advised."));
        return true;
    }

    public static boolean scanPrimeRelays(ServerPlayer player) {
        ServerLevel level = overworld(player);
        if (level == null) {
            return false;
        }
        NexusCampaignData campaign = NexusCampaignData.get(level);
        if (!campaign.isAwakened() && !awakenCore(player)) {
            return false;
        }
        campaign.scanRelays();
        NexusRelaySiteService.ensureSitesAssignedAndGenerated(level, campaign, player.blockPosition());
        AshfallAdapterCoreLateRuntime.nexusState(
                player,
                campaign,
                NexusWorldData.get(level),
                "prime_relays_scanned",
                "nexus_scan_relays");

        QuestData quest = QuestData.get(player);
        quest.visitLocation("special", "nexus:prime_relays_scanned");
        for (String poi : primeRelayPoiIds()) {
            quest.discoverPOI(poi);
        }
        QuestData.saveAndSync(player, quest);
        completeReadyMission(player, "scan_prime_relays");
        syncCampaignState(level);
        player.sendSystemMessage(Component.literal(
                "[ECHO-7] Six Prime Relay signatures indexed. Resolve at least three: /nexus relay <type> <stabilize|sever|override>."));
        return true;
    }

    public static boolean resolveNextRelay(ServerPlayer player, String action) {
        ServerLevel level = overworld(player);
        if (level == null) {
            return false;
        }
        NexusCampaignData campaign = NexusCampaignData.get(level);
        NexusRelayType type = campaign.firstEncounterCompleteUnresolvedRelay();
        if (type == null) {
            player.sendSystemMessage(Component.literal("[ECHO-7] No encounter-complete unresolved relay is ready. Use /nexus encounter <type> first."));
            return false;
        }
        return resolveRelay(player, type.name(), action);
    }

    public static boolean resolveRelay(ServerPlayer player, String typeRaw, String actionRaw) {
        ServerLevel level = overworld(player);
        if (level == null) {
            return false;
        }
        NexusCampaignData campaign = NexusCampaignData.get(level);
        if (!campaign.isAwakened()) {
            player.sendSystemMessage(Component.literal("[ECHO-7] Awaken the Nexus Core before touching relay outcomes."));
            return false;
        }
        if (campaign.getScannedRelayCount() < NexusCampaignData.REQUIRED_RELAY_SCAN_COUNT) {
            player.sendSystemMessage(Component.literal("[ECHO-7] Scan Prime Relays first. Blind relay work lets the Core choose the cost."));
            return false;
        }

        NexusRelayType type = NexusRelayType.byName(typeRaw);
        NexusRelayState outcome = parseOutcome(actionRaw);
        if (type == null || outcome == null) {
            player.sendSystemMessage(Component.literal(
                    "[ECHO-7] Relay syntax: /nexus relay <reactor|cryo|bio|transit|industrial|scar> <stabilize|sever|override>."));
            return false;
        }
        NexusRelaySiteService.ensureSitesAssignedAndGenerated(level, campaign, player.blockPosition());
        if (!campaign.isRelayEncounterComplete(type)) {
            player.sendSystemMessage(Component.literal(
                    "[ECHO-7] " + type.displayName() + " still has an active field objective. Use /nexus encounter "
                            + type.name().toLowerCase(Locale.ROOT) + " and clear the site before choosing its outcome."));
            return false;
        }
        if (!campaign.resolveRelay(type, outcome)) {
            player.sendSystemMessage(Component.literal(
                    "[ECHO-7] " + type.displayName() + " already reports " + campaign.getRelayState(type).name() + "."));
            return false;
        }

        PostNexusData post = PostNexusData.get(player);
        post.setRelaysResolved(Math.max(post.getRelaysResolved(), campaign.getResolvedRelayCount()));
        PostNexusData.saveAndSync(player, post);
        AshfallAdapterCoreLateRuntime.postNexusPersisted(player, post, "prime_relay_resolution");
        AshfallAdapterCoreLateRuntime.primeRelayResolved(
                player,
                type,
                outcome,
                campaign,
                "prime_relay_resolution");
        shareRelayProgress(level, campaign);

        QuestData quest = QuestData.get(player);
        quest.visitLocation("special", "nexus:relay:" + type.name().toLowerCase(Locale.ROOT));
        quest.visitLocation("special", "nexus:relay:" + type.name().toLowerCase(Locale.ROOT)
                + ":" + outcome.name().toLowerCase(Locale.ROOT));
        QuestData.saveAndSync(player, quest);
        completeReadyMission(player, "resolve_prime_relays");
        grantRelayReward(player, outcome);
        syncCampaignState(level);
        player.sendSystemMessage(Component.literal(
                "[NEXUS] " + type.displayName() + " " + outcome.name()
                        + ". Relays resolved: " + campaign.getResolvedRelayCount()
                        + "/" + NexusCampaignData.REQUIRED_RELAY_RESOLUTION_COUNT + "."));
        return true;
    }

    public static boolean surviveCoreCountermeasure(ServerPlayer player) {
        ServerLevel level = overworld(player);
        if (level == null) {
            return false;
        }
        NexusCampaignData campaign = NexusCampaignData.get(level);
        if (campaign.isSiegeComplete()) {
            player.sendSystemMessage(Component.literal("[ECHO-7] Core countermeasure already survived. Siege reward is sealed."));
            return false;
        }
        if (campaign.getResolvedRelayCount() < NexusCampaignData.REQUIRED_RELAY_RESOLUTION_COUNT) {
            player.sendSystemMessage(Component.literal("[ECHO-7] Resolve at least three Prime Relays before provoking the Core countermeasure."));
            return false;
        }
        NexusCoreBlockEntity core = findUnresolvedCore((ServerLevel) player.level(), player.blockPosition());
        if (core == null || core.getActivatedNodeCount(player.level(), core.getBlockPos()) < NexusCoreBlock.REQUIRED_NODES) {
            player.sendSystemMessage(Component.literal(
                    "[ECHO-7] Core countermeasure requires five active Power Nodes beside the unresolved Core."));
            return false;
        }

        campaign.markSiegeComplete();
        PostNexusData post = PostNexusData.get(player);
        post.setSiegesSurvived(Math.max(1, post.getSiegesSurvived()));
        PostNexusData.saveAndSync(player, post);
        AshfallAdapterCoreLateRuntime.postNexusPersisted(player, post, "nexus_countermeasure");
        AshfallAdapterCoreLateRuntime.nexusState(
                player,
                campaign,
                NexusWorldData.get(level),
                "siege_complete",
                "nexus_countermeasure");
        shareSiegeProgress(level);
        completeReadyMission(player, "survive_core_countermeasure");
        grantSiegeReward(player);
        syncCampaignState(level);
        player.sendSystemMessage(Component.literal(
                "[NEXUS] Core countermeasure survived. Final path operations are now reachable."));
        return true;
    }

    public static boolean completePathOperation(ServerPlayer player) {
        ServerLevel level = overworld(player);
        if (level == null) {
            return false;
        }
        PostNexusData post = PostNexusData.get(player);
        if (!post.hasMadeChoice() || !post.isWardenDefeated()) {
            player.sendSystemMessage(Component.literal(
                    "[ECHO-7] Path operations unlock after the Nexus choice and Warden defeat. The archive still has teeth."));
            return false;
        }
        NexusCampaignData campaign = NexusCampaignData.get(level);
        if (campaign.isFinaleComplete() || post.isFinalBossDefeated()) {
            player.sendSystemMessage(Component.literal("[ECHO-7] Final branch signal is already sealed for this path."));
            return false;
        }
        if (post.getPathOperationsComplete() < 1) {
            post.incrementPathOperationsComplete();
            grantPathOperationReward(player, post.getSelectedPath());
        }
        PostNexusData.saveAndSync(player, post);
        AshfallAdapterCoreLateRuntime.postNexusPersisted(player, post, "nexus_path_operation");
        AshfallAdapterCoreLateRuntime.nexusState(
                player,
                campaign,
                NexusWorldData.get(level),
                "path_operation",
                "nexus_path_operation");
        completeReadyMission(player, pathOperationMissionId(post.getSelectedPath()));
        NexusRelaySiteService.spawnFinalBoss(player, post.getSelectedPath());
        syncCampaignState(level);
        player.sendSystemMessage(Component.literal(pathOperationMessage(post.getSelectedPath())));
        return true;
    }

    public static boolean completeFinale(ServerPlayer player) {
        ServerLevel level = overworld(player);
        if (level == null) {
            return false;
        }
        PostNexusData post = PostNexusData.get(player);
        if (!post.hasMadeChoice() || !post.isWardenDefeated() || post.getPathOperationsComplete() < 1) {
            player.sendSystemMessage(Component.literal(
                    "[ECHO-7] Defeat the Warden and complete one path operation before confronting the final branch signal."));
            return false;
        }
        NexusCampaignData campaign = NexusCampaignData.get(level);
        if (campaign.isFinaleComplete() && !post.isFinalBossDefeated()) {
            player.sendSystemMessage(Component.literal("[ECHO-7] Shared finale seal found. Applying local finale credit."));
            return creditFinaleBoss(player, post.getSelectedPath());
        }
        if (!post.isFinalBossDefeated()) {
            if (NexusRelaySiteService.hasLiveFinalBoss(level, campaign, post.getSelectedPath())) {
                player.sendSystemMessage(Component.literal("[ECHO-7] Final boss signal is live. Defeat it before claiming finale credit."));
                return false;
            }
            if (!campaign.isFinalBossSummonedFor(post.getSelectedPath())) {
                NexusRelaySiteService.spawnFinalBoss(player, post.getSelectedPath());
                return false;
            }
            player.sendSystemMessage(Component.literal("[ECHO-7] No live final boss found. Applying recovery finale credit."));
            return creditFinaleBoss(player, post.getSelectedPath());
        }
        PostNexusData.saveAndSync(player, post);
        campaign.markFinaleComplete();
        AshfallAdapterCoreLateRuntime.nexusState(
                player,
                campaign,
                NexusWorldData.get(level),
                "finale_complete",
                "nexus_finale");
        completeReadyMission(player, finaleMissionId(post.getSelectedPath()));
        syncCampaignState(level);
        player.sendSystemMessage(Component.literal(finaleMessage(post.getSelectedPath())));
        return true;
    }

    public static boolean startRelayEncounter(ServerPlayer player, String typeRaw) {
        return NexusRelaySiteService.startOrRecoverEncounter(player, NexusRelayType.byName(typeRaw));
    }

    public static boolean creditFinaleBoss(ServerPlayer player, PostNexusData.NexusPath path) {
        if (player == null || path == null || path == PostNexusData.NexusPath.NONE) {
            return false;
        }
        ServerLevel level = overworld(player);
        if (level == null) {
            return false;
        }
        PostNexusData post = PostNexusData.get(player);
        if (!post.isPath(path)) {
            player.sendSystemMessage(Component.literal("[ECHO-7] Finale boss credit rejected: selected path mismatch."));
            return false;
        }
        NexusCampaignData campaign = NexusCampaignData.get(level);
        int credited = 0;
        credited += creditFinaleParticipant(player, path, true) ? 1 : 0;
        AABB sharedCreditBox = new AABB(player.blockPosition()).inflate(96.0D);
        ServerLevel playerLevel = (ServerLevel) player.level();
        for (ServerPlayer candidate : playerLevel.getEntitiesOfClass(ServerPlayer.class, sharedCreditBox,
                candidate -> candidate != player && PostNexusData.get(candidate).isPath(path))) {
            credited += creditFinaleParticipant(candidate, path, false) ? 1 : 0;
        }
        if (credited <= 0 && campaign.isFinaleComplete()) {
            syncCampaignState(level);
            return false;
        }
        boolean campaignWasComplete = campaign.isFinaleComplete();
        campaign.markFinaleComplete();
        campaign.clearFinalBossSummoned();
        syncCampaignState(level);
        return credited > 0 || !campaignWasComplete;
    }

    public static boolean sendStatus(ServerPlayer player) {
        ServerLevel level = overworld(player);
        if (level == null) {
            return false;
        }
        NexusCampaignData campaign = NexusCampaignData.get(level);
        player.sendSystemMessage(Component.literal("[NEXUS WARFRONT] " + campaign.statusLine()));
        for (String line : campaign.relaySummaryLines()) {
            player.sendSystemMessage(Component.literal("[NEXUS RELAY] " + line));
        }
        return true;
    }

    public static void syncCampaignState(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        NexusStatePacket packet = NexusStatePacket.fromWorldData(
                NexusWorldData.get(overworld), NexusCampaignData.get(overworld));
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            EchoNetSend.toPlayer(player, packet, EchoPacketKind.CLIENTBOUND_SYNC);
        }
    }

    public static void syncCampaignState(ServerPlayer player) {
        ServerLevel level = overworld(player);
        if (level == null) {
            return;
        }
        EchoNetSend.toPlayer(player, NexusStatePacket.fromWorldData(
                NexusWorldData.get(level), NexusCampaignData.get(level)), EchoPacketKind.CLIENTBOUND_SYNC);
    }

    private static ServerLevel overworld(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel playerLevel)) {
            return null;
        }
        return playerLevel.getServer().overworld();
    }

    private static NexusCoreBlockEntity findUnresolvedCore(ServerLevel level, BlockPos playerPos) {
        if (level.dimension() != Level.OVERWORLD) {
            return null;
        }
        for (BlockPos cursor : BlockPos.betweenClosed(
                playerPos.offset(-CORE_SEARCH_HORIZONTAL, -CORE_SEARCH_VERTICAL, -CORE_SEARCH_HORIZONTAL),
                playerPos.offset(CORE_SEARCH_HORIZONTAL, CORE_SEARCH_VERTICAL, CORE_SEARCH_HORIZONTAL))) {
            BlockState state = level.getBlockState(cursor);
            if (!state.is(ModBlocks.NEXUS_CORE.get())) {
                continue;
            }
            BlockEntity be = level.getBlockEntity(cursor);
            if (be instanceof NexusCoreBlockEntity core && !core.hasChoiceBeenMade()) {
                return core;
            }
        }
        return null;
    }

    private static NexusRelayState parseOutcome(String actionRaw) {
        return switch (normalize(actionRaw)) {
            case "stabilize", "restore", "stabilized" -> NexusRelayState.STABILIZED;
            case "sever", "destroy", "severed" -> NexusRelayState.SEVERED;
            case "override", "control", "overridden" -> NexusRelayState.OVERRIDDEN;
            default -> null;
        };
    }

    private static void completeReadyMission(ServerPlayer player, String missionId) {
        if (missionId == null || missionId.isBlank()) {
            return;
        }
        QuestData quest = QuestData.get(player);
        Mission mission = MissionRegistry.getMissionById(missionId);
        if (mission == null || quest.isMissionCompleted(missionId) || !mission.isComplete(player)) {
            if (quest.repairMissionState(player)) {
                QuestData.saveAndSync(player, quest);
            }
            return;
        }
        quest.completeMission(player, missionId, mission.rewards());
        quest.clearTurnInReminder(missionId);
        quest.repairMissionState(player);
        QuestData.saveAndSync(player, quest);
    }

    private static void shareRelayProgress(ServerLevel level, NexusCampaignData campaign) {
        int resolved = campaign.getResolvedRelayCount();
        for (ServerPlayer candidate : level.getServer().getPlayerList().getPlayers()) {
            PostNexusData post = PostNexusData.get(candidate);
            if (post.getRelaysResolved() < resolved) {
                post.setRelaysResolved(resolved);
                PostNexusData.saveAndSync(candidate, post);
            }
        }
    }

    private static void shareSiegeProgress(ServerLevel level) {
        for (ServerPlayer candidate : level.getServer().getPlayerList().getPlayers()) {
            PostNexusData post = PostNexusData.get(candidate);
            if (post.getSiegesSurvived() < 1) {
                post.setSiegesSurvived(1);
                PostNexusData.saveAndSync(candidate, post);
            }
        }
    }

    private static boolean creditFinaleParticipant(ServerPlayer player, PostNexusData.NexusPath path, boolean primary) {
        PostNexusData post = PostNexusData.get(player);
        if (!post.isPath(path)) {
            return false;
        }
        boolean changed = false;
        if (!post.isFinalBossDefeated()) {
            post.setFinalBossDefeated(true);
            grantFinaleReward(player, path);
            QuestData quest = QuestData.get(player);
            quest.addToArchive("[NEXUS FINALE] " + path.name()
                    + " terminal ability unlocked. Final protocol record ready for epilogue sealing.");
            QuestData.saveAndSync(player, quest);
            String bossId = NexusFinalBossProfiles.byPath(path)
                    .map(NexusFinalBossProfile::entityId)
                    .orElse("nexus_final_boss");
            AshfallAdapterCoreLateRuntime.bossDefeated(
                    player,
                    bossId,
                    path.name(),
                    player.blockPosition(),
                    "nexus_finale_boss_credit");
            changed = true;
        }
        PostNexusData.saveAndSync(player, post);
        AshfallAdapterCoreLateRuntime.postNexusPersisted(player, post, "nexus_finale_boss_credit");
        completeReadyMission(player, finaleMissionId(path));
        player.sendSystemMessage(Component.literal(primary
                ? finaleMessage(path)
                : "[NEXUS] Shared finale credit received: " + path.name() + "."));
        return changed;
    }

    private static void grantRelayReward(ServerPlayer player, NexusRelayState outcome) {
        if (!player.getInventory().contains(new ItemStack(ModItems.PORTABLE_SIGNAL_SCANNER.get()))) {
            giveOrDrop(player, new ItemStack(ModItems.PORTABLE_SIGNAL_SCANNER.get(), 1));
        }
        if (!player.getInventory().contains(new ItemStack(ModItems.RELAY_SCANNER_LENS.get()))) {
            giveOrDrop(player, new ItemStack(ModItems.RELAY_SCANNER_LENS.get(), 1));
        }
        switch (outcome) {
            case STABILIZED -> {
                giveOrDrop(player, new ItemStack(ModItems.RAD_AWAY.get(), 2));
                giveOrDrop(player, new ItemStack(ModItems.FILTER_CARTRIDGE_ELITE.get(), 1));
                giveOrDrop(player, new ItemStack(ModItems.INSTABILITY_DAMPENER.get(), 1));
            }
            case SEVERED -> {
                giveOrDrop(player, new ItemStack(ModItems.NEXUS_CRYSTAL.get(), 2));
                giveOrDrop(player, new ItemStack(ModItems.ENERGY_CELL.get(), 2));
            }
            case OVERRIDDEN -> {
                giveOrDrop(player, new ItemStack(ModItems.ENERGY_CELL.get(), 4));
                giveOrDrop(player, new ItemStack(ModItems.NEXUS_CRYSTAL.get(), 1));
            }
            default -> {
            }
        }
    }

    private static void grantSiegeReward(ServerPlayer player) {
        giveOrDrop(player, new ItemStack(ModItems.FILTER_CARTRIDGE_ELITE.get(), 1));
        giveOrDrop(player, new ItemStack(ModItems.ELITE_BATTERY.get(), 1));
        giveOrDrop(player, new ItemStack(ModItems.INSTABILITY_DAMPENER.get(), 1));
        if (!player.getInventory().contains(new ItemStack(ModItems.RETURN_BEACON.get()))) {
            giveOrDrop(player, new ItemStack(ModItems.RETURN_BEACON.get(), 1));
        }
    }

    private static void grantPathOperationReward(ServerPlayer player, PostNexusData.NexusPath path) {
        switch (path) {
            case RESTORE -> giveOrDrop(player, new ItemStack(ModItems.RAD_AWAY.get(), 3));
            case DESTROY -> giveOrDrop(player, new ItemStack(ModItems.MUTAGEN_VIAL.get(), 2));
            case CONTROL -> giveOrDrop(player, new ItemStack(ModItems.SCHEMATIC_FRAGMENT_ENERGY.get(), 2));
            case NONE -> {
            }
        }
        giveOrDrop(player, new ItemStack(ModItems.NEXUS_CRYSTAL.get(), 1));
    }

    private static void grantFinaleReward(ServerPlayer player, PostNexusData.NexusPath path) {
        switch (path) {
            case RESTORE -> giveOrDrop(player, new ItemStack(ModItems.NEXUS_HELMET.get(), 1));
            case DESTROY -> giveOrDrop(player, new ItemStack(ModItems.NEXUS_ANNIHILATOR.get(), 1));
            case CONTROL -> giveOrDrop(player, new ItemStack(ModItems.NEXUS_BLADE.get(), 1));
            case NONE -> {
            }
        }
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack.copy())) {
            player.drop(stack.copy(), false);
        }
    }

    private static String pathOperationMissionId(PostNexusData.NexusPath path) {
        return switch (path) {
            case RESTORE -> "restore_world_lattice";
            case DESTROY -> "destroy_dead_signal";
            case CONTROL -> "control_command_lattice";
            case NONE -> "";
        };
    }

    private static String finaleMissionId(PostNexusData.NexusPath path) {
        return switch (path) {
            case RESTORE -> "restore_finale";
            case DESTROY -> "destroy_finale";
            case CONTROL -> "control_finale";
            case NONE -> "";
        };
    }

    private static String pathOperationMessage(PostNexusData.NexusPath path) {
        return switch (path) {
            case RESTORE -> "[ECHO-7] Restore lattice complete. Relay purification can propagate; keep medicine close while the grid remembers how to be gentle.";
            case DESTROY -> "[ECHO-7] Dead signal collapsed. The command chain has fewer places to hide and fewer excuses to exist.";
            case CONTROL -> "[ECHO-7] Command lattice bound. Relay obedience is rising; so is the cost of being obeyed.";
            case NONE -> "[ECHO-7] Path operation recorded.";
        };
    }

    private static String finaleMessage(PostNexusData.NexusPath path) {
        return switch (path) {
            case RESTORE -> "[NEXUS] Corruption Bloom defeated. Restore epilogue seal is ready at the mission channel.";
            case DESTROY -> "[NEXUS] Severance Engine destroyed. Destroy epilogue seal is ready at the mission channel.";
            case CONTROL -> "[NEXUS] Mirror Command bound. Control epilogue seal is ready at the mission channel.";
            case NONE -> "[NEXUS] Finale complete.";
        };
    }

    private static List<String> primeRelayPoiIds() {
        return NexusRelayProfiles.all().stream()
                .map(profile -> NexusRelaySiteService.siteId(profile.type()))
                .toList();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }
}
