#!/bin/bash
# Wrapper pour la CLI

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
JAR_PATH="$SCRIPT_DIR/../dist/cii-cli.jar"

if [ ! -f "$JAR_PATH" ]; then
    echo "❌ Jar de la CLI introuvable. Exécutez d'abord ./scripts/build.sh"
    exit 1
fi

java -jar "$JAR_PATH" "$@"
