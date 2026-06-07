package com.knoxhack.echospellcore.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knoxhack.echospellcore.entity.SpellProjectileEntity;
import com.knoxhack.echospellcore.entity.SpellProjectileKind;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;

public class SpellProjectileRenderer extends EntityRenderer<SpellProjectileEntity, SpellProjectileRenderState> {
    private static final EnumMap<SpellProjectileKind, ProjectileSilhouetteModel> MODEL_CACHE =
            new EnumMap<>(SpellProjectileKind.class);

    public SpellProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public SpellProjectileRenderState createRenderState() {
        return new SpellProjectileRenderState();
    }

    @Override
    public void extractRenderState(SpellProjectileEntity entity, SpellProjectileRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.kind = entity.projectileKind();
        state.remainingLife = entity.remainingLife();
        if (entity.level().isClientSide() && entity.tickCount % 2 == 0) {
            var random = entity.level().getRandom();
            var motion = entity.getDeltaMovement();
            double backX = entity.getX() - motion.x * 0.35D;
            double backY = entity.getY() - motion.y * 0.35D;
            double backZ = entity.getZ() - motion.z * 0.35D;
            for (int i = 0; i < 2; i++) {
                double ox = (random.nextDouble() - 0.5D) * 0.08D;
                double oy = (random.nextDouble() - 0.5D) * 0.08D;
                double oz = (random.nextDouble() - 0.5D) * 0.08D;
                entity.level().addParticle(state.kind.trailParticle(), backX + ox, backY + oy, backZ + oz,
                        -motion.x * 0.02D, -motion.y * 0.02D, -motion.z * 0.02D);
            }
            if (entity.tickCount % 4 == 0) {
                entity.level().addParticle(state.kind.accentParticle(), entity.getX(), entity.getY(), entity.getZ(),
                        0.0D, 0.01D, 0.0D);
            }
        }
    }

    @Override
    public void submit(SpellProjectileRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState camera) {
        if (state.isInvisible || camera == null || camera.orientation == null) {
            super.submit(state, poseStack, collector, camera);
            return;
        }
        float pulse = 1.0F + Mth.sin(state.ageInTicks * 0.45F) * 0.13F;
        float size = state.kind.renderScale() * pulse;
        float fade = Mth.clamp(state.remainingLife / 12.0F, 0.35F, 1.0F);
        int light = LightCoordsUtil.lightCoordsWithEmission(state.lightCoords, 12);

        poseStack.pushPose();
        poseStack.mulPose(camera.orientation);
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.ageInTicks * state.kind.twistDegreesPerTick()));

        OrderedSubmitNodeCollector glow = collector.order(3);
        glow.submitCustomGeometry(poseStack, RenderTypes.textBackground(),
                (pose, consumer) -> {
                    float blend = state.kind.geometryBlend();
                    shards(pose, consumer, state.kind.shardCount(), size * 0.75F,
                            size * state.kind.haloScale(), size * 0.08F,
                            withAlpha(state.kind.glowColor(), 0.72F * fade * blend), light, -0.026F,
                            state.ageInTicks * 2.0F);
                    drawProfileGlow(pose, consumer, state, size, fade, blend, light);
                });

        OrderedSubmitNodeCollector sprite = collector.order(5);
        sprite.submitCustomGeometry(poseStack, RenderTypes.eyes(state.kind.spriteTexture()),
                (pose, consumer) -> texturedQuad(pose, consumer, size * state.kind.spriteScale(),
                        withAlpha(0xFFFFFFFF, fade), light, 0.02F));

        OrderedSubmitNodeCollector core = collector.order(4);
        core.submitCustomGeometry(poseStack, RenderTypes.textBackground(),
                (pose, consumer) -> {
                    float blend = state.kind.geometryBlend();
                    drawAuthoredProjectileModel(pose, consumer, state, size, blend, light);
                });

        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    private static void drawAuthoredProjectileModel(PoseStack.Pose pose, VertexConsumer consumer,
            SpellProjectileRenderState state, float size, float blend, int light) {
        ProjectileSilhouetteModel model = authoredModel(state.kind);
        if (model.cuboids().isEmpty()) {
            drawProfileCore(pose, consumer, state, size, blend, light);
            return;
        }
        float modelScale = size * 0.28F;
        int index = 0;
        for (ModelCuboid cuboid : model.cuboids()) {
            float shade = index++ % 2 == 0 ? 1.0F : 0.68F;
            int color = withAlpha(index == model.cuboids().size() ? state.kind.flareColor() : state.kind.coreColor(),
                    blend * shade * cuboid.alpha());
            int sideColor = withAlpha(state.kind.glowColor(), blend * shade * cuboid.alpha() * 0.42F);
            drawCuboid(pose, consumer, cuboid, modelScale, color, sideColor, light);
        }
    }

    private static ProjectileSilhouetteModel authoredModel(SpellProjectileKind kind) {
        synchronized (MODEL_CACHE) {
            return MODEL_CACHE.computeIfAbsent(kind, SpellProjectileRenderer::loadAuthoredModel);
        }
    }

    private static ProjectileSilhouetteModel loadAuthoredModel(SpellProjectileKind kind) {
        String resource = "assets/" + kind.modelAsset().getNamespace() + "/" + kind.modelAsset().getPath();
        try (var stream = SpellProjectileRenderer.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                return ProjectileSilhouetteModel.fallback(kind.modelProfile());
            }
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonArray elements = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("elements");
                if (elements == null || elements.isEmpty()) {
                    return ProjectileSilhouetteModel.fallback(kind.modelProfile());
                }
                List<ModelCuboid> cuboids = new ArrayList<>();
                int index = 0;
                for (JsonElement element : elements) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    JsonObject object = element.getAsJsonObject();
                    JsonArray from = object.getAsJsonArray("from");
                    JsonArray to = object.getAsJsonArray("to");
                    if (from == null || to == null || from.size() < 3 || to.size() < 3) {
                        continue;
                    }
                    float minX = normalized(from.get(0).getAsFloat());
                    float minY = normalized(from.get(1).getAsFloat());
                    float minZ = normalized(from.get(2).getAsFloat());
                    float maxX = normalized(to.get(0).getAsFloat());
                    float maxY = normalized(to.get(1).getAsFloat());
                    float maxZ = normalized(to.get(2).getAsFloat());
                    RotationData rotation = rotationData(object);
                    float depth = Math.max(0.04F, Math.abs(maxZ - minZ) * 0.5F);
                    cuboids.add(new ModelCuboid(Math.min(minX, maxX), Math.min(minY, maxY),
                            Math.min(minZ, maxZ) + index * 0.01F, Math.max(minX, maxX), Math.max(minY, maxY),
                            Math.max(minZ, maxZ) + index * 0.01F, rotation.angleDegrees(), rotation.originX(),
                            rotation.originY(), depth, 1.0F - index * 0.045F));
                    index++;
                }
                return cuboids.isEmpty() ? ProjectileSilhouetteModel.fallback(kind.modelProfile())
                        : new ProjectileSilhouetteModel(List.copyOf(cuboids));
            }
        } catch (RuntimeException | java.io.IOException ignored) {
            return ProjectileSilhouetteModel.fallback(kind.modelProfile());
        }
    }

    private static RotationData rotationData(JsonObject object) {
        JsonObject rotation = object.getAsJsonObject("rotation");
        if (rotation == null || !"z".equals(rotation.has("axis") ? rotation.get("axis").getAsString() : "")) {
            return RotationData.NONE;
        }
        JsonArray origin = rotation.getAsJsonArray("origin");
        float originX = 0.0F;
        float originY = 0.0F;
        if (origin != null && origin.size() >= 2) {
            originX = normalized(origin.get(0).getAsFloat());
            originY = normalized(origin.get(1).getAsFloat());
        }
        float angle = rotation.has("angle") ? rotation.get("angle").getAsFloat() : 0.0F;
        return new RotationData(angle, originX, originY);
    }

    private static float normalized(float blockModelCoord) {
        return (blockModelCoord - 8.0F) / 8.0F;
    }

    private static void drawProfileGlow(PoseStack.Pose pose, VertexConsumer consumer, SpellProjectileRenderState state,
            float size, float fade, float blend, int light) {
        int glow = withAlpha(state.kind.glowColor(), 0.76F * fade * blend);
        switch (state.kind.modelProfile()) {
            case LANCE -> {
                bar(pose, consumer, size * 3.1F, size * 0.16F, glow, light, -0.02F);
                barVertical(pose, consumer, size * 0.95F, size * 0.1F, glow, light, -0.018F);
                trailRibs(pose, consumer, state.kind.trailSegments(), size * 0.42F, size * 0.12F,
                        withAlpha(state.kind.glowColor(), 0.48F * fade * blend), light);
            }
            case STORM_RAIL -> {
                bar(pose, consumer, size * 2.8F, size * 0.09F, glow, light, -0.022F);
                zigZag(pose, consumer, size * 2.35F, size * 0.46F,
                        withAlpha(state.kind.flareColor(), 0.72F * fade * blend), light, -0.014F);
                barVertical(pose, consumer, size * 1.45F, size * 0.08F, glow, light, -0.018F);
            }
            case SHEAR -> {
                slash(pose, consumer, size * 2.45F, size * 0.18F, glow, light, -0.018F, -1.0F);
                slash(pose, consumer, size * 1.75F, size * 0.12F,
                        withAlpha(state.kind.flareColor(), 0.62F * fade * blend), light, -0.012F, 1.0F);
            }
            case VOID_CORE -> {
                diamond(pose, consumer, size * 1.85F, glow, light, -0.015F);
                ring(pose, consumer, 8, size * 1.2F, size * 0.12F,
                        withAlpha(state.kind.glowColor(), 0.46F * fade * blend), light, -0.02F,
                        state.ageInTicks * -3.0F);
            }
            case ORB -> {
                diamond(pose, consumer, size * 1.65F, glow, light, -0.015F);
                ring(pose, consumer, 10, size * 1.28F, size * 0.1F,
                        withAlpha(state.kind.flareColor(), 0.52F * fade * blend), light, -0.018F,
                        state.ageInTicks * 3.0F);
            }
        }
    }

    private static void drawProfileCore(PoseStack.Pose pose, VertexConsumer consumer, SpellProjectileRenderState state,
            float size, float blend, int light) {
        int core = withAlpha(state.kind.coreColor(), blend);
        int flare = withAlpha(state.kind.flareColor(), blend);
        switch (state.kind.modelProfile()) {
            case LANCE -> {
                bar(pose, consumer, size * 2.05F, size * 0.08F, core, light, 0.0F);
                diamond(pose, consumer, size * 0.58F, flare, light, 0.018F);
            }
            case STORM_RAIL -> {
                bar(pose, consumer, size * 1.8F, size * 0.06F, core, light, 0.0F);
                zigZag(pose, consumer, size * 1.4F, size * 0.25F, flare, light, 0.012F);
            }
            case SHEAR -> {
                slash(pose, consumer, size * 1.8F, size * 0.11F, core, light, 0.0F, -1.0F);
                diamond(pose, consumer, size * 0.34F, flare, light, 0.018F);
            }
            case VOID_CORE -> {
                diamond(pose, consumer, size * 1.05F, core, light, 0.0F);
                ring(pose, consumer, 6, size * 0.72F, size * 0.08F, flare, light, 0.018F,
                        state.ageInTicks * -4.0F);
            }
            case ORB -> {
                diamond(pose, consumer, size, core, light, 0.0F);
                diamond(pose, consumer, size * 0.44F, flare, light, 0.018F);
                bar(pose, consumer, size * 1.2F, size * 0.08F, flare, light, 0.012F);
                barVertical(pose, consumer, size * 1.2F, size * 0.08F, flare, light, 0.014F);
            }
        }
    }

    private static void diamond(PoseStack.Pose pose, VertexConsumer consumer, float size, int color, int light, float z) {
        consumer.addVertex(pose, 0.0F, -size, z).setColor(color).setLight(light);
        consumer.addVertex(pose, size, 0.0F, z).setColor(color).setLight(light);
        consumer.addVertex(pose, 0.0F, size, z).setColor(color).setLight(light);
        consumer.addVertex(pose, -size, 0.0F, z).setColor(color).setLight(light);
    }

    private static void bar(PoseStack.Pose pose, VertexConsumer consumer, float halfLength, float halfWidth,
            int color, int light, float z) {
        consumer.addVertex(pose, -halfLength, -halfWidth, z).setColor(color).setLight(light);
        consumer.addVertex(pose, halfLength, -halfWidth, z).setColor(color).setLight(light);
        consumer.addVertex(pose, halfLength, halfWidth, z).setColor(color).setLight(light);
        consumer.addVertex(pose, -halfLength, halfWidth, z).setColor(color).setLight(light);
    }

    private static void rect(PoseStack.Pose pose, VertexConsumer consumer, float minX, float minY,
            float maxX, float maxY, int color, int light, float z) {
        consumer.addVertex(pose, minX, minY, z).setColor(color).setLight(light);
        consumer.addVertex(pose, maxX, minY, z).setColor(color).setLight(light);
        consumer.addVertex(pose, maxX, maxY, z).setColor(color).setLight(light);
        consumer.addVertex(pose, minX, maxY, z).setColor(color).setLight(light);
    }

    private static void drawCuboid(PoseStack.Pose pose, VertexConsumer consumer, ModelCuboid cuboid, float scale,
            int color, int sideColor, int light) {
        float minZ = cuboid.minZ();
        float maxZ = cuboid.maxZ();
        if (Math.abs(maxZ - minZ) < cuboid.depth()) {
            float center = (minZ + maxZ) * 0.5F;
            minZ = center - cuboid.depth();
            maxZ = center + cuboid.depth();
        }
        ModelVertex frontMinMin = cuboid.vertex(cuboid.minX(), cuboid.minY(), maxZ, scale);
        ModelVertex frontMaxMin = cuboid.vertex(cuboid.maxX(), cuboid.minY(), maxZ, scale);
        ModelVertex frontMaxMax = cuboid.vertex(cuboid.maxX(), cuboid.maxY(), maxZ, scale);
        ModelVertex frontMinMax = cuboid.vertex(cuboid.minX(), cuboid.maxY(), maxZ, scale);
        ModelVertex backMinMin = cuboid.vertex(cuboid.minX(), cuboid.minY(), minZ, scale);
        ModelVertex backMaxMin = cuboid.vertex(cuboid.maxX(), cuboid.minY(), minZ, scale);
        ModelVertex backMaxMax = cuboid.vertex(cuboid.maxX(), cuboid.maxY(), minZ, scale);
        ModelVertex backMinMax = cuboid.vertex(cuboid.minX(), cuboid.maxY(), minZ, scale);

        face(pose, consumer, frontMinMin, frontMaxMin, frontMaxMax, frontMinMax, color, light);
        face(pose, consumer, backMaxMin, backMinMin, backMinMax, backMaxMax, withAlpha(color, 0.48F), light);
        face(pose, consumer, backMinMin, frontMinMin, frontMinMax, backMinMax, sideColor, light);
        face(pose, consumer, frontMaxMin, backMaxMin, backMaxMax, frontMaxMax, sideColor, light);
        face(pose, consumer, backMinMax, frontMinMax, frontMaxMax, backMaxMax, sideColor, light);
        face(pose, consumer, backMinMin, backMaxMin, frontMaxMin, frontMinMin, sideColor, light);
    }

    private static void face(PoseStack.Pose pose, VertexConsumer consumer, ModelVertex a, ModelVertex b,
            ModelVertex c, ModelVertex d, int color, int light) {
        consumer.addVertex(pose, a.x(), a.y(), a.z()).setColor(color).setLight(light);
        consumer.addVertex(pose, b.x(), b.y(), b.z()).setColor(color).setLight(light);
        consumer.addVertex(pose, c.x(), c.y(), c.z()).setColor(color).setLight(light);
        consumer.addVertex(pose, d.x(), d.y(), d.z()).setColor(color).setLight(light);
    }

    private static void barVertical(PoseStack.Pose pose, VertexConsumer consumer, float halfLength, float halfWidth,
            int color, int light, float z) {
        consumer.addVertex(pose, -halfWidth, -halfLength, z).setColor(color).setLight(light);
        consumer.addVertex(pose, halfWidth, -halfLength, z).setColor(color).setLight(light);
        consumer.addVertex(pose, halfWidth, halfLength, z).setColor(color).setLight(light);
        consumer.addVertex(pose, -halfWidth, halfLength, z).setColor(color).setLight(light);
    }

    private static void shards(PoseStack.Pose pose, VertexConsumer consumer, int count, float innerRadius,
            float outerRadius, float halfWidth, int color, int light, float z, float offsetDegrees) {
        int safeCount = Math.max(1, count);
        for (int i = 0; i < safeCount; i++) {
            float angle = (float) Math.toRadians(offsetDegrees + i * (360.0F / safeCount));
            float dx = Mth.cos(angle);
            float dy = Mth.sin(angle);
            float px = -dy * halfWidth;
            float py = dx * halfWidth;
            float ix = dx * innerRadius;
            float iy = dy * innerRadius;
            float ox = dx * outerRadius;
            float oy = dy * outerRadius;
            consumer.addVertex(pose, ix - px, iy - py, z).setColor(color).setLight(light);
            consumer.addVertex(pose, ox - px, oy - py, z).setColor(color).setLight(light);
            consumer.addVertex(pose, ox + px, oy + py, z).setColor(color).setLight(light);
            consumer.addVertex(pose, ix + px, iy + py, z).setColor(color).setLight(light);
        }
    }

    private static void ring(PoseStack.Pose pose, VertexConsumer consumer, int count, float radius, float halfWidth,
            int color, int light, float z, float offsetDegrees) {
        int safeCount = Math.max(3, count);
        for (int i = 0; i < safeCount; i++) {
            float angle = (float) Math.toRadians(offsetDegrees + i * (360.0F / safeCount));
            float next = (float) Math.toRadians(offsetDegrees + (i + 1) * (360.0F / safeCount));
            float x1 = Mth.cos(angle) * radius;
            float y1 = Mth.sin(angle) * radius;
            float x2 = Mth.cos(next) * radius;
            float y2 = Mth.sin(next) * radius;
            consumer.addVertex(pose, x1 - halfWidth, y1 - halfWidth, z).setColor(color).setLight(light);
            consumer.addVertex(pose, x2 - halfWidth, y2 - halfWidth, z).setColor(color).setLight(light);
            consumer.addVertex(pose, x2 + halfWidth, y2 + halfWidth, z).setColor(color).setLight(light);
            consumer.addVertex(pose, x1 + halfWidth, y1 + halfWidth, z).setColor(color).setLight(light);
        }
    }

    private static void trailRibs(PoseStack.Pose pose, VertexConsumer consumer, int count, float spacing,
            float halfWidth, int color, int light) {
        int safeCount = Math.max(1, count);
        for (int i = 1; i <= safeCount; i++) {
            float x = -i * spacing;
            barVertical(pose, consumer, halfWidth * (safeCount - i + 1), halfWidth * 0.42F, color, light, -0.016F);
            slash(pose, consumer, halfWidth * 1.8F, halfWidth * 0.28F, color, light, -0.014F,
                    i % 2 == 0 ? 1.0F : -1.0F, x);
        }
    }

    private static void slash(PoseStack.Pose pose, VertexConsumer consumer, float halfLength, float halfWidth,
            int color, int light, float z, float slope) {
        slash(pose, consumer, halfLength, halfWidth, color, light, z, slope, 0.0F);
    }

    private static void slash(PoseStack.Pose pose, VertexConsumer consumer, float halfLength, float halfWidth,
            int color, int light, float z, float slope, float offsetX) {
        consumer.addVertex(pose, offsetX - halfLength, -halfWidth * slope - halfWidth, z).setColor(color).setLight(light);
        consumer.addVertex(pose, offsetX + halfLength, halfWidth * slope - halfWidth, z).setColor(color).setLight(light);
        consumer.addVertex(pose, offsetX + halfLength, halfWidth * slope + halfWidth, z).setColor(color).setLight(light);
        consumer.addVertex(pose, offsetX - halfLength, -halfWidth * slope + halfWidth, z).setColor(color).setLight(light);
    }

    private static void zigZag(PoseStack.Pose pose, VertexConsumer consumer, float halfLength, float height,
            int color, int light, float z) {
        float step = halfLength / 2.0F;
        for (int i = 0; i < 4; i++) {
            float x1 = -halfLength + i * step;
            float x2 = x1 + step;
            float y1 = i % 2 == 0 ? -height : height;
            float y2 = i % 2 == 0 ? height : -height;
            consumer.addVertex(pose, x1, y1 - 0.02F, z).setColor(color).setLight(light);
            consumer.addVertex(pose, x2, y2 - 0.02F, z).setColor(color).setLight(light);
            consumer.addVertex(pose, x2, y2 + 0.02F, z).setColor(color).setLight(light);
            consumer.addVertex(pose, x1, y1 + 0.02F, z).setColor(color).setLight(light);
        }
    }

    private static void texturedQuad(PoseStack.Pose pose, VertexConsumer consumer, float halfSize, int color,
            int light, float z) {
        consumer.addVertex(pose, -halfSize, -halfSize, z).setColor(color).setUv(0.0F, 1.0F).setLight(light);
        consumer.addVertex(pose, halfSize, -halfSize, z).setColor(color).setUv(1.0F, 1.0F).setLight(light);
        consumer.addVertex(pose, halfSize, halfSize, z).setColor(color).setUv(1.0F, 0.0F).setLight(light);
        consumer.addVertex(pose, -halfSize, halfSize, z).setColor(color).setUv(0.0F, 0.0F).setLight(light);
    }

    private static int withAlpha(int color, float multiplier) {
        int alpha = (color >>> 24) & 0xFF;
        int scaled = Mth.clamp(Math.round(alpha * multiplier), 0, 255);
        return (color & 0x00FFFFFF) | (scaled << 24);
    }

    private record ProjectileSilhouetteModel(List<ModelCuboid> cuboids) {
        private static ProjectileSilhouetteModel fallback(SpellProjectileKind.ProjectileModelProfile profile) {
            return switch (profile) {
                case LANCE -> new ProjectileSilhouetteModel(List.of(
                        ModelCuboid.box(-1.8F, -0.16F, 1.4F, 0.16F, 0.08F, 0.86F),
                        ModelCuboid.box(1.1F, -0.42F, 1.9F, 0.42F, 0.1F, 0.72F)));
                case STORM_RAIL -> new ProjectileSilhouetteModel(List.of(
                        ModelCuboid.box(-1.7F, -0.1F, 1.7F, 0.1F, 0.07F, 0.84F),
                        ModelCuboid.box(-0.7F, -0.48F, -0.38F, 0.48F, 0.09F, 0.68F),
                        ModelCuboid.box(0.42F, -0.48F, 0.74F, 0.48F, 0.09F, 0.68F)));
                case SHEAR -> new ProjectileSilhouetteModel(List.of(
                        ModelCuboid.box(-1.55F, -0.18F, 1.55F, 0.18F, 0.08F, 0.84F),
                        ModelCuboid.box(-0.28F, -0.72F, 0.28F, 0.72F, 0.08F, 0.62F)));
                case VOID_CORE -> new ProjectileSilhouetteModel(List.of(
                        ModelCuboid.box(-0.78F, -0.78F, 0.78F, 0.78F, 0.12F, 0.76F),
                        ModelCuboid.box(-0.36F, -1.15F, 0.36F, 1.15F, 0.1F, 0.52F)));
                case ORB -> new ProjectileSilhouetteModel(List.of(
                        ModelCuboid.box(-0.75F, -0.75F, 0.75F, 0.75F, 0.12F, 0.8F),
                        ModelCuboid.box(-1.05F, -0.22F, 1.05F, 0.22F, 0.09F, 0.56F),
                        ModelCuboid.box(-0.22F, -1.05F, 0.22F, 1.05F, 0.09F, 0.56F)));
            };
        }
    }

    private record ModelCuboid(float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
            float rotationDegrees, float originX, float originY, float depth, float alpha) {
        private static ModelCuboid box(float minX, float minY, float maxX, float maxY, float depth, float alpha) {
            return new ModelCuboid(minX, minY, -depth, maxX, maxY, depth, 0.0F, 0.0F, 0.0F, depth, alpha);
        }

        private ModelVertex vertex(float x, float y, float z, float scale) {
            if (rotationDegrees == 0.0F) {
                return new ModelVertex(x * scale, y * scale, z * scale);
            }
            float radians = (float) Math.toRadians(rotationDegrees);
            float dx = x - originX;
            float dy = y - originY;
            float rotatedX = originX + dx * Mth.cos(radians) - dy * Mth.sin(radians);
            float rotatedY = originY + dx * Mth.sin(radians) + dy * Mth.cos(radians);
            return new ModelVertex(rotatedX * scale, rotatedY * scale, z * scale);
        }
    }

    private record ModelVertex(float x, float y, float z) {
    }

    private record RotationData(float angleDegrees, float originX, float originY) {
        private static final RotationData NONE = new RotationData(0.0F, 0.0F, 0.0F);
    }
}
