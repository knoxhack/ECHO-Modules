package com.knoxhack.echospellcore;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.echoplatform.echocore.api.EchoAddonChapter;
import com.echoplatform.echocore.api.EchoAddonRegistry;
import com.knoxhack.echospellcore.integration.SpellCoreIntegrations;
import com.knoxhack.echospellcore.network.ModNetwork;
import com.knoxhack.echospellcore.registry.ModEntities;
import com.knoxhack.echospellcore.registry.ModCreativeTabs;
import com.knoxhack.echospellcore.registry.ModItems;
import com.knoxhack.echospellcore.registry.ModMenus;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(EchoSpellCore.MODID)
public final class EchoSpellCore {
    public static final String MODID = "echospellcore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoSpellCore(IEventBus modEventBus) {
        ModEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModMenus.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, ModNetwork::registerPayloads);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
        com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge.bootstrapClientEntrypoint(modEventBus,
                "com.knoxhack.echospellcore.EchoSpellCoreClient");
}

    private void commonSetup(Object event) {
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
            EchoAddonRegistry.register(new EchoAddonChapter() {
                @Override
                public String id() {
                    return "spellcore";
                }

                @Override
                public String modId() {
                    return MODID;
                }

                @Override
                public String displayName() {
                    return "ECHO: SpellCore";
                }

                @Override
                public String summary() {
                    return "Signal Focus casting, Spell Deck loadouts, starter spells, projectiles, Aether costs, cooldowns, and HUD diagnostics.";
                }

                @Override
                public String statusLine(net.minecraft.world.entity.player.Player player) {
                    return "Signal Focus online. Spell Deck matrix ready.";
                }
            });
            SpellCoreIntegrations.registerOptional();
            LOGGER.info("ECHO: SpellCore online. Reality now accepts function calls.");
        });
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

}
