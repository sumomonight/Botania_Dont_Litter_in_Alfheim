package net.sumomo.dontlitteralfheim.client.gui;

import com.tyami.watelib.text.AnimatedText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.sumomo.dontlitteralfheim.DontLitterInAlfheim;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = DontLitterInAlfheim.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class AlfheimBossBarRenderer {
    private static final String ENFORCER_TRANSLATION_KEY = "entity.dont_litter_in_alfheim.alfheim_enforcer";

    // Primary theme colors
    private static final int COLOR_MINT = 0xFFC5FCB2;
    private static final int COLOR_LEMON = 0xFFF5FFA8;
    private static final int COLOR_MINT_DEEP = 0xFF7BC96F;
    private static final int COLOR_BACKDROP = 0xD80D1610;
    private static final int COLOR_TRAIL = 0xDDFAFFE0;

    private static final Map<UUID, Float> TRAILING_PROGRESS = new HashMap<>();

    @SubscribeEvent
    public static void onRenderBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        BossEvent bossEvent = event.getBossEvent();
        if (!isAlfheimEnforcerBar(bossEvent)) {
            return;
        }

        event.setCanceled(true);
        // Reserve total vertical height (title 11px + gap 4px + bar outer 13px + margin 2px)
        event.setIncrement(30);

        GuiGraphics gui = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;

        int screenWidth = gui.guiWidth();
        int centerX = screenWidth / 2;

        // Vertical axis calculation:
        // Title baseline at titleY; font height (9px) + wave bottom (2px) = 11px -> bar outer top at titleY + 12 -> barY at titleY + 15
        int titleY = Math.max(4, event.getY() - 3);
        int barWidth = 220;
        int barHeight = 7; // Odd height (7px) gives an exact 1px center row at barY + 3
        int barX = centerX - (barWidth / 2);
        int barY = titleY + 15;

        float targetProgress = Mth.clamp(bossEvent.getProgress(), 0.0F, 1.0F);
        UUID id = bossEvent.getId();
        float trailing = TRAILING_PROGRESS.getOrDefault(id, targetProgress);
        if (trailing > targetProgress) {
            trailing = Math.max(targetProgress, trailing - 0.0045F);
        } else {
            trailing = targetProgress;
        }
        TRAILING_PROGRESS.put(id, trailing);

        // 1. Dark forest backdrop + double ornamental frame in 0xF5FFA8 & 0xC5FCB2
        gui.fill(barX - 3, barY - 3, barX + barWidth + 3, barY + barHeight + 3, COLOR_BACKDROP);
        drawHollowRect(gui, barX - 3, barY - 3, barWidth + 6, barHeight + 6, COLOR_LEMON);
        drawHollowRect(gui, barX - 1, barY - 1, barWidth + 2, barHeight + 2, 0x99C5FCB2);

        // Left & right diamond ornaments aligned to the exact vertical center axis of the bar (barY + 3)
        int barCenterY = barY + (barHeight / 2);
        drawDiamond(gui, barX - 8, barCenterY, 4, COLOR_LEMON);
        drawDiamond(gui, barX - 8, barCenterY, 2, COLOR_MINT);
        drawDiamond(gui, barX + barWidth + 8, barCenterY, 4, COLOR_LEMON);
        drawDiamond(gui, barX + barWidth + 8, barCenterY, 2, COLOR_MINT);

        // 2. Trailing damage bar
        int trailWidth = Mth.ceil(barWidth * trailing);
        if (trailWidth > 0) {
            gui.fill(barX, barY, barX + trailWidth, barY + barHeight, COLOR_TRAIL);
        }

        // 3. Main HP bar (0xC5FCB2 -> 0xF5FFA8 vertical gradient)
        int filledWidth = Mth.ceil(barWidth * targetProgress);
        if (filledWidth > 0) {
            int topColor = (targetProgress > 0.25F) ? COLOR_MINT : COLOR_LEMON;
            int bottomColor = (targetProgress > 0.25F) ? COLOR_LEMON : COLOR_MINT_DEEP;

            gui.fillGradient(barX, barY, barX + filledWidth, barY + barHeight, topColor, bottomColor);
            gui.fill(barX, barY, barX + filledWidth, barY + 1, 0x88FFFFFF);
        }

        // 4. Subtle segment notches (10% intervals)
        for (int i = 1; i < 10; i++) {
            int notchX = barX + (barWidth * i) / 10;
            gui.fill(notchX, barY, notchX + 1, barY + barHeight, 0x440D1610);
        }

        // 5. Boss title: static ✦ ornaments at ends, wave applied ONLY to the boss name range
        String nameStr = bossEvent.getName().getString();
        String prefix = "✦ ";
        String suffix = " ✦";
        String fullTitle = prefix + nameStr + suffix;

        int nameStart = prefix.length();
        int nameEnd = nameStart + nameStr.length();

        AnimatedText titleText = AnimatedText.of(fullTitle)
                .range(nameStart, nameEnd).wave(2.0F, 1.00F, 0.50F)
                .gradient(0xC5FCB2, 0xF5FFA8)
                .gradientSpeed(1.00F)
                .gradientPhase(0.50F);

        int textWidth = titleText.getWidth(font);
        int textX = centerX - (textWidth / 2);
        titleText.render(gui, textX, titleY);
    }

    private static boolean isAlfheimEnforcerBar(BossEvent bossEvent) {
        Component name = bossEvent.getName();
        if (name.getContents() instanceof TranslatableContents translatable) {
            return ENFORCER_TRANSLATION_KEY.equals(translatable.getKey());
        }
        return false;
    }

    private static void drawHollowRect(GuiGraphics gui, int x, int y, int width, int height, int color) {
        gui.fill(x, y, x + width, y + 1, color);
        gui.fill(x, y + height - 1, x + width, y + height, color);
        gui.fill(x, y, x + 1, y + height, color);
        gui.fill(x + width - 1, y, x + width, y + height, color);
    }

    private static void drawDiamond(GuiGraphics gui, int centerX, int centerY, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int span = radius - Math.abs(dy);
            gui.fill(centerX - span, centerY + dy, centerX + span + 1, centerY + dy + 1, color);
        }
    }
}