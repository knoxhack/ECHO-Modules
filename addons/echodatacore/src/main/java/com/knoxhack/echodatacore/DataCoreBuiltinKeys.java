package com.knoxhack.echodatacore;

import com.echoplatform.echocore.api.DataScope;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.IDataKey;
import net.minecraft.resources.Identifier;

public final class DataCoreBuiltinKeys {
    public static final IDataKey<String> TERMINAL_PROBE = IDataKey.string(
            id("system/terminal_probe"), DataScope.PLAYER, "offline", true);
    public static final IDataKey<Long> PLAYER_SCHEMA_VERSION = IDataKey.counter(
            id("system/player_schema_version"), DataScope.PLAYER, 0L, true);
    public static final IDataKey<Long> WORLD_SCHEMA_VERSION = IDataKey.counter(
            id("system/world_schema_version"), DataScope.WORLD, 0L, true);
    public static final IDataKey<String> WORLDCORE_LAST_REGION = IDataKey.string(
            id("worldcore/last_region"), DataScope.PLAYER, "", true);
    public static final IDataKey<String> WORLDCORE_LAST_DISCOVERY_SOURCE = IDataKey.string(
            id("worldcore/last_discovery_source"), DataScope.PLAYER, "", true);
    public static final IDataKey<Long> WORLDCORE_REGION_DISCOVERIES = IDataKey.counter(
            id("worldcore/region_discoveries"), DataScope.PLAYER, 0L, true);
    public static final IDataKey<String> WORLDCORE_LAST_MARKER = IDataKey.string(
            id("worldcore/last_marker"), DataScope.PLAYER, "", true);
    public static final IDataKey<Long> WORLDCORE_MARKERS_REVEALED = IDataKey.counter(
            id("worldcore/markers_revealed"), DataScope.PLAYER, 0L, true);
    public static final IDataKey<String> WORLDCORE_ACTIVE_HAZARDS = IDataKey.string(
            id("worldcore/active_hazards"), DataScope.PLAYER, "", true);
    public static final IDataKey<Long> WORLDCORE_ACTIVE_HAZARD_SEVERITY = IDataKey.counter(
            id("worldcore/active_hazard_severity"), DataScope.PLAYER, 0L, true);
    public static final IDataKey<Long> WORLDCORE_WORLD_REGION_DISCOVERIES = IDataKey.counter(
            id("worldcore/world_region_discoveries"), DataScope.WORLD, 0L, true);
    public static final IDataKey<Long> WORLDCORE_WORLD_MARKERS_REVEALED = IDataKey.counter(
            id("worldcore/world_markers_revealed"), DataScope.WORLD, 0L, true);
    public static final IDataKey<Long> WORLDCORE_WORLD_HAZARD_CHANGES = IDataKey.counter(
            id("worldcore/world_hazard_changes"), DataScope.WORLD, 0L, true);
    public static final IDataKey<String> RUNTIME_SPINE_LAST_EVENT = IDataKey.string(
            id("runtime_spine/last_event"), DataScope.PLAYER, "", true);
    public static final IDataKey<String> RUNTIME_SPINE_LAST_SOURCE = IDataKey.string(
            id("runtime_spine/last_source"), DataScope.PLAYER, "", true);
    public static final IDataKey<Long> RUNTIME_SPINE_EVENTS = IDataKey.counter(
            id("runtime_spine/events"), DataScope.PLAYER, 0L, true);
    public static final IDataKey<Long> RUNTIME_SPINE_WORLD_EVENTS = IDataKey.counter(
            id("runtime_spine/world_events"), DataScope.WORLD, 0L, true);

    private DataCoreBuiltinKeys() {
    }

    public static void register() {
        EchoCoreServices.registerDataKey(TERMINAL_PROBE);
        EchoCoreServices.registerDataKey(PLAYER_SCHEMA_VERSION);
        EchoCoreServices.registerDataKey(WORLD_SCHEMA_VERSION);
        EchoCoreServices.registerDataKey(WORLDCORE_LAST_REGION);
        EchoCoreServices.registerDataKey(WORLDCORE_LAST_DISCOVERY_SOURCE);
        EchoCoreServices.registerDataKey(WORLDCORE_REGION_DISCOVERIES);
        EchoCoreServices.registerDataKey(WORLDCORE_LAST_MARKER);
        EchoCoreServices.registerDataKey(WORLDCORE_MARKERS_REVEALED);
        EchoCoreServices.registerDataKey(WORLDCORE_ACTIVE_HAZARDS);
        EchoCoreServices.registerDataKey(WORLDCORE_ACTIVE_HAZARD_SEVERITY);
        EchoCoreServices.registerDataKey(WORLDCORE_WORLD_REGION_DISCOVERIES);
        EchoCoreServices.registerDataKey(WORLDCORE_WORLD_MARKERS_REVEALED);
        EchoCoreServices.registerDataKey(WORLDCORE_WORLD_HAZARD_CHANGES);
        EchoCoreServices.registerDataKey(RUNTIME_SPINE_LAST_EVENT);
        EchoCoreServices.registerDataKey(RUNTIME_SPINE_LAST_SOURCE);
        EchoCoreServices.registerDataKey(RUNTIME_SPINE_EVENTS);
        EchoCoreServices.registerDataKey(RUNTIME_SPINE_WORLD_EVENTS);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoDataCore.MODID, path);
    }
}
