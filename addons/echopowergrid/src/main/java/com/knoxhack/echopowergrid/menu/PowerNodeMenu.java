package com.knoxhack.echopowergrid.menu;

import com.knoxhack.echopowergrid.api.EchoPowerGridApi;
import com.knoxhack.echopowergrid.api.EchoPowerNetwork;
import com.knoxhack.echopowergrid.api.PowerGridSnapshot;
import com.knoxhack.echopowergrid.block.BatteryBlock;
import com.knoxhack.echopowergrid.block.BreakerBlock;
import com.knoxhack.echopowergrid.block.ConsumerBlock;
import com.knoxhack.echopowergrid.block.GeneratorBlock;
import com.knoxhack.echopowergrid.block.MeterBlock;
import com.knoxhack.echopowergrid.block.SubstationBlock;
import com.knoxhack.echopowergrid.block.entity.BatteryBlockEntity;
import com.knoxhack.echopowergrid.block.entity.GeneratorBlockEntity;
import com.knoxhack.echopowergrid.block.entity.PowerConsumerBlockEntity;
import com.knoxhack.echopowergrid.registry.ModBlocks;
import com.knoxhack.echopowergrid.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class PowerNodeMenu extends AbstractContainerMenu {
    public static final int GUI_WIDTH = 352;
    public static final int GUI_HEIGHT = 286;
    public static final int STATUS_WIDTH = 320;
    public static final int STATUS_HEIGHT = 212;
    public static final int FUEL_SLOT = 0;
    public static final int FUEL_X = 44;
    public static final int FUEL_Y = 92;
    public static final int PLAYER_INV_X = 95;
    public static final int PLAYER_INV_Y = 150;

    public static final int BUTTON_RESET_BREAKER = 0;
    public static final int BUTTON_REFRESH_GRID = 1;

    public static final int KIND_UNKNOWN = 0;
    public static final int KIND_GENERATOR = 1;
    public static final int KIND_BATTERY = 2;
    public static final int KIND_CONSUMER = 3;
    public static final int KIND_SUBSTATION = 4;
    public static final int KIND_METER = 5;
    public static final int KIND_BREAKER = 6;

    public static final int FLAG_USES_FUEL = 1;
    public static final int FLAG_ONLINE = 1 << 1;
    public static final int FLAG_TRIPPED = 1 << 2;
    public static final int FLAG_POWERED = 1 << 3;
    public static final int FLAG_CREATIVE = 1 << 4;

    private static final int DATA_KIND = 0;
    private static final int DATA_FLAGS = 1;
    private static final int DATA_NODE_COUNT = 2;
    private static final int DATA_STATE = 3;
    private static final int DATA_QUALITY = 4;
    private static final int DATA_BURN_TIME = 5;
    private static final int DATA_TOTAL_BURN_TIME = 6;
    private static final int DATA_NETWORK_GENERATION = 7;
    private static final int DATA_NETWORK_DEMAND = 9;
    private static final int DATA_NETWORK_STORED = 11;
    private static final int DATA_NETWORK_CAPACITY = 13;
    private static final int DATA_NETWORK_AVAILABLE = 15;
    private static final int DATA_TRANSFER_LIMIT = 17;
    private static final int DATA_LOCAL_ENERGY = 19;
    private static final int DATA_LOCAL_CAPACITY = 21;
    private static final int DATA_LOCAL_INPUT = 23;
    private static final int DATA_LOCAL_OUTPUT = 25;
    private static final int DATA_LOCAL_DEMAND = 27;
    private static final int DATA_LOCAL_GENERATION = 29;
    public static final int DATA_COUNT = 31;

    private static final int PLAYER_INV_START = 1;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    private static final int HOTBAR_END = PLAYER_INV_END + 9;

    private final Level level;
    private final BlockPos pos;
    private final ContainerData data;
    private final @Nullable Container fuelContainer;

    public PowerNodeMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, playerInventory.player.level(), buf.readBlockPos(), new SimpleContainerData(DATA_COUNT));
    }

    public PowerNodeMenu(int containerId, Inventory playerInventory, Level level, BlockPos pos) {
        this(containerId, playerInventory, level, pos, new LiveData(level, pos));
    }

    private PowerNodeMenu(int containerId, Inventory playerInventory, Level level, BlockPos pos, ContainerData data) {
        super(ModMenus.POWER_NODE.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.level = level;
        this.pos = pos == null ? BlockPos.ZERO : pos.immutable();
        this.data = data;
        this.fuelContainer = fuelContainer(level, this.pos);

        if (hasFuelSlot()) {
            addSlot(new Slot(fuelContainer, FUEL_SLOT, FUEL_X, FUEL_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return GeneratorBlockEntity.isFuel(stack);
                }
            });
            addStandardInventorySlots(playerInventory, PLAYER_INV_X, PLAYER_INV_Y);
        }
        addDataSlots(data);
    }

    public static MenuProvider provider(Level level, BlockPos pos) {
        return new Provider(level, pos);
    }

    public BlockPos pos() {
        return pos;
    }

    public boolean hasFuelSlot() {
        return fuelContainer != null;
    }

    public int kind() {
        return data.get(DATA_KIND);
    }

    public int flags() {
        return data.get(DATA_FLAGS);
    }

    public boolean usesFuel() {
        return hasFlag(FLAG_USES_FUEL);
    }

    public boolean isOnline() {
        return hasFlag(FLAG_ONLINE);
    }

    public boolean isTripped() {
        return hasFlag(FLAG_TRIPPED);
    }

    public boolean isPowered() {
        return hasFlag(FLAG_POWERED);
    }

    public boolean isCreative() {
        return hasFlag(FLAG_CREATIVE);
    }

    public int nodeCount() {
        return data.get(DATA_NODE_COUNT);
    }

    public int stateId() {
        return data.get(DATA_STATE);
    }

    public int qualityId() {
        return data.get(DATA_QUALITY);
    }

    public int burnTime() {
        return data.get(DATA_BURN_TIME);
    }

    public int totalBurnTime() {
        return data.get(DATA_TOTAL_BURN_TIME);
    }

    public long networkGeneration() {
        return longValue(DATA_NETWORK_GENERATION);
    }

    public long networkDemand() {
        return longValue(DATA_NETWORK_DEMAND);
    }

    public long networkStored() {
        return longValue(DATA_NETWORK_STORED);
    }

    public long networkCapacity() {
        return longValue(DATA_NETWORK_CAPACITY);
    }

    public long networkAvailable() {
        return longValue(DATA_NETWORK_AVAILABLE);
    }

    public long transferLimit() {
        return longValue(DATA_TRANSFER_LIMIT);
    }

    public long localEnergy() {
        return longValue(DATA_LOCAL_ENERGY);
    }

    public long localCapacity() {
        return longValue(DATA_LOCAL_CAPACITY);
    }

    public long localInput() {
        return longValue(DATA_LOCAL_INPUT);
    }

    public long localOutput() {
        return longValue(DATA_LOCAL_OUTPUT);
    }

    public long localDemand() {
        return longValue(DATA_LOCAL_DEMAND);
    }

    public long localGeneration() {
        return longValue(DATA_LOCAL_GENERATION);
    }

    public String kindName() {
        return switch (kind()) {
            case KIND_GENERATOR -> "Generator";
            case KIND_BATTERY -> "Battery Bank";
            case KIND_CONSUMER -> "Consumer";
            case KIND_SUBSTATION -> "Substation";
            case KIND_METER -> "Power Meter";
            case KIND_BREAKER -> "Emergency Breaker";
            default -> "Power Node";
        };
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (!hasFuelSlot() || slotIndex < 0 || slotIndex >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack current = slot.getItem();
        ItemStack original = current.copy();
        if (slotIndex == FUEL_SLOT) {
            if (!moveItemStackTo(current, PLAYER_INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (GeneratorBlockEntity.isFuel(current)) {
            if (!moveItemStackTo(current, FUEL_SLOT, FUEL_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (current.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null) {
            return true;
        }
        BlockState state = level.getBlockState(pos);
        return ModBlocks.isPowerNode(state)
                && stillValid(ContainerLevelAccess.create(level, pos), player, state.getBlock());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (level == null || level.isClientSide()) {
            return false;
        }
        if (id == BUTTON_REFRESH_GRID) {
            EchoPowerGridApi.markNetworkDirty(level, pos);
            player.sendSystemMessage(Component.literal("ECHO GRID // Local grid refresh queued."));
            return true;
        }
        if (id == BUTTON_RESET_BREAKER) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof BreakerBlock && state.getValue(BreakerBlock.TRIPPED)) {
                level.setBlock(pos, state.setValue(BreakerBlock.TRIPPED, false), 3);
                EchoPowerGridApi.markNetworkDirty(level, pos);
                player.sendSystemMessage(Component.literal("ECHO GRID // Breaker reset. Circuit restored."));
                return true;
            }
        }
        return false;
    }

    private boolean hasFlag(int flag) {
        return (flags() & flag) != 0;
    }

    private long longValue(int index) {
        return Integer.toUnsignedLong(data.get(index))
                | (Integer.toUnsignedLong(data.get(index + 1)) << 32);
    }

    private static @Nullable Container fuelContainer(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof GeneratorBlockEntity generator && generator.usesFuel()) {
            return generator.fuelInventory();
        }
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof GeneratorBlock generator && generator.usesFuel()) {
            return new SimpleContainer(1);
        }
        return null;
    }

    private static int kindOf(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return KIND_UNKNOWN;
        }
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof GeneratorBlock) return KIND_GENERATOR;
        if (state.getBlock() instanceof BatteryBlock) return KIND_BATTERY;
        if (state.getBlock() instanceof ConsumerBlock) return KIND_CONSUMER;
        if (state.getBlock() instanceof SubstationBlock) return KIND_SUBSTATION;
        if (state.getBlock() instanceof MeterBlock) return KIND_METER;
        if (state.getBlock() instanceof BreakerBlock) return KIND_BREAKER;
        return KIND_UNKNOWN;
    }

    private static final class Provider implements MenuProvider {
        private final Level level;
        private final BlockPos pos;

        private Provider(Level level, BlockPos pos) {
            this.level = level;
            this.pos = pos == null ? BlockPos.ZERO : pos.immutable();
        }

        @Override
        public Component getDisplayName() {
            if (level != null) {
                return level.getBlockState(pos).getBlock().getName();
            }
            return Component.literal("Power Node");
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new PowerNodeMenu(containerId, playerInventory, level, pos);
        }
    }

    private static final class LiveData implements ContainerData {
        private final Level level;
        private final BlockPos pos;

        private LiveData(Level level, BlockPos pos) {
            this.level = level;
            this.pos = pos == null ? BlockPos.ZERO : pos.immutable();
        }

        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_KIND -> kindOf(level, pos);
                case DATA_FLAGS -> flags();
                case DATA_NODE_COUNT -> snapshot().nodeCount();
                case DATA_STATE -> snapshot().state().ordinal();
                case DATA_QUALITY -> snapshot().quality().ordinal();
                case DATA_BURN_TIME -> blockEntity() instanceof GeneratorBlockEntity generator ? generator.getBurnTime() : 0;
                case DATA_TOTAL_BURN_TIME -> blockEntity() instanceof GeneratorBlockEntity generator ? generator.getTotalBurnTime() : 0;
                case DATA_NETWORK_GENERATION, DATA_NETWORK_GENERATION + 1 -> packed(index, DATA_NETWORK_GENERATION, snapshot().totalGeneration());
                case DATA_NETWORK_DEMAND, DATA_NETWORK_DEMAND + 1 -> packed(index, DATA_NETWORK_DEMAND, snapshot().totalDemand());
                case DATA_NETWORK_STORED, DATA_NETWORK_STORED + 1 -> packed(index, DATA_NETWORK_STORED, snapshot().totalStored());
                case DATA_NETWORK_CAPACITY, DATA_NETWORK_CAPACITY + 1 -> packed(index, DATA_NETWORK_CAPACITY, snapshot().totalCapacity());
                case DATA_NETWORK_AVAILABLE, DATA_NETWORK_AVAILABLE + 1 -> packed(index, DATA_NETWORK_AVAILABLE, snapshot().availablePower());
                case DATA_TRANSFER_LIMIT, DATA_TRANSFER_LIMIT + 1 -> packed(index, DATA_TRANSFER_LIMIT, transferLimit());
                case DATA_LOCAL_ENERGY, DATA_LOCAL_ENERGY + 1 -> packed(index, DATA_LOCAL_ENERGY, localEnergy());
                case DATA_LOCAL_CAPACITY, DATA_LOCAL_CAPACITY + 1 -> packed(index, DATA_LOCAL_CAPACITY, localCapacity());
                case DATA_LOCAL_INPUT, DATA_LOCAL_INPUT + 1 -> packed(index, DATA_LOCAL_INPUT, localInput());
                case DATA_LOCAL_OUTPUT, DATA_LOCAL_OUTPUT + 1 -> packed(index, DATA_LOCAL_OUTPUT, localOutput());
                case DATA_LOCAL_DEMAND, DATA_LOCAL_DEMAND + 1 -> packed(index, DATA_LOCAL_DEMAND, localDemand());
                case DATA_LOCAL_GENERATION, DATA_LOCAL_GENERATION + 1 -> packed(index, DATA_LOCAL_GENERATION, localGeneration());
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }

        private int flags() {
            int flags = 0;
            BlockEntity blockEntity = blockEntity();
            if (blockEntity instanceof GeneratorBlockEntity generator) {
                if (generator.usesFuel()) flags |= FLAG_USES_FUEL;
                if (generator.isOnline()) flags |= FLAG_ONLINE;
                if (generator.getGenerationRate() >= Long.MAX_VALUE / 8) flags |= FLAG_CREATIVE;
            } else if (blockEntity instanceof BatteryBlockEntity battery) {
                if (battery.isOnline()) flags |= FLAG_ONLINE;
            } else if (blockEntity instanceof PowerConsumerBlockEntity consumer) {
                if (consumer.isOnline()) flags |= FLAG_ONLINE;
            }
            if (snapshot().isPowered()) {
                flags |= FLAG_POWERED;
            }
            BlockState state = state();
            if (state.getBlock() instanceof BreakerBlock && state.getValue(BreakerBlock.TRIPPED)) {
                flags |= FLAG_TRIPPED;
            }
            return flags;
        }

        private long localEnergy() {
            BlockEntity blockEntity = blockEntity();
            if (blockEntity instanceof GeneratorBlockEntity generator) return generator.getEnergyStored();
            if (blockEntity instanceof BatteryBlockEntity battery) return battery.getEnergyStored();
            return 0L;
        }

        private long localCapacity() {
            BlockEntity blockEntity = blockEntity();
            if (blockEntity instanceof GeneratorBlockEntity generator) return generator.getMaxEnergyStored();
            if (blockEntity instanceof BatteryBlockEntity battery) return battery.getMaxEnergyStored();
            return 0L;
        }

        private long localInput() {
            BlockEntity blockEntity = blockEntity();
            if (blockEntity instanceof GeneratorBlockEntity generator) return generator.getMaxInput();
            if (blockEntity instanceof BatteryBlockEntity battery) return battery.getMaxInput();
            return 0L;
        }

        private long localOutput() {
            BlockEntity blockEntity = blockEntity();
            if (blockEntity instanceof GeneratorBlockEntity generator) return generator.getMaxOutput();
            if (blockEntity instanceof BatteryBlockEntity battery) return battery.getMaxOutput();
            return 0L;
        }

        private long localDemand() {
            BlockEntity blockEntity = blockEntity();
            return blockEntity instanceof PowerConsumerBlockEntity consumer ? consumer.getDemandPerTick() : 0L;
        }

        private long localGeneration() {
            BlockEntity blockEntity = blockEntity();
            return blockEntity instanceof GeneratorBlockEntity generator ? generator.getGenerationPerTick() : 0L;
        }

        private long transferLimit() {
            if (level == null) {
                return 0L;
            }
            EchoPowerNetwork network = EchoPowerGridApi.getNetwork(level, pos).orElse(null);
            if (network != null) {
                return network.transferLimit;
            }
            return ModBlocks.getTransferLimit(state());
        }

        private PowerGridSnapshot snapshot() {
            return EchoPowerGridApi.getSnapshot(level, pos);
        }

        private BlockState state() {
            return level == null ? ModBlocks.POWER_METER.get().defaultBlockState() : level.getBlockState(pos);
        }

        private @Nullable BlockEntity blockEntity() {
            return level == null ? null : level.getBlockEntity(pos);
        }

        private static int packed(int index, int lowIndex, long value) {
            long clamped = Math.max(0L, value);
            return index == lowIndex ? (int) clamped : (int) (clamped >>> 32);
        }
    }
}
