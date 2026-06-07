package com.knoxhack.echoagriculturereclamation.api;

import com.knoxhack.echoagriculturereclamation.EchoAgricultureReclamation;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ReclamationIntegrationServices {
   private static final List<ReclamationFieldObserver> FIELD_OBSERVERS = new CopyOnWriteArrayList<>();

   private ReclamationIntegrationServices() {
   }

   public static boolean registerFieldObserver(ReclamationFieldObserver observer) {
      if (observer == null || FIELD_OBSERVERS.contains(observer)) {
         return false;
      }
      FIELD_OBSERVERS.add(observer);
      return true;
   }

   public static List<ReclamationFieldObserver> fieldObservers() {
      return List.copyOf(FIELD_OBSERVERS);
   }

   public static void publishFieldSnapshot(ReclamationFieldSnapshot snapshot) {
      if (snapshot == null) {
         return;
      }
      for (ReclamationFieldObserver observer : FIELD_OBSERVERS) {
         try {
            observer.onFieldSnapshot(snapshot);
         } catch (RuntimeException exception) {
            EchoAgricultureReclamation.LOGGER.warn("Agriculture field observer failed: {}", observer.getClass().getName(), exception);
         }
      }
   }

   public static void clearObserversForTests() {
      FIELD_OBSERVERS.clear();
   }
}
