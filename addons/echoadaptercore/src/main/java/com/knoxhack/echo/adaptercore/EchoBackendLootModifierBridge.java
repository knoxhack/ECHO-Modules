package com.knoxhack.echo.adaptercore;

import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * AdapterCore backend bridge for global loot modifier serializers.
 */
public final class EchoBackendLootModifierBridge {
    private EchoBackendLootModifierBridge() {
    }

    public static Object createGlobalLootModifierSerializers(String modId) {
        return DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, modId);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Object registerSerializer(Object registry, String name, MapCodec<?> codec) {
        if (registry instanceof DeferredRegister deferredRegister) {
            return deferredRegister.register(name, () -> codec);
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    public static void register(Object registry, Object eventBus) {
        if (registry instanceof DeferredRegister deferredRegister && eventBus instanceof IEventBus bus) {
            deferredRegister.register(bus);
        }
    }

    @SuppressWarnings("unchecked")
    public static MapCodec<? extends IGlobalLootModifier> codec(Object holder) {
        if (holder instanceof DeferredHolder<?, ?> deferredHolder) {
            return (MapCodec<? extends IGlobalLootModifier>) deferredHolder.get();
        }
        return null;
    }
}
