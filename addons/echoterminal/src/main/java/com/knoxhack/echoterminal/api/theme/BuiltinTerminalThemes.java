package com.knoxhack.echoterminal.api.theme;

import com.knoxhack.echoterminal.EchoTerminal;
import com.knoxhack.echoterminal.api.TerminalNavigationSection;
import com.knoxhack.echoterminal.api.TerminalTabChrome;
import com.knoxhack.echoterminal.api.TerminalVisualAssets;
import net.minecraft.resources.Identifier;

public final class BuiltinTerminalThemes {
    public static final Identifier ECHO_CONSOLE =
            Identifier.fromNamespaceAndPath(EchoTerminal.MODID, "echo_console");
    public static final Identifier NEXUS_MODPACK =
            Identifier.fromNamespaceAndPath(EchoTerminal.MODID, "nexus_modpack");
    public static final Identifier CYBERGLASS =
            Identifier.fromNamespaceAndPath("echothemecore", "cyberglass");

    private BuiltinTerminalThemes() {
    }

    static void register() {
        TerminalThemeRegistry.registerBuiltin(echoConsole());
        TerminalThemeRegistry.registerBuiltin(nexusModpack());
        TerminalThemeRegistry.registerBuiltin(cyberglass());
    }

    public static TerminalTheme echoConsole() {
        TerminalThemeTokens tokens = new TerminalThemeTokens(
                new TerminalThemeTokens.Colors(
                        0xFF02070C, 0xEE050B10, 0xDD071017, 0x6610242F, 0xB6050D14,
                        0xFF0D171F, 0xFF123241, 0xFFE9FBFF, 0xFF8CA7B5,
                        0xFF66E8FF, 0xFF2E8E9D, 0xFF92F7A6, 0xFFFFD166,
                        0xFFFF8FA3, 0xFF9FD1FF),
                TerminalThemeTokens.Typography.defaults(),
                new TerminalThemeTokens.Panels(0x7010242F, 0xC2050D14, 0xD2071017,
                        0xFF123847, 0xFF102B36, 0xAA11161A, 0x6610212B, 0.70F),
                new TerminalThemeTokens.Borders(0x3D244352, 0x6638DFF4, 0xB866E8FF,
                        0xDD66E8FF, 0x33244352, 0x7038DFF4),
                TerminalThemeTokens.Prompt.defaults(),
                TerminalThemeTokens.Output.defaults(),
                TerminalThemeTokens.States.defaults(),
                TerminalThemeTokens.Dividers.defaults(),
                TerminalThemeTokens.Effects.defaults(),
                new TerminalThemeTokens.Assets(
                        echo("backgrounds/terminal_backdrop"),
                        echo("panels/panel_plate"),
                        echo("panels/panel_selected"),
                        echo("panels/panel_hover"),
                        echo("borders/divider_rune"),
                        echo("borders/prompt_ornament"),
                        echo("backgrounds/loading_portal"),
                        echo("banners/echo_console"),
                        echo("borders/panel_border")));
        TerminalIconSet icons = themeIcons("echo_console");
        return TerminalTheme.builder(ECHO_CONSOLE, "ECHO Console")
                .tokens(tokens)
                .icons(icons)
                .fallbackChapterStyle(TerminalChapterStyle.builder("echo", "ECHO")
                        .colors(tokens.colors().accent(), tokens.colors().accentDim())
                        .banner(echo("banners/echo_console"))
                        .panel(echo("panels/panel_plate"))
                        .border(echo("borders/panel_border"))
                        .icons(icons)
                        .build())
                .chapterStyle(style("echo_console", "minecraft", "Baseline", 0xFF66E8FF, 0xFF92F7A6,
                        "banners/baseline", "icons/chapter_baseline"))
                .chapterStyle(style("echo_console", "echoashfallprotocol", "Ashfall", 0xFFFF9A5A, 0xFFFF8FA3,
                        "banners/ashfall", "icons/chapter_ashfall"))
                .chapterStyle(style("echo_console", "echoindustrialnexus", "Industrial Nexus", 0xFF66E8FF, 0xFFFFD166,
                        "banners/industrial", "icons/chapter_industrial"))
                .chapterStyle(style("echo_console", "echonexusprotocol", "Nexus Protocol", 0xFF9EB0FF, 0xFF66E8FF,
                        "banners/nexus_protocol", "icons/chapter_nexus_protocol"))
                .chapterStyle(style("echo_console", "echoorbitalremnants", "Orbital Remnants", 0xFF9FD1FF, 0xFF66E8FF,
                        "banners/orbital", "icons/chapter_orbital"))
                .chapterStyle(style("echo_console", "echostationfall", "Stationfall", 0xFFFF8FA3, 0xFFFFD166,
                        "banners/stationfall", "icons/chapter_stationfall"))
                .chapterStyle(style("echo_console", "echoblackboxprotocol", "Blackbox Protocol", 0xFF9EB0FF, 0xFF8CA7B5,
                        "banners/blackbox", "icons/chapter_blackbox"))
                .build();
    }

    public static TerminalTheme nexusModpack() {
        TerminalThemeTokens tokens = new TerminalThemeTokens(
                new TerminalThemeTokens.Colors(
                        0xFF060706, 0xF20B0E0D, 0xE30B1210, 0x76301F18, 0xC90A0B09,
                        0xFF1B2018, 0xFF2D4227, 0xFFFFF6DE, 0xFFD6CAB0,
                        0xFFFFD36A, 0xFF927D49, 0xFFA7F06C, 0xFFFFC85A,
                        0xFFFF6E6E, 0xFF74D9FF),
                new TerminalThemeTokens.Typography(false, 11, 9, 14),
                new TerminalThemeTokens.Panels(0x842A1D16, 0xDF080A08, 0xD81A1D16,
                        0xFF304A2A, 0xFF2C3A22, 0xAA17120E, 0x73342519, 0.62F),
                new TerminalThemeTokens.Borders(0x526D5A37, 0x789B7B3D, 0xD6FFC85A,
                        0xD0FFE08A, 0x555F5A4E, 0x88A66CFF),
                new TerminalThemeTokens.Prompt(0xFFFFC85A, 0xFFFFF4D8, 0xFF74D9FF, 0xFFFF6E6E),
                new TerminalThemeTokens.Output(0xFFFFF4D8, 0xFFC9BFA6, 0xFFA7F06C, 0xFFFFC85A, 0xFFFF6E6E),
                new TerminalThemeTokens.States(0xFFA7F06C, 0xFFFFC85A, 0xFF74D9FF,
                        0xFF8D8475, 0xFFFFC85A, 0xFFA7F06C, 0xFFFF6E6E),
                new TerminalThemeTokens.Dividers(0x665F4A2E, 0xFFFFC85A, 0x66A66CFF, 0x184F8A5C),
                new TerminalThemeTokens.Effects(false, false, true, 0xD0060706, 0x66A66CFF),
                new TerminalThemeTokens.Assets(
                        nexus("backgrounds/terminal_backdrop"),
                        nexus("panels/panel_plate"),
                        nexus("panels/panel_selected"),
                        nexus("panels/panel_hover"),
                        nexus("borders/divider_rune"),
                        nexus("borders/prompt_ornament"),
                        nexus("backgrounds/loading_portal"),
                        nexus("banners/nexus_modpack"),
                        nexus("borders/panel_border")));
        TerminalIconSet icons = themeIcons("nexus_modpack");
        return TerminalTheme.builder(NEXUS_MODPACK, "Nexus Modpack")
                .tokens(tokens)
                .icons(icons)
                .fallbackChapterStyle(TerminalChapterStyle.builder("nexus_modpack", "Nexus Modpack")
                        .colors(tokens.colors().accent(), tokens.colors().info())
                        .banner(nexus("banners/nexus_modpack"))
                        .panel(nexus("panels/panel_plate"))
                        .border(nexus("borders/panel_border"))
                        .icons(icons)
                        .build())
                .chapterStyle(style("nexus_modpack", "minecraft", "Baseline", 0xFF8FE36B, 0xFFB8874A,
                        "banners/baseline", "icons/chapter_baseline"))
                .chapterStyle(style("nexus_modpack", "echoashfallprotocol", "Ashfall", 0xFFFF9A5A, 0xFFFF6E6E,
                        "banners/ashfall", "icons/chapter_ashfall"))
                .chapterStyle(style("nexus_modpack", "echoindustrialnexus", "Industrial Nexus", 0xFFFFC85A, 0xFF74D9FF,
                        "banners/industrial", "icons/chapter_industrial"))
                .chapterStyle(style("nexus_modpack", "echonexusprotocol", "Nexus Protocol", 0xFFA66CFF, 0xFF74D9FF,
                        "banners/nexus_protocol", "icons/chapter_nexus_protocol"))
                .chapterStyle(style("nexus_modpack", "echoorbitalremnants", "Orbital Remnants", 0xFF74D9FF, 0xFFA66CFF,
                        "banners/orbital", "icons/chapter_orbital"))
                .chapterStyle(style("nexus_modpack", "echostationfall", "Stationfall", 0xFFFF6E6E, 0xFFFFC85A,
                        "banners/stationfall", "icons/chapter_stationfall"))
                .chapterStyle(style("nexus_modpack", "echoblackboxprotocol", "Blackbox Protocol", 0xFFC6A8FF, 0xFF8D8475,
                        "banners/blackbox", "icons/chapter_blackbox"))
                .build();
    }

    public static TerminalTheme cyberglass() {
        TerminalThemeTokens tokens = new TerminalThemeTokens(
                new TerminalThemeTokens.Colors(
                        0xFF01050B, 0xE6060F1C, 0xB808182A, 0x8A10263C, 0xCC050B14,
                        0xFF0D1726, 0xFF174766, 0xFFEAF8FF, 0xFF8EAFC1,
                        0xFF00D9FF, 0xFF3D6D85, 0xFF54F4BC, 0xFFFFB84D,
                        0xFFFF5D73, 0xFFB77DFF),
                new TerminalThemeTokens.Typography(false, 11, 9, 15),
                new TerminalThemeTokens.Panels(0x73122638, 0xC2050B14, 0xA4112538,
                        0xD4164C70, 0xA6173852, 0x7A26333F, 0x4806D9FF, 0.50F),
                new TerminalThemeTokens.Borders(0x263FD8F4, 0x5538DFF4, 0xAA5CF2FF,
                        0xD600D9FF, 0x445E7180, 0x7A9B5CFF),
                new TerminalThemeTokens.Prompt(0xFF00D9FF, 0xFFEAF8FF, 0xFFB77DFF, 0xFFFF5D73),
                new TerminalThemeTokens.Output(0xFFEAF8FF, 0xFF9EB9C9, 0xFF54F4BC, 0xFFFFB84D, 0xFFFF5D73),
                new TerminalThemeTokens.States(0xFF54F4BC, 0xFFFFB84D, 0xFF00D9FF,
                        0xFF6E8290, 0xFFFFB84D, 0xFF54F4BC, 0xFFFF5D73),
                new TerminalThemeTokens.Dividers(0x3A3FD8F4, 0xFF00D9FF, 0x50B77DFF, 0x143FD8F4),
                new TerminalThemeTokens.Effects(true, false, true, 0xD4010711, 0x7400D9FF),
                new TerminalThemeTokens.Assets(
                        nexus("backgrounds/terminal_backdrop"),
                        nexus("panels/panel_plate"),
                        nexus("panels/panel_selected"),
                        nexus("panels/panel_hover"),
                        nexus("borders/divider_rune"),
                        nexus("borders/prompt_ornament"),
                        nexus("backgrounds/loading_portal"),
                        nexus("banners/nexus_modpack"),
                        nexus("borders/panel_border")));
        TerminalIconSet icons = themeIcons("nexus_modpack");
        return TerminalTheme.builder(CYBERGLASS, "Cyberglass Terminal")
                .tokens(tokens)
                .icons(icons)
                .fallbackChapterStyle(TerminalChapterStyle.builder("cyberglass", "Cyberglass")
                        .colors(tokens.colors().accent(), tokens.colors().info())
                        .banner(nexus("banners/nexus_modpack"))
                        .panel(nexus("panels/panel_plate"))
                        .border(nexus("borders/panel_border"))
                        .icons(icons)
                        .build())
                .chapterStyle(style("nexus_modpack", "minecraft", "Baseline", 0xFF00D9FF, 0xFF54F4BC,
                        "banners/baseline", "icons/chapter_baseline"))
                .chapterStyle(style("nexus_modpack", "echoashfallprotocol", "Ashfall Protocol", 0xFFFFB84D, 0xFFFF5D73,
                        "banners/ashfall", "icons/chapter_ashfall"))
                .chapterStyle(style("nexus_modpack", "echoindustrialnexus", "Industrial Nexus", 0xFF54F4BC, 0xFF00D9FF,
                        "banners/industrial", "icons/chapter_industrial"))
                .chapterStyle(style("nexus_modpack", "echonexusprotocol", "Nexus Protocol", 0xFFB77DFF, 0xFF00D9FF,
                        "banners/nexus_protocol", "icons/chapter_nexus_protocol"))
                .chapterStyle(style("nexus_modpack", "echoorbitalremnants", "Orbital Remnants", 0xFF89C8FF, 0xFFB77DFF,
                        "banners/orbital", "icons/chapter_orbital"))
                .chapterStyle(style("nexus_modpack", "echostationfall", "Stationfall", 0xFFFF5D73, 0xFFFFB84D,
                        "banners/stationfall", "icons/chapter_stationfall"))
                .chapterStyle(style("nexus_modpack", "echoblackboxprotocol", "Blackbox Protocol", 0xFFC6A8FF, 0xFF6E8290,
                        "banners/blackbox", "icons/chapter_blackbox"))
                .build();
    }

    public static TerminalIconSet defaultIcons() {
        return TerminalIconSet.builder()
                .fallback(TerminalVisualAssets.ICON_BRAND_ECHO)
                .icon(TerminalIconKey.group(TerminalNavigationSection.COMMAND.key()), TerminalVisualAssets.ICON_GROUP_PROTOCOL)
                .icon(TerminalIconKey.group(TerminalNavigationSection.CHAPTERS.key()), TerminalVisualAssets.ICON_GROUP_CHAPTERS)
                .icon(TerminalIconKey.group(TerminalNavigationSection.INTEL.key()), TerminalVisualAssets.ICON_GROUP_FIELD)
                .icon(TerminalIconKey.group(TerminalNavigationSection.INDEX.key()), TerminalVisualAssets.ICON_PAGE_SURVIVAL_INDEX)
                .icon(TerminalIconKey.group(TerminalNavigationSection.HOLOMAP.key()), TerminalVisualAssets.ICON_PAGE_ROUTE_MAP)
                .icon(TerminalIconKey.group(TerminalNavigationSection.SYSTEM.key()), TerminalVisualAssets.ICON_GROUP_SYSTEMS)
                .icon(TerminalIconKey.group(TerminalTabChrome.GROUP_PROTOCOL), TerminalVisualAssets.ICON_GROUP_PROTOCOL)
                .icon(TerminalIconKey.group(TerminalTabChrome.GROUP_CORE), TerminalVisualAssets.ICON_GROUP_FIELD)
                .icon(TerminalIconKey.group(TerminalTabChrome.GROUP_FIELD), TerminalVisualAssets.ICON_GROUP_FIELD)
                .icon(TerminalIconKey.group(TerminalTabChrome.GROUP_SYSTEMS), TerminalVisualAssets.ICON_GROUP_SYSTEMS)
                .icon(TerminalIconKey.group(TerminalTabChrome.GROUP_NEXUS), TerminalVisualAssets.ICON_GROUP_NEXUS)
                .icon(TerminalIconKey.group(TerminalTabChrome.GROUP_ENDGAME), TerminalVisualAssets.ICON_GROUP_NEXUS)
                .icon(TerminalIconKey.group(TerminalTabChrome.GROUP_ORBITAL), TerminalVisualAssets.ICON_GROUP_ORBITAL)
                .icon(TerminalIconKey.group(TerminalTabChrome.GROUP_ADDONS), TerminalVisualAssets.ICON_GROUP_CHAPTERS)
                .icon(TerminalIconKey.page("command_deck"), TerminalVisualAssets.ICON_PAGE_COMMAND_DECK)
                .icon(TerminalIconKey.page("survival_route"), TerminalVisualAssets.ICON_PAGE_SURVIVAL_INDEX)
                .icon(TerminalIconKey.page("baseline"), TerminalVisualAssets.ICON_PAGE_BASELINE)
                .icon(TerminalIconKey.page("mission_graph"), TerminalVisualAssets.ICON_PAGE_PROTOCOL_ROADMAP)
                .icon(TerminalIconKey.page("what_now"), TerminalVisualAssets.ICON_PAGE_SIGNAL_LEADS)
                .icon(TerminalIconKey.page("vitals"), TerminalVisualAssets.ICON_PAGE_VITALS_SCAN)
                .icon(TerminalIconKey.page("route_records"), TerminalVisualAssets.ICON_PAGE_ROUTE_MAP)
                .icon(TerminalIconKey.page("factions"), TerminalVisualAssets.ICON_PAGE_ROUTE_MAP)
                .icon(TerminalIconKey.page("reward_inbox"), TerminalVisualAssets.ICON_ACTION_CLAIM)
                .icon(TerminalIconKey.page("field_archive"), TerminalVisualAssets.ICON_PAGE_FIELD_ARCHIVE)
                .icon(TerminalIconKey.page("chapter_guide"), TerminalVisualAssets.ICON_PAGE_CHAPTERS)
                .icon(TerminalIconKey.page("interface_settings"), TerminalVisualAssets.ICON_GROUP_SYSTEMS)
                .icon(TerminalIconKey.action("claim"), TerminalVisualAssets.ICON_ACTION_CLAIM)
                .icon(TerminalIconKey.action("turn_in"), TerminalVisualAssets.ICON_ACTION_TURN_IN)
                .icon(TerminalIconKey.action("view"), TerminalVisualAssets.ICON_ACTION_VIEW)
                .icon(TerminalIconKey.action("scan"), TerminalVisualAssets.ICON_ACTION_SCAN)
                .icon(TerminalIconKey.action("open"), TerminalVisualAssets.ICON_ACTION_OPEN_ROADMAP)
                .icon(TerminalIconKey.action("continue"), TerminalVisualAssets.ICON_ACTION_OPEN_ROADMAP)
                .icon(TerminalIconKey.action("review"), TerminalVisualAssets.ICON_ACTION_VIEW)
                .icon(TerminalIconKey.action("resolve"), TerminalVisualAssets.ICON_STATE_NEEDED)
                .icon(TerminalIconKey.action("settings"), TerminalVisualAssets.ICON_GROUP_SYSTEMS)
                .icon(TerminalIconKey.action("theme_cycle"), TerminalVisualAssets.ICON_GROUP_SYSTEMS)
                .icon(TerminalIconKey.state("locked"), TerminalVisualAssets.ICON_STATE_LOCKED)
                .icon(TerminalIconKey.state("active"), TerminalVisualAssets.ICON_STATE_ACTIVE)
                .icon(TerminalIconKey.state("needed"), TerminalVisualAssets.ICON_STATE_NEEDED)
                .icon(TerminalIconKey.state("open"), TerminalVisualAssets.ICON_STATE_OPEN)
                .icon(TerminalIconKey.state("available"), TerminalVisualAssets.ICON_STATE_AVAILABLE)
                .icon(TerminalIconKey.state("online"), TerminalVisualAssets.ICON_STATE_ONLINE)
                .icon(TerminalIconKey.state("claimable"), TerminalVisualAssets.ICON_ACTION_CLAIM)
                .icon(TerminalIconKey.state("complete"), TerminalVisualAssets.ICON_STATE_ONLINE)
                .icon(TerminalIconKey.state("completed"), TerminalVisualAssets.ICON_STATE_ONLINE)
                .icon(TerminalIconKey.state("warning"), TerminalVisualAssets.ICON_STATE_NEEDED)
                .icon(TerminalIconKey.state("blocker"), TerminalVisualAssets.ICON_STATE_LOCKED)
                .icon(TerminalIconKey.state("empty"), TerminalVisualAssets.ICON_BRAND_ECHO)
                .icon(TerminalIconKey.state("unknown"), TerminalVisualAssets.ICON_BRAND_ECHO)
                .icon(TerminalIconKey.missionCategory("survival"), TerminalVisualAssets.MISSION_ICON_SURVIVAL)
                .icon(TerminalIconKey.missionCategory("crafting"), TerminalVisualAssets.MISSION_ICON_CRAFTING)
                .icon(TerminalIconKey.missionCategory("tech"), TerminalVisualAssets.MISSION_ICON_TECH)
                .icon(TerminalIconKey.missionCategory("exploration"), TerminalVisualAssets.MISSION_ICON_EXPLORATION)
                .icon(TerminalIconKey.missionCategory("combat"), TerminalVisualAssets.MISSION_ICON_COMBAT)
                .icon(TerminalIconKey.missionCategory("story"), TerminalVisualAssets.MISSION_ICON_STORY)
                .icon(TerminalIconKey.missionCategory("side_ops"), TerminalVisualAssets.MISSION_ICON_SIDE_OPS)
                .icon(TerminalIconKey.missionCategory("hazard"), TerminalVisualAssets.MISSION_ICON_HAZARD)
                .icon(TerminalIconKey.reward("cache"), TerminalVisualAssets.ICON_ACTION_CLAIM)
                .icon(TerminalIconKey.reward("inbox"), TerminalVisualAssets.ICON_ACTION_CLAIM)
                .icon(TerminalIconKey.theme("brand"), TerminalVisualAssets.ICON_BRAND_ECHO)
                .icon(TerminalIconKey.theme("settings"), TerminalVisualAssets.ICON_GROUP_SYSTEMS)
                .icon(TerminalIconKey.theme("cycle"), TerminalVisualAssets.ICON_GROUP_SYSTEMS)
                .icon(TerminalIconKey.fallback("unknown"), TerminalVisualAssets.ICON_BRAND_ECHO)
                .icon(TerminalIconKey.fallback("state"), TerminalVisualAssets.ICON_BRAND_ECHO)
                .icon(TerminalIconKey.fallback("action"), TerminalVisualAssets.ICON_ACTION_VIEW)
                .icon(TerminalIconKey.fallback("page"), TerminalVisualAssets.ICON_PAGE_COMMAND_DECK)
                .build();
    }

    private static TerminalIconSet themeIcons(String themeFolder) {
        TerminalIconSet.Builder builder = TerminalIconSet.builder()
                .fallback(themeAsset(themeFolder, "icons/fallback_unknown"));
        for (TerminalIconKey key : defaultIcons().icons().keySet()) {
            builder.icon(key, themeAsset(themeFolder,
                    "icons/" + key.category() + "_" + key.name().replace(':', '_').replace('/', '_')));
        }
        builder.icon(TerminalIconKey.chapter("minecraft"), themeAsset(themeFolder, "icons/chapter_baseline"))
                .icon(TerminalIconKey.chapter("echoashfallprotocol"), themeAsset(themeFolder, "icons/chapter_ashfall"))
                .icon(TerminalIconKey.chapter("echoindustrialnexus"), themeAsset(themeFolder, "icons/chapter_industrial"))
                .icon(TerminalIconKey.chapter("echonexusprotocol"), themeAsset(themeFolder, "icons/chapter_nexus_protocol"))
                .icon(TerminalIconKey.chapter("echoorbitalremnants"), themeAsset(themeFolder, "icons/chapter_orbital"))
                .icon(TerminalIconKey.chapter("echostationfall"), themeAsset(themeFolder, "icons/chapter_stationfall"))
                .icon(TerminalIconKey.chapter("echoblackboxprotocol"), themeAsset(themeFolder, "icons/chapter_blackbox"));
        return builder.build();
    }

    private static TerminalChapterStyle style(String themeFolder, String key, String displayName, int accent,
            int secondary, String banner, String chapterIcon) {
        TerminalIconSet icons = TerminalIconSet.builder()
                .icon(TerminalIconKey.chapter(key), themeAsset(themeFolder, chapterIcon))
                .build();
        return TerminalChapterStyle.builder(key, displayName)
                .colors(accent, secondary)
                .banner(themeAsset(themeFolder, banner))
                .panel(themeAsset(themeFolder, "panels/panel_" + key))
                .border(themeAsset(themeFolder, "borders/panel_border"))
                .icons(icons)
                .build();
    }

    private static Identifier echo(String path) {
        return themeAsset("echo_console", path);
    }

    private static Identifier nexus(String path) {
        return themeAsset("nexus_modpack", path);
    }

    private static Identifier themeAsset(String themeFolder, String path) {
        return Identifier.fromNamespaceAndPath(EchoTerminal.MODID,
                "textures/gui/themes/" + themeFolder + "/" + path + ".png");
    }
}
