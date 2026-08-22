package com.kb.domain.document;

/**
 * Document processing lifecycle status.
 * @author forever-king
 */
public enum DocumentStatus {

    /** Initial state after upload, before processing starts */
    UPLOADED,

    /** Document is being parsed by Tika */
    PARSING,

    /** Document text is being split into chunks */
    CHUNKING,

    /** Chunks are being embedded into vectors */
    EMBEDDING,

    /** Document is fully processed and searchable */
    READY,

    /** Processing failed */
    FAILED
}
