package com.github.NGoedix.videoplayer.util;

import com.github.NGoedix.videoplayer.Reference;
import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import org.watermedia.api.network.NetworkAPI;

import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves YouTube watch/share/shorts URLs into a directly-playable stream URL.
 *
 * <p>WaterMedia 2.1.x dropped its bundled YouTube extractor, so VLC receives the raw youtube.com /
 * youtu.be link and silently fails to open it. This helper uses java-youtube-downloader to turn a
 * YouTube link into a concrete googlevideo stream URL (preferring a muxed video+audio format so VLC
 * only has to open a single stream) before the player starts.</p>
 */
public final class YoutubeResolver {

    private static final YoutubeDownloader DOWNLOADER = new YoutubeDownloader();

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "VideoPlayer-YT-Resolver");
        t.setDaemon(true);
        return t;
    });

    private static final Pattern ID_PATTERN = Pattern.compile(
            "(?:youtu\\.be/|youtube\\.com/(?:watch\\?(?:.*&)?v=|embed/|shorts/|live/|v/))([\\w-]{11})");

    private YoutubeResolver() {}

    public static boolean isYoutube(String url) {
        if (url == null) return false;
        String u = url.toLowerCase();
        return u.contains("youtube.com/") || u.contains("youtu.be/");
    }

    /**
     * Resolves {@code url} to a directly-playable stream and hands the result to {@code callback}.
     * YouTube links are resolved on a background thread (network call); every other url is passed
     * through immediately. On any failure the original url is used as a fallback.
     */
    public static void resolve(String url, Consumer<URI> callback) {
        if (!isYoutube(url)) {
            callback.accept(NetworkAPI.parseURI(url));
            return;
        }
        EXECUTOR.execute(() -> {
            String resolved = url;
            try {
                String direct = resolveDirect(url);
                if (direct != null) resolved = direct;
            } catch (Throwable t) {
                Reference.LOGGER.error("Failed to resolve YouTube url '{}', falling back to the raw url", url, t);
            }
            callback.accept(NetworkAPI.parseURI(resolved));
        });
    }

    private static String resolveDirect(String url) {
        String id = extractId(url);
        if (id == null) return null;

        VideoInfo info = DOWNLOADER.getVideoInfo(new RequestVideoInfo(id)).data();
        if (info == null) return null;

        var muxed = info.bestVideoWithAudioFormat();
        if (muxed != null) return muxed.url();

        var video = info.bestVideoFormat();
        if (video != null) return video.url();

        var audio = info.bestAudioFormat();
        if (audio != null) return audio.url();
        return null;
    }

    private static String extractId(String url) {
        Matcher m = ID_PATTERN.matcher(url);
        return m.find() ? m.group(1) : null;
    }
}
