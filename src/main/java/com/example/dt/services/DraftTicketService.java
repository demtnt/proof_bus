package com.example.dt.services;

import com.example.dt.model.DraftLine;
import com.example.dt.model.DraftPriceCalculationRequest;
import com.example.dt.model.DraftTicket;
import com.example.dt.model.Passenger;
import com.example.dt.model.PassengerType;
import com.example.dt.services.external.TaxServiceAccessor;
import java.math.BigDecimal;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DraftTicketService {

    private final TaxServiceAccessor taxServiceAccessor;
    private final BasePriceRepository basePriceRepository;

    @Value("${childDiscountPercent}")
    private BigDecimal childDiscountPercent;
    @Value("${luggageMultiplierPercent}")
    private BigDecimal luggageMultiplierPercent;

    public DraftTicket calculate(DraftPriceCalculationRequest draftPriceCalculationRequest) {

        final var currentVAT = taxServiceAccessor.getCurrentVATWithCircuitBreaker();
        final var basePrice = basePriceRepository.getBasePriceByRoute(draftPriceCalculationRequest.getRouteName())
                .orElseThrow(() -> new UnknownRouteException("draftPriceCalculationRequest.getRouteName()"));


        final var draftLineList = draftPriceCalculationRequest.getPassengerList()
                .stream()
                .map(passenger -> processPassenger(passenger, basePrice, currentVAT))
                .collect(Collectors.toList());

        final var total = draftLineList.stream()
                .map(draftLine -> draftLine.getPassengerPriceTotal().add(draftLine.getLuggagePriceTotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        final var draftTicket = new DraftTicket();
        draftTicket.setDraftLines(draftLineList);

        draftTicket.setTotal(total);

        return draftTicket;
    }

    DraftLine processPassenger(Passenger passenger, BigDecimal basePrice, BigDecimal currentVAT) {
        final var draftLine = new DraftLine();

        draftLine.setBasePrice(basePrice);
        draftLine.setvATPercents(currentVAT);
        draftLine.setChildDiscountPercents(childDiscountPercent);
        draftLine.setPassengerType(passenger.getType());
        draftLine.setPassengerPriceTotal(applyVAT(priceByAge(passenger, basePrice), currentVAT));
        draftLine.setLuggageCount(passenger.getLuggage());
        draftLine.setLuggagePriceTotal(applyVAT(luggagePrice(passenger.getLuggage(), basePrice), currentVAT));

        return draftLine;
    }

    BigDecimal priceByAge(Passenger passenger, BigDecimal basePrice) {
        return passenger.getType().equals(PassengerType.CHILD)
                ? basePrice.multiply(childDiscountPercent).movePointLeft(2)
                : basePrice;
    }

    BigDecimal luggagePrice(int count, BigDecimal basePrice) {
        final var luggageMultiplier = luggageMultiplierPercent.movePointLeft(2);
        return basePrice
                .multiply(BigDecimal.valueOf(count))
                .multiply(luggageMultiplier);
    }

    BigDecimal applyVAT(BigDecimal input, BigDecimal currentVATPercent) {
        final var vATDecimal = currentVATPercent.add(BigDecimal.valueOf(100)).movePointLeft(2);
        return input.multiply(vATDecimal);
    }
}