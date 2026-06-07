package com.knoxhack.echowiki.command;

import com.knoxhack.echowiki.content.GuideBookDefinition;
import com.knoxhack.echowiki.content.GuideBookLabels;
import com.knoxhack.echowiki.content.GuideBookRegistry;
import com.knoxhack.echowiki.item.GuideBookStacks;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Collection;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.ItemStack;

public final class GuideBookCommands {
    private GuideBookCommands() {
    }

    public static void register(Object event) {
        CommandDispatcher<CommandSourceStack> dispatcher = dispatcher(event);
        if (dispatcher == null) {
            return;
        }
        dispatcher.register(
                Commands.literal("echowiki")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .then(Commands.literal("guidebook")
                                .then(Commands.literal("list")
                                        .executes(context -> list(context.getSource())))
                                .then(Commands.literal("give")
                                        .then(Commands.argument("id", StringArgumentType.string())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                        visibleIdSuggestions(),
                                                        builder))
                                                .executes(context -> give(context.getSource(),
                                                        StringArgumentType.getString(context, "id"),
                                                        java.util.List.of(context.getSource().getPlayerOrException())))
                                                .then(Commands.argument("target", EntityArgument.players())
                                                        .executes(context -> give(context.getSource(),
                                                                StringArgumentType.getString(context, "id"),
                                                                EntityArgument.getPlayers(context, "target"))))))));
    }

    private static int list(CommandSourceStack source) {
        java.util.List<GuideBookDefinition> guides = GuideBookRegistry.visibleGuideBooks();
        int hidden = Math.max(0, GuideBookRegistry.guideBooks().size() - guides.size());
        source.sendSuccess(() -> Component.translatable("command.echowiki.guidebook.list.header", guides.size(), hidden)
                .withStyle(ChatFormatting.AQUA), false);
        if (guides.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.echowiki.guidebook.list.empty")
                    .withStyle(ChatFormatting.YELLOW), false);
            return 0;
        }
        guides.stream().limit(24).forEach(guide -> source.sendSuccess(
                () -> Component.translatable("command.echowiki.guidebook.list.entry",
                        GuideBookLabels.shortId(guide.id()), guide.id(), guide.title()), false));
        if (guides.size() > 24) {
            source.sendSuccess(() -> Component.translatable("command.echowiki.guidebook.list.more", guides.size() - 24), false);
        }
        return guides.size();
    }

    private static int give(CommandSourceStack source, String rawId, Collection<ServerPlayer> targets) {
        Identifier id = parse(rawId);
        GuideBookDefinition guide = GuideBookRegistry.visibleGuideBook(id).orElse(null);
        if (guide == null) {
            source.sendFailure(Component.translatable("command.echowiki.guidebook.give.unknown", rawId));
            return 0;
        }
        int count = 0;
        for (ServerPlayer target : targets) {
            ItemStack stack = GuideBookStacks.stackFor(guide);
            if (!target.addItem(stack.copy())) {
                target.drop(stack.copy(), false);
            }
            count++;
        }
        int delivered = count;
        source.sendSuccess(() -> Component.translatable("command.echowiki.guidebook.give.success",
                guide.title(), GuideBookLabels.shortId(guide.id()), delivered), true);
        return count;
    }

    private static java.util.List<String> visibleIdSuggestions() {
        return GuideBookRegistry.visibleGuideBooks().stream()
                .flatMap(guide -> java.util.stream.Stream.of(guide.id().toString(), GuideBookLabels.shortId(guide.id())))
                .distinct()
                .toList();
    }

    private static Identifier parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            String clean = raw.strip();
            return clean.contains(":")
                    ? Identifier.parse(clean)
                    : Identifier.fromNamespaceAndPath("echowiki", clean);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static CommandDispatcher<CommandSourceStack> dispatcher(Object event) {
        if (event == null) {
            return null;
        }
        try {
            Object dispatcher = event.getClass().getMethod("getDispatcher").invoke(event);
            return dispatcher instanceof CommandDispatcher<?> value
                    ? (CommandDispatcher<CommandSourceStack>) value
                    : null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }
}
