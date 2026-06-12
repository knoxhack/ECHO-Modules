package com.knoxhack.echologisticsnetwork.integration;

import com.knoxhack.echologisticsnetwork.block.LogisticsBlock.LogisticsKind;
import com.knoxhack.echologisticsnetwork.block.entity.LogisticsBlockEntity;
import com.knoxhack.echologisticsnetwork.content.LoadoutPreset;
import com.knoxhack.echologisticsnetwork.content.LogisticsContent;
import com.knoxhack.echologisticsnetwork.service.LogisticsNetworkService;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

final class LogisticsTerminalSupport {
   private static final int BASE_RADIUS = 24;
   private static final int INDUSTRIAL_RADIUS = 36;
   private static final int Y_RADIUS = 8;
   private static final List<LogisticsKind> REQUEST_TARGETS = List.of(
      LogisticsKind.LOADOUT_LOCKER,
      LogisticsKind.ROUTE_REQUESTER,
      LogisticsKind.AUTO_RESTOCK_STATION
   );

   private LogisticsTerminalSupport() {
   }

   static TerminalView view(@Nullable Player player) {
      if (player == null) {
         return TerminalView.empty("global");
      }
      return view(player.level(), player.blockPosition(), player);
   }

   static TerminalView view(Level level, BlockPos origin, @Nullable Player player) {
      if (level == null || origin == null) {
         return TerminalView.empty("global");
      }
      LogisticsBlockEntity anchor = nearestLogisticsBlock(level, origin, "");
      String networkId = anchor == null ? "global" : anchor.networkId();
      BlockPos networkOrigin = anchor == null ? origin : anchor.getBlockPos();
      List<LogisticsBlockEntity> blocks = logisticsBlocks(level, networkOrigin, networkId);
      LogisticsBlockEntity endpoint = chooseEndpoint(level, networkOrigin, networkId, "");
      LogisticsNetworkService.LogisticsSnapshot snapshot = LogisticsNetworkService.snapshot(level, networkOrigin, networkId, player);

      Optional<LogisticsBlockEntity> relay = blocks.stream()
         .filter(block -> block.kind() == LogisticsKind.REMOTE_REWARD_RELAY)
         .min(Comparator.comparingDouble(block -> block.getBlockPos().distSqr(networkOrigin)));
      Optional<LogisticsBlockEntity> depot = blocks.stream()
         .filter(block -> block.kind() == LogisticsKind.FACTION_TRADE_DEPOT)
         .min(Comparator.comparingDouble(block -> block.getBlockPos().distSqr(networkOrigin)));
      return new TerminalView(
         networkId,
         networkOrigin,
         snapshot,
         snapshot.blockCount(),
         endpoint,
         snapshot.endpointCount(),
         snapshot.dockOnline(),
         relay.orElse(null),
         snapshot.relayOnline(),
         snapshot.depotOnline(),
         snapshot.depotCooldown(),
         snapshot.selectedLoadoutId(),
         snapshot.selectedLoadoutTitle(),
         snapshot.selectedReady(),
         snapshot.selectedMissing(),
         snapshot.requestPayload()
      );
   }

   @Nullable
   static RequestTarget resolveRequestTarget(Player player, String payload) {
      if (player == null || player.level() == null) {
         return null;
      }
      PayloadSelection selection = parsePayload(payload);
      Level level = player.level();
      LogisticsBlockEntity anchor = nearestLogisticsBlock(level, player.blockPosition(), selection.networkId());
      String networkId = selection.networkId().isBlank()
         ? anchor == null ? "global" : anchor.networkId()
         : selection.networkId();
      BlockPos origin = anchor == null ? player.blockPosition() : anchor.getBlockPos();
      LogisticsBlockEntity target = chooseEndpoint(level, origin, networkId, selection.loadoutId());
      if (target == null) {
         return null;
      }
      String loadoutId = selection.loadoutId().isBlank() ? target.loadoutId() : selection.loadoutId();
      Optional<LoadoutPreset> preset = LogisticsContent.loadout(loadoutId);
      if (preset.isEmpty() || !targetAllowed(level, target.getBlockPos(), preset.get())) {
         return null;
      }
      return new RequestTarget(networkId, target, preset.get());
   }

   private static String firstReadyLoadout(LogisticsNetworkService.LogisticsSnapshot snapshot) {
      return snapshot.loadoutReadiness().stream()
         .filter(LogisticsNetworkService.LoadoutReadiness::ready)
         .map(row -> row.presetId().toString())
         .findFirst()
         .orElse(LogisticsContent.firstLoadoutId());
   }

   private static Optional<LogisticsNetworkService.LoadoutReadiness> readiness(LogisticsNetworkService.LogisticsSnapshot snapshot, String loadoutId) {
      if (loadoutId == null || loadoutId.isBlank()) {
         return Optional.empty();
      }
      return snapshot.loadoutReadiness().stream()
         .filter(row -> row.presetId().toString().equals(loadoutId))
         .findFirst();
   }

   @Nullable
   private static LogisticsBlockEntity chooseEndpoint(Level level, BlockPos origin, String networkId, String requestedLoadoutId) {
      return logisticsBlocks(level, origin, networkId).stream()
         .filter(LogisticsTerminalSupport::isRequestTarget)
         .filter(endpoint -> {
            String loadoutId = requestedLoadoutId == null || requestedLoadoutId.isBlank() ? endpoint.loadoutId() : requestedLoadoutId;
            return LogisticsContent.loadout(loadoutId).filter(preset -> targetAllowed(level, endpoint.getBlockPos(), preset)).isPresent();
         })
         .min(Comparator.comparingDouble(endpoint -> endpoint.getBlockPos().distSqr(origin)))
         .orElse(null);
   }

   @Nullable
   private static LogisticsBlockEntity nearestLogisticsBlock(Level level, BlockPos origin, String networkId) {
      return logisticsBlocks(level, origin, networkId).stream()
         .min(Comparator.comparingDouble(block -> block.getBlockPos().distSqr(origin)))
         .orElse(null);
   }

   private static List<LogisticsBlockEntity> logisticsBlocks(Level level, BlockPos origin, String networkId) {
      if (level == null || origin == null) {
         return List.of();
      }
      int radius = radius();
      return StreamSupport.stream(BlockPos.betweenClosed(origin.offset(-radius, -Y_RADIUS, -radius), origin.offset(radius, Y_RADIUS, radius)).spliterator(), false)
         .map(BlockPos::immutable)
         .map(level::getBlockEntity)
         .filter(LogisticsBlockEntity.class::isInstance)
         .map(LogisticsBlockEntity.class::cast)
         .filter(block -> networkMatches(networkId, block.networkId()))
         .sorted(Comparator.comparing(block -> block.getBlockPos().toShortString()))
         .toList();
   }

   private static int radius() {
      return EchoRuntimeModules.isLoaded("echoindustrialnexus") ? INDUSTRIAL_RADIUS : BASE_RADIUS;
   }

   private static boolean isRequestTarget(LogisticsBlockEntity block) {
      return block != null && REQUEST_TARGETS.contains(block.kind());
   }

   private static boolean targetAllowed(Level level, BlockPos targetPos, LoadoutPreset preset) {
      if (preset.targetBlockTypes().isEmpty()) {
         return true;
      }
      Identifier targetId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(targetPos).getBlock());
      return preset.targetBlockTypes().contains(targetId);
   }

   private static boolean networkMatches(String expected, String actual) {
      if (expected == null || expected.isBlank()) {
         return true;
      }
      String b = actual == null || actual.isBlank() ? "global" : actual;
      return expected.equals(b);
   }

   private static PayloadSelection parsePayload(String payload) {
      String value = payload == null ? "" : payload.strip();
      if (value.isBlank()) {
         return new PayloadSelection("", "");
      }
      int separator = value.indexOf('|');
      if (separator < 0) {
         return new PayloadSelection("", value);
      }
      return new PayloadSelection(value.substring(0, separator).strip(), value.substring(separator + 1).strip());
   }

   private record PayloadSelection(String networkId, String loadoutId) {
      private PayloadSelection {
         networkId = networkId == null ? "" : networkId;
         loadoutId = loadoutId == null ? "" : loadoutId;
      }
   }

   record RequestTarget(String networkId, LogisticsBlockEntity target, LoadoutPreset preset) {
   }

   public record TerminalView(
      String networkId,
      BlockPos origin,
      LogisticsNetworkService.LogisticsSnapshot snapshot,
      int blockCount,
      @Nullable LogisticsBlockEntity endpoint,
      int endpointCount,
      boolean dockOnline,
      @Nullable LogisticsBlockEntity relay,
      boolean relayOnline,
      boolean depotOnline,
      int depotCooldown,
      String selectedLoadoutId,
      String selectedLoadoutTitle,
      boolean selectedReady,
      int selectedMissing,
      String requestPayload
   ) {
      public TerminalView {
         networkId = networkId == null || networkId.isBlank() ? "global" : networkId;
         origin = origin == null ? BlockPos.ZERO : origin.immutable();
         snapshot = snapshot == null ? LogisticsNetworkService.LogisticsSnapshot.empty(networkId) : snapshot;
         selectedLoadoutId = selectedLoadoutId == null ? "" : selectedLoadoutId;
         selectedLoadoutTitle = selectedLoadoutTitle == null || selectedLoadoutTitle.isBlank() ? "None" : selectedLoadoutTitle;
         requestPayload = requestPayload == null ? "" : requestPayload;
      }

      static TerminalView empty(String networkId) {
         return new TerminalView(networkId, BlockPos.ZERO, LogisticsNetworkService.LogisticsSnapshot.empty(networkId),
            0, null, 0, false, null, false, false, 0, "", "None", false, 0, "");
      }

      boolean canRequest() {
         return endpoint != null && dockOnline && selectedReady && !selectedLoadoutId.isBlank();
      }
   }
}
