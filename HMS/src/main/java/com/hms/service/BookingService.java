package com.hms.service;

import com.hms.cancellation.CancellationPolicy;
import com.hms.common.ApiException;
import com.hms.common.DateRange;
import com.hms.model.Booking;
import com.hms.model.BookingStatus;
import com.hms.model.Property;
import com.hms.model.RoomType;
import com.hms.payment.PaymentService;
import com.hms.repository.BookingRepository;
import com.hms.repository.PropertyRepository;
import com.hms.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BookingService {

    private static final int MAX_NIGHTS = 30;

    private final BookingRepository bookings;
    private final PropertyRepository properties;
    private final UserRepository users;
    private final BookingRequest.AvailabilityService availability;
    private final PaymentService payments;
    private final CancellationPolicy cancellationPolicy;
    private final Clock clock;
    private final Map<String, Object> roomTypeLocks = new ConcurrentHashMap<>();

    public BookingService(BookingRepository bookings, PropertyRepository properties, UserRepository users,
                          BookingRequest.AvailabilityService availability,
                          PaymentService payments, CancellationPolicy cancellationPolicy, Clock clock) {
        this.bookings = bookings;
        this.properties = properties;
        this.users = users;
        this.availability = availability;
        this.payments = payments;
        this.cancellationPolicy = cancellationPolicy;
        this.clock = clock;
    }

    public Booking book(BookingRequest request) {
        users.findById(request.getUserId()).orElseThrow(() -> ApiException.notFound("User", request.getUserId()));
        Property property = properties.findById(request.getPropertyId())
                .orElseThrow(() -> ApiException.notFound("Property", request.getPropertyId()));
        RoomType room = property.roomType(request.getRoomTypeId());
        DateRange stay = request.stay();
        validate(request, room, stay);

        // The availability check and the save must be one step per room type, otherwise two requests
        // could both see the last room as free.
        synchronized (roomTypeLocks.computeIfAbsent(room.getId(), id -> new Object())) {
            if (availability.availableRooms(room, stay) < 1) {
                throw ApiException.conflict("No '" + room.getName() + "' rooms left for " + stay.getCheckIn() + " to " + stay.getCheckOut());
            }
            BigDecimal total = room.getPricePerNight().multiply(BigDecimal.valueOf(stay.nights()));
            return bookings.save(new Booking(property.getId(), room.getId(), request.getUserId(),
                    stay, request.getGuests(), total));
        }
    }

    public Booking get(String bookingId) {
        return bookings.getOrThrow(bookingId);
    }

    /** Cancelling frees the room automatically because only PENDING/CONFIRMED bookings count as occupying one. */
    public Booking cancel(String bookingId) {
        Booking booking = bookings.getOrThrow(bookingId);
        synchronized (booking) {
            LocalDate today = LocalDate.now(clock);
            if (!booking.canBeCancelled()) {
                throw ApiException.conflict("Booking is " + booking.getStatus() + " and cannot be cancelled");
            }
            if (!today.isBefore(booking.getStay().getCheckOut())) {
                throw ApiException.conflict("The stay is already over");
            }
            BigDecimal refund = BigDecimal.ZERO;
            if (booking.getStatus() == BookingStatus.CONFIRMED) {
                refund = cancellationPolicy.refundAmount(booking, today);
                if (refund.signum() > 0) {
                    payments.refund(booking, refund); // refund first: if it fails the booking stays as it was
                }
            }
            booking.cancel(refund);
            return bookings.save(booking);
        }
    }

    private void validate(BookingRequest request, RoomType room, DateRange stay) {
        if (stay.getCheckIn().isBefore(LocalDate.now(clock))) {
            throw ApiException.badRequest("Check-in cannot be in the past");
        }
        if (stay.nights() > MAX_NIGHTS) {
            throw ApiException.badRequest("Stays are limited to " + MAX_NIGHTS + " nights");
        }
        if (request.getGuests() < 1 || request.getGuests() > room.getCapacity()) {
            throw ApiException.badRequest("'" + room.getName() + "' takes 1 to " + room.getCapacity() + " guests");
        }
    }
}
