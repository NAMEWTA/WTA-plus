package org.namewta.common.richtext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
@Tag("local")
class RichTextProcessorTest {
    @Test
    void normalizesFormattingAndAssets() {
        RichTextContent content = RichTextProcessor.normalize("<h1 onclick='x'>标题</h1><img data-oss-id='12' src='oss://12'><a href='https://example.com'>外链</a>");
        assertTrue(content.html().contains("data-oss-id=\"12\""));
        assertEquals(1, content.ossIds().size());
        assertTrue(content.html().contains("noopener"));
    }

    @Test
    void rejectsNonOssMediaAndTypeReuse() {
        assertThrows(IllegalArgumentException.class, () -> RichTextProcessor.normalize("<img src='data:image/png;base64,aa' data-oss-id='1'>"));
        assertThrows(IllegalArgumentException.class, () -> RichTextProcessor.normalize("<img src='oss://1' data-oss-id='1'><audio src='oss://1' data-oss-id='1'></audio>"));
        assertThrows(IllegalArgumentException.class, () -> RichTextProcessor.normalize("<p style='background:url(javascript:alert(1))'>x</p>"));
    }

    @Test
    void rejectsInvalidIdAndIsIdempotent() {
        assertThrows(IllegalArgumentException.class, () -> RichTextProcessor.normalize("<img src='oss://1' data-oss-id='9223372036854775808'>"));
        String html = RichTextProcessor.normalize("<p><strong>x</strong></p>").html();
        assertEquals(html, RichTextProcessor.normalize(html).html());
    }

    @Test
    void collectsAudioAndAttachmentReferences() {
        RichTextContent content = RichTextProcessor.normalize("<audio controls data-oss-id='2' src='oss://2'></audio><a data-oss-id='3' href='oss://3'>附件</a>");
        assertEquals(List.of(2L, 3L), content.ossIds());
    }
}
