package com.example.demo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for {@link PricingService} covering validation, discount calculations and
 * discount priority rules.
 */
class PricingServiceTest {

    private final PricingService service = new PricingService();

    // ---------------------------------------------------------------------
    // Validation scenarios
    // ---------------------------------------------------------------------
    @Test
    void scenario1_negativeBasePrice_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.calculateFinalPrice(-10, 25, false));
        assertEquals("Prezzo base non può essere negativo", ex.getMessage());
    }

    @Test
    void scenario2_negativeAge_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.calculateFinalPrice(100, -5, false));
        assertEquals("Età del cliente non valida", ex.getMessage());
    }

    @Test
    void scenario3_ageAboveMaximum_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.calculateFinalPrice(100, 131, false));
        assertEquals("Età del cliente non valida", ex.getMessage());
    }

    // ---------------------------------------------------------------------
    // Discount calculation scenarios
    // ---------------------------------------------------------------------
    @Test
    void scenario4_childFreeDiscount() {
        double result = service.calculateFinalPrice(150, 8, false);
        assertEquals(0.0, result, 0.0001);
    }

    @Test
    void scenario5_teenFiftyPercentDiscount() {
        double result = service.calculateFinalPrice(200, 16, false);
        assertEquals(100.0, result, 0.0001);
    }

    @Test
    void scenario6_studentUnder30TwentyPercentDiscount() {
        double result = service.calculateFinalPrice(120, 22, true);
        assertEquals(96.0, result, 0.0001);
    }

    @Test
    void scenario7_studentAtOrAbove30NoStudentDiscount() {
        double result = service.calculateFinalPrice(120, 30, true);
        assertEquals(120.0, result, 0.0001);
    }

    @Test
    void scenario8_pensionerThirtyPercentDiscount() {
        double result = service.calculateFinalPrice(80, 70, false);
        assertEquals(56.0, result, 0.0001);
    }

    @Test
    void scenario9_adultNoDiscount() {
        double result = service.calculateFinalPrice(90, 45, false);
        assertEquals(90.0, result, 0.0001);
    }

    // ---------------------------------------------------------------------
    // Priority (best discount) scenarios
    // ---------------------------------------------------------------------
    @Test
    void scenario10_studentUnder18GetsTeenDiscount() {
        // For a student younger than 18 the best applicable discount is the
        // teen discount (50 %). The child‑free discount applies only to ages < 10.
        double result = service.calculateFinalPrice(100, 17, true);
        assertEquals(50.0, result, 0.0001);
    }

    @Test
    void scenario11_studentBetween18And30GetsStudentDiscount() {
        double result = service.calculateFinalPrice(250, 25, true);
        assertEquals(200.0, result, 0.0001);
    }

    @Test
    void scenario12_pensionerStudentGetsPensionerDiscount() {
        double result = service.calculateFinalPrice(180, 68, true);
        assertEquals(126.0, result, 0.0001);
    }

    @Test
    void scenario13_edgeAgeZeroChildDiscount() {
        double result = service.calculateFinalPrice(50, 0, false);
        assertEquals(0.0, result, 0.0001);
    }

    @Test
    void scenario14_edgeAgeMaximumPensionerDiscount() {
        double result = service.calculateFinalPrice(70, 130, false);
        assertEquals(49.0, result, 0.0001);
    }
}