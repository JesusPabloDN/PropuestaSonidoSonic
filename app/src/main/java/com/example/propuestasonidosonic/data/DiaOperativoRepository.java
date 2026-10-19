package com.example.propuestasonidosonic.data;

import androidx.lifecycle.LiveData;
import com.example.propuestasonidosonic.model.DiaOperativo;

import java.util.List;
import java.util.Map;

public interface DiaOperativoRepository {
    LiveData<List<DiaOperativo>> getDias();
    DiaOperativo obtenerDia(String fechaClave);
    Map<String, DiaOperativo> obtenerTodosPorClave();
    void guardarDia(DiaOperativo dia, Callback<DiaOperativo> callback);
    void establecerDisponibilidadAsistente(String fechaClave, String asistenteId, boolean disponible, Callback<DiaOperativo> callback);
    void establecerDisponibilidadAsistenteVarios(List<String> fechaClaves, String asistenteId, boolean disponible, Callback<Void> callback);
}
