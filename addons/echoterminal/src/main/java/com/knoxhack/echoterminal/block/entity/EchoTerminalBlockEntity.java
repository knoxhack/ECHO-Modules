package com.knoxhack.echoterminal.block.entity;

import com.knoxhack.echoterminal.registry.ModBlockEntities;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class EchoTerminalBlockEntity extends BlockEntity {
    private static final int REWARD_SLOTS = 27;

    private UUID ownerUUID;
    private String ownerName = "";
    private long lastActivityTick = 0L;
    private final NonNullList<ItemStack> storedRewards = NonNullList.withSize(REWARD_SLOTS, ItemStack.EMPTY);

    public EchoTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ECHO_TERMINAL.get(), pos, state);
    }

    public void setOwnerIfMissing(Player player) {
        if (ownerUUID == null && player != null) {
            ownerUUID = player.getUUID();
            ownerName = player.getName().getString();
            setChanged();
        }
    }

    public boolean isOwner(Player player) {
        return player != null && (ownerUUID == null || ownerUUID.equals(player.getUUID()));
    }

    public boolean isExplicitOwner(Player player) {
        return player != null && ownerUUID != null && ownerUUID.equals(player.getUUID());
    }

    public void recordActivity() {
        lastActivityTick = level == null ? 0L : level.getGameTime();
        setChanged();
    }

    public boolean storeRewards(String missionId, List<ItemStack> rewards) {
        NonNullList<ItemStack> simulatedRewards = NonNullList.withSize(storedRewards.size(), ItemStack.EMPTY);
        for (int i = 0; i < storedRewards.size(); i++) {
            simulatedRewards.set(i, storedRewards.get(i).copy());
        }
        if (!insertRewards(simulatedRewards, rewards)) {
            return false;
        }
        for (int i = 0; i < storedRewards.size(); i++) {
            storedRewards.set(i, simulatedRewards.get(i));
        }
        setChanged();
        return true;
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
                    slots.set(i, remaining.copy());
                    remaining.setCount(0);
                    storedAny = true;
                }
            }
            if (!remaining.isEmpty()) {
                return false;
            }
        }
        return storedAny;
    }

    public boolean claimAllRewards(Player player) {
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
            player.sendSystemMessage(Component.literal("[ECHO-7] Terminal rewards claimed."));
            setChanged();
        }
        return claimedAny;
    }

    public int getStoredRewardCount() {
        int count = 0;
        for (ItemStack stack : storedRewards) {
            if (!stack.isEmpty()) {
                count += stack.getCount();
            }
        }
        return count;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
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
        input.getString("owner_uuid").ifPresent(value -> ownerUUID = UUID.fromString(value));
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
                int stackCount = count;
                BuiltInRegistries.ITEM.getOptional(Identifier.parse(itemId))
                        .ifPresent(item -> storedRewards.set(slot, new ItemStack(item, stackCount)));
            }
        }
    }
}
