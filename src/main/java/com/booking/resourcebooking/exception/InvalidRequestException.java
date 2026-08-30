package com.booking.resourcebooking.exception;

/**
 * Thrown for business-rule validation failures that are not
 * covered by simple field-level bean validation annotations,
 * e.g. endTime before startTime.
 */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
