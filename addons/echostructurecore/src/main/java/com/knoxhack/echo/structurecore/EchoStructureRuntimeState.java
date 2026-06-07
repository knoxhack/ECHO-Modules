package com.knoxhack.echo.structurecore;

import com.knoxhack.echo.adaptercore.EchoNativeStructureDiscoveryStateBridge;
import com.knoxhack.echo.adaptercore.EchoNativeStructurePoiLookupBridge;
import com.knoxhack.echo.adaptercore.EchoNativeStructurePoiMarkerStateBridge;
import com.knoxhack.echo.adaptercore.EchoWorldContracts;

public final class EchoStructureRuntimeState {
    private static final String MODULE_ID = "echostructurecore";
    private static volatile LiveStructureTickState activeStructureTick = LiveStructureTickState.empty();

    private EchoStructureRuntimeState() {
    }

    public static LiveStructureTickState activeStructureTick() {
        return activeStructureTick;
    }

    public static synchronized LiveStructureTickState materializeLevelTick(long gameTick, String sourceReason) {
        String source = sourceReason == null || sourceReason.isBlank() ? "echo_native.level_tick" : sourceReason.strip();
        EchoWorldContracts.EchoStructurePlacement structure = new EchoWorldContracts.EchoStructurePlacement(
                "echoashfallprotocol:drop_pod",
                "echoashfallprotocol:poi/drop_pod",
                30,
                68,
                30);
        EchoWorldContracts.EchoStructurePoiLookupResult lookup =
                new EchoNativeStructurePoiLookupBridge(MODULE_ID).lookup(
                        new EchoWorldContracts.EchoStructurePoiLookupRequest(
                                "structurecore-live-player",
                                "echoashfallprotocol:crash_zone_wasteland",
                                32,
                                68,
                                32,
                                8,
                                Math.max(0L, gameTick),
                                source,
                                structure));
        EchoWorldContracts.EchoStructurePoiMarkerStateResult marker =
                new EchoNativeStructurePoiMarkerStateBridge(MODULE_ID).persist(
                        new EchoWorldContracts.EchoStructurePoiMarkerStateRequest(
                                lookup.playerId(),
                                "structurecore-live-marker",
                                lookup));
        EchoWorldContracts.EchoStructureDiscoveryStateResult discovery =
                new EchoNativeStructureDiscoveryStateBridge(MODULE_ID).discover(
                        new EchoWorldContracts.EchoStructureDiscoveryStateRequest(
                                marker.playerId(),
                                "structurecore-live-discovery",
                                marker));
        LiveStructureTickState state = new LiveStructureTickState(lookup, marker, discovery);
        activeStructureTick = state;
        return state;
    }

    public record LiveStructureTickState(
            EchoWorldContracts.EchoStructurePoiLookupResult lookup,
            EchoWorldContracts.EchoStructurePoiMarkerStateResult marker,
            EchoWorldContracts.EchoStructureDiscoveryStateResult discovery
    ) {
        public static LiveStructureTickState empty() {
            return new LiveStructureTickState(null, null, null);
        }

        public boolean materialized() {
            return lookup != null
                    && lookup.inRange()
                    && marker != null
                    && marker.markerPersisted()
                    && discovery != null
                    && discovery.discovered()
                    && discovery.holomapMarkerActive();
        }
    }
}
