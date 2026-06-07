package com.knoxhack.echoashfallprotocol.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import com.knoxhack.echo.adaptercore.EchoValueIOSerializable;

/**
 * Stores migration version data for the player.
 * Used to track which data migrations have been applied.
 */
public class MigrationData implements EchoValueIOSerializable {
    
    public static final com.mojang.serialization.MapCodec<MigrationData> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("version").forGetter(MigrationData::getVersion)
        ).apply(instance, MigrationData::new)
    );
    
    private int version;
    
    public MigrationData() {
        this.version = 0; // Default: no migrations applied
    }
    
    public MigrationData(int version) {
        this.version = version;
    }
    
    public int getVersion() {
        return version;
    }
    
    public void setVersion(int version) {
        this.version = version;
    }
    
    @Override
    public void serialize(net.minecraft.world.level.storage.ValueOutput output) {
        output.putInt("version", version);
    }

    @Override
    public void deserialize(net.minecraft.world.level.storage.ValueInput input) {
        version = input.getIntOr("version", 0);
    }
}
