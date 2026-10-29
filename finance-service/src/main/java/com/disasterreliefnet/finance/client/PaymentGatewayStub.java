package com.disasterreliefnet.finance.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Payment gateway integration component / client stub simulating interactions with
 * third-party card processors, mobile money, and ACH settlement networks.
 */
@Component
public class PaymentGatewayStub {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayStub.class);

    public record GatewayChargeResult(boolean success, String transactionReference, String errorMessage) {}

    public GatewayChargeResult processCharge(BigDecimal amount, String currency, String gatewayToken) {
        log.info("Processing gateway charge of {} {} with token: {}", amount, currency, gatewayToken);

        if (gatewayToken != null && gatewayToken.toLowerCase().startsWith("fail")) {
            log.warn("Payment charge simulated failure for token: {}", gatewayToken);
            return new GatewayChargeResult(false, null, "Card declined or insufficient funds");
        }

        String reference = "ch_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.info("Payment charge successful with reference: {}", reference);
        return new GatewayChargeResult(true, reference, null);
    }
}
