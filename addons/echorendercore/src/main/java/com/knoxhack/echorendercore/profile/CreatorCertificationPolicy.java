package com.knoxhack.echorendercore.profile;

import java.util.Locale;

public enum CreatorCertificationPolicy {
   ERRORS_ONLY("errors_only");

   private final String id;

   CreatorCertificationPolicy(String id) {
      this.id = id;
   }

   public String id() {
      return id;
   }

   public static CreatorCertificationPolicy byId(String id, CreatorCertificationPolicy fallback) {
      if (id != null) {
         String normalized = id.trim().toLowerCase(Locale.ROOT);
         for (CreatorCertificationPolicy policy : values()) {
            if (policy.id.equals(normalized) || policy.name().toLowerCase(Locale.ROOT).equals(normalized)) {
               return policy;
            }
         }
      }
      return fallback == null ? ERRORS_ONLY : fallback;
   }
}
