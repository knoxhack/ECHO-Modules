package com.knoxhack.echoconvoyprotocol.content;

public enum ConvoyFieldOperationPhase {
   NONE,
   STAGED,
   EN_ROUTE,
   AWAITING_SIGNAL,
   INCIDENT_BLOCKED,
   RETURNING,
   COMPLETE,
   FAILED,
   RECOVERY_PENDING,
   RECOVERED;

   public boolean active() {
      return this == STAGED
         || this == EN_ROUTE
         || this == AWAITING_SIGNAL
         || this == INCIDENT_BLOCKED
         || this == RETURNING
         || this == FAILED
         || this == RECOVERY_PENDING;
   }

   public String displayName() {
      return switch (this) {
         case NONE -> "Idle";
         case STAGED -> "Staged";
         case EN_ROUTE -> "En Route";
         case AWAITING_SIGNAL -> "Awaiting Signal";
         case INCIDENT_BLOCKED -> "Incident Blocked";
         case RETURNING -> "Returning";
         case COMPLETE -> "Complete";
         case FAILED -> "Failed";
         case RECOVERY_PENDING -> "Recovery Pending";
         case RECOVERED -> "Recovered";
      };
   }
}
