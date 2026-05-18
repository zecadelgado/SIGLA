#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
cd "${REPO_ROOT}"

if [ -z "${SIGLA_DATASOURCE_URL:-}" ] && [ -z "${SPRING_DATASOURCE_URL:-}" ]; then
  if ! command -v docker >/dev/null 2>&1; then
    echo "Docker nao encontrado. Instale/abra o Docker ou configure SIGLA_DATASOURCE_URL, SIGLA_DATASOURCE_USERNAME e SIGLA_DATASOURCE_PASSWORD." >&2
    exit 1
  fi

  docker compose up -d postgres

  echo "Aguardando PostgreSQL ficar pronto..."
  for attempt in $(seq 1 30); do
    if docker compose exec -T postgres pg_isready -U sigla -d sigla >/dev/null 2>&1; then
      break
    fi

    if [ "${attempt}" -eq 30 ]; then
      echo "PostgreSQL nao ficou pronto a tempo." >&2
      exit 1
    fi

    sleep 2
  done
fi

./mvnw -pl sigla-interface -am -DskipTests install
./mvnw -pl sigla-interface spring-boot:run
