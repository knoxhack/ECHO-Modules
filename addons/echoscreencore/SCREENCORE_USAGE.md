# ECHO: ScreenCore Usage

ScreenCore is the ECHO screen structure layer. It owns page markup, component layout, safe bindings, actions, focus, input, accessibility, and diagnostics. ThemeCore provides design tokens; RenderCore provides low-level drawing helpers.

## ScreenCore 1.0.4 Changelog

1.0.4 hardens ScreenCore for larger migrations without moving Terminal, Index, or Guide screens yet:

- Active responsive hooks: `hide-below`, `stack-below`, `collapse-below`, `compact-below`, `dense-below`, `sidebar-collapse-below`, and `detail-collapse-below`.
- Keyboard/focus routing: Tab, Shift+Tab, arrows, Enter/Space activation, Home/End, PageUp/PageDown for scroll panels, focus order, autofocus, and modal focus trap.
- Advanced selects: searchable options, clearable values, dynamic provider options, multi-select, max visible options, subtitles, menu sections, dividers, and menu item actions.
- Search/input polish: clear button, debounce window, min query length, submit action, readonly/disabled, required/min-length/pattern diagnostics.
- Page state: session page state for persisted search/filter/toggle/scroll values through reloads.
- Diagnostics: binding debug placeholders, manifest validation, page inspection, active breakpoint and overlay stack in debug overlay.
- Overlay polish: dropdown flip/clamp, tooltip edge clamp, modal click blocking, escape closes topmost overlay first.

## Pages

Create pages under:

```text
assets/<namespace>/eui/pages/<page>.eui.xml
```

Open a page from Java:

```java
EchoScreens.open(Identifier.fromNamespaceAndPath("myaddon", "my_page"), context);
```

Open a page in dev:

```text
/echoscreencore open myaddon:my_page
/echoscreencore list pages
/echoscreencore reload
/echoscreencore data invalidate
/echoscreencore state clear
/echoscreencore inspect page echoscreencore:test_page_state
/echoscreencore validate
```

## Styles

Create styles under:

```text
assets/<namespace>/eui/styles/<style>.eui.css
```

Attach page styles with a comma-separated `styles` attribute:

```xml
<page id="terminal-dashboard" styles="terminal_mock,myaddon:extra">
```

Supported selectors are intentionally small: elements, `.classes`, `#ids`, and attribute selectors such as `button[variant="primary"]` or `list-row[selected]`.

Use tokens instead of hardcoded colors where possible:

```css
card {
  padding: space(md);
  background: theme(card);
  border-color: theme(borderMuted);
  font-size: font(body);
  border-radius: radius(sm);
}
```

## Data Providers

Use `EchoDataContext` for screen-local values:

```java
EchoDataContext context = EchoDataContext.empty()
    .put("screen.title", "Terminal")
    .put("selectedMission.title", "Restore Water Supply");
```

Register shared providers for addon-owned data:

```java
EchoScreenRegistry.registerDataProvider("terminal", (context, path) -> {
    if (!path.isEmpty() && "status".equals(path.get(0))) {
        return "ready";
    }
    return null;
});
```

Markup can bind text and attributes:

```xml
<title value="{screen.title}"/>
<status-chip status="{terminal.status}">READY</status-chip>
```

Missing bindings render the context placeholder and produce diagnostics instead of crashing.

## Actions

Buttons and rows call registered Java actions. Built-ins include `noop`, `close`, `back`, `debug_toggle`, `open_page:<page_id>`, `screencore.open_page`, `screencore.back`, `screencore.close`, and `screencore.reload_page`.

```java
EchoScreenRegistry.registerAction("terminal.select", action -> {
    String id = action.actionValue();
    String type = action.param("type");
    return true;
});
```

```xml
<button action="open_page:echoscreencore:test_terminal_dashboard">Dashboard</button>
<button action="screencore.open_page" action-page="echoterminal:terminal_missions">Missions</button>
<list-row action="terminal.select" action-value="mission_001" action-param-type="mission">
```

Unknown actions are ignored safely and reported through diagnostics.

## Components

Common layout tags:

```xml
<screen-shell>
  <header title="ECHO Terminal"/>
  <grid columns="220px 1fr 280px" gap="16">
    <panel title="Navigation"/>
    <scroll/>
    <panel title="Status"/>
  </grid>
</screen-shell>
```

For dense repeated rows where copy must never disappear, prefer `copy-block` over nested `column > title + text`. It draws title/detail copy in its own component render pass and is the recommended pattern for critical list rows, mission rows, requirement/reward rows, and compact cards. See [`docs/COPY_BLOCK_TEXT_RECOVERY.md`](docs/COPY_BLOCK_TEXT_RECOVERY.md).

Lists support static rows in 1.0.1:

```xml
<list id="mission-list">
  <list-row selected="true" action="select:mission_001">
    <column>
      <title>Restore Water Supply</title>
      <text>Find a clean water source.</text>
    </column>
    <status-chip status="active">ACTIVE</status-chip>
  </list-row>
</list>
```

Lists also support provider-backed rows:

```xml
<list bind="missions.active" item="mission" selected="{selectedMission.id}">
  <list-row id="{mission.id}"
            value="{mission.id}"
            action="terminal.select_mission"
            action-value="{mission.id}">
    <column>
      <title value="{mission.title|Untitled Mission}"/>
      <text value="{mission.summary|No summary available.}"/>
    </column>
    <status-chip status="{mission.status}" value="{mission.statusLabel|INFO}"/>
  </list-row>
  <empty-state title="No missions" body="No entries matched the current filter."/>
</list>
```

`bind` resolves a collection from `EchoDataContext`. `item` names the local item context for each repeated row. The parent context is preserved, so `{selectedMission.id}` and `{mission.id}` can be used together. Supported collection values include lists, iterables, arrays, and lists of maps or view models with safe public accessors.

Generic repeaters work outside lists:

```xml
<repeat source="recipes.visible" item="recipe">
  <card action="index.select_recipe" action-value="{recipe.id}">
    <title value="{recipe.title}"/>
    <text value="{recipe.summary}"/>
  </card>
</repeat>
```

Repeated templates may use binding fallbacks with `|`:

```xml
<title value="{article.title|Untitled Article}"/>
```

Tabs switch locally without rebuilding the page:

```xml
<tabs selected="overview">
  <tab id="overview" title="Overview"><text>Overview content</text></tab>
  <tab id="rewards" title="Rewards"><text>Reward content</text></tab>
</tabs>
```

Items render real Minecraft item stacks or safe fallbacks:

```xml
<item-icon item="minecraft:iron_ingot"/>
<item-stack item="{selectedItem.id}" count="{selectedItem.count}"/>
```

## Component Templates

Reusable EUI components live under:

```text
assets/<namespace>/eui/components/<name>.eui.xml
```

Use an include for simple reusable markup:

```xml
<include src="echoscreencore:status_chip_row"
         label="Status"
         status="{system.status}"
         value="READY"/>
```

Use `component` when passing explicit params:

```xml
<component src="echoscreencore:action_card">
  <param name="title" value="Open Missions"/>
  <param name="body" value="View active objectives."/>
  <param name="action" value="screencore.open_page"/>
  <param name="page" value="echoterminal:terminal_missions"/>
</component>
```

Templates read params with fallbacks:

```xml
<card action="{param.action|noop}" action-page="{param.page|}">
  <title value="{param.title|Untitled Action}"/>
  <text value="{param.body|No description provided.}"/>
</card>
```

Named slots are supported for simple content projection:

```xml
<component src="echoscreencore:two_column_shell">
  <slot name="sidebar"><button action="noop">Dashboard</button></slot>
  <slot name="body"><text value="{screen.title}"/></slot>
</component>
```

Template parsing is cached, circular includes are blocked, and `/echoscreencore reload` clears page, style, and component-template caches.

## Responsive Layout

Breakpoints:

```text
xs=320, sm=480, md=720, lg=960, xl=1280
```

Responsive attributes accept either a pixel number or one of those names:

```xml
<grid columns="230px 1fr 280px" stack-below="md">
  <app-sidebar sidebar-collapse-below="lg" compact-below="lg"/>
  <app-content/>
  <inspector-panel detail-collapse-below="lg" hide-below="sm"/>
</grid>
```

Behavior:

- `hide-below` removes the component from layout below the threshold.
- `stack-below` makes row/grid/split-style containers lay children vertically.
- `collapse-below` keeps the component shell but hides children.
- `compact-below` and `dense-below` reduce layout gaps.
- `sidebar-collapse-below` marks sidebars compact/collapsed.
- `detail-collapse-below` tells detail/inspector panels to stack in split views.

Debug overlay shows the active breakpoint. Test pages: `test_responsive_shell`, `test_responsive_split_view`, and `test_responsive_cards`.

## Keyboard And Focus

Supported controls:

```text
Tab / Shift+Tab       focus traversal
Arrow keys            list/nav/select traversal
Enter / Space         activate focused controls
Escape                close topmost dropdown/modal, then back
Home / End            jump in lists/dropdowns/scroll focus
PageUp / PageDown     scroll focused scroll panels
```

Useful focus attributes:

```xml
<button autofocus="true" focus-order="1">Primary</button>
<list-row focusable="true" action="terminal.select_mission"/>
<dialog id="confirm" close-on-escape="true" close-on-outside="false"/>
```

Focus traversal ignores hidden and disabled components. Modals trap focus while open and return focus to the opener when closed where possible.

## Selects, Tooltips, And Modals

Dropdown/select controls support static options:

```xml
<select value="{filters.status}" on-change="terminal.set_filter">
  <option value="active">Active</option>
  <option value="ready">Ready</option>
  <option value="done">Done</option>
</select>
```

Provider-backed dynamic options use the same repeater model:

```xml
<select bind="filters.availableStatuses" item="status" value="{filters.status}" on-change="terminal.set_filter">
  <option value="{status.id}" label="{status.label}"/>
</select>
```

Searchable and multi-select examples:

```xml
<select bind="missions.statusOptions"
        item="status"
        value="{filters.status}"
        searchable="true"
        clearable="true"
        filter-mode="contains"
        max-visible-options="6"
        on-change="terminal.set_filter">
  <option value="{status.id}" label="{status.label}" subtitle="{status.description|}"/>
</select>

<select bind="tags.visible"
        item="tag"
        multi="true"
        selected-values="{filters.tags}"
        searchable="true"
        max-selected="3">
  <option value="{tag.id}" label="{tag.label}" subtitle="{tag.description}"/>
</select>
```

Dropdown menus use the same overlay:

```xml
<dropdown-menu placeholder="Actions">
  <menu-section label="Navigation"/>
  <dropdown-item value="dashboard" label="Dashboard" action="screencore.open_page" action-value="echoscreencore:test_dashboard"/>
  <divider/>
  <dropdown-item value="reset" label="Reset" action="settings.reset_accessibility" danger="true"/>
</dropdown-menu>
```

`filter-mode="provider"` is reserved for consuming addons that update provider results from the select/search action; ScreenCore does not run filtering logic outside the UI layer.

Tooltips can be attributes or wrappers:

```xml
<button tooltip="Open active objectives." action="noop">Missions</button>
<tooltip text="Requires a powered relay."><status-chip status="locked">LOCKED</status-chip></tooltip>
```

Dialogs are hidden until opened by action:

```xml
<button action="screencore.open_modal" action-target="confirm-reset">Reset</button>

<dialog id="confirm-reset" title="Reset Settings">
  <dialog-title>Reset Settings</dialog-title>
  <dialog-body><text>Reset client-local ScreenCore settings?</text></dialog-body>
  <dialog-actions>
    <button action="screencore.close_modal">Cancel</button>
    <button variant="danger" action="settings.reset_accessibility">Reset</button>
  </dialog-actions>
</dialog>
```

Overlay order is base page, dropdowns, tooltips, modals, then debug overlay. Escape closes dropdowns and dialogs before falling back to page back/close behavior.

Dialog size variants:

```xml
<dialog id="info" size="sm|md|lg|xl|fullscreen" close-on-escape="true" close-on-outside="true"/>
```

Templates are provided for `confirm_dialog`, `info_dialog`, and `error_dialog`.

## App Shell Patterns

Use generic app tags for Terminal, Index, Guide, HoloMap, or dashboard-style screens:

```xml
<app-shell>
  <app-header title="ECHO Terminal"/>
  <grid columns="230px 1fr 280px">
    <app-sidebar>
      <nav-list>
        <nav-item action="screencore.open_page" action-page="echoterminal:terminal_dashboard">
          <title>Dashboard</title>
        </nav-item>
      </nav-list>
    </app-sidebar>
    <app-content/>
    <inspector-panel/>
  </grid>
  <app-footer/>
</app-shell>
```

These tags are styled containers, not Terminal-specific logic. Keep data, filters, and game behavior in the consuming addon.

Inputs support search and filter workflows without putting filtering logic in ScreenCore:

```xml
<search-box id="mission-search"
            placeholder="Search missions..."
            value="{filters.search}"
            on-change="terminal.set_search"
            on-enter="terminal.apply_search"
            max-length="48"/>
```

When the user types, ScreenCore updates the bound context value and runs `on-change`. The addon action should update provider state, then call:

```java
EchoScreens.invalidateData();
```

Input validation and state:

```xml
<search-box state-key="missions.search"
            placeholder="Search missions..."
            debounce-ms="150"
            min-query-length="2"
            clearable="true"
            on-change="terminal.set_search"
            on-enter="terminal.apply_search"/>

<input state-key="settings.callsign"
       required="true"
       min-length="3"
       max-length="16"
       pattern="[A-Za-z0-9_]+"
       error="{field.callsign.error|}"/>
```

State keys are session-local to ScreenCore pages. They are available as `{state.<key>}` and can be cleared with:

```text
/echoscreencore state clear
/echoscreencore state clear echoscreencore:test_page_state
```

Toggles and checkboxes use the same pattern:

```xml
<toggle label="Show completed"
        checked="{filters.showCompleted}"
        on-change="terminal.set_show_completed"/>
```

Lists compare each row `value`, `id`, or `action-value` against the list `selected` binding. Matching rows receive `selected="true"` and can be styled with:

```css
list-row[selected] {
  background: theme(cardSelected);
  border-color: theme(accent);
}
```

## Accessibility And Debugging

Commands:

```text
/echoscreencore debug on
/echoscreencore debug off
/echoscreencore accessibility large_text true
/echoscreencore accessibility high_contrast true
/echoscreencore accessibility reduced_clutter true
/echoscreencore accessibility density compact
/echoscreencore accessibility density default
/echoscreencore accessibility density comfortable
```

Large text scales font tokens and spacing. High contrast strengthens text and borders. Reduced clutter suppresses decorative glow/shadow. Density changes spacing tokens.

The debug overlay is off by default. When enabled it shows component bounds, hover/focus state, scroll state, and diagnostics.

Extra diagnostic commands:

```text
/echoscreencore debug bindings on
/echoscreencore debug bindings off
/echoscreencore debug overlays on
/echoscreencore debug layout on
/echoscreencore inspect page echoscreencore:test_binding_diagnostics
/echoscreencore validate
/echoscreencore validate page echoscreencore:test_page_state
```

Binding failures warn once per page rebuild. With binding debug enabled, unresolved values render as `{?path}` placeholders instead of only the normal missing placeholder.

## Large Lists

For large dynamic collections, keep the repeated rows inside a `scroll` component and provide a stable key:

```xml
<scroll state-key="missions.list" scroll-state="true">
  <list bind="terminal.missions.visible" item="mission" key="{mission.id}" selected="{state.selectedMission}">
    <list-row action="terminal.select_mission" action-value="{mission.id}" value="{mission.id}">
      <title value="{mission.title}"/>
      <text value="{mission.summary}"/>
    </list-row>
    <empty-state title="No missions" body="No entries matched the current filter."/>
  </list>
</scroll>
```

ScreenCore rebuilds repeated children only on data/page/style invalidation, not every frame. Virtualized rendering is still a future optimization; use filtering providers and scroll panels for 100+ rows.

## Reload And Discovery

Developer commands:

```text
/echoscreencore list pages
/echoscreencore list styles
/echoscreencore list components
/echoscreencore reload
/echoscreencore reload styles
/echoscreencore reload page echoscreencore:test_app_shell
/echoscreencore validate
```

Optional manifests can live at:

```text
assets/<namespace>/eui/eui_manifest.json
```

Manifest page IDs are merged into page discovery, while direct resource scanning remains the fallback. The manifest is advisory; addons can still open any valid page resource directly.

Manifest metadata supported by ScreenCore tooling:

```json
{
  "id": "myaddon:page",
  "title": "Page Title",
  "category": "Terminal",
  "description": "What the page tests or provides",
  "defaultStyle": "myaddon:terminal",
  "requiredProviders": ["terminal"],
  "debugOnly": true
}
```

## Responsive And GUI Scale Guidance

Prefer scroll panels around dense lists, use `fr` grid tracks for flexible center columns, and give sidebars fixed widths only when the remaining content can still fit. ScreenCore warns about grid overflow and child bounds violations in debug diagnostics. `collapse-below`, `hide-below`, and `stack-below` are reserved style/attribute hooks for future responsive behavior; use them as documentation markers for now.

Focus-visible states are implemented by components such as buttons, inputs, selects, list rows, tabs, toggles, and checkboxes. Large text mode increases practical hitbox height; high contrast strengthens borders and text; reduced clutter removes extra glow/shadow.

## Future Terminal Migration Pattern

Terminal pages should move one screen at a time:

```xml
<page id="terminal-dashboard" styles="terminal">
  <screen-shell>
    <header title="{terminal.title}" subtitle="{terminal.statusLine}"/>
    <grid columns="220px 1fr 280px">
      <panel title="Commands"/>
      <scroll>
        <hero-card><title>{terminal.nextStep}</title></hero-card>
      </scroll>
      <panel title="Diagnostics"/>
    </grid>
  </screen-shell>
</page>
```

Keep Terminal gameplay and data logic in `echoterminal`; register ScreenCore data providers and actions as the integration boundary.

Dynamic Terminal pages should use this boundary:

```xml
<list bind="terminal.missions.visible" item="mission" selected="{terminal.selectedMissionId}">
  <list-row action="terminal.select_mission" action-value="{mission.id}" value="{mission.id}">
    <title value="{mission.title}"/>
    <text value="{mission.summary}"/>
  </list-row>
</list>
```

Index and Guide pages follow the same pattern:

```xml
<repeat source="recipes.visible" item="recipe">
  <card action="index.select_recipe" action-value="{recipe.id}"/>
</repeat>

<list bind="guide.searchResults" item="article">
  <list-row action="guide.open_article" action-value="{article.id}"/>
</list>
```

## Current Limits

CSS is not browser CSS: no floats, cascade inheritance, scripts, remote resources, or DOM APIs. Dynamic collection filtering belongs in the consuming addon; ScreenCore provides inputs, actions, invalidation, and repeated rendering.
