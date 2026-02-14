package au.com.addstar.slackcontrol.listeners;

import au.com.addstar.slackcontrol.Config;
import au.com.addstar.slackcontrol.SlackControl;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.slack.api.model.Attachment;
import com.slack.api.model.Field;
import com.velocitypowered.api.event.Subscribe;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles geSuit events received via ValioBungee Redis PubSub.
 * Channels: gesuit:ban, gesuit:warn, gesuit:unban
 *
 * Expected JSON payload (minimal; geSuit may send more):
 * - All: "actionBy" (String), "targetUsername" (String), "targetUuid" (String)
 * - gesuit:ban: optional "reason", "type" ("name"|"ip"|"temp"), "until" (ISO), "ip"
 * - gesuit:warn: optional "reason", "warnCount", "action", "actionExtra"
 * - gesuit:unban: only the common fields
 */
public class GesuitRedisHandler {

    private static final String CHANNEL_BAN = "gesuit:ban";
    private static final String CHANNEL_WARN = "gesuit:warn";
    private static final String CHANNEL_UNBAN = "gesuit:unban";

    private final SlackControl plugin;
    private final Config config;

    public GesuitRedisHandler(SlackControl plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    @Subscribe
    public void onPubSubMessage(com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent event) {
        String channel = event.getChannel();
        String message = event.getMessage();
        if (message == null || message.isEmpty()) return;

        JsonObject json;
        try {
            json = JsonParser.parseString(message).getAsJsonObject();
        } catch (JsonParseException e) {
            plugin.warnMsg("gesuit Redis: invalid JSON on " + channel + ": " + e.getMessage());
            return;
        }

        String actionBy = getString(json, "actionBy");
        String targetUsername = getString(json, "targetUsername");
        String targetUuid = getString(json, "targetUuid");
        if (actionBy == null || targetUsername == null || targetUuid == null) {
            plugin.warnMsg("gesuit Redis: missing actionBy/targetUsername/targetUuid on " + channel);
            return;
        }

        switch (channel) {
            case CHANNEL_BAN -> handleBan(json, targetUsername, actionBy);
            case CHANNEL_WARN -> handleWarn(json, targetUsername, actionBy);
            case CHANNEL_UNBAN -> handleUnban(targetUsername, actionBy);
            default -> { }
        }
    }

    private static String getString(JsonObject json, String key) {
        if (!json.has(key)) return null;
        try {
            return json.get(key).getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    private void handleBan(JsonObject json, String targetUsername, String actionBy) {
        String reason = getString(json, "reason");
        String type = getString(json, "type");
        String until = getString(json, "until");
        String ip = getString(json, "ip");

        List<Field> fields = new ArrayList<>();
        if (reason != null && !reason.isEmpty()) {
            fields.add(Field.builder().title("Reason").value(reason).build());
        }
        if ("ip".equalsIgnoreCase(type) && ip != null) {
            fields.add(Field.builder().title("IP").value(ip).build());
        }
        if ("temp".equalsIgnoreCase(type) && until != null) {
            fields.add(Field.builder().title("Until").value(until).build());
        }

        String title;
        String fallback;
        if ("ip".equalsIgnoreCase(type)) {
            title = String.format(":no_entry: *IPBan:* `%s` has been IP banned by %s", targetUsername, actionBy);
            fallback = String.format("BanNotice: %s has been IP banned by %s", targetUsername, actionBy);
        } else if ("temp".equalsIgnoreCase(type)) {
            title = String.format(":no_entry: *TempBan:* `%s` has been temp banned by %s", targetUsername, actionBy);
            fallback = String.format("BanNotice: %s has been temp banned by %s", targetUsername, actionBy);
        } else {
            title = String.format(":no_entry: *Ban:* `%s` has been banned by %s", targetUsername, actionBy);
            fallback = String.format("BanNotice: %s has been banned by %s", targetUsername, actionBy);
        }

        Attachment attachment = Attachment.builder()
            .color("#FF0000")
            .fallback(fallback)
            .fields(fields)
            .build();
        plugin.getSlackApp().sendChannelMessage(config.getSlackWarnChannel(), title, List.of(attachment));
    }

    private void handleWarn(JsonObject json, String targetUsername, String actionBy) {
        String reason = getString(json, "reason");
        String warnCount = json.has("warnCount") ? json.get("warnCount").getAsString() : null;
        String action = getString(json, "action");
        String actionExtra = getString(json, "actionExtra");

        List<Field> fields = new ArrayList<>();
        if (reason != null && !reason.isEmpty()) {
            fields.add(Field.builder().title("Reason").value(reason).build());
        }
        if (warnCount != null || action != null) {
            String actionVal = (actionExtra != null && !actionExtra.isEmpty())
                ? String.format("#%s - %s %s", warnCount != null ? warnCount : "?", action != null ? action : "", actionExtra)
                : String.format("#%s - %s", warnCount != null ? warnCount : "?", action != null ? action : "");
            fields.add(Field.builder().title("Action").value(actionVal).build());
        }

        String title = String.format(":warning: *Warning:* `%s` has been warned by %s", targetUsername, actionBy);
        String fallback = String.format("WarnNotice: %s was warned by %s", targetUsername, actionBy);

        Attachment attachment = Attachment.builder()
            .color("#F2BF50")
            .fallback(fallback)
            .fields(fields)
            .build();
        plugin.getSlackApp().sendChannelMessage(config.getSlackWarnChannel(), title, List.of(attachment));
    }

    private void handleUnban(String targetUsername, String actionBy) {
        String title = String.format(":white_check_mark: *Unban:* `%s` has been unbanned by %s", targetUsername, actionBy);
        String fallback = String.format("UnbanNotice: %s has been unbanned by %s", targetUsername, actionBy);

        Attachment attachment = Attachment.builder()
            .color("#00FF00")
            .fallback(fallback)
            .fields(List.of())
            .build();
        plugin.getSlackApp().sendChannelMessage(config.getSlackWarnChannel(), title, List.of(attachment));
    }
}
