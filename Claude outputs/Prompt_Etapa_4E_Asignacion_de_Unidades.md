# ETAPA 4E — Asignación de unidades físicas y Evento real (Inventario ya refleja las reservas)

> Pega primero el **CONTEXTO GLOBAL** y la **ADENDA v3**. Luego pega este texto.
> Requisito: Etapas 1 a 4D terminadas (62 pruebas pasando). La auditoría previa de esta etapa ya la hiciste; este prompt se basa en ella.

---

## OBJETIVO

Hoy un pedido confirmado reserva **capacidad por modelo** pero no ocupa **unidades físicas concretas**; por eso Inventario no muestra "Próxima reserva" ni el estado Rentado derivado. En esta etapa:

1. Al **confirmar** un pedido (anticipo registrado) se crea su **`Evento`** y se le **asignan unidades concretas** de cada modelo del paquete.
2. Si el pedido se cancela, se reprograma, se completa o se rechaza, el `Evento` y las unidades se actualizan solos.
3. Si el inventario cambia (una unidad pasa a Dañado o En reparación, se agrega o se elimina una), las asignaciones futuras se **recalculan**.
4. En el detalle del pedido se ve qué equipo quedó asignado.

No se toca el motor de disponibilidad ni la lógica de precios. No se crea pantalla nueva.

## IDEAS DEL NEGOCIO (decisiones ya tomadas)

- **Solo los pedidos confirmados ocupan unidades.** Un `APARTADO` no crea `Evento` (así Inventario no marca equipo como rentado por algo que aún no se pagó). Tienen `Evento`: `CONFIRMADO`, `PAGADO`, `EN_REVISION` y `COMPLETADO`.
- **Las unidades se ocupan durante toda la ventana de bloqueo** (montaje + evento + desmontaje), no solo durante el evento. Por eso una unidad aparece "Rentada" desde que sale a montaje.
- **La asignación se recalcula globalmente** porque asignar "la primera unidad libre" pedido por pedido puede fallar aunque el pico de uso sí quepa. Ejemplo con 2 bocinas: A usa una del 1 al 10, B usa una del 12 al 20 y llega C del 5 al 15. Con asignación incremental A tomó la bocina 1 y B la bocina 2, y C no cabe en ninguna; reacomodando B a la bocina 1, C entra en la 2.
- **Lo que ya empezó no se mueve.** Un evento cuya ventana de bloqueo ya inició (el equipo ya salió) conserva sus unidades fijas. Solo se reacomodan los futuros.
- **Estabilidad:** al recalcular, cada pedido conserva las unidades que ya tenía siempre que sigan libres; solo se mueve lo necesario.
- **Si no alcanza el equipo físico** (p. ej. una unidad se dañó después de confirmar), el pedido **no se cancela ni se bloquea**: queda con las unidades que sí se pudieron asignar y una marca de **asignación incompleta**, visible en su detalle, para que el socio resuelva (conseguir equipo, avisar al cliente). Nunca se pierde un pedido por esto.

## PASO 0 — Confirmación rápida (lee, confirma y SIGUE; detente solo si algo contradice este prompt)

Confírmame en tu reporte final (no antes) estos puntos que supongo; si alguno es falso, ajusta con criterio y dímelo:
1. `Pedido` tiene la ventana de bloqueo (`bloqueoInicioMillis`/`bloqueoFinMillis` o equivalente). Dame los nombres reales.
2. Cómo obtiene `InMemoryPedidoRepository` el catálogo de unidades (`Equipo`) para el motor. Si no existe un método síncrono para obtener una copia de todas las unidades, agrégalo a `EquipoRepository` (`List<Equipo> listarTodos()`), con copias defensivas.
3. Si `MotorDisponibilidad`/`ContextoDisponibilidad` cuenta como stock **todas** las unidades de un modelo o solo las `DISPONIBLE`. Debe contar **solo las `estadoFisico == DISPONIBLE`**. Si hoy cuenta todas, corrígelo sin romper las pruebas existentes y avísame.
4. Los `Callback` de los repositorios en memoria son síncronos (se invocan antes de retornar). Si no, dímelo.

## PASO 1 — `Evento` y `EventoRepository`

### 1.1 `Evento.java`
Agrega `long bloqueoInicioMillis` y `long bloqueoFinMillis` (default 0) con getters/setters y copia defensiva donde corresponda. **Regla de respaldo:** si valen 0 se usan `inicioMillis`/`finMillis`. Así las pruebas de Etapa 2 (que crean eventos sin bloqueo) siguen pasando sin cambios.

### 1.2 `InMemoryEventoRepository`
- Agrega dos helpers privados `desde(ev)` y `hasta(ev)` que devuelven el bloqueo si está definido y, si no, el inicio/fin. Úsalos en **`unidadesOcupadas`, `reservasVigentes` y `reservasFuturas`** (en lugar de `inicioMillis`/`finMillis` directos). Los eventos `CANCELADO` y `FINALIZADO` siguen sin contar.
- Agrega a `EventoRepository` e implementa:
  `void reemplazarAsignaciones(Map<String, List<String>> equipoIdsPorEventoId, Callback<Void> callback)`: en una sola operación `synchronized` (con comentario `// FIRESTORE: transacción`) reemplaza la lista `equipoIds` de cada evento indicado. **No valida traslapes** (la validación ya la hizo el asignador); ignora ids de evento que no existan o estén cancelados/finalizados.
- `crearEvento` conserva su validación de traslapes, ahora sobre la ventana de bloqueo.

## PASO 2 — Asignador puro (`domain/AsignadorUnidades.java`, sin Android, estático)

Clases internas públicas estáticas:
- `Demanda`: `String pedidoId`, `String modeloClave` (`ModeloUtils.clave`), `int cantidad`, `long desde`, `long hasta` (ventana de bloqueo), `List<String> unidadesPrevias` (puede ser vacía), `boolean fija` (ya inició).
- `UnidadDisponible`: `String id`, `String modeloClave`, `int numeroUnidad`.
- `Resultado`: `Map<String, List<String>> unidadesPorPedido` (clave: `pedidoId`; los ids de unidades asignadas, **agregando todos los modelos del pedido**) y `Set<String> pedidosIncompletos`.

Método: `Resultado asignar(List<Demanda> demandas, List<UnidadDisponible> unidades)` donde `unidades` son las unidades **asignables** (solo `DISPONIBLE`). Algoritmo, **por cada `modeloClave` por separado**:

1. Lleva por unidad una lista de intervalos ocupados `[desde, hasta)`.
2. Ordena las demandas del modelo por `desde` ascendente (desempate: `fija` primero, luego `pedidoId`).
3. Para cada demanda necesita `cantidad` unidades distintas:
   - Si es `fija`: conserva exactamente sus `unidadesPrevias` (aunque la unidad ya no esté en la lista de asignables, porque el equipo ya salió) y registra sus intervalos. Si no tiene previas, trátala como no fija.
   - Si no es fija: primero toma las `unidadesPrevias` que sigan en `unidades` y estén libres en `[desde, hasta)`; para las que falten elige entre las unidades libres la de **mejor ajuste** (la que terminó su último intervalo más cerca antes de `desde`; desempate por menor `numeroUnidad`). Marca el intervalo en cada unidad elegida.
   - Si no hay suficientes unidades libres, asigna las que sí encontró y agrega el `pedidoId` a `pedidosIncompletos`.
4. El resultado es determinista (mismos datos, mismo resultado).

Nota matemática para tu implementación: procesar por inicio ascendente y tomar cualquier unidad libre nunca falla si el pico de uso simultáneo cabe en el stock; por eso este asignador sí encuentra solución donde la asignación incremental falla.

## PASO 3 — Integración en `InMemoryPedidoRepository`

Crea un método privado `reasignarUnidades(long ahora)` (siempre llamado dentro del bloque `synchronized` del repositorio, con `// FIRESTORE: transacción`) que:
1. Toma todos los pedidos con `Evento` activo **más el pedido que se está confirmando o reprogramando**, y construye las `Demanda` con sus `componentes` (una por componente), `desde/hasta` = ventana de bloqueo, `unidadesPrevias` = `equipoIds` actuales del pedido filtradas por el modelo, `fija = desde <= ahora`.
2. Toma las unidades `DISPONIBLE` del catálogo y llama a `AsignadorUnidades.asignar`.
3. Aplica el resultado: actualiza `pedido.equipoIds` y `pedido.asignacionIncompleta` de cada pedido, y llama a `eventoRepository.reemplazarAsignaciones(...)` para los eventos que ya existían.

Usa `ahora` del `Reloj` inyectable. Después, en cada transición:

- **`registrarAnticipo`**: si el pedido queda en `CONFIRMADO`, `PAGADO` o `EN_REVISION`, ejecuta `reasignarUnidades` incluyéndolo, y luego **crea el `Evento`** (`crearEvento`) con: `nombre` = nombre del evento del pedido, `inicioMillis/finMillis` = el evento real, `bloqueoInicioMillis/bloqueoFinMillis` = la ventana de bloqueo, `direccion` = la del pedido si existe (si no, null), `pedidoId`, estado `PROGRAMADO`, `equipoIds` = las asignadas, `socioResponsableId` = el primer operador del pedido con rol Socio (si no hay, quien registró; si tampoco, deja el valor vacío que ya acepte el modelo) y `asistenteId` = el primer operador Asistente o null. Guarda `pedido.eventoId`. Si `registrarAnticipo` se llama de nuevo (abonos posteriores) **no** vuelve a crear el evento (si `eventoId` ya existe, no hagas nada de esto).
- **`cancelarReserva`** (y rechazo, si existe el método): si el pedido tiene `eventoId`, `eventoRepository.cancelarEvento(...)`, deja `equipoIds` vacío y `asignacionIncompleta = false`. Luego `reasignarUnidades` (los demás pueden recuperar su asignación completa).
- **`reprogramarReserva`**: tras actualizar las fechas y la ventana del pedido, actualiza el `Evento` (fechas y bloqueo) y ejecuta `reasignarUnidades`. Para actualizar el evento usa `actualizarEvento` **después** de `reemplazarAsignaciones`, de modo que no choque con su propia versión anterior (excluye el propio id al validar, como ya hace `unidadesOcupadas(..., ignorarEventoId)`).
- **`liberarApartadosVencidos`**: cuando un pedido pasa a `COMPLETADO`, `eventoRepository.finalizarEvento(eventoId)`. Los apartados que vencen no tienen evento, no hay nada que hacer.
- Agrega a `PedidoRepository` e implementa **`void reasignarUnidades(Callback<Void> callback)`** (público) que ejecuta lo anterior con el `ahora` actual.

### 3.1 Cambio de inventario dispara el recálculo
En `InMemoryEquipoRepository`, después de cada alta, edición o baja de unidad (cuando el cambio afecte `estadoFisico`, modelo o existencia), invoca un `Runnable` opcional `onInventarioCambio` (`setOnInventarioCambio(Runnable)`). En `AppContainer`, conéctalo para que llame a `pedidoRepository.reasignarUnidades(null)` (el callback es null-safe). No debe romper ninguna prueba de Etapa 2 (sin listener asignado no hace nada).

### 3.2 `Pedido.java`
Agrega `boolean asignacionIncompleta` (default `false`) con getter/setter y copia defensiva.

## PASO 4 — Detalle del pedido: "Equipo asignado"

En `PedidoDetalleFragment` / su layout (mismo estilo que las demás secciones), agrega una sección **"Equipo asignado"** visible solo si el pedido tiene `Evento` (estados confirmados o completado):
- Lista de líneas agrupadas por modelo con las unidades: `Bocina JBL 15" · unidades 1, 2` (usa `nombre` y `numeroUnidad`; obtén cada `Equipo` por id con el repositorio; si una unidad ya no existe, muestra "unidad eliminada").
- Si `asignacionIncompleta` es `true`, arriba de la lista un aviso en color de error del tema: "Faltan unidades por asignar. Revisa el inventario antes del evento."
- Textos en `strings.xml`. Sin colores fijos.

## PASO 5 — Pruebas unitarias (`Etapa4EUnitTest.java`)

No uses `android.util.Patterns` ni `TextUtils`. Usa `Reloj` fijo. Mínimo:
1. `AsignadorUnidades`: el caso A [1–10), B [12–20), C [5–15) con 2 unidades y las 3 demandas nuevas (sin previas) → las tres quedan completas, nadie comparte unidad en un traslape.
2. `AsignadorUnidades`: una demanda `fija` conserva sus unidades previas aunque otra demanda posterior las prefiera.
3. `AsignadorUnidades`: estabilidad; con unidades previas libres, el resultado no las cambia.
4. `AsignadorUnidades`: faltan unidades (cantidad mayor al stock) → `pedidosIncompletos` contiene al pedido y recibe las que sí había.
5. `AsignadorUnidades`: determinismo (dos corridas iguales dan el mismo mapa).
6. Repositorio: `registrarAnticipo` crea el `Evento` con `equipoIds` no vacío, `pedido.eventoId` correcto y ventana de bloqueo; un apartado sin pagar **no** crea `Evento`.
7. Repositorio: tras confirmar, `proximaReserva` de una unidad asignada devuelve ese evento y `estadoEfectivo` es `RENTADO` dentro de la ventana de bloqueo (también durante el montaje) y `DISPONIBLE` fuera.
8. Repositorio: `cancelarReserva` cancela el evento y la unidad vuelve a quedar sin próxima reserva.
9. Repositorio: `reprogramarReserva` actualiza fechas del evento y mantiene unidades válidas sin traslapes.
10. Repositorio: al pasar a `COMPLETADO` el evento queda `FINALIZADO`.
11. Repositorio: marcar una unidad asignada como `DANADO` provoca (vía `reasignarUnidades`) que se reemplace por otra libre si existe, o que el pedido quede con `asignacionIncompleta = true` si no hay otra.
12. Repositorio: el escenario A/B/C del punto 1 pero a través de pedidos reales (tres `registrarAnticipo` en ese orden): ninguno queda incompleto.
13. Etapas 1 a 4D siguen pasando sin modificar sus pruebas (salvo que alguna dependa del punto 3 del Paso 0; si hay que tocar una prueba, dime cuál y por qué).

Ejecuta `.\gradlew.bat test` y `.\gradlew.bat assembleDebug`.

## NO HAGAS

- No crear `Evento` para apartados.
- No cambiar la lógica de precios, anticipos, reembolsos ni de `MotorDisponibilidad` (salvo el punto 3 del Paso 0).
- No crear pantallas nuevas ni modificar Agenda.
- No llamar a `reasignarUnidades` desde la interfaz; solo desde los repositorios y el listener de inventario.
- No usar colores fijos ni `java.time`.

## REPORTE FINAL QUE ESPERO

Archivos creados/modificados, respuestas al Paso 0 (puntos 1 a 4), resultado de `test` y `assembleDebug` con el conteo total de pruebas, y cualquier desviación de este prompt.

## CRITERIOS MANUALES (los pruebo yo en el emulador)

1. Crea 3 unidades de un mismo modelo y un paquete que use 1 de ellas. Crea un pedido (apartado): **Inventario no cambia**.
2. Registra el anticipo: el pedido pasa a Confirmado; en el detalle aparece **"Equipo asignado"** con la unidad.
3. En Inventario, esa unidad muestra **Próxima reserva** con la fecha del evento.
4. Con un evento cuya ventana (incluido el montaje) ya empezó, la unidad se ve **Rentada**.
5. Cancela el pedido: la unidad vuelve a Disponible sin próxima reserva y desaparece de "Equipo asignado".
6. Reprograma el pedido a otra fecha: la próxima reserva cambia a la fecha nueva.
7. Marca como Dañada la unidad asignada de un pedido futuro: el pedido recibe otra unidad libre (o muestra el aviso de "Faltan unidades" si no hay otra).
8. Intenta eliminar una unidad con reserva futura: sigue bloqueado como antes.
9. Todo se ve bien en modo oscuro.
