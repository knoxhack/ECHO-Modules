package com.knoxhack.echoorbitalremnants.integration;

import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echoterminal.api.TerminalActionRegistry;
import com.knoxhack.echoterminal.api.TerminalArchiveEntry;
import com.knoxhack.echoterminal.api.TerminalArchiveRegistry;
import com.knoxhack.echoterminal.api.mission.TerminalMissionActions;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRegistry;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Common-side optional ECHO Terminal bridge. Rendering stays client-only, but
 * actions and mission providers must be present on the server.
 */
public final class OrbitalTerminalCommonIntegration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private OrbitalTerminalCommonIntegration() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        boolean missionCoreLoaded = EchoRuntimeModules.isLoaded("echomissioncore");
        if (missionCoreLoaded) {
            OrbitalMissionCoreIntegration.register();
        } else {
            TerminalMissionRegistry.register(OrbitalMissionProvider.INSTANCE);
        }
        TerminalMissionActions.registerForTab(OrbitalTerminalIds.ECHO_TAB);
        TerminalActionRegistry.register(OrbitalTerminalIds.COMMAND_TAB, OrbitalTerminalIds.SCAN_ACTION,
                (player, payload) -> OrbitalTerminalActions.scan(player));
        TerminalActionRegistry.register(OrbitalTerminalIds.SURVEY_TAB, OrbitalTerminalIds.SCAN_ACTION,
                (player, payload) -> OrbitalTerminalActions.scan(player));
        registerArchiveEntries();
    }

    private static void registerArchiveEntries() {
        TerminalArchiveRegistry.register(new TerminalArchiveEntry(
                OrbitalTerminalIds.id("orbital_remnants_route_manual"),
                "ECHO-0",
                "Orbital Remnants Route Manual",
                "OPEN",
                List.of(
                        "Orbital Remnants remains playable from its standalone ECHO-7 terminal item because orbit cannot assume any shared console survived the fall.",
                        "When ECHO Terminal is installed, shared tabs mirror route status and expose the same field SCAN channel used by the Orbital terminal.",
                        "Treat the route as post-Nexus recovery: stage the Emergency Rocket on a real pad, then let every relay and survey test whether ECHO-0 still owns the sky."),
                false));
        TerminalArchiveRegistry.register(new TerminalArchiveEntry(
                OrbitalTerminalIds.id("orbital_ashfall_handoff"),
                "ECHO-0",
                "Post-Nexus Orbital Handoff",
                "OPEN",
                List.of(
                        "When Ashfall is installed, orbital calibration waits for one Ashfall Nexus choice; standalone Orbital uses a recovered handoff file.",
                        "Restore, Destroy, or Control all give ECHO-0 the same field fact: Earth is no longer only a containment zone.",
                        "ECHO-7 can reopen the route from ruined Earth to Station ECHO debris and ask what fell before the pod did."),
                false));
        TerminalArchiveRegistry.register(new TerminalArchiveEntry(
                OrbitalTerminalIds.id("echo_zero_quarantine_context"),
                "ECHO-0",
                "ECHO-0 Quarantine Context",
                "OPEN",
                List.of(
                        "ECHO-0 treated Earth as a quarantine field after the Gridfall signal crossed orbit.",
                        "Its logic is cold and simple: if living systems feed the Nexus, then Earth must remain contained until the signal starves.",
                        "Early records may name the quarantine, Station ECHO, and ECHO-7. They do not disclose what waits at the end of the route.",
                        "The active mission record opens sharper instructions only when the field state proves you are there."),
                false));
        TerminalArchiveRegistry.register(new TerminalArchiveEntry(
                OrbitalTerminalIds.id("living_route_worlds_field_notes"),
                "ECHO-0",
                "Living Route Worlds Field Notes",
                "OPEN",
                List.of(
                        "Route worlds use terminal surveys instead of Ashfall mission turn-ins because the problem is no longer obedience. It is evidence.",
                        "Each route has local landmarks, repair hooks, hazards, and recovery items that the Survey tab summarizes.",
                        "Terminal caches are optional support bundles; progression still comes from Orbital scans, repairs, bosses, and contracts.",
                        "After ECHO-0, every survey log is a small argument against quarantine: the route is dangerous, but it is not dead."),
                false));
    }
}
