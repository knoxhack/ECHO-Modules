# ScreenCore Feature Hub

Open the in-game hub with:

```text
/echoscreencore hub
```

Open the live workbench with:

```text
/echoscreencore workbench
```

ScreenCore features are documented through real resources. If an example is useful enough to copy, it must exist as a page, component, or style under `src/main/resources/assets/echoscreencore/eui/`.

## Pages And Manifest

Pages live at `assets/<namespace>/eui/pages/<page>.eui.xml` and are discovered directly or through `eui_manifest.json`.

Reference page: `echoscreencore:reference_feature_hub`

Command:

```text
/echoscreencore open echoscreencore:reference_feature_hub
```

Common mistake: linking to a page that is not in the manifest and does not exist as a resource.

Diagnostic: `unknown_reference_page`

## Styles And Tokens

Styles live at `assets/<namespace>/eui/styles/<style>.eui.css`. ScreenCore supports element, class, id, attribute, descendant, and state selectors. It does not support browser layout systems.

Reference style: `echoscreencore:screencore_app_kit`

Use supported properties only: `width`, `height`, `min-height`, `max-height`, `padding`, `gap`, `layout`, `columns`, `overflow`, `wrap`, `max-lines`, and responsive hooks.

Common mistake: adding browser CSS such as floats, flexbox, media queries, or unsupported selectors.

Diagnostic: `unknown_style_property`

## Layout

Use `row`, `column`, `grid`, `stack`, `scroll`, `section`, `panel`, `card`, and app shell tags. Multi-column grids must include `stack-below`.

Reference page: `echoscreencore:reference_three_column`

Copyable pattern:

```xml
<grid class="sc-three-column" columns="1fr 1fr 1fr" gap="8" stack-below="900">
  <section class="sc-panel" title="LEFT"/>
  <section class="sc-panel" title="MIDDLE"/>
  <section class="sc-panel" title="RIGHT"/>
</grid>
```

Common mistake: a three-column grid with no `stack-below`.

Diagnostic: `grid_missing_stack_below`

## Data And Lists

Provider-backed lists use `bind`, `item`, row `value`, and an `empty-state`.

Reference page: `echoscreencore:reference_dense_list`

Dense rows that contain meaningful title/detail copy should use `copy-block` so row text draws reliably between fixed chips, icons, and buttons. Migration notes and examples: [`COPY_BLOCK_TEXT_RECOVERY.md`](COPY_BLOCK_TEXT_RECOVERY.md).

Common mistake: a provider list without an empty state.

Diagnostic: `missing_list_empty_state`

## Actions And Navigation

Use built-ins such as `screencore.open_page`, `screencore.back`, `screencore.close`, `screencore.open_modal`, and `screencore.close_modal`, or register addon-owned actions in Java.

Reference page: `echoscreencore:reference_feature_hub`

Common mistake: inventing an action id without registering it.

Diagnostic: `action_not_registered`

## Inputs, Selects, And Dropdowns

Use `input`, `search-box`, `select`, `dropdown-menu`, `option`, and `dropdown-item`. Filtering logic belongs to the data provider or addon action.

Reference pages:

- `echoscreencore:reference_inputs`
- `echoscreencore:reference_selects_dropdowns`

## Tooltips, Dialogs, And Modals

Use `tooltip`, `dialog`, `dialog-title`, `dialog-body`, and `dialog-actions`. Open dialogs through `screencore.open_modal`.

Reference page: `echoscreencore:reference_modal_overlay`

## Accessibility And Debugging

Design with large text, high contrast, reduced clutter, and density changes in mind. The debug overlay and workbench show active breakpoint and diagnostics.

Reference page: `echoscreencore:reference_accessibility`

Commands:

```text
/echoscreencore debug on
/echoscreencore inspect page echoscreencore:reference_list_detail 360 240
/echoscreencore validate references
```
