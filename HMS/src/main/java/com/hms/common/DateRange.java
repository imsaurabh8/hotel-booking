package com.hms.common;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

/** A stay: occupies the nights from checkIn up to (not including) checkOut. */
public class DateRange {

    private final LocalDate checkIn;
    private final LocalDate checkOut;

    public DateRange(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw ApiException.badRequest("Check-out must be after check-in");
        }
        this.checkIn = checkIn;
        this.checkOut = checkOut;
    }

    public long nights() {
        return ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public boolean covers(LocalDate night) {
        return !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    public Stream<LocalDate> eachNight() {
        return checkIn.datesUntil(checkOut);
    }

    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
}
