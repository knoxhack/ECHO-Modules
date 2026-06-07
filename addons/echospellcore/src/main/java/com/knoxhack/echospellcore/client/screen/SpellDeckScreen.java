package com.knoxhack.echospellcore.client.screen;

import com.knoxhack.echocore.client.ui.EchoCyberGlassUi;
import com.knoxhack.echonetcore.client.EchoNetClientActions;
import com.knoxhack.echospellcore.api.SpellCoreApi;
import com.knoxhack.echospellcore.menu.SpellDeckMenu;
import com.knoxhack.echospellcore.network.SpellLoadoutActionPacket;
import com.knoxhack.echospellcore.registry.ModItems;
import com.knoxhack.echospellcore.spell.SpellModifier;
import com.knoxhack.echospellcore.spell.StarterSpell;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SpellDeckScreen extends AbstractContainerScreen<SpellDeckMenu> {
    private static final int PANEL = 0xEE071019;
    private static final int ACCENT = 0xFF46E7FF;
    private static final int SIGNAL = 0xFF6FFFE7;
    private static final int AETHER = 0xFFB68CFF;
    private static final int ASH = 0xFFFFA66A;
    private static final int VOID = 0xFF8B6DFF;
    private static final int STORM = 0xFF6AE6FF;
    private static final int CRYSTAL = 0xFF9BFFD9;
    private static final int BLOOD = 0xFFFF6B7A;
    private static final int SOUL = 0xFF8CFFE1;
    private static final int DECAY = 0xFFB2D86C;
    private static final int VEIL = 0xFFD6B6FF;
    private static final int FRACTURE = 0xFFFF6AF3;
    private static final int TEXT = 0xFFE8FBFF;
    private static final int DIM = 0xFF8AA5B8;
    private static final int WARNING = 0xFFFFC35A;
    private final List<Hitbox> hitboxes = new ArrayList<>();

    public SpellDeckScreen(SpellDeckMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, SpellDeckMenu.GUI_WIDTH, SpellDeckMenu.GUI_HEIGHT);
        this.inventoryLabelX = 116;
        this.inventoryLabelY = 226;
        this.titleLabelX = 14;
        this.titleLabelY = 12;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        hitboxes.clear();
        int x = leftPos;
        int y = topPos;
        ItemStack deck = menu.deck();
        SpellCoreApi.initializeDeck(deck);
        EchoCyberGlassUi.panel(graphics, x, y, imageWidth, imageHeight, PANEL, ACCENT);
        drawHeader(graphics, x, y, deck);
        drawSlots(graphics, x, y, deck, mouseX, mouseY);
        drawSpellMatrix(graphics, x, y, deck, mouseX, mouseY);
        drawModifiers(graphics, x, y, deck, mouseX, mouseY);
        for (Slot slot : menu.slots) {
            EchoCyberGlassUi.slot(graphics, x + slot.x, y + slot.y, 0xFF0B121A);
        }
        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, Component.literal("SPELLCORE // LOADOUT DECK"), titleLabelX, titleLabelY, ACCENT, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int mx = (int) event.x();
            int my = (int) event.y();
            if (menuSlotAt(mx, my) != null) {
                return super.mouseClicked(event, doubleClick);
            }
            for (Hitbox hitbox : hitboxes) {
                if (hitbox.contains(mx, my)) {
                    EchoNetClientActions.sendServerboundAction(new SpellLoadoutActionPacket(
                            hitbox.action, hitbox.slot, hitbox.spellId, hitbox.modifierId));
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void drawHeader(GuiGraphicsExtractor graphics, int x, int y, ItemStack deck) {
        graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + 42, 0xDD0D1824);
        int activeSlot = SpellCoreApi.deckActiveSlot(deck);
        StarterSpell activeSpell = SpellCoreApi.deckSlotSpell(deck, activeSlot);
        graphics.text(font, "ACTIVE SLOT " + (activeSlot + 1) + " // " + activeSpell.title(),
                x + 16, y + 28, colorFor(activeSpell), false);
        graphics.text(font, activeSpell.school().name().toLowerCase(Locale.ROOT) + " // cost "
                        + (int) activeSpell.cost() + " // cooldown " + activeSpell.cooldownTicks() + "t",
                x + 126, y + 14, riskFor(activeSpell) >= 5 ? WARNING : DIM, false);
        graphics.text(font, "risk " + riskFor(activeSpell) + "% // range " + (int) activeSpell.range(),
                x + 126, y + 28, riskFor(activeSpell) >= 5 ? WARNING : DIM, false);
    }

    private void drawSlots(GuiGraphicsExtractor graphics, int x, int y, ItemStack deck, int mouseX, int mouseY) {
        int sx = x + 16;
        int sy = y + 54;
        int active = SpellCoreApi.deckActiveSlot(deck);
        for (int slot = 0; slot < SpellCoreApi.LOADOUT_SLOTS; slot++) {
            StarterSpell spell = SpellCoreApi.deckSlotSpell(deck, slot);
            ItemStack core = menu.coreStack(slot);
            boolean installed = !core.isEmpty();
            boolean overcharged = installed && core.is(ModItems.OVERCHARGED_SPELL_CORE.get());
            int bx = sx;
            int by = sy + slot * 22;
            boolean selected = slot == active;
            boolean hovered = contains(mouseX, mouseY, bx + 24, by, 72, 18);
            int color = selected ? colorFor(spell) : hovered ? ACCENT : 0x55384652;
            graphics.fill(bx, by, bx + 96, by + 18, selected ? 0xFF122436 : 0xAA0A111A);
            graphics.outline(bx, by, 96, 18, color);
            graphics.outline(bx + 2, by, 18, 18, overcharged ? WARNING : installed ? colorFor(spell) : 0x66384652);
            graphics.text(font, fit(SpellCoreApi.slotName(slot), 26), bx + 25, by + 5, color, false);
            graphics.text(font, fit(spell.title(), 41), bx + 47, by + 5, TEXT, false);
            graphics.text(font, installed ? overcharged ? "hot" : "core" : "soft",
                    bx + 73, by + 5, overcharged ? WARNING : installed ? ACCENT : DIM, false);
            hitboxes.add(new Hitbox("select_slot", slot, spell.id(), "", bx + 24, by, 72, 18));
        }
    }

    private void drawSpellMatrix(GuiGraphicsExtractor graphics, int x, int y, ItemStack deck, int mouseX, int mouseY) {
        int activeSlot = SpellCoreApi.deckActiveSlot(deck);
        boolean activeHasCore = !menu.coreStack(activeSlot).isEmpty();
        int sx = x + 116;
        int sy = y + 52;
        graphics.text(font, activeHasCore ? "SPELL MATRIX // ITEM CORE" : "SPELL MATRIX",
                sx, sy - 14, activeHasCore ? WARNING : ACCENT, false);
        int index = 0;
        for (StarterSpell spell : StarterSpell.ordered()) {
            int col = index % 4;
            int row = index / 4;
            int bx = sx + col * 62;
            int by = sy + row * 26;
            boolean active = SpellCoreApi.deckSlotSpell(deck, activeSlot) == spell;
            boolean hovered = contains(mouseX, mouseY, bx, by, 56, 22);
            int color = colorFor(spell);
            int risk = riskFor(spell);
            graphics.fill(bx, by, bx + 56, by + 22, active ? 0xFF192638 : hovered ? 0xFF122030 : 0xAA090F18);
            graphics.outline(bx, by, 56, 22, active ? color : risk >= 5 ? 0x77FFC35A : 0x66384652);
            graphics.fill(bx, by + 20, bx + 56, by + 22, risk >= 5 ? 0x88FF6A88 : color & 0x55FFFFFF);
            graphics.text(font, fit(spell.title(), 49), bx + 4, by + 4, active ? color : TEXT, false);
            String meta = spell.school().name().substring(0, Math.min(3, spell.school().name().length()))
                    .toLowerCase(Locale.ROOT) + " " + (int) spell.cost() + "/" + spell.cooldownTicks();
            graphics.text(font, fit(meta, 49), bx + 4, by + 14, risk >= 5 ? WARNING : DIM, false);
            if (!activeHasCore) {
                hitboxes.add(new Hitbox("set_spell", activeSlot, spell.id(), "", bx, by, 56, 22));
            }
            index++;
        }
    }

    private void drawModifiers(GuiGraphicsExtractor graphics, int x, int y, ItemStack deck, int mouseX, int mouseY) {
        int activeSlot = SpellCoreApi.deckActiveSlot(deck);
        int mx = x + 116;
        int my = y + SpellDeckMenu.SOCKET_Y;
        graphics.text(font, "MODIFIER SOCKETS", mx, my - 18, ACCENT, false);
        for (int socket = 0; socket < SpellCoreApi.MODIFIER_SOCKETS; socket++) {
            int bx = x + SpellDeckMenu.SOCKET_X + socket * 26;
            SpellModifier installed = SpellCoreApi.modifierForSocketItem(menu.socketStack(socket));
            boolean occupied = installed != null;
            int color = occupied && installed == SpellModifier.OVERCHARGE ? WARNING : occupied ? ACCENT : 0x66384652;
            graphics.outline(bx - 1, y + SpellDeckMenu.SOCKET_Y - 1, 20, 20, color);
            String label = occupied ? installed.title() : "Socket " + (socket + 1);
            graphics.centeredText(font, fit(label, 50), bx + 9, my + 24, occupied ? color : DIM);
        }
        graphics.text(font, "Used " + SpellCoreApi.deckUsedSockets(deck, activeSlot) + "/"
                + SpellCoreApi.MODIFIER_SOCKETS, mx + 104, my + 5, DIM, false);
        String summary = SpellCoreApi.deckModifierSummary(deck, activeSlot);
        graphics.text(font, summary.isBlank() ? "No modifiers installed." : summary,
                mx + 104, my + 18, summary.contains("overcharge") ? WARNING : DIM, false);
    }

    private int colorFor(StarterSpell spell) {
        return switch (spell.school()) {
            case SIGNAL -> SIGNAL;
            case AETHER -> AETHER;
            case ASH -> ASH;
            case VOID, RIFT -> VOID;
            case STORM -> STORM;
            case CRYSTAL -> CRYSTAL;
            case BLOOD -> BLOOD;
            case SOUL -> SOUL;
            case DECAY -> DECAY;
            case VEIL -> VEIL;
            case FRACTURE -> FRACTURE;
            default -> ACCENT;
        };
    }

    private int riskFor(StarterSpell spell) {
        return switch (spell.school()) {
            case VOID, RIFT, BLOOD, DECAY, FRACTURE -> 5;
            case VEIL -> 3;
            case ASH -> 2;
            case STORM, CRYSTAL -> 1;
            default -> 0;
        };
    }

    private String fit(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        return font.plainSubstrByWidth(text, Math.max(1, maxWidth - font.width("..."))) + "...";
    }

    private static boolean contains(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private Slot menuSlotAt(int mouseX, int mouseY) {
        for (Slot slot : menu.slots) {
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            if (contains(mouseX, mouseY, x, y, 18, 18)) {
                return slot;
            }
        }
        return null;
    }

    private record Hitbox(String action, int slot, Identifier spellId, String modifierId,
            int x, int y, int width, int height) {
        boolean contains(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }
}
