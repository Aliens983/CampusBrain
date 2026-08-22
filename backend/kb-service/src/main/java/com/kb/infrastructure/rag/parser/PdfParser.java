package com.kb.infrastructure.rag.parser;

import com.kb.domain.rag.ParsedDocument;
import com.kb.infrastructure.common.ParseException;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Parse PDF files using Apache Tika.
 *
 * @author forever-king
 */
@Slf4j
@Component
public class PdfParser implements DocumentParser, DocumentParserSpi {

    @Override
    public boolean supports(String fileType) {
        return "pdf".equalsIgnoreCase(fileType);
    }

    @Override
    public ParsedDocument parse(InputStream inputStream, String fileName) throws ParseException {
        try {
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();

            PDFParser parser = new PDFParser();
            parser.parse(inputStream, handler, metadata, context);

            String content = handler.toString().trim();

            // Extract metadata
            Map<String, Object> meta = new HashMap<>();
            meta.put("author", metadata.get("Author"));
            meta.put("creator", metadata.get("creator"));
            meta.put("pageCount", parsePageCount(metadata.get("xmpTPg:NPages")));
            meta.put("creationDate", metadata.get("Creation-Date"));

            // Extract title
            String title = metadata.get("title");
            if (title == null || title.isEmpty()) {
                title = fileName != null && fileName.contains(".")
                        ? fileName.substring(0, fileName.lastIndexOf("."))
                        : fileName;
            }

            log.debug("Parsed PDF: title={}, chars={}, pages={}",
                    title, content.length(), meta.get("pageCount"));

            return ParsedDocument.builder()
                    .title(title)
                    .content(content)
                    .fileType("pdf")
                    .metadata(meta)
                    .build();

        } catch (Exception e) {
            throw new ParseException("Failed to parse PDF: " + fileName, e);
        }
    }

    // ===== DocumentParserSpi methods =====

    @Override
    public Set<String> supportedExtensions() {
        return Set.of("pdf");
    }

    @Override
    public int priority() { return 5; }

    @Override
    public String getName() { return "PdfParser (Apache Tika)"; }

    private int parsePageCount(String nPages) {
        if (nPages == null) return 0;
        try {
            return Integer.parseInt(nPages.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
