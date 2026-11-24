@echo off
rem Outdoor Index - start the standalone server. Close this window to stop it.
setlocal
cd /d "%~dp0app"
if not exist ".env" (
  echo.
  echo  [!] Not configured yet. Run SETUP.bat first.
  echo.
  pause
  exit /b 1
)
set "JAVA_EXE=java"
where java >nul 2>nul
if errorlevel 1 (
  if defined JAVA_HOME (
    set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
  ) else (
    echo  [X] Java not found. Install Java 17+ and set PATH or JAVA_HOME.
    pause
    exit /b 1
  )
)
set "PORT=8090"
for /f "usebackq tokens=1,* delims==" %%a in (".env") do if /i "%%a"=="OUTDOOR_PORT" set "PORT=%%b"
title Outdoor Index Server - http://localhost:%PORT%/
echo.
echo  Starting server... browser opens in about 10 seconds: http://localhost:%PORT%/
echo  To stop: close this window (or run STOP.bat)
echo.
start "" /min cmd /c "timeout /t 10 /nobreak >nul & start http://localhost:%PORT%/search"
"%JAVA_EXE%" -Dfile.encoding=UTF-8 -Doutdoor.home="%CD%" -cp "target\classes;target\lib\*" com.outdoorindex.Launcher
echo.
echo  Server stopped.
pause
