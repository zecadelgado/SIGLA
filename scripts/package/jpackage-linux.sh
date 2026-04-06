#!/usr/bin/env bash
set -euo pipefail
./mvnw -pl sigla-interface -am clean package
jpackage --type deb --name SIGLA --input sigla-interface/target --main-jar sigla-interface-0.1.0-SNAPSHOT.jar --dest deploy/jpackage/installers

