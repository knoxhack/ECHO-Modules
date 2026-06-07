package com.knoxhack.echorendercore.profile;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

public record CreatorAddonIntegration(
   String namespace,
   String displayName,
   String status,
   String surface,
   Identifier showcaseProfile,
   String surfaceType,
   String fallbackExpectation,
   String qaNotes,
   String renderIntegrationStatus,
   int convertedEntityCount,
   CreatorSurfaceIntegration surfaceIntegration,
   String notes
) {
   public CreatorAddonIntegration {
      namespace = namespace == null || namespace.isBlank() ? "unknown" : namespace;
      displayName = displayName == null || displayName.isBlank() ? namespace : displayName;
      status = status == null || status.isBlank() ? "declared" : status;
      surface = surface == null ? "" : surface;
      surfaceType = surfaceType == null || surfaceType.isBlank() ? surface : surfaceType;
      fallbackExpectation = fallbackExpectation == null || fallbackExpectation.isBlank() ? "stable_emissive" : fallbackExpectation;
      qaNotes = qaNotes == null ? "" : qaNotes;
      renderIntegrationStatus = renderIntegrationStatus == null || renderIntegrationStatus.isBlank() ? "fallback_renderer" : renderIntegrationStatus;
      convertedEntityCount = Math.max(0, convertedEntityCount);
      surfaceIntegration = surfaceIntegration == null ? CreatorSurfaceIntegration.EMPTY : surfaceIntegration;
      notes = notes == null ? "" : notes;
   }

   public JsonObject toJson() {
      JsonObject root = new JsonObject();
      root.addProperty("namespace", namespace);
      root.addProperty("display_name", displayName);
      root.addProperty("status", status);
      root.addProperty("surface", surface);
      root.addProperty("showcase_profile", showcaseProfile == null ? "" : showcaseProfile.toString());
      root.addProperty("surface_type", surfaceType);
      root.addProperty("fallback_expectation", fallbackExpectation);
      root.addProperty("qa_notes", qaNotes);
      JsonObject renderIntegration = new JsonObject();
      renderIntegration.addProperty("status", renderIntegrationStatus);
      renderIntegration.addProperty("converted_entity_count", convertedEntityCount);
      root.add("render_integration", renderIntegration);
      root.add("surface_integration", surfaceIntegration.toJson());
      root.addProperty("notes", notes);
      return root;
   }
}
