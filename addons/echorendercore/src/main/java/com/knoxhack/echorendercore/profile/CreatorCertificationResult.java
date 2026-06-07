package com.knoxhack.echorendercore.profile;

import java.util.Locale;

public enum CreatorCertificationResult {
   PASS("pass"),
   WARN("warn"),
   FAIL("fail");

   private final String id;

   CreatorCertificationResult(String id) {
      this.id = id;
   }

   public String id() {
      return id;
   }

   public static CreatorCertificationResult byId(String id, CreatorCertificationResult fallback) {
      if (id != null) {
         String normalized = id.trim().toLowerCase(Locale.ROOT);
         for (CreatorCertificationResult result : values()) {
            if (result.id.equals(normalized) || result.name().toLowerCase(Locale.ROOT).equals(normalized)) {
               return result;
            }
         }
      }
      return fallback == null ? WARN : fallback;
   }
}
