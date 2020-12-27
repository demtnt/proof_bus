package com.example.dt.api;

import com.example.dt.model.DraftPriceCalculationRequest;
import com.example.dt.model.DraftTicket;
import com.example.dt.services.DraftTicketService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DraftTicketController implements DraftTicketApi{

    private final DraftTicketService draftTicketService;

    @Override
    public ResponseEntity<DraftTicket> calculate(@Valid DraftPriceCalculationRequest draftPriceCalculationRequest) {
        log.debug("Input for calculation {}", draftPriceCalculationRequest);

        final var result = draftTicketService.calculate(draftPriceCalculationRequest);

        log.debug("Output of calculation {}", result);
        return ResponseEntity.ok(result);
    }
}