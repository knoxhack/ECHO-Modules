package com.knoxhack.echoashfallprotocol.event;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.knoxhack.echo.adaptercore.EchoValueIOSerializable;

/**
 * Tracks player behavior for the Smart Events system.
 */
public class SmartEventData implements EchoValueIOSerializable {
    private int techUsageScore = 0;
    private int hoardingScore = 0;
    private int stealthScore = 0;
    private long lastDecayTick = 0;

    public SmartEventData() {}

    public int getTechUsageScore() { return techUsageScore; }
    public void addTechUsage(int amount) { techUsageScore = clampScore(techUsageScore + amount); }
    public int getHoardingScore() { return hoardingScore; }
    public void addHoarding(int amount) { hoardingScore = clampScore(hoardingScore + amount); }
    public int getStealthScore() { return stealthScore; }
    public void addStealth(int amount) { stealthScore = clampScore(stealthScore + amount); }
    public long getLastDecayTick() { return lastDecayTick; }
    public void setLastDecayTick(long tick) { lastDecayTick = tick; }

    public void decayAll(int amount) {
        techUsageScore = Math.max(0, techUsageScore - amount);
        hoardingScore = Math.max(0, hoardingScore - amount);
        stealthScore = Math.max(0, stealthScore - amount);
    }

    private static int clampScore(int value) {
        return Math.max(0, Math.min(100, value));
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putInt("techUsageScore", techUsageScore);
        output.putInt("hoardingScore", hoardingScore);
        output.putInt("stealthScore", stealthScore);
        output.putLong("lastDecayTick", lastDecayTick);
    }

    @Override
    public void deserialize(ValueInput input) {
        techUsageScore = clampScore(input.getIntOr("techUsageScore", 0));
        hoardingScore = clampScore(input.getIntOr("hoardingScore", 0));
        stealthScore = clampScore(input.getIntOr("stealthScore", 0));
        lastDecayTick = input.getLongOr("lastDecayTick", 0L);
    }
}
