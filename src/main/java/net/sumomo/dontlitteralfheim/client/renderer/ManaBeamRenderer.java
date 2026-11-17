package net.sumomo.dontlitteralfheim.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import net.sumomo.dontlitteralfheim.entity.ManaBeamEntity;
import org.joml.Matrix4f;
import vazkii.botania.client.core.helper.RenderHelper;

public class ManaBeamRenderer extends EntityRenderer<ManaBeamEntity> {

    public ManaBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(ManaBeamEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(ManaBeamEntity entity, float yaw, float partialTicks, PoseStack ms, MultiBufferSource buffers, int light) {
        Vec3 start = entity.position();
        Vec3 end = entity.getEndPos();
        Vec3 diff = end.subtract(start);
        double length = diff.length();
        if (length < 0.001) {
            return;
        }

        float age = entity.tickCount + partialTicks;
        int duration = Math.max(1, entity.getDuration());
        float progress = Mth.clamp(age / duration, 0.0F, 1.0F);

        float radiusScale;
        float alphaScale;
        if (entity.isDense()) {
            // Dense beam starts at full thickness and shrinks while fading out
            radiusScale = 1.0F - (progress * 0.75F);
            alphaScale = 1.0F - progress;
        } else {
            // Continuous beam pulses slightly and fades in/out at edges
            float fadeIn = Math.min(1.0F, age / 4.0F);
            float fadeOut = Math.min(1.0F, Math.max(0.0F, (duration - age) / 4.0F));
            radiusScale = 0.9F + 0.15F * Mth.sin(age * 0.8F);
            alphaScale = fadeIn * fadeOut;
        }

        float outerRadius = entity.getRadius() * radiusScale;
        float coreRadius = outerRadius * (entity.isDense() ? 0.45F : 0.35F);

        Vec3 dir = diff.normalize();
        Vec3 upRef = Math.abs(dir.y) > 0.99 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 side1 = dir.cross(upRef).normalize();
        Vec3 side2 = dir.cross(side1).normalize();

        int argb = entity.getColor();
        float r = ((argb >> 16) & 0xFF) / 255.0F;
        float g = ((argb >> 8) & 0xFF) / 255.0F;
        float b = (argb & 0xFF) / 255.0F;
        float outerAlpha = (((argb >> 24) & 0xFF) / 255.0F) * alphaScale * 0.75F;
        float coreAlpha = alphaScale;

        ms.pushPose();
        Matrix4f mat = ms.last().pose();
        VertexConsumer buffer = buffers.getBuffer(RenderHelper.LIGHTNING);

        // Outer colored aura (2 orthogonal quads)
        drawBeamQuad(buffer, mat, diff, side1, outerRadius, r, g, b, outerAlpha);
        drawBeamQuad(buffer, mat, diff, side2, outerRadius, r, g, b, outerAlpha);

        // Inner high-intensity white core (2 orthogonal quads)
        drawBeamQuad(buffer, mat, diff, side1, coreRadius, 1.0F, 1.0F, 1.0F, coreAlpha);
        drawBeamQuad(buffer, mat, diff, side2, coreRadius, 1.0F, 1.0F, 1.0F, coreAlpha);

        ms.popPose();
    }

    private static void drawBeamQuad(VertexConsumer buffer, Matrix4f mat, Vec3 endOffset, Vec3 sideAxis, float radius, float r, float g, float b, float a) {
        float sx = (float) (sideAxis.x * radius);
        float sy = (float) (sideAxis.y * radius);
        float sz = (float) (sideAxis.z * radius);

        float ex = (float) endOffset.x;
        float ey = (float) endOffset.y;
        float ez = (float) endOffset.z;

        // Front face
        buffer.vertex(mat, -sx, -sy, -sz).color(r, g, b, a).endVertex();
        buffer.vertex(mat,  sx,  sy,  sz).color(r, g, b, a).endVertex();
        buffer.vertex(mat, ex + sx, ey + sy, ez + sz).color(r, g, b, a).endVertex();
        buffer.vertex(mat, ex - sx, ey - sy, ez - sz).color(r, g, b, a).endVertex();

        // Back face (since RenderHelper.LIGHTNING has backface culling enabled)
        buffer.vertex(mat, ex - sx, ey - sy, ez - sz).color(r, g, b, a).endVertex();
        buffer.vertex(mat, ex + sx, ey + sy, ez + sz).color(r, g, b, a).endVertex();
        buffer.vertex(mat,  sx,  sy,  sz).color(r, g, b, a).endVertex();
        buffer.vertex(mat, -sx, -sy, -sz).color(r, g, b, a).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(ManaBeamEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}