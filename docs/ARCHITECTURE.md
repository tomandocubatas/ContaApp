# ContaApp Android - Arquitectura

## Objetivo

Crear una app Android nativa para consultar el estado financiero personal usando las APIs existentes de `C:\Codigo\ContMovApp`.

La app movil no replica toda la aplicacion web. Su foco inicial es consulta rapida, elegante y segura: saldos, cuentas, movimientos, categorias, deuda y seguros. La IA queda fuera del MVP.

## Alcance MVP

Incluido:

- Login con usuario y password contra la API existente.
- Desbloqueo biometrico local despues del primer login correcto.
- Dashboard financiero.
- Bancos, cuentas y saldos.
- Listado y detalle de movimientos.
- Filtros de movimientos.
- Resumen por categorias.
- Deuda: prestamos y financiaciones en modo lectura.
- Seguros y proximos vencimientos en modo lectura.
- Alquileres en modo lectura: rentabilidad, cashflow, desglose mensual y propiedades con actividad.
- Graficas de evolucion economica basadas en los endpoints del dashboard existente.
- Modo privacidad para ocultar importes.

Excluido inicialmente:

- IA, insights, recomendaciones y anomalias.
- Alta/importacion/edicion/borrado de movimientos.
- Split de movimientos.
- Gestion de bancos, cuentas, categorias, presupuestos, documentos, automatizaciones y configuracion.
- Funciones admin, backup o mantenimiento.

## Backend Fuente

Proyecto fuente analizado: `C:\Codigo\ContMovApp`.

Stack detectado:

- Backend: Node.js, Express, TypeScript, MongoDB native driver.
- Frontend existente: Angular 21, Tailwind, ApexCharts.
- Autenticacion: JWT con access token y refresh token.

Base API:

- Desarrollo: `http://localhost:4000/api`
- Android emulator: `http://10.0.2.2:4000/api`
- Produccion temporal: HTTP permitido hasta que exista endpoint HTTPS definitivo.
- Decision vigente 2026-06-03: no perseguir HTTPS en este ciclo; la URL base configurable puede apuntar a HTTP en produccion provisional.
- La URL base es configurable desde la pantalla de login mediante un acceso flotante `API`.

Endpoints principales para MVP:

- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/dashboard/summary`
- `GET /api/dashboard/category-history`
- `GET /api/dashboard/debt-evolution`
- `GET /api/banks`
- `GET /api/accounts/balances`
- `GET /api/accounts/balance-evolution`
- `GET /api/movements-global`
- `GET /api/movements/:movementId`
- `GET /api/categories`
- `GET /api/loans/summary/global`
- `GET /api/loans/debt-detail`
- `GET /api/loans/debt-evolution`
- `GET /api/loans/composition`
- `GET /api/loans/comparison`
- `GET /api/financings/summary`
- `GET /api/financings/debt-detail`
- `GET /api/financings/evolution/all`
- `GET /api/insurances/summary`
- `GET /api/insurances/renewal-calendar`
- `GET /api/insurances/expiring-soon`
- `GET /api/rentals/summary`
- `GET /api/rentals/properties-breakdown`
- `GET /api/rentals/cashflow`
- `GET /api/rentals/expenses-by-category`
- `GET /api/rentals/year-comparison`
- `GET /api/rentals/seasonal-analysis`
- `GET /api/rentals/top-properties`
- `GET /api/properties`
- `GET /api/properties/:id/monthly-summary`
- `GET /api/properties/:id/month-details`
- `GET /api/properties/:id/category-evolution`

## Arquitectura Android

Decision principal: app Android nativa en Kotlin con Jetpack Compose.

Capas:

- UI: Jetpack Compose, Material 3, navegacion por pantallas.
- UI actual separada por archivos: `AppRoot`, `Navigation`, `AuthScreens`, `HomeScreen`, `MovementsScreen`, `AccountsScreen`, `DebtScreen`, `InsuranceScreen`, `RentalsScreen`, `ChartsScreen`, `MoreScreen` y `CommonComponents`.
- Presentation: ViewModels con estado observable.
- Domain: modelos y casos de uso ligeros solo cuando reduzcan complejidad real.
- Data: repositories que combinan API remota y cache local.
- Network actual: OkHttp + `org.json` nativo para el primer corte compilable.
- Network objetivo: Retrofit + parser tipado cuando se estabilicen contratos.
- Local objetivo: Room para cache de datos consultables.
- Local actual: cache ligera en SharedPreferences para restaurar el ultimo snapshot financiero si la API no responde.
- Preferences/security actual: SharedPreferences con valores cifrados por Android Keystore.
- Preferences/security objetivo: mantener cifrado Keystore; evaluar DataStore solo para preferencias no sensibles.
- DI objetivo: Hilt cuando el volumen de dependencias lo justifique.

Principios:

- La UI no llama directamente a Retrofit.
- Los ViewModels dependen de repositories, no de clientes HTTP.
- Los DTOs de API se mapean a modelos de dominio/UI.
- Cache offline primero para lecturas clave cuando sea razonable.
- Las escrituras de negocio quedan fuera del MVP.
- No introducir dependencias nuevas sin comprobar que aportan valor y que el proyecto sigue compilando.
- La navegacion movil debe mantenerse con pocas entradas principales; se usa `Mas` para secciones secundarias. En tablet se permite rail con todas las secciones.
- Las pantallas de graficas y alquileres reutilizan `MovementFilters` como contrato simple de periodo (`dateFrom`/`dateTo`) para evitar duplicar estado de filtros.
- La grafica financiera principal debe usar `dashboard/summary.series.current`, porque es el contrato real que usa el dashboard web para ingresos/gastos/neto por dia o mes. `dashboard/category-history` queda para graficas historicas por categoria.
- El campo `range.granularity` de `dashboard/summary` indica si el eje X representa dias o meses.
- La seccion movil `Graficas` debe mantener paridad funcional con las cinco familias del dashboard web: evolucion financiera, distribucion rapida, categorias del periodo, top categorias e historico por categorias.
- Los controles de periodo deben reutilizar el componente premium de chips para mantener consistencia entre Home, Graficas y futuras pantallas.
- Los estados de carga y vacio deben evitar spinners aislados cuando haya contexto disponible; se prefieren skeletons y empty states con accion sugerida.
- Los detalles de entidades financieras deben mostrar badges de calidad del dato cuando aplique: sin cuenta, sin categoria, pendiente, vinculado o duplicado.

## Autenticacion Y Biometria

Flujo previsto:

1. Usuario introduce email y password.
2. La app llama a `POST /api/auth/login`.
3. Si el login es correcto, guarda tokens de forma segura.
4. En siguientes aperturas, antes de mostrar datos financieros, solicita huella/biometria.
5. Si la biometria es correcta, desbloquea la sesion local y usa/renueva tokens.
6. Si el refresh falla, vuelve a login con usuario/password.

Reglas importantes:

- La huella no sustituye al login del backend; solo desbloquea credenciales ya emitidas.
- No se guarda la password.
- Los tokens se guardan cifrados con AES/GCM usando una clave en Android Keystore.
- Nunca se muestran datos financieros antes de desbloquear.
- Debe existir opcion de cerrar sesion y borrar credenciales locales.

## Estado Actual De Implementacion

Primer corte implementado:

- Proyecto Gradle Android con modulo `app`.
- Kotlin + Jetpack Compose + Material 3.
- Login contra `POST /api/auth/login`.
- Sesion local cifrada con Android Keystore.
- Bloqueo biometrico tras existir sesion local.
- Pantallas: Inicio, Movimientos, Cuentas, Deuda y Seguros.
- Cliente API con OkHttp.
- Build debug verificado con `.\gradlew.bat :app:assembleDebug --console=plain`.
- Look and feel inicial: login coral, hero financiero, anillo de composicion, filas con acentos por tipo y barras de deuda.
- Seguros incluye resumen visual y proximas renovaciones desde `GET /api/insurances/expiring-soon`.
- Deuda muestra resumen agregado y detalle individual desde `GET /api/loans/debt-detail` y `GET /api/financings/debt-detail`.
- El login muestra el servidor activo y un acceso flotante `API` para cambiar/probar entorno sin mezclarlo con credenciales.

Pendiente importante:

- Validar payloads reales endpoint por endpoint contra backend levantado.
- Validar refresh token automatico en dispositivo/emulador contra backend real.
- Seguir mejorando estados vacios/errores por pantalla.
- Ampliar filtros de movimientos.
- Anadir cache offline.
- Sustituir mapeos defensivos por DTOs tipados cuando los contratos queden cerrados.

## Diseno

Direccion visual:

- Profesional, sobrio y premium, inspirado en apps financieras moviles modernas.
- Login con fondo coral y tarjeta blanca elevada.
- Iconografia local en vectores Android bajo `res/drawable`, evitando dependencias pesadas de iconos.
- Icono de aplicacion adaptive icon: fondo coral, marca bancaria blanca, acento teal y variante monocroma.
- Fondo general claro, tarjetas limpias, bordes discretos.
- Paleta base: coral, blanco, grafito, slate suave, teal para positivos y ambar para deuda/avisos.
- Importes grandes y legibles.
- Nada de pantallas recargadas: pocas metricas, muy bien jerarquizadas.
- Se prefieren visualizaciones nativas ligeras en Compose antes de meter librerias pesadas: anillos, barras y listas densas.
- Los filtros deben presentarse como chips compactos o acciones tipo pildora; evitar botones altos de aspecto formulario salvo acciones principales.
- Con seis secciones en movil, la navegacion inferior debe usar etiquetas cortas y no truncadas. Los titulos completos viven en la top bar.
- El anillo principal de Inicio representa actividad del periodo, no patrimonio total: ingresos/gastos deben seguir siendo legibles aunque la deuda sea grande.
- Las filas de listas financieras deben ser densas, con icono pequeno, texto principal claro e importe alineado a la derecha.
- Referencia visual aceptada por el usuario: Wallet/BudgetBakers. Traducir esa referencia como: tarjetas de lectura rapida, chips compactos, resumen financiero primero, formularios minimizados y estados vacios integrados como tarjetas.
- Las tarjetas de atencion que muestran importes deben reservar ancho suficiente; no usar cuadrados fijos para dinero porque recorta cifras en movil.
- En movil, acciones multiples dentro de una tarjeta deben apilarse full-width si no caben comodamente en una fila.
- Movimientos debe mantener filtros en bottom sheet y no como formulario permanente dentro de la lista.
- Las cabeceras de grupos de movimientos deben mostrar tambien importe neto del grupo para lectura rapida.
- Cuentas debe priorizar agrupacion por banco en tarjetas, no una lista plana de cuentas.
- Los iconos de filas deben ser vectoriales y semanticos; evitar letras como representacion principal salvo fallback inevitable.
- El placeholder de privacidad para dinero debe preservar el contexto monetario (`•••• €`) para evitar saltos visuales.
- Inicio debe funcionar como dashboard ejecutivo, no como listado de modulos: primero balance/neto/salud; despues pulso financiero, top gastos, alertas y actividad reciente.
- La salud financiera en Inicio es una metrica UI orientativa calculada localmente desde revision, categorizacion y avisos; no sustituye reglas de negocio del backend.
- En tablet, Inicio debe combinar bloques en dos columnas cuando aporte lectura; en movil debe priorizar orden vertical y jerarquia clara.
- Direccion visual vigente: `Executive Light Ledger` como base principal; usar dark premium solo en componentes puntuales si aporta contraste. Evitar volver a una UI coral dominante.
- Componentes comunes nuevos: `ExecutiveKpiCard`, `MiniSparkline`, `ExecutiveProgressCard`, `FinancialLineChart`.
- Los graficos ligeros actuales son UI nativa en Compose; cuando haya endpoints historicos reales se deben alimentar desde API, no inventar series.

## APIs Backend Revisadas

Proyecto fuente: `C:\Codigo\ContMovApp`.

Base registrada en backend: `app.use('/api', apiRouter)`.

Rutas principales detectadas:

- `GET /api/dashboard/summary`
- `GET /api/dashboard/category-history`
- `GET /api/dashboard/debt-evolution`
- `GET /api/dashboard/movements-by-type`
- `GET /api/accounts/stats`
- `GET /api/accounts/balances`
- `GET /api/accounts/balance-evolution`
- `GET /api/movements-global`
- `GET /api/movements`
- `GET /api/movements/summary`
- `GET /api/movements/summary-by-category`
- `GET /api/movements/by-insurance`
- `GET /api/categories`
- `GET /api/budgets`
- `POST /api/budgets/compare`
- `GET /api/loans/summary/global`
- `GET /api/loans/debt-evolution`
- `GET /api/loans/composition`
- `GET /api/loans/comparison`
- `GET /api/loans/debt-detail`
- `GET /api/financings/summary`
- `GET /api/financings/debt-detail`
- `GET /api/financings/evolution/all`
- `GET /api/recurring-patterns`
- `GET /api/recurring-patterns/stats`

Regla: para la app Android MVP priorizar endpoints de lectura. No consumir endpoints de creacion/edicion/importacion salvo que el usuario lo pida explicitamente.

Categorias: el backend expone `GET /api/categories` y el modelo soporta `color` e `icon`. La app Android carga categorias como `CategoryOption` para filtros por `categoryId`. Las filas de movimientos deben usar `CategoryOption.color` como acento visual y traducir `CategoryOption.icon` a la iconografia vectorial local disponible, manteniendo fallback por concepto cuando el icono backend sea emoji o no exista equivalente.

Filtros de movimientos: mantener busqueda de texto, categoria, cuenta, fecha desde/hasta, tipo, estado de revision, rango rapido e importe. Para `GET /api/movements-global`, la app debe enviar `categoryId` y `accountId` reales, no nombres visibles. Los movimientos tambien preservan `loanId`, `financingId`, `propertyId`, `insuranceId`, `parentMovementId`, `hasSplits` e `isDuplicate` para mostrar vinculaciones sin habilitar edicion.

Tipos de movimientos: `Ingresos` en la UI no equivale solo a `type=ingreso`; debe incluir `ingreso`, `transferencia_entrada` y `deposito`. `Gastos` debe incluir `gastos`, `transferencia_salida` y `retiro`. Si el backend no acepta listas de tipos, Android puede combinar varias llamadas y deduplicar por `id`.

Filtros locales de movimientos: los accesos `Vinculados` y `Duplicados` se aplican sobre la lista cacheada/recibida, porque no todos los backends exponen esos filtros como query params. Si se incorporan al backend, mantener la misma UX y mover la condicion a `MovementFilters`.

Accesos rapidos de movimientos: todos los accesos visibles deben comportarse como toggles. Un toque activa el filtro y otro toque lo desactiva sin obligar al usuario a abrir la hoja avanzada. Esta regla aplica a pendientes, gastos del mes, ingresos del mes, vinculados, duplicados y futuros accesos equivalentes.

Entidades caducadas: no mostrar en UI seguros expirados/cancelados/inactivos ni prestamos/financiaciones cerrados, pagados, cancelados, expirados, inactivos o sin capital pendiente. Una cuota vencida de una deuda activa no se considera entidad caducada y debe seguir apareciendo como aviso.

Periodo financiero: Home debe recargar `dashboard/summary` y `movements-global` con el mismo rango de fechas. No basta con cambiar el texto del periodo o filtrar solo actividad reciente, porque los KPIs principales deben representar la vista seleccionada.

Fechas inclusivas: para el usuario todos los selectores de periodo son inclusivos (`desde` y `hasta` incluyen el dia completo). El backend de movimientos/rangos usa habitualmente limite superior exclusivo (`date < dateTo`), por lo que `ContaRepository` debe transformar siempre `dateTo` a `dateTo + 1 dia` antes de enviarlo. No aplicar esta regla en cada pantalla; mantenerla centralizada para Home, Movimientos, Graficas y Alquileres.

Selector de periodo: `PremiumPeriodSelector` es el componente unico para Home y Graficas. Debe mantener una tarjeta compacta con hoja inferior y opciones rapidas compartidas en `premiumPeriodOptions()`: mes actual, mes pasado, trimestre actual, anio actual, anio pasado, ultimos 30 dias e historico. Si se agregan rangos, actualizar `QuickRange.toDates()`, `QuickRange.homeLabel()` y las etiquetas de filtros de Movimientos a la vez.

Persistencia de contexto: Home y Graficas guardan localmente el periodo seleccionado en `AppPreferencesStore`; Graficas guarda ademas la seccion activa. Esto es estado de experiencia, no regla de negocio. No debe invalidar cache por si mismo, solo provocar la carga normal del rango asociado cuando el usuario entra.

Cache y refresco: la app mantiene un snapshot financiero compartido para Inicio, Graficas, Cuentas, Deuda, Seguros y Alquileres. Ese snapshot incluye dashboard, categorias, cuentas, movimientos base, deuda, seguros, alquileres y graficas. En memoria, `loadHome` debe conservar entradas por `MovementFilters`, no solo el ultimo filtro usado; Inicio con mes actual, Graficas con un rango concreto y Alquileres con anio completo no deben pisarse entre si mientras el TTL siga vigente. La persistencia en `SnapshotStore` tambien debe guardar snapshots por clave de filtro/rango, ademas del snapshot general `home`, para reutilizar periodos ya consultados aunque la pantalla vuelva a montarse. Si el usuario vuelve a una pantalla con filtros ya cacheados dentro del TTL configurado, no se hace llamada de red. El refresco de cabecera debe llamar siempre a una carga forzada de la ultima vista y saltarse el TTL.

Estado de cache: Ajustes debe mostrar si el snapshot tiene varios rangos en memoria y Movimientos debe indicar que tipo de filtros hay activos. No hace falta exponer IDs internos de filtros; basta con que el usuario entienda si la cache es general, por fecha, por cuenta, por categoria, por tipo o por importe.

TTL: por defecto son 6 horas, porque la informacion financiera consultiva normalmente no cambia varias veces al dia. Preferencias historicas inferiores a 1 hora se normalizan a 6 horas para evitar configuraciones antiguas demasiado agresivas. Ajustes permite 1h, 3h, 6h y 12h.

Cache de Movimientos: Movimientos mantiene cache propia por `MovementFilters` porque busqueda, categoria, cuenta, fechas, importe y revision cambian con mas frecuencia que el snapshot general. El boton de cabecera limpia esa cache y fuerza recarga. Ajustes permite refrescar la entrada de Movimientos usando los filtros activos.

Visibilidad de cache: Ajustes debe mostrar por pantalla la hora de cacheo, tiempo restante de vigencia y accion de refresco selectivo. Aunque varias pantallas compartan el mismo snapshot, deben aparecer separadas para que el usuario entienda que estan cubiertas.

Compatibilidad de snapshots: los snapshots creados antes de guardar `loadedAtMillis` pueden contener datos validos pero no fecha de carga. En ese caso la UI debe mostrarlos como `Cache local` con `Fecha no disponible`, nunca como `Sin cache`, porque eso contradice que la app este mostrando datos.

Serializacion de cache: antes de persistir metricas numericas, cualquier `NaN` o infinito debe convertirse a `0.0`. `org.json.JSONObject` no acepta esos valores y puede cerrar la app si el guardado ocurre en una corrutina sin captura. `SnapshotStore.save` debe fallar suave; perder una escritura de cache es aceptable, crashear la app no.

Estado de datos: la cabecera debe mostrar una pista breve de frescura (`Act. HH:mm`) o `Cache local`. No usar banners persistentes para esto salvo error real, porque consumen demasiado espacio en movil.

Graficas de linea: toda grafica de linea debe mostrar contexto suficiente para ser auditable: etiquetas del eje X, escala/resumen del eje Y y valor inicial/final cuando exista. En graficas compactas, tocar la linea debe seleccionar el punto mas cercano y mostrar su etiqueta X y valor Y. Evitar graficas mudas donde el usuario no pueda saber que puntos o importes se estan pintando.

Centro de analisis: Graficas debe estar organizada por secciones navegables, no como scroll infinito. Secciones vigentes: Resumen, Categorias, Tendencias, Deuda y Alquileres. Cada seccion debe consumir datos reales del snapshot/API y mostrar estado vacio cuando no haya puntos suficientes.

Marca visual: la identidad de la app es `ic_brand_mark`, un simbolo vectorial financiero navy/teal/azul. Launcher, cabecera y rail deben usar la misma marca. Evitar volver al icono de banco generico como marca principal; los iconos bancarios quedan para cuentas o entidades.

Rendimiento de listas: toda lista potencialmente larga o filtrable debe usar claves estables en `LazyColumn.items` basadas en IDs backend con fallback determinista. Esto aplica especialmente a movimientos, deuda, seguros, cuentas y alquileres.

Acceso y bloqueo: Login y bloqueo biometrico deben usar una escena visual premium coherente con la marca: fondo navy, grafica decorativa, tarjeta clara y CTA teal. Evitar fondos coral planos o pantallas utilitarias sin identidad. El selector de servidor sigue visible desde login como accion secundaria.

Navegacion inicial:

- Inicio
- Movimientos
- Cuentas
- Deuda
- Seguros

Adaptacion por dispositivo:

- Movil: navegacion inferior compacta con icono y etiqueta.
- Tablet: rail lateral con marca, iconos y contenido centrado con ancho maximo para evitar pantallas estiradas. La pantalla `Mas` es solo movil; en tablet el rail ya expone la navegacion completa.
- Inicio usa layout adaptativo: movil en una columna; tablet con composicion/ranking en dos columnas y metricas en cuatro columnas.
- Inicio muestra bloque `Requiere atencion` con movimientos pendientes, deudas con aviso y seguros proximos.
- Inicio muestra tambien `Agenda financiera`, combinando proximos pagos de deuda y renovaciones de seguros. Es una vista de consulta local sobre snapshot, sin crear avisos remotos.
- Movimientos usan filtros remotos por texto, tipo, rango, fechas, revision, categoria, cuenta e importe.
- Movimientos debe mantener accesos rapidos para casos frecuentes: pendientes, gastos del mes, ingresos del mes, vinculados y duplicados.
- Movimientos se agrupan visualmente por fecha relativa para lectura rapida.
- Movimientos pueden agruparse por fecha, categoria o cuenta, y sus filas deben mostrar categoria, cuenta y vinculacion cuando el backend la proporcione.
- Los detalles emergentes de cuenta, movimiento, deuda, seguro y alquiler deben comportarse como fichas ejecutivas: cabecera con dato principal, chips de estado, metricas resumidas y actividad/contexto reciente. Movimiento debe destacar importe, estado, cuenta, categoria y vinculacion; deuda debe destacar pendiente, cuota, proximo pago y meses estimados; alquiler debe destacar cashflow anual, rentabilidad, mejor/peor mes y categorias principales por mes. Evitar volver a dialogos planos compuestos solo de pares etiqueta/valor.
- Cuentas, Deuda y Seguros usan filtros locales instantaneos sobre el snapshot cargado.
- `Mas` en movil funciona como hub ejecutivo compacto y busqueda global local sobre cache. No debe mostrar importes con jerarquia excesiva ni duplicar la navegacion completa en tablet. Los resultados de busqueda deben transportar `targetId` y abrir el detalle concreto cuando exista: movimiento, cuenta, deuda, seguro o alquiler. Si no hay id, se permite caer solo en la pestana.
- Seguros debe cargar el inventario completo de polizas desde `GET /api/insurances/all`, con fallback a `GET /api/insurances/expiring-soon?days=60` si el endpoint completo no responde. La pantalla puede filtrar localmente por vencimiento a 30/60 dias, pero el estado base debe ser inventario de polizas, no solo renovaciones.
- Seguros debe enriquecer el snapshot con `GET /api/insurances/coverage-comparison` y `GET /api/insurances/renewal-calendar` cuando esten disponibles. Estos datos son de lectura y deben mostrarse como comparativa de coberturas y calendario, sin habilitar edicion.
- Seguros debe permitir abrir detalle de poliza. El detalle minimo es compania, tipo, prima anual/mensual, renovacion, estado e identificador. Si el backend expone `policyNumber`, `coverage`, `status` o `periodicity`, Android debe conservarlos en snapshot y mostrarlos en la ficha sin habilitar edicion.
- Alquileres debe permitir abrir detalle de propiedad usando el snapshot disponible. Si existe endpoint especifico por propiedad/mes, usarlo para enriquecer la ficha, pero sin bloquear la vista basica.
- Cuentas debe mantener un selector desplegable de banco para busqueda rapida por entidad, ademas de texto libre y filtro de saldo.
- En Cuentas, si existe combo de banco, el buscador de texto debe centrarse en cuenta/alias y no duplicar la busqueda por entidad bancaria.

Modo oscuro:

- Ninguna pantalla nueva debe usar `Color.White` o colores de texto fijos para superficies principales salvo que sea una decision visual aislada y comprobada en ambos temas.
- Las tarjetas y barras de navegacion deben usar `MaterialTheme.colorScheme.surface` o `surfaceVariant`; textos principales `onSurface`; textos secundarios `onSurfaceVariant`.
- La paleta dark oficial es azul noche con superficies elevadas y acentos teal/azul/coral. Mantener contraste alto y evitar mezclar tarjetas blancas dentro de fondos oscuros.
- La tipografia de la app se fuerza a sans-serif en `ContaTheme` para mantener tono financiero profesional aunque el dispositivo tenga una fuente del sistema personalizada.
- Los colores de categorias deben parsearse con `android.graphics.Color.parseColor` y envolverse en `runCatching`; no usar constructores internos de Compose con `ULong` para valores hex backend.
- Cuentas se agrupan por banco; Deuda destaca proximo pago; Seguros muestra coste mensual equivalente.
- Ajustes centraliza modo oscuro, privacidad por defecto, refresco, exportacion CSV, cache y sesion.
- Ajustes debe explicar el modo offline/snapshot: TTL vigente, cache disponible y refresco forzado desde cabecera.
- Exportacion actual: CSV local en external files de la app.
- Notificaciones locales: canal `conta_attention`, permiso `POST_NOTIFICATIONS` solicitado en Android 13+ y accion manual desde Ajustes.
- Widget Android: `ContaWidgetProvider` muestra balance, movimientos pendientes y avisos de deuda desde el ultimo snapshot.

IA no aparece en la navegacion inicial.

## Decisiones Que Deben Mantenerse Presentes

- La app movil es de consulta, no de administracion.
- El backend existente es la fuente de verdad.
- La web sigue siendo el lugar para operaciones complejas.
- Evitar duplicar reglas de negocio del backend en Android.
- Priorizar seguridad y claridad sobre cantidad de funciones.
- Mantener documentadas las decisiones en este archivo.
- Mantener la bitacora en `docs/CHANGELOG.md` con cada cambio relevante.
- Mantener archivos de UI por responsabilidad; evitar volver a concentrar pantallas completas en `AppRoot.kt`.
