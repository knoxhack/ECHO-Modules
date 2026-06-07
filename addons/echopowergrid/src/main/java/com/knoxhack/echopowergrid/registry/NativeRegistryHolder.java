package com.knoxhack.echopowergrid.registry;

import java.util.Objects;
import java.util.function.Supplier;

public final class NativeRegistryHolder<T> implements Supplier<T> {
    private final String id;
    private final T value;

    private NativeRegistryHolder(String id, T value) {
        this.id = id == null ? "" : id;
        this.value = Objects.requireNonNull(value, this.id);
    }

    public static <T> NativeRegistryHolder<T> of(String id, T value) {
        return new NativeRegistryHolder<>(id, value);
    }

    @Override
    public T get() {
        return value;
    }

    public String id() {
        return id;
    }
}
