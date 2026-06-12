package com.echoplatform.echocore.api;

import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

public interface IDataView {
    default <T> T get(IDataKey<T> key) {
        return key == null ? null : key.defaultValue();
    }

    default <T> boolean set(IDataKey<T> key, T value) {
        return false;
    }

    default boolean clear(IDataKey<?> key) {
        return false;
    }

    default boolean has(IDataKey<?> key) {
        return false;
    }

    default CompoundTag record(Identifier id) {
        return new CompoundTag();
    }

    default boolean putRecord(Identifier id, CompoundTag value) {
        return false;
    }

    default Map<Identifier, String> debugSnapshot() {
        return Map.of();
    }
}
