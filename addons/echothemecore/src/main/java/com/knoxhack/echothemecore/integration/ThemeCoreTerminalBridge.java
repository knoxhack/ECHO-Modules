package com.knoxhack.echothemecore.integration;

import com.knoxhack.echothemecore.EchoThemeCore;
import com.knoxhack.echothemecore.api.EchoTheme;
import com.knoxhack.echothemecore.api.EchoThemeColors;
import com.knoxhack.echothemecore.api.EchoThemeTextureKey;
import com.knoxhack.echothemecore.content.ThemeRegistry;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import net.minecraft.resources.Identifier;

public final class ThemeCoreTerminalBridge {
    private static final String TERMINAL_MODID = "echoterminal";
    private static boolean registered = false;

    private ThemeCoreTerminalBridge() {
    }

    public static boolean isTerminalLoaded() {
        return EchoRuntimeModules.isLoaded(TERMINAL_MODID);
    }

    public static void registerIfAvailable() {
        if (!isTerminalLoaded()) {
            return;
        }
        try {
            int registeredCount = 0;
            for (EchoTheme theme : ThemeRegistry.listPublicThemes()) {
                if (registerTerminalTheme(theme)) {
                    registeredCount++;
                }
            }
            Identifier previousDefault = com.knoxhack.echoterminal.api.theme.TerminalThemeRegistry.defaultThemeId();
            boolean defaulted = com.knoxhack.echoterminal.api.theme.TerminalThemeRegistry.setDefaultTheme(ThemeRegistry.ECHO_PLATFORM_ID);
            if (registeredCount > 0 && !registered) {
                registered = true;
                EchoThemeCore.LOGGER.info("ECHO ThemeCore registered {} ThemeCore TerminalTheme adapters.", registeredCount);
            }
            if (defaulted && !ThemeRegistry.ECHO_PLATFORM_ID.equals(previousDefault)) {
                EchoThemeCore.LOGGER.info("ECHO ThemeCore set ECHO Platform as the Terminal default theme.");
            }
        } catch (Exception | LinkageError e) {
            EchoThemeCore.LOGGER.warn("Could not register ThemeCore TerminalTheme adapters: {}", e.getMessage());
        }
    }

    public static void syncClientTheme(Identifier themeId) {
        if (!isTerminalLoaded()) {
            return;
        }
        try {
            EchoTheme theme = ThemeRegistry.get(ThemeRegistry.resolveAlias(themeId));
            registerTerminalTheme(theme);
            com.knoxhack.echoterminal.api.theme.TerminalThemeRegistry.setDefaultTheme(theme.id());
            selectTerminalClientTheme(theme.id());
        } catch (Exception | LinkageError e) {
            EchoThemeCore.LOGGER.debug("Could not sync Terminal to ThemeCore client theme {}.", themeId, e);
        }
    }

    private static boolean registerTerminalTheme(EchoTheme theme) {
        if (theme == null || theme.id() == null
                || com.knoxhack.echoterminal.api.theme.TerminalThemeRegistry.contains(theme.id())) {
            return false;
        }
        com.knoxhack.echoterminal.api.theme.TerminalThemeRegistry.register(buildTheme(theme));
        return true;
    }

    private static void selectTerminalClientTheme(Identifier themeId) throws ReflectiveOperationException {
        Class<?> options = Class.forName("com.knoxhack.echoterminal.client.screen.TerminalClientOptions");
        options.getMethod("selectTheme", Identifier.class).invoke(null, themeId);
    }

    private static com.knoxhack.echoterminal.api.theme.TerminalTheme buildTheme(EchoTheme theme) {
        EchoThemeColors c = theme.colors();
        com.knoxhack.echoterminal.api.theme.TerminalThemeTokens.Colors colors =
            new com.knoxhack.echoterminal.api.theme.TerminalThemeTokens.Colors(
                c.background(), c.panel(), c.panelAlt(),
                c.panel(), c.panelAlt(),
                c.glass(), c.selection(),
                c.text(), c.mutedText(),
                c.primary(), c.secondary(),
                c.success(), c.warning(), c.error(),
                c.glow()
            );
        com.knoxhack.echoterminal.api.theme.TerminalThemeTokens.Panels panels =
            new com.knoxhack.echoterminal.api.theme.TerminalThemeTokens.Panels(
                c.panel(), c.panelAlt(), c.glass(),
                c.selection(), c.glass(), c.locked(),
                c.panel(), 0.68F
            );
        com.knoxhack.echoterminal.api.theme.TerminalThemeTokens.Borders borders =
            new com.knoxhack.echoterminal.api.theme.TerminalThemeTokens.Borders(
                c.borderSoft(), c.border(), c.glow(),
                c.primary(), c.locked(), c.glow()
            );
        com.knoxhack.echoterminal.api.theme.TerminalThemeTokens.Assets assets =
            new com.knoxhack.echoterminal.api.theme.TerminalThemeTokens.Assets(
                theme.uiAssets().backgroundTexture(),
                module(theme, EchoThemeTextureKey.TERMINAL_PANEL, theme.uiAssets().panelTexture()),
                theme.uiAssets().missionCardSelectedTexture(),
                theme.uiAssets().buttonHoverTexture(),
                theme.uiAssets().edgeGlow(),
                module(theme, EchoThemeTextureKey.TERMINAL_BUTTON, theme.uiAssets().buttonTexture()),
                themeTexture(theme, "rendercore/terminal_boot_effect_reference"),
                theme.uiAssets().missionCardSelectedTexture(),
                theme.uiAssets().edgeGlow()
            );
        com.knoxhack.echoterminal.api.theme.TerminalThemeTokens tokens =
            new com.knoxhack.echoterminal.api.theme.TerminalThemeTokens(
                colors,
                null,
                panels,
                borders,
                new com.knoxhack.echoterminal.api.theme.TerminalThemeTokens.Prompt(c.primary(), c.text(), c.warning(), c.error()),
                new com.knoxhack.echoterminal.api.theme.TerminalThemeTokens.Output(c.text(), c.mutedText(), c.success(), c.warning(), c.error()),
                new com.knoxhack.echoterminal.api.theme.TerminalThemeTokens.States(c.success(), c.primary(), c.secondary(), c.locked(), c.warning(), c.success(), c.error()),
                new com.knoxhack.echoterminal.api.theme.TerminalThemeTokens.Dividers(c.borderSoft(), c.border(), c.glow(), EchoThemeColors.withAlpha(c.primary(), 42)),
                new com.knoxhack.echoterminal.api.theme.TerminalThemeTokens.Effects(false, true, true, EchoThemeColors.withAlpha(c.background(), 205), EchoThemeColors.withAlpha(c.glow(), 112)),
                assets
            );
        com.knoxhack.echoterminal.api.theme.TerminalIconSet icons = icons(theme);
        return com.knoxhack.echoterminal.api.theme.TerminalTheme.builder(
                theme.id(),
                theme.displayName()
            )
            .tokens(tokens)
            .icons(icons)
            .fallbackChapterStyle(chapter(theme, theme.id().getPath(), theme.displayName(), c.primary(), c.secondary(), icons))
            .chapterStyle(chapter(theme, "minecraft", "Baseline", c.primary(), c.success(), icons))
            .chapterStyle(chapter(theme, "echoashfallprotocol", "Ashfall", c.warning(), c.error(), icons))
            .chapterStyle(chapter(theme, "echoindustrialnexus", "Industrial Nexus", c.primary(), c.warning(), icons))
            .chapterStyle(chapter(theme, "echonexusprotocol", "Nexus Protocol", c.secondary(), c.accent(), icons))
            .chapterStyle(chapter(theme, "echoorbitalremnants", "Orbital Remnants", c.primary(), c.secondary(), icons))
            .chapterStyle(chapter(theme, "echostationfall", "Stationfall", c.error(), c.warning(), icons))
            .chapterStyle(chapter(theme, "echoblackboxprotocol", "Blackbox Protocol", c.accent(), c.mutedText(), icons))
            .build();
    }

    private static Identifier module(EchoTheme theme, EchoThemeTextureKey key, Identifier fallback) {
        return theme.moduleTexture(key).orElse(fallback);
    }

    private static com.knoxhack.echoterminal.api.theme.TerminalChapterStyle chapter(
        EchoTheme theme,
        String key,
        String displayName,
        int accent,
        int secondary,
        com.knoxhack.echoterminal.api.theme.TerminalIconSet icons
    ) {
        return com.knoxhack.echoterminal.api.theme.TerminalChapterStyle.builder(key, displayName)
            .colors(accent, secondary)
            .banner(themeTexture(theme, "mission_card_selected"))
            .panel(themeTexture(theme, "mission_card_selected"))
            .border(themeTexture(theme, "edge_glow"))
            .icons(icons)
            .build();
    }

    private static com.knoxhack.echoterminal.api.theme.TerminalIconSet icons(EchoTheme theme) {
        com.knoxhack.echoterminal.api.theme.TerminalIconSet.Builder builder =
            com.knoxhack.echoterminal.api.theme.TerminalIconSet.builder()
                .fallback(icon(theme, "theme"));
        for (com.knoxhack.echoterminal.api.theme.TerminalIconKey key
            : com.knoxhack.echoterminal.api.theme.BuiltinTerminalThemes.defaultIcons().icons().keySet()) {
            builder.icon(key, iconFor(theme, key));
        }
        builder.icon(com.knoxhack.echoterminal.api.theme.TerminalIconKey.chapter("echoashfallprotocol"), icon(theme, "core"))
            .icon(com.knoxhack.echoterminal.api.theme.TerminalIconKey.chapter("echoindustrialnexus"), icon(theme, "industrial"))
            .icon(com.knoxhack.echoterminal.api.theme.TerminalIconKey.chapter("echonexusprotocol"), icon(theme, "nexus"))
            .icon(com.knoxhack.echoterminal.api.theme.TerminalIconKey.chapter("echoorbitalremnants"), icon(theme, "orbital"))
            .icon(com.knoxhack.echoterminal.api.theme.TerminalIconKey.chapter("echostationfall"), icon(theme, "blackbox"))
            .icon(com.knoxhack.echoterminal.api.theme.TerminalIconKey.chapter("echoblackboxprotocol"), icon(theme, "blackbox"));
        return builder.build();
    }

    private static Identifier iconFor(EchoTheme theme, com.knoxhack.echoterminal.api.theme.TerminalIconKey key) {
        String category = key.category();
        String name = key.name();
        if ("group".equals(category)) {
            if (name.contains("nexus") || name.contains("endgame")) {
                return icon(theme, "nexus");
            }
            if (name.contains("orbital")) {
                return icon(theme, "orbital");
            }
            if (name.contains("system")) {
                return icon(theme, "runtime");
            }
            if (name.contains("chapter") || name.contains("addon")) {
                return icon(theme, "index");
            }
            return icon(theme, "terminal");
        }
        if ("page".equals(category)) {
            if (name.contains("route") || name.contains("map")) {
                return icon(theme, "holomap");
            }
            if (name.contains("vital") || name.contains("scan")) {
                return icon(theme, "lens");
            }
            if (name.contains("reward")) {
                return icon(theme, "missions");
            }
            return icon(theme, "index");
        }
        if ("action".equals(category)) {
            return name.contains("scan") ? icon(theme, "lens") : icon(theme, "missions");
        }
        if ("state".equals(category)) {
            if (name.contains("locked") || name.contains("blocker")) {
                return icon(theme, "blackbox");
            }
            if (name.contains("warning")) {
                return icon(theme, "runtime");
            }
            return icon(theme, "core");
        }
        if ("mission_category".equals(category)) {
            if (name.contains("combat")) {
                return icon(theme, "armory");
            }
            if (name.contains("exploration")) {
                return icon(theme, "holomap");
            }
            if (name.contains("tech") || name.contains("craft")) {
                return icon(theme, "industrial");
            }
            return icon(theme, "missions");
        }
        return switch (category) {
            case "reward" -> icon(theme, "missions");
            case "chapter" -> icon(theme, "index");
            case "theme" -> icon(theme, "theme");
            default -> icon(theme, "theme");
        };
    }

    private static Identifier icon(EchoTheme theme, String name) {
        return themeTexture(theme, "icons/icon_" + name);
    }

    private static Identifier themeTexture(EchoTheme theme, String path) {
        String assetTheme = theme.metadata().getOrDefault("asset_theme", theme.id().getPath());
        return Identifier.fromNamespaceAndPath(EchoThemeCore.MODID,
                "textures/gui/themes/" + assetTheme + "/" + path + ".png");
    }
}
