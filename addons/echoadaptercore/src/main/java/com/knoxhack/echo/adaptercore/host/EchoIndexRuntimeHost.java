package com.knoxhack.echo.adaptercore.host;

import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.Set;

/**
 * Declares {@code echoindex} capabilities in the AdapterCore truth layer.
 */
public final class EchoIndexRuntimeHost extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoindex:runtime_host";
    private static final EchoIndexRuntimeHost HOST = new EchoIndexRuntimeHost();

    private EchoIndexRuntimeHost() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of(
                        "EchoNativeRuntimeHost.Capabilities",
                        "EchoNativeRuntimeHost.Events",
                        "EchoNativeRuntimeHost.Hud",
                        "EchoNativeRuntimeHost.Packets",
                        "EchoNativeRuntimeHost.PlayerInventory"),
                Set.of(
                        "index.recipe_query",
                        "index.recipe_transfer",
                        "index.recipe_trace",
                        "index.recipe_click",
                        "index.item_click",
                        "index.bookmark_add",
                        "index.bookmark_remove",
                        "index.inventory_overlay_render",
                        "index.inventory_overlay_input",
                        "index.inventory_overlay_toggle",
                        "index.catalog_open",
                        "index.catalog_search",
                        "index.filter_changed",
                        "index.ui_open",
                        "index.ui_close"),
                Set.of(
                        "index.recipes",
                        "index.inventory_overlay",
                        "echoindex:inventory_overlay",
                        "echoindex:recipe_search/index_query",
                        "echoindex:index_service",
                        "echoindex:inventory_overlay_service",
                        "echoindex:index",
                        "echoindex:recipe_search",
                        "echoindex:first_party_backend"),
                false,
                false,
                true));
    }
}
