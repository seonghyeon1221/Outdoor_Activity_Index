@echo off
rem Outdoor Index - standalone setup (no Tomcat needed)
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0..\tools\setup.ps1" -Mode standalone
if errorlevel 1 pause
