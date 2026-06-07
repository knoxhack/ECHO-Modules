package com.knoxhack.echoblackboxprotocol.world;

import com.knoxhack.echoblackboxprotocol.EchoBlackboxProtocol;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxDungeon;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class ModDimensions {
   private static final Map<BlackboxDungeon, ResourceKey<Level>> KEYS = new EnumMap<>(BlackboxDungeon.class);
   public static final ResourceKey<Level> BLACKBOX_VAULT = key("blackbox_vault");
   public static final ResourceKey<Level> COMMAND_BUNKER = key("command_bunker");
   public static final ResourceKey<Level> MEMORY_LABYRINTH = key("memory_labyrinth");
   public static final ResourceKey<Level> CORE_ACCESS_TEMPLE = key("core_access_temple");
   public static final ResourceKey<Level> NEXUS_CORE_CHAMBER = key("nexus_core_chamber");

   private ModDimensions() {
   }

   public static ResourceKey<Level> key(BlackboxDungeon dungeon) {
      return KEYS.get(dungeon);
   }

   public static ServerLevel resolve(MinecraftServer server, BlackboxDungeon dungeon, ServerLevel fallback) {
      ServerLevel level = server.getLevel(key(dungeon));
      return level == null ? fallback : level;
   }

   private static ResourceKey<Level> key(String path) {
      return ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(EchoBlackboxProtocol.MODID, path));
   }

   static {
      KEYS.put(BlackboxDungeon.VAULT, BLACKBOX_VAULT);
      KEYS.put(BlackboxDungeon.BUNKER, COMMAND_BUNKER);
      KEYS.put(BlackboxDungeon.LABYRINTH, MEMORY_LABYRINTH);
      KEYS.put(BlackboxDungeon.TEMPLE, CORE_ACCESS_TEMPLE);
      KEYS.put(BlackboxDungeon.CORE_CHAMBER, NEXUS_CORE_CHAMBER);
   }
}
