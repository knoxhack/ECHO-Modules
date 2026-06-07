package com.knoxhack.echo.adaptercore;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Supplier;

public final class EchoBackendRegistryEntry<T> implements Supplier<T>, ItemLike {
    private final Object holder;

    EchoBackendRegistryEntry(Object holder) {
        this.holder = holder;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get() {
        if (holder instanceof Supplier<?> supplier) {
            return (T) supplier.get();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public ResourceKey<T> key() {
        if (holder instanceof DeferredHolder<?, ?> deferredHolder) {
            return (ResourceKey<T>) deferredHolder.getKey();
        }
        return null;
    }

    public Identifier id() {
        if (holder instanceof DeferredHolder<?, ?> deferredHolder) {
            return deferredHolder.getId();
        }
        return null;
    }

    public Object backendHolder() {
        return holder;
    }

    @Override
    public Item asItem() {
        T value = get();
        return value instanceof ItemLike itemLike ? itemLike.asItem() : null;
    }
}
