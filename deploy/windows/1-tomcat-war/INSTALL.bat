@echo off
rem Outdoor Index - deploy outdoor.war to the installed Tomcat 11
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0..\tools\setup.ps1" -Mode war
if errorlevel 1 pause
