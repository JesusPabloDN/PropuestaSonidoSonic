package com.example.propuestasonidosonic.data;

import androidx.lifecycle.LiveData;
import com.example.propuestasonidosonic.model.Usuario;
import java.util.List;

public interface UsuarioRepository {
    LiveData<List<Usuario>> getUsuarios();
    Usuario getUsuarioPorId(String id);
    void getUsuarioPorId(String id, Callback<Usuario> callback);
    void getUsuarioPorCorreo(String correo, Callback<Usuario> callback);
    void registrarUsuario(Usuario usuario, Callback<Usuario> callback);
    void actualizarUsuario(Usuario usuario, Callback<Usuario> callback);
    void eliminarUsuario(String id, Callback<Void> callback);
    void autenticar(String correo, String contrasena, Callback<Usuario> callback);
    List<Usuario> obtenerActivosPorRol(String rol);
    void crearPersonal(Usuario nuevo, String contrasena, Callback<Usuario> callback);
    void actualizarPersonal(String actorId, String usuarioId, String nombre, String telefono,
                            Boolean cuentaActiva, String nuevaContrasena, Callback<Usuario> callback);
    void actualizarPerfil(String usuarioId, String nombre, String telefono, Callback<Usuario> callback);
    void cambiarContrasena(String usuarioId, String contrasenaActual, String contrasenaNueva, Callback<Void> callback);
}


