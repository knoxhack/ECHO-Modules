package com.knoxhack.echorelictech.data;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

public class RelicDataReloadListener extends SimplePreparableReloadListener<Void> {
    private final RelicDefinitionLoader relicLoader = new RelicDefinitionLoader();
    private final RelicFailureLoader failureLoader = new RelicFailureLoader();

    @Override
    protected Void prepare(ResourceManager manager, ProfilerFiller profiler) {
        return null;
    }

    @Override
    protected void apply(Void unused, ResourceManager manager, ProfilerFiller profiler) {
        // Handled by individual listeners registered separately
    }
}
