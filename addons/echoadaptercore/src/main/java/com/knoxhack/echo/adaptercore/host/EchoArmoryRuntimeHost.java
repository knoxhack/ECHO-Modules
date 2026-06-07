package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {echoarmory} capabilities in the AdapterCore truth layer.
 */
public final class EchoArmoryRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoarmory:runtime_host";
    private static final EchoArmoryRuntimeHost HOST = new EchoArmoryRuntimeHost();

    private EchoArmoryRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of(
                "EchoNativeRuntimeHost.Hud",
                "EchoNativeRuntimeHost.Packets",
                "EchoNativeRuntimeHost.SaveData",
                "EchoNativeRuntimeHost.WorldState",
                "EchoNativeRuntimeHost.Events",
                "EchoNativeRuntimeHost.Capabilities"),
                Set.of(
                "echoarmory.item_crafted",
                "echoarmory.item_used",
                "echoarmory.block_placed",
                "echoarmory.block_broken",
                "echoarmory.machine_use",
                "echoarmory.machine_state_changed",
                "echoarmory.mission_start",
                "echoarmory.mission_complete",
                "echoarmory.save_write",
                "echoarmory.save_read",
                "echoarmory.ui_open",
                "echoarmory.ui_close",
                "echoarmory.button_click",
                "echoarmory.packet_send",
                "echoarmory.packet_receive",
                "echoarmory.region_enter",
                "echoarmory.discovery"),
                Set.of(),
                true,
                true,
                true));

        try {
            Class<?> handlerClass = Class.forName("com.knoxhack.echoarmory.integration.ArmoryActionHandler");
            handlerClass.getMethod("register").invoke(null);
        } catch (Exception ignored) {
            // Addon action handler not present
        }
    }
}
