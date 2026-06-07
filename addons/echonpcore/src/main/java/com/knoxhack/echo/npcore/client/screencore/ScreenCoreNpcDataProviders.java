package com.knoxhack.echo.npcore.client.screencore;

import com.knoxhack.echo.npcore.config.EchoNpcCoreConfig;
import com.knoxhack.echo.npcore.network.EchoNpcScreenState;
import com.knoxhack.echocore.api.EchoRuntimeModules;
import com.knoxhack.echoscreencore.api.EchoDataContext;
import com.knoxhack.echoscreencore.api.EchoDataProvider;
import com.knoxhack.echoscreencore.api.EchoScreenRegistry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ScreenCoreNpcDataProviders {
    static final EchoDataProvider PROVIDER = ScreenCoreNpcDataProviders::resolve;

    private ScreenCoreNpcDataProviders() {
    }

    static void register() {
        EchoScreenRegistry.registerDataProvider("npcore", PROVIDER);
    }

    private static Object resolve(EchoDataContext context, List<String> path) {
        Map<String, Object> root = root();
        if (path == null || path.isEmpty()) {
            return root;
        }
        Object current = root;
        for (String part : path) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(part);
            } else if (current instanceof List<?> list) {
                current = listValue(list, part);
            } else {
                return "";
            }
            if (current == null) {
                return "";
            }
        }
        return current;
    }

    private static Map<String, Object> root() {
        EchoNpcScreenState state = ScreenCoreNpcUiState.state();
        LinkedHashMap<String, Object> root = new LinkedHashMap<>();
        root.put("activeTab", ScreenCoreNpcUiState.selectedTab());
        root.put("contact", contact(state));
        root.put("tabs", tabs(state));
        root.put("dialogue", dialogue(state));
        root.put("trades", trades(state));
        root.put("services", services(state));
        root.put("intel", intel(state));
        root.put("status", status(state));
        root.put("integrations", integrations());
        root.put("bridges", bridgeRows());
        root.put("active", active(state));
        return root;
    }

    private static Map<String, Object> contact(EchoNpcScreenState state) {
        LinkedHashMap<String, Object> contact = new LinkedHashMap<>();
        contact.put("entityId", state == null ? 0 : state.entityId());
        contact.put("profileId", value(state == null ? "" : state.profileId(), "echonpcore:test_survivor"));
        contact.put("displayName", value(state == null ? "" : state.displayName(), "ECHO NPC"));
        contact.put("role", value(state == null ? "" : state.role(), "Field Contact"));
        contact.put("faction", value(state == null ? "" : state.faction(), "Unknown Faction"));
        contact.put("relationship", value(state == null ? "" : state.relationship(), "Neutral"));
        contact.put("relationshipValue", relationshipValue(String.valueOf(contact.get("relationship"))));
        contact.put("relationshipVariant", relationshipVariant(String.valueOf(contact.get("relationship"))));
        contact.put("portraitTexture", texture(state == null ? "" : state.portraitTexture(),
                "echonpcore:textures/entity/npc/missing_npc.png"));
        contact.put("badgeTexture", texture(state == null ? "" : state.badgeTexture(),
                "echonpcore:textures/gui/npc/badges/survivors.png"));
        contact.put("frameTexture", texture(state == null ? "" : state.frameTexture(),
                "echonpcore:textures/gui/npc/frames/survivor_frame.png"));
        contact.put("themeId", value(state == null ? "" : state.themeId(), "echonpcore:ashfall_survivor"));
        contact.put("profileLabel", shortId(String.valueOf(contact.get("profileId"))));
        contact.put("factionLabel", shortId(String.valueOf(contact.get("faction"))));
        contact.put("themeLabel", "Theme: " + shortId(String.valueOf(contact.get("themeId"))));
        contact.put("callsign", callsign(String.valueOf(contact.get("displayName")), String.valueOf(contact.get("profileId"))));
        contact.put("roleLine", contact.get("role") + " / " + contact.get("relationship"));
        contact.put("signalLabel", state == null ? "SIM" : "LIVE");
        contact.put("signalVariant", state == null ? "warning" : "ready");
        contact.put("subtitle", contact.get("role") + " / " + contact.get("factionLabel") + " / " + contact.get("relationship"));
        return contact;
    }

    private static List<Map<String, Object>> tabs(EchoNpcScreenState state) {
        String selected = ScreenCoreNpcUiState.selectedTab();
        List<Map<String, Object>> dialogueRows = dialogueRows(state);
        List<Map<String, Object>> tradeRows = tradeRows(state);
        List<Map<String, Object>> serviceRows = serviceRows(state);
        List<Map<String, Object>> intelRows = rows(intel(state).get("rows"));
        return List.of(
                tab("talk", "Talk", "Dialogue", "TALK", dialogueRows.size(), selected),
                tab("trade", "Trade", "Offers", "TRADE", tradeRows.size(), selected),
                tab("services", "Services", "Services", "SVC", serviceRows.size(), selected),
                tab("intel", "Intel", "Dossier", "INTEL", intelRows.size(), selected),
                tab("exit", "Exit", "Close", "EXIT", 1, selected));
    }

    private static Map<String, Object> tab(String id, String label, String tooltip, String chip, int count, String selected) {
        LinkedHashMap<String, Object> tab = new LinkedHashMap<>();
        tab.put("id", id);
        tab.put("label", label);
        tab.put("tooltip", tooltip);
        tab.put("chip", chip);
        tab.put("count", count);
        tab.put("countLabel", "exit".equals(id) ? "Close channel" : countLabel(count, "signal"));
        tab.put("selected", id.equals(selected));
        tab.put("variant", id.equals(selected) ? "active" : count > 0 ? "ready" : "locked");
        return tab;
    }

    private static Map<String, Object> dialogue(EchoNpcScreenState state) {
        LinkedHashMap<String, Object> dialogue = new LinkedHashMap<>();
        dialogue.put("nodeId", state == null ? "" : state.dialogueNodeId());
        dialogue.put("text", value(state == null ? "" : state.dialogueText(), "No dialogue available."));
        dialogue.put("options", dialogueRows(state));
        return dialogue;
    }

    private static List<Map<String, Object>> dialogueRows(EchoNpcScreenState state) {
        if (state == null || state.dialogueOptions().isEmpty()) {
            return List.of();
        }
        ArrayList<Map<String, Object>> rows = new ArrayList<>();
        for (EchoNpcScreenState.DialogueOptionState option : state.dialogueOptions()) {
            String detail = option.action().isBlank()
                    ? "Next: " + value(option.next(), "intro")
                    : actionLabel(option.action());
            LinkedHashMap<String, Object> row = row(option.id(), option.label(),
                    detail, "npcore.dialogue.select", option.id(), !option.available());
            row.put("chip", option.available() ? dialogueChip(option) : "LOCKED");
            row.put("status", option.available() ? "ready" : "locked");
            row.put("statusLabel", row.get("chip"));
            row.put("meta", option.available() ? "Dialogue channel / available" : "Dialogue channel / locked");
            row.put("disabledReason", option.disabledReason());
            row.put("tooltip", option.available() ? detail : value(option.disabledReason(), "Dialogue option unavailable."));
            rows.add(row);
        }
        return List.copyOf(rows);
    }

    private static Map<String, Object> trades(EchoNpcScreenState state) {
        LinkedHashMap<String, Object> trades = new LinkedHashMap<>();
        List<Map<String, Object>> tradeRows = tradeRows(state);
        trades.put("groups", tradeGroups(state));
        trades.put("rows", tradeRows);
        trades.put("count", tradeRows.size());
        return trades;
    }

    private static List<Map<String, Object>> tradeGroups(EchoNpcScreenState state) {
        if (state == null || state.tradeGroups().isEmpty()) {
            return List.of();
        }
        ArrayList<Map<String, Object>> groups = new ArrayList<>();
        for (EchoNpcScreenState.TradeGroupState group : state.tradeGroups()) {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            map.put("id", group.id());
            map.put("title", group.title());
            map.put("offers", tradeRows(group));
            groups.add(map);
        }
        return List.copyOf(groups);
    }

    private static List<Map<String, Object>> tradeRows(EchoNpcScreenState state) {
        if (state == null || state.tradeGroups().isEmpty()) {
            return List.of();
        }
        ArrayList<Map<String, Object>> rows = new ArrayList<>();
        for (EchoNpcScreenState.TradeGroupState group : state.tradeGroups()) {
            rows.addAll(tradeRows(group));
        }
        return List.copyOf(rows);
    }

    private static List<Map<String, Object>> tradeRows(EchoNpcScreenState.TradeGroupState group) {
        ArrayList<Map<String, Object>> rows = new ArrayList<>();
        for (EchoNpcScreenState.TradeOfferState offer : group.offers()) {
            String detail = costLine(offer.input()) + " -> " + costLine(List.of(offer.output()));
            String meta = tradeMeta(group, offer);
            boolean outOfStock = offer.limitedStock() && offer.stock() <= 0;
            boolean missionLocked = !offer.missionAllowed();
            boolean factionLocked = !offer.factionAllowed();
            LinkedHashMap<String, Object> row = row(offer.id(), offer.title(), detail,
                    "npcore.trade.request", offer.id(), outOfStock || missionLocked || factionLocked);
            row.put("chip", missionLocked || factionLocked ? "LOCKED" : outOfStock ? "EMPTY" : "TRADE");
            row.put("status", missionLocked || factionLocked ? "locked" : outOfStock ? "warning" : "ready");
            row.put("statusLabel", row.get("chip"));
            row.put("disabledReason", missionLocked ? value(offer.disabledReason(),
                    value(offer.missionMessage(), "Mission requirement not met."))
                    : factionLocked ? value(offer.disabledReason(),
                    value(offer.factionMessage(), "Faction standing requirement not met."))
                    : outOfStock ? "This offer is out of stock." : "");
            row.put("meta", meta);
            row.put("tooltip", row.get("disabledReason").toString().isBlank()
                    ? detail + " / " + meta : row.get("disabledReason"));
            row.put("stockLabel", stockLabel(offer));
            row.put("restockLabel", restockLabel(offer.restockRemaining()));
            row.put("factionLabel", offer.requiresFactionStanding() == Integer.MIN_VALUE
                    ? "" : "standing " + offer.requiresFactionStanding());
            row.put("mission", offer.requiresMission());
            rows.add(row);
        }
        return List.copyOf(rows);
    }

    private static Map<String, Object> services(EchoNpcScreenState state) {
        LinkedHashMap<String, Object> services = new LinkedHashMap<>();
        List<Map<String, Object>> serviceRows = serviceRows(state);
        services.put("rows", serviceRows);
        services.put("count", serviceRows.size());
        return services;
    }

    private static List<Map<String, Object>> serviceRows(EchoNpcScreenState state) {
        if (state == null || state.services().isEmpty()) {
            return List.of();
        }
        ArrayList<Map<String, Object>> rows = new ArrayList<>();
        for (EchoNpcScreenState.ServiceState service : state.services()) {
            boolean coolingDown = service.cooldownRemaining() > 0;
            boolean locked = !service.missionAllowed() || !service.factionAllowed();
            String detail = value(service.description(), "Field support request");
            String meta = serviceMeta(service);
            LinkedHashMap<String, Object> row = row(service.id(), service.title(), detail,
                    "npcore.service.request", service.id(), coolingDown || locked);
            row.put("chip", locked ? "LOCKED" : coolingDown ? "WAIT" : "SVC");
            row.put("statusLabel", row.get("chip"));
            row.put("status", locked ? "locked" : coolingDown ? "warning" : "ready");
            row.put("disabledReason", locked ? value(service.disabledReason(),
                    !service.missionAllowed() ? service.missionMessage() : "Faction standing requirement not met.")
                    : coolingDown ? "Service cooldown remaining: " + cooldownLabel(service.cooldownRemaining()) : "");
            row.put("meta", meta);
            row.put("tooltip", row.get("disabledReason").toString().isBlank()
                    ? detail + " / " + meta : row.get("disabledReason"));
            row.put("cooldownLabel", cooldownLabel(service.cooldownRemaining()));
            row.put("actionType", service.action());
            row.put("cooldownRemaining", service.cooldownRemaining());
            rows.add(row);
        }
        return List.copyOf(rows);
    }

    private static Map<String, Object> intel(EchoNpcScreenState state) {
        LinkedHashMap<String, Object> intel = new LinkedHashMap<>();
        intel.put("summary", "Local contact context from the synced NPC state and installed ECHO integrations.");
        intel.put("rows", List.of(
                intelRow("profile", "Profile", value(state == null ? "" : state.profileId(), "No profile loaded.")),
                intelRow("theme", "Theme", value(state == null ? "" : state.themeId(), "No theme loaded.")),
                intelRow("relationship", "Relationship", value(state == null ? "" : state.relationship(), "Contacted")),
                intelRow("bridges", "Optional Bridges", bridgeLine())));
        return intel;
    }

    private static String bridgeLine() {
        return "Terminal " + loadedLabel("echoterminal") + " / Mission " + loadedLabel("echomissioncore")
                + " / World " + loadedLabel("echoworldcore") + " / HoloMap " + loadedLabel("echoholomap")
                + " / Data " + loadedLabel("echodatacore");
    }

    private static String loadedLabel(String modid) {
        return EchoRuntimeModules.isLoaded(modid) ? "ready" : "absent";
    }

    private static List<Map<String, Object>> bridgeRows() {
        return List.of(
                bridgeRow("ScreenCore", true, "EUI page host"),
                bridgeRow("Terminal", EchoRuntimeModules.isLoaded("echoterminal"), "Terminal contact handoff"),
                bridgeRow("Data", EchoRuntimeModules.isLoaded("echodatacore"), "Datacore profile cache"),
                bridgeRow("Mission", EchoRuntimeModules.isLoaded("echomissioncore"), "Mission and objective hooks"),
                bridgeRow("World", EchoRuntimeModules.isLoaded("echoworldcore"), "World context hooks"),
                bridgeRow("HoloMap", EchoRuntimeModules.isLoaded("echoholomap"), "Map marker handoff"),
                bridgeRow("Fallback", EchoNpcCoreConfig.bool(EchoNpcCoreConfig.FALLBACK_TO_CLASSIC_NPC_SCREENS, true),
                        "Classic screen safety path"));
    }

    private static Map<String, Object> bridgeRow(String name, boolean ready, String tooltip) {
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("name", name);
        row.put("ready", ready);
        row.put("status", ready ? "ready" : "locked");
        row.put("label", ready ? "OK" : "OFF");
        row.put("tooltip", tooltip + (ready ? " ready." : " absent."));
        return row;
    }

    private static int bridgeReadyCount() {
        int ready = 0;
        for (Map<String, Object> bridge : bridgeRows()) {
            if (Boolean.TRUE.equals(bridge.get("ready"))) {
                ready++;
            }
        }
        return ready;
    }

    private static Map<String, Object> status(EchoNpcScreenState state) {
        LinkedHashMap<String, Object> status = new LinkedHashMap<>();
        String text = state == null || state.status().isBlank()
                ? "Server-authoritative NPC channel open."
                : state.status();
        status.put("text", text);
        String lower = text.toLowerCase(Locale.ROOT);
        boolean warning = lower.contains("missing") || lower.contains("unavailable") || lower.contains("cooling")
                || lower.contains("stock");
        status.put("level", warning ? "warning" : "ready");
        status.put("label", warning ? "CHECK" : "ONLINE");
        status.put("footer", warning ? "NPC channel open with advisory state." : "Server-authoritative NPC channel open.");
        int bridgeValue = bridgeReadyCount() * 100 / Math.max(1, bridgeRows().size());
        status.put("bridgeValue", bridgeValue);
        status.put("bridgeVariant", bridgeValue >= 70 ? "ready" : bridgeValue >= 35 ? "warning" : "locked");
        return status;
    }

    private static int relationshipValue(String relationship) {
        String lower = value(relationship, "neutral").toLowerCase(Locale.ROOT);
        if (lower.contains("allied") || lower.contains("trusted") || lower.contains("friendly")) {
            return 88;
        }
        if (lower.contains("contact") || lower.contains("known")) {
            return 68;
        }
        if (lower.contains("hostile") || lower.contains("enemy")) {
            return 18;
        }
        if (lower.contains("cold") || lower.contains("strained")) {
            return 34;
        }
        return 52;
    }

    private static String relationshipVariant(String relationship) {
        int value = relationshipValue(relationship);
        return value >= 70 ? "ready" : value >= 45 ? "warning" : "danger";
    }

    private static Map<String, Object> integrations() {
        LinkedHashMap<String, Object> integrations = new LinkedHashMap<>();
        integrations.put("screenCore", true);
        integrations.put("terminal", EchoRuntimeModules.isLoaded("echoterminal"));
        integrations.put("dataCore", EchoRuntimeModules.isLoaded("echodatacore"));
        integrations.put("missionCore", EchoRuntimeModules.isLoaded("echomissioncore"));
        integrations.put("worldCore", EchoRuntimeModules.isLoaded("echoworldcore"));
        integrations.put("holoMap", EchoRuntimeModules.isLoaded("echoholomap"));
        integrations.put("classicFallback", EchoNpcCoreConfig.bool(EchoNpcCoreConfig.FALLBACK_TO_CLASSIC_NPC_SCREENS, true));
        return integrations;
    }

    private static Map<String, Object> active(EchoNpcScreenState state) {
        return switch (ScreenCoreNpcUiState.selectedTab()) {
            case "trade" -> active("TRADE", "Verified barter manifest", "Server-validated offers.",
                    tradeRows(state), "No trades", "This NPC has no trade set loaded.", "ready", "TRADE");
            case "services" -> active("SERVICES", "Field support services", "Server-validated services.",
                    serviceRows(state), "No services", "This NPC has no service set loaded.", "ready", "SVC");
            case "intel" -> active("INTEL", "Local contact dossier", String.valueOf(intel(state).get("summary")),
                    rows(intel(state).get("rows")), "No intel", "No NPC intel is available.", "info", "INTEL");
            default -> active("DIALOGUE", "Conversation channel",
                    value(state == null ? "" : state.dialogueText(), "No dialogue available."),
                    dialogueRows(state), "No dialogue", "This dialogue node has no options.", "active", "TALK");
        };
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> rows(Object value) {
        return value instanceof List<?> list ? (List<Map<String, Object>>) list : List.of();
    }

    private static Map<String, Object> active(String title, String subtitle, String body, List<Map<String, Object>> rows,
            String emptyTitle, String emptyBody, String status, String statusLabel) {
        LinkedHashMap<String, Object> active = new LinkedHashMap<>();
        active.put("title", title);
        active.put("subtitle", subtitle);
        active.put("body", body);
        active.put("rows", rows);
        active.put("emptyTitle", emptyTitle);
        active.put("emptyBody", emptyBody);
        active.put("status", status);
        active.put("statusLabel", statusLabel);
        return active;
    }

    private static LinkedHashMap<String, Object> row(String id, String title, String detail, String action,
            String value, boolean disabled) {
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("title", value(title, id));
        row.put("detail", value(detail, ""));
        row.put("action", action == null ? "" : action.trim());
        row.put("value", value(value, id));
        row.put("disabled", disabled);
        row.put("status", disabled ? "warning" : "ready");
        row.put("chip", "NPC");
        row.put("statusLabel", "NPC");
        row.put("disabledReason", disabled ? "Unavailable." : "");
        row.put("meta", "NPC command");
        row.put("tooltip", value(detail, title));
        row.put("selected", false);
        return row;
    }

    private static LinkedHashMap<String, Object> intelRow(String id, String title, String detail) {
        LinkedHashMap<String, Object> row = row(id, title, detail, "", id, false);
        row.put("chip", "INFO");
        row.put("status", "info");
        row.put("statusLabel", "INFO");
        row.put("meta", "Dossier record");
        return row;
    }

    private static Object listValue(List<?> list, String part) {
        try {
            int index = Integer.parseInt(part);
            return index >= 0 && index < list.size() ? list.get(index) : "";
        } catch (NumberFormatException exception) {
            return "";
        }
    }

    private static String callsign(String displayName, String profileId) {
        String safe = value(displayName, shortId(profileId)).trim();
        if (safe.isBlank()) {
            return "CONTACT-00";
        }
        String[] words = safe.split("\\s+");
        String left = words.length == 0 ? safe : words[0];
        String right = words.length > 1 ? words[words.length - 1] : shortId(profileId);
        return (segment(left, 3) + "-" + segment(right, 3)).toUpperCase(Locale.ROOT);
    }

    private static String segment(String value, int length) {
        String cleaned = value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "");
        if (cleaned.isBlank()) {
            return "NPC";
        }
        return cleaned.substring(0, Math.min(length, cleaned.length()));
    }

    private static String tradeMeta(EchoNpcScreenState.TradeGroupState group, EchoNpcScreenState.TradeOfferState offer) {
        StringBuilder meta = new StringBuilder(value(group.title(), "Trade"))
                .append(" / ")
                .append(offer.limitedStock() ? stockLabel(offer) : "unlimited");
        if (offer.restockRemaining() > 0) {
            meta.append(" / ").append(restockLabel(offer.restockRemaining()));
        }
        if (!offer.requiresMission().isBlank()) {
            meta.append(" / mission ").append(shortId(offer.requiresMission()));
        }
        if (offer.requiresFactionStanding() != Integer.MIN_VALUE) {
            meta.append(" / standing ").append(offer.requiresFactionStanding());
        }
        return meta.toString();
    }

    private static String serviceMeta(EchoNpcScreenState.ServiceState service) {
        StringBuilder meta = new StringBuilder(value(service.action(), "support"))
                .append(" / ")
                .append(service.cost().isEmpty() ? "free" : costLine(service.cost()))
                .append(" / ")
                .append(service.cooldownRemaining() > 0 ? cooldownLabel(service.cooldownRemaining()) : "ready");
        if (!service.requiresMission().isBlank()) {
            meta.append(" / mission ").append(shortId(service.requiresMission()));
        }
        if (service.requiresFactionStanding() != Integer.MIN_VALUE) {
            meta.append(" / standing ").append(service.requiresFactionStanding());
        }
        return meta.toString();
    }

    private static String countLabel(int count, String noun) {
        return count + " " + noun + (count == 1 ? "" : "s");
    }

    private static String actionLabel(String action) {
        return switch (action) {
            case "open_trade" -> "Open trade tab";
            case "open_services" -> "Open services tab";
            case "open_intel" -> "Open intel panel";
            case "discover_contact" -> "Record Terminal contact";
            case "close" -> "Close interaction";
            default -> value(action, "Action");
        };
    }

    private static String dialogueChip(EchoNpcScreenState.DialogueOptionState option) {
        return switch (option.action()) {
            case "open_trade" -> "TRADE";
            case "open_services" -> "SVC";
            case "open_intel" -> "INTEL";
            case "discover_contact" -> "LOG";
            case "close" -> "EXIT";
            default -> option.next().isBlank() ? "TALK" : "NEXT";
        };
    }

    private static String stockLabel(EchoNpcScreenState.TradeOfferState offer) {
        if (!offer.limitedStock()) {
            return "unlimited";
        }
        return offer.stock() <= 0 ? "out of stock" : "stock " + offer.stock();
    }

    private static String cooldownLabel(long ticks) {
        if (ticks <= 0) {
            return "ready";
        }
        long seconds = Math.max(1L, Math.round(ticks / 20.0D));
        return seconds + "s cooldown";
    }

    private static String restockLabel(long ticks) {
        if (ticks <= 0) {
            return "";
        }
        long seconds = Math.max(1L, Math.round(ticks / 20.0D));
        return "restock in " + seconds + "s";
    }

    private static String costLine(List<EchoNpcScreenState.CostState> costs) {
        if (costs == null || costs.isEmpty()) {
            return "free";
        }
        return costs.stream()
                .filter(cost -> cost != null && !cost.item().isBlank() && cost.count() > 0)
                .map(cost -> cost.count() + "x " + shortId(cost.item()))
                .reduce((left, right) -> left + ", " + right)
                .orElse("free");
    }

    private static String shortId(String id) {
        int idx = id == null ? -1 : id.indexOf(':');
        return idx >= 0 && idx + 1 < id.length() ? id.substring(idx + 1) : value(id, "");
    }

    private static String texture(String value, String fallback) {
        return value(value, fallback);
    }

    private static String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
