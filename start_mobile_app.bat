@echo off
title Patrol Tracker - Mobile & PC Launcher
cls
echo ========================================================
echo         PATROL TRACKER - MOBILE & PC LAUNCHER
echo ========================================================
echo.
echo Starting Spring Boot Server and Cloudflare Mobile Tunnel...
echo.

:: Start Spring Boot Server in background window
start "Patrol Tracker Backend Server" cmd /k ".\mvnw.cmd spring-boot:run"

echo [1/2] Server starting... Waiting 12 seconds for startup...
timeout /t 12 /nobreak > nul

:: Start Cloudflare Tunnel in background window
start "Patrol Tracker Mobile Tunnel" cmd /k "npx --yes cloudflared tunnel --protocol http2 --url http://localhost:8080"

echo [2/2] Mobile Tunnel launched!
echo.
echo ========================================================
echo  ACCESS LINKS & LOGIN DETAILS:
echo ========================================================
echo  1. Computer / Laptop Link:  http://localhost:8080
echo  2. Mobile Link (4G/5G/WiFi): Look at the 'Mobile Tunnel' window
echo     for the https://...trycloudflare.com link.
echo.
echo  LOGIN CREDENTIALS:
echo  - SP / Admin:       sp-admin  /  sp123
echo  - SHO / Supervisor: usr-003   /  super123
echo  - Guard / Constable:usr-001   /  guard123
echo ========================================================
echo.
pause
