package com.knoxhack.echoindustrialnexus.api;

import com.knoxhack.echomultiblockcore.api.MultiblockRuntimeSnapshot;
import com.knoxhack.echomultiblockcore.api.TaskExecutionSnapshot;
import com.knoxhack.echoindustrialnexus.block.entity.IndustrialMultiblockControllerBlockEntity;
import com.knoxhack.echoindustrialnexus.factory.IndustrialFactoryAlertLevel;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/**
 * Read-only Industrial factory state for optional sibling modules.
 */
public interface IndustrialFactorySnapshotView {
   Identifier definitionId();

   String displayName();

   String state();

   String alertLevel();

   int alertColor();

   BlockPos controllerPos();

   String dimension();

   float integrity();

   double completion();

   int robotCount();

   int taskCount();

   int warningCount();

   List<String> warnings();

   List<String> activeTasks();

   boolean logisticsAutoRestockEnabled();

   int logisticsRestockTargetRuns();

   String logisticsRestockStatus();

   static IndustrialFactorySnapshotView from(
      MultiblockRuntimeSnapshot snapshot,
      IndustrialMultiblockControllerBlockEntity controller
   ) {
      return Snapshot.from(snapshot, controller);
   }

   record Snapshot(
      Identifier definitionId,
      String displayName,
      String state,
      String alertLevel,
      int alertColor,
      BlockPos controllerPos,
      String dimension,
      float integrity,
      double completion,
      int robotCount,
      int taskCount,
      int warningCount,
      List<String> warnings,
      List<String> activeTasks,
      boolean logisticsAutoRestockEnabled,
      int logisticsRestockTargetRuns,
      String logisticsRestockStatus
   ) implements IndustrialFactorySnapshotView {
      public Snapshot {
         displayName = clean(displayName, definitionId == null ? "Industrial Facility" : definitionId.getPath().replace('_', ' '));
         state = clean(state, "UNKNOWN");
         alertLevel = clean(alertLevel, "IDLE");
         controllerPos = controllerPos == null ? BlockPos.ZERO : controllerPos.immutable();
         dimension = clean(dimension, "minecraft:overworld");
         integrity = Math.max(0.0F, Math.min(100.0F, integrity));
         completion = Math.max(0.0D, Math.min(1.0D, completion));
         robotCount = Math.max(0, robotCount);
         taskCount = Math.max(0, taskCount);
         warningCount = Math.max(0, warningCount);
         warnings = List.copyOf(warnings == null ? List.of() : warnings);
         activeTasks = List.copyOf(activeTasks == null ? List.of() : activeTasks);
         logisticsRestockTargetRuns = logisticsRestockTargetRuns <= 1 ? 1 : logisticsRestockTargetRuns <= 3 ? 3 : 5;
         logisticsRestockStatus = clean(logisticsRestockStatus,
            logisticsAutoRestockEnabled ? "Auto-restock enabled." : "Auto-restock disabled.");
      }

      private static Snapshot from(MultiblockRuntimeSnapshot snapshot, IndustrialMultiblockControllerBlockEntity controller) {
         if (snapshot == null) {
            return new Snapshot(null, "Industrial Facility", "UNKNOWN", "IDLE", 0xFFFF9F3D,
               BlockPos.ZERO, "minecraft:overworld", 0.0F, 0.0D, 0, 0, 0, List.of(), List.of(), false, 3,
               "Auto-restock status unavailable.");
         }
         IndustrialFactoryAlertLevel alert = IndustrialFactoryAlertLevel.from(snapshot.state(), snapshot.tasks(),
            snapshot.warningCount(), snapshot.completion());
         return new Snapshot(
            snapshot.definitionId(),
            snapshot.displayName(),
            snapshot.state().name(),
            alert.name(),
            alert.color(),
            snapshot.controllerPos(),
            snapshot.dimension().identifier().toString(),
            snapshot.integrity(),
            snapshot.completion(),
            snapshot.roboticComponentCount(),
            snapshot.taskCount(),
            snapshot.warningCount(),
            snapshot.warnings(),
            snapshot.tasks().stream().map(TaskExecutionSnapshot::displayName).toList(),
            controller != null && controller.logisticsAutoRestockEnabled(),
            controller == null ? 3 : controller.logisticsRestockTargetRuns(),
            controller == null ? "Auto-restock status unavailable." : controller.logisticsRestockStatusLine()
         );
      }

      private static String clean(String value, String fallback) {
         String cleaned = value == null ? "" : value.strip();
         return cleaned.isBlank() ? fallback : cleaned;
      }
   }
}
