package com.hms.model;

import java.util.Set;

/**
 * PENDING_PAYMENT -> CONFIRMED | PAYMENT_FAILED | CANCELLED
 * CONFIRMED       -> CANCELLED
 * PAYMENT_FAILED and CANCELLED are final.
 */
public enum BookingStatus {
    PENDING_PAYMENT, CONFIRMED, PAYMENT_FAILED, CANCELLED;

    public boolean canMoveTo(BookingStatus next) {
        Set<BookingStatus> allowed = switch (this) {
            case PENDING_PAYMENT -> Set.of(CONFIRMED, PAYMENT_FAILED, CANCELLED);
            case CONFIRMED -> Set.of(CANCELLED);
            case PAYMENT_FAILED, CANCELLED -> Set.of();
        };
        return allowed.contains(next);
    }
}
