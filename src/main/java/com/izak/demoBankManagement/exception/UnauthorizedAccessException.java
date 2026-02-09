package com.izak.demoBankManagement.exception;


/**
 * Exception thrown when a user attempts to access a resource they don't have permission for.
 * This is used for authorization failures (not authentication failures).
 *
 * Typically thrown when:
 * - A CUSTOMER tries to access another customer's accounts
 * - A BRANCH_MANAGER tries to access accounts from a different branch
 * - A LOAN_OFFICER tries to access cards or other non-loan resources
 * - A CARD_OFFICER tries to access loans or other non-card resources
 */
public class UnauthorizedAccessException extends RuntimeException {

    /**
     * Constructs a new UnauthorizedAccessException with the specified detail message.
     *
     * @param message the detail message explaining the authorization failure
     */
    public UnauthorizedAccessException(String message) {
        super(message);
    }

    /**
     * Constructs a new UnauthorizedAccessException with the specified detail message and cause.
     *
     * @param message the detail message explaining the authorization failure
     * @param cause the cause of the exception
     */
    public UnauthorizedAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new UnauthorizedAccessException with a default message.
     */
    public UnauthorizedAccessException() {
        super("Access denied: You do not have permission to access this resource");
    }
}