# Mi Tacógrafo — Planificación Kanban

> Las tarjetas están **en orden de ejecución**: se coge siempre la primera de «Por hacer» cuyas dependencias estén hechas.
> Cada tarjeta = 1 rama `feature/MT-xx-…` = 1 PR = 1 commit en `develop` (squash). Referencias técnicas en [ESPECIFICACION.md](ESPECIFICACION.md).
> Desde MT-09 cada tarjeta es una **funcionalidad completa** (dominio + datos + pantalla + tests), no una capa suelta. Las tarjetas absorbidas conservan su número entre paréntesis para no perder la trazabilidad.
> Se puede pasar tal cual a **GitHub Projects** (1 tarjeta = 1 issue, etiqueta = épica, milestone = hito).

## Tablero

| 📥 Backlog | 📋 Por hacer | 🔨 En curso | 👀 Revisión | ✅ Hecho |
|---|---|---|---|---|
| E4 – E7 | MT-27 | MT-24 | — | MT-01 … MT-18, MT-20, MT-22, MT-32, MT-41 … MT-45 · MT-33B sin reglas · **v0.2.0 publicada** |

**Límite WIP:** 1 tarjeta en curso y 1 en revisión.
**Definición de Hecho (DoD):** compila · tests en verde · ktlint y Android Lint sin errores · textos en `strings.xml` (ES y UK) · probado en el móvil o el emulador · PR fusionado en `develop`.

## Hitos

| Hito | Contenido | Estimación acumulada | Resultado |
|---|---|---|---|
| **v0.1 — Adiós libreta** | E0 + E1 | ~16–24 h | El conductor ya registra jornadas, km y avisos en su móvil. |
| **v0.2 — Viajes y descansos** | E2 + E3 | ~26–38 h | Viajes, descansos registrados y clasificados. |
| **v0.3 — Reglamento** | E4 | ~42–58 h | Motor 561/2006 (nacional e internacional) con «No verificado» y tests. |
| **v1.0 — Completa** | E5 + E6 + E7 | ~56–80 h | Estadísticas, CSV y PDF, ucraniano revisado, pulido tras el uso real. |

Leyenda de tamaño: **S** ≤ 1 h · **M** 1–3 h · **L** 3–5 h · **XL** > 5 h.

**Tamaño máximo de una tarjeta:** lo que se pueda revisar con calma en un PR (unos 15–20 archivos). Si una crece más, se divide.

---

## E0 · Preparación (Sprint 0)

### MT-01 · Entorno y decisiones previas · S
- [x] Instalar Android Studio (estable) y crear un emulador.
- [ ] Activar las opciones de desarrollador y la depuración USB en el móvil del conductor, y anotar su versión de Android (→ D-5).
- [ ] Crear 3 emuladores: **API 26** (mínima), **API 33** (como el móvil del conductor) y la última estable (espec. §3.1).
  - Hechos API 36 y **API 33** (el APK de la v0.1.0 probado ahí). Falta el de API 26.
- [x] Decidir el `applicationId` (D-6): `com.cristinachdr.mitacografo`. (D-7 está resuelto: transporte internacional y nacional.)

### MT-02 · Repositorio y Gitflow · S
- [x] Repo público en GitHub con `main` y `develop`.
- [ ] Rama por defecto `develop` (ahora es `main`) y protección de ramas: PR + CI obligatoria.
- [x] `.gitignore` de Android, licencia (MIT) y README básico.
- [x] Añadir `docs/ESPECIFICACION.md` y `docs/KANBAN.md`.
- [ ] Crear las issues y el Project (opcional: el tablero vive en este archivo).
- **Criterio:** se puede abrir un PR contra `develop`.

### MT-03 · Esqueleto multimódulo · M · deps: MT-02
- [x] Proyecto «Empty Compose Activity», Kotlin DSL y `libs.versions.toml`.
- [x] Módulos `:app`, `:core:domain` (plugin `kotlin("jvm")`) y `:core:data` (Android library).
- [x] Dependencias: app → domain y data; data → domain.
- [x] `minSdk = 26` y `targetSdk`/`compileSdk` en la última versión estable.
- [x] `enableEdgeToEdge()` desde el principio (obligatorio en Android 15+).
- **Criterio:** `./gradlew build` en verde y la app muestra «Hola» en el móvil del conductor **y** en el emulador más reciente.

### MT-04 · Hilt + tema + navegación base · M · deps: MT-03
- [x] Hilt (`@HiltAndroidApp`, `@AndroidEntryPoint`).
- [x] Tema Material 3 claro y oscuro, tipografía grande y componente `BigButton`.
- [x] NavHost con barra inferior (Inicio · Historial · Viajes · Resumen) y pantallas vacías.
- **Criterio:** se navega entre 4 pantallas vacías.

### MT-05 · Calidad y CI · M · deps: MT-03
- [x] ktlint (estilo oficial + reglas de Compose) configurado; `.editorconfig`. ~~detekt~~ descartado (incompatible con JDK 25; ver espec. §3).
- [x] JUnit 5 en `:core:domain` con un test de ejemplo (hecho en MT-03).
- [x] `.github/workflows/ci.yml`: `ktlintCheck`, tests, `lintDebug` y `assembleDebug`.
- **Criterio:** el PR muestra el check verde.

### MT-06 · Firma y release · M · deps: MT-05
- [x] Generar el keystore y **guardarlo en 2 sitios seguros fuera del repo**.
- [x] `release.yml` con tag `v*` → APK firmado en la Release (PR #5). La firma se lee de variables de entorno o de `keystore.properties` (ignorado por git).
  - Probado en local con una clave de prueba: el APK release (R8) arranca, guarda jornadas y hace y restaura la copia JSON.
- [x] Secrets en GitHub: `MT_KEYSTORE_BASE64`, `MT_KEYSTORE_PASSWORD`, `MT_KEY_ALIAS` y `MT_KEY_PASSWORD`.
- [x] Tag `v0.1.0` en `main` → el workflow publica la Release con el APK firmado (5 nov 2026).
  - Antes hubo que fusionar `main` en `develop` con `-s ours` (PR #15): la PR #6 se había fusionado directamente en `main`.
  - El APK es el de la Release, firmado con el keystore de verdad, y está probado en API 33: arranca, pide notificaciones y el aviso de 12 h 30 salta a su hora con la app en segundo plano.
- **Criterio:** el APK firmado se instala desde la Release.

---

## E1 · MVP: jornada y avisos → **v0.1**

### MT-07 · Modelo de dominio Jornada + puertos · M · deps: MT-03
- [x] `Jornada`, `Clock`, `JornadaRepository`, `SettingsRepository` y `AlarmScheduler` (interfaces).
  - Como `Clock` se usa `java.time.Clock` (JDK puro, `Clock.fixed` en los tests): no hace falta una interfaz propia.
- [x] Tests de `kmRecorridos` y de duración con cambio de hora y zonas distintas (T18, T19).
- [x] `calcularAvisos`: horas de los avisos a partir del inicio, sin los desactivados ni los ya pasados.

### MT-08 · Room: base de datos y Jornada · M · deps: MT-07
- [x] `AppDatabase` v1, `JornadaEntity`, `JornadaDao` (Flow), conversores `Instant`/`ZoneId` y mappers.
  - La entidad solo usa tipos simples (epoch ms, nombre de zona, minutos) y la conversión está en `JornadaMapper.kt`, sin `TypeConverter`.
  - `viaje_id` todavía sin clave ajena: se añade con la tabla `viaje` (MT-18).
- [x] `exportSchema = true` (carpeta `core/data/schemas/` en git).
- [x] Test del DAO in-memory (Robolectric, sin emulador) y `RoomJornadaRepository` conectado con Hilt.

### MT-09 · Flujo completo de la jornada · XL · deps: MT-08 · (antes MT-09, MT-10, MT-11 y MT-12)
**Casos de uso**
- [x] `IniciarJornada`, `FinalizarJornada`, `EditarJornada`, `ObtenerJornadaActual` y `ObtenerUltimoKm`.
  - `ObtenerUltimoKm` es `ProponerDatosInicio`: propone km, país y ciudad donde terminó la jornada anterior.
- [x] Validaciones: una sola jornada abierta, `fin > inicio`, `kmFin ≥ kmInicio`, conducción ≤ duración. Tests con un repositorio falso en memoria.
  - Además: hora no futura (5 min de margen) y no empezar antes del fin de la jornada anterior. `validarJornada` es pura y la UI la usa en vivo.
  - `FakeJornadaRepository` en los *test fixtures* de `:core:domain`, compartido con los tests de `:app` (sin MockK).

**Iniciar jornada**
- [x] Hora (por defecto, ahora; selector de fecha y hora), km (por defecto, el último), país, ciudad y observaciones.
- [x] ViewModel con `UiState` y validación en vivo.

**Inicio (jornada actual)**
- [x] Sin jornada: botón «Iniciar jornada».
- [x] Con jornada: tiempo transcurrido (se refresca cada minuto), km de inicio, próximo aviso y botón «Finalizar».
  - El próximo aviso usa los avisos por defecto hasta que existan los Ajustes (MT-13).

**Finalizar jornada**
- [x] Hora fin, km final, conducción del tacógrafo (entrada hh:mm cómoda) y km recorridos en vivo.
- **Criterio:** iniciar una jornada en **≤ 2 toques** con los valores por defecto, y finalizarla.

### MT-13 · Ajustes y avisos · XL · deps: MT-09 · (antes MT-13, MT-14 y MT-15)
**Ajustes**
- [x] `SettingsRepository` con DataStore: avisos activos y sus offsets, **país de la empresa y de residencia** (necesarios para las reglas internacionales), nombre para el PDF y zona de referencia.
  - La zona de referencia se guarda, pero aún no tiene campo en la pantalla (por defecto, la del móvil). Se añadirá con el motor de reglas (MT-24).
- [x] Pantalla Ajustes con interruptores de avisos y botón «Probar aviso» (llega en 1 minuto).

**Avisos**
- [x] ~~Tabla `alarma_programada`~~ **descartada**: los avisos se recalculan siempre desde la jornada abierta y los ajustes (`GestorAvisos` + `calcularAvisos`). Menos código, sin migración y sin datos que se puedan desincronizar.
- [x] `AndroidAlarmScheduler` (exacta con respaldo `setWindow`), `AlarmReceiver` y canal de notificación.
- [x] Al iniciar, programar. Al finalizar, **cancelar todo**. Al editar el inicio, reprogramar. Al cambiar los avisos en Ajustes, reprogramar.
- [x] Pedir `POST_NOTIFICATIONS` (solo en 13+) y guiar al usuario para conceder `SCHEDULE_EXACT_ALARM` (12+, denegado por defecto en 14+).
- [x] Aviso de optimización de batería (Xiaomi, Samsung, Huawei…).
- [x] `BootReceiver` reprograma las alarmas futuras de la jornada abierta tras reiniciar, actualizar la app o conceder las alarmas exactas.
- [x] El reloj sigue la zona horaria actual del móvil (antes se quedaba con la del arranque de la app).
- **Criterio:** probado con offsets de 1–2 min en modo debug, con el móvil bloqueado y tras reiniciar, **en la API mínima y en la última**.

### MT-16 · Historial básico · M · deps: MT-09
- [x] Lista de jornadas agrupadas por semana (lunes a domingo) con fecha, km y conducción.
  - Total de km y conducción por semana; si a alguna jornada le falta la conducción, lo dice («falta apuntar alguna»).
  - La semana se cuenta por el día en que **empieza** la jornada, en la zona de referencia de Ajustes o, si no hay, en la del inicio.
- [x] Detalle y edición, y borrado con confirmación.
  - Se puede corregir todo (horas, km, países, conducción, observaciones) con la misma validación en vivo. Una jornada en curso solo deja cambiar el inicio.
  - Nueva regla al editar: la jornada no puede **solaparse** con otra (`SOLAPA_CON_OTRA_JORNADA`).
  - Borrar quita también los avisos de esa jornada si estaba en curso.
- [x] Fechas y números en el idioma **de la app** (antes salían en el del móvil: «Wed · Nov 4» con la app en español).

### MT-17 · Copia de seguridad y restauración JSON · L · deps: MT-16
- [x] `BackupPort` + implementación con kotlinx.serialization (`version`, más `app: "mi-tacografo"` para reconocer el archivo).
  - JSON legible: fechas ISO-8601 y zonas por nombre. Clases DTO aparte del dominio. Campos desconocidos se ignoran.
  - Errores claros: «no es una copia», «es de una versión más nueva», «está dañada».
- [x] Exportar (CreateDocument) / importar (OpenDocument → resumen → confirmar → transacción → reprogramar alarmas).
  - En Ajustes → «Copia de seguridad». Nombre propuesto: `mi-tacografo-AAAA-MM-DD.json`.
  - El resumen dice cuántas jornadas trae y cuántas se borrarán. Un archivo de más de 20 MB no se lee.
- [x] Auto Backup (`dataExtractionRules` y `fullBackupContent`): base de datos y DataStore, en la nube y al pasar de móvil.
- [x] Test: exportar → importar = datos idénticos (JSON real y casos de uso), una copia v1 escrita a mano se sigue leyendo, y si la transacción de Room falla no se pierde nada.

### MT-41 · Selector de país con autocompletado · M · deps: MT-09 · *(propuesta de Cristina)*
- [x] Desplegable con búsqueda en lugar de escribir el código: se puede buscar «Alem…» o «DE» y se guarda «DE».
  - También busca por una palabra del medio («Unido»), en inglés («Germany») y con los códigos de carretera «UK» y «EL».
  - «Hecho» en el teclado elige el primer resultado. Si se sale sin elegir, se queda el país que había. Opción «Sin país» para borrarlo.
- [x] Lista de países ISO de `java.util.Locale`, con el nombre en el idioma **de la app** (español o ucraniano; si el móvil está en inglés, la app y los países salen en español) sin mantener ninguna lista a mano. Los últimos usados (`ObtenerPaisesRecientes`) y los europeos, arriba.
- [x] Usarlo en Iniciar jornada, Finalizar jornada y Ajustes (país de la empresa y de residencia).
- [x] Test: buscar por nombre, por código y sin tildes («Espana» → ES), y nombres en ucraniano.

### MT-42 · Avisos configurables · M · deps: MT-13 · *(propuesta de Cristina)*
- [x] Cambiar la hora de cada aviso (p. ej. 12 h 30 → 12 h 00) y añadir o quitar avisos propios, además de los 4 de por defecto.
  - Se toca la hora para abrir un diálogo con horas y minutos; desde ahí también se borra. La lista se mantiene ordenada.
  - Como mucho 8 avisos (un hueco de alarma por aviso). Si se borran todos, no vuelven solos los de por defecto.
- [x] Botón «Restaurar los avisos por defecto» (solo sale si se ha cambiado algo).
- [x] Explicar en la pantalla de dónde salen los 4 de por defecto (24 h − 11 h de descanso = 13 h; 24 h − 9 h = 15 h).
- [x] Validaciones en el dominio (`errorAviso`): entre 1 h y 24 h, sin repetir, máximo 8. Con una jornada en curso, los avisos se reprograman al momento.

### MT-43 · Países habituales en Ajustes · M · deps: MT-41 · *(propuesta de Cristina)*
- [x] En Ajustes, elegir **continentes** (Europa, Asia, África, América, Oceanía) y **países habituales** (donde suele cargar y descargar). Por defecto, ninguno: salen todos igual.
- [x] Los elegidos salen arriba en el selector, después de los recientes. Los demás **no se esconden**: la búsqueda los sigue encontrando debajo, en «Otros países». Escribir el código exacto («US») lo pone arriba igualmente.
- [x] ~~Continentes con `android.icu.util.Region`~~: no está en la API pública de Android. Tabla de la ONU (M49) en `Continentes.kt`, con un test que comprueba que todos los países ISO tienen continente (menos la Antártida). Turquía y Chipre cuentan como Europa.
- [x] Guardar en DataStore (`paises_habituales`, `continentes`) y test de la búsqueda con y sin filtro.
- [x] El test de DataStore usa `OkioStorage`: el almacenamiento de Android falla en Windows al sobrescribir el archivo (en el móvil y en el CI funciona igual).

> 🏁 **Release v0.1.0** ✅ (5 nov 2026) → probada en un emulador **API 33** (primer arranque, permisos, un aviso). Instalar en el móvil del conductor y **usar en paralelo con la libreta 1–2 semanas** mientras se desarrolla E2.

### MT-44 · Colores del logo en toda la app · M · *(propuesta de Cristina)*
- [x] Colores exactos del icono (media de los píxeles del PNG): azul del fondo `#234886`, naranja de la onda `#D27204` y claro de la esfera `#E6F0FD`.
- [x] **Barra superior en el azul del logo** en los dos temas, con la barra de estado detrás e iconos blancos: el logo de la esquina se funde con ella.
- [x] **Naranja = avisos:** «Próximo aviso» va en una etiqueta naranja con campana, en dos líneas (hora y tiempo de jornada). Como texto sobre blanco, el naranja se oscurece a `#9A5200`.
- [x] Tarjeta de jornada en curso blanca con borde azul (opción B de 4 bocetos). Azules claros y barra de pestañas elegidos por Cristina con un probador de colores:
  - Claro: contenedores `#BCD2F5`, barra de pestañas `#D7E6FE`, pestaña activa `#A6C5F7` con texto `#1C3F7D`.
  - Oscuro: borde, horas y botón `#95B1DF` (texto `#052048`), pestaña activa en el azul del logo con el icono `#E6F0FD` y la palabra en `#E5F1FF`.
  - Los colores de la barra de pestañas van aparte (`coloresPestanas()`), para no cambiar nada más.
- [x] Contraste comprobado (WCAG AA, 4,5:1): todos los textos pasan, la mayoría de 7:1. Los colores de estado (✅ ⚠️ ❔) no cambian.
- [x] Probado en el emulador API 33 en claro y oscuro: Inicio, Historial y Ajustes.

### MT-45 · Sonido propio de los avisos · M · deps: MT-13 · *(propuesta de Cristina)*
- [x] Que al oír el aviso se sepa que es de Mi Tacógrafo y no un mensaje cualquiera.
- [x] Sonido propio en `res/raw/aviso_jornada.wav` (1,35 s, tres notas que suben). Es una **síntesis propia**, sin grabaciones de nadie ni licencias: se regenera con `herramientas/generar_sonido_aviso.py`. Vibración larga-corta-larga.
- [x] Canal nuevo `avisos_jornada_v2` con ese sonido y esa vibración, y se borra el antiguo. Probado actualizando desde la versión de `develop`: el canal viejo queda borrado y el nuevo, creado.
  - `res/raw/keep.xml`: el sonido se usa por su nombre, así que se protege para que R8 no lo quite en la release.
  - Respeta el modo silencio y «No molestar» (es una notificación, no una alarma).
- [x] En Ajustes: «Sonido actual: …», «Escuchar el sonido» (con el volumen de notificaciones) y «Cambiar sonido o vibración», que abre la pantalla de Android del canal. «Probar aviso» ya existía (llega en 1 minuto).
  - El sonido se lee del canal cada vez que se vuelve a la pantalla: si el conductor elige otro tono en Android, la app lo muestra y reproduce ese (fallo que vio Cristina al probarlo).
- [ ] Probar en el móvil del conductor con el móvil en vibración y con «No molestar».

### MT-18 · Viajes · L · (antes MT-18 y MT-19)
- [x] `Viaje` (nombre, inicio y fin con zona, observaciones), puerto `ViajeRepository`, entidad, DAO y adaptador de Room.
- [x] **Primera migración de verdad, 1 → 2** (`AutoMigration`): tabla `viaje` y clave ajena `jornada.viaje_id` con `ON DELETE SET NULL` (borrar un viaje no borra sus jornadas). Room rehace la tabla `jornada` copiando todas las filas.
  - `MigracionesTest` (Robolectric + `MigrationTestHelper`): crea una base de datos de la v0.1 con jornadas y un descanso, la migra, la valida contra `2.json` y comprueba que no se pierde nada.
  - Probado también en el emulador: con 9 jornadas y 17 descansos en la versión anterior, se instala la nueva encima y está todo.
- [x] km del viaje = suma de sus jornadas, más conducción y países por los que pasa (tests).
- [x] Pestaña **Viajes**: «Iniciar viaje» (con un nombre propuesto, «Viaje del 9 nov 2026»), el viaje en curso con sus km y «Finalizar viaje» (pide confirmación), y los anteriores. Detalle con sus jornadas (se abren para corregirlas), y corregir nombre, fechas u observaciones, o borrarlo.
- [x] Al iniciar una jornada con un viaje en curso, se asigna sola. Si se inicia un viaje con una jornada ya empezada, también entra. En Inicio sale «Viaje: …».
- [x] Copia de seguridad con los viajes (`viajes`; las copias antiguas se siguen leyendo). Restaurar comprueba que ninguna jornada apunte a un viaje que no está en la copia.
- [ ] Km por viaje en Resumen y PDF de un viaje (MT-33B).

> 🏁 **Release v0.2.0** ✅ (9 nov 2026): viajes, descansos, resumen con PDF, sonido de los avisos y colores del logo. Probada actualizando en un emulador API 33 desde la v0.1.0 de la Release (las dos firmadas con la misma clave), con jornadas, descansos y una jornada en curso: la base de datos pasa de la versión 1 a la 2 sin perder nada y los avisos siguen programados.

---

## E3 · Descansos → **v0.2**

### MT-20 · Descansos: datos y clasificador (TDD) · L · (antes MT-20 y MT-21)
- [x] `Descanso` (pausa o descanso, país; `fin` `null` = en curso), puerto `DescansoRepository`, DAO, mapper y adaptador de Room.
  - **Sin migración:** la tabla `descanso` ya estaba en la versión 1 de la base de datos; añadir el DAO no cambia el esquema (`1.json` igual).
- [x] Validación (`validarDescanso`): fin posterior al inicio, nada en el futuro, sin solapes entre descansos y solo uno en curso. Con una jornada sí puede solaparse (espec. §5.2).
- [x] Clasificador (`clasificarDescansos`): pausa, en curso, no cuenta, 1.ª parte de fraccionado, fraccionado 3 + 9 completo, diario reducido y normal, semanal reducido (con su deuda hasta 45 h) y normal.
  - Fraccionado: la 2.ª parte (≥ 9 h) tiene que terminar dentro de las 24 h desde el último diario; las pausas de en medio no cuentan. ⚠️ Revisar con la guía oficial antes de E4.
- [x] Tests T7–T10 y T12, más límites (9 h, 24 h, 45 h), casos de uso, Room (Robolectric) y ViewModels.
- [x] Casos de uso: guardar, borrar, iniciar y terminar pausa, lista clasificada y propuesta de descanso al iniciar.
- [x] **Copia de seguridad** con los descansos (campo `descansos`): las copias antiguas se siguen leyendo (lista vacía) y las versiones anteriores de la app lo ignoran, así que no sube `VERSION_FORMATO`.

### MT-22 · Pantalla Descansos · M · deps: MT-20 · (antes MT-22 y MT-23)
- [x] **Pausas y descansos** (icono de cama arriba, junto a Ajustes): lista con chip de clase, alta con «Añadir» y edición o borrado al tocar uno. Uno nuevo se propone desde el fin de la última jornada hasta ahora, en su país de fin.
- [x] **Inicio:** botón «Iniciar pausa» / «Terminar pausa» con la jornada en curso y «En pausa desde las 19:17 · 12 min» en la tarjeta. Al finalizar la jornada, la pausa abierta termina a la misma hora (o se borra si empezó después).
- [x] **«¿Descansaste desde vie 19:18?»** al iniciar jornada, si no hay descanso apuntado desde la anterior. Interruptor apagado por defecto: solo se crea si el conductor lo activa.
- [x] Textos en español y ucraniano. Probado en el emulador API 33, en claro y oscuro.
- [ ] Pendiente de decidir con el conductor: avisos durante la pausa (p. ej. a los 45 min) y si el ucraniano de los textos nuevos se entiende bien.

---

## E4 · Motor del Reglamento 561/2006 → **v0.3**

> **TDD estricto:** primero los tests de la tabla §13 de la especificación y luego la regla. Antes de cada tarjeta, **releer el artículo** en EUR-Lex.

### MT-24 · Núcleo del motor, conducción y pausas · XL · (antes MT-24, MT-25 y MT-26)
- [x] Art. 4, 6 y 7 releídos en EUR-Lex (consolidado a 22/05/2024) antes de escribir las reglas: coinciden con la espec. §7.2 y §7.3.
- [x] `Resultado` (Cumple, Excedido, NoVerificado), `MotivoNoVerificado`, `Intervalo [min, max]` con la comparación de §8.2, y `MotorReglas` con reglas independientes (`ReglaJornada`, `ReglaSemana`). Paquete `domain/rules`.
- [x] `repartirPorSemanas`: semana de lunes 00:00 en la zona de referencia (o la de cada jornada). Una jornada que cruza el domingo reparte su conducción con un intervalo (T20); sin conducción apuntada, entre 0 y lo que duró; en curso, entre 0 y lo que lleva.
  - La asignación de semanales (D-2) se hará con las reglas de descanso semanal (MT-28).
- [x] R-COND-1/2/3/4. Tests T1–T6 y T21. La semana anterior a la primera apuntada cuenta como [0, 56 h] para las 90 h (`DATOS_INSUFICIENTES_PERIODO`).
- [x] R-PAUSA: ≤ 4 h 30 → Cumple; si no → No verificado (`SIN_TRAMOS_CONDUCCION`). Test T22.
- [x] Caso de uso `ObtenerInformeReglas` (se recalcula al corregir una jornada, T24). Aún sin pantalla: los chips llegan en MT-30.

### MT-27 · Reglas de descanso diario · L · deps: MT-24, MT-20
- [ ] R-DD-1 (24 h, con huecos → No verificado) y R-DD-3 (máx. 3 reducidos). Tests T11 y T23.

### MT-28 · Reglas de descanso semanal (nacional e internacional) · XL · deps: MT-27
- [ ] R-DS-1 (patrón bisemanal) y R-DS-2 (6 × 24 h).
- [ ] R-DS-3: 2 reducidos consecutivos permitidos solo si **ambos** son fuera del país de la empresa y del de residencia, más la regla de 4 semanas. En otro caso se aplica el patrón nacional (espec. §7.7).
- [ ] Motivos `SIN_PAIS_DESCANSO` y `PAISES_NO_CONFIGURADOS`, y aviso en Inicio si faltan los países.
- [ ] Tests T15, T16, T17, T17b, T17c y T17d.

### MT-29 · Compensaciones · XL · deps: MT-28
- [ ] Deudas (45 h − duración), fecha límite (fin de la 3ª semana siguiente), detección de compensación unida a un descanso ≥ 9 h, FIFO y estados.
- [ ] R-DS-5: tras 2 reducidos consecutivos, el siguiente semanal va precedido de la compensación.
- [ ] Tests T13, T14, T17e y T24. Resolver D-3.

### MT-30 · Semanas reales y estados en la UI · L · deps: MT-29 · (antes MT-30 y MT-31)
- [ ] 2–3 semanas de la libreta, **anonimizadas**, como fixtures JSON. Comparar los resultados con lo que el conductor sabe que pasó.
- [ ] Chips ✅⚠️❔ en Historial e Inicio, y el resumen semanal en Inicio (conducción X/56 h, ampliaciones X/2, reducidos X/3, deuda pendiente).
- [ ] Pantalla de detalle del informe con la explicación de cada regla (textos traducidos).

> 🏁 **Release v0.3.0**

---

## E5 · Resumen, estadísticas y exportación (CSV + PDF) → **v1.0**

### MT-32 · Pantalla Resumen y estadísticas · L · *(adelantada a petición de Cristina, sin el motor de reglas)*
- [x] Pestaña **Resumen**: «Semana» o «2 semanas» (la elegida y la anterior, las de las 90 h), con flechas para ir hacia atrás. No deja pasar de la semana actual.
- [x] Totales: km, jornadas, tiempo de jornada y conducción del tacógrafo (avisa si a alguna jornada le falta). En la bisemana, cada semana por separado.
- [x] Km por día con barras (también los días sin trabajar) y descansos por clase, con lo que falta por compensar de los semanales reducidos.
- [x] Dominio puro `resumirPeriodo` (TDD, test T25): solo cuenta lo que empieza en el periodo, en la zona de referencia si hay.
- [ ] Fecha límite de las deudas y km por viaje: con el motor de reglas (E4) y con Viajes (E2).

### MT-33 · Exportar CSV · M
- [ ] `jornadas.csv`, `viajes.csv` y `descansos.csv` (UTF-8 BOM, `;`, coma decimal). Compartir con `Intent.ACTION_SEND`.
- [ ] Test del formato (que se abra bien en Excel).

### MT-33B · Exportar PDF · XL · deps: MT-30 · (antes MT-33B y MT-33C)
> **Primera parte hecha sin reglas** (con MT-32): «Compartir PDF» y «Guardar PDF» en Resumen. Cabecera, resumen, tablas de jornadas y descansos con su clase, cabecera repetida al cambiar de página, «Página X de Y» y aviso legal. Probado en API 33 en español y ucraniano (el cirílico sale bien).
> - `Informe` (dominio) + `GeneradorPdf` (puerto en `app`, porque los textos traducidos están en `app`) con `PdfDocument`. Se comparte con `FileProvider` (`cache/informes/`) y se guarda con `CreateDocument`.
> - Falta: informe de reglas por semana, rango mes / viaje / fechas libres, y el test instrumentado T26.
- [ ] Puerto `ReportRenderer` e `InformeModel` puro: cabecera, resumen, tablas de jornadas y descansos, informe de reglas por semana y deudas. Test T25.
- [ ] Selección de rango: semana, bisemana, mes, viaje o fechas libres.
- [ ] `PdfReportRenderer` con `PdfDocument`: A4, tablas con paginación y cabecera repetida, «Página X de Y», pie con el aviso legal, y comprobar que el ucraniano (cirílico) se ve bien.
- [ ] Guardar (`CreateDocument`) o compartir (`FileProvider` + `ACTION_SEND`).
- [ ] Botón «Exportar → CSV / PDF» en Resumen y en el detalle de un viaje.
- [ ] Test instrumentado T26 (`PdfRenderer` cuenta las páginas).
- **Criterio:** el conductor genera el PDF de un viaje y lo envía por WhatsApp en ≤ 4 toques.

---

## E6 · Idiomas → **v1.0**

### MT-34 · Idiomas · M · (antes MT-34 y MT-35)
- [ ] `locales_config.xml`, `setApplicationLocales` y opción en Ajustes.
- [ ] `values-uk/strings.xml` completo. **Revisión por el conductor**, que es hablante nativo.
- [ ] Lint `MissingTranslation` como error en la CI.

---

## E7 · Pulido y uso real → **v1.0**

### MT-36 · Primer arranque y accesibilidad · M · (antes MT-36 y MT-37)
- [ ] Pantalla de bienvenida con el aviso legal y la petición de permisos (notificaciones y alarmas exactas).
- [ ] Revisar tamaños táctiles, contraste y TalkBack. Probar con guantes y de noche con el tema oscuro.

### MT-38 · Tests de UI y compatibilidad · L · (antes MT-38 y MT-38B)
- [ ] Compose UI test: iniciar → finalizar jornada → aparece en el historial.
- [ ] GitHub Actions con `android-emulator-runner`: tests instrumentados en la **API mínima** y en la **última**.
- [ ] Prueba manual en horizontal y en tablet o plegable (emulador).
- [ ] Instalar el APK de la Release en un móvil «limpio» (como el de un amigo) y revisar el primer arranque y los permisos.

### MT-39 · Feedback del conductor · M (continuo)
- [ ] Tras 1–2 semanas de uso, recoger las quejas y mejoras como issues nuevas y priorizarlas.

### MT-40 · README y portfolio · M
- [ ] README en inglés con capturas, diagrama hexagonal, decisiones (intervalos y «No verificado», sin INTERNET, Instant + ZoneId) y cómo compilar.
- [ ] Añadir el proyecto al CV y a LinkedIn.

> 🏁 **Release v1.0.0** — la libreta se jubila.

---

## Backlog futuro (sin planificar)

- Lectura de ficheros `.DDD` de la tarjeta del conductor.
- Widget de la jornada en curso.
- Art. 9 (ferri/tren) y art. 12 (excepciones) completos.
- Multi-manning (30 h).
- Gráficas mensuales.
- Recordatorio de copia de seguridad cada 30 días.
- Google Play (prueba cerrada).
