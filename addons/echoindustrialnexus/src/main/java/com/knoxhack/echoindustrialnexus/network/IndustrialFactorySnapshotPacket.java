package com.knoxhack.echoindustrialnexus.network;

import com.knoxhack.echoindustrialnexus.EchoIndustrialNexus;
import com.knoxhack.echoindustrialnexus.block.entity.IndustrialMultiblockControllerBlockEntity;
import com.knoxhack.echoindustrialnexus.factory.IndustrialFactoryAlertLevel;
import com.knoxhack.echomultiblockcore.api.AutomationRecipeRegistry;
import com.knoxhack.echomultiblockcore.api.MultiblockAutomationRecipe;
import com.knoxhack.echomultiblockcore.api.MultiblockIntegrationServices;
import com.knoxhack.echomultiblockcore.api.MultiblockRuntimeSnapshot;
import com.knoxhack.echomultiblockcore.api.MultiblockTaskState;
import com.knoxhack.echomultiblockcore.api.TaskExecutionSnapshot;
import com.knoxhack.echomultiblockcore.integration.DefaultMultiblockIntegrationProvider;
import com.knoxhack.echonetcore.api.EchoPayloadCodecs;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public record IndustrialFactorySnapshotPacket(List<Entry> entries, String statusLine, long gameTime)
   implements CustomPacketPayload {
   private static final int MAX_ENTRIES = 64;
   private static final int MAX_RECIPES = 16;
   private static final int MAX_TEXT = 192;

   public static final Identifier ID = EchoIndustrialNexus.id("factory_snapshot");
   public static final Type<IndustrialFactorySnapshotPacket> TYPE = new Type<>(ID);
   public static final StreamCodec<RegistryFriendlyByteBuf, IndustrialFactorySnapshotPacket> CODEC =
      StreamCodec.of(IndustrialFactorySnapshotPacket::write, IndustrialFactorySnapshotPacket::read);

   public IndustrialFactorySnapshotPacket {
      entries = List.copyOf(entries == null ? List.of() : entries.stream().limit(MAX_ENTRIES).toList());
      statusLine = clean(statusLine, "Factory Command awaiting sync.");
      gameTime = Math.max(0L, gameTime);
   }

   public static IndustrialFactorySnapshotPacket current(ServerPlayer player) {
      if (player == null) {
         return new IndustrialFactorySnapshotPacket(List.of(), "Factory Command offline.", 0L);
      }
      List<Entry> entries = industrialSnapshots(player).stream()
         .filter(snapshot -> snapshot.definitionId() != null
            && EchoIndustrialNexus.MODID.equals(snapshot.definitionId().getNamespace()))
         .sorted(Comparator.comparing((MultiblockRuntimeSnapshot snapshot) -> snapshot.displayName())
            .thenComparing(snapshot -> snapshot.controllerPos().asLong()))
         .limit(MAX_ENTRIES)
         .map(snapshot -> Entry.from(player, snapshot))
         .toList();
      long blocked = entries.stream().filter(entry -> "BLOCKED".equals(entry.alertLevel())).count();
      long active = entries.stream().filter(entry -> "ACTIVE".equals(entry.alertLevel())).count();
      String status = "Facilities " + entries.size() + " / active " + active + " / blocked " + blocked
         + " / t+" + player.level().getGameTime();
      return new IndustrialFactorySnapshotPacket(entries, status, player.level().getGameTime());
   }

   private static List<MultiblockRuntimeSnapshot> industrialSnapshots(ServerPlayer player) {
      Map<String, MultiblockRuntimeSnapshot> snapshots = new LinkedHashMap<>();
      for (MultiblockRuntimeSnapshot snapshot : MultiblockIntegrationServices.dataSnapshots(player)) {
         putSnapshot(snapshots, snapshot);
      }
      if (player != null && player.level() instanceof ServerLevel level) {
         for (MultiblockRuntimeSnapshot snapshot : DefaultMultiblockIntegrationProvider.runtimeSnapshots(level)) {
            putSnapshot(snapshots, snapshot);
         }
      }
      return List.copyOf(snapshots.values());
   }

   private static void putSnapshot(Map<String, MultiblockRuntimeSnapshot> snapshots, MultiblockRuntimeSnapshot snapshot) {
      if (snapshots == null || snapshot == null || snapshot.definitionId() == null || snapshot.controllerPos() == null) {
         return;
      }
      snapshots.putIfAbsent(snapshot.definitionId() + "@" + snapshot.controllerPos().asLong(), snapshot);
   }

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   private static void write(RegistryFriendlyByteBuf buffer, IndustrialFactorySnapshotPacket packet) {
      buffer.writeVarInt(packet.entries().size());
      for (Entry entry : packet.entries()) {
         EchoPayloadCodecs.writeIdentifier(buffer, entry.definitionId());
         buffer.writeUtf(entry.displayName(), MAX_TEXT);
         buffer.writeUtf(entry.alertLevel(), 32);
         buffer.writeInt(entry.alertColor());
         buffer.writeUtf(entry.state(), 32);
         buffer.writeFloat(entry.integrity());
         buffer.writeDouble(entry.completion());
         buffer.writeBlockPos(entry.controllerPos());
         buffer.writeUtf(entry.dimension(), EchoPayloadCodecs.ID);
         buffer.writeVarInt(entry.robotCount());
         buffer.writeVarInt(entry.taskCount());
         buffer.writeVarInt(entry.queueCapacity());
         buffer.writeUtf(entry.activeTask(), MAX_TEXT);
         buffer.writeVarInt(entry.activeProgress());
         buffer.writeVarInt(entry.activeDuration());
         buffer.writeUtf(entry.blockedReason(), MAX_TEXT);
         buffer.writeVarInt(entry.warningCount());
         buffer.writeUtf(entry.firstWarning(), MAX_TEXT);
         buffer.writeBoolean(entry.logisticsAutoRestockEnabled());
         buffer.writeVarInt(entry.logisticsRestockTargetRuns());
         buffer.writeUtf(entry.logisticsRestockStatus(), MAX_TEXT);
         buffer.writeVarInt(entry.recipeIds().size());
         for (Identifier recipeId : entry.recipeIds()) {
            EchoPayloadCodecs.writeIdentifier(buffer, recipeId);
         }
      }
      buffer.writeUtf(packet.statusLine(), MAX_TEXT);
      buffer.writeVarLong(packet.gameTime());
   }

   private static IndustrialFactorySnapshotPacket read(RegistryFriendlyByteBuf buffer) {
      int count = Math.max(0, Math.min(MAX_ENTRIES, buffer.readVarInt()));
      List<Entry> entries = new ArrayList<>();
      for (int i = 0; i < count; i++) {
         Identifier definitionId = EchoPayloadCodecs.readIdentifier(buffer);
         String displayName = buffer.readUtf(MAX_TEXT);
         String alertLevel = buffer.readUtf(32);
         int alertColor = buffer.readInt();
         String state = buffer.readUtf(32);
         float integrity = buffer.readFloat();
         double completion = buffer.readDouble();
         BlockPos controllerPos = buffer.readBlockPos();
         String dimension = buffer.readUtf(EchoPayloadCodecs.ID);
         int robotCount = buffer.readVarInt();
         int taskCount = buffer.readVarInt();
         int queueCapacity = buffer.readVarInt();
         String activeTask = buffer.readUtf(MAX_TEXT);
         int activeProgress = buffer.readVarInt();
         int activeDuration = buffer.readVarInt();
         String blockedReason = buffer.readUtf(MAX_TEXT);
         int warningCount = buffer.readVarInt();
         String firstWarning = buffer.readUtf(MAX_TEXT);
         boolean logisticsAutoRestockEnabled = buffer.readBoolean();
         int logisticsRestockTargetRuns = buffer.readVarInt();
         String logisticsRestockStatus = buffer.readUtf(MAX_TEXT);
         int recipeCount = Math.max(0, Math.min(MAX_RECIPES, buffer.readVarInt()));
         List<Identifier> recipes = new ArrayList<>();
         for (int recipe = 0; recipe < recipeCount; recipe++) {
            recipes.add(EchoPayloadCodecs.readIdentifier(buffer));
         }
         entries.add(new Entry(definitionId, displayName, alertLevel, alertColor, state, integrity, completion,
            controllerPos, dimension, robotCount, taskCount, queueCapacity, activeTask, activeProgress,
            activeDuration, blockedReason, warningCount, firstWarning, logisticsAutoRestockEnabled,
            logisticsRestockTargetRuns, logisticsRestockStatus, recipes));
      }
      return new IndustrialFactorySnapshotPacket(entries, buffer.readUtf(MAX_TEXT), buffer.readVarLong());
   }

   private static String clean(String value, String fallback) {
      String cleaned = value == null ? "" : value.strip();
      return cleaned.isBlank() ? fallback : cleaned;
   }

   public record Entry(
      Identifier definitionId,
      String displayName,
      String alertLevel,
      int alertColor,
      String state,
      float integrity,
      double completion,
      BlockPos controllerPos,
      String dimension,
      int robotCount,
      int taskCount,
      int queueCapacity,
      String activeTask,
      int activeProgress,
      int activeDuration,
      String blockedReason,
      int warningCount,
      String firstWarning,
      boolean logisticsAutoRestockEnabled,
      int logisticsRestockTargetRuns,
      String logisticsRestockStatus,
      List<Identifier> recipeIds) {
      public Entry {
         displayName = clean(displayName, definitionId == null ? "Industrial Facility" : definitionId.getPath());
         alertLevel = clean(alertLevel, IndustrialFactoryAlertLevel.IDLE.name());
         state = clean(state, "UNKNOWN");
         integrity = Math.max(0.0F, Math.min(100.0F, integrity));
         completion = Math.max(0.0D, Math.min(1.0D, completion));
         controllerPos = controllerPos == null ? BlockPos.ZERO : controllerPos.immutable();
         dimension = clean(dimension, "minecraft:overworld");
         robotCount = Math.max(0, robotCount);
         taskCount = Math.max(0, taskCount);
         queueCapacity = Math.max(1, queueCapacity);
         activeTask = clean(activeTask, "Idle");
         activeProgress = Math.max(0, activeProgress);
         activeDuration = Math.max(1, activeDuration);
         blockedReason = clean(blockedReason, "");
         warningCount = Math.max(0, warningCount);
         firstWarning = clean(firstWarning, "");
         logisticsRestockTargetRuns = logisticsRestockTargetRuns <= 1 ? 1 : logisticsRestockTargetRuns <= 3 ? 3 : 5;
         logisticsRestockStatus = clean(logisticsRestockStatus, logisticsAutoRestockEnabled ? "Auto-restock enabled." : "Auto-restock disabled.");
         recipeIds = List.copyOf(recipeIds == null ? List.of() : recipeIds.stream().limit(MAX_RECIPES).toList());
      }

      static Entry from(ServerPlayer player, MultiblockRuntimeSnapshot snapshot) {
         TaskExecutionSnapshot active = snapshot.tasks().stream()
            .filter(task -> task.state() == MultiblockTaskState.ACTIVE)
            .findFirst()
            .orElse(null);
         TaskExecutionSnapshot blocked = snapshot.tasks().stream()
            .filter(task -> task.state() == MultiblockTaskState.BLOCKED)
            .findFirst()
            .orElse(null);
         IndustrialFactoryAlertLevel alert = IndustrialFactoryAlertLevel.from(snapshot.state(), snapshot.tasks(),
            snapshot.warningCount(), snapshot.completion());
         List<Identifier> recipes = AutomationRecipeRegistry.all().stream()
            .filter(recipe -> recipe.allowsMultiblock(snapshot.definitionId()))
            .sorted(Comparator
               .comparing((MultiblockAutomationRecipe recipe) -> !recipe.allowedMultiblocks().contains(snapshot.definitionId()))
               .thenComparing(recipe -> recipe.id().toString()))
            .map(MultiblockAutomationRecipe::id)
            .limit(MAX_RECIPES)
            .toList();
         IndustrialMultiblockControllerBlockEntity controller = player.level().getBlockEntity(snapshot.controllerPos()) instanceof IndustrialMultiblockControllerBlockEntity industrial
            ? industrial
            : null;
         return new Entry(
            snapshot.definitionId(),
            snapshot.displayName(),
            alert.name(),
            alert.color(),
            snapshot.state().name(),
            snapshot.integrity(),
            snapshot.completion(),
            snapshot.controllerPos(),
            snapshot.dimension().identifier().toString(),
            snapshot.roboticComponentCount(),
            snapshot.taskCount(),
            8,
            active == null ? "Idle" : active.displayName(),
            active == null ? 0 : active.progressTicks(),
            active == null ? 1 : active.durationTicks(),
            blocked == null ? "" : blocked.blockedReason(),
            snapshot.warningCount(),
            snapshot.warnings().isEmpty() ? "" : snapshot.warnings().get(0),
            controller != null && controller.logisticsAutoRestockEnabled(),
            controller == null ? 3 : controller.logisticsRestockTargetRuns(),
            controller == null ? "Auto-restock status unavailable." : controller.logisticsRestockStatusLine(),
            recipes);
      }
   }
}
