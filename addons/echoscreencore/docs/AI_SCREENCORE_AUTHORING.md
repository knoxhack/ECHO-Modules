# AI ScreenCore Authoring

Use this file as the working checklist for AI-authored ScreenCore UI.

## Start Here

1. Open `AGENTS.md`.
2. Pick the closest page from `SCREENCORE_EXAMPLE_CATALOG.md`.
3. Copy the real resource pattern.
4. Validate with `/echoscreencore inspect page <id> 360 240`.

## Good List Detail Pattern

Source resource: `assets/echoscreencore/eui/pages/reference_list_detail.eui.xml`

```xml
<grid class="sc-list-detail" columns="0.85fr 1.15fr" gap="8" stack-below="760">
  <scroll class="sc-scroll-region" scroll-state="true" state-key="reference.records">
    <list bind="screencore.reference.records" item="record" selected="{screencore.reference.selectedRecordId}">
      <list-row class="sc-list-row" action="screencore.reference.select_record" action-value="{record.id}" value="{record.id}">
        <status-chip class="sc-row-badge" status="{record.status}" value="{record.badge}"/>
        <column class="sc-row-copy">
          <title value="{record.title}"/>
          <text value="{record.summary}"/>
        </column>
      </list-row>
      <empty-state title="No Records" body="No records are available."/>
    </list>
  </scroll>
  <section class="sc-panel" title="DETAIL"/>
</grid>
```

## Bad To Good: Grid

Bad:

```xml
<grid columns="1fr 1fr 1fr" gap="12">
  <section title="LEFT"/>
  <section title="MIDDLE"/>
  <section title="RIGHT"/>
</grid>
```

Good:

```xml
<grid class="sc-three-column" columns="1fr 1fr 1fr" gap="8" stack-below="900">
  <section class="sc-panel" title="LEFT"/>
  <section class="sc-panel" title="MIDDLE"/>
  <section class="sc-panel" title="RIGHT"/>
</grid>
```

## Bad To Good: Giant Fixed Panel

Bad:

```xml
<section title="MISSIONS" height="700px"/>
```

Good:

```xml
<scroll class="sc-scroll-region" scroll-state="true" state-key="missions">
  <list bind="screencore.reference.records" item="record">
    <list-row class="sc-list-row" value="{record.id}">
      <title value="{record.title}"/>
    </list-row>
    <empty-state title="No Records" body="No records are available."/>
  </list>
</scroll>
```

## Bad To Good: Nested Scrolls

Bad:

```xml
<scroll>
  <section>
    <scroll>
      <list/>
    </scroll>
  </section>
</scroll>
```

Good:

```xml
<section class="sc-panel" title="RESULTS">
  <scroll class="sc-scroll-region">
    <list/>
  </scroll>
</section>
```

## Bad To Good: Row Text

Bad:

```xml
<list-row>
  <status-chip value="READY"/>
  <title value="{record.longTitle}"/>
  <button action="noop">Open</button>
</list-row>
```

Good:

```xml
<list-row class="sc-list-row">
  <status-chip class="sc-row-badge" value="READY"/>
  <column class="sc-row-copy">
    <title value="{record.longTitle}"/>
    <text value="{record.summary}"/>
  </column>
  <button action="noop">Open</button>
</list-row>
```

