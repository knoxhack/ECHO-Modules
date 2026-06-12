package com.echoplatform.echocore.api;

import java.util.function.Function;
import net.minecraft.world.entity.player.Player;

@FunctionalInterface
public interface EchoHazardTelemetryService extends Function<Player, EchoHazardTelemetry> {
    EchoHazardTelemetry telemetry(Player player);

    @Override
    default EchoHazardTelemetry apply(Player player) {
        EchoHazardTelemetry telemetry = telemetry(player);
        return telemetry == null ? EchoHazardTelemetry.nominal() : telemetry;
    }
}
