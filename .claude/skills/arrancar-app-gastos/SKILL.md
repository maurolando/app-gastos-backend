---
name: arrancar-app-gastos
description: Use when running, starting or smoke-testing app-gastos locally — launching the Spring Boot backend and the Angular frontend, needing a working local login, or verifying a UI change in a real browser instead of only with tests.
---

# Arrancar app-gastos en local

Levanta el backend (Spring Boot, `:8080`) contra **H2 en memoria** y el frontend
(Angular, `:4200`). Para entrar se crea una cuenta desde el login.

**Regla que no se negocia:** el `application.properties` por defecto apunta al
**Supabase de producción con `ddl-auto=update`**. Arrancar sin forzar H2 modifica
el esquema de la base real. Después de arrancar, confirmá siempre en el log:

```
HikariPool-1 - Added connection conn0: url=jdbc:h2:mem:appgastos
```

Si ahí dice `jdbc:postgresql://...`, **matá el proceso ya** y revisá las variables.

## Requisitos del entorno (esta máquina, Fedora)

| Qué | Cómo |
|---|---|
| Java | `java-25-openjdk-devel` ya instalado. Compila `--release 17` sin problema. |
| Maven | `mvnw` viene **sin bit de ejecución**: usar `sh ./mvnw`, nunca `./mvnw`. |
| Node | Vive en nvm y **no** está en el PATH: `export PATH="$HOME/.nvm/versions/node/v22.23.2/bin:$PATH"` en la misma línea de cada comando. |
| Repos | `app-gastos-backend` y `app-gastos-frontend` son hermanos en `~/dev`. |

## 1. Backend con H2

Desde la raíz de `app-gastos-backend`. `useTestClasspath=true` es lo que pone el
driver de H2 en el classpath; las variables son las que desvían la conexión.

```bash
DB_URL='jdbc:h2:mem:appgastos;DB_CLOSE_DELAY=-1;MODE=PostgreSQL' \
DB_USERNAME=sa DB_PASSWORD= \
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.h2.Driver \
SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT=org.hibernate.dialect.H2Dialect \
SPRING_JPA_HIBERNATE_DDL_AUTO=create-drop \
JWT_SECRET=clave-de-firma-solo-para-local-no-usar-en-produccion \
SHOW_SQL=false \
nohup sh ./mvnw spring-boot:run -Dspring-boot.run.useTestClasspath=true > /tmp/backend.log 2>&1 &
```

Tarda ~10 s. Listo cuando aparece `Started BackendApplication`. Esperalo sin
`sleep` en primer plano:

```bash
until grep -qE "Started BackendApplication|APPLICATION FAILED TO START|BUILD FAILURE" /tmp/backend.log; do sleep 2; done
```

`JWT_SECRET` es obligatorio (HS384 exige ≥ 32 caracteres) o el contexto no levanta.

## 2. Crear una cuenta

La base arranca vacía. Cada cuenta es un **hogar** aislado (sus personas, gastos,
categorías, etc.), y se crea sin estar logueado con la mutation `registrar`, así
que no hace falta sembrar nada:

```bash
curl -s localhost:8080/graphql -H 'Content-Type: application/json' \
  -d '{"query":"mutation{ registrar(nombre:\"Mauro\", clave:\"secreta1\"){ token persona{ id nombre } } }"}'
```

La clave pide al menos 6 caracteres y el nombre es único entre todos los hogares
(el login es solo por nombre). Desde la UI es el botón "¿No tenés cuenta? Creá una".

La base es **en memoria**: todo lo que cargues se pierde al reiniciar.

## 3. Frontend

```bash
cd ../app-gastos-frontend && export PATH="$HOME/.nvm/versions/node/v22.23.2/bin:$PATH" && \
  nohup npx ng serve --configuration development > /tmp/frontend.log 2>&1 &
```

La config de desarrollo ya apunta a `localhost:8080`. Listo cuando el log dice
`Compiled successfully`. Abrir **http://localhost:4200** y crear una cuenta, o entrar con la creada por curl.

## 4. Recorrido en navegador

No hay Chrome ni Chromium en el sistema (instalarlos pide sudo) y el usuario usa
Firefox. Se usa el Chromium de Playwright, ya descargado en
`~/.cache/app-gastos-pw` (~676 MB, fuera del repo). Si esa carpeta no existe:

```bash
mkdir -p ~/.cache/app-gastos-pw && cd ~/.cache/app-gastos-pw && \
  export PATH="$HOME/.nvm/versions/node/v22.23.2/bin:$PATH" && \
  npm init -y && npm i playwright@1 && \
  PLAYWRIGHT_BROWSERS_PATH=$PWD/browsers npx playwright install chromium
```

`recorrer.js` (en esta carpeta) crea una cuenta desde el login, sale y crea otra
(que tiene que arrancar vacía), saca `shot-registro.png`, `shot-dashboard.png` y
`shot-dashboard-otra.png`, e imprime los errores de consola. Correlo desde ahí para
que encuentre el `node_modules`:

```bash
cp .claude/skills/arrancar-app-gastos/recorrer.js ~/.cache/app-gastos-pw/ && \
cd ~/.cache/app-gastos-pw && export PATH="$HOME/.nvm/versions/node/v22.23.2/bin:$PATH" && \
  PLAYWRIGHT_BROWSERS_PATH=$PWD/browsers node recorrer.js
```

**Mirá las capturas con Read.** Un dashboard en blanco es un fallo, aunque el
script haya terminado con éxito y no haya errores de consola.

Este recorrido ya encontró un bug que ni los tests ni el build detectaban: Apollo
congela los objetos de su caché y mutarlos rompía el guardado. Vale la pena.

## 5. Bajar todo

```bash
pkill -f "[s]pring-boot:run"; pkill -f BackendApplication; pkill -f "[n]g serve"
```

Los corchetes no son adorno: `pkill -f "ng serve"` **se mata a sí mismo**, porque
la línea de comando del propio shell que lo ejecuta contiene el patrón. Por lo
mismo, el `pkill` va en su propio comando: si en la misma llamada aparece después
`spring-boot:run` escrito tal cual, `[s]pring-boot:run` casa con el shell y lo mata. El shell
muere antes de terminar y el `ng serve` de verdad queda vivo. Con `[n]g` el
patrón sigue casando con el proceso real pero ya no con el comando que lo busca.

Si aun así el puerto sigue ocupado: `fuser -k 4200/tcp`.

## Errores comunes

| Síntoma | Causa |
|---|---|
| `url=jdbc:postgresql://...` en el log | Faltó alguna variable: **estás tocando producción**. Matalo. |
| `No autenticado` en cualquier mutation | Falta el token: creá una cuenta con `registrar` o hacé `login`. |
| `release version 17 not supported` | Está el JDK headless sin `javac`. Hace falta `java-25-openjdk-devel`. |
| `Permission denied: ./mvnw` | Usar `sh ./mvnw`. |
| `npm: command not found` | Faltó el export del PATH de nvm en **esa misma** línea. |
| `goto` de Playwright con timeout de 30 s | `waitUntil: 'networkidle'` **nunca** resuelve contra `ng serve`: el websocket de recarga en vivo no deja la red quieta. Usar `domcontentloaded` + `waitForSelector`. |
| El puerto 8080 sigue ocupado tras `pkill` | Maven lanza un JVM hijo; matá también por `pkill -f spring-boot:run`. |
