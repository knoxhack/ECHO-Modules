package com.knoxhack.echorendercore.profile;

import java.util.Locale;
import java.util.List;

public record ProfileValidationReport(List<ProfileValidationIssue> issues) {
   public static final ProfileValidationReport EMPTY = new ProfileValidationReport(List.of());

   public ProfileValidationReport {
      issues = issues == null ? List.of() : List.copyOf(issues);
   }

   public boolean hasErrors() {
      return issues.stream().anyMatch(issue -> issue.severity() == ProfileValidationSeverity.ERROR);
   }

   public long warnings() {
      return issues.stream().filter(issue -> issue.severity() == ProfileValidationSeverity.WARNING).count();
   }

   public long errors() {
      return issues.stream().filter(issue -> issue.severity() == ProfileValidationSeverity.ERROR).count();
   }

   public ProfileValidationReport forNamespace(String namespace) {
      if (namespace == null || namespace.isBlank() || namespace.equalsIgnoreCase("all")) {
         return this;
      }
      String normalized = namespace.toLowerCase(Locale.ROOT);
      return new ProfileValidationReport(issues.stream()
         .filter(issue -> issue.profileId() != null && issue.profileId().getNamespace().equals(normalized))
         .toList());
   }

   public ProfileValidationReport merge(ProfileValidationReport other) {
      if (other == null || other.issues().isEmpty()) {
         return this;
      }
      if (issues.isEmpty()) {
         return other;
      }
      java.util.ArrayList<ProfileValidationIssue> merged = new java.util.ArrayList<>(issues);
      merged.addAll(other.issues());
      return new ProfileValidationReport(merged);
   }

   public String summaryLine() {
      return warnings() + " warning(s), " + errors() + " error(s)";
   }
}
