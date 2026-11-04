#!/bin/sh
set -eu
cd "$(dirname "$0")"
if [ -f dist/SwingForge.jar ]; then exec java -jar dist/SwingForge.jar; fi
if [ -x tools/ant/bin/ant ]; then exec tools/ant/bin/ant run; fi
exec ant run
