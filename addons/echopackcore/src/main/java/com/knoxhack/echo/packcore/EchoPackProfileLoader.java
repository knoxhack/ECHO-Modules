package com.knoxhack.echo.packcore;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.knoxhack.echo.platformcore.EchoApiStability;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoFeatureRequirement;
import com.knoxhack.echo.platformcore.EchoGameModeId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoPackId;
import com.knoxhack.echo.platformcore.EchoRuntimeSide;
import com.knoxhack.echo.platformcore.EchoTrustLevel;
import com.knoxhack.echo.schemacore.EchoSchemaCompatibility;
import com.knoxhack.echo.schemacore.EchoSchemaConstants;
import com.knoxhack.echo.schemacore.EchoSchemaDescriptor;
import com.knoxhack.echo.schemacore.EchoSchemaDocumentKind;
import com.knoxhack.echo.schemacore.EchoSchemaId;
import com.knoxhack.echo.schemacore.EchoSchemaOwner;
import com.knoxhack.echo.schemacore.EchoSchemaVersion;
import com.knoxhack.echo.validationcore.EchoDiagnosticSeverity;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class EchoPackProfileLoader {
    private final EchoPackProfileRepository repository;
    private final EchoPackProfileValidator validator;

    public EchoPackProfileLoader(EchoPackProfileRepository repository, EchoPackProfileValidator validator) {
        this.repository = repository == null ? new EchoPackProfileRepository(null) : repository;
        this.validator = validator == null ? new EchoPackProfileValidator() : validator;
    }

    public EchoPackProfileParseResult load(EchoPackId packId) {
        EchoPackProfileSource source = repository.firstExistingSource(packId);
        if (source == null) {
            return missing(packId);
        }
        try {
            JsonElement element = JsonParser.parseString(Files.readString(source.path()));
            if (!element.isJsonObject()) {
                return invalidJson(packId, source, "Pack profile root must be a JSON object.");
            }
            JsonObject raw = element.getAsJsonObject();
            Map<String, Object> normalized = normalize(raw, packId);
            List<EchoPackProfileIssue> issues = validator.validate(raw, normalized, source, packId);
            EchoPackProfileStatus status = validator.statusFor(issues);
            EchoPackProfile profile = status.usable() ? toTypedProfile(normalized) : null;
            return new EchoPackProfileParseResult(packId, source, normalized, profile, status, issues);
        } catch (JsonParseException | IllegalStateException ex) {
            return invalidJson(packId, source, "Pack profile JSON is invalid: " + ex.getMessage());
        } catch (IOException ex) {
            return invalidJson(packId, source, "Pack profile could not be read: " + ex.getMessage());
        }
    }

    public EchoPackProfileIndex loadAll(List<EchoPackId> packIds) {
        return EchoPackProfileIndex.of(packIds.stream()
                .sorted(Comparator.comparing(EchoPackId::value))
                .map(this::load)
                .toList());
    }

    private EchoPackProfileParseResult missing(EchoPackId packId) {
        EchoPackProfileSource source = EchoPackProfileSource.builtInDefault();
        Map<String, Object> fallback = fallbackProfile(packId);
        EchoPackProfileIssue issue = new EchoPackProfileIssue(
                "ECHO-PACK-PROFILE-MISSING",
                EchoDiagnosticSeverity.WARNING,
                "No PackOS profile source was found; using deterministic built-in defaults.",
                source,
                repository.canonicalSourceLocations(packId),
                "Create a canonical PackOS profile before release.",
                Map.of("packId", packId.value())
        );
        return new EchoPackProfileParseResult(packId, source, fallback, toTypedProfile(fallback), EchoPackProfileStatus.FALLBACK, List.of(issue));
    }

    private EchoPackProfileParseResult invalidJson(EchoPackId packId, EchoPackProfileSource source, String summary) {
        EchoPackProfileIssue issue = new EchoPackProfileIssue(
                "ECHO-PACK-PROFILE-INVALID",
                EchoDiagnosticSeverity.ERROR,
                summary,
                source,
                List.of(source.reportPath()),
                "Fix the pack profile JSON, then run loadEchoPackProfile again.",
                Map.of("packId", packId.value())
        );
        return new EchoPackProfileParseResult(packId, source, fallbackProfile(packId), null, EchoPackProfileStatus.INVALID_JSON, List.of(issue));
    }

    private static Map<String, Object> normalize(JsonObject raw, EchoPackId packId) {
        Map<String, Object> profile = fallbackProfile(packId);
        putIfPresent(profile, raw, "schema");
        putIfPresent(profile, raw, "id");
        putIfPresent(profile, raw, "name");
        putIfPresent(profile, raw, "publisher");
        putIfPresent(profile, raw, "version");
        putIfPresent(profile, raw, "status");
        putIfPresent(profile, raw, "type");
        putIfPresent(profile, raw, "rootModule");
        putIfPresent(profile, raw, "gameMode");
        putIfPresent(profile, raw, "worldProfile");
        putIfPresent(profile, raw, "startProfile");
        putIfPresent(profile, raw, "theme");
        putIfPresent(profile, raw, "releaseChannel");
        putIfPresent(profile, raw, "minecraftVersion");
        putIfPresent(profile, raw, "strictOfficialOnly");
        putStringListIfPresent(profile, raw, "variants");
        putStringListIfPresent(profile, raw, "channels");
        putStringListIfPresent(profile, raw, "requiredModules");
        putStringListIfPresent(profile, raw, "recommendedModules");
        putStringListIfPresent(profile, raw, "optionalModules");
        putStringListIfPresent(profile, raw, "requiredFeatures");
        putStringListIfPresent(profile, raw, "optionalFeatures");
        JsonObject modules = object(raw, "modules");
        if (modules != null) {
            putStringListIfPresent(profile, modules, "required", "requiredModules");
            putStringListIfPresent(profile, modules, "recommended", "recommendedModules");
            putStringListIfPresent(profile, modules, "optional", "optionalModules");
        }
        JsonObject loader = object(raw, "loader");
        Map<String, Object> loaderMap = new LinkedHashMap<>();
        loaderMap.put("kind", loader == null ? "Echo Native" : string(loader, "kind", string(loader, "name", "Echo Native")));
        loaderMap.put("version", loader == null ? "" : string(loader, "version", ""));
        profile.put("loader", loaderMap);
        return profile;
    }

    private static Map<String, Object> fallbackProfile(EchoPackId packId) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("schema", "echo.pack.profile.v1");
        profile.put("id", packId.value());
        profile.put("name", packId.value());
        profile.put("publisher", "KnoxHack");
        profile.put("version", "0.0.0");
        profile.put("status", "planned");
        profile.put("type", "official_pack");
        profile.put("rootModule", packId.value());
        profile.put("gameMode", "custom");
        profile.put("worldProfile", "");
        profile.put("startProfile", "");
        profile.put("theme", "");
        profile.put("releaseChannel", "beta");
        profile.put("strictOfficialOnly", true);
        profile.put("minecraftVersion", "");
        profile.put("loader", Map.of("kind", "Echo Native", "version", ""));
        profile.put("variants", EchoPackConstants.BUILTIN_VARIANTS.stream().map(variant -> variant.id().value()).toList());
        profile.put("channels", EchoPackConstants.BUILTIN_CHANNELS.stream().map(channel -> channel.id().value()).toList());
        profile.put("requiredModules", List.of());
        profile.put("recommendedModules", List.of());
        profile.put("optionalModules", List.of());
        profile.put("requiredFeatures", List.of());
        profile.put("optionalFeatures", List.of());
        return profile;
    }

    @SuppressWarnings("unchecked")
    private static EchoPackProfile toTypedProfile(Map<String, Object> profile) {
        Map<String, Object> loader = profile.get("loader") instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
        return new EchoPackProfile(
                schemaDescriptor(string(profile.get("schema"), "echo.pack.profile.v1")),
                EchoPackId.of(string(profile.get("id"), "custom")),
                string(profile.get("name"), "Custom Pack"),
                string(profile.get("publisher"), ""),
                packType(string(profile.get("type"), "custom_pack")),
                EchoGameModeId.of(string(profile.get("gameMode"), "custom")),
                EchoModuleId.of(string(profile.get("rootModule"), string(profile.get("id"), "custom"))),
                string(profile.get("minecraftVersion"), "unknown"),
                string(loader.get("version"), "unknown"),
                string(profile.get("worldProfile"), ""),
                string(profile.get("startProfile"), ""),
                string(profile.get("theme"), ""),
                EchoPackChannelId.of(string(profile.get("releaseChannel"), "stable")),
                Boolean.TRUE.equals(profile.get("strictOfficialOnly")),
                moduleRequirements(strings(profile.get("requiredModules")), true),
                moduleRequirements(strings(profile.get("optionalModules")), false),
                featureRequirements(strings(profile.get("requiredFeatures")), true),
                featureRequirements(strings(profile.get("optionalFeatures")), false),
                Set.of(EchoTrustLevel.OFFICIAL, EchoTrustLevel.VERIFIED),
                variants(strings(profile.get("variants"))),
                channels(strings(profile.get("channels"))),
                null,
                List.of(),
                List.of(),
                null,
                null,
                null
        );
    }

    private static EchoSchemaDescriptor schemaDescriptor(String schemaId) {
        return EchoSchemaConstants.BUILTIN_DESCRIPTORS.stream()
                .filter(descriptor -> descriptor.kind() == EchoSchemaDocumentKind.ECHO_PACK_PROFILE)
                .findFirst()
                .orElseGet(() -> new EchoSchemaDescriptor(
                        EchoSchemaId.of(schemaId),
                        EchoSchemaVersion.of("1.0.0"),
                        EchoSchemaDocumentKind.ECHO_PACK_PROFILE,
                        new EchoSchemaOwner(EchoModuleId.of(EchoPackConstants.MOD_ID), "ECHO Platform", "KnoxHack", Set.of(EchoRuntimeSide.COMMON)),
                        "ECHO Pack Profile",
                        "Pack profile contract for Launcher, Command Center, PackOS, and AI tools.",
                        EchoApiStability.BETA,
                        EchoSchemaCompatibility.CURRENT,
                        "",
                        "docs/echo/packos/ECHO_PACK_PROFILES.md",
                        Set.of(EchoPackConstants.FEATURE_PACK_PROFILE),
                        Set.of(EchoRuntimeSide.COMMON, EchoRuntimeSide.LAUNCHER, EchoRuntimeSide.COMMAND_CENTER, EchoRuntimeSide.AI_AGENT),
                        Set.of()
                ));
    }

    private static EchoPackType packType(String raw) {
        String normalized = raw.toLowerCase(Locale.ROOT);
        for (EchoPackType type : EchoPackType.values()) {
            if (type.serializedName().equals(normalized) || type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        return EchoPackType.CUSTOM_PACK;
    }

    private static List<EchoPackModuleRequirement> moduleRequirements(List<String> moduleIds, boolean required) {
        return moduleIds.stream()
                .map(moduleId -> new EchoPackModuleRequirement(EchoModuleId.of(moduleId), required, "", required ? "pack_required" : "pack_optional", null))
                .toList();
    }

    private static List<EchoPackFeatureRequirement> featureRequirements(List<String> featureIds, boolean required) {
        return featureIds.stream()
                .map(featureId -> new EchoPackFeatureRequirement(
                        new EchoFeatureRequirement(EchoFeatureId.of(featureId), required, "", required ? "pack_required" : "pack_optional"),
                        null,
                        null,
                        required ? "pack_required" : "pack_optional"
                ))
                .toList();
    }

    private static List<EchoPackVariant> variants(List<String> variantIds) {
        return variantIds.stream()
                .map(EchoPackProfileLoader::variant)
                .toList();
    }

    private static EchoPackVariant variant(String variantId) {
        return EchoPackConstants.BUILTIN_VARIANTS.stream()
                .filter(variant -> variant.id().value().equals(variantId))
                .findFirst()
                .orElse(EchoPackConstants.VARIANT_STANDARD);
    }

    private static List<EchoPackChannel> channels(List<String> channelIds) {
        return channelIds.stream()
                .map(EchoPackProfileLoader::channel)
                .toList();
    }

    private static EchoPackChannel channel(String channelId) {
        return EchoPackConstants.BUILTIN_CHANNELS.stream()
                .filter(channel -> channel.id().value().equals(channelId))
                .findFirst()
                .orElse(EchoPackConstants.CHANNEL_STABLE);
    }

    private static void putIfPresent(Map<String, Object> target, JsonObject raw, String field) {
        JsonElement element = raw.get(field);
        if (element != null && !element.isJsonNull()) {
            target.put(field, element.isJsonPrimitive() ? primitive(element) : jsonValue(element));
        }
    }

    private static void putStringListIfPresent(Map<String, Object> target, JsonObject raw, String field) {
        putStringListIfPresent(target, raw, field, field);
    }

    private static void putStringListIfPresent(Map<String, Object> target, JsonObject raw, String field, String targetField) {
        JsonElement element = raw.get(field);
        if (element != null && element.isJsonArray()) {
            target.put(targetField, strings(element));
        }
    }

    private static Object jsonValue(JsonElement element) {
        if (element.isJsonObject()) {
            Map<String, Object> values = new LinkedHashMap<>();
            element.getAsJsonObject().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> values.put(entry.getKey(), jsonValue(entry.getValue())));
            return values;
        }
        if (element.isJsonArray()) {
            List<Object> values = new ArrayList<>();
            for (JsonElement child : element.getAsJsonArray()) {
                values.add(jsonValue(child));
            }
            return values;
        }
        return primitive(element);
    }

    private static Object primitive(JsonElement element) {
        if (element.getAsJsonPrimitive().isBoolean()) {
            return element.getAsBoolean();
        }
        if (element.getAsJsonPrimitive().isNumber()) {
            return element.getAsNumber();
        }
        return element.getAsString();
    }

    private static JsonObject object(JsonObject raw, String field) {
        JsonElement element = raw.get(field);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static String string(JsonObject raw, String field, String fallback) {
        JsonElement element = raw.get(field);
        return element != null && element.isJsonPrimitive() ? element.getAsString().trim() : fallback;
    }

    private static List<String> strings(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(Object::toString).map(String::trim).filter(item -> !item.isBlank()).toList();
        }
        return List.of();
    }

    private static List<String> strings(JsonElement element) {
        if (!element.isJsonArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        JsonArray array = element.getAsJsonArray();
        for (JsonElement child : array) {
            if (child.isJsonPrimitive()) {
                String value = child.getAsString().trim();
                if (!value.isBlank()) {
                    values.add(value);
                }
            }
        }
        return values;
    }

    private static String string(Object value, String fallback) {
        String text = value == null ? "" : value.toString().trim();
        return text.isBlank() ? fallback : text;
    }
}
