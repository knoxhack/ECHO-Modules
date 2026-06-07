package com.knoxhack.echowiki.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echowiki.EchoWiki;
import com.knoxhack.echowiki.content.GuideBookTarget;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;

public final class ModDataComponents {
    public static final Object DATA_COMPONENT_TYPES =
            EchoBackendRegistryBridge.create(Registries.DATA_COMPONENT_TYPE, EchoWiki.MODID);

    public static final EchoBackendRegistryEntry<DataComponentType<GuideBookTarget>> GUIDE_BOOK_TARGET =
            EchoBackendRegistryBridge.register(DATA_COMPONENT_TYPES, "guide_book_target",
                    () -> DataComponentType.<GuideBookTarget>builder()
                    .persistent(GuideBookTarget.CODEC)
                    .networkSynchronized(GuideBookTarget.STREAM_CODEC)
                    .build());

    private ModDataComponents() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(DATA_COMPONENT_TYPES, eventBus);
    }
}
