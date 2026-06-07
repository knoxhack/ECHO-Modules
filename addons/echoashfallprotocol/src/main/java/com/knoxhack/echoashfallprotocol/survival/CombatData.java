package com.knoxhack.echoashfallprotocol.survival;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.knoxhack.echo.adaptercore.EchoValueIOSerializable;

public class CombatData implements EchoValueIOSerializable {
    private int lastCombatTick = -1;
    private static final int COMBAT_TIMEOUT = 200; // 10 seconds

    public void onCombatTick(int currentTick) {
        this.lastCombatTick = currentTick;
    }

    public boolean isInCombat(int currentTick) {
        return lastCombatTick >= 0 && (currentTick - lastCombatTick) < COMBAT_TIMEOUT;
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putInt("lastCombatTick", lastCombatTick);
    }

    @Override
    public void deserialize(ValueInput input) {
        this.lastCombatTick = input.getIntOr("lastCombatTick", -1);
    }
}
