package com.knoxhack.signalos.block.entity;

import com.knoxhack.signalos.registry.ModBlockEntities;
import com.knoxhack.signalos.registry.ModBlocks;
import com.knoxhack.signalos.api.SignalOsDriveData;
import com.knoxhack.signalos.item.SignalOsDataDriveItem;
import com.knoxhack.signalos.service.SignalOsComputerNetworkService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class SignalOsTerminalBlockEntity extends BlockEntity {
    private static final int REWARD_SLOTS = 27;
    public static final int DRIVE_SLOTS = 1;

    private UUID ownerUUID;
    private String ownerName = "";
    private long lastActivityTick;
    private final NonNullList<ItemStack> storedRewards = NonNullList.withSize(REWARD_SLOTS, ItemStack.EMPTY);
    private final DriveInventory bootDrive = new DriveInventory(DRIVE_SLOTS, this::setChanged);

    public SignalOsTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TERMINAL.get(), pos, state);
    }

    public void setOwnerIfMissing(Player player) {
        if (ownerUUID == null && player != null) {
            ownerUUID = player.getUUID();
            ownerName = player.getName().getString();
            setChanged();
        }
    }

    public boolean isExplicitOwner(Player player) {
        return player != null && ownerUUID != null && ownerUUID.equals(player.getUUID());
    }

    public UUID ownerUUID() {
        return ownerUUID;
    }

    public String ownerName() {
        return ownerName == null ? "" : ownerName;
    }

    public long lastActivityTick() {
        return lastActivityTick;
    }

    public void recordActivity() {
        lastActivityTick = level == null ? 0L : level.getGameTime();
        setChanged();
    }

    public DriveInventory bootDrive() {
        return bootDrive;
    }

    public boolean hasActiveDrive() {
        return activeDriveStack().is(ModBlocks.DATA_DRIVE.get());
    }

    public ItemStack activeDriveStack() {
        return bootDrive.getItem(0);
    }

    public SignalOsDriveData activeDriveData() {
        return SignalOsDataDriveItem.data(activeDriveStack());
    }

    public boolean updateActiveDrive(UnaryOperator<SignalOsDriveData> updater) {
        if (updater == null || !hasActiveDrive()) {
            return false;
        }
        ItemStack stack = activeDriveStack();
        SignalOsDriveData next = updater.apply(SignalOsDataDriveItem.data(stack));
        if (next == null) {
            return false;
        }
        stack.set(com.knoxhack.signalos.registry.ModDataComponents.DRIVE_DATA.get(), next);
        bootDrive.setChanged();
        setChanged();
        return true;
    }

    public boolean insertDrive(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.is(ModBlocks.DATA_DRIVE.get()) || !activeDriveStack().isEmpty()
                || !SignalOsDataDriveItem.canInstall(stack)) {
            return false;
        }
        ItemStack inserted = stack.copyWithCount(1);
        SignalOsDataDriveItem.ensureV2Data(inserted);
        bootDrive.setItem(0, inserted);
        setChanged();
        return true;
    }

    public ItemStack extractDrive() {
        ItemStack stack = activeDriveStack();
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack extracted = stack.copy();
        bootDrive.setItem(0, ItemStack.EMPTY);
        setChanged();
        return extracted;
    }

    public boolean storeRewards(String missionId, List<ItemStack> rewards) {
        List<ItemStack> sanitizedRewards = sanitizeRewards(rewards);
        if (sanitizedRewards.isEmpty()) {
            return false;
        }
        NonNullList<ItemStack> simulatedRewards = NonNullList.withSize(storedRewards.size(), ItemStack.EMPTY);
        for (int i = 0; i < storedRewards.size(); i++) {
            simulatedRewards.set(i, storedRewards.get(i).copy());
        }
        if (!insertRewards(simulatedRewards, sanitizedRewards)) {
            return false;
        }
        for (int i = 0; i < storedRewards.size(); i++) {
            storedRewards.set(i, simulatedRewards.get(i));
        }
        setChanged();
        return true;
    }

    public boolean claimAllRewards(Player player) {
        if (player == null) {
            return false;
        }
        boolean claimedAny = false;
        for (int i = 0; i < storedRewards.size(); i++) {
            ItemStack stack = storedRewards.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack copy = stack.copy();
            if (!player.getInventory().add(copy)) {
                player.drop(copy, false);
            }
            storedRewards.set(i, ItemStack.EMPTY);
            claimedAny = true;
        }
        if (claimedAny) {
            player.sendSystemMessage(Component.literal("[SignalOS] Reward inbox claimed."));
            setChanged();
        }
        return claimedAny;
    }

    public boolean hasStoredRewards() {
        return storedRewards.stream().anyMatch(stack -> !stack.isEmpty());
    }

    public int storedRewardCount() {
        int count = 0;
        for (ItemStack stack : storedRewards) {
            if (!stack.isEmpty()) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public List<ItemStack> storedRewardStacks() {
        return storedRewards.stream()
                .filter(stack -> !stack.isEmpty())
                .map(ItemStack::copy)
                .toList();
    }

    public int rewardSlotCount() {
        return storedRewards.size();
    }

    private static List<ItemStack> sanitizeRewards(List<ItemStack> rewards) {
        if (rewards == null || rewards.isEmpty()) {
            return List.of();
        }
        List<ItemStack> sanitized = new ArrayList<>();
        for (ItemStack reward : rewards) {
            if (reward == null || reward.isEmpty()) {
                continue;
            }
            sanitized.add(reward.copy());
        }
        return List.copyOf(sanitized);
    }

    private static boolean insertRewards(NonNullList<ItemStack> slots, List<ItemStack> rewards) {
        boolean storedAny = false;
        for (ItemStack reward : rewards) {
            ItemStack remaining = reward.copy();
            if (remaining.isEmpty()) {
                continue;
            }
            for (int i = 0; i < slots.size(); i++) {
                ItemStack existing = slots.get(i);
                if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, remaining)) {
                    int moved = Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount());
                    if (moved > 0) {
                        existing.grow(moved);
                        remaining.shrink(moved);
                        storedAny = true;
                    }
                }
                if (remaining.isEmpty()) {
                    break;
                }
            }
            for (int i = 0; i < slots.size() && !remaining.isEmpty(); i++) {
                if (slots.get(i).isEmpty()) {
                    int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                    if (moved > 0) {
                        ItemStack inserted = remaining.copy();
                        inserted.setCount(moved);
                        slots.set(i, inserted);
                        remaining.shrink(moved);
                        storedAny = true;
                    }
                }
            }
            if (!remaining.isEmpty()) {
                return false;
            }
        }
        return storedAny;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        bootDrive.serialize(output);
        if (ownerUUID != null) {
            output.putString("owner_uuid", ownerUUID.toString());
        }
        output.putString("owner_name", ownerName);
        output.putLong("last_activity", lastActivityTick);
        output.putInt("reward_slot_count", storedRewards.size());
        for (int i = 0; i < storedRewards.size(); i++) {
            ItemStack stack = storedRewards.get(i);
            if (!stack.isEmpty()) {
                String prefix = "reward_" + i + "_";
                output.putString(prefix + "item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                output.putInt(prefix + "count", stack.getCount());
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        bootDrive.deserialize(input);
        input.getString("owner_uuid").ifPresent(value -> {
            try {
                ownerUUID = UUID.fromString(value);
            } catch (IllegalArgumentException ignored) {
                ownerUUID = null;
            }
        });
        ownerName = input.getStringOr("owner_name", "");
        lastActivityTick = input.getLongOr("last_activity", 0L);
        for (int i = 0; i < storedRewards.size(); i++) {
            storedRewards.set(i, ItemStack.EMPTY);
        }
        int slotCount = Math.min(input.getIntOr("reward_slot_count", REWARD_SLOTS), REWARD_SLOTS);
        for (int i = 0; i < slotCount; i++) {
            String prefix = "reward_" + i + "_";
            String itemId = input.getStringOr(prefix + "item", "");
            int count = input.getIntOr(prefix + "count", 1);
            if (!itemId.isBlank()) {
                int slot = i;
                Identifier id = Identifier.tryParse(itemId);
                if (id != null) {
                    BuiltInRegistries.ITEM.getOptional(id).ifPresent(item -> {
                        ItemStack stack = new ItemStack(item);
                        stack.setCount(Math.max(1, Math.min(count, stack.getMaxStackSize())));
                        storedRewards.set(slot, stack);
                    });
                }
            }
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        SignalOsComputerNetworkService.invalidateCache();
    }

    public static final class DriveInventory extends SimpleContainer {
        private final Runnable onChanged;

        private DriveInventory(int size, Runnable onChanged) {
            super(size);
            this.onChanged = onChanged;
        }

        public void serialize(ValueOutput output) {
            storeAsItemList(output.list("boot_drive", ItemStack.CODEC));
        }

        public void deserialize(ValueInput input) {
            fromItemList(input.listOrEmpty("boot_drive", ItemStack.CODEC));
        }

        @Override
        public void setChanged() {
            super.setChanged();
            onChanged.run();
        }
    }
}
