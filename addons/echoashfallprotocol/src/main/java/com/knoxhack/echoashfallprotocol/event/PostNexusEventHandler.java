package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echoashfallprotocol.dimension.ModDimensions;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.endgame.NexusCampaignActions;
import com.knoxhack.echoashfallprotocol.endgame.PostNexusData;
import com.knoxhack.echoashfallprotocol.endgame.PostNexusData.NexusPath;
import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import com.knoxhack.echoashfallprotocol.world.NexusCampaignData;
import com.knoxhack.echoashfallprotocol.world.NexusWorldData;
import java.util.Collections;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tracks post-Nexus branch objectives and keeps the owning client synced.
 */
public class PostNexusEventHandler {

    public static void onBlockPlace(Object event) {
        if (!(eventValue(event, "getEntity") instanceof ServerPlayer player)) return;
        if (player.level().dimension() != Level.OVERWORLD) return;

        if (!(eventValue(event, "getPlacedBlock") instanceof BlockState state)) return;
        PostNexusData data = PostNexusData.get(player);
        if (!data.hasMadeChoice()) return;

        if (data.isPath(NexusPath.CONTROL)
                && (state.is(ModBlocks.SIGNAL_SCANNER.get()) || state.is(ModBlocks.RELAY_STATION.get()))) {
            data.incrementSignalBoostersPlaced();
            player.sendSystemMessage(Component.literal(
                    "[ECHO-7] Signal expansion in progress. (" + data.getSignalBoostersPlaced() + "/3)"));
            commitProgress(player, data);
        }
    }

    public static void onBlockBreak(Object event) {
        if (!(eventValue(event, "getPlayer") instanceof ServerPlayer player)) return;
        if (!(eventValue(event, "getState") instanceof BlockState state)) return;

        if (state.is(ModBlocks.POWER_NODE.get()) && player.level() instanceof ServerLevel serverLevel) {
            if (eventValue(event, "getPos") instanceof BlockPos pos) {
                NexusWorldData.get(serverLevel).removePowerNode(pos);
            }
        }

        PostNexusData data = PostNexusData.get(player);
        if (!data.isPath(NexusPath.DESTROY)) return;

        if (state.is(ModBlocks.POWER_NODE.get())) {
            data.incrementNodesDestroyed();
            player.sendSystemMessage(Component.literal(
                    "[ECHO-7] Grid link severed. (" + data.getNodesDestroyed() + "/5)"));
            commitProgress(player, data);
        }
    }

    public static void recordPowerNodeActivated(ServerPlayer player, BlockPos pos) {
        int activeNodeCount = recordPowerNodeActivationState(player, pos);
        AshfallAdapterCoreLateRuntime.powerNodeState(
                player,
                pos,
                true,
                activeNodeCount,
                "power_node_activation");
        recordPowerNodeRestoreProgress(player);
    }

    public static int recordPowerNodeActivationState(ServerPlayer player, BlockPos pos) {
        int activeNodeCount = 0;
        if (player.level() instanceof ServerLevel serverLevel) {
            NexusWorldData worldData = NexusWorldData.get(serverLevel);
            worldData.recordPowerNodeActivated(pos);
            activeNodeCount = worldData.getActiveNodePositions().size();
        }
        return activeNodeCount;
    }

    public static boolean recordPowerNodeRestoreProgress(ServerPlayer player) {
        PostNexusData data = PostNexusData.get(player);
        if (!data.isPath(NexusPath.RESTORE)) return false;

        data.incrementNodesRepaired();
        player.sendSystemMessage(Component.literal(
                "[ECHO-7] Grid node stabilized. (" + data.getNodesRepaired() + "/3)"));
        commitProgress(player, data);
        return true;
    }

    public static void onLivingDeath(Object event) {
        Object source = eventValue(event, "getSource");
        if (!(eventValue(source, "getEntity") instanceof ServerPlayer player)) return;

        if (!(eventValue(event, "getEntity") instanceof LivingEntity killed)) return;
        PostNexusData data = PostNexusData.get(player);
        if (!data.hasMadeChoice()) return;

        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(killed.getType()).toString();
        boolean changed = false;

        if (data.isPath(NexusPath.RESTORE) && isCorruptedMob(entityId)) {
            data.incrementCorruptedMobsKilled();
            changed = true;
            if (data.getCorruptedMobsKilled() % 5 == 0) {
                player.sendSystemMessage(Component.literal(
                        "[ECHO-7] Purification in progress. (" + data.getCorruptedMobsKilled() + "/20)"));
            }
        }

        if (data.isPath(NexusPath.DESTROY) && isCorruptedMob(entityId) && player.level() instanceof ServerLevel serverLevel) {
            if (player.level().getRandom().nextFloat() < 0.30f) {
                killed.spawnAtLocation(serverLevel, new ItemStack(ModItems.DENSE_ALLOY_CHUNK.get(), 1));
            }
            if (player.level().getRandom().nextFloat() < 0.15f) {
                killed.spawnAtLocation(serverLevel, new ItemStack(ModItems.NEXUS_CRYSTAL.get(), 1));
            }
        }

        if (data.isPath(NexusPath.CONTROL) && player.getMainHandItem().is(ModItems.NEXUS_BLADE.get())) {
            data.incrementMobsDefeatedWithScepter();
            changed = true;
        }

        if ("echoashfallprotocol:warden_boss".equals(entityId) && player.level() instanceof ServerLevel serverLevel) {
            creditWardenDefeat(serverLevel, killed, player);
        }

        if (changed) {
            commitProgress(player, data);
        }
    }

    public static void onPlayerTick(Object event) {
        if (!(eventValue(event, "getEntity") instanceof ServerPlayer player)) return;
        if (player.tickCount % 100 != 0) return;

        syncPlayerToWorldChoice(player);
        NexusCampaignActions.syncCampaignState(player);

        PostNexusData data = PostNexusData.get(player);
        if (!data.hasMadeChoice()) return;

        if (ModDimensions.isPrefallArchives(player.level()) && !data.hasEnteredArchives()) {
            data.setArchivesEntered(true);
            QuestData quest = QuestData.get(player);
            quest.visitLocation("dimension", "echoashfallprotocol:prefall_archives");
            QuestData.saveAndSync(player, quest);
            player.sendSystemMessage(Component.literal("[ECHO-7] Pre-Fall Archives entry confirmed."));
            commitProgress(player, data);
            return;
        }

        if (!data.isPath(NexusPath.DESTROY) || player.level().dimension() != Level.OVERWORLD) return;

        if (player.level() instanceof ServerLevel serverLevel) {
            EnvironmentalEventData eventData = EnvironmentalEventData.get(serverLevel);
            EnvironmentalEventType activeEvent = eventData.getCurrentEvent();
            boolean stormActive = isDestroyRouteStormCreditEvent(activeEvent, serverLevel.isThundering());

            if (stormActive && !data.hasCountedCurrentStorm()) {
                data.incrementStormsSurvived();
                data.setCountedCurrentStorm(true);
                player.sendSystemMessage(Component.literal(
                        stormCreditMessage(serverLevel, player, data)));
                commitProgress(player, data);
            } else if (!stormActive && data.hasCountedCurrentStorm()) {
                data.setCountedCurrentStorm(false);
                PostNexusData.saveAndSync(player, data);
            }
        }
    }

    public static void onItemPickup(Object event) {
        if (!(eventValue(event, "getPlayer") instanceof ServerPlayer player)) return;
        Object itemEntity = eventValue(event, "getItemEntity");
        if (itemEntity == null) return;

        PostNexusData data = PostNexusData.get(player);
        if (!data.isPath(NexusPath.CONTROL)) return;

        if (!(eventValue(itemEntity, "getItem") instanceof ItemStack stack)) return;
        int count = stack.getCount();
        boolean changed = false;

        if (stack.is(ModItems.DENSE_ALLOY_CHUNK.get())) {
            data.addDenseAlloy(count);
            changed = true;
        } else if (stack.is(ModItems.NEXUS_CRYSTAL.get())) {
            data.addNexusCrystals(count);
            changed = true;
        } else if (stack.is(ModItems.ENERGY_CELL.get())) {
            data.addEnergyCells(count);
            changed = true;
        }

        if (changed) {
            int total = data.getDenseAlloyCollected() + data.getNexusCrystalsCollected() + data.getEnergyCellsCollected();
            if (total % 25 == 0) {
                player.sendSystemMessage(Component.literal(
                        "[ECHO-7] Resource stockpiling. (" + total + "/150 total)"));
            }
            commitProgress(player, data);
        }
    }

    public static boolean isDestroyRouteStormCreditEvent(EnvironmentalEventType activeEvent, boolean thundering) {
        return activeEvent == EnvironmentalEventType.RADIATION_STORM
                || activeEvent == EnvironmentalEventType.ASH_STORM
                || activeEvent == EnvironmentalEventType.NEXUS_SURGE
                || thundering;
    }

    private static String stormCreditMessage(ServerLevel level, ServerPlayer player, PostNexusData data) {
        boolean sheltered = !level.canSeeSky(player.blockPosition());
        String posture = sheltered ? "Shelter held." : "Exposure route survived.";
        return "[ECHO-7] Storm endured. " + posture + " (" + data.getStormsSurvived() + "/1)";
    }

    public static void onNexusChoiceMade(ServerPlayer player, NexusPath path) {
        PostNexusData data = PostNexusData.get(player);
        data.setSelectedPath(path);
        PostNexusData.saveAndSync(player, data);
        AshfallAdapterCoreLateRuntime.postNexusPersisted(player, data, "nexus_choice_sync");

        String message = switch (path) {
            case RESTORE -> "[ECHO-7] Restoration protocols active. Restore branch objectives unlocked.";
            case DESTROY -> "[ECHO-7] Destruction protocols active. Destroy branch objectives unlocked.";
            case CONTROL -> "[ECHO-7] Control protocols active. Control branch objectives unlocked.";
            case NONE -> "";
        };
        if (!message.isEmpty()) {
            player.sendSystemMessage(Component.literal(message));
        }

        completeNexusDecisionMissions(player);
    }

    public static void syncPlayerToWorldChoice(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel playerLevel)) return;
        ServerLevel overworld = playerLevel.getServer().overworld();
        NexusWorldData worldData = NexusWorldData.get(overworld);
        if (!worldData.hasChoiceBeenMade()) return;

        NexusPath path = switch (worldData.getState()) {
            case RESTORED -> NexusPath.RESTORE;
            case DESTROYED -> NexusPath.DESTROY;
            case CONTROLLED -> NexusPath.CONTROL;
            case NORMAL -> NexusPath.NONE;
        };
        if (path == NexusPath.NONE) return;

        PostNexusData data = PostNexusData.get(player);
        if (data.getSelectedPath() != path) {
            data.setSelectedPath(path);
            PostNexusData.saveAndSync(player, data);
            player.sendSystemMessage(Component.literal(
                    "[ECHO-7] Shared Nexus state synchronized: " + path.name() + "."));
        }

        completeNexusDecisionMissions(player);
    }

    public static void completeFinalProtocol(ServerPlayer player, String missionId) {
        PostNexusData data = PostNexusData.get(player);
        if (!data.hasMadeChoice() || !data.isWardenDefeated() || !data.isFinalBossDefeated()) {
            player.sendSystemMessage(Component.literal(
                    "[ECHO-7] Final epilogue locked until the path finale boss is defeated."));
            return;
        }

        boolean changed = false;
        if (!data.isFinalRewardClaimed()) {
            grantFinalReward(player, data.getSelectedPath());
            data.setFinalRewardClaimed(true);
            changed = true;
        }
        if (!data.isEpilogueComplete()) {
            data.setEpilogueComplete(true);
            changed = true;
        }

        if (changed) {
            player.sendSystemMessage(Component.literal(finalProtocolMessage(data.getSelectedPath(), missionId)));
            commitProgress(player, data);
        }
    }

    private static void completeNexusDecisionMissions(ServerPlayer player) {
        QuestData quest = QuestData.get(player);
        if (!quest.isMissionCompleted("find_nexus_core")) {
            quest.completeMission(player, "find_nexus_core", Collections.emptyList());
        }
        if (!quest.isMissionCompleted("awaken_nexus_core")) {
            quest.completeMission(player, "awaken_nexus_core", Collections.emptyList());
        }
        if (!quest.isMissionCompleted("scan_prime_relays")) {
            quest.completeMission(player, "scan_prime_relays", Collections.emptyList());
        }
        if (!quest.isMissionCompleted("resolve_prime_relays")) {
            quest.completeMission(player, "resolve_prime_relays", Collections.emptyList());
        }
        if (!quest.isMissionCompleted("stabilize_nexus_grid")) {
            quest.completeMission(player, "stabilize_nexus_grid", Collections.emptyList());
        }
        if (!quest.isMissionCompleted("survive_core_countermeasure")) {
            quest.completeMission(player, "survive_core_countermeasure", Collections.emptyList());
        }
        if (!quest.isMissionCompleted("reach_decision")) {
            quest.completeMission(player, "reach_decision", Collections.emptyList());
        }
        quest.clearTurnInReminder("reach_decision");
        quest.repairMissionState(player);
        QuestData.saveAndSync(player, quest);
    }

    public static int creditWardenDefeat(ServerLevel level, LivingEntity killed, ServerPlayer killer) {
        if (!ModDimensions.isPrefallArchives(level)) {
            return 0;
        }
        int credited = 0;
        for (ServerPlayer candidate : level.getServer().getPlayerList().getPlayers()) {
            if (candidate.level() != level) continue;
            if (candidate.distanceToSqr(killed) > 96.0D * 96.0D) continue;

            PostNexusData candidateData = PostNexusData.get(candidate);
            if (!candidateData.hasMadeChoice() || candidateData.isWardenDefeated()) continue;

            candidateData.setWardenDefeated(true);
            NexusCampaignData.get(level.getServer().overworld()).markWardenDefeated();
            if (!candidateData.isWardenRewardClaimed()) {
                grantWardenReward(candidate, candidateData.getSelectedPath());
                candidateData.setWardenRewardClaimed(true);
            }
            AshfallAdapterCoreLateRuntime.bossDefeated(
                    candidate,
                    "warden_boss",
                    candidateData.getSelectedPath().name(),
                    killed.blockPosition(),
                    "warden_death_credit");
            candidate.sendSystemMessage(Component.literal(
                    candidate == killer
                            ? "[ECHO-7] The Guardian falls. Return to the terminal to finish the final protocol."
                            : "[ECHO-7] Guardian defeat confirmed. Your final protocol is ready at the terminal."));
            commitProgress(candidate, candidateData);
            credited++;
        }
        if (credited > 0) {
            NexusCampaignActions.syncCampaignState(level);
        }
        return credited;
    }

    private static boolean isCorruptedMob(String entityId) {
        return entityId.endsWith("rad_zombie")
                || entityId.endsWith("glowing_ghoul")
                || entityId.endsWith("irradiated_wolf")
                || entityId.endsWith("toxic_slime")
                || entityId.endsWith("ash_wraith")
                || entityId.endsWith("rust_walker")
                || entityId.endsWith("city_stalker")
                || entityId.endsWith("steam_wraith")
                || entityId.endsWith("mutated_crawler");
    }

    private static void commitProgress(ServerPlayer player, PostNexusData data) {
        PostNexusData.saveAndSync(player, data);
        QuestData quest = QuestData.get(player);
        if (quest.repairMissionState(player)) {
            QuestData.saveAndSync(player, quest);
        } else {
            QuestData.syncToClient(player);
        }
        AshfallAdapterCoreLateRuntime.postNexusPersisted(player, data, "post_nexus_commit");
    }

    private static void grantWardenReward(ServerPlayer player, NexusPath path) {
        giveOrDrop(player, new ItemStack(ModItems.NEXUS_CRYSTAL.get(), path == NexusPath.DESTROY ? 10 : 6));
        giveOrDrop(player, new ItemStack(ModItems.DENSE_ALLOY_CHUNK.get(), path == NexusPath.CONTROL ? 10 : 6));
        giveOrDrop(player, new ItemStack(ModItems.ENERGY_CELL.get(), path == NexusPath.RESTORE ? 10 : 6));
    }

    private static void grantFinalReward(ServerPlayer player, NexusPath path) {
        switch (path) {
            case RESTORE -> {
                giveOrDrop(player, new ItemStack(ModItems.RAD_AWAY.get(), 4));
                giveOrDrop(player, new ItemStack(ModItems.FILTER_CARTRIDGE_ELITE.get(), 1));
                giveOrDrop(player, new ItemStack(ModItems.ENERGY_CELL.get(), 12));
            }
            case DESTROY -> {
                giveOrDrop(player, new ItemStack(ModItems.NEXUS_CRYSTAL.get(), 12));
                giveOrDrop(player, new ItemStack(ModItems.MUTAGEN_VIAL.get(), 3));
                giveOrDrop(player, new ItemStack(ModItems.DENSE_ALLOY_CHUNK.get(), 12));
            }
            case CONTROL -> {
                giveOrDrop(player, new ItemStack(ModItems.NEXUS_CRYSTAL.get(), 12));
                giveOrDrop(player, new ItemStack(ModItems.ENERGY_CELL.get(), 16));
                giveOrDrop(player, new ItemStack(ModItems.SCHEMATIC_FRAGMENT_ENERGY.get(), 2));
            }
            case NONE -> {
            }
        }
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack.copy())) {
            player.drop(stack.copy(), false);
        }
    }

    private static String finalProtocolMessage(NexusPath path, String missionId) {
        return switch (path) {
            case RESTORE -> "[ECHO-7] Restore epilogue sealed. The Grid breathes again, and the Archives name you caretaker.";
            case DESTROY -> "[ECHO-7] Destroy epilogue sealed. The Core is ash, and no command chain remains above the dead world.";
            case CONTROL -> "[ECHO-7] Control epilogue sealed. The signal answers, but ECHO will keep watching the throne.";
            case NONE -> "[ECHO-7] Final protocol sealed: " + missionId + ".";
        };
    }

    private static Object eventValue(Object event, String methodName) {
        if (event == null) {
            return null;
        }
        try {
            return event.getClass().getMethod(methodName).invoke(event);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }
}
