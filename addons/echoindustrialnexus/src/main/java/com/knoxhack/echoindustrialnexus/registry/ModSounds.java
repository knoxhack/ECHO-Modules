package com.knoxhack.echoindustrialnexus.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoindustrialnexus.EchoIndustrialNexus;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
   private static final Object SOUNDS = EchoBackendRegistryBridge.create(Registries.SOUND_EVENT, EchoIndustrialNexus.MODID);
   public static final EchoBackendRegistryEntry<SoundEvent> MACHINE_HUM = sound("machine_hum");
   public static final EchoBackendRegistryEntry<SoundEvent> GRINDER_LOOP = sound("grinder_loop");
   public static final EchoBackendRegistryEntry<SoundEvent> SHREDDER_LOOP = sound("shredder_loop");
   public static final EchoBackendRegistryEntry<SoundEvent> PIPE_TRANSFER = sound("pipe_transfer");
   public static final EchoBackendRegistryEntry<SoundEvent> OVERHEAT_ALARM = sound("overheat_alarm");
   public static final EchoBackendRegistryEntry<SoundEvent> SCRUBBER_OPERATION = sound("scrubber_operation");
   public static final EchoBackendRegistryEntry<SoundEvent> WARDEN_PHASE = sound("warden_phase");
   public static final EchoBackendRegistryEntry<SoundEvent> POI_AMBIENCE = sound("poi_ambience");

   private ModSounds() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(SOUNDS, eventBus);
   }

   private static EchoBackendRegistryEntry<SoundEvent> sound(String name) {
      Identifier id = Identifier.fromNamespaceAndPath(EchoIndustrialNexus.MODID, name);
      return EchoBackendRegistryBridge.register(SOUNDS, name, () -> SoundEvent.createVariableRangeEvent(id));
   }
}
