# ETAPA 1.2 — Renombrar "Técnico" a "Asistente" (solo nombres, cero cambios de comportamiento)

> Pega primero el **CONTEXTO GLOBAL** (archivo `Prompts_Antigravity_Socio_SonidoSonic.md`) y después la **ADENDA v3** de abajo. Luego pega el resto de este texto.
> La ADENDA v3 se pega también al inicio de **todas** las etapas siguientes, justo después del CONTEXTO GLOBAL.

---

## ADENDA v3 AL CONTEXTO GLOBAL (vigente desde esta etapa)

1. **Terminología.** El rol "Técnico" se llama ahora **"Asistente"** en toda la app, el código y los textos. Es un trabajador de apoyo que puede cubrir un evento él solo. Internamente, "operador" es cualquier persona (Socio o Asistente) que puede cubrir un evento; todavía no existe en pantalla.
2. **Reservas.** El objeto `Pedido` evolucionará hacia una *reserva* con origen (App, Llamada, WhatsApp, Presencial), horario con hora de inicio y duración, anticipo y estados nuevos. **No lo toques en esta etapa ni en las siguientes hasta que se te pida.**
3. **Paquetes.** El negocio vende **paquetes** formados por modelos de equipo con cantidades. La disponibilidad se calculará por **existencias** (cuántas unidades de cada modelo hay libres en ese horario), no por la identidad del paquete.
4. **El cliente nunca ve "ocupado".** Solo se le mostrarán fechas y horarios viables; los no viables simplemente no aparecen.
5. **Se mantiene todo lo demás del CONTEXTO GLOBAL**: Java, Views XML, Material 3, Navigation Component, repositorios en memoria listos para Firebase, sin datos de ejemplo, sin colores fijos, textos en `strings.xml`, prohibido Kotlin/Compose/Firebase/Room/Hilt/Safe Args.

---

## OBJETIVO

Cambiar el nombre "Técnico" por "Asistente" en **todo** el proyecto **PropuestaSonidoSonic**, sin alterar nada del comportamiento. Al final no debe quedar ninguna aparición de "técnico/tecnico/tecnic" en `app/src`.

## PASO 0 — Inventario (no edites todavía)

Busca en todo `app/src` (código, XML y comentarios), sin distinguir mayúsculas y **con y sin acento**, estos patrones: `tecnic`, `técnic`. Entrégame primero la lista completa de archivos y líneas encontradas, agrupada así: constantes y modelos, repositorios, fragmentos, layouts, navegación, strings, comentarios. Espera mi visto bueno solo si encuentras algo que no esté en la tabla del Paso 1; si todo está en la tabla, continúa.

## PASO 1 — Tabla de cambios (aplícala completa)

| Antes | Después |
|---|---|
| `Usuario.ROL_TECNICO` (valor `"TECNICO"`) | `Usuario.ROL_ASISTENTE` (valor `"ASISTENTE"`) |
| Cualquier campo, getter o setter `tecnicoId`, `tecnicoNombre`, `tecnicoAsignado`, etc. (si existen en `Evento`, `Pedido`, `ChecklistItem` o `BitacoraEntrada`) | Mismo nombre con `asistente` (`asistenteId`, `asistenteNombre`, …), con getters y setters coherentes |
| Paquete `ui.tecnico` | `ui.asistente` (actualiza la línea `package` y los `import`) |
| Clase `TecnicoHomeFragment` | `AsistenteHomeFragment` |
| Layout `fragment_tecnico_home.xml` | `fragment_asistente_home.xml` (actualiza `R.layout...` y `tools:layout`) |
| `nav_graph.xml`: grafo `tecnico_graph` | `asistente_graph` |
| `nav_graph.xml`: destino `tecnicoHomeFragment` | `asistenteHomeFragment` (actualiza también el `android:name` de la clase) |
| `nav_graph.xml`: acción `action_login_to_tecnico_graph` | `action_login_to_asistente_graph` |
| `strings.xml`: `tab_tecnico` = "Técnico" | `tab_asistente` = "Asistente" |
| `strings.xml`: `error_rol_no_coincide_tecnico` | `error_rol_no_coincide_asistente` = "Esta cuenta no tiene el rol de Asistente" |
| `strings.xml`: `drawer_header_rol_tecnico` = "Técnico" | `drawer_header_rol_asistente` = "Asistente" |
| `strings.xml`: `placeholder_tecnico_titulo` | `placeholder_asistente_titulo` = "Módulo de Asistente" |
| Comentarios y KDoc/Javadoc con "técnico" | "asistente" |

## PASO 2 — Archivos que seguro cambian (verifica cada uno)

1. `MainActivity.java`: las dos apariciones de `R.id.tecnicoHomeFragment` (conjunto de destinos de nivel superior y lógica de "Atrás"), y `R.string.drawer_header_rol_tecnico` en el observador de sesión. La rama `else` que hoy cae en "técnico" debe pasar a comparar explícitamente con `Usuario.ROL_ASISTENTE`.
2. `LoginFragment.java`: el mapeo pestaña → rol (la tercera pestaña pasa a `ROL_ASISTENTE`), el mensaje de error de rol que no coincide y la navegación al grafo del asistente (`action_login_to_asistente_graph`). **El orden de las pestañas se conserva: Socio | Cliente | Asistente.** La pestaña por defecto sigue siendo Cliente.
3. `RegisterFragment.java` y `InMemoryUsuarioRepository.java`: solo si contienen referencias; el registro sigue siendo exclusivo de Clientes.
4. `AsistenteHomeFragment.java` y su layout: solo el renombre; el texto visible sigue siendo el placeholder "Módulo de Asistente" con "Próximamente".
5. `activity_main.xml`, `bottom_menu.xml`, `themes.xml`, `colors.xml`: solo si aparecen en la búsqueda del Paso 0.

## PASO 3 — Reglas

- Renombra archivos y clases con las herramientas de refactor del IDE o con movimientos de archivo, no copiando y pegando contenido, para no dejar duplicados ni archivos huérfanos.
- No cambies lógica, estilos, colores ni diseño. No agregues funciones.
- No dejes los nombres viejos como alias ni como `@Deprecated`.
- Los datos están en memoria, así que no hace falta migrar nada.

## CRITERIOS DE ACEPTACIÓN

1. Una búsqueda sin distinguir mayúsculas de `tecnic` y `técnic` en todo `app/src` devuelve **0 resultados**.
2. `./gradlew assembleDebug` (en Windows: `.\gradlew.bat assembleDebug`) termina sin errores ni advertencias nuevas.
3. El login muestra las pestañas **Socio | Cliente | Asistente**; el enlace "Regístrate aquí" aparece solo en Cliente; la pestaña inicial es Cliente.
4. `socio@sonidosonic.com` / `Socio#2026` entra con la pestaña Socio y es rechazado desde Asistente con el mensaje "Esta cuenta no tiene el rol de Asistente".
5. Un Cliente registrado entra por Cliente y su encabezado del drawer dice "Cliente". El de un Socio dice "Socio Administrador".
6. Un usuario con rol Asistente (créalo temporalmente en una prueba rápida y quítalo después, no dejes datos de ejemplo) llega a "Módulo de Asistente", su drawer dice "Asistente", y "Atrás" lo manda a segundo plano.
7. El comportamiento del Socio no cambió: pestañas inferiores, "Atrás" hacia Dashboard, Inventario completo, modo claro/oscuro y cerrar sesión.

## AL TERMINAR

Entrégame: (a) la lista de archivos renombrados y modificados, (b) el resultado de la búsqueda final con 0 resultados, (c) el resultado de `assembleDebug`, y (d) cualquier decisión que hayas tomado fuera de la tabla.
