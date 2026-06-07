package com.knoxhack.echospellcore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echospellcore.EchoSpellCore;
import com.knoxhack.echospellcore.item.SpellCoreItem;
import com.knoxhack.echospellcore.item.SpellDeckItem;
import com.knoxhack.echospellcore.item.SpellModifierItem;
import com.knoxhack.echospellcore.item.SignalFocusItem;
import com.knoxhack.echospellcore.spell.SpellModifier;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

public final class ModItems {
    public static final Object ITEMS = EchoBackendRegistryBridge.create(BuiltInRegistries.ITEM, EchoSpellCore.MODID);
    private static final List<EchoBackendRegistryEntry<? extends Item>> CREATIVE_ITEMS = new ArrayList<>();

    public static final EchoBackendRegistryEntry<Item> SIGNAL_FOCUS = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "signal_focus", SignalFocusItem::new));
    public static final EchoBackendRegistryEntry<Item> AETHER_FOCUS = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "aether_focus", p -> new Item(p.stacksTo(1))));
    public static final EchoBackendRegistryEntry<Item> SPELL_DECK = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "spell_deck", SpellDeckItem::new));
    public static final EchoBackendRegistryEntry<Item> BLANK_SPELL_CORE = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "blank_spell_core", p -> new Item(p.stacksTo(16))));
    public static final EchoBackendRegistryEntry<Item> ENGRAVED_SPELL_CORE = tracked(EchoBackendRegistryBridge.registerItem(ITEMS,
            "engraved_spell_core", p -> new SpellCoreItem(p.stacksTo(16))));
    public static final EchoBackendRegistryEntry<Item> OVERCHARGED_SPELL_CORE = tracked(EchoBackendRegistryBridge.registerItem(ITEMS,
            "overcharged_spell_core", p -> new SpellCoreItem(p.stacksTo(8))));
    public static final EchoBackendRegistryEntry<Item> SIGNAL_CATALYST = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "signal_catalyst", Item::new));
    public static final EchoBackendRegistryEntry<Item> VOID_CATALYST = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "void_catalyst", Item::new));
    public static final EchoBackendRegistryEntry<Item> ASH_CATALYST = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "ash_catalyst", Item::new));
    public static final EchoBackendRegistryEntry<Item> BLOOD_CATALYST = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "blood_catalyst", Item::new));
    public static final EchoBackendRegistryEntry<Item> AETHER_CATALYST = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "aether_catalyst", Item::new));
    public static final EchoBackendRegistryEntry<Item> STORM_CATALYST = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "storm_catalyst", Item::new));
    public static final EchoBackendRegistryEntry<Item> RIFT_CATALYST = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "rift_catalyst", Item::new));
    public static final EchoBackendRegistryEntry<Item> SOUL_CATALYST = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "soul_catalyst", Item::new));
    public static final EchoBackendRegistryEntry<Item> CRYSTAL_CATALYST = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "crystal_catalyst", Item::new));
    public static final EchoBackendRegistryEntry<Item> DECAY_CATALYST = tracked(EchoBackendRegistryBridge.registerItem(ITEMS, "decay_catalyst", Item::new));
    public static final EchoBackendRegistryEntry<Item> RANGE_MODIFIER_SOCKET = tracked(EchoBackendRegistryBridge.registerItem(ITEMS,
            "range_modifier_socket", p -> new SpellModifierItem(p, SpellModifier.RANGE)));
    public static final EchoBackendRegistryEntry<Item> EFFICIENCY_MODIFIER_SOCKET = tracked(EchoBackendRegistryBridge.registerItem(ITEMS,
            "efficiency_modifier_socket", p -> new SpellModifierItem(p, SpellModifier.EFFICIENCY)));
    public static final EchoBackendRegistryEntry<Item> OVERCHARGE_MODIFIER_SOCKET = tracked(EchoBackendRegistryBridge.registerItem(ITEMS,
            "overcharge_modifier_socket", p -> new SpellModifierItem(p, SpellModifier.OVERCHARGE)));

    private ModItems() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(ITEMS, eventBus);
    }

    public static List<EchoBackendRegistryEntry<? extends Item>> creativeItems() {
        return List.copyOf(CREATIVE_ITEMS);
    }

    private static <T extends Item> EchoBackendRegistryEntry<T> tracked(EchoBackendRegistryEntry<T> item) {
        CREATIVE_ITEMS.add(item);
        return item;
    }
}
