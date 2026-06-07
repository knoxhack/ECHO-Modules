package com.knoxhack.echoholomap.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.resources.Identifier;

public final class HoloMapClientChunkActions {
    private static final List<IHoloMapClientChunkActionProvider> PROVIDERS = new CopyOnWriteArrayList<>();

    private HoloMapClientChunkActions() {
    }

    public static boolean register(IHoloMapClientChunkActionProvider provider) {
        if (provider == null || provider.providerId() == null) {
            return false;
        }
        for (IHoloMapClientChunkActionProvider existing : PROVIDERS) {
            if (provider.providerId().equals(existing.providerId())) {
                return false;
            }
        }
        PROVIDERS.add(provider);
        return true;
    }

    public static List<HoloMapChunkMenuAction> actions(String dimension, int chunkX, int chunkZ) {
        ArrayList<HoloMapChunkMenuAction> actions = new ArrayList<>();
        for (IHoloMapClientChunkActionProvider provider : PROVIDERS) {
            try {
                List<HoloMapChunkMenuAction> provided = provider.actions(dimension, chunkX, chunkZ);
                if (provided != null) {
                    actions.addAll(provided.stream().filter(action -> action != null).toList());
                }
            } catch (RuntimeException ignored) {
                // Keep the action menu usable if an optional provider has stale client state.
            }
        }
        return List.copyOf(actions);
    }

    public static HoloMapChunkMenuAction action(Identifier menuId, String dimension, int chunkX, int chunkZ) {
        if (menuId == null) {
            return null;
        }
        for (HoloMapChunkMenuAction action : actions(dimension, chunkX, chunkZ)) {
            if (menuId.equals(action.menuId())) {
                return action;
            }
        }
        return null;
    }

    public static void clearForTests() {
        PROVIDERS.clear();
    }
}
