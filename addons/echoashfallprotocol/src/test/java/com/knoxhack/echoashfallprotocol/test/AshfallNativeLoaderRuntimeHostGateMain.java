package com.knoxhack.echoashfallprotocol.test;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeBlockRef;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeBlockState;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeItemStack;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationContext;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativePlayerRef;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeSaveData;
import com.knoxhack.echoashfallprotocol.event.NativeLoaderEchoRuntimeHost;
import com.knoxhack.echoashfallprotocol.event.NativeLoaderRuntimeHostFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class AshfallNativeLoaderRuntimeHostGateMain {
    private AshfallNativeLoaderRuntimeHostGateMain() {
    }

    public static void main(String[] args) {
        installDuplicateCoreHostSidecar();
        NativeLoaderEchoRuntimeHost host = NativeLoaderRuntimeHostFactory.createBackendFirst();
        require(host.nativeLoaderBackendAttached(), "Native Loader backend did not attach.");
        require(host.nativeLoaderBackendPrimary(), "Native Loader backend is not the primary runtime host.");

        NativePlayerRef player = new NativePlayerRef("ashfall-runtime-host-gate");
        long proofId = System.nanoTime();
        int proofX = 7 + (int) Math.floorMod(proofId, 1024);
        assertMutated("inventory grant", host.playerInventory().grant(
                player,
                new NativeItemStack("echoashfallprotocol:portable_signal_scanner", 1,
                        Map.of("gate", "runtime_host", "proofId", proofId)),
                context("inventory", proofId)));
        assertMutated("world block placement", host.worldBlocks().setBlock(
                new NativeBlockRef("minecraft:overworld", proofX, 72, 7),
                new NativeBlockState("echoashfallprotocol:native_loader_proof_marker",
                        Map.of("gate", "runtime_host", "proofX", proofX)),
                context("world_blocks", proofId)));
        assertMutated("save data write", host.saveData().write(
                new NativeSaveData("echoashfallprotocol", "native_loader.runtime_host_gate." + proofId,
                        Map.of("terminalViewsOnline", true, "indexRoutesOnline", true, "proofId", proofId)),
                context("save_data", proofId)));
        assertMutated("hud notification", host.hud().publishNotification(
                player,
                Map.of("surface", "EchoNativeRuntimeHost.Hud",
                        "message", "runtime host gate linked " + proofId,
                        "proofId", proofId),
                context("hud", proofId)));

        List<Map<String, Object>> ledger = hostMutationLedger(host);
        require(!ledger.isEmpty(), "Native Loader backend mutation ledger is empty.");
        require(hasMutatedSurface(ledger, "inventory"), "Mutation ledger did not record inventory mutation.");
        require(hasMutatedSurface(ledger, "world_blocks"), "Mutation ledger did not record world block mutation.");
        require(hasMutatedSurface(ledger, "save_data"), "Mutation ledger did not record save-data mutation.");
        require(hasMutatedSurface(ledger, "hud"), "Mutation ledger did not record HUD mutation.");
        System.out.println("ashfall native loader runtime host gate PASS");
    }

    private static void installDuplicateCoreHostSidecar() {
        try {
            Path sidecar = Files.createTempFile("ashfall-native-loader-service-sidecar", ".json");
            Files.writeString(sidecar, """
                    {"services":[
                    {"moduleId":"echocore","serviceId":"echo.native.command_host","implementationClass":"dev.echo.nativeplatform.loader.NativeLoaderCommandHost","surfaces":["commands"]},
                    {"moduleId":"echocore","serviceId":"echo.native.network_host","implementationClass":"dev.echo.nativeplatform.loader.NativeLoaderNetworkHost","surfaces":["network"]},
                    {"moduleId":"echocore","serviceId":"echo.native.config_host","implementationClass":"dev.echo.nativeplatform.loader.NativeLoaderConfigHost","surfaces":["config"]},
                    {"moduleId":"echocore","serviceId":"echo_native.lifecycle_host","implementationClass":"dev.echo.nativeplatform.loader.NativeLoaderLifecycleEventHost","surfaces":["lifecycle"]},
                    {"moduleId":"echocore","serviceId":"echo_native.event_host","implementationClass":"dev.echo.nativeplatform.loader.NativeLoaderLifecycleEventHost","surfaces":["events"]},
                    {"moduleId":"echo-native-loader","serviceId":"adaptercore.native_loader.backend","implementationClass":"dev.echo.nativeplatform.loader.NativeLoaderAdapterCoreBackend","surfaces":["inventory"]},
                    {"moduleId":"echoashfallprotocol","serviceId":"echoashfallprotocol.test.imported_service","implementationClass":"java.lang.Object","surfaces":["diagnostics"]}
                    ]}
                    """, StandardCharsets.UTF_8);
            sidecar.toFile().deleteOnExit();
            System.setProperty("echo.native.serviceRegistryPath", sidecar.toString());
        } catch (java.io.IOException failure) {
            throw new AssertionError("Unable to create duplicate core-host sidecar fixture.", failure);
        }
    }

    private static NativeMutationContext context(String surface, long proofId) {
        return new NativeMutationContext(
                "echoashfallprotocol",
                "minecraft:overworld",
                "ashfall-native-loader-runtime-host-gate:" + surface + ":" + proofId,
                "NATIVE",
                proofId,
                Map.of("gate", "ashfall_native_loader_runtime_host", "surface", surface, "proofId", proofId));
    }

    private static void assertMutated(String label, NativeResult result) {
        require(result != null, label + " returned no result.");
        require(result.completedWithMutation(), label + " did not mutate: " + result.status()
                + " " + result.message() + " " + result.snapshot());
        require(Boolean.TRUE.equals(result.snapshot().get("adapterCoreCallEnteredNativeLoaderBackend")),
                label + " did not enter Native Loader backend: " + result.snapshot());
        require(!Boolean.TRUE.equals(result.snapshot().get("nativeLoaderBackendCallFailure")),
                label + " reported Native Loader backend call failure: " + result.snapshot());
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> hostMutationLedger(NativeLoaderEchoRuntimeHost host) {
        try {
            Object ledger = host.nativeLoaderBackend().getClass().getMethod("mutationLedger").invoke(host.nativeLoaderBackend());
            Object report = ledger.getClass().getMethod("toReport").invoke(ledger);
            if (report instanceof List<?> list) {
                return (List<Map<String, Object>>) (List<?>) list;
            }
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Unable to inspect Native Loader mutation ledger.", failure);
        }
        return List.of();
    }

    private static boolean hasMutatedSurface(List<Map<String, Object>> ledger, String surface) {
        for (Map<String, Object> record : ledger) {
            if (surface.equals(String.valueOf(record.get("surface")))
                    && "MUTATED".equals(String.valueOf(record.get("status")))) {
                return true;
            }
        }
        return false;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
