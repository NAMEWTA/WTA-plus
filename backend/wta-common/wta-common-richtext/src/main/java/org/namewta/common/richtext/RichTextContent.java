package org.namewta.common.richtext;

import java.util.List;

/** 规范化后的富文本内容，不暴露 jsoup 内部对象。 */
public record RichTextContent(String html, String text, List<RichTextAsset> assets) {
    public RichTextContent {
        html = html == null ? "" : html;
        text = text == null ? "" : text;
        assets = assets == null ? List.of() : List.copyOf(assets);
    }

    /** 返回当前内容引用的 OSS ID。 */
    public List<Long> ossIds() {
        return assets.stream().map(RichTextAsset::ossId).distinct().toList();
    }
}
