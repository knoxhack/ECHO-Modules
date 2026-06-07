package com.knoxhack.echorendercore.client;

import com.knoxhack.echorendercore.EchoRenderCore;
import com.knoxhack.echorendercore.profile.ProfileScreenshotCaptureResult;
import com.knoxhack.echorendercore.profile.ProfileScreenshotPreviewProvider;
import com.knoxhack.echorendercore.profile.VisualProfile;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

public final class RenderCoreClientScreenshotPreviewProvider implements ProfileScreenshotPreviewProvider {
   public static final RenderCoreClientScreenshotPreviewProvider INSTANCE = new RenderCoreClientScreenshotPreviewProvider();

   private RenderCoreClientScreenshotPreviewProvider() {
   }

   @Override
   public String id() {
      return "client_viewport_capture";
   }

   @Override
   public boolean available(VisualProfile profile) {
      return profile != null && profile.id() != null && Minecraft.getInstance().getMainRenderTarget() != null;
   }

   @Override
   public ProfileScreenshotCaptureResult capture(VisualProfile profile, Path exportRoot) {
      if (!available(profile)) {
         return ProfileScreenshotCaptureResult.skipped(id(), skippedReason(profile));
      }
      Path root = exportRoot == null ? Minecraft.getInstance().gameDirectory.toPath().resolve("rendercore_creator") : exportRoot;
      Path relative = Path.of(relativePath(profile));
      Path absolute = root.resolve(relative);
      try {
         Files.createDirectories(absolute.getParent());
         Path screenshotRoot = absolute.getParent() != null && absolute.getParent().getParent() != null
            ? absolute.getParent().getParent()
            : root;
         Screenshot.grab(
            screenshotRoot.toFile(),
            absolute.getFileName().toString(),
            Minecraft.getInstance().getMainRenderTarget(),
            1,
            component -> EchoRenderCore.LOGGER.info("RenderCore screenshot capture {}: {}", profile.id(), component.getString())
         );
         return ProfileScreenshotCaptureResult.captured(id(), relative.toString());
      } catch (RuntimeException | java.io.IOException exception) {
         EchoRenderCore.LOGGER.warn("RenderCore screenshot capture failed for {}", profile.id(), exception);
         return ProfileScreenshotCaptureResult.skipped(id(), exception.getMessage());
      }
   }

   @Override
   public String skippedReason(VisualProfile profile) {
      if (profile == null || profile.id() == null) {
         return "No profile is selected for screenshot capture.";
      }
      return "Client render target is unavailable.";
   }
}
