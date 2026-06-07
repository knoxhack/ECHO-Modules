# Agriculture Reclamation Config

Config file: `echoagriculturereclamation-common.toml`

## Growth

- `global_growth_chance_bonus`: Flat percentage-point bonus for all Reclamation crop growth checks.
- `stable_seed_growth_bonus`: Extra bonus for stable seed profiles.

## Hydroponics

- `hydroponic_growth_ticks`: Base ticks per hydroponic growth check.
- `hydroponic_nutrient_cap`: Tray nutrient buffer capacity.
- `hydroponic_nutrient_per_mix`: Nutrient added by one Soil Nutrient Mix.

## Greenhouse

- `greenhouse_safe_threshold`: Score required for a safe saved greenhouse zone.
- `greenhouse_glass_weight`: Score contribution for Greenhouse Glass.
- `greenhouse_filter_weight`: Score contribution for Spore Filters.
- `greenhouse_dock_weight`: Score contribution for Pollinator Drone Docks.
- `greenhouse_controller_weight`: Score contribution for Greenhouse Controllers.
- `greenhouse_tray_weight`: Score contribution for Hydroponic Trays.

## Pollinators

- `pollinator_service_radius`: Crop and tray service radius.
- `pollinator_service_ticks`: Base ticks between pollinator services.
- `pollinator_growth_bonus`: Growth chance bonus applied by pollinator service.

## Restoration

- `purify_threshold`: Chunk score threshold for early soil recovery.
- `stabilize_threshold`: Chunk score threshold for stable soil recovery.
- `restore_threshold`: Chunk score threshold for restored soil completion.

## Optional Integrations

- `enable_weather_crop_penalties`: Enables WeatherCore exposed-crop penalties.
- `weather_penalty_multiplier`: Multiplies WeatherCore growth pressure.
- `enable_power_acceleration`: Enables PowerGrid machine acceleration.
- `powered_throughput_divisor`: Tick divisor used when PowerGrid reports available power.
