#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p build/classes
find src -name '*.java' -print > build/sources.txt
javac --release 21 -encoding UTF-8 -d build/classes @build/sources.txt
jar --create --file build/Directory.jar --main-class es.um.redes.nanoFiles.application.Directory -C build/classes .
jar --create --file build/Nanofiles.jar --main-class es.um.redes.nanoFiles.application.NanoFiles -C build/classes .
printf 'Built build/Directory.jar and build/Nanofiles.jar\n'
