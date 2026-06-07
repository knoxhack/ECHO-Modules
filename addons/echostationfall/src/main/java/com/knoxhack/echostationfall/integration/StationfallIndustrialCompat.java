package com.knoxhack.echostationfall.integration;

import com.knoxhack.echostationfall.progression.SignalPanicState;
import com.knoxhack.echostationfall.progression.StationPowerState;
import com.knoxhack.echostationfall.progression.StationSection;
import com.knoxhack.echostationfall.progression.StationfallProgress;
import com.knoxhack.echostationfall.world.StationfallDimensions;
import com.knoxhack.echostationfall.world.StationfallStationState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

public final class StationfallIndustrialCompat {
   private StationfallIndustrialCompat() {
   }

   public static void stabilizeStationScrubber(ServerLevel level, BlockPos pos, int radius, int intensity) {
      if (level == null || pos == null || !StationfallDimensions.isStation(level)) {
         return;
      }
      StationSection section = StationSection.fromPosition(pos);
      StationfallStationState state = StationfallStationState.get(level);
      state.setBreachRepaired(section, true);
      if (intensity >= 2 && !state.powerState(section).stableOrBetter()) {
         state.setPower(section, StationPowerState.STABLE);
      }
   }

   public static void recordIndustrialComponent(Player player, String componentId) {
      if (player == null || componentId == null || componentId.isBlank()) {
         return;
      }
      StationfallProgress progress = StationfallProgress.get(player);
      switch (componentId) {
         case "station_battery" -> progress.setSectionPower(player, StationSection.ENGINEERING_DECK, StationPowerState.STABLE);
         case "pressure_seal_kit", "hull_repair_foam" -> supportSuit(player, 0, 45, 0);
         case "emergency_oxygen_filter" -> supportSuit(player, 55, 0, 0);
         case "signal_panic_dampener" -> {
            SignalPanicState.get(player).decay(player, 35);
            StationfallMissionHooks.recordPanicDampened(player, componentId);
         }
         case "ai_override_chip_casing" -> progress.markAiOverrideObtained(player);
         default -> {
         }
      }
   }

   public static void supportSuit(Player player, int oxygenBoost, int pressureBoost, int panicReduction) {
      if (player == null) {
         return;
      }
      StationfallSuitState suit = StationfallSuitState.get(player);
      if (oxygenBoost > 0) {
         suit.boostOxygen(oxygenBoost);
         StationfallMissionHooks.recordOxygenStabilized(player, "industrial_support");
      }
      if (pressureBoost > 0) {
         suit.applySealantPatch();
         StationfallMissionHooks.recordPressureStabilized(player, "industrial_support");
      }
      suit.save(player);
      if (panicReduction > 0) {
         SignalPanicState.get(player).decay(player, panicReduction);
      }
   }
}
