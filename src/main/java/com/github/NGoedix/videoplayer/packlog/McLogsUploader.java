package com.github.NGoedix.videoplayer.packlog;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class McLogsUploader {

    private static final URI ENDPOINT = URI.create("https://api.mclo.gs/1/log");

    private McLogsUploader() {}

    public static String upload(String content) throws IOException, InterruptedException {
        String body = "content=" + URLEncoder.encode(content, StandardCharsets.UTF_8);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        HttpRequest request = HttpRequest.newBuilder(ENDPOINT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "CrownChampionshipUtilities")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        if (json.has("success") && json.get("success").getAsBoolean() && json.has("url")) {
            return json.get("url").getAsString();
        }
        String error = json.has("error") ? json.get("error").getAsString() : "HTTP " + response.statusCode();
        throw new IOException("mclo.gs rejected the log: " + error);
    }
}
