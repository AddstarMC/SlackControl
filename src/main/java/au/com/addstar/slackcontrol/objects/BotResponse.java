package au.com.addstar.slackcontrol.objects;

import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.model.Attachment;
import com.slack.api.model.block.LayoutBlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BotResponse {
    private String text = "";
    private final List<Attachment> attachments;
    private final List<LayoutBlock> blocks;

    public BotResponse() {
        this.attachments = new ArrayList<>();
        this.blocks = new ArrayList<>();
    }

    public enum BotResponseType {
        TEXT_ONLY,
        BLOCKS_ONLY,
        TEXT_AND_BLOCKS
    }

    public BotResponse(String text) {
        this.text = text != null ? text : "";
        this.attachments = new ArrayList<>();
        this.blocks = new ArrayList<>();
    }

    public BotResponse(String text, List<Attachment> attachments) {
        this.text = text != null ? text : "";
        this.attachments = attachments != null ? new ArrayList<>(attachments) : new ArrayList<>();
        this.blocks = new ArrayList<>();
    }

    public BotResponse(String text, List<Attachment> attachments, List<LayoutBlock> blocks) {
        this.text = text != null ? text : "";
        this.attachments = attachments != null ? new ArrayList<>(attachments) : new ArrayList<>();
        this.blocks = blocks != null ? new ArrayList<>(blocks) : new ArrayList<>();
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text != null ? text : "";
    }

    public List<Attachment> getAttachments() {
        return Collections.unmodifiableList(attachments);
    }

    public List<LayoutBlock> getBlocks() {
        return Collections.unmodifiableList(blocks);
    }

    public void addBlock(LayoutBlock block) {
        if (block != null) {
            blocks.add(block);
        }
    }

    public void addAttachment(Attachment attachment) {
        if (attachment != null) {
            attachments.add(attachment);
        }
    }

    public BotResponseType getType() {
        if ((text == null) || (text.isEmpty())) {
            return BotResponseType.BLOCKS_ONLY;
        }
        if ((blocks == null) || (blocks.isEmpty())) {
            return BotResponseType.TEXT_ONLY;
        }
        return BotResponseType.TEXT_AND_BLOCKS;
    }

    public ChatPostMessageRequest makeMessageRequest() {
        return ChatPostMessageRequest.builder()
                .attachments(attachments)
                .blocks(blocks)
                .text(text)
                .build();
    }
}
