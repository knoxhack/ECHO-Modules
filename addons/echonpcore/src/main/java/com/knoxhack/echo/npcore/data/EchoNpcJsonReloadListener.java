package com.knoxhack.echo.npcore.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.knoxhack.echo.npcore.EchoNpcCore;
import com.knoxhack.echo.npcore.config.EchoNpcCoreConfig;
import com.knoxhack.echo.npcore.conversion.EchoNpcReplacementManager;
import com.knoxhack.echo.npcore.conversion.EchoNpcReplacementMapping;
import com.knoxhack.echo.npcore.dialogue.EchoNpcDialogue;
import com.knoxhack.echo.npcore.dialogue.EchoNpcDialogueManager;
import com.knoxhack.echo.npcore.dialogue.EchoNpcDialogueNode;
import com.knoxhack.echo.npcore.dialogue.EchoNpcDialogueOption;
import com.knoxhack.echo.npcore.faction.EchoNpcFactionDefinition;
import com.knoxhack.echo.npcore.faction.EchoNpcFactionManager;
import com.knoxhack.echo.npcore.profile.EchoNpcProfile;
import com.knoxhack.echo.npcore.profile.EchoNpcBehaviorSettings;
import com.knoxhack.echo.npcore.profile.EchoNpcIntegrationHints;
import com.knoxhack.echo.npcore.profile.EchoNpcProfileManager;
import com.knoxhack.echo.npcore.service.EchoNpcServiceDefinition;
import com.knoxhack.echo.npcore.service.EchoNpcServiceManager;
import com.knoxhack.echo.npcore.service.EchoNpcServiceSet;
import com.knoxhack.echo.npcore.trade.EchoNpcTradeCost;
import com.knoxhack.echo.npcore.trade.EchoNpcTradeGroup;
import com.knoxhack.echo.npcore.trade.EchoNpcTradeManager;
import com.knoxhack.echo.npcore.trade.EchoNpcTradeOffer;
import com.knoxhack.echo.npcore.trade.EchoNpcTradeSet;
import com.knoxhack.echo.npcore.visual.EchoNpcVisualProfile;
import com.knoxhack.echo.npcore.visual.EchoNpcVisualProfileManager;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoDialogueTree;
import com.knoxhack.echocore.api.EchoFactionDefinition;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class EchoNpcJsonReloadListener extends SimplePreparableReloadListener<EchoNpcJsonReloadListener.LoadedContent> {
    private static final String PROFILE_DIR = "npc_profiles";
    private static final String VISUAL_DIR = "npc_visual_profiles";
    private static final String DIALOGUE_DIR = "npc_dialogues";
    private static final String TRADE_DIR = "npc_trades";
    private static final String SERVICE_DIR = "npc_services";
    private static final String FACTION_DIR = "npc_factions";
    private static final String REPLACEMENT_DIR = "villager_replacements";

    @Override
    protected LoadedContent prepare(ResourceManager manager, ProfilerFiller profiler) {
        List<String> warnings = new ArrayList<>();
        return new LoadedContent(
                load(manager, PROFILE_DIR, warnings, EchoNpcJsonReloadListener::parseProfile),
                load(manager, VISUAL_DIR, warnings, EchoNpcJsonReloadListener::parseVisual),
                load(manager, DIALOGUE_DIR, warnings, EchoNpcJsonReloadListener::parseDialogue),
                load(manager, TRADE_DIR, warnings, EchoNpcJsonReloadListener::parseTradeSet),
                load(manager, SERVICE_DIR, warnings, EchoNpcJsonReloadListener::parseServiceSet),
                load(manager, FACTION_DIR, warnings, EchoNpcJsonReloadListener::parseFaction),
                load(manager, REPLACEMENT_DIR, warnings, EchoNpcJsonReloadListener::parseReplacement),
                warnings);
    }

    @Override
    protected void apply(LoadedContent content, ResourceManager manager, ProfilerFiller profiler) {
        EchoNpcProfileManager.replace(content.profiles(), content.warnings());
        EchoNpcVisualProfileManager.replace(content.visualProfiles());
        EchoNpcDialogueManager.replace(content.dialogues());
        EchoNpcTradeManager.replace(content.trades());
        EchoNpcServiceManager.replace(content.services());
        EchoNpcFactionManager.replace(content.factions());
        registerEchoCoreFactions(content.factions());
        EchoNpcReplacementManager.replace(content.replacements());
        EchoNpcCore.LOGGER.info("NPCore loaded {} profile(s), {} visual profile(s), {} dialogue(s), {} trade set(s), {} service set(s), {} faction(s), {} replacement mapping(s), {} warning(s).",
                content.profiles().size(), content.visualProfiles().size(), content.dialogues().size(),
                content.trades().size(), content.services().size(), content.factions().size(),
                content.replacements().size(), content.warnings().size());
        if (EchoNpcCoreConfig.bool(EchoNpcCoreConfig.DEBUG_PROFILE_LOADING, true)) {
            content.warnings().forEach(warning -> EchoNpcCore.LOGGER.warn("NPCore data warning: {}", warning));
        }
    }

    private static void registerEchoCoreFactions(Map<Identifier, EchoNpcFactionDefinition> factions) {
        for (EchoNpcFactionDefinition faction : factions.values()) {
            try {
                EchoCoreServices.registerFaction(new EchoFactionDefinition(
                        faction.id(),
                        faction.displayName(),
                        faction.shortName(),
                        "",
                        "NPCore contact faction.",
                        "",
                        "",
                        "NPC contacts, trades, and services.",
                        0xFF66E8FF,
                        false,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        EchoDialogueTree.EMPTY));
            } catch (RuntimeException exception) {
                EchoNpcCore.LOGGER.debug("NPCore faction {} was already registered by another provider.", faction.id(), exception);
            }
        }
    }

    private static <T> Map<Identifier, T> load(ResourceManager manager, String dir, List<String> warnings, Parser<T> parser) {
        Map<Identifier, T> values = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Resource> entry : manager.listResources(dir, id -> id.getPath().endsWith(".json")).entrySet()) {
            Identifier resourceId = entry.getKey();
            Identifier fallbackId = contentId(resourceId, dir);
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonObject()) {
                    throw new JsonParseException("Root must be a JSON object.");
                }
                Entry<T> parsed = parser.parse(fallbackId, root.getAsJsonObject(), warnings);
                if (values.put(parsed.id(), parsed.value()) != null) {
                    warnings.add("Duplicate NPCore " + dir + " id " + parsed.id() + " from " + resourceId + " replaced earlier entry.");
                }
            } catch (IOException | RuntimeException exception) {
                warnings.add("Could not parse NPCore " + dir + " file " + resourceId + ": " + exception.getMessage());
                EchoNpcCore.LOGGER.warn("Could not parse NPCore {} file {}.", dir, resourceId, exception);
            }
        }
        return values;
    }

    private static Entry<EchoNpcProfile> parseProfile(Identifier fallbackId, JsonObject json, List<String> warnings) {
        Identifier id = identifier(json, "id", fallbackId);
        Identifier visual = identifier(json, "visualProfile", id);
        Identifier dialogue = identifier(json, "dialogue", id);
        EchoNpcProfile profile = new EchoNpcProfile(
                id,
                string(json, "displayName", id.getPath()),
                string(json, "role", "Contact"),
                identifier(json, "faction", Identifier.fromNamespaceAndPath(EchoNpcCore.MODID, "survivors")),
                visual,
                dialogue,
                nullableIdentifier(json, "trades", EchoNpcProfile.NO_TRADES),
                nullableIdentifier(json, "services", EchoNpcProfile.NO_SERVICES),
                identifierList(json, "missions"),
                stringList(json, "ambientLines"),
                doubleValue(json, "interactionRange", 0.0D),
                parseBehavior(object(json, "behavior")),
                parseIntegrations(object(json, "integrations")),
                stringMap(json, "convertedFrom"));
        return new Entry<>(id, profile);
    }

    private static Entry<EchoNpcVisualProfile> parseVisual(Identifier fallbackId, JsonObject json, List<String> warnings) {
        Identifier id = identifier(json, "id", fallbackId);
        List<EchoNpcVisualProfile.Layer> layers = new ArrayList<>();
        for (JsonElement element : array(json, "layers")) {
            JsonObject layer = requireObject(element, "visual layer");
            layers.add(new EchoNpcVisualProfile.Layer(
                    string(layer, "type", "overlay"),
                    nullableIdentifier(layer, "texture", null),
                    bool(layer, "emissive", false),
                    string(layer, "tint", ""),
                    string(layer, "visibleWhen", "")));
        }
        return new Entry<>(id, new EchoNpcVisualProfile(
                id,
                identifier(json, "model", Identifier.fromNamespaceAndPath(EchoNpcCore.MODID, "humanoid_basic")),
                identifier(json, "texture", EchoNpcVisualProfile.FALLBACK_TEXTURE),
                nullableIdentifier(json, "emissiveTexture", null),
                nullableIdentifier(json, "portrait", null),
                nullableIdentifier(json, "factionBadge", null),
                nullableIdentifier(json, "screenFrame", null),
                string(json, "nameplateStyle", "survivor"),
                identifier(json, "theme", Identifier.fromNamespaceAndPath(EchoNpcCore.MODID, "ashfall_survivor")),
                layers));
    }

    private static Entry<EchoNpcDialogue> parseDialogue(Identifier fallbackId, JsonObject json, List<String> warnings) {
        Identifier id = identifier(json, "id", fallbackId);
        Map<String, EchoNpcDialogueNode> nodes = new LinkedHashMap<>();
        JsonObject nodesObject = object(json, "nodes");
        for (Map.Entry<String, JsonElement> entry : nodesObject.entrySet()) {
            JsonObject node = requireObject(entry.getValue(), "dialogue node");
            List<EchoNpcDialogueOption> options = new ArrayList<>();
            for (JsonElement optionElement : array(node, "options")) {
                JsonObject option = requireObject(optionElement, "dialogue option");
                options.add(new EchoNpcDialogueOption(
                        string(option, "id", "option"),
                        string(option, "label", "Continue."),
                        string(option, "next", ""),
                        string(option, "action", ""),
                        string(option, "requiresMission", ""),
                        integer(option, "requiresFactionStanding", Integer.MIN_VALUE),
                        string(option, "disabledReason", ""),
                        string(option, "target", ""),
                        string(option, "actionId", "")));
            }
            nodes.put(entry.getKey(), new EchoNpcDialogueNode(string(node, "text", "No dialogue available."), options));
        }
        if (nodes.isEmpty()) {
            warnings.add("NPCore dialogue " + id + " has no nodes; it will use a fallback line.");
        }
        return new Entry<>(id, new EchoNpcDialogue(id, string(json, "start", "intro"), nodes));
    }

    private static Entry<EchoNpcTradeSet> parseTradeSet(Identifier fallbackId, JsonObject json, List<String> warnings) {
        Identifier id = identifier(json, "id", fallbackId);
        List<EchoNpcTradeGroup> groups = new ArrayList<>();
        for (JsonElement groupElement : array(json, "groups")) {
            JsonObject group = requireObject(groupElement, "trade group");
            List<EchoNpcTradeOffer> offers = new ArrayList<>();
            for (JsonElement offerElement : array(group, "offers")) {
                JsonObject offer = requireObject(offerElement, "trade offer");
                offers.add(new EchoNpcTradeOffer(
                        string(offer, "id", "offer"),
                        string(offer, "title", "Offer"),
                        costList(offer, "input"),
                        cost(offer.getAsJsonObject("output")),
                        integer(offer, "stock", 0),
                        integer(offer, "restockTime", 0),
                        string(offer, "requiresMission", ""),
                        integer(offer, "requiresFactionStanding", Integer.MIN_VALUE),
                        string(offer, "disabledReason", "")));
            }
            groups.add(new EchoNpcTradeGroup(string(group, "id", "default"), string(group, "title", "Trades"), offers));
        }
        return new Entry<>(id, new EchoNpcTradeSet(id, groups));
    }

    private static Entry<EchoNpcServiceSet> parseServiceSet(Identifier fallbackId, JsonObject json, List<String> warnings) {
        Identifier id = identifier(json, "id", fallbackId);
        List<EchoNpcServiceDefinition> services = new ArrayList<>();
        for (JsonElement element : array(json, "services")) {
            JsonObject service = requireObject(element, "service definition");
            services.add(new EchoNpcServiceDefinition(
                    string(service, "id", "service"),
                    string(service, "title", "Service"),
                    string(service, "description", ""),
                    costList(service, "cost"),
                    string(service, "action", "noop"),
                    integer(service, "amount", 0),
                    integer(service, "cooldown", 0),
                    string(service, "requiresMission", ""),
                    integer(service, "requiresFactionStanding", Integer.MIN_VALUE),
                    string(service, "target", ""),
                    string(service, "actionId", ""),
                    string(service, "disabledReason", "")));
        }
        return new Entry<>(id, new EchoNpcServiceSet(id, services));
    }

    private static EchoNpcBehaviorSettings parseBehavior(JsonObject json) {
        if (json == null || json.entrySet().isEmpty()) {
            return EchoNpcBehaviorSettings.DEFAULT;
        }
        return new EchoNpcBehaviorSettings(
                string(json, "mode", "settler_trader"),
                integer(json, "wanderRadius", EchoNpcBehaviorSettings.DEFAULT.wanderRadius()),
                integer(json, "returnRadius", EchoNpcBehaviorSettings.DEFAULT.returnRadius()),
                integer(json, "ambientCooldown", EchoNpcBehaviorSettings.DEFAULT.ambientCooldown()),
                bool(json, "stationary", false),
                bool(json, "homebound", true));
    }

    private static EchoNpcIntegrationHints parseIntegrations(JsonObject json) {
        if (json == null || json.entrySet().isEmpty()) {
            return EchoNpcIntegrationHints.DEFAULT;
        }
        return new EchoNpcIntegrationHints(
                bool(json, "terminalContact", true),
                bool(json, "mapMarker", true),
                bool(json, "discoverOnInteract", true),
                string(json, "intelSummary", ""));
    }

    private static Entry<EchoNpcFactionDefinition> parseFaction(Identifier fallbackId, JsonObject json, List<String> warnings) {
        Identifier id = identifier(json, "id", fallbackId);
        return new Entry<>(id, new EchoNpcFactionDefinition(
                id,
                string(json, "displayName", id.getPath()),
                string(json, "shortName", id.getPath()),
                string(json, "theme", "")));
    }

    private static Entry<EchoNpcReplacementMapping> parseReplacement(Identifier fallbackId, JsonObject json, List<String> warnings) {
        Identifier id = identifier(json, "id", fallbackId);
        return new Entry<>(id, new EchoNpcReplacementMapping(
                id,
                identifierMap(json, "replace"),
                identifierMap(json, "entityTypes")));
    }

    private static EchoNpcTradeCost cost(JsonObject json) {
        if (json == null) {
            throw new JsonParseException("Trade output/cost must be an object.");
        }
        return new EchoNpcTradeCost(identifier(json, "item", Identifier.fromNamespaceAndPath("minecraft", "air")),
                integer(json, "count", 1));
    }

    private static List<EchoNpcTradeCost> costList(JsonObject json, String key) {
        List<EchoNpcTradeCost> costs = new ArrayList<>();
        for (JsonElement element : array(json, key)) {
            costs.add(cost(requireObject(element, "cost")));
        }
        return costs;
    }

    private static Identifier contentId(Identifier resourceId, String directory) {
        String path = resourceId.getPath();
        String prefix = directory + "/";
        if (path.startsWith(prefix)) {
            path = path.substring(prefix.length());
        }
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - ".json".length());
        }
        return Identifier.fromNamespaceAndPath(resourceId.getNamespace(), path);
    }

    private static JsonObject object(JsonObject json, String key) {
        JsonElement element = json.get(key);
        return element == null || element.isJsonNull() ? new JsonObject() : requireObject(element, key);
    }

    private static JsonObject requireObject(JsonElement element, String label) {
        if (element == null || !element.isJsonObject()) {
            throw new JsonParseException(label + " must be a JSON object.");
        }
        return element.getAsJsonObject();
    }

    private static JsonArray array(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element == null || element.isJsonNull()) {
            return new JsonArray();
        }
        if (!element.isJsonArray()) {
            throw new JsonParseException("Field '" + key + "' must be an array.");
        }
        return element.getAsJsonArray();
    }

    private static Identifier identifier(JsonObject json, String key, Identifier fallback) {
        String value = string(json, key, "");
        return value.isBlank() ? fallback : Identifier.parse(value);
    }

    private static Identifier nullableIdentifier(JsonObject json, String key, Identifier fallback) {
        String value = string(json, key, "");
        return value.isBlank() ? fallback : Identifier.parse(value);
    }

    private static List<Identifier> identifierList(JsonObject json, String key) {
        List<Identifier> values = new ArrayList<>();
        for (JsonElement element : array(json, key)) {
            if (!element.isJsonNull()) {
                values.add(Identifier.parse(element.getAsString()));
            }
        }
        return values;
    }

    private static Map<Identifier, Identifier> identifierMap(JsonObject json, String key) {
        Map<Identifier, Identifier> values = new LinkedHashMap<>();
        JsonObject map = object(json, key);
        for (Map.Entry<String, JsonElement> entry : map.entrySet()) {
            if (!entry.getValue().isJsonNull()) {
                values.put(Identifier.parse(entry.getKey()), Identifier.parse(entry.getValue().getAsString()));
            }
        }
        return values;
    }

    private static Map<String, String> stringMap(JsonObject json, String key) {
        Map<String, String> values = new LinkedHashMap<>();
        JsonObject map = object(json, key);
        for (Map.Entry<String, JsonElement> entry : map.entrySet()) {
            values.put(entry.getKey(), entry.getValue().isJsonNull() ? "" : entry.getValue().getAsString());
        }
        return values;
    }

    private static List<String> stringList(JsonObject json, String key) {
        List<String> values = new ArrayList<>();
        for (JsonElement element : array(json, key)) {
            if (!element.isJsonNull()) {
                values.add(element.getAsString());
            }
        }
        return values;
    }

    private static String string(JsonObject json, String key, String fallback) {
        JsonElement element = json.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsString();
    }

    private static int integer(JsonObject json, String key, int fallback) {
        JsonElement element = json.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsInt();
    }

    private static double doubleValue(JsonObject json, String key, double fallback) {
        JsonElement element = json.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsDouble();
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        JsonElement element = json.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsBoolean();
    }

    private interface Parser<T> {
        Entry<T> parse(Identifier fallbackId, JsonObject json, List<String> warnings);
    }

    private record Entry<T>(Identifier id, T value) {
    }

    public record LoadedContent(
            Map<Identifier, EchoNpcProfile> profiles,
            Map<Identifier, EchoNpcVisualProfile> visualProfiles,
            Map<Identifier, EchoNpcDialogue> dialogues,
            Map<Identifier, EchoNpcTradeSet> trades,
            Map<Identifier, EchoNpcServiceSet> services,
            Map<Identifier, EchoNpcFactionDefinition> factions,
            Map<Identifier, EchoNpcReplacementMapping> replacements,
            List<String> warnings) {
        public LoadedContent {
            profiles = Map.copyOf(profiles == null ? Map.of() : profiles);
            visualProfiles = Map.copyOf(visualProfiles == null ? Map.of() : visualProfiles);
            dialogues = Map.copyOf(dialogues == null ? Map.of() : dialogues);
            trades = Map.copyOf(trades == null ? Map.of() : trades);
            services = Map.copyOf(services == null ? Map.of() : services);
            factions = Map.copyOf(factions == null ? Map.of() : factions);
            replacements = Map.copyOf(replacements == null ? Map.of() : replacements);
            warnings = List.copyOf(warnings == null ? List.of() : warnings);
        }
    }
}
