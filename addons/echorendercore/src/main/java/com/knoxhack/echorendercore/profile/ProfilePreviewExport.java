package com.knoxhack.echorendercore.profile;

import java.util.List;

public record ProfilePreviewExport(
   ProfilePreviewReport report,
   ProfilePreviewIndex index,
   List<ProfilePreviewSnippet> snippets
) {
   public ProfilePreviewExport {
      report = report == null ? ProfilePreviewReport.EMPTY : report;
      index = index == null ? new ProfilePreviewIndex("echorendercore", report.entries(), report.cacheMetrics()) : index;
      snippets = snippets == null ? List.of() : List.copyOf(snippets);
   }
}
