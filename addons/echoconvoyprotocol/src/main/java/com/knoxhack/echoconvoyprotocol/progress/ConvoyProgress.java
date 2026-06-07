package com.knoxhack.echoconvoyprotocol.progress;

import java.util.Optional;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class ConvoyProgress {
   private static final String ROOT = "echoconvoyprotocol";
   private final Player player;
   private final CompoundTag root;

   private ConvoyProgress(Player player, CompoundTag root) {
      this.player = player;
      this.root = root;
   }

   public static ConvoyProgress get(Player player) {
      if (player == null) {
         return new ConvoyProgress(null, new CompoundTag());
      }
      CompoundTag root = player.getPersistentData().getCompoundOrEmpty(ROOT);
      return new ConvoyProgress(player, root);
   }

   public String activeRouteId() {
      return root.getStringOr("active_route", "");
   }

   public void activate(Identifier routeId) {
      activate(routeId, null, null);
   }

   public void activate(Identifier routeId, BlockPos startPos, UUID vehicleId) {
      root.putString("active_route", routeId == null ? "" : routeId.toString());
      root.putInt("active_route_leg", 0);
      root.putInt("active_marker_count", 0);
      if (startPos == null) {
         root.putBoolean("active_route_start_set", false);
      } else {
         root.putBoolean("active_route_start_set", true);
         root.putInt("active_route_start_x", startPos.getX());
         root.putInt("active_route_start_y", startPos.getY());
         root.putInt("active_route_start_z", startPos.getZ());
      }
      root.putString("active_route_vehicle", vehicleId == null ? "" : vehicleId.toString());
      save();
   }

   public int activeRouteLeg() {
      return root.getIntOr("active_route_leg", 0);
   }

   public Optional<BlockPos> activeRouteStart() {
      if (!root.getBooleanOr("active_route_start_set", false)) {
         return Optional.empty();
      }
      return Optional.of(new BlockPos(
         root.getIntOr("active_route_start_x", 0),
         root.getIntOr("active_route_start_y", 0),
         root.getIntOr("active_route_start_z", 0)
      ));
   }

   public Optional<UUID> activeRouteVehicle() {
      String value = root.getStringOr("active_route_vehicle", "");
      if (value.isBlank()) {
         return Optional.empty();
      }
      try {
         return Optional.of(UUID.fromString(value));
      } catch (IllegalArgumentException exception) {
         return Optional.empty();
      }
   }

   public void pairActiveRouteVehicle(UUID vehicleId) {
      root.putString("active_route_vehicle", vehicleId == null ? "" : vehicleId.toString());
      save();
   }

   public boolean markerVisited(Identifier routeId, BlockPos pos) {
      return readSet("active_marker").contains(markerKey(routeId, pos));
   }

   public void markSignal(Identifier routeId, BlockPos pos) {
      Set<String> markers = readSet("active_marker");
      markers.add(markerKey(routeId, pos));
      writeSet("active_marker", markers);
      root.putInt("active_route_leg", markers.size());
      save();
   }

   public boolean checkpointCleared(Identifier routeId) {
      return routeId != null && readSet("cleared_checkpoint").contains(routeId.toString());
   }

   public void clearCheckpoint(Identifier routeId) {
      if (routeId == null) {
         return;
      }
      Set<String> checkpoints = readSet("cleared_checkpoint");
      checkpoints.add(routeId.toString());
      writeSet("cleared_checkpoint", checkpoints);
      save();
   }

   public void complete(Identifier routeId) {
      if (routeId == null) {
         return;
      }
      Set<String> completed = readSet("completed_route");
      completed.add(routeId.toString());
      writeSet("completed_route", completed);
      root.putString("active_route", "");
      root.putString("active_route_vehicle", "");
      root.putBoolean("active_route_start_set", false);
      root.putInt("active_route_leg", 0);
      root.putInt("active_marker_count", 0);
      save();
   }

   public boolean completed(Identifier routeId) {
      return routeId != null && readSet("completed_route").contains(routeId.toString());
   }

   public boolean claimed(Identifier routeId) {
      return routeId != null && readSet("claimed_route").contains(routeId.toString());
   }

   public void markClaimed(Identifier routeId) {
      if (routeId == null) {
         return;
      }
      Set<String> claimed = readSet("claimed_route");
      claimed.add(routeId.toString());
      writeSet("claimed_route", claimed);
      save();
   }

   public long threatCooldown() {
      return root.getLongOr("threat_cooldown", 0L);
   }

   public void setThreatCooldown(long gameTime) {
      root.putLong("threat_cooldown", gameTime);
      save();
   }

   public void activateBeacon(Identifier beaconId) {
      if (beaconId == null) {
         return;
      }
      Set<String> beacons = readSet("activated_beacon");
      beacons.add(beaconId.toString());
      writeSet("activated_beacon", beacons);
      save();
   }

   public boolean beaconActivated(Identifier beaconId) {
      return beaconId != null && readSet("activated_beacon").contains(beaconId.toString());
   }

   public boolean flag(String key) {
      return key != null && !key.isBlank() && root.getBoolean(key).orElse(false);
   }

   public int value(String key) {
      return key == null || key.isBlank() ? 0 : root.getIntOr(key, 0);
   }

   public void mark(String key) {
      if (key == null || key.isBlank()) {
         return;
      }
      root.putBoolean(key, true);
      save();
   }

   public void increment(String key) {
      if (key == null || key.isBlank()) {
         return;
      }
      root.putInt(key, root.getIntOr(key, 0) + 1);
      root.putBoolean(key + "_complete", true);
      save();
   }

   public void save() {
      player.getPersistentData().put(ROOT, root);
      if (player instanceof ServerPlayer serverPlayer) {
         serverPlayer.getPersistentData().put(ROOT, root);
      }
   }

   private Set<String> readSet(String prefix) {
      int count = root.getIntOr(prefix + "_count", 0);
      Set<String> values = new LinkedHashSet<>();
      for (int i = 0; i < count; i++) {
         String value = root.getStringOr(prefix + "_" + i, "");
         if (!value.isBlank()) {
            values.add(value);
         }
      }
      return values;
   }

   private void writeSet(String prefix, Set<String> values) {
      int index = 0;
      for (String value : values) {
         if (value != null && !value.isBlank()) {
            root.putString(prefix + "_" + index++, value);
         }
      }
      root.putInt(prefix + "_count", index);
   }

   private static String markerKey(Identifier routeId, BlockPos pos) {
      String route = routeId == null ? "unknown" : routeId.toString();
      return route + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
   }
}
