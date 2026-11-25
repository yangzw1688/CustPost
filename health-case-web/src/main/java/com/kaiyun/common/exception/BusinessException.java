package com.kaiyun.common.exception;

/** An expected failure with a message that is safe to show to the user. */
public class BusinessException extends RuntimeException {
    private final int status;

    public BusinessException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
