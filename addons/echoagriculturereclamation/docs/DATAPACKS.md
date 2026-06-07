# ECHO: Agriculture Reclamation Datapacks

Agriculture data lives under `data/<namespace>/echoagriculturereclamation/`.

## Rule Sets

- `crops/*.json`: crop growth, yield, restoration weight, seed safety, and greenhouse modifiers.
- `soil/*.json`: soil growth support, safe state, restoration gain, and stabilized support.
- `machines/*.json`: machine tuning such as purifier radius, hydroponic timing, greenhouse scoring, and drone service.
- `progression/*.json`: route thresholds, recovery profile ranges, and restoration conversion caps.
- `processes/*.json`: machine/process cards used by Index and Terminal-facing recipe surfaces.

## Process Format

Process files may define one object or a `processes` map. Fields are intentionally text-friendly so packs can describe virtual machine routes without adding Java recipes.

```json
{
  "processes": {
    "bio_reactor_biomass": {
      "machine": "bio_reactor",
      "title": "Biomass Bio-Reaction",
      "inputs": ["Agriculture crop matter"],
      "catalysts": [],
      "outputs": ["echoagriculturereclamation:bio_gel"],
      "ticks": 120,
      "powerCost": 0,
      "notes": ["Special crops can add secondary outputs."]
    }
  }
}
```
