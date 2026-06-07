package com.knoxhack.echoashfallprotocol.block.entity;

import com.knoxhack.echoashfallprotocol.block.MicroGeneratorBlock;
import com.knoxhack.echoashfallprotocol.Config;
import com.knoxhack.echoashfallprotocol.energy.EnergyAccess;
import com.knoxhack.echoashfallprotocol.capability.IEnergyStorage;
import com.knoxhack.echoashfallprotocol.gameplay.MachineGameplayHelper;
import com.knoxhack.echoashfallprotocol.machine.MachineWearData;
import com.knoxhack.echoashfallprotocol.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import com.knoxhack.echoashfallprotocol.block.menu.MicroGeneratorMenu;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * Micro Generator — first real power source.
 * Unstable: random failures that require manual restart.
 * Generates power and outputs to adjacent IEnergyStorage blocks.
 */
public class MicroGeneratorBlockEntity extends BlockEntity implements MenuProvider, IEnergyStorage {
    private static final int FUEL_SLOT = 0;
    public static final int BATTERY_SLOT = 1;
    private static final int BURN_TICKS_PER_FUEL = 160;
    private static final int FE_PER_TICK = 8;
    private static final int OUTPUT_TRANSFER = 64;

    private static final Random RANDOM = new Random();

    private final MachineInventory inventory = new MachineInventory(2, this::setChanged);

    private int energy = 0;
    private int maxEnergy = 3000;
    private int burnTimeRemaining = 0;
    private int maxBurnTime = 0;
    private boolean failed = false;
    private int wearPercent = 0;

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energy;
                case 1 -> maxEnergy;
                case 2 -> burnTimeRemaining;
                case 3 -> maxBurnTime;
                case 4 -> failed ? 1 : 0;
                case 5 -> wearPercent;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> energy = value;
                case 1 -> maxEnergy = value;
                case 2 -> burnTimeRemaining = value;
                case 3 -> maxBurnTime = value;
                case 4 -> failed = value != 0;
                case 5 -> wearPercent = value;
            }
        }

        @Override
        public int getCount() { return 6; }
    };

    public MicroGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MICRO_GENERATOR.get(), pos, state);
    }

    public MachineInventory getInventory() { return inventory; }
    public int getEnergy() { return energy; }
    public int getMaxEnergy() { return maxEnergy; }
    public boolean isFailed() { return failed; }

    public int extractEnergy(int amount) {
        int extracted = Math.min(energy, amount);
        energy -= extracted;
        setChanged();
        return extracted;
    }

    // IEnergyStorage implementation
    @Override
    public int getEnergyStored() {
        return energy;
    }

    @Override
    public int getMaxEnergyStored() {
        return maxEnergy;
    }

    @Override
    public int receiveEnergy(int amount, boolean simulate) {
        return 0; // Generators don't receive energy
    }

    @Override
    public int extractEnergy(int amount, boolean simulate) {
        int extracted = Math.min(energy, amount);
        if (!simulate) {
            energy -= extracted;
            setChanged();
        }
        return extracted;
    }

    @Override
    public boolean canReceive() {
        return false;
    }

    @Override
    public boolean canExtract() {
        return energy > 0;
    }

    @Override
    public void setEnergyStored(int energy) {
        this.energy = Math.max(0, Math.min(maxEnergy, energy));
    }

    public void restart() {
        failed = false;
        setChanged();
    }

    private boolean isFuel(ItemStack stack) {
        return stack.is(Items.COAL) || stack.is(Items.CHARCOAL) ||
               stack.is(Items.OAK_PLANKS) || stack.is(Items.SPRUCE_PLANKS) ||
               stack.is(Items.BIRCH_PLANKS) || stack.is(Items.DARK_OAK_PLANKS) ||
               stack.is(Items.STICK);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.EchoAshfallProtocol.micro_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MicroGeneratorMenu(containerId, playerInventory, this, data);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MicroGeneratorBlockEntity entity) {
        boolean wasActive = state.getValue(MicroGeneratorBlock.ACTIVE);
        boolean isActive = false;
        EnergyAccess.chargeBatteryFromStorage(entity.inventory.getStackInSlot(BATTERY_SLOT), entity);

        MachineWearData wearData = new MachineWearData(level);
        entity.wearPercent = (int)(wearData.getWearPercent(pos) * 100);

        if (entity.failed) {
            if (wasActive) {
                level.setBlockAndUpdate(pos, state.setValue(MicroGeneratorBlock.ACTIVE, false));
            }
            return;
        }

        if (entity.burnTimeRemaining > 0) {
            entity.burnTimeRemaining--;
            int energyPerTick = Math.max(1, Math.round(FE_PER_TICK * MachineGameplayHelper.getMachineSpeedMultiplier(level, pos)));
            entity.energy = Math.min(entity.maxEnergy, entity.energy + energyPerTick);
            isActive = true;
            entity.setChanged();

            // Accumulate wear during operation (3 wear per fuel cycle - generators run hot)
            if (entity.burnTimeRemaining % 20 == 0) {
                wearData.addWear(pos, 3, level.getRandom());

                // Check for jam at high wear
                if (wearData.checkJamChance(pos, level.getRandom())) {
                    entity.failed = true;
                    entity.burnTimeRemaining = 0;
                    isActive = false;
                    entity.setChanged();
                    level.players().forEach(player -> {
                        if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 100) {
                            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                    "§c[ECHO-7]§r Micro Generator has failed due to wear! Restart required."));
                        }
                    });
                }
            }

            // Base failure chance + wear modifier
            double wearPercent = wearData.getWearPercent(pos);
            double adjustedFailureChance = Config.GENERATOR_FAILURE_CHANCE.get() + (wearPercent * 0.005); // Up to 0.5% extra at max wear
            if (RANDOM.nextDouble() < adjustedFailureChance) {
                entity.failed = true;
                entity.burnTimeRemaining = 0;
                isActive = false;
                entity.setChanged();
            }
            
            // Visual feedback based on wear level
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                if (wearPercent > 0.50 && level.getRandom().nextFloat() < 0.1f) {
                    // Spark particles when wear > 50%
                    double px = pos.getX() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.8;
                    double py = pos.getY() + 0.5 + level.getRandom().nextDouble() * 0.5;
                    double pz = pos.getZ() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.8;
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 1, 0, 0, 0, 0.1);
                }
                if (wearPercent > 0.75 && level.getRandom().nextFloat() < 0.15f) {
                    // Smoke particles when wear > 75%
                    double px = pos.getX() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.6;
                    double py = pos.getY() + 0.8;
                    double pz = pos.getZ() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.6;
                    serverLevel.sendParticles(ParticleTypes.SMOKE, px, py, pz, 1, 0, 0.05, 0, 0.02);
                }
                if (wearPercent > 0.90 && level.getRandom().nextFloat() < 0.05f) {
                    // Machine groan sound when approaching jam threshold
                    serverLevel.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.3f, 0.5f);
                }
            }
        } else if (entity.energy < entity.maxEnergy) {
            ItemStack fuel = entity.inventory.getStackInSlot(FUEL_SLOT);
            if (!fuel.isEmpty() && entity.isFuel(fuel)) {
                fuel.shrink(1);
                entity.burnTimeRemaining = BURN_TICKS_PER_FUEL;
                entity.maxBurnTime = BURN_TICKS_PER_FUEL;
                isActive = true;
                entity.setChanged();
            }
        }

        if (wasActive != isActive) {
            level.setBlockAndUpdate(pos, state.setValue(MicroGeneratorBlock.ACTIVE, isActive));
        }

        // Distribute power to adjacent consumers
        if (entity.energy > 0) {
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = pos.relative(dir);
                if (EnergyAccess.transferFromStorageToBlock(entity, level, neighborPos, dir.getOpposite(), OUTPUT_TRANSFER) > 0) {
                    entity.setChanged();
                }
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ValueOutput invOut = output.child("inventory");
        inventory.serialize(invOut);
        output.putInt("energy", energy);
        output.putInt("burnTimeRemaining", burnTimeRemaining);
        output.putInt("maxBurnTime", maxBurnTime);
        output.putBoolean("failed", failed);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("inventory").ifPresent(inv -> inventory.deserialize(inv));
        energy = input.getIntOr("energy", 0);
        burnTimeRemaining = input.getIntOr("burnTimeRemaining", 0);
        maxBurnTime = input.getIntOr("maxBurnTime", 0);
        failed = input.getBooleanOr("failed", false);
    }
}
