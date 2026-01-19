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
