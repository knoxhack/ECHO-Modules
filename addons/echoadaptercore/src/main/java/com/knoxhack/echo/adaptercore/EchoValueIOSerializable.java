package com.knoxhack.echo.adaptercore;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * AdapterCore bridge marker for value-input/value-output persistence payloads.
 */
public interface EchoValueIOSerializable {
    void serialize(ValueOutput output);

    void deserialize(ValueInput input);
}
