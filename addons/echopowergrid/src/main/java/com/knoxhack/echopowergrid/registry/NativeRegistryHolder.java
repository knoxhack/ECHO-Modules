package com.knoxhack.echopowergrid.registry;

import java.util.Objects;
import java.util.function.Supplier;

public final class NativeRegistryHolder<T> implements Supplier<T> {
    private final String id;
    private final Supplier<? extends T> value;

    private NativeRegistryHolder(String id, Supplier<? extends T> value) {
        this.id = id == null ? "" : id;
        this.value = Objects.requireNonNull(value, this.id);
    }

    public static <T> NativeRegistryHolder<T> of(String id, T value) {
        return new NativeRegistryHolder<>(id, () -> value);
    }

    public static <T> NativeRegistryHolder<T> deferred(String id, Supplier<? extends T> value) {
        return new NativeRegistryHolder<>(id, value);
    }

    @Override
    public T get() {
        return Objects.requireNonNull(value.get(), id);
    }

    public String id() {
        return id;
    }
}
