# Guia De Desarrollo

Esta guia resume como moverse por ContaApp sin tener que redescubrir el proyecto en cada cambio.

## Que Hace La App

ContaApp es una app Android de lectura financiera conectada al backend ContMovApp. No pretende sustituir a la web completa: prioriza consulta movil, seguridad local, cache, privacidad visual y un widget de resumen.

Funciones visibles:

- Login contra `auth/login`.
- Desbloqueo local con biometria cuando ya hay sesion guardada.
- Dashboard de inicio con KPIs financieros, salud, agenda y actividad.
- Movimientos con busqueda, filtros, agrupacion y export CSV.
- Cuentas agrupadas por banco.
- Graficas de evolucion, categorias, deuda, alquileres y saldos.
- Deuda: prestamos y financiaciones en lectura.
- Seguros: polizas, vencimientos, coberturas y calendario.
- Alquileres: resumen anual, propiedades, meses y detalle.
- Ajustes: privacidad, tema, cache, export, notificacion local y logout.
- Widget Android alimentado por el ultimo snapshot financiero.

## Mapa De Archivos

| Area | Archivo | Responsabilidad |
| --- | --- | --- |
| Entrada Android | `MainActivity.kt` | Crea `MainViewModel`, inicializa biometria y monta `AppRoot`. |
| Estado global | `MainViewModel.kt` | Sesion, estado UI, cargas, cache en memoria, refresh token, export CSV. |
| API y mapeos | `data/ContaRepository.kt` | Endpoints, queries, mapeo JSON defensivo a modelos de dominio. |
| Modelos | `domain/Models.kt` | Contratos internos que consume UI, cache y widget. |
| HTTP | `core/network/ApiClient.kt` | OkHttp, base URL, headers, errores HTTP. |
| Sesion segura | `core/session/SessionStore.kt` | Tokens cifrados con Android Keystore. |
| Biometria | `core/session/BiometricGate.kt` | Dialogo biometric prompt. |
| Cache offline | `core/cache/SnapshotStore.kt` | Serializacion manual de snapshots a SharedPreferences. |
| Preferencias | `core/settings/*Store.kt` | URL base, privacidad por defecto, tema y TTL cache. |
| Navegacion | `ui/screens/AppRoot.kt`, `Navigation.kt` | Login/locked/app, tabs movil/tablet y foco de detalle. |
| Pantallas | `ui/screens/*Screen.kt` | UI Compose por seccion. |
| Componentes | `ui/screens/CommonComponents.kt` | Cards, chips, graficas ligeras, estados comunes. |
| Formato | `ui/screens/Formatters.kt` | Formato de dinero y porcentajes. |
| Widget | `widget/ContaWidgetProvider.kt` | Render nativo RemoteViews desde snapshot local. |

## Flujo De Datos

1. La pantalla llama a un callback recibido desde `MainScaffold` o `AppRoot`.
2. El callback acaba en `MainViewModel`.
3. `MainViewModel` lee sesion desde `SessionStore` y llama a `ContaRepository`.
4. `ContaRepository` hace peticiones con `ApiClient` y transforma JSON en modelos de `domain`.
5. `MainViewModel` actualiza `ContaUiState`.
6. Compose recompone la pantalla.
7. En cargas principales, `MainViewModel` guarda `FinancialSnapshot` en `SnapshotStore`.
8. El widget lee ese snapshot y se refresca con `ContaWidgetProvider.updateAll`.

## Estado Global

`ContaUiState` es el contrato principal entre capa de presentacion y UI. Antes de agregar datos nuevos, decidir si el dato es:

- Global y compartido por varias pantallas: agregar a `ContaUiState`, `HomePayload`, `FinancialSnapshot` y posiblemente widget.
- Local de una sola pantalla: preferir `remember` dentro de la pantalla si no requiere cache ni API.
- Preferencia persistente: agregar a `AppPreferencesStore` o `ServerConfigStore`.
- Secreto o sesion: agregar a `SessionStore` solo si debe estar cifrado.

## Endpoints Usados

Autenticacion:

- `POST auth/login`
- `POST auth/refresh`
- `GET health`

Inicio, categorias y graficas:

- `GET dashboard/summary`
- `GET dashboard/category-history`
- `GET dashboard/debt-evolution`
- `GET categories`

Cuentas y movimientos:

- `GET accounts/balances`
- `GET accounts/balance-evolution`
- `GET movements-global`

Deuda:

- `GET loans/summary/global`
- `GET loans/debt-detail`
- `GET financings/summary`
- `GET financings/debt-detail`

Seguros:

- `GET insurances/summary`
- `GET insurances/all`
- `GET insurances/{id}`
- `GET insurances/expiring-soon`
- `GET insurances/coverage-comparison`
- `GET insurances/renewal-calendar`

Alquileres:

- `GET rentals/summary`
- `GET rentals/properties-breakdown`
- `GET rentals/cashflow`
- `GET rentals/expenses-by-category`
- `GET properties/{id}/monthly-summary`

## Regla Importante De Fechas

La app maneja rangos visualmente inclusivos, pero no todos los endpoints interpretan igual `dateTo`.

- `movements-global`: usa `dateTo` exclusivo. Android suma un dia con `exclusiveDateTo`.
- `dashboard/summary`, `dashboard/category-history` y rentals: usan fecha final inclusiva desde Android.

Antes de cambiar filtros de fecha, revisar `exclusiveDateQuery`, `inclusiveDateQuery` y la bitacora de `docs/CHANGELOG.md`.

## Recetas De Cambio

### Agregar Un Campo Que Viene De La API

1. Agregar el campo en `domain/Models.kt`.
2. Mapearlo en `ContaRepository.kt`.
3. Si se cachea, agregarlo en `SnapshotStore.kt` en `toJson` y `toSnapshot`.
4. Si lo usa widget, adaptar `ContaWidgetProvider.kt`.
5. Usarlo en la pantalla correspondiente.
6. Compilar con `.\gradlew.bat :app:compileDebugKotlin --console=plain`.

### Agregar Una Pantalla Principal

1. Crear `ui/screens/NewScreen.kt`.
2. Agregar entrada en `Tab` dentro de `Navigation.kt`.
3. Decidir si vive en bottom bar, rail de tablet o dentro de `More`.
4. Pasar callbacks desde `MainScaffold`.
5. Agregar estado/cargas en `MainViewModel` solo si son compartidas o persistentes.

### Agregar Un Endpoint

1. Crear metodo en `ContaRepository`.
2. Mantener mapeo defensivo mientras el contrato backend no este cerrado.
3. Si hay fecha final, decidir inclusiva/exclusiva y documentarlo.
4. Llamarlo desde `MainViewModel`, con refresh token si es una carga protegida.
5. Definir comportamiento de error: bloquear pantalla, degradar a cache o ignorar opcionalmente.

### Cambiar El Dashboard De Inicio

1. Revisar `HomeScreen.kt`.
2. Revisar si el dato viene de `DashboardSummary`, `Movement`, `DebtDetail`, `InsurancePolicy` o `RentalSummary`.
3. Mantener modo privacidad usando `money(value, hidden)`.
4. Mantener estados vacios y skeletons.
5. Si cambia la informacion ejecutiva base, revisar tambien el widget.

### Cambiar Movimientos

1. Revisar `MovementFilters` en `ContaRepository.kt`.
2. Revisar `MovementsScreen.kt`.
3. Si el filtro se debe cachear, comprobar `MovementFilters.snapshotKey()` y `MovementFilters.cacheLabel()`.
4. Si se exporta a CSV, revisar `exportMovementsCsv`.

### Cambiar Cache

1. Revisar `FinancialSnapshot`.
2. Revisar todos los `toJson`/`toX` de `SnapshotStore`.
3. Si el formato anterior puede contaminar datos, versionar la clave del snapshot.
4. Revisar `buildCacheStatus` para que Ajustes muestre informacion honesta.

## Riesgos Tecnicos Actuales

- `MainViewModel` concentra demasiada responsabilidad. Es funcional, pero cada nueva seccion aumenta el coste de cambio.
- `ContaRepository` contiene muchos mapeos manuales y defensivos. Es practico para contratos inestables, pero facil de duplicar.
- `SnapshotStore` serializa manualmente todos los modelos. Cada campo nuevo exige mantenimiento doble.
- No hay tests automatizados en el repo. La compilacion es la verificacion minima.
- `usesCleartextTraffic=true` esta permitido por decision temporal. Revisarlo antes de distribucion publica.
- Las llamadas de carga principal son secuenciales dentro de `loadHomePayload`; si el backend crece, puede sentirse lento.

## Refactors Buenos Para Mas Adelante

No son imprescindibles para cambios pequenos, pero conviene tenerlos localizados:

- Separar `MainViewModel` en viewmodels o coordinadores por dominio cuando una seccion reciba escrituras.
- Introducir DTOs tipados o kotlinx.serialization cuando los contratos de API esten estables.
- Extraer un helper comun de refresh token para no repetir `recoverCatching` por carga.
- Convertir `SnapshotStore` a serializacion tipada para reducir errores de mantenimiento.
- Agregar tests JVM para mapeos de `ContaRepository` y serializacion de `SnapshotStore`.
- Paralelizar cargas independientes de `loadHomePayload` con coroutines estructuradas si el tiempo de carga lo pide.

## Verificacion Recomendada

Para cambios de UI o logica Kotlin:

```powershell
.\gradlew.bat :app:compileDebugKotlin --console=plain
```

Para validacion mas completa:

```powershell
.\gradlew.bat :app:assembleDebug --console=plain
```

Si el cambio toca login, biometria, permisos, widget o render visual, probar tambien en emulador/dispositivo.
