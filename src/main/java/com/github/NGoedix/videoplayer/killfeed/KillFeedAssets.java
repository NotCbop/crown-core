package com.github.NGoedix.videoplayer.killfeed;

import com.github.NGoedix.videoplayer.Reference;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Identifiers, fonts and icon helpers that mirror Trident's {@code Resources}/{@code FontCollection}
 * so the kill feed pulls the exact same assets out of the resource pack. All rendering is plain
 * vanilla {@code GuiGraphics} though, so it does not need SheepLib/Noxesium.
 */
public final class KillFeedAssets {

    private KillFeedAssets() {}

    // --- Namespaces (server resource pack provides the trident:/mcc: assets) ---
    public static Identifier trident(String path) {
        return Identifier.fromNamespaceAndPath("trident", path);
    }

    public static Identifier mcc(String path) {
        return Identifier.fromNamespaceAndPath("mcc", path);
    }

    /** {@code crown} namespace — the only kill-feed assets bundled into this mod (chevron sprites). */
    public static Identifier crown(String path) {
        return Identifier.fromNamespaceAndPath("crown", path);
    }

    // --- Fonts ---
    /** {@code trident:icon} font, used for the hard-coded kill-method glyphs (..). */
    public static final Identifier TRIDENT_ICON_FONT = trident("icon");
    /** {@code mcc:icon} font, used for the skull / kills glyphs. */
    public static final Identifier MCC_ICON_FONT = mcc("icon");
    /** {@code mcc:hud} font, used for player names. */
    public static final Identifier MCC_HUD_FONT = mcc("hud");

    private static Style font(Identifier font) {
        return Style.EMPTY.withFont(new FontDescription.Resource(font));
    }

    public static MutableComponent withFont(MutableComponent component, Identifier font) {
        return component.withStyle(font(font));
    }

    /** A single glyph in the {@code trident:icon} font (e.g. {@code ''} = the bow). */
    public static MutableComponent tridentGlyph(int codepoint) {
        return withFont(Component.literal(new String(Character.toChars(codepoint))), TRIDENT_ICON_FONT);
    }

    /**
     * Looks up the {@code mcc:icon} glyph that renders a given bitmap texture (e.g.
     * {@code _fonts/kills.png}) by reading {@code mcc:font/icon.json} from the server pack at
     * runtime. Cached; returns {@code null} if the pack does not provide it.
     */
    public static MutableComponent mccIconByTexture(String texturePath) {
        Character ch = lookupMccIconChar(texturePath);
        if (ch == null) return null;
        return withFont(Component.literal(String.valueOf(ch.charValue())), MCC_ICON_FONT);
    }

    private static final Map<String, Character> ICON_CHAR_CACHE = new HashMap<>();
    private static final char MISSING = '\0';

    private static Character lookupMccIconChar(String texturePath) {
        Character cached = ICON_CHAR_CACHE.get(texturePath);
        if (cached != null) {
            return cached == MISSING ? null : cached;
        }

        Character found = null;
        try {
            Minecraft minecraft = Minecraft.getInstance();
            Optional<Resource> resource = minecraft.getResourceManager().getResource(mcc("font/icon.json"));
            if (resource.isPresent()) {
                try (InputStream in = resource.get().open();
                     InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonArray providers = root.getAsJsonArray("providers");
                    String want = "mcc:" + texturePath;
                    for (int i = 0; i < providers.size() && found == null; i++) {
                        JsonObject provider = providers.get(i).getAsJsonObject();
                        if (!provider.has("file") || !provider.has("chars")) continue;
                        if (!provider.get("file").getAsString().equals(want)) continue;
                        JsonArray chars = provider.getAsJsonArray("chars");
                        if (chars.isEmpty()) continue;
                        String line = chars.get(0).getAsString();
                        if (!line.isEmpty()) found = line.charAt(0);
                    }
                }
            }
        } catch (Exception e) {
            Reference.LOGGER.warn("Kill feed could not read mcc:font/icon.json for '{}': {}", texturePath, e.getMessage());
        }

        ICON_CHAR_CACHE.put(texturePath, found == null ? MISSING : found);
        return found;
    }

    public static void clearCache() {
        ICON_CHAR_CACHE.clear();
    }

    /**
     * Drops the cached icon lookups whenever resources reload, so glyphs resolve correctly after the
     * server pack is applied (or any pack is toggled). Without this, a lookup that ran before the
     * {@code mcc:} pack was available would cache a "missing" result for the whole session.
     */
    public static void register() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() {
                        return crown("killfeed_icon_cache");
                    }

                    @Override
                    public void onResourceManagerReload(ResourceManager manager) {
                        clearCache();
                    }
                });
    }

    /** ARGB from a 0xRRGGBB colour and an 0-255 alpha. */
    public static int color(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }
}
