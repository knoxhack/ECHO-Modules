package com.knoxhack.echoaetherworks.menu;

import com.knoxhack.echoaetherworks.block.entity.AetherStorageBlockEntity;
import com.knoxhack.echoaetherworks.api.AetherWorksApi;
import com.knoxhack.echoaetherworks.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class AetherMachineMenu extends AbstractContainerMenu {
    public static final int GUI_WIDTH = 276;
    public static final int GUI_HEIGHT = 286;
    public static final int BUTTON_CYCLE_MODE = 0;
    public static final int BUTTON_DRAW = 1;
    public static final int BUTTON_PURIFY = 2;
    public static final int BUTTON_TOGGLE_AUTOMATION = 3;
    public static final int BUTTON_RUN_AUTOMATION_RECIPE = 4;
    public static final int BUTTON_TOGGLE_REDSTONE_CONTROL = 5;
    public static final int BUTTON_CYCLE_REDSTONE_SIDE = 6;

    private static final int DATA_STORED = 0;
    private static final int DATA_CAPACITY = 1;
    private static final int DATA_CONTAMINATION = 2;
    private static final int DATA_MODE = 3;
    private static final int DATA_TRANSFER = 4;
    private static final int DATA_TYPE = 5;
    private static final int DATA_AUTOMATION = 6;
    private static final int DATA_NEIGHBORS = 7;
    private static final int DATA_PUSH_TARGETS = 8;
    private static final int DATA_ACCEPT_TARGETS = 9;
    private static final int DATA_GRAPH_NODES = 10;
    private static final int DATA_GRAPH_STORED = 11;
    private static final int DATA_GRAPH_CAPACITY = 12;
    private static final int DATA_ROUTE_DEPTH = 13;
    private static final int DATA_AUTOMATION_RECIPES = 14;
    private static final int DATA_COMPLETED_RECIPES = 15;
    private static final int DATA_AUTOMATION_INPUT = 16;
    private static final int DATA_AUTOMATION_OUTPUT = 17;
    private static final int DATA_REDSTONE_CONTROL = 18;
    private static final int DATA_REDSTONE_POWERED = 19;
    private static final int DATA_AUTOMATION_ACTIVE = 20;
    private static final int DATA_OVERLOAD_RISK = 21;
    private static final int DATA_OVERLOAD_EVENTS = 22;
    private static final int DATA_REDSTONE_MODE = 23;
    private static final int DATA_OVERLOAD_SEVERITY = 24;
    private static final int DATA_REDSTONE_SIDE = 25;
    private static final int DATA_OVERLOAD_LOCKOUT = 26;
    private static final int DATA_COMPARATOR = 27;
    public static final int DATA_COUNT = 28;
    private static final int MACHINE_SLOT_START = 0;
    private static final int PLAYER_INV_START = AetherStorageBlockEntity.AUTOMATION_SLOT_COUNT;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    private static final int HOTBAR_END = PLAYER_INV_END + 9;

    private final Level level;
    private final BlockPos pos;
    private final Container container;
    private final ContainerData data;

    public AetherMachineMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, playerInventory.player.level(), buf.readBlockPos(),
                null, new SimpleContainerData(DATA_COUNT));
    }

    public AetherMachineMenu(int containerId, Inventory playerInventory, Level level, BlockPos pos) {
        this(containerId, playerInventory, level, pos, null, new LiveData(level, pos));
    }

    private AetherMachineMenu(int containerId, Inventory playerInventory, Level level, BlockPos pos,
            Container container, ContainerData data) {
        super(ModMenus.AETHER_MACHINE.get(), containerId);
        Container machineContainer = container == null ? containerFor(level, pos) : container;
        checkContainerSize(machineContainer, AetherStorageBlockEntity.AUTOMATION_SLOT_COUNT);
        checkContainerDataCount(data, DATA_COUNT);
        this.level = level;
        this.pos = pos == null ? BlockPos.ZERO : pos.immutable();
        this.container = machineContainer;
        this.data = data;
        addSlot(inputSlot(machineContainer, AetherStorageBlockEntity.AUTOMATION_INPUT_SLOT, 24, 144));
        addSlot(inputSlot(machineContainer, AetherStorageBlockEntity.AUTOMATION_SECONDARY_INPUT_SLOT, 48, 144));
        addSlot(outputSlot(machineContainer, AetherStorageBlockEntity.AUTOMATION_OUTPUT_SLOT, 92, 144));
        addStandardInventorySlots(playerInventory, 57, 204);
        addDataSlots(data);
    }

    public BlockPos pos() {
        return pos;
    }

    public int stored() {
        return data.get(DATA_STORED);
    }

    public int capacity() {
        return Math.max(1, data.get(DATA_CAPACITY));
    }

    public int contaminationPercent() {
        return data.get(DATA_CONTAMINATION);
    }

    public int mode() {
        return data.get(DATA_MODE);
    }

    public int transferRate() {
        return data.get(DATA_TRANSFER);
    }

    public int typeOrdinal() {
        return data.get(DATA_TYPE);
    }

    public boolean automationEnabled() {
        return data.get(DATA_AUTOMATION) > 0;
    }

    public boolean redstoneControlEnabled() {
        return data.get(DATA_REDSTONE_CONTROL) > 0;
    }

    public int redstoneMode() {
        return data.get(DATA_REDSTONE_MODE);
    }

    public String redstoneModeName() {
        return switch (redstoneMode()) {
            case AetherStorageBlockEntity.REDSTONE_HIGH -> "High";
            case AetherStorageBlockEntity.REDSTONE_LOW -> "Low";
            case AetherStorageBlockEntity.REDSTONE_PULSE -> "Pulse";
            default -> "Free";
        };
    }

    public boolean redstonePowered() {
        return data.get(DATA_REDSTONE_POWERED) > 0;
    }

    public boolean automationActive() {
        return data.get(DATA_AUTOMATION_ACTIVE) > 0;
    }

    public int overloadRisk() {
        return data.get(DATA_OVERLOAD_RISK);
    }

    public int overloadEvents() {
        return data.get(DATA_OVERLOAD_EVENTS);
    }

    public int overloadSeverity() {
        return data.get(DATA_OVERLOAD_SEVERITY);
    }

    public int redstoneSide() {
        return data.get(DATA_REDSTONE_SIDE);
    }

    public String redstoneSideName() {
        int side = redstoneSide();
        if (side < 0) {
            return "Any";
        }
        net.minecraft.core.Direction[] values = net.minecraft.core.Direction.values();
        return side >= 0 && side < values.length ? values[side].getSerializedName() : "any";
    }

    public int overloadLockoutTicks() {
        return data.get(DATA_OVERLOAD_LOCKOUT);
    }

    public int comparatorSignal() {
        return data.get(DATA_COMPARATOR);
    }

    public int neighborCount() {
        return data.get(DATA_NEIGHBORS);
    }

    public int pushTargetCount() {
        return data.get(DATA_PUSH_TARGETS);
    }

    public int acceptTargetCount() {
        return data.get(DATA_ACCEPT_TARGETS);
    }

    public int graphNodeCount() {
        return data.get(DATA_GRAPH_NODES);
    }

    public int graphStored() {
        return data.get(DATA_GRAPH_STORED);
    }

    public int graphCapacity() {
        return Math.max(1, data.get(DATA_GRAPH_CAPACITY));
    }

    public int routeDepth() {
        return data.get(DATA_ROUTE_DEPTH);
    }

    public int automationRecipeCount() {
        return data.get(DATA_AUTOMATION_RECIPES);
    }

    public int completedRecipeCount() {
        return data.get(DATA_COMPLETED_RECIPES);
    }

    public int automationInputStock() {
        return data.get(DATA_AUTOMATION_INPUT);
    }

    public int automationOutputStock() {
        return data.get(DATA_AUTOMATION_OUTPUT);
    }

    public String modeName() {
        return switch (mode()) {
            case AetherStorageBlockEntity.MODE_HOLD -> "Hold";
            case AetherStorageBlockEntity.MODE_ACCEPT_ONLY -> "Accept Only";
            default -> "Push";
        };
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        AetherStorageBlockEntity storage = storage();
        return storage != null && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && storage.handleMenuButton(serverPlayer, id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            copy = stack.copy();
            if (index < PLAYER_INV_START) {
                if (!moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, MACHINE_SLOT_START,
                    AetherStorageBlockEntity.AUTOMATION_SLOT_COUNT - 1, false)
                    && !moveItemStackTo(stack, AetherStorageBlockEntity.AUTOMATION_SECONDARY_INPUT_SLOT,
                    AetherStorageBlockEntity.AUTOMATION_SECONDARY_INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        if (storage() == null) {
            return false;
        }
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        return player.distanceToSqr(x, y, z) <= 64.0D;
    }

    private AetherStorageBlockEntity storage() {
        if (level == null) {
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof AetherStorageBlockEntity storage ? storage : null;
    }

    private static Container containerFor(Level level, BlockPos pos) {
        if (level != null && pos != null && level.getBlockEntity(pos) instanceof AetherStorageBlockEntity storage) {
            return storage;
        }
        return new SimpleContainer(AetherStorageBlockEntity.AUTOMATION_SLOT_COUNT);
    }

    private static Slot inputSlot(Container container, int slot, int x, int y) {
        return new Slot(container, slot, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return container.canPlaceItem(slot, stack);
            }
        };
    }

    private static Slot outputSlot(Container container, int slot, int x, int y) {
        return new Slot(container, slot, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        };
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
            AetherStorageBlockEntity storage = storage();
            if (storage == null) {
                return 0;
            }
            AetherWorksApi.AetherTopologySnapshot topology = AetherWorksApi.describeTopology(level, pos);
            return switch (index) {
                case DATA_STORED -> (int) Math.round(storage.storedAmount());
                case DATA_CAPACITY -> (int) Math.round(storage.capacity());
                case DATA_CONTAMINATION -> (int) Math.round(storage.aetherStorage().contaminationLevel() * 100.0D);
                case DATA_MODE -> storage.networkMode();
                case DATA_TRANSFER -> (int) Math.round(storage.aetherStorage().transferRate());
                case DATA_TYPE -> storage.aetherStorage().outputType().ordinal();
                case DATA_AUTOMATION -> storage.automationEnabled() ? 1 : 0;
                case DATA_NEIGHBORS -> storage.adjacentAetherNodes(level, pos);
                case DATA_PUSH_TARGETS -> storage.pushableNeighborCount(level, pos);
                case DATA_ACCEPT_TARGETS -> storage.acceptingNeighborCount(level, pos);
                case DATA_GRAPH_NODES -> topology.nodeCount();
                case DATA_GRAPH_STORED -> topology.storedAmount();
                case DATA_GRAPH_CAPACITY -> topology.capacity();
                case DATA_ROUTE_DEPTH -> topology.routeDepth();
                case DATA_AUTOMATION_RECIPES -> topology.automationRecipes();
                case DATA_COMPLETED_RECIPES -> topology.completedRecipes();
                case DATA_AUTOMATION_INPUT -> storage.automationInputStock();
                case DATA_AUTOMATION_OUTPUT -> storage.automationOutputStock();
                case DATA_REDSTONE_CONTROL -> storage.redstoneControlEnabled() ? 1 : 0;
                case DATA_REDSTONE_POWERED -> storage.redstonePowered() ? 1 : 0;
                case DATA_AUTOMATION_ACTIVE -> storage.automationActive() ? 1 : 0;
                case DATA_OVERLOAD_RISK -> storage.overloadRisk();
                case DATA_OVERLOAD_EVENTS -> storage.overloadEvents();
                case DATA_REDSTONE_MODE -> storage.redstoneMode();
                case DATA_OVERLOAD_SEVERITY -> Math.max(storage.overloadSeverity(), storage.lastOverloadSeverity());
                case DATA_REDSTONE_SIDE -> storage.redstoneControlSide();
                case DATA_OVERLOAD_LOCKOUT -> storage.overloadLockoutTicks();
                case DATA_COMPARATOR -> storage.comparatorSignal();
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

        private AetherStorageBlockEntity storage() {
            if (level == null) {
                return null;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            return blockEntity instanceof AetherStorageBlockEntity storage ? storage : null;
        }
    }
}
