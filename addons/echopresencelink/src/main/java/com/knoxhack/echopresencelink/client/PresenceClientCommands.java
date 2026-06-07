package com.knoxhack.echopresencelink.client;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class PresenceClientCommands {
    private PresenceClientCommands() {
    }

    public static void register(Object event) {
        var dispatcher = dispatcher(event);
        if (dispatcher == null) {
            return;
        }
        dispatcher.register(Commands.literal("echopresence")
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .then(Commands.literal("test").executes(context -> test(context.getSource())))
                .then(Commands.literal("resend").executes(context -> resend(context.getSource()))));
    }

    private static int status(CommandSourceStack source) {
        send(source, "Presence Link status", ChatFormatting.AQUA);
        for (String line : PresenceController.INSTANCE.statusLines()) {
            send(source, line, ChatFormatting.GRAY);
        }
        return 1;
    }

    private static int test(CommandSourceStack source) {
        PresenceController.CommandResult result = PresenceController.INSTANCE.sendMinimalTestActivity();
        sendResult(source, result);
        return result.success() ? 1 : 0;
    }

    private static int resend(CommandSourceStack source) {
        PresenceController.CommandResult result = PresenceController.INSTANCE.forceResend();
        sendResult(source, result);
        return result.success() ? 1 : 0;
    }

    private static void sendResult(CommandSourceStack source, PresenceController.CommandResult result) {
        send(source, result.message(), result.success() ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private static void send(CommandSourceStack source, String message, ChatFormatting color) {
        Component component = Component.literal("[ECHO Presence] " + message).withStyle(color);
        if (color == ChatFormatting.RED) {
            source.sendFailure(component);
        } else {
            source.sendSuccess(() -> component, false);
        }
    }

    @SuppressWarnings("unchecked")
    private static com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher(Object event) {
        if (event == null) {
            return null;
        }
        try {
            Object value = event.getClass().getMethod("getDispatcher").invoke(event);
            return value instanceof com.mojang.brigadier.CommandDispatcher<?> commandDispatcher
                    ? (com.mojang.brigadier.CommandDispatcher<CommandSourceStack>) commandDispatcher
                    : null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }
}
