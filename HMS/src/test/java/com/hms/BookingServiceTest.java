package com.hms;

import com.hms.common.ApiException;
import com.hms.common.DateRange;
import com.hms.model.Booking;
import com.hms.model.BookingStatus;
import com.hms.model.Property;
import com.hms.service.BookingRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.hms.Fixture.TODAY;
import static com.hms.Fixture.stay;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingServiceTest {

    private final Fixture f = new Fixture();
    private Property hotel;

    @BeforeEach
    void setUp() {
        hotel = f.standardHotel();
    }

    private BookingRequest request(DateRange stay, int guests) {
        return new BookingRequest(hotel.getId(), hotel.getRoomTypes().get(0).getId(), stay.getCheckIn(), stay.getCheckOut(),
                guests, "Ravi", "ravi@example.com");
    }

    @Test
    void createsPendingBookingPricedByNights() {
        Booking b = f.bookings.book(request(stay(10, 3), 2));

        assertThat(b.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(b.getTotalAmount()).isEqualByComparingTo(new BigDecimal("15000"));
    }

    @Test
    void cannotBookMoreRoomsThanExist() {
        f.book(hotel, stay(10, 3));
        f.book(hotel, stay(11, 3));

        assertThatThrownBy(() -> f.book(hotel, stay(12, 1)))
                .isInstanceOf(ApiException.class).hasMessageContaining("No 'Deluxe' rooms left");
    }

    @Test
    void sameRoomsAreBookableOnOtherDatesAndBackToBack() {
        f.book(hotel, stay(10, 3));
        f.book(hotel, stay(10, 3));

        assertThat(f.book(hotel, stay(13, 2)).getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT); // checkout day = next checkin
    }

    @Test
    void stayIsBlockedIfAnySingleNightIsFull() {
        f.book(hotel, stay(12, 1));
        f.book(hotel, stay(12, 1));

        assertThatThrownBy(() -> f.book(hotel, stay(10, 5))).isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsBadGuestCounts() {
        assertThatThrownBy(() -> f.bookings.book(request(stay(10, 2), 3))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> f.bookings.book(request(stay(10, 2), 0))).isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsPastCheckInButAllowsToday() {
        DateRange past = new DateRange(TODAY.minusDays(1), TODAY.plusDays(1));
        assertThatThrownBy(() -> f.bookings.book(request(past, 2))).isInstanceOf(ApiException.class);
        assertThat(f.bookings.book(request(stay(0, 1), 2))).isNotNull();
    }

    @Test
    void rejectsOverlongStaysAndBadDates() {
        assertThatThrownBy(() -> f.bookings.book(request(stay(10, 31), 2))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> new DateRange(TODAY.plusDays(5), TODAY.plusDays(5))).isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsUnknownPropertyRoomAndBooking() {
        assertThatThrownBy(() -> f.bookings.book(new BookingRequest("x", "y", TODAY, TODAY.plusDays(1), 1, "R", "r@x.com")))
                .isInstanceOf(ApiException.class).hasMessageContaining("not found");
        assertThatThrownBy(() -> f.bookings.book(new BookingRequest(hotel.getId(), "y", TODAY, TODAY.plusDays(1), 1, "R", "r@x.com")))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> f.bookings.get("nope")).isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsMissingGuestDetails() {
        assertThatThrownBy(() -> f.bookings.book(new BookingRequest(hotel.getId(), hotel.getRoomTypes().get(0).getId(),
                TODAY, TODAY.plusDays(1), 1, " ", "r@x.com"))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> f.bookings.book(new BookingRequest(hotel.getId(), hotel.getRoomTypes().get(0).getId(),
                TODAY, TODAY.plusDays(1), 1, "Ravi", "bad"))).isInstanceOf(ApiException.class);
    }

    @Test
    void statusCannotJumpIllegally() {
        Booking b = f.book(hotel, stay(10, 2));
        b.confirm("CARD", "t1");

        assertThatThrownBy(() -> b.confirm("CARD", "t2")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> b.paymentFailed("x")).isInstanceOf(ApiException.class);
    }

    @Test
    void simultaneousRequestsForTheLastRoomsNeverOversell() throws Exception {
        int attempts = 30;
        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            results.add(pool.submit(() -> {
                start.await();
                try {
                    f.book(hotel, stay(20, 3));
                    return true;
                } catch (ApiException soldOut) {
                    return false;
                }
            }));
        }
        start.countDown();
        int booked = 0;
        for (Future<Boolean> r : results) {
            booked += r.get() ? 1 : 0;
        }
        pool.shutdown();

        assertThat(booked).isEqualTo(2); // the hotel has 2 Deluxe rooms
    }
}
