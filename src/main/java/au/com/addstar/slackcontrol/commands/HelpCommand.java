package au.com.addstar.slackcontrol.commands;
import au.com.addstar.slackcontrol.SlackControl;
import au.com.addstar.slackcontrol.objects.BotResponse;
import au.com.addstar.slackcontrol.objects.UserCommand;

import java.util.List;

public class HelpCommand implements ISlackCommandHandler {
    SlackControl plugin;
    public HelpCommand(SlackControl plugin) {
        this.plugin = plugin;
    }

    @Override
    public BotResponse commandHandler(String user, UserCommand cmd) {
        plugin.getLogger().info("Help command called");
        BotResponse resp = new BotResponse("*List of available commands:*");
        return resp;
    }
}
