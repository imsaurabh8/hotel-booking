package com.hms.cancellation;

import com.hms.model.Booking;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Decides how much of a paid booking is refunded when it is cancelled on the given day. */
public interface CancellationPolicy {

    BigDecimal refundAmount(Booking booking, LocalDate today);
}
