package com.hms.repository;

import com.hms.model.Booking;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryBookingRepository implements BookingRepository {

    private final Map<String, Booking> bookings = new ConcurrentHashMap<>();

    public Booking save(Booking booking) {
        bookings.put(booking.getId(), booking);
        return booking;
    }

    public Optional<Booking> findById(String id) {
        return Optional.ofNullable(bookings.get(id));
    }

    public List<Booking> findActiveByRoomType(String roomTypeId) {
        return bookings.values().stream()
                .filter(b -> b.getRoomTypeId().equals(roomTypeId) && b.holdsRoom())
                .toList();
    }
}
