import os
import subprocess
import re

JAVA_PROJECT_ROOT = "./"

def read_file(file_path: str) -> str:
    """Read a file (requirements or code). Path relative to the script or the java project."""
    # Try direct absolute/relative path
    if os.path.exists(file_path):
        with open(file_path, "r", encoding="utf-8") as f: return f.read()
    
    inner_path = os.path.join(JAVA_PROJECT_ROOT, file_path)
    if os.path.exists(inner_path):
        with open(inner_path, "r", encoding="utf-8") as f: return f.read()
        
    return f"ERROR: File {file_path} not found."

def read_java_file(file_path: str) -> str:
    """Read a file (requirements or code). Path relative to the script or the java project."""
    # Try direct absolute/relative path
    if os.path.exists(file_path):
        with open(file_path, "r", encoding="utf-8") as f: return f.read()
    
    # Try inside the java folder
    inner_path = os.path.join(JAVA_PROJECT_ROOT, file_path)
    if os.path.exists(inner_path):
        with open(inner_path, "r", encoding="utf-8") as f: return f.read()
        
    return f"ERROR: File {file_path} not found."

def save_test_file(content: str) -> str:
    """Save the Java test file in the correct path based on the package."""
    clean_code = content.replace("```java", "").replace("```", "").strip()
    
    # Regex to find the class name and package
    class_match = re.search(r'class\s+(\w+)', clean_code)
    package_match = re.search(r'package\s+([\w\.]+);', clean_code)
    
    if not class_match: return "ERROR: No class name found."
    
    class_name = class_match.group(1)
    package_path = package_match.group(1).replace('.', '/') if package_match else ""
    
    full_path = os.path.join(JAVA_PROJECT_ROOT, "src/test/java", package_path, f"{class_name}.java")
    
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, "w", encoding="utf-8") as f:
        f.write(clean_code)
        
    return f"SUCCESS: Test saved to {full_path}"

def run_maven_test(test_class: str) -> str:
    """Execute 'mvn test' and return filtered logs."""
    mvn_cmd = "mvn.cmd" if os.name == 'nt' else "mvn"
    print(f"\n[SYSTEM] ⚙️  Executing test test: {test_class}...")
    
    try:
        result = subprocess.run(
            [mvn_cmd, "test", f"-Dtest={test_class}"],
            cwd=JAVA_PROJECT_ROOT,
            capture_output=True, text=True
        )
        
        output = result.stdout + result.stderr
        if result.returncode == 0:
            return "BUILD SUCCESS"
        else:
            error_log = "\n".join(output.splitlines()[-40:])
            return f"BUILD FAILURE. Details:\n{error_log}"
    except Exception as e:
        return f"SYSTEM ERROR: {str(e)}"