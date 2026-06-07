package com.knoxhack.echo.creatorcore.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.knoxhack.echo.creatorcore.EchoCreatorCore;
import com.knoxhack.echo.creatorcore.config.CreatorCoreConfig;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class CodexVisionCaptureService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(2);
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(800))
            .build();

    private CodexVisionCaptureService() {
    }

    public static int capture(String rawLabel) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            message(minecraft, "Codex Vision capture needs an active world.");
            return 0;
        }
        if (minecraft.getMainRenderTarget() == null) {
            message(minecraft, "Codex Vision capture needs a render target.");
            return 0;
        }
        if (!CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_CODEX_VISUAL_CONTEXT, false)) {
            message(minecraft, "Codex Vision is locked by config (allow_codex_visual_context=false).");
            return 0;
        }
        String label = safeLabel(rawLabel);
        String id = Instant.now().toString().replace(':', '-').replace('.', '-') + "-" + UUID.randomUUID().toString().substring(0, 8);
        Path root = captureRoot();
        Path screenshot = root.resolve(id + "-" + label + ".png").normalize();
        Path metadata = root.resolve(id + "-" + label + ".json").normalize();
        try {
            Files.createDirectories(root);
            JsonObject document = metadata(id, label, screenshot, metadata, minecraft);
            Files.writeString(metadata, GSON.toJson(document), StandardCharsets.UTF_8);
            Screenshot.grab(
                    screenshot.getParent().toFile(),
                    screenshot.getFileName().toString(),
                    minecraft.getMainRenderTarget(),
                    1,
                    component -> EchoCreatorCore.LOGGER.info("Codex Vision capture {}: {}", id, component.getString()));
            pruneOldCaptures(root);
            registerWithBridge(document);
            message(minecraft, "Codex Vision captured " + label + " at " + root.relativize(screenshot) + ".");
            return 1;
        } catch (IOException | RuntimeException exception) {
            EchoCreatorCore.LOGGER.warn("Codex Vision capture failed for {}", label, exception);
            message(minecraft, "Codex Vision capture failed: " + exception.getMessage());
            return 0;
        }
    }

    private static JsonObject metadata(String id, String label, Path screenshot, Path metadata, Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        JsonObject root = new JsonObject();
        root.addProperty("id", id);
        root.addProperty("label", label);
        root.addProperty("screenshotPath", slash(screenshot));
        root.addProperty("metadataPath", slash(metadata));
        root.addProperty("createdAt", Instant.now().toString());
        if (level == null || player == null) {
            root.addProperty("notes", "Client level or player was unavailable while collecting metadata.");
            return root;
        }
        root.addProperty("dimension", level.dimension().identifier().toString());
        root.add("position", position(player));
        root.add("rotation", rotation(player));
        root.addProperty("screen", minecraft.screen == null ? "hud" : minecraft.screen.getClass().getName());
        root.add("crosshair", crosshair(minecraft, level));
        root.add("heldItem", item(player.getMainHandItem()));
        root.addProperty("biome", level.getBiome(player.blockPosition()).unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse("unknown"));
        JsonObject weather = new JsonObject();
        weather.addProperty("raining", level.isRaining());
        weather.addProperty("thundering", level.isThundering());
        weather.addProperty("gameTime", level.getGameTime());
        weather.addProperty("dayTime", level.getGameTime() % 24000L);
        root.add("weather", weather);
        root.add("nearbyEntities", nearbyEntities(level, player));
        root.addProperty("notes", "Command-triggered local Codex Vision snapshot.");
        return root;
    }

    private static JsonObject position(LocalPlayer player) {
        JsonObject position = new JsonObject();
        position.addProperty("x", player.getX());
        position.addProperty("y", player.getY());
        position.addProperty("z", player.getZ());
        position.addProperty("blockX", player.blockPosition().getX());
        position.addProperty("blockY", player.blockPosition().getY());
        position.addProperty("blockZ", player.blockPosition().getZ());
        return position;
    }

    private static JsonObject rotation(LocalPlayer player) {
        JsonObject rotation = new JsonObject();
        rotation.addProperty("yaw", player.getYRot());
        rotation.addProperty("pitch", player.getXRot());
        return rotation;
    }

    private static JsonObject crosshair(Minecraft minecraft, ClientLevel level) {
        JsonObject crosshair = new JsonObject();
        HitResult hit = minecraft.hitResult;
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            crosshair.addProperty("type", "miss");
            return crosshair;
        }
        crosshair.addProperty("type", hit.getType().name().toLowerCase(Locale.ROOT));
        if (hit instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            crosshair.addProperty("block", BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString());
            crosshair.addProperty("x", pos.getX());
            crosshair.addProperty("y", pos.getY());
            crosshair.addProperty("z", pos.getZ());
            crosshair.addProperty("face", blockHit.getDirection().getName());
        } else if (hit instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            crosshair.addProperty("entityType", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
            crosshair.addProperty("entityName", entity.getName().getString());
            crosshair.addProperty("entityId", entity.getId());
        }
        return crosshair;
    }

    private static JsonObject item(ItemStack stack) {
        JsonObject item = new JsonObject();
        item.addProperty("id", stack.isEmpty() ? "minecraft:air" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        item.addProperty("count", stack.getCount());
        item.addProperty("name", stack.getHoverName().getString());
        return item;
    }

    private static JsonArray nearbyEntities(ClientLevel level, LocalPlayer player) {
        JsonArray entities = new JsonArray();
        List<Entity> nearby = level.getEntitiesOfClass(Entity.class, new AABB(player.blockPosition()).inflate(12.0D),
                entity -> entity != player).stream()
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))
                .limit(12)
                .toList();
        for (Entity entity : nearby) {
            JsonObject entry = new JsonObject();
            entry.addProperty("type", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
            entry.addProperty("name", entity.getName().getString());
            entry.addProperty("distance", Math.sqrt(entity.distanceToSqr(player)));
            entry.addProperty("x", entity.getX());
            entry.addProperty("y", entity.getY());
            entry.addProperty("z", entity.getZ());
            entities.add(entry);
        }
        return entities;
    }

    private static void registerWithBridge(JsonObject document) {
        if (!CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_CODEX_BRIDGE, false)) {
            return;
        }
        String baseUrl = normalizeUrl(CreatorCoreConfig.string(CreatorCoreConfig.CODEX_BRIDGE_URL, "http://127.0.0.1:47321"));
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + "/captures"))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(document.toString(), StandardCharsets.UTF_8));
        String token = CreatorCoreConfig.string(CreatorCoreConfig.CODEX_BRIDGE_TOKEN, "");
        if (!token.isBlank()) {
            request.header("Authorization", "Bearer " + token);
        }
        HTTP.sendAsync(request.build(), HttpResponse.BodyHandlers.discarding())
                .exceptionally(exception -> {
                    EchoCreatorCore.LOGGER.debug("Codex Vision bridge registration failed: {}", exception.getMessage());
                    return null;
                });
    }

    private static Path captureRoot() {
        String configured = CreatorCoreConfig.string(CreatorCoreConfig.CODEX_CAPTURE_ROOT,
                "run/creatorcore/codex_vision/captures");
        Path root = Path.of(configured);
        if (root.isAbsolute()) {
            return root.normalize();
        }
        Path workspace = Path.of(CreatorCoreConfig.string(CreatorCoreConfig.CODEX_WORKSPACE_ROOT, "C:/Github/Echo"));
        return workspace.resolve(root).normalize();
    }

    private static void pruneOldCaptures(Path root) throws IOException {
        int keep = CreatorCoreConfig.integer(CreatorCoreConfig.CODEX_CAPTURE_KEEP, 25);
        try (Stream<Path> files = Files.list(root)) {
            List<Path> metadataFiles = files
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .filter(path -> !"captures_index.json".equals(path.getFileName().toString()))
                    .sorted(Comparator.comparingLong(CodexVisionCaptureService::lastModified).reversed())
                    .toList();
            for (Path oldMetadata : metadataFiles.stream().skip(keep).toList()) {
                String base = oldMetadata.getFileName().toString().replaceFirst("\\.json$", "");
                Files.deleteIfExists(oldMetadata);
                Files.deleteIfExists(root.resolve(base + ".png"));
            }
        }
    }

    private static long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException exception) {
            return 0L;
        }
    }

    private static String safeLabel(String rawLabel) {
        String label = rawLabel == null || rawLabel.isBlank() ? "manual" : rawLabel.toLowerCase(Locale.ROOT);
        label = label.replaceAll("[^a-z0-9_\\-]+", "_").replaceAll("_+", "_");
        if (label.isBlank()) {
            return "manual";
        }
        return label.length() > 48 ? label.substring(0, 48) : label;
    }

    private static String slash(Path path) {
        return path.toAbsolutePath().normalize().toString().replace('\\', '/');
    }

    private static String normalizeUrl(String raw) {
        String value = raw == null || raw.isBlank() ? "http://127.0.0.1:47321" : raw.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static void message(Minecraft minecraft, String message) {
        if (minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component.literal("[ECHO CREATOR] " + message));
        }
    }
}
