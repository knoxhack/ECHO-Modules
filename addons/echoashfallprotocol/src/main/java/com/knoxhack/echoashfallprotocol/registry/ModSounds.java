package com.knoxhack.echoashfallprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import java.util.function.Supplier;

/**
 * Sound registry for ECHO: ASHFALL PROTOCOL.
 */
public class ModSounds {
    public static final Object SOUNDS =
            EchoBackendRegistryBridge.create(BuiltInRegistries.SOUND_EVENT, EchoAshfallProtocol.MODID);

    public static final Supplier<SoundEvent> ECHO_MESSAGE = registerSound("ui.echo_message");
    public static final Supplier<SoundEvent> ECHO_COMPLETE = registerSound("ui.echo_complete");
    public static final Supplier<SoundEvent> JAM_ALERT = registerSound("machine.jam_alert");
    public static final Supplier<SoundEvent> RADIATION_STORM = registerSound("event.radiation_storm");
    public static final Supplier<SoundEvent> TOXIC_STORM = registerSound("event.toxic_storm");
    public static final Supplier<SoundEvent> ASH_STORM = registerSound("event.ash_storm");
    public static final Supplier<SoundEvent> CRYO_FRONT = registerSound("event.cryo_front");
    public static final Supplier<SoundEvent> NEXUS_SURGE = registerSound("event.nexus_surge");
    public static final Supplier<SoundEvent> BLACKOUT = registerSound("event.blackout");

    private static Supplier<SoundEvent> registerSound(String name) {
        return EchoBackendRegistryBridge.register(SOUNDS, name,
                () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, name)));
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(SOUNDS, eventBus);
    }
}
