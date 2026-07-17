package com.github.NGoedix.videoplayer.client;

import com.github.NGoedix.videoplayer.VideoPlayerUtils;
import com.github.NGoedix.videoplayer.block.entity.ModBlockEntities;
import com.github.NGoedix.videoplayer.block.entity.custom.RadioBlockEntity;
import com.github.NGoedix.videoplayer.block.entity.custom.TVBlockEntity;
import com.github.NGoedix.videoplayer.client.gui.RadioScreen;
import com.github.NGoedix.videoplayer.client.gui.TVVideoScreen;
import com.github.NGoedix.videoplayer.client.gui.VideoScreen;
import com.github.NGoedix.videoplayer.client.render.TVBlockRenderer;
import com.github.NGoedix.videoplayer.killfeed.KillFeed;
import com.github.NGoedix.videoplayer.killfeed.KillFeedAssets;
import com.github.NGoedix.videoplayer.killfeed.KillFeedChatListener;
import com.github.NGoedix.videoplayer.killfeed.KillFeedCommand;
import com.github.NGoedix.videoplayer.packlog.PackFailureReporter;
import com.github.NGoedix.videoplayer.network.PacketHandler;
import com.github.NGoedix.videoplayer.Reference;
import com.github.NGoedix.videoplayer.util.RadioStreams;
import org.watermedia.api.image.ImageAPI;
import org.watermedia.api.image.ImageRenderer;
import org.watermedia.api.network.NetworkAPI;
import org.watermedia.api.player.videolan.MusicPlayer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class ClientHandler implements ClientModInitializer {

    @Environment(EnvType.CLIENT)
    private static ImageRenderer IMG_PAUSED;

    @Environment(EnvType.CLIENT)
    private static ImageRenderer IMG_STEP10;

    @Environment(EnvType.CLIENT)
    private static ImageRenderer IMG_STEP5;

    @Environment(EnvType.CLIENT)
    public static ImageRenderer pausedImage() { return IMG_PAUSED; }

    @Environment(EnvType.CLIENT)
    public static ImageRenderer step10Image() { return IMG_STEP10; }

    @Environment(EnvType.CLIENT)
    public static ImageRenderer step5Image() { return IMG_STEP5; }

    private static final List<MusicPlayer> musicPlayers = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        Reference.LOGGER.info("Initializing Client");

        if (VideoPlayerUtils.isInstalled("mr_stellarity", "stellarity")) {
            throw new VideoPlayerUtils.UnsupportedModException("mr_stellarity (Stellarity)", "breaks picture rendering, overwrites Minecraft core shaders and isn't possible work around that");
        }

        RadioStreams.prepareRadios();

        PacketHandler.registerS2CPackets();

        // Kill feed (Crown Championship Utilities) — works with or without WaterMedia.
        KillFeedAssets.register();
        KillFeed.register();
        KillFeedChatListener.register();
        KillFeedCommand.register();

        // Resource-pack failure -> mclo.gs link in chat
        PackFailureReporter.register();

        // Video/radio playback needs WaterMedia, which is an optional dependency. Only wire those parts
        // up when it's installed; everything above keeps working without it.
        if (VideoPlayerUtils.hasWaterMedia()) {
            BlockEntityRendererRegistry.register(ModBlockEntities.TV_BLOCK_ENTITY, TVBlockRenderer::new);
            IMG_PAUSED = ImageAPI.renderer("/pictures/paused.png", ClientHandler.class.getClassLoader(), true);
            IMG_STEP10 = ImageAPI.renderer("/pictures/step10.png", ClientHandler.class.getClassLoader(), true);
            IMG_STEP5 = ImageAPI.renderer("/pictures/step5.png", ClientHandler.class.getClassLoader(), true);
        } else {
            Reference.LOGGER.info("WaterMedia not installed - video/radio features disabled; kill feed and other utilities still active.");
        }
    }

    public static void openVideo(Minecraft client, String url, int volume, boolean isControlBlocked, boolean canSkip) {
        if (!VideoPlayerUtils.hasWaterMedia()) return;
        client.execute(() -> {
            closeExistingVideoScreen(client);
            Minecraft.getInstance().setScreen(new VideoScreen(url, volume, isControlBlocked, canSkip, false));
        });
    }

    public static void openVideo(Minecraft client, String url, int volume, boolean isControlBlocked, boolean canSkip, int optionInMode, int optionInSecs, int optionOutMode, int optionOutSecs) {
        if (!VideoPlayerUtils.hasWaterMedia()) return;
        client.execute(() -> {
            closeExistingVideoScreen(client);
            Minecraft.getInstance().setScreen(new VideoScreen(url, volume, isControlBlocked, canSkip, optionInMode, optionInSecs, optionOutMode, optionOutSecs));
        });
    }

    public static void openRadioGUI(Minecraft client, BlockPos pos, String url, int volume, boolean isPlaying) {
        if (!VideoPlayerUtils.hasWaterMedia()) return;
        client.execute(() -> {
            BlockEntity be = Minecraft.getInstance().level.getBlockEntity(pos);
            if (be instanceof RadioBlockEntity) {
                RadioBlockEntity tv = (RadioBlockEntity) be;
                tv.setUrl(url);
                tv.setVolume(volume);
                tv.setPlaying(isPlaying);
                Minecraft.getInstance().setScreen(new RadioScreen(be));
            }
        });
    }

    private static void closeExistingVideoScreen(Minecraft client) {
        if (client.screen instanceof VideoScreen screen) {
            screen.onClose();
        }
    }

    public static void stopVideoIfExists(Minecraft client) {
        if (!VideoPlayerUtils.hasWaterMedia()) return;
        client.execute(() -> {
            if (Minecraft.getInstance().screen instanceof VideoScreen screen) {
                screen.onClose();
            }
        });
    }

    public static void playMusic(Minecraft client, String url, int volume) {
        if (!VideoPlayerUtils.hasWaterMedia()) return;
        client.execute(() -> {
            // Until any callback in MusicPlayer I will check if the music is playing when added other music player
            for (MusicPlayer musicPlayer : musicPlayers) {
                if (musicPlayer.isPlaying()) {
                    musicPlayer.stop();
                    musicPlayer.release();
                    musicPlayers.remove(musicPlayer);
                }
            }

            // Add the new player
            MusicPlayer musicPlayer = new MusicPlayer();
            musicPlayers.add(musicPlayer);
            musicPlayer.setVolume(volume);
            com.github.NGoedix.videoplayer.util.YoutubeResolver.resolve(url, musicPlayer::start);
        });
    }

    public static void stopMusicIfPlaying(Minecraft client) {
        if (!VideoPlayerUtils.hasWaterMedia()) return;
        client.execute(() -> {
            for (MusicPlayer musicPlayer : musicPlayers) {
                if (musicPlayer.isPlaying()) {
                    musicPlayer.stop();
                    musicPlayer.release();
                    musicPlayers.remove(musicPlayer);
                }
            }
        });
    }

    public static void openVideoGUI(Minecraft client, BlockPos pos, String url, int volume, int tick, boolean isPlaying) {
        if (!VideoPlayerUtils.hasWaterMedia()) return;
        client.execute(() -> {
            BlockEntity be = client.level.getBlockEntity(pos);
            if (be instanceof TVBlockEntity tv) {
                tv.setUrl(url);
                tv.setTick(tick);
                tv.setVolume(volume);
                tv.setPlaying(isPlaying);
                client.setScreen(new TVVideoScreen(be));
            }
        });
    }

    public static void manageVideo(Minecraft client, String url, BlockPos pos, boolean playing, int tick) {
        if (!VideoPlayerUtils.hasWaterMedia()) return;
        client.execute(() -> {
            BlockEntity be = client.level.getBlockEntity(pos);
            if (be instanceof TVBlockEntity tv) {
                tv.setUrl(url);
                tv.setPlaying(playing);
                if (tv.getTick() - 40 > tick || tv.getTick() + 40 < tick)
                    tv.setTick(tick);
                if (tv.requestDisplay() != null) {
                    if (playing)
                        tv.requestDisplay().resume(tv.getTick());
                    else
                        tv.requestDisplay().pause(tv.getTick());
                }
            }
        });
    }

    public static void manageRadio(Minecraft client, String url, BlockPos pos, boolean playing) {
        if (!VideoPlayerUtils.hasWaterMedia()) return;
        client.execute(() -> {
            BlockEntity be = Minecraft.getInstance().level.getBlockEntity(pos);
            if (be instanceof RadioBlockEntity tv) {
                tv.setUrl(url);
                tv.setPlaying(playing);
                tv.notifyPlayer();
            }
        });
    }
}
