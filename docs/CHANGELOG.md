# Bitacora De Cambios

Registro vivo de decisiones y cambios del proyecto Android ContaApp.

Formato:

```text
YYYY-MM-DD - Tipo - Resumen
- Detalle breve.
- Archivos afectados si aplica.
```

## 2026-10-29 - UX/Estados - Banner de error con Reintentar (v0.18.0)

- Nuevo `ErrorBanner` bajo la barra superior de `MainScaffold`: titulo "No se pudo actualizar", mensaje, boton "Reintentar" de 48 dp que relanza la carga y region viva para lectores de pantalla. Sustituye a los `ErrorText` sueltos de Inicio, Agenda y Ajustes (el de las pantallas de acceso se mantiene).
- Los esqueletos de carga y los estados vacios ya existentes se conservan.
- Verificacion: `:app:assembleDebug` y tests correctos; en el movil, con wifi y datos apagados por adb (no modo avion), aparece el banner con "Mostrando ultimos datos guardados" y "Reintentar"; al restaurar la red y pulsar Reintentar desaparece y la cabecera pasa a "Act. 13:52".
- Pendiente: el detalle de la excepcion llega en ingles tecnico ("Unable to resolve host...") desde la capa de red; conviene traducirlo a un mensaje de usuario.

## 2026-10-29 - UX/Seguros y Deuda - Progreso y proxima fecha (v0.17.0)

- `InsuranceHeroCard` deriva su degradado del tema (antes colores fijos oscuros), anade barra "X de Y al dia" (activos menos por renovar, del resumen de la API) y un bloque "Proxima renovacion" con poliza, fecha y dias restantes (`List<InsurancePolicy>.nextRenewal()`).
- `DebtDetailRow` explica su barra con "Cuota X · Y% del pendiente".
- Hallazgo (backend manda): el contador "Por renovar" del resumen de la API (1) no coincide con las tarjetas de 30/60 dias (0) y Vencidas (2), que se calculan en la app con otra ventana. `DebtDetail` no incluye lo pagado, asi que no hay barra pagado/pendiente por fila.
- Verificacion: `:app:assembleDebug` y tests correctos; Seguros y Deuda revisados en el movil.

## 2026-10-29 - UX/Navegacion - NavigationBar de Material en movil (v0.16.0)

- `ContaBottomBar` sustituye la barra hecha a mano por `NavigationBar`/`NavigationBarItem` de Material: Inicio, Movs, Cuentas, Grafs y Mas, etiqueta siempre visible, indicador de seleccion y estado accesible. "Mas" queda activo cuando el destino actual cuelga de el (Agenda, Deuda, Seguros...).
- `NavigationRail` (>= 720 dp) usa los mismos colores de seleccion; los iconos pierden la descripcion duplicada porque la etiqueta ya los nombra.
- Verificacion: `:app:assembleDebug` y tests correctos; en el movil Inicio queda activo en Inicio y Mas en Agenda. El rail de tablet solo se comprobo por codigo y compilacion.

## 2026-10-29 - UX/Graficos - Graficos interactivos y accesibles (v0.15.0)

- Las graficas de linea se pueden arrastrar (ademas de tocar) para leer el valor de cada punto, y el lienzo lleva `contentDescription` con punto, valor y rango.
- `CategoryDistributionBars`: cada categoria es tocable y muestra "N mov. · X% del total"; lleva numero de orden y descripcion accesible.
- Paleta: sin cambios; la lectura no depende del color (numeros de orden, porcentajes, signos de v0.10.0 y etiquetas de punto).
- Hallazgo pendiente: en "Neto" un valor negativo se pinta con el color de la serie (verde), no por signo.
- Verificacion: `:app:assembleDebug` y tests correctos; en el movil se ve el detalle de "Impuestos" (11,2 %) y el arrastre sobre Neto lleva el punto a 10-12 (754,37 €).

## 2026-10-29 - UX/Inicio - Panel de prioridades (v0.14.0)

- Inicio se ordena como: periodo, "Requiere atencion", saldo (hero), atajos y despues el detalle (metricas, comparativa, grafico, salud financiera, agenda, actividad reciente).
- "Requiere atencion" solo lista los avisos con algo que revisar (movimientos pendientes, deudas con aviso, seguros proximos) y cada uno abre su pantalla; sin avisos muestra "Sin avisos".
- Nuevo `HomeShortcuts` (Movimientos, Cuentas, Agenda, Graficas) con botones de 48 dp; `HomeScreen` recibe `onNavigate` desde `Navigation.kt` y `AttentionCard` admite clic.
- Verificacion: `:app:assembleDebug` y tests correctos; en el movil se ven 2 avisos, "Deudas con aviso" abre Deuda y el atajo Agenda abre Agenda.

## 2026-10-29 - UX/Iconos - Iconos por categoria (v0.13.0)

- Nuevos vectores `ic_cat_restaurant`, `ic_cat_cart`, `ic_cat_car`, `ic_cat_fuel`, `ic_cat_health`, `ic_cat_leisure`, `ic_cat_transfer`, `ic_cat_repeat` y `ic_cat_work`.
- `categoryIcon` elige primero por el nombre de la categoria (restaurantes, supermercado, gasolina, transportes, salud, ocio, traspasos/bizum, suscripciones, nomina); si nada coincide mantiene las reglas anteriores y el icono por defecto (recibo).
- Verificacion: `:app:assembleDebug` y tests correctos; en el movil se ven los iconos de restaurante, gasolina, coche, transferencia y casa, y el recibo por defecto en "Otros gastos". Tambien se confirma el singular "1 filtro activo" de v0.11.0.

## 2026-10-29 - UX/Listas - Agrupacion por dia con cabeceras pegajosas (v0.12.0)

- Nuevo `DayHeader` (con `stickyHeader`, fondo opaco): etiqueta del dia ("Hoy", "Ayer", "lun 27 oct"), numero de elementos y total con signo y color.
- Movimientos agrupa por dia (antes "Ultimos 7 dias", "Este mes"...); las agrupaciones por categoria y cuenta usan la misma cabecera. Se eliminan `MovementGroupHeader` y `movementGroupLabel`.
- Agenda agrupa "Proximos eventos" por dia (total = ingresos - gastos) y ya no recorta a 12 eventos.
- Verificacion: `:app:assembleDebug` y tests correctos; en el movil las cabeceras quedan fijas al desplazar en Movimientos y Agenda.

## 2026-10-29 - UX/Filtros - Chips de filtros activos en Movimientos (v0.11.0)

- `ActiveFilterBar` muestra un `InputChip` por filtro activo (texto, tipo, revision, periodo, categoria, cuenta, fechas, importe, agrupacion, vinculados, duplicados) con "x" para quitarlo sin abrir la hoja, el contador "N filtros activos" y "Limpiar todo".
- Nuevo `ActiveFilter(label, onRemove)`; `movementFilterLabels` (sin uso) se elimina y `QuickRange.activeLabel()` aporta la etiqueta del periodo.
- Verificacion: `:app:assembleDebug` y tests correctos; en el movil el chip Mes, anadir Gastos (29 mov.), quitar Gastos (37) y Limpiar todo ("Todos los datos"). El texto singular "1 filtro activo" no se vio en pantalla.

## 2026-10-29 - UX/Importes - Signo explicito y color por signo (v0.10.0)

- Nuevo `signedMoney` en `Formatters.kt`: antepone `+` a los importes positivos (el `-` ya lo pone el formato); con privacidad activa sigue `•••• €`. Se usa en las filas de movimientos, el detalle y la mini fila de Cuentas.
- Fix: en `WalletMovementRow` el importe tomaba el color de la categoria, de modo que un ingreso (p. ej. un reembolso) podia salir en rojo. Ahora el importe va en verde/rojo segun su signo; el icono conserva el color de categoria.
- Verificacion: `:app:assembleDebug` y tests correctos; en el movil un reembolso `+5,64 €` sale en verde junto a un gasto `-37,60 €` en rojo.

## 2026-10-29 - UX/Tema - Hero de saldo derivado del tema (v0.9.0)

- `HomeHeroDashboard` (hero de Inicio) usa un degradado de `primaryContainer` a `secondaryContainer` y textos `onSurface`/`onSurfaceVariant`; desaparece la deteccion de modo oscuro por color de fondo y los degradados fijos. Gana el borde de tarjeta (pendiente de v0.7.0).
- `HeroBalance` (sin usos hoy) deja el degradado coral fijo y usa los mismos colores del tema.
- Verificacion: `:app:assembleDebug` y tests correctos; Inicio revisado en el movil en oscuro y en claro.

## 2026-10-29 - UX/Accesibilidad - Selector de periodo con botones accesibles (v0.8.0)

- El selector de mes de Agenda sustituye los textos `<` y `>` por flechas con `contentDescription` ("Mes anterior", "Mes siguiente") y altura minima de 48 dp.
- `PeriodHeader` (sin usos hoy) pasa a `IconButton` de 48 dp con "Periodo anterior/siguiente"; sin accion conserva el hueco para no mover el titulo.
- Verificacion: `:app:assembleDebug` y tests correctos; en el movil Agenda pasa de Octubre a Noviembre 2026 con la flecha siguiente.

## 2026-10-29 - UX/Tarjetas - Elevacion y borde unicos (v0.7.0)

- Nuevo `ContaCardDefaults` en `Dimens.kt`: elevacion de 1 dp y borde de 1 dp (`outline` al 70 %) para todas las tarjetas de contenido. En tema oscuro la sombra apenas se ve y las tarjetas se fundian con el fondo; el borde las separa.
- 58 elevaciones de 1, 2 y 16 dp de 12 pantallas pasan al criterio unico. Se dejan las tarjetas planas (elevacion 0) y la barra de navegacion inferior.
- Pendiente: la tarjeta hero de Inicio (degradado) no lleva borde.
- Verificacion: `:app:assembleDebug` y tests correctos; pantalla de huella, Inicio y Cuentas revisados en el movil en claro y oscuro.

## 2026-10-29 - UX/Tokens - Espaciado en todas las pantallas (v0.6.1)

- Los 257 `spacedBy` y `padding` de 4 a 28 dp de las 12 pantallas restantes pasan a `ContaSpacing`, con el mismo criterio que en `CommonComponents.kt` (escalon mas cercano, empates al menor).
- Verificacion: `:app:assembleDebug` y tests correctos; nueve pantallas revisadas en el movil sin cortes ni desbordes.

## 2026-10-29 - UX/Tokens - Escala de espaciado y radios (v0.6.0)

- Nuevo `ui/theme/Dimens.kt` con `ContaSpacing` (4/8/12/16/24) y `ContaRadius` (8/12/16/20/28).
- Los 147 radios sueltos de las 13 pantallas pasan a `ContaRadius`; los espaciados de `CommonComponents.kt` pasan a `ContaSpacing`, redondeando al escalon mas cercano (empates hacia el menor).
- Pendiente: usar `ContaSpacing` en el resto de pantallas.
- Verificacion: `:app:assembleDebug` y tests correctos; Inicio, Movimientos, Cuentas y Mas revisados en el movil.

## 2026-10-29 - UX/Tipografia - Jerarquia y cifras tabulares (v0.5.0)

- `AppTypography` define pesos y espaciado por nivel (titulares Bold con tracking cerrado, titulos SemiBold, cuerpo y etiquetas mas contenidos) y activa cifras tabulares (`tnum`) en todos los estilos.
- Nuevos estilos de importe en `Theme.kt`: `amountHero`, `amountLarge`, `amountMedium` y `amountSmall`; se aplican al saldo de Inicio, a las metricas comparativas, a las filas de Movimientos y al detalle del movimiento.
- Verificacion: `:app:assembleDebug` y tests correctos; Inicio y lista de Movimientos revisados en el movil.

## 2026-10-29 - UX/Tema - Acento coherente claro/oscuro (v0.4.0)

- El tema claro pasa a primario teal (`0F766E`) y secundario azul (`0D47A1`), como ya tenia el oscuro (menta `21D7A4` y azul `72B7FF`). Antes el primario era azul en claro y menta en oscuro.
- Botones, chips de filtro y tonos de contenedor salen teal en ambos temas, en linea con el logo y el login; el azul queda para informacion.
- Verificacion: `:app:assembleDebug` y tests correctos; Inicio y Movimientos revisados en el movil en claro y oscuro.

## 2026-10-29 - Fix/Sesion - Ajustes tras cerrar sesion (v0.3.2)

- `MainViewModel.logout()` reiniciaba todo el estado y perdia la URL del servidor, el modo oscuro, la privacidad por defecto y el TTL de cache. Ahora el estado se reconstruye con los valores guardados.
- La URL de "Servidor activo" en el login lleva color de tema explicito.
- Verificacion: `:app:assembleDebug` y tests correctos; login real revisado en el movil en modo oscuro. El flujo de cerrar sesion desde la app no se ha repetido tras el cambio.

## 2026-10-29 - Fix/UI - Login oscuro y doble spinner (v0.3.1)

- Login y pantalla de desbloqueo con huella: la tarjeta, los titulos, el icono del servidor y el estado de conexion siguen el tema; antes la tarjeta era blanca fija y los campos mostraban texto claro sobre blanco en modo oscuro.
- Inicio: se elimina el spinner a pantalla completa de `MainScaffold`, que coincidia con el indicador de `PullToRefreshBox`.
- Verificacion: `:app:assembleDebug` y tests correctos; desbloqueo revisado en el movil en claro y oscuro y refresco de Inicio con un solo indicador. El login propiamente dicho no se ha probado (exige cerrar sesion).

## 2026-10-29 - UX/Tema - Colores semanticos (v0.3.0)

- `ContaPalette` incorpora `income`, `expense`, `warning` y `debt` con variante clara y oscura; se leen con `ContaIncome`, `ContaExpense`, `ContaWarning` y `ContaDebt`.
- Importes y avisos de Inicio, Movimientos, Cuentas, Deuda, Seguros, Alquileres, Graficos, Agenda y Ajustes usan estos nombres en lugar de teal/coral/ambar.
- La deuda se distingue del aviso: naranja frente a ambar. En claro, aviso y gasto se oscurecen para mejorar el contraste del texto.
- Verificacion: `:app:assembleDebug` y tests correctos; Inicio, Movimientos y Deuda revisados en el movil en ambos temas.

## 2026-10-29 - UX/Tema - Paleta unica de colores (v0.2.0)

- Las constantes de color sueltas (`ContaCoral`, `ContaTeal`, `ExecutiveBlue`...) pasan a leerse de `ContaPalette`, con variante clara y oscura servida por `ContaTheme`.
- Colores de movimientos, deuda, barra de ingresos/gastos y avatar de cuentas salen de la paleta.
- Versionado: `build.gradle.kts` calcula `versionName`/`versionCode` desde major.minor.patch (fix = patch, mejora = minor, cambio incompatible = major) y el APK sale como `ContaApp-<version>-<tipo>.apk`.
- Verificacion: `:app:assembleDebug` y tests correctos; Inicio y Movimientos revisados en el movil en tema claro y oscuro.

## 2026-06-03 - UX/Widget - Layout P&L compacto

- Ingresos, gastos y neto pasan a una misma fila de tres mini metricas iconificadas para lectura tipo P&L.
- El texto del widget se compacta: frescura `HH:mm · Xh vig.`, `Top`, `Vence` y `Todo al dia`.
- El modo privacidad del widget usa `•••• €` para una ocultacion mas elegante y estable.
- Se reduce la altura minima del widget para que encaje mejor en tamanos medios sin perder el resumen ejecutivo.
- Verificacion: `:app:assembleDebug` correcto en Gradle offline. No se prueba emulador por indicacion del usuario.

## 2026-06-03 - UX/Widget - Iconografia semantica en widget

- Se incorporan iconos compactos para ingresos, gastos, top gasto, proximo vencimiento y avisos del widget.
- Se agregan fondos visuales teal, coral, ambar y azul para que las metricas se lean por color e icono, no solo por texto.
- Las tarjetas iconificadas del widget tambien son zonas tactiles para abrir la app.
- Verificacion: `:app:assembleDebug` correcto en Gradle offline. No se prueba emulador por indicacion del usuario.

## 2026-06-03 - UX/Widget - Widget ejecutivo con salud y TTL

- El widget Android pasa de resumen basico a panel ejecutivo: balance, ingresos, gastos, neto, salud financiera, mix ingresos/gastos, top gasto, proximo vencimiento y avisos priorizados.
- El widget respeta `Privacidad por defecto`: oculta importes con `**** €` sin romper el layout.
- Se muestra frescura de cache con `Act. HH:mm`, vigencia restante segun TTL o `cache vencida`.
- Deuda y seguros del widget filtran entidades pagadas, canceladas, expiradas o inactivas para no mostrar datos caducados.
- Se mejora la presentacion visual con logo propio, barra de salud, tarjetas compactas y preview basada en la marca.
- Verificacion: `:app:assembleDebug` correcto en Gradle offline. No se prueba emulador por indicacion del usuario.

## 2026-06-03 - Correccion/UX - Hoy en Movimientos y widget premium

- Movimientos arranca ahora en `Mes actual` en vez de `Todo`, forzando rango explicito desde el dia 1 hasta hoy y evitando que el listado global paginado o caches de filtros oculten movimientos del dia actual.
- Se mantiene la semantica exclusiva de `movements-global`: Android envia `dateTo + 1 dia` para que el backend con `$lt` incluya el dia final.
- Widget Android redisenado con look ejecutivo oscuro: balance, ingresos, gastos, neto, avisos y hora de snapshot.
- El widget abre la app al tocarlo y muestra estado `Sin cache` cuando todavia no existe snapshot financiero.
- El widget se refresca al guardar un nuevo snapshot financiero, ademas de su actualizacion periodica del sistema.
- Verificacion: `:app:assembleDebug` correcto en Gradle offline. No se prueba emulador por indicacion del usuario.

## 2026-06-03 - Correccion - DateTo inclusivo en dashboard y alquileres

- Se corrige el descuadre de `Mes pasado`: `dashboard/summary` usa `dateTo` inclusivo (`$lte`), por lo que Android deja de sumar un dia en ese endpoint.
- `dashboard/category-history` tambien pasa a recibir fechas inclusivas para evitar que las graficas por categoria arrastren datos del dia siguiente.
- Alquileres pasa a enviar `dateTo` inclusivo porque el backend de rentals ya transforma internamente el limite a `< dateTo + 1 dia`.
- `movements-global` mantiene `dateTo + 1 dia`, ya que ese repositorio usa limite superior exclusivo (`$lt`) y asi se incluyen los movimientos del dia final.
- Se versiona la clave persistida de snapshot financiero a `home-v2` para no reutilizar caches antiguas contaminadas por el rango incorrecto.
- Verificacion: `:app:assembleDebug` correcto en Gradle offline. No se prueba emulador por indicacion del usuario.

## 2026-06-03 - UX/Funcional - Busqueda directa, cache por rango y detalles ejecutivos

- La busqueda global de `Mas` abre ahora el detalle concreto cuando el resultado es movimiento, cuenta, deuda, seguro o alquiler, en vez de limitarse a cambiar de pestana.
- Se agrega destino interno de navegacion para consumir ids de busqueda y abrir modales de detalle en Movimientos, Cuentas, Deuda, Seguros y Alquileres.
- Movimientos mejora su ficha de detalle con cabecera ejecutiva, estado, categoria visual, cuenta resuelta por id y bloque de vinculacion/contexto.
- Deuda mejora su ficha de detalle con pendiente destacado, entidad, estado, cuota, meses estimados y proximo pago.
- Alquileres mejora la ficha de propiedad con cashflow anual, ingresos/gastos, rentabilidad, mejor/peor mes y categorias principales por mes.
- Graficas compactas permiten seleccionar el punto mas cercano tocando directamente sobre la linea, mostrando valor y eje X seleccionado.
- La cache persistida guarda snapshots por rango/filtro financiero ademas del snapshot general, para reutilizar periodos ya consultados dentro del TTL aunque se vuelva a la pantalla despues.
- Se agregan skeletons especificos en Movimientos, Cuentas, Deuda, Seguros y Alquileres cuando una pantalla esta cargando sin datos visibles.
- Se corrige el mapeo nulo del calendario de renovaciones de seguros para evitar error si el backend no devuelve lista de polizas.
- Verificacion: `:app:compileDebugKotlin --rerun-tasks` correcto en Gradle offline y `:app:assembleDebug` correcto. No se prueba emulador por indicacion del usuario.

## 2026-06-03 - UX/Funcional - Git inicial, caducados y refinamiento profesional

- Se amplia `.gitignore` para excluir `.kotlin/` y logs JVM `hs_err_pid*.log`/`replay_pid*.log` antes del primer commit.
- Se documenta HTTP como decision temporal valida tambien para produccion provisional hasta disponer de endpoint HTTPS definitivo.
- Deuda oculta prestamos/financiaciones cerrados, pagados, cancelados, expirados, inactivos o sin capital pendiente; las cuotas vencidas de deuda activa siguen visibles como aviso.
- Seguros oculta polizas expiradas, canceladas o inactivas en listado, agenda, busqueda global y avisos.
- Home deriva la KPI de deuda desde detalles activos cuando estan disponibles.
- Graficas muestran mas contexto auditable: numero de puntos y rango min/max en las graficas de linea internas y compartidas.
- Cache en Ajustes informa mejor el numero de rangos en memoria y el tipo de filtros activos en Movimientos.
- Se agregan claves estables en listas de movimientos, deuda y seguros para mejorar rendimiento y estabilidad visual al filtrar.
- Se amplia el diccionario de iconos backend para categorias de vivienda, banco, seguros, tarjetas, graficas y servicios.
- Se agrega descripcion semantica al selector premium de periodo para mejorar accesibilidad.
- Verificacion: `:app:assembleDebug` correcto en Gradle offline. No se prueba emulador por indicacion del usuario.

## 2026-06-03 - UX/Funcional - Detalles premium, seguros enriquecidos y graficas interactivas

- Seguros incorpora comparativa de coberturas desde `GET /insurances/coverage-comparison`.
- Seguros incorpora calendario de renovaciones desde `GET /insurances/renewal-calendar`.
- Snapshot/cache guarda coberturas y renovaciones de seguros para soporte offline.
- Detalle de cuenta se redisenia como ficha ejecutiva: cabecera visual, saldo destacado, chips de estado, resumen de ingresos/gastos/neto y actividad reciente.
- Detalle de seguro se redisenia con cabecera premium, prima anual, estado, renovacion y datos tecnicos.
- Graficas compactas agregan punto destacado seleccionable entre inicio, centro y fin.
- Se mantiene fuera de alcance la automatizacion de tests porque el usuario indica que ya la ha realizado.
- Verificacion: `:app:assembleDebug` correcto en Gradle offline. No se prueba emulador por indicacion del usuario.

## 2026-06-02 - UX/Funcional - Accesos reversibles y seguros completos

- Todos los accesos rapidos de Movimientos pasan a comportamiento reversible: un toque activa y otro toque desactiva `Pendientes`, `Gastos mes`, `Ingresos mes`, `Vinculados` y `Duplicados`.
- Seguros deja de depender solo de `expiring-soon` y carga el inventario completo desde `GET /insurances/all`, con fallback al endpoint de vencimientos si falla.
- La ficha de seguro conserva y muestra numero de poliza, cobertura, estado tecnico y periodicidad cuando el backend lo informa.
- La lista de Seguros cambia a lenguaje de polizas, hace visible la accion `Ver detalle` y mantiene filtros locales de vencimiento 30/60 dias.
- Archivos afectados: `ContaRepository.kt`, `MainViewModel.kt`, `Models.kt`, `SnapshotStore.kt`, `MovementsScreen.kt`, `InsuranceScreen.kt`.
- Verificacion: `:app:assembleDebug` correcto en Gradle offline. No se prueba emulador por indicacion del usuario.

## 2026-06-02 - UX/L&F - Acceso premium y detalle ejecutivo

- Login cambia a una escena premium con fondo navy, grafico decorativo, marca `ic_brand_mark`, tarjeta translúcida y chips de confianza.
- Pantalla de bloqueo biometrico adopta el mismo fondo visual y una tarjeta central con CTA `Desbloquear con huella`.
- Se eliminan los fondos coral dominantes del acceso para alinear la app con la identidad financiera navy/teal.
- Detalle de deuda agrega resumen ejecutivo con progreso, meses estimados restantes y cuota.
- Ajustes agrega tarjeta `Modo consulta offline` explicando snapshot, TTL y refresco forzado.
- Verificacion: `:app:assembleDebug` correcto en Gradle offline. No se prueba emulador por indicacion del usuario.

## 2026-06-02 - Correccion - Fecha final inclusiva y filtro de ingresos

- Se corrige el envio de `dateTo` al backend: la UI maneja rangos inclusivos, pero la API filtra con limite superior exclusivo, asi que Android envia `dateTo + 1 dia`.
- La regla se centraliza en `ContaRepository` para dashboard, movimientos, graficas y alquileres.
- Se aumenta el limite de movimientos a 200 para evitar que ingresos de alquiler queden fuera por paginacion corta.
- El filtro `Ingresos` en Movimientos pasa a grupo semantico: `ingreso`, `transferencia_entrada` y `deposito`.
- El filtro `Gastos` pasa a grupo semantico: `gastos`, `transferencia_salida` y `retiro`.
- Verificacion: `:app:assembleDebug` correcto en Gradle offline. No se prueba emulador por indicacion del usuario.

## 2026-06-02 - UX/Funcional - Filtros rapidos y agenda financiera

- Movimientos agrega accesos rapidos: pendientes, gastos del mes, ingresos del mes, vinculados y duplicados.
- Los filtros `Vinculados` y `Duplicados` se aplican localmente sobre la cache visible para no pedir endpoints inexistentes.
- Se agrega `Movement.hasBusinessLink()` como utilidad comun para detectar vinculaciones con prestamos, financiaciones, propiedades, seguros, splits o padres.
- Inicio agrega `Comparativa del periodo` usando la serie financiera real del snapshot para ingresos, gastos y neto.
- Inicio agrega `Agenda financiera`, combinando proximos pagos de deuda y renovaciones de seguros desde datos cacheados.
- Verificacion: `:app:assembleDebug` correcto en Gradle offline. No se prueba emulador por indicacion del usuario.

## 2026-06-02 - UX/L&F - Mas movil y busqueda global

- La pantalla `Mas` compacta el balance para evitar que el importe domine la tarjeta ejecutiva.
- Se sustituye el bloque de metricas grandes por chips compactos de pendientes, avisos y cache.
- `Mas` agrega busqueda global local sobre la cache: movimientos, cuentas, deuda, seguros y alquileres.
- En tablet, si por estado previo se entra en `Mas`, la navegacion vuelve a `Inicio`; tablet debe usar rail completo y no la pagina `Mas`.
- APK debug generado correctamente en `19:36` segun artefacto local. No se prueba emulador por indicacion del usuario.

## 2026-06-02 - UX/L&F - Hub profesional, logo y detalles financieros

- Se crea una marca vectorial premium `ic_brand_mark` con fondo navy, hexagono financiero, barras y acentos teal/azul.
- El launcher cambia a fondo navy y foreground/monochrome nuevos para abandonar el icono bancario coral generico.
- Cabecera y rail lateral usan la nueva marca para reforzar identidad visual.
- Graficas pasa a centro de analisis con secciones: Resumen, Categorias, Tendencias, Deuda y Alquileres.
- La seccion Deuda de Graficas muestra evolucion y un resumen de prestamos/financiaciones desde el snapshot.
- La seccion Alquileres de Graficas muestra cashflow y top de propiedades.
- Seguros convierte cada poliza en ficha tocable con detalle: compania, tipo, prima anual/mensual, renovacion, estado e identificador.
- Las filas de seguros agregan estado visual por urgencia y chips de tipo/renovacion.
- La pantalla Mas pasa a hub ejecutivo con balance, pendientes, avisos y accesos completos.
- Home y Graficas recuerdan el periodo elegido; Graficas recuerda tambien la seccion activa.
- El detalle de Cuenta incorpora actividad reciente usando movimientos cacheados.
- El detalle de Propiedad en Alquileres incorpora rentabilidad y meses activos disponibles.
- Verificacion: `:app:assembleDebug` correcto en Gradle offline. No se prueba emulador por indicacion del usuario.

## 2026-06-02 - Cache/Graficas - Alquileres sin recarga y ejes legibles

- `loadHome` deja de depender solo del ultimo filtro y mantiene cache en memoria por `MovementFilters`.
- Home, Graficas y Alquileres pueden conservar entradas distintas dentro del TTL; cambiar de Alquileres a Inicio ya no fuerza red si la cache de ese filtro sigue vigente.
- El refresco de cabecera conserva su comportamiento forzado y salta el TTL.
- Las graficas de linea comunes incorporan escala del eje Y, etiquetas de eje X/Y e importes compactos de referencia.
- Cashflow de Alquileres pasa a mostrar etiquetas del periodo y eje X de meses.
- Verificacion: `:app:assembleDebug` correcto en Gradle offline. No se prueba emulador por indicacion del usuario.

## 2026-06-02 - UX/L&F - Selector de periodo profesional y estado de datos

- El selector de periodo pasa de chips horizontales a una tarjeta premium tocable con hoja inferior compacta.
- Se agregan rangos rapidos utiles para analisis financiero: mes actual, mes pasado, trimestre actual, anio actual, anio pasado, ultimos 30 dias e historico.
- Home, Graficas y Movimientos reutilizan la misma lista de periodos para evitar divergencias visuales y funcionales.
- Movimientos muestra el numero de filtros activos directamente en el boton `Filtrar`.
- La cabecera muestra `Act. HH:mm` cuando los datos vienen de red y `Cache local` cuando se usa snapshot guardado.
- El cambio de periodo sigue recalculando filtros remotos via `MovementFilters.dateFrom/dateTo`.
- Build real verificado fuera del sandbox con Gradle 8.9 cacheado.

## 2026-06-02 - UX/L&F - Pulido de Graficas en movil

- La tarjeta `Evolucion financiera` cambia a encabezado apilado para evitar solapes de titulo/subtitulo en movil.
- Las series de Neto, Ingresos y Gastos se muestran apiladas y compactas; se elimina la doble columna estrecha que partia importes y etiquetas.
- Las graficas con un unico punto ya no dibujan una rejilla vacia gigante; muestran un resumen `Dato unico del periodo`.
- Se sustituyen encabezados largos de Distribucion, Categorias e Historico por `ChartCardHeader`, con subtitulo en segunda linea.
- Se verifica compilacion e instalacion en emulador sin entradas en `logcat -b crash`.

## 2026-06-02 - Cache - TTL 6h y visibilidad por pantalla

- El TTL por defecto pasa a 6 horas, alineado con que la informacion financiera normalmente no cambia varias veces al dia.
- Preferencias antiguas inferiores a 1 hora se normalizan automaticamente a 6 horas.
- Ajustes muestra el estado de cache por pantalla: hora de cacheo, tiempo restante y si sigue vigente.
- El snapshot financiero persistido se amplia para incluir categorias, graficas y alquileres, ademas de dashboard, cuentas, movimientos, deuda y seguros.
- Movimientos mantiene cache propia por filtros activos, porque busqueda/cuenta/categoria cambian de forma independiente.
- Se agrega refresco selectivo desde Ajustes: pantallas de snapshot o movimientos.
- El refresco de cabecera sigue forzando carga remota y saltandose el TTL.
- Los snapshots antiguos sin `loadedAtMillis` se muestran como `Cache local` con fecha no disponible, en lugar de `Sin cache`.
- Se evita recalcular el estado de cache desde el `init` del ViewModel para prevenir crashes de arranque con snapshots antiguos o estado recien construido.
- `SessionStore` queda blindado ante errores de Keystore/preferencias cifradas: si la sesion local no se puede descifrar, la app vuelve a login en vez de crashear.
- `BiometricGate` captura errores al construir o lanzar el prompt biometrico y los transforma en mensaje de autenticacion.
- Se prueba arranque limpio en emulador `Pixel_Fold_API_36` con `com.contaapp.android.debug` sin `FATAL EXCEPTION`.
- Se corrige crash al guardar cache por `org.json.JSONException: Forbidden numeric value: NaN`; todos los `Double` del snapshot se serializan con saneo `NaN/Infinity -> 0.0`.
- `SnapshotStore.save` falla de forma suave si aparece cualquier error futuro de serializacion, evitando que una cache defectuosa cierre la app.
- Se verifica en emulador que tras reinstalar y abrir la app el proceso queda vivo y el buffer `logcat -b crash` queda limpio.
- Build real verificado fuera del sandbox con Gradle 8.9 cacheado.

## 2026-06-01 - Correcciones UX/Estabilidad - Cache, mes pasado y modo noche

- La vista financiera de Home agrega la opcion `Mes pasado`.
- Home deja de recargar al volver a la pestana si los datos actuales siguen dentro del TTL configurado.
- El boton de refresco de cabecera fuerza recarga inmediata de la ultima vista financiera consultada.
- Ajustes incorpora configuracion de TTL de cache: 5, 15, 30 o 60 minutos.
- Se corrige el parseo de `CategoryOption.color` para usar `android.graphics.Color.parseColor`, evitando posibles crashes al renderizar movimientos con colores backend.
- Se revisan superficies de modo noche en Deuda, Seguros, Login, Cuentas y componentes compartidos para eliminar tarjetas blancas residuales.
- Build real verificado fuera del sandbox con Gradle 8.9 cacheado.

## 2026-06-01 - Implementacion - Graficas y alquileres

- Se agrega seccion `Graficas` en la app movil con selector de periodo e intervalo manual de fechas.
- Graficas usa las mismas familias de datos localizadas en el dashboard web: historico por categoria, evolucion de deuda, evolucion de saldo de cuentas y cashflow de alquileres.
- Se agrega seccion `Alquileres` con KPIs de rentabilidad, cashflow, detalle mensual por ejercicio y ranking de propiedades con actividad.
- Se confirma en `C:\Codigo\ContMovApp` la disponibilidad de APIs de alquileres: `/api/rentals/summary`, `/api/rentals/properties-breakdown`, `/api/rentals/cashflow`, `/api/rentals/expenses-by-category`, `/api/rentals/year-comparison`, `/api/rentals/seasonal-analysis`, `/api/rentals/top-properties`.
- Se confirma drill-down por propiedad: `/api/properties/:id/monthly-summary`, `/api/properties/:id/month-details` y `/api/properties/:id/category-evolution`.
- La navegacion movil pasa a cinco entradas principales y una opcion `Mas` para no saturar la barra inferior; en tablet se mantiene rail con todas las secciones.
- Se corrige de forma reforzada la tarjeta `Top gastos` para que use superficie dark en modo noche.
- Build real verificado fuera del sandbox con Gradle 8.9 cacheado.

## 2026-06-02 - Correcciones funcionales - Graficas dashboard, alquileres y deuda

- Graficas deja de depender principalmente de `dashboard/category-history` y usa `dashboard/summary.series.current`, igual que el dashboard web, para la evolucion financiera.
- Se agregan modos de grafica `Diario`, `Acumulado` y `Mensual`, equivalentes a la logica del dashboard web para ingresos, gastos y neto.
- Las graficas muestran etiquetas de eje X abreviadas y explican si la serie del backend viene por dias o por meses.
- Se mantiene `category-history` como graficas especificas de categorias de ingresos/gastos, no como serie financiera principal.
- Alquileres agrega categorias de gasto por mes usando `/api/rentals/expenses-by-category`.
- Deuda separa claramente prestamos y financiaciones, con resumen por bloque y detalle de operaciones.
- Build real verificado fuera del sandbox con Gradle 8.9 cacheado.

## 2026-06-02 - UX/Funcional - Graficas alineadas con dashboard web

- La seccion `Graficas` se reestructura para mostrar las mismas familias visuales del dashboard web:
  - `Evolucion Financiera`: ingresos, gastos y neto del periodo.
  - `Distribucion rapida`: resumen de ingresos, gastos, balance y conteo de categorias.
  - `Categorias del Periodo`: distribucion por categoria con alternancia gastos/ingresos.
  - `Top categorias`: gastos e ingresos principales.
  - `Historico por Categorias`: evolucion mensual de categorias.
- Se eliminan graficas secundarias que no formaban parte de la peticion principal para evitar ruido visual.
- Las graficas dejan de ocultarse cuando solo hay un punto de datos; se muestra el valor disponible con eje X/resumen.
- Build real verificado con `--rerun-tasks` fuera del sandbox.

## 2026-06-02 - UX/L&F - Pulido premium de controles y estados

- Se incorpora un selector de periodo premium reutilizable con chips horizontales para Home y Graficas.
- Se mejora la paleta dark con fondo mas profundo, superficies azul noche y variantes con mejor jerarquia.
- Home muestra skeleton card cuando esta cargando sin datos iniciales, evitando una espera visual pobre.
- Movimientos mejora el empty state con mensaje accionable y visual propio.
- La barra de filtros activa se convierte en una tarjeta premium con icono, resumen y llamada a filtro mas clara.
- El detalle de movimiento incorpora badges de calidad: sin cuenta, sin categoria, pendiente, vinculado y duplicado.
- El bottom sheet de filtros de movimientos aumenta margen util y explica que los filtros se pueden combinar.
- Build real verificado fuera del sandbox con Gradle 8.9 cacheado.

## 2026-06-01 - Correcciones UX/API - Categorias reales, periodo y dark mode

- Home sustituye el selector con flechas por un combo de periodo mas elegante.
- El cambio de periodo en Home recarga `dashboard/summary` con `dateFrom/dateTo`, no solo la lista de movimientos.
- `dashboard/summary` en Android acepta ahora filtros de fecha/cuenta mediante `MovementFilters`.
- Se mapea `CategoryOption.color` del backend a colores reales en filas de movimientos.
- Se mapea `CategoryOption.icon` a la iconografia local disponible cuando el icono/categoria representa hogar, banco, seguro o tarjeta; se mantiene fallback semantico por concepto.
- El detalle de movimiento intenta resolver la cuenta por `accountId` contra `accounts/balances` si `accountName` no llega informado.
- La vinculacion del detalle muestra tipo y detalle disponible: prestamo/financiacion con nombre si coincide en deuda, seguro con nombre si coincide, o identificador como fallback.
- El filtro de Cuentas deja de buscar por banco en texto libre; el banco se selecciona exclusivamente desde el combo.
- Se corrigen superficies oscuras pendientes en Home, Movimientos, Cuentas, botones de filtro, grafico y componentes compartidos.
- El grafico `Ingresos vs gastos` muestra numeros de inicio/fin para orientar la lectura.
- Build real verificado fuera del sandbox con Gradle 8.9 cacheado.

## 2026-06-01 - Correccion visual - Modo oscuro premium

- Se revisa el modo oscuro tras feedback visual: el fondo era oscuro pero Ajustes, navegacion y varias tarjetas seguian usando superficies blancas fijas.
- `ContaTheme` actualiza la paleta dark con fondo azul noche, superficies elevadas, variantes de superficie, outline y acentos teal/azul con mayor contraste.
- Se fuerza una tipografia sans-serif propia de la app para evitar que la fuente del sistema degrade el aspecto profesional.
- Ajustes pasa sus tarjetas, textos secundarios, notas, toggles y acciones a `MaterialTheme.colorScheme` para funcionar en claro y oscuro.
- La navegacion lateral, barra inferior y botones de cabecera dejan de usar blanco fijo y respetan superficies/contraste del tema.
- Componentes compartidos como `SectionHeader` y `AttentionCard` usan `onSurface`/`onSurfaceVariant`.
- Build real verificado fuera del sandbox con Gradle 8.9 cacheado.

## 2026-06-01 - Correcciones UX/API - Filtros reales y contexto de movimientos

- Home convierte el selector de periodo en un control funcional: cambia entre mes actual, ultimos 30 dias y todo el historico, y recarga la actividad reciente con ese rango.
- Se incorpora `GET /api/categories` al payload principal para disponer de categorias reales del backend.
- `MovementFilters` deja de enviar categoria como texto defensivo y pasa a usar `categoryId` y `accountId`, alineado con `GET /api/movements-global`.
- El modal de filtros de Movimientos se compacta, respeta `navigationBarsPadding`, usa secciones con iconos y evita quedar tapado por los botones del sistema Android.
- Movimientos permite filtrar por texto, periodo rapido, fecha desde/hasta, tipo, cuenta, categoria, revision e importe.
- Las filas de movimientos muestran fecha, categoria, cuenta y una linea de vinculacion cuando existe relacion con prestamo, financiacion, propiedad, seguro, split o duplicado.
- El detalle de movimiento incluye cuenta, categoria, vinculacion, estado, splits/duplicados y notas.
- Cuentas agrega selector desplegable de banco para filtrar por entidad de forma directa, ademas de busqueda y filtro de saldo.
- Build real verificado fuera del sandbox con Gradle 8.9 cacheado.

## 2026-05-31 - Decision - Alcance inicial del MVP

- Se decide crear una app Android nativa de consulta financiera basada en las APIs de `C:\Codigo\ContMovApp`.
- Se excluye IA del MVP inicial.
- Se excluye alta/importacion/edicion/borrado de movimientos.
- Se priorizan dashboard, cuentas, movimientos, deuda y seguros.

## 2026-05-31 - Decision - Biometria

- Despues de un primer login con usuario/password, las siguientes aperturas deben solicitar huella o biometria antes de mostrar datos financieros.
- La biometria desbloquea la sesion local; no reemplaza la autenticacion del backend.
- No se guardara la password.

## 2026-05-31 - Documentacion - Memoria estable del proyecto

- Se crea `docs/ARCHITECTURE.md` como documento de arquitectura y decisiones.
- Se crea `docs/CHANGELOG.md` como bitacora de cambios.
- Objetivo: reducir uso de tokens usando documentos locales como contexto persistente.

## 2026-05-31 - Implementacion - Proyecto Android inicial

- Se crea proyecto Gradle Android con modulo `app`.
- Se configura Kotlin, Jetpack Compose, Material 3, OkHttp y AndroidX.
- Se copia Gradle wrapper y se ajusta a Gradle 8.9 para compatibilidad con Android Gradle Plugin 8.7.3.
- Se configura `compileSdk=35`, `targetSdk=34`, `minSdk=28`.

## 2026-05-31 - Implementacion - Login, sesion y biometria

- Se implementa login contra `POST /api/auth/login`.
- Se implementa bloqueo biometrico local con `android.hardware.biometrics.BiometricPrompt`.
- Se implementa almacenamiento de sesion cifrado con Android Keystore AES/GCM.
- No se guarda la password.

## 2026-05-31 - Implementacion - Pantallas MVP

- Se crean pantallas Compose para Inicio, Movimientos, Cuentas, Deuda y Seguros.
- Se implementa modo privacidad para ocultar importes.
- Se implementa cliente API inicial con OkHttp y parseo JSON defensivo.
- Se conectan endpoints principales de dashboard, cuentas, movimientos, deuda y seguros.

## 2026-05-31 - Verificacion - Build debug

- Comando ejecutado: `.\gradlew.bat :app:assembleDebug --console=plain`.
- Resultado: build correcto.
- APK debug generado en `app/build/outputs/apk/debug/app-debug.apk`.

## 2026-05-31 - Implementacion - Contratos, refresh y movimientos

- Se ajusta el mapeo Android a contratos reales del backend: `accounts/balances`, `insurances/summary`, `loans/summary/global`, `financings/summary` y `movements-global`.
- Se implementa refresh token automatico cuando una llamada protegida devuelve 401.
- Se agregan filtros basicos de movimientos por texto y tipo.
- Se agrega detalle de movimiento en modo lectura.
- Se habilita trafico HTTP claro para pruebas locales contra `10.0.2.2:4000`; produccion debe usar HTTPS.

## 2026-05-31 - Implementacion - Selector de servidor y cleartext

- Se agrega configuracion de servidor desde la pantalla de login mediante boton `Servidor`.
- La URL se persiste localmente y se normaliza para terminar en `/api`.
- Se agrega `network_security_config.xml` con cleartext permitido para desarrollo local.
- Se mantiene `usesCleartextTraffic=true` para evitar el error `Cleartext communication not permitted` al probar con IP/HTTP local.
- Build verificado tras limpiar `app/build` por bloqueo de fichero generado en OneDrive.

## 2026-05-31 - Diseno - Look and feel premium

- Se redisena el login con fondo coral, marca centrada y tarjeta blanca elevada.
- Se cambia la paleta base a coral, grafito, blanco, teal y ambar.
- Se agrega cabecera de periodo, hero de balance y anillo de composicion financiera en la pantalla de inicio.
- Se mejoran filas de movimientos con avatar por tipo e importes coloreados.
- Se mejoran filas de cuentas y deuda con tratamiento visual mas denso y elegante.
- Se agregan presets rapidos en el selector de servidor: emulador y movil fisico.
- Build verificado con `.\gradlew.bat :app:assembleDebug --console=plain`.

## 2026-06-01 - Infraestructura - Mover proyecto fuera de OneDrive

- Se decide mover/copiar el proyecto a `C:\Codigo\ContaApp`.
- Motivo: OneDrive genera bloqueos/snapshots inconsistentes en archivos de build de Gradle.
- La ruta recomendada de trabajo a partir de ahora sera `C:\Codigo\ContaApp`.
- Se deben excluir salidas generadas como `app/build`, `.gradle` y `.gradle-build`.

## 2026-05-31 - Implementacion - Prueba de servidor

- Se agrega boton `Probar` en el dialogo de servidor.
- La prueba llama a `GET /api/health` usando la URL configurada.
- Se muestra resultado de conexion correcta o error antes de intentar login.
- Build verificado con `.\gradlew.bat :app:assembleDebug --console=plain`.

## 2026-05-31 - Diseno/Implementacion - Dashboard visual avanzado

- Se parsean categorias de `GET /api/dashboard/summary`.
- Se agrega tarjeta `Top gastos` con ranking real y barras de progreso.
- Se agrega tarjeta de insight en Movimientos con neto, ingresos, gastos y barra comparativa.
- Se agrega tarjeta total en Cuentas y dialogo de detalle al tocar una cuenta.
- Se corrige un `@Composable` duplicado detectado por compilacion.
- Build verificado con `.\gradlew.bat :app:assembleDebug --console=plain`.

## 2026-05-31 - Implementacion - Seguros y renovaciones

- Se agrega modelo `InsurancePolicy`.
- Se consume `GET /api/insurances/expiring-soon?days=60`.
- Se rediseña pantalla de Seguros con hero oscuro de prima anual, activos y vencimientos.
- Se agrega lista de proximas renovaciones en modo lectura.
- Build verificado con `.\gradlew.bat :app:assembleDebug --console=plain`.

## 2026-06-01 - Diseno/Implementacion - Login y servidor

- Se sustituye el boton textual de servidor en login por un acceso flotante `API` en la parte superior derecha.
- Se agrega una pildora de estado con la URL base activa dentro de la tarjeta de login.
- Objetivo: mantener el selector de servidor visible para pruebas sin restar elegancia ni mezclarlo con usuario/password.
- Build verificado con `.\gradlew.bat :app:assembleDebug --console=plain`.

## 2026-06-01 - Implementacion - Detalle de deuda

- Se agrega soporte de arrays JSON directos en `ApiClient` para endpoints que devuelven listas en raiz.
- Se agrega modelo `DebtDetail` y carga combinada desde `GET /api/loans/debt-detail` y `GET /api/financings/debt-detail`.
- La pantalla Deuda muestra detalle individual con entidad, pendiente, cuota, estado y proximo pago; mantiene fallback al resumen agregado.
- Build verificado con `.\gradlew.bat :app:assembleDebug --console=plain`.

## 2026-06-01 - Diseno/Implementacion - Iconos y tablet

- Se agregan iconos vectoriales locales en `app/src/main/res/drawable` para navegacion, servidor, biometria, privacidad, refresco y salida.
- Se evita `material-icons-extended` para no introducir una dependencia pesada ni alargar compilaciones.
- La navegacion usa barra inferior con icono+texto en movil y rail lateral en tablet.
- El contenido en tablet queda centrado con ancho maximo para una lectura mas elegante.
- Build real verificado fuera del sandbox con Gradle 8.9 cacheado: `gradle.bat :app:assembleDebug --offline --console=plain --no-daemon`.

## 2026-06-01 - Diseno - Icono de aplicacion

- Se crea icono adaptive icon en `mipmap-anydpi-v26` con fondo coral y foreground vectorial.
- Se agrega fallback vectorial en `mipmap-anydpi` para versiones anteriores.
- Se agrega variante monocroma para integrarse con themed icons de Android.
- Se actualiza `ic_launcher_background` a coral para alinearlo con la identidad visual de la app.
- Build real verificado fuera del sandbox con Gradle 8.9 cacheado.

## 2026-06-01 - Diseno/Implementacion - Pulido responsive

- Se crea una top bar mas visual con icono contextual, usuario/vista segura y acciones en contenedores blancos.
- En tablet, el rail lateral incorpora marca y deja la navegacion mas parecida a una app financiera profesional.
- La barra inferior movil gana esquinas superiores redondeadas y mayor elevacion.
- La pantalla Inicio adapta su layout: composicion y ranking en dos columnas en tablet; metricas en cuatro columnas; mas movimientos visibles.
- Build real verificado fuera del sandbox con Gradle 8.9 cacheado.

## 2026-06-01 - Implementacion - Filtros, cache y HTTP temporal

- Se agrega cache ligera `SnapshotStore` en SharedPreferences para restaurar el ultimo snapshot financiero cuando la API no responde.
- Movimientos pasa a tener indicador de carga propio para no bloquear toda la app al filtrar.
- Cuentas agrega busqueda por cuenta/banco y filtro por saldo positivo/negativo.
- Deuda agrega busqueda por deuda/entidad y filtros por origen y estado.
- Seguros agrega busqueda y filtro local por proximidad de renovacion.
- Se documenta que HTTP seguira permitido temporalmente tambien para produccion mientras no exista HTTPS definitivo.
- Build real verificado fuera del sandbox con Gradle 8.9 cacheado.
- Pendiente tecnico: separar fisicamente `AppRoot.kt` en archivos por pantalla/componentes en una tanda dedicada.

## 2026-06-01 - Refactor - UI por archivos

- Se separa el antiguo `AppRoot.kt` grande en archivos por responsabilidad:
  `AppRoot.kt`, `Navigation.kt`, `AuthScreens.kt`, `HomeScreen.kt`, `MovementsScreen.kt`,
  `AccountsScreen.kt`, `DebtScreen.kt`, `InsuranceScreen.kt` y `CommonComponents.kt`.
- `AppRoot.kt` queda como raiz de composicion y decision login/bloqueo/app.
- Se mantienen componentes compartidos en `CommonComponents.kt` para reducir duplicacion y contexto futuro.
- Build real verificado fuera del sandbox con Gradle 8.9 cacheado.

## 2026-06-01 - Diseno/Usabilidad - Lectura financiera avanzada

- Home agrega bloque `Requiere atencion` con movimientos pendientes, deuda en aviso/vencida y seguros a 30 dias.
- Movimientos se agrupan por fecha relativa: hoy, ayer, ultimos 7 dias, este mes o mes/anio.
- Se agregan componentes comunes `StatusChip`, `AttentionCard` y `SectionHeader` para mejorar consistencia visual.
- Cuentas se agrupan por banco y muestran contador de cuentas en negativo.
- Deuda destaca el proximo pago con importe, entidad y estado.
- Seguros muestra coste mensual equivalente calculado desde prima anual.
- Build real verificado fuera del sandbox con Gradle 8.9 cacheado.

## 2026-06-01 - Implementacion - Ajustes, dark mode y vistas avanzadas

- Se agrega pantalla `Ajustes` con modo oscuro, privacidad por defecto, refresco, exportacion CSV, borrado de cache y cierre de sesion.
- Se agrega persistencia de preferencias en `AppPreferencesStore`.
- `ContaTheme` soporta paleta oscura real.
- Movimientos agrega agrupacion por fecha/categoria/cuenta y filtros por importe minimo/maximo.
- Deuda agrega resumen de pagos proximos 30 dias.
- Se agrega exportacion CSV local de movimientos desde el ViewModel.
- Se agrega notificacion local manual de resumen de atencion desde Ajustes.
- Se solicita `POST_NOTIFICATIONS` en Android 13+.
- Se agrega widget Android con balance, movimientos pendientes y avisos de deuda desde el ultimo snapshot.
- Build real verificado fuera del sandbox con Gradle 8.9 cacheado.

## 2026-06-01 - Diseno/Usabilidad - Pulido premium transversal

- Se compactan los filtros en todas las pantallas para evitar botones grandes, saltos de linea y sensacion de formulario pesado.
- La barra inferior movil usa etiquetas cortas para eliminar truncados y mejorar lectura con seis secciones.
- Se elevan radios y jerarquia visual de tarjetas en Inicio, Movimientos, Cuentas, Deuda, Seguros, Ajustes, Login y bloqueo biometrico.
- El grafico de actividad deja de incluir deuda en el anillo principal para que ingresos/gastos sean legibles; la deuda queda como metrica explicita.
- Movimientos gana secciones de filtro rotuladas por tipo, periodo, revision y agrupacion.
- Cuentas, Deuda y Seguros usan chips mas compactos y filas de lista menos altas para ganar densidad profesional.
- Ajustes se alinea con el resto de la app mediante tarjetas redondeadas y acciones tipo pildora.
- Build real verificado fuera del sandbox con Gradle 8.9 cacheado.

## 2026-06-01 - Diseno/Usabilidad - Correccion visual estilo Wallet

- Se corrigen las tarjetas de atencion para que los importes no se recorten en movil.
- Deuda cambia a un resumen mas claro tipo Wallet: importe principal, porcentaje pendiente, barra de progreso y metricas compactas.
- Ajustes cambia las acciones a botones full-width apilados para evitar texto vertical o botones comprimidos en pantallas estrechas.
- Los estados vacios pasan a tarjetas discretas, menos texto suelto y mejor integradas visualmente.
- Se mantiene la referencia de Wallet/BudgetBakers como direccion de L&F: densidad, chips compactos, lectura financiera rapida y menos apariencia de formulario.
- Build real verificado fuera del sandbox con Gradle 8.9 cacheado.

## 2026-06-01 - Diseno/Usabilidad - Redisenio transversal tipo Wallet

- Movimientos cambia a timeline con cabeceras por grupo que muestran numero de movimientos e importe neto del grupo.
- Los filtros de Movimientos salen de la pantalla principal y pasan a un `ModalBottomSheet`; la pantalla muestra solo un resumen compacto de filtros activos.
- Las filas de movimientos usan iconos vectoriales por tipo/concepto en lugar de letras, importes alineados y chip de pendiente cuando aplica.
- Home reutiliza la nueva fila Wallet para ultimos movimientos, mejorando densidad y consistencia.
- Cuentas se reestructura en tarjetas por banco con total, numero de cuentas y cuentas internas compactas.
- Deuda reemplaza letras en filas por iconografia financiera.
- Seguros agrega resumen calendario con coste mensual y vencimientos a 30/60 dias, y filas con icono de escudo.
- La top bar y barra inferior se aligeran para dejar mas protagonismo al contenido.
- Se mejora el placeholder de privacidad de importes a `•••• €` para mantener layout estable.
- Build real verificado fuera del sandbox con Gradle 8.9 cacheado.

## 2026-06-01 - Diseno/Usabilidad - Home como dashboard ejecutivo

- Inicio deja de ser una suma de bloques y pasa a una vista de control financiero: balance, neto, revision y categorizacion en el primer golpe visual.
- Se sustituye el hero simple por `HomeHeroDashboard` con gradiente premium, estado del periodo y mini metricas internas.
- Se agregan KPIs compactos tipo Wallet para ingresos, gastos, deuda y revision.
- Se agrega `FinancialPulseCard` con barra comparativa ingresos/gastos y neto del periodo.
- Se agrega `HomeHealthCard` con una puntuacion sintetica de salud financiera basada en revision, categorizacion y avisos de deuda.
- Las alertas se integran junto a salud financiera en tablet y quedan apiladas con mejor jerarquia en movil.
- La seccion final pasa a `Actividad reciente` y reutiliza filas timeline compactas con iconos semanticos.
- Build real verificado fuera del sandbox con Gradle 8.9 cacheado.

## 2026-06-01 - Diseno/API - Executive Ledger e inventario backend

- Se inicia sistema visual `Executive Ledger` inspirado en la propuesta light corporativa: fondo azul claro, tarjetas blancas, azul ejecutivo, teal, ambar y coral como acentos.
- Se agregan componentes reutilizables `ExecutiveKpiCard`, `MiniSparkline`, `ExecutiveProgressCard` y `FinancialLineChart`.
- Home incorpora KPIs con icono y mini sparkline, ademas de grafico ligero de ingresos/gastos estimado.
- Movimientos cambia su tarjeta resumen a look light executive para unificar con Home.
- Deuda cambia el hero oscuro por una tarjeta clara de exposicion financiera.
- Ajustes se reorganiza en secciones `Apariencia y seguridad`, `Datos` y `Sesion`, con filas de ajuste iconificadas.
- Se confirma acceso al backend `C:\Codigo\ContMovApp` y se revisan rutas disponibles bajo `backend/src/features`.
- Build real verificado fuera del sandbox con Gradle 8.9 cacheado.

## 2026-06-01 - Correcciones UX - Periodo, filtros, detalle y tema

- Se restaura el selector visual de periodo en Home tras el redisenio ejecutivo.
- Se ajustan las KPI de Home en movil: se ocultan sparklines en tarjetas estrechas para evitar textos partidos.
- Movimientos agrega filtros explicitos por categoria, fecha desde/hasta, tipo, revision, rango rapido e importe.
- `MovementFilters` envia `category` y `categoryName` como query params defensivos para adaptarse al backend.
- El detalle de movimiento se redisenia con icono, cabecera, importe destacado, cuenta, categoria, estado y notas.
- Componentes clave (`ExecutiveKpiCard`, `FinancialLineChart`, `ActiveFilterBar`, `FilterPanel`, `InlineLoading`, `EmptyHint`, detalle) pasan a respetar `MaterialTheme` para mejorar claro/oscuro.
- Se verifica en backend que las categorias soportan `icon` y `color`; la app movil aun usa iconos locales heurísticos hasta incorporar `GET /api/categories`.
- Build real verificado fuera del sandbox con Gradle 8.9 cacheado.
