package com.knoxhack.echocore.client.model;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

public final class EchoMobModelFactory {
    private EchoMobModelFactory() {
    }

    public static EntityModel<EchoMobRenderState> create(EntityRendererProvider.Context context,
            EchoMobFamily family, String entityName) {
        EchoMobFamily safeFamily = family == null ? EchoMobFamily.HUMANOID : family;
        return switch (safeFamily) {
            case QUADRUPED -> new EchoQuadrupedModel(bake(quadrupedLayer()));
            case CRAWLER -> new EchoCrawlerModel(bake(crawlerLayer()));
            case WRAITH -> new EchoWraithModel(bake(wraithLayer()));
            case SLIME -> new EchoSlimeModel(bake(slimeLayer()));
            case DRONE -> new EchoDroneModel(bake(droneLayer()));
            case STATION_SUIT -> new EchoHumanoidFallbackModel(bake(humanoidLayer(0.12F)));
            case HEAVY_BOSS -> new EchoHumanoidFallbackModel(bake(humanoidLayer(0.35F)));
            case INDUSTRIAL_CONSTRUCT -> new EchoHumanoidFallbackModel(bake(humanoidLayer(0.25F)));
            case SURVIVOR_NPC, HUMANOID -> new EchoHumanoidFallbackModel(bake(humanoidLayer(0.0F)));
            default -> new EchoHumanoidFallbackModel(bake(humanoidLayer(0.0F)));
        };
    }

    private static ModelPart bake(LayerDefinition layer) {
        return layer.bakeRoot();
    }

    private static LayerDefinition humanoidLayer(float deformation) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation inflate = new CubeDeformation(deformation);
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, inflate),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("hat",
                CubeListBuilder.create().texOffs(32, 0)
                        .addBox(-4.5F, -8.5F, -4.5F, 9.0F, 9.0F, 9.0F, new CubeDeformation(0.1F + deformation)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 16)
                        .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, inflate),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 16)
                        .addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, inflate),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 16).mirror()
                        .addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, inflate),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, inflate),
                PartPose.offset(-2.0F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 16).mirror()
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, inflate),
                PartPose.offset(2.0F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition quadrupedLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 18).addBox(-4.0F, -4.0F, -7.0F, 8.0F, 8.0F, 14.0F),
                PartPose.offset(0.0F, 15.0F, 1.0F));
        body.addOrReplaceChild("core",
                CubeListBuilder.create().texOffs(44, 0).addBox(-1.0F, -5.0F, -3.0F, 2.0F, 2.0F, 6.0F),
                PartPose.ZERO);
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -3.0F, -5.0F, 7.0F, 6.0F, 6.0F),
                PartPose.offset(0.0F, 13.5F, -7.0F));
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(42, 42).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 14.5F, 8.0F, -0.15F, 0.0F, 0.0F));
        addLeg(root, "left_front_leg", 3.0F, -4.5F);
        addLeg(root, "right_front_leg", -3.0F, -4.5F);
        addLeg(root, "left_back_leg", 3.0F, 5.0F);
        addLeg(root, "right_back_leg", -3.0F, 5.0F);
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition crawlerLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 18).addBox(-5.0F, -2.5F, -6.0F, 10.0F, 5.0F, 12.0F),
                PartPose.offset(0.0F, 19.0F, 0.0F));
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -2.0F, -4.0F, 6.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 18.5F, -6.0F));
        addLeg(root, "left_front_leg", 4.0F, -4.0F);
        addLeg(root, "right_front_leg", -4.0F, -4.0F);
        addLeg(root, "left_back_leg", 4.0F, 4.0F);
        addLeg(root, "right_back_leg", -4.0F, 4.0F);
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition wraithLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -7.0F, -3.5F, 7.0F, 7.0F, 7.0F),
                PartPose.offset(0.0F, 4.0F, 0.0F));
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 16).addBox(-3.5F, 0.0F, -2.0F, 7.0F, 13.0F, 4.0F),
                PartPose.offset(0.0F, 4.0F, 0.0F));
        root.addOrReplaceChild("trail",
                CubeListBuilder.create().texOffs(32, 16).addBox(-2.0F, 0.0F, -1.0F, 4.0F, 9.0F, 2.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition slimeLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -5.0F, -5.0F, 10.0F, 10.0F, 10.0F),
                PartPose.offset(0.0F, 19.0F, 0.0F));
        root.addOrReplaceChild("core",
                CubeListBuilder.create().texOffs(40, 0).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 19.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition droneLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -2.0F, -4.0F, 8.0F, 4.0F, 8.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        root.addOrReplaceChild("core",
                CubeListBuilder.create().texOffs(32, 0).addBox(-2.0F, -2.5F, -2.0F, 4.0F, 5.0F, 4.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        root.addOrReplaceChild("left_rotor",
                CubeListBuilder.create().texOffs(0, 20).addBox(-5.0F, -0.25F, -1.0F, 10.0F, 0.5F, 2.0F),
                PartPose.offset(7.0F, 14.5F, 0.0F));
        root.addOrReplaceChild("right_rotor",
                CubeListBuilder.create().texOffs(0, 20).addBox(-5.0F, -0.25F, -1.0F, 10.0F, 0.5F, 2.0F),
                PartPose.offset(-7.0F, 14.5F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void addLeg(PartDefinition root, String name, float x, float z) {
        root.addOrReplaceChild(name,
                CubeListBuilder.create().texOffs(0, 42).addBox(-1.2F, 0.0F, -1.2F, 2.4F, 7.0F, 2.4F),
                PartPose.offset(x, 17.0F, z));
    }

    private static final class EchoHumanoidFallbackModel extends HumanoidModel<EchoMobRenderState>
            implements EchoNamedModelPartProvider {
        private final Map<String, ModelPart> parts;

        private EchoHumanoidFallbackModel(ModelPart root) {
            super(root);
            this.parts = Map.copyOf(Map.ofEntries(
                    Map.entry("root", root),
                    Map.entry("head", head),
                    Map.entry("hat", hat),
                    Map.entry("body", body),
                    Map.entry("torso", body),
                    Map.entry("left_arm", leftArm),
                    Map.entry("right_arm", rightArm),
                    Map.entry("left_leg", leftLeg),
                    Map.entry("right_leg", rightLeg),
                    Map.entry("core", body),
                    Map.entry("scanner", head),
                    Map.entry("eyes", head),
                    Map.entry("ground", body)));
        }

        @Override
        public Map<String, ModelPart> echoNamedModelParts() {
            return parts;
        }
    }

    private abstract static class EchoSimpleModel extends EntityModel<EchoMobRenderState>
            implements EchoNamedModelPartProvider {
        protected final ModelPart root;
        protected final Map<String, ModelPart> parts;

        private EchoSimpleModel(ModelPart root) {
            super(root);
            this.root = root;
            this.parts = collect(root);
        }

        @Override
        public Map<String, ModelPart> echoNamedModelParts() {
            return parts;
        }

        protected ModelPart part(String name) {
            return parts.get(name);
        }

        private static Map<String, ModelPart> collect(ModelPart root) {
            LinkedHashMap<String, ModelPart> result = new LinkedHashMap<>();
            result.put("root", root);
            put(result, "head", child(root, "head"));
            put(result, "body", child(root, "body"));
            put(result, "torso", result.get("body"));
            put(result, "tail", child(root, "tail"));
            put(result, "trail", child(root, "trail"));
            put(result, "core", child(root, "core"));
            put(result, "left_rotor", child(root, "left_rotor"));
            put(result, "right_rotor", child(root, "right_rotor"));
            put(result, "left_front_leg", child(root, "left_front_leg"));
            put(result, "right_front_leg", child(root, "right_front_leg"));
            put(result, "left_back_leg", child(root, "left_back_leg"));
            put(result, "right_back_leg", child(root, "right_back_leg"));
            ModelPart body = result.get("body");
            put(result, "core", child(body, "core"));
            put(result, "scanner", result.get("core"));
            put(result, "eyes", result.get("head"));
            put(result, "ground", body);
            return Map.copyOf(result);
        }

        private static void put(Map<String, ModelPart> parts, String name, ModelPart part) {
            if (part != null) {
                parts.putIfAbsent(name, part);
            }
        }

        private static ModelPart child(ModelPart root, String name) {
            if (root == null) {
                return null;
            }
            try {
                return root.getChild(name);
            } catch (RuntimeException exception) {
                return null;
            }
        }
    }

    private static final class EchoQuadrupedModel extends EchoSimpleModel {
        private EchoQuadrupedModel(ModelPart root) {
            super(root);
        }

        @Override
        public void setupAnim(EchoMobRenderState state) {
            super.setupAnim(state);
            float walk = state.ageInTicks * 0.18F;
            rotate(part("head"), state.xRot * Mth.DEG_TO_RAD * 0.65F, state.yRot * Mth.DEG_TO_RAD, 0.0F);
            rotate(part("tail"), -0.15F, Mth.sin(state.ageInTicks * 0.16F) * 0.22F, 0.0F);
            walk(part("left_front_leg"), walk, 0.35F);
            walk(part("right_back_leg"), walk, 0.35F);
            walk(part("right_front_leg"), walk + Mth.PI, 0.35F);
            walk(part("left_back_leg"), walk + Mth.PI, 0.35F);
        }
    }

    private static final class EchoCrawlerModel extends EchoSimpleModel {
        private EchoCrawlerModel(ModelPart root) {
            super(root);
        }

        @Override
        public void setupAnim(EchoMobRenderState state) {
            super.setupAnim(state);
            float walk = state.ageInTicks * 0.28F;
            rotate(part("head"), state.xRot * Mth.DEG_TO_RAD * 0.35F, state.yRot * Mth.DEG_TO_RAD, 0.0F);
            walk(part("left_front_leg"), walk, 0.45F);
            walk(part("right_back_leg"), walk, 0.45F);
            walk(part("right_front_leg"), walk + Mth.PI, 0.45F);
            walk(part("left_back_leg"), walk + Mth.PI, 0.45F);
        }
    }

    private static final class EchoWraithModel extends EchoSimpleModel {
        private EchoWraithModel(ModelPart root) {
            super(root);
        }

        @Override
        public void setupAnim(EchoMobRenderState state) {
            super.setupAnim(state);
            rotate(part("head"), state.xRot * Mth.DEG_TO_RAD * 0.45F, state.yRot * Mth.DEG_TO_RAD, 0.0F);
            ModelPart trail = part("trail");
            if (trail != null) {
                trail.yRot = Mth.sin(state.ageInTicks * 0.12F) * 0.18F;
            }
        }
    }

    private static final class EchoSlimeModel extends EchoSimpleModel {
        private EchoSlimeModel(ModelPart root) {
            super(root);
        }

        @Override
        public void setupAnim(EchoMobRenderState state) {
            super.setupAnim(state);
            ModelPart body = part("body");
            if (body != null) {
                body.yScale = 1.0F + Mth.sin(state.ageInTicks * 0.2F) * 0.06F;
            }
        }
    }

    private static final class EchoDroneModel extends EchoSimpleModel {
        private EchoDroneModel(ModelPart root) {
            super(root);
        }

        @Override
        public void setupAnim(EchoMobRenderState state) {
            super.setupAnim(state);
            float spin = state.ageInTicks * 0.6F;
            rotate(part("left_rotor"), 0.0F, spin, 0.0F);
            rotate(part("right_rotor"), 0.0F, -spin, 0.0F);
        }
    }

    private static void rotate(ModelPart part, float xRot, float yRot, float zRot) {
        if (part != null) {
            part.xRot = xRot;
            part.yRot = yRot;
            part.zRot = zRot;
        }
    }

    private static void walk(ModelPart part, float phase, float stride) {
        if (part != null) {
            part.xRot = Mth.sin(phase) * stride;
        }
    }
}
