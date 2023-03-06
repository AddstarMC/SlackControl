package au.com.addstar.slackcontrol;

import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.*;

public class Config {
    Configuration config;
    SlackControl plugin;
    String slack_bot_token;
    String slack_app_token;
    String slack_warn_channel;

    Boolean debug_mode = false;

    public Config(SlackControl plugin) {
        this.plugin = plugin;
    }

    public boolean loadConfig() {
        File basedir = plugin.getDataFolder();
        String conffile = "config.yml";

        // Create plugin config folder if it doesn't exist
        if (!basedir.exists()) {
            plugin.getLogger().info("Created config folder: " + basedir.mkdir());
        }

        File configFile = new File(basedir, "config.yml");

        // Copy default config if it doesn't already exist
        if (!configFile.exists()) {
            FileOutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(configFile);
                InputStream in = plugin.getResourceAsStream("config.yml");
                in.transferTo(outputStream); // Throws IOException
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(basedir.getAbsolutePath(), conffile));
            slack_bot_token = config.getString("slack.bot_token", "");
            slack_app_token = config.getString("slack.app_token", "");
            slack_warn_channel = config.getString("slack.warn_channel", "");
            debug_mode = config.getBoolean("debug", false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    public String getSlackBotToken() {
        return slack_bot_token;
    }

    public String getSlackAppToken() {
        return slack_app_token;
    }

    public String getSlackWarnChannel() {
        return slack_warn_channel;
    }

    public Boolean getDebugMode() {
        return debug_mode;
    }
    public void setDebugMode(Boolean debug_mode) {
        this.debug_mode = debug_mode;
    }
}
