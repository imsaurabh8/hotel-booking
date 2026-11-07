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
import com.hms.search.SearchResult;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runs the main flow from the command line, without Spring or HTTP:
 * loads dummy data, then searches, books, pays and cancels.
 */
public class HmsDemo {

    private final InMemoryPropertyRepository propertyRepository = new InMemoryPropertyRepository();
    private final InMemoryBookingRepository bookingRepository = new InMemoryBookingRepository();
    private final InMemoryUserRepository userRepository = new InMemoryUserRepository();
    private final MockPaymentGateway gateway = new MockPaymentGateway();

    private final UserService users = new UserService(userRepository, bookingRepository);
    private final PropertyService propertyService = new PropertyService(new InMemoryOwnerRepository(), propertyRepository, userRepository);
    private final BookingRequest.AvailabilityService availability = new BookingRequest.AvailabilityService(bookingRepository);
    private final PaymentService payments = new PaymentService(bookingRepository, gateway,
            List.of(new CardPayment(gateway), new UpiPayment(gateway), new WalletPayment(gateway)));
    private final BookingService bookings = new BookingService(bookingRepository, propertyRepository, userRepository, availability,
            payments, new TieredCancellationPolicy(), Clock.systemDefaultZone());
    private final SearchService search = new SearchService(propertyRepository, availability,
            List.of(new PriceFilter(), new AmenityFilter(), new StarRatingFilter()));

    public static void main(String[] args) {
        HmsDemo demo = new HmsDemo();
        Property seaView = demo.loadDummyData();
        demo.runSampleBookingFlow(seaView);
    }

    /** A chain with two hotels (Sea View in Mumbai, Beach Villa in Goa). Returns Sea View. */
    private Property loadDummyData() {
        User asha = users.register("Asha", "asha@example.com");
        Owner owner = propertyService.registerOwner(asha.getId(), "Asha Hotels", List.of(
                new PropertyRequest("Sea View", "Mumbai", "Bandra", 4, Set.of("wifi", "pool"), List.of(
                        new RoomTypeRequest("Deluxe", 2, 2, new BigDecimal("5000")),
                        new RoomTypeRequest("Family Suite", 4, 1, new BigDecimal("9000")))),
                new PropertyRequest("Beach Villa", "Goa", "Calangute", 5, Set.of("spa", "pool"), List.of(
                        new RoomTypeRequest("Villa", 6, 1, new BigDecimal("12000"))))));

        List<Property> hotels = propertyService.propertiesOf(owner.getId());
        System.out.println("Loaded owner '" + owner.getName() + "' with " + hotels.size() + " properties");
        return hotels.stream().filter(p -> p.getName().equals("Sea View")).findFirst().get();
    }

    /** Search for a room, book it, pay, then cancel and watch the availability change. */
    private void runSampleBookingFlow(Property seaView) {
        DateRange stay = new DateRange(LocalDate.now().plusDays(30), LocalDate.now().plusDays(32));
        Map<String, String> filters = Map.of("amenities", "pool", "maxPrice", "10000");

        System.out.println("\n1. Search Mumbai, " + stay.getCheckIn() + " to " + stay.getCheckOut() + ", 2 guests, pool, max 10000/night");
        printSearch(search.search("Mumbai", null, stay, 2, filters));

        User ravi = users.register("Ravi", "ravi@example.com");
        System.out.println("\n2. Book a Deluxe room as " + ravi.getName());
        String deluxeId = seaView.getRoomTypes().get(0).getId();
        Booking booking = bookings.book(new BookingRequest(seaView.getId(), deluxeId,
                stay.getCheckIn(), stay.getCheckOut(), 2, ravi.getId()));
        System.out.println("   status " + booking.getStatus() + ", total " + booking.getTotalAmount());

        System.out.println("\n3. Pay by UPI");
        booking = payments.pay(booking.getId(), "UPI", Map.of("vpa", "ravi@okbank"));
        System.out.println("   status " + booking.getStatus() + ", transaction " + booking.getTransactionId());

        System.out.println("\n4. Search again (one Deluxe room is now taken)");
        printSearch(search.search("Mumbai", null, stay, 2, filters));

        System.out.println("\n5. Cancel the booking (30 days before check-in, so a full refund)");
        booking = bookings.cancel(booking.getId());
        System.out.println("   status " + booking.getStatus() + ", refund " + booking.getRefundAmount());

        System.out.println("\n6. Search once more (the room is free again)");
        printSearch(search.search("Mumbai", null, stay, 2, filters));

        System.out.println("\n7. " + ravi.getName() + "'s bookings");
        for (Booking b : users.bookingsOf(ravi.getId())) {
            System.out.println("   " + b.getId() + " " + b.getStatus());
        }
    }

    private void printSearch(List<SearchResult> results) {
        for (SearchResult result : results) {
            System.out.println("   " + result.getProperty().getName() + " (" + result.getProperty().getStars() + " stars)");
            for (SearchResult.RoomOption room : result.getRooms()) {
                System.out.println("     " + room.getRoomType().getName() + ": " + room.getAvailableRooms()
                        + " free, stay price " + room.getTotalPrice());
            }
        }
    }
}
