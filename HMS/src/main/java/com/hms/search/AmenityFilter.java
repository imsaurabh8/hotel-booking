package com.hms.search;

import com.hms.model.Property;
import com.hms.model.RoomType;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

/** Param: amenities, comma separated; the property must have all of them. */
@Component
public class AmenityFilter implements SearchFilter {

    public boolean matches(Property property, RoomType room, Map<String, String> params) {
        String wanted = params.get("amenities");
        return wanted == null || Arrays.stream(wanted.split(","))
                .allMatch(a -> property.getAmenities().contains(a.trim().toLowerCase()));
    }
}
