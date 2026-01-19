package com.example.demo;

/**
 * Utility class for price related calculations.
 *
 * It provides two independent operations:
 *  - {@link #calculate(double, double)} which computes the final amount
 *    adding a tax rate to a base price.
 *  - {@link #discount(double, double)} which applies a discount percentage
 *    to a price.
 *
 * The original implementation also contained a domain‑specific method
 * {@code calculatePrice(double, int, boolean)} that is used by the
 * {@link PricingService}. The two new methods are added to satisfy the unit
 * tests that expect them to exist.
 */
public class PriceCalculator {

    /**
     * Calcola il prezzo finale aggiungendo l'IVA (o altra tassa) al prezzo base.
     *
     * @param price prezzo base, deve essere non negativo
     * @param tax   aliquota tassa (es. 0.2 per il 20 %)
     * @return prezzo comprensivo di tassa
     * @throws IllegalArgumentException se {@code price} è negativo o se {@code tax}
     *                                  è negativo
     */
    public double calculate(double price, double tax) {
        if (price < 0) {
            throw new IllegalArgumentException("Il prezzo non può essere negativo");
        }
        if (tax < 0) {
            throw new IllegalArgumentException("La tassa non può essere negativa");
        }
        return price * (1 + tax);
    }

    /**
     * Applica uno sconto al prezzo.
     *
     * @param price          prezzo originale, deve essere non negativo
     * @param discountRate   percentuale di sconto in forma decimale (es. 0.15 = 15 %)
     * @return prezzo scontato
     * @throws IllegalArgumentException se {@code price} è negativo o se
     *                                  {@code discountRate} non è compreso tra 0 e 1
     */
    public double discount(double price, double discountRate) {
        if (price < 0) {
            throw new IllegalArgumentException("Il prezzo non può essere negativo");
        }
        if (discountRate < 0 || discountRate > 1) {
            throw new IllegalArgumentException("Il tasso di sconto deve essere compreso tra 0 e 1");
        }
        return price * (1 - discountRate);
    }

    /**
     * Calcola il prezzo finale in base ad età e stato studente.
     * Regole:
     * - Il prezzo base non può essere negativo.
     * - L'età non può essere negativa.
     * - I minori di 10 anni hanno il 100 % di sconto (gratis).
     * - Dai 10 ai 17 anni hanno il 50 % di sconto.
     * - Gli studenti sotto i 30 anni hanno il 20 % di sconto.
     * - I pensionati (> 65 anni) hanno il 30 % di sconto.
     * - Se più regole sono applicabili, viene scelto lo sconto più vantaggioso
     *   (cioè il prezzo più basso).
     * - Per tutti gli altri casi non si applica alcuno sconto.
     */
    public double calculatePrice(double basePrice, int age, boolean isStudent) {
        if (basePrice < 0) {
            throw new IllegalArgumentException("Il prezzo base non può essere negativo");
        }
        if (age < 0) {
            throw new IllegalArgumentException("L'età non può essere negativa");
        }

        double bestPrice = basePrice; // nessuno sconto
        // child free
        if (age < 10) {
            bestPrice = 0.0;
        }
        // teen 50% discount (age 10-17 inclusive)
        if (age >= 10 && age <= 17) {
            bestPrice = Math.min(bestPrice, basePrice * 0.5);
        }
        // pensioner > 65
        if (age > 65) {
            bestPrice = Math.min(bestPrice, basePrice * 0.7);
        }
        // student under 30
        if (isStudent && age < 30) {
            bestPrice = Math.min(bestPrice, basePrice * 0.8);
        }
        return bestPrice;
    }
}
