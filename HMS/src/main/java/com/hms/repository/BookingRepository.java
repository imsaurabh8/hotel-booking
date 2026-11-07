package com.hms.repository;

import com.hms.common.ApiException;
import com.hms.model.Booking;

import java.util.List;
import java.util.Optional;

public interface BookingRepository {

    Booking save(Booking booking);

    Optional<Booking> findById(String id);

    /** Bookings of this room type that currently occupy a room. */
    List<Booking> findActiveByRoomType(String roomTypeId);

    List<Booking> findByUserId(String userId);

    default Booking getOrThrow(String id) {
        return findById(id).orElseThrow(() -> ApiException.notFound("Booking", id));
    }
}
