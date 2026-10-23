# SkyChat

Channel chat for the **SkyMinigames** server (Spigot/Paper 1.8.8, Java 8), with the same features as Legendchat.

## Features
- **Channels** (one file each in `plugins/SkyChat/channels`, same keys as Legendchat): format, colour, distance,
  cross-world, delay per message, cost per message (Vault economy), shortcut command, "need focus"
  - `local` — just type (60 blocks) · `global` — `/g` · `staff` — `/sc`
- `/ch` list channels · `/ch <canal>` talk in a channel by default · `/ch mute <canal>` stop seeing a channel
- **Format tags**: `{prefix} {suffix} {groupprefix} {groupsuffix} {sender} {plainsender} {world} {name} {nick} {color}
  {msg} {time_hour} {time_min} {time_sec} {date_day} {date_month} {date_year}` (prefixes from PermissionsEx via Vault)
- **Private messages**: `/tell <jogador> <msg>` (aliases msg, pm, w, whisper, t, m), `/r`, and `/tell <jogador>` alone
  to keep talking privately with someone
- `/ignore <jogador>`, `/afk [motivo]` (shown to whoever sends you a private message)
- **Moderation**: `/mute <jogador> [minutos]`, `/unmute`, `/skychat muteall|unmuteall`, `/skychat spy` (distant channel
  messages and private messages), `/skychat hide`, word censor
- **Colours** in messages by permission, **blank lines** around messages of players with `skychat.spacing`
- `SkyChatMessageEvent` for other plugins

## Permissions
| Permission | What it allows |
|---|---|
| `skychat.channel.<canal>.chat` | read and write in a channel |
| `skychat.channel.<canal>.focus` | `/ch <canal>` |
| `skychat.channel.<canal>.nodelay` / `.free` | skip the delay / the cost |
| `skychat.tell`, `skychat.ignore`, `skychat.afk` | private messages, ignore, AFK (default: everyone) |
| `skychat.color.allcolors` / `allformats` / `<name>` | colours (`&a`) / formats (`&l`) / one code (`red`, `bold`) |
| `skychat.spacing` | blank line above and below your channel messages |
| `skychat.block.ignore` / `skychat.block.mute` | can't be ignored / muted |
| `skychat.admin.<reload/spy/hide/mute/unmute/muteall/unmuteall>`, `skychat.admin` | admin commands |

## Build
```bash
mvn clean package
```
In VS Code, run **Deploy to server** (stop the server first).
