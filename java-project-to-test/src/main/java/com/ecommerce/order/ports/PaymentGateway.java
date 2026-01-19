package com.ecommerce.order.ports;

public interface PaymentGateway {
    // Ritorna true se il pagamento è autorizzato, false se declinato
    boolean authorize(double amount, String customerEmail);
}