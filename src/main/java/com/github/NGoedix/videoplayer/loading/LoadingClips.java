package com.github.NGoedix.videoplayer.loading;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The loading screen's video backdrop: cycles a game's bundled clips (shuffled, looping) behind
 * the overlay. Each clip holds for {@link #HOLD_MS}, then crossfades into the next over
 * {@link #CROSSFADE_MS}; at most two {@link ClipPlayer}s are alive at once.
 *
 * <p>Clips live in the jar under {@code assets/crown/loading_clips/<game>/*.mp4} (grayscale,
 * made by {@code tools/encode-loading-clips.ps1}). A game with no folder of its own plays the
 * {@code hub} set. They come through the resource manager, so a resource pack can add or replace
 * clips too. Render thread only.</p>
 */
@Environment(EnvType.CLIENT)
final class LoadingClips {

    private static final long HOLD_MS = 5000L;
    private static final long CROSSFADE_MS = 1200L;
    private static final long LEAD_MS = 600L;          // start the next decoder a bit before the fade
    private static final long FIRST_FADE_MS = 600L;
    private static final long GIVE_UP_MS = 4000L;      // a clip with no frames by then is skipped

    /** Grayscale tint: a touch of blue so it sits with the rest of the screen. */
    private static final int TINT = 0xB4BAC6;

    private record Clip(Identifier id, Resource resource) {}

    private static final class Slot {
        final ClipPlayer player;
        final long createdAt;
        long readyAt;        // first frame seen; 0 = still decoding

        Slot(ClipPlayer player, long createdAt) {
            this.player = player;
            this.createdAt = createdAt;
        }
    }

    private static final List<Clip> clips = new ArrayList<>();
    private static int nextIndex;
    private static Slot current;
    private static Slot next;
    private static long shownAt;          // when the current clip became the visible one
    private static long crossStart;       // 0 = not crossfading
    private static long firstShownAt;     // fade-in of the very first clip

    private LoadingClips() {}

    /** Starts cycling {@code folder}'s clips (falling back to {@code hub}); false when there are none. */
    static boolean start(String folder) {
        stop();
        List<Clip> found = find(folder);
        if (found.isEmpty() && !"hub".equals(folder)) found = find("hub");
        if (found.isEmpty()) return false;
        clips.addAll(found);
        Collections.shuffle(clips);
        nextIndex = 0;
        return true;
    }

    private static List<Clip> find(String folder) {
        Map<Identifier, Resource> map = Minecraft.getInstance().getResourceManager().listResources(
                "loading_clips/" + folder,
                id -> id.getNamespace().equals("crown") && id.getPath().endsWith(".mp4"));
        List<Clip> out = new ArrayList<>();
        map.forEach((id, res) -> out.add(new Clip(id, res)));
        return out;
    }

    static void stop() {
        release(current);
        release(next);
        current = null;
        next = null;
        clips.clear();
        crossStart = 0L;
        shownAt = 0L;
        firstShownAt = 0L;
    }

    /** Draws the clip(s) cover-fit across the screen; returns false while nothing is visible yet. */
    static boolean render(GuiGraphics g, int w, int h, float alpha, long now) {
        if (clips.isEmpty()) return false;

        if (current == null) {
            current = spawn(now);
            if (current == null) return false;
        }
        if (!ensureReady(current, now)) {
            if (current.player.failed() || now - current.createdAt > GIVE_UP_MS) {
                // Never produced a frame — move on to the next clip.
                release(current);
                current = clips.size() > 1 ? spawn(now) : null;
                if (current == null) clips.clear();
            }
            return false;
        }
        if (shownAt == 0L) shownAt = now;
        if (firstShownAt == 0L) firstShownAt = now;

        // Queue the next clip just ahead of time so it has frames by the time the hold ends.
        if (next == null && clips.size() > 1 && now - shownAt >= HOLD_MS - LEAD_MS) {
            next = spawn(now);
        }
        if (next != null) {
            boolean ok = ensureReady(next, now);
            if (!ok && (next.player.failed() || now - next.createdAt > GIVE_UP_MS)) {
                release(next);
                next = null;   // try the following clip on the next frame
            } else if (ok && crossStart == 0L && now - shownAt >= HOLD_MS) {
                crossStart = now;
            }
        }

        float first = Math.min(1f, (now - firstShownAt) / (float) FIRST_FADE_MS);
        Identifier curTex = current.player.frame();
        draw(g, curTex, current.player, w, h, alpha * first);

        if (crossStart != 0L && next != null) {
            float t = Math.min(1f, (now - crossStart) / (float) CROSSFADE_MS);
            draw(g, next.player.frame(), next.player, w, h, alpha * t);
            if (t >= 1f) {
                release(current);
                current = next;
                next = null;
                shownAt = crossStart;
                crossStart = 0L;
            }
        }
        return true;
    }

    private static Slot spawn(long now) {
        if (clips.isEmpty()) return null;
        Clip clip = clips.get(nextIndex % clips.size());
        nextIndex++;
        return new Slot(new ClipPlayer(clip.id(), clip.resource()), now);
    }

    private static boolean ensureReady(Slot slot, long now) {
        if (slot.readyAt == 0L && slot.player.frame() != null) slot.readyAt = now;
        return slot.readyAt != 0L;
    }

    private static void draw(GuiGraphics g, Identifier tex, ClipPlayer player, int w, int h, float alpha) {
        if (tex == null || alpha <= 0.01f) return;
        int vw = player.width();
        int vh = player.height();
        if (vw <= 0 || vh <= 0) return;

        // Cover-fit (crop, never letterbox), positioned with a float transform so it doesn't snap.
        float scale = Math.max(w / (float) vw, h / (float) vh);
        g.pose().pushMatrix();
        g.pose().translate(w / 2f, h / 2f);
        g.pose().scale(scale, scale);
        int a = (int) (Math.max(0f, Math.min(1f, alpha)) * 255f);
        g.blit(RenderPipelines.GUI_TEXTURED, tex, -vw / 2, -vh / 2, 0f, 0f, vw, vh, vw, vh, vw, vh, (a << 24) | TINT);
        g.pose().popMatrix();
    }

    private static void release(Slot slot) {
        if (slot != null) slot.player.release();
    }
}
