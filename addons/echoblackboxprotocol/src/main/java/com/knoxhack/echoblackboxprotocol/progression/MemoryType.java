package com.knoxhack.echoblackboxprotocol.progression;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum MemoryType implements StringRepresentable {
   PERSONAL("personal", "Personal Logs"),
   ECHO("echo", "ECHO Logs"),
   SECURITY("security", "Security Logs"),
   COMMAND("command", "Command Logs"),
   CORE("core", "Core Logs"),
   DELETED("deleted", "Deleted Logs");

   public static final Codec<MemoryType> CODEC = StringRepresentable.fromEnum(MemoryType::values);
   public static final StreamCodec<RegistryFriendlyByteBuf, MemoryType> STREAM_CODEC = ByteBufCodecs.idMapper(MemoryType::byId, Enum::ordinal).cast();
   private static final MemoryType[] BY_ID = values();
   private final String serializedName;
   private final String displayName;

   private MemoryType(String serializedName, String displayName) {
      this.serializedName = serializedName;
      this.displayName = displayName;
   }

   public String displayName() {
      return this.displayName;
   }

   public String getSerializedName() {
      return this.serializedName;
   }

   public static MemoryType byName(String name) {
      for (MemoryType type : values()) {
         if (type.serializedName.equalsIgnoreCase(name) || type.name().equalsIgnoreCase(name)) {
            return type;
         }
      }

      return PERSONAL;
   }

   private static MemoryType byId(int id) {
      return id >= 0 && id < BY_ID.length ? BY_ID[id] : PERSONAL;
   }
}
