package com.knoxhack.echoritualcore.menu;

import com.knoxhack.echoritualcore.block.entity.BasicAltarBlockEntity;
import com.knoxhack.echoritualcore.registry.ModBlocks;
import com.knoxhack.echoritualcore.registry.ModMenus;
import com.knoxhack.echoritualcore.ritual.RitualStructureReport;
import com.knoxhack.echoritualcore.ritual.RitualStructureValidator;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class RitualAltarMenu extends AbstractContainerMenu {
    public static final int GUI_WIDTH = 276;
    public static final int GUI_HEIGHT = 188;

    private static final int DATA_RESULT = 0;
    private static final int DATA_STABILITY = 1;
    private static final int DATA_RUNES = 2;
    private static final int DATA_PEDESTALS = 3;
    private static final int DATA_MISSING = 4;
    private static final int DATA_PYLONS = 5;
    private static final int DATA_AUGMENTS = 6;
    public static final int DATA_COUNT = 7;

    private final Level level;
    private final BlockPos pos;
    private final ContainerData data;

    public RitualAltarMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, playerInventory.player.level(), buf.readBlockPos(), new SimpleContainerData(DATA_COUNT));
    }

    public RitualAltarMenu(int containerId, Inventory playerInventory, Level level, BlockPos pos) {
        this(containerId, playerInventory, level, pos, new LiveData(level, pos));
    }

    private RitualAltarMenu(int containerId, Inventory playerInventory, Level level, BlockPos pos, ContainerData data) {
        super(ModMenus.RITUAL_ALTAR.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.level = level;
        this.pos = pos == null ? BlockPos.ZERO : pos.immutable();
        this.data = data;
        addDataSlots(data);
    }

    public BlockPos pos() {
        return pos;
    }

    public int result() {
        return data.get(DATA_RESULT);
    }

    public int stability() {
        return data.get(DATA_STABILITY);
    }

    public int runes() {
        return data.get(DATA_RUNES);
    }

    public int pedestals() {
        return data.get(DATA_PEDESTALS);
    }

    public int missing() {
        return data.get(DATA_MISSING);
    }

    public int pylons() {
        return data.get(DATA_PYLONS);
    }

    public int augments() {
        return data.get(DATA_AUGMENTS);
    }

    public boolean structureReady() {
        return runes() >= RitualStructureValidator.REQUIRED_RUNE_CIRCLES && pedestals() > 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() == ModBlocks.BASIC_ALTAR.get()
                && stillValid(ContainerLevelAccess.create(level, pos), player, state.getBlock());
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
            RitualStructureReport report = RitualStructureValidator.validate(level, pos);
            return switch (index) {
                case DATA_RESULT -> altar() == null ? BasicAltarBlockEntity.RESULT_IDLE : altar().lastResult();
                case DATA_STABILITY -> report.stabilityScore();
                case DATA_RUNES -> report.runeCircles();
                case DATA_PEDESTALS -> report.pedestalCount();
                case DATA_MISSING -> report.missingCount();
                case DATA_PYLONS -> report.stabilityPylons();
                case DATA_AUGMENTS -> report.augmentCount();
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

        private BasicAltarBlockEntity altar() {
            if (level == null) {
                return null;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            return blockEntity instanceof BasicAltarBlockEntity altar ? altar : null;
        }
    }
}
