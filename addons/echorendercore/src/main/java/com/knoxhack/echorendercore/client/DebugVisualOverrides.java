package com.knoxhack.echorendercore.client;

import com.knoxhack.echorendercore.api.VisualState;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class DebugVisualOverrides {
   private static final Map<UUID, Override> ENTITY_OVERRIDES = new HashMap<>();
   private static final Map<BlockKey, Override> BLOCK_OVERRIDES = new HashMap<>();
   private static boolean missingPartWarnings;
   private static boolean hudEnabled;
   private static boolean anchorsEnabled;

   private DebugVisualOverrides() {
   }

   public static void setEntity(UUID uuid, VisualState state, int seconds) {
      if (uuid != null && state != null) {
         ENTITY_OVERRIDES.put(uuid, new Override(state, expiresAt(seconds)));
      }
   }

   public static void setBlock(Level level, BlockPos pos, VisualState state, int seconds) {
      if (level != null && pos != null && state != null) {
         BLOCK_OVERRIDES.put(new BlockKey(level.dimension(), pos.immutable()), new Override(state, expiresAt(seconds)));
      }
   }

   public static Optional<VisualState> entity(UUID uuid) {
      if (uuid == null) {
         return Optional.empty();
      }
      return override(ENTITY_OVERRIDES, uuid);
   }

   public static Optional<VisualState> block(Level level, BlockPos pos) {
      if (level == null || pos == null) {
         return Optional.empty();
      }
      return override(BLOCK_OVERRIDES, new BlockKey(level.dimension(), pos.immutable()));
   }

   public static boolean missingPartWarnings() {
      return missingPartWarnings;
   }

   public static void setMissingPartWarnings(boolean enabled) {
      missingPartWarnings = enabled;
   }

   public static boolean hudEnabled() {
      return hudEnabled;
   }

   public static void setHudEnabled(boolean enabled) {
      hudEnabled = enabled;
   }

   public static boolean anchorsEnabled() {
      return anchorsEnabled;
   }

   public static void setAnchorsEnabled(boolean enabled) {
      anchorsEnabled = enabled;
   }

   private static <T> Optional<VisualState> override(Map<T, Override> overrides, T key) {
      Override value = overrides.get(key);
      if (value == null) {
         return Optional.empty();
      }
      if (value.expired()) {
         overrides.remove(key);
         return Optional.empty();
      }
      return Optional.of(value.state());
   }

   private static long expiresAt(int seconds) {
      return seconds <= 0 ? Long.MAX_VALUE : System.currentTimeMillis() + seconds * 1000L;
   }

   private record Override(VisualState state, long expiresAt) {
      private boolean expired() {
         return System.currentTimeMillis() > expiresAt;
      }
   }

   private record BlockKey(ResourceKey<Level> dimension, BlockPos pos) {
   }
}
