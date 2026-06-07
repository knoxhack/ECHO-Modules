package com.knoxhack.echorendercore.profile;

import java.util.List;
import java.util.Locale;
import net.minecraft.resources.Identifier;

public record ProfilePreviewReport(
   List<ProfilePreviewEntry> entries,
   List<ProfilePreviewArtifact> artifacts,
   List<ProfileValidationIssue> issues,
   ProfileCacheMetrics cacheMetrics
) {
   public static final ProfilePreviewReport EMPTY =
      new ProfilePreviewReport(List.of(), List.of(), List.of(), ProfileCacheMetrics.EMPTY);

   public ProfilePreviewReport {
      entries = entries == null ? List.of() : List.copyOf(entries);
      artifacts = artifacts == null ? List.of() : List.copyOf(artifacts);
      issues = issues == null ? List.of() : List.copyOf(issues);
      cacheMetrics = cacheMetrics == null ? ProfileCacheMetrics.EMPTY : cacheMetrics;
   }

   public ProfilePreviewReport forNamespace(String namespace) {
      if (namespace == null || namespace.isBlank() || namespace.equalsIgnoreCase("all")) {
         return this;
      }
      String normalized = namespace.toLowerCase(Locale.ROOT);
      return new ProfilePreviewReport(
         entries.stream().filter(entry -> namespaceMatches(entry.profileId(), normalized)).toList(),
         artifacts.stream().filter(artifact -> namespaceMatches(artifact.id(), normalized)).toList(),
         issues.stream().filter(issue -> namespaceMatches(issue.profileId(), normalized)).toList(),
         cacheMetrics
      );
   }

   public long warnings() {
      return issues.stream().filter(issue -> issue.severity() == ProfileValidationSeverity.WARNING).count();
   }

   public long errors() {
      return issues.stream().filter(issue -> issue.severity() == ProfileValidationSeverity.ERROR).count();
   }

   public String summaryLine() {
      return entries.size() + " preview entr" + (entries.size() == 1 ? "y" : "ies")
         + ", artifacts " + artifacts.size()
         + ", preview W:" + warnings() + " E:" + errors()
         + ", " + cacheMetrics.summaryLine();
   }

   private static boolean namespaceMatches(Identifier id, String namespace) {
      return id != null && id.getNamespace().equals(namespace);
   }
}
