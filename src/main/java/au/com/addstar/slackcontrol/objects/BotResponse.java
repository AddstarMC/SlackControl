package au.com.addstar.slackcontrol.objects;

import com.slack.api.model.Attachment;

import java.util.ArrayList;
import java.util.List;

public class BotResponse {
    public List<Attachment> attachments = new ArrayList<>();
    public String text = "";
    public BotResponse() {}

    public BotResponse(String text) {
        this.text = text;
    }

    public BotResponse(String text, List<Attachment> attachments) {
        this.text = text;
        this.attachments = attachments;
    }
}
