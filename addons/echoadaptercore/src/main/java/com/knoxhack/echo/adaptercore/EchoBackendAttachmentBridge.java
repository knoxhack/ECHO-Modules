package com.knoxhack.echo.adaptercore;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * AdapterCore backend bridge for player/world attachment registrations.
 */
public final class EchoBackendAttachmentBridge {
    private EchoBackendAttachmentBridge() {
    }

    public static Object createAttachmentRegistry(String modId) {
        return EchoBackendRegistryBridge.create(NeoForgeRegistries.ATTACHMENT_TYPES, modId);
    }

    public static <T extends EchoValueIOSerializable> EchoAttachmentHandle<T> registerSerializable(Object registry, String id,
            Supplier<T> factory) {
        return register(registry, id, () -> serializableBuilder(factory).build());
    }

    public static <T extends EchoValueIOSerializable> EchoAttachmentHandle<T> registerSerializableCopyOnDeath(Object registry, String id,
            Supplier<T> factory) {
        return register(registry, id, () -> serializableBuilder(factory).copyOnDeath().build());
    }

    public static <T extends EchoValueIOSerializable> EchoAttachmentHandle<T> registerSyncedCopyOnDeath(Object registry, String id,
            Supplier<T> factory, StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
        return register(registry, id, () -> serializableBuilder(factory)
                .sync((holder, player) -> holder == player, streamCodec)
                .copyOnDeath()
                .build());
    }

    public static <T> EchoAttachmentHandle<T> registerCodecCopyOnDeath(Object registry, String id,
            Supplier<T> factory, MapCodec<T> codec) {
        return register(registry, id, () -> AttachmentType.<T>builder(factory)
                .serialize(codec)
                .copyOnDeath()
                .build());
    }

    private static <T> EchoAttachmentHandle<T> register(Object registry, String id, Supplier<AttachmentType<T>> factory) {
        return new EchoAttachmentHandle<>(EchoBackendRegistryBridge.register(registry, id, factory));
    }

    private static <T extends EchoValueIOSerializable> AttachmentType.Builder<T> serializableBuilder(
            Supplier<T> factory) {
        return AttachmentType.<T>builder(factory).serialize(new IAttachmentSerializer<>() {
            @Override
            public T read(IAttachmentHolder holder, ValueInput input) {
                T value = factory.get();
                value.deserialize(input);
                return value;
            }

            @Override
            public boolean write(T attachment, ValueOutput output) {
                attachment.serialize(output);
                return true;
            }
        });
    }
}
