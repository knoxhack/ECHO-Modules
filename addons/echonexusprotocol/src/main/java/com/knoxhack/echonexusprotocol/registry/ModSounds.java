package com.knoxhack.echonexusprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echonexusprotocol.EchoNexusProtocol;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
   private static final Object SOUNDS = EchoBackendRegistryBridge.create(Registries.SOUND_EVENT, EchoNexusProtocol.MODID);
   public static final EchoBackendRegistryEntry<SoundEvent> MACHINE_PROCESS = sound("machine_process");
   public static final EchoBackendRegistryEntry<SoundEvent> SEAL_ACTIVATE = sound("seal_activate");
   public static final EchoBackendRegistryEntry<SoundEvent> FIELD_STABILIZE = sound("field_stabilize");
   public static final EchoBackendRegistryEntry<SoundEvent> CORRUPTION_LEAK = sound("corruption_leak");
   public static final EchoBackendRegistryEntry<SoundEvent> MONOLITH_ACTIVATE = sound("monolith_activate");
   public static final EchoBackendRegistryEntry<SoundEvent> REALITY_TEAR_PULSE = sound("reality_tear_pulse");
   public static final EchoBackendRegistryEntry<SoundEvent> WARDEN_PULSE = sound("warden_pulse");
   public static final EchoBackendRegistryEntry<SoundEvent> GUARDIAN_PHASE = sound("guardian_phase");
   public static final EchoBackendRegistryEntry<SoundEvent> ENDING_CHOICE = sound("ending_choice");

   private ModSounds() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(SOUNDS, eventBus);
   }

   private static EchoBackendRegistryEntry<SoundEvent> sound(String name) {
      Identifier id = Identifier.fromNamespaceAndPath(EchoNexusProtocol.MODID, name);
      return EchoBackendRegistryBridge.register(SOUNDS, name, () -> SoundEvent.createVariableRangeEvent(id));
   }
}
