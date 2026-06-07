# Copy Block Text Recovery

`copy-block` is the ScreenCore component for critical row and detail copy. It was added after the Terminal migration exposed a repeated-row text failure: chips, buttons, status labels, section titles, icons, and progress bars rendered, but nested `title` / `text` nodes inside dense list rows could disappear, clip away, or be buried by row layout. Terminal could not be usable if mission names, requirements, rewards, and row summaries were missing, so row copy now uses a direct-render component.

Use `copy-block` anywhere the text is part of the row's meaning, not decorative copy.

## Component Contract

```xml
<copy-block title="Anchor Pod Outpost" subtitle="Craft and place an Ash Campfire near the pod."/>
```

- Tag: `copy-block`.
- Primary copy comes from `title`; if `title` is blank, `value` is used.
- Detail copy uses the first nonblank value from `subtitle`, `summary`, `detail`, `body`, then node text.
- The component draws text during its own render pass, like buttons and status chips.
- Children render below the title/detail copy. Use `content-height` when child content, such as a progress bar, needs reserved vertical space.
- Text is width-trimmed by default. Set `wrap="true"` and `max-lines` only when the row height is large enough for wrapped text.
- Local clipping is opt-in. Use `overflow: hidden` only when the copy should be clipped inside the copy block itself; scroll panels should own normal scroll clipping.

## Copyable Patterns

Basic dense row:

```xml
<list-row action="terminal.select" action-value="{row.id}" value="{row.id}">
  <status-chip status="{row.status}" value="{row.code}"/>
  <copy-block class="terminal-row-copy"
              title="{row.title}"
              subtitle="{row.summary}"/>
  <status-chip status="{row.status}" value="{row.statusLabel}"/>
</list-row>
```

Route row with progress below text:

```xml
<list-row class="terminal-route-mission-row"
          action="terminal.select_mission"
          action-value="{mission.id}"
          value="{mission.id}"
          selected="{mission.selected}">
  <status-chip class="terminal-route-index-chip"
               status="{mission.status}"
               value="{mission.displayOrderLabel|01}"/>
  <copy-block class="terminal-route-row-copy terminal-route-copy-block"
              title="{mission.title}"
              subtitle="{mission.sourceLine}">
    <progress-bar class="terminal-route-row-progress"
                  value="{mission.progressPercent}"
                  max="100"/>
  </copy-block>
  <status-chip class="terminal-route-state-chip"
               status="{mission.status}"
               value="{mission.statusCompactLabel}"/>
</list-row>
```

Detail row with an item icon and a count/status chip:

```xml
<list-row class="terminal-route-detail-row">
  <item-icon class="terminal-route-item-icon"
             item="{reward.iconItemId}"
             count="{reward.iconCount|1}"/>
  <copy-block class="terminal-route-row-copy terminal-route-copy-block"
              title="{reward.title}"
              subtitle="{reward.detail}"/>
  <status-chip class="terminal-route-index-chip"
               status="info"
               value="{reward.countLabel}"/>
</list-row>
```

## Styling Guidance

Keep row heights large enough for the copy and any child content:

```css
.terminal-route-row-copy {
  title-line-height: 11px;
  detail-line-height: 10px;
  text-gap: 2px;
  content-height: 4px;
  min-height: 27px;
  title-color: #EAFBFF;
  detail-color: #9FC8D6;
  max-lines: 1;
  wrap: false;
}
```

Use these properties first:

- `title-line-height` and `detail-line-height` to reserve readable text height.
- `text-gap` to separate title and detail lines.
- `content-height` when children render below text.
- `min-height` so dense rows cannot collapse the copy area.
- `title-color` and `detail-color` for explicit readable text colors.
- `wrap`, `max-lines`, `title-max-lines`, and `detail-max-lines` only with enough row height.
- `overflow: hidden` only for intentional local clipping.

Do not put a `progress-bar` beside the copy when it belongs under the copy. Put it inside `copy-block` and reserve space with `content-height`.

## Migration Guidance

Prefer `copy-block` over nested `column > title + text` for repeated dense rows, Terminal rows, mission rows, module rows, requirement rows, reward rows, recipe rows, map/provider rows, and any card where missing copy would break usability.

Existing `title` and `text` components remain valid for simple static page copy, large headings, empty states, and layouts where the text is not squeezed between fixed controls.

When migrating a row:

1. Keep fixed controls as siblings: chips, icons, item icons, buttons, toggles.
2. Put all meaningful row copy into one `copy-block`.
3. Put row progress inside `copy-block` only when it should sit below the title/detail text.
4. Give the row and the copy block explicit enough height for title, detail, gap, and children.
5. Verify the row in a scroll panel and at the compact breakpoint.

The Terminal Survival Route, Command Deck, HoloMap, Recipe Index, Dossier, and reward pages use this pattern so row text remains visible under ScreenCore density, scrolling, and responsive layout pressure.
