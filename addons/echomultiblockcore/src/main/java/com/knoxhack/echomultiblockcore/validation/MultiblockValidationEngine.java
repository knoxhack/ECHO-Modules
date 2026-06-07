package com.knoxhack.echomultiblockcore.validation;

import com.knoxhack.echomultiblockcore.Config;
import com.knoxhack.echomultiblockcore.api.MultiblockDefinition;
import com.knoxhack.echomultiblockcore.api.StructureBlockRequirement;
import com.knoxhack.echomultiblockcore.api.ValidationResult;
import com.knoxhack.echomultiblockcore.api.ValidationOptions;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

public final class MultiblockValidationEngine {
    private MultiblockValidationEngine() {
    }

    public static ValidationResult validate(Level level, BlockPos controllerPos, MultiblockDefinition definition) {
        return validate(level, controllerPos, definition, ValidationOptions.DEFAULT);
    }

    public static ValidationResult validate(Level level, BlockPos controllerPos, MultiblockDefinition definition, ValidationOptions options) {
        if (level == null || controllerPos == null || definition == null) {
            return ValidationResult.error(controllerPos, "Missing validation context.");
        }
        options = options == null ? ValidationOptions.DEFAULT : options;
        int maxVolume;
        try {
            maxVolume = Config.MAX_VALIDATION_VOLUME.get();
        } catch (RuntimeException exception) {
            maxVolume = 4096;
        }
        if (definition.volume() > maxVolume) {
            return ValidationResult.error(controllerPos, "Definition volume " + definition.volume()
                    + " exceeds configured maximum " + maxVolume + ".");
        }
        BlockPos controllerLocal = definition.controllerLocalPosition().orElseGet(() ->
                new BlockPos(definition.width() / 2, 0, definition.depth() / 2));
        List<Attempt> attempts = new ArrayList<>();
        for (Rotation rotation : rotations(definition.allowedRotations())) {
            attempts.add(scan(level, controllerPos, definition, rotation, false, controllerLocal, options));
            if (definition.mirrorable()) {
                attempts.add(scan(level, controllerPos, definition, rotation, true, controllerLocal, options));
            }
        }
        return attempts.stream()
                .max(Comparator.comparing((Attempt attempt) -> attempt.result().valid())
                        .thenComparingDouble(attempt -> attempt.result().completion())
                        .thenComparingInt(attempt -> -attempt.result().wrongBlocks().size())
                        .thenComparingInt(attempt -> -attempt.result().missingBlocks().size())
                        .thenComparingInt(attempt -> attempt.result().mirrored() ? 0 : 1)
                        .thenComparingInt(attempt -> rotationPreference(attempt.result().matchedRotation())))
                .map(Attempt::result)
                .orElseGet(() -> ValidationResult.error(controllerPos, "No validation attempts were produced."));
    }

    private static Attempt scan(Level level, BlockPos controllerPos, MultiblockDefinition definition,
            Rotation rotation, boolean mirror, BlockPos controllerLocal, ValidationOptions options) {
        BlockPos transformedController = transform(controllerLocal, definition, rotation, mirror);
        BlockPos origin = controllerPos.subtract(transformedController);
        List<BlockPos> matched = new ArrayList<>();
        List<ValidationResult.BlockIssue> missing = new ArrayList<>();
        List<ValidationResult.BlockIssue> wrong = new ArrayList<>();
        List<ValidationResult.BlockIssue> optional = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> unloadedWarnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int required = 0;
        int passed = 0;
        for (int y = 0; y < definition.height(); y++) {
            for (int z = 0; z < definition.depth(); z++) {
                for (int x = 0; x < definition.width(); x++) {
                    StructureBlockRequirement requirement = definition.requirementAt(x, y, z);
                    BlockPos worldPos = origin.offset(transform(new BlockPos(x, y, z), definition, rotation, mirror));
                    if (options.requireLoadedChunks() && !level.isLoaded(worldPos)) {
                        unloadedWarnings.add("Unloaded validation position " + worldPos.toShortString() + ".");
                        if (requirement.required()) {
                            required++;
                        }
                        continue;
                    }
                    BlockState state = level.getBlockState(worldPos);
                    boolean matches = requirement.matches(level, state);
                    if (requirement.required()) {
                        required++;
                    }
                    if (matches) {
                        if (requirement.required()) {
                            passed++;
                        }
                        if (!requirement.kind().equals(StructureBlockRequirement.SlotKind.AIR)
                                && !requirement.kind().equals(StructureBlockRequirement.SlotKind.WILDCARD)) {
                            matched.add(worldPos.immutable());
                        }
                    } else if (requirement.optional()) {
                        optional.add(issue(worldPos, requirement, state, "Optional slot not installed."));
                    } else if (state.isAir()) {
                        missing.add(issue(worldPos, requirement, state, "Missing " + requirement.expectedName() + "."));
                    } else {
                        wrong.add(issue(worldPos, requirement, state, "Expected " + requirement.expectedName() + "."));
                    }
                }
            }
        }
        if (!unloadedWarnings.isEmpty()) {
            errors.add("Validation skipped " + unloadedWarnings.size() + " unloaded position(s).");
        }
        if (definition.requiresFoundation() && origin.getY() <= level.getMinY()) {
            warnings.add("Foundation touches world floor; verify support blocks manually.");
        }
        double completion = required == 0 ? 1.0D : (double) passed / (double) required;
        boolean valid = missing.isEmpty() && wrong.isEmpty() && errors.isEmpty();
        return new Attempt(new ValidationResult(definition.id(), valid, completion,
                options.collectMatchedBlocks() ? matched : List.of(),
                missing, wrong, optional, List.of(), null, warnings, unloadedWarnings, errors,
                rotation, mirror, origin, controllerPos, level.getGameTime()));
    }

    public static BlockPos transform(BlockPos local, MultiblockDefinition definition, Rotation rotation, boolean mirror) {
        int x = local.getX();
        int y = local.getY();
        int z = local.getZ();
        int width = definition.width();
        int depth = definition.depth();
        if (mirror) {
            x = width - 1 - x;
        }
        return switch (rotation == null ? Rotation.NONE : rotation) {
            case NONE -> new BlockPos(x, y, z);
            case CLOCKWISE_90 -> new BlockPos(depth - 1 - z, y, x);
            case CLOCKWISE_180 -> new BlockPos(width - 1 - x, y, depth - 1 - z);
            case COUNTERCLOCKWISE_90 -> new BlockPos(z, y, width - 1 - x);
        };
    }

    private static ValidationResult.BlockIssue issue(BlockPos pos, StructureBlockRequirement requirement, BlockState state, String message) {
        Identifier found = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return new ValidationResult.BlockIssue(pos, requirement, found, message);
    }

    private static List<Rotation> rotations(boolean allowedRotations) {
        return allowedRotations
                ? List.of(Rotation.NONE, Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90)
                : List.of(Rotation.NONE);
    }

    private static int rotationPreference(Rotation rotation) {
        return switch (rotation == null ? Rotation.NONE : rotation) {
            case NONE -> 40;
            case CLOCKWISE_90 -> 30;
            case CLOCKWISE_180 -> 20;
            case COUNTERCLOCKWISE_90 -> 10;
        };
    }

    private record Attempt(ValidationResult result) {
    }
}
