# Checklist De Cambios

Usar esta lista antes de modificar ContaApp. La app mezcla UI, API, cache y widget; esta checklist evita olvidar una pieza lateral.

## Antes De Cambiar

- [ ] Entiendo si el cambio es solo UI, solo API, o afecta al estado global.
- [ ] He revisado `docs/DEVELOPMENT_GUIDE.md` para ubicar archivos.
- [ ] He comprobado si ya existe un componente reutilizable en `CommonComponents.kt`.
- [ ] He revisado `docs/CHANGELOG.md` si el cambio toca fechas, filtros, cache o contratos de backend.

## Modelos Y API

- [ ] Si agrego un campo, esta en `domain/Models.kt`.
- [ ] Si viene del backend, esta mapeado en `ContaRepository.kt`.
- [ ] Si puede faltar o venir como `null`, el mapper lo trata defensivamente.
- [ ] Si agrego un endpoint, el path no duplica una llamada ya existente.
- [ ] Si hay `dateTo`, he decidido si es inclusivo o exclusivo.
- [ ] Si la llamada requiere token, he considerado refresh token en `MainViewModel`.

## Estado Y Cache

- [ ] Si el dato es global, esta en `ContaUiState`.
- [ ] Si el dato entra en snapshot, esta en `FinancialSnapshot`.
- [ ] Si el dato se cachea, tiene serializacion y deserializacion en `SnapshotStore.kt`.
- [ ] Si cambia el formato cacheado y puede romper datos previos, he versionado la clave.
- [ ] Si afecta a filtros, he revisado `snapshotKey` y `cacheLabel`.
- [ ] Si afecta a Ajustes, `CacheStatusUi` sigue contando la verdad.

## UI

- [ ] Respeta modo privacidad con `money(value, hidden)` o equivalente.
- [ ] Tiene estado de carga, vacio y error si la pantalla lo necesita.
- [ ] No introduce formularios permanentes densos donde ya hay bottom sheet o chips.
- [ ] En movil, los textos cortos de tabs/botones no se truncaran facilmente.
- [ ] En tablet, no rompe `AdaptiveScreenFrame` ni navigation rail.
- [ ] Si abre detalles desde busqueda global, revisa `DetailTarget` y `MoreScreen`.

## Widget Y Notificaciones

- [ ] Si cambia el resumen financiero base, revisa `ContaWidgetProvider.kt`.
- [ ] Si cambia snapshot, el widget sigue pudiendo leerlo.
- [ ] Si se anaden alertas, revisa `AttentionNotifier.kt`.
- [ ] Si se toca Android 13+ notificaciones, recuerda permiso `POST_NOTIFICATIONS`.

## Seguridad Y Configuracion

- [ ] Tokens o credenciales solo pasan por `SessionStore`.
- [ ] No se guarda password.
- [ ] La URL base sigue normalizada por `ServerConfigStore`.
- [ ] Si cambia HTTP/HTTPS, revisar `AndroidManifest.xml` y `network_security_config.xml`.
- [ ] Si se toca biometria, validar flujo login -> bloqueo -> desbloqueo -> logout.

## Verificacion

- [ ] Ejecutar `.\gradlew.bat :app:compileDebugKotlin --console=plain`.
- [ ] Para cambios amplios, ejecutar `.\gradlew.bat :app:assembleDebug --console=plain`.
- [ ] Para widget, instalar/probar en emulador o dispositivo.
- [ ] Para API real, probar con backend ContMovApp levantado.
- [ ] Actualizar `docs/CHANGELOG.md` con fecha, tipo, resumen y verificacion.
