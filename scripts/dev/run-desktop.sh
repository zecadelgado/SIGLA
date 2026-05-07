#!/usr/bin/env bash
set -euo pipefail
./mvnw -pl sigla-interface -am -DskipTests install
./mvnw -pl sigla-interface spring-boot:run
