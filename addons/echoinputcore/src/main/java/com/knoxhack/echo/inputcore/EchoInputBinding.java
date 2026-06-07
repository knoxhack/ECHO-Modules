package com.knoxhack.echo.inputcore;

import com.knoxhack.echo.assetcore.EchoAssetReference;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoFeatureId;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoInputBinding(
        EchoInputBindingId id,
        EchoInputActionKind actionKind,
        EchoInputDeviceKind deviceKind,
        String defaultBinding,
        String localizationKey,
        EchoContentReference targetReference,
        List<EchoFeatureId> optionalFeatures,
        List<EchoAssetReference> promptAssets,
        Map<String, String> attributes
) {
    public EchoInputBinding {
        Objects.requireNonNull(id, "id");
        actionKind = actionKind == null ? EchoInputActionKind.UNKNOWN : actionKind;
        deviceKind = deviceKind == null ? EchoInputDeviceKind.UNKNOWN : deviceKind;
        defaultBinding = InputContractGuards.optionalText(defaultBinding);
        localizationKey = InputContractGuards.optionalText(localizationKey);
        optionalFeatures = InputContractGuards.immutableList(optionalFeatures);
        promptAssets = InputContractGuards.immutableList(promptAssets);
        attributes = InputContractGuards.immutableMap(attributes);
    }
}
