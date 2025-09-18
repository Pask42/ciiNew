#!/usr/bin/env bash
set -euo pipefail
# Script de build complet

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
PROJECT_ROOT=$(cd "${SCRIPT_DIR}/.." && pwd)

if [ ! -f "${PROJECT_ROOT}/pom.xml" ]; then
    echo "❌ Impossible de trouver pom.xml dans ${PROJECT_ROOT}. Veuillez exécuter le script depuis le dépôt du projet."
    exit 1
fi

cd "${PROJECT_ROOT}"

echo "🏗️ Construction du système de messagerie CII..."

# Vérifier Java 21
java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)
if [ "${java_version}" -lt "21" ]; then
    echo "❌ Java 21 ou supérieur est requis"
    exit 1
fi

# Clean et build
if ! mvn -f "${PROJECT_ROOT}/pom.xml" clean install -DskipTests; then
    echo "❌ Échec du build Maven. Abandon."
    exit 1
fi

# Créer le répertoire de distribution
mkdir -p dist
CLI_JAR=$(find cii-cli/target -maxdepth 1 -name 'cii-cli-*-jar-with-dependencies.jar' -print -quit || true)

if [ -z "${CLI_JAR}" ] || [ ! -f "${CLI_JAR}" ]; then
    echo "❌ CLI jar introuvable dans cii-cli/target. Le build Maven est-il complet ?"
    exit 1
fi

cp "${CLI_JAR}" dist/cii-cli.jar

echo "✅ Build terminé ! CLI disponible dans dist/cii-cli.jar"
