package com.hms.search;

import com.hms.model.Property;
import com.hms.model.RoomType;

import java.util.Map;

/**
 * An optional filter. It reads its own request parameters and accepts everything when they are absent.
 * Add a filter by adding a Spring bean that implements this - the search code does not change.
 */
public interface SearchFilter {

    boolean matches(Property property, RoomType room, Map<String, String> params);
}
