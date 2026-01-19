package com.example.demo;

public class PriceCalculator {

    /**
     * Calcola il prezzo finale basato su età e status studente.
     * * Regole:
     * - Il prezzo base non può essere negativo.
     * - L'età non può essere negativa.
     * - I minori di 18 anni hanno il 50% di sconto.
     * - Gli studenti (18+) hanno il 10% di sconto.
     * - Gli altri pagano prezzo pieno.
     */
    public double calculatePrice(double basePrice, int age, boolean isStudent) {
        if (basePrice < 0) {
            throw new IllegalArgumentException("Il prezzo base non può essere negativo");
        }

        if (age < 0) {
            throw new IllegalArgumentException("L'età non può essere negativa");
        }

        // Logica di sconto
        if (age < 18) {
            return basePrice * 0.5;
        } else if (isStudent) {
            return basePrice * 0.9;
        } else {
            return basePrice;
        }
    }
}