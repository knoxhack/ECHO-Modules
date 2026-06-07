package com.knoxhack.echorendercore.profile;

public record ProfileDiagnosticsReport(
   ProfileValidationReport validationReport,
   ProfilePerformanceReport performanceReport,
   ProfileCacheMetrics cacheMetrics
) {
   public static final ProfileDiagnosticsReport EMPTY =
      new ProfileDiagnosticsReport(ProfileValidationReport.EMPTY, ProfilePerformanceReport.EMPTY, ProfileCacheMetrics.EMPTY);

   public ProfileDiagnosticsReport {
      validationReport = validationReport == null ? ProfileValidationReport.EMPTY : validationReport;
      performanceReport = performanceReport == null ? ProfilePerformanceReport.EMPTY : performanceReport;
      cacheMetrics = cacheMetrics == null ? ProfileCacheMetrics.EMPTY : cacheMetrics;
   }
}
