package org.namewta.common.richtext;

/** 富文本中引用的 OSS 资源。 */
public record RichTextAsset(long ossId, RichTextAssetKind kind) {
    public RichTextAsset {
        if (ossId <= 0 || kind == null) {
            throw new IllegalArgumentException("富文本资源引用无效");
        }
    }
}
