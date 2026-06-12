package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echo.adaptercore.EchoCanonicalContentIds;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeEvent;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationContext;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativePlayerRef;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoFactionActionResult;
import com.echoplatform.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.block.entity.SignalScannerBlockEntity;
import com.knoxhack.echoashfallprotocol.echo.EchoIntel;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.entity.ScoutDrone;
import com.knoxhack.echoashfallprotocol.faction.FactionEvents;
import com.knoxhack.echoashfallprotocol.item.RareTechSchematicItem;
import com.knoxhack.echoashfallprotocol.item.SchematicFragmentItem;
import com.knoxhack.echoashfallprotocol.machine.MachineWearData;
import com.knoxhack.echoashfallprotocol.power.PowerNetwork;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import com.knoxhack.echoashfallprotocol.research.ResearchData;
import com.knoxhack.echoashfallprotocol.world.POIScannerService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class AshfallAdapterCoreExplorationRuntime {
    private static final String RUNTIME_HOST_ID = "echoashfallprotocol:exploration_runtime";
    private static final String LAST_EVENT_KEY = "ashes_of_tomorrow.adaptercore.last_exploration_event";
    private static final String LAST_EVENT_TICK_KEY = "ashes_of_tomorrow.adaptercore.last_exploration_event_tick";
    private static final int DRONE_REPAIR_ROUTE_THRESHOLD = 25;
    private static final AshfallAdapterCoreRuntimeTruthBridge.RuntimeBinding RUNTIME_BINDING =
            AshfallAdapterCoreRuntimeTruthBridge.binding(
                    RUNTIME_HOST_ID,
                    "exploration",
                    LAST_EVENT_KEY,
                    LAST_EVENT_TICK_KEY,
                    Set.of(
                            EchoCanonicalContentIds.EVENT_PLAYER_SCANNER_USED,
                            EchoCanonicalContentIds.EVENT_PLAYER_REGION_ENTERED,
                            EchoCanonicalContentIds.EVENT_PLAYER_TERMINAL_OPENED,
                            EchoCanonicalContentIds.EVENT_ASHFALL_DATA_LOG_RECOVERED,
                            EchoCanonicalContentIds.EVENT_ASHFALL_FACTION_ACTION,
                            EchoCanonicalContentIds.EVENT_ASHFALL_REPUTATION_UPDATED,
                            EchoCanonicalContentIds.EVENT_ASHFALL_DRONE_STATE,
                            EchoCanonicalContentIds.EVENT_ASHFALL_PERK_UNLOCKED,
                            EchoCanonicalContentIds.EVENT_ASHFALL_RESEARCH_UPDATED,
                            EchoCanonicalContentIds.EVENT_ASHFALL_SCHEMATIC_UNLOCKED),
                    Set.of(
                            "echoashfallprotocol:scan_first_poi",
                            "echoashfallprotocol:poi_explorer",
                            "echoashfallprotocol:loot_survivor_cache",
                            "echoashfallprotocol:recover_data_log",
                            "echoashfallprotocol:recover_drone_intel",
                            "echoashfallprotocol:repair_echo_drone",
                            EchoCanonicalContentIds.ITEM_PORTABLE_SIGNAL_SCANNER,
                            EchoCanonicalContentIds.BLOCK_SIGNAL_SCANNER,
                            EchoCanonicalContentIds.ITEM_RARE_TECH_SCHEMATIC,
                            "ashfall:schematic_unlocked",
                            "echoashfallprotocol:first_schematic",
                            "echoashfallprotocol:first_perk"),
                    AshfallAdapterCoreExplorationRuntime::apply);

    private AshfallAdapterCoreExplorationRuntime() {
    }

    public static NativeResult scannerUsed(
            ServerPlayer player,
            @Nullable POIScannerService.ScanHit hit,
            String source,
            boolean deepScan) {
        return scannerUsed(player, hit, source, deepScan, null, 0, false, 0);
    }

    public static NativeResult stationaryScannerUsed(ServerPlayer player, BlockPos scannerPos, String source) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("target", EchoCanonicalContentIds.BLOCK_SIGNAL_SCANNER);
        payload.put("source", safe(source));
        payload.put("blockId", EchoCanonicalContentIds.BLOCK_SIGNAL_SCANNER);
        payload.put("stationaryScanner", true);
        payload.put("targetBlockPos", positionSnapshot(scannerPos));
        payload.put("deepScan", false);
        payload.put("signalFound", false);
        payload.put("scannerDamageDelta", 0);
        payload.put("powerCost", SignalScannerBlockEntity.SCAN_POWER_COST);
        payload.put("cooldownTicks", SignalScannerBlockEntity.SCAN_COOLDOWN_TICKS);
        payload.put("wearDelta", SignalScannerBlockEntity.SCAN_WEAR_DELTA);
        payload.put("runtimeFeedback", true);
        payload.put("runtimePoiDiscovery", true);
        return publish(player, EchoCanonicalContentIds.EVENT_PLAYER_SCANNER_USED, payload, scannerPos, true);
    }

    public static NativeResult portableScannerUsed(
            ServerPlayer player,
            @Nullable POIScannerService.ScanHit hit,
            String source,
            boolean deepScan,
            InteractionHand hand,
            int scannerDamageDelta,
            boolean requestFieldOpsContract,
            int researchPointDelta) {
        return scannerUsed(
                player,
                hit,
                source,
                deepScan,
                hand,
                scannerDamageDelta,
                requestFieldOpsContract,
                researchPointDelta);
    }

    private static NativeResult scannerUsed(
            ServerPlayer player,
            @Nullable POIScannerService.ScanHit hit,
            String source,
            boolean deepScan,
            @Nullable InteractionHand hand,
            int scannerDamageDelta,
            boolean requestFieldOpsContract,
            int researchPointDelta) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("target", hit == null ? "ashfall:scanner/no_signal" : "echoashfallprotocol:scan_first_poi");
        payload.put("source", source);
        payload.put("itemId", EchoCanonicalContentIds.ITEM_PORTABLE_SIGNAL_SCANNER);
        payload.put("deepScan", deepScan);
        payload.put("signalFound", hit != null);
        payload.put("scannerDamageDelta", Math.max(0, scannerDamageDelta));
        payload.put("hand", hand == null ? "" : hand.name());
        payload.put("requestFieldOpsContract", requestFieldOpsContract);
        payload.put("researchPointDelta", Math.max(0, researchPointDelta));
        payload.put("runtimeFeedback", hand != null);
        payload.put("runtimePoiDiscovery", hand != null);
        if (hit != null) {
            payload.put("scanTarget", "poi/" + hit.id());
            payload.putAll(scanPayload(hit));
        }
        return publish(player, EchoCanonicalContentIds.EVENT_PLAYER_SCANNER_USED, payload);
    }

    public static NativeResult poiDiscovered(ServerPlayer player, POIScannerService.ScanHit hit, boolean newlyDiscovered) {
        if (hit == null) {
            return new NativeResult(false, "SKIPPED_MISSING_POI",
                    "AdapterCore POI discovery skipped for missing scan hit.", Map.of(
                    "realNativeStateMutated", false));
        }
        Map<String, Object> payload = new LinkedHashMap<>(scanPayload(hit));
        payload.put("target", "poi/" + hit.id());
        payload.put("source", "poi_discovery");
        payload.put("newlyDiscovered", newlyDiscovered);
        payload.put("discoverPoi", newlyDiscovered);
        return publish(player, EchoCanonicalContentIds.EVENT_PLAYER_REGION_ENTERED, payload);
    }

    public static NativeResult cacheOpened(ServerPlayer player, BlockPos pos, String source) {
        return publish(player, EchoCanonicalContentIds.EVENT_PLAYER_TERMINAL_OPENED, Map.of(
                "target", "echoashfallprotocol:loot_survivor_cache",
                "terminalId", "echoashfallprotocol:recovery_cache",
                "cacheId", "echoashfallprotocol:recovery_cache",
                "legacyTarget", "cache/opened",
                "source", source,
                "pos", positionSnapshot(pos)), pos, true);
    }

    public static NativeResult dataLogRecovered(ServerPlayer player, String logType, String title) {
        return dataLogRecovered(player, logType, title, "", "", null, "");
    }

    public static NativeResult dataLogRecovered(
            ServerPlayer player,
            String logType,
            String title,
            String content,
            String loreId) {
        return dataLogRecovered(player, logType, title, content, loreId, null, "");
    }

    public static NativeResult dataLogRecovered(
            ServerPlayer player,
            String logType,
            String title,
            String content,
            String loreId,
            @Nullable InteractionHand hand,
            String itemId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("target", "data_log/recovered");
        payload.put("source", "data_log_item");
        payload.put("logType", safe(logType));
        payload.put("title", safe(title));
        payload.put("content", safe(content));
        payload.put("loreId", safe(loreId));
        payload.put("hand", hand == null ? "" : hand.name());
        payload.put("itemId", safe(itemId));
        return publish(player, EchoCanonicalContentIds.EVENT_ASHFALL_DATA_LOG_RECOVERED, payload, null, false);
    }

    public static NativeResult factionAction(
            ServerPlayer player,
            Identifier factionId,
            Identifier actionId,
            String roleId,
            @Nullable Identifier targetId,
            EchoFactionActionResult result) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("target", "faction/action");
        payload.put("source", "faction_npc_action");
        payload.put("factionId", stringId(factionId));
        payload.put("actionId", stringId(actionId));
        payload.put("roleId", safe(roleId));
        payload.put("targetId", stringId(targetId));
        payload.put("refresh", result != null && result.refresh());
        payload.put("title", result == null ? "" : result.title());
        payload.put("message", result == null ? "" : result.message());
        return publish(player, "ashfall.faction_action", payload);
    }

    public static NativeResult reputationUpdated(ServerPlayer player, Identifier factionId, int delta, String source) {
        return publish(player, "ashfall.reputation_updated", Map.of(
                "target", "faction/reputation",
                "source", safe(source),
                "factionId", stringId(factionId),
                "delta", delta));
    }

    public static NativeResult droneState(
            ServerPlayer player,
            String operation,
            String mode,
            boolean success,
            Map<String, Object> extraPayload) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("target", "drone/" + sanitizeTarget(operation));
        payload.put("source", "drone_command");
        payload.put("operation", safe(operation));
        payload.put("mode", safe(mode));
        payload.put("success", success);
        if (extraPayload != null) {
            payload.putAll(extraPayload);
        }
        return publish(player, "ashfall.drone_state", payload);
    }

    public static NativeResult scoutDroneModeCycle(
            ServerPlayer player,
            @Nullable ScoutDrone drone,
            String source) {
        if (player == null) {
            return new NativeResult(false, "FAILED", "Scout Drone mode cycle skipped for missing player.", Map.of(
                    "nativeInterface", "EchoNativeRuntimeHost.Events",
                    "nativeMethod", "publish",
                    "failureReason", "missing player"));
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("target", "drone/scout_cycle_mode");
        payload.put("source", safe(source));
        payload.put("operation", "scout_cycle_mode");
        payload.put("mode", drone == null ? "" : drone.getMode().name());
        payload.put("success", true);
        payload.put("scoutDroneModeCycle", true);
        if (drone != null) {
            payload.put("targetDroneId", drone.getUUID().toString());
            payload.put("targetDronePos", positionSnapshot(drone.blockPosition()));
        }
        return publish(player, EchoCanonicalContentIds.EVENT_ASHFALL_DRONE_STATE, payload, null, false);
    }

    public static NativeResult perkUnlocked(ServerPlayer player, String perkId, int cost) {
        return publish(player, "ashfall.perk_unlocked", Map.of(
                "target", "perk/" + sanitizeTarget(perkId),
                "source", "research_purchase",
                "perkId", safe(perkId),
                "cost", cost));
    }

    public static NativeResult rareTechSchematicDecoded(ServerPlayer player, InteractionHand hand, String source) {
        SchematicFragmentItem.SchematicType nextType = firstMissingSchematicType(ResearchData.get(player));
        String category = nextType == null ? "" : categoryKey(nextType);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("target", nextType == null ? "research/rare_tech_schematic_archive" : "ashfall:schematic_unlocked");
        payload.put("source", safe(source));
        payload.put("itemId", EchoCanonicalContentIds.ITEM_RARE_TECH_SCHEMATIC);
        payload.put("hand", hand == null ? InteractionHand.MAIN_HAND.name() : hand.name());
        payload.put("schematicCategory", category);
        payload.put("duplicateArchive", nextType == null);
        payload.put("researchPointDelta", nextType == null
                ? RareTechSchematicItem.DUPLICATE_ARCHIVE_RP
                : RareTechSchematicItem.MISSING_CATEGORY_RP);
        return publish(
                player,
                nextType == null
                        ? EchoCanonicalContentIds.EVENT_ASHFALL_RESEARCH_UPDATED
                        : EchoCanonicalContentIds.EVENT_ASHFALL_SCHEMATIC_UNLOCKED,
                payload,
                null,
                false);
    }

    public static NativeResult rareTechSchematicDecoded(ServerPlayer player, int inventorySlot, String source) {
        SchematicFragmentItem.SchematicType nextType = firstMissingSchematicType(ResearchData.get(player));
        String category = nextType == null ? "" : categoryKey(nextType);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("target", nextType == null ? "research/rare_tech_schematic_archive" : "ashfall:schematic_unlocked");
        payload.put("source", safe(source));
        payload.put("itemId", EchoCanonicalContentIds.ITEM_RARE_TECH_SCHEMATIC);
        payload.put("inventorySlot", inventorySlot);
        payload.put("schematicCategory", category);
        payload.put("duplicateArchive", nextType == null);
        payload.put("researchPointDelta", nextType == null
                ? RareTechSchematicItem.DUPLICATE_ARCHIVE_RP
                : RareTechSchematicItem.MISSING_CATEGORY_RP);
        return publish(
                player,
                nextType == null
                        ? EchoCanonicalContentIds.EVENT_ASHFALL_RESEARCH_UPDATED
                        : EchoCanonicalContentIds.EVENT_ASHFALL_SCHEMATIC_UNLOCKED,
                payload,
                null,
                false);
    }

    public static NativeResult schematicFragmentAnalyzed(
            ServerPlayer player,
            SchematicFragmentItem.SchematicType type,
            String source) {
        if (player == null || type == null) {
            return new NativeResult(false, "SKIPPED", "Schematic fragment analysis skipped for missing player or type.", Map.of(
                    "nativeInterface", "EchoNativeRuntimeHost.Events",
                    "nativeMethod", "publish"));
        }
        String category = categoryKey(type);
        boolean duplicate = ResearchData.get(player).hasSchematic(category);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("target", duplicate ? "research/schematic_fragment_archive" : "ashfall:schematic_unlocked");
        payload.put("source", safe(source));
        payload.put("itemId", "echoashfallprotocol:schematic_fragment_" + category);
        payload.put("schematicType", type.name());
        payload.put("schematicCategory", category);
        payload.put("duplicateArchive", duplicate);
        payload.put("researchPointDelta", duplicate ? 5 : 25);
        return publish(
                player,
                duplicate
                        ? EchoCanonicalContentIds.EVENT_ASHFALL_RESEARCH_UPDATED
                        : EchoCanonicalContentIds.EVENT_ASHFALL_SCHEMATIC_UNLOCKED,
                payload,
                null,
                false);
    }

    public static NativeResult analyzeFirstSchematicAtResearchLab(ServerPlayer player, String source) {
        if (player == null) {
            return new NativeResult(false, "SKIPPED", "Research analysis skipped for missing player.", Map.of(
                    "nativeInterface", "EchoNativeRuntimeHost.Events",
                    "nativeMethod", "publish"));
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ModItems.RARE_TECH_SCHEMATIC.get())) {
                return rareTechSchematicDecoded(player, slot, source);
            }
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof SchematicFragmentItem fragment) {
                return schematicFragmentAnalyzed(player, fragment.getType(), source);
            }
        }
        player.sendSystemMessage(Component.literal("[ECHO-7] No schematic fragment found.")
                .withStyle(ChatFormatting.YELLOW), true);
        return new NativeResult(false, "NOOP", "No schematic was available to analyze.", Map.of(
                "nativeInterface", "EchoNativeRuntimeHost.Events",
                "nativeMethod", "publish",
                "hudOrEventEmitted", true,
                "schematicFound", false));
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
        Map<String, Object> payload = event.payload();
        String target = stringValue(payload, "target");

        boolean changed = false;
        Map<String, Object> resultSnapshot = new LinkedHashMap<>();

        switch (event.eventId()) {
            case "ashfall.scanner_used", "player.scanner_used" -> {
                if (requiresHeldPortableScanner(payload) && !hasHeldPortableScanner(player, payload)) {
                    resultSnapshot.put("failureReason", "held item was not a portable signal scanner");
                    resultSnapshot.put("scannerDamaged", false);
                    resultSnapshot.put("fieldOpsContractRequested", false);
                    resultSnapshot.put("researchPointsAdded", 0);
                    resultSnapshot.put("firstPoiScan", false);
                    resultSnapshot.put("autoDiscovered", false);
                    resultSnapshot.put("deepScanRecorded", false);
                    resultSnapshot.put("poiDiscoveryChanged", false);
                    resultSnapshot.put("hudOrEventEmitted", true);
                    player.sendSystemMessage(Component.literal("[ECHO-7] Portable Signal Scanner required.")
                            .withStyle(ChatFormatting.YELLOW));
                    break;
                }
                boolean scannerDamaged = applyHeldItemDamage(player, payload);
                boolean fieldOpsContractRequested = false;
                int researchPointsAdded = 0;
                POIScannerService.ScanHit hit = scanHitFromPayload(payload);
                Map<String, Object> activePayload = payload;
                if (booleanValue(payload, "stationaryScanner")) {
                    StationaryScannerRuntimeResult stationaryScan =
                            applyStationaryScannerUse(player, payload, resultSnapshot);
                    changed |= stationaryScan.changed();
                    hit = stationaryScan.hit();
                    if (hit != null) {
                        activePayload = scannerPayloadWithHit(payload, hit);
                    }
                    if (!stationaryScan.scanExecuted()) {
                        resultSnapshot.put("scannerDamaged", false);
                        resultSnapshot.put("fieldOpsContractRequested", false);
                        resultSnapshot.put("researchPointsAdded", 0);
                        resultSnapshot.put("firstPoiScan", false);
                        resultSnapshot.put("autoDiscovered", false);
                        resultSnapshot.put("deepScanRecorded", false);
                        resultSnapshot.put("poiDiscoveryChanged", false);
                        break;
                    }
                }
                boolean deepScan = booleanValue(activePayload, "deepScan");
                boolean firstPoiScan = false;
                if (hit != null) {
                    QuestData quest = QuestData.get(player);
                    firstPoiScan = !quest.hasPOIState(hit.id(), QuestData.POIObjectiveState.SCANNED);
                }
                if (booleanValue(activePayload, "requestFieldOpsContract")) {
                    FieldOpsContractHandler.requestContract(player, hit);
                    fieldOpsContractRequested = true;
                }
                int researchPointDelta = numberValue(activePayload, "researchPointDelta", 0);
                if (researchPointDelta > 0 && hit != null) {
                    if (firstPoiScan) {
                        ResearchData research = ResearchData.get(player);
                        researchPointsAdded = research.addPoints(researchPointDelta);
                        if (researchPointsAdded > 0) {
                            ResearchData.saveAndSync(player, research);
                        }
                    }
                }
                changed |= scannerDamaged;
                changed |= fieldOpsContractRequested;
                changed |= researchPointsAdded > 0;
                changed |= recordMission(player, MissionObjectiveType.CUSTOM, "scanner/used", 1, activePayload);
                if (hit != null) {
                    changed |= recordMission(player, MissionObjectiveType.SCAN_BLOCK, "echoashfallprotocol:scan_first_poi", 1, activePayload);
                    String siteId = stringValue(activePayload, "siteId");
                    if (!siteId.isBlank()) {
                        changed |= recordMission(player, MissionObjectiveType.SCAN_BLOCK, "echoashfallprotocol:" + sanitizeTarget(siteId), 1, activePayload);
                    }
                }
                boolean runtimePoiDiscovery = booleanValue(activePayload, "runtimePoiDiscovery");
                boolean autoDiscovered = runtimePoiDiscovery
                        && hit != null
                        && !deepScan
                        && POIScannerService.shouldAutoDiscover(hit)
                        && !hit.discovered();
                boolean deepScanRecorded = runtimePoiDiscovery && hit != null && deepScan && firstPoiScan;
                boolean poiDiscoveryChanged = false;
                if (autoDiscovered || deepScanRecorded) {
                    poiDiscoveryChanged = applyPoiDiscovery(
                            player,
                            activePayload,
                            hit,
                            !hit.discovered(),
                            !hit.discovered(),
                            resultSnapshot);
                    changed |= poiDiscoveryChanged;
                }
                boolean feedbackEmitted = emitScannerFeedback(
                        player,
                        hit,
                        deepScan,
                        firstPoiScan,
                        autoDiscovered && poiDiscoveryChanged,
                        activePayload);
                resultSnapshot.put("signalFound", hit != null);
                resultSnapshot.put("scannerDamaged", scannerDamaged);
                resultSnapshot.put("fieldOpsContractRequested", fieldOpsContractRequested);
                resultSnapshot.put("researchPointsAdded", researchPointsAdded);
                resultSnapshot.put("firstPoiScan", firstPoiScan);
                resultSnapshot.put("autoDiscovered", autoDiscovered);
                resultSnapshot.put("deepScanRecorded", deepScanRecorded);
                resultSnapshot.put("poiDiscoveryChanged", poiDiscoveryChanged);
                resultSnapshot.put("hudOrEventEmitted", feedbackEmitted || fieldOpsContractRequested);
            }
            case "ashfall.poi_discovered", "player.region_entered" -> {
                POIScannerService.ScanHit hit = scanHitFromPayload(payload);
                changed |= applyPoiDiscovery(
                        player,
                        payload,
                        hit,
                        booleanValue(payload, "discoverPoi"),
                        booleanValue(payload, "newlyDiscovered"),
                        resultSnapshot);
            }
            case "ashfall.cache_opened", "player.terminal_opened" -> {
                changed |= markSpecial(player, "cache:opened");
                boolean cachePoiStateRecorded = recordNearestPoiCacheLooted(player);
                changed |= cachePoiStateRecorded;
                changed |= recordMission(player, MissionObjectiveType.CUSTOM, "cache/opened", 1, payload);
                changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:loot_survivor_cache", 1, payload);
                resultSnapshot.put("cachePoiStateRecorded", cachePoiStateRecorded);
            }
            case "ashfall.data_log_recovered" -> {
                boolean dataLogRecovered = applyDataLogRecovery(player, payload, resultSnapshot);
                if (dataLogRecovered) {
                    changed = true;
                    changed |= markSpecial(player, "data_log:archived");
                    changed |= markSpecial(player, "data_log:" + sanitizeTarget(stringValue(payload, "logType")));
                    changed |= recordNearestPoiState(player, QuestData.POIObjectiveState.SCANNED);
                    changed |= recordNearestPoiState(player, QuestData.POIObjectiveState.DATA_RECOVERED);
                    changed |= recordMission(player, MissionObjectiveType.CUSTOM, "data_log/recovered", 1, payload);
                    changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:recover_data_log", 1, payload);
                }
            }
            case "ashfall.faction_action" -> {
                changed |= markSpecial(player, "faction_contact:" + sanitizeTarget(stringValue(payload, "factionId")));
                changed |= recordMission(player, MissionObjectiveType.CUSTOM, "faction/action", 1, payload);
                changed |= recordMission(player, MissionObjectiveType.CUSTOM, "faction_contact:any", 1, payload);
            }
            case "ashfall.reputation_updated" -> {
                int delta = numberValue(payload, "delta", 1);
                changed |= recordMission(player, MissionObjectiveType.CUSTOM, "faction/reputation", Math.max(1, Math.abs(delta)), payload);
                if (delta > 0) {
                    changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:faction_reputation", 1, payload);
                    String source = stringValue(payload, "source");
                    if (source.contains("contract")) {
                        changed |= recordMission(player, MissionObjectiveType.CUSTOM, "faction:first_task_complete", 1, payload);
                        changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:complete_first_faction_task", 1, payload);
                    }
                }
            }
            case "ashfall.drone_state" -> {
                boolean droneChanged = applyDroneState(player, payload, resultSnapshot);
                changed |= droneChanged;
                if (droneChanged) {
                    changed |= recordMission(player, MissionObjectiveType.CUSTOM, "drone/state", 1, payload);
                }
            }
            case "ashfall.perk_unlocked" -> {
                changed |= markSpecial(player, "perk:" + sanitizeTarget(stringValue(payload, "perkId")));
                changed |= recordMission(player, MissionObjectiveType.UNLOCK_RESEARCH, target, 1, payload);
                changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:first_perk", 1, payload);
            }
            case "ashfall.research_updated", "ashfall.schematic_unlocked" -> {
                if (EchoCanonicalContentIds.ITEM_RARE_TECH_SCHEMATIC.equals(stringValue(payload, "itemId"))) {
                    changed |= applyRareTechSchematicDecode(player, payload, resultSnapshot);
                } else {
                    changed |= applySchematicFragmentAnalysis(player, payload, resultSnapshot);
                }
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
                    case "MUTATED" -> "Published AdapterCore exploration runtime event and mutated state.";
                    case "FAILED" -> "AdapterCore exploration runtime event attempted a mutation and failed.";
                    default -> "AdapterCore exploration runtime event was valid but no state change was needed.";
                },
                resultSnapshot);
    }

    private static StationaryScannerRuntimeResult applyStationaryScannerUse(
            ServerPlayer player,
            Map<String, Object> payload,
            Map<String, Object> resultSnapshot) {
        BlockPos scannerPos = blockPosValue(payload.get("targetBlockPos"));
        BlockEntity blockEntity = player.level().getBlockEntity(scannerPos);
        resultSnapshot.put("stationaryScannerRuntime", true);
        resultSnapshot.put("stationaryScannerPos", positionSnapshot(scannerPos));

        if (!(blockEntity instanceof SignalScannerBlockEntity scanner)) {
            resultSnapshot.put("failureReason", "target block entity is not a signal scanner");
            resultSnapshot.put("hudOrEventEmitted", true);
            player.sendSystemMessage(Component.literal("[ECHO-7] Signal scanner link unavailable.")
                    .withStyle(ChatFormatting.RED));
            return new StationaryScannerRuntimeResult(false, false, null);
        }

        if (scanner.isScanCooldownActive()) {
            resultSnapshot.put("noopReason", "scanner cooldown active");
            resultSnapshot.put("scannerCooldownTicks", scanner.getScanCooldownTicks());
            resultSnapshot.put("hudOrEventEmitted", true);
            player.sendSystemMessage(Component.literal("Scanner cooling down..."));
            return new StationaryScannerRuntimeResult(false, false, null);
        }

        Level level = scanner.getLevel();
        if (level == null) {
            resultSnapshot.put("failureReason", "scanner has no level");
            resultSnapshot.put("hudOrEventEmitted", true);
            player.sendSystemMessage(Component.literal("[ECHO-7] Signal scanner is offline.")
                    .withStyle(ChatFormatting.RED));
            return new StationaryScannerRuntimeResult(false, false, null);
        }

        if (!PowerNetwork.hasPowerAccess(level, scannerPos)) {
            resultSnapshot.put("noopReason", "no power access");
            resultSnapshot.put("powerAvailable", false);
            resultSnapshot.put("hudOrEventEmitted", true);
            player.sendSystemMessage(Component.literal("No power available!"));
            return new StationaryScannerRuntimeResult(false, false, null);
        }

        int powerCost = numberValue(payload, "powerCost", SignalScannerBlockEntity.SCAN_POWER_COST);
        if (!PowerNetwork.tryConsumePower(level, scannerPos, powerCost)) {
            resultSnapshot.put("noopReason", "insufficient power");
            resultSnapshot.put("powerAvailable", true);
            resultSnapshot.put("powerConsumed", false);
            resultSnapshot.put("hudOrEventEmitted", true);
            player.sendSystemMessage(Component.literal("Insufficient power!"));
            return new StationaryScannerRuntimeResult(false, false, null);
        }

        int cooldownBefore = scanner.getScanCooldownTicks();
        int cooldownTicks = numberValue(payload, "cooldownTicks", SignalScannerBlockEntity.SCAN_COOLDOWN_TICKS);
        scanner.startScanCooldown(cooldownTicks);

        MachineWearData wearData = new MachineWearData(level);
        int wearBefore = wearData.getWear(scannerPos);
        int wearDelta = numberValue(payload, "wearDelta", SignalScannerBlockEntity.SCAN_WEAR_DELTA);
        wearData.addWear(scannerPos, wearDelta, level.getRandom());
        int wearAfter = wearData.getWear(scannerPos);

        POIScannerService.ScanHit hit = POIScannerService.scan(player);
        resultSnapshot.put("powerAvailable", true);
        resultSnapshot.put("powerConsumed", true);
        resultSnapshot.put("powerCost", powerCost);
        resultSnapshot.put("cooldownBefore", cooldownBefore);
        resultSnapshot.put("cooldownAfter", scanner.getScanCooldownTicks());
        resultSnapshot.put("wearBefore", wearBefore);
        resultSnapshot.put("wearAfter", wearAfter);
        resultSnapshot.put("wearDelta", wearAfter - wearBefore);
        resultSnapshot.put("stationaryScannerSignalFound", hit != null);
        if (hit != null) {
            resultSnapshot.put("stationaryScannerSiteId", hit.id());
        }
        return new StationaryScannerRuntimeResult(true, true, hit);
    }

    private static Map<String, Object> scannerPayloadWithHit(
            Map<String, Object> payload,
            POIScannerService.ScanHit hit) {
        Map<String, Object> activePayload = new LinkedHashMap<>(payload);
        activePayload.put("signalFound", true);
        activePayload.put("scanTarget", "poi/" + hit.id());
        activePayload.putAll(scanPayload(hit));
        return activePayload;
    }

    private static boolean applyPoiDiscovery(
            ServerPlayer player,
            Map<String, Object> payload,
            @Nullable POIScannerService.ScanHit hit,
            boolean discoverPoi,
            boolean newlyDiscovered,
            Map<String, Object> resultSnapshot) {
        String siteId = stringValue(payload, "siteId");
        boolean poiDiscovered = false;
        boolean changed = false;
        if (discoverPoi && hit != null) {
            poiDiscovered = POIScannerService.discover(player, hit);
            changed |= poiDiscovered;
        }

        changed |= recordMission(player, MissionObjectiveType.DISCOVER_STRUCTURE,
                "echoashfallprotocol:" + sanitizeTarget(siteId), 1, payload);
        if (newlyDiscovered) {
            if (!siteId.isBlank()) {
                FactionEvents.onPOIDiscovered(player, siteId);
                changed = true;
            }
            if (hit != null) {
                FieldOpsContractHandler.onPoiDiscovered(player, hit);
                changed = true;
            }
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:poi_explorer", 1, payload);
        }

        if (hit != null) {
            NativeResult hazardRoute = AshfallAdapterCoreHazardRuntime.hazardRouteObjective(
                    player,
                    hit.id(),
                    hit.route(),
                    hit.hazardProfile(),
                    "poi_discovered");
            changed |= hazardRoute.mutated();
            resultSnapshot.put("poiHazardRouteStatus", hazardRoute.status());
            resultSnapshot.put("poiHazardRouteMutated", hazardRoute.mutated());
        }

        resultSnapshot.put("poiDiscovered", poiDiscovered);
        resultSnapshot.put("poiDiscoveryRequested", discoverPoi);
        resultSnapshot.put("poiNewlyDiscovered", newlyDiscovered);
        return changed;
    }

    private record StationaryScannerRuntimeResult(
            boolean scanExecuted,
            boolean changed,
            @Nullable POIScannerService.ScanHit hit) {
    }

    private static boolean emitScannerFeedback(
            ServerPlayer player,
            @Nullable POIScannerService.ScanHit hit,
            boolean deepScan,
            boolean firstPoiScan,
            boolean siteArchived,
            Map<String, Object> payload) {
        if (!booleanValue(payload, "runtimeFeedback")) {
            return false;
        }
        if (hit == null) {
            player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.scanner.no_signal")
                    .withStyle(ChatFormatting.YELLOW));
            return true;
        }

        if (deepScan) {
            player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.scanner.deep_scan_recorded")
                    .withStyle(ChatFormatting.AQUA));
        }
        for (Component line : POIScannerService.createReadout(hit)) {
            player.sendSystemMessage(line);
        }
        if (siteArchived) {
            player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.scanner.site_archived")
                    .withStyle(ChatFormatting.GREEN));
        } else if (deepScan && !firstPoiScan) {
            player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.scanner.already_logged")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        return true;
    }

    private static boolean recordNearestPoiState(ServerPlayer player, QuestData.POIObjectiveState state) {
        POIScannerService.ScanHit hit = POIScannerService.scan(player);
        if (hit == null || hit.distance() > POIScannerService.DISCOVERY_RADIUS * 1.5D) {
            return false;
        }
        QuestData quest = QuestData.get(player);
        boolean changed = !quest.hasPOIState(hit.id(), state);
        quest.recordPOIState(hit.id(), state);
        quest.visitLocation("poi", hit.id());
        if (changed) {
            QuestData.saveAndSync(player, quest);
        }
        return changed;
    }

    private static boolean recordNearestPoiCacheLooted(ServerPlayer player) {
        POIScannerService.ScanHit hit = POIScannerService.scan(player);
        if (hit == null || hit.distance() > POIScannerService.DISCOVERY_RADIUS * 1.5D) {
            return false;
        }

        QuestData quest = QuestData.get(player);
        if (quest.hasPOIState(hit.id(), QuestData.POIObjectiveState.CACHE_LOOTED)) {
            return false;
        }

        quest.recordPOIState(hit.id(), QuestData.POIObjectiveState.SCANNED);
        if (hit.distance() <= POIScannerService.DISCOVERY_RADIUS) {
            quest.recordPOIState(hit.id(), QuestData.POIObjectiveState.ENTERED);
            quest.visitLocation("poi", hit.id());
        }
        quest.recordPOIState(hit.id(), QuestData.POIObjectiveState.CACHE_LOOTED);
        QuestData.saveAndSync(player, quest);
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "\u00A76[ECHO-7]\u00A7r Cache state updated: " + hit.displayName()), true);
        return true;
    }

    private static boolean applyHeldItemDamage(ServerPlayer player, Map<String, Object> payload) {
        int damageDelta = numberValue(payload, "scannerDamageDelta", 0);
        if (damageDelta <= 0) {
            return false;
        }
        InteractionHand hand = interactionHandValue(payload);
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || !stack.is(ModItems.PORTABLE_SIGNAL_SCANNER.get())) {
            return false;
        }
        int beforeDamage = stack.getDamageValue();
        int beforeCount = stack.getCount();
        stack.hurtAndBreak(damageDelta, player, hand);
        ItemStack afterStack = player.getItemInHand(hand);
        return afterStack.getDamageValue() != beforeDamage
                || afterStack.getCount() != beforeCount
                || afterStack.isEmpty();
    }

    private static boolean requiresHeldPortableScanner(Map<String, Object> payload) {
        return numberValue(payload, "scannerDamageDelta", 0) > 0
                && !stringValue(payload, "hand").isBlank();
    }

    private static boolean hasHeldPortableScanner(ServerPlayer player, Map<String, Object> payload) {
        InteractionHand hand = interactionHandValue(payload);
        ItemStack stack = player.getItemInHand(hand);
        return !stack.isEmpty() && stack.is(ModItems.PORTABLE_SIGNAL_SCANNER.get());
    }

    private static boolean applyRareTechSchematicDecode(
            ServerPlayer player,
            Map<String, Object> payload,
            Map<String, Object> resultSnapshot) {
        InteractionHand hand = interactionHandValue(payload);
        ItemStack stack = schematicStackFromPayload(player, payload, hand);
        if (stack.isEmpty() || !stack.is(ModItems.RARE_TECH_SCHEMATIC.get())) {
            resultSnapshot.put("decodeApplied", false);
            resultSnapshot.put("failureReason", "held item was not a rare tech schematic");
            return false;
        }

        int beforeStackCount = stack.getCount();
        ResearchData research = ResearchData.get(player);
        SchematicFragmentItem.SchematicType unlockedType = firstMissingSchematicType(research);
        String category = unlockedType == null ? "" : categoryKey(unlockedType);
        boolean schematicUnlocked = unlockedType != null && research.unlockSchematic(category);
        int requestedPoints = unlockedType == null
                ? RareTechSchematicItem.DUPLICATE_ARCHIVE_RP
                : RareTechSchematicItem.MISSING_CATEGORY_RP;
        int researchPointsAdded = research.addPoints(requestedPoints);

        boolean stackShrunk = false;
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
            stackShrunk = stack.getCount() != beforeStackCount;
        }

        boolean changed = schematicUnlocked || researchPointsAdded > 0 || stackShrunk;
        if (changed) {
            ResearchData.saveAndSync(player, research);
            if (unlockedType == null) {
                player.sendSystemMessage(Component.literal("[ECHO-7] Rare schematic archived. +"
                        + researchPointsAdded + " RP").withStyle(ChatFormatting.LIGHT_PURPLE));
            } else {
                player.sendSystemMessage(Component.literal("[ECHO-7] Rare schematic decoded: "
                        + unlockedType.getDisplayName() + ". +" + researchPointsAdded + " RP")
                        .withStyle(ChatFormatting.GREEN));
            }
        }

        if (schematicUnlocked) {
            changed |= markSpecial(player, "research:schematic_unlocked");
            changed |= recordMission(player, MissionObjectiveType.UNLOCK_RESEARCH, "ashfall:schematic_unlocked", 1, payload);
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:first_schematic", 1, payload);
        }
        changed |= markSpecial(player, unlockedType == null
                ? "lab:schematic_archived"
                : "lab:schematic_decoded");
        changed |= recordMission(player, MissionObjectiveType.UNLOCK_RESEARCH,
                unlockedType == null ? "lab/schematic_archived" : "lab/schematic_decoded", 1, payload);

        resultSnapshot.put("decodeApplied", changed);
        resultSnapshot.put("schematicConsumed", stackShrunk);
        resultSnapshot.put("stackCountBefore", beforeStackCount);
        resultSnapshot.put("stackCountAfter", stack.getCount());
        resultSnapshot.put("unlockedType", unlockedType == null ? "" : unlockedType.name());
        resultSnapshot.put("schematicCategory", category);
        resultSnapshot.put("researchPointsAdded", researchPointsAdded);
        resultSnapshot.put("requestedResearchPoints", requestedPoints);
        resultSnapshot.put("duplicateArchive", unlockedType == null);
        resultSnapshot.put("hand", hand.name());
        return changed;
    }

    private static boolean applySchematicFragmentAnalysis(
            ServerPlayer player,
            Map<String, Object> payload,
            Map<String, Object> resultSnapshot) {
        SchematicFragmentItem.SchematicType type = schematicTypeValue(payload);
        if (type == null) {
            resultSnapshot.put("analysisApplied", false);
            resultSnapshot.put("failureReason", "schematic fragment type was missing");
            return false;
        }

        ItemStack stack = firstSchematicFragmentStack(player, type);
        if (stack.isEmpty()) {
            resultSnapshot.put("analysisApplied", false);
            resultSnapshot.put("failureReason", "matching schematic fragment was not in inventory");
            resultSnapshot.put("hudOrEventEmitted", true);
            player.sendSystemMessage(Component.literal("[ECHO-7] No matching schematic fragment found.")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }

        int beforeStackCount = stack.getCount();
        ResearchData research = ResearchData.get(player);
        String category = categoryKey(type);
        boolean newlyUnlocked = research.unlockSchematic(category);
        int requestedPoints = newlyUnlocked ? 25 : 5;
        int researchPointsAdded = research.addPoints(requestedPoints);
        stack.shrink(1);
        boolean stackShrunk = stack.getCount() != beforeStackCount;

        boolean changed = newlyUnlocked || researchPointsAdded > 0 || stackShrunk;
        if (changed) {
            ResearchData.saveAndSync(player, research);
        } else {
            ResearchData.syncToClient(player);
        }

        String status = newlyUnlocked
                ? "Schematic decoded: " + type.getDisplayName()
                : "Duplicate schematic archived";
        player.sendSystemMessage(Component.literal("[ECHO-7] " + status + ". +" + researchPointsAdded + " RP")
                .withStyle(newlyUnlocked ? ChatFormatting.GREEN : ChatFormatting.YELLOW), false);

        if (newlyUnlocked) {
            changed |= markSpecial(player, "research:schematic_unlocked");
            changed |= recordMission(player, MissionObjectiveType.UNLOCK_RESEARCH, "ashfall:schematic_unlocked", 1, payload);
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:first_schematic", 1, payload);
        }
        changed |= markSpecial(player, newlyUnlocked ? "lab:schematic_decoded" : "lab:schematic_archived");
        changed |= recordMission(player, MissionObjectiveType.UNLOCK_RESEARCH,
                newlyUnlocked ? "lab/schematic_decoded" : "lab/schematic_archived", 1, payload);

        resultSnapshot.put("analysisApplied", changed);
        resultSnapshot.put("schematicConsumed", stackShrunk);
        resultSnapshot.put("stackCountBefore", beforeStackCount);
        resultSnapshot.put("stackCountAfter", stack.getCount());
        resultSnapshot.put("unlockedType", newlyUnlocked ? type.name() : "");
        resultSnapshot.put("schematicCategory", category);
        resultSnapshot.put("researchPointsAdded", researchPointsAdded);
        resultSnapshot.put("requestedResearchPoints", requestedPoints);
        resultSnapshot.put("duplicateArchive", !newlyUnlocked);
        resultSnapshot.put("hudOrEventEmitted", true);
        return changed;
    }

    private static ItemStack schematicStackFromPayload(
            ServerPlayer player,
            Map<String, Object> payload,
            InteractionHand hand) {
        int inventorySlot = numberValue(payload, "inventorySlot", -1);
        if (inventorySlot >= 0 && inventorySlot < player.getInventory().getContainerSize()) {
            return player.getInventory().getItem(inventorySlot);
        }
        return player.getItemInHand(hand);
    }

    private static ItemStack firstSchematicFragmentStack(ServerPlayer player, SchematicFragmentItem.SchematicType type) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof SchematicFragmentItem fragment && fragment.getType() == type) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    @Nullable
    private static SchematicFragmentItem.SchematicType schematicTypeValue(Map<String, Object> payload) {
        String value = stringValue(payload, "schematicType");
        if (value.isBlank()) {
            String category = stringValue(payload, "schematicCategory");
            for (SchematicFragmentItem.SchematicType type : SchematicFragmentItem.SchematicType.values()) {
                if (categoryKey(type).equals(category)) {
                    return type;
                }
            }
            return null;
        }
        for (SchematicFragmentItem.SchematicType type : SchematicFragmentItem.SchematicType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }

    private static boolean applyDataLogRecovery(
            ServerPlayer player,
            Map<String, Object> payload,
            Map<String, Object> resultSnapshot) {
        String loreId = dataLogLoreId(payload);
        InteractionHand hand = interactionHandValue(payload);
        ItemStack stack = player.getItemInHand(hand);
        int stackCountBefore = stack.getCount();
        String heldItemId = itemId(stack);
        boolean archived = archiveDataLog(player, payload, loreId);
        boolean itemConsumed = false;
        if (archived && !player.getAbilities().instabuild && !stack.isEmpty()) {
            String expectedItemId = stringValue(payload, "itemId");
            if (expectedItemId.isBlank() || expectedItemId.equals(heldItemId)) {
                stack.shrink(1);
                itemConsumed = stack.getCount() != stackCountBefore;
            }
        }

        if (archived) {
            player.sendSystemMessage(Component.literal("\u00A7b[ECHO-7]\u00A7r Data log archived. "
                    + stringValue(payload, "title") + " added to the Field Archive."));
        } else {
            player.sendSystemMessage(Component.literal("\u00A7e[ECHO-7]\u00A7r Data log already archived."));
        }

        resultSnapshot.put("dataLogArchived", archived);
        resultSnapshot.put("dataLogItemConsumed", itemConsumed);
        resultSnapshot.put("dataLogLoreId", loreId);
        resultSnapshot.put("hand", hand.name());
        resultSnapshot.put("itemId", stringValue(payload, "itemId"));
        resultSnapshot.put("heldItemId", heldItemId);
        resultSnapshot.put("stackCountBefore", stackCountBefore);
        resultSnapshot.put("stackCountAfter", stack.getCount());
        resultSnapshot.put("hudOrEventEmitted", true);
        if (!archived) {
            resultSnapshot.put("noopReason", "data log already archived");
        }
        return archived;
    }

    private static boolean archiveDataLog(ServerPlayer player, Map<String, Object> payload, String loreId) {
        EchoIntel echoIntel = EchoIntel.get(player);
        if (echoIntel.hasDiscoveredLore(loreId)) {
            return false;
        }
        echoIntel.discoverLore(
                loreId,
                "[DATA LOG] " + stringValue(payload, "title"),
                stringValue(payload, "content"));
        EchoIntel.saveAndSync(player, echoIntel);
        return true;
    }

    private static String dataLogLoreId(Map<String, Object> payload) {
        String loreId = stringValue(payload, "loreId");
        if (!loreId.isBlank()) {
            return loreId;
        }
        return "datalog_" + sanitizeTarget(stringValue(payload, "logType")) + "_"
                + sanitizeTarget(stringValue(payload, "title"));
    }

    private static boolean isDroneSupportOperation(String operation, int repairLevel) {
        return "scout_deployed".equals(operation)
                || ("repair".equals(operation) && repairLevel >= DRONE_REPAIR_ROUTE_THRESHOLD);
    }

    private static boolean isDroneIntelOperation(String operation, String mode) {
        return "scan_area".equals(operation)
                || "scout_scan".equals(operation)
                || ("set_mode".equals(operation) && "SCOUT".equals(mode))
                || ("scout_set_mode".equals(operation) && "SCAVENGE".equals(mode));
    }

    private static boolean applyDroneState(
            ServerPlayer player,
            Map<String, Object> payload,
            Map<String, Object> resultSnapshot) {
        if (booleanValue(payload, "scoutDroneModeCycle")) {
            return applyScoutDroneModeCycle(player, payload, resultSnapshot);
        }

        String operation = stringValue(payload, "operation");
        String mode = stringValue(payload, "mode");
        boolean success = Boolean.TRUE.equals(payload.get("success"));
        boolean scoutModeOperation = isDroneScoutModeOperation(operation, mode);
        boolean intelOperation = isDroneIntelOperation(operation, mode);
        boolean salvageModeOperation = isDroneSalvageModeOperation(operation, mode);

        resultSnapshot.put("droneOperation", operation);
        resultSnapshot.put("droneMode", mode);
        resultSnapshot.put("droneStateSuccess", success);
        if (!success) {
            return false;
        }

        boolean changed = false;
        changed |= markSpecial(player, "drone:" + sanitizeTarget(operation));
        if (isDroneSupportOperation(operation, numberValue(payload, "repairLevel", 0))) {
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:repair_echo_drone", 1, payload);
        }
        if (scoutModeOperation) {
            changed |= markSpecial(player, "drone:scout_mode");
        }
        if (salvageModeOperation) {
            changed |= markSpecial(player, "drone:salvage_mode");
        }
        if (intelOperation) {
            changed |= markSpecial(player, "drone:intel_recovered");
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, "drone:intel_recovered", 1, payload);
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:recover_drone_intel", 1, payload);
        }
        if (changed && (scoutModeOperation || intelOperation)) {
            player.sendSystemMessage(Component.literal("\u00A7b[ECHO-7 // DRONE]\u00A7r Scout route intel recorded."), true);
        }

        resultSnapshot.put("droneScoutModeOperation", scoutModeOperation);
        resultSnapshot.put("droneIntelOperation", intelOperation);
        resultSnapshot.put("droneSalvageModeOperation", salvageModeOperation);
        return changed;
    }

    private static boolean applyScoutDroneModeCycle(
            ServerPlayer player,
            Map<String, Object> payload,
            Map<String, Object> resultSnapshot) {
        resultSnapshot.put("droneOperation", "scout_cycle_mode");
        resultSnapshot.put("scoutDroneModeCycleRuntime", true);

        ScoutDrone drone = findOwnedScoutDrone(player, stringValue(payload, "targetDroneId"));
        if (drone == null) {
            resultSnapshot.put("failureReason", "no owned Scout Drone found for mode cycle");
            resultSnapshot.put("hudOrEventEmitted", true);
            player.sendSystemMessage(Component.literal(
                    "\u00A7c[ECHO-7 // DRONE]\u00A7r No deployed Scout Drone found. Deploy one first."));
            return false;
        }

        String beforeMode = drone.getMode().name();
        resultSnapshot.put("droneEntityId", drone.getUUID().toString());
        resultSnapshot.put("dronePos", positionSnapshot(drone.blockPosition()));
        resultSnapshot.put("droneModeBefore", beforeMode);

        drone.cycleMode();
        String afterMode = drone.getMode().name();
        boolean changed = !beforeMode.equals(afterMode);

        resultSnapshot.put("droneModeAfter", afterMode);
        resultSnapshot.put("droneMode", afterMode);
        resultSnapshot.put("droneStateSuccess", changed);
        resultSnapshot.put("hudOrEventEmitted", true);
        if (!changed) {
            resultSnapshot.put("noopReason", "Scout Drone mode already matched requested state");
            return false;
        }

        changed |= markSpecial(player, "drone:scout_cycle_mode");
        if ("SCAVENGE".equals(afterMode)) {
            changed |= markSpecial(player, "drone:scout_mode");
            changed |= markSpecial(player, "drone:intel_recovered");
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, "drone:intel_recovered", 1, payload);
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:recover_drone_intel", 1, payload);
            player.sendSystemMessage(Component.literal("\u00A7b[ECHO-7 // DRONE]\u00A7r Scout route intel recorded."), true);
            resultSnapshot.put("droneScoutModeOperation", true);
            resultSnapshot.put("droneIntelOperation", true);
        } else {
            resultSnapshot.put("droneScoutModeOperation", false);
            resultSnapshot.put("droneIntelOperation", false);
        }
        resultSnapshot.put("droneSalvageModeOperation", "SCAVENGE".equals(afterMode));
        return changed;
    }

    private static ScoutDrone findOwnedScoutDrone(ServerPlayer player, String targetDroneId) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        for (ScoutDrone drone : serverLevel.getEntitiesOfClass(ScoutDrone.class, player.getBoundingBox().inflate(64.0D))) {
            if (drone.getOwnerUUID() == null || !drone.getOwnerUUID().equals(player.getUUID())) {
                continue;
            }
            if (targetDroneId.isBlank() || targetDroneId.equals(drone.getUUID().toString())) {
                return drone;
            }
        }
        return null;
    }

    private static boolean isDroneScoutModeOperation(String operation, String mode) {
        return ("set_mode".equals(operation) && "SCOUT".equals(mode))
                || ("scout_set_mode".equals(operation) && "SCAVENGE".equals(mode));
    }

    private static boolean isDroneSalvageModeOperation(String operation, String mode) {
        return "set_mode".equals(operation) && "SALVAGE".equals(mode);
    }

    private static boolean markSpecial(ServerPlayer player, String marker) {
        if (marker == null || marker.isBlank()) {
            return false;
        }
        QuestData quest = QuestData.get(player);
        if (quest.hasVisitedLocation("special", marker)) {
            return false;
        }
        quest.visitLocation("special", marker);
        QuestData.saveAndSync(player, quest);
        return true;
    }

    private static SchematicFragmentItem.SchematicType firstMissingSchematicType(ResearchData research) {
        for (SchematicFragmentItem.SchematicType type : SchematicFragmentItem.SchematicType.values()) {
            if (!research.hasSchematic(categoryKey(type))) {
                return type;
            }
        }
        return null;
    }

    private static String categoryKey(SchematicFragmentItem.SchematicType type) {
        return type.getDisplayName().toLowerCase(java.util.Locale.ROOT);
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
        AshfallAdapterCoreRuntimeGuards.ensureMissionContentReady(player, "exploration");
        return EchoCoreServices.recordMissionObjective(
                player,
                type,
                targetId,
                Math.max(1, count),
                Map.of(
                        "source", EchoAshfallProtocol.MODID,
                        "adapterCoreEvent", String.valueOf(payload.getOrDefault("source", "exploration_runtime"))));
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

    private static Map<String, Object> scanPayload(POIScannerService.ScanHit hit) {
        return Map.ofEntries(
                Map.entry("siteId", hit.id()),
                Map.entry("structureId", hit.structureId()),
                Map.entry("displayName", hit.displayName()),
                Map.entry("route", hit.route()),
                Map.entry("intelLine", hit.intelLine()),
                Map.entry("objective", hit.objective()),
                Map.entry("rewardTrack", hit.rewardTrack()),
                Map.entry("riskProfile", hit.riskProfile()),
                Map.entry("hazardProfile", hit.hazardProfile()),
                Map.entry("prepHint", hit.prepHint()),
                Map.entry("resourceProfile", hit.resourceProfile()),
                Map.entry("distance", hit.distance()),
                Map.entry("direction", hit.direction()),
                Map.entry("discovered", hit.discovered()),
                Map.entry("objectiveStatus", hit.objectiveStatus()),
                Map.entry("pos", positionSnapshot(hit.position())));
    }

    @Nullable
    private static POIScannerService.ScanHit scanHitFromPayload(Map<String, Object> payload) {
        String siteId = stringValue(payload, "siteId");
        if (siteId.isBlank()) {
            return null;
        }
        return new POIScannerService.ScanHit(
                blockPosValue(payload.get("pos")),
                siteId,
                stringValue(payload, "structureId"),
                stringValue(payload, "displayName"),
                stringValue(payload, "route"),
                stringValue(payload, "intelLine"),
                stringValue(payload, "objective"),
                stringValue(payload, "rewardTrack"),
                stringValue(payload, "riskProfile"),
                stringValue(payload, "hazardProfile"),
                stringValue(payload, "prepHint"),
                stringValue(payload, "resourceProfile"),
                doubleValue(payload, "distance", 0.0D),
                stringValue(payload, "direction"),
                booleanValue(payload, "discovered"),
                stringValue(payload, "objectiveStatus"));
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

    private static int numberValue(Map<String, Object> payload, String key, int fallback) {
        Object value = payload.get(key);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static double doubleValue(Map<String, Object> payload, String key, double fallback) {
        Object value = payload.get(key);
        return value instanceof Number number ? number.doubleValue() : fallback;
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

    private static String stringId(@Nullable Identifier id) {
        return id == null ? "" : id.toString();
    }

    private static String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
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
}
