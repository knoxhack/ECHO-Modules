package com.knoxhack.echosoundcore.registry;

import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;

public final class NativeRegistryHolder<T> implements Supplier<T> {
    private final Identifier id;
    private final T value;

    private NativeRegistryHolder(Identifier id, T value) {
        this.id = Objects.requireNonNull(id, "id");
        this.value = Objects.requireNonNull(value, id.toString());
    }

    public static <T> NativeRegistryHolder<T> of(Identifier id, T value) {
        return new NativeRegistryHolder<>(id, value);
    }

    @Override
    public T get() {
        return value;
    }

    public Identifier getId() {
        return id;
    }

    public String id() {
        return id.getPath();
    }
}
