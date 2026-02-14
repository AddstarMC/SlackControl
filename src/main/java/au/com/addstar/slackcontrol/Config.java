package au.com.addstar.slackcontrol;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class Config {
    private final SlackControl plugin;
    private String slack_bot_token;
    private String slack_app_token;
    private String slack_warn_channel;
    private boolean debug_mode = false;

    public Config(SlackControl plugin) {
        this.plugin = plugin;
    }

    public boolean loadConfig() {
        Path dataDirectory = plugin.getDataDirectory();
        Path configFile = dataDirectory.resolve("config.yml");

        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Could not create config directory", e);
        }

        if (!Files.exists(configFile)) {
            try (InputStream in = plugin.getResourceAsStream("config.yml")) {
                if (in != null) {
                    Files.copy(in, configFile);
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not copy default config", e);
            }
        }

        try (InputStream in = Files.newInputStream(configFile)) {
            Yaml yaml = new Yaml();
            @SuppressWarnings("unchecked")
            Map<String, Object> root = yaml.load(in);
            if (root == null) root = Map.of();

            slack_bot_token = getString(root, "slack.bot_token", "");
            slack_app_token = getString(root, "slack.app_token", "");
            slack_warn_channel = getString(root, "slack.warn_channel", "");
            debug_mode = getBoolean(root, "debug", false);
        } catch (IOException e) {
            throw new RuntimeException("Could not load config", e);
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    private static String getString(Map<String, Object> root, String path, String def) {
        Object v = getPath(root, path);
        return v != null ? v.toString() : def;
    }

    @SuppressWarnings("unchecked")
    private static boolean getBoolean(Map<String, Object> root, String path, boolean def) {
        Object v = getPath(root, path);
        if (v == null) return def;
        if (v instanceof Boolean b) return b;
        return Boolean.parseBoolean(v.toString());
    }

    @SuppressWarnings("unchecked")
    private static Object getPath(Map<String, Object> root, String path) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);
            if (!(next instanceof Map)) return null;
            current = (Map<String, Object>) next;
        }
        return current.get(parts[parts.length - 1]);
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

    public boolean getDebugMode() {
        return debug_mode;
    }

    public void setDebugMode(boolean debug_mode) {
        this.debug_mode = debug_mode;
    }
}
