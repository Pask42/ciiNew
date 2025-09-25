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

ORIGINAL_PATH="${PATH}"

check_java_version() {
    local java_bin="$1"
    if [ ! -x "${java_bin}" ]; then
        return 1
    fi
    local version
    version=$("${java_bin}" -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)
    if [ -z "${version}" ]; then
        return 1
    fi
    if [ "${version}" -lt 21 ]; then
        return 1
    fi
    return 0
}

resolve_java_home() {
    local java_path="$1"
    local resolved
    resolved=$(readlink -f "${java_path}" 2>/dev/null || echo "${java_path}")
    cd "$(dirname "${resolved}")/.." && pwd
}

select_java21() {
    local current_java
    current_java=$(command -v java || true)
    if [ -n "${current_java}" ]; then
        local current_home
        current_home=$(resolve_java_home "${current_java}")
        if check_java_version "${current_home}/bin/java"; then
            export JAVA_HOME="${current_home}"
            export PATH="${JAVA_HOME}/bin:${ORIGINAL_PATH}"
            return 0
        fi
    fi

    local candidates=()
    if [ -n "${JAVA_HOME:-}" ]; then
        candidates+=("${JAVA_HOME}")
    fi
    if [ -n "${JAVA21_HOME:-}" ]; then
        candidates+=("${JAVA21_HOME}")
    fi
    if [ -d "/usr/lib/jvm/java-21-openjdk-amd64" ]; then
        candidates+=("/usr/lib/jvm/java-21-openjdk-amd64")
    fi
    if command -v update-alternatives >/dev/null 2>&1; then
        while IFS= read -r alt; do
            candidates+=("$(cd "$(dirname "${alt}")/.." && pwd)")
        done < <(update-alternatives --list java 2>/dev/null | grep 'java-21' || true)
    fi

    for candidate in "${candidates[@]}"; do
        if [ -x "${candidate}/bin/java" ] && check_java_version "${candidate}/bin/java"; then
            export JAVA_HOME="${candidate}"
            export PATH="${JAVA_HOME}/bin:${ORIGINAL_PATH}"
            return 0
        fi
    done

    return 1
}

if ! select_java21; then
    echo "❌ Java 21 ou supérieur est requis (aucun JDK 21 détecté)."
    exit 1
fi

echo "ℹ️ Utilisation de JAVA_HOME=${JAVA_HOME}"

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
