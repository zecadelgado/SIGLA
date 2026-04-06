./mvnw.cmd -pl sigla-interface -am clean package
jpackage --type exe --name SIGLA --input sigla-interface/target --main-jar sigla-interface-0.1.0-SNAPSHOT.jar --dest deploy/jpackage/installers

