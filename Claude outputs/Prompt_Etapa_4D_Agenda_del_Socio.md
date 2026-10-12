# ETAPA 4D — Agenda del Socio: calendario mensual y eventos por día o por mes

> Pega primero el **CONTEXTO GLOBAL** y la **ADENDA v3**. Luego pega este texto.
> Requisito: Etapas 1 a 4C terminadas y probadas.

---

## OBJETIVO

La pestaña **Agenda** (hoy un placeholder) debe mostrar un **calendario mensual** donde se vea de un vistazo qué días tienen eventos, y debajo la **lista de eventos** del día elegido o de todo el mes. Los datos salen de los **pedidos** (no existe captura manual de eventos: un evento nace de un pedido). Solo lectura: desde aquí se abre el detalle del pedido, donde ya están las acciones.

## IDEAS DEL NEGOCIO

- En la Agenda aparecen los pedidos **Confirmados** (y los que están en `PAGADO` o `EN_REVISION`), los **Completados**, y los **Apartados vigentes** (marcados como tentativos). No aparecen los apartados vencidos, ni los cancelados, rechazados o vencidos.
- Un evento pertenece al **día en que inicia** (en la zona horaria del negocio), aunque termine después de medianoche.
- Semana de lunes a domingo.

## PASO 0 — Revisión previa (no edites todavía; entrégame hallazgos y plan breve)

Lee y repórtame: `AgendaFragment.java` y `fragment_agenda.xml` (placeholder), `nav_graph.xml` (destino `nav_agenda` y las acciones de `nav_pedidos`), `Pedido.java`, `TiempoUtils.java`, `FormatUtils.java`, `PedidosFragment.java` y `PedidoAdapter.java` (para reutilizar el estilo de insignias y tarjetas), y `UsuariosFragment.java` (patrón de estado al rotar).

**Además, confírmame tres puntos de la 4C** que quedaron poco claros en tu reporte:
1. En `Paquete.java` agregaste `getPrecioBase()`/`setPrecioBase()`. ¿Existe **un solo campo** de precio en `Paquete` (el que se usa en todo el proyecto) y los métodos nuevos son solo alias que lo leen y lo escriben? No debe haber dos campos de precio distintos.
2. `Pago`: tu Paso 0 dijo que existían `metodo`, `fecha`, `TIPO_TOTAL` y `METODO_EFECTIVO`; tu reporte final habla de `metodoPago`, `fechaRegistroMillis` y `TIPO_LIQUIDACION`. Dime los nombres reales que quedaron y confirma que no hay campos duplicados.
3. En la lista de Pedidos usaste chips ("Todos / Apartados / Confirmados / Historial") y un banner de apartados por vencer, en lugar de las tres pestañas pedidas (Por pagar / Confirmados / Historial). No cambies nada ahora; solo confírmame que así quedó.

## PASO 1 — Lógica pura (`domain/AgendaUtils.java`, sin Android, métodos estáticos)

- `boolean apareceEnAgenda(Pedido p, long ahora)`: `true` para `CONFIRMADO`, `PAGADO`, `EN_REVISION` y `COMPLETADO`; `true` para `APARTADO` **solo si** `vencimientoApartadoMillis > ahora`; `false` para todo lo demás.
- `Map<String, List<Pedido>> agruparPorDia(List<Pedido> pedidos, long ahora)`: solo los que cumplen `apareceEnAgenda`, con clave `TiempoUtils.claveDia(inicioMillis)` y cada lista ordenada por `inicioMillis` ascendente.
- `List<Pedido> delMes(List<Pedido> pedidos, int anio, int mes0, long ahora)`: los que aparecen en la agenda y cuyo inicio cae en ese mes, ordenados por `inicioMillis`.
- `int[] marcadoresDia(List<Pedido> delDia)`: devuelve `{eventosConfirmados, apartados}`, donde "confirmados" cuenta `CONFIRMADO`, `PAGADO`, `EN_REVISION` y `COMPLETADO`, y "apartados" cuenta `APARTADO`.
- `List<Long> celdasDelMes(int anio, int mes0)`: siempre **42 celdas** (6 semanas × 7 días), cada una el inicio del día en milisegundos (`TiempoUtils.inicioDelDia`), empezando en el **lunes** de la semana que contiene el día 1, y continuando con los días siguientes (los de los meses vecinos completan la cuadrícula).

## PASO 2 — Pantalla Agenda

Reemplaza el placeholder. `fragment_agenda.xml` dentro de un `NestedScrollView` con este orden:

1. **Encabezado del mes:** botón `<` (`btnMesAnterior`), título del mes en español ("octubre de 2026", `tvMes`), botón `>` (`btnMesSiguiente`) y botón de texto **"Hoy"** (`btnHoy`).
2. **Etiquetas de los días de la semana** (L M M J V S D) en una fila de 7 columnas.
3. **Cuadrícula del calendario:** `RecyclerView` `rvCalendario` con `GridLayoutManager(7)`, sin scroll propio (`nestedScrollingEnabled="false"`, altura `wrap_content`), alimentado por `celdasDelMes`. Cada celda (`item_dia_calendario.xml`, altura mínima 48dp):
   - El número del día (14sp o más).
   - **Día seleccionado:** círculo relleno con `colorPrimary` y número con `colorOnPrimary`.
   - **Hoy:** círculo con solo trazo `colorPrimary`.
   - **Días de otro mes:** número atenuado con `colorOutline` (se pueden tocar y cambian el mes mostrado).
   - **Marcadores** bajo el número (puntos de 6dp): uno verde `status_disponible` si hay eventos confirmados ese día y uno ámbar `status_pendiente` si hay apartados. Los puntos nunca son la única señal: el `contentDescription` de la celda dice, por ejemplo, "12 de octubre, 2 eventos y 1 apartado".
4. **Selector de vista:** `MaterialButtonToggleGroup` `toggleVista` con **Día | Mes** (por defecto Día).
5. **Título de la lista** (`tvTituloLista`): en modo Día, la fecha larga ("lunes 12 de octubre de 2026"); en modo Mes, "octubre de 2026 · 5 eventos" (usa plurales correctos, "1 evento").
6. **Lista de eventos** `rvEventosAgenda` (sin scroll propio dentro del `NestedScrollView`) y un estado vacío con el texto `empty_agenda` ("No hay eventos para esta fecha" / "No hay eventos este mes", según el modo).

### 2.1 Lista de eventos (`EventoAgendaAdapter`, dos tipos de fila)

- **Fila de evento** (`item_evento_agenda.xml`, tarjeta con el estilo de `item_pedido.xml`): a la izquierda el rango de horas en negrita ("19:00–23:00"); a la derecha el nombre del evento, "Paquete · Cliente", el **personal asignado** (nombres, de `UsuarioRepository`) y la insignia de estado (misma paleta que en Pedidos; los **apartados** llevan la insignia ámbar "Apartado" para distinguirlos como tentativos).
- **Fila de encabezado de fecha** (solo en modo Mes): la fecha larga corta ("sábado 5 de diciembre") para separar los días.
- Tocar un evento navega al detalle del pedido (`pedidoDetalleFragment`, `Bundle` con `"pedidoId"`).

### 2.2 Comportamiento

- Al abrir: mes actual, día seleccionado = hoy, vista Día.
- `<` y `>` cambian el mes mostrado; si el día seleccionado no existe en el mes nuevo, se selecciona el día 1.
- Tocar una celda selecciona ese día (si es de otro mes, también cambia el mes mostrado).
- **"Hoy"** regresa al mes actual y selecciona hoy.
- La lista y los marcadores se recalculan al cambiar el `LiveData` de pedidos y también **cada 60 segundos** mientras la pantalla está visible (un `Handler` que se inicia en `onResume`, se detiene en `onPause` y se limpia en `onDestroyView`), para que un apartado que vence desaparezca solo.
- **Estado:** guarda mes mostrado, día seleccionado y vista en campos del fragmento y en `onSaveInstanceState`. Al volver del detalle con "Atrás" o al rotar, debe verse exactamente igual.

## PASO 3 — Navegación

En `nav_graph.xml`, dentro del destino `nav_agenda`, agrega la acción `action_agenda_to_detalle` hacia `pedidoDetalleFragment`. `MainActivity` no cambia: "Atrás" desde el detalle regresa a la Agenda y, desde la Agenda, a Dashboard.

## PASO 4 — Textos e iconos

- Todo en `strings.xml` (español de México): nombres de la vista (Día/Mes), "Hoy", estados vacíos, plurales (`<plurals>` para "evento/eventos" y "apartado/apartados"), descripciones de accesibilidad de las celdas y de las flechas.
- Si falta, crea `ic_chevron_left_24.xml` y `ic_chevron_right_24.xml` (vectores Material de 24dp con `tint="?attr/colorControlNormal"`):
  - izquierda: `M15.41,7.41L14,6l-6,6 6,6 1.41,-1.41L10.83,12z`
  - derecha: `M10,6L8.59,7.41 13.17,12l-4.58,4.59L10,18l6,-6z`
- Sin colores fijos; zonas táctiles de 48dp o más; legible en claro y oscuro.

## PASO 5 — Pruebas unitarias (obligatorias)

Crea `Etapa4DUnitTest.java` (solo `AgendaUtils`, con `TiempoUtils.construir` y fechas fijas):

1. `apareceEnAgenda`: verdadero para Confirmado, Pagado, En revisión y Completado; Apartado vigente verdadero y Apartado vencido falso; falso para Cancelado, Rechazado y Vencido.
2. `agruparPorDia`: agrupa por el día **local** de inicio; un evento que inicia a las 23:30 pertenece a ese día aunque termine al siguiente; uno que inicia a las 00:15 pertenece al día siguiente; cada lista queda ordenada por hora.
3. `delMes`: solo los del mes pedido, ordenados, sin los excluidos.
4. `marcadoresDia`: cuenta bien confirmados (incluido Completado) y apartados.
5. `celdasDelMes`: siempre 42 celdas consecutivas de un día cada una; **octubre de 2026** empieza el lunes 28 de septiembre (el 1 de octubre es la celda de índice 3); **noviembre de 2026** empieza el lunes 26 de octubre (el 1 de noviembre es el índice 6); **febrero de 2027** empieza el lunes 1 de febrero (índice 0).
6. Las pruebas de las etapas anteriores siguen pasando.

## ALCANCE: LO QUE NO SE HACE

Sin captura manual de eventos desde la Agenda, sin arrastrar o editar eventos, sin vista semanal, sin cambios en pedidos, sin asignar unidades concretas ni crear `Evento`, sin tocar `EventoRepository` ni Inventario. Sin datos de ejemplo.

## CRITERIOS DE ACEPTACIÓN

Prepara con la pestaña Pedidos: dos pedidos confirmados el mismo día futuro (A a las 10:00 y B a las 19:00), uno confirmado en otro día del mismo mes y un apartado en otro día.

1. Agenda abre en el mes actual con hoy marcado y seleccionado, y la lista del día vacía con su mensaje.
2. Los días con pedidos muestran punto verde (confirmados) o ámbar (apartados); un día con ambos muestra los dos.
3. Tocar el día con A y B: la lista muestra A y luego B (por hora), cada uno con su horario, paquete, cliente, personal y estado.
4. Cambiar a **Mes**: aparecen todos los eventos del mes agrupados con encabezados de fecha, y el título dice el total ("4 eventos"). Cambiar de mes con `<` y `>` actualiza todo. "Hoy" regresa.
5. Tocar un evento abre el detalle; "Atrás" regresa a la Agenda con el mismo mes, día y vista; otro "Atrás" va a Dashboard.
6. Cancelar un pedido desde su detalle: al volver a la Agenda, desaparece de la lista y su punto se quita. Con un apartado de pocos minutos (puedes bajar temporalmente `APARTADO_APP_MIN` en pruebas y revertirlo), al vencer desaparece solo sin salir de la pantalla.
7. Tocar un día gris (de otro mes) cambia al mes correspondiente y lo selecciona.
8. Rotar el teléfono conserva mes, día y vista.
9. Modo oscuro legible; sin colores fijos nuevos.
10. `./gradlew test` y `./gradlew assembleDebug` pasan (en Windows `.\gradlew.bat ...`).

## AL TERMINAR

Entrégame: (a) hallazgos del Paso 0 y respuestas a los **tres puntos de la 4C**, (b) archivos creados y modificados, (c) decisiones fuera de lo descrito, (d) salida de `test` y `assembleDebug` con la lista de pruebas, y (e) cómo probaste cada criterio y cualquier ambigüedad que resolviste.
