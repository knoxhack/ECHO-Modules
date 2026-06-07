package com.knoxhack.echoholomap.integration;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.index.IIndexContentProvider;
import com.knoxhack.echocore.api.index.IndexBuildContext;
import com.knoxhack.echocore.api.index.IndexCategory;
import com.knoxhack.echocore.api.index.IndexContentSnapshot;
import com.knoxhack.echocore.api.index.IndexEntry;
import com.knoxhack.echocore.api.index.IndexEntryState;
import com.knoxhack.echoholomap.EchoHoloMap;
import com.knoxhack.echoholomap.HoloMapIds;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class HoloMapIndexIntegration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final Identifier CATEGORY = HoloMapIds.id("index/holomap");
    private static final IIndexContentProvider PROVIDER = new IIndexContentProvider() {
        @Override
        public Identifier id() {
            return HoloMapIds.id("index/provider");
        }

        @Override
        public IndexContentSnapshot snapshot(IndexBuildContext context) {
            List<IndexCategory> categories = List.of(new IndexCategory(
                    CATEGORY,
                    "index.echoholomap.category",
                    "index.echoholomap.category.desc",
                    new ItemStack(Items.COMPASS),
                    130,
                    EchoHoloMap.MODID));
            List<IndexEntry> entries = List.of(
                    entry("terrain_discovery", "Terrain Discovery", "Earned, server-owned terrain tiles.", 10),
                    entry("waypoints", "Waypoints", "Local, personal, shared, and deathpoint waypoint behavior.", 20),
                    entry("marker_states", "Marker States", "Hidden, locked, discovered, and checked tactical marker states.", 30),
                    entry("routes", "Routes", "Ordered rich route data and Core marker route adaptation.", 40),
                    entry("overlays", "Overlays", "Estimated regions, scan radii, hazard footprints, and soft rings.", 50),
                    entry("provider_authoring", "Provider Authoring", "How addons register HoloMap-local providers safely.", 60));
            return new IndexContentSnapshot(id(), categories, entries,
                    List.of(), List.of(), List.of(), List.of(), List.of());
        }
    };

    private HoloMapIndexIntegration() {
    }

    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) {
            EchoCoreServices.registerIndexContentProvider(PROVIDER);
        }
    }

    private static IndexEntry entry(String path, String title, String summary, int sortOrder) {
        Identifier id = HoloMapIds.id("index/" + path);
        return new IndexEntry(
                id,
                CATEGORY,
                title,
                "ECHO: HoloMap",
                summary,
                summary,
                new ItemStack(Items.MAP),
                EchoHoloMap.MODID,
                List.of("holomap", "map", path),
                IndexEntryState.VISIBLE,
                List.of(),
                List.of(),
                List.of(),
                sortOrder);
    }
}
