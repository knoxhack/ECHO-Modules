package com.knoxhack.echoindustrialnexus.integration;

import com.knoxhack.echomultiblockcore.api.LensMultiblockScan;
import com.knoxhack.echomultiblockcore.api.MultiblockDataCoreProvider;
import com.knoxhack.echomultiblockcore.api.MultiblockIntegrationServices;
import com.knoxhack.echomultiblockcore.api.MultiblockMapMarkerProvider;
import com.knoxhack.echomultiblockcore.api.MultiblockMapMarkerSnapshot;
import com.knoxhack.echomultiblockcore.api.MultiblockRuntimeSnapshot;
import com.knoxhack.echomultiblockcore.api.MultiblockScanProvider;
import com.knoxhack.echomultiblockcore.api.MultiblockStatusSnapshot;
import com.knoxhack.echomultiblockcore.api.MultiblockTerminalProvider;
import com.knoxhack.echomultiblockcore.block.entity.MultiblockControllerBlockEntity;
import com.knoxhack.echomultiblockcore.integration.DefaultMultiblockIntegrationProvider;
import com.knoxhack.echoindustrialnexus.EchoIndustrialNexus;
import com.knoxhack.echoindustrialnexus.factory.IndustrialFactoryAlertLevel;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class IndustrialMultiblockIntegrationProvider {
   private static final MultiblockTerminalProvider TERMINAL = new IndustrialTerminalProvider();
   private static final MultiblockScanProvider SCAN = new IndustrialScanProvider();
   private static final MultiblockDataCoreProvider DATA_CORE = new IndustrialDataCoreProvider();
   private static final MultiblockMapMarkerProvider MAP_MARKERS = new IndustrialMapMarkerProvider();

   private IndustrialMultiblockIntegrationProvider() {
   }

   public static void register() {
      MultiblockIntegrationServices.registerTerminalProvider(TERMINAL);
      MultiblockIntegrationServices.registerScanProvider(SCAN);
      MultiblockIntegrationServices.registerDataCoreProvider(DATA_CORE);
      MultiblockIntegrationServices.registerMapMarkerProvider(MAP_MARKERS);
   }

   private static boolean industrial(Identifier id) {
      return id != null && EchoIndustrialNexus.MODID.equals(id.getNamespace());
   }

   private static final class IndustrialTerminalProvider implements MultiblockTerminalProvider {
      @Override
      public Identifier providerId() {
         return EchoIndustrialNexus.id("industrial_terminal_multiblocks");
      }

      @Override
      public List<MultiblockStatusSnapshot> snapshots(Player player) {
         return player instanceof ServerPlayer serverPlayer
            ? DefaultMultiblockIntegrationProvider.statusSnapshots((ServerLevel)serverPlayer.level()).stream()
               .filter(snapshot -> industrial(snapshot.definitionId()))
               .toList()
            : List.of();
      }
   }

   private static final class IndustrialScanProvider implements MultiblockScanProvider {
      @Override
      public Identifier providerId() {
         return EchoIndustrialNexus.id("industrial_lens_multiblocks");
      }

      @Override
      public Optional<LensMultiblockScan> scan(Player player, Level level, BlockPos pos) {
         if (level == null || level.isClientSide() || pos == null || !level.isLoaded(pos)) {
            return Optional.empty();
         }
         BlockEntity blockEntity = level.getBlockEntity(pos);
         if (blockEntity instanceof MultiblockControllerBlockEntity controller && industrial(controller.getMultiblockId())) {
            return Optional.of(controller.scanSnapshot());
         }
         return Optional.empty();
      }
   }

   private static final class IndustrialDataCoreProvider implements MultiblockDataCoreProvider {
      @Override
      public Identifier providerId() {
         return EchoIndustrialNexus.id("industrial_data_multiblocks");
      }

      @Override
      public List<MultiblockRuntimeSnapshot> snapshots(Player player) {
         return player instanceof ServerPlayer serverPlayer
            ? DefaultMultiblockIntegrationProvider.runtimeSnapshots((ServerLevel)serverPlayer.level()).stream()
               .filter(snapshot -> industrial(snapshot.definitionId()))
               .toList()
            : List.of();
      }
   }

   private static final class IndustrialMapMarkerProvider implements MultiblockMapMarkerProvider {
      @Override
      public Identifier providerId() {
         return EchoIndustrialNexus.id("industrial_map_multiblocks");
      }

      @Override
      public List<MultiblockMapMarkerSnapshot> markers(Player player) {
         return player instanceof ServerPlayer serverPlayer
            ? DefaultMultiblockIntegrationProvider.runtimeSnapshots((ServerLevel)serverPlayer.level()).stream()
               .filter(snapshot -> industrial(snapshot.definitionId()))
               .map(snapshot -> {
                  IndustrialFactoryAlertLevel alert = IndustrialFactoryAlertLevel.from(snapshot.state(),
                     snapshot.tasks(), snapshot.warningCount(), snapshot.completion());
                  Identifier markerId = EchoIndustrialNexus.id("marker/" + snapshot.definitionId().getPath()
                     + "/" + Long.toUnsignedString(snapshot.controllerPos().asLong()));
                  String summary = alert.name().toLowerCase(Locale.ROOT).replace('_', ' ')
                     + " / " + snapshot.role().name().toLowerCase(Locale.ROOT).replace('_', ' ')
                     + " / integrity " + Math.round(snapshot.integrity()) + "%"
                     + (snapshot.taskCount() > 0 ? " / queue " + snapshot.taskCount() : "")
                     + (snapshot.warningCount() > 0 ? " / warnings " + snapshot.warningCount() : "");
                  if (serverPlayer.level().getBlockEntity(snapshot.controllerPos()) instanceof com.knoxhack.echoindustrialnexus.block.entity.IndustrialMultiblockControllerBlockEntity controller) {
                     summary += " / restock " + (controller.logisticsAutoRestockEnabled() ? "on x" + controller.logisticsRestockTargetRuns() : "off");
                  }
                  return new MultiblockMapMarkerSnapshot(markerId, snapshot.definitionId(), snapshot.controllerPos(),
                     snapshot.dimension(), snapshot.role(), snapshot.state(), alert.color(), snapshot.displayName(), summary);
               })
               .toList()
            : List.of();
      }

      @Override
      public boolean refresh(ServerPlayer player, String reason) {
         return player != null;
      }
   }
}
