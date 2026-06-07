package com.knoxhack.echolens.provider;

import com.knoxhack.echolens.EchoLens;
import com.knoxhack.echolens.api.LensContext;
import com.knoxhack.echolens.api.LensDataCategory;
import com.knoxhack.echolens.api.LensInfoProvider;
import com.knoxhack.echolens.api.LensInfoRow;
import com.knoxhack.echolens.api.LensInfoSection;
import com.knoxhack.echolens.api.LensTone;
import com.knoxhack.echolens.api.LensVisibility;
import com.knoxhack.echolens.config.LensConfig;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public enum BeginnerHintProvider implements LensInfoProvider {
    INSTANCE;

    @Override
    public Identifier id() {
        return EchoLens.id("beginner_hints");
    }

    @Override
    public int priority() {
        return 900;
    }

    @Override
    public LensDataCategory category() {
        return LensDataCategory.HINTS;
    }

    @Override
    public boolean supports(LensContext context) {
        return LensConfig.bool(LensConfig.BEGINNER_HINTS, true) && context != null;
    }

    @Override
    public List<LensInfoSection> inspect(LensContext context) {
        List<LensInfoRow> rows = new ArrayList<>();
        if (context.hasBlock()) {
            BlockState state = context.blockState();
            ItemStack held = context.player() == null ? ItemStack.EMPTY : context.player().getMainHandItem();
            if (safeRequiresTool(state) && (held.isEmpty() || !safeCorrectTool(held, state))) {
                rows.add(LensInfoRow.of("Problem", "Use a " + requiredTool(state).toLowerCase() + " to harvest drops.",
                        "!", LensTone.WARNING, LensVisibility.COMPACT));
            }
            if (context.level().getBlockEntity(context.blockPos()) != null) {
                rows.add(LensInfoRow.of("Machine", "If output stalls, check power, input, and output space.",
                        "?", LensTone.INFO, LensVisibility.DEEP));
            }
        }
        if (context.hasFluid()) {
            rows.add(LensInfoRow.of("Fluid", "Source blocks are usually collected with a bucket.",
                    "≈", LensTone.INFO, LensVisibility.EXPANDED));
        }
        if (context.hasEntity() && context.entity() instanceof LivingEntity living && living instanceof Enemy) {
            rows.add(LensInfoRow.of("Threat", "Hostile target. Check armor and effects before engaging.",
                    "▲", LensTone.WARNING, LensVisibility.COMPACT));
        }
        if (rows.isEmpty()) {
            return List.of();
        }
        return List.of(LensInfoSection.of(EchoLens.id("section/hints"), LensDataCategory.HINTS,
                "Guidance", "?", LensTone.INFO, LensVisibility.COMPACT, rows));
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
        return "correct tool";
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
}
