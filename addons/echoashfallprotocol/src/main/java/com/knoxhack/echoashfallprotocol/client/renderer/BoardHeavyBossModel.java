package com.knoxhack.echoashfallprotocol.client.renderer;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echorendercore.client.NamedModelParts;
import com.knoxhack.echorendercore.client.RenderCorePartProvider;
import java.util.Map;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

public class BoardHeavyBossModel<S extends HumanoidRenderState> extends HumanoidModel<S> implements RenderCorePartProvider {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "board_heavy_boss"), "main");

    private final Map<String, ModelPart> renderCoreParts;

    public BoardHeavyBossModel(ModelPart root) {
        super(root);
        ModelPart reactor = body.getChild("reactor");
        ModelPart shoulderRig = body.getChild("shoulder_rig");
        ModelPart backSpines = body.getChild("back_spines");
        ModelPart eyes = head.getChild("eyes");
        this.renderCoreParts = NamedModelParts.builder()
                .put("root", root)
                .put("head", head)
                .put("hat", hat)
                .put("body", body)
                .put("torso", body)
                .put("left_arm", leftArm)
                .put("right_arm", rightArm)
                .put("left_leg", leftLeg)
                .put("right_leg", rightLeg)
                .put("reactor", reactor)
                .put("core", reactor)
                .put("shoulder_rig", shoulderRig)
                .put("back_spines", backSpines)
                .put("eyes", eyes)
                .put("scanner", eyes)
                .put("trail", backSpines)
                .put("exhaust", backSpines)
                .put("ground", body)
                .build()
                .asMap();
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-5.0F, -10.0F, -5.0F, 10.0F, 10.0F, 10.0F)
                        .texOffs(40, 0).addBox(-5.8F, -10.8F, -5.8F, 11.6F, 11.6F, 11.6F, new CubeDeformation(0.2F)),
                PartPose.offset(0.0F, -2.0F, 0.0F));
        head.addOrReplaceChild("eyes",
                CubeListBuilder.create().texOffs(0, 118).addBox(-3.6F, -6.3F, -5.65F, 7.2F, 1.6F, 0.75F),
                PartPose.ZERO);
        head.addOrReplaceChild("hat",
                CubeListBuilder.create().texOffs(40, 0).addBox(-5.9F, -10.9F, -5.9F, 11.8F, 11.8F, 11.8F, new CubeDeformation(0.32F)),
                PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(16, 24).addBox(-6.5F, 0.0F, -3.6F, 13.0F, 15.0F, 7.2F)
                        .texOffs(72, 24).addBox(-8.0F, 1.0F, -4.2F, 16.0F, 6.0F, 8.4F, new CubeDeformation(0.15F)),
                PartPose.offset(0.0F, -2.0F, 0.0F));
        body.addOrReplaceChild("reactor",
                CubeListBuilder.create()
                        .texOffs(88, 44).addBox(-3.0F, 4.0F, -4.45F, 6.0F, 6.0F, 1.2F, new CubeDeformation(0.1F))
                        .texOffs(104, 44).addBox(-1.8F, 5.2F, -4.95F, 3.6F, 3.6F, 0.8F, new CubeDeformation(0.08F)),
                PartPose.ZERO);
        body.addOrReplaceChild("shoulder_rig",
                CubeListBuilder.create()
                        .texOffs(72, 0).addBox(-10.0F, -1.0F, -4.6F, 4.0F, 5.5F, 9.2F)
                        .mirror().addBox(6.0F, -1.0F, -4.6F, 4.0F, 5.5F, 9.2F)
                        .texOffs(104, 0).addBox(-2.5F, -4.0F, 2.7F, 5.0F, 4.0F, 3.0F),
                PartPose.ZERO);
        body.addOrReplaceChild("back_spines",
                CubeListBuilder.create()
                        .texOffs(112, 16).addBox(-0.7F, -2.5F, 3.4F, 1.4F, 5.0F, 5.0F)
                        .addBox(-4.6F, 3.0F, 3.4F, 1.4F, 5.0F, 4.0F)
                        .mirror().addBox(3.2F, 3.0F, 3.4F, 1.4F, 5.0F, 4.0F),
                PartPose.ZERO);
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                        .texOffs(56, 48).addBox(-4.2F, -2.0F, -2.8F, 5.2F, 15.5F, 5.6F)
                        .texOffs(80, 64).addBox(-4.9F, 8.0F, -3.4F, 6.0F, 5.5F, 6.8F, new CubeDeformation(0.12F)),
                PartPose.offset(-7.2F, 0.2F, 0.0F));
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                        .texOffs(56, 48).mirror().addBox(-1.0F, -2.0F, -2.8F, 5.2F, 15.5F, 5.6F)
                        .texOffs(80, 64).mirror().addBox(-1.1F, 8.0F, -3.4F, 6.0F, 5.5F, 6.8F, new CubeDeformation(0.12F)),
                PartPose.offset(7.2F, 0.2F, 0.0F));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create()
                        .texOffs(0, 32).addBox(-3.0F, 0.0F, -2.8F, 5.5F, 14.0F, 5.6F)
                        .texOffs(0, 64).addBox(-3.3F, 8.2F, -3.1F, 6.2F, 5.6F, 6.2F, new CubeDeformation(0.08F)),
                PartPose.offset(-3.2F, 10.0F, 0.0F));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create()
                        .texOffs(0, 32).mirror().addBox(-2.5F, 0.0F, -2.8F, 5.5F, 14.0F, 5.6F)
                        .texOffs(0, 64).mirror().addBox(-2.9F, 8.2F, -3.1F, 6.2F, 5.6F, 6.2F, new CubeDeformation(0.08F)),
                PartPose.offset(3.2F, 10.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public Map<String, ModelPart> renderCoreParts() {
        return renderCoreParts;
    }
}
