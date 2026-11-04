# Mi Tacógrafo

> 🚧 Work in progress

Offline Android app that helps a professional truck driver replace his paper notebook: log work days, mileage, driving time and rest periods, get reminders, and check them against **EU Regulation (EC) 561/2006** on driving times and rest periods.

## Highlights

- **Kotlin + Jetpack Compose + Material 3**
- **Hexagonal architecture** in Gradle modules: a pure-Kotlin domain with the 561/2006 rules engine, separate from the Android adapters
- **Room (SQLite)**, fully offline. The app requests **no INTERNET permission**
- Rules engine that never assumes missing data: each rule reports *Compliant*, *Exceeded* or **Not verified**
- Timezone-safe: every record is stored as `Instant` plus `ZoneId`, so DST changes and international trips are handled correctly
- Spanish 🇪🇸 and Ukrainian 🇺🇦 UI
- Exact-alarm reminders, JSON backup and restore, CSV and PDF export

## Documentation

- [Technical specification (ES)](docs/ESPECIFICACION.md)
- [Kanban plan (ES)](docs/KANBAN.md)

## Disclaimer

This is a personal support tool. It does **not** replace the tachograph or its official records and has no legal value during roadside inspections.

## License

[MIT](LICENSE)
