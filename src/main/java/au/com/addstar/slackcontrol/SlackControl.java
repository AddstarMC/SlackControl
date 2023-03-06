package au.com.addstar.slackcontrol;

import au.com.addstar.slackcontrol.commands.SlackControlCommand;
import au.com.addstar.slackcontrol.listeners.geSuitListener;
import com.slack.api.methods.MethodsClient;
import net.md_5.bungee.api.plugin.Plugin;

public class SlackControl extends Plugin {
    Config config = new Config(this);
    SlackApp slackapp = new SlackApp(this);
    geSuitListener gesuit;

    @Override
    public void onEnable() {
        try {
            // Load config and initialise app
            if (config.loadConfig() && slackapp.initApp()) {
                getLogger().info("SlackControl enabled.");
            } else {
                getLogger().info("SlackControl disabled.");
                return;
            }

            // Register proxy commands
            getProxy().getPluginManager().registerCommand(this, new SlackControlCommand(this));

            // Set up geSuit event handlers
            gesuit = new geSuitListener(this);
            getProxy().getPluginManager().registerListener(this, gesuit);
        } catch (Exception e) {
            getLogger().warning("Slack App failed to initialise!");
            throw new RuntimeException(e);
        }
    }

    public Config getConfig() {
        return config;
    }

    public SlackApp getSlackApp() {
        return slackapp;
    }

    public MethodsClient getMethodsClient() {
        return getSlackApp().getMethodsClient();
    }

    public void logMsg(String msg) {
        getLogger().info(msg);
    }

    public void warnMsg(String msg) {
        getLogger().warning(msg);
    }

    public void debugMsg(String msg) {
        if (config.getDebugMode())
            getLogger().info(msg);
    }
}