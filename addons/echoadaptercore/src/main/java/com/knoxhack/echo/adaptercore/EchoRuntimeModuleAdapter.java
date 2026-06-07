package com.knoxhack.echo.adaptercore;

import java.util.Map;
import java.util.Set;

/**
 * Generic AdapterCore module activation seam shared by runtime targets.
 *
 * <p>Implementations must keep runtime-specific work behind their adapter
 * contract. The NeoForge module path, ECHO Native Loader path, and ECHO Runtime
 * Standalone path are separate targets that can share module contract metadata
 * without sharing classloaders, registries, or process launch behavior.</p>
 */
public interface EchoRuntimeModuleAdapter {
    Map<String, Object> activateRuntime(Map<String, String> context);

    default Set<EchoAdapterRuntime> supportedRuntimes() {
        return Set.of(
                EchoAdapterRuntime.NEOFORGE,
                EchoAdapterRuntime.NATIVE_CLIENT,
                EchoAdapterRuntime.STANDALONE,
                EchoAdapterRuntime.ECHO_NATIVE,
                EchoAdapterRuntime.ECHO_RUNTIME_STANDALONE);
    }

    default String adapterContract() {
        return "adaptercore.runtime_module";
    }
}
