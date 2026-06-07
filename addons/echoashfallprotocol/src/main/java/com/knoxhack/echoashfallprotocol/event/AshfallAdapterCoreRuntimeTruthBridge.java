package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeBlockRef;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeEvent;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationContext;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationTarget;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativePlayerRef;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativePosition;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import com.knoxhack.echo.adaptercore.EchoRuntimeActionDispatcher;
import com.knoxhack.echo.adaptercore.EchoRuntimeActionDispatcher.EchoRuntimeAction;
import com.knoxhack.echo.adaptercore.EchoRuntimeActionDispatcher.EchoRuntimeActionOutcome;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.block.PowerNodeBlock;
import com.knoxhack.echoashfallprotocol.block.RelayStationBlock;
import com.knoxhack.echoashfallprotocol.block.entity.PowerNodeBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.SignalScannerBlockEntity;
import com.knoxhack.echoashfallprotocol.echo.EchoIntel;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.fasttravel.RadioNetwork;
import com.knoxhack.echoashfallprotocol.machine.MachineWearData;
import com.knoxhack.echoashfallprotocol.nativebridge.AshfallNativeRuntimeMutationEvidence;
import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import com.knoxhack.echoashfallprotocol.research.ResearchData;
import com.knoxhack.echoashfallprotocol.survival.MutationData;
import com.knoxhack.echoashfallprotocol.survival.SurvivalData;
import com.knoxhack.echoashfallprotocol.endgame.PostNexusData;
import com.knoxhack.echoashfallprotocol.world.NexusCampaignData;
import com.knoxhack.echoashfallprotocol.world.NexusWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class AshfallAdapterCoreRuntimeTruthBridge {
    private AshfallAdapterCoreRuntimeTruthBridge() {
    }

    static RuntimeBinding binding(
            String runtimeHostId,
            String lane,
            String lastEventKey,
            String lastEventTickKey,
            Set<String> actionIds,
            Set<String> canonicalContentIds,
            PlayerEventApplier applier) {
        return new RuntimeBinding(
                runtimeHostId,
                lane,
                lastEventKey,
                lastEventTickKey,
                actionIds,
                canonicalContentIds,
                applier);
    }

    static NativeResult publish(
            RuntimeBinding binding,
            @Nullable ServerPlayer player,
            String eventId,
            Map<String, Object> payload,
            @Nullable BlockPos requiredLoadedPos,
            boolean dedupeSameTick) {
        OptionalGuard guard = guard(binding, player, eventId, payload, requiredLoadedPos, dedupeSameTick);
        if (guard.guarded()) {
            return guard.result();
        }

        binding.ensureRegistered();
        NativeMutationContext mutationContext = context(binding, player, eventId);
        NativeEvent event = new NativeEvent(eventId, new NativePlayerRef(player.getUUID().toString()), payload);
        EchoRuntimeAction action = new EchoRuntimeAction(
                eventId,
                binding.runtimeHostId,
                payload,
                event.player(),
                mutationContext.dimensionId(),
                playerPosition(player, mutationContext.dimensionId()),
                blockRef(requiredLoadedPos, mutationContext.dimensionId()),
                mutationContext);
        NativeResult result = EchoRuntimeActionDispatcher.global().dispatch(action, (host, dispatchedAction) -> {
            RuntimeHost runtimeHost = (RuntimeHost) host;
            Map<String, Object> before = binding.mutationSummary(player, eventId, payload, "before");
            NativeResult rawResult = runtimeHost.publishForPlayer(player, event, mutationContext);
            NativeResult nativeLoaderResult = recordNativeLoaderBackendEvent(player, event, rawResult);
            NativeResult enrichedResult = enrichDispatchedResult(nativeLoaderResult, action, true, true);
            Map<String, Object> after = binding.mutationSummary(player, eventId, payload, "after");
            return EchoRuntimeActionOutcome.of(
                    before,
                    enrichedResult,
                    after,
                    Boolean.TRUE.equals(enrichedResult.snapshot().get("saveTouched")),
                    Boolean.TRUE.equals(enrichedResult.snapshot().get("hudOrEventEmitted")));
        });
        recordMutationEvidence(binding, result);
        return result;
    }

    private static OptionalGuard guard(
            RuntimeBinding binding,
            @Nullable ServerPlayer player,
            String eventId,
            Map<String, Object> payload,
            @Nullable BlockPos requiredLoadedPos,
            boolean dedupeSameTick) {
        var guarded = AshfallAdapterCoreRuntimeGuards.guardPublish(
                player,
                binding.lane,
                eventId,
                payload,
                requiredLoadedPos,
                dedupeSameTick);
        if (guarded.isEmpty()) {
            return OptionalGuard.none();
        }
        NativeResult result = guarded.get();
        if (player != null) {
            String dimensionId = player.level() instanceof ServerLevel level
                    ? level.dimension().identifier().toString()
                    : "unknown";
            EchoRuntimeMutationLedger.global().append(
                    eventId,
                    binding.runtimeHostId,
                    payload,
                    new NativeMutationTarget(
                            new NativePlayerRef(player.getUUID().toString()),
                            dimensionId,
                            playerPosition(player, dimensionId),
                            blockRef(requiredLoadedPos, dimensionId)),
                    binding.mutationSummary(player, eventId, payload, "before_guard"),
                    binding.mutationSummary(player, eventId, payload, "after_guard"),
                    result,
                    false,
                    false);
        }
        return OptionalGuard.of(result);
    }

    private static NativeMutationContext context(RuntimeBinding binding, ServerPlayer player, String eventId) {
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
                        "gameplayLane", binding.lane,
                        "runtimeHostId", binding.runtimeHostId,
                        "compatibilityBackend", ""));
    }

    private static NativeResult recordNativeLoaderBackendEvent(
            ServerPlayer player,
            NativeEvent event,
            NativeResult result) {
        if (result == null) {
            return null;
        }
        try {
            NativeLoaderEchoRuntimeHost nativeHost = NativeLoaderRuntimeHostFactory.createBackendFirst();
            return nativeHost.recordExternalRuntimeEvent(event, result);
        } catch (Throwable throwable) {
            Map<String, Object> snapshot = new LinkedHashMap<>(result.snapshot());
            snapshot.put("nativeLoaderBackendAttached", false);
            snapshot.put("nativeLoaderBackendCallAttempted", true);
            snapshot.put("nativeLoaderBackendCallFailure", true);
            snapshot.put("nativeLoaderRuntimeHostId", NativeLoaderEchoRuntimeHost.RUNTIME_HOST_ID);
            snapshot.put("nativeLoaderBridgeFailureClass", throwable.getClass().getName());
            snapshot.put("nativeLoaderBridgeFailureMessage",
                    throwable.getMessage() == null ? throwable.getClass().getName() : throwable.getMessage());
            return new NativeResult(result.mutated(), result.status(), result.message(), Map.copyOf(snapshot));
        }
    }

    private static NativePosition playerPosition(ServerPlayer player, String dimensionId) {
        Vec3 position = player.position();
        return new NativePosition(
                dimensionId,
                position.x(),
                position.y(),
                position.z(),
                player.getYRot(),
                player.getXRot());
    }

    private static NativeBlockRef blockRef(@Nullable BlockPos pos, String dimensionId) {
        return pos == null ? null : new NativeBlockRef(dimensionId, pos.getX(), pos.getY(), pos.getZ());
    }

    private static NativeResult enrichDispatchedResult(
            NativeResult result,
            EchoRuntimeAction action,
            boolean saveTouchedWhenMutated,
            boolean hudOrEventWhenMutated) {
        if (result == null) {
            return NativeResult.failed("Runtime host action returned no NativeResult.", Map.of(
                    "adapterCoreActionDispatched", true,
                    "adapterCoreActionId", action.actionId(),
                    "runtimeHostId", action.runtimeHostId(),
                    "runtimeHostResolved", true,
                    "saveTouched", false,
                    "hudOrEventEmitted", false,
                    "failureReason", "missing runtime result"));
        }
        Map<String, Object> snapshot = new LinkedHashMap<>(result.snapshot());
        snapshot.put("adapterCoreActionDispatched", true);
        snapshot.put("adapterCoreActionId", action.actionId());
        List<String> sourceOperationIds = sourceOperationIds(action.actionId(), action.inputPayload());
        snapshot.put("adapterCoreSourceOperationId", sourceOperationIds.get(0));
        snapshot.put("adapterCoreSourceOperationIds", sourceOperationIds);
        List<String> hostCallAdapterIds = hostCallAdapterIds(action.actionId(), action.inputPayload());
        if (!hostCallAdapterIds.isEmpty()) {
            snapshot.put("adapterCoreHostCallAdapterId", hostCallAdapterIds.get(0));
            snapshot.put("adapterCoreHostCallAdapterIds", hostCallAdapterIds);
        }
        snapshot.put("runtimeHostId", action.runtimeHostId());
        snapshot.put("runtimeHostResolved", true);
        snapshot.put("minecraftRuntimeAccessed", result.mutated());
        snapshot.put("minecraftRuntimeMutated", result.mutated());
        snapshot.put("saveTouched", result.mutated()
                && (saveTouchedWhenMutated || Boolean.TRUE.equals(result.snapshot().get("saveTouched"))));
        snapshot.put("hudOrEventEmitted", Boolean.TRUE.equals(result.snapshot().get("hudOrEventEmitted"))
                || (result.mutated() && hudOrEventWhenMutated));
        snapshot.put("target", action.targetSnapshot());
        snapshot.put("realNativeStateMutated", result.mutated());
        return new NativeResult(result.mutated(), result.status(), result.message(), Map.copyOf(snapshot));
    }

    private static void recordMutationEvidence(RuntimeBinding binding, NativeResult result) {
        if (result == null || !result.mutated()) {
            return;
        }
        Map<String, Object> snapshot = result.snapshot();
        if (!Boolean.TRUE.equals(snapshot.get("adapterCoreActionDispatched"))
                || !Boolean.TRUE.equals(snapshot.get("runtimeHostResolved"))) {
            return;
        }
        Map<String, Object> nativeResult = Map.of(
                "mutated", result.mutated(),
                "status", result.status(),
                "message", result.message(),
                "snapshot", snapshot);
        AshfallNativeRuntimeMutationEvidence.record(
                "adaptercore_runtime_truth_bridge",
                binding.runtimeHostId,
                binding.lane,
                result.status(),
                true,
                1,
                List.of(nativeResult),
                Map.of(
                        "lane", binding.lane,
                        "runtimeHostId", binding.runtimeHostId,
                        "eventId", stringValue(snapshot, "eventId"),
                        "target", stringValue(snapshot, "target")));
    }

    private static List<String> sourceOperationIds(String actionId, Map<String, Object> payload) {
        List<String> explicit = stringList(payload, "adapterCoreSourceOperationIds");
        if (!explicit.isEmpty()) {
            return explicit;
        }
        String explicitOne = stringValue(payload, "adapterCoreSourceOperationId");
        if (!explicitOne.isBlank()) {
            return List.of(explicitOne);
        }
        String routeOperation = majorRouteOperationId(actionId, payload);
        if (!routeOperation.isBlank()) {
            return List.of(routeOperation, actionId);
        }
        return List.of(actionId);
    }

    private static List<String> hostCallAdapterIds(String actionId, Map<String, Object> payload) {
        List<String> explicit = stringList(payload, "adapterCoreHostCallAdapterIds");
        if (!explicit.isEmpty()) {
            return explicit;
        }
        String explicitOne = stringValue(payload, "adapterCoreHostCallAdapterId");
        if (!explicitOne.isBlank()) {
            return List.of(explicitOne);
        }
        String adapter = majorRouteHostCallAdapterId(actionId, payload);
        return adapter.isBlank() ? List.of() : List.of(adapter);
    }

    private static String majorRouteOperationId(String actionId, Map<String, Object> payload) {
        String target = routeTarget(payload);
        return switch (actionId) {
            case "ashfall.terminal_page" -> "echoashfallprotocol:ashfall_major_route_records".equals(target)
                    ? "mission.record_terminal_page_objective"
                    : "";
            case "ashfall.hazard_check" -> "echoashfallprotocol:relay_weather_window".equals(target)
                    ? "mission.record_relay_weather_window"
                    : "";
            case "holomap.marker_selected" -> "echoashfallprotocol:first_relay_station".equals(target)
                    ? "mission.track_first_relay_marker"
                    : "";
            case "player.scanner_used" -> "echoashfallprotocol:relay_station_console".equals(target)
                    ? "mission.scan_relay_console"
                    : "";
            case "powergrid.repair" -> "echoashfallprotocol:ashfall_relay_station_repair".equals(target)
                    ? "mission.repair_relay_power_coupler"
                    : "";
            case "player.terminal_opened" -> "echoashfallprotocol:relay_cache_lockbox".equals(target)
                    ? "mission.claim_relay_cache"
                    : "";
            case "terminal.route_record" -> "echoashfallprotocol:first_relay_station_route/returned".equals(target)
                    ? "mission.return_and_update_terminal"
                    : "";
            default -> "";
        };
    }

    private static String majorRouteHostCallAdapterId(String actionId, Map<String, Object> payload) {
        String target = routeTarget(payload);
        return switch (actionId) {
            case "ashfall.terminal_page" -> "echoashfallprotocol:ashfall_major_route_records".equals(target)
                    ? "native_terminal_page_event_bridge"
                    : "";
            case "ashfall.hazard_check" -> "echoashfallprotocol:relay_weather_window".equals(target)
                    ? "native_weather_hazard_check_bridge"
                    : "";
            case "holomap.marker_selected" -> "echoashfallprotocol:first_relay_station".equals(target)
                    ? "native_holomap_marker_bridge"
                    : "";
            case "player.scanner_used" -> "echoashfallprotocol:relay_station_console".equals(target)
                    ? "native_lens_scan_bridge"
                    : "";
            case "powergrid.repair" -> "echoashfallprotocol:ashfall_relay_station_repair".equals(target)
                    ? "native_powergrid_repair_bridge"
                    : "";
            case "player.terminal_opened" -> "echoashfallprotocol:relay_cache_lockbox".equals(target)
                    ? "native_loot_container_bridge"
                    : "";
            case "terminal.route_record" -> "echoashfallprotocol:first_relay_station_route/returned".equals(target)
                    ? "native_terminal_route_record_bridge"
                    : "";
            default -> "";
        };
    }

    private static String routeTarget(Map<String, Object> payload) {
        String target = stringValue(payload, "target");
        if (!target.isBlank()) {
            return target;
        }
        target = stringValue(payload, "terminalId");
        if (!target.isBlank()) {
            return target;
        }
        target = stringValue(payload, "cacheId");
        return target.isBlank() ? stringValue(payload, "marker") : target;
    }

    private static List<String> stringList(Map<String, Object> payload, String key) {
        Object value = payload == null ? null : payload.get(key);
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (Object item : iterable) {
            if (item != null) {
                String text = String.valueOf(item);
                if (!text.isBlank()) {
                    values.add(text);
                }
            }
        }
        return List.copyOf(values);
    }

    @FunctionalInterface
    interface PlayerEventApplier {
        NativeResult apply(ServerPlayer player, NativeEvent event, NativeMutationContext context);
    }

    static final class RuntimeBinding {
        private final String runtimeHostId;
        private final String lane;
        private final String lastEventKey;
        private final String lastEventTickKey;
        private final Set<String> actionIds;
        private final Set<String> canonicalContentIds;
        private final RuntimeHost host;
        private volatile boolean registered;

        private RuntimeBinding(
                String runtimeHostId,
                String lane,
                String lastEventKey,
                String lastEventTickKey,
                Set<String> actionIds,
                Set<String> canonicalContentIds,
                PlayerEventApplier applier) {
            this.runtimeHostId = runtimeHostId;
            this.lane = lane;
            this.lastEventKey = lastEventKey;
            this.lastEventTickKey = lastEventTickKey;
            this.actionIds = actionIds == null ? Set.of() : Set.copyOf(actionIds);
            this.canonicalContentIds = canonicalContentIds == null ? Set.of() : Set.copyOf(canonicalContentIds);
            this.host = new RuntimeHost(runtimeHostId, applier);
        }

        private void ensureRegistered() {
            if (registered) {
                return;
            }
            synchronized (this) {
                if (registered) {
                    return;
                }
                EchoRuntimeHostRegistry.global().register(host, new EchoRuntimeHostCapabilities(
                        runtimeHostId,
                        Set.of("EchoNativeRuntimeHost.Events"),
                        actionIds,
                        canonicalContentIds,
                        true,
                        true,
                        true));
                registered = true;
            }
        }

        private Map<String, Object> mutationSummary(
                ServerPlayer player,
                String eventId,
                Map<String, Object> payload,
                String phase) {
            CompoundTag playerData = player.getPersistentData();
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("phase", phase);
            summary.put("eventId", eventId);
            summary.put("target", stringValue(payload, "target"));
            summary.put("marker", stringValue(payload, "marker"));
            summary.put("lastEvent", playerData.getStringOr(lastEventKey, ""));
            summary.put("lastEventTick", playerData.getLongOr(lastEventTickKey, Long.MIN_VALUE));
            summary.put("gameTime", player.level().getGameTime());
            String playerDimensionId = player.level() instanceof ServerLevel playerLevel
                    ? playerLevel.dimension().identifier().toString()
                    : "unknown";
            summary.put("playerDimensionId", playerDimensionId);
            summary.put("playerBlockPos", positionMap(player.blockPosition()));
            summary.put("playerX", player.getX());
            summary.put("playerY", player.getY());
            summary.put("playerZ", player.getZ());
            SurvivalData survivalData = player.getData(ModAttachments.SURVIVAL_DATA.get());
            summary.put("survivalRadiation", survivalData.getRadiationLevel());
            summary.put("survivalHydration", survivalData.getHydration());
            summary.put("survivalAirFilterLife", survivalData.getAirFilterLife());
            summary.put("survivalFilterTier", survivalData.getFilterTier());
            MutationData mutationData = player.getData(ModAttachments.MUTATION_DATA.get());
            summary.put("mutationCount", mutationData.getMutationCount());
            summary.put("hasNausea", player.hasEffect(MobEffects.NAUSEA));
            summary.put("hasWeakness", player.hasEffect(MobEffects.WEAKNESS));
            summary.put("hasRegeneration", player.hasEffect(MobEffects.REGENERATION));
            EchoIntel echoIntel = player.getData(ModAttachments.ECHO_INTEL.get());
            summary.put("echoIntelCount", echoIntel.getAllIntel().size());
            summary.put("echoIntelUnread", echoIntel.getUnreadCount());
            QuestData questData = QuestData.get(player);
            summary.put("questArchiveCount", questData.getArchive().size());
            summary.put("discoveredPoiCount", questData.getDiscoveredPOICount());
            summary.put("cacheOpenedMarker", questData.hasVisitedLocation("special", "cache:opened"));
            summary.put("medBayUsedMarker", questData.hasVisitedLocation("special", "medical:field_med_bay_used"));
            summary.put("powerNodeActivatedMarker", questData.hasVisitedLocation("special", "power_node:activated"));
            summary.put("droneScoutModeMarker", questData.hasVisitedLocation("special", "drone:scout_mode"));
            summary.put("droneIntelRecoveredMarker", questData.hasVisitedLocation("special", "drone:intel_recovered"));
            summary.put("droneSalvageModeMarker", questData.hasVisitedLocation("special", "drone:salvage_mode"));
            summary.put("mainHandItemId", itemId(player.getMainHandItem()));
            summary.put("mainHandCount", player.getMainHandItem().getCount());
            summary.put("mainHandDamage", player.getMainHandItem().getDamageValue());
            summary.put("offHandItemId", itemId(player.getOffhandItem()));
            summary.put("offHandCount", player.getOffhandItem().getCount());
            summary.put("offHandDamage", player.getOffhandItem().getDamageValue());
            summary.put("headSlotItemId", itemId(player.getItemBySlot(EquipmentSlot.HEAD)));
            summary.put("headSlotCount", player.getItemBySlot(EquipmentSlot.HEAD).getCount());
            summary.put("filterCartridgeBasicCount", countInventory(player, ModItems.FILTER_CARTRIDGE_BASIC.get()));
            summary.put("filterCartridgeAdvancedCount", countInventory(player, ModItems.FILTER_CARTRIDGE_ADVANCED.get()));
            summary.put("filterCartridgeEliteCount", countInventory(player, ModItems.FILTER_CARTRIDGE_ELITE.get()));
            summary.put("powerCellCount", countInventory(player, ModItems.POWER_CELL.get()));
            summary.put("circuitBoardCount", countInventory(player, ModItems.CIRCUIT_BOARD.get()));
            summary.put("scrapCircuitCount", countInventory(player, ModItems.SCRAP_CIRCUIT.get()));
            summary.put("energyCellCount", countInventory(player, ModItems.ENERGY_CELL.get()));
            summary.put("radAwayCount", countInventory(player, ModItems.RAD_AWAY.get()));
            summary.put("mutagenVialCount", countInventory(player, ModItems.MUTAGEN_VIAL.get()));
            summary.put("instabilityDampenerCount", countInventory(player, ModItems.INSTABILITY_DAMPENER.get()));
            summary.put("returnBeaconCount", countInventory(player, ModItems.RETURN_BEACON.get()));
            summary.put("returnBeaconReadyTick", playerData.getLongOr(
                    AshfallAdapterCoreLateRuntime.RETURN_BEACON_COOLDOWN_KEY,
                    0L));
            RadioNetwork radioNetwork = RadioNetwork.get(player);
            summary.put("radioDiscoveredCount", radioNetwork.getDiscoveredStations().size());
            summary.put("radioActivatedCount", radioNetwork.getActivatedStations().size());
            summary.put("radioFastTravelCount", radioNetwork.getFastTravelCount());
            PostNexusData postNexusData = PostNexusData.get(player);
            summary.put("postNexusPath", postNexusData.getSelectedPath().name());
            summary.put("postNexusChoiceMade", postNexusData.hasMadeChoice());
            if (player.level() instanceof ServerLevel serverLevel) {
                summary.put("activePowerNodeCount", NexusWorldData.get(serverLevel).getActiveNodePositions().size());
                NexusCampaignData campaignData = NexusCampaignData.get(serverLevel.getServer().overworld());
                summary.put("nexusCampaignAwakened", campaignData.isAwakened());
                summary.put("nexusCampaignInstability", campaignData.getInstability());
                summary.put("nexusCampaignWarfrontComplete", campaignData.isWarfrontComplete());
                summary.put("nexusCampaignPos", positionMap(campaignData.getNexusPos()));
            }
            addTargetBlockSummary(player, payload, summary);
            ResearchData researchData = ResearchData.get(player);
            summary.put("researchPoints", researchData.getPoints());
            summary.put("unlockedSchematicCount", researchData.getUnlockedSchematics().size());
            summary.put("unlockedPerkCount", researchData.getUnlockedPerks().size());
            return Map.copyOf(summary);
        }
    }

    private static void addTargetBlockSummary(
            ServerPlayer player,
            Map<String, Object> payload,
            Map<String, Object> summary) {
        BlockPos targetBlockPos = blockPosValue(payload == null ? null : payload.get("targetBlockPos"));
        if (targetBlockPos == null) {
            return;
        }
        summary.put("targetBlockPos", positionMap(targetBlockPos));
        var targetState = player.level().getBlockState(targetBlockPos);
        summary.put("targetBlockId", BuiltInRegistries.BLOCK.getKey(
                targetState.getBlock()).toString());
        BlockEntity blockEntity = player.level().getBlockEntity(targetBlockPos);
        summary.put("targetBlockEntityId", blockEntity == null
                ? ""
                : BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()).toString());
        if (targetState.getBlock() instanceof RelayStationBlock) {
            summary.put("relayStationRepaired", targetState.getValue(RelayStationBlock.REPAIRED));
            summary.put("relayStationActive", targetState.getValue(RelayStationBlock.ACTIVE));
        }
        if (targetState.getBlock() instanceof PowerNodeBlock) {
            summary.put("powerNodeActive", targetState.getValue(PowerNodeBlock.ACTIVE));
        }
        if (blockEntity instanceof PowerNodeBlockEntity powerNode) {
            summary.put("powerNodeEntityActivated", powerNode.isActivated());
            summary.put("powerNodeEnergyStored", powerNode.getEnergyStored());
        }
        if (blockEntity instanceof SignalScannerBlockEntity scanner) {
            summary.put("signalScannerCooldownActive", scanner.isScanCooldownActive());
            summary.put("signalScannerCooldownTicks", scanner.getScanCooldownTicks());
            summary.put("signalScannerWear", new MachineWearData(player.level()).getWear(targetBlockPos));
        }
    }

    private static final class RuntimeHost extends EchoUnsupportedRuntimeHost {
        private final ThreadLocal<ServerPlayer> activePlayer = new ThreadLocal<>();
        private final PlayerEventApplier applier;

        private RuntimeHost(String runtimeHostId, PlayerEventApplier applier) {
            super(runtimeHostId);
            this.applier = applier;
        }

        private NativeResult publishForPlayer(ServerPlayer player, NativeEvent event, NativeMutationContext context) {
            activePlayer.set(player);
            try {
                return events().publish(event, context);
            } finally {
                activePlayer.remove();
            }
        }

        @Override
        public EchoNativeRuntimeHost.Events events() {
            return (event, context) -> {
                ServerPlayer player = activePlayer.get();
                if (player == null) {
                    return NativeResult.unsupported("Runtime lane requires a live server player target.", Map.of(
                            "runtimeHostId", runtimeHostId(),
                            "nativeInterface", "EchoNativeRuntimeHost.Events",
                            "nativeMethod", "publish",
                            "eventId", event == null ? "" : event.eventId(),
                            "failureReason", "missing live server player target"));
                }
                return applier.apply(player, event, context);
            };
        }
    }

    private record OptionalGuard(boolean guarded, NativeResult result) {
        private static OptionalGuard none() {
            return new OptionalGuard(false, null);
        }

        private static OptionalGuard of(NativeResult result) {
            return new OptionalGuard(true, result);
        }
    }

    private static String stringValue(Map<String, Object> payload, String key) {
        Object value = payload == null ? null : payload.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    @Nullable
    private static BlockPos blockPosValue(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return null;
        }
        return new BlockPos(
                intValue(map.get("x")),
                intValue(map.get("y")),
                intValue(map.get("z")));
    }

    private static int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static Map<String, Object> positionMap(BlockPos pos) {
        return Map.of(
                "x", pos.getX(),
                "y", pos.getY(),
                "z", pos.getZ());
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

    private static String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}
