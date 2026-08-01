package com.github.NGoedix.videoplayer.util;

import com.github.NGoedix.videoplayer.Reference;
import org.watermedia.api.network.NetworkAPI;

import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

/**
 * Resolves YouTube watch/share/shorts URLs into directly-playable stream URLs.
 *
 * <p>WaterMedia 2.1.x dropped its bundled YouTube extractor, so VLC receives the raw youtube.com /
 * youtu.be link and silently fails to open it. Resolution is delegated to a standalone yt-dlp
 * binary (see {@link YtDlp}) — the same engine WaterMedia's platform-extension uses — because
 * static Java extractors (java-youtube-downloader, NewPipeExtractor) break every time YouTube
 * changes its player, while yt-dlp keeps up.</p>
 *
 * <p>For quality above YouTube's ~360p muxed streams, video and audio come as two separate URLs;
 * the audio one is handed to VLC as an {@code :input-slave} media option, so callbacks receive the
 * primary URI plus the VLC option array to pass to
 * {@code BasePlayer.start(URI, String[])}.</p>
 */
public final class YoutubeResolver {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "VideoPlayer-YT-Resolver");
        t.setDaemon(true);
        return t;
    });

    private static final String[] NO_OPTIONS = new String[0];

    private YoutubeResolver() {}

    public static boolean isYoutube(String url) {
        if (url == null) return false;
        String u = url.toLowerCase();
        return u.contains("youtube.com/") || u.contains("youtu.be/");
    }

    /**
     * Resolves {@code url} to playable video stream(s) and hands the primary URI plus VLC media
     * options to {@code callback}. YouTube links are resolved on a background thread (spawns
     * yt-dlp); every other url is passed through immediately. On any failure the original url is
     * used as a fallback.
     */
    public static void resolve(String url, BiConsumer<URI, String[]> callback) {
        resolve(url, false, callback);
    }

    /** Like {@link #resolve} but picks an audio-only stream (for music playback). */
    public static void resolveAudio(String url, BiConsumer<URI, String[]> callback) {
        resolve(url, true, callback);
    }

    private static void resolve(String url, boolean audioOnly, BiConsumer<URI, String[]> callback) {
        // Preloaded videos play from local files — instant start, no resolving or buffering.
        VideoPreload.Entry preloaded = VideoPreload.get(url);
        if (preloaded != null) {
            if (audioOnly) {
                Path audio = preloaded.audio() != null ? preloaded.audio() : preloaded.video();
                callback.accept(audio.toUri(), NO_OPTIONS);
            } else {
                String[] options = preloaded.audio() != null
                        ? new String[]{":input-slave=" + preloaded.audio().toUri()}
                        : NO_OPTIONS;
                callback.accept(preloaded.video().toUri(), options);
            }
            return;
        }

        if (!isYoutube(url)) {
            callback.accept(NetworkAPI.parseURI(url), NO_OPTIONS);
            return;
        }
        EXECUTOR.execute(() -> {
            String resolved = url;
            String[] options = NO_OPTIONS;
            try {
                String[] streams = YtDlp.resolveStreams(url, audioOnly);
                if (streams.length > 0) {
                    resolved = streams[0];
                    if (streams.length > 1) {
                        options = new String[]{":input-slave=" + streams[1]};
                    }
                }
            } catch (Throwable t) {
                Reference.LOGGER.error("Failed to resolve YouTube url '{}', falling back to the raw url", url, t);
            }
            callback.accept(NetworkAPI.parseURI(resolved), options);
        });
    }
}
