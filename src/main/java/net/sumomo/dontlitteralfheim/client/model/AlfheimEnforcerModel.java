// Uses Botania's Doppleganger CoreShader render layer for the glitch/distortion visual effect

package net.sumomo.dontlitteralfheim.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.sumomo.dontlitteralfheim.entity.AlfheimEnforcerEntity;
import net.sumomo.dontlitteralfheim.entity.EnforcerAttackState;
import vazkii.botania.client.core.helper.RenderHelper;

public class AlfheimEnforcerModel<T extends AlfheimEnforcerEntity> extends EntityModel<T> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			new ResourceLocation("dont_litter_in_alfheim", "alfheim_enforcer"), "main"
	);
	private final ModelPart root;
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart hair;
	private final ModelPart left_arm;
	private final ModelPart right_arm;
	private final ModelPart left_leg;
	private final ModelPart right_leg;

	public AlfheimEnforcerModel(ModelPart root) {
		super(RenderHelper::getDopplegangerLayer);

		this.root = root.getChild("root");
		this.body = this.root.getChild("body");
		this.head = this.root.getChild("head");
		this.hair = this.head.getChild("hair");
		this.left_arm = this.root.getChild("left_arm");
		this.right_arm = this.root.getChild("right_arm");
		this.left_leg = this.root.getChild("left_leg");
		this.right_leg = this.root.getChild("right_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(37, 0).addBox(-4.0F, -28.0F, -2.0F, 8.0F, 14.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(0, 49).addBox(-4.0F, -28.0F, -2.0F, 8.0F, 14.0F, 4.0F, new CubeDeformation(0.5F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 32).addBox(-4.0F, -8.5F, -4.1F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
				.texOffs(33, 32).addBox(-4.0F, -8.5F, -4.1F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.5F)), PartPose.offset(0.0F, -27.5F, 0.1F));

		PartDefinition hair = head.addOrReplaceChild("hair", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -9.0F, -1.0F, 9.0F, 22.0F, 9.0F, new CubeDeformation(0.1F)), PartPose.offset(3.5F, 0.0F, -3.6F));

		PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(66, 19).addBox(0.0F, -1.0F, -1.5F, 3.0F, 14.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(13, 68).addBox(0.0F, -1.0F, -1.5F, 3.0F, 14.0F, 3.0F, new CubeDeformation(0.5F)), PartPose.offset(4.0F, -27.0F, 0.0F));

		PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 68).addBox(-3.0F, -1.0F, -1.5F, 3.0F, 14.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(26, 68).addBox(-3.0F, -1.0F, -1.5F, 3.0F, 14.0F, 3.0F, new CubeDeformation(0.5F)), PartPose.offset(-4.0F, -27.0F, 0.0F));

		PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(42, 49).addBox(-2.0F, 1.0F, -2.0F, 4.0F, 14.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(59, 49).addBox(-2.0F, 1.0F, -2.0F, 4.0F, 14.0F, 4.0F, new CubeDeformation(0.5F)), PartPose.offset(2.0F, -15.0F, 0.0F));

		PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(25, 49).addBox(-2.0F, 1.0F, -2.0F, 4.0F, 14.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(62, 0).addBox(-2.0F, 1.0F, -2.0F, 4.0F, 14.0F, 4.0F, new CubeDeformation(0.5F)), PartPose.offset(-2.0F, -15.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
		this.head.xRot = headPitch * ((float) Math.PI / 180F);

		this.right_arm.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
		this.left_arm.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
		this.right_arm.zRot = 0.0F;
		this.left_arm.zRot = 0.0F;

		this.right_leg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
		this.left_leg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;

		// 髪の毛の微細な揺れ
		this.hair.xRot = 0.04F + Mth.sin(ageInTicks * 0.08F) * 0.03F;

		// 攻撃ステートに応じたポーズ
		EnforcerAttackState state = entity.getAttackState();
		if (state == EnforcerAttackState.BABYLON_BARRAGE) {
			// 滞空中：両腕を広げてやや掲げる詠唱ポーズ
			this.right_arm.xRot = -2.2F + Mth.sin(ageInTicks * 0.2F) * 0.08F;
			this.left_arm.xRot = -2.2F + Mth.cos(ageInTicks * 0.2F) * 0.08F;
			this.right_arm.zRot = 0.35F;
			this.left_arm.zRot = -0.35F;
			// 脚を少し揃えて浮遊感を出す
			this.right_leg.xRot = 0.15F;
			this.left_leg.xRot = 0.25F;
		} else if (state == EnforcerAttackState.MANA_LASER || state == EnforcerAttackState.TRASH_BARRAGE) {
			// 右腕を前方へ突き出す射撃ポーズ
			this.right_arm.xRot = -1.55F + this.head.xRot;
			this.right_arm.zRot = 0.0F;
		} else if (state == EnforcerAttackState.REPULSION_NOVA || state == EnforcerAttackState.SUMMON_PIXIES) {
			// 両腕を左右に広げる解放ポーズ
			this.right_arm.xRot = -1.2F;
			this.left_arm.xRot = -1.2F;
			this.right_arm.zRot = 0.6F;
			this.left_arm.zRot = -0.6F;
		}
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		root.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}