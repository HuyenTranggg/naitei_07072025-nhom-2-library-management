package com.group2.library_management.exception;

public class FileStorageException extends RuntimeException {
    private String errorCode;
    private Object[] args;
    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileStorageException(String errorCode, Throwable cause, Object... args) {
        super(errorCode, cause);
        this.errorCode = errorCode;
        this.args = args;
    }

    // Getters
    public String getErrorCode() {
        return errorCode;
    }

    public Object[] getArgs() {
        return args;
    }
}
