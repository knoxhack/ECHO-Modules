package com.knoxhack.echoashfallprotocol.client.renderer;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echorendercore.client.NamedModelParts;
import com.knoxhack.echorendercore.client.RenderCorePartProvider;
import java.util.Map;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class BoardWraithModel extends EntityModel<AshfallLivingRenderState> implements RenderCorePartProvider {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "board_wraith"), "main");

    private final ModelPart head;
    private final ModelPart veil;
    private final ModelPart leftClaw;
    private final ModelPart rightClaw;
    private final ModelPart smokeTrail;
    private final ModelPart core;
    private final ModelPart eyes;
    private final Map<String, ModelPart> renderCoreParts;

    public BoardWraithModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.veil = root.getChild("veil");
        this.leftClaw = root.getChild("left_claw");
        this.rightClaw = root.getChild("right_claw");
        this.smokeTrail = root.getChild("smoke_trail");
        this.core = veil.getChild("core");
        this.eyes = head.getChild("eyes");
        this.renderCoreParts = NamedModelParts.builder()
                .put("root", root)
                .put("head", head)
                .put("veil", veil)
                .put("body", veil)
                .put("torso", veil)
                .put("left_claw", leftClaw)
                .put("right_claw", rightClaw)
                .put("smoke_trail", smokeTrail)
                .put("trail", smokeTrail)
                .put("core", core)
                .put("eyes", eyes)
                .put("scanner", eyes)
                .put("exhaust", smokeTrail)
                .put("ground", smokeTrail)
                .build()
                .asMap();
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -7.0F, -4.0F, 8.0F, 7.0F, 8.0F)
                        .texOffs(32, 0).addBox(-4.6F, -7.6F, -4.6F, 9.2F, 8.0F, 9.2F, new CubeDeformation(0.2F)),
                PartPose.offset(0.0F, 6.5F, 0.0F));
        head.addOrReplaceChild("eyes",
                CubeListBuilder.create().texOffs(0, 54).addBox(-3.0F, -4.6F, -4.5F, 6.0F, 1.1F, 0.5F),
                PartPose.ZERO);
        PartDefinition veil = root.addOrReplaceChild("veil",
                CubeListBuilder.create()
                        .texOffs(0, 18).addBox(-4.8F, -1.0F, -2.7F, 9.6F, 13.0F, 5.4F)
                        .texOffs(30, 18).addBox(-5.6F, 2.0F, -3.0F, 11.2F, 9.0F, 6.0F, new CubeDeformation(0.12F)),
                PartPose.offset(0.0F, 7.0F, 0.0F));
        veil.addOrReplaceChild("core",
                CubeListBuilder.create().texOffs(44, 44).addBox(-2.0F, 3.0F, -3.35F, 4.0F, 4.0F, 0.8F),
                PartPose.ZERO);
        root.addOrReplaceChild("left_claw",
                CubeListBuilder.create().texOffs(0, 40).addBox(-1.0F, -1.0F, -1.2F, 3.0F, 10.0F, 2.4F),
                PartPose.offset(5.2F, 9.0F, -0.2F));
        root.addOrReplaceChild("right_claw",
                CubeListBuilder.create().texOffs(0, 40).mirror().addBox(-2.0F, -1.0F, -1.2F, 3.0F, 10.0F, 2.4F),
                PartPose.offset(-5.2F, 9.0F, -0.2F));
        root.addOrReplaceChild("smoke_trail",
                CubeListBuilder.create()
                        .texOffs(16, 40).addBox(-3.2F, 0.0F, -2.2F, 6.4F, 7.0F, 4.4F)
                        .texOffs(38, 50).addBox(-2.0F, 6.0F, -1.4F, 4.0F, 4.0F, 2.8F),
                PartPose.offset(0.0F, 18.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(AshfallLivingRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        head.yRot = state.yRot * ((float) Math.PI / 180F) * 0.5F;
        head.xRot = state.xRot * ((float) Math.PI / 180F) * 0.4F;
        veil.y = Mth.sin(age * 0.12F) * 0.22F;
        veil.zRot = Mth.sin(age * 0.08F) * 0.025F;
        leftClaw.xRot = -0.18F + Mth.sin(age * 0.16F) * 0.12F;
        rightClaw.xRot = -0.18F - Mth.sin(age * 0.16F) * 0.12F;
        smokeTrail.yScale = 1.0F + Mth.sin(age * 0.18F) * 0.08F;
        core.xScale = 1.0F + Mth.sin(age * 0.24F) * 0.1F;
        eyes.xScale = 1.0F + Mth.sin(age * 0.28F) * 0.06F;
    }

    @Override
    public Map<String, ModelPart> renderCoreParts() {
        return renderCoreParts;
    }
}
