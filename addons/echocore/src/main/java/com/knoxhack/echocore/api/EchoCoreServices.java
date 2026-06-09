package com.knoxhack.echocore.api;

import com.knoxhack.echocore.EchoCore;
import com.knoxhack.echocore.api.config.EchoConfigRegistry;
import com.knoxhack.echocore.api.diagnostic.EchoDiagnosticService;
import com.knoxhack.echocore.api.mission.IMissionRegistry;
import com.knoxhack.echocore.api.mission.IMissionService;
import com.knoxhack.echocore.api.mission.InMemoryMissionRegistry;
import com.knoxhack.echocore.api.mission.InMemoryMissionService;
import com.knoxhack.echocore.api.network.INetworkService;
import com.knoxhack.echocore.api.network.NoOpNetworkService;
import com.knoxhack.echocore.api.spine.EchoSpineBus;
import com.knoxhack.echocore.api.spine.InMemoryEchoSpineBus;

public final class EchoCoreServices {
    private static final EchoServiceRegistry REGISTRY = new EchoServiceRegistry();
    private static final EchoRuntimeModules RUNTIME_MODULES = new EchoRuntimeModules();

    static {
        bootstrapDefaults();
    }

    private EchoCoreServices() {
    }

    public static EchoServiceRegistry registry() {
        return REGISTRY;
    }

    public static EchoRuntimeModules runtimeModules() {
        return RUNTIME_MODULES;
    }

    public static EchoSpineBus spineBus() {
        return REGISTRY.require(EchoSpineBus.class);
    }

    public static EchoConfigRegistry configRegistry() {
        return REGISTRY.require(EchoConfigRegistry.class);
    }

    public static IMissionRegistry missionRegistry() {
        return REGISTRY.require(IMissionRegistry.class);
    }

    public static IMissionService missionService() {
        return REGISTRY.require(IMissionService.class);
    }

    public static INetworkService networkService() {
        return REGISTRY.require(INetworkService.class);
    }

    public static EchoDiagnosticService diagnostics() {
        return REGISTRY.require(EchoDiagnosticService.class);
    }

    public static synchronized void bootstrapDefaults() {
        if (!RUNTIME_MODULES.isLoaded(EchoCore.MOD_ID)) {
            RUNTIME_MODULES.register(new EchoRuntimeModules.EchoRuntimeModule(EchoCore.MOD_ID, EchoCore.VERSION, "common", true));
        }
        REGISTRY.register(EchoRuntimeModules.class, RUNTIME_MODULES);
        REGISTRY.register(EchoSpineBus.class, new InMemoryEchoSpineBus());
        REGISTRY.register(EchoConfigRegistry.class, new EchoConfigRegistry());
        InMemoryMissionRegistry missions = new InMemoryMissionRegistry();
        REGISTRY.register(IMissionRegistry.class, missions);
        REGISTRY.register(IMissionService.class, new InMemoryMissionService(missions));
        REGISTRY.register(INetworkService.class, new NoOpNetworkService());
        REGISTRY.register(EchoDiagnosticService.class, new EchoDiagnosticService());
    }
}
