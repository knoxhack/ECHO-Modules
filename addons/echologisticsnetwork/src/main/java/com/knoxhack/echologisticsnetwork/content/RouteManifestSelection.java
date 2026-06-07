package com.knoxhack.echologisticsnetwork.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record RouteManifestSelection(String networkId) {
   public static final RouteManifestSelection GLOBAL = new RouteManifestSelection("global");

   public static final Codec<RouteManifestSelection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.STRING.optionalFieldOf("networkId", "global").forGetter(RouteManifestSelection::networkId)
   ).apply(instance, RouteManifestSelection::new));

   public static final StreamCodec<RegistryFriendlyByteBuf, RouteManifestSelection> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8,
      RouteManifestSelection::networkId,
      RouteManifestSelection::new
   );

   public RouteManifestSelection {
      networkId = sanitize(networkId);
   }

   public static String sanitize(String networkId) {
      if (networkId == null || networkId.isBlank()) {
         return "global";
      }
      String sanitized = networkId.strip().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-:.]", "_");
      return sanitized.isBlank() ? "global" : sanitized;
   }
}
