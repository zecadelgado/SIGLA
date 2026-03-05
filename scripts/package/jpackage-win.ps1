./mvnw.cmd -pl sigla-desktop -am clean package
jpackage --type exe --name SIGLA --input sigla-desktop/target --main-jar sigla-desktop-0.1.0-SNAPSHOT.jar --dest deploy/jpackage/installers
