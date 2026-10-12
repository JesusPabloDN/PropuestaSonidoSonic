# Sonido Sonic — Núcleo del Socio (v2)

Este archivo **reemplaza las Etapas 2 a 7** del archivo anterior (`Prompts_Antigravity_Socio_SonidoSonic.md`) y recoge la distribución acordada para cada pestaña. La Etapa 1 y la Etapa 1.1 siguen vigentes.

**Cómo usarlo:** pega el **CONTEXTO GLOBAL** del archivo anterior, luego la sección **Ajustes al contexto global** de este archivo, y por último el prompt de la etapa. Una etapa a la vez; no avances hasta cumplir sus criterios de aceptación.

Orden: **2** Inventario → **3** Pedidos → **4** Agenda → **5** Finanzas y recibo PDF → **6** Dashboard. Pedidos va antes que Agenda porque la Agenda muestra los eventos que nacen al confirmar un pedido, y el Dashboard va al final porque resume todo lo demás.

---

## Comparación con los documentos (qué cambió y por qué)

| Tema | Propuesta / A2.2 | Diseño acordado ahora | Decisión |
|---|---|---|---|
| Nuevo pedido | Cliente, canal, horario, dirección, partidas, paquetes, anticipo | Nombre del evento, fecha y equipo (Spinner de equipo disponible) | Se sigue lo acordado; se añade "Cliente (opcional)" para el recibo. Lo demás pasa a complementos. |
| Agenda | Calendario con alta manual de eventos | Calendario con los eventos **confirmados**, filtrable por día o por mes | Cada pedido confirmado genera su evento; no hay alta manual (RF-AGE-02 queda como complemento). |
| Inventario | Alta/edición, bitácora, foto en Storage | Tarjetas con imagen, estado, búsqueda por nombre y filtro por categoría | Foto desde galería (local) o imagen por defecto de la categoría. Bitácora pasa a complementos. |
| Finanzas | Ingresos, gastos, balance por período, gráficas | Eventos con monto, botón de recibo y ganancias del mes cobradas | Se sigue lo acordado; el cobro genera un ingreso para que los gastos se sumen después sin rehacer nada. |
| Dashboard | Eventos de la semana, equipo fuera, balance, más rentado, más fallas | Resumen general de las demás pestañas | Resumen con tarjetas que llevan a cada pestaña; "más rentado/más fallas" queda para cuando exista la bitácora. |

**Complementos pendientes (en este orden de prioridad):**
1. Usuarios y Clientes (el Socio crea otros Socios y Técnicos; clientes visibles tras una venta cerrada). Sin esto no se puede probar el rol Técnico.
2. Datos del evento: horario y dirección.
3. Bitácora de fallas y mantenimiento por equipo.
4. Checklist de salida/regreso, daños y asignación de Técnico.
5. Anticipos y pagos parciales.
6. Gastos manuales y balance con gráficas.
7. Paquetes de equipo.
8. Cotización en PDF, WhatsApp, recordatorios y notificaciones.

---

## Ajustes al contexto global (pegar justo después del contexto global)

1. **Regla 6 modificada.** Ahora **se permiten**: la librería **Glide** (versión estable) para mostrar imágenes y el **Photo Picker** del sistema (`ActivityResultContracts.PickVisualMedia`) para elegir una foto de la galería. Sigue **prohibido**: cámara, Kotlin, Compose, Firebase, Room, Hilt/Dagger, Safe Args, librerías de gráficas, notificaciones push, Google Sign-In.
2. **Datos y recursos.** Las imágenes por defecto de cada categoría no cuentan como datos falsos. Ya están en `res/drawable`: `bocinajbl.jpg`, `subwoofer.jpg`, `microfonosm.jpg`, `mezcladora.jpg`, `controlador.jpg` y `lucesled.jpg`. No las reemplaces ni las borres.
3. **Eventos de día completo.** Por ahora un evento solo tiene fecha. Su inicio es esa fecha a las 00:00:00.000 y su fin a las 23:59:59.999 (en `inicioMillis` y `finMillis`). La regla de traslape no cambia (`inicioA < finB && inicioB < finA`), así que luego se podrán agregar horas sin tocar la lógica.
4. **Cambios en el modelo.** Agrega, sin borrar nada de lo existente: `Equipo.fotoUri` (String, nullable), `Pedido.nombreEvento` (String), `Pedido.equipoIds` (`List<String>` con los ids de unidades), `Pedido.cobrado` (boolean) y `Pedido.fechaCobro` (long). `Partida` y los demás campos quedan sin uso hasta los complementos.
5. **Estados de pedido en esta fase:** `PENDIENTE` → `CONFIRMADO` → `PAGADO` (= cobrado) y `CANCELADO`.
6. **Categorías de equipo (constantes en un solo lugar):** Bocinas, Subwoofers, Micrófonos, Mezcladoras, Controladores, Luces, Cableado, Accesorios, Otro. Imagen por defecto: Bocinas→`bocinajbl`, Subwoofers→`subwoofer`, Micrófonos→`microfonosm`, Mezcladoras→`mezcladora`, Controladores→`controlador`, Luces→`lucesled`; Cableado, Accesorios y Otro usan un ícono vectorial genérico.
7. **Listas con FAB.** Todas las listas que tengan un botón flotante deben tener `paddingBottom` suficiente y `clipToPadding="false"` para que el FAB nunca tape el último elemento ni sus botones.
8. **Fechas y moneda.** Formato en español de México (por ejemplo "sáb 15 oct 2026") y moneda `es-MX`.

---

## ETAPA 2 — Inventario

**Objetivo.** Pestaña **Inventario** del Socio: tarjetas con imagen, nombre y estado de todo el equipo, con búsqueda por nombre y filtro por categoría, y alta/edición de equipo.

**Barra de búsqueda y filtro (arriba de la lista).** Una sola barra con dos partes: a la izquierda un campo de texto con ícono de lupa y el texto "Buscar por nombre"; a la derecha, **dentro de la misma barra**, un **Spinner** de categoría ("Todas" + las categorías del ajuste 6). La búsqueda filtra en vivo por nombre ignorando mayúsculas y acentos y se combina con la categoría. La barra se queda fija y solo se desplaza la lista. Al limpiar el texto vuelve a verse todo.

**Tarjetas.** Una por unidad de equipo, ordenadas por nombre y luego por número de unidad:
- Imagen a la izquierda (72dp, esquinas redondeadas): la foto elegida (`fotoUri`, con Glide) o, si no hay, la imagen por defecto de su categoría.
- Nombre con número de unidad, por ejemplo `Bocina JBL 15" (3)`, categoría y precio de renta por evento.
- **Estado con texto y color**: Disponible, Rentado, En reparación o Dañado. *Rentado* es **derivado**: la unidad está en un evento no cancelado que cae en la fecha de hoy. Si la unidad tiene una reserva futura, una segunda línea indica "Próxima reserva: 15 oct 2026 · nombre del evento".
- Estados vacíos distintos: "Aún no hay equipo registrado. Toca + para agregar" y "Sin resultados para tu búsqueda".

**Nuevo equipo (FAB +).** Formulario (hoja inferior o pantalla) con: nombre/modelo (obligatorio), categoría (Spinner), precio de renta por evento (≥ 0), estado físico inicial (Disponible / En reparación / Dañado), **cantidad de unidades iguales** (1 a 50, por defecto 1) y **foto opcional** (botón "Elegir foto" con Photo Picker, vista previa y "Quitar foto"). Con cantidad N se crean N unidades con numeración consecutiva que **continúa** la existente para ese nombre (sin distinguir mayúsculas), y todas comparten la foto elegida.

**Editar y eliminar.** Al tocar una tarjeta se abre el mismo formulario con los datos de esa unidad (sin el campo de cantidad). Se puede cambiar nombre, categoría, precio, foto y **estado físico** (Disponible / En reparación / Dañado; "Rentado" no se elige). Botón **Eliminar** con confirmación; si la unidad está en un evento no cancelado, no se puede eliminar y se explica en cuál. Si se pone En reparación o Dañado una unidad con reservas futuras, mostrar una advertencia con esos eventos, pero permitir el cambio.

**Lógica en repositorio.** `EquipoRepository`: `agregarUnidades`, `actualizar`, `eliminar`, lista como `LiveData` y `estadoEfectivo(unidad, ahoraMillis)` y `proximaReserva(unidadId, ahoraMillis)`, apoyados en `EventoRepository` (que añade `reservasVigentes(ahora)` y `reservasFuturas(unidadId, ahora)`; mientras no haya eventos devuelven vacío). Las fotos solo guardan la URI como texto en `fotoUri`; en la fase Firebase pasará a ser una URL de Storage. Como los datos son volátiles, las fotos se pierden al cerrar la app.

**Criterios de aceptación.**
1. Con la app vacía se ve el estado vacío; el FAB no tapa nada.
2. Crear 3 unidades de "Bocina JBL 15"" genera las unidades 1, 2 y 3; crear 2 más continúa en 4 y 5.
3. Cada tarjeta muestra su imagen: la foto elegida o la imagen de la categoría; Cableado/Accesorios/Otro muestran el ícono genérico.
4. Escribir en la barra filtra en vivo; el Spinner filtra por categoría; ambos combinados funcionan; "Todas" restablece.
5. Cambiar el estado físico de una unidad se refleja al instante en su tarjeta, con texto y color.
6. Eliminar pide confirmación. Rotar la pantalla conserva búsqueda, categoría y formulario.

---

## ETAPA 3 — Pedidos

**Objetivo.** Pestaña **Pedidos** con dos vistas, creación de pedidos, confirmación que reserva el equipo y pantalla de detalles. Trabaja sobre las etapas 1 y 2.

**Estructura.** `TabLayout` + `ViewPager2` con dos pestañas: **Pendientes por confirmar** y **Confirmados**. FAB **+ Nuevo pedido** (ver ajuste 7 sobre el padding de la lista).

**Pendientes por confirmar.** Cada tarjeta muestra el **nombre del evento**, la **fecha**, cuántos equipos ("3 equipos") y el cliente si lo hay. Un botón **Confirmar** por tarjeta y un menú ⋮ con **Editar** y **Eliminar** (con confirmación). Si el pedido supera `Config.RECORDATORIO_HORAS` sin respuesta, muestra la etiqueta "Sin respuesta +24 h" (con texto, no solo color). Ordenados por fecha del evento. Estado vacío.

**Nuevo pedido y Editar pedido (mismo formulario, pantalla secundaria con flecha de regreso).** Campos:
1. **Nombre del evento** (obligatorio).
2. **Cliente** (opcional, texto libre).
3. **Fecha** (DatePicker; hoy o futura; obligatoria).
4. **Equipo**: un **Spinner** "Agregar equipo", deshabilitado hasta que haya fecha. Lista las unidades **disponibles para esa fecha**: estado físico Disponible, sin reserva en un evento no cancelado ese día y que no estén ya agregadas. Cada opción se ve como `Bocina JBL 15" (3) · Bocinas · $500`, ordenadas por categoría y nombre. Al elegir una, **se agrega a la lista del pedido** y el Spinner vuelve a su texto inicial. La lista muestra cada unidad con botón de quitar. Si cambia la fecha con equipo ya agregado, se vuelve a validar: se quitan las unidades que dejaron de estar disponibles y se avisa cuáles.
5. **Total**: campo numérico que se rellena solo con la suma de los precios de renta de las unidades y se actualiza al agregar o quitar equipo, **hasta que el usuario lo edite a mano**.
- Debe haber al menos una unidad. Al guardar nace `PENDIENTE` con `fechaCreacion`, número consecutivo (`numero`, desde 1001) y `fechaActualizacion`.

**Confirmar (operación atómica y sincronizada en `PedidoRepository`).** Para cada unidad del pedido verifica estado físico Disponible y que no esté en un evento no cancelado ese mismo día. Si algo falla, **no cambia nada** y el mensaje nombra la unidad y el evento en conflicto ("Bocina JBL 15" (3) ya está reservada el 15 oct 2026 en «Boda de Ana»"). Si todo es válido, **crea el `Evento`** (nombre = nombre del evento, día completo según el ajuste 3, `equipoIds`, `pedidoId`, socio responsable = sesión, estado `PROGRAMADO`), guarda `eventoId` en el pedido, pasa el pedido a `CONFIRMADO` y muestra "Pedido confirmado y equipo reservado". El pedido pasa a la pestaña Confirmados. Comentar con `// FIRESTORE: transacción`.

**Confirmados.** Tarjeta con nombre del evento, fecha, estado (Confirmado o Cobrado, con texto y color) y un botón **Detalles**. Orden: fecha ascendente. Estado vacío.

**Detalles (pantalla secundaria).** Nombre del evento, fecha, cliente, estado, **lista de equipo solicitado** (nombre con unidad, categoría, precio), total y estado de cobro. Botón **Cancelar pedido** (con confirmación; solo si no está cobrado): pasa a `CANCELADO`, cancela su evento y **libera el equipo**. Los pedidos cancelados no aparecen en las listas.

**Criterios de aceptación.**
1. Crear un pedido con 2 unidades lo muestra en Pendientes con su nombre, fecha y "2 equipos"; el Spinner solo ofrece equipo disponible y no repite unidades agregadas.
2. Una unidad En reparación o Dañado no aparece en el Spinner.
3. Confirmar mueve el pedido a Confirmados y las unidades quedan reservadas; crear otro pedido para la **misma fecha** ya no ofrece esas unidades.
4. Dos pedidos pendientes con la misma unidad y fecha: confirmar el primero funciona; confirmar el segundo es rechazado con el mensaje exacto, y al **editarlo** y quitar la unidad ya se puede confirmar.
5. Detalles muestra el equipo solicitado. Cancelar un confirmado libera las unidades (vuelven a ofrecerse en el Spinner para esa fecha).
6. El FAB nunca tapa un botón; rotar no pierde lo escrito en el formulario.

---

## ETAPA 4 — Agenda

**Objetivo.** Pestaña **Agenda**: un calendario mensual junto a la lista de eventos confirmados, filtrable por día o por mes. Trabaja sobre las etapas 1 a 3. **No hay alta manual de eventos**: nacen al confirmar un pedido.

**Calendario propio (no uses `CalendarView`, no avisa cuando cambia el mes).** Implementa un `MonthCalendarView` o fragmento equivalente con `RecyclerView` + `GridLayoutManager` de 7 columnas:
- Encabezado: `‹  Octubre de 2026  ›` (español, primera letra en mayúscula), flechas con área táctil de 48dp, y un botón de texto **Hoy**.
- Fila de iniciales D L M M J V S (la semana empieza en domingo).
- Cada día muestra su número. Hoy se distingue con un contorno; el día seleccionado, relleno con el color primario del tema; los días **con eventos** llevan un punto indicador. Descripción de accesibilidad por día ("15, 2 eventos").
- Colores desde atributos de tema, nada fijo.

**Comportamiento.**
- Al abrir: mes actual con hoy seleccionado; la lista muestra los eventos de hoy (título "Eventos de hoy" o "Eventos del sáb 15 de octubre").
- Tocar un día lo selecciona y la lista muestra **solo los eventos de ese día**.
- Tocar de nuevo el día seleccionado lo deselecciona y pasa a **modo mes**.
- Cambiar de mes con las flechas pasa a **modo mes**: la lista muestra **todos los eventos de ese mes** (título "Eventos de octubre de 2026"), ordenados por fecha.
- **Hoy** vuelve al mes actual con hoy seleccionado.
- Los eventos aparecen y desaparecen solos (LiveData) al confirmar o cancelar pedidos.
- El mes visible y el día seleccionado sobreviven a la rotación (`ViewModel`).

**Lista de eventos.** Solo eventos no cancelados. Cada tarjeta: nombre, fecha, "N equipos", cliente si lo hay y estado de cobro (Por cobrar / Cobrado) obtenido del pedido. Al tocarla se abre la **pantalla Detalles del pedido** de la Etapa 3. Estados vacíos distintos para día y para mes ("No hay eventos este día" / "No hay eventos este mes").

**Criterios de aceptación.**
1. Con la app vacía el calendario se ve, sin puntos, con el estado vacío.
2. Confirmar un pedido para el día 20 pone un punto en el 20 y el evento aparece en la lista al seleccionarlo, sin recargar.
3. Cambiar al mes siguiente muestra solo los eventos de ese mes; volver al mes actual muestra los de ese mes.
4. Seleccionar un día y tocarlo otra vez regresa al modo mes. **Hoy** regresa a hoy.
5. Cancelar un pedido desde Detalles quita el evento y su punto.
6. La rotación conserva mes y día; el calendario se ve bien en modo claro y oscuro.

---

## ETAPA 5 — Finanzas y recibo en PDF

**Objetivo.** Pestaña **Finanzas**: eventos con su monto, cobro y recibo, y las ganancias del mes. Trabaja sobre las etapas 1 a 4.

**Encabezado.** Selector de mes `‹  Octubre de 2026  ›` (mes actual por defecto) y una tarjeta grande **Ganancias del mes**: suma de los totales de los pedidos **cobrados** cuyo evento cae en ese mes. Debajo, dos datos: "Cobrados: N" y "Por cobrar: $X (M eventos)". Todo calculado por **mes del evento** y actualizado en vivo.

**Lista.** Un filtro **Spinner** "Todos / Por cobrar / Cobrados". Cada tarjeta muestra el **nombre del evento**, su fecha, el cliente si lo hay, el **monto total a pagar** y el estado con texto y color (**Por cobrar** / **Cobrado**). Se listan los pedidos Confirmados y Pagados cuyo evento cae en el mes elegido, ordenados por fecha. Estado vacío. Botones por tarjeta:
- **Marcar como cobrado** (solo si está por cobrar): confirmación "¿Confirmas que se cobraron $X?". Una sola operación sincronizada en el repositorio: crea un `Pago` (tipo `TOTAL`, método `EFECTIVO`), crea un `MovimientoFinanciero` `INGRESO` (categoría `RENTA`, concepto "Cobro: <nombre del evento>", con `pedidoId` y `pagoId`), pone el pedido en `PAGADO`, `cobrado = true` y `fechaCobro = ahora`. No se puede deshacer en esta fase.
- **Recibo**: genera y comparte un PDF (disponible en ambos estados).

**Recibo PDF** con `android.graphics.pdf.PdfDocument`, sin librerías externas: encabezado "Sonido Sonic · Recibo", folio (número de pedido), fecha de emisión, cliente (o "—"), nombre y fecha del evento, tabla de equipo (nombre con unidad, categoría y precio), total y el estado ("PAGADO el 15/10/2026" o "PENDIENTE DE PAGO"). Se genera **fuera del hilo principal** (`Executor`) con indicador de progreso, se guarda en la caché de la app y se comparte con `FileProvider` + `ACTION_SEND` (`application/pdf`); registra el `FileProvider` y su `file_paths.xml` en el manifest. Textos en `strings.xml`.

**Criterios de aceptación.**
1. Sin pedidos confirmados, Finanzas muestra ceros y estado vacío.
2. Un pedido confirmado aparece como **Por cobrar** con su monto; "Por cobrar" lo incluye.
3. **Marcar como cobrado** (con confirmación) lo pasa a **Cobrado**, sube **Ganancias del mes** y se refleja en Detalles de Pedidos (estado Cobrado) y en Agenda.
4. El filtro y el selector de mes recalculan lista y tarjetas.
5. **Recibo** abre el selector de compartir con un PDF correcto en ambos estados y no congela la interfaz.
6. El pedido cobrado ya no puede cancelarse.

---

## ETAPA 6 — Dashboard

**Objetivo.** Pestaña **Dashboard**: un resumen general y relevante de las demás pestañas. Trabaja sobre las etapas 1 a 5. Sin librerías de gráficas.

**Contenido** (tarjetas que se actualizan solas con LiveData; **cada tarjeta, al tocarla, lleva a su pestaña** usando el mismo mecanismo de la barra inferior, para no romper la lógica de "Atrás" de la Etapa 1.1):
- **Eventos de la semana**: cantidad de eventos no cancelados de la semana actual (domingo a sábado, igual que el calendario) y "Próximo evento: nombre · fecha" o "Sin eventos próximos". → Agenda.
- **Equipo**: cuatro contadores de unidades por estado efectivo: Disponible, **Equipo fuera** (Rentado hoy), En reparación y Dañado. → Inventario.
- **Pedidos**: "Por confirmar: N" (resaltando cuántos tienen el recordatorio vencido) y "Confirmados próximos: M". → Pedidos.
- **Finanzas**: **Ganancias del mes** (cobrados) y **Por cobrar** (suma de totales de pedidos confirmados sin cobrar). → Finanzas.
- **Cobros de los últimos 7 días**: una fila por día (etiqueta corta del día y monto) con una barra horizontal proporcional (`ProgressBar`), calculado por `fechaCobro`.
- **Próximos eventos**: los siguientes 3 eventos con nombre y fecha. → Agenda.
- Con la app vacía todo muestra ceros o "Sin datos" y un texto guía ("Empieza registrando equipo en Inventario").

**Criterios de aceptación.**
1. Con la app vacía todas las tarjetas muestran ceros y textos guía, sin errores.
2. Registrar equipo, crear y confirmar un pedido y marcarlo como cobrado actualiza las tarjetas **sin recargar**.
3. Cada tarjeta lleva a su pestaña, y "Atrás" desde allí regresa al Dashboard.
4. Una unidad En reparación o Dañado se refleja en los contadores de Equipo.
5. Se ve bien en modo claro y oscuro y tras rotar.

---

## Después del núcleo

Cuando las 5 pestañas funcionen, el siguiente prompt a escribir es **Usuarios y Clientes** (primer complemento), y después los demás en el orden de la tabla. Cada uno se redacta apoyándose en lo que ya exista, para no saturar a la IA.
