package au.com.addstar.slackcontrol.commands;
import au.com.addstar.slackcontrol.SlackControl;
import au.com.addstar.slackcontrol.objects.BotResponse;
import au.com.addstar.slackcontrol.objects.UserCommand;
import com.slack.api.model.block.*;
import com.slack.api.model.block.composition.MarkdownTextObject;

import static au.com.addstar.slackcontrol.utils.SlackUtils.makeSectionBlock;

import java.util.ArrayList;
import java.util.List;


public class HelpCommand implements ISlackCommandHandler {
    SlackControl plugin;
    public HelpCommand(SlackControl plugin) {
        this.plugin = plugin;
    }

    @Override
    public BotResponse commandHandler(String user, UserCommand cmd) {
        plugin.debugMsg("Help command called");
        BotResponse resp = new BotResponse();
        resp.setText("Available bot commands");

        List<ContextBlockElement> elements = new ArrayList<>();
        elements.add(MarkdownTextObject.builder()
            .text(":information_source: *Available bot commands:*")
            .build());
        resp.addBlock(ContextBlock.builder().elements(elements).build());
        resp.addBlock(DividerBlock.builder().build());

        resp.addBlock(makeSectionBlock(
                ":black_small_square: `help` - This help message\n" +
                ":black_small_square: `who` - Show list of online players\n" +
                ":black_small_square: `names <player>` - Show a player's name history\n" +
                ":black_small_square: `seen <player>` - Show last online info\n" +
                ":black_small_square: `where <player>` - Show associated players by IPs\n" +
                ":black_small_square: `warnings <player>` - Show a player's warnings\n" +
                ":black_small_square: `bans <player>` - Show a player's ban history\n"
        ));

        return resp;
    }
}
