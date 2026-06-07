package com.knoxhack.echo.reportcore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record EchoReportRedactionPolicy(
        boolean secretsRedacted,
        boolean includeEnvironmentVariables,
        boolean includeUserAbsolutePaths,
        boolean includeLogs,
        boolean includeCrashReports,
        List<String> redactedKeyPatterns,
        String privacyNote,
        Map<String, String> attributes
) {
    private static final List<String> DEFAULT_REDACTED_KEYS = List.of(
            "token",
            "secret",
            "password",
            "apikey",
            "api_key",
            "authorization",
            "session"
    );

    public EchoReportRedactionPolicy {
        secretsRedacted = true;
        includeEnvironmentVariables = false;
        redactedKeyPatterns = withDefaults(redactedKeyPatterns);
        privacyNote = ReportContractGuards.optionalText(privacyNote);
        attributes = ReportContractGuards.immutableMap(attributes);
    }

    public static EchoReportRedactionPolicy localDefault() {
        return new EchoReportRedactionPolicy(true, false, false, false, false, List.of(), "Local report with secret fields redacted.", Map.of());
    }

    public static EchoReportRedactionPolicy supportBundleDefault() {
        return new EchoReportRedactionPolicy(true, false, false, true, true, List.of(), "Support bundle metadata is local-first and secret-redacted by contract.", Map.of());
    }

    private static List<String> withDefaults(List<String> values) {
        List<String> merged = new ArrayList<>(DEFAULT_REDACTED_KEYS);
        if (values != null) {
            values.stream()
                    .map(ReportContractGuards::optionalText)
                    .filter(value -> !value.isBlank())
                    .forEach(merged::add);
        }
        return List.copyOf(merged);
    }
}
