package com.example.dt.services;

import com.example.dt.model.DraftLine;
import com.example.dt.model.DraftPriceCalculationRequest;
import com.example.dt.model.Passenger;
import com.example.dt.model.PassengerType;
import com.example.dt.services.external.TaxServiceAccessor;
import com.example.dt.services.external.TaxServiceUnavailableException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Slf4j
@TestPropertySource(properties = {"luggageMultiplierPercent=60", "childDiscountPercent=55"})
@SpringBootTest
class DraftTicketServiceTest {

    @MockBean
    private TaxServiceAccessor taxServiceAccessor;
    @MockBean
    private BasePriceRepository basePriceRepository;

    @Captor
    private ArgumentCaptor<BigDecimal> basePriceArgument;
    @Captor
    private ArgumentCaptor<BigDecimal> currentVATArgument;


    @SpyBean
    private DraftTicketService subject;

    @BeforeAll
    static void initAll() {
        log.info("---Inside initAll---");
    }

    @BeforeEach
    void init(TestInfo testInfo) {
        log.info("Start..." + testInfo.getDisplayName());
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        log.info("Finished..." + testInfo.getDisplayName());
    }

    @AfterAll
    static void tearDownAll() {
        log.info("---Inside tearDownAll---");
    }

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void calculate_NO_TAX_SERVICE_AVAILABLE() {
        when(taxServiceAccessor.getCurrentVATWithCircuitBreaker()).thenThrow(new TaxServiceUnavailableException());

        final var draftPriceCalculationRequest = mock(DraftPriceCalculationRequest.class);
        draftPriceCalculationRequest.setRouteName(System.currentTimeMillis() + "");
        assertThrows(TaxServiceUnavailableException.class, () -> subject.calculate(draftPriceCalculationRequest));
    }

    @Test
    void calculate_NO_BASE_PRICE_FOUND() {
        when(basePriceRepository.getBasePriceByRoute(anyString())).thenReturn(Optional.empty());

        final var draftPriceCalculationRequest = mock(DraftPriceCalculationRequest.class);
        draftPriceCalculationRequest.setRouteName(System.currentTimeMillis() + "");
        assertThrows(UnknownRouteException.class, () -> subject.calculate(draftPriceCalculationRequest));

        verify(draftPriceCalculationRequest).getRouteName();
        verify(draftPriceCalculationRequest, times(0)).getPassengerList();
    }

    @Test
    void calculate() {
        final var expectedVAT = BigDecimal.valueOf(123);
        when(taxServiceAccessor.getCurrentVATWithCircuitBreaker()).thenReturn(expectedVAT);
        final var expectedBasePrice = Optional.of(BigDecimal.valueOf(321));
        when(basePriceRepository.getBasePriceByRoute(anyString())).thenReturn(expectedBasePrice);

        final var expectedDraftLine = new DraftLine()
                .passengerPriceTotal(BigDecimal.ONE)
                .luggagePriceTotal(BigDecimal.ONE);
        doReturn(expectedDraftLine).when(subject).processPassenger(any(), any(), any());


        final List<Passenger> passengerList = Arrays.asList(new Passenger(), new Passenger(), new Passenger());
        final var draftPriceCalculationRequest = new DraftPriceCalculationRequest().passengerList(passengerList).routeName("aaa");
        final var result = subject.calculate(draftPriceCalculationRequest);

        verify(subject, times(3)).processPassenger(any(), basePriceArgument.capture(), currentVATArgument.capture());
        basePriceArgument.getAllValues()
                .forEach(basePriceArgumentElem ->
                        assertThat(basePriceArgumentElem, Matchers.comparesEqualTo(expectedBasePrice.get())));
        currentVATArgument.getAllValues()
                .forEach(currentVATArgumentElem ->
                        assertThat(currentVATArgumentElem, Matchers.comparesEqualTo(expectedVAT)));

        assertEquals(3, result.getDraftLines().size());
        assertThat(result.getTotal(), Matchers.comparesEqualTo(BigDecimal.valueOf(6)));
    }

    @Test
    void processPassenger() {
        final var priceByAgeMarker = BigDecimal.valueOf(123);
        final var luggagePriceMarker = BigDecimal.valueOf(321);

        doReturn(priceByAgeMarker).when(subject).priceByAge(any(), any());
        doReturn(luggagePriceMarker).when(subject).luggagePrice(anyInt(), any());
        doReturn(BigDecimal.valueOf(111)).when(subject).applyVAT(same(priceByAgeMarker), any());
        doReturn(BigDecimal.valueOf(222)).when(subject).applyVAT(same(luggagePriceMarker), any());

        final var inputPassenger = new Passenger().type(PassengerType.CHILD).luggage(55);
        final var inputBasePrice = BigDecimal.TEN;
        final var inputCurrentVAT = BigDecimal.TEN;
        final var draftLine = subject.processPassenger(inputPassenger, inputBasePrice, inputCurrentVAT);

        assertThat(draftLine.getBasePrice(), Matchers.comparesEqualTo(inputBasePrice));
        assertThat(draftLine.getvATPercents(), Matchers.comparesEqualTo(inputCurrentVAT));
        assertThat(draftLine.getChildDiscountPercents(), Matchers.comparesEqualTo(BigDecimal.valueOf(55)));
        assertEquals(PassengerType.CHILD, draftLine.getPassengerType());
        assertThat(draftLine.getPassengerPriceTotal(), Matchers.comparesEqualTo(BigDecimal.valueOf(111)));
        assertEquals(55, draftLine.getLuggageCount());
        assertThat(draftLine.getLuggagePriceTotal(), Matchers.comparesEqualTo(BigDecimal.valueOf(222)));

        verify(subject, times(2)).applyVAT(any(), any());
        verify(subject).priceByAge(any(), any());
        verify(subject).luggagePrice(anyInt(), any());
        verifyNoInteractions(taxServiceAccessor, basePriceRepository);
    }

    private static Stream<Arguments> priceByAgeSource() {
        return Stream.of(
                Arguments.of(PassengerType.ADULT, BigDecimal.TEN),
                Arguments.of(PassengerType.CHILD, BigDecimal.valueOf(5.5))
        );
    }

    @ParameterizedTest
    @MethodSource("priceByAgeSource")
    void priceByAge(PassengerType passengerType, BigDecimal expectedPrice) {
        final var currentPassenger = new Passenger().type(passengerType);
        final var priceByAge = subject.priceByAge(currentPassenger, BigDecimal.TEN);
        assertThat(priceByAge, Matchers.comparesEqualTo(expectedPrice));
    }

    @Test
    void luggagePrice() {
        final var luggagePrice = subject.luggagePrice(5, BigDecimal.TEN);
        assertThat(luggagePrice, Matchers.comparesEqualTo(BigDecimal.valueOf(30)));
    }

    @Test
    void applyVAT() {
        final var valueWithVAT = subject.applyVAT(BigDecimal.TEN, BigDecimal.valueOf(21));
        assertThat(valueWithVAT, Matchers.comparesEqualTo(BigDecimal.valueOf(12.1)));
    }
}