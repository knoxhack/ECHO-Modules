package com.echoplatform.echocore.api;

import java.util.List;
import java.util.function.Function;
import net.minecraft.world.entity.player.Player;

public interface EchoDiagnosticService extends Function<Player, List<EchoDiagnosticBlocker>> {
    List<EchoDiagnosticBlocker> diagnostics(Player player);

    @Override
    default List<EchoDiagnosticBlocker> apply(Player player) {
        return diagnostics(player);
    }
}
