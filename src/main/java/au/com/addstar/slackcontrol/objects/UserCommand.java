package au.com.addstar.slackcontrol.objects;

public class UserCommand {
    String cmd = "";
    String args = "";

    public String getCmd() {
        return cmd.trim();
    }

    public void setCmd(String cmd) {
        this.cmd = cmd.trim().toLowerCase();
    }

    public String getArgs() {
        return args;
    }

    public void setArgs(String args) {
        this.args = args;
    }
}
