#!/usr/bin/env bash
set -euo pipefail
./mvnw -pl sigla-infraestrutura -am flyway:migrate

