@echo off
setlocal enabledelayedexpansion

echo ===================================================
echo Starting Patrol Tracker with Supabase PostgreSQL
echo ===================================================

if exist ".env" (
    echo [INFO] Loading environment variables from .env file...
    for /f "usebackq tokens=1* delims==" %%A in (".env") do (
        set "line=%%A"
        if not "!line:~0,1!"=="#" if not "%%A"=="" (
            set "%%A=%%B"
        )
    )
) else (
    echo [WARNING] No .env file found. Copying .env.example to .env...
    copy .env.example .env
)

if "%SUPABASE_DB_URL%"=="" (
    echo [ERROR] SUPABASE_DB_URL is not configured!
    echo Please edit the .env file with your Supabase Database URL and password.
    pause
    exit /b 1
)

echo [INFO] Connecting to Database: %SUPABASE_DB_URL%
echo [INFO] Using Database User: %SUPABASE_DB_USER%
echo [INFO] Launching Spring Boot...

call .\mvnw.cmd spring-boot:run
