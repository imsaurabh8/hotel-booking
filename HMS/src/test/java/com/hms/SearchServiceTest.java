package com.hms;

import com.hms.common.ApiException;
import com.hms.model.Booking;
import com.hms.model.Property;
import com.hms.search.SearchFilter;
import com.hms.search.SearchResult;
import com.hms.search.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.hms.Fixture.GOOD_CARD;
import static com.hms.Fixture.hotel;
import static com.hms.Fixture.room;
import static com.hms.Fixture.stay;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SearchServiceTest {

    private final Fixture f = new Fixture();
    private Property seaView; // Mumbai/Bandra, 4 stars, wifi+pool, 2 Deluxe rooms (2 guests) at 5000

    @BeforeEach
    void setUp() {
        seaView = f.standardHotel();
        String owner = f.newOwner().getId();
        f.propertyService.addProperty(owner, hotel("Budget Inn", "Mumbai", "Colaba", 2, Set.of("wifi"),
                room("Basic", 2, 1, "1500"), room("Family", 4, 1, "4000")));
        f.propertyService.addProperty(owner, hotel("Beach Villa", "Goa", "Calangute", 5, Set.of("spa", "pool"),
                room("Villa", 6, 1, "9000")));
    }

    private List<String> names(String city, String locality, int guests, Map<String, String> params) {
        return f.search.search(city, locality, stay(10, 2), guests, params).stream().map(r -> r.getProperty().getName()).toList();
    }

    @Test
    void searchesByCityAndLocalityIgnoringCase() {
        assertThat(names("mumbai", null, 1, Map.of())).containsExactlyInAnyOrder("Sea View", "Budget Inn");
        assertThat(names("Mumbai", "colaba", 1, Map.of())).containsExactly("Budget Inn");
        assertThat(names(null, null, 1, Map.of())).hasSize(3);
    }

    @Test
    void ordersByCheapestStayFirst() {
        assertThat(names(null, null, 1, Map.of())).containsExactly("Budget Inn", "Sea View", "Beach Villa");
    }

    @Test
    void onlyOffersRoomsThatFitTheParty() {
        List<SearchResult> results = f.search.search("Mumbai", "Colaba", stay(10, 2), 3, Map.of());

        assertThat(results.get(0).getRooms()).extracting(r -> r.getRoomType().getName()).containsExactly("Family");
        assertThat(names("Mumbai", null, 5, Map.of())).isEmpty();
    }

    @Test
    void showsStayPriceAndRoomsLeft() {
        f.book(seaView, stay(10, 2));

        SearchResult.RoomOption option = f.search.search("Mumbai", "Bandra", stay(10, 2), 2, Map.of()).get(0).getRooms().get(0);

        assertThat(option.getTotalPrice()).isEqualByComparingTo("10000");
        assertThat(option.getAvailableRooms()).isEqualTo(1);
    }

    @Test
    void hidesFullyBookedHotelsUntilABookingIsCancelled() {
        Booking first = f.bookAndPay(seaView, stay(10, 2));
        f.book(seaView, stay(10, 2));
        assertThat(names("Mumbai", "Bandra", 2, Map.of())).isEmpty();

        f.bookings.cancel(first.getId());

        assertThat(names("Mumbai", "Bandra", 2, Map.of())).containsExactly("Sea View");
    }

    @Test
    void bookingsOnOtherDatesDoNotAffectAvailability() {
        f.book(seaView, stay(30, 2));
        f.book(seaView, stay(30, 2));

        assertThat(names("Mumbai", "Bandra", 2, Map.of())).containsExactly("Sea View");
    }

    @Test
    void filtersByPriceAmenitiesAndStars() {
        assertThat(names("Mumbai", null, 2, Map.of("minPrice", "1000", "maxPrice", "2000"))).containsExactly("Budget Inn");
        assertThat(names(null, null, 1, Map.of("amenities", "POOL, wifi"))).containsExactly("Sea View");
        assertThat(names(null, null, 1, Map.of("minStars", "4"))).containsExactlyInAnyOrder("Sea View", "Beach Villa");
        assertThat(names(null, null, 1, Map.of("maxPrice", "6000", "amenities", "pool", "minStars", "4"))).containsExactly("Sea View");
    }

    @Test
    void priceFilterAppliesToTheRoomNotJustTheHotel() {
        List<SearchResult> results = f.search.search("Mumbai", "Colaba", stay(10, 2), 2, Map.of("minPrice", "1000", "maxPrice", "2000"));

        assertThat(results.get(0).getRooms()).extracting(r -> r.getRoomType().getName()).containsExactly("Basic");
    }

    @Test
    void rejectsZeroGuests() {
        assertThatThrownBy(() -> names(null, null, 0, Map.of())).isInstanceOf(ApiException.class);
    }

    @Test
    void newFiltersPlugInWithoutTouchingTheSearchService() {
        SearchFilter namePrefix = (property, room, params) ->
                !params.containsKey("namePrefix") || property.getName().startsWith(params.get("namePrefix"));
        SearchService custom = new SearchService(f.propertyRepository, f.availability, List.of(namePrefix));

        List<SearchResult> results = custom.search(null, null, stay(10, 2), 1, Map.of("namePrefix", "Beach"));

        assertThat(results).extracting(r -> r.getProperty().getName()).containsExactly("Beach Villa");
    }
}
