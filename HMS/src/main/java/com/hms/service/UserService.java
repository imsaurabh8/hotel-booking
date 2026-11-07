package com.hms.service;

import com.hms.common.ApiException;
import com.hms.model.Booking;
import com.hms.model.User;
import com.hms.repository.BookingRepository;
import com.hms.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository users;
    private final BookingRepository bookings;

    public UserService(UserRepository users, BookingRepository bookings) {
        this.users = users;
        this.bookings = bookings;
    }

    /** Email is the unique login-style identifier, so registering it twice is a conflict. */
    public synchronized User register(String name, String email) {
        if (name == null || name.isBlank() || email == null || !email.contains("@")) {
            throw ApiException.badRequest("User name and a valid email are required");
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (users.findByEmail(normalizedEmail).isPresent()) {
            throw ApiException.conflict("A user with email " + normalizedEmail + " already exists");
        }
        return users.save(new User(UUID.randomUUID().toString(), name.trim(), normalizedEmail));
    }

    public User get(String userId) {
        return users.findById(userId).orElseThrow(() -> ApiException.notFound("User", userId));
    }

    public List<Booking> bookingsOf(String userId) {
        get(userId);
        return bookings.findByUserId(userId);
    }
}
