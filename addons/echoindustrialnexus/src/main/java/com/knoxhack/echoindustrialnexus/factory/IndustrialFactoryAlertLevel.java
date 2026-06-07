package com.knoxhack.echoindustrialnexus.factory;

import com.knoxhack.echomultiblockcore.api.MultiblockState;
import com.knoxhack.echomultiblockcore.api.MultiblockTaskState;
import com.knoxhack.echomultiblockcore.api.TaskExecutionSnapshot;
import java.util.List;

public enum IndustrialFactoryAlertLevel {
   ONLINE(0xFF64D97B),
   IDLE(0xFF66E8FF),
   ACTIVE(0xFFFFA23F),
   BLOCKED(0xFFFF5D4D),
   DAMAGED(0xFFFFC857),
   INCOMPLETE(0xFF7D8790);

   private final int color;

   IndustrialFactoryAlertLevel(int color) {
      this.color = color;
   }

   public int color() {
      return color;
   }

   public static IndustrialFactoryAlertLevel from(MultiblockState state, List<TaskExecutionSnapshot> tasks,
                                                  int warningCount, double completion) {
      MultiblockState safeState = state == null ? MultiblockState.UNBUILT : state;
      if (safeState == MultiblockState.UNBUILT || safeState == MultiblockState.INCOMPLETE
         || safeState == MultiblockState.OFFLINE || completion < 1.0D && safeState == MultiblockState.VALIDATING) {
         return INCOMPLETE;
      }
      if (safeState == MultiblockState.DAMAGED || safeState == MultiblockState.JAMMED
         || safeState == MultiblockState.OVERLOADED || warningCount > 0) {
         return DAMAGED;
      }
      if (tasks != null && tasks.stream().anyMatch(task -> task.state() == MultiblockTaskState.BLOCKED)) {
         return BLOCKED;
      }
      if (safeState == MultiblockState.ACTIVE
         || tasks != null && tasks.stream().anyMatch(task -> task.state() == MultiblockTaskState.ACTIVE)) {
         return ACTIVE;
      }
      if (tasks != null && !tasks.isEmpty()) {
         return ONLINE;
      }
      return IDLE;
   }
}
