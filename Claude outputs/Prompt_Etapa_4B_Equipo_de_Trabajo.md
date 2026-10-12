# ETAPA 4B — Equipo de trabajo (Socio): crear Socios y Asistentes, y marcar quién trabaja cada día

> Pega primero el **CONTEXTO GLOBAL** y la **ADENDA v3**. Luego pega este texto.
> Requisito: Etapas 1, 1.1, 1.2, 2, 3 y 4A terminadas (40 pruebas en verde).

---

## OBJETIVO

El motor de disponibilidad (4A) decide cuántos eventos caben según las **personas disponibles**: los Socios activos (salvo los marcados como no disponibles ese día) y los Asistentes activos **marcados como disponibles ese día**. Hoy solo existe un Socio, así que la capacidad real es de 1 evento a la vez. Esta etapa agrega la pantalla para que el Socio:

1. **Cree cuentas** de Socio y de Asistente, las edite y las active o desactive.
2. **Marque, día por día, quién trabaja** (la tabla `DiaOperativo` que ya existe).

Los Clientes no se crean aquí (se registran solos) y su lista se hará más adelante junto con Pedidos.

## PASO 0 — Revisión previa (no edites todavía; entrégame hallazgos y plan breve)

Lee y repórtame:

1. `Usuario.java`, `UsuarioRepository.java`, `InMemoryUsuarioRepository.java`: cómo se guarda la contraseña, qué hacen `registrarUsuario`, `actualizarUsuario`, `autenticar`, y qué validaciones ya existen (correo único, formato).
2. `SessionManager.java`: cómo guarda al usuario actual y si se puede refrescar cuando ese usuario cambia.
3. `util/PasswordValidator.java` y `util/FormatUtils.java`.
4. `MainActivity.java` y `activity_main.xml` (estructura del drawer).
5. `Config.java`: los alias `CANAL_APP`, `CANAL_LLAMADA`, `CANAL_WHATSAPP` que dejaste en `Pedido` en la 4A: dime si algo los usa. Si **nada** los usa, elimínalos en esta etapa y reporta que lo hiciste.

## PASO 1 — Reglas de negocio (van dentro del repositorio, no en la pantalla)

Agrega a `UsuarioRepository` / `InMemoryUsuarioRepository` (mensajes de error en español, misma convención de siempre; bloque `synchronized` con el comentario `// FIRESTORE: transacción`):

### 1.1 `void crearPersonal(Usuario nuevo, String contrasena, Callback<Usuario> callback)`

Validaciones:

1. El rol debe ser `ROL_SOCIO` o `ROL_ASISTENTE` (nunca `ROL_CLIENTE`).
2. Nombre completo obligatorio (máximo 80 caracteres).
3. Correo con formato válido y **único** (sin distinguir mayúsculas), con el mismo mensaje que ya usa el registro de clientes.
4. Teléfono con al menos 10 dígitos.
5. Contraseña válida según `PasswordValidator` (mínimo 8 caracteres, una mayúscula, un número y un carácter especial).
6. La cuenta se crea **activa**.

### 1.2 `void actualizarPersonal(String actorId, String usuarioId, String nombre, String telefono, Boolean cuentaActiva, String nuevaContrasena, Callback<Usuario> callback)`

- `actorId` es quien hace el cambio (el usuario con sesión iniciada).
- El usuario a editar debe existir y ser Socio o Asistente. **El correo y el rol no se pueden cambiar.**
- Los parámetros nulos o vacíos significan "no cambiar" (`nombre`, `telefono`, `cuentaActiva`, `nuevaContrasena`). Si llega `nombre` o `telefono`, aplican las mismas validaciones de arriba. Si llega `nuevaContrasena`, debe pasar `PasswordValidator`.
- **Nadie puede desactivarse a sí mismo** (`actorId == usuarioId` con `cuentaActiva == false` → error "No puedes desactivar tu propia cuenta").
- **No se puede desactivar al último Socio activo** → error "Debe quedar al menos un Socio activo".
- No existe borrado de personal desde la pantalla: solo se desactiva (las reservas históricas lo siguen mencionando). No uses `eliminarUsuario`.
- Si el usuario editado es el de la sesión actual, refresca el usuario en `SessionManager` para que el encabezado del drawer muestre el nombre nuevo.
- Una cuenta desactivada ya no puede iniciar sesión (mensaje existente `error_cuenta_inactiva`); verifica que `autenticar` lo cumple.

### 1.3 `PedidoRepository`: `List<Pedido> obtenerReservasActivasDeOperador(String operadorId, long desdeMillis, long hastaMillis)`

Síncrono, devuelve copias de las reservas que **bloquean** recursos (`MotorDisponibilidad.bloquea(p, ahora)`), incluyen a `operadorId` en `operadorIds` y cuya ventana de bloqueo se traslapa con `[desdeMillis, hastaMillis)` (usa `TiempoUtils.solapan`). Sirve para los avisos de las pantallas.

### 1.4 `DiaOperativoRepository`

Ya existe `guardarDia`. Verifica que al guardar se eliminen ids duplicados dentro de cada lista.

## PASO 2 — Navegación y drawer

1. **Ícono:** crea `res/drawable/ic_group_24.xml` (vector de 24dp, `tint="?attr/colorControlNormal"`) con el ícono Material "people":
   `M16,11c1.66,0 2.99,-1.34 2.99,-3S17.66,5 16,5c-1.66,0 -3,1.34 -3,3s1.34,3 3,3zM8,11c1.66,0 2.99,-1.34 2.99,-3S9.66,5 8,5C6.34,5 5,6.34 5,8s1.34,3 3,3zM8,13c-2.33,0 -7,1.17 -7,3.5V19h14v-2.5c0,-2.33 -4.67,-3.5 -7,-3.5zM16,13c-0.29,0 -0.62,0.02 -0.97,0.05 1.16,0.84 1.97,1.97 1.97,3.45V19h6v-2.5c0,-2.33 -4.67,-3.5 -7,-3.5z`
2. **Drawer (`activity_main.xml`):** hasta ahora tenía solo 2 opciones al fondo. Agrega una opción nueva **arriba**, entre el divisor del encabezado y el espacio flexible, con id `drawerItemEquipo`, mismo estilo que las otras (`ic_group_24`, texto `menu_equipo_trabajo`, altura mínima 48dp). **Las dos opciones de abajo (modo oscuro/claro y cerrar sesión) no cambian de lugar.**
3. **Visibilidad:** `drawerItemEquipo` solo se ve cuando el usuario de la sesión es **Socio** (`GONE` para Cliente y Asistente). Actualízalo en el mismo observador de sesión que ya llena el encabezado.
4. **Navegación:** en `nav_graph.xml`, dentro de `socio_graph`, agrega el destino `usuariosFragment` (clase `ui.socio.UsuariosFragment`, layout `fragment_usuarios`, label `@string/titulo_equipo_trabajo`). Al tocar `drawerItemEquipo`: cierra el drawer y navega a `usuariosFragment` con `launchSingleTop`; si ya estás ahí, solo cierra el drawer. Sin Safe Args.
5. **`MainActivity`:** este destino cae en la rama `else` actual (barra superior con flecha, barra inferior oculta). Verifica que "Atrás" y la flecha regresan a la pantalla desde donde se abrió. No cambies la lógica de "Atrás" de las pestañas.

## PASO 3 — Textos (`strings.xml`, español de México; añade los que necesites)

- `menu_equipo_trabajo` = "Equipo de trabajo", `titulo_equipo_trabajo` = "Equipo de trabajo"
- `tab_personal` = "Personal", `tab_disponibilidad` = "Disponibilidad"
- `badge_rol_socio` = "Socio", `badge_rol_asistente` = "Asistente", `badge_cuenta_activa` = "Activa", `badge_cuenta_inactiva` = "Inactiva"
- `dialog_nuevo_personal_titulo` = "Nueva cuenta", `dialog_editar_personal_titulo` = "Editar cuenta"
- `label_rol_cuenta` = "Tipo de cuenta"; `hint_nueva_contrasena_opcional` = "Nueva contraseña (opcional)"; `hint_contrasena_temporal` = "Contraseña temporal"
- `label_cuenta_activa` = "Cuenta activa"
- `aviso_correo_no_editable` = "El correo y el tipo de cuenta no se pueden cambiar"
- `msg_personal_creado` = "Cuenta creada. Comparte el correo y la contraseña temporal con la persona."
- `msg_personal_actualizado` = "Cambios guardados"
- `dialog_desactivar_titulo` = "¿Desactivar cuenta?"; `dialog_desactivar_con_reservas_formato` = "%1$s aparece en %2$d reserva(s) futura(s). Seguirán asignadas a esta persona hasta que las reasignes. ¿Desactivar de todas formas?"; `dialog_desactivar_sin_reservas` = "Esta persona ya no podrá iniciar sesión ni se le asignarán eventos nuevos."
- `empty_disponibilidad_sin_asistentes` = "Aún no hay Asistentes. Crea uno en la pestaña Personal."
- `label_trabaja_este_dia` = "Trabaja este día"; `label_disponible_este_dia` = "Disponible este día"
- `resumen_personal_dia_formato` = "Personal disponible ese día: %1$d"
- `aviso_dia_pasado` = "Este día ya pasó. No se puede modificar."
- `dialog_quitar_dia_con_reservas_formato` = "%1$s tiene %2$d reserva(s) ese día. Seguirán asignadas a esta persona hasta que las reasignes. ¿Quitarla de todas formas?"
- `btn_elegir_fecha` = "Elegir fecha", `desc_dia_anterior` = "Día anterior", `desc_dia_siguiente` = "Día siguiente"

## PASO 4 — Pantalla `UsuariosFragment` (`fragment_usuarios.xml`)

Estructura: `TabLayout` `tabsUsuarios` (fixed, fill) con "Personal" y "Disponibilidad", y debajo dos contenedores que se alternan por visibilidad (mismo patrón que Inventario: `framePersonal` y `frameDisponibilidad`). Guarda la pestaña elegida en un campo y en `onSaveInstanceState`; por defecto "Personal".

### 4.1 Pestaña Personal

- `RecyclerView` `rvPersonal` (`clipToPadding="false"`, `paddingBottom="96dp"`) con tarjetas `item_personal.xml` (mismo estilo que `item_equipo.xml`): nombre (negrita), insignia de **rol** (Socio/Asistente), insignia de **estado** (Activa en `status_disponible`, Inactiva en tonos neutros), correo y teléfono. Texto de 14sp o más, sin colores fijos. Reutiliza `BadgeUtils.pintar`.
- Orden: Socios primero, luego Asistentes; dentro de cada grupo, alfabético sin distinguir mayúsculas. Se muestran **solo** Socios y Asistentes (ni Clientes ni cuentas de otros roles).
- Un `FloatingActionButton` `fabNuevoPersonal` (solo visible en esta pestaña; con el mismo relleno inferior para no tapar la última tarjeta) abre `PersonalFormBottomSheet` en modo "nuevo". Tocar una tarjeta lo abre en modo "edición".

### 4.2 `ui/socio/PersonalFormBottomSheet.java` + `res/layout/dialog_personal_form.xml`

Mismo patrón visual y de código que `EquipoFormBottomSheet`/`dialog_equipo_form.xml` (barra de arrastre, título, botones al final).

- **Nuevo:** `MaterialButtonToggleGroup` `toggleRol` (Socio | Asistente; por defecto **Asistente**), nombre completo, correo, teléfono, contraseña temporal y confirmación. Guardar llama a `crearPersonal`. Al terminar bien: cierra, Toast `msg_personal_creado`.
- **Edición:** el rol y el correo se muestran **deshabilitados** con la nota `aviso_correo_no_editable`; se pueden cambiar nombre, teléfono, "Cuenta activa" (`MaterialSwitch`) y una contraseña nueva opcional. Guardar llama a `actualizarPersonal(actorId = usuario de la sesión, …)`.
- **Al desactivar** (switch pasa de activa a inactiva): antes de guardar, consulta `obtenerReservasActivasDeOperador(id, ahora, Long.MAX_VALUE)`. Si hay reservas, muestra el diálogo `dialog_desactivar_con_reservas_formato`; si no, `dialog_desactivar_sin_reservas`. Solo guarda si confirma.
- Errores del repositorio: bajo el campo correspondiente cuando sea evidente (correo repetido, contraseña débil) y, en cualquier otro caso, en un diálogo; **sin cerrar el formulario ni borrar lo escrito**. Deshabilita el botón mientras procesa. Cierra el teclado al terminar.
- Zonas táctiles de 48dp o más.

### 4.3 Pestaña Disponibilidad

- **Selector de día:** una fila con botón de día anterior (`<`), la fecha larga en español de México tocable (por ejemplo "sábado 14 de noviembre de 2026"; agrega `FormatUtils.fechaLarga(long)` con `SimpleDateFormat("EEEE d 'de' MMMM 'de' yyyy", new Locale("es","MX"))` y la zona de `Config.ZONA_HORARIA`) y botón de día siguiente (`>`). Tocar la fecha abre un `MaterialDatePicker`.
  - **Cuidado con la zona horaria:** `MaterialDatePicker` devuelve la fecha como medianoche en **UTC**. Para obtener el día correcto, extrae año, mes y día con un `Calendar` en UTC y construye el día local con `TiempoUtils.construir(anio, mes0, dia, 0)`.
  - Por defecto se muestra **hoy**. Guarda el día elegido en `onSaveInstanceState`.
- **Lista de personal activo** (`rvDisponibilidad`, tarjetas `item_disponibilidad_operador.xml`): nombre, insignia de rol y un `MaterialSwitch`.
  - **Socios:** el switch es "Disponible este día" y arranca **encendido**, salvo que su id esté en `sociosNoDisponiblesIds` de ese día.
  - **Asistentes:** el switch es "Trabaja este día" y arranca **apagado**, salvo que su id esté en `asistentesDisponiblesIds` de ese día.
  - Solo aparecen cuentas **activas**.
- **Guardado inmediato:** al mover un switch, carga el `DiaOperativo` del día (o crea uno con `fechaClave = TiempoUtils.claveDia(...)`), actualiza la lista que corresponda sin duplicados y llama a `guardarDia`. Sin botón "Guardar".
- **Aviso al quitar a alguien:** si se apaga a una persona que tiene reservas ese día (`obtenerReservasActivasDeOperador(id, inicioDelDía, inicioDelDía + 24 h)`), muestra `dialog_quitar_dia_con_reservas_formato`; si cancela, el switch regresa a su estado anterior.
- **Resumen:** `tvResumenPersonalDia` con `resumen_personal_dia_formato` (cuántas personas cuentan ese día: Socios disponibles + Asistentes marcados).
- **Días pasados** (anteriores a hoy): los switches quedan deshabilitados y se muestra `aviso_dia_pasado`. Hoy y fechas futuras se pueden editar.
- Si no hay Asistentes activos, muestra `empty_disponibilidad_sin_asistentes` debajo de la lista de Socios.
- La lista y el resumen se refrescan cuando cambian los usuarios o los días operativos (observa ambos `LiveData`).

## PASO 5 — Pruebas unitarias (obligatorias)

Crea `Etapa4BUnitTest.java` (con `InstantTaskExecutorRule` y el `Reloj` falso de la 4A donde haga falta):

1. `crearPersonal` válido (Asistente y Socio) → queda activo, con el rol correcto.
2. `crearPersonal` rechaza: rol Cliente, correo repetido (con otras mayúsculas), contraseña débil, teléfono corto, nombre vacío.
3. `actualizarPersonal`: cambia nombre y teléfono; el correo y el rol no cambian.
4. Un Socio **no puede desactivarse a sí mismo**; **no se puede desactivar al último Socio activo**; con dos Socios activos sí se puede desactivar al otro.
5. Una cuenta desactivada no puede iniciar sesión; `obtenerActivosPorRol` no la incluye.
6. Cambiar el nombre del usuario de la sesión actualiza `SessionManager`.
7. `obtenerReservasActivasDeOperador` devuelve solo reservas que bloquean, del operador y dentro del rango.
8. **Prueba de integración con el motor:** crea un segundo Socio con `crearPersonal`; entonces dos reservas simultáneas (equipo disjunto) con `crearReserva` son viables con operadores distintos y la tercera falla; marca a un Asistente como disponible ese día con `guardarDia` y la tercera pasa; marca a un Socio como no disponible y vuelve a fallar.

## ALCANCE: LO QUE NO SE HACE

Sin lista de Clientes, sin pantalla de Pedidos, sin reasignar operadores, sin cambio forzado de contraseña en el primer inicio, sin marcado de varios días a la vez, sin datos de ejemplo, sin tocar `EventoRepository` ni el motor. No cambies el comportamiento de ninguna otra pantalla.

## REGLAS DE CALIDAD

Textos en `strings.xml`; dimensiones desde `dimens.xml`; sin colores fijos; claro y oscuro; zonas táctiles de 48dp o más; texto de 14sp o más; `contentDescription` en iconos; escrituras con `Callback<T>` y lecturas con `LiveData`; sin librerías nuevas ni Kotlin.

## CRITERIOS DE ACEPTACIÓN (en este orden)

1. Entra como `socio@sonidosonic.com`. El drawer muestra "Equipo de trabajo" arriba y, abajo, modo oscuro/claro y cerrar sesión como siempre. Entra como Cliente (se registra uno) y como Asistente: ahí **no** aparece "Equipo de trabajo".
2. "Equipo de trabajo" abre la pantalla con las pestañas Personal y Disponibilidad; en Personal aparece el Socio inicial (Activa). "Atrás" regresa a la pantalla anterior.
3. Crea un Asistente (por ejemplo `ana@correo.com`) y un segundo Socio. Ambos aparecen con su rol y su estado. Intentar repetir el correo con otras mayúsculas falla sin borrar lo escrito; una contraseña débil muestra el error.
4. Cierra sesión y entra con la cuenta del Asistente desde la pestaña Asistente: funciona. Entra con ella desde la pestaña Socio: es rechazada por rol.
5. Como Socio, desactiva al Asistente: la tarjeta pasa a "Inactiva". Cierra sesión: esa cuenta ya no puede iniciar sesión. Reactívala.
6. Intenta desactivar tu propia cuenta: se rechaza. Con dos Socios activos, desactiva al segundo y luego intenta desactivar al primero: se rechaza por ser el último.
7. Edita tu propio nombre: el encabezado del drawer lo refleja de inmediato.
8. Pestaña Disponibilidad: aparece hoy; Socios con el switch encendido, Asistente con el switch apagado. Enciende al Asistente: el resumen sube en uno. Pasa al día siguiente con `>`: el Asistente vuelve a aparecer apagado (cada día es independiente). Regresa y sigue encendido.
9. Elige una fecha con el selector: se muestra el día **exacto** elegido (no el anterior). Un día pasado muestra los switches deshabilitados y el aviso.
10. Rota el teléfono en ambas pestañas: se conservan la pestaña, el día elegido y los switches.
11. Modo oscuro: todo legible, sin colores fijos nuevos.
12. `./gradlew test` y `./gradlew assembleDebug` (en Windows `.\gradlew.bat ...`) pasan sin errores, incluyendo las pruebas anteriores.

## AL TERMINAR

Entrégame: (a) hallazgos del Paso 0 (incluye si eliminaste los alias `CANAL_*`), (b) archivos creados y modificados, (c) decisiones fuera de lo descrito, (d) salida de `test` y `assembleDebug` con la lista de pruebas, y (e) cómo probaste cada criterio y cualquier ambigüedad que resolviste.
