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
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.conversations.ConversationsInfoResponse;
import com.slack.api.methods.response.users.UsersInfoResponse;
import com.slack.api.model.Attachment;
import com.slack.api.model.Conversation;
import com.slack.api.model.event.AppMentionEvent;
import com.slack.api.model.event.MemberJoinedChannelEvent;
import com.slack.api.model.event.MessageEvent;
import com.slack.api.socket_mode.SocketModeClient;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SlackApp {
    private final SlackControl plugin;
    private final Config config;
    private AppConfig appConfig;
    private App app;
    private MethodsClient methods;
    private SocketModeApp socketModeApp;
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

    public boolean initApp() {
        if (config.getSlackBotToken() == null || config.getSlackBotToken().isEmpty()) {
            plugin.warnMsg("Slack bot_token has not been set");
            return false;
        }

        try {
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
                String senderId = Objects.requireNonNullElse(
                    req.getEvent().getUser(),
                    Objects.requireNonNullElse(req.getEvent().getParentUserId(), "")
                );
                String channelId = Objects.toString(req.getEvent().getChannel(), "");
                String messageText = req.getEvent().getText();
                logEventWithResolvedNames("Received MessageEvent", senderId, channelId, messageText);
                UserCommand cmd = extractCommandFromMsg(messageText);
                handleCommand(cmd, req.getEvent().getUser(), ctx, true);
                return ctx.ack();
            });

            app.event(AppMentionEvent.class, (req, ctx) -> {
                String senderId = Objects.toString(req.getEvent().getUser(), "");
                String channelId = Objects.toString(req.getEvent().getChannel(), "");
                String messageText = req.getEvent().getText();
                logEventWithResolvedNames("Received AppMentionEvent", senderId, channelId, messageText);
                UserCommand cmd = extractCommandFromMsg(messageText);
                handleCommand(cmd, req.getEvent().getUser(), ctx, false);
                return ctx.ack();
            });

            app.event(MemberJoinedChannelEvent.class, (req, ctx) -> {
                String userId = Objects.toString(req.getEvent().getUser(), "");
                String channelId = Objects.toString(req.getEvent().getChannel(), "");
                String inviterId = req.getEvent().getInviter();
                logMemberJoinedChannel(userId, channelId, inviterId);
                return ctx.ack();
            });

            socketModeApp = new SocketModeApp(
                config.getSlackAppToken(),
                SocketModeClient.Backend.JavaWebSocket,
                app
            );
            socketModeApp.startAsync();

            return true;
        } catch (Exception e) {
            plugin.warnMsg("Slack init failed: " + e.getMessage());
            plugin.getLogger().debug("Slack init exception", e);
            return false;
        }
    }

    /**
     * Stops the Socket Mode connection and releases resources. Safe to call if never started or already shut down.
     */
    public void shutdown() {
        if (socketModeApp != null) {
            try {
                socketModeApp.close();
            } catch (Exception e) {
                plugin.warnMsg("Error closing Slack Socket Mode: " + e.getMessage());
            } finally {
                socketModeApp = null;
            }
        }
    }

    /**
     * Schedules resolution of user and channel IDs to display names, then logs.
     * Runs asynchronously so the Slack event handler can ack quickly.
     */
    private void logEventWithResolvedNames(String prefix, String userId, String channelId, String messageText) {
        plugin.getProxy().getScheduler()
            .buildTask(plugin, () -> {
                String senderDisplay = userId.isEmpty() ? "unknown" : userId;
                String channelDisplay = channelId.isEmpty() ? "?" : channelId;
                MethodsClient client = methods;
                if (client != null) {
                    if (!userId.isEmpty()) {
                        try {
                            UsersInfoResponse r = client.usersInfo(req -> req.user(userId));
                            if (r.isOk() && r.getUser() != null) {
                                String name = r.getUser().getName();
                                if (name != null && !name.isBlank()) {
                                    senderDisplay = "@" + name;
                                }
                            }
                        } catch (IOException | SlackApiException e) {
                            plugin.debugMsg("Could not resolve user " + userId + ": " + e.getMessage());
                        }
                    }
                    if (!channelId.isEmpty()) {
                        try {
                            ConversationsInfoResponse r = client.conversationsInfo(req -> req.channel(channelId));
                            if (r.isOk() && r.getChannel() != null) {
                                Conversation ch = r.getChannel();
                                if (ch.isIm()) {
                                    channelDisplay = "DM";
                                } else {
                                    String name = ch.getName();
                                    channelDisplay = (name != null && !name.isBlank()) ? "#" + name : channelId;
                                }
                            }
                        } catch (IOException | SlackApiException e) {
                            plugin.debugMsg("Could not resolve channel " + channelId + ": " + e.getMessage());
                        }
                    }
                }
                plugin.logMsg(prefix + " from " + senderDisplay + " in " + channelDisplay + ": " + messageText);
            })
            .schedule();
    }

    /**
     * Logs member_joined_channel (e.g. bot added to a channel) with resolved names.
     */
    private void logMemberJoinedChannel(String userId, String channelId, String inviterId) {
        plugin.getProxy().getScheduler()
            .buildTask(plugin, () -> {
                String userDisplay = userId.isEmpty() ? "unknown" : userId;
                String channelDisplay = channelId.isEmpty() ? "?" : channelId;
                String inviterDisplay = null;
                if (inviterId != null && !inviterId.isBlank()) {
                    inviterDisplay = inviterId;
                }
                MethodsClient client = methods;
                if (client != null) {
                    if (!userId.isEmpty()) {
                        try {
                            UsersInfoResponse r = client.usersInfo(req -> req.user(userId));
                            if (r.isOk() && r.getUser() != null) {
                                String name = r.getUser().getName();
                                if (name != null && !name.isBlank()) {
                                    userDisplay = "@" + name;
                                }
                            }
                        } catch (IOException | SlackApiException e) {
                            plugin.debugMsg("Could not resolve user " + userId + ": " + e.getMessage());
                        }
                    }
                    if (!channelId.isEmpty()) {
                        try {
                            ConversationsInfoResponse r = client.conversationsInfo(req -> req.channel(channelId));
                            if (r.isOk() && r.getChannel() != null) {
                                Conversation ch = r.getChannel();
                                if (ch.isIm()) {
                                    channelDisplay = "DM";
                                } else {
                                    String name = ch.getName();
                                    channelDisplay = (name != null && !name.isBlank()) ? "#" + name : channelId;
                                }
                            }
                        } catch (IOException | SlackApiException e) {
                            plugin.debugMsg("Could not resolve channel " + channelId + ": " + e.getMessage());
                        }
                    }
                    if (inviterDisplay != null) {
                        try {
                            UsersInfoResponse r = client.usersInfo(req -> req.user(inviterId));
                            if (r.isOk() && r.getUser() != null) {
                                String name = r.getUser().getName();
                                if (name != null && !name.isBlank()) {
                                    inviterDisplay = "@" + name;
                                }
                            }
                        } catch (IOException | SlackApiException e) {
                            plugin.debugMsg("Could not resolve inviter " + inviterId + ": " + e.getMessage());
                        }
                    }
                }
                String msg = "Member joined channel: " + userDisplay + " joined " + channelDisplay;
                if (inviterDisplay != null) {
                    msg += " (invited by " + inviterDisplay + ")";
                }
                msg += ".";
                plugin.logMsg(msg);
            })
            .schedule();
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
                MethodsClient client = plugin.getMethodsClient();
                if (client == null) {
                    plugin.warnMsg("Slack not connected; cannot send channel message.");
                    return;
                }
                ChatPostMessageRequest request = ChatPostMessageRequest.builder()
                    .channel(channel)
                    .text(text)
                    .attachments(attachments != null ? attachments : new ArrayList<>())
                    .build();
                try {
                    plugin.debugMsg("Doing chatPostMessage()");
                    ChatPostMessageResponse response = client.chatPostMessage(request);
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

    /** Fallback text for block-only responses (accessibility and Slack API requirement). */
    private static final String BLOCKS_ONLY_FALLBACK_TEXT = "See message content below.";

    private void handleCommand(UserCommand cmd, String user, SlashCommandContext ctx) {
        BotResponse resp = handleCommand(cmd, user);
        plugin.debugMsg("handleCommand1: " + resp.getType());
        try {
            String text = resp.getText();
            if (text.isEmpty() && !resp.getBlocks().isEmpty()) {
                text = BLOCKS_ONLY_FALLBACK_TEXT;
            }
            switch (resp.getType()) {
                case TEXT_ONLY -> ctx.respond(SlashCommandResponse.builder().text(text).build());
                case BLOCKS_ONLY -> ctx.respond(SlashCommandResponse.builder()
                    .text(text)
                    .blocks(resp.getBlocks())
                    .build());
                case TEXT_AND_BLOCKS -> ctx.respond(SlashCommandResponse.builder()
                    .text(text)
                    .blocks(resp.getBlocks())
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
                resp.setText("<@" + user + "> " + resp.getText());
            }
            String text = resp.getText();
            if (text.isEmpty() && !resp.getBlocks().isEmpty()) {
                text = BLOCKS_ONLY_FALLBACK_TEXT;
            }
            ChatPostMessageResponse result = null;
            switch (resp.getType()) {
                case TEXT_ONLY -> result = ctx.say(text);
                case BLOCKS_ONLY -> result = ctx.say(text, resp.getBlocks());
                case TEXT_AND_BLOCKS -> result = ctx.say(text, resp.getBlocks());
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
