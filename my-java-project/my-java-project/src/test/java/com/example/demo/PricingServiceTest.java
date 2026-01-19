package com.example.demo;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PricingService}.
 *
 * The original version relied on Spring Boot test support, which is not
 * available in the current project configuration. The service is completely
 * stateless, so we can instantiate it directly in the test class.
 */
class PricingServiceTest {

    private PricingService pricingService;

    @BeforeEach
    void setUp() {
        pricingService = new PricingService();
    }

    // ---------------------------------------------------------------------
    // Exception scenarios
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("Prezzo base negativo → eccezione")
    void negativeBasePriceThrows() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pricingService.calculateFinalPrice(-10, 25, false));
        assertEquals("Prezzo base non può essere negativo", ex.getMessage());
    }

    @Test
    @DisplayName("Età cliente inferiore a 0 → eccezione")
    void negativeAgeThrows() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pricingService.calculateFinalPrice(50, -5, false));
        assertEquals("Età del cliente non valida", ex.getMessage());
    }

    @Test
    @DisplayName("Età cliente superiore a 130 → eccezione")
    void ageAboveMaximumThrows() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pricingService.calculateFinalPrice(50, 150, false));
        assertEquals("Età del cliente non valida", ex.getMessage());
    }

    // ---------------------------------------------------------------------
    // Discount scenarios
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("Bambino sotto i 10 anni (gratis)")
    void childUnderTenFree() {
        double price = pricingService.calculateFinalPrice(100, 7, false);
        assertEquals(0.0, price);
    }

    @Test
    @DisplayName("Età esatta 10 anni (sconto 50 %)")
    void tenYearsOldHalfPrice() {
        double price = pricingService.calculateFinalPrice(80, 10, false);
        assertEquals(40.0, price);
    }

    @Test
    @DisplayName("Età 17 anni, studente (sconto più favorevole)")
    void seventeenStudentBestDiscount() {
        double price = pricingService.calculateFinalPrice(120, 17, true);
        // 50% teen discount is better than any student discount
        assertEquals(60.0, price);
    }

    @Test
    @DisplayName("Età 18 anni (pagamento pieno)")
    void eighteenNoDiscount() {
        double price = pricingService.calculateFinalPrice(75, 18, false);
        assertEquals(75.0, price);
    }

    @Test
    @DisplayName("Studente universitario sotto i 30 anni (sconto 20 %)")
    void studentUnderThirtyDiscount() {
        double price = pricingService.calculateFinalPrice(200, 25, true);
        assertEquals(160.0, price);
    }

    @Test
    @DisplayName("Studente universitario 30 anni o più (no sconto)")
    void studentThirtyOrMoreNoDiscount() {
        double price = pricingService.calculateFinalPrice(200, 30, true);
        assertEquals(200.0, price);
    }

    @Test
    @DisplayName("Pensionato sopra i 65 anni (sconto 30 %)")
    void pensionerOverSixtyFiveDiscount() {
        double price = pricingService.calculateFinalPrice(150, 70, false);
        assertEquals(105.0, price);
    }

    @Test
    @DisplayName("Età esatta 65 anni (pagamento pieno)")
    void exactlySixtyFiveNoDiscount() {
        double price = pricingService.calculateFinalPrice(150, 65, false);
        assertEquals(150.0, price);
    }

    @Test
    @DisplayName("Pensionato + studente (sconto più favorevole)")
    void pensionerStudentBestDiscount() {
        double price = pricingService.calculateFinalPrice(180, 68, true);
        // pensioner discount 30% is better than student 20%
        assertEquals(126.0, price);
    }

    @Test
    @DisplayName("Bambino + studente (sconto più favorevole)")
    void childStudentBestDiscount() {
        double price = pricingService.calculateFinalPrice(90, 8, true);
        // child discount 100% beats student 20%
        assertEquals(0.0, price);
    }

    @Test
    @DisplayName("Età minima 0 anni (gratis)")
    void zeroAgeFree() {
        double price = pricingService.calculateFinalPrice(50, 0, false);
        assertEquals(0.0, price);
    }

    @Test
    @DisplayName("Età massima consentita 130 anni (pagamento pieno)")
    void maxAgeNoDiscount() {
        double price = pricingService.calculateFinalPrice(60, 130, false);
        assertEquals(60.0, price);
    }

    @Test
    @DisplayName("Adulto normale (30‑64 anni, non studente)")
    void normalAdultNoDiscount() {
        double price = pricingService.calculateFinalPrice(110, 45, false);
        assertEquals(110.0, price);
    }

    @Test
    @DisplayName("Prezzo base zero (operazione valida)")
    void zeroBasePrice() {
        double price = pricingService.calculateFinalPrice(0, 40, false);
        assertEquals(0.0, price);
    }

    @Test
    @DisplayName("Calcolo sconto 50 % per teen a prezzo 100")
    void teenHalfPrice() {
        double price = pricingService.calculateFinalPrice(100, 15, false);
        assertEquals(50.0, price);
    }
}
