package com.hms.model;

import java.math.BigDecimal;

public class RoomType {

    private final String id;
    private final String name;
    private final int capacity;
    private final int totalRooms;
    private final BigDecimal pricePerNight;

    public RoomType(String id, String name, int capacity, int totalRooms, BigDecimal pricePerNight) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.totalRooms = totalRooms;
        this.pricePerNight = pricePerNight;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getCapacity() { return capacity; }
    public int getTotalRooms() { return totalRooms; }
    public BigDecimal getPricePerNight() { return pricePerNight; }
}
