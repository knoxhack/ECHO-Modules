# Cyberglass ScreenCore Kit

ThemeCore owns a reusable ScreenCore-facing Cyberglass kit at `echothemecore:cyberglass_kit`. Addons can opt in by adding that style id to an EUI page or manifest entry.

```xml
<page id="example" title="Example" styles="echothemecore:cyberglass_kit">
  <screen-shell class="cyberglass-shell">
    <panel class="cyberglass-panel" title="Shared Panel">
      <text value="This page uses ThemeCore surfaces and controls."/>
      <button variant="primary">Run Action</button>
    </panel>
  </screen-shell>
</page>
```

The stylesheet uses ScreenCore's `theme-texture(...)` function for transparent PNG layers. Missing ThemeCore installs or missing texture tokens resolve to no texture with diagnostics, so the page keeps rendering with color and border fallbacks.

The shared kit styles common ScreenCore surfaces (`screen-shell`, `app-shell`, `panel`, `app-header`, `app-content`, `app-footer`, `detail-panel`, `inspector-panel`) and controls (`button`, `input-text`, `select`, `list-row`, `nav-item`, `holomap-mode-button`, `status-chip`, `progress-bar`). Page-specific styles should be listed after `echothemecore:cyberglass_kit` so addons can override layout and domain-specific details.

Interactive states are supported through both pseudo selectors and ScreenCore state attributes where available, including `:hover`, `[hovered]`, `:focused`, `[focused]`, `[selected]`, `[active]`, and `[disabled]`. These states change color, texture, and glow only; they should not alter control dimensions.

Available shared texture tokens include:

- `screencore.surface.base`
- `screencore.surface.raised`
- `screencore.surface.floating`
- `screencore.button`
- `screencore.button.hover`
- `screencore.status_chip`
- `screencore.progress_bar`
- `screencore.focus_ring`
- `screencore.corner_cuts`
- `screencore.edge_rails`
- `screencore.panel_sheen`
- `screencore.micro_ticks`

Reusable component templates are available under the `echothemecore` namespace:

- `echothemecore:cyberglass_panel_shell`
- `echothemecore:cyberglass_action_button_row`
- `echothemecore:cyberglass_status_chip_row`
- `echothemecore:cyberglass_confirm_dialog`
- `echothemecore:cyberglass_empty_state`

Example include:

```xml
<component src="echothemecore:cyberglass_status_chip_row" label="Connection" status="ready" value="ONLINE"/>
```
