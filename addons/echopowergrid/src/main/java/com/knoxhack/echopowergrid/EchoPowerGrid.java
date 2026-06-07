package com.knoxhack.echopowergrid;

import com.knoxhack.echocore.api.EchoRuntimeModules;
import com.knoxhack.echopowergrid.commands.EchoPowerCommands;
import com.knoxhack.echopowergrid.grid.PowerNetworkManager;
import com.knoxhack.echopowergrid.integration.PowerGridCoreIntegration;
import com.knoxhack.echopowergrid.integration.PowerGridMachineRuntimeHost;
import com.knoxhack.echopowergrid.registry.ModBlockEntities;
import com.knoxhack.echopowergrid.registry.ModBlocks;
import com.knoxhack.echopowergrid.registry.ModCreativeTabs;
import com.knoxhack.echopowergrid.registry.ModItems;
import com.knoxhack.echopowergrid.registry.ModMenus;
import com.mojang.logging.LogUtils;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

public class EchoPowerGrid {
    public static final String MODID = "echopowergrid";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    public EchoPowerGrid() {
        var runtime = Agent9PowerGridRuntimeAdapter.activateNativeHostEntrypoint();
        LOGGER.info("ECHO PowerGrid Agent 9 native host adapter {}.", runtime.get("status"));
        ModBlocks.register();
        ModBlockEntities.register();
        ModItems.register();
        ModMenus.register();
        ModCreativeTabs.register();
        com.knoxhack.echopowergrid.integration.prime.PowerGridPrimeIntegration.register();
        commonSetup();
    }

    public void commonSetup() {
        LOGGER.info("ECHO PowerGrid online. Restore the grid. Power the signal.");
        PowerGridCoreIntegration.registerAddonChapter();
        registerOptionalIntegrations();
    }

    public void onServerTick(MinecraftServer server) {
        PowerGridMachineRuntimeHost.bindServer(server);
        PowerNetworkManager.tickAll(server);
    }

    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext,
            Commands.CommandSelection commandSelection) {
        EchoPowerCommands.register(dispatcher, buildContext, commandSelection);
    }

    private static void registerOptionalIntegrations() {
        if (EchoRuntimeModules.isLoaded("echoterminal")) {
            tryInvoke("com.knoxhack.echopowergrid.integration.terminal.PowerGridTerminalIntegration");
        }
        if (EchoRuntimeModules.isLoaded("echolens")) {
            tryInvoke("com.knoxhack.echopowergrid.integration.lens.PowerGridLensIntegration");
        }
        if (EchoRuntimeModules.isLoaded("echoruntimeguard")) {
            tryInvoke("com.knoxhack.echopowergrid.integration.runtimeguard.PowerGridRuntimeGuardIntegration");
        }
        if (EchoRuntimeModules.isLoaded("echomultiblockcore")) {
            tryInvoke("com.knoxhack.echopowergrid.integration.multiblock.PowerGridMultiblockIntegration");
        }
        if (EchoRuntimeModules.isLoaded("echoindustrialnexus")) {
            tryInvoke("com.knoxhack.echopowergrid.integration.industrial.PowerGridIndustrialIntegration");
        }
        if (EchoRuntimeModules.isLoaded("echoholomap")) {
            tryInvoke("com.knoxhack.echopowergrid.integration.holomap.PowerGridHoloMapIntegration");
        }
        if (EchoRuntimeModules.isLoaded("echomachinecore")) {
            tryInvoke("com.knoxhack.echopowergrid.integration.PowerGridMachineCoreRuntimeProvider");
        }
    }

    private static void tryInvoke(String className) {
        try {
            Class.forName(className).getMethod("register").invoke(null);
        } catch (ClassNotFoundException e) {
            LOGGER.debug("Optional integration {} not present.", className);
        } catch (ReflectiveOperationException | LinkageError e) {
            LOGGER.warn("Optional integration {} could not be registered.", className, e);
        }
    }
}
