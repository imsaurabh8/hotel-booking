package com.hms.repository;

import com.hms.model.User;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(String id);

    Optional<User> findByEmail(String email);
}
