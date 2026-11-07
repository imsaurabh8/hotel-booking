package com.hms;

import com.hms.common.ApiException;
import com.hms.model.Booking;
import com.hms.model.BookingStatus;
import com.hms.model.Property;
import com.hms.payment.CardPayment;
import com.hms.payment.PaymentMethod;
import com.hms.payment.PaymentResult;
import com.hms.payment.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.hms.Fixture.DECLINED_CARD;
import static com.hms.Fixture.GOOD_CARD;
import static com.hms.Fixture.stay;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentServiceTest {

    private final Fixture f = new Fixture();
    private Property hotel;
    private Booking booking;

    @BeforeEach
    void setUp() {
        hotel = f.standardHotel();
        booking = f.book(hotel, stay(10, 2));
    }

    @Test
    void successfulPaymentConfirmsTheBooking() {
        Booking paid = f.payments.pay(booking.getId(), "CARD", GOOD_CARD);

        assertThat(paid.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(paid.getPaymentMethod()).isEqualTo("CARD");
        assertThat(paid.getTransactionId()).startsWith("txn_");
    }

    @Test
    void upiAndWalletWork() {
        Booking second = f.book(hotel, stay(10, 2));

        assertThat(f.payments.pay(booking.getId(), "upi", Map.of("vpa", "ravi@okbank")).getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(f.payments.pay(second.getId(), "WALLET", Map.of("walletId", "wallet-1")).getStatus())
                .isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void declinedPaymentFailsTheBookingAndFreesTheRoom() {
        f.book(hotel, stay(10, 2)); // takes the other room, so the hotel is full
        assertThat(f.availability.availableRooms(hotel.getRoomTypes().get(0), stay(10, 2))).isZero();

        Booking failed = f.payments.pay(booking.getId(), "CARD", DECLINED_CARD);

        assertThat(failed.getStatus()).isEqualTo(BookingStatus.PAYMENT_FAILED);
        assertThat(failed.getFailureReason()).isEqualTo("Declined by the payment provider");
        assertThat(f.availability.availableRooms(hotel.getRoomTypes().get(0), stay(10, 2))).isEqualTo(1);
    }

    @Test
    void anyMethodDeclinedByTheProviderFailsTheBooking() {
        Booking wallet = f.payments.pay(booking.getId(), "WALLET", Map.of("walletId", "fail-wallet"));
        Booking upi = f.payments.pay(f.book(hotel, stay(10, 2)).getId(), "UPI", Map.of("vpa", "fail@okbank"));

        assertThat(wallet.getStatus()).isEqualTo(BookingStatus.PAYMENT_FAILED);
        assertThat(upi.getStatus()).isEqualTo(BookingStatus.PAYMENT_FAILED);
    }

    @Test
    void cannotPayTwiceOrAfterAFailure() {
        f.payments.pay(booking.getId(), "CARD", GOOD_CARD);
        assertThatThrownBy(() -> f.payments.pay(booking.getId(), "CARD", GOOD_CARD)).isInstanceOf(ApiException.class);

        Booking other = f.book(hotel, stay(10, 2));
        f.payments.pay(other.getId(), "CARD", DECLINED_CARD);
        assertThatThrownBy(() -> f.payments.pay(other.getId(), "CARD", GOOD_CARD)).isInstanceOf(ApiException.class);
    }

    @Test
    void unsupportedMethodAndMalformedDetailsAreRejectedAndBookingStaysPending() {
        assertThatThrownBy(() -> f.payments.pay(booking.getId(), "CRYPTO", Map.of())).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> f.payments.pay(booking.getId(), "CARD", Map.of("cardNumber", "123", "cvv", "123")))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> f.payments.pay(booking.getId(), "UPI", Map.of("vpa", "nope"))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> f.payments.pay(booking.getId(), "WALLET", null)).isInstanceOf(ApiException.class);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    @Test
    void newPaymentMethodsPlugInWithoutChangingExistingCode() {
        PaymentMethod netBanking = new PaymentMethod() {
            public String type() { return "NETBANKING"; }
            public PaymentResult pay(BigDecimal amount, Map<String, String> details) { return PaymentResult.ok(); }
        };
        PaymentService service = new PaymentService(f.bookingRepository, f.gateway, List.of(new CardPayment(f.gateway), netBanking));

        assertThat(service.pay(booking.getId(), "netbanking", Map.of()).getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void simultaneousPaymentsOnOneBookingChargeOnlyOnce() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            results.add(pool.submit(() -> {
                start.await();
                try {
                    f.payments.pay(booking.getId(), "CARD", GOOD_CARD);
                    return true;
                } catch (ApiException alreadyPaid) {
                    return false;
                }
            }));
        }
        start.countDown();
        int charged = 0;
        for (Future<Boolean> r : results) {
            charged += r.get() ? 1 : 0;
        }
        pool.shutdown();

        assertThat(charged).isEqualTo(1);
    }
}
