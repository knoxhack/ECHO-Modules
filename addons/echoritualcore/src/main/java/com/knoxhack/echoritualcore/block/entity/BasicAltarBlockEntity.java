package com.knoxhack.echoritualcore.block.entity;

import com.knoxhack.echoritualcore.registry.ModBlockEntities;
import com.knoxhack.echoritualcore.ritual.RitualStructureReport;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BasicAltarBlockEntity extends BlockEntity {
    public static final int RESULT_IDLE = 0;
    public static final int RESULT_READY = 1;
    public static final int RESULT_COMPLETE = 2;
    public static final int RESULT_WARNING = 3;
    public static final int RESULT_FAILURE = 4;

    private String lastRitualId = "";
    private String lastSubjectId = "";
    private String lastMessage = "Circuit idle.";
    private int lastResult = RESULT_IDLE;
    private int lastStability = 0;
    private int lastRunes = 0;
    private int lastPedestals = 0;
    private int lastMissing = 0;

    public BasicAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BASIC_ALTAR.get(), pos, state);
    }

    public void updateStatus(Identifier ritualId, Identifier subjectId, int result, String message, RitualStructureReport report) {
        this.lastRitualId = ritualId == null ? "" : ritualId.toString();
        this.lastSubjectId = subjectId == null ? "" : subjectId.toString();
        this.lastResult = Math.max(RESULT_IDLE, Math.min(RESULT_FAILURE, result));
        this.lastMessage = message == null || message.isBlank() ? "Circuit idle." : message.strip();
        if (report != null) {
            this.lastStability = report.stabilityScore();
            this.lastRunes = report.runeCircles();
            this.lastPedestals = report.pedestalCount();
            this.lastMissing = report.missingCount();
        }
        setChanged();
    }

    public String lastRitualId() {
        return lastRitualId;
    }

    public String lastSubjectId() {
        return lastSubjectId;
    }

    public String lastMessage() {
        return lastMessage;
    }

    public int lastResult() {
        return lastResult;
    }

    public int lastStability() {
        return lastStability;
    }

    public int lastRunes() {
        return lastRunes;
    }

    public int lastPedestals() {
        return lastPedestals;
    }

    public int lastMissing() {
        return lastMissing;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        lastRitualId = input.getStringOr("last_ritual_id", "");
        lastSubjectId = input.getStringOr("last_subject_id", "");
        lastMessage = input.getStringOr("last_message", "Circuit idle.");
        lastResult = input.getIntOr("last_result", RESULT_IDLE);
        lastStability = input.getIntOr("last_stability", 0);
        lastRunes = input.getIntOr("last_runes", 0);
        lastPedestals = input.getIntOr("last_pedestals", 0);
        lastMissing = input.getIntOr("last_missing", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("last_ritual_id", lastRitualId);
        output.putString("last_subject_id", lastSubjectId);
        output.putString("last_message", lastMessage);
        output.putInt("last_result", lastResult);
        output.putInt("last_stability", lastStability);
        output.putInt("last_runes", lastRunes);
        output.putInt("last_pedestals", lastPedestals);
        output.putInt("last_missing", lastMissing);
    }
}
