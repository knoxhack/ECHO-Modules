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

public class BoardCrawlerModel extends EntityModel<AshfallLivingRenderState> implements RenderCorePartProvider {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "board_crawler"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart frontClaws;
    private final ModelPart midLegs;
    private final ModelPart rearLegs;
    private final ModelPart acidSacs;
    private final ModelPart mandibles;
    private final ModelPart eyes;
    private final float bodyBaseY;
    private final Map<String, ModelPart> renderCoreParts;

    public BoardCrawlerModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.bodyBaseY = body.y;
        this.head = root.getChild("head");
        this.frontClaws = root.getChild("front_claws");
        this.midLegs = root.getChild("mid_legs");
        this.rearLegs = root.getChild("rear_legs");
        this.acidSacs = body.getChild("acid_sacs");
        this.mandibles = head.getChild("mandibles");
        this.eyes = head.getChild("eyes");
        this.renderCoreParts = NamedModelParts.builder()
                .put("root", root)
                .put("body", body)
                .put("torso", body)
                .put("head", head)
                .put("front_claws", frontClaws)
                .put("mid_legs", midLegs)
                .put("rear_legs", rearLegs)
                .put("acid_sacs", acidSacs)
                .put("side_plates", body.getChild("side_plates"))
                .put("mandibles", mandibles)
                .put("eyes", eyes)
                .put("core", acidSacs)
                .put("scanner", eyes)
                .put("trail", rearLegs)
                .put("exhaust", acidSacs)
                .put("ground", body)
                .build()
                .asMap();
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 20).addBox(-5.8F, -2.9F, -5.4F, 11.6F, 4.9F, 10.8F)
                        .texOffs(0, 46).addBox(-4.7F, 1.35F, -4.3F, 9.4F, 1.35F, 8.6F)
                        .texOffs(30, 20).addBox(-5.0F, -4.15F, -5.2F, 10.0F, 1.35F, 4.7F)
                        .texOffs(30, 34).addBox(-5.35F, -4.45F, -1.8F, 10.7F, 1.25F, 5.7F)
                        .texOffs(30, 48).addBox(-4.3F, -4.25F, 2.8F, 8.6F, 1.25F, 4.4F),
                PartPose.offset(0.0F, 18.9F, 1.2F));
        body.addOrReplaceChild("side_plates",
                CubeListBuilder.create()
                        .texOffs(46, 36).addBox(-6.55F, -1.95F, -4.5F, 1.25F, 3.7F, 8.8F)
                        .mirror().addBox(5.3F, -1.95F, -4.5F, 1.25F, 3.7F, 8.8F),
                PartPose.ZERO);
        body.addOrReplaceChild("acid_sacs",
                CubeListBuilder.create()
                        .texOffs(40, 0).addBox(-4.5F, -5.35F, 0.9F, 3.2F, 2.9F, 3.7F)
                        .mirror().addBox(1.3F, -5.35F, 0.9F, 3.2F, 2.9F, 3.7F)
                        .texOffs(54, 0).addBox(-5.98F, -1.25F, -3.8F, 0.45F, 1.7F, 2.5F)
                        .mirror().addBox(5.53F, -1.25F, -3.8F, 0.45F, 1.7F, 2.5F),
                PartPose.ZERO);
        body.addOrReplaceChild("signal_spine",
                CubeListBuilder.create()
                        .texOffs(48, 12).addBox(-0.6F, -7.05F, -3.6F, 1.2F, 4.8F, 8.4F)
                        .texOffs(54, 0).addBox(-1.15F, -5.9F, -4.25F, 2.3F, 0.7F, 1.2F),
                PartPose.ZERO);
        body.addOrReplaceChild("acid_crown",
                CubeListBuilder.create().texOffs(52, 24).addBox(-3.5F, -6.4F, -3.2F, 7.0F, 2.1F, 5.0F),
                PartPose.ZERO);
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.7F, -2.7F, -6.1F, 9.4F, 4.8F, 6.8F)
                        .texOffs(30, 20).addBox(-4.2F, -3.65F, -6.05F, 8.4F, 1.15F, 4.2F)
                        .texOffs(24, 0).addBox(-3.7F, 1.45F, -6.15F, 7.4F, 1.15F, 3.6F),
                PartPose.offset(0.0F, 18.35F, -4.9F));
        head.addOrReplaceChild("eyes",
                CubeListBuilder.create().texOffs(0, 54).addBox(-3.85F, -1.65F, -6.45F, 7.7F, 1.45F, 0.5F),
                PartPose.ZERO);
        head.addOrReplaceChild("mandibles",
                CubeListBuilder.create()
                        .texOffs(24, 8).addBox(-6.1F, 0.35F, -7.25F, 3.3F, 1.25F, 5.4F)
                        .mirror().addBox(2.8F, 0.35F, -7.25F, 3.3F, 1.25F, 5.4F)
                        .texOffs(54, 8).addBox(-5.8F, -1.05F, -7.7F, 1.25F, 0.85F, 3.9F)
                        .mirror().addBox(4.55F, -1.05F, -7.7F, 1.25F, 0.85F, 3.9F),
                PartPose.ZERO);
        root.addOrReplaceChild("front_claws",
                CubeListBuilder.create()
                        .texOffs(0, 36).addBox(-8.8F, -0.8F, -2.4F, 5.8F, 1.45F, 2.3F)
                        .mirror().addBox(3.0F, -0.8F, -2.4F, 5.8F, 1.45F, 2.3F)
                        .texOffs(14, 36).addBox(-9.4F, 0.1F, -3.1F, 2.7F, 1.25F, 2.8F)
                        .mirror().addBox(6.7F, 0.1F, -3.1F, 2.7F, 1.25F, 2.8F),
                PartPose.offset(0.0F, 20.2F, -4.8F));
        root.addOrReplaceChild("mid_legs",
                CubeListBuilder.create()
                        .texOffs(20, 38).addBox(-8.3F, -0.75F, -1.4F, 4.9F, 1.45F, 2.7F)
                        .mirror().addBox(3.4F, -0.75F, -1.4F, 4.9F, 1.45F, 2.7F)
                        .texOffs(14, 36).addBox(-8.85F, 0.15F, -1.25F, 2.8F, 1.15F, 2.4F)
                        .mirror().addBox(6.05F, 0.15F, -1.25F, 2.8F, 1.15F, 2.4F),
                PartPose.offset(0.0F, 20.4F, -0.2F));
        root.addOrReplaceChild("rear_legs",
                CubeListBuilder.create()
                        .texOffs(20, 38).addBox(-8.0F, -0.8F, -0.6F, 4.9F, 1.55F, 3.9F)
                        .mirror().addBox(3.1F, -0.8F, -0.6F, 4.9F, 1.55F, 3.9F)
                        .texOffs(14, 36).addBox(-8.4F, 0.1F, 1.65F, 2.8F, 1.2F, 2.45F)
                        .mirror().addBox(5.6F, 0.1F, 1.65F, 2.8F, 1.2F, 2.45F),
                PartPose.offset(0.0F, 20.35F, 4.2F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(AshfallLivingRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        body.y = bodyBaseY + Mth.sin(age * 0.2F) * 0.08F;
        head.yRot = state.yRot * ((float) Math.PI / 180F) * 0.7F;
        frontClaws.zRot = Mth.sin(age * 0.24F) * 0.12F;
        midLegs.zRot = -frontClaws.zRot * 0.75F;
        rearLegs.zRot = frontClaws.zRot * 0.55F;
        acidSacs.yScale = 1.0F + Mth.sin(age * 0.28F) * 0.08F;
        mandibles.yScale = 1.0F + Mth.sin(age * 0.32F) * 0.05F;
        eyes.xScale = 1.0F + Mth.sin(age * 0.22F) * 0.08F;
    }

    @Override
    public Map<String, ModelPart> renderCoreParts() {
        return renderCoreParts;
    }
}
