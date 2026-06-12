package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echo.adaptercore.EchoCanonicalContentIds;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeEvent;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationContext;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativePlayerRef;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.block.PowerNodeBlock;
import com.knoxhack.echoashfallprotocol.block.RelayStationBlock;
import com.knoxhack.echoashfallprotocol.block.entity.PowerNodeBlockEntity;
import com.knoxhack.echoashfallprotocol.echo.EchoMessages;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.endgame.NexusCampaignActions;
import com.knoxhack.echoashfallprotocol.endgame.NexusRelayState;
import com.knoxhack.echoashfallprotocol.endgame.NexusRelayType;
import com.knoxhack.echoashfallprotocol.endgame.PostNexusData;
import com.knoxhack.echoashfallprotocol.entity.ModEntities;
import com.knoxhack.echoashfallprotocol.entity.ScoutDrone;
import com.knoxhack.echoashfallprotocol.fasttravel.RadioNetwork;
import com.knoxhack.echoashfallprotocol.fasttravel.StationRegistry;
import com.knoxhack.echoashfallprotocol.faction.AshfallFactionContractProgression;
import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import com.knoxhack.echoashfallprotocol.research.ResearchData;
import com.knoxhack.echoashfallprotocol.world.NexusCampaignData;
import com.knoxhack.echoashfallprotocol.world.NexusWorldData;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class AshfallAdapterCoreLateRuntime {
    private static final String RUNTIME_HOST_ID = "echoashfallprotocol:late_runtime";
    private static final String LAST_EVENT_KEY = "ashes_of_tomorrow.adaptercore.last_late_event";
    private static final String LAST_EVENT_TICK_KEY = "ashes_of_tomorrow.adaptercore.last_late_event_tick";
    public static final String RETURN_BEACON_COOLDOWN_KEY = "echoashfallprotocol_return_beacon_ready_tick";
    public static final long RETURN_BEACON_COOLDOWN_TICKS = 20L * 60L * 5L;
    private static final AshfallAdapterCoreRuntimeTruthBridge.RuntimeBinding RUNTIME_BINDING =
            AshfallAdapterCoreRuntimeTruthBridge.binding(
                    RUNTIME_HOST_ID,
                    "late",
                    LAST_EVENT_KEY,
                    LAST_EVENT_TICK_KEY,
                    Set.of(
                            EchoCanonicalContentIds.EVENT_ASHFALL_BOSS_DEFEATED,
                            EchoCanonicalContentIds.EVENT_ASHFALL_RELAY_ACTIVATED,
                            EchoCanonicalContentIds.EVENT_ASHFALL_POWER_NODE_STATE,
                            EchoCanonicalContentIds.EVENT_PLAYER_ITEM_USED,
                            EchoCanonicalContentIds.EVENT_ASHFALL_SCOUT_DRONE_ROUTE,
                            EchoCanonicalContentIds.EVENT_ASHFALL_NEXUS_CAPACITOR_STATE,
                            EchoCanonicalContentIds.EVENT_ASHFALL_NEXUS_STATE,
                            EchoCanonicalContentIds.EVENT_ASHFALL_PRIME_RELAY_RESOLVED,
                            EchoCanonicalContentIds.EVENT_ASHFALL_ENDING_CHOICE,
                            EchoCanonicalContentIds.EVENT_ASHFALL_POST_NEXUS_PERSISTED),
                    Set.of(
                            EchoCanonicalContentIds.BLOCK_RELAY_STATION,
                            "echoashfallprotocol:activate_relay_station",
                            EchoCanonicalContentIds.BLOCK_POWER_NODE,
                            "echoashfallprotocol:activate_power_node",
                            "echoashfallprotocol:stabilize_nexus_grid",
                            EchoCanonicalContentIds.ITEM_INSTABILITY_DAMPENER,
                            EchoCanonicalContentIds.ITEM_RETURN_BEACON,
                            EchoCanonicalContentIds.ITEM_SCOUT_DRONE_ITEM,
                            "echoashfallprotocol:build_scout_drone",
                            "echoashfallprotocol:nexus_capacitor",
                            "echoashfallprotocol:build_nexus_capacitor",
                            "echoashfallprotocol:awaken_nexus_core",
                            "echoashfallprotocol:scan_prime_relays",
                            "echoashfallprotocol:survive_core_countermeasure",
                            "echoashfallprotocol:resolve_prime_relays",
                            "echoashfallprotocol:reach_decision",
                            "echoashfallprotocol:restore_world_lattice",
                            "echoashfallprotocol:destroy_dead_signal",
                            "echoashfallprotocol:control_command_lattice"),
                    AshfallAdapterCoreLateRuntime::apply);

    private AshfallAdapterCoreLateRuntime() {
    }

    public static NativeResult bossDefeated(
            ServerPlayer player,
            String bossId,
            String path,
            @Nullable BlockPos pos,
            String source) {
        if (player == null) {
            return skipped("Late-runtime boss defeat skipped for missing player.");
        }
        Map<String, Object> payload = basePayload("boss/" + sanitizeTarget(bossId), source);
        payload.put("bossId", safe(bossId));
        payload.put("path", safe(path));
        if (pos != null) {
            payload.put("pos", positionSnapshot(pos));
        }
        return publish(player, "ashfall.boss_defeated", payload, pos, true);
    }

    public static NativeResult relayActivated(
            ServerPlayer player,
            String relayId,
            String relayType,
            @Nullable BlockPos pos,
            String source) {
        if (player == null) {
            return skipped("Late-runtime relay activation skipped for missing player.");
        }
        Map<String, Object> payload = basePayload("relay/" + sanitizeTarget(relayId), source);
        payload.put("relayId", safe(relayId));
        payload.put("relayType", safe(relayType));
        if (pos != null) {
            payload.put("pos", positionSnapshot(pos));
        }
        return publish(player, "ashfall.relay_activated", payload, pos, true);
    }

    public static NativeResult relayStationUsed(
            ServerPlayer player,
            BlockPos pos,
            boolean shiftTravelRequested,
            String source) {
        if (player == null) {
            return skipped("Late-runtime relay station use skipped for missing player.");
        }
        Map<String, Object> payload = basePayload(EchoCanonicalContentIds.BLOCK_RELAY_STATION, source);
        payload.put("relayStationUse", true);
        payload.put("relayId", "radio_relay");
        payload.put("relayType", "relay_station");
        payload.put("blockId", EchoCanonicalContentIds.BLOCK_RELAY_STATION);
        payload.put("targetBlockPos", positionSnapshot(pos));
        payload.put("pos", positionSnapshot(pos));
        payload.put("shiftTravelRequested", shiftTravelRequested);
        return publish(player, EchoCanonicalContentIds.EVENT_ASHFALL_RELAY_ACTIVATED, payload, pos, false);
    }

    public static NativeResult powerNodeState(
            ServerPlayer player,
            BlockPos pos,
            boolean active,
            int activeNodeCount,
            String source) {
        if (player == null) {
            return skipped("Late-runtime power node state skipped for missing player.");
        }
        Map<String, Object> payload = basePayload(EchoCanonicalContentIds.BLOCK_POWER_NODE, source);
        payload.put("machineId", EchoCanonicalContentIds.BLOCK_POWER_NODE);
        payload.put("blockId", EchoCanonicalContentIds.BLOCK_POWER_NODE);
        payload.put("legacyTarget", "power_node/" + (active ? "active" : "inactive"));
        payload.put("active", active);
        payload.put("activeNodeCount", activeNodeCount);
        payload.put("targetBlockPos", positionSnapshot(pos));
        payload.put("pos", positionSnapshot(pos));
        return publish(player, EchoCanonicalContentIds.EVENT_PLAYER_MACHINE_POWERED, payload, pos, true);
    }

    public static NativeResult powerNodeUsed(
            ServerPlayer player,
            BlockPos pos,
            String source) {
        if (player == null) {
            return skipped("Late-runtime power node use skipped for missing player.");
        }
        Map<String, Object> payload = basePayload(EchoCanonicalContentIds.BLOCK_POWER_NODE, source);
        payload.put("powerNodeUse", true);
        payload.put("machineId", EchoCanonicalContentIds.BLOCK_POWER_NODE);
        payload.put("blockId", EchoCanonicalContentIds.BLOCK_POWER_NODE);
        payload.put("legacyTarget", "power_node/active");
        payload.put("active", true);
        payload.put("targetBlockPos", positionSnapshot(pos));
        payload.put("pos", positionSnapshot(pos));
        return publish(player, EchoCanonicalContentIds.EVENT_PLAYER_MACHINE_POWERED, payload, pos, false);
    }

    public static NativeResult scoutDroneRoute(
            ServerPlayer player,
            String routeId,
            String mode,
            @Nullable BlockPos pos,
            String source) {
        if (player == null) {
            return skipped("Late-runtime scout drone route skipped for missing player.");
        }
        Map<String, Object> payload = basePayload("drone_route/" + sanitizeTarget(routeId), source);
        payload.put("routeId", safe(routeId));
        payload.put("mode", safe(mode));
        if (pos != null) {
            payload.put("pos", positionSnapshot(pos));
        }
        return publish(player, "ashfall.scout_drone_route", payload, pos, true);
    }

    public static NativeResult scoutDroneItemUsed(ServerPlayer player, InteractionHand hand) {
        if (player == null) {
            return skipped("Late-runtime Scout Drone item use skipped for missing player.");
        }
        Map<String, Object> payload = basePayload(EchoCanonicalContentIds.ITEM_SCOUT_DRONE_ITEM, "scout_drone_item");
        payload.put("itemId", EchoCanonicalContentIds.ITEM_SCOUT_DRONE_ITEM);
        payload.put("item", EchoCanonicalContentIds.ITEM_SCOUT_DRONE_ITEM);
        payload.put("hand", hand == null ? InteractionHand.MAIN_HAND.name() : hand.name());
        payload.put("scoutDroneItemUse", true);
        payload.put("routeId", "scout_deployed");
        payload.put("mode", ScoutDrone.DroneMode.FOLLOW.name());
        return publish(player, EchoCanonicalContentIds.EVENT_ASHFALL_SCOUT_DRONE_ROUTE, payload, null, false);
    }

    public static NativeResult nexusCapacitorState(
            ServerPlayer player,
            BlockPos pos,
            int storedEnergy,
            int capacity,
            String source) {
        if (player == null) {
            return skipped("Late-runtime Nexus Capacitor state skipped for missing player.");
        }
        Map<String, Object> payload = basePayload("nexus_capacitor/" + (storedEnergy > 0 ? "charged" : "linked"), source);
        payload.put("storedEnergy", Math.max(0, storedEnergy));
        payload.put("capacity", Math.max(0, capacity));
        payload.put("charged", storedEnergy > 0);
        payload.put("pos", positionSnapshot(pos));
        return publish(player, "ashfall.nexus_capacitor_state", payload, pos, true);
    }

    public static NativeResult nexusState(
            ServerPlayer player,
            @Nullable NexusCampaignData campaign,
            @Nullable NexusWorldData worldData,
            String state,
            String source) {
        if (player == null) {
            return skipped("Late-runtime Nexus state skipped for missing player.");
        }
        Map<String, Object> payload = basePayload("nexus/state", source);
        payload.put("state", safe(state));
        payload.putAll(campaignSnapshot(campaign));
        payload.putAll(worldSnapshot(worldData));
        return publish(player, "ashfall.nexus_state", payload, null, false);
    }

    public static NativeResult primeRelayResolved(
            ServerPlayer player,
            NexusRelayType type,
            NexusRelayState outcome,
            @Nullable NexusCampaignData campaign,
            String source) {
        if (player == null || type == null || outcome == null) {
            return skipped("Late-runtime Prime Relay resolution skipped for missing relay data.");
        }
        Map<String, Object> payload = basePayload("prime_relay/" + type.name().toLowerCase(java.util.Locale.ROOT), source);
        payload.put("relayType", type.name());
        payload.put("relayName", type.displayName());
        payload.put("outcome", outcome.name());
        payload.putAll(campaignSnapshot(campaign));
        return publish(player, "ashfall.prime_relay_resolved", payload, null, false);
    }

    public static NativeResult endingChoice(
            ServerPlayer player,
            PostNexusData.NexusPath path,
            @Nullable BlockPos nexusPos,
            String source) {
        if (player == null || path == null || path == PostNexusData.NexusPath.NONE) {
            return skipped("Late-runtime ending choice skipped for missing path.");
        }
        Map<String, Object> payload = basePayload("ending/" + path.name().toLowerCase(java.util.Locale.ROOT), source);
        payload.put("path", path.name());
        if (nexusPos != null) {
            payload.put("pos", positionSnapshot(nexusPos));
        }
        return publish(player, "ashfall.ending_choice", payload, null, false);
    }

    public static NativeResult postNexusPersisted(
            ServerPlayer player,
            PostNexusData data,
            String source) {
        if (player == null || data == null) {
            return skipped("Late-runtime post-Nexus persistence skipped for missing data.");
        }
        Map<String, Object> payload = basePayload("post_nexus/persisted", source);
        payload.put("path", data.getSelectedPath().name());
        payload.put("wardenDefeated", data.isWardenDefeated());
        payload.put("finalBossDefeated", data.isFinalBossDefeated());
        payload.put("epilogueComplete", data.isEpilogueComplete());
        payload.put("relaysResolved", data.getRelaysResolved());
        payload.put("pathOperationsComplete", data.getPathOperationsComplete());
        payload.put("nodesRepaired", data.getNodesRepaired());
        payload.put("nodesDestroyed", data.getNodesDestroyed());
        return publish(player, "ashfall.post_nexus_persisted", payload, null, false);
    }

    public static NativeResult instabilityDampenerUsed(
            ServerPlayer player,
            ItemStack stack,
            InteractionHand hand,
            int instabilityReduction) {
        if (player == null) {
            return skipped("Late-runtime Instability Dampener use skipped for missing player.");
        }
        Map<String, Object> payload = basePayload(EchoCanonicalContentIds.ITEM_INSTABILITY_DAMPENER, "instability_dampener_item_use");
        payload.put("itemId", EchoCanonicalContentIds.ITEM_INSTABILITY_DAMPENER);
        payload.put("item", EchoCanonicalContentIds.ITEM_INSTABILITY_DAMPENER);
        payload.put("count", Math.max(1, stack == null ? 1 : stack.getCount()));
        payload.put("hand", hand == null ? InteractionHand.MAIN_HAND.name() : hand.name());
        payload.put("instabilityDampenerUse", true);
        payload.put("instabilityReduction", Math.max(0, instabilityReduction));
        return publish(player, EchoCanonicalContentIds.EVENT_PLAYER_ITEM_USED, payload, null, false);
    }

    public static NativeResult returnBeaconUsed(ServerPlayer player, InteractionHand hand) {
        if (player == null) {
            return skipped("Late-runtime Return Beacon use skipped for missing player.");
        }
        Map<String, Object> payload = basePayload(EchoCanonicalContentIds.ITEM_RETURN_BEACON, "return_beacon_item_use");
        payload.put("itemId", EchoCanonicalContentIds.ITEM_RETURN_BEACON);
        payload.put("item", EchoCanonicalContentIds.ITEM_RETURN_BEACON);
        payload.put("hand", hand == null ? InteractionHand.MAIN_HAND.name() : hand.name());
        payload.put("returnBeaconUse", true);
        payload.put("cooldownTicks", RETURN_BEACON_COOLDOWN_TICKS);
        return publish(player, EchoCanonicalContentIds.EVENT_PLAYER_ITEM_USED, payload, null, false);
    }

    private static NativeResult publish(ServerPlayer player, String eventId, Map<String, Object> payload) {
        return publish(player, eventId, payload, null, true);
    }

    private static NativeResult publish(
            ServerPlayer player,
            String eventId,
            Map<String, Object> payload,
            BlockPos requiredLoadedPos,
            boolean dedupeSameTick) {
        return AshfallAdapterCoreRuntimeTruthBridge.publish(
                RUNTIME_BINDING,
                player,
                eventId,
                payload,
                requiredLoadedPos,
                dedupeSameTick);
    }

    private static NativeResult apply(ServerPlayer player, NativeEvent event, NativeMutationContext context) {
        Map<String, Object> payload = new LinkedHashMap<>(event.payload());
        String target = stringValue(payload, "target");

        boolean changed = false;
        Map<String, Object> resultSnapshot = new LinkedHashMap<>();

        switch (event.eventId()) {
            case "ashfall.boss_defeated" -> changed |= applyBossDefeated(player, payload);
            case "ashfall.relay_activated" -> {
                if (booleanValue(payload, "relayStationUse")) {
                    changed |= applyRelayStationUse(player, payload, resultSnapshot);
                } else {
                    changed |= applyRelayActivationMarkers(player, payload);
                }
            }
            case "ashfall.power_node_state", "player.machine_powered" -> {
                if (booleanValue(payload, "powerNodeUse")) {
                    changed |= applyPowerNodeUse(player, payload, resultSnapshot);
                } else if (booleanValue(payload, "active")) {
                    changed |= applyPowerNodeActivationMarkers(player, payload);
                }
            }
            case EchoCanonicalContentIds.EVENT_PLAYER_ITEM_USED -> {
                if (booleanValue(payload, "instabilityDampenerUse")) {
                    changed |= applyInstabilityDampenerUse(player, payload, resultSnapshot);
                } else if (booleanValue(payload, "returnBeaconUse")) {
                    changed |= applyReturnBeaconUse(player, payload, resultSnapshot);
                } else {
                    changed |= recordMission(player, MissionObjectiveType.CUSTOM, target, 1, payload);
                }
            }
            case "ashfall.scout_drone_route" -> {
                if (booleanValue(payload, "scoutDroneItemUse")) {
                    changed |= applyScoutDroneItemUse(player, payload, resultSnapshot);
                } else {
                    changed |= applyScoutDroneRouteMarkers(player, payload);
                }
            }
            case "ashfall.nexus_capacitor_state" -> {
                changed |= markLocation(player, "special", "nexus:capacitor_linked");
                if (Boolean.TRUE.equals(payload.get("charged"))) {
                    changed |= markLocation(player, "special", "nexus:capacitor_charged");
                }
                changed |= recordMission(player, MissionObjectiveType.PLACE_BLOCK, "echoashfallprotocol:nexus_capacitor", 1, payload);
                changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:build_nexus_capacitor", 1, payload);
            }
            case "ashfall.nexus_state" -> {
                String state = sanitizeTarget(stringValue(payload, "state"));
                changed |= markLocation(player, "special", "nexus:state:" + state);
                changed |= recordMission(player, MissionObjectiveType.CUSTOM, "nexus/state", 1, payload);
                changed |= recordNexusStateMission(player, state, payload);
            }
            case "ashfall.prime_relay_resolved" -> {
                String relay = sanitizeTarget(stringValue(payload, "relayType"));
                String outcome = sanitizeTarget(stringValue(payload, "outcome"));
                changed |= markLocation(player, "special", "nexus:relay:" + relay + ":" + outcome);
                changed |= recordMission(player, MissionObjectiveType.ESTABLISH_ROUTE, "echoashfallprotocol:resolve_prime_relays", 1, payload);
            }
            case "ashfall.ending_choice" -> {
                String path = sanitizeTarget(stringValue(payload, "path"));
                changed |= markLocation(player, "special", "nexus:choice:" + path);
                changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:reach_decision", 1, payload);
            }
            case "ashfall.post_nexus_persisted" -> {
                String path = sanitizeTarget(stringValue(payload, "path"));
                changed |= markLocation(player, "special", "post_nexus:persisted:" + path);
                changed |= recordMission(player, MissionObjectiveType.CUSTOM, "post_nexus/persisted", 1, payload);
                changed |= recordPostNexusMission(player, path, payload);
            }
            default -> changed |= recordMission(player, MissionObjectiveType.CUSTOM, event.eventId(), 1, payload);
        }

        if (changed) {
            CompoundTag playerData = player.getPersistentData();
            playerData.putString(LAST_EVENT_KEY, event.eventId());
            playerData.putLong(LAST_EVENT_TICK_KEY, context.gameTime());
        }

        resultSnapshot.put("eventId", event.eventId());
        resultSnapshot.put("target", target);
        resultSnapshot.put("playerId", player.getUUID().toString());
        resultSnapshot.put("nativeInterface", "EchoNativeRuntimeHost.Events");
        resultSnapshot.put("nativeMethod", "publish");
        resultSnapshot.put("realNativeStateMutated", changed);
        String status = stringValue(resultSnapshot, "failureReason").isBlank()
                ? changed ? "MUTATED" : "NOOP"
                : "FAILED";
        return new NativeResult(changed, status,
                switch (status) {
                    case "MUTATED" -> "Published AdapterCore late-runtime event and mutated state.";
                    case "FAILED" -> "AdapterCore late-runtime event attempted a mutation and failed.";
                    default -> "AdapterCore late-runtime event was valid but no state change was needed.";
                },
                resultSnapshot);
    }

    private static boolean applyBossDefeated(ServerPlayer player, Map<String, Object> payload) {
        String bossId = sanitizeTarget(stringValue(payload, "bossId"));
        String path = sanitizeTarget(stringValue(payload, "path"));
        boolean changed = false;
        changed |= markLocation(player, "special", "boss:" + bossId);
        changed |= recordMission(player, MissionObjectiveType.CUSTOM, "boss/" + bossId, 1, payload);
        String routeMission = guardianBossMission(bossId);
        if (!routeMission.isBlank()) {
            changed |= recordMission(player, MissionObjectiveType.KILL_ENTITY, "echoashfallprotocol:" + bossId, 1, payload);
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:" + routeMission, 1, payload);
        }
        if ("warden_boss".equals(bossId) || "the_warden".equals(bossId)) {
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, path + "/guardian", 1, payload);
        }
        if (!path.isBlank() && !"unknown".equals(path)) {
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, path + "/finale_boss", 1, payload);
        }
        return changed;
    }

    private static boolean applyPowerNodeUse(
            ServerPlayer player,
            Map<String, Object> payload,
            Map<String, Object> resultSnapshot) {
        Object posPayload = payload.containsKey("targetBlockPos")
                ? payload.get("targetBlockPos")
                : payload.get("pos");
        BlockPos pos = blockPosValue(posPayload);
        BlockState state = player.level().getBlockState(pos);
        boolean targetIsPowerNode = state.getBlock() instanceof PowerNodeBlock;
        boolean activeBefore = targetIsPowerNode && state.getValue(PowerNodeBlock.ACTIVE);
        boolean entityActivatedBefore = player.level().getBlockEntity(pos) instanceof PowerNodeBlockEntity node
                && node.isActivated();

        resultSnapshot.put("powerNodeRuntime", true);
        resultSnapshot.put("powerNodePos", positionSnapshot(pos));
        resultSnapshot.put("blockId", stringValue(payload, "blockId"));
        resultSnapshot.put("powerNodeActiveBefore", activeBefore);
        resultSnapshot.put("powerNodeEntityActivatedBefore", entityActivatedBefore);
        resultSnapshot.put("activeNodeCountBefore", activePowerNodeCount(player));
        addPowerNodeInventorySnapshot(player, resultSnapshot, "Before");

        if (!targetIsPowerNode) {
            resultSnapshot.put("failureReason", "target block is not a power node");
            resultSnapshot.put("hudOrEventEmitted", true);
            addPowerNodeInventorySnapshot(player, resultSnapshot, "After");
            player.sendSystemMessage(Component.literal("[ECHO-7] Power Node link unavailable.")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        if (activeBefore) {
            resultSnapshot.put("powerNodeOperation", "already_active");
            resultSnapshot.put("noopReason", "power node already active");
            resultSnapshot.put("hudOrEventEmitted", true);
            resultSnapshot.put("powerNodeActiveAfter", true);
            resultSnapshot.put("powerNodeEntityActivatedAfter", entityActivatedBefore);
            resultSnapshot.put("activeNodeCountAfter", activePowerNodeCount(player));
            addPowerNodeInventorySnapshot(player, resultSnapshot, "After");
            player.sendSystemMessage(Component.literal("[ECHO-7] Power Node already active. Grid contribution confirmed."));
            return false;
        }

        ItemStack held = player.getMainHandItem();
        if (!held.is(ModItems.ENERGY_CELL.get())) {
            resultSnapshot.put("powerNodeOperation", "missing_energy_cell");
            resultSnapshot.put("noopReason", "main hand is not an Energy Cell");
            resultSnapshot.put("hudOrEventEmitted", true);
            resultSnapshot.put("powerNodeActiveAfter", false);
            resultSnapshot.put("powerNodeEntityActivatedAfter", entityActivatedBefore);
            resultSnapshot.put("activeNodeCountAfter", activePowerNodeCount(player));
            addPowerNodeInventorySnapshot(player, resultSnapshot, "After");
            player.sendSystemMessage(Component.literal(
                    "[ECHO-7] Power Node dormant. Insert an Energy Cell to wake the local grid anchor."));
            return false;
        }

        resultSnapshot.put("powerNodeOperation", "activate");
        held.shrink(1);
        player.level().setBlock(pos, state.setValue(PowerNodeBlock.ACTIVE, true), 3);
        boolean entityActivatedAfter = false;
        if (player.level().getBlockEntity(pos) instanceof PowerNodeBlockEntity blockEntity) {
            blockEntity.activate();
            entityActivatedAfter = blockEntity.isActivated();
        }
        int activeNodeCountAfter = PostNexusEventHandler.recordPowerNodeActivationState(player, pos);
        payload.put("activeNodeCount", activeNodeCountAfter);

        boolean changed = true;
        changed |= applyPowerNodeActivationMarkers(player, payload);
        boolean factionProgressed = progressPowerNodeFactionContract(player, resultSnapshot);
        boolean restoreProgressed = PostNexusEventHandler.recordPowerNodeRestoreProgress(player);

        changed |= factionProgressed;
        changed |= restoreProgressed;

        resultSnapshot.put("powerNodeActiveAfter", true);
        resultSnapshot.put("powerNodeEntityActivatedAfter", entityActivatedAfter);
        resultSnapshot.put("activeNodeCountAfter", activeNodeCountAfter);
        resultSnapshot.put("factionRepairProgressed", factionProgressed);
        resultSnapshot.put("postNexusRestoreProgressed", restoreProgressed);
        resultSnapshot.put("hudOrEventEmitted", true);
        addPowerNodeInventorySnapshot(player, resultSnapshot, "After");
        player.sendSystemMessage(Component.literal(
                EchoMessages.getMessage(EchoMessages.Context.POWER_NODE_ACTIVATED)));
        return changed;
    }

    private static boolean applyInstabilityDampenerUse(
            ServerPlayer player,
            Map<String, Object> payload,
            Map<String, Object> resultSnapshot) {
        InteractionHand hand = interactionHandValue(payload);
        ItemStack dampener = player.getItemInHand(hand);
        int reduction = Math.max(0, intValue(payload, "instabilityReduction"));
        ServerLevel overworld = player.level() instanceof ServerLevel serverLevel
                ? serverLevel.getServer().overworld()
                : null;

        resultSnapshot.put("instabilityDampenerRuntime", true);
        resultSnapshot.put("instabilityReduction", reduction);
        resultSnapshot.put("hand", hand.name());
        resultSnapshot.put("dampenerCountBefore", dampener.getCount());

        if (dampener.isEmpty() || !dampener.is(ModItems.INSTABILITY_DAMPENER.get())) {
            resultSnapshot.put("failureReason", "missing instability dampener in selected hand");
            resultSnapshot.put("hudOrEventEmitted", true);
            resultSnapshot.put("dampenerCountAfter", dampener.getCount());
            player.sendSystemMessage(Component.literal("[ECHO-7] Instability Dampener handoff failed: no dampener in hand.")
                    .withStyle(ChatFormatting.RED));
            return false;
        }
        if (overworld == null) {
            resultSnapshot.put("failureReason", "missing overworld Nexus campaign level");
            resultSnapshot.put("hudOrEventEmitted", true);
            resultSnapshot.put("dampenerCountAfter", dampener.getCount());
            player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.instability_dampener.no_signal")
                    .withStyle(ChatFormatting.YELLOW));
            return false;
        }

        NexusCampaignData campaign = NexusCampaignData.get(overworld);
        int beforeInstability = campaign.getInstability();
        resultSnapshot.put("campaignAwakenedBefore", campaign.isAwakened());
        resultSnapshot.put("instabilityBefore", beforeInstability);
        if (!campaign.isAwakened()) {
            resultSnapshot.put("failureReason", "nexus campaign is dormant");
            resultSnapshot.put("hudOrEventEmitted", true);
            resultSnapshot.put("instabilityAfter", beforeInstability);
            resultSnapshot.put("dampenerCountAfter", dampener.getCount());
            player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.instability_dampener.no_signal")
                    .withStyle(ChatFormatting.YELLOW));
            return false;
        }
        if (beforeInstability <= 0 || !campaign.reduceInstability(reduction)) {
            resultSnapshot.put("noopReason", "nexus instability already stable");
            resultSnapshot.put("hudOrEventEmitted", true);
            resultSnapshot.put("instabilityAfter", campaign.getInstability());
            resultSnapshot.put("dampenerCountAfter", dampener.getCount());
            player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.instability_dampener.stable")
                    .withStyle(ChatFormatting.GRAY));
            return false;
        }

        if (!player.getAbilities().instabuild) {
            dampener.shrink(1);
        }
        NexusCampaignActions.syncCampaignState(overworld);
        int afterInstability = campaign.getInstability();
        boolean changed = true;
        changed |= markLocation(player, "special", "nexus:instability_dampened");
        changed |= recordMission(player, MissionObjectiveType.CUSTOM, "nexus/instability_dampened", 1, payload);
        changed |= recordMission(player, MissionObjectiveType.CUSTOM, EchoCanonicalContentIds.ITEM_INSTABILITY_DAMPENER, 1, payload);
        changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:use_instability_dampener", 1, payload);

        resultSnapshot.put("campaignAwakenedAfter", campaign.isAwakened());
        resultSnapshot.put("instabilityAfter", afterInstability);
        resultSnapshot.put("instabilityReducedBy", Math.max(0, beforeInstability - afterInstability));
        resultSnapshot.put("dampenerCountAfter", dampener.getCount());
        resultSnapshot.put("hudOrEventEmitted", true);
        player.sendSystemMessage(Component.translatable(
                        "message.EchoAshfallProtocol.instability_dampener.applied",
                        reduction,
                        afterInstability)
                .withStyle(ChatFormatting.AQUA));
        return changed;
    }

    private static boolean applyReturnBeaconUse(
            ServerPlayer player,
            Map<String, Object> payload,
            Map<String, Object> resultSnapshot) {
        InteractionHand hand = interactionHandValue(payload);
        ItemStack beacon = player.getItemInHand(hand);
        ServerLevel currentLevel = player.level() instanceof ServerLevel level ? level : null;
        ServerLevel overworld = currentLevel == null ? null : currentLevel.getServer().overworld();
        CompoundTag playerData = player.getPersistentData();
        long now = overworld == null ? player.level().getGameTime() : overworld.getGameTime();
        long readyAt = playerData.getLong(RETURN_BEACON_COOLDOWN_KEY).orElse(0L);
        long cooldownTicks = Math.max(0L, longValue(payload, "cooldownTicks"));

        resultSnapshot.put("returnBeaconRuntime", true);
        resultSnapshot.put("hand", hand.name());
        resultSnapshot.put("returnBeaconCountBefore", beacon.getCount());
        resultSnapshot.put("returnBeaconReadyTickBefore", readyAt);
        resultSnapshot.put("returnBeaconNowTick", now);
        resultSnapshot.put("returnBeaconCooldownTicks", cooldownTicks);
        resultSnapshot.put("playerBlockPosBefore", positionSnapshot(player.blockPosition()));

        if (beacon.isEmpty() || !beacon.is(ModItems.RETURN_BEACON.get())) {
            resultSnapshot.put("failureReason", "missing return beacon in selected hand");
            resultSnapshot.put("hudOrEventEmitted", true);
            resultSnapshot.put("returnBeaconCountAfter", beacon.getCount());
            resultSnapshot.put("returnBeaconReadyTickAfter", readyAt);
            resultSnapshot.put("playerBlockPosAfter", positionSnapshot(player.blockPosition()));
            player.sendSystemMessage(Component.literal("[ECHO-7] Return Beacon handoff failed: no beacon in hand.")
                    .withStyle(ChatFormatting.RED));
            return false;
        }
        if (overworld == null) {
            resultSnapshot.put("failureReason", "missing overworld Nexus campaign level");
            resultSnapshot.put("hudOrEventEmitted", true);
            resultSnapshot.put("returnBeaconCountAfter", beacon.getCount());
            resultSnapshot.put("returnBeaconReadyTickAfter", readyAt);
            resultSnapshot.put("playerBlockPosAfter", positionSnapshot(player.blockPosition()));
            player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.return_beacon.no_anchor")
                    .withStyle(ChatFormatting.YELLOW));
            return false;
        }

        NexusCampaignData campaign = NexusCampaignData.get(overworld);
        PostNexusData post = PostNexusData.get(player);
        BlockPos core = campaign.getNexusPos();
        resultSnapshot.put("returnBeaconWarfrontComplete", campaign.isWarfrontComplete());
        resultSnapshot.put("returnBeaconPostChoice", post.getSelectedPath().name());
        resultSnapshot.put("returnBeaconAnchor", positionSnapshot(core));
        if (!campaign.isWarfrontComplete() && !post.hasMadeChoice()) {
            resultSnapshot.put("failureReason", "return beacon locked until warfront complete or Nexus path committed");
            resultSnapshot.put("hudOrEventEmitted", true);
            resultSnapshot.put("returnBeaconCountAfter", beacon.getCount());
            resultSnapshot.put("returnBeaconReadyTickAfter", readyAt);
            resultSnapshot.put("playerBlockPosAfter", positionSnapshot(player.blockPosition()));
            player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.return_beacon.locked")
                    .withStyle(ChatFormatting.YELLOW));
            return false;
        }
        if (core == null || core.equals(BlockPos.ZERO)) {
            resultSnapshot.put("failureReason", "no saved Nexus Core anchor is available");
            resultSnapshot.put("hudOrEventEmitted", true);
            resultSnapshot.put("returnBeaconCountAfter", beacon.getCount());
            resultSnapshot.put("returnBeaconReadyTickAfter", readyAt);
            resultSnapshot.put("playerBlockPosAfter", positionSnapshot(player.blockPosition()));
            player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.return_beacon.no_anchor")
                    .withStyle(ChatFormatting.YELLOW));
            return false;
        }
        if (readyAt > now && !player.getAbilities().instabuild) {
            long seconds = Math.max(1L, (readyAt - now + 19L) / 20L);
            resultSnapshot.put("noopReason", "return beacon is recharging");
            resultSnapshot.put("rechargeSecondsRemaining", seconds);
            resultSnapshot.put("hudOrEventEmitted", true);
            resultSnapshot.put("returnBeaconCountAfter", beacon.getCount());
            resultSnapshot.put("returnBeaconReadyTickAfter", readyAt);
            resultSnapshot.put("playerBlockPosAfter", positionSnapshot(player.blockPosition()));
            player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.return_beacon.recharging", seconds)
                    .withStyle(ChatFormatting.GRAY));
            return false;
        }

        long nextReadyAt = now + cooldownTicks;
        playerData.putLong(RETURN_BEACON_COOLDOWN_KEY, nextReadyAt);
        player.teleportTo(overworld, core.getX() + 0.5D, core.getY() + 1.0D, core.getZ() + 0.5D,
                Set.of(), player.getYRot(), player.getXRot(), false);
        overworld.playSound(null, core, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.85F, 0.95F);

        boolean changed = true;
        changed |= markLocation(player, "special", "return_beacon_activated");
        changed |= markLocation(player, "special", "return_beacon:returned");
        changed |= recordMission(player, MissionObjectiveType.CUSTOM, EchoCanonicalContentIds.ITEM_RETURN_BEACON, 1, payload);
        changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:place_return_beacon", 1, payload);

        resultSnapshot.put("returnBeaconReadyTickAfter", nextReadyAt);
        resultSnapshot.put("returnBeaconCountAfter", beacon.getCount());
        resultSnapshot.put("playerBlockPosAfter", positionSnapshot(player.blockPosition()));
        resultSnapshot.put("hudOrEventEmitted", true);
        player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.return_beacon.returned")
                .withStyle(ChatFormatting.AQUA));
        return changed;
    }

    private static boolean applyScoutDroneItemUse(
            ServerPlayer player,
            Map<String, Object> payload,
            Map<String, Object> resultSnapshot) {
        InteractionHand hand = interactionHandValue(payload);
        ItemStack droneItem = player.getItemInHand(hand);
        String routeId = stringValue(payload, "routeId");
        String mode = stringValue(payload, "mode");

        resultSnapshot.put("scoutDroneItemRuntime", true);
        resultSnapshot.put("hand", hand.name());
        resultSnapshot.put("itemId", EchoCanonicalContentIds.ITEM_SCOUT_DRONE_ITEM);
        resultSnapshot.put("routeId", routeId);
        resultSnapshot.put("mode", mode);
        resultSnapshot.put("scoutDroneItemCountBefore", countInventory(player, ModItems.SCOUT_DRONE_ITEM.get()));
        resultSnapshot.put("playerBlockPosBefore", positionSnapshot(player.blockPosition()));

        if (droneItem.isEmpty() || !droneItem.is(ModItems.SCOUT_DRONE_ITEM.get())) {
            resultSnapshot.put("failureReason", "missing Scout Drone item in selected hand");
            resultSnapshot.put("hudOrEventEmitted", true);
            resultSnapshot.put("scoutDroneItemCountAfter", countInventory(player, ModItems.SCOUT_DRONE_ITEM.get()));
            resultSnapshot.put("playerBlockPosAfter", positionSnapshot(player.blockPosition()));
            player.sendSystemMessage(Component.literal("[ECHO-7 // DRONE] Scout Drone handoff failed: no drone in hand.")
                    .withStyle(ChatFormatting.RED));
            return false;
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            resultSnapshot.put("failureReason", "missing server level for Scout Drone deployment");
            resultSnapshot.put("hudOrEventEmitted", true);
            resultSnapshot.put("scoutDroneItemCountAfter", countInventory(player, ModItems.SCOUT_DRONE_ITEM.get()));
            resultSnapshot.put("playerBlockPosAfter", positionSnapshot(player.blockPosition()));
            player.sendSystemMessage(Component.literal("[ECHO-7 // DRONE] Scout Drone deployment failed: server level unavailable.")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        Vec3 lookVec = player.getLookAngle();
        double spawnX = player.getX() + lookVec.x * 2.0D;
        double spawnY = player.getY() + 1.5D;
        double spawnZ = player.getZ() + lookVec.z * 2.0D;
        ScoutDrone drone = new ScoutDrone(ModEntities.SCOUT_DRONE.get(), serverLevel);
        drone.setPos(spawnX, spawnY, spawnZ);
        drone.setOwner(player);
        boolean deployed = serverLevel.addFreshEntity(drone);

        resultSnapshot.put("scoutDroneSpawnAttempted", true);
        resultSnapshot.put("scoutDroneSpawned", deployed);
        resultSnapshot.put("scoutDroneEntityId", drone.getUUID().toString());
        resultSnapshot.put("scoutDroneSpawnPos", positionSnapshot(drone.blockPosition()));
        if (!deployed) {
            resultSnapshot.put("failureReason", "server rejected Scout Drone entity spawn");
            resultSnapshot.put("hudOrEventEmitted", true);
            resultSnapshot.put("scoutDroneItemCountAfter", countInventory(player, ModItems.SCOUT_DRONE_ITEM.get()));
            resultSnapshot.put("playerBlockPosAfter", positionSnapshot(player.blockPosition()));
            player.sendSystemMessage(Component.literal("[ECHO-7 // DRONE] Scout Drone deployment failed.")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        if (!player.getAbilities().instabuild) {
            droneItem.shrink(1);
        }

        boolean changed = true;
        changed |= applyScoutDroneRouteMarkers(player, payload);
        changed |= markLocation(player, "special", "drone:scout_deployed");
        changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:repair_echo_drone", 1, payload);

        resultSnapshot.put("scoutDroneItemCountAfter", countInventory(player, ModItems.SCOUT_DRONE_ITEM.get()));
        resultSnapshot.put("playerBlockPosAfter", positionSnapshot(player.blockPosition()));
        resultSnapshot.put("hudOrEventEmitted", true);
        player.sendSystemMessage(Component.literal(
                EchoMessages.getMessage(EchoMessages.Context.SCOUT_DRONE_DEPLOYED)));
        return changed;
    }

    private static boolean applyScoutDroneRouteMarkers(ServerPlayer player, Map<String, Object> payload) {
        boolean changed = false;
        changed |= markLocation(player, "special", "drone:scout_route");
        changed |= recordMission(player, MissionObjectiveType.CUSTOM, "drone/scout_route", 1, payload);
        changed |= recordMission(player, MissionObjectiveType.DELIVER_ITEM, EchoCanonicalContentIds.ITEM_SCOUT_DRONE_ITEM, 1, payload);
        changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:build_scout_drone", 1, payload);
        return changed;
    }

    private static boolean applyPowerNodeActivationMarkers(ServerPlayer player, Map<String, Object> payload) {
        boolean changed = false;
        changed |= recordMission(player, MissionObjectiveType.PLACE_BLOCK, EchoCanonicalContentIds.BLOCK_POWER_NODE, 1, payload);
        changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:activate_power_node", 1, payload);
        if (intValue(payload, "activeNodeCount") >= 5) {
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:stabilize_nexus_grid", 1, payload);
        }
        changed |= markLocation(player, "special", "power_node:activated");
        return changed;
    }

    private static boolean progressPowerNodeFactionContract(
            ServerPlayer player,
            Map<String, Object> resultSnapshot) {
        try {
            return AshfallFactionContractProgression.progressRepair(player, "power_node");
        } catch (RuntimeException exception) {
            resultSnapshot.put("factionProgressFailure", exception.getMessage() == null
                    ? exception.getClass().getName()
                    : exception.getMessage());
            return false;
        }
    }

    private static boolean applyRelayStationUse(
            ServerPlayer player,
            Map<String, Object> payload,
            Map<String, Object> resultSnapshot) {
        BlockPos pos = blockPosValue(payload.get("targetBlockPos"));
        BlockState state = player.level().getBlockState(pos);
        resultSnapshot.put("relayStationRuntime", true);
        resultSnapshot.put("relayStationPos", positionSnapshot(pos));
        resultSnapshot.put("blockId", stringValue(payload, "blockId"));
        resultSnapshot.put("shiftTravelRequested", booleanValue(payload, "shiftTravelRequested"));

        if (!(state.getBlock() instanceof RelayStationBlock)) {
            resultSnapshot.put("failureReason", "target block is not a relay station");
            resultSnapshot.put("hudOrEventEmitted", true);
            player.sendSystemMessage(Component.literal("[ECHO-7] Relay station link unavailable.")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        RadioNetwork.StationInfo station = StationRegistry.getOrCreateStation(pos);
        boolean repaired = state.getValue(RelayStationBlock.REPAIRED);
        boolean active = state.getValue(RelayStationBlock.ACTIVE);
        resultSnapshot.put("relayStationId", station.getId());
        resultSnapshot.put("relayStationName", station.getName());
        resultSnapshot.put("relayStationRepairedBefore", repaired);
        resultSnapshot.put("relayStationActiveBefore", active);

        if (!repaired) {
            return applyRelayStationRepair(player, payload, pos, state, station, resultSnapshot);
        }
        if (!active) {
            return applyRelayStationActivation(player, payload, pos, state, station, resultSnapshot);
        }
        return applyActiveRelayStationUse(player, pos, station, resultSnapshot);
    }

    private static boolean applyRelayStationRepair(
            ServerPlayer player,
            Map<String, Object> payload,
            BlockPos pos,
            BlockState state,
            RadioNetwork.StationInfo station,
            Map<String, Object> resultSnapshot) {
        resultSnapshot.put("relayOperation", "repair");
        addRelayInventorySnapshot(player, resultSnapshot, "Before");
        boolean hasMaterials = countInventory(player, ModItems.POWER_CELL.get()) >= 1
                && countInventory(player, ModItems.CIRCUIT_BOARD.get()) >= 1
                && countInventory(player, ModItems.SCRAP_CIRCUIT.get()) >= 2;

        if (!hasMaterials) {
            RadioNetwork network = RadioNetwork.get(player);
            boolean alreadyDiscovered = network.isDiscovered(station.getId());
            if (!alreadyDiscovered) {
                network.discoverStation(station);
                saveRadioNetwork(player, network);
            }
            resultSnapshot.put("relayOperation", "repair_missing_materials");
            resultSnapshot.put("relayStationDiscovered", !alreadyDiscovered);
            resultSnapshot.put("noopReason", alreadyDiscovered ? "missing repair materials" : "");
            resultSnapshot.put("hudOrEventEmitted", true);
            addRelayInventorySnapshot(player, resultSnapshot, "After");
            player.sendSystemMessage(Component.literal("[ECHO-7] Relay Station unsealed but unrepaired.")
                    .withStyle(ChatFormatting.RED));
            player.sendSystemMessage(Component.literal("Required: Power Cell, Circuit Board, 2 Scrap Circuits.")
                    .withStyle(ChatFormatting.GRAY));
            return !alreadyDiscovered;
        }

        if (!consumeItem(player, ModItems.POWER_CELL.get(), 1)
                || !consumeItem(player, ModItems.CIRCUIT_BOARD.get(), 1)
                || !consumeItem(player, ModItems.SCRAP_CIRCUIT.get(), 2)) {
            resultSnapshot.put("failureReason", "repair materials disappeared during runtime consumption");
            resultSnapshot.put("hudOrEventEmitted", true);
            addRelayInventorySnapshot(player, resultSnapshot, "After");
            player.sendSystemMessage(Component.literal("[ECHO-7] Repair failed. Required materials desynced during handoff.")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        player.level().setBlock(pos, state.setValue(RelayStationBlock.REPAIRED, true), 3);
        RadioNetwork network = RadioNetwork.get(player);
        boolean alreadyDiscovered = network.isDiscovered(station.getId());
        network.discoverStation(station);
        if (!alreadyDiscovered) {
            saveRadioNetwork(player, network);
        }
        int researchAdded = addResearchPoints(player, 15);
        AshfallFactionContractProgression.progressRepair(player, "relay");

        boolean changed = true;
        changed |= recordMission(player, MissionObjectiveType.PLACE_BLOCK, EchoCanonicalContentIds.BLOCK_RELAY_STATION, 1, payload);
        changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:ashfall_relay_station_repair", 1, payload);

        resultSnapshot.put("relayStationRepairedAfter", true);
        resultSnapshot.put("relayStationDiscovered", !alreadyDiscovered);
        resultSnapshot.put("researchPointsAdded", researchAdded);
        resultSnapshot.put("hudOrEventEmitted", true);
        addRelayInventorySnapshot(player, resultSnapshot, "After");
        player.sendSystemMessage(Component.literal("[ECHO-7] Relay Station repaired. Radio spine is listening.")
                .withStyle(ChatFormatting.GREEN));
        player.sendSystemMessage(Component.literal("Activate with a Power Cell to open the route network.")
                .withStyle(ChatFormatting.YELLOW));
        return changed;
    }

    private static boolean applyRelayStationActivation(
            ServerPlayer player,
            Map<String, Object> payload,
            BlockPos pos,
            BlockState state,
            RadioNetwork.StationInfo station,
            Map<String, Object> resultSnapshot) {
        resultSnapshot.put("relayOperation", "activate");
        addRelayInventorySnapshot(player, resultSnapshot, "Before");
        if (countInventory(player, ModItems.POWER_CELL.get()) < 1) {
            resultSnapshot.put("relayOperation", "activate_missing_materials");
            resultSnapshot.put("noopReason", "missing activation power cell");
            resultSnapshot.put("hudOrEventEmitted", true);
            addRelayInventorySnapshot(player, resultSnapshot, "After");
            player.sendSystemMessage(Component.literal("[ECHO-7] Relay hardware repaired. Activation cell missing.")
                    .withStyle(ChatFormatting.YELLOW));
            player.sendSystemMessage(Component.literal("Required: Power Cell.")
                    .withStyle(ChatFormatting.GRAY));
            return false;
        }

        if (!consumeItem(player, ModItems.POWER_CELL.get(), 1)) {
            resultSnapshot.put("failureReason", "activation power cell disappeared during runtime consumption");
            resultSnapshot.put("hudOrEventEmitted", true);
            addRelayInventorySnapshot(player, resultSnapshot, "After");
            player.sendSystemMessage(Component.literal("[ECHO-7] Activation failed. Power Cell missing during handoff.")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        player.level().setBlock(pos, state.setValue(RelayStationBlock.ACTIVE, true), 3);
        RadioNetwork network = RadioNetwork.get(player);
        boolean alreadyActivated = network.isActivated(station.getId());
        network.activateStation(station);
        saveRadioNetwork(player, network);
        boolean mapAreaRevealed = revealRelayMapArea(player, pos);
        int researchAdded = addResearchPoints(player, 10);
        AshfallFactionContractProgression.progressRepair(player, "relay");

        boolean changed = true;
        changed |= applyRelayActivationMarkers(player, payload);
        changed |= markLocation(player, "special", "relay:" + sanitizeTarget(station.getId()));

        resultSnapshot.put("relayStationActiveAfter", true);
        resultSnapshot.put("relayStationActivated", !alreadyActivated);
        resultSnapshot.put("mapAreaRevealed", mapAreaRevealed);
        resultSnapshot.put("researchPointsAdded", researchAdded);
        resultSnapshot.put("hudOrEventEmitted", true);
        addRelayInventorySnapshot(player, resultSnapshot, "After");
        player.sendSystemMessage(Component.literal("[ECHO-7] Relay Station active. Radio route added to your network.")
                .withStyle(ChatFormatting.GREEN));
        return changed;
    }

    private static boolean applyActiveRelayStationUse(
            ServerPlayer player,
            BlockPos pos,
            RadioNetwork.StationInfo station,
            Map<String, Object> resultSnapshot) {
        resultSnapshot.put("relayOperation", "open");
        RadioNetwork network = RadioNetwork.get(player);
        boolean alreadyActivated = network.isActivated(station.getId());
        if (!alreadyActivated) {
            network.activateStation(station);
            saveRadioNetwork(player, network);
        }

        player.sendSystemMessage(Component.literal("[ECHO-7] Radio network online.")
                .withStyle(ChatFormatting.AQUA));
        var destinations = network.getAvailableDestinations(pos);
        resultSnapshot.put("availableRelayDestinations", destinations.size());
        resultSnapshot.put("relayStationActivated", !alreadyActivated);
        resultSnapshot.put("hudOrEventEmitted", true);

        boolean changed = !alreadyActivated;
        if (destinations.isEmpty()) {
            resultSnapshot.put("noopReason", changed ? "" : "no other active relay stations found");
            player.sendSystemMessage(Component.literal("[ECHO-7] No other active relay stations found.")
                    .withStyle(ChatFormatting.RED));
            player.sendSystemMessage(Component.literal("Activate another Relay Station to create a return route.")
                    .withStyle(ChatFormatting.GRAY));
            return changed;
        }

        if (booleanValue(resultSnapshot, "shiftTravelRequested")) {
            RadioNetwork.StationInfo nearest = destinations.stream()
                    .min(java.util.Comparator.comparingDouble(dest -> dest.getPosition().distSqr(pos)))
                    .orElse(null);
            if (nearest != null) {
                addRelayInventorySnapshot(player, resultSnapshot, "Before");
                resultSnapshot.put("fastTravelDestinationId", nearest.getId());
                resultSnapshot.put("playerBlockPosBefore", positionSnapshot(player.blockPosition()));
                boolean traveled = network.fastTravelTo(player, nearest.getId());
                if (traveled) {
                    saveRadioNetwork(player, network);
                }
                changed |= traveled;
                resultSnapshot.put("fastTravelSucceeded", traveled);
                resultSnapshot.put("playerBlockPosAfter", positionSnapshot(player.blockPosition()));
                addRelayInventorySnapshot(player, resultSnapshot, "After");
                if (!traveled) {
                    resultSnapshot.put("noopReason", "fast travel failed validation");
                }
                return changed;
            }
        }

        player.sendSystemMessage(Component.literal("Available relay destinations:")
                .withStyle(ChatFormatting.GREEN));
        for (var destination : destinations) {
            int distance = (int) Math.sqrt(destination.getPosition().distSqr(pos));
            player.sendSystemMessage(Component.literal("  - " + destination.getName() + " [" + distance + "m]")
                    .withStyle(ChatFormatting.YELLOW));
        }
        player.sendSystemMessage(Component.literal("Sneak-use this relay to travel to the nearest listed station. Cost: 1 Power Cell or Energy Cell.")
                .withStyle(ChatFormatting.GRAY));
        if (!changed) {
            resultSnapshot.put("noopReason", "listed active relay destinations");
        }
        return changed;
    }

    private static boolean applyRelayActivationMarkers(ServerPlayer player, Map<String, Object> payload) {
        boolean changed = false;
        changed |= markLocation(player, "special", "relay:activated");
        changed |= markLocation(player, "special", "relay:" + sanitizeTarget(stringValue(payload, "relayId")));
        changed |= recordMission(player, MissionObjectiveType.PLACE_BLOCK, EchoCanonicalContentIds.BLOCK_RELAY_STATION, 1, payload);
        changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:activate_relay_station", 1, payload);
        return changed;
    }

    private static boolean revealRelayMapArea(ServerPlayer player, BlockPos pos) {
        QuestData quest = QuestData.get(player);
        quest.visitLocation("special", "relay:map_revealed");
        quest.addToArchive("[RELAY] Radio map sweep centered at " + pos.getX() + ", " + pos.getZ() + ".");
        QuestData.saveAndSync(player, quest);
        return true;
    }

    private static int addResearchPoints(ServerPlayer player, int points) {
        ResearchData research = ResearchData.get(player);
        int added = research.addPoints(points);
        if (added > 0) {
            ResearchData.saveAndSync(player, research);
        }
        return added;
    }

    private static void saveRadioNetwork(ServerPlayer player, RadioNetwork network) {
        player.setData(ModAttachments.RADIO_NETWORK.get(), network);
        player.syncData(ModAttachments.RADIO_NETWORK.get());
    }

    private static void addRelayInventorySnapshot(
            ServerPlayer player,
            Map<String, Object> resultSnapshot,
            String suffix) {
        resultSnapshot.put("powerCellCount" + suffix, countInventory(player, ModItems.POWER_CELL.get()));
        resultSnapshot.put("circuitBoardCount" + suffix, countInventory(player, ModItems.CIRCUIT_BOARD.get()));
        resultSnapshot.put("scrapCircuitCount" + suffix, countInventory(player, ModItems.SCRAP_CIRCUIT.get()));
        resultSnapshot.put("energyCellCount" + suffix, countInventory(player, ModItems.ENERGY_CELL.get()));
    }

    private static void addPowerNodeInventorySnapshot(
            ServerPlayer player,
            Map<String, Object> resultSnapshot,
            String suffix) {
        resultSnapshot.put("energyCellCount" + suffix, countInventory(player, ModItems.ENERGY_CELL.get()));
        resultSnapshot.put("mainHandEnergyCell" + suffix, player.getMainHandItem().is(ModItems.ENERGY_CELL.get()));
        resultSnapshot.put("mainHandCount" + suffix, player.getMainHandItem().getCount());
    }

    private static int activePowerNodeCount(ServerPlayer player) {
        if (player.level() instanceof ServerLevel serverLevel) {
            return NexusWorldData.get(serverLevel).getActiveNodePositions().size();
        }
        return 0;
    }

    private static int countInventory(ServerPlayer player, Item item) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static boolean consumeItem(ServerPlayer player, Item item, int count) {
        if (countInventory(player, item) < count) {
            return false;
        }
        int remaining = count;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                int toRemove = Math.min(stack.getCount(), remaining);
                stack.shrink(toRemove);
                remaining -= toRemove;
            }
        }
        return remaining == 0;
    }

    private static boolean recordNexusStateMission(ServerPlayer player, String state, Map<String, Object> payload) {
        return switch (state) {
            case "awakened" ->
                    recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:awaken_nexus_core", 1, payload);
            case "prime_relays_scanned" ->
                    recordMission(player, MissionObjectiveType.SCAN_BLOCK, "echoashfallprotocol:scan_prime_relays", 1, payload);
            case "siege_complete" ->
                    recordMission(player, MissionObjectiveType.SURVIVE_TIME, "echoashfallprotocol:survive_core_countermeasure", 1, payload);
            case "finale_complete" ->
                    recordMission(player, MissionObjectiveType.CUSTOM, "nexus/finale_complete", 1, payload);
            default -> {
                yield false;
            }
        };
    }

    private static boolean recordPostNexusMission(ServerPlayer player, String path, Map<String, Object> payload) {
        boolean changed = false;
        if (intValue(payload, "relaysResolved") >= 3) {
            changed |= recordMission(player, MissionObjectiveType.ESTABLISH_ROUTE, "echoashfallprotocol:resolve_prime_relays", 1, payload);
        }
        if (intValue(payload, "pathOperationsComplete") >= 1) {
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:" + pathOperationMission(path), 1, payload);
        }
        if (Boolean.TRUE.equals(payload.get("wardenDefeated"))) {
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:" + path + "_guardian", 1, payload);
        }
        if (Boolean.TRUE.equals(payload.get("finalBossDefeated"))) {
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:" + path + "_finale", 1, payload);
        }
        if (Boolean.TRUE.equals(payload.get("epilogueComplete"))) {
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:" + path + "_epilogue", 1, payload);
        }
        return changed;
    }

    private static String guardianBossMission(String bossId) {
        return switch (bossId) {
            case "plains_warlord" -> "neutralize_plains_warlord";
            case "city_ruin_stalker" -> "neutralize_city_ruin_stalker";
            case "industrial_juggernaut" -> "neutralize_industrial_juggernaut";
            case "toxic_hive_matriarch" -> "neutralize_toxic_hive_matriarch";
            case "crash_zone_colossus" -> "neutralize_crash_zone_colossus";
            case "radiation_behemoth" -> "neutralize_radiation_behemoth";
            case "cryogenic_overseer" -> "neutralize_cryogenic_overseer";
            case "nexus_scar_avatar" -> "neutralize_nexus_scar_avatar";
            default -> "";
        };
    }

    private static String pathOperationMission(String path) {
        return switch (path) {
            case "restore" -> "restore_world_lattice";
            case "destroy" -> "destroy_dead_signal";
            case "control" -> "control_command_lattice";
            default -> path + "_operation";
        };
    }

    private static boolean markLocation(ServerPlayer player, String category, String marker) {
        if (marker == null || marker.isBlank()) {
            return false;
        }
        QuestData quest = QuestData.get(player);
        if (quest.hasVisitedLocation(category, marker)) {
            return false;
        }
        quest.visitLocation(category, marker);
        QuestData.saveAndSync(player, quest);
        return true;
    }

    private static boolean recordMission(
            ServerPlayer player,
            MissionObjectiveType type,
            String target,
            int count,
            Map<String, Object> payload) {
        Identifier targetId = targetId(target);
        if (targetId == null) {
            return false;
        }
        AshfallAdapterCoreRuntimeGuards.ensureMissionContentReady(player, "late");
        return EchoCoreServices.recordMissionObjective(
                player,
                type,
                targetId,
                Math.max(1, count),
                Map.of(
                        "source", EchoAshfallProtocol.MODID,
                        "adapterCoreEvent", String.valueOf(payload.getOrDefault("source", "late_runtime"))));
    }

    private static NativeMutationContext context(ServerPlayer player, String eventId) {
        String dimensionId = player.level() instanceof ServerLevel level
                ? level.dimension().identifier().toString()
                : "unknown";
        return new NativeMutationContext(
                EchoAshfallProtocol.MODID,
                dimensionId,
                "event." + eventId,
                "SERVER",
                player.level().getGameTime(),
                Map.of(
                        "nativeInterface", "EchoNativeRuntimeHost.Events",
                        "nativeMethod", "publish",
                        "hostRuntime", "native_loader",
                        "runtimeLane", "Native Loader",
                        "compatibilityFallback", "legacy_backend"));
    }

    private static Map<String, Object> basePayload(String target, String source) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("target", safe(target));
        payload.put("source", safe(source));
        return payload;
    }

    private static Map<String, Object> campaignSnapshot(@Nullable NexusCampaignData campaign) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        if (campaign == null) {
            snapshot.put("campaignAvailable", false);
            return snapshot;
        }
        snapshot.put("campaignAvailable", true);
        snapshot.put("awakened", campaign.isAwakened());
        snapshot.put("instability", campaign.getInstability());
        snapshot.put("scannedRelayCount", campaign.getScannedRelayCount());
        snapshot.put("resolvedRelayCount", campaign.getResolvedRelayCount());
        snapshot.put("siegeComplete", campaign.isSiegeComplete());
        snapshot.put("wardenDefeated", campaign.isWardenDefeated());
        snapshot.put("finaleComplete", campaign.isFinaleComplete());
        snapshot.put("nexusPos", positionSnapshot(campaign.getNexusPos()));
        return snapshot;
    }

    private static Map<String, Object> worldSnapshot(@Nullable NexusWorldData worldData) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        if (worldData == null) {
            snapshot.put("worldDataAvailable", false);
            return snapshot;
        }
        snapshot.put("worldDataAvailable", true);
        snapshot.put("worldState", worldData.getState().name());
        snapshot.put("choiceMade", worldData.hasChoiceBeenMade());
        snapshot.put("activePowerNodeCount", worldData.getActiveNodePositions().size());
        snapshot.put("nexusWorldPos", positionSnapshot(worldData.getNexusPos()));
        return snapshot;
    }

    private static Identifier targetId(String target) {
        if (target == null || target.isBlank()) {
            return null;
        }
        Identifier parsed = Identifier.tryParse(target);
        if (parsed != null) {
            return parsed;
        }
        return Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, sanitizeTarget(target));
    }

    private static String sanitizeTarget(String target) {
        return target == null || target.isBlank()
                ? "unknown"
                : target.replace(':', '/').replace(' ', '_').toLowerCase(java.util.Locale.ROOT);
    }

    private static String stringValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static int intValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String string) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static long longValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String string) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    private static BlockPos blockPosValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return new BlockPos(
                    intValue(map.get("x")),
                    intValue(map.get("y")),
                    intValue(map.get("z")));
        }
        return BlockPos.ZERO;
    }

    private static int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static boolean booleanValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value instanceof Boolean bool ? bool : Boolean.parseBoolean(String.valueOf(value));
    }

    private static InteractionHand interactionHandValue(Map<String, Object> payload) {
        try {
            return InteractionHand.valueOf(stringValue(payload, "hand"));
        } catch (IllegalArgumentException exception) {
            return InteractionHand.MAIN_HAND;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static Map<String, Object> positionSnapshot(BlockPos pos) {
        if (pos == null) {
            return Map.of("x", 0, "y", 0, "z", 0);
        }
        return Map.of(
                "x", pos.getX(),
                "y", pos.getY(),
                "z", pos.getZ());
    }

    private static NativeResult skipped(String message) {
        return new NativeResult(false, "SKIPPED", message, Map.of(
                "eventId", "ashfall.late_runtime",
                "realNativeStateMutated", false));
    }
}
