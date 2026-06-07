package com.knoxhack.echo.npcore.integration.terminal;

import com.knoxhack.echo.npcore.config.EchoNpcCoreConfig;
import com.knoxhack.echo.npcore.conversion.EchoNpcReplacementManager;
import com.knoxhack.echo.npcore.data.NpcContactData;
import com.knoxhack.echo.npcore.data.NpcDataBridge;
import com.knoxhack.echo.npcore.dialogue.EchoNpcDialogueManager;
import com.knoxhack.echo.npcore.faction.EchoNpcFactionManager;
import com.knoxhack.echo.npcore.profile.EchoNpcProfileManager;
import com.knoxhack.echo.npcore.service.EchoNpcServiceManager;
import com.knoxhack.echo.npcore.trade.EchoNpcTradeManager;
import com.knoxhack.echo.npcore.visual.EchoNpcVisualProfileManager;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoRuntimeModules;
import com.knoxhack.echocore.api.config.EchoNativeConfigSpec;
import com.knoxhack.echoterminal.api.TerminalAddonGuide;
import com.knoxhack.echoterminal.api.TerminalAddonInfo;
import com.knoxhack.echoterminal.api.TerminalAddonInfoProvider;
import com.knoxhack.echoterminal.api.TerminalAddonInfoRegistry;
import com.knoxhack.echoterminal.api.TerminalAddonMetric;
import com.knoxhack.echoterminal.api.TerminalAddonSection;
import com.knoxhack.echoterminal.api.TerminalContact;
import com.knoxhack.echoterminal.api.TerminalContactProvider;
import com.knoxhack.echoterminal.api.TerminalContactRegistry;
import com.knoxhack.echoterminal.api.TerminalUi;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class NpcTerminalIntegration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final int ACCENT = 0xFF66E8FF;

    private NpcTerminalIntegration() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        TerminalAddonInfoRegistry.register(new NpcoreAddonInfoProvider());
        TerminalContactRegistry.register(new NpcoreContactProvider());
    }

    private static final class NpcoreAddonInfoProvider implements TerminalAddonInfoProvider {
        @Override
        public String chapterId() {
            return "npcore";
        }

        @Override
        public TerminalAddonInfo info(Player player) {
            return new TerminalAddonInfo(
                    "Shared ECHO NPC foundation: profiles, visuals, dialogue, trades, services, and villager replacement.",
                    metrics(),
                    sections(player),
                    List.of(),
                    guide());
        }

        private static List<TerminalAddonMetric> metrics() {
            return List.of(
                    new TerminalAddonMetric("Profiles", String.valueOf(EchoNpcProfileManager.count()),
                            "loaded NPC profile definitions", colorFor(EchoNpcProfileManager.count())),
                    new TerminalAddonMetric("Visuals", String.valueOf(EchoNpcVisualProfileManager.count()),
                            "loaded texture and portrait profiles", colorFor(EchoNpcVisualProfileManager.count())),
                    new TerminalAddonMetric("Dialogue", String.valueOf(EchoNpcDialogueManager.count()),
                            "loaded dialogue trees", colorFor(EchoNpcDialogueManager.count())),
                    new TerminalAddonMetric("Trade Sets", String.valueOf(EchoNpcTradeManager.count()),
                            "server-authoritative custom trade sets", colorFor(EchoNpcTradeManager.count())),
                    new TerminalAddonMetric("Services", String.valueOf(EchoNpcServiceManager.count()),
                            "loaded service sets", colorFor(EchoNpcServiceManager.count())),
                    new TerminalAddonMetric("Replacements", String.valueOf(EchoNpcReplacementManager.count()),
                            "villager replacement mapping files", colorFor(EchoNpcReplacementManager.count())),
                    new TerminalAddonMetric("Storage", NpcDataBridge.storageMode(),
                            "trade stock, cooldown, and conversion record backend",
                            NpcDataBridge.persistentBackendAvailable() ? TerminalUi.GREEN : TerminalUi.AMBER),
                    new TerminalAddonMetric("MissionCore", EchoCoreServices.missionCoreAvailable() ? "READY" : "ABSENT",
                            "trade requiresMission gate availability",
                            EchoCoreServices.missionCoreAvailable() ? TerminalUi.GREEN : TerminalUi.AMBER),
                    new TerminalAddonMetric("ScreenCore", loaded("echoscreencore") ? "READY" : "ABSENT",
                            "NPC screen adapter availability", loaded("echoscreencore") ? TerminalUi.GREEN : TerminalUi.AMBER));
        }

        private static List<TerminalAddonSection> sections(Player player) {
            return List.of(
                    new TerminalAddonSection("Sample Contacts", sampleContacts()),
                    new TerminalAddonSection("Discovered Contacts", discoveredContacts(player)),
                    new TerminalAddonSection("Replacement Health", replacementHealth()),
                    new TerminalAddonSection("Smoke Test", List.of(
                            "Spawn echonpcore:echo_npc_spawn_egg, then right-click the NPC.",
                            "Use /echonpcore diagnose to compare command output with this Terminal summary.",
                            "Spawn a farmer villager or wandering trader to test configured conversion.")));
        }

        private static List<String> discoveredContacts(Player player) {
            if (player == null) {
                return List.of("Open a world Terminal after meeting NPCs to see discovered contacts.");
            }
            List<String> lines = NpcContactData.discoveredContacts(player, EchoNpcProfileManager.snapshot().values())
                    .stream()
                    .limit(8)
                    .map(contact -> contact.displayName() + " / " + contact.role() + " / " + contact.factionId())
                    .toList();
            return lines.isEmpty() ? List.of("No NPC contacts discovered yet.") : lines;
        }

        private static List<String> sampleContacts() {
            ArrayList<String> lines = new ArrayList<>();
            EchoNpcProfileManager.ids().stream()
                    .limit(6)
                    .map(Object::toString)
                    .forEach(lines::add);
            if (lines.isEmpty()) {
                lines.add("No NPC profiles are loaded yet. Run /reload or inspect datapack JSON.");
            }
            return List.copyOf(lines);
        }

        private static List<String> replacementHealth() {
            return List.of(
                    "Villagers: " + enabled(EchoNpcCoreConfig.REPLACE_VANILLA_VILLAGERS),
                    "Wandering trader: " + enabled(EchoNpcCoreConfig.REPLACE_WANDERING_TRADER),
                    "On spawn: " + enabled(EchoNpcCoreConfig.REPLACE_ON_SPAWN),
                    "On first interact: " + enabled(EchoNpcCoreConfig.REPLACE_ON_FIRST_INTERACT),
                    "Conversion mode: " + EchoNpcCoreConfig.conversionMode(),
                    "Storage mode: " + NpcDataBridge.storageMode(),
                    "NPCore data keys: " + NpcDataBridge.registeredDataKeyCount(),
                    "Factions loaded: " + EchoNpcFactionManager.count());
        }

        private static TerminalAddonGuide guide() {
            return TerminalAddonGuide.optional(180, "Shared NPC runtime",
                    "NPCore is the first-party NPC substrate for ECHO addons and datapacks.",
                    List.of(
                            "Author NPC profiles, visual profiles, dialogue, trades, and services under data/<namespace>/.",
                            "Use ScreenCore for the polished NPC UI when installed; classic UI remains the fallback.",
                            "Use replacement mappings to convert vanilla villagers and wandering traders into ECHO contacts."));
        }

        private static String enabled(EchoNativeConfigSpec.BooleanValue value) {
            return EchoNpcCoreConfig.bool(value, true) ? "enabled" : "disabled";
        }

        private static boolean loaded(String modid) {
            return EchoRuntimeModules.isLoaded(modid);
        }

        private static int colorFor(int count) {
            return count > 0 ? TerminalUi.GREEN : TerminalUi.AMBER;
        }
    }

    private static final class NpcoreContactProvider implements TerminalContactProvider {
        @Override
        public Identifier providerId() {
            return Identifier.fromNamespaceAndPath("echonpcore", "contacts");
        }

        @Override
        public List<TerminalContact> contacts(Player player) {
            if (player == null) {
                return List.of();
            }
            return NpcContactData.discoveredContacts(player, EchoNpcProfileManager.snapshot().values()).stream()
                    .map(contact -> {
                        var profile = EchoNpcProfileManager.getOrFallback(contact.profileId());
                        var services = EchoNpcServiceManager.getOrEmpty(profile.services()).services().stream()
                                .map(service -> service.title())
                                .toList();
                        return new TerminalContact(contact.profileId(), contact.displayName(), contact.role(),
                                contact.factionId(), profile.integrations().intelSummary(),
                                contact.lastInteractionTick(), services, profile.missions());
                    })
                    .toList();
        }
    }
}
