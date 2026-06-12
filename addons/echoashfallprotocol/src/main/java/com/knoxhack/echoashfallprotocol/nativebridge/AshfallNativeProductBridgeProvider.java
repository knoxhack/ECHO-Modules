package com.knoxhack.echoashfallprotocol.nativebridge;

import com.knoxhack.echoashfallprotocol.registry.ModCreativeTabs;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import dev.echo.nativeplatform.loader.EchoNativeRegistryHost;
import dev.echo.nativeplatform.loader.NativeLoaderLiveClientBridge;
import dev.echo.nativeplatform.loader.NativeLoaderLiveRegistryBridge;
import dev.echo.nativeplatform.loader.NativeLoaderLiveRuntimeAttachment;
import dev.echo.nativeplatform.loader.NativeLoaderLiveRuntimeBridge;
import dev.echo.nativeplatform.loader.NativeLoaderProductBridgeContext;
import dev.echo.nativeplatform.loader.NativeLoaderProductBridgeProvider;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Product bridge provider for the first-party Ashfall Native Loader profile.
 *
 * <p>This provider belongs to the Ashfall addon instead of the generic loader.
 * It registers the Ashfall runtime, registry, and client route contracts as
 * Native Loader handoff surfaces. Runtime state is owned by the first-class
 * Native Loader runtime host, while registry, client, and world-startup
 * evidence is projected through trusted first-party native bridges.</p>
 */
public final class AshfallNativeProductBridgeProvider implements NativeLoaderProductBridgeProvider {
    private static final String ATTACHMENT_ID = "echoashfallprotocol:ashfall_native_product_attachment";
    private static final String REGISTRY_BRIDGE_ID = "echoashfallprotocol:ashfall_product_registry_bridge";
    private static final String CLIENT_BRIDGE_ID = "echoashfallprotocol:ashfall_product_client_surface_bridge";
    private static final List<String> RUNTIME_SURFACES = List.of(
            "inventory",
            "player_state",
            "world_blocks",
            "world_state",
            "structures",
            "block_entities",
            "capabilities",
            "events",
            "packets_hud",
            "hud",
            "save_data",
            "missions",
            "feedback",
            "client_tick",
            "render_layers",
            "screen_events",
            "keybinds",
            "commands",
            "network_channels",
            "config_reloads",
            "resource_reloads",
            "save_hooks",
            "lifecycle_phases",
            "server_client_sync",
            "ashfall_world_startup",
            "ashfall_survival_state"
    );
    private static final List<String> ASHFALL_CLIENT_SURFACES = List.of(
            "main_menu",
            "loading_screen",
            "hud",
            "client_overlay",
            "terminal",
            "index",
            "lens",
            "holomap"
    );
    private static final String MOD_CREATIVE_TABS_CLASS =
            "com.knoxhack.echoashfallprotocol.registry.ModCreativeTabs";
    private static final List<String> ASHFALL_CREATIVE_TABS = List.of(
            "echoashfallprotocol:ashes_tab",
            "echoashfallprotocol:native_modules_tab"
    );
    private static final List<String> ASHFALL_NATIVE_MODULE_CREATIVE_FEATURED_ITEMS = List.of(
            "echoashfallprotocol:field_manual",
            "echoashfallprotocol:portable_signal_scanner",
            "echoashfallprotocol:gas_mask",
            "echoashfallprotocol:filter_cartridge_basic",
            "echoashfallprotocol:basic_battery",
            "echoashfallprotocol:energy_cell",
            "echoashfallprotocol:hand_recycler",
            "echoashfallprotocol:water_purifier",
            "echoashfallprotocol:micro_generator",
            "echoashfallprotocol:signal_scanner",
            "echoashfallprotocol:scrap_press",
            "echoashfallprotocol:factory_controller",
            "echoashfallprotocol:relay_scanner_lens",
            "echoashfallprotocol:survey_table",
            "echoashfallprotocol:nexus_crystal"
    );
    private static final List<String> ASHFALL_NATIVE_MODULE_CREATIVE_FALLBACK_ITEM_IDS = List.of(
            "echoashfallprotocol:field_manual",
            "echoashfallprotocol:portable_signal_scanner",
            "echoashfallprotocol:gas_mask",
            "echoashfallprotocol:filter_cartridge_basic",
            "echoashfallprotocol:basic_battery",
            "echoashfallprotocol:energy_cell",
            "echoashfallprotocol:hand_recycler",
            "echoashfallprotocol:water_purifier",
            "echoashfallprotocol:micro_generator",
            "echoashfallprotocol:signal_scanner",
            "echoashfallprotocol:scrap_press",
            "echoashfallprotocol:factory_controller",
            "echoashfallprotocol:relay_scanner_lens",
            "echoashfallprotocol:survey_table",
            "echoashfallprotocol:nexus_crystal",
            "echoterminal:echo_terminal",
            "echoterminal:echo_terminal_remote"
    );
    private static final List<String> ASHFALL_WORLD_STARTUP_RESOURCES = List.of(
            "echoashfallprotocol:ashfall_worldgen_datapack",
            "echoashfallprotocol:ashfall_client_resources",
            "echoashfallprotocol:ashfall_wasteland",
            "minecraft:normal",
            "echoashfallprotocol:wasteland_overworld_noise_settings",
            "echoashfallprotocol:wasteland_biomes",
            "echoashfallprotocol:wasteland_structures",
            "echoashfallprotocol:wasteland_worldgen_tags"
    );
    private static final List<String> ASHFALL_NATIVE_HOST_LANES = List.of(
            "lifecycle",
            "events",
            "commands",
            "config",
            "networking",
            "resources",
            "client_ui",
            "adaptercore_mutations",
            "save_data"
    );

    @Override
    public NativeLoaderLiveRuntimeAttachment liveRuntimeAttachment(NativeLoaderProductBridgeContext context) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("providerClass", getClass().getName());
        evidence.put("packId", context.packId());
        evidence.put("moduleId", context.moduleId());
        evidence.put("productRoot", context.productRoot().toString());
        evidence.put("moduleRoot", context.moduleRoot().toString());
        evidence.put("productProfile", "echoashfallprotocol:ashfall_native_product");
        evidence.put("firstClassNativeRuntime", true);
        evidence.put("ashfallProductBridgeProvider", true);
        evidence.put("realMinecraftProcess", false);
        evidence.put("nativeRuntimeProcess", true);
        evidence.put("releaseRuntimeTrusted", true);
        evidence.put("liveRuntimeMutationSupported", false);
        evidence.put("nativeRuntimeMutationSupported", true);
        evidence.put("nativeStateAuthoritative", true);
        evidence.put("nativeStateMirrorRequired", false);
        evidence.put("nativeProductWorldStartupReady", true);
        evidence.put("nativeProductWorldPreset", "echoashfallprotocol:ashfall_wasteland");
        evidence.put("nativeProductDatapack", "echo-native-ashfall-datapack.zip");
        evidence.put("nativeProductResourcePack", "echoashfallprotocol:ashfall_client_resources");
        evidence.put("nativeRequiredWorldResources", ASHFALL_WORLD_STARTUP_RESOURCES);
        evidence.put("nativeProductWorldStartupOwnerClass", "dev.echo.nativeplatform.loader.NativeLoaderAshfallWorldStartupService");
        evidence.put("nativeExpectedHostLanes", ASHFALL_NATIVE_HOST_LANES);
        evidence.put("summary", "Ashfall Native product bridge trusts the first-class Native Loader runtime host for native mutation dispatch; no fake live Minecraft runtime bridge is attached for headless product launch evidence.");
        return new NativeLoaderLiveRuntimeAttachment(
                ATTACHMENT_ID,
                "echo_native_first_class_runtime",
                "ashfall_native_product_runtime",
                false,
                false,
                RUNTIME_SURFACES,
                Map.copyOf(evidence)
        );
    }

    @Override
    public NativeLoaderLiveRuntimeBridge liveRuntimeBridge(NativeLoaderProductBridgeContext context) {
        return NativeLoaderLiveRuntimeBridge.UNATTACHED;
    }

    @Override
    public NativeLoaderLiveRegistryBridge liveRegistryBridge(NativeLoaderProductBridgeContext context) {
        return new AshfallProductRegistryBridge(context);
    }

    @Override
    public Map<String, Object> clientAttachmentAssessment(NativeLoaderProductBridgeContext context) {
        boolean windowedNativeClient = windowedNativeClientActive();
        Map<String, Object> assessment = new LinkedHashMap<>();
        assessment.put("providerClass", getClass().getName());
        assessment.put("packId", context.packId());
        assessment.put("moduleId", context.moduleId());
        assessment.put("productProfile", "echoashfallprotocol:ashfall_native_product");
        assessment.put("ashfallProductBridgeProvider", true);
        assessment.put("firstClassNativeClientSurface", true);
        assessment.put("firstClassNativeClientRouteTable", true);
        assessment.put("clientRouteRegistrationSupported", true);
        assessment.put("clientRouteMutationSupported", true);
        assessment.put("nativeClientRouteProcess", true);
        assessment.put("releaseClientRouteTrusted", true);
        assessment.put("firstClassNativeClientRenderPipeline", true);
        assessment.put("nativeClientRenderProcess", windowedNativeClient);
        assessment.put("releaseClientRenderTrusted", windowedNativeClient);
        assessment.put("clientRenderMutationSupported", windowedNativeClient);
        assessment.put("nativeHudRenderBridgeClass", "dev.echo.nativeplatform.bootstrap.EchoNativeLiveHudRenderBridge");
        assessment.put("nativeLoadingRenderBridgeClass", "dev.echo.nativeplatform.bootstrap.EchoNativeLiveLoadingRenderBridge");
        assessment.put("nativeRenderBridgeClasses", List.of(
                "dev.echo.nativeplatform.bootstrap.EchoNativeLiveHudRenderBridge",
                "dev.echo.nativeplatform.bootstrap.EchoNativeLiveLoadingRenderBridge"
        ));
        assessment.put("nativeSurfaceImplementationClasses", surfaceImplementationClasses());
        assessment.put("nativeKeybindingOwnerClass", "com.knoxhack.echoashfallprotocol.EchoAshfallProtocolClient");
        assessment.put("nativeCreativeTabs", ASHFALL_CREATIVE_TABS);
        assessment.put("nativeCreativeTabOwnerClass", "com.knoxhack.echoashfallprotocol.registry.ModCreativeTabs");
        assessment.put("nativeCreativeTabPopulationStrategy", "featured_ashfall_items_then_all_registered_echo_module_items_and_block_items");
        assessment.put("nativeProductWorldStartupReady", true);
        assessment.put("nativeProductWorldPreset", "echoashfallprotocol:ashfall_wasteland");
        assessment.put("nativeProductDatapack", "echo-native-ashfall-datapack.zip");
        assessment.put("nativeProductResourcePack", "echoashfallprotocol:ashfall_client_resources");
        assessment.put("nativeRequiredWorldResources", ASHFALL_WORLD_STARTUP_RESOURCES);
        assessment.put("nativeProductWorldStartupOwnerClass", "dev.echo.nativeplatform.loader.NativeLoaderAshfallWorldStartupService");
        assessment.put("nativeExpectedHostLanes", ASHFALL_NATIVE_HOST_LANES);
        assessment.put("expectedClientSurfaces", ASHFALL_CLIENT_SURFACES);
        assessment.put("liveClientAttached", windowedNativeClient);
        assessment.put("headlessClientSurface", false);
        assessment.put("realClientProcess", windowedNativeClient);
        assessment.put("releaseClientTrusted", windowedNativeClient);
        assessment.put("realClientRenderPipeline", windowedNativeClient);
        assessment.put("summary", windowedNativeClient
                ? "Ashfall Native client routes are committed into the first-class native route table and the first-party native HUD/loading render bridges are trusted release surfaces for the windowed Native Loader client."
                : "Ashfall Native client routes are committed into the first-class native route table; visible render mutation remains gated to the windowed Native Loader client.");
        return Map.copyOf(assessment);
    }

    @Override
    public NativeLoaderLiveClientBridge liveClientBridge(NativeLoaderProductBridgeContext context) {
        return new AshfallProductClientSurfaceBridge(context);
    }

    @Override
    public Map<String, Object> productHookPlan(NativeLoaderProductBridgeContext context) {
        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("registryHooks", registryHooks(context));
        plan.put("lifecycleHooks", lifecycleHooks(context));
        plan.put("eventSubscriptions", eventSubscriptions(context));
        plan.put("eventsToPublish", eventsToPublish(context));
        plan.put("commandHooks", commandHooks(context));
        plan.put("networkHooks", networkHooks(context));
        plan.put("resourceHooks", resourceHooks(context));
        plan.put("configHooks", configHooks(context));
        plan.put("clientSurfaceHooks", clientSurfaceHooks(context));
        plan.put("runtimeHooks", runtimeHooks(context));
        plan.put("productWorldHooks", productWorldHooks(context));
        plan.put("productOnboardingHooks", productOnboardingHooks(context));
        plan.put("saveDataHooks", saveDataHooks(context));
        return Map.copyOf(plan);
    }

    private static List<Map<String, Object>> registryHooks(NativeLoaderProductBridgeContext context) {
        return List.of(
                registryHook(context, "creative_tab", "echoashfallprotocol:ashes_tab",
                        "Ashfall Protocol creative tab registered through the native product hook plan."),
                nativeModulesCreativeTabRegistryHook(context)
        );
    }

    private static Map<String, Object> nativeModulesCreativeTabRegistryHook(NativeLoaderProductBridgeContext context) {
        NativeCreativeTabSource source = nativeModulesCreativeTabSource();
        Map<String, Object> properties = new LinkedHashMap<>(hookEvidence(
                context,
                "Native Loader module content creative tab registered through the native product hook plan with registry-backed population metadata."
        ));
        properties.put("titleKey", "itemGroup.EchoAshfallNativeModules");
        properties.put("iconItem", "echoashfallprotocol:portable_signal_scanner");
        properties.put("orderAnchor", "minecraft:building_blocks");
        properties.put("orderStrategy", "with_tabs_before_anchor");
        properties.put("searchVisibility", "parent_and_search_tabs");
        properties.put("searchVisible", true);
        properties.put("itemIds", source.itemIds());
        properties.put("registryBackedItemIds", source.registryBackedItemIds());
        properties.put("featuredItemIds", source.featuredItemIds());
        properties.put("sourceNamespaces", source.sourceNamespaces());
        properties.put("surfaceIds", ASHFALL_CLIENT_SURFACES);
        properties.put("nativeCreativeTabItemsDeclared", true);
        properties.put("nativeCreativeTabRegistryBacked", source.sourceResolved());
        properties.put("nativeCreativeTabSourceBacked", source.sourceResolved());
        properties.put("nativeCreativeTabSourceResolvedFromRuntime", source.sourceResolved());
        properties.put("nativeCreativeTabFallbackPopulationUsed", !source.sourceResolved());
        properties.put("nativeCreativeTabFallbackOnlyEvidence", !source.sourceResolved());
        properties.put("releaseCreativeTabSourceTrusted", source.sourceResolved());
        properties.put("nativeCreativeTabPopulationMode", source.sourceResolved()
                ? "registered_native_module_item_ids"
                : "fallback_native_module_item_ids_pre_minecraft");
        properties.put("nativeCreativeTabPopulationOwnerClass", MOD_CREATIVE_TABS_CLASS);
        properties.put("nativeCreativeTabPopulationOwnerMember", "nativeLoaderRegistryBackedCreativeItemIds");
        properties.put("nativeCreativeTabFullPopulationOwnerMember", "nativeModuleCreativeItemIds");
        properties.put("nativeCreativeTabFeaturedOwnerMember", "nativeModuleCreativeFeaturedItemIds");
        properties.put("nativeCreativeTabNamespaceOwnerMember", "nativeModuleCreativeNamespaces");
        properties.put("nativeCreativeTabPopulationStrategy",
                "featured_ashfall_items_then_all_registered_echo_module_items_and_block_items");
        return map(
                "moduleId", context.moduleId(),
                "registry", "creative_tab",
                "id", "echoashfallprotocol:native_modules_tab",
                "properties", Map.copyOf(properties)
        );
    }

    private static NativeCreativeTabSource nativeModulesCreativeTabSource() {
        List<String> itemIds = nativeCreativeTabFullPopulation();
        List<String> registryBackedItemIds = nativeCreativeTabRegistryBackedItems();
        List<String> featuredItemIds = nativeCreativeTabFeaturedItems();
        List<String> sourceNamespaces = nativeCreativeTabSourceNamespaces();
        boolean sourceResolved = !itemIds.isEmpty()
                && !registryBackedItemIds.isEmpty()
                && !featuredItemIds.isEmpty();
        if (sourceResolved) {
            return new NativeCreativeTabSource(itemIds, registryBackedItemIds, featuredItemIds, sourceNamespaces, true);
        }
        List<String> fallbackFeatured = ASHFALL_NATIVE_MODULE_CREATIVE_FEATURED_ITEMS;
        List<String> fallbackRegistryBacked = ASHFALL_NATIVE_MODULE_CREATIVE_FALLBACK_ITEM_IDS;
        List<String> fallbackItems = mergedCreativeTabItems(fallbackFeatured, fallbackRegistryBacked);
        return new NativeCreativeTabSource(
                fallbackItems,
                fallbackRegistryBacked,
                fallbackFeatured,
                namespaces(fallbackItems),
                false
        );
    }

    private static List<String> nativeCreativeTabFullPopulation() {
        try {
            return nativeCreativeTabList(ModCreativeTabs.nativeModuleCreativeItemIds());
        } catch (LinkageError ignored) {
            // The product launcher can ask for hook plans before Minecraft/native runtime classes are available.
            return List.of();
        }
    }

    private static List<String> nativeCreativeTabRegistryBackedItems() {
        try {
            return nativeCreativeTabList(ModCreativeTabs.nativeLoaderRegistryBackedCreativeItemIds());
        } catch (LinkageError ignored) {
            return List.of();
        }
    }

    private static List<String> nativeCreativeTabFeaturedItems() {
        try {
            return nativeCreativeTabList(ModCreativeTabs.nativeModuleCreativeFeaturedItemIds());
        } catch (LinkageError ignored) {
            return List.of();
        }
    }

    private static List<String> nativeCreativeTabSourceNamespaces() {
        try {
            return nativeCreativeTabList(ModCreativeTabs.nativeModuleCreativeNamespaces());
        } catch (LinkageError ignored) {
            return List.of();
        }
    }

    private static List<String> nativeCreativeTabList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(String::valueOf)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private static List<String> mergedCreativeTabItems(List<String> featuredItemIds, List<String> registryBackedItemIds) {
        LinkedHashSet<String> itemIds = new LinkedHashSet<>();
        addCreativeTabItems(itemIds, featuredItemIds);
        addCreativeTabItems(itemIds, registryBackedItemIds);
        return List.copyOf(itemIds);
    }

    private static void addCreativeTabItems(LinkedHashSet<String> itemIds, List<String> values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                itemIds.add(value);
            }
        }
    }

    private static List<String> namespaces(List<String> itemIds) {
        List<String> namespaces = new ArrayList<>();
        for (String itemId : itemIds == null ? List.<String>of() : itemIds) {
            int separator = itemId.indexOf(':');
            if (separator > 0) {
                String namespace = itemId.substring(0, separator);
                if (!namespace.isBlank() && !namespaces.contains(namespace)) {
                    namespaces.add(namespace);
                }
            }
        }
        namespaces.sort(String::compareTo);
        return List.copyOf(namespaces);
    }

    private static List<Map<String, Object>> lifecycleHooks(NativeLoaderProductBridgeContext context) {
        return List.of(
                lifecycleHook(context, "ashfall_product_bridge_attached",
                        "Ashfall product bridge attached runtime, registry, client, and hook-plan contracts."),
                lifecycleHook(context, "ashfall_product_world_startup_resources_mounted",
                        "Ashfall product world startup resources are mounted before product launch."),
                lifecycleHook(context, "ashfall_product_client_surfaces_routed",
                        "Ashfall native client surfaces are routed through the product client UI host.")
        );
    }

    private static List<Map<String, Object>> eventSubscriptions(NativeLoaderProductBridgeContext context) {
        return List.of(
                eventSubscription(context,
                        "echoashfallprotocol:native_product_ready",
                        "ashfall.nativeProductReady",
                        "Consumes the Ashfall native product-ready event inside the product event host.")
        );
    }

    private static List<Map<String, Object>> eventsToPublish(NativeLoaderProductBridgeContext context) {
        return List.of(
                map(
                        "sourceModule", context.moduleId(),
                        "eventId", "echoashfallprotocol:native_product_ready",
                        "payload", map(
                                "packId", context.packId(),
                                "moduleId", context.moduleId(),
                                "nativeProductWorldPreset", "echoashfallprotocol:ashfall_wasteland",
                                "nativeHookPlanPublished", true
                        )
                )
        );
    }

    private static List<Map<String, Object>> commandHooks(NativeLoaderProductBridgeContext context) {
        return List.of(
                commandHook(context, "echoashfallprotocol:open_terminal", "terminal",
                        "client_surface.terminal", "Routes the product terminal command into the native command host."),
                commandHook(context, "echoashfallprotocol:open_index", "index",
                        "client_surface.index", "Routes the product index command into the native command host."),
                commandHook(context, "echoashfallprotocol:open_holomap", "holomap",
                        "client_surface.holomap", "Routes the product HoloMap command into the native command host.")
        );
    }

    private static List<Map<String, Object>> networkHooks(NativeLoaderProductBridgeContext context) {
        return List.of(
                networkHook(context,
                        "echoashfallprotocol:ashfall_runtime_sync",
                        "adaptercore.native_runtime_packet",
                        "ashfall_native_runtime",
                        List.of("terminal", "hud", "save_data"),
                        "Binds Ashfall runtime state sync into the native network host."),
                networkHook(context,
                        "echoashfallprotocol:ashfall_hud_state",
                        "packets_hud",
                        "ashfall_native_hud",
                        List.of("hud", "client_overlay"),
                        "Binds Ashfall HUD state packets into the native network host."),
                networkHook(context,
                        "echoashfallprotocol:ashfall_route_surface",
                        "client_surface",
                        "ashfall_native_client_route",
                        List.of("terminal", "index", "lens", "holomap"),
                        "Binds Ashfall route surface packets into the native network host.")
        );
    }

    private static List<Map<String, Object>> resourceHooks(NativeLoaderProductBridgeContext context) {
        return List.of(
                resourceHook(context, "echoashfallprotocol:ashfall_worldgen_datapack", "data_pack",
                        "Mounts the Ashfall product datapack in the native resource host."),
                resourceHook(context, "echoashfallprotocol:ashfall_client_resources", "resource_pack",
                        "Mounts the Ashfall client resources in the native resource host."),
                resourceHook(context, "minecraft:normal", "world_preset",
                        "Mounts the Ashfall override for the normal world preset."),
                resourceHook(context, "echoashfallprotocol:ashfall_wasteland", "world_preset",
                        "Mounts the Ashfall wasteland product world preset."),
                resourceHook(context, "echoashfallprotocol:wasteland_overworld_noise_settings", "worldgen",
                        "Mounts Ashfall wasteland overworld noise settings."),
                resourceHook(context, "echoashfallprotocol:wasteland_biomes", "worldgen",
                        "Mounts Ashfall wasteland biome definitions."),
                resourceHook(context, "echoashfallprotocol:wasteland_structures", "structure",
                        "Mounts Ashfall route, ruin, vault, and POI structure resources."),
                resourceHook(context, "echoashfallprotocol:wasteland_worldgen_tags", "tag",
                        "Mounts Ashfall biome and structure tags for native world startup.")
        );
    }

    private static List<Map<String, Object>> configHooks(NativeLoaderProductBridgeContext context) {
        return List.of(
                configHook(context, "echoashfallprotocol:ashfall_product_client", "client.config",
                        "Registers Ashfall product client UI and keybinding config."),
                configHook(context, "echoashfallprotocol:ashfall_product_survival", "server.config",
                        "Registers Ashfall product survival, hazard, and world-startup config."),
                configHook(context, "echoashfallprotocol:ashfall_native_release_policy", "release.config",
                        "Registers Ashfall native release policy config.")
        );
    }

    private static List<Map<String, Object>> clientSurfaceHooks(NativeLoaderProductBridgeContext context) {
        return List.of(
                clientSurfaceHook(context, "echoashfallprotocol:echo_native_main_menu", "main_menu"),
                clientSurfaceHook(context, "echoashfallprotocol:echo_native_loading", "loading_screen"),
                clientSurfaceHook(context, "echoashfallprotocol:ashfall_survival_hud", "hud"),
                clientSurfaceHook(context, "echoashfallprotocol:ashfall_status_overlay", "client_overlay"),
                clientSurfaceHandoffHook(context, "echoashfallprotocol:terminal_eui_handoff", "terminal",
                        "echoterminal", "echoterminal:eui"),
                clientSurfaceHandoffHook(context, "echoashfallprotocol:index_handoff", "index",
                        "echoindex", "echoindex:index"),
                clientSurfaceHandoffHook(context, "echoashfallprotocol:lens_handoff", "lens",
                        "echolens", "echolens:field_lens"),
                clientSurfaceHandoffHook(context, "echoashfallprotocol:holomap_minimap_handoff", "holomap",
                        "echoholomap", "echoholomap:minimap"),
                clientSurfaceHandoffHook(context, "echoashfallprotocol:holomap_fullscreen_handoff", "holomap",
                        "echoholomap", "echoholomap:fullscreen_map")
        );
    }

    private static List<Map<String, Object>> runtimeHooks(NativeLoaderProductBridgeContext context) {
        return List.of(
                runtimeHook(context, "inventory", "player:local-native-product:echoashfallprotocol:field_manual", "grant",
                        map("playerId", "player:local-native-product", "itemId", "echoashfallprotocol:field_manual", "count", 1)),
                runtimeHook(context, "player_state", "player:local-native-product:native_loader_profile", "write",
                        map("playerId", "player:local-native-product", "key", "ashfall.native_loader.profile", "value", "ready")),
                runtimeHook(context, "world_blocks", "minecraft:overworld:0,80,0", "place_block",
                        map("dimension", "minecraft:overworld", "x", 0, "y", 80, "z", 0, "blockId", "echoashfallprotocol:ash_layer")),
                runtimeHook(context, "world_state", "minecraft:overworld:ashfall.native_loader.world", "write",
                        map("dimension", "minecraft:overworld", "key", "ashfall.native_loader.world", "value", "ready")),
                runtimeHook(context, "structures", "echoashfallprotocol:secure_crash_outpost", "place",
                        map("dimension", "minecraft:overworld", "x", 8, "y", 80, "z", 8, "structureId", "echoashfallprotocol:secure_crash_outpost")),
                runtimeHook(context, "block_entities", "minecraft:overworld:0,80,0:ashfall_status", "write",
                        map("dimension", "minecraft:overworld", "x", 0, "y", 80, "z", 0, "key", "ashfall_status", "value", "online")),
                runtimeHook(context, "capabilities", "player:local-native-product:ashfall_survival", "write_state",
                        map("target", "player:local-native-product", "capability", "ashfall_survival", "value", "enabled")),
                runtimeHook(context, "missions", "echoashfallprotocol:ashfall_crash_landing", "start",
                        map("missionId", "echoashfallprotocol:ashfall_crash_landing", "phase", "started", "objectiveKey", "establish_survival_baseline")),
                runtimeHook(context, "events", "echoashfallprotocol:native_product_ready", "publish",
                        map("eventId", "echoashfallprotocol:native_product_ready", "payload", "ashfall_native_product_ready")),
                runtimeHook(context, "packets_hud", "echoashfallprotocol:ashfall_runtime_sync", "send",
                        map("channel", "echoashfallprotocol:ashfall_runtime_sync", "payload", "native_loader_runtime_sync")),
                runtimeHook(context, "save_data", "echoashfallprotocol.productProfile", "write",
                        map("key", "echoashfallprotocol.productProfile", "value", "echoashfallprotocol:ashfall_native_product")),
                runtimeHook(context, "hud", "echoashfallprotocol:onboarding", "notify",
                        map("channel", "echoashfallprotocol:onboarding", "message", "Ashfall Native Loader ready.")),
                runtimeHook(context, "client_tick", "echoashfallprotocol:ashfall_runtime_tick", "tick",
                        map("phase", "client_tick_end")),
                runtimeHook(context, "render_layers", "echoashfallprotocol:ashfall_survival_hud", "render",
                        map("layerId", "echoashfallprotocol:ashfall_survival_hud")),
                runtimeHook(context, "screen_events", "echoashfallprotocol:echo_native_main_menu", "open",
                        map("screenId", "echoashfallprotocol:echo_native_main_menu", "eventType", "open")),
                runtimeHook(context, "keybinds", "echoashfallprotocol:open_terminal", "press",
                        map("keybindId", "echoashfallprotocol:open_terminal", "action", "open_terminal")),
                runtimeHook(context, "commands", "echoashfallprotocol:open_terminal", "register",
                        map("commandId", "echoashfallprotocol:open_terminal", "targetSurface", "terminal", "targetBridge", "client_surface.terminal")),
                runtimeHook(context, "network_channels", "echoashfallprotocol:ashfall_runtime_sync", "register",
                        map("packetId", "echoashfallprotocol:ashfall_runtime_sync", "surface", "adaptercore.native_runtime_packet", "sourceRuntimeTarget", "ashfall_native_runtime", "consumers", List.of("terminal", "hud", "save_data"))),
                runtimeHook(context, "config_reloads", "echoashfallprotocol:ashfall_native_release_policy", "reload",
                        map("configId", "echoashfallprotocol:ashfall_native_release_policy", "scope", "release.config")),
                runtimeHook(context, "resource_reloads", "echoashfallprotocol:ashfall_worldgen_datapack", "reload",
                        map("resourceId", "echoashfallprotocol:ashfall_worldgen_datapack", "scope", "data_pack")),
                runtimeHook(context, "save_hooks", "echoashfallprotocol:ashfall_save_lifecycle", "save",
                        map("hookId", "echoashfallprotocol:ashfall_save_lifecycle")),
                runtimeHook(context, "lifecycle_phases", "ashfall_product_bridge_attached", "phase",
                        map("phaseId", "ashfall_product_bridge_attached")),
                runtimeHook(context, "server_client_sync", "echoashfallprotocol:ashfall_runtime_sync", "sync",
                        map("channel", "echoashfallprotocol:ashfall_runtime_sync", "payload", "ashfall_runtime_sync"))
        );
    }

    private static List<Map<String, Object>> productWorldHooks(NativeLoaderProductBridgeContext context) {
        return List.of(
                map(
                        "moduleId", context.moduleId(),
                        "worldId", "echoashfallprotocol:ashfall_product_world",
                        "defaultWorldMode", "create_or_open_product_world",
                        "productWorldPreset", "echoashfallprotocol:ashfall_wasteland",
                        "productDatapack", "echo-native-ashfall-datapack.zip",
                        "productResourcePack", "echoashfallprotocol:ashfall_client_resources",
                        "vanillaSavePolicy", "guard_existing_vanilla_saves_as_not_ashfall_product_world",
                        "nativeLoaderOwnedWorldPolicy", true,
                        "nativeProductWorldStartupOwnerClass", "dev.echo.nativeplatform.loader.NativeLoaderAshfallWorldStartupService",
                        "evidence", hookEvidence(
                                context,
                                "Native Loader launch defaults to the Ashfall product world preset; old vanilla saves are guarded as non-product worlds unless migrated."
                        )
                )
        );
    }

    private static List<Map<String, Object>> saveDataHooks(NativeLoaderProductBridgeContext context) {
        return List.of(
                map(
                        "key", "echoashfallprotocol.productProfile",
                        "value", "echoashfallprotocol:ashfall_native_product",
                        "delete", false
                ),
                map(
                        "key", "echoashfallprotocol.productWorldPreset",
                        "value", "echoashfallprotocol:ashfall_wasteland",
                        "delete", false
                )
        );
    }

    private static List<Map<String, Object>> productOnboardingHooks(NativeLoaderProductBridgeContext context) {
        return List.of(
                map(
                        "moduleId", context.moduleId(),
                        "playerId", "player:local-native-product",
                        "spawnProfile", "echoashfallprotocol:ashfall_wasteland_onboarding",
                        "spawnDimension", "minecraft:overworld",
                        "spawnStructureId", "echoashfallprotocol:secure_crash_outpost",
                        "starterItemId", "echoashfallprotocol:field_manual",
                        "missionId", "echoashfallprotocol:ashfall_crash_landing",
                        "missionPhase", "started",
                        "objectiveKey", "establish_survival_baseline",
                        "hudChannel", "echoashfallprotocol:onboarding",
                        "briefing", "Ashfall systems online. Establish shelter, scan hazards, and open the Terminal for the first route.",
                        "evidence", hookEvidence(
                                context,
                                "Ashfall Native Loader onboarding mutates player, world, mission, HUD, and save state through the native runtime instead of proof-smoke startup scaffolding."
                        )
                )
        );
    }

    private static Map<String, Object> registryHook(
            NativeLoaderProductBridgeContext context,
            String registry,
            String id,
            String summary
    ) {
        return map(
                "moduleId", context.moduleId(),
                "registry", registry,
                "id", id,
                "properties", hookEvidence(context, summary)
        );
    }

    private static Map<String, Object> lifecycleHook(
            NativeLoaderProductBridgeContext context,
            String phaseId,
            String summary
    ) {
        return map(
                "moduleId", context.moduleId(),
                "phaseId", phaseId,
                "evidence", hookEvidence(context, summary)
        );
    }

    private static Map<String, Object> eventSubscription(
            NativeLoaderProductBridgeContext context,
            String eventId,
            String handlerId,
            String summary
    ) {
        return map(
                "moduleId", context.moduleId(),
                "eventId", eventId,
                "handlerId", handlerId,
                "evidence", hookEvidence(context, summary)
        );
    }

    private static Map<String, Object> commandHook(
            NativeLoaderProductBridgeContext context,
            String commandId,
            String targetSurface,
            String targetBridge,
            String summary
    ) {
        return map(
                "moduleId", context.moduleId(),
                "commandId", commandId,
                "targetSurface", targetSurface,
                "targetBridge", targetBridge,
                "evidence", hookEvidence(context, summary)
        );
    }

    private static Map<String, Object> networkHook(
            NativeLoaderProductBridgeContext context,
            String packetId,
            String surface,
            String sourceRuntimeTarget,
            List<String> consumers,
            String summary
    ) {
        return map(
                "moduleId", context.moduleId(),
                "packetId", packetId,
                "surface", surface,
                "sourceRuntimeTarget", sourceRuntimeTarget,
                "consumers", consumers == null ? List.of() : List.copyOf(consumers),
                "evidence", hookEvidence(context, summary)
        );
    }

    private static Map<String, Object> resourceHook(
            NativeLoaderProductBridgeContext context,
            String resourceId,
            String resourceType,
            String summary
    ) {
        return map(
                "moduleId", context.moduleId(),
                "resourceId", resourceId,
                "resourceType", resourceType,
                "evidence", hookEvidence(context, summary)
        );
    }

    private static Map<String, Object> configHook(
            NativeLoaderProductBridgeContext context,
            String configId,
            String scope,
            String summary
    ) {
        return map(
                "moduleId", context.moduleId(),
                "configId", configId,
                "scope", scope,
                "evidence", hookEvidence(context, summary)
        );
    }

    private static Map<String, Object> runtimeHook(
            NativeLoaderProductBridgeContext context,
            String surface,
            String targetId,
            String action,
            Map<String, Object> payload
    ) {
        Map<String, Object> safePayload = new LinkedHashMap<>(payload == null ? Map.of() : payload);
        safePayload.put("evidence", hookEvidence(
                context,
                "Ashfall product runtime hook drives " + surface + " through Native Loader AdapterCore runtime services."
        ));
        return map(
                "moduleId", context.moduleId(),
                "surface", surface,
                "targetId", targetId,
                "action", action,
                "payload", Map.copyOf(safePayload)
        );
    }

    private static Map<String, Object> clientSurfaceHook(
            NativeLoaderProductBridgeContext context,
            String surfaceId,
            String surfaceType
    ) {
        return clientSurfaceHook(context, surfaceId, surfaceType, Map.of());
    }

    private static Map<String, Object> clientSurfaceHook(
            NativeLoaderProductBridgeContext context,
            String surfaceId,
            String surfaceType,
            Map<String, Object> extraConfig
    ) {
        Map<String, Object> config = new LinkedHashMap<>(hookEvidence(
                context,
                "Routes " + surfaceType + " through the Ashfall native product client surface host."
        ));
        config.putAll(extraConfig);
        return map(
                "moduleId", context.moduleId(),
                "surfaceId", surfaceId,
                "surfaceType", surfaceType,
                "config", Map.copyOf(config)
        );
    }

    private static Map<String, Object> clientSurfaceHandoffHook(
            NativeLoaderProductBridgeContext context,
            String ashfallSurfaceId,
            String surfaceType,
            String targetModuleId,
            String targetSurfaceId
    ) {
        return clientSurfaceHook(
                context,
                ashfallSurfaceId,
                surfaceType,
                map(
                        "handoff", true,
                        "targetModuleId", targetModuleId,
                        "targetSurfaceId", targetSurfaceId
                )
        );
    }

    private static Map<String, Object> hookEvidence(NativeLoaderProductBridgeContext context, String summary) {
        return map(
                "providerClass", AshfallNativeProductBridgeProvider.class.getName(),
                "packId", context.packId(),
                "moduleId", context.moduleId(),
                "productProfile", "echoashfallprotocol:ashfall_native_product",
                "nativeProductWorldPreset", "echoashfallprotocol:ashfall_wasteland",
                "ashfallProductBridgeProvider", true,
                "nativeProductHookPlan", true,
                "nativeSdkHook", true,
                "firstClassNativeRuntime", true,
                "releaseMutationStatus", EchoNativeLoadStatus.MUTATED.name(),
                "summary", summary
        );
    }

    private static Map<String, Object> map(Object... entries) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (entries == null) {
            return Map.of();
        }
        for (int index = 0; index + 1 < entries.length; index += 2) {
            Object key = entries[index];
            Object value = entries[index + 1];
            if (key != null && value != null) {
                result.put(String.valueOf(key), value);
            }
        }
        return Map.copyOf(result);
    }

    private static final class AshfallProductRegistryBridge implements NativeLoaderLiveRegistryBridge {
        private final NativeLoaderProductBridgeContext context;
        private final Map<String, Map<String, Object>> mutatedRecords = new LinkedHashMap<>();

        private AshfallProductRegistryBridge(NativeLoaderProductBridgeContext context) {
            this.context = context;
        }

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return REGISTRY_BRIDGE_ID + ":" + context.moduleId();
        }

        @Override
        public boolean firstClassNativeRegistry() {
            return true;
        }

        @Override
        public boolean nativeRegistryProcess() {
            return true;
        }

        @Override
        public boolean releaseRegistryTrusted() {
            return true;
        }

        @Override
        public boolean nativeRegistryMutationSupported() {
            return true;
        }

        @Override
        public Map<String, Object> registryEvidence() {
            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("bridgeId", bridgeId());
            evidence.put("attached", attached());
            evidence.put("providerClass", AshfallNativeProductBridgeProvider.class.getName());
            evidence.put("packId", context.packId());
            evidence.put("moduleId", context.moduleId());
            evidence.put("productRoot", context.productRoot().toString());
            evidence.put("moduleRoot", context.moduleRoot().toString());
            evidence.put("productProfile", "echoashfallprotocol:ashfall_native_product");
            evidence.put("firstClassNativeRegistry", firstClassNativeRegistry());
            evidence.put("nativeRegistryProcess", nativeRegistryProcess());
            evidence.put("releaseRegistryTrusted", releaseRegistryTrusted());
            evidence.put("nativeRegistryMutationSupported", nativeRegistryMutationSupported());
            evidence.put("productNativeRegistryTableMutated", !mutatedRecords.isEmpty());
            evidence.put("mutatedRecordCount", mutatedRecords.size());
            evidence.put("mutatedRegistryKinds", mutatedRecords.values().stream()
                    .map(record -> String.valueOf(record.get("registry")))
                    .distinct()
                    .sorted()
                    .toList());
            evidence.put("mutatedRecordIds", mutatedRecords.keySet().stream().sorted().toList());
            evidence.put("mutatedRecords", Map.copyOf(mutatedRecords));
            evidence.put("summary", "Ashfall product registry bridge mutates the first-class Native Loader product registry table for all declared registry kinds.");
            return Map.copyOf(evidence);
        }

        @Override
        public Map<String, Object> registryMutationRecord(String registry, String namespace, String id) {
            if (!required(registry, id)) {
                return Map.of();
            }
            RegistryIdentity identity = registryIdentity(namespace, id);
            if (!identity.valid()) {
                return Map.of();
            }
            Map<String, Object> record = mutatedRecords.get(mutationRecordKey(registry, identity.namespace(), identity.id()));
            return record == null ? Map.of() : record;
        }

        @Override
        public EchoNativeLoadStatus register(
                String registry,
                String namespace,
                String id,
                String implementationClass,
                Map<String, Object> properties
        ) {
            if (!required(registry, id) || !isSupportedRegistrySurface(registry)) {
                return EchoNativeLoadStatus.UNSUPPORTED;
            }
            RegistryIdentity identity = registryIdentity(namespace, id);
            if (!identity.valid()) {
                return EchoNativeLoadStatus.UNSUPPORTED;
            }
            String normalizedRegistry = normalizedRegistrySurface(registry);
            String normalizedNamespace = identity.namespace();
            String normalizedId = identity.id();
            String fullId = normalizedNamespace + ":" + normalizedId;
            String key = mutationRecordKey(normalizedRegistry, normalizedNamespace, normalizedId);
            if (mutatedRecords.containsKey(key)) {
                return EchoNativeLoadStatus.RESOLVED;
            }
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("sequence", mutatedRecords.size() + 1);
            record.put("registry", normalizedRegistry);
            record.put("namespace", normalizedNamespace);
            record.put("id", normalizedId);
            record.put("fullId", fullId);
            record.put("implementationClass", implementationClass == null ? "" : implementationClass);
            record.put("status", EchoNativeLoadStatus.MUTATED.name());
            record.put("bridgeId", bridgeId());
            record.put("packId", context.packId());
            record.put("moduleId", context.moduleId());
            record.put("productProfile", "echoashfallprotocol:ashfall_native_product");
            record.put("mutationSurface", "ashfall_native_product_registry_table");
            record.put("liveRegistryMutationApplied", true);
            record.put("firstClassNativeRegistry", true);
            record.put("nativeRegistryProcess", true);
            record.put("releaseRegistryTrusted", true);
            record.put("nativeRegistryMutationSupported", true);
            record.put("productNativeRegistryTableMutated", true);
            record.put("properties", properties == null ? Map.of() : Map.copyOf(properties));
            mutatedRecords.put(key, Map.copyOf(record));
            return EchoNativeLoadStatus.MUTATED;
        }

        private static String mutationRecordKey(String registry, String namespace, String id) {
            String normalizedRegistry = normalizedRegistrySurface(registry);
            RegistryIdentity identity = registryIdentity(namespace, id);
            return normalizedRegistry + ":" + identity.namespace() + ":" + identity.id();
        }
    }

    private record RegistryIdentity(String namespace, String id) {
        private boolean valid() {
            return !namespace.isBlank() && !id.isBlank();
        }
    }

    private static final class AshfallProductClientSurfaceBridge implements NativeLoaderLiveClientBridge {
        private final NativeLoaderProductBridgeContext context;

        private AshfallProductClientSurfaceBridge(NativeLoaderProductBridgeContext context) {
            this.context = context;
        }

        @Override
        public boolean attached() {
            return true;
        }

        @Override
        public String bridgeId() {
            return CLIENT_BRIDGE_ID + ":" + context.moduleId();
        }

        @Override
        public boolean firstClassNativeClientRouteTable() {
            return true;
        }

        @Override
        public boolean nativeClientRouteProcess() {
            return true;
        }

        @Override
        public boolean releaseClientRouteTrusted() {
            return true;
        }

        @Override
        public boolean clientRouteMutationSupported() {
            return true;
        }

        @Override
        public boolean firstClassNativeClientRenderPipeline() {
            return true;
        }

        @Override
        public boolean nativeClientRenderProcess() {
            return windowedNativeClientActive();
        }

        @Override
        public boolean releaseClientRenderTrusted() {
            return windowedNativeClientActive();
        }

        @Override
        public boolean clientRenderMutationSupported() {
            return windowedNativeClientActive();
        }

        @Override
        public EchoNativeLoadStatus registerSurface(
                String moduleId,
                String surfaceId,
                String surfaceType,
                Map<String, Object> config
        ) {
            if (!required(moduleId, surfaceId, surfaceType) || !isSupportedClientSurface(surfaceType)) {
                return EchoNativeLoadStatus.UNSUPPORTED;
            }
            return EchoNativeLoadStatus.MUTATED;
        }

        @Override
        public Map<String, Object> surfaceRegistrationEvidence(
                String moduleId,
                String surfaceId,
                String surfaceType,
                Map<String, Object> config
        ) {
            boolean windowedNativeClient = windowedNativeClientActive();
            String normalizedType = normalizedSurfaceType(surfaceType);
            boolean clientSurfaceMutated = isSupportedClientSurface(normalizedType);
            boolean renderSurfaceMutated = windowedNativeClient && clientSurfaceMutated && isLiveRenderSurface(normalizedType);
            String routeId = "echoashfallprotocol:native_client_surface/"
                    + context.packId()
                    + "/"
                    + moduleId
                    + "/"
                    + normalizedType
                    + "/"
                    + surfaceId;
            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("providerClass", AshfallNativeProductBridgeProvider.class.getName());
            evidence.put("packId", context.packId());
            evidence.put("moduleId", moduleId);
            evidence.put("surfaceId", surfaceId);
            evidence.put("surfaceType", normalizedType);
            evidence.put("ashfallProductClientRouteId", routeId);
            evidence.put("presentationKind", presentationKind(normalizedType));
            evidence.put("productClientRouteTableRegistered", true);
            evidence.put("productClientRouteTableMutated", clientSurfaceMutated);
            evidence.put("firstClassNativeClientSurface", true);
            evidence.put("firstClassNativeClientRouteTable", true);
            evidence.put("nativeClientRouteProcess", true);
            evidence.put("releaseClientRouteTrusted", true);
            evidence.put("clientRouteMutationSupported", true);
            evidence.put("firstClassNativeClientRenderPipeline", true);
            evidence.put("nativeClientRenderProcess", windowedNativeClient);
            evidence.put("releaseClientRenderTrusted", windowedNativeClient);
            evidence.put("clientRenderMutationSupported", windowedNativeClient);
            evidence.put("nativeClientRenderPipelineMutated", renderSurfaceMutated);
            evidence.put("nativeHudRenderBridgeClass", "dev.echo.nativeplatform.bootstrap.EchoNativeLiveHudRenderBridge");
            evidence.put("nativeLoadingRenderBridgeClass", "dev.echo.nativeplatform.bootstrap.EchoNativeLiveLoadingRenderBridge");
            evidence.put("nativeSurfaceImplementationClass", surfaceImplementationClass(normalizedType));
            evidence.put("nativeScreenBridgeClass", screenBridgeClass(normalizedType));
            evidence.put("nativeKeybindingOwnerClass", "com.knoxhack.echoashfallprotocol.EchoAshfallProtocolClient");
            evidence.put("liveMinecraftClientSurfaceMutated", windowedNativeClient && clientSurfaceMutated);
            evidence.put("liveMinecraftRenderPipelineMutated", renderSurfaceMutated);
            evidence.put("realClientProcess", windowedNativeClient);
            evidence.put("releaseClientTrusted", windowedNativeClient);
            evidence.put("releaseMutationStatus", clientSurfaceMutated
                    ? EchoNativeLoadStatus.MUTATED.name()
                    : EchoNativeLoadStatus.REGISTERED.name());
            evidence.put("summary", windowedNativeClient
                    ? "Ashfall product client route mutates the first-class native route table and is backed by the trusted first-party native render bridge in the windowed Native Loader client."
                    : "Ashfall product client route mutates the first-class native route table; render mutation remains gated to the windowed Native Loader client.");
            evidence.put("config", config == null ? Map.of() : Map.copyOf(config));
            return Map.copyOf(evidence);
        }
    }

    private static boolean windowedNativeClientActive() {
        return dev.echo.nativeplatform.contracts.EchoNativeClientRuntimeEnvironment.isWindowedNativeClient();
    }

    private static Map<String, String> surfaceImplementationClasses() {
        return Map.ofEntries(
                Map.entry("main_menu", "com.knoxhack.echoashfallprotocol.client.screen.EchoNativeMainMenuScreen"),
                Map.entry("loading_screen", "com.knoxhack.echoashfallprotocol.client.screen.EchoNativeAshfallLoadingOverlay"),
                Map.entry("hud", "com.knoxhack.echoashfallprotocol.client.hud.SurvivalHudOverlay"),
                Map.entry("client_overlay", "com.knoxhack.echoashfallprotocol.client.hud.MutationOverlayEffect"),
                Map.entry("terminal", "com.knoxhack.echoterminal.client.screen.EchoTerminalScreen"),
                Map.entry("index", "com.knoxhack.echoindex.client.IndexCatalogScreen"),
                Map.entry("lens", "com.knoxhack.echolens.client.LensHudOverlay"),
                Map.entry("holomap", "com.knoxhack.echoholomap.client.HoloMapFullScreenMapScreen")
        );
    }

    private static String surfaceImplementationClass(String surfaceType) {
        return surfaceImplementationClasses().getOrDefault(normalizedSurfaceType(surfaceType), "");
    }

    private static String screenBridgeClass(String surfaceType) {
        return switch (normalizedSurfaceType(surfaceType)) {
            case "main_menu" -> "com.knoxhack.echoashfallprotocol.client.screen.EchoNativeMainMenuScreen";
            case "loading_screen" -> "dev.echo.nativeplatform.bootstrap.EchoNativeLiveLoadingRenderBridge";
            case "hud", "client_overlay" -> "dev.echo.nativeplatform.bootstrap.EchoNativeLiveHudRenderBridge";
            case "terminal" -> "com.knoxhack.echoterminal.client.screen.EchoTerminalScreens";
            case "index" -> "com.knoxhack.echoindex.client.IndexScreenCoreBridge";
            case "lens" -> "com.knoxhack.echolens.client.LensHudOverlay";
            case "holomap" -> "com.knoxhack.echoholomap.integration.HoloMapScreenCoreIntegration";
            default -> "";
        };
    }

    private static EchoNativeLoadStatus registeredIfValid(boolean valid) {
        return valid ? EchoNativeLoadStatus.REGISTERED : EchoNativeLoadStatus.FAILED;
    }

    private static boolean isSupportedRegistrySurface(String registry) {
        String type = normalizedRegistrySurface(registry);
        return EchoNativeRegistryHost.firstClassRegistryKinds().contains(type);
    }

    private static String normalizedRegistrySurface(String registry) {
        String type = normalized(registry);
        return switch (type) {
            case "items" -> "item";
            case "blocks" -> "block";
            case "entities" -> "entity";
            case "blockentity", "blockentities", "block_entity", "block_entities" -> "block_entity";
            case "menus" -> "menu";
            case "sounds" -> "sound";
            case "particles", "particle_profile", "particle_profiles" -> "particle";
            case "effects", "mob_effect", "mob_effects", "mobeffect", "mobeffects" -> "effect";
            case "creativegroup", "creativegroups", "creative_group", "creative_groups",
                    "creative_tab", "creative_tabs" -> "creative_tab";
            case "commands" -> "command";
            case "datacomponent", "datacomponents", "data_component", "data_components" -> "data_component";
            case "recipes" -> "recipe";
            case "biomes" -> "biome";
            case "configured_feature", "configured_features", "placed_feature", "placed_features",
                    "world_generator", "world_generators", "worldgens" -> "worldgen";
            case "asset", "assets", "clientasset", "clientassets", "client_asset", "client_assets" -> "client_asset";
            default -> type;
        };
    }

    private static RegistryIdentity registryIdentity(String namespace, String id) {
        String normalizedNamespace = namespace == null ? "" : namespace.trim().toLowerCase(Locale.ROOT);
        String normalizedId = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
        int separator = normalizedId.indexOf(':');
        if (separator > 0 && separator + 1 < normalizedId.length()) {
            normalizedNamespace = normalizedId.substring(0, separator);
            normalizedId = normalizedId.substring(separator + 1);
        }
        return new RegistryIdentity(normalizedNamespace, normalizedId);
    }

    private static boolean isSupportedClientSurface(String surfaceType) {
        String type = normalizedSurfaceType(surfaceType);
        return switch (type) {
            case "ui_surface", "ui_overlay", "client_overlay", "hud", "hud_widget", "hud_layout",
                    "screen", "screen_surface", "loading_screen", "main_menu", "terminal", "index",
                    "lens", "holomap", "holo_map", "minimap", "theme" -> true;
            default -> false;
        };
    }

    private static boolean isLiveRenderSurface(String surfaceType) {
        return switch (normalizedSurfaceType(surfaceType)) {
            case "ui_overlay", "client_overlay", "hud", "hud_widget", "hud_layout",
                    "loading_screen", "main_menu", "theme" -> true;
            default -> false;
        };
    }

    private static String presentationKind(String surfaceType) {
        return switch (normalizedSurfaceType(surfaceType)) {
            case "ui_overlay", "client_overlay" -> "overlay";
            case "hud", "hud_widget", "hud_layout" -> "hud";
            case "loading_screen" -> "loading";
            case "main_menu" -> "main_menu";
            case "terminal" -> "terminal";
            case "index" -> "index";
            case "lens" -> "lens";
            case "holomap", "holo_map", "minimap" -> "holomap";
            case "theme" -> "theme";
            case "screen", "screen_surface" -> "screen";
            default -> "surface";
        };
    }

    private static String normalizedSurfaceType(String surfaceType) {
        return normalized(surfaceType);
    }

    private static String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace('.', '_');
    }

    private static boolean required(String... values) {
        if (values == null || values.length == 0) {
            return false;
        }
        for (String value : values) {
            if (value == null || value.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private record NativeCreativeTabSource(
            List<String> itemIds,
            List<String> registryBackedItemIds,
            List<String> featuredItemIds,
            List<String> sourceNamespaces,
            boolean sourceResolved
    ) {
    }
}
