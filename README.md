# Crown Championship Utilities

The official client mod for the **Crown Championship** server, on **Minecraft 1.21.11** (Fabric).
Install it to get the full Crown Championship experience: in‑world video screens and a clean,
team‑coloured **kill feed HUD** during PvP games.

---

## ✨ What it adds

### 💀 Kill feed HUD
- A polished, team‑coloured kill feed showing **attacker → method → victim**, with inline player
  heads, kill‑method icons, and kill‑streak / assist badges.
- Pill backgrounds use each player's **team colour**; `(YOU)` highlights your own kills and deaths.
- Shows the same kills for **everyone in the game**, and the Crown Championship server hides it
  automatically during non‑PvP rounds.

### 📺 Video screens
- Lets the Crown Championship server play videos and live streams on your screen during events
  (intros, trailers, lobby content), powered by the WaterMedia / VLC backend.

### 🧾 Resource‑pack failure reporter
- If the Crown Championship resource pack fails to apply, the mod uploads the relevant log tail to
  [mclo.gs](https://mclo.gs/) and posts a clickable + copyable link in chat, so staff can quickly
  work out why your pack didn't load.

---

## 📦 Requirements

| Dependency | Version | Notes |
|---|---|---|
| **Fabric Loader** | ≥ 0.16.0 | |
| **Fabric API** | for 1.21.11 | Required |
| **Java** | 21+ | |
| **WaterMedia** | 2.1.37 | **Optional** — only needed for the video/radio features. Without it the mod still loads and the kill feed works; video just stays disabled. |

`java-youtube-downloader` is bundled inside the mod jar, so there's nothing extra to download.

> If you only want the kill feed, you can skip WaterMedia entirely. Install it (2.1.37) only if you
> want in‑world video/radio playback.

---

## 🚀 Installation

1. Install **Fabric Loader** for Minecraft 1.21.11.
2. Download **Fabric API** (required). Optionally also grab **WaterMedia 2.1.37** if you want the
   video/radio features.
3. Drop those jars **plus this mod's jar** into your `mods` folder.
4. Launch the game and join the Crown Championship server — accept the server resource pack when
   prompted (the kill feed fonts and icons come from it).

---

## 🎮 Commands

The only command you'll use as a player:

```
/killfeed side    # flip the kill feed to the left or right edge of your screen
```

Everything else (whether the feed shows, which videos play) is handled by the Crown Championship
server.

### Video controls (when a video allows them)
| Action | Keys |
|---|---|
| Close video | `Shift` + `Esc` |
| Forward / Backward | `Shift` + `→` / `Shift` + `←` |
| Pause | `Shift` + `Space` |
| Mute | `M` |
| Volume up / down | `↑` / `↓` |

---

## ❓ FAQ

**The kill feed icons / player heads don't show.**
Make sure you accepted the Crown Championship resource pack — the kill‑method fonts and name fonts
are supplied by it. Heads and team colours only appear for players currently online.

**Can I use this on other servers?**
This mod is built specifically for the Crown Championship server. The kill feed relies on the
server's death messages, resource pack, and companion plugin, so it won't do anything meaningful
elsewhere.

---

## 🙏 Credits & license

- Author: **cbop**
- Video/audio backend: **WaterMedia** by the WaterMedia Team.
- Kill feed look inspired by the Trident mod, reimplemented for vanilla `GuiGraphics`.

Released under **CC0‑1.0**.
