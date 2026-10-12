# ETAPA 4G — Clientes y revisión de pedidos urgentes

> Pega primero el **CONTEXTO GLOBAL** y la **ADENDA v3**. Luego pega este texto.
> Requisito: Etapas 1 a 4F terminadas (87 pruebas). La auditoría de esta etapa ya la hiciste; este prompt se basa en ella.

---

## OBJETIVO

1. **Clientes:** una lista de clientes con búsqueda, y una ficha por cliente con su historial de pedidos y totales, accesible desde el drawer del Socio y desde el detalle de un pedido. En "Nuevo pedido", poder elegir un cliente que ya existe para no teclear sus datos otra vez.
2. **Revisión:** que el Socio pueda **Aprobar** o **Rechazar** un pedido que quedó `EN_REVISION` (los pedidos de la app con menos de 48 h de anticipación), con reembolso automático al rechazar.

No se crea una entidad `Cliente` en el repositorio: los clientes se **derivan** de los pedidos y de las cuentas `Usuario` con rol `CLIENTE`. (En Firebase se podría crear una colección después; hoy se mantiene simple.)

## IDEAS DEL NEGOCIO (decisiones ya tomadas)

- **Identidad del cliente = teléfono.** Dos pedidos con el mismo teléfono son del mismo cliente aunque el nombre se escriba distinto o uno venga de la app y otro del Socio. Si no hay teléfono utilizable (menos de 10 dígitos), se agrupa por `clienteId`; si tampoco hay, por el nombre normalizado (sin acentos, minúsculas, espacios colapsados).
- **Un pedido "cuenta"** para los totales del cliente si está en `CONFIRMADO`, `PAGADO`, `EN_REVISION` o `COMPLETADO`. Los apartados, cancelados, rechazados y vencidos no cuentan, pero el cliente sigue existiendo en la lista aunque solo tenga pedidos de esos tipos.
- **Aprobar** deja el pedido `CONFIRMADO`: nada más cambia (el evento y las unidades ya estaban asignados).
- **Rechazar** exige un **motivo escrito** y reembolsa el **100% del total pagado** (anticipo + abonos). El evento se cancela y las unidades se liberan.

## PASO 0 — Confirmación rápida (confirma en tu reporte final; si algo contradice este prompt, ajusta con criterio y dímelo)

1. ¿`Pedido.ESTADO_RECHAZADO` aparece hoy en la pestaña **Historial** de Pedidos? Debe aparecer ahí, con su insignia.
2. Nombres reales en `Pedido` del motivo de revisión y de los datos de cancelación (motivo, quién canceló), y de `metodoAnticipo`.
3. ¿`Config.ANTICIPACION_REVISION_HORAS` y `Pedido.MOTIVO_URGENTE` existen tal como dijiste? Si no hay un texto legible para `MOTIVO_URGENTE`, define el texto en `strings.xml` ("Evento con menos de 48 horas de anticipación").

## PASO 1 — Pedido y repositorio (revisión)

### 1.1 `Pedido.java`
Agrega `String motivoRechazo` (nullable) con getter/setter y copia defensiva.

### 1.2 `PedidoRepository` / `InMemoryPedidoRepository` (ambos métodos `synchronized`, con `// FIRESTORE: transacción`)
- `void aprobarPedido(String pedidoId, Callback<Pedido> callback)`:
  - Solo si el estado es `EN_REVISION`; si no, error "El pedido no está en revisión".
  - Pasa a `CONFIRMADO` y pone `requiereRevision = false` (no borres el motivo original).
- `void rechazarPedido(String pedidoId, String motivo, Callback<Pedido> callback)`:
  - Solo si el estado es `EN_REVISION`. El `motivo` (recortado) es obligatorio, mínimo 3 caracteres; si no, error claro.
  - Pasa a `RECHAZADO`, guarda `motivoRechazo`, cancela el `Evento` (`eventoRepository.cancelarEvento`), vacía `equipoIds`, pone `asignacionIncompleta = false` y ejecuta la reasignación de unidades para que otros pedidos recuperen las suyas (como ya hace `cancelarReserva`).
  - **Reembolso:** si el total pagado (`anticipoPagado`) es mayor a 0, registra un `Pago` de tipo `REEMBOLSO` por el 100% (redondeado a centavos, con `MoneyUtils`), con el mismo método del anticipo (`metodoAnticipo`; si es nulo, `METODO_EFECTIVO`). **No inventes métodos nuevos** como "REEMBOLSO_RECHAZO". Guarda el monto en `reembolsoMonto` y agrega el motivo a `notas` si así lo hace ya `cancelarReserva`.
  - Los pagos registrados siguen contando en Finanzas igual que en una cancelación (el reembolso aparece como movimiento).

## PASO 2 — Lógica pura de clientes (`domain/ClienteUtils.java`, sin Android, estáticos)

No uses `android.util.Patterns` ni `TextUtils`. Para quitar acentos usa `java.text.Normalizer` (existe en la JVM de las pruebas).

- `String soloDigitos(String s)` y `String telefono10(String s)`: los últimos 10 dígitos si hay 10 o más; si no, cadena vacía. (`+52 747 123 4567`, `(747) 123-4567` y `7471234567` dan lo mismo.)
- `String normalizarNombre(String s)`: minúsculas, sin acentos, sin espacios extra.
- `String clave(String clienteId, String telefono, String nombre)`: `"tel:" + telefono10` si hay teléfono utilizable; si no, `"id:" + clienteId` si hay id; si no, `"nom:" + normalizarNombre`.
- Clase pública estática `ResumenCliente`: `clave`, `nombre` (del pedido más reciente), `telefono` (el más reciente no vacío), `correo` (si coincide con un `Usuario` cliente por teléfono o id; si no, null), `clienteId` (el del `Usuario` coincidente; si no, null), `pedidosValidos` (int), `totalPagadoNeto` (ingresos − reembolsos de sus pedidos), `saldoPendiente` (suma de `getSaldoPendiente()` de sus pedidos válidos), `ultimoEventoMillis` (inicio más reciente entre todos sus pedidos, 0 si ninguno).
- `List<ResumenCliente> resumenes(List<Pedido> pedidos, List<Pago> pagos, List<Usuario> usuarios)`: agrupa por `clave`; también incluye a los `Usuario` con rol `CLIENTE` aunque no tengan pedidos; ordena alfabéticamente por nombre normalizado.
- `List<Pedido> pedidosDelCliente(List<Pedido> pedidos, String clave)`: todos sus pedidos, ordenados por `inicioMillis` descendente.
- `List<ResumenCliente> buscar(List<ResumenCliente> lista, String consulta)`: coincide si el nombre normalizado contiene la consulta normalizada **o** los dígitos del teléfono contienen los dígitos de la consulta (cuando la consulta tiene al menos 3 dígitos). Consulta vacía devuelve todo.

## PASO 3 — Pantallas de Clientes

### 3.1 Navegación
- Drawer del **Socio**: nuevo ítem `drawerItemClientes` ("Clientes", ícono de contactos) junto a "Equipo de trabajo", visible solo para el Socio, con el mismo comportamiento (cierra el drawer y navega a `clientesFragment`). Los demás roles no lo ven.
- `nav_graph.xml`: destinos `clientesFragment` y `clienteDetalleFragment` (argumento por `Bundle`: `"clienteKey"`), acciones `action_clientes_to_cliente`, `action_cliente_to_pedido` (hacia `pedidoDetalleFragment` con `"pedidoId"`) y `action_detalle_to_cliente` (desde `pedidoDetalleFragment`). Que ambos destinos se comporten como `usuariosFragment` en `MainActivity` (título en la barra, sin barra inferior seleccionada, "Atrás" correcto).

### 3.2 `ClientesFragment`
- Campo de búsqueda arriba (filtra en vivo con `ClienteUtils.buscar`) y `RecyclerView` debajo.
- Cada fila: nombre, teléfono con formato legible, "N pedidos · Pagado $X" y, si hay saldo pendiente, una insignia ámbar "Saldo $Y". Tocar una fila abre la ficha.
- Estado vacío "Aún no hay clientes" / "Sin resultados". Observa pedidos, pagos y usuarios; guarda la búsqueda al rotar.

### 3.3 `ClienteDetalleFragment`
- Encabezado: nombre, teléfono, correo (si existe).
- Tres tarjetas: **Pedidos**, **Pagado** (neto) y **Saldo pendiente**.
- Botones **Llamar** (`Intent.ACTION_DIAL` con `tel:`) y **WhatsApp** (`Intent.ACTION_VIEW` con `https://wa.me/52` + los 10 dígitos). Deshabilitados si no hay teléfono utilizable. Si no hay app que lo atienda, Snackbar "No se pudo abrir".
- Lista de todos sus pedidos (más reciente primero) con fecha del evento, nombre del evento, paquete, insignia de estado y total; tocar uno abre su detalle.
- En `PedidoDetalleFragment`, el nombre del cliente se vuelve tocable (indicado visualmente) y abre esta ficha con la clave calculada con `ClienteUtils.clave`.

### 3.4 Autocompletar en "Nuevo pedido"
En `NuevoPedidoFragment`, el campo del nombre del cliente pasa a ser un `MaterialAutoCompleteTextView` (umbral 2 letras) con las sugerencias "Nombre · teléfono" de `ClienteUtils.resumenes`. Al elegir una sugerencia se rellenan nombre y teléfono, y `clienteId` se manda solo si el cliente corresponde a un `Usuario`; si no, queda en `null` como hoy. Se sigue pudiendo escribir un cliente nuevo a mano. **No cambies el resto del formulario ni la lógica de la solicitud.**

## PASO 4 — Revisión en el detalle del pedido

En `PedidoDetalleFragment` y su layout, si el estado es `EN_REVISION`:
- Una **tarjeta de alerta** arriba (color ámbar del tema) con el título "Requiere tu revisión", el motivo legible (`MOTIVO_URGENTE` → texto de `strings.xml`) y dos botones: **Aprobar** y **Rechazar**.
- **Aprobar:** diálogo de confirmación; al aceptar llama a `aprobarPedido` y muestra Snackbar "Pedido aprobado".
- **Rechazar:** diálogo con campo de motivo obligatorio (error en línea si queda vacío o muy corto) y el texto "Se reembolsará $X (100% de lo pagado)" calculado con `anticipoPagado`; al aceptar llama a `rechazarPedido`.
- Mientras esté en revisión, siguen disponibles "Cobrar saldo" y "Cancelar"; se oculta "Reprogramar" (primero hay que aprobarlo).
- Un pedido `RECHAZADO` muestra su insignia, el **motivo del rechazo** y el reembolso en la sección Dinero, y no ofrece ninguna acción.

En el **Dashboard** agrega, en la sección "Operación", una tarjeta de ancho completo **"Por revisar"** con el número de pedidos `EN_REVISION` (color de error del tema si es mayor a 0). Calcula con una función nueva `DashboardUtils.contarEnRevision(List<Pedido>)`; **no cambies la firma ni el arreglo de `contadores`** (no toques su prueba).

## PASO 5 — Pruebas (`Etapa4GUnitTest.java`)

`Reloj` fijo. Mínimo:
1. `telefono10` y `clave`: `+52 747 123 4567`, `(747) 123-4567` y `7471234567` dan la misma clave; sin teléfono usa `id:`; sin ambos usa `nom:` ignorando acentos y mayúsculas.
2. `resumenes` agrupa un pedido de la app y uno del Socio con el mismo teléfono en un solo cliente, toma el nombre del más reciente y suma correctamente `pedidosValidos`, `totalPagadoNeto` (restando reembolsos) y `saldoPendiente`.
3. `resumenes` no cuenta apartados, cancelados, rechazados ni vencidos en `pedidosValidos`, pero el cliente sigue en la lista; incluye a un `Usuario` cliente sin pedidos.
4. `buscar` por nombre sin acentos y por teléfono parcial (3+ dígitos); consulta vacía devuelve todos.
5. `pedidosDelCliente` ordena por inicio descendente.
6. `aprobarPedido`: de `EN_REVISION` a `CONFIRMADO`, `requiereRevision` false, el evento y las unidades siguen intactos; de cualquier otro estado → error.
7. `rechazarPedido`: estado `RECHAZADO`, `motivoRechazo` guardado, evento `CANCELADO`, `equipoIds` vacío, y un `Pago` `REEMBOLSO` por el 100% del total pagado con folio y método del anticipo. Si hubo anticipo más abono, reembolsa la suma.
8. `rechazarPedido` sin motivo (nulo, vacío, 2 caracteres) → rechazado sin cambios; doble rechazo → error; rechazar un pedido no en revisión → error.
9. Tras rechazar, otro pedido que antes quedaba con asignación incompleta recupera sus unidades (reasignación).
10. En Finanzas, el neto del mes de un pedido rechazado con reembolso total queda en 0 para ese pedido, y `porCobrar` no lo incluye.
11. `DashboardUtils.contarEnRevision` cuenta solo `EN_REVISION`.
12. Las pruebas de Etapas 1 a 4F siguen pasando sin modificarse (si hay que tocar una, dime cuál y por qué).

Ejecuta `.\gradlew.bat test` y `.\gradlew.bat assembleDebug`. En el reporte final **da el conteo de pruebas por clase leyendo el reporte real**.

## NO HAGAS

- No crees una clase `Cliente` ni un repositorio de clientes. No guardes datos de clientes aparte de los pedidos.
- No cambies el motor de disponibilidad, la política de cancelación, la asignación de unidades ni la lógica de Finanzas.
- No inventes métodos de pago nuevos.
- No uses colores fijos, `java.time`, ni librerías nuevas ni permisos nuevos (las intents de llamar/WhatsApp no necesitan permisos).

## REPORTE FINAL QUE ESPERO

Respuestas al Paso 0, archivos creados/modificados, resultado de `test` y `assembleDebug` con el conteo real por clase, y cualquier desviación.

## CRITERIOS MANUALES (los pruebo yo en el emulador)

1. El drawer del Socio tiene "Clientes"; el del Asistente y el del Cliente no.
2. Crea dos pedidos con el mismo teléfono escrito distinto (`747 123 4567` y `(747)123-4567`) y nombres parecidos: en Clientes aparece **un solo cliente** con 2 pedidos.
3. La búsqueda encuentra por nombre (sin importar acentos o mayúsculas) y por parte del teléfono.
4. La ficha muestra los totales correctos y los pedidos más recientes primero; tocar uno abre su detalle y "Atrás" regresa a la ficha.
5. En un detalle de pedido, tocar el nombre del cliente abre su ficha.
6. Llamar y WhatsApp abren el marcador y WhatsApp (o avisan si no hay app).
7. En "Nuevo pedido", al escribir 2 letras sugiere clientes existentes y al elegir uno rellena nombre y teléfono.
8. Como no hay interfaz de Cliente todavía, para probar la revisión: crea un pedido con evento a menos de 48 h; si el Socio no genera revisión (nace exento), pídeme una pequeña herramienta de prueba temporal. Con un pedido en revisión: aparece la tarjeta ámbar con Aprobar/Rechazar.
9. Aprobar lo deja Confirmado. Rechazar pide motivo, reembolsa el 100% (aparece en el historial de pagos y en Finanzas), libera las unidades y el pedido sale de la Agenda.
10. Un pedido rechazado muestra su motivo y no ofrece acciones. Dashboard muestra la tarjeta "Por revisar" con el número correcto.
11. Todo se ve bien en modo oscuro y al girar la pantalla.
