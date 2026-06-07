package com.knoxhack.echoterminal.api.theme;

import java.util.Objects;
import net.minecraft.resources.Identifier;

public record TerminalThemeTokens(
        Colors colors,
        Typography typography,
        Panels panels,
        Borders borders,
        Prompt prompt,
        Output output,
        States states,
        Dividers dividers,
        Effects effects,
        Assets assets) {
    public TerminalThemeTokens {
        colors = Objects.requireNonNull(colors, "Theme colors are required.");
        typography = Objects.requireNonNullElseGet(typography, Typography::defaults);
        panels = Objects.requireNonNullElseGet(panels, Panels::defaults);
        borders = Objects.requireNonNullElseGet(borders, Borders::defaults);
        prompt = Objects.requireNonNullElseGet(prompt, Prompt::defaults);
        output = Objects.requireNonNullElseGet(output, Output::defaults);
        states = Objects.requireNonNullElseGet(states, States::defaults);
        dividers = Objects.requireNonNullElseGet(dividers, Dividers::defaults);
        effects = Objects.requireNonNullElseGet(effects, Effects::defaults);
        assets = Objects.requireNonNullElseGet(assets, Assets::empty);
    }

    public record Colors(
            int background,
            int shell,
            int content,
            int panel,
            int panelDark,
            int row,
            int rowSelected,
            int text,
            int muted,
            int accent,
            int accentDim,
            int success,
            int warning,
            int danger,
            int info) {
    }

    public record Typography(
            boolean shadowText,
            int lineHeight,
            int compactLineHeight,
            int headerLineHeight) {
        public static Typography defaults() {
            return new Typography(false, 11, 9, 14);
        }
    }

    public record Panels(
            int baseFill,
            int darkFill,
            int elevatedFill,
            int selectedFill,
            int hoverFill,
            int disabledFill,
            int headerFill,
            float imageDarken) {
        public static Panels defaults() {
            return new Panels(0x6610242F, 0xB6050D14, 0xCC071017, 0xFF123241,
                    0xFF102630, 0xAA11161A, 0x5E071923, 0.68F);
        }
    }

    public record Borders(
            int subtle,
            int normal,
            int strong,
            int selected,
            int disabled,
            int glow) {
        public static Borders defaults() {
            return new Borders(0x33244352, 0x5538DFF4, 0xAA66E8FF,
                    0xCC66E8FF, 0x33244352, 0x6638DFF4);
        }
    }

    public record Prompt(int prefixColor, int commandColor, int cursorColor, int rejectedColor) {
        public static Prompt defaults() {
            return new Prompt(0xFF66E8FF, 0xFFE9FBFF, 0xFFFFD166, 0xFFFF8FA3);
        }
    }

    public record Output(int normalColor, int mutedColor, int successColor, int warningColor, int dangerColor) {
        public static Output defaults() {
            return new Output(0xFFE9FBFF, 0xFF8CA7B5, 0xFF92F7A6, 0xFFFFD166, 0xFFFF8FA3);
        }
    }

    public record States(
            int online,
            int active,
            int available,
            int locked,
            int claimable,
            int complete,
            int blocker) {
        public static States defaults() {
            return new States(0xFF92F7A6, 0xFF66E8FF, 0xFF9FD1FF,
                    0xFF8CA7B5, 0xFFFFD166, 0xFF92F7A6, 0xFFFF8FA3);
        }
    }

    public record Dividers(int line, int activeLine, int majorLine, int gridLine) {
        public static Dividers defaults() {
            return new Dividers(0x55244352, 0xFF66E8FF, 0x5538DFF4, 0x1D2E8E9D);
        }
    }

    public record Effects(boolean scanlines, boolean grid, boolean pulse, int overlayColor, int glowColor) {
        public static Effects defaults() {
            return new Effects(false, false, true, 0xC902070C, 0x6638DFF4);
        }
    }

    public record Assets(
            Identifier shellBackdrop,
            Identifier panelPlate,
            Identifier selectedPlate,
            Identifier hoverPlate,
            Identifier divider,
            Identifier promptOrnament,
            Identifier loading,
            Identifier defaultBanner,
            Identifier border) {
        public static Assets empty() {
            return new Assets(null, null, null, null, null, null, null, null, null);
        }
    }
}
