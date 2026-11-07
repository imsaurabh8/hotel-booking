package com.hms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Whole stack over HTTP: onboard, search, book, pay, cancel, search again. */
@SpringBootTest
@AutoConfigureMockMvc
class HmsApiTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private final LocalDate checkIn = LocalDate.now().plusDays(30);
    private final LocalDate checkOut = checkIn.plusDays(2);

    private ResultActions postJson(String url, String body) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode read(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString());
    }

    private JsonNode onboard(String city) throws Exception {
        return read(postJson("/api/owners", """
                {"name":"Asha","properties":[{"name":"Sea View","city":"%s","locality":"Bandra","stars":4,
                 "amenities":["wifi","pool"],
                 "roomTypes":[{"name":"Deluxe","capacity":2,"totalRooms":1,"pricePerNight":5000}]}]}
                """.formatted(city)).andExpect(status().isCreated())).get("properties").get(0);
    }

    private String bookingJson(JsonNode property) {
        return """
                {"propertyId":"%s","roomTypeId":"%s","checkIn":"%s","checkOut":"%s","guests":2,
                 "guestName":"Ravi","guestEmail":"ravi@example.com"}
                """.formatted(property.get("id").asText(), property.get("roomTypes").get(0).get("id").asText(), checkIn, checkOut);
    }

    private ResultActions search(String city) throws Exception {
        return mvc.perform(get("/api/properties/search").param("city", city).param("checkIn", checkIn.toString())
                .param("checkOut", checkOut.toString()).param("guests", "2").param("amenities", "pool").param("minPrice", "1000"));
    }

    @Test
    void fullLifecycle() throws Exception {
        String city = "Lifecycle-" + System.nanoTime();
        JsonNode property = onboard(city);

        search(city).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].property.name").value("Sea View"))
                .andExpect(jsonPath("$[0].rooms[0].totalPrice").value(10000.0));

        String id = read(postJson("/api/bookings", bookingJson(property)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))).get("id").asText();
        postJson("/api/bookings", bookingJson(property)).andExpect(status().isConflict());
        search(city).andExpect(jsonPath("$").isEmpty());

        postJson("/api/bookings/" + id + "/payment", "{\"method\":\"UPI\",\"details\":{\"vpa\":\"ravi@okbank\"}}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"));
        postJson("/api/bookings/" + id + "/payment", "{\"method\":\"UPI\",\"details\":{\"vpa\":\"ravi@okbank\"}}")
                .andExpect(status().isConflict());

        postJson("/api/bookings/" + id + "/cancel", "").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED")).andExpect(jsonPath("$.refundAmount").value(10000.0));
        postJson("/api/bookings/" + id + "/cancel", "").andExpect(status().isConflict());

        search(city).andExpect(jsonPath("$[0].property.name").value("Sea View"));
    }

    @Test
    void declinedPaymentReturns402AndFreesTheRoom() throws Exception {
        String city = "Declined-" + System.nanoTime();
        JsonNode property = onboard(city);
        String id = read(postJson("/api/bookings", bookingJson(property)).andExpect(status().isCreated())).get("id").asText();

        postJson("/api/bookings/" + id + "/payment", "{\"method\":\"CARD\",\"details\":{\"cardNumber\":\"4000000000000002\",\"cvv\":\"123\"}}")
                .andExpect(status().isPaymentRequired()).andExpect(jsonPath("$.status").value("PAYMENT_FAILED"));

        search(city).andExpect(jsonPath("$[0].property.name").value("Sea View"));
    }

    @Test
    void errorsMapToSensibleStatuses() throws Exception {
        mvc.perform(get("/api/bookings/missing")).andExpect(status().isNotFound());
        postJson("/api/owners", "{\"name\":\"\"}").andExpect(status().isBadRequest());
        mvc.perform(get("/api/properties/search").param("city", "x")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/properties/search").param("checkIn", checkIn.toString()).param("checkOut", checkOut.toString())
                .param("minPrice", "abc")).andExpect(status().isBadRequest());
    }
}
