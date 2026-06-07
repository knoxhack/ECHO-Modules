# EchoIndex 1.3.0 Content Provider Migration

EchoIndex 1.3.0 moves first-party addon integration toward one provider-owned contract:

```java
IIndexContentProvider#snapshot(IndexBuildContext context)
```

The returned `IndexContentSnapshot` can include categories, entries, recipe/process cards, source facts, relations, and provider diagnostics. Addons should prefer this contract for new work instead of registering separate entry, recipe, and source providers.

## Datapack Layout

Pack-authored content lives under:

- `data/<namespace>/echo_index/categories/`
- `data/<namespace>/echo_index/entries/`
- `data/<namespace>/echo_index/source_facts/`
- `data/<namespace>/echo_index/recipe_cards/`
- `data/<namespace>/echo_index/relations/`

Schemas are published from ECHO Core under `assets/echocore/schemas/`.

## Migration Notes

- Keep content ownership in the addon that owns the gameplay system.
- Use source facts for acquisition, routes, caches, loot, structures, missions, research, and machine hints.
- Use recipe cards for non-vanilla processes or guidance that should appear in recipes/uses.
- Use relations to connect entries, source facts, recipe cards, routes, hazards, tutorials, and diagnostics.
- Legacy providers still work during the 1.3 migration wave, but new first-party providers should publish a single snapshot.
