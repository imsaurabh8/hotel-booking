package com.hms.model;

import com.hms.common.ApiException;

import java.util.List;
import java.util.Set;

public class Property {

    private final String id;
    private final String ownerId;
    private final String name;
    private final String city;
    private final String locality;
    private final int stars;
    private final Set<String> amenities;
    private final List<RoomType> roomTypes;

    public Property(String id, String ownerId, String name, String city, String locality,
                    int stars, Set<String> amenities, List<RoomType> roomTypes) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.city = city;
        this.locality = locality;
        this.stars = stars;
        this.amenities = amenities;
        this.roomTypes = roomTypes;
    }

    public RoomType roomType(String roomTypeId) {
        return roomTypes.stream().filter(r -> r.getId().equals(roomTypeId)).findFirst()
                .orElseThrow(() -> ApiException.notFound("Room type", roomTypeId));
    }

    public String getId() { return id; }
    public String getOwnerId() { return ownerId; }
    public String getName() { return name; }
    public String getCity() { return city; }
    public String getLocality() { return locality; }
    public int getStars() { return stars; }
    public Set<String> getAmenities() { return amenities; }
    public List<RoomType> getRoomTypes() { return roomTypes; }
}
