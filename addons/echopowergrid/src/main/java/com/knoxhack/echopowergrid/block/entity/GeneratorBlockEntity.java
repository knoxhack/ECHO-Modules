package com.knoxhack.echopowergrid.block.entity;

import com.knoxhack.echopowergrid.api.EchoEnergyStorage;
import com.knoxhack.echopowergrid.api.EchoPowerNode;
import com.knoxhack.echopowergrid.api.EchoPowerNodeType;
import com.knoxhack.echopowergrid.api.EchoPowerQuality;
import com.knoxhack.echopowergrid.api.GeneratorType;
import com.knoxhack.echopowergrid.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class GeneratorBlockEntity extends BlockEntity implements EchoEnergyStorage, EchoPowerNode {
    private long generationRate;
    private long bufferSize;
    private GeneratorType generatorType = GeneratorType.FUEL_BURNER;
    private long energy;
    private int burnTime;
    private int totalBurnTime;
    private int crankCooldown;
    private final FuelInventory fuelInventory = new FuelInventory(this::setChanged);

    public GeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GENERATOR.get(), pos, state);
        if (state.getBlock() instanceof com.knoxhack.echopowergrid.block.GeneratorBlock gen) {
            this.generationRate = gen.getGenerationRate();
            this.bufferSize = gen.getBufferSize();
            this.generatorType = gen.getGeneratorType();
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GeneratorBlockEntity gen) {
        if (level.isClientSide()) return;

        boolean changed = false;

        if (gen.crankCooldown > 0) {
            gen.crankCooldown--;
            changed = true;
        }

        switch (gen.generatorType) {
            case FUEL_BURNER -> changed |= tickFuelBurner(level, gen);
            case SOLAR -> changed |= tickSolar(level, gen);
            case CREATIVE -> changed |= tickCreative(gen);
            case HAND_CRANK -> {
                // Hand crank does not passively generate; it generates on player interaction.
            }
        }

        if (changed) {
            gen.setChanged();
        }
    }

    private static boolean tickFuelBurner(Level level, GeneratorBlockEntity gen) {
        boolean changed = false;
        if (gen.burnTime > 0) {
            gen.burnTime--;
            changed = true;
        }

        long generatedThisTick = gen.getGenerationPerTick();
        if (generatedThisTick > 0 && gen.bufferSize > 0) {
            long toGen = Math.min(generatedThisTick, gen.bufferSize - gen.energy);
            if (toGen > 0) {
                gen.energy += toGen;
                changed = true;
            }
        }

        if (gen.burnTime <= 0 && gen.energy < gen.bufferSize) {
            changed |= gen.tryConsumeFuel();
        }
        return changed;
    }

    private static boolean tickSolar(Level level, GeneratorBlockEntity gen) {
        boolean changed = false;
        long generatedThisTick = gen.getGenerationPerTick();
        if (generatedThisTick > 0 && gen.bufferSize > 0) {
            long toGen = Math.min(generatedThisTick, gen.bufferSize - gen.energy);
            if (toGen > 0) {
                gen.energy += toGen;
                changed = true;
            }
        }
        return changed;
    }

    private static boolean tickCreative(GeneratorBlockEntity gen) {
        if (gen.bufferSize > 0) {
            gen.energy = gen.bufferSize;
            return true;
        }
        return false;
    }

    public void crank(Player player) {
        if (level == null || level.isClientSide()) return;
        if (generatorType != GeneratorType.HAND_CRANK) return;
        if (crankCooldown > 0) {
            player.sendSystemMessage(Component.literal("ECHO GRID // Crank cooling down. Wait " + crankCooldown + " ticks."));
            return;
        }
        long burst = generationRate * 4; // 4 ticks worth per crank
        if (bufferSize > 0) {
            long space = bufferSize - energy;
            long added = Math.min(burst, space);
            if (added > 0) {
                energy += added;
                crankCooldown = 20; // 1 second cooldown
                setChanged();
                player.sendSystemMessage(Component.literal("ECHO GRID // Cranked +" + added + " EP. Buffer: " + energy + "/" + bufferSize));
                return;
            }
        }
        player.sendSystemMessage(Component.literal("ECHO GRID // Crank buffer full."));
    }

    public void onUse(Player player, ItemStack stack) {
        String typeLabel = switch (generatorType) {
            case HAND_CRANK -> "Hand Crank";
            case FUEL_BURNER -> "Fuel Burner";
            case SOLAR -> "Solar";
            case CREATIVE -> "Creative";
        };
        player.sendSystemMessage(Component.literal("ECHO GRID // " + typeLabel + " Generator: " + generationRate + " EP/t. Buffer: " + energy + "/" + bufferSize + " EP."));
        if (generatorType == GeneratorType.FUEL_BURNER) {
            player.sendSystemMessage(Component.literal("  Burn: " + burnTime + "/" + totalBurnTime + " ticks"));
            player.sendSystemMessage(Component.literal("  Fuel slot: " + fuelInventory.getItem(0).getCount() + " item(s)"));
        }
        if (generatorType == GeneratorType.HAND_CRANK) {
            player.sendSystemMessage(Component.literal("  Cooldown: " + crankCooldown + " ticks"));
        }
    }

    public long getGenerationRate() { return generationRate; }
    public GeneratorType getGeneratorType() { return generatorType; }
    public boolean usesFuel() { return generatorType == GeneratorType.FUEL_BURNER; }
    public int getBurnTime() { return burnTime; }
    public int getTotalBurnTime() { return totalBurnTime; }
    public int getCrankCooldown() { return crankCooldown; }
    public SimpleContainer fuelInventory() { return fuelInventory; }

    public static boolean isFuel(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && (stack.is(Items.COAL)
                || stack.is(Items.CHARCOAL)
                || stack.is(Items.OAK_PLANKS)
                || stack.is(Items.SPRUCE_PLANKS)
                || stack.is(Items.BIRCH_PLANKS)
                || stack.is(Items.DARK_OAK_PLANKS)
                || stack.is(Items.DRIED_KELP_BLOCK)
                || stack.is(Items.STICK));
    }

    private boolean tryConsumeFuel() {
        ItemStack fuel = fuelInventory.getItem(0);
        int burn = burnTimeFor(fuel);
        if (burn <= 0) {
            if (totalBurnTime != 0) {
                totalBurnTime = 0;
                return true;
            }
            return false;
        }
        fuel.shrink(1);
        burnTime = burn;
        totalBurnTime = burn;
        setChanged();
        return true;
    }

    private static int burnTimeFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) return 240;
        if (stack.is(Items.DRIED_KELP_BLOCK)) return 600;
        if (stack.is(Items.OAK_PLANKS) || stack.is(Items.SPRUCE_PLANKS)
                || stack.is(Items.BIRCH_PLANKS) || stack.is(Items.DARK_OAK_PLANKS)) return 60;
        if (stack.is(Items.STICK)) return 30;
        return 0;
    }

    public long getGenerationPerTick() {
        return switch (generatorType) {
            case HAND_CRANK -> 0; // Only generates on crank
            case FUEL_BURNER -> burnTime > 0 ? generationRate : 0;
            case CREATIVE -> generationRate;
            case SOLAR -> computeSolarOutput();
        };
    }

    private long computeSolarOutput() {
        if (level == null) return 0;
        if (!level.dimensionType().hasSkyLight()) return 0;
        long dayTime = level.getGameTime() % 24000L;
        if (dayTime >= 13000L) return 0;
        if (!level.canSeeSky(worldPosition.above())) return 0;
        long output = generationRate;
        if (level.isRaining() || level.isThundering()) {
            output = output / 4;
        }
        return output;
    }

    public long getAvailableEnergyForNetwork(int ticks) {
        if (generatorType == GeneratorType.CREATIVE) return Long.MAX_VALUE / 4;
        if (bufferSize <= 0) return saturatedMultiply(getGenerationPerTick(), Math.max(1, ticks));
        return Math.min(energy, saturatedMultiply(getMaxOutput(), Math.max(1, ticks)));
    }

    public long extractEnergyForNetwork(long amount) {
        if (amount <= 0) return 0;
        if (generatorType == GeneratorType.CREATIVE || bufferSize <= 0) {
            return Math.min(amount, Long.MAX_VALUE / 4);
        }
        return extractEnergy(amount, false);
    }

    private static long saturatedMultiply(long value, int multiplier) {
        if (value <= 0 || multiplier <= 0) return 0;
        if (value > Long.MAX_VALUE / multiplier) return Long.MAX_VALUE;
        return value * multiplier;
    }

    public EchoPowerQuality getPowerQuality() {
        return switch (generatorType) {
            case FUEL_BURNER -> EchoPowerQuality.DIRTY;
            case HAND_CRANK -> EchoPowerQuality.DIRTY;
            case SOLAR, CREATIVE -> EchoPowerQuality.STABLE;
        };
    }

    @Override
    public long getEnergyStored() { return energy; }

    @Override
    public long getMaxEnergyStored() { return bufferSize; }

    @Override
    public long receiveEnergy(long amount, boolean simulate) {
        long space = bufferSize - energy;
        long received = Math.min(amount, space);
        if (!simulate) {
            energy += received;
            setChanged();
        }
        return received;
    }

    @Override
    public long extractEnergy(long amount, boolean simulate) {
        long extracted = Math.min(amount, energy);
        if (!simulate) {
            energy -= extracted;
            setChanged();
        }
        return extracted;
    }

    @Override
    public long getMaxInput() { return generationRate; }

    @Override
    public long getMaxOutput() { return bufferSize > 0 ? generationRate : 0; }

    @Override
    public boolean canReceive() { return false; }

    @Override
    public boolean canExtract() { return bufferSize > 0; }

    @Override
    public BlockPos getNodePos() { return worldPosition; }

    @Override
    public ResourceKey<Level> getDimension() { return level != null ? level.dimension() : null; }

    @Override
    public EchoPowerNodeType getNodeType() {
        if (generatorType == GeneratorType.CREATIVE) return EchoPowerNodeType.CREATIVE_SOURCE;
        return EchoPowerNodeType.GENERATOR;
    }

    @Override
    public long getDemandPerTick() { return 0; }

    @Override
    public long getStoredEnergy() { return energy; }

    @Override
    public long getCapacity() { return bufferSize; }

    @Override
    public long getTransferLimit() { return generationRate; }

    @Override
    public boolean isOnline() {
        return switch (generatorType) {
            case HAND_CRANK -> energy > 0;
            case FUEL_BURNER -> burnTime > 0;
            case SOLAR -> getGenerationPerTick() > 0;
            case CREATIVE -> true;
        };
    }

    @Override
    public boolean isOverloaded() { return false; }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("Energy", energy);
        output.putInt("BurnTime", burnTime);
        output.putInt("TotalBurnTime", totalBurnTime);
        output.putInt("CrankCooldown", crankCooldown);
        output.putString("GeneratorType", generatorType.name());
        fuelInventory.serialize(output.child("FuelInventory"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy = input.getLongOr("Energy", 0);
        burnTime = input.getIntOr("BurnTime", 0);
        totalBurnTime = input.getIntOr("TotalBurnTime", 0);
        crankCooldown = input.getIntOr("CrankCooldown", 0);
        String typeName = input.getStringOr("GeneratorType", generatorType.name());
        try {
            generatorType = GeneratorType.valueOf(typeName);
        } catch (IllegalArgumentException ignored) {
            // Fallback: infer from block state if present
            if (getBlockState().getBlock() instanceof com.knoxhack.echopowergrid.block.GeneratorBlock gen) {
                generatorType = gen.getGeneratorType();
            }
        }
        input.child("FuelInventory").ifPresent(fuelInventory::deserialize);
    }

    public void dropFuelContents() {
        if (level != null && !level.isClientSide() && generatorType == GeneratorType.FUEL_BURNER) {
            Containers.dropContents(level, worldPosition, fuelInventory);
            fuelInventory.clearContent();
        }
    }

    private static final class FuelInventory extends SimpleContainer {
        private final Runnable onChanged;

        private FuelInventory(Runnable onChanged) {
            super(1);
            this.onChanged = onChanged;
        }

        public void serialize(ValueOutput output) {
            storeAsItemList(output.list("items", ItemStack.CODEC));
        }

        public void deserialize(ValueInput input) {
            fromItemList(input.listOrEmpty("items", ItemStack.CODEC));
        }

        @Override
        public void setChanged() {
            super.setChanged();
            onChanged.run();
        }
    }
}
