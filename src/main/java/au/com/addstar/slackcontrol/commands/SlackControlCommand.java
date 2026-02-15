package au.com.addstar.slackcontrol.commands;

import au.com.addstar.slackcontrol.PluginStats;
import au.com.addstar.slackcontrol.SlackControl;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SlackControlCommand implements SimpleCommand {

    private final SlackControl plugin;

    public SlackControlCommand(SlackControl plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        var source = invocation.source();
        String[] args = invocation.arguments();

        try {
            if (args.length == 0) {
                source.sendMessage(Component.text("Expected sub command: debug, status"));
                return;
            }
            switch (args[0].toLowerCase()) {
                case "debug" -> {
                    boolean newMode = !plugin.getConfig().getDebugMode();
                    plugin.getConfig().setDebugMode(newMode);
                    source.sendMessage(Component.text("SlackControl debug is " + newMode));
                }
                case "status" -> sendStatus(source);
                default -> source.sendMessage(Component.text("Unknown sub command"));
            }
        } catch (Exception e) {
            source.sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
        }
    }

    private void sendStatus(com.velocitypowered.api.command.CommandSource source) {
        PluginStats stats = plugin.getStats();
        source.sendMessage(Component.text("SlackControl " + plugin.getVersion(), NamedTextColor.GOLD));
        source.sendMessage(Component.empty());
        source.sendMessage(Component.text("Slack connection: ", NamedTextColor.GRAY)
            .append(Component.text(stats.isSlackConnected() ? "connected" : "disconnected",
                stats.isSlackConnected() ? NamedTextColor.GREEN : NamedTextColor.RED)));
        source.sendMessage(Component.text("Total commands from Slack: ", NamedTextColor.GRAY)
            .append(Component.text(String.valueOf(stats.getTotalSlackCommands()), NamedTextColor.WHITE)));
        if (stats.isSlackConnected() && stats.getConnectionAgeMillis() > 0) {
            source.sendMessage(Component.text("Connection age: ", NamedTextColor.GRAY)
                .append(Component.text(formatDuration(stats.getConnectionAgeMillis()), NamedTextColor.WHITE)));
        }
    }

    private static String formatDuration(long millis) {
        long secs = millis / 1000;
        if (secs < 60) return secs + "s";
        long mins = secs / 60;
        secs %= 60;
        if (mins < 60) return mins + "m " + secs + "s";
        long hours = mins / 60;
        mins %= 60;
        if (hours < 24) return hours + "h " + mins + "m " + secs + "s";
        long days = hours / 24;
        hours %= 24;
        return days + "d " + hours + "h " + mins + "m";
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("slackcontrol.command");
    }

    private static final List<String> SUBCOMMANDS = List.of("debug", "status");

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length <= 1) {
            String partial = args.length == 1 ? args[0].toLowerCase() : "";
            return SUBCOMMANDS.stream()
                .filter(cmd -> cmd.startsWith(partial))
                .toList();
        }
        return List.of();
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return CompletableFuture.completedFuture(suggest(invocation));
    }
}
