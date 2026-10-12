package com.example.propuestasonidosonic.data;

import androidx.lifecycle.LiveData;
import com.example.propuestasonidosonic.model.Paquete;
import java.util.List;

public interface PaqueteRepository {
    LiveData<List<Paquete>> getPaquetes();
    Paquete getPaquetePorId(String id);
    void guardarPaquete(Paquete paquete, Callback<Paquete> callback);
    void eliminarPaquete(String id, Callback<Void> callback);
}
