package com.hms.service;

import com.hms.common.ApiException;
import com.hms.model.Owner;
import com.hms.model.Property;
import com.hms.model.RoomType;
import com.hms.repository.OwnerRepository;
import com.hms.repository.PropertyRepository;
import com.hms.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PropertyService {

    private final OwnerRepository owners;
    private final PropertyRepository properties;
    private final UserRepository users;

    public PropertyService(OwnerRepository owners, PropertyRepository properties, UserRepository users) {
        this.owners = owners;
        this.properties = properties;
        this.users = users;
    }

    /** Makes a user a host. One property for a standalone host, several for a chain; more can be added later. */
    public synchronized Owner registerOwner(String userId, String name, List<PropertyRequest> initialProperties) {
        users.findById(userId).orElseThrow(() -> ApiException.notFound("User", userId));
        if (name == null || name.isBlank()) {
            throw ApiException.badRequest("Owner name is required");
        }
        if (owners.findByUserId(userId).isPresent()) {
            throw ApiException.conflict("User " + userId + " is already an owner");
        }
        Owner owner = new Owner(UUID.randomUUID().toString(), userId, name.trim());
        List<Property> built = initialProperties.stream().map(r -> build(owner.getId(), r)).toList();
        owners.save(owner);
        built.forEach(properties::save);
        return owner;
    }

    public Property addProperty(String ownerId, PropertyRequest request) {
        getOwner(ownerId);
        return properties.save(build(ownerId, request));
    }

    public Owner getOwner(String ownerId) {
        return owners.findById(ownerId).orElseThrow(() -> ApiException.notFound("Owner", ownerId));
    }

    public List<Property> propertiesOf(String ownerId) {
        getOwner(ownerId);
        return properties.findByOwnerId(ownerId);
    }

    public Property getProperty(String propertyId) {
        return properties.findById(propertyId).orElseThrow(() -> ApiException.notFound("Property", propertyId));
    }

    private Property build(String ownerId, PropertyRequest r) {
        if (isBlank(r.getName()) || isBlank(r.getCity()) || isBlank(r.getLocality())) {
            throw ApiException.badRequest("Property name, city and locality are required");
        }
        if (r.getStars() < 1 || r.getStars() > 5) {
            throw ApiException.badRequest("Stars must be between 1 and 5");
        }
        if (r.getRoomTypes() == null || r.getRoomTypes().isEmpty()) {
            throw ApiException.badRequest("A property needs at least one room type");
        }
        List<RoomType> rooms = r.getRoomTypes().stream().map(PropertyService::buildRoom).toList();
        Set<String> amenities = r.getAmenities() == null ? Set.of()
                : r.getAmenities().stream().map(a -> a.trim().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        return new Property(UUID.randomUUID().toString(), ownerId, r.getName().trim(), r.getCity().trim(),
                r.getLocality().trim(), r.getStars(), amenities, rooms);
    }

    private static RoomType buildRoom(PropertyRequest.RoomTypeRequest r) {
        if (isBlank(r.getName()) || r.getCapacity() < 1 || r.getTotalRooms() < 1
                || r.getPricePerNight() == null || r.getPricePerNight().signum() <= 0) {
            throw ApiException.badRequest("Room type needs a name, capacity, room count and a positive price");
        }
        return new RoomType(UUID.randomUUID().toString(), r.getName().trim(), r.getCapacity(), r.getTotalRooms(), r.getPricePerNight());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
