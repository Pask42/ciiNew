#!/bin/bash
# Valider tous les fichiers XML d'un répertoire

if [ $# -eq 0 ]; then
    echo "Utilisation : $0 <répertoire>"
    exit 1
fi

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
CLI="$SCRIPT_DIR/run-cli.sh"

echo "🔍 Validation de tous les fichiers XML dans $1..."

find "$1" -name "*.xml" -type f | while read -r file; do
    echo -n "Validation de $(basename "$file")... "
    if $CLI validate "$file" > /dev/null 2>&1; then
        echo "✅ Valide"
    else
        echo "❌ Invalide"
    fi
done
