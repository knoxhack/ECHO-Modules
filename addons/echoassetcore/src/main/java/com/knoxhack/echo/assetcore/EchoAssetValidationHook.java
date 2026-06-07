package com.knoxhack.echo.assetcore;

@FunctionalInterface
public interface EchoAssetValidationHook {
    EchoAssetValidationResult validate(EchoAssetValidationRequest request);
}
