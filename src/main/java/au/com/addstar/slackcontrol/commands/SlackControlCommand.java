package au.com.addstar.slackcontrol.commands;

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
                source.sendMessage(Component.text("Expected sub command"));
                return;
            }
            switch (args[0].toLowerCase()) {
                case "debug" -> {
                    boolean newMode = !plugin.getConfig().getDebugMode();
                    plugin.getConfig().setDebugMode(newMode);
                    source.sendMessage(Component.text("SlackControl debug is " + newMode));
                }
                default -> source.sendMessage(Component.text("Unknown sub command"));
            }
        } catch (Exception e) {
            source.sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("slackcontrol.command");
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return List.of();
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return CompletableFuture.completedFuture(List.of());
    }
}
