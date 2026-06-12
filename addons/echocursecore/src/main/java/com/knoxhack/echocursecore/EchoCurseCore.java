package com.knoxhack.echocursecore;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.echoplatform.echocore.api.EchoAddonChapter;
import com.echoplatform.echocore.api.EchoAddonRegistry;
import com.knoxhack.echocursecore.api.CurseCoreApi;
import com.knoxhack.echocursecore.integration.CurseCoreIntegrations;
import com.knoxhack.echocursecore.network.CurseCoreNetwork;
import com.knoxhack.echocursecore.registry.ModCreativeTabs;
import com.knoxhack.echocursecore.registry.ModItems;
import com.knoxhack.echocursecore.registry.ModMenus;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(EchoCurseCore.MODID)
public final class EchoCurseCore {
    public static final String MODID = "echocursecore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoCurseCore(IEventBus modEventBus) {
        ModItems.register(modEventBus);
        ModMenus.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, CurseCoreNetwork::registerPayloads);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
        EchoBackendLifecycleBridge.registerGameEventHandler(this::onPlayerTick);
    }

    private void commonSetup(Object event) {
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
            EchoAddonRegistry.register(new EchoAddonChapter() {
                @Override
                public String id() {
                    return "cursecore";
                }

                @Override
                public String modId() {
                    return MODID;
                }

                @Override
                public String displayName() {
                    return "ECHO: CurseCore";
                }

                @Override
                public String summary() {
                    return "Persistent curses, symptoms, cleansing hooks, and explicit consequence records.";
                }

                @Override
                public String statusLine(net.minecraft.world.entity.player.Player player) {
                    int curses = CurseCoreApi.activeCurses(player).size();
                    return curses == 0 ? "No active curse signatures." : curses + " active curse signature(s).";
                }
            });
            CurseCoreIntegrations.registerOptional();
            LOGGER.info("ECHO: CurseCore online. Consequences are now persistent.");
        });
    }

    private void onPlayerTick(Object event) {
        var player = EchoBackendWorldEventBridge.postTickServerPlayer(event);
        if (player != null) {
            CurseCoreApi.tick(player);
        }
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

}
