package com.knoxhack.echorendercore.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

public record CreatorProfileDraft(
   Identifier profileId,
   String title,
   String notes,
   String screenshotPath,
   String generatedPath,
   JsonObject normalizedProfile,
   boolean dirty
) {
   public CreatorProfileDraft {
      title = title == null || title.isBlank() ? defaultTitle(profileId) : title;
      notes = notes == null ? "" : notes;
      screenshotPath = normalizePath(screenshotPath);
      generatedPath = normalizePath(generatedPath);
      normalizedProfile = normalizedProfile == null ? new JsonObject() : normalizedProfile.deepCopy();
   }

   public static CreatorProfileDraft from(VisualProfile profile, CreatorProfileCard card) {
      JsonObject normalized = profile == null ? new JsonObject() : RenderCoreProfileMigration.normalizedVisualProfileJson(profile);
      String title = card == null ? defaultTitle(profile == null ? null : profile.id()) : card.title();
      String screenshot = card == null ? "" : card.screenshotPath();
      String path = profile == null || profile.id() == null
         ? ""
         : "assets/" + profile.id().getNamespace() + "/rendercore/visual_profiles/" + profile.id().getPath() + ".json";
      return new CreatorProfileDraft(profile == null ? null : profile.id(), title, "", screenshot, path, normalized, false);
   }

   public CreatorProfileDraft withTitle(String value) {
      return new CreatorProfileDraft(profileId, value, notes, screenshotPath, generatedPath, normalizedProfile, true);
   }

   public CreatorProfileDraft withNotes(String value) {
      return new CreatorProfileDraft(profileId, title, value, screenshotPath, generatedPath, normalizedProfile, true);
   }

   public CreatorProfileDraft withScreenshotPath(String value) {
      return new CreatorProfileDraft(profileId, title, notes, value, generatedPath, normalizedProfile, true);
   }

   public CreatorProfileDraft withProfileEffectPreset(String preset) {
      JsonObject root = normalizedProfile.deepCopy();
      root.add("effect", effectPreset(preset));
      return withProfileJson(root);
   }

   public CreatorProfileDraft withLayerEffectPreset(String layerId, String preset) {
      JsonObject root = normalizedProfile.deepCopy();
      JsonArray layers = array(root, "layers");
      for (int i = 0; i < layers.size(); i++) {
         if (!layers.get(i).isJsonObject()) {
            continue;
         }
         JsonObject layer = layers.get(i).getAsJsonObject();
         if (layerId.equals(layer.has("id") ? layer.get("id").getAsString() : "")) {
            layer.add("effect", effectPreset(preset));
            return withProfileJson(root);
         }
      }
      JsonObject layer = new JsonObject();
      layer.addProperty("id", layerId == null || layerId.isBlank() ? "layer" : layerId);
      layer.addProperty("kind", "overlay");
      layer.add("effect", effectPreset(preset));
      layers.add(layer);
      return withProfileJson(root);
   }

   public CreatorProfileDraft withMaterialEffectPreset(String materialId, String preset) {
      JsonObject root = normalizedProfile.deepCopy();
      JsonObject materials = object(root, "materials");
      String id = materialId == null || materialId.isBlank() ? "default" : materialId;
      JsonObject material = materials.has(id) && materials.get(id).isJsonObject() ? materials.getAsJsonObject(id) : new JsonObject();
      material.add("effect", effectPreset(preset));
      materials.add(id, material);
      return withProfileJson(root);
   }

   public CreatorProfileDraft withAnchor(String name, RenderCoreVector offset) {
      JsonObject root = normalizedProfile.deepCopy();
      JsonObject anchors = object(root, "anchors");
      JsonArray value = new JsonArray();
      RenderCoreVector vector = offset == null ? RenderCoreVector.ZERO : offset;
      value.add(vector.x());
      value.add(vector.y());
      value.add(vector.z());
      anchors.add(name == null || name.isBlank() ? "anchor" : name, value);
      return withProfileJson(root);
   }

   public CreatorProfileDraft withInclude(Identifier profile) {
      if (profile == null) {
         return this;
      }
      JsonObject root = normalizedProfile.deepCopy();
      array(root, "includes").add(profile.toString());
      return withProfileJson(root);
   }

   public CreatorProfileDraft clean() {
      return new CreatorProfileDraft(profileId, title, notes, screenshotPath, generatedPath, normalizedProfile, false);
   }

   public JsonObject toProfileJson() {
      JsonObject root = normalizedProfile.deepCopy();
      JsonObject preview = root.has("preview") && root.get("preview").isJsonObject()
         ? root.getAsJsonObject("preview")
         : new JsonObject();
      preview.addProperty("title", title);
      if (!notes.isBlank()) {
         preview.addProperty("notes", notes);
      }
      if (!screenshotPath.isBlank()) {
         preview.addProperty("screenshot", screenshotPath);
      }
      root.add("preview", preview);
      return root;
   }

   public JsonObject toJson() {
      JsonObject root = new JsonObject();
      root.addProperty("profile", profileId == null ? "" : profileId.toString());
      root.addProperty("title", title);
      root.addProperty("notes", notes);
      root.addProperty("screenshot", screenshotPath);
      root.addProperty("generated_path", generatedPath);
      root.addProperty("dirty", dirty);
      root.add("profile_json", toProfileJson());
      return root;
   }

   private static String defaultTitle(Identifier id) {
      return id == null ? "Untitled RenderCore Profile" : id.toString();
   }

   private static String normalizePath(String path) {
      if (path == null || path.isBlank()) {
         return "";
      }
      return path.replace('\\', '/').replaceFirst("^/+", "");
   }

   private CreatorProfileDraft withProfileJson(JsonObject value) {
      return new CreatorProfileDraft(profileId, title, notes, screenshotPath, generatedPath, value, true);
   }

   private static JsonObject effectPreset(String preset) {
      JsonObject effect = new JsonObject();
      VisualEffectKind kind = VisualEffectKind.byName(preset);
      effect.addProperty("preset", kind.supported() ? kind.name().toLowerCase(java.util.Locale.ROOT) : "none");
      return effect;
   }

   private static JsonObject object(JsonObject root, String key) {
      if (!root.has(key) || !root.get(key).isJsonObject()) {
         root.add(key, new JsonObject());
      }
      return root.getAsJsonObject(key);
   }

   private static JsonArray array(JsonObject root, String key) {
      if (!root.has(key) || !root.get(key).isJsonArray()) {
         root.add(key, new JsonArray());
      }
      return root.getAsJsonArray(key);
   }
}
