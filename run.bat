@echo off
setlocal EnableExtensions
cd /d "%~dp0"

rem Preferir sempre codigo atualizado do GitHub quando existir JDK.
set "JAVA=java"
where java >nul 2>&1
if not errorlevel 1 goto :java_ok
if not defined JAVA_HOME goto :no_java
if not exist "%JAVA_HOME%\bin\java.exe" goto :no_java
set "JAVA=%JAVA_HOME%\bin\java.exe"
:java_ok
if not exist "src\swingforge\SwingForge.java" goto :run_jar
set "JAVAC=javac"
where javac >nul 2>&1
if not errorlevel 1 goto :compile
if not defined JAVA_HOME goto :run_jar
if not exist "%JAVA_HOME%\bin\javac.exe" goto :run_jar
set "JAVAC=%JAVA_HOME%\bin\javac.exe"
:compile
echo [SwingForge] Compilando o codigo atual com JDK local (sem Ant)...
if not exist "build\classes" mkdir "build\classes"
"%JAVAC%" -encoding UTF-8 -source 8 -target 8 -sourcepath "src" -d "build\classes" "src\swingforge\SwingForge.java"
if errorlevel 1 goto :build_failed
echo [SwingForge] Abrindo aplicativo offline...
"%JAVA%" -cp "build\classes;resources" swingforge.SwingForge
set "CODE=%errorlevel%"
goto :finish

:run_jar
if not exist "dist\SwingForge.jar" goto :no_jdk
echo [SwingForge] Abrindo JAR offline...
"%JAVA%" -jar "dist\SwingForge.jar"
set "CODE=%errorlevel%"
goto :finish

:no_java
echo [ERRO] Java nao encontrado. Instale um JRE/JDK offline ou configure JAVA_HOME.
set "CODE=1"
goto :finish
:no_jdk
echo [ERRO] Sem JDK para compilar as fontes e sem dist\SwingForge.jar.
echo Use um JDK local ou a versao distribuida com o JAR compilado.
set "CODE=1"
goto :finish
:build_failed
echo [ERRO] Falha na compilacao. Verifique o JDK instalado.
set "CODE=1"
:finish
if not "%CODE%"=="0" pause
exit /b %CODE%