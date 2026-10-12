# ETAPA 4F — Finanzas: cobro de saldo, historial de pagos, recibos en PDF, resumen mensual y Dashboard

> Pega primero el **CONTEXTO GLOBAL** y la **ADENDA v3**. Luego pega este texto.
> Requisito: Etapas 1 a 4E terminadas (74 pruebas). La auditoría de esta etapa ya la hiciste; este prompt se basa en ella.

---

## OBJETIVO

Hoy solo se puede registrar el anticipo y los reembolsos. En esta etapa el Socio podrá:

1. **Cobrar el saldo** de un pedido (completo o en abonos) desde su detalle.
2. Ver el **historial de pagos** del pedido y generar/compartir un **recibo en PDF** por cada pago o reembolso.
3. Ver en **Finanzas** el resumen del mes (cobrado, reembolsos, neto, por cobrar) y la lista de movimientos.
4. Ver en el **Dashboard** los números del mes y un resumen operativo.

Todo con datos en memoria y arquitectura lista para Firebase (interfaces + `InMemory*`).

## IDEAS DEL NEGOCIO (decisiones ya tomadas)

- **El estado del pedido NO cambia al liquidar.** Un pedido pagado completo sigue `CONFIRMADO` (o `COMPLETADO` si ya pasó). Lo que marca que está liquidado es `cobrado = true` y `fechaCobro`. No uses `ESTADO_PAGADO` para esto.
- **El saldo se puede pagar en abonos.** Un abono parcial es `TIPO_ABONO`; el pago que deja el saldo en cero es `TIPO_TOTAL`.
- **Se puede cobrar saldo de pedidos `CONFIRMADO`, `PAGADO`, `EN_REVISION` y `COMPLETADO`.** Un pedido completado con saldo pendiente sigue apareciendo como "por cobrar".
- **Dinero siempre redondeado a centavos** (2 decimales) en todos los cálculos; compara con tolerancia de 0.005. Nunca acumules errores de punto flotante.
- **Reembolso:** `PoliticaCancelacion` sigue calculando sobre el **total pagado hasta ese momento** (`anticipoPagado` acumula anticipo + abonos). No cambies la política.
- Los recibos **no son comprobantes fiscales**; el PDF lo dice al pie.

## PASO 0 — Confirmación rápida (confirma en tu reporte final; si algo contradice este prompt, ajusta con criterio y dímelo)

1. En `InMemoryPedidoRepository.liberarApartadosVencidos`, ¿un pedido `CONFIRMADO` pasa a `COMPLETADO` aunque tenga saldo pendiente o `cobrado == false`? Debe pasar.
2. En `registrarAnticipo`, ¿qué hace si el monto cubre el 100%? Ahora debe, además de lo actual, dejar `cobrado = true` y `fechaCobro` (no cambia el estado).
3. ¿Qué formato de moneda existe ya en `FormatUtils`? Reutilízalo; no crees otro.
4. ¿`Pago.fecha` se asigna siempre (aunque la prueba o el llamador no la ponga)? Debe asignarse con el `Reloj` si viene en 0.

## PASO 1 — Modelo y repositorios

### 1.1 `Pago.java`
- Constante `TIPO_ABONO = "ABONO"`.
- Campo `String folio` (nullable) con getter/setter y copia defensiva.

### 1.2 `InMemoryPagoRepository`
- En `registrarPago`: si `folio == null`, asígnalo con un contador consecutivo `synchronized` con formato **`R-0001`**, `R-0002`... (4 dígitos, pasa a 5 si hace falta). Los pagos existentes en pruebas sin folio no deben fallar. Comentario `// FIRESTORE: transacción`.
- Agrega validaciones de integridad **solo si no rompen las pruebas existentes**: `monto > 0`, `tipo` y `metodo` no nulos. Si alguna prueba previa crea un pago inválido, no las agregues y dímelo.

### 1.3 `Pedido.java`
- Agrega los métodos (sin campos nuevos): `double getSaldoPendiente()` = `redondear2(max(0, total - anticipoPagado))` y `boolean estaLiquidado()` = `saldoPendiente == 0` (con tolerancia). No cambies nombres de campos existentes.

### 1.4 `PedidoRepository` / `InMemoryPedidoRepository`
Agrega `void liquidarSaldo(String pedidoId, double monto, String metodo, Callback<Pago> callback)`, `synchronized`, con `// FIRESTORE: transacción`:
- Valida: pedido existe; estado en `{CONFIRMADO, PAGADO, EN_REVISION, COMPLETADO}`; `monto > 0`; método válido (`EFECTIVO`, `TRANSFERENCIA`, `TARJETA`); saldo pendiente > 0; `monto <= saldoPendiente` (con tolerancia). Mensajes de error claros en español.
- Redondea `monto` a 2 decimales. Crea el `Pago` (`TIPO_TOTAL` si `monto` iguala el saldo; si no, `TIPO_ABONO`), con `fecha` del `Reloj`, `clienteId` del pedido, y regístralo en `pagoRepository`.
- Suma el monto a `anticipoPagado`. Si el saldo queda en 0: `cobrado = true`, `fechaCobro = ahora`.
- No toca estado, evento ni unidades.
Ajusta también `registrarAnticipo` según el punto 2 del Paso 0.

## PASO 2 — Lógica pura (sin Android, estáticos)

### 2.1 `domain/MoneyUtils.java`
`double redondear2(double)` (HALF_UP vía `Math.round(x*100)/100.0`) y `boolean iguales(double a, double b)` con tolerancia 0.005. Úsalo en `Pedido`, `liquidarSaldo` y los cálculos nuevos.

### 2.2 `domain/FinanzasUtils.java`
- `double[] resumenMes(List<Pago> pagos, List<Pedido> pedidos, int anio, int mes0)` → `{cobrado, reembolsos, neto, porCobrar}`:
  - `cobrado`: suma de pagos tipo `ANTICIPO`, `ABONO` y `TOTAL` cuya `fecha` cae en ese mes (zona del negocio, límites `[inicio del mes, inicio del mes siguiente)`).
  - `reembolsos`: suma de pagos `REEMBOLSO` del mes.
  - `neto = cobrado − reembolsos`.
  - `porCobrar`: suma del saldo pendiente de los pedidos en `{CONFIRMADO, PAGADO, EN_REVISION, COMPLETADO}` cuyo **evento inicia en ese mes**. Excluye apartados, cancelados, rechazados y vencidos.
- `List<Pago> movimientosDelMes(List<Pago> pagos, int anio, int mes0, int filtro)` con `filtro` 0 = todos, 1 = ingresos, 2 = reembolsos; ordenados por `fecha` **descendente** (desempate por folio descendente).

### 2.3 `domain/ReciboUtils.java`
- `double saldoDespuesDe(double totalPedido, List<Pago> pagosDelPedido, String pagoId)`: ordena los pagos por `fecha` (desempate por orden de la lista), acumula ingresos y resta reembolsos hasta incluir ese pago, y devuelve `max(0, total − neto pagado)`.
- `double totalPagadoHasta(List<Pago> pagosDelPedido, String pagoId)`: neto acumulado (ingresos − reembolsos) hasta ese pago inclusive.
- `String tituloRecibo(String tipo)`: "Recibo de anticipo", "Recibo de abono", "Recibo de pago total", "Comprobante de reembolso".

### 2.4 `domain/DashboardUtils.java`
`int[] contadores(List<Pedido> pedidos, long ahora)` → `{eventosHoy, proximos7Dias, apartadosPorPagar, asignacionIncompleta}`:
- `eventosHoy`: pedidos que `AgendaUtils.apareceEnAgenda` excluyendo apartados y cuyo inicio cae en el día de hoy.
- `proximos7Dias`: igual, con inicio entre mañana y los 7 días siguientes inclusive.
- `apartadosPorPagar`: apartados vigentes (no vencidos).
- `asignacionIncompleta`: pedidos activos con `asignacionIncompleta == true`.

## PASO 3 — Detalle del pedido

En `PedidoDetalleFragment` y su layout (mismo estilo):
1. **Botón "Cobrar saldo"** (`btnCobrarSaldo`): visible si el estado está en `{CONFIRMADO, PAGADO, EN_REVISION, COMPLETADO}` y el saldo > 0. Abre un diálogo con: monto (precargado con el saldo, editable), selector de método (mismo patrón que el diálogo de anticipo) y validación en línea. Al confirmar llama a `liquidarSaldo`; éxito → Snackbar "Pago registrado" con acción **"Recibo"** que genera y comparte el PDF de ese pago.
2. **Badge "Liquidado"** (misma paleta de insignias) cuando `estaLiquidado()` y el pedido no está cancelado.
3. **Sección "Historial de pagos"**: observa `pagoRepository.getPagosPorPedido(pedidoId)`; lista compacta (puede ser un `LinearLayout` con filas inflando un layout `item_pago.xml`, o un `RecyclerView` sin scroll propio) con: fecha, folio, tipo en español, método y monto (ingresos con `+`, reembolsos con `−` y color de error del tema). Cada fila tiene un botón de texto **"Recibo"**. Estado vacío "Sin pagos registrados". Orden ascendente por fecha.
4. La sección "Dinero" existente se mantiene y el saldo pendiente usa `getSaldoPendiente()`.
Textos en `strings.xml`, sin colores fijos.

## PASO 4 — Recibos en PDF

- `util/ReciboPdf.java` (Android): genera con `android.graphics.pdf.PdfDocument` una página **A4 (595×842 pt)** y devuelve el `File` en `getCacheDir()/recibos/recibo_<folio>.pdf` (crea la carpeta; sobrescribe si existe). Contenido: encabezado "Sonido Sonic" y el título de `tituloRecibo`; folio y fecha del pago; cliente (nombre y teléfono si existe); nombre del evento, fecha y horario, paquete; **tipo y método de pago y monto**; bloque de resumen con **Total del servicio, Pagado a la fecha de este recibo, Saldo restante** (usa `ReciboUtils`); en reembolsos sustituye por "Monto reembolsado" y el motivo si existe en `notas`; pie con "Este documento no es un comprobante fiscal." Usa tipografías del sistema y un diseño limpio (márgenes de 40 pt, líneas separadoras, monto destacado). Todo el dibujo con `Canvas`/`Paint`; sin librerías nuevas.
- `util/CompartirRecibo.java`: obtiene el URI con `FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".fileprovider", archivo)` y lanza `Intent.ACTION_SEND` (`application/pdf`, `EXTRA_STREAM`, `FLAG_GRANT_READ_URI_PERMISSION`) dentro de `Intent.createChooser`.
- `AndroidManifest.xml`: declara el `<provider>` de `androidx.core.content.FileProvider` con `android:authorities="${applicationId}.fileprovider"`, `android:exported="false"`, `android:grantUriPermissions="true"` y meta-data a `@xml/file_paths`.
- `res/xml/file_paths.xml`: `<cache-path name="recibos" path="recibos/" />`.
- Si hay un error al generar, muestra un Snackbar "No se pudo generar el recibo" (no hagas crash).

## PASO 5 — Pantalla Finanzas

Reemplaza el placeholder (`fragment_finanzas.xml` dentro de `NestedScrollView`), con el estilo del selector de mes de la Agenda:
1. **Selector de mes**: `<` título del mes `>` y botón "Mes actual" (empieza en el mes actual).
2. **Cuatro tarjetas KPI** en cuadrícula de 2×2: **Cobrado**, **Reembolsos**, **Neto** y **Por cobrar** (montos con el formato de moneda existente; Neto en color de error si es negativo).
3. **Filtro** `MaterialButtonToggleGroup`: **Todos | Ingresos | Reembolsos**.
4. **Lista de movimientos** del mes: cada fila con fecha, folio, nombre del evento/cliente (resuelto desde el pedido), tipo, método y monto (con signo y color como en el historial). Tocar una fila navega al detalle del pedido (agrega `action_finanzas_to_detalle` en `nav_graph.xml`, `Bundle` con `"pedidoId"`, igual que `action_agenda_to_detalle`). Estado vacío "Sin movimientos este mes".
5. Observa `pagoRepository.getTodosLosPagos()` y los pedidos; recalcula con `FinanzasUtils`. Guarda mes y filtro en `onSaveInstanceState`.

## PASO 6 — Dashboard

Reemplaza el placeholder con dos secciones de tarjetas:
- **"Este mes"**: Cobrado, Reembolsos, Neto y Por cobrar del mes actual (mismas funciones de `FinanzasUtils`).
- **"Operación"**: Eventos hoy, Próximos 7 días, Apartados por pagar y **Equipos por asignar** (este último en color de error del tema si es mayor a cero).
Se actualiza al cambiar los `LiveData` y cada 60 s mientras está visible (Handler en `onResume`/`onPause`/`onDestroyView`, como la Agenda). Sin navegación desde las tarjetas.

## PASO 7 — Pruebas (`Etapa4FUnitTest.java`)

Sin `android.util.Patterns` ni `TextUtils`; `Reloj` fijo. Mínimo:
1. `liquidarSaldo` parcial → `TIPO_ABONO`, saldo baja, `cobrado == false`.
2. `liquidarSaldo` por el saldo exacto → `TIPO_TOTAL`, saldo 0, `cobrado == true`, `fechaCobro` correcta, el **estado no cambia**.
3. Validaciones: monto 0 o negativo, monto mayor al saldo, método inválido, pedido en `APARTADO` / `CANCELADO` / `VENCIDO`, saldo ya en 0 → todos rechazados con mensaje.
4. Un pedido `COMPLETADO` con saldo sí puede liquidarse.
5. Dos pagos consecutivos reciben folios `R-0001` y `R-0002`, únicos.
6. `registrarAnticipo` con el 100% del total deja `cobrado == true` sin cambiar el estado.
7. Redondeo: montos como 1/3 del total o 0.1 + 0.2 no dejan saldos tipo 0.0000001; el saldo termina exactamente en 0.
8. `FinanzasUtils.resumenMes`: un pago el 1 de octubre 00:00 entra a octubre; uno el 31 de octubre 23:59 entra; uno el 1 de noviembre 00:00 no entra (zona `America/Mexico_City`).
9. `resumenMes.porCobrar` excluye apartados, cancelados, rechazados, vencidos y pedidos de otros meses; incluye completados con saldo.
10. `movimientosDelMes` con cada filtro y orden descendente.
11. `ReciboUtils.saldoDespuesDe` y `totalPagadoHasta` con anticipo, abono y total (y con un reembolso al final).
12. Cancelar con el Socio un pedido liquidado reembolsa el 100% del **total pagado** y genera un `Pago` `REEMBOLSO` con su folio.
13. `DashboardUtils.contadores` con pedidos de hoy, de mañana, de dentro de 8 días, apartados vigentes y vencidos, y uno con `asignacionIncompleta`.
14. Las pruebas de Etapas 1 a 4E siguen pasando sin modificarse (si hay que tocar una, dime cuál y por qué).

Ejecuta `.\gradlew.bat test` y `.\gradlew.bat assembleDebug`. En el reporte final **da el conteo de pruebas por clase leyendo el reporte real de resultados** (no de memoria).

## NO HAGAS

- No cambies el estado del pedido al pagar. No uses `ESTADO_PAGADO` como "liquidado".
- No cambies `PoliticaCancelacion`, el motor de disponibilidad ni la asignación de unidades.
- No agregues librerías ni permisos de almacenamiento.
- No uses colores fijos ni `java.time`. No uses `NumberFormat` con Locale por defecto sin fijarlo (usa el formateo que ya hay en `FormatUtils`).

## REPORTE FINAL QUE ESPERO

Respuestas al Paso 0, archivos creados/modificados, resultado de `test` y `assembleDebug` con el conteo real por clase, y cualquier desviación.

## CRITERIOS MANUALES (los pruebo yo en el emulador)

1. Un pedido confirmado con anticipo muestra **Cobrar saldo** y el historial con un solo pago.
2. Cobrar un abono parcial: el saldo baja, no aparece "Liquidado", y el historial tiene 2 filas.
3. Cobrar el resto: aparece **Liquidado**, el botón de cobrar desaparece y el pedido sigue en Confirmado.
4. Intentar cobrar más que el saldo muestra error y no registra nada.
5. **Recibo** en cualquier fila abre el menú de compartir con un PDF de una página, con folio, datos del evento, monto y saldo restante correcto de ese momento. El PDF se abre y se lee bien.
6. Cancelar un pedido (Socio) genera una fila de reembolso con signo negativo y su comprobante en PDF dice "reembolso".
7. Finanzas: las tarjetas coinciden con los pagos que registraste; cambiar de mes con `<` `>` cambia las cifras; el filtro Ingresos/Reembolsos funciona.
8. Tocar un movimiento abre el detalle de ese pedido y "Atrás" regresa a Finanzas con el mismo mes y filtro.
9. Dashboard muestra los mismos números del mes que Finanzas y los contadores de operación correctos.
10. Todo se ve bien en modo oscuro y al girar la pantalla.
