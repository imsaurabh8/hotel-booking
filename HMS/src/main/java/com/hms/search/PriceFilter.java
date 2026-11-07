package com.hms.search;

import com.hms.model.Property;
import com.hms.model.RoomType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/** Params: minPrice, maxPrice (per night). */
@Component
public class PriceFilter implements SearchFilter {

    public boolean matches(Property property, RoomType room, Map<String, String> params) {
        BigDecimal price = room.getPricePerNight();
        return (!params.containsKey("minPrice") || price.compareTo(new BigDecimal(params.get("minPrice"))) >= 0)
                && (!params.containsKey("maxPrice") || price.compareTo(new BigDecimal(params.get("maxPrice"))) <= 0);
    }
}
