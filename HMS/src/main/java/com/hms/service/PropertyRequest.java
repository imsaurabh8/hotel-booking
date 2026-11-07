package com.hms.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public class PropertyRequest {

    public static class RoomTypeRequest {

        private final String name;
        private final int capacity;
        private final int totalRooms;
        private final BigDecimal pricePerNight;

        public RoomTypeRequest(String name, int capacity, int totalRooms, BigDecimal pricePerNight) {
            this.name = name;
            this.capacity = capacity;
            this.totalRooms = totalRooms;
            this.pricePerNight = pricePerNight;
        }

        public String getName() { return name; }
        public int getCapacity() { return capacity; }
        public int getTotalRooms() { return totalRooms; }
        public BigDecimal getPricePerNight() { return pricePerNight; }
    }

    private final String name;
    private final String city;
    private final String locality;
    private final int stars;
    private final Set<String> amenities;
    private final List<RoomTypeRequest> roomTypes;

    public PropertyRequest(String name, String city, String locality, int stars,
                           Set<String> amenities, List<RoomTypeRequest> roomTypes) {
        this.name = name;
        this.city = city;
        this.locality = locality;
        this.stars = stars;
        this.amenities = amenities;
        this.roomTypes = roomTypes;
    }

    public String getName() { return name; }
    public String getCity() { return city; }
    public String getLocality() { return locality; }
    public int getStars() { return stars; }
    public Set<String> getAmenities() { return amenities; }
    public List<RoomTypeRequest> getRoomTypes() { return roomTypes; }
}
