# ETAPA 3 — Paquetes (Socio): crear, listar, editar y eliminar paquetes dentro de Inventario

> Pega primero el **CONTEXTO GLOBAL** (archivo `Prompts_Antigravity_Socio_SonidoSonic.md`) y la **ADENDA v3** (al inicio de `Prompt_Etapa_1.2_Renombrar_Tecnico_a_Asistente.md`). Luego pega este texto.
> Requisito: las Etapas 1, 1.1, 1.2 y 2 ya están terminadas.

---

## OBJETIVO

El negocio no vende piezas sueltas: vende **paquetes**. En esta etapa el Socio podrá armar paquetes a partir del equipo que ya existe en Inventario. Un paquete es una lista de **modelos con cantidades** (por ejemplo 2 × Bocina JBL 15", 1 × Mezcladora, 2 × Micrófono) más un precio y datos operativos que la futura lógica de reservas va a usar (duración base, personal requerido, tiempos de montaje y desmontaje).

**Lo que NO se hace en esta etapa:** reservas, horarios, disponibilidad, cambios a `Pedido`/`Evento`, fotos propias del paquete, vista del Cliente. No toques nada de Pedidos, Agenda, Finanzas ni Dashboard.

## PASO 0 — Revisión previa (no edites todavía; entrégame un plan breve con estos hallazgos)

Lee estos archivos y respóndeme cada punto:

1. `model/Paquete.java` y `model/Partida.java`: qué campos tienen hoy y si `Partida` se usa en algún lugar (búsqueda de referencias).
2. `model/Equipo.java`, `ui/socio/EquipoFormBottomSheet.java` y `data/memory/InMemoryEquipoRepository.java`: **cómo se nombran las unidades cuando se crea cantidad N** (por ejemplo `Bocina JBL 15" (1)`, `(2)`…, y qué pasa cuando la cantidad es 1), cómo se llaman el campo de precio y el de estado físico, y qué valor tiene "Disponible".
3. `adapter/EquipoAdapter.java`: dónde está el mapeo categoría → imagen por defecto (`bocinajbl`, `subwoofer`, `microfonosm`, `mezcladora`, `controlador`, `lucesled`, `ic_equipo_generico`) y cómo se pinta la insignia de estado con `bg_badge_rounded` y los colores `status_*`.
4. `data/PaqueteRepository.java`, `data/memory/InMemoryPaqueteRepository.java`, `data/EquipoRepository.java` y `data/AppContainer.java`: firmas actuales y cómo se construyen.
5. `util/Config.java`: constantes existentes.
6. La **convención de mensajes de error** que usan los repositorios en memoria (¿cadenas en español dentro del repositorio o códigos?). **Sigue exactamente esa misma convención** en lo nuevo.
7. Cómo `InventarioFragment` observa la lista de equipos (nombre del `LiveData`) y dónde guarda el estado de la búsqueda.

## PASO 1 — Modelo

### 1.1 `util/ModeloUtils.java` (nuevo)

Clase final con métodos estáticos:

- `String nombreBase(String nombreUnidad)`: quita al final un sufijo de numeración con el formato que hayas encontrado en el Paso 0 (por defecto la expresión regular `\s*\(\d+\)\s*$`) y aplica `trim()`.
- `String clave(String categoria, String nombreUnidad)`: devuelve `categoria + "|" + nombreBase(nombreUnidad).toLowerCase(Locale.ROOT)`.

Un **modelo** es el conjunto de unidades que comparten la misma clave. No se agrega ningún campo nuevo a `Equipo`.

### 1.2 `model/ModeloInventario.java` (nuevo, POJO solo de lectura)

Constructor público sin argumentos, getters y setters. Campos:

- `String clave`
- `String nombreModelo` (nombre base de la primera unidad registrada, tal como se escribió)
- `String categoria`
- `int totalUnidades` (todas las unidades del modelo, sin importar su estado físico)
- `int unidadesDisponibles` (las que tienen estado físico Disponible)
- `double precioUnitario` (precio de renta por evento de la primera unidad registrada del modelo)

### 1.3 `model/ComponentePaquete.java` (nuevo)

POJO con constructor público sin argumentos, getters y setters, más un constructor de conveniencia. Campos:

- `String modeloClave`
- `String nombreModelo`
- `String categoria`
- `int cantidad`

### 1.4 `model/Paquete.java` (concilia con lo que ya existe)

Estos son los campos objetivo. Si ya existe uno equivalente con otro nombre, **conserva el nombre existente** y repórtalo en la entrega; si `Partida` no se usa en ningún lado, déjala como está y avísame, no la borres.

- `String id` (UUID como String)
- `String nombre`
- `String descripcion` (puede ser vacía)
- `List<ComponentePaquete> componentes`
- `double precio` (MXN, precio del paquete por evento)
- `int duracionBaseHoras` (horas incluidas en el precio)
- `double precioHoraExtra` (0 si no cobran horas extra)
- `int operadoresRequeridos` (1 o 2)
- `int bufferMontajeMin` (minutos de montaje antes del evento)
- `int bufferDesmontajeMin` (minutos de desmontaje y regreso después del evento)
- `boolean activo`
- `String fotoUri` (queda en el modelo para una etapa futura; **no lo uses en la UI ahora**)
- `long fechaCreacion` (milisegundos)

Constructor público sin argumentos, getters y setters. Usa constantes tipo `String` o `int`, nunca enums.

### 1.5 `util/Config.java`: agrega estas constantes con un comentario `// valor provisional, validar con los socios`

- `DURACION_BASE_HORAS_DEFAULT = 4`
- `BUFFER_MONTAJE_MIN_DEFAULT = 90`
- `BUFFER_DESMONTAJE_MIN_DEFAULT = 120`
- `OPERADORES_POR_PAQUETE_MAX = 2`
- `MAX_EVENTOS_SIMULTANEOS = 3` (se usará en una etapa posterior; solo declárala)

## PASO 2 — Repositorios (en memoria, listos para Firebase)

### 2.1 `EquipoRepository` + `InMemoryEquipoRepository`

Agrega dos métodos **síncronos** (lectura de una instantánea):

- `List<ModeloInventario> obtenerModelos()`: agrupa todas las unidades por `ModeloUtils.clave(...)` y devuelve la lista ordenada por categoría y luego por nombre (sin distinguir mayúsculas).
- `ModeloInventario obtenerModelo(String clave)`: devuelve `null` si no existe.

Ambos dentro de bloques `synchronized` con el mismo candado que ya usa el repositorio. Si el repositorio ya expone sus unidades de otra forma, reutiliza esa estructura, no dupliques datos.

### 2.2 `PaqueteRepository` + `InMemoryPaqueteRepository`

Interfaz (conserva lo que ya exista y se use; agrega lo que falte):

- `LiveData<List<Paquete>> getPaquetes()`: lista ordenada por nombre sin distinguir mayúsculas. Cada emisión es una **lista nueva** con **copias** de los objetos, para que la UI no modifique los datos del repositorio por accidente.
- `Paquete getPaqueteById(String id)`: devuelve una copia, o `null`.
- `void guardarPaquete(Paquete paquete, Callback<Paquete> callback)`: crea si `id` es nulo o vacío (genera UUID, `fechaCreacion = ahora`) y actualiza si existe.
- `void eliminarPaquete(String id, Callback<Void> callback)`: quita el paquete. Deja el comentario `// TODO etapa de Reservas: impedir eliminar si hay reservas futuras que usan este paquete`.

Validaciones dentro de `guardarPaquete`, en un bloque `synchronized` con el comentario `// FIRESTORE: transacción`. Si una falla, llama a `onError` con el mensaje (misma convención del Paso 0) y **no guarda nada**:

1. `nombre` obligatorio, sin espacios sobrantes, máximo 60 caracteres, **único sin distinguir mayúsculas** entre los demás paquetes (al editar se excluye el propio).
2. `componentes` con al menos 1 elemento.
3. Cada componente con `cantidad >= 1`, sin dos componentes con la misma `modeloClave`.
4. Cada `modeloClave` debe existir hoy en el inventario (`obtenerModelo`). Si no: "El modelo X ya no existe en el inventario".
5. Cada `cantidad` ≤ `totalUnidades` de su modelo. Si no: "Solo hay N unidades de X".
6. `precio > 0`.
7. `duracionBaseHoras` entre 1 y 24.
8. `precioHoraExtra >= 0`.
9. `operadoresRequeridos` es 1 o 2.
10. `bufferMontajeMin` y `bufferDesmontajeMin` entre 0 y 480.

Al guardar, vuelve a leer `nombre` y `descripcion` con `trim()` y normaliza los datos de cada componente (`nombreModelo` y `categoria` se rellenan desde el inventario, no se confía en lo que llegue de la UI).

### 2.3 `AppContainer`

El `InMemoryPaqueteRepository` necesita el `EquipoRepository`: pásalo por el constructor y ajusta el orden de creación en `AppContainer`.

**No agregues datos de ejemplo.** Las listas inician vacías.

## PASO 3 — Textos (`strings.xml`, español de México)

Agrega como mínimo (puedes añadir los que necesites, siempre en `strings.xml`):

- `tab_inventario_equipo` = "Equipo", `tab_inventario_paquetes` = "Paquetes"
- `empty_paquetes` = "Aún no hay paquetes. Toca + para crear el primero"
- `paquete_form_titulo_nuevo` = "Nuevo paquete", `paquete_form_titulo_editar` = "Editar paquete"
- `hint_nombre_paquete` = "Nombre del paquete", `hint_descripcion_paquete` = "Descripción (opcional)"
- `label_equipo_incluido` = "Equipo incluido", `btn_agregar_equipo` = "Agregar equipo"
- `empty_componentes` = "Aún no has agregado equipo a este paquete"
- `dialog_agregar_equipo_titulo` = "Elige un modelo", `dialog_sin_modelos` = "Primero registra equipo en Inventario"
- `componente_stock_formato` = "%1$s · Hay %2$d en inventario"
- `modelo_opcion_formato` = "%1$s · %2$s · Disponibles %3$d de %4$d"
- `hint_precio_paquete` = "Precio del paquete por evento ($)"
- `precio_sugerido_formato` = "Suma de las piezas: %1$s", `btn_usar_precio_sugerido` = "Usar este precio"
- `hint_duracion_base` = "Horas incluidas (1 a 24)", `hint_precio_hora_extra` = "Precio por hora extra ($, 0 si no cobran)"
- `label_operadores_requeridos` = "Personal requerido", `operadores_uno` = "1 persona", `operadores_dos` = "2 personas"
- `hint_buffer_montaje` = "Montaje antes del evento (min)", `hint_buffer_desmontaje` = "Desmontaje y regreso después (min)"
- `label_paquete_activo` = "Paquete activo (visible para clientes)"
- `paquete_resumen_formato` = "%1$d piezas · %2$d tipos de equipo", `paquete_precio_formato` = "%1$s por evento"
- `paquete_detalle_formato` = "%1$d h incluidas · %2$s", y `operadores_n_formato` = "%1$d persona(s)"
- `badge_paquete_activo` = "Activo", `badge_paquete_inactivo` = "Inactivo", `badge_stock_insuficiente` = "Stock insuficiente"
- `dialog_eliminar_paquete_titulo` = "¿Eliminar paquete?", `dialog_eliminar_paquete_mensaje` = "Esta acción no se puede deshacer."
- `msg_paquete_guardado` = "Paquete guardado", `msg_paquete_eliminado` = "Paquete eliminado"
- Errores de formulario: `error_nombre_paquete_requerido`, `error_sin_componentes`, `error_precio_paquete_invalido`, `error_duracion_invalida`, `error_precio_hora_extra_invalido`, `error_buffer_invalido`

## PASO 4 — Pantalla Inventario: pestañas Equipo | Paquetes

Modifica `fragment_inventario.xml` e `InventarioFragment.java` **sin romper nada de lo que ya funciona**:

1. En el `LinearLayout` vertical de la raíz, **antes** de `cardBarraBusqueda`, agrega un `com.google.android.material.tabs.TabLayout` con id `tabsInventario` (`tabMode="fixed"`, `tabGravity="fill"`) con dos pestañas: "Equipo" y "Paquetes".
2. Al `FrameLayout` que contiene `rvEquipos` y `layoutEmptyState`, ponle el id `frameListaEquipos`.
3. Agrega, como hermano de `frameListaEquipos`, otro `FrameLayout` con id `frameListaPaquetes` (alto 0dp con peso 1, `visibility="gone"`) que contenga:
   - `RecyclerView` con id `rvPaquetes` (`clipToPadding="false"`, `paddingBottom="96dp"`, `paddingTop="@dimen/spacing_tiny"`, `tools:listitem="@layout/item_paquete"`).
   - Un estado vacío `layoutEmptyPaquetes` copiando la estructura de `layoutEmptyState` (icono `ic_equipo_generico`, texto con id `tvEmptyPaquetes` y `@string/empty_paquetes`).
4. Comportamiento de las pestañas:
   - Pestaña **Equipo** (índice 0): se muestran `cardBarraBusqueda` y `frameListaEquipos`; se oculta `frameListaPaquetes`. Todo funciona exactamente como en la Etapa 2.
   - Pestaña **Paquetes** (índice 1): se oculta `cardBarraBusqueda` y `frameListaEquipos`; se muestra `frameListaPaquetes`.
5. El FAB `fabNuevoEquipo` **conserva su id** y es compartido. En Equipo abre el formulario de equipo de siempre (`EquipoFormBottomSheet`). En Paquetes navega al formulario de paquete en modo "nuevo". Actualiza su `contentDescription` según la pestaña (`dialog_nuevo_equipo_titulo` o `paquete_form_titulo_nuevo`).
6. **Estado:** guarda la pestaña seleccionada en un campo del fragmento y en `onSaveInstanceState`. Al volver del formulario de paquete con "Atrás", debe aparecer la pestaña **Paquetes**, no Equipo. Al rotar el teléfono, se conserva.
7. `InventarioFragment` observa el `LiveData` de paquetes y el de equipos. Cuando cualquiera de los dos cambie, vuelve a calcular el estado de stock de cada paquete y refresca el adaptador de paquetes.

## PASO 5 — Lista de paquetes

### 5.1 `res/layout/item_paquete.xml`

`MaterialCardView` con el mismo estilo que `item_equipo.xml` (márgenes, `card_corner_radius`, trazo `colorSurfaceVariant`). Dentro, en horizontal:

- `ShapeableImageView` `ivPaqueteImagen` de 72dp con `@style/RoundedImageView72`. Imagen = la imagen por defecto de la categoría del componente de **mayor precio unitario**; si por alguna razón no hay componentes, `ic_equipo_generico`. **Reutiliza** el mapeo que ya existe en `EquipoAdapter`; si es privado, extráelo a un método estático público (por ejemplo `CategoriaEquipo.imagenPorDefecto(String categoria)`) y haz que `EquipoAdapter` lo use también, sin cambiar su comportamiento.
- Columna con: `tvPaqueteNombre` (negrita), insignia `tvPaqueteEstado` ("Activo"/"Inactivo", mismo patrón de pintado que la insignia de `EquipoAdapter`; Activo con `status_disponible`/`status_disponible_container`, Inactivo con `colorOnSurfaceVariant` sobre `colorSurfaceVariant`), `tvPaqueteResumen` ("6 piezas · 3 tipos de equipo"), `tvPaquetePrecio` ("$3,500.00 por evento", usando `FormatUtils` es-MX), `tvPaqueteDetalle` ("4 h incluidas · 2 persona(s)"), `tvPaqueteComponentes` (resumen corto tipo "2× Bocina JBL 15", 1× Mezcladora…", máximo 2 líneas con puntos suspensivos) y `tvPaqueteAlerta` (oculta por defecto; insignia "Stock insuficiente" con `status_pendiente`/`status_pendiente_container`).
- Todo el texto de 14sp o más, sin colores fijos.

### 5.2 `adapter/PaqueteAdapter.java`

Mismo patrón que `EquipoAdapter` (si usa `ListAdapter` + `DiffUtil`, igual aquí). Interfaz `OnPaqueteClickListener { void onPaqueteClick(Paquete p); }`. Al tocar la tarjeta se abre el formulario en modo edición.

### 5.3 Regla de "Stock insuficiente"

Un paquete está **incompleto** si algún componente tiene `cantidad > totalUnidades` del modelo (según `obtenerModelos()`), o si su modelo ya no existe. En ese caso la tarjeta muestra la insignia y, en la línea de alerta, el primer modelo con problema. No se bloquea nada: solo se avisa.

## PASO 6 — Formulario de paquete (pantalla completa, no BottomSheet)

### 6.1 Navegación (`nav_graph.xml`)

- Dentro de `socio_graph`, agrega el destino `paqueteFormFragment` (clase `ui.socio.PaqueteFormFragment`, layout `fragment_paquete_form`, label `@string/paquete_form_titulo_nuevo`) con un argumento `paqueteId` de tipo `string`, `nullable`, valor por defecto `@null`.
- Dentro del destino `nav_inventario`, agrega la acción `action_inventario_to_paqueteForm` hacia `paqueteFormFragment`. **Sin Safe Args:** el argumento viaja en un `Bundle` con la clave `"paqueteId"`.
- **No cambies `MainActivity`**: este destino cae en la rama `else` actual (barra superior visible con flecha de regreso, barra inferior oculta). Verifica que sea así. "Atrás" desde el formulario regresa a Inventario.

### 6.2 Iconos nuevos (vectores simples, 24dp, `android:tint="?attr/colorControlNormal"`)

- `ic_add_24.xml`: `M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z`
- `ic_remove_24.xml`: `M19,13H5v-2h14v2z`
- `ic_delete_24.xml`: `M6,19c0,1.1 0.9,2 2,2h8c1.1,0 2,-0.9 2,-2V7H6v12zM19,4h-3.5l-1,-1h-5l-1,1H5v2h14V4z`

### 6.3 `res/layout/fragment_paquete_form.xml`

Un `NestedScrollView` con `fillViewport="true"` y un `LinearLayout` vertical con padding `spacing_medium`, en este orden:

1. `TextInputLayout` `tilNombrePaquete` / `etNombrePaquete` (`textCapWords`, máximo 60 caracteres, contador visible).
2. `TextInputLayout` `tilDescripcionPaquete` / `etDescripcionPaquete` (varias líneas, máximo 3 visibles).
3. Título de sección `label_equipo_incluido`.
4. `RecyclerView` `rvComponentes` con `nestedScrollingEnabled="false"` y altura `wrap_content`, más `tvEmptyComponentes` (visible cuando no hay componentes; texto `empty_componentes`).
5. `MaterialButton` `btnAgregarEquipo` (estilo Tonal, ícono `ic_add_24`).
6. `tvPrecioSugerido` + `MaterialButton` `btnUsarPrecioSugerido` (estilo Text).
7. `TextInputLayout` `tilPrecioPaquete` / `etPrecioPaquete` (`numberDecimal`, prefijo `$`).
8. `TextInputLayout` `tilDuracionBase` / `etDuracionBase` (número, por defecto `Config.DURACION_BASE_HORAS_DEFAULT`).
9. `TextInputLayout` `tilPrecioHoraExtra` / `etPrecioHoraExtra` (`numberDecimal`, prefijo `$`, por defecto `0`).
10. Etiqueta `label_operadores_requeridos` + `MaterialButtonToggleGroup` `toggleOperadores` (`singleSelection`, `selectionRequired`) con dos botones: `btnOperadores1` ("1 persona") y `btnOperadores2` ("2 personas"). Por defecto 1.
11. `TextInputLayout` `tilBufferMontaje` / `etBufferMontaje` (número, por defecto `Config.BUFFER_MONTAJE_MIN_DEFAULT`).
12. `TextInputLayout` `tilBufferDesmontaje` / `etBufferDesmontaje` (número, por defecto `Config.BUFFER_DESMONTAJE_MIN_DEFAULT`).
13. `MaterialSwitch` `swActivo` con `label_paquete_activo`, activado por defecto.
14. `MaterialButton` `btnGuardarPaquete` (alto `button_height`, `cornerRadius = card_corner_radius`).
15. `MaterialButton` `btnEliminarPaquete` (Outlined, texto y trazo con `@color/status_danado` igual que en `dialog_equipo_form.xml`; visible **solo** al editar).

Al final del scroll deja un espacio inferior de `spacing_xlarge`.

### 6.4 `res/layout/item_componente_paquete.xml`

Fila horizontal con altura mínima `touch_target_min`:

- A la izquierda, columna con `tvComponenteNombre` (negrita) y `tvComponenteStock` ("Bocinas · Hay 4 en inventario").
- A la derecha: `btnComponenteMenos` (`ic_remove_24`), `tvComponenteCantidad` (ancho mínimo 32dp, centrado), `btnComponenteMas` (`ic_add_24`) y `btnComponenteQuitar` (`ic_delete_24`). Cada botón de 48dp, con `contentDescription` en `strings.xml`.

### 6.5 `ui/socio/PaqueteFormViewModel.java` (ViewModel de `androidx.lifecycle`, con ámbito del fragmento)

Guarda `List<ComponentePaquete> componentes` y un indicador `inicializado`, para que la lista **sobreviva a la rotación**. Los textos de los campos se restauran solos por el sistema (cada campo tiene su id).

### 6.6 `ui/socio/PaqueteFormFragment.java`

- **Modo:** si el `Bundle` trae `paqueteId`, es edición; de lo contrario, es nuevo. Si en edición `getPaqueteById` devuelve `null`, muestra un Toast y haz `popBackStack()`.
- **Título de la barra superior:** `paquete_form_titulo_nuevo` o `paquete_form_titulo_editar`, fijado en `onResume` con `getSupportActionBar().setTitle(...)` para que no lo pise el label del grafo.
- **Carga inicial:** solo si el ViewModel no está `inicializado`, copia los datos del paquete (o los valores por defecto) a los campos y al ViewModel.
- **Agregar equipo (`btnAgregarEquipo`):** abre un `MaterialAlertDialogBuilder` con lista de selección (`setItems` con texto `modelo_opcion_formato`) de los modelos de `obtenerModelos()` **que aún no están en el paquete**. Si no hay modelos registrados, el diálogo muestra `dialog_sin_modelos`. Al elegir uno, se agrega con cantidad 1.
- **Cantidad:** los botones − y + cambian la cantidad entre 1 y `totalUnidades` del modelo (consulta `obtenerModelo(clave)`). Al llegar al máximo, el botón + queda deshabilitado. Quitar elimina el componente de la lista.
- **Precio sugerido:** cada vez que cambia la lista de componentes, recalcula Σ (`precioUnitario` × `cantidad`) y muestra `tvPrecioSugerido` con `FormatUtils`. `btnUsarPrecioSugerido` copia ese valor a `etPrecioPaquete`. **Nunca** sobrescribas el precio por tu cuenta.
- **Guardar (`btnGuardarPaquete`):**
  1. Limpia errores previos.
  2. Valida en la pantalla, con mensajes de `strings.xml` y el error debajo del campo correspondiente: nombre obligatorio, al menos un componente (mensaje bajo la lista), precio > 0, duración 1–24, precio por hora extra ≥ 0, montaje y desmontaje 0–480.
  3. Arma el `Paquete` (conserva `id` y `fechaCreacion` si es edición) y llama a `guardarPaquete`.
  4. `onSuccess`: Toast `msg_paquete_guardado`, oculta el teclado y `popBackStack()`.
  5. `onError(mensaje)`: muestra el mensaje en un `MaterialAlertDialog` (el nombre repetido puede ponerse también como error del campo nombre). **No cierres la pantalla ni borres lo capturado.**
  6. Deshabilita el botón mientras se procesa para evitar doble toque.
- **Eliminar (`btnEliminarPaquete`):** diálogo de confirmación; al aceptar llama a `eliminarPaquete`, Toast `msg_paquete_eliminado` y `popBackStack()`.
- Cierra el teclado al guardar, eliminar o salir.

## REGLAS DE CALIDAD (aplican a todo)

- Textos en `strings.xml`; dimensiones desde `dimens.xml`; nada de colores fijos, solo atributos del tema y los `status_*` ya existentes. Todo debe verse bien en claro y oscuro.
- Zonas táctiles de 48dp o más; texto de 14sp o más; `contentDescription` en iconos y botones sin texto.
- Las escrituras usan `Callback<T>`; las lecturas, `LiveData`. Sin Kotlin, sin librerías nuevas.
- No cambies el comportamiento de ninguna otra pantalla. Si necesitas tocar algo fuera de lo descrito, avísame antes en el plan.

## CRITERIOS DE ACEPTACIÓN (pruébalos en este orden)

Prepara en Inventario: Bocina JBL 15" × 4, Subwoofer 18" × 2, Micrófono SM58 × 3, Mezcladora Behringer × 1.

1. Inventario muestra las pestañas **Equipo | Paquetes**. En Equipo, la búsqueda, el filtro por categoría, crear, editar y eliminar funcionan igual que antes.
2. En Paquetes vacío aparece el mensaje de estado vacío y el FAB abre "Nuevo paquete".
3. Crea "Paquete Básico": 2 × Bocina, 1 × Mezcladora, 2 × Micrófono. La suma sugerida coincide con Σ precio × cantidad; "Usar este precio" la copia al campo; guarda. La tarjeta muestra "5 piezas · 3 tipos de equipo", su precio, "4 h incluidas · 1 persona(s)" y el resumen de componentes.
4. Crea otro paquete llamado "paquete básico" (con minúsculas): se rechaza por nombre repetido y se conserva lo capturado.
5. En el formulario, el botón + de la Bocina se detiene en 4. Guardar sin componentes muestra el error bajo la lista. Precio 0 o vacío muestra error. Duración 0 o 25 muestra error.
6. Abre "Paquete Básico", sube los micrófonos de 2 a 3, activa 2 personas y desactiva "Activo"; guarda; la tarjeta muestra "Inactivo" y "2 persona(s)".
7. En la pestaña Equipo, elimina 3 de las 4 Bocinas. Regresa a Paquetes: "Paquete Básico" muestra **Stock insuficiente** (necesita 2 y quedó 1).
8. Rota el teléfono en el formulario con campos escritos y componentes agregados: nada se pierde. Rota en la pestaña Paquetes: se conserva la pestaña.
9. Desde el formulario, "Atrás" vuelve a Inventario **en la pestaña Paquetes**; otro "Atrás" va a Dashboard; otro sale de la app.
10. Elimina el paquete desde el formulario con confirmación: desaparece y vuelve el estado vacío.
11. Modo oscuro: todo legible, sin colores fijos nuevos. Cerrar sesión y volver a entrar como Socio funciona.
12. `./gradlew assembleDebug` (en Windows: `.\gradlew.bat assembleDebug`) termina sin errores.

## AL TERMINAR

Entrégame: (a) hallazgos del Paso 0 (cómo se nombran las unidades, qué había en `Paquete` y `Partida`, convención de errores), (b) archivos creados y modificados, (c) decisiones fuera de lo descrito, (d) resultado del `assembleDebug`, y (e) cómo probaste cada criterio.
