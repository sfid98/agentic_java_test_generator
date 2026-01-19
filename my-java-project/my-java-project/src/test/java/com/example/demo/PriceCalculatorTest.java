package com.example.demo;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PriceCalculator}.
 *
 * The original test plan expected two independent methods:
 * {@code calculate(double, double)} and {@code discount(double, double)}.
 * Both methods are now implemented in {@link PriceCalculator} and are tested
 * here without any external framework (e.g., Mockito) to keep the test suite
 * lightweight.
 */
class PriceCalculatorTest {

    private PriceCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new PriceCalculator();
    }

    @Nested
    @DisplayName("calculate method")
    class CalculateTests {
        @Test
        @DisplayName("should add tax correctly for positive values")
        void calculateAddsTaxCorrectly() {
            double price = 100.0;
            double tax = 0.2; // 20 %
            double expected = 120.0;
            assertEquals(expected, calculator.calculate(price, tax), 1e-9);
        }

        @Test
        @DisplayName("zero tax returns original price")
        void calculateZeroTax() {
            double price = 55.5;
            double tax = 0.0;
            assertEquals(price, calculator.calculate(price, tax), 1e-9);
        }

        @Test
        @DisplayName("negative price throws IllegalArgumentException")
        void calculateNegativePrice() {
            assertThrows(IllegalArgumentException.class, () -> calculator.calculate(-10.0, 0.1));
        }
    }

    @Nested
    @DisplayName("discount method")
    class DiscountTests {
        @Test
        @DisplayName("applies discount correctly")
        void discountAppliesRate() {
            double price = 200.0;
            double discountRate = 0.15; // 15 %
            double expected = 170.0;
            assertEquals(expected, calculator.discount(price, discountRate), 1e-9);
        }

        @Test
        @DisplayName("zero discount returns original price")
        void discountZeroRate() {
            double price = 80.0;
            double discountRate = 0.0;
            assertEquals(price, calculator.discount(price, discountRate), 1e-9);
        }

        @Test
        @DisplayName("discount greater than 100% throws IllegalArgumentException")
        void discountTooHighRate() {
            assertThrows(IllegalArgumentException.class, () -> calculator.discount(100.0, 1.5));
        }
    }
}
