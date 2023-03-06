package au.com.addstar.slackcontrol.commands;

import au.com.addstar.slackcontrol.SlackControl;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

public class SlackControlCommand extends Command {
    private SlackControl plugin;
    public SlackControlCommand(SlackControl plugin) {
        super("!slackcontrol", "slackcontrol.command", "slackcontrol");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        try {
            if (args.length == 0) {
                sender.sendMessage(TextComponent.fromLegacyText("Expected sub command"));
                return;
            } else {
                switch (args[0].toLowerCase())
                {
                    case "debug":
                        Boolean newmode = !plugin.getConfig().getDebugMode();
                        plugin.getConfig().setDebugMode(newmode);
                        sender.sendMessage(TextComponent.fromLegacyText("SlackControl debug is " + newmode));
                        break;
                }
            }
        } catch (Exception e) {
            sender.sendMessage(TextComponent.fromLegacyText(ChatColor.RED + e.getMessage()));
        }
    }
}
