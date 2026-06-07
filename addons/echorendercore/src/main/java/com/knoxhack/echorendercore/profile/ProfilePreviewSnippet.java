package com.knoxhack.echorendercore.profile;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

public record ProfilePreviewSnippet(
   Identifier profileId,
   String title,
   String kind,
   JsonObject json
) {
   public ProfilePreviewSnippet {
      title = title == null || title.isBlank() ? profileId == null ? "profile" : profileId.toString() : title;
      kind = kind == null || kind.isBlank() ? "visual_profile" : kind;
      json = json == null ? new JsonObject() : json.deepCopy();
   }
}
