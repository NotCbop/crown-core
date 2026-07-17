# CLAUDE.md

Guidance for working in this repository.

## Project

**Crown Championship Utilities** — a Fabric client/server mod for **Minecraft 1.21.11** (Java 21).
It started as NGoedix's *Video Player* mod (play videos/streams on in-world TV & Radio blocks
via the WaterMedia/VLC backend) and is being expanded with extra utilities. The first added
feature is a **kill feed HUD**.

- Mod id is still `videoplayer` and the base package is `com.github.NGoedix.videoplayer`
  (the rename to "Crown Championship Utilities" was **display-name only** — see `fabric.mod.json`,
  the init log line, and the `itemGroup.videoplayer.items` lang value). Do not rename the id or
  package; assets/data are keyed off `videoplayer`.

## Build & run

```powershell
.\gradlew.bat compileJava        # fast compile check (use --offline if deps are cached)
.\gradlew.bat build              # full build -> build\libs\ (main jar = the one without -sources)
.\gradlew.bat runClient          # launch a dev client; use the IDE Debug config for hot-swap
```

Runtime mods needed alongside this one: **Fabric API** (required). **WaterMedia** (the video backend)
is an **optional** dependency (`suggests` in `fabric.mod.json`): without it the mod still loads and the
kill feed / pack reporter work, while the video/radio features stay disabled. All WaterMedia entry
points are gated on `VideoPlayerUtils.hasWaterMedia()` — most importantly `VideoPlayerBlockEntity.
requestDisplay()` (so the client BE tick/renderer never touch WaterMedia classes when it's absent) and
the `ClientHandler` video/music methods + init. `java-youtube-downloader` is shaded into the jar by the
`jar` task. There is **no Kotlin and no Noxesium** dependency.

## Mappings gotcha (important)

Uses `loom.officialMojangMappings()`, but this 1.21.11 layered mapping set **renames some classes**
vs. classic Mojmap. Code in this repo uses, and you should use:

- `net.minecraft.resources.Identifier` (NOT `ResourceLocation`); factory `Identifier.fromNamespaceAndPath(ns, path)`
- `net.minecraft.client.DeltaTracker` (second arg of `HudRenderCallback.onHudRender`)
- `GuiGraphics`: `fill`, `drawString(font, String|Component, x, y, color)`, `blit(RenderPipelines.GUI_TEXTURED, id, ...)`,
  `blitSprite(RenderPipelines.GUI_TEXTURED, id, x, y, w, h, color)`, `renderItem`, `guiWidth()/guiHeight()`

When unsure of a mapped name, **verify against the loom tiny file instead of guessing** (format is
`official  intermediary  named`):
`~/.gradle/caches/fabric-loom/1.21.11/loom.mappings.1_21_11.layered+hash.*/mappings.tiny`

Mixins live in `com.github.NGoedix.videoplayer.mixin` and are declared in
`src/main/resources/videoplayer.mixins.json` (client-only entries go in its `client` array). Loom
remaps mixin refs in-place, so there is **no separate refmap file** in the built jar — that is
expected. An access widener exists at `src/main/resources/videoplayer.accesswidener`.

## Layout

- `VideoPlayer.java` — `ModInitializer` (blocks, block entities, commands, creative tab).
- `client/ClientHandler.java` — `ClientModInitializer`; registers renderers, packets, and the kill feed.
- `block/`, `network/`, `client/gui/`, `util/` — the original video player feature.
- `killfeed/` — the kill feed (see below).
- `packlog/` + `mixin/` — the resource-pack-failure log uploader (see below).

## Kill feed (`com.github.NGoedix.videoplayer.killfeed`)

A vanilla `GuiGraphics` HUD reproduction of the kill feed from the Trident mod
(`C:\Users\Owner\Desktop\Trident-master`). Trident's version is Kotlin + SheepLib + Noxesium and
renders through `GuiGraphicsExtractor`/`extractWidgetRenderState`, which **do not exist in vanilla
1.21.11** — so it is reimplemented to *look the same* using the same assets, not copied.

Asset split: **the chevron transition sprites and the streak badges (`streak1..5`) are bundled into
this mod** (under the `crown` namespace — see "Kill feed assets" below). Everything else (the
`trident:icon` / `mcc:icon` / `mcc:hud` fonts) comes from the **resource pack the server sends** —
those keep their original `trident:`/`mcc:` namespaces.

- `KillFeed` — state + options (`enabled`, `maxKills`, `removeSeconds`, `rightSide`, `marginX`,
  `positionY`, `reverseOrder`, `showYouInKill`) + registration (HUD render + tick expiry).
- `KillFeedRenderer` — draws each entry: rounded team-coloured pills (Trident's exact fill math),
  the `crown:killfeed/left|right` chevron sprites tinted per side, inline player heads
  (`Component.object(new PlayerSprite(...))` — vanilla-native, same look as Noxesium skulls),
  `mcc:hud` name font, method glyph, streak/assist badges.
- `KillMethod` — maps each death type to its icon. **This is where you change which icon is which:**
  `tridentGlyph(0xE0NN)` = a `trident:icon` font codepoint; `mccIconByTexture("_fonts/*.png")`
  = an `mcc:icon` glyph looked up from the server pack's `mcc:font/icon.json`.
- `KillFeedAssets` — namespace helpers (`crown(...)` for the bundled chevrons; `trident(...)` /
  `mcc(...)` for server-pack assets), font ids (`TRIDENT_ICON_FONT`, `MCC_ICON_FONT`,
  `MCC_HUD_FONT`), the `mcc:icon` texture→char lookup, and the ARGB helper.
- `DeathMessages` + `KillFeedChatListener` — detection. Matches the server's skript death messages,
  normally prefixed `[💀] ` (skull = U+1F480): `[💀] <victim> <verb> [by <attacker>].`. Group 1 = victim,
  group 2 = attacker. The server's `playerFormattedName` wraps the username in a rank icon glyph, so
  `resolveName` strips it back to the bare username (prefers a tab-list match) for head/colour lookup.
  Self-explosion repeats the victim as attacker, so the listener nulls attacker == victim. The local
  chat path listens on `ClientReceiveMessageEvents.GAME`; both it and the plugin broadcast (below) feed
  the shared `KillFeedChatListener.handleText(String)`. In `DeathMessages` the `[💀] ` prefix and the
  trailing period are **optional**, so the same patterns match both the victim's chat line and the
  plain vanilla death text the plugin broadcasts.
- **Showing kills to everyone:** the server messages a death only to the victim's chat, so the victim's
  client is the only one that sees it via the chat path. To show the feed to all modded players, the
  CrownChampionshipUtils plugin relays each `PlayerDeathEvent` (its `getDeathMessage()` text) as a
  `killmsg <text>` body on the `crown:play` channel to every modded player **except the victim**
  (who already has it from chat). `PacketHandler` routes `killmsg` to `handleText`. `(YOU)` is computed
  per-client in `KillFeedRenderer` (`self.equals(name)`), so it lights up for the attacker or the victim
  on their own screen automatically. Caveat: spectators get the **vanilla** wording, so skript-only
  "while trying to escape X" kills lose the attacker for everyone but the victim.
- `KillFeedCommand` — client `/killfeed` command. Players may only run `/killfeed side` (flip which
  edge the feed sits on). Whether the feed shows at all is **server-controlled**, not a player toggle:
  the CrownChampionshipUtils plugin sends a `killfeed on|off` body on the `crown:play` channel, handled
  in `PacketHandler` (see `KillFeed.setEnabled`). This hides the feed for non-PvP games.

### Kill feed assets

Only the chevron transition sprites are bundled in this mod, under
`src/main/resources/assets/crown/textures/gui/sprites/killfeed/`:

| File | Resolves as |
|---|---|
| `left.png` | `crown:killfeed/left` (tinted with the attacker colour) |
| `right.png` | `crown:killfeed/right` (tinted with the victim colour) |

Both are drawn stretched to 8×15 px and colour-tinted. To use different transitions, just replace
these two PNGs (or repoint `CHEVRON_LEFT`/`CHEVRON_RIGHT` in `KillFeedRenderer`).

The streak badges are also bundled, under
`src/main/resources/assets/crown/textures/killfeed/streaks/` as `streak1.png`..`streak5.png` (each
13×9, drawn via `g.blit` not the sprite atlas — `drawStreakBadge` clamps the streak count to 1..5 and
shows them at streak ≥ 2). To swap them, replace those PNGs (or repoint the `KillFeedAssets.crown(...)`
path in `drawStreakBadge`).

Everything else — the `trident:icon` / `mcc:icon` / `mcc:hud` fonts and their bitmaps — must be
supplied by the **resource pack the server sends**; the mod does not ship them. Player heads/team
colours resolve from the tab list, so they only appear for players currently online.

## Resource-pack failure reporter (`com.github.NGoedix.videoplayer.packlog`)

When a server-sent resource pack fails on a client, this uploads the tail of `logs/latest.log` to
[mclo.gs](https://api.mclo.gs/) and posts a clickable + copyable link in chat, so server admins can
diagnose why a player's pack didn't apply.

- `ServerboundResourcePackPacketMixin` (`mixin/`) — injects into the canonical
  `ServerboundResourcePackPacket(UUID, Action)` constructor (`@At("TAIL")`). That packet is built
  only when the **client reports a status to the server**, so it catches failures without touching
  the async download internals, and never fires when decoding incoming packets. Client-only mixin.
- `PackFailureReporter` — `onStatus(id, action)` fires for `FAILED_DOWNLOAD`, `FAILED_RELOAD`
  ("failed to load") and `INVALID_URL`; other statuses are ignored. De-dupes per pack id within 30 s,
  uploads off-thread, and queues the chat `Component`. Pack negotiation can happen in the
  configuration phase (no chat / no player yet), so messages are flushed on `END_CLIENT_TICK` once
  `mc.player` and `mc.gui` exist. `enabled` toggles the whole thing.
- `McLogsUploader` — `POST https://api.mclo.gs/1/log` with form body `content=<log>` (Java 21
  `HttpClient`), parses the JSON `url`. mclo.gs auto-scrubs IPs / tokens / home paths. Only the last
  ~1 MB of the log is sent (the failure context is always at the tail).

Chat link uses the post-1.21.5 record events: `new ClickEvent.OpenUrl(URI)` for the link and
`new ClickEvent.CopyToClipboard(String)` for the `[copy]` suffix, with `HoverEvent.ShowText`.

## Conventions

- Match the surrounding code style; the original code uses Mojmap-style names with the `Identifier`
  rename noted above.
- New client-only code goes under `client/` or a feature package and must be reachable from
  `ClientHandler.onInitializeClient()`.
- Prefer runtime options/commands over hard-coded constants for anything a user might want to tune.
