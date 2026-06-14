#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
cd "${REPO_ROOT}"

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-supabase}"

./mvnw -pl sigla-interface -am -DskipTests install
./mvnw -pl sigla-interface spring-boot:run
