package com.knoxhack.echo.adaptercore;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * AdapterCore backend bridge for legacy deferred content registries.
 */
public final class EchoBackendRegistryBridge {
    private EchoBackendRegistryBridge() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Object create(Object registry, String modId) {
        if (registry instanceof Registry liveRegistry) {
            return DeferredRegister.create(liveRegistry, modId);
        }
        if (registry instanceof ResourceKey key) {
            return DeferredRegister.create(key, modId);
        }
        throw new IllegalArgumentException("Unsupported backend registry key: " + registry);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T> EchoBackendRegistryEntry<T> register(Object registry, String id, Supplier<? extends T> factory) {
        if (registry instanceof DeferredRegister deferredRegister) {
            return new EchoBackendRegistryEntry<>(deferredRegister.register(id, factory));
        }
        throw new IllegalArgumentException("Unsupported backend registry: " + registry);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T> EchoBackendRegistryEntry<T> registerWithId(Object registry, String id,
            Function<Identifier, ? extends T> factory) {
        if (registry instanceof DeferredRegister deferredRegister) {
            return new EchoBackendRegistryEntry<>(deferredRegister.register(id, key -> factory.apply((Identifier) key)));
        }
        throw new IllegalArgumentException("Unsupported backend registry: " + registry);
    }

    public static <T extends Block> EchoBackendRegistryEntry<T> registerBlock(Object registry, String id,
            Function<BlockBehaviour.Properties, ? extends T> factory,
            UnaryOperator<BlockBehaviour.Properties> properties) {
        return register(registry, id, () -> factory.apply(blockProperties(properties)));
    }

    public static EchoBackendRegistryEntry<Block> registerSimpleBlock(Object registry, String id,
            UnaryOperator<BlockBehaviour.Properties> properties) {
        return registerBlock(registry, id, Block::new, properties);
    }

    public static <T extends Item> EchoBackendRegistryEntry<T> registerItem(Object registry, String id,
            Function<Item.Properties, ? extends T> factory) {
        return registerItem(registry, id, factory, UnaryOperator.identity());
    }

    public static <T extends Item> EchoBackendRegistryEntry<T> registerItem(Object registry, String id,
            Function<Item.Properties, ? extends T> factory,
            UnaryOperator<Item.Properties> properties) {
        return register(registry, id, () -> factory.apply(itemProperties(properties)));
    }

    public static EchoBackendRegistryEntry<Item> registerSimpleItem(Object registry, String id,
            UnaryOperator<Item.Properties> properties) {
        return registerItem(registry, id, Item::new, properties);
    }

    public static EchoBackendRegistryEntry<BlockItem> registerSimpleBlockItem(Object registry, EchoBackendRegistryEntry<? extends Block> block) {
        return register(registry, block.id().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
    }

    @SuppressWarnings("rawtypes")
    public static void registerEventBus(Object registry, Object eventBus) {
        if (registry instanceof DeferredRegister deferredRegister && eventBus instanceof IEventBus bus) {
            deferredRegister.register(bus);
        }
    }

    @SuppressWarnings("rawtypes")
    public static List<EchoBackendRegistryEntry<?>> entries(Object registry) {
        if (registry instanceof DeferredRegister deferredRegister) {
            return deferredRegister.getEntries().stream()
                    .map(EchoBackendRegistryBridge::entry)
                    .toList();
        }
        return List.of();
    }

    public static EchoBackendRegistryEntry<?> entry(Object holder) {
        return holder instanceof EchoBackendRegistryEntry<?> entry ? entry : new EchoBackendRegistryEntry<>(holder);
    }

    private static BlockBehaviour.Properties blockProperties(UnaryOperator<BlockBehaviour.Properties> properties) {
        BlockBehaviour.Properties base = BlockBehaviour.Properties.of();
        return properties == null ? base : properties.apply(base);
    }

    private static Item.Properties itemProperties(UnaryOperator<Item.Properties> properties) {
        Item.Properties base = new Item.Properties();
        return properties == null ? base : properties.apply(base);
    }
}
