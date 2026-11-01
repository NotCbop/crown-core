package com.github.NGoedix.videoplayer.loading;

import com.github.NGoedix.videoplayer.Reference;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.jcodec.api.FrameGrab;
import org.jcodec.common.io.ByteBufferSeekableByteChannel;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rect;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Plays one bundled loading clip, looping, with no native libraries: a daemon thread decodes the
 * H.264 mp4 with JCodec (pure Java) at {@link #FPS} and publishes each frame's luma as gray pixels;
 * the render thread uploads the newest one into a {@link DynamicTexture} when asked for it.
 *
 * <p>Clips are grayscale on purpose (the loading screen shows them gray), so only the Y plane is
 * read — no colour conversion. They must be H.264 <b>baseline</b> at 30 fps; the encode script
 * ({@code tools/encode-loading-clips.ps1}) produces exactly that. Decoding a 640x360 frame takes
 * ~3-4 ms, far inside the 33 ms budget.</p>
 */
@Environment(EnvType.CLIENT)
final class ClipPlayer {

    static final int FPS = 30;
    private static final long FRAME_NANOS = 1_000_000_000L / FPS;
    private static final AtomicInteger IDS = new AtomicInteger();

    private final Identifier id = Identifier.fromNamespaceAndPath("crown", "loading_clip/" + IDS.incrementAndGet());
    private final Resource source;
    private final String name;
    private final Thread thread;
    private volatile boolean stopped;
    private volatile boolean failed;

    // Decoder -> render thread hand-off (guarded by `this`).
    private int[] ready;
    private boolean fresh;
    private int width;
    private int height;

    // Render thread only.
    private ClipTexture texture;

    ClipPlayer(Identifier name, Resource source) {
        this.name = name.toString();
        this.source = source;
        this.thread = new Thread(this::run, "CrownCore-Clip-" + name.getPath());
        this.thread.setDaemon(true);
        this.thread.setPriority(Thread.NORM_PRIORITY - 1);
        this.thread.start();
    }

    /** True once the decoder gave up (bad file, unsupported encoding). */
    boolean failed() {
        return failed;
    }

    int width() {
        return width;
    }

    int height() {
        return height;
    }

    /**
     * Uploads the newest decoded frame (if there's a new one) and returns the texture to draw, or
     * null while no frame has been decoded yet. Render thread only.
     */
    Identifier frame() {
        if (stopped) return null;
        synchronized (this) {
            if (ready == null) return null;
            if (texture == null || texture.w != width || texture.h != height) {
                if (texture != null) Minecraft.getInstance().getTextureManager().release(id);
                texture = new ClipTexture(name, width, height);
                Minecraft.getInstance().getTextureManager().register(id, texture);
                fresh = true;
            }
            if (fresh) {
                long ptr = texture.getPixels().getPointer();
                MemoryUtil.memIntBuffer(ptr, width * height).put(0, ready, 0, width * height);
                fresh = false;
            } else {
                return id;
            }
        }
        texture.upload();
        return id;
    }

    /** Stops the decoder and frees the texture. Render thread only. */
    void release() {
        stopped = true;
        thread.interrupt();
        if (texture != null) {
            Minecraft.getInstance().getTextureManager().release(id);
            texture = null;
        }
    }

    // --- Decoder thread ---

    private void run() {
        try {
            byte[] data;
            try (InputStream in = source.open()) {
                data = in.readAllBytes();
            }
            int[] back = null;
            long next = System.nanoTime();
            while (!stopped) {
                FrameGrab grab = FrameGrab.createFrameGrab(
                        ByteBufferSeekableByteChannel.readFromByteBuffer(ByteBuffer.wrap(data)));
                int frames = 0;
                Picture pic;
                while (!stopped && (pic = grab.getNativeFrame()) != null) {
                    frames++;
                    back = toGray(pic, back);
                    publish(back, pic);
                    back = swapped;

                    next += FRAME_NANOS;
                    long wait = next - System.nanoTime();
                    if (wait > 0) {
                        Thread.sleep(wait / 1_000_000L, (int) (wait % 1_000_000L));
                    } else if (wait < -FRAME_NANOS * 5) {
                        next = System.nanoTime();   // fell way behind (hitch) — don't sprint to catch up
                    }
                }
                if (frames == 0) throw new IllegalStateException("no frames");
                // End of clip: loop by reopening it (cheap — it's all in memory).
            }
        } catch (InterruptedException e) {
            // released
        } catch (Throwable t) {
            if (!stopped) {
                failed = true;
                Reference.LOGGER.warn("Loading clip {} can't be played: {}", name, t.toString());
            }
        }
    }

    private int[] swapped;   // decoder thread only: the buffer handed back by publish()

    /** Swaps the decoded buffer in as the ready frame; the previous ready buffer comes back for reuse. */
    private void publish(int[] frame, Picture pic) {
        synchronized (this) {
            swapped = ready;
            ready = frame;
            fresh = true;
            width = cropW(pic);
            height = cropH(pic);
        }
    }

    /** Y plane -> opaque gray pixels (R = G = B, so ABGR vs ARGB byte order doesn't matter). */
    private static int[] toGray(Picture pic, int[] out) {
        int w = cropW(pic);
        int h = cropH(pic);
        Rect crop = pic.getCrop();
        int x0 = crop == null ? 0 : crop.getX();
        int y0 = crop == null ? 0 : crop.getY();
        int stride = pic.getPlaneWidth(0);
        byte[] y = pic.getPlaneData(0);
        if (out == null || out.length != w * h) out = new int[w * h];
        int i = 0;
        for (int row = 0; row < h; row++) {
            int src = (row + y0) * stride + x0;
            for (int col = 0; col < w; col++) {
                int v = y[src + col] + 128;   // JCodec stores samples as signed (value - 128)
                out[i++] = 0xFF000000 | (v << 16) | (v << 8) | v;
            }
        }
        return out;
    }

    private static int cropW(Picture pic) {
        return pic.getCrop() == null ? pic.getWidth() : pic.getCrop().getWidth();
    }

    private static int cropH(Picture pic) {
        return pic.getCrop() == null ? pic.getHeight() : pic.getCrop().getHeight();
    }

    /** A DynamicTexture with linear filtering (640x360 is stretched across the whole screen). */
    private static final class ClipTexture extends DynamicTexture {
        final int w;
        final int h;

        ClipTexture(String label, int w, int h) {
            super(label, w, h, false);
            this.w = w;
            this.h = h;
            this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        }
    }
}
