package com.knoxhack.echomissioncore.registry;

import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;

public record NativeAttachmentType<T>(
        Identifier id,
        Supplier<T> factory,
        boolean syncToOwner,
        boolean copyOnDeath) {
    public NativeAttachmentType {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(factory, "factory");
    }

    public T create() {
        return factory.get();
    }
}
