package com.knoxhack.echopowergrid;

import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(EchoPowerGrid.MODID)
public class EchoPowerGrid {
    public static final String MODID = "echopowergrid";
    private static final String REGISTER_COMMANDS_EVENT =
            "net.neoforged.neoforge.event.RegisterCommandsEvent";
    private static final String SERVER_TICK_POST_EVENT =
            "net.neoforged.neoforge.event.tick.ServerTickEvent$Post";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    EchoPowerGrid() {
        this(null);
    }

    public EchoPowerGrid(IEventBus modEventBus) {
        var runtime = Agent9PowerGridRuntimeAdapter.activateNativeHostEntrypoint();
        LOGGER.info("ECHO PowerGrid Agent 9 native host adapter {}.", runtime.get("status"));
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModMenus.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        com.knoxhack.echopowergrid.integration.prime.PowerGridPrimeIntegration.register();
        commonSetup();
        EchoBackendLifecycleBridge.registerGameEventHandler(REGISTER_COMMANDS_EVENT, this::onRegisterCommands);
        EchoBackendLifecycleBridge.registerGameEventHandler(SERVER_TICK_POST_EVENT, this::onServerTickEvent);
        EchoBackendLifecycleBridge.registerOptionalGameTests(modEventBus,
                "com.knoxhack.echopowergrid.test.PowerGridGameTests");
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

    private void onServerTickEvent(Object event) {
        MinecraftServer server = com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge.serverTickServer(event);
        if (server != null) {
            onServerTick(server);
        }
    }

    private void onRegisterCommands(Object event) {
        var dispatcher = com.knoxhack.echo.adaptercore.EchoBackendCommandEventBridge.dispatcher(event);
        if (dispatcher != null) {
            EchoPowerCommands.register(dispatcher, null, null);
        }
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
