package com.github.NGoedix.videoplayer.client.render;

import com.github.NGoedix.videoplayer.Reference;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Bridges a raw OpenGL texture id (owned by WaterMedia's video/image decoders) into Minecraft's
 * 1.21.5+ GpuTexture render pipeline by registering it as an {@link AbstractTexture} under a dynamic
 * {@link Identifier}, which can then be drawn through {@code GuiGraphics.blit} or a {@code RenderType}.
 */
public final class VideoTextureManager {

    private static final Map<Integer, Identifier> CACHE = new HashMap<>();

    private VideoTextureManager() {}

    public static Identifier bind(int glId, int width, int height) {
        if (glId <= 0) return null;
        Identifier id = CACHE.get(glId);
        if (id == null) {
            id = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "video_texture_" + glId);
            Minecraft.getInstance().getTextureManager().register(id, new WrappedTexture(glId, Math.max(1, width), Math.max(1, height)));
            CACHE.put(glId, id);
        }
        return id;
    }

    private static final class WrappedTexture extends AbstractTexture {
        WrappedTexture(int glId, int width, int height) {
            this.texture = new ExternalGlTexture(glId, width, height);
            this.textureView = RenderSystem.getDevice().createTextureView(this.texture);
        }

        @Override
        public void close() {
            if (this.textureView != null) {
                this.textureView.close();
                this.textureView = null;
            }
            // The underlying GL texture is owned by WaterMedia; do not delete it here.
        }
    }

    private static final class ExternalGlTexture extends GlTexture {
        ExternalGlTexture(int glId, int width, int height) {
            super(GpuTexture.USAGE_TEXTURE_BINDING, "videoplayer_external_" + glId, TextureFormat.RGBA8, width, height, 1, 1, glId);
        }

        @Override
        public void close() {
            // WaterMedia owns and frees this GL texture; avoid double-free.
        }
    }
}
