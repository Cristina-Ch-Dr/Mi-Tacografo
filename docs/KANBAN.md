# Mi Tacógrafo — Planificación Kanban

> Las tarjetas están **en orden de ejecución**: se coge siempre la primera de «Por hacer» cuyas dependencias estén hechas.
> Cada tarjeta = 1 rama `feature/MT-xx-…` = 1 PR. Referencias técnicas en [ESPECIFICACION.md](ESPECIFICACION.md).
> Se puede pasar tal cual a **GitHub Projects** (1 tarjeta = 1 issue, etiqueta = épica, milestone = hito).

## Tablero

| 📥 Backlog | 📋 Por hacer | 🔨 En curso | 👀 Revisión | ✅ Hecho |
|---|---|---|---|---|
| E2 – E7 | **E0** MT-01 … MT-06 · **E1** MT-07 … MT-17 | — | — | — |

**Límite WIP:** 1 tarjeta en curso y 1 en revisión.
**Definición de Hecho (DoD):** compila · tests en verde · ktlint y Android Lint sin errores · textos en `strings.xml` (ES y UK) · probado en el móvil o el emulador · PR fusionado en `develop`.

## Hitos

| Hito | Contenido | Estimación acumulada | Resultado |
|---|---|---|---|
| **v0.1 — Adiós libreta** | E0 + E1 | ~16–24 h | El conductor ya registra jornadas, km y avisos en su móvil. |
| **v0.2 — Viajes y descansos** | E2 + E3 | ~26–38 h | Viajes, descansos registrados y clasificados. |
| **v0.3 — Reglamento** | E4 | ~42–58 h | Motor 561/2006 (nacional e internacional) con «No verificado» y tests. |
| **v1.0 — Completa** | E5 + E6 + E7 | ~56–80 h | Estadísticas, CSV y PDF, ucraniano revisado, pulido tras el uso real. |

Leyenda de tamaño: **S** ≤ 1 h · **M** 1–3 h · **L** 3–5 h · **XL** > 5 h (dividir si se puede).

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
- [ ] `AppDatabase` v1, `JornadaEntity`, `JornadaDao` (Flow), conversores `Instant`/`ZoneId` y mappers.
- [ ] `exportSchema = true` (carpeta `schemas/` en git).
- [ ] Test del DAO in-memory.

### MT-09 · Casos de uso de jornada · M · deps: MT-08
- [ ] `IniciarJornada`, `FinalizarJornada`, `EditarJornada`, `ObtenerJornadaActual` y `ObtenerUltimoKm`.
- [ ] Validaciones: una sola jornada abierta, `fin > inicio`, `kmFin ≥ kmInicio`, conducción ≤ duración.
- [ ] Tests con MockK.

### MT-10 · Pantalla Iniciar jornada · M · deps: MT-09, MT-04
- [ ] Hora (por defecto, ahora; selector de fecha y hora), km (por defecto, el último), país, ciudad y observaciones.
- [ ] ViewModel con `UiState` y validación en vivo.
- **Criterio:** iniciar una jornada en **≤ 2 toques** con los valores por defecto.

### MT-11 · Pantalla Inicio (jornada actual) · M · deps: MT-10
- [ ] Sin jornada: botón «Iniciar jornada».
- [ ] Con jornada: tiempo transcurrido (se refresca cada minuto), km de inicio, próximo aviso y botón «Finalizar».

### MT-12 · Pantalla Finalizar jornada · M · deps: MT-11
- [ ] Hora fin, km final, conducción del tacógrafo (entrada hh:mm cómoda) y km recorridos en vivo.

### MT-13 · Ajustes (DataStore) · M · deps: MT-04
- [ ] `SettingsRepository` con DataStore: avisos activos y sus offsets, idioma, **país de la empresa y de residencia** (necesarios para las reglas internacionales), nombre para el PDF y zona de referencia.
- [ ] Pantalla Ajustes con interruptores de avisos.

### MT-14 · Avisos con AlarmManager · L · deps: MT-12, MT-13
- [ ] Tabla `alarma_programada` (migración 1→2 + test de migración).
- [ ] `AndroidAlarmScheduler` (exacta con respaldo `setWindow`), `AlarmReceiver` y canal de notificación.
- [ ] Al iniciar, programar. Al finalizar, **cancelar todo**. Al editar el inicio, reprogramar.
- [ ] Pedir `POST_NOTIFICATIONS` (solo en 13+) y guiar al usuario para conceder `SCHEDULE_EXACT_ALARM` (12+, denegado por defecto en 14+).
- [ ] Aviso de optimización de batería (Xiaomi, Samsung, Huawei…).
- **Criterio:** probado con offsets de 1–2 min en modo debug y con el móvil bloqueado, **en la API mínima y en la última**.

### MT-15 · Reprogramar tras reinicio · S · deps: MT-14
- [ ] `BootReceiver` reprograma las alarmas futuras de la jornada abierta.
- **Criterio:** reiniciar el móvil con una jornada abierta → el aviso llega igual.

### MT-16 · Historial básico · M · deps: MT-12
- [ ] Lista de jornadas agrupadas por semana (lunes a domingo) con fecha, km y conducción.
- [ ] Detalle y edición, y borrado con confirmación.

### MT-17 · Copia de seguridad y restauración JSON · L · deps: MT-16
- [ ] `BackupPort` + implementación con kotlinx.serialization (`schemaVersion`).
- [ ] Exportar (CreateDocument) / importar (OpenDocument → resumen → confirmar → transacción → reprogramar alarmas).
- [ ] Auto Backup (`dataExtractionRules`).
- [ ] Test: exportar → importar = datos idénticos.

> 🏁 **Release v0.1.0** → instalar en el móvil del conductor. **Usar en paralelo con la libreta 1–2 semanas** mientras se desarrolla E2.

---

## E2 · Viajes → **v0.2**

### MT-18 · Dominio y Room de Viaje · M
- [ ] `Viaje`, `ViajeRepository`, entidad, DAO, migración y FK `jornada.viaje_id` (`SET_NULL`).
- [ ] km del viaje = suma de las jornadas (test).

### MT-19 · Pantallas de Viajes · M · deps: MT-18
- [ ] Lista con km acumulados, iniciar y finalizar viaje, y detalle con sus jornadas.
- [ ] Al iniciar una jornada con un viaje en curso, se asigna automáticamente.

---

## E3 · Descansos → **v0.2**

### MT-20 · Dominio y Room de Descanso · M
- [ ] `Descanso` (pausa/descanso, país), entidad, DAO, migración y validación de solapes.
- [ ] País prellenado con el país de fin de la última jornada.

### MT-21 · Clasificador de descansos (TDD) · L · deps: MT-20
- [ ] Diario normal, reducido y fraccionado 3+9 (parte 1 y completo); semanal normal y reducido.
- [ ] Tests T7–T10 y T12 (clasificación).

### MT-22 · Pantalla Descansos · M · deps: MT-21
- [ ] Lista con chip de clasificación, y alta y edición manual.

### MT-23 · «¿Descansaste desde…?» al iniciar jornada · S · deps: MT-22
- [ ] Si hay un hueco sin descanso registrado, proponerlo prellenado. Se crea con 1 toque y solo si el usuario confirma.

> 🏁 **Release v0.2.0**

---

## E4 · Motor del Reglamento 561/2006 → **v0.3**

> **TDD estricto:** primero los tests de la tabla §13 de la especificación y luego la regla. Antes de cada tarjeta, **releer el artículo** en EUR-Lex.

### MT-24 · Núcleo del motor · L
- [ ] `Resultado` (Cumple, Excedido, NoVerificado), `MotivoNoVerificado`, intervalo `[min, max]` y `RuleEngine`.
- [ ] `AsignadorSemanas`: semana lunes 00:00 en la zona de referencia, reparto de la conducción con intervalos (T20) y asignación de semanales (D-2).

### MT-25 · Reglas de conducción · M · deps: MT-24
- [ ] R-COND-1/2/3/4. Tests T1–T6 y T21.

### MT-26 · Regla de pausas · S · deps: MT-24
- [ ] R-PAUSA: ≤ 4 h 30 → Cumple; si no → No verificado. Test T22.

### MT-27 · Reglas de descanso diario · L · deps: MT-24, MT-21
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

### MT-30 · Tests con semanas reales · M · deps: MT-29
- [ ] 2–3 semanas de la libreta, **anonimizadas**, como fixtures JSON. Comparar los resultados con lo que el conductor sabe que pasó.

### MT-31 · Estados en la UI · M · deps: MT-30
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

### MT-33B · Modelo de informe (dominio) · M · deps: MT-31
- [ ] Puerto `ReportRenderer` e `InformeModel` puro: cabecera, resumen, tablas de jornadas y descansos, informe de reglas por semana y deudas.
- [ ] Selección de rango: semana, bisemana, mes, viaje o fechas libres.
- [ ] Test T25.

### MT-33C · Exportar PDF · L · deps: MT-33B
- [ ] `PdfReportRenderer` con `PdfDocument`: A4, tablas con paginación y cabecera repetida, «Página X de Y», pie con el aviso legal, y comprobar que el ucraniano (cirílico) se ve bien.
- [ ] Guardar (`CreateDocument`) o compartir (`FileProvider` + `ACTION_SEND`).
- [ ] Botón «Exportar → CSV / PDF» en Resumen y en el detalle de un viaje.
- [ ] Test instrumentado T26 (`PdfRenderer` cuenta las páginas).
- **Criterio:** el conductor genera el PDF de un viaje y lo envía por WhatsApp en ≤ 4 toques.

---

## E6 · Idiomas → **v1.0**

### MT-34 · Selector de idioma · S
- [ ] `locales_config.xml`, `setApplicationLocales` y opción en Ajustes.

### MT-35 · Traducción al ucraniano · M
- [ ] `values-uk/strings.xml` completo. **Revisión por el conductor**, que es hablante nativo.
- [ ] Lint `MissingTranslation` como error en la CI.

---

## E7 · Pulido y uso real → **v1.0**

### MT-36 · Aviso legal y primer arranque · S
- [ ] Pantalla de bienvenida con el aviso legal y la petición de permisos (notificaciones y alarmas exactas).

### MT-37 · Accesibilidad y uso en cabina · M
- [ ] Revisar tamaños táctiles, contraste y TalkBack. Probar con guantes y de noche con el tema oscuro.

### MT-38 · Test de UI del flujo crítico · M
- [ ] Compose UI test: iniciar → finalizar jornada → aparece en el historial.

### MT-38B · Matriz de compatibilidad en la CI · M · deps: MT-38
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
