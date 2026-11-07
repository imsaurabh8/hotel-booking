package com.hms;

import com.hms.common.ApiException;
import com.hms.common.DateRange;
import com.hms.model.Booking;
import com.hms.model.BookingStatus;
import com.hms.model.Property;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.hms.Fixture.DECLINED_CARD;
import static com.hms.Fixture.TODAY;
import static com.hms.Fixture.stay;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CancellationTest {

    private final Fixture f = new Fixture();
    private Property hotel;

    @BeforeEach
    void setUp() {
        hotel = f.standardHotel();
    }

    private BigDecimal refundWhenCheckInIsInDays(int days) {
        Booking paid = f.bookAndPay(hotel, stay(days, 2)); // 2 nights x 5000 = 10000
        return f.bookings.cancel(paid.getId()).getRefundAmount();
    }

    @Test
    void fullRefundAtLeastSevenDaysBeforeCheckIn() {
        assertThat(refundWhenCheckInIsInDays(7)).isEqualByComparingTo("10000");
    }

    @Test
    void halfRefundBetweenTwoAndSixDays() {
        assertThat(refundWhenCheckInIsInDays(6)).isEqualByComparingTo("5000");
        assertThat(refundWhenCheckInIsInDays(2)).isEqualByComparingTo("5000");
    }

    @Test
    void noRefundInsideTwoDays() {
        assertThat(refundWhenCheckInIsInDays(1)).isEqualByComparingTo("0");
    }

    @Test
    void cancellingFreesTheRoomForOtherGuests() {
        Booking first = f.bookAndPay(hotel, stay(20, 2));
        f.book(hotel, stay(20, 2));
        assertThatThrownBy(() -> f.book(hotel, stay(20, 2))).isInstanceOf(ApiException.class);

        Booking cancelled = f.bookings.cancel(first.getId());

        assertThat(cancelled.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(f.book(hotel, stay(20, 2)).getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    @Test
    void unpaidBookingCancelsWithNoRefund() {
        Booking b = f.book(hotel, stay(20, 2));

        Booking cancelled = f.bookings.cancel(b.getId());

        assertThat(cancelled.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(cancelled.getRefundAmount()).isEqualByComparingTo("0");
    }

    @Test
    void cannotCancelTwice() {
        Booking b = f.bookAndPay(hotel, stay(20, 2));
        f.bookings.cancel(b.getId());

        assertThatThrownBy(() -> f.bookings.cancel(b.getId())).isInstanceOf(ApiException.class);
    }

    @Test
    void cannotCancelAFailedBooking() {
        Booking b = f.book(hotel, stay(20, 2));
        f.payments.pay(b.getId(), "CARD", DECLINED_CARD);

        assertThatThrownBy(() -> f.bookings.cancel(b.getId())).isInstanceOf(ApiException.class);
    }

    @Test
    void cannotCancelOnceTheStayIsOver() {
        Booking old = new Booking(hotel.getId(), hotel.getRoomTypes().get(0).getId(), f.user.getId(),
                new DateRange(TODAY.minusDays(5), TODAY.minusDays(2)), 2, new BigDecimal("100"));
        f.bookingRepository.save(old);

        assertThatThrownBy(() -> f.bookings.cancel(old.getId()))
                .isInstanceOf(ApiException.class).hasMessageContaining("over");
    }
}
