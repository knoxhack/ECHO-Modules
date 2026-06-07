package com.knoxhack.echorendercore.profile;

import com.google.gson.JsonObject;

public record ProfileScreenshotCaptureResult(
   String providerId,
   boolean captured,
   String relativePath,
   String skippedReason
) {
   public ProfileScreenshotCaptureResult {
      providerId = providerId == null || providerId.isBlank() ? "none" : providerId;
      relativePath = normalize(relativePath);
      skippedReason = skippedReason == null ? "" : skippedReason;
   }

   public static ProfileScreenshotCaptureResult captured(String providerId, String relativePath) {
      return new ProfileScreenshotCaptureResult(providerId, true, relativePath, "");
   }

   public static ProfileScreenshotCaptureResult skipped(String providerId, String reason) {
      return new ProfileScreenshotCaptureResult(providerId, false, "", reason);
   }

   public JsonObject toJson() {
      JsonObject root = new JsonObject();
      root.addProperty("provider", providerId);
      root.addProperty("captured", captured);
      root.addProperty("path", relativePath);
      root.addProperty("skipped_reason", skippedReason);
      return root;
   }

   private static String normalize(String path) {
      if (path == null || path.isBlank()) {
         return "";
      }
      return path.replace('\\', '/').replaceFirst("^/+", "");
   }
}
