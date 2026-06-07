package com.knoxhack.echoprimecore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoprimecore.EchoPrimeCore;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModCreativeTabs {
    public static final Object TABS =
            EchoBackendRegistryBridge.create(Registries.CREATIVE_MODE_TAB, EchoPrimeCore.MODID);

    public static final EchoBackendRegistryEntry<CreativeModeTab> PRIME_TAB = EchoBackendRegistryBridge.register(TABS, "prime_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.SIGNAL_SHARD.get()))
                    .title(Component.literal("ECHO: Prime Core"))
                    .displayItems((params, output) -> {
                        ModBlocks.blockItems().forEach(block -> output.accept(block.get()));
                        ModItems.creativeItems().forEach(item -> output.accept(item.get()));
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(TABS, eventBus);
    }
}
