package com.knoxhack.echoorbitalremnants.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoorbitalremnants.EchoOrbitalRemnants;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

public final class ModCreativeTabs {
    private static final Object TABS =
            EchoBackendRegistryBridge.create(Registries.CREATIVE_MODE_TAB, EchoOrbitalRemnants.MODID);

    public static final EchoBackendRegistryEntry<CreativeModeTab> ORBITAL_REMNANTS = EchoBackendRegistryBridge.register(TABS, "orbital_remnants",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.echoorbitalremnants.orbital_remnants"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.ECHO_TERMINAL.get().getDefaultInstance())
                    .displayItems((parameters, output) -> ModItems.creativeItems().forEach(output::accept))
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(TABS, eventBus);
    }
}
