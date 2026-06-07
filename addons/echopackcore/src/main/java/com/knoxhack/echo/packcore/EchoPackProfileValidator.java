package com.knoxhack.echo.packcore;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.knoxhack.echo.platformcore.EchoPackId;
import com.knoxhack.echo.validationcore.EchoDiagnosticSeverity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class EchoPackProfileValidator {
    private static final Set<String> VALID_VARIANTS = EchoPackConstants.BUILTIN_VARIANTS.stream()
            .map(variant -> variant.id().value())
            .collect(Collectors.toUnmodifiableSet());
    private static final Set<String> VALID_CHANNELS = EchoPackConstants.BUILTIN_CHANNELS.stream()
            .map(channel -> channel.id().value())
            .collect(Collectors.toUnmodifiableSet());

    public List<EchoPackProfileIssue> validate(
            JsonObject rawProfile,
            Map<String, Object> normalizedProfile,
            EchoPackProfileSource source,
            EchoPackId requestedPackId
    ) {
        List<EchoPackProfileIssue> issues = new ArrayList<>();
        require(rawProfile, "schema", EchoDiagnosticSeverity.ERROR, issues, source);
        require(rawProfile, "id", EchoDiagnosticSeverity.ERROR, issues, source);
        require(rawProfile, "type", EchoDiagnosticSeverity.ERROR, issues, source);
        require(rawProfile, "name", EchoDiagnosticSeverity.WARNING, issues, source);
        require(rawProfile, "publisher", EchoDiagnosticSeverity.WARNING, issues, source);
        require(rawProfile, "gameMode", EchoDiagnosticSeverity.WARNING, issues, source);
        require(rawProfile, "releaseChannel", EchoDiagnosticSeverity.WARNING, issues, source);
        require(rawProfile, "minecraftVersion", EchoDiagnosticSeverity.WARNING, issues, source);
        require(rawProfile, "theme", EchoDiagnosticSeverity.NOTICE, issues, source);
        requireLoaderField(rawProfile, "kind", issues, source);
        requireLoaderField(rawProfile, "version", issues, source);

        String id = string(rawProfile, "id");
        if (!id.isBlank() && !id.equals(requestedPackId.value())) {
            issues.add(issue("ECHO-PACK-PROFILE-ID-MISMATCH", EchoDiagnosticSeverity.ERROR,
                    "Pack profile id does not match requested pack: " + id + " != " + requestedPackId.value(),
                    source, "Rename the profile id or load it with the matching -PechoPack value."));
        }

        String status = string(normalizedProfile.get("status"));
        String type = string(normalizedProfile.get("type"));
        if ("active".equals(status) && "official_pack".equals(type) && string(normalizedProfile.get("rootModule")).isBlank()) {
            issues.add(issue("ECHO-PACK-PROFILE-ROOT-MODULE-MISSING", EchoDiagnosticSeverity.ERROR,
                    "Active official pack profile must declare rootModule.",
                    source, "Declare the active official pack rootModule."));
        }

        if (stringList(normalizedProfile.get("requiredModules")).isEmpty()) {
            issues.add(issue("ECHO-PACK-PROFILE-EMPTY-MODULES", EchoDiagnosticSeverity.WARNING,
                    "Pack profile has no required modules.",
                    source, "Declare requiredModules for Launcher and PackOS readiness checks."));
        }
        if (stringList(normalizedProfile.get("requiredFeatures")).isEmpty()) {
            issues.add(issue("ECHO-PACK-PROFILE-EMPTY-FEATURES", EchoDiagnosticSeverity.WARNING,
                    "Pack profile has no required features.",
                    source, "Declare requiredFeatures for Launcher and PackOS readiness checks."));
        }

        for (String variant : stringList(normalizedProfile.get("variants"))) {
            if (!VALID_VARIANTS.contains(variant)) {
                issues.add(issue("ECHO-PACK-PROFILE-INVALID-VARIANT", EchoDiagnosticSeverity.ERROR,
                        "Pack profile declares unsupported variant: " + variant,
                        source, "Use a variant registered by EchoPackConstants."));
            }
        }
        for (String channel : stringList(normalizedProfile.get("channels"))) {
            if (!VALID_CHANNELS.contains(channel)) {
                issues.add(issue("ECHO-PACK-PROFILE-INVALID-CHANNEL", EchoDiagnosticSeverity.ERROR,
                        "Pack profile declares unsupported channel: " + channel,
                        source, "Use a channel registered by EchoPackConstants."));
            }
        }
        return issues;
    }

    public EchoPackProfileStatus statusFor(List<EchoPackProfileIssue> issues) {
        if (issues.stream().anyMatch(EchoPackProfileIssue::blocking)) {
            return EchoPackProfileStatus.INVALID;
        }
        return issues.isEmpty() ? EchoPackProfileStatus.VALID : EchoPackProfileStatus.VALID_WITH_WARNINGS;
    }

    private static void require(JsonObject rawProfile, String field, EchoDiagnosticSeverity severity, List<EchoPackProfileIssue> issues, EchoPackProfileSource source) {
        if (!hasNonBlank(rawProfile, field)) {
            issues.add(issue("ECHO-PACK-PROFILE-" + field.toUpperCase(java.util.Locale.ROOT).replace('-', '_') + "-MISSING", severity,
                    "Pack profile missing field: " + field,
                    source, "Add " + field + " to the pack profile."));
        }
    }

    private static void requireLoaderField(JsonObject rawProfile, String field, List<EchoPackProfileIssue> issues, EchoPackProfileSource source) {
        JsonObject loader = object(rawProfile, "loader");
        if (loader == null || !hasNonBlank(loader, field)) {
            issues.add(issue("ECHO-PACK-PROFILE-LOADER-" + field.toUpperCase(java.util.Locale.ROOT) + "-MISSING", EchoDiagnosticSeverity.WARNING,
                    "Pack profile missing loader." + field + ".",
                    source, "Declare loader." + field + " in the pack profile."));
        }
    }

    private static boolean hasNonBlank(JsonObject object, String field) {
        return object != null && object.has(field) && !string(object, field).isBlank();
    }

    private static JsonObject object(JsonObject object, String field) {
        JsonElement element = object == null ? null : object.get(field);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static String string(JsonObject object, String field) {
        JsonElement element = object == null ? null : object.get(field);
        if (element == null || element.isJsonNull()) {
            return "";
        }
        return element.isJsonPrimitive() ? element.getAsString().trim() : "";
    }

    private static String string(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(Object::toString).map(String::trim).filter(item -> !item.isBlank()).toList();
        }
        if (value instanceof JsonArray array) {
            List<String> values = new ArrayList<>();
            for (JsonElement element : array) {
                if (element.isJsonPrimitive()) {
                    values.add(element.getAsString());
                }
            }
            return values;
        }
        return List.of();
    }

    private static EchoPackProfileIssue issue(String code, EchoDiagnosticSeverity severity, String summary, EchoPackProfileSource source, String suggestedFix) {
        return new EchoPackProfileIssue(
                code,
                severity,
                summary,
                source,
                List.of(source.reportPath()),
                suggestedFix,
                Map.of("source", source.reportPath())
        );
    }
}
