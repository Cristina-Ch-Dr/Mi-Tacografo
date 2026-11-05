# Mi Tacógrafo — Planificación Kanban

> Las tarjetas están **en orden de ejecución**: se coge siempre la primera de «Por hacer» cuyas dependencias estén hechas.
> Cada tarjeta = 1 rama `feature/MT-xx-…` = 1 PR = 1 commit en `develop` (squash). Referencias técnicas en [ESPECIFICACION.md](ESPECIFICACION.md).
> Desde MT-09 cada tarjeta es una **funcionalidad completa** (dominio + datos + pantalla + tests), no una capa suelta. Las tarjetas absorbidas conservan su número entre paréntesis para no perder la trazabilidad.
> Se puede pasar tal cual a **GitHub Projects** (1 tarjeta = 1 issue, etiqueta = épica, milestone = hito).

## Tablero

| 📥 Backlog | 📋 Por hacer | 🔨 En curso | 👀 Revisión | ✅ Hecho |
|---|---|---|---|---|
| E2 – E7 | **E1** MT-17, MT-42 | MT-16 | — | MT-01 … MT-13, MT-41, MT-43 |

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
- [ ] Instalar Android Studio (estable) y crear un emulador.
- [ ] Activar las opciones de desarrollador y la depuración USB en el móvil del conductor, y anotar su versión de Android (→ D-5).
- [ ] Crear 3 emuladores: **API 26** (mínima), **API 33** (como el móvil del conductor) y la última estable (espec. §3.1).
- [ ] Decidir el `applicationId` (D-6). (D-7 está resuelto: transporte internacional y nacional.)

### MT-02 · Repositorio y Gitflow · S
- [ ] Repo público en GitHub con `main` y `develop` (por defecto `develop`). Protección de ramas: PR + CI obligatoria.
- [ ] `.gitignore` de Android, licencia (MIT) y README básico.
- [ ] Añadir `docs/ESPECIFICACION.md` y `docs/KANBAN.md`. Crear las issues y el Project.
- **Criterio:** se puede abrir un PR contra `develop`.

### MT-03 · Esqueleto multimódulo · M · deps: MT-02
- [ ] Proyecto «Empty Compose Activity», Kotlin DSL y `libs.versions.toml`.
- [ ] Módulos `:app`, `:core:domain` (plugin `kotlin("jvm")`) y `:core:data` (Android library).
- [ ] Dependencias: app → domain y data; data → domain.
- [ ] `minSdk = 26` y `targetSdk`/`compileSdk` en la última versión estable.
- [ ] `enableEdgeToEdge()` desde el principio (obligatorio en Android 15+).
- **Criterio:** `./gradlew build` en verde y la app muestra «Hola» en el móvil del conductor **y** en el emulador más reciente.

### MT-04 · Hilt + tema + navegación base · M · deps: MT-03
- [ ] Hilt (`@HiltAndroidApp`, `@AndroidEntryPoint`).
- [ ] Tema Material 3 claro y oscuro, tipografía grande y componente `BigButton`.
- [ ] NavHost con barra inferior (Inicio · Historial · Viajes · Resumen) y pantallas vacías.
- **Criterio:** se navega entre 4 pantallas vacías.

### MT-05 · Calidad y CI · M · deps: MT-03
- [x] ktlint (estilo oficial + reglas de Compose) configurado; `.editorconfig`. ~~detekt~~ descartado (incompatible con JDK 25; ver espec. §3).
- [x] JUnit 5 en `:core:domain` con un test de ejemplo (hecho en MT-03).
- [x] `.github/workflows/ci.yml`: `ktlintCheck`, tests, `lintDebug` y `assembleDebug`.
- **Criterio:** el PR muestra el check verde.

### MT-06 · Firma y release · M · deps: MT-05
- [ ] Generar el keystore y **guardarlo en 2 sitios seguros fuera del repo**.
- [ ] Secrets en GitHub (keystore en Base64 y contraseñas) y `release.yml` con tag `v*` → APK firmado en la Release.
- [ ] Tag `v0.0.1` → instalar el APK en el móvil del conductor.
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
- [ ] `BackupPort` + implementación con kotlinx.serialization (`schemaVersion`).
- [ ] Exportar (CreateDocument) / importar (OpenDocument → resumen → confirmar → transacción → reprogramar alarmas).
- [ ] Auto Backup (`dataExtractionRules`).
- [ ] Test: exportar → importar = datos idénticos.

### MT-41 · Selector de país con autocompletado · M · deps: MT-09 · *(propuesta de Cristina)*
- [x] Desplegable con búsqueda en lugar de escribir el código: se puede buscar «Alem…» o «DE» y se guarda «DE».
  - También busca por una palabra del medio («Unido»), en inglés («Germany») y con los códigos de carretera «UK» y «EL».
  - «Hecho» en el teclado elige el primer resultado. Si se sale sin elegir, se queda el país que había. Opción «Sin país» para borrarlo.
- [x] Lista de países ISO de `java.util.Locale`, con el nombre en el idioma **de la app** (español o ucraniano; si el móvil está en inglés, la app y los países salen en español) sin mantener ninguna lista a mano. Los últimos usados (`ObtenerPaisesRecientes`) y los europeos, arriba.
- [x] Usarlo en Iniciar jornada, Finalizar jornada y Ajustes (país de la empresa y de residencia).
- [x] Test: buscar por nombre, por código y sin tildes («Espana» → ES), y nombres en ucraniano.

### MT-42 · Avisos configurables · M · deps: MT-13 · *(propuesta de Cristina)*
- [ ] Cambiar la hora de cada aviso (p. ej. 12 h 30 → 12 h 00) y añadir o quitar avisos propios, además de los 4 de por defecto.
- [ ] Botón «Restaurar los avisos por defecto».
- [ ] Explicar en la pantalla de dónde salen los 4 de por defecto (24 h − 11 h de descanso = 13 h; 24 h − 9 h = 15 h).
- [ ] El modelo (`AvisoConfig`) y DataStore ya guardan cualquier hora: solo falta la pantalla y sus validaciones (entre 1 h y 24 h, sin repetir).

### MT-43 · Países habituales en Ajustes · M · deps: MT-41 · *(propuesta de Cristina)*
- [x] En Ajustes, elegir **continentes** (Europa, Asia, África, América, Oceanía) y **países habituales** (donde suele cargar y descargar). Por defecto, ninguno: salen todos igual.
- [x] Los elegidos salen arriba en el selector, después de los recientes. Los demás **no se esconden**: la búsqueda los sigue encontrando debajo, en «Otros países». Escribir el código exacto («US») lo pone arriba igualmente.
- [x] ~~Continentes con `android.icu.util.Region`~~: no está en la API pública de Android. Tabla de la ONU (M49) en `Continentes.kt`, con un test que comprueba que todos los países ISO tienen continente (menos la Antártida). Turquía y Chipre cuentan como Europa.
- [x] Guardar en DataStore (`paises_habituales`, `continentes`) y test de la búsqueda con y sin filtro.
- [x] El test de DataStore usa `OkioStorage`: el almacenamiento de Android falla en Windows al sobrescribir el archivo (en el móvil y en el CI funciona igual).

> 🏁 **Release v0.1.0** → instalar en el móvil del conductor. **Usar en paralelo con la libreta 1–2 semanas** mientras se desarrolla E2.

---

## E2 · Viajes → **v0.2**

### MT-18 · Viajes · L · (antes MT-18 y MT-19)
- [ ] `Viaje`, `ViajeRepository`, entidad, DAO, migración y FK `jornada.viaje_id` (`SET_NULL`).
- [ ] km del viaje = suma de las jornadas (test).
- [ ] Lista con km acumulados, iniciar y finalizar viaje, y detalle con sus jornadas.
- [ ] Al iniciar una jornada con un viaje en curso, se asigna automáticamente.

---

## E3 · Descansos → **v0.2**

### MT-20 · Descansos: datos y clasificador (TDD) · L · (antes MT-20 y MT-21)
- [ ] `Descanso` (pausa/descanso, país), entidad, DAO, migración y validación de solapes.
- [ ] Clasificador: diario normal, reducido y fraccionado 3+9 (parte 1 y completo); semanal normal y reducido.
- [ ] Tests T7–T10 y T12 (clasificación).

### MT-22 · Pantalla Descansos · M · deps: MT-20 · (antes MT-22 y MT-23)
- [ ] Lista con chip de clasificación, y alta y edición manual. País prellenado con el país de fin de la última jornada.
- [ ] «¿Descansaste desde…?» al iniciar jornada: si hay un hueco sin descanso registrado, proponerlo prellenado. Se crea con 1 toque y solo si el usuario confirma.

> 🏁 **Release v0.2.0**

---

## E4 · Motor del Reglamento 561/2006 → **v0.3**

> **TDD estricto:** primero los tests de la tabla §13 de la especificación y luego la regla. Antes de cada tarjeta, **releer el artículo** en EUR-Lex.

### MT-24 · Núcleo del motor, conducción y pausas · XL · (antes MT-24, MT-25 y MT-26)
- [ ] `Resultado` (Cumple, Excedido, NoVerificado), `MotivoNoVerificado`, intervalo `[min, max]` y `RuleEngine`.
- [ ] `AsignadorSemanas`: semana lunes 00:00 en la zona de referencia, reparto de la conducción con intervalos (T20) y asignación de semanales (D-2).
- [ ] R-COND-1/2/3/4. Tests T1–T6 y T21.
- [ ] R-PAUSA: ≤ 4 h 30 → Cumple; si no → No verificado. Test T22.

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

### MT-32 · Pantalla Resumen y estadísticas · L
- [ ] Semana y bisemana: conducción, km, descansos y deudas con fecha límite. Km por día y por viaje.

### MT-33 · Exportar CSV · M
- [ ] `jornadas.csv`, `viajes.csv` y `descansos.csv` (UTF-8 BOM, `;`, coma decimal). Compartir con `Intent.ACTION_SEND`.
- [ ] Test del formato (que se abra bien en Excel).

### MT-33B · Exportar PDF · XL · deps: MT-30 · (antes MT-33B y MT-33C)
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
