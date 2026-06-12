package com.knoxhack.echoagriculturereclamation.integration;

import com.echoplatform.echocore.api.EchoMapLayer;
import com.echoplatform.echocore.api.EchoMapMarker;
import com.echoplatform.echocore.api.IMapDataProvider;
import com.echoplatform.echocore.api.IMapLayer;
import com.echoplatform.echocore.api.IMapMarker;
import com.knoxhack.echoagriculturereclamation.EchoAgricultureReclamation;
import com.knoxhack.echoagriculturereclamation.api.ReclamationFieldQuery;
import com.knoxhack.echoagriculturereclamation.api.ReclamationFieldSnapshot;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public enum ReclamationMapDataProvider implements IMapDataProvider {
   INSTANCE;

   private static final Identifier PROVIDER = id("provider/field_map");
   private static final Identifier LAYER_RESTORATION = id("layer/restoration");
   private static final Identifier LAYER_GREENHOUSE = id("layer/greenhouse");
   private static final Identifier LAYER_MISSIONS = id("layer/missions");

   @Override
   public Identifier providerId() {
      return PROVIDER;
   }

   @Override
   public List<IMapLayer> layers(Player player) {
      return List.of(
         new EchoMapLayer(LAYER_RESTORATION, "FIELD Restoration", 62, 0xFF92F7A6, true),
         new EchoMapLayer(LAYER_GREENHOUSE, "Greenhouse Zones", 63, 0xFF66E8FF, true),
         new EchoMapLayer(LAYER_MISSIONS, "Reclamation Leads", 64, 0xFFFFD166, true)
      );
   }

   @Override
   public List<IMapMarker> markers(Player player) {
      ReclamationFieldSnapshot snapshot = ReclamationFieldQuery.player(player);
      if (snapshot == null) {
         return List.of();
      }
      List<IMapMarker> markers = new ArrayList<>();
      markers.add(new EchoMapMarker(
         id("marker/restoration/" + snapshot.chunk().x() + "_" + snapshot.chunk().z()),
         LAYER_RESTORATION,
         PROVIDER,
         IMapMarker.MarkerKind.REGION,
         snapshot.restored() ? IMapMarker.MarkerState.CHECKED : IMapMarker.MarkerState.DISCOVERED,
         "FIELD Restoration " + snapshot.restorationScore() + "%",
         snapshot.soilState().displayName() + ". " + snapshot.nextAction(),
         snapshot.dimension(),
         snapshot.center().getX() + 0.5D,
         snapshot.center().getY(),
         snapshot.center().getZ() + 0.5D,
         48.0F,
         null,
         null,
         -1,
         true
      ));
      if (snapshot.hasController()) {
         markers.add(new EchoMapMarker(
            id("marker/greenhouse/" + snapshot.controllerPos().getX() + "_" + snapshot.controllerPos().getZ()),
            LAYER_GREENHOUSE,
            PROVIDER,
            IMapMarker.MarkerKind.BASE_OUTPOST,
            "safe".equals(snapshot.greenhouseQuality()) ? IMapMarker.MarkerState.CHECKED : IMapMarker.MarkerState.DISCOVERED,
            "Greenhouse " + snapshot.greenhouseQuality(),
            snapshot.cropTargetCount() + " crop targets, " + snapshot.deployedDroneCount() + " drones, "
               + snapshot.serviceTargetCount() + " service targets.",
            snapshot.dimension(),
            snapshot.controllerPos().getX() + 0.5D,
            snapshot.controllerPos().getY(),
            snapshot.controllerPos().getZ() + 0.5D,
            24.0F,
            null,
            null,
            -1,
            true
         ));
      }
      markers.add(new EchoMapMarker(
         id("marker/mission/field_reclamation"),
         LAYER_MISSIONS,
         PROVIDER,
         IMapMarker.MarkerKind.MISSION,
         IMapMarker.MarkerState.DISCOVERED,
         "FIELD > Reclamation",
         "Seeds " + snapshot.knownSeedCount() + ", food " + snapshot.foodSecurity() + "%. " + snapshot.nextAction(),
         snapshot.dimension(),
         snapshot.center().getX() + 0.5D,
         snapshot.center().getY(),
         snapshot.center().getZ() + 0.5D,
         0.0F,
         null,
         ReclamationTerminalIds.CHAPTER,
         2,
         true
      ));
      return List.copyOf(markers);
   }

   @Override
   public boolean refresh(ServerPlayer player, String reason) {
      return ReclamationFieldQuery.player(player) != null;
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(EchoAgricultureReclamation.MODID, path);
   }
}
