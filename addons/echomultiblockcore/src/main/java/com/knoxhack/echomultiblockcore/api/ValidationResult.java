package com.knoxhack.echomultiblockcore.api;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;

public record ValidationResult(
        Identifier definitionId,
        boolean valid,
        double completion,
        List<BlockPos> matchedBlocks,
        List<BlockIssue> missingBlocks,
        List<BlockIssue> wrongBlocks,
        List<BlockIssue> optionalBlocks,
        List<BlockIssue> damagedBlocks,
        List<IssueSummary> issueSummaries,
        List<String> warnings,
        List<String> unloadedAreaWarnings,
        List<String> errors,
        Rotation matchedRotation,
        boolean mirrored,
        BlockPos matchedOrigin,
        BlockPos controllerPosition,
        long validationTime) {
    public ValidationResult {
        completion = Math.max(0.0D, Math.min(1.0D, completion));
        matchedBlocks = List.copyOf(matchedBlocks == null ? List.of() : matchedBlocks.stream().map(BlockPos::immutable).toList());
        missingBlocks = List.copyOf(missingBlocks == null ? List.of() : missingBlocks);
        wrongBlocks = List.copyOf(wrongBlocks == null ? List.of() : wrongBlocks);
        optionalBlocks = List.copyOf(optionalBlocks == null ? List.of() : optionalBlocks);
        damagedBlocks = List.copyOf(damagedBlocks == null ? List.of() : damagedBlocks);
        issueSummaries = List.copyOf(issueSummaries == null ? summarize(missingBlocks, wrongBlocks, damagedBlocks) : issueSummaries);
        warnings = List.copyOf(warnings == null ? List.of() : warnings);
        unloadedAreaWarnings = List.copyOf(unloadedAreaWarnings == null ? List.of() : unloadedAreaWarnings);
        errors = List.copyOf(errors == null ? List.of() : errors);
        matchedRotation = matchedRotation == null ? Rotation.NONE : matchedRotation;
        matchedOrigin = matchedOrigin == null ? BlockPos.ZERO : matchedOrigin.immutable();
        controllerPosition = controllerPosition == null ? BlockPos.ZERO : controllerPosition.immutable();
        validationTime = Math.max(0L, validationTime);
    }

    public ValidationResult(
            boolean valid,
            double completion,
            List<BlockIssue> missingBlocks,
            List<BlockIssue> wrongBlocks,
            List<BlockIssue> optionalBlocks,
            List<BlockIssue> damagedBlocks,
            List<String> warnings,
            List<String> errors,
            Rotation matchedRotation,
            boolean mirrored,
            BlockPos matchedOrigin,
            BlockPos controllerPosition) {
        this(null, valid, completion, List.of(), missingBlocks, wrongBlocks, optionalBlocks, damagedBlocks,
                null, warnings, List.of(), errors, matchedRotation, mirrored, matchedOrigin, controllerPosition, 0L);
    }

    public static ValidationResult error(BlockPos controllerPosition, String message) {
        return error(null, controllerPosition, message);
    }

    public static ValidationResult error(Identifier definitionId, BlockPos controllerPosition, String message) {
        return new ValidationResult(definitionId, false, 0.0D, List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(message == null ? "Unknown validation error." : message),
                Rotation.NONE, false, controllerPosition, controllerPosition, 0L);
    }

    public int issueCount() {
        return missingBlocks.size() + wrongBlocks.size() + damagedBlocks.size();
    }

    public String summaryLine() {
        return (valid ? "FORMED" : "INCOMPLETE") + " / " + Math.round(completion * 100.0D)
                + "% / missing " + missingBlocks.size() + " / wrong " + wrongBlocks.size();
    }

    public List<IssueSummary> groupedIssues() {
        return issueSummaries;
    }

    private static List<IssueSummary> summarize(List<BlockIssue> missing, List<BlockIssue> wrong, List<BlockIssue> damaged) {
        java.util.Map<String, IssueSummary.Mutable> grouped = new java.util.LinkedHashMap<>();
        collect(grouped, "missing", missing);
        collect(grouped, "wrong", wrong);
        collect(grouped, "damaged", damaged);
        return grouped.values().stream().map(IssueSummary.Mutable::freeze).toList();
    }

    private static void collect(java.util.Map<String, IssueSummary.Mutable> grouped, String kind, List<BlockIssue> issues) {
        if (issues == null) {
            return;
        }
        for (BlockIssue issue : issues) {
            String expected = issue.requirement() == null ? "unknown" : issue.requirement().expectedName();
            String found = issue.foundBlock() == null ? "minecraft:air" : issue.foundBlock().toString();
            String key = kind + "|" + expected + "|" + found;
            grouped.computeIfAbsent(key, ignored -> new IssueSummary.Mutable(kind, expected, found, issue.pos())).count++;
        }
    }

    public record BlockIssue(
            BlockPos pos,
            StructureBlockRequirement requirement,
            Identifier foundBlock,
            String message) {
        public BlockIssue {
            pos = pos == null ? BlockPos.ZERO : pos.immutable();
            message = message == null ? "" : message.strip();
        }
    }

    public record IssueSummary(String kind, String expected, String found, int count, BlockPos firstPos) {
        public IssueSummary {
            kind = kind == null || kind.isBlank() ? "issue" : kind.strip();
            expected = expected == null || expected.isBlank() ? "unknown" : expected.strip();
            found = found == null || found.isBlank() ? "unknown" : found.strip();
            count = Math.max(0, count);
            firstPos = firstPos == null ? BlockPos.ZERO : firstPos.immutable();
        }

        private static final class Mutable {
            private final String kind;
            private final String expected;
            private final String found;
            private final BlockPos firstPos;
            private int count;

            private Mutable(String kind, String expected, String found, BlockPos firstPos) {
                this.kind = kind;
                this.expected = expected;
                this.found = found;
                this.firstPos = firstPos;
            }

            private IssueSummary freeze() {
                return new IssueSummary(kind, expected, found, count, firstPos);
            }
        }
    }
}
