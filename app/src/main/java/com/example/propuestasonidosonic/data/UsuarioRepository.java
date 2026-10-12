package com.example.propuestasonidosonic.data;

import androidx.lifecycle.LiveData;
import com.example.propuestasonidosonic.model.Usuario;
import java.util.List;

public interface UsuarioRepository {
    LiveData<List<Usuario>> getUsuarios();
    void getUsuarioPorId(String id, Callback<Usuario> callback);
    void getUsuarioPorCorreo(String correo, Callback<Usuario> callback);
    void registrarUsuario(Usuario usuario, Callback<Usuario> callback);
    void actualizarUsuario(Usuario usuario, Callback<Usuario> callback);
    void eliminarUsuario(String id, Callback<Void> callback);
    void autenticar(String correo, String contrasena, Callback<Usuario> callback);
}
