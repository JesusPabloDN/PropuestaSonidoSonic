package com.example.propuestasonidosonic.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.propuestasonidosonic.model.Usuario;

public class SessionManager {
    private static SessionManager instance;
    private final MutableLiveData<Usuario> usuarioActualLiveData = new MutableLiveData<>(null);

    public SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void iniciarSesion(Usuario usuario) {
        usuarioActualLiveData.setValue(usuario);
    }

    public void cerrarSesion() {
        usuarioActualLiveData.setValue(null);
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
}
