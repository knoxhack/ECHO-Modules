package com.knoxhack.echogalacticcore.asdk;

import com.knoxhack.echogalacticcore.GalacticCoreIds;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadContext;
import dev.echo.nativeplatform.contracts.EchoNativeMutationReceipt;
import dev.echo.nativeplatform.contracts.EchoNativeRuntimeSide;
import dev.echo.nativeplatform.contracts.EchoNativeServiceMutation;
import dev.echo.nativeplatform.contracts.EchoNativeTypedServiceSupport;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class GalacticCoreNativeMutations {
    private GalacticCoreNativeMutations() {
    }

    public static EchoNativeServiceMutation common(String surface, String action, String target) {
        return mutation(surface, action, target, EchoNativeRuntimeSide.COMMON, Map.of());
    }

    public static EchoNativeServiceMutation common(
            String surface,
            String action,
            String target,
            Map<String, Object> evidence
    ) {
        return mutation(surface, action, target, EchoNativeRuntimeSide.COMMON, evidence);
    }

    public static EchoNativeServiceMutation client(
            String surface,
            String action,
            String target,
            Map<String, Object> evidence
    ) {
        return mutation(surface, action, target, EchoNativeRuntimeSide.CLIENT, evidence);
    }

    public static EchoNativeServiceMutation server(
            String surface,
            String action,
            String target,
            Map<String, Object> evidence
    ) {
        return mutation(surface, action, target, EchoNativeRuntimeSide.SERVER, evidence);
    }

    public static EchoNativeServiceMutation mutation(
            String surface,
            String action,
            String target,
            EchoNativeRuntimeSide side,
            Map<String, Object> evidence
    ) {
        return new EchoNativeServiceMutation(GalacticCoreIds.MOD_ID, surface, action, target, side, evidence);
    }

    public static Map<String, Object> evidence(Object... values) {
        if (values.length % 2 != 0) {
            throw new IllegalArgumentException("evidence requires key/value pairs");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return Map.copyOf(result);
    }

    public static void record(EchoNativeModuleLoadContext context, EchoNativeMutationReceipt receipt) {
        context.recordMutation(receipt);
    }

    public static <T extends EchoNativeTypedServiceSupport> Optional<T> service(
            EchoNativeModuleLoadContext context,
            String serviceId,
            Class<T> serviceType
    ) {
        return context.serviceRegistry().service(serviceId, serviceType);
    }
}
