package com.knoxhack.echoblackboxprotocol.progression;

import com.knoxhack.echoblackboxprotocol.EchoBlackboxProtocol;
import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;

public enum BlackboxDungeon implements StringRepresentable {
   VAULT("vault", "Blackbox Vault", "Archive"),
   BUNKER("bunker", "Command Bunker", "Military"),
   LABYRINTH("labyrinth", "Memory Labyrinth", "Reality"),
   TEMPLE("temple", "Core Access Temple", "Machine-Temple"),
   CORE_CHAMBER("core_chamber", "Nexus Core Chamber", "Nexus");

   public static final Codec<BlackboxDungeon> CODEC = StringRepresentable.fromEnum(BlackboxDungeon::values);
   private final String serializedName;
   private final String displayName;
   private final String category;

   private BlackboxDungeon(String serializedName, String displayName, String category) {
      this.serializedName = serializedName;
      this.displayName = displayName;
      this.category = category;
   }

   public Identifier id() {
      return Identifier.fromNamespaceAndPath(EchoBlackboxProtocol.MODID, this.serializedName);
   }

   public String displayName() {
      return this.displayName;
   }

   public String category() {
      return this.category;
   }

   public String getSerializedName() {
      return this.serializedName;
   }
}
