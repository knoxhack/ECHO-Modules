package com.knoxhack.echostationfall.integration;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public final class StationfallSuitState {
    private static final String ROOT = "echostationfall_suit";
    private final Object delegate;
    private int oxygen;
    private int pressure;
    private int radiation;

    private StationfallSuitState(Object delegate, int oxygen, int pressure, int radiation) {
        this.delegate = delegate;
        this.oxygen = oxygen;
        this.pressure = pressure;
        this.radiation = radiation;
    }

    public static StationfallSuitState get(Player player) {
        return StationfallOrbitalCompat.suitDelegate(player)
                .map(delegate -> new StationfallSuitState(delegate, 100, 100, 0))
                .orElseGet(() -> readLocal(player));
    }

    public void drainOxygen(int amount) {
        if (delegate != null) {
            StationfallOrbitalCompat.invokeVoid(delegate, "drainOxygen", int.class, amount);
            return;
        }
        oxygen = clamp(oxygen - Math.max(0, amount));
    }

    public void boostOxygen(int amount) {
        if (delegate != null) {
            StationfallOrbitalCompat.invokeVoid(delegate, "boostOxygen", int.class, amount);
            return;
        }
        oxygen = clamp(oxygen + Math.max(0, amount));
    }

    public void compromisePressure(int amount) {
        if (delegate != null) {
            StationfallOrbitalCompat.invokeVoid(delegate, "compromisePressure", int.class, amount);
            return;
        }
        pressure = clamp(pressure - Math.max(0, amount));
    }

    public void applySealantPatch() {
        if (delegate != null) {
            StationfallOrbitalCompat.invokeVoid(delegate, "applySealantPatch");
            return;
        }
        pressure = clamp(pressure + 40);
    }

    public int oxygen() {
        return delegate == null ? oxygen : StationfallOrbitalCompat.invokeInt(delegate, "oxygen").orElse(oxygen);
    }

    public int pressure() {
        return delegate == null ? pressure : StationfallOrbitalCompat.invokeInt(delegate, "pressure").orElse(pressure);
    }

    public int radiation() {
        return delegate == null ? radiation : StationfallOrbitalCompat.invokeInt(delegate, "radiation").orElse(radiation);
    }

    public void save(Player player) {
        if (delegate != null) {
            StationfallOrbitalCompat.invokeVoid(delegate, "save", Player.class, player);
            return;
        }
        if (player == null) {
            return;
        }
        CompoundTag tag = new CompoundTag();
        tag.putInt("oxygen", oxygen);
        tag.putInt("pressure", pressure);
        tag.putInt("radiation", radiation);
        player.getPersistentData().put(ROOT, tag);
    }

    private static StationfallSuitState readLocal(Player player) {
        if (player == null) {
            return new StationfallSuitState(null, 100, 100, 0);
        }
        CompoundTag tag = player.getPersistentData().getCompoundOrEmpty(ROOT);
        return new StationfallSuitState(
                null,
                tag.getIntOr("oxygen", 100),
                tag.getIntOr("pressure", 100),
                tag.getIntOr("radiation", 0));
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
