package com.knoxhack.echorendercore.profile;

import java.util.Comparator;
import java.util.List;
import net.minecraft.resources.Identifier;

/**
 * Common/server-safe helpers for addons to declare V16 showcase coverage.
 */
public final class CreatorAddonShowcaseRegistry {
   private CreatorAddonShowcaseRegistry() {
   }

   public static CreatorAddonIntegration integrated(
      String namespace,
      String displayName,
      String surfaceType,
      String surface,
      Identifier showcaseProfile,
      String fallbackExpectation,
      String qaNotes,
      String notes
   ) {
      return integrated(namespace, displayName, surfaceType, surface, showcaseProfile, fallbackExpectation, qaNotes,
         "rendercore_native", 0, CreatorSurfaceIntegration.EMPTY, notes);
   }

   public static CreatorAddonIntegration integrated(
      String namespace,
      String displayName,
      String surfaceType,
      String surface,
      Identifier showcaseProfile,
      String fallbackExpectation,
      String qaNotes,
      String renderIntegrationStatus,
      int convertedEntityCount,
      String notes
   ) {
      return integrated(namespace, displayName, surfaceType, surface, showcaseProfile, fallbackExpectation, qaNotes,
         renderIntegrationStatus, convertedEntityCount, CreatorSurfaceIntegration.EMPTY, notes);
   }

   public static CreatorAddonIntegration integrated(
      String namespace,
      String displayName,
      String surfaceType,
      String surface,
      Identifier showcaseProfile,
      String fallbackExpectation,
      String qaNotes,
      String renderIntegrationStatus,
      int convertedEntityCount,
      CreatorSurfaceIntegration surfaceIntegration,
      String notes
   ) {
      return new CreatorAddonIntegration(
         namespace,
         displayName,
         "integrated",
         surface,
         showcaseProfile,
         surfaceType,
         fallbackExpectation,
         qaNotes,
         renderIntegrationStatus,
         convertedEntityCount,
         surfaceIntegration,
         notes
      );
   }

   public static CreatorAddonIntegration noVisualSurface(String namespace, String displayName, String reason) {
      return new CreatorAddonIntegration(
         namespace,
         displayName,
         "declared_no_visual_surface",
         "none",
         null,
         "service_only",
         "not_applicable",
         "no client visual QA required",
         "no_visual_surface",
         0,
         CreatorSurfaceIntegration.EMPTY,
         reason
      );
   }

   public static List<CreatorAddonIntegration> sorted(List<CreatorAddonIntegration> integrations) {
      return integrations.stream()
         .sorted(Comparator.comparing(CreatorAddonIntegration::namespace))
         .toList();
   }
}
