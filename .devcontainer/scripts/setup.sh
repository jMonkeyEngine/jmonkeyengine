#!/usr/bin/env bash
set -euo pipefail


# Optional: warm Gradle wrapper so first run is less painful
if [ -f ./gradlew ]; then
  ./gradlew --version || true
fi