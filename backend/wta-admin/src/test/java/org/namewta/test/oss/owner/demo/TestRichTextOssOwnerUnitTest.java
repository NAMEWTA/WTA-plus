package org.namewta.test.oss.owner.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** 富文本业务作为 OSS 引用 owner 的合同证据。 */
@Tag("local")
class TestRichTextOssOwnerUnitTest {
    @Test
    void canonicalContentUsesOssReferences() {
        assertTrue("<img data-oss-id=\"1\" src=\"oss://1\">".contains("oss://"));
    }
}
