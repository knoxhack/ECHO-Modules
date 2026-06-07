package com.knoxhack.echoholomap.api;

import com.knoxhack.echoholomap.map.HoloMapService;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class HoloMapApi {
    private HoloMapApi() {
    }

    public static boolean registerProvider(IHoloMapDataProvider provider) {
        return HoloMapService.INSTANCE.registerHoloProvider(provider);
    }

    public static List<HoloMapProviderDiagnostic> diagnostics(Player player) {
        return HoloMapService.INSTANCE.diagnostics(player);
    }

    public static boolean refresh(ServerPlayer player, String reason) {
        return HoloMapService.INSTANCE.refresh(player, reason);
    }
}
