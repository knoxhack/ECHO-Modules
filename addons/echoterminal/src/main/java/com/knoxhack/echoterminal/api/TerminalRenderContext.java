package com.knoxhack.echoterminal.api;

import com.knoxhack.echoterminal.network.TerminalActionPacket;
import com.knoxhack.echonetcore.client.EchoNetClientActions;
import com.knoxhack.echoterminal.api.theme.TerminalTheme;
import com.knoxhack.echoterminal.api.theme.TerminalThemeContext;
import com.knoxhack.echoterminal.api.theme.TerminalThemeRegistry;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public record TerminalRenderContext(
        Minecraft minecraft,
        Player player,
        int screenWidth,
        int screenHeight,
        int contentX,
        int contentY,
        int contentWidth,
        int contentHeight,
        int scrollY,
        Consumer<Identifier> tabNavigator,
        Predicate<Identifier> tabAvailability,
        TerminalTheme theme,
        TerminalThemeContext themeContext) {
    public TerminalRenderContext {
        tabNavigator = tabNavigator == null ? ignored -> { } : tabNavigator;
        tabAvailability = tabAvailability == null ? ignored -> false : tabAvailability;
        theme = theme == null ? TerminalThemeRegistry.defaultTheme() : theme;
        themeContext = themeContext == null ? TerminalThemeContext.empty() : themeContext;
    }

    public TerminalRenderContext(
            Minecraft minecraft,
            Player player,
            int screenWidth,
            int screenHeight,
            int contentX,
            int contentY,
            int contentWidth,
            int contentHeight,
            int scrollY,
            Consumer<Identifier> tabNavigator,
            Predicate<Identifier> tabAvailability) {
        this(minecraft, player, screenWidth, screenHeight, contentX, contentY, contentWidth, contentHeight,
                scrollY, tabNavigator, tabAvailability, TerminalThemeRegistry.defaultTheme(), TerminalThemeContext.empty());
    }

    public void sendAction(Identifier tabId, Identifier actionId, String payload) {
        playCommandSound();
        EchoNetClientActions.sendServerboundAction(new TerminalActionPacket(tabId, actionId, payload == null ? "" : payload));
    }

    public boolean canNavigateToTab(Identifier tabId) {
        return tabId != null && tabAvailability.test(tabId);
    }

    public void navigateToTab(Identifier tabId) {
        if (canNavigateToTab(tabId)) {
            playCommandSound();
            tabNavigator.accept(tabId);
        } else {
            playRejectedSound();
        }
    }

    public TerminalRenderContext withThemeContext(TerminalThemeContext replacement) {
        return new TerminalRenderContext(minecraft, player, screenWidth, screenHeight, contentX, contentY,
                contentWidth, contentHeight, scrollY, tabNavigator, tabAvailability, theme, replacement);
    }

    public TerminalRenderContext withChapterTheme(String chapterId, String chapterTitle, String namespace) {
        TerminalThemeContext current = themeContext == null ? TerminalThemeContext.empty() : themeContext;
        return withThemeContext(new TerminalThemeContext(
                current.activeTabId(),
                current.navigationGroup(),
                chapterId,
                chapterTitle,
                namespace,
                current.tick(),
                current.visualAssets(),
                current.reducedMotion()));
    }

    public void playCommandSound() {
        if (minecraft == null) {
            return;
        }
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(TerminalThemedSounds.click(), 1.25F, 0.45F));
    }

    public void playRejectedSound() {
        if (minecraft == null) {
            return;
        }
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(TerminalThemedSounds.error(), 0.7F, 0.45F));
    }
}
