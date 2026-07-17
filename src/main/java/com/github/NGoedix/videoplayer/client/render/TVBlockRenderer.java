package com.github.NGoedix.videoplayer.client.render;

import com.github.NGoedix.videoplayer.block.custom.TVBlock;
import com.github.NGoedix.videoplayer.block.entity.custom.TVBlockEntity;
import com.github.NGoedix.videoplayer.util.displayers.IDisplay;
import com.github.NGoedix.videoplayer.util.math.geo.*;
import com.mojang.blaze3d.vertex.PoseStack;
import org.watermedia.api.image.ImageAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class TVBlockRenderer implements BlockEntityRenderer<TVBlockEntity, TVBlockRenderer.TVRenderState> {

    public TVBlockRenderer(BlockEntityRendererProvider.Context context) {}

    public static class TVRenderState extends BlockEntityRenderState {
        public TVBlockEntity be;
    }

    @Override
    public TVRenderState createRenderState() {
        return new TVRenderState();
    }

    @Override
    public void extractRenderState(TVBlockEntity be, TVRenderState state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, overlay);
        state.be = be;
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public boolean shouldRender(TVBlockEntity frame, @NotNull Vec3 vec) {
        return Vec3.atCenterOf(frame.getBlockPos()).closerThan(vec, 128);
    }

    @Override
    public void submit(TVRenderState state, @NotNull PoseStack pose, @NotNull SubmitNodeCollector collector, @NotNull CameraRenderState camera) {
        TVBlockEntity frame = state.be;
        if (frame == null) return;

        if (frame.isURLEmpty()) {
            if (frame.display != null) frame.display.release();
            return;
        }

        IDisplay display = frame.requestDisplay();
        if (display == null) {
            if (!frame.isPlaying()) return;
            renderTexture(frame, null, ImageAPI.loadingGif().texture((int) (Minecraft.getInstance().level.getGameTime()), 1, true), pose, collector, true);
            return;
        }

        int texture = display.prepare(frame.getUrl(), frame.isPlaying(), true, frame.getTick());
        if (texture == -1) return;

        renderTexture(frame, display, ImageAPI.blackPicture().texture(1, 1, false), pose, collector, false);
        renderTexture(frame, display, texture, pose, collector, true);
    }

    private void renderTexture(TVBlockEntity frame, IDisplay display, int texture, PoseStack pose, SubmitNodeCollector collector, boolean aspectRatio) {
        int width = 16, height = 16;
        if (display != null) {
            Dimension dim = display.getDimensions();
            if (dim != null) {
                width = (int) dim.getWidth();
                height = (int) dim.getHeight();
            }
        }
        Identifier textureId = VideoTextureManager.bind(texture, width, height);
        if (textureId == null) return;

        Direction d = frame.getBlockState().getValue(TVBlock.FACING);
        if (d == Direction.NORTH) {
            d = Direction.SOUTH;
        } else if (d == Direction.SOUTH) {
            d = Direction.NORTH;
        }

        Facing facing = Facing.get(d);
        AlignedBox box = frame.getBox();

        // BEGIN ASPECT RATIO
        if (aspectRatio) {
            float videoAspectRatio = 1.0f;
            if (display != null) {
                Dimension dimensions = display.getDimensions();
                if (dimensions != null) {
                    videoAspectRatio = (float) (dimensions.getWidth() / (float) dimensions.getHeight());
                }
            }

            float h0 = box.maxY - box.minY;
            float w0 = 0F;
            switch (facing) {
                case WEST, EAST -> w0 = box.maxZ - box.minZ;
                case NORTH, SOUTH -> w0 = box.maxX - box.minX;
            }

            float screenAspectRatio = w0 / h0;
            float w = h0 * videoAspectRatio;
            float h = w0 / videoAspectRatio;

            if (videoAspectRatio > screenAspectRatio) {
                box.setMax(Axis.Y, h);
                pose.translate(0, (h0 - h) / 2F, 0);
            } else {
                box.setMax(facing.axis == Axis.Z ? Axis.X : Axis.Z, w);
                pose.translate(facing.axis == Axis.Z ? (w0 - w) / 2F : 0, 0, facing.axis == Axis.Z ? 0 : (w0 - w) / 2F);
            }

            if (facing == Facing.SOUTH) {
                box.setMax(Axis.X, box.maxX - 0.02F);
            }

            float difference = h0 - w0;
            if (difference > 0) {
                box.grow(Axis.Y, -difference / 2);
                if (facing.axis == Axis.Z) {
                    box.grow(Axis.X, difference / 2);
                } else {
                    box.grow(Axis.Z, difference / 2);
                }
            }
        }
        // END ASPECT RATIO

        float offset = aspectRatio ? 0.001f : 0;

        if (d == Direction.WEST || d == Direction.EAST) {
            box.grow(facing.axis, 0.99F + offset);
        } else {
            box.grow(facing.axis, -0.95F + offset);
        }
        BoxFace face = BoxFace.get(facing);

        pose.pushPose();

        if (d == Direction.NORTH) {
            pose.translate(-0.200, 0, 0);
        }
        if (d == Direction.SOUTH) {
            pose.translate(-0.191, 0, 0);
        }
        if (d == Direction.WEST) {
            pose.translate(0, 0, -0.200);
        }
        if (d == Direction.EAST) {
            pose.translate(0, 0, -0.200);
        }

        pose.translate(0.5, 0.5356, 0.5);
        pose.mulPose(facing.rotation().rotation((float) Math.toRadians(0)));
        pose.translate(-0.5, -0.5, -0.5);

        final AlignedBox finalBox = box;
        final BoxFace finalFace = face;
        final Vec3i normal = face.facing.normal;

        collector.submitCustomGeometry(pose, RenderTypes.entitySolid(textureId), (poseEntry, builder) -> {
            for (BoxCorner corner : finalFace.corners) {
                builder.addVertex(poseEntry, finalBox.get(corner.x), finalBox.get(corner.y), finalBox.get(corner.z))
                        .setColor(-1)
                        .setUv(corner.isFacing(finalFace.getTexU()) ? 1 : 0, corner.isFacing(finalFace.getTexV()) ? 1 : 0)
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(LightTexture.FULL_BRIGHT)
                        .setNormal(poseEntry, normal.getX(), normal.getY(), normal.getZ());
            }
        });

        pose.popPose();
    }
}
