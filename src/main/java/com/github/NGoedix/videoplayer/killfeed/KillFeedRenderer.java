package com.github.NGoedix.videoplayer.killfeed;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class KillFeedRenderer {

    private static final int BAR_H = 15;
    private static final int PAD = 4;
    private static final int RIGHT_PAD_LEFT = 2;
    private static final int CHEVRON_W = 8;
    private static final int ENTRY_GAP = 2;
    private static final int BADGE_W = 15;
    private static final int TEXT_TOP = 3;

    private static final int FIRE_TEX_W = 22;
    private static final int FIRE_TEX_H = 10;
    private static final int FIRE_FRAMES = 60;
    private static final long FIRE_FRAME_MS = 100L;
    private static final int FIRE_SLOT_W = FIRE_TEX_W + 2;
    private static final int FIRE_STREAK_MIN = 6;
    private static final int STREAK_BADGE_MIN = 2;

    static final long SLIDE_MS = 250L;

    private static final Identifier CHEVRON_LEFT = KillFeedAssets.crown("killfeed/left");
    private static final Identifier CHEVRON_RIGHT = KillFeedAssets.crown("killfeed/right");
    private static final Identifier FIRE_TEX = KillFeedAssets.crown("textures/killfeed/streaks/rampage_fire.png");

    private KillFeedRenderer() {}

    public static void render(GuiGraphics guiGraphics) {
        if (!KillFeed.enabled) return;
        List<KillEntry> entries = KillFeed.entries();
        if (entries.isEmpty()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        Font font = minecraft.font;
        String self = minecraft.player.getName().getString();

        List<KillEntry> ordered = new ArrayList<>(entries);
        if (KillFeed.reverseOrder) {
            java.util.Collections.reverse(ordered);
        }

        int screenWidth = guiGraphics.guiWidth();
        long now = System.currentTimeMillis();
        int y = KillFeed.positionY;
        for (KillEntry entry : ordered) {
            int width = entryWidth(font, entry, self);
            int baseX = KillFeed.rightSide ? screenWidth - width - KillFeed.marginX : KillFeed.marginX;
            int slide = slideOffset(entry, now, width, screenWidth, baseX);
            int x = KillFeed.rightSide ? baseX + slide : baseX - slide;
            renderEntry(guiGraphics, font, entry, self, x, y);
            y += BAR_H + ENTRY_GAP;
        }
    }

    private static int slideOffset(KillEntry entry, long now, int width, int screenWidth, int baseX) {
        int distance = KillFeed.rightSide ? (screenWidth - baseX) : (baseX + width);

        if (entry.removingAt != 0L) {
            long elapsed = now - entry.removingAt;
            if (elapsed <= 0) return 0;
            if (elapsed >= SLIDE_MS) return distance;
            float p = elapsed / (float) SLIDE_MS;
            float eased = p * p * p;
            return Math.round(eased * distance);
        }

        long elapsed = now - entry.timestamp;
        if (elapsed >= SLIDE_MS || elapsed < 0) return 0;
        float p = elapsed / (float) SLIDE_MS;
        float eased = 1f - (1f - p) * (1f - p) * (1f - p);
        return Math.round((1f - eased) * distance);
    }

    private static MutableComponent attackerContent(KillEntry entry, String self) {
        MutableComponent c = entry.attacker.head();
        c.append(nameComponent(entry.attacker.name, self));
        c.append(Component.literal(" ").append(entry.method.icon()));
        return c;
    }

    private static MutableComponent victimContent(KillEntry entry, String self) {
        return entry.victim.head().append(nameComponent(entry.victim.name, self));
    }

    private static MutableComponent nameComponent(String name, String self) {
        boolean isSelf = self.equals(name);
        String text = (isSelf && KillFeed.showYouInKill ? " (YOU) " : " ") + name.toUpperCase(Locale.ROOT);
        return KillFeedAssets.withFont(Component.literal(text), KillFeedAssets.MCC_HUD_FONT)
                .withStyle(Style.EMPTY.withColor(0xFFFFFF));
    }

    private static int entryWidth(Font font, KillEntry entry, String self) {
        int width = 0;
        if (entry.attacker != null) {
            if (entry.hasAssist) width += BADGE_W;
            width += streakSlotWidth(entry);
            width += PAD + font.width(attackerContent(entry, self)) + RIGHT_PAD_LEFT;
        } else {
            width += PAD + font.width(entry.method.icon()) + RIGHT_PAD_LEFT;
        }
        width += CHEVRON_W;
        width += PAD + font.width(victimContent(entry, self)) + PAD;
        return width;
    }

    private static void renderEntry(GuiGraphics g, Font font, KillEntry entry, String self, int x, int y) {
        int attackerRgb = entry.attacker != null ? entry.attacker.color : 0x606060;
        int victimRgb = entry.victim.color;
        int attackerAlpha = entry.attacker != null && self.equals(entry.attacker.name) ? 192 : 128;
        int victimAlpha = self.equals(entry.victim.name) ? 192 : 128;
        if (entry.attacker != null && attackerAlpha == 128 && victimAlpha == 128 && attackerRgb == victimRgb) {
            victimAlpha = 96;
        }
        int attackerColor = KillFeedAssets.color(attackerRgb, attackerAlpha);
        int victimColor = KillFeedAssets.color(victimRgb, victimAlpha);

        int cursor = x;

        if (entry.attacker != null) {
            if (entry.hasAssist) {
                drawAssistBadge(g, attackerColor, cursor, y);
                cursor += BADGE_W;
            }
            if (entry.streak >= FIRE_STREAK_MIN) {
                drawFireBadge(g, cursor, y);
                cursor += FIRE_SLOT_W;
            } else if (entry.streak >= STREAK_BADGE_MIN) {
                drawStreakBadge(g, attackerColor, entry.streak, cursor, y);
                cursor += BADGE_W;
            }
            MutableComponent content = attackerContent(entry, self);
            int leftW = PAD + font.width(content) + RIGHT_PAD_LEFT;
            fillRoundedLeft(g, cursor, y, leftW, attackerColor);
            g.drawString(font, content, cursor + PAD, y + TEXT_TOP, 0xFFFFFFFF);
            cursor += leftW;
        } else {
            int leftW = PAD + font.width(entry.method.icon()) + RIGHT_PAD_LEFT;
            fillRoundedLeft(g, cursor, y, leftW, attackerColor);
            g.drawString(font, entry.method.icon(), cursor + PAD, y + TEXT_TOP, 0xFFFFFFFF);
            cursor += leftW;
        }

        drawChevron(g, cursor, y, attackerColor, victimColor);
        cursor += CHEVRON_W;

        MutableComponent victim = victimContent(entry, self);
        int victimW = PAD + font.width(victim) + PAD;
        fillRoundedRight(g, cursor, y, victimW, victimColor);
        g.drawString(font, victim, cursor + PAD, y + TEXT_TOP, 0xFFFFFFFF);
    }

    private static void drawChevron(GuiGraphics g, int x, int y, int attackerColor, int victimColor) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, CHEVRON_LEFT, x, y, CHEVRON_W, BAR_H, attackerColor);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, CHEVRON_RIGHT, x, y, CHEVRON_W, BAR_H, victimColor);
    }

    private static int streakSlotWidth(KillEntry entry) {
        if (entry.attacker == null) return 0;
        if (entry.streak >= FIRE_STREAK_MIN) return FIRE_SLOT_W;
        if (entry.streak >= STREAK_BADGE_MIN) return BADGE_W;
        return 0;
    }

    private static void drawStreakBadge(GuiGraphics g, int color, int streak, int x, int y) {
        int by = y + BAR_H - 9;
        fillRoundedAll(g, x, by, 13, 9, color);
        int coerced = Math.max(1, Math.min(5, streak));
        Identifier tex = KillFeedAssets.crown("textures/killfeed/streaks/streak" + coerced + ".png");
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, by, 0.0F, 0.0F, 13, 9, 13, 9);
    }

    private static void drawFireBadge(GuiGraphics g, int x, int y) {
        int frame = (int) ((System.currentTimeMillis() / FIRE_FRAME_MS) % FIRE_FRAMES);
        float v = frame * FIRE_TEX_H;
        g.blit(RenderPipelines.GUI_TEXTURED, FIRE_TEX, x + 2, y + 3, 0.0F, v,
                FIRE_TEX_W, FIRE_TEX_H, FIRE_TEX_W, FIRE_TEX_H * FIRE_FRAMES);
    }

    private static void drawAssistBadge(GuiGraphics g, int color, int x, int y) {
        int by = y + BAR_H - 9;
        fillRoundedAll(g, x, by, 13, 9, color);
        Minecraft minecraft = Minecraft.getInstance();
        String self = minecraft.player.getName().getString();
        g.drawString(minecraft.font, new PlayerRef(self).head(), x + 2, by + 1, 0xFFFFFFFF);
    }

    private static void fillRoundedLeft(GuiGraphics g, int x, int y, int w, int color) {
        g.fill(x, y + 1, x + 1, y + BAR_H - 1, color);
        g.fill(x + 1, y, x + w, y + BAR_H, color);
    }

    private static void fillRoundedRight(GuiGraphics g, int x, int y, int w, int color) {
        g.fill(x, y, x + w - 1, y + BAR_H, color);
        g.fill(x + w - 1, y + 1, x + w, y + BAR_H - 1, color);
    }

    private static void fillRoundedAll(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + 1, y, x + w - 1, y + h, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }
}
