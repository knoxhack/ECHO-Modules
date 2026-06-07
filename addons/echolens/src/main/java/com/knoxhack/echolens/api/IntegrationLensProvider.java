package com.knoxhack.echolens.api;

public interface IntegrationLensProvider extends LensInfoProvider {
    @Override
    default LensDataCategory category() {
        return LensDataCategory.INTEGRATION;
    }
}
