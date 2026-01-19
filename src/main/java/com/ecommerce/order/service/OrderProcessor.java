package com.ecommerce.order.service;

import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderItem;
import com.ecommerce.order.ports.*;

public class OrderProcessor {

    private final InventoryService inventory;
    private final PaymentGateway payment;
    private final ShippingService shipping;

    public OrderProcessor(InventoryService inventory, PaymentGateway payment, ShippingService shipping) {
        this.inventory = inventory;
        this.payment = payment;
        this.shipping = shipping;
    }

    public String processOrder(Order order) {
        // 1. Controllo Stock
        for (OrderItem item : order.getItems()) {
            if (!inventory.checkStock(item.getSku(), item.getQuantity())) {
                throw new IllegalStateException("Prodotto non disponibile: " + item.getSku());
            }
        }

        // 2. Controllo Pagamento
        // Regola: Se è VIP, proviamo a pre-autorizzare solo 1€ per verifica, altrimenti tutto l'importo.
        double authAmount = order.getCustomer().isVip() ? 1.0 : order.getTotalAmount();
        
        if (!payment.authorize(authAmount, order.getCustomer().getEmail())) {
            return "PAYMENT_DECLINED";
        }

        // 3. Conferma Ordine
        inventory.reserveStock(order.getItems());
        
        // 4. Spedizione
        // Regola: Spedizione prioritaria se VIP o se l'ordine supera i 100€
        boolean isPriority = order.getCustomer().isVip() || order.getTotalAmount() > 100.0;
        shipping.scheduleDelivery(order.getId(), isPriority);

        return "ORDER_CONFIRMED";
    }
}