package com.github.NGoedix.videoplayer.util;

import com.github.NGoedix.videoplayer.Reference;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Manages a standalone <a href="https://github.com/yt-dlp/yt-dlp">yt-dlp</a> binary and uses it to
 * resolve site URLs (YouTube etc.) into direct stream URLs VLC can open.
 *
 * <p>This is the same approach WaterMedia's platform-extension takes: the self-contained yt-dlp
 * executable (no Python required) is downloaded on demand from the official GitHub releases into
 * {@code <gamedir>/videoplayer/} and re-checked for updates every few days, because YouTube breaks
 * static extractors (like the old java-youtube-downloader) every few months while yt-dlp keeps up.</p>
 */
public final class YtDlp {

    private static final String RELEASE_URL = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/";
    private static final Duration UPDATE_INTERVAL = Duration.ofDays(7);
    private static final int RESOLVE_TIMEOUT_SECONDS = 30;

    private static volatile boolean updateChecked = false;

    private YtDlp() {}

    /** Kicks off the binary download/update check in the background so the first video doesn't wait on it. */
    public static void prepareAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                binary();
            } catch (Exception e) {
                Reference.LOGGER.warn("Could not prepare yt-dlp (YouTube links will fall back to the raw url): {}", e.toString());
            }
        });
    }

    // Best H.264 video up to 1080p plus separate m4a audio (VLC plays the audio via :input-slave);
    // falls back to any <=1080p video+audio pair, then to the best muxed stream (~360p).
    private static final String VIDEO_FORMAT = "bv*[vcodec^=avc1][height<=1080]+ba[ext=m4a]/bv*[height<=1080]+ba/b";
    // Audio-only for /playmusic; falls back to a muxed stream whose audio track VLC will play.
    private static final String AUDIO_FORMAT = "ba[ext=m4a]/ba/b";

    /**
     * Resolves {@code url} to direct stream URLs, or returns an empty array if yt-dlp produced
     * nothing. One element = muxed/audio stream; two elements = separate video + audio streams.
     * Blocking (spawns a process); call from a background thread.
     */
    public static String[] resolveStreams(String url, boolean audioOnly) throws IOException, InterruptedException {
        Path bin = binary();

        // -g just prints the direct media url(s) instead of downloading (one per line).
        Process process = new ProcessBuilder(
                bin.toString(), "--no-playlist", "--no-warnings", "-q",
                "-f", audioOnly ? AUDIO_FORMAT : VIDEO_FORMAT, "-g", url)
                .start();

        String stdout;
        String stderr;
        try {
            stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!process.waitFor(RESOLVE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IOException("yt-dlp timed out after " + RESOLVE_TIMEOUT_SECONDS + "s");
            }
        } finally {
            process.destroyForcibly();
        }

        String[] urls = java.util.Arrays.stream(stdout.split("\\R"))
                .map(String::trim)
                .filter(line -> line.startsWith("http"))
                .toArray(String[]::new);
        if (urls.length == 0 && !stderr.isBlank()) {
            Reference.LOGGER.error("yt-dlp could not resolve '{}': {}", url, stderr.strip());
        }
        return urls;
    }

    /**
     * Downloads {@code url} (selected by {@code format}) into {@code outputDir} as
     * {@code baseName.<ext>} and returns the final file path, or {@code null} if yt-dlp did not
     * produce a file. Blocking, potentially for minutes on large videos; call from a background
     * thread.
     */
    public static Path download(String url, String format, Path outputDir, String baseName) throws IOException, InterruptedException {
        Path bin = binary();
        Files.createDirectories(outputDir);

        Process process = new ProcessBuilder(
                bin.toString(), "--no-playlist", "--no-warnings", "--no-simulate", "-q",
                "--print", "after_move:filepath",
                "-f", format,
                "-o", outputDir.resolve(baseName + ".%(ext)s").toString(),
                url).start();

        // Drain stderr concurrently so a full pipe buffer can't stall the download.
        CompletableFuture<String> stderrFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                return "";
            }
        });

        String stdout;
        try {
            stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!process.waitFor(15, TimeUnit.MINUTES)) {
                throw new IOException("yt-dlp download timed out for " + url);
            }
        } finally {
            process.destroyForcibly();
        }

        for (String line : stdout.split("\\R")) {
            Path file = Path.of(line.trim());
            if (!line.isBlank() && Files.exists(file)) return file;
        }
        String stderr = stderrFuture.getNow("");
        throw new IOException("yt-dlp produced no file for " + url + (stderr.isBlank() ? "" : ": " + stderr.strip()));
    }

    /** Returns the yt-dlp binary, downloading or updating it first if needed. */
    private static synchronized Path binary() throws IOException, InterruptedException {
        Path bin = FabricLoader.getInstance().getGameDir().resolve("videoplayer").resolve(binaryName());

        if (!Files.exists(bin)) {
            download(bin);
        } else if (!updateChecked && olderThan(bin, UPDATE_INTERVAL)) {
            selfUpdate(bin);
        }
        updateChecked = true;
        return bin;
    }

    private static String binaryName() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) return "yt-dlp.exe";
        if (os.contains("mac") || os.contains("darwin")) return "yt-dlp_macos";
        return "yt-dlp_linux";
    }

    private static void download(Path bin) throws IOException, InterruptedException {
        Reference.LOGGER.info("Downloading yt-dlp (one-time, ~18 MB) for YouTube playback...");
        Files.createDirectories(bin.getParent());
        Path tmp = bin.resolveSibling(bin.getFileName() + ".tmp");

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(RELEASE_URL + binaryName()))
                .timeout(Duration.ofMinutes(5))
                .build();
        HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(tmp));
        if (response.statusCode() != 200) {
            Files.deleteIfExists(tmp);
            throw new IOException("yt-dlp download failed: HTTP " + response.statusCode());
        }

        Files.move(tmp, bin, StandardCopyOption.REPLACE_EXISTING);
        //noinspection ResultOfMethodCallIgnored
        bin.toFile().setExecutable(true, false);
        Reference.LOGGER.info("yt-dlp downloaded to {}", bin);
    }

    private static void selfUpdate(Path bin) {
        try {
            Reference.LOGGER.info("Checking for yt-dlp updates...");
            Process process = new ProcessBuilder(bin.toString(), "-U").start();
            process.getInputStream().readAllBytes();
            process.waitFor(2, TimeUnit.MINUTES);
            process.destroyForcibly();
            // Bump the timestamp even when yt-dlp reports "already up to date" (it leaves the file
            // untouched then), so the check doesn't re-run every session.
            Files.setLastModifiedTime(bin, FileTime.from(Instant.now()));
        } catch (Exception e) {
            Reference.LOGGER.warn("yt-dlp self-update failed (keeping current version): {}", e.toString());
        }
    }

    private static boolean olderThan(Path bin, Duration age) {
        try {
            return Files.getLastModifiedTime(bin).toInstant().isBefore(Instant.now().minus(age));
        } catch (IOException e) {
            return false;
        }
    }
}
