package com.knoxhack.echoorbitalremnants.client;

import com.knoxhack.echoorbitalremnants.EchoOrbitalRemnants;
import com.knoxhack.echoorbitalremnants.entity.EmergencyRocketEntity;
import com.knoxhack.echocore.client.model.EchoNamedModelPartProvider;
import com.knoxhack.echocore.client.model.EchoNamedModelParts;
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

public class EmergencyRocketModel extends EntityModel<EmergencyRocketRenderState> implements EchoNamedModelPartProvider {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "emergency_rocket_vehicle"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart nose;
    private final ModelPart engine;
    private final ModelPart flame;
    private final Map<String, ModelPart> namedParts;

    public EmergencyRocketModel(ModelPart root) {
        super(root);
        this.root = root;
        this.body = root.getChild("body");
        this.nose = root.getChild("nose");
        this.engine = root.getChild("engine");
        this.flame = root.getChild("flame");
        this.namedParts = EchoNamedModelParts.builder()
                .put("root", root)
                .put("body", body)
                .put("torso", body)
                .put("nose", nose)
                .put("head", nose)
                .put("engine", engine)
                .put("core", engine)
                .put("flame", flame)
                .put("exhaust", flame)
                .put("trail", flame)
                .put("ground", engine)
                .build()
                .asMap();
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0F, -24.0F, -4.0F, 8.0F, 24.0F, 8.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        root.addOrReplaceChild("nose",
                CubeListBuilder.create()
                        .texOffs(0, 32)
                        .addBox(-3.0F, -32.0F, -3.0F, 6.0F, 8.0F, 6.0F)
                        .texOffs(24, 32)
                        .addBox(-2.0F, -36.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        root.addOrReplaceChild("engine",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-3.0F, 0.0F, -3.0F, 6.0F, 4.0F, 6.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        root.addOrReplaceChild("fin_north",
                CubeListBuilder.create()
                        .texOffs(32, 10)
                        .addBox(-1.0F, -8.0F, -7.0F, 2.0F, 8.0F, 3.0F, new CubeDeformation(0.1F)),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        root.addOrReplaceChild("fin_south",
                CubeListBuilder.create()
                        .texOffs(42, 10)
                        .addBox(-1.0F, -8.0F, 4.0F, 2.0F, 8.0F, 3.0F, new CubeDeformation(0.1F)),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        root.addOrReplaceChild("fin_west",
                CubeListBuilder.create()
                        .texOffs(52, 10)
                        .addBox(-7.0F, -8.0F, -1.0F, 3.0F, 8.0F, 2.0F, new CubeDeformation(0.1F)),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        root.addOrReplaceChild("fin_east",
                CubeListBuilder.create()
                        .texOffs(32, 21)
                        .addBox(4.0F, -8.0F, -1.0F, 3.0F, 8.0F, 2.0F, new CubeDeformation(0.1F)),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        root.addOrReplaceChild("flame",
                CubeListBuilder.create()
                        .texOffs(42, 21)
                        .addBox(-2.0F, 4.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.25F)),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(EmergencyRocketRenderState state) {
        super.setupAnim(state);
        root.x = 0.0F;
        root.y = 0.0F;
        root.z = 0.0F;

        if (state.launchState == EmergencyRocketEntity.LaunchState.COUNTDOWN.id()) {
            float urgency = 1.0F - Math.max(0.0F, Math.min(1.0F, state.countdownTicks / (float) EmergencyRocketEntity.COUNTDOWN_TICKS));
            root.y = Mth.sin(state.ageInTicks * (0.45F + urgency * 1.2F)) * (0.12F + urgency * 0.32F);
            root.x = Mth.sin(state.ageInTicks * 1.7F) * urgency * 0.05F;
            root.z = Mth.cos(state.ageInTicks * 1.4F) * urgency * 0.05F;
        } else if (state.launchState == EmergencyRocketEntity.LaunchState.LAUNCHING.id()) {
            float shake = 0.08F + state.ascentProgress * 0.16F;
            root.y = -state.ascentProgress * 0.35F + Mth.sin(state.ageInTicks * 2.8F) * 0.08F;
            root.x = Mth.sin(state.ageInTicks * 3.7F) * shake;
            root.z = Mth.cos(state.ageInTicks * 3.1F) * shake;
        }

        flame.visible = state.launchState == EmergencyRocketEntity.LaunchState.LAUNCHING.id();
        flame.yScale = 1.1F + state.ascentProgress * 1.25F + Mth.sin(state.ageInTicks * 1.3F) * 0.25F;
        flame.xScale = 0.85F + state.ascentProgress * 0.35F;
        flame.zScale = flame.xScale;
    }

    @Override
    public Map<String, ModelPart> echoNamedModelParts() {
        return namedParts;
    }
}
