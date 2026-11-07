package com.hms;

import com.hms.common.ApiException;
import com.hms.model.Owner;
import com.hms.model.Property;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.hms.Fixture.hotel;
import static com.hms.Fixture.room;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertyServiceTest {

    private final Fixture f = new Fixture();

    @Test
    void standaloneHostIsAnOwnerWithOneProperty() {
        Owner owner = f.propertyService.registerOwner("Asha",
                List.of(hotel("Villa", "Goa", "Calangute", 3, Set.of(), room("Std", 2, 3, "2000"))));

        assertThat(f.propertyService.propertiesOf(owner.getId())).hasSize(1);
    }

    @Test
    void chainOwnsManyPropertiesUnderOneAccountAndCanAddMore() {
        Owner owner = f.propertyService.registerOwner("Taj", List.of(
                hotel("Taj Mumbai", "Mumbai", "Colaba", 5, Set.of(), room("Std", 2, 3, "9000")),
                hotel("Taj Delhi", "Delhi", "Centre", 5, Set.of(), room("Std", 2, 3, "8000"))));
        f.propertyService.addProperty(owner.getId(), hotel("Taj Goa", "Goa", "Candolim", 5, Set.of(), room("Std", 2, 3, "7000")));

        assertThat(f.propertyService.propertiesOf(owner.getId())).extracting(Property::getName)
                .containsExactlyInAnyOrder("Taj Mumbai", "Taj Delhi", "Taj Goa");
    }

    @Test
    void amenitiesAreNormalised() {
        Property p = f.propertyService.addProperty(f.newOwner().getId(),
                hotel("Villa", "Goa", "x", 3, Set.of(" WiFi ", "POOL"), room("Std", 2, 1, "100")));

        assertThat(p.getAmenities()).containsExactlyInAnyOrder("wifi", "pool");
    }

    @Test
    void rejectsInvalidInput() {
        String owner = f.newOwner().getId();
        assertThatThrownBy(() -> f.propertyService.addProperty("ghost", hotel("V", "Goa", "x", 3, Set.of(), room("S", 2, 1, "100"))))
                .isInstanceOf(ApiException.class).hasMessageContaining("not found");
        assertThatThrownBy(() -> f.propertyService.addProperty(owner, hotel("V", "Goa", "x", 6, Set.of(), room("S", 2, 1, "100"))))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> f.propertyService.addProperty(owner, hotel("V", "Goa", "x", 3, Set.of())))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> f.propertyService.addProperty(owner, hotel("V", "Goa", "x", 3, Set.of(), room("S", 0, 1, "100"))))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> f.propertyService.registerOwner(" ", List.of())).isInstanceOf(ApiException.class);
    }

    @Test
    void invalidPropertyInAChainSavesNothing() {
        assertThatThrownBy(() -> f.propertyService.registerOwner("Chain", List.of(
                hotel("Good", "Goa", "x", 3, Set.of(), room("S", 2, 1, "100")),
                hotel("Bad", "Goa", "x", 9, Set.of(), room("S", 2, 1, "100")))))
                .isInstanceOf(ApiException.class);

        assertThat(f.propertyRepository.findAll()).isEmpty();
    }
}
