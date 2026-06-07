package com.knoxhack.echoashfallprotocol.block.entity;

import com.knoxhack.echoashfallprotocol.block.ScrapDynamoBlock;
import com.knoxhack.echoashfallprotocol.capability.EnergyStorage;
import com.knoxhack.echoashfallprotocol.capability.IEnergyStorage;
import com.knoxhack.echoashfallprotocol.energy.EnergyAccess;
import com.knoxhack.echoashfallprotocol.registry.ModBlockEntities;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ScrapDynamoBlockEntity extends BlockEntity implements IEnergyStorage {
    private static final int CAPACITY = 8000;
    private static final int MAX_TRANSFER = 256;
    private static final int FE_PER_TICK = 24;

    private final EnergyStorage energyStorage = new EnergyStorage(CAPACITY, MAX_TRANSFER, MAX_TRANSFER);
    private int burnTimeRemaining = 0;
    private int maxBurnTime = 0;

    public ScrapDynamoBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SCRAP_DYNAMO.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ScrapDynamoBlockEntity entity) {
        if (level.isClientSide()) return;

        boolean wasActive = state.getValue(ScrapDynamoBlock.ACTIVE);
        boolean active = entity.burnTimeRemaining > 0;
        if (entity.burnTimeRemaining > 0) {
            entity.burnTimeRemaining--;
            entity.energyStorage.receiveEnergy(FE_PER_TICK, false);
            entity.setChanged();
        }

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            if (EnergyAccess.transferFromStorageToBlock(entity, level, neighborPos, dir.getOpposite(), MAX_TRANSFER) > 0) {
                entity.setChanged();
            }
            if (entity.energyStorage.getEnergyStored() <= 0) break;
        }

        if (wasActive != active) {
            level.setBlockAndUpdate(pos, state.setValue(ScrapDynamoBlock.ACTIVE, active));
        }
    }

    public boolean isFuel(ItemStack stack) {
        return stack.is(ModItems.SCRAP_METAL.get())
                || stack.is(ModItems.SCRAP_PLASTIC.get())
                || stack.is(ModItems.SCRAP_CIRCUIT.get())
                || stack.is(Items.COAL)
                || stack.is(Items.CHARCOAL);
    }

    public void addFuel(ItemStack stack) {
        int burn = getBurnTime(stack);
        if (burn <= 0) return;
        stack.shrink(1);
        burnTimeRemaining += burn;
        maxBurnTime = Math.max(maxBurnTime, burnTimeRemaining);
        setChanged();
    }

    private int getBurnTime(ItemStack stack) {
        if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) return 240;
        if (stack.is(ModItems.SCRAP_CIRCUIT.get())) return 180;
        if (stack.is(ModItems.SCRAP_PLASTIC.get())) return 120;
        if (stack.is(ModItems.SCRAP_METAL.get())) return 80;
        return 0;
    }

    public int getBurnTimeRemaining() {
        return burnTimeRemaining;
    }

    public int getMaxBurnTime() {
        return maxBurnTime;
    }

    @Override public int getEnergyStored() { return energyStorage.getEnergyStored(); }
    @Override public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }
    @Override public int receiveEnergy(int amount, boolean simulate) { return 0; }
    @Override public int extractEnergy(int amount, boolean simulate) {
        int extracted = energyStorage.extractEnergy(amount, simulate);
        if (extracted > 0 && !simulate) setChanged();
        return extracted;
    }
    @Override public boolean canReceive() { return false; }
    @Override public boolean canExtract() { return energyStorage.getEnergyStored() > 0; }
    @Override public void setEnergyStored(int energy) { energyStorage.setEnergyStored(energy); setChanged(); }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("energy", energyStorage.getEnergyStored());
        output.putInt("burnTimeRemaining", burnTimeRemaining);
        output.putInt("maxBurnTime", maxBurnTime);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energyStorage.setEnergyStored(input.getIntOr("energy", 0));
        burnTimeRemaining = input.getIntOr("burnTimeRemaining", 0);
        maxBurnTime = input.getIntOr("maxBurnTime", 0);
    }
}
