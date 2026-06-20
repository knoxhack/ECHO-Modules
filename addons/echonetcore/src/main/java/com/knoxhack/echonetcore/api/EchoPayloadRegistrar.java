package com.knoxhack.echonetcore.api;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class EchoPayloadRegistrar {
    private final String protocolVersion;
    private final boolean optional;
    private final Object registrationEvent;
    private final List<EchoPayloadRegistration<?>> registrations = new ArrayList<>();

    private EchoPayloadRegistrar(String protocolVersion, boolean optional, Object registrationEvent) {
        this.protocolVersion = protocolVersion == null || protocolVersion.isBlank()
                ? EchoNetPayloads.VERSION
                : protocolVersion;
        this.optional = optional;
        this.registrationEvent = registrationEvent;
    }

    public static EchoPayloadRegistrar optional(String protocolVersion) {
        return new EchoPayloadRegistrar(protocolVersion, true, EchoNetPayloads.currentRegistrationEvent());
    }

    public String protocolVersion() {
        return protocolVersion;
    }

    public boolean optionalPackets() {
        return optional;
    }

    public List<EchoPayloadRegistration<?>> registrations() {
        return List.copyOf(registrations);
    }

    public <T extends CustomPacketPayload> EchoPayloadRegistrar playToClient(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        EchoPayloadRegistration<T> registration = EchoPayloadRegistration.clientbound(type, codec, null);
        registrations.add(registration);
        EchoNeoForgePayloadBridge.register(registrationEvent, protocolVersion, optional, registration);
        return this;
    }

    public <T extends CustomPacketPayload> EchoPayloadRegistrar playToClient(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            ClientboundReceiver<T> handler) {
        EchoPayloadRegistration<T> registration = EchoPayloadRegistration.clientbound(type, codec, handler);
        registrations.add(registration);
        EchoNeoForgePayloadBridge.register(registrationEvent, protocolVersion, optional, registration);
        return this;
    }

    public <T extends CustomPacketPayload> EchoPayloadRegistrar playToServer(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            ServerboundReceiver<T> handler) {
        EchoPayloadRegistration<T> registration = EchoPayloadRegistration.serverbound(type, codec, handler);
        registrations.add(registration);
        EchoNeoForgePayloadBridge.register(registrationEvent, protocolVersion, optional, registration);
        return this;
    }

    @FunctionalInterface
    public interface ClientboundReceiver<T extends CustomPacketPayload> {
        void receive(T packet, EchoPayloadContext context);
    }

    @FunctionalInterface
    public interface ServerboundReceiver<T extends CustomPacketPayload> {
        void receive(T packet, EchoPayloadContext context);
    }

    public record EchoPayloadRegistration<T extends CustomPacketPayload>(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            Direction direction,
            Object handler) {
        private static <T extends CustomPacketPayload> EchoPayloadRegistration<T> clientbound(
                CustomPacketPayload.Type<T> type,
                StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                ClientboundReceiver<T> handler) {
            return new EchoPayloadRegistration<>(type, codec, Direction.CLIENTBOUND, handler);
        }

        private static <T extends CustomPacketPayload> EchoPayloadRegistration<T> serverbound(
                CustomPacketPayload.Type<T> type,
                StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                ServerboundReceiver<T> handler) {
            return new EchoPayloadRegistration<>(type, codec, Direction.SERVERBOUND, handler);
        }
    }

    public enum Direction {
        CLIENTBOUND,
        SERVERBOUND
    }
}
