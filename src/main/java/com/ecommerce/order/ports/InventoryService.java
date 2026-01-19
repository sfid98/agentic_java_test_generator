package com.ecommerce.order.ports;
import com.ecommerce.order.model.OrderItem;
import java.util.List;

public interface InventoryService {
    boolean checkStock(String sku, int quantity);
    void reserveStock(List<OrderItem> items);
}