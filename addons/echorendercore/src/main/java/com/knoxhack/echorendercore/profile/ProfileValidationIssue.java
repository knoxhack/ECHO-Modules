package com.knoxhack.echorendercore.profile;

import net.minecraft.resources.Identifier;

public record ProfileValidationIssue(
   ProfileValidationSeverity severity,
   Identifier profileId,
   String code,
   String path,
   String message,
   String suggestion
) {
   public ProfileValidationIssue(ProfileValidationSeverity severity, Identifier profileId, String path, String message) {
      this(severity, profileId, "general", path, message, "");
   }

   public ProfileValidationIssue {
      severity = severity == null ? ProfileValidationSeverity.WARNING : severity;
      code = code == null || code.isBlank() ? "general" : code.trim();
      path = path == null ? "" : path;
      message = message == null ? "" : message;
      suggestion = suggestion == null ? "" : suggestion;
   }
}
