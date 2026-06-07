package com.knoxhack.echoarmory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record InstalledModules(List<String> modules) {
   public static final InstalledModules EMPTY = new InstalledModules(List.of());
   public static final Codec<InstalledModules> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.STRING.listOf().optionalFieldOf("modules", List.of()).forGetter(InstalledModules::modules)
   ).apply(instance, InstalledModules::new));
   public static final StreamCodec<RegistryFriendlyByteBuf, InstalledModules> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
      InstalledModules::modules,
      InstalledModules::new
   );

   public InstalledModules {
      modules = modules == null ? List.of() : modules.stream()
         .filter(value -> value != null && !value.isBlank())
         .map(String::strip)
         .distinct()
         .limit(8)
         .toList();
   }

   public boolean contains(String moduleId) {
      return moduleId != null && modules.contains(moduleId);
   }

   public InstalledModules with(String moduleId, int maxSlots) {
      if (moduleId == null || moduleId.isBlank() || contains(moduleId) || modules.size() >= Math.max(0, maxSlots)) {
         return this;
      }
      java.util.ArrayList<String> next = new java.util.ArrayList<>(modules);
      next.add(moduleId.strip());
      return new InstalledModules(next);
   }

   public InstalledModules withoutLast() {
      if (modules.isEmpty()) {
         return this;
      }
      return new InstalledModules(modules.subList(0, modules.size() - 1));
   }
}
