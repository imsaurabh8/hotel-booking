package com.hms;

import com.hms.common.ApiException;
import com.hms.model.Booking;
import com.hms.model.Property;
import com.hms.model.User;
import org.junit.jupiter.api.Test;

import static com.hms.Fixture.stay;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserServiceTest {

    private final Fixture f = new Fixture();

    @Test
    void registersAUserAndNormalizesTheEmail() {
        User user = f.userService.register("  Meera ", "Meera@Example.COM");

        assertThat(user.getName()).isEqualTo("Meera");
        assertThat(user.getEmail()).isEqualTo("meera@example.com");
        assertThat(f.userService.get(user.getId())).isSameAs(user);
    }

    @Test
    void rejectsBadInputAndDuplicateEmail() {
        assertThatThrownBy(() -> f.userService.register(" ", "a@b.com")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> f.userService.register("A", "not-an-email")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> f.userService.register("Other", "RAVI@example.com"))
                .isInstanceOf(ApiException.class).hasMessageContaining("already exists");
        assertThatThrownBy(() -> f.userService.get("nope")).isInstanceOf(ApiException.class);
    }

    @Test
    void listsOnlyTheUsersOwnBookings() {
        Property hotel = f.standardHotel();
        User meera = f.userService.register("Meera", "meera@example.com");
        Booking mine = f.book(hotel, stay(10, 2));
        f.bookings.book(new com.hms.service.BookingRequest(hotel.getId(), hotel.getRoomTypes().get(0).getId(),
                stay(20, 2).getCheckIn(), stay(20, 2).getCheckOut(), 2, meera.getId()));

        assertThat(f.userService.bookingsOf(f.user.getId())).containsExactly(mine);
        assertThat(f.userService.bookingsOf(meera.getId())).hasSize(1);
        assertThatThrownBy(() -> f.userService.bookingsOf("nope")).isInstanceOf(ApiException.class);
    }
}
