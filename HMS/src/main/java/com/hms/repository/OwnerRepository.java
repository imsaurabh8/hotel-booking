package com.hms.repository;

import com.hms.model.Owner;

import java.util.Optional;

public interface OwnerRepository {

    Owner save(Owner owner);

    Optional<Owner> findById(String id);
}
