package com.hms.service;

import com.hms.common.DateRange;
import com.hms.model.RoomType;
import com.hms.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

public class BookingRequest {

    private final String propertyId;
    private final String roomTypeId;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final String guestName;
    private final String guestEmail;

    public BookingRequest(String propertyId, String roomTypeId, LocalDate checkIn, LocalDate checkOut,
                          int guests, String guestName, String guestEmail) {
        this.propertyId = propertyId;
        this.roomTypeId = roomTypeId;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
    }

    public DateRange stay() {
        return new DateRange(checkIn, checkOut);
    }

    public String getPropertyId() { return propertyId; }
    public String getRoomTypeId() { return roomTypeId; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public int getGuests() { return guests; }
    public String getGuestName() { return guestName; }
    public String getGuestEmail() { return guestEmail; }

    /** Free rooms are the room type's total minus the bookings holding a room, counted night by night. */
    @Service
    public static class AvailabilityService {

        private final BookingRepository bookings;

        public AvailabilityService(BookingRepository bookings) {
            this.bookings = bookings;
        }

        public int availableRooms(RoomType roomType, DateRange stay) {
            var active = bookings.findActiveByRoomType(roomType.getId());
            int busiestNight = stay.eachNight()
                    .mapToInt(night -> (int) active.stream().filter(b -> b.getStay().covers(night)).count())
                    .max().orElse(0);
            return roomType.getTotalRooms() - busiestNight;
        }
    }
}
