package com.example.demo;

/**
 * Service class that delegates the calculation to {@link PriceCalculator}.
 * It is deliberately simple – it just creates an instance of {@link PriceCalculator}
 * and forwards the call to {@code calculatePrice}. This keeps the service stateless
 * and makes it easy to test without any Spring infrastructure.
 */
public class PricingService {

    private final PriceCalculator calculator = new PriceCalculator();

    /**
     * Calcola il prezzo finale applicando le regole di sconto definite in
     * {@link PriceCalculator#calculatePrice(double, int, boolean)}.
     *
     * @param basePrice prezzo di partenza, non negativo
     * @param age       età del cliente, compresa tra 0 e 130 inclusive
     * @param isStudent indica se il cliente è studente universitario
     * @return prezzo finale dopo aver applicato lo sconto più vantaggioso
     * @throws IllegalArgumentException se {@code basePrice} è negativo o se
     *                                  {@code age} è fuori dal range consentito
     */
    public double calculateFinalPrice(double basePrice, int age, boolean isStudent) {
        if (age < 0 || age > 130) {
            throw new IllegalArgumentException("Età del cliente non valida");
        }
        // {@link PriceCalculator} already validates basePrice
        return calculator.calculatePrice(basePrice, age, isStudent);
    }
}
