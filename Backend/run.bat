@echo off
echo ============================================
echo E-Learning Backend API - Startup Script
echo ============================================
echo.

REM Check if .env exists
if not exist .env (
    echo [ERROR] .env file not found!
    echo Please copy .env.example to .env and configure it.
    echo Command: copy .env.example .env
    pause
    exit /b 1
)

REM Check if virtual environment exists
if not exist .venv (
    echo [INFO] Virtual environment not found. Creating...
    uv venv
    
    echo [INFO] Installing dependencies...
    call .venv\Scripts\activate.bat
    uv pip install -r requirements.txt
) else (
    echo [INFO] Virtual environment found.
)

REM Activate virtual environment and run
echo [INFO] Starting application...
echo.
call .venv\Scripts\activate.bat && python main.py
