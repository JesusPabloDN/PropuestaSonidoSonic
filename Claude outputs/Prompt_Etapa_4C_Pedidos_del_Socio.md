# ETAPA 4C — Pedidos del Socio: nuevo pedido, anticipo, confirmación automática, cancelación con reembolso y reprogramación

> Pega primero el **CONTEXTO GLOBAL** y la **ADENDA v3**. Luego pega este texto.
> Requisito: Etapas 1 a 4B terminadas y probadas (pruebas en verde).

---

## OBJETIVO

Hasta ahora el motor decide si un horario es viable (4A) y existe el personal que lo atiende (4B). Esta etapa construye la pestaña **Pedidos** del Socio para usar todo eso:

1. El Socio registra un **pedido por llamada, WhatsApp o presencial** ("Nuevo pedido"), viendo **solo horarios viables**.
2. Cada pedido nace como **apartado** (retiene equipo y personal hasta un vencimiento).
3. Al **registrar el anticipo** el pedido se **confirma solo** (sin revisión del Socio).
4. El Socio puede **cancelar** (con reembolso calculado por política) o **reprogramar**.

## IDEAS DEL NEGOCIO QUE ESTA ETAPA IMPLEMENTA

- Un pedido registrado por el propio Socio (llamada, WhatsApp, presencial) **no necesita revisión**: el Socio ya es quien decide. La marca de revisión urgente (<48 h) solo aplica a pedidos que vengan de la App del cliente (etapa futura).
- El anticipo es 30% del total (`Config.ANTICIPO_PORCENTAJE`). Con el anticipo cubierto, el pedido pasa directo a `CONFIRMADO`.
- **Política de reembolso al cancelar** (valores provisionales, a validar con los socios): si cancelan **los Socios**, se reembolsa el 100% de lo pagado. Si cancela **el cliente**: con 7 días o más de anticipación, 100%; entre 72 horas y menos de 7 días, 50%; con menos de 72 horas, 0%. Un apartado sin pago no tiene reembolso.
- Reprogramar es la alternativa amable a cancelar: mismo paquete y misma duración, otra fecha u hora viable, sin costo.
- Los pedidos que ya terminaron pasan solos a `COMPLETADO`.

## AJUSTE PREVIO (4B): fila de Disponibilidad

En `item_disponibilidad_operador.xml` el nombre se parte en dos líneas ("Socio / Principal") porque el texto "Disponible este día" compite por el espacio. Reordena la fila: a la izquierda, una columna con el **nombre** (una línea, `ellipsize="end"`) y debajo la **insignia de rol** junto al texto pequeño del switch ("Disponible este día" / "Trabaja este día"); a la derecha, solo el `MaterialSwitch`. No cambies su comportamiento.

## PASO 0 — Revisión previa (no edites todavía; entrégame hallazgos y plan breve)

Lee y repórtame:

1. `Pedido.java`, `PedidoRepository.java`, `InMemoryPedidoRepository.java`, `MotorDisponibilidad.java` y `Etapa4AMotorTest.java` (para saber qué pruebas dependen de la firma de `cancelarReserva`).
2. `Pago.java`, `PagoRepository.java` e `InMemoryPagoRepository.java`: campos actuales y si pueden representar un **anticipo** y un **reembolso** (¿hay campo de tipo, método de pago, `pedidoId`?).
3. `PedidosFragment.java` y `fragment_pedidos.xml` (hoy son un placeholder), `nav_graph.xml` (destino `nav_pedidos`) y `bottom_menu.xml`.
4. `FormatUtils.java` (qué formatos de fecha y hora existen) y cómo `InventarioFragment` y `UsuariosFragment` manejan pestañas y estado al rotar, para repetir el patrón.

## PASO 1 — Configuración y modelo

### 1.1 `util/Config.java` (comenta: `// valor provisional, validar con los socios`)

- `REEMBOLSO_TOTAL_DIAS = 7`
- `REEMBOLSO_PARCIAL_HORAS = 72`
- `REEMBOLSO_PARCIAL_PORCENTAJE = 0.5`
- `APARTADO_MANUAL_OPCIONES_HORAS = {2, 6, 12, 24, 48}` (arreglo de enteros; por defecto 24)

### 1.2 `model/Pedido.java`: campos nuevos (POJO, con copia profunda en el constructor de copia)

- `String metodoAnticipo` (constantes `METODO_EFECTIVO`, `METODO_TRANSFERENCIA`, `METODO_TARJETA`)
- `long fechaAnticipo`
- `String canceladoPor` (constantes `CANCELA_CLIENTE`, `CANCELA_SOCIO`)
- `String motivoCancelacion`
- `double reembolsoMonto`
- `int reprogramaciones`

### 1.3 Pagos

Usa `Pago`/`PagoRepository` para dejar **registro** de cada anticipo y de cada reembolso (los necesitará Finanzas). Si `Pago` no tiene campo de tipo, agrégale `String tipo` con las constantes `TIPO_ANTICIPO` y `TIPO_REEMBOLSO`, y concilia con lo que ya existe (repórtame qué cambiaste). **No crees `MovimientoFinanciero` todavía**: Finanzas se construirá después a partir de los pagos.

## PASO 2 — Lógica de negocio

### 2.1 `domain/PoliticaCancelacion.java` (clase pura, sin Android, métodos estáticos)

`double calcularReembolso(double anticipoPagado, long inicioMillis, long ahora, String canceladoPor)`:

- Si `anticipoPagado <= 0` → 0.
- Si `canceladoPor` es `CANCELA_SOCIO` → el 100% de lo pagado.
- Si es `CANCELA_CLIENTE`, con `horas = (inicioMillis − ahora) / 3 600 000`: `horas >= REEMBOLSO_TOTAL_DIAS × 24` → 100%; `horas >= REEMBOLSO_PARCIAL_HORAS` → `REEMBOLSO_PARCIAL_PORCENTAJE`; en otro caso → 0%.
- Resultado redondeado a 2 decimales.

### 2.2 `PedidoRepository` / `InMemoryPedidoRepository`

Todos los métodos de escritura van en bloque `synchronized` con `// FIRESTORE: transacción`, usan el `Reloj` y devuelven copias. Mensajes de error en español, misma convención.

**Cambios a lo existente:**

- `crearReserva`: `requiereRevision` y `motivosRevision` se guardan **solo si el origen es `ORIGEN_APP`**; para llamada, WhatsApp y presencial quedan en `false` y vacío.
- `cancelarReserva` cambia a `cancelarReserva(String pedidoId, String canceladoPor, String motivo, Callback<Pedido> callback)`. Solo desde `APARTADO`, `PAGADO`, `CONFIRMADO` o `EN_REVISION`. Calcula `reembolsoMonto` con `PoliticaCancelacion`, guarda `canceladoPor`, `motivoCancelacion` y, si `reembolsoMonto > 0`, registra un `Pago` de tipo `REEMBOLSO` (simulado: el reembolso se considera hecho al instante). Estado → `ESTADO_CANCELADO`. Actualiza las pruebas de la 4A que usaban la firma vieja.
- `liberarApartadosVencidos()`: además de pasar a `VENCIDO` los apartados vencidos, pasa a `COMPLETADO` todo pedido `CONFIRMADO` cuyo `bloqueoFinMillis <= ahora` (se usa el fin del **desmontaje**, no el fin del evento, para no liberar equipo y personal antes de que regresen). Devuelve el total de pedidos cambiados.

**Métodos nuevos:**

- `registrarAnticipo(String pedidoId, double monto, String metodo, Callback<Pedido> callback)`:
  1. Solo desde `APARTADO` y con `vencimientoApartadoMillis > ahora` (si ya venció: "El apartado ya venció").
  2. `metodo` válido; `monto > 0`.
  3. El monto debe cubrir al menos el anticipo pendiente (`anticipoRequerido − anticipoPagado`) y no puede exceder lo que falta para el total (`total − anticipoPagado`).
  4. Suma `anticipoPagado`, guarda `metodoAnticipo` y `fechaAnticipo`, registra el `Pago` de tipo `ANTICIPO`.
  5. Como el anticipo queda cubierto: estado → `ESTADO_EN_REVISION` si `requiereRevision`, y en caso contrario → `ESTADO_CONFIRMADO`. Limpia `vencimientoApartadoMillis` (pone 0).
- `reprogramarReserva(String pedidoId, long nuevoInicioMillis, Callback<Pedido> callback)`: solo desde `APARTADO` o `CONFIRMADO`. Misma duración y mismo paquete. Reevalúa con `MotorDisponibilidad.evaluar(..., ignorarPedidoId = el propio pedido)`; si no es viable → `onError(detalle)` sin cambiar nada. Si es viable: actualiza `inicioMillis`, `finMillis`, ventana de bloqueo, `operadorIds` (reasignados por el motor), suma 1 a `reprogramaciones` y actualiza `fechaActualizacion`. El vencimiento del apartado no cambia.
- Consultas síncronas para las pantallas (construyen el contexto internamente, para que la interfaz no toque el motor):
  - `ResultadoDisponibilidad evaluarDisponibilidad(String paqueteId, long inicioMillis, int duracionMin, String ignorarPedidoId)`
  - `List<Long> consultarHorarios(String paqueteId, long unMomentoDelDia, int duracionMin, String ignorarPedidoId)`
  - `List<Long> consultarAlternativas(String paqueteId, long desdeMillis, int duracionMin, int max)`
  - `double calcularReembolsoPrevio(String pedidoId, String canceladoPor)` (para mostrar el monto antes de confirmar la cancelación).

**Reglas de las consultas:** `consultarHorarios` y `evaluarDisponibilidad` reciben `ignorarPedidoId` para poder reprogramar sin chocar con el propio pedido; pásalo al motor (puede ser nulo).

## PASO 3 — Navegación

En `nav_graph.xml`, dentro de `socio_graph`:

- Destino `nuevoPedidoFragment` (clase `ui.socio.NuevoPedidoFragment`, layout `fragment_nuevo_pedido`).
- Destino `pedidoDetalleFragment` (clase `ui.socio.PedidoDetalleFragment`, layout `fragment_pedido_detalle`) con argumento `pedidoId` (string, no nulo). Sin Safe Args: viaja en un `Bundle` con la clave `"pedidoId"`.
- Acciones desde `nav_pedidos`: `action_pedidos_to_nuevoPedido` y `action_pedidos_to_detalle`; y desde `nuevoPedidoFragment` hacia `pedidoDetalleFragment` con `popUpTo = nuevoPedidoFragment` y `popUpToInclusive = true` (al terminar un pedido nuevo, "Atrás" regresa a la lista, no al formulario).
- `MainActivity` **no cambia**: estos destinos caen en la rama `else` (barra superior con flecha, barra inferior oculta). Verifica que "Atrás" desde Detalle y desde Nuevo pedido regresa a Pedidos, y desde Pedidos a Dashboard.

## PASO 4 — Lista de Pedidos (`PedidosFragment`)

Reemplaza el placeholder. Estructura: `TabLayout` `tabsPedidos` (fixed, fill) con tres pestañas y un solo `RecyclerView` `rvPedidos` que se filtra según la pestaña (más estado vacío por pestaña y FAB `fabNuevoPedido` con el mismo relleno inferior de siempre):

| Pestaña | Estados | Orden |
|---|---|---|
| **Por pagar** | `APARTADO` | Por vencimiento, el que vence primero arriba |
| **Confirmados** | `CONFIRMADO`, `PAGADO`, `EN_REVISION` | Por fecha del evento, el más próximo arriba |
| **Historial** | `COMPLETADO`, `CANCELADO`, `RECHAZADO`, `VENCIDO` | Por fecha de actualización, el más reciente arriba |

- Guarda la pestaña elegida en un campo y en `onSaveInstanceState`.
- **Tarjeta `item_pedido.xml`** (mismo estilo que `item_equipo.xml`), con: nombre del evento (negrita), folio ("#12"), insignia de estado, fecha y horario ("sáb 5 dic 2026 · 19:00–22:00"), nombre del paquete, nombre del cliente y una línea de dinero. En **Por pagar**, la línea de dinero dice "Anticipo pendiente: $1,110.00" y otra línea "Vence en 23 h 10 min" (si le quedan menos de 2 horas, texto con el color `status_danado`). En **Confirmados**: "Anticipo pagado · Saldo: $2,590.00".
- Colores de insignia (reutiliza `BadgeUtils.pintar` y los `status_*`; siempre con texto): `APARTADO` y `EN_REVISION` → `status_pendiente`; `CONFIRMADO` y `PAGADO` → `status_disponible`; `COMPLETADO` → `status_rentado`; `CANCELADO` y `RECHAZADO` → `status_danado`; `VENCIDO` → neutro (`colorSurfaceVariant`).
- Mientras la pantalla está visible, refresca la lista cada 30 segundos (un `Handler` que se detiene en `onPause`) para que los "vence en…" se mantengan al día. También refresca al cambiar el `LiveData`.
- Tocar una tarjeta abre el detalle. Tocar el FAB abre "Nuevo pedido".

## PASO 5 — Nuevo pedido (`NuevoPedidoFragment`, pantalla completa)

`fragment_nuevo_pedido.xml` en un `NestedScrollView`, con los campos en este orden. Guarda el estado en un `ViewModel` (`NuevoPedidoViewModel`) para sobrevivir a la rotación.

1. **Paquete** (`MaterialAutoCompleteTextView` de solo selección, con los paquetes **activos**, texto "Nombre · $precio"). Si no hay paquetes activos, muestra un aviso "Primero crea un paquete en Inventario" y deshabilita el resto.
2. **Duración** (lista de `DURACION_MIN_MIN` a `DURACION_MAX_MIN` cada `PASO_DURACION_MIN`, texto "4 h", "4 h 30 min"…). Al elegir paquete, se preselecciona su `duracionBaseHoras`.
3. **Fecha:** botón que abre `MaterialDatePicker` (solo hoy en adelante). Recuerda la conversión UTC → local con `TiempoUtils.construir` que ya usas en Disponibilidad.
4. **Horarios disponibles:** `ChipGroup` (selección única) con los inicios devueltos por `consultarHorarios` ("19:00"). Se recalcula cuando cambia paquete, duración o fecha, y limpia la selección si el horario elegido ya no es viable.
   - Si no hay horarios ese día: texto "No hay horarios disponibles ese día" y, debajo, chips con las próximas fechas y horas de `consultarAlternativas` (hasta 5); tocar uno rellena fecha y horario.
5. **Resumen en vivo** (visible cuando hay horario elegido): fin del evento, montaje desde, desmontaje hasta, **personal asignado** (nombres, obtenidos de `UsuarioRepository` con los `operadorIds` de `evaluarDisponibilidad`), total y anticipo requerido (con `calcularTotal`/`calcularAnticipo` del motor).
6. **Datos del evento y del cliente:** nombre del evento (máx. 80), nombre del cliente, teléfono (mín. 10 dígitos), dirección (mín. 10 caracteres), notas (opcional).
7. **Cómo llegó el pedido:** `MaterialButtonToggleGroup` con Llamada | WhatsApp | Presencial (por defecto Llamada).
8. **Retener el apartado por:** lista con `APARTADO_MANUAL_OPCIONES_HORAS` ("2 horas", "6 horas", "12 horas", "24 horas", "48 horas"; por defecto 24).
9. Botón **"Apartar"** (alto `button_height`): valida en pantalla (errores bajo cada campo, con mensajes de `strings.xml`) y llama a `crearReserva`. En éxito: navega al detalle del pedido nuevo (acción con `popUpTo` del Paso 3) con un Toast "Apartado creado". En error: diálogo con el mensaje del repositorio, sin borrar lo capturado. Deshabilita el botón mientras procesa y cierra el teclado.

## PASO 6 — Detalle del pedido (`PedidoDetalleFragment`)

`fragment_pedido_detalle.xml` en un `NestedScrollView`. Carga el pedido con `getPedidoPorId` y observa `getPedidos()` para refrescarse cuando cambie (si el pedido ya no existe: Toast y `popBackStack()`). Secciones:

- **Encabezado:** nombre del evento, folio, insignia de estado y, si es `APARTADO`, "Vence en …".
- **Cliente:** nombre, teléfono, dirección, origen ("Llamada", "WhatsApp", "Presencial" o "App") y notas.
- **Paquete y equipo:** nombre del paquete y la lista de componentes ("2 × Bocina JBL 15\"…") desde la copia guardada en el pedido.
- **Horario:** fecha, hora de inicio y fin, montaje desde, desmontaje hasta.
- **Personal asignado:** nombres.
- **Dinero:** total, anticipo requerido, pagado (con método y fecha) y saldo pendiente (`total − anticipoPagado`). Si hubo reembolso: "Reembolso: $X (simulado)".
- **Cancelación:** si está cancelado, quién canceló y el motivo.

**Botones según estado** (en una barra de acciones al final del contenido, 48dp o más):

| Estado | Acciones |
|---|---|
| `APARTADO` | **Registrar anticipo**, **Reprogramar**, **Cancelar apartado** |
| `CONFIRMADO` | **Reprogramar**, **Cancelar pedido** |
| Los demás | Ninguna (solo lectura) |

- **Registrar anticipo** (diálogo): campo de monto (prellenado con el anticipo pendiente) y selector de método (Efectivo, Transferencia, Tarjeta). Llama a `registrarAnticipo`. En éxito, el pedido pasa a Confirmado y el detalle lo refleja.
- **Reprogramar** (pantalla/diálogo): elige nueva fecha y luego un horario con los mismos chips que Nuevo pedido, usando `consultarHorarios(..., ignorarPedidoId = este pedido)`. Confirma con `reprogramarReserva`. Los errores se muestran en diálogo.
- **Cancelar** (diálogo): pregunta **"¿Quién cancela?"** con dos opciones (Cliente / Nosotros, los Socios) y un campo de motivo (opcional). Muestra el **reembolso** que corresponde (`calcularReembolsoPrevio`) y pide confirmar. Un apartado sin pago muestra "Sin reembolso".

Reutiliza el código del selector de fecha y horarios entre Nuevo pedido y Reprogramar (una clase auxiliar o un componente), no lo dupliques.

## PASO 7 — Textos, formatos e iconos

- Todos los textos en `strings.xml` (español de México), incluidos los nombres de estados, orígenes, métodos de pago y los mensajes de validación.
- `FormatUtils`: agrega `fechaHoraCorta(long)` ("sáb 5 dic 2026 · 19:00"), `rangoHorario(long inicio, long fin)` ("19:00–22:00") y `tiempoRestante(long millis)` ("23 h 10 min", "45 min", "Vencido"), siempre con la zona de `Config.ZONA_HORARIA` y `Locale("es","MX")`.
- Sin colores fijos; zonas táctiles de 48dp o más; texto de 14sp o más; `contentDescription` en iconos; todo legible en claro y oscuro.

## PASO 8 — Pruebas unitarias (obligatorias)

Crea `Etapa4CUnitTest.java` (con `InstantTaskExecutorRule` y el `Reloj` falso). Mínimo:

1. `PoliticaCancelacion`: Socio cancela → 100%; Cliente con 10 días → 100%; con exactamente 7 días → 100%; con 5 días → 50%; con exactamente 72 horas → 50%; con 71 horas → 0%; sin anticipo → 0; redondeo a 2 decimales.
2. `crearReserva` con origen Llamada a menos de 48 horas: `requiereRevision = false`. Con origen App a menos de 48 horas: `requiereRevision = true` y `MOTIVO_URGENTE`.
3. `registrarAnticipo`: monto menor al anticipo pendiente → error; monto mayor al saldo del total → error; monto correcto → estado `CONFIRMADO`, `anticipoPagado` actualizado, vencimiento en 0 y un `Pago` de tipo `ANTICIPO`. Con origen App urgente y `requiereRevision`, queda en `EN_REVISION`.
4. `registrarAnticipo` sobre un apartado vencido (reloj avanzado) → error; sobre un pedido ya confirmado → error.
5. `cancelarReserva`: Socio cancela con anticipo pagado → reembolso 100% y un `Pago` de tipo `REEMBOLSO`; Cliente cancela con 4 días → 50%; Cliente con 2 días → 0 y sin `Pago` de reembolso; apartado sin pago → sin reembolso. Después de cancelar, el horario se libera y otra reserva sí cabe. Cancelar un pedido ya cancelado → error.
6. `reprogramarReserva`: a un horario viable → cambia inicio, fin, ventana y operadores, `reprogramaciones` = 1; a un horario que choca con otra reserva → error y el pedido **no cambia**; a un horario que solo choca con su propia ventana anterior → permitido; en el pasado → error; desde `CANCELADO` → error.
7. `liberarApartadosVencidos`: un `CONFIRMADO` pasa a `COMPLETADO` **solo cuando el reloj supera `bloqueoFinMillis`**, no cuando termina el evento (probar el caso intermedio: pasado el fin del evento pero antes del fin del desmontaje, sigue `CONFIRMADO` y sigue bloqueando).
8. `consultarHorarios` y `evaluarDisponibilidad` del repositorio coinciden con los del motor para un mismo escenario, y respetan `ignorarPedidoId`.
9. Las pruebas de las etapas 1 a 4B siguen pasando (ajusta mínimamente las de la 4A por el cambio de firma de `cancelarReserva`).

## ALCANCE: LO QUE NO SE HACE

Sin pantalla ni lógica de **revisión** (`EN_REVISION`, aprobar o rechazar), sin solicitud especial ni cupo extra, sin cliente en la app, sin tarjeta real, sin cobro del saldo restante ni Finanzas, sin Agenda, sin crear `Evento` ni asignar unidades concretas (se hará en una etapa posterior; hoy Inventario **no** mostrará "Próxima reserva" de estos pedidos), sin lista de Clientes, sin datos de ejemplo. No toques `EventoRepository`.

## CRITERIOS DE ACEPTACIÓN (en este orden)

Prepara: en Inventario, 4 × Bocina, 2 × Micrófono y 1 × Mezcladora; un paquete "Básico" (2 Bocinas, 1 Mezcladora, 2 Micrófonos, 1 persona) con precio $3,500 y 4 horas; un segundo paquete "Solo bocinas" (1 Bocina, 1 persona) con precio $800. En Equipo de trabajo, crea un segundo Socio.

1. La pestaña Pedidos muestra las tres pestañas con su estado vacío y el botón "+" (sin tapar tarjetas).
2. **Nuevo pedido:** elige "Básico", duración 4 h, una fecha futura: aparecen chips de horarios (de 09:00 a 22:00 cada 30 min). Elige las 19:00: el resumen muestra fin 23:00, montaje desde 17:30, desmontaje hasta 01:00, el personal asignado, total $3,500 y anticipo $1,050. Llena los datos y toca "Apartar": abre el detalle con estado Apartado y "Vence en 23 h…".
3. Regresa con "Atrás": llegas a la lista de Pedidos (no al formulario), con el pedido en **Por pagar** y su cuenta regresiva.
4. Crea otro pedido "Básico" a las 19:00 del mismo día: ahora **no** es posible (los Micrófonos y la Mezcladora no alcanzan para dos pedidos a la vez) y las 19:00 ya no aparece como chip; pero un pedido de "Solo bocinas" a las 19:00 **sí** es posible (1 Bocina, otro Socio). Un tercer pedido simultáneo de "Solo bocinas" no es posible con solo 2 Socios (sin personal) y no aparece el horario; si marcas a un Asistente como disponible ese día en Equipo de trabajo, vuelve a aparecer.
5. En un día sin horarios, aparece el mensaje y las **fechas alternativas**; tocar una rellena fecha y horario.
6. **Registrar anticipo:** en el detalle del primer pedido, intenta $500 → error (no cubre el anticipo); registra $1,050 en Efectivo → pasa a **Confirmado**, desaparece de Por pagar y aparece en Confirmados con "Anticipo pagado · Saldo: $2,450.00".
7. **Reprogramar** el pedido confirmado a otro día y hora viables: se actualiza el horario. Reprogramarlo a un horario que choque con otro pedido da error y no cambia nada.
8. **Cancelar** un pedido confirmado con evento a más de 7 días: eligiendo "Cliente", el diálogo muestra reembolso del 100% ($1,050.00); eligiendo "Nosotros", también 100%. Confirma: pasa a Historial como Cancelado y muestra el reembolso. Repite con un evento a 4 días y "Cliente": 50%. A menos de 72 horas y "Cliente": $0.00. Tras cancelar, el horario vuelve a estar disponible.
9. **Vencimiento:** crea un apartado de 2 horas (en pruebas puedes bajar temporalmente a 2 min `APARTADO_APP_MIN` y el mínimo de opciones; revierte el cambio después). Al vencer, pasa a Historial como Vencido y libera el horario; la cuenta regresiva de la lista se mantiene al día sin salir de la pantalla.
10. Rota el teléfono en la lista (se conserva la pestaña), en Nuevo pedido (se conservan paquete, fecha, horario y datos) y en el detalle.
11. Modo oscuro: todo legible; sin colores fijos.
12. `./gradlew test` y `./gradlew assembleDebug` pasan (en Windows `.\gradlew.bat ...`), incluidas las pruebas de todas las etapas anteriores.

## AL TERMINAR

Entrégame: (a) hallazgos del Paso 0 (cómo era `Pago` y qué cambiaste), (b) archivos creados y modificados, (c) decisiones fuera de lo descrito, (d) salida de `test` y `assembleDebug` con la lista de pruebas, y (e) cómo probaste cada criterio y cualquier ambigüedad que resolviste.
