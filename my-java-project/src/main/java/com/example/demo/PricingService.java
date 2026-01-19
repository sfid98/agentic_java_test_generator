package com.example.demo;

/**
 * Service that calculates the final price based on base price, age and student status.
 * <p>
 * The discount rules are derived from the test suite:
 * <ul>
 *   <li>Base price must be non‑negative, otherwise {@code IllegalArgumentException}</li>
 *   <li>Age must be between 0 and 130 (inclusive); otherwise {@code IllegalArgumentException}</li>
 *   <li>Children younger than 10 years receive a 100 % discount (free).</li>
 *   <li>Age 10 – 17 (inclusive) receive a 50 % discount.</li>
 *   <li>Students younger than 30 receive a 20 % discount.</li>
 *   <li>Pensioners older than 65 (i.e. age &gt; 65) receive a 30 % discount.</li>
 *   <li>If multiple discounts could apply, the most favourable (largest) discount is used.</li>
 * </ul>
 */
public class PricingService {

    /**
     * Calculates the final price.
     *
     * @param basePrice the original price, must be &ge; 0
     * @param age the customer's age, must be between 0 and 130 inclusive
     * @param isStudent {@code true} if the customer is a university student
     * @return the price after applying the best applicable discount
     */
    public double calculateFinalPrice(double basePrice, int age, boolean isStudent) {
        if (basePrice < 0) {
            throw new IllegalArgumentException("Prezzo base non può essere negativo");
        }
        if (age < 0 || age > 130) {
            throw new IllegalArgumentException("Età del cliente non valida");
        }
        // Determine the best discount rate (as a fraction, e.g. 0.20 for 20%).
        double discount = 0.0;

        // Child discount (free) for age < 10
        if (age < 10) {
            discount = 1.0; // 100% discount
        } else if (age <= 17) { // teen discount 50% for ages 10-17 inclusive
            discount = 0.5;
        } else {
            // Adult logic (age >= 18)
            if (age > 65) {
                discount = 0.30; // pensioner discount
            }
            if (isStudent && age < 30) {
                // student discount 20%, keep the higher discount if any
                discount = Math.max(discount, 0.20);
            }
        }
        // Apply discount
        return basePrice * (1.0 - discount);
    }
}
