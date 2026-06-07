# ScreenCore Diagnostics

Diagnostics include fix hints in the debug overlay, inspect command, and workbench.

| Code | Meaning | Fix |
| --- | --- | --- |
| `root_overflows_viewport` | root content exceeds viewport | use `sc_app_shell`, reduce fixed sizes, or add a scroll owner |
| `large_fixed_height` | non-scroll component uses fixed height over `260px` | move long content into `sc-scroll-region` |
| `grid_missing_stack_below` | multi-column grid has no responsive fallback | add `stack-below` |
| `nested_scroll_region` | scroll exists inside another scroll | keep one scroll owner per content path |
| `row_overflow` | row children exceed row bounds | use fixed badges/icons and flexible copy |
| `unbounded_row_text` | row text lacks a clamp/wrap/clip guard | use `sc-row-copy` or set `max-lines` and `overflow` |
| `missing_list_empty_state` | list has no empty state | add an `empty-state` child |
| `unknown_reference_page` | reference id does not resolve | add the page or fix the manifest id |
| `reference_page_failed_contract` | reference page violates the authoring contract | inspect the page at required viewports |

