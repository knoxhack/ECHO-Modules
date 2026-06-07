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

public class DroneModel extends EntityModel<DroneRenderState> implements RenderCorePartProvider {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "drone"), "main");

    private final ModelPart root;
    private final ModelPart chassis;
    private final ModelPart topPlate;
    private final ModelPart frontLeftPropeller;
    private final ModelPart frontRightPropeller;
    private final ModelPart rearLeftPropeller;
    private final ModelPart rearRightPropeller;
    private final ModelPart antennaLeft;
    private final ModelPart antennaRight;
    private final Map<String, ModelPart> renderCoreParts;

    public DroneModel(ModelPart root) {
        super(root);
        this.root = root;
        this.chassis = root.getChild("chassis");
        this.topPlate = root.getChild("top_plate");
        this.frontLeftPropeller = root.getChild("front_left_propeller");
        this.frontRightPropeller = root.getChild("front_right_propeller");
        this.rearLeftPropeller = root.getChild("rear_left_propeller");
        this.rearRightPropeller = root.getChild("rear_right_propeller");
        this.antennaLeft = root.getChild("antenna_left");
        this.antennaRight = root.getChild("antenna_right");
        this.renderCoreParts = NamedModelParts.builder()
                .put("root", root)
                .put("chassis", chassis)
                .put("body", chassis)
                .put("torso", chassis)
                .put("lens", chassis)
                .put("eyes", chassis)
                .put("scanner", chassis)
                .put("left_rotor", frontLeftPropeller)
                .put("right_rotor", frontRightPropeller)
                .put("rear_rotor", rearRightPropeller)
                .put("front_left_propeller", frontLeftPropeller)
                .put("front_right_propeller", frontRightPropeller)
                .put("rear_left_propeller", rearLeftPropeller)
                .put("rear_right_propeller", rearRightPropeller)
                .put("tool_arm", chassis)
                .put("core", topPlate)
                .put("trail", rearLeftPropeller)
                .put("exhaust", rearRightPropeller)
                .put("ground", chassis)
                .build()
                .asMap();
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("chassis",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.6F, -2.25F, -3.45F, 9.2F, 3.2F, 6.9F)
                        .texOffs(28, 12).addBox(-3.9F, -3.05F, -2.75F, 7.8F, 0.95F, 5.5F),
                PartPose.offset(0.0F, 18.0F, 0.0F));

        root.addOrReplaceChild("top_plate",
                CubeListBuilder.create()
                        .texOffs(0, 12).addBox(-3.6F, -0.55F, -2.65F, 7.2F, 1.1F, 5.3F)
                        .texOffs(30, 12).addBox(-2.25F, -1.0F, -1.65F, 4.5F, 0.8F, 3.3F)
                        .texOffs(44, 44).addBox(-1.0F, -1.25F, -1.0F, 2.0F, 0.35F, 2.0F),
                PartPose.offset(0.0F, 15.25F, 0.0F));

        addArm(root, "front_left_arm", -1.0F, -3.15F);
        addArm(root, "front_right_arm", 1.0F, -3.15F);
        addArm(root, "rear_left_arm", -1.0F, 3.15F);
        addArm(root, "rear_right_arm", 1.0F, 3.15F);
        addPropellerPod(root, "front_left", -8.2F, -5.25F);
        addPropellerPod(root, "front_right", 8.2F, -5.25F);
        addPropellerPod(root, "rear_left", -8.2F, 5.25F);
        addPropellerPod(root, "rear_right", 8.2F, 5.25F);
        addAntenna(root, "antenna_left", -2.3F, -1.9F);
        addAntenna(root, "antenna_right", 2.3F, -1.9F);

        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void addArm(PartDefinition root, String name, float side, float z) {
        float pivotX = side < 0.0F ? -4.15F : 4.15F;
        float boxX = side < 0.0F ? -4.85F : 0.0F;
        root.addOrReplaceChild(name,
                CubeListBuilder.create()
                        .texOffs(24, 48).addBox(boxX, -0.35F, -0.35F, 4.85F, 0.7F, 0.7F)
                        .texOffs(44, 58).addBox(boxX, -0.2F, -0.2F, 4.85F, 0.4F, 0.4F),
                PartPose.offset(pivotX, 18.1F, z));
    }

    private static void addPropellerPod(PartDefinition root, String prefix, float x, float z) {
        root.addOrReplaceChild(prefix + "_pod",
                CubeListBuilder.create()
                        .texOffs(36, 0).addBox(-2.8F, -0.6F, -3.1F, 5.6F, 1.2F, 0.85F)
                        .texOffs(36, 0).addBox(-2.8F, -0.6F, 2.25F, 5.6F, 1.2F, 0.85F)
                        .texOffs(36, 14).addBox(-3.1F, -0.6F, -2.25F, 0.85F, 1.2F, 4.5F)
                        .texOffs(36, 14).addBox(2.25F, -0.6F, -2.25F, 0.85F, 1.2F, 4.5F)
                        .texOffs(48, 16).addBox(-2.1F, 0.35F, -2.1F, 4.2F, 1.2F, 4.2F)
                        .texOffs(52, 32).addBox(-1.05F, -1.05F, -1.05F, 2.1F, 0.45F, 2.1F)
                        .texOffs(56, 8).addBox(-0.9F, -1.25F, -0.9F, 1.8F, 0.25F, 1.8F)
                        .texOffs(22, 34).addBox(-1.2F, 0.95F, -2.65F, 2.4F, 0.55F, 0.45F),
                PartPose.offset(x, 17.25F, z));
        root.addOrReplaceChild(prefix + "_propeller",
                CubeListBuilder.create()
                        .texOffs(44, 56).addBox(-2.35F, -1.16F, -0.16F, 4.7F, 0.24F, 0.32F)
                        .texOffs(56, 40).addBox(-0.16F, -1.18F, -2.35F, 0.32F, 0.24F, 4.7F),
                PartPose.offset(x, 17.25F, z));
    }

    private static void addAntenna(PartDefinition root, String name, float x, float z) {
        root.addOrReplaceChild(name,
                CubeListBuilder.create()
                        .texOffs(56, 0).addBox(-0.3F, -1.7F, -0.3F, 0.6F, 1.7F, 0.6F)
                        .texOffs(56, 8).addBox(-0.45F, -2.15F, -0.45F, 0.9F, 0.55F, 0.9F),
                PartPose.offset(x, 15.05F, z));
    }

    @Override
    public void setupAnim(DroneRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        float hover = (float) Math.sin(age * 0.18F) * 0.18F;
        float pulse = 1.0F + 0.08F * (0.5F + 0.5F * (float) Math.sin(age * 0.35F));

        root.y = hover;
        root.yRot = state.yRot * ((float) Math.PI / 180F);
        root.xRot = state.xRot * ((float) Math.PI / 180F);

        chassis.xRot = hover * 0.045F;
        chassis.zRot = (float) Math.sin(age * 0.12F) * 0.025F;
        topPlate.yRot = (float) Math.sin(age * 0.08F) * 0.05F;
        topPlate.yScale = pulse;

        animateQuadPropeller(frontLeftPropeller, age, 1.0F);
        animateQuadPropeller(frontRightPropeller, age, -1.0F);
        animateQuadPropeller(rearLeftPropeller, age, -1.0F);
        animateQuadPropeller(rearRightPropeller, age, 1.0F);

        antennaLeft.xRot = (float) Math.sin(age * 0.16F) * 0.05F;
        antennaRight.xRot = (float) Math.sin(age * 0.16F + 0.7F) * 0.05F;
    }

    private static void animateQuadPropeller(ModelPart part, float age, float direction) {
        part.yRot = direction * age * 0.85F;
        float pulse = 1.0F + 0.04F * (float) Math.sin(age * 0.42F);
        part.xScale = pulse;
        part.zScale = pulse;
    }

    @Override
    public Map<String, ModelPart> renderCoreParts() {
        return renderCoreParts;
    }
}
