# ECHO Recovery Datapacks

ECHO Recovery 1.3.0 loads Recovery content under the namespaced addon folder:

- `data/<namespace>/echorecovery/recovery_grave_type/`
- `data/<namespace>/echorecovery/recovery_rule/`
- `data/<namespace>/echorecovery/recovery_preset/`

The old prototype shape, `data/<namespace>/recovery_grave_type/`, is not used by the 1.3.0 loader.

## Grave Types

Example:

```json
{
  "id": "my_pack:ancient_tomb",
  "display_name": "Ancient Tomb",
  "block": "echorecovery:grave",
  "tooltip": "Protected recovery cache",
  "field_cache": false,
  "contaminated": false,
  "temporary_platform": false,
  "hazard_notes": []
}
```

## Recovery Rules

Rules may target items or item tags and apply one of the supported item rule results:

- `SOULBOUND`
- `ALWAYS_GRAVE`
- `DROP_ON_DEATH`
- `DESTROY_ON_DEATH`
- `PROTECTED`
- `NO_GRAVE`

Example:

```json
{
  "id": "my_pack:fragile_salvage",
  "priority": 10,
  "item": "minecraft:glass_bottle",
  "result": "DESTROY_ON_DEATH"
}
```

## Recovery Presets

Presets describe server policy without silently overwriting explicit config unless the server selects that preset.

Example:

```json
{
  "id": "my_pack:forgiving",
  "display_name": "Forgiving Recovery",
  "description": "Protected graves with no expiration and no remote recovery.",
  "values": {
    "protected_graves": true,
    "remote_recovery": false,
    "expiration_enabled": false
  }
}
```
