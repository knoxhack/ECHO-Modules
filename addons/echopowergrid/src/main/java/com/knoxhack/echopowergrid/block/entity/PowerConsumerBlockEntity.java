package com.knoxhack.echopowergrid.block.entity;

import com.knoxhack.echopowergrid.api.EchoPowerNode;
import com.knoxhack.echopowergrid.api.EchoPowerNodeType;
import com.knoxhack.echopowergrid.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PowerConsumerBlockEntity extends BlockEntity implements EchoPowerNode {
    private long demand;
    private long powerReceived;
    private boolean poweredLastTick;
    private int poweredTicksRemaining;

    public PowerConsumerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONSUMER.get(), pos, state);
        if (state.getBlock() instanceof com.knoxhack.echopowergrid.block.ConsumerBlock con) {
            this.demand = con.getDemand();
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PowerConsumerBlockEntity con) {
        if (level.isClientSide()) return;
        if (con.poweredTicksRemaining > 0) {
            con.poweredLastTick = true;
            con.poweredTicksRemaining--;
        } else {
            con.poweredLastTick = con.powerReceived >= con.demand;
        }
        con.powerReceived = 0; // Reset for next tick distribution
    }

    public void setPowerReceived(long amount) {
        setPowerReceived(amount, 1);
    }

    public void setPowerReceived(long amount, int ticksCovered) {
        this.powerReceived = amount;
        if (amount >= demand) {
            this.poweredLastTick = true;
            this.poweredTicksRemaining = Math.max(this.poweredTicksRemaining, Math.max(0, ticksCovered - 1));
        } else {
            this.poweredTicksRemaining = 0;
        }
        setChanged();
    }

    public long getLastReceived() {
        return powerReceived;
    }

    public void onUse(Player player) {
        player.sendSystemMessage(Component.literal("ECHO GRID // Consumer: demand " + demand + " EP/t"));
        player.sendSystemMessage(Component.literal("  Status: " + (poweredLastTick ? "POWERED" : "NO POWER")));
    }

    @Override
    public BlockPos getNodePos() { return worldPosition; }

    @Override
    public ResourceKey<Level> getDimension() { return level != null ? level.dimension() : null; }

    @Override
    public EchoPowerNodeType getNodeType() {
        if (demand >= Long.MAX_VALUE / 8) return EchoPowerNodeType.CREATIVE_SINK;
        return EchoPowerNodeType.CONSUMER;
    }

    @Override
    public long getGenerationPerTick() { return 0; }

    @Override
    public long getDemandPerTick() { return demand; }

    @Override
    public long getStoredEnergy() { return 0; }

    @Override
    public long getCapacity() { return 0; }

    @Override
    public long getTransferLimit() { return demand; }

    @Override
    public boolean isOnline() { return poweredLastTick; }

    @Override
    public boolean isOverloaded() { return false; }
}
