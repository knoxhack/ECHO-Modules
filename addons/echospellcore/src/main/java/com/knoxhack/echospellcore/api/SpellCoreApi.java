package com.knoxhack.echospellcore.api;

import com.knoxhack.echoarcanacore.api.AetherSignalType;
import com.knoxhack.echoarcanacore.api.ArcanaCoreServices;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.echoplatform.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echonetcore.api.EchoNetSend;
import com.knoxhack.echospellcore.EchoSpellCore;
import com.knoxhack.echospellcore.entity.SpellProjectileEntity;
import com.knoxhack.echospellcore.entity.SpellProjectileKind;
import com.knoxhack.echospellcore.network.SpellProjectileSyncPacket;
import com.knoxhack.echospellcore.registry.ModItems;
import com.knoxhack.echospellcore.spell.SpellModifier;
import com.knoxhack.echospellcore.spell.StarterSpell;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class SpellCoreApi {
    public static final Identifier SIGNAL_PULSE = EchoSpellCore.id("spell/signal_pulse");
    public static final Identifier ECHO_MARK = EchoSpellCore.id("spell/echo_mark");
    public static final Identifier STATIC_BURST = EchoSpellCore.id("spell/static_burst");
    public static final Identifier AETHER_BOLT = EchoSpellCore.id("spell/aether_bolt");
    public static final Identifier AETHER_SHIELD = EchoSpellCore.id("spell/aether_shield");
    public static final Identifier ARCANE_LIFT = EchoSpellCore.id("spell/arcane_lift");
    public static final Identifier ASH_VEIL = EchoSpellCore.id("spell/ash_veil");
    public static final Identifier DUST_LANCE = EchoSpellCore.id("spell/dust_lance");
    public static final Identifier CINDER_SKIN = EchoSpellCore.id("spell/cinder_skin");
    public static final Identifier VOID_STEP = EchoSpellCore.id("spell/void_step");
    public static final Identifier NULL_BOLT = EchoSpellCore.id("spell/null_bolt");
    public static final Identifier HOLLOW_CAGE = EchoSpellCore.id("spell/hollow_cage");
    public static final Identifier STORM_LANCE = EchoSpellCore.id("spell/storm_lance");
    public static final Identifier STATIC_DASH = EchoSpellCore.id("spell/static_dash");
    public static final Identifier THUNDER_CAGE = EchoSpellCore.id("spell/thunder_cage");
    public static final Identifier CRYSTAL_WALL = EchoSpellCore.id("spell/crystal_wall");
    public static final Identifier SHARD_BURST = EchoSpellCore.id("spell/shard_burst");
    public static final Identifier RESONANT_ARMOR = EchoSpellCore.id("spell/resonant_armor");
    public static final Identifier BLOOD_SURGE = EchoSpellCore.id("spell/blood_surge");
    public static final Identifier RIFT_BLINK = EchoSpellCore.id("spell/rift_blink");
    public static final Identifier SOUL_THREAD = EchoSpellCore.id("spell/soul_thread");
    public static final Identifier DECAY_TOUCH = EchoSpellCore.id("spell/decay_touch");
    public static final Identifier VEIL_TRACE = EchoSpellCore.id("spell/veil_trace");
    public static final Identifier FRACTURE_SHEAR = EchoSpellCore.id("spell/fracture_shear");
    public static final Identifier SIGNAL_FOCUS = EchoSpellCore.id("signal_focus");
    public static final Identifier CAST_EVENT = EchoSpellCore.id("spell_cast");
    public static final int LOADOUT_SLOTS = 6;
    public static final int MODIFIER_SOCKETS = 3;
    private static final Identifier AWAKENED_SPELL_CORE =
            Identifier.fromNamespaceAndPath("echoritualcore", "awakened_spell_core");
    private static final String PLAYER_ROOT = "echospellcore_player";
    private static final String DECK_ACTIVE_SLOT = "ActiveSlot";
    private static final String DECK_SLOT_PREFIX = "Slot";
    private static final String DECK_MODIFIER_PREFIX = "Modifiers";
    private static final String DECK_SOCKET_PREFIX = "Socket";
    private static final String DECK_CORE_INSTALLED_PREFIX = "CoreInstalled";
    private static final String DECK_CORE_OVERCHARGED_PREFIX = "CoreOvercharged";
    private static final String SPELL_CORE_ID = "SpellId";
    private static final String SPELL_CORE_OVERCHARGED = "Overcharged";
    private static final String STACK_SELECTED = "SelectedSpell";
    private static final String STACK_STATUS = "Status";
    private static final String STACK_AETHER = "Aether";
    private static final String STACK_MAX_AETHER = "MaxAether";
    private static final String STACK_COOLDOWN = "Cooldown";
    private static final String STACK_MODIFIERS = "Modifiers";
    private static final String STACK_CONTAMINATION = "Contamination";
    private static final String STACK_CURSE_RISK = "CurseRisk";

    private SpellCoreApi() {
    }

    public static Identifier selectedSpell(ItemStack stack) {
        CompoundTag tag = customData(stack);
        Identifier parsed = Identifier.tryParse(tag.getStringOr(STACK_SELECTED, SIGNAL_PULSE.toString()));
        return StarterSpell.safe(parsed).id();
    }

    public static void selectSpell(ItemStack stack, Identifier spellId) {
        StarterSpell spell = StarterSpell.safe(spellId);
        CompoundTag tag = customData(stack);
        tag.putString(STACK_SELECTED, spell.id().toString());
        tag.putString(STACK_STATUS, "Selected " + spell.title() + ".");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static Identifier cycleSpell(ItemStack stack) {
        List<StarterSpell> spells = StarterSpell.ordered();
        StarterSpell current = StarterSpell.safe(selectedSpell(stack));
        int next = Math.floorMod(spells.indexOf(current) + 1, spells.size());
        StarterSpell spell = spells.get(next);
        selectSpell(stack, spell.id());
        return spell.id();
    }

    public static boolean tryCast(ServerPlayer player, ItemStack focus, Identifier requestedSpellId) {
        if (player == null || focus == null || focus.isEmpty()) {
            return false;
        }
        CastProfile profile = castProfile(player, requestedSpellId);
        StarterSpell spell = profile.spell();
        if (!hasAwakenedSpellCore(player)) {
            setFocusStatus(focus, spell, "Awakened Spell Core required.", player);
            player.sendSystemMessage(Component.translatable("item.echospellcore.signal_focus.requires_awakened_core"));
            recordMission(player, EchoSpellCore.id("missing_awakened_spell_core"), "blocked");
            return false;
        }
        long remaining = cooldownRemaining(player, spell);
        if (remaining > 0L) {
            setFocusStatus(focus, spell, "Cooldown " + remaining + " ticks.", player);
            player.sendSystemMessage(Component.translatable("item.echospellcore.signal_focus.cooldown", remaining));
            recordMission(player, EchoSpellCore.id("cooldown_checked"), "cooldown");
            return false;
        }
        if (!ArcanaCoreServices.aether().consumeAether(player, profile.cost(), spell.aetherType())) {
            setFocusStatus(focus, spell, "Aether low.", player);
            player.sendSystemMessage(Component.translatable("item.echospellcore.signal_focus.no_aether", profile.cost()));
            tryApplyBacklashCurse(player, spell);
            return false;
        }
        boolean succeeded = switch (spell) {
            case SIGNAL_PULSE -> castSignalPulse(player, profile);
            case ECHO_MARK -> castEchoMark(player, profile);
            case STATIC_BURST -> castStaticBurst(player, profile);
            case AETHER_BOLT -> castProjectile(player, profile, SpellProjectileKind.AETHER_BOLT);
            case AETHER_SHIELD -> castAetherShield(player, profile);
            case ARCANE_LIFT -> castArcaneLift(player, profile);
            case ASH_VEIL -> castAshVeil(player, profile);
            case DUST_LANCE -> castProjectile(player, profile, SpellProjectileKind.DUST_LANCE);
            case CINDER_SKIN -> castCinderSkin(player, profile);
            case VOID_STEP -> castVoidStep(player, profile);
            case NULL_BOLT -> castProjectile(player, profile, SpellProjectileKind.NULL_BOLT);
            case HOLLOW_CAGE -> castHollowCage(player, profile);
            case STORM_LANCE -> castProjectile(player, profile, SpellProjectileKind.STORM_LANCE);
            case STATIC_DASH -> castStaticDash(player, profile);
            case THUNDER_CAGE -> castThunderCage(player, profile);
            case CRYSTAL_WALL -> castCrystalWall(player, profile);
            case SHARD_BURST -> castShardBurst(player, profile);
            case RESONANT_ARMOR -> castResonantArmor(player, profile);
            case BLOOD_SURGE -> castBloodSurge(player, profile);
            case RIFT_BLINK -> castRiftBlink(player, profile);
            case SOUL_THREAD -> castSoulThread(player, profile);
            case DECAY_TOUCH -> castDecayTouch(player, profile);
            case VEIL_TRACE -> castVeilTrace(player, profile);
            case FRACTURE_SHEAR -> castProjectile(player, profile, SpellProjectileKind.FRACTURE_SHEAR);
        };
        if (!succeeded) {
            return false;
        }
        setCooldown(player, spell, profile.cooldownTicks());
        setFocusStatus(focus, spell, "Cast " + spell.title() + ".", player);
        maybeApplyOverchargeBacklash(player, profile);
        SpellCoreEvents.fireCast(player, spell.id(), focus);
        recordMission(player, spell.id(), "cast");
        return true;
    }

    public static void tickFocus(ServerPlayer player, ItemStack focus, boolean selected) {
        if (player == null || focus == null || focus.isEmpty() || !focus.is(ModItems.SIGNAL_FOCUS.get())) {
            return;
        }
        Identifier selectedSpell = activeSpellId(player, focus);
        StarterSpell spell = StarterSpell.safe(selectedSpell);
        if (selected || hasAwakenedSpellCore(player)) {
            ArcanaCoreServices.aether().addAether(player, 0.15D, AetherSignalType.RAW_AETHER);
            ArcanaCoreServices.aether().addAether(player, 0.10D, AetherSignalType.SIGNAL_AETHER);
            if (spell.school() == com.knoxhack.echoarcanacore.api.SpellSchool.CRYSTAL) {
                ArcanaCoreServices.aether().addAether(player, 0.06D, AetherSignalType.REFINED_AETHER);
            } else if (spell.school() == com.knoxhack.echoarcanacore.api.SpellSchool.VOID
                    || spell.school() == com.knoxhack.echoarcanacore.api.SpellSchool.RIFT) {
                ArcanaCoreServices.aether().addAether(player, 0.04D, AetherSignalType.RIFT_AETHER);
            } else if (spell.school() == com.knoxhack.echoarcanacore.api.SpellSchool.SOUL) {
                ArcanaCoreServices.aether().addAether(player, 0.04D, AetherSignalType.SOUL_AETHER);
            } else if (spell.school() == com.knoxhack.echoarcanacore.api.SpellSchool.BLOOD
                    || spell.school() == com.knoxhack.echoarcanacore.api.SpellSchool.DECAY) {
                ArcanaCoreServices.aether().addAether(player, 0.035D, AetherSignalType.CURSED_AETHER);
            } else if (spell.school() == com.knoxhack.echoarcanacore.api.SpellSchool.VEIL) {
                ArcanaCoreServices.aether().addAether(player, 0.035D, AetherSignalType.VEIL_RESONANCE);
            } else if (spell.school() == com.knoxhack.echoarcanacore.api.SpellSchool.FRACTURE) {
                ArcanaCoreServices.aether().addAether(player, 0.03D, AetherSignalType.FRACTURE_ENERGY);
            }
        }
        setFocusStatus(focus, spell, customData(focus).getStringOr(STACK_STATUS, "Focus synchronized."), player);
    }

    public static long cooldownRemaining(Player player, StarterSpell spell) {
        if (player == null || spell == null) {
            return 0L;
        }
        CompoundTag root = playerRoot(player);
        long readyAt = root.getLongOr("cooldown_" + spell.path(), 0L);
        return Math.max(0L, readyAt - player.level().getGameTime());
    }

    public static Map<String, String> focusSummary(Player player, ItemStack focus) {
        Identifier selected = activeSpellId(player, focus);
        StarterSpell spell = StarterSpell.safe(selected);
        ItemStack deck = findDeck(player);
        return Map.of(
                "selected_spell", spell.id().toString(),
                "aether", String.format(java.util.Locale.ROOT, "%.0f/%.0f",
                        ArcanaCoreServices.aether().getAether(player, spell.aetherType()),
                        ArcanaCoreServices.aether().getMaxAether(player, spell.aetherType())),
                "cooldown", Long.toString(cooldownRemaining(player, spell)),
                "awakened_core", Boolean.toString(hasAwakenedSpellCore(player)),
                "deck", deck.isEmpty() ? "none" : "slot_" + (deckActiveSlot(deck) + 1),
                "core_state", deckCoreState(deck),
                "modifiers", deck.isEmpty() ? "none" : deckModifierSummary(deck, deckActiveSlot(deck)),
                "contamination", String.format(java.util.Locale.ROOT, "%.2f",
                        ArcanaCoreServices.aether().getContamination(player)),
                "curse_risk", String.format(java.util.Locale.ROOT, "%.2f", castProfile(player, selected).curseRisk()),
                "school", spell.school().name().toLowerCase(java.util.Locale.ROOT));
    }

    public static Identifier activeSpellId(Player player, ItemStack focus) {
        ItemStack deck = findDeck(player);
        if (!deck.isEmpty()) {
            initializeDeck(deck);
            return deckSlotSpell(deck, deckActiveSlot(deck)).id();
        }
        return focus == null || focus.isEmpty() ? SIGNAL_PULSE : selectedSpell(focus);
    }

    public static ItemStack findDeck(Player player) {
        if (player == null) {
            return ItemStack.EMPTY;
        }
        ItemStack main = player.getMainHandItem();
        if (main.is(ModItems.SPELL_DECK.get())) {
            return main;
        }
        ItemStack offhand = player.getOffhandItem();
        if (offhand.is(ModItems.SPELL_DECK.get())) {
            return offhand;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.SPELL_DECK.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static void initializeDeck(ItemStack deck) {
        if (deck == null || deck.isEmpty() || !deck.is(ModItems.SPELL_DECK.get())) {
            return;
        }
        CompoundTag tag = customData(deck);
        boolean changed = false;
        if (tag.getIntOr(DECK_ACTIVE_SLOT, -1) < 0) {
            tag.putInt(DECK_ACTIVE_SLOT, 0);
            changed = true;
        }
        List<StarterSpell> spells = StarterSpell.ordered();
        for (int slot = 0; slot < LOADOUT_SLOTS; slot++) {
            String key = slotKey(slot);
            if (tag.getStringOr(key, "").isBlank()) {
                tag.putString(key, spells.get(slot % spells.size()).id().toString());
                changed = true;
            }
        }
        if (changed) {
            deck.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    public static int deckActiveSlot(ItemStack deck) {
        CompoundTag tag = customData(deck);
        return Math.max(0, Math.min(LOADOUT_SLOTS - 1, tag.getIntOr(DECK_ACTIVE_SLOT, 0)));
    }

    public static StarterSpell deckSlotSpell(ItemStack deck, int slot) {
        initializeDeck(deck);
        CompoundTag tag = customData(deck);
        Identifier id = Identifier.tryParse(tag.getStringOr(slotKey(slot), SIGNAL_PULSE.toString()));
        return StarterSpell.safe(id);
    }

    public static ItemStack spellCoreStack(StarterSpell spell, boolean overcharged) {
        StarterSpell safeSpell = spell == null ? StarterSpell.SIGNAL_PULSE : spell;
        ItemStack stack = new ItemStack(overcharged ? ModItems.OVERCHARGED_SPELL_CORE.get()
                : ModItems.ENGRAVED_SPELL_CORE.get());
        CompoundTag tag = new CompoundTag();
        tag.putString(SPELL_CORE_ID, safeSpell.id().toString());
        tag.putBoolean(SPELL_CORE_OVERCHARGED, overcharged);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static StarterSpell spellForCoreItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        if (!stack.is(ModItems.ENGRAVED_SPELL_CORE.get()) && !stack.is(ModItems.OVERCHARGED_SPELL_CORE.get())) {
            return null;
        }
        CompoundTag tag = customData(stack);
        Identifier id = Identifier.tryParse(tag.getStringOr(SPELL_CORE_ID, SIGNAL_PULSE.toString()));
        return StarterSpell.safe(id);
    }

    public static boolean isSpellCoreItem(ItemStack stack) {
        return spellForCoreItem(stack) != null;
    }

    public static boolean deckHasInstalledCore(ItemStack deck, int slot) {
        if (deck == null || deck.isEmpty() || !deck.is(ModItems.SPELL_DECK.get())) {
            return false;
        }
        return customData(deck).getBooleanOr(coreInstalledKey(slot), false);
    }

    public static ItemStack deckSlotCoreStack(ItemStack deck, int slot) {
        if (!deckHasInstalledCore(deck, slot)) {
            return ItemStack.EMPTY;
        }
        CompoundTag tag = customData(deck);
        return spellCoreStack(deckSlotSpell(deck, slot), tag.getBooleanOr(coreOverchargedKey(slot), false));
    }

    public static void setDeckSlotCore(ItemStack deck, int slot, ItemStack stack) {
        if (deck == null || deck.isEmpty() || !deck.is(ModItems.SPELL_DECK.get())) {
            return;
        }
        initializeDeck(deck);
        CompoundTag tag = customData(deck);
        int safeSlot = Math.floorMod(slot, LOADOUT_SLOTS);
        StarterSpell spell = spellForCoreItem(stack);
        if (spell == null) {
            tag.remove(coreInstalledKey(safeSlot));
            tag.remove(coreOverchargedKey(safeSlot));
            deck.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            return;
        }
        tag.putString(slotKey(safeSlot), spell.id().toString());
        tag.putBoolean(coreInstalledKey(safeSlot), true);
        tag.putBoolean(coreOverchargedKey(safeSlot), stack.is(ModItems.OVERCHARGED_SPELL_CORE.get())
                || customData(stack).getBooleanOr(SPELL_CORE_OVERCHARGED, false));
        deck.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean deckHasModifier(ItemStack deck, int slot, SpellModifier modifier) {
        return deckModifiers(deck, slot).contains(modifier);
    }

    public static SpellModifier deckModifierAt(ItemStack deck, int slot, int socket) {
        initializeDeck(deck);
        CompoundTag tag = customData(deck);
        migrateLegacyModifiers(tag, slot);
        String value = tag.getStringOr(socketKey(slot, socket), "");
        return SpellModifier.byId(value).orElse(null);
    }

    public static String deckModifierSummary(ItemStack deck, int slot) {
        List<String> ids = deckModifiers(deck, slot).stream().map(SpellModifier::id).toList();
        return String.join(", ", ids);
    }

    public static ItemStack modifierSocketStack(SpellModifier modifier) {
        if (modifier == null) {
            return ItemStack.EMPTY;
        }
        return switch (modifier) {
            case RANGE -> new ItemStack(ModItems.RANGE_MODIFIER_SOCKET.get());
            case EFFICIENCY -> new ItemStack(ModItems.EFFICIENCY_MODIFIER_SOCKET.get());
            case OVERCHARGE -> new ItemStack(ModItems.OVERCHARGE_MODIFIER_SOCKET.get());
        };
    }

    public static SpellModifier modifierForSocketItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        if (stack.is(ModItems.RANGE_MODIFIER_SOCKET.get())) {
            return SpellModifier.RANGE;
        }
        if (stack.is(ModItems.EFFICIENCY_MODIFIER_SOCKET.get())) {
            return SpellModifier.EFFICIENCY;
        }
        if (stack.is(ModItems.OVERCHARGE_MODIFIER_SOCKET.get())) {
            return SpellModifier.OVERCHARGE;
        }
        return null;
    }

    public static boolean isModifierSocketItem(ItemStack stack) {
        return modifierForSocketItem(stack) != null;
    }

    public static void setDeckSocketModifier(ItemStack deck, int slot, int socket, SpellModifier modifier) {
        if (deck == null || deck.isEmpty() || !deck.is(ModItems.SPELL_DECK.get())) {
            return;
        }
        initializeDeck(deck);
        CompoundTag tag = customData(deck);
        migrateLegacyModifiers(tag, slot);
        String key = socketKey(slot, socket);
        if (modifier == null) {
            tag.remove(key);
        } else {
            tag.putString(key, modifier.id());
        }
        deck.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean canInstallSocketModifier(ItemStack deck, int slot, int socket, SpellModifier modifier) {
        if (modifier == null || deck == null || deck.isEmpty() || !deck.is(ModItems.SPELL_DECK.get())) {
            return false;
        }
        initializeDeck(deck);
        int used = 0;
        List<SpellModifier> seen = new ArrayList<>();
        for (int existingSocket = 0; existingSocket < MODIFIER_SOCKETS; existingSocket++) {
            if (existingSocket == Math.floorMod(socket, MODIFIER_SOCKETS)) {
                continue;
            }
            SpellModifier existing = deckModifierAt(deck, slot, existingSocket);
            if (existing == null || seen.contains(existing)) {
                continue;
            }
            if (existing == modifier) {
                return false;
            }
            seen.add(existing);
            used += existing.socketCost();
        }
        return used + modifier.socketCost() <= MODIFIER_SOCKETS;
    }

    public static int deckUsedSockets(ItemStack deck, int slot) {
        int used = 0;
        for (SpellModifier modifier : deckModifiers(deck, slot)) {
            used += modifier.socketCost();
        }
        return Math.min(MODIFIER_SOCKETS, used);
    }

    public static String slotName(int slot) {
        return switch (Math.floorMod(slot, LOADOUT_SLOTS)) {
            case 0 -> "Pulse";
            case 1 -> "Field";
            case 2 -> "Shift";
            case 3 -> "Ward";
            case 4 -> "Surge";
            default -> "Passive";
        };
    }

    public static void applyLoadoutAction(ServerPlayer player, String action, int slot, Identifier spellId,
            String modifierId) {
        ItemStack deck = findDeck(player);
        if (deck.isEmpty()) {
            player.sendSystemMessage(Component.translatable("screen.echospellcore.spell_deck.no_deck"));
            return;
        }
        initializeDeck(deck);
        int safeSlot = Math.max(0, Math.min(LOADOUT_SLOTS - 1, slot));
        CompoundTag tag = customData(deck);
        switch (action == null ? "" : action) {
            case "select_slot" -> tag.putInt(DECK_ACTIVE_SLOT, safeSlot);
            case "set_spell" -> tag.putString(slotKey(safeSlot), StarterSpell.safe(spellId).id().toString());
            case "toggle_modifier" -> {
                if (!toggleModifier(tag, safeSlot, modifierId)) {
                    player.sendSystemMessage(Component.translatable("screen.echospellcore.spell_deck.socket_full",
                            slotName(safeSlot)));
                    return;
                }
            }
            default -> {
                return;
            }
        }
        deck.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        StarterSpell spell = deckSlotSpell(deck, deckActiveSlot(deck));
        player.sendSystemMessage(Component.translatable("screen.echospellcore.spell_deck.updated",
                slotName(deckActiveSlot(deck)), spell.title()));
        recordMission(player, EchoSpellCore.id("loadout_configured"), "loadout");
    }

    public static boolean hasAwakenedSpellCore(Player player) {
        if (player == null || player.getAbilities().instabuild) {
            return true;
        }
        if (!EchoRuntimeModules.isLoaded("echoritualcore")
                || BuiltInRegistries.ITEM.getOptional(AWAKENED_SPELL_CORE).isEmpty()) {
            return true;
        }
        var item = BuiltInRegistries.ITEM.getOptional(AWAKENED_SPELL_CORE).orElse(null);
        if (item == null) {
            return true;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(item)) {
                return true;
            }
        }
        return false;
    }

    private static CastProfile castProfile(Player player, Identifier requestedSpellId) {
        StarterSpell spell = StarterSpell.safe(requestedSpellId);
        List<SpellModifier> modifiers = List.of();
        ItemStack deck = findDeck(player);
        int activeSlot = -1;
        boolean overchargedCore = false;
        if (!deck.isEmpty()) {
            initializeDeck(deck);
            activeSlot = deckActiveSlot(deck);
            spell = deckSlotSpell(deck, activeSlot);
            modifiers = deckModifiers(deck, activeSlot);
            overchargedCore = customData(deck).getBooleanOr(coreOverchargedKey(activeSlot), false);
        }
        double costScale = 1.0D;
        double rangeScale = 1.0D;
        double damageScale = 1.0D;
        double curseRisk = baseCurseRisk(spell);
        for (SpellModifier modifier : modifiers) {
            costScale *= modifier.costScale();
            rangeScale *= modifier.rangeScale();
            damageScale *= modifier.damageScale();
            curseRisk += modifier.curseRisk();
        }
        if (overchargedCore) {
            costScale *= 1.08D;
            damageScale *= 1.18D;
            curseRisk += 0.04D;
        }
        int cost = Math.max(1, (int) Math.ceil(spell.cost() * costScale));
        int cooldown = modifiers.contains(SpellModifier.EFFICIENCY)
                ? Math.max(8, spell.cooldownTicks() - 10)
                : spell.cooldownTicks();
        if (modifiers.contains(SpellModifier.OVERCHARGE) || overchargedCore) {
            cooldown += 25;
        }
        return new CastProfile(spell, modifiers, activeSlot, cost, cooldown,
                spell.range() * rangeScale, damageScale, curseRisk);
    }

    private static boolean castSignalPulse(ServerPlayer player, CastProfile profile) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        int marked = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().expandTowards(look.scale(profile.range())).inflate(4.0D),
                entity -> entity != player && entity.isAlive())) {
            Vec3 toTarget = target.getEyePosition().subtract(eye);
            if (toTarget.length() > profile.range() || toTarget.normalize().dot(look) < 0.45D) {
                continue;
            }
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 140, 0, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 0, false, true));
            marked++;
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.1D, player.getZ(),
                18, 1.6D, 0.45D, 1.6D, 0.02D);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                0.6F, 1.55F);
        player.sendSystemMessage(Component.translatable("spell.echospellcore.signal_pulse.cast", marked));
        return true;
    }

    private static boolean castEchoMark(ServerPlayer player, CastProfile profile) {
        ServerLevel level = (ServerLevel) player.level();
        LivingEntity target = findLookTarget(player, profile.range(), 0.78D);
        if (target == null) {
            player.sendSystemMessage(Component.translatable("spell.echospellcore.echo_mark.miss"));
            return true;
        }
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 220, 0, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0, false, true));
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + target.getBbHeight() * 0.65D,
                target.getZ(), 16, 0.18D, 0.3D, 0.18D, 0.02D);
        level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                0.55F, 1.35F);
        player.sendSystemMessage(Component.translatable("spell.echospellcore.echo_mark.cast",
                target.getDisplayName()));
        return true;
    }

    private static boolean castStaticBurst(ServerPlayer player, CastProfile profile) {
        ServerLevel level = (ServerLevel) player.level();
        double radius = Math.max(3.0D, profile.range());
        int disrupted = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                entity -> entity != player && entity.isAlive() && !entity.isAlliedTo(player))) {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 55, 1, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 70, 0, false, true));
            Vec3 push = target.position().subtract(player.position());
            if (push.lengthSqr() > 1.0E-6D) {
                push = push.normalize().scale(0.18D * profile.damageScale());
                target.push(push.x, 0.03D, push.z);
            }
            disrupted++;
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 0.8D, player.getZ(),
                26, radius * 0.28D, 0.45D, radius * 0.28D, 0.04D);
        level.playSound(null, player.blockPosition(), SoundEvents.COPPER_BULB_TURN_ON, SoundSource.PLAYERS,
                0.7F, 1.65F);
        player.sendSystemMessage(Component.translatable("spell.echospellcore.static_burst.cast", disrupted));
        return true;
    }

    private static boolean castProjectile(ServerPlayer player, CastProfile profile, SpellProjectileKind kind) {
        ServerLevel level = (ServerLevel) player.level();
        SpellProjectileEntity projectile = SpellProjectileEntity.create(level, player, kind, profile.range(),
                kind.velocity(), Math.max(1.0F, (float) (kind.baseDamage() * profile.damageScale())));
        level.addFreshEntity(projectile);
        EchoNetSend.toPlayers(level.players(), SpellProjectileSyncPacket.from(projectile, profile.spell().id()));
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_CLUSTER_HIT, SoundSource.PLAYERS,
                0.65F, kind.pitch());
        player.sendSystemMessage(Component.translatable("spell.echospellcore.projectile.cast",
                profile.spell().title()));
        recordMission(player, EchoSpellCore.id("spell_projectile_fired"), "projectile");
        return true;
    }

    private static boolean castAetherShield(ServerPlayer player, CastProfile profile) {
        ServerLevel level = (ServerLevel) player.level();
        int amplifier = profile.modifiers().contains(SpellModifier.OVERCHARGE) ? 1 : 0;
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 160, amplifier, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 0, false, true));
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0D, player.getZ(),
                22, 0.75D, 0.75D, 0.75D, 0.02D);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS,
                0.4F, 1.75F);
        player.sendSystemMessage(Component.translatable("spell.echospellcore.aether_shield.cast"));
        return true;
    }

    private static boolean castArcaneLift(ServerPlayer player, CastProfile profile) {
        ServerLevel level = (ServerLevel) player.level();
        LivingEntity target = findLookTarget(player, profile.range(), 0.68D);
        if (target == null) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0, false, true));
            player.push(0.0D, 0.38D, 0.0D);
            player.sendSystemMessage(Component.translatable("spell.echospellcore.arcane_lift.self"));
        } else {
            target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 45, 0, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0, false, true));
            level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + 0.2D, target.getZ(),
                    14, 0.25D, 0.3D, 0.25D, 0.02D);
            player.sendSystemMessage(Component.translatable("spell.echospellcore.arcane_lift.target",
                    target.getDisplayName()));
        }
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS,
                0.55F, 1.45F);
        return true;
    }

    private static boolean castAshVeil(ServerPlayer player, CastProfile profile) {
        ServerLevel level = (ServerLevel) player.level();
        int duration = profile.modifiers().contains(SpellModifier.RANGE) ? 130 : 100;
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 80, 0, false, true));
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX(), player.getY() + 0.4D, player.getZ(),
                20, 0.7D, 0.35D, 0.7D, 0.01D);
        level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS,
                0.5F, 0.85F);
        player.sendSystemMessage(Component.translatable("spell.echospellcore.ash_veil.cast"));
        return true;
    }

    private static boolean castCinderSkin(ServerPlayer player, CastProfile profile) {
        ServerLevel level = (ServerLevel) player.level();
        int amplifier = profile.modifiers().contains(SpellModifier.OVERCHARGE) ? 1 : 0;
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 220, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 120, amplifier, false, true));
        level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 0.8D, player.getZ(),
                18, 0.6D, 0.45D, 0.6D, 0.02D);
        level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS,
                0.45F, 0.95F);
        player.sendSystemMessage(Component.translatable("spell.echospellcore.cinder_skin.cast"));
        return true;
    }

    private static boolean castVoidStep(ServerPlayer player, CastProfile profile) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 from = player.position();
        Vec3 target = findBlinkTarget(player, profile.range());
        if (target.distanceToSqr(from) < 1.0D) {
            player.sendSystemMessage(Component.translatable("spell.echospellcore.void_step.blocked"));
            return true;
        }
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 0.7D, player.getZ(),
                18, 0.35D, 0.45D, 0.35D, 0.04D);
        player.teleportTo(level, target.x, target.y, target.z, Set.of(), player.getYRot(), player.getXRot(), false);
        level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 0.7D, player.getZ(),
                24, 0.35D, 0.45D, 0.35D, 0.05D);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS,
                0.65F, 0.8F);
        player.sendSystemMessage(Component.translatable("spell.echospellcore.void_step.cast"));
        return true;
    }

    private static boolean castHollowCage(ServerPlayer player, CastProfile profile) {
        ServerLevel level = (ServerLevel) player.level();
        LivingEntity target = findLookTarget(player, profile.range(), 0.66D);
        Vec3 center = target == null ? player.position().add(player.getLookAngle().normalize().scale(3.0D))
                : target.position();
        int caught = 0;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(2.4D),
                entity -> entity != player && entity.isAlive() && !entity.isAlliedTo(player))) {
            entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 2, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 100, 0, false, true));
            caught++;
        }
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 0.7D, center.z,
                28, 0.75D, 0.55D, 0.75D, 0.08D);
        level.playSound(null, BlockPos.containing(center), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS,
                0.45F, 0.7F);
        player.sendSystemMessage(Component.translatable("spell.echospellcore.hollow_cage.cast", caught));
        return true;
    }

    private static boolean castStaticDash(ServerPlayer player, CastProfile profile) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 look = player.getLookAngle().normalize();
        double strength = profile.modifiers().contains(SpellModifier.OVERCHARGE) ? 1.15D : 0.82D;
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 70, 1, false, true));
        player.push(look.x * strength, 0.10D + Math.max(0.0D, look.y) * 0.25D, look.z * strength);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 0.55D, player.getZ(),
                24, 0.45D, 0.25D, 0.45D, 0.06D);
        level.playSound(null, player.blockPosition(), SoundEvents.COPPER_BULB_TURN_ON, SoundSource.PLAYERS,
                0.65F, 1.85F);
        player.sendSystemMessage(Component.translatable("spell.echospellcore.static_dash.cast"));
        return true;
    }

    private static boolean castThunderCage(ServerPlayer player, CastProfile profile) {
        ServerLevel level = (ServerLevel) player.level();
        LivingEntity target = findLookTarget(player, profile.range(), 0.62D);
        Vec3 center = target == null ? player.position() : target.position();
        int stunned = 0;
        double radius = profile.modifiers().contains(SpellModifier.RANGE) ? 4.25D : 3.25D;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                entity -> entity != player && entity.isAlive() && !entity.isAlliedTo(player))) {
            entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 2, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, false, true));
            entity.hurtServer(level, player.damageSources().magic(), (float) (1.5D * profile.damageScale()));
            stunned++;
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + 0.8D, center.z,
                34, radius * 0.28D, 0.55D, radius * 0.28D, 0.08D);
        level.playSound(null, BlockPos.containing(center), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS,
                0.35F, 1.8F);
        player.sendSystemMessage(Component.translatable("spell.echospellcore.thunder_cage.cast", stunned));
        return true;
    }

    private static boolean castCrystalWall(ServerPlayer player, CastProfile profile) {
        ServerLevel level = (ServerLevel) player.level();
        int amplifier = profile.modifiers().contains(SpellModifier.OVERCHARGE) ? 1 : 0;
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 180, amplifier, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 120, 0, false, true));
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0D, player.getZ(),
                18, 0.85D, 0.75D, 0.85D, 0.02D);
        level.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 0.55D, player.getZ(),
                16, 0.75D, 0.25D, 0.75D, 0.015D);
        level.playSound(null, player.blockPosition(), SoundEvents.GLASS_PLACE, SoundSource.PLAYERS,
                0.45F, 1.25F);
        player.sendSystemMessage(Component.translatable("spell.echospellcore.crystal_wall.cast"));
        return true;
    }

    private static boolean castShardBurst(ServerPlayer player, CastProfile profile) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        int hit = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().expandTowards(look.scale(profile.range())).inflate(3.0D),
                entity -> entity != player && entity.isAlive() && !entity.isAlliedTo(player))) {
            Vec3 toTarget = target.getEyePosition().subtract(eye);
            if (toTarget.length() > profile.range() || toTarget.normalize().dot(look) < 0.55D) {
                continue;
            }
            target.hurtServer(level, player.damageSources().magic(), (float) (3.0D * profile.damageScale()));
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, true));
            hit++;
        }
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0D, player.getZ(),
                26, 1.0D, 0.45D, 1.0D, 0.05D);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS,
                0.65F, 1.55F);
        player.sendSystemMessage(Component.translatable("spell.echospellcore.shard_burst.cast", hit));
        return true;
    }

    private static boolean castResonantArmor(ServerPlayer player, CastProfile profile) {
        ServerLevel level = (ServerLevel) player.level();
        int amplifier = profile.modifiers().contains(SpellModifier.OVERCHARGE) ? 1 : 0;
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, amplifier, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, 0, false, true));
        level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0D, player.getZ(),
                20, 0.75D, 0.75D, 0.75D, 0.08D);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS,
                0.55F, 0.95F);
        player.sendSystemMessage(Component.translatable("spell.echospellcore.resonant_armor.cast"));
        return true;
    }

    private static boolean castBloodSurge(ServerPlayer player, CastProfile profile) {
        ServerLevel level = (ServerLevel) player.level();
        int amplifier = profile.modifiers().contains(SpellModifier.OVERCHARGE) ? 1 : 0;
        if (player.getHealth() > 4.0F) {
            player.hurtServer(level, player.damageSources().magic(), 2.0F);
        }
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 120, amplifier, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 80, 0, false, true));
        ArcanaCoreServices.aether().addAether(player, 2.0D + amplifier, AetherSignalType.CURSED_AETHER);
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, player.getX(), player.getY() + 1.0D, player.getZ(),
                12, 0.35D, 0.4D, 0.35D, 0.01D);
        level.playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS,
                0.35F, 1.15F);
        player.sendSystemMessage(Component.translatable("spell.echospellcore.blood_surge.cast"));
        return true;
    }

    private static boolean castRiftBlink(ServerPlayer player, CastProfile profile) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 before = player.position();
        boolean cast = castVoidStep(player, profile);
        if (cast && player.position().distanceToSqr(before) > 1.0D) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, before.x, before.y + 0.7D, before.z,
                    10, 0.25D, 0.35D, 0.25D, 0.04D);
            player.sendSystemMessage(Component.translatable("spell.echospellcore.rift_blink.cast"));
        }
        return cast;
    }

    private static boolean castSoulThread(ServerPlayer player, CastProfile profile) {
        ServerLevel level = (ServerLevel) player.level();
        LivingEntity target = findLookTarget(player, profile.range(), 0.7D);
        LivingEntity recipient = target == null || target.isAlliedTo(player) ? player : target;
        if (recipient == player || recipient.isAlliedTo(player)) {
            recipient.heal((float) (2.0D * profile.damageScale()));
            recipient.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, false, true));
        } else {
            recipient.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0, false, true));
            recipient.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, false, true));
        }
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, recipient.getX(),
                recipient.getY() + recipient.getBbHeight() * 0.55D, recipient.getZ(),
                14, 0.22D, 0.35D, 0.22D, 0.015D);
        level.playSound(null, recipient.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS,
                0.4F, 1.45F);
        player.sendSystemMessage(Component.translatable("spell.echospellcore.soul_thread.cast",
                recipient.getDisplayName()));
        return true;
    }

    private static boolean castDecayTouch(ServerPlayer player, CastProfile profile) {
        ServerLevel level = (ServerLevel) player.level();
        LivingEntity target = findLookTarget(player, profile.range(), 0.72D);
        if (target == null) {
            player.sendSystemMessage(Component.translatable("spell.echospellcore.decay_touch.miss"));
            return true;
        }
        target.hurtServer(level, player.damageSources().magic(), (float) (2.5D * profile.damageScale()));
        target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, false, true));
        level.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + target.getBbHeight() * 0.5D,
                target.getZ(), 18, 0.25D, 0.35D, 0.25D, 0.025D);
        level.playSound(null, target.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS,
                0.25F, 1.55F);
        player.sendSystemMessage(Component.translatable("spell.echospellcore.decay_touch.cast",
                target.getDisplayName()));
        return true;
    }

    private static boolean castVeilTrace(ServerPlayer player, CastProfile profile) {
        ServerLevel level = (ServerLevel) player.level();
        LivingEntity target = findLookTarget(player, profile.range(), 0.72D);
        if (target == null) {
            Vec3 eye = player.getEyePosition();
            Vec3 end = eye.add(player.getLookAngle().normalize().scale(profile.range()));
            HitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, player));
            Vec3 mark = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 120, 0, false, true));
            level.sendParticles(ParticleTypes.ENCHANT, mark.x, mark.y, mark.z,
                    12, 0.18D, 0.18D, 0.18D, 0.04D);
            level.playSound(null, BlockPos.containing(mark), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                    0.4F, 1.9F);
            player.sendSystemMessage(Component.translatable("spell.echospellcore.veil_trace.scan"));
            return true;
        }
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 180, 0, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 70, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 100, 0, false, true));
        level.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY() + target.getBbHeight() * 0.6D,
                target.getZ(), 18, 0.22D, 0.35D, 0.22D, 0.035D);
        level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                0.48F, 1.85F);
        player.sendSystemMessage(Component.translatable("spell.echospellcore.veil_trace.cast",
                target.getDisplayName()));
        return true;
    }

    private static Vec3 findBlinkTarget(ServerPlayer player, double range) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 eye = player.getEyePosition();
        Vec3 desired = eye.add(look.scale(range));
        HitResult hit = level.clip(new ClipContext(eye, desired, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        double maxDistance = hit.getType() == HitResult.Type.MISS
                ? range
                : Math.max(1.0D, hit.getLocation().distanceTo(eye) - 1.0D);
        for (double distance = maxDistance; distance >= 1.0D; distance -= 0.75D) {
            Vec3 candidate = player.position().add(look.scale(distance));
            BlockPos pos = BlockPos.containing(candidate);
            AABB movedBox = player.getBoundingBox().move(candidate.subtract(player.position()));
            if (level.isLoaded(pos) && level.noCollision(movedBox)) {
                return candidate;
            }
        }
        return player.position();
    }

    private static LivingEntity findLookTarget(ServerPlayer player, double range, double minDot) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        AABB search = player.getBoundingBox().expandTowards(look.scale(range)).inflate(2.0D);
        ServerLevel level = (ServerLevel) player.level();
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, search,
                entity -> entity != player && entity.isAlive() && player.hasLineOfSight(entity));
        return candidates.stream()
                .filter(entity -> {
                    Vec3 delta = entity.getEyePosition().subtract(eye);
                    return delta.length() <= range && delta.normalize().dot(look) >= minDot;
                })
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))
                .orElse(null);
    }

    private static List<SpellModifier> deckModifiers(ItemStack deck, int slot) {
        initializeDeck(deck);
        CompoundTag tag = customData(deck);
        migrateLegacyModifiers(tag, slot);
        List<SpellModifier> modifiers = new ArrayList<>();
        for (int socket = 0; socket < MODIFIER_SOCKETS; socket++) {
            String value = tag.getStringOr(socketKey(slot, socket), "");
            SpellModifier.byId(value).ifPresent(modifier -> {
                if (!modifiers.contains(modifier)) {
                    modifiers.add(modifier);
                }
            });
        }
        if (modifiers.isEmpty()) {
            return List.of();
        }
        return modifiers;
    }

    private static double baseCurseRisk(StarterSpell spell) {
        return switch (spell.school()) {
            case ASH -> 0.02D;
            case VOID, RIFT, BLOOD, DECAY, FRACTURE -> 0.05D;
            case VEIL -> 0.025D;
            case STORM -> 0.015D;
            case CRYSTAL -> 0.01D;
            default -> 0.0D;
        };
    }

    private static boolean toggleModifier(CompoundTag tag, int slot, String modifierId) {
        SpellModifier modifier = SpellModifier.byId(modifierId).orElse(null);
        if (modifier == null) {
            return false;
        }
        migrateLegacyModifiers(tag, slot);
        boolean removed = false;
        for (int socket = 0; socket < MODIFIER_SOCKETS; socket++) {
            String key = socketKey(slot, socket);
            if (modifier.id().equals(tag.getStringOr(key, ""))) {
                tag.remove(key);
                removed = true;
            }
        }
        if (removed) {
            return true;
        }
        int emptyStart = firstEmptySocket(tag, slot, modifier.socketCost());
        if (emptyStart < 0) {
            return false;
        }
        for (int socket = 0; socket < modifier.socketCost(); socket++) {
            tag.putString(socketKey(slot, emptyStart + socket), modifier.id());
        }
        return true;
    }

    private static void migrateLegacyModifiers(CompoundTag tag, int slot) {
        boolean hasSocketData = false;
        for (int socket = 0; socket < MODIFIER_SOCKETS; socket++) {
            if (!tag.getStringOr(socketKey(slot, socket), "").isBlank()) {
                hasSocketData = true;
                break;
            }
        }
        String legacy = tag.getStringOr(modifierKey(slot), "");
        if (hasSocketData || legacy.isBlank()) {
            return;
        }
        int socket = 0;
        for (String id : legacy.split(",")) {
            SpellModifier modifier = SpellModifier.byId(id.trim()).orElse(null);
            if (modifier == null || socket + modifier.socketCost() > MODIFIER_SOCKETS) {
                continue;
            }
            for (int cost = 0; cost < modifier.socketCost(); cost++) {
                tag.putString(socketKey(slot, socket++), modifier.id());
            }
        }
        tag.remove(modifierKey(slot));
    }

    private static int firstEmptySocket(CompoundTag tag, int slot, int needed) {
        int safeNeeded = Math.max(1, Math.min(MODIFIER_SOCKETS, needed));
        for (int socket = 0; socket <= MODIFIER_SOCKETS - safeNeeded; socket++) {
            boolean free = true;
            for (int offset = 0; offset < safeNeeded; offset++) {
                if (!tag.getStringOr(socketKey(slot, socket + offset), "").isBlank()) {
                    free = false;
                    break;
                }
            }
            if (free) {
                return socket;
            }
        }
        return -1;
    }

    private static String slotKey(int slot) {
        return DECK_SLOT_PREFIX + Math.floorMod(slot, LOADOUT_SLOTS);
    }

    private static String coreInstalledKey(int slot) {
        return DECK_CORE_INSTALLED_PREFIX + Math.floorMod(slot, LOADOUT_SLOTS);
    }

    private static String coreOverchargedKey(int slot) {
        return DECK_CORE_OVERCHARGED_PREFIX + Math.floorMod(slot, LOADOUT_SLOTS);
    }

    private static String modifierKey(int slot) {
        return DECK_MODIFIER_PREFIX + Math.floorMod(slot, LOADOUT_SLOTS);
    }

    private static String socketKey(int slot, int socket) {
        return DECK_SOCKET_PREFIX + Math.floorMod(slot, LOADOUT_SLOTS) + "_" + Math.floorMod(socket, MODIFIER_SOCKETS);
    }

    private static void setCooldown(Player player, StarterSpell spell, int cooldownTicks) {
        playerRoot(player).putLong("cooldown_" + spell.path(), player.level().getGameTime() + cooldownTicks);
    }

    private static void maybeApplyOverchargeBacklash(ServerPlayer player, CastProfile profile) {
        if (!profile.modifiers().contains(SpellModifier.OVERCHARGE) && profile.curseRisk() <= 0.0D) {
            return;
        }
        if (player.getRandom().nextDouble() >= Math.min(0.35D, profile.curseRisk())) {
            return;
        }
        tryApplyBacklashCurse(player, profile.spell());
        ArcanaCoreServices.aether().addContamination(player, 0.35D);
        player.sendSystemMessage(Component.translatable("spell.echospellcore.overcharge.backlash"));
    }

    private static void setFocusStatus(ItemStack focus, StarterSpell spell, String status, Player player) {
        CompoundTag tag = customData(focus);
        tag.putString(STACK_SELECTED, spell.id().toString());
        tag.putString(STACK_STATUS, status == null ? "" : status);
        tag.putFloat(STACK_AETHER, (float) ArcanaCoreServices.aether().getAether(player, spell.aetherType()));
        tag.putFloat(STACK_MAX_AETHER, (float) ArcanaCoreServices.aether().getMaxAether(player, spell.aetherType()));
        tag.putLong(STACK_COOLDOWN, cooldownRemaining(player, spell));
        ItemStack deck = findDeck(player);
        int activeSlot = deck.isEmpty() ? -1 : deckActiveSlot(deck);
        tag.putString(STACK_MODIFIERS, deck.isEmpty() ? "" : deckModifierSummary(deck, activeSlot));
        tag.putFloat(STACK_CONTAMINATION, (float) ArcanaCoreServices.aether().getContamination(player));
        tag.putFloat(STACK_CURSE_RISK, (float) castProfile(player, spell.id()).curseRisk());
        focus.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static String deckCoreState(ItemStack deck) {
        if (deck.isEmpty()) {
            return "none";
        }
        int activeSlot = deckActiveSlot(deck);
        CompoundTag tag = customData(deck);
        if (tag.getBooleanOr(coreOverchargedKey(activeSlot), false)) {
            return "overcharged";
        }
        return tag.getBooleanOr(coreInstalledKey(activeSlot), false) ? "engraved" : "soft";
    }

    private static CompoundTag playerRoot(Player player) {
        CompoundTag root = player.getPersistentData().getCompoundOrEmpty(PLAYER_ROOT);
        player.getPersistentData().put(PLAYER_ROOT, root);
        return root;
    }

    private static CompoundTag customData(ItemStack stack) {
        CustomData data = stack == null ? null : stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    private static void recordMission(ServerPlayer player, Identifier target, String action) {
        EchoCoreServices.recordMissionObjective(player, MissionObjectiveType.CUSTOM, target, 1,
                Map.of("source", EchoSpellCore.MODID, "action", action));
    }

    private static void tryApplyBacklashCurse(ServerPlayer player, StarterSpell spell) {
        if (!EchoRuntimeModules.isLoaded("echocursecore")) {
            return;
        }
        try {
            Class.forName("com.knoxhack.echocursecore.api.CurseCoreApi")
                    .getMethod("applyEchoRot", ServerPlayer.class, int.class, String.class)
                    .invoke(null, player, 1, "spell_backlash");
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException
                | LinkageError exception) {
            EchoSpellCore.LOGGER.debug("CurseCore backlash bridge skipped.", exception);
        }
    }

    private record CastProfile(StarterSpell spell, List<SpellModifier> modifiers, int deckSlot, int cost,
            int cooldownTicks, double range, double damageScale, double curseRisk) {
    }
}
