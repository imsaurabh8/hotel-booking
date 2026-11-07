package com.hms.payment;

import com.hms.common.ApiException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/** Details: walletId. */
@Component
public class WalletPayment implements PaymentMethod {

    private final PaymentGateway gateway;

    public WalletPayment(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    public String type() {
        return "WALLET";
    }

    public PaymentResult pay(BigDecimal amount, Map<String, String> details) {
        String walletId = details.get("walletId");
        if (walletId == null || walletId.isBlank()) {
            throw ApiException.badRequest("Wallet needs a walletId");
        }
        return gateway.charge(walletId, amount);
    }
}
