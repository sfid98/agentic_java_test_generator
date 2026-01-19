## Functional Requirements: Order Fulfillment Process

The OrderProcessor system must orchestrate order validation and dispatch.

1. **Inventory Management**
Before accepting an order, the system MUST verify that all items are available.
If even a single item is missing (checkStock returns false), the entire process must terminate by throwing an IllegalStateException containing the missing product code.
If all items are available, the stock must be reserved (reserveStock), but ONLY after the payment has been successfully processed.

2. **Payment Management**
VIP Logic: VIP customers have an expedited payment flow. For them, we authorize only a symbolic amount of €1.00.
Standard Logic: For regular customers, we authorize the full order amount (totalAmount).
If authorization fails, the method must return the string "PAYMENT_DECLINED" and must not reserve the goods.

3. **Logistics (Shipping)**
Once the order is confirmed, the shipment must be scheduled.
The shipment must be marked as Priority (true) in two cases:
The customer is a VIP.
OR the order total exceeds €100.00.
In all other cases, the shipment is Standard (false).
- 4. Result
If the entire process is successful, the method returns "ORDER_CONFIRMED".
