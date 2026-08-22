package com.kb.infrastructure.rag.parser;

import com.kb.domain.rag.ParsedDocument;
import com.kb.infrastructure.common.ParseException;

import java.io.InputStream;

/**
 * Interface for document parsers.
 * Each implementation handles one or more file types.
 *
 * @author forever-king
 */
public interface DocumentParser {

    /**
     * Check if this parser supports the given file type.
     */
    boolean supports(String fileType);

    /**
     * Parse a document input stream into clean text.
     *
     * @param inputStream the file content
     * @param fileName    original file name (for metadata extraction)
     * @return parsed document with clean text and metadata
     * @throws ParseException if parsing fails
     */
    ParsedDocument parse(InputStream inputStream, String fileName) throws ParseException;
}
