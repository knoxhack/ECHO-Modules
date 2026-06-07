package com.knoxhack.echo.validationcore;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoPackId;
import com.knoxhack.echo.schemacore.EchoSchemaDescriptor;

import java.util.Map;
import java.util.Set;

public record EchoValidationTarget(
        String id,
        String name,
        EchoValidationScope scope,
        EchoValidationCategory category,
        EchoModuleId moduleId,
        EchoPackId packId,
        EchoSchemaDescriptor schemaDescriptor,
        String path,
        Set<EchoFeatureId> features,
        Map<String, String> attributes
) {
    public EchoValidationTarget {
        id = ValidationContractGuards.requireText(id, "validation target id");
        name = ValidationContractGuards.requireText(name, "validation target name");
        scope = scope == null ? EchoValidationScope.UNKNOWN : scope;
        category = category == null ? EchoValidationCategory.UNKNOWN : category;
        path = ValidationContractGuards.optionalText(path);
        features = ValidationContractGuards.immutableSet(features);
        attributes = ValidationContractGuards.immutableStringMap(attributes);
    }

    public static EchoValidationTarget module(EchoModuleId moduleId, String name) {
        return new EchoValidationTarget(
                moduleId.value(),
                name,
                EchoValidationScope.MODULE,
                EchoValidationCategory.MODULE_MANIFEST,
                moduleId,
                null,
                null,
                "",
                Set.of(),
                Map.of()
        );
    }
}
