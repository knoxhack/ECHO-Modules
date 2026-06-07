package com.knoxhack.signalos.block.entity;

import com.knoxhack.signalos.api.SignalOsDataRecord;
import com.knoxhack.signalos.item.SignalOsDataDriveItem;
import com.knoxhack.signalos.menu.SignalOsServerRackMenu;
import com.knoxhack.signalos.registry.ModBlockEntities;
import com.knoxhack.signalos.registry.ModBlocks;
import com.knoxhack.signalos.service.SignalOsComputerNetworkService;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class SignalOsServerRackBlockEntity extends BlockEntity implements MenuProvider {
    public static final int DRIVE_SLOTS = 4;
    private final DriveInventory drives = new DriveInventory(DRIVE_SLOTS, this::setChanged);

    public SignalOsServerRackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SERVER_RACK.get(), pos, state);
    }

    public DriveInventory drives() {
        return drives;
    }

    public boolean insertDrive(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.is(ModBlocks.DATA_DRIVE.get())
                || !SignalOsDataDriveItem.canInstall(stack)) {
            return false;
        }
        for (int i = 0; i < drives.getContainerSize(); i++) {
            if (drives.getItem(i).isEmpty()) {
                ItemStack inserted = stack.copyWithCount(1);
                SignalOsDataDriveItem.ensureV2Data(inserted);
                drives.setItem(i, inserted);
                setChanged();
                return true;
            }
        }
        return false;
    }

    public ItemStack extractDrive() {
        for (int i = drives.getContainerSize() - 1; i >= 0; i--) {
            ItemStack stack = drives.getItem(i);
            if (!stack.isEmpty()) {
                drives.setItem(i, ItemStack.EMPTY);
                setChanged();
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public int driveCount() {
        int count = 0;
        for (int i = 0; i < drives.getContainerSize(); i++) {
            if (!drives.getItem(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public List<SignalOsDataRecord> driveRecords() {
        List<SignalOsDataRecord> records = new ArrayList<>();
        for (int i = 0; i < drives.getContainerSize(); i++) {
            ItemStack stack = drives.getItem(i);
            if (!stack.isEmpty()) {
                com.knoxhack.signalos.api.SignalOsDriveData data = SignalOsDataDriveItem.data(stack);
                if (data.isV2Supported()) {
                    records.addAll(data.records());
                }
            }
        }
        return List.copyOf(records);
    }

    public String statusLine() {
        return "[SignalOS] Server Rack " + driveCount() + "/" + DRIVE_SLOTS + " drive(s), "
                + driveRecords().size() + " record(s). Sneak-use empty hand to eject the last drive.";
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("SignalOS Server Rack");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SignalOsServerRackMenu(containerId, playerInventory, this);
    }

    public void clearContent() {
        drives.clearContent();
        setChanged();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        SignalOsComputerNetworkService.invalidateCache();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        drives.serialize(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        drives.deserialize(input);
    }

    public static final class DriveInventory extends SimpleContainer {
        private final Runnable onChanged;

        private DriveInventory(int size, Runnable onChanged) {
            super(size);
            this.onChanged = onChanged;
        }

        public void serialize(ValueOutput output) {
            storeAsItemList(output.list("drives", ItemStack.CODEC));
        }

        public void deserialize(ValueInput input) {
            fromItemList(input.listOrEmpty("drives", ItemStack.CODEC));
        }

        @Override
        public void setChanged() {
            super.setChanged();
            onChanged.run();
        }
    }
}
