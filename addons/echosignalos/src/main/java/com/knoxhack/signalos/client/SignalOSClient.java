package com.knoxhack.signalos.client;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echonetcore.client.EchoNetClientActions;
import com.knoxhack.signalos.SignalOS;
import com.knoxhack.signalos.network.SignalOsOpenTerminalPacket;
import com.knoxhack.signalos.platform.SignalOsModuleAccess;
import com.knoxhack.signalos.registry.ModMenus;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import org.lwjgl.glfw.GLFW;

public class SignalOSClient {
    private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
            net.minecraft.resources.Identifier.fromNamespaceAndPath(SignalOS.MODID, "terminal"));

    public static final KeyMapping OPEN_TERMINAL_KEY = new KeyMapping(
            "key.signalos.open",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            KEY_CATEGORY);

    public SignalOSClient(Object container) {
        EchoBackendLifecycleBridge.registerGameEventHandler(SignalOSClient::onKeyInput);
        EchoBackendLifecycleBridge.registerGameEventHandler(SignalOSClient::onCharacterTyped);
        EchoBackendLifecycleBridge.registerModListener(container, ClientModEvents::onRegisterKeyMappings);
        EchoBackendLifecycleBridge.registerModListener(container, ClientModEvents::onRegisterMenuScreens);
        EchoBackendLifecycleBridge.registerModListener(container, ClientModEvents::onRegisterRenderers);
        SignalNetClientRenderers.register();
        if (SignalOsModuleAccess.isLoaded("echorendercore")) {
            registerRenderCoreScreenIntegration();
        }
    }

    private static void onKeyInput(Object event) {
        if (!EchoBackendClientBridge.keyActionEquals(event, GLFW.GLFW_PRESS)
                || !EchoBackendClientBridge.keyMappingMatches(OPEN_TERMINAL_KEY, event)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        if (minecraft.screen == null) {
            EchoNetClientActions.sendServerboundAction(new SignalOsOpenTerminalPacket());
        } else if (minecraft.screen instanceof SignalOsTerminalScreen) {
            minecraft.setScreen(null);
        }
    }

    private static void onCharacterTyped(Object event) {
        Object screen = invokeNoArg(event, "getScreen");
        Object characterEvent = invokeNoArg(event, "getCharacterEvent");
        if (!(characterEvent instanceof CharacterEvent typed)) {
            return;
        }
        if (screen instanceof SignalOsTerminalScreen terminalScreen && terminalScreen.handleCharTyped(typed)) {
            setCanceled(event);
        } else if (screen instanceof SignalOsServerRackScreen rackScreen
                && rackScreen.handleCharTyped(typed)) {
            setCanceled(event);
        }
    }

    private static void registerRenderCoreScreenIntegration() {
        try {
            Class.forName("com.knoxhack.signalos.integration.SignalOsRenderCoreClientIntegration")
                    .getMethod("registerScreenVisuals")
                    .invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            SignalOS.LOGGER.warn("SignalOS RenderCore screen integration could not be registered.", exception);
        }
    }

    private static void registerRenderCoreBlockRenderers(Object event) {
        if (!SignalOsModuleAccess.isLoaded("echorendercore")) {
            return;
        }
        try {
            Class.forName("com.knoxhack.signalos.integration.SignalOsRenderCoreClientIntegration")
                    .getMethod("registerBlockRenderers", Object.class)
                    .invoke(null, event);
        } catch (ReflectiveOperationException | LinkageError exception) {
            SignalOS.LOGGER.warn("SignalOS RenderCore block integration could not be registered.", exception);
        }
    }

    public static class ClientModEvents {
        static void onRegisterKeyMappings(Object event) {
            EchoBackendClientBridge.registerKeyCategory(event, KEY_CATEGORY);
            EchoBackendClientBridge.registerKeyMapping(event, OPEN_TERMINAL_KEY);
        }

        static void onRegisterMenuScreens(Object event) {
            EchoBackendClientBridge.registerMenuScreen(event, ModMenus.TERMINAL.get(), SignalOsTerminalScreen.class);
            EchoBackendClientBridge.registerMenuScreen(event, ModMenus.SERVER_RACK.get(), SignalOsServerRackScreen.class);
        }

        static void onRegisterRenderers(Object event) {
            registerRenderCoreBlockRenderers(event);
        }
    }

    private static Object invokeNoArg(Object event, String methodName) {
        if (event == null) {
            return null;
        }
        try {
            return event.getClass().getMethod(methodName).invoke(event);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }

    private static void setCanceled(Object event) {
        try {
            event.getClass().getMethod("setCanceled", boolean.class).invoke(event, true);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }
}
