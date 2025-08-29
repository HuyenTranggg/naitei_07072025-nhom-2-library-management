package com.group2.library_management.exception;

public class DuplicateBookException extends RuntimeException {
    public DuplicateBookException() {
    }

    public DuplicateBookException(String message) {
        super(message);
    }
}
