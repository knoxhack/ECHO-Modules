package com.knoxhack.echo.settlementcore.block.entity;

import com.knoxhack.echo.settlementcore.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class OxygenRecyclerBlockEntity extends BlockEntity {
    private float oxygen = 0.0f;
    private int tickCounter = 0;

    public OxygenRecyclerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OXYGEN_RECYCLER.get(), pos, state);
    }

    public float getOxygen() {
        return oxygen;
    }

    public void setOxygen(float oxygen) {
        this.oxygen = Math.max(0.0f, Math.min(1.0f, oxygen));
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, OxygenRecyclerBlockEntity entity) {
        if (level.isClientSide()) {
            return;
        }
        entity.tickCounter++;
        if (entity.tickCounter >= 40) {
            entity.tickCounter = 0;
            entity.oxygen = Math.min(1.0f, entity.oxygen + 0.05f);
            entity.setChanged();
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        oxygen = input.getFloatOr("oxygen", 0.0f);
        tickCounter = input.getIntOr("tickCounter", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putFloat("oxygen", oxygen);
        output.putInt("tickCounter", tickCounter);
    }
}
