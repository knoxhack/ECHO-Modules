package com.knoxhack.echoarcanacore.api;

import com.knoxhack.echoarcanacore.EchoArcanaCore;
import com.knoxhack.echoarcanacore.service.PersistentAetherService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.resources.Identifier;

public final class ArcanaCoreServices {
    private static volatile IAetherService aetherService = PersistentAetherService.INSTANCE;
    private static final Map<Identifier, SpellDefinition> SPELLS = new LinkedHashMap<>();
    private static final Map<Identifier, RitualDefinition> RITUALS = new LinkedHashMap<>();
    private static final Map<Identifier, CurseDefinition> CURSES = new LinkedHashMap<>();
    private static final Map<Identifier, ArcaneRelicDefinition> RELICS = new LinkedHashMap<>();
    private static final List<Object> PROVIDERS = new CopyOnWriteArrayList<>();
    private static boolean builtInsRegistered;

    private ArcanaCoreServices() {
    }

    public static void registerAetherService(IAetherService service) {
        if (service != null) {
            aetherService = service;
        }
    }

    public static IAetherService aether() {
        return aetherService;
    }

    public static synchronized void registerBuiltIns() {
        if (builtInsRegistered) {
            return;
        }
        builtInsRegistered = true;
        registerSpell(new SpellDefinition(
                id("spell/signal_pulse"), "spell.echoarcanacore.signal_pulse", SpellSchool.SIGNAL,
                id("textures/gui/icons/spell_signal_pulse.png"), null, 8.0D, 60, 5, 0, 12.0D,
                TargetingMode.CONE, CastType.INSTANT, id("effect/signal_pulse"), 1, 0.01D, 0.0D,
                id("visual/signal"), id("sound/signal_pulse"), id("index/spells/signal_pulse"),
                id("grimoire/spells/signal_pulse"), List.of("starter", "signal", "lens")));
        registerSpell(new SpellDefinition(
                id("spell/aether_bolt"), "spell.echoarcanacore.aether_bolt", SpellSchool.AETHER,
                id("textures/gui/icons/spell_aether_bolt.png"), null, 6.0D, 40, 4, 0, 20.0D,
                TargetingMode.PROJECTILE, CastType.INSTANT, id("effect/aether_bolt"), 1, 0.0D, 0.0D,
                id("visual/aether"), id("sound/aether_bolt"), id("index/spells/aether_bolt"),
                id("grimoire/spells/aether_bolt"), List.of("starter", "aether")));
        registerRitual(new RitualDefinition(
                id("ritual/aether_calibration"), "ritual.echoarcanacore.aether_calibration",
                RitualFamily.STABILIZATION, "basic_altar", id("structure/basic_altar"), id("block/basic_altar"),
                List.of(), List.of(), List.of(), 20.0D, List.of(), List.of("time:any"),
                null, List.of(), List.of(), 1.0D, id("failure/basic_backlash"), id("effect/aether_calibration"),
                0.0D, 0.0D, 0.0D, id("index/rituals/aether_calibration"),
                id("grimoire/rituals/aether_calibration")));
        registerCurse(new CurseDefinition(
                id("curse/echo_rot"), "curse.echoarcanacore.echo_rot", CurseCategory.MIND,
                5, 5, true, id("discovery/signal_symptoms"),
                List.of("Terminal glitches", "False Lens pings", "Signal afterimages"),
                List.of("Minor false positives", "Signal spells unstable", "Hostile signal attention"),
                List.of(id("ritual/signal_purge")), null, id("visual/echo_rot"),
                id("sound/echo_rot"), id("index/curses/echo_rot"), id("grimoire/curses/echo_rot")));
    }

    public static synchronized boolean registerSpell(SpellDefinition spell) {
        return spell != null && spell.id() != null && SPELLS.putIfAbsent(spell.id(), spell) == null;
    }

    public static synchronized boolean registerRitual(RitualDefinition ritual) {
        return ritual != null && ritual.id() != null && RITUALS.putIfAbsent(ritual.id(), ritual) == null;
    }

    public static synchronized boolean registerCurse(CurseDefinition curse) {
        return curse != null && curse.id() != null && CURSES.putIfAbsent(curse.id(), curse) == null;
    }

    public static synchronized boolean registerRelic(ArcaneRelicDefinition relic) {
        return relic != null && relic.id() != null && RELICS.putIfAbsent(relic.id(), relic) == null;
    }

    public static synchronized List<SpellDefinition> spells() {
        return List.copyOf(SPELLS.values());
    }

    public static synchronized List<RitualDefinition> rituals() {
        return List.copyOf(RITUALS.values());
    }

    public static synchronized List<CurseDefinition> curses() {
        return List.copyOf(CURSES.values());
    }

    public static synchronized List<ArcaneRelicDefinition> relics() {
        return List.copyOf(RELICS.values());
    }

    public static synchronized Optional<SpellDefinition> spell(Identifier id) {
        return Optional.ofNullable(SPELLS.get(id));
    }

    public static synchronized Optional<RitualDefinition> ritual(Identifier id) {
        return Optional.ofNullable(RITUALS.get(id));
    }

    public static synchronized Optional<CurseDefinition> curse(Identifier id) {
        return Optional.ofNullable(CURSES.get(id));
    }

    public static synchronized Optional<ArcaneRelicDefinition> relic(Identifier id) {
        return Optional.ofNullable(RELICS.get(id));
    }

    public static void registerProvider(Object provider) {
        if (provider == null || PROVIDERS.contains(provider)) {
            return;
        }
        PROVIDERS.add(provider);
        if (provider instanceof ArcanaProviderInterfaces.SpellProvider spellProvider) {
            spellProvider.spells().forEach(ArcanaCoreServices::registerSpell);
        }
        if (provider instanceof ArcanaProviderInterfaces.RitualProvider ritualProvider) {
            ritualProvider.rituals().forEach(ArcanaCoreServices::registerRitual);
        }
        if (provider instanceof ArcanaProviderInterfaces.CurseProvider curseProvider) {
            curseProvider.curses().forEach(ArcanaCoreServices::registerCurse);
        }
        if (provider instanceof ArcanaProviderInterfaces.RelicProvider relicProvider) {
            relicProvider.relics().forEach(ArcanaCoreServices::registerRelic);
        }
    }

    public static <T> List<T> providers(Class<T> providerType) {
        List<T> result = new ArrayList<>();
        for (Object provider : PROVIDERS) {
            if (providerType.isInstance(provider)) {
                result.add(providerType.cast(provider));
            }
        }
        return List.copyOf(result);
    }

    public static Identifier id(String path) {
        return EchoArcanaCore.id(path);
    }
}
