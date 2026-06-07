package com.knoxhack.echo.biomecore;

import com.knoxhack.echo.adaptercore.EchoNativeBiomeAmbientStateBridge;
import com.knoxhack.echo.adaptercore.EchoNativeBiomeHazardOverlayBridge;
import com.knoxhack.echo.adaptercore.EchoWorldContracts;

import java.util.List;

public final class EchoBiomeRuntimeState {
    private static volatile LiveBiomeTickState activeBiomeTick = LiveBiomeTickState.empty();

    private EchoBiomeRuntimeState() {
    }

    public static LiveBiomeTickState activeBiomeTick() {
        return activeBiomeTick;
    }

    public static synchronized LiveBiomeTickState materializeLevelTick(long gameTick, String sourceReason) {
        String source = sourceReason == null || sourceReason.isBlank() ? "echo_native.level_tick" : sourceReason.strip();
        EchoWorldContracts.EchoBiomeAmbientStateResult ambientState =
                new EchoNativeBiomeAmbientStateBridge(EchoBiomeConstants.MOD_ID).apply(referenceAmbientStateRequest(gameTick, source));
        EchoWorldContracts.EchoBiomeHazardOverlayResult hazardOverlay =
                new EchoNativeBiomeHazardOverlayBridge(EchoBiomeConstants.MOD_ID).resolve(referenceHazardOverlayRequest(gameTick, source));
        LiveBiomeTickState state = new LiveBiomeTickState(ambientState, hazardOverlay);
        activeBiomeTick = state;
        return state;
    }

    private static EchoWorldContracts.EchoBiomeAmbientStateRequest referenceAmbientStateRequest(long gameTick, String sourceReason) {
        return new EchoWorldContracts.EchoBiomeAmbientStateRequest(
                "biomecore-live-player",
                "echoashfallprotocol:crash_zone_wasteland",
                "#echoashfallprotocol:common_wasteland_biomes",
                "echoashfallprotocol:ambience/crash_zone_wasteland",
                "echosoundcore:ambience/wasteland_wind",
                "echoparticlecore:ambient/ash_drift",
                List.of("echobiomecore:ambient/ash_haze", "echobiomecore:ambient/scrap_glints"),
                "echoatmospherecore:ash_storm_field",
                0.74D,
                Math.max(0L, gameTick),
                sourceReason);
    }

    private static EchoWorldContracts.EchoBiomeHazardOverlayRequest referenceHazardOverlayRequest(long gameTick, String sourceReason) {
        return new EchoWorldContracts.EchoBiomeHazardOverlayRequest(
                "biomecore-live-player",
                "minecraft:overworld",
                32,
                68,
                32,
                Math.max(0L, gameTick),
                sourceReason,
                new EchoWorldContracts.EchoBiomeProfile(
                        "echoashfallprotocol:crash_zone_wasteland",
                        "#echoashfallprotocol:common_wasteland_biomes",
                        "#echoworldcore:hazards/salvage_debris"),
                new EchoWorldContracts.EchoWorldHazard(
                        "echoworldcore:hazard/salvage_debris",
                        "debris",
                        32,
                        32,
                        12,
                        2.0D,
                        "echostatuscore:status/salvage_debris"),
                true,
                true);
    }

    public record LiveBiomeTickState(
            EchoWorldContracts.EchoBiomeAmbientStateResult ambientState,
            EchoWorldContracts.EchoBiomeHazardOverlayResult hazardOverlay
    ) {
        public static LiveBiomeTickState empty() {
            return new LiveBiomeTickState(null, null);
        }

        public boolean materialized() {
            return ambientState != null
                    && ambientState.applied()
                    && "echosoundcore:ambience/wasteland_wind".equals(ambientState.audioState().get("cue"))
                    && hazardOverlay != null
                    && hazardOverlay.active()
                    && Double.valueOf(2.0D).equals(hazardOverlay.intensity());
        }
    }
}
