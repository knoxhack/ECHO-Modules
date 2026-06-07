package com.knoxhack.echopowergrid;

import com.knoxhack.echocore.api.EchoRuntimeModules;
import com.knoxhack.echopowergrid.client.screen.PowerNodeScreen;
import com.knoxhack.echopowergrid.client.screen.SubstationScreen;
import com.knoxhack.echopowergrid.registry.ModMenus;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class EchoPowerGridClient {
    public EchoPowerGridClient() {
        clientSetup();
    }

    public List<NativeScreenRegistration<?>> screenFactories() {
        return List.of(
                new NativeScreenRegistration<>(ModMenus.SUBSTATION.id(), SubstationScreen::new),
                new NativeScreenRegistration<>(ModMenus.POWER_NODE.id(), PowerNodeScreen::new));
    }

    public void clientSetup() {
        if (EchoRuntimeModules.isLoaded("echoterminal")) {
            tryInvoke("com.knoxhack.echopowergrid.integration.terminal.PowerGridTerminalClientIntegration");
        }
    }

    private static void tryInvoke(String className) {
        try {
            Class.forName(className).getMethod("register").invoke(null);
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
    }

    public record NativeScreenRegistration<T extends AbstractContainerMenu>(
            String menuId,
            NativeScreenFactory<T> factory) {
    }

    @FunctionalInterface
    public interface NativeScreenFactory<T extends AbstractContainerMenu> {
        Screen create(T menu, Inventory inventory, Component title);
    }
}
