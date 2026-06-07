package com.knoxhack.echoashfallprotocol.world;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.knoxhack.echo.adaptercore.EchoValueIOSerializable;

/**
 * Stores the player's current repeatable field-ops contract.
 */
public class FieldOpsData implements EchoValueIOSerializable {

    public enum ContractType {
        NONE,
        STORM_SHELTER,
        SCANNER_SWEEP,
        CORRUPTED_BOUNTY
    }

    private ContractType activeContract = ContractType.NONE;
    private String targetId = "";
    private String targetName = "";
    private int progress = 0;
    private int goal = 0;
    private int completedContracts = 0;

    public ContractType getActiveContract() {
        return activeContract;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public int getProgress() {
        return progress;
    }

    public int getGoal() {
        return goal;
    }

    public int getCompletedContracts() {
        return completedContracts;
    }

    public boolean hasActiveContract() {
        return activeContract != ContractType.NONE;
    }

    public boolean isComplete() {
        return hasActiveContract() && progress >= goal;
    }

    public void assign(ContractType type, String targetId, String targetName, int goal) {
        this.activeContract = type;
        this.targetId = targetId;
        this.targetName = targetName;
        this.goal = Math.max(1, goal);
        this.progress = 0;
    }

    public void incrementProgress() {
        progress = Math.min(goal, progress + 1);
    }

    public void clearContract() {
        if (activeContract != ContractType.NONE) {
            completedContracts++;
        }
        activeContract = ContractType.NONE;
        targetId = "";
        targetName = "";
        progress = 0;
        goal = 0;
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putString("activeContract", activeContract.name());
        output.putString("targetId", targetId);
        output.putString("targetName", targetName);
        output.putInt("progress", progress);
        output.putInt("goal", goal);
        output.putInt("completedContracts", completedContracts);
    }

    @Override
    public void deserialize(ValueInput input) {
        try {
            activeContract = ContractType.valueOf(input.getStringOr("activeContract", "NONE"));
        } catch (IllegalArgumentException ignored) {
            activeContract = ContractType.NONE;
        }
        targetId = input.getStringOr("targetId", "");
        targetName = input.getStringOr("targetName", "");
        progress = input.getIntOr("progress", 0);
        goal = input.getIntOr("goal", 0);
        completedContracts = input.getIntOr("completedContracts", 0);
    }
}
