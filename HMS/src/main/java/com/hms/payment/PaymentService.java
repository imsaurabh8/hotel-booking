package com.hms.payment;

import com.hms.common.ApiException;
import com.hms.model.Booking;
import com.hms.model.BookingStatus;
import com.hms.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Takes payment for a booking and lets the outcome decide its state: success confirms, failure frees the room. */
@Service
public class PaymentService {

    private final BookingRepository bookings;
    private final PaymentGateway gateway;
    private final Map<String, PaymentMethod> methods;

    public PaymentService(BookingRepository bookings, PaymentGateway gateway, List<PaymentMethod> methods) {
        this.bookings = bookings;
        this.gateway = gateway;
        this.methods = methods.stream().collect(Collectors.toMap(PaymentMethod::type, Function.identity()));
    }

    public Booking pay(String bookingId, String methodType, Map<String, String> details) {
        PaymentMethod method = methods.get(methodType == null ? "" : methodType.toUpperCase());
        if (method == null) {
            throw ApiException.badRequest("Unsupported payment method '" + methodType + "', use one of " + methods.keySet());
        }
        Booking booking = bookings.getOrThrow(bookingId);
        synchronized (booking) { // one payment at a time per booking, so a double click cannot charge twice
            if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
                throw ApiException.conflict("Booking is " + booking.getStatus() + " and cannot be paid");
            }
            PaymentResult result = method.pay(booking.getTotalAmount(), details == null ? Map.of() : details);
            if (result.isSuccess()) {
                booking.confirm(method.type(), result.getTransactionId());
            } else {
                booking.paymentFailed(result.getMessage());
            }
            return bookings.save(booking);
        }
    }

    /** Refunds part or all of the booking's payment through the gateway. */
    public void refund(Booking booking, BigDecimal amount) {
        gateway.refund(booking.getTransactionId(), amount);
    }
}
