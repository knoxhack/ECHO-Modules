package com.knoxhack.echolens.api;

import java.util.List;
import net.minecraft.resources.Identifier;

public interface LensInfoProvider {
    Identifier id();

    default int priority() {
        return 1000;
    }

    default LensDataCategory category() {
        return LensDataCategory.IDENTITY;
    }

    default boolean supports(LensContext context) {
        return context != null;
    }

    List<LensInfoSection> inspect(LensContext context);
}
