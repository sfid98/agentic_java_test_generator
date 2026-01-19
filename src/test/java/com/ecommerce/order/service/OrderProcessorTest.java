package com.ecommerce.order.service;

import com.ecommerce.order.model.Customer;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderItem;
import com.ecommerce.order.ports.InventoryService;
import com.ecommerce.order.ports.PaymentGateway;
import com.ecommerce.order.ports.ShippingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class OrderProcessorTest {

    @Mock
    private InventoryService inventory;

    @Mock
    private PaymentGateway payment;

    @Mock
    private ShippingService shipping;

    @InjectMocks
    private OrderProcessor processor;

    // ---------------------------------------------------------------------
    // Helper methods to build domain objects
    // ---------------------------------------------------------------------
    private Customer customer(String email, boolean vip) {
        return new Customer(email, vip);
    }

    private OrderItem item(String sku, int qty) {
        return new OrderItem(sku, qty);
    }

    private Order order(double total, Customer cust, OrderItem... items) {
        String id = UUID.randomUUID().toString();
        List<OrderItem> itemList = new ArrayList<>(Arrays.asList(items));
        return new Order(id, cust, itemList, total);
    }

    // ---------------------------------------------------------------------
    // Scenario S1 – first item not in stock
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("S1 – throws when first item stock is missing")
    void testStockMissingFirstItemThrowsException() {
        OrderItem first = item("SKU-A", 2);
        OrderItem second = item("SKU-B", 1);
        Order order = order(50.0, customer("user@example.com", false), first, second);

        when(inventory.checkStock("SKU-A", 2)).thenReturn(false);
        // No need to stub further calls – they must not happen

        assertThatThrownBy(() -> processor.processOrder(order))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SKU-A");

        // Verify only the first checkStock call happened and then short‑circuit
        verify(inventory).checkStock("SKU-A", 2);
        verifyNoMoreInteractions(inventory, payment, shipping);
    }

    // ---------------------------------------------------------------------
    // Scenario S2 – second item not in stock
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("S2 – throws when second item stock is missing after first succeeds")
    void testStockMissingSecondItemThrowsException() {
        OrderItem first = item("SKU-1", 1);
        OrderItem second = item("SKU-2", 3);
        Order order = order(70.0, customer("user@example.com", false), first, second);

        when(inventory.checkStock("SKU-1", 1)).thenReturn(true);
        when(inventory.checkStock("SKU-2", 3)).thenReturn(false);

        assertThatThrownBy(() -> processor.processOrder(order))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SKU-2");

        InOrder inOrder = inOrder(inventory);
        inOrder.verify(inventory).checkStock("SKU-1", 1);
        inOrder.verify(inventory).checkStock("SKU-2", 3);
        verifyNoMoreInteractions(payment, shipping);
    }

    // ---------------------------------------------------------------------
    // Scenario S3 – payment declined for VIP customer
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("S3 – returns PAYMENT_DECLINED when VIP payment fails")
    void testPaymentDeclinedVip() {
        Customer vip = customer("vip@example.com", true);
        OrderItem i1 = item("SKU-1", 1);
        Order order = order(200.0, vip, i1);

        // Stock ok
        when(inventory.checkStock(anyString(), anyInt())).thenReturn(true);
        // Payment fails for the 1€ pre‑auth
        when(payment.authorize(1.0, "vip@example.com")).thenReturn(false);

        String result = processor.processOrder(order);
        assertThat(result).isEqualTo("PAYMENT_DECLINED");

        verify(inventory, times(1)).checkStock("SKU-1", 1);
        verify(payment).authorize(1.0, "vip@example.com");
        verify(inventory, never()).reserveStock(any());
        verify(shipping, never()).scheduleDelivery(anyString(), anyBoolean());
    }

    // ---------------------------------------------------------------------
    // Scenario S4 – payment declined for regular customer
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("S4 – returns PAYMENT_DECLINED when regular payment fails")
    void testPaymentDeclinedRegular() {
        Customer regular = customer("reg@example.com", false);
        OrderItem i1 = item("SKU-1", 2);
        OrderItem i2 = item("SKU-2", 1);
        Order order = order(120.0, regular, i1, i2);

        when(inventory.checkStock(anyString(), anyInt())).thenReturn(true);
        when(payment.authorize(120.0, "reg@example.com")).thenReturn(false);

        String result = processor.processOrder(order);
        assertThat(result).isEqualTo("PAYMENT_DECLINED");

        verify(inventory, times(2)).checkStock(anyString(), anyInt());
        verify(payment).authorize(120.0, "reg@example.com");
        verify(inventory, never()).reserveStock(any());
        verify(shipping, never()).scheduleDelivery(anyString(), anyBoolean());
    }

    // ---------------------------------------------------------------------
    // Scenario S5 – successful order for VIP (priority shipping)
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("S5 – successful VIP order results in priority shipping")
    void testSuccessfulOrderVip() {
        Customer vip = customer("vip@example.com", true);
        OrderItem i1 = item("SKU-1", 1);
        OrderItem i2 = item("SKU-2", 2);
        Order order = order(80.0, vip, i1, i2);

        when(inventory.checkStock(anyString(), anyInt())).thenReturn(true);
        when(payment.authorize(1.0, "vip@example.com")).thenReturn(true);

        String result = processor.processOrder(order);
        assertThat(result).isEqualTo("ORDER_CONFIRMED");

        // verify stock checks for each item
        verify(inventory).checkStock("SKU-1", 1);
        verify(inventory).checkStock("SKU-2", 2);
        verify(payment).authorize(1.0, "vip@example.com");
        verify(inventory).reserveStock(order.getItems());
        verify(shipping).scheduleDelivery(order.getId(), true); // priority because VIP
    }

    // ---------------------------------------------------------------------
    // Scenario S6 – successful non‑VIP order, total <= 100 (standard shipping)
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("S6 – successful non‑VIP order <=100 uses standard shipping")
    void testSuccessfulOrderNonVipStandardShipping() {
        Customer regular = customer("reg@example.com", false);
        OrderItem i1 = item("SKU-1", 1);
        Order order = order(90.0, regular, i1);

        when(inventory.checkStock(anyString(), anyInt())).thenReturn(true);
        when(payment.authorize(90.0, "reg@example.com")).thenReturn(true);

        String result = processor.processOrder(order);
        assertThat(result).isEqualTo("ORDER_CONFIRMED");

        verify(inventory).checkStock("SKU-1", 1);
        verify(payment).authorize(90.0, "reg@example.com");
        verify(inventory).reserveStock(order.getItems());
        verify(shipping).scheduleDelivery(order.getId(), false); // not priority
    }

    // ---------------------------------------------------------------------
    // Scenario S7 – successful non‑VIP order, total > 100 (priority shipping)
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("S7 – successful non‑VIP order >100 uses priority shipping")
    void testSuccessfulOrderNonVipPriorityShipping() {
        Customer regular = customer("reg@example.com", false);
        OrderItem i1 = item("SKU-1", 5);
        Order order = order(150.0, regular, i1);

        when(inventory.checkStock(anyString(), anyInt())).thenReturn(true);
        when(payment.authorize(150.0, "reg@example.com")).thenReturn(true);

        String result = processor.processOrder(order);
        assertThat(result).isEqualTo("ORDER_CONFIRMED");

        verify(inventory).checkStock("SKU-1", 5);
        verify(payment).authorize(150.0, "reg@example.com");
        verify(inventory).reserveStock(order.getItems());
        verify(shipping).scheduleDelivery(order.getId(), true); // priority because amount > 100
    }

    // ---------------------------------------------------------------------
    // Scenario S8 – edge amount exactly 100 (standard shipping)
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("S8 – total exactly 100 uses standard shipping for non‑VIP")
    void testEdgeAmountExactly100() {
        Customer regular = customer("reg@example.com", false);
        OrderItem i1 = item("SKU-1", 2);
        Order order = order(100.0, regular, i1);

        when(inventory.checkStock(anyString(), anyInt())).thenReturn(true);
        when(payment.authorize(100.0, "reg@example.com")).thenReturn(true);

        String result = processor.processOrder(order);
        assertThat(result).isEqualTo("ORDER_CONFIRMED");

        verify(shipping).scheduleDelivery(order.getId(), false);
    }

    // ---------------------------------------------------------------------
    // Scenario S9 – verify the exact interaction order in a happy path
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("S9 – interaction order is stock → payment → reserve → ship")
    void testInteractionOrderHappyPath() {
        Customer regular = customer("reg@example.com", false);
        OrderItem i1 = item("SKU-A", 1);
        OrderItem i2 = item("SKU-B", 2);
        Order order = order(130.0, regular, i1, i2);

        when(inventory.checkStock(anyString(), anyInt())).thenReturn(true);
        when(payment.authorize(130.0, "reg@example.com")).thenReturn(true);

        String result = processor.processOrder(order);
        assertThat(result).isEqualTo("ORDER_CONFIRMED");

        InOrder inOrder = inOrder(inventory, payment, shipping);
        // stock checks in the order of items
        inOrder.verify(inventory).checkStock("SKU-A", 1);
        inOrder.verify(inventory).checkStock("SKU-B", 2);
        // payment
        inOrder.verify(payment).authorize(130.0, "reg@example.com");
        // reserve stock
        inOrder.verify(inventory).reserveStock(order.getItems());
        // schedule delivery (priority because amount > 100)
        inOrder.verify(shipping).scheduleDelivery(order.getId(), true);
    }

    // ---------------------------------------------------------------------
    // Scenario S10 – ensure no side‑effects when payment is declined after stock OK
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("S10 – no reserve or shipping when payment declines")
    void testNoSideEffectsWhenPaymentDeclined() {
        Customer regular = customer("reg@example.com", false);
        OrderItem i1 = item("SKU-1", 3);
        Order order = order(85.0, regular, i1);

        when(inventory.checkStock(anyString(), anyInt())).thenReturn(true);
        when(payment.authorize(85.0, "reg@example.com")).thenReturn(false);

        String result = processor.processOrder(order);
        assertThat(result).isEqualTo("PAYMENT_DECLINED");

        verify(inventory).checkStock("SKU-1", 3);
        verify(payment).authorize(85.0, "reg@example.com");
        verify(inventory, never()).reserveStock(any());
        verify(shipping, never()).scheduleDelivery(anyString(), anyBoolean());
    }
}