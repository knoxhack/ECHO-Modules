package com.knoxhack.echopresencelink.presence;

import com.knoxhack.echopresencelink.api.PresenceSanitizer;

public final class PresenceLinkDiagnostics {
    private static volatile Snapshot snapshot = new Snapshot(
            "STARTING",
            "ECHO Presence Link is initializing.",
            "",
            0L,
            "",
            false,
            "",
            "");

    private PresenceLinkDiagnostics() {
    }

    public static void disabled() {
        snapshot = new Snapshot("DISABLED", "Presence Link is disabled by config.", "", 0L, "",
                false, "", "");
    }

    public static void blankApplicationId() {
        snapshot = new Snapshot("APP_ID_BLANK", "Discord application id is blank; Rich Presence is inactive.",
                "", 0L, "", false, "", "");
    }

    public static void success(String providerId, long epochSeconds, boolean connected, String transportStatus,
            String endpoint, String lastResponse) {
        String status = connected ? "ONLINE" : "QUEUED";
        String detail = "Provider " + PresenceSanitizer.text(providerId, 96, "unknown")
                + " | " + PresenceSanitizer.text(transportStatus, 160, "transport ready");
        snapshot = new Snapshot(status, detail, PresenceSanitizer.text(providerId, 96, ""),
                Math.max(0L, epochSeconds), "", connected, endpoint, lastResponse);
    }

    public static void duplicateSuppressed(String providerId, long epochSeconds, boolean connected,
            String endpoint, String lastResponse) {
        snapshot = new Snapshot("UNCHANGED", "Duplicate Discord activity suppressed for provider "
                + PresenceSanitizer.text(providerId, 96, "unknown") + ".",
                PresenceSanitizer.text(providerId, 96, ""), Math.max(0L, epochSeconds), "",
                connected, endpoint, lastResponse);
    }

    public static void payloadRejectedFull(String providerId, String reason, long epochSeconds,
            String endpoint, String lastResponse) {
        snapshot = new Snapshot("PAYLOAD_REJECTED_FULL",
                "Full Rich Presence payload was rejected; minimal no-asset payload succeeded.",
                PresenceSanitizer.text(providerId, 96, ""), Math.max(0L, epochSeconds),
                PresenceSanitizer.text(reason, 180, "Discord rejected the full payload."),
                true, endpoint, lastResponse);
    }

    public static void failure(String providerId, String reason, long epochSeconds,
            String endpoint, String lastResponse) {
        snapshot = new Snapshot("OFFLINE", "Discord activity update failed.",
                PresenceSanitizer.text(providerId, 96, ""), Math.max(0L, epochSeconds),
                PresenceSanitizer.text(reason, 180, "Unknown failure."), false, endpoint, lastResponse);
    }

    public static void cleared() {
        snapshot = new Snapshot("CLEARED", "Discord activity was cleared.", "", 0L, "", false, "", "");
    }

    public static Snapshot snapshot() {
        return snapshot;
    }

    public record Snapshot(
            String status,
            String detail,
            String currentProviderId,
            long lastUpdateEpochSeconds,
            String lastFailure,
            boolean connected,
            String endpoint,
            String lastResponse) {
        public Snapshot {
            status = PresenceSanitizer.text(status, 48, "UNKNOWN");
            detail = PresenceSanitizer.text(detail, 180, "");
            currentProviderId = PresenceSanitizer.text(currentProviderId, 96, "");
            lastUpdateEpochSeconds = Math.max(0L, lastUpdateEpochSeconds);
            lastFailure = PresenceSanitizer.text(lastFailure, 180, "");
            endpoint = PresenceSanitizer.text(endpoint, 160, "");
            lastResponse = PresenceSanitizer.text(lastResponse, 240, "");
        }

        public String statusLine() {
            StringBuilder builder = new StringBuilder(status).append(" | ").append(detail);
            if (lastUpdateEpochSeconds > 0L) {
                long age = Math.max(0L, java.time.Instant.now().getEpochSecond() - lastUpdateEpochSeconds);
                builder.append(" | last update ").append(age).append("s ago");
            }
            if (!endpoint.isBlank()) {
                builder.append(" | endpoint ").append(endpoint);
            }
            if (!lastFailure.isBlank()) {
                builder.append(" | ").append(lastFailure);
            }
            return builder.toString();
        }
    }
}
