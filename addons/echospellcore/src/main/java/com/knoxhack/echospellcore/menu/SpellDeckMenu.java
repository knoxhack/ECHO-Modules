package com.knoxhack.echospellcore.menu;

import com.knoxhack.echospellcore.api.SpellCoreApi;
import com.knoxhack.echospellcore.registry.ModMenus;
import com.knoxhack.echospellcore.spell.SpellModifier;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SpellDeckMenu extends AbstractContainerMenu {
    public static final int GUI_WIDTH = 388;
    public static final int GUI_HEIGHT = 320;
    public static final int CORE_SLOT_COUNT = SpellCoreApi.LOADOUT_SLOTS;
    public static final int SOCKET_SLOT_COUNT = SpellCoreApi.MODIFIER_SOCKETS;
    public static final int CORE_X = 18;
    public static final int CORE_Y = 54;
    public static final int SOCKET_X = 116;
    public static final int SOCKET_Y = 206;
    private static final int PLAYER_INV_X = 116;
    private static final int PLAYER_INV_Y = 238;
    private static final int SOCKET_START = 0;
    private static final int SOCKET_END = SOCKET_START + SOCKET_SLOT_COUNT;
    private static final int CORE_START = SOCKET_END;
    private static final int CORE_END = CORE_START + CORE_SLOT_COUNT;
    private static final int PLAYER_INV_START = CORE_END;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    private static final int HOTBAR_END = PLAYER_INV_END + 9;

    private final Inventory inventory;
    private final DeckCoreContainer cores;
    private final DeckSocketContainer sockets;
    private boolean loadingCores;
    private boolean loadingSockets;

    public SpellDeckMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory);
    }

    public SpellDeckMenu(int containerId, Inventory inventory) {
        super(ModMenus.SPELL_DECK.get(), containerId);
        this.inventory = inventory;
        this.cores = new DeckCoreContainer(this);
        this.sockets = new DeckSocketContainer(this);
        refreshCoresFromDeck();
        refreshSocketsFromDeck();
        for (int socket = 0; socket < SOCKET_SLOT_COUNT; socket++) {
            addSlot(new ModifierSocketSlot(sockets, socket, SOCKET_X + socket * 26, SOCKET_Y, this));
        }
        for (int slot = 0; slot < CORE_SLOT_COUNT; slot++) {
            addSlot(new SpellCoreSlot(cores, slot, CORE_X, CORE_Y + slot * 22, this));
        }
        addStandardInventorySlots(inventory, PLAYER_INV_X, PLAYER_INV_Y);
    }

    public ItemStack deck() {
        return SpellCoreApi.findDeck(inventory.player);
    }

    public int activeSlot() {
        ItemStack deck = deck();
        return deck.isEmpty() ? 0 : SpellCoreApi.deckActiveSlot(deck);
    }

    public ItemStack socketStack(int socket) {
        return socket >= 0 && socket < SOCKET_SLOT_COUNT ? sockets.getItem(socket) : ItemStack.EMPTY;
    }

    public ItemStack coreStack(int slot) {
        return slot >= 0 && slot < CORE_SLOT_COUNT ? cores.getItem(slot) : ItemStack.EMPTY;
    }

    public void refreshCoresFromDeck() {
        ItemStack deck = deck();
        loadingCores = true;
        try {
            for (int slot = 0; slot < CORE_SLOT_COUNT; slot++) {
                cores.setItem(slot, ItemStack.EMPTY);
            }
            if (!deck.isEmpty()) {
                SpellCoreApi.initializeDeck(deck);
                for (int slot = 0; slot < CORE_SLOT_COUNT; slot++) {
                    ItemStack core = SpellCoreApi.deckSlotCoreStack(deck, slot);
                    if (!core.isEmpty()) {
                        cores.setItem(slot, core);
                    }
                }
            }
        } finally {
            loadingCores = false;
        }
        broadcastChanges();
    }

    public void refreshSocketsFromDeck() {
        ItemStack deck = deck();
        loadingSockets = true;
        try {
            for (int socket = 0; socket < SOCKET_SLOT_COUNT; socket++) {
                sockets.setItem(socket, ItemStack.EMPTY);
            }
            if (!deck.isEmpty()) {
                SpellCoreApi.initializeDeck(deck);
                int activeSlot = SpellCoreApi.deckActiveSlot(deck);
                List<SpellModifier> seen = new ArrayList<>();
                for (int socket = 0; socket < SOCKET_SLOT_COUNT; socket++) {
                    SpellModifier modifier = SpellCoreApi.deckModifierAt(deck, activeSlot, socket);
                    if (modifier != null && !seen.contains(modifier)) {
                        sockets.setItem(socket, SpellCoreApi.modifierSocketStack(modifier));
                        seen.add(modifier);
                    }
                }
            }
        } finally {
            loadingSockets = false;
        }
        broadcastChanges();
    }

    public void saveCoresToDeck() {
        if (loadingCores) {
            return;
        }
        ItemStack deck = deck();
        if (deck.isEmpty()) {
            return;
        }
        for (int slot = 0; slot < CORE_SLOT_COUNT; slot++) {
            SpellCoreApi.setDeckSlotCore(deck, slot, cores.getItem(slot));
        }
    }

    public void saveSocketsToDeck() {
        if (loadingSockets) {
            return;
        }
        ItemStack deck = deck();
        if (deck.isEmpty()) {
            return;
        }
        int activeSlot = SpellCoreApi.deckActiveSlot(deck);
        for (int socket = 0; socket < SOCKET_SLOT_COUNT; socket++) {
            SpellCoreApi.setDeckSocketModifier(deck, activeSlot, socket,
                    SpellCoreApi.modifierForSocketItem(sockets.getItem(socket)));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return !SpellCoreApi.findDeck(player).isEmpty();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            copy = stack.copy();
            if (slotIndex < SOCKET_END) {
                if (!moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotIndex < CORE_END) {
                if (!moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (SpellCoreApi.isSpellCoreItem(stack)) {
                if (!moveItemStackTo(stack, CORE_START, CORE_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!SpellCoreApi.isModifierSocketItem(stack)
                    || !moveItemStackTo(stack, SOCKET_START, SOCKET_END, false)) {
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
    public void removed(Player player) {
        saveCoresToDeck();
        saveSocketsToDeck();
        super.removed(player);
    }

    private boolean canPlaceCore(int slot, ItemStack stack) {
        return SpellCoreApi.spellForCoreItem(stack) != null;
    }

    private boolean canPlaceSocket(int socket, ItemStack stack) {
        SpellModifier modifier = SpellCoreApi.modifierForSocketItem(stack);
        if (modifier == null) {
            return false;
        }
        int used = 0;
        List<SpellModifier> seen = new ArrayList<>();
        for (int existingSocket = 0; existingSocket < SOCKET_SLOT_COUNT; existingSocket++) {
            if (existingSocket == socket) {
                continue;
            }
            SpellModifier existing = SpellCoreApi.modifierForSocketItem(sockets.getItem(existingSocket));
            if (existing == null || seen.contains(existing)) {
                continue;
            }
            if (existing == modifier) {
                return false;
            }
            seen.add(existing);
            used += existing.socketCost();
        }
        return used + modifier.socketCost() <= SpellCoreApi.MODIFIER_SOCKETS
                && SpellCoreApi.canInstallSocketModifier(deck(), activeSlot(), socket, modifier);
    }

    private static final class DeckCoreContainer extends SimpleContainer {
        private final SpellDeckMenu menu;

        private DeckCoreContainer(SpellDeckMenu menu) {
            super(CORE_SLOT_COUNT);
            this.menu = menu;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if (!menu.loadingCores) {
                menu.saveCoresToDeck();
                menu.refreshSocketsFromDeck();
            }
        }
    }

    private static final class DeckSocketContainer extends SimpleContainer {
        private final SpellDeckMenu menu;

        private DeckSocketContainer(SpellDeckMenu menu) {
            super(SOCKET_SLOT_COUNT);
            this.menu = menu;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            menu.saveSocketsToDeck();
        }
    }

    private static final class SpellCoreSlot extends Slot {
        private final SpellDeckMenu menu;
        private final int deckSlot;

        private SpellCoreSlot(Container container, int slot, int x, int y, SpellDeckMenu menu) {
            super(container, slot, x, y);
            this.menu = menu;
            this.deckSlot = slot;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return menu.canPlaceCore(deckSlot, stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return 1;
        }
    }

    private static final class ModifierSocketSlot extends Slot {
        private final SpellDeckMenu menu;
        private final int socket;

        private ModifierSocketSlot(Container container, int slot, int x, int y, SpellDeckMenu menu) {
            super(container, slot, x, y);
            this.menu = menu;
            this.socket = slot;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return menu.canPlaceSocket(socket, stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return 1;
        }
    }
}
