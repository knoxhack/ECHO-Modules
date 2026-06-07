package com.knoxhack.echoindustrialnexus.event;

import com.knoxhack.echomultiblockcore.event.RoboticTaskCompletedEvent;
import com.knoxhack.echoindustrialnexus.EchoIndustrialNexus;
import com.knoxhack.echoindustrialnexus.progress.IndustrialProgress;

public final class IndustrialMultiblockMissionEvents {
   public void onRoboticTaskCompleted(Object event) {
      if (!(event instanceof RoboticTaskCompletedEvent taskEvent)) {
         return;
      }
      if (taskEvent.taskId == null || !EchoIndustrialNexus.MODID.equals(taskEvent.taskId.getNamespace())) {
         return;
      }
      IndustrialProgress.recordAutomationTaskCompleted(taskEvent.level, taskEvent.controllerPos, taskEvent.taskId);
   }
}
