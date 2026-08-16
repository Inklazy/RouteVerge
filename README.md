# RouteVerge

RouteVerge is an Android utility for local location simulation, route planning, route persistence, and route playback in authorized testing environments.

## Intended Use

This project is provided for lawful and authorized scenarios only, such as:

- Android location feature development and debugging
- Map SDK integration tests
- Route planning and playback experiments
- Personal learning, research, and reproducible test data

RouteVerge is not affiliated with, endorsed by, or authorized by any school, employer, attendance system, sports platform, campus service, payment platform, or other third-party service.

## Compliance Notice

Do not use this project to misrepresent location, bypass attendance or assessment rules, falsify activity records, abuse third-party platforms, violate user agreements, disrupt services, or infringe the rights of others.

The application only provides local device-side tools. It does not provide third-party account login, data upload, platform API integration, cracking, verification bypass, or remote data modification features. Users are solely responsible for complying with applicable laws, institutional policies, and third-party service terms.

## Configuration

Copy `local.properties.example` to `local.properties` and provide your own SDK path and map keys:

```properties
sdk.dir=C:\\Android\\Sdk
AMAP_API_KEY=replace-with-your-amap-key
GOOGLE_MAPS_API_KEY=replace-with-your-google-maps-key
```

Optional activation-related values can be supplied through `gradle.properties` or Gradle properties at build time:

```properties
activationBaseUrl=https://your-worker.example.workers.dev
telegramBotUsername=your_bot_username
```

Never commit real API keys, signing keys, APK files, generated build outputs, or `local.properties`.

## Build

```powershell
.\\gradlew.bat testDebugUnitTest assembleDebug
```

For release builds, create and store your own Android signing key outside the repository. Future app updates must be signed with the same key.

## Releases

Current stable release: **RouteVerge 2.0.0** (`versionName` 2.0.0, `versionCode` 20, tag `v2.0.0`).

Signed release APKs are published on the [Releases page](https://github.com/ZekTy/qunide-campus-run/releases). The APK filename is `CampusRunnerNfc-release.apk`; the in-app update check reads the latest GitHub release and compares the tag version against the installed `versionName`.

## License

GPL-3.0. See `LICENSE`.
