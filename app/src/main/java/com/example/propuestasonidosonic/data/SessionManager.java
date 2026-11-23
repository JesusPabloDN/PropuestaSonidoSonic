package com.example.propuestasonidosonic.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.propuestasonidosonic.model.Usuario;

/**
 * Guarda la sesión del usuario en memoria y en SharedPreferences, para que siga
 * iniciada aunque se cierre la aplicación. Solo se borra al cerrar sesión desde el
 * menú lateral o cuando la cuenta fue deshabilitada o eliminada.
 * Nunca se guarda la contraseña.
 */
public class SessionManager {
    public static final String PREFS_SESION = "sonido_sonic_sesion";
    private static final String KEY_ID = "usuario_id";
    private static final String KEY_NOMBRE = "usuario_nombre";
    private static final String KEY_CORREO = "usuario_correo";
    private static final String KEY_TELEFONO = "usuario_telefono";
    private static final String KEY_ROL = "usuario_rol";
    private static final String KEY_CUENTA_ACTIVA = "usuario_cuenta_activa";
    private static final String KEY_FECHA_ALTA = "usuario_fecha_alta";
    private static final String KEY_CORREO_DESHABILITADO = "correo_deshabilitado";

    private static SessionManager instance;
    private final MutableLiveData<Usuario> usuarioActualLiveData = new MutableLiveData<>(null);
    private SharedPreferences prefs;

    public SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public static synchronized SessionManager getInstancia(android.content.Context context) {
        SessionManager sm = getInstance();
        if (context != null && sm.prefs == null) sm.inicializar(context);
        return sm;
    }

    /** Se llama una vez al iniciar la app para recuperar la sesión guardada. */
    public void inicializar(Context context) {
        if (prefs != null) return;
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_SESION, Context.MODE_PRIVATE);
        Usuario guardado = leerSesionGuardada();
        if (guardado != null) {
            usuarioActualLiveData.setValue(guardado);
        }
    }

    public String getRol() {
        Usuario u = getUsuarioActual();
        return u != null ? u.getRol() : null;
    }

    public void iniciarSesion(Usuario usuario) {
        usuarioActualLiveData.setValue(usuario);
        guardarSesion(usuario);
        if (prefs != null) prefs.edit().remove(KEY_CORREO_DESHABILITADO).apply();
    }

    /** Cierre de sesión pedido por el usuario desde el botón del menú lateral. */
    public void cerrarSesion() {
        borrarSesionGuardada();
        usuarioActualLiveData.setValue(null);
    }

    /**
     * Cierre de sesión automático porque la cuenta fue desactivada o eliminada.
     * Se recuerda el correo para avisarle en el login cuando intente entrar de nuevo.
     */
    public void cerrarSesionPorCuentaDeshabilitada() {
        Usuario actual = usuarioActualLiveData.getValue();
        borrarSesionGuardada();
        if (prefs != null && actual != null && actual.getCorreo() != null) {
            prefs.edit().putString(KEY_CORREO_DESHABILITADO, actual.getCorreo().trim().toLowerCase()).apply();
        }
        usuarioActualLiveData.setValue(null);
    }

    /** Indica si este correo pertenece a una sesión que se cerró porque la cuenta fue deshabilitada. */
    public boolean esCorreoDeshabilitado(String correo) {
        if (prefs == null || correo == null) return false;
        String guardado = prefs.getString(KEY_CORREO_DESHABILITADO, null);
        return guardado != null && guardado.equals(correo.trim().toLowerCase());
    }

    public void refrescarUsuario(Usuario usuario) {
        if (usuario != null) {
            Usuario actual = usuarioActualLiveData.getValue();
            if (actual != null && actual.getId().equals(usuario.getId())) {
                if (mismosDatos(actual, usuario)) return;
                Usuario copia = new Usuario(usuario);
                usuarioActualLiveData.setValue(copia);
                guardarSesion(copia);
            }
        }
    }

    public Usuario getUsuarioActual() {
        return usuarioActualLiveData.getValue();
    }

    public boolean haySesion() {
        return usuarioActualLiveData.getValue() != null;
    }

    public LiveData<Usuario> getUsuarioActualLiveData() {
        return usuarioActualLiveData;
    }

    private static boolean mismosDatos(Usuario a, Usuario b) {
        return java.util.Objects.equals(a.getNombre(), b.getNombre())
                && java.util.Objects.equals(a.getCorreo(), b.getCorreo())
                && java.util.Objects.equals(a.getTelefono(), b.getTelefono())
                && java.util.Objects.equals(a.getRol(), b.getRol())
                && a.isCuentaActiva() == b.isCuentaActiva()
                && a.getFechaAlta() == b.getFechaAlta();
    }

    private void guardarSesion(Usuario usuario) {
        if (prefs == null || usuario == null || usuario.getId() == null || usuario.getRol() == null) return;
        prefs.edit()
                .putString(KEY_ID, usuario.getId())
                .putString(KEY_NOMBRE, usuario.getNombre())
                .putString(KEY_CORREO, usuario.getCorreo())
                .putString(KEY_TELEFONO, usuario.getTelefono())
                .putString(KEY_ROL, usuario.getRol())
                .putBoolean(KEY_CUENTA_ACTIVA, usuario.isCuentaActiva())
                .putLong(KEY_FECHA_ALTA, usuario.getFechaAlta())
                .apply();
    }

    private Usuario leerSesionGuardada() {
        String id = prefs.getString(KEY_ID, null);
        String rol = prefs.getString(KEY_ROL, null);
        if (id == null || rol == null) return null;
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre(prefs.getString(KEY_NOMBRE, ""));
        u.setCorreo(prefs.getString(KEY_CORREO, ""));
        u.setTelefono(prefs.getString(KEY_TELEFONO, ""));
        u.setRol(rol);
        u.setCuentaActiva(prefs.getBoolean(KEY_CUENTA_ACTIVA, true));
        u.setFechaAlta(prefs.getLong(KEY_FECHA_ALTA, 0L));
        return u;
    }

    private void borrarSesionGuardada() {
        if (prefs == null) return;
        prefs.edit()
                .remove(KEY_ID).remove(KEY_NOMBRE).remove(KEY_CORREO).remove(KEY_TELEFONO)
                .remove(KEY_ROL).remove(KEY_CUENTA_ACTIVA).remove(KEY_FECHA_ALTA)
                .apply();
    }
}
