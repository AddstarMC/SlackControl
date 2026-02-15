package au.com.addstar.slackcontrol;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks plugin usage and connection stats: Slack command count, connection state, and connection age.
 */
public class PluginStats {

    private final AtomicLong totalSlackCommands = new AtomicLong(0);
    private volatile long connectionStartedAtMillis = 0;
    private volatile boolean slackConnectionActive = false;

    public void incrementSlackCommands() {
        totalSlackCommands.incrementAndGet();
    }

    public void markConnectionStarted() {
        connectionStartedAtMillis = System.currentTimeMillis();
        slackConnectionActive = true;
    }

    public void markConnectionClosed() {
        slackConnectionActive = false;
    }

    public long getTotalSlackCommands() {
        return totalSlackCommands.get();
    }

    public boolean isSlackConnected() {
        return slackConnectionActive;
    }

    /**
     * When the current Slack connection was established (millis since epoch), or 0 if not connected.
     */
    public long getConnectionStartedAtMillis() {
        return connectionStartedAtMillis;
    }

    /**
     * Age of the current connection in milliseconds, or 0 if not connected.
     */
    public long getConnectionAgeMillis() {
        if (!slackConnectionActive || connectionStartedAtMillis <= 0) {
            return 0;
        }
        return System.currentTimeMillis() - connectionStartedAtMillis;
    }
}
