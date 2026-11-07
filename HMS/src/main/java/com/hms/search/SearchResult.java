package com.hms.search;

import com.hms.model.Property;
import com.hms.model.RoomType;

import java.math.BigDecimal;
import java.util.List;

/** A property that can host the search, with the rooms that are free for it. */
public class SearchResult {

    public static class RoomOption {

        private final RoomType roomType;
        private final int availableRooms;
        private final BigDecimal totalPrice;

        public RoomOption(RoomType roomType, int availableRooms, BigDecimal totalPrice) {
            this.roomType = roomType;
            this.availableRooms = availableRooms;
            this.totalPrice = totalPrice;
        }

        public RoomType getRoomType() { return roomType; }
        public int getAvailableRooms() { return availableRooms; }
        public BigDecimal getTotalPrice() { return totalPrice; }
    }

    private final Property property;
    private final List<RoomOption> rooms;

    public SearchResult(Property property, List<RoomOption> rooms) {
        this.property = property;
        this.rooms = rooms;
    }

    public Property getProperty() { return property; }
    public List<RoomOption> getRooms() { return rooms; }
}
