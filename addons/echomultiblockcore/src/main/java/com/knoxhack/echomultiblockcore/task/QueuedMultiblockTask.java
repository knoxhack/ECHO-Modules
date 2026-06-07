package com.knoxhack.echomultiblockcore.task;

import com.knoxhack.echomultiblockcore.api.MultiblockTaskState;
import com.knoxhack.echomultiblockcore.api.TaskExecutionSnapshot;
import com.knoxhack.echomultiblockcore.api.MultiblockAutomationRecipe;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public final class QueuedMultiblockTask {
    private final Identifier taskId;
    private MultiblockTaskState state;
    private int progressTicks;
    private int durationTicks;
    private BlockPos robotPos;
    private Identifier robotId;
    private Identifier workcellId;
    private String blockedReason;
    private int retryCount;
    private boolean inputsConsumed;
    private String inputSummary;
    private String outputSummary;
    private String effectDiagnostic;
    private final long createdGameTime;

    public QueuedMultiblockTask(Identifier taskId, long createdGameTime) {
        this(taskId, MultiblockTaskState.WAITING, 0, 1, BlockPos.ZERO, null, null, "", 0, false, "", "", "", createdGameTime);
    }

    public QueuedMultiblockTask(Identifier taskId, MultiblockTaskState state, int progressTicks, int durationTicks,
            BlockPos robotPos, String blockedReason, int retryCount, long createdGameTime) {
        this(taskId, state, progressTicks, durationTicks, robotPos, null, null, blockedReason, retryCount, false, "", "", createdGameTime);
    }

    public QueuedMultiblockTask(Identifier taskId, MultiblockTaskState state, int progressTicks, int durationTicks,
            BlockPos robotPos, String blockedReason, int retryCount, boolean inputsConsumed, long createdGameTime) {
        this(taskId, state, progressTicks, durationTicks, robotPos, null, null, blockedReason, retryCount,
                inputsConsumed, "", "", "", createdGameTime);
    }

    public QueuedMultiblockTask(Identifier taskId, MultiblockTaskState state, int progressTicks, int durationTicks,
            BlockPos robotPos, Identifier robotId, Identifier workcellId, String blockedReason, int retryCount,
            boolean inputsConsumed, String inputSummary, String outputSummary, long createdGameTime) {
        this(taskId, state, progressTicks, durationTicks, robotPos, robotId, workcellId, blockedReason, retryCount,
                inputsConsumed, inputSummary, outputSummary, "", createdGameTime);
    }

    public QueuedMultiblockTask(Identifier taskId, MultiblockTaskState state, int progressTicks, int durationTicks,
            BlockPos robotPos, Identifier robotId, Identifier workcellId, String blockedReason, int retryCount,
            boolean inputsConsumed, String inputSummary, String outputSummary, String effectDiagnostic, long createdGameTime) {
        this.taskId = taskId;
        this.state = state == null ? MultiblockTaskState.WAITING : state;
        this.progressTicks = Math.max(0, progressTicks);
        this.durationTicks = Math.max(1, durationTicks);
        this.robotPos = robotPos == null ? BlockPos.ZERO : robotPos.immutable();
        this.robotId = robotId;
        this.workcellId = workcellId;
        this.blockedReason = blockedReason == null ? "" : blockedReason.strip();
        this.retryCount = Math.max(0, retryCount);
        this.inputsConsumed = inputsConsumed;
        this.inputSummary = inputSummary == null ? "" : inputSummary.strip();
        this.outputSummary = outputSummary == null ? "" : outputSummary.strip();
        this.effectDiagnostic = effectDiagnostic == null ? "" : effectDiagnostic.strip();
        this.createdGameTime = Math.max(0L, createdGameTime);
    }

    public Identifier taskId() {
        return taskId;
    }

    public MultiblockTaskState state() {
        return state;
    }

    public int progressTicks() {
        return progressTicks;
    }

    public int durationTicks() {
        return durationTicks;
    }

    public BlockPos robotPos() {
        return robotPos;
    }

    public Identifier robotId() {
        return robotId;
    }

    public Identifier workcellId() {
        return workcellId;
    }

    public String blockedReason() {
        return blockedReason;
    }

    public int retryCount() {
        return retryCount;
    }

    public boolean inputsConsumed() {
        return inputsConsumed;
    }

    public String inputSummary() {
        return inputSummary;
    }

    public String outputSummary() {
        return outputSummary;
    }

    public long createdGameTime() {
        return createdGameTime;
    }

    public String effectDiagnostic() {
        return effectDiagnostic;
    }

    public void recordEffectDiagnostic(String diagnostic) {
        if (diagnostic != null && !diagnostic.isBlank()) {
            this.effectDiagnostic = diagnostic.strip();
        }
    }

    public void start(int durationTicks, BlockPos robotPos) {
        start(durationTicks, robotPos, null, null, "", "");
    }

    public void start(int durationTicks, BlockPos robotPos, Identifier robotId, Identifier workcellId,
            String inputSummary, String outputSummary) {
        this.state = MultiblockTaskState.ACTIVE;
        this.progressTicks = 0;
        this.durationTicks = Math.max(1, durationTicks);
        this.robotPos = robotPos == null ? BlockPos.ZERO : robotPos.immutable();
        this.robotId = robotId;
        this.workcellId = workcellId;
        this.inputSummary = inputSummary == null ? "" : inputSummary.strip();
        this.outputSummary = outputSummary == null ? "" : outputSummary.strip();
        this.blockedReason = "";
        this.effectDiagnostic = "";
    }

    public void markInputsConsumed() {
        this.inputsConsumed = true;
    }

    public void block(String reason) {
        this.state = MultiblockTaskState.BLOCKED;
        this.blockedReason = reason == null ? "" : reason.strip();
        this.effectDiagnostic = this.blockedReason;
        this.retryCount++;
    }

    public void pause(String reason) {
        this.state = MultiblockTaskState.PAUSED;
        this.blockedReason = reason == null ? "" : reason.strip();
        this.effectDiagnostic = this.blockedReason;
    }

    public void resume() {
        this.state = MultiblockTaskState.WAITING;
        this.blockedReason = "";
    }

    public void fail(String reason) {
        this.state = MultiblockTaskState.FAILED;
        this.blockedReason = reason == null ? "" : reason.strip();
        this.effectDiagnostic = this.blockedReason;
    }

    public void complete() {
        this.state = MultiblockTaskState.COMPLETED;
        this.progressTicks = this.durationTicks;
        this.blockedReason = "";
        this.inputsConsumed = false;
    }

    public void resetForRetry() {
        this.state = MultiblockTaskState.WAITING;
        this.progressTicks = 0;
        this.robotPos = BlockPos.ZERO;
        this.robotId = null;
        this.workcellId = null;
        this.blockedReason = "";
        this.effectDiagnostic = "";
    }

    public void incrementProgress() {
        this.progressTicks++;
    }

    public TaskExecutionSnapshot snapshot(String displayName) {
        return snapshot(displayName, "general");
    }

    public TaskExecutionSnapshot snapshot(String displayName, String category) {
        return snapshot(displayName, category, List.of());
    }

    public TaskExecutionSnapshot snapshot(MultiblockAutomationRecipe recipe) {
        return recipe == null
                ? snapshot(taskId.toString(), "missing", List.of())
                : new TaskExecutionSnapshot(taskId, recipe.displayName(), state, progressTicks, durationTicks, robotPos,
                        blockedReason, retryCount, recipe.category().toString(), robotId, workcellId, inputSummary,
                        outputSummary, recipe.effects(), effectDiagnostic,
                        recipe.capabilityCosts().isEmpty() ? "" : recipe.capabilityCosts().size() + " capability cost(s)",
                        recipe.requiredUpgrades().isEmpty() ? "" : recipe.requiredUpgrades().size() + " required upgrade(s)",
                        recipe.repairPriority(), recipe.animationProfile(), recipe.autoBuilderEligible());
    }

    public TaskExecutionSnapshot snapshot(String displayName, String category, List<Identifier> effectIds) {
        return new TaskExecutionSnapshot(taskId, displayName, state, progressTicks, durationTicks, robotPos,
                blockedReason, retryCount, category, robotId, workcellId, inputSummary, outputSummary,
                effectIds, effectDiagnostic);
    }
}
