package com.github.NGoedix.videoplayer.util;

import com.github.NGoedix.videoplayer.Reference;
import org.watermedia.api.network.NetworkAPI;

import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Resolves YouTube watch/share/shorts URLs into a directly-playable stream URL.
 *
 * <p>WaterMedia 2.1.x dropped its bundled YouTube extractor, so VLC receives the raw youtube.com /
 * youtu.be link and silently fails to open it. Resolution is delegated to a standalone yt-dlp
 * binary (see {@link YtDlp}) — the same engine WaterMedia's platform-extension uses — because
 * static Java extractors (java-youtube-downloader, NewPipeExtractor) break every time YouTube
 * changes its player, while yt-dlp keeps up.</p>
 */
public final class YoutubeResolver {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "VideoPlayer-YT-Resolver");
        t.setDaemon(true);
        return t;
    });

    private YoutubeResolver() {}

    public static boolean isYoutube(String url) {
        if (url == null) return false;
        String u = url.toLowerCase();
        return u.contains("youtube.com/") || u.contains("youtu.be/");
    }

    /**
     * Resolves {@code url} to a directly-playable stream and hands the result to {@code callback}.
     * YouTube links are resolved on a background thread (spawns yt-dlp); every other url is passed
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
                String direct = YtDlp.resolveDirectUrl(url);
                if (direct != null) resolved = direct;
            } catch (Throwable t) {
                Reference.LOGGER.error("Failed to resolve YouTube url '{}', falling back to the raw url", url, t);
            }
            callback.accept(NetworkAPI.parseURI(resolved));
        });
    }
}
