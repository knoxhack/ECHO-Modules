package com.echoplatform.echocore.api;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;

public interface IDataKey<T> {
    Identifier id();

    DataScope scope();

    DataValueKind kind();

    Codec<T> codec();

    T defaultValue();

    boolean synced();

    default DataKeyMetadata metadata() {
        return DataKeyMetadata.of(this, "java");
    }

    static IDataKey<Boolean> flag(Identifier id, DataScope scope, boolean defaultValue, boolean synced) {
        return new BasicDataKey<>(id, scope, DataValueKind.FLAG, Codec.BOOL, defaultValue, synced);
    }

    static IDataKey<Long> counter(Identifier id, DataScope scope, long defaultValue, boolean synced) {
        return new BasicDataKey<>(id, scope, DataValueKind.COUNTER, Codec.LONG, defaultValue, synced);
    }

    static IDataKey<String> string(Identifier id, DataScope scope, String defaultValue, boolean synced) {
        return new BasicDataKey<>(id, scope, DataValueKind.STRING, Codec.STRING, defaultValue == null ? "" : defaultValue, synced);
    }

    static IDataKey<String> enumName(Identifier id, DataScope scope, String defaultValue, boolean synced) {
        return new BasicDataKey<>(id, scope, DataValueKind.ENUM, Codec.STRING, defaultValue == null ? "" : defaultValue, synced);
    }

    static <T> IDataKey<T> record(Identifier id, DataScope scope, Codec<T> codec, T defaultValue, boolean synced) {
        return new BasicDataKey<>(id, scope, DataValueKind.RECORD, codec, defaultValue, synced);
    }

    record BasicDataKey<T>(
            Identifier id,
            DataScope scope,
            DataValueKind kind,
            Codec<T> codec,
            T defaultValue,
            boolean synced) implements IDataKey<T> {
        public BasicDataKey {
            scope = scope == null ? DataScope.PLAYER : scope;
            kind = kind == null ? DataValueKind.RECORD : kind;
        }
    }
}
