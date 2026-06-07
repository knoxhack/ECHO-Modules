package com.knoxhack.echoconvoyprotocol.content;

import com.knoxhack.echoconvoyprotocol.Config;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class ConvoyFieldOperationState {
   private String routeId = "";
   private String operationId = "";
   private ConvoyFieldOperationPhase phase = ConvoyFieldOperationPhase.NONE;
   private long startedTick;
   private long etaTick;
   private int elapsedTicks;
   private int durationTicks;
   private int currentStage;
   private int stageCount = 1;
   private String joinedVehicleUuid = "";
   private String incidentId = "";
   private String failureReason = "";
   private boolean recoveryMarker;
   private boolean completionRewardReady;
   private boolean salvageExported;
   private String resolvedIncidentIds = "";
   private String lastDiagnostic = "No field operation staged.";

   public boolean stage(ConvoyRouteDefinition route, long gameTime) {
      if (route == null) {
         lastDiagnostic = "Field operation staging blocked: route definition missing.";
         return false;
      }
      ConvoyRouteDefinition.FieldOpsSpec ops = route.fieldOps();
      routeId = route.id().toString();
      operationId = route.id().getPath() + "-" + Math.max(0L, gameTime);
      phase = ConvoyFieldOperationPhase.STAGED;
      startedTick = 0L;
      etaTick = 0L;
      elapsedTicks = 0;
      durationTicks = ops.durationTicks(route);
      currentStage = 0;
      stageCount = ops.stageCount(route);
      joinedVehicleUuid = "";
      incidentId = "";
      failureReason = "";
      recoveryMarker = false;
      completionRewardReady = false;
      salvageExported = false;
      resolvedIncidentIds = "";
      lastDiagnostic = "Field operation staged for " + route.title() + ".";
      return true;
   }

   public boolean launch(ConvoyRouteDefinition route, ConvoyFacilityState facilityState, long gameTime) {
      if (route == null || facilityState == null) {
         lastDiagnostic = "Field operation launch blocked: route or depot state missing.";
         return false;
      }
      if (routeId.isBlank() || !routeId.equals(route.id().toString())) {
         stage(route, gameTime);
      }
      if (!facilityState.readiness().dispatchReady()) {
         phase = ConvoyFieldOperationPhase.STAGED;
         lastDiagnostic = "Field operation launch blocked by readiness gate.";
         facilityState.setLastDiagnostic(lastDiagnostic);
         return false;
      }
      if (route.fieldOps().effectiveVehicleJoinPolicy() == Config.VehicleJoinPolicy.VEHICLE_REQUIRED && joinedVehicleUuid().isBlank()) {
         phase = ConvoyFieldOperationPhase.STAGED;
         lastDiagnostic = "Field operation launch blocked: vehicle join policy requires a paired physical convoy vehicle.";
         facilityState.setLastDiagnostic(lastDiagnostic);
         return false;
      }
      facilityState.prepareRoute(route);
      if (!facilityState.dispatch()) {
         phase = ConvoyFieldOperationPhase.STAGED;
         lastDiagnostic = facilityState.lastDiagnostic();
         return false;
      }
      startedTick = Math.max(0L, gameTime);
      elapsedTicks = 0;
      durationTicks = route.fieldOps().durationTicks(route);
      stageCount = route.fieldOps().stageCount(route);
      etaTick = startedTick + durationTicks;
      currentStage = 0;
      incidentId = "";
      failureReason = "";
      recoveryMarker = false;
      completionRewardReady = false;
      salvageExported = false;
      phase = ConvoyFieldOperationPhase.EN_ROUTE;
      lastDiagnostic = "Field operation launched: " + route.title() + ".";
      facilityState.setLastDiagnostic(lastDiagnostic);
      return true;
   }

   public void tick(ServerLevel level, ConvoyRouteDefinition route, ConvoyIncidentProfile incidentProfile, ConvoyFacilityState facilityState) {
      if (level == null || route == null || facilityState == null || !phase.active()) {
         return;
      }
      long now = level.getGameTime();
      if (phase == ConvoyFieldOperationPhase.INCIDENT_BLOCKED
         || phase == ConvoyFieldOperationPhase.FAILED
         || phase == ConvoyFieldOperationPhase.RECOVERY_PENDING
         || phase == ConvoyFieldOperationPhase.STAGED
         || phase == ConvoyFieldOperationPhase.AWAITING_SIGNAL) {
         return;
      }
      if (phase == ConvoyFieldOperationPhase.RETURNING) {
         elapsedTicks += 20;
         etaTick = now + Math.max(0, durationTicks + 60 - elapsedTicks);
         if (elapsedTicks >= durationTicks + 60) {
            phase = ConvoyFieldOperationPhase.COMPLETE;
            completionRewardReady = true;
            recoveryMarker = false;
            facilityState.completeActiveRoute();
            lastDiagnostic = "Field operation complete: " + route.title() + ". Salvage manifest ready.";
            facilityState.setLastDiagnostic(lastDiagnostic);
         }
         return;
      }

      elapsedTicks = Math.min(durationTicks, elapsedTicks + 20);
      int previousStage = currentStage;
      currentStage = Math.min(Math.max(0, stageCount - 1), elapsedTicks * Math.max(1, stageCount) / Math.max(1, durationTicks));
      if (previousStage != currentStage) {
         lastDiagnostic = "Field operation advanced to stage " + (currentStage + 1) + "/" + stageCount + ".";
         facilityState.setLastDiagnostic(lastDiagnostic);
      }
      maybeTriggerIncident(route, incidentProfile, facilityState);
      if (phase == ConvoyFieldOperationPhase.INCIDENT_BLOCKED || phase == ConvoyFieldOperationPhase.FAILED) {
         return;
      }
      etaTick = now + Math.max(0, durationTicks - elapsedTicks);
      if (elapsedTicks >= durationTicks) {
         phase = ConvoyFieldOperationPhase.RETURNING;
         etaTick = now + 60;
         lastDiagnostic = "Field operation returning to depot: " + route.title() + ".";
         facilityState.setLastDiagnostic(lastDiagnostic);
      }
   }

   private void maybeTriggerIncident(ConvoyRouteDefinition route, ConvoyIncidentProfile profile, ConvoyFacilityState facilityState) {
      if (profile == null || phase != ConvoyFieldOperationPhase.EN_ROUTE) {
         return;
      }
      int score = facilityState.readiness().operationScore();
      if (route.fieldOps().effectiveVehicleJoinPolicy() != Config.VehicleJoinPolicy.DEPOT_ONLY && !joinedVehicleUuid().isBlank()) {
         score = Math.min(100, score + 15);
      }
      profile.firstBlockedIncident(currentStage, score, resolvedIncidentIds()).ifPresent(incident -> {
         incidentId = incident.id().toString();
         phase = ConvoyFieldOperationPhase.INCIDENT_BLOCKED;
         durationTicks += incident.delayTicks();
         etaTick += incident.delayTicks();
         facilityState.applyFieldOperationEffect(incident.fuelEffect(), incident.integrityEffect(), incident.cargoEffect());
         if (facilityState.readiness().operationScore() <= 10) {
            phase = ConvoyFieldOperationPhase.FAILED;
            recoveryMarker = true;
            failureReason = incident.displayText() + " Depot readiness collapsed.";
         } else {
            failureReason = incident.displayText();
         }
         lastDiagnostic = "Field incident on " + route.title() + ": " + failureReason
            + " Required response: " + incident.requiredResponseTask() + ".";
         facilityState.setLastDiagnostic(lastDiagnostic);
      });
   }

   public boolean resolveIncident(ConvoyFacilityState facilityState) {
      if (phase != ConvoyFieldOperationPhase.INCIDENT_BLOCKED || incidentId.isBlank()) {
         lastDiagnostic = "No blocked field incident to resolve.";
         if (facilityState != null) {
            facilityState.setLastDiagnostic(lastDiagnostic);
         }
         return false;
      }
      addResolvedIncident(incidentId);
      incidentId = "";
      failureReason = "";
      phase = ConvoyFieldOperationPhase.EN_ROUTE;
      lastDiagnostic = "Field incident resolved; convoy operation has resumed.";
      if (facilityState != null) {
         facilityState.setLastDiagnostic(lastDiagnostic);
      }
      return true;
   }

   public boolean recall(ConvoyFacilityState facilityState) {
      if (!phase.active() || phase == ConvoyFieldOperationPhase.STAGED) {
         lastDiagnostic = "No active field operation to recall.";
         if (facilityState != null) {
            facilityState.setLastDiagnostic(lastDiagnostic);
         }
         return false;
      }
      phase = ConvoyFieldOperationPhase.RECOVERY_PENDING;
      recoveryMarker = true;
      failureReason = "Operation recalled by depot command.";
      lastDiagnostic = "Convoy recall started; recovery task required.";
      if (facilityState != null) {
         facilityState.setLastDiagnostic(lastDiagnostic);
      }
      return true;
   }

   public boolean recover(ConvoyFacilityState facilityState) {
      if (phase != ConvoyFieldOperationPhase.RECOVERY_PENDING && phase != ConvoyFieldOperationPhase.FAILED) {
         lastDiagnostic = "No failed or recalled field operation requires recovery.";
         if (facilityState != null) {
            facilityState.setLastDiagnostic(lastDiagnostic);
         }
         return false;
      }
      phase = ConvoyFieldOperationPhase.RECOVERED;
      recoveryMarker = false;
      incidentId = "";
      failureReason = "";
      if (facilityState != null) {
         facilityState.recoverConvoy();
      }
      lastDiagnostic = "Field operation recovered and ready for restaging.";
      return true;
   }

   public void markSalvageExported(boolean exported) {
      salvageExported = exported;
      if (completionRewardReady) {
         completionRewardReady = !exported;
      }
   }

   public boolean joinVehicle(UUID vehicleId) {
      if (vehicleId == null || !phase.active() || routeId.isBlank()) {
         return false;
      }
      joinedVehicleUuid = vehicleId.toString();
      lastDiagnostic = "Physical convoy vehicle joined field operation.";
      return true;
   }

   public boolean advanceFromSignal(Identifier route, int stage, BlockPos markerPos, UUID vehicleId) {
      if (route == null || routeId.isBlank() || !routeId.equals(route.toString()) || !phase.active()) {
         return false;
      }
      joinVehicle(vehicleId);
      currentStage = Math.max(currentStage, Math.min(Math.max(0, stage), Math.max(0, stageCount - 1)));
      lastDiagnostic = "Roadside signal advanced field operation to stage " + (currentStage + 1) + "/" + stageCount
         + (markerPos == null ? "." : " at " + markerPos.toShortString() + ".");
      return true;
   }

   public boolean canUseRoute(Identifier route) {
      return route != null && !routeId.isBlank() && routeId.equals(route.toString());
   }

   public Set<Identifier> resolvedIncidentIds() {
      if (resolvedIncidentIds == null || resolvedIncidentIds.isBlank()) {
         return Set.of();
      }
      Set<Identifier> values = new LinkedHashSet<>();
      for (String raw : resolvedIncidentIds.split("\\|")) {
         Identifier id = Identifier.tryParse(raw);
         if (id != null) {
            values.add(id);
         }
      }
      return values;
   }

   private void addResolvedIncident(String raw) {
      Identifier id = Identifier.tryParse(raw == null ? "" : raw);
      if (id == null) {
         return;
      }
      Set<Identifier> values = new LinkedHashSet<>(resolvedIncidentIds());
      values.add(id);
      resolvedIncidentIds = values.stream().map(Identifier::toString).reduce((left, right) -> left + "|" + right).orElse("");
   }

   public String summaryLine() {
      if (routeId.isBlank() || phase == ConvoyFieldOperationPhase.NONE) {
         return "FIELD OPS // Idle";
      }
      return "FIELD OPS // " + phase.displayName()
         + " route=" + routeId
         + " stage=" + (currentStage + 1) + "/" + stageCount
         + " eta=" + etaSeconds() + "s"
         + (incidentId.isBlank() ? "" : " incident=" + incidentId)
         + (joinedVehicleUuid.isBlank() ? "" : " vehicle=" + joinedVehicleUuid.substring(0, Math.min(8, joinedVehicleUuid.length())));
   }

   public int etaSeconds() {
      if (phase == ConvoyFieldOperationPhase.STAGED || phase == ConvoyFieldOperationPhase.NONE) {
         return 0;
      }
      return Math.max(0, (durationTicks - elapsedTicks + (phase == ConvoyFieldOperationPhase.RETURNING ? 60 : 0)) / 20);
   }

   public String routeId() {
      return routeId == null ? "" : routeId;
   }

   public Identifier routeIdentifier() {
      return Identifier.tryParse(routeId());
   }

   public String operationId() {
      return operationId == null ? "" : operationId;
   }

   public ConvoyFieldOperationPhase phase() {
      return phase == null ? ConvoyFieldOperationPhase.NONE : phase;
   }

   public int currentStage() {
      return currentStage;
   }

   public int stageCount() {
      return stageCount;
   }

   public String joinedVehicleUuid() {
      return joinedVehicleUuid == null ? "" : joinedVehicleUuid;
   }

   public String incidentId() {
      return incidentId == null ? "" : incidentId;
   }

   public String failureReason() {
      return failureReason == null ? "" : failureReason;
   }

   public boolean recoveryMarker() {
      return recoveryMarker;
   }

   public boolean completionRewardReady() {
      return completionRewardReady;
   }

   public boolean salvageExported() {
      return salvageExported;
   }

   public String lastDiagnostic() {
      return lastDiagnostic == null ? "" : lastDiagnostic;
   }

   public void load(ValueInput input) {
      routeId = input.getStringOr("convoy_ops_route_id", routeId);
      operationId = input.getStringOr("convoy_ops_operation_id", operationId);
      phase = phase(input.getStringOr("convoy_ops_phase", phase.name()));
      startedTick = input.getLongOr("convoy_ops_started_tick", startedTick);
      etaTick = input.getLongOr("convoy_ops_eta_tick", etaTick);
      elapsedTicks = input.getIntOr("convoy_ops_elapsed_ticks", elapsedTicks);
      durationTicks = input.getIntOr("convoy_ops_duration_ticks", durationTicks);
      currentStage = input.getIntOr("convoy_ops_current_stage", currentStage);
      stageCount = input.getIntOr("convoy_ops_stage_count", stageCount);
      joinedVehicleUuid = input.getStringOr("convoy_ops_vehicle_uuid", joinedVehicleUuid);
      incidentId = input.getStringOr("convoy_ops_incident_id", incidentId);
      failureReason = input.getStringOr("convoy_ops_failure_reason", failureReason);
      recoveryMarker = input.getBooleanOr("convoy_ops_recovery_marker", recoveryMarker);
      completionRewardReady = input.getBooleanOr("convoy_ops_completion_reward_ready", completionRewardReady);
      salvageExported = input.getBooleanOr("convoy_ops_salvage_exported", salvageExported);
      resolvedIncidentIds = input.getStringOr("convoy_ops_resolved_incidents", resolvedIncidentIds);
      lastDiagnostic = input.getStringOr("convoy_ops_last_diagnostic", lastDiagnostic);
   }

   public void save(ValueOutput output) {
      output.putString("convoy_ops_route_id", routeId());
      output.putString("convoy_ops_operation_id", operationId());
      output.putString("convoy_ops_phase", phase().name());
      output.putLong("convoy_ops_started_tick", startedTick);
      output.putLong("convoy_ops_eta_tick", etaTick);
      output.putInt("convoy_ops_elapsed_ticks", elapsedTicks);
      output.putInt("convoy_ops_duration_ticks", durationTicks);
      output.putInt("convoy_ops_current_stage", currentStage);
      output.putInt("convoy_ops_stage_count", stageCount);
      output.putString("convoy_ops_vehicle_uuid", joinedVehicleUuid());
      output.putString("convoy_ops_incident_id", incidentId());
      output.putString("convoy_ops_failure_reason", failureReason());
      output.putBoolean("convoy_ops_recovery_marker", recoveryMarker);
      output.putBoolean("convoy_ops_completion_reward_ready", completionRewardReady);
      output.putBoolean("convoy_ops_salvage_exported", salvageExported);
      output.putString("convoy_ops_resolved_incidents", resolvedIncidentIds == null ? "" : resolvedIncidentIds);
      output.putString("convoy_ops_last_diagnostic", lastDiagnostic());
   }

   private static ConvoyFieldOperationPhase phase(String raw) {
      if (raw == null || raw.isBlank()) {
         return ConvoyFieldOperationPhase.NONE;
      }
      try {
         return ConvoyFieldOperationPhase.valueOf(raw.strip().toUpperCase(java.util.Locale.ROOT));
      } catch (IllegalArgumentException exception) {
         return ConvoyFieldOperationPhase.NONE;
      }
   }
}
