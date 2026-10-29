# ContaApp

App Android nativa para consultar el estado financiero personal usando la API de ContMovApp.

El foco actual es consulta rapida y segura: login, desbloqueo biometrico, dashboard, movimientos, cuentas, deuda, seguros, alquileres, graficas, cache local y widget Android.

## Stack

- Kotlin
- Jetpack Compose + Material 3
- Android Gradle Plugin
- OkHttp
- `org.json` para mapeos defensivos
- SharedPreferences para preferencias/cache
- Android Keystore + AES/GCM para tokens

## Arranque Rapido

Requisitos:

- JDK 17
- Android SDK
- Backend ContMovApp disponible en `http://localhost:4000/api` para desktop o `http://10.0.2.2:4000/api/` desde emulador Android.

Comandos habituales:

```powershell
.\gradlew.bat :app:assembleDebug --console=plain
.\gradlew.bat :app:compileDebugKotlin --console=plain
```

La URL base por defecto se define en [app/build.gradle.kts](app/build.gradle.kts) con `BuildConfig.API_BASE_URL`, y tambien se puede cambiar desde el acceso `API` del login.

## Documentacion

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md): decisiones historicas, alcance y arquitectura original.
- [docs/DEVELOPMENT_GUIDE.md](docs/DEVELOPMENT_GUIDE.md): mapa operativo del codigo y recetas para cambios.
- [docs/CHANGE_CHECKLIST.md](docs/CHANGE_CHECKLIST.md): checklist antes de tocar pantallas, modelos, API, cache o widget.
- [docs/CHANGELOG.md](docs/CHANGELOG.md): bitacora viva de cambios.

## Estructura Principal

```text
app/src/main/java/com/contaapp/android/
  MainActivity.kt
  MainViewModel.kt
  data/ContaRepository.kt
  domain/Models.kt
  core/
    cache/SnapshotStore.kt
    network/ApiClient.kt
    session/SessionStore.kt
    settings/
    notifications/
  ui/screens/
  ui/theme/
  widget/ContaWidgetProvider.kt
```

## Flujo Mental

```text
Compose screen
  -> callback a MainViewModel
  -> ContaRepository
  -> ApiClient
  -> API ContMovApp
  -> mapeo a domain models
  -> ContaUiState
  -> Compose screen
```

Los datos financieros consultados tambien pueden persistirse en `SnapshotStore` para uso offline y para alimentar el widget.
