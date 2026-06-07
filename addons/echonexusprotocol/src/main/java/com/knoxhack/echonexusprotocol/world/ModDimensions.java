package com.knoxhack.echonexusprotocol.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class ModDimensions {
   public static final ResourceKey<Level> NEXUS = key("nexus");

   private ModDimensions() {
   }

   public static boolean isNexusLevel(Level level) {
      return level != null && level.dimension() == NEXUS;
   }

   public static ServerLevel resolve(MinecraftServer server, ServerLevel fallback) {
      ServerLevel level = server.getLevel(NEXUS);
      return level == null ? fallback : level;
   }

   public static ResourceKey<Level> key(String path) {
      return ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("echonexusprotocol", path));
   }
}
