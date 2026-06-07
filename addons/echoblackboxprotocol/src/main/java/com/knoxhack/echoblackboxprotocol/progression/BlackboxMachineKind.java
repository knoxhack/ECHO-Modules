package com.knoxhack.echoblackboxprotocol.progression;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum BlackboxMachineKind implements StringRepresentable {
   BLACKBOX_DECODER("blackbox_decoder", "Blackbox Decoder"),
   MEMORY_PROJECTOR("memory_projector", "Memory Projector"),
   ARCHIVE_TERMINAL("archive_terminal", "Archive Terminal"),
   CORE_KEY_ASSEMBLER("core_key_assembler", "Core Key Assembler"),
   TRUTH_ENGINE("truth_engine", "Truth Engine"),
   MEMORY_STABILIZER("memory_stabilizer", "Memory Stabilizer"),
   PROTOCOL_EXTRACTOR("protocol_extractor", "Protocol Extractor");

   public static final Codec<BlackboxMachineKind> CODEC = StringRepresentable.fromEnum(BlackboxMachineKind::values);
   public static final StreamCodec<RegistryFriendlyByteBuf, BlackboxMachineKind> STREAM_CODEC = ByteBufCodecs.idMapper(BlackboxMachineKind::byId, Enum::ordinal)
      .cast();
   private static final BlackboxMachineKind[] BY_ID = values();
   private final String serializedName;
   private final String displayName;

   private BlackboxMachineKind(String serializedName, String displayName) {
      this.serializedName = serializedName;
      this.displayName = displayName;
   }

   public String displayName() {
      return this.displayName;
   }

   public String getSerializedName() {
      return this.serializedName;
   }

   private static BlackboxMachineKind byId(int id) {
      return id >= 0 && id < BY_ID.length ? BY_ID[id] : BLACKBOX_DECODER;
   }
}
