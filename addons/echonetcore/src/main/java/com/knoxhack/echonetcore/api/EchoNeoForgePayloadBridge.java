package com.knoxhack.echonetcore.api;

import com.knoxhack.echonetcore.EchoNetCore;
import com.knoxhack.echonetcore.service.EchoNeoForgeNetworkRuntime;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

final class EchoNeoForgePayloadBridge {
    private EchoNeoForgePayloadBridge() {
    }

    static <T extends CustomPacketPayload> void register(
            Object event,
            String version,
            boolean optional,
            EchoPayloadRegistrar.EchoPayloadRegistration<T> registration) {
        if (!(event instanceof RegisterPayloadHandlersEvent payloadEvent) || registration == null) {
            return;
        }
        try {
            EchoNeoForgeNetworkRuntime.install();
            PayloadRegistrar registrar = payloadEvent.registrar(
                    version == null || version.isBlank() ? EchoNetPayloads.VERSION : version);
            if (optional) {
                registrar = registrar.optional();
            }
            if (registration.direction() == EchoPayloadRegistrar.Direction.CLIENTBOUND) {
                registerClientbound(registrar, registration);
            } else {
                registerServerbound(registrar, registration);
            }
        } catch (RuntimeException | LinkageError exception) {
            EchoNetCore.LOGGER.warn("ECHO NetCore could not bind payload {} to NeoForge.",
                    registration.type().id(), exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends CustomPacketPayload> void registerClientbound(
            PayloadRegistrar registrar,
            EchoPayloadRegistrar.EchoPayloadRegistration<T> registration) {
        Object handler = registration.handler();
        if (handler instanceof EchoPayloadRegistrar.ClientboundReceiver<?> receiver) {
            EchoPayloadRegistrar.ClientboundReceiver<T> typed =
                    (EchoPayloadRegistrar.ClientboundReceiver<T>) receiver;
            registrar.playToClient(registration.type(), registration.codec(),
                    (packet, context) -> typed.receive(packet, wrap(context)));
            return;
        }
        registrar.playToClient(registration.type(), registration.codec());
    }

    @SuppressWarnings("unchecked")
    private static <T extends CustomPacketPayload> void registerServerbound(
            PayloadRegistrar registrar,
            EchoPayloadRegistrar.EchoPayloadRegistration<T> registration) {
        Object handler = registration.handler();
        if (!(handler instanceof EchoPayloadRegistrar.ServerboundReceiver<?> receiver)) {
            EchoNetCore.LOGGER.warn("ECHO NetCore payload {} declared serverbound without a handler.",
                    registration.type().id());
            return;
        }
        EchoPayloadRegistrar.ServerboundReceiver<T> typed =
                (EchoPayloadRegistrar.ServerboundReceiver<T>) receiver;
        registrar.playToServer(registration.type(), registration.codec(),
                (packet, context) -> typed.receive(packet, wrap(context)));
    }

    private static EchoPayloadContext wrap(IPayloadContext context) {
        return new EchoPayloadContext() {
            @Override
            public Player player() {
                if (context == null) {
                    return null;
                }
                try {
                    return context.player();
                } catch (UnsupportedOperationException exception) {
                    return null;
                }
            }

            @Override
            public void enqueueWork(Runnable work) {
                if (work == null) {
                    return;
                }
                if (context == null) {
                    work.run();
                    return;
                }
                context.enqueueWork(work);
            }
        };
    }
}
