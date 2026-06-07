package com.knoxhack.echolens.provider;

import com.knoxhack.echolens.EchoLens;
import com.knoxhack.echolens.api.BlockLensProvider;
import com.knoxhack.echolens.api.LensContext;
import com.knoxhack.echolens.api.LensDataCategory;
import com.knoxhack.echolens.api.LensInfoRow;
import com.knoxhack.echolens.api.LensInfoSection;
import com.knoxhack.echolens.api.LensTone;
import com.knoxhack.echolens.api.LensVisibility;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public enum BlockStatsProvider implements BlockLensProvider {
    INSTANCE;

    @Override
    public Identifier id() {
        return EchoLens.id("block_stats");
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public LensDataCategory category() {
        return LensDataCategory.BLOCK;
    }

    @Override
    public List<LensInfoSection> inspectBlock(LensContext context, BlockState state, BlockPos pos) {
        if (state.isAir()) {
            return List.of();
        }
        Level level = context.level();
        ItemStack held = context.player() == null ? ItemStack.EMPTY : context.player().getMainHandItem();
        boolean requiresTool = safeRequiresTool(state);
        boolean canHarvest = !requiresTool || (!held.isEmpty() && safeCorrectTool(held, state));
        LensTone harvestTone = canHarvest ? LensTone.GOOD : LensTone.WARNING;
        List<LensInfoRow> rows = new ArrayList<>();
        rows.add(LensInfoRow.of("Tool", requiredTool(state), "⛏", LensTone.INFO, LensVisibility.COMPACT));
        rows.add(LensInfoRow.of("Harvest", canHarvest ? "Ready" : "Wrong or missing tool", canHarvest ? "✓" : "!",
                harvestTone, LensVisibility.COMPACT));
        rows.add(LensInfoRow.of("Hardness", formatFloat(safeDestroySpeed(state, level, pos)), "◼",
                LensTone.NEUTRAL, LensVisibility.EXPANDED));
        rows.add(LensInfoRow.of("Blast", formatFloat(safeBlastResistance(state.getBlock())), "✦",
                LensTone.NEUTRAL, LensVisibility.EXPANDED));
        rows.add(LensInfoRow.of("Light", Integer.toString(safeLight(level, pos, state)), "☼",
                LensTone.INFO, LensVisibility.EXPANDED));
        rows.add(LensInfoRow.of("Redstone", Integer.toString(safeRedstone(level, pos)), "⎍",
                safeRedstone(level, pos) > 0 ? LensTone.GOOD : LensTone.MUTED, LensVisibility.EXPANDED));
        rows.add(LensInfoRow.of("Fluid", state.getFluidState().isEmpty() ? "None" : "Present", "F",
                state.getFluidState().isEmpty() ? LensTone.MUTED : LensTone.INFO, LensVisibility.EXPANDED));
        rows.add(LensInfoRow.of("Collision", safeCollision(level, pos, state) ? "Solid" : "Passable", "C",
                safeCollision(level, pos, state) ? LensTone.NEUTRAL : LensTone.INFO, LensVisibility.DEEP));
        return List.of(LensInfoSection.of(EchoLens.id("section/block"), LensDataCategory.BLOCK, "Block",
                "▧", LensTone.NEUTRAL, LensVisibility.COMPACT, rows));
    }

    private static String requiredTool(BlockState state) {
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            return "Pickaxe";
        }
        if (state.is(BlockTags.MINEABLE_WITH_AXE)) {
            return "Axe";
        }
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            return "Shovel";
        }
        if (state.is(BlockTags.MINEABLE_WITH_HOE)) {
            return "Hoe";
        }
        return safeRequiresTool(state) ? "Correct tool" : "Any tool";
    }

    private static boolean safeRequiresTool(BlockState state) {
        try {
            Method method = BlockState.class.getMethod("requiresCorrectToolForDrops");
            return (boolean) method.invoke(state);
        } catch (ReflectiveOperationException | ClassCastException exception) {
            return false;
        }
    }

    private static boolean safeCorrectTool(ItemStack stack, BlockState state) {
        try {
            Method method = ItemStack.class.getMethod("isCorrectToolForDrops", BlockState.class);
            return (boolean) method.invoke(stack, state);
        } catch (ReflectiveOperationException | ClassCastException exception) {
            return !safeRequiresTool(state);
        }
    }

    private static float safeDestroySpeed(BlockState state, Level level, BlockPos pos) {
        try {
            Method method = BlockState.class.getMethod("getDestroySpeed", net.minecraft.world.level.BlockGetter.class,
                    BlockPos.class);
            Object result = method.invoke(state, level, pos);
            return result instanceof Number number ? number.floatValue() : -1.0F;
        } catch (ReflectiveOperationException exception) {
            return -1.0F;
        }
    }

    private static float safeBlastResistance(Block block) {
        try {
            Method method = Block.class.getMethod("getExplosionResistance");
            Object result = method.invoke(block);
            return result instanceof Number number ? number.floatValue() : -1.0F;
        } catch (ReflectiveOperationException exception) {
            return -1.0F;
        }
    }

    private static int safeLight(Level level, BlockPos pos, BlockState state) {
        try {
            Method method = BlockState.class.getMethod("getLightEmission");
            Object result = method.invoke(state);
            if (result instanceof Number number) {
                return number.intValue();
            }
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Method method = Level.class.getMethod("getRawBrightness", BlockPos.class, int.class);
            Object result = method.invoke(level, pos, 0);
            return result instanceof Number number ? number.intValue() : 0;
        } catch (ReflectiveOperationException exception) {
            return 0;
        }
    }

    private static int safeRedstone(Level level, BlockPos pos) {
        try {
            return level.getBestNeighborSignal(pos);
        } catch (RuntimeException exception) {
            return 0;
        }
    }

    private static boolean safeCollision(Level level, BlockPos pos, BlockState state) {
        try {
            return !state.getCollisionShape(level, pos).isEmpty();
        } catch (RuntimeException exception) {
            return true;
        }
    }

    private static String formatFloat(float value) {
        if (value < 0.0F) {
            return "Unknown";
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
