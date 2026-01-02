package com.gourav.LedgerLens.Exception;

public class InvalidGmailGrantException extends RuntimeException{

    public InvalidGmailGrantException(String message) {
        super(message);
    }

    public InvalidGmailGrantException(String message, Throwable cause) {
        super(message, cause);
    }
}
