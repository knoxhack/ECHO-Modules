package com.knoxhack.echoterminal.client;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeEnvironmentBridge;
import com.knoxhack.echoterminal.client.screen.EchoTerminalScreen;
import dev.echo.nativeplatform.contracts.EchoNativeClientRouteRegistries;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import java.util.Map;

public class TerminalEventHandler {
    private static final String SCREEN_CHAR_TYPED_PRE_EVENT =
            "net.neoforged.neoforge.client.event.ScreenEvent$CharacterTyped$Pre";
    private static final String SCREEN_MOUSE_SCROLLED_PRE_EVENT =
            "net.neoforged.neoforge.client.event.ScreenEvent$MouseScrolled$Pre";

    public static void register() {
        EchoBackendLifecycleBridge.registerGameEventHandler(SCREEN_CHAR_TYPED_PRE_EVENT,
                TerminalEventHandler::onCharacterTyped);
        EchoBackendLifecycleBridge.registerGameEventHandler(SCREEN_MOUSE_SCROLLED_PRE_EVENT,
                TerminalEventHandler::onMouseScroll);
    }

    private static void onCharacterTyped(Object event) {
        if (nativeLoaderActive()) {
            if (EchoNativeClientRouteRegistries.get().overlayInput("terminal", "terminal.screen.char_typed", Map.of(
                    "source", "native_client_bridge",
                    "eventType", "character_typed",
                    "screenClass", EchoBackendClientBridge.screen(event) == null ? "" : EchoBackendClientBridge.screen(event).getClass().getName(),
                    "characterEvent", String.valueOf(EchoBackendClientBridge.characterEvent(event))
            )) == EchoNativeLoadStatus.MUTATED) {
                EchoBackendClientBridge.cancel(event);
            }
            return;
        }
        if (EchoBackendClientBridge.screen(event) instanceof EchoTerminalScreen screen
                && screen.handleCharTyped(EchoBackendClientBridge.characterEvent(event))) {
            EchoBackendClientBridge.cancel(event);
        }
    }

    private static void onMouseScroll(Object event) {
        if (nativeLoaderActive()) {
            if (EchoNativeClientRouteRegistries.get().mouseInput("terminal", "terminal.screen.mouse_scroll", Map.of(
                    "source", "native_client_bridge",
                    "eventType", "mouse_scroll",
                    "screenClass", EchoBackendClientBridge.screen(event) == null ? "" : EchoBackendClientBridge.screen(event).getClass().getName(),
                    "mouseX", EchoBackendClientBridge.mouseX(event),
                    "mouseY", EchoBackendClientBridge.mouseY(event),
                    "scrollDeltaY", EchoBackendClientBridge.scrollDeltaY(event)
            )) == EchoNativeLoadStatus.MUTATED) {
                EchoBackendClientBridge.cancel(event);
            }
            return;
        }
        if (EchoBackendClientBridge.screen(event) instanceof EchoTerminalScreen screen
                && screen.handleMouseScroll(
                EchoBackendClientBridge.mouseX(event),
                EchoBackendClientBridge.mouseY(event),
                EchoBackendClientBridge.scrollDeltaY(event))) {
            EchoBackendClientBridge.cancel(event);
        }
    }

    private static boolean nativeLoaderActive() {
        return EchoNativeRuntimeEnvironmentBridge.isNativeLoaderActive();
    }
}
