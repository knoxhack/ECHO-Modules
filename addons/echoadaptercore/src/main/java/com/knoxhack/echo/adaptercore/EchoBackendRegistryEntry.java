package com.knoxhack.echo.adaptercore;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;

public final class EchoBackendRegistryEntry<T> implements Supplier<T>, ItemLike {
    private static final String DEFERRED_HOLDER_CLASS = "net.neoforged.neoforge.registries.DeferredHolder";

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
        if (holder instanceof EchoBackendRegistryBridge.LocalHolder<?> localHolder) {
            return (ResourceKey<T>) localHolder.key();
        }
        if (isDeferredHolder(holder)) {
            Object key = invoke(holder, "getKey");
            return key instanceof ResourceKey<?> resourceKey ? (ResourceKey<T>) resourceKey : null;
        }
        return null;
    }

    public Identifier id() {
        if (holder instanceof EchoBackendRegistryBridge.LocalHolder<?> localHolder) {
            return localHolder.id();
        }
        if (isDeferredHolder(holder)) {
            Object id = invoke(holder, "getId");
            return id instanceof Identifier identifier ? identifier : null;
        }
        return null;
    }

    public Object backendHolder() {
        return holder;
    }

    @Override
    public Item asItem() {
        T value = get();
        return value instanceof ItemLike itemLike ? itemLike.asItem() : Items.AIR;
    }

    private static boolean isDeferredHolder(Object value) {
        Class<?> deferredHolder = optionalClass(DEFERRED_HOLDER_CLASS);
        return deferredHolder != null && deferredHolder.isInstance(value);
    }

    private static Class<?> optionalClass(String className) {
        try {
            return Class.forName(className, false, EchoBackendRegistryEntry.class.getClassLoader());
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private static Object invoke(Object target, String methodName) {
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            return null;
        }
    }
}
