package au.com.addstar.slackcontrol;

import au.com.addstar.slackcontrol.commands.HelpCommand;
import au.com.addstar.slackcontrol.commands.ISlackCommandHandler;
import au.com.addstar.slackcontrol.commands.WhoCommand;
import au.com.addstar.slackcontrol.objects.BotResponse;
import au.com.addstar.slackcontrol.objects.UserCommand;
import com.slack.api.Slack;
import com.slack.api.app_backend.slash_commands.response.SlashCommandResponse;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.context.builtin.EventContext;
import com.slack.api.bolt.context.builtin.SlashCommandContext;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.bolt.util.BuilderConfigurator;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlackApp {
    private final SlackControl plugin;
    private final Config config;
    private AppConfig appConfig;
    private App app;
    private MethodsClient methods;
    private final Map<String, ISlackCommandHandler> slackCommandHandlers = new HashMap<>();

    public SlackApp(SlackControl plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

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
        if (config.getSlackBotToken() == null || config.getSlackBotToken().isEmpty()) {
            plugin.warnMsg("Slack bot_token has not been set");
            return false;
        }

        appConfig = AppConfig.builder().singleTeamBotToken(config.getSlackBotToken()).build();
        app = new App(appConfig);
        methods = Slack.getInstance().methods(config.getSlackBotToken());

        registerHandlers();

        app.command("/mcbot", (req, ctx) -> {
            plugin.logMsg("Received command"
                + " from " + req.getPayload().getUserName()
                + " in #" + req.getPayload().getChannelName()
                + ": " + req.getPayload().getCommand()
                + " " + req.getPayload().getText()
            );
            UserCommand cmd = extractCommandFromMsg(req.getPayload().getText());
            handleCommand(cmd, req.getPayload().getUserId(), ctx);
            return ctx.ack();
        });

        app.event(MessageEvent.class, (req, ctx) -> {
            plugin.logMsg("Received MessageEvent"
                + " from " + req.getEvent().getParentUserId()
                + " in " + req.getEvent().getChannel()
                + ": " + req.getEvent().getText()
            );
            UserCommand cmd = extractCommandFromMsg(req.getEvent().getText());
            handleCommand(cmd, req.getEvent().getUser(), ctx, true);
            return ctx.ack();
        });

        app.event(AppMentionEvent.class, (req, ctx) -> {
            plugin.logMsg("Received AppMentionEvent"
                + " from " + req.getEvent().getUser()
                + " in " + req.getEvent().getChannel()
                + ": " + req.getEvent().getText()
            );
            UserCommand cmd = extractCommandFromMsg(req.getEvent().getText());
            handleCommand(cmd, req.getEvent().getUser(), ctx, false);
            return ctx.ack();
        });

        SocketModeApp socketModeApp = new SocketModeApp(
            config.getSlackAppToken(),
            SocketModeClient.Backend.JavaWebSocket,
            app
        );
        socketModeApp.startAsync();

        return true;
    }

    private void registerHandler(ISlackCommandHandler handler, @NotNull String... commands) {
        for (String command : commands) {
            slackCommandHandlers.put(command.toLowerCase(), handler);
        }
    }

    public boolean unregisterHandlers() {
        slackCommandHandlers.clear();
        return true;
    }

    public boolean sendChannelMessage(String channel, String text, List<Attachment> attachments) {
        plugin.getProxy().getScheduler()
            .buildTask(plugin, () -> {
                ChatPostMessageRequest request = ChatPostMessageRequest.builder()
                    .channel(channel)
                    .text(text)
                    .attachments(attachments != null ? attachments : new ArrayList<>())
                    .build();
                try {
                    plugin.debugMsg("Doing chatPostMessage()");
                    ChatPostMessageResponse response = plugin.getMethodsClient().chatPostMessage(request);
                    if (!response.isOk()) {
                        plugin.warnMsg("Slack message error: " + response.getError());
                    }
                } catch (IOException | SlackApiException e) {
                    plugin.warnMsg("Slack send failed: " + e.getMessage());
                }
            })
            .schedule();
        return true;
    }

    private UserCommand extractCommandFromMsg(String fullmsg) {
        UserCommand result = new UserCommand();
        if (fullmsg == null) fullmsg = "";
        fullmsg = fullmsg.replaceFirst("<@[0-9A-Z]*>[\\s]*", "")
            .replaceAll(" +", " ")
            .trim();

        int pos = fullmsg.indexOf(" ");
        if (pos == -1) {
            result.setCmd(fullmsg);
        } else {
            result.setCmd(fullmsg.substring(0, pos));
            result.setArgs(fullmsg.substring(pos + 1));
        }

        plugin.debugMsg("Command  : " + result.getCmd());
        plugin.debugMsg("Arguments: " + result.getArgs());
        return result;
    }

    private ISlackCommandHandler getCmdHandler(UserCommand cmd) {
        return slackCommandHandlers.get(cmd.getCmd());
    }

    private void handleCommand(UserCommand cmd, String user, SlashCommandContext ctx) {
        BotResponse resp = handleCommand(cmd, user);
        plugin.debugMsg("handleCommand1: " + resp.getType());
        try {
            switch (resp.getType()) {
                case TEXT_ONLY -> ctx.respond(SlashCommandResponse.builder().text(resp.text).build());
                case BLOCKS_ONLY -> ctx.respond(SlashCommandResponse.builder().blocks(resp.blocks).build());
                case TEXT_AND_BLOCKS -> ctx.respond(SlashCommandResponse.builder()
                    .text(resp.text)
                    .blocks(resp.blocks)
                    .build());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleCommand(UserCommand cmd, String user, EventContext ctx, boolean direct) {
        BotResponse resp = handleCommand(cmd, user);
        plugin.debugMsg("handleCommand2: " + resp.getType());
        try {
            if (!direct) {
                resp.text = "<@" + user + "> " + resp.text;
            }
            ChatPostMessageResponse result = null;
            switch (resp.getType()) {
                case TEXT_ONLY -> result = ctx.say(resp.text);
                case BLOCKS_ONLY -> result = ctx.say(resp.blocks);
                case TEXT_AND_BLOCKS -> result = ctx.say(resp.text, resp.blocks);
                default -> plugin.warnMsg("Unknown message type: " + resp.getType());
            }
            if (result != null && !result.isOk()) {
                plugin.warnMsg("Slack error: " + result.getError());
            }
        } catch (IOException | SlackApiException e) {
            plugin.warnMsg("Slack error: " + e.getMessage());
        }
    }

    private BotResponse handleCommand(UserCommand cmd, String user) {
        ISlackCommandHandler handler = getCmdHandler(cmd);
        if (handler != null) {
            try {
                BotResponse resp = handler.commandHandler(user, cmd);
                if (resp == null) {
                    return new BotResponse("Sorry, the command handling failed. Try again later.");
                }
                return resp;
            } catch (Exception e) {
                plugin.warnMsg("Error handling command!");
                e.printStackTrace();
                return new BotResponse("Sorry, the command handling failed. Try again later.");
            }
        }
        return new BotResponse("Sorry, I don't understand.");
    }
}
