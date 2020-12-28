package com.example.dt.services.external;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaxServiceAccessor {

    @Value("${VATpercents}")
    private BigDecimal vATPercentage;

    private final CircuitBreakerFactory cbFactory;

    private BigDecimal getCurrentVAT() {
        return vATPercentage;
    }

    BigDecimal fallback(Throwable throwable) {
        throw new TaxServiceUnavailableException();
    }

    public BigDecimal getCurrentVATWithCircuitBreaker() {
        return cbFactory.create("cb_id").run(this::getCurrentVAT, this::fallback);
    }
}