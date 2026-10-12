# ETAPA 1.1 — Ajustes: botón "Atrás" y selector de rol por pestañas

> Pega primero el **CONTEXTO GLOBAL** (del archivo de prompts anterior) y después este texto. Esta etapa solo corrige comportamiento; no agregues funciones nuevas ni datos.

Trabajas sobre la Etapa 1 ya terminada de **PropuestaSonidoSonic**. Hay dos ajustes. Antes de codificar, lee `MainActivity.java`, `nav_graph.xml`, `fragment_login.xml`, `LoginFragment.java` y `strings.xml`, y dame un plan breve.

---

## Ajuste A — El botón "Atrás" del teléfono en el Socio

**Problema.** Al pasar por varias pestañas de la barra inferior (por ejemplo Dashboard → Inventario → Agenda → Pedidos) y presionar "Atrás", la app regresa pestaña por pestaña en el orden visitado.

**Causa probable.** `NavigationUI.setupWithNavController(bottomNavigationView, navController)` limpia la pila hasta el destino inicial del **grafo raíz** (`loginFragment`), que ya no está en la pila, así que cada pestaña visitada se acumula.

**Comportamiento requerido.**
- Desde cualquier pestaña del Socio distinta de Dashboard, "Atrás" regresa **directamente a Dashboard**, sin pasar por las demás pestañas.
- Desde Dashboard, "Atrás" **sale de la app** (la manda a segundo plano). Nunca regresa al login ni a otra pestaña.
- Con el drawer abierto, "Atrás" lo cierra primero (ya funciona; conservarlo).
- En Cliente y Técnico (placeholders), "Atrás" sale de la app. En Registro, "Atrás" vuelve al login.
- Debe seguir funcionando cuando existan pantallas de detalle dentro de una pestaña (futuras etapas): desde un detalle, "Atrás" regresa a su pestaña; desde la pestaña, a Dashboard.

**Cómo resolverlo.** Quita el `NavigationUI.setupWithNavController(bottomNavigationView, navController)` y maneja la barra inferior con tu propio `setOnItemSelectedListener`:
- Al elegir una pestaña distinta de la actual, navega a ella con `NavOptions` `setLaunchSingleTop(true)` y `setPopUpTo(R.id.nav_dashboard, false)`, de modo que la pila quede siempre como `[Dashboard, pestaña actual]`.
- Al elegir Dashboard, usa `navController.popBackStack(R.id.nav_dashboard, false)`.
- Añade un `setOnItemReselectedListener` que no haga nada.
- En el `OnDestinationChangedListener` existente, mantén **sincronizada la pestaña marcada** de la barra inferior con el destino actual (por ejemplo `bottomNavigationView.getMenu().findItem(id).setChecked(true)`), para que funcione también cuando el cambio venga de "Atrás".
- No uses `saveState`/`restoreState` en estas navegaciones.

**Criterios de aceptación.**
1. Dashboard → Inventario → Agenda → Pedidos → "Atrás" → queda en **Dashboard**, con su pestaña marcada.
2. Estando en Dashboard → "Atrás" → la app se va a segundo plano (no aparece el login ni otra pestaña).
3. Dashboard → Finanzas → "Atrás" → Dashboard.
4. Tocar la pestaña ya seleccionada no hace nada ni duplica pantallas.
5. Cambiar entre pestañas, cerrar sesión y volver a entrar como Socio sigue funcionando; después de cerrar sesión, "Atrás" no regresa a una pantalla del Socio.
6. Cambiar el modo claro/oscuro con el drawer no rompe la pila ni la pestaña marcada.

---

## Ajuste B — Selector de rol por pestañas en el login

**Problema.** Hoy el login tiene tres botones ("Entrar como Socio / Cliente / Técnico"). Se quiere una barra de **pestañas** en la parte superior, al estilo de las pestañas "Iniciar sesión / Registrarte" que había antes, pero para elegir el rol.

**Diseño requerido (`fragment_login.xml` y `LoginFragment.java`).**
- Debajo del logo, un `TabLayout` de Material (modo `fixed`, `tabGravity="fill"`) con **tres pestañas**: **Socio**, **Cliente**, **Técnico**. Colores y estilos del tema, sin colores fijos.
- Debajo, el título "Iniciar sesión", el campo de correo y el de contraseña (se conservan tal como están).
- **Un solo botón** "Iniciar sesión" que reemplaza a los tres botones actuales. Elimina `btnLoginSocio`, `btnLoginCliente` y `btnLoginTecnico` y los textos que queden sin uso.
- El enlace **"¿No tienes cuenta? Regístrate aquí" aparece solo en la pestaña Cliente**; en Socio y Técnico se oculta (`GONE`). Los Socios y Técnicos no se registran solos.
- La pestaña seleccionada es el **rol requerido**: la validación no cambia. Si el correo y la contraseña son correctos pero la cuenta tiene otro rol, se rechaza con el mensaje ya existente ("Esta cuenta no tiene el rol de …"), usando el rol de la pestaña activa.
- **Pestaña por defecto: Cliente.** Debe conservarse al rotar la pantalla, al abrir y cerrar el teclado, y al volver desde el registro (el registro solo se alcanza desde Cliente, así que al regresar debe estar seleccionada Cliente). Guarda la pestaña elegida en un `ViewModel` con ámbito de Activity o en el estado guardado.
- Al cambiar de pestaña **no se borran** los textos escritos de correo y contraseña, pero sí se limpian los errores visibles.
- La acción "Listo" del teclado en el campo de contraseña dispara el inicio de sesión.
- Respeta `dimens.xml` y los textos van en `strings.xml` (español).

**Criterios de aceptación.**
1. El login muestra las pestañas Socio / Cliente / Técnico bajo el logo y un único botón "Iniciar sesión".
2. El enlace de registro se ve solo en Cliente.
3. `socio@sonidosonic.com` / `Socio#2026` entra con la pestaña Socio; con la pestaña Cliente o Técnico es rechazado indicando el rol.
4. Un Cliente registrado entra desde la pestaña Cliente y es rechazado desde Socio o Técnico.
5. Rotar el teléfono conserva la pestaña seleccionada y lo escrito. Ir a Registro y regresar deja la pestaña Cliente seleccionada.
6. Los modos claro y oscuro se ven bien y no hay colores fijos nuevos.

**Al terminar:** ejecuta `./gradlew assembleDebug`, corrige errores y entrégame los archivos tocados, cómo probar cada criterio y cualquier decisión que hayas tomado.
