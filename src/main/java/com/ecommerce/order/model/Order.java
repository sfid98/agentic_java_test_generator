package com.ecommerce.order.model;
import java.util.List;

public class Order {
    private String id;
    private Customer customer;
    private List<OrderItem> items;
    private double totalAmount;

    // Costruttore, Getters... (L'AI li userà)
    public Order(String id, Customer customer, List<OrderItem> items, double totalAmount) {
        this.id = id;
        this.customer = customer;
        this.items = items;
        this.totalAmount = totalAmount;
    }
    public String getId() { return id; }
    public Customer getCustomer() { return customer; }
    public List<OrderItem> getItems() { return items; }
    public double getTotalAmount() { return totalAmount; }
}