# Mi Tacógrafo — Especificación técnica

> Documento de referencia para empezar a programar. Recoge **qué** hace la app, **cómo** está construida y **qué reglas legales** aplica.
> Última revisión: 2026-11-04 · Estado: borrador v1

---

## Índice

1. [Visión y alcance](#1-visión-y-alcance)
2. [Usuario y principios de diseño](#2-usuario-y-principios-de-diseño)
3. [Stack tecnológico](#3-stack-tecnológico)
4. [Arquitectura](#4-arquitectura)
5. [Modelo de datos](#5-modelo-de-datos)
6. [Fechas, horas y zonas horarias](#6-fechas-horas-y-zonas-horarias)
7. [Reglamento (CE) 561/2006 — reglas a implementar](#7-reglamento-ce-5612006--reglas-a-implementar)
8. [Motor de reglas — diseño](#8-motor-de-reglas--diseño)
9. [Avisos (alarmas)](#9-avisos-alarmas)
10. [Pantallas y flujos](#10-pantallas-y-flujos)
11. [Copias de seguridad y exportación](#11-copias-de-seguridad-y-exportación)
12. [Idiomas](#12-idiomas)
13. [Testing](#13-testing)
14. [Calidad, Git y CI/CD](#14-calidad-git-y-cicd)
15. [Privacidad y aviso legal](#15-privacidad-y-aviso-legal)
16. [Decisiones abiertas](#16-decisiones-abiertas)
17. [Ampliaciones futuras](#17-ampliaciones-futuras)
18. [Fuentes](#18-fuentes)

---

## 1. Visión y alcance

App Android **personal y sin conexión** para que un conductor profesional de camión sustituya su libreta:

- Registrar **jornadas** (inicio y fin, kilómetros, conducción total según el tacógrafo).
- Agrupar jornadas en **viajes**.
- Registrar **descansos** y clasificarlos (diario normal, reducido o fraccionado; semanal normal o reducido).
- Calcular límites del **Reglamento (CE) 561/2006** y mostrar el estado de cada regla: ✅ Cumple · ⚠️ Excedido · ❔ **No verificado**.
- **Avisos** personales a las 12:30, 13:00, 14:30 y 15:00 h desde el inicio de la jornada.
- Historial, estadísticas, exportación a **CSV y PDF**, y copia de seguridad y restauración.
- Soporta tanto **transporte internacional** (lo habitual) como **nacional**. Las reglas se aplican según el país de cada descanso.

**Fuera de alcance (v1):** cuentas de usuario, servidores, sincronización, lectura de la tarjeta del tacógrafo, conducción en equipo (*multi-manning*), tiempo de trabajo de la Directiva 2002/15/CE y transporte de viajeros.

---

## 2. Usuario y principios de diseño

| Principio | Implicación concreta |
|---|---|
| Se usa en cabina, con prisa y a veces de noche | Botones grandes (mín. 56 dp de alto), alto contraste, tema oscuro. |
| Pocos pasos | Iniciar jornada = **1 pantalla, 1 botón**. Los valores por defecto son la hora actual y el último km final. |
| No inventar datos | Si falta información, la regla muestra **«No verificado»**, nunca «Cumple». |
| Nunca perder datos | Todo se guarda en Room al instante. No hay estado "pendiente de guardar". |
| Bilingüe | Español y ucraniano, se cambia desde Ajustes sin reiniciar. |
| Recordatorio, no denuncia | Los avisos y resultados son orientativos. No sustituyen al tacógrafo. |

---

## 3. Stack tecnológico

| Capa | Tecnología | Notas |
|---|---|---|
| Lenguaje | **Kotlin** | JVM 17 como mínimo (el que pida AGP). |
| UI | **Jetpack Compose + Material 3** | Una sola Activity, con soporte de tema dinámico/oscuro. |
| Navegación | **Navigation Compose** (rutas *type-safe* con `@Serializable`) | |
| Estado | **ViewModel + StateFlow** (patrón UDF / MVI ligero) | `UiState` inmutable + eventos. |
| Asincronía | **Coroutines + Flow** | Room expone `Flow<List<…>>` y la UI se recalcula sola. |
| BD | **Room** (SQLite) + **KSP** | Migraciones versionadas y `exportSchema = true`. |
| Ajustes | **DataStore (Preferences)** | Idioma, avisos activos, país de la empresa y de residencia. |
| DI | **Hilt** | `@HiltViewModel`, módulos por capa. |
| Fechas | **java.time** (`Instant`, `ZoneId`, `ZonedDateTime`) | Nativo con minSdk 26. Con un minSdk menor, mediante *desugaring* (§3.1). |
| Serialización | **kotlinx.serialization** | Copia de seguridad en JSON y rutas de navegación. |
| PDF | **`android.graphics.pdf.PdfDocument`** (API nativa de Android, desde API 19) | Sin dependencias ni licencias. Se dibuja con `Canvas`. ❌ No usar iText: su licencia AGPL obligaría a publicar todo bajo AGPL. Ver §11. |
| Alarmas | **AlarmManager** (`setExactAndAllowWhileIdle`) + **NotificationCompat** | WorkManager **no** es puntual. Ver §9. |
| Tests | **JUnit 5** (dominio) · **MockK** · **Turbine** · **Truth/AssertK** · Room in-memory · Compose UI test | |
| Calidad | **ktlint** (estilo `ktlint_official` + reglas de Compose de `io.nlopez.compose.rules`) + **Android Lint** | Se ejecutan en la CI. detekt 1.23 no es compatible con JDK 25 y detekt 2 sigue en alfa: reevaluar cuando salga detekt 2 estable. |
| Build | **Gradle Kotlin DSL + Version Catalog** (`libs.versions.toml`) | |
| CI/CD | **GitHub Actions** | Tests en cada PR. APK firmado en cada tag `v*`. |

**SDK y compatibilidad** (detalle en §3.1)
- **`minSdk = 26` (Android 8.0)**. El conductor tiene Android 13 (API 33), así que tiene margen de sobra (D-5).
- `targetSdk` y `compileSdk`: **los últimos estables** que proponga Android Studio al crear el proyecto. Subirlos cada año.
- Usar siempre las **últimas versiones estables** de las librerías en el momento de crear el proyecto, centralizadas en `libs.versions.toml`.

**Permisos del Manifest**
- `POST_NOTIFICATIONS` (Android 13+, se pide en tiempo de ejecución).
- `SCHEDULE_EXACT_ALARM` (Android 12+, el usuario lo concede en Ajustes del sistema). Ver §9.
- `RECEIVE_BOOT_COMPLETED` (para reprogramar alarmas tras reiniciar).
- ❌ **Sin `INTERNET`**: así se garantiza técnicamente que los datos no salen del móvil.

### 3.1 Compatibilidad entre versiones de Android

**Objetivo:** la app funciona igual en el móvil actual del conductor (aunque sea antiguo), en cualquier móvil nuevo y en el de cualquier amigo al que se la pase.

**Cómo se consigue:**
- `minSdk` bajo = la versión más antigua en la que se instala.
- `targetSdk` alto = la app se comporta como esperan las versiones nuevas de Android.
- Las dos cosas son compatibles. **Todo el código que dependa de la versión va detrás de un `if (Build.VERSION.SDK_INT >= …)`** o usa las clases `*Compat` de AndroidX, que ya lo hacen por dentro.

| Si `minSdk` < 26 | Qué hacer |
|---|---|
| `java.time` no existe de forma nativa | Activar **core library desugaring** (`isCoreLibraryDesugaringEnabled = true` + dependencia `desugar_jdk_libs`). El código no cambia. |

**Diferencias por versión que hay que cubrir:**

| Versión | Cambio | Cómo lo gestiona la app |
|---|---|---|
| 6.0 (API 23) | Modo reposo (*Doze*) | `setExactAndAllowWhileIdle()` para que los avisos lleguen igual. |
| 8.0 (API 26) | Canales de notificación obligatorios | Crear el canal solo si `SDK_INT >= 26`. `NotificationCompat` lo gestiona. |
| 12 (API 31) | Alarmas exactas requieren permiso | Comprobar `canScheduleExactAlarms()`. Si no hay permiso, mostrar una tarjeta que lleve a Ajustes y usar `setWindow()` como respaldo (§9). |
| 13 (API 33) | Permiso de notificaciones en tiempo de ejecución + idioma por app | Pedir `POST_NOTIFICATIONS` solo en 33+. El idioma usa `AppCompatDelegate`, que funciona también en versiones anteriores. |
| 14 (API 34) | `SCHEDULE_EXACT_ALARM` **denegado por defecto** en instalaciones nuevas | El primer arranque guía al usuario para concederlo. Es clave en un móvil nuevo o en el de un amigo. |
| 15 (API 35) | Pantalla completa (*edge-to-edge*) obligatoria | `enableEdgeToEdge()` + `Scaffold`/insets de Compose desde el principio. |
| 16+ (API 36+) | Gesto «atrás» predictivo y pantallas grandes, sin bloqueo de orientación | Usar `BackHandler` de Compose y un diseño que se adapte a pantallas anchas y en horizontal (tablet, móvil plegable). |
| Fabricantes (Xiaomi, Huawei, Samsung…) | Ahorro de batería agresivo que mata las alarmas | Pantalla de ayuda con un enlace a «Optimización de batería» (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` solo con explicación previa) y aviso si `isIgnoringBatteryOptimizations()` = false. |

**Pruebas de compatibilidad:**
- Emuladores en Android Studio con, como mínimo, la **API del `minSdk`**, una **intermedia (API 30)** y la **última estable**.
- En la CI: tests instrumentados en un emulador de la **API mínima** y otro de la **última** (matriz de GitHub Actions con `reactivecircus/android-emulator-runner`).
- Probar siempre en el móvil real del conductor antes de cada release.

**Pasarla a un amigo:**
- Se le envía el **mismo APK** de la GitHub Release.
- Cada móvil tiene **sus propios datos**: no se comparte nada entre conductores.
- El primer arranque pide los permisos y el país de la empresa y de residencia.
- Las actualizaciones posteriores se instalan encima sin perder datos, siempre que el APK esté firmado con el mismo keystore.
- Si los amigos usan otro idioma (rumano, polaco…), añadir otro `values-xx/strings.xml` es trivial gracias a §12.

---

## 4. Arquitectura

### 4.1 Hexagonal (puertos y adaptadores) en módulos Gradle

```
mi-tacografo/
├── app/                      ← Adaptadores de entrada (UI) y de plataforma Android
├── core/
│   ├── domain/               ← Kotlin PURO (sin Android). Modelo, reglas 561, casos de uso, puertos
│   └── data/                 ← Adaptadores de salida: Room, DataStore, backup JSON, CSV
├── gradle/libs.versions.toml
├── .github/workflows/
└── docs/
```

```
            ┌──────────────────────────── app ────────────────────────────┐
            │  Compose UI ─► ViewModels ─► casos de uso                    │
            │  platform/: AndroidAlarmScheduler, NotificationHelper,       │
            │             BootReceiver, SystemClock                        │
            └───────────────────────────┬─────────────────────────────────┘
                                        │ depende de
            ┌───────────────────── core:domain ───────────────────────────┐
            │  model/      Jornada, Viaje, Descanso, Duracion…             │
            │  rules/      RuleEngine, DailyDrivingRule, WeeklyRestRule…   │
            │  usecase/    IniciarJornada, FinalizarJornada, …             │
            │  port/       JornadaRepository, DescansoRepository,          │
            │              ViajeRepository, AlarmScheduler, Clock,         │
            │              SettingsRepository, BackupPort                  │
            └───────────────────────────▲─────────────────────────────────┘
                                        │ implementa puertos
            ┌───────────────────── core:data ─────────────────────────────┐
            │  room/ (entities, DAOs, mappers)  datastore/  backup/  csv/  │
            │  pdf/ (PdfReportRenderer)                                    │
            └──────────────────────────────────────────────────────────────┘
```

**Reglas de dependencia**
- `core:domain` → no depende de nada (solo Kotlin stdlib, coroutines-core y `javax.inject` para las anotaciones).
- `core:data` → depende de `domain`.
- `app` → depende de `domain` y `data` (este último solo para el *wiring* de Hilt).
- Las entidades de Room **no salen** de `core:data`: se convierten con *mappers* a modelos de dominio.

### 4.2 Paquetes

Proponer el `applicationId` antes de crear el proyecto, por ejemplo `com.<usuario>.mitacografo`. No se puede cambiar una vez instalada la app sin perder los datos.

```
app/src/main/java/.../mitacografo/
├── MainActivity.kt
├── MiTacografoApp.kt               (@HiltAndroidApp)
├── navigation/                     (NavHost, rutas)
├── ui/theme/                       (colores, tipografía, tamaños grandes)
├── ui/components/                  (BigButton, StatusChip, DurationText…)
├── feature/
│   ├── home/        (HomeScreen, HomeViewModel)
│   ├── startday/    (iniciar jornada)
│   ├── endday/      (finalizar jornada)
│   ├── history/
│   ├── trips/
│   ├── rests/
│   ├── stats/
│   └── settings/
├── platform/        (alarmas, notificaciones, receivers)
└── di/              (módulos Hilt)
```

### 4.3 Flujo de datos

```
Room Flow ─► Repository (domain port) ─► UseCase / RuleEngine (puro) ─► ViewModel.StateFlow ─► Compose
Usuario ─► evento ─► ViewModel ─► UseCase ─► Repository ─► Room
```

**Los resultados de las reglas NO se guardan en la BD.** Se recalculan siempre a partir de los registros. Un conductor genera unas 300 jornadas al año, así que el cálculo cuesta milisegundos. Esto cumple automáticamente el requisito de *"recalcular al modificar un registro anterior"*.

---

## 5. Modelo de datos

### 5.1 Dominio

```kotlin
data class Jornada(
    val id: Long,
    val inicio: Instant,
    val zonaInicio: ZoneId,
    val kmInicio: Int,
    val paisInicio: String?,       // ISO 3166-1 alpha-2, ej. "ES"
    val ciudadInicio: String?,
    val fin: Instant?,             // null = jornada abierta
    val zonaFin: ZoneId?,
    val kmFin: Int?,
    val paisFin: String?,
    val ciudadFin: String?,
    val conduccionTacografo: Duration?, // lo que indica el tacógrafo al final
    val viajeId: Long?,
    val observaciones: String?,
) {
    val kmRecorridos: Int? get() = kmFin?.let { it - kmInicio }
    val abierta: Boolean get() = fin == null
}

data class Viaje(
    val id: Long,
    val nombre: String,
    val inicio: Instant,
    val fin: Instant?,             // null = viaje en curso
    val observaciones: String?,
)
// km del viaje = suma de kmRecorridos de sus jornadas (no se guarda)

enum class TipoRegistroDescanso { PAUSA, DESCANSO }

data class Descanso(
    val id: Long,
    val inicio: Instant,
    val zonaInicio: ZoneId,
    val fin: Instant,
    val zonaFin: ZoneId,
    val tipoRegistro: TipoRegistroDescanso, // pausa (en jornada) o descanso (entre jornadas)
    val pais: String?,             // necesario para la regla de 2 semanales reducidos fuera de casa
    val observaciones: String?,
)
// La clasificación (diario normal/reducido/fraccionado, semanal normal/reducido)
// la CALCULA el motor de reglas. No se guarda.
```

### 5.2 Tablas Room

| Tabla | Columnas clave | Índices |
|---|---|---|
| `jornada` | `id`, `inicio_epoch_ms`, `zona_inicio`, `km_inicio`, `fin_epoch_ms?`, `zona_fin?`, `km_fin?`, `conduccion_min?`, `viaje_id?` (FK → viaje, `SET_NULL`), … | `inicio_epoch_ms`, `viaje_id` |
| `viaje` | `id`, `nombre`, `inicio_epoch_ms`, `fin_epoch_ms?`, … | `inicio_epoch_ms` |
| `descanso` | `id`, `inicio_epoch_ms`, `zona_inicio`, `fin_epoch_ms`, `zona_fin`, `tipo`, `pais?`, … | `inicio_epoch_ms` |
| `alarma_programada` | `id`, `jornada_id` (FK, `CASCADE`), `dispara_epoch_ms`, `etiqueta` | `jornada_id` |

**Invariantes que validan los casos de uso**
- Solo puede haber **una jornada abierta** a la vez.
- `fin > inicio` y `kmFin ≥ kmInicio`.
- Los descansos no se solapan entre sí. Si un descanso se solapa con una jornada, se avisa pero se permite (puede haber errores de registro).
- `conduccionTacografo ≤ fin − inicio`.

---

## 6. Fechas, horas y zonas horarias

1. **Guardar siempre `Instant` (UTC, epoch ms) + `ZoneId`** del lugar donde ocurrió (ej. `Europe/Madrid`, `Europe/Kyiv`).
2. **Calcular duraciones con `Instant`**, nunca con horas locales. Así, un cambio de hora en marzo u octubre no rompe nada.
3. **Mostrar** cada registro en la zona en la que ocurrió (`ZonedDateTime.ofInstant(instant, zone)`), indicando la zona si es distinta de la del móvil.
4. **Semana legal:** de **lunes 00:00 a domingo 24:00**. Hay que decidir qué zona horaria marca ese lunes 00:00. Propuesta: una **zona de referencia** configurable en Ajustes (por defecto, la del país de la empresa). Ver §16.
5. Zona por defecto al registrar = `ZoneId.systemDefault()`, editable si el conductor apunta algo a posteriori.
6. El puerto `Clock` del dominio inyecta la hora actual. En los tests se usa un reloj fijo.

---

## 7. Reglamento (CE) 561/2006 — reglas a implementar

> Texto base: Reglamento (CE) 561/2006 consolidado, con las modificaciones de los Reglamentos (UE) **2020/1054** (Paquete de Movilidad I, aplicable a mercancías) y **2024/1258** (solo afecta al transporte **ocasional de viajeros**, así que no aplica aquí).
> ⚠️ Antes de implementar cada regla, contrastar con el texto consolidado vigente en EUR-Lex (ver §18). Las referencias entre corchetes son al artículo.

### 7.1 Definiciones (art. 4)

| Término | Definición |
|---|---|
| **Semana** | De lunes 00:00 a domingo 24:00. |
| **Pausa** | Periodo en el que no se conduce ni se trabaja, usado para reponerse. |
| **Descanso diario normal** | ≥ 11 h, o **fraccionado** en 2 partes: la primera ≥ 3 h y la segunda ≥ 9 h, en ese orden. |
| **Descanso diario reducido** | ≥ 9 h y < 11 h. |
| **Descanso semanal normal** | ≥ 45 h. |
| **Descanso semanal reducido** | ≥ 24 h y < 45 h. |
| **Tiempo de conducción diario** | Conducción total entre el final de un descanso diario (o semanal) y el inicio del siguiente. |
| **Periodo de conducción** | Conducción acumulada entre pausas o descansos. |

> En la app, "jornada" = periodo entre dos descansos. Normalmente coincide con el tiempo de conducción diario.

### 7.2 Conducción [art. 6]

| ID | Regla | Límite |
|---|---|---|
| R-COND-1 | Conducción diaria | ≤ **9 h** |
| R-COND-2 | Conducción diaria ampliada | ≤ **10 h**, como máximo **2 veces por semana** |
| R-COND-3 | Conducción semanal | ≤ **56 h** |
| R-COND-4 | Dos semanas consecutivas | ≤ **90 h** |

### 7.3 Pausas [art. 7]

| ID | Regla |
|---|---|
| R-PAUSA-1 | Tras **4 h 30** de conducción, pausa de **45 min** seguidos, salvo que empiece un descanso. |
| R-PAUSA-2 | Se puede fraccionar en **15 min + 30 min**, en ese orden, dentro de las 4 h 30. |

> ⚠️ La app **no tiene los tramos de conducción**, solo el total del tacógrafo. Por eso:
> - conducción del día ≤ 4 h 30 → ✅ (no hacía falta pausa);
> - en cualquier otro caso → ❔ **No verificado**, aunque haya pausas registradas, porque no se sabe dónde caían.

### 7.4 Descanso diario [art. 8.1–8.4]

| ID | Regla |
|---|---|
| R-DD-1 | Dentro de cada periodo de **24 h** desde el final del descanso diario o semanal anterior, debe haberse tomado un **nuevo descanso diario**. |
| R-DD-2 | Si la parte del descanso que cae dentro de esas 24 h es ≥ 9 h y < 11 h → cuenta como **reducido**. |
| R-DD-3 | Máximo **3 descansos diarios reducidos** entre dos descansos semanales. |
| R-DD-4 | Un descanso diario puede ampliarse hasta convertirse en un descanso semanal. |
| R-DD-5 | Fraccionado 3 + 9: la primera parte ≥ 3 h, la segunda ≥ 9 h, y ambas cuentan como un descanso **normal**. |

**Reglas de cálculo derivadas (validar con la guía oficial antes de implementar):**
- Con descanso normal, la **amplitud máxima** (de fin de descanso a inicio del siguiente) es 24 − 11 = **13 h**.
- Con descanso reducido es 24 − 9 = **15 h**.
- Con fraccionado 3 + 9, la segunda parte (9 h) debe **terminar** dentro de las 24 h, así que la amplitud máxima es 24 − 12 = **12 h** contando la parte de 3 h.

> Esto explica los avisos: **12:30 / 13:00** = margen y límite con descanso normal; **14:30 / 15:00** = margen y límite con descanso reducido.

### 7.5 Descanso semanal [art. 8.6–8.9]

| ID | Regla |
|---|---|
| R-DS-1 | En **2 semanas consecutivas**: 2 semanales normales, **o** 1 normal + 1 reducido (≥ 24 h). |
| R-DS-2 | El descanso semanal debe **empezar** como muy tarde al final de **6 periodos de 24 h** (144 h) desde el final del semanal anterior. |
| R-DS-3 | **Transporte internacional de mercancías:** se permiten **2 semanales reducidos consecutivos** si se toman **fuera** del Estado miembro de la empresa **y** del país de residencia del conductor. En cualquier periodo de **4 semanas consecutivas** debe haber **al menos 4 semanales, de los cuales al menos 2 normales**. |
| R-DS-4 | **Compensación:** cada reducción (45 h − duración real) se compensa con un descanso equivalente **en bloque** antes del **final de la 3ª semana siguiente** a la semana del reducido. |
| R-DS-5 | Tras **2 reducidos consecutivos** (R-DS-3), el siguiente semanal debe ir **precedido** de la compensación de ambos. |
| R-DS-6 | La compensación debe ir **unida a otro descanso de al menos 9 h**. |
| R-DS-7 | Un semanal que cae entre dos semanas cuenta **solo en una** de ellas. |
| R-DS-8 | *(Informativo)* El semanal normal y los descansos de compensación > 45 h **no pueden hacerse en el vehículo**. |
| R-DS-9 | *(Informativo)* Volver al centro de operaciones o a casa **cada 4 semanas**. Si hubo 2 reducidos consecutivos, antes de la compensación > 45 h. Es obligación de la empresa. La app solo lo recuerda. |

### 7.6 Otros artículos relevantes (informativos en v1)

- **Art. 9, ferri/tren:** un descanso diario normal o semanal reducido puede interrumpirse hasta 2 veces (máx. 1 h en total) si hay litera. Un semanal normal en ferri o tren solo vale si el trayecto es ≥ 8 h con cabina-litera. → Campo opcional "en ferri/tren" en `Descanso` para v2.
- **Art. 12, excepciones:** en circunstancias excepcionales se puede superar la conducción para llegar a un lugar seguro (y, desde 2020, hasta 1 h o 2 h más para volver a la base o a casa, con condiciones). → La app permite marcar "excepción art. 12" con un texto, y la regla muestra ⚠️ con la nota.

### 7.7 Transporte internacional y nacional

El conductor hace **sobre todo transporte internacional**, pero a veces nacional. La app **no tiene un interruptor global**: cada caso se decide con el **país de cada descanso** comparado con el país de la empresa y el de residencia (Ajustes).

| Situación | Regla aplicada |
|---|---|
| Dos semanales reducidos consecutivos, **ambos** fuera del país de la empresa **y** del de residencia | Permitido (R-DS-3), con la condición de 4 semanales en 4 semanas, al menos 2 normales, y R-DS-5 (compensación antes del siguiente semanal). |
| Dos semanales reducidos consecutivos y **alguno** en el país de la empresa o de residencia (caso nacional) | ⚠️ Excedido R-DS-1: en 2 semanas consecutivas solo se permite 1 reducido. |
| Un descanso reducido **sin país** registrado | ❔ No verificado R-DS-3 (`MotivoNoVerificado.SIN_PAIS_DESCANSO`). |
| Países de la empresa o de residencia sin configurar | ❔ No verificado R-DS-3 y un aviso en Inicio para completar Ajustes. |

Todas las demás reglas (conducción, pausas, diario, 6 × 24 h, compensación en 3 semanas) son **iguales** en ambos casos.

**UX:** el campo **país** del descanso se rellena con el **país de fin de la última jornada**, así que normalmente no cuesta ningún toque. Es editable.

---

## 8. Motor de reglas — diseño

### 8.1 Resultado de una regla

```kotlin
sealed interface Resultado {
    val reglaId: String
    data class Cumple(override val reglaId: String, val detalle: Detalle) : Resultado
    data class Excedido(override val reglaId: String, val detalle: Detalle) : Resultado
    data class NoVerificado(override val reglaId: String, val motivo: MotivoNoVerificado) : Resultado
}

enum class MotivoNoVerificado {
    SIN_CONDUCCION_TACOGRAFO,      // jornada sin el dato del tacógrafo
    SIN_TRAMOS_CONDUCCION,         // pausas: no se conocen los tramos
    DESCANSO_NO_REGISTRADO,        // hueco entre jornadas sin descanso registrado
    JORNADA_CRUZA_SEMANA,          // no se sabe cuánto se condujo a cada lado de la medianoche del domingo
    DATOS_INSUFICIENTES_PERIODO,   // faltan semanas anteriores para evaluar
    SIN_PAIS_DESCANSO,             // no se sabe si el reducido se tomó fuera de casa (R-DS-3)
    PAISES_NO_CONFIGURADOS,        // falta el país de la empresa o de residencia en Ajustes
}
```

El dominio **devuelve códigos, no textos**. La UI los traduce con `stringResource`, lo que facilita tener dos idiomas.

### 8.2 Lógica de intervalos (clave para el «No verificado»)

Cuando un dato es incierto, se calcula un **intervalo [mínimo, máximo]**:

```
si máximo ≤ límite  → Cumple
si mínimo > límite  → Excedido
si no               → NoVerificado
```

*Ejemplo:* una jornada empieza el domingo a las 20:00 y termina el lunes a las 06:00 con 6 h de conducción. A la semana A le pueden corresponder entre 0 h y 4 h, y a la B entre 2 h y 6 h. Si con el peor reparto la semana A sigue ≤ 56 h, el resultado es **Cumple** sin inventar nada.

### 8.3 Pipeline

```
1. Cargar jornadas + descansos de la ventana necesaria (≈ 5 semanas atrás).
2. ClasificadorDescansos: cada Descanso registrado → DiarioNormal | DiarioReducido |
   ParteFraccionado(3h) | DiarioFraccionadoCompleto | SemanalNormal | SemanalReducido | Pausa.
3. AsignadorSemanas: asigna cada semanal a UNA semana (R-DS-7) y reparte la conducción por semanas (con intervalos).
4. Reglas independientes (cada una es una clase con `fun evaluar(ctx): List<Resultado>`):
   DailyDrivingRule, ExtendedDrivingCountRule, WeeklyDrivingRule, FortnightDrivingRule,
   BreakRule, DailyRest24hRule, ReducedDailyRestCountRule,
   WeeklyRestPatternRule, SixPeriodsRule, ConsecutiveReducedRule, CompensationRule.
5. RuleEngine agrega → InformeSemana / InformeJornada.
```

Cada regla se puede testear sola y añadir una nueva no toca las demás (principio abierto/cerrado).

### 8.4 Compensaciones (R-DS-4/6)

- `deuda = 45 h − duración` por cada semanal reducido, con `fechaLimite = fin de la 3ª semana siguiente`.
- Se detecta compensación cuando un descanso supera su duración base: la parte de un semanal por encima de 45 h, o la parte de un diario por encima de 11 h.
- La compensación se aplica a la deuda más antigua (FIFO).
- Estados de cada deuda: `PENDIENTE` · `COMPENSADA` · `VENCIDA` · `NO_VERIFICADO` (hay huecos sin registrar).
- ⚠️ Ver la decisión abierta D-3 sobre la duración base de un diario.

### 8.5 Conversión de huecos en descansos (sin inventar)

Al **iniciar una jornada**, si la anterior terminó y no hay descanso registrado entre ambas, la app pregunta:

> «¿Descansaste desde *ayer 19:40* hasta *ahora*?» [Sí] [No / editar]

Con un toque se crea el descanso. Es el usuario quien lo confirma, así que no se inventa nada y solo cuesta un paso.

---

## 9. Avisos (alarmas)

| Aviso | Por defecto | Significado |
|---|---|---|
| 12 h 30 | ✅ | 30 min antes del límite con descanso normal |
| 13 h 00 | ✅ | Límite de amplitud con descanso normal (11 h) |
| 14 h 30 | ✅ | 30 min antes del límite con descanso reducido |
| 15 h 00 | ✅ | Límite de amplitud con descanso reducido (9 h) |

**Implementación**
- Al iniciar la jornada: `AlarmScheduler.programar(jornadaId, inicio + offset)` para cada aviso activo. Se guarda en la tabla `alarma_programada`.
- Para que sean puntuales con el móvil en reposo se usa `AlarmManager.setExactAndAllowWhileIdle()`.
  - En Android 12+ hace falta `SCHEDULE_EXACT_ALARM`. Si `canScheduleExactAlarms()` devuelve `false`, se muestra una tarjeta que lleva a Ajustes y, mientras tanto, se usa `setWindow()` (aproximado) como respaldo.
  - `USE_EXACT_ALARM` es solo para apps de reloj o calendario. **No usarlo** por si la app llega algún día a Google Play.
- `AlarmReceiver` muestra la notificación en el canal `avisos_jornada` (importancia alta, con sonido y vibración).
- **Al finalizar la jornada:** cancelar todas sus alarmas y borrar las filas.
- `BootReceiver` (`BOOT_COMPLETED`) reprograma desde la tabla las alarmas futuras de la jornada abierta.
- Los avisos se basan en `Instant`, así que un cambio de zona horaria **no** los desplaza.
- Si se edita la hora de inicio de la jornada abierta, se **reprograman** todas sus alarmas.
- El texto del aviso deja claro que es un **recordatorio personal**, no un incumplimiento.

---

## 10. Pantallas y flujos

| # | Pantalla | Contenido principal |
|---|---|---|
| 1 | **Inicio / Jornada actual** | Sin jornada: botón enorme «Iniciar jornada». Con jornada: tiempo transcurrido, próximo aviso, km de inicio, botones «Finalizar jornada» y «Registrar pausa». Debajo, resumen de la semana: conducción 0/56 h, ampliaciones usadas 0/2, reducidos 0/3, estado del semanal. |
| 2 | **Iniciar jornada** | Hora (por defecto, ahora; editable), km inicial (por defecto, el último km final), país (por defecto, el último), ciudad y observaciones opcionales, y viaje en curso. Botón «Empezar». |
| 3 | **Finalizar jornada** | Hora fin, km final, **conducción según tacógrafo (hh:mm)**, país y ciudad. Muestra en vivo los km recorridos. Al guardar, cancela los avisos. |
| 4 | **Historial** | Lista agrupada por semana con fecha, km, conducción y chips de estado (✅⚠️❔). Al pulsar, detalle y edición. |
| 5 | **Viajes** | Lista de viajes con km acumulados. Detalle con sus jornadas. Iniciar o finalizar viaje. |
| 6 | **Descansos** | Lista con la clasificación calculada. Alta y edición manual (inicio y fin, pausa o descanso, país). |
| 7 | **Resumen y estadísticas** | Semana y bisemana: conducción, km, descansos, deudas de compensación con fecha límite. Informe de reglas con su estado. Botón **«Exportar» → CSV o PDF** del periodo visible. |
| 8 | **Ajustes** | Idioma, avisos (activar o desactivar y editar), país de la empresa y de residencia (**obligatorios**: sin ellos R-DS-3 queda «No verificado»), nombre para el PDF (opcional), zona de referencia, copia de seguridad o restauración, exportar CSV o PDF, aviso legal, versión. |

**Navegación:** barra inferior con Inicio · Historial · Viajes · Resumen. Descansos y Ajustes se abren desde el menú o desde Inicio.

---

## 11. Copias de seguridad y exportación

**Copia de seguridad (JSON)**
- Formato: `{ "schemaVersion": 1, "exportedAt": "...", "jornadas": [...], "viajes": [...], "descansos": [...], "ajustes": {...} }`.
- Se guarda con el **Storage Access Framework** (`ActivityResultContracts.CreateDocument("application/json")`). El usuario elige Drive, Descargas, WhatsApp, etc.
- **Restaurar:** `OpenDocument` → validar `schemaVersion` → mostrar un resumen («312 jornadas, 25 viajes…») → confirmar → reemplazar todo en **una transacción** → reprogramar alarmas.
- Además, **Android Auto Backup** está activado (`android:allowBackup` + `dataExtractionRules`) como red de seguridad.
- Recordatorio opcional: «Hace 30 días que no haces copia».

**Exportar CSV**
- Ficheros `jornadas.csv`, `viajes.csv` y `descansos.csv`, en **UTF-8 con BOM**, con **separador `;`** y **coma decimal**, para que el Excel español los abra bien.
- Fechas en hora local del registro + columna de zona.

**Exportar PDF (informe para leer o imprimir)**
- **Rango configurable:** semana, bisemana, mes, viaje o fechas libres.
- **Contenido:**
  1. Cabecera: nombre del conductor (opcional, en Ajustes), periodo y fecha de generación.
  2. Resumen: total de km, conducción, número de jornadas y descansos.
  3. Tabla de jornadas: fecha, inicio y fin con zona, km inicio, fin y recorridos, conducción y país o ciudad.
  4. Tabla de descansos con su clasificación.
  5. Por semana, informe de reglas (✅ Cumple / ⚠️ Excedido / ❔ No verificado) y deudas de compensación.
  6. Pie de página con el aviso legal (§15) y «Página X de Y».
- **Idioma:** el activo en la app. La tipografía por defecto de Android (Roboto) incluye cirílico, así que el ucraniano funciona sin incrustar fuentes.
- **Tamaño:** A4 vertical (595 × 842 pt), con paginación manual: si una tabla no cabe, sigue en otra página y repite la cabecera.
- **Diseño hexagonal:**
  - `core:domain` construye un `InformeModel` puro (secciones, filas y textos ya resueltos) a partir de los datos y del `RuleEngine`. Se puede testear sin Android.
  - El adaptador `PdfReportRenderer` (en `core:data/pdf`) implementa el puerto `ReportRenderer` y solo dibuja.
  - Más adelante se podría añadir otro adaptador (HTML, Excel) sin tocar el dominio.
- **Salida:** guardar con `CreateDocument("application/pdf")` o compartir con `FileProvider` + `ACTION_SEND` (WhatsApp, correo, Drive).
- **Tests:**
  - el `InformeModel` en el dominio: totales, filas y paginación lógica;
  - un test instrumentado que genera el PDF y lo abre con `PdfRenderer` para comprobar el número de páginas.

---

## 12. Idiomas

- `res/values/strings.xml` (español, por defecto) y `res/values-uk/strings.xml` (ucraniano).
- `res/xml/locales_config.xml` + `android:localeConfig` en el Manifest, para el idioma por app de Android 13+.
- En Ajustes: `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("uk"))`. Con AppCompat 1.6+ funciona también en Android < 13 (requiere `AppCompatActivity` o el servicio `autoStoreLocales`).
- **Ningún texto en el código.** El dominio devuelve enums/códigos y la UI los traduce.
- Formatear fechas y números con el `Locale` activo.
- Que el conductor revise el ucraniano en la fase de uso real.

---

## 13. Testing

| Nivel | Qué | Herramientas |
|---|---|---|
| Unitario (dominio) | **Cada regla** con casos límite, el clasificador de descansos, los intervalos, las compensaciones y los cambios de hora | JUnit 5, AssertK/Truth, `Clock` fijo |
| Unitario (ViewModels) | Estados y eventos | MockK, Turbine, `kotlinx-coroutines-test` |
| Integración (data) | DAOs, mappers, migraciones y copia de seguridad (exportar → importar = mismos datos) | Room in-memory, `MigrationTestHelper` |
| UI | Flujos críticos: iniciar → finalizar jornada | Compose UI Test |

**Casos de prueba mínimos del motor (TDD: escribir antes que la regla)**

| # | Escenario | Esperado |
|---|---|---|
| T1 | Conducción 9 h 00 | Cumple |
| T2 | Conducción 9 h 01 | Ampliada (cuenta 1/2) |
| T3 | Tercera jornada de 10 h en la misma semana | Excedido R-COND-2 |
| T4 | Conducción 10 h 01 | Excedido R-COND-1 |
| T5 | Semana con 56 h 00 | Cumple. Con 56 h 01, Excedido |
| T6 | Semana 1 = 56 h, semana 2 = 35 h (91 h) | Excedido R-COND-4 |
| T7 | Descanso 11 h | Diario normal |
| T8 | Descanso 10 h 59 | Diario reducido |
| T9 | Descanso 3 h + descanso 9 h dentro de 24 h | Fraccionado = normal |
| T10 | Descanso 2 h 59 + 9 h | NO es fraccionado. 9 h = reducido |
| T11 | Cuarto diario reducido entre dos semanales | Excedido R-DD-3 |
| T12 | Descanso 44 h 59 | Semanal reducido. Deuda de 1 min |
| T13 | Reducido de 24 h en la semana 10, compensación de 21 h unida a un diario de 11 h en la semana 12 | Compensada |
| T14 | Igual que T13 pero sin compensar al acabar la semana 13 | Vencida |
| T15 | Semanal empieza 144 h 01 después del anterior | Excedido R-DS-2 |
| T16 | 2 semanales reducidos consecutivos fuera de ES y del país de residencia | Cumple R-DS-3 (si las 4 semanas tienen 2 normales) |
| T17 | 2 reducidos consecutivos, uno en ES (empresa en ES) | Excedido (caso nacional) |
| T17b | 2 reducidos consecutivos fuera de casa, pero solo 1 normal en 4 semanas | Excedido R-DS-3 |
| T17c | 2 reducidos consecutivos, uno sin país registrado | No verificado (SIN_PAIS_DESCANSO) |
| T17d | Países de la empresa o de residencia sin configurar | R-DS-3 No verificado (PAISES_NO_CONFIGURADOS) |
| T17e | 2 reducidos fuera de casa sin compensar antes del siguiente semanal | Excedido R-DS-5 |
| T18 | Jornada con un cambio de hora (último domingo de octubre) | Duración correcta (+1 h de reloj, no de tiempo real) |
| T19 | Jornada que empieza en `Europe/Madrid` y acaba en `Europe/Kyiv` | Duración correcta |
| T20 | Jornada que cruza el domingo a las 24:00 | Intervalos. Cumple o No verificado según márgenes |
| T21 | Jornada sin conducción del tacógrafo | No verificado (SIN_CONDUCCION_TACOGRAFO) |
| T22 | Conducción 4 h 30 sin pausas registradas | Pausa: Cumple. Con 4 h 31: No verificado |
| T23 | Hueco de 30 h entre jornadas sin descanso registrado | R-DD-1 No verificado |
| T24 | Editar una jornada de hace 2 semanas | Se recalculan los informes de esa bisemana |
| T25 | `InformeModel` de una semana con 5 jornadas y 6 descansos | Totales de km y conducción correctos; filas en orden cronológico |
| T26 | PDF con 60 jornadas | Varias páginas, cabecera de tabla repetida, «Página X de Y» correcto |

**Meta de cobertura:** ≥ 90 % en `core:domain/rules`.

**Datos reales:** usar semanas de la libreta, **anonimizadas** (solo horas y km, sin lugares), como fixtures en `core/domain/src/test/resources/`.

---

## 14. Calidad, Git y CI/CD

**Git**
- Repo público en GitHub con ramas **Gitflow**: `main` (releases) · `develop` · `feature/MT-xx-descripcion` · `release/x.y` · `hotfix/*`.
- Commits convencionales: `feat:`, `fix:`, `test:`, `refactor:`, `docs:`, `chore:`.
- PR a `develop` con la CI en verde. Las tarjetas del Kanban se enlazan a los PR (`Closes #12`).
- `.gitignore`: `local.properties`, `*.jks`, `*.keystore`, `/build`, `.idea/` (salvo estilos compartidos).
- 🔒 **Nunca subir** el keystore, sus contraseñas ni datos reales del conductor.

**GitHub Actions**
- `ci.yml` (en cada push o PR a `develop`/`main`): `ktlintCheck`, tests unitarios, `lintDebug` y `assembleDebug`, y sube los informes como artefacto.
- `release.yml` (en cada tag `v*`): decodificar el keystore desde un *Secret* en Base64 → `assembleRelease` → subir el APK a la **GitHub Release**.
- Versionado: `versionName` = tag (semver), `versionCode` incremental.

**Instalación en el móvil del conductor:** descargar el APK de la Release → permitir "Instalar apps desconocidas" para el navegador o el gestor de archivos → instalar. Las actualizaciones se instalan **encima** (siempre con el mismo keystore) y **conservan los datos**.

> ⚠️ Si se pierde el keystore, no se puede actualizar la app sin desinstalarla, y desinstalar **borra los datos**. Guardar copia del keystore en dos sitios seguros.

**README (en inglés):** capturas, diagrama de arquitectura, decisiones técnicas, cómo compilar y aviso legal.

---

## 15. Privacidad y aviso legal

- Sin permiso de Internet, sin analítica y sin cuentas. Los datos solo están en el móvil y en las copias que el usuario decida hacer.
- Texto del aviso, en Ajustes y en el primer arranque:
  > *«Mi Tacógrafo es una herramienta personal de apoyo. No sustituye al tacógrafo ni a sus registros oficiales y no tiene validez ante una inspección. Los cálculos son orientativos y dependen de los datos introducidos.»*

---

## 16. Decisiones abiertas

| ID | Pregunta | Propuesta |
|---|---|---|
| D-1 | ¿Qué zona horaria marca el «lunes 00:00»? | Zona de referencia configurable. Por defecto, la del país de la empresa. |
| D-2 | ¿A qué semana se asigna un semanal que cae entre dos semanas? | A la semana en que empieza, salvo que esa semana ya tenga uno. En ese caso, a la siguiente. Validar con la guía de la Comisión. |
| D-3 | ¿Qué parte de un descanso diario cuenta como compensación? | La que supera 11 h, unida a un descanso ≥ 9 h. Confirmar la interpretación. |
| D-4 | ¿Se registran pausas o solo descansos? | Ambos son opcionales. Las pausas solo sirven para mostrar información, porque sin tramos de conducción no se puede verificar R-PAUSA. |
| ~~D-5~~ | ~~`minSdk`~~ | ✅ **Resuelto:** el conductor tiene Android 13 (API 33) → **`minSdk = 26`** (Android 8.0, `java.time` nativo, sin *desugaring*). `targetSdk` y `compileSdk` = el último estable. Así cubre su móvil, móviles nuevos y casi cualquier móvil de un amigo. |
| D-6 | `applicationId` | Elegir antes del primer APK. No se puede cambiar después. |
| ~~D-7~~ | ~~¿Transporte internacional?~~ | ✅ **Resuelto:** habitualmente internacional, a veces nacional. Se soportan ambos (ver §7.7). |

---

## 17. Ampliaciones futuras

- **Lectura de ficheros `.DDD`** de la tarjeta del conductor (lector USB-C). Daría los tramos reales de conducción y eliminaría casi todos los «No verificado». El diseño con puertos permite añadirlo como otro adaptador de entrada.
- Widget en la pantalla de inicio con la jornada en curso.
- Art. 9 (ferri/tren) y art. 12 (excepciones) como reglas completas.
- Multi-manning (regla de 30 h).
- Tiempo de trabajo (Directiva 2002/15/CE).
- Gráficas de km y horas por mes.
- Publicación en Google Play (prueba cerrada).

---

## 18. Fuentes

- Reglamento (CE) 561/2006, texto consolidado: <https://eur-lex.europa.eu/legal-content/ES/TXT/?uri=CELEX:02006R0561-20240522>
- Reglamento (UE) 2020/1054 (Paquete de Movilidad I): <https://eur-lex.europa.eu/eli/reg/2020/1054/spa> · DOUE L 249/1: <https://boe.es/doue/2020/249/L00001-00016.pdf>
- Resumen de EUR-Lex sobre tiempos de conducción y descanso: <https://eur-lex.europa.eu/ES/legal-content/summary/driving-time-and-rest-periods-in-the-road-transport-sector.html>
- Texto de los arts. 6–9 tras la modificación de 2020 (versión a 20/08/2020): <https://www.legislation.gov.uk/eur/2006/561/chapter/II/2020-08-20>
- Reglamento (UE) 2024/1258, solo transporte ocasional de viajeros: <https://eurlexa.com/act/en/32024R1258/present/text>
- Pendiente de consultar: guías y notas interpretativas de la Comisión Europea sobre el Reglamento 561/2006 (para D-2 y D-3).
