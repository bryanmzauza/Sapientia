#!/usr/bin/env bash
# Sapientia Bedrock smoke test (T-210 / 1.0.0).
#
# Drives a local Paper + Floodgate + Geyser stack from a clean slate so a
# release engineer can validate the cross-platform path in ~5 minutes. The
# script is intentionally dumb: it only sets up the runtime; humans drive a
# Bedrock client through the checklist below afterwards.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WORK="${WORK:-$ROOT/build/smoke-bedrock}"
PAPER_VERSION="${PAPER_VERSION:-1.20.4}"

echo "[smoke] Workspace: $WORK"
mkdir -p "$WORK/plugins"
cd "$WORK"

if [ ! -f paper.jar ]; then
    echo "[smoke] Paper $PAPER_VERSION not present — please drop paper-$PAPER_VERSION.jar (or symlink to paper.jar) into $WORK before re-running."
    exit 1
fi
if [ ! -f plugins/floodgate.jar ]; then
    echo "[smoke] floodgate.jar missing under $WORK/plugins/. Download Floodgate-Spigot from GeyserMC and place it as floodgate.jar."
    exit 1
fi
if [ ! -f plugins/Geyser-Spigot.jar ]; then
    echo "[smoke] Geyser-Spigot.jar missing. Download it and drop it under plugins/."
    exit 1
fi

echo "[smoke] Building Sapientia plugin jar..."
( cd "$ROOT" && ./gradlew --quiet :sapientia-core:buildPluginJar )
SAP_JAR=$(ls -1t "$ROOT"/sapientia-core/build/libs/sapientia-core-*.jar | grep -v sources | head -n1)
cp -f "$SAP_JAR" "plugins/Sapientia.jar"
echo "[smoke] Sapientia jar copied: $(basename "$SAP_JAR")"

# Auto-accept EULA in dev workspace.
echo "eula=true" > eula.txt

echo "[smoke] Starting Paper. Connect with a Bedrock client to localhost:19132 and run through docs/bedrock-smoke-checklist.md."
exec java -Xms2G -Xmx2G -jar paper.jar nogui
