package com.knoxhack.echorendercore.profile;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

public record ProfilePreviewArtifact(
   Identifier id,
   String suggestedPath,
   JsonObject json
) {
   public ProfilePreviewArtifact {
      suggestedPath = suggestedPath == null ? "" : suggestedPath;
      json = json == null ? new JsonObject() : json.deepCopy();
   }
}
