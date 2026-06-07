package com.knoxhack.echomultiblockcore.api;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public interface MultiblockScanProvider {
    MultiblockScanProvider NOOP = (player, level, pos) -> Optional.empty();

    default Identifier providerId() {
        return MultiblockIntegrationServices.generatedProviderId(this, "scan");
    }

    Optional<LensMultiblockScan> scan(Player player, Level level, BlockPos pos);
}
