package com.knoxhack.echoconvoyprotocol.integration;

import com.knoxhack.echoconvoyprotocol.EchoConvoyProtocol;
import com.knoxhack.echoconvoyprotocol.block.entity.ConvoyMultiblockControllerBlockEntity;
import com.knoxhack.echomultiblockcore.api.LensMultiblockScan;
import com.knoxhack.echomultiblockcore.api.MultiblockDataCoreProvider;
import com.knoxhack.echomultiblockcore.api.MultiblockIntegrationServices;
import com.knoxhack.echomultiblockcore.api.MultiblockMapMarkerProvider;
import com.knoxhack.echomultiblockcore.api.MultiblockMapMarkerSnapshot;
import com.knoxhack.echomultiblockcore.api.MultiblockRuntimeSnapshot;
import com.knoxhack.echomultiblockcore.api.MultiblockScanProvider;
import com.knoxhack.echomultiblockcore.api.MultiblockStatusSnapshot;
import com.knoxhack.echomultiblockcore.api.MultiblockTerminalProvider;
import com.knoxhack.echomultiblockcore.integration.DefaultMultiblockIntegrationProvider;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class ConvoyMultiblockProviders {
   private static boolean registered;

   private ConvoyMultiblockProviders() {
   }

   public static void register() {
      if (registered) {
         return;
      }
      registered = true;
      MultiblockIntegrationServices.registerTerminalProvider(new TerminalProvider());
      MultiblockIntegrationServices.registerScanProvider(new ScanProvider());
      MultiblockIntegrationServices.registerDataCoreProvider(new DataProvider());
      MultiblockIntegrationServices.registerMapMarkerProvider(new MapProvider());
   }

   private static boolean isConvoy(Identifier id) {
      return id != null && EchoConvoyProtocol.MODID.equals(id.getNamespace());
   }

   private static ServerLevel serverLevel(Player player) {
      return player != null && player.level() instanceof ServerLevel level ? level : null;
   }

   private static final class TerminalProvider implements MultiblockTerminalProvider {
      @Override
      public Identifier providerId() {
         return id("terminal");
      }

      @Override
      public List<MultiblockStatusSnapshot> snapshots(Player player) {
         ServerLevel level = serverLevel(player);
         return level == null ? List.of() : DefaultMultiblockIntegrationProvider.statusSnapshots(level).stream()
            .filter(snapshot -> isConvoy(snapshot.definitionId()))
            .toList();
      }
   }

   private static final class ScanProvider implements MultiblockScanProvider {
      @Override
      public Identifier providerId() {
         return id("scan");
      }

      @Override
      public Optional<LensMultiblockScan> scan(Player player, Level level, BlockPos pos) {
         if (level != null && pos != null && level.getBlockEntity(pos) instanceof ConvoyMultiblockControllerBlockEntity controller) {
            return Optional.of(controller.scanSnapshot());
         }
         return Optional.empty();
      }
   }

   private static final class DataProvider implements MultiblockDataCoreProvider {
      @Override
      public Identifier providerId() {
         return id("data_core");
      }

      @Override
      public List<MultiblockRuntimeSnapshot> snapshots(Player player) {
         ServerLevel level = serverLevel(player);
         return level == null ? List.of() : DefaultMultiblockIntegrationProvider.runtimeSnapshots(level).stream()
            .filter(snapshot -> isConvoy(snapshot.definitionId()))
            .toList();
      }
   }

   private static final class MapProvider implements MultiblockMapMarkerProvider {
      @Override
      public Identifier providerId() {
         return id("map_markers");
      }

      @Override
      public List<MultiblockMapMarkerSnapshot> markers(Player player) {
         ServerLevel level = serverLevel(player);
         return level == null ? List.of() : DefaultMultiblockIntegrationProvider.markerSnapshots(level).stream()
            .filter(marker -> isConvoy(marker.definitionId()))
            .toList();
      }

      @Override
      public boolean refresh(ServerPlayer player, String reason) {
         return true;
      }
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(EchoConvoyProtocol.MODID, "multiblock_" + path);
   }
}
