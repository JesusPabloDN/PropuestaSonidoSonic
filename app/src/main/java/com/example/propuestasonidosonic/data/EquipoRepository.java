package com.example.propuestasonidosonic.data;

import androidx.lifecycle.LiveData;
import com.example.propuestasonidosonic.model.BitacoraEntrada;
import com.example.propuestasonidosonic.model.Equipo;
import java.util.List;

public interface EquipoRepository {
    LiveData<List<Equipo>> getEquipos();
    void getEquipoPorId(String id, Callback<Equipo> callback);
    void agregarEquipo(Equipo equipo, Callback<Equipo> callback);
    void agregarUnidades(String nombre, String categoria, double precioRenta, String estadoFisico, int cantidad, String fotoUri, Callback<List<Equipo>> callback);
    void agregarUnidades(String nombre, String categoria, double precioRenta, String estadoFisico, int cantidad, Callback<List<Equipo>> callback);
    void actualizar(Equipo equipo, Callback<Equipo> callback);
    void eliminar(String id, Callback<Void> callback);
    void cambiarEstado(String equipoId, String nuevoEstado, Callback<Equipo> callback);
    void agregarBitacora(BitacoraEntrada entrada, Callback<BitacoraEntrada> callback);
    LiveData<List<BitacoraEntrada>> obtenerBitacora(String equipoId);
    String estadoEfectivo(Equipo equipo, long ahoraMillis);
    com.example.propuestasonidosonic.model.Evento proximaReserva(String equipoId, long ahoraMillis);
    List<com.example.propuestasonidosonic.model.ModeloInventario> obtenerModelos();
    com.example.propuestasonidosonic.model.ModeloInventario obtenerModelo(String clave);
    List<Equipo> listarTodos();
}
