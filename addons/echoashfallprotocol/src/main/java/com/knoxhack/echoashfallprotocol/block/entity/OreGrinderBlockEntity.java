package com.knoxhack.echoashfallprotocol.block.entity;

import com.knoxhack.echoashfallprotocol.block.OreGrinderBlock;
import com.knoxhack.echoashfallprotocol.block.menu.OreGrinderMenu;
import com.knoxhack.echoashfallprotocol.capability.EnergyStorage;
import com.knoxhack.echoashfallprotocol.capability.IEnergyStorage;
import com.knoxhack.echoashfallprotocol.energy.EnergyAccess;
import com.knoxhack.echoashfallprotocol.gameplay.MachineGameplayHelper;
import com.knoxhack.echoashfallprotocol.machine.MachineWearData;
import com.knoxhack.echoashfallprotocol.nativebridge.AshfallAdapterCoreMachineRuntimeHost;
import com.knoxhack.echoashfallprotocol.power.PowerNetwork;
import com.knoxhack.echoashfallprotocol.registry.ModBlockEntities;
import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Substrate Grinder Block Entity - Tier 1 extraction.
 * Converts trace fragments and mined biome substrate into useful resource traces.
 */
public class OreGrinderBlockEntity extends BlockEntity implements MenuProvider, HopperHandler, IEnergyStorage {
    public static final int INPUT_SLOT_1 = 0;
    public static final int INPUT_SLOT_2 = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int BYPRODUCT_SLOT = 3;
    public static final int BATTERY_SLOT = 4;
    private static final int PROCESS_TIME = 80;
    private static final int POWER_PER_OP = 200;
    private static final int ENERGY_CAPACITY = 2_000;
    private static final int ENERGY_TRANSFER = 128;

    private final MachineInventory inventory = new MachineInventory(5, this::setChanged);
    private final EnergyStorage energyStorage = new EnergyStorage(ENERGY_CAPACITY, ENERGY_TRANSFER, ENERGY_TRANSFER);
    private int progress = 0;
    private int maxProgress = PROCESS_TIME;
    private int wearPercent = 0;
    private boolean jammed = false;

    private static final Map<Item, GrinderRecipe> GRINDER_RECIPES = buildRecipeTable();

    private boolean hasPower = false;

    public final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> hasPower ? 1 : 0;
                case 3 -> wearPercent;
                case 4 -> jammed ? 1 : 0;
                case 5 -> energyStorage.getEnergyStored();
                case 6 -> energyStorage.getMaxEnergyStored();
                default -> 0;
            };
        }
        @Override public void set(int i, int v) {
            switch (i) {
                case 0 -> progress = v;
                case 1 -> maxProgress = v;
                case 2 -> hasPower = v != 0;
                case 3 -> wearPercent = v;
                case 4 -> jammed = v != 0;
                case 5 -> energyStorage.setEnergyStored(v);
                case 6 -> {
                }
            }
        }
        @Override public int getCount() { return 7; }
    };

    public OreGrinderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ORE_GRINDER.get(), pos, state);
    }

    public MachineInventory getInventory() { return inventory; }

    public static boolean hasSubstrateRecipe(ItemStack stack) {
        return findRecipeForItem(stack) != null;
    }

    public static boolean hasSubstrateRecipe(Item item) {
        return GRINDER_RECIPES.containsKey(item);
    }

    @Nullable
    public static GrinderRecipe getSubstrateRecipe(ItemStack stack) {
        return findRecipeForItem(stack);
    }

    @Nullable
    public static GrinderRecipe getSubstrateRecipe(Item item) {
        return GRINDER_RECIPES.get(item);
    }

    public static Map<Item, GrinderRecipe> getSubstrateRecipes() {
        return GRINDER_RECIPES;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Substrate Grinder");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new OreGrinderMenu(id, inv, this, data);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, OreGrinderBlockEntity entity) {
        boolean wasActive = state.getValue(OreGrinderBlock.ACTIVE);
        boolean isProcessing = false;
        EnergyAccess.dischargeBatteryToStorage(entity.inventory.getStackInSlot(BATTERY_SLOT), entity);
        MachineWearData wearData = new MachineWearData(level);

        entity.wearPercent = (int) (wearData.getWearPercent(pos) * 100);
        entity.jammed = wearData.isJammed(pos);

        GrinderRecipe recipe = entity.getActiveRecipe();
        int baseProcessTime = recipe != null ? recipe.processTime() : PROCESS_TIME;
        entity.maxProgress = MachineGameplayHelper.getAdjustedProcessTime(level, pos, baseProcessTime);
        int powerPerOperation = recipe != null ? recipe.powerPerOperation() : POWER_PER_OP;
        int powerPerTick = Math.max(1, powerPerOperation / Math.max(1, entity.maxProgress));
        entity.hasPower = recipe != null && !entity.jammed && EnergyAccess.tryConsumeLocalOrNetworkPower(entity, level, pos,
                MachineGameplayHelper.getAdjustedPowerCost(level, pos, powerPerTick));

        if (recipe != null && entity.hasPower && !entity.jammed) {
            isProcessing = true;
            entity.progress++;
            if (entity.progress % 16 == 0 && level instanceof ServerLevel serverLevel) {
                entity.emitProcessingFeedback(serverLevel, pos, recipe);
            }
            entity.setChanged();

            if (entity.progress % 20 == 0) {
                wearData.addWear(pos, 1, level.getRandom());
                if (wearData.checkJamChance(pos, level.getRandom())) {
                    entity.jammed = true;
                    level.players().forEach(player -> {
                        if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 100) {
                            player.sendSystemMessage(Component.literal(
                                    "§c[ECHO-7]§r Substrate Grinder has jammed! Scrap repairs are required."));
                        }
                    });
                }
            }

            if (entity.progress >= entity.maxProgress) {
                entity.craftItem(level, recipe);
                entity.progress = 0;
                // Try to chain outputs to adjacent machines
                entity.tryPushOutputToNeighbors(level, pos);
            }
        } else if (!entity.hasPower || entity.jammed) {
            entity.progress = 0;
        }

        if (wasActive != isProcessing) {
            level.setBlockAndUpdate(pos, state.setValue(OreGrinderBlock.ACTIVE, isProcessing));
        }
    }

    private void emitProcessingFeedback(ServerLevel level, BlockPos pos, GrinderRecipe recipe) {
        double px = pos.getX() + 0.5D + (level.getRandom().nextDouble() - 0.5D) * 0.45D;
        double py = pos.getY() + 0.92D;
        double pz = pos.getZ() + 0.5D + (level.getRandom().nextDouble() - 0.5D) * 0.45D;
        level.sendParticles(particleForRecipe(recipe), px, py, pz, 2, 0.12D, 0.06D, 0.12D, 0.015D);
        if (progress % 48 == 0) {
            level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.22F, pitchForRecipe(recipe));
        }
    }

    private static ParticleOptions particleForRecipe(GrinderRecipe recipe) {
        Item input = recipe.input();
        if (input == ModBlocks.TOXIC_SLAGSTONE.get().asItem()) {
            return ParticleTypes.HAPPY_VILLAGER;
        }
        if (input == ModBlocks.IRRADIATED_CRUST.get().asItem() || input == ModBlocks.IRRADIATED_SHALE.get().asItem()) {
            return ParticleTypes.ELECTRIC_SPARK;
        }
        if (input == ModBlocks.CRYOGENIC_FRACTURED_STONE.get().asItem()) {
            return ParticleTypes.SNOWFLAKE;
        }
        if (input == ModBlocks.NEXUS_CRACKED_SOIL.get().asItem() || input == ModBlocks.NEXUS_SCAR_STONE.get().asItem()) {
            return ParticleTypes.PORTAL;
        }
        if (input == ModBlocks.ASH_STONE.get().asItem() || input == ModBlocks.DEEP_ASH.get().asItem()) {
            return ParticleTypes.ASH;
        }
        return ParticleTypes.SMOKE;
    }

    private static float pitchForRecipe(GrinderRecipe recipe) {
        Item input = recipe.input();
        if (input == ModBlocks.CRYOGENIC_FRACTURED_STONE.get().asItem()) {
            return 1.35F;
        }
        if (input == ModBlocks.NEXUS_CRACKED_SOIL.get().asItem() || input == ModBlocks.NEXUS_SCAR_STONE.get().asItem()) {
            return 0.72F;
        }
        if (input == ModBlocks.IRRADIATED_CRUST.get().asItem()
                || input == ModBlocks.IRRADIATED_SHALE.get().asItem()
                || input == ModBlocks.TOXIC_SLAGSTONE.get().asItem()) {
            return 1.15F;
        }
        return 0.88F;
    }

    public boolean hasRecipe() {
        return getActiveRecipe() != null;
    }

    private GrinderRecipe getActiveRecipe() {
        for (int slot : new int[]{INPUT_SLOT_1, INPUT_SLOT_2}) {
            ItemStack active = inventory.getStackInSlot(slot);
            GrinderRecipe recipe = findReadyRecipeForInput(active);
            if (recipe == null) {
                continue;
            }

            ItemStack out = inventory.getStackInSlot(OUTPUT_SLOT);
            if (!canFit(out, recipe.output(), recipe.outputCountFor(active))) {
                continue;
            }

            if (recipe.byproduct() != null) {
                ItemStack byproduct = inventory.getStackInSlot(BYPRODUCT_SLOT);
                if (!canFit(byproduct, recipe.byproduct(), recipe.byproductCount())) {
                    continue;
                }
            }

            return recipe;
        }

        return null;
    }

    private void craftItem(Level level, GrinderRecipe recipe) {
        ItemStack active = ItemStack.EMPTY;
        for (int slot : new int[]{INPUT_SLOT_1, INPUT_SLOT_2}) {
            ItemStack candidate = inventory.getStackInSlot(slot);
            if (recipe.matches(candidate)) {
                active = candidate;
                break;
            }
        }
        if (active.isEmpty()) return;

        int toConsume = recipe.consumedCount(active);
        active.shrink(toConsume);

        int outputCount = recipe.outputCountFor(toConsume);
        ItemStack out = inventory.getStackInSlot(OUTPUT_SLOT);
        ItemStack producedOutput = new ItemStack(recipe.output(), outputCount);
        if (out.isEmpty()) {
            inventory.setStackInSlot(OUTPUT_SLOT, producedOutput.copy());
        } else {
            out.grow(outputCount);
        }
        AshfallAdapterCoreMachineRuntimeHost.outputCreated(level, worldPosition, this, producedOutput);

        if (recipe.byproduct() != null && level.getRandom().nextFloat() < recipe.byproductChance()) {
            ItemStack producedByproduct = new ItemStack(recipe.byproduct(), recipe.byproductCount());
            ItemStack byproduct = inventory.getStackInSlot(BYPRODUCT_SLOT);
            if (byproduct.isEmpty()) {
                inventory.setStackInSlot(BYPRODUCT_SLOT, producedByproduct.copy());
            } else if (byproduct.is(recipe.byproduct()) && byproduct.getCount() + recipe.byproductCount() <= byproduct.getMaxStackSize()) {
                byproduct.grow(recipe.byproductCount());
            }
            AshfallAdapterCoreMachineRuntimeHost.outputCreated(level, worldPosition, this, producedByproduct);
        }
    }

    private static boolean canFit(ItemStack stack, Item item, int count) {
        if (stack.isEmpty()) {
            return count <= new ItemStack(item).getMaxStackSize();
        }
        return stack.is(item) && stack.getCount() + count <= stack.getMaxStackSize();
    }

    @Nullable
    private static GrinderRecipe findRecipeForItem(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return GRINDER_RECIPES.get(stack.getItem());
    }

    @Nullable
    private static GrinderRecipe findReadyRecipeForInput(ItemStack stack) {
        GrinderRecipe recipe = findRecipeForItem(stack);
        if (recipe == null || !recipe.matches(stack)) {
            return null;
        }
        return recipe;
    }

    private static Map<Item, GrinderRecipe> buildRecipeTable() {
        Map<Item, GrinderRecipe> recipes = new LinkedHashMap<>();

        registerFragment(recipes, ModItems.IRON_SHARD.get(), Items.IRON_INGOT);
        registerFragment(recipes, ModItems.COPPER_SHARD.get(), Items.COPPER_INGOT);
        registerFragment(recipes, ModItems.COAL_DUST.get(), Items.COAL);
        registerFragment(recipes, ModItems.GOLD_TRACE.get(), Items.GOLD_NUGGET);
        registerFragment(recipes, ModItems.GOLD_CLUSTER.get(), Items.GOLD_INGOT);
        registerFragment(recipes, ModItems.URANIUM_SHARD.get(), Items.RAW_IRON);

        register(recipes, Items.STONE, 4, Items.GRAVEL, 4, Items.FLINT, 1, 0.10f, 80, 180);
        register(recipes, Items.COBBLESTONE, 4, Items.GRAVEL, 4, Items.FLINT, 1, 0.10f, 80, 180);
        register(recipes, Items.DEEPSLATE, 3, ModItems.COAL_DUST.get(), 2, ModItems.IRON_SHARD.get(), 1, 0.20f, 100, 240);
        register(recipes, Items.COBBLED_DEEPSLATE, 3, ModItems.COAL_DUST.get(), 2, ModItems.IRON_SHARD.get(), 1, 0.20f, 100, 240);

        register(recipes, ModBlocks.WASTELAND_STONE.get().asItem(), 3, ModItems.IRON_SHARD.get(), 2, ModItems.COAL_DUST.get(), 1, 0.25f, 100, 250);
        register(recipes, ModBlocks.WASTELAND_TRACE_RUBBLE.get().asItem(), 2, ModItems.IRON_SHARD.get(), 2, ModItems.COPPER_SHARD.get(), 1, 0.30f, 90, 260);
        register(recipes, ModBlocks.SCRAP_ORE.get().asItem(), 2, ModItems.SCRAP_METAL.get(), 3, ModItems.IRON_SHARD.get(), 1, 0.35f, 90, 240);
        register(recipes, ModBlocks.RUBBLE.get().asItem(), 3, Items.GRAVEL, 4, ModItems.SCRAP_METAL.get(), 1, 0.15f, 80, 200);
        register(recipes, ModBlocks.SCATTERED_BONES.get().asItem(), 1, ModItems.ASHBONE_SHARD.get(), 3, Items.BONE_MEAL, 1, 0.25f, 80, 180);
        register(recipes, ModBlocks.CONCRETE_RUBBLE.get().asItem(), 3, Items.GRAVEL, 4, ModItems.SCRAP_METAL.get(), 1, 0.20f, 90, 220);
        register(recipes, ModBlocks.CONCRETE_CHUNK.get().asItem(), 2, Items.GRAVEL, 4, ModItems.SCRAP_METAL.get(), 1, 0.30f, 100, 240);
        register(recipes, ModBlocks.INDUSTRIAL_AGGREGATE.get().asItem(), 2, ModItems.COPPER_SHARD.get(), 2, ModItems.SCRAP_WIRE.get(), 1, 0.30f, 110, 300);
        register(recipes, ModBlocks.OIL_STAINED_CONCRETE.get().asItem(), 2, ModItems.SCRAP_PLASTIC.get(), 2, ModItems.COAL_DUST.get(), 1, 0.35f, 110, 300);
        register(recipes, ModBlocks.CRASH_SLAG.get().asItem(), 2, ModItems.SCRAP_METAL.get(), 2, ModItems.IRON_SHARD.get(), 1, 0.35f, 110, 320);
        register(recipes, ModBlocks.ASH_STONE.get().asItem(), 3, ModItems.COAL_DUST.get(), 2, ModItems.ASH.get(), 1, 0.35f, 100, 240);
        register(recipes, ModBlocks.DEEP_ASH.get().asItem(), 2, Items.SAND, 2, ModItems.COAL_DUST.get(), 1, 0.25f, 90, 220);
        register(recipes, ModBlocks.TOXIC_SLAGSTONE.get().asItem(), 2, ModItems.COAL_DUST.get(), 2, ModItems.CHARGED_ASH_CIRCUIT.get(), 1, 0.25f, 120, 350);
        register(recipes, ModBlocks.IRRADIATED_CRUST.get().asItem(), 3, ModItems.URANIUM_SHARD.get(), 1, ModBlocks.FALLOUT_DUST_ITEM.get(), 1, 0.35f, 120, 360);
        register(recipes, ModBlocks.IRRADIATED_SHALE.get().asItem(), 2, ModItems.URANIUM_SHARD.get(), 1, ModItems.CRYSTAL_DUST.get(), 1, 0.30f, 140, 420);
        register(recipes, ModBlocks.CRYOGENIC_FRACTURED_STONE.get().asItem(), 2, ModItems.CRYSTAL_DUST.get(), 1, ModItems.SCRAP_CIRCUIT.get(), 1, 0.25f, 140, 400);
        register(recipes, ModBlocks.NEXUS_CRACKED_SOIL.get().asItem(), 3, ModItems.CRYSTAL_DUST.get(), 2, ModItems.GEM_FRAGMENT.get(), 1, 0.15f, 160, 500);
        register(recipes, ModBlocks.NEXUS_SCAR_STONE.get().asItem(), 2, ModItems.GEM_FRAGMENT.get(), 1, ModItems.CRYSTAL_DUST.get(), 1, 0.35f, 180, 650);

        return Collections.unmodifiableMap(recipes);
    }

    private static void registerFragment(Map<Item, GrinderRecipe> recipes, Item input, Item output) {
        recipes.put(input, new GrinderRecipe(input, 4, output, 4,
                ModItems.CRYSTAL_DUST.get(), 1, 0.15f, PROCESS_TIME, POWER_PER_OP, true));
    }

    private static void register(Map<Item, GrinderRecipe> recipes, Item input, int inputCount, Item output, int outputCount,
                                 @Nullable Item byproduct, int byproductCount, float byproductChance,
                                 int processTime, int powerPerOperation) {
        recipes.put(input, new GrinderRecipe(input, inputCount, output, outputCount,
                byproduct, byproductCount, byproductChance, processTime, powerPerOperation, false));
    }

    public record GrinderRecipe(
            Item input,
            int inputCount,
            Item output,
            int outputCount,
            @Nullable Item byproduct,
            int byproductCount,
            float byproductChance,
            int processTime,
            int powerPerOperation,
            boolean partialBatch
    ) {
        public boolean matches(ItemStack stack) {
            if (stack.isEmpty() || !stack.is(input)) {
                return false;
            }
            return partialBatch ? stack.getCount() >= 1 : stack.getCount() >= inputCount;
        }

        public int consumedCount(ItemStack stack) {
            return partialBatch ? Math.min(inputCount, stack.getCount()) : inputCount;
        }

        public int outputCountFor(ItemStack stack) {
            return outputCountFor(consumedCount(stack));
        }

        public int outputCountFor(int consumedCount) {
            return partialBatch ? consumedCount : outputCount;
        }

        public String categoryLabel() {
            if (partialBatch) {
                return "Trace fragment";
            }
            if (input == Items.STONE || input == Items.COBBLESTONE) {
                return "Common filler";
            }
            if (input == Items.DEEPSLATE || input == Items.COBBLED_DEEPSLATE) {
                return "Deep substrate";
            }
            if (input == ModBlocks.TOXIC_SLAGSTONE.get().asItem()
                    || input == ModBlocks.IRRADIATED_CRUST.get().asItem()
                    || input == ModBlocks.IRRADIATED_SHALE.get().asItem()
                    || input == ModBlocks.CRYOGENIC_FRACTURED_STONE.get().asItem()) {
                return "Hazard substrate";
            }
            if (input == ModBlocks.NEXUS_CRACKED_SOIL.get().asItem()
                    || input == ModBlocks.NEXUS_SCAR_STONE.get().asItem()) {
                return "Anomaly substrate";
            }
            return "Trace substrate";
        }

        public String handlingHint() {
            return switch (categoryLabel()) {
                case "Common filler" -> "Bulk filler route; not an ingot shortcut.";
                case "Deep substrate" -> "Slow stone route with coal and trace iron.";
                case "Hazard substrate" -> "Higher power cost; yields rare controlled traces.";
                case "Anomaly substrate" -> "Late anomaly feedstock for crystal work.";
                case "Trace fragment" -> "Refines old trace fragments into clean materials.";
                default -> "Mine, keep, and grind for trace resources.";
            };
        }
    }

    /**
     * Try to push output items to adjacent machines (machine chaining).
     */
    private void tryPushOutputToNeighbors(Level level, BlockPos pos) {
        MachineChainingHelper.tryPushMultipleOutputs(level, pos, inventory,
                new int[]{OUTPUT_SLOT, BYPRODUCT_SLOT});
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        inventory.serialize(out.child("inventory"));
        out.putInt("progress", progress);
        out.putBoolean("jammed", jammed);
        out.putInt("energy", energyStorage.getEnergyStored());
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("inventory").ifPresent(inv -> inventory.deserialize(inv));
        progress = in.getIntOr("progress", 0);
        jammed = in.getBooleanOr("jammed", false);
        energyStorage.setEnergyStored(in.getIntOr("energy", 0));
    }

    @Override public int getEnergyStored() { return energyStorage.getEnergyStored(); }
    @Override public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }
    @Override public int receiveEnergy(int amount, boolean simulate) {
        int received = energyStorage.receiveEnergy(amount, simulate);
        if (received > 0 && !simulate) setChanged();
        return received;
    }
    @Override public int extractEnergy(int amount, boolean simulate) {
        int extracted = energyStorage.extractEnergy(amount, simulate);
        if (extracted > 0 && !simulate) setChanged();
        return extracted;
    }
    @Override public boolean canReceive() { return true; }
    @Override public boolean canExtract() { return energyStorage.getEnergyStored() > 0; }
    @Override public void setEnergyStored(int energy) {
        energyStorage.setEnergyStored(energy);
        setChanged();
    }

    // === HopperHandler Implementation ===
    @Override
    public int[] getInputSlots(Direction side) {
        // Accept shards from top and sides
        return new int[]{INPUT_SLOT_1, INPUT_SLOT_2};
    }

    @Override
    public int[] getOutputSlots(Direction side) {
        // Extract products from bottom only
        return side == Direction.DOWN ? new int[]{OUTPUT_SLOT, BYPRODUCT_SLOT} : new int[]{};
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack) {
        if (slot == INPUT_SLOT_1 || slot == INPUT_SLOT_2) {
            return hasSubstrateRecipe(stack);
        }
        return false;
    }

    @Override
    public boolean canExtractItem(int slot) {
        return slot == OUTPUT_SLOT || slot == BYPRODUCT_SLOT;
    }
}
