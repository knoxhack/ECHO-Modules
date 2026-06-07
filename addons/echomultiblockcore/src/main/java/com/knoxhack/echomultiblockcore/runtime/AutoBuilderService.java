package com.knoxhack.echomultiblockcore.runtime;

import com.knoxhack.echomultiblockcore.api.AutoBuilderPlan;
import com.knoxhack.echomultiblockcore.api.AutoBuilderResult;
import com.knoxhack.echomultiblockcore.api.AutoBuilderStep;
import com.knoxhack.echomultiblockcore.api.ConstructionPermissionPolicy;
import com.knoxhack.echomultiblockcore.api.StructureBlockRequirement;
import com.knoxhack.echomultiblockcore.api.ValidationResult;
import com.knoxhack.echomultiblockcore.block.entity.MultiblockCrateBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public final class AutoBuilderService {
    private AutoBuilderService() {
    }

    public static AutoBuilderPlan plan(ValidationResult result, int maxSteps) {
        List<AutoBuilderStep> steps = new ArrayList<>();
        if (result != null) {
            for (ValidationResult.BlockIssue issue : result.missingBlocks()) {
                StructureBlockRequirement requirement = issue.requirement();
                Identifier blockId = placeableBlock(requirement);
                if (blockId != null) {
                    steps.add(new AutoBuilderStep(issue.pos(), blockId, requirement.expectedName(), requirement.optional()));
                }
                if (steps.size() >= Math.max(1, maxSteps)) {
                    break;
                }
            }
        }
        return new AutoBuilderPlan(result == null ? null : result.definitionId(),
                result == null ? net.minecraft.core.BlockPos.ZERO : result.controllerPosition(),
                steps, steps.size(), ConstructionPermissionPolicy.OPERATOR_ONLY);
    }

    public static AutoBuilderResult execute(ServerLevel level, AutoBuilderPlan plan, MultiblockCrateBlockEntity input,
            Player actor, int maxPlacements) {
        if (level == null || plan == null) {
            return AutoBuilderResult.blocked("No auto-builder plan is available.");
        }
        if (input == null && (actor == null || !actor.isCreative())) {
            return AutoBuilderResult.blocked("Missing linked input crate.");
        }
        int placed = 0;
        for (AutoBuilderStep step : plan.steps()) {
            if (placed >= Math.max(1, maxPlacements)) {
                break;
            }
            if (!level.hasChunkAt(step.worldPosition())) {
                return AutoBuilderResult.blocked("Auto Builder reached an unloaded position at " + step.worldPosition() + ".");
            }
            if (!level.getBlockState(step.worldPosition()).isAir()) {
                String message = "Auto Builder refused occupied or wrong block at " + step.worldPosition() + ".";
                return placed > 0
                        ? AutoBuilderResult.success(placed, "Placed " + placed + " block(s); stopped before occupied or wrong block at "
                                + step.worldPosition() + ".")
                        : AutoBuilderResult.blocked(message);
            }
            Block block = BuiltInRegistries.BLOCK.getOptional(step.expectedBlockId()).orElse(null);
            if (block == null) {
                continue;
            }
            Item item = block.asItem();
            boolean consumed = false;
            if (actor == null || !actor.isCreative()) {
                if (input == null || !input.consume(item, 1)) {
                    return placed > 0
                            ? AutoBuilderResult.success(placed, "Placed " + placed + " block(s); more materials are needed.")
                            : AutoBuilderResult.blocked("Missing material: " + step.expectedText() + ".");
                }
                consumed = true;
            }
            if (level.setBlock(step.worldPosition(), block.defaultBlockState(), Block.UPDATE_ALL)) {
                placed++;
            } else {
                if (consumed && input != null) {
                    input.insertStack(new ItemStack(item, 1));
                }
                return placed > 0
                        ? AutoBuilderResult.success(placed, "Placed " + placed + " block(s); placement failed at "
                                + step.worldPosition() + " and material was restored.")
                        : AutoBuilderResult.blocked("Placement failed at " + step.worldPosition() + "; material restored.");
            }
        }
        if (placed <= 0) {
            return AutoBuilderResult.blocked("No placeable missing exact-block cells found.");
        }
        return AutoBuilderResult.success(placed, "Auto Builder placed " + placed + " block(s).");
    }

    private static Identifier placeableBlock(StructureBlockRequirement requirement) {
        if (requirement == null) {
            return null;
        }
        return switch (requirement.kind()) {
            case EXACT_BLOCK, CONTROLLER, COMPONENT, ROBOTICS, UPGRADE -> requirement.block();
            case BLOCK_LIST -> requirement.blocks().isEmpty() ? null : requirement.blocks().get(0);
            default -> null;
        };
    }
}
