package com.hms.controller;

import com.hms.model.Booking;
import com.hms.model.User;
import com.hms.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    public static class UserRequest {

        private final String name;
        private final String email;

        public UserRequest(String name, String email) {
            this.name = name;
            this.email = email;
        }

        public String getName() { return name; }
        public String getEmail() { return email; }
    }

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User register(@RequestBody UserRequest request) {
        return userService.register(request.getName(), request.getEmail());
    }

    @GetMapping("/{userId}")
    public User get(@PathVariable String userId) {
        return userService.get(userId);
    }

    @GetMapping("/{userId}/bookings")
    public List<Booking> bookings(@PathVariable String userId) {
        return userService.bookingsOf(userId);
    }
}
