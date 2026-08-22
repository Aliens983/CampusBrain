package com.kb.infrastructure.rag.parser;

import com.kb.domain.rag.ParsedDocument;
import com.kb.infrastructure.common.ParseException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parse Markdown (.md) files into clean text.
 *
 * @author forever-king
 */
@Component
public class MarkdownParser implements DocumentParser, DocumentParserSpi {

    /** 用于匹配Markdown一级标题的正则表达式模式 */
    private static final Pattern HEADING_PATTERN = Pattern.compile("^#\\s+(.+)$", Pattern.MULTILINE);

    @Override
    public boolean supports(String fileType) {
        String lower = fileType.toLowerCase();
        return "md".equals(lower) || "markdown".equals(lower);
    }

    @Override
    public ParsedDocument parse(InputStream inputStream, String fileName) throws ParseException {
        try {
            String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            // Try to extract title from first H1 heading
            String title = extractTitle(content, fileName);

            // Extract section headings
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("sectionCount", countSections(content));

            return ParsedDocument.builder()
                    .title(title)
                    .content(cleanMarkdown(content))
                    .fileType("md")
                    .metadata(metadata)
                    .build();

        } catch (IOException e) {
            throw new ParseException("Failed to parse Markdown file: " + fileName, e);
        }
    }

    private String extractTitle(String content, String defaultTitle) {
        Matcher matcher = HEADING_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        // Fall back to filename without extension
        if (defaultTitle != null && defaultTitle.contains(".")) {
            return defaultTitle.substring(0, defaultTitle.lastIndexOf("."));
        }
        return defaultTitle;
    }

    private int countSections(String content) {
        Matcher matcher = Pattern.compile("^#{1,3}\\s", Pattern.MULTILINE).matcher(content);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    // ===== DocumentParserSpi methods =====

    @Override
    public Set<String> supportedExtensions() {
        return Set.of("md", "markdown", "txt");
    }

    @Override
    public int priority() { return 5; }

    @Override
    public String getName() { return "MarkdownParser"; }

    /**
     * Remove Markdown formatting syntax, keep readable text.
     */
    private String cleanMarkdown(String content) {
        return content
                // Remove code blocks but keep content
                .replaceAll("```[\\s\\S]*?```", "")
                // Remove inline code markers
                .replaceAll("`([^`]+)`", "$1")
                // Remove image syntax
                .replaceAll("!\\[.*?]\\(.*?\\)", "")
                // Convert links to text
                .replaceAll("\\[(.*?)]\\(.*?\\)", "$1")
                // Remove bold/italic markers
                .replaceAll("\\*{1,3}([^*]+)\\*{1,3}", "$1")
                // Remove horizontal rules
                .replaceAll("^[-*_]{3,}\\s*$", "")
                // Remove blockquote markers
                .replaceAll("(?m)^>\\s?", "")
                // Collapse multiple blank lines
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }
}
