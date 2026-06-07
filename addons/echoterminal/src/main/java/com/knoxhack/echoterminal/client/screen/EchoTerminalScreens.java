package com.knoxhack.echoterminal.client.screen;

import com.knoxhack.echoterminal.EchoTerminal;
import com.knoxhack.echoterminal.menu.EchoTerminalMenu;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class EchoTerminalScreens {
    private static volatile EchoTerminalScreenProvider primaryProvider;
    private static volatile EchoTerminalScreenProvider fallbackProvider;
    private static final AtomicBoolean LOGGED_DEFAULT_FALLBACK = new AtomicBoolean(false);

    private EchoTerminalScreens() {
    }

    public static void registerPrimary(EchoTerminalScreenProvider provider) {
        primaryProvider = Objects.requireNonNull(provider, "provider");
    }

    public static void registerFallback(EchoTerminalScreenProvider provider) {
        fallbackProvider = Objects.requireNonNull(provider, "provider");
    }

    public static AbstractContainerScreen<EchoTerminalMenu> create(EchoTerminalMenu menu, Inventory playerInventory, Component title) {
        AbstractContainerScreen<EchoTerminalMenu> primary =
                createFromProvider(primaryProvider, true, menu, playerInventory, title);
        if (primary != null) {
            return primary;
        }
        AbstractContainerScreen<EchoTerminalMenu> fallback =
                createFromProvider(fallbackProvider, false, menu, playerInventory, title);
        if (fallback != null) {
            return fallback;
        }
        if (LOGGED_DEFAULT_FALLBACK.compareAndSet(false, true)) {
            EchoTerminal.LOGGER.info("Opening ECHO Terminal default fallback renderer; no registered provider supplied a screen.");
        }
        return new EchoTerminalScreen(menu, playerInventory, title);
    }

    public static boolean isManagedTerminalScreen(Screen screen) {
        if (screen instanceof EchoTerminalScreen || screen instanceof EchoNativeTerminalScreen) {
            return true;
        }
        EchoTerminalScreenProvider provider = primaryProvider;
        if (provider != null && provider.isTerminalScreen(screen)) {
            return true;
        }
        EchoTerminalScreenProvider fallback = fallbackProvider;
        return fallback != null && fallback.isTerminalScreen(screen);
    }

    public static ProviderSlot providerSlotForTests(
            boolean primaryRegistered,
            boolean primarySuppliesScreen,
            boolean fallbackRegistered,
            boolean fallbackSuppliesScreen) {
        if (primaryRegistered && primarySuppliesScreen) {
            return ProviderSlot.PRIMARY;
        }
        if (fallbackRegistered && fallbackSuppliesScreen) {
            return ProviderSlot.FALLBACK;
        }
        return ProviderSlot.DEFAULT;
    }

    private static AbstractContainerScreen<EchoTerminalMenu> createFromProvider(
            EchoTerminalScreenProvider provider,
            boolean primary,
            EchoTerminalMenu menu,
            Inventory playerInventory,
            Component title) {
        if (provider == null) {
            return null;
        }
        try {
            return provider.create(menu, playerInventory, title);
        } catch (RuntimeException | LinkageError exception) {
            if (primary) {
                primaryProvider = null;
            } else {
                fallbackProvider = null;
            }
            EchoTerminal.LOGGER.warn("ECHO Terminal {} screen provider failed; falling back.",
                    primary ? "primary" : "fallback", exception);
            return null;
        }
    }

    public enum ProviderSlot {
        PRIMARY,
        FALLBACK,
        DEFAULT
    }
}
