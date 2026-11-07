package com.hms.repository;

import com.hms.model.Property;

import java.util.List;
import java.util.Optional;

public interface PropertyRepository {

    Property save(Property property);

    Optional<Property> findById(String id);

    List<Property> findByOwnerId(String ownerId);

    List<Property> findAll();
}
