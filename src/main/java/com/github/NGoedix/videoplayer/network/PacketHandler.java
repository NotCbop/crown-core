package com.github.NGoedix.videoplayer.network;

import com.github.NGoedix.videoplayer.block.entity.custom.RadioBlockEntity;
import com.github.NGoedix.videoplayer.block.entity.custom.TVBlockEntity;
import com.github.NGoedix.videoplayer.client.ClientHandler;
import com.github.NGoedix.videoplayer.killfeed.KillFeed;
import com.github.NGoedix.videoplayer.killfeed.KillFeedChatListener;
import com.github.NGoedix.videoplayer.network.packet.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.UUID;

public class PacketHandler {

    public static void registerC2SPackets() {
        PayloadTypeRegistry.playC2S().register(VideoUpdateMessage.TYPE, VideoUpdateMessage.CODEC);
        PayloadTypeRegistry.playC2S().register(RadioUpdateMessage.TYPE, RadioUpdateMessage.CODEC);

        PayloadTypeRegistry.playS2C().register(FrameVideoMessage.TYPE, FrameVideoMessage.CODEC);
        PayloadTypeRegistry.playS2C().register(RadioMessage.TYPE, RadioMessage.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenVideoManagerScreenMessage.TYPE, OpenVideoManagerScreenMessage.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenRadioManagerScreenMessage.TYPE, OpenRadioManagerScreenMessage.CODEC);
        PayloadTypeRegistry.playS2C().register(SendVideoMessage.TYPE, SendVideoMessage.CODEC);
        PayloadTypeRegistry.playS2C().register(SendCustomVideoMessage.TYPE, SendCustomVideoMessage.CODEC);
        PayloadTypeRegistry.playS2C().register(SendMusicMessage.TYPE, SendMusicMessage.CODEC);
        PayloadTypeRegistry.playS2C().register(PlayBroadcastMessage.TYPE, PlayBroadcastMessage.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(VideoUpdateMessage.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                if (player.level().getBlockEntity(payload.pos()) instanceof TVBlockEntity tvBlockEntity) {
                    if (payload.exit()) {
                        tvBlockEntity.setBeingUsed(new UUID(0, 0));
                    } else {
                        tvBlockEntity.setUrl(payload.url());
                        tvBlockEntity.setVolume(payload.volume());
                        if (payload.tick() != -1)
                            tvBlockEntity.setTick(payload.tick());
                        tvBlockEntity.setPlaying(payload.isPlaying());
                        if (payload.stopped())
                            tvBlockEntity.stop();
                        tvBlockEntity.notifyPlayer();
                    }
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(RadioUpdateMessage.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                if (player.level().getBlockEntity(payload.pos()) instanceof RadioBlockEntity radioBlockEntity) {
                    if (payload.exit()) {
                        radioBlockEntity.setBeingUsed(new UUID(0, 0));
                    } else {
                        radioBlockEntity.setUrl(payload.url());
                        if (payload.tick() != -1)
                            radioBlockEntity.setTick(payload.tick());
                        radioBlockEntity.setVolume(payload.volume());
                        radioBlockEntity.setPlaying(payload.isPlaying());
                        radioBlockEntity.notifyPlayer();
                    }
                }
            });
        });
    }

    public static void registerS2CPackets() {
        ClientPlayNetworking.registerGlobalReceiver(FrameVideoMessage.TYPE, (payload, context) ->
                ClientHandler.manageVideo(context.client(), payload.url(), payload.pos(), payload.playing(), payload.tick()));

        ClientPlayNetworking.registerGlobalReceiver(RadioMessage.TYPE, (payload, context) ->
                ClientHandler.manageRadio(context.client(), payload.url(), payload.pos(), payload.playing()));

        ClientPlayNetworking.registerGlobalReceiver(OpenVideoManagerScreenMessage.TYPE, (payload, context) ->
                ClientHandler.openVideoGUI(context.client(), payload.pos(), payload.url(), payload.volume(), payload.tick(), payload.isPlaying()));

        ClientPlayNetworking.registerGlobalReceiver(OpenRadioManagerScreenMessage.TYPE, (payload, context) ->
                ClientHandler.openRadioGUI(context.client(), payload.pos(), payload.url(), payload.volume(), payload.isPlaying()));

        ClientPlayNetworking.registerGlobalReceiver(SendVideoMessage.TYPE, (payload, context) -> {
            if (payload.action() == SendVideoMessage.VideoMessageType.START)
                ClientHandler.openVideo(context.client(), payload.url(), payload.volume(), payload.controlBlocked(), payload.canSkip());
            else
                ClientHandler.stopVideoIfExists(context.client());
        });

        ClientPlayNetworking.registerGlobalReceiver(SendCustomVideoMessage.TYPE, (payload, context) -> {
            if (payload.action() == SendCustomVideoMessage.VideoMessageType.START) {
                if (payload.mode() == 0)
                    ClientHandler.openVideo(context.client(), payload.url(), payload.volume(), payload.controlBlocked(), payload.canSkip(),
                            payload.optionInMode(), payload.optionInSecs(), payload.optionOutMode(), payload.optionOutSecs());
            } else {
                ClientHandler.stopVideoIfExists(context.client());
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(SendMusicMessage.TYPE, (payload, context) -> {
            if (payload.action() == SendMusicMessage.MusicMessageType.START)
                ClientHandler.playMusic(context.client(), payload.url(), payload.volume());
            else
                ClientHandler.stopMusicIfPlaying(context.client());
        });

        ClientPlayNetworking.registerGlobalReceiver(PlayBroadcastMessage.TYPE, (payload, context) -> {
            String body = payload.body().trim();
            if (body.isEmpty()) return;

            if (body.equalsIgnoreCase("stop")) {
                ClientHandler.stopVideoIfExists(context.client());
                return;
            }

            if (body.regionMatches(true, 0, "killfeed", 0, "killfeed".length())) {
                String[] kf = body.split("\\s+");
                boolean on = kf.length < 2 || parseOnOff(kf[1]);
                context.client().execute(() -> KillFeed.setEnabled(on));
                return;
            }

            if (body.regionMatches(true, 0, "killmsg ", 0, "killmsg ".length())) {
                if (!KillFeed.enabled) return;
                String deathText = body.substring("killmsg ".length());
                context.client().execute(() -> KillFeedChatListener.handleText(deathText));
                return;
            }

            if (body.equalsIgnoreCase("stopmusic")) {
                ClientHandler.stopMusicIfPlaying(context.client());
                return;
            }

            if (body.regionMatches(true, 0, "music;", 0, "music;".length())) {
                String[] m = body.split(";");
                String mUrl = m.length > 1 ? m[1].trim() : "";
                int mVol = m.length > 2 ? parseIntOrDefault(m[2].trim(), 100) : 100;
                if (!mUrl.isEmpty())
                    ClientHandler.playMusic(context.client(), mUrl, mVol);
                return;
            }

            if (body.regionMatches(true, 0, "govideo;", 0, "govideo;".length())) {
                String[] p = body.split(";");
                String gUrl = p.length > 1 ? p[1].trim() : "";
                if (gUrl.isEmpty()) return;
                int gVol = p.length > 2 ? parseIntOrDefault(p[2].trim(), 100) : 100;
                boolean gBlocked = p.length > 3 && Boolean.parseBoolean(p[3].trim());
                boolean gSkip = p.length <= 4 || Boolean.parseBoolean(p[4].trim());
                int gMode = p.length > 5 ? parseIntRaw(p[5].trim(), 0) : 0;
                int optInMode = p.length > 7 ? parseIntRaw(p[7].trim(), -1) : -1;
                int optInSecs = p.length > 8 ? parseIntRaw(p[8].trim(), -1) : -1;
                int optOutMode = p.length > 9 ? parseIntRaw(p[9].trim(), -1) : -1;
                int optOutSecs = p.length > 10 ? parseIntRaw(p[10].trim(), -1) : -1;
                if (gMode == 0)
                    ClientHandler.openVideo(context.client(), gUrl, gVol, gBlocked, gSkip, optInMode, optInSecs, optOutMode, optOutSecs);
                return;
            }

            String[] parts = body.split(";");
            String url = parts[0].trim();
            int volume = parts.length > 1 ? parseIntOrDefault(parts[1].trim(), 100) : 100;
            boolean controlBlocked = parts.length > 2 && Boolean.parseBoolean(parts[2].trim());
            boolean canSkip = parts.length <= 3 || Boolean.parseBoolean(parts[3].trim());

            ClientHandler.openVideo(context.client(), url, volume, controlBlocked, canSkip);
        });
    }

    private static boolean parseOnOff(String value) {
        switch (value.toLowerCase()) {
            case "off":
            case "false":
            case "disable":
            case "disabled":
            case "0":
            case "no":
                return false;
            default:
                return true;
        }
    }

    private static int parseIntOrDefault(String value, int fallback) {
        try {
            return Math.max(0, Math.min(100, Integer.parseInt(value)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int parseIntRaw(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static void sendS2CSendVideoStart(ServerPlayer player, String url, int volume, boolean controlBlocked, boolean canSkip) {
        ServerPlayNetworking.send(player, new SendVideoMessage(SendVideoMessage.VideoMessageType.START, url, volume, controlBlocked, canSkip));
    }

    public static void sendS2CSendVideoStop(ServerPlayer player) {
        ServerPlayNetworking.send(player, new SendVideoMessage(SendVideoMessage.VideoMessageType.STOP, "", 0, false, false));
    }

    public static void sendS2CSendMusicStart(ServerPlayer player, String url, int volume) {
        ServerPlayNetworking.send(player, new SendMusicMessage(SendMusicMessage.MusicMessageType.START, url, volume));
    }

    public static void sendS2CSendMusicStop(ServerPlayer player) {
        ServerPlayNetworking.send(player, new SendMusicMessage(SendMusicMessage.MusicMessageType.STOP, "", 0));
    }

    public static void sendS2CSendVideoStart(ServerPlayer player, String url, int volume, boolean controlBlocked, boolean canSkip, int mode, int position, int optionInMode, int optionInSecs, int optionOutMode, int optionOutSecs) {
        ServerPlayNetworking.send(player, new SendCustomVideoMessage(SendCustomVideoMessage.VideoMessageType.START, url, volume, controlBlocked, canSkip, mode, position, optionInMode, optionInSecs, optionOutMode, optionOutSecs));
    }

    public static void sendS2COpenVideoManagerScreen(ServerPlayer player, BlockPos pos, String url, int volume, int tick, boolean isPlaying) {
        ServerPlayNetworking.send(player, new OpenVideoManagerScreenMessage(pos, url, volume, tick, isPlaying));
    }

    public static void sendS2COpenRadioManagerScreen(ServerPlayer player, BlockPos pos, String url, int volume, boolean isPlaying) {
        ServerPlayNetworking.send(player, new OpenRadioManagerScreenMessage(pos, url, volume, isPlaying));
    }

    public static void sendS2CFrameVideoMessage(LevelChunk chunk, String url, BlockPos pos, boolean playing, int tick) {
        FrameVideoMessage msg = new FrameVideoMessage(url, pos, playing, tick);
        for (ServerPlayer player : PlayerLookup.tracking((ServerLevel) chunk.getLevel(), chunk.getPos()))
            ServerPlayNetworking.send(player, msg);
    }

    public static void sendS2CRadioMessage(LevelChunk chunk, String url, BlockPos pos, boolean playing) {
        RadioMessage msg = new RadioMessage(url, pos, playing);
        for (ServerPlayer player : PlayerLookup.tracking((ServerLevel) chunk.getLevel(), chunk.getPos()))
            ServerPlayNetworking.send(player, msg);
    }

    public static void sendC2SVideoUpdateMessage(BlockPos pos, String url, int volume, int tick, boolean isPlaying, boolean stopped, boolean exit) {
        ClientPlayNetworking.send(new VideoUpdateMessage(pos, url, volume, tick, isPlaying, stopped, exit));
    }

    public static void sendC2SRadioUpdateMessage(BlockPos pos, String url, int volume, int tick, boolean isPlaying, boolean exit) {
        ClientPlayNetworking.send(new RadioUpdateMessage(pos, url, volume, tick, isPlaying, exit));
    }
}
