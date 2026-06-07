package com.knoxhack.echospellcore.integration.lens;

import com.knoxhack.echolens.api.LensContext;
import com.knoxhack.echolens.api.LensDataCategory;
import com.knoxhack.echolens.api.LensInfoRow;
import com.knoxhack.echolens.api.LensInfoSection;
import com.knoxhack.echolens.api.LensTone;
import com.knoxhack.echolens.api.LensVisibility;
import com.knoxhack.echolens.api.ServerLensProvider;
import com.knoxhack.echospellcore.EchoSpellCore;
import com.knoxhack.echospellcore.api.SpellCoreApi;
import com.knoxhack.echospellcore.registry.ModItems;
import com.knoxhack.echospellcore.spell.StarterSpell;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

enum SpellCoreLensProvider implements ServerLensProvider {
    INSTANCE;

    @Override
    public Identifier id() {
        return EchoSpellCore.id("spellcore_focus_scan");
    }

    @Override
    public int priority() {
        return 88;
    }

    @Override
    public LensDataCategory category() {
        return LensDataCategory.INTEGRATION;
    }

    @Override
    public boolean supports(LensContext context) {
        return context != null && context.hasEntity() && context.entity() instanceof Player player
                && !focus(player).isEmpty();
    }

    @Override
    public List<LensInfoSection> inspect(LensContext context) {
        Player player = (Player) context.entity();
        ItemStack focus = focus(player);
        StarterSpell spell = StarterSpell.safe(SpellCoreApi.activeSpellId(player, focus));
        var summary = SpellCoreApi.focusSummary(player, focus);
        List<LensInfoRow> rows = new ArrayList<>();
        rows.add(row("Active Spell", spell.title(), "S", LensTone.ECHO, LensVisibility.COMPACT));
        rows.add(row("Spell Deck", summary.get("deck"), "D", LensTone.INFO, LensVisibility.COMPACT));
        rows.add(row("Modifiers", summary.get("modifiers"), "M",
                summary.get("modifiers").contains("overcharge") ? LensTone.WARNING : LensTone.INFO,
                LensVisibility.EXPANDED));
        rows.add(row("Aether", summary.get("aether"), "A", LensTone.INFO, LensVisibility.COMPACT));
        rows.add(row("Cooldown", summary.get("cooldown"), "C",
                "0".equals(summary.get("cooldown")) ? LensTone.GOOD : LensTone.WARNING, LensVisibility.COMPACT));
        rows.add(row("Feedback", summary.get("contamination"), "F",
                "0.00".equals(summary.get("contamination")) ? LensTone.GOOD : LensTone.WARNING,
                LensVisibility.EXPANDED));
        rows.add(row("Awakened Core", summary.get("awakened_core"), "U",
                "true".equals(summary.get("awakened_core")) ? LensTone.GOOD : LensTone.WARNING, LensVisibility.EXPANDED));
        return List.of(LensInfoSection.of(
                EchoSpellCore.id("lens/spellcore_focus"),
                LensDataCategory.INTEGRATION,
                "ECHO SpellCore",
                "S",
                LensTone.ECHO,
                LensVisibility.COMPACT,
                rows));
    }

    private static ItemStack focus(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.is(ModItems.SIGNAL_FOCUS.get())) {
            return main;
        }
        ItemStack offhand = player.getOffhandItem();
        return offhand.is(ModItems.SIGNAL_FOCUS.get()) ? offhand : ItemStack.EMPTY;
    }

    private static LensInfoRow row(String label, String value, String icon, LensTone tone, LensVisibility visibility) {
        return LensInfoRow.of(label, value, icon, tone, visibility);
    }
}
