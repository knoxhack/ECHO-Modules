package com.knoxhack.echo.atmospherecore;

import com.knoxhack.echo.adaptercore.EchoNativeAtmosphereRuntimeProfileBridge;
import com.knoxhack.echo.adaptercore.EchoNativeAtmosphereStateApplyBridge;
import com.knoxhack.echo.adaptercore.EchoWorldContracts;

import java.util.Map;

public final class EchoAtmosphereRuntimeState {
    private static final String MODULE_ID = "echoatmospherecore";
    private static volatile LiveAtmosphereTickState activeAtmosphereTick = LiveAtmosphereTickState.empty();

    private EchoAtmosphereRuntimeState() {
    }

    public static LiveAtmosphereTickState activeAtmosphereTick() {
        return activeAtmosphereTick;
    }

    public static synchronized LiveAtmosphereTickState materializeLevelTick(long gameTick, String sourceReason) {
        String source = sourceReason == null || sourceReason.isBlank() ? "adaptercore.level_tick" : sourceReason.strip();
        EchoWorldContracts.EchoAtmosphereRuntimeProfileResult profileResult =
                new EchoNativeAtmosphereRuntimeProfileBridge(MODULE_ID).materialize(
                        EchoAtmosphereRuntimeProfileContract.referenceProfileRequest(
                                "ashfall",
                                Math.max(0L, gameTick),
                                source));
        EchoWorldContracts.EchoAtmosphereStateApplyResult stateApply =
                new EchoNativeAtmosphereStateApplyBridge(MODULE_ID).apply(referenceStateApplyRequest(gameTick, source));
        LiveAtmosphereTickState state = new LiveAtmosphereTickState(
                profileResult.runtimeProfileState(),
                stateApply);
        activeAtmosphereTick = state;
        return state;
    }

    private static EchoWorldContracts.EchoAtmosphereStateApplyRequest referenceStateApplyRequest(
            long gameTick,
            String sourceReason) {
        return new EchoWorldContracts.EchoAtmosphereStateApplyRequest(
                "echoashfallprotocol:event/ash_storm",
                EchoAtmosphereRuntimeProfileContract.REFERENCE_WEATHER_STATE_ID,
                "echoashfallprotocol:crash_zone_wasteland",
                "ACTIVE",
                Math.max(0L, gameTick),
                sourceReason,
                new EchoWorldContracts.EchoAtmosphereState(
                        "echoatmospherecore:ash_storm_field",
                        0.31D,
                        "minecraft:ash",
                        "fog_color:9069905"));
    }

    public record LiveAtmosphereTickState(
            Map<String, Object> runtimeProfileState,
            EchoWorldContracts.EchoAtmosphereStateApplyResult stateApply
    ) {
        public static LiveAtmosphereTickState empty() {
            return new LiveAtmosphereTickState(Map.of(), null);
        }

        public boolean materialized() {
            return EchoAtmosphereRuntimeProfileContract.referenceProfileTickPassed(runtimeProfileState)
                    && stateApply != null
                    && stateApply.applied()
                    && "ACTIVE".equals(stateApply.phase())
                    && Double.valueOf(0.31D).equals(stateApply.renderState().get("visibility"))
                    && "minecraft:ash".equals(stateApply.renderState().get("particles"))
                    && "fog_color:9069905".equals(stateApply.renderState().get("skyFog"));
        }
    }
}
