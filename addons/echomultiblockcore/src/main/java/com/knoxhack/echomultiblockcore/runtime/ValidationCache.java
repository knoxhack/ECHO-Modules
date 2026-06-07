package com.knoxhack.echomultiblockcore.runtime;

import com.knoxhack.echomultiblockcore.api.MultiblockDefinition;
import com.knoxhack.echomultiblockcore.api.ValidationCacheKey;
import com.knoxhack.echomultiblockcore.api.ValidationOptions;
import com.knoxhack.echomultiblockcore.api.ValidationResult;
import com.knoxhack.echomultiblockcore.validation.MultiblockValidationEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class ValidationCache {
    private ValidationCacheKey key;
    private ValidationResult result;
    private boolean dirty = true;

    public void markDirty() {
        dirty = true;
    }

    public ValidationResult validate(ServerLevel level, BlockPos controllerPos, MultiblockDefinition definition,
            long structureVersion, ValidationOptions options) {
        options = options == null ? ValidationOptions.DEFAULT : options;
        ValidationCacheKey nextKey = new ValidationCacheKey(level.dimension(), controllerPos, definition.id(), structureVersion);
        long now = level.getGameTime();
        if (!options.force() && !dirty && nextKey.equals(key) && result != null
                && now - result.validationTime() <= options.cacheTtlTicks()) {
            return result;
        }
        result = MultiblockValidationEngine.validate(level, controllerPos, definition, options);
        key = nextKey;
        dirty = false;
        return result;
    }

    public ValidationResult lastResult() {
        return result;
    }

    public boolean isDirty() {
        return dirty;
    }
}
