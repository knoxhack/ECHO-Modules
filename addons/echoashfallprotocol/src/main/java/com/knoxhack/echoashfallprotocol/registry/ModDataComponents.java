package com.knoxhack.echoashfallprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.item.AshfallTooltip;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;

public final class ModDataComponents {
    public static final Object DATA_COMPONENT_TYPES =
            EchoBackendRegistryBridge.create(Registries.DATA_COMPONENT_TYPE, EchoAshfallProtocol.MODID);

    public static final EchoBackendRegistryEntry<DataComponentType<Integer>> STORED_ENERGY =
            EchoBackendRegistryBridge.register(DATA_COMPONENT_TYPES, "stored_energy", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    public static final EchoBackendRegistryEntry<DataComponentType<AshfallTooltip>> ASHFALL_TOOLTIP =
            EchoBackendRegistryBridge.register(DATA_COMPONENT_TYPES, "ashfall_tooltip", () -> DataComponentType.<AshfallTooltip>builder()
                    .persistent(AshfallTooltip.CODEC)
                    .networkSynchronized(AshfallTooltip.STREAM_CODEC)
                    .build());

    private ModDataComponents() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(DATA_COMPONENT_TYPES, eventBus);
    }
}
