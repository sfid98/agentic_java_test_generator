import asyncio
import os
from agent_framework.ollama import OllamaChatClient

# Importiamo i tool reali
from tools import read_java_file, save_test_file, run_maven_test

# --- CONFIGURAZIONE ---
MODEL_ID = "gpt-oss:120b-cloud"  # Assicurati di aver fatto `ollama run llama3.2`
# Cambia questi path in base al tuo file Java reale nel progetto
TARGET_JAVA_CLASS = "src/main/java/com/example/demo/PriceCalculator.java"
TARGET_TEST_PATH = "src/test/java/com/example/demo/PriceCalculatorTest.java"
TEST_CLASS_NAME = "PriceCalculatorTest"

async def main():
    print(f"=== 🧬 AI Auto-Test Engineer (Powered by {MODEL_ID}) ===")
    
    client = OllamaChatClient(model_id=MODEL_ID)

    # --- DEFINIZIONE DEGLI AGENTI ---
    
    # 1. Developer: Legge codice, scrive test
    developer = client.as_agent(
        name="JavaDev",
        instructions="""
        Sei un Senior Java Developer esperto in JUnit 5 e Mockito.
        Il tuo obiettivo è scrivere unit test che COMPILANO ed ESEGUONO correttamente.
        
        Workflow:
        1. Leggi il file Java sorgente.
        2. Genera il codice e la **classe** di test completo (inclusi package e import).
        3. Salva il file nel percorso corretto utilizzando il tool save_test_file.
        
        IMPORTANTE: Non usare markdown nei parametri delle funzioni tool, passa solo stringhe pure.
        """,
        tools=[read_java_file, save_test_file],
    )

    # 2. QA: Esegue i test
    qa_bot = client.as_agent(
        name="QABot",
        instructions="""
        Sei un QA Automation Engineer.
        Usa 'run_maven_test' per eseguire i test richiesti.
        Analizza l'output e restituisci un riassunto (SUCCESS o FAILURE con dettagli).
        """,
        tools=[run_maven_test],
    )

    # --- ESECUZIONE DEL FLUSSO ---

    # FASE 1: Creazione iniziale
    print(f"\n1️⃣  Analisi di {TARGET_JAVA_CLASS}...")
    
    # Nota: Forziamo la lettura nel prompt per garantire che l'LLM abbia il contesto
    source_code_prompt = f"Leggi il file '{TARGET_JAVA_CLASS}'. Poi scrivi una classe di test '{TEST_CLASS_NAME}' e salvala in '{TARGET_TEST_PATH}' utilizzando il tool save_test_file che copra i casi base. Usa JUnit 5."
    
    await developer.run(source_code_prompt)
    
    # FASE 2: Ciclo di Test & Fix
    max_retries = 3
    for i in range(max_retries):
        print(f"\n🔄 Tentativo di esecuzione {i+1}/{max_retries}...")
        
        # QA Esegue il test
        result = await qa_bot.run(f"Esegui il test per la classe {TEST_CLASS_NAME}")
        print(f"\n📋 Report QA:\n{result}")

        # Controllo Esito (Logica euristica basata sulla risposta testuale)
        if "SUCCESS" in str(result):
            print("\n✅✅✅ SUCCESSO! Il test è valido e passa.")
            break
        
        # Se fallisce, chiediamo al Developer di fixare
        print("\n🛠️  Rilevato errore. Chiedo al Developer di correggere...")
        
        fix_prompt = f"""
        Il test ha fallito l'esecuzione. Ecco i log di Maven:
        {result}
        
        Analizza l'errore (es. errori di compilazione, assert falliti, package errati).
        Leggi di nuovo il file sorgente se necessario.
        Riscrivi INTERAMENTE la classe di test corretta e salvala in '{TARGET_TEST_PATH}'.
        """
        
        await developer.run(fix_prompt)
    
    else:
        print("\n❌ Max retries raggiunti. Intervento umano richiesto.")

if __name__ == "__main__":
    asyncio.run(main())