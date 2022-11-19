package au.com.addstar.slackcontrol.commands;

import au.com.addstar.slackcontrol.objects.BotResponse;
import au.com.addstar.slackcontrol.objects.UserCommand;

import java.util.List;

public interface ISlackCommandHandler {
    BotResponse commandHandler(String user, UserCommand cmd);
}
