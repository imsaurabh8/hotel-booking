package com.hms;

import com.hms.cancellation.TieredCancellationPolicy;
import com.hms.common.DateRange;
import com.hms.model.Booking;
import com.hms.model.Owner;
import com.hms.model.Property;
import com.hms.model.User;
import com.hms.payment.CardPayment;
import com.hms.payment.MockPaymentGateway;
import com.hms.payment.PaymentService;
import com.hms.payment.UpiPayment;
import com.hms.payment.WalletPayment;
import com.hms.repository.InMemoryBookingRepository;
import com.hms.repository.InMemoryOwnerRepository;
import com.hms.repository.InMemoryPropertyRepository;
import com.hms.repository.InMemoryUserRepository;
import com.hms.search.AmenityFilter;
import com.hms.search.PriceFilter;
import com.hms.search.SearchService;
import com.hms.search.StarRatingFilter;
import com.hms.service.BookingRequest;
import com.hms.service.BookingService;
import com.hms.service.PropertyRequest;
import com.hms.service.PropertyRequest.RoomTypeRequest;
import com.hms.service.PropertyService;
import com.hms.service.UserService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Wires the real classes by hand over in-memory repositories; "today" is fixed at 2026-11-06. */
public class Fixture {

    public static final LocalDate TODAY = LocalDate.of(2026, 11, 6);
    public static final Map<String, String> GOOD_CARD = Map.of("cardNumber", "4111111111111111", "cvv", "123");
    public static final Map<String, String> DECLINED_CARD = Map.of("cardNumber", "4000000000000002", "cvv", "123");

    public final Clock clock = Clock.fixed(TODAY.atTime(10, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    public final InMemoryBookingRepository bookingRepository = new InMemoryBookingRepository();
    public final InMemoryPropertyRepository propertyRepository = new InMemoryPropertyRepository();
    public final InMemoryUserRepository userRepository = new InMemoryUserRepository();
    public final UserService userService = new UserService(userRepository, bookingRepository);
    public final User user = userService.register("Ravi", "ravi@example.com");
    public final PropertyService propertyService = new PropertyService(new InMemoryOwnerRepository(), propertyRepository, userRepository);
    public final BookingRequest.AvailabilityService availability = new BookingRequest.AvailabilityService(bookingRepository);
    public final MockPaymentGateway gateway = new MockPaymentGateway();
    public final PaymentService payments = new PaymentService(bookingRepository, gateway,
            List.of(new CardPayment(gateway), new UpiPayment(gateway), new WalletPayment(gateway)));
    public final BookingService bookings = new BookingService(bookingRepository, propertyRepository, userRepository, availability,
            payments, new TieredCancellationPolicy(), clock);
    public final SearchService search = new SearchService(propertyRepository, availability,
            List.of(new PriceFilter(), new AmenityFilter(), new StarRatingFilter()));

    public static RoomTypeRequest room(String name, int capacity, int rooms, String price) {
        return new RoomTypeRequest(name, capacity, rooms, new BigDecimal(price));
    }

    public static PropertyRequest hotel(String name, String city, String locality, int stars,
                                        Set<String> amenities, RoomTypeRequest... rooms) {
        return new PropertyRequest(name, city, locality, stars, amenities, List.of(rooms));
    }

    private int userCount = 0;

    /** A new user with a unique email. */
    public User newUser() {
        userCount++;
        return userService.register("User " + userCount, "user" + userCount + "@example.com");
    }

    /** A new user who is also a host (no properties yet). */
    public Owner newOwner() {
        return propertyService.registerOwner(newUser().getId(), "Asha", List.of());
    }

    /** 4-star Mumbai/Bandra hotel with 2 Deluxe rooms (2 guests, 5000 a night). */
    public Property standardHotel() {
        return propertyService.addProperty(newOwner().getId(),
                hotel("Sea View", "Mumbai", "Bandra", 4, Set.of("wifi", "pool"), room("Deluxe", 2, 2, "5000")));
    }

    public static DateRange stay(int daysFromToday, int nights) {
        LocalDate in = TODAY.plusDays(daysFromToday);
        return new DateRange(in, in.plusDays(nights));
    }

    public Booking book(Property property, DateRange stay) {
        return bookings.book(new BookingRequest(property.getId(), property.getRoomTypes().get(0).getId(),
                stay.getCheckIn(), stay.getCheckOut(), 2, user.getId()));
    }

    public Booking bookAndPay(Property property, DateRange stay) {
        Booking booking = book(property, stay);
        return payments.pay(booking.getId(), "CARD", GOOD_CARD);
    }
}
