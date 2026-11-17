/*
 * Shader uniform handling adapted from vazkii.botania.client.render.entity.GaiaGuardianRenderer (Botania Mod by Vazkii)
 * Distributed under the Botania License: http://botaniamod.net/license.php
 */
package net.sumomo.dontlitteralfheim.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.sumomo.dontlitteralfheim.client.model.AlfheimEnforcerModel;
import net.sumomo.dontlitteralfheim.entity.AlfheimEnforcerEntity;
import net.sumomo.dontlitteralfheim.entity.EnforcerAttackState;
import org.jetbrains.annotations.NotNull;
import vazkii.botania.client.core.helper.CoreShaders;

public class AlfheimEnforcerRenderer extends MobRenderer<AlfheimEnforcerEntity, AlfheimEnforcerModel<AlfheimEnforcerEntity>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("dont_litter_in_alfheim", "textures/entity/alfheim_enforcer.png");

    public AlfheimEnforcerRenderer(EntityRendererProvider.Context context) {
        super(context, new AlfheimEnforcerModel<>(context.bakeLayer(AlfheimEnforcerModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public void render(@NotNull AlfheimEnforcerEntity entity, float entityYaw, float partialTicks,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        ShaderInstance shader = CoreShaders.doppleganger();
        if (shader != null) {
            float disfiguration = (0.025F + entity.hurtTime * ((1.0F - 0.15F) / 20.0F)) / 2.0F;
            float grainIntensity = 0.05F + entity.hurtTime * ((1.0F - 0.15F) / 10.0F);

            // 王の財宝（滞空射撃）中はオーラと歪みを強める
            if (entity.getAttackState() == EnforcerAttackState.BABYLON_BARRAGE) {
                grainIntensity = Math.max(grainIntensity, 0.25F);
                disfiguration = Math.max(disfiguration, 0.08F);
            }

            shader.safeGetUniform("BotaniaGrainIntensity").set(grainIntensity);
            shader.safeGetUniform("BotaniaDisfiguration").set(disfiguration);
        }

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    protected void scale(AlfheimEnforcerEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(1.0F, 1.0F, 1.0F);
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation(@NotNull AlfheimEnforcerEntity entity) {
        return TEXTURE;
    }
}