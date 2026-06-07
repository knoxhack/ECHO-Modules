package com.knoxhack.echorendercore.profile;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

public final class RenderCoreProfileMigration {
   private RenderCoreProfileMigration() {
   }

   public static boolean requiresMigration(VisualProfile profile) {
      return profile == null || profile.schemaVersion() != VisualProfile.CURRENT_SCHEMA_VERSION;
   }

   public static ProfileValidationIssue migrationRequiredIssue(Identifier id, int sourceSchemaVersion) {
      return migrationRequiredIssue(ProfileValidationSeverity.ERROR, id, sourceSchemaVersion);
   }

   public static ProfileValidationIssue migrationRequiredIssue(ProfileValidationSeverity severity, Identifier id, int sourceSchemaVersion) {
      return new ProfileValidationIssue(
         severity,
         id,
         "migration_required",
         "schema_version",
         "Visual profile uses schema_version " + sourceSchemaVersion
            + "; RenderCore V12 runtime activates schema_version " + VisualProfile.CURRENT_SCHEMA_VERSION
            + " and auto-migrates schema_version 11.",
         "Run /rendercore creator migrate " + (id == null ? "all" : id.getNamespace())
            + " dryrun, then write the generated V12 profile JSON when the report looks correct."
      );
   }

   public static CreatorMigrationReport migrateVisualProfile(Identifier id, JsonObject source) {
      JsonObject safeSource = source == null ? new JsonObject() : source.deepCopy();
      int sourceSchema = RenderCoreJsonParsers.visualSchemaVersion(safeSource);
      ArrayList<String> changes = new ArrayList<>();
      ArrayList<ProfileValidationIssue> issues = new ArrayList<>();
      JsonObject migrated = safeSource.deepCopy();
      boolean migrationRequired = sourceSchema != VisualProfile.CURRENT_SCHEMA_VERSION;
      if (migrationRequired) {
         issues.add(migrationRequiredIssue(ProfileValidationSeverity.WARNING, id, sourceSchema));
         changes.add("schema_version " + sourceSchema + " -> " + VisualProfile.CURRENT_SCHEMA_VERSION);
      }
      if (migrated.has("schemaVersion")) {
         migrated.remove("schemaVersion");
         changes.add("removed camelCase schemaVersion alias");
      }
      migrated.addProperty("schema_version", VisualProfile.CURRENT_SCHEMA_VERSION);
      ensureV12Sections(id, migrated, changes);
      normalizeEffectContainer(migrated, "effect", changes);
      normalizeMaterials(migrated, changes);
      normalizeLayers(migrated, changes);
      if (sourceSchema < 8) {
         changes.add("effect defaults to none for pre-V8 content");
      }
      if (sourceSchema < 10) {
         changes.add("advanced bloom mask fields use V12 defaults");
      }
      if (sourceSchema == 11) {
         changes.add("auto-migrated V11 runtime profile to V12 metadata contract");
      }
      return new CreatorMigrationReport(
         id,
         sourceSchema,
         VisualProfile.CURRENT_SCHEMA_VERSION,
         migrationRequired,
         true,
         changes,
         issues,
         suggestedPath(id),
         migrated
      );
   }

   public static JsonObject normalizedVisualProfileJson(VisualProfile profile) {
      if (profile == null) {
         JsonObject empty = new JsonObject();
         empty.addProperty("schema_version", VisualProfile.CURRENT_SCHEMA_VERSION);
         return empty;
      }
      VisualProfileBuilder builder = VisualProfileBuilder.create(profile.id())
         .schemaVersion(VisualProfile.CURRENT_SCHEMA_VERSION)
         .baseTexture(profile.baseTexture())
         .animationProfile(profile.animationProfile())
         .particleProfile(profile.particleProfile())
         .defaultState(profile.defaultState())
         .transitionSeconds(profile.transitionSeconds())
         .effect(profile.effect())
         .surface(profile.surface())
         .fallback(profile.fallback())
         .budget(profile.budget())
         .screenChrome(profile.screenChrome())
         .qa(profile.qa());
      profile.stateAnimations().forEach(builder::stateAnimation);
      profile.stateTextureVariants().forEach(builder::stateVariantTexture);
      profile.variantTextures().forEach(builder::variantTexture);
      profile.anchors().forEach((name, anchor) -> builder.anchor(name, anchor.offset()));
      profile.materials().values().stream()
         .sorted(java.util.Comparator.comparing(VisualMaterial::id))
         .forEach(builder::material);
      profile.blockParts().entrySet().stream()
         .sorted(Map.Entry.comparingByKey())
         .forEach(entry -> builder.blockPart(entry.getKey(), entry.getValue()));
      profile.includes().forEach(builder::include);
      profile.layers().forEach(builder::layer);
      return builder.toJson();
   }

   private static void normalizeMaterials(JsonObject migrated, ArrayList<String> changes) {
      JsonElement materials = migrated.get("materials");
      if (materials == null || !materials.isJsonObject()) {
         return;
      }
      for (Map.Entry<String, JsonElement> entry : materials.getAsJsonObject().entrySet()) {
         if (entry.getValue().isJsonObject()) {
            normalizeEffectContainer(entry.getValue().getAsJsonObject(), "materials." + entry.getKey(), changes);
         }
      }
   }

   private static void normalizeLayers(JsonObject migrated, ArrayList<String> changes) {
      JsonElement layers = migrated.get("layers");
      if (layers == null || !layers.isJsonArray()) {
         return;
      }
      int index = 0;
      for (JsonElement layer : layers.getAsJsonArray()) {
         if (layer.isJsonObject()) {
            normalizeEffectContainer(layer.getAsJsonObject(), "layers." + index, changes);
         }
         index++;
      }
   }

   private static void normalizeEffectContainer(JsonObject json, String path, ArrayList<String> changes) {
      if (json.has("effects") && !json.has("effect")) {
         json.add("effect", json.get("effects").deepCopy());
         changes.add("renamed " + path + ".effects to effect");
      }
      json.remove("effects");
   }

   private static void ensureV12Sections(Identifier id, JsonObject migrated, ArrayList<String> changes) {
      if (!migrated.has("surface") || !migrated.get("surface").isJsonObject()) {
         migrated.add("surface", defaultSurface(id));
         changes.add("added V12 surface metadata");
      }
      if (!migrated.has("fallback") || !migrated.get("fallback").isJsonObject()) {
         migrated.add("fallback", defaultFallback());
         changes.add("added V12 fallback policy");
      }
      if (!migrated.has("budget") || !migrated.get("budget").isJsonObject()) {
         migrated.add("budget", defaultBudget(id));
         changes.add("added V12 budget policy");
      }
      if (!migrated.has("screen_chrome") || !migrated.get("screen_chrome").isJsonObject()) {
         migrated.add("screen_chrome", defaultScreenChrome(id));
         changes.add("added V12 screen chrome policy");
      }
      if (!migrated.has("qa") || !migrated.get("qa").isJsonObject()) {
         migrated.add("qa", defaultQa(id));
         changes.add("added V12 QA expectations");
      }
   }

   private static JsonObject defaultSurface(Identifier id) {
      JsonObject surface = new JsonObject();
      String path = id == null ? "" : id.getPath();
      surface.addProperty("type", inferSurfaceType(path));
      surface.addProperty("owner_addon", id == null ? "unknown" : id.getNamespace());
      surface.addProperty("display_name", displayName(id));
      surface.add("tags", new JsonArray());
      surface.addProperty("integration_mode", "optional");
      return surface;
   }

   private static JsonObject defaultFallback() {
      JsonObject fallback = new JsonObject();
      fallback.addProperty("mode", "stable");
      fallback.addProperty("expectation", "stable RenderCore fallback remains readable");
      fallback.addProperty("allow_advanced_fx_fallback", true);
      fallback.addProperty("allow_missing_assets", false);
      return fallback;
   }

   private static JsonObject defaultBudget(Identifier id) {
      JsonObject budget = new JsonObject();
      String path = id == null ? "" : id.getPath();
      budget.addProperty("tier", path.contains("vehicle") || path.contains("multiblock") ? "high" : "normal");
      budget.addProperty("advanced_fx", "auto");
      return budget;
   }

   private static JsonObject defaultScreenChrome(Identifier id) {
      JsonObject chrome = new JsonObject();
      String path = id == null ? "" : id.getPath().toLowerCase(Locale.ROOT);
      String style = inferScreenChromeStyle(path);
      chrome.addProperty("style", style);
      chrome.addProperty("label", inferScreenLabel(path, style));
      chrome.addProperty("backdrop", false);
      chrome.addProperty("edge_glow", true);
      chrome.addProperty("corner_brackets", true);
      chrome.addProperty("accent_rails", true);
      chrome.addProperty("scanlines", false);
      chrome.addProperty("glass_glints", !"terminal".equals(style));
      chrome.addProperty("chromatic_edge", !"minimal".equals(style));
      chrome.addProperty("quiet_fallback", path.contains("overlay"));
      return chrome;
   }

   private static JsonObject defaultQa(Identifier id) {
      JsonObject qa = new JsonObject();
      String path = id == null ? "" : id.getPath();
      boolean screen = path.startsWith("screen/") || path.contains("cyberglass");
      qa.addProperty("required", screen);
      JsonArray evidence = new JsonArray();
      if (screen) {
         evidence.add("screenshot");
         evidence.add("screen_chrome");
      }
      qa.add("evidence", evidence);
      qa.addProperty("reduced_motion", path.contains("terminal"));
      return qa;
   }

   private static String inferSurfaceType(String path) {
      String normalized = path == null ? "" : path.toLowerCase(Locale.ROOT);
      if (normalized.startsWith("screen/") || normalized.contains("screen") || normalized.contains("overlay")) {
         return normalized.contains("overlay") ? "hud_overlay" : "screen";
      }
      if (normalized.startsWith("echo_mobs/")) {
         return "mob_family";
      }
      if (normalized.contains("particle")) {
         return "particle_only";
      }
      return "unknown";
   }

   private static String inferScreenChromeStyle(String path) {
      if (path.contains("terminal")) {
         return "terminal";
      }
      if (path.contains("minimap") || path.contains("lens") || path.contains("hologram")) {
         return "hologram";
      }
      if (path.contains("neon")) {
         return "neon";
      }
      if (path.contains("minimal")) {
         return "minimal";
      }
      return "cyberglass";
   }

   private static String inferScreenLabel(String path, String style) {
      if (path.contains("terminal_hud") || path.contains("echo_terminal")) {
         return "ECHO TERMINAL";
      }
      if (path.contains("server_rack")) {
         return "SIGNALOS RACK";
      }
      if (path.contains("minimap")) {
         return "HOLOMAP";
      }
      if (path.contains("terminal")) {
         return "TERMINAL";
      }
      return "cyberglass".equals(style) || "hologram".equals(style) ? "" : style.toUpperCase(Locale.ROOT);
   }

   private static String displayName(Identifier id) {
      if (id == null) {
         return "Unknown Surface";
      }
      String raw = id.getPath().replace('/', ' ').replace('_', ' ').trim();
      if (raw.isBlank()) {
         return id.toString();
      }
      StringBuilder builder = new StringBuilder();
      for (String word : raw.split(" ")) {
         if (word.isBlank()) {
            continue;
         }
         if (!builder.isEmpty()) {
            builder.append(' ');
         }
         builder.append(Character.toUpperCase(word.charAt(0)));
         if (word.length() > 1) {
            builder.append(word.substring(1));
         }
      }
      return builder.isEmpty() ? id.toString() : builder.toString();
   }

   private static String suggestedPath(Identifier id) {
      if (id == null) {
         return "generated/rendercore_migrations/unknown.visual_profile.json";
      }
      return "generated/rendercore_migrations/assets/" + id.getNamespace()
         + "/rendercore/visual_profiles/" + id.getPath() + ".json";
   }
}
