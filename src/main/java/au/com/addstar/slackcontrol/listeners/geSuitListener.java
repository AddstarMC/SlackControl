package au.com.addstar.slackcontrol.listeners;

import au.com.addstar.slackcontrol.Config;
import au.com.addstar.slackcontrol.SlackControl;
import com.slack.api.model.Attachment;
import com.slack.api.model.Field;
import net.cubespace.geSuit.events.BanPlayerEvent;
import net.cubespace.geSuit.events.UnbanPlayerEvent;
import net.cubespace.geSuit.events.WarnPlayerEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

public class geSuitListener implements Listener {
    SlackControl plugin;
    Config config;

    public geSuitListener(SlackControl plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    @EventHandler
    public void onBan(BanPlayerEvent event) {
        if (event.isAutomatic()) return;
        String title;
        String fallback;

        Attachment.AttachmentBuilder aBuilder = Attachment.builder();
        List<Attachment> attachments = new ArrayList<>();
        List<Field> fields = new ArrayList<>();
        fields.add(Field.builder()
                .title("Reason")
                .value(event.getReason())
                .build());

        switch (event.getType()) {
            default:
            case Name:
                title = String.format(":no_entry: *Ban:* `%s` has been banned by %s", event.getPlayerName(), event.getBannedBy());
                fallback = String.format("BanNotice: %s has been banned by %s", event.getPlayerName(), event.getBannedBy());
                break;
            case IP:
                title = String.format(":no_entry: *IPBan:* `%s` has been IP banned by %s", event.getPlayerName(), event.getBannedBy());
                fallback = String.format("BanNotice: %s (%s) has been IP banned by %s", event.getPlayerName(), event.getPlayerIP().getHostAddress(), event.getBannedBy());
                fields.add(Field.builder()
                        .title("IP")
                        .value(event.getPlayerIP().getHostAddress())
                        .build());
                break;
            case Temporary:
                title = String.format(":no_entry: *TempBan:* `%s` has been temp banned by %s", event.getPlayerName(), event.getBannedBy());
                fallback = String.format("BanNotice: %s has been temp banned by %s until %s", event.getPlayerName(), event.getBannedBy(),
                        DateFormat.getDateTimeInstance().format(event.getUnbanDate()));
                fields.add(Field.builder()
                        .title("Until")
                        .value(DateFormat.getDateTimeInstance().format(event.getUnbanDate()))
                        .build());
                break;
        }

        aBuilder.color("#FF0000");
        aBuilder.fallback(fallback);
        aBuilder.fields(fields);
        attachments.add(aBuilder.build());
        plugin.getSlackApp().sendChannelMessage(config.getSlackWarnChannel(), title, attachments);
    }

    @EventHandler
    public void onWarn(WarnPlayerEvent event) {
        Attachment.AttachmentBuilder aBuilder = Attachment.builder();
        List<Attachment> attachments = new ArrayList<>();
        List<Field> fields = new ArrayList<>();
        fields.add(Field.builder()
                .title("Reason")
                .value(event.getReason())
                .build());

        String title = String.format(":warning: *Warning:* `%s` has been warned by %s", event.getPlayerName(), event.getBy());
        String fallback = String.format("WarnNotice: %s was been warned by %s for %s", event.getPlayerName(), event.getBy(), event.getReason());
        if (event.getActionExtra().isEmpty())
            fields.add(Field.builder()
                    .title("Action")
                    .value(String.format("#%d - %s", event.getWarnCount(), event.getAction()))
                    .build());
        else
            fields.add(Field.builder()
                    .title("Action")
                    .value(String.format("#%d - %s %s", event.getWarnCount(), event.getAction(), event.getActionExtra()))
                    .build());

        aBuilder.color("#F2BF50");
        aBuilder.fallback(fallback);
        aBuilder.fields(fields);
        attachments.add(aBuilder.build());
        plugin.getSlackApp().sendChannelMessage(config.getSlackWarnChannel(), title, attachments);
    }

    @EventHandler
    public void onUnban(UnbanPlayerEvent event) {
        Attachment.AttachmentBuilder aBuilder = Attachment.builder();
        List<Attachment> attachments = new ArrayList<>();
        List<Field> fields = new ArrayList<>();

        String title = String.format(":white_check_mark: *Unban:* `%s` has been unbanned by %s", event.getPlayerName(), event.getBannedBy());
        String fallback = String.format("UnbanNotice: %s has been unbanned by %s", event.getPlayerName(), event.getBannedBy());

        aBuilder.color("#00FF00");
        aBuilder.fallback(fallback);
        aBuilder.fields(fields);
        attachments.add(aBuilder.build());
        plugin.getSlackApp().sendChannelMessage(config.getSlackWarnChannel(), title, attachments);
    }
}
