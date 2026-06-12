package com.knoxhack.echo.creatorcore.codex;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echo.creatorcore.EchoCreatorCore;
import com.knoxhack.echo.creatorcore.config.CreatorCoreConfig;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.echoplatform.echocore.api.EchoRuntimeModules;

public final class CodexPilotService {
    private static final Gson GSON = new Gson();
    private static final int POLL_INTERVAL_TICKS = 40;
    private static final Identifier NPCORE_ENTITY = Identifier.fromNamespaceAndPath("echonpcore", "echo_npc");

    private final List<String> recentEvents = new ArrayList<>();
    private Entity avatar;
    private Entity executor;
    private UUID followTarget;
    private BlockPos anchor = BlockPos.ZERO;
    private String profile = "";
    private String label = "codex-pilot";
    private String dimension = "";
    private String lastMessage = "Codex Pilot has not spawned yet.";
    private boolean spawned;
    private boolean paused;
    private long tickCounter;
    private volatile boolean pollInFlight;

    public void onServerTick(Object event) {
        MinecraftServer server = EchoBackendWorldEventBridge.serverTickServer(event);
        if (server == null) {
            return;
        }
        tickCounter++;
        if (spawned && !paused) {
            tickFollow(server);
        }
        if (!pilotAllowed() || !bridgeAllowed() || tickCounter % POLL_INTERVAL_TICKS != 0 || pollInFlight) {
            return;
        }
        pollBridge(server);
    }

    public CodexPilotSnapshot snapshot() {
        return new CodexPilotSnapshot(
                pilotAllowed(),
                spawned,
                paused,
                autopilotAllowed(),
                worldActionsAllowed(),
                profile,
                label,
                dimension,
                positionJson().toString(),
                lastMessage,
                0,
                recentEvents);
    }

    public int spawn(CommandSourceStack source, String rawProfile, String rawLabel) {
        if (!pilotAllowed()) {
            return fail("Codex Pilot is locked by config (allow_codex_pilot=false).");
        }
        try {
            return spawnAt(source.getPlayerOrException(), cleanProfile(rawProfile), cleanLabel(rawLabel));
        } catch (Exception exception) {
            return fail("Codex Pilot spawn needs a player source: " + exception.getMessage());
        }
    }

    public int pause() {
        paused = true;
        return success("Codex Pilot paused.");
    }

    public int resume() {
        paused = false;
        return success("Codex Pilot resumed.");
    }

    public int stop() {
        paused = true;
        followTarget = null;
        return success("Codex Pilot emergency stop applied.");
    }

    public int despawn() {
        if (avatar != null && avatar.isAlive()) {
            avatar.discard();
        }
        avatar = null;
        executor = null;
        followTarget = null;
        spawned = false;
        paused = false;
        return success("Codex Pilot despawned.");
    }

    public int follow(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            followTarget = player.getUUID();
            return success("Codex Pilot following " + player.getName().getString() + ".");
        } catch (Exception exception) {
            return fail("Follow needs a player source.");
        }
    }

    public int goTo(CommandSourceStack source, double x, double y, double z) {
        ServerLevel level = level(source);
        if (level == null) {
            return fail("Codex Pilot goto needs a server level.");
        }
        return goTo(level, x, y, z);
    }

    public int look(float yaw, float pitch) {
        if (!spawned) {
            return fail("Codex Pilot is not spawned.");
        }
        syncRotation(yaw, clampPitch(pitch));
        return success("Codex Pilot looking yaw=" + yaw + " pitch=" + clampPitch(pitch) + ".");
    }

    public int say(CommandSourceStack source, String message) {
        String text = message == null || message.isBlank() ? "Ready." : message.trim();
        Component line = Component.literal("[" + label + "] " + text);
        if (source.getServer() != null) {
            source.getServer().getPlayerList().broadcastSystemMessage(line, false);
        }
        return success("Codex Pilot said: " + text);
    }

    public int inspect(CommandSourceStack source) {
        ServerLevel level = level(source);
        Entity eye = avatar != null && avatar.isAlive() ? avatar : playerOrNull(source);
        if (level == null || eye == null) {
            return fail("Codex Pilot inspect needs a spawned pilot or player source.");
        }
        HitResult hit = raycast(level, eye, 12.0D);
        JsonObject data = new JsonObject();
        data.addProperty("type", hit.getType().name().toLowerCase(Locale.ROOT));
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = level.getBlockState(pos);
            data.addProperty("block", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
            data.addProperty("x", pos.getX());
            data.addProperty("y", pos.getY());
            data.addProperty("z", pos.getZ());
            data.addProperty("face", blockHit.getDirection().getName());
        }
        data.addProperty("nearbyEntities", nearbyEntities(level, eye));
        record("inspect", "Codex Pilot inspected: " + data, data);
        return 1;
    }

    public int capture() {
        try {
            Class<?> capture = Class.forName("com.knoxhack.echo.creatorcore.client.CodexVisionCaptureService");
            Object result = capture.getMethod("capture", String.class).invoke(null, "pilot");
            int code = result instanceof Number number ? number.intValue() : 1;
            if (code > 0) {
                success("Codex Pilot requested a Codex Vision capture.");
            }
            return code;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return fail("Codex Pilot capture failed: " + exception.getMessage());
        }
    }

    public int interact(CommandSourceStack source) {
        inspect(source);
        return success("Codex Pilot interaction probe completed. UI-driving is intentionally manual in V1.");
    }

    public int breakTarget(CommandSourceStack source, JsonObject args) {
        if (!worldActionsAllowed()) {
            return fail("Codex Pilot world actions are locked by config (allow_codex_pilot_world_actions=false).");
        }
        ServerLevel level = level(source);
        BlockPos pos = blockPosArg(args, source);
        if (level == null || pos == null) {
            return fail("Break needs x/y/z or an inspectable target.");
        }
        if (!withinRadius(pos)) {
            return fail("Break refused outside pilot radius.");
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return fail("Break target is air.");
        }
        Entity actor = avatar != null ? avatar : playerOrNull(source);
        boolean broken = actor == null ? level.destroyBlock(pos, true) : level.destroyBlock(pos, true, actor);
        return broken ? success("Codex Pilot broke " + BuiltInRegistries.BLOCK.getKey(state.getBlock()) + " at " + pos + ".")
                : fail("Break failed at " + pos + ".");
    }

    public int placeBlock(CommandSourceStack source, JsonObject args) {
        if (!worldActionsAllowed()) {
            return fail("Codex Pilot world actions are locked by config (allow_codex_pilot_world_actions=false).");
        }
        ServerLevel level = level(source);
        BlockPos pos = blockPosArg(args, source);
        Identifier blockId = Identifier.tryParse(stringArg(args, "block", "minecraft:air"));
        if (level == null || pos == null || blockId == null || !BuiltInRegistries.BLOCK.containsKey(blockId)) {
            return fail("Place needs x/y/z and a valid block id.");
        }
        if (!withinRadius(pos)) {
            return fail("Place refused outside pilot radius.");
        }
        Block block = BuiltInRegistries.BLOCK.getValue(blockId);
        if (block == Blocks.AIR) {
            return fail("Place refused minecraft:air.");
        }
        boolean placed = level.setBlockAndUpdate(pos, block.defaultBlockState());
        return placed ? success("Codex Pilot placed " + blockId + " at " + pos + ".")
                : fail("Place failed at " + pos + ".");
    }

    public int submitTask(String prompt) {
        if (!autopilotAllowed()) {
            return fail("Codex Pilot autopilot is locked by config (allow_codex_pilot_autopilot=false).");
        }
        if (!bridgeAllowed()) {
            return fail("Codex bridge is locked by config (allow_codex_bridge=false).");
        }
        try {
            client().sendPilotTask(prompt, CreatorCoreConfig.integer(CreatorCoreConfig.CODEX_PILOT_MAX_STEPS, 20), true);
            return success("Codex Pilot task queued through bridge.");
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return fail("Codex Pilot task failed: " + exception.getMessage());
        }
    }

    public void executeBridgeAction(MinecraftServer server, CodexPilotAction action) {
        if (action == null) {
            return;
        }
        int result;
        String resultState;
        String message;
        JsonObject data;
        try {
            result = executeBridgeActionCore(server, action);
            resultState = result > 0 ? "done" : "refused";
            message = lastMessage;
            data = positionJson();
        } catch (RuntimeException exception) {
            result = 0;
            resultState = "failed";
            message = "Codex Pilot action " + action.command() + " failed: " + exception.getMessage();
            record("failed", message, new JsonObject());
            data = positionJson();
        }
        reportActionResultAsync(action, resultState, message, data);
        reportStatusAsync();
    }

    private int executeBridgeActionCore(MinecraftServer server, CodexPilotAction action) {
        if (!pilotAllowed()) {
            return fail("Bridge action refused because allow_codex_pilot=false.");
        }
        CommandSourceStack source = bridgeSource(server);
        String command = action.command();
        JsonObject args = action.args();
        switch (command) {
            case "spawn" -> {
                ServerPlayer player = firstPlayer(server);
                if (player == null) {
                    return fail("Bridge spawn refused because no player is online.");
                }
                return spawnAt(player, cleanProfile(stringArg(args, "profile", "echonpcore:test_survivor")),
                        cleanLabel(stringArg(args, "label", "codex-pilot")));
            }
            case "status", "report" -> {
                reportStatusAsync();
                return success("Codex Pilot status report requested.");
            }
            case "stop" -> {
                return stop();
            }
            case "pause" -> {
                return pause();
            }
            case "resume" -> {
                return resume();
            }
            case "despawn" -> {
                return despawn();
            }
            case "task" -> {
                return runAutopilotTask(source, action);
            }
            case "inspect" -> {
                return inspect(source);
            }
            case "capture" -> {
                return capture();
            }
            case "follow" -> {
                ServerPlayer player = firstPlayer(server);
                if (player != null) {
                    followTarget = player.getUUID();
                    return success("Codex Pilot following " + player.getName().getString() + ".");
                }
                return fail("Follow refused because no player is online.");
            }
            case "goto" -> {
                return goTo(level(source), doubleArg(args, "x", x()), doubleArg(args, "y", y()), doubleArg(args, "z", z()));
            }
            case "look" -> {
                return look((float) doubleArg(args, "yaw", yaw()), (float) doubleArg(args, "pitch", pitch()));
            }
            case "say" -> {
                return say(source, stringArg(args, "message", stringArg(args, "text", action.prompt())));
            }
            case "interact", "use" -> {
                return interact(source);
            }
            case "break" -> {
                return breakTarget(source, args);
            }
            case "place" -> {
                return placeBlock(source, args);
            }
            default -> {
                return fail("Unknown Codex Pilot action: " + command);
            }
        }
    }

    private int runAutopilotTask(CommandSourceStack source, CodexPilotAction action) {
        if (!autopilotAllowed()) {
            return fail("Autopilot task refused because allow_codex_pilot_autopilot=false.");
        }
        JsonObject args = action.args();
        int limit = Math.min(
                CreatorCoreConfig.integer(CreatorCoreConfig.CODEX_PILOT_MAX_STEPS, 20),
                Math.max(1, intArg(args, "maxSteps", 20)));
        List<JsonObject> steps = taskPlan(args);
        if (steps.isEmpty()) {
            steps = defaultTaskPlan(args);
        }
        int executed = 0;
        int refused = 0;
        for (JsonObject step : steps) {
            if (executed >= limit) {
                break;
            }
            int stepResult = executeTaskStep(source, step);
            if (stepResult <= 0) {
                refused++;
            }
            executed++;
        }
        JsonObject data = new JsonObject();
        data.addProperty("prompt", action.prompt());
        data.addProperty("stepsExecuted", executed);
        data.addProperty("refusedSteps", refused);
        data.addProperty("planner", stringArg(args, "planner", "creatorcore_fallback"));
        record("success", "Codex Pilot task ran guarded fallback plan: "
                + (action.prompt().isBlank() ? "no prompt" : action.prompt()), data);
        return 1;
    }

    private int executeTaskStep(CommandSourceStack source, JsonObject step) {
        String command = stringArg(step, "command", "inspect");
        JsonObject args = step.has("args") && step.get("args").isJsonObject()
                ? step.getAsJsonObject("args")
                : new JsonObject();
        return switch (command) {
            case "inspect" -> inspect(source);
            case "capture" -> CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_CODEX_VISUAL_CONTEXT, false)
                    ? capture()
                    : fail("Autopilot capture step skipped because allow_codex_visual_context=false.");
            case "report" -> success(stringArg(args, "message", "Codex Pilot task report."));
            case "say" -> say(source, stringArg(args, "message", ""));
            case "look" -> look((float) doubleArg(args, "yaw", yaw()), (float) doubleArg(args, "pitch", pitch()));
            default -> fail("Autopilot refused unsupported task step: " + command);
        };
    }

    private List<JsonObject> taskPlan(JsonObject args) {
        if (args == null || !args.has("plan") || !args.get("plan").isJsonArray()) {
            return List.of();
        }
        List<JsonObject> steps = new ArrayList<>();
        for (JsonElement element : args.getAsJsonArray("plan")) {
            if (element.isJsonObject()) {
                JsonObject step = element.getAsJsonObject();
                String command = stringArg(step, "command", "inspect");
                if (List.of("inspect", "capture", "report", "say", "look").contains(command)) {
                    steps.add(step);
                }
            }
        }
        return List.copyOf(steps);
    }

    private List<JsonObject> defaultTaskPlan(JsonObject args) {
        JsonArray plan = new JsonArray();
        JsonObject inspect = new JsonObject();
        inspect.addProperty("command", "inspect");
        plan.add(inspect);
        if (booleanArg(args, "useLatestCapture", true)) {
            JsonObject capture = new JsonObject();
            capture.addProperty("command", "capture");
            plan.add(capture);
        }
        JsonObject report = new JsonObject();
        JsonObject reportArgs = new JsonObject();
        report.addProperty("command", "report");
        reportArgs.addProperty("message", "fallback task plan completed");
        report.add("args", reportArgs);
        plan.add(report);
        List<JsonObject> steps = new ArrayList<>();
        for (JsonElement element : plan) {
            steps.add(element.getAsJsonObject());
        }
        return List.copyOf(steps);
    }

    private int spawnAt(ServerPlayer player, String profileId, String rawLabel) {
        ServerLevel level = player.level();
        despawn();
        profile = profileId;
        label = rawLabel;
        dimension = level.dimension().identifier().toString();
        anchor = player.blockPosition().immutable();
        executor = player;
        syncExecutor(level, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        avatar = spawnNpcAvatar(level, player, profileId);
        spawned = true;
        paused = false;
        String visible = avatar == null ? "player-backed executor" : "NPCore avatar " + avatar.getId();
        int result = success("Codex Pilot spawned as " + visible + " with profile " + profileId + ".");
        reportStatusAsync();
        return result;
    }

    private Entity spawnNpcAvatar(ServerLevel level, ServerPlayer player, String profileId) {
        if (!EchoRuntimeModules.isLoaded("echonpcore") || !BuiltInRegistries.ENTITY_TYPE.containsKey(NPCORE_ENTITY)) {
            return null;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(NPCORE_ENTITY);
        Entity entity = type.create(level, EntitySpawnReason.EVENT);
        if (entity == null) {
            return null;
        }
        entity.setPos(player.getX(), player.getY(), player.getZ());
        entity.setYRot(player.getYRot());
        entity.setXRot(0.0F);
        entity.setCustomName(Component.literal(label));
        entity.setCustomNameVisible(true);
        invokeIfPresent(entity, "configureProfile", Identifier.tryParse(profileId));
        invokeIfPresent(entity, "setHome", player.blockPosition());
        level.addFreshEntity(entity);
        return entity;
    }

    private void tickFollow(MinecraftServer server) {
        if (followTarget == null || avatar == null || !avatar.isAlive()) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(followTarget);
        if (player == null || player.level() != avatar.level()) {
            return;
        }
        double distance = avatar.distanceTo(player);
        if (distance > 3.0D) {
            goTo(player.level(), player.getX(), player.getY(), player.getZ());
        }
    }

    private void pollBridge(MinecraftServer server) {
        pollInFlight = true;
        Thread thread = new Thread(() -> {
            try {
                List<CodexPilotAction> actions = client().claimPilotActions();
                if (actions.isEmpty()) {
                    reportStatusAsync();
                    pollInFlight = false;
                    return;
                }
                server.execute(() -> {
                    int limit = CreatorCoreConfig.integer(CreatorCoreConfig.CODEX_PILOT_MAX_STEPS, 20);
                    actions.stream().limit(limit).forEach(action -> executeBridgeAction(server, action));
                    actions.stream().skip(limit).forEach(action -> reportActionResultAsync(action, "refused",
                            "Codex Pilot refused action because codex_pilot_max_steps was reached.",
                            new JsonObject()));
                    pollInFlight = false;
                });
            } catch (IOException | InterruptedException exception) {
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                pollInFlight = false;
                EchoCreatorCore.LOGGER.debug("Codex Pilot bridge poll failed: {}", exception.getMessage());
            }
        }, "creatorcore-codex-pilot-poll");
        thread.setDaemon(true);
        thread.start();
    }

    private int goTo(ServerLevel level, double x, double y, double z) {
        if (!spawned) {
            return fail("Codex Pilot is not spawned.");
        }
        if (level == null) {
            return fail("Codex Pilot goto needs a server level.");
        }
        BlockPos target = BlockPos.containing(x, y, z);
        if (!withinRadius(target)) {
            return fail("Goto refused outside pilot radius.");
        }
        if (avatar instanceof Mob mob) {
            mob.getNavigation().moveTo(x, y, z, 0.9D);
        } else if (avatar != null) {
            avatar.teleportTo(x, y, z);
        }
        syncExecutor(level, x, y, z, yaw(), pitch());
        return success("Codex Pilot moving to " + target + ".");
    }

    private void syncExecutor(ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        if (executor == null || executor.level() != level) {
            return;
        }
        executor.setPos(x, y, z);
        executor.setYRot(yaw);
        executor.setXRot(pitch);
    }

    private void syncRotation(float yaw, float pitch) {
        if (avatar != null) {
            avatar.setYRot(yaw);
            avatar.setXRot(pitch);
        }
        if (executor != null) {
            executor.setYRot(yaw);
            executor.setXRot(pitch);
        }
    }

    private HitResult raycast(ServerLevel level, Entity entity, double range) {
        Vec3 from = entity.getEyePosition();
        Vec3 look = entity.getViewVector(1.0F).normalize();
        Vec3 to = from.add(look.scale(range));
        return level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, entity));
    }

    private String nearbyEntities(ServerLevel level, Entity eye) {
        return level.getEntitiesOfClass(Entity.class, new AABB(eye.blockPosition()).inflate(8.0D), entity -> entity != eye)
                .stream()
                .limit(8)
                .map(entity -> BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()) + "@" + entity.blockPosition().toShortString())
                .toList()
                .toString();
    }

    private BlockPos blockPosArg(JsonObject args, CommandSourceStack source) {
        if (args != null && args.has("x") && args.has("y") && args.has("z")) {
            return BlockPos.containing(doubleArg(args, "x", x()), doubleArg(args, "y", y()), doubleArg(args, "z", z()));
        }
        ServerLevel level = level(source);
        Entity eye = avatar != null ? avatar : playerOrNull(source);
        if (level == null || eye == null) {
            return null;
        }
        HitResult hit = raycast(level, eye, 8.0D);
        return hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK ? blockHit.getBlockPos() : null;
    }

    private boolean withinRadius(BlockPos pos) {
        int radius = CreatorCoreConfig.integer(CreatorCoreConfig.CODEX_PILOT_MAX_RADIUS, 32);
        return anchor == BlockPos.ZERO || pos.distSqr(anchor) <= (double) radius * radius;
    }

    private JsonObject positionJson() {
        JsonObject json = new JsonObject();
        Entity entity = avatar != null && avatar.isAlive() ? avatar : executor;
        if (entity == null) {
            return json;
        }
        json.addProperty("x", entity.getX());
        json.addProperty("y", entity.getY());
        json.addProperty("z", entity.getZ());
        json.addProperty("blockX", entity.blockPosition().getX());
        json.addProperty("blockY", entity.blockPosition().getY());
        json.addProperty("blockZ", entity.blockPosition().getZ());
        return json;
    }

    private void reportStatusAsync() {
        if (!bridgeAllowed()) {
            return;
        }
        JsonObject status = new JsonObject();
        status.addProperty("enabled", pilotAllowed());
        status.addProperty("spawned", spawned);
        status.addProperty("paused", paused);
        status.addProperty("autopilotAllowed", autopilotAllowed());
        status.addProperty("worldActionsAllowed", worldActionsAllowed());
        status.addProperty("profile", profile);
        status.addProperty("label", label);
        status.addProperty("dimension", dimension);
        status.add("position", positionJson());
        status.addProperty("lastMessage", lastMessage);
        Thread thread = new Thread(() -> {
            try {
                client().reportPilotStatus(status);
            } catch (IOException | InterruptedException exception) {
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "creatorcore-codex-pilot-report");
        thread.setDaemon(true);
        thread.start();
    }

    private void reportActionResultAsync(CodexPilotAction action, String status, String message, JsonObject data) {
        if (!bridgeAllowed()) {
            return;
        }
        Thread thread = new Thread(() -> {
            try {
                client().reportPilotActionResult(action, status, message, data);
            } catch (IOException | InterruptedException exception) {
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "creatorcore-codex-pilot-result");
        thread.setDaemon(true);
        thread.start();
    }

    private void record(String type, String message, JsonObject data) {
        lastMessage = message == null ? "" : message;
        String line = Instant.now() + " " + type + " " + lastMessage;
        recentEvents.add(line);
        while (recentEvents.size() > 25) {
            recentEvents.remove(0);
        }
        writeLog(type, lastMessage, data);
        if (bridgeAllowed()) {
            Thread thread = new Thread(() -> {
                try {
                    client().reportPilotEvent(type, lastMessage, data);
                } catch (IOException | InterruptedException exception) {
                    if (exception instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                }
            }, "creatorcore-codex-pilot-event");
            thread.setDaemon(true);
            thread.start();
        }
    }

    private void writeLog(String type, String message, JsonObject data) {
        try {
            Path root = pilotLogRoot();
            Files.createDirectories(root);
            JsonObject entry = new JsonObject();
            entry.addProperty("createdAt", Instant.now().toString());
            entry.addProperty("type", type);
            entry.addProperty("message", message);
            entry.add("data", data == null ? new JsonObject() : data);
            Files.writeString(root.resolve("pilot.jsonl"), GSON.toJson(entry) + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException exception) {
            EchoCreatorCore.LOGGER.debug("Codex Pilot log write failed: {}", exception.getMessage());
        }
    }

    private int success(String message) {
        JsonObject data = positionJson();
        record("success", message, data);
        return 1;
    }

    private int fail(String message) {
        record("refusal", message, new JsonObject());
        return 0;
    }

    private Path pilotLogRoot() {
        Path root = Path.of(CreatorCoreConfig.string(CreatorCoreConfig.CODEX_PILOT_LOG_ROOT,
                "run/creatorcore/codex_pilot"));
        if (root.isAbsolute()) {
            return root.normalize();
        }
        Path workspace = Path.of(CreatorCoreConfig.string(CreatorCoreConfig.CODEX_WORKSPACE_ROOT, "C:/Github/Echo"));
        return workspace.resolve(root).normalize();
    }

    private CodexBridgeClient client() {
        return new CodexBridgeClient(
                CreatorCoreConfig.string(CreatorCoreConfig.CODEX_BRIDGE_URL, "http://127.0.0.1:47321"),
                CreatorCoreConfig.string(CreatorCoreConfig.CODEX_BRIDGE_TOKEN, ""));
    }

    private boolean pilotAllowed() {
        return CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_CODEX_PILOT, false);
    }

    private boolean autopilotAllowed() {
        return pilotAllowed() && CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_CODEX_PILOT_AUTOPILOT, false);
    }

    private boolean worldActionsAllowed() {
        return pilotAllowed() && CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_CODEX_PILOT_WORLD_ACTIONS, false);
    }

    private boolean bridgeAllowed() {
        return CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_CODEX_BRIDGE, false);
    }

    private static CommandSourceStack bridgeSource(MinecraftServer server) {
        ServerPlayer player = firstPlayer(server);
        return player == null ? server.createCommandSourceStack() : player.createCommandSourceStack();
    }

    private static ServerPlayer firstPlayer(MinecraftServer server) {
        return server.getPlayerList().getPlayers().stream().findFirst().orElse(null);
    }

    private static ServerLevel level(CommandSourceStack source) {
        try {
            return source.getLevel();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static Player playerOrNull(CommandSourceStack source) {
        return source.getEntity() instanceof Player player ? player : null;
    }

    private double x() {
        Entity entity = avatar != null ? avatar : executor;
        return entity == null ? 0.0D : entity.getX();
    }

    private double y() {
        Entity entity = avatar != null ? avatar : executor;
        return entity == null ? 0.0D : entity.getY();
    }

    private double z() {
        Entity entity = avatar != null ? avatar : executor;
        return entity == null ? 0.0D : entity.getZ();
    }

    private float yaw() {
        Entity entity = avatar != null ? avatar : executor;
        return entity == null ? 0.0F : entity.getYRot();
    }

    private float pitch() {
        Entity entity = avatar != null ? avatar : executor;
        return entity == null ? 0.0F : entity.getXRot();
    }

    private static float clampPitch(float pitch) {
        return Math.max(-90.0F, Math.min(90.0F, pitch));
    }

    private static double doubleArg(JsonObject args, String key, double fallback) {
        try {
            return args != null && args.has(key) ? args.get(key).getAsDouble() : fallback;
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static int intArg(JsonObject args, String key, int fallback) {
        try {
            return args != null && args.has(key) ? args.get(key).getAsInt() : fallback;
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static boolean booleanArg(JsonObject args, String key, boolean fallback) {
        try {
            return args != null && args.has(key) ? args.get(key).getAsBoolean() : fallback;
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static String stringArg(JsonObject args, String key, String fallback) {
        try {
            return args != null && args.has(key) ? args.get(key).getAsString() : fallback;
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static String cleanProfile(String raw) {
        String value = raw == null || raw.isBlank() ? "echonpcore:test_survivor" : raw.trim();
        return value.contains(":") ? value : "echonpcore:" + value.toLowerCase(Locale.ROOT);
    }

    private static String cleanLabel(String raw) {
        String value = raw == null || raw.isBlank() ? "codex-pilot" : raw.trim();
        return value.length() > 48 ? value.substring(0, 48) : value;
    }

    private static void invokeIfPresent(Entity entity, String methodName, Object arg) {
        if (entity == null || arg == null) {
            return;
        }
        try {
            Method method = entity.getClass().getMethod(methodName, arg.getClass());
            method.invoke(entity, arg);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Optional NPCore bridge: absence should leave the avatar usable.
        }
    }
}
