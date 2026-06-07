package com.knoxhack.echoconvoyprotocol.world;

import com.knoxhack.echoconvoyprotocol.EchoConvoyProtocol;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class ConvoyRouteMarkerIndex extends SavedData {
   private static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.STRING.fieldOf("route").forGetter(entry -> entry.routeId().toString()),
      Codec.STRING.optionalFieldOf("leg", "destination").forGetter(Entry::legId),
      Codec.INT.optionalFieldOf("order", 0).forGetter(Entry::order),
      Codec.INT.fieldOf("x").forGetter(entry -> entry.pos().getX()),
      Codec.INT.fieldOf("y").forGetter(entry -> entry.pos().getY()),
      Codec.INT.fieldOf("z").forGetter(entry -> entry.pos().getZ())
   ).apply(instance, Entry::fromCodec));

   public static final Codec<ConvoyRouteMarkerIndex> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      ENTRY_CODEC.listOf().optionalFieldOf("markers", List.of()).forGetter(ConvoyRouteMarkerIndex::entries)
   ).apply(instance, ConvoyRouteMarkerIndex::new));

   public static final SavedDataType<ConvoyRouteMarkerIndex> TYPE = new SavedDataType<>(
      Identifier.fromNamespaceAndPath(EchoConvoyProtocol.MODID, "convoy_route_markers"),
      ConvoyRouteMarkerIndex::new,
      CODEC
   );

   private final List<Entry> entries = new ArrayList<>();

   public ConvoyRouteMarkerIndex() {
   }

   private ConvoyRouteMarkerIndex(List<Entry> entries) {
      this.entries.addAll(entries == null ? List.of() : entries.stream().filter(entry -> entry != null && entry.routeId() != null).toList());
   }

   public static ConvoyRouteMarkerIndex get(ServerLevel level) {
      return level.getDataStorage().computeIfAbsent(TYPE);
   }

   public void record(Identifier routeId, String legId, int order, BlockPos pos) {
      if (routeId == null || pos == null) {
         return;
      }
      Entry entry = new Entry(routeId, legId, order, pos);
      entries.removeIf(existing -> existing.routeId().equals(entry.routeId()) && existing.legId().equals(entry.legId()));
      entries.add(entry);
      setDirty();
   }

   public List<Entry> routeEntries(Identifier routeId) {
      return entries.stream()
         .filter(entry -> entry.routeId().equals(routeId))
         .sorted(Comparator.comparingInt(Entry::order).thenComparing(entry -> entry.legId()))
         .toList();
   }

   public List<Entry> entries() {
      return List.copyOf(entries);
   }

   public record Entry(Identifier routeId, String legId, int order, BlockPos pos) {
      public Entry {
         legId = legId == null || legId.isBlank() ? "destination" : legId.strip();
         order = Math.max(0, order);
         pos = pos == null ? BlockPos.ZERO : pos.immutable();
      }

      private static Entry fromCodec(String route, String leg, int order, int x, int y, int z) {
         Identifier routeId = Identifier.tryParse(route);
         return new Entry(routeId, leg, order, new BlockPos(x, y, z));
      }
   }
}
