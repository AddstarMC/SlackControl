# SlackControl

Velocity proxy plugin to interact with Slack. Supports slash commands, DMs, and app mentions; can post ban/warn notifications when used with geSuit (e.g. via Snap).

## Requirements

- **Velocity** 3.4.x (API 3.4.0-SNAPSHOT)
- **Java** 21+
- **ValioBungee** (RedisBungee-Velocity) — optional; when present, enables geSuit ban/warn/unban notifications over Redis

## Build

```bash
mvn clean package
```

Build with JDK 21 (required):

```bash
JAVA_HOME=/path/to/jdk21 mvn clean package
```

Output: `target/SlackControl-2.0.<buildNumber>-SNAPSHOT.jar` (e.g. `2.0.0-SNAPSHOT` locally).

**Jenkins:** The build number is taken from the `BUILD_NUMBER` environment variable when set (no `-D` needed). Use goals `clean package` or `clean install` (not `build`). Optional: pass `-DbuildNumber=$BUILD_NUMBER` if your job doesn’t expose env to Maven.

## Install

1. Copy the built JAR into Velocity’s `plugins/` folder.
2. Restart Velocity (or load the plugin).
3. Edit `plugins/slackcontrol/config.yml` and set your Slack `bot_token`, `app_token`, and `warn_channel` (e.g. `#warns-bans`).

## Config

See `config.yml` (created on first run):

- `slack.bot_token` — Bot token (xoxb-…)
- `slack.app_token` — App-level token for Socket Mode (xapp-…)
- `slack.warn_channel` — Channel for ban/warn notifications (e.g. `#warns-bans`)
- `debug` — Toggle debug logging (or use in-game: `slackcontrol debug`)

## Commands

- **In-game:** `slackcontrol` (requires `slackcontrol.command`) — e.g. `slackcontrol debug`, `slackcontrol status`
- **Slack:** Use `/mcbot` or DM/mention the app; supported: `who`, `help`, etc.

## geSuit integration (Redis / ValioBungee)

SlackControl subscribes to **ValioBungee** Redis PubSub channels and posts to the configured Slack `warn_channel` when it receives messages. No direct geSuit plugin dependency; geSuit (or a bridge) publishes to these channels.

**Channels:** `gesuit:ban`, `gesuit:warn`, `gesuit:unban`

**Payload (JSON, minimal):** All events must include:

- `actionBy` — staff who performed the action, or `CONSOLE`
- `targetUsername` — target player username
- `targetUuid` — target player UUID (string)

**Optional per event:**

- **gesuit:ban:** `reason`, `type` (`name`|`ip`|`temp`), `until` (ISO date for temp), `ip`
- **gesuit:warn:** `reason`, `warnCount`, `action`, `actionExtra`
- **gesuit:unban:** only the common fields above

Publish via ValioBungee’s API: `RedisBungeeAPI.getRedisBungeeApi().sendChannelMessage("gesuit:ban", jsonString)` (and similarly for Bungee when geSuit runs there).
