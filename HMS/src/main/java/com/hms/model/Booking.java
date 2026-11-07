package com.hms.model;

import com.hms.common.ApiException;
import com.hms.common.DateRange;

import java.math.BigDecimal;
import java.util.UUID;

/** Mutable fields are volatile: they change under the booking lock but are read elsewhere (availability, API). */
public class Booking {

    private final String id = UUID.randomUUID().toString();
    private final String propertyId;
    private final String roomTypeId;
    private final String guestName;
    private final String guestEmail;
    private final DateRange stay;
    private final int guests;
    private final BigDecimal totalAmount;
    private volatile BookingStatus status = BookingStatus.PENDING_PAYMENT;
    private volatile String paymentMethod;
    private volatile String transactionId;
    private volatile String failureReason;
    private volatile BigDecimal refundAmount;

    public Booking(String propertyId, String roomTypeId, String guestName, String guestEmail,
                   DateRange stay, int guests, BigDecimal totalAmount) {
        this.propertyId = propertyId;
        this.roomTypeId = roomTypeId;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.stay = stay;
        this.guests = guests;
        this.totalAmount = totalAmount;
    }

    public void confirm(String paymentMethod, String transactionId) {
        moveTo(BookingStatus.CONFIRMED);
        this.paymentMethod = paymentMethod;
        this.transactionId = transactionId;
    }

    public void paymentFailed(String reason) {
        moveTo(BookingStatus.PAYMENT_FAILED);
        this.failureReason = reason;
    }

    public void cancel(BigDecimal refund) {
        moveTo(BookingStatus.CANCELLED);
        this.refundAmount = refund;
    }

    public boolean canBeCancelled() {
        return status.canMoveTo(BookingStatus.CANCELLED);
    }

    /** Pending and confirmed bookings occupy a room; failed and cancelled ones do not. */
    public boolean holdsRoom() {
        return status == BookingStatus.PENDING_PAYMENT || status == BookingStatus.CONFIRMED;
    }

    private void moveTo(BookingStatus next) {
        if (!status.canMoveTo(next)) {
            throw ApiException.conflict("Booking is " + status + ", it cannot become " + next);
        }
        status = next;
    }

    public String getId() { return id; }
    public String getPropertyId() { return propertyId; }
    public String getRoomTypeId() { return roomTypeId; }
    public String getGuestName() { return guestName; }
    public String getGuestEmail() { return guestEmail; }
    public DateRange getStay() { return stay; }
    public int getGuests() { return guests; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BookingStatus getStatus() { return status; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getTransactionId() { return transactionId; }
    public String getFailureReason() { return failureReason; }
    public BigDecimal getRefundAmount() { return refundAmount; }
}
