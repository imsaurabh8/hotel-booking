package com.hms.payment;

import com.hms.common.ApiException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/** Details: cardNumber (16 digits) and cvv (3 digits). */
@Component
public class CardPayment implements PaymentMethod {

    private final PaymentGateway gateway;

    public CardPayment(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    public String type() {
        return "CARD";
    }

    public PaymentResult pay(BigDecimal amount, Map<String, String> details) {
        String card = details.get("cardNumber");
        String cvv = details.get("cvv");
        if (card == null || !card.matches("\\d{16}") || cvv == null || !cvv.matches("\\d{3}")) {
            throw ApiException.badRequest("Card needs a 16 digit cardNumber and a 3 digit cvv");
        }
        return gateway.charge(card, amount);
    }
}
