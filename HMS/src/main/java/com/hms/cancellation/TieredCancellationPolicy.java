package com.hms.cancellation;

import com.hms.model.Booking;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Full refund 7+ days before check-in, half from 2 to 6 days, nothing closer. */
@Component
public class TieredCancellationPolicy implements CancellationPolicy {

    public BigDecimal refundAmount(Booking booking, LocalDate today) {
        long daysToCheckIn = ChronoUnit.DAYS.between(today, booking.getStay().getCheckIn());
        BigDecimal total = booking.getTotalAmount();
        if (daysToCheckIn >= 7) {
            return total;
        }
        if (daysToCheckIn >= 2) {
            return total.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }
}
