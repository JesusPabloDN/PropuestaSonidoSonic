package com.example.propuestasonidosonic.data.memory;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.SessionManager;
import com.example.propuestasonidosonic.data.UsuarioRepository;
import com.example.propuestasonidosonic.model.Usuario;
import com.example.propuestasonidosonic.util.Config;
import com.example.propuestasonidosonic.util.PasswordValidator;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class InMemoryUsuarioRepository implements UsuarioRepository {
    public static final String COLECCION_USUARIOS = "usuarios";
    private static final String SEED_SOCIO_ID = "socio-seed-0001";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final List<Usuario> usuarios = new ArrayList<>();
    private final MutableLiveData<List<Usuario>> usuariosLiveData = new MutableLiveData<>(new ArrayList<>());
    private SessionManager sessionManager;
    private FirebaseFirestore firestore;
    private final Usuario seedSocio;

    public InMemoryUsuarioRepository() {
        this(SessionManager.getInstance());
    }

    public InMemoryUsuarioRepository(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
        // Cuenta semilla para el Socio (fijada para pruebas y arranque inicial)
        seedSocio = new Usuario();
        seedSocio.setId(SEED_SOCIO_ID);
        seedSocio.setNombre(Config.CUENTA_SOCIO_NOMBRE);
        seedSocio.setCorreo(Config.CUENTA_SOCIO_CORREO);
        seedSocio.setTelefono(Config.CUENTA_SOCIO_TELEFONO);
        seedSocio.setRol(Usuario.ROL_SOCIO);
        seedSocio.setContrasena(Config.CUENTA_SOCIO_CONTRASENA);
        seedSocio.setCuentaActiva(true);
        seedSocio.setFechaAlta(System.currentTimeMillis());
        usuarios.add(seedSocio);
        notificarCambios();
    }

    public synchronized void conectarFirestore(FirebaseFirestore db) {
        if (db == null || this.firestore != null) return;
        this.firestore = db;
        this.firestore.collection(COLECCION_USUARIOS)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        return;
                    }
                    synchronized (InMemoryUsuarioRepository.this) {
                        List<Usuario> remotos = new ArrayList<>();
                        boolean existeSocioSeed = false;
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Usuario u = doc.toObject(Usuario.class);
                            if (u != null) {
                                if (u.getId() == null || u.getId().trim().isEmpty()) {
                                    u.setId(doc.getId());
                                }
                                if (Config.CUENTA_SOCIO_CORREO.equalsIgnoreCase(u.getCorreo())
                                        || SEED_SOCIO_ID.equals(u.getId())) {
                                    existeSocioSeed = true;
                                }
                                remotos.add(u);
                            }
                        }

                        if (!existeSocioSeed && remotos.isEmpty()) {
                            guardarEnFirestore(seedSocio);
                            remotos.add(new Usuario(seedSocio));
                        }

                        usuarios.clear();
                        usuarios.addAll(remotos);
                        notificarCambios();

                        if (sessionManager != null && sessionManager.getUsuarioActual() != null) {
                            String actualId = sessionManager.getUsuarioActual().getId();
                            for (Usuario u : usuarios) {
                                if (u.getId().equals(actualId)) {
                                    sessionManager.refrescarUsuario(u);
                                    break;
                                }
                            }
                        }
                    }
                });
    }

    private void guardarEnFirestore(Usuario usuario) {
        if (firestore != null && usuario != null && usuario.getId() != null) {
            firestore.collection(COLECCION_USUARIOS)
                    .document(usuario.getId())
                    .set(new Usuario(usuario));
        }
    }

    private void eliminarEnFirestore(String id) {
        if (firestore != null && id != null) {
            firestore.collection(COLECCION_USUARIOS)
                    .document(id)
                    .delete();
        }
    }

    public void setSessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    private void notificarCambios() {
        List<Usuario> copia = new ArrayList<>();
        for (Usuario u : usuarios) {
            copia.add(new Usuario(u));
        }
        usuariosLiveData.setValue(copia);
    }

    @Override
    public LiveData<List<Usuario>> getUsuarios() {
        return usuariosLiveData;
    }

    @Override
    public synchronized Usuario getUsuarioPorId(String id) {
        if (id == null) return null;
        for (Usuario u : usuarios) {
            if (id.equals(u.getId())) {
                return new Usuario(u);
            }
        }
        return null;
    }

    @Override
    public synchronized void getUsuarioPorId(String id, Callback<Usuario> callback) {
        if (id != null) {
            for (Usuario u : usuarios) {
                if (u.getId().equals(id)) {
                    if (callback != null) callback.onSuccess(new Usuario(u));
                    return;
                }
            }
        }
        if (callback != null) callback.onError("Usuario no encontrado");
    }

    @Override
    public synchronized void getUsuarioPorCorreo(String correo, Callback<Usuario> callback) {
        if (correo == null) {
            if (callback != null) callback.onError("Correo nulo");
            return;
        }
        for (Usuario u : usuarios) {
            if (correo.equalsIgnoreCase(u.getCorreo())) {
                if (callback != null) callback.onSuccess(new Usuario(u));
                return;
            }
        }
        if (callback != null) callback.onError("Usuario no encontrado");
    }

    @Override
    public synchronized void registrarUsuario(Usuario usuario, Callback<Usuario> callback) {
        if (usuario == null) {
            if (callback != null) callback.onError("Usuario nulo");
            return;
        }
        if (usuario.getCorreo() != null) {
            for (Usuario u : usuarios) {
                if (usuario.getCorreo().equalsIgnoreCase(u.getCorreo())) {
                    if (callback != null) callback.onError("El correo ya está registrado");
                    return;
                }
            }
        }
        usuarios.add(usuario);
        guardarEnFirestore(usuario);
        notificarCambios();
        if (callback != null) callback.onSuccess(new Usuario(usuario));
    }

    @Override
    public synchronized void actualizarUsuario(Usuario usuario, Callback<Usuario> callback) {
        if (usuario == null) {
            if (callback != null) callback.onError("Usuario nulo");
            return;
        }
        for (int i = 0; i < usuarios.size(); i++) {
            if (usuarios.get(i).getId().equals(usuario.getId())) {
                usuarios.set(i, usuario);
                guardarEnFirestore(usuario);
                notificarCambios();
                if (callback != null) callback.onSuccess(new Usuario(usuario));
                return;
            }
        }
        if (callback != null) callback.onError("Usuario no encontrado para actualizar");
    }

    @Override
    public synchronized void eliminarUsuario(String id, Callback<Void> callback) {
        boolean removed = usuarios.removeIf(u -> u.getId().equals(id));
        if (removed) {
            eliminarEnFirestore(id);
            notificarCambios();
            if (callback != null) callback.onSuccess(null);
        } else {
            if (callback != null) callback.onError("Usuario no encontrado");
        }
    }

    @Override
    public synchronized void autenticar(String correo, String contrasena, Callback<Usuario> callback) {
        if (correo == null || contrasena == null) {
            if (callback != null) callback.onError("Credenciales vacías");
            return;
        }
        for (Usuario u : usuarios) {
            if (correo.trim().equalsIgnoreCase(u.getCorreo()) && contrasena.equals(u.getContrasena())) {
                if (!u.isCuentaActiva()) {
                    if (callback != null) callback.onError("Esta cuenta se encuentra desactivada");
                    return;
                }
                if (callback != null) callback.onSuccess(new Usuario(u));
                return;
            }
        }
        if (callback != null) callback.onError("Correo o contraseña incorrectos");
    }

    @Override
    public synchronized List<Usuario> obtenerActivosPorRol(String rol) {
        List<Usuario> resultado = new ArrayList<>();
        if (rol == null) return resultado;
        for (Usuario u : usuarios) {
            if (rol.equalsIgnoreCase(u.getRol()) && u.isCuentaActiva()) {
                resultado.add(new Usuario(u));
            }
        }
        return resultado;
    }

    @Override
    public synchronized void crearPersonal(Usuario nuevo, String contrasena, Callback<Usuario> callback) {
        if (nuevo == null) {
            if (callback != null) callback.onError("Usuario nulo");
            return;
        }

        // 1. Rol debe ser Socio o Asistente (nunca Cliente)
        if (!Usuario.ROL_SOCIO.equals(nuevo.getRol()) && !Usuario.ROL_ASISTENTE.equals(nuevo.getRol())) {
            if (callback != null) callback.onError("El rol debe ser Socio o Asistente");
            return;
        }

        // 2. Nombre completo obligatorio (máximo 80 caracteres)
        if (nuevo.getNombre() == null || nuevo.getNombre().trim().isEmpty()) {
            if (callback != null) callback.onError("El nombre completo es obligatorio");
            return;
        }
        if (nuevo.getNombre().trim().length() > 80) {
            if (callback != null) callback.onError("El nombre no puede exceder 80 caracteres");
            return;
        }

        // 3. Correo con formato válido y único (sin distinguir mayúsculas)
        String correo = nuevo.getCorreo() != null ? nuevo.getCorreo().trim() : "";
        if (correo.isEmpty() || !EMAIL_PATTERN.matcher(correo).matches()) {
            if (callback != null) callback.onError("Correo electrónico inválido");
            return;
        }
        for (Usuario u : usuarios) {
            if (correo.equalsIgnoreCase(u.getCorreo())) {
                if (callback != null) callback.onError("El correo ya está registrado");
                return;
            }
        }

        // 4. Teléfono con al menos 10 dígitos
        String tel = nuevo.getTelefono() != null ? nuevo.getTelefono().trim() : "";
        String digitos = tel.replaceAll("\\D", "");
        if (digitos.length() < 10) {
            if (callback != null) callback.onError("El teléfono debe tener al menos 10 dígitos");
            return;
        }

        // 5. Contraseña válida según PasswordValidator
        if (contrasena == null || !PasswordValidator.isValid(contrasena)) {
            if (callback != null) callback.onError("La contraseña no cumple con los requisitos de seguridad");
            return;
        }

        // 6. La cuenta se crea activa
        nuevo.setNombre(nuevo.getNombre().trim());
        nuevo.setCorreo(correo.toLowerCase(Locale.ROOT));
        nuevo.setTelefono(tel);
        nuevo.setContrasena(contrasena);
        nuevo.setCuentaActiva(true);
        nuevo.setFechaAlta(System.currentTimeMillis());

        usuarios.add(nuevo);
        guardarEnFirestore(nuevo);
        notificarCambios();
        if (callback != null) {
            callback.onSuccess(new Usuario(nuevo));
        }
    }

    @Override
    public synchronized void actualizarPersonal(String actorId, String usuarioId, String nombre,
                                                String telefono, Boolean cuentaActiva,
                                                String nuevaContrasena, Callback<Usuario> callback) {
        Usuario target = null;
        for (Usuario u : usuarios) {
            if (u.getId().equals(usuarioId)) {
                target = u;
                break;
            }
        }

        if (target == null) {
            if (callback != null) callback.onError("Usuario no encontrado");
            return;
        }

        if (!Usuario.ROL_SOCIO.equals(target.getRol()) && !Usuario.ROL_ASISTENTE.equals(target.getRol())) {
            if (callback != null) callback.onError("Solo se puede editar a Socios o Asistentes");
            return;
        }

        // Validar y actualizar nombre si llega
        if (nombre != null && !nombre.trim().isEmpty()) {
            if (nombre.trim().length() > 80) {
                if (callback != null) callback.onError("El nombre no puede exceder 80 caracteres");
                return;
            }
            target.setNombre(nombre.trim());
        }

        // Validar y actualizar teléfono si llega
        if (telefono != null && !telefono.trim().isEmpty()) {
            String digitos = telefono.replaceAll("\\D", "");
            if (digitos.length() < 10) {
                if (callback != null) callback.onError("El teléfono debe tener al menos 10 dígitos");
                return;
            }
            target.setTelefono(telefono.trim());
        }

        // Validar y actualizar contraseña si llega
        if (nuevaContrasena != null && !nuevaContrasena.trim().isEmpty()) {
            if (!PasswordValidator.isValid(nuevaContrasena)) {
                if (callback != null) callback.onError("La contraseña no cumple con los requisitos de seguridad");
                return;
            }
            target.setContrasena(nuevaContrasena);
        }

        // Validar y actualizar estado si llega
        if (cuentaActiva != null) {
            if (!cuentaActiva) {
                // Nadie puede desactivarse a sí mismo
                if (actorId != null && actorId.equals(usuarioId)) {
                    if (callback != null) callback.onError("No puedes desactivar tu propia cuenta");
                    return;
                }
                // No se puede desactivar al último Socio activo
                if (Usuario.ROL_SOCIO.equals(target.getRol())) {
                    int sociosActivos = 0;
                    for (Usuario u : usuarios) {
                        if (Usuario.ROL_SOCIO.equals(u.getRol()) && u.isCuentaActiva()) {
                            sociosActivos++;
                        }
                    }
                    if (sociosActivos <= 1) {
                        if (callback != null) callback.onError("Debe quedar al menos un Socio activo");
                        return;
                    }
                }
                target.setCuentaActiva(false);
            } else {
                target.setCuentaActiva(true);
            }
        }

        guardarEnFirestore(target);

        // Si el usuario editado es el de la sesión actual, refrescar SessionManager
        if (sessionManager != null) {
            sessionManager.refrescarUsuario(target);
        }

        notificarCambios();
        if (callback != null) {
            callback.onSuccess(new Usuario(target));
        }
    }

    @Override
    public synchronized void actualizarPerfil(String usuarioId, String nombre, String telefono, Callback<Usuario> callback) {
        Usuario target = null;
        for (Usuario u : usuarios) {
            if (u.getId().equals(usuarioId)) {
                target = u;
                break;
            }
        }
        if (target == null) {
            if (callback != null) callback.onError("Usuario no encontrado");
            return;
        }

        if (nombre == null || nombre.trim().isEmpty()) {
            if (callback != null) callback.onError("El nombre es obligatorio");
            return;
        }
        if (nombre.trim().length() > 80) {
            if (callback != null) callback.onError("El nombre no puede exceder 80 caracteres");
            return;
        }

        if (telefono == null) {
            if (callback != null) callback.onError("El teléfono es obligatorio");
            return;
        }
        String digitos = telefono.trim().replaceAll("\\D", "");
        if (digitos.length() < 10) {
            if (callback != null) callback.onError("El teléfono debe tener al menos 10 dígitos");
            return;
        }

        target.setNombre(nombre.trim());
        target.setTelefono(telefono.trim());

        guardarEnFirestore(target);

        if (sessionManager != null) {
            sessionManager.refrescarUsuario(target);
        }
        notificarCambios();
        if (callback != null) {
            callback.onSuccess(new Usuario(target));
        }
    }

    @Override
    public synchronized void cambiarContrasena(String usuarioId, String contrasenaActual, String contrasenaNueva, Callback<Void> callback) {
        Usuario target = null;
        for (Usuario u : usuarios) {
            if (u.getId().equals(usuarioId)) {
                target = u;
                break;
            }
        }
        if (target == null) {
            if (callback != null) callback.onError("Usuario no encontrado");
            return;
        }

        if (contrasenaActual == null || !contrasenaActual.equals(target.getContrasena())) {
            if (callback != null) callback.onError("La contraseña actual es incorrecta");
            return;
        }

        if (contrasenaNueva == null || !PasswordValidator.isValid(contrasenaNueva)) {
            if (callback != null) callback.onError("La nueva contraseña no cumple con los requisitos de seguridad");
            return;
        }

        if (contrasenaNueva.equals(contrasenaActual)) {
            if (callback != null) callback.onError("La nueva contraseña no puede ser igual a la actual");
            return;
        }

        target.setContrasena(contrasenaNueva);

        guardarEnFirestore(target);

        if (sessionManager != null) {
            sessionManager.refrescarUsuario(target);
        }
        notificarCambios();
        if (callback != null) {
            callback.onSuccess(null);
        }
    }
}
