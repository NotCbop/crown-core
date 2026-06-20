package com.github.NGoedix.videoplayer.client.gui;

import com.github.NGoedix.videoplayer.Reference;
import com.github.NGoedix.videoplayer.client.ClientHandler;
import com.github.NGoedix.videoplayer.client.render.VideoTextureManager;
import com.github.NGoedix.videoplayer.util.YoutubeResolver;
import com.github.NGoedix.videoplayer.util.math.VideoMathUtil;
import org.watermedia.api.image.ImageAPI;
import org.watermedia.api.image.ImageRenderer;
import org.watermedia.api.math.MathAPI;
import org.watermedia.api.network.NetworkAPI;
import org.watermedia.api.player.videolan.VideoPlayer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;

public class VideoScreen extends AbstractContainerScreen<AbstractContainerMenu> {

    private static final DateFormat FORMAT = new SimpleDateFormat("HH:mm:ss");
    static {
        FORMAT.setTimeZone(TimeZone.getTimeZone("GMT-00:00"));
    }

    private int tick = 0;
    private int closingOnTick = -1;
    private float fadeLevel = 0;
    private float fadeStep10 = 0;
    private float fadeStep5 = 0;
    private boolean started;
    private boolean closing = false;
    private float volume;

    private final boolean controlBlocked;
    private final boolean canSkip;
    private int optionInMode;
    private int optionInSecs;
    private int optionOutMode;
    private int optionOutSecs;

    private final VideoPlayer player;

    int videoTexture = -1;

    public VideoScreen(String url, int volume, boolean controlBlocked, boolean canSkip, int optionInMode, int optionInSecs, int optionOutMode, int optionOutSecs) {
        this(url, volume, controlBlocked, canSkip, optionInMode != -1 && optionInSecs > 0);
        this.optionInMode = optionInMode;
        this.optionInSecs = optionInSecs;
        this.optionOutMode = optionOutMode;
        this.optionOutSecs = optionOutSecs;
    }

    public VideoScreen(String url, int volume, boolean controlBlocked, boolean canSkip, boolean fadeIn) {
        super(new DummyContainer(), Objects.requireNonNull(Minecraft.getInstance().player).getInventory(), Component.literal(""));

        Minecraft minecraft = Minecraft.getInstance();
        Minecraft.getInstance().getSoundManager().pauseAllExcept();

        this.volume = volume;
        this.controlBlocked = controlBlocked;
        this.canSkip = canSkip;
        this.optionInMode = -1;
        this.optionInSecs = -1;
        this.optionOutMode = -1;
        this.optionOutSecs = -1;

        this.player = new VideoPlayer(null, minecraft);
        Reference.LOGGER.info("Playing video (" + (!controlBlocked ? "not" : "") + "blocked) (" + url + " with volume: " + (int) (Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER) * volume));

        player.setVolume((int) (Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER) * volume));
        if (!fadeIn) {
            started = true;
            YoutubeResolver.resolve(url, player::start);
        } else {
            YoutubeResolver.resolve(url, player::startPaused);
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics pPoseguiGraphics, int pMouseX, int pMouseY) {}

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        if (started && !closing) {
            videoTexture = player.texture();
        }

        if ((tick < optionInSecs * 20 && optionInMode != -1) || !started) {
            float t = tick / (float) (optionInSecs * 20);
            fadeLevel = (float) applyEasing(optionInMode, 0, 1, t);
            if (!started && fadeLevel >= 1.0) {
                player.play();
                started = true;
                fadeLevel = 0;
            }
        }

        if (closing || player.isEnded() || player.isBroken()) {
            if (optionOutMode == -1) {
                onClose();
            }
            if (optionInMode != -1 || closing) {
                closing = true;
                if (closingOnTick == -1) closingOnTick = tick + optionOutSecs * 20;
                float t = (tick - closingOnTick + optionOutSecs * 20) / (float) (optionOutSecs * 20);
                fadeLevel = (float) applyEasing(optionOutMode, 1, 0, t);
                renderBlackBackground(guiGraphics);
                if (fadeLevel == 0) onClose();
                return;
            }
        }

        if (!player.isPaused() || optionInMode != -1 || optionOutMode != -1)
            renderBlackBackground(guiGraphics);

        if (!started) return;

        boolean playingState = (player.isPlaying() || player.isPaused());

        if (playingState || player.isStopped() || player.isEnded()) {
            renderTexture(guiGraphics, videoTexture);
        }

        if (!player.isPaused())
            renderBlackBackground(guiGraphics);

        if (!player.isPlaying()) {
            if (player.isPaused()) {
                renderIcon(guiGraphics, ClientHandler.pausedImage());
            } else {
                renderIcon(guiGraphics, ImageAPI.loadingGif());
            }
        }

        renderStepIcon(guiGraphics, pPartialTick, true);
        renderStepIcon(guiGraphics, pPartialTick, false);

        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            draw(guiGraphics, String.format("State: %s", player.getStateName()), getHeightCenter(-12));
            draw(guiGraphics, String.format("Time: %s (%s) / %s (%s)", FORMAT.format(new Date(player.getTime())), player.getTime(), FORMAT.format(new Date(player.getDuration())), player.getDuration()), getHeightCenter(0));
            draw(guiGraphics, String.format("Media Duration: %s (%s)", FORMAT.format(new Date(player.getMediaInfoDuration())), player.getMediaInfoDuration()), getHeightCenter(12));
        }
    }

    private void renderTexture(GuiGraphics guiGraphics, int texture) {
        Dimension videoDimensions = player.dimension();
        if (videoDimensions == null) return;

        drawTexture(guiGraphics, texture, (int) videoDimensions.getWidth(), (int) videoDimensions.getHeight(), 0, 0, width, height, 0xFFFFFFFF);
    }

    private void renderBlackBackground(GuiGraphics guiGraphics) {
        guiGraphics.fill(0, 0, width, height, MathAPI.argb((int) (fadeLevel * 255), 0, 0, 0));
    }

    private int getHeightCenter(int offset) {
        return (height / 2) + offset;
    }

    private void renderIcon(GuiGraphics guiGraphics, ImageRenderer image) {
        int iconSize = 36;
        int xOffset = width - iconSize;
        int yOffset = height - iconSize;

        drawTexture(guiGraphics, image.texture(tick, 1, true), image.width, image.height, xOffset, yOffset, iconSize, iconSize, 0xFFFFFFFF);
    }

    private void renderStepIcon(GuiGraphics stack, float pPartialTicks, boolean forward) {
        float alpha = forward ? fadeStep10 : fadeStep5;
        int color = ((int) (alpha * 255) << 24) | 0xFFFFFF;
        ImageRenderer image = forward ? ClientHandler.step10Image() : ClientHandler.step5Image();
        drawTexture(stack, image.texture(0), image.width, image.height, width / 2 + (forward ? 70 : -134), height / 2 - 32, 64, 64, color);
        if (forward) {
            fadeStep10 = Math.max(fadeStep10 - (pPartialTicks / 8), 0.0f);
        } else {
            fadeStep5 = Math.max(fadeStep5 - (pPartialTicks / 8), 0.0f);
        }
    }

    private void drawTexture(GuiGraphics guiGraphics, int texture, int texWidth, int texHeight, int x, int y, int width, int height, int color) {
        Identifier id = VideoTextureManager.bind(texture, texWidth, texHeight);
        if (id == null) return;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, id, x, y, 0.0F, 0.0F, width, height, width, height, color);
    }

    private double applyEasing(int mode, double start, double end, double t) {
        return switch (mode) {
            case 0 -> VideoMathUtil.easeIn(start, end, t);
            case 1 -> VideoMathUtil.easeOut(start, end, t);
            default -> end;
        };
    }

    private void draw(GuiGraphics guiGraphics, String text, int height) {
        guiGraphics.drawString(Minecraft.getInstance().font, text, 5, height, 0xffffff);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int pKeyCode = event.key();
        boolean shift = (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;

        if (Minecraft.getInstance().options.keyInventory.matches(event)) {
            return true;
        }

        if (canSkip && shift && pKeyCode == 256) {
            this.onClose();
        }

        if (pKeyCode == 265) {
            if (volume <= 120) {
                volume += 5;
            } else {
                volume = 125;
                float masterVolume = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER);
                Minecraft.getInstance().options.getSoundSourceOptionInstance(SoundSource.MASTER).set((double) (masterVolume <= 0.95 ? masterVolume + 0.1F : 1.0F));
            }

            float actualVolume = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER);
            float newVolume = volume * actualVolume;
            Reference.LOGGER.info("Volume UP to: " + newVolume);
            player.setVolume((int) newVolume);
        }

        if (pKeyCode == 264) {
            if (volume >= 5) {
                volume -= 5;
            } else {
                volume = 0;
            }
            float actualVolume = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER);
            float newVolume = volume * actualVolume;
            Reference.LOGGER.info("Volume DOWN to: " + newVolume);
            player.setVolume((int) newVolume);
        }

        if (pKeyCode == 77) {
            if (player.isMuted()) {
                player.unmute();
            } else {
                player.mute();
            }
        }

        if (controlBlocked) return super.keyPressed(event);

        if (shift && pKeyCode == 262) {
            player.seekTo(player.getTime() + 10000);
            fadeStep10 = 1;
        }

        if (shift && pKeyCode == 263) {
            player.seekTo(player.getTime() - 5000);
            fadeStep5 = 1;
        }

        if (shift && pKeyCode == 32) {
            player.togglePlayback();
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
        super.onClose();
        if (started) {
            started = false;
            player.stop();
            Minecraft.getInstance().getSoundManager().resume();
            player.release();
        }
    }

    @Override
    protected void init() {
        if (Minecraft.getInstance().screen != null) {
            this.imageWidth = Minecraft.getInstance().screen.width;
            this.imageHeight = Minecraft.getInstance().screen.height;
        }
        super.init();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        tick++;
    }
}
