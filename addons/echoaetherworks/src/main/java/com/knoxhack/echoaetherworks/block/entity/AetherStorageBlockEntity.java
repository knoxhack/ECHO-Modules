package com.knoxhack.echoaetherworks.block.entity;

import com.knoxhack.echoarcanacore.api.AetherSignalType;
import com.knoxhack.echoarcanacore.api.AetherStorage;
import com.knoxhack.echoarcanacore.api.AetherStorageTarget;
import com.knoxhack.echoarcanacore.api.ArcanaCoreServices;
import com.knoxhack.echoaetherworks.registry.ModItems;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public abstract class AetherStorageBlockEntity extends BlockEntity implements AetherStorageTarget, Container, WorldlyContainer {
    public static final int AUTOMATION_INPUT_SLOT = 0;
    public static final int AUTOMATION_OUTPUT_SLOT = 1;
    public static final int AUTOMATION_SECONDARY_INPUT_SLOT = 2;
    public static final int AUTOMATION_SLOT_COUNT = 3;
    private static final int[] AUTOMATION_INPUT_SLOTS =
            new int[] {AUTOMATION_INPUT_SLOT, AUTOMATION_SECONDARY_INPUT_SLOT};
    private static final int[] AUTOMATION_OUTPUT_SLOTS = new int[] {AUTOMATION_OUTPUT_SLOT};
    public static final int MODE_PUSH = 0;
    public static final int MODE_HOLD = 1;
    public static final int MODE_ACCEPT_ONLY = 2;
    public static final int REDSTONE_DISABLED = 0;
    public static final int REDSTONE_HIGH = 1;
    public static final int REDSTONE_LOW = 2;
    public static final int REDSTONE_PULSE = 3;
    public static final int REDSTONE_SIDE_ANY = -1;
    protected AetherStorage storage;
    private final NonNullList<ItemStack> automationItems = NonNullList.withSize(AUTOMATION_SLOT_COUNT, ItemStack.EMPTY);
    private int networkMode = MODE_PUSH;
    private boolean automationEnabled = true;
    private int redstoneMode = REDSTONE_DISABLED;
    private int redstoneControlSide = REDSTONE_SIDE_ANY;
    private boolean lastRedstonePowered;
    private boolean redstonePulseArmed;
    private int automationCycles;
    private int automationInputStock;
    private int automationOutputStock;
    private int overloadEvents;
    private int lastOverloadSeverity;
    private int overloadLockoutTicks;
    private String lastOverloadConsequence = "none";

    private record OverloadConsequence(
            int severity,
            double minimumFillRatio,
            double ventRatio,
            double transferPenalty,
            double contaminationGain,
            int lockoutTicks,
            int particles,
            String name) {
    }

    private static final OverloadConsequence[] OVERLOAD_CONSEQUENCES = new OverloadConsequence[] {
            new OverloadConsequence(1, 0.86D, 0.07D, 0.04D, 0.02D, 20, 7, "pressure_bleed"),
            new OverloadConsequence(2, 0.75D, 0.22D, 0.16D, 0.08D, 80, 16, "emergency_vent"),
            new OverloadConsequence(3, 0.75D, 0.30D, 0.24D, 0.14D, 160, 24, "cascade_lockout")
    };

    protected AetherStorageBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
            double capacity, double transferRate, AetherSignalType outputType, Set<AetherSignalType> acceptedTypes) {
        super(type, pos, state);
        this.storage = new AetherStorage(0.0D, capacity, acceptedTypes, outputType, transferRate, 0.0D);
    }

    @Override
    public AetherStorage aetherStorage() {
        return storage;
    }

    @Override
    public boolean setAetherStorage(AetherStorage storage) {
        if (storage == null) {
            return false;
        }
        this.storage = storage;
        setChanged();
        return true;
    }

    public double storedAmount() {
        return storage.storedAmount();
    }

    public double capacity() {
        return storage.maxStoredAmount();
    }

    public double fillRatio() {
        return capacity() <= 0.0D ? 0.0D : storedAmount() / capacity();
    }

    public int networkMode() {
        return networkMode;
    }

    public String networkModeName() {
        return switch (networkMode) {
            case MODE_HOLD -> "hold";
            case MODE_ACCEPT_ONLY -> "accept_only";
            default -> "push";
        };
    }

    public boolean canPushNetwork() {
        return networkMode == MODE_PUSH;
    }

    public boolean acceptsNetworkInput() {
        return networkMode != MODE_HOLD;
    }

    public boolean automationEnabled() {
        return automationEnabled;
    }

    public boolean redstoneControlEnabled() {
        return redstoneMode != REDSTONE_DISABLED;
    }

    public int redstoneMode() {
        return redstoneMode;
    }

    public String redstoneModeName() {
        return switch (redstoneMode) {
            case REDSTONE_HIGH -> "high";
            case REDSTONE_LOW -> "low";
            case REDSTONE_PULSE -> "pulse";
            default -> "free";
        };
    }

    public int redstoneControlSide() {
        return redstoneControlSide;
    }

    public String redstoneControlSideName() {
        if (redstoneControlSide < 0) {
            return "any";
        }
        Direction[] values = Direction.values();
        return redstoneControlSide >= 0 && redstoneControlSide < values.length
                ? values[redstoneControlSide].getSerializedName()
                : "any";
    }

    public boolean redstonePowered() {
        if (level == null) {
            return false;
        }
        if (redstoneControlSide < 0) {
            return level.hasNeighborSignal(getBlockPos());
        }
        Direction[] values = Direction.values();
        if (redstoneControlSide >= values.length) {
            return level.hasNeighborSignal(getBlockPos());
        }
        Direction side = values[redstoneControlSide];
        return level.hasSignal(getBlockPos().relative(side), side.getOpposite());
    }

    public boolean redstoneAllowsAutomation() {
        updateRedstoneState();
        return switch (redstoneMode) {
            case REDSTONE_HIGH -> redstonePowered();
            case REDSTONE_LOW -> !redstonePowered();
            case REDSTONE_PULSE -> redstonePulseArmed;
            default -> true;
        };
    }

    public boolean automationActive() {
        tickOverloadLockout();
        return automationEnabled && overloadLockoutTicks <= 0 && redstoneAllowsAutomation();
    }

    public int automationCycles() {
        return automationCycles;
    }

    public int overloadEvents() {
        return overloadEvents;
    }

    public int lastOverloadSeverity() {
        return lastOverloadSeverity;
    }

    public int overloadLockoutTicks() {
        return overloadLockoutTicks;
    }

    public String lastOverloadConsequence() {
        return lastOverloadConsequence;
    }

    public int comparatorSignal() {
        int fillSignal = (int) Math.round(Math.max(0.0D, Math.min(1.0D, fillRatio())) * 15.0D);
        int riskSignal = (int) Math.round(Math.max(0.0D, Math.min(100.0D, overloadRisk())) * 15.0D / 100.0D);
        ItemStack output = automationItems.get(AUTOMATION_OUTPUT_SLOT);
        int outputSignal = output.isEmpty() ? 0
                : (int) Math.ceil(output.getCount() * 15.0D / Math.max(1, Math.min(output.getMaxStackSize(), getMaxStackSize())));
        return Math.max(fillSignal, Math.max(riskSignal, outputSignal));
    }

    public int overloadRisk() {
        double fill = fillRatio();
        double contamination = storage.contaminationLevel();
        return (int) Math.round(Math.min(100.0D, fill * 70.0D + contamination * 45.0D));
    }

    public int overloadSeverity() {
        int risk = overloadRisk();
        if (risk >= 95) {
            return 3;
        }
        if (risk >= 80) {
            return 2;
        }
        if (risk >= 60) {
            return 1;
        }
        return 0;
    }

    public int automationInputStock() {
        return automationItems.get(AUTOMATION_INPUT_SLOT).getCount()
                + automationItems.get(AUTOMATION_SECONDARY_INPUT_SLOT).getCount()
                + automationInputStock;
    }

    public int automationOutputStock() {
        return automationItems.get(AUTOMATION_OUTPUT_SLOT).getCount() + automationOutputStock;
    }

    public boolean hasAutomationInput(Item item, int amount) {
        if (amount <= 0) {
            return true;
        }
        return countAutomationInput(item) >= amount;
    }

    public boolean canAcceptAutomationOutput(ItemStack output) {
        if (output.isEmpty()) {
            return true;
        }
        ItemStack existing = automationItems.get(AUTOMATION_OUTPUT_SLOT);
        if (existing.isEmpty()) {
            return output.getCount() <= Math.min(output.getMaxStackSize(), getMaxStackSize());
        }
        return ItemStack.isSameItemSameComponents(existing, output)
                && existing.getCount() + output.getCount() <= Math.min(existing.getMaxStackSize(), getMaxStackSize());
    }

    public void stageAutomationInput(int amount) {
        if (amount <= 0) {
            return;
        }
        ItemStack input = new ItemStack(ModItems.AETHER_COIL.get(), amount);
        int inserted = insertAutomationInput(input);
        if (inserted < amount) {
            automationInputStock = Math.min(64, automationInputStock + amount - inserted);
        }
        setChanged();
    }

    public int extractAutomationOutput(int amount) {
        if (amount <= 0) {
            return 0;
        }
        ItemStack output = automationItems.get(AUTOMATION_OUTPUT_SLOT);
        int extracted = 0;
        if (!output.isEmpty()) {
            extracted = Math.min(amount, output.getCount());
            output.shrink(extracted);
            if (output.isEmpty()) {
                automationItems.set(AUTOMATION_OUTPUT_SLOT, ItemStack.EMPTY);
            }
        }
        int remaining = amount - extracted;
        if (remaining > 0 && automationOutputStock > 0) {
            int virtualExtracted = Math.min(remaining, automationOutputStock);
            automationOutputStock -= virtualExtracted;
            extracted += virtualExtracted;
        }
        if (extracted <= 0) {
            return 0;
        }
        setChanged();
        return extracted;
    }

    public void incrementAutomationCycles() {
        automationCycles++;
        setChanged();
    }

    public void consumeRedstonePulse() {
        if (redstoneMode == REDSTONE_PULSE && redstonePulseArmed) {
            redstonePulseArmed = false;
            setChanged();
        }
    }

    public boolean consumeAutomationInput(int amount) {
        return consumeAutomationInput(ModItems.AETHER_COIL.get(), amount);
    }

    public boolean consumeAutomationInput(Item item, int amount) {
        if (amount <= 0) {
            return true;
        }
        if (!hasAutomationInput(item, amount)) {
            return false;
        }
        int remaining = amount;
        for (int slot : AUTOMATION_INPUT_SLOTS) {
            ItemStack input = automationItems.get(slot);
            if (input.isEmpty() || input.getItem() != item || remaining <= 0) {
                continue;
            }
            int consumed = Math.min(remaining, input.getCount());
            input.shrink(consumed);
            remaining -= consumed;
            if (input.isEmpty()) {
                automationItems.set(slot, ItemStack.EMPTY);
            }
        }
        if (remaining > 0 && item == ModItems.AETHER_COIL.get()) {
            int consumed = Math.min(remaining, automationInputStock);
            automationInputStock -= consumed;
            remaining -= consumed;
        }
        setChanged();
        return remaining <= 0;
    }

    public void addAutomationOutput(int amount) {
        addAutomationOutput(new ItemStack(ModItems.AETHER_CAPACITOR.get(), amount));
    }

    public boolean addAutomationOutput(ItemStack output) {
        if (output.isEmpty()) {
            return true;
        }
        if (!canAcceptAutomationOutput(output)) {
            return false;
        }
        ItemStack existing = automationItems.get(AUTOMATION_OUTPUT_SLOT);
        if (existing.isEmpty()) {
            automationItems.set(AUTOMATION_OUTPUT_SLOT, output.copy());
        } else {
            existing.grow(output.getCount());
        }
        setChanged();
        return true;
    }

    private int insertAutomationInput(ItemStack stack) {
        if (stack.isEmpty() || !isSupportedAutomationInput(stack)) {
            return 0;
        }
        int inserted = 0;
        for (int slot : AUTOMATION_INPUT_SLOTS) {
            if (inserted >= stack.getCount()) {
                break;
            }
            inserted += insertAutomationInput(slot, stack.copyWithCount(stack.getCount() - inserted));
        }
        return inserted;
    }

    private int insertAutomationInput(int slot, ItemStack stack) {
        if (stack.isEmpty() || !canPlaceItem(slot, stack)) {
            return 0;
        }
        ItemStack existing = automationItems.get(slot);
        int max = Math.min(stack.getMaxStackSize(), getMaxStackSize());
        if (existing.isEmpty()) {
            int inserted = Math.min(stack.getCount(), max);
            automationItems.set(slot, stack.copyWithCount(inserted));
            return inserted;
        }
        if (!ItemStack.isSameItemSameComponents(existing, stack)) {
            return 0;
        }
        int inserted = Math.min(stack.getCount(), max - existing.getCount());
        if (inserted > 0) {
            existing.grow(inserted);
        }
        return inserted;
    }

    public ItemStack automationOutputItem() {
        return automationItems.get(AUTOMATION_OUTPUT_SLOT);
    }

    public ItemStack automationInputItem() {
        return automationItems.get(AUTOMATION_INPUT_SLOT);
    }

    public ItemStack automationSecondaryInputItem() {
        return automationItems.get(AUTOMATION_SECONDARY_INPUT_SLOT);
    }

    private int countAutomationInput(Item item) {
        int count = 0;
        for (int slot : AUTOMATION_INPUT_SLOTS) {
            ItemStack stack = automationItems.get(slot);
            if (!stack.isEmpty() && stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        if (item == ModItems.AETHER_COIL.get()) {
            count += automationInputStock;
        }
        return count;
    }

    private static boolean isSupportedAutomationInput(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        return item == ModItems.AETHER_COIL.get() || item == ModItems.AETHER_CAPACITOR.get()
                || item == ModItems.PURITY_CATALYST.get();
    }

    public void addLegacyAutomationOutput(int amount) {
        if (amount <= 0) {
            return;
        }
        automationOutputStock = Math.min(64, automationOutputStock + amount);
        setChanged();
    }

    public void toggleAutomation() {
        automationEnabled = !automationEnabled;
        setChanged();
    }

    public void toggleRedstoneControl() {
        cycleRedstoneMode();
    }

    public void cycleRedstoneMode() {
        redstoneMode = (redstoneMode + 1) % 4;
        if (redstoneMode != REDSTONE_PULSE) {
            redstonePulseArmed = false;
        }
        lastRedstonePowered = redstonePowered();
        setChanged();
    }

    public void cycleRedstoneControlSide() {
        redstoneControlSide++;
        if (redstoneControlSide >= Direction.values().length) {
            redstoneControlSide = REDSTONE_SIDE_ANY;
        }
        lastRedstonePowered = redstonePowered();
        if (redstoneMode == REDSTONE_PULSE) {
            redstonePulseArmed = false;
        }
        setChanged();
    }

    public void cycleNetworkMode() {
        networkMode = (networkMode + 1) % 3;
        setChanged();
    }

    public boolean purifyStorage() {
        if (storage.contaminationLevel() <= 0.0D) {
            return false;
        }
        storage = storage.withContamination(Math.max(0.0D, storage.contaminationLevel() - 0.15D));
        setChanged();
        return true;
    }

    public boolean handleMenuButton(ServerPlayer player, int buttonId) {
        if (buttonId == com.knoxhack.echoaetherworks.menu.AetherMachineMenu.BUTTON_CYCLE_MODE) {
            cycleNetworkMode();
            if (player != null) {
                player.sendSystemMessage(Component.translatable("message.echoaetherworks.mode",
                        Component.translatable("mode.echoaetherworks." + networkModeName())));
            }
            return true;
        }
        if (buttonId == com.knoxhack.echoaetherworks.menu.AetherMachineMenu.BUTTON_DRAW && player != null) {
            return com.knoxhack.echoaetherworks.api.AetherWorksApi.drawToPlayer(player, this,
                    com.knoxhack.echoaetherworks.api.AetherWorksApi.machineIdFor(this));
        }
        if (buttonId == com.knoxhack.echoaetherworks.menu.AetherMachineMenu.BUTTON_PURIFY) {
            boolean purified = purifyStorage();
            if (player != null) {
                player.sendSystemMessage(Component.translatable(purified
                        ? "message.echoaetherworks.purified"
                        : "message.echoaetherworks.pure"));
            }
            return purified;
        }
        if (buttonId == com.knoxhack.echoaetherworks.menu.AetherMachineMenu.BUTTON_TOGGLE_AUTOMATION) {
            toggleAutomation();
            if (player != null) {
                player.sendSystemMessage(Component.translatable(automationEnabled
                        ? "message.echoaetherworks.automation_enabled"
                        : "message.echoaetherworks.automation_disabled"));
            }
            return true;
        }
        if (buttonId == com.knoxhack.echoaetherworks.menu.AetherMachineMenu.BUTTON_TOGGLE_REDSTONE_CONTROL) {
            cycleRedstoneMode();
            if (player != null) {
                player.sendSystemMessage(Component.translatable("message.echoaetherworks.redstone_mode",
                        Component.translatable("redstone.echoaetherworks." + redstoneModeName())));
            }
            return true;
        }
        if (buttonId == com.knoxhack.echoaetherworks.menu.AetherMachineMenu.BUTTON_CYCLE_REDSTONE_SIDE) {
            cycleRedstoneControlSide();
            if (player != null) {
                player.sendSystemMessage(Component.translatable("message.echoaetherworks.redstone_side",
                        Component.translatable("redstone_side.echoaetherworks." + redstoneControlSideName())));
            }
            return true;
        }
        if (buttonId == com.knoxhack.echoaetherworks.menu.AetherMachineMenu.BUTTON_RUN_AUTOMATION_RECIPE) {
            return com.knoxhack.echoaetherworks.api.AetherWorksApi.runNetworkAutomationRecipe(player, level, this);
        }
        return false;
    }

    protected void generate(double amount, AetherSignalType type) {
        double inserted = ArcanaCoreServices.aether().insertAether(this, amount, type);
        if (inserted > 0.0D) {
            setChanged();
            if (level instanceof ServerLevel serverLevel) {
                checkOverloadSafety(serverLevel);
            }
        }
    }

    protected void pushToNeighbors(Level level, BlockPos pos) {
        if (level == null || level.isClientSide() || !automationActive() || !canPushNetwork() || storage.storedAmount() <= 0.0D) {
            return;
        }
        double budget = storage.transferRate();
        double routed = com.knoxhack.echoaetherworks.api.AetherWorksApi.routeFromNetwork(level, this, budget);
        budget -= routed;
        double moved = routed;
        for (Direction direction : Direction.values()) {
            if (budget <= 0.0D || storage.storedAmount() <= 0.0D) {
                break;
            }
            BlockEntity neighbor = level.getBlockEntity(pos.relative(direction));
            if (!(neighbor instanceof AetherStorageTarget target) || neighbor == this) {
                continue;
            }
            if (neighbor instanceof AetherStorageBlockEntity storageTarget && !storageTarget.acceptsNetworkInput()) {
                continue;
            }
            double extracted = ArcanaCoreServices.aether().extractAether(this, budget, storage.outputType());
            if (extracted <= 0.0D) {
                continue;
            }
            double accepted = ArcanaCoreServices.aether().insertAether(target, extracted, storage.outputType());
            if (accepted < extracted) {
                ArcanaCoreServices.aether().insertAether(this, extracted - accepted, storage.outputType());
            }
            moved += accepted;
            budget -= accepted;
        }
        if (moved > 0.0D) {
            consumeRedstonePulse();
        }
    }

    public int adjacentAetherNodes(Level level, BlockPos pos) {
        return countNeighbors(level, pos, false);
    }

    public int pushableNeighborCount(Level level, BlockPos pos) {
        return canPushNetwork() && automationActive() ? countNeighbors(level, pos, true) : 0;
    }

    public int acceptingNeighborCount(Level level, BlockPos pos) {
        return countNeighbors(level, pos, true);
    }

    private int countNeighbors(Level level, BlockPos pos, boolean requireInputEnabled) {
        if (level == null || pos == null) {
            return 0;
        }
        int count = 0;
        for (Direction direction : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(direction));
            if (!(neighbor instanceof AetherStorageTarget target) || neighbor == this) {
                continue;
            }
            if (requireInputEnabled && neighbor instanceof AetherStorageBlockEntity storageTarget
                    && !storageTarget.acceptsNetworkInput()) {
                continue;
            }
            count++;
        }
        return count;
    }

    protected void spark(ServerLevel level, BlockPos pos, net.minecraft.core.particles.ParticleOptions particle) {
        level.sendParticles(particle, pos.getX() + 0.5D, pos.getY() + 0.72D, pos.getZ() + 0.5D,
                2, 0.18D, 0.12D, 0.18D, 0.015D);
    }

    public boolean checkOverloadSafety(ServerLevel level) {
        int severity = overloadSeverity();
        OverloadConsequence consequence = consequenceFor(severity);
        if (level == null || storage.maxStoredAmount() <= 0.0D || consequence == null
                || fillRatio() < consequence.minimumFillRatio()) {
            return false;
        }
        double vented = Math.max(8.0D, storage.maxStoredAmount() * consequence.ventRatio());
        double transferMultiplier = Math.max(0.45D, 1.0D - consequence.transferPenalty());
        storage = new AetherStorage(
                Math.max(0.0D, storage.storedAmount() - vented),
                storage.maxStoredAmount(),
                storage.acceptedTypes(),
                storage.outputType(),
                Math.max(1.0D, storage.transferRate() * transferMultiplier),
                Math.min(1.0D, storage.contaminationLevel() + consequence.contaminationGain()));
        automationEnabled = false;
        overloadLockoutTicks = Math.max(overloadLockoutTicks, consequence.lockoutTicks());
        overloadEvents++;
        lastOverloadSeverity = severity;
        lastOverloadConsequence = consequence.name();
        setChanged();
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                getBlockPos().getX() + 0.5D, getBlockPos().getY() + 0.8D, getBlockPos().getZ() + 0.5D,
                consequence.particles(), 0.28D + severity * 0.05D, 0.18D, 0.28D + severity * 0.05D, 0.035D);
        return true;
    }

    public void tickAutomationState() {
        tickOverloadLockout();
    }

    private static @Nullable OverloadConsequence consequenceFor(int severity) {
        for (OverloadConsequence consequence : OVERLOAD_CONSEQUENCES) {
            if (consequence.severity() == severity) {
                return consequence;
            }
        }
        return null;
    }

    private void tickOverloadLockout() {
        if (level == null || level.isClientSide() || overloadLockoutTicks <= 0) {
            return;
        }
        overloadLockoutTicks--;
        if (overloadLockoutTicks == 0) {
            setChanged();
        }
    }

    private void updateRedstoneState() {
        boolean powered = redstonePowered();
        if (redstoneMode == REDSTONE_PULSE && powered && !lastRedstonePowered) {
            redstonePulseArmed = true;
            setChanged();
        }
        lastRedstonePowered = powered;
    }

    @Override
    public int getContainerSize() {
        return automationItems.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : automationItems) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < automationItems.size() ? automationItems.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(automationItems, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(automationItems, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= automationItems.size()) {
            return;
        }
        automationItems.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return (slot == AUTOMATION_INPUT_SLOT || slot == AUTOMATION_SECONDARY_INPUT_SLOT)
                && isSupportedAutomationInput(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? AUTOMATION_OUTPUT_SLOTS : AUTOMATION_INPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return direction != Direction.DOWN && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return direction == Direction.DOWN && slot == AUTOMATION_OUTPUT_SLOT && !stack.isEmpty();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        automationItems.clear();
        setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, automationItems);
        storage = new AetherStorage(
                input.getFloatOr("stored", 0.0F),
                input.getFloatOr("capacity", (float) storage.maxStoredAmount()),
                storage.acceptedTypes(),
                AetherSignalType.byId(input.getStringOr("output_type", storage.outputType().serializedName())),
                input.getFloatOr("transfer_rate", (float) storage.transferRate()),
                input.getFloatOr("contamination", 0.0F));
        networkMode = Math.max(MODE_PUSH, Math.min(MODE_ACCEPT_ONLY, input.getIntOr("network_mode", MODE_PUSH)));
        automationEnabled = input.getBooleanOr("automation_enabled", true);
        redstoneMode = Math.max(REDSTONE_DISABLED, Math.min(REDSTONE_PULSE,
                input.getIntOr("redstone_mode", input.getBooleanOr("redstone_control_enabled", false)
                        ? REDSTONE_HIGH : REDSTONE_DISABLED)));
        redstoneControlSide = input.getIntOr("redstone_control_side", REDSTONE_SIDE_ANY);
        if (redstoneControlSide < REDSTONE_SIDE_ANY || redstoneControlSide >= Direction.values().length) {
            redstoneControlSide = REDSTONE_SIDE_ANY;
        }
        lastRedstonePowered = input.getBooleanOr("last_redstone_powered", false);
        redstonePulseArmed = input.getBooleanOr("redstone_pulse_armed", false);
        automationCycles = Math.max(0, input.getIntOr("automation_cycles", 0));
        automationInputStock = Math.max(0, input.getIntOr("automation_input_stock", 0));
        automationOutputStock = Math.max(0, input.getIntOr("automation_output_stock", 0));
        overloadEvents = Math.max(0, input.getIntOr("overload_events", 0));
        lastOverloadSeverity = Math.max(0, input.getIntOr("last_overload_severity", 0));
        overloadLockoutTicks = Math.max(0, input.getIntOr("overload_lockout_ticks", 0));
        lastOverloadConsequence = input.getStringOr("last_overload_consequence", "none");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, automationItems);
        output.putFloat("stored", (float) storage.storedAmount());
        output.putFloat("capacity", (float) storage.maxStoredAmount());
        output.putString("output_type", storage.outputType().serializedName());
        output.putFloat("transfer_rate", (float) storage.transferRate());
        output.putFloat("contamination", (float) storage.contaminationLevel());
        output.putInt("network_mode", networkMode);
        output.putBoolean("automation_enabled", automationEnabled);
        output.putBoolean("redstone_control_enabled", redstoneControlEnabled());
        output.putInt("redstone_mode", redstoneMode);
        output.putInt("redstone_control_side", redstoneControlSide);
        output.putBoolean("last_redstone_powered", lastRedstonePowered);
        output.putBoolean("redstone_pulse_armed", redstonePulseArmed);
        output.putInt("automation_cycles", automationCycles);
        output.putInt("automation_input_stock", automationInputStock);
        output.putInt("automation_output_stock", automationOutputStock);
        output.putInt("overload_events", overloadEvents);
        output.putInt("last_overload_severity", lastOverloadSeverity);
        output.putInt("overload_lockout_ticks", overloadLockoutTicks);
        output.putString("last_overload_consequence", lastOverloadConsequence);
    }
}
