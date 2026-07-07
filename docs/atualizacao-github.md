# Atualizacao via GitHub Releases

O SIGLA passa a abrir pelo `sigla-launcher.jar`. O launcher copia/atualiza o `sigla.jar` em `%LOCALAPPDATA%\SIGLA\app` e executa essa copia local. Isso evita escrever em `Program Files` e permite atualizar sem permissao de administrador.

## Gerar o sigla.jar atualizado

Na raiz do projeto:

```powershell
.\mvnw.cmd clean package -pl sigla-interface,sigla-launcher -am -DskipTests
```

O jar do sistema sera gerado em:

```text
sigla-interface\target\sigla-interface-0.1.0-SNAPSHOT.jar
```

Para publicar no GitHub Releases, envie esse arquivo com o nome:

```text
sigla.jar
```

## Montar o versao.json

Crie um arquivo `versao.json` com build numerico crescente e a URL publica do `sigla.jar` na release:

```json
{
  "build": 1,
  "versao": "0.1.0",
  "mensagem": "Uma nova atualizacao do SIGLA esta disponivel.",
  "url": "https://github.com/Richarlison-Avila/sigla-update/releases/latest/download/sigla.jar"
}
```

Regras:

- `build` deve sempre aumentar a cada publicacao.
- `url` deve apontar para o asset `sigla.jar` da release.
- `versao` e `mensagem` aparecem na tela de confirmacao do launcher.

## Criar uma Release no sigla-update

No repositorio:

```text
https://github.com/Richarlison-Avila/sigla-update
```

Crie uma nova Release e anexe dois arquivos:

```text
sigla.jar
versao.json
```

O launcher consulta sempre:

```text
https://github.com/Richarlison-Avila/sigla-update/releases/latest/download/versao.json
```

## Frequencia de verificacao

O launcher verifica atualizacao no maximo uma vez por turno:

- madrugada: 00:00 ate 05:59
- manha: 06:00 ate 11:59
- tarde: 12:00 ate 17:59
- noite: 18:00 ate 23:59

O controle fica em:

```text
%LOCALAPPDATA%\SIGLA\app\ultima-verificacao.txt
```

O build local fica em:

```text
%LOCALAPPDATA%\SIGLA\app\versao-local.txt
```

## Testar o atualizador

Para forcar uma nova consulta, apague:

```powershell
Remove-Item "$env:LOCALAPPDATA\SIGLA\app\ultima-verificacao.txt" -Force -ErrorAction SilentlyContinue
```

Para simular que a versao local esta antiga:

```powershell
Set-Content "$env:LOCALAPPDATA\SIGLA\app\versao-local.txt" "0"
```

Para limpar a copia local inteira e testar a primeira inicializacao:

```powershell
Remove-Item "$env:LOCALAPPDATA\SIGLA" -Recurse -Force -ErrorAction SilentlyContinue
```

Se o GitHub estiver fora, sem internet, JSON invalido ou download falhar, o launcher abre a copia local existente.

## Gerar app-image e instalador

O script Windows ja copia `sigla-launcher.jar` e `sigla.jar` para o input do jpackage:

```powershell
.\scripts\package\jpackage-win.ps1
```

Comandos equivalentes manuais:

```powershell
.\mvnw.cmd clean package -pl sigla-interface,sigla-launcher -am -DskipTests

Remove-Item sigla-interface\target\jpackage-input -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force sigla-interface\target\jpackage-input
Copy-Item sigla-launcher\target\sigla-launcher-0.1.0-SNAPSHOT.jar sigla-interface\target\jpackage-input\sigla-launcher.jar
Copy-Item sigla-interface\target\sigla-interface-0.1.0-SNAPSHOT.jar sigla-interface\target\jpackage-input\sigla.jar

jpackage --type app-image --name SIGLA --input sigla-interface\target\jpackage-input --main-jar sigla-launcher.jar --java-options "--enable-native-access=ALL-UNNAMED" --dest deploy\jpackage\app-image
jpackage --type exe --name SIGLA --input sigla-interface\target\jpackage-input --main-jar sigla-launcher.jar --java-options "--enable-native-access=ALL-UNNAMED" --dest deploy\jpackage\installers
```
