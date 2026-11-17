package net.sumomo.dontlitteralfheim.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.sumomo.dontlitteralfheim.entity.MagicCircleEntity;
import org.joml.Matrix4f;
import vazkii.botania.client.core.helper.RenderHelper;

public class MagicCircleRenderer extends EntityRenderer<MagicCircleEntity> {

    public MagicCircleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(MagicCircleEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(MagicCircleEntity entity, float yaw, float partialTicks, PoseStack ms, MultiBufferSource buffers, int light) {
        ms.pushPose();

        float age = entity.tickCount + partialTicks;
        int duration = Math.max(1, entity.getDuration());

        float scaleIn = Math.min(1.0F, age / 8.0F);
        float fadeOut = Math.min(1.0F, Math.max(0.0F, (duration - age) / 8.0F));
        float currentRadius = entity.getRadius() * scaleIn;

        if (entity.isVertical()) {
            ms.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
            ms.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
        } else {
            ms.translate(0.0, 0.02, 0.0);
        }

        ms.mulPose(Axis.YP.rotationDegrees(age * 2.5F));
        ms.scale(currentRadius, currentRadius, currentRadius);

        int argb = entity.getColor();
        float r = ((argb >> 16) & 0xFF) / 255.0F;
        float g = ((argb >> 8) & 0xFF) / 255.0F;
        float b = (argb & 0xFF) / 255.0F;
        float a = (((argb >> 24) & 0xFF) / 255.0F) * fadeOut;

        VertexConsumer buffer = buffers.getBuffer(RenderHelper.BABYLON_ICON);
        Matrix4f mat = ms.last().pose();

        buffer.vertex(mat, -1.0F, 0.0F, -1.0F).color(r, g, b, a).uv(0.0F, 0.0F).endVertex();
        buffer.vertex(mat, -1.0F, 0.0F,  1.0F).color(r, g, b, a).uv(0.0F, 1.0F).endVertex();
        buffer.vertex(mat,  1.0F, 0.0F,  1.0F).color(r, g, b, a).uv(1.0F, 1.0F).endVertex();
        buffer.vertex(mat,  1.0F, 0.0F, -1.0F).color(r, g, b, a).uv(1.0F, 0.0F).endVertex();

        ms.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(MagicCircleEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}