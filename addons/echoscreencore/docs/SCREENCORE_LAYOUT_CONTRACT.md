# ScreenCore Layout Contract

ScreenCore pages must fit Minecraft GUI-space sizes, not desktop pixels. A wide monitor can still produce a narrow logical viewport at high GUI scale.

Required viewport checks:

- `360x240`
- `854x480`
- `1280x720`

Contract rules:

1. Root content must stay inside the viewport.
2. Multi-column grids must include `stack-below`.
3. Dense/repeated content must have one scroll owner.
4. Scroll regions must not nest.
5. Fixed heights above `260px` are reserved for scroll regions.
6. Lists must include `empty-state`.
7. Rows must reserve fixed widths for badges/icons/buttons and flexible space for copy.
8. Row copy must be bounded with `max-lines`, `wrap`, or `overflow: hidden`.

Validation commands:

```text
/echoscreencore inspect page echoscreencore:reference_list_detail 360 240
/echoscreencore inspect page echoscreencore:reference_list_detail 854 480
/echoscreencore inspect page echoscreencore:reference_list_detail 1280 720
/echoscreencore validate references
```

