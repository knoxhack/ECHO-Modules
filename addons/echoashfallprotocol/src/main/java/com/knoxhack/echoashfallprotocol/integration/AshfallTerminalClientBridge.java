package com.knoxhack.echoashfallprotocol.integration;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.client.screen.EchoTerminalStyle;
import com.knoxhack.echoashfallprotocol.echo.MissionUxSummary;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoterminal.EchoTerminalClient;
import com.knoxhack.echoterminal.client.hud.TerminalHudNoticeSurface;
import com.knoxhack.echoterminal.client.screen.EchoTerminalNativeSessionBridge;
import com.knoxhack.echoterminal.client.screen.EchoTerminalScreen;
import com.knoxhack.echoterminal.client.screen.EchoTerminalScreenProvider;
import com.knoxhack.echoterminal.client.screen.EchoTerminalScreens;
import com.knoxhack.echoterminal.client.screen.TerminalScreenTheme;
import com.knoxhack.echoterminal.menu.EchoTerminalMenu;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Keeps optional Terminal client classes out of Ashfall's Native route bootstrap class.
 */
public final class AshfallTerminalClientBridge {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final AtomicBoolean LOGGED_TERMINAL_FALLBACK_OPEN = new AtomicBoolean(false);

    private AshfallTerminalClientBridge() {
    }

    public static void register(String ownerModId) {
        String owner = ownerModId == null || ownerModId.isBlank() ? EchoAshfallProtocol.MODID : ownerModId;
        TerminalHudNoticeSurface.claimExternalSurface(owner);
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        EchoTerminalScreens.registerFallback(new EchoTerminalScreenProvider() {
            @Override
            public AbstractContainerScreen<EchoTerminalMenu> create(
                    EchoTerminalMenu menu,
                    Inventory playerInventory,
                    Component title
            ) {
                if (LOGGED_TERMINAL_FALLBACK_OPEN.compareAndSet(false, true)) {
                    EchoAshfallProtocol.LOGGER.info("Opening ECHO Terminal Ashfall legacy fallback renderer.");
                }
                return new EchoTerminalScreen(menu, playerInventory, title, ashfallTerminalTheme());
            }

            @Override
            public boolean isTerminalScreen(Screen screen) {
                return screen instanceof EchoTerminalScreen;
            }
        });
    }

    public static boolean openTerminalSurface(
            Minecraft minecraft,
            String action,
            Map<String, Object> actionMetadata
    ) {
        if (minecraft == null || minecraft.player == null) {
            return false;
        }
        register(EchoAshfallProtocol.MODID);
        boolean screenAlreadyOpen = EchoTerminalScreens.isManagedTerminalScreen(minecraft.screen);
        boolean opened = EchoTerminalClient.openNativeTerminalFromLaunchScreen();
        if (!opened) {
            minecraft.setScreen(EchoTerminalScreens.create(
                    new EchoTerminalMenu(0, minecraft.player.getInventory()),
                    minecraft.player.getInventory(),
                    Component.translatable("container.echoterminal.echo_terminal")));
            opened = true;
        }
        EchoTerminalNativeSessionBridge.recordNativeOpen(
                action,
                actionMetadata == null ? Map.of() : actionMetadata,
                opened,
                screenAlreadyOpen);
        return opened;
    }

    private static TerminalScreenTheme ashfallTerminalTheme() {
        return new TerminalScreenTheme(
                "ECHO-7 ASHFALL TERMINAL",
                minecraft -> {
                    if (minecraft.player == null) {
                        return "LINK OFFLINE";
                    }
                    QuestData quest = QuestData.get(minecraft.player);
                    MissionUxSummary summary = MissionUxSummary.current(minecraft.player, quest);
                    return summary.missionId().isBlank() ? "PROTOCOL SYNC PENDING" : summary.shortTitle();
                },
                "M / ESC closes | arrows cycle tabs | up/down groups | wheel/page scrolls",
                0xEE050B10,
                0xE8050B10,
                0xD8061016,
                EchoTerminalStyle.CYAN,
                0xFF244352,
                EchoTerminalStyle.TEXT,
                EchoTerminalStyle.MUTED,
                1500,
                820);
    }
}
