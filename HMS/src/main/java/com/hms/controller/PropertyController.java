package com.hms.controller;

import com.hms.common.DateRange;
import com.hms.model.Owner;
import com.hms.model.Property;
import com.hms.search.SearchResult;
import com.hms.search.SearchService;
import com.hms.service.PropertyRequest;
import com.hms.service.PropertyService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PropertyController {

    public static class OwnerRequest {

        private final String userId;
        private final String name;
        private final List<PropertyRequest> properties;

        public OwnerRequest(String userId, String name, List<PropertyRequest> properties) {
            this.userId = userId;
            this.name = name;
            this.properties = properties;
        }

        public String getUserId() { return userId; }
        public String getName() { return name; }
        public List<PropertyRequest> getProperties() { return properties; }
    }

    public static class OwnerResponse {

        private final Owner owner;
        private final List<Property> properties;

        public OwnerResponse(Owner owner, List<Property> properties) {
            this.owner = owner;
            this.properties = properties;
        }

        public Owner getOwner() { return owner; }
        public List<Property> getProperties() { return properties; }
    }

    private final PropertyService propertyService;
    private final SearchService searchService;

    public PropertyController(PropertyService propertyService, SearchService searchService) {
        this.propertyService = propertyService;
        this.searchService = searchService;
    }

    @PostMapping("/owners")
    @ResponseStatus(HttpStatus.CREATED)
    public OwnerResponse registerOwner(@RequestBody OwnerRequest request) {
        List<PropertyRequest> initial = request.getProperties() == null ? List.of() : request.getProperties();
        Owner owner = propertyService.registerOwner(request.getUserId(), request.getName(), initial);
        return new OwnerResponse(owner, propertyService.propertiesOf(owner.getId()));
    }

    @GetMapping("/owners/{ownerId}")
    public OwnerResponse getOwner(@PathVariable String ownerId) {
        return new OwnerResponse(propertyService.getOwner(ownerId), propertyService.propertiesOf(ownerId));
    }

    @PostMapping("/owners/{ownerId}/properties")
    @ResponseStatus(HttpStatus.CREATED)
    public Property addProperty(@PathVariable String ownerId, @RequestBody PropertyRequest request) {
        return propertyService.addProperty(ownerId, request);
    }

    @GetMapping("/properties/{propertyId}")
    public Property getProperty(@PathVariable String propertyId) {
        return propertyService.getProperty(propertyId);
    }

    /** Every request parameter is handed to the filters, so new filters need no change here. */
    @GetMapping("/properties/search")
    public List<SearchResult> search(@RequestParam(required = false) String city,
                                     @RequestParam(required = false) String locality,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
                                     @RequestParam(defaultValue = "1") int guests,
                                     @RequestParam Map<String, String> params) {
        return searchService.search(city, locality, new DateRange(checkIn, checkOut), guests, params);
    }
}
