package com.kb.infrastructure.common;

/**
 * Exception thrown when document parsing fails.
 *
 * @author forever-king
 */
public class ParseException extends RuntimeException {

    public ParseException(String message) {
        super(message);
    }

    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
