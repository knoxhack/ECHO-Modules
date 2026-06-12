package com.echoplatform.echocore.api;

import java.util.List;
import java.util.function.Function;
import net.minecraft.world.entity.player.Player;

@FunctionalInterface
public interface EchoRouteRecordService extends Function<Player, List<EchoRouteRecord>> {
    List<EchoRouteRecord> routeRecords(Player player);

    @Override
    default List<EchoRouteRecord> apply(Player player) {
        return routeRecords(player);
    }
}
