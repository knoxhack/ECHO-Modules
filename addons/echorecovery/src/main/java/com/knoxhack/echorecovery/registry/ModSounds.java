package com.knoxhack.echorecovery.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echorecovery.EchoRecovery;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
    private static final Object SOUNDS =
        EchoBackendRegistryBridge.create(Registries.SOUND_EVENT, EchoRecovery.MODID);

    public static final EchoBackendRegistryEntry<SoundEvent> GRAVE_OPEN =
        sound("grave_open");
    public static final EchoBackendRegistryEntry<SoundEvent> GRAVE_CLOSE =
        sound("grave_close");
    public static final EchoBackendRegistryEntry<SoundEvent> GRAVE_RECOVER =
        sound("grave_recover");
    public static final EchoBackendRegistryEntry<SoundEvent> GRAVE_CREATE =
        sound("grave_create");

    private ModSounds() {}

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(SOUNDS, eventBus);
    }

    private static EchoBackendRegistryEntry<SoundEvent> sound(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(EchoRecovery.MODID, name);
        return EchoBackendRegistryBridge.register(SOUNDS, name, () -> SoundEvent.createVariableRangeEvent(id));
    }
}
