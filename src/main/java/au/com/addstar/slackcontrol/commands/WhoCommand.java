package au.com.addstar.slackcontrol.commands;
import au.com.addstar.slackcontrol.SlackControl;
import au.com.addstar.slackcontrol.objects.BotResponse;
import au.com.addstar.slackcontrol.objects.UserCommand;

import java.util.List;

public class WhoCommand implements ISlackCommandHandler {
    SlackControl plugin;
    public WhoCommand(SlackControl plugin) {
        this.plugin = plugin;
    }

    @Override
    public BotResponse commandHandler(String user, UserCommand cmd) {
        plugin.getLogger().info("Who command called");
        BotResponse resp = new BotResponse("*Players online:*");
        return resp;
    }
}
