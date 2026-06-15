package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeBlockRef;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeBlockState;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeEvent;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeItemStack;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationContext;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationProofKind;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationReceipt;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativePlayerRef;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativePosition;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeSaveData;
import com.knoxhack.echo.adaptercore.EchoRuntimeActionDispatcher.EchoRuntimeAction;
import com.knoxhack.echo.adaptercore.EchoRuntimeActionDispatcher.EchoRuntimeActionOutcome;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeLoaderAttachedRuntimeHostSmoke {
    private static final String RUNTIME_HOST_ID = EchoNativeLoaderAttachedRuntimeHost.DEFAULT_RUNTIME_HOST_ID;
    private static final String ACTION_ID = "ashfall.native_loader.first_spawn";

    private EchoNativeLoaderAttachedRuntimeHostSmoke() {
    }

    public static void main(String[] args) throws Exception {
        Path savesDirectory = args.length > 0
                ? Path.of(args[0]).toAbsolutePath().normalize()
                : Path.of("build/adaptercore-native-loader-host/saves").toAbsolutePath().normalize();
        Path reportPath = args.length > 1
                ? Path.of(args[1]).toAbsolutePath().normalize()
                : Path.of("build/adaptercore-native-loader-host/adaptercore-native-loader-host.json")
                        .toAbsolutePath()
                        .normalize();
        deleteRecursively(savesDirectory);

        Object nativeLoaderBackend = createNativeLoaderBackend(savesDirectory);
        EchoRuntimeHostRegistry registry = new EchoRuntimeHostRegistry();
        EchoRuntimeMutationLedger adapterCoreLedger = new EchoRuntimeMutationLedger();
        EchoRuntimeActionDispatcher dispatcher = new EchoRuntimeActionDispatcher(
                registry,
                adapterCoreLedger,
                EchoContentAliasResolver.standard());
        EchoRuntimeHostRegistry.RegisteredRuntimeHost registered =
                EchoNativeLoaderAttachedRuntimeHost.register(registry, RUNTIME_HOST_ID, nativeLoaderBackend);
        EchoNativeLoaderAttachedRuntimeHost nativeHost =
                (EchoNativeLoaderAttachedRuntimeHost) registered.host();

        dispatcher.registerAction(RUNTIME_HOST_ID, ACTION_ID, (runtimeHost, action) -> {
            EchoNativeLoaderAttachedRuntimeHost host = (EchoNativeLoaderAttachedRuntimeHost) runtimeHost;
            int beforeLedgerCount = host.nativeLoaderMutationLedger().size();
            Map<String, Object> before = Map.of(
                    "nativeLoaderLedgerCount", beforeLedgerCount,
                    "nativeLoaderSnapshot", host.nativeLoaderSnapshot());

            NativeResult grant = host.playerInventory().grant(action.targetPlayer(), new NativeItemStack(
                    "echoashfallprotocol:drop_pod_beacon",
                    1,
                    Map.of("source", "adaptercore_native_loader_smoke")), action.context());
            NativeResult persistentState = host.playerState().writePersistentState(
                    action.targetPlayer(),
                    "ashfall.first_spawn",
                    "drop_pod_linked",
                    action.context());
            NativeResult block = host.worldBlocks().setBlock(action.targetBlock(), new NativeBlockState(
                    "echoashfallprotocol:drop_pod_marker",
                    Map.of("source", "adaptercore_native_loader_smoke")), action.context());
            NativeResult save = host.saveData().write(new NativeSaveData(
                    "ashfall",
                    "first_spawn",
                    Map.of("status", "complete", "playerId", action.targetPlayer().playerId())), action.context());
            NativeResult event = host.events().publish(new NativeEvent(
                    "ashfall.first_spawn",
                    action.targetPlayer(),
                    Map.of("status", "native_loader_backend_mutated")), action.context());
            NativeResult hud = host.hud().publishNotification(
                    action.targetPlayer(),
                    Map.of("message", "Drop pod telemetry linked through Native Loader."),
                    action.context());

            List<NativeResult> surfaceResults = List.of(grant, persistentState, block, save, event, hud);
            boolean everySurfaceMutated = surfaceResults.stream().allMatch(NativeResult::completedWithMutation);
            int afterLedgerCount = host.nativeLoaderMutationLedger().size();
            boolean nativeLoaderStateChanged = afterLedgerCount > beforeLedgerCount && everySurfaceMutated;
            Map<String, Object> after = Map.of(
                    "nativeLoaderLedgerCount", afterLedgerCount,
                    "nativeLoaderSnapshot", host.nativeLoaderSnapshot(),
                    "surfaceStatuses", surfaceResults.stream().map(NativeResult::status).toList());
            NativeResult result = nativeLoaderStateChanged
                    ? NativeResult.mutated("AdapterCore action mutated through Native Loader backend.", Map.of(
                            "adapterCoreEnteredNativeLoaderBackend", true,
                            "nativeLoaderLedgerDelta", afterLedgerCount - beforeLedgerCount,
                            "surfaceStatuses", surfaceResults.stream().map(NativeResult::status).toList()),
                    nativeLoaderActionReceipt(action, before, after))
                    : NativeResult.failed("AdapterCore action did not mutate through every Native Loader surface.", Map.of(
                            "adapterCoreEnteredNativeLoaderBackend", true,
                            "nativeLoaderLedgerDelta", afterLedgerCount - beforeLedgerCount,
                            "surfaceStatuses", surfaceResults.stream().map(NativeResult::status).toList()));
            EchoNativeRuntimeHost.validateTruth(result, nativeLoaderStateChanged);
            return EchoRuntimeActionOutcome.of(before, result, after, true, true);
        });

        NativeMutationContext context = new NativeMutationContext(
                EchoAdapterConstants.MOD_ID,
                "minecraft:overworld",
                "adaptercore-native-loader-host-smoke-1",
                "server",
                1729L,
                Map.of("source", "adaptercore_native_loader_host_smoke"));
        NativePlayerRef player = new NativePlayerRef("player:adaptercore-native-loader-smoke");
        NativeBlockRef block = new NativeBlockRef("minecraft:overworld", 0, 80, 0);
        NativeResult dispatchResult = dispatcher.dispatch(new EchoRuntimeAction(
                ACTION_ID,
                RUNTIME_HOST_ID,
                Map.of("packId", "echoashfallprotocol", "moduleId", "echoadaptercore"),
                player,
                "minecraft:overworld",
                new NativePosition("minecraft:overworld", 0.0D, 80.0D, 0.0D, 0.0F, 0.0F),
                block,
                context));
        NativeResult unchangedBlock = nativeHost.worldBlocks().setBlock(block, new NativeBlockState(
                "echoashfallprotocol:drop_pod_marker",
                Map.of()), context);
        NativeMutationContext typedReceiptContext = new NativeMutationContext(
                EchoAdapterConstants.MOD_ID,
                "minecraft:overworld",
                "adaptercore-native-loader-typed-receipt-smoke",
                "server",
                1730L,
                Map.of("source", "adaptercore_native_loader_typed_receipt_smoke"));
        NativeResult acceptedTypedReceipt = new EchoNativeLoaderAttachedRuntimeHost(new FakeTypedReceiptBackend(true))
                .playerInventory()
                .grant(player, new NativeItemStack(
                        "echoashfallprotocol:typed_receipt_probe",
                        1,
                        Map.of()), typedReceiptContext);
        NativeResult rejectedTypedReceipt = new EchoNativeLoaderAttachedRuntimeHost(new FakeTypedReceiptBackend(false))
                .playerInventory()
                .grant(player, new NativeItemStack(
                        "echoashfallprotocol:typed_receipt_probe",
                        1,
                        Map.of()), typedReceiptContext);

        List<Map<String, Object>> nativeLoaderLedger = nativeHost.nativeLoaderMutationLedger();
        List<Map<String, Object>> adapterLedger = adapterCoreLedger.snapshots();
        boolean nativeLedgerMutated = nativeLoaderLedger.stream()
                .filter(record -> "RESOLVED".equals(String.valueOf(record.get("status"))))
                .count() == 1
                && nativeLoaderLedger.stream()
                .filter(record -> "MUTATED".equals(String.valueOf(record.get("status"))))
                .count() >= 6;
        boolean nativeTypedReceiptsPresent = nativeLoaderLedger.stream()
                .filter(record -> "MUTATED".equals(String.valueOf(record.get("status"))))
                .allMatch(record -> record.get("typedMutationReceipt") instanceof Map<?, ?> receipt
                        && "MUTATED".equals(String.valueOf(receipt.get("status"))));
        boolean adapterCoreLedgerMutated = adapterLedger.size() == 1
                && "MUTATED".equals(String.valueOf(adapterLedger.get(0).get("resultStatus")))
                && Boolean.TRUE.equals(adapterLedger.get(0).get("releaseProof"));
        boolean noFakeNoopMutation = unchangedBlock.completedWithoutMutation();
        boolean typedReceiptEvidenceRequired = acceptedTypedReceipt.completedWithReleaseProof()
                && rejectedTypedReceipt.resultStatus() == EchoNativeRuntimeHost.NativeResultStatus.FAILED
                && !rejectedTypedReceipt.hasReleaseProof()
                && rejectedTypedReceipt.failureReason().contains("missing AdapterCore live Minecraft proof");
        boolean saveDataWritten = Files.isRegularFile(savesDirectory.resolve("saveData.json"));
        boolean inventoryWritten = Files.isRegularFile(savesDirectory.resolve("inventory.json"));
        boolean blockWritten = Files.isRegularFile(savesDirectory.resolve("worldBlocks.json"));
        boolean passed = dispatchResult.completedWithMutation()
                && dispatchResult.hasReleaseProof()
                && nativeLedgerMutated
                && nativeTypedReceiptsPresent
                && adapterCoreLedgerMutated
                && noFakeNoopMutation
                && typedReceiptEvidenceRequired
                && saveDataWritten
                && inventoryWritten
                && blockWritten;

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("schema", "echo.adaptercore.native_loader_runtime_host_bridge_smoke.v1");
        report.put("status", passed ? "PASS" : "FAIL");
        report.put("runtimeLane", "Native Loader");
        report.put("laneRole", "primary future mod loader");
        report.put("fallbackLane", "NeoForge compatibility backend");
        report.put("parityLane", "Standalone Runtime parity/runtime harness");
        report.put("adapterCoreContract", EchoNativeRuntimeHost.class.getName());
        report.put("nativeLoaderRuntimeHost", "dev.echo.nativeplatform.loader.NativeLoaderRuntimeHost");
        report.put("nativeLoaderBackend", "dev.echo.nativeplatform.loader.NativeLoaderAdapterCoreBackend");
        report.put("adapterCoreRuntimeHost", EchoNativeLoaderAttachedRuntimeHost.class.getName());
        report.put("runtimeHostId", RUNTIME_HOST_ID);
        report.put("actionId", ACTION_ID);
        report.put("adapterCoreCallEnteredNativeLoaderBackend", dispatchResult.completedWithMutation());
        report.put("dispatchStatus", dispatchResult.status());
        report.put("dispatchReleaseProof", dispatchResult.hasReleaseProof());
        report.put("unchangedBlockStatus", unchangedBlock.status());
        report.put("noFakeNoopMutation", noFakeNoopMutation);
        report.put("acceptedTypedReceiptStatus", acceptedTypedReceipt.status());
        report.put("acceptedTypedReceiptReleaseProof", acceptedTypedReceipt.hasReleaseProof());
        report.put("rejectedTypedReceiptStatus", rejectedTypedReceipt.status());
        report.put("typedReceiptEvidenceRequired", typedReceiptEvidenceRequired);
        report.put("nativeLoaderMutationLedger", nativeLoaderLedger);
        report.put("nativeTypedReceiptsPresent", nativeTypedReceiptsPresent);
        report.put("adapterCoreMutationLedger", adapterLedger);
        report.put("nativeLoaderSnapshot", nativeHost.nativeLoaderSnapshot());
        report.put("persistedSaveDirectory", savesDirectory.toString().replace('\\', '/'));
        report.put("persistedStateFiles", Map.of(
                "inventory", inventoryWritten,
                "worldBlocks", blockWritten,
                "saveData", saveDataWritten));
        report.put("gameplayReadyClaimAllowed", false);
        report.put("liveClientGameplayReadyClaimAllowed", false);
        report.put("claimBoundary", "AdapterCore host mutation is proven through an attached Native Loader backend and persisted Native Loader host state. This is not a live Minecraft client proof.");

        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, toJson(report) + "\n", StandardCharsets.UTF_8);
        if (!passed) {
            throw new AssertionError("AdapterCore Native Loader runtime host smoke failed: " + report);
        }
        System.out.println("adaptercore native loader runtime host smoke PASS " + reportPath);
    }

    private static NativeMutationReceipt nativeLoaderActionReceipt(
            EchoRuntimeAction action,
            Map<String, Object> before,
            Map<String, Object> after) {
        return new NativeMutationReceipt(
                ACTION_ID + ":" + action.context().idempotencyKey(),
                RUNTIME_HOST_ID,
                EchoAdapterConstants.MOD_ID,
                EchoNativeRuntimeHost.interfaceForHostApi(ACTION_ID),
                ACTION_ID,
                "MUTATED",
                NativeMutationProofKind.HOST_STATE,
                before,
                after,
                true,
                true,
                action.context().idempotencyKey());
    }

    private static Object createNativeLoaderBackend(Path savesDirectory) throws Exception {
        Class<?> registryClass = Class.forName("dev.echo.nativeplatform.contracts.EchoNativeServiceRegistry");
        Object serviceRegistry = registryClass.getConstructor().newInstance();

        Class<?> contextClass = Class.forName("dev.echo.nativeplatform.loader.NativeLoaderRuntimeHostContext");
        Constructor<?> contextConstructor = contextClass.getConstructor(
                String.class,
                String.class,
                registryClass,
                Path.class,
                String.class,
                boolean.class);
        Object context = contextConstructor.newInstance(
                "echoashfallprotocol",
                EchoAdapterConstants.MOD_ID,
                serviceRegistry,
                savesDirectory,
                RUNTIME_HOST_ID,
                true);

        Class<?> hostClass = Class.forName("dev.echo.nativeplatform.loader.NativeLoaderRuntimeHost");
        Object host = hostClass.getConstructor(contextClass).newInstance(context);
        Class<?> bridgeClass = Class.forName("dev.echo.nativeplatform.loader.NativeLoaderServiceBridge");
        Object bridge = bridgeClass.getConstructor(registryClass).newInstance(serviceRegistry);
        Class<?> ledgerClass = Class.forName("dev.echo.nativeplatform.loader.NativeLoaderMutationLedger");
        Object ledger = ledgerClass.getConstructor().newInstance();
        Class<?> backendClass = Class.forName("dev.echo.nativeplatform.loader.NativeLoaderAdapterCoreBackend");
        Object backend = backendClass.getConstructor(hostClass, bridgeClass, ledgerClass)
                .newInstance(host, bridge, ledger);
        Method register = registryClass.getMethod("register", String.class, String.class, Object.class, List.class, String.class);
        register.invoke(serviceRegistry, EchoAdapterConstants.MOD_ID, "adaptercore.native_loader.backend", backend, List.of(
                "inventory",
                "player_state",
                "world_blocks",
                "world_state",
                "structures",
                "block_entities",
                "capabilities",
                "events",
                "packets_hud",
                "hud",
                "save_data"),
                "dev.echo.nativeplatform.loader.NativeLoaderAdapterCoreBackend");
        return backend;
    }

    public static final class FakeTypedReceiptBackend {
        private final boolean stateChanged;

        FakeTypedReceiptBackend(boolean stateChanged) {
            this.stateChanged = stateChanged;
        }

        public FakeMutationRecord grantItem(String playerId, String itemId, int count) {
            return new FakeMutationRecord(stateChanged, playerId, itemId, count);
        }
    }

    public static final class FakeMutationRecord {
        private final boolean stateChanged;
        private final String playerId;
        private final String itemId;
        private final int count;

        FakeMutationRecord(boolean stateChanged, String playerId, String itemId, int count) {
            this.stateChanged = stateChanged;
            this.playerId = playerId;
            this.itemId = itemId;
            this.count = count;
        }

        public Map<String, Object> toReport() {
            Map<String, Object> before = stateChanged
                    ? Map.of("inventoryCount", 0)
                    : Map.of("inventoryCount", count);
            Map<String, Object> after = Map.of("inventoryCount", count);
            Map<String, Object> receipt = Map.of(
                    "status", "MUTATED",
                    "evidence", Map.of(
                            "before", before,
                            "after", after));
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("status", "MUTATED");
            report.put("operationId", stateChanged ? "fake-typed-receipt-accepted" : "fake-typed-receipt-rejected");
            report.put("idempotencyKey", stateChanged ? "fake-typed-accepted" : "fake-typed-rejected");
            report.put("playerId", playerId);
            report.put("itemId", itemId);
            report.put("count", count);
            report.put("typedMutationReceipt", receipt);
            report.put("liveRuntimeAccessed", false);
            report.put("minecraftRuntimeAccessed", false);
            report.put("liveRuntimeMutationSupported", false);
            report.put("liveRuntimeReleaseProofSatisfied", false);
            report.put("liveRuntimeSurfaceMutationSatisfied", false);
            report.put("mirrorOnlyReleaseProof", true);
            return Map.copyOf(report);
        }
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (var stream = Files.walk(path)) {
            for (Path item : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(item);
            }
        }
    }

    private static String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String string) {
            return "\"" + string.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder out = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    out.append(',');
                }
                out.append('\n')
                        .append("  ")
                        .append(toJson(String.valueOf(entry.getKey())))
                        .append(": ")
                        .append(indent(toJson(entry.getValue())));
                first = false;
            }
            if (!map.isEmpty()) {
                out.append('\n');
            }
            return out.append('}').toString();
        }
        if (value instanceof Collection<?> collection) {
            StringBuilder out = new StringBuilder("[");
            boolean first = true;
            for (Object item : collection) {
                if (!first) {
                    out.append(',');
                }
                out.append('\n').append("  ").append(indent(toJson(item)));
                first = false;
            }
            if (!collection.isEmpty()) {
                out.append('\n');
            }
            return out.append(']').toString();
        }
        return toJson(String.valueOf(value));
    }

    private static String indent(String value) {
        return value.replace("\n", "\n  ");
    }
}
