package com.knoxhack.echolens.api;

public interface MachineLensProvider extends LensInfoProvider {
    @Override
    default LensDataCategory category() {
        return LensDataCategory.MACHINE;
    }
}
