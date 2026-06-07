package com.knoxhack.echoashfallprotocol.client.renderer;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Custom model for the Warden Boss — bulkier, more menacing humanoid proportions.
 * Wider torso, elongated arms with weapon mounts, thicker legs.
 */
public class WardenBossModel<S extends HumanoidRenderState> extends HumanoidModel<S> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "warden_boss"), "main");

    public WardenBossModel(ModelPart root) {
        super(root);
    }

    /**
     * Creates the layer definition for the Warden Boss mesh.
     * Scaled up from standard humanoid for imposing presence.
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Head — slightly larger
        PartDefinition head = partdefinition.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, -9.0F, -4.5F, 9.0F, 9.0F, 9.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // Hat layer (for helmet/visor effects)
        head.addOrReplaceChild("hat",
                CubeListBuilder.create().texOffs(32, 0).addBox(-4.5F, -9.0F, -4.5F, 9.0F, 9.0F, 9.0F, new CubeDeformation(0.5F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // Body — wider and taller for bulk
        partdefinition.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 16).addBox(-5.0F, 0.0F, -3.0F, 10.0F, 14.0F, 6.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // Right arm — longer with slight outward angle
        partdefinition.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 16).addBox(-3.5F, -2.0F, -2.5F, 5.0F, 15.0F, 5.0F),
                PartPose.offset(-6.0F, 2.0F, 0.0F));

        // Left arm — mirror of right
        partdefinition.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.5F, -2.0F, -2.5F, 5.0F, 15.0F, 5.0F),
                PartPose.offset(6.0F, 2.0F, 0.0F));

        // Right leg — thicker
        partdefinition.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 14.0F, 5.0F),
                PartPose.offset(-2.5F, 10.0F, 0.0F));

        // Left leg — mirror of right
        partdefinition.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.5F, 0.0F, -2.5F, 5.0F, 14.0F, 5.0F),
                PartPose.offset(2.5F, 10.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 128, 64);
    }
}
