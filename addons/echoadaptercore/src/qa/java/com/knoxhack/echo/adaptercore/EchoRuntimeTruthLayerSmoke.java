package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeBlockEntitySnapshot;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeBlockRef;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeBlockState;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeCapabilityRequest;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeEvent;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeItemStack;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationContext;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativePacket;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativePlayerRef;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativePosition;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeSaveData;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeStructurePlacement;
import com.knoxhack.echo.adaptercore.EchoRuntimeActionDispatcher.EchoRuntimeAction;
import com.knoxhack.echo.adaptercore.EchoRuntimeActionDispatcher.EchoRuntimeActionOutcome;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EchoRuntimeTruthLayerSmoke {
    private static final String RUNTIME_HOST_ID = "echoadaptercore:truth_smoke_host";

    private EchoRuntimeTruthLayerSmoke() {
    }

    public static void main(String[] args) {
        Map<String, Object> report = capture();
        if (!Boolean.TRUE.equals(report.get("passed"))) {
            throw new AssertionError("EchoRuntimeTruthLayerSmoke failed: " + report);
        }
        System.out.println("echo adaptercore runtime truth layer smoke PASS ledgerEntries="
                + report.get("ledgerEntryCount"));
    }

    public static Map<String, Object> capture() {
        EchoRuntimeHostRegistry registry = new EchoRuntimeHostRegistry();
        EchoRuntimeMutationLedger ledger = new EchoRuntimeMutationLedger();
        EchoRuntimeActionDispatcher dispatcher = new EchoRuntimeActionDispatcher(
                registry,
                ledger,
                EchoContentAliasResolver.standard());
        TruthSmokeHost host = new TruthSmokeHost();
        registry.register(host, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of("EchoNativeRuntimeHost.SaveData", "EchoNativeRuntimeHost.Events"),
                Set.of(EchoCanonicalContentIds.EVENT_PLAYER_ITEM_CONSUMED),
                Set.of(EchoCanonicalContentIds.ITEM_CLEAN_WATER_BOTTLE),
                true,
                true,
                true));

        dispatcher.registerAction(RUNTIME_HOST_ID, "item_consumed", (runtimeHost, action) -> {
            TruthSmokeHost truthHost = (TruthSmokeHost) runtimeHost;
            Map<String, Object> before = Map.of(
                    "saveWrites", truthHost.saveWrites(),
                    "events", truthHost.eventsPublished());
            NativeResult saveResult = runtimeHost.saveData().write(new NativeSaveData(
                    "mission",
                    "player.item_used",
                    Map.of(
                            "playerId", action.targetPlayer().playerId(),
                            "itemId", action.inputPayload().get("itemId"),
                            "count", action.inputPayload().get("count"))), action.context());
            NativeResult eventResult = runtimeHost.events().publish(new NativeEvent(
                    EchoCanonicalContentIds.EVENT_PLAYER_ITEM_CONSUMED,
                    action.targetPlayer(),
                    action.inputPayload()), action.context());
            Map<String, Object> after = Map.of(
                    "saveWrites", truthHost.saveWrites(),
                    "events", truthHost.eventsPublished(),
                    "eventStatus", eventResult.status());
            return EchoRuntimeActionOutcome.of(before, saveResult, after, true, true);
        });

        NativeMutationContext context = new NativeMutationContext(
                EchoAdapterConstants.MOD_ID,
                "minecraft:overworld",
                "truth-smoke-1",
                "server",
                42L,
                Map.of("source", "truth_layer_smoke"));
        NativePlayerRef player = new NativePlayerRef("player-truth-smoke");
        NativeResult mutated = dispatcher.dispatch(new EchoRuntimeAction(
                "item_consumed",
                RUNTIME_HOST_ID,
                Map.of(
                        "itemId", EchoCanonicalContentIds.ITEM_CLEAN_WATER_BOTTLE,
                        "count", 1),
                player,
                "minecraft:overworld",
                new NativePosition("minecraft:overworld", 1.0D, 64.0D, 1.0D, 0.0F, 0.0F),
                null,
                context));
        NativeResult unsupported = dispatcher.dispatch(new EchoRuntimeAction(
                "ashfall.unimplemented_action",
                RUNTIME_HOST_ID,
                Map.of("source", "truth_layer_smoke"),
                player,
                "minecraft:overworld",
                null,
                null,
                context));

        NativeResult legacyNoop = new NativeResult(false, "SKIPPED_ALREADY_REPAIRED", "legacy skip", Map.of());
        NativeResult legacyMutated = new NativeResult(true, "MUTATED_DROPPED", "legacy mutation", Map.of());
        NativeResult queued = new NativeResult(false, "PLANNED", "queued only", Map.of());
        Map<String, Object> gapAudit = new EchoNativeRuntimeGapAudit(EchoAdapterConstants.MOD_ID).audit(
                "echoadaptercore:truth_smoke_gap_audit",
                List.of(Map.of(
                        "id", "echoadaptercore:truth_smoke_claimed_live_replay",
                        "bridge", "adaptercore.native_event_replay",
                        "liveRuntimeMutation", true,
                        "nativeStateMutated", true,
                        "minecraftRuntimeAccessed", false,
                        "minecraftRuntimeMutated", false,
                        "status", "PASS")));
        List<EchoNativeRuntimeHost.NativeMutationLedgerEntry> entries = ledger.entries();
        boolean ledgerMutatedTruth = entries.size() == 2
                && EchoCanonicalContentIds.EVENT_PLAYER_ITEM_CONSUMED.equals(entries.get(0).actionId())
                && entries.get(0).resultStatus() == NativeResultStatus.MUTATED
                && entries.get(0).saveTouched()
                && entries.get(0).hudOrEventEmitted()
                && entries.get(1).resultStatus() == NativeResultStatus.UNSUPPORTED
                && !entries.get(1).saveTouched()
                && !entries.get(1).hudOrEventEmitted();
        // validateTruth acceptance gate checks
        boolean validateTruthPassMutated = true;
        boolean validateTruthPassNoop = true;
        boolean validateTruthFailLyingMutated = false;
        boolean validateTruthFailLyingNoop = false;
        try {
            EchoNativeRuntimeHost.validateTruth(mutated, true);
        } catch (IllegalStateException e) {
            validateTruthPassMutated = false;
        }
        try {
            EchoNativeRuntimeHost.validateTruth(NativeResult.noop("actual noop", Map.of()), false);
        } catch (IllegalStateException e) {
            validateTruthPassNoop = false;
        }
        try {
            EchoNativeRuntimeHost.validateTruth(NativeResult.mutated("lying mutation", Map.of()), false);
        } catch (IllegalStateException e) {
            validateTruthFailLyingMutated = true;
        }
        try {
            EchoNativeRuntimeHost.validateTruth(NativeResult.noop("lying noop", Map.of()), true);
        } catch (IllegalStateException e) {
            validateTruthFailLyingNoop = true;
        }

        // Queue must report QUEUED, not done
        EchoNativeMinecraftRuntimeHostCallQueue mcQueue = new EchoNativeMinecraftRuntimeHostCallQueue(EchoAdapterConstants.MOD_ID);
        NativeResult mcQueueResult = mcQueue.asNativeResult("smoke:mc_queue");
        boolean mcQueueIsQueued = mcQueueResult.resultStatus() == NativeResultStatus.QUEUED && !mcQueueResult.mutated();

        EchoNativeSurfaceHostCallQueue surfaceQueue = new EchoNativeSurfaceHostCallQueue(EchoAdapterConstants.MOD_ID);
        NativeResult surfaceQueueResult = surfaceQueue.asNativeResult("smoke:surface_queue");
        boolean surfaceQueueIsQueued = surfaceQueueResult.resultStatus() == NativeResultStatus.QUEUED && !surfaceQueueResult.mutated();

        // Queue canConsume against host capabilities
        boolean mcQueueCanConsume = mcQueue.canConsume(registry.resolve(RUNTIME_HOST_ID).map(EchoRuntimeHostRegistry.RegisteredRuntimeHost::capabilities).orElse(null), Map.of("hostCalls", List.of(Map.of("nativeInterface", "EchoNativeRuntimeHost.SaveData"))));
        boolean mcQueueCannotConsume = !mcQueue.canConsume(registry.resolve(RUNTIME_HOST_ID).map(EchoRuntimeHostRegistry.RegisteredRuntimeHost::capabilities).orElse(null), Map.of("hostCalls", List.of(Map.of("nativeInterface", "EchoNativeRuntimeHost.WorldBlocks"))));

        boolean passed = mutated.resultStatus() == NativeResultStatus.MUTATED
                && mutated.mutated()
                && unsupported.resultStatus() == NativeResultStatus.UNSUPPORTED
                && !unsupported.mutated()
                && legacyNoop.resultStatus() == NativeResultStatus.NOOP
                && !legacyNoop.mutated()
                && legacyMutated.resultStatus() == NativeResultStatus.MUTATED
                && legacyMutated.mutated()
                && queued.resultStatus() == NativeResultStatus.QUEUED
                && !queued.mutated()
                && host.saveWrites() == 1
                && host.eventsPublished() == 1
                && gapAudit.get("queuedGapCount") instanceof Number gapCount
                && gapCount.intValue() == 1
                && ledgerMutatedTruth
                && validateTruthPassMutated
                && validateTruthPassNoop
                && validateTruthFailLyingMutated
                && validateTruthFailLyingNoop
                && mcQueueIsQueued
                && surfaceQueueIsQueued
                && mcQueueCanConsume
                && mcQueueCannotConsume;

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("schema", "echo.adaptercore.runtime_truth_layer_smoke.v1");
        report.put("passed", passed);
        report.put("mutatedStatus", mutated.status());
        report.put("unsupportedStatus", unsupported.status());
        report.put("legacyNoopStatus", legacyNoop.status());
        report.put("legacyMutatedStatus", legacyMutated.status());
        report.put("queuedStatus", queued.status());
        report.put("saveWrites", host.saveWrites());
        report.put("eventsPublished", host.eventsPublished());
        report.put("ledgerEntryCount", entries.size());
        report.put("truthClaimGapCount", gapAudit.get("queuedGapCount"));
        report.put("validateTruthPassMutated", validateTruthPassMutated);
        report.put("validateTruthPassNoop", validateTruthPassNoop);
        report.put("validateTruthFailLyingMutated", validateTruthFailLyingMutated);
        report.put("validateTruthFailLyingNoop", validateTruthFailLyingNoop);
        report.put("mcQueueIsQueued", mcQueueIsQueued);
        report.put("surfaceQueueIsQueued", surfaceQueueIsQueued);
        report.put("mcQueueCanConsume", mcQueueCanConsume);
        report.put("mcQueueCannotConsume", mcQueueCannotConsume);
        report.put("ledger", ledger.snapshots());
        return Map.copyOf(report);
    }

    private static NativeResult unsupported(String message, Map<String, Object> snapshot) {
        return NativeResult.unsupported(message, snapshot);
    }

    private static final class TruthSmokeHost implements EchoNativeRuntimeHost {
        private final Map<String, Map<String, Object>> saveData = new LinkedHashMap<>();
        private final List<NativeEvent> events = new java.util.ArrayList<>();

        int saveWrites() {
            return saveData.size();
        }

        int eventsPublished() {
            return events.size();
        }

        @Override
        public PlayerInventory playerInventory() {
            return new PlayerInventory() {
                @Override
                public NativeResult grant(NativePlayerRef player, NativeItemStack stack, NativeMutationContext context) {
                    return unsupported("Truth smoke host does not grant inventory.", Map.of());
                }

                @Override
                public NativeResult remove(NativePlayerRef player, String itemId, int count, NativeMutationContext context) {
                    return unsupported("Truth smoke host does not remove inventory.", Map.of());
                }

                @Override
                public List<NativeItemStack> snapshot(NativePlayerRef player, NativeMutationContext context) {
                    return List.of();
                }
            };
        }

        @Override
        public PlayerState playerState() {
            return new PlayerState() {
                @Override
                public NativeResult teleport(NativePlayerRef player, NativePosition position, NativeMutationContext context) {
                    return unsupported("Truth smoke host does not teleport.", Map.of());
                }

                @Override
                public NativeResult bindRespawn(
                        NativePlayerRef player,
                        NativePosition position,
                        boolean forced,
                        NativeMutationContext context) {
                    return unsupported("Truth smoke host does not bind respawn.", Map.of());
                }

                @Override
                public NativeResult grantAdvancement(
                        NativePlayerRef player,
                        String advancementId,
                        String criterion,
                        NativeMutationContext context) {
                    return unsupported("Truth smoke host does not grant advancements.", Map.of());
                }

                @Override
                public NativeResult writePersistentState(
                        NativePlayerRef player,
                        String key,
                        Object value,
                        NativeMutationContext context) {
                    return unsupported("Truth smoke host does not write player state.", Map.of());
                }
            };
        }

        @Override
        public WorldBlocks worldBlocks() {
            return new WorldBlocks() {
                @Override
                public NativeResult setBlock(NativeBlockRef block, NativeBlockState state, NativeMutationContext context) {
                    return unsupported("Truth smoke host does not set blocks.", Map.of());
                }

                @Override
                public NativeResult clearBlock(NativeBlockRef block, NativeMutationContext context) {
                    return unsupported("Truth smoke host does not clear blocks.", Map.of());
                }

                @Override
                public NativeBlockState blockState(NativeBlockRef block, NativeMutationContext context) {
                    return new NativeBlockState("minecraft:air", Map.of());
                }

                @Override
                public boolean isLoaded(NativeBlockRef block, NativeMutationContext context) {
                    return false;
                }
            };
        }

        @Override
        public WorldState worldState() {
            return new WorldState() {
                @Override
                public NativeResult writeMarker(String markerId, Map<String, Object> payload, NativeMutationContext context) {
                    return unsupported("Truth smoke host does not write world markers.", Map.of());
                }

                @Override
                public NativeResult writeWeatherState(String stateId, Map<String, Object> payload, NativeMutationContext context) {
                    return unsupported("Truth smoke host does not write weather state.", Map.of());
                }

                @Override
                public NativeResult writeRouteState(String routeId, Map<String, Object> payload, NativeMutationContext context) {
                    return unsupported("Truth smoke host does not write route state.", Map.of());
                }
            };
        }

        @Override
        public Structures structures() {
            return (placement, context) -> unsupported("Truth smoke host does not place structures.", Map.of());
        }

        @Override
        public BlockEntities blockEntities() {
            return new BlockEntities() {
                @Override
                public NativeResult tick(NativeBlockRef block, NativeMutationContext context) {
                    return unsupported("Truth smoke host does not tick block entities.", Map.of());
                }

                @Override
                public NativeBlockEntitySnapshot snapshot(NativeBlockRef block, NativeMutationContext context) {
                    return new NativeBlockEntitySnapshot("smoke:none", block, Map.of());
                }

                @Override
                public NativeResult applySnapshot(NativeBlockEntitySnapshot snapshot, NativeMutationContext context) {
                    return unsupported("Truth smoke host does not apply block entity snapshots.", Map.of());
                }
            };
        }

        @Override
        public Capabilities capabilities() {
            return new Capabilities() {
                @Override
                public NativeResult insertItem(NativeCapabilityRequest request, NativeItemStack stack, NativeMutationContext context) {
                    return unsupported("Truth smoke host does not insert capability items.", Map.of());
                }

                @Override
                public NativeResult extractItem(NativeCapabilityRequest request, String itemId, int count, NativeMutationContext context) {
                    return unsupported("Truth smoke host does not extract capability items.", Map.of());
                }

                @Override
                public NativeResult receiveEnergy(NativeCapabilityRequest request, int amount, NativeMutationContext context) {
                    return unsupported("Truth smoke host does not receive energy.", Map.of());
                }

                @Override
                public NativeResult extractEnergy(NativeCapabilityRequest request, int amount, NativeMutationContext context) {
                    return unsupported("Truth smoke host does not extract energy.", Map.of());
                }

                @Override
                public Map<String, Object> readCapability(NativeCapabilityRequest request, NativeMutationContext context) {
                    return Map.of("status", NativeResultStatus.UNSUPPORTED.name());
                }
            };
        }

        @Override
        public Events events() {
            return (event, context) -> {
                events.add(event);
                return NativeResult.mutated("Published truth smoke event.", Map.of(
                        "eventId", event.eventId(),
                        "realNativeStateMutated", true));
            };
        }

        @Override
        public Packets packets() {
            return new Packets() {
                @Override
                public NativeResult sendToPlayer(NativePacket packet, NativeMutationContext context) {
                    return unsupported("Truth smoke host does not send packets.", Map.of());
                }

                @Override
                public NativeResult broadcast(NativePacket packet, NativeMutationContext context) {
                    return unsupported("Truth smoke host does not broadcast packets.", Map.of());
                }
            };
        }

        @Override
        public Hud hud() {
            return (player, payload, context) -> unsupported("Truth smoke host does not publish HUD.", Map.of());
        }

        @Override
        public SaveData saveData() {
            return new SaveData() {
                @Override
                public NativeResult write(NativeSaveData data, NativeMutationContext context) {
                    saveData.put(data.scope() + "/" + data.key(), data.payload());
                    return NativeResult.mutated("Wrote truth smoke save data.", Map.of(
                            "scope", data.scope(),
                            "key", data.key(),
                            "realNativeStateMutated", true));
                }

                @Override
                public Map<String, Object> read(String scope, String key, NativeMutationContext context) {
                    return saveData.getOrDefault(scope + "/" + key, Map.of());
                }

                @Override
                public NativeResult delete(String scope, String key, NativeMutationContext context) {
                    boolean removed = saveData.remove(scope + "/" + key) != null;
                    return removed
                            ? NativeResult.mutated("Deleted truth smoke save data.", Map.of("scope", scope, "key", key))
                            : NativeResult.noop("Truth smoke save key was already absent.", Map.of("scope", scope, "key", key));
                }
            };
        }
    }
}
