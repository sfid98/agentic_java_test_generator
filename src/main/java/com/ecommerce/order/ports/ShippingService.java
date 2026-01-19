package com.ecommerce.order.ports;

public interface ShippingService {
    void scheduleDelivery(String orderId, boolean priority);
}