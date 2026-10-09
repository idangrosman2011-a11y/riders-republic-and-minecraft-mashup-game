#!/bin/bash
# Builds RidersCraft-<version>.zip: portable Prism Launcher + a Fabric 1.21.1 instance holding the mod and Fabric API.
# Installs to {localappdata}/RidersCraft (see melty.draft.json).
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
VER=$(grep mod_version "$ROOT/mod/gradle.properties" | cut -d= -f2)
PRISM=10.0.2; FAPI=0.116.17+1.21.1
OUT=${1:-$ROOT/build-release}; W=$(mktemp -d)
(cd "$ROOT/mod" && ./gradlew build -q)
mkdir -p "$W/Prism" && curl -sSL -o "$W/prism.zip" "https://github.com/PrismLauncher/PrismLauncher/releases/download/$PRISM/PrismLauncher-Windows-MSVC-Portable-$PRISM.zip"
unzip -q "$W/prism.zip" -d "$W/Prism" && rm "$W/prism.zip"
I="$W/Prism/instances/RidersCraft"; mkdir -p "$I/.minecraft/mods"
cp "$ROOT/packaging/instance.cfg" "$ROOT/packaging/mmc-pack.json" "$I/"
cp "$ROOT/mod/build/libs/riderscraft-$VER.jar" "$I/.minecraft/mods/"
curl -sSL -o "$I/.minecraft/mods/fabric-api-$FAPI.jar" "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/${FAPI/+/%2B}/fabric-api-${FAPI/+/%2B}.jar"
cp "$ROOT/packaging/CREDITS.txt" "$W/"
mkdir -p "$OUT" && rm -f "$OUT/RidersCraft-$VER.zip" && (cd "$W" && zip -qr "$OUT/RidersCraft-$VER.zip" .)
echo "$OUT/RidersCraft-$VER.zip"
