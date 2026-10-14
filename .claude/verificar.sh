#!/bin/sh
# Verificación completa del repo. La corre el hook de cierre de mr-sherlock
# (~/.claude/hooks/mr-sherlock-cierre.py); sale con 0 solo si todo pasa.
cd "$(dirname "$0")/.." || exit 1
exec sh ./mvnw -q test
