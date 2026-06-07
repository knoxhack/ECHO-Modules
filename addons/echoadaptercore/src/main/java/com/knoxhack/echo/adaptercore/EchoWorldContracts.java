package com.knoxhack.echo.adaptercore;

import java.util.List;
import java.util.Map;

public final class EchoWorldContracts {
    private EchoWorldContracts() {
    }

    public record EchoWorldRegion(
            String id,
            String displayName,
            int minX,
            int maxX,
            int minZ,
            int maxZ,
            String missionId) {
        public EchoWorldRegion {
            id = AdapterContractGuards.requireText(id, "world region id");
            displayName = AdapterContractGuards.requireText(displayName, "world region display name");
            missionId = AdapterContractGuards.optionalText(missionId);
            if (minX > maxX || minZ > maxZ) {
                throw new IllegalArgumentException("world region bounds must be ordered");
            }
        }

        public boolean contains(int x, int z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }
    }

    public record EchoWorldRegionTransitionRequest(
            String playerId,
            String previousRegionId,
            String currentRegionId,
            String currentMissionId,
            long gameTick,
            String sourceReason) {
        public EchoWorldRegionTransitionRequest {
            playerId = AdapterContractGuards.requireText(playerId, "world region transition player id");
            previousRegionId = AdapterContractGuards.optionalText(previousRegionId);
            currentRegionId = AdapterContractGuards.optionalText(currentRegionId);
            currentMissionId = AdapterContractGuards.optionalText(currentMissionId);
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (gameTick < 0L) {
                throw new IllegalArgumentException("world region transition game tick must not be negative");
            }
        }
    }

    public record EchoWorldRegionTransitionResult(
            String playerId,
            String previousRegionId,
            String currentRegionId,
            String eventType,
            boolean regionEntered,
            boolean regionExited,
            List<String> missionEvents,
            long gameTick,
            String sourceReason) {
        public EchoWorldRegionTransitionResult {
            playerId = AdapterContractGuards.requireText(playerId, "world region transition result player id");
            previousRegionId = AdapterContractGuards.optionalText(previousRegionId);
            currentRegionId = AdapterContractGuards.optionalText(currentRegionId);
            eventType = AdapterContractGuards.requireText(eventType, "world region transition result event type");
            missionEvents = missionEvents == null ? List.of() : List.copyOf(missionEvents);
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (gameTick < 0L) {
                throw new IllegalArgumentException("world region transition result game tick must not be negative");
            }
        }
    }

    public record EchoWorldHazard(
            String id,
            String type,
            int centerX,
            int centerZ,
            int radius,
            double damagePerTick,
            String statusEffectId) {
        public EchoWorldHazard {
            id = AdapterContractGuards.requireText(id, "world hazard id");
            type = AdapterContractGuards.requireText(type, "world hazard type");
            statusEffectId = AdapterContractGuards.optionalText(statusEffectId);
            if (radius < 0 || damagePerTick < 0.0D) {
                throw new IllegalArgumentException("world hazard radius and damage must not be negative");
            }
        }

        public boolean affects(int x, int z) {
            int dx = x - centerX;
            int dz = z - centerZ;
            return dx * dx + dz * dz <= radius * radius;
        }
    }

    public record EchoWorldHazardTransitionRequest(
            String playerId,
            String previousHazardId,
            String currentHazardId,
            String statusEffectId,
            long gameTick,
            String sourceReason) {
        public EchoWorldHazardTransitionRequest {
            playerId = AdapterContractGuards.requireText(playerId, "world hazard transition player id");
            previousHazardId = AdapterContractGuards.optionalText(previousHazardId);
            currentHazardId = AdapterContractGuards.optionalText(currentHazardId);
            statusEffectId = AdapterContractGuards.optionalText(statusEffectId);
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (gameTick < 0L) {
                throw new IllegalArgumentException("world hazard transition game tick must not be negative");
            }
        }
    }

    public record EchoWorldHazardTransitionResult(
            String playerId,
            String previousHazardId,
            String currentHazardId,
            String eventType,
            boolean hazardEntered,
            boolean hazardExited,
            List<String> statusEffects,
            long gameTick,
            String sourceReason) {
        public EchoWorldHazardTransitionResult {
            playerId = AdapterContractGuards.requireText(playerId, "world hazard transition result player id");
            previousHazardId = AdapterContractGuards.optionalText(previousHazardId);
            currentHazardId = AdapterContractGuards.optionalText(currentHazardId);
            eventType = AdapterContractGuards.requireText(eventType, "world hazard transition result event type");
            statusEffects = statusEffects == null ? List.of() : List.copyOf(statusEffects);
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (gameTick < 0L) {
                throw new IllegalArgumentException("world hazard transition result game tick must not be negative");
            }
        }
    }

    public record EchoHazardTickDamageRequest(
            String playerId,
            double healthBefore,
            int severity,
            long gameTick,
            String sourceReason,
            EchoWorldHazard hazard,
            EchoDifficultyProfile difficulty) {
        public EchoHazardTickDamageRequest {
            playerId = AdapterContractGuards.requireText(playerId, "hazard tick damage player id");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (healthBefore < 0.0D || severity < 0 || gameTick < 0L) {
                throw new IllegalArgumentException("hazard tick damage health, severity, and tick must not be negative");
            }
            if (hazard == null) {
                throw new IllegalArgumentException("hazard tick damage hazard must not be null");
            }
            if (difficulty == null) {
                throw new IllegalArgumentException("hazard tick damage difficulty must not be null");
            }
        }
    }

    public record EchoHazardTickDamageResult(
            String playerId,
            String hazardId,
            String statusEffectId,
            String difficultyId,
            double healthBefore,
            double healthAfter,
            double baseDamage,
            double damageApplied,
            double hazardMultiplier,
            int severity,
            long gameTick,
            String sourceReason,
            boolean damaged) {
        public EchoHazardTickDamageResult {
            playerId = AdapterContractGuards.requireText(playerId, "hazard tick damage result player id");
            hazardId = AdapterContractGuards.requireText(hazardId, "hazard tick damage result hazard id");
            statusEffectId = AdapterContractGuards.optionalText(statusEffectId);
            difficultyId = AdapterContractGuards.requireText(difficultyId, "hazard tick damage result difficulty id");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (healthBefore < 0.0D || healthAfter < 0.0D || baseDamage < 0.0D || damageApplied < 0.0D
                    || hazardMultiplier < 0.0D || severity < 0 || gameTick < 0L) {
                throw new IllegalArgumentException("hazard tick damage result values must not be negative");
            }
        }
    }

    public record EchoWeatherState(
            String id,
            String hudLine,
            String audioCue,
            String renderProfile) {
        public EchoWeatherState {
            id = AdapterContractGuards.requireText(id, "weather state id");
            hudLine = AdapterContractGuards.requireText(hudLine, "weather HUD line");
            audioCue = AdapterContractGuards.requireText(audioCue, "weather audio cue");
            renderProfile = AdapterContractGuards.requireText(renderProfile, "weather render profile");
        }
    }

    public record EchoWeatherScheduleProfile(
            String id,
            String type,
            String severity,
            String scope,
            int durationTicks,
            int warningTicks,
            int weight,
            boolean enabled) {
        public EchoWeatherScheduleProfile {
            id = AdapterContractGuards.requireText(id, "weather schedule profile id");
            type = AdapterContractGuards.requireText(type, "weather schedule type");
            severity = AdapterContractGuards.requireText(severity, "weather schedule severity");
            scope = AdapterContractGuards.requireText(scope, "weather schedule scope");
            if (durationTicks < 0 || warningTicks < 0 || weight < 0) {
                throw new IllegalArgumentException("weather schedule timing and weight must not be negative");
            }
        }
    }

    public record EchoWeatherScheduleRequest(
            long currentTick,
            int minimumWarningTicks,
            int centerX,
            int centerY,
            int centerZ,
            int radius,
            String sourceReason,
            EchoWeatherScheduleProfile profile) {
        public EchoWeatherScheduleRequest {
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (currentTick < 0L || minimumWarningTicks < 0 || radius < 0) {
                throw new IllegalArgumentException("weather schedule tick, warning, and radius must not be negative");
            }
            if (profile == null) {
                throw new IllegalArgumentException("weather schedule profile must not be null");
            }
        }
    }

    public record EchoWeatherScheduleResult(
            String profileId,
            String type,
            String severity,
            String scope,
            String phase,
            long warningStartTick,
            long startTick,
            long endTick,
            int centerX,
            int centerY,
            int centerZ,
            int radius,
            String sourceReason,
            boolean scheduled) {
        public EchoWeatherScheduleResult {
            profileId = AdapterContractGuards.requireText(profileId, "weather schedule result profile id");
            type = AdapterContractGuards.requireText(type, "weather schedule result type");
            severity = AdapterContractGuards.requireText(severity, "weather schedule result severity");
            scope = AdapterContractGuards.requireText(scope, "weather schedule result scope");
            phase = AdapterContractGuards.requireText(phase, "weather schedule result phase");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (warningStartTick < 0L || startTick < 0L || endTick < 0L || radius < 0) {
                throw new IllegalArgumentException("weather schedule result timing and radius must not be negative");
            }
        }
    }

    public record EchoWeatherScheduleTickRequest(
            String eventId,
            long gameTick,
            EchoWeatherScheduleResult schedule) {
        public EchoWeatherScheduleTickRequest {
            eventId = AdapterContractGuards.requireText(eventId, "weather schedule tick event id");
            if (gameTick < 0L) {
                throw new IllegalArgumentException("weather schedule tick game tick must not be negative");
            }
            if (schedule == null) {
                throw new IllegalArgumentException("weather schedule tick schedule must not be null");
            }
        }
    }

    public record EchoWeatherScheduleTickResult(
            String eventId,
            String profileId,
            String type,
            String severity,
            String scope,
            String previousPhase,
            String phase,
            long gameTick,
            long warningStartTick,
            long startTick,
            long endTick,
            int centerX,
            int centerY,
            int centerZ,
            int radius,
            String sourceReason,
            boolean active,
            boolean ended,
            boolean phaseChanged) {
        public EchoWeatherScheduleTickResult {
            eventId = AdapterContractGuards.requireText(eventId, "weather schedule tick result event id");
            profileId = AdapterContractGuards.requireText(profileId, "weather schedule tick result profile id");
            type = AdapterContractGuards.requireText(type, "weather schedule tick result type");
            severity = AdapterContractGuards.requireText(severity, "weather schedule tick result severity");
            scope = AdapterContractGuards.requireText(scope, "weather schedule tick result scope");
            previousPhase = AdapterContractGuards.requireText(previousPhase, "weather schedule tick previous phase");
            phase = AdapterContractGuards.requireText(phase, "weather schedule tick result phase");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (gameTick < 0L || warningStartTick < 0L || startTick < 0L || endTick < 0L || radius < 0) {
                throw new IllegalArgumentException("weather schedule tick result timing and radius must not be negative");
            }
        }
    }

    public record EchoWeatherStateApplyRequest(
            String eventId,
            String regionId,
            String phase,
            long gameTick,
            String sourceReason,
            EchoWeatherState weather,
            EchoAtmosphereState atmosphere) {
        public EchoWeatherStateApplyRequest {
            eventId = AdapterContractGuards.requireText(eventId, "weather state apply event id");
            regionId = AdapterContractGuards.optionalText(regionId);
            phase = AdapterContractGuards.requireText(phase, "weather state apply phase");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (gameTick < 0L) {
                throw new IllegalArgumentException("weather state apply game tick must not be negative");
            }
            if (weather == null) {
                throw new IllegalArgumentException("weather state apply weather must not be null");
            }
            if (atmosphere == null) {
                throw new IllegalArgumentException("weather state apply atmosphere must not be null");
            }
        }
    }

    public record EchoWeatherStateApplyResult(
            String eventId,
            String weatherId,
            String regionId,
            String phase,
            Map<String, Object> hudState,
            Map<String, Object> audioState,
            Map<String, Object> renderState,
            long gameTick,
            String sourceReason,
            boolean applied) {
        public EchoWeatherStateApplyResult {
            eventId = AdapterContractGuards.requireText(eventId, "weather state apply result event id");
            weatherId = AdapterContractGuards.requireText(weatherId, "weather state apply result weather id");
            regionId = AdapterContractGuards.optionalText(regionId);
            phase = AdapterContractGuards.requireText(phase, "weather state apply result phase");
            hudState = hudState == null ? Map.of() : Map.copyOf(hudState);
            audioState = audioState == null ? Map.of() : Map.copyOf(audioState);
            renderState = renderState == null ? Map.of() : Map.copyOf(renderState);
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (gameTick < 0L) {
                throw new IllegalArgumentException("weather state apply result game tick must not be negative");
            }
        }
    }

    public record EchoWeatherWarningRequest(
            String eventId,
            String weatherId,
            String regionId,
            String phase,
            String channel,
            String message,
            List<String> recipientPlayerIds,
            long gameTick,
            String sourceReason) {
        public EchoWeatherWarningRequest {
            eventId = AdapterContractGuards.requireText(eventId, "weather warning event id");
            weatherId = AdapterContractGuards.requireText(weatherId, "weather warning weather id");
            regionId = AdapterContractGuards.optionalText(regionId);
            phase = AdapterContractGuards.requireText(phase, "weather warning phase");
            channel = AdapterContractGuards.requireText(channel, "weather warning channel");
            message = AdapterContractGuards.requireText(message, "weather warning message");
            recipientPlayerIds = immutableTextList(recipientPlayerIds, "weather warning recipient player id");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (gameTick < 0L) {
                throw new IllegalArgumentException("weather warning game tick must not be negative");
            }
        }
    }

    public record EchoWeatherWarningResult(
            String eventId,
            String weatherId,
            String regionId,
            String phase,
            String channel,
            String message,
            List<String> recipientPlayerIds,
            int recipientCount,
            Map<String, Object> hudState,
            Map<String, Object> audioState,
            Map<String, Object> renderState,
            long gameTick,
            String sourceReason,
            boolean delivered) {
        public EchoWeatherWarningResult {
            eventId = AdapterContractGuards.requireText(eventId, "weather warning result event id");
            weatherId = AdapterContractGuards.requireText(weatherId, "weather warning result weather id");
            regionId = AdapterContractGuards.optionalText(regionId);
            phase = AdapterContractGuards.requireText(phase, "weather warning result phase");
            channel = AdapterContractGuards.requireText(channel, "weather warning result channel");
            message = AdapterContractGuards.requireText(message, "weather warning result message");
            recipientPlayerIds = immutableTextList(recipientPlayerIds, "weather warning result recipient player id");
            hudState = hudState == null ? Map.of() : Map.copyOf(hudState);
            audioState = audioState == null ? Map.of() : Map.copyOf(audioState);
            renderState = renderState == null ? Map.of() : Map.copyOf(renderState);
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (recipientCount < 0 || gameTick < 0L) {
                throw new IllegalArgumentException("weather warning result recipient count and tick must not be negative");
            }
        }
    }

    public record EchoEmergencySirenUseRequest(
            String playerId,
            List<String> weatherIds,
            boolean activeWeatherDetected,
            String phase,
            String severity,
            int x,
            int y,
            int z,
            long gameTick,
            String sourceReason) {
        public EchoEmergencySirenUseRequest {
            playerId = AdapterContractGuards.optionalText(playerId);
            weatherIds = immutableTextList(weatherIds, "emergency siren weather id");
            phase = AdapterContractGuards.requireText(phase, "emergency siren phase");
            severity = AdapterContractGuards.requireText(severity, "emergency siren severity");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (gameTick < 0L) {
                throw new IllegalArgumentException("emergency siren game tick must not be negative");
            }
        }
    }

    public record EchoEmergencySirenUseResult(
            String playerId,
            List<String> weatherIds,
            boolean activeWeatherDetected,
            String phase,
            String severity,
            int x,
            int y,
            int z,
            String message,
            Map<String, Object> hudState,
            Map<String, Object> audioState,
            Map<String, Object> renderState,
            long gameTick,
            String sourceReason,
            boolean delivered) {
        public EchoEmergencySirenUseResult {
            playerId = AdapterContractGuards.optionalText(playerId);
            weatherIds = immutableTextList(weatherIds, "emergency siren result weather id");
            phase = AdapterContractGuards.requireText(phase, "emergency siren result phase");
            severity = AdapterContractGuards.requireText(severity, "emergency siren result severity");
            message = AdapterContractGuards.requireText(message, "emergency siren result message");
            hudState = hudState == null ? Map.of() : Map.copyOf(hudState);
            audioState = audioState == null ? Map.of() : Map.copyOf(audioState);
            renderState = renderState == null ? Map.of() : Map.copyOf(renderState);
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (gameTick < 0L) {
                throw new IllegalArgumentException("emergency siren result game tick must not be negative");
            }
        }
    }

    public record EchoClimateSensorReadRequest(
            String playerId,
            List<String> weatherIds,
            boolean sheltered,
            double visibilityMultiplier,
            double scannerReliabilityMultiplier,
            double filterDrainMultiplier,
            double toxicExposureMultiplier,
            double routeRiskModifier,
            int x,
            int y,
            int z,
            long gameTick,
            String sourceReason) {
        public EchoClimateSensorReadRequest {
            playerId = AdapterContractGuards.optionalText(playerId);
            weatherIds = immutableTextList(weatherIds, "climate sensor weather id");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (visibilityMultiplier < 0.0D || scannerReliabilityMultiplier < 0.0D || filterDrainMultiplier < 0.0D
                    || toxicExposureMultiplier < 0.0D || routeRiskModifier < 0.0D || gameTick < 0L) {
                throw new IllegalArgumentException("climate sensor multipliers and tick must not be negative");
            }
        }
    }

    public record EchoClimateSensorReadResult(
            String playerId,
            List<String> weatherIds,
            boolean sheltered,
            int visibilityPercent,
            int scannerReliabilityPercent,
            double filterDrainMultiplier,
            double toxicExposureMultiplier,
            double routeRiskModifier,
            int x,
            int y,
            int z,
            List<String> messageLines,
            Map<String, Object> hudState,
            Map<String, Object> audioState,
            Map<String, Object> renderState,
            long gameTick,
            String sourceReason,
            boolean delivered) {
        public EchoClimateSensorReadResult {
            playerId = AdapterContractGuards.optionalText(playerId);
            weatherIds = immutableTextList(weatherIds, "climate sensor result weather id");
            messageLines = immutableTextList(messageLines, "climate sensor message line");
            hudState = hudState == null ? Map.of() : Map.copyOf(hudState);
            audioState = audioState == null ? Map.of() : Map.copyOf(audioState);
            renderState = renderState == null ? Map.of() : Map.copyOf(renderState);
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (visibilityPercent < 0 || scannerReliabilityPercent < 0 || filterDrainMultiplier < 0.0D
                    || toxicExposureMultiplier < 0.0D || routeRiskModifier < 0.0D || gameTick < 0L) {
                throw new IllegalArgumentException("climate sensor result values and tick must not be negative");
            }
        }
    }

    public record EchoWeatherRadioUseRequest(
            String playerId,
            List<String> weatherIds,
            List<String> forecastLines,
            boolean forecastsAvailable,
            String strongestSeverity,
            String routeRisk,
            int cooldownTicks,
            long gameTick,
            String sourceReason) {
        public EchoWeatherRadioUseRequest {
            playerId = AdapterContractGuards.optionalText(playerId);
            weatherIds = immutableTextList(weatherIds, "weather radio weather id");
            forecastLines = immutableTextList(forecastLines, "weather radio forecast line");
            strongestSeverity = AdapterContractGuards.requireText(strongestSeverity, "weather radio strongest severity");
            routeRisk = AdapterContractGuards.requireText(routeRisk, "weather radio route risk");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (cooldownTicks < 0 || gameTick < 0L) {
                throw new IllegalArgumentException("weather radio cooldown and tick must not be negative");
            }
        }
    }

    public record EchoWeatherRadioUseResult(
            String playerId,
            List<String> weatherIds,
            List<String> forecastLines,
            boolean forecastsAvailable,
            String strongestSeverity,
            String routeRisk,
            int cooldownTicks,
            List<String> messageLines,
            Map<String, Object> hudState,
            Map<String, Object> audioState,
            Map<String, Object> renderState,
            long gameTick,
            String sourceReason,
            boolean delivered) {
        public EchoWeatherRadioUseResult {
            playerId = AdapterContractGuards.optionalText(playerId);
            weatherIds = immutableTextList(weatherIds, "weather radio result weather id");
            forecastLines = immutableTextList(forecastLines, "weather radio result forecast line");
            strongestSeverity = AdapterContractGuards.requireText(strongestSeverity,
                    "weather radio result strongest severity");
            routeRisk = AdapterContractGuards.requireText(routeRisk, "weather radio result route risk");
            messageLines = immutableTextList(messageLines, "weather radio result message line");
            hudState = hudState == null ? Map.of() : Map.copyOf(hudState);
            audioState = audioState == null ? Map.of() : Map.copyOf(audioState);
            renderState = renderState == null ? Map.of() : Map.copyOf(renderState);
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (cooldownTicks < 0 || gameTick < 0L) {
                throw new IllegalArgumentException("weather radio result cooldown and tick must not be negative");
            }
        }
    }

    public record EchoWeatherStationUseRequest(
            String playerId,
            List<String> weatherIds,
            List<String> forecastLines,
            boolean forecastsAvailable,
            String strongestSeverity,
            String routeRisk,
            int x,
            int y,
            int z,
            long gameTick,
            String sourceReason) {
        public EchoWeatherStationUseRequest {
            playerId = AdapterContractGuards.optionalText(playerId);
            weatherIds = immutableTextList(weatherIds, "weather station weather id");
            forecastLines = immutableTextList(forecastLines, "weather station forecast line");
            strongestSeverity = AdapterContractGuards.requireText(strongestSeverity,
                    "weather station strongest severity");
            routeRisk = AdapterContractGuards.requireText(routeRisk, "weather station route risk");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (gameTick < 0L) {
                throw new IllegalArgumentException("weather station tick must not be negative");
            }
        }
    }

    public record EchoWeatherStationUseResult(
            String playerId,
            List<String> weatherIds,
            List<String> forecastLines,
            boolean forecastsAvailable,
            String strongestSeverity,
            String routeRisk,
            int x,
            int y,
            int z,
            List<String> messageLines,
            Map<String, Object> hudState,
            Map<String, Object> audioState,
            Map<String, Object> renderState,
            long gameTick,
            String sourceReason,
            boolean delivered) {
        public EchoWeatherStationUseResult {
            playerId = AdapterContractGuards.optionalText(playerId);
            weatherIds = immutableTextList(weatherIds, "weather station result weather id");
            forecastLines = immutableTextList(forecastLines, "weather station result forecast line");
            strongestSeverity = AdapterContractGuards.requireText(strongestSeverity,
                    "weather station result strongest severity");
            routeRisk = AdapterContractGuards.requireText(routeRisk, "weather station result route risk");
            messageLines = immutableTextList(messageLines, "weather station result message line");
            hudState = hudState == null ? Map.of() : Map.copyOf(hudState);
            audioState = audioState == null ? Map.of() : Map.copyOf(audioState);
            renderState = renderState == null ? Map.of() : Map.copyOf(renderState);
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (gameTick < 0L) {
                throw new IllegalArgumentException("weather station result tick must not be negative");
            }
        }
    }

    public record EchoAtmosphereState(
            String id,
            double visibility,
            String particleProfile,
            String skyFog) {
        public EchoAtmosphereState {
            id = AdapterContractGuards.requireText(id, "atmosphere state id");
            particleProfile = AdapterContractGuards.requireText(particleProfile, "atmosphere particle profile");
            skyFog = AdapterContractGuards.requireText(skyFog, "atmosphere sky fog");
            if (visibility < 0.0D) {
                throw new IllegalArgumentException("atmosphere visibility must not be negative");
            }
        }
    }

    public record EchoAtmosphereStateApplyRequest(
            String eventId,
            String weatherId,
            String regionId,
            String phase,
            long gameTick,
            String sourceReason,
            EchoAtmosphereState atmosphere) {
        public EchoAtmosphereStateApplyRequest {
            eventId = AdapterContractGuards.requireText(eventId, "atmosphere state apply event id");
            weatherId = AdapterContractGuards.requireText(weatherId, "atmosphere state apply weather id");
            regionId = AdapterContractGuards.optionalText(regionId);
            phase = AdapterContractGuards.requireText(phase, "atmosphere state apply phase");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (gameTick < 0L) {
                throw new IllegalArgumentException("atmosphere state apply game tick must not be negative");
            }
            if (atmosphere == null) {
                throw new IllegalArgumentException("atmosphere state apply atmosphere must not be null");
            }
        }
    }

    public record EchoAtmosphereStateApplyResult(
            String eventId,
            String weatherId,
            String regionId,
            String phase,
            Map<String, Object> renderState,
            Map<String, Object> runtimeBindings,
            long gameTick,
            String sourceReason,
            boolean applied) {
        public EchoAtmosphereStateApplyResult {
            eventId = AdapterContractGuards.requireText(eventId, "atmosphere state apply result event id");
            weatherId = AdapterContractGuards.requireText(weatherId, "atmosphere state apply result weather id");
            regionId = AdapterContractGuards.optionalText(regionId);
            phase = AdapterContractGuards.requireText(phase, "atmosphere state apply result phase");
            renderState = renderState == null ? Map.of() : Map.copyOf(renderState);
            runtimeBindings = runtimeBindings == null ? Map.of() : Map.copyOf(runtimeBindings);
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (gameTick < 0L) {
                throw new IllegalArgumentException("atmosphere state apply result game tick must not be negative");
            }
        }
    }

    public record EchoAtmosphereRuntimeProfileRequest(
            String packId,
            String profileId,
            String weatherStateId,
            String biomeAmbienceId,
            double clearVisibility,
            double stormVisibility,
            double screenHazeIntensity,
            boolean reducesDistantLights,
            String fogId,
            int fogColorArgb,
            double fogDensity,
            double fogStartDistance,
            double fogEndDistance,
            boolean stormAffected,
            String skyTintId,
            int dayColorArgb,
            int nightColorArgb,
            int stormColorArgb,
            double celestialVisibility,
            String particleProfileId,
            List<String> particleReferences,
            double particleDensity,
            boolean affectedByStormVisibility,
            String renderCoreHookReference,
            String soundCoreHookReference,
            String weatherProfileReference,
            String runtimePacketConsumer,
            long gameTick,
            String sourceReason) {
        public EchoAtmosphereRuntimeProfileRequest {
            packId = AdapterContractGuards.optionalText(packId);
            profileId = AdapterContractGuards.requireText(profileId, "atmosphere runtime profile id");
            weatherStateId = AdapterContractGuards.requireText(weatherStateId, "atmosphere runtime weather state id");
            biomeAmbienceId = AdapterContractGuards.requireText(biomeAmbienceId,
                    "atmosphere runtime biome ambience id");
            fogId = AdapterContractGuards.requireText(fogId, "atmosphere runtime fog id");
            skyTintId = AdapterContractGuards.requireText(skyTintId, "atmosphere runtime sky tint id");
            particleProfileId = AdapterContractGuards.requireText(particleProfileId,
                    "atmosphere runtime particle profile id");
            particleReferences = immutableTextList(particleReferences, "atmosphere runtime particle reference");
            renderCoreHookReference = AdapterContractGuards.requireText(renderCoreHookReference,
                    "atmosphere runtime render hook");
            soundCoreHookReference = AdapterContractGuards.requireText(soundCoreHookReference,
                    "atmosphere runtime sound hook");
            weatherProfileReference = AdapterContractGuards.requireText(weatherProfileReference,
                    "atmosphere runtime weather profile reference");
            runtimePacketConsumer = AdapterContractGuards.requireText(runtimePacketConsumer,
                    "atmosphere runtime packet consumer");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (clearVisibility < 0.0D || stormVisibility < 0.0D || screenHazeIntensity < 0.0D
                    || fogDensity < 0.0D || fogStartDistance < 0.0D || fogEndDistance < 0.0D
                    || celestialVisibility < 0.0D || particleDensity < 0.0D) {
                throw new IllegalArgumentException("atmosphere runtime profile numeric fields must not be negative");
            }
            if (gameTick < 0L) {
                throw new IllegalArgumentException("atmosphere runtime profile tick must not be negative");
            }
        }
    }

    public record EchoAtmosphereRuntimeProfileResult(
            String packId,
            String profileId,
            String weatherStateId,
            String biomeAmbienceId,
            Map<String, Object> stormVisibilityState,
            Map<String, Object> fogProfileState,
            Map<String, Object> skyTintState,
            Map<String, Object> ambientParticlesState,
            Map<String, Object> hookRefs,
            List<Map<String, String>> runtimeBindings,
            List<String> diagnostics,
            Map<String, Object> runtimeProfileState,
            long gameTick,
            String sourceReason,
            boolean applied) {
        public EchoAtmosphereRuntimeProfileResult {
            packId = AdapterContractGuards.optionalText(packId);
            profileId = AdapterContractGuards.requireText(profileId, "atmosphere runtime result profile id");
            weatherStateId = AdapterContractGuards.requireText(weatherStateId,
                    "atmosphere runtime result weather state id");
            biomeAmbienceId = AdapterContractGuards.requireText(biomeAmbienceId,
                    "atmosphere runtime result biome ambience id");
            stormVisibilityState = stormVisibilityState == null ? Map.of() : Map.copyOf(stormVisibilityState);
            fogProfileState = fogProfileState == null ? Map.of() : Map.copyOf(fogProfileState);
            skyTintState = skyTintState == null ? Map.of() : Map.copyOf(skyTintState);
            ambientParticlesState = ambientParticlesState == null ? Map.of() : Map.copyOf(ambientParticlesState);
            hookRefs = hookRefs == null ? Map.of() : Map.copyOf(hookRefs);
            runtimeBindings = immutableMapList(runtimeBindings);
            diagnostics = immutableTextList(diagnostics, "atmosphere runtime diagnostic");
            runtimeProfileState = runtimeProfileState == null ? Map.of() : Map.copyOf(runtimeProfileState);
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (gameTick < 0L) {
                throw new IllegalArgumentException("atmosphere runtime result tick must not be negative");
            }
        }
    }

    public record EchoWeatherExposureModifier(
            String weatherType,
            double filterDrainMultiplier,
            double radiationExposureMultiplier,
            double toxicExposureMultiplier,
            double coldExposureMultiplier,
            double heatExposureMultiplier,
            double routeRiskModifier) {
        public EchoWeatherExposureModifier {
            weatherType = AdapterContractGuards.requireText(weatherType, "weather exposure modifier type");
            if (filterDrainMultiplier < 0.0D || radiationExposureMultiplier < 0.0D
                    || toxicExposureMultiplier < 0.0D || coldExposureMultiplier < 0.0D
                    || heatExposureMultiplier < 0.0D || routeRiskModifier < 0.0D) {
                throw new IllegalArgumentException("weather exposure modifier values must not be negative");
            }
        }
    }

    public record EchoWeatherExposureMitigationRequest(
            String playerId,
            String weatherId,
            String weatherType,
            boolean sheltered,
            long gameTick,
            String sourceReason,
            EchoWeatherExposureModifier weatherModifier,
            EchoWeatherExposureModifier countermeasureModifier) {
        public EchoWeatherExposureMitigationRequest {
            playerId = AdapterContractGuards.optionalText(playerId);
            weatherId = AdapterContractGuards.requireText(weatherId, "weather exposure weather id");
            weatherType = AdapterContractGuards.requireText(weatherType, "weather exposure weather type");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (gameTick < 0L) {
                throw new IllegalArgumentException("weather exposure game tick must not be negative");
            }
            if (weatherModifier == null || countermeasureModifier == null) {
                throw new IllegalArgumentException("weather exposure modifiers must not be null");
            }
        }
    }

    public record EchoWeatherExposureMitigationResult(
            String playerId,
            String weatherId,
            String weatherType,
            boolean sheltered,
            Map<String, Object> modifierState,
            long gameTick,
            String sourceReason,
            boolean mitigated) {
        public EchoWeatherExposureMitigationResult {
            playerId = AdapterContractGuards.optionalText(playerId);
            weatherId = AdapterContractGuards.requireText(weatherId, "weather exposure result weather id");
            weatherType = AdapterContractGuards.requireText(weatherType, "weather exposure result weather type");
            modifierState = modifierState == null ? Map.of() : Map.copyOf(modifierState);
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (gameTick < 0L) {
                throw new IllegalArgumentException("weather exposure result game tick must not be negative");
            }
        }
    }

    public record EchoWeatherRouteRiskRequest(
            String playerId,
            String weatherId,
            String severity,
            double routeRiskModifier,
            long gameTick,
            String sourceReason) {
        public EchoWeatherRouteRiskRequest {
            playerId = AdapterContractGuards.optionalText(playerId);
            weatherId = AdapterContractGuards.optionalText(weatherId);
            severity = AdapterContractGuards.requireText(severity, "weather route risk severity");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (routeRiskModifier < 0.0D || gameTick < 0L) {
                throw new IllegalArgumentException("weather route risk modifier and tick must not be negative");
            }
        }
    }

    public record EchoWeatherRouteRiskResult(
            String playerId,
            String weatherId,
            String severity,
            double routeRiskModifier,
            double riskScore,
            String risk,
            long gameTick,
            String sourceReason) {
        public EchoWeatherRouteRiskResult {
            playerId = AdapterContractGuards.optionalText(playerId);
            weatherId = AdapterContractGuards.optionalText(weatherId);
            severity = AdapterContractGuards.requireText(severity, "weather route risk result severity");
            risk = AdapterContractGuards.requireText(risk, "weather route risk result risk");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (routeRiskModifier < 0.0D || riskScore < 0.0D || gameTick < 0L) {
                throw new IllegalArgumentException("weather route risk result values must not be negative");
            }
        }
    }

    public record EchoRouteWarningPostUseRequest(
            String playerId,
            String weatherId,
            String severity,
            String risk,
            double routeRiskModifier,
            int x,
            int y,
            int z,
            long gameTick,
            String sourceReason) {
        public EchoRouteWarningPostUseRequest {
            playerId = AdapterContractGuards.optionalText(playerId);
            weatherId = AdapterContractGuards.optionalText(weatherId);
            severity = AdapterContractGuards.requireText(severity, "route warning post severity");
            risk = AdapterContractGuards.requireText(risk, "route warning post risk");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (routeRiskModifier < 0.0D || gameTick < 0L) {
                throw new IllegalArgumentException("route warning post modifier and tick must not be negative");
            }
        }
    }

    public record EchoRouteWarningPostUseResult(
            String playerId,
            String weatherId,
            String severity,
            String risk,
            double routeRiskModifier,
            int x,
            int y,
            int z,
            String message,
            Map<String, Object> hudState,
            Map<String, Object> audioState,
            Map<String, Object> renderState,
            long gameTick,
            String sourceReason,
            boolean delivered) {
        public EchoRouteWarningPostUseResult {
            playerId = AdapterContractGuards.optionalText(playerId);
            weatherId = AdapterContractGuards.optionalText(weatherId);
            severity = AdapterContractGuards.requireText(severity, "route warning post result severity");
            risk = AdapterContractGuards.requireText(risk, "route warning post result risk");
            message = AdapterContractGuards.requireText(message, "route warning post result message");
            hudState = hudState == null ? Map.of() : Map.copyOf(hudState);
            audioState = audioState == null ? Map.of() : Map.copyOf(audioState);
            renderState = renderState == null ? Map.of() : Map.copyOf(renderState);
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (routeRiskModifier < 0.0D || gameTick < 0L) {
                throw new IllegalArgumentException("route warning post result modifier and tick must not be negative");
            }
        }
    }

    public record EchoWeatherForecastRequest(
            String playerId,
            String eventId,
            String weatherId,
            String weatherType,
            String displayName,
            String phase,
            String severity,
            String regionId,
            long gameTick,
            long startTick,
            long endTick,
            long etaTicks,
            double routeRiskModifier,
            double scannerReliabilityMultiplier,
            List<String> recommendedGear,
            String shelterRecommendation,
            List<String> echoLines,
            String sourceReason) {
        public EchoWeatherForecastRequest {
            playerId = AdapterContractGuards.requireText(playerId, "weather forecast player id");
            eventId = AdapterContractGuards.requireText(eventId, "weather forecast event id");
            weatherId = AdapterContractGuards.requireText(weatherId, "weather forecast weather id");
            weatherType = AdapterContractGuards.requireText(weatherType, "weather forecast weather type");
            displayName = AdapterContractGuards.requireText(displayName, "weather forecast display name");
            phase = AdapterContractGuards.requireText(phase, "weather forecast phase");
            severity = AdapterContractGuards.requireText(severity, "weather forecast severity");
            regionId = AdapterContractGuards.optionalText(regionId);
            recommendedGear = recommendedGear == null ? List.of() : List.copyOf(recommendedGear);
            shelterRecommendation = AdapterContractGuards.optionalText(shelterRecommendation);
            echoLines = echoLines == null ? List.of() : List.copyOf(echoLines);
            sourceReason = AdapterContractGuards.requireText(sourceReason, "weather forecast source reason");
            if (gameTick < 0L || startTick < 0L || endTick < 0L || etaTicks < 0L
                    || routeRiskModifier < 0.0D || scannerReliabilityMultiplier < 0.0D) {
                throw new IllegalArgumentException("weather forecast timing and modifiers must not be negative");
            }
        }
    }

    public record EchoWeatherForecastResult(
            String playerId,
            String eventId,
            String weatherId,
            String weatherType,
            String displayName,
            String phase,
            String severity,
            long etaTicks,
            String regionName,
            int durationEstimateTicks,
            List<String> recommendedGear,
            String shelterRecommendation,
            String routeRisk,
            double routeRiskModifier,
            String scannerReliability,
            List<String> echoLines,
            long gameTick,
            String sourceReason,
            boolean forecasted) {
        public EchoWeatherForecastResult {
            playerId = AdapterContractGuards.requireText(playerId, "weather forecast result player id");
            eventId = AdapterContractGuards.requireText(eventId, "weather forecast result event id");
            weatherId = AdapterContractGuards.requireText(weatherId, "weather forecast result weather id");
            weatherType = AdapterContractGuards.requireText(weatherType, "weather forecast result weather type");
            displayName = AdapterContractGuards.requireText(displayName, "weather forecast result display name");
            phase = AdapterContractGuards.requireText(phase, "weather forecast result phase");
            severity = AdapterContractGuards.requireText(severity, "weather forecast result severity");
            regionName = AdapterContractGuards.optionalText(regionName);
            recommendedGear = recommendedGear == null ? List.of() : List.copyOf(recommendedGear);
            shelterRecommendation = AdapterContractGuards.optionalText(shelterRecommendation);
            routeRisk = AdapterContractGuards.requireText(routeRisk, "weather forecast result route risk");
            scannerReliability = AdapterContractGuards.requireText(scannerReliability, "weather forecast scanner reliability");
            echoLines = echoLines == null ? List.of() : List.copyOf(echoLines);
            sourceReason = AdapterContractGuards.requireText(sourceReason, "weather forecast result source reason");
            if (etaTicks < 0L || durationEstimateTicks < 0 || routeRiskModifier < 0.0D || gameTick < 0L) {
                throw new IllegalArgumentException("weather forecast result timing and modifiers must not be negative");
            }
        }
    }

    public record EchoBiomeProfile(String id, String biomeTag, String hazardTag) {
        public EchoBiomeProfile {
            id = AdapterContractGuards.requireText(id, "biome profile id");
            biomeTag = AdapterContractGuards.requireText(biomeTag, "biome profile tag");
            hazardTag = AdapterContractGuards.optionalText(hazardTag);
        }
    }

    public record EchoBiomeAmbientStateRequest(
            String playerId,
            String biomeProfileId,
            String biomeTag,
            String ambienceId,
            String soundProfileId,
            String particleProfileId,
            List<String> ambientAssetIds,
            String atmosphereProfileId,
            double visibilityModifier,
            long gameTick,
            String sourceReason) {
        public EchoBiomeAmbientStateRequest {
            playerId = AdapterContractGuards.requireText(playerId, "biome ambient player id");
            biomeProfileId = AdapterContractGuards.requireText(biomeProfileId, "biome ambient profile id");
            biomeTag = AdapterContractGuards.requireText(biomeTag, "biome ambient tag");
            ambienceId = AdapterContractGuards.requireText(ambienceId, "biome ambience id");
            soundProfileId = AdapterContractGuards.requireText(soundProfileId, "biome ambient sound profile id");
            particleProfileId = AdapterContractGuards.requireText(particleProfileId, "biome ambient particle profile id");
            ambientAssetIds = immutableTextList(ambientAssetIds, "biome ambient asset id");
            atmosphereProfileId = AdapterContractGuards.requireText(atmosphereProfileId,
                    "biome ambient atmosphere profile id");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (visibilityModifier < 0.0D || gameTick < 0L) {
                throw new IllegalArgumentException("biome ambient visibility and tick must not be negative");
            }
        }
    }

    public record EchoBiomeAmbientStateResult(
            String playerId,
            String biomeProfileId,
            String biomeTag,
            String ambienceId,
            Map<String, Object> hudState,
            Map<String, Object> audioState,
            Map<String, Object> renderState,
            Map<String, Object> ambientState,
            List<Map<String, String>> runtimeBindings,
            long gameTick,
            String sourceReason,
            boolean applied) {
        public EchoBiomeAmbientStateResult {
            playerId = AdapterContractGuards.requireText(playerId, "biome ambient result player id");
            biomeProfileId = AdapterContractGuards.requireText(biomeProfileId, "biome ambient result profile id");
            biomeTag = AdapterContractGuards.requireText(biomeTag, "biome ambient result tag");
            ambienceId = AdapterContractGuards.requireText(ambienceId, "biome ambient result ambience id");
            hudState = hudState == null ? Map.of() : Map.copyOf(hudState);
            audioState = audioState == null ? Map.of() : Map.copyOf(audioState);
            renderState = renderState == null ? Map.of() : Map.copyOf(renderState);
            ambientState = ambientState == null ? Map.of() : Map.copyOf(ambientState);
            runtimeBindings = immutableMapList(runtimeBindings);
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (gameTick < 0L) {
                throw new IllegalArgumentException("biome ambient result tick must not be negative");
            }
        }
    }

    public record EchoBiomeHazardOverlayRequest(
            String playerId,
            String worldId,
            int x,
            int y,
            int z,
            long gameTick,
            String sourceReason,
            EchoBiomeProfile biome,
            EchoWorldHazard hazard,
            boolean inRegion,
            boolean inHazard) {
        public EchoBiomeHazardOverlayRequest {
            playerId = AdapterContractGuards.requireText(playerId, "biome hazard overlay player id");
            worldId = AdapterContractGuards.requireText(worldId, "biome hazard overlay world id");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (gameTick < 0L) {
                throw new IllegalArgumentException("biome hazard overlay game tick must not be negative");
            }
            if (biome == null) {
                throw new IllegalArgumentException("biome hazard overlay biome must not be null");
            }
            if (hazard == null) {
                throw new IllegalArgumentException("biome hazard overlay hazard must not be null");
            }
        }
    }

    public record EchoBiomeHazardOverlayResult(
            String playerId,
            String worldId,
            String biomeProfileId,
            String biomeTag,
            String hazardTag,
            String hazardId,
            String overlayId,
            String cellKey,
            double intensity,
            boolean active,
            boolean visibleOnHud,
            long gameTick,
            String sourceReason) {
        public EchoBiomeHazardOverlayResult {
            playerId = AdapterContractGuards.requireText(playerId, "biome hazard overlay result player id");
            worldId = AdapterContractGuards.requireText(worldId, "biome hazard overlay result world id");
            biomeProfileId = AdapterContractGuards.requireText(biomeProfileId, "biome hazard overlay result biome id");
            biomeTag = AdapterContractGuards.requireText(biomeTag, "biome hazard overlay result biome tag");
            hazardTag = AdapterContractGuards.optionalText(hazardTag);
            hazardId = AdapterContractGuards.optionalText(hazardId);
            overlayId = AdapterContractGuards.requireText(overlayId, "biome hazard overlay result overlay id");
            cellKey = AdapterContractGuards.requireText(cellKey, "biome hazard overlay result cell key");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (intensity < 0.0D || gameTick < 0L) {
                throw new IllegalArgumentException("biome hazard overlay result intensity and tick must not be negative");
            }
        }
    }

    public record EchoStructurePlacement(String id, String poiId, int x, int y, int z) {
        public EchoStructurePlacement {
            id = AdapterContractGuards.requireText(id, "structure placement id");
            poiId = AdapterContractGuards.requireText(poiId, "structure POI id");
        }
    }

    public record EchoWorldDataCatalogRequest(
            List<String> regionIds,
            List<String> hazardIds,
            List<String> weatherProfileIds,
            List<String> biomeIds,
            List<String> structureIds,
            List<String> statusEffectIds,
            List<String> difficultyIds,
            int spawnRuleCount,
            List<String> sourceFiles,
            String sourceReason) {
        public EchoWorldDataCatalogRequest {
            regionIds = immutableTextList(regionIds, "world data catalog region id");
            hazardIds = immutableTextList(hazardIds, "world data catalog hazard id");
            weatherProfileIds = immutableTextList(weatherProfileIds, "world data catalog weather profile id");
            biomeIds = immutableTextList(biomeIds, "world data catalog biome id");
            structureIds = immutableTextList(structureIds, "world data catalog structure id");
            statusEffectIds = immutableTextList(statusEffectIds, "world data catalog status effect id");
            difficultyIds = immutableTextList(difficultyIds, "world data catalog difficulty id");
            sourceFiles = immutableTextList(sourceFiles, "world data catalog source file");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (spawnRuleCount < 0) {
                throw new IllegalArgumentException("world data catalog spawn rule count must not be negative");
            }
        }
    }

    public record EchoWorldDataCatalogResult(
            int regionCount,
            int hazardCount,
            int weatherProfileCount,
            int biomeCount,
            int structureCount,
            int statusEffectCount,
            int difficultyRuleCount,
            int spawnRuleCount,
            int sourceFileCount,
            List<String> representativeRegionIds,
            List<String> representativeHazardIds,
            List<String> representativeWeatherProfileIds,
            List<String> representativeBiomeIds,
            List<String> representativeStructureIds,
            List<String> representativeStatusEffectIds,
            List<String> representativeDifficultyIds,
            String sourceReason,
            boolean loaded) {
        public EchoWorldDataCatalogResult {
            if (regionCount < 0 || hazardCount < 0 || weatherProfileCount < 0 || biomeCount < 0
                    || structureCount < 0 || statusEffectCount < 0 || difficultyRuleCount < 0
                    || spawnRuleCount < 0 || sourceFileCount < 0) {
                throw new IllegalArgumentException("world data catalog counts must not be negative");
            }
            representativeRegionIds = immutableTextList(representativeRegionIds, "world data catalog representative region id");
            representativeHazardIds = immutableTextList(representativeHazardIds, "world data catalog representative hazard id");
            representativeWeatherProfileIds = immutableTextList(representativeWeatherProfileIds,
                    "world data catalog representative weather profile id");
            representativeBiomeIds = immutableTextList(representativeBiomeIds, "world data catalog representative biome id");
            representativeStructureIds = immutableTextList(representativeStructureIds,
                    "world data catalog representative structure id");
            representativeStatusEffectIds = immutableTextList(representativeStatusEffectIds,
                    "world data catalog representative status effect id");
            representativeDifficultyIds = immutableTextList(representativeDifficultyIds,
                    "world data catalog representative difficulty id");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
        }
    }

    public record EchoWorldCellSampleRequest(
            String playerId,
            String worldId,
            int x,
            int y,
            int z,
            long gameTick,
            String sourceReason,
            EchoWorldRegion region,
            EchoWorldHazard hazard,
            EchoBiomeProfile biome,
            EchoStructurePlacement structure) {
        public EchoWorldCellSampleRequest {
            playerId = AdapterContractGuards.requireText(playerId, "world cell sample player id");
            worldId = AdapterContractGuards.requireText(worldId, "world cell sample world id");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (gameTick < 0L) {
                throw new IllegalArgumentException("world cell sample game tick must not be negative");
            }
            if (region == null) {
                throw new IllegalArgumentException("world cell sample region must not be null");
            }
            if (hazard == null) {
                throw new IllegalArgumentException("world cell sample hazard must not be null");
            }
            if (biome == null) {
                throw new IllegalArgumentException("world cell sample biome must not be null");
            }
            if (structure == null) {
                throw new IllegalArgumentException("world cell sample structure must not be null");
            }
        }
    }

    public record EchoWorldCellSampleResult(
            String playerId,
            String worldId,
            String activeRegionId,
            String activeHazardId,
            String biomeProfileId,
            String structureId,
            String poiId,
            String cellKey,
            int x,
            int y,
            int z,
            boolean inRegion,
            boolean inHazard,
            long gameTick,
            String sourceReason) {
        public EchoWorldCellSampleResult {
            playerId = AdapterContractGuards.requireText(playerId, "world cell sample result player id");
            worldId = AdapterContractGuards.requireText(worldId, "world cell sample result world id");
            activeRegionId = AdapterContractGuards.optionalText(activeRegionId);
            activeHazardId = AdapterContractGuards.optionalText(activeHazardId);
            biomeProfileId = AdapterContractGuards.optionalText(biomeProfileId);
            structureId = AdapterContractGuards.optionalText(structureId);
            poiId = AdapterContractGuards.optionalText(poiId);
            cellKey = AdapterContractGuards.requireText(cellKey, "world cell sample result cell key");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (gameTick < 0L) {
                throw new IllegalArgumentException("world cell sample result game tick must not be negative");
            }
        }
    }

    public record EchoWorldChunkStateRequest(
            String playerId,
            String worldId,
            int x,
            int y,
            int z,
            long gameTick,
            String sourceReason,
            EchoWorldCellSampleResult cellSample) {
        public EchoWorldChunkStateRequest {
            playerId = AdapterContractGuards.requireText(playerId, "world chunk state player id");
            worldId = AdapterContractGuards.requireText(worldId, "world chunk state world id");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (gameTick < 0L) {
                throw new IllegalArgumentException("world chunk state game tick must not be negative");
            }
            if (cellSample == null) {
                throw new IllegalArgumentException("world chunk state cell sample must not be null");
            }
        }
    }

    public record EchoWorldChunkStateResult(
            String playerId,
            String worldId,
            String chunkKey,
            int chunkX,
            int chunkZ,
            String lastCellKey,
            int lastSampleX,
            int lastSampleY,
            int lastSampleZ,
            String activeRegionId,
            String activeHazardId,
            String biomeProfileId,
            String structureId,
            String poiId,
            boolean inRegion,
            boolean inHazard,
            long lastGameTick,
            String sourceReason) {
        public EchoWorldChunkStateResult {
            playerId = AdapterContractGuards.requireText(playerId, "world chunk state result player id");
            worldId = AdapterContractGuards.requireText(worldId, "world chunk state result world id");
            chunkKey = AdapterContractGuards.requireText(chunkKey, "world chunk state result chunk key");
            lastCellKey = AdapterContractGuards.requireText(lastCellKey, "world chunk state result cell key");
            activeRegionId = AdapterContractGuards.optionalText(activeRegionId);
            activeHazardId = AdapterContractGuards.optionalText(activeHazardId);
            biomeProfileId = AdapterContractGuards.optionalText(biomeProfileId);
            structureId = AdapterContractGuards.optionalText(structureId);
            poiId = AdapterContractGuards.optionalText(poiId);
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (lastGameTick < 0L) {
                throw new IllegalArgumentException("world chunk state result tick must not be negative");
            }
        }
    }

    public record EchoHazardFieldStateRequest(
            String playerId,
            String worldId,
            long gameTick,
            String sourceReason,
            EchoWorldHazard hazard,
            EchoWorldCellSampleResult cellSample) {
        public EchoHazardFieldStateRequest {
            playerId = AdapterContractGuards.requireText(playerId, "hazard field state player id");
            worldId = AdapterContractGuards.requireText(worldId, "hazard field state world id");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (gameTick < 0L) {
                throw new IllegalArgumentException("hazard field state game tick must not be negative");
            }
            if (hazard == null) {
                throw new IllegalArgumentException("hazard field state hazard must not be null");
            }
            if (cellSample == null) {
                throw new IllegalArgumentException("hazard field state cell sample must not be null");
            }
        }
    }

    public record EchoHazardFieldStateResult(
            String playerId,
            String worldId,
            String hazardId,
            String type,
            int centerX,
            int centerZ,
            int radius,
            double damagePerTick,
            String statusEffectId,
            String lastCellKey,
            boolean sampledInside,
            long lastGameTick,
            String sourceReason) {
        public EchoHazardFieldStateResult {
            playerId = AdapterContractGuards.requireText(playerId, "hazard field state result player id");
            worldId = AdapterContractGuards.requireText(worldId, "hazard field state result world id");
            hazardId = AdapterContractGuards.requireText(hazardId, "hazard field state result hazard id");
            type = AdapterContractGuards.requireText(type, "hazard field state result type");
            statusEffectId = AdapterContractGuards.optionalText(statusEffectId);
            lastCellKey = AdapterContractGuards.requireText(lastCellKey, "hazard field state result cell key");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (radius < 0 || damagePerTick < 0.0D || lastGameTick < 0L) {
                throw new IllegalArgumentException("hazard field state result radius, damage, and tick must not be negative");
            }
        }
    }

    public record EchoStructurePoiLookupRequest(
            String playerId,
            String regionId,
            int playerX,
            int playerY,
            int playerZ,
            int maxDistance,
            long gameTick,
            String sourceReason,
            EchoStructurePlacement structure) {
        public EchoStructurePoiLookupRequest {
            playerId = AdapterContractGuards.requireText(playerId, "structure POI lookup player id");
            regionId = AdapterContractGuards.requireText(regionId, "structure POI lookup region id");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (maxDistance < 0 || gameTick < 0L) {
                throw new IllegalArgumentException("structure POI lookup distance and tick must not be negative");
            }
            if (structure == null) {
                throw new IllegalArgumentException("structure POI lookup structure must not be null");
            }
        }
    }

    public record EchoStructurePoiLookupResult(
            String playerId,
            String regionId,
            String structureId,
            String poiId,
            int x,
            int y,
            int z,
            long distanceSquared,
            int maxDistance,
            boolean inRange,
            String markerId,
            String lookupType,
            long gameTick,
            String sourceReason) {
        public EchoStructurePoiLookupResult {
            playerId = AdapterContractGuards.requireText(playerId, "structure POI lookup result player id");
            regionId = AdapterContractGuards.requireText(regionId, "structure POI lookup result region id");
            structureId = AdapterContractGuards.requireText(structureId, "structure POI lookup result structure id");
            poiId = AdapterContractGuards.requireText(poiId, "structure POI lookup result POI id");
            markerId = AdapterContractGuards.requireText(markerId, "structure POI lookup result marker id");
            lookupType = AdapterContractGuards.requireText(lookupType, "structure POI lookup result type");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (distanceSquared < 0L || maxDistance < 0 || gameTick < 0L) {
                throw new IllegalArgumentException("structure POI lookup result distance and tick must not be negative");
            }
        }
    }

    public record EchoStructurePoiMarkerStateRequest(
            String playerId,
            String sourceReason,
            EchoStructurePoiLookupResult lookup) {
        public EchoStructurePoiMarkerStateRequest {
            playerId = AdapterContractGuards.requireText(playerId, "structure POI marker state player id");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (lookup == null) {
                throw new IllegalArgumentException("structure POI marker state lookup must not be null");
            }
        }
    }

    public record EchoStructurePoiMarkerStateResult(
            String playerId,
            String markerId,
            String regionId,
            String structureId,
            String poiId,
            int x,
            int y,
            int z,
            long distanceSquared,
            int maxDistance,
            boolean inRange,
            boolean markerPersisted,
            String lookupType,
            long lastGameTick,
            String sourceReason) {
        public EchoStructurePoiMarkerStateResult {
            playerId = AdapterContractGuards.requireText(playerId, "structure POI marker state result player id");
            markerId = AdapterContractGuards.requireText(markerId, "structure POI marker state result marker id");
            regionId = AdapterContractGuards.requireText(regionId, "structure POI marker state result region id");
            structureId = AdapterContractGuards.requireText(structureId, "structure POI marker state result structure id");
            poiId = AdapterContractGuards.requireText(poiId, "structure POI marker state result POI id");
            lookupType = AdapterContractGuards.requireText(lookupType, "structure POI marker state result type");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (distanceSquared < 0L || maxDistance < 0 || lastGameTick < 0L) {
                throw new IllegalArgumentException("structure POI marker state result distance and tick must not be negative");
            }
        }
    }

    public record EchoStructureDiscoveryStateRequest(
            String playerId,
            String sourceReason,
            EchoStructurePoiMarkerStateResult markerState) {
        public EchoStructureDiscoveryStateRequest {
            playerId = AdapterContractGuards.requireText(playerId, "structure discovery state player id");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (markerState == null) {
                throw new IllegalArgumentException("structure discovery marker state must not be null");
            }
        }
    }

    public record EchoStructureDiscoveryStateResult(
            String playerId,
            String markerId,
            String regionId,
            String structureId,
            String poiId,
            String previousDiscoveryState,
            String discoveryState,
            boolean discovered,
            boolean firstDiscovery,
            boolean holomapMarkerActive,
            long lastGameTick,
            String sourceReason) {
        public EchoStructureDiscoveryStateResult {
            playerId = AdapterContractGuards.requireText(playerId, "structure discovery state result player id");
            markerId = AdapterContractGuards.requireText(markerId, "structure discovery state result marker id");
            regionId = AdapterContractGuards.requireText(regionId, "structure discovery state result region id");
            structureId = AdapterContractGuards.requireText(structureId, "structure discovery state result structure id");
            poiId = AdapterContractGuards.requireText(poiId, "structure discovery state result POI id");
            previousDiscoveryState = AdapterContractGuards.requireText(previousDiscoveryState,
                    "structure discovery previous state");
            discoveryState = AdapterContractGuards.requireText(discoveryState, "structure discovery state");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (lastGameTick < 0L) {
                throw new IllegalArgumentException("structure discovery state result tick must not be negative");
            }
        }
    }

    public record EchoSpawnRule(String id, String entityId, String regionId, int maxCount, double difficultyWeight) {
        public EchoSpawnRule {
            id = AdapterContractGuards.requireText(id, "spawn rule id");
            entityId = AdapterContractGuards.requireText(entityId, "spawn entity id");
            regionId = AdapterContractGuards.requireText(regionId, "spawn region id");
            if (maxCount < 0 || difficultyWeight < 0.0D) {
                throw new IllegalArgumentException("spawn rule max count and difficulty weight must not be negative");
            }
        }
    }

    public record EchoStatusEffect(String id, int durationTicks, int amplifier, String saveKey) {
        public EchoStatusEffect {
            id = AdapterContractGuards.requireText(id, "status effect id");
            saveKey = AdapterContractGuards.requireText(saveKey, "status effect save key");
            if (durationTicks < 0 || amplifier < 0) {
                throw new IllegalArgumentException("status effect duration and amplifier must not be negative");
            }
        }
    }

    public record EchoStatusExposureMitigationRequest(
            String playerId,
            String exposureId,
            String hazardId,
            EchoStatusEffect statusEffect,
            String statusKind,
            double exposureIntensity,
            int durationTicks,
            double accumulationPerSecond,
            String resistanceId,
            double mitigationRatio,
            double immunityThreshold,
            long gameTick,
            String sourceReason) {
        public EchoStatusExposureMitigationRequest {
            playerId = AdapterContractGuards.requireText(playerId, "status exposure mitigation player id");
            exposureId = AdapterContractGuards.requireText(exposureId, "status exposure mitigation exposure id");
            hazardId = AdapterContractGuards.requireText(hazardId, "status exposure mitigation hazard id");
            statusKind = AdapterContractGuards.requireText(statusKind, "status exposure mitigation kind");
            resistanceId = AdapterContractGuards.requireText(resistanceId, "status exposure mitigation resistance id");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (statusEffect == null) {
                throw new IllegalArgumentException("status exposure mitigation status effect must not be null");
            }
            if (exposureIntensity < 0.0D || durationTicks < 0 || accumulationPerSecond < 0.0D
                    || mitigationRatio < 0.0D || mitigationRatio > 1.0D || immunityThreshold < 0.0D
                    || gameTick < 0L) {
                throw new IllegalArgumentException("status exposure mitigation request values are out of range");
            }
        }
    }

    public record EchoStatusExposureMitigationResult(
            String playerId,
            String exposureId,
            String hazardId,
            String effectId,
            String statusKind,
            double originalIntensity,
            double effectiveIntensity,
            int originalDurationTicks,
            int effectiveDurationTicks,
            double originalAccumulationPerSecond,
            double effectiveAccumulationPerSecond,
            String resistanceId,
            double mitigationRatio,
            double immunityThreshold,
            boolean immune,
            Map<String, Object> exposureState,
            long gameTick,
            String sourceReason,
            boolean applied) {
        public EchoStatusExposureMitigationResult {
            playerId = AdapterContractGuards.requireText(playerId, "status exposure mitigation result player id");
            exposureId = AdapterContractGuards.requireText(exposureId, "status exposure mitigation result exposure id");
            hazardId = AdapterContractGuards.requireText(hazardId, "status exposure mitigation result hazard id");
            effectId = AdapterContractGuards.requireText(effectId, "status exposure mitigation result effect id");
            statusKind = AdapterContractGuards.requireText(statusKind, "status exposure mitigation result kind");
            resistanceId = AdapterContractGuards.requireText(resistanceId, "status exposure mitigation result resistance id");
            exposureState = exposureState == null ? Map.of() : Map.copyOf(exposureState);
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (originalIntensity < 0.0D || effectiveIntensity < 0.0D || originalDurationTicks < 0
                    || effectiveDurationTicks < 0 || originalAccumulationPerSecond < 0.0D
                    || effectiveAccumulationPerSecond < 0.0D || mitigationRatio < 0.0D
                    || mitigationRatio > 1.0D || immunityThreshold < 0.0D || gameTick < 0L) {
                throw new IllegalArgumentException("status exposure mitigation result values are out of range");
            }
        }
    }

    public record EchoStatusEffectApplyRequest(
            String playerId,
            String hazardId,
            float damageApplied,
            long gameTick,
            String sourceReason,
            EchoStatusEffect statusEffect,
            boolean loaded) {
        public EchoStatusEffectApplyRequest {
            playerId = AdapterContractGuards.requireText(playerId, "status effect apply player id");
            hazardId = AdapterContractGuards.requireText(hazardId, "status effect apply hazard id");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (damageApplied < 0.0F || gameTick < 0L) {
                throw new IllegalArgumentException("status effect apply damage and tick must not be negative");
            }
            if (statusEffect == null) {
                throw new IllegalArgumentException("status effect apply status effect must not be null");
            }
        }
    }

    public record EchoStatusEffectApplyResult(
            String playerId,
            String hazardId,
            String effectId,
            int durationTicks,
            int amplifier,
            String saveKey,
            float damageApplied,
            long appliedGameTick,
            long expiresAtTick,
            Map<String, Object> activeStatusState,
            String sourceReason,
            boolean loaded,
            boolean applied) {
        public EchoStatusEffectApplyResult {
            playerId = AdapterContractGuards.requireText(playerId, "status effect apply result player id");
            hazardId = AdapterContractGuards.requireText(hazardId, "status effect apply result hazard id");
            effectId = AdapterContractGuards.requireText(effectId, "status effect apply result effect id");
            saveKey = AdapterContractGuards.requireText(saveKey, "status effect apply result save key");
            activeStatusState = activeStatusState == null ? Map.of() : Map.copyOf(activeStatusState);
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (durationTicks < 0 || amplifier < 0 || damageApplied < 0.0F
                    || appliedGameTick < 0L || expiresAtTick < 0L) {
                throw new IllegalArgumentException("status effect apply result values must not be negative");
            }
        }
    }

    public record EchoStatusEffectStackingRequest(
            String playerId,
            String hazardId,
            String stackingPolicy,
            int previousDurationTicks,
            int previousAmplifier,
            double previousDamageApplied,
            long previousAppliedGameTick,
            long previousExpiresAtTick,
            float damageApplied,
            long gameTick,
            String sourceReason,
            EchoStatusEffect statusEffect,
            boolean hadPrevious,
            boolean loaded) {
        public EchoStatusEffectStackingRequest {
            playerId = AdapterContractGuards.requireText(playerId, "status effect stacking player id");
            hazardId = AdapterContractGuards.requireText(hazardId, "status effect stacking hazard id");
            stackingPolicy = AdapterContractGuards.requireText(stackingPolicy, "status effect stacking policy");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (previousDurationTicks < 0 || previousAmplifier < 0 || previousDamageApplied < 0.0D
                    || previousAppliedGameTick < 0L || previousExpiresAtTick < 0L
                    || damageApplied < 0.0F || gameTick < 0L) {
                throw new IllegalArgumentException("status effect stacking request values must not be negative");
            }
            if (statusEffect == null) {
                throw new IllegalArgumentException("status effect stacking status effect must not be null");
            }
        }
    }

    public record EchoStatusEffectStackingResult(
            String playerId,
            String hazardId,
            String effectId,
            String saveKey,
            String stackingPolicy,
            int durationTicks,
            int amplifier,
            double damageApplied,
            long appliedGameTick,
            long expiresAtTick,
            boolean hadPrevious,
            boolean refreshed,
            boolean amplifierUpgraded,
            boolean stacked,
            boolean retained,
            boolean loaded,
            String sourceReason) {
        public EchoStatusEffectStackingResult {
            playerId = AdapterContractGuards.requireText(playerId, "status effect stacking result player id");
            hazardId = AdapterContractGuards.requireText(hazardId, "status effect stacking result hazard id");
            effectId = AdapterContractGuards.requireText(effectId, "status effect stacking result effect id");
            saveKey = AdapterContractGuards.requireText(saveKey, "status effect stacking result save key");
            stackingPolicy = AdapterContractGuards.requireText(stackingPolicy, "status effect stacking result policy");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (durationTicks < 0 || amplifier < 0 || damageApplied < 0.0D
                    || appliedGameTick < 0L || expiresAtTick < 0L) {
                throw new IllegalArgumentException("status effect stacking result values must not be negative");
            }
        }
    }

    public record EchoStatusEffectSaveRequest(
            String playerId,
            String hazardId,
            float damageApplied,
            long gameTick,
            String sourceReason,
            EchoStatusEffect statusEffect) {
        public EchoStatusEffectSaveRequest {
            playerId = AdapterContractGuards.requireText(playerId, "status effect save player id");
            hazardId = AdapterContractGuards.requireText(hazardId, "status effect save hazard id");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (damageApplied < 0.0F || gameTick < 0L) {
                throw new IllegalArgumentException("status effect save damage and tick must not be negative");
            }
            if (statusEffect == null) {
                throw new IllegalArgumentException("status effect save status effect must not be null");
            }
        }
    }

    public record EchoStatusEffectSaveResult(
            String playerId,
            String hazardId,
            String effectId,
            int durationTicks,
            int amplifier,
            String saveKey,
            float damageApplied,
            long gameTick,
            Map<String, Object> savedStatusState,
            String sourceReason,
            boolean saved) {
        public EchoStatusEffectSaveResult {
            playerId = AdapterContractGuards.requireText(playerId, "status effect save result player id");
            hazardId = AdapterContractGuards.requireText(hazardId, "status effect save result hazard id");
            effectId = AdapterContractGuards.requireText(effectId, "status effect save result effect id");
            saveKey = AdapterContractGuards.requireText(saveKey, "status effect save result save key");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            savedStatusState = savedStatusState == null ? Map.of() : Map.copyOf(savedStatusState);
            if (durationTicks < 0 || amplifier < 0 || damageApplied < 0.0F || gameTick < 0L) {
                throw new IllegalArgumentException("status effect save result values must not be negative");
            }
        }
    }

    public record EchoStatusEffectLoadRequest(
            String playerId,
            String hazardId,
            String saveKey,
            Map<String, Object> savedStatusState,
            long gameTick,
            String sourceReason) {
        public EchoStatusEffectLoadRequest {
            playerId = AdapterContractGuards.requireText(playerId, "status effect load player id");
            hazardId = AdapterContractGuards.requireText(hazardId, "status effect load hazard id");
            saveKey = AdapterContractGuards.requireText(saveKey, "status effect load save key");
            savedStatusState = savedStatusState == null ? Map.of() : Map.copyOf(savedStatusState);
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (gameTick < 0L) {
                throw new IllegalArgumentException("status effect load tick must not be negative");
            }
        }
    }

    public record EchoStatusEffectLoadResult(
            String playerId,
            String hazardId,
            String effectId,
            int durationTicks,
            int amplifier,
            String saveKey,
            float damageApplied,
            long savedGameTick,
            long loadedGameTick,
            String sourceReason,
            boolean loaded) {
        public EchoStatusEffectLoadResult {
            playerId = AdapterContractGuards.requireText(playerId, "status effect load result player id");
            hazardId = AdapterContractGuards.requireText(hazardId, "status effect load result hazard id");
            effectId = AdapterContractGuards.optionalText(effectId);
            saveKey = AdapterContractGuards.requireText(saveKey, "status effect load result save key");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (durationTicks < 0 || amplifier < 0 || damageApplied < 0.0F || savedGameTick < 0L || loadedGameTick < 0L) {
                throw new IllegalArgumentException("status effect load result values must not be negative");
            }
        }
    }

    public record EchoStatusEffectExpiryRequest(
            String playerId,
            String hazardId,
            String effectId,
            String saveKey,
            long appliedGameTick,
            long expiresAtTick,
            long gameTick,
            String sourceReason) {
        public EchoStatusEffectExpiryRequest {
            playerId = AdapterContractGuards.requireText(playerId, "status effect expiry player id");
            hazardId = AdapterContractGuards.requireText(hazardId, "status effect expiry hazard id");
            effectId = AdapterContractGuards.requireText(effectId, "status effect expiry effect id");
            saveKey = AdapterContractGuards.requireText(saveKey, "status effect expiry save key");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (appliedGameTick < 0L || expiresAtTick < 0L || gameTick < 0L) {
                throw new IllegalArgumentException("status effect expiry ticks must not be negative");
            }
        }
    }

    public record EchoStatusEffectExpiryResult(
            String playerId,
            String hazardId,
            String effectId,
            String saveKey,
            long appliedGameTick,
            long expiresAtTick,
            long gameTick,
            boolean expired,
            boolean retained,
            String sourceReason) {
        public EchoStatusEffectExpiryResult {
            playerId = AdapterContractGuards.requireText(playerId, "status effect expiry result player id");
            hazardId = AdapterContractGuards.requireText(hazardId, "status effect expiry result hazard id");
            effectId = AdapterContractGuards.requireText(effectId, "status effect expiry result effect id");
            saveKey = AdapterContractGuards.requireText(saveKey, "status effect expiry result save key");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (appliedGameTick < 0L || expiresAtTick < 0L || gameTick < 0L) {
                throw new IllegalArgumentException("status effect expiry result ticks must not be negative");
            }
            if (expired == retained) {
                throw new IllegalArgumentException("status effect expiry result must be either expired or retained");
            }
        }
    }

    public record EchoDifficultyProfileSelectionRequest(
            String playerId,
            String regionId,
            String missionId,
            String requestedDifficulty,
            long gameTick,
            String sourceReason) {
        public EchoDifficultyProfileSelectionRequest {
            playerId = AdapterContractGuards.optionalText(playerId);
            regionId = AdapterContractGuards.optionalText(regionId);
            missionId = AdapterContractGuards.optionalText(missionId);
            requestedDifficulty = AdapterContractGuards.requireText(requestedDifficulty,
                    "difficulty profile selection requested difficulty");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (gameTick < 0L) {
                throw new IllegalArgumentException("difficulty profile selection tick must not be negative");
            }
        }
    }

    public record EchoDifficultyProfileSelectionResult(
            String playerId,
            String regionId,
            String missionId,
            String requestedDifficulty,
            String selectedDifficulty,
            String difficultyId,
            double hazardMultiplier,
            double spawnMultiplier,
            long gameTick,
            String sourceReason,
            boolean selected) {
        public EchoDifficultyProfileSelectionResult {
            playerId = AdapterContractGuards.optionalText(playerId);
            regionId = AdapterContractGuards.optionalText(regionId);
            missionId = AdapterContractGuards.optionalText(missionId);
            requestedDifficulty = AdapterContractGuards.requireText(requestedDifficulty,
                    "difficulty profile selection result requested difficulty");
            selectedDifficulty = AdapterContractGuards.requireText(selectedDifficulty,
                    "difficulty profile selection result selected difficulty");
            difficultyId = AdapterContractGuards.requireText(difficultyId,
                    "difficulty profile selection result difficulty id");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (hazardMultiplier < 0.0D || spawnMultiplier < 0.0D || gameTick < 0L) {
                throw new IllegalArgumentException("difficulty profile selection result values must not be negative");
            }
        }
    }

    public record EchoDifficultyProfile(String id, double hazardMultiplier, double spawnMultiplier) {
        public EchoDifficultyProfile {
            id = AdapterContractGuards.requireText(id, "difficulty profile id");
            if (hazardMultiplier < 0.0D || spawnMultiplier < 0.0D) {
                throw new IllegalArgumentException("difficulty multipliers must not be negative");
            }
        }
    }

    public record EchoDifficultyApplicationRequest(
            String playerId,
            String regionId,
            String appliedHazardId,
            double baseHazardDamage,
            double scaledHazardDamage,
            String appliedSpawnRuleId,
            int maxSpawnCount,
            int scaledSpawnBudget,
            int activeSpawnPopulation,
            long gameTick,
            String sourceReason,
            EchoDifficultyProfile difficulty) {
        public EchoDifficultyApplicationRequest {
            playerId = AdapterContractGuards.requireText(playerId, "difficulty application player id");
            regionId = AdapterContractGuards.optionalText(regionId);
            appliedHazardId = AdapterContractGuards.optionalText(appliedHazardId);
            appliedSpawnRuleId = AdapterContractGuards.optionalText(appliedSpawnRuleId);
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (baseHazardDamage < 0.0D || scaledHazardDamage < 0.0D || maxSpawnCount < 0
                    || scaledSpawnBudget < 0 || activeSpawnPopulation < 0 || gameTick < 0L) {
                throw new IllegalArgumentException("difficulty application values must not be negative");
            }
            if (difficulty == null) {
                throw new IllegalArgumentException("difficulty application profile must not be null");
            }
        }
    }

    public record EchoDifficultyApplicationResult(
            String playerId,
            String regionId,
            String difficultyId,
            double hazardMultiplier,
            double spawnMultiplier,
            String appliedHazardId,
            double baseHazardDamage,
            double scaledHazardDamage,
            String appliedSpawnRuleId,
            int maxSpawnCount,
            int scaledSpawnBudget,
            int activeSpawnPopulation,
            long lastGameTick,
            String sourceReason,
            boolean applied) {
        public EchoDifficultyApplicationResult {
            playerId = AdapterContractGuards.requireText(playerId, "difficulty application result player id");
            regionId = AdapterContractGuards.optionalText(regionId);
            difficultyId = AdapterContractGuards.requireText(difficultyId, "difficulty application result difficulty id");
            appliedHazardId = AdapterContractGuards.optionalText(appliedHazardId);
            appliedSpawnRuleId = AdapterContractGuards.optionalText(appliedSpawnRuleId);
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (hazardMultiplier < 0.0D || spawnMultiplier < 0.0D || baseHazardDamage < 0.0D
                    || scaledHazardDamage < 0.0D || maxSpawnCount < 0 || scaledSpawnBudget < 0
                    || activeSpawnPopulation < 0 || lastGameTick < 0L) {
                throw new IllegalArgumentException("difficulty application result values must not be negative");
            }
        }
    }

    public record EchoSpawnRuleEventRequest(
            String playerId,
            String regionId,
            int x,
            int y,
            int z,
            int activeMobCount,
            long gameTick,
            String sourceReason,
            EchoSpawnRule spawnRule,
            EchoDifficultyProfile difficulty) {
        public EchoSpawnRuleEventRequest {
            playerId = AdapterContractGuards.requireText(playerId, "spawn rule event player id");
            regionId = AdapterContractGuards.requireText(regionId, "spawn rule event region id");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (activeMobCount < 0 || gameTick < 0L) {
                throw new IllegalArgumentException("spawn rule event active count and tick must not be negative");
            }
            if (spawnRule == null) {
                throw new IllegalArgumentException("spawn rule event spawn rule must not be null");
            }
            if (difficulty == null) {
                throw new IllegalArgumentException("spawn rule event difficulty profile must not be null");
            }
        }
    }

    public record EchoSpawnRuleEventResult(
            String playerId,
            String ruleId,
            String entityId,
            String regionId,
            String difficultyId,
            int maxCount,
            int activeMobCount,
            int scaledBudget,
            int spawnCount,
            double spawnMultiplier,
            double difficultyWeight,
            String eventType,
            int x,
            int y,
            int z,
            long gameTick,
            String sourceReason) {
        public EchoSpawnRuleEventResult {
            playerId = AdapterContractGuards.requireText(playerId, "spawn rule event result player id");
            ruleId = AdapterContractGuards.requireText(ruleId, "spawn rule event result rule id");
            entityId = AdapterContractGuards.requireText(entityId, "spawn rule event result entity id");
            regionId = AdapterContractGuards.requireText(regionId, "spawn rule event result region id");
            difficultyId = AdapterContractGuards.requireText(difficultyId, "spawn rule event result difficulty id");
            eventType = AdapterContractGuards.requireText(eventType, "spawn rule event result type");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (maxCount < 0 || activeMobCount < 0 || scaledBudget < 0 || spawnCount < 0 || gameTick < 0L
                    || spawnMultiplier < 0.0D || difficultyWeight < 0.0D) {
                throw new IllegalArgumentException("spawn rule event result counts, tick, and multipliers must not be negative");
            }
        }
    }

    public record EchoSpawnZoneStateRequest(
            String playerId,
            String sourceReason,
            EchoSpawnRuleEventResult event) {
        public EchoSpawnZoneStateRequest {
            playerId = AdapterContractGuards.requireText(playerId, "spawn zone state player id");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (event == null) {
                throw new IllegalArgumentException("spawn zone state event must not be null");
            }
        }
    }

    public record EchoSpawnZoneStateResult(
            String playerId,
            String regionId,
            String ruleId,
            String zoneKey,
            String entityId,
            String difficultyId,
            int maxCount,
            int activeMobCount,
            int scaledBudget,
            int spawnCount,
            int activePopulation,
            double spawnMultiplier,
            double difficultyWeight,
            String eventType,
            int x,
            int y,
            int z,
            long lastGameTick,
            String sourceReason) {
        public EchoSpawnZoneStateResult {
            playerId = AdapterContractGuards.requireText(playerId, "spawn zone state result player id");
            regionId = AdapterContractGuards.requireText(regionId, "spawn zone state result region id");
            ruleId = AdapterContractGuards.requireText(ruleId, "spawn zone state result rule id");
            zoneKey = AdapterContractGuards.requireText(zoneKey, "spawn zone state result zone key");
            entityId = AdapterContractGuards.requireText(entityId, "spawn zone state result entity id");
            difficultyId = AdapterContractGuards.requireText(difficultyId, "spawn zone state result difficulty id");
            eventType = AdapterContractGuards.requireText(eventType, "spawn zone state result event type");
            sourceReason = AdapterContractGuards.optionalText(sourceReason);
            if (maxCount < 0 || activeMobCount < 0 || scaledBudget < 0 || spawnCount < 0 || activePopulation < 0
                    || spawnMultiplier < 0.0D || difficultyWeight < 0.0D || lastGameTick < 0L) {
                throw new IllegalArgumentException("spawn zone state result counts, multipliers, and tick must not be negative");
            }
        }
    }

    public record EchoWorldEffectTick(
            String playerId,
            int x,
            int y,
            int z,
            double health,
            String previousRegionId,
            EchoWorldRegion region,
            EchoWorldHazard hazard,
            EchoWeatherState weather,
            EchoAtmosphereState atmosphere,
            EchoBiomeProfile biome,
            EchoStructurePlacement structure,
            EchoSpawnRule spawnRule,
            EchoStatusEffect statusEffect,
            EchoDifficultyProfile difficulty) {
        public EchoWorldEffectTick {
            playerId = AdapterContractGuards.requireText(playerId, "world effect player id");
            if (health < 0.0D) {
                throw new IllegalArgumentException("player health must not be negative");
            }
        }
    }

    public record EchoWorldEffectResult(
            String playerId,
            String activeRegionId,
            String activeHazardId,
            double healthBefore,
            double healthAfter,
            List<String> missionEvents,
            List<String> statusEffects,
            Map<String, Object> hudState,
            Map<String, Object> audioState,
            Map<String, Object> renderState,
            Map<String, Object> worldLookup,
            Map<String, Object> spawnEvent,
            Map<String, Object> savedStatusState) {
        public EchoWorldEffectResult {
            playerId = AdapterContractGuards.requireText(playerId, "world effect result player id");
            activeRegionId = AdapterContractGuards.optionalText(activeRegionId);
            activeHazardId = AdapterContractGuards.optionalText(activeHazardId);
            missionEvents = missionEvents == null ? List.of() : List.copyOf(missionEvents);
            statusEffects = statusEffects == null ? List.of() : List.copyOf(statusEffects);
            hudState = hudState == null ? Map.of() : Map.copyOf(hudState);
            audioState = audioState == null ? Map.of() : Map.copyOf(audioState);
            renderState = renderState == null ? Map.of() : Map.copyOf(renderState);
            worldLookup = worldLookup == null ? Map.of() : Map.copyOf(worldLookup);
            spawnEvent = spawnEvent == null ? Map.of() : Map.copyOf(spawnEvent);
            savedStatusState = savedStatusState == null ? Map.of() : Map.copyOf(savedStatusState);
        }
    }

    private static List<String> immutableTextList(List<String> values, String fieldName) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(value -> AdapterContractGuards.requireText(value, fieldName))
                .toList();
    }

    private static List<Map<String, String>> immutableMapList(List<Map<String, String>> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(value -> value == null ? Map.<String, String>of() : Map.copyOf(value))
                .toList();
    }
}
