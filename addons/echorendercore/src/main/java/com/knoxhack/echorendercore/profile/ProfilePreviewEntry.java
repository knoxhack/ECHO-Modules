package com.knoxhack.echorendercore.profile;

import net.minecraft.resources.Identifier;

public record ProfilePreviewEntry(
   Identifier profileId,
   int schemaVersion,
   int layerCount,
   int maskedLayerCount,
   int animationClipCount,
   int animationTrackCount,
   int emitterCount,
   int anchorCount,
   int blockPartAliasCount,
   long validationWarningCount,
   long validationErrorCount,
   int performanceWarningCount,
   boolean screenshotAvailable,
   String screenshotProvider,
   String screenshotSkippedReason,
   String suggestedArtifactPath
) {
   public ProfilePreviewEntry(
         net.minecraft.resources.Identifier profileId,
         int schemaVersion,
         int layerCount,
         int maskedLayerCount,
         int animationClipCount,
         int animationTrackCount,
         int emitterCount,
         int anchorCount,
         int blockPartAliasCount,
         long validationWarningCount,
         long validationErrorCount,
         int performanceWarningCount,
         String suggestedArtifactPath) {
      this(profileId, schemaVersion, layerCount, maskedLayerCount, animationClipCount, animationTrackCount,
         emitterCount, anchorCount, blockPartAliasCount, validationWarningCount, validationErrorCount,
         performanceWarningCount, false, "none", "No screenshot preview provider is registered.", suggestedArtifactPath);
   }

   public ProfilePreviewEntry {
      schemaVersion = Math.max(0, schemaVersion);
      layerCount = Math.max(0, layerCount);
      maskedLayerCount = Math.max(0, maskedLayerCount);
      animationClipCount = Math.max(0, animationClipCount);
      animationTrackCount = Math.max(0, animationTrackCount);
      emitterCount = Math.max(0, emitterCount);
      anchorCount = Math.max(0, anchorCount);
      blockPartAliasCount = Math.max(0, blockPartAliasCount);
      validationWarningCount = Math.max(0, validationWarningCount);
      validationErrorCount = Math.max(0, validationErrorCount);
      performanceWarningCount = Math.max(0, performanceWarningCount);
      screenshotProvider = screenshotProvider == null || screenshotProvider.isBlank() ? "none" : screenshotProvider;
      screenshotSkippedReason = screenshotSkippedReason == null ? "" : screenshotSkippedReason;
      suggestedArtifactPath = suggestedArtifactPath == null ? "" : suggestedArtifactPath;
   }
}
