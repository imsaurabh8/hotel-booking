package com.hms.payment;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Stand-in for a real provider. It declines any instrument (card number, UPI id or wallet id)
 * that ends with "0002" or starts with "fail", and accepts everything else.
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    public PaymentResult charge(String instrument, BigDecimal amount) {
        if (instrument.endsWith("0002") || instrument.startsWith("fail")) {
            return PaymentResult.declined("Declined by the payment provider");
        }
        return PaymentResult.ok();
    }

    public void refund(String transactionId, BigDecimal amount) {
        // the mock provider always accepts refunds
    }
}
