package au.com.addstar.slackcontrol;

import au.com.addstar.slackcontrol.commands.SlackControlCommand;
import au.com.addstar.slackcontrol.listeners.GesuitRedisHandler;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
    id = "slackcontrol",
    name = "SlackControl",
    version = GeneratedVersion.VERSION,
    description = "Slack integration for Velocity",
    authors = {"add5tar"},
    dependencies = {@Dependency(id = "redisbungee", optional = true)}
)
public class SlackControl {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private final PluginStats stats = new PluginStats();
    private Config config;
    private SlackApp slackApp;
    private GesuitRedisHandler gesuitRedisHandler;

    @Inject
    public SlackControl(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        try {
            config = new Config(this);
            if (!config.loadConfig()) {
                logger.warn("SlackControl config failed to load.");
                return;
            }

            slackApp = new SlackApp(this);
            if (!slackApp.initApp()) {
                logger.warn("SlackControl disabled (Slack init failed).");
                return;
            }

            logger.info("SlackControl enabled.");

            server.getCommandManager().register(
                server.getCommandManager().metaBuilder("slackcontrol")
                    .plugin(this)
                    .build(),
                new SlackControlCommand(this)
            );

            if (server.getPluginManager().getPlugin("redisbungee").isPresent()) {
                try {
                    gesuitRedisHandler = new GesuitRedisHandler(this);
                    server.getEventManager().register(this, gesuitRedisHandler);
                    logger.info("geSuit Redis integration enabled (ValioBungee).");
                } catch (NoClassDefFoundError | Exception e) {
                    logger.warn("geSuit Redis integration unavailable: {}", e.getMessage());
                }
            } else {
                logger.info("ValioBungee not present; geSuit Redis integration disabled.");
            }
        } catch (Exception e) {
            logger.error("SlackControl failed to initialise!", e);
            throw new RuntimeException(e);
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (slackApp != null) {
            slackApp.shutdown();
        }
    }

    public ProxyServer getProxy() {
        return server;
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }

    public org.slf4j.Logger getLogger() {
        return logger;
    }

    public java.io.InputStream getResourceAsStream(String name) {
        return getClass().getClassLoader().getResourceAsStream(name);
    }

    public Config getConfig() {
        return config;
    }

    public SlackApp getSlackApp() {
        return slackApp;
    }

    public PluginStats getStats() {
        return stats;
    }

    public String getVersion() {
        return GeneratedVersion.VERSION;
    }

    public com.slack.api.methods.MethodsClient getMethodsClient() {
        return slackApp != null ? slackApp.getMethodsClient() : null;
    }

    public void logMsg(String msg) {
        logger.info(msg);
    }

    public void warnMsg(String msg) {
        logger.warn(msg);
    }

    public void debugMsg(String msg) {
        if (config != null && config.getDebugMode()) {
            logger.info(msg);
        }
    }
}
