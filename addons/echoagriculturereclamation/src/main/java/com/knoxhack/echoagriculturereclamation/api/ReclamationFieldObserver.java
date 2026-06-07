package com.knoxhack.echoagriculturereclamation.api;

/**
 * Optional observer hook for addons that want read-only Agriculture field state.
 */
@FunctionalInterface
public interface ReclamationFieldObserver {
   void onFieldSnapshot(ReclamationFieldSnapshot snapshot);
}
