package com.knoxhack.echorendercore.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import net.minecraft.resources.Identifier;

public record CreatorMigrationReport(
   Identifier profileId,
   int sourceSchemaVersion,
   int targetSchemaVersion,
   boolean migrationRequired,
   boolean writeSafe,
   List<String> changes,
   List<ProfileValidationIssue> issues,
   String suggestedPath,
   JsonObject migratedJson
) {
   public static final CreatorMigrationReport EMPTY = new CreatorMigrationReport(
      null,
      VisualProfile.CURRENT_SCHEMA_VERSION,
      VisualProfile.CURRENT_SCHEMA_VERSION,
      false,
      true,
      List.of(),
      List.of(),
      "",
      new JsonObject()
   );

   public CreatorMigrationReport {
      sourceSchemaVersion = Math.max(0, sourceSchemaVersion);
      targetSchemaVersion = Math.max(1, targetSchemaVersion);
      changes = changes == null ? List.of() : List.copyOf(changes);
      issues = issues == null ? List.of() : List.copyOf(issues);
      suggestedPath = suggestedPath == null ? "" : suggestedPath;
      migratedJson = migratedJson == null ? new JsonObject() : migratedJson.deepCopy();
   }

   public JsonObject toJson() {
      JsonObject root = new JsonObject();
      root.addProperty("profile", profileId == null ? "" : profileId.toString());
      root.addProperty("source_schema_version", sourceSchemaVersion);
      root.addProperty("target_schema_version", targetSchemaVersion);
      root.addProperty("migration_required", migrationRequired);
      root.addProperty("write_safe", writeSafe);
      root.addProperty("suggested_path", suggestedPath);
      JsonArray changeArray = new JsonArray();
      changes.forEach(changeArray::add);
      root.add("changes", changeArray);
      JsonArray issueArray = new JsonArray();
      for (ProfileValidationIssue issue : issues) {
         JsonObject value = new JsonObject();
         value.addProperty("severity", issue.severity().name().toLowerCase(java.util.Locale.ROOT));
         value.addProperty("code", issue.code());
         value.addProperty("path", issue.path());
         value.addProperty("message", issue.message());
         value.addProperty("suggestion", issue.suggestion());
         issueArray.add(value);
      }
      root.add("issues", issueArray);
      root.add("migrated_profile", migratedJson.deepCopy());
      return root;
   }
}
