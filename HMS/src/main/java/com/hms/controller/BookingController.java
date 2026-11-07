package com.hms.controller;

import com.hms.model.Booking;
import com.hms.model.BookingStatus;
import com.hms.payment.PaymentService;
import com.hms.service.BookingRequest;
import com.hms.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    /** method is CARD, UPI or WALLET; details holds that method's fields. */
    public static class PaymentRequest {

        private final String method;
        private final Map<String, String> details;

        public PaymentRequest(String method, Map<String, String> details) {
            this.method = method;
            this.details = details;
        }

        public String getMethod() { return method; }
        public Map<String, String> getDetails() { return details; }
    }

    private final BookingService bookingService;
    private final PaymentService paymentService;

    public BookingController(BookingService bookingService, PaymentService paymentService) {
        this.bookingService = bookingService;
        this.paymentService = paymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Booking book(@RequestBody BookingRequest request) {
        return bookingService.book(request);
    }

    @GetMapping("/{bookingId}")
    public Booking get(@PathVariable String bookingId) {
        return bookingService.get(bookingId);
    }

    /** 200 when paid, 402 when the provider declines (the booking is then PAYMENT_FAILED). */
    @PostMapping("/{bookingId}/payment")
    public ResponseEntity<Booking> pay(@PathVariable String bookingId, @RequestBody PaymentRequest request) {
        Booking booking = paymentService.pay(bookingId, request.getMethod(), request.getDetails());
        HttpStatus status = booking.getStatus() == BookingStatus.PAYMENT_FAILED ? HttpStatus.PAYMENT_REQUIRED : HttpStatus.OK;
        return ResponseEntity.status(status).body(booking);
    }

    @PostMapping("/{bookingId}/cancel")
    public Booking cancel(@PathVariable String bookingId) {
        return bookingService.cancel(bookingId);
    }
}
