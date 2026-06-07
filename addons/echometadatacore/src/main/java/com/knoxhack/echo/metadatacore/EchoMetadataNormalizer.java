package com.knoxhack.echo.metadatacore;

import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoModuleKind;
import com.knoxhack.echo.platformcore.EchoModuleRole;
import com.knoxhack.echo.platformcore.EchoModuleVersion;

import java.util.Map;

public final class EchoMetadataNormalizer {
    private EchoMetadataNormalizer() {
    }

    public static EchoModuleManifest minimalManifest(EchoModuleId moduleId, String displayName, String version) {
        return EchoModuleManifest.minimal(
                moduleId,
                displayName == null || displayName.isBlank() ? moduleId.value() : displayName,
                EchoModuleVersion.of(version == null || version.isBlank() ? "unknown" : version),
                EchoModuleKind.ADDON,
                EchoModuleRole.CONTENT_EXPANSION
        );
    }

    public static String stringField(Map<String, Object> payload, String key, String fallback) {
        Object value = payload.get(key);
        return value instanceof String text && !text.isBlank() ? text.trim() : fallback;
    }
}
