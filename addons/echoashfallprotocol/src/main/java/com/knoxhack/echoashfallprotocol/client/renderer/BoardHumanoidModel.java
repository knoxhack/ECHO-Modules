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

public class BoardHumanoidModel<S extends HumanoidRenderState> extends HumanoidModel<S> implements RenderCorePartProvider {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "board_humanoid"), "main");

    private final Map<String, ModelPart> renderCoreParts;

    public BoardHumanoidModel(ModelPart root) {
        super(root);
        ModelPart core = body.getChild("core");
        ModelPart pack = body.getChild("pack");
        ModelPart scanner = pack.getChild("scanner");
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
                .put("pack", pack)
                .put("satchel", pack)
                .put("radio", scanner)
                .put("core", core)
                .put("eyes", eyes)
                .put("scanner", scanner)
                .put("trail", pack)
                .put("exhaust", pack)
                .put("ground", body)
                .build()
                .asMap();
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)
                        .texOffs(32, 0).addBox(-4.5F, -8.5F, -4.5F, 9.0F, 9.0F, 9.0F, new CubeDeformation(0.18F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("eyes",
                CubeListBuilder.create().texOffs(0, 52).addBox(-3.0F, -5.1F, -4.55F, 6.0F, 1.3F, 0.55F),
                PartPose.ZERO);
        head.addOrReplaceChild("hat",
                CubeListBuilder.create().texOffs(32, 0).addBox(-4.5F, -8.5F, -4.5F, 9.0F, 9.0F, 9.0F, new CubeDeformation(0.26F)),
                PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(16, 16).addBox(-4.5F, 0.0F, -2.4F, 9.0F, 12.0F, 4.8F)
                        .texOffs(16, 36).addBox(-5.0F, 1.3F, -2.8F, 10.0F, 4.5F, 5.6F, new CubeDeformation(0.08F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("core",
                CubeListBuilder.create().texOffs(44, 36).addBox(-2.0F, 3.4F, -3.15F, 4.0F, 4.0F, 0.8F, new CubeDeformation(0.05F)),
                PartPose.ZERO);
        PartDefinition pack = body.addOrReplaceChild("pack",
                CubeListBuilder.create()
                        .texOffs(48, 18).addBox(-3.5F, 2.0F, 2.25F, 7.0F, 8.0F, 2.4F)
                        .texOffs(48, 29).addBox(-1.1F, 0.6F, 3.1F, 2.2F, 2.0F, 1.0F),
                PartPose.ZERO);
        pack.addOrReplaceChild("scanner",
                CubeListBuilder.create().texOffs(58, 50).addBox(-0.45F, -3.0F, 3.4F, 0.9F, 3.0F, 0.9F),
                PartPose.ZERO);
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                        .texOffs(40, 16).addBox(-3.1F, -2.0F, -2.0F, 4.1F, 12.0F, 4.0F)
                        .texOffs(40, 48).addBox(-3.45F, 6.2F, -2.25F, 4.5F, 3.5F, 4.5F, new CubeDeformation(0.05F)),
                PartPose.offset(-5.4F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                        .texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.1F, 12.0F, 4.0F)
                        .texOffs(40, 48).mirror().addBox(-1.05F, 6.2F, -2.25F, 4.5F, 3.5F, 4.5F, new CubeDeformation(0.05F)),
                PartPose.offset(5.4F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create()
                        .texOffs(0, 16).addBox(-2.1F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F)
                        .texOffs(0, 36).addBox(-2.35F, 7.0F, -2.25F, 4.45F, 4.5F, 4.5F, new CubeDeformation(0.05F)),
                PartPose.offset(-2.0F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create()
                        .texOffs(0, 16).mirror().addBox(-1.9F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F)
                        .texOffs(0, 36).mirror().addBox(-2.1F, 7.0F, -2.25F, 4.45F, 4.5F, 4.5F, new CubeDeformation(0.05F)),
                PartPose.offset(2.0F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public Map<String, ModelPart> renderCoreParts() {
        return renderCoreParts;
    }
}
