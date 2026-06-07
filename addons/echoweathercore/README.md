<!-- CURSEFORGE_README_START -->
# WeatherCore by ECHO Labs

![WeatherCore by ECHO Labs brand sheet](../../../publishing/curseforge/ai-generated-v4/echoweathercore/brand-sheet.png)

**Mod ID: echoweathercore**

![WeatherCore by ECHO Labs feature overview portrait](../../../publishing/curseforge/ai-generated-v4/echoweathercore/features-portrait.png)

![WeatherCore by ECHO Labs feature overview landscape](../../../publishing/curseforge/ai-generated-v4/echoweathercore/features-landscape.png)

## CurseForge Summary

Mod ID: echoweathercore

## Main Features

- Storm and forecast systems.
- Atmospheric hazard sensors.
- Weather event hooks.

## CurseForge Asset Files

- Brand sheet: `../../../publishing/curseforge/ai-generated-v4/echoweathercore/brand-sheet.png`
- Feature overview portrait: `../../../publishing/curseforge/ai-generated-v4/echoweathercore/features-portrait.png`
- Feature overview landscape: `../../../publishing/curseforge/ai-generated-v4/echoweathercore/features-landscape.png`

<!-- CURSEFORGE_README_END -->
---

## Existing Developer Notes

# ECHO: WeatherCore

**Mod ID:** `echoweathercore`  
**Display Name:** ECHO: WeatherCore  
**Version:** 1.0.0

## What is WeatherCore?

ECHO: WeatherCore is the shared first-party weather, atmospheric hazard, storm forecasting, and environmental event system for the ECHO / Ashfall ecosystem. It makes Ashfall's ruined world feel unstable, hostile, cinematic, and alive.

## Design Rule

**Weather changes expedition decisions, not just annoys the player.**

WeatherCore is not a generic "rain with debuffs" mod. It is a ruined-world weather framework built around:
- Broken climate infrastructure
- Toxic atmosphere
- Radiation pressure
- Orbital collapse
- Nexus interference
- Post-Gridfall environmental failure

## Core Weather Events

WeatherCore supports eight core weather events:

1. **Ash Storm** - Visibility and exploration pressure
2. **Toxic Rain** - Chemical and filter pressure
3. **Radiation Storm** - Radiation and route lockdown
4. **Cryo Front** - Cold and shelter pressure
5. **Heat Surge** - Hydration and machine pressure
6. **Nexus Signal Storm** - Digital and navigation corruption
7. **Orbital Debris Shower** - Impact and salvage opportunity
8. **Electromagnetic Blackout** - Power and electronic disruption

## Warning Phases

All weather events progress through phases:
- **Forecast** - Long warning. Plan accordingly.
- **Incoming** - Short warning. Reduce travel speed.
- **Active** - Storm starts. Effects apply.
- **Critical** - Conditions worsen. Expedition not advised.
- **Clearing** - Storm ends. Systems restore.
- **Ended** - Event complete.

## Severity System

- **Low** - Minor inconvenience
- **Moderate** - Noticeable impact
- **Severe** - Dangerous conditions
- **Extreme** - Life-threatening

## Scope System

- **Local** - Affects a small radius
- **Regional** - Affects a large area
- **Global** - Affects the entire dimension
- **Route-based** - Tied to specific expedition routes

## Forecast System

Weather forecasts are available through:
- Storm Scanner (item)
- Weather Radio (item)
- Weather Station (block)
- `/echoweathercore forecast` command

Forecasts show:
- Current weather
- Incoming weather
- ETA
- Severity
- Expected effects
- Recommended gear
- Route risk

## Shelter / Countermeasure System

Shelter reduces or prevents weather effects:
- Under solid roof / no sky visible
- Within radius of Faraday Shelter Core
- Within Atmospheric Shield Emitter
- Near Portable Shelter Beacon
- Inside valid structure region

Countermeasure items reduce specific weather effects:
- **Ash Filter Wrap** - Ash storms
- **Signal Anchor** - Nexus signal storms
- **Cryo Heat Cell** - Cryo fronts
- **Faraday Coil** - EM blackouts

## Weather Items

- **Storm Scanner** - Local weather and forecast
- **Weather Radio** - Regional forecast
- **Portable Shelter Beacon** - Temporary shelter marker
- **Ash Filter Wrap** - Ash storm countermeasure
- **Faraday Coil** - EM blackout countermeasure
- **Signal Anchor** - Nexus storm countermeasure
- **Cryo Heat Cell** - Cryo front countermeasure
- **Toxic Rain Collector** - Collects toxic rain
- **Debris Tracker** - Locates orbital debris
- **Route Flare** - Temporary route marker

## Weather Blocks

- **Weather Station** - Local forecast provider
- **Storm Beacon** - Safe route marker
- **Faraday Shelter Core** - EM shelter
- **Atmospheric Shield Emitter** - Powered weather shield
- **Route Warning Post** - Route weather warning
- **Debris Radar Dish** - Debris prediction
- **Signal Stabilizer** - Nexus interference reducer
- **Emergency Siren** - Warning alarm
- **Climate Sensor** - Local atmosphere reading

## Weather Resources

Each weather type can generate unique resources:
- Ash Storm: Fine Ash, Ash Glass Dust, Storm-Sifted Scrap
- Toxic Rain: Condensed Toxin, Acidic Sludge, Toxic Rainwater
- Radiation Storm: Charged Uranium Dust, Irradiated Crystal Dust
- Cryo Front: Cryo Frost, Frozen Conduit Shard
- Heat Surge: Thermal Residue, Baked Ash Glass
- Nexus Signal Storm: Static Filament, Memory Residue, Nexus Trace
- Orbital Debris: Orbital Alloy Scrap, Burned Circuitry, Satellite Lens
- EM Blackout: Magnetized Scrap, Burned Relay Coil, Overloaded Capacitor

## Terminal Integration

When ECHO Terminal is present, WeatherCore provides:
- Weather forecast page provider
- Incoming weather alerts
- Active weather alerts
- Critical weather warnings
- Clearing notifications

## HoloMap Integration

When ECHO HoloMap is present, WeatherCore exposes:
- Active weather region markers
- Forecast boundaries
- Safe shelter markers
- Route risk overlays

## Lens Integration

When ECHO Lens is present, WeatherCore provides:
- Atmosphere scan rows
- Weather station status
- Scanner reliability readings

## PowerGrid Integration

When ECHO PowerGrid is present, WeatherCore reports:
- Solar panel reduction during ash storms
- Scrubber power draw during toxic rain
- Substation flicker during radiation storms
- Battery efficiency loss during cryo fronts
- Machine heat rise during heat surges
- Grid instability during EM blackouts
- Unstable nodes during Nexus storms

## Drone Integration

Weather affects drones through API:
- Scout reliability reduced in ash storms
- Casing corrosion warning in toxic rain
- Telemetry noise in radiation storms
- Actuator freeze warning in cryo fronts
- Recall/shutdown warning in EM blackouts
- False command risk in Nexus storms

## Faction Integration

Weather exposes faction patrol behavior hooks:
- Radwarden: Active during radiation/cryo, lockdown in severe storms
- Crashbreak: Salvage after debris, retreat in radiation
- Sporebound: Active in toxic rain, harvests toxins

## SoundCore Integration

When SoundCore is present:
- Weather warning stingers
- Ash storm ambience
- Toxic rain ambience
- Radiation storm ambience
- Cryo front ambience
- Nexus signal storm ambience
- EM blackout pulse
- Orbital debris warning cues

## TutorialCore Integration

When TutorialCore is present:
- Unlock tutorial card on first weather event
- Beginner warning when entering first storm
- Explain storm scanner, weather station, shelter

## Nexus Ending Effects

Weather responds to Nexus path if available:
- **Restore**: Storms lessen over time
- **Destroy**: Climate systems destabilize
- **Control**: Player can influence some weather
- **Merge**: Reality weather becomes unpredictable

## Data-Driven Weather Profiles

Weather profiles are loaded from JSON at:
```
data/<namespace>/weather_profiles/*.json
```

Addons can define their own weather profiles.

Example profile:
```json
{
  "id": "echoweathercore:ash_storm",
  "displayName": "Ash Storm",
  "type": "ASH_STORM",
  "defaultSeverity": "MODERATE",
  "scope": "REGIONAL",
  "durationTicks": 12000,
  "warningTicks": 2400,
  "weight": 40,
  "cooldownTicks": 24000,
  "effects": {
    "visibilityMultiplier": 0.45,
    "scannerRangeMultiplier": 0.60
  },
  "terminalWarning": "Ash front detected. Visibility loss expected.",
  "echoLines": [
    "Ash density rising. Visual navigation unreliable."
  ]
}
```

## Commands

- `/echoweathercore current` - Show current weather
- `/echoweathercore forecast` - Show forecast
- `/echoweathercore trigger <weather> [severity] [scope]` - Trigger weather (admin)
- `/echoweathercore clear` - Clear all weather (admin)
- `/echoweathercore clear <weather>` - Clear specific weather (admin)
- `/echoweathercore debug` - Debug info (admin)
- `/echoweathercore locate_shelter` - Check shelter status
- `/echoweathercore route_risk` - Show route risk
- `/echoweathercore list_profiles` - List loaded profiles
- `/echoweathercore reload` - Reload data (admin)

## Config

Server config:
- `enableWeatherCore` - Master toggle
- `enableAshStorms` through `enableElectromagneticBlackouts` - Per-type toggles
- `globalWeatherFrequency` - LOW, NORMAL, HIGH, EXTREME, DISABLED
- `minimumWarningTicks` - Minimum warning time
- `maxSimultaneousRegionalEvents` - Cap concurrent events
- `earlyGameSevereWeather` - Allow severe weather early
- `weatherCheckIntervalTicks` - Scheduler interval
- `allowWeatherDamage`, `allowMachineWear`, `allowPowerGridDisruption`
- Per-weather base weights

Client config:
- `enableWeatherParticles` - Particle toggle
- `enableWeatherScreenEffects` - Screen effect toggle
- `enableWeatherSounds` - Sound toggle
- `enableStormWarnings` - Warning toggle
- `enableHoloMapWeatherOverlay` - HoloMap overlay toggle
- `weatherParticleDensity` - LOW, NORMAL, HIGH
- `screenDistortionIntensity` - LOW, NORMAL, HIGH
- `showForecastToasts` - Toast toggle

## API Examples

```java
// Get weather at position
List<ActiveWeatherEvent> weather = WeatherCoreApi.getCurrentWeather(level, pos);

// Get modifiers
WeatherEffectModifiers mods = WeatherCoreApi.getWeatherModifiers(level, pos);

// Check shelter
boolean safe = WeatherCoreApi.isSheltered(player);

// Get route risk
WeatherRouteRisk risk = WeatherCoreApi.getRouteWeatherRisk(player, routeId);

// Get forecast
List<WeatherForecast> forecast = WeatherCoreApi.getForecast(player);
```

## Known Limitations

- Full GUI screens for weather station and machines are planned for a future pass.
- Advanced visual effects (screen frost, glitch overlays) require RenderCore and are scaffolded.
- Deep faction behavior changes require faction API integration.
- PowerGrid destructive effects are disabled by default.

## Future Roadmap

- Advanced weather movement/tracking
- Full Terminal forecast UI
- HoloMap overlay provider
- Lens atmosphere scan provider
- SoundCore ambience integration
- TutorialCore tutorial cards
- Weather-based missions/FieldOps
- Memory Rain and Spore Bloom full implementation
- Dynamic weather resource spawning
- Weather damage system (configurable)
- Machine wear system (configurable)

---

WeatherCore makes Ashfall's world feel unstable, tactical, and alive without turning weather into random punishment.

## 1.0.0 Public Beta Quickstart

1. Install required dependencies: echocore, echonetcore.
2. Launch the game or tool and confirm the module appears in `metadata/modules/echoweathercore.json`.
3. First action: open the module UI, command, keybind, or primary block/item.
4. Common issue: missing optional integrations should reduce features, not crash.
5. Ashfall behavior: Ashfall is optional and may add profile-specific content.

Public release page: `docs/release_pages/echoweathercore.md`.
