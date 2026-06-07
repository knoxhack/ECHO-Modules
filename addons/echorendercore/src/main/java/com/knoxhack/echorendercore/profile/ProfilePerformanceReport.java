package com.knoxhack.echorendercore.profile;

import java.util.List;
import java.util.Map;

public record ProfilePerformanceReport(
        Map<String, ProfilePerformanceSummary> summaries,
        List<ProfilePerformanceIssue> issues
) {
    public static final ProfilePerformanceReport EMPTY = new ProfilePerformanceReport(Map.of(), List.of());

    public ProfilePerformanceReport {
        summaries = summaries == null ? Map.of() : Map.copyOf(summaries);
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public int warningCount() {
        return (int) issues.stream()
                .filter(issue -> issue.severity() == ProfileValidationSeverity.WARNING)
                .count();
    }

    public ProfileValidationReport asValidationReport() {
        if (issues.isEmpty()) {
            return ProfileValidationReport.EMPTY;
        }
        return new ProfileValidationReport(issues.stream()
                .map(issue -> new ProfileValidationIssue(
                        issue.severity(),
                        issue.profileId(),
                        issue.code(),
                        "visual_profile",
                        issue.message(),
                        suggestionFor(issue.code())))
                .toList());
    }

    private static String suggestionFor(String code) {
        return switch (code) {
            case "profile_perf_high_layer_count" -> "Combine compatible layers or gate expensive layers by state/variant.";
            case "profile_perf_high_emitter_rate" -> "Lower emitter rate or burst values for steady-state effects.";
            case "profile_perf_high_animation_track_count" -> "Split rarely used clips or reduce animated part/channel count.";
            case "profile_perf_high_effect_cost" -> "Reduce bloom, scanline, hue-shift, or pulse-heavy layers on profiles rendered many times.";
            case "profile_perf_high_bloom_cost" -> "Reduce bloom_radius, bloom_passes, screen_blend, or the number of bloom-capable layers.";
            case "budget_exceeded" -> "Adjust the V12 budget tier or reduce layers, emitters, effect cost, bloom cost, or mask submissions.";
            default -> "Review the profile for unnecessary runtime work.";
        };
    }
}
