package com.knoxhack.echo.atmospherecore;

import com.knoxhack.echo.adaptercore.EchoNativeAtmosphereRuntimeProfileBridge;
import com.knoxhack.echo.adaptercore.EchoWorldContracts;
import java.util.List;
import java.util.Map;

public final class EchoAtmosphereRuntimeProfileContract {
    public static final String MODULE_ID = "echoatmospherecore";
    public static final String ADAPTERCORE_CONTRACT_ID = "echoatmospherecore:atmosphere/runtime_profile_tick";
    public static final String REFERENCE_PROFILE_ID = "echoatmospherecore:profile/ashfall_storm_visibility";
    public static final String REFERENCE_WEATHER_STATE_ID = "echoweathercore:weather/ash_storm_active";

    private EchoAtmosphereRuntimeProfileContract() {
    }

    public static Map<String, Object> executeReferenceProfileTick(String packId) {
        EchoWorldContracts.EchoAtmosphereRuntimeProfileResult result =
                new EchoNativeAtmosphereRuntimeProfileBridge(MODULE_ID).materialize(referenceProfileRequest(
                        packId,
                        0L,
                        "echoatmospherecore-reference-profile-tick"));
        return result.runtimeProfileState();
    }

    public static EchoWorldContracts.EchoAtmosphereRuntimeProfileRequest referenceProfileRequest(
            String packId,
            long gameTick,
            String sourceReason) {
        return new EchoWorldContracts.EchoAtmosphereRuntimeProfileRequest(
                packId == null || packId.isBlank() ? "unknown" : packId,
                REFERENCE_PROFILE_ID,
                REFERENCE_WEATHER_STATE_ID,
                "echoashfallprotocol:ambience/wasteland_surface",
                0.82D,
                0.31D,
                0.66D,
                true,
                "echoatmospherecore:fog/ashfall_active",
                -9263400,
                0.58D,
                6.0D,
                72.0D,
                true,
                "echoatmospherecore:sky_tint/ashfall_active",
                -6313816,
                -11905975,
                -10274248,
                0.24D,
                "echoatmospherecore:ambient_particles/ashfall_active",
                List.of(
                        "echoashfallprotocol:particle/fine_ash",
                        "echoashfallprotocol:particle/ember_trace"
                ),
                0.64D,
                true,
                "echorendercore:hook/atmosphere_fog_sky",
                "echosoundcore:ambience/ash_storm",
                "echoweathercore:weather_profiles/ash_storm",
                "echoatmospherecore:ashfall_runtime_packet_consumers",
                Math.max(0L, gameTick),
                sourceReason);
    }

    public static boolean referenceProfileTickPassed(Map<String, Object> tick) {
        return Boolean.TRUE.equals(tick.get("atmosphereProfileTickExecuted"))
                && ADAPTERCORE_CONTRACT_ID.equals(tick.get("adapterCoreContract"))
                && REFERENCE_PROFILE_ID.equals(tick.get("profileId"))
                && REFERENCE_WEATHER_STATE_ID.equals(tick.get("weatherStateId"))
                && String.valueOf(tick.get("stormVisibility")).contains("stormVisibility=0.31")
                && String.valueOf(tick.get("fogProfile")).contains("density=0.58")
                && String.valueOf(tick.get("skyTint")).contains("celestialVisibility=0.24")
                && String.valueOf(tick.get("ambientParticles")).contains("echoashfallprotocol:particle/fine_ash")
                && String.valueOf(tick.get("hookRefs")).contains("echorendercore:hook/atmosphere_fog_sky")
                && String.valueOf(tick.get("runtimeBindings")).contains("sound.ambience")
                && String.valueOf(tick.get("diagnostics")).contains("atmosphere.fog_sky.bound");
    }

}
