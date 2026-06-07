package com.knoxhack.signalos.api;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public interface SignalOsPeripheralProvider {
    Identifier id();

    List<Peripheral> peripherals(Player player);

    default int order() {
        return 0;
    }

    default SignalOsProviderStatus providerStatus(Player player) {
        return SignalOsProviderStatus.online(id(), "Peripheral Provider");
    }

    record Peripheral(Identifier id, String kind, String label, String status, BlockPos pos, int tier) {
        public Peripheral {
            id = TerminalIds.requireLowercase(id, "SignalOS peripheral");
            kind = kind == null || kind.isBlank() ? "device" : kind.strip().toLowerCase(java.util.Locale.ROOT);
            label = label == null || label.isBlank() ? id.getPath() : label.strip();
            status = status == null || status.isBlank() ? "ONLINE" : status.strip().toUpperCase(java.util.Locale.ROOT);
            tier = Math.max(0, tier);
        }
    }
}
