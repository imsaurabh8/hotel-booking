package com.hms.repository;

import com.hms.model.Owner;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryOwnerRepository implements OwnerRepository {

    private final Map<String, Owner> owners = new ConcurrentHashMap<>();

    public Owner save(Owner owner) {
        owners.put(owner.getId(), owner);
        return owner;
    }

    public Optional<Owner> findById(String id) {
        return Optional.ofNullable(owners.get(id));
    }
}
