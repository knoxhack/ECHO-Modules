package com.knoxhack.echorendercore.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.resources.Identifier;

public record CreatorCertificationReport(
   String namespace,
   CreatorCertificationResult status,
   CreatorCertificationPolicy policy,
   int checkedProfileCount,
   int passingProfileCount,
   int warningProfileCount,
   int failingProfileCount,
   long validationWarningCount,
   long validationErrorCount,
   int performanceWarningCount,
   int migrationRequiredCount,
   int missingDependencyCount,
   int malformedSourceCount,
   int missingScreenshotCount,
   int artifactFailureCount,
   List<IssueSummary> issueSummaries
) {
   private static final Comparator<IssueSummary> ISSUE_ORDER = Comparator
      .comparing(IssueSummary::profile)
      .thenComparingInt(issue -> severityRank(issue.severity()))
      .thenComparing(IssueSummary::code)
      .thenComparing(IssueSummary::path)
      .thenComparing(IssueSummary::category)
      .thenComparing(IssueSummary::message);

   public static final CreatorCertificationReport EMPTY = certify(
      "all",
      List.of(),
      List.of(),
      List.of(),
      ProfileDiagnosticsReport.EMPTY,
      CreatorCertificationPolicy.ERRORS_ONLY
   );

   public CreatorCertificationReport {
      namespace = namespace == null || namespace.isBlank() ? "all" : namespace.toLowerCase(Locale.ROOT);
      status = status == null ? CreatorCertificationResult.WARN : status;
      policy = policy == null ? CreatorCertificationPolicy.ERRORS_ONLY : policy;
      checkedProfileCount = Math.max(0, checkedProfileCount);
      passingProfileCount = Math.max(0, passingProfileCount);
      warningProfileCount = Math.max(0, warningProfileCount);
      failingProfileCount = Math.max(0, failingProfileCount);
      validationWarningCount = Math.max(0, validationWarningCount);
      validationErrorCount = Math.max(0, validationErrorCount);
      performanceWarningCount = Math.max(0, performanceWarningCount);
      migrationRequiredCount = Math.max(0, migrationRequiredCount);
      missingDependencyCount = Math.max(0, missingDependencyCount);
      malformedSourceCount = Math.max(0, malformedSourceCount);
      missingScreenshotCount = Math.max(0, missingScreenshotCount);
      artifactFailureCount = Math.max(0, artifactFailureCount);
      issueSummaries = sortedDistinct(issueSummaries);
   }

   public static CreatorCertificationReport certify(
         String namespace,
         List<CreatorProfileCard> cards,
         List<CreatorProfileAudit> audits,
         List<CreatorMigrationReport> migrationReports,
         ProfileDiagnosticsReport diagnostics,
         CreatorCertificationPolicy policy) {
      ProfileDiagnosticsReport safeDiagnostics = diagnostics == null ? ProfileDiagnosticsReport.EMPTY : diagnostics;
      ArrayList<IssueSummary> issues = new ArrayList<>();
      addValidationIssues(issues, safeDiagnostics.validationReport().issues());
      if (safeDiagnostics.validationReport().issues().isEmpty()) {
         List<CreatorProfileAudit> safeAudits = audits == null ? List.of() : audits;
         safeAudits.forEach(audit -> addValidationIssues(issues, audit.validationIssues()));
      }
      addPerformanceIssues(issues, safeDiagnostics.performanceReport().issues());
      if (safeDiagnostics.performanceReport().issues().isEmpty()) {
         List<CreatorProfileAudit> safeAudits = audits == null ? List.of() : audits;
         safeAudits.forEach(audit -> addPerformanceIssues(issues, audit.performanceIssues()));
      }
      List<CreatorMigrationReport> safeReports = migrationReports == null ? List.of() : migrationReports;
      for (CreatorMigrationReport report : safeReports) {
         if (report.migrationRequired()) {
            issues.add(new IssueSummary(
               "migration",
               ProfileValidationSeverity.ERROR.name().toLowerCase(Locale.ROOT),
               profileString(report.profileId()),
               "migration_required",
               "schema_version",
               "Visual profile requires migration before runtime activation.",
               true
            ));
         }
         addValidationIssues(issues, report.issues());
      }
      List<CreatorProfileCard> safeCards = cards == null ? List.of() : cards;
      for (CreatorProfileCard card : safeCards) {
         if (!card.screenshotAvailable()) {
            issues.add(new IssueSummary(
               "screenshot",
               ProfileValidationSeverity.WARNING.name().toLowerCase(Locale.ROOT),
               profileString(card.profileId()),
               "screenshot_missing",
               "preview.screenshot",
               "Optional screenshot preview is unavailable; export will use deterministic metadata cards.",
               false
            ));
         }
      }
      int migrationIssues = distinctProfiles(issues, "migration_required").size();
      int malformedSources = Math.max(0, safeDiagnostics.cacheMetrics().failedJsonCount() - migrationIssues);
      if (malformedSources > 0) {
         issues.add(new IssueSummary(
            "source",
            ProfileValidationSeverity.ERROR.name().toLowerCase(Locale.ROOT),
            "",
            "malformed_source_json",
            "resource_reload",
            "One or more RenderCore source JSON files could not be parsed.",
            true
         ));
      }
      return fromSummaries(namespace, safeCards, safeReports, policy, issues, malformedSources, 0);
   }

   public CreatorCertificationReport forNamespace(
         String namespace,
         List<CreatorProfileCard> cards,
         List<CreatorProfileAudit> audits,
         List<CreatorMigrationReport> migrationReports) {
      String normalized = namespace == null || namespace.isBlank() ? "all" : namespace.toLowerCase(Locale.ROOT);
      if ("all".equals(normalized)) {
         return this;
      }
      List<IssueSummary> filteredIssues = issueSummaries.stream()
         .filter(issue -> profileNamespace(issue.profile()).equals(normalized))
         .toList();
      List<CreatorProfileCard> safeCards = cards == null ? List.of() : cards;
      List<CreatorMigrationReport> safeReports = migrationReports == null ? List.of() : migrationReports;
      return fromSummaries(normalized, safeCards, safeReports, policy, filteredIssues, 0, artifactFailureCount);
   }

   public boolean failed() {
      return status == CreatorCertificationResult.FAIL;
   }

   public JsonObject toJson() {
      JsonObject root = new JsonObject();
      root.addProperty("namespace", namespace);
      root.addProperty("status", status.id());
      root.addProperty("policy", policy.id());
      root.addProperty("checked_profile_count", checkedProfileCount);
      root.addProperty("passing_profile_count", passingProfileCount);
      root.addProperty("warning_profile_count", warningProfileCount);
      root.addProperty("failing_profile_count", failingProfileCount);
      root.addProperty("validation_warning_count", validationWarningCount);
      root.addProperty("validation_error_count", validationErrorCount);
      root.addProperty("performance_warning_count", performanceWarningCount);
      root.addProperty("migration_required_count", migrationRequiredCount);
      root.addProperty("missing_dependency_count", missingDependencyCount);
      root.addProperty("malformed_source_count", malformedSourceCount);
      root.addProperty("missing_screenshot_count", missingScreenshotCount);
      root.addProperty("artifact_failure_count", artifactFailureCount);
      JsonArray summaries = new JsonArray();
      issueSummaries.forEach(issue -> summaries.add(issue.toJson()));
      root.add("issue_summaries", summaries);
      return root;
   }

   public String summaryLine() {
      return "certification " + status.id()
         + " (" + policy.id() + ")"
         + ", checked " + checkedProfileCount
         + ", pass/warn/fail " + passingProfileCount + "/" + warningProfileCount + "/" + failingProfileCount
         + ", validation W:" + validationWarningCount + " E:" + validationErrorCount
         + ", perf W:" + performanceWarningCount
         + ", migrations " + migrationRequiredCount
         + ", missing deps " + missingDependencyCount
         + ", malformed " + malformedSourceCount;
   }

   private static CreatorCertificationReport fromSummaries(
         String namespace,
         List<CreatorProfileCard> cards,
         List<CreatorMigrationReport> migrationReports,
         CreatorCertificationPolicy policy,
         List<IssueSummary> summaries,
         int malformedSourceCount,
         int artifactFailureCount) {
      List<IssueSummary> issues = sortedDistinct(summaries);
      LinkedHashSet<String> checked = new LinkedHashSet<>();
      LinkedHashSet<String> failing = new LinkedHashSet<>();
      LinkedHashSet<String> warning = new LinkedHashSet<>();
      List<CreatorProfileCard> safeCards = cards == null ? List.of() : cards;
      safeCards.stream()
         .sorted(Comparator.comparing(card -> profileString(card.profileId())))
         .forEach(card -> checked.add(profileString(card.profileId())));
      List<CreatorMigrationReport> safeReports = migrationReports == null ? List.of() : migrationReports;
      safeReports.stream()
         .sorted(Comparator.comparing(report -> profileString(report.profileId())))
         .forEach(report -> {
            String profile = profileString(report.profileId());
            if (!profile.isBlank()) {
               checked.add(profile);
            }
            if (report.migrationRequired() && !profile.isBlank()) {
               failing.add(profile);
            }
         });
      for (IssueSummary issue : issues) {
         if (!issue.profile().isBlank()) {
            checked.add(issue.profile());
         }
         if (issue.blocking()) {
            if (!issue.profile().isBlank()) {
               failing.add(issue.profile());
            }
         } else if (ProfileValidationSeverity.WARNING.name().equalsIgnoreCase(issue.severity()) && !issue.profile().isBlank()) {
            warning.add(issue.profile());
         }
      }
      warning.removeAll(failing);
      int unknownFailures = Math.max(0, malformedSourceCount) + Math.max(0, artifactFailureCount);
      int checkedCount = checked.size() + unknownFailures;
      int failingCount = failing.size() + unknownFailures;
      int warningCount = warning.size();
      int passingCount = Math.max(0, checkedCount - failingCount - warningCount);
      long validationWarnings = issues.stream()
         .filter(issue -> "validation".equals(issue.category()))
         .filter(issue -> ProfileValidationSeverity.WARNING.name().equalsIgnoreCase(issue.severity()))
         .count();
      long validationErrors = issues.stream()
         .filter(issue -> "validation".equals(issue.category()))
         .filter(issue -> ProfileValidationSeverity.ERROR.name().equalsIgnoreCase(issue.severity()))
         .count();
      int performanceWarnings = (int)issues.stream()
         .filter(issue -> "performance".equals(issue.category()))
         .filter(issue -> ProfileValidationSeverity.WARNING.name().equalsIgnoreCase(issue.severity()))
         .count();
      int migrationRequired = distinctProfiles(issues, "migration_required").size();
      int missingDependencies = (int)issues.stream().filter(issue -> isMissingDependencyCode(issue.code())).count();
      int missingScreenshots = (int)issues.stream().filter(issue -> "screenshot_missing".equals(issue.code())).count();
      boolean fail = validationErrors > 0
         || migrationRequired > 0
         || missingDependencies > 0
         || malformedSourceCount > 0
         || artifactFailureCount > 0
         || issues.stream().anyMatch(IssueSummary::blocking);
      boolean warn = !fail && (validationWarnings > 0 || performanceWarnings > 0 || missingScreenshots > 0);
      CreatorCertificationResult status = fail ? CreatorCertificationResult.FAIL : (warn ? CreatorCertificationResult.WARN : CreatorCertificationResult.PASS);
      return new CreatorCertificationReport(
         namespace,
         status,
         policy,
         checkedCount,
         passingCount,
         warningCount,
         failingCount,
         validationWarnings,
         validationErrors,
         performanceWarnings,
         migrationRequired,
         missingDependencies,
         malformedSourceCount,
         missingScreenshots,
         artifactFailureCount,
         issues
      );
   }

   private static void addValidationIssues(List<IssueSummary> issues, List<ProfileValidationIssue> validationIssues) {
      if (validationIssues == null) {
         return;
      }
      for (ProfileValidationIssue issue : validationIssues) {
         if (isPerformanceCode(issue.code())) {
            continue;
         }
         boolean blocking = issue.severity() == ProfileValidationSeverity.ERROR
            || "migration_required".equals(issue.code())
            || isMissingDependencyCode(issue.code());
         issues.add(new IssueSummary(
            "validation",
            issue.severity().name().toLowerCase(Locale.ROOT),
            profileString(issue.profileId()),
            issue.code(),
            issue.path(),
            issue.message(),
            blocking
         ));
      }
   }

   private static void addPerformanceIssues(List<IssueSummary> issues, List<ProfilePerformanceIssue> performanceIssues) {
      if (performanceIssues == null) {
         return;
      }
      for (ProfilePerformanceIssue issue : performanceIssues) {
         issues.add(new IssueSummary(
            "performance",
            issue.severity().name().toLowerCase(Locale.ROOT),
            profileString(issue.profileId()),
            issue.code(),
            "performance",
            issue.message(),
            false
         ));
      }
   }

   private static List<IssueSummary> sortedDistinct(List<IssueSummary> issues) {
      if (issues == null || issues.isEmpty()) {
         return List.of();
      }
      LinkedHashMap<String, IssueSummary> distinct = new LinkedHashMap<>();
      issues.stream()
         .filter(issue -> issue != null)
         .sorted(ISSUE_ORDER)
         .forEach(issue -> distinct.putIfAbsent(issue.key(), issue));
      return List.copyOf(distinct.values());
   }

   private static Set<String> distinctProfiles(List<IssueSummary> issues, String code) {
      LinkedHashSet<String> profiles = new LinkedHashSet<>();
      if (issues != null) {
         issues.stream()
            .filter(issue -> code.equals(issue.code()))
            .map(IssueSummary::profile)
            .filter(profile -> !profile.isBlank())
            .forEach(profiles::add);
      }
      return profiles;
   }

   private static boolean isPerformanceCode(String code) {
      return code != null && code.startsWith("profile_perf_");
   }

   private static boolean isMissingDependencyCode(String code) {
      return "missing_profile_reference".equals(code)
         || "missing_profile_include".equals(code)
         || "missing_animation_clip".equals(code)
         || "profile_include_cycle".equals(code);
   }

   private static String profileString(Identifier id) {
      return id == null ? "" : id.toString();
   }

   private static String profileNamespace(String profile) {
      if (profile == null || profile.isBlank()) {
         return "";
      }
      int separator = profile.indexOf(':');
      return separator > 0 ? profile.substring(0, separator).toLowerCase(Locale.ROOT) : "";
   }

   private static int severityRank(String severity) {
      if (ProfileValidationSeverity.ERROR.name().equalsIgnoreCase(severity)) {
         return 0;
      }
      if (ProfileValidationSeverity.WARNING.name().equalsIgnoreCase(severity)) {
         return 1;
      }
      return 2;
   }

   public record IssueSummary(
      String category,
      String severity,
      String profile,
      String code,
      String path,
      String message,
      boolean blocking
   ) {
      public IssueSummary {
         category = category == null || category.isBlank() ? "validation" : category.toLowerCase(Locale.ROOT);
         severity = severity == null || severity.isBlank()
            ? ProfileValidationSeverity.WARNING.name().toLowerCase(Locale.ROOT)
            : severity.toLowerCase(Locale.ROOT);
         profile = profile == null ? "" : profile;
         code = code == null || code.isBlank() ? "general" : code;
         path = path == null ? "" : path;
         message = message == null ? "" : message;
      }

      public JsonObject toJson() {
         JsonObject root = new JsonObject();
         root.addProperty("category", category);
         root.addProperty("severity", severity);
         root.addProperty("profile", profile);
         root.addProperty("code", code);
         root.addProperty("path", path);
         root.addProperty("message", message);
         root.addProperty("blocking", blocking);
         return root;
      }

      private String key() {
         return category + "\n" + severity + "\n" + profile + "\n" + code + "\n" + path + "\n" + message + "\n" + blocking;
      }
   }
}
