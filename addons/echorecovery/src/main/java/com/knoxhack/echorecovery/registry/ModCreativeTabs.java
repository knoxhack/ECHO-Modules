package com.knoxhack.echorecovery.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echorecovery.EchoRecovery;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModCreativeTabs {
    public static final Object TABS = EchoBackendRegistryBridge.create(Registries.CREATIVE_MODE_TAB, EchoRecovery.MODID);

    public static final EchoBackendRegistryEntry<CreativeModeTab> RECOVERY_TAB = EchoBackendRegistryBridge.register(TABS, "recovery_tab",
        () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(ModItems.RECOVERY_COMPASS.get()))
            .title(Component.literal("ECHO Recovery"))
            .displayItems((params, output) -> {
                ModBlocks.blockItems().forEach(b -> output.accept(b.get()));
                output.accept(ModItems.GRAVE_KEY.get());
                output.accept(ModItems.RECOVERY_COMPASS.get());
                output.accept(ModItems.DEATH_RECORD.get());
                output.accept(ModItems.RECOVERY_TOKEN.get());
            })
            .build());

    private ModCreativeTabs() {}

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(TABS, eventBus);
    }
}
