package com.ecommerce.order.model;
public class Customer {
    private boolean isVip;
    private String email;
    
    public Customer(String email, boolean isVip) { 
        this.email = email; 
        this.isVip = isVip; 
    }
    public boolean isVip() { return isVip; }
    public String getEmail() { return email; }
}