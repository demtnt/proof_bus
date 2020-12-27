package com.example.dt.services;

import java.math.BigDecimal;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class BasePriceRepository {

    @Value("${basePrice}")
    private BigDecimal basePrice;

    public Optional<BigDecimal> getBasePriceByRoute(String route) {
        log.debug("Looking base price for {}", route);
        return Optional.of(basePrice);
    }
}