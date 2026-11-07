package com.hms.payment;

import java.math.BigDecimal;

/** The external payment provider. Only a mock implements it here; a real provider would replace it. */
public interface PaymentGateway {

    PaymentResult charge(String instrument, BigDecimal amount);

    void refund(String transactionId, BigDecimal amount);
}
