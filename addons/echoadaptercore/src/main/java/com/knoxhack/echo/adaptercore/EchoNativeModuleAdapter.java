package com.knoxhack.echo.adaptercore;

import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadContext;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Tiny JDK-only surface contract for the ECHO Native Loader target.
 *
 * <p>Implementations must not require a NeoForge event bus, Minecraft runtime
 * objects, registry mutation, transforms, or addon service execution just to
 * describe their native surfaces. The native bootstrap invokes this contract through
 * the Native Loader lifecycle.
 * ECHO Runtime Standalone implementations should prefer {@link EchoRuntimeModuleAdapter}
 * directly unless they intentionally mirror the Native Loader contract.</p>
 */
public interface EchoNativeModuleAdapter extends EchoRuntimeModuleAdapter, EchoNativeModuleEntrypoint {
    String EVENT_HOST_SERVICE_ID = "echo_native.event_host";
    String LIFECYCLE_HOST_SERVICE_ID = "echo_native.lifecycle_host";
    String REGISTRY_HOST_SERVICE_ID = "echo.native.registry.host";
    String COMMAND_HOST_SERVICE_ID = "echo.native.command_host";
    String CONFIG_HOST_SERVICE_ID = "echo.native.config_host";
    String RESOURCE_HOST_SERVICE_ID = "echo.native.resource_host";
    String NETWORK_HOST_SERVICE_ID = "echo.native.network_host";

    Map<String, Object> describeNativeSurfaces(Map<String, String> context);

    @Override
    default void discover(EchoNativeModuleLoadContext context) {
        context.attribute("nativeEntrypointBridge", "direct_adaptercore_native_module_adapter");
        context.attribute("nativeEntrypointDelegateClass", getClass().getName());
        context.attribute("adaptercoreNativeEntrypoint", true);
        Map<String, Object> callback = onModuleDiscovered(bridgeContext(context));
        context.attribute("adaptercoreNativeLifecycle.onModuleDiscovered", callback);
        recordLifecycleCallback(context, "onModuleDiscovered");
    }

    @Override
    default void registerServices(EchoNativeModuleLoadContext context) {
        Map<String, Object> registerCallback = onRegister(bridgeContext(context));
        context.attribute("adaptercoreNativeLifecycle.onRegister", registerCallback);
        recordLifecycleCallback(context, "onRegister");
        Map<String, Object> activation = activation(context);
        context.registerService(
                "module." + normalized(context.descriptor().id()) + ".native_adapter",
                this,
                "lifecycle",
                "diagnostics",
                "adaptercore"
        );
        if (Boolean.TRUE.equals(activation.get("adapterCoreUsed"))
                || !list(activation.get("registeredFeatureContracts")).isEmpty()) {
            context.registerService(
                    "adaptercore." + normalized(context.descriptor().id()) + ".contract",
                    activation,
                    surfaces(activation, "adaptercore")
            );
        }
        registerDeclaredNativeServices(context, activation);
        registerFeatureContracts(context, activation);
        registerEventHooks(context, activation);
        registerLifecyclePhases(context, activation);
        registerAdapterDomains(context, activation);
        registerRuntimeTargets(context, activation);
        registerNativeProductHosts(context, activation);
        context.recordMutation(
                "lifecycle",
                "direct_adaptercore_native_module_adapter",
                getClass().getName(),
                EchoNativeLoadStatus.REGISTERED
        );
        recordActivationMutations(context, activation);
    }

    @Override
    default void registerContent(EchoNativeModuleLoadContext context) {
        registerRegistryBridge(context, activation(context));
    }

    @Override
    default void commonSetup(EchoNativeModuleLoadContext context) {
        Map<String, Object> callback = onCommonSetup(bridgeContext(context));
        context.attribute("adaptercoreNativeLifecycle.onCommonSetup", callback);
        recordLifecycleCallback(context, "onCommonSetup");
    }

    @Override
    default void clientSetup(EchoNativeModuleLoadContext context) {
        Map<String, Object> callback = onClientSetup(bridgeContext(context));
        context.attribute("adaptercoreNativeLifecycle.onClientSetup", callback);
        recordLifecycleCallback(context, "onClientSetup");
    }

    @Override
    default void serverSetup(EchoNativeModuleLoadContext context) {
        Map<String, Object> worldReady = onWorldReady(bridgeContext(context));
        Map<String, Object> playerReady = onPlayerReady(bridgeContext(context));
        context.attribute("adaptercoreNativeLifecycle.onWorldReady", worldReady);
        context.attribute("adaptercoreNativeLifecycle.onPlayerReady", playerReady);
        recordLifecycleCallback(context, "onWorldReady");
        recordLifecycleCallback(context, "onPlayerReady");
    }

    @Override
    default void ready(EchoNativeModuleLoadContext context) {
        Map<String, Object> resourcesReady = onResourcesReady(bridgeContext(context));
        Map<String, Object> firstTick = onFirstTick(bridgeContext(context));
        context.attribute("adaptercoreNativeLifecycle.onResourcesReady", resourcesReady);
        context.attribute("adaptercoreNativeLifecycle.onFirstTick", firstTick);
        recordLifecycleCallback(context, "onResourcesReady");
        recordLifecycleCallback(context, "onFirstTick");
    }

    @Override
    default void shutdown(EchoNativeModuleLoadContext context) {
        Map<String, Object> callback = onRuntimeShutdown(bridgeContext(context));
        context.attribute("adaptercoreNativeLifecycle.onRuntimeShutdown", callback);
        recordLifecycleCallback(context, "onRuntimeShutdown");
    }

    default Map<String, Object> onModuleDiscovered(Map<String, String> context) {
        return lifecycleCallback("onModuleDiscovered");
    }

    default Map<String, Object> onRegister(Map<String, String> context) {
        return lifecycleCallback("onRegister");
    }

    default Map<String, Object> onCommonSetup(Map<String, String> context) {
        return lifecycleCallback("onCommonSetup");
    }

    default Map<String, Object> onClientSetup(Map<String, String> context) {
        return lifecycleCallback("onClientSetup");
    }

    default Map<String, Object> onResourcesReady(Map<String, String> context) {
        return lifecycleCallback("onResourcesReady");
    }

    default Map<String, Object> onWorldReady(Map<String, String> context) {
        return lifecycleCallback("onWorldReady");
    }

    default Map<String, Object> onPlayerReady(Map<String, String> context) {
        return lifecycleCallback("onPlayerReady");
    }

    default Map<String, Object> onFirstTick(Map<String, String> context) {
        return lifecycleCallback("onFirstTick");
    }

    default Map<String, Object> onRuntimeShutdown(Map<String, String> context) {
        return lifecycleCallback("onRuntimeShutdown");
    }

    @Override
    default Map<String, Object> activateRuntime(Map<String, String> context) {
        return describeNativeSurfaces(context);
    }

    @Override
    default Set<EchoAdapterRuntime> supportedRuntimes() {
        return Set.of(EchoAdapterRuntime.NATIVE_CLIENT, EchoAdapterRuntime.ECHO_NATIVE);
    }

    @Override
    default String adapterContract() {
        return "adaptercore.native_loader_module";
    }

    private static Map<String, Object> lifecycleCallback(String callback) {
        return Map.of(
                "callback", callback,
                "called", true,
                "adapterContract", "adaptercore.native_loader_lifecycle"
        );
    }

    private static void recordLifecycleCallback(EchoNativeModuleLoadContext context, String callback) {
        context.recordMutation(
                "lifecycle",
                "adaptercore_native_lifecycle_callback_executed",
                callback,
                EchoNativeLoadStatus.MUTATED
        );
    }

    private Map<String, Object> activation(EchoNativeModuleLoadContext context) {
        Map<String, Object> existing = object(context.attributes().get("adaptercoreNativeActivation"));
        if (!existing.isEmpty()) {
            return existing;
        }
        Map<String, Object> activation = object(describeNativeSurfaces(bridgeContext(context)));
        context.attribute("adaptercoreNativeActivation", activation);
        context.attribute("adaptercoreNativeActivationStage", string(activation.get("activationStage")));
        context.attribute("adaptercoreNativeAdapterCodeExecuted",
                Boolean.TRUE.equals(activation.get("nativeAdapterCodeExecuted")));
        return activation;
    }

    private static Map<String, String> bridgeContext(EchoNativeModuleLoadContext context) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("moduleId", context.descriptor().id());
        data.put("moduleName", context.descriptor().name());
        data.put("moduleVersion", context.descriptor().version());
        data.put("runtime", "echo_native");
        data.put("loader", "echo-native-loader");
        data.put("packId", string(context.attributes().getOrDefault("packId", "native-loader")));
        data.put("descriptorPath", context.descriptor().descriptorPath() == null
                ? ""
                : context.descriptor().descriptorPath().toString());
        String repoRoot = inferRepoRoot(context.descriptor().descriptorPath());
        if (!repoRoot.isBlank()) {
            data.put("repoRoot", repoRoot);
        }
        for (Map.Entry<String, Object> entry : context.attributes().entrySet()) {
            data.putIfAbsent(entry.getKey(), string(entry.getValue()));
        }
        return data;
    }

    private static void registerFeatureContracts(EchoNativeModuleLoadContext context, Map<String, Object> data) {
        for (Object contract : list(data.get("registeredFeatureContracts"))) {
            String id = string(contract);
            if (!id.isBlank()) {
                context.registerService(
                        "feature." + normalized(context.descriptor().id()) + "." + normalized(id),
                        Map.of("kind", "feature_contract", "id", id),
                        "features",
                        "contracts"
                );
            }
        }
    }

    private static void registerDeclaredNativeServices(EchoNativeModuleLoadContext context, Map<String, Object> data) {
        Map<String, Object> bridge = object(data.get("serviceBridge"));
        for (Map<String, Object> service : objectList(bridge.get("services"))) {
            String id = string(service.get("id"));
            if (id.isBlank()) {
                continue;
            }
            List<String> declaredSurfaces = stringList(service.get("surfaces"));
            context.registerService(
                    id,
                    Map.of("kind", "adaptercore_declared_native_service", "evidence", Map.copyOf(service)),
                    declaredSurfaces.toArray(String[]::new)
            );
        }
    }

    private static void registerEventHooks(EchoNativeModuleLoadContext context, Map<String, Object> data) {
        Map<String, Object> bridge = object(data.get("eventBridge"));
        Object eventHost = nativeHost(context, EVENT_HOST_SERVICE_ID);
        int nativeSubscriptionCount = 0;
        for (Map<String, Object> hook : objectList(bridge.get("hooks"))) {
            String event = string(hook.get("event"));
            String handler = string(hook.get("handler"));
            if (!event.isBlank() && !handler.isBlank()) {
                Map<String, Object> evidence = new LinkedHashMap<>(hook);
                if (subscribeNativeEventHook(context, eventHost, event, handler, hook)) {
                    nativeSubscriptionCount++;
                    evidence.put("nativeEventHostSubscribed", true);
                    context.recordMutation(
                            "events",
                            "adaptercore_native_event_handler_subscribed",
                            event + "#" + handler,
                            EchoNativeLoadStatus.MUTATED
                    );
                } else {
                    evidence.put("nativeEventHostSubscribed", false);
                }
                context.registerService(
                        "event." + normalized(event) + "." + normalized(handler),
                        Map.of("kind", "event_hook", "evidence", Map.copyOf(evidence)),
                        "events",
                        normalized(event)
                );
            }
        }
        if (nativeSubscriptionCount > 0) {
            context.attribute("adaptercoreNativeEventHostSubscriptionCount", nativeSubscriptionCount);
        }
    }

    private static void registerLifecyclePhases(EchoNativeModuleLoadContext context, Map<String, Object> data) {
        Map<String, Object> bridge = object(data.get("lifecycleBridge"));
        Object lifecycleHost = nativeHost(context, LIFECYCLE_HOST_SERVICE_ID);
        int nativeLifecycleRecordCount = 0;
        for (Map<String, Object> phase : objectList(bridge.get("phases"))) {
            String id = string(phase.get("id"));
            if (!id.isBlank()) {
                Map<String, Object> evidence = new LinkedHashMap<>(phase);
                if (recordNativeLifecyclePhase(context, lifecycleHost, id, phase)) {
                    nativeLifecycleRecordCount++;
                    evidence.put("nativeLifecycleHostRecorded", true);
                    context.recordMutation(
                            "lifecycle",
                            "adaptercore_native_lifecycle_phase_recorded",
                            id,
                            EchoNativeLoadStatus.MUTATED
                    );
                } else {
                    evidence.put("nativeLifecycleHostRecorded", false);
                }
                context.registerService(
                        "lifecycle." + normalized(context.descriptor().id()) + "." + normalized(id),
                        Map.of("kind", "lifecycle_phase", "evidence", Map.copyOf(evidence)),
                        "lifecycle"
                );
            }
        }
        if (nativeLifecycleRecordCount > 0) {
            context.attribute("adaptercoreNativeLifecycleHostRecordCount", nativeLifecycleRecordCount);
        }
    }

    private static Object nativeHost(EchoNativeModuleLoadContext context, String serviceId) {
        return context.serviceRegistry()
                .service("echocore", serviceId)
                .or(() -> context.serviceRegistry().service(serviceId))
                .orElse(null);
    }

    private static boolean subscribeNativeEventHook(
            EchoNativeModuleLoadContext context,
            Object eventHost,
            String event,
            String handler,
            Map<String, Object> hook
    ) {
        if (eventHost == null) {
            return false;
        }
        try {
            Method subscribe = eventHost.getClass().getMethod(
                    "subscribeDeclaredHook",
                    String.class,
                    String.class,
                    String.class,
                    Map.class
            );
            subscribe.invoke(eventHost, context.descriptor().id(), event, handler, Map.copyOf(hook));
            return true;
        } catch (ReflectiveOperationException | IllegalArgumentException exception) {
            context.attribute("adaptercoreNativeEventHostSubscriptionError",
                    exception.getClass().getSimpleName() + ": " + exception.getMessage());
            return false;
        }
    }

    private static boolean recordNativeLifecyclePhase(
            EchoNativeModuleLoadContext context,
            Object lifecycleHost,
            String phaseId,
            Map<String, Object> phase
    ) {
        if (lifecycleHost == null) {
            return false;
        }
        try {
            Method record = lifecycleHost.getClass().getMethod(
                    "recordDeclaredLifecyclePhase",
                    String.class,
                    String.class,
                    Map.class
            );
            record.invoke(lifecycleHost, context.descriptor().id(), phaseId, Map.copyOf(phase));
            return true;
        } catch (ReflectiveOperationException | IllegalArgumentException exception) {
            context.attribute("adaptercoreNativeLifecycleHostRecordError",
                    exception.getClass().getSimpleName() + ": " + exception.getMessage());
            return false;
        }
    }

    private static void registerAdapterDomains(EchoNativeModuleLoadContext context, Map<String, Object> data) {
        for (Object domain : list(data.get("adapterDomains"))) {
            String id = string(domain);
            if (!id.isBlank()) {
                context.registerService(
                        "adapter_domain." + normalized(context.descriptor().id()) + "." + normalized(id),
                        Map.of("kind", "adapter_domain", "id", id),
                        "adaptercore",
                        normalized(id)
                );
            }
        }
    }

    private static void registerRuntimeTargets(EchoNativeModuleLoadContext context, Map<String, Object> data) {
        for (Object target : list(data.get("runtimeTargets"))) {
            String id = string(target);
            if (!id.isBlank()) {
                context.registerService(
                        "runtime_target." + normalized(context.descriptor().id()) + "." + normalized(id),
                        Map.of("kind", "runtime_target", "id", id),
                        "runtime"
                );
            }
        }
    }

    private static void registerNativeProductHosts(EchoNativeModuleLoadContext context, Map<String, Object> data) {
        registerDescriptorSurfaceClaims(context, data, COMMAND_HOST_SERVICE_ID, "commands",
                "adaptercore_native_command_host_descriptor_registered");
        registerDescriptorSurfaceClaims(context, data, CONFIG_HOST_SERVICE_ID, "config",
                "adaptercore_native_config_host_descriptor_registered");
        registerDescriptorSurfaceClaims(context, data, RESOURCE_HOST_SERVICE_ID, "resources",
                "adaptercore_native_resource_host_descriptor_registered");
        registerDescriptorSurfaceClaims(context, data, NETWORK_HOST_SERVICE_ID, "network",
                "adaptercore_native_network_host_descriptor_registered");
        registerCommandReports(context, data);
        registerRuntimePacketReports(context, data);
    }

    private static void registerDescriptorSurfaceClaims(
            EchoNativeModuleLoadContext context,
            Map<String, Object> data,
            String hostServiceId,
            String mutationSurface,
            String mutationAction
    ) {
        Object host = nativeHost(context, hostServiceId);
        if (host == null) {
            return;
        }
        int mutationCount = 0;
        for (String claim : surfaceClaims(data)) {
            Map<String, Object> evidence = Map.of(
                    "source", "adaptercore.native_activation_surface_claim",
                    "claim", claim,
                    "moduleId", context.descriptor().id(),
                    "liveMinecraftMutation", false,
                    "minecraftRuntimeAccessed", false
            );
            String status = invokeHostStatus(context, host, "registerDescriptorDomain",
                    new Class<?>[]{String.class, String.class, Map.class},
                    context.descriptor().id(), claim, evidence);
            if ("MUTATED".equals(status)) {
                mutationCount++;
                context.recordMutation(mutationSurface, mutationAction, claim, EchoNativeLoadStatus.MUTATED);
            }
        }
        if (mutationCount > 0) {
            context.attribute("adaptercoreNative" + hostAttributeName(mutationSurface) + "HostDescriptorMutationCount", mutationCount);
        }
    }

    private static void registerCommandReports(EchoNativeModuleLoadContext context, Map<String, Object> data) {
        Object host = nativeHost(context, COMMAND_HOST_SERVICE_ID);
        if (host == null) {
            return;
        }
        int mutationCount = 0;
        for (Map<String, Object> report : bridgeReports(data, "adaptercore.native_command", "commands")) {
            String status = invokeHostStatus(context, host, "registerCommandReport",
                    new Class<?>[]{String.class, Map.class},
                    context.descriptor().id(), report);
            if ("MUTATED".equals(status)) {
                mutationCount++;
                context.recordMutation(
                        "commands",
                        "adaptercore_native_command_host_queued",
                        string(report.get("id")),
                        EchoNativeLoadStatus.MUTATED
                );
                context.registerService(
                        "command_host." + normalized(string(report.get("id"))),
                        Map.of("kind", "native_command_host_report", "id", string(report.get("id"))),
                        "commands",
                        "native_host"
                );
            }
        }
        if (mutationCount > 0) {
            context.attribute("adaptercoreNativeCommandHostQueuedReportCount", mutationCount);
        }
    }

    private static void registerRuntimePacketReports(EchoNativeModuleLoadContext context, Map<String, Object> data) {
        Object host = nativeHost(context, NETWORK_HOST_SERVICE_ID);
        if (host == null) {
            return;
        }
        int mutationCount = 0;
        for (Map<String, Object> report : bridgeReports(data, "adaptercore.native_runtime_packet", "packets")) {
            String status = invokeHostStatus(context, host, "registerPacketReport",
                    new Class<?>[]{String.class, Map.class},
                    context.descriptor().id(), report);
            if ("MUTATED".equals(status)) {
                mutationCount++;
                context.recordMutation(
                        "network",
                        "adaptercore_native_network_host_packet_bound",
                        string(report.get("id")),
                        EchoNativeLoadStatus.MUTATED
                );
                context.registerService(
                        "network_host." + normalized(string(report.get("id"))),
                        Map.of("kind", "native_network_host_packet_report", "id", string(report.get("id"))),
                        "network",
                        "packets",
                        "native_host"
                );
            }
        }
        if (mutationCount > 0) {
            context.attribute("adaptercoreNativeNetworkHostPacketReportCount", mutationCount);
        }
    }

    private static List<String> surfaceClaims(Map<String, Object> data) {
        List<String> claims = new ArrayList<>();
        for (Object domain : list(data.get("adapterDomains"))) {
            addClaim(claims, string(domain));
        }
        Map<String, Object> serviceBridge = object(data.get("serviceBridge"));
        for (Map<String, Object> service : objectList(serviceBridge.get("services"))) {
            for (Object surface : list(service.get("surfaces"))) {
                addClaim(claims, string(surface));
            }
            for (Object feature : list(service.get("features"))) {
                addClaim(claims, string(feature));
            }
        }
        return List.copyOf(claims);
    }

    private static void addClaim(List<String> claims, String claim) {
        if (claim == null || claim.isBlank() || claims.contains(claim)) {
            return;
        }
        claims.add(claim);
    }

    private static List<Map<String, Object>> bridgeReports(Map<String, Object> data, String bridgeId, String listKey) {
        List<Map<String, Object>> reports = new ArrayList<>();
        collectBridgeReports(data, bridgeId, listKey, reports, new IdentityHashMap<>(), 0);
        return List.copyOf(reports);
    }

    private static void collectBridgeReports(
            Object value,
            String bridgeId,
            String listKey,
            List<Map<String, Object>> reports,
            IdentityHashMap<Object, Boolean> visited,
            int depth
    ) {
        if (value == null || depth > 32) {
            return;
        }
        if (value instanceof Map<?, ?> map) {
            if (visited.put(map, Boolean.TRUE) != null) {
                return;
            }
            Map<String, Object> object = object(map);
            if (bridgeId.equals(string(object.get("bridge"))) && object.get(listKey) instanceof List<?>) {
                reports.add(object);
            }
            for (Object child : object.values()) {
                collectBridgeReports(child, bridgeId, listKey, reports, visited, depth + 1);
            }
            return;
        }
        if (value instanceof List<?> list) {
            if (visited.put(list, Boolean.TRUE) != null) {
                return;
            }
            for (Object item : list) {
                collectBridgeReports(item, bridgeId, listKey, reports, visited, depth + 1);
            }
        }
    }

    private static String invokeHostStatus(
            EchoNativeModuleLoadContext context,
            Object host,
            String methodName,
            Class<?>[] parameterTypes,
            Object... args
    ) {
        try {
            Method method = host.getClass().getMethod(methodName, parameterTypes);
            Object status = method.invoke(host, args);
            return string(status);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            context.attribute("adaptercoreNativeHostProjectionError",
                    methodName + " " + exception.getClass().getSimpleName() + ": " + exception.getMessage());
            return "FAILED";
        }
    }

    private static String hostAttributeName(String surface) {
        if (surface == null || surface.isBlank()) {
            return "Surface";
        }
        return Character.toUpperCase(surface.charAt(0)) + surface.substring(1);
    }

    private static void registerRegistryBridge(EchoNativeModuleLoadContext context, Map<String, Object> data) {
        Map<String, Object> bridge = object(data.get("registryBridge"));
        Object registryHost = nativeHost(context, REGISTRY_HOST_SERVICE_ID);
        Object resourceHost = nativeHost(context, RESOURCE_HOST_SERVICE_ID);
        int nativeRegistryMutationCount = 0;
        int nativeResourceHostRegistryCount = 0;
        for (Map<String, Object> registration : objectList(bridge.get("registrations"))) {
            String registry = string(registration.get("registry"));
            String id = string(registration.get("id"));
            if (registry.isBlank() || id.isBlank()) {
                continue;
            }
            Map<String, Object> evidence = new LinkedHashMap<>(registration);
            String nativeRegistryStatus = registerNativeRegistry(context, registryHost, registry, id, registration);
            evidence.put("nativeRegistryHostStatus", nativeRegistryStatus);
            boolean nativeRegistryMutated = "MUTATED".equals(nativeRegistryStatus);
            evidence.put("nativeRegistryHostMutated", nativeRegistryMutated);
            if (nativeRegistryMutated) {
                nativeRegistryMutationCount++;
                context.recordMutation(
                        "registry",
                        "adaptercore_native_registry_host_registered",
                        registry + ":" + id,
                        EchoNativeLoadStatus.MUTATED
                );
            }
            EchoNativeLoadStatus nativeResourceStatus = registerNativeResourceRegistration(
                    context,
                    resourceHost,
                    registry,
                    id,
                    registration
            );
            if (nativeResourceStatus != null && nativeResourceStatus != EchoNativeLoadStatus.UNSUPPORTED) {
                evidence.put("nativeResourceHostStatus", nativeResourceStatus.name());
                evidence.put("nativeResourceHostMounted", nativeResourceStatus == EchoNativeLoadStatus.MUTATED
                        || nativeResourceStatus == EchoNativeLoadStatus.RESOLVED);
                if (nativeResourceStatus == EchoNativeLoadStatus.MUTATED) {
                    nativeResourceHostRegistryCount++;
                    context.recordMutation(
                            "resources",
                            "adaptercore_native_resource_host_mounted",
                            registry + ":" + id,
                            EchoNativeLoadStatus.MUTATED
                    );
                }
                context.registerService(
                        "resource_host." + normalized(context.descriptor().id()) + "." + normalized(id),
                        Map.of("kind", "resource_host_registration", "evidence", Map.copyOf(evidence)),
                        "resources",
                        "native_host"
                );
            }
            String prefix = "service".equals(registry) ? "service." : "content." + normalized(registry) + ".";
            context.registerService(
                    prefix + normalized(id),
                    Map.of("kind", "registry_registration", "evidence", Map.copyOf(evidence)),
                    "registry",
                    normalized(registry)
            );
            if (mountClientUiSurface(context, registry, id, evidence)) {
                context.recordMutation(
                        "ui",
                        "adaptercore_native_client_ui_surface_registered",
                        id,
                        EchoNativeLoadStatus.MUTATED
                );
            }
        }
        if (nativeRegistryMutationCount > 0) {
            context.attribute("adaptercoreNativeRegistryHostMutationCount", nativeRegistryMutationCount);
        }
        if (nativeResourceHostRegistryCount > 0) {
            context.attribute("adaptercoreNativeResourceHostRegistryMountedCount", nativeResourceHostRegistryCount);
        }
    }

    private static String registerNativeRegistry(
            EchoNativeModuleLoadContext context,
            Object registryHost,
            String registry,
            String id,
            Map<String, Object> registration
    ) {
        if (registryHost == null) {
            return "UNAVAILABLE";
        }
        try {
            Method registerDeclared = registryHost.getClass().getMethod(
                    "registerDeclared",
                    String.class,
                    String.class,
                    String.class,
                    Map.class
            );
            Object status = registerDeclared.invoke(
                    registryHost,
                    context.descriptor().id(),
                    registry,
                    id,
                    Map.copyOf(registration)
            );
            return status == null ? "" : String.valueOf(status);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            context.attribute("adaptercoreNativeRegistryHostRegistrationError",
                    exception.getClass().getSimpleName() + ": " + exception.getMessage());
            return "FAILED";
        }
    }

    private static EchoNativeLoadStatus registerNativeResourceRegistration(
            EchoNativeModuleLoadContext context,
            Object resourceHost,
            String registry,
            String id,
            Map<String, Object> registration
    ) {
        if (!isResourceRegistry(registry)) {
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        if (resourceHost == null) {
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        Map<String, Object> evidence = new LinkedHashMap<>(registration);
        evidence.put("source", "adaptercore.registryBridge.resource");
        evidence.put("moduleId", context.descriptor().id());
        evidence.put("resourceRegistry", registry);
        try {
            Method register = resourceHost.getClass().getMethod(
                    "registerResource",
                    String.class,
                    String.class,
                    String.class,
                    Map.class
            );
            Object status = register.invoke(
                    resourceHost,
                    context.descriptor().id(),
                    id,
                    resourceSurfaceType(registry),
                    Map.copyOf(evidence)
            );
            return loadStatus(status);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            context.attribute("adaptercoreNativeResourceHostRegistrationError",
                    exception.getClass().getSimpleName() + ": " + exception.getMessage());
            return EchoNativeLoadStatus.FAILED;
        }
    }

    private static EchoNativeLoadStatus loadStatus(Object status) {
        if (status instanceof EchoNativeLoadStatus loadStatus) {
            return loadStatus;
        }
        String text = string(status);
        if (text.isBlank()) {
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        try {
            return EchoNativeLoadStatus.valueOf(text);
        } catch (IllegalArgumentException exception) {
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
    }

    private static boolean isResourceRegistry(String registry) {
        String id = normalized(registry);
        return id.equals("resource")
                || id.equals("resources")
                || id.equals("resource.profile")
                || id.equals("resource.pack")
                || id.equals("resourcepack")
                || id.equals("data")
                || id.equals("data.pack")
                || id.equals("datapack")
                || id.equals("recipe")
                || id.equals("recipes")
                || id.equals("loot")
                || id.equals("loot.table")
                || id.equals("loot.tables")
                || id.equals("loottables")
                || id.equals("tag")
                || id.equals("tags")
                || id.equals("sound")
                || id.equals("sounds")
                || id.equals("structure")
                || id.equals("structures")
                || id.equals("worldgen")
                || id.equals("world.generator")
                || id.equals("world.preset")
                || id.equals("world.template")
                || id.equals("asset")
                || id.equals("assets")
                || id.equals("ui.screen")
                || id.equals("ui.screens")
                || id.equals("theme")
                || id.equals("themes")
                || id.equals("theme.tokens")
                || id.equals("ui.skin")
                || id.equals("ui.skins")
                || id.equals("render.profile")
                || id.equals("render.profiles")
                || id.equals("asset.kit")
                || id.equals("asset.kits")
                || id.equals("block.palette")
                || id.equals("block.palettes")
                || id.equals("screen.markup")
                || id.equals("screen.layout")
                || id.equals("screen.layouts")
                || id.equals("style")
                || id.equals("styles")
                || id.equals("data.provider")
                || id.equals("data.providers")
                || id.endsWith(".resource")
                || id.endsWith(".resources")
                || id.endsWith(".data");
    }

    private static boolean mountClientUiSurface(
            EchoNativeModuleLoadContext context,
            String registry,
            String id,
            Map<String, Object> registration
    ) {
        String surfaceType = clientUiSurfaceType(registry);
        if (surfaceType.isBlank()) {
            return false;
        }
        Object uiHost = context.serviceRegistry()
                .service("echocore", "echo.native.client_ui_host")
                .or(() -> context.serviceRegistry().service("echo.native.client_ui_host"))
                .orElse(null);
        if (uiHost == null) {
            context.attribute("adaptercoreNativeClientUiHostMountSkipped", "echo.native.client_ui_host service not available");
            return false;
        }
        try {
            Method registerSurfaceStatus = uiHost.getClass().getMethod(
                    "registerSurfaceStatus",
                    String.class,
                    String.class,
                    String.class,
                    Map.class
            );
            Object status = registerSurfaceStatus.invoke(
                    uiHost,
                    context.descriptor().id(),
                    id,
                    surfaceType,
                    Map.copyOf(registration)
            );
            String statusText = status == null ? "" : String.valueOf(status);
            context.attribute("adaptercoreNativeClientUiHostMountStatus", statusText);
            if (isUiMutationStatus(statusText)) {
                context.attribute("adaptercoreNativeClientUiHostMounted", true);
                return true;
            }
            context.attribute("adaptercoreNativeClientUiHostMountSkipped",
                    "echo.native.client_ui_host returned " + statusText);
            return false;
        } catch (NoSuchMethodException ignored) {
            // Older host implementations expose only the void registration method.
        } catch (ReflectiveOperationException | RuntimeException exception) {
            context.attribute("adaptercoreNativeClientUiHostMountSkipped",
                    exception.getClass().getSimpleName() + ": " + exception.getMessage());
            return false;
        }
        try {
            Method registerSurface = uiHost.getClass().getMethod(
                    "registerSurface",
                    String.class,
                    String.class,
                    String.class,
                    Map.class
            );
            registerSurface.invoke(
                    uiHost,
                    context.descriptor().id(),
                    id,
                    surfaceType,
                    Map.copyOf(registration)
            );
            context.attribute("adaptercoreNativeClientUiHostMounted", true);
            context.attribute("adaptercoreNativeClientUiHostMountStatus", "LEGACY_VOID_REGISTER_SURFACE");
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            context.attribute("adaptercoreNativeClientUiHostMountSkipped",
                    exception.getClass().getSimpleName() + ": " + exception.getMessage());
            return false;
        }
    }

    private static boolean isClientUiRegistry(String registry) {
        return !clientUiSurfaceType(registry).isBlank();
    }

    private static String clientUiSurfaceType(String registry) {
        String id = normalized(registry);
        return switch (id) {
            case "ui.surface", "ui.surfaces", "ui" -> "ui_surface";
            case "ui.overlay", "ui.overlays", "overlay", "overlays" -> "ui_overlay";
            case "client.overlay", "client.overlays" -> "client_overlay";
            case "hud", "huds" -> "hud";
            case "hud.widget", "hud.widgets" -> "hud_widget";
            case "hud.layout", "hud.layouts" -> "hud_layout";
            case "screen", "screens" -> "screen";
            case "screen.surface", "screen.surfaces", "screen.host", "screen.hosts" -> "screen_surface";
            case "loading.screen", "loading.screens", "loading", "load.screen" -> "loading_screen";
            case "main.menu", "main.menus", "mainmenu", "mainmenus" -> "main_menu";
            case "terminal", "eui" -> "terminal";
            case "index", "recipe.index", "recipe.browser", "inventory.overlay" -> "index";
            case "lens", "scanner.lens", "field.lens" -> "lens";
            case "holomap", "holo.map", "map", "fullscreen.map", "mini.map", "minimap" -> "holomap";
            case "theme.surface", "theme.overlay", "theme.ui" -> "theme";
            default -> "";
        };
    }

    private static String resourceSurfaceType(String registry) {
        String id = normalized(registry);
        return switch (id) {
            case "resource.pack", "resourcepack", "resourcepacks" -> "resource_pack";
            case "data.pack", "datapack", "datapacks" -> "data_pack";
            case "loot", "loottables" -> "loot_table";
            case "recipes" -> "recipe";
            case "tags" -> "tag";
            case "sounds" -> "sound";
            case "structures" -> "structure";
            case "assets" -> "asset";
            case "ui.screen", "ui.screens" -> "ui_screen";
            case "world.generator" -> "worldgen";
            case "world.preset" -> "world_preset";
            case "world.template" -> "world_template";
            case "theme.tokens" -> "theme_tokens";
            case "ui.skin", "ui.skins" -> "ui_skin";
            case "render.profile", "render.profiles" -> "render_profile";
            case "asset.kit", "asset.kits" -> "asset_kit";
            case "block.palette", "block.palettes" -> "block_palette";
            case "screen.markup" -> "screen_markup";
            case "screen.layout", "screen.layouts" -> "screen_layout";
            default -> id.replace('.', '_');
        };
    }

    private static boolean isUiMutationStatus(String status) {
        return "MUTATED".equals(status);
    }

    private static void recordActivationMutations(EchoNativeModuleLoadContext context, Map<String, Object> data) {
        if (Boolean.TRUE.equals(data.get("registryMutated"))) {
            context.recordMutation(
                    "registry",
                    "direct_registry_mutated",
                    context.descriptor().id(),
                    EchoNativeLoadStatus.MUTATED);
        }
        if (Boolean.TRUE.equals(data.get("transformsPerformed"))) {
            context.recordMutation(
                    "transform",
                    "direct_transforms_performed",
                    context.descriptor().id(),
                    EchoNativeLoadStatus.MUTATED);
        }
        if (Boolean.TRUE.equals(data.get("serviceCodeExecuted"))) {
            context.recordMutation(
                    "service",
                    "direct_service_code_executed",
                    context.descriptor().id(),
                    EchoNativeLoadStatus.MUTATED);
        }
        if (Boolean.TRUE.equals(data.get("nativeAdapterCodeExecuted"))) {
            context.recordMutation(
                    "adapter",
                    "direct_native_adapter_code_executed",
                    context.descriptor().id(),
                    EchoNativeLoadStatus.MUTATED);
        }
    }

    private static String inferRepoRoot(Path descriptorPath) {
        if (descriptorPath == null) {
            return "";
        }
        Path current = descriptorPath.toAbsolutePath().normalize();
        while (current != null) {
            if (Files.exists(current.resolve("settings.gradle")) && Files.isDirectory(current.resolve("echo-native-platform"))) {
                return current.toString();
            }
            current = current.getParent();
        }
        return "";
    }

    private static String[] surfaces(Map<String, Object> data, String fallback) {
        List<String> result = new ArrayList<>();
        result.add(fallback);
        for (Object domain : list(data.get("adapterDomains"))) {
            String value = normalized(string(domain));
            if (!value.isBlank() && !result.contains(value)) {
                result.add(value);
            }
        }
        return result.toArray(String[]::new);
    }

    private static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return result;
    }

    private static List<Object> list(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return List.copyOf(list);
    }

    private static List<Map<String, Object>> objectList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> object = object(item);
            if (!object.isEmpty()) {
                result.add(object);
            }
        }
        return List.copyOf(result);
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            String text = string(item);
            if (!text.isBlank()) {
                result.add(text);
            }
        }
        return List.copyOf(result);
    }

    private static String normalized(String value) {
        String text = value == null ? "" : value.toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder();
        boolean previousSeparator = false;
        for (int index = 0; index < text.length(); index++) {
            char ch = text.charAt(index);
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) {
                result.append(ch);
                previousSeparator = false;
            } else if (!previousSeparator) {
                result.append('.');
                previousSeparator = true;
            }
        }
        while (result.length() > 0 && result.charAt(result.length() - 1) == '.') {
            result.deleteCharAt(result.length() - 1);
        }
        return result.toString();
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
