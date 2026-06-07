package com.knoxhack.echoconvoyprotocol.block.entity;

import com.knoxhack.echoconvoyprotocol.content.ConvoyContent;
import com.knoxhack.echoconvoyprotocol.content.ConvoyFieldOperationState;
import com.knoxhack.echoconvoyprotocol.content.ConvoyFacilityState;
import com.knoxhack.echoconvoyprotocol.content.ConvoyReadiness;
import com.knoxhack.echoconvoyprotocol.content.ConvoyRouteDefinition;
import com.knoxhack.echoconvoyprotocol.integration.ConvoyMissionHooks;
import com.knoxhack.echoconvoyprotocol.registry.ModBlockEntities;
import com.knoxhack.echomultiblockcore.api.LensMultiblockScan;
import com.knoxhack.echomultiblockcore.api.MultiblockRuntimeSnapshot;
import com.knoxhack.echomultiblockcore.api.MultiblockStatusSnapshot;
import com.knoxhack.echomultiblockcore.block.entity.MultiblockControllerBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ConvoyMultiblockControllerBlockEntity extends MultiblockControllerBlockEntity {
   private final ConvoyFacilityState convoyState = new ConvoyFacilityState();
   private final ConvoyFieldOperationState fieldOperation = new ConvoyFieldOperationState();

   public ConvoyMultiblockControllerBlockEntity(BlockPos pos, BlockState blockState) {
      super(ModBlockEntities.CONVOY_MULTIBLOCK_CONTROLLER.get(), pos, blockState);
   }

   public static void tick(Level level, BlockPos pos, BlockState state, ConvoyMultiblockControllerBlockEntity controller) {
      MultiblockControllerBlockEntity.tick(level, pos, state, controller);
   }

   public ConvoyFacilityState convoyState() {
      return convoyState;
   }

   public ConvoyFieldOperationState fieldOperation() {
      return fieldOperation;
   }

   public ConvoyReadiness readiness() {
      return convoyState.readiness();
   }

   @Override
   public void handlePlayerUse(Player player, boolean diagnosticsOnly) {
      super.handlePlayerUse(player, diagnosticsOnly);
      if (!diagnosticsOnly && isFormedForOperations()) {
         ConvoyMissionHooks.recordDepotFormation(player, getMultiblockId());
      }
   }

   @Override
   public MultiblockStatusSnapshot statusSnapshot() {
      MultiblockStatusSnapshot base = super.statusSnapshot();
      List<String> tasks = new ArrayList<>(base.currentTasks());
      tasks.add(fieldOperation.summaryLine());
      List<String> warnings = new ArrayList<>(base.warnings());
      convoyOperationWarnings(warnings);
      return new MultiblockStatusSnapshot(
         base.definitionId(),
         base.name(),
         base.state(),
         base.integrity(),
         base.completion(),
         base.controllerPos(),
         base.installedModules(),
         base.roboticArms(),
         tasks,
         warnings
      );
   }

   @Override
   public MultiblockRuntimeSnapshot runtimeSnapshot() {
      MultiblockRuntimeSnapshot base = super.runtimeSnapshot();
      List<String> warnings = new ArrayList<>(base.warnings());
      convoyOperationWarnings(warnings);
      return new MultiblockRuntimeSnapshot(
         base.definitionId(),
         base.controllerPos(),
         base.state(),
         base.integrity(),
         base.completion(),
         base.matchedBlockCount(),
         base.roboticComponentCount(),
         base.tasks(),
         warnings,
         base.lastValidationTime(),
         base.dimension(),
         base.displayName(),
         base.category(),
         base.role(),
         base.markerColor(),
         base.taskCount(),
         warnings.size(),
         base.capabilityRuntime(),
         base.installedUpgrades(),
         base.damageGroups(),
         base.repairActions(),
         base.robotAnimations(),
         base.constructionProgress()
      );
   }

   @Override
   public LensMultiblockScan scanSnapshot() {
      LensMultiblockScan base = super.scanSnapshot();
      List<String> roboticStatus = new ArrayList<>(base.roboticStatus());
      roboticStatus.add("Operation score: " + readiness().operationScore() + "%");
      List<String> taskQueue = new ArrayList<>(base.taskQueue());
      taskQueue.add(fieldOperation.summaryLine());
      if (!fieldOperation.failureReason().isBlank()) {
         taskQueue.add("Field blocker: " + fieldOperation.failureReason());
      }
      return new LensMultiblockScan(
         base.targetId(),
         base.structureName(),
         base.state(),
         base.completion(),
         base.targetPos(),
         base.missingBlocks(),
         roboticStatus,
         taskQueue
      );
   }

   @Override
   public void tickFormedStructure() {
      super.tickFormedStructure();
      if (level instanceof ServerLevel serverLevel && serverLevel.getGameTime() % 20L == 0L) {
         Identifier routeId = fieldOperation.routeIdentifier();
         ConvoyRouteDefinition route = routeId == null ? null : ConvoyContent.route(routeId).orElse(null);
         fieldOperation.tick(serverLevel, route, route == null ? null : ConvoyContent.incidentProfile(route.fieldOps().incidentProfile()).orElse(null), convoyState);
         if (fieldOperation.phase().active()) {
            setChanged();
         }
      }
   }

   @Override
   public List<String> diagnosticLines() {
      List<String> lines = new ArrayList<>(super.diagnosticLines());
      ConvoyReadiness readiness = readiness();
      lines.add(readiness.summaryLine());
      lines.add("CONVOY SCORE // Field operation readiness " + readiness.operationScore() + "%");
      lines.add(fieldOperation.summaryLine());
      if (!fieldOperation.incidentId().isBlank()) {
         lines.add("FIELD INCIDENT // " + fieldOperation.failureReason());
      }
      lines.add("CONVOY STATUS // Active route: " + (convoyState.activeRouteId().isBlank() ? "none" : convoyState.activeRouteId())
         + ", Completed missions: " + convoyState.completedMissions()
         + ", Recovery: " + (convoyState.damagedConvoy() ? "needed" : "standby"));
      lines.add("CONVOY LOGISTICS // Network " + convoyState.logisticsNetworkId()
         + ", Loadout " + (convoyState.logisticsLoadoutId().isBlank() ? "none" : convoyState.logisticsLoadoutId())
         + ", Online " + (convoyState.logisticsNetworkOnline() ? "yes" : "no")
         + ", Ready " + (convoyState.logisticsLoadoutReady() ? "yes" : "no")
         + ", Deliveries " + convoyState.logisticsActiveDeliveries());
      lines.add("CONVOY DIAGNOSTIC // " + convoyState.lastDiagnostic());
      return List.copyOf(lines);
   }

   public Component readinessComponent() {
      return Component.literal(readiness().summaryLine());
   }

   private void convoyOperationWarnings(List<String> warnings) {
      warnings.add("Readiness score " + readiness().operationScore() + "%");
      if (!fieldOperation.routeId().isBlank()) {
         warnings.add(fieldOperation.summaryLine());
      }
      if (!fieldOperation.failureReason().isBlank()) {
         warnings.add("Field Ops blocker: " + fieldOperation.failureReason());
      }
   }

   @Override
   protected void loadAdditional(ValueInput input) {
      super.loadAdditional(input);
      convoyState.load(input);
      fieldOperation.load(input);
   }

   @Override
   protected void saveAdditional(ValueOutput output) {
      super.saveAdditional(output);
      convoyState.save(output);
      fieldOperation.save(output);
   }
}
