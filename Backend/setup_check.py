#!/usr/bin/env python3
"""
E-Learning Platform Backend - Setup Verification Script

This script verifies that your environment is properly configured
before running the application.
"""

import sys
import os
import subprocess
from pathlib import Path


class Colors:
    """ANSI color codes for terminal output"""
    GREEN = '\033[92m'
    RED = '\033[91m'
    YELLOW = '\033[93m'
    BLUE = '\033[94m'
    END = '\033[0m'
    BOLD = '\033[1m'


def print_header(text):
    """Print a formatted header"""
    print(f"\n{Colors.BOLD}{Colors.BLUE}{'='*60}{Colors.END}")
    print(f"{Colors.BOLD}{Colors.BLUE}{text.center(60)}{Colors.END}")
    print(f"{Colors.BOLD}{Colors.BLUE}{'='*60}{Colors.END}\n")


def print_success(text):
    """Print success message"""
    print(f"{Colors.GREEN}✓{Colors.END} {text}")


def print_error(text):
    """Print error message"""
    print(f"{Colors.RED}✗{Colors.END} {text}")


def print_warning(text):
    """Print warning message"""
    print(f"{Colors.YELLOW}⚠{Colors.END} {text}")


def print_info(text):
    """Print info message"""
    print(f"{Colors.BLUE}ℹ{Colors.END} {text}")


def check_python_version():
    """Check Python version"""
    print_info("Checking Python version...")
    version = sys.version_info
    
    if version.major >= 3 and version.minor >= 11:
        print_success(f"Python {version.major}.{version.minor}.{version.micro}")
        return True
    else:
        print_error(f"Python {version.major}.{version.minor} found, but 3.11+ required")
        return False


def check_env_file():
    """Check if .env file exists"""
    print_info("Checking .env configuration...")
    
    if Path(".env").exists():
        print_success(".env file found")
        
        # Check for required variables
        required_vars = [
            "DATABASE_URL",
            "SECRET_KEY",
            "OLLAMA_BASE_URL",
            "OLLAMA_CHAT_MODEL",
            "OLLAMA_EMBEDDING_MODEL"
        ]
        
        with open(".env", "r") as f:
            env_content = f.read()
        
        missing_vars = []
        for var in required_vars:
            if var not in env_content or f"{var}=" not in env_content:
                missing_vars.append(var)
        
        if missing_vars:
            print_warning(f"Missing variables in .env: {', '.join(missing_vars)}")
            return False
        else:
            print_success("All required environment variables present")
            return True
    else:
        print_error(".env file not found")
        print_info("Run: copy .env.example .env (Windows) or cp .env.example .env (Unix)")
        return False


def check_postgresql():
    """Check PostgreSQL connection"""
    print_info("Checking PostgreSQL connection...")
    
    # First try to check if psql is available
    psql_available = False
    try:
        result = subprocess.run(
            ["psql", "--version"],
            capture_output=True,
            text=True,
            timeout=5
        )
        if result.returncode == 0:
            version = result.stdout.strip()
            print_success(f"PostgreSQL client found: {version}")
            psql_available = True
    except (subprocess.TimeoutExpired, FileNotFoundError):
        print_warning("psql command not in PATH (this is OK if PostgreSQL is installed)")
    
    # Try to actually connect to the database using Python
    try:
        import psycopg2
        from dotenv import load_dotenv
        load_dotenv()
        
        database_url = os.getenv("DATABASE_URL")
        if database_url:
            try:
                conn = psycopg2.connect(database_url)
                conn.close()
                print_success("Successfully connected to PostgreSQL database!")
                return True
            except psycopg2.OperationalError as e:
                print_error(f"Cannot connect to database: {str(e)}")
                print_info("Make sure PostgreSQL is running and credentials in .env are correct")
                print_info("Create database with: psql -U postgres -c \"CREATE DATABASE elearning_db;\"")
                return False
        else:
            print_warning("DATABASE_URL not found in .env")
            return False
    except ImportError:
        print_warning("psycopg2 not installed yet, skipping database connection test")
        return psql_available


def check_ollama():
    """Check Ollama installation and models"""
    print_info("Checking Ollama installation...")
    
    try:
        result = subprocess.run(
            ["ollama", "list"],
            capture_output=True,
            text=True,
            timeout=10
        )
        
        if result.returncode == 0:
            print_success("Ollama is installed")
            
            # Check for required models
            output = result.stdout.lower()
            
            has_llama = "llama3.2" in output
            has_embed = "nomic-embed-text" in output
            
            if has_llama:
                print_success("llama3.2 model found")
            else:
                print_error("llama3.2 model not found")
                print_info("Run: ollama pull llama3.2")
            
            if has_embed:
                print_success("nomic-embed-text model found")
            else:
                print_error("nomic-embed-text model not found")
                print_info("Run: ollama pull nomic-embed-text")
            
            return has_llama and has_embed
        else:
            print_error("Ollama not responding")
            print_info("Run: ollama serve")
            return False
    
    except (subprocess.TimeoutExpired, FileNotFoundError):
        print_error("Ollama not found or not in PATH")
        print_info("Please install Ollama from https://ollama.com/download")
        return False


def check_directories():
    """Check if required directories exist"""
    print_info("Checking directory structure...")
    
    required_dirs = [
        "app",
        "app/api",
        "app/models",
        "app/repositories",
        "app/schemas",
        "app/services",
        "app/utils"
    ]
    
    all_exist = True
    for directory in required_dirs:
        if Path(directory).is_dir():
            print_success(f"{directory}/ exists")
        else:
            print_error(f"{directory}/ not found")
            all_exist = False
    
    return all_exist


def check_dependencies():
    """Check if Python dependencies are installed"""
    print_info("Checking Python dependencies...")
    
    required_packages = [
        "fastapi",
        "uvicorn",
        "sqlalchemy",
        "pydantic",
        "llama_index",
        "chromadb",
        "pypdf"
    ]
    
    all_installed = True
    for package in required_packages:
        try:
            __import__(package.replace("-", "_"))
            print_success(f"{package} installed")
        except ImportError:
            print_error(f"{package} not installed")
            all_installed = False
    
    if not all_installed:
        print_info("Run: uv pip install -e . (or pip install -r requirements.txt)")
    
    return all_installed


def create_directories():
    """Create runtime directories if they don't exist"""
    print_info("Creating runtime directories...")
    
    dirs = ["uploads", "chroma_db"]
    
    for directory in dirs:
        Path(directory).mkdir(exist_ok=True)
        print_success(f"{directory}/ ready")
    
    return True


def main():
    """Main verification function"""
    print_header("E-Learning Platform - Environment Setup Verification")
    
    checks = {
        "Python Version": check_python_version(),
        "Environment Configuration": check_env_file(),
        "PostgreSQL": check_postgresql(),
        "Ollama & Models": check_ollama(),
        "Directory Structure": check_directories(),
        "Python Dependencies": check_dependencies(),
        "Runtime Directories": create_directories()
    }
    
    print_header("Verification Summary")
    
    passed = 0
    failed = 0
    
    for check_name, result in checks.items():
        if result:
            print_success(f"{check_name}: PASSED")
            passed += 1
        else:
            print_error(f"{check_name}: FAILED")
            failed += 1
    
    print(f"\n{Colors.BOLD}Results: {passed} passed, {failed} failed{Colors.END}\n")
    
    if failed == 0:
        print_success("✨ All checks passed! Your environment is ready.")
        print_info("\nTo start the application, run:")
        print(f"  {Colors.BOLD}python main.py{Colors.END}")
        print(f"\nAPI will be available at: {Colors.BOLD}http://localhost:8000{Colors.END}")
        print(f"API Documentation: {Colors.BOLD}http://localhost:8000/docs{Colors.END}\n")
        return 0
    else:
        print_error("❌ Some checks failed. Please fix the issues above.")
        print_info("\nRefer to QUICKSTART.md or README.md for setup instructions.\n")
        return 1


if __name__ == "__main__":
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        print(f"\n\n{Colors.YELLOW}Setup verification cancelled.{Colors.END}\n")
        sys.exit(130)
