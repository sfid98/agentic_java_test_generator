# 🧬 Agentic Java SDET: Autonomous Unit Testing Pipeline

![Python](https://img.shields.io/badge/Python-3.10%2B-blue)
![Java](https://img.shields.io/badge/Java-17%2B-orange)
![Framework](https://img.shields.io/badge/Microsoft%20Agent%20Framework-Preview-green)
![Status](https://img.shields.io/badge/Status-Prototype-yellow)

## 🎯 Project Overview

This project showcases an **autonomous multi-agent system** capable of generating Enterprise-grade Unit Tests (JUnit 5 + Mockito) starting directly from **Business Requirements** written in natural language.

Unlike standard "code generation" tools that simply look at the code (often replicating existing bugs), this system uses a **Chain of Responsibility** pattern to ensure tests validate the *business logic* and strictly adhere to QA best practices.

### 🚀 Key Features for Enterprise

* **Requirements-Driven Testing:** The agents prioritize `requirements.md` over implementation details to ensure business compliance.
* **Self-Healing Architecture:** If a test fails compilation or execution, the agents analyze the Maven stack trace and rewrite the code automatically (up to N retries).
* **Enterprise Stack Support:** Built to handle Spring Boot style architectures with Dependency Injection and Mocking strategies.
* **Privacy-First:** Configurable to run 100% locally using **Ollama (Llama 3.2)** or cloud-native with **Azure OpenAI**.

---

## 🏗️ Architecture

The system utilizes the **Microsoft Agent Framework** to orchestrate three specialized agents:

```mermaid
graph TD
    User[User / CI Pipeline] -->|Inputs Requirements| Analyst
    Analyst[🤖 QA Analyst Agent] -->|Produces Test Plan| Dev
    Dev[👨‍💻 Java SDET Agent] -->|Writes JUnit Code| Runner
    Runner[⚙️ Build Runner Agent] -->|Executes Maven| Validation{Build Success?}
    Validation -->|Yes| End[✅ Test Suite Validated]
    Validation -->|No| Dev
    style Analyst fill:#e1f5fe,stroke:#01579b
    style Dev fill:#fff3e0,stroke:#ff6f00
    style Runner fill:#e8f5e9,stroke:#1b5e20

## 🔍 End-to-End Example: From Specs to Code

Here is a real-world example demonstrating how the **Agentic Framework** translates raw business requirements into executable test logic.

### 1. The Input (`specs/requirements.md`)
The QA Analyst Agent reads the functional specification provided by the business stakeholders:

> **1. Inventory Management**
> Before accepting an order, the system MUST verify that all items are available.
> If even a single item is missing (**checkStock returns false**), the entire process must terminate by **throwing an IllegalStateException** containing the missing product code. [...]

### 2. The Output (Generated JUnit Code)
The SDET Agent interprets "terminate process" and "single item missing" to generate a **Short-Circuit Test Pattern**. It understands that if the first call fails, no further interactions with the system should occur.

```java
@Test
@DisplayName("S1 – throws when first item stock is missing")
void testStockMissingFirstItemThrowsException() {
    // ARRANGE: Setup data derived from the Requirement context
    OrderItem first = new OrderItem("SKU-A", 2);
    OrderItem second = new OrderItem("SKU-B", 1);
    Order order = new Order("id", new Customer("user@example.com", false), List.of(first, second), 50.0);

    // MOCKING: Simulate "checkStock returns false" behavior
    when(inventory.checkStock("SKU-A", 2)).thenReturn(false);

    // ACT & ASSERT: Verify the "IllegalStateException" rule
    assertThatThrownBy(() -> processor.processOrder(order))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("SKU-A"); // Verifies the "missing product code" requirement

    // VERIFICATION: Ensure strict boundary compliance (No side effects)
    verify(inventory).checkStock("SKU-A", 2);
    
    // Critical: Proves the process "terminated" immediately as requested
    verifyNoMoreInteractions(inventory, payment, shipping);
}
