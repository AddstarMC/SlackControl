package au.com.addstar.slackcontrol.commands;
import au.com.addstar.slackcontrol.SlackControl;
import au.com.addstar.slackcontrol.objects.BotResponse;
import au.com.addstar.slackcontrol.objects.UserCommand;
import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.slack.api.model.block.ContextBlock;
import com.slack.api.model.block.ContextBlockElement;
import com.slack.api.model.block.DividerBlock;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static au.com.addstar.slackcontrol.utils.SlackUtils.makeSectionBlock;

public class WhoCommand implements ISlackCommandHandler {
    SlackControl plugin;
    public WhoCommand(SlackControl plugin) {
        this.plugin = plugin;
    }

    @Override
    public BotResponse commandHandler(String user, UserCommand cmd) {
        plugin.debugMsg("Who command called");
        Collection<ProxiedPlayer> players = ProxyServer.getInstance().getPlayers();
        BotResponse resp = new BotResponse();
        List<LayoutBlock> blocks = new ArrayList<>();

        List<ContextBlockElement> elements = new ArrayList<>();
        elements.add(MarkdownTextObject.builder()
                .text(":bookmark_tabs: *Players online:* " + players.size())
                .build());
        resp.blocks.add(ContextBlock.builder().elements(elements).build());
        resp.blocks.add(DividerBlock.builder().build());

        // Collect groups of players by server
        ListMultimap<String, String> groups = ArrayListMultimap.create();
        for (ProxiedPlayer player : players)
        {
            String serverName;
            if (player.getServer() != null) {
                serverName = player.getServer().getInfo().getName();
                plugin.debugMsg(serverName + "/" + player.getName());
            } else {
                serverName = "Unknown";
            }

            groups.put(serverName, player.getDisplayName());
        }

        // Construct formatted lines of players per server
        List<String> lines = new ArrayList<>();
        List<String> sortedKeys = Lists.newArrayList(groups.keySet());
        Collections.sort(sortedKeys);
        for(String key : sortedKeys)
        {
            List<String> groupPlayers = Lists.newArrayList(groups.get(key));
            Collections.sort(groupPlayers);
            lines.add(String.format(":black_small_square: *%s* (%d): ", key, groupPlayers.size()) +
                    Joiner.on(", ").join(groupPlayers));
        }

        // Only show server list if someone is online
        if (lines.size() > 0)
            resp.blocks.add(makeSectionBlock(Joiner.on("\n").join(lines)));

        return resp;
    }
}
