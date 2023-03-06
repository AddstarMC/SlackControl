package au.com.addstar.slackcontrol.objects;

import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.model.Attachment;
import com.slack.api.model.block.LayoutBlock;

import java.util.*;

public class BotResponse {
    public List<Attachment> attachments = new ArrayList<>();
    public List<LayoutBlock> blocks = new ArrayList<>();
    public String text = "";
    public BotResponse() {}
    public enum BotResponseType {
        TEXT_ONLY,
        BLOCKS_ONLY,
        TEXT_AND_BLOCKS
    }

    public BotResponse(String text) {
        this.text = text;
    }

    public BotResponse(String text, List<Attachment> attachments) {
        this.text = text;
        this.attachments = attachments;
    }

    public BotResponse(String text, List<Attachment> attachments, List<LayoutBlock> blocks) {
        this.text = text;
        this.attachments = attachments;
        this.blocks = blocks;
    }

    // Determine the type of response content
    public BotResponseType getType() {
        if ((text == null) || (text.isEmpty())) {
            return BotResponseType.BLOCKS_ONLY;
        }
        else if ((blocks == null) || (blocks.isEmpty())) {
            return BotResponseType.TEXT_ONLY;
        }
        else {
            return BotResponseType.TEXT_AND_BLOCKS;
        }
    }

    public ChatPostMessageRequest makeMessageRequest() {
        ChatPostMessageRequest req = ChatPostMessageRequest.builder()
                .attachments(attachments)
                .blocks(blocks)
                .text(text)
                .build();
        return req;
    }
}
