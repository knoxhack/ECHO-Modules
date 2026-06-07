package com.knoxhack.echo.adaptercore;

import net.neoforged.neoforge.attachment.AttachmentType;

import java.util.function.Supplier;

public final class EchoAttachmentHandle<T> implements Supplier<AttachmentType<T>> {
    private final EchoBackendRegistryEntry<AttachmentType<T>> entry;

    EchoAttachmentHandle(EchoBackendRegistryEntry<AttachmentType<T>> entry) {
        this.entry = entry;
    }

    @Override
    public AttachmentType<T> get() {
        return entry.get();
    }

    public Object backendHolder() {
        return entry.backendHolder();
    }
}
