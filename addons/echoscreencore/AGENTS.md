# ScreenCore Authoring Rules

This is the first file to read before editing ScreenCore EUI. ScreenCore is not browser HTML/CSS. Use the real ScreenCore tags, styles, diagnostics, and reference pages in this module.

Required references:

- `docs/SCREENCORE_FEATURE_HUB.md`
- `docs/AI_SCREENCORE_AUTHORING.md`
- `docs/SCREENCORE_LAYOUT_CONTRACT.md`
- `docs/SCREENCORE_EXAMPLE_CATALOG.md`
- `docs/SCREENCORE_DIAGNOSTICS.md`
- `docs/screencore_ai_contract.json`

Hard rules:

1. Start from a ScreenCore reference page or `echoscreencore:sc_app_shell`.
2. Use real ScreenCore tags only; do not invent browser, CSS, or HTML behavior.
3. Every multi-column grid must include `stack-below`.
4. Dense or repeated content must have one clear `scroll` owner.
5. Do not put scroll regions inside scroll regions.
6. Do not use fixed heights above `260px` except for scroll regions.
7. Rows must use fixed-width badges/icons and one flexible copy column.
8. Row text must use `max-lines`, `wrap`, or `overflow: hidden`.
9. Every list must include an `empty-state`.
10. Every action must be a built-in ScreenCore action or registered addon action.
11. Every copied example must come from a real `.eui.xml` or `.eui.css` resource.
12. Validate layouts at `360x240`, `854x480`, and `1280x720`.

Useful commands:

```text
/echoscreencore hub
/echoscreencore workbench
/echoscreencore workbench echoscreencore:reference_list_detail
/echoscreencore inspect page echoscreencore:reference_list_detail 360 240
/echoscreencore validate references
```

