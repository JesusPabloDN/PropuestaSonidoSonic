# Sonido Sonic — Prompts para Antigravity (apartado del Socio)

**Cómo usar este archivo**

1. Cada etapa es un prompt independiente. Pégalos **en orden** y no pases a la siguiente hasta que la anterior compile y cumpla sus criterios de aceptación.
2. Al inicio de **cada** prompt pega primero el bloque **CONTEXTO GLOBAL** y después el texto de la etapa.
3. Si la IA se sale del alcance, respóndele: "Revierte lo que no pertenece a esta etapa".

Etapas: 1 Base (navegación, drawer, tema, sesión, datos en memoria, login) · 2 Inventario y Paquetes · 3 Agenda y Eventos · 4 Pedidos y Pagos · 5 Finanzas y Dashboard · 6 Usuarios y Clientes · 7 PDF (cotización y comprobantes).

---

## CONTEXTO GLOBAL (pegar al inicio de cada prompt)

Eres un desarrollador Android senior. Trabajas en el proyecto abierto **PropuestaSonidoSonic** (package `com.example.propuestasonidosonic`). Lenguaje **Java**, vistas **XML** con **Material 3**, `minSdk 24`, Gradle Kotlin DSL con version catalog (`gradle/libs.versions.toml`).

**La app.** "Sonido Sonic" administra un negocio de renta de equipo de audio para eventos (bodas, XV años, posadas, eventos corporativos) operado por dos socios que atienden eventos simultáneos en lugares distintos. Resuelve cuatro problemas: doble reservación de equipo, pedidos sin seguimiento, falta de cobro formal y falta de visibilidad financiera. Hay tres roles: **Socio** (administrador, acceso total), **Cliente** (catálogo, pedidos, pagos, chatbot) y **Técnico** (solo el evento que se le asigna). **Por ahora solo se construye el rol Socio de forma completa**; Cliente y Técnico serán placeholders.

**Estado actual del proyecto.** `MainActivity` (login/registro con TabLayout + ViewPager2) y `HomeActivity` (BottomNavigationView de 4 pestañas con fragments vacíos: Dashboard, Inventario, Agenda, Pedidos, cambiados con `replace` manual). No hay roles, ni Navigation Component, ni Finanzas. El login deja pasar cualquier texto no vacío.

### Reglas técnicas globales (obligatorias)

1. **Sin decoración.** Interfaz funcional y limpia, con componentes Material estándar. No inventes gradientes, ilustraciones ni animaciones. Más adelante se rediseña con otra herramienta, así que **no hardcodees colores ni tamaños en los layouts**: usa atributos de tema (`?attr/colorSurface`, `?attr/colorOnSurface`, `?attr/colorPrimary`, etc.) y `dimens.xml`. Los estados de equipo/pedido usan colores definidos en `colors.xml` (con variante en `values-night/colors.xml`) **y siempre llevan texto**, nunca solo color.
2. **Sin datos falsos precargados.** Ninguna lista viene con ejemplos. Todo se registra desde la app y se muestra mientras la app corre. Cada pantalla tiene un **estado vacío** claro ("Aún no hay equipo registrado. Toca + para agregar"). Única excepción: la cuenta semilla del Socio (ver Etapa 1).
3. **Datos volátiles, pensados para Firebase.** No hay base de datos todavía (ni Room, ni Firebase, ni archivos). Los datos viven en memoria dentro de repositorios singleton, y se pierden al cerrar la app (es lo esperado). Para que migrar a Firestore sea cambiar solo la implementación:
   - Cada colección tiene una **interfaz** (`EquipoRepository`, etc.) y una **implementación en memoria** (`InMemoryEquipoRepository`). Las pantallas dependen solo de la interfaz, obtenida desde un `AppContainer` creado en la clase `SonidoSonicApp extends Application`.
   - Las lecturas devuelven `LiveData<List<T>>` o `LiveData<T>`; las pantallas **observan** y se actualizan solas cuando cambia un dato. Las pantallas nunca modifican listas directamente.
   - Las escrituras usan una interfaz `Callback<T> { void onSuccess(T r); void onError(String mensaje); }` aunque hoy respondan al instante (así quedan listas para ser asíncronas).
   - Modelos = POJOs con constructor vacío público, getters/setters, `String id` (UUID), fechas como `long` en milisegundos, y estados/roles como constantes `String` (no `enum`), para serializarse directo a Firestore. Dinero como `double` en MXN. Los repositorios no reciben `Context`.
   - Las reglas críticas (bloqueo de equipo, confirmación de pedido, registro de pago) viven **dentro del repositorio**, en un método `synchronized`, y se comentan con `// FIRESTORE: aquí irá una transacción`.
4. **Reglas de negocio vigentes.**
   - RN-01: un equipo En reparación o Dañado no puede asignarse a un evento.
   - RN-02/03: un equipo no puede estar en dos eventos con horario traslapado; verificar y bloquear es una sola operación atómica. Traslape: `inicioA < finB && inicioB < finA`.
   - RN-04: un pedido Pendiente con más de 24 horas sin respuesta se marca con recordatorio (constante `Config.RECORDATORIO_HORAS = 24`, en un solo lugar).
   - RN-05: al confirmar un pedido, el equipo se bloquea y se crea el evento.
   - RN-09: al cancelar o reprogramar un evento se liberan sus bloqueos.
   - RN-13: las finanzas son solo del Socio.
   - RN-14: contraseña con mínimo 8 caracteres, una mayúscula, un número y un carácter especial.
   - El estado físico del equipo (Disponible, En reparación, Dañado) es un dato guardado. **"Rentado" es derivado**: el equipo tiene una reserva vigente en este momento (un evento no cancelado cuyo rango contiene la hora actual).
   - Estados de pedido: Pendiente → Confirmado → Pagado → Entregado (y Cancelado).
5. **Calidad.** Todo texto visible va en `strings.xml` (español). Validaciones con `setError` y mensajes claros. Las pantallas sobreviven a la rotación sin perder lo que el usuario escribió. Listas con `RecyclerView`; textos de al menos 14sp; áreas táctiles de al menos 48dp. Formato de moneda `NumberFormat` locale `es-MX`.
6. **Prohibido en esta fase:** Kotlin, Jetpack Compose, Firebase, Room, Hilt/Dagger, Safe Args, librerías de gráficas, cámara/fotos, notificaciones push, Google Sign-In y datos de ejemplo.
7. **Forma de trabajo.** Antes de escribir código, lee el proyecto y dame un plan breve (archivos a crear, modificar y borrar). Implementa **solo lo de esta etapa**. Al terminar ejecuta `./gradlew assembleDebug`, corrige errores y entrégame: lista de archivos tocados, cómo probar cada criterio de aceptación y cualquier decisión que hayas tomado.

---

## ETAPA 1 — Base: navegación, drawer, tema, sesión, datos y login

**Objetivo.** Reemplazar la estructura actual por una arquitectura de una sola Activity con Navigation Component, drawer lateral en los 3 roles, tema claro/oscuro, sesión, capa de datos en memoria completa y un login con rol validado. El Socio queda con sus 5 pestañas vacías.

**A. Dependencias.** En `libs.versions.toml` y `app/build.gradle.kts` agrega `navigation-fragment` y `navigation-ui` (2.8.5), `recyclerview`, `lifecycle-livedata` y `lifecycle-viewmodel` (versiones estables compatibles con AGP 9.2.1 y compileSdk 36). Conserva `viewpager2` (se usará en Pedidos). Si alguna versión falla, ajústala y avísame.

**B. Estructura de paquetes** (bajo `com.example.propuestasonidosonic`): `ui/auth`, `ui/socio`, `ui/cliente`, `ui/tecnico`, `ui/common`, `data` (interfaces), `data/memory` (implementaciones), `model`, `adapter`, `util`.

**C. `SonidoSonicApp` y `AppContainer`.** Registra la clase en el manifest. `AppContainer` expone todos los repositorios y el `SessionManager`. Al arrancar aplica el modo de tema guardado.

**D. Modelos y repositorios (crearlos todos ahora, con CRUD simple; la lógica de negocio se agrega en etapas posteriores).**
- `Usuario`: id, nombre, correo, telefono, rol (`SOCIO`/`CLIENTE`/`TECNICO`), contrasena (solo en memoria; Firebase Auth la gestionará después), cuentaActiva (boolean), fechaAlta.
- `Equipo`: id, nombre (modelo/tipo, p. ej. `Bocina JBL 15"`), categoria, numeroUnidad (int), estadoFisico (`DISPONIBLE`/`EN_REPARACION`/`DANADO`), precioRenta. `BitacoraEntrada`: id, equipoId, fecha, tipo (`FALLA`/`MANTENIMIENTO`), descripcion, costo.
- `Paquete`: id, nombre, descripcion, precio, `List<Partida>`. `Partida`: tipo (`EQUIPO`/`PAQUETE`), nombre, cantidad (int), precioUnitario.
- `Evento`: id, nombre, inicioMillis, finMillis, direccion, socioResponsableId, tecnicoId (nullable), `List<String> equipoIds`, pedidoId (nullable), estado (`PROGRAMADO`/`CANCELADO`/`FINALIZADO`), notas, `List<ChecklistItem>` (ChecklistItem: equipoId, nombre, salio, regreso, observacion).
- `Pedido`: id, numero (long consecutivo desde 1001), clienteId, clienteNombre, clienteTelefono, canalOrigen (`LLAMADA`/`WHATSAPP`/`APP`), `List<Partida>`, inicioMillis, finMillis, direccion, estado, total, eventoId (nullable), fechaCreacion, fechaActualizacion.
- `Pago`: id, pedidoId, clienteId, monto, tipo (`ANTICIPO`/`TOTAL`), metodo (`EFECTIVO`/`TRANSFERENCIA`), fecha.
- `MovimientoFinanciero`: id, tipo (`INGRESO`/`GASTO`), monto, fecha, concepto, categoria (`RENTA`/`MANTENIMIENTO`/`REPARACION`/`COMPRA`/`OTRO`), pedidoId (nullable), pagoId (nullable), creadoPor.
- `SessionManager`: usuario actual, `iniciarSesion`, `cerrarSesion`.

**E. Cuenta semilla.** Crea una única cuenta Socio al iniciar: `socio@sonidosonic.com` / `Socio#2026`, nombre "Socio Principal" (simula la cuenta que un administrador crea en Firebase).

**F. Navegación.** Elimina `HomeActivity`, `LoginPagerAdapter`, `LoginTabFragment` y `SignUpTabFragment` una vez migrada su lógica. `MainActivity` pasa a ser la única Activity y contiene: `DrawerLayout` → (`MaterialToolbar` + `NavHostFragment` + `BottomNavigationView`) y el panel del drawer.
- `nav_graph.xml`: `loginFragment` (inicio), `registerFragment`, y tres grafos anidados `socio_graph`, `cliente_graph`, `tecnico_graph`. Desde login se navega a cada grafo con `popUpTo` inclusivo del login, para que "Atrás" nunca regrese al login.
- Socio, destinos principales (barra inferior, 5): **Dashboard, Inventario, Agenda, Pedidos, Finanzas**. Cada uno es un fragment con su título y el texto de estado vacío; se llenan en etapas posteriores. La barra inferior solo se muestra en esos 5 destinos. En los demás destinos la barra superior muestra la flecha de regreso; en los principales, el ícono de menú que abre el drawer.
- Cliente y Técnico: un fragment placeholder cada uno ("Próximamente") dentro de su grafo, sin barra inferior.
- Los argumentos entre pantallas se pasan con `Bundle` y claves constantes (sin Safe Args).

**G. Drawer (los 3 roles).** Panel lateral izquierdo, **bloqueado y cerrado en login y registro**. Arriba, nombre y rol del usuario en sesión. **Abajo**, pegadas al fondo, solo dos opciones:
1. **Modo oscuro / Modo claro**: una sola opción cuya etiqueta e ícono cambian según el modo actual. Usa `AppCompatDelegate.setDefaultNightMode`, guarda la elección en `SharedPreferences` y debe conservarse al reabrir la app. Al cambiar el modo la sesión y el destino actual no se pierden.
2. **Cerrar sesión**: pide confirmación, limpia la sesión y vuelve al login borrando toda la pila de navegación.

**H. Tema.** `Theme.Material3.DayNight.NoActionBar`. Elimina los colores fijos actuales (`background_login`, `card_background`, etc.) de los layouts y usa atributos de tema. Define una paleta mínima y neutra para claro y oscuro (`values/` y `values-night/`). Colores de estado: verde = disponible/confirmado, amarillo = en reparación/pendiente, rojo = dañado/cancelado. El logo `sonidosonic` solo aparece en login y registro.

**I. Login.** Campos: correo y contraseña (con mostrar/ocultar), y tres botones: **Entrar como Socio / Cliente / Técnico**. Valida que correo y contraseña existan y sean correctos **y que el rol del botón coincida con el rol de la cuenta**; si no coincide, mensaje claro ("Esta cuenta no tiene el rol de Socio"). Un enlace lleva al registro.

**J. Registro (solo Clientes).** Nombre completo, correo, teléfono, contraseña (con la regla RN-14 visible como ayuda y validada) y confirmar contraseña. Correo único. Al registrar, la cuenta se crea como `CLIENTE` con `cuentaActiva = true` y regresa al login con un mensaje de éxito.

**Criterios de aceptación.**
- La app compila y abre en el login; no existen `HomeActivity` ni el ViewPager de login.
- Socio entra con la cuenta semilla y botón Socio; con el botón Cliente es rechazado. Datos incorrectos son rechazados.
- Un Cliente se registra (probar contraseñas inválidas), inicia sesión como Cliente y ve el placeholder con su drawer.
- El Socio ve las 5 pestañas vacías con su estado vacío; el menú abre el drawer; el drawer no se puede abrir en login.
- El cambio de modo claro/oscuro funciona, se conserva tras cerrar y abrir la app, y no cierra la sesión.
- Cerrar sesión regresa al login y "Atrás" no vuelve a una pantalla protegida.
- Rotar el teléfono en cualquier pantalla no rompe nada.

---

## ETAPA 2 — Inventario, Bitácora y Paquetes

**Objetivo.** Pestaña **Inventario** del Socio totalmente funcional, con dos secciones (TabLayout): **Equipo** y **Paquetes**. Trabaja solo sobre la Etapa 1 ya terminada.

**Lista de Equipo.**
- Cada elemento muestra `nombre (numeroUnidad)`, categoría y **estado efectivo con texto y color**: Disponible, Rentado (derivado de una reserva vigente ahora; aún no habrá eventos, déjalo calculado para cuando existan), En reparación, Dañado. Ordenada por nombre y número de unidad.
- Filtro por estado con **Spinner** (Todos / Disponible / Rentado / En reparación / Dañado).
- Estado vacío cuando no hay equipo.
- FAB **+ Nuevo equipo**.

**Alta y edición de equipo (formulario).** Campos: nombre/modelo, categoría (Spinner: Audio, Iluminación, Micrófonos, Cableado, Accesorios, Otro), precio de renta, estado físico inicial (Disponible / En reparación / Dañado) y **cantidad de unidades iguales** (por defecto 1). Al guardar con cantidad N se crean N unidades con numeración consecutiva que continúa la existente para ese nombre. Validar: nombre obligatorio, precio ≥ 0, cantidad entre 1 y 50, no duplicar (nombre + número de unidad). La edición permite cambiar nombre, categoría, precio y estado físico de una unidad.

**Detalle de equipo.** Al tocar un elemento: datos, estado efectivo, botón **Cambiar estado** (diálogo con los 3 estados físicos; "Rentado" no se elige, es derivado), botón **Editar**, y la **bitácora** (lista de entradas, más reciente primero). Botón **Agregar a bitácora**: tipo (Falla / Mantenimiento), descripción, costo opcional y fecha (hoy por defecto). Si hay costo, un checkbox "Registrar como gasto" que crea un `MovimientoFinanciero` de tipo `GASTO` (categoría `MANTENIMIENTO` o `REPARACION` según el tipo) mediante `FinanzasRepository`. Si se cambia a En reparación o Dañado una unidad que ya tiene eventos futuros, mostrar una advertencia con esos eventos (no bloquear el cambio).

**Paquetes.** Lista con nombre, descripción, precio y composición. FAB **+ Nuevo paquete**. Formulario: nombre, descripción, precio y la **composición** (una lista de partidas con nombre de equipo — elegido de los nombres/modelos ya registrados en inventario — y cantidad; poder agregar y quitar filas). Editar y eliminar paquete (con confirmación). Estado vacío.

**Lógica en repositorio.** `EquipoRepository`: `agregarUnidades`, `actualizar`, `cambiarEstado`, `agregarBitacora`, `obtenerBitacora`, y una consulta `estadoEfectivo(equipo, ahoraMillis)` que consulte las reservas vigentes de `EventoRepository` (que aún estará vacío). `PaqueteRepository`: CRUD.

**Criterios de aceptación.** Crear 3 unidades de un mismo modelo genera números 1, 2, 3; crear otras 2 después continúa en 4 y 5. El filtro por estado funciona. Cambiar estado y agregar entradas a bitácora se refleja de inmediato en la lista y en el detalle. Una entrada con costo y "Registrar como gasto" crea el gasto (aún no se ve en Finanzas, pero queda en el repositorio). Crear, editar y eliminar paquetes funciona. Todas las pantallas muestran su estado vacío y sobreviven a la rotación.

---

## ETAPA 3 — Agenda y Eventos (con bloqueo de equipo)

**Objetivo.** Pestaña **Agenda** del Socio completa, con creación de eventos, bloqueo atómico de equipo, checklist de salida/regreso y reporte de daños. Trabaja sobre las etapas 1 y 2.

**Pantalla Agenda.** `CalendarView` con selección de día y, debajo, la lista de eventos del día seleccionado (nombre, horario, dirección, socio responsable, estado). Si el día tiene **más de un evento**, un aviso visible: "N eventos simultáneos este día" (RF-AGE-07). FAB **+ Nuevo evento**. Los cambios en eventos se reflejan sin recargar (LiveData). Estado vacío por día.

**Formulario de evento (crear y editar).**
- Campos: nombre, fecha (DatePicker), hora de inicio y fin (TimePicker; el fin debe ser posterior al inicio, el evento ocurre dentro de un mismo día), dirección, socio responsable (Spinner de usuarios con rol Socio; por defecto el de la sesión), técnico (Spinner de usuarios con rol Técnico más la opción "Sin técnico"), notas.
- **Equipo asignado**: botón que abre un diálogo de selección múltiple con todas las unidades. Las que no se pueden asignar aparecen **deshabilitadas con el motivo**: "En reparación", "Dañado" o "Ocupado en: <nombre del evento>" (por traslape con ese horario).
- Al guardar se ejecuta **una sola operación atómica y sincronizada** en `EventoRepository` (`crearEvento` / `actualizarEvento`) que verifica estado físico y traslapes, y crea o actualiza el evento solo si todo es válido. Si algo falla, el mensaje indica la unidad y el evento en conflicto. Al editar, el evento se ignora a sí mismo en la verificación.
- Un evento sin equipo es válido (se podrá asignar después).

**Detalle de evento.** Datos completos y equipo asignado. Botones: **Editar**, **Cancelar evento** (confirmación; pasa a `CANCELADO` y libera el equipo, RN-09; un evento cancelado deja de bloquear). Si el evento viene de un pedido, mostrar "Pedido #N".
- **Checklist**: una fila por unidad asignada con casillas **Salió** y **Regresó** y un campo de **observación**. Se guarda al marcar.
- **Reportar daño** por unidad: cambia su estado físico a `DANADO` y crea una entrada `FALLA` en su bitácora con la descripción escrita por el usuario.
- **Finalizar evento**: pasa a `FINALIZADO`. Si hay unidades sin marcar como regresadas, pedir confirmación indicando cuáles.

**Lógica en repositorio.** `EventoRepository`: `crearEvento`, `actualizarEvento`, `cancelarEvento`, `finalizarEvento`, `actualizarChecklist`, `eventosDelDia(fecha)`, `reservasVigentes(ahora)` y `unidadesOcupadas(inicio, fin, ignorarEventoId)` (reutilizable por Pedidos). Conecta `EquipoRepository.estadoEfectivo` con las reservas vigentes para que el inventario ya muestre **Rentado** cuando corresponda.

**Criterios de aceptación.** Crear un evento con 2 unidades y luego intentar crear otro con horario traslapado y una de esas unidades: es rechazado indicando cuál. El mismo equipo en horarios que no se traslapan se acepta. Una unidad En reparación aparece deshabilitada. Cancelar un evento libera las unidades de inmediato. Dos eventos el mismo día muestran el aviso de simultáneos. Un evento que contiene la hora actual hace que sus unidades aparezcan como **Rentado** en Inventario. Reportar daño cambia el estado en Inventario y agrega la entrada de bitácora.

---

## ETAPA 4 — Pedidos, confirmación y pagos

**Objetivo.** Pestaña **Pedidos** completa. Confirmar un pedido crea su evento y bloquea el equipo; el Socio puede registrar pagos. Trabaja sobre las etapas 1 a 3.

**Pantalla Pedidos.** `TabLayout` + `ViewPager2` con dos pestañas: **Pendientes por confirmar** y **Confirmados** (esta incluye Confirmado, Pagado y Entregado, mostrando el estado con texto y color; los cancelados no aparecen). Cada tarjeta: "Pedido #numero", cliente, partidas resumidas, fecha del evento, total y estado. Las tarjetas pendientes que superan `Config.RECORDATORIO_HORAS` muestran la etiqueta "Sin respuesta +24 h" (hacerlo configurable para poder probar con un valor corto). FAB **+ Nuevo pedido**. Estados vacíos.

**Nuevo pedido (formulario).**
- Canal de origen (Llamada / WhatsApp / App).
- **Cliente**: Spinner con los clientes existentes y la opción "Cliente nuevo", que pide nombre y teléfono y crea un `Usuario` con rol `CLIENTE` y `cuentaActiva = false` (cliente sin cuenta; si ya existe uno con ese teléfono, reutilizarlo).
- Fecha del evento, hora de inicio y fin (misma validación que Agenda), dirección.
- **Partidas**: agregar filas eligiendo un equipo (por nombre/modelo, tomado del inventario) o un paquete, con cantidad; el precio unitario se toma del equipo/paquete y es editable. El **total** se calcula y se puede ajustar. Al menos una partida.
- El pedido nace `PENDIENTE` con `fechaCreacion` y número consecutivo.

**Detalle de pedido.** Datos, partidas, total, **pagado**, **saldo**, estado, evento vinculado (si existe) y estos botones según el estado:
- **Confirmar pedido** (si es Pendiente). Operación atómica en `PedidoRepository`: por cada partida de equipo, contar unidades del mismo nombre con estado físico Disponible y sin traslape en ese horario (usar `unidadesOcupadas`); los **paquetes se expanden** a su composición multiplicada por la cantidad. Si falta disponibilidad, rechazar con el mensaje exacto: "Falta Bocina JBL 15": necesitas 2, disponibles 1". Si alcanza, **crear el evento** (nombre "Pedido #N – cliente", mismos horario y dirección, socio responsable = sesión, **unidades asignadas automáticamente**), guardar `eventoId`, y pasar el pedido a `CONFIRMADO`. Todo o nada.
- **Registrar pago** (si está Confirmado o Pagado con saldo): monto, tipo (Anticipo / Total) y método (Efectivo / Transferencia). El monto no puede exceder el saldo. Crea un `Pago` **y** un `MovimientoFinanciero` de tipo `INGRESO` (categoría `RENTA`, concepto "Pago pedido #N", con `pedidoId` y `pagoId`) en una sola operación. Cuando el saldo llega a 0 el pedido pasa a `PAGADO`. Muestra el historial de pagos.
- **Marcar como entregado** (solo si está Pagado).
- **Cancelar pedido** (confirmación): pasa a `CANCELADO` y, si tenía evento, lo cancela y libera el equipo. Los pagos ya registrados no se borran.
- **Contactar por WhatsApp**: `Intent` `ACTION_VIEW` con `https://wa.me/52<telefono>?text=<mensaje prellenado con el número de pedido>`. Si el cliente no tiene teléfono, ocultar el botón.

**Criterios de aceptación.** Crear un pedido con cliente nuevo aparece en Pendientes. Confirmarlo con equipo suficiente crea el evento en Agenda con las unidades bloqueadas y mueve el pedido a Confirmados. Confirmarlo sin disponibilidad suficiente muestra el mensaje exacto y no cambia nada. Un pedido con un paquete bloquea la composición completa. Registrar un anticipo actualiza pagado y saldo; pagar el resto lo pasa a Pagado. Cancelar un pedido confirmado libera el equipo. El recordatorio "+24 h" se ve al usar un valor corto de prueba.

---

## ETAPA 5 — Finanzas y Dashboard

**Objetivo.** Pestañas **Finanzas** y **Dashboard** del Socio funcionales y alimentadas por lo ya registrado. Trabaja sobre las etapas 1 a 4. Sin librerías de gráficas.

**Finanzas.**
- Selector de **período** (Spinner: Este mes / Mes anterior / Últimos 7 días / Todo) y de **tipo** (Spinner: Todos / Ingresos / Gastos).
- Tarjetas de resumen: **Ingresos**, **Gastos** y **Balance** (ingresos menos gastos) del período, con el balance en verde o rojo y su signo en texto.
- Lista de movimientos del período (concepto, categoría, fecha, monto con signo). Los ingresos generados por pagos aparecen automáticamente.
- Resumen simple de ingresos y gastos por día o categoría del período usando filas con `ProgressBar` horizontal (gráfica sencilla sin librerías).
- FAB **+ Registrar movimiento**: tipo (Ingreso/Gasto), monto, fecha, concepto, categoría (Spinner) y pedido asociado opcional.
- Estados vacíos.
- Eliminar un movimiento manual con confirmación; los movimientos creados por pagos **no** se pueden eliminar desde aquí.

**Dashboard.** Tarjetas que se actualizan solas con LiveData:
- **Eventos de la semana** (conteo de eventos no cancelados de la semana actual).
- **Equipo fuera** (unidades con estado efectivo Rentado ahora) y **En reparación** (conteo).
- **Pedidos por confirmar** (conteo de Pendientes, resaltando los que tienen recordatorio vencido).
- **Balance mensual** (ingresos − gastos del mes actual).
- **Ingresos de los últimos 7 días**: una fila por día con su monto y una barra proporcional (`ProgressBar`).
- **Equipo más rentado** y **Equipo con más fallas** (por nombre/modelo: contando unidades en eventos no cancelados y entradas `FALLA` en bitácora; si no hay datos, "Sin datos").
- Con la app vacía todo muestra ceros/"Sin datos" y un texto guía.

**Criterios de aceptación.** Con la app vacía el Dashboard y Finanzas muestran ceros y estados vacíos. Registrar un pago en un pedido sube el ingreso en Finanzas y el balance y la gráfica del Dashboard sin recargar. Registrar un gasto lo baja. Cambiar de período recalcula todo. Cancelar un evento actualiza "Eventos de la semana". Una unidad en reparación se refleja en el Dashboard.

---

## ETAPA 6 — Usuarios y Clientes

**Objetivo.** Que el Socio administre cuentas y vea a sus clientes, sin agregar opciones al drawer (solo tiene Modo oscuro/claro y Cerrar sesión). Trabaja sobre las etapas anteriores.

**Acceso.** Un ícono de **Usuarios** en la barra superior (menú de la Toolbar) visible solo en los destinos principales del Socio, que abre la pantalla **Usuarios** (destino secundario con flecha de regreso).

**Pantalla Usuarios.** Dos pestañas:
1. **Clientes**: lista de clientes que tienen al menos un pedido en estado Confirmado, Pagado o Entregado (una venta cerrada). Cada elemento: nombre, teléfono, correo (si lo hay), indicador "Con cuenta" / "Sin cuenta", número de pedidos y total pagado. Al tocar, un detalle con sus pedidos (tocar uno abre el detalle del pedido). Estado vacío: "Aún no hay clientes con ventas cerradas".
2. **Equipo de trabajo**: lista de Socios y Técnicos (nombre, correo, teléfono, rol). FAB **+ Nuevo usuario**: formulario con nombre, correo, teléfono, rol (Spinner Socio / Técnico) y contraseña inicial con la regla RN-14. Correo único. La cuenta queda activa. No se puede eliminar la propia cuenta ni la última cuenta Socio; permitir eliminar Técnicos y otros Socios con confirmación.

**Reglas.** Los clientes que se registran solos (Etapa 1) y los creados al capturar un pedido aparecen en la pestaña Clientes **solo cuando tienen una venta cerrada**. Si alguien se registra con un correo o teléfono que ya existe como cliente sin cuenta, se vincula y activa esa cuenta en lugar de duplicarla. Los Técnicos creados aquí ya aparecen en el Spinner de técnico de Agenda. Un Técnico creado puede iniciar sesión con el botón Técnico y ve su placeholder.

**Criterios de aceptación.** Un Cliente se registra por su cuenta pero no aparece en Clientes hasta que el Socio le confirma un pedido. Un cliente creado por teléfono aparece con "Sin cuenta" tras confirmar su pedido. El Socio crea un Técnico y otro Socio; ambos inician sesión con su rol (y son rechazados con otro botón). Las contraseñas inválidas son rechazadas. No se puede eliminar la última cuenta Socio.

---

## ETAPA 7 — PDF: cotización y comprobantes

**Objetivo.** Generar y compartir PDFs desde el detalle del pedido. Sin librerías externas: usa `android.graphics.pdf.PdfDocument`.

**Documentos** (encabezado con el nombre del negocio "Sonido Sonic", número de pedido y fecha de emisión):
- **Cotización** (disponible en cualquier estado excepto Cancelado): cliente, fecha, horario y dirección del evento, tabla de partidas (concepto, cantidad, precio unitario, importe) y total.
- **Comprobante de renta** (pedido Confirmado o posterior): lo anterior más estado del pedido, total pagado y saldo.
- **Comprobante de pago** (uno por cada pago del historial): pedido, cliente, monto, tipo, método, fecha y saldo restante.

**Detalle de pedido.** Botones **Cotización PDF** y **Comprobante de renta**, y en cada pago del historial **Comprobante**. Cada uno genera el PDF **fuera del hilo principal** (`Executor`), lo guarda en el almacenamiento de caché de la app, lo comparte con `FileProvider` + `ACTION_SEND` (`application/pdf`) y muestra un indicador de progreso mientras se genera. Registra el `FileProvider` en el manifest con su `file_paths.xml`. Los textos en `strings.xml` y el formato de moneda `es-MX`.

**Criterios de aceptación.** Desde un pedido con partidas se genera la cotización y se abre el selector de compartir. El comprobante de renta refleja pagado y saldo correctos. Cada pago genera su comprobante con sus datos. La interfaz no se congela mientras se genera.

---

## Pendiente para la fase de Firebase (no incluir ahora)

Autenticación real (correo y Google), Firestore, Storage, fotos de perfil/equipo/evidencia con cámara, modo sin imágenes, notificaciones FCM (recordatorio de 24 h, cambio de estado, pago), persistencia local con Room, gráficas con MPAndroidChart, y los módulos del Cliente (catálogo, mis pedidos, pagos sandbox, Sonic Bot) y del Técnico (evento asignado y evidencia).
