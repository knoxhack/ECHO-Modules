package com.knoxhack.echoholomap.map;

import com.knoxhack.echoholomap.EchoHoloMap;
import com.knoxhack.echoholomap.api.HoloMapChunkActionResult;
import com.knoxhack.echoholomap.api.HoloMapChunkSelection;
import com.knoxhack.echoholomap.api.IHoloMapChunkActionProvider;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class HoloMapChunkActions {
    private static final Map<Identifier, IHoloMapChunkActionProvider> PROVIDERS = new LinkedHashMap<>();

    private HoloMapChunkActions() {
    }

    public static synchronized boolean register(IHoloMapChunkActionProvider provider) {
        if (provider == null || provider.providerId() == null || PROVIDERS.containsKey(provider.providerId())) {
            return false;
        }
        PROVIDERS.put(provider.providerId(), provider);
        return true;
    }

    public static HoloMapChunkActionResult handle(ServerPlayer player, HoloMapChunkSelection selection,
            Identifier providerId, Identifier actionId) {
        IHoloMapChunkActionProvider provider;
        synchronized (HoloMapChunkActions.class) {
            provider = PROVIDERS.get(providerId);
        }
        if (provider == null) {
            return HoloMapChunkActionResult.failure("Action Unavailable",
                    "No HoloMap chunk action provider is registered for " + providerId + ".");
        }
        try {
            HoloMapChunkActionResult result = provider.handle(player, selection, actionId);
            return result == null
                    ? HoloMapChunkActionResult.failure("Action Failed", "The chunk action returned no result.")
                    : result;
        } catch (RuntimeException exception) {
            EchoHoloMap.LOGGER.warn("HoloMap chunk action {} from {} failed.", actionId, providerId, exception);
            return HoloMapChunkActionResult.failure("Action Failed", "The HoloMap chunk action failed.");
        }
    }

    public static synchronized void clearForTests() {
        PROVIDERS.clear();
    }
}
