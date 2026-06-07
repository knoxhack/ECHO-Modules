package com.knoxhack.echowiki.item;

import com.knoxhack.echowiki.EchoWiki;
import com.knoxhack.echowiki.content.GuideBookDefinition;
import com.knoxhack.echowiki.content.GuideBookLabels;
import com.knoxhack.echowiki.content.GuideBookRegistry;
import com.knoxhack.echowiki.integration.GuideBookTutorialHooks;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class GuideBookItem extends Item {
    public GuideBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Identifier targetId = GuideBookStacks.guideId(stack);
        GuideBookDefinition guide = GuideBookStacks.visibleDefinition(stack).orElse(null);
        if (level.isClientSide()) {
            boolean opened = guide == null ? openClientGuideLibrary() : openClientGuide(guide.id());
            return opened ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }
        if (guide == null) {
            sendLibraryFallbackMessage(player, targetId, GuideBookStacks.definition(stack).orElse(null));
            return InteractionResult.SUCCESS_SERVER;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            GuideBookTutorialHooks.reportOpened(serverPlayer, guide);
        }
        player.sendSystemMessage(Component.translatable("message.echowiki.guide_book.opening", guide.title()));
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        Identifier targetId = GuideBookStacks.guideId(stack);
        if (targetId == null) {
            tooltip.accept(Component.translatable("tooltip.echowiki.guide_book.library.title")
                    .withStyle(ChatFormatting.AQUA));
            tooltip.accept(Component.translatable("tooltip.echowiki.guide_book.library.summary")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.accept(GuideBookLabels.availableManualsComponent(GuideBookRegistry.visibleGuideBooks().size())
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        GuideBookDefinition guide = GuideBookStacks.definition(stack).orElse(null);
        if (guide == null) {
            tooltip.accept(Component.translatable("tooltip.echowiki.guide_book.unknown.title")
                    .withStyle(ChatFormatting.YELLOW));
            tooltip.accept(Component.translatable("tooltip.echowiki.guide_book.unknown.target", targetId)
                    .withStyle(ChatFormatting.DARK_GRAY));
            tooltip.accept(Component.translatable("tooltip.echowiki.guide_book.open_library_hint")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        boolean visible = GuideBookRegistry.isVisible(guide);
        if (!visible) {
            tooltip.accept(Component.literal(guide.subtitle()).withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.translatable("tooltip.echowiki.guide_book.unavailable",
                    GuideBookLabels.moduleLabelComponent(guide.requiredModId())).withStyle(ChatFormatting.YELLOW));
            tooltip.accept(Component.translatable("tooltip.echowiki.guide_book.open_library_hint")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.accept(Component.literal(guide.subtitle()).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal(guide.summary()).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.echowiki.guide_book.module",
                GuideBookLabels.moduleLabelComponent(guide.moduleId())).withStyle(ChatFormatting.AQUA));
        tooltip.accept(GuideBookLabels.chaptersAvailableComponent(guide.allArticleIds().size())
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    private static void sendLibraryFallbackMessage(Player player, Identifier targetId, GuideBookDefinition guide) {
        if (targetId == null) {
            player.sendSystemMessage(Component.translatable("message.echowiki.guide_book.opening_library"));
            return;
        }
        if (guide == null) {
            player.sendSystemMessage(Component.translatable("message.echowiki.guide_book.missing_target", targetId));
            return;
        }
        player.sendSystemMessage(Component.translatable("message.echowiki.guide_book.unavailable",
                guide.title(), GuideBookLabels.moduleLabelComponent(guide.requiredModId())));
    }

    private static boolean openClientGuide(Identifier guideId) {
        try {
            Class<?> bridge = Class.forName("com.knoxhack.echowiki.client.WikiScreenCoreBridge");
            Method method = bridge.getMethod("openGuideBook", Identifier.class);
            Object opened = method.invoke(null, guideId);
            return opened instanceof Boolean value && value;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            EchoWiki.LOGGER.warn("ECHO: Guide book UI could not be opened for {}.", guideId, exception);
            return false;
        }
    }

    private static boolean openClientGuideLibrary() {
        try {
            Class<?> bridge = Class.forName("com.knoxhack.echowiki.client.WikiScreenCoreBridge");
            Method method = bridge.getMethod("openGuideBookLibrary");
            Object opened = method.invoke(null);
            return opened instanceof Boolean value && value;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            EchoWiki.LOGGER.warn("ECHO: Guide book library UI could not be opened.", exception);
            return false;
        }
    }
}
