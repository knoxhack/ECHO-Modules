package com.knoxhack.echoterminal.client.screen;

import com.knoxhack.echoterminal.EchoTerminalDashboardContract;
import com.knoxhack.echoterminal.api.TerminalTab;
import com.knoxhack.echoterminal.api.TerminalTabChrome;
import com.knoxhack.echoterminal.api.TerminalTabDescriptor;
import com.knoxhack.echoterminal.api.TerminalTabRegistry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;

public final class EchoTerminalNativeSessionBridge {
    private static final int HISTORY_LIMIT = 12;
    private static final Deque<Map<String, Object>> COMMAND_HISTORY = new ArrayDeque<>();
    private static final Deque<String> OUTPUT_HISTORY = new ArrayDeque<>();
    private static volatile Map<String, Object> lastSession = Map.of(
            "nativeTerminalSessionReady", false,
            "commandHistory", List.of(),
            "outputHistory", List.of(),
            "modulePages", List.of());

    private EchoTerminalNativeSessionBridge() {
    }

    public static synchronized Map<String, Object> recordNativeOpen(
            String actionId,
            Map<String, Object> actionMetadata,
            boolean opened,
            boolean screenAlreadyOpen
    ) {
        String command = commandFor(actionId, actionMetadata);
        Map<String, Object> commandResult = EchoTerminalDashboardContract.executeReferenceCommand(command);
        Map<String, Object> commandEntry = new LinkedHashMap<>();
        commandEntry.put("actionId", clean(actionId, "terminal.open"));
        commandEntry.put("command", command);
        commandEntry.put("opened", opened);
        commandEntry.put("screenAlreadyOpen", screenAlreadyOpen);
        commandEntry.put("pageId", String.valueOf(commandResult.get("pageId")));
        commandEntry.put("focusedControl", String.valueOf(commandResult.get("focusedControl")));
        push(COMMAND_HISTORY, Map.copyOf(commandEntry));
        push(OUTPUT_HISTORY, outputLine(commandEntry, commandResult));

        Map<String, Object> session = new LinkedHashMap<>();
        session.put("nativeTerminalSessionReady", true);
        session.put("terminalLiveUxBridge", "echo-terminal-native-session");
        session.put("actionId", clean(actionId, "terminal.open"));
        session.put("command", command);
        session.put("opened", opened);
        session.put("screenAlreadyOpen", screenAlreadyOpen);
        session.put("commandExecuted", commandResult.get("commandExecuted"));
        session.put("pageId", commandResult.get("pageId"));
        session.put("focusedControl", commandResult.get("focusedControl"));
        session.put("visibleCards", commandResult.get("visibleCards"));
        session.put("actions", commandResult.get("actions"));
        session.put("actionDispatch", actionDispatch(commandResult.get("actions")));
        session.put("diagnostics", commandResult.get("diagnostics"));
        List<Map<String, Object>> pages = modulePages();
        session.put("modulePages", pages);
        session.put("modulePageCount", pages.size());
        session.put("modulePageGroups", modulePageGroups(pages));
        session.put("commandHistory", List.copyOf(COMMAND_HISTORY));
        session.put("outputHistory", List.copyOf(OUTPUT_HISTORY));
        session.put("commandHistoryCount", COMMAND_HISTORY.size());
        session.put("outputHistoryCount", OUTPUT_HISTORY.size());
        session.put("outputHistoryReady", !OUTPUT_HISTORY.isEmpty());
        Map<String, Object> playerContext = playerContext();
        session.put("playerContext", playerContext);
        session.put("worldIntegration", worldIntegration(playerContext));
        session.put("playerIntegration", playerIntegration(playerContext));
        session.put("nativeTerminalLiveUxComplete", Boolean.TRUE.equals(commandResult.get("commandExecuted"))
                && !pages.isEmpty()
                && !OUTPUT_HISTORY.isEmpty()
                && Boolean.TRUE.equals(playerContext.get("playerPresent"))
                && Boolean.TRUE.equals(playerContext.get("levelPresent")));
        session.put("summary", "Terminal native session routed a command, recorded output history, exposed module pages, dispatched linked surface actions, and captured live player/world context.");
        lastSession = Map.copyOf(session);
        return lastSession;
    }

    public static Map<String, Object> snapshot() {
        return lastSession;
    }

    private static String commandFor(String actionId, Map<String, Object> actionMetadata) {
        Object explicitCommand = actionMetadata == null ? null : actionMetadata.get("command");
        String command = clean(explicitCommand, "");
        if (!command.isBlank()) {
            return command;
        }
        return switch (clean(actionId, "terminal.open")) {
            case "signalos.terminal" -> "open:signalos_dashboard";
            case "terminal.open" -> EchoTerminalDashboardContract.REFERENCE_COMMAND;
            default -> "open:" + clean(actionId, "terminal.open").replace('.', '_');
        };
    }

    private static List<Map<String, Object>> modulePages() {
        List<Map<String, Object>> pages = new ArrayList<>();
        for (TerminalTab tab : TerminalTabRegistry.tabs()) {
            TerminalTabDescriptor descriptor = tab.descriptor();
            TerminalTabChrome chrome = tab.chrome();
            Map<String, Object> page = new LinkedHashMap<>();
            page.put("id", descriptor.id().toString());
            page.put("title", descriptor.title());
            page.put("shortTitle", chrome.shortTitle());
            page.put("group", chrome.group());
            page.put("summary", chrome.summary());
            page.put("order", descriptor.order());
            pages.add(Map.copyOf(page));
        }
        return List.copyOf(pages);
    }

    private static Map<String, Object> modulePageGroups(List<Map<String, Object>> pages) {
        Map<String, Object> groups = new LinkedHashMap<>();
        for (Map<String, Object> page : pages) {
            String group = clean(page.get("group"), "terminal");
            int count = 0;
            Object existing = groups.get(group);
            if (existing instanceof Number number) {
                count = number.intValue();
            }
            groups.put(group, count + 1);
        }
        return Map.copyOf(groups);
    }

    private static List<Map<String, Object>> actionDispatch(Object actions) {
        List<Map<String, Object>> dispatch = new ArrayList<>();
        for (String action : strings(actions)) {
            String[] parts = action.split(":", 3);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("action", action);
            entry.put("kind", parts.length > 0 ? parts[0] : "");
            entry.put("targetModule", parts.length > 1 ? parts[1] : "");
            entry.put("targetRoute", parts.length > 2 ? parts[2] : "");
            entry.put("dispatchReady", parts.length == 3);
            dispatch.add(Map.copyOf(entry));
        }
        return List.copyOf(dispatch);
    }

    private static Map<String, Object> playerContext() {
        Minecraft minecraft = Minecraft.getInstance();
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("playerPresent", minecraft.player != null);
        context.put("levelPresent", minecraft.level != null);
        context.put("screen", minecraft.screen == null ? "none" : minecraft.screen.getClass().getName());
        if (minecraft.player != null) {
            context.put("playerName", minecraft.player.getName().getString());
            context.put("dimension", minecraft.player.level().dimension().identifier().toString());
            context.put("blockPosition", minecraft.player.blockPosition().toShortString());
            context.put("health", Math.round(minecraft.player.getHealth()));
            context.put("armor", minecraft.player.getArmorValue());
            context.put("levelGameTime", minecraft.player.level().getGameTime());
        }
        return Map.copyOf(context);
    }

    private static Map<String, Object> worldIntegration(Map<String, Object> playerContext) {
        return Map.of(
                "levelPresent", Boolean.TRUE.equals(playerContext.get("levelPresent")),
                "dimension", clean(playerContext.get("dimension"), ""),
                "blockPosition", clean(playerContext.get("blockPosition"), ""),
                "levelGameTime", playerContext.getOrDefault("levelGameTime", 0),
                "liveWorldContext", Boolean.TRUE.equals(playerContext.get("levelPresent"))
                        && !clean(playerContext.get("dimension"), "").isBlank()
        );
    }

    private static Map<String, Object> playerIntegration(Map<String, Object> playerContext) {
        return Map.of(
                "playerPresent", Boolean.TRUE.equals(playerContext.get("playerPresent")),
                "playerName", clean(playerContext.get("playerName"), ""),
                "health", playerContext.getOrDefault("health", 0),
                "armor", playerContext.getOrDefault("armor", 0),
                "livePlayerContext", Boolean.TRUE.equals(playerContext.get("playerPresent"))
                        && !clean(playerContext.get("playerName"), "").isBlank()
        );
    }

    private static String outputLine(Map<String, Object> commandEntry, Map<String, Object> commandResult) {
        return "terminal.native " + commandEntry.get("actionId")
                + " -> " + commandResult.get("pageId")
                + " / " + commandResult.get("focusedControl");
    }

    private static <T> void push(Deque<T> history, T value) {
        history.addFirst(value);
        while (history.size() > HISTORY_LIMIT) {
            history.removeLast();
        }
    }

    private static List<String> strings(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : iterable) {
            String text = clean(item, "");
            if (!text.isBlank()) {
                result.add(text);
            }
        }
        return List.copyOf(result);
    }

    private static String clean(Object value, String fallback) {
        String text = value == null ? "" : String.valueOf(value).trim();
        return text.isBlank() ? fallback : text;
    }
}
