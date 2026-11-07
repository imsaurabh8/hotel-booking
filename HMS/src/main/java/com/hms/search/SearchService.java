package com.hms.search;

import com.hms.common.ApiException;
import com.hms.common.DateRange;
import com.hms.model.Property;
import com.hms.model.RoomType;
import com.hms.repository.PropertyRepository;
import com.hms.service.BookingRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class SearchService {

    private final PropertyRepository properties;
    private final BookingRequest.AvailabilityService availability;
    private final List<SearchFilter> filters;

    public SearchService(PropertyRepository properties, BookingRequest.AvailabilityService availability, List<SearchFilter> filters) {
        this.properties = properties;
        this.availability = availability;
        this.filters = filters;
    }

    /**
     * Properties that have a free room fitting the party and passing every filter, cheapest first.
     * City and locality are optional; params holds the filter parameters (minPrice, amenities, ...).
     */
    public List<SearchResult> search(String city, String locality, DateRange stay, int guests, Map<String, String> params) {
        if (guests < 1) {
            throw ApiException.badRequest("At least one guest is required");
        }

        List<SearchResult> results = new ArrayList<>();
        for (Property property : properties.findAll()) {
            if (!matches(city, property.getCity()) || !matches(locality, property.getLocality())) {
                continue;
            }
            List<SearchResult.RoomOption> rooms = findFreeRooms(property, stay, guests, params);
            if (!rooms.isEmpty()) {
                results.add(new SearchResult(property, rooms));
            }
        }
        results.sort(Comparator.comparing(SearchService::cheapestStay));
        return results;
    }

    private List<SearchResult.RoomOption> findFreeRooms(Property property, DateRange stay, int guests, Map<String, String> params) {
        List<SearchResult.RoomOption> rooms = new ArrayList<>();
        for (RoomType room : property.getRoomTypes()) {
            if (room.getCapacity() < guests || !passesFilters(property, room, params)) {
                continue;
            }
            int freeRooms = availability.availableRooms(room, stay);
            if (freeRooms > 0) {
                BigDecimal stayPrice = room.getPricePerNight().multiply(BigDecimal.valueOf(stay.nights()));
                rooms.add(new SearchResult.RoomOption(room, freeRooms, stayPrice));
            }
        }
        return rooms;
    }

    private boolean passesFilters(Property property, RoomType room, Map<String, String> params) {
        for (SearchFilter filter : filters) {
            if (!filter.matches(property, room, params)) {
                return false;
            }
        }
        return true;
    }

    /** An empty or missing search value matches anything; otherwise compare ignoring case. */
    private static boolean matches(String wanted, String actual) {
        return wanted == null || wanted.isBlank() || wanted.trim().equalsIgnoreCase(actual);
    }

    private static BigDecimal cheapestStay(SearchResult result) {
        BigDecimal cheapest = result.getRooms().get(0).getTotalPrice();
        for (SearchResult.RoomOption room : result.getRooms()) {
            if (room.getTotalPrice().compareTo(cheapest) < 0) {
                cheapest = room.getTotalPrice();
            }
        }
        return cheapest;
    }
}
