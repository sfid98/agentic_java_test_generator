package com.ecommerce.order.service;

import com.ecommerce.order.model.Customer;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderItem;
import com.ecommerce.order.ports.InventoryService;
import com.ecommerce.order.ports.PaymentGateway;
import com.ecommerce.order.ports.ShippingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderProcessorTest {

    @Mock
    private InventoryService inventoryService;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private ShippingService shippingService;

    @InjectMocks
    private OrderProcessor orderProcessor;

    // ---------------------------------------------------------------------
    // Helper builders to keep tests readable
    // ---------------------------------------------------------------------
    private static class CustomerBuilder {
        private boolean vip = false;
        private String email = "customer@example.com";
        CustomerBuilder vip(boolean vip) { this.vip = vip; return this; }
        CustomerBuilder email(String email) { this.email = email; return this; }
        Customer build() { return new Customer(email, vip); }
    }

    private static class OrderItemBuilder {
        private String sku = "SKU-001";
        private int quantity = 1;
        OrderItemBuilder sku(String sku) { this.sku = sku; return this; }
        OrderItemBuilder quantity(int qty) { this.quantity = qty; return this; }
        OrderItem build() { return new OrderItem(sku, quantity); }
    }

    private static class OrderBuilder {
        private String id = UUID.randomUUID().toString();
        private Customer customer = new CustomerBuilder().build();
        private final List<OrderItem> items = new ArrayList<>();
        private double totalAmount = 0.0;
        OrderBuilder id(String id) { this.id = id; return this; }
        OrderBuilder customer(Customer customer) { this.customer = customer; return this; }
        OrderBuilder addItem(OrderItem item) { this.items.add(item); return this; }
        OrderBuilder totalAmount(double amount) { this.totalAmount = amount; return this; }
        Order build() { return new Order(id, customer, Collections.unmodifiableList(new ArrayList<>(items)), totalAmount); }
    }

    // ---------------------------------------------------------------------
    // 1. Happy path – VIP (priority shipping)
    // ---------------------------------------------------------------------
    @Test
    void processOrder_whenVipAndStockAvailable_andPaymentSucceeds_returnsConfirmed_andSchedulesPriorityShipping() {
        // arrange
        Customer vipCustomer = new CustomerBuilder().vip(true).email("vip@example.com").build();
        OrderItem item1 = new OrderItemBuilder().sku("SKU-1").quantity(2).build();
        OrderItem item2 = new OrderItemBuilder().sku("SKU-2").quantity(1).build();
        Order order = new OrderBuilder()
                .customer(vipCustomer)
                .addItem(item1)
                .addItem(item2)
                .totalAmount(50.0) // amount is irrelevant for VIP
                .build();

        when(inventoryService.checkStock(eq("SKU-1"), eq(2))).thenReturn(true);
        when(inventoryService.checkStock(eq("SKU-2"), eq(1))).thenReturn(true);
        when(paymentGateway.authorize(eq(1.0), eq(vipCustomer.getEmail()))).thenReturn(true);

        // act
        String result = orderProcessor.processOrder(order);

        // assert
        assertEquals("ORDER_CONFIRMED", result);
        // verify stock checks for each item
        verify(inventoryService).checkStock("SKU-1", 2);
        verify(inventoryService).checkStock("SKU-2", 1);
        // verify payment amount is 1.0 for VIP
        verify(paymentGateway).authorize(1.0, vipCustomer.getEmail());
        // verify reserveStock called once with the exact list
        verify(inventoryService).reserveStock(order.getItems());
        // verify priority shipping
        verify(shippingService).scheduleDelivery(eq(order.getId()), eq(true));
        // verify call order
        InOrder inOrder = inOrder(inventoryService, paymentGateway, shippingService);
        inOrder.verify(inventoryService).checkStock("SKU-1", 2);
        inOrder.verify(inventoryService).checkStock("SKU-2", 1);
        inOrder.verify(paymentGateway).authorize(1.0, vipCustomer.getEmail());
        inOrder.verify(inventoryService).reserveStock(order.getItems());
        inOrder.verify(shippingService).scheduleDelivery(order.getId(), true);
    }

    // ---------------------------------------------------------------------
    // 2. Happy path – Regular, total > 100 (priority shipping)
    // ---------------------------------------------------------------------
    @Test
    void processOrder_whenRegularAndTotalGreaterThan100_andAllStockAvailable_andPaymentSucceeds_returnsConfirmed_andSchedulesPriorityShipping() {
        // arrange
        Customer regular = new CustomerBuilder().vip(false).email("regular@example.com").build();
        OrderItem item = new OrderItemBuilder().sku("SKU-10").quantity(5).build();
        Order order = new OrderBuilder()
                .customer(regular)
                .addItem(item)
                .totalAmount(150.0)
                .build();

        when(inventoryService.checkStock(eq("SKU-10"), eq(5))).thenReturn(true);
        when(paymentGateway.authorize(eq(150.0), eq(regular.getEmail()))).thenReturn(true);

        // act
        String result = orderProcessor.processOrder(order);

        // assert
        assertEquals("ORDER_CONFIRMED", result);
        verify(inventoryService).checkStock("SKU-10", 5);
        verify(paymentGateway).authorize(150.0, regular.getEmail());
        verify(inventoryService).reserveStock(order.getItems());
        verify(shippingService).scheduleDelivery(order.getId(), true);
    }

    // ---------------------------------------------------------------------
    // 3. Happy path – Regular, total <= 100 (non‑priority shipping)
    // ---------------------------------------------------------------------
    @Test
    void processOrder_whenRegularAndTotalAtOrBelow100_andAllStockAvailable_andPaymentSucceeds_returnsConfirmed_andSchedulesNormalShipping() {
        // arrange
        Customer regular = new CustomerBuilder().vip(false).email("reg2@example.com").build();
        OrderItem item = new OrderItemBuilder().sku("SKU-20").quantity(1).build();
        Order order = new OrderBuilder()
                .customer(regular)
                .addItem(item)
                .totalAmount(80.0)
                .build();

        when(inventoryService.checkStock("SKU-20", 1)).thenReturn(true);
        when(paymentGateway.authorize(80.0, regular.getEmail())).thenReturn(true);

        // act
        String result = orderProcessor.processOrder(order);

        // assert
        assertEquals("ORDER_CONFIRMED", result);
        verify(inventoryService).checkStock("SKU-20", 1);
        verify(paymentGateway).authorize(80.0, regular.getEmail());
        verify(inventoryService).reserveStock(order.getItems());
        verify(shippingService).scheduleDelivery(order.getId(), false);
    }

    // ---------------------------------------------------------------------
    // 4. Stock missing – should throw IllegalStateException, no further calls
    // ---------------------------------------------------------------------
    @Test
    void processOrder_whenStockMissing_throwsIllegalStateException_andNoFurtherInteractions() {
        // arrange
        Customer customer = new CustomerBuilder().vip(false).email("c@example.com").build();
        OrderItem missing = new OrderItemBuilder().sku("MISSING-SKU").quantity(3).build();
        OrderItem present = new OrderItemBuilder().sku("PRESENT-SKU").quantity(2).build();
        Order order = new OrderBuilder()
                .customer(customer)
                .addItem(missing)
                .addItem(present)
                .totalAmount(60.0)
                .build();

        when(inventoryService.checkStock(eq("MISSING-SKU"), eq(3))).thenReturn(false);
        // the second item should never be queried, but we can leave it unstubbed

        // act & assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> orderProcessor.processOrder(order));
        assertTrue(ex.getMessage().contains("MISSING-SKU"));
        // verify no payment, reserve or shipping calls
        verify(paymentGateway, never()).authorize(anyDouble(), anyString());
        verify(inventoryService, never()).reserveStock(any());
        verify(shippingService, never()).scheduleDelivery(anyString(), anyBoolean());
    }

    // ---------------------------------------------------------------------
    // 5. Payment declined – VIP
    // ---------------------------------------------------------------------
    @Test
    void processOrder_whenVipAndPaymentDeclined_returnsPaymentDeclined_andNoReservationOrShipping() {
        // arrange
        Customer vip = new CustomerBuilder().vip(true).email("vip2@example.com").build();
        OrderItem item = new OrderItemBuilder().sku("SKU-99").quantity(1).build();
        Order order = new OrderBuilder()
                .customer(vip)
                .addItem(item)
                .totalAmount(40.0)
                .build();

        when(inventoryService.checkStock("SKU-99", 1)).thenReturn(true);
        when(paymentGateway.authorize(1.0, vip.getEmail())).thenReturn(false);

        // act
        String result = orderProcessor.processOrder(order);

        // assert
        assertEquals("PAYMENT_DECLINED", result);
        verify(inventoryService).checkStock("SKU-99", 1);
        verify(paymentGateway).authorize(1.0, vip.getEmail());
        verify(inventoryService, never()).reserveStock(any());
        verify(shippingService, never()).scheduleDelivery(anyString(), anyBoolean());
    }

    // ---------------------------------------------------------------------
    // 6. Payment declined – Regular
    // ---------------------------------------------------------------------
    @Test
    void processOrder_whenRegularAndPaymentDeclined_returnsPaymentDeclined_andNoReservationOrShipping() {
        // arrange
        Customer regular = new CustomerBuilder().vip(false).email("reg3@example.com").build();
        OrderItem item = new OrderItemBuilder().sku("SKU-88").quantity(2).build();
        Order order = new OrderBuilder()
                .customer(regular)
                .addItem(item)
                .totalAmount(70.0)
                .build();

        when(inventoryService.checkStock("SKU-88", 2)).thenReturn(true);
        when(paymentGateway.authorize(70.0, regular.getEmail())).thenReturn(false);

        // act
        String result = orderProcessor.processOrder(order);

        // assert
        assertEquals("PAYMENT_DECLINED", result);
        verify(inventoryService).checkStock("SKU-88", 2);
        verify(paymentGateway).authorize(70.0, regular.getEmail());
        verify(inventoryService, never()).reserveStock(any());
        verify(shippingService, never()).scheduleDelivery(anyString(), anyBoolean());
    }

    // ---------------------------------------------------------------------
    // 7. Boundary – total exactly 100 (non‑priority)
    // ---------------------------------------------------------------------
    @Test
    void processOrder_whenRegularTotalExactly100_prioritisationIsFalse() {
        Customer regular = new CustomerBuilder().vip(false).email("boundary100@example.com").build();
        OrderItem item = new OrderItemBuilder().sku("SKU-B1").quantity(1).build();
        Order order = new OrderBuilder()
                .customer(regular)
                .addItem(item)
                .totalAmount(100.0)
                .build();

        when(inventoryService.checkStock("SKU-B1", 1)).thenReturn(true);
        when(paymentGateway.authorize(100.0, regular.getEmail())).thenReturn(true);

        String result = orderProcessor.processOrder(order);
        assertEquals("ORDER_CONFIRMED", result);
        verify(shippingService).scheduleDelivery(order.getId(), false);
    }

    // ---------------------------------------------------------------------
    // 8. Boundary – total 100.01 (priority)
    // ---------------------------------------------------------------------
    @Test
    void processOrder_whenRegularTotalJustAbove100_prioritisationIsTrue() {
        Customer regular = new CustomerBuilder().vip(false).email("boundary100p@example.com").build();
        OrderItem item = new OrderItemBuilder().sku("SKU-B2").quantity(1).build();
        Order order = new OrderBuilder()
                .customer(regular)
                .addItem(item)
                .totalAmount(100.01)
                .build();

        when(inventoryService.checkStock("SKU-B2", 1)).thenReturn(true);
        when(paymentGateway.authorize(100.01, regular.getEmail())).thenReturn(true);

        String result = orderProcessor.processOrder(order);
        assertEquals("ORDER_CONFIRMED", result);
        verify(shippingService).scheduleDelivery(order.getId(), true);
    }

    // ---------------------------------------------------------------------
    // 9. Multiple items – ensure each stock check and that reserve receives the same list
    // ---------------------------------------------------------------------
    @Test
    void processOrder_withMultipleItems_verifiesIndividualStockChecks_andReserveUsesSameItemList() {
        Customer regular = new CustomerBuilder().vip(false).email("multi@example.com").build();
        OrderItem item1 = new OrderItemBuilder().sku("SKU-M1").quantity(1).build();
        OrderItem item2 = new OrderItemBuilder().sku("SKU-M2").quantity(3).build();
        OrderItem item3 = new OrderItemBuilder().sku("SKU-M3").quantity(2).build();
        Order order = new OrderBuilder()
                .customer(regular)
                .addItem(item1)
                .addItem(item2)
                .addItem(item3)
                .totalAmount(200.0)
                .build();

        when(inventoryService.checkStock(anyString(), anyInt())).thenReturn(true);
        when(paymentGateway.authorize(200.0, regular.getEmail())).thenReturn(true);

        String result = orderProcessor.processOrder(order);
        assertEquals("ORDER_CONFIRMED", result);

        // verify each SKU/quantity checked exactly once
        verify(inventoryService).checkStock("SKU-M1", 1);
        verify(inventoryService).checkStock("SKU-M2", 3);
        verify(inventoryService).checkStock("SKU-M3", 2);
        // capture the argument passed to reserveStock
        ArgumentCaptor<List<OrderItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(inventoryService).reserveStock(captor.capture());
        List<OrderItem> captured = captor.getValue();
        assertEquals(order.getItems(), captured);
        // shipping should be priority because total > 100
        verify(shippingService).scheduleDelivery(eq(order.getId()), eq(true));
    }

    // ---------------------------------------------------------------------
    // 10. ReserveStock throws – shipping must not be called, exception propagates
    // ---------------------------------------------------------------------
    @Test
    void processOrder_whenReserveStockThrows_propagatesException_andDoesNotScheduleShipping() {
        Customer regular = new CustomerBuilder().vip(false).email("exc@example.com").build();
        OrderItem item = new OrderItemBuilder().sku("SKU-ERR").quantity(1).build();
        Order order = new OrderBuilder()
                .customer(regular)
                .addItem(item)
                .totalAmount(120.0)
                .build();

        when(inventoryService.checkStock("SKU-ERR", 1)).thenReturn(true);
        when(paymentGateway.authorize(120.0, regular.getEmail())).thenReturn(true);
        doThrow(new IllegalStateException("DB error"))
                .when(inventoryService).reserveStock(any());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> orderProcessor.processOrder(order));
        assertEquals("DB error", ex.getMessage());
        verify(shippingService, never()).scheduleDelivery(anyString(), anyBoolean());
    }
}