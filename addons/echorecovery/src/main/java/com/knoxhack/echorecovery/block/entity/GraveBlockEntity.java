package com.knoxhack.echorecovery.block.entity;

import com.knoxhack.echorecovery.EchoRecovery;
import com.knoxhack.echorecovery.api.RecoveryGraveSnapshot;
import com.knoxhack.echorecovery.data.RecoveryWorldData;
import com.knoxhack.echorecovery.registry.ModBlockEntities;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class GraveBlockEntity extends BlockEntity implements Container {
    private UUID graveId = new UUID(0, 0);
    private UUID ownerId = new UUID(0, 0);
    private String ownerName = "";
    private long createdAt = 0;
    private long expiresAt = 0;
    private int xpStored = 0;
    private String deathCause = "";
    private String dimension = "";
    private String sourceDimension = "";
    private String graveTypeId = "echorecovery:vanilla_grave";
    private int graveType = 0;
    private boolean recovered = false;
    private boolean expired = false;
    private boolean contaminated = false;
    private boolean temporaryPlatform = false;
    private String deathMessage = "";
    private String hazardNotes = "";
    private final Set<UUID> sharedPlayers = new LinkedHashSet<>();
    private final NonNullList<ItemStack> items = NonNullList.withSize(54, ItemStack.EMPTY);
    private final int[] originalSlots = new int[54];

    public GraveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GRAVE.get(), pos, state);
        Arrays.fill(originalSlots, -1);
    }

    public void setOwner(UUID id, String name) {
        this.ownerId = id;
        this.ownerName = name == null ? "" : name;
        setChanged();
    }

    public void setGraveId(UUID id) {
        this.graveId = id == null ? new UUID(0, 0) : id;
        setChanged();
    }

    public UUID graveId() {
        return graveId;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public String ownerName() {
        return ownerName;
    }

    public long createdAt() {
        return createdAt;
    }

    public void setCreatedAt(long time) {
        this.createdAt = time;
        setChanged();
    }

    public long expiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long time) {
        this.expiresAt = Math.max(0L, time);
        setChanged();
    }

    public int xpStored() {
        return xpStored;
    }

    public void setXpStored(int xp) {
        this.xpStored = xp;
        setChanged();
    }

    public String deathCause() {
        return deathCause;
    }

    public void setDeathCause(String cause) {
        this.deathCause = cause == null ? "" : cause;
        setChanged();
    }

    public String dimension() {
        return dimension;
    }

    public void setDimension(String dim) {
        this.dimension = dim == null ? "" : dim;
        setChanged();
    }

    public String sourceDimension() {
        return sourceDimension.isBlank() ? dimension : sourceDimension;
    }

    public void setSourceDimension(String dim) {
        this.sourceDimension = dim == null ? "" : dim;
        setChanged();
    }

    public String graveTypeId() {
        return graveTypeId;
    }

    public void setGraveTypeId(String id) {
        this.graveTypeId = id == null || id.isBlank() ? "echorecovery:vanilla_grave" : id;
        setChanged();
    }

    public int graveType() {
        return graveType;
    }

    public void setGraveType(int type) {
        this.graveType = type;
        setChanged();
    }

    public boolean isRecovered() {
        return recovered;
    }

    public void setRecovered(boolean value) {
        this.recovered = value;
        setChanged();
    }

    public boolean isExpired() {
        return expired;
    }

    public void setExpired(boolean value) {
        this.expired = value;
        setChanged();
    }

    public boolean contaminated() {
        return contaminated;
    }

    public void setContaminated(boolean value) {
        this.contaminated = value;
        setChanged();
    }

    public boolean temporaryPlatform() {
        return temporaryPlatform;
    }

    public void setTemporaryPlatform(boolean value) {
        this.temporaryPlatform = value;
        setChanged();
    }

    public String deathMessage() {
        return deathMessage;
    }

    public void setDeathMessage(String msg) {
        this.deathMessage = msg == null ? "" : msg;
        setChanged();
    }

    public String hazardNotes() {
        return hazardNotes;
    }

    public List<String> hazardNoteList() {
        if (hazardNotes.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(hazardNotes.split("\\|"))
                .filter(note -> note != null && !note.isBlank())
                .toList();
    }

    public void setHazardNotes(List<String> notes) {
        this.hazardNotes = notes == null ? "" : String.join("|", notes);
        setChanged();
    }

    public boolean isSharedWith(UUID playerId) {
        return playerId != null && sharedPlayers.contains(playerId);
    }

    public void shareWith(UUID playerId) {
        if (playerId != null && sharedPlayers.add(playerId)) {
            setChanged();
            syncToWorldData();
        }
    }

    public void clearSharing(UUID playerId) {
        if (playerId != null && sharedPlayers.remove(playerId)) {
            setChanged();
        }
    }

    public Set<UUID> sharedPlayers() {
        return Set.copyOf(sharedPlayers);
    }

    public int originalSlot(int graveSlot) {
        return graveSlot >= 0 && graveSlot < originalSlots.length ? originalSlots[graveSlot] : -1;
    }

    public void setOriginalSlot(int graveSlot, int originalSlot) {
        if (graveSlot >= 0 && graveSlot < originalSlots.length) {
            originalSlots[graveSlot] = originalSlot;
            setChanged();
        }
    }

    public List<RecoveryWorldData.StoredSlot> storedSlotMetadata() {
        java.util.List<RecoveryWorldData.StoredSlot> slots = new java.util.ArrayList<>();
        for (int i = 0; i < originalSlots.length; i++) {
            if (originalSlots[i] >= 0 || !items.get(i).isEmpty()) {
                slots.add(new RecoveryWorldData.StoredSlot(i, originalSlots[i], slotKind(originalSlots[i])));
            }
        }
        return List.copyOf(slots);
    }

    public RecoveryGraveSnapshot snapshot() {
        return new RecoveryGraveSnapshot(
                graveId.toString(),
                ownerId,
                ownerName,
                worldPosition,
                dimension,
                graveTypeId,
                itemCount(),
                xpStored,
                createdAt,
                expiresAt,
                recovered,
                expired,
                contaminated,
                temporaryPlatform,
                hazardNoteList());
    }

    public NonNullList<ItemStack> items() {
        return items;
    }

    public void syncToWorldData() {
        if (level instanceof ServerLevel serverLevel
                && (graveId.getMostSignificantBits() != 0 || graveId.getLeastSignificantBits() != 0)) {
            RecoveryWorldData.getOrCreate(serverLevel).updateFromBlockEntity(this);
        }
    }

    public void setItems(NonNullList<ItemStack> stacks) {
        for (int i = 0; i < Math.min(stacks.size(), items.size()); i++) {
            items.set(i, stacks.get(i).copy());
        }
        setChanged();
    }

    public boolean isCompletelyEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return xpStored <= 0;
    }

    public int itemCount() {
        int count = 0;
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("GraveId", graveId.toString());
        output.putString("OwnerId", ownerId.toString());
        output.putString("OwnerName", ownerName);
        output.putLong("CreatedAt", createdAt);
        output.putLong("ExpiresAt", expiresAt);
        output.putInt("XpStored", xpStored);
        output.putString("DeathCause", deathCause);
        output.putString("Dimension", dimension);
        output.putString("SourceDimension", sourceDimension);
        output.putString("GraveTypeId", graveTypeId);
        output.putInt("GraveType", graveType);
        output.putBoolean("Recovered", recovered);
        output.putBoolean("Expired", expired);
        output.putBoolean("Contaminated", contaminated);
        output.putBoolean("TemporaryPlatform", temporaryPlatform);
        output.putString("DeathMessage", deathMessage);
        output.putString("HazardNotes", hazardNotes);
        output.putInt("SharedPlayerCount", sharedPlayers.size());
        int sharedIndex = 0;
        for (UUID sharedPlayer : sharedPlayers) {
            output.putString("SharedPlayer_" + sharedIndex++, sharedPlayer.toString());
        }
        for (int i = 0; i < originalSlots.length; i++) {
            output.putInt("OriginalSlot_" + i, originalSlots[i]);
        }
        storeItems(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getString("GraveId").ifPresent(s -> {
            try { graveId = UUID.fromString(s); } catch (IllegalArgumentException ignored) {}
        });
        input.getString("OwnerId").ifPresent(s -> {
            try { ownerId = UUID.fromString(s); } catch (IllegalArgumentException ignored) {}
        });
        ownerName = input.getStringOr("OwnerName", "");
        createdAt = input.getLongOr("CreatedAt", 0);
        expiresAt = input.getLongOr("ExpiresAt", 0);
        xpStored = input.getIntOr("XpStored", 0);
        deathCause = input.getStringOr("DeathCause", "");
        dimension = input.getStringOr("Dimension", "");
        sourceDimension = input.getStringOr("SourceDimension", "");
        graveTypeId = input.getStringOr("GraveTypeId", "echorecovery:vanilla_grave");
        graveType = input.getIntOr("GraveType", 0);
        recovered = input.getBooleanOr("Recovered", false);
        expired = input.getBooleanOr("Expired", false);
        contaminated = input.getBooleanOr("Contaminated", false);
        temporaryPlatform = input.getBooleanOr("TemporaryPlatform", false);
        deathMessage = input.getStringOr("DeathMessage", "");
        hazardNotes = input.getStringOr("HazardNotes", "");
        sharedPlayers.clear();
        int sharedCount = input.getIntOr("SharedPlayerCount", 0);
        for (int i = 0; i < sharedCount; i++) {
            input.getString("SharedPlayer_" + i).ifPresent(s -> {
                try { sharedPlayers.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
            });
        }
        Arrays.fill(originalSlots, -1);
        for (int i = 0; i < originalSlots.length; i++) {
            originalSlots[i] = input.getIntOr("OriginalSlot_" + i, -1);
        }
        loadItems(input);
    }

    private void storeItems(ValueOutput output) {
        int count = 0;
        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).isEmpty()) {
                count++;
            }
        }
        output.putInt("ItemCount", count);
        int idx = 0;
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                output.putInt("Slot_" + idx, i);
                output.store("Stack_" + idx, ItemStack.CODEC, stack);
                idx++;
            }
        }
    }

    private void loadItems(ValueInput input) {
        items.clear();
        int count = input.getIntOr("ItemCount", 0);
        for (int i = 0; i < count; i++) {
            int slot = input.getIntOr("Slot_" + i, -1);
            ItemStack stack = input.read("Stack_" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY);
            if (slot >= 0 && slot < items.size() && !stack.isEmpty()) {
                items.set(slot, stack);
            }
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        return tag;
    }

    public Component getDisplayName() {
        String name = EchoRecovery.displayName();
        if (!ownerName.isBlank()) {
            return Component.literal(name + " (" + ownerName + ")");
        }
        return Component.literal(name);
    }

    // Container implementation
    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < items.size() ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = getItem(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = stack.split(amount);
        if (stack.isEmpty()) {
            items.set(slot, ItemStack.EMPTY);
        }
        setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = getItem(slot);
        items.set(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < items.size()) {
            items.set(slot, stack);
            setChanged();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    private static String slotKind(int originalSlot) {
        if (originalSlot >= 36 && originalSlot <= 39) {
            return "equipment";
        }
        if (originalSlot == 40) {
            return "offhand";
        }
        return "inventory";
    }
}
