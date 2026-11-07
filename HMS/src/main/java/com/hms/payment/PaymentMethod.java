package com.hms.payment;

import java.math.BigDecimal;
import java.util.Map;

/**
 * One way of paying: it checks the details that method needs, then charges through the PaymentGateway.
 * Add a method by adding a Spring bean that implements this.
 */
public interface PaymentMethod {

    /** Key clients send to pick this method, e.g. "CARD". */
    String type();

    /** Validates the method's details (ApiException.badRequest if malformed) and charges. */
    PaymentResult pay(BigDecimal amount, Map<String, String> details);
}
