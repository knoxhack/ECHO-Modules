package com.knoxhack.echorendercore.profile;

public record ComposedVisualProfile(
   VisualProfile original,
   VisualProfile composed,
   ProfileValidationReport report
) {
   public ComposedVisualProfile {
      report = report == null ? ProfileValidationReport.EMPTY : report;
   }
}
