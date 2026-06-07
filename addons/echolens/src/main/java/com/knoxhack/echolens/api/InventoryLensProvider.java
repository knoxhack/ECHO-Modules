package com.knoxhack.echolens.api;

public interface InventoryLensProvider extends LensInfoProvider {
    @Override
    default LensDataCategory category() {
        return LensDataCategory.INVENTORY;
    }
}
