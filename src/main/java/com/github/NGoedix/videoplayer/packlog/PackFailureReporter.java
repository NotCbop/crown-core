package com.github.NGoedix.videoplayer.packlog;

import com.github.NGoedix.videoplayer.Reference;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * When the client tells the server that a server-sent resource pack failed to download or load, this
 * uploads the tail of {@code logs/latest.log} to mclo.gs and drops a clickable / copyable link into
 * the player's chat so the failure can be diagnosed.
 *
 * <p>Triggered from {@code ServerboundResourcePackPacketMixin}. Pack negotiation can happen during
 * the configuration phase (before the player is in-world and chat exists), so finished messages are
 * queued and flushed on the client tick once a chat is available.
 */
public final class PackFailureReporter {

    /** Master switch; set to {@code false} to disable log uploads entirely. */
    public static boolean enabled = true;

    /** Don't re-upload for the same pack id within this window. */
    private static final long DEDUPE_MILLIS = 30_000L;
    /** Tail of latest.log to upload — enough for the failure context, well under mclo.gs limits. */
    private static final long MAX_LOG_BYTES = 1_000_000L;

    private static final Map<UUID, Long> RECENT = new ConcurrentHashMap<>();
    private static final ConcurrentLinkedQueue<Component> PENDING = new ConcurrentLinkedQueue<>();

    private PackFailureReporter() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(PackFailureReporter::drain);
    }

    /** Called for every resource-pack status the client reports; only failures do anything. */
    public static void onStatus(UUID id, ServerboundResourcePackPacket.Action action) {
        if (!enabled) return;

        String reason = switch (action) {
            case FAILED_DOWNLOAD -> "failed to download";
            case FAILED_RELOAD -> "failed to load";
            case INVALID_URL -> "had an invalid URL";
            default -> null;
        };
        if (reason == null) return;

        long now = System.currentTimeMillis();
        prune(now);
        Long last = RECENT.put(id, now);
        if (last != null && now - last < DEDUPE_MILLIS) return;

        Reference.LOGGER.warn("Resource pack {} {} - uploading logs to mclo.gs", id, reason);

        Thread worker = new Thread(() -> uploadAndQueue(reason), "crown-packlog-upload");
        worker.setDaemon(true);
        worker.start();
    }

    private static void uploadAndQueue(String reason) {
        String url = null;
        try {
            url = McLogsUploader.upload(readLatestLogTail());
            Reference.LOGGER.info("Uploaded pack-failure logs: {}", url);
        } catch (Exception e) {
            Reference.LOGGER.error("Failed to upload pack-failure logs to mclo.gs: {}", e.getMessage());
        }
        PENDING.add(buildMessage(reason, url));
    }

    private static String readLatestLogTail() throws IOException {
        Path log = Minecraft.getInstance().gameDirectory.toPath().resolve("logs").resolve("latest.log");
        try (FileChannel channel = FileChannel.open(log, StandardOpenOption.READ)) {
            long size = channel.size();
            long start = Math.max(0, size - MAX_LOG_BYTES);
            channel.position(start);
            ByteBuffer buffer = ByteBuffer.allocate((int) (size - start));
            while (buffer.hasRemaining() && channel.read(buffer) != -1) {
                // keep reading
            }
            buffer.flip();
            String text = StandardCharsets.UTF_8.decode(buffer).toString();
            if (start > 0) {
                int firstNewline = text.indexOf('\n');
                text = "...(log truncated to last " + (MAX_LOG_BYTES / 1000) + " KB)\n"
                        + (firstNewline >= 0 ? text.substring(firstNewline + 1) : text);
            }
            return text;
        }
    }

    private static Component buildMessage(String reason, String url) {
        MutableComponent message = Component.literal("[Crown] ").withStyle(ChatFormatting.GOLD);
        if (url == null) {
            return message.append(Component.literal("Resource pack " + reason
                    + ", and uploading the logs to mclo.gs failed. See logs/latest.log.")
                    .withStyle(ChatFormatting.RED));
        }

        MutableComponent link = Component.literal(url).withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent.OpenUrl(URI.create(url)))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to open in your browser"))));
        MutableComponent copy = Component.literal(" [copy]").withStyle(style -> style
                .withColor(ChatFormatting.GRAY)
                .withClickEvent(new ClickEvent.CopyToClipboard(url))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy the link"))));

        return message
                .append(Component.literal("Resource pack " + reason + ". Logs: ").withStyle(ChatFormatting.YELLOW))
                .append(link)
                .append(copy);
    }

    private static void drain(Minecraft client) {
        if (PENDING.isEmpty() || client.player == null || client.gui == null) return;
        Component component;
        while ((component = PENDING.poll()) != null) {
            client.gui.getChat().addMessage(component);
        }
    }

    private static void prune(long now) {
        Iterator<Map.Entry<UUID, Long>> it = RECENT.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue() > DEDUPE_MILLIS) it.remove();
        }
    }
}
