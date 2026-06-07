package com.knoxhack.echorendercore.profile;

import java.nio.file.Path;

public interface ProfileScreenshotPreviewProvider {
   ProfileScreenshotPreviewProvider NO_OP = new ProfileScreenshotPreviewProvider() {
      @Override
      public String id() {
         return "none";
      }

      @Override
      public boolean available(VisualProfile profile) {
         return false;
      }

      @Override
      public String skippedReason(VisualProfile profile) {
         return "No screenshot preview provider is registered.";
      }
   };

   String id();

   boolean available(VisualProfile profile);

   default String relativePath(VisualProfile profile) {
      if (profile == null || profile.id() == null) {
         return "";
      }
      return "assets/" + profile.id().getNamespace() + "/rendercore/creator/screenshots/" + sanitize(profile.id().getPath()) + ".png";
   }

   default ProfileScreenshotCaptureResult capture(VisualProfile profile, Path exportRoot) {
      return ProfileScreenshotCaptureResult.skipped(id(), skippedReason(profile));
   }

   default String skippedReason(VisualProfile profile) {
      return available(profile) ? "" : "Screenshot preview unavailable.";
   }

   private static String sanitize(String path) {
      String value = path == null || path.isBlank() ? "profile" : path;
      return value.replace('\\', '/').replace('/', '_').replace(':', '_');
   }
}
