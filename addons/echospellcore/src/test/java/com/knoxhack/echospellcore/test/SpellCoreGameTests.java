package com.knoxhack.echospellcore.test;

import com.knoxhack.echoarcanacore.api.AetherSignalType;
import com.knoxhack.echoarcanacore.api.ArcanaCoreServices;
import com.knoxhack.echospellcore.EchoSpellCore;
import com.knoxhack.echospellcore.api.SpellCoreApi;
import com.knoxhack.echospellcore.entity.SpellProjectileEntity;
import com.knoxhack.echospellcore.integration.arcana.SpellCoreArcanaIntegration;
import com.knoxhack.echospellcore.menu.SpellDeckMenu;
import com.knoxhack.echospellcore.registry.ModItems;
import com.knoxhack.echospellcore.spell.SpellModifier;
import com.knoxhack.echospellcore.spell.StarterSpell;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SpellCoreGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, EchoSpellCore.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SPELL_PROVIDER_REGISTERS =
            TEST_FUNCTIONS.register("spell_provider_registers", () -> SpellCoreGameTests::spellProviderRegisters);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SIGNAL_FOCUS_CYCLES_SPELL =
            TEST_FUNCTIONS.register("signal_focus_cycles_spell", () -> SpellCoreGameTests::signalFocusCyclesSpell);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STARTER_SPELL_CASTS =
            TEST_FUNCTIONS.register("starter_spell_cast_consumes_aether_and_sets_cooldown", () -> SpellCoreGameTests::starterSpellCasts);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ASH_VEIL_APPLIES_EFFECT =
            TEST_FUNCTIONS.register("ash_veil_applies_effect", () -> SpellCoreGameTests::ashVeilAppliesEffect);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SPELL_DECK_LOADOUT =
            TEST_FUNCTIONS.register("spell_deck_loadout_configures_slot_and_modifier", () -> SpellCoreGameTests::spellDeckLoadout);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PROJECTILE_SPAWNS =
            TEST_FUNCTIONS.register("aether_bolt_spawns_spell_projectile", () -> SpellCoreGameTests::aetherBoltSpawnsProjectile);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MODIFIER_SOCKETS =
            TEST_FUNCTIONS.register("spell_deck_enforces_modifier_sockets", () -> SpellCoreGameTests::spellDeckModifierSockets);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ITEM_SOCKET_MENU =
            TEST_FUNCTIONS.register("spell_deck_item_socket_menu_persists", () -> SpellCoreGameTests::spellDeckItemSocketMenuPersists);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> EXPANDED_SCHOOLS =
            TEST_FUNCTIONS.register("expanded_school_spells_cast", () -> SpellCoreGameTests::expandedSchoolSpellsCast);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FORBIDDEN_SCHOOLS =
            TEST_FUNCTIONS.register("forbidden_school_spells_cast", () -> SpellCoreGameTests::forbiddenSchoolSpellsCast);

    private SpellCoreGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("spellcore_first_slice"));
        register(event, environment, "spell_provider_registers", SPELL_PROVIDER_REGISTERS.getId());
        register(event, environment, "signal_focus_cycles_spell", SIGNAL_FOCUS_CYCLES_SPELL.getId());
        register(event, environment, "starter_spell_cast_consumes_aether_and_sets_cooldown", STARTER_SPELL_CASTS.getId());
        register(event, environment, "ash_veil_applies_effect", ASH_VEIL_APPLIES_EFFECT.getId());
        register(event, environment, "spell_deck_loadout_configures_slot_and_modifier", SPELL_DECK_LOADOUT.getId());
        register(event, environment, "aether_bolt_spawns_spell_projectile", PROJECTILE_SPAWNS.getId());
        register(event, environment, "spell_deck_enforces_modifier_sockets", MODIFIER_SOCKETS.getId());
        register(event, environment, "spell_deck_item_socket_menu_persists", ITEM_SOCKET_MENU.getId());
        register(event, environment, "expanded_school_spells_cast", EXPANDED_SCHOOLS.getId());
        register(event, environment, "forbidden_school_spells_cast", FORBIDDEN_SCHOOLS.getId());
    }

    private static void spellProviderRegisters(GameTestHelper helper) {
        SpellCoreArcanaIntegration.register();
        helper.assertTrue(ArcanaCoreServices.spells().stream().anyMatch(spell -> SpellCoreApi.SIGNAL_PULSE.equals(spell.id())),
                "Signal Pulse should register into Arcana Core");
        helper.assertTrue(ArcanaCoreServices.spells().stream().anyMatch(spell -> SpellCoreApi.ASH_VEIL.equals(spell.id())),
                "Ash Veil should register into Arcana Core");
        helper.succeed();
    }

    private static void signalFocusCyclesSpell(GameTestHelper helper) {
        ItemStack focus = new ItemStack(ModItems.SIGNAL_FOCUS.get());
        helper.assertTrue(SpellCoreApi.SIGNAL_PULSE.equals(SpellCoreApi.selectedSpell(focus)),
                "Signal Focus should default to Signal Pulse");
        helper.assertTrue(SpellCoreApi.ECHO_MARK.equals(SpellCoreApi.cycleSpell(focus)),
                "Signal Focus should cycle to Echo Mark");
        helper.assertTrue(SpellCoreApi.STATIC_BURST.equals(SpellCoreApi.cycleSpell(focus)),
                "Signal Focus should cycle to Static Burst");
        helper.succeed();
    }

    private static void starterSpellCasts(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        giveAwakenedCoreIfPresent(player);
        ItemStack focus = new ItemStack(ModItems.SIGNAL_FOCUS.get());
        ArcanaCoreServices.aether().addAether(player, 40.0D, AetherSignalType.SIGNAL_AETHER);
        helper.assertTrue(SpellCoreApi.tryCast(player, focus, SpellCoreApi.SIGNAL_PULSE),
                "Signal Pulse should cast when aether and awakened core are available");
        helper.assertTrue(ArcanaCoreServices.aether().getAether(player, AetherSignalType.SIGNAL_AETHER) < 40.0D,
                "Signal Pulse should consume signal aether");
        helper.assertTrue(SpellCoreApi.cooldownRemaining(player, StarterSpell.SIGNAL_PULSE) > 0,
                "Signal Pulse should set cooldown");
        helper.succeed();
    }

    private static void ashVeilAppliesEffect(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        giveAwakenedCoreIfPresent(player);
        ItemStack focus = new ItemStack(ModItems.SIGNAL_FOCUS.get());
        ArcanaCoreServices.aether().addAether(player, 40.0D, AetherSignalType.RAW_AETHER);
        helper.assertTrue(SpellCoreApi.tryCast(player, focus, SpellCoreApi.ASH_VEIL),
                "Ash Veil should cast with raw aether");
        helper.assertTrue(player.hasEffect(MobEffects.INVISIBILITY),
                "Ash Veil should apply invisibility");
        helper.succeed();
    }

    private static void spellDeckLoadout(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack deck = new ItemStack(ModItems.SPELL_DECK.get());
        ItemStack focus = new ItemStack(ModItems.SIGNAL_FOCUS.get());
        player.getInventory().add(deck);
        player.getInventory().add(focus);
        deck = SpellCoreApi.findDeck(player);
        SpellCoreApi.initializeDeck(deck);
        SpellCoreApi.applyLoadoutAction(player, "set_spell", 2, SpellCoreApi.DUST_LANCE, "");
        SpellCoreApi.applyLoadoutAction(player, "toggle_modifier", 2, SpellCoreApi.DUST_LANCE, SpellModifier.RANGE.id());
        SpellCoreApi.applyLoadoutAction(player, "select_slot", 2, SpellCoreApi.DUST_LANCE, "");
        helper.assertTrue(SpellCoreApi.deckActiveSlot(deck) == 2, "Spell Deck should move the active slot");
        helper.assertTrue(SpellCoreApi.deckSlotSpell(deck, 2) == StarterSpell.DUST_LANCE,
                "Spell Deck slot should store Dust Lance");
        helper.assertTrue(SpellCoreApi.deckHasModifier(deck, 2, SpellModifier.RANGE),
                "Spell Deck slot should store Range modifier");
        helper.assertTrue(SpellCoreApi.DUST_LANCE.equals(SpellCoreApi.activeSpellId(player, focus)),
                "Signal Focus should cast the active deck spell");
        helper.succeed();
    }

    private static void aetherBoltSpawnsProjectile(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        prepareCasterLane(helper, player);
        giveAwakenedCoreIfPresent(player);
        ItemStack focus = new ItemStack(ModItems.SIGNAL_FOCUS.get());
        SpellCoreApi.selectSpell(focus, SpellCoreApi.AETHER_BOLT);
        ArcanaCoreServices.aether().addAether(player, 40.0D, AetherSignalType.RAW_AETHER);
        helper.assertTrue(SpellCoreApi.tryCast(player, focus, SpellCoreApi.AETHER_BOLT),
                "Aether Bolt should cast with raw aether");
        helper.runAfterDelay(1L, () -> {
            int projectiles = countNearbyProjectiles(player);
            helper.assertTrue(projectiles > 0, "Aether Bolt should spawn a synchronized spell projectile");
            helper.succeed();
        });
    }

    private static void spellDeckModifierSockets(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack deck = new ItemStack(ModItems.SPELL_DECK.get());
        player.getInventory().add(deck);
        deck = SpellCoreApi.findDeck(player);
        SpellCoreApi.initializeDeck(deck);
        SpellCoreApi.applyLoadoutAction(player, "toggle_modifier", 0, SpellCoreApi.SIGNAL_PULSE,
                SpellModifier.OVERCHARGE.id());
        SpellCoreApi.applyLoadoutAction(player, "toggle_modifier", 0, SpellCoreApi.SIGNAL_PULSE,
                SpellModifier.RANGE.id());
        SpellCoreApi.applyLoadoutAction(player, "toggle_modifier", 0, SpellCoreApi.SIGNAL_PULSE,
                SpellModifier.EFFICIENCY.id());
        helper.assertTrue(SpellCoreApi.deckHasModifier(deck, 0, SpellModifier.OVERCHARGE),
                "Overcharge should install into two modifier sockets");
        helper.assertTrue(SpellCoreApi.deckHasModifier(deck, 0, SpellModifier.RANGE),
                "Range should fill the remaining modifier socket");
        helper.assertFalse(SpellCoreApi.deckHasModifier(deck, 0, SpellModifier.EFFICIENCY),
                "Efficiency should not install when all sockets are occupied");
        helper.assertTrue(SpellCoreApi.deckUsedSockets(deck, 0) == SpellCoreApi.MODIFIER_SOCKETS,
                "Spell Deck should report full socket capacity");
        helper.succeed();
    }

    private static void spellDeckItemSocketMenuPersists(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack deck = new ItemStack(ModItems.SPELL_DECK.get());
        player.getInventory().add(deck);
        deck = SpellCoreApi.findDeck(player);
        SpellCoreApi.initializeDeck(deck);
        SpellDeckMenu menu = new SpellDeckMenu(42, player.getInventory());
        ItemStack overcharge = new ItemStack(ModItems.OVERCHARGE_MODIFIER_SOCKET.get());
        ItemStack range = new ItemStack(ModItems.RANGE_MODIFIER_SOCKET.get());
        ItemStack efficiency = new ItemStack(ModItems.EFFICIENCY_MODIFIER_SOCKET.get());
        helper.assertTrue(menu.getSlot(0).mayPlace(overcharge),
                "Empty Spell Deck should accept an Overcharge socket chip");
        menu.getSlot(0).setByPlayer(overcharge);
        helper.assertTrue(menu.getSlot(1).mayPlace(range),
                "Range chip should fit after Overcharge consumes two logical sockets");
        menu.getSlot(1).setByPlayer(range);
        helper.assertFalse(menu.getSlot(2).mayPlace(efficiency),
                "Efficiency chip should not fit after all logical sockets are occupied");
        helper.assertTrue(SpellCoreApi.deckHasModifier(deck, 0, SpellModifier.OVERCHARGE),
                "Item-backed menu should persist Overcharge to deck data");
        helper.assertTrue(SpellCoreApi.deckHasModifier(deck, 0, SpellModifier.RANGE),
                "Item-backed menu should persist Range to deck data");
        helper.assertTrue(SpellCoreApi.deckUsedSockets(deck, 0) == SpellCoreApi.MODIFIER_SOCKETS,
                "Item-backed menu should preserve logical socket accounting");
        SpellDeckMenu reopened = new SpellDeckMenu(43, player.getInventory());
        helper.assertTrue(reopened.socketStack(0).is(ModItems.OVERCHARGE_MODIFIER_SOCKET.get()),
                "Reopened Spell Deck should display the persisted Overcharge chip");
        helper.assertTrue(reopened.socketStack(1).is(ModItems.RANGE_MODIFIER_SOCKET.get()),
                "Reopened Spell Deck should display the persisted Range chip");
        helper.succeed();
    }

    private static void expandedSchoolSpellsCast(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        prepareCasterLane(helper, player);
        giveAwakenedCoreIfPresent(player);
        ItemStack focus = new ItemStack(ModItems.SIGNAL_FOCUS.get());
        ArcanaCoreServices.aether().addAether(player, 40.0D, AetherSignalType.REFINED_AETHER);
        SpellCoreApi.selectSpell(focus, SpellCoreApi.CRYSTAL_WALL);
        helper.assertTrue(SpellCoreApi.tryCast(player, focus, SpellCoreApi.CRYSTAL_WALL),
                "Crystal Wall should cast with refined aether");
        helper.assertTrue(player.hasEffect(MobEffects.ABSORPTION),
                "Crystal Wall should apply absorption");
        player.getPersistentData().remove("echospellcore_player");
        ArcanaCoreServices.aether().addAether(player, 40.0D, AetherSignalType.SIGNAL_AETHER);
        SpellCoreApi.selectSpell(focus, SpellCoreApi.STORM_LANCE);
        helper.assertTrue(SpellCoreApi.tryCast(player, focus, SpellCoreApi.STORM_LANCE),
                "Storm Lance should cast with signal aether");
        helper.runAfterDelay(1L, () -> {
            int projectiles = countNearbyProjectiles(player);
            helper.assertTrue(projectiles > 0, "Storm Lance should spawn a synchronized spell projectile");
            helper.succeed();
        });
    }

    private static void forbiddenSchoolSpellsCast(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        prepareCasterLane(helper, player);
        giveAwakenedCoreIfPresent(player);
        ItemStack focus = new ItemStack(ModItems.SIGNAL_FOCUS.get());
        ArcanaCoreServices.aether().addAether(player, 40.0D, AetherSignalType.CURSED_AETHER);
        SpellCoreApi.selectSpell(focus, SpellCoreApi.BLOOD_SURGE);
        helper.assertTrue(SpellCoreApi.tryCast(player, focus, SpellCoreApi.BLOOD_SURGE),
                "Blood Surge should cast with cursed aether");
        helper.assertTrue(player.hasEffect(MobEffects.STRENGTH),
                "Blood Surge should apply strength");
        ArcanaCoreServices.aether().addAether(player, 40.0D, AetherSignalType.SOUL_AETHER);
        SpellCoreApi.selectSpell(focus, SpellCoreApi.SOUL_THREAD);
        helper.assertTrue(SpellCoreApi.tryCast(player, focus, SpellCoreApi.SOUL_THREAD),
                "Soul Thread should cast with soul aether");
        helper.assertTrue(player.hasEffect(MobEffects.REGENERATION),
                "Soul Thread should apply regeneration to self without a target");
        ArcanaCoreServices.aether().addAether(player, 40.0D, AetherSignalType.CURSED_AETHER);
        SpellCoreApi.selectSpell(focus, SpellCoreApi.DECAY_TOUCH);
        helper.assertTrue(SpellCoreApi.tryCast(player, focus, SpellCoreApi.DECAY_TOUCH),
                "Decay Touch should safely cast even when it misses");
        ArcanaCoreServices.aether().addAether(player, 40.0D, AetherSignalType.RIFT_AETHER);
        SpellCoreApi.selectSpell(focus, SpellCoreApi.RIFT_BLINK);
        helper.assertTrue(SpellCoreApi.tryCast(player, focus, SpellCoreApi.RIFT_BLINK),
                "Rift Blink should cast with rift aether");
        ArcanaCoreServices.aether().addAether(player, 40.0D, AetherSignalType.VEIL_RESONANCE);
        SpellCoreApi.selectSpell(focus, SpellCoreApi.VEIL_TRACE);
        helper.assertTrue(SpellCoreApi.tryCast(player, focus, SpellCoreApi.VEIL_TRACE),
                "Veil Trace should cast with veil resonance");
        helper.assertTrue(player.hasEffect(MobEffects.NIGHT_VISION),
                "Veil Trace should grant diagnostic night vision when no target resolves");
        ArcanaCoreServices.aether().addAether(player, 40.0D, AetherSignalType.FRACTURE_ENERGY);
        SpellCoreApi.selectSpell(focus, SpellCoreApi.FRACTURE_SHEAR);
        helper.assertTrue(SpellCoreApi.tryCast(player, focus, SpellCoreApi.FRACTURE_SHEAR),
                "Fracture Shear should cast with fracture energy");
        helper.runAfterDelay(1L, () -> {
            int projectiles = countNearbyProjectiles(player);
            helper.assertTrue(projectiles > 0, "Fracture Shear should spawn a synchronized spell projectile");
            helper.succeed();
        });
    }

    private static void prepareCasterLane(GameTestHelper helper, ServerPlayer player) {
        BlockPos relative = new BlockPos(2, 2, 2);
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = 0; z <= 7; z++) {
                    helper.setBlock(relative.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
        helper.setBlock(relative.below(), Blocks.STONE.defaultBlockState());
        BlockPos absolute = helper.absolutePos(relative);
        player.setPos(absolute.getX() + 0.5D, absolute.getY(), absolute.getZ() + 0.5D);
        player.setYRot(0.0F);
        player.setXRot(0.0F);
        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
    }

    private static int countNearbyProjectiles(ServerPlayer player) {
        AABB area = new AABB(player.getX() - 16.0D, player.getY() - 4.0D, player.getZ() - 16.0D,
                player.getX() + 16.0D, player.getY() + 6.0D, player.getZ() + 16.0D);
        return player.level().getEntitiesOfClass(SpellProjectileEntity.class, area).size();
    }

    private static void giveAwakenedCoreIfPresent(ServerPlayer player) {
        Identifier awakened = Identifier.fromNamespaceAndPath("echoritualcore", "awakened_spell_core");
        BuiltInRegistries.ITEM.getOptional(awakened).ifPresent(item -> player.getInventory().add(new ItemStack(item)));
    }

    private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment,
            String testName, Identifier functionId) {
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment,
                Identifier.withDefaultNamespace("empty"),
                400,
                0,
                true,
                Rotation.NONE,
                false,
                1,
                1,
                false,
                2);
        event.registerTest(id(testName), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, functionId), data));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoSpellCore.MODID, path);
    }
}
