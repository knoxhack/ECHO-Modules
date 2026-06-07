package com.knoxhack.echospellcore.client;

import com.knoxhack.echospellcore.api.SpellCoreApi;
import com.knoxhack.echospellcore.registry.ModItems;
import com.knoxhack.echospellcore.spell.StarterSpell;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class SpellHudOverlay {
    private SpellHudOverlay() {
    }

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || minecraft.screen != null) {
            return;
        }
        ItemStack focus = focus(minecraft);
        if (focus.isEmpty()) {
            return;
        }
        StarterSpell spell = StarterSpell.safe(SpellCoreApi.activeSpellId(minecraft.player, focus));
        ItemStack deck = SpellCoreApi.findDeck(minecraft.player);
        CompoundTag tag = data(focus);
        int screenW = minecraft.getWindow().getGuiScaledWidth();
        int screenH = minecraft.getWindow().getGuiScaledHeight();
        int width = 228;
        int height = deck.isEmpty() ? 62 : 76;
        int x = screenW - width - 12;
        int y = screenH - 112;
        Font font = minecraft.font;
        int spellColor = colorFor(spell);
        int border = 0xAA46E7FF;
        int panel = 0xAA080E18;
        graphics.fill(x, y, x + width, y + height, panel);
        graphics.outline(x, y, width, height, border);
        graphics.fill(x + 2, y + 2, x + 5, y + height - 2, spellColor);
        graphics.text(font, fit(font, "Signal Focus // " + spell.title(), 184), x + 9, y + 6, 0xFFE8FBFF, false);
        float aetherValue = tag.getFloatOr("Aether", 0.0F);
        float maxAether = Math.max(1.0F, tag.getFloatOr("MaxAether", 100.0F));
        String aether = ((int) aetherValue) + "/" + ((int) maxAether);
        long cooldown = tag.getLongOr("Cooldown", 0L);
        String status = cooldown > 0L ? "cooldown " + cooldown : "aether " + aether;
        graphics.text(font, status, x + 9, y + 20, cooldown > 0L ? 0xFFFFC35A : 0xFF7FFFD4, false);
        int barW = 102;
        int fill = Math.max(0, Math.min(barW, Math.round(barW * (aetherValue / maxAether))));
        graphics.fill(x + 116, y + 22, x + 116 + barW, y + 27, 0x66142735);
        graphics.fill(x + 116, y + 22, x + 116 + fill, y + 27, spellColor);
        if (!deck.isEmpty()) {
            int slot = SpellCoreApi.deckActiveSlot(deck);
            String modifiers = SpellCoreApi.deckModifierSummary(deck, slot);
            String deckLine = "Deck " + (slot + 1) + " " + SpellCoreApi.slotName(slot)
                    + (modifiers.isBlank() ? " // clean" : " // " + modifiers);
            graphics.text(font, fit(font, deckLine, 210), x + 9, y + 34, 0xFF9AD7FF, false);
            int used = SpellCoreApi.deckUsedSockets(deck, slot);
            for (int socket = 0; socket < SpellCoreApi.MODIFIER_SOCKETS; socket++) {
                int sx = x + 9 + socket * 18;
                graphics.fill(sx, y + 48, sx + 13, y + 55,
                        socket < used ? 0xAA46E7FF : 0x66213A4A);
                graphics.outline(sx, y + 48, 13, 7, 0x8846E7FF);
            }
        }
        float contamination = tag.getFloatOr("Contamination", 0.0F);
        float risk = tag.getFloatOr("CurseRisk", 0.0F);
        String modifiers = tag.getStringOr("Modifiers", "");
        int riskY = y + height - 18;
        int riskW = 78;
        int riskFill = Math.max(0, Math.min(riskW, Math.round(riskW * (risk / 0.15F))));
        int riskColor = risk >= 0.08F || contamination >= 0.5F ? 0xFFFF6A88 : 0xFFFFC35A;
        if (contamination > 0.0F || risk > 0.0F) {
            String warning = "feedback " + Math.round(contamination * 100.0F) + "% / risk "
                    + Math.round(risk * 100.0F) + "%";
            graphics.fill(x + 140, riskY + 2, x + 140 + riskW, riskY + 6, 0x663A2030);
            graphics.fill(x + 140, riskY + 2, x + 140 + riskFill, riskY + 6, riskColor);
            graphics.text(font, fit(font, warning, 128), x + 9, riskY, riskColor, false);
        }
        if (modifiers.contains("overcharge")) {
            graphics.text(font, "OVERCHARGE", x + 140, riskY - 8, 0xFFFFC35A, false);
        }
    }

    private static ItemStack focus(Minecraft minecraft) {
        ItemStack main = minecraft.player.getMainHandItem();
        if (main.is(ModItems.SIGNAL_FOCUS.get())) {
            return main;
        }
        ItemStack offhand = minecraft.player.getOffhandItem();
        return offhand.is(ModItems.SIGNAL_FOCUS.get()) ? offhand : ItemStack.EMPTY;
    }

    private static CompoundTag data(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    private static int colorFor(StarterSpell spell) {
        return switch (spell.school()) {
            case SIGNAL -> 0xFF6FFFE7;
            case AETHER -> 0xFFB68CFF;
            case ASH -> 0xFFFFA66A;
            case VOID, RIFT -> 0xFF8B6DFF;
            case STORM -> 0xFF6AE6FF;
            case CRYSTAL -> 0xFF9BFFD9;
            case BLOOD -> 0xFFFF6B7A;
            case SOUL -> 0xFF8CFFE1;
            case DECAY -> 0xFFB2D86C;
            case VEIL -> 0xFFD6B6FF;
            case FRACTURE -> 0xFFFF6AF3;
            default -> 0xFF46E7FF;
        };
    }

    private static String fit(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        return font.plainSubstrByWidth(text, Math.max(1, maxWidth - font.width("..."))) + "...";
    }
}
