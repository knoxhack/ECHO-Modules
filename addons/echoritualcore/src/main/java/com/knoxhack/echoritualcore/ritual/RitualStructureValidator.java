package com.knoxhack.echoritualcore.ritual;

import com.knoxhack.echoritualcore.block.entity.OfferingPedestalBlockEntity;
import com.knoxhack.echoritualcore.registry.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public final class RitualStructureValidator {
    public static final int REQUIRED_RUNE_CIRCLES = 4;
    public static final int SEARCH_RADIUS = 4;

    private RitualStructureValidator() {
    }

    public static RitualStructureReport validate(Level level, BlockPos altarPos) {
        if (level == null || altarPos == null) {
            return new RitualStructureReport(BlockPos.ZERO, 0, 0, 0, 0, 0, 0, 0, List.of("altar"));
        }
        int runes = 0;
        int pedestals = 0;
        int pylons = 0;
        int moonDials = 0;
        int weatherAnchors = 0;
        int corruptedAltars = 0;
        for (BlockPos scanPos : BlockPos.betweenClosed(
                altarPos.offset(-SEARCH_RADIUS, -1, -SEARCH_RADIUS),
                altarPos.offset(SEARCH_RADIUS, 1, SEARCH_RADIUS))) {
            if (scanPos.equals(altarPos)) {
                continue;
            }
            Block block = level.getBlockState(scanPos).getBlock();
            if (block == ModBlocks.RUNE_CIRCLE.get()) {
                runes++;
            } else if (block == ModBlocks.OFFERING_PEDESTAL.get()) {
                pedestals++;
            } else if (block == ModBlocks.STABILITY_PYLON.get()) {
                pylons++;
            } else if (block == ModBlocks.MOON_DIAL.get()) {
                moonDials++;
            } else if (block == ModBlocks.WEATHER_ANCHOR.get()) {
                weatherAnchors++;
            } else if (block == ModBlocks.CORRUPTED_ALTAR.get()) {
                corruptedAltars++;
            }
        }
        List<String> missing = new ArrayList<>();
        if (runes < REQUIRED_RUNE_CIRCLES) {
            missing.add("rune_circles:" + (REQUIRED_RUNE_CIRCLES - runes));
        }
        if (pedestals <= 0) {
            missing.add("offering_pedestal");
        }
        int stability = 20 + Math.min(runes, 8) * 9 + Math.min(pedestals, 4) * 4
                + Math.min(pylons, 4) * 8 + Math.min(moonDials + weatherAnchors, 2) * 4
                - Math.min(corruptedAltars, 4) * 15;
        return new RitualStructureReport(altarPos, runes, pedestals, pylons, moonDials, weatherAnchors,
                corruptedAltars, stability, missing);
    }

    public static List<OfferingPedestalBlockEntity> pedestals(Level level, BlockPos altarPos) {
        if (level == null || altarPos == null) {
            return List.of();
        }
        List<OfferingPedestalBlockEntity> pedestals = new ArrayList<>();
        for (BlockPos scanPos : BlockPos.betweenClosed(
                altarPos.offset(-SEARCH_RADIUS, -1, -SEARCH_RADIUS),
                altarPos.offset(SEARCH_RADIUS, 1, SEARCH_RADIUS))) {
            if (level.getBlockEntity(scanPos) instanceof OfferingPedestalBlockEntity pedestal) {
                pedestals.add(pedestal);
            }
        }
        return List.copyOf(pedestals);
    }
}
