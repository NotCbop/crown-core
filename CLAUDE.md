# CLAUDE.md

Guidance for working in this repository.

## Project

**CrownCore** (formerly "Crown Championship Utilities") — a Fabric client/server mod for
**Minecraft 1.21.11** (Java 21).
It started as NGoedix's *Video Player* mod, but **all video/music playback, the TV/radio blocks
and the WaterMedia dependency have been removed** (2026-10-31) — what remains is the Crown
utilities: kill feed HUD, game loading screens (with grayscale background clips, decoded in pure
Java by JCodec — not WaterMedia), F1 block, resource-pack handling, version handshake. The mod
registers **no blocks, items or commands server-side**; the jar is ~4.3 MB (JCodec ~2 MB + the
bundled clips ~2 MB).

- Mod id is still `videoplayer` and the base package is `com.github.NGoedix.videoplayer`
  (the rename to "CrownCore" is **display-name only** — see `fabric.mod.json` and the init log
  line). Do not rename the id or package. Mod assets live under the `crown` namespace.

## Build & run

```powershell
.\gradlew.bat compileJava        # fast compile check (use --offline if deps are cached)
.\gradlew.bat build              # full build -> build\libs\ (main jar = the one without -sources)
.\gradlew.bat runClient          # launch a dev client; use the IDE Debug config for hot-swap
```

Runtime mods needed alongside this one: **Fabric API** (required). Nothing else — there is
**no WaterMedia, no Kotlin and no Noxesium** dependency. The only library is **JCodec 0.2.5**
(pure-Java H.264 decoder, no natives), jar-in-jar'd via `include` in `build.gradle`.

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
expected. There is no access widener.

## Layout

- `VideoPlayer.java` — `ModInitializer`: only registers the `crown:play` payload type (both sides).
- `client/ClientHandler.java` — `ClientModInitializer`; wires up everything below.
- `network/PacketHandler.java` + `network/packet/PlayBroadcastMessage.java` — the `crown:play` channel.
- `killfeed/` — the kill feed (see below).
- `loading/` — the game loading screen (see below).
- `client/CrownServer.java`, `client/HideUiBlocker.java` — Crown-server detection, F1 block.
- `packlog/` + `mixin/` — resource-pack handling and the packet-error fix (see below).

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
- `KillFeedCommand` — client `/killfeed` command: `/killfeed on|off` is the player's own show/hide
  preference (`KillFeed.userEnabled`, persisted to `config/crown-killfeed.properties` by
  `KillFeedToggle`), `/killfeed side` flips which edge the feed sits on. The server *also* has its
  own switch (`KillFeed.enabled`): the CrownChampionshipUtils plugin sends a `killfeed on|off` body
  on the `crown:play` channel, handled in `PacketHandler` (see `KillFeed.setEnabled`), to hide the
  feed for non-PvP games. The feed renders only when BOTH are on (`KillFeed.shouldShow()`).

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

## Loading screen (`com.github.NGoedix.videoplayer.loading`)

`LoadingScreen` is Crown's animated game-loading overlay. Its textures are under
`assets/crown/textures/loading/` and come from the Celestesium mod (MIT), with the background and
glow recolored from purple to blue: `background.png` (1024x576, cover-fit with a slow breathing
zoom), `glow.png` (256, pulsing blue halo behind the icon), `games/<game>.png` (the big center
icon) and `side/<game>.png` (grey icons drifting in two opposite-scrolling columns along the screen
edges; the list repeats down the whole column so there's never a gap). The rest is procedural: the
icon's easeOutBack pop + bob/sway, the title/subtitle fades, and a **thin rounded yellow progress line** (3 gui px)
(`pill()` fills rows at half-gui-pixel resolution so the caps stay round; faint blue edge, soft yellow glow + shimmer
sweep). If the game is unknown or missing, the 16x16 pixel-art Crown crown (`crown.png`, drawn with
the same pop/bob/sway) takes the icon's place. The game table (aliases like `skb`/`sky_battle`,
display names, icon sizes) lives in its static block. Registered via `HudRenderCallback` **after**
the kill feed so it draws on top; cleared on disconnect.

**Background clips** (`LoadingClips` + `ClipPlayer`): the game's mp4s under
`assets/crown/loading_clips/<game>/` (a game without a folder plays `hub/`) cycle behind the
overlay — shuffled, 5 s each, 1.2 s crossfades, max two alive — tinted gray-blue and darkened by
`drawClipShade`. Each `ClipPlayer` decodes on its own daemon thread with JCodec at 30 fps, reads
only the luma plane (clips are stored grayscale), and the render thread uploads the newest frame
into a linear-filtered `DynamicTexture`. JCodec only handles **H.264 baseline**, so always add
clips through `tools/encode-loading-clips.ps1` (needs ffmpeg): put raw clips in
`loading-clips-src/<game>/` (gitignored) and run
`powershell -ExecutionPolicy Bypass -File tools\encode-loading-clips.ps1` — it outputs 640x360,
30 fps, grayscale, baseline, first 10 s, ~250-500 KB each. Clips come through the resource
manager, so a resource pack can add/replace them too.

Everything that moves is placed with **float pose transforms** (translate/scale), never
`Math.round`ed blit coordinates — rounding made the side icons twitch a pixel back and forth. Clock
math is done in `double` (`now * 0.018f` in float has no precision at epoch-millis magnitudes).

Driven by the plugin over `crown:play`: `loadscreen;<seconds 1-120>;<title>;<subtitle>;<game>`
shows it (a blank title falls back to the game's name; the bar fills across the duration, then it
fades out **on its own** — a missed "off" can't trap anyone), `loadscreen off` dismisses early.
Plugin command: `/loadscreen <target> <seconds> <game|none> [title][|<subtitle>]` or
`/loadscreen <target> off` (also a `/crown` subcommand); `|` splits title/subtitle, `;` is
stripped (it's the body separator).

## Crown-server detection (`client/CrownServer.java`)

The kill feed only works on servers running the CrownChampionshipUtils plugin. `CrownServer`
flips `isPresent()` when Paper's `minecraft:register` announces the `crown:play` channel
(`C2SPlayChannelEvents.REGISTER`), excluding the integrated/singleplayer server (this mod's own
server side also announces that channel), and as a fallback when any `crown:play` payload actually
arrives (`markPresent()` in `PacketHandler`). Reset on disconnect. `KillFeed.shouldShow()` includes
it, so the chat-parsing path can't produce a feed from similar death messages on other servers.
The other Crown features (loading screen, F1 block) don't need the gate — they only ever fire from
`crown:play` payloads the plugin sends.

## Server resource packs & version handshake

- `mixin/ServerDataMixin` forces `ServerData.getResourcePackStatus()` to `ENABLED` for **every**
  server, so server-sent packs are always auto-accepted (no prompt, can never be declined). The
  Crown pack carries the kill feed fonts, so a decline would silently break the HUD.
- `mixin/PacketErrorMixin` cancels `ClientCommonPacketListenerImpl#onPacketError` (logs the bad
  packet instead), so a malformed server packet never triggers the "Network Protocol Error"
  disconnect — a reimplementation of Mobilelize's Network-Protocol-Disconnect mod.
- On play join, `ClientHandler` sends `version;<mod version>` (read from `fabric.mod.json`) C2S on
  `crown:play`. The CrownChampionshipUtils plugin compares it against config `mod-version.required`
  and warns (clickable `download-url`) or kicks (`action: warn|kick`) outdated players. Modded
  clients that register the channel but never report within 5s are treated as pre-handshake builds;
  vanilla players are ignored. Empty `required` disables the check. `/crown list` shows each
  player's reported version.

## Cheat client reporting (`anticheat/AntiCheatScanner.java`)

The plugin sends `acscan;<probe|probe...>` on join and every few minutes. The mod replies with
`acreport;<section>;<a|b|c>` per section (`mods`, `keys`, `found`, `agents`, `libs`), then
`acreport;done`. The mod only gathers facts: **signatures live in the plugin's `anticheat:` config**,
so they change without a mod update and can't be read from the mod jar. It stays inside the game
process (no process list, no disk scanning) and sends file names only, never paths. A modified
client can lie, so a clean report proves nothing. Behaviour cheats (kill aura, fly) need a
server-side anticheat. `ALLOWED_SERVER_HOSTS` limits which servers may request a scan (the address
typed into the client must match), so **add any test server's address there** or it reports "no scan reply".

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
