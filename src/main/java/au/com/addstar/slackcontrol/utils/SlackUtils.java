package au.com.addstar.slackcontrol.utils;

import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.composition.TextObject;

import java.util.ArrayList;
import java.util.List;

public class SlackUtils {
    public static SectionBlock makeSectionBlocks(List<String> lines) {
        SectionBlock block = new SectionBlock();
        List<TextObject> fields = new ArrayList<>();
        for (String line : lines) {
            fields.add(MarkdownTextObject.builder()
                    .text(line)
                    .build());
        }
        block.setFields(fields);
        return block;
    }
    public static SectionBlock makeSectionBlock(String text) {
        SectionBlock block = new SectionBlock();
        block.setText(MarkdownTextObject.builder()
                .text(text)
                .build());
        return block;
    }
}
