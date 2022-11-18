package au.com.addstar.slackcontrol;

import com.slack.api.Slack;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.model.Attachment;
import com.slack.api.model.event.AppMentionEvent;
import com.slack.api.model.event.MessageEvent;
import com.slack.api.model.event.MessageMeEvent;
import com.slack.api.socket_mode.SocketModeClient;

import java.io.IOException;
import java.util.List;

public class SlackApp {
    SlackControl plugin;
    Config config;
    AppConfig appConfig;
    App app;
    MethodsClient methods;

    public SlackApp(SlackControl plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public MethodsClient getMethodsClient() {
        return methods;
    }

    public boolean initApp() throws Exception {
        if (config.slack_bot_token.isEmpty()) {
            plugin.getLogger().warning("Slack bot_token has not been set");
            return false;
        }

        appConfig = AppConfig.builder().singleTeamBotToken(config.slack_bot_token).build();
        app = new App(appConfig);
        methods = Slack.getInstance().methods(config.slack_bot_token);

        app.command("/mcbot", (req, ctx) -> {
            plugin.getLogger().info("Received command"
                    + " from " + req.getPayload().getUserName()
                    + " in #" + req.getPayload().getChannelName()
                    + ": " + req.getPayload().getCommand()
                    + " " + req.getPayload().getText()
            );
            return ctx.ack(":wave: Hello! This is from the SlackControl plugin :awesome:");
        });

        app.event(AppMentionEvent.class, (req, ctx) -> {
            plugin.getLogger().info("Received AppMentionEvent"
                    + " from " + req.getEvent().getUser()
                    + " in " + req.getEvent().getChannel()
                    + ": " + req.getEvent().getText()
            );
            ctx.say("Yo, why you pinging me? :roll_eyes:");
            return ctx.ack();
        });

        app.event(MessageMeEvent.class, (req, ctx) -> {
            plugin.getLogger().info("Received MessageMeEvent"
                    + " from " + req.getEvent().getUsername()
                    + " in " + req.getEvent().getChannel()
                    + ": " + req.getEvent().getText()
            );

            ctx.say("Yo, why you talking to me? :roll_eyes:");
            return ctx.ack();
        });

        // Initialize the adapter for Socket Mode
        // with an app-level token and your Bolt app with listeners.
        SocketModeApp socketModeApp = new SocketModeApp(
                config.slack_app_token,
                SocketModeClient.Backend.JavaWebSocket,
                app
        );
        socketModeApp.startAsync();

        app.event(MessageEvent.class, (req, ctx) -> {
            plugin.getLogger().info("Received MessageEvent"
                    + " from " + req.getEvent().getUser()
                    + " in " + req.getEvent().getChannel()
                    + ": " + req.getEvent().getText()
            );
            ctx.say("Yo, why you talking to me? :roll_eyes:");
            return ctx.ack();
        });

        return true;
    }

    public boolean sendChannelMessage(String channel, String text, List<Attachment> attachments) {
        plugin.getProxy().getScheduler().runAsync(plugin, new Runnable() {
            @Override
            public void run() {
                // Construct outgoing slack message
                ChatPostMessageRequest request = ChatPostMessageRequest.builder()
                        .channel(channel)
                        .text(text)
                        .attachments(attachments)
                        .build();

                // Attempt to send message to slack
                try {
                    ChatPostMessageResponse response = plugin.getMethodsClient().chatPostMessage(request);
                    if (!response.isOk()) {
                        plugin.getLogger().warning("Slack message error: " + response.getError());
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (SlackApiException e) {
                    throw new RuntimeException(e);
                }

            }
        });

        return true;
    }
}
