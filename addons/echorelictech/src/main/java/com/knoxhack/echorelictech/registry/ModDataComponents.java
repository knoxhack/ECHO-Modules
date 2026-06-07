package com.knoxhack.echorelictech.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echorelictech.EchoRelicTech;
import com.knoxhack.echorelictech.api.relic.RelicInstanceData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;

public final class ModDataComponents {
    public static final Object DATA_COMPONENT_TYPES =
            EchoBackendRegistryBridge.create(Registries.DATA_COMPONENT_TYPE, EchoRelicTech.MODID);

    public static final EchoBackendRegistryEntry<DataComponentType<RelicInstanceData>> RELIC_DATA =
            EchoBackendRegistryBridge.register(DATA_COMPONENT_TYPES, "relic_data", () -> DataComponentType.<RelicInstanceData>builder()
                    .persistent(RelicInstanceData.CODEC)
                    .networkSynchronized(RelicInstanceData.STREAM_CODEC)
                    .build());

    public static final EchoBackendRegistryEntry<DataComponentType<Integer>> NULL_CHARGE =
            EchoBackendRegistryBridge.register(DATA_COMPONENT_TYPES, "null_charge", () -> DataComponentType.<Integer>builder()
                    .persistent(net.minecraft.util.ExtraCodecs.NON_NEGATIVE_INT)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT)
                    .build());

    public static final EchoBackendRegistryEntry<DataComponentType<com.knoxhack.echorelictech.api.relic.UnidentifiedRelicData>> UNIDENTIFIED_RELIC_DATA =
            EchoBackendRegistryBridge.register(DATA_COMPONENT_TYPES, "unidentified_relic_data", () -> DataComponentType.<com.knoxhack.echorelictech.api.relic.UnidentifiedRelicData>builder()
                    .persistent(com.knoxhack.echorelictech.api.relic.UnidentifiedRelicData.CODEC)
                    .networkSynchronized(com.knoxhack.echorelictech.api.relic.UnidentifiedRelicData.STREAM_CODEC)
                    .build());

    private ModDataComponents() {}

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(DATA_COMPONENT_TYPES, eventBus);
    }
}
