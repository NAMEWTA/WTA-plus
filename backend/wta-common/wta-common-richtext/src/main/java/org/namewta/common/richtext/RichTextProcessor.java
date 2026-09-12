package org.namewta.common.richtext;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.jsoup.nodes.Entities;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 富文本统一规范化工具。输入只允许引用正整数 OSS ID，所有外部或内联媒体地址都会被拒绝。
 * 该类是无状态的纯 Java 工具，不依赖 Spring、OSS 或业务模块。
 */
public final class RichTextProcessor {
    private static final int MAX_BYTES = 1024 * 1024;
    private static final int MAX_NODES = 10_000;
    private static final int MAX_ASSETS = 100;
    private static final Set<String> MEDIA_TAGS = Set.of("img", "audio", "video");
    private static final Set<String> STYLE_PROPERTIES = Set.of("color", "background-color", "font-size",
        "text-align", "text-indent", "line-height", "width", "height");
    private static final Safelist SAFELIST = Safelist.relaxed()
        .addTags("h1", "h2", "h3", "h4", "h5", "h6", "u", "s", "strike", "sub", "sup",
            "blockquote", "pre", "code", "table", "thead", "tbody", "tr", "th", "td", "audio", "video")
        .removeTags("form", "input", "textarea", "select", "button", "iframe", "object", "embed")
        .addAttributes(":all", "style", "class")
        .addAttributes("a", "target", "rel", "data-oss-id")
        .addAttributes("img", "data-oss-id", "alt", "width", "height")
        .addAttributes("audio", "data-oss-id", "controls", "preload")
        .addAttributes("video", "data-oss-id", "controls", "preload", "width", "height")
        .addProtocols("a", "href", "http", "https", "oss")
        .addProtocols("img", "src", "oss")
        .addProtocols("audio", "src", "oss")
        .addProtocols("video", "src", "oss");

    private RichTextProcessor() {
    }

    /** 规范化 HTML fragment，结果可重复处理且输出集合不可变。 */
    public static RichTextContent normalize(String html) {
        String input = html == null ? "" : html;
        if (input.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new IllegalArgumentException("富文本内容超过 1 MiB 限制");
        }
        Document document = Jsoup.parseBodyFragment(input, "");
        if (document.getAllElements().size() > MAX_NODES) {
            throw new IllegalArgumentException("富文本节点数量超过限制");
        }
        validateStyles(document);
        Map<Long, RichTextAssetKind> assets = new LinkedHashMap<>();
        for (Element element : document.body().getAllElements()) {
            String tag = element.normalName();
            if (MEDIA_TAGS.contains(tag)) {
                validateMedia(element, tag, assets);
            } else if ("a".equals(tag) && element.hasAttr("data-oss-id")) {
                long id = parseId(element.attr("data-oss-id"));
                String href = element.attr("href");
                if (!href.equals("oss://" + id)) {
                    throw new IllegalArgumentException("附件链接必须使用 OSS 引用");
                }
                putAsset(assets, id, RichTextAssetKind.ATTACHMENT);
            }
        }
        String clean = Jsoup.clean(document.body().html(), "", SAFELIST,
            new Document.OutputSettings().prettyPrint(false).escapeMode(Entities.EscapeMode.xhtml));
        Document cleaned = Jsoup.parseBodyFragment(clean);
        normalizeLinks(cleaned);
        String normalized = cleaned.body().html();
        String text = cleaned.body().text();
        List<RichTextAsset> result = assets.entrySet().stream()
            .map(entry -> new RichTextAsset(entry.getKey(), entry.getValue())).toList();
        if (result.size() > MAX_ASSETS) {
            throw new IllegalArgumentException("富文本资源数量超过限制");
        }
        return new RichTextContent(normalized, text, result);
    }

    private static void validateStyles(Document document) {
        for (Element element : document.body().getAllElements()) {
            if (!element.hasAttr("style")) continue;
            String style = element.attr("style");
            for (String declaration : style.split(";")) {
                if (declaration.isBlank()) continue;
                int separator = declaration.indexOf(':');
                if (separator <= 0) throw new IllegalArgumentException("富文本样式格式无效");
                String property = declaration.substring(0, separator).trim().toLowerCase(Locale.ROOT);
                String value = declaration.substring(separator + 1).trim().toLowerCase(Locale.ROOT);
                if (!STYLE_PROPERTIES.contains(property) || value.contains("url(") || value.contains("expression")
                    || value.contains("javascript") || !value.matches("[a-z0-9#%(),.\\s+\\-]+")) {
                    throw new IllegalArgumentException("富文本包含不允许的样式");
                }
            }
        }
    }

    private static void validateMedia(Element element, String tag, Map<Long, RichTextAssetKind> assets) {
        long id = parseId(element.attr("data-oss-id"));
        String src = element.attr("src");
        if (!src.equals("oss://" + id) || element.hasAttr("poster") || element.hasAttr("srcset")) {
            throw new IllegalArgumentException("媒体必须引用 OSS ID，禁止外部地址");
        }
        RichTextAssetKind kind = switch (tag) {
            case "img" -> RichTextAssetKind.IMAGE;
            case "audio" -> RichTextAssetKind.AUDIO;
            default -> RichTextAssetKind.VIDEO;
        };
        putAsset(assets, id, kind);
    }

    private static long parseId(String value) {
        if (value == null || !value.matches("[1-9][0-9]*")) {
            throw new IllegalArgumentException("OSS ID 必须为正整数");
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("OSS ID 超出 Long 范围", ex);
        }
    }

    private static void putAsset(Map<Long, RichTextAssetKind> assets, long id, RichTextAssetKind kind) {
        RichTextAssetKind previous = assets.putIfAbsent(id, kind);
        if (previous != null && previous != kind) {
            throw new IllegalArgumentException("同一 OSS ID 不可用于不同媒体类型");
        }
    }

    private static void normalizeLinks(Document document) {
        for (Element link : document.select("a[href^=http], a[href^=https]")) {
            link.attr("rel", "noopener noreferrer");
            if (!link.hasAttr("target")) {
                link.attr("target", "_blank");
            }
        }
    }
}
