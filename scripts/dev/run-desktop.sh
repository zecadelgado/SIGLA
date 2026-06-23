#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
cd "${REPO_ROOT}"

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-supabase}"

required_env=(
  SIGLA_DATASOURCE_PASSWORD
)

for name in "${required_env[@]}"; do
  if [[ -z "${!name:-}" ]]; then
    echo "Variavel de ambiente obrigatoria ausente: ${name}" >&2
    exit 1
  fi
done

./mvnw -pl sigla-interface -am -DskipTests install
./mvnw -pl sigla-interface spring-boot:run
