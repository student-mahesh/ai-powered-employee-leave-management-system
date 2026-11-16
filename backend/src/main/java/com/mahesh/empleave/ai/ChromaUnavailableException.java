package com.mahesh.empleave.ai;

public class ChromaUnavailableException extends RuntimeException {
    public ChromaUnavailableException(String message) { super(message); }
    public ChromaUnavailableException(String message, Throwable cause) { super(message, cause); }
}
