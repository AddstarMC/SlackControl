package au.com.addstar.slackcontrol;

import au.com.addstar.slackcontrol.commands.HelpCommand;
import au.com.addstar.slackcontrol.commands.ISlackCommandHandler;
import au.com.addstar.slackcontrol.commands.WhoCommand;
import au.com.addstar.slackcontrol.objects.BotResponse;
import au.com.addstar.slackcontrol.objects.UserCommand;
import com.slack.api.Slack;
import com.slack.api.app_backend.events.payload.EventsApiPayload;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.context.builtin.EventContext;
import com.slack.api.bolt.context.builtin.SlashCommandContext;
import com.slack.api.bolt.model.Bot;
import com.slack.api.bolt.request.builtin.SlashCommandRequest;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.model.Attachment;
import com.slack.api.model.event.AppMentionEvent;
import com.slack.api.model.event.MessageEvent;
import com.slack.api.socket_mode.SocketModeClient;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

public class SlackApp {
    SlackControl plugin;
    Config config;
    AppConfig appConfig;
    App app;
    MethodsClient methods;
    Map<String, ISlackCommandHandler> slackCommandHandlers;

    public SlackApp(SlackControl plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        slackCommandHandlers = new HashMap<>();
    }

    // Register all handlers for incoming commands from Slack
    public boolean registerHandlers() {
        unregisterHandlers();
        registerHandler(new WhoCommand(plugin), "who", "list", "players", "online", "glist");
        registerHandler(new HelpCommand(plugin), "help", "whatcanyoudo", "helpme", "?");
        return true;
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

        // Register all configured command handlers
        registerHandlers();

        // Using slash command (/mcbot)
        app.command("/mcbot", (req, ctx) -> {
            plugin.getLogger().info("Received command"
                    + " from " + req.getPayload().getUserName()
                    + " in #" + req.getPayload().getChannelName()
                    + ": " + req.getPayload().getCommand()
                    + " " + req.getPayload().getText()
            );
            UserCommand cmd = extractCommandFromMsg(req.getPayload().getText());
            handleCommand(cmd, req.getPayload().getUserId(), ctx);
            return ctx.ack();
        });

        // Message sent via DM to the bot
        // https://api.slack.com/events/message
        app.event(MessageEvent.class, (req, ctx) -> {
            plugin.getLogger().info("Received MessageEvent"
                    + " from " + req.getEvent().getParentUserId()
                    + " in " + req.getEvent().getChannel()
                    + ": " + req.getEvent().getText()
            );
            UserCommand cmd = extractCommandFromMsg(req.getEvent().getText());
            handleCommand(cmd, req.getEvent().getUser(), ctx, true);
            return ctx.ack();
        });

        // Mentioning the bot in a channel
        // https://api.slack.com/events/app_mention
        app.event(AppMentionEvent.class, (req, ctx) -> {
            plugin.getLogger().info("Received AppMentionEvent"
                    + " from " + req.getEvent().getUser()
                    + " in " + req.getEvent().getChannel()
                    + ": " + req.getEvent().getText()
            );
            UserCommand cmd = extractCommandFromMsg(req.getEvent().getText());
            handleCommand(cmd, req.getEvent().getUser(), ctx, false);
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

        return true;
    }

    // Register an individual handler (can have multiple commands for a single handler)
    private boolean registerHandler(ISlackCommandHandler handler, @NotNull String... commands) {
        for (String command : commands)
            slackCommandHandlers.put(command.toLowerCase(), handler);
        return true;
    }

    // Unregister all handlers
    // For now this just clears the handler list and lets the GC do the rest
    public boolean unregisterHandlers() {
        slackCommandHandlers.clear();
        return true;
    }

    // Use this method to send a message of any content to any channel (from the bot)
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

    // Extracts the relevant command/arguments from any given message text to the bot
    private UserCommand extractCommandFromMsg(String fullmsg) {
        UserCommand result = new UserCommand();

        // Strip everything up to (and including) the bot ping
        // We only want anything after the @ mention
        plugin.getLogger().info("Old Message: \"" + fullmsg + "\"");
        fullmsg = fullmsg.replaceFirst("<@[0-9A-Z]*>[\\s]*", "")
                .replaceAll(" +", " ")
                .trim();
        plugin.getLogger().info("New Message: \"" + fullmsg + "\"");

        int pos = fullmsg.indexOf(" ");
        if (pos == -1) {
            result.setCmd(fullmsg.substring(0));
        } else {
            result.setCmd(fullmsg.substring(0, pos));
            result.setArgs(fullmsg.substring(pos+1));
        }

        plugin.getLogger().info("Command  : " + result.getCmd());
        plugin.getLogger().info("Arguments: " + result.getArgs());
        return result;
    }

    // Return the command handler for any given UserCommand
    private ISlackCommandHandler getCmdHandler(UserCommand cmd) {
        return slackCommandHandlers.getOrDefault(cmd.getCmd(), null);
    }

    private void handleCommand(UserCommand cmd, String user, SlashCommandContext ctx) {
        BotResponse resp = handleCommand(cmd, user);
        try {
            ctx.respond(resp.text);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private void handleCommand(UserCommand cmd, String user, EventContext ctx, boolean direct) {
        BotResponse resp = handleCommand(cmd, user);
        try {
            if (direct) {
                ctx.say(resp.text);
            } else {
                ctx.say("<@"+user+"> " + resp.text);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SlackApiException e) {
            throw new RuntimeException(e);
        }
    }

    private BotResponse handleCommand(UserCommand cmd, String user) {
        ISlackCommandHandler handler = getCmdHandler(cmd);
        if (handler != null) {
            BotResponse resp = handler.commandHandler(user, cmd);
            if (resp == null) {
                return new BotResponse("Sorry, the command handling failed. Try again later.");
            }
            return resp;
        } else {
            return new BotResponse("Sorry, I don't understand.");
        }
    }
}