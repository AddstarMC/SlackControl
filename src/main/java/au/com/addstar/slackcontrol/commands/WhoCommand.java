package au.com.addstar.slackcontrol.commands;

import au.com.addstar.slackcontrol.SlackControl;
import au.com.addstar.slackcontrol.objects.BotResponse;
import au.com.addstar.slackcontrol.objects.UserCommand;
import com.slack.api.model.block.ContextBlock;
import com.slack.api.model.block.ContextBlockElement;
import com.slack.api.model.block.DividerBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static au.com.addstar.slackcontrol.utils.SlackUtils.makeSectionBlock;

public class WhoCommand implements ISlackCommandHandler {

    private final SlackControl plugin;

    public WhoCommand(SlackControl plugin) {
        this.plugin = plugin;
    }

    @Override
    public BotResponse commandHandler(String user, UserCommand cmd) {
        plugin.debugMsg("Who command called");
        Collection<Player> players = plugin.getProxy().getAllPlayers();
        BotResponse resp = new BotResponse();

        List<ContextBlockElement> elements = new ArrayList<>();
        elements.add(MarkdownTextObject.builder()
            .text(":bookmark_tabs: *Players online:* " + players.size())
            .build());
        resp.blocks.add(ContextBlock.builder().elements(elements).build());
        resp.blocks.add(DividerBlock.builder().build());

        // Group players by server (sorted by server name)
        TreeMap<String, List<String>> groups = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Player player : players) {
            String serverName = player.getCurrentServer()
                .map(conn -> conn.getServerInfo().getName())
                .orElse("Unknown");
            plugin.debugMsg(serverName + "/" + player.getUsername());
            groups.computeIfAbsent(serverName, k -> new ArrayList<>()).add(player.getUsername());
        }

        List<String> lines = new ArrayList<>();
        for (var entry : groups.entrySet()) {
            List<String> names = entry.getValue();
            names.sort(String.CASE_INSENSITIVE_ORDER);
            lines.add(String.format(":black_small_square: *%s* (%d): %s",
                entry.getKey(), names.size(), String.join(", ", names)));
        }

        if (!lines.isEmpty()) {
            resp.blocks.add(makeSectionBlock(String.join("\n", lines)));
        }

        return resp;
    }
}
