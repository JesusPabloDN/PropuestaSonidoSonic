package com.example.propuestasonidosonic.data.memory;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.propuestasonidosonic.data.Callback;
import com.example.propuestasonidosonic.data.UsuarioRepository;
import com.example.propuestasonidosonic.model.Usuario;

import com.example.propuestasonidosonic.util.Config;

import java.util.ArrayList;
import java.util.List;

public class InMemoryUsuarioRepository implements UsuarioRepository {
    private final List<Usuario> usuarios = new ArrayList<>();
    private final MutableLiveData<List<Usuario>> usuariosLiveData = new MutableLiveData<>(new ArrayList<>());

    public InMemoryUsuarioRepository() {
        // Cuenta semilla para el Socio (fijada para pruebas)
        Usuario seedSocio = new Usuario();
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

    private void notificarCambios() {
        usuariosLiveData.setValue(new ArrayList<>(usuarios));
    }

    @Override
    public LiveData<List<Usuario>> getUsuarios() {
        return usuariosLiveData;
    }

    @Override
    public synchronized void getUsuarioPorId(String id, Callback<Usuario> callback) {
        for (Usuario u : usuarios) {
            if (u.getId().equals(id)) {
                callback.onSuccess(u);
                return;
            }
        }
        callback.onError("Usuario no encontrado");
    }

    @Override
    public synchronized void getUsuarioPorCorreo(String correo, Callback<Usuario> callback) {
        if (correo == null) {
            callback.onError("Correo nulo");
            return;
        }
        for (Usuario u : usuarios) {
            if (correo.equalsIgnoreCase(u.getCorreo())) {
                callback.onSuccess(u);
                return;
            }
        }
        callback.onError("Usuario no encontrado");
    }

    @Override
    public synchronized void registrarUsuario(Usuario usuario, Callback<Usuario> callback) {
        // FIRESTORE: aquí irá una transacción
        if (usuario.getCorreo() != null) {
            for (Usuario u : usuarios) {
                if (usuario.getCorreo().equalsIgnoreCase(u.getCorreo())) {
                    callback.onError("El correo ya está registrado");
                    return;
                }
            }
        }
        usuarios.add(usuario);
        notificarCambios();
        callback.onSuccess(usuario);
    }

    @Override
    public synchronized void actualizarUsuario(Usuario usuario, Callback<Usuario> callback) {
        // FIRESTORE: aquí irá una transacción
        for (int i = 0; i < usuarios.size(); i++) {
            if (usuarios.get(i).getId().equals(usuario.getId())) {
                usuarios.set(i, usuario);
                notificarCambios();
                callback.onSuccess(usuario);
                return;
            }
        }
        callback.onError("Usuario no encontrado para actualizar");
    }

    @Override
    public synchronized void eliminarUsuario(String id, Callback<Void> callback) {
        // FIRESTORE: aquí irá una transacción
        boolean removed = usuarios.removeIf(u -> u.getId().equals(id));
        if (removed) {
            notificarCambios();
            callback.onSuccess(null);
        } else {
            callback.onError("Usuario no encontrado");
        }
    }

    @Override
    public synchronized void autenticar(String correo, String contrasena, Callback<Usuario> callback) {
        if (correo == null || contrasena == null) {
            callback.onError("Credenciales vacías");
            return;
        }
        for (Usuario u : usuarios) {
            if (correo.equalsIgnoreCase(u.getCorreo()) && contrasena.equals(u.getContrasena())) {
                callback.onSuccess(u);
                return;
            }
        }
        callback.onError("Correo o contraseña incorrectos");
    }
}
