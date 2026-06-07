package com.knoxhack.echologisticsnetwork.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record RemoteRequestSelection(String networkId, String loadoutId, int targetX, int targetY, int targetZ, boolean hasTarget) {
   public static final RemoteRequestSelection EMPTY = new RemoteRequestSelection("", "", 0, 0, 0, false);

   public static final Codec<RemoteRequestSelection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.STRING.optionalFieldOf("networkId", "").forGetter(RemoteRequestSelection::networkId),
      Codec.STRING.optionalFieldOf("loadoutId", "").forGetter(RemoteRequestSelection::loadoutId),
      Codec.INT.optionalFieldOf("targetX", 0).forGetter(RemoteRequestSelection::targetX),
      Codec.INT.optionalFieldOf("targetY", 0).forGetter(RemoteRequestSelection::targetY),
      Codec.INT.optionalFieldOf("targetZ", 0).forGetter(RemoteRequestSelection::targetZ),
      Codec.BOOL.optionalFieldOf("hasTarget", false).forGetter(RemoteRequestSelection::hasTarget)
   ).apply(instance, RemoteRequestSelection::new));

   public static final StreamCodec<RegistryFriendlyByteBuf, RemoteRequestSelection> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8,
      RemoteRequestSelection::networkId,
      ByteBufCodecs.STRING_UTF8,
      RemoteRequestSelection::loadoutId,
      ByteBufCodecs.VAR_INT,
      RemoteRequestSelection::targetX,
      ByteBufCodecs.VAR_INT,
      RemoteRequestSelection::targetY,
      ByteBufCodecs.VAR_INT,
      RemoteRequestSelection::targetZ,
      ByteBufCodecs.BOOL,
      RemoteRequestSelection::hasTarget,
      RemoteRequestSelection::new
   );

   public RemoteRequestSelection(String networkId, String loadoutId) {
      this(networkId, loadoutId, 0, 0, 0, false);
   }

   public RemoteRequestSelection(String networkId, String loadoutId, BlockPos targetPos) {
      this(networkId, loadoutId, targetPos == null ? 0 : targetPos.getX(), targetPos == null ? 0 : targetPos.getY(), targetPos == null ? 0 : targetPos.getZ(), targetPos != null);
   }

   public RemoteRequestSelection {
      networkId = networkId == null || networkId.isBlank() ? "" : RouteManifestSelection.sanitize(networkId);
      loadoutId = loadoutId == null ? "" : loadoutId.strip();
      if (!hasTarget) {
         targetX = 0;
         targetY = 0;
         targetZ = 0;
      }
   }

   public boolean bound() {
      return !networkId.isBlank() && hasTarget;
   }

   public Optional<BlockPos> targetPos() {
      return hasTarget ? Optional.of(new BlockPos(targetX, targetY, targetZ)) : Optional.empty();
   }
}
