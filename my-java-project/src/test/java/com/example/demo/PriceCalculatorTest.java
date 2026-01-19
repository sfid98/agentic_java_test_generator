package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PriceCalculator}.
 *
 * The {@link PriceCalculator#calculatePrice(double, int, boolean)} method calculates the final price
 * based on a base price, the customer's age and whether the customer is a student. The tests cover
 * the normal discount rules as well as validation of illegal arguments.
 */
class PriceCalculatorTest {

    private PriceCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new PriceCalculator();
    }

    @Nested
    @DisplayName("Valid price calculations")
    class ValidCalculations {
        @Test
        @DisplayName("Minor (under 18) receives 50% discount")
        void minorGetsHalfPrice() {
            double basePrice = 200.0;
            int age = 16;
            boolean isStudent = false; // irrelevant for minors
            double expected = 100.0; // 50% of 200
            assertEquals(expected, calculator.calculatePrice(basePrice, age, isStudent), 1e-9);
        }

        @Test
        @DisplayName("Adult student (18+) receives 10% discount")
        void adultStudentGetsTenPercentDiscount() {
            double basePrice = 150.0;
            int age = 20;
            boolean isStudent = true;
            double expected = 135.0; // 90% of 150
            assertEquals(expected, calculator.calculatePrice(basePrice, age, isStudent), 1e-9);
        }

        @Test
        @DisplayName("Adult non‑student pays full price")
        void adultNonStudentPaysFullPrice() {
            double basePrice = 80.0;
            int age = 30;
            boolean isStudent = false;
            double expected = 80.0; // no discount
            assertEquals(expected, calculator.calculatePrice(basePrice, age, isStudent), 1e-9);
        }
    }

    @Nested
    @DisplayName("Invalid arguments")
    class InvalidArguments {
        @Test
        @DisplayName("Negative base price throws IllegalArgumentException")
        void negativeBasePrice() {
            assertThrows(IllegalArgumentException.class, () -> calculator.calculatePrice(-10.0, 25, false));
        }

        @Test
        @DisplayName("Negative age throws IllegalArgumentException")
        void negativeAge() {
            assertThrows(IllegalArgumentException.class, () -> calculator.calculatePrice(50.0, -5, true));
        }
    }
}