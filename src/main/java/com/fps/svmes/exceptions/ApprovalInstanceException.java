package com.fps.svmes.exceptions;

public class ApprovalInstanceException extends RuntimeException {
    public ApprovalInstanceException(String message) {
        super(message);
    }

    public ApprovalInstanceException(String message, Throwable cause) {
        super(message, cause);
    }
}
