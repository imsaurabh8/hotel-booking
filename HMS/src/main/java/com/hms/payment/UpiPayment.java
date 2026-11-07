package com.hms.payment;

import com.hms.common.ApiException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/** Details: vpa, like name@bank. */
@Component
public class UpiPayment implements PaymentMethod {

    private final PaymentGateway gateway;

    public UpiPayment(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    public String type() {
        return "UPI";
    }

    public PaymentResult pay(BigDecimal amount, Map<String, String> details) {
        String vpa = details.get("vpa");
        if (vpa == null || !vpa.matches("[\\w.\\-]+@\\w+")) {
            throw ApiException.badRequest("UPI needs a vpa like name@bank");
        }
        return gateway.charge(vpa, amount);
    }
}
