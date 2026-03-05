#!/usr/bin/env bash
set -euo pipefail
./mvnw -pl sigla-infrastructure -am flyway:migrate
