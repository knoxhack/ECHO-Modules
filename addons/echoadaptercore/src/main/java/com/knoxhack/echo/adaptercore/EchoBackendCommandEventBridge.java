package com.knoxhack.echo.adaptercore;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class EchoBackendCommandEventBridge {
    private EchoBackendCommandEventBridge() {
    }

    public static CommandDispatcher<CommandSourceStack> dispatcher(Object event) {
        return event instanceof RegisterCommandsEvent commands ? commands.getDispatcher() : null;
    }

    public static CommandBuildContext buildContext(Object event) {
        return event instanceof RegisterCommandsEvent commands ? commands.getBuildContext() : null;
    }

    public static Commands.CommandSelection commandSelection(Object event) {
        return event instanceof RegisterCommandsEvent commands ? commands.getCommandSelection() : null;
    }

    public static CommandDispatcher<CommandSourceStack> clientDispatcher(Object event) {
        return event instanceof RegisterClientCommandsEvent commands ? commands.getDispatcher() : null;
    }
}
