@echo off
rem Outdoor Index - stop the standalone server
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$p = Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { $_.CommandLine -like '*com.outdoorindex.Launcher*' }; if ($p) { $p | ForEach-Object { Stop-Process -Id $_.ProcessId -Force; Write-Host ('stopped PID ' + $_.ProcessId) } } else { Write-Host 'server is not running' }"
pause
