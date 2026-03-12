package org.innowise.internship.paymentservice.bankclient;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Random;

@Component
public class BankClient {
    private final Random random = new Random();

    public BankPaymentStatus processPayment(String userId, BigDecimal amount) {
        int chance = random.nextInt(100);
        return chance % 2 == 0 ? BankPaymentStatus.SUCCESS : BankPaymentStatus.FAILED;
    }
}
