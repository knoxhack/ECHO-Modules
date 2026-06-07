package com.knoxhack.echoterminal;

import com.knoxhack.echo.adaptercore.EchoNativeEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeModuleAdapter;
import com.knoxhack.echo.adaptercore.EchoNativeRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimePacketConsumerBridge;
import dev.echo.nativeplatform.contracts.EchoNativeActivationSurfaceRegistrar;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoTerminalNativeModule implements EchoNativeModuleAdapter, EchoNativeModuleEntrypoint {
    @Override
    public void discover(EchoNativeModuleLoadContext context) {
        context.attribute("nativeEntrypointBridge", "direct_native_module_entrypoint");
        context.attribute("nativeEntrypointClass", getClass().getName());
        context.attribute("nativeModuleEntrypoint", true);
    }

    @Override
    public void registerServices(EchoNativeModuleLoadContext context) {
        EchoNativeActivationSurfaceRegistrar.registerServices(
                context,
                this,
                activation(context),
                "native_module_entrypoint",
                "direct_native_module_entrypoint"
        );
    }

    @Override
    public void registerContent(EchoNativeModuleLoadContext context) {
        EchoNativeActivationSurfaceRegistrar.registerContent(context, activation(context));
    }

    @Override
    public void ready(EchoNativeModuleLoadContext context) {
        registerNativeClientRoutesFromModule(context);
        EchoNativeActivationSurfaceRegistrar.ready(context);
    }

    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        EchoNativeLifecycleBridge lifecycle = new EchoNativeLifecycleBridge(MODULE_ID)
                .phase("discover", "Discover Terminal EUI, command, diagnostic, and pack dashboard contracts.")
                .phase("register_terminal_surface", "Record terminal surfaces, menu, and service contracts before native UI execution.")
                .phase("attach_terminal_events", "Record network, command, screen, and reload hooks for the native event bridge.")
                .phase("ready", "Expose Terminal as the native Ashfall dashboard surface.");
        EchoNativeRegistryBridge registry = new EchoNativeRegistryBridge(MODULE_ID)
                .register("block", "echoterminal:terminal", "Terminal block contract.")
                .register("item", "echoterminal:terminal_remote", "Terminal remote item contract.")
                .register("creative_tab", "echoterminal:terminal_tab", "Terminal native module creative tab contract.")
                .register("menu", "echoterminal:terminal", "Terminal menu contract.")
                .register("terminal", "echoterminal:eui", "Terminal EUI surface contract.", Map.of(
                        "nativeSurfaceImplementationClass", "com.knoxhack.echoterminal.client.screen.EchoTerminalScreen",
                        "nativeScreenBridgeClass", "com.knoxhack.echoterminal.client.screen.EchoTerminalScreens",
                        "actions", terminalNativeActions()))
                .register("client_overlay", "echoterminal:hud_overlay", "Terminal mission HUD and discovery toast overlay contract.", Map.of(
                        "nativeSurfaceImplementationClass", "com.knoxhack.echoterminal.client.mission.TerminalMissionHudController",
                        "nativeScreenBridgeClass", "com.knoxhack.echoterminal.client.discovery.DiscoveryToastHud",
                        "actions", terminalOverlayActions()))
                .register("service", "echoterminal:terminal_service", "Terminal dashboard service contract.");
        EchoNativeEventBridge events = new EchoNativeEventBridge(MODULE_ID)
                .hook("common.setup", "EchoTerminal.commonSetup", "Attach terminal services and optional integrations.")
                .hook("network.payload_register", "TerminalPayloads.register", "Prepare terminal packet contracts.")
                .hook("commands.register", "TerminalCommands.register", "Expose terminal commands when native command bridge exists.")
                .hook("screen.open", "TerminalScreenBridge.open", "Prepare terminal screen open flow.")
                .hook("data.reload", "TerminalContentReloaders.register", "Attach terminal content reloaders.");
        Map<String, Object> dashboardSurface = EchoTerminalDashboardContract.executeReferenceCommand(
                EchoTerminalDashboardContract.REFERENCE_COMMAND);
        boolean dashboardOpened = EchoTerminalDashboardContract.referenceCommandPassed(dashboardSurface);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("activationStage", "terminal_native_dashboard_surface_active");
        result.put("adapterCoreUsed", true);
        result.put("nativeAdapterCodeExecuted", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context.getOrDefault("packId", "unknown"));
        result.put("lifecycleBridge", lifecycle.describe());
        result.put("registryBridge", registry.describe());
        result.put("eventBridge", events.describe());
        result.put("logicalRegistrationCount", 7);
        result.put("eventHookCount", 5);
        result.put("registeredFeatureContracts", List.of(
                "terminal.surface",
                EchoTerminalDashboardContract.ADAPTERCORE_CONTRACT_ID
        ));
        result.put("dashboardSurface", dashboardSurface);
        result.put("dashboardSurfaceExecuted", dashboardOpened);
        result.put("requiresUiBridge", true);
        result.put("requiresRegistryBridge", true);
        result.put("requiresLifecycleBridge", true);
        result.put("requiresEventBridge", true);
        result.put("registryInjected", true);
        result.put("registryMutated", true);
        result.put("nativeClientRouteRegistrarClass", "com.knoxhack.echoterminal.EchoTerminalClient");
        result.put("serviceCodeExecuted", dashboardOpened);
        result.put("transformsPerformed", false);
        result.put("nativeProjectionPerformed", true);
        result.put("nativeProjectionMode", "terminal_surface_projection");
        result.put("summary", "Terminal native contract registered Ashfall dashboard surfaces and executed the AdapterCore dashboard service.");
        return result;
    }

    private static void registerNativeClientRoutesFromModule(EchoNativeModuleLoadContext context) {
        try {
            Object registered = Class.forName("com.knoxhack.echoterminal.EchoTerminalClient")
                    .getMethod("ensureNativeClientRoutesRegisteredForNativeLoader")
                    .invoke(null);
            boolean mutated = Boolean.TRUE.equals(registered);
            context.attribute("nativeClientRouteRegistrarClass", "com.knoxhack.echoterminal.EchoTerminalClient");
            context.attribute("nativeClientRoutesRegistered", mutated);
            if (mutated) {
                context.recordMutation("client_routes", "register", "echoterminal:native_client_routes",
                        EchoNativeLoadStatus.MUTATED);
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            context.attribute("nativeClientRoutesRegistered", false);
            context.attribute("nativeClientRouteRegistrarFailure", exception.getClass().getSimpleName());
        }
    }

    public Map<String, Object> consumeAshfallRuntimePackets(Map<String, Object> runtimePacketBindings) {
        return new EchoNativeRuntimePacketConsumerBridge(MODULE_ID).consume(
                "echoterminal:ashfall_runtime_packet_consumers",
                runtimePacketBindings,
                List.of("echoterminal:first_ten_minutes_card"));
    }

    private Map<String, Object> activation(EchoNativeModuleLoadContext context) {
        return EchoNativeActivationSurfaceRegistrar.activation(
                context,
                () -> describeNativeSurfaces(EchoNativeActivationSurfaceRegistrar.bridgeContext(context))
        );
    }

    private static Map<String, Object> terminalNativeActions() {
        return Map.ofEntries(
                Map.entry("terminal.open", Map.of(
                        "kind", "terminal_screen",
                        "fallbackScreenClass", "com.knoxhack.echoterminal.client.screen.EchoTerminalScreen")),
                Map.entry("terminal.command_deck", terminalTabAction("overview", "Command Deck")),
                Map.entry("terminal.survival_route", terminalTabAction("survival_route", "Survival Route")),
                Map.entry("terminal.mission_graph", terminalTabAction("mission_graph", "Route Sources")),
                Map.entry("terminal.route_records", terminalTabAction("route_records", "Route Records")),
                Map.entry("terminal.discovery_grid", terminalTabAction("discovery_grid", "Discovery Grid")),
                Map.entry("terminal.factions", terminalTabAction("faction_atlas", "Factions")),
                Map.entry("terminal.recipe_index", terminalTabAction("recipe_index", "Recipe Index")),
                Map.entry("terminal.archives", terminalTabAction("archives", "Field Archive")),
                Map.entry("terminal.vitals", terminalTabAction("vitals", "Vitals")),
                Map.entry("terminal.rewards", terminalTabAction("reward_inbox", "Reward Inbox")),
                Map.entry("terminal.data_core", terminalTabAction("data_core", "Data Core")),
                Map.entry("terminal.settings", terminalTabAction("settings", "Interface Settings")),
                Map.entry("signalos.terminal", Map.of(
                        "kind", "terminal_screen",
                        "bridgeMethod", "create",
                        "command", "open:signalos_dashboard",
                        "aliasSurface", "signalos",
                        "liveSessionBridge", "echo-terminal-native-session")),
                Map.entry("terminal.screen.char_typed", Map.of(
                        "kind", "terminal_screen_input",
                        "inputType", "char_typed",
                        "liveSessionBridge", "echo-terminal-native-session")),
                Map.entry("terminal.screen.mouse_scroll", Map.of(
                        "kind", "terminal_screen_input",
                        "inputType", "mouse_scroll",
                        "liveSessionBridge", "echo-terminal-native-session")),
                Map.entry("terminal.screen.frame.render", Map.of(
                        "kind", "terminal_screen_frame_render",
                        "renderer", "echorendercore",
                        "liveSessionBridge", "echo-terminal-native-session")),
                Map.entry("terminal.screencore.mouse", Map.of(
                        "kind", "terminal_screencore_mouse_input",
                        "inputType", "mouse",
                        "screenBridge", "echoscreencore",
                        "liveSessionBridge", "echo-terminal-native-session")),
                Map.entry("terminal.screencore.scroll", Map.of(
                        "kind", "terminal_screencore_scroll_input",
                        "inputType", "mouse_scroll",
                        "screenBridge", "echoscreencore",
                        "liveSessionBridge", "echo-terminal-native-session")),
                Map.entry("terminal.screencore.key", Map.of(
                        "kind", "terminal_screencore_key_input",
                        "inputType", "key_pressed",
                        "screenBridge", "echoscreencore",
                        "liveSessionBridge", "echo-terminal-native-session")),
                Map.entry("terminal.screencore.char", Map.of(
                        "kind", "terminal_screencore_char_input",
                        "inputType", "char_typed",
                        "screenBridge", "echoscreencore",
                        "liveSessionBridge", "echo-terminal-native-session")),
                Map.entry("terminal.screencore.action", Map.of(
                        "kind", "terminal_screencore_action",
                        "screenBridge", "echoscreencore",
                        "actionCatalog", "TerminalScreenCoreActionIds",
                        "liveSessionBridge", "echo-terminal-native-session")));
    }

    private static Map<String, Object> terminalOverlayActions() {
        return Map.of(
                "terminal.mission_hud.tick", Map.of("kind", "terminal_overlay_tick", "overlay", "mission_hud"),
                "terminal.discovery_toast.tick", Map.of("kind", "terminal_overlay_tick", "overlay", "discovery_toast"),
                "terminal.mission_hud.render", Map.of("kind", "terminal_overlay_render", "overlay", "mission_hud"),
                "terminal.discovery_toast.render", Map.of("kind", "terminal_overlay_render", "overlay", "discovery_toast"));
    }

    private static Map<String, Object> terminalTabAction(String tabPath, String label) {
        return Map.of(
                "kind", "terminal_tab",
                "tabId", MODULE_ID + ":" + tabPath,
                "label", label,
                "bridgeMethod", "openTab",
                "command", "open:" + tabPath,
                "liveSessionBridge", "echo-terminal-native-session");
    }

    private static final String MODULE_ID = "echoterminal";
}
