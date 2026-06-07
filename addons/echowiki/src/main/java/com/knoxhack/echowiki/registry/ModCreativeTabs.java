package com.knoxhack.echowiki.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echowiki.EchoWiki;
import com.knoxhack.echowiki.item.GuideBookStacks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

public final class ModCreativeTabs {
    private static final Object TABS =
            EchoBackendRegistryBridge.create(Registries.CREATIVE_MODE_TAB, EchoWiki.MODID);

    public static final EchoBackendRegistryEntry<CreativeModeTab> WIKI_TAB = EchoBackendRegistryBridge.register(TABS,
            "wiki",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.echowiki.wiki"))
                    .withTabsBefore(new ResourceKey[]{CreativeModeTabs.TOOLS_AND_UTILITIES})
                    .icon(() -> ModItems.GUIDE_BOOK.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        java.util.List<net.minecraft.world.item.ItemStack> stacks = GuideBookStacks.visibleStacks();
                        if (stacks.isEmpty()) {
                            output.accept(ModItems.GUIDE_BOOK.get());
                            return;
                        }
                        stacks.forEach(output::accept);
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(TABS, eventBus);
    }
}
