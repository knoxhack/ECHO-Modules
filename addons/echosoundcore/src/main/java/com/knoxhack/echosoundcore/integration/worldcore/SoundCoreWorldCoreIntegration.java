package com.knoxhack.echosoundcore.integration.worldcore;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoWorldRuntimeBus;
import com.knoxhack.echocore.api.WorldHazardSnapshot;
import com.knoxhack.echocore.api.WorldRegionInstance;
import com.knoxhack.echosoundcore.EchoSoundCore;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;

public final class SoundCoreWorldCoreIntegration {
    private static boolean registered;

    private SoundCoreWorldCoreIntegration() {}

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        EchoSoundCore.LOGGER.info("SoundCore WorldCore integration registered.");
        EchoWorldRuntimeBus.onRegionEntered(SoundCoreWorldCoreIntegration::onRegionEntered);
        EchoWorldRuntimeBus.onHazardChanged(SoundCoreWorldCoreIntegration::onHazardChanged);
    }

    private static void onRegionEntered(EchoWorldRuntimeBus.RegionEntered event) {
        WorldRegionInstance region = event.region();
        if (event.player() == null || region == null) {
            return;
        }
        Map<String, String> patch = new LinkedHashMap<>();
        patch.put("region", region.definitionId().toString());
        patch.put("structure", region.id().toString());
        patch.put("hazardLevel", Integer.toString(region.hazardIds().isEmpty() ? 0 : 1));
        EchoCoreServices.soundService().patchContext(event.player(), patch);
    }

    private static void onHazardChanged(EchoWorldRuntimeBus.HazardChanged event) {
        WorldHazardSnapshot current = event.current();
        if (event.player() == null || current == null) {
            return;
        }
        Map<String, String> patch = new LinkedHashMap<>();
        patch.put("hazardLevel", Integer.toString(current.safeZone() ? 0 : Math.max(1, current.severity() / 34)));
        patch.put("world_hazard_safe", Boolean.toString(current.safeZone()));
        patch.put("world_hazards", current.hazardIds().stream()
                .map(Identifier::toString)
                .reduce((left, right) -> left + "," + right)
                .orElse(""));
        if (!current.regionIds().isEmpty()) {
            patch.put("region", current.regionIds().getFirst().toString());
        }
        EchoCoreServices.soundService().patchContext(event.player(), patch);
    }
}
