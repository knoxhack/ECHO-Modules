package com.knoxhack.echoblackboxprotocol.world;

import com.knoxhack.echoblackboxprotocol.EchoBlackboxProtocol;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxEnding;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class BlackboxWorldData extends SavedData {
   private static final Codec<BlockPos> BLOCK_POS_CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            Codec.INT.fieldOf("x").forGetter(Vec3i::getX), Codec.INT.fieldOf("y").forGetter(Vec3i::getY), Codec.INT.fieldOf("z").forGetter(Vec3i::getZ)
         )
         .apply(instance, BlockPos::new)
   );
   public static final Codec<BlackboxWorldData> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            BlackboxEnding.CODEC.optionalFieldOf("ending", BlackboxEnding.NONE).forGetter(data -> data.ending),
            Codec.BOOL.optionalFieldOf("globalApplied", false).forGetter(data -> data.globalApplied),
            Codec.LONG.optionalFieldOf("choiceTime", -1L).forGetter(data -> data.choiceTime),
            BLOCK_POS_CODEC.optionalFieldOf("corePos", BlockPos.ZERO).forGetter(data -> data.corePos),
            Codec.STRING.optionalFieldOf("playerName", "").forGetter(data -> data.playerName)
         )
         .apply(instance, (ending, globalApplied, choiceTime, corePos, playerName) -> {
            BlackboxWorldData data = new BlackboxWorldData();
            data.ending = ending;
            data.globalApplied = globalApplied;
            data.choiceTime = choiceTime;
            data.corePos = corePos == null ? BlockPos.ZERO : corePos;
            data.playerName = playerName == null ? "" : playerName;
            return data;
         })
   );
   public static final SavedDataType<BlackboxWorldData> TYPE = new SavedDataType(
      Identifier.fromNamespaceAndPath(EchoBlackboxProtocol.MODID, "blackbox_world"), BlackboxWorldData::new, CODEC
   );
   private BlackboxEnding ending = BlackboxEnding.NONE;
   private boolean globalApplied = false;
   private long choiceTime = -1L;
   private BlockPos corePos = BlockPos.ZERO;
   private String playerName = "";

   public static BlackboxWorldData get(ServerLevel level) {
      return (BlackboxWorldData)level.getDataStorage().computeIfAbsent(TYPE);
   }

   public BlackboxEnding ending() {
      return this.ending;
   }

   public boolean hasEnding() {
      return this.ending != BlackboxEnding.NONE;
   }

   public boolean globalApplied() {
      return this.globalApplied;
   }

   public void recordEnding(BlackboxEnding ending, BlockPos pos, String playerName) {
      if (ending != null && ending != BlackboxEnding.NONE && !this.hasEnding()) {
         this.ending = ending;
         this.corePos = pos == null ? BlockPos.ZERO : new BlockPos(pos.getX(), pos.getY(), pos.getZ());
         this.playerName = playerName == null ? "" : playerName;
         this.choiceTime = System.currentTimeMillis();
         this.setDirty();
      }
   }

   public void markGlobalApplied() {
      this.globalApplied = true;
      this.setDirty();
   }

   public String statusLine() {
      return !this.hasEnding() ? "Blackbox ending unresolved." : "Blackbox ending " + this.ending.displayName() + " selected by " + this.playerName + ".";
   }
}
