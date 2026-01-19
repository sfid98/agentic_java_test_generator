import asyncio
from agent_framework.ollama import OllamaChatClient
from tools import read_java_file, save_test_file, run_maven_test

MODEL_ID = "gpt-oss:120b-cloud"
MAX_RETRIES = 3

REQ_FILE = "specs/requirements.md"
SERVICE_CLASS = "src/main/java/com/ecommerce/order/service/OrderProcessor.java"

async def main():
    client = OllamaChatClient(model_id=MODEL_ID)
    
    analyst = client.as_agent(
        name="QA_Analyst",
        instructions="""
        You are a Senior QA Analyst expert in Enterprise E-Commerce systems.
        Your task is NOT to write code, but to define the testing strategy based on requirements.
        
        Analyze the requirements and the provided Java code. Produce a discursive plan listing:
        1. Which methods to mock (e.g., InventoryService, PaymentGateway).
        2. Test scenarios (e.g., "VIP user with stock available", "Payment failed").
        3. Expected verifications (e.g., "Must throw exception", "Must call shipping with priority=true").
        """
    )

    java_dev = client.as_agent(
        name="Java_SDET",
        instructions="""
        You are a Senior Java Developer specializing in testing with JUnit 5 and Mockito.
        
        YOUR GOAL:
        Produce compilable and correct Java test code based on the Analyst's plan.
        
        STRICT TECHNICAL RULES:
        - Use `@ExtendWith(MockitoExtension.class)` on the class.
        - Use `@Mock` for interfaces and `@InjectMocks` for the class under test.
        - For verifications, use `verify(mock, times(1)).method(...)` or `verify(mock, never()).method(...)`.
        - Correctly handle mock return types (e.g., `when(...).thenReturn(...)`).
        - Correctly import classes (e.g., `com.ecommerce.order.model.*`).
        
        When asked to fix an error, analyze Maven logs and rewrite the entire file.
        """,
        tools=[read_java_file, save_test_file]
    )

    runner = client.as_agent(
        name="Runner",
        instructions="You are a CI/CD system. Execute Maven via the tool. Report exact output.",
        tools=[run_maven_test]
    )

    print(f"=== 🏗️  AI Autonomous Testing Agent (Model: {MODEL_ID}) ===\n")

    print("📂 [System] Reading project files...")
    try:
        reqs_content = open(REQ_FILE, "r").read()
    except FileNotFoundError:
        reqs_content = "Requirements: Test OrderProcessor. If VIP priority shipping. If stock missing error."
        print("⚠️ Requirements file not found, using in-memory requirements.")

    code_content = read_java_file(SERVICE_CLASS)
    print(f"   - Requirements: {len(reqs_content)} chars")
    print(f"   - Java Code: {len(code_content)} chars")

    print("\n🧠 [QA Analyst] Generating Test Plan...")
    strategy_prompt = f"""
    BUSINESS REQUIREMENTS:
    {reqs_content}
    
    CURRENT SOURCE CODE:
    {code_content}
    
    Define the complete test strategy.
    """
    strategy_res = await analyst.run(strategy_prompt)
    strategy = strategy_res.text
    print(f"\n--- STRATEGIC PLAN ---\n{strategy[:300]}...\n(omitted)\n------------------------")

    print("\n👨‍💻 [Java Dev] Writing initial JUnit Code...")
    code_prompt = f"""
    Generate the class 'src/test/java/com/ecommerce/order/service/OrderProcessorTest.java'.
    
    Strictly follow this strategy:
    {strategy}
    
    Use the source code as reference for method names:
    {code_content}
    """
    await java_dev.run(code_prompt)

    print(f"\n🚀 [System] Starting validation loop (Max {MAX_RETRIES} attempts)...")

    for attempt in range(1, MAX_RETRIES + 1):
        print(f"\n🔄 ---------------- ATTEMPT {attempt} ----------------")
        
        print("⚙️  [Runner] Executing 'mvn test'...")
        run_res = await runner.run("Execute tests for class OrderProcessorTest")
        logs = run_res.text
        
        if "BUILD SUCCESS" in logs:
            print("\n✅✅✅ TESTS PASSED! Code is solid.")
            print("🎉 Workflow completed successfully.")
            return 
        
        else:
            print(f"\n❌ Tests FAILED.")
            error_snippet = logs if len(logs) < 500 else logs[-500:]
            print(f"   Error Log: \n{error_snippet}")

            if attempt < MAX_RETRIES:
                print(f"\n🛠️  [Java Dev] Applying code fix (Attempt {attempt})...")
                
                fix_prompt = f"""
                The test you wrote failed compilation or execution.
                
                Here are the Maven error logs:
                {logs}
                
                Analyze the error (e.g., missing imports, wrong mocks, failed assertions).
                Rewrite the ENTIRE OrderProcessorTest class correctly.
                Save the file overwriting the old one.
                """
                
                await java_dev.run(fix_prompt)
            else:
                print("\n💀 [System] Max retries reached. Human intervention needed.")

if __name__ == "__main__":
    asyncio.run(main())