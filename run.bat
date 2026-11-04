@echo off
cd /d "%~dp0"
where java >nul 2>&1
if errorlevel 1 (echo Java nao encontrado. Instale JDK 8 ou superior offline. & pause & exit /b 1)
if exist "dist\SwingForge.jar" (java -jar "dist\SwingForge.jar" & goto :end)
if exist "tools\ant\bin\ant.bat" (call "tools\ant\bin\ant.bat" run & goto :end)
where ant >nul 2>&1
if errorlevel 1 (echo Ant nao encontrado. Abra pelo NetBeans ou coloque o pacote offline em tools\ant. & pause & exit /b 1)
call ant run
:end
if errorlevel 1 pause
