@echo off
setlocal EnableExtensions
cd /d "%~dp0"

rem SwingForge abre sem Apache Ant quando o JAR esta incluido.
set "JAVA=java"
where java >nul 2>&1
if not errorlevel 1 goto :java_ok
if not defined JAVA_HOME goto :no_java
if not exist "%JAVA_HOME%\bin\java.exe" goto :no_java
set "JAVA=%JAVA_HOME%\bin\java.exe"
:java_ok
if exist "dist\SwingForge.jar" goto :run_jar

rem Checkout do GitHub: dist/ nao e versionado, entao compila com JDK.
if not exist "src\swingforge\SwingForge.java" goto :no_sources
set "JAVAC=javac"
where javac >nul 2>&1
if not errorlevel 1 goto :compile
if not defined JAVA_HOME goto :no_jdk
if not exist "%JAVA_HOME%\bin\javac.exe" goto :no_jdk
set "JAVAC=%JAVA_HOME%\bin\javac.exe"
:compile
echo [SwingForge] JAR ausente. Compilando com JDK local (Ant nao necessario)...
if not exist "build\classes" mkdir "build\classes"
"%JAVAC%" -encoding UTF-8 -source 8 -target 8 -sourcepath "src" -d "build\classes" "src\swingforge\SwingForge.java"
if errorlevel 1 goto :build_failed
rem Recursos/templates ficam disponiveis no classpath, sem precisar copiar arquivos.
echo [SwingForge] Iniciando...
"%JAVA%" -cp "build\classes;resources" swingforge.SwingForge
set "CODE=%errorlevel%"
goto :finish

:run_jar
echo [SwingForge] Iniciando aplicativo offline...
"%JAVA%" -jar "dist\SwingForge.jar"
set "CODE=%errorlevel%"
goto :finish

:no_java
echo [ERRO] Java nao encontrado. Instale um JRE/JDK offline ou configure JAVA_HOME.
set "CODE=1"
goto :finish
:no_jdk
echo [ERRO] Este checkout do GitHub nao contem o JAR compilado.
echo Instale um JDK 8+ offline (com javac), ou use o ZIP distribuido com dist\SwingForge.jar.
set "CODE=1"
goto :finish
:no_sources
echo [ERRO] Nao encontrei dist\SwingForge.jar nem src\swingforge\SwingForge.java.
echo Extraia o ZIP completo do SwingForge antes de executar.
set "CODE=1"
goto :finish
:build_failed
echo [ERRO] Falha na compilacao. Verifique a versao do JDK instalada.
set "CODE=1"
:finish
if not "%CODE%"=="0" pause
exit /b %CODE%
