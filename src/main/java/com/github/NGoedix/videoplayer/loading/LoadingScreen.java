package com.github.NGoedix.videoplayer.loading;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Crown's animated game loading overlay, triggered by the CrownChampionshipUtils plugin
 * ({@code loadscreen;<seconds>;<title>;<subtitle>;<game>} on {@code crown:play};
 * {@code loadscreen off} clears it early).
 *
 * <p>Layers, back to front: the dark vignette background (slow breathing zoom), the game's bundled
 * video clips cycling grayscale + darkened on top of it ({@link LoadingClips}; the {@code hub} set
 * for games without their own), two columns of grey
 * game icons drifting in opposite directions along the screen edges, a pulsing blue glow behind
 * the current game's icon (which pops in, bobs and sways), the title + subtitle, and a rounded
 * yellow progress pill with a shimmer sweep that fills across the given duration. Textures live
 * under {@code assets/crown/textures/loading/} (background, glow, {@code games/}, {@code side/}).
 * Unknown/absent game → the pixel-art Crown crown ({@code crown.png}) stands in for the icon. Fades itself
 * out when the duration elapses (or on {@code loadscreen off} / disconnect).</p>
 */
@Environment(EnvType.CLIENT)
public final class LoadingScreen {

    private static final int FADE_IN_MS = 350;
    private static final int FADE_OUT_MS = 500;
    private static final int HOLD_AFTER_FULL_MS = 350;   // bar full -> small hold -> auto fade
    private static final int ICON_DELAY_MS = 120;
    private static final int ICON_POP_MS = 550;
    private static final int TITLE_DELAY_MS = 220;
    private static final int TITLE_POP_MS = 500;
    private static final int SUB_DELAY_MS = 450;
    private static final int SUB_FADE_MS = 450;
    private static final long SHIMMER_PERIOD_MS = 1400L;
    private static final long ZOOM_PERIOD_MS = 12000L;

    private static final int BAR_YELLOW = 0xFFC83D;
    private static final int BAR_YELLOW_TOP = 0xFFE58A;
    private static final int BAR_RING_BLUE = 0x3B82F6;
    private static final int SUBTITLE_GRAY = 0xA9B8D6;

    private static final int BG_TEX_W = 1024;
    private static final int BG_TEX_H = 576;
    private static final int GLOW_TEX = 256;

    private static final Identifier BACKGROUND = tex("background.png");
    private static final Identifier GLOW = tex("glow.png");
    private static final Identifier CROWN = tex("crown.png");
    private static final int CROWN_TEX = 16;

    /** A game's icon file (under games/ and side/), display name, and icon texture size. */
    private record Game(String file, String name, int size, boolean hasSide) {
        Identifier icon() { return tex("games/" + file + ".png"); }
        Identifier side() { return tex("side/" + file + ".png"); }
    }

    /** Lookup by every accepted id: Crown's short game ids (skb, sg, ...) and the full names. */
    private static final Map<String, Game> GAMES = new LinkedHashMap<>();
    /** Every game with a grey side icon, in a fixed order, for the drifting edge columns. */
    private static final List<Game> SIDE_GAMES = new ArrayList<>();

    static {
        game(new Game("ace_race", "Ace Race", 32, true), "ar");
        game(new Game("battle_box", "Battle Box", 32, true), "bb");
        game(new Game("bingo_but_fast", "Bingo But Fast", 32, true), "bbf");
        game(new Game("build_mart", "Build Mart", 32, true), "bm");
        game(new Game("dodge_bolt", "Dodgebolt", 32, true), "db", "dodgebolt");
        game(new Game("grid_runners", "Grid Runners", 64, true), "gr");
        game(new Game("hole_in_the_wall", "Hole in the Wall", 32, true), "hitw");
        game(new Game("meltdown", "Meltdown", 32, true), "md");
        game(new Game("parkour_tag", "Parkour Tag", 32, true), "pkt");
        game(new Game("parkour_warrior", "Parkour Warrior", 128, true), "pkw");
        game(new Game("railroad_rush", "Railroad Rush", 32, true), "rr");
        game(new Game("rocket_spleef", "Rocket Spleef", 32, true), "rs");
        game(new Game("sands_of_time", "Sands of Time", 64, true), "sot");
        game(new Game("sky_battle", "Sky Battle", 32, true), "skb", "sb");
        game(new Game("survival_games", "Survival Games", 32, true), "sg");
        game(new Game("tgttosawaf", "TGTTOSAWAF", 32, true), "tgttos");
    }

    private static void game(Game game, String... aliases) {
        GAMES.put(game.file(), game);
        for (String alias : aliases) GAMES.put(alias, game);
        if (game.hasSide()) SIDE_GAMES.add(game);
    }

    /** Resolves a plugin-supplied game id (case/space/dash-insensitive); null when unknown. */
    public static String gameName(String id) {
        Game game = findGame(id);
        return game == null ? null : game.name();
    }

    private static Game findGame(String id) {
        if (id == null) return null;
        String key = id.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        return key.isEmpty() ? null : GAMES.get(key);
    }

    // Active state (only touched on the render/client thread).
    private static String title = "";
    private static String subtitle = "";
    private static Game game;
    private static long startMs;
    private static int durationMs;
    private static long hideStartMs;   // 0 = not fading out yet
    private static boolean showing;
    private static boolean clipsActive;  // LoadingClips running for this screen

    private LoadingScreen() {}

    public static void register() {
        // Registered after the kill feed in ClientHandler, so this draws on top of it.
        HudRenderCallback.EVENT.register((guiGraphics, tickCounter) -> render(guiGraphics));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(LoadingScreen::finish));
    }

    /** Hard stop: overlay gone, clip decoders stopped. */
    private static void finish() {
        showing = false;
        if (clipsActive) {
            LoadingClips.stop();
            clipsActive = false;
        }
    }

    public static boolean isShowing() {
        return showing;
    }

    /**
     * Starts (or restarts) the overlay. A blank title falls back to the game's name. The game's
     * bundled clips (or the {@code hub} ones) play grayscale behind everything. Must be called on
     * the client thread.
     */
    public static void show(String newTitle, String newSubtitle, String gameId, int newDurationMs) {
        game = findGame(gameId);
        clipsActive = LoadingClips.start(game != null ? game.file() : "hub");
        String t = newTitle == null ? "" : newTitle.trim();
        if (t.isEmpty() && game != null) t = game.name();
        title = t.toUpperCase(Locale.ROOT);
        subtitle = newSubtitle == null ? "" : newSubtitle.trim();
        durationMs = Math.max(1000, newDurationMs);
        startMs = System.currentTimeMillis();
        hideStartMs = 0L;
        showing = true;
    }

    /** Begins the fade-out (early dismiss). Safe to call when nothing is showing. */
    public static void hide() {
        if (showing && hideStartMs == 0L) {
            hideStartMs = System.currentTimeMillis();
        }
    }

    private static void render(GuiGraphics g) {
        if (!showing) return;
        long now = System.currentTimeMillis();
        long elapsed = now - startMs;

        // Bar full + hold -> start the fade on our own, so a missing "off" can never trap anyone.
        if (hideStartMs == 0L && elapsed >= durationMs + HOLD_AFTER_FULL_MS) {
            hideStartMs = now;
        }

        float alpha;
        if (hideStartMs != 0L) {
            float out = (now - hideStartMs) / (float) FADE_OUT_MS;
            if (out >= 1f) { finish(); return; }
            // Fading out may start mid-fade-in; never pop brighter than we were.
            alpha = Math.min(fadeIn(hideStartMs - startMs), 1f - smooth(out));
        } else {
            alpha = fadeIn(elapsed);
        }
        if (alpha <= 0.01f) return;

        Font font = Minecraft.getInstance().font;
        int w = g.guiWidth();
        int h = g.guiHeight();
        int cx = w / 2;

        int iconSize = Math.max(48, Math.min(120, Math.round(h * 0.26f)));
        int iconCy = Math.round(h * 0.40f);

        drawBackground(g, w, h, now, alpha);
        if (clipsActive && LoadingClips.render(g, w, h, alpha, now)) {
            drawClipShade(g, w, h, alpha);
        }
        drawSideColumns(g, w, h, now, alpha);
        drawGlow(g, cx, iconCy, iconSize, elapsed, now, alpha);
        if (game != null) {
            drawIcon(g, game.icon(), game.size(), cx, iconCy, iconSize, elapsed, now, alpha);
        } else {
            drawIcon(g, CROWN, CROWN_TEX, cx, iconCy, iconSize, elapsed, now, alpha);
        }

        int titleY = iconCy + iconSize / 2 + 18;
        drawTitle(g, font, cx, titleY, w, elapsed, alpha);
        drawSubtitle(g, font, cx, titleY + 16, elapsed, alpha);
        drawProgress(g, font, cx, w, h - 40, elapsed, now, alpha);
    }

    // --- Layers ---

    /**
     * Cover-fit background with a slow breathing zoom so the screen never feels static. Placed with
     * a float transform (not rounded int sizes) so the zoom glides instead of stepping a pixel.
     */
    private static void drawBackground(GuiGraphics g, int w, int h, long now, float alpha) {
        float breathe = 0.5f - 0.5f * (float) Math.cos(2 * Math.PI * (now % ZOOM_PERIOD_MS) / ZOOM_PERIOD_MS);
        float scale = Math.max(w / (float) BG_TEX_W, h / (float) BG_TEX_H) * (1f + 0.04f * breathe);
        g.fill(0, 0, w, h, argb(alpha, 0x0E0F12));
        g.pose().pushMatrix();
        g.pose().translate(w / 2f, h / 2f);
        g.pose().scale(scale, scale);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, -BG_TEX_W / 2, -BG_TEX_H / 2, 0f, 0f,
                BG_TEX_W, BG_TEX_H, BG_TEX_W, BG_TEX_H, BG_TEX_W, BG_TEX_H, argb(alpha, 0xFFFFFF));
        g.pose().popMatrix();
    }

    /**
     * Darkens the grayscale clips so the icon, title and bar stay readable: an even dim plus a
     * heavier fade toward the top and bottom edges.
     */
    private static void drawClipShade(GuiGraphics g, int w, int h, float alpha) {
        g.fill(0, 0, w, h, argb(alpha * 0.45f, 0x05070B));
        int band = h / 4;
        g.fillGradient(0, 0, w, band, argb(alpha * 0.55f, 0x05070B), argb(0f, 0x05070B));
        g.fillGradient(0, h - band, w, h, argb(0f, 0x05070B), argb(alpha * 0.65f, 0x05070B));
    }

    /**
     * Grey game icons drifting in two columns hugging the screen edges — the left column scrolls up,
     * the right column down (offset by half the list so the two never mirror). Each icon fades out
     * as it nears the top/bottom edge.
     */
    private static void drawSideColumns(GuiGraphics g, int w, int h, long now, float alpha) {
        if (SIDE_GAMES.isEmpty() || w < 220) return;
        int size = 18;
        int spacing = 34;
        int count = SIDE_GAMES.size();
        // Computed in double and wrapped by one full cycle: (now * 0.018f) in float lost all
        // precision at epoch-millis magnitudes, so the scroll moved in big uneven steps.
        float scroll = (float) ((now * 0.018) % (count * (double) spacing));
        int edgeFade = 40;

        for (int col = 0; col < 2; col++) {
            boolean left = col == 0;
            int baseX = left ? 12 : w - 12 - size;
            float offset = left ? -scroll : scroll;
            // Repeat the icon list down the whole column (slots k*spacing + offset), so there's
            // never a gap however tall the screen is — the old single pass of `count` icons
            // left an empty band at the bottom once the screen was taller than the list.
            int kStart = (int) Math.floor((-size - offset) / spacing);
            int kEnd = (int) Math.ceil((h - offset) / spacing);
            for (int k = kStart; k <= kEnd; k++) {
                float y = k * spacing + offset;
                if (y < -size || y > h) continue;
                float edge = Math.min(y + size / 2f, h - y - size / 2f) / edgeFade;
                float a = alpha * 0.32f * clamp01(edge);
                if (a <= 0.01f) continue;
                int idx = Math.floorMod(k + (left ? 0 : count / 2), count);
                Game gm = SIDE_GAMES.get(idx);
                float sway = (float) Math.sin(now / 1100.0 + idx * 1.3) * 2f;
                // Float transform, not Math.round'ed blit coords: rounding made the icons twitch a
                // whole pixel back and forth as the sway/scroll crossed .5 boundaries.
                g.pose().pushMatrix();
                g.pose().translate(baseX + sway, y);
                g.blit(RenderPipelines.GUI_TEXTURED, gm.side(), 0, 0, 0f, 0f,
                        size, size, gm.size(), gm.size(), gm.size(), gm.size(), argb(a, 0xFFFFFF));
                g.pose().popMatrix();
            }
        }
    }

    /** Blue glow behind the icon: a wide soft halo plus a tighter core, pulsing out of phase. */
    private static void drawGlow(GuiGraphics g, int cx, int cy, int iconSize, long elapsed, long now, float alpha) {
        float p = easeOutCubic(clamp01((elapsed - ICON_DELAY_MS) / (float) ICON_POP_MS));
        if (p <= 0f) return;
        float pulse = (float) Math.sin(now / 700.0);
        drawCentered(g, GLOW, cx, cy, iconSize * 3.4f * p * (1f + 0.06f * pulse), alpha * 0.55f);
        drawCentered(g, GLOW, cx, cy, iconSize * 1.7f * p * (1f - 0.05f * pulse), alpha * (0.30f + 0.10f * pulse));
    }

    /** The big center icon (a game's, or the Crown crown): easeOutBack pop, then a gentle bob + sway. */
    private static void drawIcon(GuiGraphics g, Identifier texture, int texSize, int cx, int cy, int size,
                                 long elapsed, long now, float alpha) {
        float p = clamp01((elapsed - ICON_DELAY_MS) / (float) ICON_POP_MS);
        if (p <= 0f) return;
        float pop = easeOutBack(p);
        float bob = (float) Math.sin(now / 900.0) * 3f;
        float sway = (float) Math.sin(now / 1300.0) * 3f;
        g.pose().pushMatrix();
        g.pose().translate(cx, cy + bob);
        g.pose().rotate((float) Math.toRadians(sway));
        g.pose().scale(pop, pop);
        g.blit(RenderPipelines.GUI_TEXTURED, texture, -size / 2, -size / 2, 0f, 0f,
                size, size, texSize, texSize, texSize, texSize, argb(alpha * smooth(p), 0xFFFFFF));
        g.pose().popMatrix();
    }

    private static void drawTitle(GuiGraphics g, Font font, int cx, int y, int w, long elapsed, float alpha) {
        if (title.isEmpty()) return;
        float p = clamp01((elapsed - TITLE_DELAY_MS) / (float) TITLE_POP_MS);
        if (p <= 0f) return;
        float scale = 2f;
        int textW = font.width(title);
        if (textW * scale > w - 80) {
            scale = Math.max(1f, (w - 80) / (float) textW);
        }
        float pop = 0.85f + 0.15f * easeOutBack(p);
        g.pose().pushMatrix();
        g.pose().translate(cx, y + (1f - smooth(p)) * 8f);   // slides up as it fades in
        g.pose().scale(scale * pop, scale * pop);
        g.drawCenteredString(font, title, 0, -font.lineHeight / 2, argb(alpha * smooth(p), 0xFFFFFF));
        g.pose().popMatrix();
    }

    private static void drawSubtitle(GuiGraphics g, Font font, int cx, int y, long elapsed, float alpha) {
        if (subtitle.isEmpty()) return;
        float p = clamp01((elapsed - SUB_DELAY_MS) / (float) SUB_FADE_MS);
        if (p <= 0f) return;
        g.drawCenteredString(font, subtitle, cx, y, argb(alpha * smooth(p), SUBTITLE_GRAY));
    }

    /** Thin rounded yellow progress line on a dark track, with a soft glow and a shimmer sweep. */
    private static void drawProgress(GuiGraphics g, Font font, int cx, int w, int y, long elapsed, long now, float alpha) {
        // A thin line rather than a chunky bar: 3 gui px tall (6 rows at the pill's half-pixel
        // resolution, so the caps still read round), a little wider, with a soft glow instead of
        // the old hard blue ring.
        float barW = Math.min(260, w * 0.55f);
        float barH = 3f;
        float x = cx - barW / 2f;
        float by = y + 2.5f;   // keep the line where the old 8px bar's middle was

        pill(g, x - 0.5f, by - 0.5f, barW + 1f, barH + 1f, argb(alpha * 0.22f, BAR_RING_BLUE), argb(alpha * 0.22f, BAR_RING_BLUE),
                Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
        pill(g, x, by, barW, barH, argb(alpha * 0.6f, 0x07080B), argb(alpha * 0.6f, 0x07080B),
                Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);

        float p = clamp01(elapsed / (float) durationMs);
        if (p > 0f) {
            // Grows as a pill (never narrower than its height) so the caps stay round from the start.
            float fillW = barH + easeOutCubic(p) * (barW - barH);
            // Soft yellow glow hugging the fill.
            pill(g, x - 1f, by - 1.5f, fillW + 2f, barH + 3f, argb(alpha * 0.16f, BAR_YELLOW), argb(alpha * 0.16f, BAR_YELLOW),
                    Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
            pill(g, x, by, fillW, barH, argb(alpha, BAR_YELLOW_TOP), argb(alpha, BAR_YELLOW),
                    Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);

            float sweep = (now % SHIMMER_PERIOD_MS) / (float) SHIMMER_PERIOD_MS;
            float band = 30f;
            float bandX = x - band + sweep * (fillW + band);
            pill(g, x, by, fillW, barH, argb(alpha * 0.6f, 0xFFFFFF), argb(alpha * 0.3f, 0xFFFFFF),
                    bandX, bandX + band);
        }

        int dots = (int) ((now / 400) % 4);
        String loading = "LOADING" + ".".repeat(dots) + " ".repeat(3 - dots); // pad so it doesn't recenter
        g.drawCenteredString(font, loading, cx, Math.round(by + barH) + 7, argb(alpha * 0.85f, SUBTITLE_GRAY));
    }

    /**
     * Fills a horizontal pill (fully rounded ends) row by row at half-gui-pixel resolution, so the
     * caps read as round even at gui scale 2. Rows blend topColor -> bottomColor; only the part
     * between clipL and clipR (gui px) is drawn, which lets the shimmer stay inside the rounded ends.
     */
    private static void pill(GuiGraphics g, float x, float y, float w, float h, int topColor, int bottomColor,
                             float clipL, float clipR) {
        int X = Math.round(x * 2), Y = Math.round(y * 2), W = Math.round(w * 2), H = Math.round(h * 2);
        if (W <= 0 || H <= 0) return;
        float r = Math.min(H, W) / 2f;
        int cl = clipL == Float.NEGATIVE_INFINITY ? Integer.MIN_VALUE : Math.round(clipL * 2);
        int cr = clipR == Float.POSITIVE_INFINITY ? Integer.MAX_VALUE : Math.round(clipR * 2);
        g.pose().pushMatrix();
        g.pose().scale(0.5f, 0.5f);
        for (int dy = 0; dy < H; dy++) {
            float dist = Math.abs(dy + 0.5f - H / 2f);
            float inset = dist >= r ? r : r - (float) Math.sqrt(r * r - dist * dist);
            int left = Math.max(X + Math.round(inset), cl);
            int right = Math.min(X + W - Math.round(inset), cr);
            if (right <= left) continue;
            float t = H == 1 ? 0f : dy / (float) (H - 1);
            g.fill(left, Y + dy, right, Y + dy + 1, lerpArgb(topColor, bottomColor, t));
        }
        g.pose().popMatrix();
    }

    private static void drawCentered(GuiGraphics g, Identifier texture, float cx, float cy, float size, float alpha) {
        if (alpha <= 0.01f || size < 1f) return;
        float scale = size / GLOW_TEX;   // float scale so the pulse grows smoothly, not in 1px steps
        g.pose().pushMatrix();
        g.pose().translate(cx, cy);
        g.pose().scale(scale, scale);
        g.blit(RenderPipelines.GUI_TEXTURED, texture, -GLOW_TEX / 2, -GLOW_TEX / 2, 0f, 0f,
                GLOW_TEX, GLOW_TEX, GLOW_TEX, GLOW_TEX, GLOW_TEX, GLOW_TEX, argb(alpha, 0xFFFFFF));
        g.pose().popMatrix();
    }

    // --- Easing / color helpers ---

    private static Identifier tex(String path) {
        return Identifier.fromNamespaceAndPath("crown", "textures/loading/" + path);
    }

    private static float fadeIn(long elapsed) {
        return smooth(clamp01(elapsed / (float) FADE_IN_MS));
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    private static float smooth(float p) {
        return p * p * (3f - 2f * p);
    }

    private static float easeOutCubic(float p) {
        float q = 1f - p;
        return 1f - q * q * q;
    }

    private static float easeOutBack(float p) {
        float c1 = 1.70158f;
        float q = p - 1f;
        return 1f + (c1 + 1f) * q * q * q + c1 * q * q;
    }

    private static int argb(float alpha, int rgb) {
        int a = (int) (clamp01(alpha) * 255f);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    private static int lerpArgb(int from, int to, float t) {
        int a = Math.round(((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t);
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int gr = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (a << 24) | (r << 16) | (gr << 8) | b;
    }
}
