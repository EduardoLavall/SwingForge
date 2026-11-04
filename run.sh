#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
if ! command -v java >/dev/null 2>&1; then
  echo '[ERRO] Java nao encontrado. Instale o Java offline.' >&2
  exit 1
fi
if [ -f dist/SwingForge.jar ]; then
  exec java -jar dist/SwingForge.jar
fi
if ! command -v javac >/dev/null 2>&1; then
  echo '[ERRO] JAR ausente. Instale um JDK (javac) ou utilize o ZIP com dist/SwingForge.jar.' >&2
  exit 1
fi
if [ ! -f src/swingforge/SwingForge.java ]; then
  echo '[ERRO] Fonte do SwingForge nao encontrada.' >&2
  exit 1
fi
mkdir -p build/classes
echo '[SwingForge] Compilando fonte com javac (sem Ant)...'
javac -encoding UTF-8 -source 8 -target 8 -sourcepath src -d build/classes src/swingforge/SwingForge.java
exec java -cp 'build/classes:resources' swingforge.SwingForge
