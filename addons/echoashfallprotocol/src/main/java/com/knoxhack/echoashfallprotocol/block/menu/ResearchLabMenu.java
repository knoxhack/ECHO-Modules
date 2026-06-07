package com.knoxhack.echoashfallprotocol.block.menu;

import com.knoxhack.echoashfallprotocol.registry.ModMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Research Lab Menu — zero-slot container used purely to open the Research UI.
 * Research data is read from player attachments (synced automatically).
 */
public class ResearchLabMenu extends AbstractContainerMenu {

    public ResearchLabMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory);
    }

    public ResearchLabMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.RESEARCH_LAB.get(), containerId);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
