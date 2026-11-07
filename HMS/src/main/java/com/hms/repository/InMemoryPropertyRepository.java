package com.hms.repository;

import com.hms.model.Property;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryPropertyRepository implements PropertyRepository {

    private final Map<String, Property> properties = new ConcurrentHashMap<>();

    public Property save(Property property) {
        properties.put(property.getId(), property);
        return property;
    }

    public Optional<Property> findById(String id) {
        return Optional.ofNullable(properties.get(id));
    }

    public List<Property> findByOwnerId(String ownerId) {
        return properties.values().stream().filter(p -> p.getOwnerId().equals(ownerId)).toList();
    }

    public List<Property> findAll() {
        return List.copyOf(properties.values());
    }
}
