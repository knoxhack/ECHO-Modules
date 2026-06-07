package com.knoxhack.echomultiblockcore.menu;

import com.knoxhack.echomultiblockcore.block.entity.MultiblockControllerBlockEntity;
import com.knoxhack.echomultiblockcore.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MultiblockControllerMenu extends AbstractContainerMenu {
    public static final int GUI_WIDTH = 332;
    public static final int GUI_HEIGHT = 276;
    public static final int DATA_COUNT = 17;
    public static final int DATA_STATE = 0;
    public static final int DATA_INTEGRITY = 1;
    public static final int DATA_COMPLETION = 2;
    public static final int DATA_ROBOTS = 3;
    public static final int DATA_QUEUE = 4;
    public static final int DATA_BLOCKED = 5;
    public static final int DATA_CAPABILITY_OK = 6;
    public static final int DATA_UPGRADES = 7;
    public static final int DATA_DAMAGE_GROUPS = 8;
    public static final int DATA_REPAIR_ACTIONS = 9;
    public static final int DATA_PROGRESSION_TIER = 10;
    public static final int DATA_FEATURED_RECIPES = 11;
    public static final int DATA_QUEUE_CAPACITY = 12;
    public static final int DATA_BLOCKED_TASKS = 13;
    public static final int DATA_ACTIVE_TASKS = 14;
    public static final int DATA_CAPABILITY_DIAGNOSTICS = 15;
    public static final int DATA_UPGRADE_SLOTS = 16;

    public static final int BUTTON_VALIDATE = 0;
    public static final int BUTTON_START = 1;
    public static final int BUTTON_CLEAR = 2;
    public static final int BUTTON_RETRY = 3;
    public static final int BUTTON_PAUSE = 4;
    public static final int BUTTON_RESUME = 5;
    public static final int BUTTON_REPAIR = 6;
    public static final int BUTTON_AUTOBUILD = 7;
    public static final int BUTTON_RECIPE_BASE = 100;
    public static final int BUTTON_RECIPE_LIMIT = BUTTON_RECIPE_BASE + 1024;

    private final ContainerData data;
    private final MultiblockControllerBlockEntity controller;

    public MultiblockControllerMenu(int containerId, Inventory inventory, MultiblockControllerBlockEntity controller, ContainerData data) {
        super(ModMenus.CONTROLLER.get(), containerId);
        checkContainerDataCount(data, DATA_COUNT);
        this.controller = controller;
        this.data = data;
        this.addDataSlots(data);
    }

    public MultiblockControllerMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null, new SimpleContainerData(DATA_COUNT));
    }

    public static MultiblockControllerMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MultiblockControllerBlockEntity controller) {
            return new MultiblockControllerMenu(containerId, inventory, controller, controller.menuData());
        }
        return new MultiblockControllerMenu(containerId, inventory);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return controller == null
                || (player != null && player.distanceToSqr(controller.getBlockPos().getX() + 0.5D,
                        controller.getBlockPos().getY() + 0.5D, controller.getBlockPos().getZ() + 0.5D) <= 64.0D);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return controller != null && controller.handleMenuButton(player, id);
    }

    public Identifier definitionId() {
        return controller == null ? null : controller.definitionId();
    }

    public int stateOrdinal() {
        return data.get(DATA_STATE);
    }

    public int integrity() {
        return data.get(DATA_INTEGRITY);
    }

    public int completion() {
        return data.get(DATA_COMPLETION);
    }

    public int robots() {
        return data.get(DATA_ROBOTS);
    }

    public int queueSize() {
        return data.get(DATA_QUEUE);
    }

    public boolean blocked() {
        return data.get(DATA_BLOCKED) > 0;
    }

    public boolean capabilityOk() {
        return data.get(DATA_CAPABILITY_OK) > 0;
    }

    public int upgrades() {
        return data.get(DATA_UPGRADES);
    }

    public int damageGroups() {
        return data.get(DATA_DAMAGE_GROUPS);
    }

    public int repairActions() {
        return data.get(DATA_REPAIR_ACTIONS);
    }

    public int progressionTier() {
        return data.get(DATA_PROGRESSION_TIER);
    }

    public int featuredRecipes() {
        return data.get(DATA_FEATURED_RECIPES);
    }

    public int queueCapacity() {
        return data.get(DATA_QUEUE_CAPACITY);
    }

    public int blockedTasks() {
        return data.get(DATA_BLOCKED_TASKS);
    }

    public int activeTasks() {
        return data.get(DATA_ACTIVE_TASKS);
    }

    public int capabilityDiagnostics() {
        return data.get(DATA_CAPABILITY_DIAGNOSTICS);
    }

    public int upgradeSlots() {
        return data.get(DATA_UPGRADE_SLOTS);
    }
}
