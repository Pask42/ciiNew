#!/bin/bash
# Valider tous les fichiers XML d'un répertoire

if [ $# -eq 0 ]; then
    echo "Usage: $0 <directory>"
    exit 1
fi

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
CLI="$SCRIPT_DIR/run-cli.sh"

echo "🔍 Validating all XML files in $1..."

find "$1" -name "*.xml" -type f | while read -r file; do
    echo -n "Validating $(basename "$file")... "
    if $CLI validate "$file" > /dev/null 2>&1; then
        echo "✅ Valid"
    else
        echo "❌ Invalid"
    fi
done
