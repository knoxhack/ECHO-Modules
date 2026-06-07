package com.knoxhack.echoruntimeguard.api;

import java.util.List;

/**
 * Stable category ids exposed through Echo Core's runtime budget service.
 */
public final class RuntimeGuardBudgetCategories {
    public static final String SERVER_TICK = "server_tick";
    public static final String CLIENT_FRAME = "client_frame";
    public static final String PARTICLES = "particles";
    public static final String NETWORK = "network";
    public static final String MULTIBLOCK_VALIDATION = "multiblock_validation";
    public static final String LENS_SCAN = "lens_scan";
    public static final String HOLOMAP_REFRESH = "holomap_refresh";
    public static final String BLOCK_ENTITY = "block_entity";
    public static final String ENTITY_AI = "entity_ai";
    public static final String WORLDGEN = "worldgen";
    public static final String PROFILED_WORK = "profiled_work";

    public static final List<String> ALL = List.of(
            SERVER_TICK,
            CLIENT_FRAME,
            PARTICLES,
            NETWORK,
            MULTIBLOCK_VALIDATION,
            LENS_SCAN,
            HOLOMAP_REFRESH,
            BLOCK_ENTITY,
            ENTITY_AI,
            WORLDGEN,
            PROFILED_WORK);

    private RuntimeGuardBudgetCategories() {
    }
}
