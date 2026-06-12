package com.knoxhack.echocore.command;

import java.util.ArrayList;
import java.util.List;

public final class EchoCommandRegistry {
    private static final List<Object> COMMANDS = new ArrayList<>();

    private EchoCommandRegistry() {
    }

    public static synchronized void register(Object command) {
        if (command != null && !COMMANDS.contains(command)) {
            COMMANDS.add(command);
        }
    }

    public static synchronized List<Object> commands() {
        return List.copyOf(COMMANDS);
    }
}
