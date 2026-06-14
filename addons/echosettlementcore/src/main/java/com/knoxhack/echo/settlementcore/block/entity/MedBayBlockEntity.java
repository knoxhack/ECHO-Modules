package com.knoxhack.echo.settlementcore.block.entity;

import com.knoxhack.echo.settlementcore.registry.ModBlockEntities;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class MedBayBlockEntity extends BlockEntity {
    private int tickCounter = 0;

    public MedBayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MED_BAY.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MedBayBlockEntity entity) {
        if (level.isClientSide()) {
            return;
        }
        entity.tickCounter++;
        if (entity.tickCounter < 60) {
            return;
        }
        entity.tickCounter = 0;
        AABB area = new AABB(pos).inflate(4.0D);
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, area);
        for (ServerPlayer player : players) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 0, false, false));
        }
        entity.setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tickCounter = input.getIntOr("tickCounter", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("tickCounter", tickCounter);
    }
}
