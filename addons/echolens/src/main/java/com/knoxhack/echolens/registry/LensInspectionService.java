package com.knoxhack.echolens.registry;

import com.knoxhack.echolens.EchoLens;
import com.knoxhack.echolens.api.ILensInspectionService;
import com.knoxhack.echolens.api.LensAction;
import com.knoxhack.echolens.api.LensContext;
import com.knoxhack.echolens.api.LensDataCategory;
import com.knoxhack.echolens.api.LensInfoProvider;
import com.knoxhack.echolens.api.LensInfoRow;
import com.knoxhack.echolens.api.LensInfoSection;
import com.knoxhack.echolens.api.LensReport;
import com.knoxhack.echolens.api.LensScanMode;
import com.knoxhack.echolens.api.LensTargetKind;
import com.knoxhack.echolens.api.LensTone;
import com.knoxhack.echolens.api.LensVisibility;
import com.knoxhack.echolens.config.LensConfig;
import com.knoxhack.echolens.platform.LensModuleAccess;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public final class LensInspectionService implements ILensInspectionService {
    public static final LensInspectionService INSTANCE = new LensInspectionService();

    private LensInspectionService() {
    }

    @Override
    public LensReport inspect(LensContext context) {
        if (context == null || context.targetKind() == LensTargetKind.MISS) {
            return empty();
        }
        List<LensInfoSection> sections = new ArrayList<>();
        for (LensInfoProvider provider : LensProviderRegistry.providers()) {
            try {
                if (!provider.supports(context)) {
                    continue;
                }
                List<LensInfoSection> provided = provider.inspect(context);
                if (provided == null) {
                    continue;
                }
                LensProviderRegistry.recordProviderSuccess(provider);
                for (LensInfoSection section : LensReportSanitizer.normalizeLocal(provided)) {
                    if (section != null && includeSection(section, context.scanMode())) {
                        sections.add(trimSection(section, context.scanMode()));
                    }
                }
            } catch (RuntimeException exception) {
                LensProviderRegistry.recordProviderFailure(provider, exception);
                EchoLens.LOGGER.warn("Lens provider {} failed; continuing without its output.", provider.id(), exception);
            }
        }
        return new LensReport(title(context), subtitle(context), icon(context), context.targetKind(), targetId(context),
                targetNamespace(context), sections, actions(context));
    }

    public static List<LensInfoRow> visibleRows(LensInfoSection section, LensScanMode mode) {
        return section.rows().stream().filter(row -> row.visibleIn(mode)).toList();
    }

    private static LensReport empty() {
        return LensReport.empty();
    }

    private static boolean includeSection(LensInfoSection section, LensScanMode mode) {
        return section.visibleIn(mode) && categoryEnabled(section.category());
    }

    private static LensInfoSection trimSection(LensInfoSection section, LensScanMode mode) {
        int limit = switch (mode) {
            case COMPACT -> LensConfig.integer(LensConfig.COMPACT_ROW_LIMIT, 4);
            case EXPANDED -> LensConfig.integer(LensConfig.EXPANDED_ROW_LIMIT, 12);
            case DEEP -> LensConfig.integer(LensConfig.DEEP_ROW_LIMIT, 40);
        };
        List<LensInfoRow> rows = visibleRows(section, mode);
        if (rows.size() > limit) {
            rows = rows.subList(0, limit);
        }
        return new LensInfoSection(section.id(), section.category(), section.title(), section.icon(), section.tone(),
                section.visibility(), rows);
    }

    private static boolean categoryEnabled(LensDataCategory category) {
        return switch (category) {
            case IDENTITY -> LensConfig.bool(LensConfig.SHOW_IDENTITY, true);
            case BLOCK -> LensConfig.bool(LensConfig.SHOW_BLOCK, true);
            case ENTITY -> LensConfig.bool(LensConfig.SHOW_ENTITY, true);
            case FLUID -> LensConfig.bool(LensConfig.SHOW_FLUID, true);
            case MACHINE -> LensConfig.bool(LensConfig.SHOW_MACHINE, true);
            case INVENTORY -> LensConfig.bool(LensConfig.SHOW_INVENTORY, true);
            case INTEGRATION -> LensConfig.bool(LensConfig.SHOW_INTEGRATION, true);
            case HINTS -> LensConfig.bool(LensConfig.BEGINNER_HINTS, true);
            case ACTIONS -> LensConfig.bool(LensConfig.SHOW_ACTIONS, true);
        };
    }

    private static Component title(LensContext context) {
        if (context.hasEntity()) {
            return context.entity().getDisplayName();
        }
        if (context.hasBlock() && context.blockState().getBlock() != Blocks.AIR) {
            return context.blockState().getBlock().getName();
        }
        if (context.hasFluid()) {
            return Component.translatable(context.fluidState().getType().getFluidType().getDescriptionId());
        }
        return Component.literal("Unknown Target");
    }

    private static Component subtitle(LensContext context) {
        String namespace = targetNamespace(context);
        String displayName = LensModuleAccess.displayName(namespace);
        return Component.literal(displayName + " | " + context.targetKind().name().toLowerCase(Locale.ROOT));
    }

    private static ItemStack icon(LensContext context) {
        if (context.hasBlock()) {
            Block block = context.blockState().getBlock();
            return block == Blocks.AIR ? ItemStack.EMPTY : new ItemStack(block);
        }
        if (context.hasFluid()) {
            return context.fluidState().getType().getBucket().getDefaultInstance();
        }
        return ItemStack.EMPTY;
    }

    private static List<LensAction> actions(LensContext context) {
        if (!LensConfig.bool(LensConfig.SHOW_ACTIONS, true)) {
            return List.of();
        }
        ItemStack stack = icon(context);
        if (stack.isEmpty()) {
            return List.of();
        }
        boolean indexLoaded = LensModuleAccess.isLoaded("echoindex");
        String hint = indexLoaded ? "Open in ECHO: Index" : "Install ECHO: Index to enable this shortcut";
        return List.of(
                new LensAction(EchoLens.id("view_recipes"), Component.literal("Recipes"), Component.literal(hint),
                        "R", indexLoaded ? LensTone.ECHO : LensTone.MUTED, indexLoaded),
                new LensAction(EchoLens.id("view_uses"), Component.literal("Uses"), Component.literal(hint),
                        "U", indexLoaded ? LensTone.ECHO : LensTone.MUTED, indexLoaded),
                new LensAction(EchoLens.id("track_item"), Component.literal("Track"), Component.literal(hint),
                        "T", indexLoaded ? LensTone.GOOD : LensTone.MUTED, indexLoaded));
    }

    private static Identifier targetId(LensContext context) {
        if (context.hasEntity()) {
            return BuiltInRegistries.ENTITY_TYPE.getKey(context.entity().getType());
        }
        if (context.hasBlock()) {
            return BuiltInRegistries.BLOCK.getKey(context.blockState().getBlock());
        }
        if (context.hasFluid()) {
            return BuiltInRegistries.FLUID.getKey(context.fluidState().getType());
        }
        return EchoLens.id("unknown");
    }

    private static String targetNamespace(LensContext context) {
        Identifier id = targetId(context);
        return id == null ? "minecraft" : id.getNamespace();
    }
}
