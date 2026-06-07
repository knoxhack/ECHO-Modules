package com.knoxhack.echoashfallprotocol.client.renderer;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echorendercore.client.NamedModelParts;
import com.knoxhack.echorendercore.client.RenderCorePartProvider;
import java.util.Map;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class BoardQuadrupedModel extends EntityModel<AshfallLivingRenderState> implements RenderCorePartProvider {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "board_quadruped"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart tail;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftBackLeg;
    private final ModelPart rightBackLeg;
    private final ModelPart spines;
    private final ModelPart eyes;
    private final float bodyBaseY;
    private final Map<String, ModelPart> renderCoreParts;

    public BoardQuadrupedModel(ModelPart root) {
        super(root);
        this.root = root;
        this.body = root.getChild("body");
        this.bodyBaseY = body.y;
        this.head = root.getChild("head");
        this.tail = root.getChild("tail");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftBackLeg = root.getChild("left_back_leg");
        this.rightBackLeg = root.getChild("right_back_leg");
        this.spines = body.getChild("spines");
        this.eyes = head.getChild("eyes");
        this.renderCoreParts = NamedModelParts.builder()
                .put("root", root)
                .put("body", body)
                .put("torso", body)
                .put("head", head)
                .put("tail", tail)
                .put("left_front_leg", leftFrontLeg)
                .put("right_front_leg", rightFrontLeg)
                .put("left_back_leg", leftBackLeg)
                .put("right_back_leg", rightBackLeg)
                .put("spines", spines)
                .put("eyes", eyes)
                .put("core", spines)
                .put("scanner", eyes)
                .put("trail", tail)
                .put("exhaust", tail)
                .put("ground", body)
                .build()
                .asMap();
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 18).addBox(-4.8F, -4.0F, -8.0F, 9.6F, 8.0F, 16.0F)
                        .texOffs(34, 18).addBox(-5.2F, -5.0F, -3.0F, 10.4F, 2.0F, 9.0F),
                PartPose.offset(0.0F, 15.0F, 1.0F));
        body.addOrReplaceChild("spines",
                CubeListBuilder.create().texOffs(48, 0).addBox(-0.7F, -4.0F, -5.5F, 1.4F, 3.0F, 11.0F),
                PartPose.ZERO);
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -3.5F, -5.5F, 8.0F, 7.0F, 7.0F)
                        .texOffs(30, 0).addBox(-3.2F, -1.8F, -8.0F, 6.4F, 3.6F, 3.0F),
                PartPose.offset(0.0F, 13.8F, -7.2F));
        head.addOrReplaceChild("eyes",
                CubeListBuilder.create().texOffs(0, 54).addBox(-2.8F, -1.2F, -8.35F, 5.6F, 1.2F, 0.55F),
                PartPose.ZERO);
        root.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(50, 44).addBox(-1.2F, -1.2F, 0.0F, 2.4F, 2.4F, 7.0F),
                PartPose.offsetAndRotation(0.0F, 14.8F, 8.0F, -0.15F, 0.0F, 0.0F));
        addLeg(root, "left_front_leg", 3.2F, -4.7F);
        addLeg(root, "right_front_leg", -3.2F, -4.7F);
        addLeg(root, "left_back_leg", 3.2F, 5.2F);
        addLeg(root, "right_back_leg", -3.2F, 5.2F);
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void addLeg(PartDefinition root, String name, float x, float z) {
        root.addOrReplaceChild(name,
                CubeListBuilder.create().texOffs(0, 42).addBox(-1.4F, 0.0F, -1.5F, 2.8F, 8.0F, 3.0F),
                PartPose.offset(x, 16.0F, z));
    }

    @Override
    public void setupAnim(AshfallLivingRenderState state) {
        super.setupAnim(state);
        float walk = state.ageInTicks * 0.18F;
        float stride = 0.35F;
        head.yRot = state.yRot * ((float) Math.PI / 180F);
        head.xRot = state.xRot * ((float) Math.PI / 180F) * 0.65F;
        body.y = bodyBaseY + Mth.sin(state.ageInTicks * 0.12F) * 0.12F;
        tail.yRot = Mth.sin(state.ageInTicks * 0.16F) * 0.22F;
        leftFrontLeg.xRot = Mth.sin(walk) * stride;
        rightBackLeg.xRot = leftFrontLeg.xRot;
        rightFrontLeg.xRot = Mth.sin(walk + Mth.PI) * stride;
        leftBackLeg.xRot = rightFrontLeg.xRot;
        spines.yScale = 1.0F + Mth.sin(state.ageInTicks * 0.2F) * 0.04F;
        eyes.xScale = 1.0F + Mth.sin(state.ageInTicks * 0.25F) * 0.08F;
    }

    @Override
    public Map<String, ModelPart> renderCoreParts() {
        return renderCoreParts;
    }
}
