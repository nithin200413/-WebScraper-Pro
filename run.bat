@echo off
setlocal enabledelayedexpansion
title WebScraper Pro

:: ── Environment ──
set "JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot"
set "PATH=%JAVA_HOME%\bin;C:\maven\apache-maven-3.9.16\bin;%PATH%"
set "PORT=8080"
set "JAR=target\web-scraper-1.0.0.jar"

cd /d "%~dp0"

echo ============================================
echo   WebScraper Pro - launcher
echo ============================================

:: -- Check Java --
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found at %JAVA_HOME%
    pause
    exit /b 1
)

:: -- Free the port if busy --
echo [1/3] Checking if port %PORT% is in use...
set "BUSY="
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%PORT%" ^| findstr "LISTENING"') do (
    set "BUSY=1"
    echo       Port %PORT% busy on PID %%a - terminating...
    taskkill /PID %%a /F >nul 2>&1
)
if defined BUSY (
    timeout /t 2 /nobreak >nul
    echo       Port %PORT% freed.
) else (
    echo       Port %PORT% is free.
)

:: -- Ensure the JAR exists, build if missing --
echo [2/3] Checking build...
if not exist "%JAR%" (
    echo       JAR not found - building with Maven...
    call mvn package -q
    if errorlevel 1 (
        echo [ERROR] Build failed.
        pause
        exit /b 1
    )
)

:: -- Run --
echo [3/3] Starting server...
echo.
echo   Open: http://localhost:%PORT%
echo   Press Ctrl+C to stop.
echo.
java -jar "%JAR%"

pause
