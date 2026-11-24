@echo off
rem Outdoor Index - status check. Saves check-result.txt in this folder.
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\check.ps1"
