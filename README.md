# Crown Core

The official client mod for the **Crown Championship** server, built for **Minecraft 1.21.11** using Fabric.

Install Crown Core to get the full Crown Championship experience, including a team-coloured **kill feed HUD**, in-game video screens, and resource-pack error reporting.

---

## ✨ What it adds

### 💀 Kill feed HUD

* A polished, team-coloured kill feed showing **attacker → method → victim**, with inline player heads, kill-method icons, and kill-streak or assist badges.

### 📺 Video screens

* Allows the Crown Championship server to play videos during events, including game intros, trailers, announcements, and lobby content.

### 🧾 Resource-pack failure reporter

* If the Crown Championship resource pack fails to apply, the mod uploads the relevant section of your log to mclo.gs.
* A clickable and copyable link is then posted in chat, allowing staff to quickly determine why the resource pack failed to load.

---

## 📦 Requirements

| Dependency        |               Version | Notes    |
| ----------------- | --------------------: | -------- |
| **Fabric Loader** |              ≥ 0.16.0 | Required |
| **Fabric API**    | For Minecraft 1.21.11 | Required |

---

## 🚀 Installation

1. Install **Fabric Loader** for Minecraft 1.21.11.
2. Download **Fabric API** for Minecraft 1.21.11.
3. Place both the Fabric API jar and the Crown Core jar inside your Minecraft `mods` folder.
4. Launch the game and join the Crown Championship server.
5. Accept the server resource pack when prompted. The kill feed fonts, icons, and other assets are provided through the resource pack.

---

## 🎮 Commands

The primary player command is:

```text
/killfeed side
```

This moves the kill feed between the left and right sides of your screen.

Everything else, including kill feed visibility and video playback, is controlled by the Crown Championship server.

---

## ❓ FAQ

### The kill feed icons or player heads do not appear.

Make sure you accepted the Crown Championship resource pack. The kill-method icons and custom fonts are supplied by it.

Player heads and team colours may only appear correctly for players who are currently online and participating in the event.

### Can I use this mod on other servers?

Crown Core is built specifically for the Crown Championship server. Features such as the kill feed, video system, and resource-pack integration rely on the server's companion plugin and infrastructure, so the mod will not do anything meaningful on most other servers.

---

## 🙏 Credits and license

* Original **Video Player** mod by **NGoedix**: https://github.com/NGoedix/WatchVideoMod
* Kill feed design inspired by the **[Trident](https://modrinth.com/mod/trident-mcci)** mod and reimplemented using vanilla `GuiGraphics`

---

## ⚠️ Development notice

Some features in this mod were created with the assistance of AI while I continue learning Java. The code is still reviewed, tested, and maintained for use on the Crown Championship server.

Released under the **CC0-1.0** license.
