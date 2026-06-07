package com.knoxhack.echonexusprotocol.integration;

import com.knoxhack.echonexusprotocol.data.NexusPlayerData;
import com.knoxhack.echonexusprotocol.world.NexusWorldData;
import java.util.ArrayList;
import java.util.List;

public final class NexusFieldMapPlanner {
   public static final int STORM_RISK_PENALTY = 30;
   public static final int TEAR_RISK_PENALTY = 20;
   public static final int CRITICAL_RECOVERY_RISK = 100;

   private NexusFieldMapPlanner() {
   }

   public static Analysis analyze(NexusPlayerData data) {
      NexusPlayerData source = data == null ? new NexusPlayerData() : data;
      List<CellRisk> cells = new ArrayList<>(NexusPlayerData.FIELD_MAP_SIZE);
      CellRisk center = null;
      CellRisk safestAdjacent = null;
      CellRisk highestRisk = null;
      int collapsedCells = 0;
      int stormCells = 0;
      int tearCells = 0;
      int index = 0;

      for (int row = 0; row < NexusPlayerData.FIELD_MAP_DIAMETER; row++) {
         for (int col = 0; col < NexusPlayerData.FIELD_MAP_DIAMETER; col++) {
            int field = source.telemetryMapField(index);
            int corruption = source.telemetryMapCorruption(index);
            boolean storm = source.telemetryMapStorm(index);
            int tears = source.telemetryMapTears(index);
            NexusWorldData.FieldState state = NexusWorldData.FieldState.fromValue(field);
            int risk = Math.max(0, 100 - field) + corruption + (storm ? STORM_RISK_PENALTY : 0) + tears * TEAR_RISK_PENALTY;
            CellRisk cell = new CellRisk(
               index, row, col, col - NexusPlayerData.FIELD_MAP_RADIUS, row - NexusPlayerData.FIELD_MAP_RADIUS,
               field, corruption, storm, tears, state, risk
            );
            cells.add(cell);
            if (cell.isCenter()) {
               center = cell;
            }
            if (cell.isCardinalAdjacent() && (safestAdjacent == null || isSafer(cell, safestAdjacent))) {
               safestAdjacent = cell;
            }
            if (highestRisk == null || isHigherRisk(cell, highestRisk)) {
               highestRisk = cell;
            }
            if (state == NexusWorldData.FieldState.COLLAPSED) {
               collapsedCells++;
            }
            if (storm) {
               stormCells++;
            }
            if (tears > 0) {
               tearCells++;
            }
            index++;
         }
      }

      return new Analysis(List.copyOf(cells), center, safestAdjacent, highestRisk, collapsedCells, stormCells, tearCells);
   }

   private static boolean isSafer(CellRisk candidate, CellRisk current) {
      if (candidate.risk() != current.risk()) {
         return candidate.risk() < current.risk();
      }
      if (candidate.field() != current.field()) {
         return candidate.field() > current.field();
      }
      if (candidate.corruption() != current.corruption()) {
         return candidate.corruption() < current.corruption();
      }
      if (candidate.tears() != current.tears()) {
         return candidate.tears() < current.tears();
      }
      if (candidate.storm() != current.storm()) {
         return !candidate.storm();
      }
      int candidatePriority = directionPriority(candidate);
      int currentPriority = directionPriority(current);
      return candidatePriority != currentPriority ? candidatePriority < currentPriority : candidate.index() < current.index();
   }

   private static boolean isHigherRisk(CellRisk candidate, CellRisk current) {
      if (candidate.risk() != current.risk()) {
         return candidate.risk() > current.risk();
      }
      if (candidate.field() != current.field()) {
         return candidate.field() < current.field();
      }
      if (candidate.corruption() != current.corruption()) {
         return candidate.corruption() > current.corruption();
      }
      if (candidate.storm() != current.storm()) {
         return candidate.storm();
      }
      if (candidate.tears() != current.tears()) {
         return candidate.tears() > current.tears();
      }
      return candidate.index() < current.index();
   }

   private static int directionPriority(CellRisk cell) {
      if (cell.dx() == 0 && cell.dz() < 0) {
         return 0;
      }
      if (cell.dx() > 0 && cell.dz() == 0) {
         return 1;
      }
      if (cell.dx() == 0 && cell.dz() > 0) {
         return 2;
      }
      if (cell.dx() < 0 && cell.dz() == 0) {
         return 3;
      }
      return 4;
   }

   public static String recoveryToolFor(CellRisk cell) {
      if (cell == null) {
         return "Refresh Nexus Field telemetry before choosing a recovery tool.";
      }
      if (cell.state() == NexusWorldData.FieldState.COLLAPSED || cell.storm() || cell.tears() > 0) {
         return "Use a Field Anchor first, then a Stabilized Purity Charge from a safer edge.";
      }
      if (cell.state() == NexusWorldData.FieldState.CRITICAL || cell.risk() >= CRITICAL_RECOVERY_RISK) {
         return "Use a Stabilized Purity Charge, then run a Field Stabilizer before resuming machines.";
      }
      if (cell.state() == NexusWorldData.FieldState.FRACTURED || cell.corruption() >= 45) {
         return "Run a Corruption Filter and Field Stabilizer before dirty processing.";
      }
      if (cell.corruption() > 0) {
         return "Run a Corruption Filter before pressure becomes a storm window.";
      }
      return "Keep a Field Stabilizer nearby and continue the current Nexus route.";
   }

   public record Analysis(
      List<CellRisk> cells,
      CellRisk center,
      CellRisk safestAdjacent,
      CellRisk highestRisk,
      int collapsedCells,
      int stormCells,
      int tearCells
   ) {
      public CellRisk cell(int index) {
         return index >= 0 && index < this.cells.size() ? this.cells.get(index) : this.center;
      }

      public boolean hasHazards() {
         return this.collapsedCells > 0 || this.stormCells > 0 || this.tearCells > 0;
      }

      public String safestAdjacentGuidance() {
         if (this.safestAdjacent == null) {
            return "No adjacent field telemetry is available.";
         }
         return "Move "
            + this.safestAdjacent.directionLabel()
            + " for the safest work chunk: field "
            + this.safestAdjacent.field()
            + "%, corruption "
            + this.safestAdjacent.corruption()
            + "%, risk "
            + this.safestAdjacent.risk()
            + ". "
            + recoveryToolFor(this.safestAdjacent);
      }

      public String priorityRecoveryGuidance() {
         if (this.highestRisk == null) {
            return "No recovery target is available.";
         }
         return "Stabilize "
            + offsetLabel(this.highestRisk)
            + " first: "
            + this.highestRisk.state().name().toLowerCase(java.util.Locale.ROOT)
            + ", field "
            + this.highestRisk.field()
            + "%, risk "
            + this.highestRisk.risk()
            + ". "
            + recoveryToolFor(this.highestRisk);
      }

      public String hazardSummary() {
         return this.collapsedCells + " collapsed, " + this.stormCells + " storming, " + this.tearCells + " tear-marked cells in local map.";
      }
   }

   public record CellRisk(
      int index,
      int row,
      int col,
      int dx,
      int dz,
      int field,
      int corruption,
      boolean storm,
      int tears,
      NexusWorldData.FieldState state,
      int risk
   ) {
      public boolean isCenter() {
         return this.dx == 0 && this.dz == 0;
      }

      public boolean isCardinalAdjacent() {
         return Math.abs(this.dx) + Math.abs(this.dz) == 1;
      }

      public String directionLabel() {
         if (this.dx == 0 && this.dz < 0) {
            return "north";
         }
         if (this.dx > 0 && this.dz == 0) {
            return "east";
         }
         if (this.dx == 0 && this.dz > 0) {
            return "south";
         }
         if (this.dx < 0 && this.dz == 0) {
            return "west";
         }
         return "current";
      }
   }

   private static String offsetLabel(CellRisk cell) {
      if (cell.isCenter()) {
         return "current chunk";
      }
      StringBuilder builder = new StringBuilder();
      if (cell.dx() != 0) {
         builder.append(Math.abs(cell.dx())).append(cell.dx() > 0 ? " east" : " west");
      }
      if (cell.dz() != 0) {
         if (!builder.isEmpty()) {
            builder.append(", ");
         }
         builder.append(Math.abs(cell.dz())).append(cell.dz() > 0 ? " south" : " north");
      }
      return builder.toString();
   }
}
