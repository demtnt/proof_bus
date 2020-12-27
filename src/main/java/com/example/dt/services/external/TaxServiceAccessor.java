package com.example.dt.services.external;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TaxServiceAccessor {

    @Value("${VATpercents}")
    private BigDecimal vATPercentage;

    public BigDecimal getCurrentVAT() {
        return vATPercentage;
    }

    public BigDecimal getCurrentVATWithCircuitBreaker() {
        return vATPercentage;
    }
}