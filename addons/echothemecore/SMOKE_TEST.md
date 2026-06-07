# ECHO Stack 1.3.0 CyberGlass Default Smoke Test

1. Run `.\gradlew :echothemecore:build`.
2. Attempt `.\gradlew buildEchoWorkspace -PechoAddonSet=all`.
3. Launch a dev client with ThemeCore enabled.
4. Confirm the log reports `ECHO ThemeCore 1.3.0 online`.
5. Run `/echo_theme list`.
6. Run `/echo_theme current`.
7. Confirm `echothemecore:cyberglass` is default and fallback.
8. Open Terminal with no saved Terminal theme and confirm CyberGlass is selected.
9. Set a valid non-CyberGlass Terminal theme, restart the client, and confirm the saved selection is preserved.
10. Run `/echo_theme set echothemecore:nexus`.
11. Run `/echo_theme reset` and confirm CyberGlass is active again.
12. Run `/reload`, then `/echo_theme list` again.
13. Run `/echo_theme preview echothemecore:cyberglass` and `/echo_theme preview echothemecore:nexus`.
14. Run `/echo_theme player clear <player>` after setting a player override and confirm the global CyberGlass fallback is restored.
15. Run `/echo_theme preset list`.
16. Run `/echo_theme preset preview echothemecore:cyberglass_terminal_boot`.
17. Run `/echo_theme visual current`.
18. Run `/echo_theme visual test terminal`; if RenderCore is absent, confirm the command reports unavailable.
19. Run `/echo_theme vanilla current`.
20. Open title, pause, options, inventory, chest/container, creative inventory, tooltips, toasts, hotbar, and boss bar.
21. Confirm CyberGlass vanilla UI accents do not cover slot interiors, move slots/widgets, or reduce text contrast.
22. Confirm HoloMap, Lens, RenderCore, SignalOS, SoundCore, Blockworks, and RuntimeGuard use CyberGlass/ThemeCore styling or visual settings when present and keep their existing fallbacks when ThemeCore is absent.
23. Confirm dedicated server startup does not load `com.knoxhack.echothemecore.client`.
24. Run `python tools/echo-themeforge/themeforge.py prompts`.
25. Run `python tools/echo-themeforge/themeforge.py validate --theme cyberglass --strict`.
26. Run `python tools/echo-themeforge/themeforge.py validate --theme nexus --strict`.
27. Run `python tools/echo-themeforge/themeforge.py report`.
28. Confirm `tools/echo-themeforge/generated/reports/missing_assets.md` is readable.
29. Confirm ThemeCore theme data and assets contain no forbidden legacy CRT line-overlay terms or files.
