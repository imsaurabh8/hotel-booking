package com.hms.search;

import com.hms.model.Property;
import com.hms.model.RoomType;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Param: minStars. */
@Component
public class StarRatingFilter implements SearchFilter {

    public boolean matches(Property property, RoomType room, Map<String, String> params) {
        return !params.containsKey("minStars") || property.getStars() >= Integer.parseInt(params.get("minStars"));
    }
}
