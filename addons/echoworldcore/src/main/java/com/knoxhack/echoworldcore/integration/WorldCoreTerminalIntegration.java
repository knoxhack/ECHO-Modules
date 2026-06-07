package com.knoxhack.echoworldcore.integration;

import com.knoxhack.echocore.api.EchoRuntimeModules;
import com.knoxhack.echocore.api.WorldHazardSnapshot;
import com.knoxhack.echocore.api.WorldRegionInstance;
import com.knoxhack.echoterminal.api.TerminalAddonGuide;
import com.knoxhack.echoterminal.api.TerminalAddonInfo;
import com.knoxhack.echoterminal.api.TerminalAddonInfoProvider;
import com.knoxhack.echoterminal.api.TerminalAddonInfoRegistry;
import com.knoxhack.echoterminal.api.TerminalAddonLink;
import com.knoxhack.echoterminal.api.TerminalAddonMetric;
import com.knoxhack.echoterminal.api.TerminalAddonSection;
import com.knoxhack.echoterminal.api.TerminalArchiveEntry;
import com.knoxhack.echoterminal.api.TerminalArchiveRegistry;
import com.knoxhack.echoterminal.api.TerminalUi;
import com.knoxhack.echoworldcore.EchoWorldCore;
import com.knoxhack.echoworldcore.service.WorldRegionService;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class WorldCoreTerminalIntegration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private WorldCoreTerminalIntegration() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        TerminalAddonInfoRegistry.register(new WorldCoreAddonInfoProvider());
        TerminalArchiveRegistry.register(new TerminalArchiveEntry(
                id("worldcore_field_index"),
                "WorldCore",
                "Shared Region Index",
                "OPEN",
                List.of(
                        "WorldCore centralizes region names, hazard identity, discovery markers, and route-safe world events.",
                        "Ashfall, Orbital, Convoy, Nexus, Terminal, HoloMap-style consumers, and debug tools can all read the same shared field map.",
                        "Gameplay authority stays in the owning chapter; WorldCore records what the world is called and what the player has discovered."),
                false));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoWorldCore.MODID, path);
    }

    private static final class WorldCoreAddonInfoProvider implements TerminalAddonInfoProvider {
        @Override
        public String chapterId() {
            return EchoWorldCore.CHAPTER_ID;
        }

        @Override
        public TerminalAddonInfo info(Player player) {
            WorldRegionService service = WorldRegionService.INSTANCE;
            if (player == null) {
                return new TerminalAddonInfo(
                        "Shared field map, region discovery, marker registry, and hazard context.",
                        List.of(new TerminalAddonMetric("Definitions", String.valueOf(service.regionDefinitions().size()),
                                "registered shared region profiles", TerminalUi.CYAN)),
                        List.of(new TerminalAddonSection("World Feed",
                                List.of("Waiting for player telemetry to resolve nearby regions."))),
                        List.of(),
                        guide());
            }
            List<WorldRegionInstance> active = service.activeRegions(player);
            WorldHazardSnapshot hazard = service.hazardSnapshot(player);
            List<String> validation = service.validateMarkers(player.level());
            return new TerminalAddonInfo(
                    "Shared field map, region discovery, marker registry, and hazard context.",
                    List.of(
                            new TerminalAddonMetric("Active", String.valueOf(active.size()),
                                    "regions at current position", active.isEmpty() ? TerminalUi.AMBER : TerminalUi.GREEN),
                            new TerminalAddonMetric("Markers", String.valueOf(service.markers(player).size()),
                                    "known markers in this dimension", TerminalUi.CYAN),
                            new TerminalAddonMetric("Hazard", hazard.safeZone() ? "NOMINAL" : String.valueOf(hazard.severity()),
                                    hazard.summary(), hazard.safeZone() ? TerminalUi.GREEN : TerminalUi.AMBER),
                            new TerminalAddonMetric("Discovered", String.valueOf(service.discoveredRegions(player).size()),
                                    "stored shared region discoveries", TerminalUi.CYAN),
                            new TerminalAddonMetric("Validation", validation.isEmpty() ? "OK" : String.valueOf(validation.size()),
                                    validation.isEmpty() ? "definitions and markers validate" : "warning(s) require operator review",
                                    validation.isEmpty() ? TerminalUi.GREEN : TerminalUi.AMBER)),
                    List.of(new TerminalAddonSection("Nearby Regions",
                            active.isEmpty()
                                    ? List.of("No shared region is active at this position.")
                                    : active.stream().limit(4).map(region -> region.displayName()
                                            + " | " + region.type().displayName()).toList()),
                            new TerminalAddonSection("Validation",
                                    validation.isEmpty()
                                            ? List.of("WorldCore definitions and marker references are currently valid.")
                                            : validation.stream().limit(4).toList()),
                            new TerminalAddonSection("Next Step",
                                    List.of("Enter mapped regions, scan structures, or open HoloMap to inspect shared markers."))),
                    links(),
                    guide());
        }

        private static List<TerminalAddonLink> links() {
            if (!EchoRuntimeModules.isLoaded("echoholomap")) {
                return List.of();
            }
            return List.of(new TerminalAddonLink(
                    Identifier.fromNamespaceAndPath("echoholomap", "terminal/holomap"),
                    "Open HoloMap",
                    "Inspect WorldCore regions, markers, and hazards through the shared map feed.",
                    TerminalUi.CYAN));
        }

        private static TerminalAddonGuide guide() {
            return TerminalAddonGuide.optional(100, "Shared systems",
                    "Use WorldCore as the shared map vocabulary for scans, hazards, routes, and discovered structures.",
                    List.of(
                            "Enter Ashfall region biomes to discover shared regions.",
                            "Scan POIs or route markers to record map markers.",
                            "Use /echoworld nearby, hazard, markers, and validate for QA."));
        }
    }
}
