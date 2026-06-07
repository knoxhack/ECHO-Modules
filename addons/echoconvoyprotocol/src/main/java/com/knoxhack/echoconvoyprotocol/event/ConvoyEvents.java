package com.knoxhack.echoconvoyprotocol.event;

import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echoconvoyprotocol.content.ConvoyContent;
import com.knoxhack.echoconvoyprotocol.content.ConvoyRouteDefinition;
import com.knoxhack.echoconvoyprotocol.entity.ConvoyVehicleEntity;
import com.knoxhack.echoconvoyprotocol.progress.ConvoyProgress;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public final class ConvoyEvents {
   private ConvoyEvents() {
   }

   public static void onPlayerTick(Object event) {
      ServerPlayer player = EchoBackendWorldEventBridge.postTickServerPlayer(event);
      if (player == null || player.tickCount % 100 != 0) {
         return;
      }
      if (!(player.getVehicle() instanceof ConvoyVehicleEntity vehicle)) {
         return;
      }
      ConvoyProgress progress = ConvoyProgress.get(player);
      Identifier routeId = Identifier.tryParse(progress.activeRouteId());
      if (routeId == null) {
         return;
      }
      if (!vehicle.isOwner(player) || !pairedRouteVehicle(progress, vehicle, routeId)) {
         return;
      }
      Optional<ConvoyRouteDefinition> route = ConvoyContent.route(routeId);
      if (route.isEmpty() || !route.get().threat().enabled()) {
         return;
      }
      long now = player.level().getGameTime();
      if (now < progress.threatCooldown()) {
         return;
      }
      ConvoyRouteDefinition.ThreatSpec threat = route.get().threat();
      if (player.getRandom().nextInt(threat.chanceOneIn()) != 0) {
         return;
      }
      progress.setThreatCooldown(now + threat.cooldownTicks());
      triggerRoadAmbush(player, vehicle, route.get(), threat);
   }

   private static boolean pairedRouteVehicle(ConvoyProgress progress, ConvoyVehicleEntity vehicle, Identifier routeId) {
      Optional<UUID> activeVehicle = progress.activeRouteVehicle();
      if (activeVehicle.isPresent() && !activeVehicle.get().equals(vehicle.getUUID())) {
         return false;
      }
      String vehicleRoute = vehicle.activeRouteId();
      if (activeVehicle.isPresent()) {
         return vehicleRoute.isBlank() || vehicleRoute.equals(routeId.toString());
      }
      return vehicleRoute.equals(routeId.toString());
   }

   private static void triggerRoadAmbush(
      ServerPlayer player,
      ConvoyVehicleEntity vehicle,
      ConvoyRouteDefinition route,
      ConvoyRouteDefinition.ThreatSpec threat
   ) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(threat.entityType());
      if (type == null) {
         type = EntityType.ZOMBIE;
      }
      int count = threat.rollCount(player.getRandom());
      for (int i = 0; i < count; i++) {
         Entity entity = type.create(level, EntitySpawnReason.EVENT);
         if (entity != null) {
            double angle = player.getRandom().nextDouble() * Math.PI * 2.0D;
            Vec3 pos = player.position().add(Math.cos(angle) * threat.spawnRadius(), 0.0D, Math.sin(angle) * threat.spawnRadius());
            entity.setPos(pos.x, player.getY(), pos.z);
            if (entity instanceof Mob mob) {
               mob.setTarget(player);
            }
            level.addFreshEntity(entity);
         }
      }
      vehicle.applyHazardDamage(threat.vehicleDamage());
      player.sendSystemMessage(Component.literal("ECHO CONVOY // " + threat.warningFor(route)));
   }
}
