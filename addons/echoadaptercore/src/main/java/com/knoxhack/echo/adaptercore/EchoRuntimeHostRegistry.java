package com.knoxhack.echo.adaptercore;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class EchoRuntimeHostRegistry {
    private static final EchoRuntimeHostRegistry GLOBAL = new EchoRuntimeHostRegistry();

    private final ConcurrentMap<String, RegisteredRuntimeHost> hosts = new ConcurrentHashMap<>();

    public static EchoRuntimeHostRegistry global() {
        return GLOBAL;
    }

    public RegisteredRuntimeHost register(EchoNativeRuntimeHost host, EchoRuntimeHostCapabilities capabilities) {
        if (capabilities == null) {
            throw new IllegalArgumentException("runtime host capabilities must not be null");
        }
        return register(capabilities.runtimeHostId(), host, capabilities);
    }

    public RegisteredRuntimeHost register(
            String runtimeHostId,
            EchoNativeRuntimeHost host,
            EchoRuntimeHostCapabilities capabilities) {
        runtimeHostId = AdapterContractGuards.requireText(runtimeHostId, "runtime host id");
        if (host == null) {
            throw new IllegalArgumentException("runtime host must not be null");
        }
        if (capabilities == null) {
            capabilities = EchoRuntimeHostCapabilities.empty(runtimeHostId);
        }
        if (!runtimeHostId.equals(capabilities.runtimeHostId())) {
            throw new IllegalArgumentException("runtime host id must match capabilities runtime host id");
        }
        RegisteredRuntimeHost registered = new RegisteredRuntimeHost(runtimeHostId, host, capabilities);
        hosts.put(runtimeHostId, registered);
        return registered;
    }

    public Optional<RegisteredRuntimeHost> resolve(String runtimeHostId) {
        return Optional.ofNullable(hosts.get(AdapterContractGuards.optionalText(runtimeHostId)));
    }

    public Optional<EchoNativeRuntimeHost> host(String runtimeHostId) {
        return resolve(runtimeHostId).map(RegisteredRuntimeHost::host);
    }

    public Optional<EchoRuntimeHostCapabilities> capabilities(String runtimeHostId) {
        return resolve(runtimeHostId).map(RegisteredRuntimeHost::capabilities);
    }

    public Optional<RegisteredRuntimeHost> unregister(String runtimeHostId) {
        return Optional.ofNullable(hosts.remove(AdapterContractGuards.optionalText(runtimeHostId)));
    }

    public List<RegisteredRuntimeHost> registeredHosts() {
        return hosts.values().stream()
                .sorted(Comparator.comparing(RegisteredRuntimeHost::runtimeHostId))
                .toList();
    }

    public Map<String, Map<String, Object>> snapshot() {
        return registeredHosts().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        RegisteredRuntimeHost::runtimeHostId,
                        host -> host.capabilities().snapshot()));
    }

    public record RegisteredRuntimeHost(
            String runtimeHostId,
            EchoNativeRuntimeHost host,
            EchoRuntimeHostCapabilities capabilities) {
        public RegisteredRuntimeHost {
            runtimeHostId = AdapterContractGuards.requireText(runtimeHostId, "runtime host id");
            if (host == null) {
                throw new IllegalArgumentException("runtime host must not be null");
            }
            if (capabilities == null) {
                throw new IllegalArgumentException("runtime host capabilities must not be null");
            }
        }
    }
}
