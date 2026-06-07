package com.knoxhack.echothemecore.content;

import com.knoxhack.echothemecore.EchoThemeCore;
import com.knoxhack.echothemecore.api.DistortionStyle;
import com.knoxhack.echothemecore.api.EchoTheme;
import com.knoxhack.echothemecore.api.EchoThemeBlockPalette;
import com.knoxhack.echothemecore.api.EchoThemeColors;
import com.knoxhack.echothemecore.api.EchoThemeRenderProfile;
import com.knoxhack.echothemecore.api.EchoThemeSoundProfile;
import com.knoxhack.echothemecore.api.EchoThemeTextureKey;
import com.knoxhack.echothemecore.api.EchoThemeUiAssets;
import com.knoxhack.echothemecore.api.EchoThemeVanillaUiProfile;
import com.knoxhack.echothemecore.api.HologramStyle;
import com.knoxhack.echothemecore.api.ParticleStyle;
import com.knoxhack.echothemecore.api.TransitionStyle;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

/**
 * Built-in theme presets. All presets reuse the CyberGlass texture set as a safe fallback.
 * Color palettes are distinct so each preset has a recognizable visual identity.
 */
public final class BuiltinThemes {
    private static final Identifier CYBERGLASS_ICON = Identifier.fromNamespaceAndPath(EchoThemeCore.MODID,
            "textures/gui/themes/cyberglass/icons/icon_theme.png");

    private BuiltinThemes() {
    }

    public static EchoTheme echoPlatform() {
        return buildPublic("echo_platform", "ECHO Platform",
                "Blue console ECHO Platform skin shared by Native Loader and Standalone surfaces.",
                new EchoThemeColors(
                        0xFF69D7FF, 0xFF2B8CFF, 0xFFB8FBFF, 0xFF030815,
                        0xE6071426, 0xCC0B1E38, 0x88102745, 0xFF69D7FF,
                        0xFF1E5B91, 0xFFEAF4FF, 0xFF8AA9C4, 0xFF52F2B8,
                        0xFFFFD166, 0xFFFF5E7A, 0xFF243447, 0xFF2B8CFF, 0xFF9BE7FF),
                HologramStyle.CLEAN_GRID, ParticleStyle.SOFT_GLINTS, DistortionStyle.NONE,
                "ECHO_PLATFORM_BLUE_CONSOLE", TransitionStyle.GLASS_FADE, "echo_platform", 0, "full");
    }

    public static EchoTheme defaultDark() {
        return build("default_dark", "Default Dark",
                "Clean dark ECHO UI with cyan accents.",
                new EchoThemeColors(
                        0xFF1A1A2E, 0xFF16213E, 0xFF00E5FF, 0xFF0F0F1A,
                        0xCC1E1E33, 0x8828284A, 0x881A1A2E, 0xFF3A3A5C,
                        0xFF2A2A4A, 0xFFE0E0E0, 0xFF8A8A8A, 0xFF45FFB0,
                        0xFFFFD166, 0xFFFF4D6D, 0xFF3B4652, 0xFF00E5FF, 0xFFB44CFF),
                HologramStyle.FLAT_GRID, ParticleStyle.NONE, DistortionStyle.NONE,
                "FLAT_DARK", TransitionStyle.FADE);
    }

    public static EchoTheme vanillaBook() {
        return build("vanilla_book", "Vanilla Book",
                "Classic parchment and leather book aesthetic.",
                new EchoThemeColors(
                        0xFFF3E5AB, 0xFFDEB887, 0xFF8B4513, 0xFFF5F5DC,
                        0xFFF3E5AB, 0xFFE8D4A2, 0x88F5F5DC, 0xFF8B4513,
                        0xFFCD853F, 0xFF3E2723, 0xFF8D6E63, 0xFF2E7D32,
                        0xFFFF8F00, 0xFFC62828, 0xFF8D6E63, 0xFF8B4513, 0xFFD2691E),
                HologramStyle.NONE, ParticleStyle.NONE, DistortionStyle.NONE,
                "PARCHMENT", TransitionStyle.FADE);
    }

    public static EchoTheme techConsole() {
        return buildPublic("cyberconsole", "CyberConsole",
                "Amber phosphor on dark green terminal.",
                new EchoThemeColors(
                        0xFF001100, 0xFF002200, 0xFF33FF33, 0xFF000A00,
                        0xCC003300, 0x88004400, 0x88001100, 0xFF33FF33,
                        0xFF1A801A, 0xFF33FF33, 0xFF1A801A, 0xFF33FF33,
                        0xFFFFFF00, 0xFFFF3333, 0xFF1A401A, 0xFF33FF33, 0xFF66FF66),
                HologramStyle.CLEAN_GRID, ParticleStyle.NONE, DistortionStyle.NONE,
                "TECH_GREEN", TransitionStyle.GLITCH_CUT, "cyberconsole", 20, "hybrid");
    }

    public static EchoTheme magicGrimoire() {
        return buildPublic("magic", "Magic",
                "Arcane purples and gold runes.",
                new EchoThemeColors(
                        0xFF1A0A2E, 0xFF2E1A47, 0xFFFFD700, 0xFF120524,
                        0xCC2A1A4A, 0x883D2A6A, 0x881A0A2E, 0xFFFFD700,
                        0xFF8B7DB8, 0xFFE6E0F0, 0xFF9B8BB8, 0xFF50C878,
                        0xFFFFA500, 0xFFFF4444, 0xFF5A4A78, 0xFFFFD700, 0xFFB44CFF),
                HologramStyle.RUNE_FIELD, ParticleStyle.SOFT_GLINTS, DistortionStyle.NONE,
                "RUNE_PURPLE", TransitionStyle.FADE, "magic", 40, "full");
    }

    public static EchoTheme skyblockClean() {
        return build("skyblock_clean", "Skyblock Clean",
                "Bright, minimal sky and cloud palette.",
                new EchoThemeColors(
                        0xFF87CEEB, 0xFFB0E0E6, 0xFF1E90FF, 0xFFF0F8FF,
                        0xCCFFFFFF, 0x88E0FFFF, 0x88F0F8FF, 0xFF1E90FF,
                        0xFF4682B4, 0xFF2F4F4F, 0xFF708090, 0xFF32CD32,
                        0xFFFFA500, 0xFFFF4500, 0xFF778899, 0xFF1E90FF, 0xFF00BFFF),
                HologramStyle.FLAT_GRID, ParticleStyle.NONE, DistortionStyle.NONE,
                "SKY_MINIMAL", TransitionStyle.FADE);
    }

    public static EchoTheme rpgJournal() {
        return build("rpg_journal", "RPG Journal",
                "Weathered paper with ink and wax seal tones.",
                new EchoThemeColors(
                        0xFFF5E6CC, 0xFFE8D5B5, 0xFF8B0000, 0xFFFAEBD7,
                        0xCCF5E6CC, 0x88E8D5B5, 0x88FAEBD7, 0xFF8B0000,
                        0xFFCD853F, 0xFF2F1810, 0xFF8B7355, 0xFF228B22,
                        0xFFDAA520, 0xFF8B0000, 0xFF8B7355, 0xFF8B0000, 0xFFD2691E),
                HologramStyle.NONE, ParticleStyle.NONE, DistortionStyle.NONE,
                "PARCHMENT_RED", TransitionStyle.FADE);
    }

    public static EchoTheme industrialPanel() {
        return build("industrial_panel", "Industrial Panel",
                "Safety yellow and steel gray warning panels.",
                new EchoThemeColors(
                        0xFF2A2A2A, 0xFF3D3D3D, 0xFFFFCC00, 0xFF1A1A1A,
                        0xCC3D3D3D, 0x885C5C5C, 0x882A2A2A, 0xFFFFCC00,
                        0xFF8A8A8A, 0xFFE0E0E0, 0xFF9E9E9E, 0xFF00E676,
                        0xFFFF6D00, 0xFFFF1744, 0xFF616161, 0xFFFFCC00, 0xFFFF6D00),
                HologramStyle.FLAT_GRID, ParticleStyle.NONE, DistortionStyle.NONE,
                "STEEL_YELLOW", TransitionStyle.GLITCH_CUT);
    }

    public static EchoTheme horrorMonitor() {
        return build("horror_monitor", "Horror Monitor",
                "Flickering red-black interference and decay.",
                new EchoThemeColors(
                        0xFF0A0000, 0xFF1A0000, 0xFFFF0000, 0xFF050000,
                        0xCC1A0000, 0x882A0000, 0x880A0000, 0xFFFF0000,
                        0xFF8B0000, 0xFFFFAAAA, 0xFFAA4444, 0xFF00FF00,
                        0xFFFFFF00, 0xFFFF0000, 0xFF550000, 0xFFFF0000, 0xFFAA0000),
                HologramStyle.BLACKBOX_ARCHIVE, ParticleStyle.NONE, DistortionStyle.NOISE_STATIC,
                "HORROR_RED", TransitionStyle.GLITCH_CUT);
    }

    public static EchoTheme cyberglass() {
        return ThemeRegistry.getCyberGlassBuiltin();
    }

    public static EchoTheme ashfall() {
        return buildPublic("ashfall", "Ashfall",
                "Ash-gray survival palette with hazard orange.",
                new EchoThemeColors(
                        0xFF1E1E1E, 0xFF2A2A2A, 0xFFFF6600, 0xFF141414,
                        0xCC2E2E2E, 0x883D3D3D, 0x881E1E1E, 0xFFFF6600,
                        0xFF8A8A8A, 0xFFE0E0E0, 0xFF8A8A8A, 0xFF44FF88,
                        0xFFFFCC00, 0xFFFF3333, 0xFF555555, 0xFFFF6600, 0xFFFFAA00),
                HologramStyle.FLAT_GRID, ParticleStyle.SOFT_GLINTS, DistortionStyle.NONE,
                "ASH_GRAY", TransitionStyle.FADE, "ashfall", 30, "hybrid");
    }

    public static EchoTheme prime() {
        return buildPublic("prime", "Prime",
                "Stable survival interface with field teal, signal amber, and grounded slate panels.",
                new EchoThemeColors(
                        0xFF2BE8C8, 0xFF78A7C8, 0xFFFFD166, 0xFF08100F,
                        0xCC10211F, 0xCC1E2C35, 0x882A3D42, 0xFF2BE8C8,
                        0xFF395A63, 0xFFEAF7EF, 0xFF93A7A2, 0xFF65E68E,
                        0xFFFFD166, 0xFFFF6B6B, 0xFF334347, 0xFF2BE8C8, 0xFFFFD166),
                HologramStyle.CLEAN_GRID, ParticleStyle.SOFT_GLINTS, DistortionStyle.NONE,
                "PRIME_FIELD_CONSOLE", TransitionStyle.FADE, "prime", 10, "full");
    }

    public static List<EchoTheme> all() {
        return List.of(
                echoPlatform(),
                defaultDark(),
                vanillaBook(),
                techConsole(),
                magicGrimoire(),
                skyblockClean(),
                rpgJournal(),
                industrialPanel(),
                horrorMonitor(),
                cyberglass(),
                ashfall(),
                prime()
        );
    }

    private static EchoTheme build(String id, String displayName, String description,
            EchoThemeColors colors, HologramStyle hologram, ParticleStyle particle,
            DistortionStyle distortion, String overlayStyle, TransitionStyle transition) {
        return build(id, displayName, description, colors, hologram, particle, distortion,
                overlayStyle, transition, id, Integer.MAX_VALUE, "overlay", false);
    }

    private static EchoTheme buildPublic(String id, String displayName, String description,
            EchoThemeColors colors, HologramStyle hologram, ParticleStyle particle,
            DistortionStyle distortion, String overlayStyle, TransitionStyle transition,
            String family, int cycleOrder, String replacementLevel) {
        return build(id, displayName, description, colors, hologram, particle, distortion,
                overlayStyle, transition, family, cycleOrder, replacementLevel, true);
    }

    private static EchoTheme build(String id, String displayName, String description,
            EchoThemeColors colors, HologramStyle hologram, ParticleStyle particle,
            DistortionStyle distortion, String overlayStyle, TransitionStyle transition,
            String family, int cycleOrder, String replacementLevel, boolean publicTheme) {
        Identifier themeId = Identifier.fromNamespaceAndPath(EchoThemeCore.MODID, id);

        // Reuse CyberGlass textures as a safe fallback for all built-in presets.
        // The color palette is what gives each theme its distinct identity.
        EchoThemeUiAssets ui = BuiltinThemeFallbackAssets.uiAssets();
        EchoThemeRenderProfile render = new EchoThemeRenderProfile(
                colors.primary(), colors.secondary(), colors.primary(), colors.accent(),
                colors.primary(), colors.secondary(), colors.glow(), colors.warning(),
                colors.success(), colors.error(), colors.secondary(),
                0.85F, 0.9F, 0.68F, 0.62F, 0.05F, 0.0F, 0.75F, 0.45F, 0.35F, 0.65F, 0.75F, 0.85F,
                hologram, particle, distortion, overlayStyle, transition
        );
        EchoThemeSoundProfile sound = BuiltinThemeFallbackAssets.soundProfile();
        EchoThemeBlockPalette blocks = BuiltinThemeFallbackAssets.blockPalette();
        EchoThemeVanillaUiProfile vanilla = BuiltinThemeFallbackAssets.vanillaProfile(colors);
        Map<EchoThemeTextureKey, Identifier> moduleTextures = BuiltinThemeFallbackAssets.moduleTextures();

        return new EchoTheme(themeId, displayName, description, colors, ui, render, sound, blocks, vanilla,
                moduleTextures, Map.ofEntries(
                    Map.entry("builtin", "true"),
                    Map.entry("source", "echothemecore"),
                    Map.entry("family", family),
                    Map.entry("public", Boolean.toString(publicTheme)),
                    Map.entry("cycle_order", Integer.toString(cycleOrder)),
                    Map.entry("icon", CYBERGLASS_ICON.toString()),
                    Map.entry("asset_theme", "cyberglass"),
                    Map.entry("replacement_level", replacementLevel),
                    Map.entry("module_tags", "terminal,signalos,index,holomap,lens,screencore,rendercore,soundcore,blockworks,vanilla_ui,hud,loading,menu,item_icon")
                ));
    }
}
