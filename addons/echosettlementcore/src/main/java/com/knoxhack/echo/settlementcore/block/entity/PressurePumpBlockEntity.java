package com.knoxhack.echo.settlementcore.block.entity;

import com.knoxhack.echo.settlementcore.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class PressurePumpBlockEntity extends BlockEntity {
    private float pressure = 0.0f;

    public PressurePumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRESSURE_PUMP.get(), pos, state);
    }

    public float getPressure() {
        return pressure;
    }

    public void setPressure(float pressure) {
        this.pressure = Math.max(0.0f, Math.min(1.0f, pressure));
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PressurePumpBlockEntity entity) {
        if (level.isClientSide()) {
            return;
        }
        if (level.getGameTime() % 20 == 0) {
            entity.pressure = Math.min(1.0f, entity.pressure + 0.02f);
            entity.setChanged();
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        pressure = input.getFloatOr("pressure", 0.0f);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putFloat("pressure", pressure);
    }
}
