#!/bin/bash
# Script de build complet

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
cd "$SCRIPT_DIR/.." || exit 1

echo "🏗️ Building CII Messaging System..."

# Vérifier Java 21
java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)
if [ "$java_version" -lt "21" ]; then
    echo "❌ Java 21 or higher is required"
    exit 1
fi

# Clean et build
mvn clean install -DskipTests

# Créer le répertoire de distribution
mkdir -p dist
cp cii-cli/target/cii-cli-*-jar-with-dependencies.jar dist/cii-cli.jar

echo "✅ Build complete! CLI available at dist/cii-cli.jar"
