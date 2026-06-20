package com.github.NGoedix.videoplayer.client.gui;

import com.github.NGoedix.videoplayer.Reference;
import com.github.NGoedix.videoplayer.block.entity.custom.TVBlockEntity;
import com.github.NGoedix.videoplayer.client.gui.components.CustomSlider;
import com.github.NGoedix.videoplayer.client.gui.components.ImageButtonHoverable;
import com.github.NGoedix.videoplayer.client.render.VideoTextureManager;
import com.github.NGoedix.videoplayer.network.PacketHandler;
import com.github.NGoedix.videoplayer.util.displayers.VideoDisplayer;
import org.watermedia.api.image.ImageAPI;
import org.watermedia.api.image.ImageRenderer;
import org.watermedia.api.math.MathAPI;
import org.watermedia.api.player.videolan.VideoPlayer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.awt.*;

public class TVVideoScreen extends Screen {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/background.png");

    private static final Identifier PLAY_BUTTON_TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/play_button.png");
    private static final Identifier PLAY_HOVER_BUTTON_TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/play_button_hover.png");

    private static final Identifier PAUSE_BUTTON_TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/pause_button.png");
    private static final Identifier PAUSE_HOVER_BUTTON_TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/pause_button_hover.png");

    private static final Identifier STOP_BUTTON_TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/stop_button.png");
    private static final Identifier STOP_HOVER_BUTTON_TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/stop_button_hover.png");

    private ImageButtonHoverable playButton;
    private ImageButtonHoverable pauseButton;
    private ImageButtonHoverable stopButton;

    private CustomSlider timeSlider;

    private final TVBlockEntity be;
    private String url;
    private int volume;
    private long maxDuration;

    private final int videoWidth = 200;
    private final int videoHeight = 150;

    private final int imageWidth = 256;
    private final int imageHeight = 256;
    private int leftPos;
    private int topPos;

    private EditBox urlBox;
    private CustomSlider volumeSlider;

    private boolean canClick = true;

    public TVVideoScreen(BlockEntity be) {
        super(Component.translatable("gui.tv_video_screen.title"));
        this.be = (TVBlockEntity) be;
        this.url = this.be.getUrl();
        this.volume = this.be.getVolume();
    }

    @Override
    protected void init() {
        super.init();

        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;

        String urlPattern = "(http|https)://(www\\.)?([\\w]+\\.)+[\\w]{2,63}/?[\\w\\-\\?\\=\\&\\%\\.\\/]*/?";

        addRenderableWidget(urlBox = new EditBox(font, leftPos + 10, topPos + 165, imageWidth - 26, 20, Component.literal("")));
        urlBox.setMaxLength(32767);
        urlBox.setValue(url == null ? "" : url);
        urlBox.setSuggestion(url == null || url.isEmpty() ? "https://youtube.com/watch?v=FUIcBBM5-xQ" : "");
        urlBox.setResponder(s -> {
            if (s != null && !s.isEmpty()) {
                urlBox.setSuggestion("");
                if (s.matches(urlPattern) && (be.getTick() > 5 || url.isEmpty())) {
                    if (!url.equals(s)) {
                        be.setTick(0);
                        url = s;
                        PacketHandler.sendC2SVideoUpdateMessage(be.getBlockPos(), url, volume, 0, true, false, false);
                        playButton.visible = false;
                        pauseButton.visible = true;
                        maxDuration = 0;
                        timeSlider.setValue(0);

                        if (be.requestDisplay() == null) return;
                        be.requestDisplay().stop();
                        be.requestDisplay().resume(0);
                    }
                }
            } else {
                urlBox.setSuggestion("https://youtube.com/watch?v=FUIcBBM5-xQ");
            }
        });

        addRenderableWidget(playButton = new ImageButtonHoverable(leftPos + 10, topPos + 190, 20, 20, 0, 0, 0, PLAY_BUTTON_TEXTURE, PLAY_HOVER_BUTTON_TEXTURE, 20, 20, button -> {
            if (be.requestDisplay() != null && !url.isEmpty() && canClick) {
                canClick = false;
                playButton.visible = false;
                pauseButton.visible = true;

                if (be.requestDisplay() == null) return;
                be.requestDisplay().resume(be.getTick());
                PacketHandler.sendC2SVideoUpdateMessage(be.getBlockPos(), url, volume, be.getTick(), true, false, false);
            }
        }));

        addRenderableWidget(pauseButton = new ImageButtonHoverable(leftPos + 10, topPos + 190, 20, 20, 0, 0, 0, PAUSE_BUTTON_TEXTURE, PAUSE_HOVER_BUTTON_TEXTURE, 20, 20, button -> {
            if (be.requestDisplay() != null && !url.isEmpty() && canClick) {
                canClick = false;
                playButton.visible = true;
                pauseButton.visible = false;

                be.requestDisplay().pause(be.getTick());
                PacketHandler.sendC2SVideoUpdateMessage(be.getBlockPos(), url, volume, be.getTick(), false, false, false);
            }
        }));

        playButton.visible = !be.isPlaying();
        pauseButton.visible = be.isPlaying();

        addRenderableWidget(stopButton = new ImageButtonHoverable(leftPos + 32, topPos + 190, 20, 20, 0, 0, 0, STOP_BUTTON_TEXTURE, STOP_HOVER_BUTTON_TEXTURE, 20, 20, button -> {
            if (be.requestDisplay() != null && !url.isEmpty()) {
                playButton.visible = true;
                pauseButton.visible = false;

                timeSlider.setValue(0);
                if (be.requestDisplay() == null) return;
                be.requestDisplay().stop();
                PacketHandler.sendC2SVideoUpdateMessage(be.getBlockPos(), url, volume, 0, false, true, false);
            }
        }));

        addRenderableWidget(timeSlider = new CustomSlider(leftPos + 54, topPos + 200, 187, 10, null, 0 / 100f, true));
        timeSlider.setOnSlideListener(value -> {
            if (be.requestDisplay() == null) return;
            if (be.requestDisplay() instanceof VideoDisplayer) {
                VideoPlayer player = (VideoPlayer) ((VideoDisplayer) be.requestDisplay()).player;
                if (player.isReady() && !player.isLive()) {
                    player.seekTo((int) ((value / 100D) * player.getDuration()));
                }
                PacketHandler.sendC2SVideoUpdateMessage(be.getBlockPos(), url, volume, MathAPI.msToTick(player.getTime()), pauseButton.visible, false, false);
            }
        });

        if (be.requestDisplay() != null && be.requestDisplay() instanceof VideoDisplayer) {
            VideoPlayer player = (VideoPlayer) ((VideoDisplayer) be.requestDisplay()).player;
            timeSlider.setValue((double) player.getTime() / player.getDuration());
        }

        addRenderableWidget(volumeSlider = new CustomSlider(leftPos + 10, topPos + 215, imageWidth - 24, 20, Component.translatable("gui.tv_video_screen.volume"), volume / 100f, false));
        volumeSlider.setOnSlideListener(value -> {
            be.setVolume((int) value);
            volume = (int) volumeSlider.getValue();
            PacketHandler.sendC2SVideoUpdateMessage(be.getBlockPos(), url, volume, -1, pauseButton.visible, false, false);
        });
        volumeSlider.setValue(volume / 100f);

        be.setVolume(volume);
    }

    @Override
    public void render(GuiGraphics context, int pMouseX, int pMouseY, float pPartialTick) {
        renderBackground(context, pMouseX, pMouseY, pPartialTick);
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0.0F, 0.0F, 320, 320, imageWidth, imageHeight, imageWidth, imageHeight);

        drawTexture(context, ImageAPI.blackPicture().texture(0), leftPos + (imageWidth / 2) - (videoWidth / 2), topPos + 10, videoWidth, videoHeight);

        super.render(context, pMouseX, pMouseY, pPartialTick);

        String maxTimeFormatted = "00:00";
        String actualTimeFormatted = "00:00";

        if (be.requestDisplay() instanceof VideoDisplayer) {
            VideoPlayer player = (VideoPlayer) ((VideoDisplayer) be.requestDisplay()).player;

            if (player != null && player.isReady()) {
                timeSlider.setActive(!player.isLive());

                if (maxDuration == 0 && !player.isLive())
                    maxDuration = player.getDuration();
            }

            if (player != null && player.isReady() && !player.isLive()) {
                long durationSeconds = maxDuration / 1000;
                long maxMinute = durationSeconds / 60;
                long maxSeconds = durationSeconds % 60;

                long actualTime = MathAPI.tickToMs(be.getTick()) / 1000;
                long actualMinute = actualTime / 60;
                long actualSeconds = actualTime % 60;

                if (durationSeconds != 0)
                    timeSlider.setValue((double) actualTime / durationSeconds);

                maxTimeFormatted = String.format("%02d:%02d", maxMinute, maxSeconds);
                if (actualSeconds == -1) actualSeconds = 0;
                actualTimeFormatted = String.format("%02d:%02d", actualMinute, actualSeconds);
            }
        }

        context.drawString(font, Component.translatable("gui.tv_video_screen.time", actualTimeFormatted, maxTimeFormatted), leftPos + 54, topPos + 190, 0xFFFFFF);

        renderVideo(context);

        canClick = true;
    }

    public void renderVideo(GuiGraphics guiGraphics) {
        if (url.isEmpty()) return;

        if (be.requestDisplay() == null) {
            renderIcon(guiGraphics, ImageAPI.loadingGif());
            return;
        }

        boolean playingState = be.requestDisplay().isPlaying();

        if (playingState || be.requestDisplay().isStopped()) {
            if (be.requestDisplay().getDimensions() == null) return;

            int textureId = be.requestDisplay().getRenderTexture();

            if (textureId == -1) return;

            guiGraphics.fill(leftPos + (imageWidth / 2) - (videoWidth / 2), topPos + 10, leftPos + (imageWidth / 2) - (videoWidth / 2) + videoWidth, topPos + 10 + videoHeight, MathAPI.argb(255, 0, 0, 0));

            Dimension videoDimensions = be.requestDisplay().getDimensions();
            double nativeVideoWidth = videoDimensions.getWidth();
            double nativeVideoHeight = videoDimensions.getHeight();

            float screenAspectRatio = (float) videoWidth / videoHeight;
            float videoAspectRatio = (float) (nativeVideoWidth / nativeVideoHeight);

            int renderWidth, renderHeight;

            if (videoAspectRatio > screenAspectRatio) {
                renderWidth = videoWidth;
                renderHeight = (int) (videoWidth / videoAspectRatio);
            } else {
                renderWidth = (int) (videoHeight * videoAspectRatio);
                renderHeight = videoHeight;
            }

            int xOffset = (videoWidth - renderWidth) / 2;
            int yOffset = (videoHeight - renderHeight) / 2;

            drawTexture(guiGraphics, textureId, leftPos + (imageWidth / 2) - (videoWidth / 2) + xOffset, topPos + 10 + yOffset, renderWidth, renderHeight);
        }
    }

    private void renderIcon(GuiGraphics stack, ImageRenderer image) {
        drawTexture(stack, image.texture(be.getTick(), 1, true), leftPos + (imageWidth / 2) - (videoWidth / 2), topPos + 10, videoWidth, videoHeight);
    }

    private void drawTexture(GuiGraphics guiGraphics, int texture, int x, int y, int width, int height) {
        Identifier id = VideoTextureManager.bind(texture, width, height);
        if (id == null) return;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, id, x, y, 0.0F, 0.0F, width, height, width, height);
    }

    @Override
    public void removed() {
        PacketHandler.sendC2SVideoUpdateMessage(be.getBlockPos(), url, volume, -1, pauseButton.visible, false, true);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
