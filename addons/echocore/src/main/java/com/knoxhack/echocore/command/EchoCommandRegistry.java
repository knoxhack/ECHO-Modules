package com.knoxhack.echocore.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class EchoCommandRegistry {
    private static final List<Object> COMMANDS = new ArrayList<>();
    private static final IdentityHashMap<CommandDispatcher<CommandSourceStack>, Set<Object>> REGISTERED_BY_DISPATCHER =
            new IdentityHashMap<>();

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

    public static void onRegisterCommands(Object event) {
        if (event instanceof RegisterCommandsEvent commands) {
            registerAll(commands.getDispatcher());
        }
    }

    public static synchronized int registerAll(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (dispatcher == null || COMMANDS.isEmpty()) {
            return 0;
        }
        Set<Object> registered = REGISTERED_BY_DISPATCHER.computeIfAbsent(
                dispatcher,
                ignored -> Collections.newSetFromMap(new IdentityHashMap<>()));
        List<Object> pendingCommandObjects = new ArrayList<>();
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("echo");
        for (Object command : COMMANDS) {
            if (registered.contains(command) || !(command instanceof LiteralArgumentBuilder<?> literal)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            LiteralArgumentBuilder<CommandSourceStack> typed =
                    (LiteralArgumentBuilder<CommandSourceStack>) literal;
            root.then(typed);
            pendingCommandObjects.add(command);
        }
        if (pendingCommandObjects.isEmpty()) {
            return 0;
        }
        dispatcher.register(root);
        pendingCommandObjects.forEach(registered::add);
        return pendingCommandObjects.size();
    }
}
