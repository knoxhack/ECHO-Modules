package com.knoxhack.echoritualcore.integration.lens;

import com.knoxhack.echolens.api.LensContext;
import com.knoxhack.echolens.api.LensDataCategory;
import com.knoxhack.echolens.api.LensInfoRow;
import com.knoxhack.echolens.api.LensInfoSection;
import com.knoxhack.echolens.api.LensTone;
import com.knoxhack.echolens.api.LensVisibility;
import com.knoxhack.echolens.api.ServerLensProvider;
import com.knoxhack.echoritualcore.EchoRitualCore;
import com.knoxhack.echoritualcore.block.entity.BasicAltarBlockEntity;
import com.knoxhack.echoritualcore.block.entity.OfferingPedestalBlockEntity;
import com.knoxhack.echoritualcore.registry.ModBlocks;
import com.knoxhack.echoritualcore.ritual.RitualStructureReport;
import com.knoxhack.echoritualcore.ritual.RitualStructureValidator;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

enum RitualCoreLensProvider implements ServerLensProvider {
    INSTANCE;

    @Override
    public Identifier id() {
        return EchoRitualCore.id("ritualcore_scan");
    }

    @Override
    public int priority() {
        return 85;
    }

    @Override
    public LensDataCategory category() {
        return LensDataCategory.INTEGRATION;
    }

    @Override
    public boolean supports(LensContext context) {
        if (context == null || !context.hasBlock()) {
            return false;
        }
        Block block = context.blockState().getBlock();
        return block == ModBlocks.BASIC_ALTAR.get()
                || block == ModBlocks.OFFERING_PEDESTAL.get()
                || block == ModBlocks.RUNE_CIRCLE.get()
                || block == ModBlocks.STABILITY_PYLON.get()
                || block == ModBlocks.MOON_DIAL.get()
                || block == ModBlocks.WEATHER_ANCHOR.get()
                || block == ModBlocks.CORRUPTED_ALTAR.get()
                || block == ModBlocks.RITUAL_BASIN.get();
    }

    @Override
    public List<LensInfoSection> inspect(LensContext context) {
        List<LensInfoRow> rows = new ArrayList<>();
        Block block = context.blockState().getBlock();
        if (block == ModBlocks.BASIC_ALTAR.get()) {
            RitualStructureReport report = RitualStructureValidator.validate(context.level(), context.blockPos());
            rows.add(row("Array", report.validBasicArray() ? "ready" : "incomplete", "A",
                    report.validBasicArray() ? LensTone.GOOD : LensTone.WARNING, LensVisibility.COMPACT));
            rows.add(row("Runes", report.runeCircles() + "/" + RitualStructureValidator.REQUIRED_RUNE_CIRCLES, "R",
                    report.runeCircles() >= RitualStructureValidator.REQUIRED_RUNE_CIRCLES ? LensTone.GOOD : LensTone.WARNING,
                    LensVisibility.COMPACT));
            rows.add(row("Pedestals", Integer.toString(report.pedestalCount()), "P",
                    report.pedestalCount() > 0 ? LensTone.GOOD : LensTone.WARNING, LensVisibility.COMPACT));
            rows.add(row("Stability", report.stabilityScore() + "%", "S",
                    report.stabilityScore() >= 70 ? LensTone.GOOD : LensTone.INFO, LensVisibility.COMPACT));
            BlockEntity be = context.level().getBlockEntity(context.blockPos());
            if (be instanceof BasicAltarBlockEntity altar) {
                rows.add(row("Last", altar.lastMessage(), "L", toneForResult(altar.lastResult()), LensVisibility.EXPANDED));
            }
            if (!report.validBasicArray()) {
                rows.add(row("Missing", String.join(", ", report.missingAnchors()), "!", LensTone.WARNING,
                        LensVisibility.EXPANDED));
            }
        } else if (block == ModBlocks.OFFERING_PEDESTAL.get()) {
            BlockEntity be = context.level().getBlockEntity(context.blockPos());
            String item = be instanceof OfferingPedestalBlockEntity pedestal && !pedestal.displayStack().isEmpty()
                    ? pedestal.displayStack().getHoverName().getString()
                    : "empty";
            rows.add(row("Input", item, "I", "empty".equals(item) ? LensTone.WARNING : LensTone.GOOD,
                    LensVisibility.COMPACT));
            rows.add(row("Network", "searched by nearby Basic Altars", "N", LensTone.INFO, LensVisibility.EXPANDED));
        } else {
            rows.add(row("Role", role(block), "R", LensTone.INFO, LensVisibility.COMPACT));
            rows.add(row("Array", "scan Basic Altar for complete diagnostics", "A", LensTone.MUTED,
                    LensVisibility.EXPANDED));
        }
        return List.of(LensInfoSection.of(
                EchoRitualCore.id("lens/ritualcore"),
                LensDataCategory.INTEGRATION,
                "ECHO RitualCore",
                "R",
                rows.stream().anyMatch(row -> row.tone() == LensTone.WARNING || row.tone() == LensTone.DANGER)
                        ? LensTone.WARNING : LensTone.ECHO,
                LensVisibility.COMPACT,
                rows));
    }

    private static String role(Block block) {
        if (block == ModBlocks.RUNE_CIRCLE.get()) {
            return "structure circuit";
        }
        if (block == ModBlocks.STABILITY_PYLON.get()) {
            return "stability support";
        }
        if (block == ModBlocks.MOON_DIAL.get()) {
            return "time augment";
        }
        if (block == ModBlocks.WEATHER_ANCHOR.get()) {
            return "weather augment";
        }
        if (block == ModBlocks.CORRUPTED_ALTAR.get()) {
            return "forbidden center";
        }
        if (block == ModBlocks.RITUAL_BASIN.get()) {
            return "mid-tier center bridge";
        }
        return "ritual block";
    }

    private static LensTone toneForResult(int result) {
        return switch (result) {
            case BasicAltarBlockEntity.RESULT_COMPLETE, BasicAltarBlockEntity.RESULT_READY -> LensTone.GOOD;
            case BasicAltarBlockEntity.RESULT_WARNING -> LensTone.WARNING;
            case BasicAltarBlockEntity.RESULT_FAILURE -> LensTone.DANGER;
            default -> LensTone.MUTED;
        };
    }

    private static LensInfoRow row(String label, String value, String icon, LensTone tone, LensVisibility visibility) {
        return LensInfoRow.of(label, value, icon, tone, visibility);
    }
}
