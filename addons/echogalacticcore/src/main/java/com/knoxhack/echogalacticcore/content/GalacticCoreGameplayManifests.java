package com.knoxhack.echogalacticcore.content;

import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadContext;
import dev.echo.nativeplatform.contracts.EchoNativeResourceService;

import java.util.List;
import java.util.Map;

public final class GalacticCoreGameplayManifests {
    private static final List<GalacticCoreContentDefinitions.Registration> MANIFESTS = List.of(
            manifest(
                    "gameplay/foundation",
                    "data/echogalacticcore/port/gameplay_foundation.json",
                    "Material, block, and recipe foundation contracts for the first Galacticraft-style progression slice."
            ),
            manifest(
                    "gameplay/machines",
                    "data/echogalacticcore/port/gameplay_machines.json",
                    "Machine runtime contracts for oxygen collector, oxygen sealer, fuel loader, and rocket workbench."
            ),
            manifest(
                    "gameplay/life_support",
                    "data/echogalacticcore/port/life_support_contracts.json",
                    "Oxygen gear, thermal state, breathable entities, and player attachment contracts."
            ),
            manifest(
                    "gameplay/rocket_progression",
                    "data/echogalacticcore/port/rocket_progression.json",
                    "Rocket launch state, route unlocks, vehicle tiers, packets, and HoloMap route contracts."
            ),
            manifest(
                    "gameplay/energy_bridge",
                    "data/echogalacticcore/port/energy_bridge.json",
                    "Galacticraft energy interface parity mapped onto ECHO power and machine capability contracts."
            ),
            manifest(
                    "gameplay/player_gear",
                    "data/echogalacticcore/port/player_gear.json",
                    "Player gear slots, migrated GCPlayerStats attachments, and client HUD/render readiness contracts."
            ),
            manifest(
                    "gameplay/celestial_environments",
                    "data/echogalacticcore/port/celestial_environments.json",
                    "Moon, Mars, Asteroids, Venus, and orbit environment contracts for worldgen, Lens, HoloMap, and Ashfall milestones."
            ),
            manifest(
                    "gameplay/outer_planet_progression",
                    "data/echogalacticcore/port/outer_planet_progression.json",
                    "Mars, Asteroids, and Venus route unlock order, rocket tier requirements, and PackOS milestone contracts."
            ),
            manifest(
                    "gameplay/dungeon_reward_progression",
                    "data/echogalacticcore/port/dungeon_reward_progression.json",
                    "Dungeon boss keys, treasure loot, schematic rewards, and progression unlock contracts."
            ),
            manifest(
                    "gameplay/dungeon_boss_parity",
                    "data/echogalacticcore/port/dungeon_boss_parity.json",
                    "Dungeon configuration, boss, loot, and key parity manifest for Moon, Mars, and Venus."
            ),
            manifest(
                    "gameplay/celestial_routes",
                    "data/echogalacticcore/port/celestial_routes.json",
                    "Data manifest for Orbit, Moon, Mars, Asteroids, and Venus route registration."
            ),
            manifest(
                    "gameplay/echo_integrations",
                    "data/echogalacticcore/port/echo_integrations.json",
                    "Typed integration manifest for PackOS, Index, Lens, HoloMap, ScreenCore, and Ashfall."
            )
    );

    private GalacticCoreGameplayManifests() {
    }

    public static void register(EchoNativeModuleLoadContext context, EchoNativeResourceService resources) {
        for (GalacticCoreContentDefinitions.Registration manifest : MANIFESTS) {
            GalacticCoreRegistrarSupport.record(
                    context,
                    resources.registerReloadListener(GalacticCoreRegistrarSupport.mutation(
                            "resources",
                            "registerReloadListener",
                            manifest
                    ))
            );
        }
    }

    private static GalacticCoreContentDefinitions.Registration manifest(String path, String resource, String description) {
        return new GalacticCoreContentDefinitions.Registration(
                path,
                "gameplay_parity_manifest",
                "Galacticraft Legacy gameplay systems",
                Map.of("resource", resource, "description", description, "system", "gameplay_parity")
        );
    }
}
