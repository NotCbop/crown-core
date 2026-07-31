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

    /**
     * Resolves {@code url} to a direct stream URL, or returns {@code null} if yt-dlp produced nothing.
     * Blocking (spawns a process); call from a background thread.
     */
    public static String resolveDirectUrl(String url) throws IOException, InterruptedException {
        Path bin = binary();

        // -f b = best muxed (single video+audio stream; VLC can't sync separate streams),
        // -g just prints the direct media url instead of downloading.
        Process process = new ProcessBuilder(
                bin.toString(), "--no-playlist", "--no-warnings", "-q", "-f", "b", "-g", url)
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

        for (String line : stdout.split("\\R")) {
            if (line.startsWith("http")) return line.trim();
        }
        if (!stderr.isBlank()) {
            Reference.LOGGER.error("yt-dlp could not resolve '{}': {}", url, stderr.strip());
        }
        return null;
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
