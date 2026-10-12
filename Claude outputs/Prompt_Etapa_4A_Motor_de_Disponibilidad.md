# ETAPA 4A — Motor de disponibilidad y reservas (solo lógica + pruebas, SIN pantallas nuevas)

> Pega primero el **CONTEXTO GLOBAL** y la **ADENDA v3** (ya sabes que "Técnico" es ahora "Asistente"). Luego pega este texto.
> Requisito: Etapas 1, 1.1, 1.2, 2 y 3 terminadas.

---

## POR QUÉ ESTA ETAPA NO TIENE PANTALLAS

El corazón del negocio es decidir **si un paquete se puede entregar en una fecha, hora y duración dadas**. Si esa decisión falla, todo lo demás (pedidos, agenda, cliente) falla. Por eso se construye primero como lógica pura, probada con pruebas unitarias, y las pantallas se montan después encima. **No crees ni modifiques ninguna pantalla, layout ni `strings.xml` en esta etapa.**

## IDEAS DEL NEGOCIO QUE ESTA ETAPA IMPLEMENTA

1. Una **reserva** (el objeto `Pedido`) es un paquete + un horario (inicio y duración) + datos del cliente + un origen (App, Llamada, WhatsApp, Presencial). Todos los orígenes pasan por **el mismo motor**.
2. Una reserva bloquea **existencias** (cuántas unidades de cada modelo) y **personas** (operadores) durante su *ventana de bloqueo*: desde `inicio − montaje` hasta `fin + desmontaje`. Los tiempos de montaje y desmontaje vienen del paquete.
3. **Existencias:** el paquete A y el paquete B pueden coexistir en el mismo horario si, sumando lo que cada uno usa, no se pasa de las unidades disponibles de cada modelo. No importa el nombre del paquete: importa el equipo.
4. **Personas:** los operadores son todos los Socios activos (salvo los marcados como no disponibles ese día) más los Asistentes activos **marcados como disponibles ese día**. Un paquete pide 1 o 2 operadores. Nadie puede estar en dos reservas cuyas ventanas se traslapen.
5. **Máximo de eventos simultáneos:** `Config.MAX_EVENTOS_SIMULTANEOS` (3).
6. Un **apartado** (reserva recién creada, sin pago) retiene recursos hasta su vencimiento; después se libera solo.
7. Solo se ofrecen al cliente los horarios **viables**; los demás simplemente no existen para él. Por eso el motor puede **listar** horarios viables, no solo validar uno.

## PASO 0 — Revisión previa (no edites todavía; entrégame hallazgos y plan breve)

Lee y repórtame:

1. `model/Pedido.java`: todos sus campos, constantes de estado y quién los usa. `model/Evento.java` y `model/Usuario.java` (¿tiene campo de cuenta activa? ¿constantes de rol?).
2. `data/PedidoRepository.java` e `InMemoryPedidoRepository.java`: firmas actuales y si algo del proyecto los usa.
3. `data/EventoRepository.java`: qué hace hoy y **quién lo usa** (en especial `InventarioFragment` para "Próxima reserva" y `EquipoFormBottomSheet` para la advertencia de reservas futuras). **No lo vas a modificar en esta etapa**; solo quiero saber cómo está conectado.
4. `data/UsuarioRepository.java` e `InMemoryUsuarioRepository.java`: métodos disponibles para listar por rol.
5. `SonidoSonicApp.java` y `AppContainer.java`.
6. Si las constantes de estado antiguas de `Pedido` (PENDIENTE, CONFIRMADO, PAGADO, CANCELADO) se usan fuera del modelo y del repositorio. Si solo se usan ahí, se reemplazan por las nuevas.

## PASO 1 — Constantes en `util/Config.java` (comenta cada una: `// valor provisional, validar con los socios`)

- `ZONA_HORARIA = "America/Mexico_City"`
- `PRIMER_INICIO_MIN = 540` (09:00, en minutos desde medianoche) y `ULTIMO_INICIO_MIN = 1320` (22:00)
- `PASO_HORARIO_MIN = 30`
- `DURACION_MIN_MIN = 60`, `DURACION_MAX_MIN = 720`, `PASO_DURACION_MIN = 30`
- `APARTADO_APP_MIN = 30` y `APARTADO_MANUAL_HORAS = 24`
- `ANTICIPACION_REVISION_HORAS = 48`
- `ANTICIPO_PORCENTAJE = 0.30`
- `PREFERIR_ASISTENTES = true` (al asignar operadores se prefiere a los Asistentes antes que a los Socios)
- `DIAS_BUSQUEDA_MAX = 60`

## PASO 2 — Utilidades de tiempo

**No uses `java.time`** (el proyecto soporta Android 7.0). Usa `java.util.Calendar` y `SimpleDateFormat` siempre con `TimeZone.getTimeZone(Config.ZONA_HORARIA)`.

### 2.1 `util/Reloj.java` (interfaz) y `util/RelojSistema.java`

`interface Reloj { long ahora(); }`. `RelojSistema` devuelve `System.currentTimeMillis()`. Sirve para poder probar con fechas fijas.

### 2.2 `util/TiempoUtils.java` (métodos estáticos)

- `long construir(int anio, int mes0, int dia, int minutosDelDia)` (mes base 0)
- `long inicioDelDia(long millis)`
- `int minutosDelDia(long millis)`
- `String claveDia(long millis)` → `"yyyy-MM-dd"`
- `long sumarMinutos(long millis, int minutos)`
- `boolean solapan(long aIni, long aFin, long bIni, long bFin)` → `aIni < bFin && bIni < aFin` (los extremos que se tocan **no** se consideran traslape)

## PASO 3 — Modelos

### 3.1 `model/Pedido.java` (concilia con lo existente; conserva `id`, `nombreEvento`, `equipoIds`, `cobrado`, `fechaCobro` y `partidas` si existen y no estorban)

Campos objetivo (POJO, constructor público sin argumentos, getters/setters, `String` para constantes, nada de enums):

- Identidad y origen: `String paqueteId`, `String paqueteNombre` (copia al momento de reservar), `String origen`.
- Horario: `long inicioMillis`, `long finMillis`, `long bloqueoInicioMillis`, `long bloqueoFinMillis`.
- Recursos: `List<ComponentePaquete> componentes` (**copia** de los componentes del paquete al reservar, para que editar el paquete después no altere lo ya comprometido), `int operadoresRequeridos`, `List<String> operadorIds`.
- Cliente: `String clienteId` (puede ser nulo en pedidos por llamada, WhatsApp o presencial), `String clienteNombre`, `String clienteTelefono`, `String direccion`, `String notas`.
- Dinero: `double total`, `double anticipoRequerido`, `double anticipoPagado`.
- Ciclo de vida: `String estado`, `long vencimientoApartadoMillis` (0 si no aplica), `boolean requiereRevision`, `List<String> motivosRevision`, `long fechaCreacion`, `long fechaActualizacion`.

Constantes de **origen**: `ORIGEN_APP`, `ORIGEN_LLAMADA`, `ORIGEN_WHATSAPP`, `ORIGEN_PRESENCIAL`.
Constantes de **estado**: `ESTADO_APARTADO`, `ESTADO_PAGADO`, `ESTADO_CONFIRMADO`, `ESTADO_EN_REVISION`, `ESTADO_RECHAZADO`, `ESTADO_CANCELADO`, `ESTADO_VENCIDO`, `ESTADO_COMPLETADO`.
Constante de motivo de revisión: `MOTIVO_URGENTE`.

Si las constantes de estado antiguas solo se usan en el modelo y en el repositorio, bórralas y reemplázalas; repórtame lo que hayas quitado.

### 3.2 `model/DiaOperativo.java` (nuevo)

POJO con `String fechaClave` (formato `yyyy-MM-dd`, también hace de id), `List<String> asistentesDisponiblesIds` (opt-in: el Asistente solo cuenta si aparece aquí) y `List<String> sociosNoDisponiblesIds` (opt-out: el Socio cuenta salvo que aparezca aquí).

### 3.3 `domain/SolicitudReserva.java` (nuevo, paquete `domain`)

POJO con: `paqueteId`, `inicioMillis`, `duracionMin`, `origen`, `nombreEvento`, `clienteId`, `clienteNombre`, `clienteTelefono`, `direccion`, `notas`, `Integer horasApartado` (opcional, solo para orígenes manuales).

### 3.4 `domain/ResultadoDisponibilidad.java` (nuevo)

Campos: `boolean viable`, `String motivoNoViable` (una de las constantes de abajo o nulo), `String detalle` (mensaje en español para el Socio, por ejemplo "No alcanzan las unidades de Bocina JBL 15\""), `List<String> operadorIds`, `boolean requiereRevision`, `List<String> motivosRevision`, `long bloqueoInicio`, `long bloqueoFin`.

Constantes de motivo: `PAQUETE_INACTIVO`, `PAQUETE_INCOMPLETO`, `DURACION_INVALIDA`, `EN_EL_PASADO`, `FUERA_DE_HORARIO`, `SIN_STOCK`, `MAXIMO_SIMULTANEOS`, `SIN_OPERADORES`.

### 3.5 `domain/ContextoDisponibilidad.java` (nuevo)

Paquete de datos que el motor recibe (así el motor es puro y fácil de probar): `long ahora`, `List<ModeloInventario> modelos`, `List<Pedido> reservas`, `List<Usuario> socios` (activos), `List<Usuario> asistentes` (activos), `Map<String, DiaOperativo> dias` (por `fechaClave`).

## PASO 4 — `domain/MotorDisponibilidad.java` (clase final, solo métodos estáticos, **sin Android y sin estado**)

### 4.1 Funciones públicas

- `ResultadoDisponibilidad evaluar(ContextoDisponibilidad ctx, Paquete paquete, long inicioMillis, int duracionMin, String ignorarPedidoId)`
- `List<Long> horariosDisponibles(ContextoDisponibilidad ctx, Paquete paquete, long unMomentoDelDia, int duracionMin)`: inicios viables de ese día, de `PRIMER_INICIO_MIN` a `ULTIMO_INICIO_MIN` cada `PASO_HORARIO_MIN`, en orden ascendente.
- `Set<Integer> diasDisponibles(ContextoDisponibilidad ctx, Paquete paquete, int anio, int mes0, int duracionMin)`: días del mes (1 a 31) con al menos un horario viable.
- `List<Long> proximasAlternativas(ContextoDisponibilidad ctx, Paquete paquete, long desdeMillis, int duracionMin, int max)`: los siguientes `max` inicios viables a partir de `desdeMillis`, buscando hasta `Config.DIAS_BUSQUEDA_MAX` días.
- `boolean bloquea(Pedido p, long ahora)`: `true` si el estado es `APARTADO` con `vencimientoApartadoMillis > ahora`, o `PAGADO`, `CONFIRMADO` o `EN_REVISION`. Todo lo demás (incluido un apartado vencido) **no** bloquea.
- `double calcularTotal(Paquete paquete, int duracionMin)`: `precio` + horas extra × `precioHoraExtra`, donde horas extra = `max(0, ceil((duracionMin − duracionBaseHoras × 60) / 60.0))`.
- `double calcularAnticipo(double total)`: `total × ANTICIPO_PORCENTAJE` redondeado a 2 decimales.

### 4.2 Reglas de `evaluar`, **en este orden** (se detiene en la primera que falle)

1. `paquete` no nulo, `activo`, con al menos 1 componente → si no, `PAQUETE_INACTIVO`.
2. `duracionMin` entre `DURACION_MIN_MIN` y `DURACION_MAX_MIN` y múltiplo de `PASO_DURACION_MIN` → si no, `DURACION_INVALIDA`.
3. `inicioMillis > ctx.ahora` → si no, `EN_EL_PASADO`.
4. Minuto del día del inicio entre `PRIMER_INICIO_MIN` y `ULTIMO_INICIO_MIN` y múltiplo de `PASO_HORARIO_MIN` → si no, `FUERA_DE_HORARIO`.
5. Calcula `fin = inicio + duración` y la **ventana** = `[inicio − bufferMontajeMin, fin + bufferDesmontajeMin]`.
6. **Reservas bloqueantes** = las de `ctx.reservas` que cumplan `bloquea(p, ahora)`, no sean `ignorarPedidoId` y cuya ventana de bloqueo (`bloqueoInicioMillis`, `bloqueoFinMillis`) se traslape con la ventana nueva según `TiempoUtils.solapan`.
7. **Existencias.** Para cada componente del paquete: busca su modelo en `ctx.modelos` por clave (si no existe → `PAQUETE_INCOMPLETO`). La capacidad es `unidadesDisponibles`. Calcula el **pico de uso simultáneo** del modelo dentro de la ventana nueva y exige `pico + cantidad_del_componente ≤ capacidad`; si no, `SIN_STOCK` con el nombre del modelo en `detalle`.
   - **Importante: el pico NO es la suma de todas las reservas que tocan la ventana.** Dos reservas que no coinciden entre sí en el tiempo no se acumulan. Algoritmo: recorta el intervalo de cada reserva bloqueante a la ventana, genera eventos (+cantidad al inicio, −cantidad al fin), ordénalos por tiempo y, **a igualdad de tiempo, procesa primero los −**; el pico es el máximo acumulado.
   - La cantidad que una reserva usa de un modelo sale de **su propia copia** `componentes` (no del paquete actual).
8. **Simultáneos.** Calcula con el mismo método de barrido el pico de **reservas concurrentes** en la ventana; exige `pico + 1 ≤ Config.MAX_EVENTOS_SIMULTANEOS`; si no, `MAXIMO_SIMULTANEOS`.
9. **Operadores.** Candidatos = Socios activos cuyo id **no** esté en `sociosNoDisponiblesIds` del día + Asistentes activos cuyo id **sí** esté en `asistentesDisponiblesIds` del día. El día es el del **inicio** del evento (`claveDia(inicio)`); si no hay registro para ese día, no hay Asistentes disponibles y todos los Socios cuentan. Un candidato está **libre** si ninguna reserva bloqueante lo incluye en su `operadorIds`. Se necesitan `paquete.operadoresRequeridos` libres; se eligen en orden (Asistentes primero si `PREFERIR_ASISTENTES`, luego Socios; dentro de cada grupo, por id ascendente, para que el resultado sea determinista). Si no alcanzan → `SIN_OPERADORES`.
10. **Revisión por excepción.** Si `inicio − ahora < ANTICIPACION_REVISION_HORAS` horas, el resultado sigue siendo viable pero con `requiereRevision = true` y `MOTIVO_URGENTE` en `motivosRevision`.
11. Resultado viable con `operadorIds`, `bloqueoInicio` y `bloqueoFin`.

El motor **no modifica** ningún objeto que reciba.

## PASO 5 — Repositorios

### 5.1 `data/DiaOperativoRepository.java` + `data/memory/InMemoryDiaOperativoRepository.java`

- `LiveData<List<DiaOperativo>> getDias()`
- `DiaOperativo obtenerDia(String fechaClave)` (copia o nulo)
- `Map<String, DiaOperativo> obtenerTodosPorClave()` (copias)
- `void guardarDia(DiaOperativo dia, Callback<DiaOperativo> callback)` (crea o reemplaza por `fechaClave`; valida formato `yyyy-MM-dd`)

### 5.2 `UsuarioRepository` / `InMemoryUsuarioRepository`

Si no existe, agrega `List<Usuario> obtenerActivosPorRol(String rol)` síncrono, que devuelve copias.

### 5.3 `PedidoRepository` / `InMemoryPedidoRepository`

El repositorio recibe por constructor: `EquipoRepository`, `PaqueteRepository`, `UsuarioRepository`, `DiaOperativoRepository` y un `Reloj`. Interfaz:

- `LiveData<List<Pedido>> getPedidos()` (lista nueva con copias, más recientes primero).
- `Pedido getPedidoPorId(String id)` (copia o nulo).
- `List<Pedido> obtenerTodos()` (copias).
- `void crearReserva(SolicitudReserva solicitud, Callback<Pedido> callback)`.
- `void cancelarReserva(String pedidoId, String motivo, Callback<Pedido> callback)`.
- `int liberarApartadosVencidos()`.

Quita los métodos viejos que ya nadie use (repórtame cuáles).

**`crearReserva`** (todo dentro de un bloque `synchronized` con el comentario `// FIRESTORE: transacción`):

1. Llama a `liberarApartadosVencidos()`.
2. Valida la solicitud (mensajes en español en `onError`, misma convención de los otros repositorios): `origen` válido; `nombreEvento` no vacío y de máximo 80 caracteres; `clienteNombre` no vacío; `clienteTelefono` con al menos 10 dígitos; `direccion` de al menos 10 caracteres; y `clienteId` obligatorio si el origen es `ORIGEN_APP`.
3. Busca el paquete con `getPaquetePorId`; si no existe, error "Paquete no encontrado".
4. Arma el `ContextoDisponibilidad` con datos actuales: `ahora = reloj.ahora()`, `equipoRepository.obtenerModelos()`, `obtenerTodos()`, `obtenerActivosPorRol(Usuario.ROL_SOCIO)`, `obtenerActivosPorRol(Usuario.ROL_ASISTENTE)` y `diaOperativoRepository.obtenerTodosPorClave()`.
5. Llama a `MotorDisponibilidad.evaluar`. Si no es viable → `onError(detalle)` sin guardar nada.
6. Si es viable, crea el `Pedido`: UUID nuevo, `estado = ESTADO_APARTADO`, copia de `paqueteNombre` y `componentes` (copias profundas), `inicio`, `fin`, ventana de bloqueo, `operadoresRequeridos`, `operadorIds`, `total`, `anticipoRequerido`, `anticipoPagado = 0`, `requiereRevision` y `motivosRevision`, `fechaCreacion`/`fechaActualizacion = ahora`. El **vencimiento** es `ahora + APARTADO_APP_MIN` minutos si el origen es `ORIGEN_APP`; en los demás orígenes, `ahora + (horasApartado ?? APARTADO_MANUAL_HORAS)` horas.
7. Lo guarda, publica el `LiveData` y llama a `onSuccess` con una copia.

**`cancelarReserva`:** solo desde `APARTADO`, `PAGADO`, `CONFIRMADO` o `EN_REVISION`; pasa a `ESTADO_CANCELADO`, guarda el motivo en `notas` (agrégalo al final con salto de línea si ya había notas) y actualiza `fechaActualizacion`. Desde cualquier otro estado: error "Esta reserva ya no se puede cancelar". (El reembolso y las políticas de cancelación se hacen en una etapa posterior.)

**`liberarApartadosVencidos`:** pasa a `ESTADO_VENCIDO` todo `APARTADO` con `vencimientoApartadoMillis ≤ reloj.ahora()`; publica el `LiveData` solo si algo cambió; devuelve cuántas liberó.

### 5.4 `AppContainer`

Construye en este orden: Equipo, Paquete, Usuario, DiaOperativo, Pedido (con `new RelojSistema()`). Expón `getDiaOperativoRepository()`.

### 5.5 `SonidoSonicApp`

Programa un `Handler(Looper.getMainLooper())` que cada 60 segundos llame a `pedidoRepository.liberarApartadosVencidos()`. Inícialo en `onCreate`. Es solo un barrido de respaldo; el motor ya ignora los apartados vencidos aunque el barrido no haya corrido.

## PASO 6 — Pruebas unitarias (obligatorias)

Crea `app/src/test/java/com/example/propuestasonidosonic/Etapa4AMotorTest.java`. Para el motor usa contextos armados a mano con `TiempoUtils.construir`. Fecha de referencia: `ahora = 2026-11-20 10:00`; eventos el sábado `2026-12-05`. Un método de ayuda por prueba para crear paquetes, modelos, socios, asistentes y reservas.

Casos mínimos del **motor**:

1. Horario libre, 1 Socio: viable, 1 operador asignado.
2. **Buffers.** Reserva A de 19:00 a 22:00 (montaje 90, desmontaje 120; bloqueo 17:30 a 00:00). Con un solo operador y equipo suficiente: un evento B de 10:00 a 13:00 es viable (su ventana 08:30–15:00 no toca la de A); un evento B de 14:00 a 16:00 **no** (ventana 12:30–18:00 se traslapa) → `SIN_OPERADORES`.
3. **Extremos que se tocan:** una ventana que termina exactamente cuando otra empieza **no** se traslapa.
4. **Existencias:** 4 bocinas disponibles; A usa 3 en la misma ventana; B pide 2 → `SIN_STOCK` (con dos operadores libres); B pide 1 → viable.
5. **Pico, no suma:** capacidad 4; A usa 3 de 08:00 a 12:00 y C usa 3 de 18:00 a 22:00 (no coinciden entre sí); una solicitud de 1 bocina cuya ventana abarca ambas es **viable** (pico 3 + 1 ≤ 4); una de 2 no lo es.
6. **Operadores:** con 2 Socios, dos reservas simultáneas de equipo disjunto son viables con operadores distintos; la tercera da `SIN_OPERADORES`; si hay un Asistente marcado disponible ese día, la tercera es viable y se le asigna a él.
7. Paquete con `operadoresRequeridos = 2` y solo 1 operador libre → `SIN_OPERADORES`; con 2 libres → viable con 2 ids distintos.
8. Un Socio en `sociosNoDisponiblesIds` del día no cuenta; un Asistente que no aparece en `asistentesDisponiblesIds` tampoco.
9. Apartado **vigente** bloquea; apartado **vencido** no; reserva cancelada, rechazada o vencida no.
10. `MAX_EVENTOS_SIMULTANEOS`: con 4 operadores y 3 reservas simultáneas (equipo de sobra), la cuarta da `MAXIMO_SIMULTANEOS`.
11. Validaciones: 08:00 y 22:30 → `FUERA_DE_HORARIO`; fecha pasada → `EN_EL_PASADO`; duración 45 min → `DURACION_INVALIDA`; paquete inactivo → `PAQUETE_INACTIVO`; modelo inexistente → `PAQUETE_INCOMPLETO`.
12. Evento a menos de 48 horas: viable con `requiereRevision = true` y `MOTIVO_URGENTE`.
13. `horariosDisponibles` con la reserva A del caso 2 y un solo operador: no contiene horarios cuya ventana choque, sí contiene el primero libre, todos son múltiplos de 30 min y están dentro del rango permitido.
14. `ignorarPedidoId`: una reserva no choca consigo misma al reevaluarse.
15. `calcularTotal`: precio 3500, 4 h incluidas, $100 por hora extra, duración 5.5 h → 3700; `calcularAnticipo(3700)` → 1110.00.
16. `proximasAlternativas` devuelve horarios viables en orden y respeta `max`.

Casos mínimos del **repositorio** (con `InstantTaskExecutorRule` y un `Reloj` falso con hora fija), usando repositorios reales en memoria y un solo Socio:

17. `crearReserva` válida → estado `APARTADO`, vencimiento `ahora + 30 min` para `ORIGEN_APP` y `ahora + 24 h` para `ORIGEN_LLAMADA`, `operadorIds` con 1 id, total y anticipo correctos.
18. Una segunda reserva en el mismo horario (un solo operador) → error; **no** se guarda.
19. `cancelarReserva` libera: después de cancelar, la segunda reserva sí se puede crear.
20. Avanzar el reloj falso más allá del vencimiento y llamar a `liberarApartadosVencidos` → devuelve 1 y el estado queda `VENCIDO`; cancelar una reserva vencida da error.
21. Validaciones de la solicitud: teléfono corto, dirección corta, `ORIGEN_APP` sin `clienteId`, nombre de evento vacío.

## ALCANCE: LO QUE NO SE HACE

Nada de pantallas, strings ni layouts. No toques `EventoRepository`, `InventarioFragment` ni `EquipoFormBottomSheet`. No implementes pagos, anticipos pagados, confirmación, rechazo, reembolsos, distancia, bloqueo de clientes, ni la asignación de unidades concretas. No agregues datos de ejemplo.

## CRITERIOS DE ACEPTACIÓN

1. `./gradlew test` (en Windows: `.\gradlew.bat test`) pasa con **todas** las pruebas de arriba (muéstrame el listado de pruebas ejecutadas).
2. `./gradlew assembleDebug` termina sin errores.
3. La app abre, el login funciona y Inventario (con Equipo y Paquetes) se comporta exactamente igual que al terminar la Etapa 3.
4. El motor no importa nada de `android.*` (revisa los `import` de `domain/`); `InMemoryPedidoRepository` y `SonidoSonicApp` son las únicas clases nuevas que tocan Android (`LiveData`, `Handler`).

## AL TERMINAR

Entrégame: (a) hallazgos del Paso 0, incluyendo quién usa `EventoRepository` y qué estados viejos de `Pedido` quitaste, (b) archivos creados y modificados, (c) decisiones fuera de lo descrito, (d) salida de `test` y `assembleDebug` con la lista de pruebas, y (e) cualquier caso en que una regla de este documento te pareció ambigua y cómo la resolviste.
